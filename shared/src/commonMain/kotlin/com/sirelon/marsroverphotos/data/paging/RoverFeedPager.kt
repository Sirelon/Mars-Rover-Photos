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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

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
        /**
         * Monotonic token making every explicit [setFeed] distinct. Without it, re-anchoring to
         * the *current* sol (e.g. re-picking the same date after scrolling away, or re-randomizing
         * to the same sol) produces an equal [Params] that [MutableStateFlow] conflates away, so
         * the Pager never rebuilds and the feed appears "stuck / not updating".
         */
        val generation: Long,
    ) {
        val solMode: FeedMode.Sol? get() = mode as? FeedMode.Sol
    }

    private val paramsFlow = MutableStateFlow<Params?>(null)
    private var generation = 0L

    // In-memory cache of the full result list from images.nasa.gov, keyed by query string.
    // Populated after the first network fetch for a query; subsequent reshuffles (same query,
    // new seed) reorder this list instantly without any network round-trip.
    private val pageCache = mutableMapOf<String, List<MarsImage>>()

    /** Current feed params, or null until [setFeed] is first called. */
    val currentParams: Params? get() = paramsFlow.value

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
        .filterNotNull()
        .flatMapLatest { p ->
            when (val mode = p.mode) {
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
                        ImagesSearchPagingSource(
                            imagesDao = imagesDao,
                            shuffleSeed = shuffleSeed,
                            cachedImages = pageCache[query],
                            onAllImagesFetched = { images -> pageCache[query] = images },
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
        }
        .cachedIn(appScope)

    fun setFeed(roverId: Long, mode: FeedMode) {
        paramsFlow.value = Params(
            roverId = roverId,
            mode = mode,
            generation = generation++,
        )
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
