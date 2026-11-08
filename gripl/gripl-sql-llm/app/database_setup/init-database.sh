#!/bin/sh

set -eu

echo "===================================="
echo "Starting database initialization"
echo "===================================="

# Use the configured database path
export DB_PATH="${DB_PATH:-/app/data/test.db}"

# Ensure that the database directory exists
mkdir -p "$(dirname "$DB_PATH")"

echo "Database path: $DB_PATH"

# Execute the existing Python initialization script
python -m scripts.populate_database

echo "===================================="
echo "Database initialization completed"
echo "===================================="

# Start FastAPI
echo "Starting FastAPI..."

exec "$@"