package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Desktop implementation: Use default font family as fallback.
 * TODO: Bundle Material Symbols font for Desktop and load it properly.
 *
 * To implement:
 * 1. Add material_symbols_outlined.ttf to resources
 * 2. Load font using java.awt.Font or Compose font loading
 */
@Composable
actual fun getMaterialSymbolsFont(filled: Boolean, weight: Int): FontFamily {
    // For now, return default font family
    // Material Symbols font should be bundled and loaded for Desktop
    return FontFamily.Default
}
