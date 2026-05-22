# Migrating an Android App to Kotlin Multiplatform: Lessons Learned

This is an experience-based guide for developers and AI agents migrating a production Android app to Kotlin Multiplatform (KMP) with Compose Multiplatform UI. It covers what worked, what failed, and what surprises are waiting. It is not tied to any specific project.

---

## The Approach That Worked: Ticket-Based Screen Migration

The most effective structure for a large migration is to keep the app running at all times and migrate one screen or cross-cutting concern at a time. Concretely:

1. Keep the old Android module alive as a read-only reference. Do not delete it until everything is verified.
2. Create a `shared/` KMP module alongside the existing `androidApp/`.
3. Define clear "tickets" — one per screen or concern. Each ticket has an explicit definition of done.
4. Migrate screens in dependency order: shared data layer first, then leaf screens, then navigation.
5. Compile all platforms after every ticket: Android, iOS simulator framework, Desktop. If any platform breaks, fix it before moving on.

This approach works especially well with AI agents because each ticket is a bounded context. A well-written ticket can be handed to an agent with clear success criteria and no ambiguity about what is out of scope.

The migration plan itself should include:
- A "do not move these" list (Android-only APIs)
- A "preferred replacements" table (e.g., `Timber` → Kermit, `java.text.NumberFormat` → custom KMP helper)
- Per-ticket notes that accumulate as implementation reveals surprises

---

## What Went Well

### ViewModels and repositories translated almost verbatim

KMP `ViewModel` (from `androidx.lifecycle:lifecycle-viewmodel`) is nearly API-identical to Android `ViewModel`. If your architecture was already ViewModel + repository + coroutines, 80% of the business logic moves to `commonMain` with minimal changes. The main exclusions are Android-specific things injected into ViewModels (Context, Application, etc.) — replace those with interfaces.

### Koin DI works great across all platforms

Koin's multiplatform support is mature. Module definitions, `startKoin`, `koinViewModel`, and `get<T>()` all work from `commonMain`. The only platform-specific code is the entry point that calls `startKoin` (one per platform shell). This is a clean split.

### Compose Multiplatform resources are a solid foundation

The `composeResources/` system handles strings, drawables, fonts, and other files cleanly. String arguments, plurals, and qualifiers all work. One gotcha: use positional format specifiers (`%1$s`, `%2$d`) rather than `%s` and `%d` because the argument order can differ across platforms.

### expect/actual is the right pattern for platform differences

For anything that must behave differently per platform (file I/O, sharing, permissions, date pickers, URI handling), `expect`/`actual` with a common interface in `commonMain` and platform implementations in `androidMain`/`iosMain`/`desktopMain` is clean and scales well. Avoid putting too much in `commonMain` that should be platform-specific — it creates fragile `if (platform == ...)` code.

### Room 3.0 works across Android, iOS, and Desktop

Room 3.0 alpha ships KMP artifacts that support iOS and Desktop in addition to Android. The migration from Room 2.x is mostly mechanical (package rename, minor API changes). The database schema, DAOs, and migrations carry over intact.

---

## What Was Hard

### The iOS build toolchain is a significant hurdle

Getting the iOS app to actually build and run on a physical device involved a chain of problems that each had to be solved before the next one was visible:

1. **CocoaPods vs SPM**: CocoaPods-based KMP setups are increasingly painful. SPM is the right choice for new projects and migrations. Switching mid-migration is disruptive but worth it.
2. **XCFramework naming**: Gradle tasks for building XCFrameworks are named specifically — `assembleSharedDebugXCFramework`, not `assembleXCFramework`. Using an ambiguous or wrong task name causes cryptic build failures.
3. **Signing with a Personal Team**: Personal Team certificates do not support capabilities like Associated Domains. Any empty entitlement key blocks provisioning profile creation. Clean up the entitlements file before trying to sign.
4. **Embedding vs linking**: The shared XCFramework must be in both the "Link Binary With Libraries" phase (for compilation) AND the "Embed Frameworks" phase (for runtime). Linking without embedding means code runs but resources (fonts, images, strings) are inaccessible at runtime — a crash that is entirely invisible at build time.
5. **Firebase SPM cache corruption**: SPM package caches corrupt on incomplete downloads. When Firebase packages fail to resolve with HTTP stream errors, delete `~/Library/Caches/org.swift.swiftpm/` and re-resolve. Don't spend time debugging this — just clear it.

### iOS runtime crashes that are invisible at build time

The build succeeding tells you nothing about runtime behavior on iOS. There are at least four categories of runtime-only crash to expect:

**Room requires an explicit SQLite driver on iOS.** Room 3.0 no longer discovers a driver automatically. Add `.setDriver(NativeSQLiteDriver())` to every `Room.databaseBuilder()` call in `iosMain`. The crash message is clear once you see it, but it only appears at runtime.

