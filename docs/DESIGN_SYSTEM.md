# Design System — Mars Rover Photos

**Living document.** Update it whenever you add a reusable UI component, a token, or learn a
non-obvious UI/UX fact about this codebase. It exists so future sessions don't re-derive the
conventions or reinvent components under new names. Keep entries short and prescriptive.

The app is **Compose Multiplatform** for Android / iOS / Desktop. The repo also contains an
experimental `webApp/` WASM shell, but the shared `:shared` UI target is still disabled there. All
shared UI lives in `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/`:
- `theme/` — color scheme, tokens (spacing, sizes, typography)
- `ui/` — reusable design-system components (the `App*` family)
- `navigation/` — the adaptive nav shell
- `screens/` — feature screens (compose tokens + `ui/` components; do not put reusable widgets here)

Signature look: near-black **M3 dark theme** (`#121212`), steel-blue **primary** (`#385C8A`),
coral **accent/secondary** (`#FC6C4B`, "the Mars accent"). Light theme is white-based.

---

## Rules (prescriptive — follow these)

### Tokens, never raw literals
- **Spacing / padding / gaps** → `AppSpacing` (`theme/AppSpacing.kt`, 8dp grid: `xs`4 `sm`8 `md`12
  `lg`16 `xl`24 `xxl`32 `x3l`48). New layout spacing should use tokens; a few older reusable
  helpers still contain inline `dp` literals and should be normalized when touched.
- **Component dimensions & corner radii** → `AppSize` (`theme/AppSize.kt`). Add a token here rather
  than inlining a size literal. If a needed value is neither a grid spacing nor a clear size, prefer
  extending `AppSize` over leaving a magic number.
- **Type** → `AppTypography` (`theme/AppTypography.kt`) semantic aliases over the M3 type scale, or
  `MaterialTheme.typography.*` directly (they resolve identically). Do **not** bake colors into type.
- **Color** → always `MaterialTheme.colorScheme.*`. **No hardcoded theme colors.** The only allowed
  exceptions are documented per-theme helpers that fill a missing M3 slot (see Insights). Apply text
  color at the call site so light/dark both work.
- **Motion (durations / easing / shared-element specs)** → `AppMotion` (`theme/AppMotion.kt`). No raw
  `tween(600)`/`spring()` in screens or nav specs — route through `AppMotion` so the cross-screen fade
  and the shared-element bounds share one timing curve. See **Motion** below.

### Components
- Reuse the `App*` family in `presentation/ui/` before writing new UI. If you need a variant, prefer a
  new design-system component over a one-off in a screen file.
- **Cards:** `AppCard` = elevated (2dp), no border. The grouped/outlined card = hairline outline +
  `surfaceContainerHigh` fill, 16dp radius, **no elevation** — a *separate* component, never a flag on
  `AppCard`.
- **Naming:** design-system components are general and `App*`-prefixed (e.g. `AppCard`, `AppChip`,
  `AppButton`). Do **not** name an app-wide reusable component after the screen it first appeared in
  (no `Settings*`, `About*` for shared pieces). Screens own only screen-specific composition.
- **Icons:** `MaterialSymbolIcon` + the `MaterialSymbol` enum (`ui/MaterialSymbolIcon.kt`). Render via
  font ligatures. To add an icon, add an enum entry — the bundled font is the **full variable Material
  Symbols Outlined set**, so standard glyphs are present (verify the ligature name if unsure).

### Adaptive layout & navigation
- The **navigation** chrome (compact = bottom bar, medium/expanded = nav rail) is owned by
  **`MarsNavigationSuite`** (`navigation/MarsBottomBar.kt`). Don't add your own bottom bar / nav rail.
- A screen **does** own its own `Scaffold` + **`AppTopBar`** (title / subtitle / actions, with
  `TopAppBarDefaults.enterAlwaysScrollBehavior()` via `nestedScroll`) — that's the title bar, distinct
  from the nav chrome above. See `FavoriteScreen.kt`, `PopularScreen.kt`, `RoversScreen.kt`. Use
  `contentWindowInsets = WindowInsets()` and consume `innerPadding` in the content.
