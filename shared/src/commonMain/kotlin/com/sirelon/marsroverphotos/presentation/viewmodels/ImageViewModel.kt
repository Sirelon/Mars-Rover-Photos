package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.platform.ImageOperationResult
import com.sirelon.marsroverphotos.platform.ImageOperations
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the fullscreen image viewer.
 * Handles image viewing, zooming, saving, and sharing.
 *
 * Created on 22.08.2020 18:59 for Mars-Rover-Photos.
 */
class ImageViewModel(
    private val imagesRepository: ImagesRepository,
    private val imageOperations: ImageOperations
) : ViewModel() {

    private val idsEmitter = MutableStateFlow<List<String>>(emptyList())
    private val uiEventEmitter = Channel<UiEvent>(capacity = Channel.BUFFERED)
    private val hideUiEmitter = MutableStateFlow(false)

    /**
     * Flow of UI events (photo saved, errors, etc.).
     */
    val uiEvent: Flow<UiEvent> = uiEventEmitter.receiveAsFlow()

    /**
     * Whether to track user interactions (can be disabled for specific contexts).
     */
    var shouldTrack = true

    private val imagesFlow: Flow<ImmutableList<MarsImage>> = idsEmitter
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                imagesRepository.loadImages(ids)
            }
        }
        .flatMapLatest { images ->
            if (images.isEmpty() && idsEmitter.value.isNotEmpty()) {
                // Retry once if database is empty but we have IDs
                Logger.e("ImageViewModel", null) { "Images flow returned empty, retrying" }
                delay(500)
                imagesRepository.loadImages(idsEmitter.value)
            } else {
                flowOf(images)
            }
        }
        .mapLatest { it.toImmutableList() }

    /**
     * Screen state combining images and UI visibility.
     */
    val screenState = combine(imagesFlow, hideUiEmitter, ::ImageScreenState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ImageScreenState()
        )

    /**
     * Set the list of image IDs to display.
     * @param ids List of image IDs
     */
    fun setIdsToShow(ids: List<String>) {
        idsEmitter.value = ids
    }

    /**
     * Toggle favorite status for an image.
     * @param image The image to update
     */
    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            try {
                imagesRepository.updateFavForImage(image)
                Logger.d("ImageViewModel") {
                    "Updated favorite for image ${image.id}: ${!image.favorite}"
                }
            } catch (e: Exception) {
                Logger.e("ImageViewModel", e) {
                    "Error updating favorite for image ${image.id}"
                }
            }
        }
    }

    /**
     * Save an image to device storage.
     * @param photo The image to save
     */
    fun saveImage(photo: MarsImage) {
        viewModelScope.launch {
            val result = imageOperations.saveImage(photo)
            when (result) {
                is ImageOperationResult.Success -> {
                    Logger.d("ImageViewModel") { "Image saved: ${result.message}" }
                    uiEventEmitter.send(UiEvent.PhotoSaved(result.message))
                }
                is ImageOperationResult.Error -> {
                    Logger.e("ImageViewModel", null) { "Error saving image: ${result.error}" }
                    uiEventEmitter.send(UiEvent.PhotoSaveError(result.error))
                }
            }
        }
    }

    /**
     * Share an image via platform share mechanisms.
     * @param marsImage The image to share
     */
    fun shareMarsImage(marsImage: MarsImage) {
        viewModelScope.launch {
            val result = imageOperations.shareImage(marsImage)
            when (result) {
                is ImageOperationResult.Success -> {
                    Logger.d("ImageViewModel") { "Image shared successfully" }
                }
                is ImageOperationResult.Error -> {
                    Logger.e("ImageViewModel", null) { "Error sharing image: ${result.error}" }
                    uiEventEmitter.send(UiEvent.ShareError(result.error))
                }
            }
        }
    }

    /**
     * Handle image being shown (for analytics).
     * @param marsPhoto The photo being shown
     * @param page The page/index of the photo
     */
    fun onShown(marsPhoto: MarsImage, page: Int) {
        Logger.d("ImageViewModel") { "Photo shown: ${marsPhoto.id} at page $page" }
        // Analytics tracking would go here if needed
    }

    /**
     * Toggle UI visibility (toolbar, controls).
     */
    fun onTap() {
        hideUiEmitter.value = !hideUiEmitter.value
    }
}

/**
 * Sealed class for UI events.
 */
sealed class UiEvent {
    /**
     * Photo was saved successfully.
     * @param imagePath Optional path where the image was saved
     */
    data class PhotoSaved(val imagePath: String?) : UiEvent()

    /**
     * Error occurred while saving photo.
     * @param errorMessage Description of the error
     */
    data class PhotoSaveError(val errorMessage: String) : UiEvent()

    /**
     * Error occurred while sharing photo.
     * @param errorMessage Description of the error
     */
    data class ShareError(val errorMessage: String) : UiEvent()
}

/**
 * State for the image viewer screen.
 *
 * @param images List of images to display
 * @param hideUi Whether to hide UI chrome (toolbar, controls)
 */
data class ImageScreenState(
    val images: ImmutableList<MarsImage> = persistentListOf(),
    val hideUi: Boolean = false,
)
