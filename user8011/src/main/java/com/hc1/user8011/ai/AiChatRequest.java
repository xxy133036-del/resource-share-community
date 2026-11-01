package com.hc1.user8011.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 调用 Python AI Agent 的请求体
 * 对应 Python 端的 ChatRequest
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)  // 为 null 的字段不序列化（可选）
public class AiChatRequest {
    private List<ChatMessage> messages;
    @JsonProperty("user_id")  // 消息列表，至少包含一条用户消息
    private String userId;               // 当前用户ID（必填）
    @JsonProperty("conversation_id")
    private String conversationId;       // 会话ID（可选，用于多轮对话）
    private Boolean stream;              // 是否启用流式输出，默认 false

}