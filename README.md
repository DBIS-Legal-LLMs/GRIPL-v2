# GRIPL - GDPR Risk Identification in Processes using Large Language Models

Identifying GDPR-critical tasks in business processes using large language
models: model or import a BPMN process, have an LLM classify GDPR-relevant
activities, label your own evaluation datasets, and run evaluations against
them. Backed by a Kotlin/Spring backend, a Next.js frontend, and RAGulate's
sibling RAG service (`gripl-rag`) for retrieval-augmented, GDPR-grounded
reasoning.

**[Installation Guide](#installation-guide) · [Quick Start (Already Installed)](#quick-start-already-installed) · [Features](#features) · [Project Structure](#project-structure) · [Authentication & Roles](#authentication--roles) · [Running the Tests](#running-the-tests) · [Endpoints](#endpoints) · [Useful Docker Commands](#useful-docker-commands) · [TODOs](#todos) · [Tech Stack](#tech-stack)**

---

## Installation Guide

### Prerequisites

- [Docker](https://docs.docker.com/engine/install/) & [Docker Compose](https://docs.docker.com/compose/install/)
- An OpenAI-compatible LLM API key (OpenRouter by default)
- A running instance of [`auth-service`](https://github.com/DBIS-Legal-LLMs/auth-service) — login/register won't work without it. See that repo's README; by default GRIPL expects it at `http://localhost:8100` (or `http://host.docker.internal:8100` from inside Docker).

> **Windows**: Docker Desktop → Settings → General → enable "Expose daemon on tcp://localhost:2375 without TLS"; Settings → Docker Engine → add `"min-api-version": "1.24"`.
> **Linux** (production Traefik setup only): `docker-compose.traefik.yml` needs `--providers.docker.endpoint=unix://var/run/docker.sock` and the socket mounted read-only — see the file.

### 1. Local development

```bash
git clone <repo-url>
cd GRIPL-v2
cp .env.local.example .env.local
```

Fill in `.env.local` — at minimum an LLM key (`OPENAI_API_KEY`/`OPEN_ROUTER_API_KEY` and the `LLM_*`/`EMBEDDING_*` block for `gripl-rag`); the `AUTH_SERVICE_JWKS_URI`/`AUTH_SERVICE_INTERNAL_URL` defaults already point at `auth-service` running in Docker on the same host (see [Authentication & Roles](#authentication--roles)).

```bash
docker compose -f docker-compose.local.yml up --build
```

Starts:

| Service | URL |
|---|---|
| Frontend | http://localhost:3001 (3000 is reserved for RAGulate's frontend on this machine) |
| Backend (Swagger UI) | http://localhost:8001/swagger-ui (8000 is reserved for RAGulate's backend) |
| RAG service | http://localhost:8081 |
| Neo4j browser | http://localhost:7474 |
| Postgres | `localhost:5432`, database `gripl_db` |

Login/register go through `auth-service` — it must already be running (its own `docker compose up`).

> **Reproducing the paper's dataset**: `dataset/` ships the labeled BPMN corpus as CSV exports. `python scripts/import_all_data.py` (needs `psycopg2`, reads `.env.local` for the Postgres connection) imports `dataset.csv` then `evaluation_data.csv`; `scripts/check_datasets.py` sanity-checks what's already imported against the CSVs.

### 2. Production (server already runs Traefik + Watchtower)

```bash
cp .env.prod.example .env.prod
docker network create web   # only if it doesn't exist yet
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Attaches every service to the existing `web` network, exposes the frontend on `${GRIPL_HOST}`/`www.${GRIPL_HOST}`, the backend on `${GRIPL_HOST}/api`, and enables Watchtower auto-updates for all of them (containers labeled `com.centurylinklabs.watchtower.enable=true` in `docker-compose.prod.yml`).

### 3. Production, without Traefik/Watchtower already running

```bash
docker compose --env-file .env.prod \
  -f docker-compose.yml -f docker-compose.prod.yml \
  -f docker-compose.traefik.yml -f docker-compose.watchtower.yml up -d
```

### Running components locally without Docker

See [`gripl/gripl-backend/README.md`](gripl/gripl-backend/README.md) (Maven, CLI commands, needs a Postgres instance — Flyway applies pending migrations automatically on startup) and [`gripl/gripl-frontend/README.md`](gripl/gripl-frontend/README.md). `gripl-rag` needs its own dependencies (`gripl/gripl-rag/requirements.txt`) plus a running Neo4j; on Linux it additionally needs system packages `libgl1`/`libglib2.0-0` for PyMuPDF/LightRAG (the Docker image installs these automatically).

## Quick Start (Already Installed)

Everything above already done once (`.env.local` filled in, images built) — just bringing the local stack back up:

```bash
# auth-service must already be running — see its own README
docker compose -f docker-compose.local.yml up -d
```

Health checks: backend Swagger at `http://localhost:8001/swagger-ui`, frontend at `http://localhost:3001`, RAG service status via the frontend or `GET /gdpr/rag/status` on the backend.

## Features

The GRIPL tool has three main areas — which ones you can reach depends on your role, see [Authentication & Roles](#authentication--roles).

**1. The Sandbox** — model or import a process, let the LLM analyze and classify GDPR-relevant activities, with reasoning per classification. Open to every role.

<img width="2232" height="1265" alt="sandbox-analyzed-model-annotated" src="https://github.com/user-attachments/assets/1e7ac03b-2f76-4e10-8f21-ed4fc1d4af16" />
<img width="1138" height="607" alt="sandbox-ai-reasoning" src="https://github.com/user-attachments/assets/7ed9f60b-18d8-4a06-b8b5-09e4e15d682b" />

**2. The Labeling Editor** — create and label your own BPMN 2.0 evaluation datasets, with an optional reasoning per label. `admin`/`researcher` only.

<img width="2227" height="1258" alt="labeling-datasets" src="https://github.com/user-attachments/assets/7651e26e-b3a5-48bb-beca-67daee7d6c7e" />
<img width="2232" height="1260" alt="labeling-editor-annotated" src="https://github.com/user-attachments/assets/e7f03bc1-2a74-44c6-adf7-e05f906594a2" />

**3. The Evaluation Platform** — configure evaluations via the GUI or a YAML file; results broken down by model, by run, and by test case. `admin`/`researcher` only.

<img width="1137" height="1183" alt="evaluation-config_new" src="https://github.com/user-attachments/assets/7ccd4c36-a674-4a69-88c5-6ae181fae186" />
<img width="1327" height="1211" alt="evaluation-result-by-model" src="https://github.com/user-attachments/assets/52a3e330-22d4-42ad-9e70-14e3a4baef7a" />
<img width="1318" height="1125" alt="evaluation-result-by-run_new" src="https://github.com/user-attachments/assets/c82634f2-71c4-4bf8-9b94-5a059ecada9c" />
<img width="1503" height="1268" alt="evaluation-result-by-testcase_new" src="https://github.com/user-attachments/assets/6272dfc7-533e-47dc-a57d-c143e1caa864" />

Datasets are **private per user** — you only see and manage your own (see [Authentication & Roles](#authentication--roles)).

## Project Structure

```
GRIPL-v2/
├── gripl/
│   ├── gripl-frontend/       # Next.js application (Sandbox, Labeling, Evaluation UIs)
│   ├── gripl-backend/        # Spring Boot (Kotlin) API + CLI — analysis, datasets, evaluation
│   └── gripl-rag/            # RAG service (FastAPI + LightRAG + Neo4j), GDPR knowledge graph
├── dataset/                  # Labeled BPMN corpus (CSV exports) for reproducing the evaluation
├── experiments/              # Experiment configurations and results
├── scripts/                  # import_all_data.py, check_datasets.py — reproduce the dataset in Postgres
├── system demo/              # Recorded demo video
├── docker-compose.yml               # base services
├── docker-compose.local.yml         # local dev/testing
├── docker-compose.prod.yml          # production, Traefik + Watchtower labels
├── docker-compose.traefik.yml       # optional Traefik container
├── docker-compose.watchtower.yml    # optional Watchtower container
├── .env.local.example
└── .env.prod.example
```

## Authentication & Roles

User accounts, login, registration, and JWT issuance are **not** handled by
this repository. They live in
[`auth-service`](https://github.com/DBIS-Legal-LLMs/auth-service), a
standalone identity service shared with RAGulate and future DBIS tools.

- `auth-service` signs tokens with RS256 and publishes its public key at `/.well-known/jwks.json`. `gripl-backend` only *verifies* incoming tokens against that JWKS (`JwtAuthenticationWebFilter`) — no shared secret, nothing to keep in sync between repos.
- `gripl-frontend` never talks to `auth-service` cross-origin: its `/login` page calls `/auth/*`, which Next.js rewrites server-side to `auth-service` (`next.config.ts`, `AUTH_SERVICE_INTERNAL_URL`).
- `auth-service` runs as its **own** `docker-compose` stack, listening on `:8100`, and must be running and reachable before login/register work.

| Variable | Used by | Example (Docker on the same host) |
|---|---|---|
| `AUTH_SERVICE_JWKS_URI` | `gripl-backend` | `http://host.docker.internal:8100/.well-known/jwks.json` |
| `AUTH_SERVICE_INTERNAL_URL` | `gripl-frontend` | `http://host.docker.internal:8100` |

The same account works across GRIPL and RAGulate — both verify the same `auth-service` tokens.

### Roles

`auth-service` resolves each user's GRIPL role into the token's `app_roles.gripl` claim: `admin`, `researcher`, `dpo`, or `end-user`. Enforced at two layers:

- **Backend** (real enforcement) — `requireGriplRole`/`requirePrivilegedGriplRole` (`security/AuthenticatedUser.kt`) gate individual endpoints, `403` if the caller's role isn't allowed.
- **Frontend** (UX only, not a security boundary) — the sidebar hides nav entries the caller can't use, and `middleware.ts` redirects direct navigation to them.

| Surface | Routes / endpoints | Roles |
|---|---|---|
| Sandbox | `/`, `/gdpr/analysis/**`, `/bpmn/**`, `/gdpr/rag/status` | all four |
| Labeling | `/labeling`, `/dataset/**` | `admin`, `researcher` |
| Evaluation | `/evaluation`, `/gdpr/evaluation/**` | `admin`, `researcher` |

`dpo`'s RAG-knowledge-base access (#37/#38) isn't built yet — until then `dpo` gets the sandbox only, same as `end-user`. Datasets and test cases are additionally scoped **per owner** on top of the role check — see `gripl/gripl-backend/README.md`.

## Running the Tests

Backend (Kotlin/JUnit/Mockito) — no Maven install needed, runs in a throwaway container:

```bash
cd gripl/gripl-backend
docker run --rm -v "$PWD":/app -v gripl-m2:/root/.m2 -w /app \
  maven:3.8.6-eclipse-temurin-17 \
  mvn -Dtest='DatasetControllerOwnershipTest,EvaluationDataControllerOwnershipTest,EvaluationControllerRoleTest,JwtAuthenticationWebFilterTest,MultiEvaluationRunnerSeedTest,GlobalExceptionHandlerTest' test
```

Covers JWKS verification, dataset/test-case ownership, and role-gating — 35
tests, no external services needed. Clean the root-owned `target/` afterwards:
`docker run --rm -v "$PWD":/app alpine rm -rf /app/target`.

`GriplBackendApplicationTests` (plain `mvn test` includes it) boots the full
Spring context and needs a live Postgres + Neo4j + a real LLM key to pass —
not included above on purpose.

Frontend has no automated test suite yet (`npm run lint` is the only check) — see [TODOs](#todos).

## Endpoints

All paths below except `/actuator/health`, `/swagger-ui`, `/v3/api-docs` and `/thesis/pdf` require a valid `auth-service` token; the **Role** column is the additional role check on top of that.

| Surface | Method | Path | Role | Description |
|---|---|---|---|---|
| Sandbox | `GET` | `/gdpr/analysis/endpoints` | any | List available analysis endpoints |
| Sandbox | `POST` | `/gdpr/analysis/prompt-engineering` | any | Analyze a BPMN file (prompt-engineering approach) |
| Sandbox | `POST` | `/gdpr/analysis/baseline` | any | Analyze a BPMN file (baseline approach) |
| Sandbox | `POST` | `/gdpr/analysis/multiclass` | any | Analyze a BPMN file (multiclass classification) |
| Sandbox | `POST` | `/bpmn/extract` | any | Extract BPMN elements from an uploaded file |
| Sandbox | `GET` | `/gdpr/rag/status` | any | Whether the RAG knowledge graph currently holds ingested data |
| Labeling | `POST` `GET` `DELETE` | `/dataset`, `/dataset/{id}` | admin, researcher | Create/list/delete datasets (owner-scoped) |
| Labeling | `GET` `POST` `DELETE` | `/dataset/testcase`, `/dataset/testcase/{id}` | admin, researcher | List/create/get/update/delete test cases (owner-scoped via parent dataset) |
| Labeling | `GET` | `/dataset/testcase/{id}/preview` | admin, researcher | SVG preview of a test case's process model |
| Evaluation | `POST` | `/gdpr/evaluation/markdown` | admin, researcher | Run an evaluation, markdown report |
| Evaluation | `POST` | `/gdpr/evaluation/stream` | admin, researcher | Run an evaluation, NDJSON stream |
| — | `GET` | `/thesis/pdf` | — (public) | Serves the thesis PDF |
| — | `GET` | `/actuator/health`, `/swagger-ui`, `/v3/api-docs` | — (public) | Liveness + API docs |

Full request/response shapes: Swagger UI at `/swagger-ui` once the backend is running.

## Useful Docker Commands

```bash
# Show running containers
docker ps

# Follow logs for each piece
docker logs -f gripl-backend
docker logs -f gripl-rag
docker logs -f gripl-neo4j
docker logs -f auth-service      # separate compose project

# Open a shell inside a container
docker exec -it gripl-backend bash

# Open a Postgres shell
docker exec -it gripl-postgres psql -U postgres -d gripl_db
```

## TODOs

- [GRIPL#37](https://github.com/DBIS-Legal-LLMs/GRIPL-v2/issues/37) — DPO: read-only overview of the RAG knowledge base's ingested documents
- [GRIPL#38](https://github.com/DBIS-Legal-LLMs/GRIPL-v2/issues/38) — DPO: upload/remove RAG knowledge base documents
- [GRIPL#14](https://github.com/DBIS-Legal-LLMs/GRIPL-v2/issues/14) — Embed RAGulate's chat widget (blocked on RAGulate_v2#123, not built yet)
- [GRIPL#8](https://github.com/DBIS-Legal-LLMs/GRIPL-v2/issues/8) — Host the application on the DBIS AI PC (deliberately last)
- Frontend automated test suite (currently lint-only)

## Tech Stack

- [Next.js](https://nextjs.org/) 15 / [React](https://react.dev/) 18, [TypeScript](https://www.typescriptlang.org/), [Tailwind CSS](https://tailwindcss.com/) — frontend
- [Spring Boot](https://spring.io/projects/spring-boot) 4 (Kotlin, WebFlux) — backend API + CLI
- [FastAPI](https://fastapi.tiangolo.com/) + [LightRAG](https://github.com/HKUDS-LightRAG/LightRAG) + [Neo4j](https://neo4j.com/) — `gripl-rag`, the GDPR knowledge graph
- [PostgreSQL](https://www.postgresql.org/) — datasets, test cases, evaluation results
- [Docker](https://www.docker.com/) / [Docker Compose](https://docs.docker.com/compose/), [Traefik](https://traefik.io/) + [Watchtower](https://containrrr.dev/watchtower/) — running and deploying it
