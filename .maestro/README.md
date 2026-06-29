# Store screenshots

Automated capture of App Store / Google Play screenshots with [Maestro](https://maestro.dev).
One flow drives the app through 7 screens; ads are suppressed and both light and dark themes are captured.

**The flow does NOT clear app data.** Open the app first and **favorite the photos you want shown on the
Favorites screen** — the flow just navigates and screenshots whatever is there.

## Prerequisites

- Maestro installed: `curl -Ls "https://get.maestro.mobile.dev" | bash`
- A device/simulator **booted** with the app **installed** and **online** (photos load over the network).
  - Android: `./gradlew :androidApp:installDebug`
  - iOS: build & run the `iosApp` scheme once onto a booted simulator.
- At least one photo **favorited by hand** (so the Favorites screenshot isn't empty).

## Run

```bash
./.maestro/capture.sh android   # connected Android device/emulator
./.maestro/capture.sh ios       # booted iPhone simulator
./.maestro/capture.sh ipad      # booted iPad simulator (landscape) → screenshots/ipad/
```

`ipad` captures in landscape. The simulator saves the native portrait framebuffer (2064×2752) **plus
an EXIF Orientation tag**, which makes Finder/Preview/App Store render frames sideways. The wrapper
fixes this with Pillow: it rotates 90° CW and **strips all metadata**, producing true upright landscape
images (2752×2064) that every viewer agrees on. (Needs `python3 -m pip install pillow`; without it,
it falls back to `sips` and warns that the orientation tag — and the sideways look — will remain.)
Boot the iPad sim first and install the same build (`xcrun simctl install <udid> <iosApp.app>`).

The wrapper **pins the target device** (so Maestro can't pick the wrong one when several are connected),
runs the flow twice (light + dark), and restores the device theme afterwards. To target a specific device:

```bash
ANDROID_SERIAL=<serial> ./.maestro/capture.sh android
IOS_TARGET=<sim-udid>   ./.maestro/capture.sh ios
```

Output lands in `.maestro/screenshots/<platform>/` as `NN-<screen>-light.png` / `-dark.png`
(14 frames per platform). Android and iOS sets stay in separate folders.

Single pass (pin the device; `THEME` and `PLATFORM` are required on the CLI):

```bash
maestro --device <id> test -e THEME=light -e PLATFORM=android .maestro/store-screenshots.yaml
```

## How ads are hidden

The flow launches with `arguments: { hideAds: true }`. The app reads this at launch
(`MainActivity` on Android, `MarsRoverApp` on iOS) and sets `BuildInfo.hideAds`, which the single
`AdSlot` in `AppNavigation` honors — so no banner appears in any frame. The flag is `false` in normal use.

## Recommended store ordering

The capture order isn't the listing order. For the storefront, arrange them as a funnel:

1. `04-image` — the hero (full-screen Mars photo)
2. `02-photos` — the photo feed
3. `01-rovers` — rover missions
4. `05-popular` — most-viewed photos
5. `06-favorites` — saved collection
6. `03-filters` — camera / date / sol filters
7. `07-about` — settings & app info

Dark-theme frames are optional extras — drop in one or two if the listing benefits.

## Troubleshooting

- **A wait times out** (slow network): bump the `timeout` values in `store-screenshots.yaml`.
- **`Curiosity` not found**: the rover list label changed — update the `tapOn: "Curiosity"` step.
- **Favorites step times out**: you have no favorites — open the app and favorite at least one photo first.
- **Ran on the wrong device**: the wrapper pins `--device`; for a direct `maestro` run, always pass `--device <id>`.
