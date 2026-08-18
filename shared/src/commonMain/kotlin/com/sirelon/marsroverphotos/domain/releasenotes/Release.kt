package com.sirelon.marsroverphotos.domain.releasenotes

import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate

enum class ChangeType {
    MISSION_INFO, MULTIPLATFORM, OFFLINE_CACHE,
    FAVORITES, POPULAR, REDESIGN,
    BROWSING, BUG_FIX, FACTS, INITIAL_RELEASE, NAVIGATION,
    NEW_ROVER, PHOTO_VIEWER, SAVE_PHOTO, THEMING, UKRAINE, WIDGET,
}

data class Release(
    val version: String,
    val date: LocalDate,
    val changes: ImmutableList<Change>,
) {
    data class Change(
        val id: String,
        val type: ChangeType,
        val title: String,
        val summary: String,
        val detail: String? = null,
        val imageUrl: String? = null,
    )
}
