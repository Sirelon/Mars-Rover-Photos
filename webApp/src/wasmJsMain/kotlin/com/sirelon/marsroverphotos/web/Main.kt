package com.sirelon.marsroverphotos.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.material3.Text

// Placeholder - will be replaced with actual App composable from shared module
@Composable
fun PlaceholderApp() {
    Text("Mars Rover Photos - Web (Coming Soon)")
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget", title = "Mars Rover Photos") {
        // TODO: Initialize Koin
        // initKoin()

        // TODO: Load App from shared module
        // App()

        PlaceholderApp()
    }
}
