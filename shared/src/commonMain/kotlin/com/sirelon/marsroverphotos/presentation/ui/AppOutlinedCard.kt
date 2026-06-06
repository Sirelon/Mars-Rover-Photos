package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sirelon.marsroverphotos.presentation.theme.AppSize

/** Shared rounded-corner shape ([AppSize.cardRadius]) for [AppOutlinedCard] and other grouped surfaces. */
internal val CardShape = RoundedCornerShape(AppSize.cardRadius)

/**
 * Design-system outlined card — a raised surface ([AppSize.cardRadius] radius) with a hairline
 * outline and NO drop shadow. Distinct from the elevated [AppCard] (shadow, no border): the fill is
 * the M3 raised `surfaceContainerHigh` role so the card lifts off the screen background in both
 * themes — in dark, `surface == background`, so the hairline alone wouldn't separate it. A general
 * container for grouped rows (settings groups, list sections, …).
 */
@Composable
fun AppOutlinedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surfaceContainerHigh)
            .border(AppSize.hairline, colors.outlineVariant, CardShape),
        content = content,
    )
}
