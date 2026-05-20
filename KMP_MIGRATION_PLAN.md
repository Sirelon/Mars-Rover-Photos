# Mars Rover Photos KMP Migration Plan

> Goal: ship Mars Rover Photos on Android and iOS from one shared Kotlin Multiplatform
> codebase, while keeping Desktop building as a useful side benefit. Web/WASM is deferred
> to `WASM_WEB_SUPPORT.md`.

This document is meant to be read top-down. The main plan is first. Background and
technical reference notes are at the bottom.

---

## Main Plan

### Current State

- ✅ KMP scaffolding exists: `shared/`, `androidApp/`, `desktopApp/`, and `iosApp/`.
- ✅ Shared Navigation 3, Koin, common resources, tracking, BuildInfo, Crashlytics hooks,
  shared image UI helpers, zoom support, and ad slots are in place.
- ✅ Ticket S0 is done.
- ✅ Ticket S1, Rovers home, is done.
- ✅ Ticket S2 is done.
- ✅ Ticket S3 is done.
- ✅ Ticket S4 is done.
- ✅ Ticket S6, Mission info, is done.
- ⏭️ Next main ticket: **S5 — Popular Photos**.
- ⚠️ iOS still needs Firebase, save/share, an Xcode project, and deep links before the app
  is fully useful there.
- 🚫 Web/WASM remains out of scope for this milestone.

### How Agents Should Use This Plan

1. Confirm which ticket you own before editing: `S0`, `S1`, `S2`, ..., `S9`,
   or iOS work `6.1`, `6.2`, `6.3`, `6.4`.
2. Stay inside the owned ticket. Other agents may be working nearby.
3. Use the legacy `app/` module only as a reference. Do not modify it.
4. Do not re-add `:app` to `settings.gradle.kts`.
5. Do not bump Gradle or library versions unless the ticket explicitly requires it.
6. After finishing a ticket, update this file with minimal additive edits:
   mark DoD bullets with ✅, mark the ticket header complete, and add any blocker note.

### Migration Board

| Ticket | Status | Main outcome |
|---|---|---|
| S0 — Cross-cutting prerequisites | ✅ Done | Shared resources, tracker, BuildInfo, Crashlytics hook, paging-compose, zoom, image UI helpers, ad slot |
| S1 — Rovers home | ✅ Done | Real shared rover list, rover images, bottom navigation, mission/photos navigation |
| S2 — Rover photos grid | ✅ Done | Real rover photos screen with sol/date filters |
| S3 — Image gallery + photo info sheet | ✅ Done | Fullscreen gallery, zoom, photo info sheet, save/share hooks |
| S4 — Favorites | ✅ Done | Shared favorites grid backed by Room/Paging |
| S5 — Popular photos | Pending | Shared popular tab backed by Firebase data on Android |
| S6 — Mission info | ✅ Done | Shared rover mission detail screen |
| S7 — About/settings | ✅ Done | Shared settings UI: theme, facts, cache, rate app |
| S8 — Ukraine route decision | ✅ Done | Shared Ukraine banner and Ukraine screen |
| S9 — Android widget adaptation | ✅ Done | Keep widget Android-only, but wire it to shared repositories/settings/tracker |
| 6.1 — Firebase iOS | Pending | Popular data, analytics, Crashlytics on iOS |
| 6.2 — iOS image save/share | Pending | Save to Photos and native share sheet |
| 6.3 — Xcode project bootstrap | ✅ Done | Checked-in iOS project/workspace that teammates can run |
| 6.4 — iOS deep links | Pending | `marsrover://` and universal-link handling on iOS |
| Final cleanup | Pending | Delete legacy `app/`, final smoke tests, CI gate |

---

## Screen Migration Tickets

### ~~Ticket S0 — Cross-Cutting Prerequisites~~ ✅

**Goal:** create the shared pieces that real screens need before they move into `commonMain`.

