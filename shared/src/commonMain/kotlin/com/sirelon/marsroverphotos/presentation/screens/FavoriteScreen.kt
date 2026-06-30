package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.repositories.FavoriteSortOrder
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.compactCount
import com.sirelon.marsroverphotos.presentation.ui.sharedPhoto
import com.sirelon.marsroverphotos.presentation.viewmodels.FavoriteImagesViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.img_placeholder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FavoriteScreen(
    onNavigateToImages: (selected: MarsImage) -> Unit,
    onNavigateToRovers: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoriteImagesViewModel = koinViewModel()
) {
    val items = viewModel.favoritePagedFlow.collectAsLazyPagingItems()
    val chips by viewModel.roverChips.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val roverFilter by viewModel.roverFilter.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        val targetId = viewModel.consumeLastViewedPhotoId() ?: return@LaunchedEffect
        // Query DB directly for the sorted index — avoids the peek() limitation where
        // items beyond the initial paging buffer would never be found.
        val index = viewModel.findScrollIndex(targetId)
        if (index < 0) return@LaunchedEffect
        val visibleKeys = withTimeoutOrNull(SCROLL_RESTORE_TIMEOUT_MS) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.key } }
                .first { it.isNotEmpty() }
        }.orEmpty()
        // stats row is always present; chips row only when more than one rover chip exists.
        val headerCount = if (chips.size > 1) 2 else 1
        if (targetId !in visibleKeys) gridState.scrollToItem((index + headerCount).coerceAtLeast(0))
    }

    FavoritePhotosContent(
        modifier = modifier,
        items = items,
        chips = chips,
        stats = stats,
        sortOrder = sortOrder,
        roverFilter = roverFilter,
        gridState = gridState,
        onFavoriteClick = { viewModel.updateFavForImage(it) },
        onItemClick = onNavigateToImages,
        onSortChange = { viewModel.sortOrder.value = it },
        onRoverFilterChange = { viewModel.roverFilter.value = it },
        onNavigateToRovers = {
            viewModel.track("click_empty_favorite")
            onNavigateToRovers()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritePhotosContent(
    modifier: Modifier = Modifier,
    items: LazyPagingItems<MarsImage>,
    chips: List<FavoriteImagesViewModel.RoverChip>,
    stats: FavoriteImagesViewModel.FavoriteStats,
    sortOrder: FavoriteSortOrder,
    roverFilter: Long?,
    gridState: LazyGridState,
    onItemClick: (MarsImage) -> Unit,
    onFavoriteClick: (MarsImage) -> Unit,
    onSortChange: (FavoriteSortOrder) -> Unit,
    onRoverFilterChange: (Long?) -> Unit,
    onNavigateToRovers: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val isAllEmpty = stats.savedCount == 0
    val isFilterEmpty = !isAllEmpty && items.itemCount == 0 && items.loadState.refresh !is LoadState.Loading

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = { Text(text = "Favorites", style = MaterialTheme.typography.headlineMedium) },
                subtitle = {
                    Text(
                        "Photos you saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    if (!isAllEmpty) {
                        Box {
                            FilterChip(
                                selected = sortOrder != FavoriteSortOrder.Recent,
                                onClick = { sortMenuExpanded = true },
                                label = {
                                    Text(
                                        sortOrder.label,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                leadingIcon = {
                                    MaterialSymbolIcon(
                                        symbol = MaterialSymbol.Tune,
                                        contentDescription = "Sort",
                                        size = 16.dp,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                FavoriteSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            onSortChange(order)
                                            sortMenuExpanded = false
                                        },
                                        trailingIcon = if (order == sortOrder) {
                                            {
                                                MaterialSymbolIcon(
                                                    symbol = MaterialSymbol.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    size = 16.dp,
                                                )
                                            }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (isAllEmpty) {
            AppEmptyState(
                title = "No favorite photos yet.\nYou can save any photos you like.\nJust mark them as \"favorite\".",
                modifier = Modifier.padding(innerPadding),
                action = {
                    AppButton(onClick = onNavigateToRovers) { Text("Go to rovers") }
                },
            )
        } else {
            LazyVerticalGrid(
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(
                    start = AppSpacing.md,
                    end = AppSpacing.md,
                    top = innerPadding.calculateTopPadding() + AppSpacing.sm,
                    bottom = innerPadding.calculateBottomPadding() + AppSpacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FavoriteStatsRow(stats)
                }
                if (chips.size > 1) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FavoriteRoverChips(
                            chips = chips,
                            activeRoverId = roverFilter,
                            onChipClick = onRoverFilterChange,
                        )
                    }
                }
                if (isFilterEmpty) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AppEmptyState(
                            title = "No photos saved for this rover yet.",
                            showImage = false,
                            action = {
                                AppButton(onClick = { onRoverFilterChange(null) }) {
                                    Text("Show all")
                                }
                            },
                        )
                    }
                } else {
                    items(
                        count = items.itemCount,
                        key = { index -> items.peek(index)?.id ?: index },
                    ) { index ->
                        val image = items[index] ?: return@items
                        FavoritePhotoCard(
                            photo = image,
                            onClick = { onItemClick(image) },
                            onFavoriteClick = { onFavoriteClick(image) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritePhotoCard(
    photo: MarsImage,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(photo.imageUrl)
                .crossfade(true)
                .memoryCacheKey("photo_${photo.id}")
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .sharedPhoto(photo.id),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(Res.drawable.img_placeholder),
        )
        // Bottom vignette
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color(0xB8000000),
                    )
                )
        )
        // Heart button — top end
        FavoriteCardHeartButton(
            isFav = photo.favorite,
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        )
        // Bottom: view count
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Visibility,
                contentDescription = null,
                size = 12.dp,
                tint = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = compactCount(photo.stats.see),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun FavoriteCardHeartButton(
    isFav: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isFav) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f) else Color(0x6B000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Favorite,
            contentDescription = if (isFav) "Unlike" else "Like",
            size = 20.dp,
            tint = if (isFav) MaterialTheme.colorScheme.error else Color.White,
            filled = isFav,
        )
    }
}

@Composable
private fun FavoriteStatsRow(
    stats: FavoriteImagesViewModel.FavoriteStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            size = 14.dp,
        )
        Text(
            text = "${stats.savedCount} saved",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Rocket,
            contentDescription = null,
            size = 14.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${stats.roverCount} rover${if (stats.roverCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MaterialSymbolIcon(
            symbol = MaterialSymbol.CameraAlt,
            contentDescription = null,
            size = 14.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${stats.cameraCount} camera${if (stats.cameraCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteRoverChips(
    chips: List<FavoriteImagesViewModel.RoverChip>,
    activeRoverId: Long?,
    onChipClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        chips.forEach { chip ->
            val selected = chip.roverId == activeRoverId
            FilterChip(
                selected = selected,
                onClick = { onChipClick(chip.roverId) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    ) {
                        Text(chip.name, style = MaterialTheme.typography.labelLarge)
                        if (chip.count > 0) {
                            Text(
                                text = chip.count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                },
            )
        }
    }
}
