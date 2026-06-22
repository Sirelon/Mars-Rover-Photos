package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.presentation.theme.activeStatusColor
import com.sirelon.marsroverphotos.presentation.ui.AppBadge
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedCard
import com.sirelon.marsroverphotos.presentation.ui.BadgeRow
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.StatusBadge
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverBadge
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverCameras
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverFunFact
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverImage
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverName
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverObjectives
import com.sirelon.marsroverphotos.presentation.viewmodels.MilestoneType
import com.sirelon.marsroverphotos.presentation.viewmodels.MissionInfoState
import com.sirelon.marsroverphotos.presentation.viewmodels.TimelineMilestone
import com.sirelon.marsroverphotos.utils.formatThousands
import kotlinx.collections.immutable.ImmutableList

// ── Expanded 2-panel layout ───────────────────────────────────────────────────

@Composable
internal fun ExpandedLayout(
    state: MissionInfoState,
    onCameraClick: (String) -> Unit,
    onBrowsePhotos: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // ── Left panel — rover portrait + identity + stats + fun fact ──────
        LazyColumn(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            item {
                RoverImageCard(
                    rover = state.rover,
                    aspectRatio = 4f / 3f,
                    modifier = Modifier
                        .sharedRoverImage(state.rover.id)
                        .clip(MaterialTheme.shapes.large),
                )
            }
            item { RoverIdentity(state) }
            item {
                StatsListTable(
                    totalPhotos = state.rover.totalPhotos,
                    daysActive = state.daysActive,
                    earthDaysActive = state.earthDaysActive,
                )
            }
            state.missionFacts?.funFacts?.firstOrNull()?.let { fact ->
                item { FunFact(text = fact, modifier = Modifier.sharedRoverFunFact(state.rover.id)) }
            }
        }

        VerticalDivider()

        // ── Right panel — expedition log ───────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                horizontal = AppSpacing.x3l,
                vertical = AppSpacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxl),
        ) {
            item { ExpeditionLogHeader() }
            item {
                DesktopLogSection("Mission Timeline") {
                    DesktopTimeline(milestones = state.timelineMilestones, status = state.rover.status)
                }
            }
            if (state.missionFacts?.objectives?.isNotEmpty() == true) {
                item {
                    DesktopLogSection("Mission Objectives") {
                        V1Objectives(objectives = state.missionFacts.objectives, modifier = Modifier.sharedRoverObjectives(state.rover.id))
                    }
                }
            }
            if (state.cameras.isNotEmpty()) {
                item {
                    DesktopLogSection("Cameras & Instruments") {
                        CamerasGrid(cameras = state.cameras, onCameraClick = onCameraClick, modifier = Modifier.sharedRoverCameras(state.rover.id))
                    }
                }
            }
        }
    }
}

// ── Left panel composables ────────────────────────────────────────────────────

@Composable
private fun RoverIdentity(state: MissionInfoState) {
    val baseStyle = MaterialTheme.typography.headlineMedium
    val nameStyle = remember(baseStyle) {
        baseStyle.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp)
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(
            text = state.rover.name,
            style = nameStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.sharedRoverName(state.rover.id),
        )
        BadgeRow(modifier = Modifier.sharedRoverBadge(state.rover.id)) {
            if (state.rover.isActive) StatusBadge("Active", activeStatusColor())
            else AppBadge(state.rover.status.replaceFirstChar { it.uppercaseChar() })
        }
    }
}

@Composable
private fun StatsListTable(totalPhotos: Int, daysActive: Long, earthDaysActive: Long) {
    val rows = remember(totalPhotos, daysActive, earthDaysActive) {
        listOf(
            "Total Photos" to formatThousands(totalPhotos),
            "Sols on Mars" to formatThousands(daysActive),
            "Earth Days"   to formatThousands(earthDaysActive),
        )
    }
    AppOutlinedCard {
        rows.forEachIndexed { i, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = AppTypography.bodySecondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (i < rows.size - 1) HorizontalDivider()
        }
    }
}

// ── Right panel composables ───────────────────────────────────────────────────

@Composable
private fun ExpeditionLogHeader() {
    val baseStyle = MaterialTheme.typography.headlineSmall
    val titleStyle = remember(baseStyle) {
        baseStyle.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        SectionLabel("Expedition Log")
        Text(
            text = "Mission Overview",
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DesktopLogSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
        Text(text = title, style = AppTypography.sectionHeader, color = MaterialTheme.colorScheme.tertiary)
        HorizontalDivider()
        content()
    }
}

@Composable
private fun DesktopTimeline(milestones: ImmutableList<TimelineMilestone>, status: String) {
    val activeDotColor   = MaterialTheme.colorScheme.secondary
    val inactiveDotColor = MaterialTheme.colorScheme.secondaryContainer
    val activeIconTint   = MaterialTheme.colorScheme.onSecondary
    val inactiveIconTint = MaterialTheme.colorScheme.onSecondaryContainer
    val connectorColor   = MaterialTheme.colorScheme.outlineVariant

    Column {
        milestones.forEachIndexed { i, milestone ->
            val isCurrent = milestone.type == MilestoneType.CURRENT || milestone.type == MilestoneType.END
            val isLast = i == milestones.size - 1
            val dotColor = if (isCurrent) activeDotColor  else inactiveDotColor
            val iconTint = if (isCurrent) activeIconTint  else inactiveIconTint

            Row {
                Column(
                    modifier = Modifier.width(AppSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppSpacing.xl)
                            .clip(CircleShape)
                            .background(dotColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        val iconSymbol = when (milestone.type) {
                            MilestoneType.LAUNCH  -> MaterialSymbol.Rocket
                            MilestoneType.LANDING -> MaterialSymbol.FlightLand
                            MilestoneType.CURRENT -> MaterialSymbol.Star
                            MilestoneType.END     -> MaterialSymbol.Flag
                        }
                        MaterialSymbolIcon(
                            symbol = iconSymbol,
                            contentDescription = null,
                            tint = iconTint,
                            size = AppSize.iconInline,
                        )
                    }
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(AppSize.hairline * 2)
                                .height(AppSpacing.x3l)
                                .background(connectorColor)
                        )
                    }
                }
                Spacer(Modifier.width(AppSpacing.lg))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (!isLast) AppSpacing.xl else AppSpacing.xs),
                ) {
                    SectionLabel(milestone.label)
                    Text(
                        text = milestone.date,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    milestone.sol?.let {
                        Text(
                            text = "Sol $it",
                            style = AppTypography.bodySecondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
