# Release timeline (derived from git)

Generated deterministically from versionName/versionCode across the four files that
ever held the app version: `app/build.gradle` -> `androidApp/build.gradle.kts` ->
`buildSrc/src/main/kotlin/AppVersion.kt` (+ `gradle/versioning.gradle.kts` for iOS sync).

Ordering key is **versionCode** (monotonic for Play), not versionName.
`range` is the commit span whose contents shipped in that version.

| code | version | date | release commit | range from prev | commits |
|-----:|---------|------|----------------|-----------------|--------:|
| 1 | 1.0 | 2017-03-05 | `72e5ed2c` | `(root)..72e5ed2c` | 108 |
| 3 | 1.1 | 2016-11-08 | `fbdb0ed0` | `72e5ed2c..fbdb0ed0` | 5 |
| 5 | 1.1.1 | 2016-11-10 | `3c3f8de1` | `fbdb0ed0..3c3f8de1` | 11 |
| 7 | 1.2.0 | 2016-11-17 | `92846f15` | `3c3f8de1..92846f15` | 15 |
| 8 | 1.0.1 | 2016-11-23 | `567f9227` | `92846f15..567f9227` | 1 |
| 8 | 1.2.1 | 2016-11-23 | `d6454eb2` | `567f9227..d6454eb2` | 1 |
| 9 | 1.2.2 | 2017-04-18 | `24ef3330` | `d6454eb2..24ef3330` | 23 |
| 10 | 1.2.3 | 2017-05-22 | `2c746d33` | `24ef3330..2c746d33` | 6 |
| 12 | 1.2.4 | 2017-11-18 | `c0fa7b66` | `2c746d33..c0fa7b66` | 23 |
| 13 | 1.3.0 | 2018-09-02 | `19d4284e` | `c0fa7b66..19d4284e` | 5 |
| 14 | 1.3.1 | 2019-02-25 | `26d3d33e` | `19d4284e..26d3d33e` | 10 |
| 15 | 1.4.0 | 2019-04-03 | `424cdea0` | `26d3d33e..424cdea0` | 21 |
| 16 | 1.4.1 | 2019-04-03 | `6f1f36b6` | `424cdea0..6f1f36b6` | 5 |
| 17 | 1.4.2 | 2019-04-03 | `d55ac566` | `6f1f36b6..d55ac566` | 1 |
| 18 | 1.4.3 | 2019-04-30 | `ee33e78c` | `d55ac566..ee33e78c` | 2 |
| 19 | 1.4.4 | 2019-11-30 | `5a7924e2` | `ee33e78c..5a7924e2` | 11 |
| 20 | 1.4.5 | 2020-05-11 | `97ff01f2` | `5a7924e2..97ff01f2` | 3 |
| 21 | 1.5.0 | 2020-06-14 | `f780f0d3` | `97ff01f2..f780f0d3` | 7 |
| 22 | 1.5.1 | 2020-06-14 | `2b10425e` | `f780f0d3..2b10425e` | 1 |
| 23 | 1.5.2 | 2020-08-30 | `594fb74e` | `2b10425e..594fb74e` | 14 |
| 24 | 1.6.0 | 2020-09-06 | `477af046` | `594fb74e..477af046` | 12 |
| 25 | 1.6.1 | 2021-02-21 | `3d243c11` | `477af046..3d243c11` | 16 |
| 26 | 1.7.0 | 2021-02-22 | `421ff8e2` | `3d243c11..421ff8e2` | 7 |
| 27 | 1.7.1 | 2021-02-26 | `ef2a2ae6` | `421ff8e2..ef2a2ae6` | 28 |
| 28 | 1.7.2 | 2021-04-04 | `83294801` | `ef2a2ae6..83294801` | 33 |
| 30 | 2.0.0 | 2021-05-12 | `2058418d` | `83294801..2058418d` | 58 |
| 31 | 2.1.0 | 2021-05-26 | `8cbf26c4` | `2058418d..8cbf26c4` | 2 |
| 33 | 2.1.1 | 2021-07-25 | `f84c5545` | `8cbf26c4..f84c5545` | 26 |
| 34 | 2.2.0 | 2021-07-29 | `aae9356f` | `f84c5545..aae9356f` | 22 |
| 35 | 2.3.0 | 2021-09-07 | `ab426ee7` | `aae9356f..ab426ee7` | 8 |
| 36 | 2.3.1 | 2022-02-16 | `8e3a00da` | `ab426ee7..8e3a00da` | 22 |
| 37 | 2.3.2 | 2022-09-03 | `6a3a38da` | `8e3a00da..6a3a38da` | 5 |
| 38 | 2.4.0 | 2022-09-06 | `6144edde` | `6a3a38da..6144edde` | 8 |
| 39 | 2.4.1 | 2023-02-22 | `2e8461f3` | `6144edde..2e8461f3` | 6 |
| 39 | 2.4.2 | 2023-04-24 | `ec24fd38` | `2e8461f3..ec24fd38` | 27 |
| 40 | 2.4.3 | 2023-08-21 | `60eb11ae` | `ec24fd38..60eb11ae` | 1 |
| 41 | 2.4.4 | 2023-09-19 | `c0c88880` | `60eb11ae..c0c88880` | 1 |
| 42 | 2.4.5 | 2024-01-16 | `77b0f93d` | `c0c88880..77b0f93d` | 15 |
| 43 | 2.5.0 | 2024-01-16 | `0daf1cbf` | `77b0f93d..0daf1cbf` | 4 |
| 45 | 2.5.1 | 2024-07-06 | `da4afdad` | `0daf1cbf..da4afdad` | 10 |
| 46 | 2.5.2 | 2025-07-26 | `3196308c` | `da4afdad..3196308c` | 4 |
| 47 | 2.5.3 | 2026-05-03 | `f1fe62d6` | `3196308c..f1fe62d6` | 41 |
| 48 | 3.0.0 | 2026-06-12 | `e487e17f` | `f1fe62d6..e487e17f` | 77 |
| 50 | 4.0.0 | 2026-06-12 | `58eca016` | `e487e17f..58eca016` | 8 |
| 51 | 5.0.0 | 2026-06-29 | `06ee1cab` | `58eca016..06ee1cab` | 24 |

## Known anomalies (need a human call)

- versionCode 8 is claimed by both `1.0.1` and `1.2.1` (same day, 2016-11-23) - branch artifact.
- versionCode 39 is claimed by both `2.4.1` and `2.4.2`.
- versionName went *backwards* while code advanced at 1.3.0->1.2.4 and 3.0.0->2.5.3, caused by
  development on `master` while the `publish` branch carried the released version.
- Codes 2, 4, 29, 44, 49 never appear in history (skipped or never committed).
## Tags

44 annotated tags were created locally on the release commits above. **None have been pushed.**

- **`4.0.0` is the one exception and is intentionally left alone.** It points at `6240b293`, not at
  the bump commit `58eca016`. Both commits genuinely carry version 4.0.0 (the bump was 18 commits
  earlier), so the tag is not wrong — just inconsistent with the other 44.
- It is **already published on `origin`**, and it is a *lightweight* tag while the new ones are
  *annotated*. Moving it locally would leave the local tag silently disagreeing with `origin`;
  making it stick would need a force-push over published history. Neither is worth a cosmetic fix.
- **This costs nothing functionally.** Release ranges are derived from version-bump commits, never
  from tag positions — see the guard in `.claude/skills/release-notes/SKILL.md` (Case B). Any
  future run reaches the same boundaries regardless of where this tag sits.

Decided 2026-08-09. Do not re-litigate without a reason to touch the published tag.
