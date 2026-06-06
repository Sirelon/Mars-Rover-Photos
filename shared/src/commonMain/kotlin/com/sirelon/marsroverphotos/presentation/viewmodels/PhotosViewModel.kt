package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map as pagingMap
import com.sirelon.marsroverphotos.data.paging.FeedMode
import com.sirelon.marsroverphotos.data.paging.RoverFeedPager
import com.sirelon.marsroverphotos.data.paging.pageQuery
import com.sirelon.marsroverphotos.data.paging.usesPageFeed
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.FactsRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.presentation.models.GridItem
import com.sirelon.marsroverphotos.utils.Logger
import com.sirelon.marsroverphotos.utils.RoverDateUtil
import com.sirelon.marsroverphotos.utils.formatDisplayDate
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel for the photos screen — an infinite bidirectional feed paged by sol (sol mode) or
 * a forward-only page-numbered search feed (page mode for Spirit/Opportunity).
 *
 * The feed lives in the shared [RoverFeedPager]. This ViewModel:
 *  - selects [FeedMode] per rover,
 *  - layers date-section headers and educational facts (sol mode only),
 *  - tracks the top-visible sol for the floating header and pickers (sol mode only).
 *
 * Created on 21.02.2021 20:25 for Mars-Rover-Photos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotosViewModel(
    private val roversRepository: RoversRepository,
    private val factsRepository: FactsRepository,
    private val appSettings: AppSettings,
    private val roverFeedPager: RoverFeedPager,
) : ViewModel() {

    private val roverIdEmitter = MutableStateFlow<Long?>(null)
    private val cameraFilterEmitter = MutableStateFlow<Set<String>>(emptySet())

    /** Top-visible sol — drives the floating header chip and the Sol/Earth pickers (sol mode only). */
    private val visibleSolEmitter = MutableStateFlow<Long?>(null)

    /**
     * Fires every time the feed is re-anchored (date/sol picker, randomize, go-to-latest, camera
     * filter change). The grid collects this to scroll back to item 0 so the user actually sees
     * the newly anchored sol — otherwise the previous scroll offset keeps the old day on screen.
     */
    private val _scrollToTopEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val scrollToTopEvents: SharedFlow<Unit> = _scrollToTopEvents.asSharedFlow()

    private var dateUtil: RoverDateUtil? = null

    private val factPoolFlow = MutableStateFlow<List<EducationalFact>>(emptyList())

    private val roverStateFlow: StateFlow<Rover?> = roverIdEmitter
        .filterNotNull()
        .mapNotNull { roversRepository.loadRoverById(it) }
        .onEach { dateUtil = RoverDateUtil(it) }
        .catch { e -> Logger.e("PhotosViewModel", e) { "Error loading rover" } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val roverFlow: Flow<Rover> = roverStateFlow.filterNotNull()

    /**
     * Paged grid stream. In sol mode, date-section headers and educational facts are interleaved.
     * In page mode (Spirit/Opportunity), only [GridItem.PhotoItem]s are emitted — no sol data.
     */
    val gridItemsFlow: Flow<PagingData<GridItem>> = combine(
        roverFeedPager.pagedFlow,
        roverIdEmitter.filterNotNull().map { it.usesPageFeed() },
        appSettings.showFactsFlow,
        factPoolFlow,
    ) { pagingData, isPageMode, factsEnabled, factPool ->
        if (isPageMode) {
            pagingData.pagingMap { photo -> GridItem.PhotoItem(photo) as GridItem }
        } else {
            pagingData
                .pagingMap { photo -> GridItem.PhotoItem(photo) }
                .insertSeparators<GridItem.PhotoItem, GridItem> { before, after ->
                    if (after != null && before?.image?.sol != after.image.sol) {
                        GridItem.DateHeader(sol = after.image.sol, earthDate = after.image.earthDate)
                    } else {
                        null
                    }
                }
                .insertSeparators<GridItem, GridItem> { before, _ ->
                    if (factsEnabled && before is GridItem.DateHeader) {
                        factForSol(before.sol, factPool)?.let { fact ->
                            GridItem.FactItem(fact = fact, position = before.sol.toInt())
                        }
                    } else {
                        null
                    }
                }
        }
    }.cachedIn(viewModelScope)

    /** Current sol — reflects the top-visible day. Consumed by [SolPickerScreen]. */
    val solFlow: Flow<Long> = visibleSolEmitter.filterNotNull()

    /**
     * Single-observable UI state for the screen chrome.
     * For page-mode rovers (Spirit/Opportunity), [PhotosUiState.showSolControls] is false and
     * sol/date/camera fields are not meaningful — the screen hides those controls.
     */
    val uiState: StateFlow<PhotosUiState> = combine(
        roverStateFlow.filterNotNull(),
        visibleSolEmitter.map { it ?: 0L },
        cameraFilterEmitter,
        appSettings.showCameraNameFlow,
    ) { rover, sol, cameraFilters, showCameraName ->
        if (rover.id.usesPageFeed()) {
            PhotosUiState(roverName = rover.name, showSolControls = false)
        } else {
            PhotosUiState(
                roverName = rover.name,
                sol = sol,
                earthDate = earthDateStr(sol),
                maxSol = rover.maxSol.coerceAtLeast(1L),
                cameraFilters = cameraFilters,
                datePickerSelectedMillis = earthTime(sol),
                datePickerMinMillis = minDate(),
                datePickerMaxMillis = maxDate(),
                showCameraName = showCameraName,
                showSolControls = true,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PhotosUiState(),
    )

    init {
        viewModelScope.launch {
            runCatching {
                factsRepository.loadFacts()
                factPoolFlow.value = buildList {
                    repeat(FACT_POOL_SIZE) {
                        val fact = factsRepository.getNextFact() ?: return@repeat
                        factsRepository.markFactAsShown(fact)
                        add(fact)
                    }
                }
            }.onFailure { e -> Logger.e("PhotosViewModel", e) { "Failed to preload fact pool" } }
        }

        viewModelScope.launch {
            val rover = roverFlow.first()
            if (dateUtil == null) dateUtil = RoverDateUtil(rover)
            if (rover.id.usesPageFeed()) {
                applyPageFeed(rover)
            } else if (rover.id == CURIOSITY_ID) {
                goToLatest()
            } else {
                randomize()
            }
        }
    }

    fun setRoverId(roverId: Long) {
        roverIdEmitter.tryEmit(roverId)
    }

    fun setCameraFilters(cameras: Set<String>) {
        if (roverIdEmitter.value?.usesPageFeed() == true) return
        if (cameraFilterEmitter.value == cameras) return
        cameraFilterEmitter.value = cameras
        val params = roverFeedPager.currentParams
        val solMode = params?.solMode
        if (params != null && solMode != null) {
            val anchor = (visibleSolEmitter.value ?: solMode.anchorSol).coerceIn(solMode.minSol, solMode.maxSol)
            roverFeedPager.setFeed(
                roverId = params.roverId,
                mode = FeedMode.Sol(
                    anchorSol = anchor,
                    minSol = solMode.minSol,
                    maxSol = solMode.maxSol,
                    cameras = cameras,
                ),
            )
            _scrollToTopEvents.tryEmit(Unit)
        } else {
            viewModelScope.launch {
                val rover = roverFlow.first()
                val anchor = visibleSolEmitter.value ?: return@launch
                applyAnchor(rover, anchor)
            }
        }
    }

    /** No-op in page mode (Spirit/Opportunity have no sol data). */
    fun onVisibleSolChanged(sol: Long) {
        if (roverIdEmitter.value?.usesPageFeed() == true) return
        visibleSolEmitter.value = sol
    }

    fun consumeLastViewedPhotoId(): String? = roverFeedPager.consumeLastViewedPhotoId()

    fun randomize() {
        val roverId = roverIdEmitter.value ?: return
        if (roverId.usesPageFeed()) {
            roverFeedPager.setFeed(
                roverId = roverId,
                mode = FeedMode.Page(roverId.pageQuery(), shuffleSeed = Random.nextLong()),
            )
            return
        }
        viewModelScope.launch {
            val rover = roverFlow.first()
            applyAnchor(rover, Random.nextLong(0L, rover.maxSol.coerceAtLeast(1L)))
        }
    }

    fun goToLatest() {
        if (roverIdEmitter.value?.usesPageFeed() == true) return
        viewModelScope.launch {
            val rover = roverFlow.first()
            val target = (rover.maxSol - 1).coerceAtLeast(0L)
            if (roverFeedPager.currentParams?.solMode?.anchorSol == target) randomize()
            else applyAnchor(rover, target)
        }
    }

    /** No-op in page mode. */
    fun loadBySol(sol: Long) {
        if (roverIdEmitter.value?.usesPageFeed() == true) return
        viewModelScope.launch {
            val rover = roverFlow.first()
            applyAnchor(rover, sol)
        }
    }

    /** No-op in page mode. */
    fun setEarthTime(time: Long) {
        if (roverIdEmitter.value?.usesPageFeed() == true) return
        val sol = dateUtil?.solFromDate(time) ?: run {
            Logger.w("PhotosViewModel") { "DateUtil not initialized, defaulting to sol 1" }
            1L
        }
        loadBySol(sol)
    }

    fun earthDateStr(sol: Long): String {
        val time = earthTime(sol)
        val isoDate = dateUtil?.parseTime(time) ?: run {
            Logger.w("PhotosViewModel") { "DateUtil not initialized, returning empty date string" }
            return ""
        }
        return formatDisplayDate(isoDate)
    }

    fun earthTime(sol: Long = getSol()) = dateUtil?.dateFromSol(sol) ?: run {
        Logger.w("PhotosViewModel") { "DateUtil not initialized, returning current time" }
        Clock.System.now().toEpochMilliseconds()
    }

    fun maxDate() = dateUtil?.roverLastDate ?: run {
        Logger.w("PhotosViewModel") { "DateUtil not initialized, returning current time for max date" }
        Clock.System.now().toEpochMilliseconds()
    }

    suspend fun maxSol() = roverFlow.first().maxSol

    fun minDate() = dateUtil?.roverLandingDate ?: run {
        Logger.w("PhotosViewModel") { "DateUtil not initialized, returning current time for min date" }
        Clock.System.now().toEpochMilliseconds()
    }

    fun dateFromSol() = dateUtil?.dateFromSol(getSol()) ?: run {
        Logger.w("PhotosViewModel") { "DateUtil not initialized, returning current time" }
        Clock.System.now().toEpochMilliseconds()
    }

    private fun applyPageFeed(rover: Rover) {
        roverFeedPager.setFeed(
            roverId = rover.id,
            mode = FeedMode.Page(rover.id.pageQuery(), shuffleSeed = Random.nextLong()),
        )
        _scrollToTopEvents.tryEmit(Unit)
    }

    private fun applyAnchor(rover: Rover, sol: Long) {
        val maxSol = rover.maxSol.coerceAtLeast(1L)
        val clamped = sol.coerceIn(0L, maxSol)
        visibleSolEmitter.value = clamped
        roverFeedPager.setFeed(
            roverId = rover.id,
            mode = FeedMode.Sol(
                anchorSol = clamped,
                minSol = 0L,
                maxSol = maxSol,
                cameras = cameraFilterEmitter.value,
            ),
        )
        _scrollToTopEvents.tryEmit(Unit)
    }

    private fun getSol(): Long = visibleSolEmitter.value ?: 0L

    private fun factForSol(sol: Long, pool: List<EducationalFact>): EducationalFact? {
        if (pool.isEmpty() || sol % FACT_EVERY_N_DAYS != 0L) return null
        val raw = ((sol / FACT_EVERY_N_DAYS) % pool.size).toInt()
        val idx = if (raw < 0) raw + pool.size else raw
        return pool[idx]
    }

    private companion object {
        private const val FACT_POOL_SIZE = 15
        private const val FACT_EVERY_N_DAYS = 3L
    }
}

/**
 * UI state for the photos-screen chrome.
 * When [showSolControls] is false (Spirit/Opportunity), sol/date/camera fields are meaningless
 * and those controls are hidden in the UI.
 */
data class PhotosUiState(
    val roverName: String = "",
    val sol: Long = 0L,
    val earthDate: String = "",
    val maxSol: Long = 1L,
    val cameraFilters: Set<String> = emptySet(),
    val datePickerSelectedMillis: Long = 0L,
    val datePickerMinMillis: Long = 0L,
    val datePickerMaxMillis: Long = 0L,
    val showCameraName: Boolean = true,
    /** False for page-mode rovers (Spirit/Opportunity) — hides sol nav, date picker, camera filter. */
    val showSolControls: Boolean = true,
)
