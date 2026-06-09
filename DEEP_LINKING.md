# Deep Linking Support

Mars Rover Photos currently supports direct navigation to rover feeds and photo detail screens.

## Supported public links

### Custom scheme: `marsrover://`

Open a rover:

```text
marsrover://rover/{roverId}
```

Open a photo:

```text
marsrover://photo/{photoId}
```

Examples:

- `marsrover://rover/5` — open Curiosity photos
- `marsrover://rover/3` — open Perseverance photos
- `marsrover://photo/12345` — open photo `12345`

### HTTPS app links

Android declares HTTPS intent filters for:

```text
https://marsroverphotos.app/rover/{roverId}
https://marsroverphotos.app/photo/{photoId}
```

The iOS shared parser also understands `marsroverphotos.app` URLs, but the checked-in iOS shell does
not yet declare associated domains, so universal links are not currently wired on iOS.

## Rover IDs

| Rover | ID |
| --- | --- |
| Perseverance | 3 |
| InSight | 4 |
| Curiosity | 5 |
| Opportunity | 6 |
| Spirit | 7 |

## Platform behavior

- Android — custom scheme and HTTPS intent filters are declared in [`androidApp/src/main/AndroidManifest.xml`](/Users/sirelon/Projects/MarsRoverPhotos/androidApp/src/main/AndroidManifest.xml).
- iOS — custom scheme `marsrover://` is registered in [`iosApp/iosApp/Info.plist`](/Users/sirelon/Projects/MarsRoverPhotos/iosApp/iosApp/Info.plist), and `MarsRoverApp.swift` forwards incoming URLs to Kotlin.
- Desktop — no checked-in protocol-handler registration yet.
- Web — no published deep-link contract yet; see [`WASM_WEB_SUPPORT.md`](/Users/sirelon/Projects/MarsRoverPhotos/WASM_WEB_SUPPORT.md).

## Test locally

### Android

```bash
adb shell am start -W -a android.intent.action.VIEW \
  -d "marsrover://rover/5" \
  com.sirelon.marsroverphotos

adb shell am start -W -a android.intent.action.VIEW \
  -d "marsrover://photo/12345" \
  com.sirelon.marsroverphotos
```

### iOS simulator

```bash
xcrun simctl openurl booted "marsrover://rover/5"
xcrun simctl openurl booted "marsrover://photo/12345"
```

## Implementation notes

- The shared deep-link model lives in [`shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/navigation/DeepLink.kt`](/Users/sirelon/Projects/MarsRoverPhotos/shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/navigation/DeepLink.kt).
- iOS URL parsing and dispatch live in [`shared/src/iosMain/kotlin/Main.ios.kt`](/Users/sirelon/Projects/MarsRoverPhotos/shared/src/iosMain/kotlin/Main.ios.kt).
- The home-screen widget can navigate internally to a specific image via `DeepLink.Image`, but that is not a documented public URI format yet.
