package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sirelon.marsroverphotos.presentation.theme.AppSize

/**
 * Design-system dismissible notice — a single [AppRow] on an outlined card, for a nudge that sits
 * at the top of a list and goes away for good once acted on or dismissed (a release card, a
 * one-time opt-in). The whole card is tappable through [onClick]; [onDismiss] adds a close action
 * in the trailing slot. Never modal: it takes a slot in the list, not the screen.
 */
@Composable
fun AppNoticeCard(
    icon: MaterialSymbol,
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    AppSection(modifier = modifier, onClick = onClick) {
        AppRow(
            icon = icon,
            iconContainer = colors.secondaryContainer,
            iconTint = colors.onSecondaryContainer,
            label = title,
            sub = sub,
            trailing = onDismiss?.let { dismiss ->
                {
                    IconButton(onClick = dismiss) {
                        MaterialSymbolIcon(
                            symbol = MaterialSymbol.Close,
                            contentDescription = "Dismiss",
                            tint = colors.onSurfaceVariant,
                            size = AppSize.icon,
                        )
                    }
                }
            },
        )
    }
}
