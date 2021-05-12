package com.sirelon.marsroverphotos.feature

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.size.Scale
import com.google.accompanist.coil.rememberCoilPainter
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 01.03.2021 22:33 for Mars-Rover-Photos.
 */
@Composable
fun MarsImageComposable(marsImage: MarsImage, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    val ready = remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            ImageLoader(marsImage.imageUrl) { ready.value = true }
//            NetworkImage(
//                modifier = Modifier.requiredHeightIn(200.dp, 400.dp),
//                imageUrl = marsImage.imageUrl
//            )
            if (ready.value) {
                PhotoStats(marsImage, onFavoriteClick)
            }
        }
    }
}

@Composable
fun PhotoStats(marsImage: MarsImage, onFavoriteClick: () -> Unit) {
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
            MarsImageFavoriteToggle(
                checked = marsImage.favorite,
                onCheckedChange = { onFavoriteClick() }
            )
        }
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
            modifier = Modifier.fillMaxSize(),
            tint = colorResource(id = R.color.colorAccent),
            imageVector = if (checked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null // handled by click label of parent
        )
    }
}

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageUrl: String
) {
    val painter = rememberCoilPainter(
        request = imageUrl,
        fadeIn = true,
        previewPlaceholder = R.drawable.img_placeholder
    )
    Image(
        modifier = modifier,
        painter = painter,
        contentDescription = imageUrl,
        contentScale = contentScale
    )
}

@Composable
private fun ImageLoader(imageUrl: String, success: () -> Unit) {

    val painter = rememberCoilPainter(
        request = imageUrl,
        fadeIn = true,
        previewPlaceholder = R.drawable.img_placeholder,
        requestBuilder = {
//            size(1500, 800)
            scale(Scale.FILL)
                .listener(onSuccess = { _, _ -> success() })
        }
    )
    Image(
        modifier = Modifier
            .requiredHeightIn(100.dp, 300.dp)
            .fillMaxWidth(),
        contentScale = ContentScale.FillWidth,
        painter = painter,
        contentDescription = imageUrl
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
