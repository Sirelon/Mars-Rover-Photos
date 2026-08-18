# Release notes — review document

Proposed user-facing copy for the What's New screen, newest release first.
Dates come from [VERSIONS.md](VERSIONS.md). Findings were derived from each release's commit
range; the raw per-release JSON is scratch and is not kept in the repo.

**What is included.** Every raw finding marked `user_impact: "major"`, with the `minor` items that
hang off it through `related_to` merged in. Plus, in seven releases, one **umbrella** entry
(marked 🧵) that tells the combined story of three or more related `minor` items. Maximum one
umbrella per release. No standalone `minor` item appears on its own. The full 1.0 → 5.0.0 span is
in scope; nothing was cut by date.

`ChangeType` values marked **(new)** do not exist yet — see
[EDITORIAL_NOTES.md](EDITORIAL_NOTES.md).
Entries marked ⚠️ rest on a medium-confidence finding and need a human check.

24 releases with notes · 35 changes · 7 umbrellas · 11 new `ChangeType` constants.

> **The shipped list has 13 cards, not 24.** Fourteen thin releases were merged into five denser
> cards after this document was written; all 35 changes were kept. The sections below are still
> organised by the version each change *shipped in*, which is the useful view for verifying
> evidence. For which card each now appears on, see **EDITORIAL_NOTES.md §9**; for the order they
> appear in, **§10**.

Two entries were authored by hand rather than produced by the pipeline: **2.4.0** (Stand with
Ukraine, §1) and **1.0.0** (the initial release, §8).

---

## 5.0.0 — 2026-06-29

**Mission Info** · `MISSION_INFO`
- summary: A richer look at every rover's mission
- detail: Mission Info now opens with a full-width hero image, a scrollable mission timeline, and cameras you can expand one at a time. A Browse Photos button takes you straight into that rover's feed, and the fun fact changes each time you visit. On a wide window the whole screen splits into two panels.
- 📷 Mission Info for Perseverance on a tablet or desktop window, two-panel layout, hero image + timeline visible.

🧵 **Seamless Photos** · `PHOTO_VIEWER` **(new)**
- summary: Opening a photo feels like one continuous move
- detail: Thumbnails grow into the fullscreen photo from every grid now, Favorite and Popular included, while the navigation bar folds quietly out of the way. Swiping down to close follows your finger and reveals the list underneath. Photos arrive faster and without a flash, and closing one puts you back exactly where you were.
- 📷 Mid-transition frame: a Favorite grid thumbnail expanding into the fullscreen viewer, nav bar half-collapsed.
- 🧵 Umbrella over 8 minors — see EDITORIAL_NOTES §4.

---

## 3.0.0 — 2026-06-12

**All Your Devices** · `MULTIPLATFORM`
- summary: Now on Android, iPhone, iPad and desktop
- detail: Mars Rover Photos runs natively on iPhone and iPad, and as a desktop app on Mac, Windows and Linux. It is the same app everywhere, so your rovers, photos and settings look and behave the same on whichever screen you pick up.
- 📷 Three-up composite: the rover list on a phone, an iPad, and a resized desktop window.

**Photos Restored** · `BUG_FIX` **(new)**
- summary: Curiosity, Spirit and Opportunity are browsable again
- detail: The NASA feed those three rovers relied on stopped answering, which left their photos completely unreachable. They now come straight from NASA's own image archives: Curiosity keeps day-by-day and camera browsing, while Spirit and Opportunity are browsed page by page. One picker handles jumping to a date or to a page, whichever the rover supports.
- 📷 A full Curiosity photo grid, and beside it the Spirit feed with the page-jump picker open.

🧵 **Filters** · `BROWSING` **(new)**
- summary: Narrow things down to what you want to see
- detail: A Filters sheet lets you pick cameras with chips, jump to a date or a page, and turn the camera name on each tile on or off. Tapping a camera on Mission Info opens the feed already filtered to it, and a search icon narrows the rover list itself.
- 📷 The Filters bottom sheet open over the Photos grid with two camera chips selected.
- 🧵 Umbrella over 3 minors. The competing theme in this release — the Rovers/About redesign, new icon and splash, theme crossfade — lost the one-per-release cap; see EDITORIAL_NOTES §4.

---

## 2.5.3 — 2026-05-03

**Mission Info** · `MISSION_INFO`
- summary: Explore each rover's full mission profile
- detail: An info button on any rover card opens a dedicated screen: the mission timeline, total photos and days active, the rover's cameras, and what it was sent to Mars to do. Fun facts about each mission are there too.
- 📷 The original Mission Info screen for Curiosity, scrolled to the timeline and statistics.

