# Versioning, release notes and store releases

Read before bumping the version, editing release notes, or running any fastlane lane. Cutting a
release itself goes through the `store-release` skill, which owns the step order.

## Versioning
The app version lives in **one place**: `buildSrc/src/main/kotlin/AppVersion.kt` (`name` = marketing version, `code` = build number). Android (`androidApp/build.gradle.kts`) and Desktop (`desktopApp/build.gradle.kts`) read it directly at build time. iOS can't read Kotlin, so it's kept in sync via Gradle tasks (group `versioning`, defined in `gradle/versioning.gradle.kts`):

- `./gradlew bumpVersion` — increments `code` by 1 and bumps the **minor** part of `name`, resetting patch (e.g. `3.0.0` → `3.1.0`). Updates `AppVersion.kt` and the iOS project in one shot.
- `./gradlew bumpVersion -PversionName=5.0.0` — same, but sets `name` to the value you pass (must be `major.minor.patch`); `code` is still incremented by 1.
- `./gradlew syncIosVersion` — pushes the current `AppVersion.kt` values into `iosApp/iosApp.xcodeproj/project.pbxproj` (`MARKETING_VERSION` ← `name`, `CURRENT_PROJECT_VERSION` ← `code`). Use this only if you edit `AppVersion.kt` by hand; `bumpVersion` already does it.

Never hand-edit the version in `project.pbxproj` — it is overwritten on the next sync/bump.

## Release notes
The What's New screens read the `release-notes` Firestore collection, not the app binary, so a
version's notes can be written or corrected after that build is already on people's phones.

`scripts/release-notes.json` is the authored source of truth (git-reviewed, one entry per release);
`node scripts/publish-release-notes.mjs` pushes it to Firestore in a single atomic commit and is
idempotent. Add `--dry-run` to inspect the payload first. It authenticates with Application Default
Credentials, so `gcloud auth application-default login` must have been run.

Publishing is a step of its own: `bumpVersion` does not do it. Release notes surface as one
dismissible card at the top of the Rovers list, from the second launch on and never as a launch
dialog: an update card toward the newest `active` release (`active: false` for a build pushed ahead
of store approval — see the `store-release` skill) when that version is newer than the installed
build, otherwise a what's-new card for the installed build once after an update. Tapping the
update card on Android starts Play's own in-app update screen (`platform/AppUpdate`); where Play
offers none, and on iOS, the tap opens the store listing. The update card does
not require an entry matching the installed version exactly, but with nothing published at all there
is no release to nudge toward. Each change names its icon with a Material Symbols ligature (`"rocket_launch"`); the
script warns for
any name missing from the `MaterialSymbol` enum, which would silently render the default symbol.
Regenerate the JSON with the `release-notes` skill.

## Store releases
Cutting a release goes through the `store-release` skill (`.claude/skills/store-release/SKILL.md`),
which owns the whole sequence: bump the version, build and upload Android to the Play internal track,
generate and publish this release's notes concurrently, upload iOS to TestFlight with the same
changelog, then commit and tag locally. Promoting to Play production and submitting to the App Store
stay manual.

App Store header and search-results banners come from the global `store-banners` skill; campaigns
live in `Design/store-banners/<slug>/`. Their imagery source is NASA's public-domain library: search
`https://images-api.nasa.gov/search?q=<query>&media_type=image`, take the original at
`https://images-assets.nasa.gov/image/<nasa_id>/<nasa_id>~orig.jpg` (check its size in the
`metadata.json` beside it), and use mission photos only, never NASA logos or insignia.

Releases are tagged on the version-bump commit (`git tag 5.0.0`), which is what makes
`<prev tag>..HEAD` a trustworthy range for the next release's notes. Tags are created locally;
pushing one needs explicit permission.

`fastlane/Fastfile` carries the lanes: `android beta` (build + upload AAB), `android changelog`
(attach release notes to that upload — a separate call because supply skips all metadata when no
`metadata_path` resolves — then run `android listing`), `android listing` (Play title and
descriptions), `android release` (promote to production), `ios beta` (TestFlight), `ios release`
(App Store binary) and `ios release_notes` (App Store "What's New", promotional text, name,
subtitle, keywords and description; metadata only).

## Samsung Galaxy Store
`fastlane android galaxy` is the whole Samsung release in one lane: it builds the release APK,
re-signs it, pushes the Play listing text plus the staged changelog as the store's "What's New",
attaches the binary and submits the listing for Samsung's review. `skip_build:true` reuses the APK
already in `androidApp/build/outputs/apk/release/`. It runs after the Play upload of the same
version, never concurrently (same Gradle directory), and only when the Galaxy listing is not
already in review; it also refuses a version code the listing already holds.

Samsung has no equivalent of Play App Signing, so the listing still expects the certificate the
app was first published with: `anyNew2.jks`, alias `marsrovers`, not `upload.jks`. The lane
re-signs the Play-signed APK with apksigner rather than building twice. Credentials, all
gitignored:

- `fastlane/galaxy-store.env` — `GALAXY_KEYSTORE`, `GALAXY_KEY_ALIAS`, `GALAXY_KS_PASS`,
  `GALAXY_KEY_PASS`. The passwords are never on the command line; apksigner reads them from the
  environment.
- `fastlane/galaxy-store-key.pem` — private key of the Seller Portal service account (Seller Portal
  → Assistance → API Service → Create Service Account, Content Publish API scope). Samsung shows the
  key once; the service account id and the listing's content id are constants in the Fastfile.

Existing binaries on the listing are left attached: Samsung serves each binary to the devices it
targets, and the two from 2016 and 2019 may be the only builds still offered to very old Android
versions. Removing one is a Seller Portal decision, not the lane's.

The store listing text lives in `fastlane/listing/{android,ios}/en-US/*.txt`, one file per field,
and is committed. Every release uploads it through `android changelog` and `ios release_notes`, so
to change what the store pages say, edit those files and it ships with the next release. The lanes
refuse a field over the store's limit (Play title 30, short description 80, full description 4000;
Apple name and subtitle 30, keywords 100 bytes, description 4000).
The Android lanes refuse to build without a resolvable keystore and the iOS lanes without the App
Store Connect key and `GoogleService-Info.plist`, so a missing credential fails in a second rather
than after an archive.

The store changelog is a separate artifact from the in-app What's New above — written to the
gitignored `.claude/tmp/release-metadata/` and uploaded by fastlane — but both carry the same text,
derived from the same `scripts/release-notes.json` entry. Every credential the release path needs
(`keystore.properties`, `fastlane/google-play-key.json`, `fastlane/AuthKey_*.p8`,
`iosApp/iosApp/GoogleService-Info.plist`) is gitignored, so a fresh worktree has none of them.
Without `keystore.properties` in particular, `bundleRelease` succeeds and produces an unsigned AAB
that Play rejects.
