package com.sirelon.marsroverphotos.platform

import com.sirelon.marsroverphotos.utils.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
// Declared on an Objective-C category (UIApplication + UIRemoteNotifications), which Kotlin/Native
// surfaces as an extension function — so it needs importing by name.
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation of [PushNotifications].
 *
 * Only the parts that are portable live here: authorization, APNs registration and topic
 * subscription. Receiving a tap needs a `UNUserNotificationCenterDelegate` that exists from the
 * first moment of launch, which is the app delegate's job in `MarsRoverApp.swift` — a Koin
 * singleton is created lazily and would miss a notification that cold-launched the app.
 */
class IosPushNotifications : PushNotifications {

    override suspend fun permissionStatus(): PushPermissionStatus =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val status = when (settings?.authorizationStatus) {
                        UNAuthorizationStatusNotDetermined -> PushPermissionStatus.NotDetermined
                        UNAuthorizationStatusAuthorized,
                        UNAuthorizationStatusProvisional,
                        UNAuthorizationStatusEphemeral,
                        -> PushPermissionStatus.Granted

                        else -> PushPermissionStatus.Denied
                    }
                    if (continuation.isActive) continuation.resume(status)
                }
        }

    override suspend fun requestPermission(): PushPermissionStatus {
        val current = permissionStatus()
        if (current != PushPermissionStatus.NotDetermined) return current

        val granted = suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { isGranted, error ->
                if (error != null) Logger.w(TAG) { "Authorization request failed: $error" }
                if (continuation.isActive) continuation.resume(isGranted)
            }
        }

        if (granted) {
            // FCM cannot mint a registration token — and so cannot subscribe to a topic — until
            // APNs has issued a device token, which only starts after this call.
            withContext(Dispatchers.Main) {
                UIApplication.sharedApplication.registerForRemoteNotifications()
            }
        }

        return if (granted) PushPermissionStatus.Granted else PushPermissionStatus.Denied
    }

    /**
     * Call this only once FCM holds a registration token, which in practice means from the app
     * delegate's `didReceiveRegistrationToken` — see `onFcmRegistrationTokenAvailable` in
     * `Main.ios.kt`. Calling it earlier is not merely racy, it fails outright (see below).
     */
    override suspend fun setSubscribed(subscribed: Boolean) {
        try {
            if (subscribed) {
                // getToken() does not wait for APNs — it fails with FCM error 505, "No APNS token
                // specified before fetching FCM Token", whenever the device token has not arrived
                // yet. That is why neither the opt-in tap nor app launch can drive the
                // subscription: both run while APNs registration is still in flight.
                val token = Firebase.messaging.getToken()
                if (BuildInfo.isDebug) Logger.d(TAG) { "FCM registration token: $token" }
                Firebase.messaging.subscribeToTopic(MarsUpdatesTopic)
            } else {
                Firebase.messaging.unsubscribeFromTopic(MarsUpdatesTopic)
            }
        } catch (e: Exception) {
            // Reached when there is still no APNs token. Recovery is the delegate callback, which
            // fires as soon as one exists — not a retry from here.
            Logger.w(TAG) { "Topic subscribed=$subscribed failed: ${e.message}" }
        }
    }

    override fun openSystemSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    private companion object {
        const val TAG = "IosPushNotifications"
    }
}
