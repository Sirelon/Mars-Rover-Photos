package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository

/**
 * Implementation of PhotosRepository.
 * Fetches Mars rover photos from the NASA API.
 * Created on 21.02.2021 20:19 for Mars-Rover-Photos.
 */
class PhotosRepositoryImpl(
    private val api: RestApi
) : PhotosRepository {

    override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> {
        return api.getRoversPhotos(query)
    }
}
