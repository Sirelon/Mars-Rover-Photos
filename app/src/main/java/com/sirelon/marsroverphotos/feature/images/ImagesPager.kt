package com.sirelon.marsroverphotos.feature.images

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.coil.CoilImage
import com.google.accompanist.imageloading.ImageLoadState
import com.google.accompanist.imageloading.MaterialLoadingImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.sirelon.marsroverphotos.feature.MultitouchDetector
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.utils.MultitouchLockGestureDetector

/**
 * Created on 13.04.2021 22:52 for Mars-Rover-Photos.
 */

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImagesPager(viewModel: ImageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

//    if (true) {
//        MultitouchGestureDetector()
////        MultitouchLockGestureDetector()
//        return
//    }


    viewModel.setIdsToShow(emptyList())

    val imagesA by viewModel.imagesLiveData.observeAsState()
    val images = imagesA
    if (images == null) {
        Text(text = "Empty")
        return
    }

    val pagerState = rememberPagerState(pageCount = images.size)
    var scale by remember { mutableStateOf(1f) }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
//            .clip(RectangleShape)
            .fillMaxSize()
            .background(Color.Gray)
//            .pointerInput(Unit) {
//                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, rotation ->
//                    scale *= zoom
//                }
//            },
    ) { page ->

        Log.d("Sirelon", "page $page")
        if (false) {
            MultitouchLockGestureDetector()
        } else {

            // Our page content
            val marsImage = images[page]

            MultitouchDetector {
                FullScreenImage(marsImage)
            }
        }
    }
}

@Composable
private fun FullScreenImage(marsImage: MarsImage) {
    CoilImage(
        modifier = Modifier.Companion.align(Alignment.Center),
        data = marsImage.imageUrl
    ) { imageState ->
        when (imageState) {
            is ImageLoadState.Success -> {
                MaterialLoadingImage(
                    result = imageState,
                    contentDescription = "My content description",
                    fadeInEnabled = true,
                    fadeInDurationMs = 600,

                    )
            }
//                is ImageLoadState.Error -> /* TODO */
//                    ImageLoadState.Loading
//                -> /* TODO */
//                    ImageLoadState.Empty
//                -> /* TODO */
        }
    }
}