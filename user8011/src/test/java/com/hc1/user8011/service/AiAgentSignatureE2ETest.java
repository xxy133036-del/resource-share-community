package com.hc1.user8011.service;

import com.hc1.user8011.util.AiAgentSigner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 跨语言 RSA 签名端到端测试：Java 私钥签名 -> Python 公钥验签。
 *
 * 这是本次"签名覆盖 method+path+query"改造的核心回归：
 * 两端算法若有一处不一致，所有内部调用都会 401。
 * 该测试不依赖 LLM（走会话查询接口），因此上游抖动时依然可跑。
 *
 * Python 服务未启动时自动跳过。
 *
 * @Author xwj
 * @Date 2026-10-04
 */
class AiAgentSignatureE2ETest {

    private static final String PRIVATE_KEY_FILE = System.getenv("AI_AGENT_PRIVATE_KEY_FILE");
    private static final String BASE = "http://127.0.0.1:8013";

    private boolean agentUp() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 8013), 1500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int get(String path, String query, String signedQuery) throws Exception {
        String url = BASE + path + (query.isEmpty() ? "" : "?" + query);
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String sign = AiAgentSigner.sign(PRIVATE_KEY_FILE, timestamp, requestId, new byte[0],
                "GET", path, signedQuery);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Request-Id", requestId);
        conn.setRequestProperty("X-Timestamp", timestamp);
        conn.setRequestProperty("X-Sign", sign);
        try {
            int code = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            System.out.println("GET " + path + "?" + query + " -> " + code + " " + sb);
            return code;
        } finally {
            conn.disconnect();
        }
    }

    @Test
    void javaSignatureAcceptedByPythonOnGetWithQuery() throws Exception {
        Assumptions.assumeTrue(agentUp(), "ai-agent 未启动，跳过跨语言签名测试");
        Assumptions.assumeTrue(AiAgentSigner.isConfigured(PRIVATE_KEY_FILE), "未配置 AI_AGENT_PRIVATE_KEY_FILE，跳过跨语言签名测试");

        int code = get("/v1/sessions", "userId=15&limit=3", "userId=15&limit=3");
        assertEquals(200, code, "Java 签名必须被 Python 接受（含查询参数）");
    }

    @Test
    void tamperedQueryRejectedByPython() throws Exception {
        Assumptions.assumeTrue(agentUp(), "ai-agent 未启动，跳过跨语言签名测试");
        Assumptions.assumeTrue(AiAgentSigner.isConfigured(PRIVATE_KEY_FILE), "未配置 AI_AGENT_PRIVATE_KEY_FILE，跳过跨语言签名测试");

        // 用 userId=15 的签名，实际发送 userId=1 —— 必须被拒绝
        int code = get("/v1/sessions", "userId=1&limit=3", "userId=15&limit=3");
        assertEquals(401, code, "篡改查询参数必须被 Python 拒绝");
    }

    @Test
    void missingSignatureRejectedByPython() throws Exception {
        Assumptions.assumeTrue(agentUp(), "ai-agent 未启动，跳过跨语言签名测试");

        HttpURLConnection conn = (HttpURLConnection) new URL(BASE + "/v1/sessions?userId=15").openConnection();
        conn.setRequestMethod("GET");
        try {
            assertTrue(conn.getResponseCode() == 401, "无签名必须被拒绝");
        } finally {
            conn.disconnect();
        }
    }
}
