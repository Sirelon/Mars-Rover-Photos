package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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

/** Emphasized motion (~200ms, cubic-bezier(.2,0,0,1)) for the sliding selection indicator. */
private const val SegmentAnimMillis = 200
private val SegmentEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Design-system segmented control — a pill of mutually exclusive options. The selected option is
 * marked by a `tertiary` highlight that **slides** to the chosen segment; the others read as plain
 * labels. Tap a segment or drag horizontally across the control to change the selection. Generic
 * over the option type [T]: pass the [options], the currently [selected] one, an [onSelect]
 * callback, and a [label] mapper.
 *
 * Usage:
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

    // Per-segment bounds in px (relative to the segment Row), filled in via onGloballyPositioned.
    val offsets = remember(options.size) { mutableStateListOf(*Array(options.size) { 0f }) }
    val widths = remember(options.size) { mutableStateListOf(*Array(options.size) { 0f }) }
    var segmentHeight by remember { mutableStateOf(0f) }

    // Animated highlight position + width; snapped on first measure, animated on later changes.
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
            launch {
                indicatorWidth.animateTo(targetWidth, tween(SegmentAnimMillis, easing = SegmentEasing))
            }
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.background)
            .border(AppSize.hairline, colors.outlineVariant, CircleShape)
            .padding(AppSize.segmentedControlPadding)
            .pointerInput(options, offsets.toList(), widths.toList()) {
                val padPx = AppSize.segmentedControlPadding.toPx()
                detectHorizontalDragGestures { change, _ ->
                    val x = change.position.x - padPx
                    var best = selectedIndex
                    var bestDistance = Float.MAX_VALUE
                    for (i in options.indices) {
                        if (widths[i] <= 0f) continue
                        val distance = abs(x - (offsets[i] + widths[i] / 2f))
                        if (distance < bestDistance) {
                            bestDistance = distance
                            best = i
                        }
                    }
                    if (best in options.indices && options[best] != selected) onSelect(options[best])
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
                            if (widths[index] != coords.size.width.toFloat()) {
                                widths[index] = coords.size.width.toFloat()
                            }
                            segmentHeight = coords.size.height.toFloat()
                        }
                        .clip(CircleShape)
                        // Fallback fill for the one frame before the overlay is measured.
                        .background(if (!initialized && isSelected) colors.tertiary else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
                )
            }
        }
    }
}
