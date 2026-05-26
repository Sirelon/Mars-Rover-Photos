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
- ✅ Ticket S5, Popular Photos, is done.
- ✅ Final cleanup is done: CI gate is in place (`.github/workflows/ci.yml`); legacy `app/` module deleted.
- ✅ iOS platform parity items from this milestone are done: Firebase (6.1), save/share (6.2),
  and deep links (6.4).
- 🚫 Web/WASM remains out of scope for this milestone.

### How Agents Should Use This Plan

> **The KMP migration is complete.** All tickets are done and the legacy `app/` module has been deleted. This plan is kept as a historical record.

1. Do not re-add `:app` to `settings.gradle.kts`.
2. Do not bump Gradle or library versions unless the task explicitly requires it.

### Migration Board

| Ticket | Status | Main outcome |
|---|---|---|
| S0 — Cross-cutting prerequisites | ✅ Done | Shared resources, tracker, BuildInfo, Crashlytics hook, paging-compose, zoom, image UI helpers, ad slot |
| S1 — Rovers home | ✅ Done | Real shared rover list, rover images, bottom navigation, mission/photos navigation |
| S2 — Rover photos grid | ✅ Done | Real rover photos screen with sol/date filters |
| S3 — Image gallery + photo info sheet | ✅ Done | Fullscreen gallery, zoom, photo info sheet, save/share hooks |
| S4 — Favorites | ✅ Done | Shared favorites grid backed by Room/Paging |
| S5 — Popular photos | ✅ Done | Shared popular tab backed by Firebase data on Android |
| S6 — Mission info | ✅ Done | Shared rover mission detail screen |
| S7 — About/settings | ✅ Done | Shared settings UI: theme, facts, cache, rate app |
| S8 — Ukraine route decision | ✅ Done | Shared Ukraine banner and Ukraine screen |
| S9 — Android widget adaptation | ✅ Done | Keep widget Android-only, but wire it to shared repositories/settings/tracker |
| 6.1 — Firebase iOS | ✅ Done | Popular data, analytics, Crashlytics on iOS |
| 6.2 — iOS image save/share | ✅ Done | Save to Photos and native share sheet |
| 6.3 — Xcode project bootstrap | ✅ Done | Checked-in iOS project/workspace that teammates can run |
| 6.4 — iOS deep links | ✅ Done | `marsrover://` and universal-link handling on iOS |
| S10 — GDPR / Consent | ✅ Done | Android UMP consent flow restored; iOS ATT prompt added |
| S11 — iOS AdMob banner | ✅ Done | Real banner via GoogleMobileAds SPM + UMP iOS; Android NPA wired to consent state |
| Final cleanup | ✅ Done | CI gate (`.github/workflows/ci.yml`); legacy `app/` module deleted |
| Edge-to-edge | ✅ Done | Full edge-to-edge support: `isNavigationBarContrastEnforced`, `adjustResize`, ImagesScreen fullscreen |

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
- `androidx.room3:room3-paging:3.0.0-alpha05` ships KMP artifacts for iOS/Desktop (b/339934824
  fixed). `room-paging` moved to `commonMain`; `@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)`
  added to `AppDataBase`; `loadFavoritePagedSource()` and `loadPopularPagedSource()` restored in
  `ImagesDao`, `ImagesRepository`, and `ImagesRepositoryImpl`. ViewModels and screens still use
  the non-paged `Flow<List<MarsImage>>` path — wiring them to `LazyPagingItems` is a follow-up.

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

### ~~Ticket S5 — Popular Photos~~ ✅

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
- ✅ Popular tab renders the grid on Android with real data.
- ✅ iOS and Desktop compile and show a sane empty state where Firebase is not available.

*Note: `IFirebasePhotos.loadPopularPhotos()` is injected directly into `PopularPhotosViewModel` (bypassing the paged Room mediator which is still commented out for KMP-target reasons). Photos load as a `StateFlow<List<MarsImage>>` via a one-shot coroutine with retry. The placeholder `PopularScreen` in `PlaceholderScreens.kt` was removed. `popular_empty_title` string added to `strings.xml`.*

*Follow-up (2026-05-22): Favorites/Popular now use `LazyPagingItems` + Room paging sources and `PopularRemoteMediator` again. To avoid truncated `ImagesScreen` swiping when opening from a paged list, `AppDestination.Images` now carries an `ImagesSource` enum; the image viewer loads full lists from Room for `FAVORITES`/`POPULAR` and uses explicit `photoIds` only for `DIRECT_IDS` (Photos screen flow).*

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

