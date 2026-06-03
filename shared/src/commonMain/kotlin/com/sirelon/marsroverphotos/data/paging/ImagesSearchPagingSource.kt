package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.Logger
import kotlin.random.Random

/**
 * [PagingSource] for Spirit/Opportunity: loads ALL images.nasa.gov search results in a single
 * [load] call (datasets are tiny: Spirit ~258, Opportunity ~523 images; 3–6 pages of 100).
 *
 * The images.nasa.gov /search API has no server-side sort parameter; ordering is client-side:
 *   - Default ([shuffleSeed] == null): newest-first by earthDate (ISO-8601 string, lexicographic desc).
 *   - Shuffled ([shuffleSeed] != null): deterministic random order using the given seed, so the
 *     list and detail pager stay consistent across the same shuffle invocation.
 *
 * Returns a single [LoadResult.Page] with prevKey = null and nextKey = null. Write-through cache
 * and favorite/stats merge mirror [SolPagingSource].
 *
 * @param fetchPage Async function that fetches one page. Receives the 1-based page number
 *                  and returns a [PageResult]. Decoupled from the network layer for testability.
 * @param shuffleSeed If non-null, shuffle the collected images using this seed instead of date sort.
 * @param cachedImages If non-null, skip the network fetch entirely and apply sort/shuffle to this
 *                     pre-loaded list. Set by [RoverFeedPager] after the first fetch so reshuffles
 *                     are instant.
 * @param onAllImagesFetched Called with the merged image list after a real network fetch completes.
 *                           Not called when [cachedImages] is used. Use to populate the caller's cache.
 */
class ImagesSearchPagingSource(
    private val pageSize: Int = PAGE_SIZE,
    private val imagesDao: ImagesDao,
    private val shuffleSeed: Long? = null,
    private val cachedImages: List<MarsImage>? = null,
    private val onAllImagesFetched: ((List<MarsImage>) -> Unit)? = null,
    private val fetchPage: suspend (page: Int) -> PageResult,
) : PagingSource<Int, MarsImage>() {

    data class PageResult(
        val images: List<MarsImage>,
        val totalHits: Int,
    )

    override fun getRefreshKey(state: PagingState<Int, MarsImage>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MarsImage> {
        return try {
            val merged = if (cachedImages != null) {
                cachedImages
            } else {
                val allImages = mutableListOf<MarsImage>()
                var page = 1
                var totalHits = 0

                while (true) {
                    val result = fetchPage(page)
                    if (result.totalHits > 0) totalHits = result.totalHits
                    allImages += result.images
                    if (result.images.isEmpty() || result.images.size < pageSize || allImages.size >= totalHits) break
                    if (page >= MAX_PAGES) {
                        Logger.w("ImagesSearchPagingSource") { "Safety cap reached after $page pages; truncating at ${allImages.size} images" }
                        break
                    }
                    page++
                }

                cacheAndMerge(allImages).also { onAllImagesFetched?.invoke(it) }
            }
            val ordered = if (shuffleSeed != null) {
                merged.shuffled(Random(shuffleSeed))
            } else {
                merged.sortedByDescending { it.earthDate }
            }
            LoadResult.Page(data = ordered, prevKey = null, nextKey = null)
        } catch (e: Exception) {
            Logger.e("ImagesSearchPagingSource", e) { "Error loading all pages" }
            LoadResult.Error(e)
        }
    }

    private suspend fun cacheAndMerge(networkPhotos: List<MarsImage>): List<MarsImage> {
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

    private companion object {
        private const val PAGE_SIZE = 100
        private const val CACHE_KEEP_LIMIT = 2000
        private const val MAX_PAGES = 50
    }
}
