package com.sirelon.marsroverphotos.feature.billing

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenCreated
import com.android.billingclient.api.*
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.extensions.recordException
import com.sirelon.marsroverphotos.feature.settings.BundleUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Created on 12.05.2021 22:38 for Mars-Rover-Photos.
 */
class BillingHelper(private val activity: FragmentActivity) {

    private val bundlesList = listOf<BundleUi>(
        BundleUi("🔥", "All life!", "Buy once, use all life!", "ad_remover_for_month", false),
        BundleUi("✨", "For a month", "Remove ad for a month!", "ad_remover_for_life", false),
    )

    private var billingClient: BillingClient? = null
    private val skuDetailsList = mutableListOf<SkuDetails>()

    private val adRemovedMutableFlow = MutableStateFlow<Boolean?>(null)
    val adRemovedFlow = adRemovedMutableFlow.filterNotNull()

    val bundlesFlow = MutableStateFlow(bundlesList)

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
                } else {
                    adRemovedMutableFlow.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                "onBillingServiceDisconnected".logD()
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                adRemovedMutableFlow.value = false
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

    private suspend fun querySkuDetails() {
        val billingClient = billingClient ?: return
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(bundlesList.map { it.sku }).setType(BillingClient.SkuType.INAPP)

        // leverage querySkuDetails Kotlin extension function
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }

        skuDetailsList.clear()
        skuDetailsResult.skuDetailsList?.let(skuDetailsList::addAll)

        checkPurchased()
    }

    private suspend fun checkPurchased() {
        Timber.d("checkPurchased() called");
        val billingClient = billingClient ?: return
        val adRemoverSkus = bundlesFlow.value.map { it.sku }

        val queryPurchaseHistory = billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP)

        val r = queryPurchaseHistory.billingResult
        val l = queryPurchaseHistory.purchasesList
        "queryPurchasesAsync ${r.responseCode} and $l".logD()

        val atLeastOnePurchased = adRemoverSkus.any { sku ->
            val adRemoverPurchase = l.find { it.skus.contains(sku) }
            adRemoverPurchase?.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        adRemovedMutableFlow.value = atLeastOnePurchased
    }

    fun purchase(bundleUi: BundleUi) {
        val billingClient = billingClient ?: return
        val skuDetails = skuDetailsList.find { it.sku == bundleUi.sku }

        if (skuDetails == null) {
            // TODO: Show Error
            val e =
                IllegalStateException("Try Purchase $bundleUi but $skuDetailsList don't contains bundle")
            recordException(e)

            Toast.makeText(
                activity,
                "Something went wrong, sorry. Try abit later.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
    }
}