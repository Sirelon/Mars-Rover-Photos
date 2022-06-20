package com.sirelon.marsroverphotos.feature.favorite

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.feature.MarsImageComposable
import com.sirelon.marsroverphotos.feature.navigateToImages
import com.sirelon.marsroverphotos.feature.photos.EmptyPhotos
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosViewModel
import com.sirelon.marsroverphotos.storage.MarsImage
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
            navController.navigateToImages(image, items.itemSnapshotList.items)
        },
        emptyContent = {}
    )
}

@Composable
fun FavoritePhotosContent(
    modifier: Modifier,
    title: String,
    items: LazyPagingItems<MarsImage>,
    onItemClick: (image: MarsImage) -> Unit,
    onFavoriteClick: (image: MarsImage) -> Unit,
    emptyContent: @Composable () -> Unit
) {
    if (false) {
        LazyVerticalGrid(columns = GridCells.Fixed(2), content = {
            items(items.itemCount) {
                val image = items[it]
                if (image != null) {
                    MarsImageComposable(
                        marsImage = image,
                        onClick = { onItemClick(image) },
                        onFavoriteClick = { onFavoriteClick(image) })
                } else
                    Image(
                        painter = painterResource(id = R.drawable.img_placeholder),
                        contentDescription = ""
                    )
            }

        })
        return
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), content = {

        item { TopAppBar(title = { Text(text = title) }) }

        Log.d("Sirelon", "FavoritePhotosContent() called ${items.loadState}")
        if (items.loadState.refresh == LoadState.Loading) {
            item {
                CenteredProgress()
            }
        }

        if (items.loadState.append.endOfPaginationReached && items.itemCount == 0) {
            item {
                emptyContent()
            }
        }

        items(items) { image ->
            if (image != null) {
                MarsImageComposable(
                    marsImage = image,
                    onClick = { onItemClick(image) },
                    onFavoriteClick = { onFavoriteClick(image) })
            } else
                Image(
                    painter = painterResource(id = R.drawable.img_placeholder),
                    contentDescription = ""
                )
        }

        if (items.loadState.append == LoadState.Loading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    })
}
