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

    /**
     * A paged feed that settled with nothing to show because its source failed.
     *
     * Rover photos come from several independent NASA backends, so a source going down degrades
     * into an ordinary-looking empty state rather than a crash — invisible to Crashlytics and
     * indistinguishable, in the funnel, from a user who simply never opened a rover.
     */
    fun trackFeedError(screen: String, error: Throwable, params: Map<String, String> = emptyMap())

    /**
     * A paged feed that loaded successfully and legitimately had no results — an unfiltered feed
     * hitting this is a data-coverage gap rather than an outage, which is why it is a separate
     * event from [trackFeedError] rather than a parameter on it.
     */
    fun trackFeedEmpty(screen: String, params: Map<String, String> = emptyMap())

    fun trackFavorite(photo: MarsImage, from: String, fav: Boolean)
    fun trackSeen(photo: MarsImage)
    fun trackScale(photo: MarsImage)
    fun trackSave(photo: MarsImage)
    fun trackShare(photo: MarsImage, packageName: String?)
}
