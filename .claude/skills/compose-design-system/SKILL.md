---
name: compose-design-system
description: Use when writing, changing, or reviewing Compose Multiplatform UI in this repo — to check that it consumes the existing design system instead of rebuilding it. Covers reimplementation of existing `presentation/ui` App* components, AppSpacing / AppSize / AppTypography / AppMotion token usage, hardcoded colors and strings, adaptive layout + nav-chrome ownership, insets, and the ViewModel/composition boundary. Run before creating a new widget and when reviewing a UI diff.
---

# Compose design system compliance

Detection procedure for the Mars Rover Photos design system. The **rules** and the **component
index** live in [docs/DESIGN_SYSTEM.md](../../../docs/DESIGN_SYSTEM.md) (plus the short pointer in
[AGENTS.md](../../../AGENTS.md) § Design System & UI/UX). This skill contributes the checks, not a
second copy of the rules — when a check fires, cite the owning doc section.

**Why mechanical checks matter here:** `./gradlew detekt` does **not** lint `shared/commonMain`
(it sources `androidApp` + root only). Raw `.dp`, hardcoded colors, and hardcoded strings in shared
UI are caught by *this* review or not at all. A green detekt run is not evidence.

## Scope gate

Applies to changed `*.kt` files containing `@Composable` or `@Preview` — in practice everything
under `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/`. If none, stop.

Build the file list (include untracked — a brand-new screen or component file is invisible to
`git diff`):

```bash
{ git diff --name-only "${BASE:-origin/master}"...HEAD; git ls-files --others --exclude-standard; } \
  | grep '\.kt$' | sort -u > /tmp/compose.txt
xargs git grep --no-index -l -E '@Composable|@Preview' -- < /tmp/compose.txt > /tmp/compose_ui.txt
```

Every grep below uses `git grep --no-index` (reaches untracked files) with
`xargs … < /tmp/compose_ui.txt` (zsh does not word-split `$VAR`; BSD `xargs` has no `-a`).
Substituting plain `grep` or a bare `$FILES` pathspec returns zero matches and reads as a clean pass.

Route by location — these are different reviews:

| Changed under | Review as | Weight the checks toward |
|---|---|---|
| `presentation/ui/`, `presentation/theme/` | design-system **authoring** | §3 API shape, §4 token definitions, generality of the name |
| `presentation/screens/`, `presentation/navigation/` | design-system **consumption** | §2 reimplementation, §4 tokens, §5 strings, §6 layout/insets |

## 1. Duplicate component files (run first — cheapest, highest signal)

For every **new** public composable in the diff:

```bash
git grep --untracked -n "fun <ComponentName>" -- '*.kt'
```

`--untracked` matters: without it a duplicate sitting in a brand-new file goes unseen. More than one
hit → it already exists. Report 🔴 and name the existing file.

Also check the **name** itself: design-system components are general and `App*`-prefixed. A new
app-wide reusable named after the screen it first appeared in (`Rovers*`, `About*`, `Photos*`) is a
finding — generalize it into `presentation/ui/` instead. The documented exception is list-row-shaped
`Settings*` primitives in `ui/SettingsComponents.kt`.

## 2. Reimplementation of an existing component

The core check. Match the shape of new UI against this table before accepting it as new code.
All paths are relative to `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/`.

