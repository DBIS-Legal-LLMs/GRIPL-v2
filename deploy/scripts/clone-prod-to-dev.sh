#!/usr/bin/env bash
# Replace the GRIPL dev databases with a copy of prod.
#
#   clone-prod-to-dev.sh [--with-rag] [--yes]
#
# What is copied:
#   always      Postgres (datasets, test cases, process models, per-user settings)
#   --with-rag  also the RAG knowledge graph (Neo4j volume + LightRAG working dir),
#               so dev does not have to pay for a re-ingestion. This briefly stops
#               prod's gripl-rag and gripl-neo4j (a minute or so) to get a clean copy.
#
# Prod is only ever READ. Everything in dev that is copied is overwritten.
# Environment: DBIS_ROOT (default /srv/gripl) — the folder holding {prod,dev}/gripl.
set -euo pipefail

root=${DBIS_ROOT:-/srv/gripl}
prod="$root/prod/gripl"
dev="$root/dev/gripl"
with_rag=0 assume_yes=0
for arg in "$@"; do
  case "$arg" in
    --with-rag) with_rag=1 ;;
    --yes) assume_yes=1 ;;
    *) echo "unknown option: $arg" >&2; exit 2 ;;
  esac
done

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
die() { log "ERROR: $*" >&2; exit 1; }
env_value() { sed -n "s/^$2=//p" "$1/.env" | tail -n1; }

for d in "$prod" "$dev"; do [ -f "$d/compose.yaml" ] && [ -f "$d/.env" ] || die "$d is not a stack directory"; done
[ "$(env_value "$prod" ENVIRONMENT)" = prod ] || die "$prod/.env does not say ENVIRONMENT=prod"
[ "$(env_value "$dev" ENVIRONMENT)" = dev ]  || die "$dev/.env does not say ENVIRONMENT=dev"

prod_project=$(env_value "$prod" COMPOSE_PROJECT_NAME)
dev_project=$(env_value "$dev" COMPOSE_PROJECT_NAME)
[ -n "$prod_project" ] && [ -n "$dev_project" ] && [ "$prod_project" != "$dev_project" ] || die "COMPOSE_PROJECT_NAME missing or identical in the two .env files"
pg_user=$(env_value "$prod" POSTGRES_USER); pg_user=${pg_user:-postgres}
pg_db=$(env_value "$prod" POSTGRES_DB);     pg_db=${pg_db:-gripl_db}

if [ "$with_rag" = 1 ]; then
  # A Neo4j data directory carries its own password; the dev stack must use the same one.
  [ "$(env_value "$prod" NEO4J_PASSWORD)" = "$(env_value "$dev" NEO4J_PASSWORD)" ] \
    || die "--with-rag needs the same NEO4J_PASSWORD in both .env files (the copied database keeps prod's password)"
fi

if [ "$assume_yes" != 1 ]; then
  echo "This OVERWRITES the dev databases ($dev_project) with a copy of prod ($prod_project)."
  read -r -p "Type 'dev' to continue: " answer
  [ "$answer" = dev ] || die "aborted"
fi

pcompose() { (cd "$prod" && docker compose "$@"); }
dcompose() { (cd "$dev" && docker compose "$@"); }

log "stopping the dev app containers"
dcompose stop gripl-frontend gripl-backend gripl-rag
dcompose up -d gripl-postgres --wait

log "copying Postgres $pg_db: prod -> dev"
pcompose exec -T gripl-postgres pg_dump -U "$pg_user" -d "$pg_db" --format=custom --no-owner \
  | dcompose exec -T gripl-postgres pg_restore -U "$pg_user" -d "$pg_db" --clean --if-exists --no-owner --exit-on-error

if [ "$with_rag" = 1 ]; then
  copy_volume() {  # <volume-name-suffix>
    local from="${prod_project}_$1" to="${dev_project}_$1"
    docker volume inspect "$from" >/dev/null || die "volume $from does not exist"
    docker volume create "$to" >/dev/null
    docker run --rm -v "$from":/from:ro -v "$to":/to alpine:3 \
      sh -c 'find /to -mindepth 1 -delete && cp -a /from/. /to/'
  }
  log "stopping prod's gripl-rag and gripl-neo4j for a consistent copy"
  dcompose stop gripl-neo4j
  pcompose stop gripl-rag gripl-neo4j
  trap 'log "restarting prod rag + neo4j"; pcompose start gripl-neo4j gripl-rag' EXIT
  log "copying the knowledge graph volumes: prod -> dev"
  copy_volume neo4j-data
  copy_volume rag-working-dir
  pcompose start gripl-neo4j gripl-rag
  trap - EXIT
fi

log "starting the dev stack"
dcompose up -d --wait
log "done: dev now holds a copy of prod's data as of $(date -Is)"
