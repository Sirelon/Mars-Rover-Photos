package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.data.database.entities.MarsImage

/**
 * Repository for fetching Mars rover photos from the API.
 * Created on 21.02.2021 20:19 for Mars-Rover-Photos.
 */
interface PhotosRepository {
    /**
     * Fetch photos from the API based on query parameters.
     * @param query Query parameters (rover ID, sol, camera)
     * @return List of Mars images
     */
    suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage>
}
