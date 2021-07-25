package com.sirelon.marsroverphotos.feature.images

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.viewpager2.widget.ViewPager2
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
import com.sirelon.marsroverphotos.feature.*
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import com.sirelon.marsroverphotos.ui.MarsSnackbar
import kotlinx.coroutines.launch

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
    list: List<MarsImage>,
    pagerState: PagerState
) {
    var titleState by remember { mutableStateOf("Mars rover photos") }

    Column {
        TopAppBar(
            title = { Text(text = titleState) },
            actions = {
                saveIcon(
                    activity,
                    viewModel,
                    image = { list[pagerState.currentPage] },
                )
            },
        )
        Spacer(modifier = Modifier.height(30.dp))
        Box {
            ImagesPager(
                pagerState = pagerState,
                images = list,
                callback = viewModel,
                onShownPage = { marsPhoto, page ->
                    viewModel.onShown(marsPhoto, page)
                    titleState = "Mars image id: ${marsPhoto.id}"
                }
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
                launch {
                    snackbarHostState.showSnackbar(
                        message = "File was saved on path $imagePath",
                        actionLabel = "View"
                    )
                }
            })
        }
        is UiEvent.CameraPermissionDenied -> {
            MarsSnackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbarHostState = snackbarHostState,
                actionClick = { activity.showAppSettings() },
            )
            LaunchedEffect(key1 = uiEvent, block = {
                launch {
                    snackbarHostState.showSnackbar(
                        message = "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings",
                        actionLabel = "Open setting"
                    )
                }
            })
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

    IconButton(onClick = {
        viewModel.trackSaveClick()
        checkPermissionState(
            cameraPermissionState = cameraPermissionState,
            launchAgain = { cameraPermissionState.launchPermissionRequest() },
            permissionGranted = { viewModel.saveImage(activity, image()) },
            permissionDenied = viewModel::onPermissionDenied,
        )

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
    launchAgain: () -> Unit,
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
            launchAgain()
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
    callback: MultitouchDetectorCallback,
    onShownPage: (marsPhoto: MarsImage, page: Int) -> Unit,
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

        LaunchedEffect(key1 = page, block = {
            if (!pagerState.isScrollInProgress) {
                onShownPage(marsImage, page)
            }
        })

        callback.currentImage = marsImage

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