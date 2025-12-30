# iOS App Module

This module will contain the iOS app built with SwiftUI that wraps the shared Compose Multiplatform UI.

## Setup

The iOS app will be created using Xcode and will:
1. Link to the `shared` framework
2. Use Cocoapods for Firebase dependencies
3. Implement iOS-specific platform code

## Structure

```
iosApp/
├── iosApp/
│   ├── MarsRoverApp.swift       # App entry point
│   ├── ContentView.swift        # Main view
│   └── Info.plist
└── iosApp.xcodeproj/            # Xcode project
```

## TODO

- [ ] Create Xcode project
- [ ] Configure Cocoapods for Firebase
- [ ] Implement SwiftUI wrapper for Compose UI
- [ ] Setup iOS platform implementations

## Building

Will be built using Xcode once the project is set up.
