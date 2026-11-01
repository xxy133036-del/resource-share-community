"""对话用例编排：统一非流式与流式，负责上下文装配、持久化与限流。

持久化模型（参照成熟 Agent 的会话设计）：
- 事件日志落 MySQL 的 ai_session_event，只追加、不覆盖，是唯一事实来源
- ai_session 存索引与派生状态（标题/轮次/用量/摘要）
- 流式场景下，SSE token 直接透传给前端，**不逐 token 落库**；
  一轮结束再整轮批量写入，避免半截轮次污染日志
"""

import time
import uuid
import hashlib
from typing import Any, AsyncIterator, Dict, List, Optional

from app.agent.graph import AgentEngine
from app.core.exceptions import RateLimitedError, ValidationError
from app.core.logging import get_logger
from app.core.settings import Settings
from app.infra.redis import get_redis
from app.infra.session_store import SessionStore, new_turn_id

logger = get_logger(__name__)


class ChatService:
    def __init__(self, settings: Settings, engine: AgentEngine, sessions: SessionStore):
        self._settings = settings
        self._engine = engine
        self._sessions = sessions

    # ---------------- 内部工具 ----------------
    @staticmethod
    def _new_conversation_id() -> str:
        return f"conv_{uuid.uuid4().hex[:16]}"

    async def _check_rate_limit(self, user_id: Optional[str], conversation_id: str) -> None:
        """按用户和会话双令牌桶限流，避免突发与新建会话绕过限额。"""
        redis = get_redis()
        owner = hashlib.sha256((user_id or "anon").encode("utf-8")).hexdigest()
        conversation = hashlib.sha256(conversation_id.encode("utf-8")).hexdigest()
        result = await redis.consume_rate_limits(
            (
                # 用户桶：容量 60，每秒补 1 个令牌（长期平均 60 次/分钟）。
                (f"agent:rate:{{{owner}}}:user", 60, 1.0, 1.0),
                # 会话桶：容量 20，每 3 秒补 1 个令牌（长期平均 20 次/分钟）。
                (f"agent:rate:{{{owner}}}:conversation:{conversation}", 20, 1 / 3, 1.0),
            )
        )
        if result is not None and not result[0]:
            logger.warning(
                "rate_limited",
                user_id=user_id,
                conversation_id=conversation_id,
                retry_after_ms=result[1],
            )
            raise RateLimitedError()

    @staticmethod
    def _normalize_question(question: str) -> str:
        question = (question or "").strip()
        if not question:
            raise ValidationError("问题不能为空")
        if len(question) > 1000:
            raise ValidationError("问题过长，请精简后再提问")
        return question

    async def _prepare(self, question: str, user_id: Optional[str], conversation_id: Optional[str]):
        """限流 + 建会话 + 装配上下文，返回 (question, conv_id, context)。"""
        question = self._normalize_question(question)
        conv_id = (conversation_id or "").strip() or self._new_conversation_id()
        await self._check_rate_limit(user_id, conv_id)

        # 无签名上下文（本地调试）时退化为匿名，仍然落库，保证可回溯
        owner = str(user_id) if user_id else "anonymous"
        await self._sessions.ensure_session(conv_id, owner)
        context = await self._sessions.build_context(conv_id, owner)
        return question, conv_id, context

    # ---------------- 对外用例 ----------------
    async def chat(
        self,
        question: str,
        user_id: Optional[str],
        conversation_id: Optional[str],
    ) -> Dict[str, Any]:
        """非流式对话。"""
        question, conv_id, context = await self._prepare(question, user_id, conversation_id)
        turn_id = new_turn_id()

        started = time.time()
        result = await self._engine.run(
            question, context.messages, user_id, summary=context.summary
        )
        elapsed_ms = int((time.time() - started) * 1000)

        await self._sessions.finalize_turn(
            conv_id,
            str(user_id) if user_id else "anonymous",
            turn_id=turn_id,
            question=question,
            answer=result["content"],
            tool_calls=result.get("tool_calls") or [],
            usage=result.get("usage") or {},
            latency_ms=elapsed_ms,
        )
        if self._settings.session_compaction_enabled:
            try:
                await self._sessions.maybe_compact(
                    conv_id, str(user_id) if user_id else "anonymous"
                )
            except Exception as exc:  # noqa: BLE001
                # 压缩失败不能影响用户回答，下一轮会重试
                logger.warning("compaction_failed", conversation_id=conv_id, error=str(exc))

        logger.info(
            "chat_completed",
            conversation_id=conv_id,
            user_id=user_id,
            latency_ms=elapsed_ms,
            tool_calls=[c.get("name") for c in result.get("tool_calls", [])],
            usage=result.get("usage"),
        )
        return {
            "content": result["content"],
            "conversation_id": conv_id,
            "usage": result.get("usage") or {},
            "message_id": f"msg_{uuid.uuid4().hex[:16]}",
        }

    async def stream_chat(
        self,
        question: str,
        user_id: Optional[str],
        conversation_id: Optional[str],
    ) -> AsyncIterator[Dict[str, Any]]:
        """流式对话：产出 SSE 事件，结束后整轮落库。"""
        question, conv_id, context = await self._prepare(question, user_id, conversation_id)
        turn_id = new_turn_id()

        started = time.time()
        answer = ""
        usage: Dict[str, Any] = {}
        tool_calls: List[Dict[str, Any]] = []
        failed = False

        try:
            async for event in self._engine.stream(
                question, context.messages, user_id, summary=context.summary
            ):
                if event.get("type") == "done":
                    answer = event.get("text") or ""
                    usage = event.get("usage") or {}
                    tool_calls = event.get("tool_calls") or []
                yield event
        except Exception as exc:  # noqa: BLE001
            # 失败也要留痕：日志里记 error 事件，否则这轮提问会凭空消失
            failed = True
            logger.error("stream_chat_failed", conversation_id=conv_id, user_id=user_id, error=str(exc))
            await self._sessions.finalize_turn(
                conv_id,
                str(user_id) if user_id else "anonymous",
                turn_id=turn_id,
                question=question,
                answer="",
                error=str(exc)[:500],
                latency_ms=int((time.time() - started) * 1000),
            )
            raise

        if not failed:
            await self._sessions.finalize_turn(
                conv_id,
                str(user_id) if user_id else "anonymous",
                turn_id=turn_id,
                question=question,
                answer=answer,
                tool_calls=tool_calls,
                usage=usage,
                latency_ms=int((time.time() - started) * 1000),
            )
            if self._settings.session_compaction_enabled:
                try:
                    await self._sessions.maybe_compact(
                        conv_id, str(user_id) if user_id else "anonymous"
                    )
                except Exception as exc:  # noqa: BLE001
                    # 压缩失败不能影响用户回答，下一轮会重试
                    logger.warning("compaction_failed", conversation_id=conv_id, error=str(exc))

        logger.info(
            "stream_chat_completed",
            conversation_id=conv_id,
            user_id=user_id,
            latency_ms=int((time.time() - started) * 1000),
            tool_calls=[c.get("name") for c in tool_calls],
            usage=usage,
        )
