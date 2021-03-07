package com.sirelon.marsroverphotos.feature.photos

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.request.RequestOptions
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.storage.MarsImage
import com.skydoves.landscapist.glide.GlideImage

/**
 * Created on 07.03.2021 12:46 for Mars-Rover-Photos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoverPhotosScreen(
    activity: AppCompatActivity,
    modifier: Modifier,
    roverId: Long,
    viewModel: PhotosViewModel = viewModel()
) {
    viewModel.setRoverId(roverId)

    val photos: List<MarsImage> by viewModel.photosFlow.collectAsState(initial = emptyList())
    val sol by viewModel.solFlow.collectAsState(initial = 0)

    Column {
        Row(modifier = Modifier.height(52.dp)) {
            HeaderButton("Sol date: \n$sol") {

            }
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
            )
            HeaderButton("Earth date: \n${viewModel.earthDateStr(sol)}") {

            }
        }
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
                            val intent = ImageActivity.createIntent(activity, image.id, ids, false)
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
}

@Composable
private fun RowScope.HeaderButton(txt: String, onClick: () -> Unit) {
    TextButton(
        modifier = Modifier
        .weight(1f)
        .animateContentSize(),
        onClick = onClick) {
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