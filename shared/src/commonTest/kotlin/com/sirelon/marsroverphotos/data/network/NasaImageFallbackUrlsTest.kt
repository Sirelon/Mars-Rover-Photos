package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.utils.nasaImageLargeFallbackUrls
import com.sirelon.marsroverphotos.utils.nasaImageSmallFallbackUrls
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the ordered variant-URL fallback chains [nasaImageLargeFallbackUrls] and
 * [nasaImageSmallFallbackUrls] walk when NASA hasn't generated a given size variant for an asset
 * (S3 answers a missing key with 403, not 404 — see NasaImageUrl.kt).
 */
class NasaImageFallbackUrlsTest {

    // ── nasaImageLargeFallbackUrls ──────────────────────────────────────────────

    @Test
    fun large_chain_isLargeThenMediumThenOrig() {
        assertEquals(
            listOf(
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~large.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~medium.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            ),
            nasaImageLargeFallbackUrls("https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg"),
        )
    }

    @Test
    fun large_chain_fromThumbToken() {
        assertEquals(
            listOf(
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~large.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~medium.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            ),
            nasaImageLargeFallbackUrls("https://images-assets.nasa.gov/image/PIA05040/PIA05040~thumb.jpg"),
        )
    }

    @Test
    fun large_chain_pngExtensionPreserved() {
        assertEquals(
            listOf(
                "https://images-assets.nasa.gov/image/foo/foo~large.png",
                "https://images-assets.nasa.gov/image/foo/foo~medium.png",
                "https://images-assets.nasa.gov/image/foo/foo~orig.png",
            ),
            nasaImageLargeFallbackUrls("https://images-assets.nasa.gov/image/foo/foo~small.png"),
        )
    }

    @Test
    fun large_chain_noSizeToken_singleElementUnchanged() {
        // Non-NASA-Image-Library hrefs (Curiosity/Perseverance/Ingenuity/InSight/Viking) must
        // never retry: no known size token means the raw source URL is returned as-is.
        val solUrl = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/03456/opgs/edr/ncam/NRB_720505614EDR_F1010502NCAM00595M_.JPG"
        assertEquals(listOf(solUrl), nasaImageLargeFallbackUrls(solUrl))
    }

    @Test
    fun large_chain_caseInsensitiveToken() {
        assertEquals(
            listOf(
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~large.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~medium.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            ),
            nasaImageLargeFallbackUrls("https://images-assets.nasa.gov/image/PIA05040/PIA05040~SMALL.jpg"),
        )
    }

    // ── nasaImageSmallFallbackUrls ──────────────────────────────────────────────

    @Test
    fun small_chain_isSmallThenThumbThenOrig() {
        assertEquals(
            listOf(
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~thumb.jpg",
                "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            ),
            nasaImageSmallFallbackUrls("https://images-assets.nasa.gov/image/PIA05040/PIA05040~large.jpg"),
        )
    }

    @Test
    fun small_chain_firstEntryMatchesStoredSmallUrl() {
        // MarsImage.imageUrl already stores ~small (see Mappers.kt) — the chain's first entry
        // must equal that stored value so NetworkImage's primary attempt is unchanged.
        val storedSmallUrl = "https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg"
        assertEquals(storedSmallUrl, nasaImageSmallFallbackUrls(storedSmallUrl).first())
    }

    @Test
    fun small_chain_noSizeToken_singleElementUnchanged() {
        val solUrl = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/03456/opgs/edr/ncam/NRB_720505614EDR_F1010502NCAM00595M_.JPG"
        assertEquals(listOf(solUrl), nasaImageSmallFallbackUrls(solUrl))
    }
}
