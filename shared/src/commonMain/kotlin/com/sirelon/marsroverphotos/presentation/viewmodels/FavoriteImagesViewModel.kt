package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for the favorite images screen.
 * Loads user's favorite images.
 *
 * Created on 25.08.2020 12:13 for Mars-Rover-Photos.
 */
class FavoriteImagesViewModel(
    private val imagesRepository: ImagesRepository,
    private val tracker: Tracker
) : ViewModel() {

    /**
     * Flow of favorite images.
     */
    val favoriteImagesFlow: Flow<List<MarsImage>> = imagesRepository.loadFavoriteImages()

    /**
     * Track an analytics event.
     * @param event Event name to track
     */
    fun track(event: String) {
        tracker.trackClick(event)
    }

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
