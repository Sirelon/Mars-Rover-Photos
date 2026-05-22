package com.sirelon.marsroverphotos.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sirelon.marsroverphotos.di.initKoinDesktop
import com.sirelon.marsroverphotos.presentation.App
import kotlinx.coroutines.delay

// Mars orange (matches Android/iOS splash)
private val SplashBackground = Color(0xFFD84315)

/**
 * Desktop (JVM) entry point for Mars Rover Photos.
 * Shows a brief branded splash screen before the main UI.
 */
fun main() = application {
    initKoinDesktop()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Mars Rover Photos"
    ) {
        var showSplash by remember { mutableStateOf(true) }

        Box(Modifier.fillMaxSize()) {
            App(rateAppUrl = "https://play.google.com/store/apps/details?id=com.sirelon.marsroverphotos")

            AnimatedVisibility(
                visible = showSplash,
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SplashBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("icon.png"),
                        contentDescription = "Mars Rover Photos",
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            delay(1500)
            showSplash = false
        }
    }
}
