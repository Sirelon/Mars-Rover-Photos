# iOS App

SwiftUI shell that hosts the shared Compose Multiplatform UI from the `shared` KMP module.

## Quick start

```bash
# 1. Build the debug XCFramework consumed by Xcode
./gradlew :shared:assembleSharedDebugXCFramework

# 2. Open the project in Xcode — Firebase packages resolve automatically via SPM
open iosApp/iosApp.xcodeproj
```

Press `Cmd+R` to run on a simulator.

> The checked-in Xcode project includes a "Build KMP Framework" run script phase that calls
> `./gradlew :shared:assembleSharedDebugXCFramework`, so after the first successful build you can
> usually run directly from Xcode.

## Firebase setup

Firebase is wired via Swift Package Manager:

- `FirebaseCore`, `FirebaseAnalytics`, `FirebaseCrashlytics`, and `FirebaseFirestore` are declared in
  `iosApp.xcodeproj`.
- `MarsRoverApp.swift` calls `FirebaseApp.configure()` on launch.
- `GoogleService-Info.plist` must exist at `iosApp/iosApp/GoogleService-Info.plist`.
- `GoogleService-Info.template.plist` is checked in as a shape reference.

To configure Firebase for a fresh clone:

1. Download `GoogleService-Info.plist` from the Firebase Console for bundle ID `com.sirelon.marsroverphotos`.
2. Place it at `iosApp/iosApp/GoogleService-Info.plist`.
3. Open the project and run.

## Structure

```text
iosApp/
├── GoogleService-Info.template.plist
├── iosApp.xcodeproj/
│   ├── project.pbxproj
│   └── xcshareddata/xcschemes/iosApp.xcscheme
└── iosApp/
    ├── MarsRoverApp.swift
    ├── ContentView.swift
    ├── BannerAdFactory.swift
    ├── GoogleService-Info.plist
    ├── Info.plist
    └── Assets.xcassets/
```

## Deep links (ticket 6.4 ✅)

The `marsrover://` URL scheme is registered in `Info.plist`. When iOS delivers a URL,
`MarsRoverApp.swift` forwards it to `Main_iosKt.pushDeepLink(urlString:)` which parses
it in Kotlin and injects the result into the running Compose content via a `MutableStateFlow`.

- Custom scheme `marsrover://` is registered in `Info.plist`.
- `MarsRoverApp.swift` forwards incoming URLs to `Main_iosKt.pushDeepLink(urlString:)`.
- The shared iOS parser also understands `https://marsroverphotos.app/...`, but associated domains
  are not configured in the checked-in entitlements yet, so universal links are still a TODO.

Supported URLs:
| URL | Effect |
|---|---|
| `marsrover://rover/5` | Navigate to rover 5 (Curiosity) photos |
| `marsrover://photo/123` | Open photo 123 in the gallery |

Test from terminal (simulator must be running):
```bash
xcrun simctl openurl booted "marsrover://rover/5"
xcrun simctl openurl booted "marsrover://photo/123"
```

Universal links (`https://marsroverphotos.app/…`) work once the AASA file is hosted on the domain.

## Architecture

```text
MarsRoverApp.swift  →  FirebaseApp.configure()
                    →  IosApp.shared.start()
                    →  .onOpenURL → Main_iosKt.pushDeepLink()
ContentView.swift   →  Main_iosKt.MainViewController()
                    →  shared.xcframework (Compose Multiplatform)
                    →  App.kt (commonMain)
```

## Shared framework output

The Xcode project points at:

```text
shared/build/XCFrameworks/debug/shared.xcframework
```

That XCFramework bundles both simulator and device slices for the shared Kotlin framework.
