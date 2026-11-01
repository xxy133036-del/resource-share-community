"""配置层测试：默认值、校验、fail fast、DSN 拼装。"""

import pytest
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

from app.core.settings import Settings


def test_defaults():
    s = Settings(_env_file=None, deepseek_api_key="sk-test")
    assert s.deepseek_api_base == "https://api.deepseek.com"
    assert s.deepseek_model == "deepseek-flash"
    assert s.agent_engine == "graph"
    assert s.llm_max_tokens == 4096


def test_missing_api_key_fails_fast():
    """缺少密钥时必须启动即失败，不能静默使用空值。"""
    with pytest.raises(Exception):
        Settings(_env_file=None, deepseek_api_key="")


def test_placeholder_api_key_rejected():
    with pytest.raises(Exception):
        Settings(_env_file=None, deepseek_api_key="<your-deepseek-api-key>")


def test_invalid_engine_rejected():
    with pytest.raises(Exception):
        Settings(_env_file=None, deepseek_api_key="sk-test", agent_engine="unknown")


def test_dsn_masking():
    s = Settings(_env_file=None, deepseek_api_key="sk-test", mysql_password="pw", redis_password="rp")
    assert "mysql+asyncmy://" in s.mysql_dsn
    assert s.redis_dsn.startswith("redis://")


def test_dsn_escapes_reserved_password_characters():
    s = Settings(
        _env_file=None,
        deepseek_api_key="sk-test",
        mysql_password="p@ss:/?#%",
        redis_password="p@ss:/?#%",
    )
    assert "p%40ss%3A%2F%3F%23%25" in s.mysql_dsn
    assert "p%40ss%3A%2F%3F%23%25" in s.redis_dsn


def test_auth_disabled_by_default():
    s = Settings(_env_file=None, deepseek_api_key="sk-test")
    assert s.auth_enabled is False


def test_auth_enabled_when_public_key_configured(tmp_path):
    public_key = tmp_path / "public.pem"
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    public_key.write_bytes(key.public_key().public_bytes(
        serialization.Encoding.PEM,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    ))
    s = Settings(
        _env_file=None,
        deepseek_api_key="sk-test",
        internal_call_public_key_file=str(public_key),
    )
    assert s.auth_enabled is True
