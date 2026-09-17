# qoder-terminal-user — Agent 指南

Java 25 + Spring Boot 4 + Maven。**本 repo 维护 `api/openapi.yaml`**，JWT 的 `iss` / `aud` / claims 也是契约的一部分，
下游 data、analyst（验签）与 web（登录、上报行为）依赖它。

## 架构（按功能分包）
- `auth/` — 注册、登录、签发令牌、JWKS
- `user/` — 用户实体与 `/v1/me`
- `activity/` — 行为记录
- `config/` — 安全、JWT 密钥、时钟
- `common/` — 统一错误与健康检查
- `src/main/resources/db/migration/` — Flyway 迁移脚本

## 规则
- **数据库变更只能通过新增 Flyway 脚本**（`V<n>__<描述>.sql`），禁止修改已合入的脚本。
- **启动时禁止自动迁移或自动建表**：`spring.flyway.enabled=false`、`ddl-auto=none` 不得改动；迁移只走 `make db-migrate`。
- user_id 一律取自 JWT `sub`，不信任请求体中的用户标识。
- 登录失败不区分“用户不存在”和“密码错误”。
- 密钥、邀请码、数据库密码只从环境变量读取；生产环境禁止 `USER_JWT_ALLOW_EPHEMERAL=true`。
- 先写测试：接口行为用 `UserServiceIntegrationTest`（真实 PostgreSQL），纯逻辑用单元测试。
- 编译告警视为错误。

## 命令
- `make test` / `make lint` / `make db-up` / `make db-migrate` / `make dev`
