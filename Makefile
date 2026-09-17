# 需要 JDK 25：未设置 JAVA_HOME 时尝试 Homebrew 的 openjdk@25
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 25 2>/dev/null || echo /opt/homebrew/opt/openjdk@25)
export JAVA_HOME

DB_CONTAINER := qoder-postgres
JAR := target/qoder-terminal-user-0.0.1-SNAPSHOT.jar

.PHONY: test lint lint-api build dev db-up db-down db-migrate image

test:
	./mvnw -q -B test
# 编译告警即失败（-Xlint:all -Werror）+ OpenAPI 校验
lint: lint-api
	./mvnw -q -B -DskipTests test-compile
lint-api:
	npx --yes @redocly/cli@1 lint --config api/redocly.yaml api/openapi.yaml
build:
	./mvnw -q -B -DskipTests package

# 本地 PostgreSQL（与 web repo 的 docker-compose 使用同一镜像版本）
db-up:
	docker start $(DB_CONTAINER) 2>/dev/null || docker run -d --name $(DB_CONTAINER) \
		-e POSTGRES_DB=qoder -e POSTGRES_USER=qoder -e POSTGRES_PASSWORD=qoder -p 5432:5432 postgres:17-alpine
db-down:
	docker stop $(DB_CONTAINER)

# 数据库迁移：独立入口，服务启动时不会自动迁移（AutoWonder QA 数据库步骤使用）
db-migrate: build
	java -jar $(JAR) --spring.profiles.active=migrate

# 本地开发：临时签名密钥 + 演示邀请码
dev: build
	USER_JWT_ALLOW_EPHEMERAL=true USER_INVITE_CODE=$${USER_INVITE_CODE:-demo-invite} java -jar $(JAR)

image:
	docker build -t qoder-terminal-user:latest .
