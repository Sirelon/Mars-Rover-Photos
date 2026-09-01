package com.sirelon.marsroverphotos.presentation.viewmodels

import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.domain.repositories.ReleaseNotesRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.PlatformPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** In-memory [PlatformPreferences]; only the string keys matter for the What's New marker. */
private class FakePreferences : PlatformPreferences {
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
 * The dialog gate has to wait for a network fetch before it can decide anything, which is the part
 * worth pinning down: it must not hang, and it must not burn the acknowledgement when it gives up.
 *
 * It also has to compare versions rather than match them exactly: the dialog now nudges a user
 * sitting on an old build toward the newest *store-approved* release, not just announce the notes
 * for whatever build they happen to already be running (the repository is what drops anything still
 * pending approval — see `ReleaseNotesRepositoryImpl`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WhatsNewGateTest {

    private val installedVersion = "5.0.0"

    private fun release(version: String) = Release(
        version = version,
        date = LocalDate(2026, 6, 29),
        changes = persistentListOf(
            Release.Change(id = "c", icon = "rocket_launch", title = "Mission Info", summary = "New"),
        ),
    )

    private fun viewModel(
        repository: ReleaseNotesRepository,
        settings: AppSettings = AppSettings(FakePreferences()),
    ): Pair<WhatsNewViewModel, AppSettings> {
        BuildInfo.init(versionName = installedVersion, isDebug = true, packageName = "test")
        return WhatsNewViewModel(repository, settings) to settings
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        // BuildInfo is a process-wide singleton and these tests write to it, so it has to be put
        // back — the desktop default other tests may rely on.
        BuildInfo.init(versionName = "unknown", isDebug = false, packageName = "com.sirelon.marsroverphotos")
    }

    @Test
    fun showsDialogWhenANewerVersionIsAvailable() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (vm, _) = viewModel(FakeReleaseNotesRepository(persistentListOf(release("5.1.0"))))

        assertTrue(vm.shouldShowDialog())
    }

    @Test
    fun doesNotShowDialogWhenAlreadyOnTheLatestVersion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (vm, _) = viewModel(FakeReleaseNotesRepository(persistentListOf(release(installedVersion))))

        assertFalse(vm.shouldShowDialog())
    }

    @Test
    fun doesNotShowDialogWhenTheInstalledVersionIsAlreadyAheadOfTheNotes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // A bumpVersion whose notes are not published yet, or Desktop's "unknown" version.
        val (vm, _) = viewModel(FakeReleaseNotesRepository(persistentListOf(release("4.0.0"))))

        assertFalse(vm.shouldShowDialog())
    }

    @Test
    fun doesNotShowDialogOnDesktopWhereTheVersionIsUnresolved() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // KoinInit.desktop.kt never resolves a real version, so BuildInfo.versionName is always the
        // literal "unknown" there — comparing that against a real version must not read it as 0.0.0
        // and treat every release as an update.
        val (vm, _) = viewModel(FakeReleaseNotesRepository(persistentListOf(release("5.1.0"))))
        BuildInfo.init(versionName = "unknown", isDebug = false, packageName = "test")

        assertFalse(vm.shouldShowDialog())
    }

    @Test
    fun comparesVersionsNumericallyNotLexically() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // Lexical comparison would put "5.9.0" ahead of "5.10.0" and wrongly treat 5.10.0 as older.
        val (vm, _) = viewModel(FakeReleaseNotesRepository(persistentListOf(release("5.10.0"))))
        BuildInfo.init(versionName = "5.9.0", isDebug = true, packageName = "test")

        assertTrue(vm.shouldShowDialog())
    }

    @Test
    fun doesNotShowDialogWhenAlreadyAcknowledged() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val settings = AppSettings(FakePreferences()).apply { lastSeenVersion = "5.1.0" }
        val (vm, _) = viewModel(FakeReleaseNotesRepository(persistentListOf(release("5.1.0"))), settings)

        assertFalse(vm.shouldShowDialog())
    }

    @Test
    fun givesUpWhenTheFetchOutlastsTheWaitAndLeavesTheMarkerAlone() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // A fetch that never lands: the gate must return rather than hang the launch...
        val (vm, settings) = viewModel(
            FakeReleaseNotesRepository(persistentListOf(release("5.1.0")), CompletableDeferred()),
        )

        assertFalse(vm.shouldShowDialog())
        // ...and must not record a version as seen, so the next launch still gets its chance.
        assertFalse(settings.lastSeenVersion == "5.1.0")
    }

    @Test
    fun markSeenSuppressesTheDialogForThisVersionOnly() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (vm, settings) = viewModel(FakeReleaseNotesRepository(persistentListOf(release("5.1.0"))))

        assertTrue(vm.shouldShowDialog())
        vm.markSeen()
        assertFalse(vm.shouldShowDialog())
        assertEquals("5.1.0", settings.lastSeenVersion)

        // A later release re-arms it: the marker is compared against the newest published version,
        // not stored as a boolean, so acknowledging 5.1.0 does not acknowledge 5.2.0. A second
        // instance over the same settings stands in for Nav3 handing the dialog's nav entry its own
        // WhatsNewViewModel, separate from the root's.
        val (vmAfterNextRelease, _) = viewModel(FakeReleaseNotesRepository(persistentListOf(release("5.2.0"))), settings)
        assertTrue(vmAfterNextRelease.shouldShowDialog())
    }
}
