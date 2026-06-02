# Maestro + App Navigation Notes — for future AI sessions

Audience: future Claude/Codex/Gemini sessions that need to drive the Mars Rover Photos app via Maestro MCP and reason about navigation. Captured 2026-05-21 on this app version (Compose Multiplatform + Navigation 3).

## TL;DR setup

- App id: `com.sirelon.marsroverphotos`
- Connected device id (real): `R58M34G0DHE` (Samsung A50, Android 11).
- Install fresh debug build with `./gradlew :androidApp:installDebug` if commits are newer than `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. The Gradle JVM warmup is the slow part; ~1 min on this machine.
- Launch via Maestro:
  ```yaml
  - launchApp:
      appId: com.sirelon.marsroverphotos
      clearState: true
  - waitForAnimationToEnd
  ```
- Then call `inspect_screen` before every interaction. Don't author `tapOn: "..."` strings from screenshots — copy them verbatim from the hierarchy `txt` field.

## App navigation map (Nav 3)

Source of truth: `shared/.../presentation/navigation/AppNavigation.kt` + `AppDestinations.kt`.

```
Rovers (start)
 ├─ tap rover row    → Photos(roverId)
 │                       └─ tap photo → Images(photoIds, selectedId)
 │                                        └─ "info" icon → PhotoInfoBottomSheet (modal)
 ├─ tap (i) on row   → Mission(roverId)
 └─ tap Ukraine top  → Ukraine

Bottom bar (top-level)
  Rovers | Favorites | Popular | About
```

Notes:
- `Images` is intentionally edge-to-edge — the Ukraine banner, ad slot, and bottom bar are hidden while on it (see `AppNavigation.kt` `isImages` logic).
- All other top-level destinations show the Ukraine banner unless you are already on `Ukraine`.

## Maestro gotchas hit (and fixes)

### 1. Material Symbols come through as `txt: "info"` / `txt: "favorite"` etc.

Icons are rendered as `MaterialSymbolIcon` which is a `Text` composable using a Material Symbols font. In the view hierarchy they appear as plain TextViews whose `txt` is the symbol's ligature name (`info`, `favorite`, `local_fire_department`). This is great for selectors — `tapOn: { text: "info" }` works — but be careful:
- A screenshot shows a glyph, the hierarchy shows the ligature name. Trust the hierarchy.
- Several rows can have the same `txt: "info"`; use `index:` or anchor with `below`/`rightOf` against a unique nearby label (rover name).

### 2. `text:` matcher is full-string regex with IGNORE_CASE

Per Maestro docs reaffirmed in the MCP inspect_screen output: partial strings do not match. To tap "Perseverance" use the full string, not "Persever". To match a date like `2026-05-21` anchor with `.*` if needed.

### 3. Nav bar uses BottomBar items as `selected` View elements with text labels

Each bottom tab shows up as `txt: "Rovers" | "Favorites" | "Popular" | "About"`. Tapping by text is reliable. The "active" tab is a sibling `View` with `selected: true` but no helpful resource-id.

### 4. The top-level "Ukraine" banner is a single clickable View

```
{"b":"[0,80][1080,202]", "clickable":true, c:[{"txt":"#Stand with Ukraine", ...}]}
```
Tap by `text: "#Stand with Ukraine"` (escape `#` is not needed for Maestro regex but quote the string). The banner disappears while on the `Images` route.

### 5. There's a Samsung-only "Панелі Edge" floating handle ImageView

Bounds `[1013,438][1080,780]`, resource-id `com.samsung.android.app.cocktailbarservice:id/trigger_layout_container`. It's an OS overlay (Edge Panels), not part of the app. Ignore it; do not author selectors against it. It can sometimes intercept taps near the right edge — use `tapOn` with explicit coordinates well inside the screen, or `swipe` from x < 980.

### 6. Status bar locale leaks into a11y strings

