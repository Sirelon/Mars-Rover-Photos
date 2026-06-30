package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.compactCount
import com.sirelon.marsroverphotos.presentation.ui.sharedPhoto
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.img_placeholder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.PopularPhotosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularScreen(
    onNavigateToImages: (selected: MarsImage) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PopularPhotosViewModel = koinViewModel(),
) {
    // Keeps the PopularRemoteMediator alive so Firebase data flows into Room.
    val lazyPagingItems = viewModel.popularPagedFlow.collectAsLazyPagingItems()
    val items by viewModel.popularImages.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        val targetId = viewModel.consumeLastViewedPhotoId() ?: return@LaunchedEffect
        // Wait until items loads from Room (initial StateFlow value is null)
        val itemList = withTimeoutOrNull(SCROLL_RESTORE_TIMEOUT_MS) {
            snapshotFlow { items }.first { it != null }
        } ?: return@LaunchedEffect
        val index = itemList.indexOfFirst { it.id == targetId }
        if (index < 0) return@LaunchedEffect
        val visibleKeys = withTimeoutOrNull(SCROLL_RESTORE_TIMEOUT_MS) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.key } }
                .first { it.isNotEmpty() }
        }.orEmpty()
        if (targetId !in visibleKeys) {
            // LazyGrid offset: 0=hero, 1=runner-up#2, 2=runner-up#3, 3=divider, 4+=grid
            val gridIndex = when (index) {
                0 -> 0; 1 -> 1; 2 -> 2; else -> index + 1
            }
            gridState.scrollToItem(gridIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = { Text("Popular", style = MaterialTheme.typography.headlineMedium) },
            subtitle = {
                Text(
                    "The most-viewed Mars photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            scrollBehavior = scrollBehavior,
        )

        val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading && items == null
        val isError = lazyPagingItems.loadState.refresh is LoadState.Error && items?.isEmpty() == true

        when {
            isRefreshing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            isError -> {
                AppEmptyState(
                    title = "No popular photos available.\nCheck back later!",
                    showImage = false,
                    action = {
                        AppButton(onClick = { lazyPagingItems.retry() }) { Text("Retry") }
                    },
                )
            }
            items.isNullOrEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        horizontal = AppSpacing.md,
                        vertical = AppSpacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    val photoList = items!!

                    // #1 Hero (full span)
                    if (photoList.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HeroCard(
                                photo = photoList[0],
                                onFavoriteClick = { viewModel.updateFavorite(photoList[0]) },
                                onClick = { onNavigateToImages(photoList[0]) },
                            )
                        }
                    }

                    // #2 and #3 runner-up (each span=1)
                    if (photoList.size > 1) {
                        item {
                            RunnerUpCard(
                                photo = photoList[1],
                                rank = 2,
                                onFavoriteClick = { viewModel.updateFavorite(photoList[1]) },
                                onClick = { onNavigateToImages(photoList[1]) },
                            )
                        }
                    }
                    if (photoList.size > 2) {
                        item {
                            RunnerUpCard(
                                photo = photoList[2],
                                rank = 3,
                                onFavoriteClick = { viewModel.updateFavorite(photoList[2]) },
                                onClick = { onNavigateToImages(photoList[2]) },
                            )
                        }
                    }

                    // Section divider (full span)
                    if (photoList.size > 3) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PopularSectionDivider()
                        }
                        // Grid items #4+ (span=1 each)
                        itemsIndexed(photoList.drop(3), key = { _, photo -> photo.id }) { index, photo ->
                            val rank = index + 4
                            PopularGridCard(
                                photo = photo,
                                rank = rank,
                                onFavoriteClick = { viewModel.updateFavorite(photo) },
                                onClick = { onNavigateToImages(photo) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    photo: MarsImage,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppSize.cardRadius))
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
                .aspectRatio(16f / 9f)
                .sharedPhoto(photo.id),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(Res.drawable.img_placeholder),
        )
        // Dual vignette: top-left corner (for badge) + bottom (for info)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color(0xD9000000),
                    )
                )
        )
        // #1 badge — top start
        Row(
            modifier = Modifier
                .padding(AppSpacing.md)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.LocalFireDepartment,
                contentDescription = null,
                size = 14.dp,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "#1",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        // Fav button — top end
        PopularFavButton(
            isFav = photo.favorite,
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
        )
        // Bottom info strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    photo.camera?.name?.let { camName ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = camName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(
                        text = "Most Viewed",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0x7A000000))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.Visibility,
                            contentDescription = null,
                            size = 13.dp,
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                        Text(
                            text = compactCount(photo.stats.see),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "Sol ${photo.sol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RunnerUpCard(
    photo: MarsImage,
    rank: Int,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
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
                        0.5f to Color.Transparent,
                        1f to Color(0xC5000000),
                    )
                )
        )
        // Rank badge — top start
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0x8C000000))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        // Fav button — top end
        PopularFavButton(
            isFav = photo.favorite,
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            size = 28.dp,
            iconSize = 13.dp,
        )
        // Bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Text(
                text = photo.camera?.name ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = photo.camera?.name?.take(8) ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0x7A000000))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.Visibility,
                        contentDescription = null,
                        size = 11.dp,
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
    }
}

@Composable
private fun PopularSectionDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Text(
            text = "MORE POPULAR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun PopularGridCard(
    photo: MarsImage,
    rank: Int,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
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
                        1f to Color(0xB0000000),
                    )
                )
        )
        // Rank badge — top start
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(7.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x8C000000))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
        // Fav button — top end
        PopularFavButton(
            isFav = photo.favorite,
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            size = 26.dp,
            iconSize = 12.dp,
        )
        // Bottom row: rover name + view count
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = photo.camera?.name?.take(6) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x7A000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Visibility,
                    contentDescription = null,
                    size = 10.dp,
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
}

@Composable
private fun PopularFavButton(
    isFav: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 30.dp,
    iconSize: androidx.compose.ui.unit.Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x6B000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Favorite,
            contentDescription = if (isFav) "Unlike" else "Like",
            size = iconSize,
            tint = if (isFav) MaterialTheme.colorScheme.primary else Color.White,
            filled = isFav,
        )
    }
}
