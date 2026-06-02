package com.sirelon.marsroverphotos.platform

import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene

/**
 * iOS implementation of [AppReview] backed by [SKStoreReviewController].
 *
 * On iOS 16+ the scene-based `requestReview(in:)` API is used so the system
 * can anchor the sheet correctly on iPad. On older iOS the (deprecated-but-working)
 * class-level `requestReview()` is used as a fallback.
 *
 * Apple rate-limits the prompt to at most 3 appearances per 365-day period per
 * device, so the dialog may not show on every call. Returning `true` signals that
 * the request was dispatched; the caller in [AboutScreen] only opens the App Store
 * URL when this returns `false`.
 */
class IosAppReview : AppReview {

    override suspend fun requestReview(): Boolean = try {
        val scene = UIApplication.sharedApplication.connectedScenes
            .asSequence()
            .mapNotNull { it as? UIWindowScene }
            .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            ?: UIApplication.sharedApplication.connectedScenes
                .asSequence()
                .mapNotNull { it as? UIWindowScene }
                .firstOrNull()

        if (scene != null) {
            SKStoreReviewController.requestReviewInScene(scene)
        } else {
            @Suppress("DEPRECATION")
            SKStoreReviewController.requestReview()
        }
        true
    } catch (_: Exception) {
        false
    }
}
