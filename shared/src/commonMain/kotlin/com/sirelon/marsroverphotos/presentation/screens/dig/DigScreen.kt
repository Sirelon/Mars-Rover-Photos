package com.sirelon.marsroverphotos.presentation.screens.dig

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.sirelon.marsroverphotos.data.paging.usesPageFeed
import com.sirelon.marsroverphotos.presentation.theme.AppMotion
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MarsSnackbar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.ui.photoContentDescription
import com.sirelon.marsroverphotos.presentation.viewmodels.DigUiState
import com.sirelon.marsroverphotos.presentation.viewmodels.DigViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.no_connection_title
import com.sirelon.marsroverphotos.shared.resources.retry
import com.sirelon.marsroverphotos.utils.formatDisplayDate
import com.sirelon.marsroverphotos.utils.formatThousands
import com.sirelon.marsroverphotos.utils.nasaImageLargeFallbackUrls
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

/** Share of the card width a drag must pass to count as a swipe. */
private const val SWIPE_THRESHOLD = 0.3f

/** A release this fast, in card widths per second, swipes even when the drag was short. */
private const val FLING_WIDTHS_PER_SECOND = 1.5f

/** How far, in card widths, a swiped card travels before it is gone. */
private const val FLY_OFF_WIDTHS = 1.5f

/** Rotation of the top card at a full card width of drag, in degrees. */
private const val MAX_ROTATION = 12f

/** Scale of the card under the top one while the top card rests. */
private const val UNDER_CARD_SCALE = 0.94f

/** Darkens the blurred copy behind a photo so the photo stands out from it. */
private const val BACKDROP_SCRIM_ALPHA = 0.45f

@Composable
fun DigScreen(
    modifier: Modifier = Modifier,
    viewModel: DigViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.keepNotices.collect { snackbarHostState.showSnackbar("Saved to Favorites") }
    }

    DigContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onKeep = viewModel::keep,
        onSkip = viewModel::skip,
        onUndo = viewModel::undo,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun DigContent(
    state: DigUiState,
    snackbarHostState: SnackbarHostState,
    onKeep: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = { Text(text = "Dig", style = MaterialTheme.typography.headlineMedium) },
                subtitle = {
                    Text(
                        "Raw photos from every mission. Keep the good ones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        snackbarHost = { MarsSnackbar(snackbarHostState = snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding).fillMaxSize()
        when {
            state.cards.isNotEmpty() -> DigDeck(
                cards = state.cards,
                canUndo = state.canUndo,
                onKeep = onKeep,
                onSkip = onSkip,
                onUndo = onUndo,
                modifier = contentModifier,
            )

            state.loadFailed -> AppEmptyState(
                title = stringResource(Res.string.no_connection_title),
                modifier = contentModifier,
                action = { AppButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) } },
            )

            else -> Box(contentModifier) { CenteredProgress() }
        }
    }
}

/**
 * The top card and the ones under it, with the Skip / Undo / Keep row below. Only the card
 * right under the top one shows; the rest sit behind it at the same scale so their photos load. The top card follows
 * a horizontal drag and tilts with it; past [SWIPE_THRESHOLD] (or on a quick fling) it flies off
 * and the swipe is reported, otherwise it springs back. The buttons and the arrow keys fly it off
 * the same way, so every path looks alike. Undo flies the card back in from the side it left.
 */
