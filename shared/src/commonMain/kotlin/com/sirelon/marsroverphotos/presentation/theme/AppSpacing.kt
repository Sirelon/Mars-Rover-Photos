package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mars Rover Photos spacing scale — 8dp grid.
 *
 * Token → dp mapping follows the CSS design tokens in colors_and_type.css:
 *   xs  =  4dp  (--space-1) — icon inner padding, tiny gaps
 *   sm  =  8dp  (--space-2) — component gaps, tile inner padding
 *   md  = 12dp  (--space-3) — content horizontal padding in grid
 *   lg  = 16dp  (--space-4) — card padding, screen horizontal padding
 *   xl  = 24dp  (--space-5) — section gap
 *   xxl = 32dp  (--space-6) — empty-state padding
 *   x3l = 48dp  (--space-7) — large layout gaps
 */
object AppSpacing {
    /** 4dp — icon padding, hairline gaps. */
    val xs: Dp = 4.dp

    /** 8dp — between items in a row/column, tile inner padding. */
    val sm: Dp = 8.dp

    /** 12dp — horizontal content padding in photo grid. */
    val md: Dp = 12.dp

    /** 16dp — card padding, screen horizontal margin. */
    val lg: Dp = 16.dp

    /** 24dp — between sections on a content screen. */
    val xl: Dp = 24.dp

    /** 32dp — large breathing room, empty-state padding. */
    val xxl: Dp = 32.dp

    /** 48dp — hero / illustration vertical clearance. */
    val x3l: Dp = 48.dp
}
