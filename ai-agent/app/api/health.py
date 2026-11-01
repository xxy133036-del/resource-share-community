"""健康检查：区分进程存活（liveness）与依赖就绪（readiness）。"""

from fastapi import APIRouter, Request

from app.schemas.chat import HealthResponse

router = APIRouter(tags=["health"])

VERSION = "1.0.0"


@router.get("/healthz", response_model=HealthResponse)
async def healthz(request: Request) -> HealthResponse:
    """存活探针：只要进程活着就返回 ok，不探测下游。"""
    settings = request.app.state.settings
    return HealthResponse(status="ok", version=VERSION, engine=settings.agent_engine, dependencies={})


@router.get("/readyz", response_model=HealthResponse)
async def readyz(request: Request) -> HealthResponse:
    """就绪探针：探测 MySQL / Redis 连通性。"""
    from app.infra.mysql import get_database
    from app.infra.redis import get_redis

    settings = request.app.state.settings
    deps = {
        "mysql": await get_database().ping(),
        "redis": await get_redis().ping(),
        "langsmith": settings.langsmith_enabled,
    }
    healthy = deps["mysql"] and deps["redis"]
    return HealthResponse(
        status="ok" if healthy else "degraded",
        version=VERSION,
        engine=settings.agent_engine,
        dependencies=deps,
    )