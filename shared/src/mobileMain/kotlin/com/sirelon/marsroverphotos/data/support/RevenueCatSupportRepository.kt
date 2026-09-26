package com.sirelon.marsroverphotos.data.support

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.sirelon.marsroverphotos.domain.repositories.SupportRepository
import com.sirelon.marsroverphotos.domain.support.SupportPurchaseResult
import com.sirelon.marsroverphotos.domain.support.SupportTier
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [SupportRepository] over the RevenueCat SDK (Play Billing on Android, StoreKit on iOS).
 *
 * Configures the SDK on construction, so the Koin `single` must be created early (the navigation
 * root injects it through `SupportViewModel` on the first frame). [isAdFree] is seeded from the
 * SDK's on-disk customer cache — instant, no network — and then kept current by the SDK delegate,
 * which fires after every purchase, restore and background refresh.
 *
 * The current offering is fetched once per process behind a [Mutex]: every Nav3 entry builds its
 * own ViewModel, so without this the About row, the ad gate and the Support screen would each hit
 * the store. An empty result is not cached so a flaky launch retries on the next call.
 */
class RevenueCatSupportRepository(
    apiKey: String,
    isDebug: Boolean,
    private val scope: CoroutineScope,
) : SupportRepository {

    private companion object {
        const val TAG = "SupportRepository"
        const val AD_FREE_ENTITLEMENT = "ad_free"

        /** The store titles are "Support: Coffee"; the screen already says "Support". */
        const val TITLE_PREFIX = "Support: "

        const val TIER_UNAVAILABLE = "This tier isn't available right now."

        /** Google Play appends " (App Name)" to every product title. */
        val PLAY_APP_NAME_SUFFIX = Regex("""\s*\([^)]*\)\s*$""")
    }

    private val _isAdFree = MutableStateFlow<Boolean?>(null)
    override val isAdFree: StateFlow<Boolean?> = _isAdFree.asStateFlow()

    private val offeringMutex = Mutex()
    private var cachedPackages: List<Package>? = null

    init {
        if (!Purchases.isConfigured) {
            Purchases.logLevel = if (isDebug) LogLevel.DEBUG else LogLevel.WARN
            Purchases.configure(PurchasesConfiguration.Builder(apiKey).build())
        }
        Purchases.sharedInstance.delegate = object : PurchasesDelegate {
            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) = apply(customerInfo)

            // App Store promoted purchases started from the store page: let them proceed; the
            // delegate above picks up the resulting entitlement.
            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: (
                    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                    onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
                ) -> Unit,
            ) {
                startPurchase({ error, _ -> Logger.w(TAG) { "Promo purchase failed: ${error.message}" } }, { _, _ -> })
            }
        }
        scope.launch {
            runCatching { Purchases.sharedInstance.awaitCustomerInfo() }
                .onSuccess(::apply)
                .onFailure {
                    Logger.w(TAG) { "Customer info unavailable at start: ${it.message}" }
                    // Unknown is not "supporter": ads stay on until a purchase or restore says otherwise.
                    _isAdFree.compareAndSet(null, false)
                }
        }
    }

    // Cheapest first: the dashboard's package order is not something the UI should depend on.
    override suspend fun tiers(): List<SupportTier> =
        packages().sortedBy { it.storeProduct.price.amountMicros }.map { it.toTier() }

    override suspend fun purchase(tierId: String): SupportPurchaseResult {
        val pkg = packages().firstOrNull { it.storeProduct.id == tierId }
            ?: return SupportPurchaseResult.Failure(TIER_UNAVAILABLE, code = "TierMissing")
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(packageToPurchase = pkg)
            apply(result.customerInfo)
            SupportPurchaseResult.Success
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) SupportPurchaseResult.Cancelled else e.toFailure(tierId)
        } catch (e: PurchasesException) {
            e.toFailure(tierId)
        }
    }

    private suspend fun PurchasesException.toFailure(tierId: String): SupportPurchaseResult {
        Logger.w(TAG) { "Purchase of $tierId failed: $code $underlyingErrorMessage" }
        return when (code) {
            PurchasesErrorCode.PurchaseCancelledError -> SupportPurchaseResult.Cancelled
            // The store account already owns it (e.g. bought on another device): that is a restore.
            PurchasesErrorCode.ProductAlreadyPurchasedError -> {
                runCatching { Purchases.sharedInstance.awaitRestore() }.onSuccess(::apply)
                if (_isAdFree.value == true) {
                    SupportPurchaseResult.Success
                } else {
                    SupportPurchaseResult.Failure(
                        message = "This store account already owns it, but it couldn't be restored. Try Restore purchases.",
                        code = code.name,
                    )
                }
            }
            else -> SupportPurchaseResult.Failure(message = code.userMessage(), code = code.name)
        }
    }

    private fun PurchasesErrorCode.userMessage(): String = when (this) {
        PurchasesErrorCode.StoreProblemError,
        PurchasesErrorCode.NetworkError,
        PurchasesErrorCode.UnknownBackendError -> "The store didn't respond. Nothing was charged — try again in a moment."
        PurchasesErrorCode.PurchaseNotAllowedError,
        PurchasesErrorCode.InsufficientPermissionsError -> "Purchases aren't allowed on this device."
        PurchasesErrorCode.PaymentPendingError -> "Your payment is pending. Ads switch off once the store confirms it."
        PurchasesErrorCode.ProductNotAvailableForPurchaseError -> TIER_UNAVAILABLE
        else -> "The purchase didn't go through — nothing was charged."
    }

    override suspend fun restore(): Boolean {
        runCatching { Purchases.sharedInstance.awaitRestore() }
            .onSuccess(::apply)
            .onFailure { Logger.w(TAG) { "Restore failed: ${it.message}" } }
        return _isAdFree.value == true
    }

    private fun apply(customerInfo: CustomerInfo) {
        _isAdFree.value = customerInfo.entitlements.active.containsKey(AD_FREE_ENTITLEMENT)
    }

    private suspend fun packages(): List<Package> = offeringMutex.withLock {
        cachedPackages ?: fetchPackages().also { if (it.isNotEmpty()) cachedPackages = it }
    }

    private suspend fun fetchPackages(): List<Package> = try {
        Purchases.sharedInstance.awaitOfferings().current?.availablePackages.orEmpty()
    } catch (e: PurchasesException) {
        Logger.w(TAG) { "Offerings unavailable: ${e.error.code} ${e.underlyingErrorMessage}" }
        emptyList()
    }

    private fun Package.toTier() = SupportTier(
        id = storeProduct.id,
        title = storeProduct.title
            .replace(PLAY_APP_NAME_SUFFIX, "")
            .removePrefix(TITLE_PREFIX)
            .trim(),
        price = storeProduct.price.formatted,
    )
}