- For content that shouldn't stretch on wide windows, constrain to a centered max width
  (`AppSize` content-width token) rather than filling.

### Process
- Run `./gradlew detekt` before review; `./gradlew testDebugUnitTest` (JVM) and
  `:shared:desktopTest` run the shared logic tests. There is **no Compose UI-test infra** in `shared`
  yet — styling-only changes are verified by compile + visual smoke test, not unit tests.

### Motion & shared-element transitions
- **Tokens:** `AppMotion` (`theme/AppMotion.kt`) — `ScreenEnterMs`/`ScreenExitFadeMs` (standard slide
  nav), `SharedContainerMs` (photo grid/list ↔ fullscreen viewer), `Emphasized` easing, and
  `PhotoBoundsTransform`/`FavoriteBoundsTransform`. The container fade and the shared-bounds morph
  **must share one duration** (`SharedContainerMs`) or they desync.
- **Photo ↔ viewer container transform:** apply `Modifier.sharedPhoto(id)` (grid/list item) and
  `Modifier.sharedFavorite(id)` (corner heart) from `ui/SharedPhotoTransition.kt` — the single source of
  truth for keys (`photo_<id>` / `photo_favorite_<id>`), resize mode (`scaleToBounds(Fit)` — the two ends
  are different crops, so it crossfades rather than morphs), bounds spec, and overlay clip. They no-op
  outside a `SharedTransitionLayout`+`NavDisplay`, so previews don't crash.
  - The pager applies them only to the **settled page** (`enabled = pagerState.settledPage == page`).
  - The heart flies only where a source exists (the rover Photos grid). Elsewhere
    (Favorite/Popular/deep-link) use `Modifier.navFadeEnter()` so it fades instead of popping under the
    viewer's `EnterTransition.None`.
- **Instant placeholder (no blank flash):** the source screen **writes** its thumbnail under a shared
  Coil key via `NetworkImage(cacheKey = "photo_<id>")`; the viewer **reads** it via `placeholderCacheKey`.
  Writer and reader are **separate params** (`cacheKey` vs `placeholderCacheKey`) — never the same key on
  both ends, or the thumb/full-res entries ping-pong while both are composed during the transition.
- **Progressive res:** the viewer loads `~large` first (`nasaImageLargeUrl`) and upgrades to `~orig`
  (`nasaImageOrigUrl`) only on zoom; `~large` is cached under `photo_large_<id>` so the upgrade is
  flash-free.
- **Where the nav specs live:** the Photos→viewer **open** fade is on the `Images` nav entry
  (`di/NavigationModule.kt`); the **close / predictive-pop** fade is on the `NavDisplay`
  (`navigation/AppNavigation.kt`). Both use `AppMotion.SharedContainerMs`; standard slide nav uses
  `ANIM_DURATION`/`ANIM_DURATION_2` (sourced from `AppMotion`).
- **Caveats:** the placeholder only hits while the thumbnail is memory-resident (far-scrolled returns
  may miss → drawable fallback). An explicit `memoryCacheKey` decouples the cache entry from size — watch
  the **adaptive Favorite staggered grid** (variable width) for a re-decode loop. Compose shared
  transitions don't honor system **reduced-motion** (out of scope, tracked separately).

---

## Insights / gotchas (the expensive-to-rediscover stuff)

- **Dark `surface` == `background` (`#121212`).** A card filled with `colorScheme.surface` is invisible
  against the screen in dark mode. Use a raised role (**`surfaceContainerHigh`**) for cards that must
  lift off the background. (`surfaceColorAtElevation` is **not** available in the M3 version pinned
  here — use the `surfaceContainer*` role family.)
- **No green slot in the M3 palette.** The "live/connected" green is resolved per applied theme via the
  documented luminance-based helper **`activeStatusColor()`** (`theme/AppColors.kt`; `#5BBF86` dark /
  `#2E9E63` light), not a palette token. Use the helper — never a literal green.
