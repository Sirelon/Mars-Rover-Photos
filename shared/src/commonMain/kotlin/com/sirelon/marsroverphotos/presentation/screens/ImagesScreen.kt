package com.sirelon.marsroverphotos.presentation.screens

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.presentation.ui.MarsSnackbar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.ui.NoScrollEffect
import com.sirelon.marsroverphotos.presentation.ui.rememberZoomableState
import com.sirelon.marsroverphotos.presentation.ui.zoomable
import com.sirelon.marsroverphotos.presentation.viewmodels.ImageViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Fullscreen image viewer — horizontal pager with pinch-to-zoom, save, share, info sheet.
 *
 * Ported from `app/.../feature/images/ImagesScreen.kt` to Compose Multiplatform.
 *
 * @param photoIds    Ordered list of image IDs to show in the pager.
 * @param selectedId  Which ID to land on initially (null → first page).
 * @param onBack      Navigate back (e.g. pop to Photos screen).
 */
@Composable
fun ImagesScreen(
    photoIds: List<String>,
    selectedId: String?,
    onBack: () -> Unit,
    viewModel: ImageViewModel = koinViewModel(),
) {
    // Guard: nothing to show → pop immediately instead of hanging on the loader.
    if (photoIds.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    LaunchedEffect(photoIds) {
        viewModel.setIdsToShow(photoIds)
    }

    DisposableEffect(photoIds) {
        onDispose { /* nothing to clean up */ }
    }

    val initialPage = remember(photoIds, selectedId) {
        selectedId?.let { photoIds.indexOf(it).takeIf { i -> i >= 0 } } ?: 0
    }

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val images = screenState.images

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { images.size },
    )

    Crossfade(targetState = images.isEmpty(), label = "Images") { isEmpty ->
        if (isEmpty) {
            CenteredProgress()
        } else {
            ImagesPagerContent(
                viewModel = viewModel,
                list = images,
                pagerState = pagerState,
                hideUi = screenState.hideUi,
                onBack = onBack,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagesPagerContent(
    viewModel: ImageViewModel,
    list: List<MarsImage>,
    pagerState: PagerState,
    hideUi: Boolean,
    onBack: () -> Unit,
) {
    var titleState by remember { mutableStateOf("Mars Rover Photos") }
    var showInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (list.isNotEmpty() && page < list.size) {
                val marsPhoto = list[page]
                viewModel.onShown(marsPhoto, page)
                titleState = "Mars image ID: ${marsPhoto.id}"
            }
        }
    }

    val currentImage = remember(pagerState.currentPage, list) {
        list.getOrNull(pagerState.currentPage)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !hideUi) {
            TopAppBar(
                title = { Text(text = titleState) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (currentImage != null) {
                        SaveIcon(onClick = {
                            viewModel.saveImage(currentImage)
                        })
                        ShareIcon(onClick = {
                            viewModel.shareMarsImage(currentImage)
                        })
                        InfoIcon(onClick = {
                            showInfoSheet = true
                        })
                    }
                },
            )
        }

        // Debug buttons — shown only on debug builds
        if (BuildInfo.isDebug) {
            AnimatedVisibility(visible = !hideUi) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = { /* makePopular — requires repo extension; noop in shared */ }) {
                        Text(text = "Mark as popular")
                    }
                    Button(onClick = { /* removePopular — noop in shared */ }) {
                        Text(text = "Remove from popular")
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            ImagesPager(
                pagerState = pagerState,
                images = list,
                onTap = { viewModel.onTap() },
                onFavoriteClick = { marsImage -> viewModel.updateFavorite(marsImage) },
            )

            val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(initialValue = null)
            OnEvent(uiEvent = uiEvent)
        }
    }

    // Photo info bottom sheet
    if (showInfoSheet && currentImage != null) {
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
    val uriHandler = LocalUriHandler.current
    var showCheckmark by remember { mutableStateOf(false) }

    when (uiEvent) {
        is UiEvent.PhotoSaved -> {
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
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                actionClick = {
                    if (!imagePath.isNullOrBlank()) {
                        try {
                            uriHandler.openUri(imagePath)
                        } catch (_: Exception) {
                            // URI may not be openable on all platforms — silently ignore
                        }
                    }
                }
            )
            LaunchedEffect(uiEvent) {
                snackbarHostState.showSnackbar(
                    message = "Image saved successfully",
                    actionLabel = if (imagePath != null) "Open" else null
                )
            }
        }

        is UiEvent.PhotoSaveError -> {
            LaunchedEffect(uiEvent) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                snackbarHostState.showSnackbar(
                    message = "Failed to save image: ${uiEvent.errorMessage}"
                )
            }
            MarsSnackbar(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                actionClick = null
            )
        }

        is UiEvent.ShareError -> {
            LaunchedEffect(uiEvent) {
                snackbarHostState.showSnackbar(
                    message = "Failed to share image: ${uiEvent.errorMessage}"
                )
            }
            MarsSnackbar(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                actionClick = null
            )
        }

        null -> { /* idle */ }
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

// ─── Icon buttons ────────────────────────────────────────────────────────────

@Composable
private fun SaveIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        MaterialSymbolIcon(symbol = MaterialSymbol.Save, contentDescription = "Save")
    }
}

@Composable
private fun ShareIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        MaterialSymbolIcon(symbol = MaterialSymbol.Share, contentDescription = "Share")
    }
}

@Composable
private fun InfoIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        MaterialSymbolIcon(symbol = MaterialSymbol.Info, contentDescription = "Photo information")
    }
}

// ─── Pager ───────────────────────────────────────────────────────────────────

@Composable
private fun ImagesPager(
    pagerState: PagerState,
    images: List<MarsImage>,
    onTap: () -> Unit,
    onFavoriteClick: (MarsImage) -> Unit,
) {
    val scope = rememberCoroutineScope()

    NoScrollEffect {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) { page ->
            val marsImage = images[page]
            val zoomState = rememberZoomableState()

            Box(modifier = Modifier.fillMaxSize()) {
                NetworkImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(state = zoomState)
                        .pointerInput(zoomState) {
                            detectTapGestures(
                                onTap = { onTap() },
                                onDoubleTap = {
                                    if (zoomState.scale > 1f) {
                                        scope.launch { zoomState.reset() }
                                    }
                                },
                            )
                        },
                    contentScale = ContentScale.FillWidth,
                    imageUrl = marsImage.imageUrl,
                    showPlaceholder = false,
                )

                // Controls layer — image fills full screen; controls stay above nav bar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    MarsImageFavoriteToggle(
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.BottomCenter),
                        checked = marsImage.favorite,
                        onCheckedChange = { onFavoriteClick(marsImage) }
                    )
                }

                // Reset zoom when this page scrolls off-screen
                val isSettled = page == pagerState.settledPage
                LaunchedEffect(isSettled) {
                    if (!isSettled) zoomState.reset()
                }
            }
        }
    }
}
