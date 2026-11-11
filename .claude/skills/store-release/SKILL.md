---
name: store-release
description: >-
  Cut a release end to end: bump the version, generate and publish this release's What's New notes,
  build both apps, upload to Google Play internal testing and TestFlight, and tag the release commit
  locally. Use when the user says "we're ready for release", "release a new version", "ship version
  X.Y", "cut a release", "publish an update", or "bump the version and publish". Covers
  internal/TestFlight only — promoting to Play production and submitting to the App Store are
  separate manual steps this skill never runs.
---

# Release

One entry point for shipping. It owns the version bump, this release's release notes (both the
in-app What's New and the store changelog), both builds, both uploads, and the local tag.

**Scope is beta/internal.** `fastlane android beta` (Play internal track) and `fastlane ios beta`
(TestFlight). Never `android release` (promote to production) or `ios release` (App Store upload) —
the user triggers those when ready.

**This is a skill, not a Workflow**, because the long steps are shell builds and a Firestore push,
which only the main loop can run in the background. The one genuinely delegatable piece — digging
through the diff to work out what shipped — is a single subagent (step 3).

## The shape of it

```
bump version                    ── seconds, must be first
   │
   ├─► fastlane android beta ────────────────► AAB on internal track ──┐
   │      (background, owns Gradle)                                    │
   │                                                                   ├─► Play changelog + listing
   ├─► release-archaeologist agent ─► editorial ─► publish to Firestore ┘
   │      (concurrent, no Gradle)          │
   │                                       └─► store changelog text
   │                                                    │
   └────────► (after Android's Gradle is done) fastlane ios beta ──────► TestFlight
                                                                              │
                                                            fastlane ios release_notes
                                                (What's New + Promo text + listing → ASC draft)
                                                                              │
                                                    commit + local tag ◄──────┘
```

Two constraints fix that order. Both are easy to get wrong:

1. **The bump comes before either build.** `versionCode`/`versionName` are compiled into the AAB and
   the IPA from `AppVersion.kt`, so a build started before the bump carries the old code and Play
   rejects it. "Start the builds first" is only true relative to the *release notes*, not the bump.
2. **The two builds do not run concurrently.** Both drive Gradle in this same project directory —
   Android via `:androidApp:bundleRelease`, iOS via the Xcode "Build KMP Framework" phase, which
   shells out to `./gradlew` for the matching XCFramework. Two Gradle builds in one
   project dir contend on the same project-level caches and `:shared` outputs, so they are
   serialized here rather than risk a lock timeout or a half-written framework mid-archive.

What actually overlaps is the expensive agent work and the Firestore publish, which touch neither
Gradle nor Xcode.

Background jobs notify you when they exit — don't sleep-poll them. Use the waiting time for step 3.

## Step 1 — pre-flight

**Sync with the remote before anything else.** Work merged on GitHub is not in the local tree until
you pull, and nothing downstream catches its absence: the build succeeds, the archaeologist reads a
truthful-but-incomplete `git log`, and the notes describe a release that is missing its headline
feature. The only symptom is the store listing not mentioning work that shipped.

```bash
git fetch --all --prune
git rev-list --left-right --count HEAD...@{u}   # left=local-only  right=remote-only
git log --oneline HEAD..@{u}                    # must be empty before you bump
```

Any remote-only commits → `git pull` and resolve before continuing. Dependency files
(`build.gradle.kts`, `gradle/libs.versions.toml`) are the usual conflict: take the **higher** version
on each pin, keep whichever comment stays factually true against the merged values, then re-run
`./gradlew detekt testDebugUnitTest :shared:desktopTest` before committing the merge — a resolved pin
that does not build is worse than the conflict.

A version code is spent the moment it reaches Play, even on a rejected or replaced upload. If a build
already went up before you noticed the missing work, the fix is a **new code**, not a re-upload.

The lanes guard themselves — `android beta`/`build` refuse to run without a resolvable keystore,
and `ios beta`/`release` refuse without the App Store Connect key and `GoogleService-Info.plist`,
before either build starts. Run the check anyway so a missing credential surfaces once, up front,
rather than one lane at a time:

```bash
for f in keystore.properties fastlane/google-play-key.json \
         fastlane/AuthKey_G5TTXS7GV3.p8 iosApp/iosApp/GoogleService-Info.plist; do
  [ -f "$f" ] && echo "ok   $f" || echo "MISS $f"
done
grep -h storeFile keystore.properties 2>/dev/null    # the .jks path must resolve too
bundle exec fastlane --version >/dev/null 2>&1 || echo "run: bundle install"
gcloud auth application-default print-access-token >/dev/null 2>&1 \
  || echo "run: gcloud auth application-default login"   # needed by the publish script
git status --short                                    # a release wants a clean tree
```

