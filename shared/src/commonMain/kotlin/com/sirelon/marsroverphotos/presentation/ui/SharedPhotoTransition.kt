package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sirelon.marsroverphotos.presentation.navigation.LocalMissionLayoutAnimatedVisibilityScope
import com.sirelon.marsroverphotos.presentation.navigation.LocalSharedTransitionScope
import com.sirelon.marsroverphotos.presentation.theme.AppMotion

/**
 * Shared-element transition primitives for the photo grid/list ↔ fullscreen viewer.
 *
 * Single source of truth for the keying scheme (`photo_<id>` / `photo_favorite_<id>`), the resize
 * mode, the bounds spec, and overlay clipping — so the grid item and the viewer page stay in sync
 * and there's no duplicated `with(scope) { … }` boilerplate at each call site.
 *
 * Both read [LocalSharedTransitionScope] and the Nav3 [LocalNavAnimatedContentScope] null-safely:
 * outside a `SharedTransitionLayout` + `NavDisplay` (previews, tests) they no-op instead of
 * crashing. [LocalNavAnimatedContentScope] is only read after the shared-scope null check, so a
 * preview that never provides the shared scope never touches the error-defaulted Nav3 local.
 */

/**
 * Photo container transform between a grid/list thumbnail and the fullscreen pager page.
 *
 * Uses `sharedBounds` (not `sharedElement`) with `scaleToBounds(Fit)`: the two ends are different
 * renderings of the same photo (square-crop thumbnail vs letterboxed fullscreen), so a crossfade is
 * the right model rather than morphing one frame into the other.
 *
 * @param enabled lets the pager apply the transition only to its settled page.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPhoto(id: String, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "photo_$id"),
            animatedVisibilityScope = animatedScope,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(contentScale = ContentScale.Fit),
            boundsTransform = AppMotion.PhotoBoundsTransform,
            // Keep the card's rounded corners during the overlay flight. Constant 16 dp is
            // near-invisible against the viewer's black background; revisit with a progress-
            // interpolated radius only if it reads wrong on device.
            clipInOverlayDuringTransition = OverlayClip(MaterialTheme.shapes.large),
        )
    }
}

/**
 * Favorite heart shared element. Flies between the grid corner and the viewer corner where both
 * ends exist (the rover Photos flow). On screens without a corner heart (Favorite/Popular) there's
 * no source, so the viewer heart should fade in separately rather than relying on this.
 *
 * @param enabled lets the pager apply the transition only to its settled page.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedFavorite(id: String, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(shared) {
        sharedElement(
            sharedContentState = rememberSharedContentState(key = "photo_favorite_$id"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = AppMotion.FavoriteBoundsTransform,
        )
    }
}

/**
 * Shared bounds for the rover hero/portrait image between the Mission Info compact and expanded
 * layouts. Uses `sharedBounds` because the two ends have different aspect ratios (16:9 vs 4:3).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedRoverImage(roverId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalMissionLayoutAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "mission_rover_image_$roverId"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = AppMotion.PhotoBoundsTransform,
        )
    }
}

/**
 * Shared bounds for the rover name text between the Mission Info compact (hero overlay) and
 * expanded (identity card) layouts. Uses `sharedBounds` because the text styles differ.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedRoverName(roverId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalMissionLayoutAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "mission_rover_name_$roverId"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = AppMotion.TextBoundsTransform,
        )
    }
}

/** Shared bounds for the rover status badge (flies with the name between hero and identity card). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedRoverBadge(roverId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalMissionLayoutAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "mission_rover_badge_$roverId"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = AppMotion.TextBoundsTransform,
        )
    }
}

/** Shared bounds for the fun-fact card (same composable in both layouts, morphs to new position). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedRoverFunFact(roverId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalMissionLayoutAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "mission_rover_funfact_$roverId"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = AppMotion.PhotoBoundsTransform,
        )
    }
}

/** Shared bounds for the objectives section (exact same composable in both layouts). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedRoverObjectives(roverId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalMissionLayoutAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "mission_rover_objectives_$roverId"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = AppMotion.PhotoBoundsTransform,
        )
    }
}

/** Shared bounds for the cameras grid (exact same composable in both layouts). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedRoverCameras(roverId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalMissionLayoutAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "mission_rover_cameras_$roverId"),
            animatedVisibilityScope = animatedScope,
            boundsTransform = AppMotion.PhotoBoundsTransform,
        )
    }
}

/**
 * Fades the element in/out with the Nav3 transition. Fallback entrance for the fullscreen viewer's
 * favorite heart on screens that have no shared source for it (Favorite/Popular/deep-link) — without
 * it the heart would pop in under the viewer's `EnterTransition.None`. No-op outside a
 * SharedTransitionLayout + NavDisplay (previews/tests).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.navFadeEnter(): Modifier {
    LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(animatedScope) {
        this@navFadeEnter.animateEnterExit(
            enter = fadeIn(tween(AppMotion.SharedContainerMs, easing = AppMotion.Emphasized)),
            // No own exit: the heart fades out with the parent screen's container fade on pop, so it
            // doesn't double-fade (alpha²) faster than the surrounding UI.
            exit = ExitTransition.None,
        )
    }
}