**Compose Multiplatform enforces `CADisableMinimumFrameDurationOnPhone`.** As of CMP 1.11.0, if this key is absent from `Info.plist` (or set to `false`), the app throws an `IllegalStateException` from a background thread shortly after the first frame is composed. Add `<key>CADisableMinimumFrameDurationOnPhone</key><true/>` to `Info.plist`. This key also has a real functional purpose: it enables ProMotion (120 Hz) display refresh on iPhones that support it.

**Compose resources need the framework embedded, not just linked.** `DefaultIOsResourceReader` searches for resources inside `app.app/Frameworks/*.framework/composeResources/`. If `shared.framework` is only linked (not embedded), the resources directory is never present in the installed bundle. The symptom is a `MissingResourceException` on the first composable that loads a font, image, or string from the shared resource system.

**NSLog with `%@` format crashes in Kotlin/Native debug builds.** If you bridge Kotlin strings to `NSLog` using `%@` format specifiers, it can produce `EXC_BREAKPOINT`. Use Swift for any `NSLog`/`print` calls, or write diagnostic output to a file in the Documents directory instead.

### Java standard library APIs are everywhere

Android code accumulates `java.*` usage over time that must be replaced for KMP:

| Legacy API | KMP replacement |
|---|---|
| `java.util.Calendar`, `java.util.Date` | `kotlinx-datetime` |
| `java.text.NumberFormat`, `java.text.SimpleDateFormat` | Custom KMP helper or `kotlinx-datetime` |
| `java.util.UUID` | `kotlin.uuid.Uuid.random()` |
| `java.io.File` | `okio` or platform actuals |
| `String.format("%d", n)` | Custom helpers or string templates |
| `BuildConfig.*` | Shared `BuildInfo` initialized per platform |
| `Timber` | Kermit or a thin wrapper |
| `SharedPreferences` | `multiplatform-settings` or custom expect/actual |

Automated migration tools can find most of these, but some are subtle (e.g., `String.format` with positional args compiles on Android but fails at runtime on iOS if arguments are in the wrong order).

### Not all libraries have KMP versions

Before choosing any dependency, verify it has a KMP-compatible artifact. Common situations:

- **Firebase**: Firebase does not publish official KMP libraries. GitLive Firebase (a community wrapper) works for Firestore, Analytics, and Crashlytics. For Android-only Firebase features (Performance, Messaging), keep them in `androidApp`.
- **Material3 Adaptive Navigation Suite**: Not available for iOS targets at the time of writing. Fall back to standard `NavigationBar` in common code.
- **`paging-compose`**: The artifact is `androidx.paging:paging-compose`, not `paging-compose-common` (which does not exist on Maven Central).
- **AdMob**: Google Mobile Ads SDK is platform-specific. The correct approach is an `AdSlot` expect composable in `commonMain` with real implementations in `androidMain` and `iosMain`, and a no-op in `desktopMain`.

### AGP + KMP version alignment is brittle

Android Gradle Plugin (AGP), Kotlin, Compose Multiplatform, and Room all need to be version-aligned. A version bump to any one of them can break another. Keep a `gradle/libs.versions.toml` as the single source of truth and avoid bumping versions mid-migration unless a specific fix requires it.

### Nested lazy layouts cause issues

`LazyVerticalGrid` inside `LazyColumn` (or any nested scrollable with the same axis) does not work correctly in Compose Multiplatform. Replace nested grids with `Row`-based layouts or restructure the composable hierarchy to avoid nesting.

---

## Architecture Decisions

### What stays in each platform shell

Keep the following Android-only:
- `Application`, `Activity`, `ComponentActivity`, manifests
- Android splash screen, edge-to-edge setup, nav bar contrast enforcement
- Firebase Android SDK initialization
- Google Play services: AdMob init, UMP consent, Glance widgets, WorkManager
- `SharedPreferences`, `Context`, `Bitmap`, `MediaStore`, `ContentResolver`
- `Timber`, `@Parcelize`, `@DrawableRes`

Keep the following iOS-only:
- `UIApplication`, `UIViewController`, `SwiftUI.App`
- Firebase iOS via SPM, `FirebaseApp.configure()`
- `ATTrackingManager` (ATT prompt)
- `PHPhotoLibrary` (photo saving)
- `UIActivityViewController` (share sheet)

Anything else can usually go into `commonMain`.

### Navigation

AndroidX Navigation Component (Nav 2) is Android-only. Navigation 3 (`androidx.navigation3`) has KMP-compatible artifacts. If migrating, switch to Navigation 3 in `commonMain`. The API is different enough that it is not a drop-in replacement, but the migration is manageable screen by screen.

### Firebase via GitLive

GitLive Firebase wraps the native SDKs and exposes a common Kotlin API. It works well for Firestore, Analytics, and Crashlytics. The initialization is still platform-specific (call `Firebase.initialize()` in each platform shell), but the read/write API lives entirely in `commonMain`. This is the most practical Firebase strategy for KMP until Google ships official KMP libraries.

---

## iOS-Specific Notes for AI Agents

When an AI agent works on the iOS side of a KMP migration, these are the highest-value things to know:

