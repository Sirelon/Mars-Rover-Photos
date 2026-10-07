package com.sirelon.marsroverphotos.data.support

import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import com.sirelon.marsroverphotos.domain.support.SupportPurchaseResult
import com.sirelon.marsroverphotos.domain.support.SupportTier
import com.sirelon.marsroverphotos.domain.support.SupportTierKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop stand-in: there is no store on the JVM, so the tiers are the dashboard's four with US
 * prices; buying any tier flips ad-free for the process. Exists so the Support screen can be
 * driven through Compose Hot Reload without a phone.
 */
class FakeSupportRepository : SupportRepository {

    private companion object {
        const val PURCHASE_DELAY_MS = 600L
    }

    private val _isAdFree = MutableStateFlow<Boolean?>(false)
    override val isAdFree: StateFlow<Boolean?> = _isAdFree.asStateFlow()

    private val tiers = listOf(
        SupportTier("tip_coffee", "support_small", SupportTierKind.TIP, "Coffee", "$0.99"),
        SupportTier("support_medium", "support_medium", SupportTierKind.AD_FREE, "Lunch", "$1.99"),
        SupportTier("support_large", "support_large", SupportTierKind.AD_FREE, "Dinner", "$4.99"),
        SupportTier("support_huge", "support_huge", SupportTierKind.AD_FREE, "Feast", "$9.99"),
    )

    override suspend fun tiers(): List<SupportTier> = tiers

    override suspend fun purchase(tierId: String): SupportPurchaseResult {
        delay(PURCHASE_DELAY_MS)
        val tier = tiers.firstOrNull { it.id == tierId }
            ?: return SupportPurchaseResult.Failure("This tier isn't available right now.", code = "TierMissing")
        _isAdFree.value = true
        return SupportPurchaseResult.Success
    }

    override suspend fun restore(): Boolean = _isAdFree.value == true
}
