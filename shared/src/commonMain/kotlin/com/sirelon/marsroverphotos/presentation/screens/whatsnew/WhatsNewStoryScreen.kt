package com.sirelon.marsroverphotos.presentation.screens.whatsnew

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppMotion
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.DarkColorPalette
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppIconBox
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.toIcon
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewViewModel
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val PAGE_DURATION_MS = 5_000

/** How long the current segment takes to run out its remaining fill when the user skips ahead. */
private const val CATCH_UP_MS = 250

/** One-shot fade/scale of the whole story when the screen appears. */
private const val SCREEN_ENTER_MS = 420

@Composable
fun WhatsNewStoryScreen(version: String, startPage: Int) {
    val navigator = LocalAppNavigator.current
    val viewModel: WhatsNewViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val release = state.releases.firstOrNull { it.version == version } ?: return
    val changes = release.changes
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = startPage) { changes.size }
    // Absolute position across the whole story: page index + that page's 0→1 fill. One value rather
    // than an (index, fraction) pair, so the bar can never draw a fresh segment with the previous
    // page's leftover fill for a frame before the reset lands.
    val storyProgress = remember { Animatable(startPage.toFloat()) }
    val screenEnter = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        screenEnter.animateTo(
            targetValue = 1f,
            animationSpec = tween(SCREEN_ENTER_MS, easing = AppMotion.Emphasized),
        )
    }

    // Auto-advance: fill the target page's segment over PAGE_DURATION_MS, then move on.
    // Keyed on targetPage, not settledPage: the pager commits to a page the moment the scroll starts,
    // so the next segment begins filling during the slide instead of idling until it settles.
    LaunchedEffect(pagerState.targetPage) {
        val page = pagerState.targetPage
        if (storyProgress.value < page) {
            // Skipping ahead — run the segments we're leaving out to full instead of snapping them.
            storyProgress.animateTo(
                targetValue = page.toFloat(),
                animationSpec = tween(CATCH_UP_MS, easing = AppMotion.Emphasized),
            )
        } else {
            // Going back — rewind straight to the start of the target segment.
            storyProgress.snapTo(page.toFloat())
        }
        storyProgress.animateTo(
            targetValue = page + 1f,
            animationSpec = tween(durationMillis = PAGE_DURATION_MS, easing = LinearEasing),
        )
        // Only auto-advance if the fill ran to completion (wasn't cut short by a tap or swipe).
        // The scroll runs outside this effect: advancing moves targetPage, which cancels us.
        if (storyProgress.value >= page + 1f) {
            val next = page + 1
            if (next < changes.size) scope.launch { pagerState.animateScrollToPage(next) }
            else navigator.goBack()
        }
    }

    MaterialTheme(colorScheme = DarkColorPalette) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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
                    IconButton(onClick = { navigator.goBack() }) {
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
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 96.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = AppSpacing.xxl),
                    )

                    // Pager — tap zones live inside each page so they're peers of the page content.
                    // The pager only claims drag gestures; taps fall through to the page's pointerInput.
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            StoryPage(
                                change = changes[page],
                                pageOffset = { pagerState.pageOffsetOf(page) },
                            )

                            // 25% left → back, 75% right → forward
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.25f)
                                        .pointerInput(page) {
                                            detectTapGestures {
                                                val target = page - 1
                                                if (target < 0) return@detectTapGestures
                                                scope.launch { pagerState.animateScrollToPage(target) }
                                            }
                                        },
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.75f)
                                        .pointerInput(page) {
                                            detectTapGestures {
                                                val target = page + 1
                                                if (target < changes.size) {
                                                    scope.launch { pagerState.animateScrollToPage(target) }
                                                } else {
                                                    navigator.goBack()
                                                }
                                            }
                                        },
                                )
                            }
                        }
                    }
                }

                // Primary action button
                val currentPage = pagerState.currentPage
                val isLast = currentPage == changes.size - 1
                val change = changes[currentPage]
                AppButton(
                    onClick = {
                        if (isLast) {
                            navigator.goBack()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.xl)
                        .padding(bottom = AppSpacing.xl),
                ) {
                    Text(text = if (isLast) "Done" else (change.actionLabel ?: "Next"))
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
 * keeps the 5s fill in the draw phase, so a frame costs one repaint of this thin strip rather than a
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
private fun StoryPage(change: Release.Change, pageOffset: () -> Float) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.xl)
                .padding(vertical = AppSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIconBox(
                symbol = change.type.toIcon(),
                container = colors.onBackground.copy(alpha = 0.15f),
                tint = colors.onBackground,
                modifier = Modifier.storyParallax(pageOffset, depth = -0.65f),
            )
            Spacer(modifier = Modifier.height(AppSpacing.xl))
            Text(
                text = change.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                ),
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
