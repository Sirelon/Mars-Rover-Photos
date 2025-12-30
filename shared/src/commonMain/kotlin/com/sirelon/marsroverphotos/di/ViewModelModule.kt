package com.sirelon.marsroverphotos.di

import com.sirelon.marsroverphotos.presentation.viewmodels.FavoriteImagesViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.ImageViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.PopularPhotosViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.RoverMissionInfoViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for ViewModel dependencies.
 * Provides ViewModels for Compose screens.
 */
val viewModelModule = module {
    // ViewModels - using Koin viewModelOf for automatic lifecycle management
    viewModelOf(::FavoriteImagesViewModel)
    viewModelOf(::ImageViewModel)
    viewModelOf(::PhotosViewModel)
    viewModelOf(::PopularPhotosViewModel)
    viewModelOf(::RoverMissionInfoViewModel)
}
