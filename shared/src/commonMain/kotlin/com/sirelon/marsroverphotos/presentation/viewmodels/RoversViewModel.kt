package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.ConsentPromptGate
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RoversViewModel(
    roversRepository: RoversRepository,
    private val tracker: Tracker,
    private val appSettings: AppSettings,
    private val consentPromptGate: ConsentPromptGate,
) : ViewModel() {

    val rovers: StateFlow<List<Rover>> = roversRepository.getRovers()
        .catch { e ->
            Logger.e("RoversViewModel", e) { "Error loading rovers" }
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredRovers: StateFlow<List<Rover>> = combine(rovers, searchQuery) { list, query ->
        filterRovers(list, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onRoverClicked(rover: Rover) {
        tracker.trackEvent(RoverSelectedEvent, mapOf(RoverParam to rover.name))
        // The ad-consent prompts go up on the second rover the user has ever opened: counted across
        // sessions, so a one-rover-per-visit user is asked on the next visit's tap, and released
        // only here, on a tap, so a returning user meets the sheet at a navigation moment rather
        // than at launch off the saved count.
        if (appSettings.recordRoverOpened() >= ConsentPromptRoverTaps) consentPromptGate.open()
    }

    fun onMissionInfoClicked(rover: Rover) {
        tracker.trackEvent(MissionInfoSelectedEvent, mapOf(RoverParam to rover.name))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /**
         * Rover name travels as a parameter so one event covers every rover; baking it into the
         * event name (the superseded `click_rover_*` / `click_mission_info_*` names) spreads a
         * single interaction across as many GA4 events as there are rovers, none of which can be
         * summed without a regex.
         *
         * Tapping the mission-info button is deliberately not `mission_info_opened` — that name
         * belongs to the screen actually opening, which RoverMissionInfoScreen reports.
         */
        const val RoverSelectedEvent = "rover_selected"

        /** The rover tap, counted across sessions, that releases the ad-consent prompts. */
        const val ConsentPromptRoverTaps = 2
        const val MissionInfoSelectedEvent = "mission_info_selected"
        const val RoverParam = "rover"
    }
}

internal fun filterRovers(rovers: List<Rover>, query: String): List<Rover> =
    if (query.isBlank()) rovers
    else rovers.filter { it.name.contains(query, ignoreCase = true) }
