#!/usr/bin/env python3
"""
m-Indicator Mumbai Local Train Timetable Extractor
Reverse-Engineered from com.mobond.mindicator Android APK.

Extracts all suburban train routes, schedules, stops, timings, platforms,
and door sides directly from the APK assets without requiring Android runtime.

v2.0 — Bug fixes applied per INITIAL_ASSESSMENT.md:
  BUG-1: Direction flag 4 now maps to SPECIAL (not collapsed to NEUTRAL)
  BUG-2: "General on" annotations parsed into structured generalOnDays field
  BUG-3: MEMU/EXPRESS/PASSENGER annotation check runs before single-letter typeCode
  BUG-5: Return train mappings now extracted from index section 6
  Plus: g_str, h_str, raw directionFlag, Ladies Spl, Extended to, Will Not Halt
"""

import sys
import os
import struct
import json
import zipfile
import csv
import io
import re
import datetime
import argparse

# Line definitions mapped from m-Indicator directory codes
LINE_DEFINITIONS = {
    "C": {
        "id": "CENTRAL",
        "displayName": "Central Line",
        "corridor": "CSMT - Kalyan - Kasara / Khopoli"
    },
    "W": {
        "id": "WESTERN",
        "displayName": "Western Line",
        "corridor": "Churchgate - Borivali - Virar - Dahanu Road"
    },
    "H": {
        "id": "HARBOUR",
        "displayName": "Harbour Line",
        "corridor": "CSMT - Panvel / Goregaon"
    },
    "T": {
        "id": "TRANS_HARBOUR",
        "displayName": "Trans-Harbour Line",
        "corridor": "Thane - Vashi / Panvel"
    },
    "U": {
        "id": "URAN",
        "displayName": "Uran Line",
        "corridor": "Nerul / Belapur CBD - Uran"
    },
    "DVP": {
        "id": "DIVA_VASAI_PANVEL",
        "displayName": "Diva-Vasai-Panvel Line",
        "corridor": "Diva - Vasai - Panvel - Roha"
    }
}

ALL_DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
DOOR_CODE_MAP = {1: "L", 2: "R", 3: "B", "1": "L", "2": "R", "3": "B", 0: None, "0": None}

# Direction flag constants (from pa.h static fields)
DIR_UP = 1
DIR_DOWN = 2
DIR_NEUTRAL = 3
DIR_SPECIAL = 4

DIRECTION_MAP = {
    DIR_UP: "UP",
    DIR_DOWN: "DOWN",
    DIR_NEUTRAL: "NEUTRAL",
    DIR_SPECIAL: "SPECIAL",
}


def parse_operating_days(annotation_str):
    """Parses natural language running day restrictions into structured day lists.

    Only handles "Not on" and "Only on" patterns that restrict which days
    the train runs. "General on" patterns do NOT affect operating days —
    they affect the train's class/category on those days.
    """
    if not annotation_str:
        return ALL_DAYS[:]

    ann_upper = annotation_str.upper()
    days = set(ALL_DAYS)

    # "Only on" patterns — return immediately with just those days
    if "ONLY ON FRIDAY AND SATURDAY" in ann_upper:
        return ["FRI", "SAT"]
    if "ONLY ON THURSDAY AND FRIDAY" in ann_upper:
        return ["THU", "FRI"]

    # "Not on" patterns — remove days from the full set
    if "NOT ON SATURDAY AND SUNDAY" in ann_upper:
        days.discard("SAT")
        days.discard("SUN")
    else:
        # Handle separate "Not on Saturday" and "Not on Sunday" phrases
        if "NOT ON SATURDAY" in ann_upper:
            days.discard("SAT")
        if "NOT ON SUNDAY" in ann_upper:
            days.discard("SUN")

    if "NOT ON FRIDAY AND SATURDAY" in ann_upper:
        days.discard("FRI")
        days.discard("SAT")
    if "NOT ON THURSDAY AND FRIDAY" in ann_upper:
        days.discard("THU")
        days.discard("FRI")

    return [d for d in ALL_DAYS if d in days]


