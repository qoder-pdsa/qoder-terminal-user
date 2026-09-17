# qoder-terminal-user — Agent Guide

Java 25 + Spring Boot 4 + Maven. **This repo owns `api/openapi.yaml`**, and the JWT `iss` / `aud` / claims are part of the contract;
downstream data and analyst (token verification) and web (login, activity reporting) depend on it.

## Architecture (packaged by feature)
- `auth/` — registration, login, token issuing, JWKS
- `user/` — user entity and `/v1/me`
- `activity/` — activity history
- `config/` — security, JWT keys, clock
- `common/` — unified errors and health check
- `src/main/resources/db/migration/` — Flyway migration scripts

## Rules
- **Database changes only through new Flyway scripts** (`V<n>__<description>.sql`); never modify merged scripts.
- **No automatic migrations or DDL on startup**: keep `spring.flyway.enabled=false` and `ddl-auto=none`; migrations run only via `make db-migrate`.
- user_id always comes from the JWT `sub`; never trust user identifiers in request bodies.
- Login failures never distinguish "unknown user" from "wrong password".
- Keys, invite codes, and database passwords come only from environment variables; `USER_JWT_ALLOW_EPHEMERAL=true` is forbidden in production.
- Tests first: API behavior goes in `UserServiceIntegrationTest` (real PostgreSQL), pure logic in unit tests.
- Compiler warnings are errors.

## Commands
- `make test` / `make lint` / `make db-up` / `make db-migrate` / `make dev`
