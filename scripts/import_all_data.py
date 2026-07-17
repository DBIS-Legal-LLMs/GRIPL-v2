#!/usr/bin/env python3
"""
Import datasets and evaluation data from CSV into PostgreSQL.
Imports dataset/dataset.csv first, then dataset/evaluation_data.csv.
"""

import csv
import json
import os
import sys
from datetime import datetime
import psycopg2
from psycopg2.extras import execute_values
from dotenv import load_dotenv

csv.field_size_limit(int(1e8))
load_dotenv(".env.local")

# Windows console UTF-8 fix
if sys.stdout.encoding != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

DB_HOST = os.getenv("POSTGRES_HOST", "localhost")
DB_PORT = os.getenv("POSTGRES_PORT", "5432")
DB_USER = os.getenv("POSTGRES_USER", "postgres")
DB_PASSWORD = os.getenv("POSTGRES_PASSWORD", "postgres")
DB_NAME = os.getenv("POSTGRES_DB", "gripldb")


def import_datasets(cursor):
    print("\n--- Importing datasets ---")
    with open("dataset/dataset.csv", "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = []
        skipped = 0
        for row in reader:
            dataset_id = int(row["id"])
            cursor.execute("SELECT 1 FROM dataset WHERE id = %s", (dataset_id,))
            if cursor.fetchone():
                skipped += 1
                continue
            rows.append((
                dataset_id,
                row["name"],
                row.get("description") or None,
                row["created_at"],
                row["updated_at"],
            ))

    if not rows:
        print(f"  No new datasets to import (skipped {skipped} existing).")
        return

    execute_values(cursor, """
        INSERT INTO dataset (id, name, description, created_at, updated_at)
        VALUES %s
    """, rows)

    # Reset sequence so future inserts don't collide with imported IDs
    cursor.execute("SELECT setval('dataset_id_seq', (SELECT MAX(id) FROM dataset))")

    print(f"  ✓ Inserted {len(rows)} datasets (skipped {skipped} existing).")


def import_evaluation_data(cursor):
    print("\n--- Importing evaluation data ---")
    rows = []
    skipped = 0
    with open("dataset/evaluation_data.csv", "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for i, row in enumerate(reader, 1):
            try:
                row_id = int(row["id"]) if row.get("id") else None
                if row_id:
                    cursor.execute("SELECT 1 FROM evaluation_data WHERE id = %s", (row_id,))
                    if cursor.fetchone():
                        skipped += 1
                        continue

                bpmn_xml = row.get("bpmn_xml", "")
                expected_values = json.loads(row.get("expected_values", "[]"))
                name = row.get("name") or f"Test Case {i}"
                dataset_id_str = row.get("dataset_id", "").strip()
                dataset_id = int(dataset_id_str) if dataset_id_str else None

                rows.append((row_id, bpmn_xml, json.dumps(expected_values), name, dataset_id, datetime.now(), datetime.now()))
            except Exception as e:
                print(f"  Warning: skipping row {i}: {e}")

    if not rows:
        print(f"  No new evaluation data to import (skipped {skipped} existing).")
        return

    execute_values(cursor, """
        INSERT INTO evaluation_data (id, bpmn_xml, expected_values, name, dataset_id, created_at, updated_at)
        VALUES %s
    """, rows, page_size=100)
    # Reset sequence past the max imported ID
    cursor.execute("SELECT setval('evaluation_data_id_seq', (SELECT MAX(id) FROM evaluation_data))")

    print(f"  ✓ Inserted {len(rows)} rows (skipped {skipped} existing).")


def main():
    print(f"Connecting to PostgreSQL at {DB_HOST}:{DB_PORT}/{DB_NAME}...")
    try:
        conn = psycopg2.connect(
            host=DB_HOST, port=DB_PORT, user=DB_USER,
            password=DB_PASSWORD, database=DB_NAME
        )
        cursor = conn.cursor()
        print("✓ Connected")

        import_datasets(cursor)
        import_evaluation_data(cursor)

        conn.commit()

        cursor.execute("SELECT COUNT(*) FROM dataset")
        print(f"\n✓ Total datasets: {cursor.fetchone()[0]}")
        cursor.execute("SELECT COUNT(*) FROM evaluation_data")
        print(f"✓ Total evaluation_data rows: {cursor.fetchone()[0]}")

        cursor.close()
        conn.close()
        print("✓ Done!")
        return 0

    except psycopg2.Error as e:
        print(f"\n✗ Database error: {e}")
        return 1
    except FileNotFoundError as e:
        print(f"\n✗ File not found: {e}")
        return 1
    except Exception as e:
        import traceback
        print(f"\n✗ Error: {e}")
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
