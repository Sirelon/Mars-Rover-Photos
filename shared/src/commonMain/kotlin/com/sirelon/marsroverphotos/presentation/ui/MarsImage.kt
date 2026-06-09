package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import androidx.compose.material3.CircularProgressIndicator
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.memory.MemoryCache
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

    val heartState = rememberLikeHeartState()

    val state by painter.state.collectAsState()
    val showStats = state is AsyncImagePainter.State.Success
    AppCard(
        modifier = modifier
            .padding(vertical = AppSpacing.sm)
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 100.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                    painter = painter,
                    alignment = Alignment.TopCenter,
                    contentDescription = imageUrl
                )
                LikeHeartOverlay(
                    visible = heartState.visible,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (showStats) {
                PhotoStats(
                    marsImage = marsImage,
                    onFavoriteClick = {
                        if (!marsImage.favorite) heartState.trigger()
                        onFavoriteClick()
                    },
                )
            }
        }
    }
}

@Composable
fun PhotoStats(
    marsImage: MarsImage,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = marsImage.stats

    Column(
        modifier = modifier
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatsInfoText(stats.see, MaterialSymbol.Visibility, "Views")
            StatsInfoText(stats.scale, MaterialSymbol.ZoomIn, "Zooms")
            StatsInfoText(stats.save, MaterialSymbol.Save, "Saves")
            StatsInfoText(stats.share, MaterialSymbol.Share, "Shares")
        }
        LikeAction(
            count = stats.favorite,
            checked = marsImage.favorite,
            onClick = onFavoriteClick,
        )
    }
}

@Composable
private fun LikeAction(
    count: Long,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Favorite,
            contentDescription = if (checked) "Unlike" else "Like",
            filled = checked,
            tint = tint,
            size = 22.dp,
        )
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        Text(
            text = if (count > 0) "Like · ${compactCount(count)}" else "Like",
            style = MaterialTheme.typography.labelLarge,
            color = tint,
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
            contentDescription = "Favorites",
            filled = true,
            tint = if (checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showPlaceholder: Boolean = true,
    imageUrl: String,
    placeholderMemoryCacheKey: String? = null,
) {
    val context = LocalPlatformContext.current
    val request = remember(imageUrl, placeholderMemoryCacheKey) {
        ImageRequest.Builder(context)
            .data(data = imageUrl)
            .apply {
                crossfade(true)
                if (placeholderMemoryCacheKey != null) {
                    placeholderMemoryCacheKey(MemoryCache.Key(placeholderMemoryCacheKey))
                }
            }
            .build()
    }
    if (showPlaceholder) {
        AsyncImage(
            model = request,
            contentDescription = imageUrl,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = painterResource(Res.drawable.img_placeholder)
        )
    } else if (placeholderMemoryCacheKey != null) {
        // Memory-cached placeholder (e.g. grid thumbnail) shows instantly; crossfades to full-res.
        // AsyncImage lets Coil handle the placeholder — SubcomposeAsyncImage's loading slot would
        // override it and show the spinner instead.
        AsyncImage(
            model = request,
            contentDescription = imageUrl,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = imageUrl,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        )
    }
}

@Composable
private fun StatsInfoText(counter: Long, symbol: MaterialSymbol, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MaterialSymbolIcon(
            symbol = symbol,
            contentDescription = desc,
            size = 16.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = if (counter > 0) compactCount(counter) else "0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun compactCount(value: Long): String {
    val abs = if (value < 0) -value else value
    return when {
        abs < 1_000 -> value.toString()
        abs < 10_000 -> formatOneDecimal(value, 1_000) + "K"
        abs < 1_000_000 -> (value / 1_000).toString() + "K"
        abs < 10_000_000 -> formatOneDecimal(value, 1_000_000) + "M"
        else -> (value / 1_000_000).toString() + "M"
    }
}

private fun formatOneDecimal(value: Long, divisor: Int): String {
    val whole = value / divisor
    val tenths = ((value % divisor) * 10 / divisor)
    return if (tenths == 0L) whole.toString() else "$whole.$tenths"
}
