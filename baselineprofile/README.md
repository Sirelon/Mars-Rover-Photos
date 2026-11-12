# Baseline Profile (Android)

The release build ships a Baseline Profile and a startup profile from
`androidApp/src/release/generated/baselineProfiles/`, so ART compiles the startup path and the photo
grid ahead of time instead of interpreting them on first launch. `:baselineprofile` produces them by
driving two journeys (`Journeys.kt`): cold start until the rover list shows, then opening Curiosity and
flinging the photo grid. The UI is found by Compose `testTag` (`roverCard`, `photoGrid`),
so renaming one of those tags breaks generation.

- `./gradlew :androidApp:generateBaselineProfile` — regenerates both files; commit the result. It runs
  on the Gradle-managed `pixel6Api37` emulator, never on a connected device, because installing the
  benchmark build replaces whatever copy of the app is on the device. The photo journey needs network.
- `./gradlew :baselineprofile:pixel6Api37BenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR`
  — `StartupBenchmarks` measures cold start with and without the profile. Emulator timings are only
  meaningful relative to each other.

Regenerate after substantial changes to startup or the photo feed; a stale profile still helps, but
less.
