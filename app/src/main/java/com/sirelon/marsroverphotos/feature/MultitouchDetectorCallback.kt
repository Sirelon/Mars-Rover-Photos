package com.sirelon.marsroverphotos.feature

import com.sirelon.marsroverphotos.storage.MarsImage

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