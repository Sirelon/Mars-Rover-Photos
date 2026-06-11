package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger
import kotlin.random.Random

/**
 * Page-keyed [PagingSource] for Spirit/Opportunity, backed by the images.nasa.gov /search API
 * ([PAGE_SIZE] results per page; the API has no server-side sort parameter, so items appear in
 * API order — [MarsImage.order] encodes the global position `(page-1)*PAGE_SIZE + indexInPage`,
 * stamped by the [fetchPage] caller).
 *
 * The key is the 1-based API page number: APPEND walks page+1, PREPEND walks page-1, so a feed
 * anchored mid-dataset grows in both directions — mirroring the sol feed's random-anchor UX.
 *
 * Refresh resolves its anchor in priority order:
 *  1. `params.key` — Paging re-centering after invalidation (see [getRefreshKey]);
 *  2. [anchorPage] — an explicit page jump from the picker;
 *  3. a random page. Picking one needs the total page count: [totalHitsHint] (cached by
 *     [RoverFeedPager] from a previous fetch) avoids any extra request; without it, page 1 is
 *     fetched first just to learn `totalHits` and is reused as the result when the random pick
 *     lands on page 1.
 *
 * An explicit anchor is clamped against [totalHitsHint] when available; without a hint, a stale
 * anchor beyond the dataset simply yields an empty terminal page (the picker bounds its input by
 * the cached total, so this is a cold-start edge case only).
 *
 * Empty fetches always end pagination (both keys null) — an empty page with a live key would
 * make Paging 3 re-trigger the same load in a loop. [onTotalHits] reports the dataset size after
 * every fetch so the caller can cache it and feed the page picker. Write-through cache and
 * favorite/stats merge mirror [SolPagingSource].
 *
 * @param fetchPage Async function that fetches one page. Receives the 1-based page number
 *                  and returns a [PageResult]. Decoupled from the network layer for testability.
 */
class ImagesSearchPagingSource(
    private val imagesDao: ImagesDao,
    private val anchorPage: Int? = null,
    private val totalHitsHint: Int? = null,
    private val onTotalHits: ((Int) -> Unit)? = null,
    private val random: Random = Random.Default,
    private val pageSize: Int = PAGE_SIZE,
    private val fetchPage: suspend (page: Int) -> PageResult,
) : PagingSource<Int, MarsImage>() {

    data class PageResult(
        val images: List<MarsImage>,
        val totalHits: Int,
    )

    /** Refresh re-centers on the page of the item closest to the current scroll anchor. */
    override fun getRefreshKey(state: PagingState<Int, MarsImage>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestItemToPosition(anchor)?.order?.let { it / pageSize + 1 }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MarsImage> {
        return try {
            when (params) {
                is LoadParams.Refresh -> loadRefresh(params.key)
                is LoadParams.Append -> loadPage(params.key)
                is LoadParams.Prepend -> loadPage(params.key)
            }
        } catch (e: Exception) {
            Logger.e("ImagesSearchPagingSource", e) { "Error loading page" }
            LoadResult.Error(e)
        }
    }

    private suspend fun loadRefresh(key: Int?): LoadResult<Int, MarsImage> {
        val requested = key ?: anchorPage
        if (requested != null) {
            val clamped = totalHitsHint
                ?.let { requested.coerceIn(1, totalPages(it)) }
                ?: requested.coerceAtLeast(1)
            return loadPage(clamped)
        }
        // Random anchor: the total page count must be known to pick one. Use the cached hint
        // when available; otherwise bootstrap with page 1 (reused as the result if the pick
        // lands on page 1), so a cold start costs at most one extra request.
        var bootstrap: PageResult? = null
        val totalHits = totalHitsHint ?: fetchPage(1).also { bootstrap = it }.totalHits
        val totalPages = totalPages(totalHits)
        val target = if (totalPages <= 1) 1 else random.nextInt(1, totalPages + 1)
        val result = bootstrap.takeIf { target == 1 } ?: fetchPage(target)
        return result.toLoadPage(target)
    }

    private suspend fun loadPage(page: Int): LoadResult<Int, MarsImage> =
        fetchPage(page).toLoadPage(page)

    private suspend fun PageResult.toLoadPage(page: Int): LoadResult.Page<Int, MarsImage> {
        onTotalHits?.invoke(totalHits)
        val data = cacheAndMerge(images)
        // An empty fetch (stale anchor beyond the dataset, or no results at all) ends
        // pagination in both directions — never hand Paging an empty page with a live key.
        if (data.isEmpty()) return LoadResult.Page(data = data, prevKey = null, nextKey = null)
        return LoadResult.Page(
            data = data,
            prevKey = (page - 1).takeIf { it >= 1 },
            nextKey = (page + 1).takeIf { it <= totalPages(totalHits) },
        )
    }

    private fun totalPages(totalHits: Int): Int =
        if (totalHits <= 0) 1 else (totalHits + pageSize - 1) / pageSize

    private suspend fun cacheAndMerge(networkPhotos: List<MarsImage>): List<MarsImage> {
        if (networkPhotos.isEmpty()) return networkPhotos
        return try {
            imagesDao.insertImages(networkPhotos)
            try {
                imagesDao.deleteNonUserImagesBeyondCount(CACHE_KEEP_LIMIT)
            } catch (e: Exception) {
                Logger.e("ImagesSearchPagingSource", e) { "Cache eviction failed; continuing" }
            }
            val persisted = imagesDao
                .loadImagesByIds(networkPhotos.map { it.id })
                .associateBy { it.id }
            networkPhotos.map { persisted[it.id] ?: it }
        } catch (e: Exception) {
            Logger.e("ImagesSearchPagingSource", e) { "Write-through cache failed; serving network" }
            networkPhotos
        }
    }

    companion object {
        /**
         * Page size requested from images.nasa.gov (the API allows up to 100). Small pages make
         * the page picker and random anchor meaningful on these curated datasets (~434/565
         * photos → ~44/57 pages). Single source of truth — also used by [RoverFeedPager]'s
         * paging config / startIndex math and the viewer's page restore.
         */
        internal const val PAGE_SIZE = 10
        private const val CACHE_KEEP_LIMIT = 2000
    }
}
