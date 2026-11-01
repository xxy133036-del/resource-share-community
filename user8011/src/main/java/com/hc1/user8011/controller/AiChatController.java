package com.hc1.user8011.controller;

import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.service.AiChatService;
import com.hc1.user8011.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * AI 客服对话接口。
 *
 * - POST /chat/ask    ：非流式，一次返回完整回答（保持兼容）
 * - GET  /chat/stream ：流式 SSE，前端可实时看到思考过程与逐字输出
 *
 * 说明：统一使用 Spring MVC 的 SseEmitter，项目无需引入 WebFlux。
 *
 * @Author xwj
 * @Date 2026-10-01
 */
@Slf4j
@RestController
@RequestMapping("/chat")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/ask")
    public CommonResult<?> ask(@RequestParam("question") String question,
                               @RequestParam(value = "conversationId", required = false) String conversationId) {
        Integer userIdInt = UserContext.getUserId();
        if (userIdInt == null) {
            return CommonResult.error(401, "未登录");
        }
        String userId = String.valueOf(userIdInt);
        String convId = normalizeConversationId(conversationId);

        log.info("用户 {} [会话 {}] 提交问题，长度 {}", userId, convId, question.length());
        try {
            String answer = aiChatService.chatWithAgent(question, userId, convId);
            return CommonResult.success(answer);
        } catch (IllegalArgumentException e) {
            return CommonResult.error(400, e.getMessage());
        }
    }

    /**
     * 流式问答。
     *
     * 用 GET 而非 POST，是为了浏览器端可以直接用 EventSource；
     * 代价是问题需要通过查询参数传递，因此限制了最大长度。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("question") String question,
                             @RequestParam(value = "conversationId", required = false) String conversationId) {
        Integer userIdInt = UserContext.getUserId();
        if (userIdInt == null) {
            throw new IllegalArgumentException("未登录");
        }
        String userId = String.valueOf(userIdInt);
        String convId = normalizeConversationId(conversationId);

        log.info("用户 {} [会话 {}] 提交流式问题，长度 {}", userId, convId, question.length());
        return aiChatService.streamWithAgent(question, userId, convId);
    }

    /** 会话 ID 为空时由服务端生成，避免前端必须自己维护。 */
    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return "conv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        return conversationId.trim();
    }
}