*Note (2026-05-22): `IosAppReview` added in `shared/src/iosMain/.../platform/AppReview.ios.kt`.
Uses `SKStoreReviewController.requestReview(in:)` (scene-based, iOS 16+) with a fallback to the
class-level `requestReview()` on older iOS. Wired into Koin `PlatformModule.ios.kt`; `NoOpAppReview`
replaced. Desktop remains `NoOpAppReview` — falls back to opening the store URL.*

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

### ~~Ticket S10 — GDPR / Consent Flow~~ ✅

**Goal:** restore the Google UMP-based GDPR consent flow on Android; add iOS ATT prompt.

**Android work:**
- Ported `GdprHelper` from legacy `app/` into `androidApp/gdpr/GdprHelper.kt`.
- Replaced `Timber` with shared `Logger`; replaced `recordException` import with shared platform actual.
- Wrapped `ConsentDebugSettings` (EEA force) in `if (BuildConfig.DEBUG)`.
- `gdprHelper.init()` called in `MainActivity.onCreate` after `setContent`.
- `gdprHelper.acceptGdpr: StateFlow<Boolean>` is ready to wire into `AdSlot` when ads land.

**iOS work:**
- `NSUserTrackingUsageDescription` added to `Info.plist`.
- `ATTrackingManager.requestTrackingAuthorization` called from `MarsRoverApp.init()` with a 1 s delay
  (Apple guideline: show prompt after first frame). No new SPM dependency — system framework.

**Definition of Done:**
- ✅ `GdprHelper` lives in `androidApp/gdpr/`.
- ✅ `MainActivity` initialises the consent flow on startup.
- ✅ iOS `Info.plist` carries `NSUserTrackingUsageDescription`.
- ✅ iOS ATT authorization is requested on launch.
- ✅ Android, iOS framework, and unit tests compile.

*Note: ads remain a no-op (`AdSlot.android.kt` placeholder) until a future ticket wires them up.
GDPR/ATT consent is collected proactively so it is already in place when ads land.*

---

## iOS Work That Can Run in Parallel

### ~~6.1 — Firebase iOS SDK~~ ✅

**Goal:** make Firebase-backed features work on iOS.

**Work:**
- Add Firebase dependencies to the Xcode project.
- Keep `GoogleService-Info.plist` local/gitignored and provide a template if needed.
- Prefer GitLive Firebase KMP if it reduces platform-specific wrapper code.
- Replace the current iOS no-op Firebase implementations.

**Definition of Done:**
- ✅ Popular data works on iOS (`FirebasePhotosImpl` via GitLive Firestore, already in `commonMain`).
- ✅ Analytics works on iOS (`IosTracker` now backed by `FirebaseAnalytics` via GitLive).
- ✅ Crashlytics can receive a test crash from iOS (`RecordException.ios.kt` via GitLive).

*Note: Firebase iOS SDK added via Swift Package Manager (FirebaseCore, FirebaseAnalytics,
FirebaseCrashlytics, FirebaseFirestore) directly in `project.pbxproj`. No Podfile needed —
Xcode resolves packages automatically on open. `GoogleService-Info.plist` is gitignored; real
file must be provided locally. `IosTracker` now mirrors `AndroidTracker` using `FirebaseAnalytics`.*

---

### ~~6.2 — Image Save / Share on iOS~~ ✅

**Goal:** make image save and share useful from the iOS gallery.

**Work:**
- Implement `IosImageOperations.saveImage`.
- Request add-only Photos permission.
- Add `NSPhotoLibraryAddUsageDescription`.
- Implement `IosImageOperations.shareImage` with a native share sheet.
- Handle iPad popover source configuration.

**Definition of Done:**
- ✅ Save writes the current photo to the iOS Photos app.
- ✅ Share opens the native iOS share sheet from `ImagesScreen`.

*Note: `saveImage` downloads via Ktor Darwin, writes to a POSIX temp file, and saves via
`PHPhotoLibrary.performChanges` + `PHAssetCreationRequest.addResourceWithType(fileURL:)`. Add-only
permission is requested at runtime (iOS 14+ `PHAccessLevelAddOnly`). `shareImage` presents
`UIActivityViewController` with share text + image URL; `modalPresentationStyle` is set to
`UIModalPresentationFormSheet` to handle iPad without a popover anchor — Kotlin/Native does not
bridge the `UIViewController(UIPopoverController)` ObjC category that holds
`popoverPresentationController`. For a proper popover anchor, that category must be bridged in a
follow-up (or access it via ObjC runtime).*

