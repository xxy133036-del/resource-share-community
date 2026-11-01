"""对外数据契约。

1) OpenAI 兼容体：保持与 Java 侧现有 DTO（AiChatRequest/AiChatResponse）一致，
   老接口 POST /v1/chat/completions 可原样工作。
2) SSE 事件体：流式接口 POST /v1/chat/stream 使用。
"""

from typing import Any, Dict, List, Literal, Optional

from pydantic import BaseModel, Field


# ==================== 请求 ====================
class ChatMessage(BaseModel):
    role: str = Field(..., description="user / assistant / system")
    content: str = ""


class ChatRequest(BaseModel):
    """兼容 Java 侧 AiChatRequest。"""

    messages: List[ChatMessage] = Field(..., min_length=1)
    user_id: Optional[str] = Field(default=None, alias="userId")
    conversation_id: Optional[str] = Field(default=None, alias="conversationId")
    stream: Optional[bool] = False

    model_config = {"populate_by_name": True, "extra": "ignore"}

    @property
    def last_user_message(self) -> str:
        for m in reversed(self.messages):
            if m.role == "user" and m.content.strip():
                return m.content.strip()
        return ""


# ==================== 响应（OpenAI 兼容） ====================
class ChoiceMessage(BaseModel):
    role: str = "assistant"
    content: str = ""


class Choice(BaseModel):
    index: int = 0
    message: ChoiceMessage
    finish_reason: str = "stop"


class ChatResponse(BaseModel):
    id: str
    object: str = "chat.completion"
    created: int
    choices: List[Choice]
    usage: Optional[Dict[str, Any]] = None


# ==================== SSE 事件 ====================
# type=status  : 状态提示，如"正在检索平台资源…"
# type=thinking: 模型推理过程（本模型会输出 reasoning_content）
# type=tool    : 工具调用信息（名称 + 参数）
# type=token   : 正文增量
# type=done    : 结束，附带 usage 与会话信息
# type=error   : 错误
SSEEventType = Literal["status", "thinking", "tool", "token", "done", "error"]


class SSEEvent(BaseModel):
    type: SSEEventType
    text: Optional[str] = None
    name: Optional[str] = None
    args: Optional[Dict[str, Any]] = None
    usage: Optional[Dict[str, Any]] = None
    conversation_id: Optional[str] = None
    message_id: Optional[str] = None
    code: Optional[str] = None

    def to_sse(self) -> str:
        return f"data: {self.model_dump_json(exclude_none=True)}\n\n"


class HealthResponse(BaseModel):
    status: str
    version: str
    engine: str
    dependencies: Dict[str, Any]