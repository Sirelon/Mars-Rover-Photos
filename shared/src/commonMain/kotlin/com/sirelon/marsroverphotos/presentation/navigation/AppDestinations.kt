package com.sirelon.marsroverphotos.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation destinations for the app.
 */
@Serializable
sealed interface AppDestination : NavKey {
    sealed interface DialogDestination : AppDestination

    @Serializable
    enum class ImagesSource {
        DIRECT_IDS,
        FAVORITES,
        POPULAR,

        /** Infinite rover feed — the detail pager shares the list's paged stream. */
        ROVER_FEED,
    }

    @Serializable
    data object Rovers : AppDestination

    @Serializable
    data class Photos(val roverId: Long, val camera: String? = null) : AppDestination

    @Serializable
    data class Images(
        val photoIds: List<String> = emptyList(),
        val selectedId: String? = null,
        val source: ImagesSource = ImagesSource.DIRECT_IDS,
        /** Set for [ImagesSource.ROVER_FEED] — the rover whose shared feed the pager scrolls. */
        val roverId: Long? = null,
        /** Legacy single-camera filter for [ImagesSource.ROVER_FEED]. */
        val camera: String? = null,
        /** Active camera filters for [ImagesSource.ROVER_FEED] when opened from Photos. */
        val cameras: Set<String> = emptySet(),
    ) : AppDestination

    @Serializable
    data object Favorite : AppDestination

    @Serializable
    data object Popular : AppDestination

    @Serializable
    data class Mission(val roverId: Long) : AppDestination

    @Serializable
    data object About : AppDestination

    @Serializable
    data object Ukraine : AppDestination

    /** Dialog destination: combined sol+earth date or page jump picker. Shares PhotosViewModel with [Photos]. */
    @Serializable
    data class PhotosDateJumpPicker(val roverId: Long) : DialogDestination

    /** Dialog destination: filters sheet (camera, date, appearance). Shares PhotosViewModel with [Photos]. */
    @Serializable
    data class PhotosFilters(val roverId: Long) : DialogDestination
}
