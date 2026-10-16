#!/usr/bin/env bash
# Create (or re-link) one stack directory.
#
#   init-stack.sh <stack-dir> <git-url> <branch> <compose-path-inside-repo>
#
# e.g.
#   init-stack.sh /srv/dbis-universe/gripl/prod https://github.com/DBIS-Legal-LLMs/GRIPL-v2.git main deploy/gripl/docker-compose.yml
#   init-stack.sh /srv/dbis-universe/gripl/dev  https://github.com/DBIS-Legal-LLMs/GRIPL-v2.git dev  deploy/gripl/docker-compose.yml
#
# Clones the repo (just the branch tip) into <stack-dir>/repo, links the compose
# file as compose.yaml and copies every *.example next to it (env.example -> .env,
# rag.env.example -> rag.env, ...) unless the real file already exists. It never
# overwrites configuration and does not start anything.
set -euo pipefail

dir=${1:?usage: init-stack.sh <stack-dir> <git-url> <branch> <compose-path-in-repo>}
url=${2:?missing git url}
branch=${3:?missing branch}
compose=${4:?missing compose path inside the repo}

mkdir -p "$dir"
cd "$dir"

if [ -d repo/.git ]; then
  echo "repo/ already exists - leaving it (update-stack.sh keeps it current)"
else
  git clone --quiet --depth 1 --single-branch --branch "$branch" "$url" repo
fi
[ -f "repo/$compose" ] || { echo "repo/$compose does not exist on branch $branch" >&2; exit 1; }

ln -sfn "repo/$compose" compose.yaml

src="$(dirname "repo/$compose")"
shopt -s nullglob dotglob
for example in "$src"/*.example; do
  target="$(basename "${example%.example}")"
  [ "$target" = "env" ] && target=".env"
  if [ -e "$target" ]; then
    echo "keeping existing $target"
  else
    cp "$example" "$target"
    chmod 600 "$target"
    echo "created $target from $(basename "$example") - fill it in"
  fi
done

echo
echo "Stack directory ready: $dir"
echo "Next: edit the files above, then  cd $dir && docker compose up -d"
