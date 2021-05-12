package com.sirelon.marsroverphotos.feature.billing

import androidx.fragment.app.FragmentActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

/**
 * Created on 12.05.2021 22:38 for Mars-Rover-Photos.
 */
object BillingHelper {

    fun init(activity: FragmentActivity) {
        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                // To be implemented in a later section.
            }

        var billingClient = BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
    }

}