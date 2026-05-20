package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Placeholder screens for navigation testing.
 * These will be replaced with actual screens once the UI components are migrated.
 */

@Composable
fun FavoriteScreen() {
    PlaceholderScreen(
        title = "Favorite Photos",
        description = "Your favorite Mars photos\n\n(Screen pending migration)"
    )
}

@Composable
fun PopularScreen() {
    PlaceholderScreen(
        title = "Popular Photos",
        description = "Most popular photos from the community\n\n(Screen pending migration)"
    )
}

@Composable
fun MissionInfoScreen(roverId: Long, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Mission Info",
        description = "Rover ID: $roverId\nMission details, timeline, and cameras\n\n(Screen pending migration)"
    ) {
        Button(onClick = onBack) {
            Text("Back to Photos")
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actions != null) {
            Spacer(modifier = Modifier.height(24.dp))
            actions()
        }
    }
}
