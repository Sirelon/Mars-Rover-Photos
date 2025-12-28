package com.sirelon.marsroverphotos.feature.photos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.feature.facts.EducationalFact
import com.sirelon.marsroverphotos.ui.MaterialSymbol
import com.sirelon.marsroverphotos.ui.MaterialSymbolIcon

/**
 * Displays an educational fact card in the photos grid.
 * Full-width card with distinct styling from photo cards.
 */
@Composable
fun FactCard(
    fact: EducationalFact,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Info,
                    contentDescription = "Educational fact",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Did You Know?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Fact text
            Text(
                text = fact.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
