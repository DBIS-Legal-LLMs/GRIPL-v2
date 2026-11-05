#!/usr/bin/env bash
# Pull the newest images for one stack and apply them.
#
#   update-stack.sh /srv/gripl/prod/gripl
#
# Meant to run every few minutes from a systemd timer (deploy/systemd) or cron
# as the `deploy` user. It is idempotent: when nothing changed, `compose pull` and
# `compose up -d` do nothing and the stack keeps running untouched.
#
# Before it changes anything that is running it takes a backup (a new backend image can apply
# database migrations, and those cannot be undone by going back to the old image):
#   Postgres (GRIPL) and MongoDB (auth-service) are dumped, and on prod the knowledge-graph
#   volumes (Neo4j + RAG working dir) are archived when the RAG service is about to be replaced.
#   Backups go to $BACKUP_DIR/<env>/<app>/<timestamp>-before-update/ (default /srv/gripl/backups),
#   the newest $BACKUP_KEEP (default 5) are kept. If the backup fails, nothing is updated.
#   BACKUP_VOLUMES=1|0 overrides the default (1 on prod, 0 elsewhere).
#
# A stack directory (created by init-stack.sh) contains:
#   repo/          git checkout of the app repo on the stack's branch (main|dev)
#   compose.yaml   symlink to repo/deploy/<app>/docker-compose.yml
#   .env, ...      the stack's own configuration and secrets (not in git)
set -euo pipefail

dir=${1:?usage: update-stack.sh <stack-dir>}
cd "$dir"
name="$(basename "$(dirname "$PWD")")/$(basename "$PWD")"
log() { printf '%s [%s] %s\n' "$(date -Is)" "$name" "$*"; }
env_value() { sed -n "s/^$1=//p" .env 2>/dev/null | tail -n1; }

[ -f compose.yaml ] || { log "no compose.yaml here - was this directory created with init-stack.sh?"; exit 2; }

# never run two updates of the same stack at once
exec 9>.update.lock
flock -n 9 || { log "another update is still running, skipping"; exit 0; }

# 1. refresh the compose definition itself (cheap; only if the stack has a repo checkout)
if [ -d repo/.git ]; then
  branch=$(git -C repo rev-parse --abbrev-ref HEAD)
  before=$(git -C repo rev-parse HEAD)
  git -C repo fetch --quiet --depth 1 origin "$branch"
  git -C repo reset --quiet --hard FETCH_HEAD
  after=$(git -C repo rev-parse HEAD)
  [ "$before" = "$after" ] || log "compose definition updated ${before:0:7} -> ${after:0:7} ($branch)"
fi

# 2. pull images, one service at a time. Pulling them in parallel makes containerd fail with
#    "failed commit on ref layer-sha256..." when images share layers. A registry outage must not
#    touch the running stack.
pull_all() {
  local svc attempt
  for svc in $(docker compose config --services); do
    for attempt in 1 2 3; do
      docker compose pull --quiet "$svc" && continue 2
      log "pull of $svc failed (attempt $attempt of 3)"; sleep 5
    done
    return 1
  done
}
if ! pull_all; then
  log "image pull failed - leaving the running containers as they are"
  exit 1
fi

# 3. what would `up` change? (a dry run: nothing is touched)
plan=$(docker compose up -d --remove-orphans --dry-run 2>&1) || {
  log "dry run failed:"; printf '%s\n' "$plan"; exit 1
}
if ! grep -q -E ' (Recreate|Creating) *$' <<<"$plan"; then
  docker image prune --force >/dev/null
  exit 0   # nothing to apply
fi

# 3a. backup before anything that is running gets replaced
backup_root=${BACKUP_DIR:-$(dirname "$(dirname "$PWD")")/backups}
backup_keep=${BACKUP_KEEP:-5}
environment=$(env_value ENVIRONMENT); environment=${environment:-$(basename "$(dirname "$PWD")")}
project=$(env_value COMPOSE_PROJECT_NAME)
backup_volumes=${BACKUP_VOLUMES:-$([ "$environment" = prod ] && echo 1 || echo 0)}
services=$(docker compose config --services)
running() { [ -n "$(docker compose ps --status running -q "$1" 2>/dev/null)" ]; }

