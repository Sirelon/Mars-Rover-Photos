package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sirelon.marsroverphotos.presentation.theme.AppTypography

/**
 * Screen-level section title ("Mission Timeline", "Statistics", …).
 * Uses [AppTypography.sectionHeader] in tertiary colour.
 */
@Composable
fun AppSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = AppTypography.sectionHeader,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier,
    )
}

/**
 * Small all-caps accent label used inside cards and timeline milestones.
 * Uses labelSmall + wide tracking in secondary colour.
 */
@Composable
fun AppSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        ),
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier,
    )
}
