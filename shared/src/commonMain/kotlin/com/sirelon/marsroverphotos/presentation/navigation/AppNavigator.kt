package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class AppNavigator(
    val backStack: NavBackStack<NavKey>
) {
    fun navigate(destination: AppDestination) {
        backStack.add(destination)
    }

    fun replaceTop(destination: AppDestination) {
        if (backStack.isEmpty()) {
            backStack.add(destination)
        } else {
            backStack[backStack.lastIndex] = destination
        }
    }

    fun selectTopLevel(destination: AppDestination) {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
        if (backStack.isEmpty()) {
            backStack.add(destination)
        } else {
            backStack[0] = destination
        }
    }

    fun goBack(): Boolean {
        if (backStack.size <= 1) {
            return false
        }
        backStack.removeLastOrNull()
        return true
    }
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> {
    error("AppNavigator not provided")
}

// Nullable + static: the scope is provided once by SharedTransitionLayout and never changes, so
// static avoids per-read invalidation; the null default lets previews/tests render screens that use
// sharedPhoto/sharedFavorite without a SharedTransitionLayout (the modifiers no-op when it's null).
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

// Provided by screens that run their own AnimatedContent layout transition (e.g. Mission Info
// compact ↔ expanded). Null-defaulted so sharedRoverImage/sharedRoverName no-op in previews.
val LocalMissionLayoutAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
