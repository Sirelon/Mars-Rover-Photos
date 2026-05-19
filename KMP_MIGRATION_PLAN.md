# Mars Rover Photos — Android + iOS KMP Migration Plan

> Living document. Goal: ship the app on both Android and iOS off a single shared codebase,
> with Desktop kept building as a side-benefit. Web/WASM stays deferred (see `WASM_WEB_SUPPORT.md`).

---

## 1. Executive summary

The KMP scaffolding is **already in place**: shared module, Android shell (`androidApp/`),
desktop shell (`desktopApp/`), iOS Swift shell (`iosApp/`), Koin DI, Room KMP, Ktor, Navigation 3,
Compose Multiplatform 1.10.3, Kotlin 2.3.21 (see §2.1 for the full version snapshot).

What is **not done yet**: every real feature screen still lives in the legacy `app/` module
(commented out in `settings.gradle.kts`). `shared/src/commonMain/.../presentation/screens/PlaceholderScreens.kt`
is a stub set wired into Koin/Navigation 3 — running the app on any platform today shows seven
"(Screen pending migration)" placeholders.

The migration is therefore **mostly a UI port + cross-cutting-abstraction job**, screen by screen,
plus filling in three iOS implementation gaps (Firebase, photo library save/share, Xcode project).

Rough size: ~4,800 LOC of Kotlin UI to move (across 25 feature files), the heaviest being
`RoversActivity.kt` (656 lines), `RoverMissionInfoScreen.kt` (514), `ImagesScreen.kt` (401),
`RoverPhotosScreen.kt` (456), `AboutAppScreen.kt` (244), `FavoriteScreen.kt` (230), `Ukraine.kt` (159).

---

## 2. Current module layout

```
settings.gradle.kts
├── :shared        (kotlin-multiplatform + android.library + CMP) — common code, all platforms
├── :androidApp    (android.application)                          — Android shell, Activity, manifest, AdMob, widgets
├── :desktopApp    (kotlin-jvm + CMP)                             — Desktop shell, Window {} + App()
├── (:iosApp)      Swift + Xcode (no .xcodeproj checked in yet)   — wraps shared.framework
└── (:app)         legacy Android-only module — commented out, kept as reference
```

Target source sets in `:shared`:
- `commonMain` — domain, data, presentation (Compose), DI scaffolding, expects.
- `androidMain` — Android actuals + Firebase Android SDK + Ktor OkHttp + Glance-free helpers.
- `iosMain` — iOS actuals (NSUserDefaults, Documents dir, Ktor Darwin) — Firebase + photo I/O are **stubs**.
- `desktopMain` — JVM actuals (Java Prefs, file system, Ktor OkHttp, Swing dispatcher) — Firebase is a stub.

Targets declared in `shared/build.gradle.kts`: `androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64`, `jvm("desktop")`. WASM block commented out.

iOS framework output (after `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`):
`shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework` (Debug ≈ 240 MB —
Kotlin/Native runtime + Compose runtime + transitive deps).

### 2.1 Tech stack snapshot

Pulled from `gradle/libs.versions.toml` (verify there for ground truth):

| Layer | Component | Version |
|---|---|---|
| Compiler | Kotlin | 2.3.21 |
| Compiler | KSP | 2.3.6 |
| Build | Android Gradle Plugin | 9.2.0 |
| UI | Compose Multiplatform | 1.10.3 |
| UI | Compose BOM (Android) | 1.11.0 |
| UI | Material3 (Android) | 1.4.0 |
| Data | Room (KMP stable) | 2.8.4 |
| Data | Paging (Android + KMP) | 3.4.2 |
| Data | Ktor | 3.4.3 |
| DI | Koin / Koin Compose | 4.2.1 |
| Logging | Kermit | 2.1.0 |
| Image loading | Coil (KMP) | 3.4.0 |
| Lang | kotlinx-serialization | 1.10.0 |
| Lang | kotlinx-coroutines | 1.10.2 |
| Lang | kotlinx-datetime | 0.7.1 |
| Lang | kotlinx-collections-immutable | 0.4.0 |
| Lifecycle | AndroidX Lifecycle (KMP) | 2.10.0 |
| Android-only | Glance | 1.2.0-rc01 |
| Android-only | WorkManager | 2.11.0 |
| Android-only | Core Splashscreen | 1.2.0-beta02 |

**Known caveat baked into the build (`gradle.properties`):**
`android.builtInKotlin=false` and `android.newDsl=false` to keep the shared `kotlin.multiplatform + android.library` combo working under AGP 9.x. The proper fix is to migrate `:shared` to `com.android.kotlin.multiplatform.library` — tracked separately, not blocking screen ports. See §9.

**Room caveat:** `room-paging` is Android-only AAR — already isolated to
`androidMain.dependencies` in `shared/build.gradle.kts`. DAO `PagingSource` methods only
compile on Android; common code uses `Flow<List<T>>` instead. The Paging *Compose* layer
(`collectAsLazyPagingItems`) is multiplatform via Paging 3.4.2 — see ticket S0 / §4.5.

---

## 3. Inventory — what's already in `shared/`

### 3.1 Already migrated (works in `commonMain`)

**Domain (`domain/`)**
- Models: `Rover` (`expect class` w/ Android `drawableRes()`), `MarsPhoto`, `RoverCamera`,
  `PhotosQueryRequest`, `EducationalFact`, `FirebasePhoto`, `RoverConstants`,
  `mission/CameraSpec`, `mission/RoverMissionData`, `mission/RoverMissionFacts`.