**Done:**
- ✅ Compose resources moved into `shared/src/commonMain/composeResources/`.
- ✅ `Tracker` interface and platform implementations are wired through Koin.
- ✅ `BuildInfo` is initialized by each platform entry point.
- ✅ `recordException` expect/actual exists.
- ✅ Paging Compose is available from common code.
- ✅ Shared `Modifier.zoomable` exists.
- ✅ Shared `MarsImageComposable`, stats, favorite toggle, and network image UI exist.
- ✅ `AdSlot` expect composable exists; platform implementations are safe placeholders unless
  Android chooses to render a real ad.

**Validation expectation:** Android builds, iOS framework links, Desktop still compiles/runs,
and the old placeholders still render before real screens replace them.

**Notes:**
- `paging-compose-common` does not exist on Maven Central. The working artifact is
  `androidx.paging:paging-compose:3.4.2`.

---

### ~~Ticket S1 — Rovers Home~~ ✅

**Goal:** replace the shared `RoversScreen` placeholder with the real rover list.

**Done:**
- ✅ `RoversScreen(onNavigateToPhotos, onMissionInfoClick)` lives in `commonMain`.
- ✅ `RoversViewModel` exposes real rover data from `RoversRepository`.
- ✅ Rover images resolve from Compose Multiplatform resources.
- ✅ Rover taps navigate to photos.
- ✅ Mission-info taps navigate to mission detail.
- ✅ Bottom navigation for Rovers, Favorites, Popular, and About is available from common code.
- ✅ Desktop and iOS Koin startup include navigation entries and initialize rover data.
- ✅ `KMP_MIGRATION_PLAN.md` marks S1 complete.

**Notes:**
- `androidx.compose.material3:material3-adaptive-navigation-suite` does not resolve for the
  enabled iOS targets, so `MarsBottomBar` currently uses common Material3 `NavigationBar`
  instead of `NavigationSuiteScaffold`.

---

### ~~Ticket S2 — Rover Photos Grid~~ ✅

**Goal:** replace the shared `PhotosScreen` placeholder with the real rover photo browser.

**Port from legacy:**
- `feature/photos/RoverPhotosScreen.kt`
- `feature/photos/EmptyPhotos.kt`
- `feature/photos/FactCard.kt`
- `feature/photos/Mapper.kt`
- Any small photo-screen helpers still needed by the screen

**Already available in shared:**
- `PhotosViewModel`
- `GridItemTransformer`
- `GridItem`
- `RoverDateUtil`
- `PhotosRepository`

**Use these replacements:**
- Android `R.string.*` and `R.drawable.*` → Compose Multiplatform `Res.string.*` /
  `Res.drawable.*`
- `java.util.Calendar` or `TimeZone` → `kotlinx-datetime`
- Direct Material3 DatePicker usage → existing `PlatformDatePickerDialog`
- `rememberSaveable` local state is fine to keep

**Definition of Done:**
- ✅ `PhotosScreen(roverId, ...)` is fully in `commonMain`.
- ✅ Photos load for the selected rover.
- ✅ Sol filtering works.
- ✅ Earth-date picking works.
- ✅ Android, iOS framework, and Desktop compile.

*Note: uses the shared CMP Material3 date picker wrapper, not native platform pickers.
`Mapper.kt` was not duplicated — shared `data/network/Mappers.kt` already covers the mapping.*

---

### ~~Ticket S3 — Image Gallery + Photo Info Sheet~~ ✅

**Goal:** replace the shared `ImagesScreen` placeholder with the real fullscreen gallery.

**Port from legacy:**
- `feature/images/ImagesScreen.kt`
- `feature/images/PhotoInfoBottomSheet.kt`

**Already available in shared:**
- `ImageViewModel`
- `ImageOperations`
- `MarsImage`
- `Modifier.zoomable`

**Use these replacements:**
- Legacy JVM-only zoomable library → shared `Modifier.zoomable`
- `BuildConfig.DEBUG` → `BuildInfo.isDebug`
- Android image-open `Intent` → `openLocalImage(uri)` expect API if needed
- `LocalHapticFeedback` can stay if it compiles in CMP

