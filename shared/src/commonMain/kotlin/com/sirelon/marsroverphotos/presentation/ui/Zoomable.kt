package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Mutable state for [Modifier.zoomable].
 *
 * Holds the current scale and pan offset; read these in your layout to react to zoom changes.
 * Call [reset] to animate/snap back to 1× (e.g. when the current page leaves the viewport).
 */
class ZoomableState {
    var scale by mutableFloatStateOf(1f)
        internal set
    var offset by mutableStateOf(Offset.Zero)
        internal set

    /** Snap scale back to 1× and pan back to zero. */
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }
}

/** Remember a [ZoomableState] across recompositions. */
@Composable
fun rememberZoomableState(): ZoomableState = remember { ZoomableState() }

/**
 * Adds pinch-to-zoom and one-finger-pan support to any composable.
 *
 * The [state] is updated on every pointer event; reading [ZoomableState.scale] or
 * [ZoomableState.offset] outside this modifier will reflect the current value.
 * Double-tap reset is NOT handled here — add a separate `pointerInput { detectTapGestures
 * (onDoubleTap = { state.reset() }) }` on the same modifier chain if you need it.
 *
 * @param state     The [ZoomableState] driving scale and translation.
 * @param minScale  Minimum zoom level (default 1×).
 * @param maxScale  Maximum zoom level (default 5×).
 */
fun Modifier.zoomable(
    state: ZoomableState,
    minScale: Float = 1f,
    maxScale: Float = 5f,
): Modifier = this
    .pointerInput(state) {
        detectTransformGestures { _, pan, zoom, _ ->
            val newScale = (state.scale * zoom).coerceIn(minScale, maxScale)
            state.scale = newScale
            state.offset = if (newScale > minScale) state.offset + pan else Offset.Zero
        }
    }
    .graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }
