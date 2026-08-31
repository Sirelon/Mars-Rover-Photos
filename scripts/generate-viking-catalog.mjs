#!/usr/bin/env node
/**
 * Generates the bundled Viking Lander photo catalogues from the PDS Imaging Node archive.
 *
 * Viking has no photo API — the archive is a static Apache tree with one PDS3 fixed-width
 * catalogue (`index.tab` + `index.lbl`) per volume. This script turns that into the compact
 * text resource the app parses at runtime, and *verifies* every derived image URL against the
 * live directory listings before writing, so the committed file is trustworthy without anyone
 * having to re-check 6,585 rows by hand.
 *
 * Instrument readings are excluded — see MIN_BROWSABLE_WIDTH.
 *
 * The dataset has been frozen since 1982; this is expected to run approximately once.
 *
 *   node scripts/generate-viking-catalog.mjs [--dry-run]
 */

import { writeFile } from 'node:fs/promises'

const BASE = 'https://planetarydata.jpl.nasa.gov/img/data/vl1_vl2-m-lcs-2-edr-v1.0'
const OUT_DIR = 'shared/src/commonMain/composeResources/files'

/**
 * Minimum image width, in pixels, to include in the app's feed.
 *
 * The Viking cameras were facsimile scanners: they built an image one vertical 512-pixel column at
 * a time while rotating in azimuth, so every image is exactly 512 tall and its width is purely how
 * far the camera was told to turn (~8.9 px per degree). Commands that swept only a few degrees
 * produced 21-99 px slivers, and they are not photographs of anything — that band is entirely
 * atmospheric optical-depth readings, twilight rescans, photometric studies, reference test charts
 * and internal camera calibration. At 512 tall they would each dominate a feed screen.
 *
 * 100 px is where real subjects start appearing ("Monitor Troughs and Drifts", "Backhoe Magnets"),
 * so that is the floor. Raising it further would start discarding actual scenes.
 */
const MIN_BROWSABLE_WIDTH = 100

/**
 * Observations that are diagnostics rather than pictures, and render as black or blown-out
 * rectangles whatever their width. Measured with ImageMagick over sampled frames — mean pixel
 * brightness out of 255, against 95-195 for ordinary surface photographs:
 *
 *   Erase Image                        7, 10, 14      (camera erase cycle)
 *   Radio Science Experiment           3, 4, 13
 *   Direct Link Realtime Imaging Test  5, 14, 19
 *   Gain / Offset / Gain and Offset    0.04, 1.6, 247 (detector electronics sweeps)
 *   Tape Recorder Track 4 Test         52             (telemetry check)
 *   S-Band Command Antenna Check       49             (telemetry check)
 *
 * Deliberately NOT excluded: "Reference Test Chart" and "Illumination Geometry Reproduction" look
 * like calibration by name but photograph real hardware and terrain (mean 43-116), and the sky and
 * photometric studies are genuine images of the Martian sky.
 */
const NON_PHOTOGRAPHIC_NOTES = new Set([
    'Erase Image',
    'Radio Science Experiment',
    'Direct Link Realtime Imaging Test',
    'Gain Study',
    'Offset Study',
    'Gain and Offset Study',
    'Tape Recorder Track 4 Test',
    'S-Band Command Antenna Check',
])

const VOLUMES = [
    { lander: 1, volume: 'vl_0001', spacecraft: 'VIKING_LANDER_1', out: 'viking1_catalog.txt' },
    { lander: 2, volume: 'vl_0002', spacecraft: 'VIKING_LANDER_2', out: 'viking2_catalog.txt' },
]

const dryRun = process.argv.includes('--dry-run')

async function fetchText(url) {
    const res = await fetch(url)
    if (!res.ok) throw new Error(`${res.status} ${res.statusText} for ${url}`)
    return res.text()
}

/**
 * Parses PDS3 COLUMN definitions out of an index label. START_BYTE is 1-based and points at the
 * field content, not the enclosing quote, so it maps directly onto substring offsets.
 */
