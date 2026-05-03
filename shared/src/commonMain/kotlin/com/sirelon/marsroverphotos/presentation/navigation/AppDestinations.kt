package com.sirelon.marsroverphotos.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation destinations for the app.
 */
@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Rovers : AppDestination

    @Serializable
    data class Photos(val roverId: Long) : AppDestination

    @Serializable
    data class Images(val photoId: String? = null) : AppDestination

    @Serializable
    data object Favorite : AppDestination

    @Serializable
    data object Popular : AppDestination

    @Serializable
    data class Mission(val roverId: Long) : AppDestination

    @Serializable
    data object About : AppDestination
}
