package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.activeStatusColor
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppMetricItem
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.StatusBadge
import com.sirelon.marsroverphotos.presentation.ui.blurb
import com.sirelon.marsroverphotos.presentation.ui.painter
import com.sirelon.marsroverphotos.presentation.viewmodels.RoversViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.metric_label_last
import com.sirelon.marsroverphotos.shared.resources.metric_label_photos
import com.sirelon.marsroverphotos.shared.resources.metric_label_sols
import com.sirelon.marsroverphotos.shared.resources.rovers_search_empty_fmt
import com.sirelon.marsroverphotos.shared.resources.rovers_search_placeholder
import com.sirelon.marsroverphotos.shared.resources.rovers_subtitle_fmt
import com.sirelon.marsroverphotos.shared.resources.rovers_title
import com.sirelon.marsroverphotos.utils.formatCompact
import com.sirelon.marsroverphotos.utils.formatDisplayDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoversScreen(
    onNavigateToPhotos: (Long) -> Unit,
    onMissionInfoClick: (Long) -> Unit
) {
    val viewModel: RoversViewModel = koinViewModel()
    val rovers by viewModel.rovers.collectAsStateWithLifecycle()
    val filteredRovers by viewModel.filteredRovers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    RoversContent(
        rovers = filteredRovers,
        allRovers = rovers,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoversContent(
    rovers: List<Rover>,
    allRovers: List<Rover>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    // 1 column on compact, 2 on medium AND expanded — same adaptive source as the nav suite.
    val columns = if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) 2 else 1
    // Cap+center the whole content column only in the EXPANDED width class (the AboutScreen pattern).
    val expandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val contentWidth = if (expandedWidth) {
        Modifier.widthIn(max = AppSize.contentMaxWidth)
    } else {
        Modifier.fillMaxWidth()
    }

    val colors = MaterialTheme.colorScheme
    val activeCount = allRovers.count { it.status.equals("active", ignoreCase = true) }

    // Search stays hidden behind the top-bar search action until the user opts in.
    var searchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(),
        topBar = {
            AppTopBar(
                title = {
                    if (searchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            leadingIcon = {
                                MaterialSymbolIcon(
                                    symbol = MaterialSymbol.Search,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant
                                )
                            },
                            placeholder = { Text(stringResource(Res.string.rovers_search_placeholder)) }
                        )
                    } else {
                        Text(text = stringResource(Res.string.rovers_title))
                    }
                },
                subtitle = if (searchActive) {
                    null
                } else {
                    {
                        Text(
                            text = stringResource(
                                Res.string.rovers_subtitle_fmt,
                                allRovers.size,
                                activeCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    if (searchActive) {
                        IconButton(onClick = {
                            searchActive = false
                            onSearchQueryChange("")
                        }) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.Close,
                                contentDescription = "Close search"
                            )
                        }
                    } else {
                        IconButton(onClick = { searchActive = true }) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        // Focus the field the moment search mode turns on so the keyboard appears immediately.
        LaunchedEffect(searchActive) {
            if (searchActive) focusRequester.requestFocus()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (rovers.isEmpty() && searchQuery.isNotBlank()) {
                AppEmptyState(
                    title = stringResource(Res.string.rovers_search_empty_fmt, searchQuery),
                    showImage = false,
                    modifier = contentWidth
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = contentWidth.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.md,
                        end = AppSpacing.md,
                        top = innerPadding.calculateTopPadding() + AppSpacing.sm,
                        bottom = innerPadding.calculateBottomPadding() + AppSpacing.sm
                    ),
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
        modifier = modifier,
        onClick = { onClick(rover) },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Image(
                    painter = rover.painter(),
                    contentDescription = rover.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(AppSize.roverThumbWidth)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.medium)
                )
                Column(modifier = Modifier.weight(1f)) {
                    TitleLine(rover)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text(
                        text = rover.blurb(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    HorizontalDivider(
                        thickness = AppSize.hairline,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    MetricStrip(rover)
                }
            }
            IconButton(
                onClick = { onMissionInfoClick(rover) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Info,
                    contentDescription = "Mission Info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleLine(rover: Rover) {
    val active = rover.status.equals("active", ignoreCase = true)
    // FlowRow so a long rover name + status chip wrap gracefully instead of truncating tight.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = AppSize.roverInfoReserve),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text = rover.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        StatusBadge(
            label = if (active) "Active" else "Complete",
            color = if (active) activeStatusColor() else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricStrip(rover: Rover) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        AppMetricItem(
            symbol = MaterialSymbol.Collections,
            value = formatCompact(rover.totalPhotos),
            label = stringResource(Res.string.metric_label_photos)
        )
        AppMetricItem(
            symbol = MaterialSymbol.Schedule,
            value = formatCompact(rover.maxSol),
            label = stringResource(Res.string.metric_label_sols)
        )
        AppMetricItem(
            symbol = MaterialSymbol.Event,
            value = formatDisplayDate(rover.maxDate),
            label = stringResource(Res.string.metric_label_last)
        )
    }
}
