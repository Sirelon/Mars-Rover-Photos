package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

@Composable
actual fun isSystemInDarkTheme(): Boolean {
    // Web: Check browser's dark mode preference
    // Could use window.matchMedia('(prefers-color-scheme: dark)') via JS interop
    return false  // Default to light theme for now
}
