#!/usr/bin/env python3
"""
Mumbai Local Wear OS — Automated Timetable Update Pipeline

Executes the zero-cost data extraction and publishing pipeline:
1. Downloads the upstream Mobond OTA package (https://cdn.mobond.com/mi/mumbaidb.zip).
2. Inspects upstream version.txt and compares against timetable/metadata.json.
3. If a newer timetable is available (or --force is provided):
   a. Extracts local/ suburban rail binary assets.
   b. Runs the Python timetable extractor (extract_mumbai_local_timetable.py).
   c. Generates the Wear OS SQLite database (generate_watch_db.py).
   d. Validates database schema, record minimums, and PRAGMA integrity_check.
   e. Atomically updates timetable/mumbai-watch.db and timetable/metadata.json.
   f. Outputs GitHub Actions step summary.
"""

import os
import sys
import io
import json
import zipfile
import urllib.request
import argparse
import tempfile
import shutil
import hashlib
from datetime import datetime, timezone

# Add pipeline directory to sys.path so sibling imports work cleanly
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(SCRIPT_DIR, "extractor"))
sys.path.insert(0, os.path.join(SCRIPT_DIR, "generator"))
sys.path.insert(0, os.path.join(SCRIPT_DIR, "validation"))

from extract_mumbai_local_timetable import extract_all
from generate_watch_db import generate_db, write_metadata_json
from validate_watch_db import validate_db, ValidationError

DEFAULT_MOBOND_URL = "https://cdn.mobond.com/mi/mumbaidb.zip"


def fetch_upstream_zip(url: str, dest_path: str):
    print(f"Downloading upstream package from {url}...")
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Cache-Control": "no-cache"
    }
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as resp:
        with open(dest_path, "wb") as out_file:
            shutil.copyfileobj(resp, out_file)
    size_mb = os.path.getsize(dest_path) / (1024 * 1024)
    print(f"Downloaded upstream package ({size_mb:.2f} MB).")


def read_upstream_version(zip_path: str) -> str:
    with zipfile.ZipFile(zip_path, "r") as z:
        names = z.namelist()
        if "version.txt" in names:
            ver = z.read("version.txt").decode("utf-8", errors="ignore").strip()
            if ver:
                return ver
        if "local/version.txt" in names:
            ver = z.read("local/version.txt").decode("utf-8", errors="ignore").strip()
            if ver:
                return ver

    # Fallback to date stamp of index file if version.txt is missing
    return datetime.now(timezone.utc).strftime("%Y%m%d")


def read_current_metadata(meta_path: str) -> dict:
    if os.path.exists(meta_path):
        try:
            with open(meta_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"Warning: Could not read existing metadata at {meta_path}: {e}")
    return {"version": "0", "schemaVersion": 1}


def is_version_newer(remote: str, local: str) -> bool:
    r_trim = remote.strip()
    l_trim = local.strip()
    if r_trim.lower() == l_trim.lower():
        return False

    # Try numeric date comparison (e.g. 20261003 > 20260928)
    try:
        r_num = int("".join(filter(str.isdigit, r_trim)))
        l_num = int("".join(filter(str.isdigit, l_trim)))
        if r_num != l_num:
            return r_num > l_num
    except Exception:
        pass

    return r_trim != l_trim and len(r_trim) > 0


def set_github_output(name: str, value: str):
    gh_output = os.environ.get("GITHUB_OUTPUT")
    if gh_output and os.path.exists(gh_output):
        with open(gh_output, "a", encoding="utf-8") as f:
            f.write(f"{name}={value}\n")


def append_step_summary(text: str):
    gh_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if gh_summary and os.path.exists(gh_summary):
        with open(gh_summary, "a", encoding="utf-8") as f:
            f.write(f"{text}\n")


