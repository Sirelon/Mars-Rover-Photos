package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.presentation.models.GridItem
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import com.sirelon.marsroverphotos.presentation.ui.AppFactCard
import com.sirelon.marsroverphotos.presentation.ui.AppFloatingActionButton
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.CenteredColumn
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosUiState
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import com.sirelon.marsroverphotos.shared.resources.did_you_know
import com.sirelon.marsroverphotos.shared.resources.educational_fact
import com.sirelon.marsroverphotos.shared.resources.no_photos_title
import com.sirelon.marsroverphotos.shared.resources.tap_to_retry
import com.sirelon.marsroverphotos.utils.formatDisplayDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

// ── State-holder composable ───────────────────────────────────────────────────

/**
 * State-holder entry point: collects [PhotosViewModel.uiState] and the paged feed, and
 * delegates layout to the private [PhotosScreen] UI overload.
 *
 * The feed is an infinite, bidirectional sol-paged list (`gridItemsFlow`). The floating date
 * chip mirrors the top-visible day and opens the Sol/Earth pickers (separate nav entries that
 * share this screen's [PhotosViewModel]).
 */
@Composable
fun PhotosScreen(
    roverId: Long,
    onNavigateToImages: (clickedId: String) -> Unit,
    onBack: () -> Unit,
    onOpenSolPicker: () -> Unit,
    onOpenEarthDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
    cameraFilter: String? = null,
    onClearCameraFilter: () -> Unit = {},
    viewModel: PhotosViewModel = koinViewModel()
) {
    LaunchedEffect(roverId) {
        viewModel.setRoverId(roverId)
    }

    LaunchedEffect(cameraFilter) {
        viewModel.setCameraFilter(cameraFilter)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.gridItemsFlow.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    // Keep the floating chip + pickers in sync with the top-visible day as the user scrolls.
    val currentPagingItems = rememberUpdatedState(pagingItems)
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .collect { index ->
                solAtIndex(currentPagingItems.value, index)?.let(viewModel::onVisibleSolChanged)
            }
    }

    // On return from the fullscreen viewer, jump the grid to the photo the user was last viewing
    // there. The detail pager and this grid share the same RoverFeedPager pages, so that photo is
    // already in the loaded snapshot; we locate it by id (its grid index differs from the pager's
    // because of the date headers and fact cards interleaved here). Runs once per (re)entry; the
    // id is cleared on read so later manual scrolls aren't overridden.
    LaunchedEffect(Unit) {
        val targetId = viewModel.consumeLastViewedPhotoId() ?: return@LaunchedEffect
        val index = snapshotFlow {
            pagingItems.itemSnapshotList.indexOfFirst {
                (it as? GridItem.PhotoItem)?.image?.id == targetId
            }
        }.first { it >= 0 }
        gridState.scrollToItem(index)
    }

    PhotosScreen(
        state = state,
        pagingItems = pagingItems,
        gridState = gridState,
        onRandomize = viewModel::randomize,
        onGoToLatest = viewModel::goToLatest,
        onSetCameraFilter = viewModel::setCameraFilter,
        onClearCameraFilter = onClearCameraFilter,
        onNavigateToImages = onNavigateToImages,
        onBack = onBack,
        onOpenSolPicker = onOpenSolPicker,
        onOpenEarthDatePicker = onOpenEarthDatePicker,
        modifier = modifier,
    )
}

// ── UI composable ─────────────────────────────────────────────────────────────

