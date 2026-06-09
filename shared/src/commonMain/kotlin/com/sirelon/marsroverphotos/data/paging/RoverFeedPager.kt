package com.sirelon.marsroverphotos.data.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.network.toMarsImages
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

/**
 * App-singleton holder for the rover photo feed's paged stream.
 *
 * Builds a [SolPagingSource]-backed [Pager] and caches it in [appScope] so that BOTH the
 * photos list (PhotosViewModel) and the fullscreen detail pager (ImageViewModel) collect the
 * SAME loaded pages — they stay in sync, and paging/scroll state survives list→detail→back.
 *
 * Call [setFeed] to (re)anchor the feed on a sol or to change rover/camera; the cached flow
 * rebuilds and resets to the new anchor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoverFeedPager(
    private val photosRepository: PhotosRepository,
    private val imagesDao: ImagesDao,
    private val restApi: RestApi,
    appScope: CoroutineScope,
) {

    data class Params(
        val roverId: Long,
        val mode: FeedMode,
    ) {
        val solMode: FeedMode.Sol? get() = mode as? FeedMode.Sol
    }

    /**
     * Event-style stream of feed requests. Deliberately a [MutableSharedFlow], NOT a StateFlow: a
     * SharedFlow does NOT conflate equal values, so re-anchoring to the *current* sol (re-picking
     * the same date after scrolling away, or re-randomizing to the same sol) still re-emits and
     * rebuilds the Pager — a StateFlow would drop the duplicate and the feed would appear "stuck".
     * `replay = 1` lets late collectors (the detail pager) pick up the active feed.
     */
    private val paramsFlow = MutableSharedFlow<Params>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Latest params, for synchronous reads by callers. Mutated only from [setFeed] (main thread). */
    private var latestParams: Params? = null

    // In-memory cache of the full result list from images.nasa.gov, keyed by query string.
    // Populated after the first network fetch for a query; subsequent reshuffles (same query,
    // new seed) reorder this list instantly without any network round-trip.
    private val pageCache = mutableMapOf<String, List<MarsImage>>()

    /** Current feed params, or null until [setFeed] is first called. */
    val currentParams: Params? get() = latestParams

    private val _totalPagePhotos = MutableStateFlow(0)

    /** Total photos loaded for page-mode rovers (Spirit/Opportunity). 0 until the first fetch completes. */
    val totalPagePhotosFlow: StateFlow<Int> = _totalPagePhotos.asStateFlow()

    private val _currentRoverId = MutableStateFlow<Long?>(null)

    /**
     * Rover id of the most recently EMITTED page — updated as data flows out of [pagedFlow], not
     * when [setFeed] is called. The list ViewModel gates its grid on this: while this app-singleton
     * pager still holds the *previous* rover's cached pages (the [cachedIn] replay), the newly
     * opened rover's screen shows a loading placeholder instead of briefly rendering the previous
     * rover's photos. Tracking the emitted data (rather than the requested params) keeps the gate in
     * lockstep with what the list actually receives, so the placeholder stays up for the whole feed
     * rebuild instead of flashing the previous rover while the new Pager spins up.
     */
    val currentRoverId: StateFlow<Long?> = _currentRoverId.asStateFlow()

    /**
     * Id of the photo most recently shown in the fullscreen detail pager. The list reads (and
     * clears) this via [consumeLastViewedPhotoId] when the viewer is closed so it can restore its
     * scroll position to the photo the user actually swiped to — not the one they originally
     * tapped. Only the rover-feed source sets it (other sources don't share this list's grid).
     */
    private val lastViewedPhotoId = MutableStateFlow<String?>(null)

    /**
     * Optimistic favorite overrides — written by both the list and detail VMs so the UI stays
     * consistent across screens without waiting for the paging source to re-fetch from Room.
     */
    val favoriteOverrides = mutableStateMapOf<String, Boolean>()

    fun setLastViewedPhotoId(id: String) {
        lastViewedPhotoId.value = id
    }

    /** Return the last-viewed photo id and clear it, so it's applied at most once per viewer visit. */
    fun consumeLastViewedPhotoId(): String? =
        lastViewedPhotoId.value.also { lastViewedPhotoId.value = null }

    /**
     * Shared, cached stream of feed photos (photos only — date headers and facts are layered
     * on by the list ViewModel). Collected by both the list grid and the detail pager.
     */
    val pagedFlow: Flow<PagingData<MarsImage>> = paramsFlow
        .flatMapLatest { p ->
            val pages = when (val mode = p.mode) {
                is FeedMode.Sol -> Pager(
                    config = pagingConfig,
                    initialKey = mode.anchorSol,
                    pagingSourceFactory = {
                        SolPagingSource(
                            photosRepository = photosRepository,
                            imagesDao = imagesDao,
                            roverId = p.roverId,
                            cameras = mode.cameras,
                            initialSol = mode.anchorSol,
                            minSol = mode.minSol,
                            maxSol = mode.maxSol,
                        )
                    },
                ).flow
                is FeedMode.Page -> Pager(
                    config = pageSearchConfig,
                    initialKey = 1,
                    pagingSourceFactory = {
                        val query = mode.query
                        val shuffleSeed = mode.shuffleSeed
                        val cachedImages = pageCache[query]
                        _totalPagePhotos.value = cachedImages?.size ?: 0
                        ImagesSearchPagingSource(
                            imagesDao = imagesDao,
                            shuffleSeed = shuffleSeed,
                            cachedImages = cachedImages,
                            onAllImagesFetched = { images ->
                                pageCache[query] = images
                                _totalPagePhotos.value = images.size
                            },
                            fetchPage = { page ->
                                val response = restApi.searchImages(query, page)
                                val startIndex = (page - 1) * 100
                                ImagesSearchPagingSource.PageResult(
                                    images = response.toMarsImages(startIndex),
                                    totalHits = response.collection.metadata?.totalHits ?: 0,
                                )
                            },
                        )
                    },
                ).flow
            }
            // Tag the rover as each page is emitted (not when params change), so [currentRoverId]
            // stays in lockstep with the data the list actually receives.
            pages.onEach { _currentRoverId.value = p.roverId }
        }
        .cachedIn(appScope)

    fun setFeed(roverId: Long, mode: FeedMode) {
        val params = Params(roverId = roverId, mode = mode)
        latestParams = params
        paramsFlow.tryEmit(params)
    }

    private companion object {
        private val pagingConfig = PagingConfig(
            pageSize = 20,
            prefetchDistance = 10,
            initialLoadSize = 20,
            enablePlaceholders = false,
        )
        private val pageSearchConfig = PagingConfig(
            pageSize = 100,
            prefetchDistance = 20,
            initialLoadSize = 100,
            enablePlaceholders = false,
        )
    }
}
