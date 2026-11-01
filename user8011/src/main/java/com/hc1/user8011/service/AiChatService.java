package com.hc1.user8011.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hc1.user8011.ai.AiChatRequest;
import com.hc1.user8011.ai.AiChatResponse;
import com.hc1.user8011.ai.AiChatStreamEvent;
import com.hc1.user8011.ai.ChatMessage;
import com.hc1.user8011.config.AiAgentProperties;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 客服业务层。
 *
 * 两条路径：
 * 1) 非流式：优先走 Feign（带 Sentinel 熔断与降级）；
 *    若降级工厂返回兜底话术，则再用 RestTemplate 直连重试一次，避免"AI 明明在线却回答兜底话术"。
 * 2) 流式：RestTemplate 流式读取 ai-agent 的 SSE，逐事件转发给前端 SseEmitter。
 *
 * @Author xwj
 * @Date 2026-10-01
 */
@Slf4j
@Service
public class AiChatService {

    @Autowired
    private AiAgentFeignClient aiAgentFeignClient;

    @Autowired
    private AiAgentClient aiAgentClient;

    @Autowired
    private AiAgentProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    private ExecutorService streamExecutor;

    @PostConstruct
    public void initializeStreamExecutor() {
        streamExecutor = Executors.newFixedThreadPool(Math.max(properties.getStreamPoolSize(), 1));
    }

    @PreDestroy
    public void shutdownStreamExecutor() {
        if (streamExecutor != null) {
            streamExecutor.shutdownNow();
        }
    }

    // ==================== 非流式 ====================

    public String chatWithAgent(String question, String userId, String conversationId) {
        validateQuestion(question);
        AiChatRequest request = buildRequest(question, userId, conversationId, false);

        AiChatResponse response = null;
        try {
            response = aiAgentFeignClient.chat(request);
        } catch (Exception e) {
            log.warn("Feign 调用 AI 客服异常，改用直连重试：{}", e.getMessage());
            response = null;
        }

        if (response == null) {
            try {
                response = aiAgentClient.chat(request, userId);
            } catch (Exception e) {
                log.error("直连 AI 客服也失败。userId={}, conversationId={}", userId, conversationId, e);
                return "小弘现在有点忙，请稍后再问我吧～";
            }
        }

        return extractAnswer(response);
    }

    // ==================== 流式 ====================

    /**
     * 流式问答。
     *
     * SseEmitter 的超时时间设为略大于 Python 侧的读取超时，
     * 保证由 Python 侧先超时并给出可读错误，而不是前端先看到无意义的断连。
     */
    public SseEmitter streamWithAgent(String question, String userId, String conversationId) {
        validateQuestion(question);
        SseEmitter emitter = new SseEmitter((long) properties.getReadTimeout() + 10000L);
        AiChatRequest request = buildRequest(question, userId, conversationId, true);
        final String convId = conversationId;

        emitter.onTimeout(() -> {
            log.warn("SSE 超时。conversationId={}", convId);
            emitter.complete();
        });
        emitter.onError(e -> log.warn("SSE 异常。conversationId={}, err={}", convId, e.getMessage()));
        emitter.onCompletion(() -> log.debug("SSE 完成。conversationId={}", convId));

        streamExecutor.submit(() -> {
            try {
                relayStream(emitter, request, userId);
                emitter.complete();
            } catch (Exception e) {
                log.error("流式转发失败。conversationId={}", convId, e);
                try {
                    emitter.send(SseEmitter.event().data(buildErrorEvent(), org.springframework.http.MediaType.APPLICATION_JSON));
                } catch (Exception ignore) {
                    // 连接已断开，忽略
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    /** 读取 ai-agent 的 SSE 流并逐事件转发。 */
    private void relayStream(SseEmitter emitter, AiChatRequest request, String userId) throws Exception {
        try (InputStream in = aiAgentClient.openStream(request, userId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring("data:".length()).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                AiChatStreamEvent event = parseEvent(payload);
                if (event == null) {
                    continue;
                }
                emitter.send(SseEmitter.event()
                        .name(event.getType() == null ? "message" : event.getType())
                        .data(payload, org.springframework.http.MediaType.APPLICATION_JSON));
            }
        }
    }

    private AiChatStreamEvent parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, AiChatStreamEvent.class);
        } catch (Exception e) {
            log.warn("跳过无法解析的 SSE 事件：{}", payload);
            return null;
        }
    }

    private String buildErrorEvent() {
        return "{\"type\":\"error\",\"text\":\"小弘现在有点忙，请稍后再问我吧～\",\"code\":\"upstream_error\"}";
    }

    // ==================== 公共方法 ====================

    private void validateQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (question.length() > properties.getMaxQuestionLength()) {
            throw new IllegalArgumentException("问题过长，请精简后再提问");
        }
    }

    private AiChatRequest buildRequest(String question, String userId, String conversationId, boolean stream) {
        AiChatRequest request = new AiChatRequest();
        request.setMessages(Collections.singletonList(new ChatMessage("user", question)));
        request.setUserId(userId);
        request.setConversationId(conversationId);
        request.setStream(stream);
        return request;
    }

    private String extractAnswer(AiChatResponse response) {
        if (response != null
                && response.getChoices() != null
                && !response.getChoices().isEmpty()
                && response.getChoices().get(0).getMessage() != null) {
            String answer = response.getChoices().get(0).getMessage().getContent();
            if (answer != null && !answer.trim().isEmpty()) {
                return answer;
            }
        }
        return "AI 没有返回有效回答，请重新提问。";
    }
}