@Composable
private fun DigDeck(
    cards: List<DigUiState.Card>,
    canUndo: Boolean,
    onKeep: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val top = cards.first()
    // The last card that flew off and where it stopped; Undo flies it back in from there.
    var lastSwipe by remember { mutableStateOf<DeckSwipe?>(null) }
    var returning by remember { mutableStateOf<DeckSwipe?>(null) }
    val offset = remember(top.photo.id) {
        Animatable(returning?.takeIf { it.photoId == top.photo.id }?.offset ?: 0f)
    }
    var flying by remember(top.photo.id) { mutableStateOf(false) }
    var deckWidth by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(top.photo.id) {
        if (returning?.photoId != top.photo.id) return@LaunchedEffect
        returning = null
        offset.animateTo(0f, tween(AppMotion.CardSwipeMs, easing = AppMotion.Emphasized))
    }

    fun swipe(keep: Boolean) {
        if (flying || deckWidth == 0f) return
        flying = true
        scope.launch {
            val target = deckWidth * FLY_OFF_WIDTHS * if (keep) 1f else -1f
            offset.animateTo(target, tween(AppMotion.CardSwipeMs, easing = AppMotion.Emphasized))
            lastSwipe = DeckSwipe(top.photo.id, target)
            if (keep) onKeep() else onSkip()
        }
    }

    fun undo() {
        // A card still flying off has not been reported yet; undoing now would restore the swipe
        // before it, and then the pending Keep or Skip would land on the restored card.
        if (flying) return
        returning = lastSwipe
        lastSwipe = null
        onUndo()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val expanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> { swipe(keep = true); true }
                    Key.DirectionLeft -> { swipe(keep = false); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .then(if (expanded) Modifier.widthIn(max = AppSize.contentMaxWidth) else Modifier.fillMaxWidth())
                .fillMaxSize()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { deckWidth = it.width.toFloat() },
            ) {
                // Drawn under-card first. Keyed by photo, so the card under the top one keeps its
                // loaded image when it moves up.
                cards.asReversed().forEach { card ->
                    key(card.photo.id) {
                        val isTop = card.photo.id == top.photo.id
                        DigCard(
                            card = card,
                            dragProgress = {
                                if (isTop) offset.value / (deckWidth * SWIPE_THRESHOLD).coerceAtLeast(1f) else 0f
                            },
                            modifier = if (isTop) {
                                Modifier
                                    .pointerInput(card.photo.id) {
                                        val velocity = VelocityTracker()
                                        val flingSpeed = size.width * FLING_WIDTHS_PER_SECOND
                                        detectHorizontalDragGestures(
                                            onDragStart = { velocity.resetTracking() },
                                            onDragEnd = {
                                                val speed = velocity.calculateVelocity().x
                                                val distance = size.width * SWIPE_THRESHOLD
                                                when {
                                                    offset.value > distance || speed > flingSpeed -> swipe(keep = true)
                                                    offset.value < -distance || speed < -flingSpeed -> swipe(keep = false)
                                                    else -> scope.launch { offset.animateTo(0f, tween(AppMotion.CardSwipeMs)) }
                                                }
                                            },
                                            onDragCancel = {
                                                scope.launch { offset.animateTo(0f, tween(AppMotion.CardSwipeMs)) }
                                            },
                                        ) { change, dragAmount ->
                                            if (flying) return@detectHorizontalDragGestures
                                            change.consume()
                                            velocity.addPosition(change.uptimeMillis, change.position)
                                            scope.launch { offset.snapTo(offset.value + dragAmount) }
                                        }
                                    }
                                    .graphicsLayer {
                                        translationX = offset.value
                                        rotationZ = offset.value / size.width.coerceAtLeast(1f) * MAX_ROTATION
                                    }
                            } else {
                                // Waiting under the top card: not something to read out yet.
                                val isNext = card.photo.id == cards.getOrNull(1)?.photo?.id
                                Modifier.clearAndSetSemantics {}.graphicsLayer {
                                    // Grows to full size as the top card is dragged away.
                                    val progress = if (isNext) {
                                        (abs(offset.value) / (size.width * SWIPE_THRESHOLD)).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                    val scale = UNDER_CARD_SCALE + (1f - UNDER_CARD_SCALE) * progress
                                    scaleX = scale
                                    scaleY = scale
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(AppSpacing.lg))
            DigActions(
                canUndo = canUndo,
                onKeep = { swipe(keep = true) },
                onSkip = { swipe(keep = false) },
                onUndo = ::undo,
            )
        }
    }
}

/** A card that left the deck: which photo, and the offset it flew off to. */
private data class DeckSwipe(val photoId: String, val offset: Float)

/**
 * One photo of the deck: the whole photo over a blurred copy of itself, with what it is under it.
 * [dragProgress] is the drag toward Keep (positive) or Skip (negative), 1 at the swipe threshold;
 * it fades in the matching label. Read only in the draw phase.
 */
@Composable
private fun DigCard(
    card: DigUiState.Card,
    dragProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxSize()) {
        // A photo with labels over it: a permanently dark region, see DESIGN_SYSTEM.md › Color.
        MarsRoverPhotosTheme(darkTheme = true) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                val photo = card.photo
                val chain = remember(photo.imageUrl) { nasaImageLargeFallbackUrls(photo.imageUrl) }
                val cacheKey = "dig_${photo.id}"
                // Fills the space the uncropped photo leaves; reads the photo's own bitmap as its
                // placeholder so it appears with the photo instead of after it.
                NetworkImage(
                    modifier = Modifier.fillMaxSize().blur(AppSize.photoBackdropBlur),
                    contentScale = ContentScale.Crop,
                    showPlaceholder = false,
                    imageUrl = chain.first(),
                    fallbackUrls = chain.drop(1),
                    placeholderCacheKey = cacheKey,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = BACKDROP_SCRIM_ALPHA)),
                )
                NetworkImage(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    showPlaceholder = false,
                    imageUrl = chain.first(),
                    fallbackUrls = chain.drop(1),
                    contentDescription = photo.photoContentDescription(),
                    cacheKey = cacheKey,
                )
                DragLabel(
                    text = "Keep",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    alpha = { dragProgress().coerceIn(0f, 1f) },
                    alignment = Alignment.TopStart,
                )
                DragLabel(
                    text = "Skip",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    alpha = { (-dragProgress()).coerceIn(0f, 1f) },
                    alignment = Alignment.TopEnd,
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(
                text = card.missionName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.details(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "MAST RIGHT · Sol 1,234 · May 26, 2016"; each part only when the photo has it. */
private fun DigUiState.Card.details(): String {
    val camera = (photo.camera?.fullName?.takeIf { it.isNotBlank() } ?: photo.camera?.name)
        ?.replace('_', ' ')
    // Spirit and Opportunity come from images.nasa.gov, which has no sol (stored as 0). A sol-keyed
    // mission shows sol 0 — Viking's first photo from the surface was taken on it.
    val sol = if (photo.sol != 0L || !photo.roverId.usesPageFeed()) "Sol ${formatThousands(photo.sol)}" else null
    val date = photo.earthDate.takeIf { it.isNotBlank() }?.let(::formatDisplayDate)
    return listOfNotNull(camera, sol, date).joinToString(" · ")
}

@Composable
private fun BoxScope.DragLabel(
    text: String,
    containerColor: Color,
    contentColor: Color,
    alpha: () -> Float,
    alignment: Alignment,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = contentColor,
        modifier = Modifier
            .align(alignment)
            // The buttons carry Keep and Skip for screen readers; the label only teaches the drag.
            .clearAndSetSemantics {}
            .padding(AppSpacing.lg)
            .graphicsLayer { this.alpha = alpha() }
            .background(containerColor, CircleShape)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
    )
}

@Composable
private fun DigActions(
    canUndo: Boolean,
    onKeep: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppOutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
            MaterialSymbolIcon(symbol = MaterialSymbol.Close, contentDescription = null, size = AppSize.icon)
            Spacer(Modifier.width(AppSpacing.sm))
            Text("Skip")
        }
        IconButton(onClick = onUndo, enabled = canUndo) {
            MaterialSymbolIcon(symbol = MaterialSymbol.Undo, contentDescription = "Undo")
        }
        AppButton(onClick = onKeep, modifier = Modifier.weight(1f)) {
            MaterialSymbolIcon(
                symbol = MaterialSymbol.Favorite,
                contentDescription = null,
                size = AppSize.icon,
                filled = true,
            )
            Spacer(Modifier.width(AppSpacing.sm))
            Text("Keep")
        }
    }
}
