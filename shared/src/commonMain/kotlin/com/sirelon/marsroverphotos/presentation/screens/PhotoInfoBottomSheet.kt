package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.data.database.entities.MarsImage

/**
 * Bottom sheet displaying detailed information about a Mars rover photo.
 *
 * Shows:
 * - Description (Perseverance only)
 * - Credit (Perseverance only)
 * - Camera information
 * - Sol and Earth date
 * - Usage statistics
 *
 * Ported from `app/.../feature/images/PhotoInfoBottomSheet.kt` to Compose Multiplatform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoInfoBottomSheet(
    image: MarsImage,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Photo Information",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Description section (Perseverance only)
            image.description?.takeIf { it.isNotBlank() }?.let { description ->
                InfoSection(title = "Description", content = description)
            }

            // Credit section (Perseverance only)
            image.credit?.takeIf { it.isNotBlank() }?.let { credit ->
                InfoSection(title = "Credit", content = credit)
            }

            // Camera information
            image.camera?.let { camera ->
                InfoSection(
                    title = "Camera",
                    content = "${camera.fullName} (${camera.name})"
                )
            }

            // Sol and Earth Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoSection(title = "Sol", content = image.sol.toString())
                }
                Column(modifier = Modifier.weight(1f)) {
                    InfoSection(title = "Earth Date", content = image.earthDate)
                }
            }

            // Statistics
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Views", image.stats.see)
                StatItem("Scales", image.stats.scale)
                StatItem("Saves", image.stats.save)
                StatItem("Shares", image.stats.share)
                StatItem("Favorites", image.stats.favorite)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatItem(label: String, value: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formatStatValue(value),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label.lowercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * KMP-safe compact number formatter.
 * Replaces `String.format("%.1fK", value / 1000.0)` which is JVM-only.
 *
 * Examples: 0 → "0", 999 → "999", 1000 → "1.0K", 1500 → "1.5K", 12345 → "12.3K"
 */
private fun formatStatValue(value: Long): String {
    if (value < 1000) return value.toString()
    val thousands = value / 1000
    val tenths = (value % 1000) / 100
    return "${thousands}.${tenths}K"
}
