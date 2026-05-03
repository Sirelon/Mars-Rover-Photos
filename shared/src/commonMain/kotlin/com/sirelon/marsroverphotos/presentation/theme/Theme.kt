package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Brand colors for Mars Rover Photos app.
 */
val primary = Color(0xFF385C8A)
val primaryDark = Color(0xFF6200EE)
val primaryVariant = Color(0xFF1A283D)
val accent = Color(0xFFFC6C4B)

/**
 * Dark color scheme for the app.
 */
val DarkColorPalette = darkColorScheme(
    primary = primary,
    secondary = accent,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    error = Color(0xFFCF6679),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

/**
 * Light color scheme for the app.
 */
val LightColorPalette = lightColorScheme(
    primary = primary,
    secondary = accent,
    background = Color.White,
    surface = Color.White,
    error = Color(0xFFB00020),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White
)

/**
 * Main theme composable for Mars Rover Photos app.
 * Supports dark/light themes with platform-specific dynamic color support.
 *
 * @param darkTheme Whether to use dark theme (defaults to system preference)
 * @param dynamicColor Whether to use dynamic colors (Android 12+)
 * @param content The content to display within the theme
 */
@Composable
fun MarsRoverPhotosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        // Dynamic color is available on Android 12+ (API 31+)
        dynamicColor && supportsDynamicColor() -> {
            if (darkTheme) {
                getDynamicDarkColorScheme()
            } else {
                getDynamicLightColorScheme()
            }
        }
        // Fall back to static color schemes
        darkTheme -> DarkColorPalette
        else -> LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

/**
 * Check if the current system is in dark theme.
 * Platform-specific implementation.
 */
@Composable
expect fun isSystemInDarkTheme(): Boolean

/**
 * Check if the platform supports dynamic colors (Material You).
 * Returns true for Android 12+ (API 31+), false for other platforms.
 */
@Composable
expect fun supportsDynamicColor(): Boolean

/**
 * Get dynamic dark color scheme from system (Android 12+).
 * Returns static dark scheme for platforms without dynamic color support.
 */
@Composable
expect fun getDynamicDarkColorScheme(): ColorScheme

/**
 * Get dynamic light color scheme from system (Android 12+).
 * Returns static light scheme for platforms without dynamic color support.
 */
@Composable
expect fun getDynamicLightColorScheme(): ColorScheme
