package com.sirelon.marsroverphotos.feature

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.size.Scale
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 01.03.2021 22:33 for Mars-Rover-Photos.
 */
@Composable
fun MarsImageComposable(
    modifier: Modifier = Modifier,
    marsImage: MarsImage,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val imageUrl = marsImage.imageUrl

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(data = imageUrl)
            .apply(block = fun ImageRequest.Builder.() {
                crossfade(true)
                scale(Scale.FILL)
            }).build()
    )

    val loading by remember {
        derivedStateOf {
            painter.state is AsyncImagePainter.State.Loading
        }
    }
    Card(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // TODO: some bug here: when flings, by some reason state doesn't update properly.
//            .placeholder(
////                visible = loading,
//                highlight = PlaceholderHighlight.shimmer()
//            )
        ,
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Image(
                modifier = Modifier
                    .defaultMinSize(minHeight = 100.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                painter = painter,
                alignment = Alignment.TopCenter,
                contentDescription = imageUrl
            )

            if (!loading) {
                PhotoStats(marsImage, onFavoriteClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoStats(marsImage: MarsImage, onFavoriteClick: () -> Unit) {
    val stats = marsImage.stats

    FlowRow(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.Center
    ) {

        StatsInfoText(stats.see, Icons.Filled.Visibility, "counterSee")
        StatsInfoText(stats.scale, Icons.Filled.ZoomIn, "counterScale")
        StatsInfoText(stats.save, Icons.Filled.Save, "counterSave")
        StatsInfoText(stats.share, Icons.Filled.Share, "counterShare")

        MarsImageFavoriteToggle(
            modifier = Modifier.fillMaxWidth(),
            checked = marsImage.favorite,
            onCheckedChange = { onFavoriteClick() }
        )
    }
}

@Composable
fun MarsImageFavoriteToggle(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    IconToggleButton(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange
    ) {
        Icon(
//            tint = MaterialTheme.colorScheme.secondary,
            imageVector = if (checked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null // handled by click label of parent
        )
    }
}

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes
    placeholderRes: Int? = R.drawable.img_placeholder,
    imageUrl: String
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(data = imageUrl)
            .apply(block = fun ImageRequest.Builder.() {
                crossfade(true)
                placeholderRes?.let {
                    placeholder(placeholderRes)
                }
            }).build()
    )
    Image(
        modifier = modifier,
        painter = painter,
        contentDescription = imageUrl,
        contentScale = contentScale
    )
}

@Composable
private fun StatsInfoText(counter: Long, image: ImageVector, desc: String) {

    if (counter <= 0) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = rememberVectorPainter(image = image),
            contentDescription = desc,
//            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = counter.toString(),
            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
