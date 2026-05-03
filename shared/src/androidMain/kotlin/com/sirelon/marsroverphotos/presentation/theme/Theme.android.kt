package com.sirelon.marsroverphotos.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme as androidIsSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation: Use system dark theme preference.
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return androidIsSystemInDarkTheme()
}

/**
 * Android implementation: Dynamic colors supported on Android 12+ (API 31+).
 */
@Composable
actual fun supportsDynamicColor(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * Android implementation: Get dynamic dark color scheme from system.
 */
@Composable
actual fun getDynamicDarkColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        DarkColorPalette
    }
}

/**
 * Android implementation: Get dynamic light color scheme from system.
 */
@Composable
actual fun getDynamicLightColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        LightColorPalette
    }
}
