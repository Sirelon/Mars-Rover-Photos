package com.sirelon.marsroverphotos.feature

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.request.RequestOptions
import com.sirelon.marsroverphotos.storage.MarsImage
import com.skydoves.landscapist.glide.GlideImage

/**
 * Created on 01.03.2021 22:33 for Mars-Rover-Photos.
 */
@Composable
fun ImageItem(marsImage: MarsImage) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        GlideImage(
//            imageRequest = ImageRequest.Builder(LocalContext.current)
//                .data(marsImage.imageUrl)
//                .size(800, 800)
//                .build(),
            imageModel = marsImage.imageUrl,
            requestOptions = RequestOptions()
                .override(1500, 800)
                .centerCrop(),
            contentScale = ContentScale.Crop,
            circularRevealedEnabled = true,
        )
    }
}