**Did You Know** · `FACTS` **(new)**
- summary: Learn something while you scroll
- detail: A fact card appears every so often as you browse a rover's photos, with a short piece of Mars or mission trivia. If you would rather just see photos, switch them off in About.
- 📷 A photo grid with a "Did You Know?" fact card inline between photo tiles.

**Home Widget** · `WIDGET` **(new)**
- summary: The latest Mars photo on your home screen
- detail: Add the Mars Photo widget, pick a rover, and its newest picture sits on your home screen and refreshes on its own. Tap the widget to open that photo in the app.
- 📷 Home screen with the widget placed, or the widget configuration screen with the rover picker open.

---

## 2.4.5 — 2024-01-16

**New Look** · `REDESIGN`
- summary: Refreshed colors, type and controls throughout
- detail: Colors, text styles, the bottom bar, cards and buttons were all rebuilt, giving the rover list, photo grid, Favorites, About and the photo viewer a cleaner and more consistent feel. On Android 12 and newer the app takes its palette from the colors your phone generates from your wallpaper, and the top bar on Favorites and Popular shrinks as you scroll.
- 📷 Rovers list and Favorites side by side, ideally under two different system wallpapers to show the palette following along.

---

## 2.4.0 — 2022-09-06

**Stand with Ukraine** · `UKRAINE` *(new type)*
- summary: A message from the developer
- detail: A blue and yellow banner sits at the top of the rover list. Tapping it opens a short note about the war in Ukraine and ways to help.
- 📷 The rover list with the blue/yellow banner in the top bar, or the info screen itself.
- ⚠️ Included by explicit user override. Editorially this is a lone `minor` with no umbrella available — see EDITORIAL_NOTES §1.

---

## 2.3.2 — 2022-09-03

**Newer Photos** · `BUG_FIX` **(new)**
- summary: The date and sol pickers reach today
- detail: Each rover's mission details are refreshed from NASA when the app starts. The sol slider and date picker now cover everything a rover has sent, instead of stopping at a date frozen years earlier.
- 📷 The sol slider for Curiosity pushed to its maximum, showing a recent sol rather than an old one.

---

## 2.3.1 — 2022-02-16

🧵 **Cleaner Cards** · `REDESIGN`
- summary: Less clutter on cards and while swiping
- detail: Counters that would have read zero are hidden, so a card only shows numbers that mean something. Swiping past the first or last photo no longer stretches the screen, and About leads with a single logo instead of a row of rover thumbnails.
- 📷 Popular grid where cards with no activity show no counter row at all.
- 🧵 Umbrella over 3 minors. This release's only major is an ad change, so without the umbrella the release would not appear at all.

---

## 2.3.0 — 2021-09-07

**Pick A Theme** · `THEMING` **(new)**
- summary: Light, dark, or whatever your phone is doing
- detail: About now offers White, Dark and System. Choose one and the whole app changes immediately and stays that way next time you open it. The screen was reorganised to make room, and now leads with the rovers themselves.
- 📷 The theme card on the About screen with the three options, ideally as a light/dark pair.

---

## 2.1.1 — 2021-07-25

🧵 **Save Photos** · `SAVE_PHOTO` **(new)**
- summary: Keep a photo, in a steadier viewer
- detail: A save button in the fullscreen viewer downloads the picture to your device, confirms when it is done, and offers a shortcut to view it. The viewer around it is steadier too: liking a photo no longer snaps you back to the first image, and double-tapping to zoom out re-centres the picture instead of leaving it shifted.
- 📷 Fullscreen viewer top bar with the save icon, plus the "saved" snackbar and its View action.
- 🧵 Umbrella over 4 minors, all in the fullscreen viewer. This release has no major, so the umbrella is the only reason saving to the gallery is announced anywhere in the app's history.

---

## 2.0.0 — 2021-05-12

**Zoom And Swipe** · `PHOTO_VIEWER` **(new)**
- summary: Pinch, double-tap and fling through photos
- detail: The fullscreen viewer was rebuilt: pinch to zoom, double-tap to snap in, drag around a zoomed photo, and fling to the next one. You can mark a photo as a favorite without leaving it, and a photo that comes up empty now retries instead of staying blank.
- 📷 A zoomed-in crop of a Mars surface photo in the fullscreen viewer, heart icon visible.

---

## 1.7.2 — 2021-04-04

**One Screen** · `NAVIGATION` **(new)**
- summary: Rovers, Favorites and Popular live in one place
- detail: Tapping a rover or switching tabs now opens inside the same screen, with the bottom bar always in reach. A rover's photos gained a sol and Earth-date picker with a slider, a shuffle button, and a tap-to-retry message when a day has nothing.
- 📷 Rover photos screen with the bottom bar still visible and the sol picker dialog open.

