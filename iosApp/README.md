# iOS App

SwiftUI shell that hosts the shared Compose Multiplatform UI from the `shared` KMP module.

## Quick start

```bash
# 1. Build the debug XCFramework consumed by Xcode
./gradlew :shared:assembleSharedDebugXCFramework

# 2. Add iosApp/iosApp/GoogleService-Info.plist (see Firebase setup)

# 3. Open the project in Xcode — packages resolve automatically via SPM
open iosApp/iosApp.xcodeproj
```

Press `Cmd+R` to run on a simulator. From the command line, always build the explicit `iosApp`
scheme: `KotlinMultiplatformLinkedPackage` registers one scheme per subpackage, so scheme
auto-detection picks a dependency instead of the app.

## Shared framework

The Xcode project links `$(KMP_XCFRAMEWORK_DIR)/shared.xcframework`. The build setting is defined
per configuration, and the "Build KMP Framework" run script phase assembles the XCFramework that
matches the configuration being built:

| Configuration | Gradle task | Links |
|---|---|---|
| Debug | `:shared:assembleSharedDebugXCFramework` | `shared/build/XCFrameworks/debug` |
| Release | `:shared:assembleSharedReleaseXCFramework` | `shared/build/XCFrameworks/release` |

So TestFlight and App Store archives link optimized Kotlin code. Xcode resolves the framework while
planning the build, before script phases run. A checkout that has never built a configuration
therefore needs the matching Gradle task run by hand once; after that the script phase keeps the
framework current.

Each XCFramework bundles the device (`ios-arm64`) and simulator (`ios-arm64-simulator`) slices.
There is no x86_64 slice — `shared` declares only `iosArm64` and `iosSimulatorArm64` targets.

## Firebase setup

Firebase and the ad SDKs are wired via Swift Package Manager. `iosApp.xcodeproj` declares
`FirebaseCore`, `FirebaseAnalytics`, `FirebaseCrashlytics`, `FirebaseFirestore`,
`FirebasePerformance`, `FirebaseStorage`, `FirebaseMessaging`, `GoogleMobileAds` and
`GoogleUserMessagingPlatform`. `MarsRoverApp.swift` calls `FirebaseApp.configure()` on launch.

`iosApp/iosApp/GoogleService-Info.plist` is gitignored, and the build fails on the missing input
file without it. For a fresh clone or worktree:

1. Copy it from an existing checkout, or download it from Firebase Console → Project Settings →
   iOS apps for bundle ID `com.sirelon.marsroverphotos`.
2. Place it at `iosApp/iosApp/GoogleService-Info.plist`.

`GoogleService-Info.template.plist` is checked in as a shape reference.

## KotlinMultiplatformLinkedPackage

`KotlinMultiplatformLinkedPackage/` is a checked-in Swift package. The GitLive Firebase KMP bindings
reach the Firebase iOS SDK through SwiftPM-import cinterops, and a framework consumed with
`embedAndSign` has to guarantee those Swift symbols end up in the binary — this package is that
guarantee, and the Xcode project references it as a local package.

It stays in git because Xcode resolves packages before the "Build KMP Framework" script phase runs,
so nothing can create it on demand: without it, `xcodebuild` stops at
`the package ... cannot be accessed`.

The build keeps the package current by itself: when the iOS-linked SwiftPM dependencies change, it
regenerates the package and stops once (see *Synthetic project regenerated* below). **Do not run
`./gradlew :shared:integrateLinkagePackage` on this project**, even when a failing build prints that
command. The project is already integrated; the task rewrites `iosApp.xcodeproj/project.pbxproj`
wholesale, and the app it then builds installs with `Missing bundle ID` because the packaged `.app`
has no `Info.plist`.

## Structure

```text
iosApp/
├── GoogleService-Info.template.plist
├── KotlinMultiplatformLinkedPackage/
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

```text
MarsRoverApp.swift  →  FirebaseApp.configure()
                    →  IosApp.shared.start()
                    →  .onOpenURL → Main_iosKt.pushDeepLink()
ContentView.swift   →  Main_iosKt.MainViewController()
                    →  shared.xcframework (Compose Multiplatform)
                    →  App.kt (commonMain)
```

## Deep links

The `marsrover://` URL scheme is registered in `Info.plist`. `MarsRoverApp.swift` forwards an
incoming URL to `Main_iosKt.pushDeepLink(urlString:)`, which parses it in Kotlin and injects the
result into the running Compose content. Universal links (`https://marsroverphotos.app/…`) are not
wired: the entitlements declare no associated domains. The supported URLs and the test commands are
in [docs/DEEP_LINKING.md](../docs/DEEP_LINKING.md).

## Troubleshooting

### "There is no XCFramework found" / shared framework not found

The configuration being built has never had its XCFramework assembled. Run the matching task once:

```bash
./gradlew :shared:assembleSharedDebugXCFramework    # or ...ReleaseXCFramework for a Release build
```

### `error: Synthetic project regenerated`

Emitted by `:shared:generateSyntheticLinkageSwiftPMImportProjectForEmbedAndSignLinkage` after a
GitLive (or other SwiftPM-backed) dependency is added to `iosMain` or bumped: the plugin rewrites
`iosApp/KotlinMultiplatformLinkedPackage` and the build stops until packages are resolved again.
Resolve them, then rebuild:

```bash
xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj -scheme iosApp
```

That directory is **tracked**, so commit the regenerated `Package.swift` and the new `subpackages/`
entry along with the Gradle change — otherwise the iOS build fails for everyone else.

### SPM packages do not resolve

In Xcode, run `File -> Packages -> Resolve Package Versions`, or use the `xcodebuild` command above.

### `shared.h has been modified since the module file ... .pcm was built`

Xcode's explicit-module cache is stale after a shared API change. Clean it, then build again:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -derivedDataPath iosApp/build/DerivedData clean
```

### `ld: ignoring file ... found architecture 'arm64', required architecture 'x86_64'`

There is no x86_64 slice to link. Build for an Apple-silicon simulator, or pass `ARCHS=arm64` to
`xcodebuild`.

### `iOS <version> is not installed` on every device destination

Xcode's iOS platform component is separate from the SDK and can be missing while
`xcodebuild -showsdks` still lists the SDK. Simulator builds work; device builds report
"Found no destinations". `xcodebuild -downloadPlatform iOS` restores it (a multi-gigabyte download).

### `git` and `xcodebuild` refuse to run after a macOS update

The Xcode license needs accepting again: `sudo xcodebuild -license`.

### Release archive fails with "Compilation finished with errors"

The Kotlin/Native linker ran out of heap. See
[.claude/skills/store-release/PITFALLS.md](../.claude/skills/store-release/PITFALLS.md).
