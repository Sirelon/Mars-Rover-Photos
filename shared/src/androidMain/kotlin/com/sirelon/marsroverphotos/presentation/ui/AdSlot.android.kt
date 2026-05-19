package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AdSlot(modifier: Modifier) {
    // AdMob integration stays in androidApp; this slot is a placeholder.
    // The real ad banner is injected via the navigation scaffold in androidApp.
    Box(modifier = modifier)
}
