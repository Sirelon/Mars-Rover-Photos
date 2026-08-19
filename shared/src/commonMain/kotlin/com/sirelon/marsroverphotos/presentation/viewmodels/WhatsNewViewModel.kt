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
    val currentRelease: Release? = null,
    val isLoading: Boolean = true,
)

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
                    currentRelease = releases.firstOrNull { release -> release.version == BuildInfo.versionName },
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
     * Gated on [WhatsNewUiState.currentRelease] as well as the marker: a build with no matching
     * release (a `./gradlew bumpVersion` with the notes not yet published, or Desktop's `"unknown"`
     * version) would otherwise push a dialog that renders nothing and swallows a back press on every
     * launch.
     *
     * A missing marker counts as "show": it can't be told apart from a first run after this
     * feature shipped, and skipping it there would silently exclude every pre-existing user —
     * the whole audience for the dialog — permanently. A brand-new install seeing the current
     * release's highlights once is the cheaper of the two mistakes.
     */
    suspend fun shouldShowDialog(): Boolean {
        val loaded = withTimeoutOrNull(DIALOG_LOAD_WAIT_MS) {
            state.first { !it.isLoading }
        } ?: return false
        return loaded.currentRelease != null && appSettings.lastSeenVersion != BuildInfo.versionName
    }

    /** Records this version as acknowledged, so [shouldShowDialog] stays false until the next update. */
    fun markSeen() {
        appSettings.lastSeenVersion = BuildInfo.versionName
    }
}
