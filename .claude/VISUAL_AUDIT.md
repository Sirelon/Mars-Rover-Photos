# Visual Audit — Mars Rover Photos (Android, real device SM-A505FM / API 30)

Branch: `Sirelon/kmp-migration`
Device: Samsung Galaxy A50 (R58M34G0DHE), 1080×2340, Android 11
Build: debug, versionName 3.0.0 (versionCode 48), Compose Multiplatform / Nav 3
Date: 2026-05-21

Legend:
- **[FIX]** — clear, low-risk visual fix; ready to be delegated to a fixer agent.
- **[ASK]** — needs your decision (visual change with multiple valid options, or behavior tied to product intent).
- **[IDEA]** — larger UX/visual improvement to consider in a future iteration.

Cross-platform reminder for every fix: code lives in `shared/src/commonMain/...`. Don't hard-code pixel widths, screen-orientation-specific layouts, or system-bar paddings that break tablets / desktop / iOS. Prefer `WindowInsets`, `BoxWithConstraints`, `LocalConfiguration`, and theme tokens. Material 3 components used here are already cross-platform-friendly.

---

## 1. Rovers screen (start destination)

Source: `shared/.../presentation/screens/RoversScreen.kt`

### Findings
- **[FIX] Label/value pairs wrap awkwardly inside each card.** Right-hand column is only ~488 px wide on a 1080-px phone, so phrases like `Launch date from Earth: 2020-07-30` break mid-phrase: the orange label runs onto one line, and the value lands on the next. Visually messy and the cards become very tall.
  - Recommended fix: render each pair as a two-row block — orange label on row 1, black value on row 2, with `maxLines` set. Or use `FlowRow`-style alignment so the value sits inline only when there's room. Verify on tablet/desktop widths the result still looks intentional (not stuck in a 2-line mode forever).
- **[FIX] Info `(i)` button overlaps the rover name.** Bounds `[934,224][1060,350]` on Perseverance overlap the title's `[318,223][763,319]`. The info button is currently positioned absolutely. It should share the row with the title using weighted layout so the title shrinks (or ellipsizes) instead of being painted under the button.
- **[ASK] Rover-image visual rounded shape vs. card shape.** Image corners are rounded with a different radius than the row background. Want them aligned to the same shape token?
- **[IDEA] Single-column list is fixed.** On tablets/desktop the same list wastes ~half the screen. A responsive grid (1 col phone / 2 col tablet / 3 col desktop) would feel natural.
- **[IDEA] Whole row tappable but no affordance.** Add a subtle chevron or a clearer card border so users know the card itself is clickable (not just the (i) button).
- **[IDEA] Palette mix.** Brand orange on Rovers vs. a deep blue header on Mission-info; both screens belong to the same flow. Pick one accent and stick with it across navigation depths.

---

## 2. Mission-info screen — `(i)` on a rover row → `Mission(roverId)`

Source: `shared/.../presentation/screens/RoverMissionInfoScreen.kt`

### Findings
- **[FIX] "Mission Info" section shows `Failed to load mission facts`** (red error banner) even when the rest of the screen rendered fine. Either retry silently and hide the section if it permanently fails, or render a "Retry" affordance instead of a dead-end error. Investigate root cause: this likely fetches a `mission_facts` document from a backend that doesn't have an entry for this rover.
  - Recommended split: (a) hide the empty section by default; (b) only show an inline retry button when the error is transient (network). Don't show a scary red error for a chunk of content the user didn't even ask for.
- **[FIX] Inconsistent number formatting.** Rovers list shows `Total photos: 74525`. Mission info shows `Total Photos 74.5K`. Pick one (`74,525` is most readable). Make it a single helper used in both screens so they can't diverge.
- **[FIX] `1.8K sols` and `1.9K days` round-down readouts feel imprecise** for stats that the rovers-list screen already shows precisely. Either render `1,867 sols` / `1,945 days` or use locale-aware formatting. (Same helper as above.)
- **[ASK] Different palette than Rovers screen.** Headings are deep blue (`Mission Timeline`, `Statistics`, `Cameras`, `Mission Info`), labels inside cards are orange, body text is black. Either unify with the Rovers palette or commit to a deeper "data view" look. Which direction do you prefer?
- **[IDEA] Mission Timeline visual.** The dashed line between rocket/landing/current is a nice idea but currently has no animation or progress emphasis. Consider a filled progress segment between Landing and Current to convey "we are here now".
- **[IDEA] Camera list could optionally deep-link.** Tapping a camera (e.g. NAVCAM) could pre-filter the Photos screen by that camera. Big feature, not a fix.

---

## 3. Photos screen — tap a rover row

Source: `shared/.../presentation/screens/PhotosScreen.kt`

