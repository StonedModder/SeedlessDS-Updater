<!-- unleashedapi -->
<p align="center">
  <a href="https://unleashedapi.com/?utm_source=github&utm_medium=readme&utm_campaign=stonedmodder&utm_content=SeedlessDS-Updater">
    <img src="https://raw.githubusercontent.com/StonedModder/unleashedapi/main/assets/banner-slim.png" alt="UnleashedAPI — uncensored OpenAI-compatible AI API. Free to start." width="100%">
  </a><br>
  <a href="https://unleashedapi.com/?utm_source=github&utm_medium=readme&utm_campaign=stonedmodder&utm_content=SeedlessDS-Updater"><strong>UnleashedAPI</strong></a> — uncensored OpenAI-compatible models. Free to start, no prompt logging.
</p>

<p align="center">
  <img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" width="80" height="80" alt="Seedless Updater" />
</p>

<h1 align="center">Seedless Updater</h1>

<p align="center">
  <a href="https://github.com/SapphireRhodonite/SeedlessDS"><img src="https://img.shields.io/badge/updates-SeedlessDS-4CAF50?style=flat-square" alt="SeedlessDS" /></a>
  <img src="https://img.shields.io/badge/min%20SDK-21%20(Android%205.0)-34A853?style=flat-square&logo=android" alt="Min SDK" />
  <img src="https://img.shields.io/badge/target%20SDK-36-4285F4?style=flat-square&logo=android" alt="Target SDK" />
  <img src="https://img.shields.io/badge/language-Kotlin-7F52FF?style=flat-square&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/platform-arm64--v8a-FF6F00?style=flat-square" alt="Architecture" />
  <img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License" />
</p>

<p align="center">
  <b>A lightweight installer and updater for <a href="https://github.com/SapphireRhodonite/SeedlessDS">SeedlessDS</a> — the Nintendo DS emulator for Android handhelds.</b>
</p>

---

## What it does

Seedless Updater is a companion app that keeps [SeedlessDS](https://github.com/SapphireRhodonite/SeedlessDS) installed and up to date on your Android handheld. It works even if SeedlessDS has never been installed before.

- Fetches the latest SeedlessDS release directly from the GitHub API
- Downloads the APK with a live progress bar (percentage and bytes)
- Requests install-from-unknown-sources permission automatically
- Launches the Android package installer when the download completes
- Detects whether SeedlessDS is already installed and offers **install** or **update** accordingly
- Works on devices where SeedlessDS has never been installed

## Why

SeedlessDS releases appear on GitHub, but Android handhelds don't have an app store. Sideloading each update from a PC is tedious. This app gives you a one-tap update path directly on the device.

---

## Screenshot

<p align="center">
  <img src=".github/screenshot.png" width="320" alt="Seedless Updater screenshot" />
</p>

---

## Requirements

| Requirement | Detail |
|---|---|
| Android | 5.0+ (API 21) |
| Architecture | arm64-v8a |
| Internet | Required to fetch releases and download APKs |
| Permissions | `INTERNET`, `REQUEST_INSTALL_PACKAGES` |

> **Note:** The installer only fetches SeedlessDS. The emulator itself requires a compatible Android handheld with arm64-v8a support.

---

## Building

Open the project in **Android Studio** (Hedgehog or later) and run **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Or from the command line:

```bash
# Set Android SDK path (or create local.properties)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# Build debug APK (auto-signed)
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## How it works

```
┌──────────────┐     ┌──────────────┐     ┌───────────────┐
│  GitHub API  │────▶│   Updater    │────▶│  Android       │
│  (releases)  │     │   downloads  │     │  Package       │
│              │     │   APK +      │     │  Installer     │
│              │     │   progress   │     │               │
└──────────────┘     └──────────────┘     └───────────────┘
```

1. **Fetch** — Queries `api.github.com/repos/SapphireRhodonite/SeedlessDS/releases/latest`
2. **Download** — Streams the APK over HTTPS with real-time progress tracking
3. **Permission** — Prompts for `REQUEST_INSTALL_PACKAGES` if needed (Android 8+)
4. **Install** — Hands the APK to Android's package installer via `FileProvider`

---

## Permissions explained

| Permission | Why |
|---|---|
| `INTERNET` | Fetch release metadata and download the APK |
| `REQUEST_INSTALL_PACKAGES` | Required on Android 8+ to install APKs from outside an app store |
| `WRITE_EXTERNAL_STORAGE` | Only requested on Android 9 and below for download storage |

The app requests permissions only when needed — tap **Install** or **Update** and it guides you through the flow.

---

## Project structure

```
SeedlessUpdater/
├── app/
│   ├── build.gradle.kts          # App dependencies and config
│   ├── proguard-rules.pro        # R8 / ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, activities, FileProvider
│       ├── java/com/seedlessds/updater/
│       │   └── MainActivity.kt   # All UI + download + install logic
│       └── res/
│           ├── layout/           # activity_main.xml
│           ├── values/           # strings, colors, theme
│           ├── xml/              # FileProvider paths
│           ├── drawable/         # Launcher icon foreground
│           └── mipmap-*/         # Adaptive icon
├── build.gradle.kts              # Project-level Gradle config
├── settings.gradle.kts           # Module declarations
├── gradle.properties             # JVM args and AndroidX flags
└── gradle/wrapper/               # Gradle wrapper
```

---

## Related

- [SeedlessDS](https://github.com/SapphireRhodonite/SeedlessDS) — The Nintendo DS emulator this app installs
- [WatermelonDS](https://github.com/SapphireRhodonite/WatermelonDS) — The sister melonDS-based project

---

## Disclaimer

Seedless Updater is not affiliated with, endorsed by, or connected to SeedlessDS, DraStic, or Nintendo. It only downloads the publicly available APK from the official SeedlessDS GitHub releases page.

---

## License

MIT
