package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.mission.CameraSpec
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.presentation.theme.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.presentation.theme.activeStatusColor
import com.sirelon.marsroverphotos.presentation.ui.AppBadge
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import com.sirelon.marsroverphotos.presentation.ui.AppFactCard
import com.sirelon.marsroverphotos.presentation.ui.AppIconBox
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedCard
import com.sirelon.marsroverphotos.presentation.ui.AppSectionHeader
import com.sirelon.marsroverphotos.presentation.ui.AppSectionLabel
import com.sirelon.marsroverphotos.presentation.ui.BadgeRow
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.StatusBadge
import com.sirelon.marsroverphotos.presentation.ui.painter
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverBadge
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverCameras
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverFunFact
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverImage
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverName
import com.sirelon.marsroverphotos.presentation.ui.sharedRoverObjectives
import com.sirelon.marsroverphotos.presentation.viewmodels.MilestoneType
import com.sirelon.marsroverphotos.presentation.viewmodels.MissionInfoState
import com.sirelon.marsroverphotos.presentation.viewmodels.TimelineMilestone
import com.sirelon.marsroverphotos.utils.formatCompact
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
        // The nav chrome (ad slot + bottom bar) is laid out *below* the content area by
        // MarsNavigationSuite, so it never overlaps this list; the trailing space is only
        // breathing room under the last section.
        contentPadding = PaddingValues(bottom = AppSpacing.x3l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        // Full-bleed hero — no horizontal padding
        item {
            MissionHeroSection(
                rover = state.rover,
                landingLocation = state.landingLocation,
                onBack = onBack,
            )
        }

        // Mission description paragraph — just below the hero
        if (state.missionDescription.isNotEmpty()) {
            item {
                Text(
                    text = state.missionDescription,
                    style = AppTypography.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            ) {
                AppSectionHeader("Mission Timeline")
                MissionTimeline(milestones = state.timelineMilestones)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                AppSectionHeader("Statistics")
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
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    AppSectionHeader("Cameras & Instruments")
                    if (isMediumPlus) {
                        CamerasGrid(
                            cameras = state.cameras,
                            onCameraClick = onCameraClick,
                            modifier = Modifier.sharedRoverCameras(state.rover.id),
                        )
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
                    AppSectionHeader("Mission Info")
                    Spacer(Modifier.height(AppSpacing.md))
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
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        ) {
                            AppSectionHeader("Mission Objectives")
                            V1Objectives(
                                objectives = state.missionFacts.objectives,
                                twoCol = isMediumPlus,
                                modifier = Modifier.sharedRoverObjectives(state.rover.id),
                            )
                        }
                    }
                }

                state.missionFacts.funFacts.randomOrNull()?.let { fact ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        ) {
                            AppSectionHeader("Did You Know?")
                            FunFact(text = fact, modifier = Modifier.sharedRoverFunFact(state.rover.id))
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

/** Mission hero photo proportions — the rover portraits are 16:9 crops. */
private const val HeroAspectRatio = 16f / 9f

/**
 * Rover photo under a [scrim]. The caller sizes it (aspect ratio or fixed height) and may supply its
 * own scrim; the default fades the bottom of the image into the surrounding background.
 */
@Composable
internal fun RoverImageCard(
    rover: Rover,
    modifier: Modifier = Modifier,
    scrim: Brush? = null,
) {
    val bgColor = MaterialTheme.colorScheme.background
    val defaultScrim = remember(bgColor) {
        Brush.verticalGradient(
            0.45f to Color.Transparent,
            1f to bgColor.copy(alpha = 0.96f),
        )
    }
    Box(modifier = modifier) {
        Image(
            painter = rover.painter(),
            contentDescription = rover.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(modifier = Modifier.fillMaxSize().background(scrim ?: defaultScrim))
    }
}

@Composable
private fun MissionHeroSection(
    rover: Rover,
    landingLocation: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // The hero is a photo with text laid over it — a permanently dark region whatever the app theme
    // is, so it runs on the dark theme and everything inside reads `colorScheme` normally instead of
    // hardcoding white. Forcing the theme (not the palette) keeps the brand overrides and dynamic
    // color — see docs/DESIGN_SYSTEM.md › Color.
    MarsRoverPhotosTheme(darkTheme = true) {
        val colors = MaterialTheme.colorScheme
        val heroScrim = remember(colors.scrim, colors.background) {
            Brush.verticalGradient(
                0f to colors.scrim.copy(alpha = 0.52f),
                0.4f to Color.Transparent,
                1f to colors.background.copy(alpha = 0.97f),
            )
        }
        Box(modifier = modifier.fillMaxWidth()) {
            RoverImageCard(
                rover = rover,
                scrim = heroScrim,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HeroAspectRatio)
                    .sharedRoverImage(rover.id),
            )
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(AppSpacing.lg)
                        .background(colors.scrim.copy(alpha = 0.42f), CircleShape),
                ) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onSurface,
                        size = AppSize.iconDefault,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = AppSpacing.lg,
                        end = AppSpacing.lg,
                        bottom = AppSpacing.lg,
                    ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Text(
                    text = rover.name,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedRoverName(rover.id),
                )
                Row(
                    modifier = Modifier.sharedRoverBadge(rover.id),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    if (rover.isActive) StatusBadge("Active", activeStatusColor())
                    else AppBadge(rover.status.replaceFirstChar { it.uppercaseChar() })
                    if (landingLocation.isNotEmpty()) {
                        Text(
                            text = landingLocation,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ── Timeline ──────────────────────────────────────────────────────────────────

/**
 * Vertical mission timeline — one circular icon node per milestone, joined by a 2dp rail, with the
 * label / date / "Sol X · Location" sub-line stacked beside it. Used by every window size: it wraps
 * instead of scrolling sideways, so nothing clips at compact widths.
 */
@Composable
internal fun MissionTimeline(
    milestones: ImmutableList<TimelineMilestone>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        milestones.forEachIndexed { i, milestone ->
            val isCurrent = milestone.type == MilestoneType.CURRENT || milestone.type == MilestoneType.END
            val isLast = i == milestones.size - 1
            // The row is measured to its tallest child so the rail can stretch between nodes.
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier.width(AppSize.iconBox),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppSize.iconBox)
                            .clip(CircleShape)
                            .background(if (isCurrent) colors.secondary else colors.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        MaterialSymbolIcon(
                            symbol = milestone.type.symbol(),
                            contentDescription = null,
                            tint = if (isCurrent) colors.onSecondary else colors.onSecondaryContainer,
                            size = AppSize.icon,
                        )
                    }
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(AppSize.hairline * 2)
                                .weight(1f)
                                .background(colors.outlineVariant)
                        )
                    }
                }
                Spacer(Modifier.width(AppSpacing.lg))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isLast) Modifier else Modifier.padding(bottom = AppSpacing.xl)),
                ) {
                    AppSectionLabel(milestone.label)
                    Text(
                        text = milestone.date,
                        style = AppTypography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val subLabel = milestone.subLabel()
                    if (subLabel != null) {
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun MilestoneType.symbol(): MaterialSymbol = when (this) {
    MilestoneType.LAUNCH  -> MaterialSymbol.Rocket
    MilestoneType.LANDING -> MaterialSymbol.FlightLand
    MilestoneType.CURRENT -> MaterialSymbol.Star
    MilestoneType.END     -> MaterialSymbol.Flag
}

/** "Sol X · Location" sub-line, or null when the milestone has neither. */
private fun TimelineMilestone.subLabel(): String? =
    listOfNotNull(sol?.let { "Sol ${formatThousands(it)}" }, location)
        .joinToString(" · ")
        .takeIf { it.isNotEmpty() }

// ── Statistics ────────────────────────────────────────────────────────────────

@Composable
private fun V1Stats(totalPhotos: Int, daysActive: Long, earthDaysActive: Long, modifier: Modifier = Modifier) {
    // Photo counts run into the millions, so they're abbreviated (1.0M); sol/day counts stay exact
    // and grouped (1,945). Both fit one line in a third of a compact screen.
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        StatCard("Total Photos", formatCompact(totalPhotos), Modifier.weight(1f).fillMaxHeight())
        StatCard("Sols on Mars", formatThousands(daysActive), Modifier.weight(1f).fillMaxHeight())
        StatCard("Earth Days", formatThousands(earthDaysActive), Modifier.weight(1f).fillMaxHeight())
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
            AppSectionLabel(label)
            // One line, always — a wrapped value would make the three cards different heights.
            Text(
                text = value,
                style = AppTypography.statValue,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

// ── Objectives ────────────────────────────────────────────────────────────────

@Composable
internal fun V1Objectives(objectives: List<String>, twoCol: Boolean = false, modifier: Modifier = Modifier) {
    if (twoCol) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            objectives.chunked(2).forEachIndexed { chunkIdx, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    pair.forEachIndexed { colIdx, obj ->
                        ObjectiveCard(index = chunkIdx * 2 + colIdx, text = obj, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            objectives.forEachIndexed { i, obj -> ObjectiveRow(index = i, text = obj) }
        }
    }
}

@Composable
private fun ObjectiveRow(index: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
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
    val arrowRotation = animateFloatAsState(
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
                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation.value },
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
    modifier: Modifier = Modifier,
) {
    AppOutlinedCard(modifier = modifier) {
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
            AppSectionLabel("✨  Did You Know?")
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
