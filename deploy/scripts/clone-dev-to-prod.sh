#!/usr/bin/env bash
# One-time cutover, GRIPL part: copy the GRIPL dev data into the (empty) prod stack, so the people who
# work on dev find their datasets, test cases and process models on prod.
#
#   clone-dev-to-prod.sh [--with-rag] [--force] [--keep-dev-stopped] [--yes]
#
# Run the auth-service counterpart FIRST (auth-service/deploy/scripts/clone-dev-to-prod.sh): GRIPL's
# data is owned by auth user ids, and the check at the end needs prod's accounts.
#
# What is copied:
#   always      Postgres (datasets, test cases, process models, per-user settings, Flyway history)
#   --with-rag  also the RAG knowledge graph (Neo4j volume + LightRAG working dir), so prod does not
#               have to pay for a re-ingestion. Needs the same NEO4J_PASSWORD in both .env files.
# Note: Postgres is copied as it is. Process models of dev may contain users' OpenRouter keys in clear
# text (GRIPL#62), they are copied too.
#
# Safety:
#   * dev is only ever READ: its app containers (and RAG parts with --with-rag) are stopped during the
#     copy and started again at the end (unless --keep-dev-stopped)
#   * refuses to run when prod already holds data, unless --force (which REPLACES prod's data)
#   * after the copy it checks that every owner of copied data exists as a user in prod's auth-service;
#     if not, prod's apps are NOT started
#   * the prod backend must be at least as new as dev's database schema: promote the version that dev
#     runs before you run this (an older backend refuses a database that is ahead of it)
# Environment: DBIS_ROOT (default /srv/gripl) - the folder holding {prod,dev}/{gripl,auth}.
set -euo pipefail

root=${DBIS_ROOT:-/srv/gripl}
prod="$root/prod/gripl"; dev="$root/dev/gripl"
prod_auth="$root/prod/auth"
with_rag=0 force=0 keep_dev_stopped=0 assume_yes=0
for arg in "$@"; do
  case "$arg" in
    --with-rag) with_rag=1 ;;
    --force) force=1 ;;
    --keep-dev-stopped) keep_dev_stopped=1 ;;
    --yes) assume_yes=1 ;;
    *) echo "unknown option: $arg" >&2; exit 2 ;;
  esac
done

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
die() { log "ERROR: $*" >&2; exit 1; }
env_value() { sed -n "s/^$2=//p" "$1/.env" | tail -n1; }

for d in "$prod" "$dev" "$prod_auth"; do [ -f "$d/compose.yaml" ] && [ -f "$d/.env" ] || die "$d is not a stack directory"; done
[ "$(env_value "$prod" ENVIRONMENT)" = prod ] || die "$prod/.env does not say ENVIRONMENT=prod"
[ "$(env_value "$dev" ENVIRONMENT)" = dev ]  || die "$dev/.env does not say ENVIRONMENT=dev"

prod_project=$(env_value "$prod" COMPOSE_PROJECT_NAME)
dev_project=$(env_value "$dev" COMPOSE_PROJECT_NAME)
[ -n "$prod_project" ] && [ -n "$dev_project" ] && [ "$prod_project" != "$dev_project" ] || die "COMPOSE_PROJECT_NAME missing or identical in the two .env files"
pg_user_prod=$(env_value "$prod" POSTGRES_USER); pg_user_prod=${pg_user_prod:-postgres}
pg_db_prod=$(env_value "$prod" POSTGRES_DB);     pg_db_prod=${pg_db_prod:-gripl_db}
pg_user_dev=$(env_value "$dev" POSTGRES_USER);   pg_user_dev=${pg_user_dev:-postgres}
pg_db_dev=$(env_value "$dev" POSTGRES_DB);       pg_db_dev=${pg_db_dev:-gripl_db}
auth_db=$(env_value "$prod_auth" MONGO_DB_NAME); auth_db=${auth_db:-auth_db}

if [ "$with_rag" = 1 ]; then
  # A Neo4j data directory carries its own password; prod must use the same one.
  [ "$(env_value "$prod" NEO4J_PASSWORD)" = "$(env_value "$dev" NEO4J_PASSWORD)" ] \
    || die "--with-rag needs the same NEO4J_PASSWORD in both .env files (the copied database keeps dev's password)"
fi

pcompose() { (cd "$prod" && docker compose "$@"); }
dcompose() { (cd "$dev" && docker compose "$@"); }
acompose() { (cd "$prod_auth" && docker compose "$@"); }
# psql helpers: -tA = bare values; stdin closed so `exec -T` cannot swallow input meant for a prompt
ppsql() { pcompose exec -T gripl-postgres psql -U "$pg_user_prod" -d "$pg_db_prod" -tAc "$1" </dev/null; }
dpsql() { dcompose exec -T gripl-postgres psql -U "$pg_user_dev" -d "$pg_db_dev" -tAc "$1" </dev/null; }

log "starting prod's database (a fresh stack creates its volumes now)"
pcompose up -d gripl-postgres --wait
# a fresh prod has no tables yet (the backend creates them on its first start), and Postgres resolves
# table names before it runs a CASE, so look for the table in a separate query
if [ "$(ppsql "select to_regclass('public.dataset') is not null")" = t ]; then
  prod_rows=$(ppsql "select (select count(*) from dataset) + (select count(*) from process_model) + (select count(*) from custom_analysis_endpoint) + (select count(*) from evaluation_data)")
else
  prod_rows=0
