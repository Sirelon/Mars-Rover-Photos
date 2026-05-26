@file:OptIn(KoinExperimentalAPI::class)

package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.screens.AboutScreen
import com.sirelon.marsroverphotos.presentation.screens.EarthDatePickerScreen
import com.sirelon.marsroverphotos.presentation.screens.FavoriteScreen
import com.sirelon.marsroverphotos.presentation.screens.ImagesScreen
import com.sirelon.marsroverphotos.presentation.screens.PhotosScreen
import com.sirelon.marsroverphotos.presentation.screens.PopularScreen
import com.sirelon.marsroverphotos.presentation.screens.RoverMissionInfoScreen
import com.sirelon.marsroverphotos.presentation.screens.RoversScreen
import com.sirelon.marsroverphotos.presentation.screens.SolPickerScreen
import com.sirelon.marsroverphotos.presentation.screens.UkraineScreen
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

/** Marker type qualifying the Koin scope shared by the Photos flow (screen + its dialogs). */
class PhotosScope

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

    // Photos flow: the screen and its two dialog destinations live in one Koin scope and
    // share a single scoped PhotosViewModel. Each scoped navigation entry resolves the
    // same instance via the Scope receiver's get().
    scope<PhotosScope> {
        scoped { PhotosViewModel(get(), get(), get(), get(), get()) }

        navigation<AppDestination.Photos> { destination ->
            val navigator = LocalAppNavigator.current
            PhotosScreen(
                viewModel = get<PhotosViewModel>(),
                roverId = destination.roverId,
                cameraFilter = destination.camera,
                onNavigateToImages = { photoId ->
                    navigator.navigate(
                        AppDestination.Images(
                            photoIds = listOf(photoId),
                            selectedId = photoId,
                            source = AppDestination.ImagesSource.DIRECT_IDS,
                        )
                    )
                },
                onClearCameraFilter = {
                    navigator.replaceTop(AppDestination.Photos(destination.roverId, camera = null))
                },
                onBack = { navigator.goBack() },
                onOpenSolPicker = {
                    navigator.navigate(AppDestination.PhotosSolPicker(destination.roverId))
                },
                onOpenEarthDatePicker = {
                    navigator.navigate(AppDestination.PhotosEarthDatePicker(destination.roverId))
                },
            )
        }

        navigation<AppDestination.PhotosSolPicker> {
            val navigator = LocalAppNavigator.current
            SolPickerScreen(
                viewModel = get<PhotosViewModel>(),
                onDismiss = { navigator.goBack() },
            )
        }

        navigation<AppDestination.PhotosEarthDatePicker> {
            val navigator = LocalAppNavigator.current
            EarthDatePickerScreen(
                viewModel = get<PhotosViewModel>(),
                onDismiss = { navigator.goBack() },
            )
        }
    }

    navigation<AppDestination.Images> { destination ->
        val navigator = LocalAppNavigator.current
        ImagesScreen(
            photoIds = destination.photoIds,
            selectedId = destination.selectedId,
            source = destination.source,
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
