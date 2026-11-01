"""工具层测试（需要真实 MySQL）。只读权限与状态过滤是关键回归点。"""

import pytest

pytestmark = pytest.mark.integration


@pytest.fixture(scope="module")
def db(settings):
    from app.infra.mysql import init_database
    return init_database(settings)


def _tools(user_id="15"):
    from app.tools.registry import build_tools
    return {t.name: t for t in build_tools(user_id)}


@pytest.mark.asyncio
async def test_search_finds_keyword(db):
    r = await _tools()["search_resources"].ainvoke({"keyword": "Java"})
    assert r["status"] == "success"
    assert r["total"] >= 1


@pytest.mark.asyncio
async def test_search_excludes_unapproved(db):
    """status != 2 的资源（未审核/未通过）绝不能出现在用户端搜索结果里。"""
    r = await _tools()["search_resources"].ainvoke({"keyword": "springboot"})
    names = [x["file_name"] for x in r.get("resources", [])]
    assert "springboot" not in names


@pytest.mark.asyncio
async def test_search_no_match(db):
    r = await _tools()["search_resources"].ainvoke({"keyword": "zzz绝对不存在zzz"})
    assert r["status"] == "no_match"


@pytest.mark.asyncio
async def test_resource_detail_invisible_hidden(db):
    """ID=10 的资源 status=0，不应对外可见。"""
    r = await _tools()["get_resource_detail"].ainvoke({"resource_id": 10})
    assert r["status"] == "not_found"


@pytest.mark.asyncio
async def test_profile_scoped_to_owner(db):
    """get_user_profile 只能查自己的信息，且不接受 user_id 参数。"""
    tools = _tools(user_id="15")
    profile_tool = tools["get_user_profile"]
    assert "user_id" not in profile_tool.args
    r = await profile_tool.ainvoke({})
    assert r["status"] == "success"
    assert r["profile"]["id"] == 15


@pytest.mark.asyncio
async def test_profile_rejects_injected_user_id(db):
    """即使模型硬塞 user_id 参数，也不应影响查询目标。"""
    r = await _tools(user_id="15")["get_user_profile"].ainvoke({"user_id": 14})
    assert r["profile"]["id"] == 15