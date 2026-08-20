---
name: release-notes
description: Generate release notes for this app from git history via a release-archaeologist agent fan-out, then an editorial pass that produces user-facing copy for the What's New screen. Use when the user wants release notes, a changelog, "what shipped in version X", to backfill version history, or to publish notes after a release. Handles both incremental (since last tag) and full-history bootstrap.
---

# Release notes pipeline

Recovers what actually shipped in each release **from the code**, not from commit messages, and
turns it into user-facing copy for the app's What's New screen.

Target: `scripts/release-notes.json`, published to the `release-notes` Firestore collection with
`node scripts/publish-release-notes.mjs`. The app fetches it at launch — nothing is compiled in, so
notes for a shipped version can still be written or fixed. Shape mirrors `Release` / `Release.Change`
in `domain/releasenotes/Release.kt`; UI lives in `presentation/screens/whatsnew/`.

**Cutting a release enters through [`store-release`](../store-release/SKILL.md), not here.** That
skill orchestrates the bump, the builds, the uploads and the tag, and calls into this one for a
single range: one archaeologist agent, editorial done inline, one entry prepended to
`scripts/release-notes.json`. Use this skill directly for a backfill, a correction, or a question
about what shipped in some past version.

Because `store-release` tags the version-bump commit, the Step 1 guard below gets easier for every
release from 5.0.0 onward — tag `5.0.0` already sits on its bump commit. Older tags don't; keep
validating rather than trusting.

## Where output goes

**Committed** — `docs/release-notes/`:
- `VERSIONS.md` — the version/date/range timeline, plus anomalies and tag decisions
- `RELEASE_NOTES.md` — human review copy: proposed changes, types, screenshot suggestions
- `EDITORIAL_NOTES.md` — decisions, drops, merges, overrides, open questions

**Throwaway** — `.claude/tmp/release-notes/` (gitignored, see `.gitignore`):
- `raw/<version>.json` — one release-archaeologist's findings per file
- `release-notes.json.proposed` — staging copy, deleted once merged into `scripts/release-notes.json`

**Never commit the raw JSON.** It is intermediate scratch: bulky, one file per release, superseded
the moment the editorial pass runs, and it goes stale the instant anyone re-runs the pipeline. The
durable record is `EDITORIAL_NOTES.md` (why each decision was made) plus git itself (the evidence
the findings were derived from) — both survive without it.

Evidence packs are scratch too: `/tmp/release-packs/*.txt` and `/tmp/ranges.tsv`. Regenerate them
with the Step 3 bash rather than keeping them.

Because the raw JSON is disposable, **nothing durable may reference it by path.** Do not cite
`raw/<version>.json` in `scripts/release-notes.json`, in committed docs, or in a commit message — those
references dangle the moment the folder is cleaned.

## RULE 0 — commit messages are not evidence

This repo's commit subjects are unreliable and routinely misdescribe what shipped. **Verified:**
the v1.3.0 release commit is titled *"Update gradle"* but that range shipped the entire Popular
Photos feature (`feature/popular/`, `PopularPhotosActivity.kt`, string `popular_title`).

Derive every finding from the diff. Subjects may be read as hints, never cited as evidence, and
never used to conclude "nothing shipped". **If a subject contradicts the code, the code wins.**

## Step 1 — the guard (always run first)

The **version file is the source of truth; tags are a cache that can be stale or wrong.** Do not
trust a tag without validating it.

```bash
CUR=$(grep -oE '"[0-9]+\.[0-9]+\.[0-9]+"' buildSrc/src/main/kotlin/AppVersion.kt | tr -d '"')
TAG=$(git describe --tags --abbrev=0 2>/dev/null)
git show "$TAG:buildSrc/src/main/kotlin/AppVersion.kt" 2>/dev/null | grep -oE '"[0-9.]+"'
```

Then branch:

| Case | Condition | Action |
|---|---|---|
| **A. No tags** | `git tag` empty | **Bootstrap** — tell the user there's no tagged history and run Step 2 (full discovery) before anything else. Do not guess a range. |
| **B. Tag off-bump** | tag's commit is not the commit that *bumped* to that version | **Report, then use the bump commit as the boundary** — never the tag's commit. Offer to move the tag. |
| **C. Tag lies** | tag's commit doesn't carry that version at all | **Stop and report.** Do not guess a range. |
| **D. Behind** | `CUR` > latest tag | **Incremental** — generate for each untagged release. There may be more than one. |
| **E. Current** | `CUR` == latest tag | Nothing new to document. Say so and exit. |