---

### ~~6.3 — Xcode Project Bootstrap~~ ✅

**Goal:** make the iOS app buildable by a teammate without manual project setup.

**Work:**
- Check in an iOS Xcode project/workspace.
- Wire the shared framework build step.
- Use Swift Package Manager for all iOS dependencies.

**Definition of Done:**
- ✅ `iosApp/iosApp.xcodeproj` checked in with a shared scheme.
- ✅ "Build KMP Framework" run script phase calls `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` before compile.
- ✅ `FRAMEWORK_SEARCH_PATHS` points to the Gradle output directory.
- ✅ `shared.framework` linked and embedded.
- ✅ `GoogleService-Info.template.plist` checked in as shape reference; real file is gitignored.
- ✅ `.gitignore` updated: `xcuserdata/`, `GoogleService-Info.plist` excluded.
- ✅ `iosApp/README.md` updated with quick-start steps (SPM, no pod install).

---

### ~~6.4 — iOS Deep Links~~ ✅

**Goal:** match Android deep-link behavior on iOS.

**Work:**
- Add `marsrover://` URL scheme.
- Add associated domains for universal links if the required AASA file is hosted.
- Parse incoming Swift URLs into shared `DeepLink`.
- Pass the pending deep link into `MainViewController`.

**Definition of Done:**
- ✅ Opening `marsrover://rover/5` on iOS navigates to Curiosity photos.
- Universal links work once domain setup is available.

*Note: `marsrover://` URL scheme registered in `Info.plist`. `MarsRoverApp.swift` forwards
URLs to `pushDeepLink(urlString:)` (exported from `Main.ios.kt`), which parses the path in
Kotlin and feeds the result into a `MutableStateFlow<DeepLink?>` that `MainViewController`
collects via `collectAsState()` — no UIViewController recreation needed. Universal links
deferred until the AASA file is hosted on the domain.*

---

---

### Edge-to-Edge ✅

**Goal:** Make the app truly edge-to-edge so content draws behind system bars.

**Done:**
- ✅ `window.isNavigationBarContrastEnforced = false` in `MainActivity` (API 29+) — prevents unwanted
  translucent scrim over the nav bar when using M3 `NavigationBar`.
- ✅ `android:windowSoftInputMode="adjustResize"` added to `MainActivity` in the manifest.
- ✅ `AppNavigation`: when on `AppDestination.Images`, status-bar insets are removed from the root
  Column and the bottom chrome (AdSlot + MarsBottomBar) is hidden, giving `ImagesScreen` the full window.
- ✅ `ImagesScreen`: `TopAppBar` now uses its default window insets (handles status bar itself);
  overlay controls Box uses `navigationBarsPadding()` so the favorite toggle stays above the nav bar;
  all `MarsSnackbar` instances in `OnEvent` also use `navigationBarsPadding()`.
- ✅ `MaterialSymbolIcon`: removed `PlatformTextStyle(includeFontPadding = false)` which was
  Android-only and broke the iOS framework compile; `platformStyle = null` leaves the Box-centered
  glyph layout from the prior commit intact.

*Note: The Box-wrapping approach for `MarsImageFavoriteToggle` and `MarsSnackbar` ensures images
extend behind the nav bar while interactive controls remain tappable above it.*

---

### ~~Ticket S11 — Restore AdMob banner (Android + iOS)~~ ✅

**Goal:** restore the legacy AdMob banner that was dropped during migration.
The shared `AdSlot` was a placeholder `Box(modifier)` on every platform; this
ticket lights up Android and iOS while keeping Desktop a no-op.

**Done (Android):**
- ✅ `shared/src/androidMain/.../AdSlot.android.kt` renders an adaptive AdMob
  banner via `AndroidView`; lifecycle pause/resume/destroy is handled in a
  `DisposableEffect` against `LocalLifecycleOwner`.
- ✅ `play-services-ads` added to `shared/build.gradle.kts` `androidMain`
  dependencies.
- ✅ `MobileAds.initialize(...)` called from `MarsRoverApplication.onCreate`
  inside a try/catch.
