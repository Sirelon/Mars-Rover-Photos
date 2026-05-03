@file:OptIn(KoinExperimentalAPI::class)

package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.screens.AboutScreen
import com.sirelon.marsroverphotos.presentation.screens.FavoriteScreen
import com.sirelon.marsroverphotos.presentation.screens.ImagesScreen
import com.sirelon.marsroverphotos.presentation.screens.MissionInfoScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosScreen
import com.sirelon.marsroverphotos.presentation.screens.PopularScreen
import com.sirelon.marsroverphotos.presentation.screens.RoversScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val navigationModule = module {
    navigation<AppDestination.Rovers> {
        val navigator = LocalAppNavigator.current
        RoversScreen(
            onNavigateToPhotos = { roverId ->
                navigator.navigate(AppDestination.Photos(roverId))
            }
        )
    }

    navigation<AppDestination.Photos> { destination ->
        val navigator = LocalAppNavigator.current
        PhotosScreen(
            roverId = destination.roverId,
            onNavigateToImages = {
                navigator.navigate(AppDestination.Images())
            },
            onNavigateToMission = { roverId ->
                navigator.navigate(AppDestination.Mission(roverId))
            }
        )
    }

    navigation<AppDestination.Images> { destination ->
        val navigator = LocalAppNavigator.current
        ImagesScreen(
            photoId = destination.photoId,
            onBack = { navigator.goBack() }
        )
    }

    navigation<AppDestination.Favorite> {
        FavoriteScreen()
    }

    navigation<AppDestination.Popular> {
        PopularScreen()
    }

    navigation<AppDestination.Mission> { destination ->
        val navigator = LocalAppNavigator.current
        MissionInfoScreen(
            roverId = destination.roverId,
            onBack = { navigator.goBack() }
        )
    }

    navigation<AppDestination.About> {
        AboutScreen()
    }
}
