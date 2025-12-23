package com.sirelon.marsroverphotos.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Calculates the number of grid columns based on screen width.
 * - Phones portrait: 2 columns
 * - Phones landscape: 3-4 columns
 * - Tablets portrait: 3-4 columns
 * - Tablets landscape: 4-6 columns
 */
@Composable
fun rememberAdaptiveGridColumns(minColumnWidth: Dp = 160.dp): GridCells {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // Calculate columns based on minimum column width
    val columns = (screenWidthDp / minColumnWidth).toInt().coerceAtLeast(2)

    return GridCells.Fixed(columns)
}

/**
 * Returns the number of columns as an Int for use with Adaptive grid cells.
 * Useful for LazyVerticalGrid with GridCells.Adaptive.
 */
@Composable
fun calculateAdaptiveColumns(minColumnWidth: Dp = 160.dp): Int {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    return (screenWidthDp / minColumnWidth).toInt().coerceAtLeast(2)
}

/**
 * Determines if the device is in landscape mode.
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

/**
 * Returns an adaptive grid cells based on minimum column width.
 * This allows the grid to automatically adjust columns based on available space.
 */
@Composable
fun adaptiveGridCells(minColumnWidth: Dp = 160.dp): GridCells {
    return GridCells.Adaptive(minColumnWidth)
}
