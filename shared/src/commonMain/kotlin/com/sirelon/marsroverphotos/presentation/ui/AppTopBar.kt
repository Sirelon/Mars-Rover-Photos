package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    val resolvedNavigationIcon: @Composable () -> Unit = when {
        navigationIcon != null -> navigationIcon
        onBack != null -> {
            {
                IconButton(onClick = onBack) {
                    MaterialSymbolIcon(
                        symbol = MaterialSymbol.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        }
        else -> ({})
    }

    val bottomInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    val safeInsets = windowInsets.union(bottomInsets)

    TopAppBar(
        modifier = modifier.windowInsetsPadding(safeInsets),
        title = {
            if (subtitle == null) {
                title()
            } else {
                Column {
                    title()
                    subtitle()
                }
            }
        },
        navigationIcon = resolvedNavigationIcon,
        actions = { actions() },
        scrollBehavior = scrollBehavior,
        colors = colors,
    )
}
