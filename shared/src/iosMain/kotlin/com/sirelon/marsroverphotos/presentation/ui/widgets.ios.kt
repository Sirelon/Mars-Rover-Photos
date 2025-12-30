package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

/**
 * iOS implementation: Pass-through since iOS handles overscroll natively.
 */
@Composable
actual fun NoScrollEffect(content: @Composable () -> Unit) {
    content()
}
