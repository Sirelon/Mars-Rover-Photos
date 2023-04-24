package com.sirelon.marsroverphotos.feature

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
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
            tint = MaterialTheme.colors.secondary,
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
            tint = MaterialTheme.colors.secondary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = counter.toString(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface
        )
    }
}
