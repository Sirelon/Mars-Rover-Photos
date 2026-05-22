package com.sirelon.marsroverphotos.platform

import android.content.Context
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.sirelon.marsroverphotos.utils.Logger

/**
 * Android implementation of [AppReview] backed by Google Play's in-app review flow.
 *
 * Notes:
 * - In-app review is throttled by Google Play and may simply not show in debug builds
 *   or when the device doesn't have a recent Play Store. We return `true` whenever the
 *   flow completes (regardless of whether the UI was actually rendered) so the caller
 *   does NOT redirect the user to the store - per Play Core guidance.
 * - If anything fails (Play Services missing, quota exhausted, no Activity), we return
 *   `false` so the caller can fall back to the store URL.
 */
class AndroidAppReview(
    private val context: Context,
    private val activityProvider: () -> android.app.Activity? = { ActivityProvider.current() },
) : AppReview {

    override suspend fun requestReview(): Boolean {
        val activity = activityProvider() ?: run {
            Logger.w(TAG) { "No Activity available for in-app review" }
            return false
        }
        return try {
            val manager = ReviewManagerFactory.create(context)
            val reviewInfo = manager.requestReview()
            manager.launchReview(activity, reviewInfo)
            true
        } catch (e: ReviewException) {
            Logger.w(TAG) { "In-app review failed: errorCode=${e.errorCode}, ${e.message}" }
            false
        } catch (e: Exception) {
            Logger.w(TAG) { "In-app review failed: ${e.message}" }
            false
        }
    }

    companion object {
        private const val TAG = "AndroidAppReview"
    }
}
