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

    /**
     * First sol at or after [fromSol] that holds photos, resolved in one request.
     *
     * Only for picking an opening anchor on a sparse feed, where most sols are empty and the
     * paging source would otherwise probe them one network round-trip at a time. Returns null
     * when the source cannot answer — the caller then keeps its original sol and lets the normal
     * scan find the photos, which is slower but equally correct.
     */
    suspend fun nearestSolWithPhotosAtOrAfter(roverId: Long, fromSol: Long): Long? = null
}
