package com.sirelon.marsroverphotos.presentation.ui

import androidx.core.view.WindowInsetsControllerCompat
import com.sirelon.marsroverphotos.platform.ActivityProvider

/**
 * Android implementation — flips `isAppearanceLightStatusBars` on the current
 * Activity window via [WindowInsetsControllerCompat].
 *
 * Requires that [ActivityProvider] holds a valid Activity reference (registered in
 * `MainActivity.onResume`).  If no Activity is available the call is silently ignored.
 */
actual fun setStatusBarAppearance(lightIcons: Boolean) {
    val activity = ActivityProvider.current() ?: return
    val window = activity.window ?: return
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    // isAppearanceLightStatusBars = true  → dark icons (for light backgrounds)
    // isAppearanceLightStatusBars = false → light/white icons (for dark backgrounds)
    controller.isAppearanceLightStatusBars = !lightIcons
}
