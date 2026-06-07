package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing

/**
 * Design-system inline metric — a leading [symbol] icon, a bold [value], and a dim [label] laid out
 * in a single row (e.g. "🖼 133,811 photos"). General-purpose: it repeats across a rover row's
 * metric strip and can be reused anywhere a compact icon + value + label trio is needed.
 *
 * Colors are theme roles: the icon and label use `onSurfaceVariant`, the value uses `onSurface`.
 */
@Composable
fun AppMetricItem(
    symbol: MaterialSymbol,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        MaterialSymbolIcon(
            symbol = symbol,
            contentDescription = null,
            size = AppSize.iconInline,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
