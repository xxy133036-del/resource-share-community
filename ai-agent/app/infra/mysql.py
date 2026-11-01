"""MySQL 异步访问层。

为什么用异步：旧实现把同步 mysql-connector 放在 async def 里，
单个慢查询会阻塞整个事件循环，并发能力约等于 1。
这里改用 SQLAlchemy 2.0 async + asyncmy，是真异步 + 真连接池。

安全：所有 SQL 均使用参数绑定，禁止字符串拼接用户输入。

除只读查询外，这里还提供事务入口（transaction），
供会话事件日志做"发号 + 追加 + 更新汇总"的原子写入。
"""

from contextlib import asynccontextmanager
from typing import Any, AsyncIterator, Dict, Iterable, List, Optional

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine

from app.core.exceptions import UpstreamError
from app.core.logging import get_logger
from app.core.settings import Settings

logger = get_logger(__name__)


class Database:
    """MySQL 连接池封装。"""

    def __init__(self, settings: Settings):
        self._engine: AsyncEngine = create_async_engine(
            settings.mysql_dsn,
            pool_size=settings.mysql_pool_size,
            max_overflow=settings.mysql_max_overflow,
            pool_pre_ping=True,        # 取连接前探活，避免拿到已断开的连接
            pool_recycle=settings.mysql_pool_recycle,
            echo=False,
            future=True,
        )
        self._session_factory = async_sessionmaker(
            self._engine, expire_on_commit=False, class_=AsyncSession
        )

    async def ping(self) -> bool:
        try:
            async with self._engine.connect() as conn:
                await conn.execute(text("SELECT 1"))
            return True
        except Exception as exc:  # noqa: BLE001
            logger.warning("mysql_ping_failed", error=str(exc))
            return False

    # ---------------- 只读查询 ----------------
    async def fetch_all(self, sql: str, params: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """执行查询并返回字典列表。"""
        try:
            async with self._session_factory() as session:
                result = await session.execute(text(sql), params or {})
                return [dict(row._mapping) for row in result.fetchall()]
        except Exception as exc:  # noqa: BLE001
            logger.error("mysql_query_failed", error=str(exc), sql=sql[:200])
            raise UpstreamError("数据查询失败，请稍后再试") from exc

    async def fetch_one(self, sql: str, params: Optional[Dict[str, Any]] = None) -> Optional[Dict[str, Any]]:
        rows = await self.fetch_all(sql, params)
        return rows[0] if rows else None

    async def execute_ddl(self, sql: str) -> None:
        """执行无结果集的语句（DDL / 管理类语句）。

        不能用 fetch_all 代替：DDL 不返回结果集，
        fetchall() 会抛 ResourceClosedError。
        """
        try:
            async with self._engine.begin() as conn:
                await conn.execute(text(sql))
        except Exception as exc:  # noqa: BLE001
            logger.error("mysql_ddl_failed", error=str(exc), sql=sql[:200])
            raise UpstreamError("建表/迁移语句执行失败") from exc

    # ---------------- 事务写入 ----------------
    @asynccontextmanager
    async def transaction(self) -> AsyncIterator[AsyncSession]:
        """开启一个事务。

        会话事件日志的写入必须原子：分配序号、追加事件、更新会话汇总
        要么全成功，要么全回滚，否则会出现序号空洞或统计与事件不一致。
        """
        async with self._session_factory() as session:
            async with session.begin():
                yield session

    async def close(self) -> None:
        await self._engine.dispose()


# ---------------- 事务内可复用的执行助手 ----------------
async def exec_all(session: AsyncSession, sql: str, params: Optional[Dict[str, Any]] = None) -> int:
    """执行写语句，返回受影响行数。"""
    result = await session.execute(text(sql), params or {})
    return result.rowcount or 0


async def select_all(session: AsyncSession, sql: str, params: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
    result = await session.execute(text(sql), params or {})
    return [dict(row._mapping) for row in result.fetchall()]


async def select_one(session: AsyncSession, sql: str, params: Optional[Dict[str, Any]] = None) -> Optional[Dict[str, Any]]:
    rows = await select_all(session, sql, params)
    return rows[0] if rows else None


_db: Optional[Database] = None


def init_database(settings: Settings) -> Database:
    global _db
    _db = Database(settings)
    return _db


def get_database() -> Database:
    if _db is None:
        raise RuntimeError("数据库尚未初始化")
    return _db


__all__ = [
    "Database",
    "init_database",
    "get_database",
    "exec_all",
    "select_all",
    "select_one",
]