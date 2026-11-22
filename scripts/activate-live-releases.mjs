#!/usr/bin/env node
// Marks a release `active: true` in the `release-notes` Firestore collection once the public can
// install it from both stores. Runs on a schedule from .github/workflows/activate-live-releases.yml;
// safe to run by hand and to run repeatedly.
//
//   node scripts/activate-live-releases.mjs [--dry-run] [--project <id>]
//
// A version counts as live on a store when the newest version that store serves is the same or
// newer: Play's production track (status `completed` or `inProgress`), the App Store's versions in
// READY_FOR_DISTRIBUTION. A release becomes active only when both stores pass. Only inactive
// releases newer than the newest active one are candidates, so an older release set to
// `active: false` by hand stays off.
//
// The script writes only the `active` field. publish-release-notes.mjs keeps an `active: true` it
// finds in Firestore, so the two scripts never undo each other.
//
// Credentials come from the environment in CI and from the gitignored fastlane/ files locally:
//   GOOGLE_PLAY_KEY_JSON   Play service account JSON (default: fastlane/google-play-key.json)
//   FIRESTORE_KEY_JSON     service account with Firestore write access (default: your gcloud login)
//   ASC_PRIVATE_KEY        App Store Connect .p8 contents (default: fastlane/AuthKey_<ASC_KEY_ID>.p8)
// No npm install needed (Node 18+).

import { readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { createPrivateKey, createSign, sign } from 'node:crypto'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const REPO = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const PACKAGE = 'com.sirelon.marsroverphotos'
const ASC_KEY_ID = 'G5TTXS7GV3' // also in fastlane/Fastfile
const ASC_ISSUER_ID = '8873b38c-775e-4a3e-baf5-4e2bada765a8'
const COLLECTION = 'release-notes'

const args = process.argv.slice(2)
const dryRun = args.includes('--dry-run')
const projectId = args.includes('--project') ? args[args.indexOf('--project') + 1] : 'mars-rover-photos'

const fromEnvOrFile = (name, file) => process.env[name] || readFileSync(resolve(REPO, file), 'utf8')
const b64url = (value) => Buffer.from(typeof value === 'string' ? value : JSON.stringify(value)).toString('base64url')

/** -1, 0 or 1 for dotted numeric versions; missing segments count as 0. */
function compareVersions(a, b) {
  const pa = a.split('.').map(Number)
  const pb = b.split('.').map(Number)
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const d = (pa[i] ?? 0) - (pb[i] ?? 0)
    if (d !== 0) return Math.sign(d)
  }
  return 0
}

const newest = (versions) => versions.reduce((max, v) => (max && compareVersions(max, v) >= 0 ? max : v), null)

async function request(url, init, what) {
  const response = await fetch(url, init)
  if (!response.ok) throw new Error(`${what} failed (${response.status}): ${await response.text()}`)
  return response.status === 204 ? null : response.json()
}

/** OAuth access token for a Google service account (JWT bearer grant). */
async function googleToken(serviceAccountJson, scope) {
  const account = JSON.parse(serviceAccountJson)
  const now = Math.floor(Date.now() / 1000)
  const unsigned = `${b64url({ alg: 'RS256', typ: 'JWT' })}.${b64url({
    iss: account.client_email,
    scope,
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 600,
  })}`
  const signature = createSign('RSA-SHA256').update(unsigned).sign(account.private_key).toString('base64url')
  const { access_token } = await request(
    'https://oauth2.googleapis.com/token',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: `${unsigned}.${signature}`,
      }),
    },
    'Google token exchange',
  )
  return access_token
}