### Findings
- **[FIX] No screen title / no app bar.** Users coming from Rovers lose the context of which rover they're looking at. Add a thin `TopAppBar` with the rover name and a back arrow (rely on Nav 3 back), OR show a small breadcrumb above the date selectors.
- **[ASK] Sol/Earth date selectors look like centered text, not interactive controls.** The hierarchy reveals each is a `Button`. Without an outline or chip background, they read as labels, not pickers. Should we wrap each in an `OutlinedButton` (with a small `expand_more` icon) or use Material 3 `AssistChip`?
- **[FIX] Photo captions duplicate the Sol that's already at the top.** Caption: `Mars Perseverance Sol 1446: SkyCam Camera`. The screen header already says `Sol date: 1446`. Trim the caption to just the camera name (`SkyCam Camera`) so it occupies less vertical room.
- **[FIX] Floating "autorenew" button overlaps the bottom-right card** (bounds `[891,1815][1038,1962]` sits inside `[561,1789][1059,2004]`). Add bottom padding to the grid or float the FAB above the BottomBar with a sufficient `navigationBarsPadding`. Also, "autorenew" is the Material Symbols ligature name — the button has no label/contentDescription explaining what it does (random sol? refresh?). At minimum set `contentDescription`.
- **[IDEA] Grid is 2-column fixed.** Use a responsive `LazyVerticalGrid(GridCells.Adaptive(minSize = 160.dp))` so phones get 2, tablets 4, desktop 6.

---

## 4. Image pager / detail — tap a photo

Source: `shared/.../presentation/screens/ImagesScreen.kt`

### Findings
- **[FIX] TopAppBar title wraps to FOUR lines** because the title is `Mars image ID: WSM_1446_0795322185_000ECM_N0700000MEDA00000_0000LUJ` (literally the NASA filename). The title pushes the back arrow into the title area and squeezes save/share/info into the right edge. The fix is in `ImagesScreen.kt:140` and `:152`:
  - `Text(text = titleState, maxLines = 1, overflow = TextOverflow.Ellipsis)`
  - And consider stripping the prefix to just the ID, or showing a shorter "Sol 1446 · SkyCam" label derived from the photo metadata. The full ID belongs in the PhotoInfoBottomSheet (where it already is).
- **[FIX] "Mark as popular" and "Remove from popular" buttons are shown side by side, both as no-op debug stubs** (`ImagesScreen.kt:178-191`). They're gated by `BuildInfo.isDebug`, so production users don't see them, but they make the debug build hard to test. Either wire them up to the existing `viewModel` actions (preferred — code is already there in the repo) or hide them until they're wired. Showing both at once will always be wrong UX even when they do something, since the photo is either popular or not.
- **[ASK] What should the title actually be?** Options: (1) `Sol 1446 · SkyCam`, (2) Rover name (`Perseverance`), (3) Empty title with a chip overlay on the image. Pick one.
- **[IDEA] Edge-to-edge experience already implemented** (no banner/ad/bottom-bar — good!), but the TopAppBar is opaque white. On dark photos this looks heavy. Consider a translucent scrim that fades in on tap.

---

## 5. Photo info bottom sheet — info icon on Images

Source: `shared/.../presentation/screens/PhotoInfoBottomSheet.kt`

