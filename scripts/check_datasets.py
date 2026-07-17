#!/usr/bin/env python3
"""Check existing datasets and required dataset_ids from CSV."""

import csv
import os
from dotenv import load_dotenv
import psycopg2

# Load environment variables
load_dotenv(".env.local")

# Database configuration
DB_HOST = os.getenv("POSTGRES_HOST", "localhost")
DB_PORT = os.getenv("POSTGRES_PORT", "5432")
DB_USER = os.getenv("POSTGRES_USER", "postgres")
DB_PASSWORD = os.getenv("POSTGRES_PASSWORD", "postgres")
DB_NAME = os.getenv("POSTGRES_DB", "gripldb")

# Check existing datasets
print("Checking existing datasets in database...")
try:
    conn = psycopg2.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD, database=DB_NAME
    )
    cursor = conn.cursor()
    cursor.execute("SELECT id, name FROM dataset ORDER BY id")
    datasets = cursor.fetchall()
    print(f"✓ Existing datasets: {datasets if datasets else 'NONE'}")

    cursor.close()
    conn.close()
except Exception as e:
    print(f"✗ Database error: {e}")

# Check CSV dataset_ids
print("\nChecking dataset_ids in CSV...")
dataset_ids = set()
try:
    with open("dataset/evaluation_data.csv", "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            did = row.get("dataset_id", "").strip()
            if did:
                dataset_ids.add(int(did))
    print(f"✓ Dataset IDs required by CSV: {sorted(dataset_ids)}")
except FileNotFoundError:
    print(f"✗ CSV file not found")

# Compare
missing_ids = sorted(dataset_ids - {d[0] for d in (datasets or [])})
if missing_ids:
    print(f"\n⚠️  Missing dataset IDs: {missing_ids}")
else:
    print("\n✓ All required datasets exist!")
