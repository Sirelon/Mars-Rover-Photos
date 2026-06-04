package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing

/** Inset where a [SettingsRow]'s text starts: horizontal padding + icon-box + gap. */
private val RowTextInset = AppSpacing.lg + AppSize.iconBox + AppSize.rowIconGap

/** Indent aligning content under a row's label (icon-box + gap), e.g. the theme segmented control. */
internal val SettingsRowIndent = AppSize.iconBox + AppSize.rowIconGap

/**
 * Design-system section label — coral, uppercase, letter-spaced header that sits above a grouped
 * card. Mirrors the design's grouped-settings section headers (APPEARANCE / DATA …).
 */
@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(bottom = AppSpacing.sm)
    )
}

/**
 * Design-system settings row — leading [AppIconBox] + [label] + optional [sub] label, with a
 * trailing region that is either a custom [trailing] control (toggle, segmented…) or, for a link
 * row (non-null [onClick] and no [trailing]), a chevron. Tapping anywhere fires [onClick].
 */
@Composable
fun SettingsRow(
    icon: MaterialSymbol,
    iconContainer: Color,
    iconTint: Color,
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = AppSpacing.lg, vertical = AppSize.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSize.rowIconGap)
    ) {
        AppIconBox(symbol = icon, container = iconContainer, tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> MaterialSymbolIcon(
                symbol = MaterialSymbol.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
                size = AppSize.icon
            )
        }
    }
}

/** Hairline divider between [SettingsRow]s, inset to start after the leading icon-box. */
@Composable
fun SettingsRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = RowTextInset, end = AppSpacing.lg),
        thickness = AppSize.hairline,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
