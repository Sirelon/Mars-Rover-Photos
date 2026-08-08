package com.sirelon.marsroverphotos.presentation.screens.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.AppRowDivider
import com.sirelon.marsroverphotos.presentation.ui.WhatsNewRow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WhatsNewDialogScreen() {
    val navigator = LocalAppNavigator.current
    val viewModel: WhatsNewViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val release = state.currentRelease ?: return
    val hasDetail = release.changes.any { it.detail != null }

    fun dismiss() {
        viewModel.markSeen()
        navigator.goBack()
    }

    Dialog(onDismissRequest = ::dismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
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
                        onClick = if (hasDetail) {
                            { navigator.navigate(AppDestination.WhatsNewStory(release.version, page = index)) }
                        } else null,
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
