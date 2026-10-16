# Server deployment

How GRIPL and auth-service run on the shared DBIS server (`project-gripl`,
reachable only from the university network / VPN). The same layout takes
RAGulate later.

> Status: the files in this folder are tested locally with real containers
> (routing, prod/dev isolation, updater, dev copy). They have **not** been run on
> the server yet; the server still needs Docker and a few one-time steps (below).

## The idea in one picture

```
                         university network / VPN
                                   │  :80 / :443
                            ┌──────▼──────┐
                            │   Traefik   │  one reverse proxy for everything,
                            └──┬───────┬──┘  routes by host name
        gripl.<domain>  ───────┘       └───────  gripl-dev.<domain>
        auth.<domain>                            auth-dev.<domain>
              │                                        │
   ┌──────────▼───────────┐               ┌───────────▼──────────┐
   │ network  dbis-prod   │               │ network  dbis-dev    │
   │ gripl-frontend       │               │ gripl-frontend       │
   │ gripl-backend        │               │ gripl-backend        │
   │ auth-service         │               │ auth-service         │
   └─ private per stack: ─┘               └─ private per stack: ─┘
     postgres, neo4j, rag, mongo            postgres, neo4j, rag, mongo
```

* **Environments.** `prod` runs what is on `main`, `dev` runs what is on `dev`.
  Each environment has its own Docker network (`dbis-prod`, `dbis-dev`), so a prod
  container can never talk to a dev one.
* **Only Traefik publishes ports** (80/443). Databases and the RAG service sit on
  a private network of their own stack and are not reachable from outside. To look
  into a database, use an SSH tunnel.
* **Only the frontend is routed publicly.** The browser talks to the Next.js
  frontend, which proxies `/api`, `/rag` and `/auth` to the backend, RAG service
  and auth-service inside the environment network.
* **The service names are part of the contract.** The frontend image has the proxy
  targets baked in at build time (`http://gripl-backend:8080`, `http://gripl-rag:8081`,
  `http://auth-service:8000`). Because each environment has its own network, the
  *same* image works in prod and dev. Do not rename the services.

## What is in this folder

| Path | What |
|---|---|
| `traefik/` | the shared reverse proxy (+ read-only Docker socket proxy), HTTPS overlay |
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
/srv/dbis-universe/
  bin/                  -> symlink to traefik/repo/deploy/scripts
  traefik/              compose.yaml, .env, repo/
  gripl/prod/           compose.yaml, .env, rag.env, repo/   (branch main)
  gripl/dev/            compose.yaml, .env, rag.env, repo/   (branch dev)
  auth/prod/            compose.yaml, .env, repo/            (branch main)
  auth/dev/             compose.yaml, .env, repo/            (branch dev)
```

Each stack directory holds the stack's **own configuration and secrets** (`.env`,
`rag.env` — never committed) plus a `repo/` checkout of the app repository on the
matching branch. `compose.yaml` is a symlink into that checkout, so a merge into
`dev` or `main` updates the compose definition as well as the images.

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

# 3. the base directory. /srv/gripl already exists (group project-gripl, setgid,
#    group-writable); rename it to /srv/dbis-universe if agreed. `project-gripl` is a
#    central (sssd) group, so `deploy` cannot be added with usermod; use an ACL:
sudo setfacl -R  -m u:deploy:rwx /srv/dbis-universe
sudo setfacl -Rd -m u:deploy:rwx /srv/dbis-universe

# 4. optional but recommended: swap, because prod + dev share 11 GB of RAM
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Then, as `deploy` (`sudo -iu deploy`):

```bash
cd /srv/dbis-universe
G=https://github.com/DBIS-Legal-LLMs/GRIPL-v2.git
A=https://github.com/DBIS-Legal-LLMs/auth-service.git

# bootstrap: the scripts live in the repo, so fetch them once to create the first
# stack (traefik, which also creates the dbis-prod / dbis-dev networks).
# Until this folder is on `main`, use `dev` (or the branch it is on) instead of `main`.
git clone --depth 1 --branch main "$G" /tmp/gripl-bootstrap
/tmp/gripl-bootstrap/deploy/scripts/init-stack.sh traefik "$G" main deploy/traefik/docker-compose.yml
ln -sfn traefik/repo/deploy/scripts bin
rm -rf /tmp/gripl-bootstrap