**Definition of Done:**
- ✅ Fullscreen image paging works.
- ✅ Zoom and pan work (`Zoomable.kt` redesigned with proper `ZoomableState`; resets on page swipe).
- ✅ Tap-to-toggle controls work.
- ✅ Photo info sheet works (`String.format` replaced with KMP-safe `formatStatValue()`).
- ✅ Android share/save works; `Intent.ACTION_VIEW` replaced with `LocalUriHandler.openUri`.
- ✅ iOS renders the screen; Save/Share show an error snackbar until ticket `6.2`.
- ✅ `AppDestination.Images` updated: `photoId` → `photoIds: List<String>` + `selectedId`.

*Note: `makePopular`/`removePopular` debug buttons are no-ops — `ImagesRepository` common
interface doesn't expose those Firebase methods. Wire up when `6.1` (Firebase iOS) lands.*

---

### ~~Ticket S4 — Favorites~~ ✅

**Goal:** replace the shared `FavoriteScreen` placeholder with the real favorites grid.

**Port from legacy:**
- `feature/favorite/FavoriteScreen.kt`

**Already available in shared:**
- Common `FavoriteImagesViewModel`
- Shared `MarsImageComposable`
- Paging Compose dependency from S0

**Use these replacements:**
- Android `LazyPagingItems` assumptions → multiplatform Paging Compose APIs
- Android resources → Compose Multiplatform resources
- `java.util.UUID` → `kotlin.uuid.Uuid.random()`
- Direct `Prefs` access → `AppSettings` through Koin

**Definition of Done:**
- ✅ Favorites grid loads from Room.
- ✅ Favorite/unfavorite behavior still works.
- ✅ Empty state works.
- ✅ Android, iOS framework, and Desktop compile.

---

### Ticket S5 — Popular Photos

**Goal:** extract and migrate the popular-photos tab into shared UI.

**Port from legacy:**
- Popular tab UI currently embedded in the legacy favorite/popular scaffolding
- Create `presentation/screens/PopularScreen.kt`

**Already available in shared:**
- `PopularPhotosViewModel`
- `PopularRemoteMediator`
- `IFirebasePhotos`

**Important caveat:**
- Android can show real data.
- iOS will show the empty state until Firebase iOS work in ticket `6.1` is done.

**Definition of Done:**
- Popular tab renders the grid on Android with real data.
- iOS and Desktop compile and show a sane empty state where Firebase is not available.

---

### ~~Ticket S6 — Mission Info~~ ✅

**Goal:** replace the shared mission placeholder with the real rover mission detail screen.

**Port from legacy:**
- `feature/mission/RoverMissionInfoScreen.kt`

**Already available in shared:**
- `RoverMissionInfoViewModel`
- `RoverMissionData`
- `CameraSpec`
- `MissionInfoUtils`
- `Rover.painter()` from S1

**Use these replacements:**
- `LocalContext.current` image lookup → `Rover.painter()`
- `java.text.NumberFormat` → small common thousands-format helper
- `coil3.compose.AsyncImage` can stay

**Definition of Done:**
- ✅ Rover header renders.
- ✅ Mission stats render.
- ✅ Camera grid renders.
- ✅ Android, iOS framework, and Desktop compile.

*Note: `java.text.NumberFormat` replaced with a KMP-safe `Int.abbreviate()` helper (e.g. `4.3M`, `471.0K`) — more readable in compact stat cards than thousands separators. `LazyVerticalGrid` inside `LazyColumn` replaced with two plain `Row`s to avoid nested-scroll issues.*

---

### ~~Ticket S7 — About / Settings~~ ✅

**Goal:** complete the shared About/settings screen.

**Port from legacy:**
- `feature/settings/AboutAppScreen.kt`

**Use these replacements:**
- `BuildConfig.VERSION_NAME` → `BuildInfo.versionName`
- Android resources → Compose Multiplatform resources
- `Prefs.themeLiveData` → `AppSettings.themeFlow`
- Calendar year formatting → `kotlinx-datetime`
- Android cache formatting → common byte formatting
- Android rate-app intent → `openAppStore()` or platform callback

