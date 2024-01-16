package com.sirelon.marsroverphotos.feature.favorite

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.feature.MarsImageComposable
import com.sirelon.marsroverphotos.feature.navigateToImages
import com.sirelon.marsroverphotos.feature.photos.EmptyPhotos
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosViewModel
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.storage.Prefs
import com.sirelon.marsroverphotos.ui.CenteredProgress

/**
 * Created on 01.03.2021 22:32 for Mars-Rover-Photos.
 */
@Composable
fun FavoriteScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: FavoriteImagesViewModel = viewModel()
) {
    val items = viewModel.favoriteImagesFlow.collectAsLazyPagingItems()
    FavoritePhotosContent(
        modifier = modifier,
        title = stringResource(id = R.string.favorite_title),
        items = items,
        onFavoriteClick = {
            // ToDO: store favorite in another table
            viewModel.updateFavForImage(it)
        },
        onItemClick = { image ->
            navController.navigateToImages(image, items.itemSnapshotList.items)
        },
        emptyContent = {
            EmptyPhotos(
                title = stringResource(id = R.string.favorite_empty_title),
                btnTitle = stringResource(id = R.string.favorite_empty_btn)
            ) {
                viewModel.track("click_empty_favorite")
                navController.navigate("rovers")
            }
        }
    )
}

@Composable
fun PopularScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PopularPhotosViewModel = viewModel()
) {
    val items = viewModel.popularPhotos.collectAsLazyPagingItems()
    FavoritePhotosContent(
        modifier = modifier,
        title = stringResource(id = R.string.popular_title),
        items = items,
        onFavoriteClick = viewModel::updateFavorite,
        onItemClick = { image ->
            navController.navigateToImages(
                image = image,
                allphotos = items.itemSnapshotList.items,
                trackingEnabled = false
            )
        },
        emptyContent = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePhotosContent(
    modifier: Modifier,
    title: String,
    items: LazyPagingItems<MarsImage>,
    onItemClick: (image: MarsImage) -> Unit,
    onFavoriteClick: (image: MarsImage) -> Unit,
    emptyContent: @Composable () -> Unit
) {

    var gridView by rememberSaveable {
        mutableStateOf(Prefs.gridView)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        TopAppBar(
            title = { Text(text = title) },
            windowInsets = WindowInsets(0,0,0,0),
            actions = {
                IconButton(
                    onClick = {
                        gridView = !gridView
                        Prefs.gridView = gridView
                    },
                ) {
                    if (gridView) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Change to List View",
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Change to Grid View",
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior,
        )

        if (items.loadState.refresh == LoadState.Loading) {
            CenteredProgress()
        }

        if (items.loadState.append.endOfPaginationReached && items.itemCount == 0) {
            emptyContent()
        }

        val spacedBy = Arrangement.spacedBy(16.dp)
        LazyVerticalStaggeredGrid(
            modifier = modifier
                .weight(1f)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
            ,
            contentPadding = PaddingValues(16.dp),
            columns = if (gridView) {
                StaggeredGridCells.Fixed(2)
            } else {
                StaggeredGridCells.Fixed(1)
            },
            verticalItemSpacing = 16.dp,
            horizontalArrangement = spacedBy,
            content = {
                items(items.itemCount) {
                    val image = items[it]
                    if (image != null) {
                        MarsImageComposable(
                            // TODO: Animation
                            modifier = Modifier,
                            marsImage = image,
                            onClick = { onItemClick(image) },
                            onFavoriteClick = { onFavoriteClick(image) })
                    } else
                        Image(
                            painter = painterResource(id = R.drawable.img_placeholder),
                            contentDescription = ""
                        )
                }
            }
        )

        if (items.loadState.append == LoadState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