bin/init-stack.sh gripl/prod $G main deploy/gripl/docker-compose.yml
bin/init-stack.sh gripl/dev  $G dev  deploy/gripl/docker-compose.yml
bin/init-stack.sh auth/prod  $A main deploy/docker-compose.yml
bin/init-stack.sh auth/dev   $A dev  deploy/docker-compose.yml
```

Fill in the `.env` / `rag.env` files (the init script created them from the
examples; secrets are generated with `openssl rand -base64 24 | tr -d '/+='`), then
start in this order: `traefik`, `auth/prod`, `auth/dev`, `gripl/prod`, `gripl/dev`
with `cd <stack> && docker compose up -d --wait`.

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

* Nothing connects *into* the server, so it works behind the university firewall.
* A registry outage or a failing pull leaves the running containers untouched.
* `up -d` only recreates containers whose image or configuration changed.
* A container that never becomes healthy makes the run fail (visible with
  `journalctl -u 'dbis-update@*'`); the previous image is not rolled back
  automatically. To roll back, set `IMAGE_TAG=sha-<commit>` in the stack's `.env`
  and run `docker compose up -d`; remove the line again to follow the branch.

Enable the timers (root, once per stack):

```bash
sudo cp /srv/dbis-universe/traefik/repo/deploy/systemd/dbis-update@.* /etc/systemd/system/
sudo systemctl daemon-reload
for s in gripl/prod gripl/dev auth/prod auth/dev; do
  sudo systemctl enable --now "dbis-update@$(systemd-escape --path /srv/dbis-universe/$s).timer"
done
```

Why not Watchtower? The upstream project (`containrrr/watchtower`) was archived in
December 2025 and its old Docker API version no longer works with Docker 29, which
Ubuntu 26.04 ships. The updater above needs no extra container and no access to the
Docker socket beyond the `deploy` user, and it honours compose's health checks.

### What CI has to produce

Per repository and branch, images named exactly like this (public GHCR packages, so
the server needs no login — which also means **never bake secrets into an image**):

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

## HTTPS

HTTP works out of the box. Let's Encrypt's usual check (HTTP-01) needs the server to be
reachable from the internet, which it is not, so HTTPS uses the **DNS-01** challenge:
Traefik proves control of the domain by creating a `_acme-challenge` TXT record, which
only needs the *DNS server* to be reachable. One wildcard certificate (`*.<base domain>`)
then covers every stack.

1. Fill `BASE_DOMAIN`, `ACME_EMAIL` and the `RFC2136_*` values in `traefik/.env`
   (needs a DNS server that accepts RFC2136 updates with a TSIG key restricted to
   `_acme-challenge`; other lego providers work by changing `ACME_DNS_PROVIDER`).
2. `docker compose -f docker-compose.yml -f docker-compose.https.yml up -d` in `traefik/`.
   It uses the Let's Encrypt **staging** CA first; check that a certificate is issued,
   then set `ACME_CA_SERVER=https://acme-v02.api.letsencrypt.org/directory`.
3. Set `PUBLIC_SCHEME=https` in the GRIPL `.env` files and re-run `docker compose up -d`.

Port 80 then redirects to HTTPS for every host. Until real DNS names exist, the
`<name>.134.60.71.167.nip.io` fallback names work for HTTP (a certificate for them is
not possible).

## Dev as a copy of prod

```bash
# auth-service: users and roles (not the signing key, not the stored OpenRouter keys)
/srv/dbis-universe/auth/prod/repo/deploy/scripts/clone-prod-to-dev.sh
# GRIPL: Postgres; add --with-rag to also copy the knowledge graph
/srv/dbis-universe/bin/clone-prod-to-dev.sh [--with-rag]
```

* Prod is only read. `--with-rag` stops prod's RAG service and Neo4j for roughly a
  minute to get a consistent copy, and needs the same `NEO4J_PASSWORD` in both `.env`
  files.
* Dev deliberately keeps **its own** token signing key and `SECRETS_ENCRYPTION_KEY`.
  With prod's key, a token minted in dev (where developers can edit the database)
  would be accepted by prod.

## Notes and gotchas

* **Memory.** Prod and dev together should idle at roughly 4–6 GB. Each container has
  an upper limit (`MEM_*` in `.env`) so one environment cannot starve the other or the
  other users of the server.
* **Database versions are pinned** (`postgres:15.18`, `neo4j:5.26.30-community`, `mongo:7.0.25`). A floating tag such as `postgres:15` would make the updater upgrade and restart a database whenever the registry publishes a new patch release. Bump the pin deliberately, preferably in dev first.
* **MongoDB** requires a password (`MONGO_ROOT_PASSWORD`) even though it is only
  reachable inside its own stack.
* **Looking into a database:** `ssh -L 5432:localhost:5432 ...` does not work because
  nothing is published; use `docker compose exec gripl-postgres psql -U postgres gripl_db`
  as `deploy`.
* **VPN and Docker subnets (your laptop, not the server).** Some university VPN clients
  claim `172.17.0.0/16` and `172.20.0.0/16`, which are Docker's default bridge ranges;
  containers on those subnets become unreachable from the host while the VPN is up. If
  that bites, give Docker other ranges (`default-address-pools` in
  `/etc/docker/daemon.json`).
* The older root-level `docker-compose.prod.yml`, `docker-compose.traefik.yml` and
  `docker-compose.watchtower.yml` belong to the previous deployment approach (images
  from another Docker Hub account, Traefik talking to a Windows Docker daemon) and are
  superseded by this folder.
