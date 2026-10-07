package com.sirelon.marsroverphotos.domain.repositories

import com.sirelon.marsroverphotos.domain.support.SupportPurchaseResult
import com.sirelon.marsroverphotos.domain.support.SupportTier
import kotlinx.coroutines.flow.StateFlow

/**
 * Store billing for the "Support the developer" tiers and the `ad_free` entitlement they grant.
 *
 * Every tier grants ad-free. The non-consumable tiers do it for good, stay with the store account
 * and restore on a new device; the consumable Coffee tip does it for 30 days on this install. Bound as a Koin `single`: the ad gate at
 * the navigation root, the About row and the Support screen all read the same [isAdFree].
 */
interface SupportRepository {
    /**
     * Whether any tier is owned. `null` until the billing SDK's cached state has been read (or the
     * read failed), then follows purchases and restores. Callers that must not show ads to a
     * supporter wait for the first non-null value with their own bound.
     */
    val isAdFree: StateFlow<Boolean?>

    /** The purchasable tiers in display order; empty when the store could not be reached. */
    suspend fun tiers(): List<SupportTier>

    suspend fun purchase(tierId: String): SupportPurchaseResult

    /** Re-checks the store account for earlier purchases; true when ad-free is active afterwards. */
    suspend fun restore(): Boolean
}
