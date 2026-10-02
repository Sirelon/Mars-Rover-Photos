package com.sirelon.marsroverphotos.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * When the ad-consent prompts may go up: the GDPR consent form (UMP, both platforms) and App
 * Tracking Transparency (iOS).
 *
 * Both wait for the second rover the user opens, counted across sessions, so a first launch shows
 * the rover list and a whole first visit before any sheet appears, and a one-rover-per-visit user is
 * asked on the next visit's tap. The gate opens only on a tap — never at launch off a saved count —
 * so the sheet always lands at a navigation moment. Only the prompts wait: the consent-info refresh
 * still runs at launch, so a returning user whose consent is already on record gets ads at once,
 * and the gate only matters while consent is unresolved.
 *
 * The latch itself is not persisted; the tap count lives in `AppSettings`, and `RoversViewModel`
 * decides when to [open] this. Observed by `MainActivity` on Android and, through `Main.ios.kt`,
 * by the iOS app shell.
 */
class ConsentPromptGate {
    private val _opened = MutableStateFlow(false)

    /** `true` once the prompts may be shown in this session. */
    val opened: StateFlow<Boolean> = _opened.asStateFlow()

    fun open() {
        _opened.value = true
    }
}
