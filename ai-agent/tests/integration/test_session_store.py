"""会话存储集成测试（需要真实 MySQL）。

覆盖：会话创建归属、事件追加的原子发号、越权隔离、上下文装配、
压缩后原文保留，以及失败轮次也要留痕。
"""

import uuid

import pytest

pytestmark = pytest.mark.integration


@pytest.fixture(scope="module")
def store(settings):
    from app.infra.mysql import init_database
    from app.infra.session_store import SessionStore

    db = init_database(settings)
    return SessionStore(settings, db)


def _sid():
    return f"conv_it_{uuid.uuid4().hex[:12]}"


@pytest.mark.asyncio
async def test_ensure_session_is_idempotent(store):
    sid = _sid()
    first = await store.ensure_session(sid, "15")
    second = await store.ensure_session(sid, "15")
    assert first["session_id"] == sid
    assert second["created_at"] == first["created_at"], "重复调用不应重建会话"


@pytest.mark.asyncio
async def test_ensure_session_does_not_reassign_owner(store):
    """会话归属不可被后续请求改变，防止会话 ID 被猜到后接管。"""
    from app.core.exceptions import NotFoundError

    sid = _sid()
    await store.ensure_session(sid, "15")
    with pytest.raises(NotFoundError):
        await store.ensure_session(sid, "99")
    row = await store.get_session(sid, "15")
    assert row is not None and row["user_id"] == "15"

    stolen = await store.get_session(sid, "99")
    assert stolen is None, "非归属者必须查不到该会话"


@pytest.mark.asyncio
async def test_archived_session_cannot_be_reused_for_chat(store):
    from app.core.exceptions import ValidationError

    sid = _sid()
    await store.ensure_session(sid, "15")
    assert await store.archive_session(sid, "15")
    with pytest.raises(ValidationError):
        await store.ensure_session(sid, "15")


@pytest.mark.asyncio
async def test_finalize_turn_rejects_non_owner(store):
    from app.core.exceptions import NotFoundError

    sid = _sid()
    await store.ensure_session(sid, "15")
    with pytest.raises(NotFoundError):
        await store.finalize_turn(
            sid, "99", turn_id="turn_attack", question="injected", answer="injected"
        )
    events = await store.get_events(sid, "15")
    assert events == []


@pytest.mark.asyncio
async def test_append_events_assigns_monotonic_ordinals(store):
    sid = _sid()
    await store.ensure_session(sid, "15")
    await store.append_events(
        sid,
        [
            {"type": "user_message", "role": "user", "content": "你好"},
            {"type": "assistant_message", "role": "assistant", "content": "你好呀"},
        ],
    )
    await store.append_events(
        sid, [{"type": "user_message", "role": "user", "content": "第二问"}]
    )

    events = await store.get_events(sid, "15")
    ordinals = [e["ordinal"] for e in events]
    assert ordinals == [1, 2, 3], f"序号必须连续递增，实际 {ordinals}"


@pytest.mark.asyncio
async def test_concurrent_append_never_reuses_ordinals(store):
    """并发追加必须各拿各的号段，不能串号或互相覆盖。"""
    import asyncio

    sid = _sid()
    await store.ensure_session(sid, "15")

    await asyncio.gather(
        *[
            store.append_events(
                sid,
                [
                    {"type": "user_message", "role": "user", "content": f"提问{i}"},
                    {"type": "assistant_message", "role": "assistant", "content": f"回答{i}"},
                ],
            )
            for i in range(5)
        ]
    )

    events = await store.get_events(sid, "15")
    ordinals = [e["ordinal"] for e in events]
    assert len(events) == 10
    assert len(set(ordinals)) == 10, "序号必须唯一"
    assert sorted(ordinals) == list(range(1, 11)), "序号必须连续无空洞"


