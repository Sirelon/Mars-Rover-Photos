# iOS App

SwiftUI shell that hosts the shared Compose Multiplatform UI from the `shared` KMP module.

## Quick start

```bash
# 1. Build the shared KMP framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 2. Open the project in Xcode — Firebase packages resolve automatically via SPM
open iosApp/iosApp.xcodeproj
```

Press **⌘R** to run on a simulator.

> **Note:** The "Build KMP Framework" run script phase in Xcode calls Gradle automatically before
> compiling, so after the first manual build you can just press Run from Xcode.
>
> On first open, Xcode will resolve the Firebase iOS SDK via Swift Package Manager.
> This requires an internet connection and may take a few minutes.

## Firebase setup (ticket 6.1 ✅)

Firebase is wired end-to-end via Swift Package Manager:

- **FirebaseCore, FirebaseAnalytics, FirebaseCrashlytics, FirebaseFirestore** are declared as
  SPM dependencies in `iosApp.xcodeproj` — Xcode resolves them automatically.
- `MarsRoverApp.swift` calls `FirebaseApp.configure()` on launch.
- `GoogleService-Info.plist` must be present at `iosApp/iosApp/GoogleService-Info.plist`
  (gitignored — never commit the real file).
- `GoogleService-Info.template.plist` is checked in as a shape reference.

To configure Firebase for a fresh clone:

1. Download `GoogleService-Info.plist` from the Firebase Console  
   (Project Settings → iOS app, bundle ID `com.sirelon.marsroverphotos`).
2. Place it at `iosApp/iosApp/GoogleService-Info.plist`.
3. Open the project and run — Firebase resolves via SPM on first open.

## Structure

```
iosApp/
├── GoogleService-Info.template.plist  # Shape reference — fill in & rename for Firebase
├── iosApp.xcodeproj/                  # Xcode project (Firebase added via SPM)
│   ├── project.pbxproj
│   └── xcshareddata/xcschemes/iosApp.xcscheme
└── iosApp/
    ├── MarsRoverApp.swift           # @main entry — FirebaseApp.configure() + initKoinIos()
    ├── ContentView.swift            # Wraps MainViewController from shared framework
    ├── GoogleService-Info.plist     # Real Firebase config (gitignored)
    └── Info.plist
```

## Architecture

```
MarsRoverApp.swift  →  FirebaseApp.configure()
                    →  initKoinIos()
ContentView.swift   →  Main_iosKt.MainViewController()
                    →  shared.framework (Compose Multiplatform)
                    →  App.kt (commonMain)
```

## Framework locations

| Config | Path |
|---|---|
| Simulator Debug | `shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework` |
| Simulator Release | `shared/build/bin/iosSimulatorArm64/releaseFramework/shared.framework` |
| Device Debug | `shared/build/bin/iosArm64/debugFramework/shared.framework` |
| Device Release | `shared/build/bin/iosArm64/releaseFramework/shared.framework` |
