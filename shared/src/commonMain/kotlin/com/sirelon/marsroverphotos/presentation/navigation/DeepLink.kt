package com.sirelon.marsroverphotos.presentation.navigation

/**
 * Deep link targets supported by the app.
 */
sealed class DeepLink {
    data class Rover(val id: Long) : DeepLink()
    data class Photo(val id: Long) : DeepLink()
}
