"""应用入口：装配 FastAPI、生命周期、路由与异常处理。"""

import time
import uuid
import os
from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.agent.graph import AgentEngine
from app.api import chat as chat_api
from app.api import health as health_api
from app.api import sessions as sessions_api
from app.core.exceptions import AgentError
from app.core.llm import build_llm
from app.core.logging import configure_logging, get_logger
from app.core.settings import get_settings
from app.infra.mysql import init_database
from app.infra.redis import init_redis
from app.infra.session_store import SessionStore
from app.services.chat_service import ChatService

logger = get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    configure_logging(settings.log_level)
    logger.info(
        "app_starting",
        engine=settings.agent_engine,
        model=settings.deepseek_model,
        env=settings.app_env,
    )

    db = init_database(settings)
    redis = init_redis(settings)
    logger.info("dependencies_initialized", mysql=await db.ping(), redis=await redis.ping())

    engine = AgentEngine(settings, llm_factory=lambda **kw: build_llm(settings, **kw))
    sessions = SessionStore(settings, db)
    app.state.settings = settings
    app.state.engine = engine
    app.state.sessions = sessions
    app.state.chat_service = ChatService(settings, engine, sessions)

    logger.info("app_ready", port=settings.app_port)
    try:
        yield
    finally:
        await db.close()
        await redis.close()
        logger.info("app_stopped")


def create_app() -> FastAPI:
    production = os.getenv("APP_ENV", "dev").strip().lower() in {"prod", "production"}
    app = FastAPI(
        title="HC1 AI 客服 Agent",
        version="1.0.0",
        lifespan=lifespan,
        docs_url=None if production else "/docs",
        redoc_url=None if production else "/redoc",
        openapi_url=None if production else "/openapi.json",
    )

    # ---------------- 中间件：request_id 贯穿 ----------------
    @app.middleware("http")
    async def request_context_middleware(request: Request, call_next):
        if (
            production
            and request.url.path in {"/healthz", "/readyz"}
            and request.client is not None
            and request.client.host not in {"127.0.0.1", "::1"}
        ):
            return JSONResponse(status_code=404, content={"detail": "Not Found"})
        request_id = request.headers.get("X-Request-Id") or f"req_{uuid.uuid4().hex[:16]}"
        started = time.time()
        structlog.contextvars.clear_contextvars()
        structlog.contextvars.bind_contextvars(request_id=request_id, path=request.url.path)
        try:
            response = await call_next(request)
        except Exception:
            logger.error("unhandled_exception", path=request.url.path, duration_ms=int((time.time() - started) * 1000))
            raise
        response.headers["X-Request-Id"] = request_id
        logger.info(
            "http_request",
            method=request.method,
            status=response.status_code,
            duration_ms=int((time.time() - started) * 1000),
        )
        return response

    # ---------------- 统一异常处理 ----------------
    @app.exception_handler(AgentError)
    async def agent_error_handler(_request: Request, exc: AgentError):
        logger.warning("agent_error", code=exc.code, message=exc.message)
        return JSONResponse(status_code=exc.http_status, content=exc.to_dict())

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(_request: Request, exc: RequestValidationError):
        return JSONResponse(
            status_code=422,
            content={"error": {"code": "invalid_request", "message": "请求参数不合法", "detail": exc.errors()}},
        )

    app.include_router(health_api.router)
    app.include_router(chat_api.router)
    app.include_router(sessions_api.router)
    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    _settings = get_settings()
    uvicorn.run(app, host="0.0.0.0", port=_settings.app_port)
