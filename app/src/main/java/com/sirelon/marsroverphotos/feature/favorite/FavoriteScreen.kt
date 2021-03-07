package com.sirelon.marsroverphotos.feature.favorite

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.feature.MarsImageComposable
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosViewModel
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 01.03.2021 22:32 for Mars-Rover-Photos.
 */
@Composable
fun FavoriteScreen(
    modifier: Modifier,
    activity: AppCompatActivity,
    viewModel: PopularPhotosViewModel = viewModel()
) {
    val items = viewModel.popularPhotos.collectAsLazyPagingItems()
    val context = activity
    FavoritePhotosContent(
        modifier = modifier,
        items = items,
        onFavoriteClick = viewModel::updateFavorite,
        onItemClick = { image ->
            val ids = items.snapshot().mapNotNull { it?.id }
            val intent = ImageActivity.createIntent(context, image.id, ids, false)
            activity.startActivity(intent)
        },
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritePhotosContent(
    modifier: Modifier,
    items: LazyPagingItems<MarsImage>,
    onItemClick: (image: MarsImage) -> Unit,
    onFavoriteClick: (image: MarsImage) -> Unit
) {
    // If I dont call it, paging doesn't work.
//    items.loadState
//    val scrollState = rememberLazyListState()
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), content = {
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
