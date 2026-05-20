package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.mission.CameraSpec
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.painter
import com.sirelon.marsroverphotos.presentation.viewmodels.MilestoneType
import com.sirelon.marsroverphotos.presentation.viewmodels.MissionInfoState
import com.sirelon.marsroverphotos.presentation.viewmodels.RoverMissionInfoViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.TimelineMilestone
import org.koin.compose.viewmodel.koinViewModel

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

// ── Content ──────────────────────────────────────────────────────────────────

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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        item { RoverHeader(state.rover) }

        item {
            SectionHeader("Mission Timeline")
            Spacer(modifier = Modifier.height(8.dp))
            MissionTimeline(
                milestones = state.timelineMilestones,
                status = state.rover.status
            )
        }

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

        item {
            SectionHeader("Cameras")
            Spacer(modifier = Modifier.height(8.dp))
            CamerasList(cameras = state.cameras)
        }

        when {
            state.factsLoading -> item {
                SectionHeader("Mission Info")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.missionFacts != null -> {
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
            }

            state.factsError != null -> item {
                SectionHeader("Mission Info")
                Spacer(modifier = Modifier.height(8.dp))
                ErrorMessage(state.factsError)
            }
        }
    }
}

// ── Sections ─────────────────────────────────────────────────────────────────

@Composable
private fun RoverHeader(rover: Rover) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rover.painter(),
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
    val milestoneColor = if (status.equals("active", ignoreCase = true)) {
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
                Text(
                    text = milestone.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(milestoneColor),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (milestone.type) {
                        MilestoneType.LAUNCH  -> MaterialSymbol.Rocket
                        MilestoneType.LANDING -> MaterialSymbol.FlightLand
                        MilestoneType.CURRENT -> MaterialSymbol.Star
                        MilestoneType.END     -> MaterialSymbol.Flag
                    }
                    MaterialSymbolIcon(
                        symbol = icon,
                        contentDescription = milestone.label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = milestone.date,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                milestone.sol?.let {
                    Text(
                        text = "Sol $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

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

/**
 * 2×2 stat grid using plain Row/Column — avoids nested-scrollable issues
 * and doesn't need a fixed height override.
 */
@Composable
private fun MissionStatistics(
    totalPhotos: Int,
    daysActive: Long,
    earthDaysActive: Long,
    status: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Total Photos",
                value = totalPhotos.abbreviate(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Days on Mars",
                value = "${daysActive.abbreviate()} sols",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Earth Days",
                value = "${earthDaysActive.abbreviate()} days",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Status",
                value = status.replaceFirstChar { it.uppercaseChar() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
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
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(text = "✨", modifier = Modifier.padding(end = 8.dp))
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

// ── Number formatting ─────────────────────────────────────────────────────────

/**
 * KMP-safe compact number formatter. No java.text.NumberFormat, no Locale.
 *
 * Examples: 999 → "999", 1_500 → "1.5K", 471_091 → "471.0K", 4_345_812 → "4.3M"
 */
private fun Int.abbreviate(): String = when {
    this >= 1_000_000 -> {
        val whole = this / 1_000_000
        val tenths = (this % 1_000_000) / 100_000
        "${whole}.${tenths}M"
    }
    this >= 1_000 -> {
        val whole = this / 1_000
        val tenths = (this % 1_000) / 100
        "${whole}.${tenths}K"
    }
    else -> "$this"
}

private fun Long.abbreviate(): String = toInt().abbreviate()
