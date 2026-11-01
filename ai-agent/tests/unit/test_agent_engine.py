"""Agent 层测试：验证工具调用、兜底与事件流结构。

使用假模型，不消耗真实 token，可离线回归。
注意：graph.py 在导入时就绑定了 build_tools，因此必须 patch
app.agent.graph.build_tools，而不是 app.tools.registry.build_tools。
"""

import pytest
from langchain_core.messages import AIMessage

from app.agent import graph as graph_mod
from app.agent.graph import FALLBACK_TEXT, MAX_ITER_FALLBACK, AgentEngine


class _FakeTool:
    def __init__(self, name, handler):
        self.name = name
        self.args = {}
        self._handler = handler

    async def ainvoke(self, args, config=None):
        return await self._handler(args, config)


def _factory(name, handler):
    """返回一个可被 monkeypatch 的 build_tools 替身。"""
    return lambda user_id: [_FakeTool(name, handler)]


class _FakeLLM:
    """按预设脚本依次返回响应，模拟模型决策。"""

    def __init__(self, script):
        self._script = list(script)

    def bind_tools(self, tools):
        return self

    async def ainvoke(self, messages, config=None):
        if self._script:
            return self._script.pop(0)
        return AIMessage(content="默认回答")


@pytest.mark.asyncio
async def test_direct_answer_without_tool(settings):
    """模型直接作答时，不应调用任何工具。"""
    fake = _FakeLLM([AIMessage(content="你好，我是小弘～")])
    eng = AgentEngine(settings, llm_factory=lambda **kw: fake)
    result = await eng.run("你好", [], "15")
    assert result["content"] == "你好，我是小弘～"
    assert result["tool_calls"] == []


@pytest.mark.asyncio
async def test_tool_then_answer(settings, monkeypatch):
    """模型先请求工具，拿到结果后再作答。"""
    tool_call = AIMessage(
        content="",
        tool_calls=[{"name": "get_user_profile", "args": {}, "id": "call_1"}],
    )
    fake = _FakeLLM([tool_call, AIMessage(content="你的积分是 51 分")])
    eng = AgentEngine(settings, llm_factory=lambda **kw: fake)

    called = {}

    async def handler(args, config=None):
        called["args"] = args
        return {"status": "success", "profile": {"id": 15, "points": 51}}

    monkeypatch.setattr(graph_mod, "build_tools", _factory("get_user_profile", handler))

    result = await eng.run("我的积分", [], "15")
    assert result["content"] == "你的积分是 51 分"
    assert called["args"] == {}
    assert [c["name"] for c in result["tool_calls"]] == ["get_user_profile"]


@pytest.mark.asyncio
async def test_tool_failure_is_fed_back(settings, monkeypatch):
    """工具抛异常时必须把错误回喂给模型，而不是让请求直接 500。"""
    tool_call = AIMessage(
        content="", tool_calls=[{"name": "get_user_profile", "args": {}, "id": "call_1"}]
    )
    fake = _FakeLLM([tool_call, AIMessage(content="查询失败了，请稍后再试")])
    eng = AgentEngine(settings, llm_factory=lambda **kw: fake)

    async def handler(args, config=None):
        raise RuntimeError("db down")

    monkeypatch.setattr(graph_mod, "build_tools", _factory("get_user_profile", handler))
    result = await eng.run("我的积分", [], "15")
    assert "失败" in result["content"] or result["content"]


@pytest.mark.asyncio
async def test_iteration_limit_falls_back(settings, monkeypatch):
    """模型无限请求工具时必须被截断并兜底，不能死循环。"""

    class _LoopLLM:
        def bind_tools(self, tools):
            return self

        async def ainvoke(self, messages, config=None):
            return AIMessage(
                content="", tool_calls=[{"name": "get_user_profile", "args": {}, "id": "call_x"}]
            )

    eng = AgentEngine(settings, llm_factory=lambda **kw: _LoopLLM())

    async def handler(args, config=None):
        return {"status": "success"}

    monkeypatch.setattr(graph_mod, "build_tools", _factory("get_user_profile", handler))
    result = await eng.run("循环测试", [], "15")
    assert result["content"] in (MAX_ITER_FALLBACK, FALLBACK_TEXT)


