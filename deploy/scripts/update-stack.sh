#!/usr/bin/env bash
# Pull the newest images for one stack and apply them.
#
#   update-stack.sh /srv/dbis-universe/gripl/prod
#
# Meant to run every few minutes from a systemd timer (deploy/systemd) or cron
# as the `deploy` user. It is idempotent: when nothing changed, `compose pull` and
# `compose up -d` do nothing and the stack keeps running untouched.
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

# 2. pull images. A registry outage must not touch the running stack.
if ! docker compose pull --quiet; then
  log "image pull failed - leaving the running containers as they are"
  exit 1
fi

# 3. recreate only what changed (compose compares image IDs and configuration);
#    --wait makes a container that never becomes healthy show up as a failed run.
out=$(docker compose up -d --remove-orphans --wait --wait-timeout "${UPDATE_WAIT_TIMEOUT:-300}" 2>&1) || {
  log "up failed:"; printf '%s\n' "$out"; exit 1
}
if grep -q -E 'Recreated|Created|Started' <<<"$out"; then
  log "applied changes:"; grep -E 'Recreated|Created|Started' <<<"$out" | sed 's/^/    /'
fi

# 4. drop images that no container uses any more (only untagged leftovers)
docker image prune --force >/dev/null
