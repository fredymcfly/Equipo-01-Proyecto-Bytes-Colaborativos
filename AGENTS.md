# AGENTS.md

Guidance for AI coding assistants (Codex, Claude Code, etc.) working in this repository. The full team rules live in [CONTRIBUTING.md](CONTRIBUTING.md); this file covers what matters most when you write or change code.

## Project overview

FleetControl is a fleet management backend made up of 9 microservices: Java 17, Spring Boot 3.x, PostgreSQL 15, Flyway, OpenFeign and Resilience4j. There are no external APIs. Demo data comes from a built-in seeder that runs under the `demo` Spring profile. See [README.MD](README.md) for the architecture and startup steps.

| Service | Port |
|---|---|
| ms-gateway | 8080 |
| ms-auth | 8081 |
| ms-vehicles | 8082 |
| ms-drivers | 8083 |
| ms-routes | 8085 |
| ms-maintenance | 8086 |
| ms-fuel | 8087 |
| ms-alerts | 8088 |
| ms-dashboard | 8089 |

## Repository layout

```
.github/      GitHub Actions workflows and the PR template
config/       Shared linter configuration (PMD, Spotless)
docs/         Project documentation and architecture notes
docker/       Init scripts (PostgreSQL)
ms-*/         One directory per microservice (src/, Dockerfile, pom.xml)
```

Each service follows the same package layout under `src/main/java/...`: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `config` and `exception`. Tests go under `src/test/java/...`.

## Commands

```bash
# Linters (run inside the service you changed)
mvn spotless:apply      # format the code
mvn spotless:check
mvn checkstyle:check    # Google Style
mvn pmd:check

# Tests
mvn test

# Docker
cp .env.example .env
docker-compose up --build          # whole platform
docker-compose up postgres         # database only
docker-compose down -v             # stop everything and wipe the data
```

## Git workflow

- Work happens on `dev`. `main` only receives stable releases from `dev`.
- Never commit directly to `dev` or `main`.
- Create one branch per task, starting from `dev`, named `type/issue-number-short-description` (for example `feat/12-login-jwt`). Valid types: `feat`, `fix`, `refactor`, `docs`, `chore`, `ci`.
- Open every PR against `dev`. It needs approval from at least one teammate who is not the author.
- Never force-push to a shared branch.

## Commit messages

Follow Conventional Commits: `<type>: <short description>`, written in English and in lowercase.
Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`.

Examples: `feat: add rate limiting filter to gateway`, `fix: validate expired jwt tokens`.

## Pull requests

Use the same convention as commits for the title. The description has three sections: `Work performed`, `Files created/modified` and `Notes`. CONTRIBUTING.md has the details and a full example.

## Code conventions

- Target Java 17. Code must pass `spotless:check`, `checkstyle:check` and `pmd:check` before it is pushed.
- Javadoc: open with a one-line summary of what the method does, without repeating its name. Add `@param`, `@return` and `@throws` only when they tell the reader something the name doesn't. Don't use `@author`, `@version` or `@since`. Skip getters, setters, trivial constructors and tests.
- Document controller endpoints with Swagger/OpenAPI, not Javadoc.
- Write code, comments and commits in English. The team has not formally confirmed this yet.
- Use MapStruct for mapping and Lombok to cut boilerplate.
- Each service owns its database (`<service>_db`) and applies its own Flyway migrations.
- Schema changes are always a new Flyway migration in `src/main/resources/db/migration/` named `V<n>__<description>.sql`. Never edit an applied migration. JPA runs with `ddl-auto=validate`, so every entity needs its migration.

## API contract

- Errors return `error`, `message` and `timestamp`. `details` appears only on `VALIDATION_ERROR`, and `service` only on `SERVICE_UNAVAILABLE`.
- IDs are UUIDs, timestamps are ISO-8601 in UTC, and dates are `YYYY-MM-DD`.
- Pagination uses `page` (zero-based) and `size` (default 20, max 100).
- Money is in EUR with 2 decimals, distances in km, fuel consumption in L/100 km.
- Every service exposes `/actuator/health` and `/swagger-ui.html`.
- Service-to-service calls carry the `X-Internal-Key` header.
- Feign clients use a 2 s connect timeout and a 5 s read timeout, retry once on GET only, and sit behind a Resilience4j circuit breaker.

## Ground rules for assistants

- Never put credentials, secrets or `.env` contents in code or commits. Example values belong in `.env.example` only.
- Stay inside the scope of the task. Each person owns one microservice, so don't change another service unless the task says so.
- Before calling a task done, run the linters and tests for every service you touched.
- Code written with AI gets the same review as any other code: whoever pushes it must understand it.
- If a task conflicts with CONTRIBUTING.md or the requirements document, ask instead of guessing.