@pytest.mark.asyncio
async def test_build_context_returns_messages_in_order(store):
    sid = _sid()
    await store.ensure_session(sid, "15")
    await store.append_events(
        sid,
        [
            {"type": "user_message", "role": "user", "content": "问题一"},
            {"type": "assistant_message", "role": "assistant", "content": "回答一"},
        ],
    )
    ctx = await store.build_context(sid, "15")
    assert [m["content"] for m in ctx.messages] == ["问题一", "回答一"]


@pytest.mark.asyncio
async def test_finalize_turn_updates_derived_state(store):
    sid = _sid()
    await store.ensure_session(sid, "15")
    await store.finalize_turn(
        sid,
        "15",
        turn_id="turn_x",
        question="平台上有什么 Java 资料？",
        answer="有 Java面试真题。",
        tool_calls=[{"name": "search_resources", "args": {"keyword": "Java"}}],
        usage={"total_tokens": 120},
        latency_ms=900,
    )

    row = await store.get_session(sid, "15")
    assert row["turn_count"] == 1
    assert row["message_count"] == 2
    assert row["total_tokens"] == 120
    assert row["title"].startswith("平台上有什么"), "首轮提问应自动成为标题"

    events = await store.get_events(sid, "15")
    types = [e["type"] for e in events]
    assert types.count("tool_call") == 1, "工具调用必须留痕，用于审计与排障"
    assert types.count("usage") == 1


@pytest.mark.asyncio
async def test_failed_turn_is_recorded(store):
    """失败的轮次也要落 error 事件，不能凭空消失。"""
    sid = _sid()
    await store.ensure_session(sid, "15")
    await store.finalize_turn(
        sid, "15", turn_id="turn_err", question="会失败的问题", answer="", error="upstream timeout"
    )

    events = await store.get_events(sid, "15")
    types = [e["type"] for e in events]
    assert "error" in types
    assert "assistant_message" not in types


@pytest.mark.asyncio
async def test_compaction_keeps_raw_events(store, settings):
    """压缩只更新摘要与压缩点，原始事件必须一条不少。"""
    sid = _sid()
    await store.ensure_session(sid, "15")

    for i in range(30):
        await store.append_events(
            sid,
            [
                {"type": "user_message", "role": "user", "content": f"提问{i}"},
                {"type": "assistant_message", "role": "assistant", "content": f"回答{i}"},
            ],
        )
    before = await store.get_events(sid, "15")

    changed = await store.maybe_compact(sid, "15")
    assert changed, "超过阈值必须触发压缩"

    row = await store.get_session(sid, "15")
    assert row["summary"], "压缩后必须有摘要"
    assert int(row["summary_upto_ordinal"]) > 0

    after = await store.get_events(sid, "15")
    assert len(after) == len(before), "原始事件不允许被删除"

    ctx = await store.build_context(sid, "15")
    assert ctx.summary == row["summary"]
    assert all(
        not m["content"].startswith("提问0") for m in ctx.messages
    ), "已被摘要覆盖的早期内容不应重复进入上下文"


@pytest.mark.asyncio
async def test_archive_keeps_history(store):
    sid = _sid()
    await store.ensure_session(sid, "15")
    await store.finalize_turn(sid, "15", turn_id="t", question="问", answer="答")

    assert await store.archive_session(sid, "15")
    row = await store.get_session(sid, "15")
    assert row["status"] == "archived"
    assert len(await store.get_events(sid, "15")) > 0, "归档不等于删除"


@pytest.mark.asyncio
async def test_cross_user_isolation(store):
    """A 用户不能读取、改名或归档 B 用户的会话。"""
    sid = _sid()
    await store.ensure_session(sid, "15")
    await store.finalize_turn(sid, "15", turn_id="t", question="私密问题", answer="私密回答")

    ctx = await store.build_context(sid, "99")
    assert ctx.messages == [], "越权读取必须返回空上下文"

    assert await store.get_events(sid, "99") == []
    assert not await store.rename_session(sid, "99", "hacked")
    assert not await store.archive_session(sid, "99")
