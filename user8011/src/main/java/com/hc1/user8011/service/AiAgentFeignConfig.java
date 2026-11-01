package com.hc1.user8011.service;

import com.hc1.user8011.config.AiAgentProperties;
import feign.Request;
import com.hc1.user8011.util.AiAgentSigner;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

/**
 * AI Agent Feign 的专属配置（不加 @Configuration，避免被全局扫描到其他 Feign 客户端）。
 *
 * 职责：注入内部调用标记、链路 ID 与 RSA 签名；设置连接/读取超时。
 *
 * @Author xwj
 * @Date 2026-10-01
 */
public class AiAgentFeignConfig {

    @Autowired
    private AiAgentProperties properties;

    @Bean
    public RequestInterceptor aiAgentRequestInterceptor() {
        return template -> {
            String requestId = UUID.randomUUID().toString().replace("-", "");
            template.header("X-Internal-Call", "true");
            template.header("X-Request-Id", requestId);

            String privateKeyFile = properties.getPrivateKeyFile();
            if (AiAgentSigner.isConfigured(privateKeyFile)) {
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
                byte[] body = template.body() == null ? new byte[0] : template.body();
                template.header("X-Timestamp", timestamp);
                // 签名覆盖 method + path + query + body：
                // 只签 body 的话，GET 这类无 body 的请求可被改参数绕过
                template.header("X-Sign", AiAgentSigner.sign(
                        privateKeyFile, timestamp, requestId, body,
                        template.method(), template.path(), template.queryLine()));
            }
        };
    }

    @Bean
    public Request.Options aiAgentRequestOptions() {
        return new Request.Options(properties.getConnectTimeout(), properties.getAskTimeout());
    }
}
