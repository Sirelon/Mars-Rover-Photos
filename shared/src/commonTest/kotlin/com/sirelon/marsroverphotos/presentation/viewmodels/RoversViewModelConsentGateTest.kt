package com.sirelon.marsroverphotos.presentation.viewmodels

import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.repositories.RoversRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.ConsentPromptGate
import com.sirelon.marsroverphotos.platform.FakePreferences
import com.sirelon.marsroverphotos.platform.RecordingTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val curiosity = Rover(
    id = 5L,
    name = "Curiosity",
    drawableName = "",
    landingDate = "",
    launchDate = "",
    status = "active",
    maxSol = 0L,
    maxDate = "",
    totalPhotos = 0,
)

private class FakeRoversRepository : RoversRepository {
    override fun initialize() = Unit
    override fun getRovers(): Flow<List<Rover>> = flowOf(listOf(curiosity))
    override suspend fun loadRoverById(id: Long): Rover? = null
    override suspend fun updateRoverCountPhotos(roverId: Long, photos: Long) = Unit
}

/**
 * The consent prompts key off this one signal, so what matters is that it cannot fire before the
 * user's second rover, that taps from earlier sessions count, and that only a tap — never a launch
 * with a saved count — opens it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoversViewModelConsentGateTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(gate: ConsentPromptGate, settings: AppSettings) =
        RoversViewModel(FakeRoversRepository(), RecordingTracker(), settings, gate)

    @Test
    fun staysClosedThroughTheFirstEverRoverTap() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = ConsentPromptGate()
        val settings = AppSettings(FakePreferences())
        val viewModel = viewModel(gate, settings)

        viewModel.onRoverClicked(curiosity)

        assertFalse(gate.opened.value)
        assertEquals(1, settings.roverOpenCount)
    }

    @Test
    fun opensOnTheSecondRoverTapAndStaysOpen() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = ConsentPromptGate()
        val viewModel = viewModel(gate, AppSettings(FakePreferences()))

        viewModel.onRoverClicked(curiosity)
        viewModel.onRoverClicked(curiosity)
        assertTrue(gate.opened.value)

        viewModel.onRoverClicked(curiosity)
        assertTrue(gate.opened.value)
    }

    @Test
    fun aTapFromAnEarlierSessionCountsButOnlyATapOpensTheGate() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        // One rover opened in a previous session, then a fresh launch: the saved count alone must
        // not open the gate, or a returning user would meet the sheet at launch instead of on a tap.
        val settings = AppSettings(FakePreferences()).apply { recordRoverOpened() }
        val gate = ConsentPromptGate()
        val viewModel = viewModel(gate, settings)
        assertFalse(gate.opened.value)

        viewModel.onRoverClicked(curiosity)

        assertTrue(gate.opened.value)
    }
}
