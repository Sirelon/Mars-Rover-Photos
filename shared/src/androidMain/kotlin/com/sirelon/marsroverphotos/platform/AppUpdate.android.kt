package com.sirelon.marsroverphotos.platform

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.sirelon.marsroverphotos.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [AppUpdate] backed by Google Play's in-app update flow.
 *
 * Uses the IMMEDIATE flow: Play draws the whole update screen, downloads, installs and restarts
 * the app, so the app adds no screen, state or copy of its own. The caller reaches this from an
 * explicit "update" tap, which is why a full-screen flow is acceptable.
 *
 * Returns `false` — the caller opens the store listing — when Play offers no update for this
 * install (a debug or sideloaded build has no store backing), when no Activity is available, or
 * when Play reports the flow failed. A user who backs out of Play's screen has made a choice, so
 * that returns `true` and nothing else opens.
 */
class AndroidAppUpdate(
    private val context: Context,
    private val activityProvider: () -> Activity? = { ActivityProvider.current() },
) : AppUpdate {

    override suspend fun startUpdate(): Boolean {
        val activity = activityProvider() ?: run {
            Logger.w(TAG) { "No Activity available for in-app update" }
            return false
        }
        return try {
            val manager = AppUpdateManagerFactory.create(context)
            val info = manager.requestAppUpdateInfo()
            // IN_PROGRESS: the user left an immediate update half way; Play resumes it.
            val offered = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            if (!offered || !info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                Logger.d(TAG) { "No immediate in-app update offered (availability=${info.updateAvailability()})" }
                return false
            }
            val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            val resultCode = suspendCancellableCoroutine { continuation ->
                manager.startUpdateFlow(info, activity, options)
                    .addOnSuccessListener { code -> continuation.resume(code) }
                    .addOnFailureListener { error ->
                        Logger.w(TAG) { "In-app update flow failed: ${error.message}" }
                        continuation.resume(ActivityResult.RESULT_IN_APP_UPDATE_FAILED)
                    }
            }
            resultCode != ActivityResult.RESULT_IN_APP_UPDATE_FAILED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG) { "In-app update failed: ${e.message}" }
            false
        }
    }

    companion object {
        private const val TAG = "AndroidAppUpdate"
    }
}
