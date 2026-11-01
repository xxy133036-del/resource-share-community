# 多阶段构建：先在 builder 中装依赖，再复制到运行镜像，减小体积
FROM python:3.10-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build
COPY requirements.txt .
RUN pip install --prefix=/install --default-timeout=120 \
        -i https://mirrors.aliyun.com/pypi/simple/ \
        -r requirements.txt

FROM python:3.10-slim

ENV TZ=Asia/Shanghai \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --shell /bin/bash appuser

COPY --from=builder /install /usr/local

WORKDIR /app
COPY --chown=appuser:appuser app ./app
COPY --chown=appuser:appuser knowledge ./knowledge
COPY --chown=appuser:appuser scripts ./scripts
COPY --chown=appuser:appuser db ./db

# 安全：非 root 运行
USER appuser

EXPOSE 8013

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8013/healthz || exit 1

# 启动时先执行幂等迁移（编号 SQL + schema_migration 记录表），再拉起服务，
# 保证全新环境也能自动建表；迁移失败则退出，交由 restart 策略重试。
CMD ["sh", "-c", "python scripts/migrate.py && exec python -m app.main"]