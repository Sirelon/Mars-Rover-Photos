package com.sirelon.marsroverphotos.domain.releasenotes

import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate

data class Release(
    val version: String,
    val date: LocalDate,
    val changes: ImmutableList<Change>,
) {
    /**
     * One user-facing entry in a release.
     *
     * [icon] is a Material Symbols ligature name (`"rocket_launch"`, `"bug_report"`) as published on
     * fonts.google.com/icons, kept as a plain [String] so `domain/` stays free of Compose-facing
     * types. The UI resolves it with `materialSymbolOrDefault`, which falls back to a default symbol
     * for a name this build does not know — notes are authored remotely, so an icon name can arrive
     * that this version of the app has never heard of.
     */
    data class Change(
        val id: String,
        val icon: String,
        val title: String,
        val summary: String,
        val detail: String? = null,
        val imageUrl: String? = null,
    )
}