- **Theme-aware hero tint.** The About hero gradient top must be resolved per theme (deep navy in dark,
  soft light-blue `#DCE8F6` in light) — a fixed navy looks wrong in light. Same luminance-helper pattern.
- **Material Symbols font is the full ~10MB variable file**, not a subset — all standard glyphs render;
  no need to fall back to vector paths for common icons.
- **Brand color overrides survive dynamic color.** `Theme.kt` re-applies brand-critical slots
  (secondary/coral, secondaryContainer, tertiary, primaryContainer) on top of Material You so
  design-system components stay on-brand. Read `Theme.kt` before touching colors.
- **`./gradlew detekt` does NOT lint `shared/commonMain`.** It only sources `androidApp` + root, so
  shared-module style violations (raw `.dp`, hardcoded theme colors) are **not** caught automatically.
  Verify shared UI by reading the diff for those rules + `:shared:compileKotlinDesktop` — a clean
  detekt run is not evidence the shared code is clean.
- **Android in-app review silently no-ops in debug.** `AppReview.requestReview()` returns `true` even
  when nothing renders (Play throttling), so any "rate" flow needs a guaranteed store-listing fallback;
  ensure each platform shell passes a store URL.

---

## Component index

Stable design-system pieces (path = `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/`):

| Component | File | Notes |
| --- | --- | --- |
| `AppCard` | `ui/AppCard.kt` | **Elevated** card (2dp), 16dp radius. Optional `onClick` makes it interactive: the card owns the `clickable` + a desktop **hover-lift** (`cardElevationResting`→`cardElevationHover` via `animateDpAsState`) internally — pass `onClick` rather than wrapping the card in your own `clickable`/elevation. Null `onClick` ⇒ non-interactive and honors the caller's `elevation` param (default resting; e.g. a 4dp header card). `AppFactCard` = secondaryContainer fact card. |
| `AppOutlinedCard` | `ui/AppOutlinedCard.kt` | **Non-elevated** grouped card: `surfaceContainerHigh` fill + hairline outline + 16dp radius. Exports `CardShape`. Use for grouped lists/surfaces; not a flag on `AppCard`. |
| `AppIconBox` | `ui/AppIconBox.kt` | Tinted rounded container holding a `MaterialSymbol` (tinted container + icon). General — use anywhere a colored icon tile is needed. |
| `AppMetricItem` | `ui/AppMetricItem.kt` | Inline icon + value + label trio in one `Row` (e.g. "🖼 134K photos"). Icon/label = `onSurfaceVariant`, value = `onSurface` (SemiBold `bodyMedium`); inline icon size. Repeats 3× in a rover row's metric strip; general anywhere a compact metric is needed. |
| `AppBadge` / `StatusBadge` / `BadgeRow` | `ui/Badges.kt` | `AppBadge` = neutral outlined pill; `StatusBadge(label, color)` = colored dot + label (parameterized); `BadgeRow` = slot row composing them. |
| `SegmentedControl<T>` | `ui/SegmentedControl.kt` | Generic pill selector with an **animated sliding** indicator (measured bounds, ~200ms emphasized tween). Drag to slide through options continuously; tap to jump. Uses `draggable` + `rememberDraggableState` — never cancelled mid-gesture by recomposition. API: `options, selected, onSelect, label`. |
| `AppChip` | `ui/AppChip.kt` | secondaryContainer assist chip. |
| `AppButton` / `AppOutlinedButton` | `ui/AppButton.kt` | Brand buttons. |
| `MaterialSymbolIcon` + `MaterialSymbol` | `ui/MaterialSymbolIcon.kt` | Icon font; add glyphs to the enum. Default size `AppSize.iconDefault` (24dp). |
| `AppTopBar` | `ui/AppTopBar.kt` | Shared top app bar with optional subtitle, back handling, and custom nav/action slots. |
| `AppEmptyState` | `ui/AppEmptyState.kt` | Shared empty/error state with optional alien mascot and action slot. |
| `AppFloatingActionButton` | `ui/AppFloatingActionButton.kt` | Branded FAB that defaults to the Mars accent (`colorScheme.secondary`). |
| `MarsSnackbar` | `ui/widgets.kt` | Branded snackbar host. |
| `MarsImage` | `ui/MarsImage.kt` | Coil-backed image. |
| `PlatformDatePickerDialog` | `ui/PlatformDatePicker.kt` | Compose-only cross-platform date picker dialog with min/max range support. |
| `CenteredColumn` / `CenteredProgress` | `ui/CenteredComponents.kt` | Shared centering primitives for loading and empty states. |
| `MarsNavigationSuite` | `navigation/MarsBottomBar.kt` | Adaptive bottom bar ↔ nav rail (compact ↔ medium/expanded). |
| `AppSpacing` | `theme/AppSpacing.kt` | 8dp-grid **spacing** tokens. |
| `AppSize` | `theme/AppSize.kt` | Component **dimension/radius** tokens (icon/card/hero/content-width, etc.). |
| `AppTypography` | `theme/AppTypography.kt` | Semantic type aliases. |
| `AppMotion` | `theme/AppMotion.kt` | Motion **tokens**: durations (screen slide, shared container), `Emphasized` easing, `Photo`/`FavoriteBoundsTransform`. Route all transition timing through here. |
| `sharedPhoto` / `sharedFavorite` / `navFadeEnter` | `ui/SharedPhotoTransition.kt` | `Modifier` extensions for the photo grid/list ↔ viewer shared-element transition (keys, resize mode, bounds, overlay clip). Null-safe (no-op in previews). See **Motion**. |
| `MarsRoverPhotosTheme` + palettes | `theme/Theme.kt` | Color schemes, brand overrides, dynamic color. |
| `activeStatusColor()` | `theme/AppColors.kt` | Theme-aware "active / live / connected" green (`@Composable @ReadOnlyComposable`). Luminance-based: `#5BBF86` dark / `#2E9E63` light. Fills the **no-green-slot** gap (see Insights); use instead of a literal. Generalized out of the old AboutScreen-local `liveColor`. |

