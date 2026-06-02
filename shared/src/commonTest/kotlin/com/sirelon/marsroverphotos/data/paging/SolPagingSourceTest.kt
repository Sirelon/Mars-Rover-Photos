package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolPagingSourceTest {

    private fun source(
        solToPhotos: Map<Long, List<MarsImage>>,
        roverId: Long = CURIOSITY_ID,
        cameras: Set<String> = emptySet(),
        initialSol: Long = 0,
        minSol: Long = 0,
        maxSol: Long = 100,
        dao: FakeImagesDao = FakeImagesDao(),
        repo: FakePhotosRepository = FakePhotosRepository(solToPhotos),
    ): SolPagingSource = SolPagingSource(
        photosRepository = repo,
        imagesDao = dao,
        roverId = roverId,
        cameras = cameras,
        initialSol = initialSol,
        minSol = minSol,
        maxSol = maxSol,
    )

    private fun page(result: PagingSource.LoadResult<Long, MarsImage>) =
        assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(result)

    // 1. Append finds the immediate non-empty sol.
    @Test
    fun append_findsImmediateNonEmptySol() = runTest {
        val src = source(mapOf(10L to listOf(marsImage("a", 10))), minSol = 0, maxSol = 100)

        val result = src.load(PagingSource.LoadParams.Append(10L, 5, false))

        val p = page(result)
        assertEquals(listOf("a"), p.data.map { it.id })
        assertEquals(9L, p.prevKey)
        assertEquals(11L, p.nextKey)
    }

    // 2. Append skips empty sols within budget to the next non-empty sol.
    @Test
    fun append_skipsEmptySolsWithinBudget() = runTest {
        // photos only at sol 13; sols 10..12 empty
        val src = source(mapOf(13L to listOf(marsImage("b", 13))), minSol = 0, maxSol = 100)

        val result = src.load(PagingSource.LoadParams.Append(10L, 5, false))

        val p = page(result)
        assertEquals(listOf("b"), p.data.map { it.id })
        assertEquals(12L, p.prevKey)
        assertEquals(14L, p.nextKey)
    }

    // 3. Key clamping at bounds.
    @Test
    fun foundPage_clampsKeysAtBounds() = runTest {
        // Found at sol == minSol -> prevKey null
        val atMin = source(mapOf(0L to listOf(marsImage("c", 0))), minSol = 0, maxSol = 100)
        val pMin = page(atMin.load(PagingSource.LoadParams.Append(0L, 5, false)))
        assertNull(pMin.prevKey)
        assertEquals(1L, pMin.nextKey)

        // Found at sol == maxSol -> nextKey null
        val atMax = source(mapOf(100L to listOf(marsImage("d", 100))), minSol = 0, maxSol = 100)
        val pMax = page(atMax.load(PagingSource.LoadParams.Append(100L, 5, false)))
        assertEquals(99L, pMax.prevKey)
        assertNull(pMax.nextKey)
    }

    // 4. Terminal page at the bound: nothing until maxSol -> empty page, both keys null.
    @Test
    fun append_terminalPageAtBound() = runTest {
        // Empty map, small bounded range so the scan reaches maxSol within budget.
        val src = source(emptyMap(), minSol = 0, maxSol = 50)

        val result = src.load(PagingSource.LoadParams.Append(40L, 5, false))

        val p = page(result)
        assertTrue(p.data.isEmpty())
        assertNull(p.prevKey)
        assertNull(p.nextKey)
    }

    // 5. Bounded scan: empty map, huge range -> continuation page after the 300-probe budget.
    @Test
    fun append_boundedScanReturnsContinuationPage() = runTest {
        val repo = FakePhotosRepository(emptyMap())
        val src = source(emptyMap(), minSol = 0, maxSol = 10_000, initialSol = 0, repo = repo)

        val result = src.load(PagingSource.LoadParams.Append(0L, 5, false))

        val p = page(result)
        assertTrue(p.data.isEmpty())
        assertNull(p.prevKey)
        // budget is 300 unfiltered: probes sols 0..299, returns Exhausted(300) -> nextKey 300
        assertEquals(300L, p.nextKey)
        assertEquals(300, repo.probedSols.size)
    }

    // 5b. Regression (Codex P2): a camera-filtered load must ALSO stay bounded. Before the fix the
    // filtered branch looped on every Exhausted segment, so a far-off/absent camera match could
    // walk the whole rover range (thousands of probes) in one synchronous load. It now caps the
    // cumulative scan at FILTER_LOAD_BUDGET (300 sols) and yields a continuation page.
    @Test
    fun append_filteredFeedBoundsScanPerLoad() = runTest {
        val repo = FakePhotosRepository(emptyMap())
        val src = source(
            emptyMap(),
            cameras = setOf("FHAZ"),
            minSol = 0,
            maxSol = 10_000,
            initialSol = 0,
            repo = repo,
        )

        val result = src.load(PagingSource.LoadParams.Append(0L, 5, false))

        val p = page(result)
        assertTrue(p.data.isEmpty())
        assertNull(p.prevKey)
        assertEquals(300L, p.nextKey)
        // Bounded to ~FILTER_LOAD_BUDGET sols, NOT the full 10_000-sol range.
        assertTrue(repo.probedSols.size <= 300, "filtered load must stay bounded, was ${repo.probedSols.size}")
    }

    // 6a. Refresh with photos near start returns a non-empty page.
    @Test
    fun refresh_withPhotosReturnsNonEmptyPage() = runTest {
        val src = source(mapOf(3L to listOf(marsImage("e", 3))), initialSol = 0, minSol = 0, maxSol = 100)

        val result = src.load(PagingSource.LoadParams.Refresh(null, 5, false))

        val p = page(result)
        assertEquals(listOf("e"), p.data.map { it.id })
    }

    // 6b. Refresh over an empty bounded range returns the terminal page (never a continuation).
    @Test
    fun refresh_emptyRangeReturnsTerminalPage() = runTest {
        val src = source(emptyMap(), initialSol = 5, minSol = 0, maxSol = 10)

        val result = src.load(PagingSource.LoadParams.Refresh(null, 5, false))

        val p = page(result)
        assertTrue(p.data.isEmpty())
        assertNull(p.prevKey)
        assertNull(p.nextKey)
    }

    // 7. Write-through merge: persisted favorite is merged back; insertImages got the network photos.
    @Test
    fun load_mergesPersistedStatsAndInsertsNetworkPhotos() = runTest {
        val network = marsImage("f", 20, favorite = 0)
        val persisted = marsImage("f", 20, favorite = 1)
        val dao = FakeImagesDao(persistedById = mapOf("f" to persisted))
        val src = source(
            solToPhotos = mapOf(20L to listOf(network)),
            minSol = 0,
            maxSol = 100,
            dao = dao,
        )

        val result = src.load(PagingSource.LoadParams.Append(20L, 5, false))

        val p = page(result)
        assertEquals(1, p.data.size)
        assertEquals(1L, p.data.first().stats.favorite)
        assertEquals(listOf("f"), dao.insertedIds)
    }

    // Optional: filtered feed using a real Curiosity camera name.
    @Test
    fun append_filteredFeedReturnsOnlyMatchingCamera() = runTest {
        // sol 10 has only NAVCAM (no match), sol 11 has FHAZ (match)
        val src = source(
            solToPhotos = mapOf(
                10L to listOf(marsImage("nav", 10, cameraName = "NAVCAM")),
                11L to listOf(
                    marsImage("fhaz", 11, cameraName = "FHAZ"),
                    marsImage("nav2", 11, cameraName = "NAVCAM"),
                ),
            ),
            roverId = CURIOSITY_ID,
            cameras = setOf("FHAZ"),
            minSol = 0,
            maxSol = 100,
        )

        val result = src.load(PagingSource.LoadParams.Append(10L, 5, false))

        val p = page(result)
        assertEquals(listOf("fhaz"), p.data.map { it.id })
        assertEquals(10L, p.prevKey)
        assertEquals(12L, p.nextKey)
    }
}
