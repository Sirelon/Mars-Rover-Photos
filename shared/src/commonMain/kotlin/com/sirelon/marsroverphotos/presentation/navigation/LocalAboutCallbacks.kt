package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.staticCompositionLocalOf

data class AboutCallbacks(
    val onRateApp: () -> Unit = {},
    val appVersion: String = ""
)

val LocalAboutCallbacks = staticCompositionLocalOf { AboutCallbacks() }
