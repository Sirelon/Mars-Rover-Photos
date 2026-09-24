package com.sirelon.marsroverphotos.utils

/**
 * Derives NASA Image Library size-variant URLs from a search-result href, and the fallback
 * chains callers walk when a derived variant is missing.
 *
 * Search result links end in a size token: `~thumb`, `~small`, `~medium`, or `~large` followed
 * by the file extension. The `~orig` asset (same path, same extension) is the full-res version.
 * If no known size token is found the href is returned as-is (no blind replacement) — this is
 * what keeps non-NASA-Image-Library hrefs (Curiosity/Perseverance/Ingenuity/InSight from
 * mars.nasa.gov, Viking from planetarydata.jpl.nasa.gov) completely unaffected.
 *
 * NASA does not generate every size variant for every asset — S3 answers a missing key with a
 * 403, not a 404. A curl probe across several Opportunity items missing `~large` found `~medium`
 * present for fewer than half of them, so `~medium` is a bonus rung, not a reliable single-step
 * fallback; `~orig` is the one variant every probed item had, and — for the items missing
 * `~large` — a modest 140KB-700KB, cheap enough to fall back to directly. `~thumb` was present
 * for every probed item, including the one missing `~small`.
 *
 * Examples:
 *   `…/PIA05040~small.jpg`  → `…/PIA05040~orig.jpg`
 *   `…/PIA05040~thumb.jpg`  → `…/PIA05040~orig.jpg`
 *   `…/PIA05040~large.jpg`  → `…/PIA05040~orig.jpg`
 *   `…/PIA05040.jpg`        → `…/PIA05040.jpg` (unchanged)
 */
internal fun nasaImageOrigUrl(href: String): String = nasaImageVariantUrl(href, "orig")

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
internal fun nasaImageSmallUrl(href: String): String = nasaImageVariantUrl(href, "small")

/**
 * Derives the screen-sized `~large` URL from a NASA Image Library preview href.
 *
 * Any known size token (`~thumb`, `~small`, `~medium`, `~large`) is replaced with `~large`.
 * If no known size token is found the href is returned as-is.
 *
 * The fullscreen viewer loads `~large` first — fast to decode and indistinguishable from `~orig`
 * at fit-to-screen size — then upgrades to [nasaImageOrigUrl] only when the user zooms in.
 */
internal fun nasaImageLargeUrl(href: String): String = nasaImageVariantUrl(href, "large")

/**
 * Ordered variant-URL fallback chain for the fullscreen viewer: `~large` (fast, screen-sized),
 * then `~medium` (present for some but not most items missing `~large`), then `~orig` (the
 * reliable terminal fallback). [com.sirelon.marsroverphotos.presentation.ui.NetworkImage] walks
 * this list once per load, advancing only on a load error.
 *
 * A non-NASA-Image-Library href (no known size token) yields a single-element list — the href
 * unchanged — so those photos never retry.
 */
fun nasaImageLargeFallbackUrls(href: String): List<String> =
    nasaImageFallbackChain(href, "large", "medium", "orig")

/**
 * Ordered variant-URL fallback chain for the grid thumbnail: `~small`, then `~thumb` (generated
 * for every probed item, including the one missing `~small`), then `~orig` as a last resort.
 *
 * A non-NASA-Image-Library href (no known size token) yields a single-element list — the href
 * unchanged — so those photos never retry.
 */
fun nasaImageSmallFallbackUrls(href: String): List<String> =
    nasaImageFallbackChain(href, "small", "thumb", "orig")

private fun nasaImageFallbackChain(href: String, vararg variants: String): List<String> {
    if (SIZE_TOKEN_REGEX.find(href) == null) return listOf(href)
    return variants.map { variant -> nasaImageVariantUrl(href, variant) }
}

private fun nasaImageVariantUrl(href: String, variant: String): String {
    val match = SIZE_TOKEN_REGEX.find(href) ?: return href
    val ext = match.groupValues[2]
    return href.substring(0, match.range.first) + "~$variant.$ext"
}

private val SIZE_TOKEN_REGEX = Regex(
    "~(thumb|small|medium|large)\\.(jpg|jpeg|png)$",
    RegexOption.IGNORE_CASE,
)
