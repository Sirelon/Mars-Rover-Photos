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

The Xcode project's "Build KMP Framework" script assembles the XCFramework for the configuration
being built, and the linked `shared.xcframework` resolves through the per-configuration
`KMP_XCFRAMEWORK_DIR` build setting:

| Configuration | Gradle task | Framework location |
|---|---|---|
| Debug | `:shared:assembleSharedDebugXCFramework` | `shared/build/XCFrameworks/debug/shared.xcframework` |
| Release | `:shared:assembleSharedReleaseXCFramework` | `shared/build/XCFrameworks/release/shared.xcframework` |

## Troubleshooting

### Shared framework not found

Xcode resolves the framework while planning the build, before script phases run, so a fresh checkout
needs the matching task run by hand once — the debug one for a normal simulator run:

```bash
./gradlew :shared:assembleSharedDebugXCFramework    # or ...ReleaseXCFramework for a Release build
```

### `ld: ignoring file ... found architecture 'arm64', required architecture 'x86_64'`

`shared` declares only `iosArm64` and `iosSimulatorArm64` targets, so there is no x86_64 slice to
link. Build for an Apple-silicon simulator, or pass `ARCHS=arm64` to `xcodebuild`.

### SPM packages do not resolve

In Xcode, run `File -> Packages -> Resolve Package Versions`, or from the command line:

```bash
xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj -scheme iosApp
```

### `error: Synthetic project regenerated`

Emitted by `:shared:generateSyntheticLinkageSwiftPMImportProjectForEmbedAndSignLinkage` after a new
GitLive dependency is added to `iosMain`: the plugin rewrites
`iosApp/KotlinMultiplatformLinkedPackage` to add a subpackage for it, and the build stops until
packages are resolved again. Resolve as above, then rebuild.

That directory is **tracked**, so commit the regenerated `Package.swift` and new `subpackages/`
entry along with the Gradle change — otherwise the iOS build fails for everyone else.

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
