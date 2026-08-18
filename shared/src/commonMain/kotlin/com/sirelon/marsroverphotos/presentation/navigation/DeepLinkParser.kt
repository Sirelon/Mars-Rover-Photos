package com.sirelon.marsroverphotos.presentation.navigation

/** Host of the app's HTTPS links, as declared in the Android manifest's app-link intent filter. */
private const val WEB_HOST = "marsroverphotos.app"

/**
 * Parses the app's public deep-link URIs into a [DeepLink].
 *
 * Shared by every surface that can hand the app a URI — Android intent filters, the iOS
 * `onOpenURL` bridge and notification taps — so a new link form is added in one place rather than
 * once per platform.
 *
 * Supported:
 * ```
 * marsrover://rover/{roverId}
 * marsrover://photo/{photoId}
 * https://marsroverphotos.app/rover/{roverId}
 * https://marsroverphotos.app/photo/{photoId}
 * ```
 *
 * Deliberately plain string handling rather than `android.net.Uri`/`NSURL` so the one
 * implementation is shared and unit-testable. The custom scheme puts the target in the URI's
 * *host* (`marsrover://rover/5`) while the HTTPS form puts it in the first path segment
 * (`marsroverphotos.app/rover/5`), so both are normalised to the same `kind`/`id` pair below.
 *
 * @return the matching [DeepLink], or null when [uri] is malformed or unrecognised.
 */
fun parseDeepLink(uri: String): DeepLink? {
    val afterScheme = uri.substringAfter("://", missingDelimiterValue = "")
    if (afterScheme.isEmpty()) return null

    // None of the supported forms carry parameters; drop any so they can't leak into an id.
    val segments = afterScheme
        .substringBefore('?')
        .substringBefore('#')
        .split('/')
        .filter { it.isNotBlank() }
    if (segments.isEmpty()) return null

    val host = segments[0].lowercase()
    val rest = segments.drop(1)

    val kind: String
    val id: String?
    if (host == WEB_HOST) {
        if (rest.isEmpty()) return null
        kind = rest[0].lowercase()
        id = rest.getOrNull(1)
    } else {
        kind = host
        id = rest.firstOrNull()
    }

    return when (kind) {
        "rover" -> id?.toLongOrNull()?.let { DeepLink.Rover(it) }
        "photo" -> id?.toLongOrNull()?.let { DeepLink.Photo(it) }
        else -> null
    }
}
