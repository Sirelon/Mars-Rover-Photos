package com.sirelon.marsroverphotos.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sirelon.marsroverphotos.utils.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [PushNotifications].
 *
 * Display is left entirely to the FCM SDK: messages carry a `notification` block, so the library's
 * own service renders them while the app is backgrounded. That is why no `FirebaseMessagingService`
 * subclass exists here — and why a message arriving while the app is in the foreground is dropped
 * rather than shown, which is the intended behaviour.
 */
class AndroidPushNotifications(
    private val context: Context,
    private val preferences: PlatformPreferences,
    private val activityProvider: () -> Activity? = { ActivityProvider.current() },
) : PushNotifications {

    override suspend fun permissionStatus(): PushPermissionStatus {
        // Before Android 13 there is no runtime permission — notifications are on unless the user
        // switched them off for the app, and there is no prompt to show either way.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PushPermissionStatus.Granted
            } else {
                PushPermissionStatus.Denied
            }
        }

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        return when {
            granted -> PushPermissionStatus.Granted
            // checkSelfPermission reports DENIED both before the first ask and after a refusal,
            // and the OS exposes nothing to tell them apart — so the marker below is what
            // distinguishes "can still prompt" from "prompt is spent".
            preferences.getBoolean(KEY_PERMISSION_ASKED, false) -> PushPermissionStatus.Denied
            else -> PushPermissionStatus.NotDetermined
        }
    }

    override suspend fun requestPermission(): PushPermissionStatus {
        val current = permissionStatus()
        if (current != PushPermissionStatus.NotDetermined) return current

        val activity = activityProvider() as? ComponentActivity ?: run {
            Logger.w(TAG) { "No ComponentActivity available to request notification permission" }
            return current
        }

        // Written before launching, not in the callback: if the user dismisses the dialog without
        // choosing, the callback reports "denied" but Android still counts the prompt as used.
        preferences.setBoolean(KEY_PERMISSION_ASKED, true)

        val granted = suspendCancellableCoroutine { continuation ->
            // A unique key per call — registry keys must not collide, and the launcher is
            // registered outside the Activity's lifecycle-aware overload so it must be released
            // by hand once the result lands.
            val key = "$REGISTRY_KEY_PREFIX${requestCounter++}"
            var launcher: ActivityResultLauncher<String>? = null
            launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                // Registered outside the lifecycle-aware overload, so the registry holds this
                // callback until it is released by hand — on the result path as well as cancellation.
                launcher?.unregister()
                if (continuation.isActive) continuation.resume(isGranted)
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        return if (granted) PushPermissionStatus.Granted else PushPermissionStatus.Denied
    }

    override suspend fun setSubscribed(subscribed: Boolean) {
        if (subscribed) ensureChannelExists()
        try {
            if (subscribed) {
                Firebase.messaging.subscribeToTopic(MarsUpdatesTopic)
            } else {
                Firebase.messaging.unsubscribeFromTopic(MarsUpdatesTopic)
            }
        } catch (e: Exception) {
            // Play Services missing or offline. The startup re-subscribe covers the next launch.
            Logger.w(TAG) { "Topic subscribed=$subscribed failed: ${e.message}" }
        }
    }

    override fun openSystemSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Logger.w(TAG) { "Could not open notification settings: ${e.message}" }
        }
    }

    /**
     * FCM does not create the channel named by the manifest's `default_notification_channel_id`
     * metadata — without this, messages would silently land in the SDK's fallback channel and the
     * user would have nothing meaningful to mute.
     */
    private fun ensureChannelExists() {
        val channel = NotificationChannelCompat
            .Builder(NotificationChannelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Mars Updates")
            .setDescription("New photos and release news")
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private companion object {
        const val TAG = "AndroidPushNotifications"
        const val KEY_PERMISSION_ASKED = "pushPermissionAsked"
        const val REGISTRY_KEY_PREFIX = "push_permission_"

        var requestCounter = 0
    }
}

/**
 * Id of the notification channel. Must stay in step with the `notification_channel_id` string
 * resource that `AndroidManifest.xml` points `default_notification_channel_id` at.
 */
const val NotificationChannelId: String = "mars_updates"
