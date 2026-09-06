package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.viking.VikingCatalog
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
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
}
