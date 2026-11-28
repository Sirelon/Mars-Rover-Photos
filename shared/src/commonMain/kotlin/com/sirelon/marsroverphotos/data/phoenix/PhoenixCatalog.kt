package com.sirelon.marsroverphotos.data.phoenix

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.PHOENIX_ID
import com.sirelon.marsroverphotos.domain.models.RoverCamera
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * The Phoenix Mars Lander's photos, served from a bundled catalogue. Like the Viking landers,
 * Phoenix (2008) has no photo API: its PDS archive is a static Apache tree, so the app ships the
 * index of what is in it.
 *
 * The catalogue holds 1,924 of the archive's 32,115 frames from the Surface Stereo Imager and the
 * Robotic Arm Camera. The rest are sun and sky studies, calibration, blank frames and repeats of
 * one scene through other filters; `scripts/generate-phoenix-catalog.mjs` documents each cut. It
 * also verifies every derived URL against the live archive before it writes. Regenerate the
 * catalogue there, never by hand.
 *
 * Every row carries a real sol, so Phoenix looks like any sol-keyed rover upstream of the
 * [com.sirelon.marsroverphotos.domain.repositories.PhotosRepository] seam.
 */
class PhoenixCatalog {

    private val mutex = Mutex()
    private var bySol: Map<Long, List<MarsImage>>? = null

    /**
     * Photos taken on [sol], optionally narrowed to one camera. Returns an empty list for a sol
     * with no photos; `SolPagingSource` treats empty as "keep scanning", so this never throws.
     */
    suspend fun photosForSol(sol: Long, camera: String?): List<MarsImage> {
        val photos = catalog()[sol].orEmpty()
        return if (camera == null) photos else photos.filter { it.camera?.name == camera }
    }

    /** The last photo of the mission's last imaging sol. */
    suspend fun latestPhoto(): MarsImage? = catalog().maxByOrNull { it.key }?.value?.lastOrNull()

    /** A random photo from the whole mission, for surfaces that want "something to show". */
    suspend fun randomPhoto(): MarsImage? = catalog().values.flatten().randomOrNull()

    private suspend fun catalog(): Map<Long, List<MarsImage>> = mutex.withLock {
        bySol ?: run {
            try {
                @OptIn(ExperimentalResourceApi::class)
                val text = Res.readBytes("files/$RESOURCE").decodeToString()
                PhoenixCatalogParser.parse(text).also {
                    Logger.d("PhoenixCatalog") { "Parsed $RESOURCE: ${it.size} populated sols" }
                }
            } catch (e: Exception) {
                // An unreadable bundled resource is a packaging fault, not a user-facing error:
                // degrade to an empty mission rather than crashing the feed.
                Logger.e("PhoenixCatalog", e) { "Failed to read $RESOURCE" }
                emptyMap()
            }
        }.also { bySol = it }
    }

    private companion object {
        const val RESOURCE = "phoenix_catalog.txt"
    }
}

/**
 * Parses the generated Phoenix catalogue text. Kept free of the resource API and of coroutines so
 * the format can be tested directly against fixtures.
 *
 * Format (see `scripts/generate-phoenix-catalog.mjs`):
 * ```
 * phoenix-catalog v1 rows=1924
 * SS000EDN896227848_10C70R1M1|0|2008-05-26T00:09:42Z|672    <- productId | sol | UTC time | filter nm
 * RS001EFF896305363_10D31MBM1|1|2008-05-26T21:41:37Z|       <- the arm camera has no filter
 * ```
 */
object PhoenixCatalogParser {

    private const val ARCHIVE_BASE = "https://planetarydata.jpl.nasa.gov/img/data/phoenix"

    /** Returns the photos grouped by sol, in the order they were taken. */
    fun parse(text: String): Map<Long, List<MarsImage>> {
        val lines = text.lineSequence().iterator()
        val header = lines.next()
        require(header.startsWith("phoenix-catalog v1")) { "Unsupported Phoenix catalogue header: $header" }

        val result = mutableMapOf<Long, MutableList<MarsImage>>()
        while (lines.hasNext()) {
            val line = lines.next()
            if (line.isBlank()) continue
            val (productId, solText, earthDate, wavelength) = line.split('|', limit = 4)
            val sol = solText.toLong()
            val photos = result.getOrPut(sol) { mutableListOf() }
            photos += toMarsImage(productId, sol, earthDate, wavelength, order = photos.size)
        }
        return result
    }

    private fun toMarsImage(productId: String, sol: Long, earthDate: String, wavelength: String, order: Int): MarsImage {
        val instrument = Instrument.forProductId(productId)
        return MarsImage(
            id = productId,
            order = order,
            sol = sol,
            name = instrument.camera.fullName,
            imageUrl = browseUrl(instrument, productId),
            earthDate = earthDate,
            roverId = PHOENIX_ID,
            camera = instrument.camera,
            stats = MarsImage.Stats(see = 0, scale = 0, save = 0, share = 0, favorite = 0),
            description = filterLabel(productId, wavelength),
            credit = instrument.credit,
        )
    }

    /**
     * `SS012EFF897271925_11C60L1M1` is served at
     * `.../phxssi_0xxx/extras/browse/sol012/ss012eff897271925_11c60l1m1.img.jpeg`: the volume
     * comes from the instrument prefix, the sol directory from characters 3-5.
     */
    private fun browseUrl(instrument: Instrument, productId: String): String =
        "$ARCHIVE_BASE/${instrument.volume}/extras/browse/sol${productId.substring(2, 5)}/" +
            "${productId.lowercase()}.img.jpeg"

    /**
     * The stereo imager's filter and eye, from the catalogue's wavelength and the product id's
     * eye letter (the 4th character from the end): `672 nm filter, left eye`.
     */
    private fun filterLabel(productId: String, wavelength: String): String? {
        if (wavelength.isEmpty()) return null
        val eye = when (productId[productId.length - 4]) {
            'L' -> ", left eye"
            'R' -> ", right eye"
            else -> ""
        }
        return "$wavelength nm filter$eye"
    }

    private enum class Instrument(val prefix: String, val volume: String, val camera: RoverCamera, val credit: String) {
        SSI(
            prefix = "SS",
            volume = "phxssi_0xxx",
            camera = RoverCamera(id = 1, name = "SSI", fullName = "Surface Stereo Imager"),
            credit = "NASA/JPL-Caltech/University of Arizona/Texas A&M University",
        ),
        RAC(
            prefix = "RS",
            volume = "phxrac_0xxx",
            camera = RoverCamera(id = 2, name = "RAC", fullName = "Robotic Arm Camera"),
            credit = "NASA/JPL-Caltech/University of Arizona/Max Planck Institute",
        );

        companion object {
            fun forProductId(productId: String): Instrument =
                entries.firstOrNull { productId.startsWith(it.prefix) }
                    ?: error("No Phoenix instrument for product $productId")
        }
    }
}
