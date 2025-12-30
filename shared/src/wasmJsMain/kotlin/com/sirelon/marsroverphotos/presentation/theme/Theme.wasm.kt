package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import kotlinx.browser.window

/**
 * Web implementation: Detect system dark mode via media query.
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return try {
        window.matchMedia("(prefers-color-scheme: dark)").matches
    } catch (e: Exception) {
        // Default to light theme if detection fails
        false
    }
}

/**
 * Web implementation: No dynamic color support.
 */
@Composable
actual fun supportsDynamicColor(): Boolean {
    return false
}

/**
 * Web implementation: Return static dark color scheme.
 */
@Composable
actual fun getDynamicDarkColorScheme(): ColorScheme {
    return DarkColorPalette
}

/**
 * Web implementation: Return static light color scheme.
 */
@Composable
actual fun getDynamicLightColorScheme(): ColorScheme {
    return LightColorPalette
}
