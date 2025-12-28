package com.sirelon.marsroverphotos.feature.mission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.firebase.mission.FirestoreMission
import com.sirelon.marsroverphotos.firebase.mission.RoverMissionFacts
import com.sirelon.marsroverphotos.models.Rover
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Rover Mission Info screen.
 */
class RoverMissionInfoViewModel(app: Application) : AndroidViewModel(app) {

    private val dataManager = RoverApplication.APP.dataManger
    private val firestoreMission = FirestoreMission()

    private val roverIdFlow = MutableStateFlow<Long?>(null)
    private val _stateFlow = MutableStateFlow<MissionInfoState?>(null)
    val stateFlow: StateFlow<MissionInfoState?> = _stateFlow.asStateFlow()
    private val missionFactsStatus = MutableStateFlow<Map<Long, MissionFactsStatus>>(emptyMap())

    init {
        viewModelScope.launch {
            combine(
                roverIdFlow,
                dataManager.roverRepo.getRovers(),
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

                // Create initial state
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
            val facts = firestoreMission.getRoverMissionFacts(roverId)
            if (facts == null) {
                MissionFactsStatus.Empty
            } else {
                MissionFactsStatus.Success(facts)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching mission facts")
            MissionFactsStatus.Error("Failed to load mission facts")
        }

        missionFactsStatus.update { current ->
            current + (roverId to status)
        }
    }

    fun trackEvent(event: String) {
        dataManager.trackClick(event)
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
