# 资源分享平台（springbootjob）

一个基于 **Spring Cloud Alibaba 微服务架构** 的资源分享社区平台。用户可以上传、下载、搜索资源，通过积分体系、点赞评论进行互动；管理员端提供资源审核、用户管理、公告发布等后台能力；同时集成 **AI 智能客服**，基于 FastAPI + LangGraph + DeepSeek 构建，作为独立 Python Agent 服务为用户提供基于真实数据的智能问答。

> **部署范围：本仓库当前是面向开发者本机运行和功能演示的版本，不是生产部署包。** 仓库包含 Java 后端、Vue 前端和 Python Agent。Compose 中的本机路径、演示用基础设施配置和默认账号策略是为了便于本机联调；请勿将当前配置直接暴露到公网或用于生产数据。

## 本机演示约定与生产改造说明

### 当前版本的本机约定

- `docker-compose.yml` 是本机全栈演示编排：包含 MySQL、Redis、Nacos、Sentinel、两个 Java 服务和 Python Agent。Agent 的 build context、`.env` 和知识库挂载使用当前 Windows 工作区路径；换电脑时需要改成新机器上的实际路径。
- Compose 与 Spring 配置中的数据库、Nacos、Druid 等本机演示账号/连接参数用于本地快速启动。请只在可信的本机或隔离演示网络中使用，并使用演示数据。
- `user8011` 目前通过 `X-Internal-Call: true` 区分管理端经 Feign 发起的内部调用，以便复用用户服务中的管理接口；这是本机演示阶段的简化实现。该标记本身不是强身份凭证，因此不能把这套服务直接暴露到不可信网络。
- `vuejob/` 是前端工程，`ai-agent/` 是 Python Agent 工程。本机现有 Compose 仍通过绝对路径 `D:\python\workspace\springbootjobagent` 接入 Agent；从 GitHub 克隆到其他位置后，须将 Compose 中的 build context、env_file 和知识库挂载路径指向本仓库的 `ai-agent/` 所在位置。
- `.env`、RSA 私钥和真实 API Key 属于本机敏感配置，不应提交到仓库。`.env.example` 仅提供变量名与配置说明。

### 若要部署到生产环境，至少需要完成

1. **统一配置管理**：把 Java、Python、Compose 中的数据库/Nacos/Redis/服务地址和运行参数归一到一套可按环境覆盖的配置方案（例如环境变量或 Nacos 配置），移除绑定个人工作区的绝对路径；开发、测试、生产配置分开维护。
2. **替换演示账号与权限边界**：为应用服务创建独立的最小权限数据库账号，设置强口令并通过密钥管理系统或容器 Secret 注入；更改 Nacos、Druid、Sentinel 等管理入口的账号策略。禁止生产服务使用仓库中的演示默认值。
3. **收紧网络入口**：仅通过 HTTPS 网关对外提供必要的前端/API 入口；MySQL、Redis、Nacos 控制台、Sentinel、Druid 和 Agent 管理接口限制在内网或管理网段，移除不必要的宿主机端口映射。
4. **替换内部调用简化方案**：以经过认证的服务间身份校验取代单独依赖 `X-Internal-Call` 标记，并限制用户服务的网络可达范围；验证未认证的外部请求不能进入管理员调用路径。
5. **补齐生产运行保障**：配置 TLS、健康检查与合理的容器重启策略；限制 Actuator/Druid 可见范围；关闭 SQL 明细日志；设置备份恢复、日志保留、监控告警、资源限制和密钥轮换流程。
6. **完成生产验收**：在目标 Linux/容器环境验证镜像构建、数据库迁移、备份恢复、登录与权限、资源上传/下载、AI SSE 联调、故障恢复和并发容量，再部署真实数据。

以上是生产化改造方向，不代表这些配置已经具备生产安全性。本项目当前用途仅为本机部署演示。

## ✨ 功能特性

