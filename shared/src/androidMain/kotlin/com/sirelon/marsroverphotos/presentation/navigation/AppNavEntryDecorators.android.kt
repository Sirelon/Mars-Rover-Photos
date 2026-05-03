package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
actual fun rememberAppNavEntryDecorators(): List<NavEntryDecorator<NavKey>> {
    val savedStateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    return remember(savedStateDecorator, viewModelStoreDecorator) {
        listOf(savedStateDecorator, viewModelStoreDecorator)
    }
}
