package com.sirelon.marsroverphotos.data

/**
 * Shared marker for the photo most recently shown in the fullscreen viewer, for the list sources
 * that don't share the rover-feed pager (Favorites, Popular). The originating list reads + clears it
 * via [consume] when the viewer closes, to restore its scroll position to the photo the user
 * actually swiped to — not the one they tapped. (The rover feed uses RoverFeedPager's own marker.)
 *
 * Single instance (Koin): written by the viewer VM, read by the list VMs.
 */
class LastViewedPhotoStore {
    private var id: String? = null

    fun set(value: String) {
        id = value
    }

    /** Return the last-viewed id and clear it, so it's applied at most once per viewer visit. */
    fun consume(): String? = id.also { id = null }
}