### 用户端（user8011）
- 用户注册 / 登录（JWT 无状态认证）
- 资源上传 / 下载（积分结算，单文件 ≤ 10MB）
- 资源搜索（关键词 + 时间范围 + 排序）
- 点赞 / 收藏状态批量查询
- 两级评论（评论 + 回复）
- 消息中心（点赞、评论、系统公告，支持已读标记）
- 每日签到（积分奖励，连续签到加成）
- 个人中心（资料修改、头像上传、我的发布）

### 管理端（admin8012）
- 管理员登录（JWT 认证）
- RBAC 细粒度权限控制（用户管理 / 资源管理 / 公告管理 / 管理员管理）
- 资源审核工作流（待审核 → 审核通过 / 驳回，支持下载资源内容进行审核）
- 用户管理（冻结 / 解冻、详情查看）
- 管理员账号管理（增删改查、角色分配）
- 公告管理（发布后通过 Redis Pub/Sub 实时推送）

### AI 智能客服（ai-agent）
- 独立的 Python Agent 服务（FastAPI + LangGraph），与 Java 服务通过 HTTP 对接
- 接入 DeepSeek 官方模型（deepseek-flash，推理模型），提供 SSE 流式与 OpenAI 兼容两种对话接口
- **原生 Tool Calling**：大模型可自主调用「资源搜索 / 资源详情 / 当前用户信息」三个只读工具直查 MySQL，杜绝幻觉
- **会话事件日志**：对话以追加写事件日志持久化到 MySQL，配套摘要压缩，保证多轮对话连贯且可回溯
- **LangSmith 观测**：完整记录 LangGraph 执行链路与工具调用，便于排查与调优
- **非对称服务间鉴权**：Java 用 RSA 私钥签名 `method + path + query + body`，Python 仅用公钥验签；私钥不进入 Python 容器

## 🛠 技术栈

### 后端（Java）
| 分类 | 技术 | 版本 |
| --- | --- | --- |
| 语言 | Java | 8 |
| 框架 | Spring Boot | 2.3.2 |
| 微服务 | Spring Cloud / Spring Cloud Alibaba | Hoxton.SR8 / 2.2.5 |
| 服务治理 | Nacos（注册中心 + 配置中心） | — |
| 流量控制 | Sentinel（限流 + 熔断降级，规则持久化到 Nacos） | 1.8.0 |
| 服务调用 | OpenFeign + Sentinel | — |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL 8 | — |
| 连接池 | Druid（慢 SQL 监控 + SQL 防火墙） | 1.2.28 |
| 缓存 | Redis（缓存 + Pub/Sub） | — |
| 认证 | JWT（auth0 java-jwt） | 3.18.2 |
| 工具 | Hutool / Lombok | — |
| 监控 | Spring Boot Actuator + Druid Monitor | — |

### AI 服务（Python）
| 分类 | 技术 | 版本 |
| --- | --- | --- |
| 语言 | Python | 3.10 |
| Web 框架 | FastAPI | 0.115.6 |
| Agent 编排 | LangGraph | 0.2.62 |
| LLM 框架 | LangChain + langchain-openai | 0.3.14 / 0.2.14 |
| 大模型 | DeepSeek deepseek-flash（官方 API，推理模型） | — |
| 观测 | LangSmith | 0.2.11 |
| 数据库 | SQLAlchemy async + asyncmy / redis | — |
| ASGI 服务 | uvicorn | 0.34.0 |

### 前端（vuejob）
Vue 3 + Vite + Vue Router + Axios

### 部署
Docker + Docker Compose（全栈容器化）

## 🏗 系统架构