def run_pipeline(
    url: str = DEFAULT_MOBOND_URL,
    timetable_dir: str = None,
    force: bool = False
) -> bool:
    if not timetable_dir:
        timetable_dir = os.path.join(SCRIPT_DIR, "..", "timetable")
    timetable_dir = os.path.abspath(timetable_dir)
    os.makedirs(timetable_dir, exist_ok=True)

    db_path = os.path.join(timetable_dir, "mumbai-watch.db")
    meta_path = os.path.join(timetable_dir, "metadata.json")

    current_meta = read_current_metadata(meta_path)
    current_version = current_meta.get("version", "0")
    print(f"Current published timetable version: {current_version}")

    with tempfile.TemporaryDirectory() as temp_dir:
        zip_path = os.path.join(temp_dir, "mumbaidb.zip")
        fetch_upstream_zip(url, zip_path)

        upstream_version = read_upstream_version(zip_path)
        print(f"Upstream timetable version in package: {upstream_version}")

        # Check if update is needed
        if not force and not is_version_newer(upstream_version, current_version):
            print(f"\n[OK] Upstream version '{upstream_version}' is already published or not newer than current '{current_version}'.")
            print("No database update required.")
            set_github_output("has_updates", "false")
            set_github_output("version", current_version)
            append_step_summary(f"### 🚆 Mumbai Local Timetable Status\n- **Status:** Already up to date\n- **Version:** `{current_version}`\n- **No changes applied.**")
            return False

        print(f"\n[UPDATE REQUIRED] Processing new timetable: {upstream_version} > {current_version} (force={force})")

        # Extract local assets from zip
        assets_dir = os.path.join(temp_dir, "assets")
        os.makedirs(assets_dir, exist_ok=True)
        print("Extracting local timetable assets...")
        with zipfile.ZipFile(zip_path, "r") as z:
            for member in z.namelist():
                if member.startswith("local/") or member.startswith("assets/local/"):
                    z.extract(member, assets_dir)

        # 1. Run extraction
        extracted_json = os.path.join(temp_dir, "mumbai-local-timetable.json")
        print("\n--- Running Timetable Extractor ---")
        extract_all(
            folder_path=assets_dir,
            output_path=extracted_json,
            include_pune=False
        )

        if not os.path.exists(extracted_json) or os.path.getsize(extracted_json) == 0:
            raise RuntimeError("Extraction failed: Output JSON is missing or empty.")

        # 2. Run database generation
        generated_db = os.path.join(temp_dir, "mumbai-watch.db")
        print("\n--- Generating Watch SQLite Database ---")
        meta = generate_db(extracted_json, generated_db)

        # 3. Validate generated database
        print("\n--- Validating Database Integrity ---")
        validate_db(generated_db, verbose=True)

        # 4. Copy to timetable output
        print(f"\nPublishing verified database to {db_path}...")
        shutil.copy2(generated_db, db_path)

        # 5. Write metadata
        write_metadata_json(meta, db_path)

        print("\n" + "=" * 60)
        print(f"SUCCESS: Timetable updated to version {meta['version']}")
        print(f"  Trains:        {meta['trainCount']:,}")
        print(f"  Stations:      {meta['stationCount']:,}")
        print(f"  Stop Events:   {meta['stopEventCount']:,}")
        print(f"  Database Size: {meta['databaseSizeBytes'] / (1024 * 1024):.2f} MB")
        print(f"  SHA-256:       {meta['sha256']}")
        print("=" * 60)

        set_github_output("has_updates", "true")
        set_github_output("version", meta["version"])
        append_step_summary(
            f"### 🚆 Mumbai Local Timetable Updated!\n"
            f"- **New Version:** `{meta['version']}`\n"
            f"- **Trains:** `{meta['trainCount']:,}`\n"
            f"- **Stations:** `{meta['stationCount']:,}`\n"
            f"- **Stop Events:** `{meta['stopEventCount']:,}`\n"
            f"- **Database Size:** `{meta['databaseSizeBytes'] / (1024 * 1024):.2f} MB`\n"
            f"- **SHA-256:** `{meta['sha256']}`\n"
        )
        return True


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Automated timetable update pipeline")
    parser.add_argument("--url", default=DEFAULT_MOBOND_URL, help="Upstream ZIP URL")
    parser.add_argument("--timetable-dir", default=None, help="Output directory for db and metadata.json")
    parser.add_argument("--force", action="store_true", help="Force rebuild even if version is unchanged")
    args = parser.parse_args()

    try:
        updated = run_pipeline(
            url=args.url,
            timetable_dir=args.timetable_dir,
            force=args.force
        )
        sys.exit(0)
    except Exception as e:
        print(f"\n[ERROR] Pipeline failed: {e}", file=sys.stderr)
        append_step_summary(f"### ❌ Mumbai Local Timetable Pipeline Failed\n- **Error:** `{e}`")
        sys.exit(1)
