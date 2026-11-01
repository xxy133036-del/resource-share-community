"""数据库迁移执行器。

为什么不用 Alembic：
当前只有 1 张业务表、且没有多环境 schema 漂移问题，引入 Alembic 会带来
autogenerate 的模型声明负担。这里用"编号 SQL 文件 + 已执行记录表"实现最小可用方案：
迁移文件按序号命名、只执行一次、可重复运行（幂等）。

用法：
    venv\\Scripts\\python.exe scripts\\migrate.py            # 执行所有未应用的迁移
    venv\\Scripts\\python.exe scripts\\migrate.py --status   # 只看状态，不改动数据库

约定：
- 文件名形如 001_xxx.sql，按数字前缀升序执行
- 每个文件在**单个事务**内执行（DDL 在 MySQL 中会隐式提交，故以记录表为准）
- 执行记录写入 schema_migration 表，重复运行不会重复执行
"""

import argparse
import asyncio
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

MIGRATION_DIR = ROOT / "db" / "migrations"
MIGRATION_PATTERN = re.compile(r"^(\d+)_.+\.sql$")

TRACKING_TABLE = """
CREATE TABLE IF NOT EXISTS `schema_migration` (
  `version`    varchar(64) NOT NULL COMMENT '迁移版本号（文件名数字前缀）',
  `filename`   varchar(255) NOT NULL COMMENT '迁移文件名',
  `applied_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库迁移执行记录'
"""


def discover_migrations() -> list[tuple[str, Path]]:
    """发现迁移文件并按版本号升序返回。"""
    if not MIGRATION_DIR.is_dir():
        return []
    found = []
    for path in MIGRATION_DIR.iterdir():
        match = MIGRATION_PATTERN.match(path.name)
        if match:
            found.append((match.group(1), path))
    return sorted(found, key=lambda item: int(item[0]))


def split_statements(sql: str) -> list[str]:
    """按分号拆分 SQL 语句，忽略注释行。

    说明：只处理本项目迁移脚本的简单形态（无存储过程、无 DELIMITER），
    不做通用 SQL 解析——通用解析的正确做法是用成熟库，而不是手写。
    """
    lines = []
    for line in sql.splitlines():
        stripped = line.strip()
        if stripped.startswith("--") or not stripped:
            continue
        lines.append(line)
    body = "\n".join(lines)
    return [stmt.strip() for stmt in body.split(";") if stmt.strip()]


async def main() -> int:
    parser = argparse.ArgumentParser(description="执行数据库迁移")
    parser.add_argument("--status", action="store_true", help="只查看状态，不执行")
    args = parser.parse_args()

    from sqlalchemy import text

    from app.core.settings import get_settings
    from app.infra.mysql import init_database

    settings = get_settings()
    db = init_database(settings)

    if not await db.ping():
        print("无法连接数据库，请检查 .env 中的 MySQL 配置")
        return 1

    migrations = discover_migrations()
    if not migrations:
        print(f"未发现迁移文件（查找目录：{MIGRATION_DIR}）")
        return 0

    async with db.transaction() as session:
        await session.execute(text(TRACKING_TABLE))

    applied = {
        row["version"]
        for row in await db.fetch_all("SELECT version FROM schema_migration")
    }

    pending = [(v, p) for v, p in migrations if v not in applied]

    print(f"迁移文件 {len(migrations)} 个，已应用 {len(applied)} 个，待应用 {len(pending)} 个")
    for version, path in migrations:
        state = "已应用" if version in applied else "待应用"
        print(f"  [{state}] {path.name}")
    if args.status:
        await db.close()
        return 0

    if not pending:
        print("数据库已是最新，无需执行")
        await db.close()
        return 0

    for version, path in pending:
        statements = split_statements(path.read_text(encoding="utf-8"))
        print(f"执行 {path.name}（{len(statements)} 条语句）")
        for stmt in statements:
            await db.execute_ddl(stmt)
        async with db.transaction() as session:
            await session.execute(
                text(
                    "INSERT INTO schema_migration (version, filename) VALUES (:v, :f)"
                ),
                {"v": version, "f": path.name},
            )
        print(f"  ✓ {path.name}")

    print("迁移完成")
    await db.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))