> Settings-row primitives (`SettingsRow`, `SettingsSectionLabel`, `SettingsRowDivider`) live in
> `ui/SettingsComponents.kt` and compose the general pieces above; they're list-row-shaped, so they
> keep the `Settings*` name. The general building blocks (`AppIconBox`, `AppOutlinedCard`, badges) are
> reusable beyond settings.

**New `MaterialSymbol` glyphs (Rovers):** `Collections`, `Schedule`, `Event`, `Search` — added to the
enum in `ui/MaterialSymbolIcon.kt` (the bundled full variable font renders them).

**New `AppSize` tokens (Rovers):** `roverThumbWidth` (112dp portrait-thumb fixed width),
`roverInfoReserve` (40dp title-line trailing reserve clearing the overlaid info button),
`cardElevationResting` (2dp) / `cardElevationHover` (6dp) for the hover-lift.

**Number formatting** (`utils/NumberFormat.kt`): `formatThousands` → grouped (`74,525`) for exact
counts; `formatCompact` → abbreviated K/M (`1.5K`, `134K`, `1.2M`) for tight metric strips. Use
`formatCompact` in dense/inline metric contexts (rover `AppMetricItem`s), `formatThousands` where the
full number reads better.

**Adaptive layout (one source, two breakpoints):** drive both column count and width-cap from
`currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(...)` (the same adaptive source
as the nav suite) — never a hand-rolled `BoxWithConstraints` threshold or column math.
- **Columns:** `isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)` → `GridCells.Fixed(2)` on
  medium+expanded, else `Fixed(1)` on compact. (Rovers grid.)
- **Width cap:** `isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)` → `widthIn(max =
  AppSize.contentMaxWidth)` and center, else `fillMaxWidth()`. (Cap only in EXPANDED.)
See `RoversScreen.kt` (`RoversContent`) and `AboutScreen.kt` (full-bleed hero + capped content) for
the pattern.

