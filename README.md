# Mumbai Local for Wear OS (MumbaiLocalWO) 🚆⌚

[![Wear OS](https://img.shields.io/badge/Wear%20OS-API%2030%2B-blue.svg)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Material 3](https://img.shields.io/badge/Material%203-Wear%20Compose-green.svg)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/github/v/release/HAJ1MEI/mumbai-local-wear-os?include_prereleases&color=orange)](https://github.com/HAJ1MEI/mumbai-local-wear-os/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-brightgreen.svg)](LICENSE)

An offline-first, standalone Wear OS application for Mumbai Suburban Railway commuters. Designed specifically for circular smartwatches with Jetpack Compose for Wear OS and Material 3, delivering sub-second timetable lookups, real-time crowdsourced live tracking, and over-the-air timetable updates.

---

## 📸 Overview & Design

Built for quick wrist glanceability and thumb ergonomics on Wear OS:
- **Pure AMOLED Black (`#000000`)**: Maximizes battery life on OLED watch displays.
- **Curved Bezel Geometry**: Padding and constraints prevent edge clipping on circular screens.
- **Micro-Animations & Visual Hierarchy**: Color-coded delay badges, live position banners, and glowing train indicators.

---

## ✨ Features

### 1. 📴 100% Offline-First Timetable Engine
- **Full Suburban Network Coverage**: Pre-packaged SQLite timetable covering **Central (Main)**, **Western**, **Harbour**, and **Trans-Harbour** lines.
- **Extensive Database**: Over **3,100+ trains**, **140+ stations**, and **53,000+ stop schedules**.
- **Instant Search**: Sub-second queries executed completely locally on device—no internet connection required to look up departure times, platforms, or schedules.
- **Service Day Awareness**: Accurate day-of-week schedule filters (Weekdays, Sundays, Saturday specials).

### 2. 🔍 Point-to-Point Search (Direct & Connecting Routes)
- **Station Search**: Fast station selector with recent searches and line categorization.
- **Direct Trains**: Displays all direct services between Origin and Destination with departure countdowns.
- **Connecting & Interchange Engine**: When no direct trains run between lines (e.g. Central ↔ Western), automatically detects optimal interchange junctions (such as Dadar or Kurla) and displays two-leg journey cards (`Leg 1: To Dadar`, `Leg 2: To Destination`).

### 3. 🚉 Board Train by Station & Direction
- Pick your boarding station, select the line, and choose your direction (e.g., *Towards Kalyan / Karjat / Kasara* or *Towards CSMT*).
- Real-time departure feed showing scheduled departures, train speed (Fast/Slow), destination, and live delays.

### 4. 📡 Real-Time Crowdsourced Live Tracking
- Seamlessly queries the live crowdsourced tracking network.
- **Live Status Badges**:
  - `🟢 On Time`
  - `🟡 X min late`
  - `🔴 Cancelled` / `⚪ Not Running Today`
- **Current Train Location**: Highlights exact train position on the summary card:
  - `At Station` (e.g. `🟡 1 min late · At Vithalwadi`)
  - `Between Stations` (e.g. `🟡 3 min late · Bet. Thane & Diva`)
- **Smart 60s TTL Cache**: Prevents duplicate network calls and conserves watch battery and cellular data.

### 5. 🗺️ Station Timeline Visualizer
- Visual vertical timeline for every train showing all intermediate stops and scheduled times.
- **Interactive Live Marker**:
  - **Train at Station**: Highlights the station card with an emerald green border (`#00E676`), soft glow, and a `🚆` train icon.
  - **Train In Transit**: Displays a dedicated `🚆 [Between Station A & Station B]` card inserted right between the two stations on the route.
- **Auto-Scroll to Train**: Automatically scrolls and centers on the train's active location when opening the detail view.

### 6. 🔄 Over-The-Air (OTA) Timetable Updates (Phase 5)
- **Remote Version Check**: Checks GitHub-hosted timetable metadata against the locally installed database.
- **Background Streaming Download**: Downloads updated timetable SQLite database (`.db`) directly on the watch with progress percentage and transferred MB.
- **Integrity & Security Validation**:
  - Computes and verifies **SHA-256** checksum.
  - Executes SQLite `PRAGMA integrity_check`.
  - Verifies presence of all core tables and validates minimum record thresholds before touching active data.
- **Zero-Downtime Atomic Swap**: Swaps the new database in place atomically. If an update fails or is interrupted, the existing database remains untouched.
- **Live Lifecycle Refresh**: Returning to Settings or the Home screen instantly updates the active timetable date.

---

## 🛠️ Technology Stack & Architecture

| Layer | Technologies |
|---|---|
| **Platform** | Wear OS 3.0+ (Target API 34, Min API 30) |
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose for Wear OS 1.4+, Wear Material 3 1.0+, Wear Foundation |
| **Concurrency** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Database** | SQLite via custom thread-safe Room/Android SQLite wrapper with atomic file swap support |
| **Networking** | OkHttp 4 with transparent GZIP / chunked decompression, timeout gates |
| **Battery Optimization** | Wear OS `NetworkGate` handles radio wake lock & Bluetooth/Wi-Fi proxy transitions |

### Directory Structure

```
app/src/main/java/com/example/mumbailocalwo/
├── data/
│   ├── db/                 # TimetableDatabase, SQLite schema & atomic swap logic
│   ├── model/              # Station, Train, SearchResult data models
│   └── repository/         # TimetableRepository (caching, queries, interchange routing)
├── live/
│   ├── model/              # LiveStatus, DelayStatus models
│   ├── LiveApiClient.kt    # HTTP client for live tracking
│   ├── LiveCache.kt        # In-memory TTL cache
│   └── LiveStatusParser.kt # Parser for live tracking responses
├── net/
│   └── NetworkGate.kt      # Wear OS radio & network acquisition helper
├── presentation/
│   ├── board/              # Boarding station, direction & train list screens
│   ├── components/         # Wear OS cards, station badges, delay pills, message states
│   ├── home/               # Circular Home screen with full-width action tiles
│   ├── search/             # Station picker & search results screens
│   ├── settings/           # Settings, auto-live toggle, timetable version
│   ├── traindetail/        # Train detail & station timeline visualizer
│   └── update/             # UpdateStatusScreen with download progress
└── update/
    ├── model/              # UpdateMetadata data class
    ├── DatabaseDownloader  # Chunked file streaming with progress
    ├── DatabaseValidator   # SHA256 & SQLite schema integrity checker
    ├── MetadataClient      # Remote metadata JSON fetcher
    └── UpdateManager       # Update workflow & state machine
```

---

## 🤖 Automated Upstream Timetable Pipeline (Zero-Cost CI/CD)

The timetable data pipeline runs **fully autonomously** via GitHub Actions with zero server maintenance:

```text
GitHub Actions Cron (Every 6 Hours)
       ↓
Fetch Upstream Mobond OTA package (https://cdn.mobond.com/mi/mumbaidb.zip)
       ↓
Inspect version.txt vs published timetable/metadata.json
       ↓
If unchanged ──→ Stop (0 compute waste)
       ↓
If newer:
  1. Extract suburban rail binary assets (local/)
  2. Run extract_mumbai_local_timetable.py
  3. Run generate_watch_db.py (builds mumbai-watch.db)
  4. Run validate_watch_db.py (checks PRAGMA integrity, foreign keys, row minimums)
  5. Calculate SHA-256 and update timetable/metadata.json
  6. Automatically commit and push to main branch
```

### Wear OS Client Behavior
- Update checking on the watch is **strictly on-demand / manual** via **Settings → Check for updates**.
- **No battery-draining background polling**: The watch avoids running unnecessary background workers or wake locks, preserving smartwatch battery life while ensuring commuters can grab updates whenever desired.

---

## 📲 Installation

### Option 1: Download Pre-built APK (Recommended)

1. Download the latest `app-debug.apk` from [GitHub Releases](https://github.com/HAJ1MEI/mumbai-local-wear-os/releases).
2. Install to your smartwatch using **Wireless ADB** or **Bugjaeger**.

---

### Option 2: Install via Wireless ADB (Samsung Galaxy Watch & Pixel Watch)

#### Step 1: Enable Developer Options & Wireless Debugging
1. On your smartwatch, go to **Settings** → **About watch** → **Software info**.
2. Tap **Software version** 7 times until you see the message *"Developer mode turned on"*.
3. Go back to **Settings** → **Developer options**.
4. Enable **ADB debugging**.
5. Scroll down and enable **Wireless debugging**.
6. Ensure the watch is connected to the same Wi-Fi network as your computer.

#### Step 2: Pair Device (First Time Only)
1. In **Wireless debugging**, tap **Pair new device**.
2. Note the **IP address & Port** (e.g. `192.168.0.144:38409`) and the **6-digit Wi-Fi pairing code** displayed on your watch.
3. Open a terminal on your computer and run:
   ```bash
   adb pair <WATCH_IP>:<PAIRING_PORT> <PAIRING_CODE>
   # Example:
   # adb pair 192.168.0.144:38409 190392
   ```
4. You should see: `Successfully paired to 192.168.0.144:38409`.

#### Step 3: Connect and Install
1. Look at the main **Wireless debugging** screen for the connection port under **IP address & Port** (note: this port is usually different from the pairing port).
2. Connect to the watch:
   ```bash
   adb connect <WATCH_IP>:<CONNECTION_PORT>
   # Example:
   # adb connect 192.168.0.144:46137
   ```
3. Verify connection:
   ```bash
   adb devices
   # Should list: 192.168.0.144:46137 device
   ```
4. Install the application:
   ```bash
   adb -s <WATCH_IP>:<CONNECTION_PORT> install -r app-debug.apk
   ```

---

### Option 3: Install via Bugjaeger (From Android Phone)
If you don't have a PC nearby, you can install the APK directly from your Android phone:
1. Install **Bugjaeger** from the Google Play Store on your Android phone.
2. Enable Wireless Debugging on your watch.
3. Pair and connect Bugjaeger to your watch over Wi-Fi.
4. In the **Packages** tab, tap **+ (Install)**, select the downloaded `app-debug.apk`, and install.

---

## 🔨 Building from Source

### Prerequisites
- **Android Studio** (Ladybug / Koala or newer recommended)
- **JDK 17 or JDK 21**
- **Android SDK API 34** with Wear OS system images

### Clone and Compile

```bash
# Clone the repository
git clone https://github.com/HAJ1MEI/mumbai-local-wear-os.git
cd mumbai-local-wear-os

# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install directly to connected device / emulator:

```bash
./gradlew installDebug
```

---

## 🚂 Lines & Networks Supported

- **Central Railway (CR)**: CSMT ↔ Dadar ↔ Kurla ↔ Thane ↔ Kalyan ↔ Karjat / Kasara / Khopoli
- **Western Railway (WR)**: Churchgate ↔ Dadar ↔ Bandra ↔ Andheri ↔ Borivali ↔ Virar ↔ Dahanu Road
- **Harbour Line (HR)**: CSMT ↔ Vadala Road ↔ Kurla ↔ Vashi ↔ Panvel / Goregaon
- **Trans-Harbour Line**: Thane ↔ Vashi / Nerul ↔ Panvel

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
