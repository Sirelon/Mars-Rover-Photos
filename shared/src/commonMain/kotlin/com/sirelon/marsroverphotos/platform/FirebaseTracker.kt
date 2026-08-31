package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage

/**
 * Tracker implementation backed by FirebaseAnalytics.
 * Shared by Android and iOS — platform differences are handled inside FirebaseAnalytics itself.
 */
class FirebaseTracker(private val analytics: FirebaseAnalytics) : Tracker {

    override fun trackClick(event: String) {
        analytics.logEvent(event, emptyMap())
    }

    override fun trackEvent(event: String, params: Map<String, String>) {
        analytics.logEvent(event, params)
    }

    override fun trackScreen(screenName: String, params: Map<String, String>) {
        // GA4 rewrites screen_name/screen_class into firebase_screen/firebase_screen_class in the
        // BigQuery export. screen_class is sent too so the "Screen class" report stops collapsing
        // every Compose destination into the host Activity.
        analytics.logEvent(
            "screen_view",
            params + mapOf("screen_name" to screenName, "screen_class" to screenName),
        )
    }

    override fun trackFeedError(screen: String, error: Throwable, params: Map<String, String>) {
        analytics.logEvent(
            "feed_error",
            params + buildMap {
                put("screen", screen)
                put("error_type", error::class.simpleName ?: "Unknown")
                // GA4 truncates string params at 100 chars. The head of the message carries the
                // status code / host that identifies which backend broke; the tail does not.
                error.message?.takeIf { it.isNotBlank() }?.let { put("reason", it.take(100)) }
            },
        )
    }

    override fun trackFeedEmpty(screen: String, params: Map<String, String>) {
        analytics.logEvent("feed_empty", params + mapOf("screen" to screen))
    }

    override fun trackFavorite(photo: MarsImage, from: String, fav: Boolean) {
        val event = if (fav) "FavoritePhoto" else "UnFavoritePhoto"
        analytics.logEvent(event, mapOf("screen" to from, "photo_id" to photo.id))
    }

    override fun trackSeen(photo: MarsImage) {
        analytics.logEvent("PhotoSeen", mapOf("photo_id" to photo.id))
    }

    override fun trackScale(photo: MarsImage) {
        analytics.logEvent("Scale", mapOf("photo_id" to photo.id))
    }

    override fun trackSave(photo: MarsImage) {
        analytics.logEvent("SavePhoto", mapOf("photo_id" to photo.id))
    }

    override fun trackShare(photo: MarsImage, packageName: String?) {
        analytics.logEvent(
            "SharePhoto",
            buildMap {
                put("photo_id", photo.id)
                if (packageName != null) put("package_name", packageName)
            },
        )
    }
}
