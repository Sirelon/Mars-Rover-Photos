package com.sirelon.marsroverphotos.feature.images

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.sirelon.marsroverphotos.feature.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.feature.MultitouchDetector
import com.sirelon.marsroverphotos.feature.MultitouchState
import com.sirelon.marsroverphotos.feature.NetworkImage
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress

/**
 * Created on 13.04.2021 22:52 for Mars-Rover-Photos.
 */

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun ImageScreen(
    viewModel: ImageViewModel = viewModel(),
    photoIds: List<String>?,
    selectedId: String?
) {
    val ids = photoIds ?: emptyList()
    Log.d("Sirelon", "ImageScreen() called with: photoIds = $photoIds, selectedId = $selectedId");
    viewModel.setIdsToShow(ids)

    val selectedPosition = ids.indexOf(selectedId)

    val imagesA by viewModel.imagesLiveData.observeAsState()
    val images = imagesA

    Crossfade(targetState = images) {
        when (it) {
            null -> CenteredProgress()
            else -> {
                ImagesPager(images = it, selectedPosition = selectedPosition) { marsImage, _ ->
                    viewModel.updateFavorite(marsImage)
                }
            }
        }
    }

//    Column {
//        TopAppBar(
//            title = {
//                Text(text = "Mars rover photos")
//            },
//            actions = {
//                IconButton(onClick = { /*TODO*/ }) {
////                    Icon(painter = , contentDescription = )
//                }
//            })
//        ImagesPager(viewModel = viewModel)
//    }

}

@ExperimentalPagerApi
@Composable
fun ImagesPager(
    images: List<MarsImage>,
    selectedPosition: Int,
    favoriteClick: (MarsImage, Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = images.size)

    if (selectedPosition > 0) {
        LaunchedEffect(key1 = selectedPosition) {
            pagerState.scrollToPage(selectedPosition)
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) { page ->
        // Our page content
        val marsImage = images[page]

        Box {
            val state = rememberSaveable(saver = MultitouchState.Saver) {
                MultitouchState(
                    maxZoom = 5f,
                    minZoom = 1f,
                    zoom = 1f,
                    enabled = !pagerState.isScrollInProgress
                )
            }

            MultitouchDetector(modifier = Modifier, state = state, pagerState = pagerState) {
                NetworkImage(
                    modifier = Modifier.fillMaxSize(),
                    imageUrl = marsImage.imageUrl
                )
            }

            MarsImageFavoriteToggle(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.BottomCenter),
                checked = marsImage.favorite,
                onCheckedChange = {
                    favoriteClick(marsImage, it)
                }
            )
        }
    }
}