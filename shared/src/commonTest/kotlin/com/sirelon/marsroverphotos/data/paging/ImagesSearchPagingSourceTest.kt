package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImagesSearchPagingSourceTest {

    /** 450 hits at pageSize 100 → 5 pages; page N carries items "pN_1".."pN_100" (page 5: 50). */
    private val totalHits = 450

    private fun pageResult(page: Int, totalHits: Int = this.totalHits): ImagesSearchPagingSource.PageResult {
        val count = (totalHits - (page - 1) * 100).coerceIn(0, 100)
        return ImagesSearchPagingSource.PageResult(
            images = (1..count).map { marsImage("p${page}_$it", 0L).copy(order = (page - 1) * 100 + it - 1) },
            totalHits = totalHits,
        )
    }

    /**
     * Source over the 5-page dataset; [fetchedPages] records every network page request.
     * pageSize is pinned to 100 — the fixtures encode 100-per-page math, and the paging logic
     * under test is independent of the production PAGE_SIZE value.
     */
    private fun source(
        anchorPage: Int? = null,
        totalHitsHint: Int? = null,
        random: Random = Random(0),
        dao: FakeImagesDao = FakeImagesDao(),
        onTotalHits: ((Int) -> Unit)? = null,
        fetchedPages: MutableList<Int> = mutableListOf(),
        fetch: suspend (page: Int) -> ImagesSearchPagingSource.PageResult = { pageResult(it) },
    ): ImagesSearchPagingSource = ImagesSearchPagingSource(
        imagesDao = dao,
        anchorPage = anchorPage,
        totalHitsHint = totalHitsHint,
        onTotalHits = onTotalHits,
        random = random,
        pageSize = 100,
        fetchPage = { page ->
            fetchedPages += page
            fetch(page)
        },
    )

    private fun page(result: PagingSource.LoadResult<Int, MarsImage>) =
        assertIs<PagingSource.LoadResult.Page<Int, MarsImage>>(result)

    private fun refresh(key: Int? = null) = PagingSource.LoadParams.Refresh(key, 100, false)

    // 1. Refresh with an explicit anchor fetches exactly that page, with real directional keys.
    @Test
    fun refresh_explicitAnchorFetchesThatPage() = runTest {
        val fetched = mutableListOf<Int>()
        val p = page(source(anchorPage = 2, fetchedPages = fetched).load(refresh()))

        assertEquals(listOf(2), fetched)
        assertEquals(100, p.data.size)
        assertEquals("p2_1", p.data.first().id)
        assertEquals(1, p.prevKey)
        assertEquals(3, p.nextKey)
    }

    // 2. A refresh key (Paging re-centering after invalidation) takes priority over the anchor.
    @Test
    fun refresh_keyTakesPriorityOverAnchor() = runTest {
        val fetched = mutableListOf<Int>()
        page(source(anchorPage = 2, fetchedPages = fetched).load(refresh(key = 4)))
        assertEquals(listOf(4), fetched)
    }

    // 3. Random anchor with a cached totalHits hint: a single fetch, straight to the picked page.
    @Test
    fun refresh_randomAnchorWithHint_singleFetch() = runTest {
        val seed = 42L
        val expectedPage = Random(seed).nextInt(1, 6) // same draw the source will make over 5 pages
        val fetched = mutableListOf<Int>()

        val p = page(
            source(totalHitsHint = totalHits, random = Random(seed), fetchedPages = fetched)
                .load(refresh())
        )

        assertEquals(listOf(expectedPage), fetched)
        assertEquals("p${expectedPage}_1", p.data.first().id)
    }

    // 4. Random anchor without a hint bootstraps page 1 to learn totalHits, then fetches the target.
    @Test
    fun refresh_randomAnchorWithoutHint_bootstrapsPage1() = runTest {
        // Pick a seed whose draw over 5 pages lands beyond page 1, so the bootstrap isn't reused.
        val seed = (1L..100L).first { Random(it).nextInt(1, 6) > 1 }
        val expectedPage = Random(seed).nextInt(1, 6)
        val fetched = mutableListOf<Int>()

        val p = page(source(random = Random(seed), fetchedPages = fetched).load(refresh()))

        assertEquals(listOf(1, expectedPage), fetched)
        assertEquals("p${expectedPage}_1", p.data.first().id)
    }

    // 5. Single-page dataset: the bootstrap result is reused — exactly one fetch.
    @Test
    fun refresh_singlePageDataset_reusesBootstrap() = runTest {
        val fetched = mutableListOf<Int>()
        val p = page(
            source(fetchedPages = fetched, fetch = { pageResult(it, totalHits = 80) })
                .load(refresh())
        )

        assertEquals(listOf(1), fetched)
        assertEquals(80, p.data.size)
        assertNull(p.prevKey)
        assertNull(p.nextKey)
    }

    // 6. Append fetches the requested page with both directional keys.
    @Test
    fun append_fetchesPageWithKeys() = runTest {
        val p = page(source().load(PagingSource.LoadParams.Append(3, 100, false)))
        assertEquals("p3_1", p.data.first().id)
        assertEquals(2, p.prevKey)
        assertEquals(4, p.nextKey)
    }

    // 7. Last page: nextKey is null, prepend stays open.
    @Test
    fun lastPage_nextKeyNull() = runTest {
        val p = page(source().load(PagingSource.LoadParams.Append(5, 100, false)))
        assertEquals(50, p.data.size)
        assertEquals(4, p.prevKey)
        assertNull(p.nextKey)
    }

    // 8. First page: prevKey is null, append stays open.
    @Test
    fun firstPage_prevKeyNull() = runTest {
        val p = page(source().load(PagingSource.LoadParams.Prepend(1, 100, false)))
        assertNull(p.prevKey)
        assertEquals(2, p.nextKey)
    }

    // 9. An empty fetch ends pagination in both directions — never an empty page with a live key.
    @Test
    fun emptyFetch_returnsTerminalPage() = runTest {
        val p = page(
            source(anchorPage = 7, fetch = { ImagesSearchPagingSource.PageResult(emptyList(), 0) })
                .load(refresh())
        )
        assertTrue(p.data.isEmpty())
        assertNull(p.prevKey)
        assertNull(p.nextKey)
    }

    // 10. An explicit anchor beyond the dataset is clamped against the totalHits hint.
    @Test
    fun refresh_anchorClampedToHintRange() = runTest {
        val fetched = mutableListOf<Int>()
        page(
            source(anchorPage = 99, totalHitsHint = totalHits, fetchedPages = fetched)
                .load(refresh())
        )
        assertEquals(listOf(5), fetched)
    }

    // 11. Write-through: insertImages called and persisted favorite merged back.
    @Test
    fun load_writesThroughAndMergesPersistedFavorite() = runTest {
        val network = marsImage("favImg", 0L, favorite = 0)
        val persisted = marsImage("favImg", 0L, favorite = 1)
        val dao = FakeImagesDao(persistedById = mapOf("favImg" to persisted))
        val src = source(
            anchorPage = 1,
            dao = dao,
            fetch = { ImagesSearchPagingSource.PageResult(listOf(network), totalHits = 1) },
        )

        val p = page(src.load(refresh()))
        assertEquals(1L, p.data.first().stats.favorite)
        assertTrue(dao.insertedIds.contains("favImg"))
    }

    // 12. Network error propagates as LoadResult.Error.
    @Test
    fun load_networkErrorProducesErrorResult() = runTest {
        val src = source(anchorPage = 1, fetch = { throw RuntimeException("network failure") })
        val result = src.load(refresh())
        assertIs<PagingSource.LoadResult.Error<Int, MarsImage>>(result)
    }

    // 13. onTotalHits reports the dataset size after every fetch.
    @Test
    fun load_reportsTotalHits() = runTest {
        var reported = -1
        page(source(anchorPage = 2, onTotalHits = { reported = it }).load(refresh()))
        assertEquals(totalHits, reported)
    }

    // 14. getRefreshKey maps the anchor item's global order back to its API page.
    @Test
    fun getRefreshKey_mapsAnchorItemOrderToPage() = runTest {
        val src = source()
        val item = marsImage("x", 0L).copy(order = 250) // global index 250 → page 3
        val state = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(data = listOf(item), prevKey = 2, nextKey = 4)
            ),
            anchorPosition = 0,
            config = PagingConfig(pageSize = 100),
            leadingPlaceholderCount = 0,
        )
        assertEquals(3, src.getRefreshKey(state))
    }
}
