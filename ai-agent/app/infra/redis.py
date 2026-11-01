"""Redis 异步访问层：原子令牌桶限流 + 轻量缓存。

说明：会话历史已迁移到 MySQL 的追加写事件日志（见 infra/session_store.py），
Redis 不再承担历史存储职责，只保留限流这一类天然适合放 Redis 的数据。
"""

from typing import Optional, Sequence, Tuple

import redis.asyncio as aioredis

from app.core.logging import get_logger
from app.core.settings import Settings

logger = get_logger(__name__)

_TOKEN_BUCKET_SCRIPT = """
local time = redis.call('TIME')
local now_ms = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local states = {}
local allowed = 1
local retry_after_ms = 0

for i, key in ipairs(KEYS) do
    local arg = (i - 1) * 3
    local capacity = tonumber(ARGV[arg + 1])
    local refill_per_second = tonumber(ARGV[arg + 2])
    local cost = tonumber(ARGV[arg + 3])
    local values = redis.call('HMGET', key, 'tokens', 'updated_ms')
    local tokens = tonumber(values[1]) or capacity
    local updated_ms = tonumber(values[2]) or now_ms
    local elapsed_ms = math.max(0, now_ms - updated_ms)
    tokens = math.min(capacity, tokens + elapsed_ms * refill_per_second / 1000)

    if tokens < cost then
        allowed = 0
        local wait_ms = math.ceil((cost - tokens) * 1000 / refill_per_second)
        retry_after_ms = math.max(retry_after_ms, wait_ms)
    end
    states[i] = {tokens, capacity, refill_per_second, cost}
end

for i, key in ipairs(KEYS) do
    local state = states[i]
    local tokens = state[1]
    if allowed == 1 then
        tokens = tokens - state[4]
    end
    redis.call('HSET', key, 'tokens', tokens, 'updated_ms', now_ms)
    local ttl_ms = math.max(1000, math.ceil(state[2] * 2000 / state[3]))
    redis.call('PEXPIRE', key, ttl_ms)
end

return {allowed, retry_after_ms}
"""


class RedisStore:
    def __init__(self, settings: Settings):
        self._client: aioredis.Redis = aioredis.from_url(
            settings.redis_dsn,
            encoding="utf-8",
            decode_responses=True,
            socket_connect_timeout=5,
            socket_timeout=5,
        )

    async def ping(self) -> bool:
        try:
            await self._client.ping()
            return True
        except Exception as exc:  # noqa: BLE001
            logger.warning("redis_ping_failed", error=str(exc))
            return False

    # ---------------- 令牌桶限流 ----------------
    async def consume_rate_limits(
        self, buckets: Sequence[Tuple[str, float, float, float]]
    ) -> Optional[Tuple[bool, int]]:
        """原子检查并扣减多个令牌桶，返回 (allowed, retry_after_ms)。

        每个桶参数为 (key, capacity, refill_per_second, cost)。Redis 异常时
        返回 None，由调用方按可用性策略处理。多个桶在同一 Lua 脚本内扣减，
        因此用户桶或会话桶任一个拒绝时，另一个都不会被扣令牌。
        """
        if not buckets:
            return True, 0
        try:
            keys = [item[0] for item in buckets]
            args = [str(value) for _, capacity, refill, cost in buckets
                    for value in (capacity, refill, cost)]
            result = await self._client.eval(_TOKEN_BUCKET_SCRIPT, len(keys), *keys, *args)
            return bool(int(result[0])), int(result[1])
        except Exception as exc:  # noqa: BLE001
            logger.warning("redis_rate_limit_failed", error=str(exc))
            return None

    async def close(self) -> None:
        await self._client.aclose()


_redis: Optional[RedisStore] = None


def init_redis(settings: Settings) -> RedisStore:
    global _redis
    _redis = RedisStore(settings)
    return _redis


def get_redis() -> RedisStore:
    if _redis is None:
        raise RuntimeError("Redis 尚未初始化")
    return _redis
