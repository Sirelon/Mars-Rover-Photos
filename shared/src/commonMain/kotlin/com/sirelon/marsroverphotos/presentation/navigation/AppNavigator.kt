package com.sirelon.marsroverphotos.presentation.navigation

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