🧵 **Blurred Backdrop** · `PHOTO_VIEWER` **(new)**
- summary: Photos sit on a soft blur of themselves
- detail: The fullscreen viewer fills the space around a photo with a blurred, darkened copy of it instead of flat color, and adds a back arrow at the top so you are never stuck. The heart updates straight away now when you favorite the photo you are looking at.
- 📷 A portrait-shaped photo in the fullscreen viewer, with the blurred backdrop filling the sides.
- 🧵 Umbrella over 3 minors.

---

## 1.7.1 — 2021-02-26

**Bottom Bar** · `NAVIGATION` **(new)**
- summary: Rovers, Favorite, Popular and About, one tap away
- detail: A bottom navigation bar replaces the single scrolling list and its hidden menu. Favorites and Popular are tabs of their own now, Popular shows its grid right there instead of on a separate screen, and a Favorites row joins the rover list itself.
- 📷 The main screen with the four-tab bottom bar, Popular selected and its grid filled.

**Day Filter** · `BUG_FIX` **(new)**
- summary: Changing the day reliably reloads photos
- detail: Switching to another sol now always fetches that day's pictures. Perseverance in particular used to ask for a computed date range that could return the wrong photos or none at all.
- 📷 Perseverance photos for a specific sol, with the sol shown in the toolbar.

---

## 1.7.0 — 2021-02-22

**Perseverance** · `NEW_ROVER` **(new)**
- summary: NASA's newest rover joins the list
- detail: Perseverance landed in February 2021 and now sits in the rover list alongside Curiosity, Opportunity, Spirit and InSight. Pick it to browse its photos like any other mission.
- 📷 The rover list with the Perseverance card at the top, or a Perseverance photo grid.

⚠️ **Popular Fixed** · `POPULAR`
- summary: Popular entries load with all their details
- detail: Some photos on the Popular screen could come through with no image, name or date. Each entry is now read out field by field, so it arrives complete.
- 📷 A fully populated Popular grid with every tile showing an image and its counters.
- ⚠️ Medium confidence: the raw record establishes that the old mapping was failure-prone, not that users definitely saw blank entries. Softened accordingly; drop it if you cannot confirm the symptom.

---

## 1.6.1 — 2021-02-21

**Steady Lists** · `BUG_FIX` **(new)**
- summary: Every row shows its own photo
- detail: Rows in the photo grid, the rover list, Favorites and Popular were reading their contents from the wrong place, which could show another row's picture or close the app. Each row now draws itself correctly.
- 📷 A correctly rendered photo grid where every tile matches its caption.

---

## 1.6.0 — 2020-09-06

**Popular Opens** · `POPULAR`
- summary: Tapping a popular photo opens it fullscreen
- detail: Photos on the Popular screen used to do nothing when tapped. They now open in the same fullscreen viewer as everywhere else, and you can swipe on through the rest.
- 📷 Popular grid with a tap in progress, next to the fullscreen result.
- Note: this release also removed every way to reach Favorites. Confirmed real and deliberate, and deliberately not written up — see EDITORIAL_NOTES §6.

---

## 1.5.2 — 2020-08-30

**Favorites** · `FAVORITES`
- summary: Save your best shots to revisit later
- detail: Tap the heart while viewing a photo to keep it. A tile on the home screen opens everything you have saved, using your most recent favorite as its cover, and you can remove a photo straight from that list.
- 📷 The Favorites screen with several saved photos, plus the home-screen tile showing the newest one.

---

## 1.4.2 — 2019-04-03

**Live Stats** · `MISSION_INFO`
- summary: Rover cards show real mission numbers
- detail: Landing and launch dates, the latest day and the total photo count were stuck at the values the app shipped with, or showed a bare 0. They now update from NASA.
- 📷 A rover card with plausible current landing date, max sol and total photos.

---

## 1.4.0 — 2019-04-03

🧵 **Fresh Look** · `REDESIGN`
- summary: New colors, new type, smoother moves
- detail: The app moved to a blue and orange palette with a single typeface across every screen, dialogs and date pickers included, and margins were tightened so more of the screen belongs to the photos. Tapping a picture animates the thumbnail into the fullscreen view instead of cutting to it, and a day with no photos shows a proper empty screen rather than a blank list.
- 📷 The rover list and photo grid in the new blue/orange palette with the custom typeface; optionally a mid-transition frame of a thumbnail expanding.
- 🧵 Umbrella over 3 minors. This release has no major at all — without the umbrella the app's visual history would start at 2.4.5 in 2024.

---

