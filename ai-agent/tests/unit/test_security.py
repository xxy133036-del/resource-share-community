"""服务间 RSA 签名测试：验签、时间窗和请求篡改检测。"""

import base64
import hashlib
import time

import pytest
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

from app.core.exceptions import AuthError
from app.core.security import canonical_query, canonical_request, verify_signature
from app.core.settings import Settings


@pytest.fixture
def rsa_keys(tmp_path):
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    private_file = tmp_path / "private.pem"
    public_file = tmp_path / "public.pem"
    private_file.write_bytes(private_key.private_bytes(
        serialization.Encoding.PEM,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    ))
    public_file.write_bytes(private_key.public_key().public_bytes(
        serialization.Encoding.PEM,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    ))
    return private_key, str(public_file)


def _settings(public_key_file):
    return Settings(
        _env_file=None,
        deepseek_api_key="sk-test",
        internal_call_public_key_file=public_key_file,
    )


def _sign(private_key, timestamp, request_id, body, *, method="POST", path="", query=""):
    canonical = canonical_request(method, path, query, body)
    digest = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    payload = f"{timestamp}.{request_id}.{digest}".encode("utf-8")
    signature = private_key.sign(payload, padding.PKCS1v15(), hashes.SHA256())
    return base64.b64encode(signature).decode("ascii")


def test_verification_disabled_without_public_key():
    settings = Settings(_env_file=None, deepseek_api_key="sk-test")
    verify_signature(settings, timestamp=None, request_id=None, signature=None, body=b"{}")


def test_valid_signature_passes(rsa_keys):
    private_key, public_file = rsa_keys
    settings = _settings(public_file)
    timestamp = str(time.time())
    body = b'{"hello":"world"}'
    signature = _sign(private_key, timestamp, "req-1", body)
    verify_signature(settings, timestamp=timestamp, request_id="req-1", signature=signature, body=body)


def test_missing_headers_rejected(rsa_keys):
    _, public_file = rsa_keys
    with pytest.raises(AuthError):
        verify_signature(_settings(public_file), timestamp=None, request_id="r", signature="x", body=b"{}")


def test_tampered_body_rejected(rsa_keys):
    private_key, public_file = rsa_keys
    settings = _settings(public_file)
    timestamp = str(time.time())
    signature = _sign(private_key, timestamp, "req-1", b'{"a":1}')
    with pytest.raises(AuthError):
        verify_signature(settings, timestamp=timestamp, request_id="req-1", signature=signature, body=b'{"a":2}')


def test_wrong_signature_rejected(rsa_keys):
    _, public_file = rsa_keys
    with pytest.raises(AuthError):
        verify_signature(_settings(public_file), timestamp=str(time.time()), request_id="req-1", signature="deadbeef", body=b"{}")


def test_expired_timestamp_rejected(rsa_keys):
    private_key, public_file = rsa_keys
    settings = _settings(public_file)
    timestamp = str(time.time() - 9999)
    body = b"{}"
    signature = _sign(private_key, timestamp, "req-1", body)
    with pytest.raises(AuthError):
        verify_signature(settings, timestamp=timestamp, request_id="req-1", signature=signature, body=body)


def test_canonical_query_is_order_insensitive():
    assert canonical_query("userId=15&limit=10") == canonical_query("limit=10&userId=15")


def test_canonical_query_keeps_blank_values():
    assert canonical_query("flag=") == "flag="
    assert canonical_query("a=1&flag=") == "a=1&flag="


def test_canonical_query_escapes_delimiters_inside_values():
    encoded_value = canonical_query("a=b%26c%3Dd")
    separate_parameters = canonical_query("a=b&c=d")
    assert encoded_value == "a=b%26c%3Dd"
    assert separate_parameters == "a=b&c=d"
    assert encoded_value != separate_parameters


def test_canonical_query_sorts_encoded_unicode_components():
    assert canonical_query("𐀀=1&=2") == "%EE%80%80=2&%F0%90%80%80=1"


def test_get_request_signature_covers_query(rsa_keys):
    private_key, public_file = rsa_keys
    settings = _settings(public_file)
    timestamp = str(time.time())
    signature = _sign(private_key, timestamp, "req-get", b"", method="GET",
                      path="/v1/sessions", query="userId=15&limit=10")
    verify_signature(settings, timestamp=timestamp, request_id="req-get", signature=signature,
                     body=b"", method="GET", path="/v1/sessions", query="userId=15&limit=10")


def test_tampered_query_rejected(rsa_keys):
    private_key, public_file = rsa_keys
    settings = _settings(public_file)
    timestamp = str(time.time())
    signature = _sign(private_key, timestamp, "req-get", b"", method="GET",
                      path="/v1/sessions", query="userId=15&limit=10")
    with pytest.raises(AuthError):
        verify_signature(settings, timestamp=timestamp, request_id="req-get", signature=signature,
                         body=b"", method="GET", path="/v1/sessions", query="userId=1&limit=10")


def test_tampered_path_rejected(rsa_keys):
    private_key, public_file = rsa_keys
    settings = _settings(public_file)
    timestamp = str(time.time())
    signature = _sign(private_key, timestamp, "req-get", b"", method="GET",
                      path="/v1/sessions", query="userId=15")
    with pytest.raises(AuthError):
        verify_signature(settings, timestamp=timestamp, request_id="req-get", signature=signature,
                         body=b"", method="GET", path="/v1/sessions/other", query="userId=15")


def test_get_request_without_query_is_valid(rsa_keys):
    private_key, public_file = rsa_keys
    settings = _settings(public_file)
    timestamp = str(time.time())
    signature = _sign(private_key, timestamp, "r", b"", method="GET", path="/healthz", query="")
    verify_signature(settings, timestamp=timestamp, request_id="r", signature=signature,
                     body=b"", method="GET", path="/healthz")
