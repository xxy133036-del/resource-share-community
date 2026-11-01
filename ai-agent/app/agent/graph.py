"""LangGraph Agent 内核。

替代旧实现中"正则抠 JSON 再手工 append messages"的伪 Tool Calling：
- 使用模型原生 tool_calls（LangChain bind_tools 自动生成 JSON Schema）
- 用 LangGraph StateGraph 显式建模：agent -> tools -> agent -> 收尾
- 迭代上限可配，超限走兜底话术，不会无限循环
- 全程 async，配合异步 MySQL/Redis，慢查询不再阻塞事件循环
"""

import asyncio
import json
import uuid
from typing import Annotated, Any, AsyncIterator, Dict, List, Optional, Sequence, TypedDict

from langchain_core.messages import (
    AIMessage,
    BaseMessage,
    HumanMessage,
    SystemMessage,
    ToolMessage,
)
from langgraph.graph import END, StateGraph
from langchain_core.runnables import RunnableConfig
from langgraph.graph.message import add_messages

from app.agent.prompts import build_system_prompt
from app.core.exceptions import UpstreamError
from app.core.logging import get_logger
from app.core.reasoning import extract_reasoning
from app.core.settings import Settings
from app.tools.registry import build_tools

logger = get_logger(__name__)

FALLBACK_TEXT = "抱歉，我这边暂时没能处理这个问题，你可以换个说法再问我一次～"
MAX_ITER_FALLBACK = "抱歉，这个问题有点复杂，我没能整理出答案。你可以说得更具体一些，比如想找什么类型的资料～"


class AgentState(TypedDict):
    messages: Annotated[Sequence[BaseMessage], add_messages]


class _StreamedText:
    """按"单次 LLM 生成"分段管理流式正文。

    实测背景：deepseek-flash 是推理模型，偶尔会把"调用工具前的旁白"写进
    正文 content（而不是 reasoning_content），例如先输出
    "I'll search for Java-related resources on the platform." 再发起工具调用。
    旁白与最终回答分属两次不同的 LLM 生成，若把所有正文分片简单累加，
    旁白就会被当成答案开头（实测约 1/5 概率）。

    处理策略：
      1. 只有"不含工具调用的最后一次生成"才是给用户的最终回答；
      2. 第一次生成的一段短正文先缓冲，若该次生成最后带工具调用（即旁白），
         直接丢弃，避免英文旁白闪现在前端；否则原样补发，不丢内容。
    由于真实场景下工具调用轮次通常没有正文，缓冲几乎不会给用户带来可感知延迟。
    """

    # 首段正文的缓冲阈值：超过它即认为不是旁白，立即开始正常流式
    PREAMBLE_PROBE_CHARS = 80

    def __init__(self) -> None:
        self._completed: List[tuple] = []      # [(text, has_tool_calls), ...]
        self._current: List[str] = []
        self._held: List[str] = []
        self._released = False
        self._first = True
        self._held_len = 0

    def feed(self, text: str) -> Optional[str]:
        """喂入一个正文分片，返回需立即下发的文本（None 表示先不下发）。"""
        self._current.append(text)
        if not self._first or self._released:
            return text
        self._held.append(text)
        self._held_len += len(text)
        if self._held_len > self.PREAMBLE_PROBE_CHARS:
            self._released = True
            out = "".join(self._held)
            self._held.clear()
            return out
        return None

    def end_segment(self, has_tool_calls: bool) -> Optional[str]:
        """结束一次生成，返回需补发的缓冲文本（None 表示无）。"""
        text = "".join(self._current)
        self._completed.append((text, has_tool_calls))
        self._current.clear()
        out: Optional[str] = None
        if self._first:
            self._first = False
            if has_tool_calls:
                self._held.clear()                    # 判定为旁白，丢弃
            else:
                out = "".join(self._held) or None     # 直接作答，原样补发
            self._held.clear()
            self._released = True
        return out

    @property
    def answer(self) -> str:
        """最终回答：不含工具调用的最后一次生成。"""
        for text, has_tools in reversed(self._completed):
            if not has_tools and text.strip():
                return text.strip()
        return ""