**Rovers chrome (`RoversContent`):** a `Scaffold` + `AppTopBar` with
`TopAppBarDefaults.enterAlwaysScrollBehavior()` (the `FavoriteScreen` pattern) — not a scrolling
header `Column`. The top bar shows the title (`rovers_title`) + a counts subtitle
(`rovers_subtitle_fmt`, computed from the FULL `allRovers` list) and a trailing **search
`IconButton`**. The grid consumes `innerPadding` (`consumeWindowInsets` + top/bottom `calculateXPadding()`),
keeping the width-cap above. **Toggle search:** search is hidden by default; tapping the search action
flips a hoisted `searchActive` flag that swaps the title slot for an inline single-line
`OutlinedTextField` (focused via `FocusRequester` + `LaunchedEffect`) and the action for a **close
`IconButton`** that clears the query and exits. Filtering still lives in the VM (`filteredRovers`).

---

## Rovers — Variant A "Refined List" row

The rover list row (`screens/RoversScreen.kt` → `RoverItem`). Anatomy, top→bottom / left→right:

- **Base:** `AppCard` (elevated, large radius). The whole card is clickable via `AppCard(onClick = …)`
  → `onNavigateToPhotos` (the card owns the `clickable` + hover-lift; the row no longer wraps it).
- **Thumbnail:** full-height portrait `Image`, fixed width `AppSize.roverThumbWidth`,
  `ContentScale.Crop`, clipped to `shapes.medium`. The row uses `height(IntrinsicSize.Min)` so the
  thumb fills the text column's height.
- **Title line:** a `FlowRow` of the coral rover name (`colorScheme.secondary`, `titleLarge`, 1 line +
  ellipsis) followed by an Active/Complete `StatusBadge` — `FlowRow` (not `Row`) so a long name + chip
  wrap gracefully instead of squeezing. The line reserves `AppSize.roverInfoReserve` trailing padding
  so the name/chip never collide with the overlaid info button.
- **Status chip:** `StatusBadge(label, color)`. `rover.status.equals("active", ignoreCase = true)` →
  label "Active" + `activeStatusColor()`; otherwise "Complete" + `onSurfaceVariant` (neutral). Derive
  from `rover.status` (case-insensitive) — never hardcode the label, never a literal green.
- **Blurb:** 2-line `bodyMedium` / `onSurfaceVariant`, ellipsized, from `Rover.blurb()` (presentation
  string resource — see gotcha below).
- **Metric strip:** a top hairline `HorizontalDivider` (`AppSize.hairline` / `colorScheme.outline`)
  then a `FlowRow` of EXACTLY three `AppMetricItem`s — Photos (`totalPhotos` via `formatCompact`),
  Sols (`maxSol` via `formatCompact`), Last date (`maxDate` via `formatDisplayDate`).
- **Info affordance:** an `IconButton` (info glyph) `align(Alignment.TopEnd)` as an overlay in the
  row's `Box` — **outside** the card's `clickable` — so tapping info fires `onMissionInfoClick` and
  does NOT trigger the row tap. (Tap isolation = separate clickable target, not event consumption.)
- **Hover lift:** owned by `AppCard` itself when `onClick` is passed — `MutableInteractionSource` +
  `collectIsHoveredAsState` drive `animateDpAsState` between `AppSize.cardElevationResting` and
  `cardElevationHover`, with desktop hover + all-platform press feedback via `LocalIndication`. The row
  just hands `AppCard` an `onClick`; it no longer manages the interaction source/elevation locally.

**Gotchas:**
- `roverInfoReserve` (40dp) is **hand-tuned** to clear the 48dp info `IconButton` overlay — it is not
  derived from the icon-button size (deferred to follow-ups). Re-tune if the info button size changes.
- Rover **mission blurbs are presentation-only** localized string resources, keyed on `RoverConstants`
  ids in `ui/RoverBlurb.kt` (`Rover.blurb()` / `blurbResource()`) — **NOT** domain data. Never add a
  blurb field to the `Rover` model or the data/API layer.
