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

Open the release notes:

```text
marsrover://whatsnew
marsrover://whatsnew/{version}
```

Examples:

- `marsrover://rover/5` — open Curiosity photos
- `marsrover://rover/3` — open Perseverance photos
- `marsrover://photo/12345` — open photo `12345`
- `marsrover://whatsnew` — open the full version history
- `marsrover://whatsnew/4.2.0` — open the story view for 4.2.0, falling back to the version history
  when the installed build ships no notes for that version

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
| Viking 1 | 8 |
| Viking 2 | 9 |

Photo links (`photo/{id}`) resolve numeric NASA photo ids only. The Viking landers and the two
MER rovers use alphanumeric archive ids (`12A001-BB1`, `PIA00565`), so their individual photos are
not addressable this way — link to the mission instead.

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

adb shell am start -W -a android.intent.action.VIEW \
  -d "marsrover://whatsnew/4.2.0" \
  com.sirelon.marsroverphotos
```

### iOS simulator

```bash
xcrun simctl openurl booted "marsrover://rover/5"
xcrun simctl openurl booted "marsrover://photo/12345"
xcrun simctl openurl booted "marsrover://whatsnew"
```

## Implementation notes

- The shared deep-link model lives in [`shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/navigation/DeepLink.kt`](/Users/sirelon/Projects/MarsRoverPhotos/shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/navigation/DeepLink.kt).
- All URI parsing goes through `parseDeepLink` in [`shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/navigation/DeepLinkParser.kt`](/Users/sirelon/Projects/MarsRoverPhotos/shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/navigation/DeepLinkParser.kt), shared by Android intents, the iOS `onOpenURL` bridge and notification taps. A new link form is added there once.
- iOS dispatch into the running Compose content lives in [`shared/src/iosMain/kotlin/Main.ios.kt`](/Users/sirelon/Projects/MarsRoverPhotos/shared/src/iosMain/kotlin/Main.ios.kt).
- Notification taps carry their target as a `link` value in the message's data payload — see [`docs/PUSH_NOTIFICATIONS.md`](/Users/sirelon/Projects/MarsRoverPhotos/docs/PUSH_NOTIFICATIONS.md).
- The home-screen widget can navigate internally to a specific image via `DeepLink.Image`, but that is not a documented public URI format yet.
