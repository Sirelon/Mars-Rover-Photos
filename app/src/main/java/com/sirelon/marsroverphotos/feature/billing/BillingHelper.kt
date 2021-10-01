package com.sirelon.marsroverphotos.feature.billing

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenCreated
import com.android.billingclient.api.*
import com.sirelon.marsroverphotos.extensions.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Created on 12.05.2021 22:38 for Mars-Rover-Photos.
 */
class BillingHelper(private val activity: FragmentActivity) {

    private var billingClient: BillingClient? = null
    private var adRemover: SkuDetails? = null

    init {
        activity.lifecycleScope.launch {
            activity.lifecycle.whenCreated {
                init()
            }

            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                checkPurchased()
            }
        }
    }

    fun init() {
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
                "onBillingSetupFinished".logD()
                billingResult.logD()
                billingResult.responseCode.logD()
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
        Log.i("Sirelon", "handlePurchase() called with: activity = $activity, purchase = $purchase");
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
        val sku = "array_ist"
        skuList.add(sku)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        // leverage querySkuDetails Kotlin extension function
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }

        adRemover = skuDetailsResult.skuDetailsList?.find { it.sku == sku }

        "querySkuDetails".logD()
        adRemover.logD()
        skuDetailsResult.billingResult.responseCode.logD()
        skuDetailsResult.billingResult.debugMessage.logD()

        checkPurchased()

//            if (l.first().purchaseState == Purchase.PurchaseState.PURCHASED) {
//                // remove Ad
//            }
//        val queryPurchaseHistory = BillingHelper.billingClient?.queryPurchasesAsync(sku) { r, l ->
//
//            "queryPurchasesAsync ${r.responseCode} ".logD()
//            l.forEach {
//                "State ${it.purchaseState}".logD()
//            }
//
////            if (l.first().purchaseState == Purchase.PurchaseState.PURCHASED) {
////                // remove Ad
////            }
//        }
//        queryPurchaseHistory.logD()
//        queryPurchaseHistory?.purchaseHistoryRecordList?.find {
//            it.skus.contains(sku)
//        }?.let {
//
//            it.
//        }
//        queryPurchaseHistory?.billingResult?.run {
//            Log.i("Sirelon", "HISTORY")
//            this.debugMessage?.logD()
//            this.responseCode?.logD()
//        }

        // Process the result.
    }

    private suspend fun checkPurchased() {
        Timber.d("checkPurchased() called");
        val billingClient = billingClient ?: return
        val queryPurchaseHistory = billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP)

        val r = queryPurchaseHistory.billingResult
        val l = queryPurchaseHistory.purchasesList
        "queryPurchasesAsync ${r.responseCode} and $l".logD()
        l?.forEach {
            "State ${it.purchaseState}".logD()
        }
    }

    fun purchase() {
        val skuDetails = adRemover ?: return
        val billingClient = billingClient ?: return


        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
    }
}