1. **Always verify the build with a physical device, not just the simulator.** The simulator runs under Rosetta and has different behavior. Signing, provisioning, and some runtime APIs only manifest on device.

2. **Collect crash logs via `xcrun devicectl`.** When the app crashes on device, use `xcrun devicectl device copy from --device <udid> --domain-type systemCrashLogs --source <file.ips>` to pull `.ips` crash reports. The `lastExceptionBacktrace` and `asi` fields contain the most useful information.

3. **The Embed Frameworks build phase must include the shared XCFramework.** This is a common omission in templated Xcode projects. Without it, all Compose resources are unavailable at runtime.

4. **Kotlin/Native debug builds split code from resources.** In debug mode, the actual Kotlin code runs from `iosApp.debug.dylib`, not from `shared.framework`. The framework binary is a stub. The resources, however, live only in the framework, so embedding it is still required even though the framework binary itself is not what executes.

5. **`PlistSanityCheck` runs on a background thread after first compose.** The crash appears asynchronously and may look unrelated to Compose initialization. The fix is a single plist key, not a code change.

6. **SPM package caches corrupt silently.** If `xcodebuild -resolvePackageDependencies` fails with HTTP errors, clear `~/Library/Caches/org.swift.swiftpm/` and retry before doing anything else.

---

## Things That Drew Attention

### Compose resource bundle structure on iOS

When Compose Multiplatform resources are compiled, they land inside the XCFramework at `shared.framework/composeResources/<package.name>/`. The `DefaultIOsResourceReader` looks for files at `app.app/Frameworks/*.framework/composeResources/`. This lookup only works if the framework is embedded. The root cause is non-obvious from the `MissingResourceException` message alone — the exception says a file path is missing, but says nothing about why the path doesn't exist.

### Room 3.0 requires a driver declaration that Room 2.x did not

Room 2.x detected and used a default SQLite driver automatically. Room 3.0 requires an explicit `.setDriver()` call. On Android the driver is `BundledSQLiteDriver()` or `AndroidSQLiteDriver()`. On iOS it is `NativeSQLiteDriver()` from `androidx.sqlite:sqlite-driver`. Forgetting this produces a clear error message at runtime, but only on iOS — Android can fall through to a default in some configurations.

### CocoaPods and KMP do not age well together

KMP's CocoaPods integration worked when it was introduced but has accumulated friction as both ecosystems evolved. SPM is simpler, faster, and more reliably supported by both Kotlin tooling and Xcode. If a project uses CocoaPods for KMP, migrating to SPM early in the migration (not last) removes a consistent source of friction.

### Paging 3 source of truth for the image viewer

When migrating a photo gallery backed by Room + Paging 3, the image detail screen needs access to the full ordered list to support swiping between images. A common mistake is to navigate with only a single image ID and try to reconstruct the list from the ViewModel. Instead, navigate with either an ordered list of IDs or a typed source enum (`DIRECT`, `FAVORITES`, `POPULAR`) and let the destination load the full list from Room. This avoids the list being truncated to whatever the current paging window held when the user tapped.

### `String.format` with positional specifiers

Android's `String.format("Hello %s, you have %d items", name, count)` works on Android but is JVM-only. Compose Multiplatform resource strings need positional specifiers: `%1$s` and `%2$d`. Even when the string itself is in `composeResources/values/strings.xml`, the format-call site must use the positional form. This is easy to miss because the compiler does not catch it — it fails at runtime on iOS.

---

## Summary Checklist for a KMP Migration

**Before you start:**
- [ ] Identify all `java.*` usages and map each to a KMP replacement
- [ ] Audit third-party dependencies for KMP artifact availability
- [ ] Choose SPM over CocoaPods for iOS
- [ ] Set up a verification script that compiles Android + iOS framework + Desktop after every change

**During screen migration:**
- [ ] Move domain model, repository interface, and ViewModel first
- [ ] Replace Android resources with `composeResources/`
- [ ] Use `expect`/`actual` for any platform-specific behavior
- [ ] Replace nested lazy layouts with non-scrolling alternatives

**iOS runtime readiness:**
- [ ] Add `CADisableMinimumFrameDurationOnPhone = true` to `Info.plist`
- [ ] Add `.setDriver(NativeSQLiteDriver())` to every iOS Room builder
- [ ] Add the shared XCFramework to the Embed Frameworks build phase, not just Link
- [ ] Clean entitlements — no empty keys (especially Associated Domains)
- [ ] Test on a physical device, not only the simulator

**Firebase:**
- [ ] Use GitLive Firebase for Firestore, Analytics, Crashlytics in `commonMain`
- [ ] Keep Firebase Performance, Messaging, and Android-only SDKs in `androidApp`
- [ ] Add `GoogleService-Info.plist` to `.gitignore`; commit only a template

**Ads:**
- [ ] Implement `AdSlot` as an `expect` composable with real implementations per platform
- [ ] Initialize AdMob after consent (UMP on Android, ATT + UMP on iOS)
- [ ] Use test ad unit IDs during development; swap to real IDs before release
