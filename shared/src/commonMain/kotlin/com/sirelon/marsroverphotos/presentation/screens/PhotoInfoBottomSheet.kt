package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.toLocalDateTime

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
                .navigationBarsPadding()
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

            // Sol and Earth Date — Sol is absent for page-mode rovers (Spirit/Opportunity, sol=0).
            if (image.sol != 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoSection(title = "Sol", content = image.sol.toString())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        InfoSection(title = "Earth Date", content = formatEarthDate(image.earthDate))
                    }
                }
            } else if (image.earthDate.isNotBlank()) {
                InfoSection(title = "Date", content = formatEarthDate(image.earthDate))
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

/**
 * Formats an ISO-8601 timestamp (e.g. `2025-03-16T22:11:53Z`) as a human-friendly
 * date+time in UTC. Falls back to the raw input if parsing fails so the UI never crashes.
 *
 * Example: `2025-03-16T22:11:53Z` → `Mar 16, 2025 · 22:11 UTC`
 */
internal fun formatEarthDate(iso: String): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
    val ldt = instant.toLocalDateTime(TimeZone.UTC)
    val month = MonthNames.ENGLISH_ABBREVIATED.names[ldt.monthNumber - 1]
    val hh = ldt.hour.toString().padStart(2, '0')
    val mm = ldt.minute.toString().padStart(2, '0')
    return "$month ${ldt.dayOfMonth}, ${ldt.year} · $hh:$mm UTC"
}
