package com.sirelon.marsroverphotos.data.viking

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.paging.FakeImagesDao
import com.sirelon.marsroverphotos.data.paging.SolPagingSource
import com.sirelon.marsroverphotos.data.repositories.PhotosRepositoryImpl
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Mimics PhotosViewModel.randomize(): open the feed at an arbitrary sol, then page both ways. */
class VikingStressTest {

    private val config = PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false)
    private val catalog = VikingCatalog()

    private fun source(roverId: Long, anchor: Long, maxSol: Long, cameras: Set<String> = emptySet()) =
        SolPagingSource(
            photosRepository = PhotosRepositoryImpl(api = RestApi(), vikingCatalog = catalog),
            imagesDao = FakeImagesDao(),
            roverId = roverId,
            cameras = cameras,
            initialSol = anchor,
            minSol = 0,
            maxSol = maxSol,
        )

    @Test
    fun `every sol is a safe anchor for both landers`() = runTest {
        for ((roverId, maxSol) in listOf(VIKING_1_ID to 2230L, VIKING_2_ID to 1212L)) {
            for (anchor in 0..maxSol step 7) {
                val pager = TestPager(config, source(roverId, anchor, maxSol))
                val refreshed = assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(
                    pager.refresh(), "refresh failed for rover=$roverId anchor=$anchor"
                )
                assertTrue(refreshed.data.isNotEmpty(), "empty refresh page rover=$roverId anchor=$anchor")
                repeat(2) { pager.append() }
                repeat(2) { pager.prepend() }
            }
        }
    }

    @Test
    fun `every sol is a safe anchor with each camera filter`() = runTest {
        // Viking 1's VLC2 has a 370-sol drought after sol 1009, so this also sweeps the only
        // range in either mission where a refresh has to scan past the load budget.
        for ((roverId, maxSol) in listOf(VIKING_1_ID to 2230L, VIKING_2_ID to 1212L)) {
            for (camera in listOf("VLC1", "VLC2")) {
                for (anchor in 0..maxSol step 37) {
                    val pager = TestPager(config, source(roverId, anchor, maxSol, setOf(camera)))
                    val refreshed = pager.refresh()
                    assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(
                        refreshed, "refresh failed rover=$roverId camera=$camera anchor=$anchor"
                    )
                    pager.append(); pager.prepend()
                }
            }
        }
    }
}
