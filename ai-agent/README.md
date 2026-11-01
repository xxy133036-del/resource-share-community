# HC1 AI 客服 Agent（ai-agent）

弘创资源平台的 AI 客服「小弘」。独立 Python 服务，基于 **FastAPI + LangGraph + DeepSeek**，
替代原 `springbootjob/ai-agent` 中「单文件 + 正则抠 JSON」的旧实现。

本目录现已随 Java 后端和 Vue 前端一起收录在 `resource-share-community` 仓库中。根目录 `README.md` 说明了本机演示范围及生产环境需要完成的配置、网络和鉴权改造。运行时的 `.env`、密钥文件、日志和虚拟环境不随仓库发布。

## 相比旧实现的关键改进

| 维度 | 旧实现 | 现在 |
| --- | --- | --- |
| 工具调用 | 正则从模型输出抠 JSON，模型加 Markdown 就失效 | 模型**原生 tool_calls**，自动生成 JSON Schema |
| 并发 | 同步 mysql/redis 写在 `async def` 里，慢查询阻塞整个事件循环 | SQLAlchemy async + asyncmy、`redis.asyncio`，真异步 |
| 越权 | 无条件信任请求体里的 `user_id` | `get_user_profile` **不接受 user_id 入参**，身份由服务端注入 |
| 观测 | 只有 print 日志 | LangSmith trace + structlog JSON 日志 + SSE 思考过程 |
| 流式 | 无 | SSE 主推，含 `thinking`/`tool`/`token` 事件 |
| 业务规则 | 硬编码在 System Prompt 字符串里 | `knowledge/knowledge.md`，改规则不动代码 |
| 安全 | 密钥硬编码在代码与 compose 中 | 全部走环境变量，启动 fail fast |
| 测试 | 无 | 51 项 pytest（单元 + 集成） |

## 目录结构

```
app/
├── main.py                 # FastAPI 装配、生命周期、中间件、异常处理
├── api/
│   ├── chat.py             # /v1/chat/stream（主）+ /v1/chat/completions（兼容）
│   ├── health.py           # /healthz /readyz
│   └── deps.py
├── core/
│   ├── settings.py         # pydantic-settings，唯一配置入口
│   ├── logging.py          # structlog JSON + 脱敏
│   ├── security.py         # RSA-SHA256 服务间验签
│   ├── reasoning.py        # reasoning_content 补丁（推理模型支持）
│   ├── llm.py              # ChatOpenAI 工厂
│   └── exceptions.py
├── agent/
│   ├── graph.py            # LangGraph 状态机
│   └── prompts.py          # 分层 Prompt
├── tools/registry.py       # 3 个只读工具
├── infra/
│   ├── mysql.py            # 异步连接池 + 事务入口
│   ├── redis.py            # 限流计数
│   └── session_store.py    # 会话事件日志（追加写 + 压缩摘要）
├── schemas/chat.py         # DTO + SSE 事件体
└── services/chat_service.py
db/migrations/              # 编号 SQL 迁移（scripts/migrate.py 执行）
knowledge/knowledge.md      # 业务规则（可热改）
scripts/
├── probe_model.py          # 模型能力探针
├── smoke_test.py           # 端到端冒烟验收（28 项）
├── migrate.py              # 数据库迁移执行器
└── run.ps1                 # 本地启动
tests/                      # unit + integration
logs/                       # 运行时日志（已 gitignore）
```

## 快速开始

```bash
# 1. 依赖
python -m venv venv
.\venv\Scripts\pip install -r requirements.txt

# 2. 配置（复制模板后填入真实值，.env 已被 gitignore）
copy .env.example .env

# 3. 环境探针（换模型/升级 SDK 后建议先跑）
python scripts/probe_model.py

# 4. 建表（幂等，可重复执行）
python scripts/migrate.py

# 5. 启动
.\scripts\run.ps1        # 或 python -m app.main

# 5. 测试
.\venv\Scripts\python -m pytest tests/ -q
```

## 接口

### 流式（主推）

`POST /v1/chat/stream`，`text/event-stream`：

