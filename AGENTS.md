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

## Architecture & Layering
Before adding a screen, ViewModel, or domain model, read **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.
It carries the layer map (`domain` ← `data` / `presentation`, wired only in `di/`) and the prescriptive
rules that reviews keep catching: `domain/` never imports `presentation/` (map domain values to UI types
with an extension in the UI layer); every screen that shows data gets a ViewModel registered with
`viewModelOf(::X)`, even when the data is currently static; ViewModel state is always observable
(`StateFlow`/`Flow`, never a plain `val`) with one-shot signals as `Channel`/`SharedFlow`; navigation
composables route but never make business decisions; and top-level type names must stand alone
(`Release.Change`, not `Change`). It ends with a pre-review self-check list.

## Design System & UI/UX
Before writing or changing any Compose UI, read **[docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md)** — the
living design-system doc. It carries the prescriptive UI/UX rules (token usage, the `App*` component
family, adaptive-nav ownership), a component index with file links, and the non-obvious insights
(e.g. dark `surface` == `background`, no green slot in the M3 palette, the full Material Symbols font).
Reuse the `App*` components in `presentation/ui/` before adding new ones; prefer `AppSpacing` /
`AppSize` over adding new raw `.dp` literals, and use `MaterialTheme.colorScheme` instead of hardcoded theme colors.
The same reuse-first rule applies **within** the token scales: check `AppSpacing` / `AppSize` / `AppMotion`
for a value that fits before adding an entry, and don't borrow a token whose name belongs to another
component just because the number matches. **Keep the doc updated**: when you add a reusable component,
a token, or learn a UI gotcha, record it there — and when a UI fix takes several rounds to land, leave a
short symptom-first note in that doc's *Insights / gotchas* section so the next reader recognizes it from
the behavior.

## Strings & Localization
**The app is English-only and there is no localization.** `shared/src/commonMain/composeResources/`
has a single `values/strings.xml` and no `values-*` locale variants; none are planned. Consequences
for reviews and new code:

- Hardcoded user-facing copy in a composable (`Text("Version History")`, `contentDescription = "Close"`)
  is **acceptable** — it is not a review finding, and moving copy into `strings.xml` is not a
  prerequisite for merging. `AboutScreen.kt` and the What's New screens are the established precedent.
- `strings.xml` is still fine to use, and existing `stringResource(Res.string.…)` call sites should stay
  as they are — don't churn them either direction. Reach for a resource when the same copy is genuinely
  shared across several call sites; inline it when it isn't.
- The same applies to English-language copy on a domain model (`Release.Change.title/summary/detail` in
  `domain/releasenotes/`). What `domain/` must not hold is a **Compose/UI type** — see the
  domain-never-imports-presentation rule in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). A `String` is
  not that.
- Plural/count copy may be built inline (`"$n change${if (n != 1) "s" else ""}"`); no plural resource
  needed.

If translations are ever added, this section is the thing to revisit — at that point the copy does need
to be pulled into resources, and this note should be replaced rather than quietly ignored.

## Testing Guidelines
Place unit specs in `shared/src/commonTest` (or `androidTest` for Android-instrumented tests), mirroring the source package and ending class names with `*Test`. Common tests use `kotlin.test` + `kotlinx-coroutines-test` with hand-rolled fakes (see `data/paging/Fakes.kt`) — no mocking library in commonTest; JUnit4 and Mockito-Kotlin apply to Android-instrumented tests only. Paging behavior should be verified at two levels: `PagingSource.load()` unit tests, plus `TestPager` (androidx.paging:paging-testing, KMP) for Pager-level invariants like continuation pages and end-of-pagination. Compose UI or Room integration checks belong in `androidApp/src/androidTest` and should describe the scenario in the test name. Cover paging boundaries, offline caching, and error flows whenever you touch those areas.

## Acting on Code-Review Comments
**Verify every review comment against the current code before changing anything.** A comment is a
claim, not an instruction — treat it as a lead to check, not a ticket to close. In practice:

- **Re-read the code the comment points at, at HEAD.** Reviews are frequently written against an
  earlier commit on the same branch; findings about performance, stability or an API's shape are
  routinely already fixed by a later commit. Check `git log`/`git diff` for the file before acting.
- **Confirm the mechanism, not just the conclusion.** If a comment says a value is unclamped, a
  ViewModel is a different instance, or a doc rule is being violated, open the relevant source /
  doc and confirm it. Quoted compiler reports, line numbers and library internals in a comment can
  all be stale.
- **Check the suggested fix is actually equivalent.** "Deleting X produces the same output" is often
  true on the path the reviewer looked at and false on another call site.
- **Say so when a comment is wrong, and don't apply it.** Reply on the comment (Conductor MCP:
  `mcp__conductor__DiffComment`) explaining what the code actually does, and list the rejected or
  already-fixed comments in the summary alongside the ones you fixed. Silently "fixing" an inaccurate
  comment is worse than leaving it open.
- A comment that is right about the mechanism but wrong about severity or scope gets the same
  treatment: fix what is real, state plainly what isn't.

## Commit & Pull Request Guidelines
Follow the existing history with concise imperative subject lines under ~70 characters (e.g., `Fix release build`). Keep functional changes grouped per commit. Pull requests need a short summary, call out UI or API changes, link issues, and attach emulator screenshots or recordings when visuals move. Confirm `assembleDebug`, `testDebugUnitTest`, `:shared:desktopTest`, and `detekt` succeed before requesting review.

## Security & Configuration Tips
Signing data comes from `keystore.properties`; keep it local and out of version control. Avoid committing `local.properties`, API keys, or regenerated `*.jks` files. Prefer runtime configuration or encrypted storage for new secrets and alert maintainers immediately if a credential leaks.
