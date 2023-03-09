package com.sirelon.marsroverphotos.feature

import android.graphics.Matrix
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Created on 15.04.2021 21:48 for Mars-Rover-Photos.
 */

interface MultitouchDetectorCallback {

    var currentImage: MarsImage?

    fun onTap()

    fun onDoubleTap(zoomToChange: Float)

    fun onZoomGesture(zoomToChange: Float, offsetY: Float, offsetX: Float, shouldBlock: Boolean)

    fun scrollGesture(scrollDelta: Float)
}

@Composable
fun MultitouchDetector(
    state: MultitouchState,
    modifier: Modifier = Modifier,
    callback: MultitouchDetectorCallback? = null,
    content: @Composable () -> Unit,
) {
    var zoomToChange by remember { mutableStateOf(state.zoom) }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var childSize by remember { mutableStateOf(IntSize(0, 0)) }
    var position by remember { mutableStateOf(Offset.Zero) }
    var parentSize by remember { mutableStateOf(IntSize(0, 0)) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                scope.launch {
                    detectTapGestures(
                        onTap = {
                            callback?.onTap()
                        },
                        onDoubleTap = {
                            val toChange = if (zoomToChange != 1f) 1f else state.maxZoom
                            offsetX = 0f
                            offsetY = 0f
                            zoomToChange = toChange
                            callback?.onDoubleTap(toChange)
                        })
                }

                gestureDetectorAnalyser { zoomVal: Float, offsetXVal: Float, offsetYVal: Float ->
                    var shouldBlock = true

                    val zoom = zoomToChange * zoomVal
                    zoomToChange = zoom.coerceIn(state.minZoom, state.maxZoom)


                    if (zoomToChange == 1f) {
                        val one = if (offsetXVal < 0) 1 else -1
                        val delta = offsetXVal - 10f * one
                        Timber.d("MultitouchDetector() called with: offsetXVal = $offsetXVal")
//                        pagerState.dispatchRawDelta(delta)

//                        scope.launch {
//                            val toScroll =
//                                if (offsetXVal >= 1f) 1f else if (offsetXVal < 0f) 0f else offsetXVal
//                            val page =
//                                if (one == 1) pagerState.currentPage + 1 else pagerState.currentPage - 1
//                            Timber.d("MultitouchDetector() called $offsetXVal");
////                            if (!pagerState.isScrollInProgress)
//                            pagerState.animateScrollToPage(
//                                page = page
//                                    .coerceAtMost(pagerState.pageCount - 1)
//                                    .coerceAtLeast(1),
//                                pageOffset = toScroll.absoluteValue
//                            )
//                        }
                        callback?.scrollGesture(delta)
                        return@gestureDetectorAnalyser false
                    }

                    val yLimitBottom = offsetY + (childSize.height * zoom) - parentSize.height * 1.7
                    if (offsetYVal < 0) {
                        if (position.y + yLimitBottom > 0) {
                            offsetY += offsetYVal
                        } else {
                            // This is need to position view after zoom it to the right place
                            if (state.zoom != zoomToChange) {
                                offsetY = 0f
                            }
                        }
                    } else {
                        if (offsetY + position.y < 0) {
                            offsetY += offsetYVal
                        } else {
                            // This is need to position view after zoom it to the right place
                            if (state.zoom != zoomToChange) {
                                offsetY = 0f
                            }
                        }
                    }

                    val offX = position.x + (childSize.width * zoomToChange)

                    if (offsetXVal > 0) {
                        if (position.x < 0) {
                            offsetX += offsetXVal
                        } else {
                            // This is need to position view after zoom it to the right place
                            if (state.zoom != zoomToChange) {
                                offsetX = 0f
                            }
                            shouldBlock = false
                        }
                    } else if (offX > parentSize.width) {
                        offsetX += offsetXVal
                    } else {
                        shouldBlock = false
                    }

                    state.zoom = zoomToChange

                    if (zoom == 1f) {
                        offsetY = 0f
                        offsetX = 0f
                    }

                    callback?.onZoomGesture(zoomToChange, offsetY, offsetX, shouldBlock)

                    shouldBlock
                }
            }
    ) {
        val intOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt())

        Box(
            Modifier
                .offset { intOffset }
                .graphicsLayer(scaleX = zoomToChange, scaleY = zoomToChange)
                .onGloballyPositioned {
                    childSize = it.size
                    position = it.positionInParent()
                    parentSize = it.parentCoordinates?.size!!
                }
                .fillMaxSize(),
        ) {
            content()
        }
    }
}

class MultitouchState(
    val maxZoom: Float,
    val minZoom: Float,
    var zoom: Float = 1f,
    val enabled: Boolean = true
) {
    companion object {
        /**
         * The default [Saver] implementation for [MultitouchState].
         */
        val Saver: Saver<MultitouchState, *> = listSaver(
            save = { listOf<Any>(it.maxZoom, it.minZoom, it.zoom, it.enabled) },
            restore = {
                MultitouchState(
                    maxZoom = it[0] as Float,
                    minZoom = it[1] as Float,
                    zoom = it[2] as Float,
                    enabled = it[3] as Boolean,
                )
            }
        )
    }
}

suspend fun PointerInputScope.gestureDetectorAnalyser(analyse: (zoomVal: Float, offsetXVal: Float, offsetYVal: Float) -> Boolean) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize =
                        event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion =
                        abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = false && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation =
                        if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        val anchorX = centroid.x - size.width / 2f
                        val anchorY = centroid.y - size.height / 2f
                        val matrix = Matrix()
                        matrix.postRotate(effectiveRotation, anchorX, anchorY)
                        matrix.postScale(
                            zoomChange,
                            zoomChange,
                            anchorX,
                            anchorY
                        )
                        matrix.postTranslate(pan.x, pan.y)

                        val v = FloatArray(9)
                        matrix.getValues(v)
                        val scaleX = v[Matrix.MSCALE_X]
                        val skewY = v[Matrix.MSKEW_Y]
//                                            val offsetX = v[Matrix.MTRANS_X]
//                                            val offsetY = v[Matrix.MTRANS_Y]
                        val offsetXVal = panChange.x
                        val offsetYVal = panChange.y
                        val zoomVal = sqrt(scaleX * scaleX + skewY * skewY)
                        val shouldBlock = analyse(zoomVal, offsetXVal, offsetYVal)

                        if (shouldBlock) {
                            event.changes.fastForEach {
                                if (it.positionChanged()) {
                                    it.consume()
                                }
                            }
                        }
                    } else {
                        event.changes.fastForEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}