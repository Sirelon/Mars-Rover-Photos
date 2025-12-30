package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.repositories.FactsRepository
import com.sirelon.marsroverphotos.domain.repositories.ImagesRepository
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.presentation.models.GridItem
import com.sirelon.marsroverphotos.presentation.models.GridItemTransformer
import com.sirelon.marsroverphotos.utils.Logger
import com.sirelon.marsroverphotos.utils.RoverDateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel for the photos screen.
 * Manages photo browsing by sol, camera filtering, and educational facts integration.
 *
 * Created on 21.02.2021 20:25 for Mars-Rover-Photos.
 */
class PhotosViewModel(
    private val roversRepository: RoversRepository,
    private val photosRepository: PhotosRepository,
    private val imagesRepository: ImagesRepository,
    private val factsRepository: FactsRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val queryEmitter = MutableStateFlow<PhotosQueryRequest?>(null)
    private val roverIdEmitter = MutableStateFlow<Long?>(null)
    private var dateUtil: RoverDateUtil? = null

    private val roverFlow = roverIdEmitter
        .filterNotNull()
        .mapNotNull { roversRepository.loadRoverById(it) }
        .onEach { dateUtil = RoverDateUtil(it) }
        .catch { e ->
            Logger.e("PhotosViewModel", e) { "Error loading rover" }
        }

    /**
     * Flow of photos loaded from the API.
     * Null indicates loading state.
     */
    private val photosFlowInternal = queryEmitter
        .map { query ->
            if (query != null) {
                try {
                    photosRepository.refreshImages(query)
                } catch (e: Exception) {
                    Logger.e("PhotosViewModel", e) { "Error loading photos" }
                    null
                }
            } else {
                null
            }
        }
        .catch { e ->
            Logger.e("PhotosViewModel", e) { "Error in photos flow" }
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * Public flow of photos.
     */
    val photosFlow = photosFlowInternal

    /**
     * Grid items flow that contains both photos and educational facts.
     * Facts are inserted every 10 photos if the showFacts preference is enabled.
     */
    val gridItemsFlow = combine(
        photosFlowInternal,
        appSettings.showFactsFlow
    ) { photos, factsEnabled ->
        photos to factsEnabled
    }
        .mapLatest { (photos, factsEnabled) ->
            if (photos == null) {
                null
            } else if (!factsEnabled || photos.isEmpty()) {
                // No facts, just convert photos to grid items
                photos.map { GridItem.PhotoItem(it) }
            } else {
                // Calculate how many facts we need
                val requiredFactCount = GridItemTransformer.calculateRequiredFacts(photos.size)

                // Fetch required facts
                val facts = buildList {
                    repeat(requiredFactCount) {
                        val fact = factsRepository.getNextFact()
                        if (fact != null) {
                            factsRepository.markFactAsShown(fact)
                            add(fact)
                        }
                    }
                }

                Logger.d("PhotosViewModel") {
                    "Creating grid items with ${photos.size} photos and ${facts.size} facts"
                }

                // Transform to mixed grid items
                GridItemTransformer.createGridItems(photos, facts, factsEnabled)
            }
        }
        .catch { e ->
            Logger.e("PhotosViewModel", e) { "Error creating grid items" }
            emit(emptyList())
        }

    /**
     * Flow of current sol number.
     */
    val solFlow = queryEmitter.mapNotNull { it?.sol }

    init {
        viewModelScope.launch {
            roverFlow.first()
            randomize()
        }
    }

    /**
     * Set the rover to display photos for.
     * @param roverId The rover ID
     */
    fun setRoverId(roverId: Long) {
        roverIdEmitter.tryEmit(roverId)
    }

    /**
     * Load a random sol for the current rover.
     */
    fun randomize() {
        viewModelScope.launch {
            val query = randomPhotosQueryRequest()
            setPhotosQuery(query)
        }
    }

    /**
     * Navigate to the latest sol for the current rover.
     */
    fun goToLatest() {
        viewModelScope.launch {
            val rover = roverFlow.first()
            val maxSol = rover.maxSol - 1
            val query = if (queryEmitter.value?.sol == maxSol) {
                randomPhotosQueryRequest()
            } else {
                PhotosQueryRequest(rover.id, maxSol, null)
            }

            setPhotosQuery(query)
        }
    }

    /**
     * Load photos for a specific sol.
     * @param sol Mars sol number
     */
    fun loadBySol(sol: Long) {
        val queryRequest = queryEmitter.value ?: return
        if (sol == queryRequest.sol) return
        queryEmitter.tryEmit(null)

        val queryUpdated = queryRequest.copy(sol = sol)
        setPhotosQuery(queryUpdated)
    }

    /**
     * Convert Earth time to Earth date string.
     * @param sol Mars sol number
     * @return Earth date string in yyyy-MM-dd format
     */
    fun earthDateStr(sol: Long): String {
        val time = earthTime(sol)
        return dateUtil?.parseTime(time) ?: ""
    }

    /**
     * Convert sol to Earth time in milliseconds.
     * @param sol Mars sol number (defaults to current sol)
     * @return Earth time in milliseconds
     */
    fun earthTime(sol: Long = getSol()) = dateUtil?.dateFromSol(sol) ?: System.currentTimeMillis()

    /**
     * Set the current sol from Earth time.
     * @param time Earth time in milliseconds
     */
    fun setEarthTime(time: Long) {
        val sol = dateUtil?.solFromDate(time) ?: 1
        loadBySol(sol)
    }

    /**
     * Get the maximum date for the current rover.
     * @return Maximum date in milliseconds
     */
    fun maxDate() = dateUtil?.roverLastDate ?: System.currentTimeMillis()

    /**
     * Get the maximum sol for the current rover.
     * @return Maximum sol number
     */
    suspend fun maxSol() = roverFlow.first().maxSol

    /**
     * Get the minimum date (landing date) for the current rover.
     * @return Landing date in milliseconds
     */
    fun minDate() = dateUtil?.roverLandingDate ?: System.currentTimeMillis()

    /**
     * Get Earth date from current sol.
     * @return Earth date in milliseconds
     */
    fun dateFromSol() = dateUtil?.dateFromSol(getSol()) ?: System.currentTimeMillis()

    /**
     * Handle photo click - save photos to local database for caching.
     */
    fun onPhotoClick() {
        viewModelScope.launch {
            try {
                val photos = photosFlow.filterNotNull().filterNot { it.isEmpty() }.first()
                Logger.d("PhotosViewModel") { "Saving ${photos.size} photos to cache" }
                imagesRepository.saveImages(photos)
            } catch (e: Exception) {
                Logger.e("PhotosViewModel", e) { "Error saving photos" }
            }
        }
    }

    // Private helper methods

    private fun setPhotosQuery(query: PhotosQueryRequest) {
        Logger.d("PhotosViewModel") { "Loading photos: $query" }
        queryEmitter.tryEmit(null)
        viewModelScope.launch {
            queryEmitter.emit(query)
        }
    }

    private fun getSol() = queryEmitter.value?.sol ?: 0

    private suspend fun randomPhotosQueryRequest(): PhotosQueryRequest {
        val rover = roverFlow.first()
        val sol = Random.nextLong(0L, rover.maxSol)
        return PhotosQueryRequest(rover.id, sol, null)
    }
}
