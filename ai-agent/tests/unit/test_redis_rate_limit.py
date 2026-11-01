"""Redis token bucket wrapper tests that do not require a running Redis server."""

import pytest

from app.infra.redis import RedisStore


class _FakeRedisClient:
    def __init__(self, result=None, error=None):
        self.result = result
        self.error = error
        self.call = None

    async def eval(self, script, numkeys, *args):
        self.call = (script, numkeys, args)
        if self.error:
            raise self.error
        return self.result


@pytest.mark.asyncio
async def test_consume_rate_limits_checks_and_debits_all_buckets_atomically():
    client = _FakeRedisClient(result=[1, 0])
    store = RedisStore.__new__(RedisStore)
    store._client = client

    result = await store.consume_rate_limits(
        [("user", 60, 1.0, 1.0), ("conversation", 20, 1 / 3, 1.0)]
    )

    assert result == (True, 0)
    script, numkeys, args = client.call
    assert "redis.call('TIME')" in script
    assert numkeys == 2
    assert args[:2] == ("user", "conversation")
    assert args[2:] == ("60", "1.0", "1.0", "20", str(1 / 3), "1.0")


@pytest.mark.asyncio
async def test_consume_rate_limits_returns_retry_after_when_rejected():
    client = _FakeRedisClient(result=[0, 3000])
    store = RedisStore.__new__(RedisStore)
    store._client = client

    assert await store.consume_rate_limits([("bucket", 2, 1.0, 1.0)]) == (False, 3000)


@pytest.mark.asyncio
async def test_consume_rate_limits_fails_open_when_redis_is_unavailable():
    client = _FakeRedisClient(error=ConnectionError("offline"))
    store = RedisStore.__new__(RedisStore)
    store._client = client

    assert await store.consume_rate_limits([("bucket", 2, 1.0, 1.0)]) is None
