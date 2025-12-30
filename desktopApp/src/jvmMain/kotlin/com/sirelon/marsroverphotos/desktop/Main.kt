package com.sirelon.marsroverphotos.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material3.Text

// Placeholder - will be replaced with actual App composable from shared module
@Composable
fun PlaceholderApp() {
    Text("Mars Rover Photos - Desktop (Coming Soon)")
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mars Rover Photos"
    ) {
        // TODO: Initialize Koin
        // initKoin()

        // TODO: Load App from shared module
        // App()

        PlaceholderApp()
    }
}