| Hand-rolled shape in the diff | Use instead | Lives in |
|---|---|---|
| `Card`/`Surface` + elevation + `clickable` wrapper + hover/elevation animation | `AppCard(onClick = …)` — it owns the clickable **and** the hover-lift | `ui/AppCard.kt` |
| `Card` with a border + `surfaceContainerHigh` fill | `AppOutlinedCard` (a separate component, never a flag on `AppCard`) | `ui/AppOutlinedCard.kt` |
| `Box(Modifier.background(tint, shape)) { Icon(...) }` | `AppIconBox` | `ui/AppIconBox.kt` |
| `Row { Icon; Text(value); Text(label) }` metric trio | `AppMetricItem` | `ui/AppMetricItem.kt` |
| coloured-dot + label pill, or an outlined pill | `StatusBadge` / `AppBadge` / `BadgeRow` | `ui/Badges.kt` |
| `Row` of `FilterChip`s or a hand-rolled selector with an indicator | `SegmentedControl<T>` (animated, draggable) | `ui/SegmentedControl.kt` |
| `TopAppBar` built inline (title/subtitle/back/actions) | `AppTopBar` | `ui/AppTopBar.kt` |
| `Column { Icon; Text; Button }` "nothing here" state | `AppEmptyState` | `ui/AppEmptyState.kt` |
| `Box(contentAlignment = Center) { CircularProgressIndicator() }` | `CenteredProgress` / `CenteredColumn` | `ui/CenteredComponents.kt` |
| `Icon(painterResource(...))` / `Icons.Default.*` | `MaterialSymbolIcon` + a `MaterialSymbol` enum entry | `ui/MaterialSymbolIcon.kt` |
| `AsyncImage` / `Image` for a network photo | `MarsImage` / `NetworkImage` (owns Coil config + cache keys) | `ui/MarsImage.kt` |
| own `NavigationBar` / `NavigationRail` | `MarsNavigationSuite` owns the nav chrome | `navigation/MarsBottomBar.kt` |
| `DatePickerDialog` per platform | `PlatformDatePickerDialog` | `ui/PlatformDatePicker.kt` |
| inline `sharedElement`/`sharedBounds` keys for photo↔viewer | `Modifier.sharedPhoto` / `sharedFavorite` / `navFadeEnter` | `ui/SharedPhotoTransition.kt` |
| a literal green for "active / live" | `activeStatusColor()` — there is **no green slot** in the palette | `theme/AppColors.kt` |
| `BoxWithConstraints` width threshold / hand-rolled column math | `currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(...)` | see §6 |

Not in the table? Search the catalog before concluding it's genuinely new:

```bash
grep -n '^| `App\|^| `' docs/DESIGN_SYSTEM.md
```

Then confirm against the source — the catalog is an index and may lag.

## 3. Component API shape (authoring only)

- `modifier: Modifier = Modifier` as the first optional parameter on every composable, private ones
  included; the caller's modifier is applied to the outermost node.
- Caller controls visibility — no internal "nothing to show" early return.
- Pass the narrowest model a widget needs, not the whole screen state.
- Components are general: no screen-specific data types, no `Settings`/`Rovers` vocabulary in a
  `presentation/ui/` API.
- Uniform gaps use `Arrangement.spacedBy()`, not `Spacer` between children.
- Composition-local-dependent modifiers (shared transitions) must **no-op outside their scope** so
  previews don't crash — follow `SharedPhotoTransition.kt`.

For a component whose visual regions vary by caller, or where boolean shape flags are accumulating,
hand off to the `compose-slot-api-pattern` skill. For state placement questions hand off to
`compose-state-hoisting`; for `LaunchedEffect`/`DisposableEffect` review, `compose-side-effects`.

Note: there is **no Compose UI-test infra** in `shared`, and previews are not enforced. Verify with
`./gradlew :shared:desktopTest :shared:compileAndroidMain` plus a visual smoke test; don't claim
test coverage for a styling change.

## 4. Tokens and theme

```bash
# literal dimensions — theme/AppSpacing.kt and theme/AppSize.kt are the only legitimate sites
xargs git grep --no-index -nE '[0-9]+\.dp|[0-9]+\.sp' -- < /tmp/compose_ui.txt

# hardcoded colors — theme/Theme.kt and theme/AppColors.kt are the only legitimate sites
xargs git grep --no-index -nE 'Color\(0x|Color\.(Red|Green|Blue|White|Black|Gray)' -- < /tmp/compose_ui.txt

# raw motion — theme/AppMotion.kt is the only legitimate site
xargs git grep --no-index -nE 'tween\([0-9]|spring\(|durationMillis *= *[0-9]' -- < /tmp/compose_ui.txt
```

Expected replacements: `AppSpacing` (8dp grid: `xs`4 `sm`8 `md`12 `lg`16 `xl`24 `xxl`32 `x3l`48) for
spacing/padding/gaps; `AppSize` for component dimensions and corner radii; `AppTypography` or
`MaterialTheme.typography.*` for type (never bake color into a `TextStyle`);
`MaterialTheme.colorScheme.*` for color; `AppMotion` for durations/easing/bounds transforms.

If a value is neither grid spacing nor a clear size, **extend `AppSize`** rather than leaving a magic
number. Read `theme/AppSize.kt` and `theme/AppSpacing.kt` before asserting a wrong token.

Colour gotchas that produce real bugs (details in DESIGN_SYSTEM.md § Insights):
- Dark `surface` == `background` (`#121212`) — a card on `colorScheme.surface` is invisible in dark
  mode. Cards that must lift use `surfaceContainerHigh`. `surfaceColorAtElevation` is not available
  in the pinned M3.