def parse_general_on_days(annotation_str):
    """Parses 'General on Saturday and Sunday' type annotations.

    Returns a list of days when the train runs as General (non-AC) class,
    or None if no such annotation exists.
    """
    if not annotation_str:
        return None

    ann_upper = annotation_str.upper()

    if "GENERAL ON SATURDAY AND SUNDAY" in ann_upper:
        return ["SAT", "SUN"]
    if "GENERAL ON SUNDAY AND HOLIDAY" in ann_upper:
        return ["SUN"]
    if "GENERAL ON SUNDAY AND HOLIDAYS" in ann_upper:
        return ["SUN"]
    if "GENERAL ON SUNDAY" in ann_upper:
        return ["SUN"]
    if "GENERAL ON SATURDAY" in ann_upper:
        return ["SAT"]

    return None


def parse_station_platform_overrides(annotation_str):
    """Parses station-specific platform overrides from annotations.

    Annotations like '(KYN PF1A)' or '(BO PF2)' indicate that at a specific
    station, the train uses a non-default platform.

    Returns a dict {station_code: platform_string} or empty dict.
    """
    if not annotation_str:
        return {}

    overrides = {}
    # Match patterns like (KYN PF1A), (BO PF1), (BO PF2), (VR PF1)
    pattern = re.compile(r'\((\w+)\s+(PF\w+)\)')
    for match in pattern.finditer(annotation_str):
        station_code = match.group(1)
        platform = match.group(2)
        overrides[station_code] = platform

    return overrides


def parse_annotation_metadata(annotation_str):
    """Extracts structured metadata from annotation strings.

    Returns a dict with optional keys:
      isLadiesSpecial, extendedTo, willNotHaltAt, trainNameFromAnnotation
    """
    result = {}
    if not annotation_str:
        return result

    ann = annotation_str

    # Ladies Special
    if "Ladies Spl" in ann:
        result["isLadiesSpecial"] = True

    # Ladies Coaches info (e.g. "3 Coaches for Ladies from Kalyan end")
    ladies_coaches_match = re.search(r'(\d+\s+Coaches for Ladies from \w+\s+end)', ann)
    if ladies_coaches_match:
        result["ladiesCoaches"] = ladies_coaches_match.group(1).strip()

    # Extended to
    extended_match = re.search(r'Extended to (\w[\w\s]*?)(?:,|$)', ann)
    if extended_match:
        result["extendedTo"] = extended_match.group(1).strip()

    # Will Not Halt at
    halt_match = re.search(r'Will Not Halt at (.+?)(?:,|$)', ann)
    if halt_match:
        result["willNotHaltAt"] = halt_match.group(1).strip()

    # Train name from annotation (Express, MEMU, Shuttle, Passenger, Flying Ranee, etc.)
    if any(k in ann.upper() for k in ["EXP", "MEMU", "PASSENGER", "SHUTTLE", "FLYING RANEE"]):
        # Extract the descriptive name, stripping day/platform suffixes
        name = ann
        # Remove common suffixes
        name = re.sub(r',\s*(Not on|Only on|General on).*', '', name)
        name = re.sub(r',\s*\(\w+ PF\w+\)', '', name)
        name = name.strip().rstrip(',').strip()
        if name:
            result["trainNameFromAnnotation"] = name

    return result


def normalize_train_type(m_str, m2_str):
    """Classifies train type (Fast, Slow, Semi-Fast, AC, etc.).

    BUG-3 FIX: Check annotation-based types (MEMU, EXPRESS, PASSENGER)
    BEFORE single-letter typeCode to avoid "S" masking MEMU.
    """
    m = (m_str or "").strip().upper()
    m2 = (m2_str or "").strip().upper()

    is_ac = bool(re.search(r'\bAC\b', m2) or re.search(r'\bAC\b', m))

    # Check annotation-based types FIRST (BUG-3 fix)
    if "MEMU" in m2:
        return "MEMU", is_ac
    if "EXP" in m2 or "EXPRESS" in m2:
        return "EXPRESS", is_ac
    if "PASSENGER" in m2 or "PSG" in m2:
        return "PASSENGER", is_ac
    if "SHUTTLE" in m2:
        return "SHUTTLE", is_ac

    # Then check typeCode prefix
    if m.startswith("SF"):
        return "SEMI_FAST", is_ac
    if m.startswith("F"):
        return "FAST", is_ac
    if m.startswith("S"):
        return "SLOW", is_ac

    # Default: if typeCode is empty, classify as SLOW
    return "SLOW", is_ac


