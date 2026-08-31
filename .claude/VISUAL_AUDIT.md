# Visual Audit — Mars Rover Photos (Android, real device SM-A505FM / API 30)

Branch: `Sirelon/kmp-migration`
Device: Samsung Galaxy A50 (R58M34G0DHE), 1080×2340, Android 11
Build: debug, versionName 3.0.0 (versionCode 48), Compose Multiplatform / Nav 3
Date: 2026-05-21

**Re-verified 2026-08-29** against the current branch, after the app went through a broader
redesign. Every `[FIX]` in the original "safe-to-fix" backlog is confirmed already resolved —
in most cases via that redesign rather than a targeted patch, so the original line references no
longer match. All three still-open `[ASK]` questions have since been decided; see each item below
and the "Open questions" section at the end for the current answer. Findings are left in place for
history, each tagged with its current status.

Legend:
- **[FIX]** — clear, low-risk visual fix; ready to be delegated to a fixer agent.
- **[ASK]** — needs your decision (visual change with multiple valid options, or behavior tied to product intent).
- **[IDEA]** — larger UX/visual improvement to consider in a future iteration.
- **✅ RESOLVED** — verified fixed in the current code (2026-08-29).
- **✅ DECIDED** — an `[ASK]` that now has an owner decision, whether or not it changed the code.

Cross-platform reminder for every fix: code lives in `shared/src/commonMain/...`. Don't hard-code pixel widths, screen-orientation-specific layouts, or system-bar paddings that break tablets / desktop / iOS. Prefer `WindowInsets`, `BoxWithConstraints`, `LocalConfiguration`, and theme tokens. Material 3 components used here are already cross-platform-friendly.

---

## 1. Rovers screen (start destination)

Source: `shared/.../presentation/screens/RoversScreen.kt`

### Findings
- **[FIX] Label/value pairs wrap awkwardly inside each card.** ✅ RESOLVED — replaced by `MetricStrip`, a `FlowRow` of `AppMetricItem(symbol, value, label)` cards; no more raw "Label: value" text that can wrap mid-phrase.
- **[FIX] Info `(i)` button overlaps the rover name.** ✅ RESOLVED — `TitleLine` wraps the name in a `FlowRow` with `.padding(end = AppSize.roverInfoReserve)`, reserving space so the top-end info `IconButton` never paints over it.
- **[ASK] Rover-image visual rounded shape vs. card shape.** ✅ DECIDED — align them. The rover thumbnail now clips to `MaterialTheme.shapes.large`, matching `AppCard`'s own radius (was `shapes.medium`, a visible mismatch).
- **[IDEA] Single-column list is fixed.** Still open — a responsive grid (1 col phone / 2 col tablet / 3 col desktop) would feel natural. Not scheduled.
- **[IDEA] Whole row tappable but no affordance.** Still open.
- **[IDEA] Palette mix.** ✅ DECIDED (superseded by the Mission-info palette decision below) — see item 3 there.
- **[ASK] Status for completed-but-active rovers with a stale `lastPhotoDate`.** ✅ DECIDED — leave as-is. `rover.status` is already the authoritative signal for mission status (confirmed: InSight now correctly shows "Complete", the exact case this question was raised about). A photo-date-age heuristic can't reliably distinguish "our data is stale" from "the rover is legitimately quiet but still active" (e.g. dust storms, conjunction blackouts), so a separate staleness indicator would risk being misleading rather than informative.

---

## 2. Mission-info screen — `(i)` on a rover row → `Mission(roverId)`

Source: `shared/.../presentation/screens/RoverMissionInfoScreen.kt`

