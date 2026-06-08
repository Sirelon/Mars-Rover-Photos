# WASM / Web Support

## Current state

The repository includes a standalone `:webApp` module, and `settings.gradle.kts` still includes it.
That module defines a browser `wasmJs` target in [`webApp/build.gradle.kts`](/Users/sirelon/Projects/MarsRoverPhotos/webApp/build.gradle.kts).

The shared `:shared` module does **not** currently expose a WASM target. Its `wasmJs` block and
`wasmJsMain` dependencies are commented out in
[`shared/build.gradle.kts`](/Users/sirelon/Projects/MarsRoverPhotos/shared/build.gradle.kts).

## Why shared web support is disabled

The checked-in comments in `shared/build.gradle.kts` describe the current blockers:

- Room KMP does not support the app's WASM path yet.
- Database-dependent code would need `expect`/`actual` or an alternative storage layer.
- An IndexedDB-backed web persistence strategy is the likely replacement if full web support resumes.

## Practical implication

Today, the repo is **not** set up to run the full shared Mars Rover Photos app on the web. The
`webApp/` module is an experimental shell, while the production architecture remains Android + iOS +
desktop through `:shared`.

## TODO before re-enabling shared WASM

- Decide whether the web app should be read-only or support local persistence.
- Replace Room-dependent shared storage with a web-capable abstraction.
- Re-enable the shared `wasmJs` target and its dependencies in `shared/build.gradle.kts`.
- Wire the web entry point to shared navigation and screen/view-model flows once storage is resolved.
