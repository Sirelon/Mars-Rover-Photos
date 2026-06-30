package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.data.LastViewedPhotoStore
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PopularPhotosViewModel(
    private val imagesRepository: ImagesRepository,
    private val lastViewedPhotoStore: LastViewedPhotoStore,
) : ViewModel() {

    private val pageSize = 20

    private val _popularImages = MutableStateFlow<List<MarsImage>?>(null)
    val popularImages: StateFlow<List<MarsImage>?> = _popularImages.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    init {
        loadFirstPage()
    }

    fun refresh() {
        _popularImages.value = null
        _hasMore.value = true
        _isLoadingMore.value = false
        loadFirstPage()
    }

    private fun loadFirstPage() {
        viewModelScope.launch {
            try {
                val page = imagesRepository.loadPopularPage(count = pageSize, after = null)
                _popularImages.value = page
                _hasMore.value = page.size >= pageSize
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.e("PopularPhotosViewModel", e) { "Failed to load popular photos" }
                _popularImages.value = emptyList()
                _hasMore.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMore.value) return
        val current = _popularImages.value ?: return
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val page = imagesRepository.loadPopularPage(count = pageSize, after = current.lastOrNull())
                _popularImages.value = (_popularImages.value ?: emptyList()) + page
                _hasMore.value = page.size >= pageSize
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.e("PopularPhotosViewModel", e) { "Failed to load more popular photos" }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun consumeLastViewedPhotoId(): String? = lastViewedPhotoStore.consume()

    fun updateFavorite(image: MarsImage) {
        viewModelScope.launch {
            try {
                imagesRepository.updateFavForImage(image)
                val current = _popularImages.value ?: return@launch
                _popularImages.value = current.map {
                    if (it.id == image.id) it.copy(favorite = !image.favorite) else it
                }
            } catch (e: Exception) {
                Logger.e("PopularPhotosViewModel", e) { "Error updating favorite for ${image.id}" }
            }
        }
    }
}
