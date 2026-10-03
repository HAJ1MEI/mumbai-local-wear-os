#!/usr/bin/env python3
"""
Mumbai Local Watch Database Validator

Validates a generated mumbai-watch.db for correctness:
- SQLite integrity check
- Required tables exist
- Foreign key validity
- Row counts > 0
- Schema version present
- Metadata consistency
"""

import sqlite3
import os
import sys
import argparse


REQUIRED_TABLES = {"metadata", "stations", "trains", "stop_events"}

REQUIRED_METADATA_KEYS = {"schemaVersion", "version", "trainCount", "stationCount", "stopEventCount"}

MIN_EXPECTED_TRAINS = 100
MIN_EXPECTED_STATIONS = 50
MIN_EXPECTED_STOP_EVENTS = 1000


class ValidationError(Exception):
    pass


def validate_db(db_path, verbose=True):
    """Validate the Watch database. Returns True if valid, raises on failure."""
    errors = []

    def log(msg):
        if verbose:
            print(f"  {msg}")

    def check(condition, message):
        if not condition:
            errors.append(message)
            log(f"FAIL: {message}")
        else:
            log(f"PASS: {message}")

    print(f"\nValidating: {db_path}")
    print(f"{'='*50}")

    # 1. File exists and is non-empty
    check(os.path.exists(db_path), "Database file exists")
    if not os.path.exists(db_path):
        raise ValidationError(f"Database file not found: {db_path}")

    file_size = os.path.getsize(db_path)
    check(file_size > 0, f"Database file is non-empty ({file_size:,} bytes)")
    if file_size == 0:
        raise ValidationError("Database file is empty")

    # 2. SQLite opens
    try:
        conn = sqlite3.connect(db_path)
        conn.execute("PRAGMA foreign_keys=ON")
        cursor = conn.cursor()
        log("PASS: SQLite connection opened")
    except Exception as e:
        raise ValidationError(f"Cannot open SQLite database: {e}")

    # 3. Integrity check
    cursor.execute("PRAGMA integrity_check")
    integrity = cursor.fetchone()[0]
    check(integrity == "ok", f"Integrity check: {integrity}")

    # 4. Required tables exist
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = {row[0] for row in cursor.fetchall()}
    for table in REQUIRED_TABLES:
        check(table in tables, f"Table '{table}' exists")

    # 5. Row counts
    counts = {}
    for table in REQUIRED_TABLES:
        if table in tables:
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            count = cursor.fetchone()[0]
            counts[table] = count
            check(count > 0, f"Table '{table}' has {count:,} rows")

    train_count = counts.get("trains", 0)
    station_count = counts.get("stations", 0)
    stop_count = counts.get("stop_events", 0)

    check(train_count >= MIN_EXPECTED_TRAINS,
          f"Train count ({train_count:,}) >= minimum ({MIN_EXPECTED_TRAINS})")
    check(station_count >= MIN_EXPECTED_STATIONS,
          f"Station count ({station_count:,}) >= minimum ({MIN_EXPECTED_STATIONS})")
    check(stop_count >= MIN_EXPECTED_STOP_EVENTS,
          f"Stop event count ({stop_count:,}) >= minimum ({MIN_EXPECTED_STOP_EVENTS})")

    # 6. Metadata keys
    if "metadata" in tables:
        cursor.execute("SELECT key, value FROM metadata")
        meta = {row[0]: row[1] for row in cursor.fetchall()}
        for key in REQUIRED_METADATA_KEYS:
            check(key in meta, f"Metadata key '{key}' present (value={meta.get(key, 'MISSING')})")

        # Check schemaVersion is a number
        sv = meta.get("schemaVersion", "")
        try:
            schema_ver = int(sv)
            check(schema_ver >= 1, f"Schema version is valid ({schema_ver})")
        except ValueError:
            errors.append(f"Invalid schemaVersion: {sv}")

        # Cross-check counts
        if "trainCount" in meta:
            meta_trains = int(meta["trainCount"])
            check(meta_trains == train_count,
                  f"Metadata trainCount ({meta_trains}) matches actual ({train_count})")

    # 7. Foreign key check
    cursor.execute("PRAGMA foreign_key_check")
    fk_errors = cursor.fetchall()
    check(len(fk_errors) == 0,
          f"Foreign key check: {len(fk_errors)} violations" if fk_errors else "No foreign key violations")

    # 8. No orphan stop events
    if "stop_events" in tables and "trains" in tables:
        cursor.execute("""
            SELECT COUNT(*) FROM stop_events
            WHERE train_id NOT IN (SELECT id FROM trains)
        """)
        orphan_train = cursor.fetchone()[0]
        check(orphan_train == 0, f"No orphan stop events (train_id): {orphan_train}")

    if "stop_events" in tables and "stations" in tables:
        cursor.execute("""
            SELECT COUNT(*) FROM stop_events
            WHERE station_id NOT IN (SELECT id FROM stations)
        """)
        orphan_station = cursor.fetchone()[0]
        check(orphan_station == 0, f"No orphan stop events (station_id): {orphan_station}")

    # 9. Sample query: can we find trains from a known station?
    if "stop_events" in tables and "stations" in tables:
        cursor.execute("""
            SELECT s.code, COUNT(DISTINCT se.train_id) as train_count
            FROM stations s
            JOIN stop_events se ON se.station_id = s.id
            GROUP BY s.id
            ORDER BY train_count DESC
            LIMIT 3
        """)
        top_stations = cursor.fetchall()
        for st_code, st_trains in top_stations:
            log(f"INFO: Station {st_code}: {st_trains} trains")

    # 10. Check stop event ordering
    if "stop_events" in tables:
        cursor.execute("""
            SELECT train_id, COUNT(*) as cnt,
                   MIN(sequence) as min_seq, MAX(sequence) as max_seq
            FROM stop_events
            GROUP BY train_id
            HAVING min_seq != 0 OR max_seq != cnt - 1
            LIMIT 5
        """)
        bad_seq = cursor.fetchall()
        check(len(bad_seq) == 0,
              f"Stop event sequences are contiguous (0..N-1)" if not bad_seq
              else f"Sequence gaps found in {len(bad_seq)} trains")

    conn.close()

    # Summary
    print(f"\n{'='*50}")
    if errors:
        print(f"VALIDATION FAILED: {len(errors)} error(s)")
        for e in errors:
            print(f"  [X] {e}")
        return False
    else:
        print("VALIDATION PASSED [OK]")
        print(f"  Trains:      {train_count:,}")
        print(f"  Stations:    {station_count:,}")
        print(f"  Stop events: {stop_count:,}")
        print(f"  File size:   {file_size / (1024*1024):.2f} MB")
        return True


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Validate Watch SQLite database")
    parser.add_argument(
        "--db",
        default=os.path.join(os.path.dirname(__file__), "..", "data", "generated", "mumbai-watch.db"),
        help="Path to SQLite database to validate",
    )
    args = parser.parse_args()

    valid = validate_db(args.db)
    sys.exit(0 if valid else 1)
