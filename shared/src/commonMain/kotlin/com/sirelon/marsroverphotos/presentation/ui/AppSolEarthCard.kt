package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.earth_date_nearest_match
import com.sirelon.marsroverphotos.shared.resources.sol_martian_day
import org.jetbrains.compose.resources.stringResource

/**
 * Two-column display card showing the current Sol number alongside its nearest Earth date.
 * Used in the jump-to-date picker and photo info sheets.
 */
@Composable
fun AppSolEarthCard(
    sol: Long,
    earthDate: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        SolInfoCard(sol = sol, modifier = Modifier.weight(1f))
        EarthDateInfoCard(earthDate = earthDate, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SolInfoCard(sol: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Schedule,
                    contentDescription = null,
                    size = AppSize.iconInline,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "SOL",
                    style = AppTypography.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = sol.toString(),
                style = AppTypography.statValue.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(Res.string.sol_martian_day),
                style = AppTypography.statLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EarthDateInfoCard(earthDate: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.CalendarMonth,
                    contentDescription = null,
                    size = AppSize.iconInline,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "EARTH DATE",
                    style = AppTypography.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = earthDate.ifBlank { "—" },
                style = AppTypography.infoValue.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = stringResource(Res.string.earth_date_nearest_match),
                style = AppTypography.statLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