class TimetableDataSource:
    """Abstracts reading assets from APK zip or a folder."""
    def __init__(self, apk_path=None, folder_path=None):
        self.apk = None
        self.folder = None
        if apk_path and os.path.exists(apk_path):
            self.apk = zipfile.ZipFile(apk_path, "r")
        elif folder_path and os.path.exists(folder_path):
            self.folder = folder_path
        else:
            raise ValueError("Must provide a valid APK path or extracted folder path")

    def read_bytes(self, relative_path):
        if self.apk:
            entry_name = relative_path.replace("\\", "/")
            if entry_name.startswith("/"):
                entry_name = entry_name[1:]
            if not entry_name.startswith("assets/"):
                entry_name = "assets/" + entry_name
            try:
                return self.apk.read(entry_name)
            except KeyError:
                return None
        else:
            candidates = [
                os.path.join(self.folder, relative_path),
                os.path.join(self.folder, "assets", relative_path),
                os.path.join(self.folder, relative_path.replace("mumbai/local/", "")),
                os.path.join(self.folder, "local", relative_path.replace("mumbai/local/", "")),
            ]
            for p in candidates:
                if os.path.exists(p) and os.path.isfile(p):
                    with open(p, "rb") as f:
                        return f.read()
            return None


def extract_station_database(data_source):
    """Loads and indexes stationlist.csv metadata."""
    raw_csv = data_source.read_bytes("mumbai/local/stationlist.csv")
    if not raw_csv:
        bundled = os.path.join(os.path.dirname(__file__), "..", "data", "stationlist.csv")
        if os.path.exists(bundled):
            with open(bundled, "rb") as f:
                raw_csv = f.read()
    if not raw_csv:
        return {}

    stations = {}
    csv_text = raw_csv.decode("utf-8", errors="ignore")
    reader = csv.DictReader(io.StringIO(csv_text))
    for row in reader:
        st_name = row["station"].strip()
        st_upper = st_name.upper()
        stations[st_upper] = {
            "name": st_name,
            "marathiName": row.get("station_marathi", "").strip() or None,
            "info": row.get("station_info", "").strip() or None,
            "latitude": float(row["lat"]) if row.get("lat") else None,
            "longitude": float(row["lon"]) if row.get("lon") else None,
            "lines": [l.strip() for l in row.get("lines", "").split(",") if l.strip()]
        }
    return stations