**Definition of Done:**
- ✅ Theme selection works.
- ✅ Facts toggle works.
- ✅ Clear cache works.
- ✅ Rate app action works or degrades safely on each platform.
- ✅ Android, iOS framework, and Desktop compile.

---

### ~~Ticket S8 — Ukraine Route Decision~~ ✅

**Goal:** decide and implement the shared Ukraine banner/screen flow.

**Port from legacy:**
- `feature/ukraine/Ukraine.kt`

**Target shared files:**
- `presentation/screens/UkraineScreen.kt`
- `presentation/ui/UkraineBanner.kt`

**Use these replacements:**
- `RoverApplication.APP.dataManger.trackClick` → injected `Tracker`
- Existing `recordException` expect API for error reporting
- Existing CMP URI handler for outbound links

**Definition of Done:**
- ✅ Ukraine banner appears in the intended root location.
- ✅ Tapping the banner opens `UkraineScreen`.
- ✅ Outbound links work or degrade safely on all platforms.
- ✅ Android, iOS framework, and Desktop compile.

*Note: `UkraineBanner` is inserted into `AppNavigation` between the main content and the ad slot; it is hidden when already on `UkraineScreen` to avoid redundancy. `LocalUriHandler`/`RoverApplication` replaced with `rememberPlatformUriHandler()` and Koin-injected `Tracker`. `UkraineScreen` wraps all links in try/catch with `recordException` so link failures degrade safely on Desktop and iOS.*

---

### ~~Ticket S9 — Android Widget Adaptation~~ ✅

**Goal:** keep the widget Android-only, but make it use shared data contracts.

**Keep in `androidApp/`:**
- `widget/MarsPhotoWidget.kt`
- `widget/MarsPhotoWidgetReceiver.kt`
- `widget/MarsPhotoWidgetWorker.kt`
- `widget/MarsPhotoWidgetConfigActivity.kt`

**Work:**
- Replace `Prefs.*` reads with shared `AppSettings`.
- Replace direct DB access with `ImagesRepository`.
- Replace `RoverApplication.APP.tracker` with shared `Tracker`.

**Definition of Done:**
- ✅ Widget builds from `androidApp`.
- ✅ Widget reads shared settings/repositories.
- ✅ Widget can still deep link into the app through `WidgetExtraImageId`.

*Note: `ImagesRepository` and `RestApi` are injected via Koin (`KoinComponent` in `CoroutineWorker`). `Timber` replaced with shared `Logger` (Kermit). Legacy `feature/rovers/*Id` constants replaced with shared `domain.models.*_ID`. `RoversActivity` replaced with `MainActivity`. Added `DeepLink.Image(id: String)` to handle widget photo taps; handled in `MainActivity.handleDeepLink` by checking `WidgetExtraImageId` extra before processing URI deep links. `initialLayout` uses a local placeholder layout — `@layout/glance_default_loading` from the Glance library was not resolvable in this setup.*

---

## iOS Work That Can Run in Parallel

### 6.1 — Firebase iOS SDK

**Goal:** make Firebase-backed features work on iOS.

**Work:**
- Add Firebase dependencies to `iosApp/Podfile`.
- Keep `GoogleService-Info.plist` local/gitignored and provide a template if needed.
- Prefer GitLive Firebase KMP if it reduces platform-specific wrapper code.
- Replace the current iOS no-op Firebase implementations.

**Definition of Done:**
- Popular data works on iOS.
- Analytics works on iOS.
- Crashlytics can receive a test crash from iOS.

---

### 6.2 — Image Save / Share on iOS

**Goal:** make image save and share useful from the iOS gallery.

**Work:**
- Implement `IosImageOperations.saveImage`.
- Request add-only Photos permission.
- Add `NSPhotoLibraryAddUsageDescription`.
- Implement `IosImageOperations.shareImage` with a native share sheet.
- Handle iPad popover source configuration.

**Definition of Done:**
- Save writes the current photo to the iOS Photos app.
- Share opens the native iOS share sheet from `ImagesScreen`.

