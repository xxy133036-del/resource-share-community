"""会话存储层单元测试（纯函数，无需数据库）。

重点验证：裁剪必须落在轮次边界上——这是旧实现 `history[-N:]`
最容易出问题的地方（会切出"只有回答没有提问"的残缺上下文）。
"""

from app.infra.session_store import (
    estimate_tokens,
    new_turn_id,
    trim_to_budget,
    trim_to_turn_boundary,
)


def _ev(ordinal, type_, role, content):
    return {"ordinal": ordinal, "type": type_, "role": role, "content": content}


def _conversation(turns):
    """把 (提问, 回答) 列表展开成事件序列。"""
    events = []
    ordinal = 1
    for q, a in turns:
        events.append(_ev(ordinal, "user_message", "user", q))
        ordinal += 1
        events.append(_ev(ordinal, "assistant_message", "assistant", a))
        ordinal += 1
    return events


def test_no_trim_when_under_limit():
    events = _conversation([("q1", "a1"), ("q2", "a2")])
    result = trim_to_turn_boundary(events, limit=10)
    assert [m["content"] for m in result] == ["q1", "a1", "q2", "a2"]


def test_trim_never_starts_with_assistant():
    """limit 为奇数时不能从回答中间切入，必须回退到该轮提问。"""
    events = _conversation([("q1", "a1"), ("q2", "a2"), ("q3", "a3")])
    result = trim_to_turn_boundary(events, limit=3)
    assert result[0]["role"] == "user", "裁剪后第一条必须是提问，否则上下文残缺"
    assert [m["content"] for m in result] == ["q3", "a3"], "应保留最近的完整轮次"


def test_trim_keeps_most_recent_turns():
    events = _conversation([("q1", "a1"), ("q2", "a2"), ("q3", "a3"), ("q4", "a4")])
    result = trim_to_turn_boundary(events, limit=4)
    assert [m["content"] for m in result] == ["q3", "a3", "q4", "a4"]


def test_empty_events():
    assert trim_to_turn_boundary([], limit=10) == []


def test_zero_limit_returns_empty():
    events = _conversation([("q1", "a1")])
    assert trim_to_turn_boundary(events, limit=0) == []


def test_skips_empty_content():
    events = [
        _ev(1, "user_message", "user", "问题"),
        _ev(2, "assistant_message", "assistant", ""),
    ]
    result = trim_to_turn_boundary(events, limit=10)
    assert [m["content"] for m in result] == ["问题"]


def test_turn_id_is_unique_and_prefixed():
    a, b = new_turn_id(), new_turn_id()
    assert a.startswith("turn_") and b.startswith("turn_")
    assert a != b


def test_estimate_tokens_matches_deepseek_heuristic():
    """DeepSeek 官方口径：英文 ≈0.3 token/字，中文 ≈0.6 token/字，向上取整。"""
    assert estimate_tokens("") == 0
    assert estimate_tokens("hello") == 2          # 5*0.3=1.5 -> 2
    assert estimate_tokens("你好") == 2           # 2*0.6=1.2 -> 2
    assert estimate_tokens("你好hello") == 3      # 1.2+1.5=2.7 -> 3


def test_trim_to_budget_token_is_primary():
    """每条消息 1 个中文字符 ≈ 1 token，token 预算 5 应切掉更早的轮次。"""
    events = [
        _ev(1, "user_message", "user", "一"),
        _ev(2, "assistant_message", "assistant", "二"),
        _ev(3, "user_message", "user", "三"),
        _ev(4, "assistant_message", "assistant", "四"),
        _ev(5, "user_message", "user", "五"),
        _ev(6, "assistant_message", "assistant", "六"),
        _ev(7, "user_message", "user", "七"),
        _ev(8, "assistant_message", "assistant", "八"),
    ]
    result = trim_to_budget(events, max_messages=10, max_tokens=5)
    assert [m["content"] for m in result] == ["五", "六", "七", "八"]


def test_trim_to_budget_message_cap_is_fallback():
    """消息极短时 token 预算不触发，条数兜底仍然生效。"""
    events = [
        _ev(1, "user_message", "user", "一"),
        _ev(2, "assistant_message", "assistant", "二"),
        _ev(3, "user_message", "user", "三"),
        _ev(4, "assistant_message", "assistant", "四"),
        _ev(5, "user_message", "user", "五"),
        _ev(6, "assistant_message", "assistant", "六"),
    ]
    result = trim_to_budget(events, max_messages=4, max_tokens=1000)
    assert [m["content"] for m in result] == ["三", "四", "五", "六"]


def test_trim_to_budget_lands_on_turn_boundary():
    """token 预算切在轮次中间时，必须回退到该轮提问。"""
    events = [
        _ev(1, "user_message", "user", "一"),
        _ev(2, "assistant_message", "assistant", "二"),
        _ev(3, "user_message", "user", "三"),
        _ev(4, "assistant_message", "assistant", "四"),
        _ev(5, "user_message", "user", "五"),
        _ev(6, "assistant_message", "assistant", "六"),
    ]
    # token 预算 3：从后往前累加会停在 [六,五,四]，回退后应落在"五"这一轮
    result = trim_to_budget(events, max_messages=10, max_tokens=3)
    assert result[0]["role"] == "user"
    assert [m["content"] for m in result] == ["五", "六"]