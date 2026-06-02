package com.sirelon.marsroverphotos.web

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@Composable
fun PlaceholderApp() {
    Text("Mars Rover Photos - Web (Coming Soon)")
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        PlaceholderApp()
    }
}