System a11y strings come through in the device language (Ukrainian here: "Назад", "Додому", "Сповіщення..."). Don't rely on system a11y text in selectors; only use elements from the `ComposeView` subtree.

### 7. `clearState: true` resets nav back stack and KMP-side state

Use it at the start of every audit run to start at `Rovers` reliably. Without it you can land mid-flow if the user left the app on Images.

### 8. Compose nodes are flat `android.view.View`s in the hierarchy

Don't expect Compose-specific class names. Each composable becomes a `View` (or `TextView` for `Text`). Resource-ids are almost never set; rely on `text:` or position-based matchers.

## Recommended exploration pattern

1. `launchApp` with `clearState: true`.
2. `inspect_screen` → take screenshot if needed.
3. Note unique `txt` strings of clickable elements to use as next-step selectors.
4. Drive forward one screen at a time; re-inspect after every transition.
5. Use `pressKey: Back` (system back) instead of looking for an in-app back arrow when in doubt — Nav 3 handles back via `navigator.goBack()`.

## Known dead-ends / things to verify

- Deep links: `DeepLink.Rover/Photo/Image` are handled in `AppNavigation`. To exercise them through Maestro use:
  ```yaml
  - openLink: marsrover://photo/<id>  # see DEEP_LINKING.md for exact schemes
  ```
  Confirm schemes in `androidApp/src/main/AndroidManifest.xml` before depending on it.
- The PhotoInfoBottomSheet is a modal sheet — it appears over Images without a route change. `inspect_screen` shows both the underlying pager and the sheet content; use the sheet's unique fields to disambiguate.
- `PlaceholderScreens.kt` defines a `private` composable; there are no live placeholder routes in this build.

## Files to look at when something doesn't tap

- `shared/.../presentation/navigation/AppNavigation.kt` — top-level chrome, Ukraine banner, ad slot, bottom bar visibility rules.
- `shared/.../presentation/navigation/MarsBottomBar.kt` — bottom-bar items and labels (string resources are the selectors).
- `shared/.../presentation/screens/*.kt` — per-screen composables.
- `shared/.../presentation/ui/MaterialSymbolIcon.kt` — explains why icons show up as text in the hierarchy.

## Per-screen selector cookbook (verified 2026-05-21)

Use these as starting points; always re-`inspect_screen` after a transition.

### Rovers (start destination)
- Tap a rover row → Photos: `tapOn: "Perseverance"` (or any other rover name from the visible list).
- Tap the info `(i)` button → Mission info: the icon text is `info`. There are multiple `info` TextViews on screen (one per rover). Use `index:` matching the row position.
  - Example for Perseverance: `tapOn: { text: "info", index: 0 }`.
- Tap the top Ukraine banner: `tapOn: "#Stand with Ukraine"`.

### Mission info
- Back: `pressKey: Back` works reliably. There is also an `arrow_back` TextView (Material Symbol) inside the in-app top bar — `tapOn: "arrow_back"` is fine but be aware multiple `arrow_back` views exist when you're on Images.
- Headers (`Mission Timeline`, `Statistics`, `Cameras`, `Mission Info`) are plain `TextView`s — useful anchors for scroll assertions.

### Photos
- Tap a photo → Images: `tapOn: "Mars Perseverance Sol 1446: SkyCam Camera.*"` (regex/.* tail is necessary because the on-device text has a trailing space — `"Mars Perseverance Sol 1446: SkyCam Camera "`). This is one of the cases where matching the literal string with no `.*` will fail.
- The Sol/Earth date pickers are siblings — both contain a multi-line `txt: "Sol date: \n1446"` / `"Earth date: \n2025-03-14"`. To tap the picker, target the **container** by anchoring with `containsChild`:
  ```yaml
  - tapOn:
      containsChild:
        text: "Sol date.*"
  ```
- The renew/random FAB is `txt: "autorenew"` and is the only one on screen. Tap with `tapOn: "autorenew"`.

