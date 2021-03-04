package com.sirelon.marsroverphotos.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * Created on 01.03.2021 22:33 for Mars-Rover-Photos.
 */
@Composable
fun StaggeredVerticalGrid(
    modifier: Modifier = Modifier,
    maxColumnWidth: Dp,
    children: @Composable () -> Unit
) {
    Layout(
//        children = children,
        modifier = modifier,
        content = children
    ) { measurables, constraints ->
        check(constraints.hasBoundedWidth) {
            "Unbounded width not supported"
        }
        val columns = ceil(constraints.maxWidth / maxColumnWidth.toPx()).toInt()
        val columnWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(maxWidth = columnWidth)
        val colHeights = IntArray(columns) { 0 } // track each column's height
        val placeables = measurables.map { measurable ->
            val column = shortestColumn(colHeights)
            val placeable = measurable.measure(itemConstraints)
            colHeights[column] += placeable.height
            placeable
        }

        val height = colHeights.maxOrNull()?.coerceIn(constraints.minHeight, constraints.maxHeight)
            ?: constraints.minHeight
        layout(
            width = constraints.maxWidth,
            height = height
        ) {
            val colY = IntArray(columns) { 0 }
            placeables.forEach { placeable ->
                val column = shortestColumn(colY)
                placeable.place(
                    x = columnWidth * column,
                    y = colY[column]
                )
                colY[column] += placeable.height
            }
        }
    }
}

private fun shortestColumn(colHeights: IntArray): Int {
    var minHeight = Int.MAX_VALUE
    var column = 0
    colHeights.forEachIndexed { index, height ->
        if (height < minHeight) {
            minHeight = height
            column = index
        }
    }
    return column
}

@Composable
fun <T> LazyGridFor(
    items: List<T> = listOf(),
    rows: Int = 3,
    hPadding: Int = 8,
    itemContent: @Composable LazyItemScope.(T, Int) -> Unit
) {
    val chunkedList = items.chunked(rows)
    LazyColumn(
        modifier = Modifier.padding(horizontal = hPadding.dp)
    ) {
        itemsIndexed(chunkedList) { index, it ->
            if (index == 0) {
                columnSpacer(value = 8)
            }

            Row {
                it.forEachIndexed { rowIndex, item ->
                    Box(
                        modifier = Modifier
                            .weight(1F)
                            .align(Alignment.Top)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        itemContent(item, index * rows + rowIndex)
                    }
                }
                repeat(rows - it.size) {
                    Box(
                        modifier = Modifier
                            .weight(1F)
                            .padding(8.dp)
                    ) {}
                }
            }
        }
    }
}

fun columnSpacer(value: Int) {

}

