# 资源分享社区前端（vuejob）

本目录是资源分享社区的 Vue 3 前端，包含用户端与管理端页面。Java 后端位于仓库根目录的 `user8011/`、`admin8012/`，AI 客服服务位于 `ai-agent/`。本仓库当前用于本机部署演示；生产化要求见根目录 `README.md`。

## 功能与技术

- 用户端：注册登录、资源搜索与详情、上传下载、评论、收藏、个人资料、消息和 AI 客服。
- 管理端：仪表盘、资源审核、用户管理、公告管理和管理员管理。
- 技术：Vue 3、Vue Router、Axios、Vite。前端路由检查登录状态；实际访问权限由后端接口负责校验。

## 本机运行

需要 Node.js 20.19+ 或 22.12+，并先启动 Java 后端及其 MySQL、Redis、Nacos 依赖。

```bash
cd vuejob
npm ci
npm run dev
```

默认访问 `http://localhost:5173`。Vite 开发代理将 `/api/*` 转发到用户服务的 `/user8011/*`，将 `/admin-api/*` 转发到管理服务的 `/admin8012/*`；代理配置见 `vite.config.js`。AI 客服页面还需要 Java 服务连接 `ai-agent`。

## 构建与部署范围

运行 `npm run build` 会生成 `dist/` 静态文件。根目录的 Docker Compose **没有部署前端静态文件**；它只编排后端、Agent 和依赖服务。若要把前端作为生产站点部署，需要配置静态文件服务、Vue history 路由回退、HTTPS，以及 `/api` 和 `/admin-api` 的反向代理。`dist/`、`node_modules/` 不应提交到仓库。

当前令牌由前端保存在浏览器本地存储中，适用于本机演示。生产环境需要按根 README 的说明重新评估身份会话和页面安全策略。
