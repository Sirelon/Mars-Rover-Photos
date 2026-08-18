package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.platform.FirebaseAnalytics
import com.sirelon.marsroverphotos.platform.PushNotifications
import com.sirelon.marsroverphotos.platform.PushPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutViewModel(
    private val appSettings: AppSettings,
    private val analytics: FirebaseAnalytics,
    private val pushNotifications: PushNotifications,
) : ViewModel() {

    val themeFlow = appSettings.themeFlow
    val showFactsFlow = appSettings.showFactsFlow
    val notificationsEnabledFlow = appSettings.notificationsEnabledFlow

    private val _pushStatus = MutableStateFlow(PushPermissionStatus.NotDetermined)

    /**
     * OS authorization, kept here rather than in [PushNotifications] because it can change while
     * the app is backgrounded — the user can revoke it in system settings at any time, and only a
     * re-query on screen entry notices.
     */
    val pushStatus: StateFlow<PushPermissionStatus> = _pushStatus.asStateFlow()

    init {
        refreshPushStatus()
    }

    fun setTheme(theme: Theme) {
        appSettings.theme = theme
        analytics.logEvent("change_theme_$theme", emptyMap())
    }

    fun toggleFacts(enabled: Boolean) {
        appSettings.showFacts = enabled
        analytics.logEvent("toggle_facts_$enabled", emptyMap())
    }

    fun refreshPushStatus() {
        viewModelScope.launch {
            val status = pushNotifications.permissionStatus()
            _pushStatus.value = status
            // Permission can be revoked from system settings without the app being told. Drop the
            // stale opt-in so the launch-time re-subscribe stops renewing a subscription the OS
            // will never display.
            if (status == PushPermissionStatus.Denied && appSettings.notificationsEnabled) {
                appSettings.notificationsEnabled = false
                pushNotifications.setSubscribed(false)
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (_pushStatus.value == PushPermissionStatus.NotDetermined) {
                    analytics.logEvent("push_permission_prompted", emptyMap())
                }
                val status = pushNotifications.requestPermission()
                _pushStatus.value = status
                val granted = status == PushPermissionStatus.Granted
                // Only mirror a grant into settings: leaving the flag false on refusal keeps the
                // switch and the OS in agreement instead of showing an opt-in that can't deliver.
                appSettings.notificationsEnabled = granted
                if (granted) pushNotifications.setSubscribed(true)
                analytics.logEvent(
                    if (granted) "push_permission_granted" else "push_permission_denied",
                    emptyMap(),
                )
            } else {
                appSettings.notificationsEnabled = false
                pushNotifications.setSubscribed(false)
                analytics.logEvent("push_disabled", emptyMap())
            }
        }
    }

    fun openNotificationSettings() {
        pushNotifications.openSystemSettings()
    }
}