### Findings
- **[FIX] Earth Date shown as raw ISO timestamp `2025-03-16T22:11:53Z`.** Not human-readable. Format to e.g. `Mar 16, 2025 · 22:11 UTC` (use `kotlinx-datetime` formatting that's already in the project).
- **[FIX] Bottom of sheet may be clipped by system navigation bar.** When the sheet is expanded to max height, the lowest row of stats sits behind/under the gesture navigation pill. Add `navigationBarsPadding()` to the sheet content.
- **[IDEA] All "Statistics" values are 0 for fresh installs.** Either hide the section when all values are 0, or label it "No engagement yet — be the first!" so it doesn't look like a bug.

---

## 6. Favorites tab — empty state

Source: `shared/.../presentation/screens/FavoriteScreen.kt`

### Findings
- **[FIX] Layout-toggle button (grid icon) shown even when the list is empty.** Move it out of the top-right slot until there's content to lay out. Otherwise it looks like a broken action.
- **[ASK] Empty illustration is a red planet with an alien face** — friendly but feels off-brand for "Mars science" tone. Want to swap for a rover-themed illustration?
- **[IDEA] Empty-state copy** is decent but "Just mark them as 'favorite'." could become an inline tip with a small heart icon for context.

---

## 7. Popular tab

Source: `shared/.../presentation/screens/PopularScreen.kt`

### Findings
- **[FIX] Heart icon under stats row floats with no label.** Looks unfinished — looks like a 5th stat with a missing number. Either move it next to the stats row with its own count (favorites count) or make it a clear "Favorite this" CTA.
- **[FIX] Stats row icons have no visible labels.** Eye, magnifier, save, share — users have to guess which is which. Add tooltips/contentDescriptions and consider showing the metric name on tap.
- **[IDEA] Same responsive-grid suggestion as Photos** — 1-column list on phones is fine, but tablets/desktop should switch to a grid.

---

## 8. About tab

Source: `shared/.../presentation/screens/AboutScreen.kt`

### Findings
- **[FIX] `Email: mailto:sasha.sirelon@gmail.com` shown literally with the `mailto:` scheme** because `LinkifyText` (`AboutScreen.kt:215`) appends the raw URL as the display text. Pass a separate `displayText` + `link` so the user sees `sasha.sirelon@gmail.com` while the underlying intent uses `mailto:`. The Ukraine screen already shows it cleanly, so consistency is the bar.
- **[FIX] Theme picker uses three radio buttons inline (`White`, `Dark`, `System`)** without an "Auto" / `System` ringing more clearly as the recommended default. Visually fine on a phone; on a tablet the three columns become very wide. Wrap with a `FlowRow` so it can break onto a second row on narrow contexts and stretches less on wide ones.
- **[FIX] "Educational Facts" Show/Hide picker is two radio buttons; this is a binary state.** Use a `Switch` (Material 3) with the title — "Show 'Did you know?' facts" — and the description as supporting text. Saves a row of vertical space and is the standard pattern.
- **[ASK] "Rate App" button.** This sends users to the store. On Android we should use the in-app review flow if available before falling back to the store URL. Want me to add the in-app review path? (Requires `play-core` on Android only, no-op elsewhere.)
- **[IDEA] About screen content order.** Branding block at the top, settings (theme/facts) in the middle, "Clear Cache" / "Rate App" at the bottom, then legal. Current order roughly matches this — small polish only.

---

## 9. Ukraine screen — tap top banner

Source: `shared/.../presentation/screens/UkraineScreen.kt`

### Findings
- **[FIX] No back/up affordance.** No app bar at all. On phones it's discoverable via the system back gesture; on desktop/iOS without a back gesture it's a dead end. Add a `TopAppBar` with a back arrow that pops the route.
- **[FIX] Heading "Hello, I'm Oleksandr, a proud Ukrainian" uses body styling.** Apply `MaterialTheme.typography.headlineSmall` so it reads as a heading.
- **[ASK] Inline link `rashism.` is styled blue without underline by default Material link** — fine, but on a paragraph with so much weight it can be missed. Want me to add `TextDecoration.Underline`?
- **[IDEA] The Ukraine banner stays visible at the top of every other screen.** Consider a one-time dismissible banner with an "X" — or surface it as a slim ribbon (smaller font, shorter) so it doesn't dominate every screen-top. (This is a product decision; banner is intentional currently.)

---

## Open questions for you (consolidated)

1. Status for completed-but-active rovers (Curiosity/Insight/Perseverance show "active" alongside stale "Last photo date") — display as-is, or visually de-emphasize when `lastPhotoDate` is N months stale?
2. Rover-image rounded-corner radius vs. card shape — align both to the same shape token?
3. Mission-info palette — unify with Rovers orange, or keep the deeper blue "data view" look?
4. Photos screen — wrap Sol/Earth pickers in `OutlinedButton` or `AssistChip`?
5. Images screen — what should the TopAppBar title be? Sol+camera, rover name, or empty?
6. Favorites empty-state illustration — keep the alien planet or swap?
7. About — wire up Android in-app review for the Rate App button?
8. Ukraine screen `rashism.` link — add underline?

---

## "Safe-to-fix" backlog (will be delegated to fixer agents)

These are the items above marked **[FIX]** that don't depend on an open question:

1. Rovers: label/value wrap; info-button overlap.
2. Mission-info: hide/retry the "Failed to load mission facts" red banner; unify number formatting with the Rovers list.
3. Photos: add screen title (rover name); trim caption duplication; FAB padding above grid; add `contentDescription` to the renew FAB.
4. Images: ellipsize title + short label derivation; either wire up or hide the debug Mark/Remove popular buttons.
5. PhotoInfoBottomSheet: format Earth date; add `navigationBarsPadding`.
6. Favorites: hide layout-toggle when list is empty.
7. Popular: clarify the orphan heart icon; add contentDescriptions for stat icons.
8. About: strip `mailto:` from displayed text; convert Facts radios to a `Switch`; flow-wrap the theme picker.
9. Ukraine: add a back arrow / top app bar; apply `headlineSmall` to the greeting.