@pytest.mark.asyncio
async def test_stream_emits_status_and_done(settings, monkeypatch):
    """流式必须产出 status/done 事件，且 done 带 message_id。"""
    fake = _FakeLLM([AIMessage(content="你好呀")])
    eng = AgentEngine(settings, llm_factory=lambda **kw: fake)
    events = [ev async for ev in eng.stream("你好", [], "15")]
    types = [e["type"] for e in events]
    assert "status" in types
    assert types[-1] == "done"
    assert events[-1]["message_id"].startswith("msg_")

# ---------------- 旁白（preamble）泄漏回归 ----------------

from app.agent.graph import _StreamedText


def test_streamed_text_drops_preamble_before_tool_call():
    """工具调用前的旁白必须被丢弃，答案只取最后一次无工具调用的生成。"""
    seg = _StreamedText()
    seg.feed("I'll search for Java")
    seg.feed("-related resources on the platform.")
    assert seg.end_segment(has_tool_calls=True) is None
    seg.feed("平台上")
    seg.feed("有 3 个资源")
    seg.end_segment(has_tool_calls=False)
    assert seg.answer == "平台上有 3 个资源"


def test_streamed_text_keeps_content_when_no_tool_call():
    """不涉及工具调用时，正文必须完整保留，不能被误当作旁白丢弃。"""
    seg = _StreamedText()
    assert seg.feed("你好") is None
    assert seg.feed("，我是小弘") is None
    assert seg.end_segment(has_tool_calls=False) == "你好，我是小弘"
    assert seg.answer == "你好，我是小弘"


def test_streamed_text_streams_long_first_segment_immediately():
    """首段过长时立即下发，保证正常回答的流式体验不受影响。"""
    seg = _StreamedText()
    first = seg.feed("x" * 100)
    assert first == "x" * 100
    assert seg.end_segment(has_tool_calls=False) is None
    assert seg.answer == "x" * 100


@pytest.mark.asyncio
async def test_stream_answer_excludes_tool_preamble(settings, monkeypatch):
    """端到端流式：模型输出旁白并调用工具时，done 不应包含旁白。"""

    class _NarratingLLM:
        def __init__(self):
            self._n = 0

        def bind_tools(self, tools):
            return self

        async def ainvoke(self, messages, config=None):
            self._n += 1
            if self._n == 1:
                return AIMessage(
                    content="I'll search for Java resources on the platform.",
                    tool_calls=[{"name": "search_resources", "args": {"keyword": "Java"}, "id": "c1"}],
                )
            return AIMessage(content="平台上找到 3 个 Java 资源")

    eng = AgentEngine(settings, llm_factory=lambda **kw: _NarratingLLM())

    async def handler(args, config=None):
        return {"status": "success", "total": 3, "resources": []}

    monkeypatch.setattr(graph_mod, "build_tools", _factory("search_resources", handler))
    events = [ev async for ev in eng.stream("找 Java 资料", [], "15")]
    done = events[-1]
    assert done["type"] == "done"
    assert "平台上找到 3 个 Java 资源" in done["text"]
    assert "I'll search" not in done["text"]
    assert "I'll search" not in "".join(
        e.get("text", "") for e in events if e["type"] == "token"
    )

@pytest.mark.asyncio
async def test_run_retries_transient_upstream_error(settings, monkeypatch):
    """上游偶发连接抖动时，非流式必须重试，而不是直接把 503 抛给用户。"""

    class _FlakyLLM:
        def __init__(self):
            self.calls = 0

        def bind_tools(self, tools):
            return self

        async def ainvoke(self, messages, config=None):
            self.calls += 1
            if self.calls < 2:
                raise ConnectionError("Connection error.")
            return AIMessage(content="重试后正常回答")

    fake = _FlakyLLM()
    eng = AgentEngine(settings, llm_factory=lambda **kw: fake)
    result = await eng.run("你好", [], "15")
    assert result["content"] == "重试后正常回答"
    assert fake.calls == 2


@pytest.mark.asyncio
async def test_run_gives_up_after_max_attempts(settings, monkeypatch):
    """持续失败时应在有限次重试后抛出，不能无限重试。"""
    from app.core.exceptions import UpstreamError

    class _AlwaysDown:
        def __init__(self):
            self.calls = 0

        def bind_tools(self, tools):
            return self

        async def ainvoke(self, messages, config=None):
            self.calls += 1
            raise ConnectionError("Connection error.")

    fake = _AlwaysDown()
    eng = AgentEngine(settings, llm_factory=lambda **kw: fake)
    with pytest.raises(UpstreamError):
        await eng.run("你好", [], "15")
    assert fake.calls == max(int(settings.llm_invoke_max_attempts), 1)
