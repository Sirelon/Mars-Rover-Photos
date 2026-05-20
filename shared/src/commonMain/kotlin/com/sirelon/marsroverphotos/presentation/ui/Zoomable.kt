package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

data class ZoomableState(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
)

@Composable
fun rememberZoomableState(): ZoomableState {
    // State is managed internally by Modifier.zoomable via composed {}
    // This function is kept for API compatibility with callers
    return ZoomableState()
}

fun Modifier.zoomable(
    minScale: Float = 1f,
    maxScale: Float = 5f,
    onScaleChanged: ((Float) -> Unit)? = null
): Modifier = composed {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    this
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                scale = newScale
                onScaleChanged?.invoke(newScale)
                // Only pan when zoomed in; reset offset when back at min scale
                if (scale > minScale) {
                    offset += pan
                } else {
                    offset = Offset.Zero
                }
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    scale = minScale
                    offset = Offset.Zero
                    onScaleChanged?.invoke(minScale)
                }
            )
        }
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y
        )
}
