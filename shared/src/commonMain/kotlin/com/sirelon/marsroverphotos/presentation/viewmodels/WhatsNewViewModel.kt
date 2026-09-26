package com.sirelon.marsroverphotos.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.domain.repositories.ReleaseNotesRepository
import com.sirelon.marsroverphotos.domain.settings.AppSettings
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.platform.Tracker
import com.sirelon.marsroverphotos.presentation.review.ReviewPrompter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one release card the Rovers list may show: at most one per launch, never on the first launch
 * of a fresh install, and gone for good once opened or dismissed.
 */
sealed interface WhatsNewCard {
    val release: Release

    /** A newer store-approved release than the installed build. Tapping it goes to the store. */
    data class UpdateAvailable(override val release: Release) : WhatsNewCard

    /** The notes for the build the user just updated to. Tapping it opens the story. */
    data class Highlights(override val release: Release) : WhatsNewCard
}

data class WhatsNewUiState(
    val releases: ImmutableList<Release> = persistentListOf(),
    val latestRelease: Release? = null,
    val isLoading: Boolean = true,
    val card: WhatsNewCard? = null,
)

/** Numeric `major.minor.patch` comparison — versions in this repo are always that shape (see AGENTS.md › Versioning), so a plain dotted split is enough; no need for a general semver parser. */
private fun compareVersions(a: String, b: String): Int {
    val partsA = a.split(".")
    val partsB = b.split(".")
    for (i in 0 until maxOf(partsA.size, partsB.size)) {
        val cmp = (partsA.getOrNull(i)?.toIntOrNull() ?: 0).compareTo(partsB.getOrNull(i)?.toIntOrNull() ?: 0)
        if (cmp != 0) return cmp
    }
    return 0
}

class WhatsNewViewModel(
    private val releaseNotesRepository: ReleaseNotesRepository,
    private val appSettings: AppSettings,
    private val tracker: Tracker,
    private val reviewPrompter: ReviewPrompter,
) : ViewModel() {

    private val _state = MutableStateFlow(WhatsNewUiState())
    val state: StateFlow<WhatsNewUiState> = _state.asStateFlow()

    /** Cards this instance has already reported as shown — a recomposition must not re-log one. */
    private val shownCards = mutableSetOf<WhatsNewCard>()

    init {
        viewModelScope.launch {
            val releases = releaseNotesRepository.getReleases()
            // The repository already dropped anything not "available" in the store, so the highest
            // version left here is always something the user could actually update to — never a
            // build pending approval.
            val latest = releases.maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
            _state.update {
                it.copy(
                    releases = releases,
                    latestRelease = latest,
                    // Also cleared on the failure path — the repository returns an empty list rather
                    // than throwing, so nothing can leave this stuck loading forever.
                    isLoading = false,
                    card = cardFor(releases, latest),
                )
            }
        }
    }

    /** The release matching [version], or null when the notes carry nothing for it. */
    fun releaseFor(version: String): Release? =
        _state.value.releases.firstOrNull { it.version == version }

    /**
     * Which card, if any, the Rovers list shows this launch.
     *
     * Reads [AppSettings] live rather than caching a flag: Nav3 gives every entry its own
     * ViewModelStore, so the instance that renders the card (Rovers) is not the one behind the story
     * or the version list, and the persisted markers are the only state they share.
     *
     * Rules, in priority order:
     * - Never on the first launch. A fresh install has nothing to call "new", and the first launch is
     *   already the busiest moment the app has (consent prompts). Decided by [AppSettings.launchCount].
     * - [WhatsNewCard.UpdateAvailable] when the newest `active` release is newer than the installed
     *   build and that version's card has not been dismissed. This is what tells a user on an old
     *   build to go get the new one — deliberately not "is there a note for the version I run".
     * - Otherwise [WhatsNewCard.Highlights] when the installed build has notes the user has not
     *   acknowledged and this is not the version they first installed: a fresh install of 5.3.0 is
     *   not told what is new in 5.3.0, an update from 5.2.0 is.
     */
    private fun cardFor(releases: List<Release>, latest: Release?): WhatsNewCard? {
        if (appSettings.launchCount < MIN_LAUNCHES_FOR_CARD) return null
        val installed = BuildInfo.versionName
        // Desktop never resolves a real version (KoinInit.desktop.kt reads an "app.version" system
        // property nothing ever sets), so BuildInfo.versionName is always the literal "unknown"
        // there. compareVersions would otherwise read every one of its segments as 0 and treat any
        // published release as an update, with nowhere sensible to send a desktop user.
        if (installed.substringBefore('.').toIntOrNull() == null) return null
        if (latest != null &&
            compareVersions(latest.version, installed) > 0 &&
            appSettings.dismissedUpdateVersion != latest.version
        ) {
            return WhatsNewCard.UpdateAvailable(latest)
        }
        val current = releases.firstOrNull { it.version == installed } ?: return null
        // The story has no pages for a release without changes, so there would be nothing to open.
        if (current.changes.isEmpty()) return null
        if (appSettings.lastSeenVersion == installed) return null
        val freshInstall = appSettings.firstLaunchVersion == installed && appSettings.lastSeenVersion == null
        if (freshInstall) return null
        return WhatsNewCard.Highlights(current)
    }

    /**
     * The card reached the screen. Logged once per card, and noted with [ReviewPrompter] so the
     * store-review prompt stays quiet for the rest of this session — one nudge at a time.
     */
    fun onCardShown(card: WhatsNewCard) {
        if (!shownCards.add(card)) return
        reviewPrompter.onWhatsNewCardShown()
        tracker.trackEvent("whats_new_card_shown", card.params())
    }

    fun onCardOpened(card: WhatsNewCard) {
        acknowledge(card)
        tracker.trackEvent("whats_new_card_opened", card.params())
    }

    fun onCardDismissed(card: WhatsNewCard) {
        acknowledge(card)
        tracker.trackEvent("whats_new_card_dismissed", card.params())
    }

    /**
     * Records the card as dealt with and takes it off the screen. Whatever card would come next
     * waits for the next launch rather than sliding in right away: one nudge per session.
     */
    private fun acknowledge(card: WhatsNewCard) {
        when (card) {
            is WhatsNewCard.UpdateAvailable -> appSettings.dismissedUpdateVersion = card.release.version
            is WhatsNewCard.Highlights -> appSettings.lastSeenVersion = card.release.version
        }
        _state.update { it.copy(card = null) }
    }

    private fun WhatsNewCard.params(): Map<String, String> = mapOf(
        "kind" to when (this) {
            is WhatsNewCard.UpdateAvailable -> "update"
            is WhatsNewCard.Highlights -> "highlights"
        },
        "version" to release.version,
    )

    private companion object {
        /** The card first appears on the second launch. */
        const val MIN_LAUNCHES_FOR_CARD = 2
    }
}
