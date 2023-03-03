package com.sirelon.marsroverphotos.feature.images

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.sirelon.marsroverphotos.feature.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.feature.MultitouchDetector
import com.sirelon.marsroverphotos.feature.MultitouchDetectorCallback
import com.sirelon.marsroverphotos.feature.MultitouchState
import com.sirelon.marsroverphotos.feature.NetworkImage
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import com.sirelon.marsroverphotos.ui.MarsSnackbar
import com.sirelon.marsroverphotos.ui.NoScrollEffect

/**
 * Created on 13.04.2021 22:52 for Mars-Rover-Photos.
 */
@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun ImageScreen(
    activity: FragmentActivity,
    viewModel: ImageViewModel = viewModel(),
    photoIds: List<String>?,
    selectedId: String?
) {
    val ids = photoIds ?: emptyList()
    LaunchedEffect(key1 = photoIds, block = {
        viewModel.setIdsToShow(ids)
    })

    val selectedPosition = remember(key1 = photoIds, key2 = selectedId) {
        ids.indexOf(selectedId)
    }

    val images by viewModel.imagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val pagerState = rememberPagerState(selectedPosition)

    Crossfade(targetState = images, label = "Images") {
        when {
            it.isEmpty() -> CenteredProgress()
            else -> {
                ImagesPagerContent(activity, viewModel, it, pagerState)
            }
        }
    }
}

@ExperimentalPagerApi
@Composable
private fun ImagesPagerContent(
    activity: FragmentActivity,
    viewModel: ImageViewModel,
    list: List<MarsImage>,
    pagerState: PagerState
) {
    var titleState by remember { mutableStateOf("Mars rover photos") }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val marsPhoto = list[page]

            viewModel.onShown(marsPhoto, page)
            titleState = "Mars image id: ${marsPhoto.id}"
        }
    }

    Column {
        TopAppBar(
            title = { Text(text = titleState) },
            actions = {
                SaveIcon(
                    activity,
                    viewModel,
                    image = { list[pagerState.currentPage] },
                )
                ShareIcon(activity, viewModel, image = { list[pagerState.currentPage] })
            },
        )
        Spacer(modifier = Modifier.height(30.dp))
        Box {
            ImagesPager(
                pagerState = pagerState,
                images = list,
                callback = viewModel,
            ) { marsImage, _ ->
                viewModel.updateFavorite(marsImage)
            }

            val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(initialValue = null)

            OnEvent(uiEvent = uiEvent, activity = activity)
        }
    }
}

@Composable
private fun BoxScope.OnEvent(uiEvent: UiEvent?, activity: FragmentActivity) {
    if (uiEvent?.handled == true) return

    SideEffect {
        uiEvent?.handled = true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    when (uiEvent) {
        is UiEvent.PhotoSaved -> {
            val imagePath = uiEvent.imagePath
            MarsSnackbar(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                actionClick = {
                    val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imagePath))
                    activity.startActivity(openIntent)
                }
            )
            LaunchedEffect(key1 = uiEvent, block = {
                snackbarHostState.showSnackbar(
                    message = "File was saved on path $imagePath",
                    actionLabel = "View"
                )
            })
        }

        null -> {

        }
    }
}

@Composable
private fun SaveIcon(
    activity: FragmentActivity,
    viewModel: ImageViewModel,
    image: () -> MarsImage
) {
    IconButton(onClick = {
        viewModel.trackSaveClick()
        viewModel.saveImage(activity, image())
    }) {
        Icon(
            painter = rememberVectorPainter(image = Icons.Filled.Save),
            contentDescription = "Save"
        )
    }
}

@Composable
fun ShareIcon(activity: FragmentActivity, viewModel: ImageViewModel, image: () -> MarsImage) {
    IconButton(onClick = {
        viewModel.shareMarsImage(activity = activity, marsImage = image())
    }) {
        Icon(
            painter = rememberVectorPainter(image = Icons.Filled.Share),
            contentDescription = "Share"
        )
    }
}

@ExperimentalPagerApi
@Composable
fun ImagesPager(
    pagerState: PagerState,
    images: List<MarsImage>,
    callback: MultitouchDetectorCallback,
    favoriteClick: (MarsImage, Boolean) -> Unit
) {

    NoScrollEffect {

        // TODO: can be native https://android-review.googlesource.com/#/q/I01120224eaccd9ee255890eb409e87a7ef7ffd5f
        HorizontalPager(
            count = images.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) { page ->
            // Our page content
            val marsImage = images[page]

            LaunchedEffect(key1 = marsImage) {
                callback.currentImage = marsImage
            }

            Box {
                val state = rememberSaveable(saver = MultitouchState.Saver) {
                    MultitouchState(
                        maxZoom = 5f,
                        minZoom = 1f,
                        zoom = 1f,
                        enabled = !pagerState.isScrollInProgress
                    )
                }

                MultitouchDetector(
                    modifier = Modifier,
                    state = state,
                    callback = callback
                ) {
                    NetworkImage(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillWidth,
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
}