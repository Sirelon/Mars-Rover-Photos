package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.domain.repositories.ReleaseNotesRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class WhatsNewUiState(
    val releases: ImmutableList<Release> = persistentListOf(),
    val latestRelease: Release? = null,
    val isLoading: Boolean = true,
)

/** Numeric `major.minor.patch` comparison — versions in this repo are always that shape (see AGENTS.md › Versioning), so a plain dotted split is enough; no need for a general semver parser. */
private fun compareVersions(a: String, b: String): Int {
    val partsA = a.split(".")
    val partsB = b.split(".")
    for (i in 0 until maxOf(partsA.size, partsB.size)) {
        val cmp = (partsA.getOrNull(i)?.toIntOrNull() ?: 0).compareTo(partsB.getOrNull(i)?.toIntOrNull() ?: 0)
        if (cmp != 0) return cmp
    }
    return 0
}

class WhatsNewViewModel(
    private val releaseNotesRepository: ReleaseNotesRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    private companion object {
        /**
         * How long [shouldShowDialog] waits for the notes before giving up on this launch.
         *
         * Bounded on purpose. Unbounded, a slow network would drop a modal dialog on top of content
         * the user has already started reading. Timing out costs only this launch: the acknowledged
         * marker is left alone, so the next launch — served from Firestore's local cache, effectively
         * instantly — shows the dialog instead.
         */
        const val DIALOG_LOAD_WAIT_MS = 3_000L
    }

    private val _state = MutableStateFlow(WhatsNewUiState())
    val state: StateFlow<WhatsNewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val releases = releaseNotesRepository.getReleases()
            _state.update {
                it.copy(
                    releases = releases,
                    // The repository already dropped anything not "available" in the store, so the
                    // highest version left here is always something the user could actually update
                    // to — never a build pending approval.
                    latestRelease = releases.maxWithOrNull { a, b -> compareVersions(a.version, b.version) },
                    // Also cleared on the failure path — the repository returns an empty list rather
                    // than throwing, so nothing can leave this stuck loading forever.
                    isLoading = false,
                )
            }
        }
    }

    /** The release matching [version], or null when the notes carry nothing for it. */
    fun releaseFor(version: String): Release? =
        _state.value.releases.firstOrNull { it.version == version }

    /**
     * Whether the What's New dialog should open on this launch.
     *
     * Suspends until the notes have loaded (bounded by [DIALOG_LOAD_WAIT_MS]) because they come from
     * Firestore — there is nothing to decide on before the fetch lands.
     *
     * Deliberately a function reading [AppSettings] live rather than a flag cached in [state]:
     * the dialog is shown from the root composable but acknowledged from the dialog's own nav
     * entry, and Nav3 gives each entry its own `ViewModelStore` — so the two call sites resolve
     * different [WhatsNewViewModel] instances. Only the persisted marker is shared between them,
     * so that is what the decision has to read. A cached flag would still say "show" on the root
     * instance after the entry-scoped instance recorded the dialog as seen (e.g. after rotation).
     *
     * Shows when [WhatsNewUiState.latestRelease] is a newer version than the running build — i.e.
     * there is a released, store-approved update the user has not installed yet — and that version's
     * nudge has not already been dismissed. This is deliberately not "does a release note exist for
     * the version I'm running": that would only ever fire right after an update, never for a user
     * sitting on an old build who should be told to go get the new one.
     */
    suspend fun shouldShowDialog(): Boolean {
        val loaded = withTimeoutOrNull(DIALOG_LOAD_WAIT_MS) {
            state.first { !it.isLoading }
        } ?: return false
        val latest = loaded.latestRelease ?: return false
        // Desktop never resolves a real version (KoinInit.desktop.kt reads an "app.version" system
        // property nothing ever sets), so BuildInfo.versionName is always the literal "unknown"
        // there. compareVersions would otherwise read every one of its segments as 0 and treat any
        // published release as newer, popping the dialog on every launch with an "Update" button
        // that has nowhere sensible to send a desktop user.
        if (BuildInfo.versionName.substringBefore('.').toIntOrNull() == null) return false
        return compareVersions(latest.version, BuildInfo.versionName) > 0 &&
            appSettings.lastSeenVersion != latest.version
    }

    /**
     * Records the current [WhatsNewUiState.latestRelease] as acknowledged, so [shouldShowDialog]
     * stays false until a newer version's notes are published.
     */
    fun markSeen() {
        appSettings.lastSeenVersion = _state.value.latestRelease?.version ?: BuildInfo.versionName
    }
}
