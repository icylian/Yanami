# YanamiNext iPhone

Native SwiftUI iPhone app for YanamiNext.

## Scope

- Connects to Komari with password, API Key, or guest mode.
- Supports custom HTTP headers, including Cloudflare Access service token headers.
- Stores server profiles, credentials, custom headers, active instance, and refresh settings in Keychain.
- Tests the connection through `common:getVersion`.
- Loads node information, latest status, node detail, recent load data, load records, and ping records through Komari RPC.
- Auto-refreshes node status on the same cadence as the Android live node list.

## Build

```bash
BUILD_NUMBER=${GITHUB_RUN_NUMBER:-1}
BRANCH_REF=${GITHUB_REF_NAME:-local}
BRANCH_VERSION=$(printf '%s' "$BRANCH_REF" | tr '[:upper:]' '[:lower:]' | tr '/' '-' | sed -E 's/[^a-z0-9._-]+/-/g; s/-+/-/g; s/^-//; s/-$//')
SHORT_SHA=${GITHUB_SHA:-local}
SHORT_SHA=${SHORT_SHA:0:7}
VERSION="YanamiNext-Build-${BRANCH_VERSION:-local}-${SHORT_SHA}"
xcodebuild \
  -project Yanami.xcodeproj \
  -scheme Yanami \
  -configuration Release \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  -derivedDataPath ../../build/ios \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY="" \
  DEVELOPMENT_TEAM="" \
  PROVISIONING_PROFILE_SPECIFIER="" \
  MARKETING_VERSION="1.0" \
  CURRENT_PROJECT_VERSION="$BUILD_NUMBER" \
  build
mkdir -p ../../build/ios-ipa/Payload
ditto ../../build/ios/Build/Products/Release-iphoneos/Yanami.app ../../build/ios-ipa/Payload/YanamiNext.app
(cd ../../build/ios-ipa && ditto -c -k --sequesterRsrc --keepParent Payload "../${VERSION}.ipa")
```

The unsigned IPA is generated at `../../build/YanamiNext-Build-<branch>-<short-sha>.ipa`.

The IPA must be signed before device installation.
