package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * iOS implementation: Use default font family as fallback.
 * TODO: Bundle Material Symbols font for iOS and load it properly.
 *
 * To implement:
 * 1. Add material_symbols_outlined.ttf to iOS bundle
 * 2. Register font in Info.plist
 * 3. Load font using UIFont or CoreText
 */
@Composable
actual fun getMaterialSymbolsFont(filled: Boolean, weight: Int): FontFamily {
    // For now, return default font family
    // Material Symbols font should be bundled and loaded for iOS
    return FontFamily.Default
}
