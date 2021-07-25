package com.sirelon.marsroverphotos.tracker

import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.feature.MultitouchDetectorCallback
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import com.sirelon.marsroverphotos.storage.MarsImage
import timber.log.Timber
import kotlin.math.absoluteValue

/**
 * Created on 25.07.2021 21:07 for Mars-Rover-Photos.
 */
class FullscreenImageTracker : MultitouchDetectorCallback {

    private val tracker = RoverApplication.APP.tracker
    private val dataManager = RoverApplication.APP.dataManger

    // TODO: Seen Counter

    private val scrollChangeCollector = DataFlowCollector<Float>(callback = { first, last ->
        last ?: return@DataFlowCollector
        first ?: return@DataFlowCollector
        val distance = first + last
        dataManager.trackEvent(
            "scroll", mapOf(
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
            trackScale()
        })

    override var currentImage: MarsImage? = null

    override fun onDoubleTap(zoomToChange: Float) {
        Timber.d("onDoubleTap() called with: zoomToChange = $zoomToChange, $currentImage");
        trackScale()
    }

    override fun onZoomGesture(
        zoomToChange: Float,
        offsetY: Float,
        offsetX: Float,
        shouldBlock: Boolean
    ) {
        Timber.d("onZoomGesture() called with: zoomToChange = $zoomToChange, offsetY = $offsetY, offsetX = $offsetX, shouldBlock = $shouldBlock");
        zoomChangeCollector.onEvent(Triple(zoomToChange, offsetY, offsetX))
    }

    override fun scrollGesture(scrollDelta: Float) {
        Timber.d("scrollGesture() called with: scrollDelta = $scrollDelta");
        scrollChangeCollector.onEvent(scrollDelta)
    }

    private fun trackScale() {
        val marsPhoto = currentImage?.toMarsPhoto()
        marsPhoto?.let {
            FirebaseProvider.firebasePhotos.updatePhotoScaleCounter(marsPhoto)
                .onErrorReturn { 0 }
                .subscribe()
            tracker.trackScale(marsPhoto)
        }
    }
}