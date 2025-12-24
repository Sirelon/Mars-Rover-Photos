package com.sirelon.marsroverphotos.tracker

import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.exceptionHandler
import com.sirelon.marsroverphotos.feature.MultitouchDetectorCallback
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.absoluteValue

/**
 * Created on 25.07.2021 21:07 for Mars-Rover-Photos.
 */
class FullscreenImageTracker : MultitouchDetectorCallback {

    private val tracker = RoverApplication.APP.tracker
    private val dataManager = RoverApplication.APP.dataManger

    private val scrollChangeCollector = DataFlowCollector<Float>(callback = { first, last ->
        last ?: return@DataFlowCollector
        first ?: return@DataFlowCollector
        val distance = first + last
        dataManager.trackEvent(
            "gesture_scroll", mapOf(
                "from" to first,
                "to" to last,
                "direction" to if (last > 0f) "forward" else "backward",
                "distance" to distance.absoluteValue
            )
        )
    })

    private val zoomChangeCollector =
        DataFlowCollector<Triple<Float, Float, Float>>(callback = { first, last ->
            last ?: return@DataFlowCollector
            first ?: return@DataFlowCollector

            dataManager.trackEvent(
                "gesture_zoom", mapOf(
                    "fromZoom" to first.first,
                    "toZoom" to last.first,

                    "fromY" to first.second,
                    "toY" to last.second,

                    "fromX" to first.third,
                    "toX" to last.third,
                )
            )

            trackScale()
        })

    override var currentImage: MarsImage? = null
    override fun onTap() {
        Timber.d("onTap() called")
        dataManager.trackClick("tap_fullscreen_photo")
    }

    override fun onDoubleTap(zoomToChange: Float) {
        Timber.d("onDoubleTap() called with: zoomToChange = $zoomToChange, $currentImage")
        trackScale()
    }

    override fun onZoomGesture(
        zoomToChange: Float,
        offsetY: Float,
        offsetX: Float,
        shouldBlock: Boolean
    ) {
        Timber.d("onZoomGesture() called with: zoomToChange = $zoomToChange, offsetY = $offsetY, offsetX = $offsetX, shouldBlock = $shouldBlock")
        zoomChangeCollector.onEvent(Triple(zoomToChange, offsetY, offsetX))
    }

    override fun scrollGesture(scrollDelta: Float) {
        Timber.d("scrollGesture() called with: scrollDelta = $scrollDelta")
        scrollChangeCollector.onEvent(scrollDelta)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun trackScale() {
        currentImage?.let { marsPhoto ->
            GlobalScope.launch(exceptionHandler) {
                FirebaseProvider.firebasePhotos.updatePhotoScaleCounter(marsPhoto)
                tracker.trackScale(marsPhoto)
            }
        }
    }
}
