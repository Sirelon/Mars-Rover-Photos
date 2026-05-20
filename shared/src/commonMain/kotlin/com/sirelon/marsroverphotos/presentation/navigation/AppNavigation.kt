package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.presentation.ui.AdSlot
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

/**
 * Main Navigation 3 display for the app.
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    startDestination: AppDestination = AppDestination.Rovers,
    deepLink: DeepLink? = null,
    onDeepLinkConsumed: (() -> Unit)? = null
) {
    val backStack = rememberNavBackStack(
        configuration = navBackStackConfiguration,
        elements = arrayOf(startDestination)
    )
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val entryDecorators = rememberAppNavEntryDecorators()
    val entryProvider = koinEntryProvider<NavKey>()
    val tracker: Tracker = koinInject()
    val currentDestination = backStack.lastOrNull() as? AppDestination ?: startDestination

    LaunchedEffect(deepLink) {
        val target = deepLink ?: return@LaunchedEffect
        when (target) {
            is DeepLink.Rover -> navigator.navigate(AppDestination.Photos(target.id))
            is DeepLink.Photo -> navigator.navigate(AppDestination.Images(target.id.toString()))
        }
        onDeepLinkConsumed?.invoke()
    }

    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        Column(modifier = modifier) {
            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.goBack() },
                    entryDecorators = entryDecorators,
                    entryProvider = entryProvider
                )
            }
            AdSlot(modifier = Modifier.fillMaxWidth())
            MarsBottomBar(
                selectedDestination = currentDestination.topLevelDestination(),
                onDestinationClick = { destination ->
                    tracker.trackClick("bottom_${destination.analyticsTag}")
                    navigator.selectTopLevel(destination)
                }
            )
        }
    }
}

private fun AppDestination.topLevelDestination(): AppDestination {
    return when (this) {
        AppDestination.Rovers,
        is AppDestination.Photos,
        is AppDestination.Images,
        is AppDestination.Mission -> AppDestination.Rovers
        AppDestination.Favorite -> AppDestination.Favorite
        AppDestination.Popular -> AppDestination.Popular
        AppDestination.About -> AppDestination.About
    }
}

private val AppDestination.analyticsTag: String
    get() = when (this) {
        AppDestination.Rovers -> "rovers"
        AppDestination.Favorite -> "favorite"
        AppDestination.Popular -> "popular"
        AppDestination.About -> "about"
        else -> "other"
    }

private val navBackStackConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppDestination.Rovers::class, AppDestination.Rovers.serializer())
            subclass(AppDestination.Photos::class, AppDestination.Photos.serializer())
            subclass(AppDestination.Images::class, AppDestination.Images.serializer())
            subclass(AppDestination.Favorite::class, AppDestination.Favorite.serializer())
            subclass(AppDestination.Popular::class, AppDestination.Popular.serializer())
            subclass(AppDestination.Mission::class, AppDestination.Mission.serializer())
            subclass(AppDestination.About::class, AppDestination.About.serializer())
        }
    }
}