- `keystore.properties` — **`androidApp/build.gradle.kts` applies the release signing config only if
  this file exists.** Without it `bundleRelease` still succeeds and produces an *unsigned* AAB that
  Play rejects at upload. `ensure_signing` in the Fastfile blocks that, and also resolves `storeFile`
  the way Gradle does (relative paths against `androidApp/`) so a stale path fails here too. The
  upload key is `upload.jks`; `anyNew2.jks` belongs to a different app and Play rejects it as a wrong
  signature.
- `fastlane/google-play-key.json` — Play service account. Not guarded by a lane; supply reports it
  itself, after the build.
- `fastlane/AuthKey_G5TTXS7GV3.p8` — App Store Connect API key. `before_all` silently skips
  `app_store_connect_api_key` when it's missing, which is why `ensure_credentials` checks for it
  explicitly rather than letting a finished archive fail at upload.
- `iosApp/iosApp/GoogleService-Info.plist` — copy from `GoogleService-Info.template.plist`.
- `fastlane/galaxy-store.env` + `fastlane/galaxy-store-key.pem` — only for `android galaxy`
  (Samsung Galaxy Store; see `docs/RELEASE.md`). Not part of this skill's beta flow: the Samsung lane
  submits straight to review, so it runs by hand once the Play build is approved.

Ask the user to copy in whatever is missing. Never regenerate a keystore.

## Step 2 — version, then launch the Android build

```bash
./gradlew bumpVersion                       # code +1, minor bump (5.0.0 -> 5.1.0)
./gradlew bumpVersion -PversionName=6.0.0   # code +1, name set explicitly (major.minor.patch)
```

Infer the name from how the user asked: "release 6.0" → `-PversionName=6.0.0`; a hotfix wants an
explicit patch (`-PversionName=5.0.1`), since plain `bumpVersion` bumps the *minor*. If they just
said "we're ready for release" with no version, plain `bumpVersion` is the default — say which
version you picked before building.

`code` always increments. A version code can never be reused on Play, not even one whose upload was
deleted, and both stores reject a build number at or below one they've already seen. Never hand-edit
`project.pbxproj` — `bumpVersion` syncs it.

Then start Android immediately, in the background, and move to step 3 while it runs:

```bash
bundle exec fastlane android beta      # bundleRelease + upload AAB to the internal track
```

This lane needs no release notes, which is exactly why it goes first — nothing blocks it.

## Step 3 — this release's notes, and only this release's

Do not re-run the backfill. Everything already in `scripts/release-notes.json` stays untouched; you
are adding one entry.

**The range.** The previous release's tag is the boundary — verify it sits on the commit that bumped
to that version before trusting it:

```bash
PREV=$(git describe --tags --abbrev=0 HEAD^)        # HEAD^ skips the tag you're about to create
git log -1 --format='%h %s' "$PREV"
git log --reverse --format='%h' -G"name = \"$PREV\"" -- buildSrc/src/main/kotlin/AppVersion.kt | head -1
```

Same commit → the range is `$PREV..HEAD`. Different → use the bump commit, not the tag, and see the
tag rules in [`release-notes`](../release-notes/SKILL.md). Tag `5.0.0` does sit on its bump commit
(`06ee1cab`), so from 5.0.0 onward this check should pass.

**`A..B` is a set difference, not a date range.** Because of merges it can contain commits older
than the tag. Verified: `48519bd4 "Mission Info redesign"` is *not* an ancestor of tag `5.0.0` and
so falls inside `5.0.0..HEAD`, even though the published 5.0.0 notes already describe that redesign.
So: **diff the findings against the entries already in `scripts/release-notes.json` and drop
anything already described there.** Re-announcing a shipped feature is the exact misattribution this
pipeline exists to prevent.

**One agent.** Spawn a single release-archaeologist — `model: sonnet`, `effort: high` — with
[`../release-notes/release-archaeologist-prompt.md`](../release-notes/release-archaeologist-prompt.md)
verbatim, substituting the range and version. Its Rule 6 needs `{{PREV_RELEASE_ENTRY_JSON}}` —
paste in `$PREV`'s `changes` array from `scripts/release-notes.json` — so the agent can catch the
merge-topology overlap described above itself, instead of you re-deriving the exclusion by hand. It
writes
`.claude/tmp/release-notes/raw/<version>.json` and returns a one-line summary. Build the evidence
pack in bash first (step 3 of the `release-notes` skill) so it doesn't spend tokens rediscovering
the file list.

