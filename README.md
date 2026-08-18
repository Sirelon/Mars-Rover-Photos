# Mars Rover Photos

Compose Multiplatform app for browsing Mars rover imagery on Android, iOS, and desktop.
The shared Kotlin module provides the app's data, navigation, view models, and Compose UI; platform
modules host it for each target.

Photo feeds use NASA Mars imagery sources. Curiosity, Perseverance, and InSight use the Mars raw-image
service; Spirit and Opportunity use the curated NASA Images library.

## Development

Read [AGENTS.md](AGENTS.md) for the module layout, build and test commands, versioning workflow, and
links to the architecture and design-system guidance.

The experimental `webApp/` module is not wired to the shared application yet; see
[WASM_WEB_SUPPORT.md](WASM_WEB_SUPPORT.md).

## Deep links

The app supports `marsrover://` rover and photo links. See [DEEP_LINKING.md](DEEP_LINKING.md) for the
supported URLs and platform behavior.
