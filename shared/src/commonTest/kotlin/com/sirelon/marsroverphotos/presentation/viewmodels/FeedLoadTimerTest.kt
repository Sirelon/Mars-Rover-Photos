package com.sirelon.marsroverphotos.presentation.viewmodels

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.platform.Tracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Records [trackFeedLoaded] calls; every other [Tracker] method is an unused no-op stub. */
private class RecordingTracker : Tracker {
    data class FeedLoaded(val rover: String, val loadMs: Long)

    val feedLoadedCalls = mutableListOf<FeedLoaded>()

    override fun trackClick(event: String) = Unit
    override fun trackEvent(event: String, params: Map<String, String>) = Unit
    override fun trackScreen(screenName: String, params: Map<String, String>) = Unit
    override fun trackFeedError(screen: String, error: Throwable, params: Map<String, String>) = Unit
    override fun trackFeedEmpty(screen: String, params: Map<String, String>) = Unit

    override fun trackFeedLoaded(rover: String, loadMs: Long) {
        feedLoadedCalls += FeedLoaded(rover, loadMs)
    }

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

/**
 * [FeedLoadTimer] is split out of [PhotosViewModel] specifically so this can be tested without the
 * ViewModel's seven collaborators — see the class KDoc.
 */
class FeedLoadTimerTest {

    @Test
    fun firstSettleWithPhotosReportsTheRoverAndANonNegativeDuration() {
        val tracker = RecordingTracker()
        val timer = FeedLoadTimer()

        timer.onSettled(itemCount = 3, rover = "Curiosity", tracker = tracker)

        val call = tracker.feedLoadedCalls.single()
        assertEquals("Curiosity", call.rover)
        assertTrue(call.loadMs >= 0)
    }

    @Test
    fun zeroItemsDoesNotReport() {
        val tracker = RecordingTracker()
        val timer = FeedLoadTimer()

        timer.onSettled(itemCount = 0, rover = "Curiosity", tracker = tracker)

        assertTrue(tracker.feedLoadedCalls.isEmpty())
    }

    @Test
    fun onlyTheFirstSettleIsReported() {
        val tracker = RecordingTracker()
        val timer = FeedLoadTimer()

        // Initial load settles, then a later append/pull-to-refresh grows the item count.
        timer.onSettled(itemCount = 3, rover = "Curiosity", tracker = tracker)
        timer.onSettled(itemCount = 30, rover = "Curiosity", tracker = tracker)

        assertEquals(1, tracker.feedLoadedCalls.size)
    }
}
