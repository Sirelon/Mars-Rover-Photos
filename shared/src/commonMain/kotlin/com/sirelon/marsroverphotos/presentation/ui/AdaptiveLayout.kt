package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Get the screen width in dp for the current platform.
 * Platform-specific implementation.
 */
@Composable
expect fun getScreenWidthDp(): Float

/**
 * Get the screen height in dp for the current platform.
 * Platform-specific implementation.
 */
@Composable
expect fun getScreenHeightDp(): Float
