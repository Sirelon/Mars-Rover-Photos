package com.sirelon.marsroverphotos.data.support

import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import com.sirelon.marsroverphotos.domain.support.SupportPurchaseResult
import com.sirelon.marsroverphotos.domain.support.SupportTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop stand-in: there is no store on the JVM, so the tiers are the dashboard's four with US
 * prices and a "purchase" flips ad-free for the process. Exists so the Support screen can be
 * driven through Compose Hot Reload without a phone.
 */
class FakeSupportRepository : SupportRepository {

    private companion object {
        const val PURCHASE_DELAY_MS = 600L
    }

    private val _isAdFree = MutableStateFlow<Boolean?>(false)
    override val isAdFree: StateFlow<Boolean?> = _isAdFree.asStateFlow()

    private val tiers = listOf(
        SupportTier(id = "support_small", title = "Coffee", price = "$0.99"),
        SupportTier(id = "support_medium", title = "Lunch", price = "$2.99"),
        SupportTier(id = "support_large", title = "Dinner", price = "$4.99"),
        SupportTier(id = "support_huge", title = "Feast", price = "$9.99"),
    )

    override suspend fun tiers(): List<SupportTier> = tiers

    override suspend fun purchase(tierId: String): SupportPurchaseResult {
        delay(PURCHASE_DELAY_MS)
        if (tiers.none { it.id == tierId }) return SupportPurchaseResult.Failure("This tier isn't available right now.", code = "TierMissing")
        _isAdFree.value = true
        return SupportPurchaseResult.Success
    }

    override suspend fun restore(): Boolean = _isAdFree.value == true
}
