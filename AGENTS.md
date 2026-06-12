# Repository Guidelines

## Project Structure & Module Organization
This is a Kotlin Multiplatform project. The module layout:
- `shared/` — all shared code: domain, data, repositories, view models, common Compose UI, navigation, DI
- `androidApp/` — Android shell: `MainActivity`, `MarsRoverApplication`, widget, GDPR helper, app icons
- `desktopApp/` — Desktop shell
- `iosApp/` — Swift/iOS shell, Xcode project, Firebase config
- `webApp/` — experimental standalone WASM shell; shared web support is still disabled (see `WASM_WEB_SUPPORT.md`)

Feature screens and view models live in `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation`. Android-specific implementations go in `shared/src/androidMain` or `androidApp/`. Static-analysis configuration is stored under `config/detekt/`.

The KMP migration is complete. The legacy `app/` module has been deleted.

## Photo Feed & Data Sources
The heart of the app is the shared rover photo feed in `shared/.../data/paging/`: an app-singleton `RoverFeedPager` exposes one cached `PagingData` stream collected by both the list grid and the fullscreen viewer, with a per-rover `FeedMode` seam — sol-keyed (`SolPagingSource`: Curiosity, Perseverance, InSight) vs page-keyed with a random anchor (`ImagesSearchPagingSource`: Spirit, Opportunity). The class KDocs there document the non-obvious invariants (continuation pages, anchor resolution, write-through cache merge rules) — read them before touching paging. Data-source constraints: the classic `api.nasa.gov/mars-photos` API is permanently dead; Spirit/Opportunity only exist in the curated images.nasa.gov library (no raw archive API); each rover's source is mapped in the repository/network layer.

## Dependency Versions
Beta, alpha, and RC dependency versions are acceptable in this project. Prefer the version that unlocks a needed multiplatform capability over waiting for a stable release (e.g., `lifecycle-viewmodel-navigation3` requires `2.11.0+` for the iOS/Desktop/Web ViewModelStore Nav3 APIs; `2.10.0` was Android-only). When pinning a pre-release, record why it is required so the choice stays revisitable, but do not reject a pre-release on stability grounds alone.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build the debuggable APK with the repository Compose compiler flags.
- `./gradlew testDebugUnitTest` — run Android-side JVM unit tests.
- `./gradlew :shared:desktopTest` — run the shared KMP/common tests on the desktop JVM target.
- `./gradlew connectedDebugAndroidTest` — launch instrumentation tests on an attached emulator or device.
- `./gradlew detekt` — lint and autocorrect according to `config/detekt/detekt.yml`.
- `./gradlew :shared:compileAndroidMain` — quick Android-target compile check of shared code (AGP 9 KMP task naming; there is no `compileDebugKotlinAndroid` on `:shared`).
Run commands from the repository root so the Gradle wrapper can supply the pinned toolchain.

### iOS dev builds
The Xcode project consumes a prebuilt framework from `shared/build/XCFrameworks/debug/shared.xcframework` — in a fresh clone/worktree (or after any shared-code change) run `./gradlew :shared:assembleSharedDebugXCFramework` before building the iOS app, or Xcode fails with "There is no XCFramework found". `iosApp/iosApp/GoogleService-Info.plist` is gitignored; copy it from an existing checkout or Firebase console, otherwise the build fails on a missing input file.

## Versioning
The app version lives in **one place**: `buildSrc/src/main/kotlin/AppVersion.kt` (`name` = marketing version, `code` = build number). Android (`androidApp/build.gradle.kts`) and Desktop (`desktopApp/build.gradle.kts`) read it directly at build time. iOS can't read Kotlin, so it's kept in sync via Gradle tasks (group `versioning`, defined in `gradle/versioning.gradle.kts`):

- `./gradlew bumpVersion` — increments `code` by 1 and bumps the **minor** part of `name`, resetting patch (e.g. `3.0.0` → `3.1.0`). Updates `AppVersion.kt` and the iOS project in one shot.
- `./gradlew bumpVersion -PversionName=5.0.0` — same, but sets `name` to the value you pass (must be `major.minor.patch`); `code` is still incremented by 1.
- `./gradlew syncIosVersion` — pushes the current `AppVersion.kt` values into `iosApp/iosApp.xcodeproj/project.pbxproj` (`MARKETING_VERSION` ← `name`, `CURRENT_PROJECT_VERSION` ← `code`). Use this only if you edit `AppVersion.kt` by hand; `bumpVersion` already does it.

Never hand-edit the version in `project.pbxproj` — it is overwritten on the next sync/bump.

## Coding Style & Naming Conventions
Kotlin files use four-space indentation, `val` first, and explicit visibility for public APIs. Compose functions and classes stay in PascalCase, constants in `UPPER_SNAKE_CASE`, and extension files match their receiver (`ImageRequestExt.kt`). Keep packages cohesive; add a `feature/*` subpackage for new screens. Run `./gradlew detekt` before review instead of hand-tuning formatting.

## Design System & UI/UX
Before writing or changing any Compose UI, read **[docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md)** — the
living design-system doc. It carries the prescriptive UI/UX rules (token usage, the `App*` component
family, adaptive-nav ownership), a component index with file links, and the non-obvious insights
(e.g. dark `surface` == `background`, no green slot in the M3 palette, the full Material Symbols font).
Reuse the `App*` components in `presentation/ui/` before adding new ones; prefer `AppSpacing` /
`AppSize` over adding new raw `.dp` literals, and use `MaterialTheme.colorScheme` instead of hardcoded theme colors. **Keep the doc
updated**: when you add a reusable component, a token, or learn a UI gotcha, record it there.

## Testing Guidelines
Place unit specs in `shared/src/commonTest` (or `androidTest` for Android-instrumented tests), mirroring the source package and ending class names with `*Test`. Common tests use `kotlin.test` + `kotlinx-coroutines-test` with hand-rolled fakes (see `data/paging/Fakes.kt`) — no mocking library in commonTest; JUnit4 and Mockito-Kotlin apply to Android-instrumented tests only. Paging behavior should be verified at two levels: `PagingSource.load()` unit tests, plus `TestPager` (androidx.paging:paging-testing, KMP) for Pager-level invariants like continuation pages and end-of-pagination. Compose UI or Room integration checks belong in `androidApp/src/androidTest` and should describe the scenario in the test name. Cover paging boundaries, offline caching, and error flows whenever you touch those areas.

## Commit & Pull Request Guidelines
Follow the existing history with concise imperative subject lines under ~70 characters (e.g., `Fix release build`). Keep functional changes grouped per commit. Pull requests need a short summary, call out UI or API changes, link issues, and attach emulator screenshots or recordings when visuals move. Confirm `assembleDebug`, `testDebugUnitTest`, `:shared:desktopTest`, and `detekt` succeed before requesting review.

## Security & Configuration Tips
Signing data comes from `keystore.properties`; keep it local and out of version control. Avoid committing `local.properties`, API keys, or regenerated `*.jks` files. Prefer runtime configuration or encrypted storage for new secrets and alert maintainers immediately if a credential leaks.
