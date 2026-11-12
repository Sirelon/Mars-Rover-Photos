# Repository Guidelines

Mars Rover Photos is a Kotlin Multiplatform app (Android, iOS, desktop) that shows NASA Mars rover
photos. One owner; agents do most of the work and the owner checks builds on a device.

## Modules
- `shared/` — all shared code: domain, data, repositories, view models, common Compose UI, navigation, DI
- `androidApp/` — Android shell: `MainActivity`, `MarsRoverApplication`, widget, GDPR helper, app icons
- `baselineprofile/` — Baseline Profile generator and cold-startup Macrobenchmark for `androidApp`
- `desktopApp/` — Desktop shell, wired for Compose Hot Reload
- `iosApp/` — Swift/iOS shell, Xcode project, Firebase config
- `webApp/` — experimental standalone WASM shell, not wired to `shared`

Shared code lives in `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/`. Screens go in
`presentation/screens/` (a multi-file feature gets its own subpackage, like `screens/whatsnew/`), view
models in `presentation/viewmodels/`. Android-specific implementations go in `shared/src/androidMain`
or `androidApp/`.

`shared/src/mobileMain` is the intermediate source set shared by Android and iOS but not desktop
(declared with `applyDefaultHierarchyTemplate` in `shared/build.gradle.kts`). It holds what only the
two store platforms have: the RevenueCat billing implementation (`data/support/`) and Firebase Remote
Config feature flags (`data/featureflags/`), wired by `di/SupportModule.kt`. `desktopMain` binds
fakes for the same interfaces (`FakeSupportRepository`, `StaticFeatureFlags`), so a `commonMain`
consumer never knows which it got. Put code here when its dependency has no JVM variant — check the
artifact's Gradle module metadata before assuming.

## Read before you touch
Each row is binding for the work it names. Read the doc first; it carries invariants the code does
not show.

| Work | Read |
|---|---|
| A screen, ViewModel, domain model or repository | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Any Compose UI | [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md), then run the `compose-design-system` skill on the diff |
| Paging, `NasaApi`, a rover's photo source, the Viking catalogue | [docs/PHOTO_FEED.md](docs/PHOTO_FEED.md) |
| Support screen, RevenueCat, feature flags, ads, UMP / ATT prompts | [docs/MONETIZATION.md](docs/MONETIZATION.md) |
| Version number, release notes, fastlane lanes, Galaxy Store | [docs/RELEASE.md](docs/RELEASE.md); cut a release with the `store-release` skill |
| Looking at or driving the UI | [docs/DESKTOP_HOT_RELOAD.md](docs/DESKTOP_HOT_RELOAD.md) |
| iOS build, Xcode project, SwiftPM linkage | [iosApp/README.md](iosApp/README.md) |
| Startup path, photo grid, the `roverCard` / `photoGrid` test tags | [baselineprofile/README.md](baselineprofile/README.md) |
| Deep links, notification tap targets | [docs/DEEP_LINKING.md](docs/DEEP_LINKING.md), [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) |
| Store screenshots | [.maestro/README.md](.maestro/README.md) |
| Web / WASM target | [docs/WASM_WEB_SUPPORT.md](docs/WASM_WEB_SUPPORT.md) |

## Commands
Run from the repository root so the Gradle wrapper supplies the pinned toolchain.

- `./gradlew :shared:desktopTest :shared:compileAndroidMain` — the **verify pair**: every common test
  on the desktop JVM plus an Android-target compile of shared code. Run it before every commit.
  (AGP 9 KMP task naming; there is no `compileDebugKotlinAndroid` on `:shared`.)
- `./gradlew :androidApp:assembleDebug` — the debuggable APK. Run it when `androidApp/` changed.
- `./gradlew :shared:assembleSharedDebugXCFramework` — the framework Xcode links. Run it after a
  shared change that iOS must pick up.
- `./gradlew :desktopApp:hotRun` — the desktop app with hot reload.
- `./gradlew detekt` — lints and autocorrects `androidApp` and the root project per
  `config/detekt/detekt.yml`. It does not source `shared/`, so a clean run says nothing about shared
  code; check shared UI against the design-system rules by reading the diff.

## Debug build label
Before building a debug APK, write a short feature name to
`androidApp/src/main/assets/debug_label.txt`:

```bash
mkdir -p androidApp/src/main/assets
echo "your-feature-name" > androidApp/src/main/assets/debug_label.txt
```

The app shows it as a badge in the top-right corner of every debug build, so the owner can tell
which feature is installed when several agents build in parallel. Use the branch name or a 1–3 word
slug (`sol-feed`, `dark-theme`). The file is gitignored and the next `assembleDebug` picks it up.

## Architecture & Layering
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) carries the layer map (`domain` ← `data` /
`presentation`, wired only in `di/`) and the prescriptive rules that reviews keep catching:
`domain/` never imports `presentation/` (map domain values to UI types with an extension in the UI
layer); every screen that shows data gets a ViewModel registered with `viewModelOf(::X)`, even when
the data is currently static; ViewModel state is always observable (`StateFlow`/`Flow`, never a
plain `val`) with one-shot signals as `Channel`/`SharedFlow`; navigation composables route but never
make business decisions; top-level type names must stand alone (`Release.Change`, not `Change`); and
behaviour goes in `commonMain` even when one platform's data found the problem — a platform source
set holds only what common code cannot express, and an engine setting goes into every `actual`.
It ends with a pre-review self-check list.