### Findings
- **[FIX] "Mission Info" section shows `Failed to load mission facts`** even when the rest of the screen rendered fine. ✅ RESOLVED — the error state (`MissionFactsStatus.Error`) still exists in the ViewModel, but `MissionInfoSections.kt`'s rendering branches only on `factsLoading` / `missionFacts != null`; the section is silently hidden on failure rather than showing a red banner (option (a) from the original recommendation).
- **[FIX] Inconsistent number formatting.** ✅ RESOLVED — Rovers and Mission-info both call the same `formatCompact(totalPhotos)` helper; Sols/Earth Days use grouped `formatThousands` (e.g. `1,945`) instead of rounded `1.8K`/`1.9K`.
- **[ASK] Different palette than Rovers screen.** ✅ DECIDED — unify to Rovers' orange. `AppSectionHeader` (used for "Mission Timeline", "Statistics", "Cameras & Instruments", "Mission Info") now uses `MaterialTheme.colorScheme.secondary` instead of `tertiary`. `tertiary` (blue) remains in use elsewhere (`SegmentedControl`, some About-screen icons) — only the Mission-info section headers changed.
- **[IDEA] Mission Timeline visual.** Still open — no progress-fill animation between Landing and Current.
- **[IDEA] Camera list could optionally deep-link.** Still open.

---

## 3. Photos screen — tap a rover row

Source: `shared/.../presentation/screens/PhotosScreen.kt`

### Findings
- **[FIX] No screen title / no app bar.** ✅ RESOLVED — `AppTopBar` with `title = { Text(state.roverName) }` and `onBack` is in place.
- **[ASK] Sol/Earth date selectors look like centered text, not interactive controls.** ✅ DECIDED (resolved by redesign) — now `FloatingJumpChip`, a chip-styled `Surface` with a trailing chevron icon, clearly interactive.
- **[FIX] Photo captions duplicate the Sol that's already at the top.** ✅ RESOLVED — `shortCaption()` trims the caption to just the camera name.
- **[FIX] Floating "autorenew" button overlaps the bottom-right card.** ✅ RESOLVED — grid `contentPadding` reserves bottom space for the FAB, and the FAB has `contentDescription = "Jump to latest Sol"`.
- **[IDEA] Grid is 2-column fixed.** Still open — no adaptive `GridCells.Adaptive` yet.

---

## 4. Image pager / detail — tap a photo

Source: `shared/.../presentation/screens/ImagesScreen.kt`

### Findings
- **[FIX] TopAppBar title wraps to FOUR lines.** ✅ RESOLVED — title is `maxLines = 1, overflow = TextOverflow.Ellipsis`, and is now derived as `"Sol {sol} · {camera}"` rather than the raw NASA filename.
- **[FIX] "Mark as popular" / "Remove from popular" debug stub buttons shown side by side.** ✅ RESOLVED — removed entirely; no debug popular-toggle exists in `ImagesScreen.kt` or `ImageViewModel.kt` anymore.
- **[ASK] What should the title actually be?** ✅ DECIDED — option (1), `Sol {sol} · {camera}`.
- **[IDEA] Edge-to-edge experience.** Still open (translucent scrim on tap) — no change.

---

## 5. Photo info bottom sheet — info icon on Images

Source: `shared/.../presentation/screens/PhotoInfoBottomSheet.kt`

### Findings
- **[FIX] Earth Date shown as raw ISO timestamp.** ✅ RESOLVED — `formatEarthDate(iso)` renders e.g. `Mar 16, 2025 · 22:11 UTC`.
- **[FIX] Bottom of sheet may be clipped by system navigation bar.** ✅ RESOLVED — content `Column` has `.navigationBarsPadding()`.
- **[IDEA] All "Statistics" values are 0 for fresh installs.** Still open.

---

## 6. Favorites tab — empty state

Source: `shared/.../presentation/screens/FavoriteScreen.kt`

### Findings
- **[FIX] Layout-toggle button (grid icon) shown even when the list is empty.** ✅ RESOLVED — the toggle was removed outright rather than conditionally hidden; there's no grid/list layout switch anywhere in the current top bar.
- **[ASK] Empty illustration is a red planet with an alien face.** Still open / undecided — the shared `alien_icon` mascot asset is still used in `AppEmptyState`. No decision has been made to swap it.
- **[IDEA] Empty-state copy.** Still open.

---

## 7. Popular tab

Source: `shared/.../presentation/screens/PopularScreen.kt`

