package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.mission.CameraSpec
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionData
import com.sirelon.marsroverphotos.domain.models.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.domain.repositories.MissionRepository
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.FirebaseAnalytics
import com.sirelon.marsroverphotos.utils.Logger
import com.sirelon.marsroverphotos.utils.buildTimelineMilestones
import com.sirelon.marsroverphotos.utils.calculateEarthDaysActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Rover Mission Info screen.
 */
class RoverMissionInfoViewModel(
    private val roversRepository: RoversRepository,
    private val missionRepository: MissionRepository,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    private val roverIdFlow = MutableStateFlow<Long?>(null)
    private val _stateFlow = MutableStateFlow<MissionInfoState?>(null)
    val stateFlow: StateFlow<MissionInfoState?> = _stateFlow.asStateFlow()
    private val missionFactsStatus = MutableStateFlow<Map<Long, MissionFactsStatus>>(emptyMap())

    init {
        viewModelScope.launch {
            combine(
                roverIdFlow,
                roversRepository.getRovers(),
                missionFactsStatus
            ) { roverId, rovers, factsStatus ->
                if (roverId == null) return@combine null

                val rover = rovers.find { it.id == roverId } ?: return@combine null

                // Calculate statistics
                val daysActive = rover.maxSol + 1
                val earthDaysActive = calculateEarthDaysActive(
                    landingDate = rover.landingDate,
                    maxDate = rover.maxDate
                )

                // Get cameras
                val cameras = RoverMissionData.getCamerasForRover(roverId)

                // Build timeline milestones
                val milestones = buildTimelineMilestones(rover)

                val factsUiState = MissionFactsUiState.fromStatus(factsStatus[roverId])

                // Create state
                MissionInfoState(
                    rover = rover,
                    daysActive = daysActive,
                    earthDaysActive = earthDaysActive,
                    cameras = cameras,
                    missionFacts = factsUiState.facts,
                    factsLoading = factsUiState.loading,
                    factsError = factsUiState.error,
                    timelineMilestones = milestones
                )
            }.collect { state ->
                _stateFlow.value = state
            }
        }

        viewModelScope.launch {
            roverIdFlow
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { roverId ->
                    ensureMissionFacts(roverId)
                }
        }
    }

    fun setRoverId(id: Long) {
        roverIdFlow.value = id
    }

    private suspend fun ensureMissionFacts(roverId: Long) {
        val existing = missionFactsStatus.value[roverId]
        if (existing is MissionFactsStatus.Success ||
            existing is MissionFactsStatus.Empty ||
            existing is MissionFactsStatus.Loading
        ) {
            return
        }

        missionFactsStatus.update { current ->
            current + (roverId to MissionFactsStatus.Loading)
        }

        val status = try {
            val facts = missionRepository.getRoverMissionFacts(roverId)
            if (facts == null) {
                MissionFactsStatus.Empty
            } else {
                MissionFactsStatus.Success(facts)
            }
        } catch (e: Exception) {
            Logger.e("RoverMissionInfoViewModel", e) { "Error fetching mission facts" }
            MissionFactsStatus.Error("Failed to load mission facts")
        }

        missionFactsStatus.update { current ->
            current + (roverId to status)
        }
    }

    fun trackEvent(event: String) {
        analytics.logEvent(event, emptyMap())
    }
}

private data class MissionFactsUiState(
    val facts: RoverMissionFacts?,
    val loading: Boolean,
    val error: String?
) {
    companion object {
        fun fromStatus(status: MissionFactsStatus?): MissionFactsUiState {
            return when (status) {
                is MissionFactsStatus.Success -> MissionFactsUiState(
                    facts = status.facts,
                    loading = false,
                    error = null
                )
                MissionFactsStatus.Empty -> MissionFactsUiState(
                    facts = null,
                    loading = false,
                    error = "No mission facts available"
                )
                is MissionFactsStatus.Error -> MissionFactsUiState(
                    facts = null,
                    loading = false,
                    error = status.message
                )
                MissionFactsStatus.Loading, null -> MissionFactsUiState(
                    facts = null,
                    loading = true,
                    error = null
                )
            }
        }
    }
}

private sealed interface MissionFactsStatus {
    data object Loading : MissionFactsStatus
    data object Empty : MissionFactsStatus
    data class Success(val facts: RoverMissionFacts) : MissionFactsStatus
    data class Error(val message: String) : MissionFactsStatus
}

/**
 * State for the mission info screen.
 */
data class MissionInfoState(
    val rover: Rover,
    val daysActive: Long,
    val earthDaysActive: Long,
    val cameras: List<CameraSpec>,
    val missionFacts: RoverMissionFacts?,
    val factsLoading: Boolean,
    val factsError: String?,
    val timelineMilestones: List<TimelineMilestone>
)

/**
 * Represents a milestone in the mission timeline.
 */
data class TimelineMilestone(
    val label: String,
    val date: String,
    val sol: Long?,
    val type: MilestoneType
)

/**
 * Types of milestones in the mission timeline.
 */
enum class MilestoneType {
    LAUNCH,
    LANDING,
    CURRENT,
    END
}
