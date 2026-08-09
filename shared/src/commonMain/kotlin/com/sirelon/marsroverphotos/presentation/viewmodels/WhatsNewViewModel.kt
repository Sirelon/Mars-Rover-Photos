package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.sirelon.marsroverphotos.domain.releasenotes.RELEASES
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WhatsNewUiState(
    val releases: ImmutableList<Release>,
    val currentRelease: Release?,
    val shouldShowDialog: Boolean,
)

class WhatsNewViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow(computeInitialState())
    val state: StateFlow<WhatsNewUiState> = _state.asStateFlow()

    private fun computeInitialState(): WhatsNewUiState {
        val releases = RELEASES
        val lastSeen = appSettings.lastSeenVersion
        val shouldShowDialog = when {
            lastSeen == null -> {
                // Fresh install — record version silently, no dialog.
                appSettings.lastSeenVersion = BuildInfo.versionName
                false
            }
            lastSeen != BuildInfo.versionName -> true
            else -> false
        }
        return WhatsNewUiState(
            releases = releases,
            currentRelease = releases.firstOrNull { it.version == BuildInfo.versionName },
            shouldShowDialog = shouldShowDialog,
        )
    }

    fun markSeen() {
        appSettings.lastSeenVersion = BuildInfo.versionName
        _state.update { it.copy(shouldShowDialog = false) }
    }
}
