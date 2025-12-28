package com.sirelon.marsroverphotos.feature.mission

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sirelon.marsroverphotos.models.drawableRes
import com.sirelon.marsroverphotos.ui.MaterialSymbol
import com.sirelon.marsroverphotos.ui.MaterialSymbolIcon
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Main entry point for the Rover Mission Info screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoverMissionInfoScreen(
    roverId: Long,
    onBack: () -> Unit,
    viewModel: RoverMissionInfoViewModel = koinViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(roverId) {
        viewModel.setRoverId(roverId)
    }

    LaunchedEffect(state?.rover?.id) {
        state?.rover?.let {
            viewModel.trackEvent("mission_info_opened_${it.name}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.rover?.name ?: "Mission Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Crossfade(
            targetState = state,
            label = "MissionInfoCrossfade",
            modifier = Modifier.padding(paddingValues)
        ) { currentState ->
            when {
                currentState == null -> CenteredProgress()
                else -> MissionInfoContent(state = currentState)
            }
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MissionInfoContent(state: MissionInfoState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Rover Header
        item {
            RoverHeader(state.rover)
        }

        // Mission Timeline
        item {
            SectionHeader("Mission Timeline")
            Spacer(modifier = Modifier.height(8.dp))
            MissionTimeline(
                milestones = state.timelineMilestones,
                status = state.rover.status
            )
        }

        // Mission Statistics
        item {
            SectionHeader("Statistics")
            Spacer(modifier = Modifier.height(8.dp))
            MissionStatistics(
                totalPhotos = state.rover.totalPhotos,
                daysActive = state.daysActive,
                earthDaysActive = state.earthDaysActive,
                status = state.rover.status
            )
        }

        // Cameras
        item {
            SectionHeader("Cameras")
            Spacer(modifier = Modifier.height(8.dp))
            CamerasList(cameras = state.cameras)
        }

        // Mission Facts (from Firebase)
        if (state.factsLoading) {
            item {
                SectionHeader("Mission Info")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else if (state.missionFacts != null) {
            if (state.missionFacts.objectives.isNotEmpty()) {
                item {
                    SectionHeader("Mission Objectives")
                    Spacer(modifier = Modifier.height(8.dp))
                    MissionObjectives(objectives = state.missionFacts.objectives)
                }
            }

            if (state.missionFacts.funFacts.isNotEmpty()) {
                item {
                    SectionHeader("Fun Facts")
                    Spacer(modifier = Modifier.height(8.dp))
                    FunFacts(facts = state.missionFacts.funFacts)
                }
            }
        } else if (state.factsError != null) {
            item {
                SectionHeader("Mission Info")
                Spacer(modifier = Modifier.height(8.dp))
                ErrorMessage(state.factsError)
            }
        }
    }
}

@Composable
private fun RoverHeader(rover: com.sirelon.marsroverphotos.models.Rover) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val drawableRes = rover.drawableRes(LocalContext.current)
            AsyncImage(
                model = drawableRes,
                contentDescription = rover.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = rover.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MissionTimeline(milestones: List<TimelineMilestone>, status: String) {
    val isActive = status.equals("active", ignoreCase = true)
    val milestoneColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        milestones.forEachIndexed { index, milestone ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Label
                Text(
                    text = milestone.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Milestone marker
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(milestoneColor),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (milestone.type) {
                        MilestoneType.LAUNCH -> MaterialSymbol.Rocket
                        MilestoneType.LANDING -> MaterialSymbol.FlightLand
                        MilestoneType.CURRENT -> MaterialSymbol.Star
                        MilestoneType.END -> MaterialSymbol.Flag
                    }
                    MaterialSymbolIcon(
                        symbol = icon,
                        contentDescription = milestone.label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date
                Text(
                    text = milestone.date,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                // Sol (if available)
                milestone.sol?.let {
                    Text(
                        text = "Sol $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Connector line (except after last milestone)
            if (index < milestones.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 4.dp)
                        .background(milestoneColor)
                )
            }
        }
    }
}

@Composable
private fun MissionStatistics(
    totalPhotos: Int,
    daysActive: Long,
    earthDaysActive: Long,
    status: String
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(280.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            listOf(
                StatItem("Total Photos", numberFormat.format(totalPhotos)),
                StatItem("Days on Mars", "$daysActive sols"),
                StatItem("Earth Days", "$earthDaysActive days"),
                StatItem("Status", status.replaceFirstChar { it.uppercase() })
            )
        ) { statItem ->
            StatCard(label = statItem.label, value = statItem.value)
        }
    }
}

private data class StatItem(val label: String, val value: String)

@Composable
private fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CamerasList(cameras: List<CameraSpec>) {
    if (cameras.isEmpty()) {
        Text(
            text = "No camera information available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            cameras.forEachIndexed { index, camera ->
                CameraItem(camera)
                if (index < cameras.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun CameraItem(camera: CameraSpec) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = camera.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = camera.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = camera.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 36.dp)
        )
    }
}

@Composable
private fun MissionObjectives(objectives: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            objectives.forEach { objective ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = objective,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FunFacts(facts: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            facts.forEach { fact ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "✨",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = fact,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
