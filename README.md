# EDSS Self Backend v1

Business Operations Platform backend — Spring Boot 3 modular monolith following the specifications in `01-project-vision.md` through `06-claude-code-master-prompt.md`.

## Stack

- Java 17, Spring Boot 3.2, Spring Modulith 1.1
- PostgreSQL 14 (schema-per-module), Redis 7
- Flyway migrations, JJWT for JWT, BCrypt (strength 12) for passwords, TOTP (RFC 6238) for 2FA
- Springdoc OpenAPI

## Local run

```
cp .env.example .env      # then edit secrets
docker-compose up -d
./mvnw spring-boot:run
```

App boots on `http://localhost:8080`.

- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`
- Mailhog UI (dev SMTP): `http://localhost:8025`

## Module map

Functional modules (v1): `identity`, `projects`, `finance`, `commitments`, `knowledge`, `notifications`.

Skeleton modules (module boundary reserved, no code yet): `organization`, `relationship`, `sales`, `communication`, `governance`, `integrations`, `ai`.

Each module owns its own Postgres schema. Cross-module writes are forbidden — modules communicate via versioned domain events routed through a per-module transactional outbox (see `src/main/java/com/edss/shared/events/`). This is the seam a future Kafka/microservice split will use, without publisher-side code changes.

## API

- Base path: `/api/v1`
- Wire format: snake_case globally, with the `identity` `User` DTO using camelCase for `avatarUrl`, `primaryRole`, `hasBothRoles` to match the frontend `userSchema`.
- Errors: `{ code, message, details? }` with the code enum locked to the frontend `ApiErrorCode` type.

The wire contract mirrors the frontend Zod schemas at `../External Frontend/self v1/packages/validation/` and MSW handlers at `../External Frontend/self v1/apps/dashboard/mocks/handlers/`.

## Tests

```
./mvnw test
```

Includes `ModuleStructureTest` (Spring Modulith boundary verify), unit tests, Testcontainers-based integration tests, and superset-tolerant contract tests.
