package com.sirelon.marsroverphotos.domain.repositories

import androidx.paging.PagingData
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing local Mars images cache and favorites.
 * Created on 22.08.2020 17:48 for Mars-Rover-Photos.
 */
interface ImagesRepository {
    /**
     * Save images to local database.
     * @param photos List of images to save
     */
    suspend fun saveImages(photos: List<MarsImage>)

    /**
     * Load specific images by their IDs.
     * @param ids List of image IDs
     * @return Flow of list of images
     */
    fun loadImages(ids: List<String>): Flow<List<MarsImage>>

    /**
     * Load favorite images with paging support.
     * @return Flow of paged favorite images
     */
    fun loadFavoritePagedSource(): Flow<PagingData<MarsImage>>

    /**
     * Toggle favorite status for an image.
     * @param item Image to update
     */
    suspend fun updateFavForImage(item: MarsImage)

    /**
     * Load popular images with paging support.
     * @return Flow of paged popular images
     */
    fun loadPopularPagedSource(): Flow<PagingData<MarsImage>>
}
