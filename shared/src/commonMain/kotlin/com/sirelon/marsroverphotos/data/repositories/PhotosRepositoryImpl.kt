package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.network.toMarsImages
import com.sirelon.marsroverphotos.data.paging.MER_KEYWORDS
import com.sirelon.marsroverphotos.data.paging.pageQuery
import com.sirelon.marsroverphotos.data.paging.usesPageFeed
import com.sirelon.marsroverphotos.data.viking.VikingCatalog
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CancellationException

/**
 * Implementation of PhotosRepository.
 *
 * Chooses where a rover's photos come from: the NASA raw-image APIs for every active or
 * recently-ended mission, and the bundled [VikingCatalog] for the two Viking landers, whose
 * 1976-1982 archive has no API to query.
 *
 * Created on 21.02.2021 20:19 for Mars-Rover-Photos.
 */
/** Opportunity's last mission year; its 2018 images are its own photos, top hit a self-portrait. */
private const val OPPORTUNITY_COVER_YEAR_START = 2018

/** Candidate frames whose pixels are checked for a raw-feed cover, newest first. */
private const val MAX_COVER_CHECKS = 6

class PhotosRepositoryImpl(
    private val api: RestApi,
    private val vikingCatalog: VikingCatalog,
    /**
     * Judges a raw-feed candidate for a rover card ([CoverFrameCheck] in the app). Null takes the
     * newest candidate unchecked.
     */
    private val isGoodCover: (suspend (MarsImage) -> Boolean)? = null,
) : PhotosRepository {

    override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> {
        return if (query.roverId == VIKING_1_ID || query.roverId == VIKING_2_ID) {
            vikingCatalog.photosForSol(query.roverId, query.sol, query.camera)
        } else {
            api.getRoversPhotos(query)
        }
    }

    /**
     * Answered for Ingenuity only. Its 14,553 images sit on 162 of the 1,027 sols it spent on
     * Mars, so an unlucky random anchor can cost dozens of sequential probes before the feed
     * finds a photo; the feed's range operators collapse that to one request. Every other
     * mission is dense enough that the extra round-trip would cost more than it saves.
     */
    override suspend fun nearestSolWithPhotosAtOrAfter(roverId: Long, fromSol: Long): Long? {
        if (roverId != INGENUITY_ID) return null
        return runCatching { api.getIngenuityNearestSolAtOrAfter(fromSol) }.getOrNull()
    }

    /**
     * Per source: the active rovers (Perseverance, Curiosity) with their newest color-camera frame
     * that passes [isGoodCover] (complete and in color; at most [MAX_COVER_CHECKS] tried, none
     * passing means null); the closed InSight and Ingenuity missions with their final frame; the Viking catalogue with the last
     * frame of its last sol.
     * images.nasa.gov has no date order for Spirit and Opportunity, so they take the top hit of
     * the same search their photo feed runs. Opportunity's search starts at 2018: its unfiltered
     * top hit is an artist's concept, and a cover must be a real rover photo.
     */
    override suspend fun latestPhoto(roverId: Long): MarsImage? = when {
        roverId == VIKING_1_ID || roverId == VIKING_2_ID -> vikingCatalog.latestPhoto(roverId)
        roverId.usesPageFeed() -> api
            .searchImages(
                roverId.pageQuery(),
                page = 1,
                pageSize = 1,
                keywords = MER_KEYWORDS,
                yearStart = if (roverId == OPPORTUNITY_ID) OPPORTUNITY_COVER_YEAR_START else null,
            )
            .toMarsImages(roverId)
            .firstOrNull()
        else -> latestGoodFullFrame(roverId)
    }

    private suspend fun latestGoodFullFrame(roverId: Long): MarsImage? {
        val candidates = api.getLatestFullFrames(roverId)
        // Only the active rovers' candidates come from color cameras; the closed missions' final
        // frame is fixed (and Ingenuity's camera is greyscale), so it is taken as it is.
        val check = isGoodCover?.takeIf { roverId == PERSEVERANCE_ID || roverId == CURIOSITY_ID }
            ?: return candidates.firstOrNull()
        return candidates.take(MAX_COVER_CHECKS).firstOrNull { candidate ->
            try {
                check(candidate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("PhotosRepository") { "Cover check failed for ${candidate.imageUrl}: ${e.message}" }
                false
            }
        }
    }
}
