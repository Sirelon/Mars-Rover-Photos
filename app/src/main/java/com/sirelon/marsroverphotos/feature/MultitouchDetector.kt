package com.sirelon.marsroverphotos.feature

import android.graphics.Matrix
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import javax.security.auth.callback.Callback
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Created on 15.04.2021 21:48 for Mars-Rover-Photos.
 */
@Composable
fun MultitouchDetector(
    modifier: Modifier,
    enabled: Boolean,
    callback: (zoom: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    val matrix by remember { mutableStateOf(Matrix()) }
//    var angle by remember { mutableStateOf(0f) }
//    var zoom by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }

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