**Do the editorial pass yourself.** It's one release; a second agent buys nothing. Apply the
`release-notes` skill's rules: drop `invisible` impact, merge `related_to` entries, order changes by
importance to the user rather than by date, and don't pad — `maintenance_only` is a legitimate
outcome. Then prepend the entry to `scripts/release-notes.json` (`active: false`, today's date) and
add any Material Symbols ligature the notes use that `MaterialSymbol` in
`presentation/ui/MaterialSymbolIcon.kt` lacks.

**`active: false`, not `true`, every time here.** This step runs right after the build lands on
Play's internal track / TestFlight — nobody outside your testers can install it yet. The app filters
out any `active: false` document everywhere (What's New card, story, Version History), so
publishing this stage as `false` is what keeps the update card from telling a user on the
previous version to go get a build the public store doesn't have. See "Marking a release available"
below for the flip once that stops being true. (`active` also covers retracting a malformed entry —
this is the same flag, just a different reason to be `false`.)

**Publish, in the background:**

```bash
node scripts/publish-release-notes.mjs --dry-run   # counts + icon warnings, writes nothing
node scripts/publish-release-notes.mjs            # background; one atomic Firestore commit
```

The update card is keyed off the newest `active` release compared against the installed build, not off an
entry matching the installed version exactly — a user on 5.1.5 gets nudged toward a published 5.2.0
even though 5.1.5 itself has no entry. What an inactive version *does* block is the card ever
recommending it: nothing to update to shows nothing.

### Marking a release available

Once you've promoted this version to Play production **and** Apple has approved the App Store
release — i.e. once a member of the public, not just an internal/TestFlight tester, can actually
install it — flip that entry to `active: true` and republish:

```bash
# edit scripts/release-notes.json: this version's "active" → true
node scripts/publish-release-notes.mjs
```

This is a separate, later action — often days after this skill's run finishes, once both stores'
review/promotion has actually happened — so it isn't a step this skill executes itself; do it (or
remind the user to) when the production promotion and App Store approval are confirmed.

## Step 4 — store changelog, and the Play upload of it

Condense the same entry into store copy — don't write different words for the store. One headline
line, blank line, then `•` bullets, 2–5 of them, benefit language ("Photos open without a flash",
not "Fix Coil cache key"). **Hard ceiling 500 characters** (Google Play's limit; the App Store's is
4000, so one text serves both). **No emoji** — App Store Connect has rejected metadata containing
it. If the range was maintenance-only, a plain "Bug fixes and performance improvements" is the
honest answer.

```bash
CODE=$(grep -oE 'code = [0-9]+' buildSrc/src/main/kotlin/AppVersion.kt | grep -oE '[0-9]+')
META=.claude/tmp/release-metadata
mkdir -p "$META/android/en-US/changelogs" "$META/ios/en-US"
cat > "$META/android/en-US/changelogs/$CODE.txt" <<'EOF'
<headline>

• <bullet>
• <bullet>
EOF
cp "$META/android/en-US/changelogs/$CODE.txt" "$META/ios/en-US/release_notes.txt"
cat > "$META/ios/en-US/promotional_text.txt" <<'EOF'
<one-sentence teaser, ≤170 chars>
EOF
```

`.claude/tmp/` is gitignored, so this stages the exact bytes that will be published without
polluting the diff. The Android copy is the one the release path reads; the iOS copy plus
`promotional_text.txt` feed `fastlane ios release_notes` in step 5, which pushes both straight to
the App Store Connect version draft — no separate manual step.

**Promotional Text** is Apple-only (Play has no equivalent field) and appears in a different place
on the store listing than "What's New", so don't just copy the changelog headline verbatim — write
a distinct one-sentence teaser, same theme, **hard ceiling 170 characters**, no emoji.

The Android filename **must** be the numeric version code: `supply` looks for `<version_code>.txt`,
falls back to `default.txt` in the same folder, and ignores anything else, so a misnamed file ships
empty notes. The `android changelog` lane checks the file exists for the current code and refuses
over 500 characters, so a typo fails in a second instead of shipping silence.

Once the Android lane from step 2 has finished, attach the changelog:

```bash
bundle exec fastlane android changelog
```

A second call is needed because `fastlane android beta` uploads **no** notes: `upload_to_play_store`
derives `metadata_path` from `./fastlane/metadata/android`, which doesn't exist here, and skips all
metadata when that path is nil. The lane looks up the release for the current version code in the
internal track, so the AAB must already be there.

The lane passes `skip_upload_metadata`/`images`/`screenshots` — not politeness. With a
`metadata_path` set, `supply` otherwise walks **every** directory under it and runs `listing.save()`
plus an image and screenshot sync per locale, so an incomplete staging tree could overwrite the live
store listing. Those flags live in the Fastfile precisely so no invocation can forget them.

The same lane then runs `android listing`, which uploads the committed store listing text
(`fastlane/listing/android/en-US/`: `title`, `short_description`, `full_description`) in a second
edit. supply sets only the fields that have a file, so the video URL, images and screenshots are left
alone. The listing is app-wide rather than per-track, so the new text goes live after Google's
review, usually before this build reaches production. If the text describes a feature that isn't in
production yet, that feature shows on the listing before users can get it. The lane refuses any field
over Play's limit. `bundle exec fastlane android listing validate_only:true` checks an edit with
Google without publishing it.

**Locale.** English-only app, so one locale: `en-US`. Confirmed 2026-09-25: `en-US` is the only
listing on Play. To re-check after adding a store language:

```bash
bundle exec fastlane run download_from_play_store \
  package_name:com.sirelon.marsroverphotos json_key:fastlane/google-play-key.json \
  metadata_path:.claude/tmp/play-listing-check   # must NOT already exist — supply no-ops on an existing dir
ls .claude/tmp/play-listing-check           # locale folders that actually have a listing
```

Only push a changelog for a locale with a real listing. Google validates the whole edit atomically,
so one locale without a base listing fails the entire upload — and the error may name whichever
locale it checked first, not the one that's actually missing.

## Step 5 — iOS

Once the Android lane's Gradle work is done and the changelog is staged:

```bash
bundle exec fastlane ios beta
```

The lane reads the same staged file for TestFlight's "What to Test" — no env var to quote. If the
file is missing it warns and falls back to `$CHANGELOG`, then to "Bug fixes and improvements", so a
missing note degrades rather than fails; watch for that warning, since it means step 4 hasn't run.

Background it; this is usually the longest step. The lane runs `syncIosVersion`, builds with
`-allowProvisioningUpdates`, and uploads with `skip_waiting_for_build_processing: true` — which,
*with* a changelog, makes pilot wait just long enough for the build to appear in App Store Connect,
set the changelog, and skip the rest. So the text does land and the run stays short.

Once that succeeds, immediately follow with the App Store Connect metadata push — it's metadata
only (`skip_binary_upload`, `submit_for_review: false`), so it stays inside this skill's
beta/internal scope; it does not submit anything:

```bash
bundle exec fastlane ios release_notes
```

This pushes the staged `release_notes.txt` (What's New) and `promotional_text.txt` (Promotional
Text) to the App Store Connect version draft, plus the committed listing text in
`fastlane/listing/ios/en-US/` (`name`, `subtitle`, `keywords`, `description`). Apple locks all four
to a version, so they go live when this version is approved. The lane refuses any field over
Apple's limit, measuring keywords in bytes. Confirmed 2026-08-20: that draft already exists and
already matches the current marketing version right after `ios beta` alone — `ios release` (the
separate, still-manual App Store binary upload) is **not** a prerequisite, contrary to what an
earlier version of this doc assumed. If the lane ever reports the version doesn't match, that's a
real anomaly worth investigating, not the expected case.

## Step 6 — commit and tag

Now the tree holds the bump, the notes JSON, and any icon addition. Verify, then commit them
together and tag that commit:

```bash
./gradlew detekt testDebugUnitTest :shared:desktopTest
git commit -am "Release <version>"
git tag <version>
```

Tagging the bump commit is what keeps the boundary in step 3 trustworthy for the *next* release —
it's the whole reason the tag exists. **Never push the commit or the tag.** Pushing needs explicit
permission, and the user verifies a release on-device first.

## Step 7 — report

- The version, and the store text, once — both stores got the same string.
- Which of the six things landed: Play internal upload, Play changelog, Play listing text,
  TestFlight, Firestore, App Store Connect metadata (What's New, Promotional Text and the listing
  text on the version draft).
- That the release commit and tag are **local and unpushed**.
- Anything still manual: production promotion, App Store submission, flipping this release to
  `active: true` in Firestore once both of those land (see "Marking a release available" in step 3),
  and the outstanding items below.

## When a step fails

Read [PITFALLS.md](PITFALLS.md) before you diagnose. It holds the confirmed failure causes for this
repo, App Store Connect and Google Play: the Kotlin/Native linker heap, the missing release
XCFramework, the fastlane `deliver` and `supply` traps, and the store forms nothing here checks.
