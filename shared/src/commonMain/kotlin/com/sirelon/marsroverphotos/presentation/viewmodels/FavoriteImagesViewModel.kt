package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sirelon.marsroverphotos.data.LastViewedPhotoStore
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.FavoriteSortOrder
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoriteImagesViewModel(
    private val imagesRepository: ImagesRepository,
    private val roversRepository: RoversRepository,
    private val tracker: Tracker,
    private val lastViewedPhotoStore: LastViewedPhotoStore,
) : ViewModel() {

    data class RoverChip(val roverId: Long?, val name: String, val count: Int)
    data class FavoriteStats(val savedCount: Int, val roverCount: Int, val cameraCount: Int)

    val sortOrder: MutableStateFlow<FavoriteSortOrder> = MutableStateFlow(FavoriteSortOrder.Recent)
    val roverFilter: MutableStateFlow<Long?> = MutableStateFlow(null)

    // Drives the grid — paged, switches paging source when sort or rover filter changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    val favoritePagedFlow: Flow<PagingData<MarsImage>> = combine(sortOrder, roverFilter) { sort, rover ->
        sort to rover
    }.flatMapLatest { (sort, rover) ->
        imagesRepository.loadFavoritePaged(sort, rover)
    }.cachedIn(viewModelScope)

    // Side flow for stats + rover chips — always the full unfiltered list.
    private val allFavorites: StateFlow<List<MarsImage>> = imagesRepository.loadFavoriteImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val roverChips: StateFlow<List<RoverChip>> = combine(
        allFavorites, roversRepository.getRovers()
    ) { favorites, rovers ->
        val roverMap = rovers.associateBy { it.id }
        val countByRover = favorites.groupingBy { it.roverId }.eachCount()
        buildList {
            add(RoverChip(null, "All", favorites.size))
            countByRover.entries
                .sortedBy { roverMap[it.key]?.name ?: "" }
                .forEach { (id, count) ->
                    val name = roverMap[id]?.name ?: return@forEach
                    add(RoverChip(id, name, count))
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(RoverChip(null, "All", 0)))

    val stats: StateFlow<FavoriteStats> = allFavorites.map { all ->
        FavoriteStats(
            savedCount = all.size,
            roverCount = all.map { it.roverId }.toSet().size,
            cameraCount = all.mapNotNull { it.camera?.name }.toSet().size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoriteStats(0, 0, 0))

    fun consumeLastViewedPhotoId(): String? = lastViewedPhotoStore.consume()

    fun track(event: String) {
        tracker.trackClick(event)
    }

    fun updateFavForImage(image: MarsImage) {
        viewModelScope.launch {
            try {
                imagesRepository.updateFavForImage(item = image)
            } catch (e: Exception) {
                Logger.e("FavoriteImagesViewModel", e) {
                    "Error updating favorite for image ${image.id}"
                }
            }
        }
    }
}
