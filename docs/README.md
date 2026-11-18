# GRIPL documentation

Short how-tos for running, changing and releasing GRIPL. Each page is a list of tasks: the command or
steps, and one line on where it is implemented if you have to change it.

## How the system fits together

| Part | What it does | Code |
|---|---|---|
| `gripl-frontend` | Next.js web app, login page, sidebar, role-based pages | `gripl/gripl-frontend` |
| `gripl-backend` | Kotlin/Spring API, Postgres, Flyway migrations, role checks | `gripl/gripl-backend` |
| `gripl-rag` | Knowledge graph (Neo4j + LightRAG) used for retrieval | `gripl/gripl-rag` |
| `auth-service` | Accounts, roles, JWT login (own repository, MongoDB) | `DBIS-Legal-LLMs/auth-service` |
| Traefik | One reverse proxy for all stacks, TLS, rate limits | `deploy/traefik` |

The server runs two independent environments, each with its own databases and its own accounts:

| Environment | Branch | Images | Address |
|---|---|---|---|
| **dev** | `dev` | `:dev`, `:X.Y.Z` | `gripl-dev.<base>` |
| **prod** | `main` | `:prod` (a promoted `:X.Y.Z`) | `gripl.<base>` |

Nothing is deployed by hand: CI builds the images, and a timer on the server pulls them
([develop-and-release.md](develop-and-release.md), [server-operations.md](server-operations.md)).

## Pages

| Page | Read it when you want to ... |
|---|---|
| [develop-and-release.md](develop-and-release.md) | write commits and PRs, understand versions and releases, add a database migration |
| [deploy-to-prod.md](deploy-to-prod.md) | put a tested version on prod, check it, or roll prod back |
| [users-and-roles.md](users-and-roles.md) | create a superuser, change a user's role, register an application, reset an OpenRouter key, change the token lifetime |
| [server-operations.md](server-operations.md) | look at logs, switch the updater off, change its interval, make or restore a backup |
| [move-dev-to-prod.md](move-dev-to-prod.md) | switch the dev users and their data over to prod (one time) |
| [troubleshooting.md](troubleshooting.md) | find the cause of a symptom |

Deployment internals (stack layout, Traefik, security, why things are built the way they are) are in
[`deploy/README.md`](../deploy/README.md).

## Rule for changes

Every change to code, CI or deployment also updates the pages above where they are affected. Put the
documentation in a separate commit in the same pull request into `dev`.
