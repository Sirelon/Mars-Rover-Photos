package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class RoversViewModel(
    roversRepository: RoversRepository,
    private val tracker: Tracker
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

    fun onRoverClicked(rover: Rover) {
        tracker.trackClick("click_rover_${rover.name}")
    }

    fun onMissionInfoClicked(rover: Rover) {
        tracker.trackClick("click_mission_info_${rover.name}")
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