- "Active / live" green → `activeStatusColor()`, never a literal.
- Brand slots are re-applied over dynamic color in `Theme.kt` — read it before touching colors.

## 5. Strings

```bash
# hardcoded user-facing copy
xargs git grep --no-index -nE 'text = "|label = "|contentDescription = "|Text\(\s*"' -- < /tmp/compose_ui.txt
```

All user-facing copy belongs in
`shared/src/commonMain/composeResources/values/strings.xml`, read via
`stringResource(Res.string.<name>)`. Before accepting a **new** string, check for existing copy:

```bash
git grep -in "<the exact copy>" -- shared/src/commonMain/composeResources/values/strings.xml
```

Naming is feature-first snake_case (`rovers_title`, `rovers_subtitle_fmt`); format strings end in
`_fmt`. Count-dependent copy uses a plural resource, not `if (n == 1)` in composition. Repeated
"verb + noun" labels become one parameterized string, not one per noun.

Presentation-only copy keyed on domain ids (rover mission blurbs, `ui/RoverBlurb.kt`) stays in the
presentation layer — never add such a field to a domain model or the data/API layer.

## 6. Adaptive layout, chrome, and insets

| Pattern in the diff | Why it's wrong |
|---|---|
| own `NavigationBar` / `NavigationRail` / bottom bar | `MarsNavigationSuite` owns nav chrome for compact ↔ medium/expanded |
| screen `Scaffold` without `contentWindowInsets = WindowInsets()` | insets double-counted in `innerPadding` |
| `innerPadding` dropped, or applied as container `Modifier.padding` around a lazy list | clips scrolling content; belongs in `contentPadding` (+ `consumeWindowInsets`) |
| `verticalScroll` / `LazyColumn` as a **non-weighted** child of a `Column` | infinite height constraint — scroll dies / lazy list crashes |
| explicit bottom `Spacer` inside a `ModalBottomSheet` | M3 already adds the inset — double gap |
| `BoxWithConstraints` width threshold or hand-rolled column math | use `currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(...)` — the same source as the nav suite |
| content stretched edge-to-edge on wide windows | cap at `AppSize.contentMaxWidth` and center |
| top bar without `TopAppBarDefaults.enterAlwaysScrollBehavior()` + `nestedScroll` | inconsistent with `FavoriteScreen`/`PopularScreen`/`RoversScreen` |

Breakpoint convention: **columns from MEDIUM** (`WIDTH_DP_MEDIUM_LOWER_BOUND` → `GridCells.Fixed(2)`),
**width cap from EXPANDED** (`WIDTH_DP_EXPANDED_LOWER_BOUND` → `widthIn(max = AppSize.contentMaxWidth)`).
See `RoversScreen.kt` (`RoversContent`) and `AboutScreen.kt`.

## 7. State / ViewModel boundary

- Derivation done in composition that belongs in the ViewModel — e.g. filtering or combining lists.
  `RoversViewModel.filteredRovers = combine(rovers, searchQuery)` is the pattern; screens read the
  derived `StateFlow` via `collectAsStateWithLifecycle()`.
- Parallel `isLoading` / `isError` / `isEmpty` booleans → a sealed interface for the state.
- Ephemeral UI-only flags (search field open, expanded/collapsed) may be hoisted locally with
  `remember` — they don't belong in the ViewModel. Actual data does.
- Business/domain data reaching UI as a presentation string, or presentation copy pushed down into a
  domain model — both are boundary violations (see §5).
- A widget callback typed against the whole screen event surface when it emits a bounded subset.

## Report

Per finding: severity (🔴 correctness · 🟡 should-fix · 🔵 nit), `path/File.kt:line`, the concrete
replacement, and the owning doc section. Review only — do not edit unless asked.

State which of §1–§7 were checked and clean on one line. If a section was skipped, say which and
why; a silent omission reads as a pass.

Finish by asking whether DESIGN_SYSTEM.md needs an update: a new reusable component, a new
`AppSize`/`AppSpacing`/`MaterialSymbol` entry, or a newly learned gotcha must be recorded there
(index + History entry) — that's a repo rule, not an optional nicety.

## When NOT to apply

- No `@Composable` / `@Preview` in the diff.
- Pure `theme/AppSpacing.kt` / `AppSize.kt` / `Theme.kt` / `AppColors.kt` / `AppMotion.kt` /
  `strings.xml` edits with no call-site change — the literal values there are the definitions.
- A deliberate, discussed divergence from the design system. Note it and move on; don't re-litigate.
