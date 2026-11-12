#!/usr/bin/env python3
"""Collect an app-health snapshot from the Firebase BigQuery exports.

Usage:
  python3 .claude/skills/app-health/collect.py [--end YYYYMMDD] [--days 28]

Writes <store>/snapshots/<end-date>.json and prints its path. The store sits
outside the repository (the repo is public) at a path every worktree shares.
The snapshot holds two windows of equal length: `cur` (ending at --end,
default = newest GA4 export day) and `prev` (the window right before it).
"""
import argparse
import datetime as dt
import json
import pathlib
import subprocess
import sys

PROJECT = "mars-rover-photos"
GA = f"`{PROJECT}.analytics_153659024.events_*`"
CRASH = {
    "ANDROID": f"`{PROJECT}.firebase_crashlytics.com_sirelon_marsroverphotos_ANDROID`",
    "IOS": f"`{PROJECT}.firebase_crashlytics.com_sirelon_marsroverphotos_IOS`",
}
PERF = {
    "ANDROID": f"`{PROJECT}.firebase_performance.com_sirelon_marsroverphotos_ANDROID`",
    "IOS": f"`{PROJECT}.firebase_performance.com_sirelon_marsroverphotos_IOS`",
}
STORE = (pathlib.Path.home() / ".claude" / "projects"
         / "-Users-sirelon-Projects-MarsRoverPhotos" / "app-health")
OUT_DIR = STORE / "snapshots"


def bq(sql: str) -> list[dict]:
    res = subprocess.run(
        ["bq", f"--project_id={PROJECT}", "query", "--nouse_legacy_sql",
         "--format=json", "--max_rows=10000", sql],
        capture_output=True, text=True,
    )
    if res.returncode != 0:
        sys.exit(f"bq failed:\n{res.stderr or res.stdout}\nSQL:\n{sql}")
    out = res.stdout.strip()
    return json.loads(out[out.index("["):]) if "[" in out else []


def ymd(d: dt.date) -> str:
    return d.strftime("%Y%m%d")


def num(v):
    if v is None:
        return None
    f = float(v)
    return int(f) if f.is_integer() else round(f, 3)


