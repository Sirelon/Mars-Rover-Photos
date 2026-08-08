package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sirelon.marsroverphotos.domain.releasenotes.ChangeType
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing

fun ChangeType.toIcon(): MaterialSymbol = when (this) {
    ChangeType.MISSION_INFO -> MaterialSymbol.Rocket
    ChangeType.MULTIPLATFORM -> MaterialSymbol.Devices
    ChangeType.OFFLINE_CACHE -> MaterialSymbol.CloudOff
    ChangeType.FAVORITES -> MaterialSymbol.Favorite
    ChangeType.POPULAR -> MaterialSymbol.LocalFireDepartment
    ChangeType.REDESIGN -> MaterialSymbol.Star
}

/** A what's-new change row — delegates to [AppRow] for consistent icon-box + label layout. */
@Composable
fun WhatsNewRow(
    change: Release.Change,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    AppRow(
        icon = change.type.toIcon(),
        iconContainer = colors.primaryContainer,
        iconTint = colors.onPrimaryContainer,
        label = change.title,
        sub = change.summary,
        modifier = modifier,
        onClick = if (change.detail != null) onClick else null,
        trailing = if (change.detail != null) {
            {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    size = AppSize.icon,
                )
            }
        } else null,
    )
}

/**
 * A card summarising one [Release] — version header, list of [WhatsNewRow] changes, and a footer
 * linking to the full story. Uses [AppSection] so it shares the same grouped-card structure as
 * the rest of the app.
 */
@Composable
fun ReleaseCard(release: Release, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isCurrent = release.version == BuildInfo.versionName

    AppSection(
        modifier = modifier,
        onClick = onOpen,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Text(
                    text = release.version,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) {
                    StatusBadge(label = "INSTALLED", color = colors.secondary)
                }
                Text(
                    text = release.date.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            AppRowDivider()
        },
        footer = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${release.changes.size} change${if (release.changes.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.ChevronRight,
                        contentDescription = null,
                        tint = colors.primary,
                        size = AppSize.icon,
                    )
                }
            }
        },
    ) {
        release.changes.forEachIndexed { index, change ->
            if (index > 0) AppRowDivider()
            WhatsNewRow(change = change)
        }
    }
}
