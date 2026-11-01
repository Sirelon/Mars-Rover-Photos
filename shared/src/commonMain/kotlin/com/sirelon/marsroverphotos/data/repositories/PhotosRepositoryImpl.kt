package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.network.toMarsImages
import com.sirelon.marsroverphotos.data.paging.MER_KEYWORDS
import com.sirelon.marsroverphotos.data.paging.pageQuery
import com.sirelon.marsroverphotos.data.paging.usesPageFeed
import com.sirelon.marsroverphotos.data.viking.VikingCatalog
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository

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

class PhotosRepositoryImpl(
    private val api: RestApi,
    private val vikingCatalog: VikingCatalog,
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
     * Per source: the raw feeds answer with their newest full frame (the final one for the closed
     * InSight and Ingenuity missions); the Viking catalogue with the last frame of its last sol.
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
        else -> api.getLatestFullFramePhoto(roverId)
    }
}
