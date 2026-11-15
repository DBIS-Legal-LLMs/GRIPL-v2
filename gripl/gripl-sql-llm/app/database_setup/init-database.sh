#!/bin/sh
set -eu

echo "Starting database initialization"

export DB_PATH="${DB_PATH:-/app/test.db}"

cd /app

mkdir -p "$(dirname "$DB_PATH")"


python -m scripts.insert_data_in_db
python -m scripts.fill_example_in_vector_database


echo "Database initialization completed"

exec "$@"