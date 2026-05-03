# Repository Guidelines

## Project Structure & Module Organization
The project is migrating from the legacy Android-only `app/` module to Kotlin Multiplatform. The active KMP structure is `shared/` for common code, `androidApp/` for the Android shell, `desktopApp/` for Desktop, and `iosApp/` for iOS setup. The legacy `app/` module remains the reference for screen behavior while migration is in progress. Feature screens and view models being migrated should land under `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation`, with Android-specific implementations in `shared/src/androidMain` or `androidApp`. Static-analysis configuration is stored under `config/detekt/`.

## KMP Migration Rules for AI Agents
The current Linear project is `Mars Rover Photos`, milestone `Migration to KMP`. This milestone targets Android, iOS, and Desktop. WASM/Web is deliberately deferred to the separate `Web/WASM support` milestone because Room does not support the current WASM setup.

Migrate screen-by-screen instead of doing a broad platform-abstraction refactor first. Start with the shared UI ticket, move the screen into `shared/src/commonMain`, compile the smallest useful slice, and only solve Android-only blockers encountered by that screen. Preserve Android behavior while making the migrated code usable by Android, iOS, and Desktop.

Screen migration order: Rovers home, Rover photos, Image gallery/photo info sheet, Favorites, Popular photos, Mission info, About/settings, Ukraine route decision, then Android widget adaptation.

Do not move these into `commonMain` as-is: `Application`, `Activity`, `ComponentActivity`, manifests, Android intents, splash/edge-to-edge setup, `R.string`/`R.drawable`, Android `painterResource`/`stringResource`, `AndroidViewModel`, `Application` injection, `koin-android`, Firebase Android SDKs, Google Play services/AdMob/UMP, Glance widgets, WorkManager, `room-ktx`, `room-paging`, Android `Room.databaseBuilder(context, ...)`, `SharedPreferences`, `Context`, `Bitmap`, `MediaStore`, `ContentResolver`, Android system UI/theme APIs, platform Ktor engines, AndroidX Navigation 3, `Parcelable`/`@Parcelize`, AndroidX annotations like `@DrawableRes`, and Timber.

Use common-safe replacements instead: KMP `ViewModel`, injected repositories/settings, Compose Multiplatform resources, common interfaces with platform actuals or callbacks, Koin core/Compose/ViewModel APIs in common, Kermit for logging, Ktor core in common with engines in platform source sets, and Room runtime only for the enabled Android/iOS/Desktop targets.

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
