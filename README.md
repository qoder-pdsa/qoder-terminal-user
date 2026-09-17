# qoder-terminal-user

Qoder Terminal 的**用户服务**（Java 25 + Spring Boot 4）：邀请制注册、登录、签发 JWT、记录用户行为。
接口契约：[`api/openapi.yaml`](api/openapi.yaml)。

```bash
make db-up        # 本地 PostgreSQL 容器
make db-migrate   # 执行 Flyway 迁移（服务启动时不会自动迁移）
make dev          # :8084，临时签名密钥，邀请码 demo-invite
make test         # 单元 + 集成测试（Testcontainers，需要 Docker）
make lint
```

```bash
curl -X POST localhost:8084/v1/auth/register -H 'content-type: application/json' \
  -d '{"inviteCode":"demo-invite","username":"pat","password":"s3cret-pass","displayName":"Pat"}'
curl localhost:8084/v1/me -H "Authorization: Bearer <accessToken>"
```

## 能力
| 能力 | 状态 |
|---|---|
| 邀请码注册、登录（BCrypt） | ✅ |
| RS256 JWT + `/.well-known/jwks.json` | ✅ |
| `/v1/me` | ✅ |
| 行为记录：命令 / ASK / 打开面板 | ✅ |
| data / analyst 验签、web 登录页 | 🚧 backlog BL-09 |
| refresh token、登录限流、管理员接口 | 🚧 backlog |

## 数据库
PostgreSQL，独占 `qoder_user` schema；迁移脚本在 `src/main/resources/db/migration/`。
