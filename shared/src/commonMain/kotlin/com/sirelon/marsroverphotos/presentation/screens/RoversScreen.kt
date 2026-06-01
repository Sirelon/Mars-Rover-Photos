package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.IconButton
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.painter
import com.sirelon.marsroverphotos.presentation.viewmodels.RoversViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.label_photos_total
import com.sirelon.marsroverphotos.utils.formatDisplayDate
import com.sirelon.marsroverphotos.utils.formatThousands
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoversScreen(
    onNavigateToPhotos: (Long) -> Unit,
    onMissionInfoClick: (Long) -> Unit
) {
    val viewModel: RoversViewModel = koinViewModel()
    val rovers by viewModel.rovers.collectAsStateWithLifecycle()

    RoversContent(
        rovers = rovers,
        onClick = { rover ->
            viewModel.onRoverClicked(rover)
            onNavigateToPhotos(rover.id)
        },
        onMissionInfoClick = { rover ->
            viewModel.onMissionInfoClicked(rover)
            onMissionInfoClick(rover.id)
        }
    )
}

@Composable
fun RoversContent(
    rovers: List<Rover>,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 360.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        items(rovers, key = { it.id }) { item ->
            RoverItem(
                modifier = Modifier.fillMaxWidth(),
                rover = item,
                onClick = onClick,
                onMissionInfoClick = onMissionInfoClick
            )
        }
    }
}

@Composable
fun RoverItem(
    rover: Rover,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.padding(AppSpacing.sm),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onClick(rover) })
                .padding(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    TitleText(rover.name)
                }
                IconButton(onClick = { onMissionInfoClick(rover) }) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.Info,
                        contentDescription = "Mission Info"
                    )
                }
            }
            InfoText(label = "Status:", text = rover.status)

            Row(modifier = Modifier.padding(AppSpacing.sm)) {
                Image(
                    contentScale = ContentScale.FillHeight,
                    painter = rover.painter(),
                    modifier = Modifier
                        .height(175.dp)
                        .weight(1f)
                        .clip(shape = MaterialTheme.shapes.large),
                    contentDescription = rover.name
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    InfoText(
                        label = stringResource(Res.string.label_photos_total),
                        text = formatThousands(rover.totalPhotos)
                    )
                    InfoText(label = "Current sol:", text = "${rover.maxSol}")
                    InfoText(label = "Last photo date:", text = formatDisplayDate(rover.maxDate))
                    InfoText(label = "Launch date from Earth:", text = formatDisplayDate(rover.launchDate))
                    InfoText(label = "Landing date on Mars:", text = formatDisplayDate(rover.landingDate))
                }
            }
        }
    }
}

@Composable
private fun TitleText(text: String) {
    Text(
        text = text,
        style = AppTypography.roverTitle,
        color = MaterialTheme.colorScheme.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun InfoText(label: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = AppTypography.infoLabel,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = text,
            style = AppTypography.infoValue,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
