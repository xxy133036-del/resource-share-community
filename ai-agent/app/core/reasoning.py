"""推理模型的 reasoning_content 提取。

背景（实测发现）：
deepseek-flash 是推理模型，思考过程放在 delta.reasoning_content 中。
当前版本的 langchain-openai 会丢弃该字段，导致：
  1) 流式下用户看不到任何"思考中"反馈，正文 token 事件也可能为空；
  2) 思考 token 会消耗 max_tokens 预算。

这里在 langchain-openai 的模块级转换函数上打补丁，
把 reasoning_content 转存进 additional_kwargs，供上层提取。
"""

import logging
from typing import Any

from langchain_openai.chat_models import base as _base

logger = logging.getLogger(__name__)

_PATCH_FLAG = "_reasoning_content_patched"


def _read_reasoning(delta: Any) -> str:
    """从 OpenAI 原始 delta（dict，兼容对象）中取出 reasoning_content。"""
    try:
        if isinstance(delta, dict):
            value = delta.get("reasoning_content")
        else:
            value = getattr(delta, "reasoning_content", None)
        return value if isinstance(value, str) else ""
    except Exception:  # noqa: BLE001
        return ""


def patch_reasoning_support() -> bool:
    """给 langchain-openai 打补丁以保留 reasoning_content。幂等，失败不致命。"""
    if getattr(_base, _PATCH_FLAG, False):
        return True

    original = getattr(_base, "_convert_delta_to_message_chunk", None)
    if original is None:
        logger.warning("reasoning patch skipped: target function not found")
        return False

    def patched(_dict, default_class):  # noqa: ANN001
        message_chunk = original(_dict, default_class)
        reasoning = _read_reasoning(_dict)
        if reasoning:
            try:
                kwargs = dict(getattr(message_chunk, "additional_kwargs", None) or {})
                kwargs["reasoning_content"] = reasoning
                message_chunk.additional_kwargs = kwargs
            except Exception:  # noqa: BLE001
                pass
        return message_chunk

    _base._convert_delta_to_message_chunk = patched
    setattr(_base, _PATCH_FLAG, True)
    logger.info("reasoning patch applied")
    return True


def extract_reasoning(chunk: Any) -> str:
    """从 message chunk 中提取 reasoning_content。"""
    try:
        kwargs = getattr(chunk, "additional_kwargs", None) or {}
        value = kwargs.get("reasoning_content")
        return value if isinstance(value, str) else ""
    except Exception:  # noqa: BLE001
        return ""