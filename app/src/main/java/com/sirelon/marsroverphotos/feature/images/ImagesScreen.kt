package com.sirelon.marsroverphotos.feature.images

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.feature.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.feature.MultitouchDetectorCallback
import com.sirelon.marsroverphotos.feature.NetworkImage
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import com.sirelon.marsroverphotos.ui.MarsSnackbar
import com.sirelon.marsroverphotos.ui.MaterialSymbol
import com.sirelon.marsroverphotos.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.ui.NoScrollEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.androidx.compose.koinViewModel

/**
 * Created on 13.04.2021 22:52 for Mars-Rover-Photos.
 */

@Composable
fun ImageScreen(
    viewModel: ImageViewModel = koinViewModel(),
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

    val pagerState = rememberPagerState(
        initialPage = selectedPosition,
        pageCount = { images.size },
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagesPagerContent(
    viewModel: ImageViewModel,
    list: List<MarsImage>,
    hideUi: Boolean,
    pagerState: PagerState
) {
    var titleState by remember { mutableStateOf("Mars rover photos") }
    var showInfoSheet by remember { mutableStateOf(false) }

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
                windowInsets = WindowInsets(0,0,0,0),
                title = { Text(text = titleState) },
                actions = {
                    SaveIcon(onClick = {
                        viewModel.trackSaveClick()
                        viewModel.saveImage(currentImage)
                    })
                    ShareIcon(onClick = {
                        viewModel.shareMarsImage(currentImage)
                    })
                    InfoIcon(onClick = {
                        showInfoSheet = true
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

    // Photo info bottom sheet
    if (showInfoSheet) {
        PhotoInfoBottomSheet(
            image = currentImage,
            onDismiss = { showInfoSheet = false }
        )
    }
}

@Composable
private fun BoxScope.OnEvent(uiEvent: UiEvent?) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var showCheckmark by remember { mutableStateOf(false) }

    when (uiEvent) {
        is UiEvent.PhotoSaved -> {
            val context = LocalContext.current
            val imagePath = uiEvent.imagePath

            LaunchedEffect(uiEvent) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showCheckmark = true
                delay(1500)
                showCheckmark = false
            }

            SaveSuccessOverlay(visible = showCheckmark)

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
                    message = "Image saved successfully",
                    actionLabel = "Open"
                )
            })
        }

        is UiEvent.PhotoSaveError -> {
            LaunchedEffect(uiEvent) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            MarsSnackbar(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                actionClick = null
            )
            LaunchedEffect(key1 = uiEvent) {
                snackbarHostState.showSnackbar(
                    message = "Failed to save image: ${uiEvent.errorMessage}"
                )
            }
        }

        null -> {

        }
    }
}

@Composable
private fun BoxScope.SaveSuccessOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        modifier = Modifier.align(Alignment.TopEnd)
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.CheckCircle,
            contentDescription = "Saved",
            modifier = Modifier.padding(16.dp),
            tint = MaterialTheme.colorScheme.primary,
            size = 48.dp,
            filled = true
        )
    }
}

@Composable
private fun SaveIcon(
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Save,
            contentDescription = "Save"
        )
    }
}

@Composable
private fun ShareIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Share,
            contentDescription = "Share"
        )
    }
}

@Composable
private fun InfoIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Info,
            contentDescription = "Photo information"
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

    val scope = rememberCoroutineScope()
    NoScrollEffect {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            ) { page ->
            // Our page content
            val marsImage = images[page]

            LaunchedEffect(key1 = marsImage) {
                callback.currentImage = marsImage
            }

            BoxWithConstraints {
                val zoomState = rememberZoomState(
                    contentSize = Size(
                        width = this.minWidth.value,
                        height = this.minHeight.value
                    )
                )

                NetworkImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(zoomState, enableOneFingerZoom = false)
                        .pointerInput(key1 = null) {
                            detectTapGestures(
                                onTap = {
                                    callback.onTap()
                                },
                                onDoubleTap = {
                                    if (zoomState.scale > 1f) {
                                        scope.launch {
                                            zoomState.reset()
                                        }
                                    }

                                    callback.onDoubleTap(zoomState.scale)
                                },
                            )
                        },
                    contentScale = ContentScale.FillWidth,
                    imageUrl = marsImage.imageUrl,
                    placeholderRes = null,
                )

                MarsImageFavoriteToggle(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.BottomCenter),
                    checked = marsImage.favorite,
                    onCheckedChange = {
                        favoriteClick(marsImage, it)
                    }
                )

                // Reset zoom state when the page is moved out of the window.
                val isVisible = page == pagerState.settledPage
                LaunchedEffect(isVisible) {
                    if (!isVisible) {
                        zoomState.reset()
                    }
                }
            }
        }
    }
}
