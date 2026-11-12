# Mars Rover Photos

Compose Multiplatform app for browsing Mars rover imagery on Android, iOS, and desktop.
The shared Kotlin module provides the app's data, navigation, view models, and Compose UI; platform
modules host it for each target.

Photo feeds use NASA Mars imagery sources. Curiosity, Perseverance, InSight, and Ingenuity use the
Mars raw-image service; Spirit and Opportunity use the curated NASA Images library. Viking 1 and
Viking 2 use bundled catalogs generated from the static PDS archive.

## Development

Read [AGENTS.md](AGENTS.md) for the module layout, the build and verify commands, and the index of
topic docs under [docs/](docs/): architecture, design system, photo feed, monetization, releases.

The experimental `webApp/` module is not wired to the shared application yet; see
[WASM_WEB_SUPPORT.md](docs/WASM_WEB_SUPPORT.md).

## Deep links

The app supports `marsrover://` rover and photo links. See [DEEP_LINKING.md](docs/DEEP_LINKING.md) for the
supported URLs and platform behavior.
