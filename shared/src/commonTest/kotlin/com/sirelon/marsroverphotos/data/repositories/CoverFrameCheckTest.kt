package com.sirelon.marsroverphotos.data.repositories

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The pixel rules that keep partly downlinked and greyscale frames off the rover cards. */
class CoverFrameCheckTest {

    private val marsSoil = Color(red = 0.75f, green = 0.5f, blue = 0.33f)
    private val grey = Color(red = 0.5f, green = 0.5f, blue = 0.5f)

    @Test
    fun aCompleteColorFrameIsAGoodCover() {
        assertTrue(isGoodCoverFrame(800, 600) { _, _ -> marsSoil })
    }

    @Test
    fun aGreyscaleFrameIsRejected() {
        assertFalse(isGoodCoverFrame(800, 600) { _, _ -> grey })
    }

    // A partly downlinked frame: the received top half in color, black below it.
    @Test
    fun aFrameWithABlackBandIsRejected() {
        assertFalse(isGoodCoverFrame(800, 600) { _, y -> if (y < 300) marsSoil else Color.Black })
    }

    // A thin dark edge is part of many complete frames and must not reject them.
    @Test
    fun aFewDarkRowsAreAllowed() {
        assertTrue(isGoodCoverFrame(800, 600) { _, y -> if (y < 20) Color.Black else marsSoil })
    }

    @Test
    fun aSmallSubframeIsRejected() {
        assertFalse(isGoodCoverFrame(320, 240) { _, _ -> marsSoil })
    }
}
