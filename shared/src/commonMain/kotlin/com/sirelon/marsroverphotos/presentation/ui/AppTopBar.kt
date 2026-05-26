package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * App-level TopAppBar wrapper that lives in the design system.
 *
 * All screens should use this instead of [TopAppBar] directly so that
 * global appearance or behaviour (colours, elevation, shape, …) can be
 * adjusted in a single place without touching every screen.
 *
 * The opt-in to [ExperimentalMaterial3Api] is contained here, so callers
 * only need it if they also use other experimental APIs (e.g.
 * [TopAppBarDefaults.enterAlwaysScrollBehavior]).
 *
 * TODO: switch to MediumFlexibleTopAppBar once it becomes public in M3.
 *   It is currently internal in material3 1.5.0-alpha20 / CMP 1.11.0.
 *   When it lands, add: subtitle, titleHorizontalAlignment, collapsedHeight, expandedHeight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}
