# What's New Since the KMP Migration — User-Facing Changes

*Baseline: the version right before the `kmp-migration` branch merged (May 20, 2026).
This is everything a user would actually notice in **v4.0.0**, ignoring internal
architecture / caching / build plumbing.*

---

## 🚀 Headline: Dead rovers are back

Before, Spirit, Opportunity, and Curiosity browsing was **broken** — NASA killed
the old API and those rovers returned nothing. Now all of them work again:

- **Curiosity** — full sol-based browsing and camera filtering restored, anchored
  to the latest sol on open.
- **Spirit & Opportunity** — revived through NASA's curated image library. You can
  browse their galleries again (hundreds of photos each), with a **page-jump**
  control and a **randomize** button to land somewhere new.
- **Perseverance & InSight** — unchanged, still working.

---

## 📱 Now runs on more than just Android

The app is no longer Android-only. The same experience now ships on:

- **iOS** (iPhone & iPad) — save photos to your library, share sheet, deep links,
  in-app review, ads/GDPR consent, splash screen, app icon — all native.
- **Desktop** (macOS / Windows / Linux) — branded splash + app icons.
- **Web** (experimental) — splash, favicon, icons.

If you only used Android before, this means the app now follows you across devices.

---

## ✨ Photo viewing feels completely different

- **Shared-element transitions** — tapping a photo now *morphs* the thumbnail into
  the fullscreen viewer (and back), instead of a hard cut. Returning animates back
  to whichever photo you swiped to.
- **No more black-screen loading flash** — the fullscreen image shows the cached
  thumbnail instantly and crossfades up to full resolution. Zooming in upgrades to
  the original full-res image.
- **Swipe-down to dismiss** the fullscreen viewer, integrated with the system
  predictive-back gesture.
- **Opens on the right photo** — fixed a bug where the viewer briefly flashed the
  first photo / double-animated when opening.
- **Light status-bar icons** + edge-to-edge while a photo is fullscreen.
- **Loading spinner** while a detail image loads.

---

## ❤️ Double-tap to like

- **Double-tap any photo to favorite it**, with a heart-pop animation and haptic
  feedback — on the grid, the popular list, and fullscreen.
- Favorite state now **syncs instantly across every screen** — like it in one place,
  it's liked everywhere immediately (no refresh lag).

---

## 🧭 Scroll position is remembered

- Returning from the fullscreen viewer **scrolls back to the photo you were looking
  at** (Photos grid, Favorites, and Popular) — and *keeps* your position if that
  photo is already on screen, instead of jumping.

---

## 🎨 Redesigned screens (Material 3)

- **Rovers screen** — rebuilt as clean left-aligned list rows: rover portrait,
  name, Active/Complete status chip, mission blurb, and a Photos / Sols / Last-date
  metric strip. Search is now a top-bar icon. Shows each rover's **current sol**.
- **About screen** — full redesign: hero header with the mascot, app version + a
  live "NASA online" status badge, grouped settings cards, an animated **swipeable
  light/dark theme picker**, facts toggle, clear-cache, and Rate / Feedback / NASA
  links. The Rate button now reliably reaches the Play Store listing.
- **Animated dark/light theme switching** — the color change now transitions
  smoothly instead of snapping.

---

## 🗓️ Unified date / page jumping

- The separate Sol-picker and Earth-date pickers were **merged into one
  DateJumpPicker**.
- For Spirit/Opportunity (which have no sols), the same control becomes a
  **page jump** ("Page X of Y"), with a floating chip showing where you are.

---

## 📅 Camera filtering

- **Tap a camera** (in Mission Info, or on a photo's camera label in the grid) to
  filter the feed to just that camera. Filter shows as a chip you can clear, with a
  proper empty state ("no <CAM> photos on Sol N").

---

## 🖥️ Tablet / large-screen support

- **Adaptive grids** on Rovers, Photos, Favorites, and Popular — tablets and
  desktop get more columns and centered, width-capped content.

---

## 🇺🇦 Ukraine banner

- Slimmer, dismissible banner moved to the top under the status bar.

---

## 🧹 Smaller polish

- Human-readable dates everywhere (e.g. "May 26, 2026" instead of raw strings).
- Thousands separators on big photo/sol counts.
- Numerous icon-rendering fixes (Material Symbols glyphs were previously clipped /
  half-drawn).
- Photo captions trimmed; long NASA image IDs ellipsized to one line.
- "Popular" list shows a favorite count; empty Favorites hides the layout toggle.
- Mission-info section hides itself if the data fails to load.
- Splash screens on every platform.
- Updated app icons.

---

## 🔧 Behind the scenes (mentioned for completeness, not user-visible)

Pagination overhaul (popular feed, sol feed, bidirectional page-keyed feed for
Spirit/Opportunity), a debug-only Admin Photos screen for stale-URL cleanup,
KMP Room paging, in-app review plumbing, ad/GDPR restoration, CI, and dependency
cleanup.
