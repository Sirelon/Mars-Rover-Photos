package com.sirelon.marsroverphotos.presentation.viewmodels

import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlag
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlags
import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import com.sirelon.marsroverphotos.domain.support.SupportPurchaseResult
import com.sirelon.marsroverphotos.domain.support.SupportTier
import com.sirelon.marsroverphotos.domain.support.SupportTierKind
import com.sirelon.marsroverphotos.platform.Tracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val LUNCH = SupportTier("support_medium", "support_medium", SupportTierKind.AD_FREE, "Lunch", "$1.99")
private val COFFEE = SupportTier("tip_coffee", "support_small", SupportTierKind.TIP, "Coffee", "$0.99")

private class FakeSupportRepository(
    private val tiers: List<SupportTier> = listOf(LUNCH),
    var purchaseResult: SupportPurchaseResult = SupportPurchaseResult.Success,
    var restoreResult: Boolean = false,
    initialAdFree: Boolean? = false,
) : SupportRepository {
    private val _isAdFree = MutableStateFlow(initialAdFree)
    override val isAdFree: StateFlow<Boolean?> = _isAdFree.asStateFlow()
    val purchased = mutableListOf<String>()

    /** Simulates the billing SDK's cached CustomerInfo landing after the ViewModel exists. */
    fun setAdFree(value: Boolean?) { _isAdFree.value = value }

    override suspend fun tiers(): List<SupportTier> = tiers

    override suspend fun purchase(tierId: String): SupportPurchaseResult {
        purchased += tierId
        if (purchaseResult == SupportPurchaseResult.Success) _isAdFree.value = true
        return purchaseResult
    }

    override suspend fun restore(): Boolean {
        if (restoreResult) _isAdFree.value = true
        return restoreResult
    }
}

private class CountingSupportRepository : SupportRepository {
    var tierCalls = 0
    override val isAdFree: StateFlow<Boolean?> = MutableStateFlow(false)
    override suspend fun tiers(): List<SupportTier> { tierCalls++; return emptyList() }
    override suspend fun purchase(tierId: String): SupportPurchaseResult = SupportPurchaseResult.Cancelled
    override suspend fun restore(): Boolean = false
}

private class FakeFeatureFlags(enabled: Boolean) : FeatureFlags {
    private val flow = MutableStateFlow(enabled)
    override fun isEnabled(flag: FeatureFlag): StateFlow<Boolean> = flow
}

/** Records [trackEvent] calls; every other [Tracker] method is an unused no-op stub. */
private class EventRecordingTracker : Tracker {
    val events = mutableListOf<Pair<String, Map<String, String>>>()