## Design System & UI/UX
[docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) carries the prescriptive UI/UX rules (token usage,
the `App*` component family, adaptive-nav ownership), a component index with file links, and the
non-obvious insights (e.g. dark `surface` == `background`, no green slot in the M3 palette, the full
Material Symbols font). Reuse the `App*` components in `presentation/ui/` before adding new ones;
prefer `AppSpacing` / `AppSize` over raw `.dp` literals, and use `MaterialTheme.colorScheme` instead
of hardcoded theme colors. The same reuse-first rule applies **within** the token scales: check
`AppSpacing` / `AppSize` / `AppMotion` for a value that fits before adding an entry, and don't borrow
a token whose name belongs to another component just because the number matches. **Keep the doc
updated**: when you add a reusable component, a token, or learn a UI gotcha, record it there — and
when a UI fix takes several rounds to land, leave a short symptom-first note in that doc's
*Insights / gotchas* section so the next reader recognizes it from the behavior.

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
- The same applies to English-language copy on a domain model (`Release.Change.title/summary/detail`
  in `domain/releasenotes/`, filled from Firestore). What `domain/` must not hold is a **Compose/UI
  type** — see the domain-never-imports-presentation rule in
  [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). A `String` is not that.
- Plural/count copy may be built inline (`"$n change${if (n != 1) "s" else ""}"`); no plural resource
  needed.

If translations are ever added, this section is the thing to revisit — at that point the copy does need
to be pulled into resources, and this note should be replaced rather than quietly ignored.

## Dependency Versions
Beta, alpha, and RC dependency versions are acceptable in this project. Prefer the version that unlocks a needed multiplatform capability over waiting for a stable release (e.g., `lifecycle-viewmodel-navigation3` requires `2.11.0+` for the iOS/Desktop/Web ViewModelStore Nav3 APIs; `2.10.0` was Android-only). When pinning a pre-release, record why it is required so the choice stays revisitable, but do not reject a pre-release on stability grounds alone.

## Testing
Tests live in `shared/src/commonTest`, mirror the source package, end class names with `*Test`, and
run through `:shared:desktopTest`. They use `kotlin.test` + `kotlinx-coroutines-test` with
hand-rolled fakes (see `data/paging/Fakes.kt`) and no mocking library. Verify paging behavior at two
levels: `PagingSource.load()` unit tests, plus `TestPager` (`androidx.paging:paging-testing`, KMP)
for Pager-level invariants like continuation pages and end-of-pagination. Cover paging boundaries,
offline caching, and error flows whenever you touch those areas.

The repo has no Android-instrumented and no Compose UI test suite. Verify a UI change by looking at
it: desktop hot reload first, a device for Android-only or iOS-only code.

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

## Commits and branches
Where the work lands depends on where the session runs. This rule is strict.

- **Main checkout, on `master`.** Commit on `master`. Stay there unless the owner asks for a branch
  or a worktree.
- **A worktree** — any checkout a tool created for the session, such as Conductor or Paseo. Create a
  branch for the work and commit there. The change reaches `master` only through a pull request
  (merge request) from that branch. From a worktree, never commit to `master`, merge into it, or push
  to it.

Check which case applies before the first commit:

```bash
[ "$(git rev-parse --git-dir)" = "$(git rev-parse --git-common-dir)" ] && echo "main checkout" || echo "worktree"
```

Write a concise imperative subject under ~70 characters (`Fix release build`) and keep one
functional change per commit. Run the verify pair first, plus `:androidApp:assembleDebug` when
`androidApp/` changed.

## Linear ticket status after merge
**Human Check** is a waiting state: the work is on the default branch (see *Commits and branches*)
and the owner has not looked at it on a device yet. It lets him accept agent-built work now and
check it when he has time.

- **Human Check** — set it when the change has behaviour a person must see on a device: UI, flows,
  copy, notifications, store or ads behaviour, anything that only shows in a real install. Add a
  comment with numbered check steps and the expected result for each.
- **Done** — set it when the build and tests fully prove the change (refactors, docs, tests, build
  config), or as soon as the owner confirms the change.
- **The owner's confirmation closes the ticket.** When he says in the session that he checked a
  change and it works, move its ticket from Human Check to Done in the same turn, and add a comment
  that records what he confirmed and on which device. His check on one platform is enough; leave
  the status decision to him only when he says more checking is needed.
- Your own test pass, on a simulator or a device, leaves the ticket in Human Check. A ticket he did
  not check stays there too: his confirmation covers what he looked at.

## Credentials
The repository is public. Every credential is gitignored — `keystore.properties`, `*.jks`, the
`fastlane/` keys, `iosApp/iosApp/GoogleService-Info.plist` — so a fresh worktree has none of them;
[docs/RELEASE.md](docs/RELEASE.md) lists each one and what fails without it. Keep new secrets out of
tracked files the same way.

## Subagent limits
Append this block to the brief of every subagent that can run commands or edit files:

> Project limits: run `./gradlew :shared:desktopTest :shared:compileAndroidMain` before reporting
> done. Leave releases to the main session: no `fastlane`, no `bumpVersion`, no
> `publish-release-notes.mjs`, nothing that uploads to a store or writes to Firestore. Use the
> desktop app or a simulator; do not install on, wipe, or automate a physical device. Do not run
> `:shared:integrateLinkagePackage`. Report a design or scope fork upward instead of choosing.
