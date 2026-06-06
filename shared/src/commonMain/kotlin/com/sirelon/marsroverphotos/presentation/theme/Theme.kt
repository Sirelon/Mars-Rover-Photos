package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
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
    val isDynamic = dynamicColor && supportsDynamicColor()
    val darkScheme = if (isDynamic) getDynamicDarkColorScheme().withBrandOverrides(darkTheme = true) else DarkColorPalette
    val lightScheme = if (isDynamic) getDynamicLightColorScheme().withBrandOverrides(darkTheme = false) else LightColorPalette

    MaterialTheme(
        colorScheme = animateColorScheme(darkTheme, darkScheme, lightScheme),
        content = content
    )
}

@Composable
private fun animateColorScheme(darkTheme: Boolean, dark: ColorScheme, light: ColorScheme): ColorScheme {
    val transition = updateTransition(darkTheme, label = "theme")
    val spec = tween<Color>(durationMillis = 400, easing = FastOutSlowInEasing)

    val primary by transition.animateColor({ spec }, "primary") { if (it) dark.primary else light.primary }
    val onPrimary by transition.animateColor({ spec }, "onPrimary") { if (it) dark.onPrimary else light.onPrimary }
    val primaryContainer by transition.animateColor({ spec }, "primaryContainer") { if (it) dark.primaryContainer else light.primaryContainer }
    val onPrimaryContainer by transition.animateColor({ spec }, "onPrimaryContainer") { if (it) dark.onPrimaryContainer else light.onPrimaryContainer }
    val inversePrimary by transition.animateColor({ spec }, "inversePrimary") { if (it) dark.inversePrimary else light.inversePrimary }
    val secondary by transition.animateColor({ spec }, "secondary") { if (it) dark.secondary else light.secondary }
    val onSecondary by transition.animateColor({ spec }, "onSecondary") { if (it) dark.onSecondary else light.onSecondary }
    val secondaryContainer by transition.animateColor({ spec }, "secondaryContainer") { if (it) dark.secondaryContainer else light.secondaryContainer }
    val onSecondaryContainer by transition.animateColor({ spec }, "onSecondaryContainer") { if (it) dark.onSecondaryContainer else light.onSecondaryContainer }
    val tertiary by transition.animateColor({ spec }, "tertiary") { if (it) dark.tertiary else light.tertiary }
    val onTertiary by transition.animateColor({ spec }, "onTertiary") { if (it) dark.onTertiary else light.onTertiary }
    val tertiaryContainer by transition.animateColor({ spec }, "tertiaryContainer") { if (it) dark.tertiaryContainer else light.tertiaryContainer }
    val onTertiaryContainer by transition.animateColor({ spec }, "onTertiaryContainer") { if (it) dark.onTertiaryContainer else light.onTertiaryContainer }
    val background by transition.animateColor({ spec }, "background") { if (it) dark.background else light.background }
    val onBackground by transition.animateColor({ spec }, "onBackground") { if (it) dark.onBackground else light.onBackground }
    val surface by transition.animateColor({ spec }, "surface") { if (it) dark.surface else light.surface }
    val onSurface by transition.animateColor({ spec }, "onSurface") { if (it) dark.onSurface else light.onSurface }
    val surfaceVariant by transition.animateColor({ spec }, "surfaceVariant") { if (it) dark.surfaceVariant else light.surfaceVariant }
    val onSurfaceVariant by transition.animateColor({ spec }, "onSurfaceVariant") { if (it) dark.onSurfaceVariant else light.onSurfaceVariant }
    val surfaceTint by transition.animateColor({ spec }, "surfaceTint") { if (it) dark.surfaceTint else light.surfaceTint }
    val inverseSurface by transition.animateColor({ spec }, "inverseSurface") { if (it) dark.inverseSurface else light.inverseSurface }
    val inverseOnSurface by transition.animateColor({ spec }, "inverseOnSurface") { if (it) dark.inverseOnSurface else light.inverseOnSurface }
    val error by transition.animateColor({ spec }, "error") { if (it) dark.error else light.error }
    val onError by transition.animateColor({ spec }, "onError") { if (it) dark.onError else light.onError }
    val errorContainer by transition.animateColor({ spec }, "errorContainer") { if (it) dark.errorContainer else light.errorContainer }
    val onErrorContainer by transition.animateColor({ spec }, "onErrorContainer") { if (it) dark.onErrorContainer else light.onErrorContainer }
    val outline by transition.animateColor({ spec }, "outline") { if (it) dark.outline else light.outline }
    val outlineVariant by transition.animateColor({ spec }, "outlineVariant") { if (it) dark.outlineVariant else light.outlineVariant }
    val scrim by transition.animateColor({ spec }, "scrim") { if (it) dark.scrim else light.scrim }
    val surfaceBright by transition.animateColor({ spec }, "surfaceBright") { if (it) dark.surfaceBright else light.surfaceBright }
    val surfaceDim by transition.animateColor({ spec }, "surfaceDim") { if (it) dark.surfaceDim else light.surfaceDim }
    val surfaceContainer by transition.animateColor({ spec }, "surfaceContainer") { if (it) dark.surfaceContainer else light.surfaceContainer }
    val surfaceContainerHigh by transition.animateColor({ spec }, "surfaceContainerHigh") { if (it) dark.surfaceContainerHigh else light.surfaceContainerHigh }
    val surfaceContainerHighest by transition.animateColor({ spec }, "surfaceContainerHighest") { if (it) dark.surfaceContainerHighest else light.surfaceContainerHighest }
    val surfaceContainerLow by transition.animateColor({ spec }, "surfaceContainerLow") { if (it) dark.surfaceContainerLow else light.surfaceContainerLow }
    val surfaceContainerLowest by transition.animateColor({ spec }, "surfaceContainerLowest") { if (it) dark.surfaceContainerLowest else light.surfaceContainerLowest }

    return dark.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
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
