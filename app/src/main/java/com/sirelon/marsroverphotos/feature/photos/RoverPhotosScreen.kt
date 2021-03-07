package com.sirelon.marsroverphotos.feature.photos

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.request.RequestOptions
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.storage.MarsImage
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

    LazyVerticalGrid(cells = GridCells.Fixed(2), modifier = modifier) {
        items(photos) { image ->
//            MarsImageComposable(
//                marsImage = it,
//                onClick = { /*TODO*/ },
//                onFavoriteClick = { /*TODO*/ })

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
                ImageItem(image)
            }
        }
    }
}

@Composable
fun ImageItem(marsImage: MarsImage) {
    GlideImage(
        modifier = Modifier.fillMaxSize(),
        imageModel = marsImage.imageUrl,
        requestOptions = RequestOptions()
//            .override(800, 400)
            .optionalCenterCrop(),
        contentScale = ContentScale.Crop,
//        circularRevealedEnabled = true,
    )
}