def extract_line_timetable(line_code, data_source, station_db):
    """Extracts all trains and timings for a specific suburban line."""
    idx_bytes = data_source.read_bytes(f"mumbai/local/{line_code}/index")
    if not idx_bytes:
        print(f"Warning: Index file not found for line {line_code}", file=sys.stderr)
        return [], {}

    # 1. Station names list
    station_len = struct.unpack(">I", idx_bytes[0:4])[0]
    station_str = idx_bytes[4:4+station_len].decode("ascii", errors="ignore")
    stations = [s.strip() for s in station_str.split(",")]

    # 2. g_str (primary origin) and h_str (terminal destinations)
    offset = 4 + station_len
    g_len = idx_bytes[offset]
    g_str = idx_bytes[offset+1:offset+1+g_len].decode("ascii", errors="ignore")
    offset += 1 + g_len
    h_len = idx_bytes[offset]
    h_str = idx_bytes[offset+1:offset+1+h_len].decode("ascii", errors="ignore")
    offset += 1 + h_len

    # 3. Train definitions section
    t_len = struct.unpack(">I", idx_bytes[offset:offset+4])[0]
    offset += 4
    end_train_sec = offset + t_len

    train_defs = []
    while offset < end_train_sec:
        src_idx = struct.unpack(">H", idx_bytes[offset:offset+2])[0]
        dst_idx = struct.unpack(">H", idx_bytes[offset+2:offset+4])[0]
        tflag = idx_bytes[offset+4]
        mlen = idx_bytes[offset+5]
        offset += 6
        mstr = idx_bytes[offset:offset+mlen].decode("ascii", errors="ignore")
        offset += mlen
        m2len = idx_bytes[offset]
        offset += 1
        m2str = idx_bytes[offset:offset+m2len].decode("ascii", errors="ignore")
        offset += m2len

        src_name = stations[src_idx] if src_idx < len(stations) else f"UNKNOWN_{src_idx}"
        dst_name = stations[dst_idx] if dst_idx < len(stations) else f"UNKNOWN_{dst_idx}"

        # BUG-1 FIX: Map direction flag 4 to SPECIAL
        dir_str = DIRECTION_MAP.get(tflag, f"UNKNOWN_{tflag}")
        category, is_ac = normalize_train_type(mstr, m2str)
        operating_days = parse_operating_days(m2str)
        general_on_days = parse_general_on_days(m2str)
        station_pf_overrides = parse_station_platform_overrides(m2str)
        annotation_meta = parse_annotation_metadata(m2str)

        train_defs.append({
            "source": src_name,
            "destination": dst_name,
            "directionFlag": tflag,
            "direction": dir_str,
            "trainType": category,
            "isAC": is_ac,
            "is15Car": "15 CAR" in (m2str or "").upper(),
            "typeCode": mstr,
            "annotation": m2str if m2str else None,
            "operatingDays": operating_days,
            "generalOnDays": general_on_days,
            "stationPlatformOverrides": station_pf_overrides if station_pf_overrides else None,
            "isLadiesSpecial": annotation_meta.get("isLadiesSpecial", False),
            "ladiesCoaches": annotation_meta.get("ladiesCoaches"),
            "extendedTo": annotation_meta.get("extendedTo"),
            "willNotHaltAt": annotation_meta.get("willNotHaltAt"),
            "trainNameFromAnnotation": annotation_meta.get("trainNameFromAnnotation"),
        })

    # 4. Platform door custom lookup table (strM3)
    h3_len = struct.unpack(">I", idx_bytes[offset:offset+4])[0]
    offset += 4
    strM3 = idx_bytes[offset:offset+h3_len].decode("ascii", errors="ignore")
    offset += h3_len

    custom_pf_door = {}
    m3_parts = strM3.split(",")
    for i in range(0, len(m3_parts) - 2, 3):
        key = m3_parts[i].strip()
        if key:
            custom_pf_door[key] = (m3_parts[i+1].strip(), m3_parts[i+2].strip())

    # 5. Train numbers mapping
    h4_len = struct.unpack(">I", idx_bytes[offset:offset+4])[0]
    offset += 4
    train_nums_str = idx_bytes[offset:offset+h4_len].decode("ascii", errors="ignore")
    offset += h4_len

    train_numbers = {}
    for line_str in train_nums_str.strip().split("\n"):
        parts = line_str.split(",")
        if len(parts) >= 2 and parts[0].strip():
            try:
                train_numbers[int(parts[0].strip())] = parts[1].strip()
            except ValueError:
                pass

    # 6. Return train mappings (BUG-5 FIX: now extracted)
    return_train_map = {}
    remaining = len(idx_bytes) - offset
    if remaining >= 4:
        m5_len = struct.unpack(">I", idx_bytes[offset:offset+4])[0]
        offset += 4
        n_records = m5_len // 3
        for i in range(n_records):
            b0 = idx_bytes[offset + i*3]
            b1 = idx_bytes[offset + i*3 + 1]
            b2 = idx_bytes[offset + i*3 + 2]
            val24 = (b0 << 16) | (b1 << 8) | b2
            train_id = (val24 >> 12) & 0xFFF
            return_id = val24 & 0xFFF
            return_train_map[train_id] = return_id

    # 7. Read every station binary file and index timetable events
    total_trains = len(train_defs)
    train_stops = {tid: [] for tid in range(1, total_trains + 1)}

    for st in stations:
        st_bytes = data_source.read_bytes(f"mumbai/local/{line_code}/{st}")
        if not st_bytes:
            continue

        for i in range(0, len(st_bytes), 4):
            rec = st_bytes[i:i+4]
            tid = (rec[2] & 0xFF) + ((rec[1] & 0x0F) << 8)
            if tid in train_stops:
                mins = ((rec[0] & 0xFF) << 4) + ((rec[1] & 0xF0) >> 4)
                b3 = rec[3]
                pf_num = (b3 >> 3) & 31
                pf = f"PF{pf_num}" if pf_num > 0 else None
                door_code = b3 & 3
                door = DOOR_CODE_MAP.get(door_code)

                # Check custom lookup if bit 2 is set
                if (b3 & 4) == 4:
                    hex_key = f"{rec[0]:02X}{rec[1]:02X}{rec[2]:02X}"
                    if hex_key in custom_pf_door:
                        c_pf, c_door = custom_pf_door[hex_key]
                        if c_pf:
                            pf = f"PF{c_pf}"
                        if c_door in DOOR_CODE_MAP:
                            door = DOOR_CODE_MAP[c_door]

                train_stops[tid].append({
                    "stationName": st,
                    "minutesFromMidnight": mins,
                    "platform": pf,
                    "doorSide": door
                })

    # Assemble structured train objects
    line_info = LINE_DEFINITIONS.get(line_code, {"id": line_code, "displayName": line_code})
    result_trains = []

    line_metadata = {
        "lineCode": line_code,
        "lineId": line_info["id"],
        "displayName": line_info["displayName"],
        "corridor": line_info.get("corridor", ""),
        "origin": g_str,
        "terminus": h_str,
        "stationCount": len(stations),
        "stations": stations,
    }

    for tid in range(1, total_trains + 1):
        meta = train_defs[tid - 1]
        raw_stops = train_stops[tid]
        if not raw_stops:
            continue

        # Sort stops chronologically and handle midnight rollover (m-Indicator H() algorithm)
        raw_stops.sort(key=lambda s: s["minutesFromMidnight"])
        rollover_idx = -1
        for k in range(len(raw_stops) - 1):
            if raw_stops[k + 1]["minutesFromMidnight"] - raw_stops[k]["minutesFromMidnight"] > 500:
                rollover_idx = k
                break
        if rollover_idx >= 0:
            raw_stops = raw_stops[rollover_idx + 1:] + raw_stops[:rollover_idx + 1]

        # Apply station-specific platform overrides from annotations
        pf_overrides = meta.get("stationPlatformOverrides") or {}

        # Build station schedule list
        num_stops = len(raw_stops)
        formatted_stations = []
        for idx, stop in enumerate(raw_stops):
            mins = stop["minutesFromMidnight"]
            hh = mins // 60
            mm = mins % 60
            time_str = f"{hh:02d}:{mm:02d}"

            st_key = stop["stationName"].upper()
            st_meta = station_db.get(st_key, {})

            arr_time = None if idx == 0 else time_str
            dep_time = None if idx == num_stops - 1 else time_str

            # Check for annotation-based platform override
            platform = stop["platform"]
            for override_code, override_pf in pf_overrides.items():
                if override_code.upper() in st_key or st_key.startswith(override_code.upper()):
                    platform = override_pf

            formatted_stations.append({
                "stationCode": stop["stationName"],
                "stationName": st_meta.get("name", stop["stationName"]),
                "stationMarathi": st_meta.get("marathiName"),
                "arrival": arr_time,
                "departure": dep_time,
                "minutesFromMidnight": mins,
                "platform": platform,
                "doorSide": stop["doorSide"],
                "latitude": st_meta.get("latitude"),
                "longitude": st_meta.get("longitude")
            })

        train_number = train_numbers.get(tid)
        train_name = meta.get("trainNameFromAnnotation")

        # Resolve return train number
        return_train_id = return_train_map.get(tid)
        return_train_number = None
        if return_train_id:
            return_train_number = train_numbers.get(return_train_id)

        result_trains.append({
            "trainId": tid,
            "trainNumber": train_number,
            "trainName": train_name,
            "line": line_info["id"],
            "lineCode": line_code,
            "lineDisplayName": line_info["displayName"],
            "direction": meta["direction"],
            "directionFlag": meta["directionFlag"],
            "source": meta["source"],
            "destination": meta["destination"],
            "trainType": meta["trainType"],
            "typeCode": meta["typeCode"],
            "isAC": meta["isAC"],
            "is15Car": meta["is15Car"],
            "isLadiesSpecial": meta["isLadiesSpecial"],
            "ladiesCoaches": meta.get("ladiesCoaches"),
            "operatingDays": meta["operatingDays"],
            "generalOnDays": meta["generalOnDays"],
            "extendedTo": meta.get("extendedTo"),
            "willNotHaltAt": meta.get("willNotHaltAt"),
            "rawAnnotation": meta["annotation"],
            "returnTrainNumber": return_train_number,
            "returnTrainId": return_train_id,
            "stopsCount": len(formatted_stations),
            "departureTime": formatted_stations[0]["departure"],
            "arrivalTime": formatted_stations[-1]["arrival"],
            "stations": formatted_stations
        })

    return result_trains, line_metadata


