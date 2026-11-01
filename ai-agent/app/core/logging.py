"""结构化日志：JSON 输出 + 敏感信息脱敏。

设计要点：
- 每条日志携带 request_id / conversation_id / user_id，可与 Java 侧日志串联。
- 密钥、密码、token 一律在进入日志前被替换为 ***，避免泄露。
"""

import logging
import re
import sys
from typing import Any, Dict, MutableMapping

import structlog

# 需要脱敏的字段名：精确匹配（避免误伤 input_tokens 之类的计量字段）
_SENSITIVE_KEYS = {
    "api_key", "apikey", "secret", "password", "passwd", "token",
    "access_token", "refresh_token", "authorization", "cookie", "jwt",
    "sign", "x_sign", "internal_call_public_key_file", "langsmith_api_key",
}

# token 计量字段属于可观测指标，不是密钥，必须保留
_METRIC_KEYS = {
    "input_tokens", "output_tokens", "total_tokens",
    "input_token_details", "output_token_details",
    "cache_read", "reasoning", "prompt_tokens", "completion_tokens",
    "prompt_cache_hit_tokens", "prompt_cache_miss_tokens", "reasoning_tokens",
}


def _is_sensitive(key: str) -> bool:
    k = str(key).lower()
    if k in _METRIC_KEYS:
        return False
    return k in _SENSITIVE_KEYS

# 形如 sk-xxxx / lsv2_sk_xxxx 的密钥字面量，兜底再脱敏一次
_SECRET_PATTERN = re.compile(r"\b(sk-[A-Za-z0-9_\-]{8,}|lsv2_(sk|pt)_[A-Za-z0-9_\-]{8,})")


def _redact_value(v: Any) -> Any:
    if isinstance(v, str):
        return _SECRET_PATTERN.sub("***", v)
    return v


def _redact_processor(_logger: Any, _method: str, event_dict: MutableMapping[str, Any]) -> MutableMapping[str, Any]:
    """structlog processor：递归脱敏敏感字段。"""
    def walk(obj: Any, depth: int = 0) -> Any:
        if depth > 6:
            return obj
        if isinstance(obj, dict):
            out: Dict[str, Any] = {}
            for k, v in obj.items():
                if _is_sensitive(k):
                    out[k] = "***"
                else:
                    out[k] = walk(v, depth + 1)
            return out
        if isinstance(obj, (list, tuple)):
            return [walk(i, depth + 1) for i in obj]
        return _redact_value(obj)

    for k in list(event_dict.keys()):
        if k.startswith("_"):
            continue
        if _is_sensitive(k):
            event_dict[k] = "***"
        else:
            event_dict[k] = walk(event_dict[k])
    return event_dict


def configure_logging(level: str = "INFO", json_output: bool = True) -> None:
    """初始化 structlog。在应用启动时调用一次。"""
    logging.basicConfig(
        format="%(message)s",
        stream=sys.stdout,
        level=getattr(logging, level.upper(), logging.INFO),
    )
    # 让第三方库（httpx/openai等）的日志也走统一格式，但降低噪音
    for noisy in ("httpx", "httpcore", "openai", "urllib3"):
        logging.getLogger(noisy).setLevel(logging.WARNING)

    renderer = (
        structlog.processors.JSONRenderer(ensure_ascii=False)
        if json_output
        else structlog.dev.ConsoleRenderer(colors=False)
    )

    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.add_log_level,
            structlog.processors.TimeStamper(fmt="iso", utc=False),
            _redact_processor,
            renderer,
        ],
        wrapper_class=structlog.make_filtering_bound_logger(
            getattr(logging, level.upper(), logging.INFO)
        ),
        logger_factory=structlog.PrintLoggerFactory(),
        cache_logger_on_first_use=True,
    )


def get_logger(name: str = "ai-agent"):
    return structlog.get_logger(name)
