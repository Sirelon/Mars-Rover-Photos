package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Desktop implementation: Detect system dark mode via system property.
 * Checks for dark appearance using system properties or defaults to false.
 */
@Composable
@ReadOnlyComposable
actual fun isSystemInDarkTheme(): Boolean {
    // Try to detect dark mode from system properties
    // This works on macOS and some Linux desktops
    return try {
        val appearance = System.getProperty("apple.awt.application.appearance")
        appearance?.contains("dark", ignoreCase = true) == true
    } catch (e: Exception) {
        // Default to light theme if detection fails
        false
    }
}

/**
 * Desktop implementation: No dynamic color support.
 */
@Composable
@ReadOnlyComposable
actual fun supportsDynamicColor(): Boolean {
    return false
}

/**
 * Desktop implementation: Return static dark color scheme.
 */
@Composable
@ReadOnlyComposable
actual fun getDynamicDarkColorScheme(): ColorScheme {
    return DarkColorPalette
}

/**
 * Desktop implementation: Return static light color scheme.
 */
@Composable
@ReadOnlyComposable
actual fun getDynamicLightColorScheme(): ColorScheme {
    return LightColorPalette
}
