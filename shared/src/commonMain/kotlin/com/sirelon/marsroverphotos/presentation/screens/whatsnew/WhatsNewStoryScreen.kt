package com.sirelon.marsroverphotos.presentation.screens.whatsnew

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppMotion
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.presentation.theme.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppIconBox
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.setStatusBarAppearance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.materialSymbolOrDefault
import androidx.compose.runtime.snapshotFlow
import coil3.compose.AsyncImage
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewViewModel
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * One-shot latch guarding the story's single exit. Deliberately not snapshot state: it only has to
 * stop a second [AppNavigator.goBack][com.sirelon.marsroverphotos.presentation.navigation.AppNavigator.goBack]
 * in the same frame, and nothing renders from it.
 */
private class ExitLatch {
    var fired = false
}

@Composable
fun WhatsNewStoryScreen(version: String, startPage: Int) {
    val navigator = LocalAppNavigator.current
    val viewModel: WhatsNewViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Keyed on the loaded list, not on the ViewModel: the notes arrive from Firestore after the
    // first composition, and a key that never changes would freeze this at the pre-load null and
    // leave the story permanently blank.
    val release = remember(state.releases, version) {
        state.releases.firstOrNull { it.version == version }
    }
    if (release == null) {
        // Still fetching, or the notes carry nothing for this version — which a restored back-stack
        // entry or an unpublished release can both produce, so it has to lead somewhere rather than
        // sit on an empty screen.
        if (state.isLoading) {
            CenteredProgress()
        } else {
            LaunchedEffect(version) { navigator.goBack() }
        }
        return
    }
    val changes = release.changes
    // A release with no changes has no story: the pager would report 0 pages and every page-indexed
    // read below would be out of bounds.
    if (changes.isEmpty()) return

    val scope = rememberCoroutineScope()
    // startPage arrives from a serialized back-stack key, so it can outlive the release it was saved
    // against (a shorter changes list after an update). PagerState does not clamp initialPage until
    // the layout phase, so it has to be clamped here, before anything indexes with it.
    val firstPage = startPage.coerceIn(0, changes.lastIndex)
    val pagerState = rememberPagerState(initialPage = firstPage) { changes.size }
    // Absolute position across the whole story: page index + that page's 0→1 fill. One value rather
    // than an (index, fraction) pair, so the bar can never draw a fresh segment with the previous
    // page's leftover fill for a frame before the reset lands.
    val storyProgress = remember { Animatable(firstPage.toFloat()) }
    val screenEnter = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var dismissOffset by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }

    // The story paints a forced-dark background under the status bar, so the system icons have to
    // be light for the whole time it is up — otherwise a device on a light system theme draws dark
    // icons on black. Same handling as the fullscreen photo viewer.
    DisposableEffect(Unit) {
        setStatusBarAppearance(lightIcons = true)
        onDispose { setStatusBarAppearance(lightIcons = false) }
    }

    // The auto-advance timer, the forward tap zone and the "Done" button can all decide to leave on
    // the last page, and a tap landing in the same frame the timer fires would otherwise pop two
    // entries — silently dropping the Version History screen behind this one.
    val exitLatch = remember { ExitLatch() }
    fun exit() {
        if (exitLatch.fired) return
        exitLatch.fired = true
        navigator.goBack()
    }

    LaunchedEffect(Unit) {
        screenEnter.animateTo(
            targetValue = 1f,
            animationSpec = tween(AppMotion.StoryEnterMs, easing = AppMotion.Emphasized),
        )
    }

    // Auto-advance: fill the target page's segment over AppMotion.StoryPageMs, then move on.
    // snapshotFlow emits whenever targetPage changes; collectLatest cancels the in-progress block the
    // moment the pager commits to a new page — same cancel-on-advance behaviour as an
    // LaunchedEffect(targetPage) key, but without invalidating WhatsNewStoryScreen's restart scope.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage to isPaused }.collectLatest { (page, paused) ->
            if (paused) {
                awaitCancellation()
            }
            val position = storyProgress.value
            when {
                // Skipping ahead — run the segments we're leaving out to full instead of snapping them.
                position < page -> storyProgress.animateTo(
                    targetValue = page.toFloat(),
                    animationSpec = tween(AppMotion.StoryCatchUpMs, easing = AppMotion.Emphasized),
                )
                // Going back — rewind straight to the start of the target segment.
                position >= page + 1f -> storyProgress.snapTo(page.toFloat())
                // Already inside this segment: targetPage predicted a move mid-drag and the drag came
                // back. Nothing was left, so the fill keeps running instead of visibly resetting to 0%.
                else -> Unit
            }
            // Resume from wherever the segment stands, at the same fill rate, so a reverted drag
            // doesn't hand the page a fresh full-length timer.
            val remaining = (page + 1f - storyProgress.value).coerceIn(0f, 1f)
            storyProgress.animateTo(
                targetValue = page + 1f,
                animationSpec = tween(
                    durationMillis = (AppMotion.StoryPageMs * remaining).toInt(),
                    easing = LinearEasing,
                ),
            )
            // Only auto-advance if the fill ran to completion (wasn't cut short by a tap or swipe).
            if (storyProgress.value >= page + 1f) {
                val next = page + 1
                if (next < changes.size) scope.launch { pagerState.animateScrollToPage(next) }
                else exit()
            }
        }
    }

    // Forced dark, but through the app's theme entry point so the story still picks up dynamic
    // color and the brand overrides every other surface gets. See docs/DESIGN_SYSTEM.md › Color.
    MarsRoverPhotosTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dismissOffset
                    val progress = (dismissOffset / dismissThresholdPx).coerceIn(0f, 1f)
                    alpha = 1f - progress * 0.4f
                }
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    var springJob: Job? = null
                    detectVerticalDragGestures(
                        onDragStart = { springJob?.cancel() },
                        onVerticalDrag = { _, delta ->
                            dismissOffset = (dismissOffset + delta).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dismissOffset > dismissThresholdPx) {
                                exit()
                            } else if (dismissOffset > 0f) {
                                val startOffset = dismissOffset
                                springJob = scope.launch {
                                    animate(
                                        initialValue = startOffset,
                                        targetValue = 0f,
                                        animationSpec = spring(),
                                    ) { value, _ -> dismissOffset = value }
                                }
                            }
                        },
                        onDragCancel = {
                            if (dismissOffset > 0f) {
                                val startOffset = dismissOffset
                                springJob = scope.launch {
                                    animate(
                                        initialValue = startOffset,
                                        targetValue = 0f,
                                        animationSpec = spring(),
                                    ) { value, _ -> dismissOffset = value }
                                }
                            }
                        },
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Read the entry animation in the layer phase only — the whole story tree would
                    // otherwise recompose on every frame of the fade.
                    .graphicsLayer {
                        val t = screenEnter.value
                        alpha = t
                        val scale = 0.94f + 0.06f * t
                        scaleX = scale
                        scaleY = scale
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                StoryProgressBar(
                    count = changes.size,
                    progress = { storyProgress.value },
                    modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                )

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.sm),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = ::exit) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    // Ghost version number — a static backdrop behind the pager, not part of any page,
                    // so it stays put while the pages swipe over it.
                    Text(
                        text = release.version,
                        style = AppTypography.storyVersionGhost,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = AppSpacing.xxl),
                    )

                    // Pager — tap zones live inside each page so they're peers of the page content.
                    // The pager only claims drag gestures; taps fall through to the page's zones.
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            StoryPage(
                                change = changes[page],
                                pageOffset = { pagerState.pageOffsetOf(page) },
                            )

                            // 25% left → back, 75% right → forward.
                            // semantics provides the accessibility node; detectTapGestures handles
                            // both pause-on-hold and tap-to-navigate in one gesture handler.
                            // onLongPress={} arms the platform long-press timer (~500ms) so holds
                            // longer than that don't fire onTap — the user gets a clean pause with
                            // no accidental navigation on release.
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.25f)
                                        .semantics {
                                            if (page > 0) {
                                                onClick(label = "Previous") {
                                                    scope.launch { pagerState.animateScrollToPage(page - 1) }
                                                    true
                                                }
                                            }
                                        }
                                        .pointerInput(page) {
                                            if (page > 0) {
                                                detectTapGestures(
                                                    onPress = { _ ->
                                                        try {
                                                            isPaused = true
                                                            tryAwaitRelease()
                                                        } finally {
                                                            isPaused = false
                                                        }
                                                    },
                                                    onLongPress = {},
                                                    onTap = {
                                                        scope.launch { pagerState.animateScrollToPage(page - 1) }
                                                    },
                                                )
                                            }
                                        },
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.75f)
                                        .semantics {
                                            onClick(label = if (page < changes.lastIndex) "Next" else "Close") {
                                                val target = page + 1
                                                if (target < changes.size) scope.launch { pagerState.animateScrollToPage(target) }
                                                else exit()
                                                true
                                            }
                                        }
                                        .pointerInput(page) {
                                            detectTapGestures(
                                                onPress = { _ ->
                                                    try {
                                                        isPaused = true
                                                        tryAwaitRelease()
                                                    } finally {
                                                        isPaused = false
                                                    }
                                                },
                                                onLongPress = {},
                                                onTap = {
                                                    val target = page + 1
                                                    if (target < changes.size) {
                                                        scope.launch { pagerState.animateScrollToPage(target) }
                                                    } else {
                                                        exit()
                                                    }
                                                },
                                            )
                                        },
                                )
                            }
                        }
                    }
                }

                // Primary action button — reads currentPage inside the content lambda so that only
                // the button subtree recomposes on page changes, not WhatsNewStoryScreen itself.
                AppButton(
                    onClick = {
                        val page = pagerState.currentPage
                        if (page >= changes.lastIndex) {
                            exit()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(page + 1) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.xl)
                        .padding(bottom = AppSpacing.xl),
                ) {
                    // Compared rather than indexed: currentPage is the raw pager position, only
                    // corrected to the real page range in the layout phase.
                    Text(text = if (pagerState.currentPage >= changes.lastIndex) "Done" else "Next")
                }
            }
        }
    }
}

