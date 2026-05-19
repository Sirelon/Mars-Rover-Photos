package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.platform.FirebaseAnalytics

class AboutViewModel(
    private val appSettings: AppSettings,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    val themeFlow = appSettings.themeFlow
    val showFactsFlow = appSettings.showFactsFlow

    fun setTheme(theme: Theme) {
        appSettings.theme = theme
        analytics.logEvent("change_theme_$theme", emptyMap())
    }

    fun toggleFacts(enabled: Boolean) {
        appSettings.showFacts = enabled
        analytics.logEvent("toggle_facts_$enabled", emptyMap())
    }
}
