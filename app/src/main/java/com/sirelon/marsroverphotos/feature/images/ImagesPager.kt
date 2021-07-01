package com.sirelon.marsroverphotos.feature.images

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.extensions.showSnackBar
import com.sirelon.marsroverphotos.feature.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.feature.MultitouchDetector
import com.sirelon.marsroverphotos.feature.MultitouchState
import com.sirelon.marsroverphotos.feature.NetworkImage
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                Column {
                    TopAppBar(
                        title = {
                            Text(text = "Mars rover photos")
                        },
                        actions = {
                            saveIcon(activity) {
                                it.get(pagerState.currentPage)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    ImagesPager(pagerState = pagerState, images = it) { marsImage, _ ->
                        viewModel.updateFavorite(marsImage)
                    }
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun saveIcon(activity: FragmentActivity, image: () -> MarsImage) {
    // permission state
    val cameraPermissionState =
        rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    checkPermissionState(cameraPermissionState) {
        savePhotoToLocal(activity = activity, photo = image())
    }

    IconButton(onClick = {
        cameraPermissionState.launchPermissionRequest()
    }) {
        Icon(
            painter = painterResource(R.drawable.ic_save),
            contentDescription = null
        )
    }
}

private fun savePhotoToLocal(activity: FragmentActivity, photo: MarsImage) {
    val appUrl = "https://play.google.com/store/apps/details?id=${activity.packageName}"
    activity.lifecycleScope.launch(Dispatchers.IO) {
        kotlin.runCatching {
            val bitmap =
                Glide.with(activity).asBitmap().load(photo.imageUrl).submit().get()
            val localUrl = MediaStore.Images.Media.insertImage(
                activity.contentResolver, bitmap,
                "mars_photo_${photo.id}",
                "Photo saved from $appUrl"
            )
            activity.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse(localUrl)
                )
            )
            val dataManager = RoverApplication.APP.dataManger
            dataManager.updatePhotoSaveCounter(photo)
            // Update counter for save
            localUrl
        }.onSuccess {
            withContext(Dispatchers.Main) {
                showSnackBarOnSaved(activity, it)
            }
        }.onFailure {
            withContext(Dispatchers.Main) {
                it.printStackTrace()
                Toast.makeText(activity, "Error occured ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun showSnackBarOnSaved(activity: FragmentActivity, imagePath: String?) {
    val fullscreenImageRoot = activity.findViewById<View>(android.R.id.content)
    fullscreenImageRoot.showSnackBar(
        msg = "File was saved on path $imagePath",
        actionTxt = "View", actionCallback = View.OnClickListener {
            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imagePath))
            activity.startActivity(openIntent)
        }, duration = com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
    )
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun checkPermissionState(
    cameraPermissionState: PermissionState,
    permissionGranted: () -> Unit
) {

    val snackBarSettings = remember { mutableStateOf(false) }

    when {
        cameraPermissionState.hasPermission -> {
            permissionGranted()
        }
        cameraPermissionState.shouldShowRationale -> {
            Text("Feature not available")
        }
        !cameraPermissionState.permissionRequested -> {
//            launchPermissionRequest = true
        }
        else -> {
            snackBarSettings.value = true
        }
    }

    if (snackBarSettings.value) {
        com.sirelon.marsroverphotos.ui.Snackbar(
            text = "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings",
            "Open setting"
        ) {
            // TODO:
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