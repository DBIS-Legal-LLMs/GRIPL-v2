# Server deployment

How auth-service and GRIPL (and later RAGulate) run on the shared server
`project-gripl` (`134.60.71.167`), in two environments: **prod** (branch `main`)
and **dev** (branch `dev`).

> **The server is reachable from the internet on ports 80 and 443.** Everything
> published there must be protected; see [Security](#security).
>
> Status: the files in this folder are tested locally with real containers
> (routing, prod/dev isolation, protections, updater, dev copy). They have **not**
> been run on the server yet; the server still needs Docker and the one-time steps
> below.

## The idea in one picture

```
                    internet / university network
                                 │ :80 (only redirects) / :443
                          ┌──────▼──────┐
                          │   Traefik   │  one reverse proxy for everything,
                          └──┬───────┬──┘  routes by host name, applies the protections
   gripl.<base>  ────────────┘       └────────────  gripl-dev.<base>
                │                                        │
   ┌────────────▼─────────┐               ┌─────────────▼────────┐
   │ network  dbis-prod   │               │ network  dbis-dev    │
   │ gripl-frontend       │               │ gripl-frontend       │
   │ gripl-backend        │               │ gripl-backend        │
   │ auth-service         │               │ auth-service         │
   └─ private per stack: ─┘               └─ private per stack: ─┘
     postgres, neo4j, rag, mongo            postgres, neo4j, rag, mongo
```

* **Environments.** Each has its own Docker network (`dbis-prod`, `dbis-dev`), so a
  prod container can never talk to a dev one.
* **Only Traefik publishes ports.** Databases and the RAG service sit on a private
  network of their own stack. To look into a database use `docker compose exec` as
  the `deploy` user.
* **Only the frontend is routed publicly.** The browser talks to the Next.js
  frontend, which proxies `/api` to the backend and `/auth` to auth-service inside
  the environment network. auth-service has no public address of its own unless
  `AUTH_EXPOSE=true`.
* **Host names.** There is no domain yet, so the names come from
  [nip.io](https://nip.io): `gripl.134.60.71.167.nip.io` resolves to the server's IP
  (as does any other name in front of `.134.60.71.167.nip.io`). One IP can only serve
  several sites by host name, which is why plain `https://134.60.71.167/` is not
  enough. With a real domain later, only `GRIPL_HOST` / `BASE_DOMAIN` change.
* **The service names are part of the contract.** The frontend image has the proxy
  targets baked in at build time (`http://gripl-backend:8080`, `http://gripl-rag:8081`,
  `http://auth-service:8000`). Because each environment has its own network, the
  *same* image works in prod and dev. Do not rename the services.

## What is in this folder

| Path | What |
|---|---|
| `traefik/` | the shared reverse proxy, its security rules (`dynamic/`), the self-signed certificate script, and an optional Let's Encrypt overlay |
| `gripl/` | the GRIPL stack: `docker-compose.yml`, `env.example`, `rag.env.example` |
| `scripts/init-stack.sh` | creates a stack directory on the server |
| `scripts/update-stack.sh` | pulls new images and applies them (run by a timer) |
| `scripts/clone-prod-to-dev.sh` | overwrites the GRIPL dev databases with a copy of prod |
| `systemd/` | timer + service templates that run the updater |

auth-service has its own `deploy/` folder in its repo (same structure, plus its own
`clone-prod-to-dev.sh`). The scripts above are shared; they live here until there is
a dedicated infrastructure repository.

## Server layout

```
/srv/gripl/
  bin/                  -> symlink to traefik/repo/deploy/scripts
  traefik/              compose.yaml, .env, certs/, repo/
  prod/auth/            compose.yaml, .env, repo/            (branch main)
  prod/gripl/           compose.yaml, .env, rag.env, repo/   (branch main)
  prod/ragulate/        later
  dev/auth/             ... same, branch dev
  dev/gripl/
  dev/ragulate/
```

Each stack directory holds the stack's **own configuration and secrets** (`.env`,
`rag.env`; never committed) plus a `repo/` checkout of the app repository on the
matching branch. `compose.yaml` is a symlink into that checkout, so a merge into
`dev` or `main` updates the compose definition as well as the images. Always run
`docker compose` from the stack directory.

## One-time setup (needs root)

```bash
# 1. Docker from Docker's own apt repository
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
echo "deb [signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 2. a service user that runs the stacks (membership in `docker` is root-equivalent,
#    so only this user gets it)
sudo useradd --create-home --shell /bin/bash deploy
sudo usermod -aG docker deploy

# 3. /srv/gripl already exists (group project-gripl, setgid, group-writable).
#    `project-gripl` is a central (sssd) group, so `deploy` cannot be added with
#    usermod; give it access with an ACL instead:
sudo setfacl -R  -m u:deploy:rwx /srv/gripl
sudo setfacl -Rd -m u:deploy:rwx /srv/gripl

# 4. optional but recommended: swap, because prod + dev share 11 GB of RAM
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Then, as `deploy` (`sudo -iu deploy`):

```bash
cd /srv/gripl
G=https://github.com/DBIS-Legal-LLMs/GRIPL-v2.git
A=https://github.com/DBIS-Legal-LLMs/auth-service.git

# bootstrap: the scripts live in the repo, so fetch them once to create the first
# stack (traefik, which also creates the dbis-prod / dbis-dev networks).
# Until this folder is on `main`, use `dev` (or the branch it is on) instead of `main`.
git clone --depth 1 --branch main "$G" /tmp/gripl-bootstrap
/tmp/gripl-bootstrap/deploy/scripts/init-stack.sh traefik "$G" main deploy/traefik/docker-compose.yml
ln -sfn traefik/repo/deploy/scripts bin
rm -rf /tmp/gripl-bootstrap

bin/init-stack.sh prod/auth  "$A" main deploy/docker-compose.yml
bin/init-stack.sh dev/auth   "$A" dev  deploy/docker-compose.yml
bin/init-stack.sh prod/gripl "$G" main deploy/gripl/docker-compose.yml
bin/init-stack.sh dev/gripl  "$G" dev  deploy/gripl/docker-compose.yml

# the self-signed certificate Traefik serves (see "HTTPS" below)
cd traefik && repo/deploy/traefik/make-selfsigned-cert.sh 134.60.71.167.nip.io 134.60.71.167 && cd ..
```

Fill in the `.env` / `rag.env` files (the init script created them from the
examples; secrets: `openssl rand -base64 24 | tr -d '/+='`). Set `ENVIRONMENT`,
`COMPOSE_PROJECT_NAME`, `IMAGE_TAG`, the host names, and:

* prod: `ALLOWED_CIDRS=0.0.0.0/0,::/0` (public) or the university ranges if prod
  should not be public either; `API_DOCS_ENABLED=false`
* dev: `ALLOWED_CIDRS=<university network ranges>` (dev holds a copy of prod's
  data and must not be public), `API_DOCS_ENABLED=true` if wanted

Start in this order, each with `cd <stack> && docker compose up -d --wait`:
`traefik`, `prod/auth`, `dev/auth`, `prod/gripl`, `dev/gripl`.

First-start chores per environment:

* **Register GRIPL in auth-service** and create the first superuser (see the
  auth-service README): `docker compose exec auth-service python scripts/register_application.py gripl ...`
* **Ingest the RAG knowledge base** once per environment (costs OpenRouter credit),
  or copy it from prod with `clone-prod-to-dev.sh --with-rag`:
  `docker compose exec gripl-rag python scripts/ingest.py`

## How updates reach the server (pull-based)

```
merge into dev/main ──► CI builds the images ──► ghcr.io/dbis-legal-llms/<name>:dev|prod
                                                          │
         timer on the server, every ~3 min, per stack ◄───┘  update-stack.sh:
           git fetch (compose definition) → docker compose pull → docker compose up -d --wait
```

* Nothing connects *into* the server for deployments.
* A registry outage or a failing pull leaves the running containers untouched.
* `up -d` only recreates containers whose image or configuration changed.
* A container that never becomes healthy makes the run fail (visible with
  `journalctl -u 'dbis-update@*'`); the previous image is not rolled back
  automatically. To roll back, set `IMAGE_TAG=sha-<commit>` in the stack's `.env`
  and run `docker compose up -d`; remove the line again to follow the branch.

Enable the timers (root, once per stack):

```bash
sudo cp /srv/gripl/traefik/repo/deploy/systemd/dbis-update@.* /etc/systemd/system/
sudo systemctl daemon-reload
for s in prod/auth dev/auth prod/gripl dev/gripl; do
  sudo systemctl enable --now "dbis-update@$(systemd-escape --path /srv/gripl/$s).timer"
done
```

Why not Watchtower? The upstream project (`containrrr/watchtower`) was archived in
December 2025 and its old Docker API version no longer works with Docker 29, which
Ubuntu 26.04 ships. The updater above needs no extra container and no access to the
Docker socket beyond the `deploy` user, and it honours compose's health checks.

### What CI has to produce

Per repository and branch, images named exactly like this (public GHCR packages, so
the server needs no login, which also means **never bake secrets into an image**):

| Image | `main` → tag | `dev` → tag |
|---|---|---|
| `ghcr.io/dbis-legal-llms/gripl-frontend` | `prod` | `dev` |
| `ghcr.io/dbis-legal-llms/gripl-backend` | `prod` | `dev` |
| `ghcr.io/dbis-legal-llms/gripl-rag` | `prod` | `dev` |
| `ghcr.io/dbis-legal-llms/auth-service` | `prod` | `dev` |

(plus an immutable `sha-<commit>` tag each, for rollbacks). The frontend must be built
with these build args, because Next.js freezes its proxy targets at build time:

```
NEXT_PUBLIC_API_BASE_URL=http://gripl-backend:8080
RAG_INTERNAL_URL=http://gripl-rag:8081
AUTH_SERVICE_INTERNAL_URL=http://auth-service:8000
```

This is implemented in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) (GRIPL-v2) and in
auth-service's own `.github/workflows/ci.yml`:

* every push and pull request runs the tests (GRIPL: backend tests against a Postgres service
  container, frontend type check; auth-service: pytest) and builds every image
* only a push to `main` / `dev` publishes (`:prod` / `:dev` plus `:sha-<commit>`); pull requests
  build but publish nothing
* layers are cached in the registry (`:buildcache` tag), because the RAG image is several GB
* third-party actions are pinned to commits; the workflows only get the permissions they need

After the **first** publish, each of the four packages has to be made **public** once, otherwise
the server cannot pull it without a login. New packages are created **private**; a package
inherits the access permissions of its repository, but not its visibility. In the organisation's
*Packages* page open the package, then *Package settings → Danger Zone → Change visibility →
Public*. GitHub does not allow switching a public package back to private (deleting it is the
way back), so only do this for images that contain nothing secret. These do not: they are built
from the public repositories by CI, and no `.env` or key ever enters an image. A manual re-run
of the pipeline is possible via *Actions → CI → Run workflow*.

## HTTPS

Port 80 only redirects to HTTPS; every site lives on 443. Until the project has a
domain, Traefik serves a **self-signed certificate**
(`traefik/make-selfsigned-cert.sh`), valid for `*.134.60.71.167.nip.io` and the IP.
The connection is encrypted, but browsers warn on every new visit. Two ways around
the warning for the people who work with it regularly: install `traefik/certs/ca.crt`
as a trusted CA on their machine (it is a small private CA created by the script;
keep `ca.key` on the server), or accept the warning.

Rotating the certificate (before it expires after 13 months): run the script again.
It keeps the CA, so installed copies stay valid.

### A real certificate later

Once there is a domain, the overlay `traefik/docker-compose.https.yml` obtains a
Let's Encrypt wildcard certificate with the DNS-01 challenge (Traefik creates a
`_acme-challenge` TXT record; this needs a DNS server that accepts RFC2136 updates with
a TSIG key restricted to `_acme-challenge`, or another lego DNS provider). Steps:
fill `BASE_DOMAIN`, `ACME_EMAIL` and `RFC2136_*` in `traefik/.env`, start with
`docker compose -f compose.yaml -f repo/deploy/traefik/docker-compose.https.yml up -d`
(it begins against the Let's Encrypt staging CA, switch `ACME_CA_SERVER` to production
once that works), then change `GRIPL_HOST` in the stacks' `.env` files. Add HSTS to
`traefik/dynamic/middlewares.yml` at that point, not before. Certificates for
`*.nip.io` names are not an option: Let's Encrypt's limits are shared by everyone using
nip.io.

## Security

What is in place:

* **Only 80/443 are open**; Traefik is the only container with published ports (and
  binds IPv4 only, so the client address stays visible for rate limiting and
  allow-lists). Databases, RAG service and backend are not reachable from outside.
* **Traefik reads the Docker API through a read-only socket proxy**, not the raw socket.
* **Per-environment allow-list** (`ALLOWED_CIDRS`): dev should only be reachable from
  the university network because it holds a copy of prod's users and data.
* **Rate limits per client IP** on `/auth/login` (20/min) and `/auth/register`
  (5/min), see `traefik/dynamic/middlewares.yml`.
* **`/rag/*` is blocked** (403), except `/rag/api/pdf/` (the sandbox's PDF viewer). The RAG
  service has no authentication and runs on the project's own LLM key; the PDF routes only serve
  the corpus's public source documents and match names against existing files (no path input).
* **API docs are off** (`API_DOCS_ENABLED=false`): the backend serves Swagger without a
  login, and auth-service's `/docs`/`/redoc` are blocked.
* Security headers (no sniffing, no framing, referrer policy); logs are rotated
  (10 MB × 5 per container); database images are pinned; MongoDB requires a password.
* Dev has its own token signing key and secrets key, so a dev token is useless on prod.

Known gaps (application level, not solved by this folder):

* **auth-service has no lockout or brute-force protection**; the rate limit only slows
  guessing down. Registration is open to everyone on the internet.
* A registered user with the default role can run analyses with RAG, which uses the
  project's LLM key. Decide whether registration should stay open on prod, and set a
  spending limit on the OpenRouter key either way.
* The login cookie (`gripl_token`) is readable by JavaScript and has no `Secure` flag.
* Rate limits count per client IP, so many users behind one university NAT address
  share a budget.

## Dev as a copy of prod

```bash
# auth-service: users and roles (not the signing key, not the stored OpenRouter keys)
/srv/gripl/prod/auth/repo/deploy/scripts/clone-prod-to-dev.sh
# GRIPL: Postgres; add --with-rag to also copy the knowledge graph
/srv/gripl/bin/clone-prod-to-dev.sh [--with-rag]
```

* Prod is only read. `--with-rag` stops prod's RAG service and Neo4j for roughly a
  minute to get a consistent copy, and needs the same `NEO4J_PASSWORD` in both `.env`
  files.
* Dev deliberately keeps **its own** token signing key and `SECRETS_ENCRYPTION_KEY`.
  With prod's key, a token minted in dev (where developers can edit the database)
  would be accepted by prod.

## Notes and gotchas

* **Memory.** Prod and dev together should idle at roughly 4–6 GB. Each container has
  an upper limit (`MEM_*` in `.env`) so one environment cannot starve the other.
* **Database versions are pinned** (`postgres:15.18`, `neo4j:5.26.30-community`,
  `mongo:7.0.25`). A floating tag such as `postgres:15` would make the updater upgrade
  and restart a database whenever the registry publishes a new patch release. Bump the
  pin deliberately, preferably in dev first.
* **Looking into a database:** nothing is published, so use
  `docker compose exec gripl-postgres psql -U postgres gripl_db` as `deploy`.
* **VPN and Docker subnets (your laptop, not the server).** Some university VPN clients
  claim `172.17.0.0/16` and `172.20.0.0/16`, which are Docker's default bridge ranges;
  containers on those subnets become unreachable from the host while the VPN is up. If
  that bites, give Docker other ranges (`default-address-pools` in
  `/etc/docker/daemon.json`).
* The older root-level `docker-compose.prod.yml`, `docker-compose.traefik.yml` and
  `docker-compose.watchtower.yml` belong to the previous deployment approach (images
  from another Docker Hub account, Traefik talking to a Windows Docker daemon) and are
  superseded by this folder.
