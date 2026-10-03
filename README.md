# Mumbai Local Wear OS (MumbaiLocalWO) 🚆⌚

An offline-first, standalone Wear OS application for Mumbai Suburban Railway commuters, built with Jetpack Compose for Wear OS and Material 3.

## ✨ Features

- **Offline-First Timetable Engine**:
  - Embedded SQLite database covering Central (Main), Western, Harbour, and Trans-Harbour lines.
  - Sub-second local timetable queries without needing an active data connection.
- **Dynamic Live Tracking**:
  - Real-time train tracking integration via Mobond m-Indicator live endpoints.
  - Automatically displays delay and train location (e.g., `🟡 1 min late · At Vithalwadi` or `🟡 5 min late · Bet. Thane & Diva`).
- **Live Station Timeline Visualizer**:
  - **Train at Station**: Highlights the active station in emerald green (`#00E676`) with glowing card background and a `🚆` train marker.
  - **Train Between Stations**: Displays an in-transit indicator `🚆 [Between Station A & Station B]` along the route timeline.
  - Automatic focus scrolling to the train's current position.
- **Connecting & Interchange Routes**:
  - Smart multi-line routing between lines with no direct trains (e.g., Central ↔ Western).
  - Automatically identifies interchange hubs (preferring Dadar / Kurla) with two-leg selector tabs (`1: To Dadar`, `2: To Andheri`).
- **Wear OS AMOLED Design System**:
  - Fully optimized for circular smartwatches (e.g., Samsung Galaxy Watch 4/5/6/7, Pixel Watch).
  - Content padding and bezel-curved constraints prevent clipping by the bezel or system status indicators.
  - AMOLED pure black theme (`#000000`) for maximum battery efficiency.

## 🛠️ Tech Stack & Architecture

- **Platform**: Wear OS (API 30+)
- **UI Framework**: Jetpack Compose for Wear OS, Wear Material 3, Wear Foundation
- **Language**: Kotlin 2.0+ (Coroutines, Flow)
- **Local Persistence**: Room SQLite database with optimized spatial & sequence queries
- **Networking**: OkHttp 4 with transparent GZIP decompression and fallback stream decoding
- **Testing & Deployment**: Verified on Wear OS emulator and physical Samsung Galaxy Watch 6 Classic

## 📱 Installation

### Via ADB (Wireless Debugging)

```bash
# Pair device (first-time only)
adb pair <WATCH_IP>:<PAIRING_PORT> <PAIRING_CODE>

# Connect to watch
adb connect <WATCH_IP>:<CONNECTION_PORT>

# Install APK
adb -s <WATCH_IP>:<CONNECTION_PORT> install -r app/build/outputs/apk/debug/app-debug.apk
```
