package com.sirelon.marsroverphotos.feature.images

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.*
import com.sirelon.marsroverphotos.extensions.showAppSettings
import com.sirelon.marsroverphotos.feature.*
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import com.sirelon.marsroverphotos.ui.MarsSnackbar
import com.sirelon.marsroverphotos.ui.NoScrollEffect
import kotlinx.coroutines.flow.collect

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
    viewModel.setIdsToShow(ids)

    val selectedPosition = ids.indexOf(selectedId)

    val pagerState = rememberPagerState(selectedPosition)

    val imagesA by viewModel.imagesLiveData.observeAsState()
    val images = imagesA

    Crossfade(targetState = images) {
        when {
            it.isNullOrEmpty() -> CenteredProgress()
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

            val uiEvent = viewModel.uiEvent.observeAsState()
            val value = uiEvent.value

            onEvent(value, activity)
        }
    }
}

@Composable
private fun BoxScope.onEvent(uiEvent: UiEvent?, activity: FragmentActivity) {
    if (uiEvent?.handled == true) return

    uiEvent?.handled = true
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
        is UiEvent.CameraPermissionDenied -> {
            MarsSnackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbarHostState = snackbarHostState,
                actionClick = { activity.showAppSettings() },
            )
            LaunchedEffect(key1 = uiEvent, block = {
                snackbarHostState.showSnackbar(
                    message = "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings",
                    actionLabel = "Open setting"
                )
            })
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SaveIcon(
    activity: FragmentActivity,
    viewModel: ImageViewModel,
    image: () -> MarsImage
) {
    // permission state
    val cameraPermissionState =
        rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    IconButton(onClick = {
        viewModel.trackSaveClick()
        checkPermissionState(
            cameraPermissionState = cameraPermissionState,
            permissionGranted = { viewModel.saveImage(activity, image()) },
            permissionDenied = viewModel::onPermissionDenied,
        )

        cameraPermissionState.launchPermissionRequest()
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

@OptIn(ExperimentalPermissionsApi::class)
private fun checkPermissionState(
    cameraPermissionState: PermissionState,
    permissionGranted: () -> Unit,
    permissionDenied: (rationale: Boolean) -> Unit
) {
    val status = cameraPermissionState.status
    if (status is PermissionStatus.Denied) {
        permissionDenied(status.shouldShowRationale)
    } else if (status.isGranted) {
        permissionGranted()
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
                    pagerState = pagerState,
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