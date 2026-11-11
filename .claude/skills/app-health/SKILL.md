---
name: app-health
description: Report how the app is doing from Firebase data (GA4, Crashlytics, Performance via BigQuery) — users, retention, activation funnel, crashes, performance — compared with the previous period and the last stored report, plus insights and actions. Use when the user asks "how is the app going", for an app health / analytics / metrics report, or what changed since the last check.
---

# App health report

Each run stores a **snapshot** (raw numbers, JSON) and a **report** (readable Markdown). The next
run reads the newest older ones, so every report states the change since the last look.

The store is `~/.claude/projects/-Users-sirelon-Projects-MarsRoverPhotos/app-health/`, called
`<store>` below. It sits outside the repository because the repository is public and these are the
app's user and revenue numbers; one fixed path also gives every worktree the same baseline. Keep
both files out of git.

- `<store>/snapshots/<end-date>.json` — written by `collect.py`
- `<store>/reports/<end-date>.md` — written by you

## 1. Collect

```
python3 .claude/skills/app-health/collect.py            # last 28 days ending at the newest export day
python3 .claude/skills/app-health/collect.py --days 7   # weekly view
```

It prints the snapshot path. Each snapshot holds two equal windows: `cur` and `prev` (the window
just before). `bq` auth errors → ask the user to run `! gcloud auth login`. Done when the
snapshot file exists.

## 2. Gather the baseline and extra sources

- **Last run**: the newest snapshot and report in `<store>` older than this one. None →
  this is the first report; say so.
- **Releases**: `git tag --sort=-creatordate --format='%(refname:short) %(creatordate:short)' | head`
  — a release inside a window explains many shifts.
- **iOS crashes**: `crashes_ios` / `crash_issues_ios` appear in the snapshot once the iOS
  Crashlytics BigQuery export has data. Missing → report "iOS crashes: not exported".
- **Revenue**: RevenueCat MCP `get-chart-data`, project `projf1deab13`, chart `revenue`,
  currency USD, `aggregate: ["total"]`, once per window (`start_date`/`end_date` from the
  snapshot), plus `segment: product_id` and `segment: store` for `cur`. Write the totals into
  the snapshot as `data.revenuecat` = `{currency, cur: {revenue, transactions, by_product,
  by_store}, prev: {revenue, transactions}}`. RevenueCat unauthenticated → run its
  `authenticate` tool and hand the user the URL.

## 3. Write the report

Write `<store>/reports/<end-date>.md` with these sections, in order:

1. **Verdict** — one line: growing / flat / declining, and the single biggest reason.
2. **Scorecard** — table with columns Metric · Now · Prev window · Δ · Last report. Rows:
   active users, new users, avg DAU, DAU/MAU, sessions per user, engagement min per user,
   D1 and week-1 retention, revenue and transactions, activation (new users who reached `PhotoSeen`), uninstalls ÷ new
   users, Android crashed installs, app-start p50 per platform. Split Android / iOS where they
   differ. Derive avg DAU from `daily`, rates from their parts.
3. **Funnel** — first_open → rover_selected → photo_seen → fullscreen → fav/save/share, % of step 1.
4. **Reliability** — crash issues (new in `cur`, gone since `prev`), feed_error / feed_loaded,
   ad_load_failed rate, network hosts with failed share > 5 %.
5. **Audience** — version adoption of the latest release, top countries, acquisition sources.
6. **Insights** — 3–6 findings. Each one: the observation with its numbers → the likely cause
   (marked _hypothesis_ when unverified) → a concrete action.
7. **Watch list** — items to re-check next run, each with the number to beat.
8. **Since last report** — every Watch list item from the previous report: resolved, worse, or
   unchanged, with numbers.

Insight quality bar: a finding names a number and a delta; a cause traces to a release, a
country, a version, or an event; an action names a file, a store setting, or an experiment.
Anomalies (a country or source that jumps from ~0, a metric that moves > 50 %) get a drill-down
`bq` query before you explain them.

## 4. Deliver

Reply with the verdict, the top 3 insights, and the report path, within 8 lines. For a visual view, offer an
Artifact page built from the snapshot.

## Gotchas

- Always pass `bq --project_id=mars-rover-photos`; the gcloud default project is another one.
- The user base is ~100–300 MAU: treat percentages as directional and show raw counts beside them.
- Named Compose `screen_view` is reliable only from 5.1.0; funnels use version-stable events.
- Seven event names are PascalCase and frozen: `PhotoSeen`, `Scale`, `SavePhoto`, `SharePhoto`,
  `FavoritePhoto`, `UnFavoritePhoto` and the click `UkraineBanner_Root`. Every app version sends them under these
  names, and the funnels here read `PhotoSeen` and `SavePhoto`. Query them as they are; new events
  use snake_case (`platform/FirebaseTracker.kt`).
- Performance network hosts are aggregated (`host/**`), so per-endpoint breakdowns are impossible.
- `images-assets.nasa.gov` failures are mostly by design: NASA serves no `~large` for about half
  of Spirit/Opportunity items and answers 403, so the viewer's `~large → ~medium → ~orig` chain
  (`utils/NasaImageUrl.kt`) logs misses before it loads. A 2026-11-06 probe of 60 items: grid
  chain 8 % misses, viewer chain 44 %, every item loadable. Flag the host only when its failed
  share rises well above ~40 %, or when `feed_error` / blank-tile reports point at it.
- Crashlytics installs and GA4 users are different ids; crash-free rate is an approximation.
- `app_remove` / `app_clear_data` exist on Android only.
- Revenue truth is RevenueCat. In GA4, `support_purchase` is the purchase *attempt* funnel
  (`result` = success / cancelled / failed, sent from the Support screen); `support_transaction`
  is one event per completed store transaction (any path, incl. App Store promoted purchases).
  Drop `test_store=true` rows. Builds before `support_transaction` shipped missed purchases.
- Google Play pre-launch-report devices and test devices inflate new users from one country with
  near-zero retention; check that before calling it growth.
- The Firebase MCP cannot read Crashlytics here (its `firebasecrashlytics.googleapis.com` host
  does not resolve); crash data comes from the BigQuery export only.
- Performance tables expire after 60 days, so `--days` above 30 loses `prev` perf data.
