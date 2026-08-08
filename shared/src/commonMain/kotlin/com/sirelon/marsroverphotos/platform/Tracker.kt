package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage

interface Tracker {
    fun trackClick(event: String)
    fun trackEvent(event: String, params: Map<String, String> = emptyMap())

    /**
     * Log a GA4 `screen_view` for a Compose destination. Activity-level tracking only ever reports
     * the host Activity, so every Compose screen is invisible in analytics without this.
     */
    fun trackScreen(screenName: String, params: Map<String, String> = emptyMap())

    fun trackFavorite(photo: MarsImage, from: String, fav: Boolean)
    fun trackSeen(photo: MarsImage)
    fun trackScale(photo: MarsImage)
    fun trackSave(photo: MarsImage)
    fun trackShare(photo: MarsImage, packageName: String?)
}
