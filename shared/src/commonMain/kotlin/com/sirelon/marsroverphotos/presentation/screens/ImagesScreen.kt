package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.presentation.ui.setStatusBarAppearance
import com.sirelon.marsroverphotos.presentation.ui.MarsSnackbar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.ui.NoScrollEffect
import com.sirelon.marsroverphotos.presentation.ui.rememberZoomableState
import com.sirelon.marsroverphotos.presentation.ui.zoomable
import com.sirelon.marsroverphotos.presentation.viewmodels.ImageViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.UiEvent
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.images_empty_btn
import com.sirelon.marsroverphotos.shared.resources.images_empty_title
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Fullscreen image viewer — horizontal pager with pinch-to-zoom, save, share, info sheet.
 *
 * Ported from `app/.../feature/images/ImagesScreen.kt` to Compose Multiplatform.
 *
 * @param photoIds    Ordered list of image IDs to show in the pager.
 * @param selectedId  Which ID to land on initially (null → first page).
 * @param source      Defines where images should be loaded from.
 * @param onBack      Navigate back (e.g. pop to Photos screen).
 */
@Composable
fun ImagesScreen(
    photoIds: List<String>,
    selectedId: String?,
    source: AppDestination.ImagesSource,
    onBack: () -> Unit,
    viewModel: ImageViewModel = koinViewModel(),
) {
    // Guard: nothing to show → pop immediately instead of hanging on the loader.
    if (source == AppDestination.ImagesSource.DIRECT_IDS && photoIds.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    LaunchedEffect(photoIds, source) {
        viewModel.setImageSource(source = source, ids = photoIds)
    }

    // Force light (white) status-bar icons while the fullscreen black pager is visible,
    // and restore the default appearance when this screen leaves composition.
    DisposableEffect(Unit) {
        setStatusBarAppearance(lightIcons = true)
        onDispose {
            setStatusBarAppearance(lightIcons = false)
        }
    }

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val images = screenState.images

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { images.size },
    )
    var isInitialPositionApplied by remember(source, selectedId) { mutableStateOf(false) }
    var hasRenderedImages by remember(source) { mutableStateOf(false) }

    if (images.isNotEmpty() && !hasRenderedImages) {
        SideEffect {
            hasRenderedImages = true
        }
    }

    LaunchedEffect(images, selectedId, isInitialPositionApplied) {
        if (isInitialPositionApplied) return@LaunchedEffect

        if (selectedId == null) {
            isInitialPositionApplied = true
            return@LaunchedEffect
        }

        if (images.isEmpty()) {
            return@LaunchedEffect
        }

        val index = selectedId.let { targetId ->
            images.indexOfFirst { it.id == targetId }.takeIf { it >= 0 }
        }

        if (index != null && index < pagerState.pageCount) {
            if (pagerState.currentPage != index) {
                pagerState.scrollToPage(index)
            }
        }

        isInitialPositionApplied = true
    }

    Crossfade(targetState = images.isEmpty(), label = "Images") { isEmpty ->
        if (isEmpty) {
            if (source != AppDestination.ImagesSource.DIRECT_IDS && hasRenderedImages) {
                ImagesEmptyState(onBack = onBack)
            } else {
                CenteredProgress()
            }
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

@Composable
private fun ImagesEmptyState(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.images_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) {
                Text(text = stringResource(Res.string.images_empty_btn))
            }
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

    // Swipe-to-dismiss: tracks how far the user has dragged the screen downward.
    // Lives here so the graphicsLayer wrapper can cover ALL content (pager + overlays).
    val dismissOffsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 150.dp.toPx() }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (list.isNotEmpty() && page < list.size) {
                val marsPhoto = list[page]
                viewModel.onShown(marsPhoto, page)
                titleState = buildString {
                    append("Sol ")
                    append(marsPhoto.sol)
                    val cam = marsPhoto.camera?.fullName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && it != "Unknown Camera" }
                    if (cam != null) {
                        append(" · ")
                        append(cam)
                    }
                }
            }
        }
    }

    val currentImage = remember(pagerState.currentPage, list) {
        list.getOrNull(pagerState.currentPage)
    }

    // The graphicsLayer wrapper translates & fades the entire screen during swipe-to-dismiss,
    // including the TopAppBar overlay, so they all move together naturally.
    Box(modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            translationY = dismissOffsetY.value
            // Fade from fully opaque (offset=0) to transparent (offset=2×threshold)
            alpha = (1f - (dismissOffsetY.value / (dismissThresholdPx * 2f)).coerceIn(0f, 1f))
        }
    ) {
        ImagesPager(
            pagerState = pagerState,
            images = list,
            hideUi = hideUi,
            onTap = { viewModel.onTap() },
            onFavoriteClick = { marsImage -> viewModel.updateFavorite(marsImage) },
            onBack = onBack,
            dismissOffsetY = dismissOffsetY,
            dismissThresholdPx = dismissThresholdPx,
        )

        val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(initialValue = null)
        OnEvent(uiEvent = uiEvent)

        // Translucent TopAppBar overlay + (debug-only) action row.
        AnimatedVisibility(
            visible = !hideUi,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    ),
                    title = {
                        Text(
                            text = titleState,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
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
    // Captures the saved-image path so the "Open" action in the snackbar can open it.
    // Held outside the `when` branch so MarsSnackbar (always rendered) can reference it.
    var openImagePath by remember { mutableStateOf<String?>(null) }

    when (uiEvent) {
        is UiEvent.PhotoSaved -> {
            val imagePath = uiEvent.imagePath
            LaunchedEffect(uiEvent) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showCheckmark = true
                openImagePath = imagePath
                // Show checkmark for 1.5 s concurrently with the snackbar.
                launch {
                    delay(1500)
                    showCheckmark = false
                }
                snackbarHostState.showSnackbar(
                    message = "Image saved successfully",
                    actionLabel = if (!imagePath.isNullOrBlank()) "Open" else null
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
        }

        is UiEvent.ShareError -> {
            LaunchedEffect(uiEvent) {
                snackbarHostState.showSnackbar(
                    message = "Failed to share image: ${uiEvent.errorMessage}"
                )
            }
        }

        null -> { /* idle */ }
    }

    // SaveSuccessOverlay lives outside the when so AnimatedVisibility can play its exit
    // animation even after the event is consumed (uiEvent → null).
    SaveSuccessOverlay(visible = showCheckmark)

    // MarsSnackbar is always rendered so the SnackbarHost is never removed mid-display.
    MarsSnackbar(
        snackbarHostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        actionClick = {
            val path = openImagePath
            if (!path.isNullOrBlank()) {
                try {
                    uriHandler.openUri(path)
                } catch (_: Exception) {
                    // URI may not be openable on all platforms — silently ignore
                }
            }
        }
    )
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
    hideUi: Boolean,
    onTap: () -> Unit,
    onFavoriteClick: (MarsImage) -> Unit,
    onBack: () -> Unit,
    dismissOffsetY: Animatable<Float, AnimationVector1D>,
    dismissThresholdPx: Float,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    NoScrollEffect {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) { page ->
            val marsImage = images[page]
            val zoomState = rememberZoomableState()

            Box(modifier = Modifier
                .fillMaxSize()
                // PointerEventPass.Final: let HorizontalPager consume horizontal swipes first.
                // We observe the remaining gesture and handle downward-only drags for dismiss.
                .pointerInput(onBack, zoomState) {
                    awaitEachGesture {
                        val velocityTracker = VelocityTracker()
                        val down = awaitFirstDown(requireUnconsumed = false)
                        velocityTracker.addPointerInputChange(down)
                        val startY = down.position.y
                        val startX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            velocityTracker.addPointerInputChange(change)

                            if (!change.pressed) {
                                // Finger lifted — decide: dismiss or snap back
                                val velocity = velocityTracker.calculateVelocity()
                                val currentOffset = dismissOffsetY.value
                                // Dismiss if dragged past threshold OR if a quick flick downward
                                val shouldDismiss = currentOffset >= dismissThresholdPx ||
                                        (currentOffset > 0f && velocity.y > 800f)
                                if (shouldDismiss) {
                                    scope.launch {
                                        dismissOffsetY.animateTo(
                                            with(density) { 1000.dp.toPx() },
                                            animationSpec = tween(durationMillis = 220),
                                        )
                                        onBack()
                                    }
                                } else if (currentOffset > 0f) {
                                    scope.launch {
                                        dismissOffsetY.animateTo(
                                            0f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        )
                                    }
                                }
                                break
                            }

                            val dy = change.position.y - startY
                            val dx = kotlin.math.abs(change.position.x - startX)

                            when {
                                zoomState.scale > 1f -> {
                                    // Zoomed in — don't intercept, snap any offset back
                                    if (dismissOffsetY.value > 0f) {
                                        scope.launch { dismissOffsetY.animateTo(0f) }
                                    }
                                    break
                                }
                                dx > dy.coerceAtLeast(0f) * 1.5f -> {
                                    // Horizontal swipe dominant — let the pager handle it, snap back
                                    if (dismissOffsetY.value > 0f) {
                                        scope.launch {
                                            dismissOffsetY.animateTo(
                                                0f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                            )
                                        }
                                    }
                                    break
                                }
                                dy > 0f -> {
                                    // Downward vertical drag — update the dismiss offset live
                                    scope.launch { dismissOffsetY.snapTo(dy) }
                                }
                                dy < 0f && dismissOffsetY.value > 0f -> {
                                    // Upward drag while offset is non-zero — pull back toward 0
                                    scope.launch {
                                        dismissOffsetY.snapTo((dismissOffsetY.value + dy).coerceAtLeast(0f))
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
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
                    contentScale = ContentScale.Fit,
                    imageUrl = marsImage.imageUrl,
                    showPlaceholder = false,
                )

                // Controls layer — image fills full screen; controls stay above nav bar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    AnimatedVisibility(
                        visible = !hideUi,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        MarsImageFavoriteToggle(
                            modifier = Modifier.size(64.dp),
                            checked = marsImage.favorite,
                            onCheckedChange = { onFavoriteClick(marsImage) }
                        )
                    }
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
