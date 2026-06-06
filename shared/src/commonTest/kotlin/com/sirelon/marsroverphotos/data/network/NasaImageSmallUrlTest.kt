package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.data.network.models.NasaImagesCollection
import com.sirelon.marsroverphotos.data.network.models.NasaImagesItem
import com.sirelon.marsroverphotos.data.network.models.NasaImagesItemData
import com.sirelon.marsroverphotos.data.network.models.NasaImagesItemLink
import com.sirelon.marsroverphotos.data.network.models.NasaImagesSearchResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NasaImageSmallUrlTest {

    // ── nasaImageSmallUrl ──────────────────────────────────────────────────────

    @Test
    fun thumbToken_replacedWithSmall() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg",
            nasaImageSmallUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~thumb.jpg"),
        )
    }

    @Test
    fun smallToken_remainsSmall() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg",
            nasaImageSmallUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg"),
        )
    }

    @Test
    fun mediumToken_replacedWithSmall() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg",
            nasaImageSmallUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~medium.jpg"),
        )
    }

    @Test
    fun largeToken_replacedWithSmall() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg",
            nasaImageSmallUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~large.jpg"),
        )
    }

    @Test
    fun pngExtension_preserved() {
        assertEquals(
            "https://images-assets.nasa.gov/image/foo/foo~small.png",
            nasaImageSmallUrl("https://images-assets.nasa.gov/image/foo/foo~thumb.png"),
        )
    }

    @Test
    fun noSizeToken_passthroughUnchanged() {
        val plain = "https://images-assets.nasa.gov/image/PIA05040/PIA05040.jpg"
        assertEquals(plain, nasaImageSmallUrl(plain))
    }

    @Test
    fun caseInsensitive_uppercaseToken() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg",
            nasaImageSmallUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~LARGE.jpg"),
        )
    }

    // ── nasaImageOrigUrl passthrough for non-NASA-library URLs ─────────────────

    @Test
    fun nonNasaLibraryUrl_origHelperPassesThrough() {
        // Sol rover URLs (Curiosity / Perseverance / InSight) don't contain a size token;
        // nasaImageOrigUrl must return them unchanged so the detail viewer is safe for all rovers.
        val solUrl = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/03456/opgs/edr/ncam/NRB_720505614EDR_F1010502NCAM00595M_.JPG"
        assertEquals(solUrl, nasaImageOrigUrl(solUrl))
    }

    // ── toMarsImages stores ~small; nasaImageOrigUrl recovers ~orig ────────────

    @Test
    fun toMarsImages_storesSmallUrl() {
        val thumbHref = "https://images-assets.nasa.gov/image/PIA05040/PIA05040~thumb.jpg"
        val response = fakeResponse(thumbHref)
        val images = response.toMarsImages()
        assertEquals(1, images.size)
        assertTrue(
            images[0].imageUrl.contains("~small."),
            "Expected ~small in imageUrl but got: ${images[0].imageUrl}",
        )
    }

    @Test
    fun toMarsImages_origUrlRecoverable() {
        val thumbHref = "https://images-assets.nasa.gov/image/PIA05040/PIA05040~thumb.jpg"
        val response = fakeResponse(thumbHref)
        val imageUrl = response.toMarsImages()[0].imageUrl
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            nasaImageOrigUrl(imageUrl),
        )
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun fakeResponse(thumbHref: String) = NasaImagesSearchResponse(
        collection = NasaImagesCollection(
            items = listOf(
                NasaImagesItem(
                    data = listOf(NasaImagesItemData(nasaId = "PIA05040", title = "Test")),
                    links = listOf(NasaImagesItemLink(href = thumbHref, render = "image")),
                )
            )
        )
    )
}
