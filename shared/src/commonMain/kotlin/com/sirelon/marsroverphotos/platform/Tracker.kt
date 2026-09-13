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

    /**
     * What a single ad impression was worth, from the AdMob paid-event callback.
     *
     * Typed rather than a [trackEvent] call because the money has to travel as a number: GA4 only
     * folds ad revenue into user LTV and campaign ROAS when it arrives on the reserved
     * `ad_impression` event as a numeric `value` next to its `currency`, and [trackEvent] carries
     * strings. The AdMob console knows this revenue too, but only per day and per ad unit — never
     * per user, session or screen, which is the whole reason to log it here as well.
     *
     * [value] is in whole [currencyCode] units. The platforms report an impression's worth
     * differently — micros on Android, a decimal on iOS — so each converts at its own call site.
     */
    fun trackAdImpression(
        adSource: String,
        adFormat: String,
        adUnitName: String,
        value: Double,
        currencyCode: String,
        precision: String,
    )

    fun trackFavorite(photo: MarsImage, from: String, fav: Boolean)
    fun trackSeen(photo: MarsImage)
    fun trackScale(photo: MarsImage)
    fun trackSave(photo: MarsImage)
    fun trackShare(photo: MarsImage, packageName: String?)
}
