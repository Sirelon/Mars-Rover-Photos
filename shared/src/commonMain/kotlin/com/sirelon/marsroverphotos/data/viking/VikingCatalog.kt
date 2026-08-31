package com.sirelon.marsroverphotos.data.viking

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.RoverCamera
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * The two Viking landers are the only missions in the app with no photo API behind them. The PDS
 * archive that holds them is a static Apache tree — there is no endpoint that answers "what did
 * Viking 1 photograph on sol 500?" — so the app ships the answer instead: a generated catalogue
 * listing each image with its sol, date and caption.
 *
 * It lists only the frames worth looking at. The Viking cameras were facsimile scanners that built
 * an image one 512-pixel column at a time while turning, so an image's width is purely how far the
 * camera swept; the generator drops anything under 100px, which is a sweep of a few degrees — an
 * instrument reading rather than a picture. It also drops a short list of blank diagnostic
 * frames. 2914 of the archive's 6585 images survive.
 *
 * That is enough to make Viking look exactly like a sol-keyed rover to everything upstream. This
 * class stands in for the network at the [com.sirelon.marsroverphotos.domain.repositories.PhotosRepository]
 * seam, so `SolPagingSource`, the feed, the pickers and the fullscreen pager are untouched.
 *
 * The catalogue is generated and verified by `scripts/generate-viking-catalog.mjs`, which checks
 * every derived URL against the live archive before writing. Regenerate it there, never by hand.
 *
 * Parsing is per-lander and cached for the process: opening Viking 1 does not pay for Viking 2.
 */
class VikingCatalog {

    private val mutex = Mutex()
    private val bySol = mutableMapOf<Long, Map<Long, List<MarsImage>>>()

    /**
     * Photos this lander took on [sol], optionally narrowed to one camera.
     *
     * Returns an empty list for a sol with no photos — the majority of them, since Viking 1
     * imaged on only 374 of its 2,231 sols. `SolPagingSource` treats empty as "keep scanning",
     * so this must never throw for an absent sol.
     */
    suspend fun photosForSol(roverId: Long, sol: Long, camera: String?): List<MarsImage> {
        val photos = catalog(roverId)[sol].orEmpty()
        return if (camera == null) photos else photos.filter { it.camera?.name == camera }
    }

    /** A random photo from the whole mission, for surfaces that want "something to show". */
    suspend fun randomPhoto(roverId: Long): MarsImage? =
        catalog(roverId).values.flatten().randomOrNull()

    private suspend fun catalog(roverId: Long): Map<Long, List<MarsImage>> = mutex.withLock {
        bySol.getOrPut(roverId) {
            val fileName = VikingCatalogParser.resourceFor(roverId)
            try {
                @OptIn(ExperimentalResourceApi::class)
                val text = Res.readBytes("files/$fileName").decodeToString()
                VikingCatalogParser.parse(text, roverId).also {
                    Logger.d("VikingCatalog") { "Parsed $fileName: ${it.size} populated sols" }
                }
            } catch (e: Exception) {
                // An unreadable bundled resource is a packaging fault, not a user-facing error:
                // degrade to an empty mission rather than crashing the feed.
                Logger.e("VikingCatalog", e) { "Failed to read $fileName" }
                emptyMap()
            }
        }
    }
}

/**
 * Parses the generated Viking catalogue text. Kept free of the resource API and of coroutines so
 * the format can be tested directly against fixtures.
 *
 * Format (see `scripts/generate-viking-catalog.mjs`):
 * ```
 * viking-catalog v1 lander=1 volume=vl_0001 notes=187 rows=3542
 * First Lander 1 Image                     <- `notes` caption lines, referenced by index
 * ...
 * A0XX/12A001.BB1|0|1976-07-20|0           <- fileSpec | sol | earthDate | noteIndex
 * ```
 */
object VikingCatalogParser {

    private const val ARCHIVE_BASE =
        "https://planetarydata.jpl.nasa.gov/img/data/vl1_vl2-m-lcs-2-edr-v1.0"

    private const val CREDIT = "NASA/JPL"

