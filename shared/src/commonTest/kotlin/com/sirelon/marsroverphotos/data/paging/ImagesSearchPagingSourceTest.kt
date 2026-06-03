package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImagesSearchPagingSourceTest {

    private fun source(
        pages: Map<Int, ImagesSearchPagingSource.PageResult>,
        dao: FakeImagesDao = FakeImagesDao(),
        shuffleSeed: Long? = null,
    ): ImagesSearchPagingSource = ImagesSearchPagingSource(
        pageSize = 100,
        imagesDao = dao,
        shuffleSeed = shuffleSeed,
        fetchPage = { page -> pages[page] ?: ImagesSearchPagingSource.PageResult(emptyList(), 0) },
    )

    private fun page(result: PagingSource.LoadResult<Int, MarsImage>) =
        assertIs<PagingSource.LoadResult.Page<Int, MarsImage>>(result)

    private fun imageWithDate(id: String, date: String): MarsImage =
        marsImage(id, 0L).copy(earthDate = date)

    // 1. Fetch-all: a two-page dataset is loaded in a single load() call.
    @Test
    fun fetchAll_loadsAllPages() = runTest {
        val src = source(
            mapOf(
                1 to ImagesSearchPagingSource.PageResult(
                    images = (1..100).map { marsImage("p1_img$it", 0L) },
                    totalHits = 150,
                ),
                2 to ImagesSearchPagingSource.PageResult(
                    images = (1..50).map { marsImage("p2_img$it", 0L) },
                    totalHits = 150,
                ),
            )
        )

        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertEquals(150, p.data.size)
    }

    // 2. nextKey is always null — single-page result, no incremental paging.
    @Test
    fun load_nextKeyIsAlwaysNull() = runTest {
        val src = source(
            mapOf(1 to ImagesSearchPagingSource.PageResult(
                images = (1..100).map { marsImage("img$it", 0L) },
                totalHits = 200,
            ))
        )
        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertNull(p.nextKey)
    }

    // 3. prevKey is always null.
    @Test
    fun load_prevKeyIsAlwaysNull() = runTest {
        val src = source(mapOf(1 to ImagesSearchPagingSource.PageResult(listOf(marsImage("x", 0L)), 1)))
        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertNull(p.prevKey)
    }

    // 4. Default sort: newest-first by earthDate (ISO-8601 lexicographic descending, blank last).
    @Test
    fun fetchAll_defaultSortIsNewestFirst() = runTest {
        val pages = mapOf(1 to ImagesSearchPagingSource.PageResult(
            images = listOf(
                imageWithDate("old", "2010-01-01T00:00:00Z"),
                imageWithDate("new", "2020-06-15T00:00:00Z"),
                imageWithDate("mid", "2015-03-10T00:00:00Z"),
                imageWithDate("blank", ""),
            ),
            totalHits = 4,
        ))
        val p = page(source(pages).load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertEquals(listOf("new", "mid", "old", "blank"), p.data.map { it.id })
    }

    // 5. Shuffle: same seed → same order (deterministic).
    @Test
    fun fetchAll_shuffleWithSeedIsDeterministic() = runTest {
        val images = (1..20).map { marsImage("img$it", 0L) }
        val pages = mapOf(1 to ImagesSearchPagingSource.PageResult(images, totalHits = 20))
        val seed = 42L

        val p1 = page(source(pages, shuffleSeed = seed).load(PagingSource.LoadParams.Refresh(null, 100, false)))
        val p2 = page(source(pages, shuffleSeed = seed).load(PagingSource.LoadParams.Refresh(null, 100, false)))

        assertEquals(p1.data.map { it.id }, p2.data.map { it.id })
    }

    // 6. Shuffle: different seeds produce different orders (with overwhelming probability for 20 items).
    @Test
    fun fetchAll_differentSeedsProduceDifferentOrders() = runTest {
        val images = (1..20).map { marsImage("img$it", 0L) }
        val pages = mapOf(1 to ImagesSearchPagingSource.PageResult(images, totalHits = 20))

        val p1 = page(source(pages, shuffleSeed = 1L).load(PagingSource.LoadParams.Refresh(null, 100, false)))
        val p2 = page(source(pages, shuffleSeed = 999L).load(PagingSource.LoadParams.Refresh(null, 100, false)))

        assertTrue(p1.data.map { it.id } != p2.data.map { it.id })
    }

    // 7. Shuffle does not change total item count.
    @Test
    fun fetchAll_shufflePreservesItemCount() = runTest {
        val images = (1..50).map { marsImage("img$it", 0L) }
        val pages = mapOf(1 to ImagesSearchPagingSource.PageResult(images, totalHits = 50))
        val p = page(source(pages, shuffleSeed = 7L).load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertEquals(50, p.data.size)
    }

    // 8. Short last page ends the fetch-all loop before totalHits is reached.
    @Test
    fun fetchAll_shortLastPageStopsLooping() = runTest {
        val src = source(
            mapOf(
                1 to ImagesSearchPagingSource.PageResult(
                    images = (1..100).map { marsImage("p1_$it", 0L) },
                    totalHits = 130,
                ),
                2 to ImagesSearchPagingSource.PageResult(
                    images = (1..30).map { marsImage("p2_$it", 0L) },
                    totalHits = 130,
                ),
            )
        )
        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertEquals(130, p.data.size)
        assertNull(p.nextKey)
    }

    // 9. Empty first page → empty result with no keys.
    @Test
    fun fetchAll_emptyResultReturnsEmptyPage() = runTest {
        val src = source(mapOf(1 to ImagesSearchPagingSource.PageResult(emptyList(), 0)))
        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertTrue(p.data.isEmpty())
        assertNull(p.nextKey)
        assertNull(p.prevKey)
    }

    // 10. Write-through: insertImages called and persisted favorite merged back.
    @Test
    fun load_writesThroughAndMergesPersistedFavorite() = runTest {
        val network = marsImage("favImg", 0L, favorite = 0)
        val persisted = marsImage("favImg", 0L, favorite = 1)
        val dao = FakeImagesDao(persistedById = mapOf("favImg" to persisted))
        val src = ImagesSearchPagingSource(
            pageSize = 100,
            imagesDao = dao,
            fetchPage = { ImagesSearchPagingSource.PageResult(listOf(network), totalHits = 1) },
        )

        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertEquals(1, p.data.size)
        assertEquals(1L, p.data.first().stats.favorite)
        assertTrue(dao.insertedIds.contains("favImg"))
    }

    // 11. Network error propagates as LoadResult.Error.
    @Test
    fun load_networkErrorProducesErrorResult() = runTest {
        val src = ImagesSearchPagingSource(
            pageSize = 100,
            imagesDao = FakeImagesDao(),
            fetchPage = { throw RuntimeException("network failure") },
        )
        val result = src.load(PagingSource.LoadParams.Refresh(null, 100, false))
        assertIs<PagingSource.LoadResult.Error<Int, MarsImage>>(result)
    }

    // 12. Safety cap: loop stops after 50 pages when the API never signals end-of-data.
    //     Uses pageSize=2 for efficiency: 50 pages × 2 items = 100 items total.
    @Test
    fun fetchAll_stopsAtMaxPagesCap() = runTest {
        val src = ImagesSearchPagingSource(
            pageSize = 2,
            imagesDao = FakeImagesDao(),
            fetchPage = { page ->
                ImagesSearchPagingSource.PageResult(
                    images = listOf(marsImage("pg${page}_a", 0L), marsImage("pg${page}_b", 0L)),
                    totalHits = 99999,
                )
            },
        )
        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 2, false)))
        assertEquals(100, p.data.size)   // 50 pages × 2 items/page
        assertNull(p.nextKey)
    }

    // 13. Cache hit: when cachedImages is provided, fetchPage is never called.
    @Test
    fun load_cacheHit_skipsNetwork() = runTest {
        var fetchCallCount = 0
        val cached = (1..5).map { marsImage("cached$it", 0L) }
        val src = ImagesSearchPagingSource(
            pageSize = 100,
            imagesDao = FakeImagesDao(),
            cachedImages = cached,
            fetchPage = { _ ->
                fetchCallCount++
                ImagesSearchPagingSource.PageResult(emptyList(), 0)
            },
        )
        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertEquals(0, fetchCallCount)
        assertEquals(cached.size, p.data.size)
    }

    // 14. onAllImagesFetched is invoked with merged images after a network fetch.
    @Test
    fun load_networkFetch_callsOnAllImagesFetched() = runTest {
        val images = (1..3).map { marsImage("n$it", 0L) }
        var captured: List<MarsImage>? = null
        val src = ImagesSearchPagingSource(
            pageSize = 100,
            imagesDao = FakeImagesDao(),
            onAllImagesFetched = { captured = it },
            fetchPage = { ImagesSearchPagingSource.PageResult(images, totalHits = 3) },
        )
        src.load(PagingSource.LoadParams.Refresh(null, 100, false))
        assertEquals(3, captured?.size)
    }

    // 15. Cache hit: onAllImagesFetched is NOT called (no network fetch happened).
    @Test
    fun load_cacheHit_doesNotCallOnAllImagesFetched() = runTest {
        var callCount = 0
        val cached = listOf(marsImage("c1", 0L))
        val src = ImagesSearchPagingSource(
            pageSize = 100,
            imagesDao = FakeImagesDao(),
            cachedImages = cached,
            onAllImagesFetched = { callCount++ },
            fetchPage = { ImagesSearchPagingSource.PageResult(emptyList(), 0) },
        )
        src.load(PagingSource.LoadParams.Refresh(null, 100, false))
        assertEquals(0, callCount)
    }

    // 16. Cache + shuffle: different seeds applied to the same cached list produce different orders.
    @Test
    fun load_cacheHit_differentSeedsDifferentOrders() = runTest {
        val cached = (1..20).map { marsImage("c$it", 0L) }
        val p1 = page(
            ImagesSearchPagingSource(
                pageSize = 100,
                imagesDao = FakeImagesDao(),
                cachedImages = cached,
                shuffleSeed = 1L,
                fetchPage = { ImagesSearchPagingSource.PageResult(emptyList(), 0) },
            ).load(PagingSource.LoadParams.Refresh(null, 100, false))
        )
        val p2 = page(
            ImagesSearchPagingSource(
                pageSize = 100,
                imagesDao = FakeImagesDao(),
                cachedImages = cached,
                shuffleSeed = 999L,
                fetchPage = { ImagesSearchPagingSource.PageResult(emptyList(), 0) },
            ).load(PagingSource.LoadParams.Refresh(null, 100, false))
        )
        assertTrue(p1.data.map { it.id } != p2.data.map { it.id })
    }
}
