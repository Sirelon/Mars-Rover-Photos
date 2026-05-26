# Repository Guidelines

## Project Structure & Module Organization
This is a Kotlin Multiplatform project. The module layout:
- `shared/` — all shared code: domain, data, repositories, view models, common Compose UI, navigation, DI
- `androidApp/` — Android shell: `MainActivity`, `MarsRoverApplication`, widget, GDPR helper, app icons
- `desktopApp/` — Desktop shell
- `iosApp/` — Swift/iOS shell, Xcode project, Firebase config
- `webApp/` — Web/WASM (currently disabled; see `WASM_WEB_SUPPORT.md`)

Feature screens and view models live in `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation`. Android-specific implementations go in `shared/src/androidMain` or `androidApp/`. Static-analysis configuration is stored under `config/detekt/`.

The KMP migration is complete. The legacy `app/` module has been deleted.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build the debuggable APK with the repository Compose compiler flags.
- `./gradlew testDebugUnitTest` — run JVM unit tests; execute before every commit.
- `./gradlew connectedDebugAndroidTest` — launch instrumentation tests on an attached emulator or device.
- `./gradlew detekt` — lint and autocorrect according to `config/detekt/detekt.yml`.
Run commands from the repository root so the Gradle wrapper can supply the pinned toolchain.

## Coding Style & Naming Conventions
Kotlin files use four-space indentation, `val` first, and explicit visibility for public APIs. Compose functions and classes stay in PascalCase, constants in `UPPER_SNAKE_CASE`, and extension files match their receiver (`ImageRequestExt.kt`). Keep packages cohesive; add a `feature/*` subpackage for new screens. Run `./gradlew detekt` before review instead of hand-tuning formatting.

## Testing Guidelines
Place unit specs in `shared/src/commonTest` (or `androidTest` for Android-instrumented tests), mirroring the source package and ending class names with `*Test`. Use JUnit4 and Mockito-Kotlin for Android tests. Compose UI or Room integration checks belong in `androidApp/src/androidTest` and should describe the scenario in the test name. Cover paging boundaries, offline caching, and error flows whenever you touch those areas.

## Commit & Pull Request Guidelines
Follow the existing history with concise imperative subject lines under ~70 characters (e.g., `Fix release build`). Keep functional changes grouped per commit. Pull requests need a short summary, call out UI or API changes, link issues, and attach emulator screenshots or recordings when visuals move. Confirm `assembleDebug`, `testDebugUnitTest`, and `detekt` succeed before requesting review.

## Security & Configuration Tips
Signing data comes from `keystore.properties`; keep it local and out of version control. Avoid committing `local.properties`, API keys, or regenerated `*.jks` files. Prefer runtime configuration or encrypted storage for new secrets and alert maintainers immediately if a credential leaks.