- Rover **search is VM-derived**, not filtered in composition: `RoversViewModel` exposes
  `filteredRovers` as `combine(rovers, searchQuery)`. Screens read the derived `StateFlow`.

### Handoff design-token → app-token mapping

The design bundle's `colors_and_type.css` is a mirror of the app, not the source — it states
*"Source of truth: Theme.kt"*. When implementing from the handoff, map design tokens back to app
tokens rather than re-introducing literals:

| Design token (`colors_and_type.css`) | App token |
| --- | --- |
| `--accent` / `--t-accent` `#FC6C4B` (coral) | `colorScheme.secondary` |
| `--t-active` `#5BBF86` dark / `#2E9E63` light | `activeStatusColor()` |
| `--primary` `#385C8A` (steel blue) | `colorScheme.primary` |
| `--space-*` (8dp grid) | `AppSpacing` |
| `--radius-*` | `MaterialTheme.shapes` (small/medium/large) / `AppSize` radii |
| M3 type scale (`--body-medium`, `--title-large`, …) | `MaterialTheme.typography.*` / `AppTypography` |

---

## History
- 2026-06-03 — Created during the **About screen redesign**. Captured the dark surface==background,
  no-green-slot, theme-aware-hero, full-icon-font, and in-app-review insights; established the
  no-raw-`.dp` / `AppSize` rule and the "generalize, don't screen-prefix" component rule.
- 2026-06-03 — Backfilled the component index with the pieces the About redesign generalized:
  `AppIconBox`, `AppOutlinedCard`, `AppBadge`/`StatusBadge`/`BadgeRow`, animated+swipeable
  `SegmentedControl<T>`, and the `AppSize` token class. Added the adaptive-centering pattern
  (`windowSizeClass.isWidthAtLeastBreakpoint`).
- 2026-06-04 — Fixed `SegmentedControl` drag gesture on iOS CMP: replaced `pointerInput`/
  `awaitEachGesture` with `draggable` + `rememberDraggableState`. Root cause: `pointerInput` keys
  included `offsets.toList()`, so each recomposition triggered by `onSelect` restarted the gesture
  coroutine mid-drag. `rememberDraggableState` is stable across recompositions and immune to this.
- 2026-06-07 — **Rovers redesign (Variant A "Refined List")**. Generalized `activeStatusColor()` into
  `theme/AppColors.kt` (was AboutScreen-local `liveColor`) and added `AppMetricItem` to the index.
  Added `MaterialSymbol` glyphs (Collections/Schedule/Event/Search), `AppSize` tokens
  (`roverThumbWidth`, `roverInfoReserve`, `cardElevation{Resting,Hover}`), and `formatCompact`
  (`utils/NumberFormat.kt`). New "Rovers — Variant A row" section (row anatomy, tap isolation,
  hover-lift, blurb/search provenance) + the handoff token-mapping table. Generalized the
  adaptive-centering note into the columns-from-MEDIUM / cap-from-EXPANDED pattern. Recorded that
  `./gradlew detekt` does not lint `shared/commonMain`.
- 2026-06-15 — **Shared-element photo transition refactor.** Added `AppMotion` motion tokens and the
  `sharedPhoto`/`sharedFavorite`/`navFadeEnter` `Modifier` extensions (`ui/SharedPhotoTransition.kt`),
  centralizing the grid/list ↔ fullscreen-viewer container transform (was duplicated inline in
  `PhotosScreen`/`ImagesScreen`). Fixed the blank-flash root cause by splitting `NetworkImage` into a
  writer `cacheKey` + reader `placeholderCacheKey`; made `LocalSharedTransitionScope` nullable/`static`
  (kills the dead `else` branch + preview crash); coordinated the open (`NavigationModule`) and pop
  (`AppNavigation`) fades with the bounds via `AppMotion.SharedContainerMs`; extended the effect to
  Favorite/Popular (`MarsImageComposable`); added `~large`→`~orig` progressive loading
  (`nasaImageLargeUrl`). Reviewed by a 2-member committee before implementation.
