"""会话存储：追加写事件日志 + 压缩摘要。

## 为什么改成这个模型

原实现把整段会话历史当成一个 JSON 数组塞进 Redis 单键，每轮全量覆盖：

1. **重启即丢**：Redis 只是缓存，没有持久化保证，用户会话说没就没。
2. **无审计**：只有最终问答，工具调用、用量、失败原因全被丢掉，出问题无法回溯。
3. **截断会撕裂轮次**：超长时直接 `history[-N:]`，可能从一轮问答的中间切开，
   模型会看到"只有回答没有提问"的残缺上下文。
4. **无跨会话能力**：无法列出用户的历史会话，也无法归档。

现在参照成熟 Agent 的会话模型（append-only event log + 索引 + 压缩摘要）：

- `ai_session`       ：会话索引与派生状态（标题、轮次、摘要、发号器）
- `ai_session_event` ：**唯一事实来源**，只追加、不更新、不删除
- 模型上下文         ：摘要 + 压缩点之后的原始事件，按轮次边界裁剪

事件日志是唯一事实来源，任何派生状态（轮次、token、摘要）都可以由它重建。
"""

import json
import math
import uuid
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Sequence

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import NotFoundError, ValidationError
from app.core.logging import get_logger
from app.core.settings import Settings
from app.infra.mysql import Database, exec_all, select_all, select_one

logger = get_logger(__name__)

# 进入模型上下文的事件类型
MESSAGE_TYPES = ("user_message", "assistant_message")


@dataclass
class SessionContext:
    """装配好的模型上下文。

    summary 不为空时，表示 summary_upto_ordinal 之前的历史已被压缩，
    调用方应把 summary 作为一条 system 消息放在最前面。
    """

    summary: str = ""
    summary_upto_ordinal: int = 0
    messages: List[Dict[str, str]] = field(default_factory=list)
    turn_count: int = 0
    total_tokens: int = 0


