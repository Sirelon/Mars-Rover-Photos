package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Calculates the number of grid columns based on screen width.
 * - Phones portrait: 2 columns
 * - Phones landscape: 3-4 columns
 * - Tablets portrait: 3-4 columns
 * - Tablets landscape: 4-6 columns
 *
 * @param minColumnWidth Minimum width for each column (default 160.dp)
 * @return GridCells configuration for adaptive grid
 */
@Composable
fun rememberAdaptiveGridColumns(minColumnWidth: Dp = 160.dp): GridCells {
    val screenWidthDp = getScreenWidthDp()

    // Calculate columns based on minimum column width
    val columns = (screenWidthDp / minColumnWidth.value).toInt().coerceAtLeast(2)

    return GridCells.Fixed(columns)
}

/**
 * Returns the number of columns as an Int for use with adaptive grid cells.
 * Useful for LazyVerticalGrid with GridCells.Adaptive.
 *
 * @param minColumnWidth Minimum width for each column (default 160.dp)
 * @return Number of columns to display
 */
@Composable
fun calculateAdaptiveColumns(minColumnWidth: Dp = 160.dp): Int {
    val screenWidthDp = getScreenWidthDp()
    return (screenWidthDp / minColumnWidth.value).toInt().coerceAtLeast(2)
}

/**
 * Determines if the device is in landscape mode.
 *
 * @return True if in landscape orientation, false otherwise
 */
@Composable
fun isLandscape(): Boolean {
    val screenWidthDp = getScreenWidthDp()
    val screenHeightDp = getScreenHeightDp()
    return screenWidthDp > screenHeightDp
}

/**
 * Returns an adaptive grid cells based on minimum column width.
 * This allows the grid to automatically adjust columns based on available space.
 *
 * @param minColumnWidth Minimum width for each column (default 160.dp)
 * @return GridCells.Adaptive configuration
 */
@Composable
fun adaptiveGridCells(minColumnWidth: Dp = 160.dp): GridCells {
    return GridCells.Adaptive(minColumnWidth)
}

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
