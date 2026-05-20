package com.sirelon.marsroverphotos.presentation.navigation

/**
 * Deep link targets supported by the app.
 */
sealed class DeepLink {
    data class Rover(val id: Long) : DeepLink()
    data class Photo(val id: Long) : DeepLink()
    /** Navigate directly to an image by its String ID (e.g. from the home-screen widget). */
    data class Image(val id: String) : DeepLink()
}
