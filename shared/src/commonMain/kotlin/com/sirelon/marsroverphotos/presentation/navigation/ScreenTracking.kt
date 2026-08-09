package com.sirelon.marsroverphotos.presentation.navigation

/**
 * The analytics name of every screen, in one place.
 *
 * These are both the `screen_name` on `screen_view` and the `screen` param on the actions logged
 * from a screen (e.g. `Tracker.trackFavorite`), so a funnel can join the two without a lookup
 * table. Changing a value here re-labels a screen everywhere at once — which is the point.
 */
object ScreenNames {
    const val ROVERS = "rovers"
    const val PHOTOS = "photos"
    const val PHOTO_DETAIL = "photo_detail"
    const val FAVORITE = "favorite"
    const val POPULAR = "popular"
    const val MISSION_INFO = "mission_info"
    const val ABOUT = "about"
    const val UKRAINE = "ukraine"
    const val ADMIN_PHOTOS = "admin_photos"
    const val PHOTOS_DATE_JUMP = "photos_date_jump"
    const val PHOTOS_FILTERS = "photos_filters"
    const val WHATS_NEW_DIALOG = "whats_new_dialog"
    const val ALL_VERSIONS = "all_versions"
    const val WHATS_NEW_STORY = "whats_new_story"
}

/**
 * The GA4 `screen_view` payload for a Nav3 destination.
 *
 * Value semantics matter: [AppNavigation] keys its tracking effect on this, so navigation that
 * swaps the back-stack key without changing the logical screen — e.g. a camera-filter change on
 * [AppDestination.Photos], or swiping to another photo in the viewer — doesn't re-log a view.
 */
data class ScreenView(val name: String, val params: Map<String, String> = emptyMap())

/**
 * Maps a destination to the screen view logged when it reaches the top of the back stack.
 * Exhaustive on purpose: a new [AppDestination] won't compile until it declares a screen name.
 */
fun AppDestination.toScreenView(): ScreenView = when (this) {
    AppDestination.Rovers -> ScreenView(ScreenNames.ROVERS)
    AppDestination.Favorite -> ScreenView(ScreenNames.FAVORITE)
    AppDestination.Popular -> ScreenView(ScreenNames.POPULAR)
    AppDestination.About -> ScreenView(ScreenNames.ABOUT)
    AppDestination.Ukraine -> ScreenView(ScreenNames.UKRAINE)
    AppDestination.AdminPhotos -> ScreenView(ScreenNames.ADMIN_PHOTOS)

    is AppDestination.Photos -> ScreenView(ScreenNames.PHOTOS, roverParams(roverId))
    is AppDestination.Mission -> ScreenView(ScreenNames.MISSION_INFO, roverParams(roverId))
    is AppDestination.PhotosDateJumpPicker -> ScreenView(ScreenNames.PHOTOS_DATE_JUMP, roverParams(roverId))
    is AppDestination.PhotosFilters -> ScreenView(ScreenNames.PHOTOS_FILTERS, roverParams(roverId))

    // selectedId is deliberately excluded — per-photo views are tracked by Tracker.trackSeen as
    // the user swipes, which is finer-grained than the destination ever is.
    is AppDestination.Images -> ScreenView(
        name = ScreenNames.PHOTO_DETAIL,
        params = buildMap {
            put("source", source.name.lowercase())
            if (roverId != null) putAll(roverParams(roverId))
        },
    )

    AppDestination.WhatsNewDialog -> ScreenView(ScreenNames.WHATS_NEW_DIALOG)
    AppDestination.AllVersions -> ScreenView(ScreenNames.ALL_VERSIONS)
    is AppDestination.WhatsNewStory -> ScreenView(
        name = ScreenNames.WHATS_NEW_STORY,
        params = mapOf("version" to version),
    )
}

private fun roverParams(roverId: Long): Map<String, String> = mapOf("rover_id" to roverId.toString())
