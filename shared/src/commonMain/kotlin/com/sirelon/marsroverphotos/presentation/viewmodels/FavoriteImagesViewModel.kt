package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for the favorite images screen.
 * Loads user's favorite images with paging support.
 *
 * Created on 25.08.2020 12:13 for Mars-Rover-Photos.
 */
class FavoriteImagesViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    /**
     * Flow of favorite images with paging support.
     */
    val favoriteImagesFlow: Flow<PagingData<MarsImage>> = imagesRepository.loadFavoritePagedSource()

    /**
     * Toggle favorite status for an image.
     * @param image The image to update
     */
    fun updateFavForImage(image: MarsImage) {
        viewModelScope.launch {
            try {
                imagesRepository.updateFavForImage(item = image)
                Logger.d("FavoriteImagesViewModel") {
                    "Updated favorite for image ${image.id}: ${!image.favorite}"
                }
            } catch (e: Exception) {
                Logger.e("FavoriteImagesViewModel", e) {
                    "Error updating favorite for image ${image.id}"
                }
            }
        }
    }
}