### Images (edge-to-edge)
- `arrow_back` (back), `save`, `share`, `info` are all TextViews — all are Material Symbol ligatures. Use `index:` because some screens repeat them.
- The favorite toggle at the bottom is `txt: "favorite"`. It's a CheckBox in the hierarchy (`cls: "android.widget.CheckBox"`), so `assertChecked` can verify state.
- "Mark as popular" / "Remove from popular" are debug-only buttons (gated by `BuildInfo.isDebug`). They will not appear on release builds — your selectors should not depend on them.

### PhotoInfoBottomSheet
- Title: `txt: "Photo Information"`. Use it as the unique anchor for "is the sheet open?" assertions.
- Sections inside the sheet (`Description`, `Credit`, `Camera`, `Sol`, `Earth Date`, `Statistics`) are independent TextViews — good for sub-section scroll/assert.
- Dismiss via `pressKey: Back` (sheet eats the back key first). The drag handle is non-clickable.

### Favorites
- Title: `txt: "My favorite Mars photos"`.
- Empty-state CTA: `tapOn: "Go to rovers"` — useful to bounce back to the Rovers route.
- Top-right grid-toggle button has no text; it has bounds in the top-right area only.

### Popular
- Title: `txt: "The most popular Mars photos"`.
- Each card has stats `views`, `zoom_in`, `save`, `share` icons (ligature names) + numbers — use the numbers as anchors when filtering.

### About
- Anchors: `txt: "Mars Rover Photos"`, `txt: "Version: 3.0.0"`, `txt: "Change the app theme"`, `txt: "Educational Facts"`.
- Theme radios: `White`, `Dark`, `System` are radio button labels. Tap each by text.
- "Clear Cache" and "Rate App" are buttons with literal labels.

### Ukraine
- Anchors: `txt: "Hello, I'm Oleksandr, a proud Ukrainian"`, `txt: "Thank you!"`, `txt: "Glory to Ukraine"`.
- The bottom banner ("Glory to Ukraine") is rendered as overlay text on top of the blue/yellow gradient — it has its own TextView.
- There is **no in-app back arrow** on this route currently. Use `pressKey: Back`.

## Tips that saved time during this session

- **Always run with `clearState: true`** as the first command of an inspect/audit run. The Photos screen retains the last-viewed Sol on warm start, so without `clearState` you might land on a completely different photo set than expected.
- **Use `scroll` two or three times** rather than `swipe` for content scans — `scroll` is content-aware and stops at the end; `swipe` over-shoots.
- **Don't trust screenshots for selector authoring.** Many "icons" on screen are actually `TextView`s holding Material Symbol ligatures. The view hierarchy is the source of truth.
- **Treat the Samsung "Edge Panel" handle on the right edge as a trap.** Use coordinates < x=980 when tapping near the right edge.
- **The bottom NavigationBar highlight does NOT change when navigating to Ukraine.** It stays on Rovers (per `topLevelDestination()` mapping). Don't waste cycles asserting otherwise.
- **When the screen tree is huge** (Images screen with TopAppBar + actions + pager), prefer targeting the unique TextView (e.g. `arrow_back`) rather than the button wrapper — the button's bounds usually wrap an inner TextView and tapping the inner text works the same and is easier to author.

## What I'd want pre-built for the next AI

If you're going to do another audit pass, the following would have saved me ~10 minutes:

1. A `.maestro/` folder with one flow per screen (`rovers.yaml`, `photos.yaml`, …) that just navigates to the screen and `assertVisible`s a unique anchor. Lets the next AI run "go to screen X" in one call.
2. A small helper Maestro file that does `launchApp(clearState: true)` + bottom-bar tap, callable via `runFlow`.
3. Testing IDs (`Modifier.testTag(...)`) on the renew FAB, the layout-toggle button, and the date pickers. Right now they're addressable only by index or partial text.

