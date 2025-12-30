package com.sirelon.marsroverphotos.feature.photos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.feature.NetworkImage
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import com.sirelon.marsroverphotos.ui.MaterialSymbol
import com.sirelon.marsroverphotos.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.ui.adaptiveGridCells
import java.util.Calendar
import java.util.TimeZone

/**
 * Created on 07.03.2021 12:46 for Mars-Rover-Photos.
 */
@ExperimentalAnimationApi
@Composable
fun RoverPhotosScreen(
    modifier: Modifier = Modifier,
    roverId: Long,
    onNavigateToImages: (MarsImage, List<MarsImage>) -> Unit,
    viewModel: PhotosViewModel = koinViewModel()
) {
    viewModel.setRoverId(roverId)

    val gridItems: List<GridItem>? by viewModel.gridItemsFlow.collectAsStateWithLifecycle(initialValue = null)
    val photos = remember(gridItems) {
        gridItems?.mapNotNull { item ->
            (item as? GridItem.PhotoItem)?.image
        } ?: emptyList()
    }

    val sol by viewModel.solFlow.collectAsStateWithLifecycle(initialValue = 0)

    var openSolDialog by remember { mutableStateOf(false) }
    var openEarthDateDialog by rememberSaveable { mutableStateOf(false) }

    var maxSol: Long by remember(calculation = { mutableLongStateOf(Long.MAX_VALUE) })
    LaunchedEffect(key1 = maxSol, block = {
        maxSol = viewModel.maxSol()
    })

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

    Column {
        Row(modifier = Modifier.height(52.dp)) {
            HeaderButton("Sol date: \n$sol") {
                openSolDialog = true
            }
            HorizontalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
            )
            HeaderButton("Earth date: \n${viewModel.earthDateStr(sol)}") {
                viewModel.track("click_choose_earth")
                openEarthDateDialog = true
            }
        }

        if (openEarthDateDialog) {
            EarthDatePickerDialog(
                selectedDateMillis = viewModel.dateFromSol(),
                minDateMillis = viewModel.minDate(),
                maxDateMillis = viewModel.maxDate(),
                onDismiss = { openEarthDateDialog = false },
                onDateSelected = { selectedMillis ->
                    viewModel.setEarthTime(selectedMillis)
                    openEarthDateDialog = false
                }
            )
        }

        Crossfade(targetState = gridItems, label = "[Anim]:Progress") {
            when {
                it == null -> CenteredProgress()
                it.isEmpty() -> {
                    EmptyPhotos(
                        title = stringResource(id = R.string.no_photos_title),
                        btnTitle = stringResource(R.string.tap_to_retry),
                        callback = {
                            viewModel.track("click_refresh_no_data")
                            viewModel.randomize()
                        })
                }

                else -> {
                    PhotosList(modifier, it) { image ->
                        viewModel.onPhotoClick()
                        onNavigateToImages(image, photos)
                    }
                }

            }
        }
    }

    val fabVisible = photos.isNotEmpty()

    RefreshButton(
        fabVisible = fabVisible,
        modifier = modifier,
        onClick = {
            viewModel.track("click_refresh")
            viewModel.goToLatest()
        },
    )
}

@ExperimentalAnimationApi
@Composable
private fun RefreshButton(
    fabVisible: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    AnimatedVisibility(visible = fabVisible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = onClick
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Autorenew,
                    contentDescription = "refresh"
                )
            }

        }
    }
}

@Composable
private fun PhotosList(
    modifier: Modifier,
    gridItems: List<GridItem>,
    onPhotoClick: (image: MarsImage) -> Unit
) {

    LazyVerticalGrid(
        columns = adaptiveGridCells(minColumnWidth = 160.dp),
        modifier = modifier
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
                    is GridItem.PhotoItem -> androidx.compose.foundation.lazy.grid.GridItemSpan(1)
                    is GridItem.FactItem -> androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)
                }
            }
        ) { item ->
            when (item) {
                is GridItem.PhotoItem -> PhotoCard(image = item.image, onPhotoClick = onPhotoClick)
                is GridItem.FactItem -> FactCard(fact = item.fact)
            }
        }
    }
}

