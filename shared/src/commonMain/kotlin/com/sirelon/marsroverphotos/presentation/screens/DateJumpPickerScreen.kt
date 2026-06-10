package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.AppTypography
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.AppSolEarthCard
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.PlatformDatePickerDialog
import com.sirelon.marsroverphotos.presentation.ui.localDateToUtcMillis
import com.sirelon.marsroverphotos.presentation.ui.utcMillisToLocalDate
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.cancel
import com.sirelon.marsroverphotos.shared.resources.go_to_page_fmt
import com.sirelon.marsroverphotos.shared.resources.go_to_sol_fmt
import com.sirelon.marsroverphotos.shared.resources.jump_to_date_subtitle
import com.sirelon.marsroverphotos.shared.resources.jump_to_date_title
import com.sirelon.marsroverphotos.shared.resources.jump_to_page_title
import com.sirelon.marsroverphotos.shared.resources.or_pick_earth_date
import com.sirelon.marsroverphotos.shared.resources.page_of_fmt
import com.sirelon.marsroverphotos.shared.resources.select
import com.sirelon.marsroverphotos.shared.resources.select_date
import org.jetbrains.compose.resources.stringResource

/**
 * Combined "Jump to a date" (sol-mode) or "Jump to a page" (page-mode) bottom sheet.
 * Replaces the separate SolPickerScreen and EarthDatePickerScreen.
 * Shares [PhotosViewModel] with the Photos back-stack entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateJumpPickerScreen(
    viewModel: PhotosViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        if (uiState.showSolControls) {
            SolDateContent(viewModel = viewModel, onDismiss = onDismiss)
        } else {
            PageContent(
                totalPagePhotos = uiState.totalPagePhotos,
                onDismiss = onDismiss,
                onConfirm = { page ->
                    viewModel.loadByPage(page)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun SolDateContent(
    viewModel: PhotosViewModel,
    onDismiss: () -> Unit,
) {
    var maxSol by remember { mutableLongStateOf(1L) }
    LaunchedEffect(Unit) {
        maxSol = viewModel.maxSol().coerceAtLeast(1L)
    }

    val currentSol by viewModel.solFlow.collectAsState(initial = 0L)
    var selectedSol by remember(currentSol) { mutableLongStateOf(currentSol) }
    var showDatePicker by remember { mutableStateOf(false) }

    val earthDateText = viewModel.earthDateStr(selectedSol)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.xl)
            .padding(bottom = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(
                text = stringResource(Res.string.jump_to_date_title),
                style = AppTypography.appTitle,
            )
            Text(
                text = stringResource(Res.string.jump_to_date_subtitle),
                style = AppTypography.bodySecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AppSolEarthCard(sol = selectedSol, earthDate = earthDateText)

        Column {
            Slider(
                value = selectedSol.coerceIn(0L, maxSol).toFloat(),
                valueRange = 0f..maxSol.toFloat(),
                onValueChange = { selectedSol = it.toLong() },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "0",
                    style = AppTypography.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = maxSol.toString(),
                    style = AppTypography.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(
                text = stringResource(Res.string.or_pick_earth_date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            EarthDateRow(
                earthDate = earthDateText,
                onClick = { showDatePicker = true },
            )
        }

        ActionRow(
            confirmText = stringResource(Res.string.go_to_sol_fmt, selectedSol),
            onDismiss = onDismiss,
            onConfirm = {
                viewModel.loadBySol(selectedSol)
                onDismiss()
            },
        )
    }

    if (showDatePicker) {
        PlatformDatePickerDialog(
            selectedDate = utcMillisToLocalDate(viewModel.earthTime(selectedSol)),
            minDate = utcMillisToLocalDate(viewModel.minDate()),
            maxDate = utcMillisToLocalDate(viewModel.maxDate()),
            onDateSelected = { date ->
                selectedSol = viewModel.solFromDate(localDateToUtcMillis(date))
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false },
            title = stringResource(Res.string.select_date),
            confirmText = stringResource(Res.string.select),
            dismissText = stringResource(Res.string.cancel),
        )
    }
}

@Composable
private fun EarthDateRow(
    earthDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = earthDate.ifBlank { "—" },
                style = AppTypography.body,
            )
            MaterialSymbolIcon(
                symbol = MaterialSymbol.CalendarMonth,
                contentDescription = null,
                size = AppSize.iconDefault,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PageContent(
    totalPagePhotos: Int,
    onDismiss: () -> Unit,
    onConfirm: (page: Int) -> Unit,
) {
    val maxPage = remember(totalPagePhotos) {
        if (totalPagePhotos > 0) (totalPagePhotos + 99) / 100 else 1
    }
    var selectedPage by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.xl)
            .padding(bottom = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.jump_to_page_title),
            style = AppTypography.appTitle,
        )

        Text(
            text = stringResource(Res.string.page_of_fmt, selectedPage, maxPage),
            style = AppTypography.infoLabel,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
        )

        Column {
            Slider(
                value = selectedPage.toFloat(),
                valueRange = 1f..maxPage.toFloat(),
                onValueChange = { selectedPage = it.toInt() },
                modifier = Modifier.fillMaxWidth(),
                enabled = totalPagePhotos > 0,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "1",
                    style = AppTypography.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = maxPage.toString(),
                    style = AppTypography.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ActionRow(
            confirmText = stringResource(Res.string.go_to_page_fmt, selectedPage),
            onDismiss = onDismiss,
            onConfirm = { onConfirm(selectedPage) },
            confirmEnabled = totalPagePhotos > 0,
        )
    }
}

@Composable
private fun ActionRow(
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        AppOutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        ) {
            Text(stringResource(Res.string.cancel))
        }
        AppButton(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(2f).heightIn(min = 56.dp),
        ) {
            Text(confirmText)
        }
    }
}
