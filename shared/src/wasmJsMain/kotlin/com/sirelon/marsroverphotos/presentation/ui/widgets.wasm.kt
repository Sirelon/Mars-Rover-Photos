package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

/**
 * Web implementation: Pass-through since web doesn't have native overscroll in Compose.
 */
@Composable
actual fun NoScrollEffect(content: @Composable () -> Unit) {
    content()
}
