# iOS App Setup Guide

## Current Status

✅ Xcode project checked in (`iosApp.xcodeproj`)
✅ Swift source files in place (`MarsRoverApp.swift`, `ContentView.swift`)
✅ Shared KMP framework linked and build script wired
✅ Firebase iOS SDK declared via SPM (auto-resolved by Xcode)
✅ iOS platform Koin modules set up
✅ `GoogleService-Info.plist` gitignored; template checked in

## Quick Start (fresh clone)

```bash
# 1. Build the shared KMP framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 2. Open the project — Xcode resolves Firebase via SPM automatically
open iosApp/iosApp.xcodeproj

# 3. Add your GoogleService-Info.plist (see Firebase section below)

# 4. Run (⌘R)
```

## Firebase Setup

Firebase packages (FirebaseCore, FirebaseAnalytics, FirebaseCrashlytics, FirebaseFirestore)
are declared as Swift Package Manager dependencies in `iosApp.xcodeproj`. Xcode resolves
them automatically when you open the project — no manual steps needed.

You must supply `iosApp/iosApp/GoogleService-Info.plist` (gitignored):

1. Go to Firebase Console → Project Settings → iOS apps.
2. Download `GoogleService-Info.plist` for bundle ID `com.sirelon.marsroverphotos`.
3. Place it at `iosApp/iosApp/GoogleService-Info.plist`.

A `GoogleService-Info.template.plist` is checked in as a shape reference.

## Build for Different Configurations

For **device builds**, update the framework search path in Build Settings:
- Simulator Debug: `$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework`
- Device Release: `$(SRCROOT)/../shared/build/bin/iosArm64/releaseFramework`

## Troubleshooting

### Framework Not Found

Build the framework manually first:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### SPM Packages Not Resolving

In Xcode: **File → Packages → Resolve Package Versions**

### Swift Linker Errors

Ensure `shared.framework` is set to **Embed & Sign** in  
Target → General → Frameworks, Libraries, and Embedded Content.

## Architecture

```
MarsRoverApp.swift  →  FirebaseApp.configure()
                    →  initKoinIos()
ContentView.swift   →  Main_iosKt.MainViewController()
                    →  shared.framework (Compose Multiplatform)
                    →  App.kt (commonMain)
```

## Framework Locations

| Config | Path |
|---|---|
| Simulator Debug | `shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework` |
| Simulator Release | `shared/build/bin/iosSimulatorArm64/releaseFramework/shared.framework` |
| Device Debug | `shared/build/bin/iosArm64/debugFramework/shared.framework` |
| Device Release | `shared/build/bin/iosArm64/releaseFramework/shared.framework` |
