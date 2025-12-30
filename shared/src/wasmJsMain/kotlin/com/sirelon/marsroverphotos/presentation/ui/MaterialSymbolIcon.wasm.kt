package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Web implementation: Use default font family as fallback.
 * TODO: Load Material Symbols font from Google Fonts or bundle it.
 *
 * To implement:
 * 1. Add Material Symbols font link to HTML head:
 *    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined">
 * 2. Or bundle the font file and load it via CSS @font-face
 */
@Composable
actual fun getMaterialSymbolsFont(filled: Boolean, weight: Int): FontFamily {
    // For now, return default font family
    // Material Symbols should be loaded via Google Fonts or bundled
    return FontFamily.Default
}
