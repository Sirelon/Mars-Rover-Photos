package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.sirelon.marsroverphotos.presentation.navigation.LocalMissionLayoutAnimatedVisibilityScope
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.mission.CameraSpec
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.presentation.theme.AppMotion
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.viewmodels.MilestoneType
import com.sirelon.marsroverphotos.presentation.viewmodels.MissionInfoState
import com.sirelon.marsroverphotos.presentation.viewmodels.RoverMissionInfoViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.TimelineMilestone
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

data class RoverMissionInfoUiState(
    val missionInfoState: MissionInfoState? = null,
)

private data class MissionLayoutKey(
    val data: MissionInfoState?,
    val isExpanded: Boolean,
    val isMediumPlus: Boolean,
)

// ── State-holder composable ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoverMissionInfoScreen(
    roverId: Long,
    onBack: () -> Unit,
    onCameraClick: (String) -> Unit = {},
    onBrowsePhotos: () -> Unit = {},
    viewModel: RoverMissionInfoViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(roverId) { viewModel.setRoverId(roverId) }
    LaunchedEffect(state?.rover?.id) {
        state?.rover?.let { viewModel.trackEvent("mission_info_opened_${it.name}") }
    }

    RoverMissionInfoScreen(
        state = RoverMissionInfoUiState(missionInfoState = state),
        onBack = onBack,
        onCameraClick = onCameraClick,
        onBrowsePhotos = onBrowsePhotos,
    )
}

// ── Pure-UI composable ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoverMissionInfoScreen(
    state: RoverMissionInfoUiState,
    onBack: () -> Unit,
    onCameraClick: (String) -> Unit = {},
    onBrowsePhotos: () -> Unit = {},
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isMediumPlus = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isExpanded   = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)

    val scrollBehavior = if (isMediumPlus) TopAppBarDefaults.enterAlwaysScrollBehavior() else null

    Scaffold(
        modifier = if (scrollBehavior != null)
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier,
        contentWindowInsets = WindowInsets(),
        topBar = {
            if (isMediumPlus) {
                AppTopBar(
                    scrollBehavior = scrollBehavior,
                    title = { Text(state.missionInfoState?.rover?.name ?: "Mission Info") },
                    onBack = onBack,
                    actions = {
                        AppButton(
                            onClick = onBrowsePhotos,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                        ) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                size = AppSize.iconInline,
                            )
                            Spacer(Modifier.width(AppSpacing.xs))
                            Text("Browse Photos")
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        AnimatedContent(
            targetState = MissionLayoutKey(state.missionInfoState, isExpanded, isMediumPlus),
            label = "MissionInfoContent",
            modifier = Modifier.padding(paddingValues),
            transitionSpec = {
                val dur = AppMotion.SharedContainerMs
                when {
                    // Loading → content: fade + gentle upward reveal
                    initialState.data == null -> ContentTransform(
                        targetContentEnter = fadeIn(tween(dur)) +
                            slideInVertically(tween(dur)) { it / 10 },
                        initialContentExit = fadeOut(tween(dur / 2)),
                        sizeTransform = null,
                    )
                    // Compact/medium → expanded (window widened): slide in from right
                    targetState.isExpanded && !initialState.isExpanded -> ContentTransform(
                        targetContentEnter = fadeIn(tween(dur)) +
                            slideInHorizontally(tween(dur)) { it / 6 },
                        initialContentExit = fadeOut(tween(dur / 2)) +
                            slideOutHorizontally(tween(dur)) { -it / 6 },
                        sizeTransform = null,
                    )
                    // Expanded → compact/medium (window narrowed): slide in from left
                    !targetState.isExpanded && initialState.isExpanded -> ContentTransform(
                        targetContentEnter = fadeIn(tween(dur)) +
                            slideInHorizontally(tween(dur)) { -it / 6 },
                        initialContentExit = fadeOut(tween(dur / 2)) +
                            slideOutHorizontally(tween(dur)) { it / 6 },
                        sizeTransform = null,
                    )
                    // Same layout tier, data change: simple crossfade
                    else -> ContentTransform(
                        targetContentEnter = fadeIn(tween(dur / 2)),
                        initialContentExit = fadeOut(tween(dur / 2)),
                        sizeTransform = null,
                    )
                }
            },
        ) { key ->
            CompositionLocalProvider(LocalMissionLayoutAnimatedVisibilityScope provides this) {
            when {
                key.data == null -> CenteredProgress()
                key.isExpanded -> ExpandedLayout(
                    state = key.data,
                    onCameraClick = onCameraClick,
                    onBrowsePhotos = onBrowsePhotos,
                )
                else -> MissionInfoContent(
                    state = key.data,
                    isMediumPlus = key.isMediumPlus,
                    onCameraClick = onCameraClick,
                    onBrowsePhotos = onBrowsePhotos,
                    onBack = if (!key.isMediumPlus) onBack else null,
                )
            }
            } // CompositionLocalProvider
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
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
        onBrowsePhotos = {},
    )
}

@Preview
@Composable
private fun RoverMissionInfoScreenLoadedPreview() {
    val previewRover = Rover(
        id = 5, name = "Curiosity", drawableName = "curiosity",
        landingDate = "2012-08-06", launchDate = "2011-11-26",
        status = "active", maxSol = 4000, maxDate = "2023-10-01", totalPhotos = 695670,
    )
    val previewMilestones = persistentListOf(
        TimelineMilestone(label = "Launch",  date = "Nov 26, 2011", sol = null,   type = MilestoneType.LAUNCH),
        TimelineMilestone(label = "Landing", date = "Aug 6, 2012",  sol = null,   type = MilestoneType.LANDING),
        TimelineMilestone(label = "Today",   date = "Oct 1, 2023",  sol = 4000L,  type = MilestoneType.CURRENT),
    )
    val previewCameras = persistentListOf(
        CameraSpec(name = "MAST", fullName = "Mast Camera",         description = "Two cameras for color imaging."),
        CameraSpec(name = "FHAZ", fullName = "Front Hazard Camera",  description = "Wide-angle hazard detection."),
    )
    val previewFacts = RoverMissionFacts(
        roverId = 5, roverName = "Curiosity",
        objectives = listOf("Assess habitability", "Study Martian climate"),
        funFacts = listOf("Curiosity is the size of a small car."),
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
        onBrowsePhotos = {},
    )
}
