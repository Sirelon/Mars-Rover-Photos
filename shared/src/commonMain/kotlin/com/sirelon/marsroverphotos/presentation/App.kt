package com.sirelon.marsroverphotos.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.presentation.theme.MarsRoverPhotosTheme
import com.sirelon.marsroverphotos.presentation.ui.CenteredColumn
import com.sirelon.marsroverphotos.presentation.ui.isSystemInDarkTheme
import com.sirelon.marsroverphotos.presentation.ui.supportsDynamicColor
import org.koin.compose.koinInject

/**
 * Main app composable.
 * Root of the Compose UI hierarchy.
 */
@Composable
fun App() {
    val appSettings: AppSettings = koinInject()
    val theme by appSettings.themeFlow.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val dynamicColor = supportsDynamicColor()

    val useDarkTheme = when (theme) {
        Theme.DARK -> true
        Theme.WHITE -> false
        Theme.SYSTEM -> systemDarkTheme
    }

    MarsRoverPhotosTheme(
        darkTheme = useDarkTheme,
        dynamicColor = dynamicColor
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppContent()
        }
    }
}

@Composable
private fun AppContent() {
    // TODO: Setup Navigation Compose navigation graph
    // For now, show a placeholder
    CenteredColumn {
        Text(
            text = "Mars Rover Photos",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "KMP Migration in Progress",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
