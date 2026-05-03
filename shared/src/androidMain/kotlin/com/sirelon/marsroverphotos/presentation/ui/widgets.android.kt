package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

/**
 * Android implementation: Pass-through for now.
 * LocalOverscrollFactory is not available in Compose Multiplatform for Android.
 */
@Composable
actual fun NoScrollEffect(content: @Composable () -> Unit) {
    // TODO: Implement overscroll disabling when available in Compose Multiplatform
    content()
}
