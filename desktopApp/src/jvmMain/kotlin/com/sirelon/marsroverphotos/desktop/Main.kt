package com.sirelon.marsroverphotos.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sirelon.marsroverphotos.di.initKoinDesktop
import com.sirelon.marsroverphotos.presentation.App

/**
 * Desktop (JVM) entry point for Mars Rover Photos.
 * Uses the shared Compose Multiplatform UI.
 */
fun main() = application {
    // Initialize Koin dependency injection
    initKoinDesktop()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Mars Rover Photos - Desktop"
    ) {
        // Load shared App composable
        App()
    }
}
