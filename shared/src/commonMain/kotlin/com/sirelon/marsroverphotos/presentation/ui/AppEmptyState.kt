package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import org.jetbrains.compose.resources.painterResource

/**
 * Shared empty / error state composable for all screens.
 *
 * Shows the alien mascot by default, a headline [title], and an optional
 * [action] slot (typically an [AppButton] or a retry label).
 *
 * Usage:
 * ```
 * AppEmptyState(
 *     title = stringResource(Res.string.no_photos_title),
 *     action = { AppButton(onClick = onRetry) { Text("Tap to retry") } }
 * )
 * ```
 *
 * Set [showImage] = false on smaller surfaces (e.g. inline error rows) where
 * the mascot would be too dominant.
 */
@Composable
fun AppEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    showImage: Boolean = true,
    action: @Composable (() -> Unit)? = null,
) {
    CenteredColumn(modifier = modifier) {
        if (showImage) {
            Image(
                painter = painterResource(Res.drawable.alien_icon),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
            )
            Spacer(modifier = Modifier.size(AppSpacing.lg))
        }
        Text(
            text = title,
            style = AppTypography.appTitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(modifier = Modifier.size(AppSpacing.lg))
            action()
        }
    }
}
