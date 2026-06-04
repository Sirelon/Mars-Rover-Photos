# Design System — Mars Rover Photos

**Living document.** Update it whenever you add a reusable UI component, a token, or learn a
non-obvious UI/UX fact about this codebase. It exists so future sessions don't re-derive the
conventions or reinvent components under new names. Keep entries short and prescriptive.

The app is **Compose Multiplatform** (Android / iOS / Desktop; Web/WASM disabled). All shared UI
lives in `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/`:
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
  `lg`16 `xl`24 `xxl`32 `x3l`48). Never a bare `.dp` for spacing.
- **Component dimensions & corner radii** → `AppSize` (`theme/AppSize.kt`). Add a token here rather
  than inlining a size literal. If a needed value is neither a grid spacing nor a clear size, prefer
  extending `AppSize` over leaving a magic number.
- **Type** → `AppTypography` (`theme/AppTypography.kt`) semantic aliases over the M3 type scale, or
  `MaterialTheme.typography.*` directly (they resolve identically). Do **not** bake colors into type.
- **Color** → always `MaterialTheme.colorScheme.*`. **No hardcoded theme colors.** The only allowed
  exceptions are documented per-theme helpers that fill a missing M3 slot (see Insights). Apply text
  color at the call site so light/dark both work.

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
- The nav chrome (compact = bottom bar, medium/expanded = nav rail) is owned by **`MarsNavigationSuite`**
  (`navigation/MarsBottomBar.kt`). Screens render **content only** — do not add your own nav or a
  duplicate top bar for the primary destinations.
- For content that shouldn't stretch on wide windows, constrain to a centered max width
  (`AppSize` content-width token) rather than filling.

### Process
- Run `./gradlew detekt` before review; `./gradlew testDebugUnitTest` (JVM) and
  `:shared:desktopTest` run the shared logic tests. There is **no Compose UI-test infra** in `shared`
  yet — styling-only changes are verified by compile + visual smoke test, not unit tests.

---

## Insights / gotchas (the expensive-to-rediscover stuff)

- **Dark `surface` == `background` (`#121212`).** A card filled with `colorScheme.surface` is invisible
  against the screen in dark mode. Use a raised role (**`surfaceContainerHigh`**) for cards that must
  lift off the background. (`surfaceColorAtElevation` is **not** available in the M3 version pinned
  here — use the `surfaceContainer*` role family.)
- **No green slot in the M3 palette.** The "live/connected" green is resolved per applied theme via a
  documented luminance-based helper (`#5BBF86` dark / `#2E9E63` light), not a palette token.
- **Theme-aware hero tint.** The About hero gradient top must be resolved per theme (deep navy in dark,
  soft light-blue `#DCE8F6` in light) — a fixed navy looks wrong in light. Same luminance-helper pattern.
- **Material Symbols font is the full ~10MB variable file**, not a subset — all standard glyphs render;
  no need to fall back to vector paths for common icons.
- **Brand color overrides survive dynamic color.** `Theme.kt` re-applies brand-critical slots
  (secondary/coral, secondaryContainer, tertiary, primaryContainer) on top of Material You so
  design-system components stay on-brand. Read `Theme.kt` before touching colors.
- **Android in-app review silently no-ops in debug.** `AppReview.requestReview()` returns `true` even
  when nothing renders (Play throttling), so any "rate" flow needs a guaranteed store-listing fallback;
  ensure each platform shell passes a store URL.

---

## Component index

Stable design-system pieces (path = `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/`):

| Component | File | Notes |
| --- | --- | --- |
| `AppCard` | `ui/AppCard.kt` | **Elevated** card (2dp), 16dp radius. `AppFactCard` = secondaryContainer fact card. |
| `AppOutlinedCard` | `ui/AppOutlinedCard.kt` | **Non-elevated** grouped card: `surfaceContainerHigh` fill + hairline outline + 16dp radius. Exports `CardShape`. Use for grouped lists/surfaces; not a flag on `AppCard`. |
| `AppIconBox` | `ui/AppIconBox.kt` | Tinted rounded container holding a `MaterialSymbol` (tinted container + icon). General — use anywhere a colored icon tile is needed. |
| `AppBadge` / `StatusBadge` / `BadgeRow` | `ui/Badges.kt` | `AppBadge` = neutral outlined pill; `StatusBadge(label, color)` = colored dot + label (parameterized); `BadgeRow` = slot row composing them. |
| `SegmentedControl<T>` | `ui/SegmentedControl.kt` | Generic pill selector with an **animated sliding** indicator (measured bounds, ~200ms emphasized tween) + **swipe/drag** to change; tap retained. API: `options, selected, onSelect, label`. |
| `AppChip` | `ui/AppChip.kt` | secondaryContainer assist chip. |
| `AppButton` / `AppOutlinedButton` | `ui/AppButton.kt` | Brand buttons. |
| `MaterialSymbolIcon` + `MaterialSymbol` | `ui/MaterialSymbolIcon.kt` | Icon font; add glyphs to the enum. Default size `AppSize.iconDefault` (24dp). |
| `MarsSnackbar` | `ui/` | Branded snackbar host. |
| `MarsImage` | `ui/MarsImage.kt` | Coil-backed image. |
| `MarsNavigationSuite` | `navigation/MarsBottomBar.kt` | Adaptive bottom bar ↔ nav rail (compact ↔ medium/expanded). |
| `AppSpacing` | `theme/AppSpacing.kt` | 8dp-grid **spacing** tokens. |
| `AppSize` | `theme/AppSize.kt` | Component **dimension/radius** tokens (icon/card/hero/content-width, etc.). |
| `AppTypography` | `theme/AppTypography.kt` | Semantic type aliases. |
| `MarsRoverPhotosTheme` + palettes | `theme/Theme.kt` | Color schemes, brand overrides, dynamic color. |

> Settings-row primitives (`SettingsRow`, `SettingsSectionLabel`, `SettingsRowDivider`) live in
> `ui/SettingsComponents.kt` and compose the general pieces above; they're list-row-shaped, so they
> keep the `Settings*` name. The general building blocks (`AppIconBox`, `AppOutlinedCard`, badges) are
> reusable beyond settings.

**Adaptive centering:** to cap+center content only on large windows, use
`currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)`
(the same adaptive source as the nav suite) rather than a hand-rolled `BoxWithConstraints` threshold.
See `AboutScreen.kt` for the pattern (full-bleed hero + content capped at `AppSize.contentMaxWidth`).

---

## History
- 2026-06-03 — Created during the **About screen redesign**. Captured the dark surface==background,
  no-green-slot, theme-aware-hero, full-icon-font, and in-app-review insights; established the
  no-raw-`.dp` / `AppSize` rule and the "generalize, don't screen-prefix" component rule.
- 2026-06-03 — Backfilled the component index with the pieces the About redesign generalized:
  `AppIconBox`, `AppOutlinedCard`, `AppBadge`/`StatusBadge`/`BadgeRow`, animated+swipeable
  `SegmentedControl<T>`, and the `AppSize` token class. Added the adaptive-centering pattern
  (`windowSizeClass.isWidthAtLeastBreakpoint`).
