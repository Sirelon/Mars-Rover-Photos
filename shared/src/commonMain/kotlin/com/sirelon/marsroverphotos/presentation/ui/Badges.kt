package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing

/**
 * Design-system pill badge — a neutral, outlined, pill-shaped label (e.g. a version tag).
 */
@Composable
fun AppBadge(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
        modifier = modifier
            .clip(CircleShape)
            .background(colors.onSurface.copy(alpha = 0.06f))
            .border(AppSize.hairline, colors.outlineVariant, CircleShape)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
    )
}

/**
 * Design-system status badge — a status dot + [label] on a tinted pill, parameterized by [color]
 * (e.g. a "live" / "online" indicator). The dot and label share [color]; the pill is a soft tint
 * of it.
 */
@Composable
fun StatusBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Box(modifier = Modifier.size(AppSize.badgeDot).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/**
 * Design-system badge row — lays out a set of badges ([AppBadge], [StatusBadge], …) in a spaced
 * row. Callers fill it with their own badges.
 */
@Composable
fun BadgeRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        content = content
    )
}