backup_dir="$backup_root/$(basename "$(dirname "$PWD")")/$(basename "$PWD")"
dest="$backup_dir/$(date +%Y%m%dT%H%M%S)-before-update"
backup_failed() {
  log "BACKUP FAILED ($*) - nothing was updated"; rm -rf "$dest"; exit 1
}

if grep -q -x gripl-postgres <<<"$services" && running gripl-postgres; then
  [ -d "$dest" ] || install -d -m 700 "$dest"
  pg_user=$(env_value POSTGRES_USER); pg_user=${pg_user:-postgres}
  pg_db=$(env_value POSTGRES_DB);     pg_db=${pg_db:-gripl_db}
  log "backup: Postgres $pg_db"
  docker compose exec -T gripl-postgres pg_dump -U "$pg_user" -d "$pg_db" --format=custom > "$dest/postgres.dump" \
    || backup_failed "pg_dump"
  # the dump must be readable, an empty or broken file is as good as none
  docker compose exec -T gripl-postgres pg_restore --list < "$dest/postgres.dump" >/dev/null \
    || backup_failed "the Postgres dump cannot be read back"
fi

if grep -q -x auth-mongo <<<"$services" && running auth-mongo; then
  [ -d "$dest" ] || install -d -m 700 "$dest"
  mongo_db=$(env_value MONGO_DB_NAME); mongo_db=${mongo_db:-auth_db}
  log "backup: MongoDB $mongo_db"
  # run the tool inside the container so the credentials never leave it
  docker compose exec -T auth-mongo sh -c "mongodump --quiet --archive --db '$mongo_db' -u \"\$MONGO_INITDB_ROOT_USERNAME\" -p \"\$MONGO_INITDB_ROOT_PASSWORD\" --authenticationDatabase admin" \
    > "$dest/mongo.archive" || backup_failed "mongodump"
  [ -s "$dest/mongo.archive" ] || backup_failed "the MongoDB archive is empty"
fi

# the knowledge graph is expensive to rebuild (it costs OpenRouter credit): archive its volumes when
# the RAG service is about to be replaced. They are stopped for a clean copy; `up` starts them again.
if [ "$backup_volumes" = 1 ] && [ -n "$project" ] && grep -q -E 'gripl-(rag|neo4j)-[0-9]+ +Recreate *$' <<<"$plan" \
   && grep -q -x gripl-neo4j <<<"$services"; then
  [ -d "$dest" ] || install -d -m 700 "$dest"
  log "backup: knowledge graph volumes (stopping gripl-rag and gripl-neo4j for a clean copy)"
  docker compose stop gripl-rag gripl-neo4j >/dev/null || backup_failed "could not stop the RAG services"
  for vol in neo4j-data rag-working-dir; do
    docker volume inspect "${project}_$vol" >/dev/null 2>&1 || continue
    docker run --rm -v "${project}_$vol":/from:ro -v "$dest":/to alpine:3 \
      tar -C /from -czf "/to/$vol.tar.gz" . || backup_failed "archive of volume $vol"
  done
fi

if [ -d "$dest" ]; then
  log "backup written to $dest ($(du -sh "$dest" | cut -f1))"
  # keep the newest $backup_keep, drop the rest
  ls -1dt "$backup_dir"/*-before-update 2>/dev/null | tail -n "+$((backup_keep + 1))" | xargs -r rm -rf
fi

# 3b. recreate only what changed (compose compares image IDs and configuration);
#     --wait makes a container that never becomes healthy show up as a failed run.
out=$(docker compose up -d --remove-orphans --wait --wait-timeout "${UPDATE_WAIT_TIMEOUT:-300}" 2>&1) || {
  log "up failed:"; printf '%s\n' "$out"
  [ -d "$dest" ] && log "the pre-update backup is in $dest"
  exit 1
}
if grep -q -E 'Recreated|Created|Started' <<<"$out"; then
  log "applied changes:"; grep -E 'Recreated|Created|Started' <<<"$out" | sed 's/^/    /'
fi

# 4. drop images that no container uses any more (only untagged leftovers)
docker image prune --force >/dev/null
