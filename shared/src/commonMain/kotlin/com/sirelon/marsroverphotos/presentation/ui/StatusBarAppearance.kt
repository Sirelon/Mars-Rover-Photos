package com.sirelon.marsroverphotos.presentation.ui

/**
 * Sets the status bar icon/text appearance.
 *
 * When [lightIcons] is `true`, the status bar is put into "dark content" mode so that
 * light (white) icons are shown — correct for use over a black background such as the
 * fullscreen photo pager.  When `false` the original appearance is restored.
 *
 * Platform implementations:
 * - **Android**: flips `WindowInsetsControllerCompat.isAppearanceLightStatusBars`
 * - **iOS**: sets `overrideUserInterfaceStyle` on the key window (`.dark` / `.unspecified`)
 * - **Desktop**: no-op
 */
expect fun setStatusBarAppearance(lightIcons: Boolean)
