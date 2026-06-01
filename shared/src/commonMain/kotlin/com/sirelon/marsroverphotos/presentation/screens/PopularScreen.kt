package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.CircularProgressIndicator
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.presentation.ui.CenteredColumn
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.MarsImageComposable
import com.sirelon.marsroverphotos.presentation.viewmodels.PopularPhotosViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.popular_empty_title
import com.sirelon.marsroverphotos.shared.resources.popular_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Popular photos screen.
 * Displays the most popular Mars photos ranked by community engagement.
 * Data is fetched from Firebase via [PopularRemoteMediator] and cached in Room,
 * then surfaced to the UI as paged items — works on Android, iOS, and Desktop.
 *
 * Created for KMP migration — Ticket S5.
 */
@Composable
fun PopularScreen(
    onNavigateToImages: (selected: MarsImage) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PopularPhotosViewModel = koinViewModel(),
) {
    val appSettings: AppSettings = koinInject()
    val gridView by appSettings.gridViewFlow.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.popularPagedFlow.collectAsLazyPagingItems()

    PopularPhotosContent(
        modifier = modifier,
        title = stringResource(Res.string.popular_title),
        lazyPagingItems = lazyPagingItems,
        gridView = gridView,
        onGridChange = { appSettings.gridView = !gridView },
        onFavoriteClick = { viewModel.updateFavorite(it) },
        onItemClick = onNavigateToImages,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularPhotosContent(
    modifier: Modifier = Modifier,
    title: String,
    lazyPagingItems: LazyPagingItems<MarsImage>,
    gridView: Boolean,
    onGridChange: () -> Unit,
    onItemClick: (image: MarsImage) -> Unit,
    onFavoriteClick: (image: MarsImage) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        AppTopBar(
            title = { Text(text = title) },
            windowInsets = WindowInsets(0, 0, 0, 0),
            actions = {
                IconButton(
                    onClick = onGridChange,
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
            },
            scrollBehavior = scrollBehavior,
        )

        when {
            lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            lazyPagingItems.loadState.refresh is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
                PopularEmptyContent(
                    title = stringResource(Res.string.popular_empty_title),
                    onRetry = { lazyPagingItems.retry() },
                )
            }

            lazyPagingItems.itemCount == 0 -> {
                PopularEmptyContent(
                    title = stringResource(Res.string.popular_empty_title),
                    onRetry = { lazyPagingItems.refresh() },
                )
            }

            else -> {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    columns = if (gridView) {
                        StaggeredGridCells.Adaptive(minSize = 180.dp)
                    } else {
                        StaggeredGridCells.Fixed(1)
                    },
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
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
                    }
                )
            }
        }
    }
}

@Composable
private fun PopularEmptyContent(
    title: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredColumn(modifier = modifier) {
        Text(
            text = title,
            style = AppTypography.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        AppButton(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}