```
                    ┌─────────────────────────────┐
                    │         前端 (Vue 3)          │
                    └──────────────┬──────────────┘
                                   │ HTTP
            ┌──────────────────────┼──────────────────────┐
            │                      │                      │
     ┌──────▼──────┐        ┌──────▼──────┐        ┌──────▼──────┐
     │  user8011   │◄──────►│  admin8012  │        │  ai-agent   │
     │  用户服务    │  Feign │  管理服务    │        │  AI 客服     │
     │  (8011)     │        │  (8012)     │        │  (8013)     │
     └──────┬──────┘        └──────┬──────┘        └──────┬──────┘
            │                      │                      │
            └──────────────┬───────┴──────────────┬───────┘
                           │                      │
              ┌────────────▼─────────┐  ┌─────────▼─────────┐
              │   Nacos 集群 (注册中心) │  │  Redis (缓存/Pub-Sub) │
              │   8848/8849/8850      │  │       6379         │
              └──────────────────────┘  └───────────────────┘
                           │
              ┌────────────▼─────────┐   ┌───────────────────┐
              │  MySQL (hc1db_user / │   │  Sentinel Dashboard │
              │        hc1db_admin)   │   │       8080         │
              └──────────────────────┘   └───────────────────┘
```

## 📁 目录结构

```
springbootjob/
├── vuejob/                    # Vue 3 前端（开发端口 5173）
├── ai-agent/                  # Python AI 客服（端口 8013）
├── admin8012/                 # 管理端微服务（端口 8012）
│   └── src/main/java/com/hc1/admin8012/
│       ├── controller/        # 控制层（登录、资源审核、用户/管理员/公告管理）
│       ├── service/           # 业务层
│       ├── mapper/            # 持久层（MyBatis-Plus）
│       ├── bean/              # 实体类 / DTO
│       ├── config/            # 配置类（Feign 拦截器等）
│       └── util/              # 工具类
├── user8011/                  # 用户端微服务（端口 8011）
│   └── src/main/java/com/hc1/user8011/
│       ├── controller/        # 控制层（登录、文件、评论、消息、AI 对话）
│       ├── service/           # 业务层
│       ├── mapper/            # 持久层
│       ├── bean/              # 实体类
│       ├── ai/                # AI 客服相关
│       └── config/            # 拦截器、Redis 等配置
├── docker-compose.yml         # 全栈容器编排（含 ai-agent 服务）
├── .env.example               # compose 变量模板（签名密钥等）
├── pom.xml                    # 父工程（依赖版本管理）
└── mvnw / mvnw.cmd            # Maven Wrapper

# 本机 Compose 目前沿用原开发机的 Agent 绝对路径；克隆后按上文调整。
```

## 🚀 快速开始

### 环境要求

| 组件 | 版本 / 端口 | 说明 |
| --- | --- | --- |
| JDK | 1.8+ | 后端运行环境 |
| Maven | 3.6+ | 构建工具 |
| MySQL | 8.x（3306） | 需创建 `hc1db_user`、`hc1db_admin` 两个库 |
| Redis | 5.x+（6379） | 缓存 + Pub/Sub |
| Nacos | 集群（8848/8849/8850） | 服务注册与配置 |
| Sentinel Dashboard | 8080 | 流量控制面板 |
| Python | 3.10 | AI 客服运行环境（ai-agent/） |

### 1. 初始化数据库

在 MySQL 中创建两个数据库，并根据 `bean` 目录下的实体类（`@TableName` 注解）创建对应数据表：

```sql
CREATE DATABASE IF NOT EXISTS hc1db_user DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS hc1db_admin DEFAULT CHARACTER SET utf8mb4;
```

> 数据表结构可参考实体类定义，核心表包括：
> - `hc1db_user`：`user_info`（用户）、`file_info`（资源）、`comment`（评论）、`message_info`（消息）
> - `hc1db_admin`：`sys_admin`（管理员）、`sys_notice`（公告）、`sys_privilege`（角色权限）

### 2. 启动基础设施

启动 Nacos、MySQL、Redis、Sentinel Dashboard。

### 3. 启动后端服务

```bash
# 用户服务（端口 8011）
cd user8011 && mvn spring-boot:run

# 管理服务（端口 8012）
cd admin8012 && mvn spring-boot:run
```

### 4. 启动 AI 客服

