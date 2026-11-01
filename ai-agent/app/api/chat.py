"""对话接口。

- POST /v1/chat/completions : 非流式，OpenAI 兼容，保持与 Java 侧现有 DTO 对齐
- POST /v1/chat/stream      : 流式 SSE，主推路径
"""

import time
import uuid
from typing import Any, Dict

import structlog
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse, StreamingResponse

from app.core.exceptions import AuthError, ValidationError
from app.core.logging import get_logger
from app.core.security import verify_signature
from app.schemas.chat import ChatRequest
from app.services.chat_service import ChatService

router = APIRouter(prefix="/v1/chat", tags=["chat"])
logger = get_logger(__name__)

SSE_HEADERS = {
    "Cache-Control": "no-cache",
    "Connection": "keep-alive",
    "X-Accel-Buffering": "no",  # 关闭 nginx 缓冲，否则流式会被攒批
}


def verify_request(request: Request, body: bytes) -> None:
    """校验请求签名。

    签名覆盖 method + path + query + body 四部分，所以 GET 这类
    没有 body 的请求同样受保护——否则只改查询参数即可绕过校验。
    """
    settings = request.app.state.settings
    verify_signature(
        settings,
        timestamp=request.headers.get("X-Timestamp"),
        request_id=request.headers.get("X-Request-Id"),
        signature=request.headers.get("X-Sign"),
        body=body,
        method=request.method,
        path=request.url.path,
        query=request.url.query,
    )


def _resolve_identity(request: Request, payload: ChatRequest, body: bytes) -> tuple[str, str]:
    """校验签名并返回 (user_id, conversation_id)。

    安全要点：user_id 只在签名校验通过后才采用；
    未配置密钥时（本地调试）允许直接使用请求体中的值。
    """
    settings = request.app.state.settings
    verify_request(request, body)
    user_id = payload.user_id
    if settings.auth_enabled and not user_id:
        raise AuthError("签名请求缺少 user_id")
    conversation_id = payload.conversation_id or f"conv_{uuid.uuid4().hex[:16]}"
    return user_id or "anonymous", conversation_id


@router.post("/completions")
async def chat_completions(request: Request) -> JSONResponse:
    """非流式对话（OpenAI 兼容响应体）。"""
    raw = await request.body()
    payload = ChatRequest.model_validate_json(raw)
    user_id, conversation_id = _resolve_identity(request, payload, raw)
    question = payload.last_user_message
    if not question:
        raise ValidationError("messages 中缺少用户提问内容")

    service: ChatService = request.app.state.chat_service
    structlog.contextvars.bind_contextvars(conversation_id=conversation_id, user_id=user_id)

    result = await service.chat(question, user_id, conversation_id)

    return JSONResponse(
        content={
            "id": f"chatcmpl-{uuid.uuid4().hex}",
            "object": "chat.completion",
            "created": int(time.time()),
            "choices": [
                {
                    "index": 0,
                    "message": {"role": "assistant", "content": result["content"]},
                    "finish_reason": "stop",
                }
            ],
            "usage": result.get("usage") or {},
        }
    )


@router.post("/stream")
async def chat_stream(request: Request) -> StreamingResponse:
    """流式对话（SSE）。事件类型见 schemas.SSEEvent。"""
    raw = await request.body()
    payload = ChatRequest.model_validate_json(raw)
    user_id, conversation_id = _resolve_identity(request, payload, raw)
    question = payload.last_user_message
    if not question:
        raise ValidationError("messages 中缺少用户提问内容")

    service: ChatService = request.app.state.chat_service

    async def event_source():
        # 注意：structlog 的 bound_contextvars 是同步上下文管理器，不能 async with
        with structlog.contextvars.bound_contextvars(
            conversation_id=conversation_id, user_id=user_id
        ):
            from app.schemas.chat import SSEEvent

            # 先告知会话 ID，便于前端续聊
            yield SSEEvent(type="status", text="正在连接小弘…", conversation_id=conversation_id).to_sse()
            try:
                async for event in service.stream_chat(question, user_id, conversation_id):
                    if event.get("type") == "done":
                        yield SSEEvent(
                            type="done",
                            text=event.get("text"),
                            usage=event.get("usage"),
                            conversation_id=conversation_id,
                            message_id=event.get("message_id"),
                        ).to_sse()
                    else:
                        yield SSEEvent(**event).to_sse()
            except Exception as exc:  # noqa: BLE001
                logger.error("sse_stream_error", error=str(exc))
                yield SSEEvent(type="error", text="服务暂时不可用，请稍后再试", code="upstream_error").to_sse()
            yield "data: [DONE]\n\n"

    return StreamingResponse(event_source(), media_type="text/event-stream", headers=SSE_HEADERS)