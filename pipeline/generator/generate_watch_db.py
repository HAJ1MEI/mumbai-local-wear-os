#!/usr/bin/env python3
"""
Mumbai Local Watch Database Generator

Converts the extracted mumbai-local-timetable.json into a Watch-optimized
SQLite database (mumbai-watch.db).

Schema version: 1
"""

import json
import sqlite3
import os
import sys
import hashlib
import argparse
import datetime


SCHEMA_VERSION = 1

SCHEMA_SQL = """
-- Metadata key-value store
CREATE TABLE IF NOT EXISTS metadata (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

-- Station definitions
CREATE TABLE IF NOT EXISTS stations (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    code          TEXT NOT NULL UNIQUE,
    name          TEXT NOT NULL,
    display_name  TEXT,
    marathi_name  TEXT,
    live_code     TEXT,
    latitude      REAL,
    longitude     REAL
);

-- Lines definition
CREATE TABLE IF NOT EXISTS lines (
    line_code      TEXT PRIMARY KEY,
    name           TEXT NOT NULL,
    color_hex      TEXT NOT NULL,
    live_supported INTEGER NOT NULL DEFAULT 0
);

-- Line directions
CREATE TABLE IF NOT EXISTS line_directions (
    line_code      TEXT NOT NULL REFERENCES lines(line_code),
    direction_flag TEXT NOT NULL,
    label          TEXT NOT NULL,
    short_label    TEXT NOT NULL,
    arrow          TEXT NOT NULL,
    PRIMARY KEY (line_code, direction_flag)
);

-- Station to lines mapping (interchange stations belong to multiple lines)
CREATE TABLE IF NOT EXISTS station_lines (
    station_id INTEGER NOT NULL REFERENCES stations(id),
    line_code  TEXT NOT NULL REFERENCES lines(line_code),
    PRIMARY KEY (station_id, line_code)
);

-- Train definitions
CREATE TABLE IF NOT EXISTS trains (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    train_number      TEXT,
    train_name        TEXT,
    line              TEXT NOT NULL,
    line_code         TEXT NOT NULL REFERENCES lines(line_code),
    direction         TEXT NOT NULL,
    source_station_id INTEGER NOT NULL REFERENCES stations(id),
    dest_station_id   INTEGER NOT NULL REFERENCES stations(id),
    train_type        TEXT NOT NULL,
    is_ac             INTEGER NOT NULL DEFAULT 0,
    is_15_car         INTEGER NOT NULL DEFAULT 0,
    is_ladies_special INTEGER NOT NULL DEFAULT 0,
    operating_days    TEXT NOT NULL,
    departure_time    TEXT,
    arrival_time      TEXT,
    departure_minutes INTEGER,
    arrival_minutes   INTEGER,
    stops_count       INTEGER NOT NULL DEFAULT 0
);

-- Stop events
CREATE TABLE IF NOT EXISTS stop_events (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    train_id              INTEGER NOT NULL REFERENCES trains(id),
    station_id            INTEGER NOT NULL REFERENCES stations(id),
    sequence              INTEGER NOT NULL,
    minutes_from_midnight INTEGER NOT NULL,
    day_offset            INTEGER NOT NULL DEFAULT 0,
    arrival               TEXT,
    departure             TEXT,
    platform              TEXT,
    door_side             TEXT
);

-- Indexes for fast Watch queries
CREATE INDEX IF NOT EXISTS idx_trains_line ON trains(line_code);
CREATE INDEX IF NOT EXISTS idx_trains_source ON trains(source_station_id);
CREATE INDEX IF NOT EXISTS idx_trains_dest ON trains(dest_station_id);
CREATE INDEX IF NOT EXISTS idx_trains_type ON trains(train_type);
CREATE INDEX IF NOT EXISTS idx_trains_direction ON trains(direction);
CREATE INDEX IF NOT EXISTS idx_trains_number ON trains(train_number);
CREATE INDEX IF NOT EXISTS idx_stop_events_train ON stop_events(train_id);
CREATE INDEX IF NOT EXISTS idx_stop_events_station ON stop_events(station_id);
CREATE INDEX IF NOT EXISTS idx_stop_events_station_time ON stop_events(station_id, minutes_from_midnight);
CREATE INDEX IF NOT EXISTS idx_stop_events_train_seq ON stop_events(train_id, sequence);
CREATE INDEX IF NOT EXISTS idx_station_lines ON station_lines(station_id, line_code);
"""


def time_str_to_minutes(time_str):
    """Convert HH:MM to minutes from midnight."""
    if not time_str:
        return None
    parts = time_str.split(":")
    if len(parts) != 2:
        return None
    try:
        return int(parts[0]) * 60 + int(parts[1])
    except ValueError:
        return None


