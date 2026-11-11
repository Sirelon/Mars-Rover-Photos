package com.sirelon.marsroverphotos.utils

import kotlinx.datetime.LocalDate

/**
 * Formats an ISO-8601 date "yyyy-MM-dd" to a human-readable form "May 26, 2026".
 * A full timestamp ("2026-11-10T15:15:47.000Z") is accepted too: the raw-image feeds report the
 * moment a photo was taken, and only its date part is shown.
 * Returns the original string unchanged if parsing fails.
 */
fun formatDisplayDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate.substringBefore('T'))
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        "$month ${date.day}, ${date.year}"
    } catch (_: Exception) {
        isoDate
    }
}
