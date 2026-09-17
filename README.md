# qoder-terminal-user

The **user service** (Java 25 + Spring Boot 4) for Qoder Terminal: invite-only registration, login, JWT issuing, and user activity history.
API contract: [`api/openapi.yaml`](api/openapi.yaml).

```bash
make db-up        # local PostgreSQL container
make db-migrate   # run Flyway migrations (the service never migrates on startup)
make dev          # :8084, ephemeral signing key, invite code demo-invite
make test         # unit + integration tests (Testcontainers, requires Docker)
make lint
```

```bash
curl -X POST localhost:8084/v1/auth/register -H 'content-type: application/json' \
  -d '{"inviteCode":"demo-invite","username":"pat","password":"s3cret-pass","displayName":"Pat"}'
curl localhost:8084/v1/me -H "Authorization: Bearer <accessToken>"
```

## Capabilities
| Capability | Status |
|---|---|
| Invite-code registration and login (BCrypt) | ✅ |
| RS256 JWT + `/.well-known/jwks.json` | ✅ |
| `/v1/me` | ✅ |
| Activity history: commands / ASK / panel opens | ✅ |
| Token verification in data / analyst and a web login page | 🚧 backlog BL-09 |
| Refresh tokens, login rate limiting, admin APIs | 🚧 backlog |

## Database
PostgreSQL with the `qoder_user` schema owned exclusively by this service; migration scripts live in `src/main/resources/db/migration/`.
