package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MarsImageFavoriteToggle
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
 * @param camera      Camera filter used to restore [AppDestination.ImagesSource.ROVER_FEED] after recreation.
 * @param onBack      Navigate back.
 */
@Composable
fun ImagesScreen(
    photoIds: List<String>,
    selectedId: String?,
    source: AppDestination.ImagesSource,
    roverId: Long? = null,
    camera: String? = null,
    onBack: () -> Unit,
    viewModel: ImageViewModel = koinViewModel(),
) {
    // Guard: nothing to show for direct IDs → pop immediately instead of hanging on the loader.
    if (source == AppDestination.ImagesSource.DIRECT_IDS && photoIds.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    LaunchedEffect(photoIds, selectedId, source, roverId, camera) {
        viewModel.setImageSource(
            source = source,
            ids = photoIds,
            selectedId = selectedId,
            roverId = roverId,
            camera = camera,
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
                onBack = onBack,
                onShown = viewModel::onShown,
                onTap = viewModel::onTap,
                onFavoriteClick = viewModel::setFavorite,
                onSaveClick = viewModel::saveImage,
                onShareClick = viewModel::shareMarsImage,
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
    onBack: () -> Unit,
    onShown: (MarsImage, Int) -> Unit,
    onTap: () -> Unit,
    onFavoriteClick: (MarsImage, Boolean) -> Unit,
    onSaveClick: (MarsImage) -> Unit,
    onShareClick: (MarsImage) -> Unit,
) {
    var titleState by remember { mutableStateOf("Mars Rover Photos") }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Per-page zoom scale — used to gate the dismiss gesture when an image is zoomed in
    val pageScales = remember { mutableStateMapOf<Int, Float>() }
    val isCurrentPageZoomed = (pageScales[pagerState.currentPage] ?: 1f) > 1f

    // Dismiss-by-drag state
    val dismissOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { 150.dp.toPx() }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val marsPhoto = pagingItems.peek(page) ?: return@collect
            onShown(marsPhoto, page)
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

    val currentImage = pagingItems.peek(pagerState.currentPage)

    // Black backdrop stays put so the window background never shows through during dismiss
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dismissOffsetY.value
                // Gently fade the screen as the user drags it down
                alpha = (1f - dismissOffsetY.value / (dismissThresholdPx * 2.5f))
                    .coerceIn(0f, 1f)
            }
            .pointerInput(isCurrentPageZoomed) {
                if (isCurrentPageZoomed) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dismissOffsetY.value > dismissThresholdPx) {
                            onBack()
                        } else {
                            scope.launch {
                                dismissOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { dismissOffsetY.animateTo(0f) }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // Only respond to downward drags (or releasing back up once started)
                        if (dragAmount > 0f || dismissOffsetY.value > 0f) {
                            change.consume()
                            scope.launch {
                                dismissOffsetY.snapTo(
                                    (dismissOffsetY.value + dragAmount).coerceAtLeast(0f)
                                )
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
            onTap = onTap,
            onFavoriteClick = onFavoriteClick,
            onScaleChange = { page, scale -> pageScales[page] = scale },
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
                            SaveIcon(onClick = { onSaveClick(currentImage) })
                            ShareIcon(onClick = { onShareClick(currentImage) })
                            InfoIcon(onClick = { showInfoSheet = true })
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
    pagingItems: LazyPagingItems<MarsImage>,
    hideUi: Boolean,
    onTap: () -> Unit,
    onFavoriteClick: (MarsImage, Boolean) -> Unit,
    onScaleChange: (page: Int, scale: Float) -> Unit,
) {
    // Immediate visual feedback for the heart: the paged item's favorite flag is correct on load
    // (merged from Room by the write-through cache) but can't be mutated in place after a tap,
    // so the override holds the desired state until the page is re-fetched.
    val favoriteOverrides = remember { mutableStateMapOf<String, Boolean>() }

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
                NetworkImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(
                            zoomState = zoomState,
                            scrollGesturePropagation = ScrollGesturePropagation.NotZoomed,
                            enableOneFingerZoom = false,
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onTap() })
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
                        val checked = favoriteOverrides[marsImage.id] ?: marsImage.favorite
                        MarsImageFavoriteToggle(
                            modifier = Modifier.size(64.dp),
                            checked = checked,
                            onCheckedChange = {
                                val desired = !checked
                                favoriteOverrides[marsImage.id] = desired
                                onFavoriteClick(marsImage, desired)
                            }
                        )
                    }
                }
            }
        }
    }
}
