package com.sirelon.marsroverphotos.data.phoenix

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import com.sirelon.marsroverphotos.data.paging.FakeImagesDao
import com.sirelon.marsroverphotos.data.paging.SolPagingSource
import com.sirelon.marsroverphotos.data.repositories.PhotosRepositoryImpl
import com.sirelon.marsroverphotos.data.viking.VikingCatalog
import com.sirelon.marsroverphotos.domain.models.PHOENIX_ID
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises the real bundled catalogue end to end, and the routing that puts it in front of the
 * network for Phoenix.
 *
 * Counts here are the archive's, as filtered and verified by `scripts/generate-phoenix-catalog.mjs`.
 * If one of them changes, the generator was re-run against different data or with different
 * filters, and that deserves a look rather than a test edit.
 */
class PhoenixCatalogTest {

    private val catalog = PhoenixCatalog()

    private suspend fun allPhotos() = (0L..151L).flatMap { catalog.photosForSol(it, camera = null) }

    @Test
    fun `holds every catalogued photo with both cameras`() = runTest {
        val all = allPhotos()

        assertEquals(1924, all.size)
        assertEquals(849, all.count { it.camera?.name == "SSI" })
        assertEquals(1075, all.count { it.camera?.name == "RAC" })
        // The product id is the Room primary key: a duplicate would merge two photos.
        assertEquals(all.size, all.map { it.id }.distinct().size)
        assertTrue(all.all { it.roverId == PHOENIX_ID })
    }

    @Test
    fun `serves the landing sol`() = runTest {
        val sol0 = catalog.photosForSol(sol = 0, camera = null)

        assertEquals(10, sol0.size)
        assertEquals("SS000EDN896227848_10C70R1M1", sol0.first().id)
        assertEquals("2008-05-26T00:09:42Z", sol0.first().earthDate)
    }

    @Test
    fun `filters by camera`() = runTest {
        val all = catalog.photosForSol(sol = 30, camera = null)
        val ssi = catalog.photosForSol(sol = 30, camera = "SSI")
        val rac = catalog.photosForSol(sol = 30, camera = "RAC")

        assertEquals(21, ssi.size)
        assertEquals(8, rac.size)
        assertEquals(all.size, ssi.size + rac.size)
        // The arm camera was not deployed on the landing sol.
        assertTrue(catalog.photosForSol(sol = 0, camera = "RAC").isEmpty())
    }

    @Test
    fun `an empty sol is an empty list, not an error`() = runTest {
        assertTrue(catalog.photosForSol(sol = 23, camera = null).isEmpty())
        assertTrue(catalog.photosForSol(sol = 99_999, camera = null).isEmpty())
    }

    @Test
    fun `latest and random photos come from the catalogue`() = runTest {
        val latest = assertNotNull(catalog.latestPhoto())
        assertEquals("SS151EFF909587950_208F0RAM1", latest.id)
        assertEquals(151L, latest.sol)

        val random = assertNotNull(catalog.randomPhoto())
        assertEquals(PHOENIX_ID, random.roverId)
    }

    @Test
    fun `the repository routes Phoenix to the catalogue`() = runTest {
        val repository = PhotosRepositoryImpl(api = RestApi(), vikingCatalog = VikingCatalog(), phoenixCatalog = catalog)

        val photos = repository.refreshImages(PhotosQueryRequest(PHOENIX_ID, sol = 0, camera = null))
        assertEquals(10, photos.size)
        assertEquals("SS151EFF909587950_208F0RAM1", repository.latestPhoto(PHOENIX_ID)?.id)
    }

    @Test
    fun `the sol feed steps over Phoenix's empty sols`() = runTest {
        val repository = PhotosRepositoryImpl(api = RestApi(), vikingCatalog = VikingCatalog(), phoenixCatalog = catalog)
        val pager = TestPager(
            // Mirrors the production config in RoverFeedPager.
            PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false),
            SolPagingSource(
                photosRepository = repository,
                imagesDao = FakeImagesDao(),
                roverId = PHOENIX_ID,
                cameras = emptySet(),
                initialSol = 22,
                minSol = 0,
                maxSol = 151,
            ),
        )

        // Sol 22 has photos and sol 23 has none, so the page after sol 22 is sol 24.
        val first = assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(pager.refresh())
        assertTrue(first.data.isNotEmpty() && first.data.all { it.sol == 22L })
        val second = assertIs<PagingSource.LoadResult.Page<Long, MarsImage>>(pager.append())
        assertEquals(24L, second.data.first().sol)
    }
}