/**
 * Pure UI overload: knows nothing about [PhotosViewModel]. Safe to preview and unit-test.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotosScreen(
    state: PhotosUiState,
    pagingItems: LazyPagingItems<GridItem>,
    gridState: LazyGridState,
    onRandomize: () -> Unit,
    onGoToLatest: () -> Unit,
    onSetCameraFilter: (String?) -> Unit,
    onClearCameraFilter: () -> Unit,
    onNavigateToImages: (clickedId: String) -> Unit,
    onBack: () -> Unit,
    onOpenSolPicker: () -> Unit,
    onOpenEarthDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val refresh = pagingItems.loadState.refresh

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(),
        topBar = {
            AppTopBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = state.roverName) },
                onBack = onBack,
            )
        },
        floatingActionButton = {
            RefreshButton(
                visible = pagingItems.itemCount > 0,
                onClick = onGoToLatest
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!state.cameraFilter.isNullOrBlank()) {
                CameraFilterChip(
                    camera = state.cameraFilter,
                    onClear = {
                        onSetCameraFilter(null)
                        onClearCameraFilter()
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    refresh is LoadState.Loading && pagingItems.itemCount == 0 -> CenteredProgress()

                    refresh is LoadState.Error && pagingItems.itemCount == 0 -> EmptyPhotos(
                        title = stringResource(Res.string.no_photos_title),
                        btnTitle = stringResource(Res.string.tap_to_retry),
                        callback = { pagingItems.retry() }
                    )

                    pagingItems.itemCount == 0 -> EmptyPhotos(
                        title = if (!state.cameraFilter.isNullOrBlank()) {
                            "No ${state.cameraFilter} photos near Sol ${state.sol}. Try another Sol."
                        } else {
                            stringResource(Res.string.no_photos_title)
                        },
                        btnTitle = stringResource(Res.string.tap_to_retry),
                        callback = onRandomize
                    )

                    else -> {
                        PhotosGrid(
                            pagingItems = pagingItems,
                            gridState = gridState,
                            onPhotoClick = { image -> onNavigateToImages(image.id) },
                            onCameraClick = onSetCameraFilter,
                        )

                        // Floating "sticky" date chip — mirrors the top-visible day.
                        FloatingDateChip(
                            sol = state.sol,
                            earthDate = state.earthDate,
                            onOpenSolPicker = onOpenSolPicker,
                            onOpenEarthDatePicker = onOpenEarthDatePicker,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = AppSpacing.sm)
                        )

                        // Prepend (scroll-up) progress at the top, append (scroll-down) at the bottom.
                        if (pagingItems.loadState.prepend is LoadState.Loading) {
                            EdgeProgress(modifier = Modifier.align(Alignment.TopCenter))
                        }
                        if (pagingItems.loadState.append is LoadState.Loading) {
                            EdgeProgress(modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        AppFloatingActionButton(onClick = onClick) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Autorenew,
                contentDescription = "Jump to latest Sol"
            )
        }
    }
}

@Composable
private fun PhotosGrid(
    pagingItems: LazyPagingItems<GridItem>,
    gridState: LazyGridState,
    onPhotoClick: (image: MarsImage) -> Unit,
    onCameraClick: ((cameraName: String) -> Unit)? = null
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppSpacing.md,
            end = AppSpacing.md,
            // Leave room for the floating date chip at the top.
            top = 52.dp,
            // 56dp FAB + 16dp FAB padding + 8dp margin above FAB
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.gridKey },
            contentType = pagingItems.itemContentType { it.contentType },
            span = { index ->
                when (pagingItems.peek(index)) {
                    is GridItem.PhotoItem -> GridItemSpan(1)
                    else -> GridItemSpan(maxLineSpan)
                }
            }
        ) { index ->
            when (val item = pagingItems[index]) {
                is GridItem.PhotoItem -> PhotoCard(
                    image = item.image,
                    onPhotoClick = onPhotoClick,
                    onCameraClick = onCameraClick
                )

                is GridItem.DateHeader -> DateHeaderRow(header = item)

                is GridItem.FactItem -> FactCard(fact = item.fact)

                null -> Unit
            }
        }
    }
}

@Composable
private fun DateHeaderRow(
    header: GridItem.DateHeader,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text = formatDisplayDate(header.earthDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Sol ${header.sol}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FloatingDateChip(
    sol: Long,
    earthDate: String,
    onOpenSolPicker: () -> Unit,
    onOpenEarthDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (earthDate.isNotBlank()) "Sol $sol · $earthDate" else "Sol $sol",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    size = 18.dp
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Pick by Sol") },
                onClick = {
                    expanded = false
                    onOpenSolPicker()
                }
            )
            DropdownMenuItem(
                text = { Text("Pick by Earth date") },
                onClick = {
                    expanded = false
                    onOpenEarthDatePicker()
                }
            )
        }
    }
}

@Composable
private fun EdgeProgress(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PhotoCard(
    image: MarsImage,
    onPhotoClick: (image: MarsImage) -> Unit,
    onCameraClick: ((cameraName: String) -> Unit)? = null
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPhotoClick(image) },
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            NetworkImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1F),
                imageUrl = image.imageUrl
            )
            Text(
                text = shortCaption(image.name.orEmpty()),
                style = AppTypography.photoCaption,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(AppSpacing.xs)
                    .fillMaxWidth()
                    .then(
                        if (onCameraClick != null && image.camera != null) {
                            Modifier.clickable { onCameraClick(image.camera.name) }
                        } else Modifier
                    ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyPhotos(title: String, btnTitle: String, callback: () -> Unit) {
    CenteredColumn(
        modifier = Modifier
            .clickable(onClick = callback)
            .padding(AppSpacing.xxl)
    ) {
        Image(painter = painterResource(Res.drawable.alien_icon), contentDescription = null)
        Spacer(modifier = Modifier.size(AppSpacing.sm))
        Text(
            text = title,
            style = AppTypography.appTitle,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(AppSpacing.sm))
        Text(text = btnTitle, style = AppTypography.roverTitle)
    }
}

@Composable
private fun FactCard(
    fact: EducationalFact,
    modifier: Modifier = Modifier
) {
    AppFactCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.md),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Info,
                    contentDescription = stringResource(Res.string.educational_fact),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(Res.string.did_you_know),
                    style = AppTypography.factHeader,
                )
            }

            Text(
                text = fact.text,
                style = AppTypography.body,
                modifier = Modifier.padding(top = AppSpacing.xs)
            )
        }
    }
}

private fun shortCaption(full: String): String =
    full.substringAfter(": ", missingDelimiterValue = full).trim()

@Composable
private fun CameraFilterChip(camera: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onClear,
            label = { Text("Camera: $camera ×") },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val GridItem.gridKey: String
    get() = when (this) {
        is GridItem.PhotoItem -> id
        is GridItem.FactItem -> id
        is GridItem.DateHeader -> id
    }

private val GridItem.contentType: String
    get() = when (this) {
        is GridItem.PhotoItem -> "photo"
        is GridItem.FactItem -> "fact"
        is GridItem.DateHeader -> "header"
    }

/**
 * Sol of the day at (or just above) [index] — used to drive the floating header. For a
 * [GridItem.FactItem] (no sol of its own) it scans backward to the nearest photo/header.
 */