class AgentEngine:
    """封装 LangGraph 图的构建与执行。"""

    def __init__(self, settings: Settings, llm_factory):
        self._settings = settings
        self._llm_factory = llm_factory
        self._max_iterations = settings.agent_max_iterations

    def _build_graph(self, user_id: Optional[str], *, streaming: bool = False):
        tools = build_tools(user_id)
        tools_by_name = {t.name: t for t in tools}
        llm = self._llm_factory(streaming=streaming).bind_tools(tools)

        async def agent_node(state: AgentState, config: RunnableConfig) -> Dict[str, Any]:
            # 必须透传 config，否则 LangSmith trace 与 astream_events 分片都会丢失
            response = await llm.ainvoke(list(state["messages"]), config)
            return {"messages": [response]}

        async def tool_node(state: AgentState, config: RunnableConfig) -> Dict[str, Any]:
            """执行模型请求的工具调用。

            自己实现而不用 ToolNode，是为了把工具调用过程暴露出来（用于 SSE/日志），
            并统一异常兜底——工具失败也要把错误作为 ToolMessage 回喂给模型，
            让模型有机会换策略，而不是直接抛 500。
            """
            last = state["messages"][-1]
            outputs: List[ToolMessage] = []
            for call in getattr(last, "tool_calls", []) or []:
                name = call.get("name")
                args = call.get("args") or {}
                tool = tools_by_name.get(name)
                if tool is None:
                    content = json.dumps({"error": f"未知工具 {name}"}, ensure_ascii=False)
                else:
                    try:
                        content = await tool.ainvoke(args, config)
                        content = json.dumps(content, ensure_ascii=False) if not isinstance(content, str) else content
                    except Exception as exc:  # noqa: BLE001
                        logger.warning("tool_failed", tool=name, error=str(exc))
                        content = json.dumps({"error": f"工具执行失败：{exc}"}, ensure_ascii=False)
                outputs.append(ToolMessage(content=content, tool_call_id=call.get("id") or "", name=name))
            return {"messages": outputs}

        def should_continue(state: AgentState) -> str:
            last = state["messages"][-1]
            if isinstance(last, AIMessage) and getattr(last, "tool_calls", None):
                # 达到迭代上限则停止，交由收尾节点兜底
                tool_rounds = sum(
                    1 for m in state["messages"] if isinstance(m, ToolMessage)
                )
                if tool_rounds >= self._max_iterations:
                    logger.warning("agent_max_iterations_reached", rounds=tool_rounds)
                    return "give_up"
                return "tools"
            return END

        builder = StateGraph(AgentState)
        builder.add_node("agent", agent_node)
        builder.add_node("tools", tool_node)
        builder.add_node("give_up", lambda state: {"messages": [AIMessage(content=MAX_ITER_FALLBACK)]})
        builder.set_entry_point("agent")
        builder.add_conditional_edges("agent", should_continue, {"tools": "tools", "give_up": "give_up", END: END})
        builder.add_edge("tools", "agent")
        builder.add_edge("give_up", END)
        return builder.compile()

    def _initial_messages(
        self,
        question: str,
        history: List[Dict[str, str]],
        user_id: Optional[str],
        summary: str = "",
    ) -> List[BaseMessage]:
        messages: List[BaseMessage] = [
            SystemMessage(content=build_system_prompt(self._settings.knowledge_file, user_id=user_id))
        ]
        # 更早的轮次已被压缩成摘要，作为一条 system 消息置于历史之前，
        # 让模型保留背景又不至于撑爆上下文窗口
        if summary:
            messages.append(
                SystemMessage(content="## 早前对话摘要（更早的内容已压缩）\n\n" + summary)
            )
        for item in history:
            role, content = item.get("role"), item.get("content") or ""
            if not content:
                continue
            if role == "user":
                messages.append(HumanMessage(content=content))
            elif role == "assistant":
                messages.append(AIMessage(content=content))
            elif role == "system":
                messages.append(SystemMessage(content=content))
        messages.append(HumanMessage(content=question))
        return messages

    async def run(
        self,
        question: str,
        history: List[Dict[str, str]],
        user_id: Optional[str],
        summary: str = "",
    ) -> Dict[str, Any]:
        """非流式执行，返回 {content, tool_calls, usage}。"""
        graph = self._build_graph(user_id)
        messages = self._initial_messages(question, history, user_id, summary)

        # 上游偶发连接抖动（实测非流式约 1/6 概率命中），这里重试与流式路径对齐；
        # 非流式没有"已产出内容"的问题，重试对用户完全无感。
        attempts = max(int(self._settings.llm_invoke_max_attempts), 1)
        result = None
        last_error: Optional[Exception] = None
        for attempt in range(1, attempts + 1):
            try:
                result = await graph.ainvoke({"messages": messages})
                break
            except Exception as exc:  # noqa: BLE001
                last_error = exc
                if attempt >= attempts:
                    logger.error("agent_run_failed", error=str(exc), attempt=attempt)
                    raise UpstreamError("模型服务暂时不可用，请稍后再试") from exc
                logger.warning("agent_run_retry", error=str(exc), attempt=attempt)
                await asyncio.sleep(0.4 * attempt)
        if result is None:  # 理论上不可达，防御性兜底
            raise UpstreamError("模型服务暂时不可用，请稍后再试") from last_error

        out_messages = result["messages"]
        answer = ""
        for m in reversed(out_messages):
            if isinstance(m, AIMessage) and not getattr(m, "tool_calls", None) and m.content:
                answer = m.content if isinstance(m.content, str) else str(m.content)
                break
        if not answer:
            answer = FALLBACK_TEXT

        tool_calls = []
        for m in out_messages:
            for call in getattr(m, "tool_calls", []) or []:
                tool_calls.append({"name": call.get("name"), "args": call.get("args")})

        usage = {}
        for m in reversed(out_messages):
            meta = getattr(m, "usage_metadata", None)
            if meta:
                usage = dict(meta)
                break

        return {"content": answer, "tool_calls": tool_calls, "usage": usage, "messages": out_messages}

    async def stream(
        self,
        question: str,
        history: List[Dict[str, str]],
        user_id: Optional[str],
        summary: str = "",
    ) -> AsyncIterator[Dict[str, Any]]:
        """流式执行，逐事件产出。

        事件类型与 schemas.SSEEvent 对应：
        status / thinking / tool / token / done
        """
        graph = self._build_graph(user_id, streaming=True)
        messages = self._initial_messages(question, history, user_id, summary)
        seen_tool_calls: List[Dict[str, Any]] = []
        usage: Dict[str, Any] = {}
        streamed = _StreamedText()

        yield {"type": "status", "text": "正在理解你的问题…"}

        # 上游偶发连接抖动。只要还没向前端吐出任何内容，重试对用户完全无感；
        # 一旦已产出 token 就不能重试，否则前端会看到重复内容。
        emitted = False
        attempts = max(int(self._settings.llm_stream_max_attempts), 1)
        for attempt in range(1, attempts + 1):
            try:
                async for event in graph.astream_events({"messages": messages}, version="v2"):
                    async for out in self._translate_event(event, streamed, seen_tool_calls):
                        if out.get("type") == "usage":
                            usage = out["usage"]
                            continue
                        if out.get("type") in ("token", "thinking", "tool"):
                            emitted = True
                        yield out
                break
            except Exception as exc:  # noqa: BLE001
                if emitted or attempt >= attempts:
                    logger.error("agent_stream_failed", error=str(exc), attempt=attempt)
                    raise UpstreamError("模型服务暂时不可用，请稍后再试") from exc
                logger.warning("agent_stream_retry", error=str(exc), attempt=attempt)
                await asyncio.sleep(0.4 * attempt)

        text = streamed.answer
        if not text:
            # 兜底：未收到任何正文分片时退化为非流式调用，
            # 避免因模型/库版本差异导致"流式不可用"。
            logger.warning("stream_no_chunks_fallback_to_invoke")
            fallback = await self.run(question, history, user_id, summary)
            text = fallback["content"]
            usage = fallback.get("usage") or usage
            yield {"type": "token", "text": text}

        yield {
            "type": "done",
            "text": text,
            "usage": usage,
            "tool_calls": seen_tool_calls,
            "message_id": f"msg_{uuid.uuid4().hex[:16]}",
        }

    async def _translate_event(
        self,
        event: Dict[str, Any],
        streamed: "_StreamedText",
        seen_tool_calls: List[Dict[str, Any]],
    ) -> AsyncIterator[Dict[str, Any]]:
        """把 LangGraph 的底层事件翻译成对外的 SSE 事件。

        抽成独立方法，是为了让上面重试循环里只剩"取事件 + 重试"两件事，
        翻译规则变化时不影响重试与兜底逻辑。
        """
        kind = event.get("event")
        name = event.get("name")

        if kind == "on_chat_model_stream":
            chunk = event.get("data", {}).get("chunk")
            if chunk is None:
                return
            # 推理模型的思考过程
            reasoning = extract_reasoning(chunk)
            if reasoning:
                yield {"type": "thinking", "text": reasoning}
            content = getattr(chunk, "content", None)
            if content:
                text = content if isinstance(content, str) else str(content)
                visible = streamed.feed(text)
                if visible:
                    yield {"type": "token", "text": visible}
            meta = getattr(chunk, "usage_metadata", None)
            if meta:
                yield {"type": "usage", "usage": dict(meta)}

        elif kind == "on_chat_model_end":
            # 一次 LLM 生成结束：据此判定该段是"旁白"还是"最终回答"
            output = event.get("data", {}).get("output")
            tool_calls = getattr(output, "tool_calls", None) or []
            pending = streamed.end_segment(bool(tool_calls))
            if pending:
                yield {"type": "token", "text": pending}

        elif kind == "on_tool_start":
            payload = event.get("data", {}).get("input")
            yield {"type": "status", "text": "正在查询平台数据…"}
            yield {"type": "tool", "name": name, "args": payload}
            seen_tool_calls.append({"name": name, "args": payload})