function parseColumns(label) {
    const columns = {}
    const blocks = label.matchAll(/OBJECT\s*=\s*COLUMN(.*?)END_OBJECT\s*=\s*COLUMN/gs)
    for (const [, block] of blocks) {
        const name = /NAME\s*=\s*(\S+)/.exec(block)?.[1]
        const start = /START_BYTE\s*=\s*(\d+)/.exec(block)?.[1]
        const bytes = /BYTES\s*=\s*(\d+)/.exec(block)?.[1]
        if (name && start && bytes) columns[name] = { start: Number(start) - 1, length: Number(bytes) }
    }
    return columns
}

const field = (line, col) => line.substring(col.start, col.start + col.length).replace(/"/g, '').trim()

/**
 * Derives the browse-image path from FILE_SPECIFICATION_NAME. The browse tree lowercases the
 * directory and stem, and abbreviates the filter-as-extension to its 1st and 3rd characters:
 * `A0XX/12A001.BB1` -> `a0xx/12a001b1.jpeg`. Verified against every served file, both landers.
 */
function browsePath(fileSpec) {
    const [dir, file] = fileSpec.split('/')
    const [stem, ext] = file.split('.')
    return `${dir.toLowerCase()}/${stem.toLowerCase()}${(ext[0] + ext[2]).toLowerCase()}.jpeg`
}

const SMALL_WORDS = new Set([
    'a', 'an', 'and', 'at', 'for', 'from', 'in', 'into', 'near', 'of', 'on', 'over', 'the', 'to',
    'with', 'after', 'before', 'during',
])
/** Archive acronyms that must survive title-casing. */
const ACRONYMS = new Set(['GCMS', 'XRFS', 'PDA', 'ICL'])

/** `"FIRST LANDER 1 IMAGE"` -> `"First Lander 1 Image"`. Captions are stored all-caps. */
function titleCase(note) {
    // Semicolons in this archive are comma substitutes from the original tabulation.
    return note
        .replace(/;/g, ',')
        .split(' ')
        .map((word, index) => {
            const bare = word.replace(/[^A-Za-z-]/g, '')
            if (ACRONYMS.has(bare)) return word
            const lower = word.toLowerCase()
            if (index > 0 && SMALL_WORDS.has(lower.replace(/[^a-z]/g, ''))) return lower
            // Capitalise each hyphenated part: "PRE-DAWN" -> "Pre-Dawn", "S-BAND" -> "S-Band".
            return lower.replace(/(^|-)([a-z])/g, (_, sep, ch) => sep + ch.toUpperCase())
        })
        .join(' ')
}

/**
 * Lists every browse image in the volume. The partition directories (`a0xx`, `a1xx`, … ) are read
 * from the server rather than assumed — the ranges are not fully populated (VL1 has no `j2xx`),
 * and inventing them would turn a missing directory into a silent zero.
 */
async function servedBrowseFiles(volume) {
    const root = `${BASE}/${volume}/extras/browse`
    const index = await fetchText(`${root}/`)
    const dirs = [...new Set([...index.matchAll(/href="([a-j][0-2]xx)\/"/g)].map(([, d]) => d))]
    console.log(`  ${dirs.length} browse directories`)
    const served = new Set()
    for (const dir of dirs) {
        const html = await fetchText(`${root}/${dir}/`)
        for (const [, name] of html.matchAll(/href="([0-9a-z]+\.jpeg)"/g)) served.add(`${dir}/${name}`)
    }
    return served
}

async function buildVolume({ lander, volume, spacecraft, out }) {
    console.log(`\n${volume} — reading index`)
    const columns = parseColumns(await fetchText(`${BASE}/${volume}/index/index.lbl`))
    const tab = await fetchText(`${BASE}/${volume}/index/index.tab`)

    const rows = tab.split('\n').filter((line) => line.trim().length > 0).map((line) => {
        const fileSpec = field(line, columns.FILE_SPECIFICATION_NAME)
        const stem = fileSpec.split('/')[1].split('.')[0]
        return {
            fileSpec,
            productId: field(line, columns.PRODUCT_ID),
            spacecraft: field(line, columns.SPACECRAFT_NAME),
            // The camera number is the 2nd character of the product stem ("12A001" -> camera 2).
            camera: Number(stem[1]),
            instrument: field(line, columns.INSTRUMENT_NAME),
            sol: Number(field(line, columns.PLANET_DAY_NUMBER)),
            earthDate: field(line, columns.START_TIME).substring(0, 10),
            note: titleCase(field(line, columns.NOTE)),
            width: Number(field(line, columns.LINE_SAMPLES)),
        }
    })
    console.log(`  ${rows.length} rows, sols ${Math.min(...rows.map(r => r.sol))}–${Math.max(...rows.map(r => r.sol))}`)

    for (const row of rows) {
        if (row.spacecraft !== spacecraft) throw new Error(`${row.productId}: unexpected spacecraft ${row.spacecraft}`)
        if (row.instrument !== `CAMERA_${row.camera}`) {
            throw new Error(`${row.productId}: stem says camera ${row.camera}, index says ${row.instrument}`)
        }
        if (!Number.isInteger(row.sol) || row.sol < 0) throw new Error(`${row.productId}: bad sol ${row.sol}`)
        if (!/^\d{4}-\d{2}-\d{2}$/.test(row.earthDate)) throw new Error(`${row.productId}: bad date ${row.earthDate}`)
    }

    console.log('  verifying derived URLs against the live browse tree…')
    const served = await servedBrowseFiles(volume)
    const derived = new Set(rows.map((r) => browsePath(r.fileSpec)))
    const missing = [...derived].filter((p) => !served.has(p))
    const unclaimed = [...served].filter((p) => !derived.has(p))
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
    const wideEnough = rows.filter((r) => r.width >= MIN_BROWSABLE_WIDTH)
    const browsable = wideEnough.filter((r) => !NON_PHOTOGRAPHIC_NOTES.has(r.note))
    console.log(
        `  keeping ${browsable.length} of ${rows.length}: dropped ` +
        `${rows.length - wideEnough.length} narrower than ${MIN_BROWSABLE_WIDTH}px and ` +
        `${wideEnough.length - browsable.length} blank diagnostic frames`
    )
    const sols = browsable.map((r) => r.sol)
    console.log(
        `  browsable range: sols ${Math.min(...sols)}-${Math.max(...sols)}, ` +
        `${new Set(sols).size} populated, last photo ${browsable[browsable.length - 1].earthDate}`
    )

    const rowsOut = browsable
    const notes = [...new Set(rowsOut.map((r) => r.note))]
    const noteIndex = new Map(notes.map((n, i) => [n, i]))
    const lines = [
        `viking-catalog v1 lander=${lander} volume=${volume} minWidth=${MIN_BROWSABLE_WIDTH} ` +
            `notes=${notes.length} rows=${rowsOut.length}`,
        ...notes,
        ...rowsOut.map((r) => `${r.fileSpec}|${r.sol}|${r.earthDate}|${noteIndex.get(r.note)}`),
    ]
    return { out, rows: rowsOut, body: lines.join('\n') + '\n' }
}

const built = []
for (const volume of VOLUMES) built.push(await buildVolume(volume))

// The product id is the Room primary key, so a collision across volumes would silently merge
// two photos (and their favourite state) into one row.
const allIds = built.flatMap((b) => b.rows.map((r) => r.productId))
const distinct = new Set(allIds)
if (distinct.size !== allIds.length) {
    throw new Error(`PRODUCT_ID collision: ${allIds.length} rows but ${distinct.size} distinct ids`)
}
console.log(`\n✓ ${allIds.length} product ids, all distinct`)

for (const { out, body } of built) {
    const path = `${OUT_DIR}/${out}`
    const kb = (Buffer.byteLength(body) / 1024).toFixed(0)
    if (dryRun) {
        console.log(`[dry-run] would write ${path} (${kb} KB)`)
        console.log(body.split('\n').slice(0, 3).join('\n'))
    } else {
        await writeFile(path, body)
        console.log(`wrote ${path} (${kb} KB)`)
    }
}