def generate_db(json_path, output_path):
    """Generate the Watch SQLite database from extracted JSON."""
    print(f"Reading JSON from {json_path}...")
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    metadata = data["metadata"]
    trains_data = data["trains"]

    print(f"  Total trains: {metadata['totalTrains']}")
    print(f"  Total stop events: {metadata['totalStopEvents']}")
    print(f"  Lines: {', '.join(metadata['lines'])}")

    # Remove existing DB if present
    if os.path.exists(output_path):
        os.remove(output_path)

    conn = sqlite3.connect(output_path)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    cursor = conn.cursor()

    # Create schema
    print("Creating schema...")
    cursor.executescript(SCHEMA_SQL)

    # Insert lines
    print("Inserting lines...")
    lines_data = [
        ("C", "Central", "#1E88E5", 1),
        ("W", "Western", "#E53935", 1),
        ("H", "Harbour", "#43A047", 1),
        ("T", "Trans-Harbour", "#8E24AA", 0),
        ("U", "Uran", "#FB8C00", 0),
        ("DVP", "Diva-Vasai-Panvel", "#00ACC1", 0),
    ]
    cursor.executemany("INSERT INTO lines (line_code, name, color_hex, live_supported) VALUES (?, ?, ?, ?)", lines_data)

    # Insert line directions from direction_labels.json
    dir_labels_path = os.path.join(os.path.dirname(__file__), "direction_labels.json")
    if os.path.exists(dir_labels_path):
        print("Inserting line directions...")
        with open(dir_labels_path, "r", encoding="utf-8") as f:
            dir_data = json.load(f)
        for line_code, dirs in dir_data.items():
            for dir_flag, info in dirs.items():
                cursor.execute(
                    "INSERT INTO line_directions (line_code, direction_flag, label, short_label, arrow) VALUES (?, ?, ?, ?, ?)",
                    (line_code, dir_flag, info["label"], info["short_label"], info["arrow"]),
                )

    # --- Pass 1: Collect all unique stations ---
    print("Collecting stations...")
    station_map = {}  # code -> {name, marathi, lat, lon}

    for train in trains_data:
        for stop in train["stations"]:
            code = stop["stationCode"].upper()
            if code not in station_map:
                station_map[code] = {
                    "name": stop["stationName"],
                    "marathi_name": stop.get("stationMarathi"),
                    "latitude": stop.get("latitude"),
                    "longitude": stop.get("longitude"),
                }

    # Insert stations
    print(f"Inserting {len(station_map)} stations...")
    station_id_map = {}  # code -> db id
    for code in sorted(station_map.keys()):
        info = station_map[code]
        cursor.execute(
            "INSERT INTO stations (code, name, display_name, marathi_name, live_code, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?)",
            (code, info["name"], info["name"], info["marathi_name"], code, info["latitude"], info["longitude"]),
        )
        station_id_map[code] = cursor.lastrowid

    # --- Pass 2: Insert trains and stop events ---
    print(f"Inserting {len(trains_data)} trains...")
    total_stops_inserted = 0

    for train in trains_data:
        source_code = train["source"].upper()
        dest_code = train["destination"].upper()

        # Some source/dest may not be in stop events (edge case) — ensure they exist
        source_id = station_id_map.get(source_code)
        dest_id = station_id_map.get(dest_code)

        if source_id is None:
            # Insert the missing station
            cursor.execute(
                "INSERT OR IGNORE INTO stations (code, name, display_name, live_code) VALUES (?, ?, ?, ?)",
                (source_code, train["source"], train["source"], source_code),
            )
            if cursor.lastrowid:
                station_id_map[source_code] = cursor.lastrowid
                source_id = cursor.lastrowid
            else:
                cursor.execute("SELECT id FROM stations WHERE code = ?", (source_code,))
                source_id = cursor.fetchone()[0]
                station_id_map[source_code] = source_id

        if dest_id is None:
            cursor.execute(
                "INSERT OR IGNORE INTO stations (code, name, display_name, live_code) VALUES (?, ?, ?, ?)",
                (dest_code, train["destination"], train["destination"], dest_code),
            )
            if cursor.lastrowid:
                station_id_map[dest_code] = cursor.lastrowid
                dest_id = cursor.lastrowid
            else:
                cursor.execute("SELECT id FROM stations WHERE code = ?", (dest_code,))
                dest_id = cursor.fetchone()[0]
                station_id_map[dest_code] = dest_id

        dep_minutes = time_str_to_minutes(train.get("departureTime"))
        arr_minutes = time_str_to_minutes(train.get("arrivalTime"))

        operating_days = ",".join(train.get("operatingDays", []))

        cursor.execute(
            """INSERT INTO trains (
                train_number, train_name, line, line_code, direction,
                source_station_id, dest_station_id, train_type,
                is_ac, is_15_car, is_ladies_special, operating_days,
                departure_time, arrival_time, departure_minutes, arrival_minutes,
                stops_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                train.get("trainNumber"),
                train.get("trainName"),
                train["line"],
                train["lineCode"],
                train["direction"],
                source_id,
                dest_id,
                train["trainType"],
                1 if train.get("isAC") else 0,
                1 if train.get("is15Car") else 0,
                1 if train.get("isLadiesSpecial") else 0,
                operating_days,
                train.get("departureTime"),
                train.get("arrivalTime"),
                dep_minutes,
                arr_minutes,
                train.get("stopsCount", len(train.get("stations", []))),
            ),
        )
        train_db_id = cursor.lastrowid

        # Insert stop events with day_offset calculation
        prev_mins = -1
        cur_day_offset = 0
        for seq, stop in enumerate(train.get("stations", [])):
            st_code = stop["stationCode"].upper()
            st_id = station_id_map.get(st_code)
            if st_id is None:
                continue

            mins = stop["minutesFromMidnight"]
            if prev_mins != -1 and mins < prev_mins:
                cur_day_offset = 1
            prev_mins = mins

            cursor.execute(
                """INSERT INTO stop_events (
                    train_id, station_id, sequence, minutes_from_midnight, day_offset,
                    arrival, departure, platform, door_side
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    train_db_id,
                    st_id,
                    seq,
                    mins,
                    cur_day_offset,
                    stop.get("arrival"),
                    stop.get("departure"),
                    stop.get("platform"),
                    stop.get("doorSide"),
                ),
            )
            total_stops_inserted += 1

    # Populate station_lines
    print("Populating station_lines...")
    cursor.execute("""
        INSERT OR IGNORE INTO station_lines (station_id, line_code)
        SELECT DISTINCT se.station_id, t.line_code
        FROM stop_events se
        JOIN trains t ON se.train_id = t.id
    """)

    # --- Insert metadata ---
    now_utc = datetime.datetime.now(datetime.timezone.utc).isoformat()
    source_version = metadata.get("extractedAt", now_utc)[:10].replace("-", "")

    meta_entries = {
        "schemaVersion": str(SCHEMA_VERSION),
        "version": source_version,
        "generatedAt": now_utc,
        "trainCount": str(len(trains_data)),
        "stationCount": str(len(station_id_map)),
        "stopEventCount": str(total_stops_inserted),
        "liveSupportedLines": "C,W,H",
    }

    for key, value in meta_entries.items():
        cursor.execute("INSERT INTO metadata (key, value) VALUES (?, ?)", (key, value))

    conn.commit()

    # Compute stats
    cursor.execute("SELECT COUNT(*) FROM trains")
    db_train_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM stations")
    db_station_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM stop_events")
    db_stop_count = cursor.fetchone()[0]

    # Integrity check
    cursor.execute("PRAGMA integrity_check")
    integrity = cursor.fetchone()[0]

    conn.close()

    # VACUUM to optimize file size
    conn2 = sqlite3.connect(output_path)
    conn2.execute("VACUUM")
    conn2.close()

    db_size = os.path.getsize(output_path)
    db_size_mb = db_size / (1024 * 1024)

    # Compute SHA-256
    sha256 = hashlib.sha256()
    with open(output_path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            sha256.update(chunk)
    sha256_hex = sha256.hexdigest()

    print(f"\n{'='*50}")
    print(f"Database generated successfully!")
    print(f"{'='*50}")
    print(f"  Output:       {output_path}")
    print(f"  Size:         {db_size_mb:.2f} MB ({db_size:,} bytes)")
    print(f"  Trains:       {db_train_count:,}")
    print(f"  Stations:     {db_station_count:,}")
    print(f"  Stop events:  {db_stop_count:,}")
    print(f"  Integrity:    {integrity}")
    print(f"  SHA-256:      {sha256_hex}")
    print(f"  Schema ver:   {SCHEMA_VERSION}")
    print(f"  Version:      {source_version}")

    return {
        "version": source_version,
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": now_utc,
        "trainCount": db_train_count,
        "stationCount": db_station_count,
        "stopEventCount": db_stop_count,
        "databaseSizeBytes": db_size,
        "sha256": sha256_hex,
    }


def write_metadata_json(meta, output_path):
    """Write metadata.json alongside the DB."""
    meta_path = os.path.join(os.path.dirname(output_path), "metadata.json")
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
    print(f"  Metadata:     {meta_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate Watch SQLite DB from extracted JSON")
    parser.add_argument(
        "--input",
        default=os.path.join(os.path.dirname(__file__), "..", "data", "generated", "mumbai-local-timetable.json"),
        help="Path to extracted JSON timetable",
    )
    parser.add_argument(
        "--output",
        default=os.path.join(os.path.dirname(__file__), "..", "data", "generated", "mumbai-watch.db"),
        help="Output path for SQLite database",
    )
    parser.add_argument("--metadata", action="store_true", help="Also generate metadata.json")
    args = parser.parse_args()

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)

    meta = generate_db(args.input, args.output)

    if args.metadata:
        write_metadata_json(meta, args.output)