AI 客服代码位于本仓库的 `ai-agent/`；在项目根目录执行：

```bash
cd ai-agent
python -m venv venv
.\venv\Scripts\pip install -r requirements.txt
copy .env.example .env      # 填入 DeepSeek / LangSmith / MySQL / Redis 配置
.\venv\Scripts\python scripts\migrate.py   # 建表（幂等）
.\scripts\run.ps1          # 或 python -m app.main
```

> 启动前请在 `.env` 中配置 DeepSeek API Key、LangSmith（可选）以及 MySQL / Redis 连接信息。
> 详细说明见 `ai-agent/README.md`。当前 Compose 沿用原本机路径，使用本仓库中的 Agent 构建时需先调整上述三个路径。

### 5. 启动前端

前端位于本仓库的 `vuejob/`：

```bash
cd vuejob
npm install
npm run dev
```

## 🐳 Docker Compose 部署

项目提供 `docker-compose.yml`，可一键启动三个服务：

```bash
docker-compose up -d --build
```

服务与端口映射：

| 服务 | 容器名 | 端口 |
| --- | --- | --- |
| 管理服务 | hc1admin8012 | 8012 |
| 用户服务 | hc1user8011 | 8011 |
| AI 客服 | ai-agent | 8013 |

## 💡 核心亮点

- **跨服务鉴权**：管理服务通过自定义 `FeignRequestInterceptor` 将 JWT 注入请求头，用户服务端通过拦截器识别内部调用标记，实现用户端与管理端共用一套接口，避免重复开发
- **RBAC 权限控制**：基于角色-权限表的动态权限模型，新增角色只需配置映射，无需改动业务代码
- **Sentinel 多层流控**：搜索接口 QPS 限流 + Feign 熔断降级，限流规则持久化到 Nacos，支持运行时动态调整
- **AI 客服防幻觉**：通过 LangChain Tool Calling 让大模型直查真实 MySQL 数据，约束模型只依据工具返回结果作答
- **实时公告推送**：基于 Redis Pub/Sub 实现公告毫秒级推送，替代传统轮询方案
- **全栈容器化**：Java 服务 + Python 服务 + 中间件通过 Docker Compose 统一编排

## ⚠️ 注意事项

- 数据库连接、Nacos 地址等配置位于各服务 `application.yaml`，请按实际环境修改
- Python 工程 `.env` 中的 DeepSeek API Key、LangSmith Key，以及 Compose 挂载的 RSA 私钥文件均为敏感信息，**请勿提交到公开仓库**，建议使用 Docker secrets 或密钥管理服务

首次启动前，在 Java 项目根目录生成 JWT 和 Java→Python 调用两组 RSA 密钥对，并在 `.env` 中配置四个文件路径。两组密钥使用同一 RS256 算法，但分开保存，避免跨用途复用密钥：

```powershell
New-Item -ItemType Directory -Force secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/ai-agent-private.pem
openssl pkey -in secrets/ai-agent-private.pem -pubout -out secrets/ai-agent-public.pem
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/jwt-private.pem
openssl pkey -in secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem
```

Compose 把 JWT 私钥和公钥挂载给两个 Java 服务，用于签发和验证 JWT；Java→Python 私钥只挂给 `hc1user8011`，对应公钥只挂给 `ai-agent`。Python 不需要任何私钥。

两条签名链路现在都使用 RSA。Java JWT 使用 RS256；Java→Python 请求使用 RSA-SHA256 对规范化 HTTP 请求签名。若在宿主机分别启动服务，Java 进程设置 `JWT_PRIVATE_KEY_FILE`、`JWT_PUBLIC_KEY_FILE` 和 `AI_AGENT_PRIVATE_KEY_FILE`；Python `.env` 只设置 `INTERNAL_CALL_PUBLIC_KEY_FILE`。Python 依赖通过 `pip install -r requirements.txt` 安装。
- 文件存储目录默认挂载于 `D:/java/file`，可通过 Docker Compose 的 volumes 配置调整
