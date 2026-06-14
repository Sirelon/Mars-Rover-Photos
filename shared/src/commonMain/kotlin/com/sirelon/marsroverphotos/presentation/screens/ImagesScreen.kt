package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.ExperimentalMaterial3Api
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.utils.nasaImageOrigUrl
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.LikeHeartOverlay
import com.sirelon.marsroverphotos.presentation.ui.MarsImageFavoriteToggle
import com.sirelon.marsroverphotos.presentation.ui.rememberLikeHeartState
import com.sirelon.marsroverphotos.presentation.ui.setStatusBarAppearance
import com.sirelon.marsroverphotos.presentation.ui.MarsSnackbar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.ui.NoScrollEffect
import net.engawapg.lib.zoomable.ScrollGesturePropagation
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import com.sirelon.marsroverphotos.presentation.viewmodels.ImageViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.UiEvent
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.images_empty_btn
import com.sirelon.marsroverphotos.shared.resources.images_empty_title
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.sirelon.marsroverphotos.presentation.navigation.LocalSharedTransitionScope
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Fullscreen image viewer — horizontal pager with pinch-to-zoom, save, share, info sheet.
 *
 * Content is delivered as paged [androidx.paging.PagingData]. For [AppDestination.ImagesSource.ROVER_FEED]
 * the pager shares the photos list's [com.sirelon.marsroverphotos.data.paging.RoverFeedPager] stream, so
 * swiping past the first/last loaded photo auto-loads the previous/next day (bidirectional, in sync with
 * the list). Other sources present a finite, single-page list.
 *
 * @param photoIds    Image IDs (used by [AppDestination.ImagesSource.DIRECT_IDS]; empty for ROVER_FEED).
 * @param selectedId  Which ID to land on initially (null → first page).
 * @param source      Where images are loaded from.
 * @param roverId     Rover id used to restore [AppDestination.ImagesSource.ROVER_FEED] after recreation.
 * @param camera      Legacy single-camera filter used to restore [AppDestination.ImagesSource.ROVER_FEED].
 * @param cameras     Active camera filters used to restore [AppDestination.ImagesSource.ROVER_FEED].
 * @param onBack      Navigate back.
 */
@Composable
fun ImagesScreen(
    photoIds: List<String>,
    selectedId: String?,
    source: AppDestination.ImagesSource,
    roverId: Long? = null,
    camera: String? = null,
    cameras: Set<String> = emptySet(),
    onBack: () -> Unit,
    viewModel: ImageViewModel = koinViewModel(),
) {
    // Guard: nothing to show for direct IDs → pop immediately instead of hanging on the loader.
    if (source == AppDestination.ImagesSource.DIRECT_IDS && photoIds.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    LaunchedEffect(photoIds, selectedId, source, roverId, camera, cameras) {
        viewModel.setImageSource(
            source = source,
            ids = photoIds,
            selectedId = selectedId,
            roverId = roverId,
            camera = camera,
            cameras = cameras,
        )
    }

    // Force light (white) status-bar icons while the fullscreen black pager is visible,
    // and restore the default appearance when this screen leaves composition.
    DisposableEffect(Unit) {
        setStatusBarAppearance(lightIcons = true)
        onDispose { setStatusBarAppearance(lightIcons = false) }
    }

    val pagingItems = viewModel.pagedImages.collectAsLazyPagingItems()
    val hideUi by viewModel.hideUi.collectAsStateWithLifecycle()
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(initialValue = null)

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pagingItems.itemCount },
    )

    // Land on the selected photo once it is present in the loaded snapshot.
    var didInitialScroll by remember(source, selectedId) { mutableStateOf(false) }
    LaunchedEffect(pagingItems.itemCount, selectedId) {
        if (didInitialScroll) return@LaunchedEffect
        if (selectedId == null) {
            didInitialScroll = true
            return@LaunchedEffect
        }
        if (pagingItems.itemCount == 0) return@LaunchedEffect
        val index = pagingItems.itemSnapshotList.indexOfFirst { it?.id == selectedId }
        if (index >= 0) {
            if (pagerState.currentPage != index) pagerState.scrollToPage(index)
            didInitialScroll = true
        }
    }

    // The photo that carries the shared element. Starts as the tapped photo and follows the
    // settled page only after the initial scroll, so the async jump from page 0 to the selected
    // photo (and PREPEND index shifts) never re-keys the element mid-flight — re-keying restarts
    // the open animation, which reads as the transition playing twice.
    var sharedPhotoId by remember(source, selectedId) { mutableStateOf(selectedId) }
    LaunchedEffect(pagerState, didInitialScroll) {
        if (!didInitialScroll) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }.collect { page ->
            pagingItems.peek(page)?.id?.let { sharedPhotoId = it }
        }
    }

    val refresh = pagingItems.loadState.refresh
    Crossfade(targetState = pagingItems.itemCount == 0, label = "Images") { isEmpty ->
        if (isEmpty) {
            // Only treat the feed as genuinely empty once a load has settled: a
            // refresh that finished with no results (endOfPaginationReached) or one
            // that errored. While the first refresh is still pending or in-flight,
            // show progress instead of flashing the back-prompting empty state.
            val settledEmpty = (refresh is LoadState.NotLoading && refresh.endOfPaginationReached) ||
                refresh is LoadState.Error
            if (settledEmpty) {
                ImagesEmptyState(onBack = onBack)
            } else {
                CenteredProgress()
            }
        } else {
            ImagesPagerContent(
                uiEvent = uiEvent,
                pagingItems = pagingItems,
                pagerState = pagerState,
                hideUi = hideUi,
                favoriteOverrides = viewModel.favoriteOverrides,
                onBack = onBack,
                onShown = viewModel::onShown,
                onTap = viewModel::onTap,
                onFavoriteClick = viewModel::setFavorite,
                onSaveClick = viewModel::saveImage,
                onShareClick = viewModel::shareMarsImage,
                sharedPhotoId = sharedPhotoId,
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
            Spacer(modifier = Modifier.height(AppSpacing.md))
            AppButton(onClick = onBack) {
                Text(text = stringResource(Res.string.images_empty_btn))
            }
        }
    }
}