    override fun trackClick(event: String) = Unit
    override fun trackEvent(event: String, params: Map<String, String>) { events += event to params }
    override fun trackScreen(screenName: String, params: Map<String, String>) = Unit
    override fun trackFeedError(screen: String, error: Throwable, params: Map<String, String>) = Unit
    override fun trackFeedEmpty(screen: String, params: Map<String, String>) = Unit
    override fun trackFeedLoaded(rover: String, loadMs: Long) = Unit
    override fun trackAdImpression(
        adSource: String,
        adFormat: String,
        adUnitName: String,
        value: Double,
        currencyCode: String,
        precision: String,
    ) = Unit
    override fun trackFavorite(photo: MarsImage, from: String, fav: Boolean) = Unit
    override fun trackSeen(photo: MarsImage) = Unit
    override fun trackScale(photo: MarsImage) = Unit
    override fun trackSave(photo: MarsImage) = Unit
    override fun trackShare(photo: MarsImage, packageName: String?) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class SupportViewModelTest {

    private val tracker = EventRecordingTracker()

    private fun viewModel(repository: SupportRepository, flagEnabled: Boolean = true) =
        SupportViewModel(repository, FakeFeatureFlags(flagEnabled), tracker).also { it.load() }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsTiersAndLeavesLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeSupportRepository())
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("support_medium"), vm.state.value.tiers.map { it.id })
        assertTrue(vm.isFeatureEnabled.value)
    }

    @Test
    fun successfulPurchaseFlipsAdFreeAndEmitsEvent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeSupportRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.purchase("support_medium")
        advanceUntilIdle()

        assertEquals(listOf("support_medium"), repo.purchased)
        assertTrue(vm.isAdFree.value)
        assertTrue(vm.state.value.isAdFree)
        assertNull(vm.state.value.purchasingTierId)
        assertEquals(SupportUiEvent.PurchaseSucceeded, vm.collectOne())
        assertEquals(
            "support_purchase" to mapOf("tier" to "support_medium", "kind" to "ad_free", "result" to "success"),
            tracker.events.single(),
        )
    }

    @Test
    fun tipThanksAndUnlocksAdFree() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeSupportRepository(tiers = listOf(COFFEE, LUNCH))
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.purchase("tip_coffee")
        advanceUntilIdle()

        assertEquals(listOf("tip_coffee"), repo.purchased)
        assertTrue(vm.isAdFree.value)
        assertEquals(SupportUiEvent.TipThanks, vm.collectOne())
        assertEquals("tip", tracker.events.single().second["kind"])
    }

    @Test
    fun cancelledPurchaseIsSilent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeSupportRepository(purchaseResult = SupportPurchaseResult.Cancelled))
        advanceUntilIdle()

        vm.purchase("support_medium")
        advanceUntilIdle()

        assertFalse(vm.isAdFree.value)
        assertNull(vm.collectOneOrNull())
        assertEquals("cancelled", tracker.events.single().second["result"])
    }

    @Test
    fun failedPurchaseReportsMessage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeSupportRepository(purchaseResult = SupportPurchaseResult.Failure("Store is down", code = "StoreProblemError")))
        advanceUntilIdle()

        vm.purchase("support_medium")
        advanceUntilIdle()

        assertEquals(SupportUiEvent.Failed("Store is down"), vm.collectOne())
        assertEquals("StoreProblemError", tracker.events.single().second["code"])
    }

    @Test
    fun secondPurchaseWhileBusyIsIgnored() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeSupportRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.purchase("support_medium")
        vm.purchase("support_medium")
        advanceUntilIdle()

        assertEquals(1, repo.purchased.size)
    }

    @Test
    fun restoreReportsRestoredOrNothing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val nothing = viewModel(FakeSupportRepository(restoreResult = false))
        advanceUntilIdle()
        nothing.restore()
        advanceUntilIdle()
        assertEquals(SupportUiEvent.NothingToRestore, nothing.collectOne())
        assertFalse(nothing.isAdFree.value)

        val restored = viewModel(FakeSupportRepository(restoreResult = true))
        advanceUntilIdle()
        restored.restore()
        advanceUntilIdle()
        assertEquals(SupportUiEvent.Restored, restored.collectOne())
        assertTrue(restored.isAdFree.value)
    }

    @Test
    fun tiersAreNotFetchedUntilTheScreenAsks() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = CountingSupportRepository()
        val vm = SupportViewModel(repo, FakeFeatureFlags(true), tracker)
        advanceUntilIdle()
        assertEquals(0, repo.tierCalls)

        vm.load()
        vm.load()
        advanceUntilIdle()
        assertEquals(1, repo.tierCalls)
    }

    @Test
    fun unknownAdFreeStateCountsAsNotAdFree() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = viewModel(FakeSupportRepository(initialAdFree = null))
        advanceUntilIdle()

        assertFalse(vm.isAdFree.value)
    }

    @Test
    fun adFreeArrivingAfterConstructionFlipsBothFlows() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeSupportRepository(initialAdFree = null)
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.state.value.isAdFree)

        repo.setAdFree(true)
        advanceUntilIdle()

        assertTrue(vm.isAdFree.value)
        assertTrue(vm.state.value.isAdFree)
    }

    @Test
    fun restoreIsIgnoredWhileAPurchaseIsInFlight() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeSupportRepository(restoreResult = true)
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.purchase("support_medium")
        vm.restore()
        advanceUntilIdle()

        assertEquals(SupportUiEvent.PurchaseSucceeded, vm.collectOne())
        assertNull(vm.collectOneOrNull())
    }
}

private object StopCollecting : RuntimeException()

/** The buffered event channel holds the outcome until something collects it. */
private suspend fun SupportViewModel.collectOne(): SupportUiEvent =
    collectOneOrNull() ?: error("expected an event")

private suspend fun SupportViewModel.collectOneOrNull(): SupportUiEvent? {
    var received: SupportUiEvent? = null
    withTimeoutOrNull(50) {
        try {
            events.collect { received = it; throw StopCollecting }
        } catch (_: StopCollecting) {
        }
    }
    return received
}
