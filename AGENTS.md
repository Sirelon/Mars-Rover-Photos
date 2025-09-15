# Repository Guidelines

## Project Structure & Module Organization
The Android application lives in `app/`. Feature screens and view models sit in `app/src/main/java/com/sirelon/marsroverphotos/feature` with reusable UI in `ui`, data access in `network` and `storage`, and shared models/helpers in `models`, `utils`, and `extensions`. Resources for Compose themes, drawables, and icons reside in `app/src/main/res`. Static-analysis configuration is stored under `config/detekt/`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build the debuggable APK with the repository Compose compiler flags.
- `./gradlew testDebugUnitTest` — run JVM unit tests; execute before every commit.
- `./gradlew connectedDebugAndroidTest` — launch instrumentation tests on an attached emulator or device.
- `./gradlew detekt` — lint and autocorrect according to `config/detekt/detekt.yml`.
Run commands from the repository root so the Gradle wrapper can supply the pinned toolchain.

## Coding Style & Naming Conventions
Kotlin files use four-space indentation, `val` first, and explicit visibility for public APIs. Compose functions and classes stay in PascalCase, constants in `UPPER_SNAKE_CASE`, and extension files match their receiver (`ImageRequestExt.kt`). Keep packages cohesive; add a `feature/*` subpackage for new screens. Run `./gradlew detekt` before review instead of hand-tuning formatting.

## Testing Guidelines
Place unit specs in `app/src/test/java`, mirroring the source package and ending class names with `*Test`. Use JUnit4 and Mockito-Kotlin already declared in `app/build.gradle`. Compose UI or Room integration checks belong in `app/src/androidTest/java` and should describe the scenario in the test name. Cover paging boundaries, offline caching, and error flows whenever you touch those areas.

## Commit & Pull Request Guidelines
Follow the existing history with concise imperative subject lines under ~70 characters (e.g., `Fix release build`). Keep functional changes grouped per commit. Pull requests need a short summary, call out UI or API changes, link issues, and attach emulator screenshots or recordings when visuals move. Confirm `assembleDebug`, `testDebugUnitTest`, and `detekt` succeed before requesting review.

## Security & Configuration Tips
Signing data comes from `keystore.properties`; keep it local and out of version control. Avoid committing `local.properties`, API keys, or regenerated `*.jks` files. Prefer runtime configuration or encrypted storage for new secrets and alert maintainers immediately if a credential leaks.
