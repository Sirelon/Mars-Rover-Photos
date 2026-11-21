package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlag
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlags
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.RoverCoversRepository
import com.sirelon.marsroverphotos.domain.repositories.RoverCoversRepository.Cover
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
    roverCoversRepository: RoverCoversRepository,
    featureFlags: FeatureFlags,
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

    /**
     * Latest real photo per rover id, shown on its card over the static artwork. Starts with the
     * photos saved by the last launch; the repository refreshes them once per process.
     */
    val covers: StateFlow<Map<Long, Cover>> = roverCoversRepository.covers

    /**
     * The "Today on Mars" card: the freshest photo among the active rovers, or null while none is
     * known or the [FeatureFlag.TODAY_ON_MARS] switch is off.
     */
    val todayOnMars: StateFlow<TodayOnMars?> = combine(
        rovers,
        covers,
        featureFlags.isEnabled(FeatureFlag.TODAY_ON_MARS),
    ) { roster, roverCovers, enabled ->
        if (enabled) pickTodayOnMars(roster, roverCovers) else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    init {
        roverCoversRepository.refreshOnce()
    }

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
        roverOpened(rover, source = SourceRoverCard)
    }

    fun onTodayOnMarsClicked(today: TodayOnMars) {
        roverOpened(today.rover, source = SourceTodayOnMars)
    }

    private fun roverOpened(rover: Rover, source: String) {
        tracker.trackEvent(RoverSelectedEvent, mapOf(RoverParam to rover.name, SourceParam to source))
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

        /** Which control opened the rover: its card, or the "Today on Mars" card. */
        const val SourceParam = "source"
        const val SourceRoverCard = "rover_card"
        const val SourceTodayOnMars = "today_on_mars"
    }
}

/** The photo the "Today on Mars" card shows, with the rover whose feed a tap opens. */
data class TodayOnMars(val rover: Rover, val cover: Cover)

/**
 * The freshest dated photo among the active rovers, compared on the first 19 characters of the
 * date (`yyyy-MM-ddTHH:mm:ss`): the feeds write timestamps with and without milliseconds and a
 * zone suffix, and a bare date still sorts correctly against them. Closed
 * missions never qualify — their "latest" photo is years old — and neither does a cover with no
 * date (one saved by a build that stored only the URL).
 */
internal fun pickTodayOnMars(rovers: List<Rover>, covers: Map<Long, Cover>): TodayOnMars? =
    rovers.asSequence()
        .filter { it.isActive }
        .mapNotNull { rover ->
            covers[rover.id]?.takeIf { it.earthDate.isNotBlank() }?.let { TodayOnMars(rover, it) }
        }
        .maxWithOrNull(
            compareBy<TodayOnMars>({ it.cover.earthDate.take(TIMESTAMP_TO_SECONDS) }, { it.cover.sol })
        )

private const val TIMESTAMP_TO_SECONDS = 19

internal fun filterRovers(rovers: List<Rover>, query: String): List<Rover> =
    if (query.isBlank()) rovers
    else rovers.filter { it.name.contains(query, ignoreCase = true) }
