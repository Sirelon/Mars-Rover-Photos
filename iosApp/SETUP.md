# iOS App Setup Guide

## Current status

✅ Xcode project checked in
✅ Swift source files in place (`MarsRoverApp.swift`, `ContentView.swift`)
✅ Shared debug XCFramework build script wired into Xcode
✅ Firebase iOS SDK declared via SPM
✅ iOS platform Koin startup wired via `IosApp.shared.start()`
✅ `GoogleService-Info.plist` gitignored; template checked in

## Quick start

```bash
# 1. Build the debug XCFramework consumed by Xcode
./gradlew :shared:assembleSharedDebugXCFramework

# 2. Open the project
open iosApp/iosApp.xcodeproj

# 3. Add iosApp/iosApp/GoogleService-Info.plist if you need Firebase

# 4. Run (Cmd+R)
```

## Firebase setup

Firebase packages are resolved automatically by Xcode through Swift Package Manager.

You must provide `iosApp/iosApp/GoogleService-Info.plist`:

1. Open Firebase Console → Project Settings → iOS apps.
2. Download the plist for bundle ID `com.sirelon.marsroverphotos`.
3. Place it at `iosApp/iosApp/GoogleService-Info.plist`.

`GoogleService-Info.template.plist` is checked in only as a shape reference.

## Build artifacts

The Xcode project's "Build KMP Framework" script runs:

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```

and expects the shared framework at:

```text
shared/build/XCFrameworks/debug/shared.xcframework
```

## Troubleshooting

### Shared framework not found

Build the XCFramework manually first:

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```

### SPM packages do not resolve

In Xcode, run `File -> Packages -> Resolve Package Versions`.

### `GoogleService-Info.plist` missing

Firebase startup will fail until the real plist is present at `iosApp/iosApp/GoogleService-Info.plist`.

## Deep links

- `marsrover://` is registered in `Info.plist`.
- `MarsRoverApp.swift` forwards URLs into Kotlin with `Main_iosKt.pushDeepLink(urlString:)`.
- TODO: universal links still need associated-domain entitlements before `https://marsroverphotos.app/...` opens the iOS app directly.

## Architecture

```text
MarsRoverApp.swift  →  FirebaseApp.configure()
                    →  IosApp.shared.start()
ContentView.swift   →  Main_iosKt.MainViewController()
                    →  shared.xcframework
                    →  App.kt (commonMain)
```
