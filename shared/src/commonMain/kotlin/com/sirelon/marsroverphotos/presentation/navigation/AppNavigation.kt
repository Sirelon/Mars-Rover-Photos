package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.presentation.ui.AdSlot
import com.sirelon.marsroverphotos.presentation.ui.UkraineBanner
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
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
    val isImages = currentDestination is AppDestination.Images

    LaunchedEffect(deepLink) {
        val target = deepLink ?: return@LaunchedEffect
        when (target) {
            is DeepLink.Rover -> navigator.navigate(AppDestination.Photos(target.id))
            is DeepLink.Photo -> navigator.navigate(
                AppDestination.Images(
                    photoIds = listOf(target.id.toString()),
                    selectedId = target.id.toString()
                )
            )

            is DeepLink.Image -> navigator.navigate(
                AppDestination.Images(
                    photoIds = listOf(target.id),
                    selectedId = target.id
                )
            )
        }
        onDeepLinkConsumed?.invoke()
    }

    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        // Images screen goes fully edge-to-edge: no status-bar inset, no bottom chrome.
        Column(
            modifier = if (isImages) modifier else modifier.windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (!isImages && currentDestination !is AppDestination.Ukraine) {
                UkraineBanner(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        tracker.trackClick("UkraineBanner_Root")
                        navigator.navigate(AppDestination.Ukraine)
                    },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.goBack() },
                    entryDecorators = entryDecorators,
                    entryProvider = entryProvider
                )
            }
            if (!isImages) {
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
}

private fun AppDestination.topLevelDestination(): AppDestination {
    return when (this) {
        AppDestination.Rovers,
        is AppDestination.Photos,
        is AppDestination.Images,
        is AppDestination.Mission,
        AppDestination.Ukraine -> AppDestination.Rovers

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
        AppDestination.Ukraine -> "ukraine"
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
            subclass(AppDestination.Ukraine::class, AppDestination.Ukraine.serializer())
        }
    }
}
