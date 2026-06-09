package com.sirelon.marsroverphotos.utils

/**
 * KMP-safe thousands-separated formatter.
 *
 * Examples: 1867 → "1,867", 74525 → "74,525", 320999 → "320,999".
 */
fun formatThousands(value: Long): String {
    if (value == 0L) return "0"
    val negative = value < 0
    // Use Long to safely negate Long.MIN_VALUE-adjacent values via string handling.
    var remaining = if (negative) -value else value
    val builder = StringBuilder()
    var digitsWritten = 0
    while (remaining > 0) {
        val digit = (remaining % 10).toInt()
        if (digitsWritten > 0 && digitsWritten % 3 == 0) {
            builder.append(',')
        }
        builder.append(('0' + digit))
        remaining /= 10
        digitsWritten++
    }
    if (negative) builder.append('-')
    return builder.reverse().toString()
}

fun formatThousands(value: Int): String = formatThousands(value.toLong())

/**
 * KMP-safe compact/abbreviated formatter mirroring the design's `fmtK`:
 * `<1000` → as-is; `<1,000,000` → "X.YK" (1 decimal below 10,000, else 0 decimal);
 * `≥1,000,000` → "X.YM".
 *
 * Examples: 999 → "999", 1505 → "1.5K", 133811 → "134K", 1_200_000 → "1.2M".
 */
fun formatCompact(value: Long): String {
    if (value < 0) return "-" + formatCompact(-value)
    return when {
        value < 1_000L -> value.toString()
        value < 1_000_000L ->
            if (value < 10_000L) decimal1(value, 1_000L) + "K" else roundDiv(value, 1_000L).toString() + "K"
        else -> decimal1(value, 1_000_000L) + "M"
    }
}

fun formatCompact(value: Int): String = formatCompact(value.toLong())

/** Rounds value/divisor to the nearest whole number (half-up); value/divisor assumed non-negative. */
private fun roundDiv(value: Long, divisor: Long): Long = (value + divisor / 2) / divisor

/** Formats value/divisor with exactly one decimal place, half-up rounded, no locale dependency. */
private fun decimal1(value: Long, divisor: Long): String {
    val tenths = roundDiv(value * 10, divisor)
    return "${tenths / 10}.${tenths % 10}"
}
