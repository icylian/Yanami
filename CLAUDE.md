# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yanami supports Android & iPhone for the **Komari** server monitoring tool. The Android app uses Material Design 3, and the iPhone app uses SwiftUI.

- **Package:** `com.sekusarisu.yanami`
- **Android:** Kotlin | **Min SDK:** 28 | **Target/Compile SDK:** 36 | Jetpack Compose with Material 3
- **iPhone:** Swift 5 | iOS 16+ | SwiftUI

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew testDebugUnitTest --tests "com.sekusarisu.yanami.ExampleUnitTest"

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean assembleDebug

# Build unsigned iPhone device app for IPA packaging
xcodebuild -project ios/Yanami.xcodeproj -scheme Yanami -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" DEVELOPMENT_TEAM="" PROVISIONING_PROFILE_SPECIFIER="" MARKETING_VERSION="$(grep 'versionName' app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')" CURRENT_PROJECT_VERSION="${GITHUB_RUN_NUMBER:-1}" build
```

## Architecture

The Android app uses an **MVI (Model-View-Intent)** pattern with three layers:

- **UI Layer:** Voyager `Screen` + Compose UI + `MviViewModel<State, Event, Effect>`
- **Domain Layer:** Repository interfaces + domain models (`Node`, `ServerInstance`, etc.)
- **Data Layer:** Repository implementations, Ktor (HTTP + WebSocket), Room DB, DataStore

Each screen follows a **Contract pattern** — a `Contract` object containing nested `State`, `Event`, and `Effect` types (e.g., `NodeListContract`).

### Key Libraries

| Library | Purpose |
|---|---|
| Voyager 1.1.0-beta03 | Navigation + ScreenModel lifecycle |
| Koin 4.1.1 | Dependency injection |
| Ktor 3.4.0 | HTTP client + WebSocket (RPC2 protocol) |
| Room 2.8.4 + KSP | Local database (encrypted credentials) |
| Vico 3.0.4 | Charts (Compose M3) |
| DataStore | User preferences (theme, language, dark mode) |

### Navigation Flow (Voyager)

```
ServerListScreen → AddServerScreen → NodeListScreen → NodeDetailScreen
                 ↘ SettingsHubScreen → SettingsScreen (视觉与样式)
                                     ↘ AboutScreen
```

### Data & Networking

- **Komari API** uses JSON-RPC 2.0 over WebSocket (`wss://domain/api/rpc2`) as primary transport, with HTTP POST fallback.
- WebSocket requires `Origin` header — without it, server returns 403.
- **Dual authentication modes** (`AuthType` enum):
  - `PASSWORD`: `session_token` cookie via `POST /api/login`, injected as `Cookie: session_token=xxx`
  - `API_KEY`: API Key used directly as `Authorization: Bearer <api-key>`, no login required
- `SessionCookieInterceptor` (OkHttp) auto-injects the appropriate auth header based on `authType`.
- Credentials and API Keys encrypted with AES/GCM via Android KeyStore (`CryptoManager`).
- Room DB v3 stores `auth_type` and `encrypted_api_key` columns.
- API details documented in `docs/API_STRUCTURES.md`.

### DI Setup

All dependencies registered in `di/AppModule.kt` via Koin. App initialized in `YanamiApplication.kt`.

The iPhone app lives in `ios/` as a native SwiftUI project. It supports Komari password / API Key / guest auth, custom HTTP headers for Cloudflare Access service tokens, Keychain persistence, connection testing, and node list loading.

## Internationalization

Default language is **Chinese (zh)**. Also supports English (en) and Japanese (ja). String resources in `res/values/`, `res/values-en/`, `res/values-ja/`. Runtime switching uses `AppCompatDelegate.setApplicationLocales()` — requires `AppCompatActivity`.

## Documentation

- `docs/ARCHITECTURE.md` — Architecture diagrams and flows (Chinese)
- `docs/API_STRUCTURES.md` — Complete Komari API reference
- `docs/PROGRESS.md` — Development status and session notes
- `REQUEST.MD` — Original requirements specification
