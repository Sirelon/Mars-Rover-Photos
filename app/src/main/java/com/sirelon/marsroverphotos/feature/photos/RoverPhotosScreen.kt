package com.sirelon.marsroverphotos.feature.photos

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.feature.NetworkImage
import com.sirelon.marsroverphotos.feature.navigateToImages
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredProgress
import java.util.Calendar
import java.util.TimeZone

/**
 * Created on 07.03.2021 12:46 for Mars-Rover-Photos.
 */
@ExperimentalAnimationApi
@Composable
fun RoverPhotosScreen(
    activity: FragmentActivity,
    modifier: Modifier = Modifier,
    navHost: NavController,
    roverId: Long,
    viewModel: PhotosViewModel = viewModel()
) {
    viewModel.setRoverId(roverId)

    val photos: List<MarsImage>? by viewModel.photosFlow.collectAsStateWithLifecycle(initialValue = null)

    val sol by viewModel.solFlow.collectAsStateWithLifecycle(initialValue = 0)

    var openSolDialog by remember { mutableStateOf(false) }

    var maxSol: Long by remember(calculation = { mutableStateOf(Long.MAX_VALUE) })
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
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
            )
            HeaderButton("Earth date: \n${viewModel.earthDateStr(sol)}") {
                viewModel.track("click_choose_earth")
                showEarthDateeChooser(activity, viewModel)
            }
        }

        Crossfade(targetState = photos) {
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
                        viewModel.onPhotoClick(image)
                        navHost.navigateToImages(image, it)
                    }
                }

            }
        }
    }

    val fabVisible = photos?.isNotEmpty() == true
    RefreshButton(fabVisible, modifier, viewModel)
}

@ExperimentalAnimationApi
@Composable
private fun RefreshButton(
    fabVisible: Boolean,
    modifier: Modifier,
    viewModel: PhotosViewModel
) {
    AnimatedVisibility(visible = fabVisible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = {
                    viewModel.track("click_refresh")
                    viewModel.goToLatest()
                }) {
                Icon(
                    imageVector = Icons.Filled.Autorenew,
                    contentDescription = "refresh"
                )
            }

        }
    }
}

@Composable
private fun PhotosList(
    modifier: Modifier,
    photos: List<MarsImage>,
    onPhotoClick: (image: MarsImage) -> Unit
) {

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = modifier) {
        items(photos) { image ->
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
                    val title = image.name
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
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
                val context = LocalContext.current
                SolChanger(sol, maxSol) {
                    if ((sol ?: 0) > maxSol) {
                        sol = maxSol
                        Toast.makeText(
                            context,
                            "The max sol for this rover is $maxSol",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        sol = it
                    }
                }
            }
        )
    }
}

@Composable
private fun SolChanger(sol: Long?, maxSol: Long, onSolChanged: (sol: Long?) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val solStr = sol?.toString() ?: ""
            Text(
                text = "Sol: ",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.h6.copy(color = MaterialTheme.colors.secondary)
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

private fun showEarthDateeChooser(activity: FragmentActivity, viewModel: PhotosViewModel) {
    val calender = Calendar.getInstance(TimeZone.getDefault())
    calender.clear()

    val time = viewModel.earthTime()

    calender.timeInMillis = time

    val datePicker = DatePickerDialog(
        activity, { _, year, monthOfYear, dayOfMonth ->
            calender.clear()
            calender.set(year, monthOfYear, dayOfMonth)
            viewModel.setEarthTime(calender.timeInMillis)
        },
        calender.get(Calendar.YEAR),
        calender.get(Calendar.MONTH),
        calender.get(Calendar.DAY_OF_MONTH)
    )

    datePicker.datePicker.maxDate = viewModel.maxDate()
    datePicker.datePicker.minDate = viewModel.minDate()

    val timeFromSol = viewModel.dateFromSol()

    calender.timeInMillis = timeFromSol

    datePicker.updateDate(
        calender.get(Calendar.YEAR),
        calender.get(Calendar.MONTH),
        calender.get(Calendar.DAY_OF_MONTH)
    )
    datePicker.show()

//    findViewById<View>(R.id.dateEarthChoose).setOnClickListener {
//        // UPDATE TIME
//        val timeFromSol = viewModel.dateFromSol()
//
//        calender.timeInMillis = timeFromSol
//
//        datePicker.updateDate(
//            calender.get(Calendar.YEAR),
//            calender.get(Calendar.MONTH),
//            calender.get(Calendar.DAY_OF_MONTH)
//        )
//
//        // Hide title. Need to set AFTER all
//        datePicker.setTitle("")
//        // SHOW DIALOG
//        datePicker.show()
//    }
}