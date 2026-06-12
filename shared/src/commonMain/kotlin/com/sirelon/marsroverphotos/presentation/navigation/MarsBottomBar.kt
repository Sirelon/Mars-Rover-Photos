package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.ic_rovers
import com.sirelon.marsroverphotos.shared.resources.nav_about
import com.sirelon.marsroverphotos.shared.resources.nav_favorite
import com.sirelon.marsroverphotos.shared.resources.nav_popular
import com.sirelon.marsroverphotos.shared.resources.nav_rovers
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Adaptive navigation wrapper.
 *
 * - Compact (phone): NavigationBar at the bottom with exitAlways scroll-hide behaviour.
 * - Medium/Expanded (tablet, desktop): NavigationRail on the start edge, always visible.
 *
 * [resetScrollKey] — pass the current top-level destination so the bottom bar
 * snaps back into view whenever the user switches tabs.
 *
 * [bottomChrome] — content placed between the main area and the NavigationBar
 * (e.g. an ad slot), or at the bottom of the content column for the rail layout.
 *
 * [chromeVisible] — animates the navigation chrome (bar/rail + [bottomChrome]) in and
 * out. The content area resizes smoothly with it, so a fullscreen destination can
 * collapse the chrome on entry and expand it in step with the pop transition on exit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarsNavigationSuite(
    selectedDestination: AppDestination,
    onDestinationClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    resetScrollKey: Any? = null,
    chromeVisible: Boolean = true,
    bottomChrome: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo()
    )

    when (layoutType) {
        NavigationSuiteType.NavigationBar -> {
            val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

            // Snap the bar back to fully visible on tab switch.
            LaunchedEffect(resetScrollKey) {
                scrollBehavior.state.heightOffset = 0f
            }

            Column(modifier) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                ) {
                    content()
                }
                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = expandVertically(tween(CHROME_SHOW_MS)) + fadeIn(tween(CHROME_SHOW_MS)),
                    exit = shrinkVertically(tween(CHROME_HIDE_MS)) + fadeOut(tween(CHROME_HIDE_MS)),
                ) {
                    Column {
                        bottomChrome()
                        BottomAppBar(
                            scrollBehavior = scrollBehavior,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            marsNavigationItems.forEach { item ->
                                val selected = item.destination == selectedDestination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { onDestinationClick(item.destination) },
                                    icon = {
                                        val label = stringResource(item.label)
                                        MarsNavigationIcon(item.icon, selected, label)
                                    },
                                    label = { Text(stringResource(item.label)) },
                                )
                            }
                        }
                    }
                }
            }
        }

        else -> {
            Row(modifier) {
                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = expandHorizontally(tween(CHROME_SHOW_MS)) + fadeIn(tween(CHROME_SHOW_MS)),
                    exit = shrinkHorizontally(tween(CHROME_HIDE_MS)) + fadeOut(tween(CHROME_HIDE_MS)),
                ) {
                    NavigationRail {
                        marsNavigationItems.forEach { item ->
                            val selected = item.destination == selectedDestination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onDestinationClick(item.destination) },
                                icon = {
                                    val label = stringResource(item.label)
                                    MarsNavigationIcon(item.icon, selected, label)
                                },
                                label = { Text(stringResource(item.label)) },
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Box(Modifier.weight(1f)) {
                        content()
                    }
                    AnimatedVisibility(
                        visible = chromeVisible,
                        enter = expandVertically(tween(CHROME_SHOW_MS)) + fadeIn(tween(CHROME_SHOW_MS)),
                        exit = shrinkVertically(tween(CHROME_HIDE_MS)) + fadeOut(tween(CHROME_HIDE_MS)),
                    ) {
                        bottomChrome()
                    }
                }
            }
        }
    }
}

/** Chrome show matches the 450ms Images pop fade; hide is quicker so the open transition leads. */
internal const val CHROME_SHOW_MS = 450
internal const val CHROME_HIDE_MS = 250

@Composable
private fun MarsNavigationIcon(
    icon: MarsNavigationItemIcon,
    selected: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    when (icon) {
        is MarsNavigationItemIcon.Drawable -> Icon(
            painter = painterResource(icon.resource),
            contentDescription = contentDescription,
            modifier = modifier,
        )
        is MarsNavigationItemIcon.Symbol -> MaterialSymbolIcon(
            symbol = icon.symbol,
            contentDescription = contentDescription,
            modifier = modifier,
            filled = selected,
        )
    }
}

private data class MarsNavigationItem(
    val destination: AppDestination,
    val label: StringResource,
    val icon: MarsNavigationItemIcon,
)

private sealed interface MarsNavigationItemIcon {
    data class Drawable(val resource: DrawableResource) : MarsNavigationItemIcon
    data class Symbol(val symbol: MaterialSymbol) : MarsNavigationItemIcon
}

private val marsNavigationItems = listOf(
    MarsNavigationItem(
        destination = AppDestination.Rovers,
        label = Res.string.nav_rovers,
        icon = MarsNavigationItemIcon.Drawable(Res.drawable.ic_rovers),
    ),
    MarsNavigationItem(
        destination = AppDestination.Favorite,
        label = Res.string.nav_favorite,
        icon = MarsNavigationItemIcon.Symbol(MaterialSymbol.Favorite),
    ),
    MarsNavigationItem(
        destination = AppDestination.Popular,
        label = Res.string.nav_popular,
        icon = MarsNavigationItemIcon.Symbol(MaterialSymbol.LocalFireDepartment),
    ),
    MarsNavigationItem(
        destination = AppDestination.About,
        label = Res.string.nav_about,
        icon = MarsNavigationItemIcon.Symbol(MaterialSymbol.Info),
    ),
)
