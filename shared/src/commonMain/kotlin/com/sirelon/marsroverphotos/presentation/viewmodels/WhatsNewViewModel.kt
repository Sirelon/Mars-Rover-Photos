package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.releasenotes.RELEASES
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.PushNotifications
import com.sirelon.marsroverphotos.platform.PushPermissionStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WhatsNewUiState(
    val releases: ImmutableList<Release>,
    val currentRelease: Release?,
)

class WhatsNewViewModel(
    private val appSettings: AppSettings,
    private val pushNotifications: PushNotifications,
) : ViewModel() {

    private val _showPushOptIn = MutableStateFlow(false)

    /**
     * Whether the dialog offers to turn on notifications.
     *
     * Only while the OS prompt is still available — once it has been used, the answer is settled
     * and About is the place to change it. This is the app's primary opt-in surface: the dialog
     * already reaches everyone who just updated, which is exactly the audience for release
     * notifications, and it costs no extra prompt because the OS is only asked on tap.
     */
    val showPushOptIn: StateFlow<Boolean> = _showPushOptIn.asStateFlow()

    init {
        viewModelScope.launch {
            _showPushOptIn.value =
                pushNotifications.permissionStatus() == PushPermissionStatus.NotDetermined
        }
    }

    /** Requests notification permission and subscribes on success. Hides the row either way. */
    fun enablePushNotifications() {
        viewModelScope.launch {
            _showPushOptIn.value = false
            val granted = pushNotifications.requestPermission() == PushPermissionStatus.Granted
            appSettings.notificationsEnabled = granted
            if (granted) pushNotifications.setSubscribed(true)
        }
    }

    private val _state = MutableStateFlow(
        WhatsNewUiState(
            releases = RELEASES,
            currentRelease = RELEASES.firstOrNull { it.version == BuildInfo.versionName },
        )
    )
    val state: StateFlow<WhatsNewUiState> = _state.asStateFlow()

    /** The release matching [version], or null when this build ships no notes for it. */
    fun releaseFor(version: String): Release? =
        _state.value.releases.firstOrNull { it.version == version }

    /**
     * Whether the What's New dialog should open on this launch.
     *
     * Deliberately a function reading [AppSettings] live rather than a flag cached in [state]:
     * the dialog is shown from the root composable but acknowledged from the dialog's own nav
     * entry, and Nav3 gives each entry its own `ViewModelStore` — so the two call sites resolve
     * different [WhatsNewViewModel] instances. Only the persisted marker is shared between them,
     * so that is what the decision has to read. A cached flag would still say "show" on the root
     * instance after the entry-scoped instance recorded the dialog as seen (e.g. after rotation).
     *
     * Gated on [WhatsNewUiState.currentRelease] as well as the marker: a build with no matching
     * `RELEASES` entry (a `./gradlew bumpVersion` with the notes not yet written, or Desktop's
     * `"unknown"` version) would otherwise push a dialog that renders nothing and swallows a back
     * press on every launch.
     *
     * A missing marker counts as "show": it can't be told apart from a first run after this
     * feature shipped, and skipping it there would silently exclude every pre-existing user —
     * the whole audience for the dialog — permanently. A brand-new install seeing the current
     * release's highlights once is the cheaper of the two mistakes.
     */
    fun shouldShowDialog(): Boolean =
        _state.value.currentRelease != null && appSettings.lastSeenVersion != BuildInfo.versionName

    /** Records this version as acknowledged, so [shouldShowDialog] stays false until the next update. */
    fun markSeen() {
        appSettings.lastSeenVersion = BuildInfo.versionName
    }
}
