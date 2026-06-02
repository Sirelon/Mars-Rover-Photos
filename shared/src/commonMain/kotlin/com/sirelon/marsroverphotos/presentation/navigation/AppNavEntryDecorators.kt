package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
fun rememberAppNavEntryDecorators(): List<NavEntryDecorator<NavKey>> {
    val savedStateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val sharedViewModelStoreDecorator = rememberSharedViewModelStoreNavEntryDecorator<NavKey>()
    return remember(savedStateDecorator, sharedViewModelStoreDecorator) {
        listOf(savedStateDecorator, sharedViewModelStoreDecorator)
    }
}