- Repositories (interfaces): `RoversRepository`, `PhotosRepository`, `ImagesRepository`,
  `FactsRepository`, `MissionRepository`.
- Settings: `AppSettings`, `Theme`.

**Data (`data/`)**
- Database: `AppDataBase` (Room KMP, `@ConstructedBy(AppDataBaseConstructor)`, migrations 7→8 and 8→9),
  entities `MarsImage`, `FactDisplay`, plus `Rover` entity from domain.
- DAOs: `RoverDao`, `ImagesDao`, `FactDisplayDao`.
- Network: `RestApi`, `NasaApi` (Ktor), `models/RoverResponse`, `models/PhotosResponse`, `Mappers`.
- Repositories: `RoversRepositoryImpl`, `PhotosRepositoryImpl`, `ImagesRepositoryImpl`,
  `FactsRepositoryImpl`; `MissionRepositoryImpl` is `expect class` (Android has Firestore impl,
  iOS/desktop are stubs).
- Paging: `PopularRemoteMediator` (`androidx.paging.RemoteMediator` from Paging KMP).

**Presentation (`presentation/`)**
- Entry: `App.kt` (theme + Surface + AppNavigation), `navigation/AppNavigation.kt` (Navigation 3,
  Koin `koinEntryProvider`), `navigation/AppDestinations.kt` (`@Serializable sealed interface AppDestination`
  with Rovers/Photos/Images/Favorite/Popular/Mission/About), `navigation/AppNavigator.kt`,
  `navigation/AppNavEntryDecorators.kt`, `navigation/DeepLink.kt`.
- ViewModels: `PhotosViewModel`, `ImageViewModel`, `FavoriteImagesViewModel`,
  `RoverMissionInfoViewModel`, `PopularPhotosViewModel` (all on `androidx.lifecycle.ViewModel`,
  registered via `viewModelOf(::…)`).
- Theme: `theme/Theme.kt` (`MarsRoverPhotosTheme` is an expect/actual composable;
  Android pulls dynamic colors).
- Shared UI helpers in `presentation/ui/`: `MaterialSymbolIcon`, `MarsSnackbar`,
  `RadioButtonText`, `NoScrollEffect` (expect), `PlatformDatePickerDialog`,
  `PlatformToastHost`/`rememberPlatformToastState`, `PlatformUriHandler` (expect),
  `CenteredComponents`, `AdaptiveLayout` (expect), `DynamicColor` (expect), `SystemTheme` (expect).
- Models: `GridItem`, `GridItemTransformer`.
- Screens: `screens/PlaceholderScreens.kt` — **all seven are stubs**.

**Platform abstractions (`platform/`)** — expects in `commonMain`, actuals per platform
| Capability | expect | Android actual | iOS actual | Desktop actual |
|---|---|---|---|---|
| HTTP engine | `createHttpClientEngine()` | OkHttp | Darwin | OkHttp |
| Room DB builder | `getDatabaseBuilder()` | filesDir | Documents dir | user.home |
| Preferences | `PlatformPreferences` | SharedPreferences | NSUserDefaults | java.util.prefs |
| Firebase Analytics | `class FirebaseAnalytics` | Firebase Android SDK | `println` stub | `println` stub |
| Firestore | `IFirebasePhotos` | `AndroidFirebasePhotos` | **stub returning empty** | stub |
| Image save/share | `ImageOperations` | MediaStore + Share Intent | **stub returns Error** | not implemented |
| Theme bits | `supportsDynamicColor`, `isSystemInDarkTheme` | Material-You | static false | static system |

**Koin DI (`di/`)** — `KoinInit.initKoin(platformModule, …)` wires `databaseModule`, `networkModule`,
`repositoryModule`, `viewModelModule`, `navigationModule`. `navigationModule` already maps every
`AppDestination` to its placeholder composable through `koinEntryProvider<NavKey>()`.

**Compose Resources** — `shared/src/commonMain/composeResources/` has only `font/material_symbols_outlined.ttf`
today. **All strings and drawables are still under `app/src/main/res/`** and must be ported.

### 3.2 Still **only** in the legacy `app/` module

