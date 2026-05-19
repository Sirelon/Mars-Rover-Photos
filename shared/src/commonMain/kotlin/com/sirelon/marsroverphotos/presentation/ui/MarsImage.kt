package com.sirelon.marsroverphotos.presentation.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.img_placeholder
import org.jetbrains.compose.resources.painterResource

/**
 * Created on 01.03.2021 22:33 for Mars-Rover-Photos.
 * Ported to Compose Multiplatform (KMP).
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
        ImageRequest.Builder(LocalPlatformContext.current).data(data = imageUrl)
            .apply(block = fun ImageRequest.Builder.() {
                crossfade(true)
                scale(Scale.FILL)
            }).build()
    )

    val state by painter.state.collectAsState()
    val showStats = state is AsyncImagePainter.State.Success
    Card(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
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

            if (showStats) {
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

        StatsInfoText(stats.see, MaterialSymbol.Visibility, "counterSee")
        StatsInfoText(stats.scale, MaterialSymbol.ZoomIn, "counterScale")
        StatsInfoText(stats.save, MaterialSymbol.Save, "counterSave")
        StatsInfoText(stats.share, MaterialSymbol.Share, "counterShare")

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
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Favorite,
            contentDescription = null, // handled by click label of parent
            filled = checked
        )
    }
}

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showPlaceholder: Boolean = true,
    imageUrl: String
) {
    val request = ImageRequest.Builder(LocalPlatformContext.current)
        .data(data = imageUrl)
        .apply {
            crossfade(true)
        }
        .build()
    AsyncImage(
        model = request,
        contentDescription = imageUrl,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = if (showPlaceholder) painterResource(Res.drawable.img_placeholder) else null
    )
}

@Composable
private fun StatsInfoText(counter: Long, symbol: MaterialSymbol, desc: String) {

    if (counter <= 0) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        MaterialSymbolIcon(
            symbol = symbol,
            contentDescription = desc,
            size = 20.dp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = counter.toString(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
