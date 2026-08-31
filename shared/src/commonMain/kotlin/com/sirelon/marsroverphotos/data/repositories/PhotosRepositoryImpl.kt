package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.viking.VikingCatalog
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
}
