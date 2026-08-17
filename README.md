# 资源分享平台（springbootjob）

一个基于 **Spring Cloud Alibaba 微服务架构** 的资源分享社区平台。用户可以上传、下载、搜索资源，通过积分体系、点赞评论进行互动；管理员端提供资源审核、用户管理、公告发布等后台能力；同时集成 **AI 智能客服**，接入通义千问大模型为用户提供基于真实数据的智能问答。

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
- 基于 FastAPI + LangChain 的独立 Python 服务
- 接入通义千问大模型（qwen-max），提供与 ChatGPT 兼容的对话接口
- **Tool Calling 工具链**：大模型可自主调用「资源搜索 / 资源详情 / 用户信息」三个工具直查 MySQL，杜绝幻觉
- 对话历史存储于 Redis（TTL 24h，最多 200 条），保证多轮对话连贯性

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
| Web 框架 | FastAPI | 0.112.0 |
| LLM 框架 | LangChain + langchain-openai | 0.3.13 / 0.2.12 |
| 大模型 | 通义千问 qwen-max（经 OneAPI 代理） | — |
| 数据库 | mysql-connector-python / redis-py | — |
| ASGI 服务 | uvicorn | 0.30.6 |

### 前端（vuejob，独立仓库）
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
├── ai-agent/                  # Python AI 客服服务（端口 8013）
│   ├── main.py                # FastAPI 主程序 + Tool Calling 工具链
│   ├── requirements.txt
│   └── Dockerfile
├── docker-compose.yml         # 全栈容器编排
├── pom.xml                    # 父工程（依赖版本管理）
└── mvnw / mvnw.cmd            # Maven Wrapper
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
| OneAPI | 3000 | 大模型 API 网关（AI 客服依赖） |
| Python | 3.10 | AI 客服运行环境 |

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

启动 Nacos、MySQL、Redis、Sentinel Dashboard、OneAPI。

### 3. 启动后端服务

```bash
# 用户服务（端口 8011）
cd user8011 && mvn spring-boot:run

# 管理服务（端口 8012）
cd admin8012 && mvn spring-boot:run
```

### 4. 启动 AI 客服

```bash
cd ai-agent
pip install -r requirements.txt
python main.py
```

> 启动前请根据 `.env` 文件配置 OneAPI 地址、API Key、模型名及 MySQL/Redis 连接信息。

### 5. 启动前端

前端位于独立仓库 `vuejob`：

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
- `ai-agent/.env` 与 `docker-compose.yml` 中的 OneAPI API Key、数据库密码为敏感信息，**请勿将真实密钥提交到公开仓库**，建议使用环境变量或密钥管理服务
- 文件存储目录默认挂载于 `D:/java/file`，可通过 Docker Compose 的 volumes 配置调整