**Boundaries come from version-bump commits, not from tags.** A tag can sit anywhere inside a
version's window and still be valid. Case B is live here: tag `4.0.0` points at `6240b293`, which
genuinely carries 4.0.0 — but the *bump* to 4.0.0 was `58eca016`, **18 commits earlier**. Starting
from the tag would shift 5.0.0's range by those 18 commits.

Find the bump commit for a version, don't assume the tag is it:

```bash
# first commit where AppVersion.kt changed to this version
git log --reverse --format='%H' -G"name = \"$VER\"" -- buildSrc/src/main/kotlin/AppVersion.kt | head -1
```

**Convention in this repo:** the version is bumped at/near ship time, so the content of version N
is the range `<bump-to-(N-1)>..<bump-to-N>` — the bump commit *closes* a release rather than
opening one. This was validated against the 5.0.0 output. Confirm with the user if a range's
findings look like they belong to the neighbouring release.

## Step 2 — full discovery (bootstrap only)

Pure bash, no agent, no token cost. Four files have ever held the version:

`app/build.gradle` → `androidApp/build.gradle.kts` → `buildSrc/src/main/kotlin/AppVersion.kt`
(plus `gradle/versioning.gradle.kts`, which only syncs iOS).

```bash
git log --reverse --format='%H|%ad|%s' --date=short -- \
  app/build.gradle androidApp/build.gradle.kts \
  buildSrc/src/main/kotlin/AppVersion.kt build.gradle shared/build.gradle.kts
```

For each commit, read `versionName`/`name` and `versionCode`/`code` out of the first file that has
them, then:

- **Order by `versionCode`, never by `versionName`.** Names go *backwards* in this repo
  (`1.3.0`→`1.2.4`, `3.0.0`→`2.5.3`) because development ran on `master` while the `publish`
  branch carried the released version.
- Per version name, keep the row with the **highest** versionCode — that's the release point.
- Expect anomalies. Record them in `VERSIONS.md` rather than silently resolving them: duplicate
  codes, codes never committed, name/code disagreements.

The range that shipped in version N is `<release commit of N-1>..<release commit of N>`.

**The first version is a special case — the pipeline cannot cover it.** It has no previous release
to diff against, so no evidence pack and no findings are produced, and it silently falls out of
scope. Two consequences:

- Its **entry must be authored by hand**, from the file tree at its last commit
  (`git ls-tree -r <sha>`, plus its `strings.xml`) rather than from a range diff. Without this the
  history opens on whatever the second release was — here, a crash fix.
- Its **date is unreliable.** "Keep the last commit carrying each versionName" misfires on the root
  version, which often lingers on a stale branch long after launch: `1.0` dated to 2017-03-05, four
  months after the app actually shipped. Look for real ship evidence instead — store screenshots, a
  `publish` branch merge, signing config — and treat the result as inferred until confirmed.

## Step 3 — evidence packs (bash, free)

Precompute per range so agents don't burn tokens rediscovering it:

- added / deleted `.kt` `.java` `.swift` paths — **the package layout is self-labeling**
  (`feature/popular/`, `presentation/screens/whatsnew/`)
- modified files by churn (`git diff --numstat | sort -rn`)
- new strings from `*.xml`, filtering out `ca-app-pub*`, `*api_key*`, `google_*`, `gcm_*`, `firebase_*`
- layout/drawable/`composeResources` churn — catches redesigns that add no `.kt`
- manifest deltas, dependency additions

**KMP-era caveat:** post-migration code keeps UI text inline in composables, not `strings.xml`, so
the new-strings signal returns nothing. Agents must read added composables for user-visible text.

**Do not pre-filter ranges as "maintenance" with bash.** Measured on the full backfill: the
heuristic (no added files + no new strings) predicted **14** maintenance-only ranges; the agents
found **6**. Eight releases with real user-facing content — including 1.4.4 and 1.7.0 — would have
been silently dropped. A range can ship a redesign via layouts alone, or a feature via edits inside
existing files. Let the agent decide.

## Step 4 — release-archaeologist fan-out

One agent per range, **model: sonnet**, in parallel. Prompt template:
[release-archaeologist-prompt.md](release-archaeologist-prompt.md) — use it verbatim, substituting range/version.

Each agent writes `.claude/tmp/release-notes/raw/<version>.json` and returns a **one-line summary only**.
Returning full JSON floods the orchestrator's context for no benefit.

