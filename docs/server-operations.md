# Server operations

Everything here runs on the server over SSH (university network or VPN). `<env>` is `dev` or `prod`.

## Layout

```
/srv/gripl/
  bin/            -> traefik/repo/deploy/scripts   (the updater and the clone scripts)
  backups/<env>/<app>/<time>-before-update/        (automatic backups, see below)
  traefik/        shared reverse proxy
  <env>/auth/     auth-service + MongoDB        compose.yaml, .env, logs/, repo/
  <env>/gripl/    frontend, backend, RAG, Neo4j, Postgres   compose.yaml, .env, rag.env, logs/, repo/
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

## Error logs

Besides the container logs, the backend of each stack writes its warnings and errors to a file of its own, one file per day, in the `logs/`
folder of the stack (not in git, not part of any backup):

```
/srv/gripl/<env>/gripl/logs/gripl-backend-errors-2026-11-23.log
/srv/gripl/<env>/auth/logs/auth-service-errors-2026-11-23.log
```

Only warnings and errors are in them (`docker compose logs` has everything else). The application keeps the last 30 days and deletes older
files itself, and keeps the files of the past days under 200 MB together, so nothing has to be cleaned up by hand.

```bash
cd /srv/gripl/<env>/gripl
ls -lh logs/
less logs/gripl-backend-errors-$(date +%F).log      # one day; change the date for another one
grep -h -A 25 "requestId=a1b2c3d4" logs/*.log       # a failure by the request id the user saw, with its cause chain
grep -c ' ERROR ' logs/*.log                         # errors per day
```

A failure is one line (`code=`, `requestId=` and the ids of what failed, e.g. `processModelId=` or `testCaseId=`) followed by its cause chain
(`Caused by:` lines). Prompts, process models and API keys are not logged.

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

### Update now, by hand

When the timer only runs at night (see below) and a promoted version should be live earlier, run the updater yourself, once per
stack. It does exactly what the timer does: refresh the checkout, pull, back up, recreate what changed.

```bash
/srv/gripl/bin/update-stack.sh /srv/gripl/prod/gripl
/srv/gripl/bin/update-stack.sh /srv/gripl/prod/auth
```

Look at the last lines of its output: `applied changes` lists what was restarted, `BACKUP FAILED` means nothing was touched.

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

### Update once a night (prod)

Prod should not restart during the day. This replaces the 3-minute interval by a run at 03:00 for both prod stacks;
`Persistent=true` makes a run that was missed because the server was down happen at the next boot.

```bash
for s in gripl auth; do
  unit="dbis-update@$(systemd-escape --path /srv/gripl/prod/$s).timer"
  sudo mkdir -p "/etc/systemd/system/$unit.d"
  printf '[Timer]\nOnBootSec=\nOnUnitActiveSec=\nOnCalendar=\nOnCalendar=*-*-* 03:00:00\nPersistent=true\n' | sudo tee "/etc/systemd/system/$unit.d/override.conf" >/dev/null
  sudo systemctl daemon-reload
  sudo systemctl enable --now "$unit"
done
systemctl list-timers 'dbis-update@*'
```

The list must show the prod timers with a `NEXT` of 03:00 tomorrow. Nothing is paused for the backup: the databases are dumped while they run, and
a container is only restarted when its image or configuration changed. A restart takes seconds to a minute; when the RAG image
changes on prod, the knowledge graph is stopped for the copy of its volumes, which takes a few minutes longer.

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

**Old backups still contain OpenRouter keys.** Until migration `V20` (GRIPL#62), the key of a user who started an analysis was stored in plain
text in `process_model.analysis_options`. A backup or dump made before the release with `V20` still holds those keys, and so does a copy
made by `clone-prod-to-dev.sh` earlier. They are gone from the live databases once `V20` has run, and the automatic backups roll out of the
newest 5. To get rid of them sooner, delete the older folders in `/srv/gripl/backups/<env>/gripl/` by hand, and delete dumps you copied elsewhere.
Treat a key as exposed if someone who should not see it could have read a backup, and have its owner replace it at OpenRouter. Restoring such a
dump is safe: the backend runs `V20` again when it starts.

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
# a restore also brings back refresh tokens that were revoked since the backup: drop them (users log in again);
# auth-service recreates the collection and its indexes when it starts
docker compose exec -T auth-mongo sh -c 'mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin auth_db --eval "db.refresh_tokens.drop()"'
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

## Renew the HTTPS certificate

Traefik serves a self-signed certificate until the project has a domain. It lives in `/srv/gripl/traefik/certs/`: `tls.crt` and `tls.key`
are what Traefik serves, `ca.crt` is the public CA that people install in their browser, `ca.key` is the CA's secret key and never leaves the
server. The script issues a server certificate that is valid for **397 days** (the longest lifetime browsers accept), so it has to be renewed
**at the latest 397 days after the last run**. The first one was issued on 2026-10-16 and runs out on **2027-11-17**; renew it well before,
for example in October 2027, and put that date in a calendar.

Check how long the certificate is still valid:

```bash
openssl x509 -in /srv/gripl/traefik/certs/tls.crt -noout -enddate
```

`openssl x509 -in /srv/gripl/traefik/certs/tls.crt -noout -checkend 2592000` prints whether it expires within 30 days. To see what Traefik
really serves, from any machine: `openssl s_client -connect gripl.134.60.71.167.nip.io:443 -servername gripl.134.60.71.167.nip.io </dev/null 2>/dev/null | openssl x509 -noout -enddate`.

Renew it (this keeps the CA and issues a new `tls.crt` and `tls.key`; Traefik reads them at start, so it has to be restarted, which
interrupts open connections for a second):

```bash
cd /srv/gripl/traefik && repo/deploy/traefik/make-selfsigned-cert.sh 134.60.71.167.nip.io 134.60.71.167 && docker compose restart traefik
```

The CA is limited by X.509 name constraints to the server's base domain and IP, so it can only vouch for this server. For that reason the
script checks the new certificate against the existing CA and stops with an error, changing nothing, if they do not fit together (the
domain or IP changed, or the CA expired); the fix is a new CA, see [rebuild-server.md](rebuild-server.md).

Run the `openssl ... -enddate` command again; the date must be about 13 months away. Nothing has to be done on the users' machines: the
installed `ca.crt` stays valid and the browser accepts the new certificate by itself. The CA was created on 2026-10-16 and is valid for
five years, until **2031-10-15**. Plan a new CA by then ([rebuild-server.md](rebuild-server.md) says what that means for users).

If the certificate expires, the site still works but every browser stops with "Your connection is not private" (`NET::ERR_CERT_DATE_INVALID`)
even where the CA is installed; users can click through the warning, scripts and API clients fail. Renewing fixes it at once.

Implemented in: `deploy/traefik/make-selfsigned-cert.sh`; the certificate is configured in `deploy/traefik/dynamic/tls.yml`.
