package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for the popular photos screen.
 * Loads popular photos via Room + [PopularRemoteMediator] (Firebase remote source).
 * Room invalidates the PagingSource automatically when the mediator refreshes data.
 *
 * Created on 31.08.2020 21:59 for Mars-Rover-Photos.
 */
class PopularPhotosViewModel(
    private val imagesRepository: ImagesRepository,
) : ViewModel() {

    /**
     * Paged flow of popular images, cached in [viewModelScope].
     * The [PopularRemoteMediator] fetches from Firebase and writes to Room on first load
     * and on subsequent page requests.
     */
    val popularPagedFlow: Flow<PagingData<MarsImage>> = imagesRepository
        .loadPopularPagedSource()
        .cachedIn(viewModelScope)

    /**
     * Toggle favorite status for an image.
     * Room invalidates the PagingSource automatically after the DB write.
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
