package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mars Rover Photos size scale — component dimensions and corner radii.
 *
 * Distinct from [AppSpacing] (gaps / padding on the 8dp grid): [AppSize] holds the fixed
 * measurements of design-system components — icon-box size, badge dot, card radius, hero
 * illustration sizes, content max width, etc. Reach for [AppSpacing] for spacing between things
 * and [AppSize] for how big a thing is, so neither leaves bare `.dp` literals at the call site.
 */
object AppSize {
    /** 1dp — hairline borders and dividers. */
    val hairline: Dp = 1.dp

    /** 18dp — inline icon glyph (e.g. a paragraph's leading icon). */
    val iconInline: Dp = 18.dp

    /** 20dp — standard icon glyph inside a tinted box, or a row's trailing chevron. */
    val icon: Dp = 20.dp

    /** 24dp — standard Material icon size; the default for [com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon]. */
    val iconDefault: Dp = 24.dp

    /** 38dp — tinted icon-box square ([com.sirelon.marsroverphotos.presentation.ui.AppIconBox]). */
    val iconBox: Dp = 38.dp

    /** 11dp — tinted icon-box corner radius. */
    val iconBoxRadius: Dp = 11.dp

    /** 6dp — status-badge status dot. */
    val badgeDot: Dp = 6.dp

    /** 3dp — segmented-control inner inset between the pill border and its segments. */
    val segmentedControlPadding: Dp = 3.dp

    /** 2dp — gap between adjacent segments in a segmented control. */
    val segmentedControlGap: Dp = 2.dp

    /** 16dp — grouped / outlined card corner radius. */
    val cardRadius: Dp = 16.dp

    /** 2dp — resting card elevation (matches the M3 elevated [AppCard] default). */
    val cardElevationResting: Dp = 2.dp

    /** 6dp — raised card elevation on desktop hover (the subtle lift on a rover row). */
    val cardElevationHover: Dp = 6.dp

    /** 112dp — fixed width of the full-height portrait thumbnail in a rover row. */
    val roverThumbWidth: Dp = 112.dp

    /** 40dp — trailing space reserved on a rover row's title line so the name/chip never
     *  collide with the overlaid top-end info button. */
    val roverInfoReserve: Dp = 40.dp

    /** 13dp — settings-row top/bottom padding. */
    val rowVerticalPadding: Dp = 13.dp

    /** 14dp — gap between a settings row's icon-box and its text. */
    val rowIconGap: Dp = 14.dp

    /** 840dp — max settings-content width; the column caps and centers only in the EXPANDED width class. */
    val contentMaxWidth: Dp = 840.dp

    /** 40dp — hero top breathing room above the mascot. */
    val heroTopPadding: Dp = 40.dp

    /** 100dp — hero mascot image. */
    val heroMascot: Dp = 100.dp

    /** 110dp — hero coral ring around the mascot. */
    val heroRing: Dp = 110.dp

    /** 140dp — hero coral radial glow. */
    val heroGlow: Dp = 140.dp

    /** 16dp — hero mascot drop-shadow elevation. */
    val heroShadow: Dp = 16.dp
}
