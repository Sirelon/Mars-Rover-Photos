package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class LikeHeartState(private val onHaptic: () -> Unit) {
    var visible by mutableStateOf(false)
        internal set
    internal var animKey by mutableStateOf(0)

    fun trigger() {
        onHaptic()
        animKey++
    }
}

@Composable
fun rememberLikeHeartState(): LikeHeartState {
    val haptic = LocalHapticFeedback.current
    val state = remember(haptic) {
        LikeHeartState { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    LaunchedEffect(state.animKey) {
        if (state.animKey == 0) return@LaunchedEffect
        state.visible = true
        delay(900)
        state.visible = false
    }
    return state
}

@Composable
fun LikeHeartOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 96.dp,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        ) + fadeIn(animationSpec = tween(durationMillis = 100)),
        exit = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(durationMillis = 300),
        ) + fadeOut(animationSpec = tween(durationMillis = 300)),
        modifier = modifier,
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Favorite,
            contentDescription = null,
            size = iconSize,
            tint = Color.White,
            filled = true,
        )
    }
}
