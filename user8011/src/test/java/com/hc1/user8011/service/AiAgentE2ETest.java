package com.hc1.user8011.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hc1.user8011.ai.AiChatRequest;
import com.hc1.user8011.ai.AiChatResponse;
import com.hc1.user8011.ai.ChatMessage;
import com.hc1.user8011.config.AiAgentProperties;
import com.hc1.user8011.util.AiAgentSigner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端联调测试：需要本地已启动 ai-agent（默认 127.0.0.1:8013）。
 *
 * 验证重点：
 * 1) Java 生成的 RSA 签名能被 Python 端验证通过（跨语言算法一致性）
 * 2) 非流式调用能拿到真实回答
 *
 * 服务未启动时自动跳过，不阻塞 CI。
 *
 * @Author xwj
 * @Date 2026-10-01
 */
class AiAgentE2ETest {

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

    private AiAgentProperties props() {
        AiAgentProperties p = new AiAgentProperties();
        p.setUrl(BASE);
        p.setPrivateKeyFile(PRIVATE_KEY_FILE);
        p.setAskTimeout(120000);
        p.setConnectTimeout(5000);
        p.setReadTimeout(300000);
        return p;
    }

    private AiAgentClient client() {
        AiAgentClient c = new AiAgentClient();
        ReflectionTestUtils.setField(c, "properties", props());
        ReflectionTestUtils.setField(c, "objectMapper", new ObjectMapper());
        return c;
    }

    @Test
    void chatWithSignedRequestSucceeds() throws Exception {
        Assumptions.assumeTrue(agentUp(), "ai-agent 未启动，跳过端到端测试");
        Assumptions.assumeTrue(AiAgentSigner.isConfigured(PRIVATE_KEY_FILE), "未配置 AI_AGENT_PRIVATE_KEY_FILE，跳过端到端测试");

        AiChatRequest request = new AiChatRequest();
        request.setMessages(Collections.singletonList(new ChatMessage("user", "平台上有没有 Java 资料？")));
        request.setUserId("15");
        request.setConversationId("conv_java_e2e");
        request.setStream(false);

        AiChatResponse response = client().chat(request, "15");

        assertNotNull(response, "响应不应为 null");
        assertNotNull(response.getChoices());
        assertTrue(response.getChoices().size() > 0, "应至少返回一个 choice");

        String answer = response.getChoices().get(0).getMessage().getContent();
        System.out.println("Java->Python 端到端回答: " + answer);
        assertNotNull(answer);
        assertTrue(answer.length() > 0, "回答不应为空");
    }

    @Test
    void wrongPrivateKeyIsRejectedByPython() throws Exception {
        Assumptions.assumeTrue(agentUp(), "ai-agent 未启动，跳过端到端测试");
        Assumptions.assumeTrue(AiAgentSigner.isConfigured(PRIVATE_KEY_FILE), "未配置 AI_AGENT_PRIVATE_KEY_FILE，跳过端到端测试");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        byte[] wrongKey = generator.generateKeyPair().getPrivate().getEncoded();
        String wrongPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(wrongKey)
                + "\n-----END PRIVATE KEY-----\n";
        Path wrongKeyFile = Files.createTempFile("ai-agent-wrong-private", ".pem");
        Files.write(wrongKeyFile, wrongPem.getBytes(StandardCharsets.UTF_8));

        AiAgentProperties bad = props();
        bad.setPrivateKeyFile(wrongKeyFile.toString());
        AiAgentClient c = new AiAgentClient();
        ReflectionTestUtils.setField(c, "properties", bad);
        ReflectionTestUtils.setField(c, "objectMapper", new ObjectMapper());

        AiChatRequest request = new AiChatRequest();
        request.setMessages(Collections.singletonList(new ChatMessage("user", "你好")));
        request.setUserId("15");
        request.setConversationId("conv_java_badsig");
        request.setStream(false);

        boolean rejected = false;
        try {
            c.chat(request, "15");
        } catch (Exception e) {
            rejected = true;
        } finally {
            Files.deleteIfExists(wrongKeyFile);
        }
        assertTrue(rejected, "不匹配的 RSA 私钥签名必须被 Python 拒绝");
    }
}
