package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionData
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import org.koin.compose.koinInject

/**
 * Filters bottom sheet — camera selection, date navigation shortcuts, and appearance toggles.
 * Camera selection is staged locally; changes only take effect when Apply is tapped.
 * Shares [PhotosViewModel] with the Photos back-stack entry via [LocalSharedViewModelStoreOwner].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotosFiltersScreen(
    viewModel: PhotosViewModel,
    roverId: Long,
    onDismiss: () -> Unit,
    onOpenDateJumpPicker: () -> Unit,
    appSettings: AppSettings = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameras = RoverMissionData.getCamerasForRover(roverId)

    // Pending camera selection — committed only when Apply is tapped.
    var pendingCameras by remember(uiState.cameraFilters) { mutableStateOf(uiState.cameraFilters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.headlineSmall,
            )

            // ── Camera ──────────────────────────────────────────────────────────
            if (cameras.isNotEmpty()) {
                Text(
                    text = "Camera",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = pendingCameras.isEmpty(),
                        onClick = { pendingCameras = emptySet() },
                        label = { Text("All") },
                    )
                    cameras.forEach { camera ->
                        val selected = camera.name in pendingCameras
                        FilterChip(
                            selected = selected,
                            onClick = {
                                pendingCameras = if (selected) {
                                    pendingCameras - camera.name
                                } else {
                                    pendingCameras + camera.name
                                }
                            },
                            label = { Text(camera.name) },
                        )
                    }
                }

                HorizontalDivider()
            }

            // ── Date ─────────────────────────────────────────────────────────────
            Text(
                text = "Date",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenDateJumpPicker,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text("Jump to date")
            }

            HorizontalDivider()

            // ── Appearance ───────────────────────────────────────────────────────
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Show camera name",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = uiState.showCameraName,
                    onCheckedChange = { appSettings.showCameraName = it },
                )
            }

            HorizontalDivider()

            // ── Apply ────────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                onClick = {
                    viewModel.setCameraFilters(pendingCameras)
                    onDismiss()
                },
            ) {
                Text("Apply")
            }
        }
    }
}
