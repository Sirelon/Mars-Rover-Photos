package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INGENUITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.img_placeholder
import com.sirelon.marsroverphotos.utils.formatThousands
import com.sirelon.marsroverphotos.utils.nasaImageChainWorkingFirst
import com.sirelon.marsroverphotos.utils.nasaImageVariantLoaded
import org.jetbrains.compose.resources.painterResource

/**
 * Screen-reader description for a photo: "Rover, Camera, sol N" (e.g. "Perseverance, Mastcam-Z,
 * sol 1,234"), used as `NetworkImage`'s `contentDescription` so TalkBack/VoiceOver read a photo's
 * context instead of its raw URL.
 *
 * Parts are omitted rather than printed empty — Spirit/Opportunity come from the NASA Image
 * Library with no sol or camera (see `Mappers.kt#toMarsImages`), so a missing camera is skipped
 * and a missing sol falls back to the earth date. `sol == 0` is treated as "missing" (it is how
 * those NASA Image Library rows spell "no sol"); a genuine sol-0 landing-day photo elsewhere
 * would print its earth date instead of "sol 0", which still reads sensibly. If nothing
 * structured resolves at all (unrecognized rover id, no sol, no earth date), the photo's title or
 * description stands in, and "Mars photo" is the last resort — never the URL.
 */
fun MarsImage.photoContentDescription(): String {
    val parts = mutableListOf<String>()
    roverDisplayName(roverId)?.let { parts += it }
    val cameraName = camera?.fullName?.takeIf { it.isNotBlank() } ?: camera?.name?.takeIf { it.isNotBlank() }
    if (cameraName != null) parts += cameraName
    when {
        sol > 0 -> parts += "sol ${formatThousands(sol)}"
        earthDate.isNotBlank() -> parts += earthDate
    }
    if (parts.isNotEmpty()) return parts.joinToString(", ")
    return name?.takeIf { it.isNotBlank() }
        ?: description?.takeIf { it.isNotBlank() }
        ?: "Mars photo"
}

/**
 * Rover id → display name, matching the names `RoversRepositoryImpl` seeds onto `Rover.name`.
 * Duplicated here rather than reused because `presentation` may only depend on `domain` (see
 * docs/ARCHITECTURE.md) and `RoversRepositoryImpl` lives in `data`; follows the same id `when`
 * dispatch as `Rover.drawableResource()` in `RoverPainter.kt`. Returns null for an unrecognized
 * id so [photoContentDescription] can fall further back instead of printing a placeholder name.
 */
private fun roverDisplayName(roverId: Long): String? = when (roverId) {
    PERSEVERANCE_ID -> "Perseverance"
    INSIGHT_ID -> "Insight"
    CURIOSITY_ID -> "Curiosity"
    OPPORTUNITY_ID -> "Opportunity"
    SPIRIT_ID -> "Spirit"
    VIKING_1_ID -> "Viking 1"
    VIKING_2_ID -> "Viking 2"
    INGENUITY_ID -> "Ingenuity Helicopter"
    else -> null
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
    // Screen-reader label, e.g. MarsImage.photoContentDescription(). Null for a purely decorative
    // load — a placeholder layer behind the real image, or a caller whose parent already carries
    // a merged accessibility label — so TalkBack/VoiceOver don't announce the same photo twice.
    contentDescription: String? = null,
    // Additional NASA Image Library variant URLs to try, in order, if `imageUrl` fails to load
    // (e.g. nasaImageLargeFallbackUrls(...).drop(1)) — NASA doesn't generate every ~size variant
    // for every asset, and a missing one 403s rather than 404s. Each URL is attempted once; when
    // the chain is exhausted the load shows Coil's normal error state, un-retried. Empty for
    // non-NASA-Image-Library images, which never had a derived variant to begin with.
    fallbackUrls: List<String> = emptyList(),
    // Writer: stores this load under a stable shared key (e.g. "photo_<id>") so a sibling screen
    // can read it as an instant placeholder. Reader: shows the bitmap cached under this key while
    // the (possibly higher-res, different-URL) image loads. Kept separate so the grid writes and the
    // viewer reads without colliding on the same key — see docs/DESIGN_SYSTEM.md › Motion.
    cacheKey: String? = null,
    placeholderCacheKey: String? = null,
) {
    val context = LocalPlatformContext.current
    // The chain — and which rung we're on — resets whenever the requested image or its fallback
    // chain changes (a different photo, or the fullscreen viewer swapping ~large for ~orig on
    // zoom), so each image walks its own chain once rather than carrying over a prior failure.
    // A variant that already loaded for this photo goes first, so a revisit skips the 403 rungs.
    val chain = remember(imageUrl, fallbackUrls) { nasaImageChainWorkingFirst(listOf(imageUrl) + fallbackUrls) }
    var attempt by remember(imageUrl, fallbackUrls) { mutableStateOf(0) }
    val currentUrl = chain[attempt.coerceIn(0, chain.lastIndex)]
    val advanceToNextVariant = { if (attempt < chain.lastIndex) attempt += 1 }
    val rememberLoadedVariant = { if (chain.size > 1) nasaImageVariantLoaded(imageUrl, currentUrl) }

    // cacheKey/placeholderCacheKey stay fixed across attempts regardless of currentUrl, so a
    // bitmap that only loaded via a fallback variant is still stored under the same shared key
    // the grid/viewer placeholders read.
    val request = remember(currentUrl, cacheKey, placeholderCacheKey) {
        ImageRequest.Builder(context)
            .data(data = currentUrl)
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
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = painterResource(Res.drawable.img_placeholder),
            onSuccess = { rememberLoadedVariant() },
            onError = { advanceToNextVariant() },
        )
    } else if (placeholderCacheKey != null) {
        // Memory-cached placeholder (e.g. grid thumbnail) shows instantly; crossfades to full-res.
        // AsyncImage lets Coil handle the placeholder — SubcomposeAsyncImage's loading slot would
        // override it and show the spinner instead.
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            onSuccess = { rememberLoadedVariant() },
            onError = { advanceToNextVariant() },
        )
    } else {
        var isLoading by remember(imageUrl) { mutableStateOf(true) }
        Box(modifier = modifier) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onSuccess = {
                    isLoading = false
                    rememberLoadedVariant()
                },
                onError = {
                    if (attempt < chain.lastIndex) advanceToNextVariant() else isLoading = false
                },
            )
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
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
