package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.toFirebasePhoto
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the popular photos screen.
 * Loads popular photos from Firebase and allows toggling favorites.
 *
 * Created on 31.08.2020 21:59 for Mars-Rover-Photos.
 */
class PopularPhotosViewModel(
    private val imagesRepository: ImagesRepository,
    private val firebasePhotos: IFirebasePhotos,
) : ViewModel() {

    private val _popularPhotos = MutableStateFlow<List<MarsImage>>(emptyList())
    val popularPhotos: StateFlow<List<MarsImage>> = _popularPhotos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPopularPhotos()
    }

    fun loadPopularPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val photos = firebasePhotos.loadPopularPhotos(count = 50)
                val mappedPhotos = photos.mapIndexed { index, fp -> fp.toMarsImage(index) }
                imagesRepository.saveImages(mappedPhotos)
                _popularPhotos.value = mappedPhotos
            } catch (e: Exception) {
                Logger.e("PopularPhotosViewModel", e) { "Error loading popular photos" }
                _popularPhotos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle favorite status for an image.
     * @param image The image to update
     */
    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            val updatedFavorite = !image.favorite
            _popularPhotos.value = _popularPhotos.value.map { current ->
                if (current.id == image.id) current.copy(favorite = updatedFavorite) else current
            }
            try {
                imagesRepository.updateFavForImage(image)
                Logger.d("PopularPhotosViewModel") {
                    "Updated favorite for image ${image.id}: $updatedFavorite"
                }
            } catch (e: Exception) {
                _popularPhotos.value = _popularPhotos.value.map { current ->
                    if (current.id == image.id) current.copy(favorite = image.favorite) else current
                }
                Logger.e("PopularPhotosViewModel", e) {
                    "Error updating favorite for image ${image.id}"
                }
            }
        }
    }
}
