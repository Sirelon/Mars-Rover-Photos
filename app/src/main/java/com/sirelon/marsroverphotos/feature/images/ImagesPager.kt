package com.sirelon.marsroverphotos.feature.images

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.defaultDecayAnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.coil.CoilImage
import com.google.accompanist.imageloading.ImageLoadState
import com.google.accompanist.imageloading.MaterialLoadingImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerDefaults
import com.google.accompanist.pager.rememberPagerState
import com.sirelon.marsroverphotos.feature.MultitouchDetector
import com.sirelon.marsroverphotos.feature.MultitouchState
import kotlin.math.abs

/**
 * Created on 13.04.2021 22:52 for Mars-Rover-Photos.
 */

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImagesPager(viewModel: ImageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
//    if (true) {
//        PointerTypeInput()
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

    val flingBehavior = PagerDefaults.defaultPagerFlingConfig(pagerState)
    val scr = rememberScrollState()

    HorizontalPager(
        flingBehavior = flingBehavior,
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
    ) { page ->
        // Our page content
        val marsImage = images[page]

        Box {
//            if (pagerState.isScrollInProgress) {
//                zoom = 1f
//                offsetX = 0f
//                offsetY = 0f
//            }

            val state = rememberSaveable(saver = MultitouchState.Saver) {
                MultitouchState(
                    maxZoom = 5f,
                    minZoom = 1f,
                    zoom = 1f,
                    enabled = !pagerState.isScrollInProgress
                )
            }

//            FullScreenImage(
//                modifier = Modifier.fillMaxSize(),
//                imageUrl = marsImage.imageUrl
//            )
            MultitouchDetector(modifier = Modifier, state = state, pagerState = pagerState) {
                FullScreenImage(
                    modifier = Modifier.fillMaxSize(),
                    imageUrl = marsImage.imageUrl
                )
            }
        }
    }
}

@Composable
fun flingBehavior(): FlingBehavior {
    val flingSpec = defaultDecayAnimationSpec()
    return remember(flingSpec) {
        DefaultFlingBehavior(flingSpec)
    }
}

private class DefaultFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return if (abs(initialVelocity) > 1f) {
            var velocityLeft = initialVelocity
            var lastValue = 0f
            AnimationState(
                initialValue = 0f,
                initialVelocity = initialVelocity,
            ).animateDecay(flingDecay) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
            velocityLeft
        } else {
            initialVelocity
        }
    }
}

@Composable
fun FullScreenImage(modifier: Modifier, imageUrl: String) {
    CoilImage(
        modifier = modifier,
        data = imageUrl
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