package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.mission.CameraSpec
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.presentation.theme.activeStatusColor
import com.sirelon.marsroverphotos.presentation.ui.AppBadge
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import com.sirelon.marsroverphotos.presentation.ui.AppFactCard
import com.sirelon.marsroverphotos.presentation.ui.AppIconBox
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedCard
import com.sirelon.marsroverphotos.presentation.ui.BadgeRow
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.StatusBadge
import com.sirelon.marsroverphotos.presentation.ui.painter
import com.sirelon.marsroverphotos.presentation.viewmodels.MilestoneType
import com.sirelon.marsroverphotos.presentation.viewmodels.MissionInfoState
import com.sirelon.marsroverphotos.presentation.viewmodels.TimelineMilestone
import com.sirelon.marsroverphotos.utils.formatThousands
import kotlinx.collections.immutable.ImmutableList

// ── Single-column content (compact + medium) ──────────────────────────────────

@Composable
internal fun MissionInfoContent(
    state: MissionInfoState,
    isMediumPlus: Boolean,
    onCameraClick: (String) -> Unit,
    onBrowsePhotos: () -> Unit,
    onBack: (() -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppSpacing.x3l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        // Full-bleed hero — no horizontal padding
        item {
            MissionHeroSection(rover = state.rover, onBack = onBack)
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
            ) {
                SectionHeader("Mission Timeline")
                V1Timeline(
                    milestones = state.timelineMilestones,
                    status = state.rover.status,
                    fillWidth = isMediumPlus,
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                SectionHeader("Statistics")
                V1Stats(
                    totalPhotos = state.rover.totalPhotos,
                    daysActive = state.daysActive,
                    earthDaysActive = state.earthDaysActive,
                )
            }
        }

        if (state.cameras.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    SectionHeader("Cameras & Instruments")
                    if (isMediumPlus) {
                        CamerasGrid(cameras = state.cameras, onCameraClick = onCameraClick)
                    } else {
                        V1CamerasAccordion(cameras = state.cameras, onCameraClick = onCameraClick)
                    }
                }
            }
        }

        when {
            state.factsLoading -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                ) {
                    SectionHeader("Mission Info")
                    Spacer(Modifier.height(AppSpacing.sm))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }

            state.missionFacts != null -> {
                if (state.missionFacts.objectives.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        ) {
                            SectionHeader("Mission Objectives")
                            V1Objectives(
                                objectives = state.missionFacts.objectives,
                                twoCol = isMediumPlus,
                            )
                        }
                    }
                }

                state.missionFacts.funFacts.firstOrNull()?.let { fact ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        ) {
                            SectionHeader("Did You Know?")
                            FunFact(text = fact)
                        }
                    }
                }
            }
        }

        // Browse Photos CTA — compact only (medium+ has it in the top bar)
        if (!isMediumPlus) {
            item {
                BrowsePhotosButton(
                    onClick = onBrowsePhotos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                )
            }
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

// ── Shared rover image with gradient ─────────────────────────────────────────

@Composable
internal fun RoverImageCard(
    rover: Rover,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    val bgColor = MaterialTheme.colorScheme.background
    val gradient = remember(bgColor) {
        Brush.verticalGradient(
            0.45f to Color.Transparent,
            1f to bgColor.copy(alpha = 0.96f),
        )
    }
    Box(modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio)) {
        Image(
            painter = rover.painter(),
            contentDescription = rover.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(modifier = Modifier.fillMaxSize().background(gradient))
    }
}

@Composable
private fun MissionHeroSection(rover: Rover, onBack: (() -> Unit)?) {
    val topGradient = remember {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.52f),
            0.4f to Color.Transparent,
        )
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        RoverImageCard(rover = rover, aspectRatio = 16f / 9f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(topGradient),
        )
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(AppSpacing.lg)
                    .background(Color.Black.copy(alpha = 0.42f), CircleShape),
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    size = AppSize.iconDefault,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = AppSpacing.xl, end = AppSpacing.xl, bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Text(
                text = rover.name,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BadgeRow {
                if (rover.isActive) StatusBadge("Active", activeStatusColor())
                else AppBadge(rover.status.replaceFirstChar { it.uppercaseChar() })
            }
        }
    }
}

// ── Timeline ──────────────────────────────────────────────────────────────────

@Composable
private fun V1Timeline(
    milestones: ImmutableList<TimelineMilestone>,
    status: String,
    fillWidth: Boolean,
) {
    @Composable
    fun connector(rowScope: androidx.compose.foundation.layout.RowScope) = with(rowScope) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .height(AppSize.hairline * 2)
                .run { if (fillWidth) weight(0.25f) else width(AppSpacing.lg) }
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }

    if (fillWidth) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            milestones.forEachIndexed { i, milestone ->
                TimelineCard(milestone = milestone, modifier = Modifier.weight(1f))
                if (i < milestones.size - 1) connector(this)
            }
        }
    } else {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top,
        ) {
            milestones.forEachIndexed { i, milestone ->
                TimelineCard(milestone = milestone)
                if (i < milestones.size - 1) connector(this)
            }
        }
    }
}

