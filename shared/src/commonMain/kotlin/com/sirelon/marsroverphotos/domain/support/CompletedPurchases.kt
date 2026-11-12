package com.sirelon.marsroverphotos.domain.support

/** One store transaction as the purchase SDK reports it. */
data class StorePurchase(val transactionId: String, val productId: String, val purchaseMillis: Long)

/**
 * The purchases to report as completed, out of everything the store says this customer owns.
 *
 * A purchase counts once: [reportedIds] holds the transaction ids already reported. Purchases made
 * before [trackingSinceMillis] are history, not news — that covers what a reinstall pulls back in
 * through purchase sync, and what existing supporters owned when tracking began.
 */
fun purchasesToReport(
    owned: List<StorePurchase>,
    reportedIds: Set<String>,
    trackingSinceMillis: Long,
): List<StorePurchase> = owned.filter {
    it.transactionId !in reportedIds && it.purchaseMillis >= trackingSinceMillis
}
