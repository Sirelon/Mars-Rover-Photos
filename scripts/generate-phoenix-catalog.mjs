#!/usr/bin/env node
/**
 * Generates the bundled Phoenix Mars Lander photo catalogue from the PDS Imaging Node archive.
 *
 * Phoenix (2008) has no photo API. Its archive is a static Apache tree with one PDS3 fixed-width
 * index (`index.tab` + `index.lbl`) per camera volume, the same shape as Viking. This script turns
 * the two camera volumes into the compact text resource the app parses at runtime, and *verifies*
 * every derived image URL against the live browse listings before writing.
 *
 * The archive is 32,115 frames from the two cameras the app shows; about 1,900 survive. Phoenix
 * imaged almost everything several times over, and most of its frames are not pictures anyone
 * would browse:
 *
 *   1. Only full frames and 2x2-decimated frames are kept. The Surface Stereo Imager's windowed
 *      frames (19,858) are sun-tracking crops, telltale (wind sock) crops and 32-px slivers; its
 *      4x1-summed and 4x4-decimated frames (6,375) are sky and atmosphere studies. Decided from
 *      contact sheets of random samples of each mode.
 *   2. Solar-filter frames (SSI_L4/L5/R4/R5) are pointed at the sun and are a black field with a
 *      dot. Flat-field (FFL) and reference-pixel (ERP) products are calibration.
 *   3. Frames whose browse JPEG is smaller than MIN_BROWSE_KB are blank: black, overexposed or a
 *      featureless sky. A JPEG's size tracks how much detail it holds, and the listings carry it
 *      for free.
 *   4. One frame per scene. Phoenix shot a scene through several filters and both stereo eyes in
 *      quick succession, so the raw feed shows the same view four to fifteen times in a row.
 *      See sceneClusters.
 *
 * The Optical Microscope volume (phxom_0xxx) is not included: its frames are abstract strips of
 * soil particles on a substrate wheel, many of them dark, and they would be a third of the feed.
 *
 * The dataset has been frozen since 2008; this is expected to run approximately once.
 *
 *   node scripts/generate-phoenix-catalog.mjs [--dry-run]
 */

import { writeFile } from 'node:fs/promises'

const BASE = 'https://planetarydata.jpl.nasa.gov/img/data/phoenix'
const OUT = 'shared/src/commonMain/composeResources/files/phoenix_catalog.txt'

/** Full frames are 512x512 (SSI) and 512x256 (RAC) browse JPEGs, so RAC's floor is lower. */
const VOLUMES = [
    {
        volume: 'phxssi_0xxx',
        prefix: 'SS',
        camera: 'SSI',
        modes: new Set(['FULL_FRAME', '2X2DECIMATION_FRAME']),
        productTypes: new Set(['EFF', 'EDN']),
        minBrowseKb: 12,
    },
    {
        volume: 'phxrac_0xxx',
        prefix: 'RS',
        camera: 'RAC',
        modes: new Set(['FULL_FRAME']),
        productTypes: new Set(['EFF']),
        minBrowseKb: 8,
    },
]

/** The SSI's solar filters: neutral-density glass for imaging the sun directly. */
const SOLAR_FILTERS = new Set(['SSI_L4_990NM', 'SSI_L5_886NM', 'SSI_R4_936NM', 'SSI_R5_936NM'])

/** Two frames of one sequence further apart than this belong to different scenes. */
const SCENE_GAP_SECONDS = 300

const dryRun = process.argv.includes('--dry-run')

/** The archive answers bursts of parallel requests with HTTP 500, so retry with a pause. */
async function fetchText(url) {
    for (let attempt = 1; ; attempt++) {
        const res = await fetch(url)
        if (res.ok) return res.text()
        if (attempt === 5) throw new Error(`${res.status} ${res.statusText} for ${url}`)
        await new Promise((resolve) => setTimeout(resolve, 1000 * attempt))
    }
}

/** Runs [task] over [items] with at most [limit] in flight. */
async function inPool(items, limit, task) {
    const queue = [...items]
    await Promise.all(Array.from({ length: limit }, async () => {
        while (queue.length) await task(queue.shift())
    }))
}

