package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Maps a Photos rover id → the ViewModelStoreOwner of that Photos navigation entry.
 *
 * The Photos navigation entry registers its own [ViewModelStoreOwner] here (keyed by
 * roverId). Its dialog entries ([AppDestination.PhotosSolPicker],
 * [AppDestination.PhotosEarthDatePicker]) look it up so they can resolve the *same*
 * PhotosViewModel instance via koinViewModel(viewModelStoreOwner = ...).
 *
 * On Android the owner is the per-entry store (from rememberViewModelStoreNavEntryDecorator);
 * on iOS/Desktop there is no per-entry isolation so it is the shared root owner — either way
 * the dialog ends up with the same store as the Photos screen.
 *
 * Provided at the [AppNavigation] level so it is visible to every entry inside NavDisplay.
 */
val LocalPhotosViewModelStoreOwners =
    compositionLocalOf<MutableMap<Long, ViewModelStoreOwner>> { mutableMapOf() }
