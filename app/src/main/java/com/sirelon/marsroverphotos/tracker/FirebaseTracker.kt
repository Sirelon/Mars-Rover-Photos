package com.sirelon.marsroverphotos.tracker

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 19/04/2017 15:33.
 */
class FirebaseTracker(context: Context) : ITracker {

    private val fb = FirebaseAnalytics.getInstance(context)

    companion object {
        private const val EARTH_DATE: String = "earthDate"
        private const val SCREEN_NAME: String = "screenName"
        private const val SHARE_PACKAGE: String = "sharePackage"
        private const val EVENT_MARS_PHOTO_SCALE: String = "Scale"
        private const val EVENT_MARS_PHOTO_SHARE: String = "SharePhoto"
        private const val EVENT_MARS_PHOTO_SAVE: String = "SavePhoto"
        private const val EVENT_MARS_PHOTO_FAVORITE: String = "FavoritePhoto"
        private const val EVENT_MARS_PHOTO_UNFAVORITE: String = "UnFavoritePhoto"
    }

    override fun trackSeen(photo: MarsImage) {
        fb.logEvent("PhotoSeen", photo.arguments())
    }

    override fun trackScale(photo: MarsImage) {
        fb.logEvent(EVENT_MARS_PHOTO_SCALE, photo.arguments())
    }

    override fun trackShare(photo: MarsImage, packageName: String) {
        val arguments = photo.arguments()
        arguments.putString(SHARE_PACKAGE, packageName)
        fb.logEvent(EVENT_MARS_PHOTO_SHARE, arguments)
    }

    override fun trackSave(photo: MarsImage) {
        fb.logEvent(EVENT_MARS_PHOTO_SAVE, photo.arguments())
    }

    override fun trackClick(event: String) {
        fb.logEvent(event, null)
    }

    override fun trackEvent(event: String, params: Bundle) {
        fb.logEvent(event, params)
    }

    override fun trackFavorite(photo: MarsImage, from: String, fav: Boolean) {
        val event = if (fav) EVENT_MARS_PHOTO_FAVORITE else EVENT_MARS_PHOTO_UNFAVORITE
        val arguments = photo.arguments()
        arguments.putString(SCREEN_NAME, from)
        fb.logEvent(event, arguments)
    }

    private fun MarsImage.arguments() =
        bundleOf("id" to id, "name" to (name ?: ""), EARTH_DATE to earthDate)

}
