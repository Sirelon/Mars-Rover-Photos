package com.sirelon.marsroverphotos.feature.images

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.google.accompanist.coil.CoilImage
import com.google.accompanist.imageloading.ImageLoadState
import com.google.accompanist.imageloading.MaterialLoadingImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.sirelon.marsroverphotos.feature.MultitouchDetector
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlin.math.roundToInt

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

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
    ) { page ->

        Log.d("Sirelon", "page $page")

        // Our page content
        val marsImage = images[page]

        Box {
            var zoom by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            if (pagerState.isScrollInProgress) {
                zoom = 1f
                offsetX = 0f
                offsetY = 0f
            }

            FullScreenImage(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                    }
                    .graphicsLayer(scaleX = zoom, scaleY = zoom)
                    .fillMaxSize(),
                marsImage = marsImage)

            Column {
                Spacer(modifier = Modifier.weight(1f))
                MultitouchDetector(
                    modifier = Modifier
                        .weight(2f)
//                            .background(Color.Red)
                ) { z, x, y ->
                    zoom = z
                    offsetX = x
                    offsetY = y
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FullScreenImage(modifier: Modifier, marsImage: MarsImage) {
    CoilImage(
        modifier = modifier,
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