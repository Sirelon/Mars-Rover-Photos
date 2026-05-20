# iOS App

SwiftUI shell that hosts the shared Compose Multiplatform UI from the `shared` KMP module.

## Quick start

```bash
# 1. Build the shared KMP framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 2. Install Firebase pods
cd iosApp
pod install

# 3. Open the workspace (NOT the .xcodeproj)
open iosApp.xcworkspace
```

Press **⌘R** to run on a simulator.

> **Note:** The "Build KMP Framework" run script phase in Xcode calls Gradle automatically before
> compiling, so after the first manual build you can just press Run from Xcode.

## Firebase setup (ticket 6.1)

The app will launch and show the Compose UI without Firebase. Popular photos will show an empty
state until Firebase is configured.

To enable Firebase:

1. Download `GoogleService-Info.plist` from the Firebase Console  
   (Project Settings → iOS app, bundle ID `com.sirelon.marsroverphotos.iosApp`).
2. Place it at `iosApp/iosApp/GoogleService-Info.plist`  
   (this path is gitignored — never commit the real file).
3. A `GoogleService-Info.template.plist` is checked in at the repo root as a shape reference.
4. Add `FirebaseApp.configure()` to `MarsRoverApp.swift` (ticket 6.1 work).

## Structure

```
iosApp/
├── Podfile                          # CocoaPods: Firebase pods
├── GoogleService-Info.template.plist  # Shape reference — fill in & rename for Firebase
├── iosApp.xcodeproj/               # Xcode project (checked in)
│   ├── project.pbxproj
│   └── xcshareddata/xcschemes/iosApp.xcscheme
└── iosApp/
    ├── MarsRoverApp.swift           # @main entry — calls initKoinIos()
    ├── ContentView.swift            # Wraps MainViewController from shared framework
    └── Info.plist
```

## Architecture

```
MarsRoverApp.swift  →  initKoinIos()
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
