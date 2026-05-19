package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage

class AndroidTracker(private val analytics: FirebaseAnalytics) : Tracker {

    override fun trackClick(event: String) {
        analytics.logEvent(event, emptyMap())
    }

    override fun trackEvent(event: String, params: Map<String, String>) {
        analytics.logEvent(event, params as Map<String, Any>)
    }

    override fun trackFavorite(photo: MarsImage, from: String, fav: Boolean) {
        val event = if (fav) "FavoritePhoto" else "UnFavoritePhoto"
        analytics.logEvent(
            event,
            mapOf("screen" to from, "photo_id" to photo.id) as Map<String, Any>
        )
    }

    override fun trackSeen(photo: MarsImage) {
        analytics.logEvent("PhotoSeen", mapOf("photo_id" to photo.id) as Map<String, Any>)
    }

    override fun trackScale(photo: MarsImage) {
        analytics.logEvent("Scale", mapOf("photo_id" to photo.id) as Map<String, Any>)
    }

    override fun trackSave(photo: MarsImage) {
        analytics.logEvent("SavePhoto", mapOf("photo_id" to photo.id) as Map<String, Any>)
    }

    override fun trackShare(photo: MarsImage, packageName: String?) {
        val params = mutableMapOf<String, Any>("photo_id" to photo.id)
        if (packageName != null) params["package_name"] = packageName
        analytics.logEvent("SharePhoto", params)
    }
}