private fun solAtIndex(pagingItems: LazyPagingItems<GridItem>, index: Int): Long? {
    if (pagingItems.itemCount == 0) return null
    var i = index.coerceIn(0, pagingItems.itemCount - 1)
    while (i >= 0) {
        when (val item = pagingItems.peek(i)) {
            is GridItem.PhotoItem -> return item.image.sol
            is GridItem.DateHeader -> return item.sol
            else -> i--
        }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PhotosScreenPreview() {
    val sampleImage = MarsImage(
        id = "preview_1",
        order = 0,
        sol = 3200L,
        name = "Curiosity: FHAZ",
        imageUrl = "https://example.com/img.jpg",
        earthDate = "2015-08-05",
        camera = com.sirelon.marsroverphotos.domain.models.RoverCamera(
            id = 1,
            name = "FHAZ",
            fullName = "Front Hazard Avoidance Camera"
        ),
        favorite = false,
        popular = false,
        stats = MarsImage.Stats(see = 42, scale = 0, save = 2, share = 1, favorite = 5),
    )
    val sampleItems: List<GridItem> = listOf(
        GridItem.DateHeader(sol = 3200L, earthDate = "2015-08-05"),
        GridItem.PhotoItem(sampleImage),
        GridItem.PhotoItem(sampleImage.copy(id = "preview_2", name = "Curiosity: MAST")),
    )
    val pagingItems = flowOf(PagingData.from(sampleItems)).collectAsLazyPagingItems()
    MaterialTheme {
        PhotosScreen(
            state = PhotosUiState(
                roverName = "Curiosity",
                sol = 3200L,
                earthDate = "Aug 5, 2015",
                maxSol = 4000L,
                cameraFilter = null,
            ),
            pagingItems = pagingItems,
            gridState = rememberLazyGridState(),
            onRandomize = {},
            onGoToLatest = {},
            onSetCameraFilter = {},
            onClearCameraFilter = {},
            onNavigateToImages = {},
            onBack = {},
            onOpenSolPicker = {},
            onOpenEarthDatePicker = {},
        )
    }
}
