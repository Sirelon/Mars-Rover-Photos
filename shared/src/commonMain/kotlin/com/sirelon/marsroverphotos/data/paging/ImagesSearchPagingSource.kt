package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger

/**
 * [PagingSource] for Spirit/Opportunity: pages through images.nasa.gov search results.
 *
 * Keyed by 1-based page number. Loads forward only (prevKey is always null). Pagination
 * ends when a page returns fewer items than [pageSize] or when cumulative items cover
 * [PageResult.totalHits]. Write-through cache and favorite/stats merge mirror [SolPagingSource].
 *
 * @param fetchPage Async function that fetches one page. Receives the 1-based page number
 *                  and returns a [PageResult]. Decoupled from the network layer for testability.
 */
class ImagesSearchPagingSource(
    private val pageSize: Int = PAGE_SIZE,
    private val imagesDao: ImagesDao,
    private val fetchPage: suspend (page: Int) -> PageResult,
) : PagingSource<Int, MarsImage>() {

    data class PageResult(
        val images: List<MarsImage>,
        val totalHits: Int,
    )

    private var loadsSinceEviction = 0

    override fun getRefreshKey(state: PagingState<Int, MarsImage>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MarsImage> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val result = fetchPage(page)
            val merged = cacheAndMerge(result.images, startIndex = (page - 1) * pageSize)
            val loadedSoFar = (page - 1) * pageSize + merged.size
            val nextKey = when {
                merged.isEmpty() -> null
                loadedSoFar >= result.totalHits -> null
                merged.size < pageSize -> null
                else -> page + 1
            }
            LoadResult.Page(data = merged, prevKey = null, nextKey = nextKey)
        } catch (e: Exception) {
            Logger.e("ImagesSearchPagingSource", e) { "Error loading page $page" }
            LoadResult.Error(e)
        }
    }

    private suspend fun cacheAndMerge(networkPhotos: List<MarsImage>, startIndex: Int): List<MarsImage> {
        return try {
            imagesDao.insertImages(networkPhotos)
            if (++loadsSinceEviction >= EVICT_EVERY_N_LOADS) {
                loadsSinceEviction = 0
                try {
                    imagesDao.deleteNonUserImagesBeyondCount(CACHE_KEEP_LIMIT)
                } catch (e: Exception) {
                    Logger.e("ImagesSearchPagingSource", e) { "Cache eviction failed; continuing" }
                }
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

    private companion object {
        private const val FIRST_PAGE = 1
        private const val PAGE_SIZE = 100
        private const val CACHE_KEEP_LIMIT = 2000
        private const val EVICT_EVERY_N_LOADS = 20
    }
}
