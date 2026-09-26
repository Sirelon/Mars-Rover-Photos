package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import kotlin.time.Clock
import kotlin.time.Instant

/** In-memory [PlatformPreferences]. */
internal class FakePreferences : PlatformPreferences {
    private val values = mutableMapOf<String, Any>()
    override fun getInt(key: String, defaultValue: Int) = values[key] as? Int ?: defaultValue
    override fun setInt(key: String, value: Int) { values[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean) = values[key] as? Boolean ?: defaultValue
    override fun setBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getString(key: String, defaultValue: String) = values[key] as? String ?: defaultValue
    override fun setString(key: String, value: String) { values[key] = value }
    override fun getLong(key: String, defaultValue: Long) = values[key] as? Long ?: defaultValue
    override fun setLong(key: String, value: Long) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
    override fun clear() = values.clear()
    override fun contains(key: String) = values.containsKey(key)
}

/** Records [trackEvent] and [trackClick] calls; every other [Tracker] method is a no-op stub. */
internal class RecordingTracker : Tracker {
    data class Event(val name: String, val params: Map<String, String>)

    val events = mutableListOf<Event>()

    fun named(name: String): List<Event> = events.filter { it.name == name }

    override fun trackClick(event: String) { events += Event(event, emptyMap()) }
    override fun trackEvent(event: String, params: Map<String, String>) { events += Event(event, params) }
    override fun trackScreen(screenName: String, params: Map<String, String>) = Unit
    override fun trackFeedError(screen: String, error: Throwable, params: Map<String, String>) = Unit
    override fun trackFeedEmpty(screen: String, params: Map<String, String>) = Unit
    override fun trackFeedLoaded(rover: String, loadMs: Long) = Unit
    override fun trackAdImpression(
        adSource: String,
        adFormat: String,
        adUnitName: String,
        value: Double,
        currencyCode: String,
        precision: String,
    ) = Unit
    override fun trackFavorite(photo: MarsImage, from: String, fav: Boolean) = Unit
    override fun trackSeen(photo: MarsImage) = Unit
    override fun trackScale(photo: MarsImage) = Unit
    override fun trackSave(photo: MarsImage) = Unit
    override fun trackShare(photo: MarsImage, packageName: String?) = Unit
}

/** Counts requests and answers each with [result] — what the platform would report. */
internal class FakeAppReview(var result: Boolean = true) : AppReview {
    var requests = 0
    override suspend fun requestReview(): Boolean {
        requests++
        return result
    }
}

/** A clock the test moves by hand. */
internal class FixedClock(var nowMillis: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
}
