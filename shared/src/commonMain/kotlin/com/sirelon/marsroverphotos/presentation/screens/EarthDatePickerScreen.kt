package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.runtime.Composable
import com.sirelon.marsroverphotos.presentation.ui.PlatformDatePickerDialog
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.cancel
import com.sirelon.marsroverphotos.shared.resources.select
import com.sirelon.marsroverphotos.shared.resources.select_date
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * Navigation-entry dialog that lets the user pick an Earth date.
 * Shares [PhotosViewModel] with the [com.sirelon.marsroverphotos.presentation.navigation.AppDestination.Photos]
 * back-stack entry via that entry's ViewModelStore (Nav3 SharedViewModelStoreNavEntryDecorator).
 */
@Composable
fun EarthDatePickerScreen(
    viewModel: PhotosViewModel,
    onDismiss: () -> Unit,
) {
    PlatformDatePickerDialog(
        selectedDate = millisToUtcLocalDate(viewModel.dateFromSol()),
        minDate = millisToUtcLocalDate(viewModel.minDate()),
        maxDate = millisToUtcLocalDate(viewModel.maxDate()),
        onDateSelected = { selectedDate ->
            viewModel.setEarthTime(selectedDate.toUtcMillis())
            onDismiss()
        },
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.select_date),
        confirmText = stringResource(Res.string.select),
        dismissText = stringResource(Res.string.cancel),
    )
}

private fun millisToUtcLocalDate(timeMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(timeMillis).toLocalDateTime(TimeZone.UTC).date

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
