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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing

private val AppRowTextInset = AppSpacing.lg + AppSize.iconBox + AppSize.rowIconGap

/** Indent aligning content under a row's label (icon-box + gap), e.g. a segmented control. */
val AppRowIndent = AppSize.iconBox + AppSize.rowIconGap

/**
 * Design-system list row — leading [AppIconBox] + [label] + optional [sub] label, with a trailing
 * region that is either a custom [trailing] control or, for a link row (non-null [onClick] and no
 * [trailing]), a chevron.
 */
@Composable
fun AppRow(
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

/** Hairline divider between [AppRow]s, inset to start after the leading icon-box. */
@Composable
fun AppRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = AppRowTextInset, end = AppSpacing.lg),
        thickness = AppSize.hairline,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * Design-system grouped section — an optional label above an [AppOutlinedCard] that contains
 * [content] rows.
 *
 * [header] and [footer] are rendered inside the card before and after [content]. A divider is
 * automatically inserted before [footer]. This covers both the classic labelled-settings pattern
 * (pass [label]) and richer cards with custom inner header/footer rows (pass [header]/[footer]).
 *
 * Pass [onClick] to make the whole card tappable.
 */
@Composable
fun AppSection(
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // Clip before clickable: AppOutlinedCard applies its own clip *after* this modifier, so an
    // unclipped clickable would ripple as a rectangle over the card's rounded corners and take
    // touches outside them.
    val cardModifier = if (onClick != null) {
        Modifier.clip(CardShape).clickable(onClick = onClick)
    } else {
        Modifier
    }
    Column(modifier = modifier) {
        if (label != null) AppSectionLabel(text = label)
        AppOutlinedCard(modifier = cardModifier) {
            Column {
                header?.invoke()
                content()
                if (footer != null) {
                    AppRowDivider()
                    footer()
                }
            }
        }
    }
}