@Composable
private fun PhotoCard(
    image: MarsImage,
    onPhotoClick: (image: MarsImage) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                onPhotoClick(image)
            },
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
                text = image.name.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp),
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
        var sol: Long? by remember(calculation = { mutableStateOf(sol) })
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = onClose,
            confirmButton = {
                TextButton(onClick = {
                    if (sol != null) {
                        onChoose(sol!!)
                    } else {
                        onClose()
                    }
                }) {
                    Text("Choose")
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("Cancel")
                }
            },
            title = { Text(text = stringResource(id = R.string.sol_description)) },
            text = {
                SolChanger(sol, maxSol, errorMessage) {
                    if ((it ?: 0) > maxSol) {
                        sol = maxSol
                        errorMessage = "The max sol for this rover is $maxSol"
                    } else {
                        sol = it
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
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val solStr = sol?.toString() ?: ""
            Text(
                text = "Sol: ",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge.copy(
                )
            )
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                value = solStr,
                modifier = Modifier.weight(1f),
                onValueChange = {
                    onSolChanged(it.toLongOrNull())
                })
        }
        Slider(
            value = sol?.toFloat() ?: 0f,
            valueRange = 0f..maxSol.toFloat(),
            onValueChange = { onSolChanged(it.toLong()) })
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
private fun RowScope.HeaderButton(txt: String, onClick: () -> Unit) {
    TextButton(
        modifier = Modifier
            .weight(1f)
            .animateContentSize(),
        onClick = onClick
    ) {
        Text(
            text = txt,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EarthDatePickerDialog(
    selectedDateMillis: Long,
    minDateMillis: Long,
    maxDateMillis: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val initialUtcMillis = localDateToUtcMillis(selectedDateMillis)
    val minUtcMillis = localDateToUtcMillis(minDateMillis)
    val maxUtcMillis = localDateToUtcMillis(maxDateMillis)

    val selectableDates = remember(minUtcMillis, maxUtcMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in minUtcMillis..maxUtcMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year in extractYear(minDateMillis)..extractYear(maxDateMillis)
            }
        }
    }

    val yearRange = extractYear(minDateMillis)..extractYear(maxDateMillis)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis,
        initialDisplayedMonthMillis = initialUtcMillis,
        yearRange = yearRange,
        selectableDates = selectableDates
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedUtc = datePickerState.selectedDateMillis ?: initialUtcMillis
                onDateSelected(utcMillisToLocalDateMillis(selectedUtc))
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(text = "Select date")
                }
            )
        }
    )
}

private fun extractYear(timeMillis: Long): Int {
    val calendar = Calendar.getInstance(TimeZone.getDefault())
    calendar.timeInMillis = timeMillis
    return calendar.get(Calendar.YEAR)
}

private fun localDateToUtcMillis(localTimeMillis: Long): Long {
    val localCalendar = Calendar.getInstance(TimeZone.getDefault())
    localCalendar.timeInMillis = localTimeMillis
    val year = localCalendar.get(Calendar.YEAR)
    val month = localCalendar.get(Calendar.MONTH)
    val day = localCalendar.get(Calendar.DAY_OF_MONTH)
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utcCalendar.clear()
    utcCalendar.set(year, month, day)
    return utcCalendar.timeInMillis
}

private fun utcMillisToLocalDateMillis(utcTimeMillis: Long): Long {
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utcCalendar.timeInMillis = utcTimeMillis
    val year = utcCalendar.get(Calendar.YEAR)
    val month = utcCalendar.get(Calendar.MONTH)
    val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
    val localCalendar = Calendar.getInstance(TimeZone.getDefault())
    localCalendar.clear()
    localCalendar.set(year, month, day)
    return localCalendar.timeInMillis
}
