package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

public val marsTopLevelDestinations: List<AppDestination> = listOf(
    AppDestination.Rovers,
    AppDestination.Favorite,
    AppDestination.Popular,
    AppDestination.About,
)

@Composable
public fun MarsBottomBar(
    selectedDestination: AppDestination,
    onDestinationClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MarsNavigationBar(
        selectedDestination = selectedDestination,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
public fun MarsNavigationBar(
    selectedDestination: AppDestination,
    onDestinationClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    NavigationBar(modifier = modifier) {
        marsNavigationItems.forEach { item ->
            val selected = item.destination == selectedDestination
            val label = stringResource(item.label)

            NavigationBarItem(
                selected = selected,
                enabled = enabled,
                onClick = { onDestinationClick(item.destination) },
                icon = {
                    MarsNavigationIcon(
                        icon = item.icon,
                        selected = selected,
                        contentDescription = label,
                    )
                },
                label = { Text(text = label) },
            )
        }
    }
}

@Composable
private fun MarsNavigationIcon(
    icon: MarsNavigationItemIcon,
    selected: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    when (icon) {
        is MarsNavigationItemIcon.Drawable -> {
            Icon(
                painter = painterResource(icon.resource),
                contentDescription = contentDescription,
                modifier = modifier,
            )
        }
        is MarsNavigationItemIcon.Symbol -> {
            MaterialSymbolIcon(
                symbol = icon.symbol,
                contentDescription = contentDescription,
                modifier = modifier,
                filled = selected,
            )
        }
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