/** Signed distance of [page] from the settled position: 0 when centered, ±1 one page away. */
private fun PagerState.pageOffsetOf(page: Int): Float =
    (currentPage - page) + currentPageOffsetFraction

/**
 * Segmented story progress, drawn in a single [Canvas].
 *
 * [progress] is an absolute position over all [count] segments (2.4f = segment 2 filled 40%), and a
 * lambda on purpose: reading the running [Animatable] here instead of in the caller's composition
 * keeps the 10s fill in the draw phase, so a frame costs one repaint of this thin strip rather than a
 * recomposition + relayout of the pager and its page.
 */
@Composable
private fun StoryProgressBar(
    count: Int,
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val gap = AppSpacing.xs
    // Capture colors in composition — Canvas draw lambda runs off the composition tree.
    val segmentFilled = MaterialTheme.colorScheme.onBackground
    val segmentEmpty = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(AppSize.progressTrack),
    ) {
        val gapPx = gap.toPx()
        val segmentWidth = ((size.width - gapPx * (count - 1)) / count).coerceAtLeast(0f)
        val radius = CornerRadius(size.height / 2f)
        val position = progress()
        repeat(count) { i ->
            val left = i * (segmentWidth + gapPx)
            drawRoundRect(
                color = segmentEmpty,
                topLeft = Offset(left, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = radius,
            )
            val fill = (position - i).coerceIn(0f, 1f)
            if (fill > 0f) {
                drawRoundRect(
                    color = segmentFilled,
                    topLeft = Offset(left, 0f),
                    size = Size(segmentWidth * fill, size.height),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/**
 * Depth-staggered appearance: content slides and fades against the page as it swipes past, so the
 * icon, title and detail arrive at slightly different speeds. [offset] is read inside the layer
 * block, which keeps the whole effect off the recomposition path.
 *
 * @param depth multiplier on the [AppSpacing.x3l] parallax travel — a larger magnitude means the
 *   element lags further behind the page.
 * @param fadeSpeed how fast the element fades out as the page leaves; >1 fades before the page has
 *   fully scrolled away.
 */
private fun Modifier.storyParallax(
    offset: () -> Float,
    depth: Float,
    fadeSpeed: Float = 1.6f,
) = graphicsLayer {
    val o = offset().coerceIn(-1f, 1f)
    val visibility = (1f - abs(o) * fadeSpeed).coerceIn(0f, 1f)
    alpha = visibility
    translationX = o * AppSpacing.x3l.toPx() * depth
    val scale = 0.88f + 0.12f * visibility
    scaleX = scale
    scaleY = scale
}

@Composable
private fun StoryPage(
    change: Release.Change,
    pageOffset: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.xl)
                .padding(vertical = AppSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (change.imageUrl != null) {
                AsyncImage(
                    model = change.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSize.storyImage)
                        .clip(MaterialTheme.shapes.large)
                        .storyParallax(pageOffset, depth = -0.5f),
                )
                Spacer(modifier = Modifier.height(AppSpacing.xl))
            }
            AppIconBox(
                symbol = materialSymbolOrDefault(change.icon),
                container = colors.onBackground.copy(alpha = 0.15f),
                tint = colors.onBackground,
                modifier = Modifier.storyParallax(pageOffset, depth = -0.65f),
            )
            Spacer(modifier = Modifier.height(AppSpacing.xl))
            Text(
                text = change.title,
                style = AppTypography.storyTitle,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.storyParallax(pageOffset, depth = -1.35f),
            )
            if (change.detail != null) {
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                Text(
                    text = change.detail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.storyParallax(pageOffset, depth = -2f, fadeSpeed = 2f),
                )
            }
        }
    }
}