---

### ~~6.3 — Xcode Project Bootstrap~~ ✅

**Goal:** make the iOS app buildable by a teammate without manual project setup.

**Work:**
- Check in an iOS Xcode project/workspace.
- Wire the shared framework build step.
- Prefer a CocoaPods-based setup if it makes framework integration cleaner.
- Commit `Podfile.lock` if CocoaPods is used.

**Definition of Done:**
- ✅ `iosApp/iosApp.xcodeproj` checked in with a shared scheme.
- ✅ "Build KMP Framework" run script phase calls `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` before compile.
- ✅ `FRAMEWORK_SEARCH_PATHS` points to the Gradle output directory.
- ✅ `shared.framework` linked and embedded.
- ✅ `GoogleService-Info.template.plist` checked in as shape reference; real file is gitignored.
- ✅ `.gitignore` updated: `Pods/`, `xcuserdata/`, `GoogleService-Info.plist` excluded; `Podfile.lock` not excluded.
- ✅ `iosApp/README.md` updated with quick-start steps.

*Note: `Podfile.lock` is not yet committed because `pod install` has not been run in this session.
Run `cd iosApp && pod install` and commit the resulting `Podfile.lock` to pin pod versions.*

---

### 6.4 — iOS Deep Links

**Goal:** match Android deep-link behavior on iOS.

**Work:**
- Add `marsrover://` URL scheme.
- Add associated domains for universal links if the required AASA file is hosted.
- Parse incoming Swift URLs into shared `DeepLink`.
- Pass the pending deep link into `MainViewController`.

**Definition of Done:**
- Opening `marsrover://rover/5` on iOS navigates to Curiosity photos.
- Universal links work once domain setup is available.

---

## Verification

Before declaring a ticket done, run:

```bash
./gradlew :androidApp:assembleDebug \
          :shared:linkDebugFrameworkIosSimulatorArm64 \
          :shared:testDebugUnitTest detekt
```

Use extra checks when they are relevant:

```bash
./gradlew :shared:compileKotlinDesktop
./gradlew :desktopApp:run
./gradlew connectedDebugAndroidTest
```

Manual smoke flow after migrated screens exist:

1. Open the app.
2. Cycle through Rovers, Favorites, Popular, and About.
3. Open a rover.
4. Open a photo.
5. Exercise save/share where the platform supports it.

---

## Whole Migration Done Means

1. `settings.gradle.kts` lists only active KMP app modules; legacy `:app` is gone.
2. `PlaceholderScreens.kt` is gone.
3. Every `AppDestination` maps to a real shared composable or a platform-owned route.
4. Android has feature parity with the legacy app.
5. iOS builds and runs from a checked-in project/workspace.
6. iOS has Firebase, save/share, and deep-link behavior.
7. Desktop opens the same shared UI, with acceptable no-op behavior for mobile-only features.
8. CI runs the important Android, iOS framework, unit-test, and detekt checks.

---

## Reference Details

The sections below are for implementation context. They are intentionally below the main plan.

### Module Layout

```text
settings.gradle.kts
├── :shared      common KMP code, Compose UI, domain/data, DI
├── :androidApp  Android shell: Activity, manifest, app icons, Android-only services
├── :desktopApp  Desktop shell
├── iosApp/      Swift/iOS shell; project bootstrap still pending
└── app/         legacy Android app, reference-only during migration
```

### Source Set Responsibilities

- `shared/src/commonMain`: shared domain, repositories, view models, common Compose UI,
  common navigation, common interfaces, and common resources.
- `shared/src/androidMain`: Android actuals and Android platform integrations.
- `shared/src/iosMain`: iOS actuals and iOS platform integrations.
- `shared/src/desktopMain`: Desktop actuals and Desktop platform integrations.
- `androidApp/`: Android app shell and Android-only features.
- `iosApp/`: Swift shell, iOS project files, iOS app metadata.
- `desktopApp/`: Desktop app shell.
- `app/`: legacy reference only; do not edit during screen migration.

