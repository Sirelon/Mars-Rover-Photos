package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.domain.releasenotes.Release
import kotlinx.collections.immutable.ImmutableList

/**
 * Repository interface for the version history shown in What's New.
 */
interface ReleaseNotesRepository {
    /**
     * All published releases, newest first. Empty when the notes could not be fetched — callers
     * render an empty state rather than handling an error.
     */
    suspend fun getReleases(): ImmutableList<Release>
}
