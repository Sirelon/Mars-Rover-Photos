package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSize

/**
 * Design-system branded card — large-radius (16 dp) elevated card.
 * Drop-in replacement for Material3 [Card] that enforces the app's shape and elevation tokens.
 *
 * Pass [onClick] to make the card interactive: the card applies the clickable + a desktop
 * hover-lift (resting → hover elevation) internally, so callers should hand the card an
 * [onClick] rather than wrapping it in their own `clickable`/elevation handling. When [onClick]
 * is null the card is non-interactive and keeps the default resting elevation.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = AppSize.cardElevationResting),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val animatedElevation by animateDpAsState(
        targetValue = if (isHovered) AppSize.cardElevationHover else AppSize.cardElevationResting,
        label = "appCardElevation"
    )

    val cardModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
    } else {
        modifier
    }
    // Interactive cards animate their elevation on hover; non-interactive cards honor the
    // caller-supplied [elevation] (e.g. a 4dp header card).
    val cardElevation: CardElevation = if (onClick != null) {
        CardDefaults.cardElevation(defaultElevation = animatedElevation)
    } else {
        elevation
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = colors,
        elevation = cardElevation,
        content = content,
    )
}

/**
 * Design-system "Did You Know?" fact card — secondary-container tinted, medium radius (12 dp),
 * higher elevation (4 dp) to lift it above the photo grid.
 */
@Composable
fun AppFactCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = content,
    )
}
