"""日志脱敏测试：密钥绝不能进入日志，但计量字段必须保留。"""

from app.core.logging import _redact_processor


def test_secret_fields_masked():
    ev = {"event": "x", "api_key": "sk-abcdefgh12345678", "password": "123456", "nested": {"access_token": "t0ken"}}
    out = _redact_processor(None, "info", ev)
    assert out["api_key"] == "***"
    assert out["password"] == "***"
    assert out["nested"]["access_token"] == "***"


def test_secret_literals_in_text_masked():
    ev = {"event": "call failed with sk-abcdefgh12345678", "note": "lsv2_sk_6eefadda068c4cfba68d4826d0a8078c"}
    out = _redact_processor(None, "info", ev)
    assert "sk-abcdefgh12345678" not in str(out)
    assert "lsv2_sk_6eefadda" not in str(out)


def test_usage_metrics_not_masked():
    """token 计量是可观测指标，不能被当成密钥脱敏。"""
    ev = {"event": "chat_completed", "usage": {"input_tokens": 100, "output_tokens": 5, "output_token_details": {"reasoning": 3}}}
    out = _redact_processor(None, "info", ev)
    assert out["usage"]["input_tokens"] == 100
    assert out["usage"]["output_tokens"] == 5
    assert out["usage"]["output_token_details"]["reasoning"] == 3


def test_normal_content_preserved():
    ev = {"event": "chat_completed", "conversation_id": "conv_1", "latency_ms": 120}
    out = _redact_processor(None, "info", ev)
    assert out["conversation_id"] == "conv_1"
    assert out["latency_ms"] == 120