| Path | Lines | Status |
|---|---:|---|
| `feature/rovers/RoversActivity.kt` | 656 | Root screen + NavigationSuiteScaffold + AdMob banner + Ukraine banner + edge-to-edge |
| `feature/rovers/RoversNavigation.kt` | 131 | Sealed `RoversDestination`, `RoversNavigationState`, in-screen Nav3 wiring |
| `feature/mission/RoverMissionInfoScreen.kt` | 514 | Mission detail UI, NumberFormat, drawables |
| `feature/photos/RoverPhotosScreen.kt` | 456 | Sol/Earth-date pickers, grid/list view, facts mixed in |
| `feature/images/ImagesScreen.kt` | 401 | Fullscreen pager + zoom + share + save |
| `feature/settings/AboutAppScreen.kt` | 244 | About/settings, theme switcher, clear cache, rate-us |
| `feature/favorite/FavoriteScreen.kt` | 230 | Favorites grid with Paging Compose |
| `feature/photos/PhotosViewModel.kt` | 197 | **Already migrated** (shared copy is more complete) |
| `feature/mission/RoverMissionInfoViewModel.kt` | 197 | Already migrated |
| `feature/mission/RoverMissionData.kt` | 183 | Already migrated |
| `feature/images/MarsImageFavoriteToggle`, `feature/MarsImage.kt` | 178 | `MarsImageComposable`, `PhotoStats`, `MarsImageFavoriteToggle`, `NetworkImage` — **shared reusable, not yet ported** |
| `feature/ukraine/Ukraine.kt` | 159 | `UkraineInfoScreen`, `UkraineBanner` |
| `feature/images/PhotoInfoBottomSheet.kt` | 156 | Photo info bottom sheet |
| `feature/photos/FactCard.kt`, `EmptyPhotos.kt`, `Mapper.kt`, etc. | ~290 | Helper composables not yet ported |
| `feature/popular/PopularPhotoDataSource.kt` | 77 | Already migrated as `data/paging/PopularRemoteMediator` |
| `feature/popular/PopularPhotosViewModel.kt` | 28 | Already migrated; **but `PopularPhotosScreen` was never extracted** — it's inlined inside FavoriteScreen body in legacy |
| `feature/favorite/FavoriteImagesViewModel.kt` | 37 | `AndroidViewModel` legacy; shared has a clean `ViewModel` version |
| `feature/gdpr/GdprHelper.kt` | 75 | Android-only by design |
| `feature/firebase/*`, `firebase/photos/*`, `firebase/mission/*`, `firebase/facts/*` | ~250 | Mostly Android-only Firebase admin/upload tooling |
| `widget/*` (4 files) | ~330 | Glance widget — Android-only by design |
| `tracker/*` (5 files) | ~250 | `ITracker`, `FirebaseTracker`, `AnalyticsUtils`, `DataFlowCollector`, `FullscreenImageTracker`. **Not yet ported** as a common abstraction |
| `DataManager.kt`, `RoverApplication.kt`, `extensions/Extensions.kt`, `ui/Theme.kt`, `ui/widgets.kt`, `ui/AdaptiveLayout.kt`, `ui/MaterialSymbolIcon.kt`, `ui/centered.kt` | mixed | Reference only; new code already in `shared/.../presentation/ui` and `androidApp/` |

Resources still in `app/src/main/res/`:
- `drawable/ic_rovers.xml`, `drawable-*/alien_icon`, `drawable-nodpi/img_placeholder`,
  `drawable-*/mars*` per-rover images.
- `values/strings.xml` (rovers/photos/favorite/popular/about + all CTAs).
- `font/material_symbols_outlined.ttf` (already duplicated into `shared/composeResources`).

### 3.3 Still only in `androidApp/`

`androidApp/src/main/kotlin/com/sirelon/marsroverphotos/MainActivity.kt`,
`MarsRoverApplication.kt`, `AndroidManifest.xml`, app icons, theme XML. These are
**already a thin wrapper**: Activity calls `App(deepLink, onDeepLinkConsumed)` from
`commonMain`. Deep links (`marsrover://rover/{id}`, `marsrover://photo/{id}`, and
`https://marsroverphotos.app/...`) parsed in `MainActivity.handleDeepLink` and pushed
into `App` as `DeepLink`. This is the model to keep — Android remains responsible
for Intent → `DeepLink` translation.

### 3.4 What's in `iosApp/` today

- `iosApp/iosApp/MarsRoverApp.swift` — `KoinInitKt.initKoinIos()` in `init`, then `WindowGroup { ContentView() }`.
- `iosApp/iosApp/ContentView.swift` — `UIViewControllerRepresentable` → `Main_iosKt.MainViewController()`.
- `iosApp/Podfile` — empty placeholder, no Firebase pods installed.
- `iosApp/Info.plist` — present but not bound to a real Xcode project.
- **No `.xcodeproj` / `.xcworkspace` checked in.** `iosApp/SETUP.md` walks the user through
  creating the project manually in Xcode. This needs to be checked in before iOS can be
  built on CI / by anyone but the author.

---

## 4. Common abstractions still missing

These must land **before or alongside** screen ports because most screens depend on them.
Putting them up first lets every screen migration be small and self-contained.

### 4.1 Compose Resources port (BLOCKER for any screen)

Move into `shared/src/commonMain/composeResources/`:
- `values/strings.xml` → `values/strings.xml` (CMP picks up the same XML format).
- `drawable/ic_rovers.xml` → `drawable/ic_rovers.xml` (vector XML is CMP-compatible).
- All rover/landing JPEGs and `alien_icon`, `img_placeholder`, `mars-bg` to `drawable/`.

After porting, replace every `R.string.x` / `R.drawable.x` with
`org.jetbrains.compose.resources.stringResource(Res.string.x)` /
`painterResource(Res.drawable.x)`. The `packageOfResClass` is already
`com.sirelon.marsroverphotos.shared.resources`.

### 4.2 Tracker abstraction (BLOCKER for Rovers/Favorite/Popular/Ukraine/About)

Today every screen calls `RoverApplication.APP.dataManger.trackClick(...)` and
`RoverApplication.APP.tracker.trackFavorite(...)`. The legacy `ITracker` takes
`android.os.Bundle`, which can't move as-is.

Add to `shared/.../platform/Tracker.kt`:

```kotlin
interface Tracker {
    fun trackClick(event: String)
    fun trackEvent(event: String, params: Map<String, String> = emptyMap())
    fun trackFavorite(photo: MarsImage, from: String, fav: Boolean)
    fun trackSeen(photo: MarsImage)
    fun trackScale(photo: MarsImage)
    fun trackSave(photo: MarsImage)
    fun trackShare(photo: MarsImage, packageName: String?)
}
```

