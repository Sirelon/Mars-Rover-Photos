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
