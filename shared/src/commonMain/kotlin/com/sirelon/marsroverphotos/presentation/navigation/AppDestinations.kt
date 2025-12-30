package com.sirelon.marsroverphotos.presentation.navigation

/**
 * Navigation destinations for the app.
 */
sealed class AppDestination(val route: String) {
    data object Rovers : AppDestination("rovers")
    data object Photos : AppDestination("photos/{roverId}") {
        fun createRoute(roverId: Long) = "photos/$roverId"
    }
    data object Images : AppDestination("images")
    data object Favorite : AppDestination("favorite")
    data object Popular : AppDestination("popular")
    data object Mission : AppDestination("mission/{roverId}") {
        fun createRoute(roverId: Long) = "mission/$roverId"
    }
    data object About : AppDestination("about")
}
