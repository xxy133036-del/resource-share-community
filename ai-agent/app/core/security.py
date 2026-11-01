"""服务间鉴权：RSA-SHA256 请求签名。

背景：旧实现中 Python 端无条件信任请求体里的 user_id，
任何能访问 8013 的调用方都能越权查他人数据。
现改为由 Java 侧签名，Python 侧校验，user_id 只从签名上下文取。

## 签名覆盖范围

只签 body 是不够的：GET 请求没有 body，攻击者只要改一下查询参数
（例如把 userId=15 换成 userId=1）就能越权，签名形同虚设。
因此签名覆盖整个请求的规范化描述：

    canonical = f"{METHOD}\n{PATH}\n{CANONICAL_QUERY}\n{sha256hex(body)}"
    payload   = f"{timestamp}.{request_id}.{sha256hex(canonical)}"
    sign      = RSA_SHA256_sign(java_private_key, payload)

其中 CANONICAL_QUERY 是按键名排序后的 `k=v&k=v`，保证两端拼出同一串。

请求头：X-Timestamp / X-Request-Id / X-Sign
"""

import base64
import hashlib
import time
from functools import lru_cache
from pathlib import Path
from typing import Optional
from urllib.parse import parse_qsl, quote

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

from app.core.exceptions import AuthError
from app.core.logging import get_logger
from app.core.settings import Settings

logger = get_logger(__name__)

# 允许的时间偏差（秒），用于防重放
_MAX_SKEW_SECONDS = 300


def _sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data or b"").hexdigest()


def canonical_query(query: str) -> str:
    """把查询串规范化：按键名排序后重新拼接。

    排序是必须的——`a=1&b=2` 和 `b=2&a=1` 语义相同，
    若不排序，两端可能拼出不同字符串导致误判为签名错误。
    空值参数也保留在规范化结果中，不会因排序而被丢弃。
    """
    if not query:
        return ""
    # Sort the ASCII-encoded components so Java and Python use identical
    # ordering even for non-BMP Unicode characters.
    pairs = [
        (quote(k, safe="~"), quote(v, safe="~"))
        for k, v in parse_qsl(query, keep_blank_values=True)
    ]
    pairs.sort()
    # Encoding before joining prevents delimiters inside values from changing
    # the parameter structure (`a=b%26c%3Dd` vs `a=b&c=d`).
    return "&".join(f"{k}={v}" for k, v in pairs)


def canonical_request(method: str, path: str, query: str, body: bytes) -> str:
    """构造规范化请求描述。两端必须完全一致。"""
    return "\n".join(
        [
            (method or "POST").upper(),
            path or "",
            canonical_query(query),
            _sha256_hex(body),
        ]
    )


@lru_cache(maxsize=4)
def _load_public_key(public_key_file: str):
    key = serialization.load_pem_public_key(Path(public_key_file).read_bytes())
    if not isinstance(key, rsa.RSAPublicKey) or key.key_size < 2048:
        raise ValueError("服务间验签公钥必须是至少 2048 位的 RSA 公钥")
    return key


def verify_signature(
    settings: Settings,
    *,
    timestamp: Optional[str],
    request_id: Optional[str],
    signature: Optional[str],
    body: bytes,
    method: str = "POST",
    path: str = "",
    query: str = "",
) -> None:
    """校验签名。未配置公钥时仅允许本地开发模式跳过。"""
    if not settings.auth_enabled:
        return

    if not timestamp or not request_id or not signature:
        raise AuthError("缺少鉴权请求头")

    try:
        ts = float(timestamp)
    except (TypeError, ValueError) as exc:
        raise AuthError("时间戳格式错误") from exc

    if abs(time.time() - ts) > _MAX_SKEW_SECONDS:
        raise AuthError("请求已过期，请检查系统时间")

    digest = _sha256_hex(canonical_request(method, path, query, body).encode("utf-8"))
    payload = f"{timestamp}.{request_id}.{digest}".encode("utf-8")
    try:
        signature_bytes = base64.b64decode(signature, validate=True)
        _load_public_key(settings.internal_call_public_key_file).verify(
            signature_bytes, payload, padding.PKCS1v15(), hashes.SHA256()
        )
    except (ValueError, InvalidSignature, OSError) as exc:
        logger.warning(
            "signature_mismatch", request_id=request_id, method=method, path=path
        )
        raise AuthError("签名校验失败")
