package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pager-level integration tests via [TestPager]. The [SolPagingSource] design relies on Paging 3
 * re-enqueuing APPEND/PREPEND after an empty page with a non-null key (the "continuation page"
 * mechanism for gaps longer than one scan budget) — these tests prove the key chain actually
 * carries a real pager across such a gap, and that a terminal page ends pagination.
 */
class SolPagingSourcePagerTest {

    // Mirrors the production PagingConfig (RoverPhotosRepository / photos feed).
    private val config = PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false)

    private fun source(
        solToPhotos: Map<Long, List<MarsImage>>,
        maxSol: Long,
        initialSol: Long = 0,
    ): SolPagingSource = SolPagingSource(
        photosRepository = FakePhotosRepository(solToPhotos),
        imagesDao = FakeImagesDao(),
        roverId = CURIOSITY_ID,
        cameras = emptySet(),
        initialSol = initialSol,
        minSol = 0,
        maxSol = maxSol,
    )

    private fun page(result: PagingSource.LoadResult<Long, MarsImage>?) =
        assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(result)

    // 1. A gap LONGER than one unfiltered scan budget (300 sols): photos at sol 0 and sol 350.
    // The pager must step through the continuation page(s) and reach sol 350 without stalling.
    @Test
    fun append_stepsThroughContinuationPagesAcrossLongGap() = runTest {
        val pager = TestPager(
            config,
            source(
                solToPhotos = mapOf(
                    0L to listOf(marsImage("start", 0)),
                    350L to listOf(marsImage("far", 350)),
                ),
                maxSol = 1000,
            ),
        )

        // Refresh lands on sol 0.
        val refreshed = page(pager.refresh())
        assertEquals(listOf("start"), refreshed.data.map { it.id })

        // Append until photos arrive; the loop bound proves we don't stall or spin.
        var appends = 0
        var found: PagingSource.LoadResult.Page<Long, MarsImage>? = null
        while (found == null && appends < 5) {
            val p = page(pager.append())
            appends++
            if (p.data.isNotEmpty()) found = p
        }
        // One continuation page (budget hit at sol 301) + the page that finds sol 350.
        assertEquals(2, appends)
        assertEquals(listOf("far"), found?.data?.map { it.id })

        // Keep appending: continuation pages walk the rest of the range (651, 951) until the
        // maxSol bound yields a terminal page, after which TestPager stops appending entirely.
        var last: PagingSource.LoadResult.Page<Long, MarsImage>? = null
        var tailAppends = 0
        while (tailAppends < 10) {
            val r = pager.append() ?: break
            last = page(r)
            tailAppends++
        }
        assertEquals(3, tailAppends)
        assertTrue(last!!.data.isEmpty())
        assertNull(last.nextKey)
    }

    // 2. Terminal page ends pagination: nothing after sol 0 within bounds -> empty page with a
    // null nextKey, and TestPager returns null for any further append.
    @Test
    fun append_terminalPageEndsPagination() = runTest {
        val pager = TestPager(
            config,
            source(solToPhotos = mapOf(0L to listOf(marsImage("only", 0))), maxSol = 50),
        )

        val refreshed = page(pager.refresh())
        assertEquals(listOf("only"), refreshed.data.map { it.id })

        // Scan 1..50 reaches the bound within budget -> terminal page.
        val terminal = page(pager.append())
        assertTrue(terminal.data.isEmpty())
        assertNull(terminal.nextKey)

        // A null nextKey ends pagination: TestPager refuses further appends.
        assertNull(pager.append())
    }
}
