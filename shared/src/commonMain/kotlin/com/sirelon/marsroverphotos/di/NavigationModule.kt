@file:OptIn(KoinExperimentalAPI::class)

package com.sirelon.marsroverphotos.di

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.screens.AboutScreen
import com.sirelon.marsroverphotos.presentation.screens.FavoriteScreen
import com.sirelon.marsroverphotos.presentation.screens.ImagesScreen
import com.sirelon.marsroverphotos.presentation.screens.PopularScreen
import com.sirelon.marsroverphotos.presentation.screens.RoverMissionInfoScreen
import com.sirelon.marsroverphotos.presentation.screens.RoversScreen
import com.sirelon.marsroverphotos.presentation.screens.UkraineScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal const val IMAGES_DESTINATION_KEY = "isImagesDestination"

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

    navigation<AppDestination.Images>(
        metadata = mapOf(IMAGES_DESTINATION_KEY to true) +
            NavDisplay.transitionSpec { EnterTransition.None togetherWith fadeOut(tween(300)) },
    ) { destination ->
        val navigator = LocalAppNavigator.current
        ImagesScreen(
            photoIds = destination.photoIds,
            selectedId = destination.selectedId,
            source = destination.source,
            roverId = destination.roverId,
            camera = destination.camera,
            cameras = destination.cameras,
            onBack = { navigator.goBack() }
        )
    }

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
