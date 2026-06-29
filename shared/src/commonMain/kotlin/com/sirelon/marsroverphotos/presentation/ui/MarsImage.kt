package com.sirelon.marsroverphotos.presentation.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
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
    var showStats by remember(imageUrl) { mutableStateOf(false) }

    AppCard(
        modifier = modifier
            .padding(vertical = AppSpacing.sm)
            .fillMaxWidth()
            .testTag("photoItem")
            .clickable(onClick = onClick),
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    // Writer: same shared key the fullscreen viewer reads as its instant placeholder.
                    .memoryCacheKey("photo_${marsImage.id}")
                    .build(),
                contentDescription = imageUrl,
                modifier = Modifier
                    .defaultMinSize(minHeight = 100.dp)
                    .fillMaxWidth()
                    // Shared-element source for Favorite/Popular → fullscreen viewer.
                    .sharedPhoto(marsImage.id),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                placeholder = painterResource(Res.drawable.img_placeholder),
                onSuccess = { showStats = true },
            )

            if (showStats) {
                PhotoStats(
                    marsImage = marsImage,
                    onFavoriteClick = {
                        if (!marsImage.favorite) {
                            // heartState would be triggered by caller if needed
                        }
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
    // Writer: stores this load under a stable shared key (e.g. "photo_<id>") so a sibling screen
    // can read it as an instant placeholder. Reader: shows the bitmap cached under this key while
    // the (possibly higher-res, different-URL) image loads. Kept separate so the grid writes and the
    // viewer reads without colliding on the same key — see docs/DESIGN_SYSTEM.md › Motion.
    cacheKey: String? = null,
    placeholderCacheKey: String? = null,
) {
    val context = LocalPlatformContext.current
    val request = remember(imageUrl, cacheKey, placeholderCacheKey) {
        ImageRequest.Builder(context)
            .data(data = imageUrl)
            .apply {
                crossfade(true)
                if (cacheKey != null) memoryCacheKey(cacheKey)
                if (placeholderCacheKey != null) placeholderMemoryCacheKey(MemoryCache.Key(placeholderCacheKey))
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
    } else if (placeholderCacheKey != null) {
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
        var isLoading by remember(imageUrl) { mutableStateOf(true) }
        Box(modifier = modifier) {
            AsyncImage(
                model = request,
                contentDescription = imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onSuccess = { isLoading = false },
                onError = { isLoading = false },
            )
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
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

internal fun compactCount(value: Long): String {
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
