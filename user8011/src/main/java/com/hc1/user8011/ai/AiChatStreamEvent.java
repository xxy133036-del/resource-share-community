package com.hc1.user8011.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * AI 客服流式事件。
 *
 * 与 Python 端 app/schemas/chat.py 中的 SSEEvent 对应。
 * type 取值：status / thinking / tool / token / done / error
 *
 * @Author xwj
 * @Date 2026-10-01
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AiChatStreamEvent {

    public static final String TYPE_STATUS = "status";
    public static final String TYPE_THINKING = "thinking";
    public static final String TYPE_TOOL = "tool";
    public static final String TYPE_TOKEN = "token";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    /** 事件类型 */
    private String type;

    /** 文本内容：status 提示语 / thinking 思考片段 / token 正文片段 */
    private String text;

    /** 工具名（type=tool 时） */
    private String name;

    /** 会话 ID（status 首帧与 done 帧携带） */
    private String conversationId;

    /** 消息 ID（type=done 时） */
    private String messageId;

    /** 错误码（type=error 时） */
    private String code;
}