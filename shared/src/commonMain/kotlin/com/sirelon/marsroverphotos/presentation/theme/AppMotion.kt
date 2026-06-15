package com.sirelon.marsroverphotos.presentation.theme

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/**
 * Centralized motion tokens (durations + easing) for the app's transitions.
 *
 * Why this exists: the cross-screen container fade and the shared-element bounds morph must share
 * one timing curve, otherwise the fade (a tween) and the bounds (a default spring) desync and the
 * photo "open" looks disconnected. Keeping the durations here also lets the Photos↔viewer open
 * (declared on the Images nav entry) and pop (declared on the NavDisplay) stay in lockstep.
 *
 * See docs/DESIGN_SYSTEM.md › Motion.
 */
object AppMotion {

    /** Standard screen-to-screen (slide) navigation — snappier than the previous 600 ms. */
    const val ScreenEnterMs = 400

    /** Fade applied to the outgoing screen on a standard slide nav. */
    const val ScreenExitFadeMs = 250

    /**
     * Shared-element container transform (photo grid/list ↔ fullscreen viewer). The NavDisplay
     * transition window (the container fade) and the shared-bounds spec below share this single
     * duration so the element morph and the cross-screen fade move together.
     */
    const val SharedContainerMs = 400

    /** Material 3 "emphasized" easing (reused from the segmented-control indicator). */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Bounds spec for the photo container transform — matches [SharedContainerMs] + [Emphasized]. */
    @OptIn(ExperimentalSharedTransitionApi::class)
    val PhotoBoundsTransform = BoundsTransform { _, _ -> tween(SharedContainerMs, easing = Emphasized) }

    /** Bounds spec for the favorite heart shared element — same curve as the photo. */
    @OptIn(ExperimentalSharedTransitionApi::class)
    val FavoriteBoundsTransform = BoundsTransform { _, _ -> tween(SharedContainerMs, easing = Emphasized) }
}
