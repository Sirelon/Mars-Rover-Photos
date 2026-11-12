package com.sirelon.marsroverphotos.domain.support

import kotlin.test.Test
import kotlin.test.assertEquals

class CompletedPurchasesTest {

    private val since = 1_000L

    @Test
    fun newPurchaseAfterTrackingStartIsReported() {
        val coffee = StorePurchase("t1", "tip_coffee", purchaseMillis = 2_000)
        assertEquals(listOf(coffee), purchasesToReport(listOf(coffee), emptySet(), since))
    }

    @Test
    fun alreadyReportedPurchaseIsSkipped() {
        val coffee = StorePurchase("t1", "tip_coffee", purchaseMillis = 2_000)
        assertEquals(emptyList(), purchasesToReport(listOf(coffee), setOf("t1"), since))
    }

    @Test
    fun purchaseFromBeforeTrackingStartIsHistory() {
        val restored = StorePurchase("t0", "support_medium", purchaseMillis = 500)
        assertEquals(emptyList(), purchasesToReport(listOf(restored), emptySet(), since))
    }

    @Test
    fun repeatedTipsAreReportedEachTime() {
        val first = StorePurchase("t1", "tip_coffee", purchaseMillis = 2_000)
        val second = StorePurchase("t2", "tip_coffee", purchaseMillis = 3_000)
        assertEquals(listOf(second), purchasesToReport(listOf(first, second), setOf("t1"), since))
    }
}
