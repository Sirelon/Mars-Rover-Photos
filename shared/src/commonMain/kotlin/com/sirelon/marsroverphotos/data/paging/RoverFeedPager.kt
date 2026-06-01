package com.sirelon.marsroverphotos.data.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
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
    appScope: CoroutineScope,
) {

    data class Params(
        val roverId: Long,
        val anchorSol: Long,
        val minSol: Long,
        val maxSol: Long,
        val camera: String?,
        /**
         * Monotonic token making every explicit [setFeed] distinct. Without it, re-anchoring to
         * the *current* sol (e.g. re-picking the same date after scrolling away, or re-randomizing
         * to the same sol) produces an equal [Params] that [MutableStateFlow] conflates away, so
         * the Pager never rebuilds and the feed appears "stuck / not updating".
         */
        val generation: Long,
    )

    private val paramsFlow = MutableStateFlow<Params?>(null)
    private var generation = 0L

    /** Current feed params, or null until [setFeed] is first called. */
    val currentParams: Params? get() = paramsFlow.value

    /**
     * Shared, cached stream of feed photos (photos only — date headers and facts are layered
     * on by the list ViewModel). Collected by both the list grid and the detail pager.
     */
    val pagedFlow: Flow<PagingData<MarsImage>> = paramsFlow
        .filterNotNull()
        .flatMapLatest { p ->
            Pager(
                config = pagingConfig,
                initialKey = p.anchorSol,
                pagingSourceFactory = {
                    SolPagingSource(
                        photosRepository = photosRepository,
                        imagesDao = imagesDao,
                        roverId = p.roverId,
                        camera = p.camera,
                        initialSol = p.anchorSol,
                        minSol = p.minSol,
                        maxSol = p.maxSol,
                    )
                },
            ).flow
        }
        .cachedIn(appScope)

    fun setFeed(roverId: Long, anchorSol: Long, minSol: Long, maxSol: Long, camera: String?) {
        paramsFlow.value = Params(
            roverId = roverId,
            anchorSol = anchorSol,
            minSol = minSol,
            maxSol = maxSol,
            camera = camera,
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
    }
}
