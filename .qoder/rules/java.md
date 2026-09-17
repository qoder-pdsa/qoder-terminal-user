---
trigger: glob
paths:
  - "**/*.java"
---
- 按功能分包（`auth`、`user`、`activity`），不按 controller/service 分层分包。
- 构造器注入；DTO 用 `record`；实体不直接返回给客户端。
- 业务错误抛 `ApiException`，由 `ApiExceptionHandler` 统一转换为 `{"code","message"}`。
- 集成测试用 Testcontainers 的真实 PostgreSQL，不用 H2。
