package com.sirelon.marsroverphotos.tracker

import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.feature.MultitouchDetectorCallback
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import com.sirelon.marsroverphotos.storage.MarsImage
import timber.log.Timber

/**
 * Created on 25.07.2021 21:07 for Mars-Rover-Photos.
 */
class FullscreenImageTracker : MultitouchDetectorCallback {

    private val tracker = RoverApplication.APP.tracker

    override var currentImage: MarsImage? = null

    override fun onDoubleTap(zoomToChange: Float) {
        Timber.d("onDoubleTap() called with: zoomToChange = $zoomToChange, $currentImage");
    }

    override fun onZoomGesture(
        zoomToChange: Float,
        offsetY: Float,
        offsetX: Float,
        shouldBlock: Boolean
    ) {
        Timber.d("onZoomGesture() called with: zoomToChange = $zoomToChange, offsetY = $offsetY, offsetX = $offsetX, shouldBlock = $shouldBlock");
    }

    override fun scrollGesture(scrollDelta: Float) {
        Timber.d("scrollGesture() called with: scrollDelta = $scrollDelta");
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