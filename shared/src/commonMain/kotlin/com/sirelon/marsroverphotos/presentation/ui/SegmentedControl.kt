package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SegmentAnimMillis = 200
private val SegmentEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Segmented control — a pill of mutually exclusive options with a sliding `tertiary` indicator.
 * Tap or drag to select; the indicator follows the finger without interruption.
 *
 * ```
 * SegmentedControl(
 *     options = listOf(Theme.DARK, Theme.WHITE, Theme.SYSTEM),
 *     selected = currentTheme,
 *     onSelect = onThemeChange,
 *     label = { it.displayName },
 * )
 * ```
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)

    val offsets = remember(options.size) { mutableStateListOf(*Array(options.size) { 0f }) }
    val widths = remember(options.size) { mutableStateListOf(*Array(options.size) { 0f }) }
    var segmentHeight by remember { mutableStateOf(0f) }

    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex, offsets.toList(), widths.toList()) {
        val targetX = offsets.getOrElse(selectedIndex) { 0f }
        val targetWidth = widths.getOrElse(selectedIndex) { 0f }
        if (targetWidth <= 0f) return@LaunchedEffect
        if (!initialized) {
            indicatorX.snapTo(targetX)
            indicatorWidth.snapTo(targetWidth)
            initialized = true
        } else {
            launch { indicatorX.animateTo(targetX, tween(SegmentAnimMillis, easing = SegmentEasing)) }
            launch { indicatorWidth.animateTo(targetWidth, tween(SegmentAnimMillis, easing = SegmentEasing)) }
        }
    }

    // rememberUpdatedState so the drag lambda always sees the latest selected value without
    // needing to restart the DraggableState (which would cancel an in-progress drag).
    val currentSelected by rememberUpdatedState(selected)
    val dragAbsX = remember { floatArrayOf(0f) }

    fun findBest(x: Float): Int {
        var best = -1
        var bestDist = Float.MAX_VALUE
        for (i in options.indices) {
            val w = widths.getOrElse(i) { 0f }
            if (w <= 0f) continue
            val dist = abs(x - (offsets.getOrElse(i) { 0f } + w / 2f))
            if (dist < bestDist) { bestDist = dist; best = i }
        }
        return best
    }

    val draggableState = rememberDraggableState { delta ->
        dragAbsX[0] += delta
        val best = findBest(dragAbsX[0])
        if (best in options.indices && options[best] != currentSelected) onSelect(options[best])
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.background)
            .border(AppSize.hairline, colors.outlineVariant, CircleShape)
            .padding(AppSize.segmentedControlPadding)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = { dragAbsX[0] = it.x }
            )
            .pointerInput(options) {
                detectTapGestures { offset ->
                    val best = findBest(offset.x)
                    if (best in options.indices) onSelect(options[best])
                }
            }
    ) {
        if (initialized) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                    .width(with(density) { indicatorWidth.value.toDp() })
                    .height(with(density) { segmentHeight.toDp() })
                    .clip(CircleShape)
                    .background(colors.tertiary)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSize.segmentedControlGap)) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) colors.onTertiary else colors.onSurfaceVariant,
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val x = coords.positionInParent().x
                            if (offsets[index] != x) offsets[index] = x
                            if (widths[index] != coords.size.width.toFloat()) widths[index] = coords.size.width.toFloat()
                            segmentHeight = coords.size.height.toFloat()
                        }
                        .clip(CircleShape)
                        .background(if (!initialized && isSelected) colors.tertiary else Color.Transparent)
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
                )
            }
        }
    }
}
