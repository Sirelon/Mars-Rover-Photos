package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.viewmodels.AdminPhotoItem
import com.sirelon.marsroverphotos.presentation.viewmodels.AdminPhotosState
import com.sirelon.marsroverphotos.presentation.viewmodels.AdminPhotosViewModel
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotoCheckStatus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPhotosScreen(onBack: () -> Unit) {
    val viewModel: AdminPhotosViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Popular Photos Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialSymbolIcon(MaterialSymbol.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            val selected = state.selectedPhotos
            if (state.phase == AdminPhotosState.Phase.Done && selected.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = viewModel::deleteSelected,
                        enabled = !state.isDeleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(AppSpacing.sm))
                        }
                        Text("Delete Selected (${selected.size})")
                    }
                }
            }
        }
    ) { padding ->
        when (state.phase) {
            AdminPhotosState.Phase.Idle -> IdleContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                onStart = viewModel::startCheck
            )

            AdminPhotosState.Phase.FetchingPhotos -> FetchingContent(
                modifier = Modifier.fillMaxSize().padding(padding)
            )

            AdminPhotosState.Phase.CheckingUrls, AdminPhotosState.Phase.Done -> {
                var showStaleOnly by rememberSaveable { mutableStateOf(false) }
                PhotoListContent(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    state = state,
                    showStaleOnly = showStaleOnly,
                    onToggleStaleFilter = { showStaleOnly = !showStaleOnly },
                    onSelectAllStale = viewModel::selectAllStale,
                    onDeselectAll = viewModel::deselectAll,
                    onToggle = { id -> viewModel.toggleSelection(id) }
                )
            }
        }
    }
}

@Composable
private fun IdleContent(modifier: Modifier, onStart: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Button(onClick = onStart) {
            Text("Fetch & Check All Photos")
        }
    }
}

@Composable
private fun FetchingContent(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(AppSpacing.md))
            Text("Fetching popular photos...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PhotoListContent(
    modifier: Modifier,
    state: AdminPhotosState,
    showStaleOnly: Boolean,
    onToggleStaleFilter: () -> Unit,
    onSelectAllStale: () -> Unit,
    onDeselectAll: () -> Unit,
    onToggle: (String) -> Unit
) {
    val isDone = state.phase == AdminPhotosState.Phase.Done
    val total = state.photos.size
    val checked = state.checkedCount
    val displayedPhotos = if (showStaleOnly && isDone) state.stalePhotos else state.photos

    Column(modifier) {
        if (!isDone) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
            ) {
                Text(
                    "Checking URLs: $checked / $total",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(AppSpacing.sm))
                LinearProgressIndicator(
                    progress = { if (total > 0) checked.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            val staleCount = state.stalePhotos.size
            val deletedCount = state.deletedCount
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Text(
                    buildString {
                        append("$staleCount stale of $total photos")
                        if (deletedCount > 0) append(" · $deletedCount deleted")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (staleCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showStaleOnly,
                            onClick = onToggleStaleFilter,
                            label = { Text("Stale only") }
                        )
                        OutlinedButton(onClick = onSelectAllStale) {
                            Text("Select All Stale ($staleCount)")
                        }
                        OutlinedButton(onClick = onDeselectAll) {
                            Text("Deselect All")
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        LazyColumn {
            items(displayedPhotos, key = { it.photo.id }) { item ->
                AdminPhotoRow(
                    item = item,
                    showCheckbox = isDone,
                    onToggle = { onToggle(item.photo.id) }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun AdminPhotoRow(
    item: AdminPhotoItem,
    showCheckbox: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showCheckbox) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        if (showCheckbox) {
            Checkbox(checked = item.selected, onCheckedChange = { onToggle() })
        }

        NetworkImage(
            imageUrl = item.photo.imageUrl,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            showPlaceholder = true
        )

        Column(Modifier.weight(1f)) {
            Text(
                text = item.photo.name.ifBlank { item.photo.id },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.photo.imageUrl.ifBlank { "(no URL)" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Sol ${item.photo.sol} · ${item.photo.earthDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        StatusChip(status = item.status)
    }
}

@Composable
private fun StatusChip(status: PhotoCheckStatus) {
    val colors = MaterialTheme.colorScheme
    val (label, color) = when (status) {
        PhotoCheckStatus.Pending -> "—" to colors.onSurfaceVariant.copy(alpha = 0.5f)
        PhotoCheckStatus.Checking -> "…" to colors.primary
        PhotoCheckStatus.Ok -> "OK" to Color(0xFF4CAF50)
        PhotoCheckStatus.Stale -> "404" to colors.error
        PhotoCheckStatus.Error -> "Err" to Color(0xFFFF9800)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
