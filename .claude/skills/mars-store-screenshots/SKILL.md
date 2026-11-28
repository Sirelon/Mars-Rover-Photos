---
name: mars-store-screenshots
description: >-
  Make Mars Rover Photos store screenshots end to end: render the real app headless with live NASA
  data (no device, no simulator), frame and caption the screens, check every caption against its
  screen, upload to App Store Connect and Google Play. Use for the full set, one screen ("redo the
  screenshot of the mission screen"), a new caption, a new device slot, or "upload the screenshots".
---

# Mars Rover Photos store screenshots

Four stages, each one command from the repo root. Any stage runs alone, for one scene or device.

| Stage | Command | Writes |
| --- | --- | --- |
| Render | `STORE_RENDER=1 ./gradlew :shared:desktopTest --tests '*StoreScenesTest*' --rerun -q` | `store/captures/<device>/<scene>_<theme>.png` |
| Compose | `node store/tools/generate-store-screenshots.mjs --sheet` | `store/assets/<store>/<target>/NN-<id>.jpg`, `store/previews/<target>.jpg` |
| Review | your eyes on the contact sheets | fixes in scenes or captions |
| Upload | `LANG=en_US.UTF-8 bundle exec ruby store/tools/upload-screenshots.rb app-store\|play [--apply] [--create-version=X.Y.Z]` | the store consoles |

Narrow the render with comma lists: `STORE_RENDER_SCENES`, `STORE_RENDER_DEVICES` (`iphone`, `ipad`,
`android-phone`, `android-tablet`), `STORE_RENDER_THEMES` (`light`, `dark`). Narrow the compositor
with `--target` and `--scene`. A full render (12 scenes x 4 devices x 2 themes) takes about 15
minutes: run it in the background. `upload-screenshots.rb status` reads the consoles without
changing anything.

Files:

| Path | Role |
| --- | --- |
| `shared/src/desktopTest/.../storerender/StoreRender.kt` | devices, the renderer, the Koin overrides, system-bar strips |
| `storerender/StoreScenes.kt` | the scene registry (`storeScenes`), the hero photo, the favorites, and the test |
| `store/tools/scenes.json` | **the manifest**: which scene, theme and caption fills each store slot, with a `why` |
| `store/copy/copy.json` | captions by key (English only) |
| `store/tools/generate-store-screenshots.mjs` | targets (sizes, frames) and the HTML layout |
| `store/tools/upload-screenshots.rb` | `status`, App Store Connect and Google Play upload, dry run by default |
| `store/tools/fonts/` | caption fonts (OFL) |

Store rules, sizes, slot types and NASA image terms: [reference.md](reference.md). Read it before you
change a size, add a slot, write a caption or upload.

## How the render works

A scene is a navigation stack, not a mock: the test runs the app's own `AppNavigation` with the
desktop Koin graph and live data from NASA and Firestore, inside a headless `ImageComposeScene` at
the store pixel size. So a screenshot shows what the app shows today, and a messy screenshot is
usually a real app defect.

- **Isolation.** The database is in memory and the preferences sit in a scratch node, so a render
  never touches the desktop app's data. Firestore is read only (`ReadOnlyFirebasePhotos`): opening
  the viewer or favoriting in a scene never bumps the shared counters.
- **Repeatable feeds.** The app opens most rover feeds on a random sol. `StoreScene.feedSols` pins
  it (through `PinnedSolPhotosRepository`). Curiosity always opens on its latest sol.
- **Favorites and the viewer** come from the live Popular feed. Each run writes the candidates to
  `store/cache/popular.txt`; pick ids there by eye and set `HERO_PHOTO` / `FAVORITE_PHOTOS`.
- **System bars.** The JVM has no window insets. The harness leaves a status-bar and home-bar strip
  in the colour the app would draw there (`StripStyle`: `App`, `Fullscreen` black, `Sheet` scrim
  over background); the compositor draws the clock and full signal, Wi-Fi and battery into it.
- **Settling.** A frame counts as done when it has not changed for 2 s after `minSettleMs`; network
  photos need the wait. A screen that animates forever is captured after 40 s more, with a log line.
- **Threads.** All Compose work and the view models run on the Swing thread, so no cross-thread
  layout errors. The Gradle test task runs on Java 21 and puts `firebase-java-sdk` last on the
  classpath (`shared/build.gradle.kts` says why).

## Steps

### 1. Scope

Name the scenes and devices, and whether captions change. A caption is truthful only for the screen
under it: a new screen needs a caption, a changed screen needs its caption re-read.
Done when the request reads "scenes x devices, captions changed or not".

### 2. Render

Run the render for the scope. A new scene is a `StoreScene` with a back stack (bottom first), plus
`feedSols`, `strips` or `prepare` when it needs them. Dialog destinations sit on their parent screen
in the stack, as in the app.
Done when every PNG of the scope exists and is newer than your last change.

### 3. Look at every capture

Downscale (`magick in.png -resize 600x out.png`) and open each capture of the scope, light and dark.
Each one shows loaded photos (no grey box or spinner), no "Offline" snackbar, nothing cut or
overlapping. Data changes daily: Today on Mars, the latest sol and the mission-card photos move.
Done when every capture passes, or its defect is written down with the cause. A defect in the app's
own UI goes to the owner with the image; ask before changing app UI.

### 4. Captions

A new or reworded caption is a sentence the owner approves before it ships: post
`key -> headline / sub` with your recommended wording, and wait. A caption sells what the user gets,
is literally true of its screen, carries no "best", "#1", "top", "new" or call to action, and never
implies NASA made or endorses the app (reference.md). Reuse an existing caption when it is true.
Done when every `copy` key in `scenes.json` exists in `copy.json` and the owner approved new ones.

### 5. Compose

Edit `scenes.json` (order = store order; the first three show in App Store search; `statusBar:
"light"` for a scene whose top strip is dark in the light theme), then run the compositor. It stops
a target with a missing capture and flags two slots with the same capture.
Done when it exits 0 with no `Problems` line.

### 6. Review

Open every `store/previews/<target>.jpg` and judge each image against its caption and `why`: true of
what the screen shows, headline within two lines, nothing over the device, status bar legible.
Done when every image of every target passes.

### 7. Upload

Dry run first; it prints every set it would replace. App Store writes only into an editable version
(not in review, not on sale). When the latest is locked, `--create-version=X.Y.Z` creates the next
draft first: use the number the next `bumpVersion` gives (a minor bump). A draft ships nothing, and
the next release's `ios release_notes` renames it if the number differs (deliver calls Spaceship's
`ensure_version!`). The screenshots then go live with that release. Play `--apply` commits the edit,
which publishes the listing change at once: the owner's request to upload is the approval, once he
has approved the captions.
Done when the dry run matches the plan and `--apply` reports every set replaced, or the blocker is
reported with what the owner must do.
