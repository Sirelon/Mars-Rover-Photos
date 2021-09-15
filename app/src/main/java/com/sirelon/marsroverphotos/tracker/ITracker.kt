package com.sirelon.marsroverphotos.tracker

import android.os.Bundle
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 19/04/2017 15:32.
 */
interface ITracker {

    fun trackSeen(photo: MarsImage)

    fun trackScale(photo: MarsImage)

    fun trackShare(photo: MarsImage, packageName: String)

    fun trackSave(photo: MarsImage)

    fun trackClick(event: String)

    fun trackFavorite(photo: MarsImage, from: String, fav: Boolean)
    fun trackEvent(event: String, params: Bundle)
}