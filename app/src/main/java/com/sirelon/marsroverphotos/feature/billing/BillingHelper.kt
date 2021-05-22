package com.sirelon.marsroverphotos.feature.billing

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.querySkuDetails
import com.sirelon.marsroverphotos.extensions.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created on 12.05.2021 22:38 for Mars-Rover-Photos.
 */
object BillingHelper {

    private var billingClient: BillingClient? = null
    private var adRemover: SkuDetails? = null

    fun init(activity: FragmentActivity) {
        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                // To be implemented in a later section.
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(activity, purchase)
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    // Handle an error caused by a user cancelling the purchase flow.
                } else {
                    // Handle any other error codes.
                }
            }

        billingClient = BillingClient.newBuilder(activity)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                billingResult.logD()
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    activity.lifecycleScope.launch {
                        querySkuDetails()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                "onBillingServiceDisconnected".logD()
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun handlePurchase(activity: FragmentActivity, purchase: Purchase?) {
        Log.i("Sirelo", "handlePurchase() called with: activity = $activity, purchase = $purchase");
        val purchase = purchase ?: return
        val client = billingClient ?: return
        activity.lifecycleScope.launch {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                    val ackPurchaseResult = withContext(Dispatchers.IO) {
                        client.acknowledgePurchase(acknowledgePurchaseParams.build())
                    }
                    ackPurchaseResult.debugMessage.logD()
                    ackPurchaseResult.responseCode.logD()
                }
            }

        }
    }

    suspend fun querySkuDetails() {
        val billingClient = billingClient ?: return
        val skuList = ArrayList<String>()
        val sku = "ad_remover"
        skuList.add(sku)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        // leverage querySkuDetails Kotlin extension function
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }

        adRemover = skuDetailsResult.skuDetailsList?.find { it.sku == sku }


        // TODO:

        val queryPurchaseHistory = BillingHelper.billingClient?.queryPurchaseHistory(sku)
        queryPurchaseHistory.logD()
        queryPurchaseHistory?.billingResult?.run {
            Log.i("Sirelon", "HISTORY")
            this.debugMessage?.logD()
            this.responseCode?.logD()
        }

        // Process the result.
    }

    fun purchase(activity: FragmentActivity) {
        val skuDetails = adRemover ?: return
        val billingClient = billingClient ?: return


        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
    }
}