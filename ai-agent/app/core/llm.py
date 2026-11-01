"""LLM 工厂：DeepSeek 官方 API（弃用 OneAPI 网关）。

实测结论（详见 UPGRADE_PLAN.md 第零节）：
- deepseek-flash = DeepSeek-V4.1-Flash，支持原生 tool calling 与流式
- 属于推理模型，会返回 reasoning_content，需单独走 thinking 事件
"""

import os
from typing import Optional

from langchain_openai import ChatOpenAI

from app.core.logging import get_logger
from app.core.reasoning import patch_reasoning_support
from app.core.settings import Settings

logger = get_logger(__name__)


def _apply_langsmith_env(settings: Settings) -> None:
    """把 LangSmith 配置注入环境变量。

    LangChain/LangGraph 通过读取环境变量来决定是否上报 trace，
    因此这里在创建模型之前统一设置，避免调用方遗漏。
    注意：异常的 trace 上报不应影响业务，LangChain 本身是异步非阻塞上报。
    """
    if not settings.langsmith_enabled:
        os.environ["LANGSMITH_TRACING"] = "false"
        return
    os.environ["LANGSMITH_TRACING"] = "true"
    os.environ["LANGSMITH_API_KEY"] = settings.langsmith_api_key.get_secret_value()
    os.environ["LANGSMITH_PROJECT"] = settings.langsmith_project
    if settings.langsmith_endpoint:
        os.environ["LANGSMITH_ENDPOINT"] = settings.langsmith_endpoint
    logger.info(
        "langsmith_enabled",
        project=settings.langsmith_project,
        endpoint=settings.langsmith_endpoint,
    )


def build_llm(settings: Settings, *, streaming: bool = False, temperature: Optional[float] = None) -> ChatOpenAI:
    """构建 ChatOpenAI 实例（DeepSeek 兼容 OpenAI 协议）。"""
    _apply_langsmith_env(settings)
    # deepseek-flash 是推理模型，需保留 reasoning_content 才能做思考中提示
    patch_reasoning_support()
    llm = ChatOpenAI(
        model=settings.deepseek_model,
        api_key=settings.deepseek_api_key.get_secret_value(),
        base_url=settings.deepseek_api_base,
        temperature=settings.llm_temperature if temperature is None else temperature,
        timeout=settings.llm_timeout,
        max_retries=settings.llm_max_retries,
        max_tokens=settings.llm_max_tokens,
        streaming=streaming,
    )
    logger.info(
        "llm_built",
        model=settings.deepseek_model,
        base_url=settings.deepseek_api_base,
        streaming=streaming,
    )
    return llm