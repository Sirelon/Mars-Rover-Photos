package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * Nav3 decorator that lets an entry share a parent entry's [androidx.lifecycle.ViewModelStore].
 *
 * Adapted from the official AndroidX nav3 "shared ViewModel" recipe. An entry declares its parent
 * via [parent] in its metadata; the parent's owner is then exposed through
 * [LocalSharedViewModelStoreOwner], so `koinViewModel(viewModelStoreOwner = ...)` resolves the same
 * instance the parent holds. The shared store is cleared when the parent entry is popped.
 */
@Composable
fun <T : Any> rememberSharedViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
): SharedViewModelStoreNavEntryDecorator<T> {
    val viewModelStoreProvider = rememberViewModelStoreProvider(viewModelStoreOwner)
    return remember(viewModelStoreOwner) {
        SharedViewModelStoreNavEntryDecorator(viewModelStoreProvider)
    }
}

class SharedViewModelStoreNavEntryDecorator<T : Any>(
    viewModelStoreProvider: ViewModelStoreProvider,
) : NavEntryDecorator<T>(
    onPop = { key -> viewModelStoreProvider.clearKey(key) },
    decorate = { entry ->
        val localOwner = rememberViewModelStoreOwner(
            entry.contentKey,
            viewModelStoreProvider,
            savedStateRegistryOwner = LocalSavedStateRegistryOwner.current,
        )
        val localValues: MutableList<ProvidedValue<*>> =
            mutableListOf(LocalViewModelStoreOwner provides localOwner)

        // If the entry declares a parent, also expose the parent's owner so the entry can
        // resolve the parent's ViewModel(s) via LocalSharedViewModelStoreOwner.
        val parentContentKey = entry.metadata[ParentKey]
        if (parentContentKey != null) {
            val parentOwner = rememberViewModelStoreOwner(
                parentContentKey,
                viewModelStoreProvider,
                savedStateRegistryOwner = LocalSavedStateRegistryOwner.current,
            )
            localValues.add(LocalSharedViewModelStoreOwner provides parentOwner)
        }
        CompositionLocalProvider(values = localValues.toTypedArray()) { entry.Content() }
    },
) {
    companion object {
        /** Declares the [contentKey][androidx.navigation3.runtime.NavEntry.contentKey] of the
         * parent entry whose ViewModelStore this entry shares. */
        fun parent(key: Any): Map<String, Any> = metadata {
            put(ParentKey, key)
        }

        object ParentKey : NavMetadataKey<Any>
    }
}

val LocalSharedViewModelStoreOwner =
    staticCompositionLocalOf<ViewModelStoreOwner> { error("No LocalSharedViewModelStoreOwner provided!") }
