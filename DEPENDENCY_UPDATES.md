# Dependency Update Guide

This guide explains how to check for, apply, and test dependency updates for both the frontend (Next.js) and backend (Spring Boot / Kotlin).

---

## Frontend (Next.js / npm)

Located at: `gripl/gripl-frontend/`

### 1. Check for outdated packages

```bash
cd gripl/gripl-frontend
npm outdated
```

The output shows three columns:
- **Current** — installed version (MISSING = not installed yet)
- **Wanted** — latest version within your `package.json` range
- **Latest** — absolute latest on npm

### 2. Install / update within existing ranges

```bash
npm install
```

This resolves all `MISSING` packages and updates within the `^` ranges defined in `package.json`.

### 3. Check for security vulnerabilities

```bash
npm audit
```

Apply safe fixes (no breaking changes):
```bash
npm audit fix
```

Only use `--force` if you understand the impact — it may introduce breaking changes.

### 4. Manual major version updates

For packages where **Wanted** < **Latest** (major version jumps), edit `package.json` manually and update the version, then run `npm install` again.

**Risk levels for this project:**

| Package | Risk | Notes |
|---|---|---|
| `@radix-ui/*`, `recharts`, `swagger-ui-react` etc. | Low | Patch/minor, rarely breaking |
| `lucide-react`, `react-apexcharts` | Medium | Check changelog |
| `js-yaml` | Medium | v5 has breaking API changes vs v4 |
| `next` | High | Read the Next.js upgrade guide before updating |
| `react` / `react-dom` | High | React 19 has breaking changes |

### 5. Build and test

Always test locally before committing:

```bash
docker compose -f docker-compose.local.yml build --no-cache gripl-frontend
docker compose -f docker-compose.local.yml up gripl-frontend
```

Open [http://localhost:3000](http://localhost:3000) and verify the app works.

---

## Backend (Spring Boot / Kotlin / Maven)

Located at: `gripl/gripl-backend/`

### 1. Check for outdated dependencies

```bash
cd gripl/gripl-backend
mvn versions:display-dependency-updates
```

This lists all transitive dependencies. Focus only on what is directly defined in `pom.xml`.

For plugin updates:
```bash
mvn versions:display-plugin-updates
```

For property updates (e.g. `kotlin.version`):
```bash
mvn versions:display-property-updates
```

### 2. Apply updates

Edit `pom.xml` manually and change the `<version>` tag of the desired dependency.

**Risk levels for this project:**

| Package | Risk | Notes |
|---|---|---|
| `picocli`, `postgresql` | Low | Patch updates, safe |
| `camunda-*` | Low–Medium | Minor versions generally stable |
| `playwright` | Medium | Larger jumps may change browser API |
| `kotlin-logging-jvm` | Medium | Major version changed API |
| `langchain4j-*` | High | API changes frequently between minor versions |
| `flyway-core` | High | 3+ major versions behind; migration scripts may need adjustment |
| `springdoc-openapi` | High | v3.x requires compatible Spring Boot version |
| `kotlin.version` | Medium | Must match what dependencies were compiled with |
| `spring-boot-starter-parent` | High | Controls many transitive versions; test thoroughly |

### 3. Verify the build compiles

```bash
mvn compile -q
```

If this fails, read the error carefully — it usually points to a Kotlin version mismatch or an API change.

### 4. Run tests

```bash
mvn test
```

Note: Integration tests that require a database connection will fail if no PostgreSQL instance is running locally. This is expected in a standalone environment — use Docker for full integration testing.

### 5. Build and test with Docker

Always build with `--no-cache` to avoid using stale layers:

```bash
docker compose -f docker-compose.local.yml build --no-cache gripl-backend
docker compose -f docker-compose.local.yml up gripl-backend
```

The backend is available at [http://localhost:8000](http://localhost:8000).

---

## Full Stack Rebuild (both services)

To rebuild everything from scratch:

```bash
docker compose -f docker-compose.local.yml build --no-cache
docker compose -f docker-compose.local.yml up
```

Services and ports:
- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **Backend**: [http://localhost:8000](http://localhost:8000)
- **PostgreSQL**: `localhost:5432`

---

## Recommended Update Strategy

1. **Update in small batches** — never update everything at once
2. **Patch versions first**, then minor, then major
3. **Test after each batch** before moving to the next
4. **One commit per batch** with a clear message following [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
5. **Read changelogs** for any major version bump before applying it

### Commit message format

```
fix(deps): bump <scope> dependencies

- package-a: x.x.x -> y.y.y
- package-b: x.x.x -> y.y.y
```

---

## Known Skipped Updates

| Package | Reason |
|---|---|
| `springdoc-openapi` 2.8.9 → 3.0.3 | `BeanDefinitionOverrideException` with Spring Boot 3.5.0 — revisit when a compatible version is released |
| `react` / `react-dom` 18 → 19 | Breaking changes require migration effort |
| `next` 15 → 16 | Major release, requires thorough testing and migration guide review |
| `flyway-core` 9 → 12 | Three major versions; migration scripts must be verified |
