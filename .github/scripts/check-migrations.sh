#!/usr/bin/env bash
# Guard for the Flyway migrations of the GRIPL backend (they run on every database, including prod).
#
#   check-migrations.sh <base-ref>        e.g. origin/dev
#
# Fails (exit 1) when the pull request
#   * edits, renames or deletes a migration that already exists: Flyway stores a checksum of every
#     applied file and refuses to start the backend when one changed, so dev or prod would not come up;
#   * adds a migration whose version is not higher than the highest one on the base branch: Flyway does
#     not apply "out of order" migrations, and two pull requests that both add V17 collide the same way.
# Warns (a visible annotation, no failure) about statements that can lose data in new migrations:
# DROP TABLE/COLUMN/SCHEMA, TRUNCATE, DELETE FROM, ALTER COLUMN ... TYPE, RENAME. The warning is the
# reminder to try the migration on a copy of prod's data before the release is approved.
# Needs the full history (actions/checkout with fetch-depth: 0).
set -euo pipefail

base=${1:?usage: check-migrations.sh <base-ref>}
dir=gripl/gripl-backend/src/main/resources/db/migration
status=0

error() { echo "::error file=$1::$2"; status=1; }
warn()  { echo "::warning file=$1,line=$2::$3"; }

# version of a migration file name as something `sort -V` understands: V1_2__x.sql -> 1.2
version_of() { basename "$1" | sed -E 's/^V([0-9]+([._][0-9]+)*)__.*/\1/; s/_/./g'; }

# 1. versioned migrations that exist on the base branch must stay exactly as they are
while IFS=$'\t' read -r st old new; do
  case "$st" in
    A*) ;;
    *) [[ $old =~ /V[0-9][^/]*__[^/]*\.sql$ ]] \
         && error "$old" "An existing migration was changed (git status $st). Applied migrations are checksummed by Flyway; the backend would not start on a database that already ran it. Add a new migration instead." ;;
  esac
done < <(git diff --name-status -M "$base"...HEAD -- "$dir")

# 2. new migrations must be newer than everything on the base branch
highest=$(git ls-tree -r --name-only "$base" -- "$dir" | grep -E '/V[0-9][^/]*__[^/]*\.sql$' \
          | while read -r f; do version_of "$f"; done | sort -V | tail -n1 || true)
added=$(git diff --name-only --diff-filter=A "$base"...HEAD -- "$dir" | grep -E '/V[0-9][^/]*__[^/]*\.sql$' || true)

for f in $added; do
  v=$(version_of "$f")
  if [ -n "$highest" ] && [ "$(printf '%s\n%s\n' "$highest" "$v" | sort -V | tail -n1)" != "$v" ] || [ "$v" = "$highest" ]; then
    error "$f" "Version $v is not higher than $highest, the newest migration on $base. Rename it to the next free number (someone else may have merged one in the meantime)."
  fi
  # 3. statements that can lose data (comments are ignored)
  while IFS=: read -r line text; do
    warn "$f" "$line" "This migration contains a statement that can lose or reshape data: ${text# }. Before the release is approved, run it on dev against a copy of prod's data (deploy/scripts/clone-prod-to-dev.sh)."
  done < <(grep -n -i -E '\b(drop[[:space:]]+(table|column|schema|database)|truncate|delete[[:space:]]+from|alter[[:space:]]+column[^;]*[[:space:]]type|rename[[:space:]]+(to|column))\b' "$f" | grep -v -E '^[0-9]+:[[:space:]]*--' || true)
done

if [ -z "$added" ] && [ "$status" = 0 ]; then echo "No migration changes."; fi
[ "$status" = 0 ] && [ -n "$added" ] && echo "New migrations: $(echo $added | xargs -n1 basename | tr '\n' ' ')(newest on $base: ${highest:-none})"
exit $status
