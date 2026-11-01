"""领域异常与统一错误响应。"""

from typing import Any, Optional


class AgentError(Exception):
    """所有业务异常的基类。"""

    code = "agent_error"
    http_status = 500
    message = "服务暂时不可用，请稍后再试"

    def __init__(self, message: Optional[str] = None, *, detail: Any = None):
        super().__init__(message or self.message)
        if message:
            self.message = message
        self.detail = detail

    def to_dict(self) -> dict:
        payload = {"error": {"code": self.code, "message": self.message}}
        if self.detail is not None:
            payload["error"]["detail"] = self.detail
        return payload


class ConfigError(AgentError):
    code = "config_error"
    http_status = 500
    message = "服务配置有误"


class AuthError(AgentError):
    code = "unauthorized"
    http_status = 401
    message = "未授权的调用"


class ValidationError(AgentError):
    code = "invalid_request"
    http_status = 422
    message = "请求参数不合法"


class RateLimitedError(AgentError):
    code = "rate_limited"
    http_status = 429
    message = "请求过于频繁，请稍后再试"


class UpstreamError(AgentError):
    """LLM / 数据库等外部依赖异常。"""

    code = "upstream_error"
    http_status = 503
    message = "依赖服务暂时不可用，请稍后再试"


class NotFoundError(AgentError):
    code = "not_found"
    http_status = 404
    message = "资源不存在"


class ModelRefusedError(AgentError):
    """模型拒绝了请求（内容安全等）。"""

    code = "model_refused"
    http_status = 200
    message = "抱歉，我无法回答这个问题"