"""FastAPI 依赖：请求上下文与鉴权。"""

import uuid
from typing import Optional

from fastapi import Header, Request

from app.core.exceptions import AuthError
from app.core.security import verify_signature
from app.core.settings import Settings


class RequestContext:
    """单次请求的上下文，承载服务端可信身份。"""

    def __init__(self, *, request_id: str, user_id: Optional[str], conversation_id: Optional[str]):
        self.request_id = request_id
        self.user_id = user_id
        self.conversation_id = conversation_id


async def get_request_context(
    request: Request,
    x_request_id: Optional[str] = Header(default=None),
    x_timestamp: Optional[str] = Header(default=None),
    x_sign: Optional[str] = Header(default=None),
) -> RequestContext:
    """校验签名并构造请求上下文。

    user_id 从请求体读取，但只有在签名校验通过后才可信；
    未配置签名密钥时（本地调试）不做校验。
    """
    body = await request.body()
    settings: Settings = request.app.state.settings

    verify_signature(
        settings,
        timestamp=x_timestamp,
        request_id=x_request_id,
        signature=x_sign,
        body=body,
    )

    request_id = x_request_id or f"req_{uuid.uuid4().hex[:16]}"
    return RequestContext(request_id=request_id, user_id=None, conversation_id=None)