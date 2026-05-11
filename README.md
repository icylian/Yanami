English | [简体中文](README_zh.md)

# YanamiNext

![Badge](https://hitscounter.dev/api/hit?url=https%3A%2F%2Fgithub.com%2Fciallothu%2FYanamiNext&label=ciallothu%2FYanamiNext&icon=github&color=%23feb272&message=&style=flat&tz=UTC)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/ciallothu/YanamiNext)

<p style="text-align: center;">
    <img alt="banner" src="docs/assets/banner.png">
</p>

**YanamiNext** supports Android & iPhone for the [Komari](https://github.com/komari-monitor/komari) server monitoring tool. The Android app is built with Material Design 3, and the iPhone app is built with SwiftUI.

> A Komari client that supports Android & iPhone.

---

## Features

- **Multi-Instance Management** — Add, edit, and switch between multiple Komari server instances.
- **Three Authentication Modes** — Support password, API Key, and guest mode authentication.
- **Real-Time Node List** — WebSocket real-time push for node status (CPU / RAM / Disk / Network IO).
- **Node Detail Dashboard** — Load history line charts, Ping latency trends, basic server information.
- **SSH Terminal** — Full-featured ANSI/VT100 terminal based on termux terminal-view + WebSocket, supporting special key toolbars and font size adjustment.
- **Home Screen Widget** — Glance widget for node overview, refresh, and update interval configuration.
- **iPhone App Preview** — Native SwiftUI iPhone app with multi-instance management, password / API Key / guest mode, custom HTTP headers, auto-refreshing node list, node detail, load and ping records.
- **Tablet Landscape Layout** — Adaptive large-screen layout with NavigationRail, multi-column lists, and split detail panels.
- **Multi-Language Support** — Chinese (Default), English, Japanese.
- **Theme System** — Material You dynamic colors (Android 12+) + 6 preset color palettes, supporting dark/light mode and system-following mode.

## Screenshots

<details>

<summary>Expand</summary>

### Instance Management

<p style="text-align: center;">
    <img alt="addserver" src="docs/assets/addserver.png" width="360"> <img alt="serverlist" src="docs/assets/serverlist.png" width="360">
</p>

### Day/Light Mode (Phone)

<p style="text-align: center;">
    <img alt="nodelist" src="docs/assets/nodelist.png" width="360"> <img alt="nodedetail1" src="docs/assets/nodedetail1.png" width="360">
</p>

### Day/Light Mode (Tablet)

<p style="text-align: center;">
    <img alt="nodelisttablet" src="docs/assets/nodelisttablet.png" width="720">
</p>

<p style="text-align: center;">
    <img alt="nodedetail1tablet" src="docs/assets/nodedetail1tablet.png" width="720">
</p>

### Night/Dark Mode

<p style="text-align: center;">
    <img alt="nodelistdark" src="docs/assets/nodelistdark.png" width="360"> <img alt="nodedetaildark" src="docs/assets/nodedetaildark.png" width="360">
</p>

### Latency Monitoring/SSH Terminal

<p style="text-align: center;">
    <img alt="nodedetail2" src="docs/assets/nodedetail2.png" width="360"> <img alt="nodeterminal" src="docs/assets/nodeterminal.png" width="360">
</p>

### Snippets

<p style="text-align: center;">
    <img alt="snippetslist" src="docs/assets/snippetslist.png" width="360"> <img alt="addsnippet" src="docs/assets/addsnippet.png" width="360">
</p>

### Widget

<p style="text-align: center;">
    <img alt="widget" src="docs/assets/widget.png" width="360"> <img alt="widgetdark" src="docs/assets/widgetdark.png" width="360">
</p>

</details>

## System Requirements

| Item | Requirement |
|---|---|
| Android | 9.0 (API 28) and above |
| iPhone | iOS 16 and above |
| Server | Komari 1.1.7 or above |

## Build

```bash
# Android debug APK
(cd apps/android && ./gradlew assembleDebug)

# Android release APK
(cd apps/android && ./gradlew assembleRelease)

# Clean and build Android debug APK
(cd apps/android && ./gradlew clean assembleDebug)

# Unsigned iPhone IPA
BUILD_NUMBER=${GITHUB_RUN_NUMBER:-1}
BRANCH_REF=${GITHUB_REF_NAME:-local}
BRANCH_VERSION=$(printf '%s' "$BRANCH_REF" | tr '[:upper:]' '[:lower:]' | tr '/' '-' | sed -E 's/[^a-z0-9._-]+/-/g; s/-+/-/g; s/^-//; s/-$//')
SHORT_SHA=${GITHUB_SHA:-local}
SHORT_SHA=${SHORT_SHA:0:7}
VERSION="YanamiNext-Build-${BRANCH_VERSION:-local}-${SHORT_SHA}"
xcodebuild \
  -project apps/iphone/Yanami.xcodeproj \
  -scheme Yanami \
  -configuration Release \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  -derivedDataPath build/ios \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY="" \
  DEVELOPMENT_TEAM="" \
  PROVISIONING_PROFILE_SPECIFIER="" \
  MARKETING_VERSION="1.0" \
  CURRENT_PROJECT_VERSION="$BUILD_NUMBER" \
  build
mkdir -p build/ios-ipa/Payload
ditto build/ios/Build/Products/Release-iphoneos/Yanami.app build/ios-ipa/Payload/YanamiNext.app
(cd build/ios-ipa && ditto -c -k --sequesterRsrc --keepParent Payload "../${VERSION}.ipa")
```

Android build outputs are located at `apps/android/app/build/outputs/apk/`. CI pre-release assets use `YanamiNext-Build-<branch>-<short-sha>`; the unsigned iPhone IPA is generated at `build/YanamiNext-Build-<branch>-<short-sha>.ipa` and must be signed by the installer before device installation.

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Kotlin | 2.3.10 | Android main language |
| Swift | 5 | iPhone main language |
| Jetpack Compose BOM | 2026.02.01 | Android UI framework |
| SwiftUI | iOS 16+ | iPhone UI framework |
| MD3 | — | Android design system |
| Voyager | 1.1.0-beta03 | Navigation + ScreenModel |
| Koin | 4.1.1 | Dependency Injection |
| Ktor | 3.4.1 | HTTP Client + WebSocket |
| Room | 2.8.4 | Local Database (Encrypted credential storage) |
| Vico | 3.0.3 | Charts (Compose M3) |
| termux terminal-view | 0.119.0-beta.3 | Terminal ANSI/VT100 Rendering |
| DataStore Preferences | 1.2.0 | User Preferences Persistence |

## Architecture

The Android app adopts the **MVI (Model-View-Intent)** pattern with an adaptive root shell:

```
UI Layer      MainActivity Root Shell + Voyager Screen + Compose UI + MviViewModel<State, Event, Effect>
Domain Layer  Repository Interface + Domain Model (Node, ServerInstance …)
Data Layer    Repository Implementation, Ktor, Room, DataStore
```

Each page follows the **Contract Pattern**, describing the complete MVI contract of the page with nested `State` / `Event` / `Effect`.

The Android app lives under `apps/android`, and the iPhone app lives under `apps/iphone` as a native SwiftUI project. The iPhone source is split into `Models`, `Services`, `Stores`, `Views`, `Utilities`, and `Resources`, matching the Android domain/data/UI separation while using native iOS APIs.

### Navigation Flow

```
ServerListScreen → AddServerScreen
                 → NodeListScreen → NodeDetailScreen → SshTerminalScreen
                 → SettingsHubScreen → SettingsScreen / AboutScreen
```

### Authentication & Network

- **PASSWORD** — Obtain `session_token` via `POST /api/login` (supports 2FA).
- **API_KEY** — Use `Authorization: Bearer <api-key>` directly, without login flow.
- **GUEST** — No authentication header; monitor APIs and WebSocket data remain available, but SSH terminal is disabled.
- Credentials and session data are encrypted with AES/GCM and stored in Room, automatically restored on startup.
- WebSocket (`wss://host/api/rpc2`) always requires the `Origin` header.
- `SessionCookieInterceptor` / network layer automatically inject Cookie, Bearer token, or skip auth headers according to `authType`.

### Adaptive Layout

- Phone / narrow width: standard Voyager stack navigation.
- Tablet landscape: root-level `NavigationRail` + content pane.
- Node and server lists switch to multi-column card layouts on wide landscape screens.
- Node detail charts and info cards use split wide-screen layouts.
- Form and settings pages use centered constrained-width content on large screens.

## License

This project is licensed under the [MIT License](LICENSE).
