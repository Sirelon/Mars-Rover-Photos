package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
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

    LaunchedEffect(state?.rover?.id) {
        state?.rover?.let {
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
            AppTopBar(
                scrollBehavior = scrollBehavior,
                title = { Text(state.missionInfoState?.rover?.name ?: "Mission Info") },
                onBack = onBack,
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
            .padding(horizontal = AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        contentPadding = PaddingValues(vertical = AppSpacing.lg),
    ) {
        item { RoverHeader(state.rover) }

        item {
            SectionHeader("Mission Timeline")
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MissionTimeline(
                milestones = state.timelineMilestones,
                status = state.rover.status
            )
        }

        item {
            SectionHeader("Statistics")
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MissionStatistics(
                totalPhotos = state.rover.totalPhotos,
                daysActive = state.daysActive,
                earthDaysActive = state.earthDaysActive,
                status = state.rover.status
            )
        }

        item {
            SectionHeader("Cameras")
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            CamerasList(
                cameras = state.cameras,
                onCameraClick = onCameraClick
            )
        }

        when {
            state.factsLoading -> item {
                SectionHeader("Mission Info")
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.missionFacts != null -> {
                if (state.missionFacts.objectives.isNotEmpty()) {
                    item {
                        SectionHeader("Mission Objectives")
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        MissionObjectives(objectives = state.missionFacts.objectives)
                    }
                }
                if (state.missionFacts.funFacts.isNotEmpty()) {
                    item {
                        SectionHeader("Fun Facts")
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
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
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
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
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            Text(
                text = rover.name,
                style = AppTypography.roverTitle,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.sectionHeader,
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun MissionTimeline(milestones: ImmutableList<TimelineMilestone>, status: String) {
    val roverActive = status.equals("active", ignoreCase = true)
    val milestoneColor = if (roverActive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    // Animate the Landing→Current segment from 0f → 1f on first composition for active rovers.
    // Completed rovers snap straight to 1f. Using Animatable so snapTo(0f) + animateTo(1f) are
    // two distinct frames — animateFloatAsState + a boolean flag batches both writes into one
    // snapshot and the animation never resets on rover change.
    val landingToCurrentProgress = remember { Animatable(0f) }
    LaunchedEffect(milestones) {
        if (roverActive) {
            landingToCurrentProgress.snapTo(0f)
            landingToCurrentProgress.animateTo(1f, animationSpec = tween(durationMillis = 600))
        } else {
            landingToCurrentProgress.snapTo(1f)
        }
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
                    style = AppTypography.milestoneLabel,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
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
                Spacer(modifier = Modifier.height(AppSpacing.xs))
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
                    !roverActive -> 1f
                    else -> landingToCurrentProgress.value
                }
                TimelineSegment(
                    progress = { segmentProgress },
                    modifier = Modifier
                        .weight(0.5f)
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = AppSpacing.xs)
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
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
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
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
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
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = AppTypography.statLabel,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = value,
                style = AppTypography.statValue,
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
            style = AppTypography.bodySecondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            cameras.forEachIndexed { index, camera ->
                CameraItem(
                    camera = camera,
                    onClick = { onCameraClick(camera.name) }
                )
                if (index < cameras.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.md))
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
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = camera.name,
                    style = AppTypography.factHeader
                )
                Text(
                    text = camera.fullName,
                    style = AppTypography.bodySecondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text = camera.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 36.dp)
        )
    }
}

@Composable
private fun MissionObjectives(objectives: List<String>) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            objectives.forEach { objective ->
                Row(modifier = Modifier.padding(vertical = AppSpacing.xs)) {
                    Text(
                        text = "•",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    )
                    Text(
                        text = objective,
                        style = AppTypography.body
                    )
                }
            }
        }
    }
}

@Composable
private fun FunFacts(facts: List<String>) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            facts.forEach { fact ->
                Row(modifier = Modifier.padding(vertical = AppSpacing.xs)) {
                    Text(text = "✨", modifier = Modifier.padding(end = AppSpacing.sm))
                    Text(
                        text = fact,
                        style = AppTypography.body
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
