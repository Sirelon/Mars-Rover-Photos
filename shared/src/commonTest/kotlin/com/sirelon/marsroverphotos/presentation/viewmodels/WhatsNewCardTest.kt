package com.sirelon.marsroverphotos.presentation.viewmodels

import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.domain.repositories.ReleaseNotesRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.FakeAppReview
import com.sirelon.marsroverphotos.platform.FakePreferences
import com.sirelon.marsroverphotos.platform.RecordingTracker
import com.sirelon.marsroverphotos.presentation.review.ReviewPrompter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Returns [releases] only once [ready] completes, so a test can hold the fetch open. */
private class FakeReleaseNotesRepository(
    private val releases: ImmutableList<Release>,
    private val ready: CompletableDeferred<Unit> = CompletableDeferred(Unit),
) : ReleaseNotesRepository {
    override suspend fun getReleases(): ImmutableList<Release> {
        ready.await()
        return releases
    }
}

/**
 * The card replaces a launch dialog, so the rules worth pinning down are the ones that keep it
 * quiet: never on a first launch, never for a fresh install's own version, never twice for one
 * version, and one card per session. The update case still has to compare versions rather than
 * match them, so a user on an old build is pointed at the newest *store-approved* release.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WhatsNewCardTest {

    private val installedVersion = "5.1.0"
    private val tracker = RecordingTracker()

    private fun release(version: String, changes: Int = 1) = Release(
        version = version,
        date = LocalDate(2026, 6, 29),
        changes = List(changes) { i ->
            Release.Change(id = "$version-$i", icon = "rocket_launch", title = "Change $i", summary = "New")
        }.toImmutableList(),
    )

    private fun releases(vararg versions: String) = versions.map { release(it) }.toImmutableList()

    /**
     * Settings for a user [launches] opens in. [lastSeen] set means the older build's card was
     * acknowledged — i.e. this is an update, not a fresh install; [firstLaunchVersion] is the build
     * that was running when counting started.
     */
    private fun settings(
        launches: Int = 2,
        lastSeen: String? = null,
        firstLaunchVersion: String = installedVersion,
    ): AppSettings = AppSettings(FakePreferences()).apply {
        lastSeen?.let { lastSeenVersion = it }
        repeat(launches) { recordLaunch(versionName = firstLaunchVersion, nowMillis = 1_000L) }
    }

    private fun viewModel(
        repository: ReleaseNotesRepository,
        settings: AppSettings = settings(),
    ): WhatsNewViewModel {
        BuildInfo.init(versionName = installedVersion, isDebug = true, packageName = "test")
        return WhatsNewViewModel(
            releaseNotesRepository = repository,
            appSettings = settings,
            tracker = tracker,
            reviewPrompter = ReviewPrompter(settings, FakeAppReview(), tracker),
        )
    }

    private suspend fun WhatsNewViewModel.cardOnceLoaded(): WhatsNewCard? = state.first { !it.isLoading }.card

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        // BuildInfo is a process-wide singleton and these tests write to it, so it has to be put
        // back — the desktop default other tests may rely on.
        BuildInfo.init(versionName = "unknown", isDebug = false, packageName = "com.sirelon.marsroverphotos")
    }

    @Test
    fun noCardOnTheFirstLaunch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(
            FakeReleaseNotesRepository(releases("5.1.0", "5.2.0")),
            settings(launches = 1, lastSeen = "5.0.0"),
        )

        assertNull(vm.cardOnceLoaded())
    }

    @Test
    fun updateCardWhenANewerVersionIsAvailable() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.1.0", "5.2.0")))

        assertEquals(WhatsNewCard.UpdateAvailable(release("5.2.0")), vm.cardOnceLoaded())
    }

    @Test
    fun updateCardOutranksTheHighlightsOfTheInstalledBuild() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(
            FakeReleaseNotesRepository(releases("5.1.0", "5.2.0")),
            settings(lastSeen = "5.0.0"),
        )

        assertEquals(WhatsNewCard.UpdateAvailable(release("5.2.0")), vm.cardOnceLoaded())
    }

    @Test
    fun highlightsAfterAnUpdate() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(
            FakeReleaseNotesRepository(releases("5.0.0", "5.1.0")),
            settings(lastSeen = "5.0.0"),
        )

        assertEquals(WhatsNewCard.Highlights(release("5.1.0")), vm.cardOnceLoaded())
    }

    @Test
    fun noHighlightsOnAFreshInstall() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // Installed at 5.1.0, never acknowledged anything: there is no "new" to speak of.
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.0.0", "5.1.0")))

        assertNull(vm.cardOnceLoaded())
    }

    @Test
    fun highlightsWhenCountingStartedOnAnOlderBuild() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(
            FakeReleaseNotesRepository(releases("5.0.0", "5.1.0")),
            settings(firstLaunchVersion = "5.0.0"),
        )

        assertEquals(WhatsNewCard.Highlights(release("5.1.0")), vm.cardOnceLoaded())
    }

    @Test
    fun noHighlightsOnceAcknowledged() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(
            FakeReleaseNotesRepository(releases("5.0.0", "5.1.0")),
            settings(lastSeen = "5.1.0"),
        )

        assertNull(vm.cardOnceLoaded())
    }

    @Test
    fun noHighlightsForAReleaseWithoutChanges() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(
            FakeReleaseNotesRepository(persistentListOf(release("5.1.0", changes = 0))),
            settings(lastSeen = "5.0.0"),
        )

        assertNull(vm.cardOnceLoaded())
    }

    @Test
    fun dismissingTheUpdateCardSilencesThatVersionAndOnlyThatVersion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val settings = settings(lastSeen = "5.0.0")
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.1.0", "5.2.0")), settings)
        val card = vm.cardOnceLoaded()
        assertEquals(WhatsNewCard.UpdateAvailable(release("5.2.0")), card)

        vm.onCardDismissed(card!!)

        // Gone for this session — the highlights of 5.1.0 do not slide in right behind it.
        assertNull(vm.state.value.card)
        assertEquals("5.2.0", settings.dismissedUpdateVersion)
        // Next launch: the update nudge stays down, so the unread 5.1.0 notes get their turn.
        val nextLaunch = viewModel(FakeReleaseNotesRepository(releases("5.1.0", "5.2.0")), settings)
        assertEquals(WhatsNewCard.Highlights(release("5.1.0")), nextLaunch.cardOnceLoaded())
        // A later release re-arms the update card: the marker names a version, it is not a boolean.
        val afterNextRelease = viewModel(FakeReleaseNotesRepository(releases("5.1.0", "5.2.0", "5.3.0")), settings)
        assertEquals(WhatsNewCard.UpdateAvailable(release("5.3.0")), afterNextRelease.cardOnceLoaded())
    }

    @Test
    fun openingTheHighlightsAcknowledgesTheInstalledVersion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val settings = settings(lastSeen = "5.0.0")
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.0.0", "5.1.0")), settings)
        val card = vm.cardOnceLoaded()

        vm.onCardOpened(card!!)

        assertNull(vm.state.value.card)
        assertEquals("5.1.0", settings.lastSeenVersion)
        assertEquals("highlights", tracker.named("whats_new_card_opened").single().params["kind"])
    }

    @Test
    fun theCardIsReportedShownOnce() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.1.0", "5.2.0")))
        val card = vm.cardOnceLoaded()!!

        vm.onCardShown(card)
        vm.onCardShown(card)

        val shown = tracker.named("whats_new_card_shown")
        assertEquals(1, shown.size)
        assertEquals(mapOf("kind" to "update", "version" to "5.2.0"), shown.single().params)
    }

    @Test
    fun noCardOnDesktopWhereTheVersionIsUnresolved() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.1.0", "5.2.0")))
        // KoinInit.desktop.kt never resolves a real version, so BuildInfo.versionName is always the
        // literal "unknown" there — that must not read as 0.0.0 and turn every release into an update.
        BuildInfo.init(versionName = "unknown", isDebug = false, packageName = "test")

        assertNull(vm.cardOnceLoaded())
    }

    @Test
    fun comparesVersionsNumericallyNotLexically() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.10.0")))
        // Lexical comparison would put "5.9.0" ahead of "5.10.0" and wrongly treat 5.10.0 as older.
        BuildInfo.init(versionName = "5.9.0", isDebug = true, packageName = "test")

        assertEquals(WhatsNewCard.UpdateAvailable(release("5.10.0")), vm.cardOnceLoaded())
    }

    @Test
    fun noCardWhileTheNotesAreStillLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeReleaseNotesRepository(releases("5.1.0", "5.2.0"), CompletableDeferred()))

        assertTrue(vm.state.value.isLoading)
        assertNull(vm.state.value.card)
    }
}
