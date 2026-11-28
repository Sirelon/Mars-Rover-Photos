package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.models.MissionDay
import com.sirelon.marsroverphotos.domain.repositories.DayOnMarsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class DayOnMarsUiState(
    /** Null until the user picks a date. */
    val date: LocalDate? = null,
    /** The first date the picker allows; null until the mission roster is read. */
    val minDate: LocalDate? = null,
    /** The last date the picker allows (today, UTC); null until the mission roster is read. */
    val maxDate: LocalDate? = null,
    val isLoading: Boolean = false,
    /** Every mission on the surface on [date]; empty before a date is picked. */
    val missions: ImmutableList<MissionDay> = persistentListOf(),
)

/** "A day on Mars": the user picks an Earth date and sees what every mission photographed on it. */
class DayOnMarsViewModel(
    private val repository: DayOnMarsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DayOnMarsUiState())
    val state: StateFlow<DayOnMarsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val range = repository.dateRange()
            _state.update { it.copy(minDate = range?.start, maxDate = range?.endInclusive) }
        }
    }

    fun selectDate(date: LocalDate) {
        loadJob?.cancel()
        _state.update { it.copy(date = date, isLoading = true, missions = persistentListOf()) }
        loadJob = viewModelScope.launch {
            val missions = repository.missionsOn(date)
            _state.update { it.copy(isLoading = false, missions = missions.toImmutableList()) }
        }
    }

    /** Asks again for the current date, e.g. after a mission failed to load. */
    fun retry() {
        state.value.date?.let(::selectDate)
    }
}
