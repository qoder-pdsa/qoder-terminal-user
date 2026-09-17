---
trigger: glob
paths:
  - "**/*.java"
---
- Package by feature (`auth`, `user`, `activity`), not by controller/service layers.
- Use constructor injection; DTOs are `record`s; never return entities directly to clients.
- Throw `ApiException` for business errors; `ApiExceptionHandler` converts them to `{"code","message"}`.
- Integration tests use a real PostgreSQL via Testcontainers, never H2.
