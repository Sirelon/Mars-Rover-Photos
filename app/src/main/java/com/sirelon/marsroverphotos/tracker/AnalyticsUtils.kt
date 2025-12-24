package com.sirelon.marsroverphotos.tracker

import android.os.Bundle

private const val MAX_ANALYTICS_NAME_LENGTH = 40

internal fun normalizeClickEventName(rawEvent: String): String {
    val trimmed = rawEvent.trim()
    if (trimmed.isEmpty()) return "click"
    return if (trimmed.startsWith("click_", ignoreCase = true)) {
        trimmed
    } else {
        "click_$trimmed"
    }
}

internal fun normalizeEventName(rawEvent: String): String =
    normalizeAnalyticsName(rawEvent, "event")

internal fun normalizeParamName(rawParam: String): String =
    normalizeAnalyticsName(rawParam, "param")

internal fun Map<String, Any?>.toAnalyticsBundle(): Bundle {
    val bundle = Bundle()
    forEach { (key, value) ->
        val normalizedKey = normalizeParamName(key)
        when (value) {
            null -> Unit
            is String -> bundle.putString(normalizedKey, value)
            is CharSequence -> bundle.putString(normalizedKey, value.toString())
            is Boolean -> bundle.putLong(normalizedKey, if (value) 1L else 0L)
            is Byte -> bundle.putLong(normalizedKey, value.toLong())
            is Short -> bundle.putLong(normalizedKey, value.toLong())
            is Int -> bundle.putLong(normalizedKey, value.toLong())
            is Long -> bundle.putLong(normalizedKey, value)
            is Float -> bundle.putDouble(normalizedKey, value.toDouble())
            is Double -> bundle.putDouble(normalizedKey, value)
            is Number -> bundle.putDouble(normalizedKey, value.toDouble())
            else -> bundle.putString(normalizedKey, value.toString())
        }
    }
    return bundle
}

private fun normalizeAnalyticsName(rawValue: String, fallbackPrefix: String): String {
    val normalized = rawValue
        .trim()
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .lowercase()
        .replace(Regex("[^a-z0-9_]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifEmpty { "${fallbackPrefix}_unknown" }

    val withPrefix = if (normalized.first().isLetter()) {
        normalized
    } else {
        "${fallbackPrefix}_$normalized"
    }

    return withPrefix.take(MAX_ANALYTICS_NAME_LENGTH)
}
