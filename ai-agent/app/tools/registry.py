"""工具集（只读）。

设计原则：
- 全部只读，Agent 无写权限，即使被 Prompt 注入也无法改数据。
- 参数用 pydantic 约束，模型通过原生 tool calling 传参，不再依赖正则解析。
- 所有 SQL 均参数绑定。

安全重点：get_user_profile 不接受 user_id 入参。
模型无权指定查谁，用户身份从服务端签名上下文注入，从根上消灭越权查询。
"""

from typing import Any, Dict, List, Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.core.logging import get_logger
from app.infra.mysql import get_database

logger = get_logger(__name__)

# 平台资源对用户可见的状态：2=审核通过（用户端只看这个）
_VISIBLE_STATUS = 2

# 排序白名单：杜绝把用户输入拼进 ORDER BY
_SORT_COLUMNS = {
    "downloads": "downloads_count",
    "likes": "likes_count",
    "comments": "comments_count",
    "latest": "update_time",
}


class SearchResourcesArgs(BaseModel):
    keyword: str = Field(default="", description="搜索关键词，匹配资源名或标签，可为空")
    sort_by: str = Field(
        default="downloads",
        description="排序方式：downloads(下载量) / likes(点赞) / comments(评论) / latest(最新)",
    )
    limit: int = Field(default=5, description="返回条数，1~10", ge=1, le=10)


class ResourceDetailArgs(BaseModel):
    resource_id: int = Field(..., description="资源 ID", ge=1)


def build_tools(user_id: Optional[str]):
    """构建绑定到当前请求上下文的工具列表。

    user_id 由服务端注入（来自签名上下文），模型既看不到也无法指定。
    """

    @tool("search_resources", args_schema=SearchResourcesArgs)
    async def search_resources(keyword: str = "", sort_by: str = "downloads", limit: int = 5) -> Dict[str, Any]:
        """搜索平台上的资源。当用户想找某个主题的资料时使用。"""
        db = get_database()
        column = _SORT_COLUMNS.get(sort_by, "downloads_count")
        sql = [
            "SELECT id, file_name, tags, likes_count, comments_count, downloads_count",
            "FROM file_info WHERE status = :status",
        ]
        params: Dict[str, Any] = {"status": _VISIBLE_STATUS, "limit": limit}
        if keyword and keyword.strip():
            sql.append("AND (file_name LIKE :kw OR tags LIKE :kw)")
            params["kw"] = f"%{keyword.strip()}%"
        sql.append(f"ORDER BY {column} DESC, id DESC LIMIT :limit")

        rows = await db.fetch_all(" ".join(sql), params)
        if not rows:
            return {"status": "no_match", "message": "没有找到相关资源，可以换个关键词试试"}
        return {"status": "success", "total": len(rows), "resources": rows}

    @tool("get_resource_detail", args_schema=ResourceDetailArgs)
    async def get_resource_detail(resource_id: int) -> Dict[str, Any]:
        """查询某个资源的详细信息。用户询问具体资源（如"这个文件多少积分""第几个资源"）时使用。"""
        db = get_database()
        row = await db.fetch_one(
            """
            SELECT id, file_name, description, tags, likes_count, comments_count, downloads_count
            FROM file_info WHERE id = :rid AND status = :status
            """,
            {"rid": resource_id, "status": _VISIBLE_STATUS},
        )
        if not row:
            return {"status": "not_found", "message": f"资源 {resource_id} 不存在或未上架"}
        return {"status": "success", "resource": row}

    @tool("get_user_profile")
    async def get_user_profile() -> Dict[str, Any]:
        """查询当前登录用户的昵称和积分。仅能查询提问者本人，不能查询他人。"""
        if not user_id:
            return {"status": "no_user", "message": "当前没有登录用户信息"}
        db = get_database()
        row = await db.fetch_one(
            "SELECT id, username, nickname, points FROM user_info WHERE id = :uid",
            {"uid": int(user_id) if str(user_id).isdigit() else user_id},
        )
        if not row:
            return {"status": "not_found", "message": "没有找到该用户"}
        return {"status": "success", "profile": row}

    return [search_resources, get_resource_detail, get_user_profile]