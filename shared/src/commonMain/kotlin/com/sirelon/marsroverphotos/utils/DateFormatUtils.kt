package com.sirelon.marsroverphotos.utils

import kotlinx.datetime.LocalDate

/**
 * Formats an ISO-8601 date string "yyyy-MM-dd" to a human-readable form "May 26, 2026".
 * Returns the original string unchanged if parsing fails.
 */
fun formatDisplayDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        "$month ${date.day}, ${date.year}"
    } catch (_: Exception) {
        isoDate
    }
}
