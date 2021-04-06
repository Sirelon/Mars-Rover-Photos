package com.sirelon.marsroverphotos.feature.favorite

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.feature.MarsImageComposable
import com.sirelon.marsroverphotos.feature.photos.EmptyPhotos
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosViewModel
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 01.03.2021 22:32 for Mars-Rover-Photos.
 */
@Composable
fun FavoriteScreen(
    activity: AppCompatActivity,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: FavoriteImagesViewModel = viewModel()
) {
    val items = viewModel.favoriteImagesFlow.collectAsLazyPagingItems()
    val context = activity
    FavoritePhotosContent(
        modifier = modifier,
        title = stringResource(id = R.string.favorite_title),
        items = items,
        onFavoriteClick = viewModel::updateFavForImage,
        onItemClick = { image ->
            val ids = items.snapshot().mapNotNull { it?.id }
            val intent = ImageActivity.createIntent(context, image.id, ids, false)
            activity.startActivity(intent)
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
    activity: AppCompatActivity,
    modifier: Modifier = Modifier,
    viewModel: PopularPhotosViewModel = viewModel()
) {
    val items = viewModel.popularPhotos.collectAsLazyPagingItems()
    val context = activity
    FavoritePhotosContent(
        modifier = modifier,
        title = stringResource(id = R.string.popular_title),
        items = items,
        onFavoriteClick = viewModel::updateFavorite,
        onItemClick = { image ->
            val ids = items.snapshot().mapNotNull { it?.id }
            val intent = ImageActivity.createIntent(context, image.id, ids, false)
            activity.startActivity(intent)
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
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), content = {
        item { TopAppBar(title = { Text(text = title) }) }

        Log.d("Sirelon", "FavoritePhotosContent() called ${items.loadState}");
        if (items.loadState.refresh == LoadState.Loading) {
            item {
                Column(
                    modifier = Modifier.fillParentMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
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
