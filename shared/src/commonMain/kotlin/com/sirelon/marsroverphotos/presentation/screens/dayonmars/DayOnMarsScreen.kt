package com.sirelon.marsroverphotos.presentation.screens.dayonmars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.domain.models.MissionDay
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.AppSectionHeader
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.NetworkImage
import com.sirelon.marsroverphotos.presentation.ui.PlatformDatePickerDialog
import com.sirelon.marsroverphotos.presentation.ui.photoContentDescription
import com.sirelon.marsroverphotos.presentation.viewmodels.DayOnMarsUiState
import com.sirelon.marsroverphotos.presentation.viewmodels.DayOnMarsViewModel
import com.sirelon.marsroverphotos.utils.formatDisplayDate
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

/**
 * "A day on Mars": the user picks an Earth date and sees, mission by mission, the photos taken on
 * it. The plain list the later collage step builds on.
 */
@Composable
fun DayOnMarsScreen(onBack: () -> Unit) {
    val viewModel: DayOnMarsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    DayOnMarsContent(
        state = state,
        onBack = onBack,
        onDateSelected = viewModel::selectDate,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOnMarsContent(
    state: DayOnMarsUiState,
    onBack: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onRetry: () -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val openPicker = { showPicker = true }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(),
        topBar = {
            AppTopBar(
                scrollBehavior = scrollBehavior,
                title = { Text("A day on Mars") },
                subtitle = state.date?.let { date -> { Text(formatDisplayDate(date.toString())) } },
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        val fill = Modifier.fillMaxSize().padding(innerPadding)
        when {
            state.date == null -> AppEmptyState(
                modifier = fill,
                title = "Pick an Earth date to see what every mission photographed that day",
                action = {
                    AppButton(onClick = openPicker, enabled = state.maxDate != null) {
                        Text("Pick a date")
                    }
                },
            )

            state.isLoading -> Box(modifier = fill) { CenteredProgress() }

            state.missions.isEmpty() -> AppEmptyState(
                modifier = fill,
                title = "No mission was on Mars that day",
                action = { AppButton(onClick = openPicker) { Text("Pick another date") } },
            )

            else -> MissionList(
                state = state,
                contentPadding = innerPadding,
                onPickDate = openPicker,
                onRetry = onRetry,
            )
        }
    }

    val maxDate = state.maxDate
    if (showPicker && maxDate != null) {
        PlatformDatePickerDialog(
            selectedDate = state.date ?: maxDate,
            onDateSelected = { date ->
                showPicker = false
                onDateSelected(date)
            },
            onDismissRequest = { showPicker = false },
            title = "Earth date",
            minDate = state.minDate,
            maxDate = maxDate,
        )
    }
}

@Composable
private fun MissionList(
    state: DayOnMarsUiState,
    contentPadding: PaddingValues,
    onPickDate: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().consumeWindowInsets(contentPadding),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + AppSpacing.lg,
            bottom = contentPadding.calculateBottomPadding() + AppSpacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "date") {
            AppOutlinedButton(onClick = onPickDate) {
                MaterialSymbolIcon(symbol = MaterialSymbol.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.size(AppSpacing.sm))
                Text("Change date")
            }
        }
        items(state.missions, key = { it.rover.id }) { mission ->
            MissionSection(
                mission = mission,
                onRetry = onRetry,
                modifier = Modifier.widthIn(max = AppSize.contentMaxWidth).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MissionSection(
    mission: MissionDay,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Column(modifier = Modifier.padding(horizontal = AppSpacing.lg)) {
            AppSectionHeader(title = mission.rover.name)
            Text(
                text = mission.photos.summary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (val photos = mission.photos) {
            is MissionDay.Photos.Found -> LazyRow(
                contentPadding = PaddingValues(horizontal = AppSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                items(photos.images, key = { it.id }) { image ->
                    NetworkImage(
                        imageUrl = image.imageUrl,
                        contentDescription = image.photoContentDescription(),
                        modifier = Modifier
                            .size(AppSize.dayOnMarsThumb)
                            .clip(MaterialTheme.shapes.medium),
                    )
                }
            }

            MissionDay.Photos.Failed -> AppOutlinedButton(
                onClick = onRetry,
                modifier = Modifier.padding(horizontal = AppSpacing.lg),
            ) { Text("Retry") }

            MissionDay.Photos.None, MissionDay.Photos.NotDated -> Unit
        }
    }
}

private fun MissionDay.Photos.summary(): String = when (this) {
    is MissionDay.Photos.Found -> {
        val count = total
        val solLabel = if (sols.size == 1) "Sol ${sols.single()}" else "Sols ${sols.joinToString(", ")}"
        val shown = if (images.size < count) " (showing ${images.size})" else ""
        "$solLabel · $count photo${if (count != 1) "s" else ""}$shown"
    }
    MissionDay.Photos.None -> "No photos taken this day"
    MissionDay.Photos.NotDated -> "Its photo archive is not dated by day"
    MissionDay.Photos.Failed -> "Couldn't load the photos"
}
