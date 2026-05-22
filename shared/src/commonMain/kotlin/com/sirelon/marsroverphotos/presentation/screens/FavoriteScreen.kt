package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.presentation.ui.CenteredColumn
import com.sirelon.marsroverphotos.presentation.ui.MarsImageComposable
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.viewmodels.FavoriteImagesViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import com.sirelon.marsroverphotos.shared.resources.favorite_empty_btn
import com.sirelon.marsroverphotos.shared.resources.favorite_empty_title
import com.sirelon.marsroverphotos.shared.resources.favorite_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Favorite images screen.
 * Displays the user's favorite Mars photos with paging support backed by Room.
 *
 * Created on 01.03.2021 22:32 for Mars-Rover-Photos.
 * Ported to Compose Multiplatform (KMP).
 */
@Composable
fun FavoriteScreen(
    onNavigateToImages: (selected: MarsImage, all: List<MarsImage>) -> Unit,
    onNavigateToRovers: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoriteImagesViewModel = koinViewModel()
) {
    val appSettings: AppSettings = koinInject()
    val lazyPagingItems = viewModel.favoritePagedFlow.collectAsLazyPagingItems()

    FavoritePhotosContent(
        modifier = modifier,
        title = stringResource(Res.string.favorite_title),
        lazyPagingItems = lazyPagingItems,
        appSettings = appSettings,
        onFavoriteClick = { viewModel.updateFavForImage(it) },
        onItemClick = { image ->
            val loadedItems = (0 until lazyPagingItems.itemCount).mapNotNull { lazyPagingItems.peek(it) }
            onNavigateToImages(image, loadedItems)
        },
        emptyContent = {
            FavoriteEmptyContent(
                title = stringResource(Res.string.favorite_empty_title),
                btnTitle = stringResource(Res.string.favorite_empty_btn),
                onBtnClick = {
                    viewModel.track("click_empty_favorite")
                    onNavigateToRovers()
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritePhotosContent(
    modifier: Modifier,
    title: String,
    lazyPagingItems: LazyPagingItems<MarsImage>,
    appSettings: AppSettings,
    onItemClick: (image: MarsImage) -> Unit,
    onFavoriteClick: (image: MarsImage) -> Unit,
    emptyContent: @Composable () -> Unit
) {
    val gridViewState by appSettings.gridViewFlow.collectAsState()
    var gridView by rememberSaveable { mutableStateOf(gridViewState) }

    Column(modifier = Modifier.fillMaxSize()) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        TopAppBar(
            title = { Text(text = title) },
            windowInsets = WindowInsets(0, 0, 0, 0),
            actions = {
                if (lazyPagingItems.itemCount > 0) {
                    IconButton(
                        onClick = {
                            gridView = !gridView
                            appSettings.gridView = gridView
                        },
                    ) {
                        if (gridView) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.ViewList,
                                contentDescription = "Change to List View",
                            )
                        } else {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.GridView,
                                contentDescription = "Change to Grid View",
                            )
                        }
                    }
                }
            },
            scrollBehavior = scrollBehavior,
        )

        when {
            lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            lazyPagingItems.itemCount == 0 -> {
                emptyContent()
            }

            else -> {
                LazyVerticalStaggeredGrid(
                    modifier = modifier
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    columns = if (gridView) {
                        StaggeredGridCells.Adaptive(minSize = 180.dp)
                    } else {
                        StaggeredGridCells.Fixed(1)
                    },
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey { it.id },
                            contentType = lazyPagingItems.itemContentType { "MarsImageComposable" }
                        ) { index ->
                            val image = lazyPagingItems[index] ?: return@items
                            MarsImageComposable(
                                modifier = Modifier,
                                marsImage = image,
                                onClick = { onItemClick(image) },
                                onFavoriteClick = { onFavoriteClick(image) }
                            )
                        }
                        if (lazyPagingItems.loadState.append is LoadState.Loading) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LoadingMoreItem()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteEmptyContent(
    title: String,
    btnTitle: String,
    onBtnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredColumn(modifier = modifier) {
        Image(
            painter = painterResource(Res.drawable.alien_icon),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBtnClick) {
            Text(text = btnTitle)
        }
    }
}

@Composable
private fun LoadingMoreItem(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Loading more photos...")
        }
    }
}