    fun resourceFor(roverId: Long): String = when (roverId) {
        VIKING_1_ID -> "viking1_catalog.txt"
        VIKING_2_ID -> "viking2_catalog.txt"
        else -> error("No Viking catalogue for rover id=$roverId")
    }

    /** Returns the lander's photos grouped by sol, in archive order within each sol. */
    fun parse(text: String, roverId: Long): Map<Long, List<MarsImage>> {
        val lines = text.lineSequence().iterator()
        val header = parseHeader(lines.next())
        val notes = List(header.notes) { lines.next() }

        val result = mutableMapOf<Long, MutableList<MarsImage>>()
        while (lines.hasNext()) {
            val line = lines.next()
            if (line.isBlank()) continue
            val (fileSpec, solText, earthDate, noteIndex) = line.split('|', limit = 4)
            val sol = solText.toLong()
            val photos = result.getOrPut(sol) { mutableListOf() }
            photos += toMarsImage(
                fileSpec = fileSpec,
                sol = sol,
                earthDate = earthDate,
                note = notes[noteIndex.toInt()],
                volume = header.volume,
                roverId = roverId,
                order = photos.size,
            )
        }
        return result
    }

    private data class Header(val volume: String, val notes: Int)

    private fun parseHeader(line: String): Header {
        val fields = line.split(' ').mapNotNull { field ->
            field.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()
        require(line.startsWith("viking-catalog v1")) { "Unsupported Viking catalogue header: $line" }
        return Header(
            volume = requireNotNull(fields["volume"]) { "Viking catalogue header has no volume" },
            notes = requireNotNull(fields["notes"]?.toIntOrNull()) { "Viking catalogue header has no notes count" },
        )
    }

    private fun toMarsImage(
        fileSpec: String,
        sol: Long,
        earthDate: String,
        note: String,
        volume: String,
        roverId: Long,
        order: Int,
    ): MarsImage {
        val (dir, file) = fileSpec.split('/', limit = 2)
        val stem = file.substringBefore('.')
        val extension = file.substringAfter('.')
        // The camera number is the 2nd character of the product stem: "12A001" -> camera 2.
        val cameraNumber = stem[1].digitToInt()
        return MarsImage(
            id = "$stem-$extension",
            order = order,
            sol = sol,
            name = note,
            imageUrl = browseUrl(volume, dir, stem, extension),
            earthDate = earthDate,
            roverId = roverId,
            camera = RoverCamera(
                id = cameraNumber,
                name = "VLC$cameraNumber",
                fullName = "Viking Lander Camera $cameraNumber",
            ),
            stats = MarsImage.Stats(see = 0, scale = 0, save = 0, share = 0, favorite = 0),
            description = filterLabel(extension),
            credit = CREDIT,
        )
    }

    /**
     * The browse tree lowercases the raw file's directory and stem, and abbreviates the
     * filter-as-extension to its 1st and 3rd characters: `A0XX/12A001.BB1` becomes
     * `.../extras/browse/a0xx/12a001b1.jpeg`. Verified against all 6,585 served files.
     */
    private fun browseUrl(volume: String, dir: String, stem: String, extension: String): String {
        val filterCode = "${extension[0]}${extension[2]}".lowercase()
        return "$ARCHIVE_BASE/$volume/extras/browse/${dir.lowercase()}/${stem.lowercase()}$filterCode.jpeg"
    }

    /**
     * Human-readable name for the filter wheel position, taken from the raw file's extension.
     * `N06`/`N07`/`N15` carry no filter in the index — they are camera calibration frames.
     */
    private fun filterLabel(extension: String): String = when (extension) {
        "RED" -> "Red filter"
        "GRN" -> "Green filter"
        "BLU" -> "Blue filter"
        "SUN" -> "Solar filter"
        "SUR" -> "Survey scan"
        else -> when (extension.first()) {
            'B' -> "Broadband filter ${extension.last()}"
            'I' -> "Infrared filter ${extension.last()}"
            else -> "Calibration frame"
        }
    }
}
