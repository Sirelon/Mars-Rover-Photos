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

    private fun pageResult(
        ids: List<String>,
        totalHits: Int,
        sol: Long = 0L,
    ): ImagesSearchPagingSource.PageResult = ImagesSearchPagingSource.PageResult(
        images = ids.mapIndexed { i, id -> marsImage(id, sol) },
        totalHits = totalHits,
    )

    private fun source(
        pages: Map<Int, ImagesSearchPagingSource.PageResult>,
        dao: FakeImagesDao = FakeImagesDao(),
    ): ImagesSearchPagingSource = ImagesSearchPagingSource(
        pageSize = 100,
        imagesDao = dao,
        fetchPage = { page -> pages[page] ?: ImagesSearchPagingSource.PageResult(emptyList(), 0) },
    )

    private fun page(result: PagingSource.LoadResult<Int, MarsImage>) =
        assertIs<PagingSource.LoadResult.Page<Int, MarsImage>>(result)

    // 1. First load of a full page yields nextKey=2 when totalHits > page_size.
    @Test
    fun firstLoad_returnsItemsAndNextKey() = runTest {
        val ids = (1..100).map { "img$it" }
        val src = source(
            mapOf(1 to pageResult(ids, totalHits = 200))
        )

        val result = src.load(PagingSource.LoadParams.Refresh(null, 100, false))

        val p = page(result)
        assertEquals(100, p.data.size)
        assertNull(p.prevKey)
        assertEquals(2, p.nextKey)
    }

    // 2. Append to next page returns correct nextKey.
    @Test
    fun append_returnsNextKeyWhileItemsRemain() = runTest {
        val src = source(
            mapOf(2 to pageResult((1..100).map { "img$it" }, totalHits = 300))
        )

        val result = src.load(PagingSource.LoadParams.Append(2, 100, false))

        val p = page(result)
        assertEquals(100, p.data.size)
        assertEquals(3, p.nextKey)
    }

    // 3. End-of-pagination: loadedSoFar >= totalHits → nextKey null.
    @Test
    fun append_endOfPaginationWhenTotalHitsReached() = runTest {
        // 2 pages of 50 items, total=100 — after page 2 we've loaded 100 >= 100.
        val src = source(
            mapOf(2 to pageResult((1..50).map { "img2_$it" }, totalHits = 100))
        )

        val result = src.load(PagingSource.LoadParams.Append(2, 100, false))

        val p = page(result)
        assertEquals(50, p.data.size)
        assertNull(p.nextKey)
    }

    // 4. End-of-pagination: empty page → nextKey null.
    @Test
    fun append_endOfPaginationOnEmptyPage() = runTest {
        val src = source(
            mapOf(3 to pageResult(emptyList(), totalHits = 250))
        )

        val result = src.load(PagingSource.LoadParams.Append(3, 100, false))

        val p = page(result)
        assertTrue(p.data.isEmpty())
        assertNull(p.nextKey)
    }

    // 5. Short page (< pageSize) ends pagination even if total not reached.
    @Test
    fun append_shortPageEndsPageination() = runTest {
        val src = source(
            mapOf(1 to pageResult((1..30).map { "img$it" }, totalHits = 999))
        )

        val result = src.load(PagingSource.LoadParams.Refresh(null, 100, false))

        val p = page(result)
        assertEquals(30, p.data.size)
        assertNull(p.nextKey)
    }

    // 6. prevKey is always null (forward-only pagination).
    @Test
    fun load_prevKeyIsAlwaysNull() = runTest {
        val src = source(
            mapOf(1 to pageResult(listOf("x"), totalHits = 500))
        )

        val p = page(src.load(PagingSource.LoadParams.Refresh(null, 100, false)))
        assertNull(p.prevKey)
    }

    // 7. Write-through: insertImages called and persisted favorite merged back.
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

    // 8. Network error propagates as LoadResult.Error.
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
}
