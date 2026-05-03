package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
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

    LaunchedEffect(deepLink) {
        val target = deepLink ?: return@LaunchedEffect
        when (target) {
            is DeepLink.Rover -> navigator.navigate(AppDestination.Photos(target.id))
            is DeepLink.Photo -> navigator.navigate(AppDestination.Images(target.id.toString()))
        }
        onDeepLinkConsumed?.invoke()
    }

    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = { navigator.goBack() },
            entryDecorators = entryDecorators,
            entryProvider = entryProvider
        )
    }
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
