package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sirelon.marsroverphotos.presentation.screens.AboutScreen
import com.sirelon.marsroverphotos.presentation.screens.FavoriteScreen
import com.sirelon.marsroverphotos.presentation.screens.ImagesScreen
import com.sirelon.marsroverphotos.presentation.screens.MissionInfoScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosScreen
import com.sirelon.marsroverphotos.presentation.screens.PopularScreen
import com.sirelon.marsroverphotos.presentation.screens.RoversScreen

/**
 * Main navigation graph for the app.
 * Sets up all routes and screen transitions.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestination.Rovers.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Rovers list screen (home)
        composable(AppDestination.Rovers.route) {
            RoversScreen(
                onNavigateToPhotos = { roverId ->
                    navController.navigate(AppDestination.Photos.createRoute(roverId))
                }
            )
        }

        // Photos screen (browse photos for a specific rover)
        composable(
            route = AppDestination.Photos.route,
            arguments = listOf(
                navArgument("roverId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val roverId = backStackEntry.arguments?.getLong("roverId") ?: 0L
            PhotosScreen(
                roverId = roverId,
                onNavigateToImages = {
                    navController.navigate(AppDestination.Images.route)
                },
                onNavigateToMission = { roverId ->
                    navController.navigate(AppDestination.Mission.createRoute(roverId))
                }
            )
        }

        // Images screen (fullscreen image viewer)
        composable(AppDestination.Images.route) {
            ImagesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Favorite photos screen
        composable(AppDestination.Favorite.route) {
            FavoriteScreen()
        }

        // Popular photos screen
        composable(AppDestination.Popular.route) {
            PopularScreen()
        }

        // Mission info screen
        composable(
            route = AppDestination.Mission.route,
            arguments = listOf(
                navArgument("roverId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val roverId = backStackEntry.arguments?.getLong("roverId") ?: 0L
            MissionInfoScreen(
                roverId = roverId,
                onBack = { navController.popBackStack() }
            )
        }

        // About/Settings screen
        composable(AppDestination.About.route) {
            AboutScreen()
        }
    }
}
