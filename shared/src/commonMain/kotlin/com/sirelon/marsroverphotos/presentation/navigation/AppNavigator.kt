package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
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

// androidx.lifecycle.compose.dropUnlessResumed only covers zero-arg handlers. Nav callbacks
// declared on entries (onNavigateToPhotos: (Long) -> Unit, onNavigateToImages: (String, Set<String>)
// -> Unit, ...) need the same guard — reads the same per-entry LocalLifecycleOwner, so it drops the
// call exactly when the entry it's declared in is no longer RESUMED (mid pop/push transition),
// which is what keeps a fast double-tap from pushing two entries.
@Composable
fun <T> dropUnlessResumed(block: (T) -> Unit): (T) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(lifecycleOwner) {
        { arg: T ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) block(arg)
        }
    }
}

@Composable
fun <A, B> dropUnlessResumed(block: (A, B) -> Unit): (A, B) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(lifecycleOwner) {
        { a: A, b: B ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) block(a, b)
        }
    }
}
