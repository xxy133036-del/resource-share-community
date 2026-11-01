"""pytest 公共配置与夹具。"""

import asyncio
import os
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

# 测试期间关闭 LangSmith 上报，避免污染真实项目
os.environ.setdefault("LANGSMITH_TRACING", "false")


@pytest.fixture(scope="session")
def settings():
    from app.core.settings import get_settings

    return get_settings()


@pytest.fixture(scope="session")
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


def pytest_configure(config):
    config.addinivalue_line("markers", "integration: 需要真实 MySQL/Redis/LLM 的测试")