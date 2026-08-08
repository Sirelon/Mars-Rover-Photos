package com.sirelon.marsroverphotos.domain.releasenotes

import kotlinx.datetime.LocalDate

enum class ChangeType {
    MISSION_INFO, MULTIPLATFORM, OFFLINE_CACHE,
    FAVORITES, POPULAR, REDESIGN,
}

data class Release(
    val version: String,
    val date: LocalDate,
    val changes: List<Change>,
) {
    data class Change(
        val id: String,
        val type: ChangeType,
        val title: String,
        val summary: String,
        val detail: String? = null,
        val actionLabel: String? = null,
    )
}
