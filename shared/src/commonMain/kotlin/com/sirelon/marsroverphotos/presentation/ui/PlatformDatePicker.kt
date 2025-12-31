package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Compose-only date picker dialog shared across platforms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformDatePickerDialog(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    title: String = "",
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    confirmText: String = "OK",
    dismissText: String = "Cancel"
) {
    val initialDate = selectedDate ?: currentLocalDate()
    val initialUtcMillis = localDateToUtcMillis(initialDate)
    val minUtcMillis = minDate?.let { localDateToUtcMillis(it) }
    val maxUtcMillis = maxDate?.let { localDateToUtcMillis(it) }

    val selectableDates = remember(minUtcMillis, maxUtcMillis, minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val afterMin = minUtcMillis?.let { utcTimeMillis >= it } ?: true
                val beforeMax = maxUtcMillis?.let { utcTimeMillis <= it } ?: true
                return afterMin && beforeMax
            }

            override fun isSelectableYear(year: Int): Boolean {
                val afterMin = minDate?.let { year >= it.year } ?: true
                val beforeMax = maxDate?.let { year <= it.year } ?: true
                return afterMin && beforeMax
            }
        }
    }

    val baseRange = DatePickerDefaults.YearRange
    val startYear = minDate?.year ?: baseRange.first
    val endYear = maxDate?.year ?: baseRange.last
    val yearRange = if (startYear <= endYear) startYear..endYear else endYear..startYear

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis,
        initialDisplayedMonthMillis = initialUtcMillis,
        yearRange = yearRange,
        selectableDates = selectableDates
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedUtc = datePickerState.selectedDateMillis
                    val resolvedDate = selectedUtc?.let { utcMillisToLocalDate(it) } ?: initialDate
                    if (isDateWithinRange(resolvedDate, minDate, maxDate)) {
                        onDateSelected(resolvedDate)
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissText)
            }
        },
        text = {
            DatePicker(
                state = datePickerState,
                title = if (title.isNotBlank()) {
                    { Text(title) }
                } else {
                    null
                }
            )
        }
    )
}

internal fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

internal fun localDateToUtcMillis(date: LocalDate): Long =
    date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun utcMillisToLocalDate(utcMillis: Long): LocalDate =
    kotlinx.datetime.Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date

internal fun isDateWithinRange(
    date: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?
): Boolean {
    val afterMin = minDate?.let { date >= it } ?: true
    val beforeMax = maxDate?.let { date <= it } ?: true
    return afterMin && beforeMax
}
