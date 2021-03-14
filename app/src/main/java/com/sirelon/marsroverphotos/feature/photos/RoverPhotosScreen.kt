package com.sirelon.marsroverphotos.feature.photos

//import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.request.RequestOptions
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.activity.ui.accent
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.ui.CenteredColumn
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch

/**
 * Created on 07.03.2021 12:46 for Mars-Rover-Photos.
 */
@Composable
fun RoverPhotosScreen(
    activity: AppCompatActivity,
    modifier: Modifier,
    roverId: Long,
    viewModel: PhotosViewModel = viewModel()
) {
    viewModel.setRoverId(roverId)

    val photos: List<MarsImage>? by viewModel.photosFlow.collectAsState(initial = null)

    val sol by viewModel.solFlow.collectAsState(initial = 0)

    var openSolDialog by remember { mutableStateOf(false) }

    var maxSol: Long by remember(calculation = { mutableStateOf(Long.MAX_VALUE) })
    rememberCoroutineScope().launch {
        maxSol = viewModel.maxSol()
    }

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

            }
        }

        val photos = photos
        if (photos == null) {
            CenteredColumn {
                CircularProgressIndicator()
            }
        } else if (photos.isEmpty()) {
            EmptyPhotos(title = "No data here", callback = {
                viewModel.track("click_refresh_no_data")
                viewModel.randomize()
            })
        } else {
            PhotosList(modifier, photos, viewModel, activity)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotosList(
    modifier: Modifier,
    photos: List<MarsImage>,
    viewModel: PhotosViewModel,
    activity: AppCompatActivity
) {
    LazyVerticalGrid(cells = GridCells.Fixed(2), modifier = modifier) {
        items(photos) { image ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onPhotoClick(image)

                        val ids = photos.map { it.id }

                        // Enable camera filter if the same camera was choose.
                        // If all camera choosed then no need to filtering
//                        val cameraFilter = filteredCamera != null
                        val intent =
                            ImageActivity.createIntent(activity, image.id, ids, false)
                        activity.startActivity(intent)
                    },
                shape = MaterialTheme.shapes.large
            ) {
                Column(verticalArrangement = Arrangement.SpaceBetween) {
                    ImageItem(image)
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
                    if (sol ?: 0 > maxSol) {
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
                style = MaterialTheme.typography.h6.copy(color = accent)
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

@Composable
fun ImageItem(marsImage: MarsImage) {
    GlideImage(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1F),
        imageModel = marsImage.imageUrl,
        requestOptions = RequestOptions()
            .optionalCenterCrop(),
        contentScale = ContentScale.Crop,
        circularRevealedEnabled = true,
    )
}

@Preview
@Composable
fun TestA() {
    EmptyPhotos(title = "No data here", callback = { /*TODO*/ })
}

@Composable
fun EmptyPhotos(title: String, callback: () -> Unit) {
    CenteredColumn(
        modifier = Modifier
            .clickable(onClick = callback)
            .padding(horizontal = 16.dp)
    ) {
        Image(painter = painterResource(R.drawable.alien_icon), contentDescription = null)
        Text(text = title, style = MaterialTheme.typography.h4)
        Text(
            text = stringResource(R.string.tap_to_retry),
            style = MaterialTheme.typography.subtitle1
        )
    }
}