def clean(rows):
    return [{k: (num(v) if isinstance(v, str) and v.replace(".", "", 1).lstrip("-").isdigit() else v)
             for k, v in r.items()} for r in rows]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--end")
    ap.add_argument("--days", type=int, default=28)
    a = ap.parse_args()

    end_s = a.end or bq(f"SELECT MAX(_TABLE_SUFFIX) m FROM {GA}")[0]["m"]
    end = dt.datetime.strptime(end_s, "%Y%m%d").date()
    n = a.days
    cur_start = end - dt.timedelta(days=n - 1)
    prev_end = cur_start - dt.timedelta(days=1)
    prev_start = prev_end - dt.timedelta(days=n - 1)
    win = (f"IF(PARSE_DATE('%Y%m%d', event_date) >= DATE '{cur_start}', 'cur', 'prev')")
    ga_range = f"_TABLE_SUFFIX BETWEEN '{ymd(prev_start)}' AND '{ymd(end)}'"
    ts_range = (f"event_timestamp >= TIMESTAMP('{prev_start}') AND "
                f"event_timestamp < TIMESTAMP('{end + dt.timedelta(days=1)}')")
    ts_win = f"IF(DATE(event_timestamp) >= DATE '{cur_start}', 'cur', 'prev')"

    q = {}

    # Users, new users, sessions, engagement, uninstalls — per platform + ALL.
    q["overview"] = f"""
      WITH e AS (
        SELECT {win} w, platform p, user_pseudo_id u, event_name n,
          (SELECT value.int_value FROM UNNEST(event_params) WHERE key='ga_session_id') sid,
          (SELECT value.int_value FROM UNNEST(event_params) WHERE key='engagement_time_msec') eng
        FROM {GA} WHERE {ga_range})
      SELECT w, IFNULL(p, 'ALL') platform,
        COUNT(DISTINCT u) users,
        COUNT(DISTINCT IF(n='first_open', u, NULL)) new_users,
        COUNT(DISTINCT IF(sid IS NOT NULL, CONCAT(u, '-', CAST(sid AS STRING)), NULL)) sessions,
        ROUND(SUM(IFNULL(eng, 0)) / 60000, 1) engagement_min,
        COUNT(DISTINCT IF(n='app_remove', u, NULL)) uninstalls,
        COUNT(DISTINCT IF(n='app_clear_data', u, NULL)) clear_data
      FROM e GROUP BY GROUPING SETS ((w, p), (w)) ORDER BY w, platform"""

    # Daily active users — for avg DAU, stickiness and the trend line.
    q["daily"] = f"""
      SELECT event_date date, platform, COUNT(DISTINCT user_pseudo_id) dau,
        COUNT(DISTINCT IF(event_name='first_open', user_pseudo_id, NULL)) new_users
      FROM {GA} WHERE {ga_range} GROUP BY 1, 2 ORDER BY 1, 2"""

    # Every event: count + users per window and platform.
    q["events"] = f"""
      SELECT {win} w, platform, event_name, COUNT(*) c, COUNT(DISTINCT user_pseudo_id) u
      FROM {GA} WHERE {ga_range} GROUP BY 1, 2, 3 ORDER BY c DESC"""

    # Retention of first_open cohorts that have had 7 full days to return.
    q["retention"] = f"""
      WITH f AS (
        SELECT user_pseudo_id u, platform p, MIN(PARSE_DATE('%Y%m%d', event_date)) d0
        FROM {GA} WHERE {ga_range} AND event_name='first_open' GROUP BY 1, 2),
      a AS (SELECT DISTINCT user_pseudo_id u, PARSE_DATE('%Y%m%d', event_date) d
            FROM {GA} WHERE {ga_range} AND event_name IN ('user_engagement','session_start','screen_view'))
      SELECT IF(d0 >= DATE '{cur_start}', 'cur', 'prev') w, IFNULL(p, 'ALL') platform,
        COUNT(DISTINCT f.u) cohort,
        COUNT(DISTINCT IF(a.d = DATE_ADD(d0, INTERVAL 1 DAY), f.u, NULL)) d1,
        COUNT(DISTINCT IF(a.d BETWEEN DATE_ADD(d0, INTERVAL 1 DAY) AND DATE_ADD(d0, INTERVAL 7 DAY), f.u, NULL)) week1
      FROM f LEFT JOIN a ON a.u = f.u
      WHERE d0 <= DATE '{end - dt.timedelta(days=7)}'
      GROUP BY GROUPING SETS ((w, p), (w)) ORDER BY w, platform"""

    # New-user activation funnel (same user, anywhere in the window).
    q["funnel"] = f"""
      WITH e AS (SELECT {win} w, platform p, user_pseudo_id u, event_name n FROM {GA} WHERE {ga_range}),
      nu AS (SELECT DISTINCT w, p, u FROM e WHERE n='first_open')
      SELECT nu.w, IFNULL(nu.p, 'ALL') platform,
        COUNT(DISTINCT nu.u) first_open,
        COUNT(DISTINCT IF(n='rover_selected', nu.u, NULL)) rover_selected,
        COUNT(DISTINCT IF(n='PhotoSeen', nu.u, NULL)) photo_seen,
        COUNT(DISTINCT IF(n='tap_fullscreen_photo', nu.u, NULL)) fullscreen,
        COUNT(DISTINCT IF(n IN ('FavoritePhoto','SavePhoto','SharePhoto'), nu.u, NULL)) fav_save_share
      FROM nu LEFT JOIN e ON e.u = nu.u AND e.w = nu.w
      GROUP BY GROUPING SETS ((nu.w, nu.p), (nu.w)) ORDER BY 1, 2"""

    # App version mix over the last 7 days of the current window.
    q["versions"] = f"""
      SELECT platform, app_info.version version, COUNT(DISTINCT user_pseudo_id) users
      FROM {GA} WHERE _TABLE_SUFFIX BETWEEN '{ymd(end - dt.timedelta(days=6))}' AND '{ymd(end)}'
      GROUP BY 1, 2 ORDER BY users DESC"""

    q["countries"] = f"""
      SELECT {win} w, geo.country country, COUNT(DISTINCT user_pseudo_id) users
      FROM {GA} WHERE {ga_range} GROUP BY 1, 2
      QUALIFY ROW_NUMBER() OVER (PARTITION BY w ORDER BY users DESC) <= 10 ORDER BY w, users DESC"""

    q["acquisition"] = f"""
      SELECT {win} w, platform, IFNULL(traffic_source.source, '(none)') source,
        IFNULL(traffic_source.medium, '(none)') medium, COUNT(DISTINCT user_pseudo_id) new_users
      FROM {GA} WHERE {ga_range} AND event_name='first_open' GROUP BY 1, 2, 3, 4
      ORDER BY new_users DESC"""

    # Crashes, per platform whose Crashlytics export table exists.
    crash_tables = bq("SELECT table_name FROM `mars-rover-photos.firebase_crashlytics.INFORMATION_SCHEMA.TABLES`")
    exported = {r["table_name"].rsplit("_", 1)[-1] for r in crash_tables}
    for plat, table in CRASH.items():
        if plat not in exported:
            continue
        q[f"crashes_{plat.lower()}"] = f"""
          SELECT {ts_win} w, is_fatal, COUNT(*) events, COUNT(DISTINCT installation_uuid) installs
          FROM {table} WHERE {ts_range} GROUP BY 1, 2 ORDER BY 1, 2"""
        q[f"crash_issues_{plat.lower()}"] = f"""
          SELECT {ts_win} w, issue_id, ANY_VALUE(issue_title) title, ANY_VALUE(issue_subtitle) subtitle,
            LOGICAL_OR(is_fatal) fatal, COUNT(*) events, COUNT(DISTINCT installation_uuid) installs,
            STRING_AGG(DISTINCT application.display_version, ', ') versions
          FROM {table} WHERE {ts_range} GROUP BY 1, 2
          QUALIFY ROW_NUMBER() OVER (PARTITION BY w ORDER BY events DESC) <= 8 ORDER BY w, events DESC"""

    # Performance: cold start and network failures.
    for plat, table in PERF.items():
        q[f"app_start_{plat.lower()}"] = f"""
          SELECT {ts_win} w, COUNT(*) n,
            APPROX_QUANTILES(trace_info.duration_us, 100)[OFFSET(50)] / 1000 p50_ms,
            APPROX_QUANTILES(trace_info.duration_us, 100)[OFFSET(90)] / 1000 p90_ms
          FROM {table} WHERE {ts_range} AND event_type='DURATION_TRACE' AND event_name='_app_start'
          GROUP BY 1 ORDER BY 1"""
        q[f"network_{plat.lower()}"] = f"""
          SELECT {ts_win} w, event_name host, COUNT(*) requests,
            COUNTIF(network_info.response_code >= 400 OR network_info.response_code IS NULL) failed,
            APPROX_QUANTILES((network_info.response_completed_time_us), 100)[OFFSET(50)] / 1000 p50_ms
          FROM {table} WHERE {ts_range} AND event_type='NETWORK_REQUEST'
          GROUP BY 1, 2 HAVING requests >= 20 ORDER BY 1, requests DESC"""

    data = {}
    for name, sql in q.items():
        print(f"  querying {name}…", file=sys.stderr)
        data[name] = clean(bq(sql))

    snap = {
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "windows": {"cur": [str(cur_start), str(end)], "prev": [str(prev_start), str(prev_end)]},
        "days": n,
        "data": data,
    }
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / f"{end}.json"
    path.write_text(json.dumps(snap, indent=1, ensure_ascii=False) + "\n")
    print(path)


if __name__ == "__main__":
    main()