@Preview
@Composable
private fun ImagesEmptyStatePreview() {
    MaterialTheme {
        ImagesEmptyState(onBack = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagesPagerContent(
    uiEvent: UiEvent?,
    pagingItems: LazyPagingItems<MarsImage>,
    pagerState: PagerState,
    hideUi: Boolean,
    favoriteOverrides: SnapshotStateMap<String, Boolean>,
    onBack: () -> Unit,
    onShown: (MarsImage, Int) -> Unit,
    onTap: () -> Unit,
    onFavoriteClick: (MarsImage, Boolean) -> Unit,
    onSaveClick: (MarsImage) -> Unit,
    onShareClick: (MarsImage) -> Unit,
    sharedPhotoId: String? = null,
) {
    var titleState by remember { mutableStateOf("Mars Rover Photos") }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Per-page zoom scale — used to gate the dismiss gesture when an image is zoomed in
    val pageScales = remember { mutableStateMapOf<Int, Float>() }
    val isCurrentPageZoomed = (pageScales[pagerState.currentPage] ?: 1f) > 1f

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val marsPhoto = pagingItems.peek(page) ?: return@collect
            onShown(marsPhoto, page)
            titleState = if (marsPhoto.sol == 0L && marsPhoto.camera == null) {
                marsPhoto.name.orEmpty().ifBlank { "Mars Rover Photos" }
            } else {
                buildString {
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

    val currentImage = pagingItems.peek(pagerState.currentPage)

    // Drag-to-dismiss drives NavDisplay's predictive-back machinery through a synthetic
    // input, so the previous screen is composed and revealed beneath the gesture — the
    // same behavior as the system back swipe.
    val navEventDispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val backInput = remember { DirectNavigationEventInput() }
    DisposableEffect(navEventDispatcher) {
        navEventDispatcher?.addInput(backInput)
        onDispose { navEventDispatcher?.removeInput(backInput) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isCurrentPageZoomed) {
                if (isCurrentPageZoomed) return@pointerInput
                // The gesture only scrubs the predictive pop transition — no content
                // translation, so the shared element flies from its true bounds on release
                // (translating the content made it jump at the start of the fly-out).
                // Deliberate thresholds keep a quick flick from slamming the transition.
                val dismissThresholdPx = size.height * 0.18f
                val fullProgressPx = size.height * 0.4f
                var dragOffset = 0f
                var backDispatched = false
                detectVerticalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onDragEnd = {
                        if (dragOffset > dismissThresholdPx) {
                            if (backDispatched) backInput.backCompleted() else onBack()
                        } else if (backDispatched) {
                            backInput.backCancelled()
                        }
                        backDispatched = false
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        if (backDispatched) backInput.backCancelled()
                        backDispatched = false
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // Only respond to downward drags (or releasing back up once started)
                        if (dragAmount > 0f || dragOffset > 0f) {
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                            if (navEventDispatcher != null) {
                                if (!backDispatched && dragOffset > 0f) {
                                    backDispatched = true
                                    backInput.backStarted(NavigationEvent())
                                }
                                if (backDispatched) {
                                    backInput.backProgressed(
                                        NavigationEvent(
                                            progress = (dragOffset / fullProgressPx)
                                                .coerceIn(0f, 1f),
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
    ) {
        ImagesPager(
            pagerState = pagerState,
            pagingItems = pagingItems,
            hideUi = hideUi,
            favoriteOverrides = favoriteOverrides,
            onTap = onTap,
            onFavoriteClick = onFavoriteClick,
            onScaleChange = { page, scale -> pageScales[page] = scale },
            sharedPhotoId = sharedPhotoId,
        )

        OnEvent(uiEvent = uiEvent)

        // Translucent TopAppBar overlay + action row.
        AnimatedVisibility(
            visible = !hideUi,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
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
                    onBack  = onBack,
                    actions = {
                        if (currentImage != null) {
                            SaveIcon(onClick = { onSaveClick(currentImage) })
                            ShareIcon(onClick = { onShareClick(currentImage) })
                        }
                    },
                )
            }
        }

        // Bottom-left info icon overlay.
        if (currentImage != null) {
            AnimatedVisibility(
                visible = !hideUi,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding(),
            ) {
                InfoIcon(onClick = { showInfoSheet = true })
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
    var openImagePath by remember { mutableStateOf<String?>(null) }

    when (uiEvent) {
        is UiEvent.PhotoSaved -> {
            val imagePath = uiEvent.imagePath
            LaunchedEffect(uiEvent) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showCheckmark = true
                openImagePath = imagePath
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

    SaveSuccessOverlay(visible = showCheckmark)

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
            modifier = Modifier.padding(AppSpacing.lg),
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImagesPager(
    pagerState: PagerState,
    pagingItems: LazyPagingItems<MarsImage>,
    hideUi: Boolean,
    favoriteOverrides: SnapshotStateMap<String, Boolean>,
    onTap: () -> Unit,
    onFavoriteClick: (MarsImage, Boolean) -> Unit,
    onScaleChange: (page: Int, scale: Float) -> Unit,
    sharedPhotoId: String? = null,
) {

    NoScrollEffect {
        HorizontalPager(
            state = pagerState,
            // Stable keys keep the centered photo in place when a PREPEND inserts earlier days.
            key = { page -> pagingItems.peek(page)?.id ?: page },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) { page ->
            val marsImage = pagingItems[page] ?: return@HorizontalPager
            val zoomState = rememberZoomState()

            // Reset zoom when the user swipes to another page
            LaunchedEffect(pagerState.settledPage) {
                if (pagerState.settledPage != page) {
                    zoomState.reset()
                }
            }

            // Propagate scale changes so the parent can gate the dismiss gesture
            LaunchedEffect(zoomState) {
                snapshotFlow { zoomState.scale }
                    .collect { scale -> onScaleChange(page, scale) }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val sharedScope = LocalSharedTransitionScope.current
                // Only the page holding sharedPhotoId carries the shared elements: id-based
                // gating follows the user's swipes (back animates to the current photo) without
                // re-keying during the initial scroll-to-selected jump or PREPEND index shifts.
                val isSharedPage = marsImage.id == sharedPhotoId
                val sharedModifier = if (sharedScope != null && isSharedPage) {
                    val animScope = LocalNavAnimatedContentScope.current
                    with(sharedScope) {
                        // sharedBounds matches PhotoCard's end of this key: the grid renders a
                        // cropped square while this end is letterboxed, so the contents must
                        // crossfade rather than scale into each other.
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "photo_${marsImage.id}"),
                            animatedVisibilityScope = animScope,
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                contentScale = ContentScale.Fit,
                            ),
                        )
                    }
                } else Modifier
                val sharedFavModifier = if (sharedScope != null && isSharedPage) {
                    val animScope = LocalNavAnimatedContentScope.current
                    with(sharedScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "photo_favorite_${marsImage.id}"),
                            animatedVisibilityScope = animScope,
                        )
                    }
                } else Modifier

                val heartState = rememberLikeHeartState()

                NetworkImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(sharedModifier)
                        .zoomable(
                            zoomState = zoomState,
                            scrollGesturePropagation = ScrollGesturePropagation.NotZoomed,
                            enableOneFingerZoom = false,
                            onTap = { onTap() },
                            onDoubleTap = { _ ->
                                val currentFavorite = favoriteOverrides[marsImage.id] ?: marsImage.favorite
                                if (!currentFavorite) {
                                    favoriteOverrides[marsImage.id] = true
                                    onFavoriteClick(marsImage, true)
                                    heartState.trigger()
                                }
                            },
                        ),
                    contentScale = ContentScale.Fit,
                    imageUrl = nasaImageOrigUrl(marsImage.imageUrl),
                    showPlaceholder = false,
                    placeholderMemoryCacheKey = marsImage.imageUrl,
                )

                LikeHeartOverlay(
                    visible = heartState.visible,
                    modifier = Modifier.align(Alignment.Center),
                )

                val checked = favoriteOverrides[marsImage.id] ?: marsImage.favorite
                MarsImageFavoriteToggle(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .then(sharedFavModifier)
                        .navigationBarsPadding()
                        .padding(bottom = AppSpacing.lg, end = AppSpacing.lg)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape),
                    checked = checked,
                    onCheckedChange = {
                        val desired = !checked
                        if (desired) heartState.trigger()
                        favoriteOverrides[marsImage.id] = desired
                        onFavoriteClick(marsImage, desired)
                    },
                )
            }
        }
    }
}