@Composable
private fun TimelineCard(milestone: TimelineMilestone, modifier: Modifier = Modifier) {
    val iconSymbol = when (milestone.type) {
        MilestoneType.LAUNCH  -> MaterialSymbol.Rocket
        MilestoneType.LANDING -> MaterialSymbol.FlightLand
        MilestoneType.CURRENT -> MaterialSymbol.Star
        MilestoneType.END     -> MaterialSymbol.Flag
    }
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            AppIconBox(
                symbol = iconSymbol,
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(AppSpacing.sm))
            SectionLabel(milestone.label)
            Text(
                text = milestone.date,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            milestone.sol?.let {
                Text(
                    text = "Sol $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Statistics ────────────────────────────────────────────────────────────────

@Composable
private fun V1Stats(totalPhotos: Int, daysActive: Long, earthDaysActive: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        StatCard(label = "Total Photos", value = formatThousands(totalPhotos), modifier = Modifier.weight(1f))
        StatCard(label = "Sols on Mars", value = formatThousands(daysActive),  modifier = Modifier.weight(1f))
        StatCard(label = "Earth Days",   value = formatThousands(earthDaysActive), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalAlignment = Alignment.Start,
        ) {
            SectionLabel(label)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ── Objectives ────────────────────────────────────────────────────────────────

@Composable
internal fun V1Objectives(objectives: List<String>, twoCol: Boolean = false) {
    if (twoCol) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            objectives.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    pair.forEach { obj ->
                        ObjectiveCard(index = objectives.indexOf(obj), text = obj, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            objectives.forEachIndexed { i, obj -> ObjectiveRow(index = i, text = obj) }
        }
    }
}

@Composable
private fun ObjectiveRow(index: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        NumberCircle(number = index + 1)
        Text(
            text = text,
            style = AppTypography.bodySecondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ObjectiveCard(index: Int, text: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            NumberCircle(number = index + 1)
            Text(
                text = text,
                style = AppTypography.bodySecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NumberCircle(number: Int) {
    Box(
        modifier = Modifier
            .size(AppSpacing.xl + AppSpacing.xs)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

// ── Cameras ───────────────────────────────────────────────────────────────────

@Composable
private fun V1CamerasAccordion(
    cameras: ImmutableList<CameraSpec>,
    onCameraClick: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "accordionArrow",
    )

    AppOutlinedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            AppIconBox(
                symbol = MaterialSymbol.CameraAlt,
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cameras",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${cameras.size} instruments aboard",
                    style = AppTypography.bodySecondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MaterialSymbolIcon(
                symbol = MaterialSymbol.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(arrowRotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.sm))
                Spacer(Modifier.height(AppSpacing.sm))
                cameras.forEach { camera ->
                    CameraItemCard(camera = camera, onClick = { onCameraClick(camera.name) })
                    Spacer(Modifier.height(AppSpacing.sm))
                }
            }
        }
    }
}

@Composable
internal fun CamerasGrid(
    cameras: ImmutableList<CameraSpec>,
    onCameraClick: (String) -> Unit,
) {
    AppOutlinedCard {
        Row(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            AppIconBox(
                symbol = MaterialSymbol.CameraAlt,
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column {
                Text(
                    text = "Cameras & Instruments",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${cameras.size} cameras aboard",
                    style = AppTypography.bodySecondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
        cameras.chunked(2).forEach { pair ->
            Row {
                pair.forEachIndexed { colIdx, camera ->
                    CameraItemCard(
                        camera = camera,
                        onClick = { onCameraClick(camera.name) },
                        modifier = Modifier.weight(1f),
                    )
                    if (colIdx == 0 && pair.size == 2) {
                        VerticalDivider(modifier = Modifier.height(AppSize.cardRadius * 8))
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun CameraItemCard(
    camera: CameraSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = camera.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = camera.fullName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = camera.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Fun Fact ──────────────────────────────────────────────────────────────────

@Composable
internal fun FunFact(text: String, modifier: Modifier = Modifier) {
    AppFactCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSpacing.xl)) {
            SectionLabel("✨  Did You Know?")
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                text = text,
                style = AppTypography.body,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// ── Browse Photos CTA ─────────────────────────────────────────────────────────

@Composable
private fun BrowsePhotosButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
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
        Spacer(Modifier.width(AppSpacing.sm))
        Text(
            text = "Browse Photos",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

// ── Shared screen-level helpers ───────────────────────────────────────────────

/** Small uppercase accent label used inside cards (stat labels, timeline type, etc.). */
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        ),
        color = MaterialTheme.colorScheme.secondary,
    )
}

/** Screen-level section title, e.g. "Mission Timeline". */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.sectionHeader,
        color = MaterialTheme.colorScheme.tertiary,
    )
}
