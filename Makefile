# Requires JDK 25: falls back to Homebrew's openjdk@25 when JAVA_HOME is not set
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 25 2>/dev/null || echo /opt/homebrew/opt/openjdk@25)
export JAVA_HOME

DB_CONTAINER := qoder-postgres
JAR := target/qoder-terminal-user-0.0.1-SNAPSHOT.jar

.PHONY: test lint lint-api build dev db-up db-down db-migrate image

test:
	./mvnw -q -B test
# Compiler warnings fail the build (-Xlint:all -Werror) + OpenAPI lint
lint: lint-api
	./mvnw -q -B -DskipTests test-compile
lint-api:
	npx --yes @redocly/cli@1 lint --config api/redocly.yaml api/openapi.yaml
build:
	./mvnw -q -B -DskipTests package

# Local PostgreSQL (same image version as the web repo's docker-compose)
db-up:
	docker start $(DB_CONTAINER) 2>/dev/null || docker run -d --name $(DB_CONTAINER) \
		-e POSTGRES_DB=qoder -e POSTGRES_USER=qoder -e POSTGRES_PASSWORD=qoder -p 5432:5432 postgres:17-alpine
db-down:
	docker stop $(DB_CONTAINER)

# Database migration: separate entry point; the service never migrates on startup (used by the AutoWonder QA database steps)
db-migrate: build
	java -jar $(JAR) --spring.profiles.active=migrate

# Local development: ephemeral signing key + demo invite code
dev: build
	USER_JWT_ALLOW_EPHEMERAL=true USER_INVITE_CODE=$${USER_INVITE_CODE:-demo-invite} java -jar $(JAR)

image:
	docker build -t qoder-terminal-user:latest .
