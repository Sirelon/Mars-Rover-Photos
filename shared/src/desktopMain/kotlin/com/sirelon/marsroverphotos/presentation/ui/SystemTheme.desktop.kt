package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

@Composable
actual fun isSystemInDarkTheme(): Boolean {
    // Desktop: Use system theme detection
    // This is a simplified version - could be enhanced with JNA to detect OS theme
    return false  // Default to light theme
}
