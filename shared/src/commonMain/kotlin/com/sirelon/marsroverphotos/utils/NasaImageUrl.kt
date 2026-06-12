package com.sirelon.marsroverphotos.utils

/**
 * Derives the full-resolution URL from a NASA Image Library preview href.
 *
 * Search result links end in a size token: `~thumb`, `~small`, `~medium`, or `~large` followed
 * by the file extension. The `~orig` asset (same path, same extension) is the full-res version.
 * If no known size token is found the href is returned as-is (no blind replacement).
 *
 * Examples:
 *   `…/PIA05040~small.jpg`  → `…/PIA05040~orig.jpg`
 *   `…/PIA05040~thumb.jpg`  → `…/PIA05040~orig.jpg`
 *   `…/PIA05040~large.jpg`  → `…/PIA05040~orig.jpg`
 *   `…/PIA05040.jpg`        → `…/PIA05040.jpg` (unchanged)
 */
internal fun nasaImageOrigUrl(href: String): String {
    val match = SIZE_TOKEN_REGEX.find(href) ?: return href
    val ext = match.groupValues[2]
    return href.substring(0, match.range.first) + "~orig.$ext"
}

/**
 * Derives the lightweight `~small` URL from a NASA Image Library preview href.
 *
 * Any known size token (`~thumb`, `~small`, `~medium`, `~large`) is replaced with `~small`.
 * If no known size token is found the href is returned as-is.
 *
 * Storing `~small` in [com.sirelon.marsroverphotos.data.database.entities.MarsImage.imageUrl]
 * keeps the grid fast; call [nasaImageOrigUrl] on the stored value to recover the full-res URL
 * for the detail viewer.
 */
internal fun nasaImageSmallUrl(href: String): String {
    val match = SIZE_TOKEN_REGEX.find(href) ?: return href
    val ext = match.groupValues[2]
    return href.substring(0, match.range.first) + "~small.$ext"
}

private val SIZE_TOKEN_REGEX = Regex(
    "~(thumb|small|medium|large)\\.(jpg|jpeg|png)$",
    RegexOption.IGNORE_CASE,
)