- ✅ `android.adservices.AD_SERVICES_CONFIG` `<property>` + matching
  `res/xml/custom_ad_services_config.xml` added to androidApp.
- ✅ NPA gating: `AndroidAdConsent.personalizedAds` in
  `shared/src/androidMain/.../platform/AndroidAdConsent.kt` mirrors
  `GdprHelper.canRequestAds()`; `AdSlot.android.kt` appends an
  `AdMobAdapter` extras bundle with `npa=1` when consent is denied and
  reloads the ad when the consent state flips.

**Done (iOS):**
- ✅ `GoogleMobileAds` + `UserMessagingPlatform` added via SPM in
  `iosApp/iosApp.xcodeproj/project.pbxproj`.
- ✅ `GADApplicationIdentifier` + `SKAdNetworkItems` added to
  `iosApp/iosApp/Info.plist` (using Google's universal test app ID until
  the real iOS AdMob app is provisioned).
- ✅ `shared/src/iosMain/.../platform/IosAdSlot.kt` — Kotlin
  `AdBannerFactory` interface and an `IosAdSlot.factory` registry.
- ✅ `shared/src/iosMain/.../presentation/ui/AdSlot.ios.kt` — `BoxWithConstraints`
  + `UIKitView` rendering the registered factory; falls back to an empty
  `Box(modifier)` when the factory hasn't been installed yet (e.g. before
  UMP/ATT/MobileAds bootstrap finishes).
- ✅ `iosApp/iosApp/BannerAdFactory.swift` builds an anchored adaptive
  `GADBannerView` with the test banner unit and a topmost-VC lookup that
  matches the existing pattern in `IosImageOperations`.
- ✅ `iosApp/iosApp/MarsRoverApp.swift` chains UMP → ATT → `GADMobileAds.start`
  → registers `BannerAdFactoryImpl()` into `IosAdSlot.shared.factory`. The
  chain is deferred 1s after launch to satisfy Apple's HIG for ATT timing.

*Note: iOS `GADRequest` does not have an NPA-bundle equivalent — Google's
Mobile Ads SDK reads consent from UMP's stored state automatically. As long
as UMP runs to completion before `GADMobileAds.start(...)`, banner requests
honor the user's choice.*

*Note: production AdMob unit IDs for iOS are not yet provisioned. The
checked-in values are Google's published test IDs and must be swapped before
release (see Followups).*

**Followups from S11 (carry-overs, not blockers):**

Production AdMob configuration — required before App Store release:
- ✅ Swapped iOS `GADApplicationIdentifier` in `iosApp/iosApp/Info.plist` to real app ID
      (`ca-app-pub-7516059448019339~1086903880`).
- ✅ Swapped `BannerAdFactory.swift` to real banner unit IDs: List (`7887281803`),
      Content (`6993836680`), Image gallery (`9920068688`).
- [ ] Expand `SKAdNetworkItems` in `Info.plist` from the single
      representative ID to Google's full recommended list (~75 entries) for
      iOS 14.5+ attribution.

Privacy settings re-entry point (AdMob policy in some regions):
- [ ] Add a "Privacy settings" row in the About screen that re-presents the
      consent form.
      - Android: `consentInformation.resetConsent()` + `GdprHelper.init()`.
      - iOS: `UMPConsentForm.presentPrivacyOptionsForm(from:completionHandler:)`.

iOS universal links (deferred — blocked on domain ops):
- [ ] Host an AASA file on `https://marsroverphotos.app/.well-known/` so
      `https://marsroverphotos.app/rover/{id}` and `/photo/{id}` work on
      iOS. Kotlin side is already wired; blocked on domain ops only.

Optional polish:
- [ ] Lift `MobileAds.initialize` + UMP bootstrap into a shared
      `AdsBootstrap` abstraction in commonMain — would let the platforms
      call one shared function instead of each holding its own init path.
- [ ] `UIKitView` used in `AdSlot.ios.kt` is deprecated in Compose
      Multiplatform 1.11; migrate to the newer overload next time we touch
      the file.
- [ ] Pre-existing detekt warnings on `MainActivity.handleDeepLink`
      (cyclomatic complexity 18 > 15, nested-block depth) — unrelated to
      ads but lives in the same file we edited.

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
├── :androidApp  Android shell: Activity, manifest, app icons, widget, GDPR
├── :desktopApp  Desktop shell
└── iosApp/      Swift/iOS shell, Xcode project
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
