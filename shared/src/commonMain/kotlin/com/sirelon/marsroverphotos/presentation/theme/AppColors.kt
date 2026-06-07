package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Theme-aware "active / live / connected" status green.
 *
 * M3 has no green slot, so this color is resolved per applied theme via background luminance —
 * keeping it legible in both dark and light themes and matching the design's `--t-active` token
 * (#5BBF86 dark / #2E9E63 light). Use wherever an "active" or "live" state needs a green accent
 * instead of a literal color.
 */
@Composable
@ReadOnlyComposable
fun activeStatusColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF5BBF86) else Color(0xFF2E9E63)
