package com.sirelon.marsroverphotos.presentation.screens.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.AppRow
import com.sirelon.marsroverphotos.presentation.ui.AppRowDivider
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.CardShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WhatsNewDialogScreen() {
    val navigator = LocalAppNavigator.current
    val viewModel: WhatsNewViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showPushOptIn by viewModel.showPushOptIn.collectAsStateWithLifecycle()
    val release = state.currentRelease ?: return
    val hasDetail = release.changes.any { it.detail != null }

    fun dismiss() {
        viewModel.markSeen()
        navigator.goBack()
    }

    Dialog(onDismissRequest = ::dismiss) {
        // Same shape + raised fill as every other grouped surface (AppOutlinedCard), so the dialog
        // reads as one of the app's cards rather than a one-off radius/elevation pair.
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                Column(modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xl)) {
                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Version ${release.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                release.changes.forEachIndexed { index, change ->
                    if (index > 0) AppRowDivider()
                    WhatsNewRow(
                        change = change,
                        // Same dismiss-then-open as "See All": opening a story counts as reading
                        // the dialog, so it must not be waiting again underneath when the story
                        // is popped.
                        onClick = if (hasDetail) {
                            {
                                dismiss()
                                navigator.navigate(AppDestination.WhatsNewStory(release.version, page = index))
                            }
                        } else null,
                    )
                }

                if (showPushOptIn) {
                    AppRowDivider()
                    AppRow(
                        icon = MaterialSymbol.Notifications,
                        iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                        label = "Mars Updates",
                        sub = "Get told about new photos and releases",
                        trailing = {
                            // Always unchecked: the row only exists while permission is
                            // undecided, and enablePushNotifications() clears showPushOptIn
                            // before it awaits the OS, so the whole row leaves on the same tap.
                            Switch(
                                checked = false,
                                onCheckedChange = { viewModel.enablePushNotifications() },
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg)
                        .padding(bottom = AppSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    if (hasDetail) {
                        AppOutlinedButton(
                            onClick = {
                                dismiss()
                                navigator.navigate(AppDestination.WhatsNewStory(release.version, page = 0))
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("See All") }
                    }
                    AppButton(
                        onClick = ::dismiss,
                        modifier = Modifier.weight(1f),
                    ) { Text("Got It") }
                }
            }
        }
    }
}