## 1.3.1 — 2019-02-25

⚠️ **InSight** · `NEW_ROVER` **(new)**
- summary: NASA's InSight lander joins the list
- detail: InSight sits alongside Curiosity, Opportunity and Spirit and opens a set of its surface images. Its mission numbers on the card are still placeholders for now.
- 📷 The rover list with the InSight card, and an InSight photo grid.
- ⚠️ Medium confidence, and the feature shipped unfinished: the photo set was a fixed hardcoded batch and the card's stats were copied from Spirit. The last sentence is the honest hedge — if you would rather not admit the placeholder stats, shorten the entry instead of overstating it.

---

## 1.3.0 — 2018-09-02

**Popular Photos** · `POPULAR`
- summary: Community-curated highlights from each mission
- detail: A card at the top of the rover list opens Most Popular Mars Photos: the images people view, save, zoom into and share the most, each showing its counts. More load as you keep scrolling.
- 📷 The Most Popular grid with its per-tile see/save/scale/share counters.

---

## 1.2.4 — 2017-11-18

**Sharing Works** · `BUG_FIX` **(new)**
- summary: Photos actually reach the app you pick
- detail: Choosing an app from the share sheet used to do nothing at all. The photo is now handed over properly, so the app you picked opens with it ready to send.
- 📷 The share target open with the Mars photo already attached.

🧵 **Crisper Photos** · `REDESIGN`
- summary: Sharper thumbnails and a new app icon
- detail: Pictures in the grid and on the rover picker were being squeezed into a small box before being drawn; they now render at full resolution. The rover portraits ship with the app instead of being pulled from other websites, so they always appear, and the launcher icon was redrawn to sit properly on any home screen.
- 📷 The rover picker with all three rover portraits present and sharp, plus the launcher icon on a home screen.
- 🧵 Umbrella over 3 minors.

---

## 1.2.0 — 2016-11-17

**Swipe Through** · `PHOTO_VIEWER` **(new)**
- summary: Open one photo and keep going
- detail: Tapping a picture opens a swipeable, pinch-zoomable gallery built from the grid you were browsing, starting on the photo you tapped. If you had filtered to one camera, swiping stays within that camera.
- 📷 A fullscreen photo mid-swipe with the next photo edging in from the side.

---

## 1.1.1 — 2016-11-10

**Crash Fix** · `BUG_FIX` **(new)**
- summary: No more crashes on older Android versions
- detail: The retry, save and about icons closed the app on devices older than Android 5.0. Those screens now open normally.
- 📷 The About screen and the photo-detail menu, both open on an old Android device.

---

## Releases with no user-facing notes

21 of the 45 versions in VERSIONS.md, broken down by reason in
[EDITORIAL_NOTES.md](EDITORIAL_NOTES.md) §3: 1.0.1, 1.1, 1.2.1, 1.2.2, 1.2.3, 1.4.1, 1.4.3, 1.4.4,
1.4.5, 1.5.0, 1.5.1, 2.1.0, 2.2.0, 2.4.1, 2.4.2, 2.4.3, 2.4.4, 2.5.0, 2.5.1, 2.5.2, 4.0.0.

`2.4.0` is no longer dropped — it ships the Stand with Ukraine entry by user override (§1).
`1.0` is no longer dropped either — it ships as **1.0.0**, authored by hand (§8).

## 1.0.0 — 2016-11-04

**Hello, Mars** · `INITIAL_RELEASE` *(new type)*
- summary: The first release
- detail: Browse real photos taken on the surface of Mars by NASA's rovers. Pick Curiosity, Opportunity or Spirit and scroll through what it saw, day by day.
- 📷 The rover list showing Curiosity, Opportunity and Spirit.

**Full-Screen Photos** · `PHOTO_VIEWER`
- summary: Tap any photo, then pinch to zoom
- detail: Open a photo to fill the screen and pinch to zoom right in on the rocks, dust and rover tracks.
- 📷 A Mars surface photo zoomed in, full screen.

**Save and Share** · `SAVE_PHOTO`
- summary: Keep a photo, or send it to someone
- detail: Save any Mars photo to your device, or share it straight from the viewer.
- 📷 The viewer with the save and share actions visible.

- ⚠️ **Authored by hand, not by the pipeline.** See EDITORIAL_NOTES §8 — including why the date is
  2016-11-04 and not the 2017-03-05 that VERSIONS.md records for version `1.0`.

---

**4.0.0 is a recorded decision, not an oversight.** It contains exactly one user-facing item — a
`minor` widget fix — so it cannot support an umbrella, and the list therefore steps 3.0.0 → 5.0.0.
See EDITORIAL_NOTES §3.
