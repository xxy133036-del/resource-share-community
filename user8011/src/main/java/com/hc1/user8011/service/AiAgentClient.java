package com.hc1.user8011.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hc1.user8011.ai.AiChatRequest;
import com.hc1.user8011.ai.AiChatResponse;
import com.hc1.user8011.config.AiAgentProperties;
import com.hc1.user8011.util.AiAgentSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Agent 调用客户端。
 *
 * 说明：
 * 1) 非流式走 RestTemplate，简单直接。
 * 2) 流式必须用 HttpURLConnection 手工建连，不能用 RestTemplate。
 *    原因：RestTemplate.execute 在返回前会执行 response.close()，把响应流一并关掉，
 *    调用方拿到的是一个已关闭的流，读取时直接抛 IOException。
 *    这里改为自行建连，并把 conn.disconnect() 挂到流关闭时，保证连接正常释放。
 *
 * @Author xwj
 * @Date 2026-10-01
 */
@Slf4j
@Component
public class AiAgentClient {

    private static final String PATH_COMPLETIONS = "/v1/chat/completions";
    private static final String PATH_STREAM = "/v1/chat/stream";

    @Autowired
    private AiAgentProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    /** 非流式调用，用于 Feign 降级后的兜底路径 */
    public AiChatResponse chat(AiChatRequest request, String userId) throws Exception {
        String baseUrl = trimTrailingSlash(properties.getUrl());
        byte[] body = objectMapper.writeValueAsBytes(request);
        String requestId = UUID.randomUUID().toString().replace("-", "");

        HttpHeaders headers = buildHeaders(userId, requestId, body, "POST", PATH_COMPLETIONS, "");
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate(buildRequestFactory(properties.getAskTimeout()));
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(baseUrl + PATH_COMPLETIONS, entity, String.class);
            return objectMapper.readValue(response, AiChatResponse.class);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("AI 客服响应超时或不可达", e);
        } catch (RestClientException e) {
            throw new IllegalStateException("AI 客服调用失败：" + e.getMessage(), e);
        }
    }

    /**
     * 建立流式连接，返回可逐行读取的响应流。
     *
     * 读取超时在流式场景下代表"两次数据之间的最大间隔"，
     * 因此不会因为回答较长而中途断流。
     */
    public InputStream openStream(AiChatRequest request, String userId) throws Exception {
        String baseUrl = trimTrailingSlash(properties.getUrl());
        byte[] body = objectMapper.writeValueAsBytes(request);
        String requestId = UUID.randomUUID().toString().replace("-", "");
        HttpHeaders headers = buildHeaders(userId, requestId, body, "POST", PATH_STREAM, "");

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + PATH_STREAM).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(properties.getConnectTimeout());
        conn.setReadTimeout(properties.getReadTimeout());
        conn.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        conn.setRequestProperty("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);
        // 显式声明不带压缩，避免中间层压缩后破坏 SSE 分片边界
        conn.setRequestProperty("Accept-Encoding", "identity");
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            for (String value : entry.getValue()) {
                conn.addRequestProperty(entry.getKey(), value);
            }
        }

        try {
            OutputStream out = conn.getOutputStream();
            try {
                out.write(body);
                out.flush();
            } finally {
                out.close();
            }

            int code = conn.getResponseCode();
            if (code >= 400) {
                String detail = readQuietly(conn.getErrorStream());
                conn.disconnect();
                throw new IllegalStateException("AI 客服流式调用失败：HTTP " + code + " " + detail);
            }
            return new ConnectionBoundInputStream(conn.getInputStream(), conn);
        } catch (IOException e) {
            conn.disconnect();
            throw e;
        }
    }

    /** 流关闭时同步释放底层连接，避免连接泄漏。 */
    private static final class ConnectionBoundInputStream extends FilterInputStream {
        private final HttpURLConnection connection;

        private ConnectionBoundInputStream(InputStream in, HttpURLConnection connection) {
            super(in);
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                connection.disconnect();
            }
        }
    }

    private static String readQuietly(InputStream in) {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int len;
            while ((len = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, len);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * 组装内部调用请求头：内部标记 + 链路 ID + 签名。
     *
     * 签名覆盖 method + path + query + body，与 Python 端一致；
     * 只签 body 时，GET 这类无 body 的请求可被改参数绕过。
     */
    private HttpHeaders buildHeaders(String userId, String requestId, byte[] body,
                                     String method, String path, String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Call", "true");
        headers.set("X-Request-Id", requestId);

        String privateKeyFile = properties.getPrivateKeyFile();
        if (AiAgentSigner.isConfigured(privateKeyFile)) {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
            headers.set("X-Timestamp", timestamp);
            headers.set("X-Sign", AiAgentSigner.sign(
                    privateKeyFile, timestamp, requestId, body, method, path, query));
        } else {
            log.warn("未配置 ai-agent.private-key-file，跳过签名校验（仅限本地调试）");
        }
        return headers;
    }

    private SimpleClientHttpRequestFactory buildRequestFactory(int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
