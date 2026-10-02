package com.sirelon.marsroverphotos.presentation.review

import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.AppReview
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.presentation.navigation.ScreenNames
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Decides when to ask for a store review, and asks.
 *
 * The ask is the platform's own sheet ([AppReview]) with no lead-in question of the app's own: both
 * stores forbid pre-screening, and Apple shows the sheet at most three times a year per device. So
 * the whole policy is *when*, and the answer is a moment of delight once the app has earned it:
 *
 * - the fullscreen viewer was just closed after a favorite, save or share during that visit;
 * - this is at least the [MIN_LAUNCHES]th launch, [MIN_DAYS_SINCE_FIRST_LAUNCH] days after the first;
 * - no prompt in the last [COOLDOWN_DAYS] days, and none yet on this build;
 * - nothing this session has soured the moment: no release card was shown, no feed failed.
 *
 * Neither store reports whether the sheet was shown or what was chosen, so a prompt counts as spent
 * the moment the platform accepted it, and the automatic path never falls back to opening the store
 * listing — that would turn a quiet nudge into a redirect. The manual path (About → Rate the App)
 * keeps its fallback and also counts as a prompt, so the automatic one stays quiet afterwards.
 *
 * "Session" is the process: the flags below live in memory and start clean on every launch.
 */
class ReviewPrompter(
    private val appSettings: AppSettings,
    private val appReview: AppReview,
    private val tracker: Tracker,
    private val clock: Clock = Clock.System,
) {
    private var delightInViewer = false
    private var whatsNewCardShown = false
    private var feedErrorSeen = false

    /** A favorite, save or share happened in the fullscreen viewer. */
    fun onDelightAction() {
        delightInViewer = true
    }

    /** The Rovers list showed a release card; one nudge per session is enough. */
    fun onWhatsNewCardShown() {
        whatsNewCardShown = true
    }

    /** A feed failed to load; nobody rates an app that just showed them an error. */
    fun onFeedError() {
        feedErrorSeen = true
    }

    /**
     * The fullscreen viewer was closed. Asks for a review only when that visit earned it and the
     * policy in the class doc allows it. The delight marker is consumed either way, so a later
     * visit has to earn it again.
     */
    suspend fun onViewerClosed() {
        val earned = delightInViewer
        delightInViewer = false
        if (!earned || !isEligible()) return
        // Let the pop transition finish and the list settle before a sheet slides up over it.
        delay(SETTLE_DELAY_MS)
        request(trigger = TRIGGER_VIEWER)
    }

    /**
     * The About row. Always asks, and returns whether the platform accepted the request — `false`
     * means nothing was shown and the caller may open the store listing instead.
     */
    suspend fun requestFromAbout(): Boolean {
        tracker.trackEvent("rate_app_clicked", mapOf("from" to ScreenNames.ABOUT))
        return request(trigger = TRIGGER_ABOUT)
    }

    private suspend fun request(trigger: String): Boolean {
        val shown = appReview.requestReview()
        if (shown) {
            appSettings.lastReviewPromptAt = clock.now().toEpochMilliseconds()
            appSettings.lastReviewPromptVersion = BuildInfo.versionName
            tracker.trackEvent(
                "review_prompted",
                mapOf("trigger" to trigger, "launch_count" to appSettings.launchCount.toString()),
            )
        } else {
            Logger.d(TAG) { "Review sheet not shown (trigger=$trigger)" }
        }
        return shown
    }

    private fun isEligible(): Boolean {
        val now = clock.now().toEpochMilliseconds()
        val firstLaunchAt = appSettings.firstLaunchAt ?: return false
        if (appSettings.launchCount < MIN_LAUNCHES) return false
        if (now - firstLaunchAt < MIN_DAYS_SINCE_FIRST_LAUNCH.days.inWholeMilliseconds) return false
        val lastPromptAt = appSettings.lastReviewPromptAt
        if (lastPromptAt != null && now - lastPromptAt < COOLDOWN_DAYS.days.inWholeMilliseconds) return false
        if (appSettings.lastReviewPromptVersion == BuildInfo.versionName) return false
        return !whatsNewCardShown && !feedErrorSeen
    }

    companion object {
        private const val TAG = "ReviewPrompter"
        const val MIN_LAUNCHES = 3
        const val MIN_DAYS_SINCE_FIRST_LAUNCH = 3
        const val COOLDOWN_DAYS = 120

        /** Longer than the viewer's pop fade (AppMotion.SharedContainerMs), so the grid is at rest. */
        const val SETTLE_DELAY_MS = 600L
        const val TRIGGER_VIEWER = "viewer_delight"
        const val TRIGGER_ABOUT = "about"
    }
}
