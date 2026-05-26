package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberUpdatedState
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
import com.sirelon.marsroverphotos.utils.formatThousands
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Immutable UI state for [RoverMissionInfoScreen].
 *
 * [missionInfoState] is null while the data is loading — the screen shows a
 * centered progress indicator in that case.
 */
data class RoverMissionInfoUiState(
    val missionInfoState: MissionInfoState? = null,
)

// ── State-holder composable ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoverMissionInfoScreen(
    roverId: Long,
    onBack: () -> Unit,
    onCameraClick: (String) -> Unit = {},
    viewModel: RoverMissionInfoViewModel = koinViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(roverId) {
        viewModel.setRoverId(roverId)
    }

    // rememberUpdatedState so the analytics call reads the latest state after the
    // roverId key settles, even if state arrives one frame after the id changes.
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(state?.rover?.id) {
        latestState?.rover?.let {
            viewModel.trackEvent("mission_info_opened_${it.name}")
        }
    }

    RoverMissionInfoScreen(
        state = RoverMissionInfoUiState(missionInfoState = state),
        onBack = onBack,
        onCameraClick = onCameraClick,
    )
}

// ── Pure-UI composable ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoverMissionInfoScreen(
    state: RoverMissionInfoUiState,
    onBack: () -> Unit,
    onCameraClick: (String) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(state.missionInfoState?.rover?.name ?: "Mission Info") },
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
            targetState = state.missionInfoState,
            label = "MissionInfoCrossfade",
            modifier = Modifier.padding(paddingValues)
        ) { currentState ->
            when {
                currentState == null -> CenteredProgress()
                else -> MissionInfoContent(
                    state = currentState,
                    onCameraClick = onCameraClick
                )
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
private fun MissionInfoContent(
    state: MissionInfoState,
    onCameraClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
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
            CamerasList(
                cameras = state.cameras,
                onCameraClick = onCameraClick
            )
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

            // When facts fail to load we intentionally render nothing here —
            // hiding the section avoids showing a broken-looking error banner
            // on first run.
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
                color = MaterialTheme.colorScheme.tertiary
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
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun MissionTimeline(milestones: ImmutableList<TimelineMilestone>, status: String) {
    val isActive = status.equals("active", ignoreCase = true)
    val milestoneColor = if (isActive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    // Animate the Landing→Current segment from 0f → 1f on first composition for
    // active rovers. Completed rovers go straight to 1f (mission ended).
    // Key on the full milestones list (ImmutableList has value equality) so the animation
    // replays when the rover changes. Reset animationStarted first to restart from 0f.
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(milestones) {
        animationStarted = false  // reset so the animation starts from 0f for the new rover
        animationStarted = true
    }
    val landingToCurrentProgress = animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "LandingToCurrentProgress"
    )

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
                // First connector (Launch → Landing) is always 100% — that
                // journey is complete once the rover has landed.
                // Second connector (Landing → Current/End) is animated for
                // active rovers and fully filled for completed missions.
                val segmentProgress = when {
                    index == 0 -> 1f
                    !isActive -> 1f
                    else -> landingToCurrentProgress.value
                }
                TimelineSegment(
                    progress = { segmentProgress },
                    modifier = Modifier
                        .weight(0.5f)
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineSegment(progress: () -> Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    val fillColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .drawBehind {
                drawRect(trackColor)
                drawRect(
                    color = fillColor,
                    size = Size(size.width * progress().coerceIn(0f, 1f), size.height),
                )
            },
    )
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
                value = formatThousands(totalPhotos),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Days on Mars",
                value = "${formatThousands(daysActive)} sols",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Earth Days",
                value = "${formatThousands(earthDaysActive)} days",
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
private fun CamerasList(
    cameras: ImmutableList<CameraSpec>,
    onCameraClick: (String) -> Unit = {}
) {
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
                CameraItem(
                    camera = camera,
                    onClick = { onCameraClick(camera.name) }
                )
                if (index < cameras.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun CameraItem(
    camera: CameraSpec,
    onClick: () -> Unit = {}
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
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

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun RoverMissionInfoScreenLoadingPreview() {
    RoverMissionInfoScreen(
        state = RoverMissionInfoUiState(missionInfoState = null),
        onBack = {},
        onCameraClick = {},
    )
}

@Preview
@Composable
private fun RoverMissionInfoScreenLoadedPreview() {
    val previewRover = Rover(
        id = 5,
        name = "Curiosity",
        drawableName = "curiosity",
        landingDate = "2012-08-06",
        launchDate = "2011-11-26",
        status = "active",
        maxSol = 4000,
        maxDate = "2023-10-01",
        totalPhotos = 695670
    )
    val previewMilestones = persistentListOf(
        TimelineMilestone(label = "Launch", date = "2011-11-26", sol = null, type = MilestoneType.LAUNCH),
        TimelineMilestone(label = "Landing", date = "2012-08-06", sol = null, type = MilestoneType.LANDING),
        TimelineMilestone(label = "Today", date = "2023-10-01", sol = 4000L, type = MilestoneType.CURRENT),
    )
    val previewCameras = persistentListOf(
        CameraSpec(
            name = "MAST",
            fullName = "Mast Camera",
            description = "Two cameras mounted on the mast for color imaging."
        )
    )
    val previewFacts = RoverMissionFacts(
        roverId = 5,
        roverName = "Curiosity",
        objectives = listOf("Assess habitability", "Study Martian climate"),
        funFacts = listOf("Curiosity is the size of a small car.")
    )
    RoverMissionInfoScreen(
        state = RoverMissionInfoUiState(
            missionInfoState = MissionInfoState(
                rover = previewRover,
                daysActive = 4001L,
                earthDaysActive = 4110L,
                cameras = previewCameras,
                missionFacts = previewFacts,
                factsLoading = false,
                factsError = null,
                timelineMilestones = previewMilestones,
            )
        ),
        onBack = {},
        onCameraClick = {},
    )
}
