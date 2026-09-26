package com.sirelon.marsroverphotos.presentation.review

import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.FakeAppReview
import com.sirelon.marsroverphotos.platform.FakePreferences
import com.sirelon.marsroverphotos.platform.FixedClock
import com.sirelon.marsroverphotos.platform.RecordingTracker
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DAY_MS = 24 * 60 * 60 * 1000L
// A real-world epoch: the cooldown tests subtract months from "now", which must stay positive.
private const val FIRST_LAUNCH_AT = 1_750_000_000_000L
private const val INSTALLED = "5.1.0"

/**
 * The prompt is a policy with no visible outcome — the stores never say whether the sheet showed —
 * so what is worth pinning down is exactly when it fires and what it records, per rule.
 */
class ReviewPrompterTest {

    private val appReview = FakeAppReview()
    private val tracker = RecordingTracker()
    private val clock = FixedClock(FIRST_LAUNCH_AT + 10 * DAY_MS)

    @BeforeTest
    fun setUp() {
        BuildInfo.init(versionName = INSTALLED, isDebug = true, packageName = "test")
    }

    @AfterTest
    fun tearDown() {
        // BuildInfo is a process-wide singleton; put back the desktop default other tests rely on.
        BuildInfo.init(versionName = "unknown", isDebug = false, packageName = "com.sirelon.marsroverphotos")
    }

    /** Settings for a user [launches] opens in, the first of them at [FIRST_LAUNCH_AT]. */
    private fun settings(launches: Int = 3): AppSettings =
        AppSettings(FakePreferences()).apply {
            repeat(launches) { recordLaunch(versionName = INSTALLED, nowMillis = FIRST_LAUNCH_AT) }
        }

    private fun prompter(settings: AppSettings = settings()) =
        ReviewPrompter(settings, appReview, tracker, clock)

    @Test
    fun staysQuietWithoutADelightAction() = runTest {
        val prompter = prompter()

        prompter.onViewerClosed()

        assertEquals(0, appReview.requests)
    }

    @Test
    fun asksAfterADelightOnceTheAppHasEarnedIt() = runTest {
        val settings = settings()
        val prompter = prompter(settings)

        prompter.onDelightAction()
        prompter.onViewerClosed()

        assertEquals(1, appReview.requests)
        assertEquals(clock.nowMillis, settings.lastReviewPromptAt)
        assertEquals(INSTALLED, settings.lastReviewPromptVersion)
        val event = tracker.named("review_prompted").single()
        assertEquals(ReviewPrompter.TRIGGER_VIEWER, event.params["trigger"])
        assertEquals("3", event.params["launch_count"])
    }

    @Test
    fun aDelightIsSpentByTheCloseThatFollowsIt() = runTest {
        // A "not shown" answer leaves no cooldown behind, so only the consumed marker can explain
        // the second close staying quiet.
        appReview.result = false
        val prompter = prompter()

        prompter.onDelightAction()
        prompter.onViewerClosed()
        prompter.onViewerClosed()

        assertEquals(1, appReview.requests)
    }

    @Test
    fun waitsForTheThirdLaunch() = runTest {
        val prompter = prompter(settings(launches = 2))

        prompter.onDelightAction()
        prompter.onViewerClosed()

        assertEquals(0, appReview.requests)
    }

    @Test
    fun waitsThreeDaysAfterTheFirstLaunch() = runTest {
        clock.nowMillis = FIRST_LAUNCH_AT + 2 * DAY_MS
        val prompter = prompter()

        prompter.onDelightAction()
        prompter.onViewerClosed()
        assertEquals(0, appReview.requests)

        clock.nowMillis = FIRST_LAUNCH_AT + 3 * DAY_MS
        prompter.onDelightAction()
        prompter.onViewerClosed()
        assertEquals(1, appReview.requests)
    }

    @Test
    fun respectsTheCooldownAfterAnEarlierPrompt() = runTest {
        val settings = settings().apply {
            lastReviewPromptAt = clock.nowMillis - 100 * DAY_MS
            lastReviewPromptVersion = "5.0.0"
        }
        val prompter = prompter(settings)

        prompter.onDelightAction()
        prompter.onViewerClosed()
        assertEquals(0, appReview.requests)

        settings.lastReviewPromptAt = clock.nowMillis - 120 * DAY_MS
        prompter.onDelightAction()
        prompter.onViewerClosed()
        assertEquals(1, appReview.requests)
    }

    @Test
    fun asksAtMostOncePerBuild() = runTest {
        val settings = settings().apply {
            lastReviewPromptAt = clock.nowMillis - 200 * DAY_MS
            lastReviewPromptVersion = INSTALLED
        }
        val prompter = prompter(settings)

        prompter.onDelightAction()
        prompter.onViewerClosed()

        assertEquals(0, appReview.requests)
    }

    @Test
    fun staysQuietInASessionThatShowedAReleaseCard() = runTest {
        val prompter = prompter()

        prompter.onWhatsNewCardShown()
        prompter.onDelightAction()
        prompter.onViewerClosed()

        assertEquals(0, appReview.requests)
    }

    @Test
    fun staysQuietInASessionWithAFeedError() = runTest {
        val prompter = prompter()

        prompter.onFeedError()
        prompter.onDelightAction()
        prompter.onViewerClosed()

        assertEquals(0, appReview.requests)
    }

    @Test
    fun doesNotSpendThePromptWhenThePlatformShowedNothing() = runTest {
        appReview.result = false
        val settings = settings()
        val prompter = prompter(settings)

        prompter.onDelightAction()
        prompter.onViewerClosed()

        assertEquals(1, appReview.requests)
        assertNull(settings.lastReviewPromptAt)
        assertNull(settings.lastReviewPromptVersion)
        assertTrue(tracker.named("review_prompted").isEmpty())
    }

    @Test
    fun theAboutRowAlwaysAsksAndCountsAsAPrompt() = runTest {
        // First launch, well inside every waiting period: the automatic prompt would stay quiet.
        val settings = settings(launches = 1)
        val prompter = prompter(settings)

        val shown = prompter.requestFromAbout()

        assertTrue(shown)
        assertEquals(1, appReview.requests)
        assertEquals(clock.nowMillis, settings.lastReviewPromptAt)
        assertEquals("about", tracker.named("rate_app_clicked").single().params["from"])
        assertEquals(ReviewPrompter.TRIGGER_ABOUT, tracker.named("review_prompted").single().params["trigger"])
    }
}
