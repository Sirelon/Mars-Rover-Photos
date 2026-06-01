package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map as pagingMap
import com.sirelon.marsroverphotos.data.paging.RoverFeedPager
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
 * ViewModel for the photos screen — now an **infinite bidirectional feed** paged by sol.
 *
 * The feed itself lives in the shared [RoverFeedPager] (so the fullscreen detail pager can
 * collect the same loaded pages). This ViewModel:
 *  - anchors the feed (random sol on first open, or a user-chosen sol/date),
 *  - layers date-section headers and per-day educational facts onto the grid stream,
 *  - tracks the top-visible sol for the floating header and the Sol/Earth pickers.
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

    /** Top-visible sol — drives the floating header chip and the Sol/Earth pickers. */
    private val visibleSolEmitter = MutableStateFlow<Long?>(null)

    private var dateUtil: RoverDateUtil? = null

    /**
     * Small in-memory pool of facts for the feed (separators can't fetch lazily/idempotently).
     * Exposed as a [StateFlow] so the grid transform re-runs once the async preload finishes —
     * otherwise the first (cached) feed emission sees an empty pool and inserts no facts.
     */
    private val factPoolFlow = MutableStateFlow<List<EducationalFact>>(emptyList())

    private val roverFlow: Flow<Rover> = roverIdEmitter
        .filterNotNull()
        .mapNotNull { roversRepository.loadRoverById(it) }
        .onEach { dateUtil = RoverDateUtil(it) }
        .catch { e -> Logger.e("PhotosViewModel", e) { "Error loading rover" } }

    /**
     * Paged grid stream: the shared feed photos with date-section headers inserted at sol
     * boundaries and an educational fact inserted after some day headers (when enabled).
     */
    val gridItemsFlow: Flow<PagingData<GridItem>> = combine(
        roverFeedPager.pagedFlow,
        appSettings.showFactsFlow,
        factPoolFlow,
    ) { pagingData, factsEnabled, factPool ->
        Triple(pagingData, factsEnabled, factPool)
    }.map { (pagingData, factsEnabled, factPool) ->
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
    }.cachedIn(viewModelScope)

    /** Current sol — reflects the top-visible day. Consumed by [SolPickerScreen]. */
    val solFlow: Flow<Long> = visibleSolEmitter.filterNotNull()

    /**
     * Single-observable UI state for the screen chrome (title, floating chip, picker bounds).
     * Grid items come from [gridItemsFlow] via `collectAsLazyPagingItems`, not from here.
     */
    val uiState: StateFlow<PhotosUiState> = combine(
        roverFlow.map { it.name to it.maxSol.coerceAtLeast(1L) },
        visibleSolEmitter.filterNotNull(),
        cameraFilterEmitter,
        appSettings.showCameraNameFlow,
    ) { roverInfo, sol, cameraFilters, showCameraName ->
        val (roverName, maxSol) = roverInfo
        PhotosUiState(
            roverName = roverName,
            sol = sol,
            earthDate = earthDateStr(sol),
            maxSol = maxSol,
            cameraFilters = cameraFilters,
            datePickerSelectedMillis = earthTime(sol),
            datePickerMinMillis = minDate(),
            datePickerMaxMillis = maxDate(),
            showCameraName = showCameraName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PhotosUiState(),
    )

    init {
        // Pre-load a small fact pool once (best-effort).
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

        // Anchor the feed. If the shared pager is already anchored on this rover (e.g. we are
        // returning from the detail viewer), keep its scroll/paging state instead of resetting.
        viewModelScope.launch {
            val rover = roverFlow.first()
            if (dateUtil == null) dateUtil = RoverDateUtil(rover)
            val existing = roverFeedPager.currentParams
            if (existing?.roverId == rover.id) {
                if (visibleSolEmitter.value == null) visibleSolEmitter.value = existing.anchorSol
            } else {
                randomize()
            }
        }
    }

    /** Set the rover to display photos for. */
    fun setRoverId(roverId: Long) {
        roverIdEmitter.tryEmit(roverId)
    }

    /**
     * Apply a set of camera filters (empty = show all). Rebuilds the feed source synchronously
     * using the current pager params — no DB roundtrip needed.
     */
    fun setCameraFilters(cameras: Set<String>) {
        if (cameraFilterEmitter.value == cameras) return
        cameraFilterEmitter.value = cameras
        val params = roverFeedPager.currentParams
        if (params != null) {
            val anchor = (visibleSolEmitter.value ?: params.anchorSol)
                .coerceIn(params.minSol, params.maxSol)
            roverFeedPager.setFeed(
                roverId = params.roverId,
                anchorSol = anchor,
                minSol = params.minSol,
                maxSol = params.maxSol,
                cameras = cameras,
            )
        } else {
            viewModelScope.launch {
                val rover = roverFlow.first()
                val anchor = visibleSolEmitter.value ?: return@launch
                applyAnchor(rover, anchor)
            }
        }
    }

    /** Update the top-visible sol as the grid scrolls (drives the floating header + pickers). */
    fun onVisibleSolChanged(sol: Long) {
        visibleSolEmitter.value = sol
    }

    /**
     * Id of the photo last viewed in the fullscreen detail pager (or null), cleared on read. The
     * grid uses it to scroll back to that photo when the viewer is closed. See [RoverFeedPager].
     */
    fun consumeLastViewedPhotoId(): String? = roverFeedPager.consumeLastViewedPhotoId()

    /** Anchor the feed on a random sol for the current rover. */
    fun randomize() {
        viewModelScope.launch {
            val rover = roverFlow.first()
            val sol = Random.nextLong(0L, rover.maxSol.coerceAtLeast(1L))
            applyAnchor(rover, sol)
        }
    }

    /** Anchor the feed on the latest sol; re-randomize if already there (matches old FAB behavior). */
    fun goToLatest() {
        viewModelScope.launch {
            val rover = roverFlow.first()
            val target = (rover.maxSol - 1).coerceAtLeast(0L)
            if (roverFeedPager.currentParams?.anchorSol == target) {
                randomize()
            } else {
                applyAnchor(rover, target)
            }
        }
    }

    /** Anchor the feed on a specific sol. */
    fun loadBySol(sol: Long) {
        viewModelScope.launch {
            val rover = roverFlow.first()
            applyAnchor(rover, sol)
        }
    }

    /** Convert Earth time (millis) to a sol and anchor there. */
    fun setEarthTime(time: Long) {
        val sol = dateUtil?.solFromDate(time) ?: run {
            Logger.w("PhotosViewModel") { "DateUtil not initialized, defaulting to sol 1" }
            1L
        }
        loadBySol(sol)
    }

    // ── Date helpers (used by the Sol/Earth pickers) ──────────────────────────

    /** Earth-date string ("Jan 15, 2021") for the given sol. */
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun applyAnchor(rover: Rover, sol: Long) {
        val maxSol = rover.maxSol.coerceAtLeast(1L)
        val clamped = sol.coerceIn(0L, maxSol)
        visibleSolEmitter.value = clamped
        roverFeedPager.setFeed(
            roverId = rover.id,
            anchorSol = clamped,
            minSol = 0L,
            maxSol = maxSol,
            cameras = cameraFilterEmitter.value,
        )
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
 * UI state for the photos-screen chrome (title, floating date chip, picker bounds).
 * Grid items are delivered separately as paged `LazyPagingItems`.
 */
data class PhotosUiState(
    val roverName: String = "",
    /** Top-visible sol. */
    val sol: Long = 0L,
    /** Earth-date string for [sol] (e.g. "Jan 15, 2021"). */
    val earthDate: String = "",
    val maxSol: Long = 1L,
    val cameraFilters: Set<String> = emptySet(),
    /** Earth-date millis for [sol] — the date-picker's initial selection. */
    val datePickerSelectedMillis: Long = 0L,
    /** Rover landing-date millis — lower bound for the date picker. */
    val datePickerMinMillis: Long = 0L,
    /** Rover last-date millis — upper bound for the date picker. */
    val datePickerMaxMillis: Long = 0L,
    /** Whether to show the camera name label on photo cards. */
    val showCameraName: Boolean = true,
)