fi
[[ $prod_rows =~ ^[0-9]+$ ]] || die "could not read prod's data (got '$prod_rows')"
if [ "$prod_rows" != 0 ] && [ "$force" != 1 ]; then
  die "prod already holds data ($prod_rows rows); this script is for the first fill of an empty prod. Use --force only to REPLACE prod's data with dev's."
fi

if [ "$assume_yes" != 1 ]; then
  echo "This copies the GRIPL data of dev ($dev_project) into prod ($prod_project)$([ "$with_rag" = 1 ] && echo ' including the knowledge graph')."
  [ "$prod_rows" = 0 ] || echo "prod holds $prod_rows rows now; they are REPLACED."
  read -r -p "Type 'prod' to continue: " answer || die "aborted (no answer)"
  [ "$answer" = prod ] || die "aborted"
fi

# whatever happens below, dev is started again (prod's apps are only started on success)
restore() {
  if [ "$keep_dev_stopped" != 1 ]; then log "starting the dev stack again"; dcompose up -d --wait || true; fi
}
trap restore EXIT

log "stopping the app containers of dev and prod, so nothing is written during the copy"
dcompose stop gripl-frontend gripl-backend gripl-rag
pcompose stop gripl-frontend gripl-backend gripl-rag
dcompose up -d gripl-postgres --wait
dev_rows=$(dpsql "select (select count(*) from dataset) + (select count(*) from process_model) + (select count(*) from custom_analysis_endpoint) + (select count(*) from evaluation_data)")
[[ $dev_rows =~ ^[0-9]+$ ]] && [ "$dev_rows" != 0 ] || die "dev holds no data ('$dev_rows'), refusing to copy an empty database"

log "copying Postgres $pg_db_dev (dev) -> $pg_db_prod (prod)"
dcompose exec -T gripl-postgres pg_dump -U "$pg_user_dev" -d "$pg_db_dev" --format=custom --no-owner </dev/null \
  | pcompose exec -T gripl-postgres pg_restore -U "$pg_user_prod" -d "$pg_db_prod" --clean --if-exists --no-owner --exit-on-error

if [ "$with_rag" = 1 ]; then
  copy_volume() {  # <volume-name-suffix>
    local from="${dev_project}_$1" to="${prod_project}_$1"
    docker volume inspect "$from" >/dev/null || die "volume $from does not exist"
    docker volume inspect "$to" >/dev/null || die "volume $to does not exist"
    docker run --rm -v "$from":/from:ro -v "$to":/to alpine:3 \
      sh -c 'find /to -mindepth 1 -delete && cp -a /from/. /to/'
  }
  log "stopping dev's gripl-neo4j for a consistent copy, and prod's RAG parts"
  dcompose stop gripl-neo4j
  pcompose up --no-start gripl-neo4j gripl-rag   # creates prod's volumes the way Compose wants them
  pcompose stop gripl-neo4j gripl-rag
  log "copying the knowledge graph volumes: dev -> prod"
  copy_volume neo4j-data
  copy_volume rag-working-dir
fi

# every owner of copied data must be an account in prod, otherwise that data belongs to nobody there
log "checking that all owners exist in prod's auth-service"
acompose up -d auth-mongo --wait
owners=$(ppsql "select owner_user_id from dataset union select owner_user_id from process_model union select owner_user_id from custom_analysis_endpoint union select owner_user_id from user_analysis_settings" | grep -v '^$' | sort -u || true)
known=$(acompose exec -T auth-mongo sh -c "mongosh --quiet -u \"\$MONGO_INITDB_ROOT_USERNAME\" -p \"\$MONGO_INITDB_ROOT_PASSWORD\" --authenticationDatabase admin '$auth_db' --eval 'db.users.find({}, {_id: 1}).forEach(u => print(u._id.toString()))'" </dev/null | grep -E '^[0-9a-f]{24}$' | sort -u || true)
[ -n "$known" ] || die "prod's auth-service has no users: run the auth-service clone-dev-to-prod.sh first. Prod's apps were not started."
orphans=$(comm -23 <(printf '%s\n' "$owners") <(printf '%s\n' "$known") | grep -v '^$' || true)
if [ -n "$orphans" ]; then
  printf '%s\n' "$orphans" | sed 's/^/    owner without an account in prod: /' >&2
  die "$(wc -l <<<"$orphans") owners of copied data are not users in prod; users who registered on dev after the auth copy? Re-run the auth-service script, then this one with --force. Prod's apps were not started."
fi
unowned=$(ppsql "select (select count(*) from dataset where owner_user_id is null) + (select count(*) from process_model where owner_user_id is null) + (select count(*) from custom_analysis_endpoint where owner_user_id is null)")
log "owners ok: $(printf '%s\n' "$owners" | grep -c .) accounts own data${unowned:+; $unowned rows have no owner (they were hidden on dev already)}"

log "starting prod"
pcompose up -d --wait || die "prod did not become healthy. If the backend log says the database is ahead of it (Flyway), prod's image is older than dev's: promote the version dev runs first (docs/deploy-to-prod.md), then run 'docker compose up -d' in $prod"

log "check: datasets $(ppsql 'select count(*) from dataset') (dev $(dpsql 'select count(*) from dataset')), test cases $(ppsql 'select count(*) from evaluation_data') (dev $(dpsql 'select count(*) from evaluation_data')), process models $(ppsql 'select count(*) from process_model') (dev $(dpsql 'select count(*) from process_model'))"
log "done: prod holds a copy of dev's GRIPL data as of $(date -Is)"