/** PDS3 COLUMN definitions; START_BYTE is 1-based and points past the opening quote. */
function parseColumns(label) {
    const columns = {}
    for (const [, block] of label.matchAll(/OBJECT\s*=\s*COLUMN(.*?)END_OBJECT\s*=\s*COLUMN/gs)) {
        const name = /NAME\s*=\s*(\S+)/.exec(block)?.[1]
        const start = /START_BYTE\s*=\s*(\d+)/.exec(block)?.[1]
        const bytes = /BYTES\s*=\s*(\d+)/.exec(block)?.[1]
        if (name && start && bytes) columns[name] = { start: Number(start) - 1, length: Number(bytes) }
    }
    return columns
}

const field = (line, col) => line.substring(col.start, col.start + col.length).replace(/"/g, '').trim()

/** `SS012EFF897271925_11C60L1M1` -> `sol012/ss012eff897271925_11c60l1m1.img.jpeg`. */
function browsePath(productId) {
    return `sol${productId.substring(2, 5)}/${productId.toLowerCase()}.img.jpeg`
}

/** Apache prints sizes as `512`, `8.4K`, `1.2M`. */
function toKb(size) {
    const value = parseFloat(size)
    if (size.endsWith('K')) return value
    if (size.endsWith('M')) return value * 1024
    return value / 1024
}

/** Every browse JPEG the server holds for this volume, with its size in KB. */
async function servedBrowseFiles(volume) {
    const root = `${BASE}/${volume}/extras/browse`
    const index = await fetchText(`${root}/`)
    const sols = [...new Set([...index.matchAll(/href="(sol\d{3})\/"/g)].map(([, sol]) => sol))]
    console.log(`  ${sols.length} browse directories`)
    const served = new Map()
    await inPool(sols, 4, async (sol) => {
        const html = await fetchText(`${root}/${sol}/`)
        const entries = html.matchAll(/indexcolname"><a href="([0-9a-z_]+\.img\.jpeg)">.*?indexcolsize">\s*([^<]*?)\s*<\/td>/g)
        for (const [, name, size] of entries) served.set(`${sol}/${name}`, toKb(size))
    })
    return served
}

/**
 * Groups frames into scenes. Product ids end in `<observation:4><x><eye><filter>M1`, and a scene
 * is a run of frames from one observation that cycles through filters and eyes without repeating one:
 * `L1 L2 R6 R7` is one view through four filters, `L1 R1` is a stereo pair. A repeated
 * eye+filter means the camera has moved on, as in a single-filter panorama, and so does a gap of
 * more than SCENE_GAP_SECONDS.
 */
function sceneClusters(rows) {
    const sorted = [...rows].sort((a, b) => a.sclk - b.sclk)
    const scenes = []
    let current = null
    for (const row of sorted) {
        const startsNew = !current ||
            current.sequence !== row.sequence ||
            row.sclk - current.lastSclk > SCENE_GAP_SECONDS ||
            current.filters.has(row.eyeFilter)
        if (startsNew) {
            current = { sequence: row.sequence, filters: new Set(), lastSclk: 0, rows: [] }
            scenes.push(current)
        }
        current.filters.add(row.eyeFilter)
        current.lastSclk = row.sclk
        current.rows.push(row)
    }
    return scenes
}

async function buildVolume({ volume, prefix, camera, modes, productTypes, minBrowseKb }) {
    console.log(`\n${volume} — reading index`)
    const columns = parseColumns(await fetchText(`${BASE}/${volume}/index/index.lbl`))
    const tab = await fetchText(`${BASE}/${volume}/index/index.tab`)

    const rows = tab.split('\n').filter((line) => line.trim().length > 0).map((line) => {
        const productId = field(line, columns.PRODUCT_ID)
        return {
            productId,
            camera,
            sol: Number(field(line, columns.PLANET_DAY_NUMBER)),
            // UTC on the spacecraft clock, millisecond precision: `2008-05-26T00:09:42.931`.
            startTime: field(line, columns.START_TIME).substring(0, 19) + 'Z',
            mode: field(line, columns.INSTRUMENT_MODE_ID),
            filter: field(line, columns.FILTER_NAME),
            productType: productId.substring(5, 8),
            sclk: Number(productId.substring(8, 17)),
            // The 4-character observation id. The 5th character after the underscore changes within
            // one pointing (`_20126R1M1` then `_20120RAM1` is one view through two filters), so it
            // is left out; keying on it shows the same view twice in a row.
            sequence: productId.substring(18, 22),
            eyeFilter: productId.substring(23, 25),
        }
    })
    console.log(`  ${rows.length} rows, sols ${Math.min(...rows.map((r) => r.sol))}–${Math.max(...rows.map((r) => r.sol))}`)

    for (const row of rows) {
        if (!row.productId.startsWith(prefix)) throw new Error(`${row.productId}: expected prefix ${prefix}`)
        if (Number(row.productId.substring(2, 5)) !== row.sol) {
            throw new Error(`${row.productId}: id says sol ${row.productId.substring(2, 5)}, index says ${row.sol}`)
        }
        if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/.test(row.startTime)) {
            throw new Error(`${row.productId}: bad start time ${row.startTime}`)
        }
        if (!Number.isInteger(row.sclk)) throw new Error(`${row.productId}: bad spacecraft clock`)
    }

    console.log('  verifying derived URLs against the live browse tree…')
    const served = await servedBrowseFiles(volume)
    const derived = new Set(rows.map((r) => browsePath(r.productId)))
    const missing = [...derived].filter((p) => !served.has(p))
    const unclaimed = [...served.keys()].filter((p) => !derived.has(p))
    if (missing.length || unclaimed.length) {
        throw new Error(
            `URL derivation mismatch in ${volume}: ${missing.length} derived-but-absent ` +
            `(${missing.slice(0, 5).join(', ')}), ${unclaimed.length} served-but-underived ` +
            `(${unclaimed.slice(0, 5).join(', ')})`
        )
    }
    if (derived.size !== rows.length) throw new Error(`${volume}: ${rows.length} rows collapsed to ${derived.size} URLs`)
    console.log(`  ✓ ${served.size} browse images, exact match`)

    // Filtering happens after verification so the URL check still covers the whole archive.
    const framed = rows.filter((r) => modes.has(r.mode) && productTypes.has(r.productType) && !SOLAR_FILTERS.has(r.filter))
    for (const row of framed) row.kb = served.get(browsePath(row.productId))
    const detailed = framed.filter((r) => r.kb >= minBrowseKb)
    const scenes = sceneClusters(detailed)
    // The largest JPEG of a scene is the frame with the most detail: the best-exposed filter.
    const kept = scenes.map((scene) => scene.rows.reduce((best, row) => (row.kb > best.kb ? row : best)))
    console.log(
        `  keeping ${kept.length} of ${rows.length}: ${framed.length} full or 2x2 frames, ` +
        `${detailed.length} of them at least ${minBrowseKb} KB, in ${scenes.length} scenes`
    )
    return kept
}

