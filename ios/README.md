# Yanami iPhone

Native SwiftUI iPhone app for Yanami.

## Scope

- Connects to Komari with password, API Key, or guest mode.
- Supports custom HTTP headers, including Cloudflare Access service token headers.
- Stores the server profile and credentials in Keychain.
- Tests the connection through `common:getVersion`.
- Loads node information and latest status through Komari RPC.

## Build

```bash
BASE_VERSION=$(grep 'versionName' ../app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')
BUILD_NUMBER=${GITHUB_RUN_NUMBER:-1}
VERSION="${BASE_VERSION}-${BUILD_NUMBER}"
xcodebuild \
  -project Yanami.xcodeproj \
  -scheme Yanami \
  -configuration Release \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  -derivedDataPath ../build/ios \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY="" \
  DEVELOPMENT_TEAM="" \
  PROVISIONING_PROFILE_SPECIFIER="" \
  MARKETING_VERSION="$BASE_VERSION" \
  CURRENT_PROJECT_VERSION="$BUILD_NUMBER" \
  build
mkdir -p ../build/ios-ipa/Payload
ditto ../build/ios/Build/Products/Release-iphoneos/Yanami.app ../build/ios-ipa/Payload/Yanami.app
(cd ../build/ios-ipa && ditto -c -k --sequesterRsrc --keepParent Payload "../Yanami-v${VERSION}.ipa")
```

The unsigned IPA is generated at `../build/Yanami-v<base-version>-<build-number>.ipa`.

The IPA must be signed before device installation.