**Measured cost** (full 44-range backfill): ~$18 / ~4.7M tokens / 48 runs. Average ~95k tokens and
~22 tool calls per agent; a 1-commit range ~55k, the KMP range ~158k. Budget ~$0.40 per range.

**Run in batches of ~11.** Expect occasional `API Error: Connection closed mid-response` — 2 of 44
died that way. They are not retryable in place, so after each batch diff the expected version list
against `.claude/tmp/release-notes/raw/*.json` and re-spawn whatever is missing:

```bash
cut -f1 /tmp/ranges.tsv | while read v; do [ -f ".claude/tmp/release-notes/raw/$v.json" ] || echo "missing: $v"; done
```

Also validate every file parses — a killed agent can leave a truncated write:
`for f in .claude/tmp/release-notes/raw/*.json; do python3 -c "import json;json.load(open('$f'))" || echo "INVALID $f"; done`

## Step 5 — editorial pass

One agent, **model: opus**, reads all `raw/*.json` and produces user-facing copy matching
`Release.Change` (`id`, `icon`, `title`, `summary`, `detail`, optional `imageUrl`).

- Drop `maintenance_only` releases entirely — do not pad them.
- Filter on `user_impact`; `invisible` never reaches a user.
- Merge entries linked by `related_to` into one user-facing item.
- `icon` is a Material Symbols ligature name from fonts.google.com/icons (`"rocket_launch"`,
  `"bug_report"`). Prefer one already in the `MaterialSymbol` enum in
  `presentation/ui/MaterialSymbolIcon.kt` — the app falls back to a default symbol for any name it
  does not know, and the publish script warns about those. Adding an entry to that enum is the fix;
  verify the ligature is a real Material Symbol first.
- May suggest a screenshot per entry.
- **Order changes within a card by importance to the user, not by date.** Reverse-chronological
  ordering is wrong and buries headlines — it once put Perseverance third of five. Rank:

  1. **A new rover or lander joining the app** — this happens a handful of times a decade. Also the
     app's own first release. Always first, whatever else is in the card.
  2. **A new capability** — something the user could not do at all before.
  3. **Restored access** — content or a feature that was unreachable now works.
  4. **A visible improvement** to something that already worked.
  5. **Fixes.**
- **Consider merging thin releases into denser cards.** A card carrying one small change ("Steady
  Lists") is not worth reading. Consecutive thin releases can be folded into one card that keeps
  the newest version and date of the group. Propose this; do not do it unasked.
  If you do merge, record the mapping — a change then sits under a later version than it shipped
  in, which is indistinguishable from the misattribution bug this pipeline exists to fix, and a
  future reader will otherwise "correct" it back.

## Step 6 — apply, verify, publish, clean up

1. Merge the staged proposal into `scripts/release-notes.json` — newest release first, `active: true`.
   Editing an existing entry is a legitimate fix; the app reads Firestore, so corrections reach users
   who already updated.
2. Add any icon ligature the notes use that `MaterialSymbol` lacks
   (`presentation/ui/MaterialSymbolIcon.kt`). Nothing forces this at compile time any more — an
   unknown name renders the default symbol — so the publish script's warnings are the check.

Verify, then publish:

```bash
node scripts/publish-release-notes.mjs --dry-run   # counts + icon warnings, writes nothing
./gradlew detekt :shared:desktopTest               # only needed if you touched Kotlin
node scripts/publish-release-notes.mjs            # one atomic commit of every release
```

Publishing is what ships the notes. A release left unpublished shows no What's New dialog at all.

Then delete the scratch — `.claude/tmp/release-notes/`, `/tmp/release-packs/`, `/tmp/ranges.tsv`.
Keep only the three committed docs. Re-running the pipeline regenerates everything else.

## Hard rules

- **Never invent a release item.** `maintenance_only: true` is a correct, welcome answer. Most
  releases in this repo are dependency bumps.
- **Scope is the shipped app**: `shared/`, `androidApp/`, `iosApp/`, `desktopApp/`, `webApp/`.
  Out: `index.html` (marketing site), `docs/`, `.maestro/`, `graphify-out/`, CI, markdown.
  App Store metadata (privacy manifest, SKAdNetwork) is `internal`.
- **Never push tags.** Creating local tags is fine; pushing needs explicit user permission.
- Existing entries in `scripts/release-notes.json` have been wrong before — the list once declared a
  version `4.2.0` that never shipped, with misdated features. Verify against `VERSIONS.md`, don't
  trust it. The same goes for what is already live in Firestore.
