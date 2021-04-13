package com.sirelon.marsroverphotos.feature.images

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.google.accompanist.coil.CoilImage
import com.google.accompanist.imageloading.ImageLoadState
import com.google.accompanist.imageloading.MaterialLoadingImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

/**
 * Created on 13.04.2021 22:52 for Mars-Rover-Photos.
 */

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImagesPager(viewModel: ImageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

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
            .clip(RectangleShape)
            .fillMaxSize()
            .background(Color.Gray)
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, rotation ->
                    scale *= zoom
                }
            },
    ) { page ->
        // Our page content
        val marsImage = images[page]
//        ImageItem(marsImage = marsImage)

        if (pagerState.isScrollInProgress) {
            scale = 1f
        }

        var toScale = maxOf(1f, minOf(5f, scale))

        if (pagerState.currentPage != page) {
            toScale = 1f
        }

        CoilImage(
            modifier = Modifier
                .align(Alignment.Center) // keep the image centralized into the Box
                .graphicsLayer(
                    // adding some zoom limits (min 50%, max 200%)
                    scaleX = toScale,
                    scaleY = toScale
                )
            ,
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
}