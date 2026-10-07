package com.sirelon.marsroverphotos.domain.support

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** How long one Coffee tip keeps ads off. */
val TIP_AD_FREE_PERIOD: Duration = 30.days

/**
 * When the ad-free window bought with Coffee tips ends, in epoch millis, or `null` with no tips.
 *
 * Tips stack: a tip bought while a window is still open extends it by another [period] from its end;
 * a tip bought after it closed starts a fresh window from its purchase time.
 */
fun tipAdFreeUntil(purchaseMillis: List<Long>, period: Duration = TIP_AD_FREE_PERIOD): Long? {
    val periodMillis = period.inWholeMilliseconds
    var end: Long? = null
    for (purchase in purchaseMillis.sorted()) {
        end = maxOf(purchase, end ?: purchase) + periodMillis
    }
    return end
}
