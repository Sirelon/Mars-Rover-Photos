# Architecture — Mars Rover Photos

Companion to **[DESIGN_SYSTEM.md](DESIGN_SYSTEM.md)**: that doc governs how the UI *looks*, this one
governs where code *lives* and which layer is allowed to know about which. Read both before adding a
screen. Every rule below was verified against the codebase when it was written — they describe the
conventions this project already follows, not aspirations.

---

## Layer map

All shared code lives in `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/`:

| Package | Holds | May depend on |
|---|---|---|
| `domain/` | models, repository *interfaces*, `AppSettings` | nothing in this app — pure Kotlin + kotlinx |
| `data/` | network, database, paging, repository implementations | `domain` |
| `presentation/` | `screens/`, `viewmodels/`, `ui/` (the design system), `navigation/`, `theme/` | `domain` |
| `platform/` | `expect`/`actual` services: `Tracker`, `AppReview`, `BuildInfo` | — |
| `di/` | Koin modules — the only place that wires the layers together | everything |

Imports point **inward**: `presentation` → `domain`, never the reverse.

---

## Rules (prescriptive — follow these)

### Domain never imports presentation
- `domain/` has **zero** `presentation` imports. Keep it that way: no `MaterialSymbol`, no `Color`, no
  `dp`, no Compose-facing type in a domain model.
- When a domain value needs a UI representation, map it **in the UI layer** next to the component
  that renders it. `Release.Change.icon` is a plain Material Symbols name (`"rocket_launch"`);
  `materialSymbolOrDefault()` in `presentation/ui/MaterialSymbolIcon.kt` turns it into a
  `MaterialSymbol`. The identifier is domain data, the Compose type is not.
- The check: *would `domain/` still compile if `presentation/` were deleted?* If not, something is in
  the wrong layer. Wanting to import a UI type into a domain class means you're solving it at the
  wrong altitude — add a domain enum and map it in the UI.

### Every screen that shows data has a ViewModel
- Register it in `di/ViewModelModule.kt` with `viewModelOf(::X)`; retrieve it with `koinViewModel()`.
- "The data is static right now" is **not** a reason to skip it. `WhatsNewViewModel` was written
  against a hardcoded release list and later moved to `ReleaseNotesRepository` (Firestore) without any
  screen being rewritten — the whole point of the indirection.
- Injecting a **platform service** directly into a composable is fine and established — `Tracker` in
  `AppNavigation.kt`, `AppReview` in `AboutScreen.kt`. Injecting a **domain service so the composable
  can decide what to show** is not: that decision is the ViewModel's job.

### ViewModel state is observable — always
- Expose `StateFlow` for state, `Flow` for paging streams. **Never a plain `val`.** A `val` on a
  ViewModel is a constant with extra steps: the UI can't observe it, and the day the source becomes
  async the ViewModel's public API has to be rewritten. All nine ViewModels follow this today.
- One screen's state → one `data class XUiState` exposed as `StateFlow<XUiState>` and collected with
  `collectAsStateWithLifecycle()`. See `PhotosUiState`, `WhatsNewUiState`.
- Genuinely independent streams may stay separate flows when they change independently —
  `FavoriteImagesViewModel` keeps `sortOrder`, `roverFilter`, and the paging flow apart on purpose.
- One-shot things (navigation, snackbars, scroll-to-top) are **events, not state**: use a `Channel` or
  `SharedFlow` — `ImageViewModel.uiEvent`, `PhotosViewModel.scrollToTopEvents`. Putting them in
  `UiState` makes them replay on every recomposition and on process restore.
- Mutate with `_state.update { it.copy(...) }`, expose with `.asStateFlow()`.

### Navigation routes; it does not decide
- `navigation/AppNavigation.kt` and the entry builders in `di/NavigationModule.kt` move between
  destinations. They must not read settings or repositories to decide **whether** to navigate.
- The condition belongs in a ViewModel and is read back as a single call. Reference implementation:
  `WhatsNewViewModel.shouldShowDialog()` decides (fresh install vs. update, notes present or not) and
  the nav root only does
  `if (deepLink == null && whatsNewViewModel.shouldShowDialog()) navigator.navigate(...)`.
  It is a `suspend` function because the notes are fetched, and it bounds its own wait — the nav layer
  neither knows nor decides where the data comes from.
- Corollary: business state that outlives a screen ("the user has seen version N") is written by the
  ViewModel through `AppSettings` — not by a `LaunchedEffect` in the navigation composable. Logic
  parked in nav is untestable and duplicates a responsibility that already has an owner.

### Repositories cache; ViewModels don't count on being alone
- Content the app shows but does not compute — rovers, popular photos, educational facts, release
  notes — lives in Firestore and is reached through a `domain/repositories` interface with a
  `data/repositories` implementation, bound as a Koin `single`. There is no bundled copy: the app needs
  the network regardless, and Firestore's own local cache covers repeat launches.
- **Cache in the repository, not the ViewModel.** Nav3 gives every back-stack entry its own
  `ViewModelStore`, so one screen's ViewModel is constructed once per entry that asks for it —
  `WhatsNewViewModel` exists up to four times over (nav root, dialog, story pager, version list).
  `ReleaseNotesRepositoryImpl` holds the fetched list behind a `Mutex` so that costs one query, not
  four. Anything a ViewModel fetches in `init` needs the same treatment.
- A fetch that fails returns empty rather than throwing, and the screen renders `CenteredProgress` /
  `AppEmptyState` off `isLoading`. Remote content that has not arrived is a normal state, not an error.

### Type names must stand alone
- A top-level name that only makes sense once you know its package is too generic. `Change`, `Item`,
  `Entry`, `State`, `Data` standing alone are red flags.
- **Nest** it when it has no meaning without its parent (`Release.Change`); **prefix** it
  (`ReleaseChange`) when it's referenced widely enough that nesting reads noisy.
- Design-system components use the `App*` convention instead — see DESIGN_SYSTEM.md.

---

## Self-check before review

- [ ] `domain/` gained no `presentation` imports; UI mapping lives in the UI layer
- [ ] new screen has a ViewModel registered in `ViewModelModule.kt`
- [ ] VM state is `StateFlow`/`Flow`; one-shot signals are `Channel`/`SharedFlow`
- [ ] no business condition in `AppNavigation.kt` or a nav entry builder
- [ ] no new top-level type with a name that needs its package to disambiguate it
- [ ] checked `presentation/ui/` for an existing component before writing a new one — DESIGN_SYSTEM.md
- [ ] remote content is cached in the repository, not per-ViewModel; loading and empty states exist
- [ ] no hardcoded theme colors, no raw `.dp` — DESIGN_SYSTEM.md

---

## History

- 2026-08-08 — Created from the What's New session retro (`.claude/session-retro-whats-new.md`). That
  feature took seven rounds of review corrections; five were architecture — a UI enum
  (`MaterialSymbol`) inside a domain model, three screens with no ViewModel, then a ViewModel exposing
  plain `val`s, the show-the-dialog decision living in a `LaunchedEffect` in the nav root, and a
  top-level type named `Change`. The remaining two (duplicating an existing row component, hardcoded
  color constants) were already covered by DESIGN_SYSTEM.md and simply not read.
