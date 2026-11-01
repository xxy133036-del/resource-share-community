package com.hc1.user8011.service;

import com.hc1.user8011.ai.AiChatRequest;
import com.hc1.user8011.ai.AiChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI Agent 的 Feign 客户端。
 *
 * 注意：
 * 1) 这里只承载非流式调用。流式转发由 {@link AiAgentClient} 用 RestTemplate 完成，
 *    因为 Feign 会把响应体一次性读完，无法逐块转发给 SseEmitter。
 * 2) 未配置 fallbackFactory：Sentinel 2.2.5 的 Feign 降级依赖已废弃的 feign-hystrix 模块。
 *    降级改为在 {@link AiChatService} 中通过 try/catch + 直连重试实现，
 *    既避免引入 Hystrix，也让兜底逻辑更可控。
 *
 * @Author xwj
 * @Date 2026-10-01
 */
@FeignClient(
        name = "ai-agent",
        url = "${ai-agent.url}",
        configuration = AiAgentFeignConfig.class
)
public interface AiAgentFeignClient {

    @PostMapping("/v1/chat/completions")
    AiChatResponse chat(@RequestBody AiChatRequest request);
}