### Shared Code Already Available

- Domain models and repository interfaces for rovers, photos, images, facts, mission data,
  settings, and themes.
- Room database, DAOs, network API, repository implementations, and paging mediator.
- Shared app entry, Navigation 3 destinations, navigator, and Koin navigation entries.
- Shared view models for photos, image gallery, favorites, popular photos, mission info,
  about/settings, and rovers.
- Shared UI helpers: material-symbol icons, snackbar, platform date picker, toast host,
  URI handling, dynamic color/system theme expects, adaptive layout, zoom, image cards,
  rover painter, ad slot, and bottom navigation.

### Legacy Reference Files

Use these files as references only:

| Legacy area | Reference files |
|---|---|
| Rovers home | `feature/rovers/RoversActivity.kt`, `feature/rovers/RoversNavigation.kt` |
| Rover photos | `feature/photos/RoverPhotosScreen.kt`, `EmptyPhotos.kt`, `FactCard.kt`, `Mapper.kt` |
| Gallery | `feature/images/ImagesScreen.kt`, `PhotoInfoBottomSheet.kt` |
| Favorites | `feature/favorite/FavoriteScreen.kt` |
| Popular | Popular UI embedded around the legacy favorite/popular flow |
| Mission info | `feature/mission/RoverMissionInfoScreen.kt` |
| About/settings | `feature/settings/AboutAppScreen.kt` |
| Ukraine | `feature/ukraine/Ukraine.kt` |
| Widget | `widget/*` |

### Do Not Move These Into `commonMain`

- Android `Application`, `Activity`, `ComponentActivity`, manifests, intents, splash setup,
  edge-to-edge setup, Android system UI/theme APIs.
- Android `R.string`, `R.drawable`, `painterResource`, `stringResource`.
- `AndroidViewModel`, `Application` injection, `koin-android`.
- Firebase Android SDKs, Google Play services, AdMob, UMP, Glance, WorkManager.
- `room-ktx`, `room-paging`, Android `Room.databaseBuilder(context, ...)`.
- `SharedPreferences`, `Context`, `Bitmap`, `MediaStore`, `ContentResolver`.
- Android platform Ktor engines.
- AndroidX Navigation 3 APIs that are not available to the enabled shared targets.
- `Parcelable`, `@Parcelize`, AndroidX annotations like `@DrawableRes`.
- Timber.

### Preferred Common Replacements

- KMP `ViewModel`.
- Injected repositories/settings.
- Compose Multiplatform resources.
- Common interfaces with platform actuals or callbacks.
- Koin core, Koin Compose, and Koin ViewModel APIs.
- Kermit/logging wrapper.
- Ktor core in common with platform engines in source sets.
- Room runtime only for enabled Android/iOS/Desktop targets.

### Android-Only Features That Stay in `androidApp`

| Concern | Owner |
|---|---|
| AdMob banner | Android app shell or Android actual slot |
| GDPR/UMP consent | Android app shell |
| Glance widget | `androidApp/widget/` |
| Firebase Crashlytics init / Performance | `androidApp/MarsRoverApplication.kt` |
| Splashscreen | `androidApp/MainActivity.kt` |
| Mission/facts uploader tooling | Android debug-only tooling |
| Android intent parsing | `androidApp/MainActivity.kt`, converted to shared `DeepLink` |

### Known Caveats

- The shared module still uses `kotlin.multiplatform + com.android.library` with AGP compatibility
  flags. Migrating to `com.android.kotlin.multiplatform.library` is separate work.
- Some dependency-resolution warnings mention `iosX64`; the required simulator framework target is
  `iosSimulatorArm64`.
- Material3 adaptive navigation suite is not currently usable from this shared iOS setup.
- Popular photos on iOS stay empty until Firebase iOS is implemented.
- Save/share on iOS stays limited until ticket `6.2`.
- Desktop can use no-op Firebase/ad behavior as long as shared UI compiles and opens.

### Version Details

This plan deliberately does not list library versions. Use `gradle/libs.versions.toml` as the
source of truth for versions.
