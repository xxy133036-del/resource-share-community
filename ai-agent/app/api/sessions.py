"""会话管理接口。

对应成熟 Agent 会话模型里的"索引视图"能力：
- 列出我的会话
- 查看某个会话的完整事件日志（含工具调用，用于回放与排障）
- 重命名 / 归档

安全：每个接口都先验签（覆盖 method+path+query+body），
再以签名上下文里的 user_id 为准；会话归属不匹配一律按"不存在"处理，
不泄露他人会话是否存在。

为什么这里必须验签：这些接口是 GET，没有 body。
若只验 body，攻击者只需改一下 userId 查询参数就能读他人的会话列表。
签名把 query 也纳入计算后，改参数会直接导致签名不匹配。
"""

from typing import Any, Optional

import structlog
from fastapi import APIRouter, Query, Request
from fastapi.responses import JSONResponse

from app.core.exceptions import AuthError, NotFoundError, ValidationError
from app.core.logging import get_logger
from app.core.security import verify_signature
from app.infra.session_store import SessionStore

router = APIRouter(prefix="/v1/sessions", tags=["sessions"])
logger = get_logger(__name__)


async def _resolve_user(request: Request, user_id: Optional[str]) -> str:
    """先验签，再解析会话归属者。

    生产路径下 user_id 来自签名上下文（Java 侧注入）；
    未启用鉴权时（本地调试）才允许显式传入。
    """
    settings = request.app.state.settings
    body = await request.body()
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
    if settings.auth_enabled and not user_id:
        raise AuthError("缺少用户身份")
    return str(user_id or "anonymous")


@router.get("")
async def list_sessions(
    request: Request,
    user_id: Optional[str] = Query(default=None, alias="userId"),
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    include_archived: bool = Query(default=False, alias="includeArchived"),
) -> JSONResponse:
    """列出当前用户的会话，按最近活跃倒序。"""
    owner = await _resolve_user(request, user_id)
    store: SessionStore = request.app.state.sessions
    structlog.contextvars.bind_contextvars(user_id=owner)

    rows = await store.list_sessions(
        owner, limit=limit, offset=offset, include_archived=include_archived
    )
    return JSONResponse(
        content={
            "sessions": [_serialize(row) for row in rows],
            "limit": limit,
            "offset": offset,
        }
    )


@router.get("/{session_id}")
async def get_session(
    session_id: str,
    request: Request,
    user_id: Optional[str] = Query(default=None, alias="userId"),
    event_limit: int = Query(default=200, ge=1, le=1000, alias="eventLimit"),
) -> JSONResponse:
    """查看会话详情与完整事件日志。"""
    owner = await _resolve_user(request, user_id)
    store: SessionStore = request.app.state.sessions
    structlog.contextvars.bind_contextvars(user_id=owner, conversation_id=session_id)

    session = await store.get_session(session_id, owner)
    if not session:
        raise NotFoundError("会话不存在")

    events = await store.get_events(session_id, owner, limit=event_limit)
    return JSONResponse(
        content={"session": _serialize(session), "events": [_serialize_event(e) for e in events]}
    )


@router.patch("/{session_id}")
async def rename_session(
    session_id: str,
    request: Request,
    user_id: Optional[str] = Query(default=None, alias="userId"),
    title: str = Query(..., min_length=1, max_length=255),
) -> JSONResponse:
    """重命名会话。"""
    owner = await _resolve_user(request, user_id)
    store: SessionStore = request.app.state.sessions
    if not await store.rename_session(session_id, owner, title):
        raise NotFoundError("会话不存在")
    return JSONResponse(content={"session_id": session_id, "title": title})


@router.delete("/{session_id}")
async def archive_session(
    session_id: str,
    request: Request,
    user_id: Optional[str] = Query(default=None, alias="userId"),
) -> JSONResponse:
    """归档会话（软删除，事件日志保留，仍可回溯）。"""
    owner = await _resolve_user(request, user_id)
    store: SessionStore = request.app.state.sessions

    archived = await store.archive_session(session_id, owner)
    if not archived:
        raise NotFoundError("会话不存在")
    logger.info("session_archive_requested", conversation_id=session_id, user_id=owner)
    return JSONResponse(content={"session_id": session_id, "status": "archived"})


# ---------------- 序列化 ----------------
def _serialize(row: dict) -> dict:
    return {
        "sessionId": row.get("session_id"),
        "title": row.get("title"),
        "status": row.get("status"),
        "turnCount": row.get("turn_count"),
        "messageCount": row.get("message_count"),
        "totalTokens": row.get("total_tokens"),
        "createdAt": _iso(row.get("created_at")),
        "updatedAt": _iso(row.get("updated_at")),
    }


def _serialize_event(row: dict) -> dict:
    payload: Any = row.get("payload")
    if isinstance(payload, str):
        try:
            import json

            payload = json.loads(payload)
        except (TypeError, ValueError):
            payload = None
    return {
        "ordinal": row.get("ordinal"),
        "turnId": row.get("turn_id"),
        "type": row.get("type"),
        "role": row.get("role"),
        "content": row.get("content"),
        "payload": payload,
        "createdAt": _iso(row.get("created_at")),
    }


def _iso(value: Any) -> Optional[str]:
    return value.isoformat() if hasattr(value, "isoformat") else value