---
name: fact-research
description: Research, score, and curate educational Mars facts (with source links) via a multi-agent workflow, then — after user review — write them to the app's Firestore collection. Use when the user wants to find new facts, refresh the educational facts section, or re-run fact research/scoring.
---

# Fact Research Pipeline

Researches educational facts about Mars and its rovers with a multi-agent Workflow, scores them for credibility/readability/interest, produces review tables, and (only after explicit user approval) writes them to Firestore.

## Target data

- Firebase project: `mars-rover-photos`, Firestore database `(default)`
- Collection: `educational_facts_v2`
- Document ID: readable slug derived from the fact text (e.g. `sunsets_blue_instead_red_like`)
- Fields per document:
  | Field | Type | Notes |
  |---|---|---|
  | `text` | string | The fact, 1–2 short sentences, plain language |
  | `section` | string | One of the section names below |
  | `sourceUrl` | string | Page that genuinely states the fact — never invented |
  | `sourceName` | string | Link label, e.g. "NASA JPL" |
  | `score` | int 0–100 | credibility (0–50) + readability (0–25) + simplicity (0–25) |
  | `interest` | int 0–100 | How surprising/delightful for a casual user |
  | `active` | bool | `true` on insert; flip to `false` to retire a fact |

- Canonical sections: `Mars: The Planet`, `Water & Ancient Mars`, `Rover Missions`, `Curiosity & InSight`, `Perseverance & Ingenuity`, `Photography & Cameras`, `Exploration History & Firsts`, `Humans on Mars`.

## Pipeline

1. **Fetch current facts** so researchers don't duplicate them (they already have source links — duplication is now waste, unlike the pre-v2 collection which had none):
   ```bash
   TOKEN=$(gcloud auth print-access-token)
   curl -s -H "Authorization: Bearer $TOKEN" \
     "https://firestore.googleapis.com/v1/projects/mars-rover-photos/databases/(default)/documents/educational_facts_v2?pageSize=300&mask.fieldPaths=text"
   ```
   Follow `nextPageToken` if present. Inline the fact texts into the researcher and scorer prompts as an exclusion list.

2. **Run the Workflow** using `workflow-template.js` in this skill directory as the base. Copy it, inline the exclusion list, adjust topics if the user asked for specific areas. Model/effort assignments (per the user's global subagent model-selection table — do NOT let agents inherit the session model):
   - Research agents: `haiku`, effort `medium` — structured web research with a clear spec
   - Scoring agents: `haiku`, effort `low` — per-item rubric scoring
   - Merge agent: `sonnet`, effort `high` — judgment-heavy synthesis
   - Duplicate-detection agent (step 4): `sonnet`, effort `medium` — single pivotal judge

3. **Render the candidates table** to `.claude/tmp/educational-facts-candidates-<N>.md` (gitignored — never commit): one `## Section` per group, table columns `# | Fact | Source | Score | Interest`. Never overwrite tables from previous runs; use the next free `<N>`.

4. **Detect duplicates** if merging with a previous candidate set: spawn one agent over the combined numbered fact list to find groups stating the same claim in different wording (same claim ≠ same topic). Render a separate `.claude/tmp/educational-facts-duplicates-<N>.md` with the shared claim and each wording. Keep duplicates in the candidate set — different wordings of one claim are useful A/B material for the planned swipe-to-rate feature.

5. **STOP for user review.** Never write to Firestore until the user has reviewed the tables and explicitly approved. Apply any cuts or rules they give ("top 20 per section", "prefer v2 wording", etc.).

6. **Write to Firestore** via `documents:batchWrite` (max 500 writes per call) with slugified doc IDs; verify with a `runAggregationQuery` count afterwards. Slugs: lowercase, first ~5 meaningful words joined by `_`, stopwords removed, `_2`/`_3` suffix on collision. Never delete or overwrite existing documents unless the user asks.

## Rules

- Every fact MUST have a real source URL found during research. Prefer nasa.gov / jpl.nasa.gov / mars.nasa.gov / science.nasa.gov / esa.int; then universities, Britannica, major science outlets.
- Quality over quantity — a per-section cap (default 50) is a ceiling, never a target. No padding with weak facts.
- Facts scoring below 60 or credibility below 25 are dropped at the merge stage.
- Photography/cameras is the flagship topic — the app is about Mars rover photos.
