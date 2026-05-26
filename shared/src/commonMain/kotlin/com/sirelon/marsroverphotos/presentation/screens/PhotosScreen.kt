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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.EducationalFact
import com.sirelon.marsroverphotos.presentation.models.GridItem
import com.sirelon.marsroverphotos.presentation.ui.CenteredColumn
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.ui.PlatformDatePickerDialog
import com.sirelon.marsroverphotos.presentation.ui.adaptiveGridCells
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import com.sirelon.marsroverphotos.shared.resources.cancel
import com.sirelon.marsroverphotos.shared.resources.choose
import com.sirelon.marsroverphotos.shared.resources.did_you_know
import com.sirelon.marsroverphotos.shared.resources.educational_fact
import com.sirelon.marsroverphotos.shared.resources.no_photos_title
import com.sirelon.marsroverphotos.shared.resources.select
import com.sirelon.marsroverphotos.shared.resources.select_date
import com.sirelon.marsroverphotos.shared.resources.sol_description
import com.sirelon.marsroverphotos.shared.resources.sol_label
import com.sirelon.marsroverphotos.shared.resources.sol_max_error_fmt
import com.sirelon.marsroverphotos.shared.resources.tap_to_retry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    roverId: Long,
    onNavigateToImages: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cameraFilter: String? = null,
    onClearCameraFilter: () -> Unit = {},
    viewModel: PhotosViewModel = koinViewModel()
) {
    var maxSol by remember { mutableLongStateOf(1L) }

    LaunchedEffect(roverId) {
        viewModel.setRoverId(roverId)
        maxSol = viewModel.maxSol().coerceAtLeast(1L)
    }

    LaunchedEffect(cameraFilter) {
        viewModel.setCameraFilter(cameraFilter)
    }

    val gridItems by viewModel.gridItemsFlow.collectAsState(initial = null)
    val photos = remember(gridItems) {
        gridItems?.mapNotNull { item ->
            (item as? GridItem.PhotoItem)?.image
        }.orEmpty()
    }
    val sol by viewModel.solFlow.collectAsState(initial = 0L)
    val roverName by viewModel.roverNameFlow.collectAsState(initial = "")

    var openSolDialog by rememberSaveable { mutableStateOf(false) }
    var openEarthDateDialog by rememberSaveable { mutableStateOf(false) }

    SolDialog(
        maxSol = maxSol,
        openDialog = openSolDialog,
        sol = sol,
        onClose = { openSolDialog = false },
        onChoose = {
            viewModel.loadBySol(it)
            openSolDialog = false
        }
    )

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Column {
                        Text(text = roverName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DateSelectorButton(
                                modifier = Modifier.weight(1f),
                                label = "Sol date",
                                value = sol.toString(),
                                onClick = { openSolDialog = true }
                            )
                            DateSelectorButton(
                                modifier = Modifier.weight(1f),
                                label = "Earth date",
                                value = viewModel.earthDateStr(sol),
                                onClick = { openEarthDateDialog = true }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            RefreshButton(
                visible = photos.isNotEmpty(),
                onClick = { viewModel.goToLatest() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!cameraFilter.isNullOrBlank()) {
                CameraFilterChip(
                    camera = cameraFilter,
                    onClear = {
                        viewModel.setCameraFilter(null)
                        onClearCameraFilter()
                    }
                )
            }

            if (openEarthDateDialog) {
                PlatformDatePickerDialog(
                    selectedDate = millisToUtcLocalDate(viewModel.dateFromSol()),
                    minDate = millisToUtcLocalDate(viewModel.minDate()),
                    maxDate = millisToUtcLocalDate(viewModel.maxDate()),
                    onDateSelected = { selectedDate ->
                        viewModel.setEarthTime(selectedDate.toUtcMillis())
                        openEarthDateDialog = false
                    },
                    onDismissRequest = { openEarthDateDialog = false },
                    title = stringResource(Res.string.select_date),
                    confirmText = stringResource(Res.string.select),
                    dismissText = stringResource(Res.string.cancel)
                )
            }

            Crossfade(targetState = gridItems, label = "[Anim]:PhotosProgress") { items ->
                when {
                    items == null -> CenteredProgress()
                    items.isEmpty() -> {
                        if (!cameraFilter.isNullOrBlank()) {
                            EmptyPhotos(
                                title = "No $cameraFilter photos on Sol $sol. Try another Sol.",
                                btnTitle = stringResource(Res.string.tap_to_retry),
                                callback = { viewModel.randomize() }
                            )
                        } else {
                            EmptyPhotos(
                                title = stringResource(Res.string.no_photos_title),
                                btnTitle = stringResource(Res.string.tap_to_retry),
                                callback = { viewModel.randomize() }
                            )
                        }
                    }

                    else -> {
                        PhotosList(
                            gridItems = items,
                            onPhotoClick = { image ->
                                viewModel.onPhotoClick()
                                onNavigateToImages(image.id)
                            },
                            onCameraClick = { cameraName -> viewModel.setCameraFilter(cameraName) }
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
        FloatingActionButton(onClick = onClick) {
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
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onPhotoClick(image) },
        shape = MaterialTheme.shapes.large
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
private fun SolDialog(
    openDialog: Boolean,
    maxSol: Long,
    sol: Long,
    onClose: () -> Unit,
    onChoose: (sol: Long) -> Unit
) {
    if (openDialog) {
        var selectedSol: Long? by remember(sol) { mutableStateOf(sol) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = onClose,
            confirmButton = {
                TextButton(
                    onClick = {
                        val resolvedSol = selectedSol
                        if (resolvedSol != null) {
                            onChoose(resolvedSol)
                        } else {
                            onClose()
                        }
                    }
                ) {
                    Text(stringResource(Res.string.choose))
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            title = { Text(text = stringResource(Res.string.sol_description)) },
            text = {
                val maxSolError = stringResource(Res.string.sol_max_error_fmt, maxSol)
                SolChanger(selectedSol, maxSol, errorMessage) { nextSol ->
                    if ((nextSol ?: 0L) > maxSol) {
                        selectedSol = maxSol
                        errorMessage = maxSolError
                    } else {
                        selectedSol = nextSol
                        errorMessage = null
                    }
                }
            }
        )
    }
}

@Composable
private fun SolChanger(
    sol: Long?,
    maxSol: Long,
    errorMessage: String?,
    onSolChanged: (sol: Long?) -> Unit
) {
    val sliderMax = maxSol.coerceAtLeast(1L)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.sol_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                value = sol?.toString().orEmpty(),
                modifier = Modifier.weight(1f),
                singleLine = true,
                onValueChange = { onSolChanged(it.toLongOrNull()) }
            )
        }
        Slider(
            value = (sol ?: 0L).coerceIn(0L, sliderMax).toFloat(),
            valueRange = 0f..sliderMax.toFloat(),
            onValueChange = { onSolChanged(it.toLong()) }
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
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
    OutlinedButton(
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(Res.string.did_you_know),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = fact.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun millisToUtcLocalDate(timeMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(timeMillis).toLocalDateTime(TimeZone.UTC).date

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

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
