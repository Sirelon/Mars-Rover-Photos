package com.sirelon.marsroverphotos.feature

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.feature.favorite.FavoriteImagesViewModel
import com.sirelon.marsroverphotos.storage.MarsImage
import com.skydoves.landscapist.glide.GlideImage

/**
 * Created on 01.03.2021 22:33 for Mars-Rover-Photos.
 */
@Composable
fun MarsImageComposable(marsImage: MarsImage) {
    val ready = remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        Column {
            ImageLoader(marsImage.imageUrl) { ready.value = true }
            if (ready.value) {
                PhotoStats(marsImage)
            }
        }
    }
}

@Composable
fun PhotoStats(marsImage: MarsImage, favoriteImagesViewModel: FavoriteImagesViewModel = viewModel()) {
    val stats = marsImage.stats

    Row(
//        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column {
            StatsInfoText(stats.see, R.drawable.ic_see_counter, "counterSee")
            StatsInfoText(stats.scale, R.drawable.ic_scale_counter, "counterScale")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            StatsInfoText(stats.save, R.drawable.ic_save_counter, "counterSave")
            StatsInfoText(stats.share, R.drawable.ic_share_counter, "counterShare")
        }
//        val name = marsImage.name
//        if (name == null) {
//            Spacer(modifier = Modifier.width(8.dp))
//        } else {
//            Text(
//                modifier = Modifier.padding(horizontal = 8.dp),
//                textAlign = TextAlign.Center,
//                text = name
//            )
//        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            var favorite by remember { mutableStateOf(marsImage.favorite) }
            IconToggleButton(
                checked = marsImage.favorite,
                onCheckedChange = {
                    favorite = !favorite
                    favoriteImagesViewModel.updateFavForImage(marsImage)
                }) {
                Icon(
                    tint = colorResource(id = R.color.colorAccent),
                    imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null // handled by click label of parent
                )
            }
        }
    }
}

@Composable
private fun ImageLoader(imageUrl: String, success: () -> Unit) {

    val requestListener = object : RequestListener<Bitmap> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Bitmap>?,
            isFirstResource: Boolean
        ): Boolean {
            return false
        }

        override fun onResourceReady(
            resource: Bitmap?,
            model: Any?,
            target: Target<Bitmap>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            success()
            return false
        }
    }
    GlideImage(
        modifier = Modifier.requiredHeightIn(200.dp, 400.dp),
        imageModel = imageUrl,
        requestBuilder = Glide
            .with(LocalContext.current)
            .asBitmap()
            .addListener(requestListener)
            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)),
        requestOptions = RequestOptions()
            .override(1500, 800)
            .optionalCenterCrop(),
        contentScale = ContentScale.Crop,
        circularRevealedEnabled = true,
    )
}

@Composable
private fun StatsInfoText(counter: Long, @DrawableRes drawable: Int, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = drawable),
            contentDescription = desc,
            tint = colorResource(id = R.color.colorAccent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = counter.toString())
    }
}
