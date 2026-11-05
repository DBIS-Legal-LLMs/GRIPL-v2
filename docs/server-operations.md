# Server operations

Everything here runs on the server over SSH (university network or VPN). `<env>` is `dev` or `prod`.

## Layout

```
/srv/gripl/
  bin/            -> traefik/repo/deploy/scripts   (the updater and the clone scripts)
  backups/<env>/<app>/<time>-before-update/        (automatic backups, see below)
  traefik/        shared reverse proxy
  <env>/auth/     auth-service + MongoDB        compose.yaml, .env, repo/
  <env>/gripl/    frontend, backend, RAG, Neo4j, Postgres   compose.yaml, .env, rag.env, repo/
```

Each stack folder holds its own configuration and secrets (`.env`, `rag.env`, never committed). Always run `docker compose` from
the stack folder. After editing a `.env`, apply it with `docker compose up -d`.

Services: `gripl-frontend`, `gripl-backend`, `gripl-rag`, `gripl-neo4j`, `gripl-postgres` (GRIPL stack); `auth-service`, `auth-mongo` (auth stack).

## Look at what is running

```bash
cd /srv/gripl/<env>/gripl
docker compose ps                         # state and health of every container
docker compose logs --tail 80 gripl-backend
docker compose logs -f gripl-rag          # follow, Ctrl+C to leave
docker stats --no-stream; free -h; df -h /srv/gripl
```

Look into a database (nothing is published to the network, so go through the container):

```bash
cd /srv/gripl/<env>/gripl && docker compose exec gripl-postgres psql -U postgres gripl_db
```

For MongoDB see the command in [users-and-roles.md](users-and-roles.md).

Stop a stack without losing data: `docker compose down`. **Never add `-v`**, it deletes the volumes (the databases).

## The updater

A systemd timer per stack runs `/srv/gripl/bin/update-stack.sh` about every 3 minutes. It fetches the compose definition, pulls the
newest images one at a time, and, if something would change, takes a backup first, then recreates only what changed and waits until it is
healthy. A failing pull or backup leaves the running containers untouched.

The timers are named after the stack folder:

```bash
sudo systemctl enable --now  "dbis-update@$(systemd-escape --path /srv/gripl/<env>/gripl).timer"   # on
sudo systemctl disable --now "dbis-update@$(systemd-escape --path /srv/gripl/<env>/gripl).timer"   # off
systemctl list-timers 'dbis-update@*'                                                              # state
journalctl -u 'dbis-update@*' -n 30                                                                # log
```

The stacks are `<env>/gripl` and `<env>/auth`, so switching prod off for a while means both `prod/gripl` and `prod/auth`. The log shows only
your own runs unless you are in the `adm` or `systemd-journal` group; the service runs as the user it was set up for.

Run one update now, by hand: `/srv/gripl/bin/update-stack.sh /srv/gripl/<env>/gripl`.

### Change how often it runs

```bash
sudo systemctl edit "dbis-update@$(systemd-escape --path /srv/gripl/<env>/gripl).timer"
```

In the editor that opens, put the following (the empty line first clears the old value), save, then restart the timer and check:

```
[Timer]
OnUnitActiveSec=
OnUnitActiveSec=10min
```

```bash
sudo systemctl restart "dbis-update@$(systemd-escape --path /srv/gripl/<env>/gripl).timer"; systemctl list-timers 'dbis-update@*'
```

For all stacks at once use `sudo systemctl edit dbis-update@.timer`.

### Update the updater itself

`/srv/gripl/bin` points into the checkout `/srv/gripl/traefik/repo`, which follows `main` and does not update itself:

```bash
git -C /srv/gripl/traefik/repo fetch --depth 1 origin main && git -C /srv/gripl/traefik/repo reset --hard FETCH_HEAD
```

Implemented in: `deploy/scripts/update-stack.sh`, `deploy/systemd/dbis-update@.service` and `.timer`.

## Backups

**Automatic:** before every update that replaces something, the updater writes
`/srv/gripl/backups/<env>/<app>/<time>-before-update/` with `postgres.dump` (GRIPL), `mongo.archive` (auth-service) and, on prod when
the RAG service is replaced, `neo4j-data.tar.gz` and `rag-working-dir.tar.gz`. The newest 5 per stack are kept (`BACKUP_KEEP`). If the
backup fails, nothing is updated and the log says `BACKUP FAILED`. These backups are on the same server, so they protect against a bad
update, not against losing the machine: copy important ones elsewhere (`scp`).

