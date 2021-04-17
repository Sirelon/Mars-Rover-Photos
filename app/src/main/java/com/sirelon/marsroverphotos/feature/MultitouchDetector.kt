package com.sirelon.marsroverphotos.feature

import android.graphics.Matrix
import android.util.Log
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * Created on 15.04.2021 21:48 for Mars-Rover-Photos.
 */
@Composable
fun MultitouchDetectorNorm(
    modifier: Modifier,
    enabled: Boolean,
    callback: (zoom: Float, offsetX: Float, offsetY: Float) -> Unit
) {
//    val matrix by remember { mutableStateOf(Matrix()) }
//    var angle by remember { mutableStateOf(0f) }
//    var zoom by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }

    val scale = remember { mutableStateOf(1f) }
    val translate = remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier
            .fillMaxSize()
//            .background(Color.Green)
            .pointerInput(Unit) {
                if (enabled) {
                    detectTransformGestures { centroid, pan, gestureZoom, gestureAngle ->
                        Log.i("Sirelon1", "$pan")
                        val anchorX = centroid.x - size.width / 2f
                        val anchorY = centroid.y - size.height / 2f
                        val matrix = Matrix()
                        matrix.postRotate(gestureAngle, anchorX, anchorY)
                        matrix.postScale(gestureZoom, gestureZoom, anchorX, anchorY)
                        matrix.postTranslate(pan.x, pan.y)

                        val v = FloatArray(9)
                        matrix.getValues(v)
                        val scaleX = v[Matrix.MSCALE_X]
                        val skewY = v[Matrix.MSKEW_Y]
                        val offsetX = v[Matrix.MTRANS_X]
                        val offsetY = v[Matrix.MTRANS_Y]
                        val zoom = sqrt(scaleX * scaleX + skewY * skewY)
                        callback(zoom, offsetX, offsetY)
//                    offsetX = v[Matrix.MTRANS_X]
//                    offsetY = v[Matrix.MTRANS_Y]
//                    zoom = sqrt(scaleX * scaleX + skewY * skewY)
//                angle = atan2(v[Matrix.MSKEW_X], v[Matrix.MSCALE_X]) * (-180 / Math.PI.toFloat())
                    }
                }
            }
    ) {
//        Box(
//            Modifier
//                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
//                .graphicsLayer(
//                    scaleX = zoom,
//                    scaleY = zoom,
////                    rotationZ = angle
//                )
//                .fillMaxSize()
//        ) {
//            content()
//        }
    }

}

@Composable
fun MultitouchDetector(
    modifier: Modifier,
    enabled: Boolean,
    callback: (zoom: Float, offsetX: Float, offsetY: Float) -> Unit
) {
//    val matrix by remember { mutableStateOf(Matrix()) }
//    var angle by remember { mutableStateOf(0f) }
//    var zoom by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }

    val scale = remember { mutableStateOf(1f) }
    val translate = remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier
            .fillMaxSize()
//            .background(Color.Green)
            .pointerInput(Unit) {
                if (enabled) {
                    forEachGesture {
                        awaitPointerEventScope {
                            var rotation = 0f
                            var zoom = 1f
                            var pan = Offset.Zero
                            var pastTouchSlop = false
                            val touchSlop = viewConfiguration.touchSlop
                            var lockedToPanZoom = false

                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.fastAny { it.positionChangeConsumed() }
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
//                                            onGesture(centroid, panChange, zoomChange, effectiveRotation, callback)

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
                                            val offsetX = panChange.x
                                            val offsetY = panChange.y
                                            val zoom = sqrt(scaleX * scaleX + skewY * skewY)
                                            callback(zoom, offsetX, offsetY)


                                            if (offsetX.absoluteValue < 7f) {
                                                Log.i("Sirelon1", "OFFSET ! $offsetX")
                                                event.changes.fastForEach {
                                                    if (it.positionChanged()) {
                                                        it.consumeAllChanges()
                                                    }
                                                }
                                            }
                                        }
//                                        event.changes.fastForEach {
//                                            if (it.positionChanged()) {
//                                                it.consumeAllChanges()
//                                            }
//                                        }
                                    }
                                }
                            } while (!canceled && event.changes.fastAny { it.pressed })
                        }
                    }
                }
            }
    ) {
//        Box(
//            Modifier
//                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
//                .graphicsLayer(
//                    scaleX = zoom,
//                    scaleY = zoom,
////                    rotationZ = angle
//                )
//                .fillMaxSize()
//        ) {
//            content()
//        }
    }

}
