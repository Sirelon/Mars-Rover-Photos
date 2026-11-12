# Release pitfalls

Confirmed failure causes and store facts for the `store-release` skill ([SKILL.md](SKILL.md)).
Step numbers refer to that file.

## This repo

- **`assembleSharedReleaseXCFramework` fails on `:shared:linkReleaseFrameworkIosArm64` when the
  Kotlin/Native linker runs out of heap.** Gradle surfaces it as a bare "Compilation finished with
  errors"; the real diagnostic is further up the log:

  ```
  e: Compilation failed: Java heap space
  e: java.lang.OutOfMemoryError: Java heap space
  ```

  `:shared:linkReleaseFrameworkIosArm64` and `:shared:linkReleaseFrameworkIosSimulatorArm64` run
  concurrently, and each Kotlin/Native link is memory-hungry. The knob is `kotlin.native.jvmArgs` in
  `gradle.properties` — the linker runs in **its own process**, so neither `org.gradle.jvmargs` nor
  the `kotlin.daemon.jvm.options` nested inside it reaches it. Raising the daemon heap alone
  reproduces the same OOM. Current setting:

  ```properties
  kotlin.native.jvmArgs=-Xmx16G
  ```

  Confirmed 2026-08-31 under Kotlin 2.4.20-Beta1: 4G OOMs, 6G on the daemon (wrong knob) OOMs, 8G on
  `kotlin.native.jvmArgs` links in ~6m. Confirmed 2026-11-12 under Kotlin 2.4.21 with Compose
  Multiplatform 1.13.0-beta02: 12G OOMs in `DevirtualizationAnalysis`, 16G links in ~5m30s. The
  need grows with the Kotlin and Compose versions, so expect to raise it again. Run `./gradlew --stop` after editing, or the old daemon keeps
  the previous value. Re-running unchanged sometimes passes, because the sibling link has already
  finished and freed memory — treat that as luck, not a fix. **Never read this as broken code**, and
  always pull the `e:` lines out of the full log rather than reading `tail`.
- iOS release builds link optimized Kotlin: the framework resolves through the per-configuration
  `KMP_XCFRAMEWORK_DIR` build setting, so Release uses `XCFrameworks/release` and Debug uses
  `XCFrameworks/debug`, and the "Build KMP Framework" phase assembles whichever matches
  `$CONFIGURATION`. Xcode resolves the framework while *planning* the build, before script phases run,
  so the first build in a configuration whose XCFramework directory doesn't exist yet fails archiving
  with "no XCFramework found at ... XCFrameworks/release/shared.xcframework" — **this is not only a
  fresh-worktree problem.** Confirmed 2026-08-20: this repo's long-lived main worktree hit it too,
  because that day's commit (`b09615d7`) had just moved `project.pbxproj` to per-configuration
  resolution, and `XCFrameworks/release/` had simply never been assembled before (only `debug/` had,
  from ordinary iOS dev builds). Same fix either way — one manual
  `./gradlew :shared:assembleSharedReleaseXCFramework` before the first build in that configuration —
  after that Xcode keeps it current. See `iosApp/README.md`.
- Screenshots are a separate job — the `.maestro/` kit and the `store-screenshots` skill. This flow
  never uploads images.
- Listing text (store titles, descriptions, iOS subtitle and keywords) lives in `fastlane/listing/`,
  is committed, and ships with every release through steps 4 and 5. To change the listing, edit
  those files; the next release publishes them.
- The version lives only in `buildSrc/src/main/kotlin/AppVersion.kt`; Android and Desktop read it
  directly, iOS is synced into `project.pbxproj`.

## Apple

- `Info.plist` declares `ITSAppUsesNonExemptEncryption = false`, so App Store Connect no longer asks
  the export-compliance question per build. That declaration covers an app whose only cryptography is
  HTTPS through the OS stack — revisit it if the app ever ships crypto of its own.
- **The App Store product page "What's New" and "Promotional Text" are a separate command from
  TestFlight**, but not a separate *manual step* any more — step 5 now runs
  `fastlane ios release_notes` automatically right after `ios beta`. `ios release` passes
  `skip_metadata: true` and uploads the binary only, and stays a deliberate manual step (it's the
  App Store submission path this skill never runs on its own); `release_notes` pushes metadata only,
  no binary, no submission.
- **Confirmed 2026-08-20, first real run:** the App Store Connect version draft for the new
  version already existed and already matched (`'5.1.0' is the latest version on App Store
  Connect`) right after `ios beta` alone — `ios release` uploading a binary first is **not** a
  prerequisite. Apple appears to keep the next version's
  draft open on its own once the previous one shipped; nothing here creates it.
- **`run_precheck_before_submit: false` is required on that lane.** Without it, deliver's default
  precheck step fails with "Precheck cannot check In-app purchases with the App Store Connect API
  Key (yet)" — a known limitation of API-key auth, unrelated to `submit_for_review: false` already
  being set. This app has no IAP and isn't submitting here, so precheck buys nothing.
- **Pass every field as an explicit per-locale hash, not `metadata_path`.** A `metadata_path`
  mirrors the *entire* local folder onto the live listing, and a field whose file isn't staged
  locally is not guaranteed to survive that sync untouched — the same "incomplete staging tree
  overwrites the live listing" risk already guarded against on the Play side below. Explicit hashes
  touch only the six fields this flow has content for: What's New, Promotional Text, name,
  subtitle, keywords and description. The committed listing text therefore lives in
  `fastlane/listing/`, not the `fastlane/metadata/` folder that deliver and supply load by
  default.
- **Name and subtitle live on App Info, not on the version.** App Info is editable only while a
  version draft exists, which is always the case at this point in the flow. If `release_notes` ever
  fails with "could not find an editable 'App Info'", no draft is open.
- External TestFlight testers need Beta App Review; internal testers don't.
- The app serves AdMob and asks for tracking (`NSUserTrackingUsageDescription`, `SKAdNetworkItems`,
  `PrivacyInfo.xcprivacy`), so App Privacy answers must track the SDKs, and the SKAdNetwork list
  needs periodic re-syncing from Google. Nothing enforces either.

## Google Play

- An unsigned AAB was the likeliest failure; `ensure_signing` now catches it — see step 1.
- `fastlane android release` (promote to production) passes the current `version_code` from
  `AppVersion.kt`. Without one, supply refuses as soon as the internal track holds more than one
  release. If you ever promote something *other* than the current version, pass the code explicitly
  rather than editing the lane.
- Notes set on internal **do** carry to production: promotion copies the whole release object,
  `release_notes` included. No need to re-upload the text when promoting.
- The Data safety form and ads declaration must match what the app does (ads + Firebase). Play blocks
  releases on a stale form and nothing here checks it.
- Play enforces a target API level for updates each August. `targetSdk` is 36, comfortably ahead —
  re-check before a late-summer release.

## Localization

When the app gains a second language, three things in the release flow change: the single `en-US` folder becomes one
per Play *listing* locale, iOS needs `localized_build_info` in the Fastfile (pilot's plain
`changelog` is one language only), and the two stores use different codes for the same language
(`pl-PL` vs `pl`, `ru-RU` vs `ru`) over different supported sets. Never assume the in-app language
list equals either store's listing locales — each store only has the listings someone configured.
