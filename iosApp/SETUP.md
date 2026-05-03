# iOS App Setup Guide

## Overview

The iOS app structure has been created with Kotlin 2.3.0 and all necessary files. You now need to create the Xcode project and configure it.

## Current Status

✅ iOS framework builds successfully with Kotlin 2.3.0
✅ Swift source files created (MarsRoverApp.swift, ContentView.swift)
✅ Main.ios.kt created for Compose UI integration
✅ Info.plist configured
✅ Podfile created for Firebase dependencies
✅ iOS platform Koin modules set up

## Steps to Complete Setup

### 1. Create Xcode Project

Open Xcode and create a new project:

1. **File → New → Project**
2. Select **iOS → App**
3. Configure the project:
   - **Product Name**: `iosApp`
   - **Organization Identifier**: `com.sirelon.marsroverphotos`
   - **Bundle Identifier**: `com.sirelon.marsroverphotos.iosApp`
   - **Interface**: SwiftUI
   - **Language**: Swift
   - **Use Core Data**: NO
   - **Include Tests**: YES (optional)
4. Save location: Choose `iosApp/` directory (replace existing)

### 2. Add Existing Swift Files

1. In Xcode, delete the default Swift files
2. **Right-click on iosApp folder → Add Files to "iosApp"**
3. Select:
   - `MarsRoverApp.swift`
   - `ContentView.swift`
4. Ensure **"Copy items if needed"** is UNCHECKED (files are already in place)

### 3. Link Shared Framework

1. In Xcode, select the **iosApp project** in navigator
2. Select the **iosApp target**
3. Go to **General tab**
4. Scroll to **Frameworks, Libraries, and Embedded Content**
5. Click **+** button
6. Click **Add Other → Add Files**
7. Navigate to: `../shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework`
8. Set **Embed & Sign**

**Important**: Add a **Run Script Phase** to build the framework before compilation:

1. Go to **Build Phases tab**
2. Click **+** → **New Run Script Phase**
3. Drag it **above "Compile Sources"**
4. Name it: **"Build Shared Framework"**
5. Add script:
```bash
cd "$SRCROOT/.."
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### 4. Configure Framework Search Paths

1. Select **iosApp target → Build Settings**
2. Search for **"Framework Search Paths"**
3. Add:
```
$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework
```

### 5. Install Firebase via CocoaPods

```bash
cd iosApp
pod install
```

After installation:
- Close Xcode if open
- Open `iosApp.xcworkspace` (NOT .xcodeproj)

### 6. Build for Different Configurations

For **Release builds** on real devices:
1. Change the framework path in Build Settings to use:
   - Simulator: `.../iosSimulatorArm64/debugFramework/`
   - Device: `.../iosArm64/releaseFramework/`
2. Update Run Script to build appropriate target

### 7. Test the Build

1. Select **iPhone 15 Pro simulator** (or any iOS 15+ simulator)
2. Click **Run** (⌘R)
3. The app should launch with the Compose UI

## Troubleshooting

### Framework Not Found

Ensure you've built the framework first:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### Swift Linker Errors

Make sure the framework is:
1. Added to **Frameworks, Libraries, and Embedded Content**
2. Set to **Embed & Sign**
3. Framework Search Path is configured

### Cocoapods Issues

```bash
cd iosApp
pod deintegrate
pod install
```

## Framework Locations

- **Simulator (Debug)**: `shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework`
- **Simulator (Release)**: `shared/build/bin/iosSimulatorArm64/releaseFramework/shared.framework`
- **Device (Debug)**: `shared/build/bin/iosArm64/debugFramework/shared.framework`
- **Device (Release)**: `shared/build/bin/iosArm64/releaseFramework/shared.framework`

## Next Steps

After successful build:
1. Implement actual Firebase iOS SDK in `FirebaseAnalytics.ios.kt`
2. Implement actual Firebase Firestore in `IosFirebasePhotos`
3. Test all features on iOS
4. Configure app icons and launch screen
5. Setup Firebase project for iOS
6. Add GoogleService-Info.plist

## Architecture

```
iosApp (SwiftUI)
    ↓
MarsRoverApp.swift → initKoinIos()
    ↓
ContentView.swift → ComposeView
    ↓
Main_iosKt.MainViewController()
    ↓
shared.framework (Compose UI)
    ↓
App.kt (commonMain)
```

## Current Versions

- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.9.3
- **Room**: 2.8.4 (KMP stable)
- **Ktor**: 3.3.3
- **Koin**: 4.2.0-beta2

All dependencies are at their latest compatible versions! 🎉
