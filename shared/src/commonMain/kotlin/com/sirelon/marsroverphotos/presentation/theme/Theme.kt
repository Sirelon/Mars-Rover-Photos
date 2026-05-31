package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Brand colors for Mars Rover Photos app.
 */
val primary = Color(0xFF385C8A)
val primaryDark = Color(0xFF6200EE)
val primaryVariant = Color(0xFF1A283D)
val accent = Color(0xFFFC6C4B)

/** Lighter blue used for section headers and rover titles in Mission Info. */
val primaryLight = Color(0xFF7FA6D8)

/** Navy used for the Fun Facts card background. */
val primaryNavy = Color(0xFF1A283D)

/**
 * Dark color scheme for the app.
 */
val DarkColorPalette = darkColorScheme(
    primary = primary,
    onPrimary = Color.Black,
    primaryContainer = primaryNavy,        // Fun Facts card background
    onPrimaryContainer = Color.White,
    secondary = accent,                    // coral — rover names, captions, FAB, selected nav
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A3A4D), // "Did You Know?" fact card
    onSecondaryContainer = Color(0xFFDCE6F2),
    tertiary = primaryLight,               // lighter blue — section headers, Mission Info titles
    onTertiary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    error = Color(0xFFCF6679),
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black,
)

/**
 * Light color scheme for the app.
 */
val LightColorPalette = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E3F5),
    onPrimaryContainer = Color(0xFF1A283D),
    secondary = accent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE4ECF6),
    onSecondaryContainer = Color(0xFF1A283D),
    tertiary = primary,
    onTertiary = Color.White,
    background = Color.White,
    surface = Color.White,
    error = Color(0xFFB00020),
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White,
)

/**
 * Overlays brand-critical color slots onto any [ColorScheme].
 *
 * Dynamic color (Material You) replaces the full palette with wallpaper-derived
 * colors, which would override the design-system tokens for secondary (coral),
 * secondaryContainer, tertiary, and primaryContainer. This function restores
 * those slots so branded components always render correctly.
 */
private fun ColorScheme.withBrandOverrides(darkTheme: Boolean): ColorScheme = if (darkTheme) {
    copy(
        secondary = accent,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF2A3A4D),
        onSecondaryContainer = Color(0xFFDCE6F2),
        tertiary = primaryLight,
        onTertiary = Color.Black,
        primaryContainer = primaryNavy,
        onPrimaryContainer = Color.White,
    )
} else {
    copy(
        secondary = accent,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFFE4ECF6),
        onSecondaryContainer = Color(0xFF1A283D),
        tertiary = primary,
        onTertiary = Color.White,
        primaryContainer = Color(0xFFD4E3F5),
        onPrimaryContainer = Color(0xFF1A283D),
    )
}

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
        // Dynamic color (Material You) on Android 12+: use wallpaper-derived scheme
        // but restore brand-critical slots so design-system components are consistent.
        dynamicColor && supportsDynamicColor() -> {
            if (darkTheme) {
                getDynamicDarkColorScheme().withBrandOverrides(darkTheme = true)
            } else {
                getDynamicLightColorScheme().withBrandOverrides(darkTheme = false)
            }
        }
        // Fall back to fully static brand palettes
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
@ReadOnlyComposable
expect fun isSystemInDarkTheme(): Boolean

/**
 * Check if the platform supports dynamic colors (Material You).
 * Returns true for Android 12+ (API 31+), false for other platforms.
 */
@Composable
@ReadOnlyComposable
expect fun supportsDynamicColor(): Boolean

/**
 * Get dynamic dark color scheme from system (Android 12+).
 * Returns static dark scheme for platforms without dynamic color support.
 */
@Composable
@ReadOnlyComposable
expect fun getDynamicDarkColorScheme(): ColorScheme

/**
 * Get dynamic light color scheme from system (Android 12+).
 * Returns static light scheme for platforms without dynamic color support.
 */
@Composable
@ReadOnlyComposable
expect fun getDynamicLightColorScheme(): ColorScheme
