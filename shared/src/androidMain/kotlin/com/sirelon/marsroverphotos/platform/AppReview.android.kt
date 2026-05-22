package com.sirelon.marsroverphotos.platform

import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [AppReview] backed by the Google Play In-App Review API.
 *
 * Returns `true` if the review overlay was launched.
 * Returns `false` if no Activity is available or the review flow fails — the
 * caller in [AboutScreen] will fall back to opening the Play Store URL.
 */
class AndroidAppReview(private val context: Context) : AppReview {

    override suspend fun requestReview(): Boolean {
        val activity = ActivityProvider.get() ?: return false
        return try {
            val manager = ReviewManagerFactory.create(context)

            // Step 1: request a ReviewInfo token (quota-managed by Play)
            val reviewInfo: ReviewInfo = suspendCancellableCoroutine { cont ->
                manager.requestReviewFlow()
                    .addOnSuccessListener { info -> cont.resume(info) }
                    .addOnFailureListener { cont.resume(null) }
            } ?: return false

            // Step 2: launch the overlay (may be a no-op if Play throttles it)
            suspendCancellableCoroutine { cont ->
                manager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener { cont.resume(Unit) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