### Make a backup of one system by hand

Only the stack you name is touched. Replace `<env>`; the files land in your home folder.

```bash
# GRIPL data (datasets, test cases, process models, settings)
cd /srv/gripl/<env>/gripl && docker compose exec -T gripl-postgres pg_dump -U postgres -d gripl_db --format=custom > ~/gripl-<env>-$(date +%F).dump
```
```bash
# accounts and roles
cd /srv/gripl/<env>/auth && docker compose exec -T auth-mongo sh -c 'mongodump --quiet --archive --db auth_db -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin' > ~/auth-<env>-$(date +%F).archive
```
```bash
# knowledge graph (RAG + Neo4j are stopped for about a minute for a clean copy)
cd /srv/gripl/<env>/gripl && docker compose stop gripl-rag gripl-neo4j
for v in neo4j-data rag-working-dir; do docker run --rm -v gripl-<env>_$v:/from:ro -v "$HOME":/to alpine:3 tar -C /from -czf /to/gripl-<env>-$v-$(date +%F).tar.gz .; done
docker compose start gripl-neo4j gripl-rag
```

`POSTGRES_USER`/`POSTGRES_DB` are `postgres`/`gripl_db` unless the stack's `.env` says otherwise. Also keep a copy of the `.env` files:
without `SECRETS_ENCRYPTION_KEY` the stored OpenRouter keys are lost, and the database passwords must match the volumes.

### Restore a backup

This **overwrites** the current data of that one system. Stop the application that uses it first, restore, start it again.

```bash
# GRIPL data
cd /srv/gripl/<env>/gripl && docker compose stop gripl-backend
docker compose exec -T gripl-postgres pg_restore -U postgres -d gripl_db --clean --if-exists --no-owner --exit-on-error < /path/to/postgres.dump
docker compose start gripl-backend
```
```bash
# accounts and roles
cd /srv/gripl/<env>/auth && docker compose stop auth-service
docker compose exec -T auth-mongo sh -c 'mongorestore --quiet --archive --drop --nsInclude "auth_db.*" -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin' < /path/to/mongo.archive
docker compose start auth-service
```
```bash
# knowledge graph: replaces both volumes with the archives
cd /srv/gripl/<env>/gripl && docker compose stop gripl-rag gripl-neo4j
for v in neo4j-data rag-working-dir; do docker run --rm -v gripl-<env>_$v:/to -v /path/to/folder:/from:ro alpine:3 sh -c "find /to -mindepth 1 -delete && tar -C /to -xzf /from/$v.tar.gz"; done
docker compose start gripl-neo4j gripl-rag
```

For the knowledge graph, `/path/to/folder` holds the two `.tar.gz` files named `neo4j-data.tar.gz` and `rag-working-dir.tar.gz` (the
automatic backups are already named like this). If the restore is the result of a bad release, roll back the image as well
([deploy-to-prod.md](deploy-to-prod.md)).

Implemented in: backup code in `deploy/scripts/update-stack.sh`.

## Knowledge base (RAG)

Ingesting the documents costs OpenRouter credit; watch the first documents before leaving it alone.

```bash
cd /srv/gripl/<env>/gripl && docker compose exec -d gripl-rag python scripts/ingest.py
docker compose logs -f gripl-rag
```

To avoid paying twice, copy an already ingested knowledge graph from the other environment:
`/srv/gripl/bin/clone-prod-to-dev.sh --with-rag` (needs the same `NEO4J_PASSWORD` in both `.env` files).

## Give dev a copy of prod's data

```bash
/srv/gripl/prod/auth/repo/deploy/scripts/clone-prod-to-dev.sh      # accounts and roles
/srv/gripl/bin/clone-prod-to-dev.sh [--with-rag]                   # GRIPL data (+ knowledge graph)
```

Prod is only read. Dev keeps its own token signing key and `SECRETS_ENCRYPTION_KEY`; stored OpenRouter keys are not copied.
