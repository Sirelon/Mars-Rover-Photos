@file:OptIn(ExperimentalFoundationApi::class)

package com.sirelon.marsroverphotos.feature.images

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Button
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirelon.marsroverphotos.BuildConfig
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageScreen(
    viewModel: ImageViewModel = viewModel(),
    trackingEnabled: Boolean,
    photoIds: List<String>?,
    selectedId: String?,
    onHideUi: (Boolean) -> Unit,
) {
    val ids = photoIds ?: emptyList()
    LaunchedEffect(key1 = photoIds, block = {
        viewModel.setIdsToShow(ids)
    })

    LaunchedEffect(key1 = trackingEnabled) {
        viewModel.shouldTrack = trackingEnabled
    }

    DisposableEffect(key1 = ids) {
        onDispose {
            onHideUi(false)
        }
    }

    val selectedPosition = remember(key1 = photoIds, key2 = selectedId) {
        ids.indexOf(selectedId)
    }

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    SideEffect {
        onHideUi(screenState.hideUi)
    }

    val images = screenState.images

    val pagerState = rememberPagerState(selectedPosition)
    Crossfade(targetState = images.isEmpty(), label = "Images") {
        if (it) {
            CenteredProgress()
        } else {
            ImagesPagerContent(
                viewModel = viewModel,
                list = images,
                pagerState = pagerState,
                hideUi = screenState.hideUi,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagesPagerContent(
    viewModel: ImageViewModel,
    list: List<MarsImage>,
    hideUi: Boolean,
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

    val currentImage = remember(pagerState.currentPage) {
        list[pagerState.currentPage]
    }

    Column {
        AnimatedVisibility(visible = !hideUi) {
            TopAppBar(
                title = { Text(text = titleState) },
                actions = {
                    SaveIcon(onClick = {
                        viewModel.trackSaveClick()
                        viewModel.saveImage(currentImage)
                    })
                    ShareIcon(onClick = {
                        viewModel.shareMarsImage(currentImage)
                    })
                },
            )
            Spacer(modifier = Modifier.height(30.dp))
        }

        if (BuildConfig.DEBUG) {
            AnimatedVisibility(visible = !hideUi) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = { viewModel.makePopular(currentImage) }) {
                        Text(text = "Make popular")
                    }

                    Button(onClick = { viewModel.removePopular(currentImage) }) {
                        Text(text = "Remove popular")
                    }
                }
            }
        }

        Box {
            ImagesPager(
                pagerState = pagerState,
                images = list,
                callback = viewModel,
            ) { marsImage, _ ->
                viewModel.updateFavorite(marsImage)
            }

            val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(initialValue = null)

            OnEvent(uiEvent = uiEvent)
        }
    }
}

@Composable
private fun BoxScope.OnEvent(uiEvent: UiEvent?) {
    val snackbarHostState = remember { SnackbarHostState() }

    when (uiEvent) {
        is UiEvent.PhotoSaved -> {
            val context = LocalContext.current
            val imagePath = uiEvent.imagePath
            MarsSnackbar(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                actionClick = {
                    val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imagePath))
                    openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(openIntent)
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
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = rememberVectorPainter(image = Icons.Filled.Save),
            contentDescription = "Save"
        )
    }
}

@Composable
private fun ShareIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = rememberVectorPainter(image = Icons.Filled.Share),
            contentDescription = "Share"
        )
    }
}

@Composable
private fun ImagesPager(
    pagerState: PagerState,
    images: List<MarsImage>,
    callback: MultitouchDetectorCallback,
    favoriteClick: (MarsImage, Boolean) -> Unit
) {

    NoScrollEffect {
        HorizontalPager(
            pageCount = images.size,
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
                        imageUrl = marsImage.imageUrl,
                        placeholderRes = null,
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