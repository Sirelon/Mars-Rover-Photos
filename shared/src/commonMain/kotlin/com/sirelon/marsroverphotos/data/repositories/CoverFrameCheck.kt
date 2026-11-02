package com.sirelon.marsroverphotos.data.repositories

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.network.RestApi
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.max

/**
 * Decides whether a raw frame is good enough for a rover card: it downloads the frame and looks
 * at its pixels, because the feed's metadata cannot tell. A partly downlinked frame has the same
 * size and flags as a complete one; only its black band shows the difference.
 */
class CoverFrameCheck(private val api: RestApi) {

    @OptIn(ExperimentalResourceApi::class)
    suspend fun isGoodCover(photo: MarsImage): Boolean {
        val bitmap = api.downloadBytes(photo.imageUrl).decodeToImageBitmap()
        val pixels = bitmap.toPixelMap()
        return isGoodCoverFrame(bitmap.width, bitmap.height) { x, y -> pixels[x, y] }
    }
}

/** Below this width the frame is a small subframe that looks blurry on the card. */
private const val MIN_COVER_WIDTH = 600

/** A sampled row whose brightest channel stays under this level counts as black. */
private const val BLACK_LEVEL = 20f / 255f

/** More black rows than this share means a partly downlinked frame. */
private const val MAX_BLACK_ROW_SHARE = 0.1f

/**
 * Lowest average colorfulness (brightest minus darkest channel, over the lit pixels). Greyscale
 * and raw Bayer frames measure 0; Mastcam and Mastcam-Z color frames measure about 0.2–0.3.
 */
private const val MIN_CHROMA = 15f / 255f

/** About this many samples per row and column, so the check costs the same for any frame size. */
private const val SAMPLES_PER_SIDE = 80

/**
 * True for a frame that is wide enough, has no black band (a partly downlinked frame is black
 * below the received part), and is in color.
 */
internal fun isGoodCoverFrame(width: Int, height: Int, pixelAt: (x: Int, y: Int) -> Color): Boolean {
    if (width < MIN_COVER_WIDTH || height <= 0) return false
    val stepX = max(1, width / SAMPLES_PER_SIDE)
    val stepY = max(1, height / SAMPLES_PER_SIDE)
    var rows = 0
    var blackRows = 0
    var litPixels = 0
    var chromaSum = 0f
    for (y in 0 until height step stepY) {
        rows++
        var rowIsBlack = true
        for (x in 0 until width step stepX) {
            val c = pixelAt(x, y)
            val brightest = maxOf(c.red, c.green, c.blue)
            if (brightest < BLACK_LEVEL) continue
            rowIsBlack = false
            litPixels++
            chromaSum += brightest - minOf(c.red, c.green, c.blue)
        }
        if (rowIsBlack) blackRows++
    }
    if (blackRows > rows * MAX_BLACK_ROW_SHARE) return false
    return litPixels > 0 && chromaSum / litPixels >= MIN_CHROMA
}