class SessionStore:
    """会话与事件日志的读写入口。"""

    def __init__(self, settings: Settings, db: Database):
        self._settings = settings
        self._db = db

    # ==================== 会话生命周期 ====================

    async def ensure_session(
        self, session_id: str, user_id: str, *, title: Optional[str] = None
    ) -> Dict[str, Any]:
        """幂等地创建会话。已存在则直接返回，不做任何覆盖。

        注意：user_id 不同但 session_id 相同时**不会**改变归属，
        避免会话 ID 被猜到后被他人"接管"。
        """
        session_id = (session_id or "").strip()
        if not session_id:
            raise ValueError("session_id 不能为空")

        async with self._db.transaction() as session:
            existing = await select_one(
                session, "SELECT * FROM ai_session WHERE session_id = :sid", {"sid": session_id}
            )
            if existing:
                if str(existing.get("user_id")) != str(user_id):
                    raise NotFoundError("会话不存在")
                if existing.get("status") == "archived":
                    raise ValidationError("会话已归档，无法继续对话")
                return existing

            await exec_all(
                session,
                """
                INSERT INTO ai_session (session_id, user_id, title, status)
                VALUES (:sid, :uid, :title, 'active')
                """,
                {"sid": session_id, "uid": str(user_id), "title": title},
            )
            created = await select_one(
                session, "SELECT * FROM ai_session WHERE session_id = :sid", {"sid": session_id}
            )
            logger.info("session_created", session_id=session_id, user_id=user_id)
            return created or {}

    async def get_session(self, session_id: str, user_id: str) -> Optional[Dict[str, Any]]:
        """读取会话；只有归属者本人能读到，否则返回 None（等同不存在）。"""
        return await self._db.fetch_one(
            "SELECT * FROM ai_session WHERE session_id = :sid AND user_id = :uid",
            {"sid": session_id, "uid": str(user_id)},
        )

    async def list_sessions(
        self, user_id: str, *, limit: int = 20, offset: int = 0, include_archived: bool = False
    ) -> List[Dict[str, Any]]:
        """列出某个用户的会话，按最后活跃时间倒序。"""
        sql = [
            "SELECT session_id, title, status, turn_count, message_count, total_tokens,",
            "       created_at, updated_at",
            "FROM ai_session WHERE user_id = :uid",
        ]
        if not include_archived:
            sql.append("AND status = 'active'")
        sql.append("ORDER BY updated_at DESC, id DESC LIMIT :limit OFFSET :offset")
        return await self._db.fetch_all(
            " ".join(sql), {"uid": str(user_id), "limit": int(limit), "offset": int(offset)}
        )

    async def archive_session(self, session_id: str, user_id: str) -> bool:
        """归档会话。不删除任何事件，历史仍可回溯。"""
        async with self._db.transaction() as session:
            affected = await exec_all(
                session,
                "UPDATE ai_session SET status = 'archived' WHERE session_id = :sid AND user_id = :uid",
                {"sid": session_id, "uid": str(user_id)},
            )
        if affected:
            logger.info("session_archived", session_id=session_id, user_id=user_id)
        return bool(affected)

    async def rename_session(self, session_id: str, user_id: str, title: str) -> bool:
        async with self._db.transaction() as session:
            affected = await exec_all(
                session,
                "UPDATE ai_session SET title = :title WHERE session_id = :sid AND user_id = :uid",
                {"sid": session_id, "uid": str(user_id), "title": (title or "")[:255]},
            )
        return bool(affected)

    # ==================== 事件追加 ====================

    async def append_events(
        self,
        session_id: str,
        events: Sequence[Dict[str, Any]],
        *,
        turn_id: Optional[str] = None,
    ) -> None:
        """把一批事件原子地追加到日志。

        events 中每项形如：
            {"type": "user_message", "role": "user", "content": "...", "payload": {...}}

        序号由 ai_session.event_seq 在同一事务内分配，
        因此并发写入同一会话也不会串号或覆盖。
        """
        if not events:
            return

        async with self._db.transaction() as session:
            new_seq = await self._allocate_ordinals(session, session_id, len(events))
            if new_seq is None:
                logger.warning("append_events_session_missing", session_id=session_id)
                return
            start = new_seq - len(events) + 1

            rows = []
            for offset, event in enumerate(events):
                payload = event.get("payload")
                rows.append(
                    {
                        "sid": session_id,
                        "ordinal": start + offset,
                        "turn_id": event.get("turn_id") or turn_id,
                        "type": event["type"],
                        "role": event.get("role"),
                        "content": event.get("content"),
                        "payload": json.dumps(payload, ensure_ascii=False) if payload else None,
                    }
                )

            await session.execute(
                text(
                    """
                    INSERT INTO ai_session_event
                        (session_id, ordinal, turn_id, type, role, content, payload)
                    VALUES (:sid, :ordinal, :turn_id, :type, :role, :content, :payload)
                    """
                ),
                rows,
            )

    async def _allocate_ordinals(
        self,
        session: AsyncSession,
        session_id: str,
        count: int,
        *,
        user_id: Optional[str] = None,
    ) -> Optional[int]:
        """在事务内原子发号，返回发号后的新值（末位序号）。

        用 LAST_INSERT_ID(expr) 把新值带回当前连接，避免"先读后写"的竞态：
        两个并发请求各自拿到独占的号段，不会互相覆盖。
        """
        owner_filter = " AND user_id = :uid" if user_id is not None else ""
        params = {"n": int(count), "sid": session_id}
        if user_id is not None:
            params["uid"] = str(user_id)
        affected = await exec_all(
            session,
            "UPDATE ai_session SET event_seq = LAST_INSERT_ID(event_seq + :n) "
            "WHERE session_id = :sid" + owner_filter,
            params,
        )
        if not affected:
            return None
        row = await select_one(session, "SELECT LAST_INSERT_ID() AS seq")
        return int(row["seq"]) if row else None

    # ==================== 上下文装配 ====================

    async def build_context(
        self,
        session_id: str,
        user_id: str,
        *,
        max_messages: Optional[int] = None,
        max_tokens: Optional[int] = None,
    ) -> SessionContext:
        """按"摘要 + 压缩点之后的事件"装配模型上下文。

        与旧实现 `history[-N:]` 的关键区别：按**轮次边界**裁剪，
        不会出现只有回答没有提问的残缺上下文；裁剪预算以 token 为主、条数为兜底。
        """
        row = await self.get_session(session_id, user_id)
        if not row:
            return SessionContext()

        summary = row.get("summary") or ""
        upto = int(row.get("summary_upto_ordinal") or 0)
        msg_limit = int(max_messages or self._settings.session_max_context_messages)
        tok_limit = int(
            self._settings.session_max_context_tokens if max_tokens is None else max_tokens
        )

        events = await self._db.fetch_all(
            """
            SELECT ordinal, type, role, content, payload
            FROM ai_session_event
            WHERE session_id = :sid
              AND ordinal > :upto
              AND type IN ('user_message', 'assistant_message')
            ORDER BY ordinal ASC
            """,
            {"sid": session_id, "upto": upto},
        )

        messages = trim_to_budget(events, max_messages=msg_limit, max_tokens=tok_limit)
        logger.debug(
            "context_built",
            session_id=session_id,
            has_summary=bool(summary),
            replayed_messages=len(messages),
            token_budget=tok_limit,
        )
        return SessionContext(
            summary=summary,
            summary_upto_ordinal=upto,
            messages=messages,
            turn_count=int(row.get("turn_count") or 0),
            total_tokens=int(row.get("total_tokens") or 0),
        )

    async def get_events(self, session_id: str, user_id: str, *, limit: int = 200) -> List[Dict[str, Any]]:
        """读取完整事件日志（含工具调用），用于前端回放与问题排查。"""
        if not await self.get_session(session_id, user_id):
            return []
        rows = await self._db.fetch_all(
            """
            SELECT ordinal, turn_id, type, role, content, payload, created_at
            FROM ai_session_event WHERE session_id = :sid
            ORDER BY ordinal ASC LIMIT :limit
            """,
            {"sid": session_id, "limit": int(limit)},
        )
        for row in rows:
            if isinstance(row.get("payload"), str):
                try:
                    row["payload"] = json.loads(row["payload"])
                except (TypeError, ValueError):
                    row["payload"] = None
        return rows

    # ==================== 轮次收尾 ====================

    async def finalize_turn(
        self,
        session_id: str,
        user_id: str,
        *,
        turn_id: str,
        question: str,
        answer: str,
        tool_calls: Sequence[Dict[str, Any]] = (),
        usage: Optional[Dict[str, Any]] = None,
        latency_ms: int = 0,
        error: Optional[str] = None,
    ) -> None:
        """一轮问答结束后，把本轮产生的所有事件一次性落库。

        采用"整轮批量写"而不是"边流边写"：
        模型可能在流式中途失败，逐条写会留下半截轮次；
        整轮写入保证日志里只会出现完整轮次。
        """
        events: List[Dict[str, Any]] = [
            {"type": "user_message", "role": "user", "content": question, "turn_id": turn_id}
        ]
        for call in tool_calls:
            events.append(
                {
                    "type": "tool_call",
                    "role": "tool",
                    "content": None,
                    "payload": {"name": call.get("name"), "args": call.get("args")},
                    "turn_id": turn_id,
                }
            )
        if error:
            events.append(
                {"type": "error", "role": "system", "content": error, "turn_id": turn_id}
            )
        else:
            events.append(
                {"type": "assistant_message", "role": "assistant", "content": answer, "turn_id": turn_id}
            )
        if usage:
            events.append(
                {
                    "type": "usage",
                    "role": "system",
                    "content": None,
                    "payload": {**usage, "latency_ms": latency_ms},
                    "turn_id": turn_id,
                }
            )

        total_tokens = int((usage or {}).get("total_tokens") or 0)

        async with self._db.transaction() as session:
            new_seq = await self._allocate_ordinals(
                session, session_id, len(events), user_id=str(user_id)
            )
            if new_seq is None:
                logger.warning(
                    "finalize_turn_session_missing_or_not_owned",
                    session_id=session_id,
                    user_id=user_id,
                )
                raise NotFoundError("会话不存在")
            start = new_seq - len(events) + 1

            rows = []
            for offset, event in enumerate(events):
                payload = event.get("payload")
                rows.append(
                    {
                        "sid": session_id,
                        "ordinal": start + offset,
                        "turn_id": turn_id,
                        "type": event["type"],
                        "role": event.get("role"),
                        "content": event.get("content"),
                        "payload": json.dumps(payload, ensure_ascii=False) if payload else None,
                    }
                )
            await session.execute(
                text(
                    """
                    INSERT INTO ai_session_event
                        (session_id, ordinal, turn_id, type, role, content, payload)
                    VALUES (:sid, :ordinal, :turn_id, :type, :role, :content, :payload)
                    """
                ),
                rows,
            )

            # 派生状态：首轮补标题，累计轮次/消息/用量
            await exec_all(
                session,
                """
                UPDATE ai_session
                SET turn_count = turn_count + 1,
                    message_count = message_count + :msg_delta,
                    total_tokens = total_tokens + :tokens,
                    title = COALESCE(NULLIF(title, ''), :title)
                WHERE session_id = :sid AND user_id = :uid
                """,
                {
                    "sid": session_id,
                    "uid": str(user_id),
                    "msg_delta": 1 if error else 2,
                    "tokens": total_tokens,
                    "title": (question or "").strip()[:255] or None,
                },
            )

        logger.info(
            "turn_finalized",
            session_id=session_id,
            user_id=user_id,
            turn_id=turn_id,
            events=len(events),
            tool_calls=[c.get("name") for c in tool_calls],
            latency_ms=latency_ms,
            error=bool(error),
        )

    async def maybe_compact(self, session_id: str, user_id: str) -> bool:
        """上下文超预算时，把较早的轮次压缩成摘要。

        触发条件：压缩点之后的消息条数超限，或估算 token 总量超预算。
        保留"最近"一段（token 预算的一半 + 条数兜底），其余折叠进摘要；
        摘要自身也限长，避免越滚越大反过来吃掉窗口。

        原文**永不删除**，只更新 summary 与 summary_upto_ordinal，
        因此压缩后依然可以完整回溯，也能重建上下文。
        """
        row = await self.get_session(session_id, user_id)
        if not row:
            return False

        msg_limit = int(self._settings.session_max_context_messages)
        tok_limit = int(self._settings.session_max_context_tokens)
        upto = int(row.get("summary_upto_ordinal") or 0)

        events = await self._db.fetch_all(
            """
            SELECT ordinal, type, role, content FROM ai_session_event
            WHERE session_id = :sid AND ordinal > :upto
              AND type IN ('user_message', 'assistant_message')
            ORDER BY ordinal ASC
            """,
            {"sid": session_id, "upto": upto},
        )
        if not events:
            return False

        total_tokens = sum(estimate_tokens(e.get("content") or "") for e in events)
        over_budget = len(events) > msg_limit or total_tokens > tok_limit
        if not over_budget:
            return False

        # 保留最近一段：token 预算的一半 + 条数兜底（各取一半），并回退到轮次边界
        keep = _window_by_budget(events, max(msg_limit // 2, 1), max(tok_limit // 2, 1))
        if not keep:
            return False

        first_kept = int(keep[0]["ordinal"])
        fold = [e for e in events if int(e["ordinal"]) < first_kept]
        if not fold:
            return False
        fold_end = int(fold[-1]["ordinal"])

        folded_text = "\n".join(
            f"{'用户' if e['role'] == 'user' else '客服'}：{(e.get('content') or '').strip()}"
            for e in fold
            if e.get("content")
        )
        previous = (row.get("summary") or "").strip()
        new_summary = f"{previous}\n{folded_text}".strip() if previous else folded_text
        new_summary = _trim_summary(new_summary, max(tok_limit // 2, 1))

        async with self._db.transaction() as session:
            await exec_all(
                session,
                """
                UPDATE ai_session SET summary = :summary, summary_upto_ordinal = :upto
                WHERE session_id = :sid AND user_id = :uid
                """,
                {"summary": new_summary, "upto": fold_end, "sid": session_id, "uid": str(user_id)},
            )
        logger.info(
            "session_compacted",
            session_id=session_id,
            folded_events=len(fold),
            kept_messages=len(keep),
            summary_upto_ordinal=fold_end,
        )
        return True


# ==================== 纯函数（便于单测） ====================

# DeepSeek 官方口径：1 英文字符 ≈ 0.3 token，1 中文字符 ≈ 0.6 token。
# 这里只用于窗口预算的保守估算，实际用量以 API 返回的 usage 字段为准。
_ASCII_TOKENS_PER_CHAR = 0.3
_CJK_TOKENS_PER_CHAR = 0.6


def estimate_tokens(text: str) -> int:
    """按字符启发式估算文本 token 数（向上取整，非 ASCII 一律按中文口径）。

    只用于"决定上下文窗口保留多少历史"，不是计费依据。估算偏大是刻意的：
    宁可少留历史，也不让估算偏低导致超窗。
    """
    if not text:
        return 0
    ascii_chars = sum(1 for ch in text if ord(ch) < 128)
    non_ascii = len(text) - ascii_chars
    return math.ceil(ascii_chars * _ASCII_TOKENS_PER_CHAR + non_ascii * _CJK_TOKENS_PER_CHAR)


def _back_to_turn_start(events: List[Dict[str, Any]]) -> None:
    """就地回退：若窗口第一条是助手消息，说明它的提问被切掉了，往前找到提问。"""
    while events and events[0].get("type") != "user_message":
        events.pop(0)


def _window_by_budget(
    events: List[Dict[str, Any]], max_messages: int, max_tokens: int
) -> List[Dict[str, Any]]:
    """取"最近"的上下文窗口：条数兜底 + token 预算，并回退到轮次开头。

    返回原始事件（保留 ordinal 等字段），时间正序。
    """
    if not events or max_messages <= 0:
        return []

    window = events[-max_messages:] if len(events) > max_messages else list(events)
    trimmed = len(window) < len(events)

    if max_tokens > 0:
        kept: List[Dict[str, Any]] = []
        total = 0
        for event in reversed(window):
            tokens = estimate_tokens(event.get("content") or "")
            if kept and total + tokens > max_tokens:
                break
            kept.append(event)
            total += tokens
        window = list(reversed(kept))
        trimmed = trimmed or len(window) < len(events)

    if trimmed:
        _back_to_turn_start(window)
    return window


def trim_to_budget(
    events: List[Dict[str, Any]],
    *,
    max_messages: int,
    max_tokens: int,
) -> List[Dict[str, str]]:
    """按 token 预算为主、条数为兜底裁剪历史，并回退到轮次边界。"""
    return [
        {"role": e.get("role") or "user", "content": e.get("content") or ""}
        for e in _window_by_budget(events, max_messages, max_tokens)
        if e.get("content")
    ]


def trim_to_turn_boundary(events: List[Dict[str, Any]], limit: int) -> List[Dict[str, str]]:
    """保留最近 limit 条消息，并把起点回退到轮次开头（旧的条数版入口）。

    回退规则：若裁剪后的第一条是助手消息，说明它的提问被切掉了，
    继续往前找，直到落在一条 user_message 上。
    """
    return trim_to_budget(events, max_messages=limit, max_tokens=0)


def _trim_summary(summary: str, max_tokens: int) -> str:
    """摘要超长时只保留最近 max_tokens 的内容（从尾部向前按估算 token 截取）。"""
    if not summary or max_tokens <= 0:
        return summary or ""
    kept_chars: List[str] = []
    acc = 0.0
    for ch in reversed(summary):
        cost = _ASCII_TOKENS_PER_CHAR if ord(ch) < 128 else _CJK_TOKENS_PER_CHAR
        if acc + cost > max_tokens:
            break
        kept_chars.append(ch)
        acc += cost
    return "".join(reversed(kept_chars))


def new_turn_id() -> str:
    return f"turn_{uuid.uuid4().hex[:16]}"
