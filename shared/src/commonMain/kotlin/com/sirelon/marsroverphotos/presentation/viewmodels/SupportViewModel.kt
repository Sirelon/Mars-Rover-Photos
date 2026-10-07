package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlag
import com.sirelon.marsroverphotos.domain.featureflags.FeatureFlags
import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import com.sirelon.marsroverphotos.domain.support.SupportPurchaseResult
import com.sirelon.marsroverphotos.domain.support.SupportTier
import com.sirelon.marsroverphotos.domain.support.SupportTierKind
import com.sirelon.marsroverphotos.platform.Tracker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupportUiState(
    val tiers: ImmutableList<SupportTier> = persistentListOf(),
    val isLoading: Boolean = true,
    val isAdFree: Boolean = false,
    /** The tier whose store sheet is open, so the list can show progress on it and lock the others. */
    val purchasingTierId: String? = null,
    val isRestoring: Boolean = false,
) {
    val isBusy: Boolean get() = purchasingTierId != null || isRestoring
}

/** One-shot outcomes for the snackbar; a cancelled store sheet produces none. */
sealed interface SupportUiEvent {
    /** An ad-free tier was bought (or recognised as already owned). */
    data object PurchaseSucceeded : SupportUiEvent

    /** A coffee was bought; ads are off for 30 days and the row stays buyable. */
    data object TipThanks : SupportUiEvent
    data object Restored : SupportUiEvent
    data object NothingToRestore : SupportUiEvent
    data class Failed(val message: String) : SupportUiEvent
}

/**
 * Drives the Support screen and answers two questions for the rest of the app: is the feature
 * switched on ([isFeatureEnabled], Remote Config) and does this user own a tier ([isAdFree]).
 *
 * Nav3 gives the navigation root, the About entry and the Support entry each their own instance;
 * that is fine because everything they share lives in the `single` [SupportRepository].
 */
class SupportViewModel(
    private val repository: SupportRepository,
    featureFlags: FeatureFlags,
    private val tracker: Tracker,
) : ViewModel() {

    private companion object {
        const val EVENT_PURCHASE = "support_purchase"
        const val EVENT_RESTORE = "support_restore"
    }

    val isFeatureEnabled: StateFlow<Boolean> = featureFlags.isEnabled(FeatureFlag.SUPPORT_TIERS)

    /** Owns a tier — the ad gate reads this. Unknown (cache not read yet) counts as not ad-free. */
    val isAdFree: StateFlow<Boolean> = repository.isAdFree
        .map { it == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.isAdFree.value == true)

    private val _state = MutableStateFlow(SupportUiState(isAdFree = isAdFree.value))
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    private val _events = Channel<SupportUiEvent>(Channel.BUFFERED)
    val events: Flow<SupportUiEvent> = _events.receiveAsFlow()

    private var tiersRequested = false

    init {
        viewModelScope.launch {
            isAdFree.collect { adFree -> _state.update { it.copy(isAdFree = adFree) } }
        }
    }

    /**
     * Fetches the tiers once per instance. Called by the Support screen on entry rather than in
     * `init`: the navigation root and About also hold an instance for [isAdFree] / [isFeatureEnabled],
     * and those must not cost every launch a store round-trip.
     */
    fun load() {
        if (tiersRequested) return
        tiersRequested = true
        loadTiers()
    }

    fun retry() = loadTiers()

    fun purchase(tierId: String) {
        if (_state.value.isBusy) return
        val kind = _state.value.tiers.firstOrNull { it.id == tierId }?.kind ?: SupportTierKind.AD_FREE
        _state.update { it.copy(purchasingTierId = tierId) }
        viewModelScope.launch {
            val result = repository.purchase(tierId)
            _state.update { it.copy(purchasingTierId = null) }
            trackPurchase(tierId, kind, result)
            when (result) {
                SupportPurchaseResult.Success -> _events.send(
                    if (kind == SupportTierKind.TIP) SupportUiEvent.TipThanks else SupportUiEvent.PurchaseSucceeded,
                )
                SupportPurchaseResult.Cancelled -> Unit
                is SupportPurchaseResult.Failure -> _events.send(SupportUiEvent.Failed(result.message))
            }
        }
    }

    fun restore() {
        if (_state.value.isBusy) return
        _state.update { it.copy(isRestoring = true) }
        viewModelScope.launch {
            val restored = repository.restore()
            _state.update { it.copy(isRestoring = false) }
            tracker.trackEvent(EVENT_RESTORE, mapOf("result" to if (restored) "restored" else "nothing"))
            _events.send(if (restored) SupportUiEvent.Restored else SupportUiEvent.NothingToRestore)
        }
    }

    private fun trackPurchase(tierId: String, kind: SupportTierKind, result: SupportPurchaseResult) {
        val params = buildMap {
            put("tier", tierId)
            put("kind", if (kind == SupportTierKind.TIP) "tip" else "ad_free")
            put(
                "result",
                when (result) {
                    SupportPurchaseResult.Success -> "success"
                    SupportPurchaseResult.Cancelled -> "cancelled"
                    is SupportPurchaseResult.Failure -> "failed"
                },
            )
            if (result is SupportPurchaseResult.Failure) put("code", result.code)
        }
        tracker.trackEvent(EVENT_PURCHASE, params)
    }

    private fun loadTiers() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // The repository returns empty rather than throwing when the store is unreachable, so
            // the screen renders its empty state off isLoading and offers a retry.
            val tiers = repository.tiers()
            _state.update { it.copy(tiers = tiers.toImmutableList(), isLoading = false) }
        }
    }
}
