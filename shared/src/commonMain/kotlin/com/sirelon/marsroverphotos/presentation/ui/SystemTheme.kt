package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

/**
 * Returns whether the system is in dark theme.
 * Platform-specific implementation.
 */
@Composable
expect fun isSystemInDarkTheme(): Boolean
