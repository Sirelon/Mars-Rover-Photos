#!/usr/bin/env node
// Publishes scripts/release-notes.json to the `release-notes` Firestore collection.
//
// The JSON is the authored source of truth (git-reviewed); Firestore is only the delivery channel.
// One document per release, keyed by version — see docs/ARCHITECTURE.md.
//
//   node scripts/publish-release-notes.mjs [--dry-run] [--project <id>]
//
// Auth is Application Default Credentials via `gcloud auth print-access-token`, i.e. an admin
// credential that bypasses security rules. Run `gcloud auth application-default login` first if the
// token call fails. No npm install needed (Node 18+ for global fetch).

import { readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const REPO = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const SOURCE = resolve(REPO, 'scripts/release-notes.json')
const SYMBOLS = resolve(
  REPO,
  'shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/presentation/ui/MaterialSymbolIcon.kt',
)
const COLLECTION = 'release-notes'

const args = process.argv.slice(2)
const dryRun = args.includes('--dry-run')
const projectId = args.includes('--project') ? args[args.indexOf('--project') + 1] : 'mars-rover-photos'

/** Encodes a plain JS value as a Firestore REST `Value`. */
function toValue(value) {
  if (value === null || value === undefined) return { nullValue: null }
  if (typeof value === 'boolean') return { booleanValue: value }
  if (typeof value === 'number') {
    return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value }
  }
  if (typeof value === 'string') return { stringValue: value }
  if (Array.isArray(value)) return { arrayValue: { values: value.map(toValue) } }
  if (typeof value === 'object') return { mapValue: { fields: toFields(value) } }
  throw new Error(`Cannot encode ${typeof value}: ${JSON.stringify(value)}`)
}

const toFields = (object) =>
  Object.fromEntries(
    Object.entries(object)
      .filter(([, v]) => v !== undefined)
      .map(([k, v]) => [k, toValue(v)]),
  )

/**
 * The app resolves `icon` against the MaterialSymbol whitelist and silently falls back to a default
 * for anything else, so a typo is invisible at runtime. This is the only place it can be caught.
 */
function knownIcons() {
  const source = readFileSync(SYMBOLS, 'utf8')
  return new Set([...source.matchAll(/^\s{4}\w+\("([a-z0-9_]+)"\),/gm)].map((m) => m[1]))
}

const { releases } = JSON.parse(readFileSync(SOURCE, 'utf8'))
if (!Array.isArray(releases) || releases.length === 0) {
  console.error(`No releases found in ${SOURCE}`)
  process.exit(1)
}

const icons = knownIcons()
let warnings = 0
for (const release of releases) {
  for (const change of release.changes ?? []) {
    if (!icons.has(change.icon)) {
      console.warn(
        `WARNING  ${release.version}/${change.id}: icon "${change.icon}" is not in MaterialSymbol — ` +
          `it will render the default symbol. Add it to MaterialSymbolIcon.kt or fix the name.`,
      )
      warnings++
    }
  }
}

const writes = releases.map((release) => ({
  update: {
    name: `projects/${projectId}/databases/(default)/documents/${COLLECTION}/${release.version}`,
    fields: toFields(release),
  },
}))

const changeCount = releases.reduce((n, r) => n + (r.changes?.length ?? 0), 0)
console.log(
  `${dryRun ? '[dry run] ' : ''}${releases.length} releases / ${changeCount} changes → ` +
    `${projectId}/${COLLECTION}${warnings ? `  (${warnings} icon warning(s))` : ''}`,
)

if (dryRun) {
  console.log(JSON.stringify({ writes }, null, 2))
  process.exit(0)
}

const token = execFileSync('gcloud', ['auth', 'print-access-token'], { encoding: 'utf8' }).trim()
const response = await fetch(
  `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents:commit`,
  {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    // A single commit: 13 documents land together or not at all, so a half-published version
    // history is not a state the app can observe.
    body: JSON.stringify({ writes }),
  },
)

if (!response.ok) {
  console.error(`Firestore commit failed (${response.status}):\n${await response.text()}`)
  process.exit(1)
}

const { writeResults } = await response.json()
console.log(`Published ${writeResults?.length ?? 0} documents.`)
for (const release of releases) console.log(`  ${COLLECTION}/${release.version}`)