```
data: {"type":"status","text":"正在连接小弘…","conversation_id":"conv_xxx"}
data: {"type":"status","text":"正在理解你的问题…"}
data: {"type":"thinking","text":"用户问的是签到规则…"}       # 推理模型的思考过程
data: {"type":"tool","name":"search_resources","args":{...}}
data: {"type":"token","text":"找到"}
data: {"type":"token","text":" 3 个"}
data: {"type":"done","text":"...","usage":{...},"message_id":"msg_xxx"}
data: [DONE]
```

事件类型：`status` / `thinking` / `tool` / `token` / `done` / `error`。

### 非流式（兼容旧契约）

`POST /v1/chat/completions`，响应体与 OpenAI 一致，Java 侧现有 DTO 无需改动。

### 健康检查

- `GET /healthz`：进程存活
- `GET /readyz`：MySQL / Redis 就绪 + LangSmith 开关状态

## 配置项

见 `.env.example`。关键项：

| 变量 | 说明 |
| --- | --- |
| `DEEPSEEK_API_KEY` | 必填，缺失则启动即失败 |
| `DEEPSEEK_MODEL` | 默认 `deepseek-flash` |
| `AGENT_ENGINE` | 目前仅 `graph`（LangGraph 状态机） |
| `LLM_MAX_TOKENS` | 默认 4096。**推理模型的思考会占用此预算**，调小可能导致正文被截断 |
| `AGENT_MAX_ITERATIONS` | 工具调用最大轮次，超出走兜底 |
| `LANGSMITH_*` | 观测（LangSmith），未配置则自动关闭，不影响业务 |
| `INTERNAL_CALL_PUBLIC_KEY_FILE` | Java RSA 验签公钥文件；生产必须配置，只有本地开发可留空 |

## 与 SpringBoot 对接

**安全约定**：`user_id` 必须由 Java 侧从 JWT 解析后放入签名体，**前端不得直接指定**。

Java 侧需实现签名（与 `app/core/security.py` 对称）。**签名覆盖请求行**
（method + path + query）与 body，因此 GET 类接口同样受保护，无法靠省略 body 绕过。

第一步，构造规范化请求串（以 `\n` 分隔，末段为 body 的 SHA-256 十六进制）：

```
canonical = f"{METHOD}\n{PATH}\n{CANONICAL_QUERY}\n{sha256_hex(body)}"
```

- `METHOD` / `PATH`：大写方法与**不含 query** 的路径，如 `GET` / `/v1/sessions`
- `CANONICAL_QUERY`：按**参数名 + 参数值**升序排序，键和值分别百分号编码后再拼接（与原始顺序无关），无参数则为空串
- body 为空时，取其空字节串的 SHA-256

第二步，拼接时间戳与请求 ID 后签名：

```
payload = f"{timestamp}.{requestId}.{sha256_hex(canonical)}"
X-Sign  = Base64(RSA_SHA256_sign(private_key, payload))
```

请求头：`X-Timestamp`（秒级时间戳，服务端容忍 300s 偏差）、`X-Request-Id`（追踪/幂等）、`X-Sign`。Java 容器挂载 RSA 私钥，Python 只挂载公钥。

Java 侧参考实现见 `springbootjob` 的 `AiAgentSigner`；跨语言一致性由 `AiAgentSignerTest`
与 `AiAgentSignatureE2ETest` 保证。

## 实测结论（deepseek-flash）

- 支持原生 tool calling 与流式 SSE
- **是推理模型**：会返回 `reasoning_content`，思考会消耗输出预算，故 `LLM_MAX_TOKENS` 默认为 4096
- 支持 Prompt 缓存：知识库放在 Prompt 前部可显著降低输入成本（实测第二次命中 512 tokens）
- 上下文 1M tokens，支持图片输入

## 部署

```bash
docker build -t hc1-ai-agent .
docker run --env-file .env -p 8013:8013 hc1-ai-agent
```

镜像非 root 运行，带 `/healthz` 健康检查。
生产建议：MySQL 使用**只读账号**，密钥接 Nacos 配置中心。
