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
 * ViewModel for the popular photos screen.
 * Loads popular photos from Firebase and allows toggling favorites.
 *
 * Created on 31.08.2020 21:59 for Mars-Rover-Photos.
 */
class PopularPhotosViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    /**
     * Flow of popular photos with paging support.
     */
    val popularPhotos: Flow<PagingData<MarsImage>> = imagesRepository.loadPopularPagedSource()

    /**
     * Toggle favorite status for an image.
     * @param image The image to update
     */
    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            try {
                imagesRepository.updateFavForImage(image)
                Logger.d("PopularPhotosViewModel") {
                    "Updated favorite for image ${image.id}: ${!image.favorite}"
                }
            } catch (e: Exception) {
                Logger.e("PopularPhotosViewModel", e) {
                    "Error updating favorite for image ${image.id}"
                }
            }
        }
    }
}