Provide actuals: `AndroidTracker` (Firebase Analytics + DataManager merged),
`IosTracker` (logs only until Firebase iOS lands), `DesktopTracker` (logs only).
Bind via `platformModule`. Inject into ViewModels — strip the `RoverApplication.APP` references.

### 4.3 Build/version info expect

```kotlin
// commonMain
expect object BuildInfo {
    val versionName: String
    val isDebug: Boolean
    val packageName: String
}
```

- Android actual reads `BuildConfig` from `androidApp` (small `androidApp/src/main/kotlin/.../BuildInfo.android.kt`,
  but since `BuildConfig` is in androidApp, the actual must live in `:shared` `androidMain` as a `expect val`
  that's overridden by a generated constant — simplest is `BuildKonfig` plugin
  (`com.codingfeline.buildkonfig`) wired in `:shared`.
- iOS/desktop actuals: read from `Bundle.main.infoDictionary` / system properties (or hard-code in BuildKonfig).

### 4.4 Crashlytics expect

```kotlin
expect fun recordException(t: Throwable)
```

Android wires to `FirebaseCrashlytics.getInstance().recordException`; iOS/desktop log via Kermit.

### 4.5 Paging Compose multiplatform

`FavoriteScreen` and `PopularScreen` need `collectAsLazyPagingItems()` in `commonMain`.
Paging KMP 3.4.2 ships `androidx.paging.compose` as a multiplatform artifact —
add to `shared/build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(libs.paging.common)
    implementation(libs.paging.compose) // androidx.paging:paging-compose:3.4.2 (multiplatform)
}
```

(`libs.versions.toml` already pins `paging-kmp = "3.4.2"`. If the multiplatform Compose
adapter is still missing on iOS in your alignment, fall back to
`app.cash.paging:paging-compose-common`.)

### 4.6 Zoomable replacement (BLOCKER for Image gallery)

Legacy uses `net.engawapg.lib.zoomable` — JVM-only artifact. Options, ranked:
1. **Custom modifier with `Modifier.graphicsLayer` + `pointerInput(detectTransformGestures)`** —
   ~60 lines, fully in `commonMain`. Recommended.
2. `me.saket.telephoto:zoomable` — Android-only, would need expect/actual.
3. KMP fork (`com.diamondedge:compose-zoomable`, `pro.respawn:apiresult-zoomable`) — small ecosystem, evaluate stability.

Recommendation: ship our own `Modifier.zoomable(state)` in `presentation/ui/` since the
feature set we use is small (pan + double-tap reset + scale).

### 4.7 Open-saved-image expect

`ImagesScreen` opens the saved file with `Intent.ACTION_VIEW`. Add:

```kotlin
expect fun openLocalImage(uri: String): Boolean
```

Android → `Intent(Intent.ACTION_VIEW, Uri.parse(uri)) | FLAG_ACTIVITY_NEW_TASK`.
iOS → can present a preview via `UIDocumentInteractionController`; for v1, log and return false.
Desktop → `Desktop.getDesktop().open(File(uri))`.

### 4.8 "Rate the app" expect

```kotlin
expect fun openAppStore()
```

Android → `market://details?id=…` then fallback to Play Store https URL.
iOS → `itms-apps://apps.apple.com/app/idXXX`.
Desktop → no-op (or open the GitHub repo).

### 4.9 NumberFormat / locale formatting

`RoverMissionInfoScreen` uses `java.text.NumberFormat.getNumberInstance(Locale.US)`.
Replace with a tiny common util — kotlinx-datetime gives `LocalDate`; for thousands
separators just use a manual formatter or expect `formatThousands(Long): String`.

### 4.10 Android-only kept in `androidApp/`

Stays in `androidApp/` (not migrated):
- AdMob (`MobileAds`, `AdView`, `AdRequest`) — wrap entry from common via expect `AdSlot(modifier)`
  composable; Android renders the banner, iOS/desktop return `Box(modifier)`.
- `GdprHelper` (UMP) — same expect-`Composable` strategy.
- Glance widget (`widget/*`) — App Widget. Stays Android-only.
- Firebase admin uploaders (`firebase/mission/MissionDataUploader`, `firebase/facts/EducationalFactsUploader`) —
  developer tools, keep in `androidApp/` under a debug flag.
- Splashscreen (`installSplashScreen`) — Android API 12 splash; stays in `MainActivity`.
- Edge-to-edge wiring — kept in `MainActivity`, with the common `SystemTheme` expect
  exposing dark-mode signal to common.

---

## 5. Screen-by-screen migration tickets

Order follows `AGENTS.md`'s "screen migration order" rule and goes smallest-blast-radius first.
Each ticket lists files to port, Android-only blockers to swap, DI/expect work, and Definition of Done.

### ~~Ticket S0 — Cross-cutting prerequisites (do FIRST)~~ ✅

Land everything in §4 that the first screen will touch. Concretely:
- ✅ Compose Resources port: `strings.xml` + `ic_rovers`, `alien_icon`, `img_placeholder`, and rover JPEGs → `shared/src/commonMain/composeResources/`.
- ✅ `Tracker` interface + Android/iOS/desktop actuals + Koin bindings.
- ✅ `BuildInfo` hand-rolled singleton initialized from each platform entry point.
- ✅ `recordException` expect/actual (Crashlytics on Android, Kermit on iOS/desktop).
- ✅ Add `androidx.paging:paging-compose:3.4.2` to `commonMain` deps (raw coordinate — TOML alias `paging-compose-common` maps to a non-existent artifact).
- ✅ Ship `Modifier.zoomable` in `presentation/ui/Zoomable.kt`.
- ✅ Port `feature/MarsImage.kt` content (`MarsImageComposable`, `PhotoStats`, `MarsImageFavoriteToggle`, `NetworkImage`) into `presentation/ui/MarsImage.kt`.
- ✅ `AdSlot` expect composable added (Box stub on all platforms; real AdMob stays in androidApp).

**DoD:** ✅ App still builds on Android, iOS framework still links, desktop runs;
all placeholder screens still render; new abstractions are wired through Koin.

*Note: `paging-compose-common` artifact doesn't exist on Maven Central — the correct KMP artifact is `androidx.paging:paging-compose:3.4.2` (added as raw coordinate).*

---

### Ticket S1 — Rovers home

**Goal:** real list of rovers in `commonMain`, replacing `RoversScreen` placeholder.

**Files to port (source → destination):**
- `app/.../feature/rovers/RoversActivity.kt` (parts only) →
  `shared/.../presentation/screens/RoversScreen.kt`. **Only** the `RoversContent` /
  `RoverItem` composables move here; the `RoversActivity` shell stays Android-only.
- `app/.../models/Rover.kt::drawableRes(Context)` →
  replace with `Rover.painter(): Painter` in common, looking up by `id` in a `when {}`
  that returns CMP `Res.drawable.*`.

**Android-only blockers and replacements:**
| Blocker | Replacement |
|---|---|
| `FragmentActivity` + edge-to-edge + window-insets | Already handled by `MainActivity` + common theme. Don't move. |
| `painterResource(R.drawable.ic_rovers)` | CMP `painterResource(Res.drawable.ic_rovers)` |
| `stringResource(R.string.nav_*)` | CMP `stringResource(Res.string.nav_*)` |
| `RoverApplication.APP.dataManger.trackClick("…")` | Inject `Tracker` and call `tracker.trackClick("…")` |
| AdMob / `GdprHelper` | Keep in `androidApp/`. Expose `AdSlot()` expect composable in `presentation/ui/`; iOS/desktop actuals render `Box`. |
| `calculateWindowSizeClass(activity)` | `currentWindowAdaptiveInfo()` (CMP) — already imported in legacy file. |
| `Timber.d(...)` | `Logger.d(...)` (Kermit, already in shared). |

**ViewModel:** new `RoversViewModel(roversRepository, tracker)` in `presentation/viewmodels`,
exposing `StateFlow<List<Rover>>` via `roversRepository.loadAllRovers()`. Bind in
`viewModelModule`.

**DoD:**
- `RoversScreen(onNavigateToPhotos, onMissionInfoClick)` is fully in `commonMain`,
  shows real rovers on Android + iOS simulator + desktop.
- Bottom-nav (Rovers / Favorite / Popular / About) still works on Android.
- Navigation suite logic moves to `commonMain` as `MarsBottomBar` (it's CMP-compatible)
  with `AdSlot` rendered only on Android.

---

### Ticket S2 — Rover photos (grid)

**Files to port:**
- `feature/photos/RoverPhotosScreen.kt` →
  `presentation/screens/PhotosScreen.kt`.
- `feature/photos/EmptyPhotos.kt`, `feature/photos/FactCard.kt`, `feature/photos/Mapper.kt`
  (anything still Android-only).

**Already migrated:** `PhotosViewModel`, `GridItemTransformer`, `GridItem`, `RoverDateUtil`,
`PhotosRepository`.

**Blockers:**
| Blocker | Replacement |
|---|---|
| `R.string.*` | CMP resources |
| `java.util.Calendar` / `TimeZone` for date math | kotlinx-datetime — already used elsewhere in shared. |
| `androidx.compose.material3.DatePicker` directly | Use `PlatformDatePickerDialog` (already in `presentation/ui`). |
| `rememberSaveable { mutableStateOf(false) }` | OK in CMP — keep. |

**DoD:** `PhotosScreen(roverId, …)` shows real photos for the rover, with the sol slider
dialog and earth-date picker working on both platforms.

---

### Ticket S3 — Image gallery + photo info sheet

**Files:**
- `feature/images/ImagesScreen.kt` → `presentation/screens/ImagesScreen.kt`.
- `feature/images/PhotoInfoBottomSheet.kt` → `presentation/screens/PhotoInfoBottomSheet.kt`.

**Already migrated:** `ImageViewModel`, `ImageOperations` (interface), `MarsImage` entity.

**Blockers:**
| Blocker | Replacement |
|---|---|
| `net.engawapg.lib.zoomable` (`rememberZoomState`, `Modifier.zoomable`) | Custom `Modifier.zoomable(state)` shipped in S0 |
| `BuildConfig.DEBUG` | `BuildInfo.isDebug` (S0) |
| `Intent.ACTION_VIEW` to open saved image | `openLocalImage(uri)` expect (S0) |
| `LocalHapticFeedback` | Works in CMP — keep |

**iOS gap unblocked here:** `IosImageOperations.saveImage` / `shareImage` must be
implemented before this screen is useful on iOS — see §6.2. Until then the screen still
renders and pages through images on iOS, but Save/Share show an error snackbar.

**DoD:** Fullscreen swipeable gallery with zoom and tap-to-toggle UI works on both
platforms. Share works on Android via `UIActivityViewController` on iOS (after §6.2).

---

### Ticket S4 — Favorites

**Files:** `feature/favorite/FavoriteScreen.kt` → `presentation/screens/FavoriteScreen.kt`.

**Already migrated:** `FavoriteImagesViewModel` (clean common ViewModel exists alongside
the legacy `AndroidViewModel` variant — discard the legacy one).

**Blockers:**
| Blocker | Replacement |
|---|---|
| `androidx.paging.compose.LazyPagingItems` / `collectAsLazyPagingItems()` | Paging Compose multiplatform (S0) |
| `R.string.*`, `R.drawable.*` | CMP resources |
| `java.util.UUID` | `kotlin.uuid.Uuid.random()` (Kotlin 2.0+) |
| `Prefs` direct access | `AppSettings` via Koin |

**Reuse:** `MarsImageComposable` ported in S0.

**DoD:** Favorites grid loads from Room on both platforms with proper Paging Compose.

---

### Ticket S5 — Popular photos

**Files:** extract a `PopularScreen` composable (it's currently implemented inline in legacy
through the same scaffolding `FavoriteScreen` uses) → `presentation/screens/PopularScreen.kt`.

**Already migrated:** `PopularPhotosViewModel`, `PopularRemoteMediator`, `IFirebasePhotos` (Android impl real, iOS stub).

**Caveats:**
- On iOS the Firestore stub returns an empty list — Popular tab will be empty until §6.1 lands.
- Same Paging Compose multiplatform dependency as S4.

**DoD:** Popular tab renders the staggered grid on Android with real data; on iOS empty-state
shows until Firebase iOS SDK is wired.

---

### Ticket S6 — Mission info

**Files:** `feature/mission/RoverMissionInfoScreen.kt` →
`presentation/screens/MissionInfoScreen.kt`.

**Already migrated:** `RoverMissionInfoViewModel`, `RoverMissionData`, `CameraSpec`,
`MissionInfoUtils`.

**Blockers:**
| Blocker | Replacement |
|---|---|
| `LocalContext.current` to resolve `rover.drawableRes(context)` | `Rover.painter()` (added in S1) |
| `java.text.NumberFormat` | Tiny common `formatThousands(Long)` helper |
| `coil3.compose.AsyncImage` | Already CMP-compatible. Keep. |

**DoD:** Mission info screen renders the rover header, mission stats, and camera grid
on both platforms.

---

### Ticket S7 — About / settings

**Files:** `feature/settings/AboutAppScreen.kt` →
`presentation/screens/AboutScreen.kt`.

**Blockers:**
| Blocker | Replacement |
|---|---|
| `BuildConfig.VERSION_NAME` | `BuildInfo.versionName` (S0) |
| `R.drawable.alien_icon`, `R.string.about_description` | CMP resources |
| `Prefs.themeLiveData` | `AppSettings.themeFlow` |
| `Calendar` for "Year ${year}" copyright | kotlinx-datetime |
| "Clear cache" hook that touches Coil's disk cache + Android `Formatter.formatFileSize` | Inject a `CacheManager` interface; Android impl uses `coil3.ImageLoader(context).diskCache`; iOS/desktop use Coil 3 KMP cache APIs (Coil 3 disk cache is multiplatform). Format bytes manually. |

**DoD:** Settings screen lets the user pick theme, toggle facts, clear cache, rate app
(via `openAppStore`), all on both platforms.

---

### Ticket S8 — Ukraine route decision

**Files:** `feature/ukraine/Ukraine.kt` → `presentation/screens/UkraineScreen.kt`
plus `presentation/ui/UkraineBanner.kt`.

**Blockers:**
| Blocker | Replacement |
|---|---|
| `RoverApplication.APP.dataManger.trackClick` | Injected `Tracker` |
| `recordException(e)` | `recordException` expect (S0) |
| `LocalUriHandler.current` | Use the existing CMP `LocalUriHandler` — works on all platforms. |

**Decision point:** the Ukraine banner is part of the root scaffold today. After S1, that
banner lives in `commonMain` and is shown on both platforms.

**DoD:** Ukraine banner appears under the status bar on both platforms; tapping pushes
`UkraineScreen` which contains the messaging + outbound links.

---

### Ticket S9 — Android widget (no migration, keep & adapt)

**Files stay in `androidApp/`:**
- `widget/MarsPhotoWidget.kt`, `MarsPhotoWidgetReceiver.kt`, `MarsPhotoWidgetWorker.kt`,
  `MarsPhotoWidgetConfigActivity.kt`.

**Work:**
- Replace `Prefs.*` reads with `AppSettings` injected from the shared Koin container.
- Replace direct DB access with the `ImagesRepository` interface.
- Replace `RoverApplication.APP.tracker` with the `Tracker` from S0.

**DoD:** Widget builds in `androidApp/`, talks to shared repositories, can still launch
the app via `WidgetExtraImageId` deep link.

---

## 6. iOS-specific work

iOS can build the framework today, but two flows are stubbed and the Xcode project isn't
checked in. These can run **in parallel** with the screen tickets (different files, no
conflict) — they unblock real functionality, not compilation.

### 6.1 Firebase iOS SDK

`shared/.../iosMain/.../platform/FirebasePhotosImpl.ios.kt` and `FirebaseAnalytics.ios.kt`
are no-op stubs.

**Steps:**
1. In `iosApp/Podfile`, add `pod 'FirebaseFirestore'`, `pod 'FirebaseAnalytics'`,
   `pod 'FirebaseCrashlytics'`. Run `pod install`. Commit `Podfile.lock`.
2. Add `iosApp/GoogleService-Info.plist` (not committed — keep `keystore.properties`-style
   gitignored entry; provide a template).
3. Bridge via Kotlin/Native cinterop. Two options:
   - **Direct cinterop** to the ObjC Firebase classes (`FIRApp`, `FIRFirestore`, `FIRQuery`,
     `FIRDocumentSnapshot`) — verbose but no extra layer.
   - **GitLive Firebase KMP** (`dev.gitlive:firebase-firestore`, `firebase-analytics`,
     `firebase-crashlytics`) — wraps both Android + iOS SDKs in one API. Strongly
     recommended; adopt and rewrite both `AndroidFirebasePhotos` and `IosFirebasePhotos`
     using the GitLive APIs in `commonMain` as a single class, dropping the expect/actual split.

If choosing GitLive, also rewrite `FirebaseAnalytics` and `recordException` against
`dev.gitlive:firebase-analytics` + `firebase-crashlytics`.

**DoD:** Popular tab and stat counters work on iOS. Crashlytics receives test crash from iOS.

### 6.2 Image save / share on iOS

`IosImageOperations.saveImage` / `shareImage` are stubs returning `Error`.

**saveImage:**
1. Download bytes via `NSURLSession.sharedSession.dataTaskWithRequest(...)`.
2. Wrap in `UIImage(data: …)`.
3. Request `PHPhotoLibrary.requestAuthorizationForAccessLevel(.addOnly)`.
4. `PHPhotoLibrary.sharedPhotoLibrary.performChanges { PHAssetChangeRequest.creationRequestForAssetFromImage(...) }`.
5. Add `NSPhotoLibraryAddUsageDescription` to `iosApp/iosApp/Info.plist`.

**shareImage:**
1. Build `UIActivityViewController(activityItems = [imageUrl, shareText])`.
2. Present from the topmost `UIViewController` (helper: walk `keyWindow.rootViewController`).
3. Handle iPad popover-source bug — set `popoverPresentationController.sourceView`.

**DoD:** From `ImagesScreen`, Save lands the photo in the iOS Photos app and Share opens
the native share sheet.

### 6.3 Xcode project bootstrap

Today nothing under `iosApp/` is buildable without doing the manual setup in `SETUP.md`.

**Steps (one-time):**
1. Create `iosApp/iosApp.xcodeproj` using Xcode "iOS App" template, point it at the existing
   Swift files in `iosApp/iosApp/`.
2. Add a "Build Shared Framework" Run Script Phase before Compile Sources:
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode \
     -Pkotlin.native.cocoapods.target=$ARCHS \
     -Pkotlin.native.cocoapods.configuration=$CONFIGURATION
   ```
   (or use the `cocoapods` Gradle plugin and `pod install`).
3. Add Framework Search Path: `$(BUILT_PRODUCTS_DIR)/../shared.framework` (or use
   `embedAndSignAppleFrameworkForXcode` which handles this).
4. Commit `iosApp/iosApp.xcodeproj` and `iosApp/Podfile.lock`.

**Strongly recommended:** enable the `kotlin("native.cocoapods")` plugin in
`shared/build.gradle.kts` and generate `shared.podspec`. That removes the manual framework
linking step and makes `pod install` from `iosApp/` self-sufficient.

**DoD:** A teammate can clone, `pod install` in `iosApp/`, open `iosApp.xcworkspace`,
hit Run, and see the app.

### 6.4 iOS deep links

`MainActivity` already converts `marsrover://rover/{id}` and `https://marsroverphotos.app/...`
into `DeepLink`. On iOS, replicate in Swift:

- Add `CFBundleURLSchemes = ["marsrover"]` to `Info.plist`.
- Add Associated Domains entitlement for `applinks:marsroverphotos.app` (requires Apple Developer
  AASA file hosted at `https://marsroverphotos.app/.well-known/apple-app-site-association`).
- In `MarsRoverApp.swift`:
  ```swift
  @State private var pendingDeepLink: DeepLink? = nil
  …
  WindowGroup {
      ContentView(deepLink: pendingDeepLink, onConsumed: { pendingDeepLink = nil })
        .onOpenURL { url in pendingDeepLink = parseDeepLink(url) }
        .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
            pendingDeepLink = activity.webpageURL.flatMap(parseDeepLink)
        }
  }
  ```
- `ContentView` passes `deepLink` to `Main_iosKt.MainViewController(deepLink:onConsumed:)` —
  this means updating `Main.ios.kt` to accept the same args as `App()`.

**DoD:** Tapping `marsrover://rover/5` in Safari on iOS navigates to the Curiosity photos
screen.

---

## 7. Android-only modules to leave in `androidApp/`

These are intentionally **not** ported — they don't belong in `commonMain`.

| Concern | Stays where | Notes |
|---|---|---|
| AdMob banner | `androidApp/` | Expose `AdSlot()` expect-composable; iOS/desktop return `Box`. |
| UMP / GDPR consent | `androidApp/` (`GdprHelper.kt`) | Expose `rememberConsentFlow()` expect; iOS/desktop no-op. |
| Glance widget | `androidApp/widget/` | App Widget is Android-only. Talk to shared repos via injected Koin scope. |
| Firebase Crashlytics init / Performance | `androidApp/MarsRoverApplication.kt` | Already wired correctly. |
| Splashscreen (`installSplashScreen`) | `androidApp/MainActivity.kt` | Already wired. iOS uses LaunchScreen storyboard. |
| Mission/facts uploader admin tools (`firebase/mission/*`, `firebase/facts/*`) | `androidApp/` (debug-only) | Developer maintenance — gate behind `BuildInfo.isDebug`. |
| `MainActivity` deep-link Intent parsing | `androidApp/` | Translates Android Intent → common `DeepLink`. |

---

## 8. Build / verification checklist

After each ticket:

```bash
./gradlew :shared:assemble                  # all targets compile
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :shared:testDebugUnitTest
./gradlew detekt
```

Manual smoke flow per platform:
- **Android emulator:** open app → cycle through 4 bottom tabs → drill into a rover → open a
  photo → save → share.
- **iOS simulator (after §6.3):** same flow. Skip save/share assertions until §6.2 is in.
- **Desktop:** `./gradlew :desktopApp:run` → confirm the same tabs render. Acceptable to skip
  Firebase-driven Popular tab on desktop.

CI gate to add (suggested): `:shared:compileKotlinIosSimulatorArm64` + `:androidApp:assembleDebug`
on every PR.

---

## 9. Risks & sequencing notes

- **Single biggest risk:** the `shared` module is configured as `android.library + kotlin.multiplatform`
  with `android.builtInKotlin=false` and `android.newDsl=false` (see `gradle.properties`). AGP 9.0
  expects KMP libraries to use `com.android.kotlin.multiplatform.library`. Migrating to that
  plugin is a separate ticket — track it but don't block S0 on it.
- **Paging Compose multiplatform** (3.4.2) should be stable now, but if iOS bindings still
  misbehave, fall back to `app.cash.paging:paging-compose-common` (the Cash App fork is
  rock-solid on iOS).
- **Coil 3 KMP** image loader is already wired in `shared` deps, so `AsyncImage` / `rememberAsyncImagePainter`
  should "just work" on iOS. If you see crashes loading images, double-check that the
  `coil-network-ktor` is present.
- **Compose Multiplatform 1.10.3 + Material3 adaptive APIs** (`NavigationSuiteScaffold`,
  `currentWindowAdaptiveInfo`, `calculateFromAdaptiveInfo`) — confirm the
  `material3-adaptive-navigation-suite` artifact (1.4.0) is wired into `commonMain`
  before S1; today it only ships with Compose BOM on Android.
- **ViewModel `viewModelScope` on iOS** — works as of Kotlin 2.3.21 + Lifecycle 2.10.0. If
  init-time coroutines never fire on iOS, double-check the `lifecycle.runtime.compose.multiplatform`
  artifact is present (it is).
- **Don't touch legacy `app/`** during the migration. It's already excluded from `settings.gradle.kts`.
  Use it as the reference implementation, then delete the whole module at the end of S9.

---

## 10. Suggested order & rough sizing

Single-engineer days, assuming the engineer is comfortable with KMP + CMP:

| Ticket | Est. | Notes |
|---|---:|---|
| S0 Cross-cutting | 2–3 d | Resources port + Tracker + BuildInfo + paging-compose + zoomable + MarsImageComposable port |
| S1 Rovers home | 1 d | Most groundwork done by S0; just port the composables. |
| S2 Photos | 1.5 d | Date pickers / sol UI is the meaty part. |
| S3 Image gallery | 1.5 d | Custom zoomable + bottom sheet. iOS save/share blocked on §6.2. |
| S4 Favorites | 0.5 d | Paging compose is the main risk. |
| S5 Popular | 0.5 d | Mostly the same as S4. |
| S6 Mission info | 1 d | Long but straightforward. |
| S7 About / settings | 1 d | Coil cache clearing on iOS is the only twist. |
| S8 Ukraine | 0.5 d | Easy. |
| S9 Widget adaptation | 0.5 d | Android only. |
| §6.1 Firebase iOS | 1–2 d | If we adopt GitLive Firebase KMP. |
| §6.2 iOS save/share | 1 d | cinterop with PhotoKit + UIActivityViewController. |
| §6.3 Xcode project | 0.5 d | One-time; commit project + Podfile.lock. |
| §6.4 iOS deep links | 0.5 d | Plus AASA file hosting. |
| §8 Stabilize + delete `app/` | 0.5 d | Removal commit, manifest cleanup. |

**Total:** ~13–16 engineering days, depending on Firebase strategy and zoom polish.

---

## 11. Definition of "done" for the whole migration

1. `settings.gradle.kts` lists `:shared`, `:androidApp`, `:iosApp` (Xcode-driven), `:desktopApp`. Legacy `:app` directory deleted.
2. `shared/src/commonMain/.../presentation/screens/PlaceholderScreens.kt` is gone — every `AppDestination`
   maps to a real composable.
3. `./gradlew :androidApp:assembleDebug` builds; the resulting APK has feature parity with
   the legacy app (rovers list, photos, gallery, favorites, popular, mission info, about, Ukraine, widget).
4. `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` builds; `iosApp.xcworkspace`
   opens, builds, and runs in the simulator with the same feature set (Firebase + photo
   save/share included, GoogleService-Info.plist supplied locally).
5. `./gradlew :desktopApp:run` opens the app with the same UI (Firebase / AdMob slots are
   no-ops — acceptable).
6. `detekt` + unit tests pass on CI for every push to main.
