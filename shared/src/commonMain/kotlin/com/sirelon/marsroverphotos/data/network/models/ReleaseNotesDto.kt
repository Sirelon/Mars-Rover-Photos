package com.sirelon.marsroverphotos.data.network.models

import com.sirelon.marsroverphotos.domain.releasenotes.Release
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * A `release-notes/<version>` Firestore document.
 *
 * Every field carries a default on purpose: GitLive's decoder throws for the whole document when a
 * non-defaulted field is absent, and these documents are hand-authored server-side, so a missing
 * `active` or an empty `changes` must degrade rather than fail.
 */
@Serializable
data class ReleaseDto(
    val version: String = "",
    val date: String = "",
    val active: Boolean = true,
    val changes: List<ReleaseChangeDto> = emptyList(),
)

@Serializable
data class ReleaseChangeDto(
    val id: String = "",
    /**
     * Material Symbols ligature name, resolved by the UI. Deliberately a [String] and not an enum:
     * GitLive's decoder throws on an enum value it does not know, which would turn a note naming a
     * newer icon into a crash instead of a fallback icon.
     */
    val icon: String = "",
    val title: String = "",
    val summary: String = "",
    val detail: String? = null,
    val imageUrl: String? = null,
)

/**
 * Maps a document to its domain model, or null when it cannot produce something renderable —
 * an unparseable date, no version, or no usable changes. Returning null keeps one malformed
 * document from taking out the rest of the list.
 */
fun ReleaseDto.toDomain(): Release? {
    if (version.isBlank()) return null
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val mapped = changes.mapNotNull { it.toDomain() }
    if (mapped.isEmpty()) return null
    return Release(
        version = version,
        date = parsedDate,
        changes = mapped.toImmutableList(),
    )
}

/** A change with no title has nothing to render, so it is dropped rather than shown blank. */
fun ReleaseChangeDto.toDomain(): Release.Change? {
    if (title.isBlank()) return null
    return Release.Change(
        id = id.ifBlank { title },
        icon = icon,
        title = title,
        summary = summary,
        detail = detail?.takeIf { it.isNotBlank() },
        imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    )
}
