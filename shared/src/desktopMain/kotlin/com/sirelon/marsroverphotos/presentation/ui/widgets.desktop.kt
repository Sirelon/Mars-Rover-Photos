package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable

/**
 * Desktop implementation: Pass-through since desktop doesn't have overscroll.
 */
@Composable
actual fun NoScrollEffect(content: @Composable () -> Unit) {
    content()
}
