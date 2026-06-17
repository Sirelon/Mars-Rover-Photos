package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.models.ImageSourceResponse
import com.sirelon.marsroverphotos.data.network.models.NasaImagesSearchResponse
import com.sirelon.marsroverphotos.data.network.models.PerseveranceCameraResponse
import com.sirelon.marsroverphotos.data.network.models.PerseverancePhotoItemResponse
import com.sirelon.marsroverphotos.domain.models.MarsPhoto
import com.sirelon.marsroverphotos.domain.models.RoverCamera
import com.sirelon.marsroverphotos.utils.nasaImageSmallUrl
import kotlin.math.abs

/**
 * Resolves a raw MSL instrument string to the Curiosity CameraSpec.name it belongs to.
 *
 * The leading token (text before the first '_') is extracted first, then looked up in
 * [MSL_INSTRUMENT_ALIASES]. Aliases are needed when the feed's instrument prefix does not
 * literally match the spec name: the navigation camera is reported as "NAV_LEFT_A" / "NAV_RIGHT_B"
 * etc. (token "NAV") but the CameraSpec.name the filter chip uses is "NAVCAM".
 *
 * Any unrecognised token falls through to the token itself, so future instruments surface as-is
 * rather than silently dropping.
 */
internal fun String.mslInstrumentToken(): String {
    val token = substringBefore('_')
    return MSL_INSTRUMENT_ALIASES[token] ?: token
}

/** Maps leading-token → Curiosity CameraSpec.name for instruments whose prefix differs. */
private val MSL_INSTRUMENT_ALIASES = mapOf(
    "NAV" to "NAVCAM",
)

/**
 * Created on 21.02.2021 20:22 for Mars-Rover-Photos.
 * Mapper functions for converting network models to database entities.
 */

fun List<MarsPhoto>.mapToUi(roverId: Long): List<MarsImage> {
    return mapIndexed { index, marsPhoto ->
        // It's okay to use not correct data for favorite and popular with Stats,
        // 'cause if these images already in database, we'll ignore replacing them.
        MarsImage(
            id = marsPhoto.id,
            sol = marsPhoto.sol,
            name = marsPhoto.name,
            imageUrl = marsPhoto.imageUrl,
            earthDate = marsPhoto.earthDate,
            roverId = roverId,
            camera = marsPhoto.camera,
            favorite = false,
            popular = false,
            order = index,
            stats = defaultStats(),
            description = null,
            credit = null
        )
    }
}

fun List<PerseverancePhotoItemResponse>.preveranceToUI(roverId: Long): List<MarsImage> {
    return mapIndexed { index, response ->
        // It's okay to use not correct data for favorite and popular with Stats,
        // 'cause if these images already in database, we'll ignore replacing them.
        MarsImage(
            id = response.id,
            sol = response.sol,
            name = response.name,
            imageUrl = response.imageSourceResponse?.image() ?: "",
            earthDate = response.earthDate ?: "",
            roverId = roverId,
            camera = response.camera?.toUI(),
            favorite = false,
            popular = false,
            order = index,
            stats = defaultStats(),
            description = response.description,
            credit = response.credit
        )
    }
}

private fun defaultStats() = MarsImage.Stats(0, 0, 0, 0, 0)

fun ImageSourceResponse.image(): String {
    return medium ?: small ?: fullRes ?: large ?: ""
}

fun PerseveranceCameraResponse.toUI(): RoverCamera {
    // Convert camera model type string to a stable positive integer ID
    // Handle Int.MIN_VALUE edge case where abs() would overflow
    val idHash = this.id.hashCode()
    val positiveId = if (idHash == Int.MIN_VALUE) 0 else abs(idHash)
    return RoverCamera(id = positiveId, name = this.name, fullName = this.fullName)
}

/**
 * Maps a NASA Image Library search response to [MarsImage] entities for Spirit/Opportunity.
 *
 * Full-res URL is derived from the preview thumbnail: `~thumb.jpg` suffix → `~orig.jpg`.
 * If the item link does not end in `~thumb.jpg` the href is used as-is (no blind replacement).
 * [roverId] identifies the MER rover that owns the curated library image.
 * [startIndex] offsets [MarsImage.order] so rows from different pages don't collide on `order`.
 */
fun NasaImagesSearchResponse.toMarsImages(roverId: Long, startIndex: Int = 0): List<MarsImage> =
    collection.items.mapIndexedNotNull { localIdx, item ->
        val data = item.data.firstOrNull() ?: return@mapIndexedNotNull null
        val thumbHref = item.links
            ?.firstOrNull { it.render == "image" }?.href
            ?: item.links?.firstOrNull()?.href
            ?: return@mapIndexedNotNull null
        val smallUrl = nasaImageSmallUrl(thumbHref)
        MarsImage(
            id = data.nasaId,
            sol = 0L,
            name = data.title,
            imageUrl = smallUrl,
            earthDate = data.dateCreated.orEmpty(),
            roverId = roverId,
            camera = null,
            favorite = false,
            popular = false,
            order = startIndex + localIdx,
            stats = defaultStats(),
            description = data.description,
            credit = data.center,
        )
    }

/**
 * Maps MSL raw-feed photos to [MarsImage], deriving [RoverCamera] from the flat [MarsPhoto.instrument]
 * string. The camera [name][RoverCamera.name] is the leading token (e.g. "MAST_RIGHT" → "MAST"),
 * which matches Curiosity's CameraSpec.name values so the existing filterByCameras logic works
 * unchanged. The camera [fullName][RoverCamera.fullName] retains the full instrument string.
 */
fun List<MarsPhoto>.mapToUiMsl(roverId: Long): List<MarsImage> {
    return mapIndexed { index, marsPhoto ->
        MarsImage(
            id = marsPhoto.id,
            sol = marsPhoto.sol,
            name = marsPhoto.name,
            imageUrl = marsPhoto.imageUrl,
            earthDate = marsPhoto.earthDate,
            roverId = roverId,
            camera = marsPhoto.instrument?.let { instrument ->
                val token = instrument.mslInstrumentToken()
                val hash = instrument.hashCode()
                RoverCamera(
                    id = if (hash == Int.MIN_VALUE) 0 else abs(hash),
                    name = token,
                    fullName = instrument,
                )
            },
            favorite = false,
            popular = false,
            order = index,
            stats = defaultStats(),
            description = null,
            credit = null,
        )
    }
}