const built = []
for (const volume of VOLUMES) built.push(...await buildVolume(volume))
built.sort((a, b) => a.sclk - b.sclk)

const ids = new Set(built.map((r) => r.productId))
if (ids.size !== built.length) throw new Error(`PRODUCT_ID collision: ${built.length} rows, ${ids.size} distinct`)

const sols = built.map((r) => r.sol)
console.log(
    `\n✓ ${built.length} photos, sols ${Math.min(...sols)}-${Math.max(...sols)}, ` +
    `${new Set(sols).size} populated, last photo ${built[built.length - 1].startTime}`
)

/** `SSI_L1_672NM` -> `672`. The Robotic Arm Camera has no filter wheel and reports `N/A`. */
const wavelength = (filter) => /_(\d+)NM$/.exec(filter)?.[1] ?? ''

const body = [
    `phoenix-catalog v1 rows=${built.length}`,
    ...built.map((r) => `${r.productId}|${r.sol}|${r.startTime}|${wavelength(r.filter)}`),
].join('\n') + '\n'

const kb = (Buffer.byteLength(body) / 1024).toFixed(0)
if (dryRun) {
    console.log(`[dry-run] would write ${OUT} (${kb} KB)`)
    console.log(body.split('\n').slice(0, 4).join('\n'))
} else {
    await writeFile(OUT, body)
    console.log(`wrote ${OUT} (${kb} KB)`)
}
