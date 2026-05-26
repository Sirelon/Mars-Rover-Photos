package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

/**
 * iOS implementation: Detect system dark mode.
 */
@Composable
@ReadOnlyComposable
actual fun isSystemInDarkTheme(): Boolean {
    return UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
}

/**
 * iOS implementation: No dynamic color support.
 */
@Composable
@ReadOnlyComposable
actual fun supportsDynamicColor(): Boolean {
    return false
}

/**
 * iOS implementation: Return static dark color scheme.
 */
@Composable
@ReadOnlyComposable
actual fun getDynamicDarkColorScheme(): ColorScheme {
    return DarkColorPalette
}

/**
 * iOS implementation: Return static light color scheme.
 */
@Composable
@ReadOnlyComposable
actual fun getDynamicLightColorScheme(): ColorScheme {
    return LightColorPalette
}
