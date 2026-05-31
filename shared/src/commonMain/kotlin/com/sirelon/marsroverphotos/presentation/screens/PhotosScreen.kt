package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.sirelon.marsroverphotos.presentation.ui.adaptiveGridCells
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosUiState
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import com.sirelon.marsroverphotos.shared.resources.did_you_know
import com.sirelon.marsroverphotos.shared.resources.educational_fact
import com.sirelon.marsroverphotos.shared.resources.no_photos_title
import com.sirelon.marsroverphotos.shared.resources.tap_to_retry
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

// ── State-holder composable ───────────────────────────────────────────────────

/**
 * State-holder entry point: collects [PhotosViewModel.uiState] and delegates
 * all layout to the private [PhotosScreen] UI overload.
 *
 * The Sol and Earth-date pickers are separate navigation entries (sharing this
 * screen's [PhotosViewModel]); tapping a date selector navigates to them via
 * [onOpenSolPicker] / [onOpenEarthDatePicker].
 */
@Composable
fun PhotosScreen(
    roverId: Long,
    onNavigateToImages: (clickedId: String, allIds: List<String>) -> Unit,
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

    PhotosScreen(
        state = state,
        onRandomize = viewModel::randomize,
        onGoToLatest = viewModel::goToLatest,
        onPhotoClick = viewModel::onPhotoClick,
        onSetCameraFilter = viewModel::setCameraFilter,
        onClearCameraFilter = onClearCameraFilter,
        onNavigateToImages = { clickedId, allIds -> onNavigateToImages(clickedId, allIds) },
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
    onRandomize: () -> Unit,
    onGoToLatest: () -> Unit,
    onPhotoClick: () -> Unit,
    onSetCameraFilter: (String?) -> Unit,
    onClearCameraFilter: () -> Unit,
    onNavigateToImages: (clickedId: String, allIds: List<String>) -> Unit,
    onBack: () -> Unit,
    onOpenSolPicker: () -> Unit,
    onOpenEarthDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(text = state.roverName)
                },
                subtitle = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateSelectorButton(
                            modifier = Modifier.weight(1f),
                            label = "Sol date",
                            value = state.sol.toString(),
                            onClick = onOpenSolPicker
                        )
                        DateSelectorButton(
                            modifier = Modifier.weight(1f),
                            label = "Earth date",
                            value = state.earthDate,
                            onClick = onOpenEarthDatePicker
                        )
                    }
                },
                onBack = onBack,
            )
        },
        floatingActionButton = {
            RefreshButton(
                visible = state.hasPhotos,
                onClick = onGoToLatest,
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

            Crossfade(targetState = state.gridItems, label = "[Anim]:PhotosProgress") { items ->
                when {
                    items == null -> CenteredProgress()
                    items.isEmpty() -> {
                        if (!state.cameraFilter.isNullOrBlank()) {
                            EmptyPhotos(
                                title = "No ${state.cameraFilter} photos on Sol ${state.sol}. Try another Sol.",
                                btnTitle = stringResource(Res.string.tap_to_retry),
                                callback = { onRandomize() }
                            )
                        } else {
                            EmptyPhotos(
                                title = stringResource(Res.string.no_photos_title),
                                btnTitle = stringResource(Res.string.tap_to_retry),
                                callback = { onRandomize() }
                            )
                        }
                    }

                    else -> {
                        PhotosList(
                            gridItems = items,
                            onPhotoClick = { image ->
                                onPhotoClick()
                                val allIds = items
                                    .filterIsInstance<GridItem.PhotoItem>()
                                    .map { it.id }
                                onNavigateToImages(image.id, allIds)
                            },
                            onCameraClick = { cameraName -> onSetCameraFilter(cameraName) }
                        )
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
                contentDescription = "Reload random Sol"
            )
        }
    }
}

@Composable
private fun PhotosList(
    gridItems: List<GridItem>,
    onPhotoClick: (image: MarsImage) -> Unit,
    onCameraClick: ((cameraName: String) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = adaptiveGridCells(minColumnWidth = 180.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 8.dp,
            // 56dp FAB + 16dp FAB padding + 8dp margin above FAB
            // nav bar inset is handled by Scaffold's innerPadding on the parent Column
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = gridItems,
            key = { item ->
                when (item) {
                    is GridItem.PhotoItem -> item.id
                    is GridItem.FactItem -> item.id
                }
            },
            contentType = { item ->
                when (item) {
                    is GridItem.PhotoItem -> "MarsPhotoContentType"
                    is GridItem.FactItem -> "FactCardContentType"
                }
            },
            span = { item ->
                when (item) {
                    is GridItem.PhotoItem -> GridItemSpan(1)
                    is GridItem.FactItem -> GridItemSpan(maxLineSpan)
                }
            }
        ) { item ->
            when (item) {
                is GridItem.PhotoItem -> PhotoCard(
                    image = item.image,
                    onPhotoClick = onPhotoClick,
                    onCameraClick = onCameraClick
                )

                is GridItem.FactItem -> FactCard(fact = item.fact)
            }
        }
    }
}

@Composable
private fun PhotoCard(
    image: MarsImage,
    onPhotoClick: (image: MarsImage) -> Unit,
    onCameraClick: ((cameraName: String) -> Unit)? = null
) {
    AppCard(
        modifier = Modifier
            .padding(8.dp)
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(4.dp)
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
private fun DateSelectorButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppOutlinedButton(
        modifier = modifier.animateContentSize(),
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            MaterialSymbolIcon(
                symbol = MaterialSymbol.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                size = 18.dp
            )
        }
    }
}

@Composable
private fun EmptyPhotos(title: String, btnTitle: String, callback: () -> Unit) {
    CenteredColumn(
        modifier = Modifier
            .clickable(onClick = callback)
            .padding(32.dp)
    ) {
        Image(painter = painterResource(Res.drawable.alien_icon), contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = btnTitle, style = MaterialTheme.typography.headlineMedium)
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
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Info,
                    contentDescription = stringResource(Res.string.educational_fact),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(Res.string.did_you_know),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = fact.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PhotosScreenPreview() {
    MaterialTheme {
        PhotosScreen(
            state = PhotosUiState(
                roverName = "Curiosity",
                sol = 3200L,
                earthDate = "Aug 5, 2015",
                gridItems = emptyList(),
                maxSol = 4000L,
                cameraFilter = null,
                hasPhotos = false,
            ),
            onRandomize = {},
            onGoToLatest = {},
            onPhotoClick = {},
            onSetCameraFilter = {},
            onClearCameraFilter = {},
            onNavigateToImages = { _, _ -> },
            onBack = {},
            onOpenSolPicker = {},
            onOpenEarthDatePicker = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PhotosScreenWithPhotosPreview() {
    val sampleImage = MarsImage(
        id = "123",
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
    MaterialTheme {
        PhotosScreen(
            state = PhotosUiState(
                roverName = "Curiosity",
                sol = 3200L,
                earthDate = "Aug 5, 2015",
                gridItems = listOf(
                    com.sirelon.marsroverphotos.presentation.models.GridItem.PhotoItem(sampleImage),
                    com.sirelon.marsroverphotos.presentation.models.GridItem.PhotoItem(
                        sampleImage.copy(id = "124", name = "Curiosity: MAST")
                    ),
                ),
                maxSol = 4000L,
                cameraFilter = null,
                hasPhotos = true,
            ),
            onRandomize = {},
            onGoToLatest = {},
            onPhotoClick = {},
            onSetCameraFilter = {},
            onClearCameraFilter = {},
            onNavigateToImages = { _, _ -> },
            onBack = {},
            onOpenSolPicker = {},
            onOpenEarthDatePicker = {},
        )
    }
}
