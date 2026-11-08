#!/bin/sh
set -eu

echo "Starting database initialization"

export DB_PATH="${DB_PATH:-/app/test.db}"

mkdir -p "$(dirname "$DB_PATH")"

echo "Database path: $DB_PATH"

python -m scripts.insert_data_in_db

echo "Database initialization completed"

exec "$@"