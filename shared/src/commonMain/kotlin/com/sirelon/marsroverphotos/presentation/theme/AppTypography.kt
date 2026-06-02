package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Semantic typography shortcuts for the Mars Rover Photos design system.
 *
 * Each property is a named alias that maps a product-level intent to a slot
 * in the Material 3 type scale. Colours are NOT baked in here — apply them
 * at the call site using [MaterialTheme.colorScheme] so that light/dark
 * themes work correctly.
 *
 * All getters are [@Composable] + [@ReadOnlyComposable] so they must be
 * accessed inside a composable function, exactly like [MaterialTheme.typography].
 *
 * Usage:
 * ```
 * Text(text = rover.name, style = AppTypography.roverTitle,
 *      color = MaterialTheme.colorScheme.secondary)
 * ```
 */
object AppTypography {

    /** Rover name / screen-level title — headlineMedium. */
    val roverTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.headlineMedium

    /** Section header inside a detail screen (e.g. "Mission Timeline") — titleLarge bold. */
    val sectionHeader: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)

    /** Label in a key–value info pair (e.g. "Status:") — titleMedium. */
    val infoLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleMedium

    /** Value in a key–value info pair — titleSmall. */
    val infoValue: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleSmall

    /**
     * Photo / camera caption beneath a grid tile — bodySmall.
     * Typically rendered in [MaterialTheme.colorScheme.secondary] (coral).
     */
    val photoCaption: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodySmall

    /** Stat card value (e.g. "320K") — titleLarge bold. */
    val statValue: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)

    /** Stat card label (e.g. "Total Photos") — labelMedium. */
    val statLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelMedium

    /** Primary body text (facts, descriptions) — bodyLarge. */
    val body: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge

    /** Secondary / supporting body text — bodyMedium. */
    val bodySecondary: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyMedium

    /** "Did You Know?" card header — titleMedium bold. */
    val factHeader: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)

    /** Small label (e.g. timeline milestone label) — labelSmall bold. */
    val milestoneLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)

    /** Large app-title or display text — headlineSmall. */
    val appTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.headlineSmall
}
