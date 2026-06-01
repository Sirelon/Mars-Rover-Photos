package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.paging.RoverFeedPager
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.ImageOperationResult
import com.sirelon.marsroverphotos.platform.ImageOperations
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the fullscreen image viewer.
 *
 * Delivers the pager content as a single [PagingData] stream regardless of source:
 *  - [AppDestination.ImagesSource.ROVER_FEED] shares the list's [RoverFeedPager] stream, so the
 *    pager can swipe infinitely in both directions (loading adjacent days) in sync with the list.
 *  - All other sources wrap their finite list as a single static page via [PagingData.from].
 *
 * Created on 22.08.2020 18:59 for Mars-Rover-Photos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImageViewModel(
    private val imagesRepository: ImagesRepository,
    private val roversRepository: RoversRepository,
    private val imageOperations: ImageOperations,
    private val roverFeedPager: RoverFeedPager,
) : ViewModel() {

    private val idsEmitter = MutableStateFlow<List<String>>(emptyList())
    private val sourceEmitter = MutableStateFlow(AppDestination.ImagesSource.DIRECT_IDS)
    private val uiEventEmitter = Channel<UiEvent>(capacity = Channel.BUFFERED)
    private val hideUiEmitter = MutableStateFlow(false)

    /** Flow of UI events (photo saved, errors, etc.). */
    val uiEvent: Flow<UiEvent> = uiEventEmitter.receiveAsFlow()

    /** Whether to hide UI chrome (toolbar, controls). */
    val hideUi: StateFlow<Boolean> = hideUiEmitter

    /** Whether to track user interactions (can be disabled for specific contexts). */
    var shouldTrack = true

    /** Paged images for the pager — see class doc for per-source behavior. */
    val pagedImages: Flow<PagingData<MarsImage>> =
        combine(idsEmitter, sourceEmitter) { ids, source -> ids to source }
            .flatMapLatest { (ids, source) ->
                when (source) {
                    AppDestination.ImagesSource.ROVER_FEED -> roverFeedPager.pagedFlow

                    AppDestination.ImagesSource.DIRECT_IDS ->
                        if (ids.isEmpty()) {
                            flowOf(PagingData.empty())
                        } else {
                            imagesRepository.loadImages(ids).map { PagingData.from(it) }
                        }

                    AppDestination.ImagesSource.FAVORITES ->
                        imagesRepository.loadFavoriteImages().map { PagingData.from(it) }

                    AppDestination.ImagesSource.POPULAR ->
                        imagesRepository.loadPopularImages().map { PagingData.from(it) }
                }
            }

    /** Set the source + ids to display. */
    fun setImageSource(
        source: AppDestination.ImagesSource,
        ids: List<String>,
        selectedId: String?,
        roverId: Long?,
        camera: String?,
    ) {
        sourceEmitter.value = source
        idsEmitter.value = ids
        if (source == AppDestination.ImagesSource.ROVER_FEED) {
            restoreRoverFeedIfNeeded(roverId = roverId, selectedId = selectedId, camera = camera)
        }
    }

    private fun restoreRoverFeedIfNeeded(roverId: Long?, selectedId: String?, camera: String?) {
        if (roverId == null) return

        val current = roverFeedPager.currentParams
        if (current?.roverId == roverId && current.camera == camera) return

        viewModelScope.launch {
            val rover = roversRepository.loadRoverById(roverId) ?: return@launch
            val selectedSol = selectedId
                ?.let { imagesRepository.loadImages(listOf(it)).first().firstOrNull()?.sol }
            val anchorSol = selectedSol ?: (rover.maxSol - 1).coerceAtLeast(0L)
            val maxSol = rover.maxSol.coerceAtLeast(1L)
            roverFeedPager.setFeed(
                roverId = rover.id,
                anchorSol = anchorSol.coerceIn(0L, maxSol),
                minSol = 0L,
                maxSol = maxSol,
                camera = camera,
            )
        }
    }

    /**
     * Set favorite to an explicit [favorite] state (the UI tracks the desired value, so this
     * stays correct across repeated taps even though the paged item isn't reactively updated).
     * [ImagesRepository.setFavorite] ensures the row exists first for not-yet-cached feed photos.
     */
    fun setFavorite(image: MarsImage, favorite: Boolean) {
        viewModelScope.launch {
            try {
                imagesRepository.setFavorite(image, favorite)
            } catch (e: Exception) {
                Logger.e("ImageViewModel", e) { "Error setting favorite for image ${image.id}" }
            }
        }
    }

    /** Save an image to device storage. */
    fun saveImage(photo: MarsImage) {
        viewModelScope.launch {
            when (val result = imageOperations.saveImage(photo)) {
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

    /** Share an image via platform share mechanisms. */
    fun shareMarsImage(marsImage: MarsImage) {
        viewModelScope.launch {
            when (val result = imageOperations.shareImage(marsImage)) {
                is ImageOperationResult.Success ->
                    Logger.d("ImageViewModel") { "Image shared successfully" }
                is ImageOperationResult.Error -> {
                    Logger.e("ImageViewModel", null) { "Error sharing image: ${result.error}" }
                    uiEventEmitter.send(UiEvent.ShareError(result.error))
                }
            }
        }
    }

    /** Handle image being shown (for analytics). */
    fun onShown(marsPhoto: MarsImage, page: Int) {
        Logger.d("ImageViewModel") { "Photo shown: ${marsPhoto.id} at page $page" }
        // Remember the photo currently on screen so the rover-feed list can restore its scroll
        // position to it when the viewer is closed. Only meaningful for the shared feed.
        if (sourceEmitter.value == AppDestination.ImagesSource.ROVER_FEED) {
            roverFeedPager.setLastViewedPhotoId(marsPhoto.id)
        }
        // Analytics tracking would go here if needed
    }

    /** Toggle UI visibility (toolbar, controls). */
    fun onTap() {
        hideUiEmitter.value = !hideUiEmitter.value
    }
}

/**
 * Sealed class for UI events.
 */
sealed class UiEvent {
    /** Photo was saved successfully. */
    data class PhotoSaved(val imagePath: String?) : UiEvent()

    /** Error occurred while saving photo. */
    data class PhotoSaveError(val errorMessage: String) : UiEvent()

    /** Error occurred while sharing photo. */
    data class ShareError(val errorMessage: String) : UiEvent()
}
