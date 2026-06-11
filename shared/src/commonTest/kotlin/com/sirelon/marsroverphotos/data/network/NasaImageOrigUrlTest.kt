package com.sirelon.marsroverphotos.data.network

import com.sirelon.marsroverphotos.utils.nasaImageOrigUrl
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [nasaImageOrigUrl] derives the full-resolution URL from a NASA Image Library
 * preview href by replacing the size token (~thumb / ~small / ~medium / ~large) with ~orig.
 */
class NasaImageOrigUrlTest {

    @Test
    fun smallToken_replacedWithOrig() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            nasaImageOrigUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~small.jpg"),
        )
    }

    @Test
    fun thumbToken_replacedWithOrig() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            nasaImageOrigUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~thumb.jpg"),
        )
    }

    @Test
    fun mediumToken_replacedWithOrig() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            nasaImageOrigUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~medium.jpg"),
        )
    }

    @Test
    fun largeToken_replacedWithOrig() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            nasaImageOrigUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~large.jpg"),
        )
    }

    @Test
    fun pngExtension_preserved() {
        assertEquals(
            "https://images-assets.nasa.gov/image/foo/foo~orig.png",
            nasaImageOrigUrl("https://images-assets.nasa.gov/image/foo/foo~small.png"),
        )
    }

    @Test
    fun noSizeToken_returnsHrefAsIs() {
        val plain = "https://images-assets.nasa.gov/image/PIA05040/PIA05040.jpg"
        assertEquals(plain, nasaImageOrigUrl(plain))
    }

    @Test
    fun caseInsensitive_lowercaseToken() {
        assertEquals(
            "https://images-assets.nasa.gov/image/PIA05040/PIA05040~orig.jpg",
            nasaImageOrigUrl("https://images-assets.nasa.gov/image/PIA05040/PIA05040~SMALL.jpg"),
        )
    }
}