### Findings
- **[FIX] Heart icon under stats row floats with no label.** ✅ RESOLVED (superseded) — the screen was redesigned around ranked Hero/RunnerUp/Grid cards; there's no orphan stats row anymore. The favorite button now has `contentDescription = "Like"/"Unlike"`.
- **[FIX] Stats row icons have no visible labels.** ✅ RESOLVED (superseded) — each card shows a view-count badge (icon + adjacent visible count text), not a row of unlabeled icons.
- **[IDEA] Same responsive-grid suggestion as Photos.** Still open.

---

## 8. About tab

Source: `shared/.../presentation/screens/AboutScreen.kt`

### Findings
- **[FIX] `Email: mailto:...` shown literally.** ✅ RESOLVED — `LinkifyText` no longer exists; feedback is a plain "Send Feedback" row whose `onClick` opens the `mailto:` URI, never displayed as text.
- **[FIX] Theme picker uses three radio buttons inline.** ✅ RESOLVED (superseded) — replaced wholesale by `SegmentedControl`, a self-sizing pill control that doesn't stretch to fill width, so the "very wide on tablet" problem doesn't apply either.
- **[FIX] "Educational Facts" Show/Hide picker is two radio buttons.** ✅ RESOLVED — now a Material3 `Switch`.
- **[ASK] "Rate App" button — use in-app review flow?** ✅ DECIDED — yes. `AboutScreen.kt` calls `appReview.requestReview()` (Play Core `ReviewManager` on Android via an expect/actual `AppReview`), falling back to the store URL only if that returns `false`.
- **[IDEA] About screen content order.** Still open (minor).

---

## 9. Ukraine screen — tap top banner

Source: `shared/.../presentation/screens/UkraineScreen.kt`

### Findings
- **[FIX] No back/up affordance.** ✅ RESOLVED — `Scaffold`'s `topBar` renders `AppTopBar(title = { Text("Ukraine") }, onBack = onBack)`.
- **[FIX] Heading uses body styling.** ✅ RESOLVED — uses `AppTypography.appTitle`, which resolves to `headlineSmall`.
- **[ASK] Inline link `rashism.` — add underline?** ✅ DECIDED — yes, already applied (`TextDecoration.Underline`, along with the other inline links in the same screen).
- **[IDEA] Ukraine banner stays visible at the top of every screen.** Still open — intentional for now.

---

## Open questions for you (consolidated)

1. ~~Status for completed-but-active rovers...~~ ✅ DECIDED — leave as-is; `rover.status` is authoritative and a photo-date heuristic would be misleading.
2. ~~Rover-image rounded-corner radius vs. card shape...~~ ✅ DECIDED — aligned to `shapes.large`.
3. ~~Mission-info palette...~~ ✅ DECIDED — unified to Rovers' orange (`secondary`).
4. ~~Photos screen — Sol/Earth pickers...~~ ✅ DECIDED (resolved by redesign) — chip-styled `FloatingJumpChip`.
5. ~~Images screen — TopAppBar title...~~ ✅ DECIDED — `Sol {sol} · {camera}`.
6. **Favorites empty-state illustration — keep the alien planet or swap?** Still open.
7. ~~About — wire up Android in-app review...~~ ✅ DECIDED — yes, done.
8. ~~Ukraine screen `rashism.` link — add underline?~~ ✅ DECIDED — yes, done.

Only item 6 remains genuinely open.

---

## "Safe-to-fix" backlog (original)

All items below were confirmed ✅ RESOLVED on 2026-08-29 — no further action needed.

1. Rovers: label/value wrap; info-button overlap.
2. Mission-info: hide/retry the "Failed to load mission facts" red banner; unify number formatting with the Rovers list.
3. Photos: add screen title (rover name); trim caption duplication; FAB padding above grid; add `contentDescription` to the renew FAB.
4. Images: ellipsize title + short label derivation; either wire up or hide the debug Mark/Remove popular buttons.
5. PhotoInfoBottomSheet: format Earth date; add `navigationBarsPadding`.
6. Favorites: hide layout-toggle when list is empty.
7. Popular: clarify the orphan heart icon; add contentDescriptions for stat icons.
8. About: strip `mailto:` from displayed text; convert Facts radios to a `Switch`; flow-wrap the theme picker.
9. Ukraine: add a back arrow / top app bar; apply `headlineSmall` to the greeting.
