"""全局配置：唯一配置入口。

所有配置从环境变量 / .env 读取，代码中不出现任何密钥。
缺失关键密钥时在启动阶段 fail fast，避免静默使用错误配置。
"""

from functools import lru_cache
from pathlib import Path
from urllib.parse import quote, quote_plus

from pydantic import Field, SecretStr, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

# 占位符：.env.example 中的值，出现即视为未配置
_PLACEHOLDERS = {"<your-deepseek-api-key>", "<your-langsmith-key>", ""}


class Settings(BaseSettings):
    """应用配置。字段名与 .env 中的变量名一致（大小写不敏感）。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # ---------- 服务 ----------
    app_name: str = "HC1 AI 客服"
    app_port: int = 8013
    app_env: str = "dev"
    log_level: str = "INFO"

    # 引擎实现，目前仅 graph（LangGraph 状态机）。
    agent_engine: str = "graph"

    # ---------- LLM：DeepSeek 官方 API ----------
    deepseek_api_base: str = "https://api.deepseek.com"
    deepseek_api_key: SecretStr = Field(default=SecretStr(""))
    deepseek_model: str = "deepseek-flash"
    llm_temperature: float = 0.3
    # 单次请求超时（秒）与重试次数
    llm_timeout: float = 60.0
    llm_max_retries: int = 2
    # 实测：推理模型的思考过程会占用 max_tokens 预算，给足输出空间
    llm_max_tokens: int = 4096
    # Agent 工具调用最大轮次，超出则兜底回复
    agent_max_iterations: int = 4
    # 流式请求在上游连接抖动时的最大尝试次数。
    # 只在尚未产出任何 token 时重试，已产出内容后绝不重试（避免重复输出）
    llm_stream_max_attempts: int = 3
    # 非流式请求的最大尝试次数。上游偶发连接抖动时重试，
    # 避免把可恢复的瞬时故障直接暴露成 503（实测约 1/6 概率命中）
    llm_invoke_max_attempts: int = 3

    # ---------- 观测：LangSmith ----------
    langsmith_tracing: bool = False
    langsmith_api_key: SecretStr = Field(default=SecretStr(""))
    langsmith_project: str = "hc1-ai-agent"
    langsmith_endpoint: str = "https://api.smith.langchain.com"

    # ---------- MySQL ----------
    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "root"
    mysql_password: SecretStr = Field(default=SecretStr(""))
    mysql_database: str = "hc1db_user"
    # 使用 SQLAlchemy async（asyncmy），异步驱动才不会被慢查询阻塞事件循环
    mysql_pool_size: int = 5
    mysql_max_overflow: int = 5
    mysql_pool_recycle: int = 3600

    # ---------- Redis ----------
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: SecretStr = Field(default=SecretStr(""))
    redis_db: int = 0
    # 会话历史保留时长（秒），默认 24 小时
    history_ttl: int = 86400
    # 单会话最多保留的消息条数，超出触发摘要/截断
    history_max_messages: int = 40

    # ---------- 会话存储（事件日志） ----------
    # 装配进模型上下文的历史 token 预算（字符启发式估算，非精确计费）。
    # 这是裁剪的"主预算"；条数是兜底硬上限，防止估算法被大量超短消息绕过。
    session_max_context_tokens: int = 8000
    # 装配进模型上下文的最大消息条数（兜底硬上限）
    session_max_context_messages: int = 40
    # 是否启用摘要压缩。关闭后只做 token/条数裁剪，不折叠成摘要
    session_compaction_enabled: bool = True

    # ---------- 服务间鉴权 ----------
    # Python 只加载 Java RSA 签名公钥；留空仅允许本地开发环境跳过校验
    internal_call_public_key_file: str = ""

    # ---------- Prompt ----------
    # 业务知识文件路径（相对项目根目录）
    knowledge_file: str = "knowledge/knowledge.md"
    # dumps 原始模型输出到日志（仅调试用，可能含用户数据）
    debug_dump_llm: bool = False

    # ==================== 校验 ====================
    @field_validator("agent_engine")
    @classmethod
    def _check_engine(cls, v: str) -> str:
        v = (v or "").strip().lower()
        if v != "graph":
            raise ValueError("AGENT_ENGINE 目前只支持 graph")
        return v

    @field_validator("deepseek_api_key", mode="after")
    @classmethod
    def _check_deepseek_key(cls, v: SecretStr) -> SecretStr:
        if v.get_secret_value() in _PLACEHOLDERS:
            raise ValueError(
                "缺少 DEEPSEEK_API_KEY：请在 .env 中配置（参考 .env.example）"
            )
        return v

    @field_validator("internal_call_public_key_file")
    @classmethod
    def _check_internal_public_key_file(cls, value: str) -> str:
        value = (value or "").strip()
        if value:
            path = Path(value)
            if not path.is_file():
                raise ValueError("INTERNAL_CALL_PUBLIC_KEY_FILE 指向的公钥文件不存在")
            try:
                key = serialization.load_pem_public_key(path.read_bytes())
            except (ValueError, OSError) as exc:
                raise ValueError("INTERNAL_CALL_PUBLIC_KEY_FILE 不是有效的 PEM 公钥") from exc
            if not isinstance(key, rsa.RSAPublicKey) or key.key_size < 2048:
                raise ValueError("服务间验签公钥必须是至少 2048 位的 RSA 公钥")
        return value

    @model_validator(mode="after")
    def _require_auth_key_in_production(self):
        if self.app_env.strip().lower() in {"prod", "production"} and not self.internal_call_public_key_file:
            raise ValueError("生产环境必须配置 INTERNAL_CALL_PUBLIC_KEY_FILE")
        return self

    # ==================== 派生属性 ====================
    @property
    def mysql_dsn(self) -> str:
        """SQLAlchemy async 连接串（asyncmy 驱动）。"""
        pwd = self.mysql_password.get_secret_value()
        return (
            f"mysql+asyncmy://{quote_plus(self.mysql_user, safe='')}:{quote_plus(pwd, safe='')}"
            f"@{self.mysql_host}:{self.mysql_port}/{quote_plus(self.mysql_database, safe='')}"
            f"?charset=utf8mb4"
        )

    @property
    def redis_dsn(self) -> str:
        pwd = self.redis_password.get_secret_value()
        auth = f":{quote(pwd, safe='')}@" if pwd else ""
        return f"redis://{auth}{self.redis_host}:{self.redis_port}/{self.redis_db}"

    @property
    def langsmith_enabled(self) -> bool:
        return self.langsmith_tracing and bool(self.langsmith_api_key.get_secret_value())

    @property
    def auth_enabled(self) -> bool:
        return bool(self.internal_call_public_key_file)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """进程内单例。"""
    return Settings()