/** Newest version name on Play's production track that users are being served. */
async function playLiveVersion() {
  const token = await googleToken(
    fromEnvOrFile('GOOGLE_PLAY_KEY_JSON', 'fastlane/google-play-key.json'),
    'https://www.googleapis.com/auth/androidpublisher',
  )
  const base = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE}/edits`
  const headers = { Authorization: `Bearer ${token}` }
  // Reading a track needs an edit; it is deleted unchanged, so nothing is ever committed.
  const edit = await request(base, { method: 'POST', headers }, 'Play edit insert')
  try {
    const track = await request(`${base}/${edit.id}/tracks/production`, { headers }, 'Play production track')
    const served = (track.releases ?? []).filter((r) => r.status === 'completed' || r.status === 'inProgress')
    // supply names a release after its versionName; a console-made one may read "59 (5.5.0)".
    return newest(served.map((r) => r.name?.match(/\d+\.\d+\.\d+/)?.[0]).filter(Boolean))
  } finally {
    await fetch(`${base}/${edit.id}`, { method: 'DELETE', headers })
  }
}

/** Newest App Store version in READY_FOR_DISTRIBUTION. */
async function appStoreLiveVersion() {
  const key = createPrivateKey(fromEnvOrFile('ASC_PRIVATE_KEY', `fastlane/AuthKey_${ASC_KEY_ID}.p8`))
  const now = Math.floor(Date.now() / 1000)
  const unsigned = `${b64url({ alg: 'ES256', kid: ASC_KEY_ID, typ: 'JWT' })}.${b64url({
    iss: ASC_ISSUER_ID,
    iat: now,
    exp: now + 600,
    aud: 'appstoreconnect-v1',
  })}`
  const signature = sign('sha256', Buffer.from(unsigned), { key, dsaEncoding: 'ieee-p1363' }).toString('base64url')
  const headers = { Authorization: `Bearer ${unsigned}.${signature}` }
  const api = 'https://api.appstoreconnect.apple.com/v1'
  const apps = await request(`${api}/apps?filter[bundleId]=${PACKAGE}`, { headers }, 'App Store app lookup')
  const appId = apps.data?.[0]?.id
  if (!appId) throw new Error(`No App Store app with bundle id ${PACKAGE}`)
  const versions = await request(
    `${api}/apps/${appId}/appStoreVersions?filter[platform]=IOS&limit=50` +
      '&fields[appStoreVersions]=versionString,appVersionState',
    { headers },
    'App Store versions',
  )
  return newest(
    versions.data
      .filter((v) => v.attributes.appVersionState === 'READY_FOR_DISTRIBUTION')
      .map((v) => v.attributes.versionString),
  )
}

const firestoreToken = async () =>
  process.env.FIRESTORE_KEY_JSON
    ? googleToken(process.env.FIRESTORE_KEY_JSON, 'https://www.googleapis.com/auth/datastore')
    : execFileSync('gcloud', ['auth', 'print-access-token'], { encoding: 'utf8' }).trim()

const documentsUrl = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`

const [play, appStore, token] = await Promise.all([playLiveVersion(), appStoreLiveVersion(), firestoreToken()])
console.log(`Live on Play: ${play ?? 'none'}  ·  live on the App Store: ${appStore ?? 'none'}`)

const { documents = [] } = await request(
  `${documentsUrl}/${COLLECTION}?pageSize=300&mask.fieldPaths=active`,
  { headers: { Authorization: `Bearer ${token}` } },
  'Firestore list',
)
const releases = documents.map((d) => ({
  version: d.name.split('/').pop(),
  active: d.fields?.active?.booleanValue ?? true, // the app reads a missing `active` as true
}))
const newestActive = newest(releases.filter((r) => r.active).map((r) => r.version))
const liveEverywhere = (version) =>
  play && appStore && compareVersions(play, version) >= 0 && compareVersions(appStore, version) >= 0

const candidates = releases.filter(
  (r) => !r.active && (!newestActive || compareVersions(r.version, newestActive) > 0),
)
const toActivate = candidates.filter((r) => liveEverywhere(r.version)).map((r) => r.version)
for (const r of candidates) if (!toActivate.includes(r.version)) console.log(`  ${r.version}: not live on both stores yet`)

if (toActivate.length === 0) {
  console.log('Nothing to activate.')
  process.exit(0)
}
if (dryRun) {
  console.log(`[dry run] would activate: ${toActivate.join(', ')}`)
  process.exit(0)
}

const result = await request(
  `${documentsUrl}:commit`,
  {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      writes: toActivate.map((version) => ({
        update: { name: `${documentsUrl.split('/v1/')[1]}/${COLLECTION}/${version}`, fields: { active: { booleanValue: true } } },
        updateMask: { fieldPaths: ['active'] },
        currentDocument: { exists: true },
      })),
    }),
  },
  'Firestore commit',
)
console.log(`Activated ${result.writeResults?.length ?? 0}: ${toActivate.join(', ')}`)
