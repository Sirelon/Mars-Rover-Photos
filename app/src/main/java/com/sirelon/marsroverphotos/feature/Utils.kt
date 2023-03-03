package com.sirelon.marsroverphotos.feature

import androidx.navigation.NavController
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 04.05.2021 17:42 for Mars-Rover-Photos.
 */
fun NavController.navigateToImages(
    image: MarsImage,
    allphotos: List<MarsImage>,
    trackingEnabled: Boolean = true
) {
    val ids = allphotos.map { it.id }
    navigate("photos/${image.id}?ids=${ids.joinToString()}&shouldTrack=$trackingEnabled")
}