def extract_all(apk_path=None, folder_path=None, output_path="mumbai-local-timetable.json", include_pune=False):
    """Main extraction routine."""
    print(f"Opening data source: {apk_path or folder_path}...")
    data_source = TimetableDataSource(apk_path=apk_path, folder_path=folder_path)

    print("Loading station database...")
    station_db = extract_station_database(data_source)
    print(f"Loaded {len(station_db)} stations.")

    lines_to_process = list(LINE_DEFINITIONS.keys())
    if include_pune:
        lines_to_process.append("P")
        LINE_DEFINITIONS["P"] = {
            "id": "PUNE_SUBURBAN",
            "displayName": "Pune Suburban Local",
            "corridor": "Pune - Lonavla"
        }

    all_trains = []
    lines_metadata = {}
    line_summaries = {}

    for line_code in lines_to_process:
        line_name = LINE_DEFINITIONS[line_code]["displayName"]
        print(f"Extracting {line_name} ({line_code})...")
        trains, line_meta = extract_line_timetable(line_code, data_source, station_db)
        all_trains.extend(trains)
        lines_metadata[LINE_DEFINITIONS[line_code]["id"]] = line_meta
        line_summaries[LINE_DEFINITIONS[line_code]["id"]] = {
            "displayName": line_name,
            "corridor": LINE_DEFINITIONS[line_code]["corridor"],
            "trainCount": len(trains),
            "origin": line_meta.get("origin", ""),
            "terminus": line_meta.get("terminus", ""),
            "stationCount": line_meta.get("stationCount", 0),
        }
        print(f"  Extracted {len(trains)} trains.")

    # Compute total stop events
    total_stops = sum(t["stopsCount"] for t in all_trains)

    metadata = {
        "datasetName": "Mumbai Suburban Local Train Complete Timetable",
        "source": "m-Indicator Android APK (com.mobond.mindicator)",
        "extractorVersion": "2.0",
        "extractedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "totalTrains": len(all_trains),
        "totalStopEvents": total_stops,
        "lines": list(line_summaries.keys()),
        "linesMetadata": line_summaries
    }

    full_output = {
        "metadata": metadata,
        "lines": lines_metadata,
        "trains": all_trains
    }

    print(f"Writing JSON dataset to {output_path}...")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(full_output, f, indent=2, ensure_ascii=False)

    file_size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"Extraction successful! Extracted {len(all_trains)} trains ({total_stops} stop events) to {output_path} ({file_size_mb:.2f} MB).")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract Mumbai local train timetables from m-Indicator APK")
    parser.add_argument("--apk", default=None, help="Path to base.apk")
    parser.add_argument("--assets", default=None, help="Path to extracted assets folder if unzipped")
    parser.add_argument("--output", default=None, help="Path for output JSON")
    parser.add_argument("--include-pune", action="store_true", help="Include Pune suburban line (P)")
    args = parser.parse_args()

    # Default paths relative to project structure
    default_apk = os.path.join(os.path.dirname(__file__), "..", "..", "mindicator-apk", "base.apk")
    default_output = os.path.join(os.path.dirname(__file__), "..", "data", "generated", "mumbai-local-timetable.json")

    output = args.output or default_output
    if args.assets:
        apk = None
        assets = args.assets
    else:
        apk = args.apk or default_apk
        assets = None

    # Ensure output directory exists
    os.makedirs(os.path.dirname(os.path.abspath(output)), exist_ok=True)

    extract_all(apk_path=apk, folder_path=assets, output_path=output, include_pune=args.include_pune)
