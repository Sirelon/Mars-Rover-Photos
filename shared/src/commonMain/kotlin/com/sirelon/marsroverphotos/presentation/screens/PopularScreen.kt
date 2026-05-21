package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Popular photos screen.
 * Displays the most popular Mars photos ranked by community engagement.
 * On Android, shows real Firebase data. On iOS and Desktop, degrades to an empty state
 * until platform Firebase support is fully active.
 *
 * Created for KMP migration — Ticket S5.
 */
@Composable
fun PopularScreen(
    onNavigateToImages: (selected: MarsImage, all: List<MarsImage>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PopularPhotosViewModel = koinViewModel(),
) {
    val appSettings: AppSettings = koinInject()
    val items by viewModel.popularPhotos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    PopularPhotosContent(
        modifier = modifier,
        title = stringResource(Res.string.popular_title),
        items = items,
        isLoading = isLoading,
        appSettings = appSettings,
        onFavoriteClick = { viewModel.updateFavorite(it) },
        onItemClick = { image -> onNavigateToImages(image, items) },
        onRetry = { viewModel.loadPopularPhotos() },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
private fun PopularPhotosContent(
    modifier: Modifier,
    title: String,
    items: List<MarsImage>,
    isLoading: Boolean,
    appSettings: AppSettings,
    onItemClick: (image: MarsImage) -> Unit,
    onFavoriteClick: (image: MarsImage) -> Unit,
    onRetry: () -> Unit,
) {
    val gridViewState by appSettings.gridViewFlow.collectAsState()
    var gridView by rememberSaveable { mutableStateOf(gridViewState) }

    Column(modifier = Modifier.fillMaxSize()) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        TopAppBar(
            title = { Text(text = title) },
            windowInsets = WindowInsets(0, 0, 0, 0),
            actions = {
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
            },
            scrollBehavior = scrollBehavior,
        )

        when {
            isLoading && items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            items.isEmpty() -> {
                PopularEmptyContent(
                    title = stringResource(Res.string.popular_empty_title),
                    onRetry = onRetry,
                )
            }

            else -> {
                val spacedBy = Arrangement.spacedBy(8.dp)
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
                    horizontalArrangement = spacedBy,
                    content = {
                        items(
                            count = items.size,
                            key = { items[it].id.ifEmpty { Uuid.random().toString() } },
                            contentType = { "MarsImageComposable" }
                        ) { index ->
                            val image = items[index]
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}
