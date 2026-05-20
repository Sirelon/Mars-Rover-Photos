@file:OptIn(KoinExperimentalAPI::class)

package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.screens.AboutScreen
import com.sirelon.marsroverphotos.presentation.screens.FavoriteScreen
import com.sirelon.marsroverphotos.presentation.screens.ImagesScreen
import com.sirelon.marsroverphotos.presentation.screens.RoverMissionInfoScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosScreen
import com.sirelon.marsroverphotos.presentation.screens.PopularScreen
import com.sirelon.marsroverphotos.presentation.screens.RoversScreen
import com.sirelon.marsroverphotos.presentation.screens.UkraineScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val navigationModule = module {
    navigation<AppDestination.Rovers> {
        val navigator = LocalAppNavigator.current
        RoversScreen(
            onNavigateToPhotos = { roverId ->
                navigator.navigate(AppDestination.Photos(roverId))
            },
            onMissionInfoClick = { roverId ->
                navigator.navigate(AppDestination.Mission(roverId))
            }
        )
    }

    navigation<AppDestination.Photos> { destination ->
        val navigator = LocalAppNavigator.current
        PhotosScreen(
            roverId = destination.roverId,
            onNavigateToImages = { photoId ->
                navigator.navigate(
                    AppDestination.Images(
                        photoIds = listOf(photoId),
                        selectedId = photoId
                    )
                )
            }
        )
    }

    navigation<AppDestination.Images> { destination ->
        val navigator = LocalAppNavigator.current
        ImagesScreen(
            photoIds = destination.photoIds,
            selectedId = destination.selectedId,
            onBack = { navigator.goBack() }
        )
    }

    navigation<AppDestination.Favorite> {
        val navigator = LocalAppNavigator.current
        FavoriteScreen(
            onNavigateToImages = { image, allImages ->
                navigator.navigate(
                    AppDestination.Images(
                        photoIds = allImages.map { it.id },
                        selectedId = image.id
                    )
                )
            },
            onNavigateToRovers = { navigator.selectTopLevel(AppDestination.Rovers) }
        )
    }

    navigation<AppDestination.Popular> {
        PopularScreen()
    }

    navigation<AppDestination.Mission> { destination ->
        val navigator = LocalAppNavigator.current
        RoverMissionInfoScreen(
            roverId = destination.roverId,
            onBack = { navigator.goBack() }
        )
    }

    navigation<AppDestination.About> {
        AboutScreen()
    }

    navigation<AppDestination.Ukraine> {
        UkraineScreen()
    }
}
