@file:OptIn(KoinExperimentalAPI::class)

package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.screens.AboutScreen
import com.sirelon.marsroverphotos.presentation.screens.FavoriteScreen
import com.sirelon.marsroverphotos.presentation.screens.PopularScreen
import com.sirelon.marsroverphotos.presentation.screens.RoverMissionInfoScreen
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

    // AppDestination.Photos and its dialog destinations (PhotosSolPicker, PhotosEarthDatePicker)
    // are declared as raw NavEntry in AppNavigation: they need a camera-independent contentKey so
    // the shared PhotosViewModel survives camera-filter changes, and the dialogs name the Photos
    // entry as their ViewModelStore parent (see SharedViewModelStoreNavEntryDecorator).

    // AppDestination.Images is declared as a raw NavEntry in AppNavigation so it can carry
    // NavDisplay.transitionSpec metadata (fade instead of the default horizontal slide).

    navigation<AppDestination.Favorite> {
        val navigator = LocalAppNavigator.current
        FavoriteScreen(
            onNavigateToImages = { image ->
                navigator.navigate(
                    AppDestination.Images(
                        selectedId = image.id,
                        source = AppDestination.ImagesSource.FAVORITES,
                    )
                )
            },
            onNavigateToRovers = { navigator.selectTopLevel(AppDestination.Rovers) }
        )
    }

    navigation<AppDestination.Popular> {
        val navigator = LocalAppNavigator.current
        PopularScreen(
            onNavigateToImages = { image ->
                navigator.navigate(
                    AppDestination.Images(
                        selectedId = image.id,
                        source = AppDestination.ImagesSource.POPULAR,
                    )
                )
            }
        )
    }

    navigation<AppDestination.Mission> { destination ->
        val navigator = LocalAppNavigator.current
        RoverMissionInfoScreen(
            roverId = destination.roverId,
            onBack = { navigator.goBack() },
            onCameraClick = { cameraAbbrev ->
                navigator.navigate(
                    AppDestination.Photos(
                        roverId = destination.roverId,
                        camera = cameraAbbrev
                    )
                )
            }
        )
    }

    navigation<AppDestination.About> {
        AboutScreen()
    }

    navigation<AppDestination.Ukraine> {
        val navigator = LocalAppNavigator.current
        UkraineScreen(onBack = { navigator.goBack() })
    }
}
