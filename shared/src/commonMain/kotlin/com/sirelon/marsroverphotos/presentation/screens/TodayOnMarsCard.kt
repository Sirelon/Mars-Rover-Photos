package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.viewmodels.TodayOnMars
import com.sirelon.marsroverphotos.utils.formatDisplayDate

/**
 * The first thing on the rovers list: the freshest photo from an active rover, full width, with
 * the rover, sol and date over it. A tap opens that rover's feed, which starts on the same sol —
 * so a new user sees Mars before having to choose a rover.
 *
 * The photo and the text over it are a dark region whatever the app theme, so they run on the
 * dark theme (docs/DESIGN_SYSTEM.md › Color).
 */
@Composable
internal fun TodayOnMarsCard(
    today: TodayOnMars,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rover = today.rover.name
    val sol = today.cover.sol.takeIf { it > 0 }?.let { "Sol $it" }
    val date = formatDisplayDate(today.cover.earthDate)
    val detail = listOfNotNull(sol, date).joinToString(" · ")

    AppCard(
        modifier = modifier
            .testTag("todayOnMarsCard")
            .semantics(mergeDescendants = true) {
                contentDescription = "Today on Mars: the latest photo from $rover, $detail. Opens its photos."
            },
        onClick = onClick,
    ) {
        MarsRoverPhotosTheme(darkTheme = true) {
            val colors = MaterialTheme.colorScheme
            val scrim = remember(colors.scrim) {
                Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    1f to colors.scrim.copy(alpha = SCRIM_ALPHA),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSize.photoCardHeight)
                    .background(colors.surfaceContainerHigh)
            ) {
                NetworkImage(
                    imageUrl = today.cover.imageUrl,
                    modifier = Modifier.matchParentSize(),
                )
                Box(modifier = Modifier.matchParentSize().background(scrim))
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(AppSpacing.lg),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    ) {
                        Text(
                            text = "TODAY ON MARS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.secondary,
                        )
                        Text(
                            text = rover,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurface,
                    )
                }
            }
        }
    }
}

/** How dark the bottom of the photo gets under the text, for legibility on bright terrain. */
private const val SCRIM_ALPHA = 0.85f
