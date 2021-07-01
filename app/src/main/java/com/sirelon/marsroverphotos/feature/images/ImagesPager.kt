package com.sirelon.marsroverphotos.feature.images

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.extensions.showAppSettings
import com.sirelon.marsroverphotos.feature.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.feature.MultitouchDetector
import com.sirelon.marsroverphotos.feature.MultitouchState
import com.sirelon.marsroverphotos.feature.NetworkImage
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import com.sirelon.marsroverphotos.ui.MarsSnackbar

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
    Log.d("Sirelon", "ImageScreen() called with: photoIds = $photoIds, selectedId = $selectedId");
    viewModel.setIdsToShow(ids)

    val selectedPosition = ids.indexOf(selectedId)

    val pagerState = rememberPagerState(pageCount = ids.size)

    if (selectedPosition >= 0 && selectedPosition < ids.size) {
        LaunchedEffect(key1 = selectedPosition) {
            pagerState.scrollToPage(selectedPosition)
        }
    } else {
        recordException(IllegalArgumentException("Try to open $ids with $selectedPosition"))
    }

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
    it: List<MarsImage>,
    pagerState: PagerState
) {
    Column {
        TopAppBar(
            title = { Text(text = "Mars rover photos") },
            actions = {
                saveIcon(
                    activity,
                    viewModel,
                    image = { it[pagerState.currentPage] },
                )
            },
        )
        Spacer(modifier = Modifier.height(30.dp))
        Box {
            ImagesPager(pagerState = pagerState, images = it) { marsImage, _ ->
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
    Log.d("Sirelon", "onEvent() called with: uiEvent = $uiEvent, activity = $activity");

    when (uiEvent) {
        is UiEvent.PhotoSaved -> {
            val imagePath = uiEvent.imagePath
            MarsSnackbar(
                text = "File was saved on path $imagePath",
                modifier = Modifier.align(Alignment.BottomCenter),
                actionText = "View",
                actionClick = {
                    val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imagePath))
                    activity.startActivity(openIntent)
                }
            )
        }
        is UiEvent.CameraPermissionDenied -> {
            MarsSnackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings",
                actionText = "Open setting",
                actionClick = { activity.showAppSettings() },
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun saveIcon(
    activity: FragmentActivity,
    viewModel: ImageViewModel,
    image: () -> MarsImage
) {
    // permission state
    val cameraPermissionState =
        rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    checkPermissionState(
        cameraPermissionState = cameraPermissionState,
        permissionGranted = { viewModel.saveImage(activity, image()) },
        permissionDenied = viewModel::onPermissionDenied,
    )

    IconButton(onClick = {
        cameraPermissionState.launchPermissionRequest()
    }) {
        Icon(
            painter = painterResource(R.drawable.ic_save),
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun checkPermissionState(
    cameraPermissionState: PermissionState,
    permissionGranted: () -> Unit,
    permissionDenied: (rationale: Boolean) -> Unit
) {
    when {
        cameraPermissionState.hasPermission -> {
            permissionGranted()
        }
        cameraPermissionState.shouldShowRationale -> {
            permissionDenied(true)
        }
        !cameraPermissionState.permissionRequested -> {
//            launchPermissionRequest = true
        }
        else -> {
            permissionDenied(false)
        }
    }
}

@ExperimentalPagerApi
@Composable
fun ImagesPager(
    pagerState: PagerState,
    images: List<MarsImage>,
    favoriteClick: (MarsImage, Boolean) -> Unit
) {
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