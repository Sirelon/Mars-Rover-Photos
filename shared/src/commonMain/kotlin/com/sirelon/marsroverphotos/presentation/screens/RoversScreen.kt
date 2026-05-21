package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.painter
import com.sirelon.marsroverphotos.presentation.viewmodels.RoversViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.label_photos_total
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
    onMissionInfoClick: (rover: Rover) -> Unit
) {
    LazyColumn(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        items(rovers, key = { it.id }) { item ->
            RoverItem(
                rover = item,
                onClick = onClick,
                onMissionInfoClick = onMissionInfoClick
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun RoverItem(
    rover: Rover,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit
) {
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(8.dp)
                .clickable(onClick = { onClick(rover) })
        ) {
            TitleText(rover.name)
            InfoText(label = "Status:", text = rover.status)

            Row(modifier = Modifier.padding(8.dp)) {
                Image(
                    contentScale = ContentScale.FillHeight,
                    painter = rover.painter(),
                    modifier = Modifier
                        .height(175.dp)
                        .weight(1f)
                        .clip(shape = MaterialTheme.shapes.large),
                    contentDescription = rover.name
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    InfoText(
                        label = stringResource(Res.string.label_photos_total),
                        text = "${rover.totalPhotos}"
                    )
                    InfoText(label = "Last photo date:", text = rover.maxDate)
                    InfoText(label = "Launch date from Earth:", text = rover.launchDate)
                    InfoText(label = "Landing date on Mars:", text = rover.landingDate)
                }
            }
        }
        IconButton(
            onClick = { onMissionInfoClick(rover) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Info,
                contentDescription = "Mission Info"
            )
        }
    }
}

@Composable
private fun TitleText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun InfoText(label: String, text: String) {
    val typography = MaterialTheme.typography
    val textToShow = AnnotatedString.Builder().apply {
        pushStyle(
            typography.titleMedium.toSpanStyle()
                .copy(color = MaterialTheme.colorScheme.secondary)
        )
        append("$label ")
        pushStyle(
            style = typography.titleSmall.toSpanStyle()
                .copy(color = MaterialTheme.colorScheme.onSurface)
        )
        append(text)
    }
    Text(
        text = textToShow.toAnnotatedString(),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
