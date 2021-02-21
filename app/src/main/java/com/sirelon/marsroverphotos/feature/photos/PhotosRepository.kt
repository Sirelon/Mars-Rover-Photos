package com.sirelon.marsroverphotos.feature.photos

import androidx.annotation.WorkerThread
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 21.02.2021 20:19 for Mars-Rover-Photos.
 */
class PhotosRepository(private val api: RestApi) {

    @WorkerThread
    suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> {
        return api.getRoversPhotos(query)
    }

}