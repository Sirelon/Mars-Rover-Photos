package com.sirelon.marsroverphotos.platform

/**
 * The single Firebase Cloud Messaging topic every broadcast is sent to.
 *
 * One topic by design: at this app's volume a second one would double the settings UI and the send
 * runbook to serve a split nobody has asked for. See `docs/PUSH_NOTIFICATIONS.md`.
 */
const val MarsUpdatesTopic: String = "mars-updates"

/** OS-level authorization for showing notifications. */
enum class PushPermissionStatus {
    /** Never asked. [PushNotifications.requestPermission] will show the system prompt. */
    NotDetermined,

    Granted,

    /**
     * Refused, or switched off in system settings later. The OS will not show its prompt again,
     * so the only way back is [PushNotifications.openSystemSettings].
     */
    Denied,

    /** The platform has no push support (Desktop, Web). */
    Unsupported,
}

/**
 * Platform-agnostic push notification opt-in.
 *
 * Deliberately tiny and free of ordering requirements: the app asks for notification permission
 * only in response to a user tap, so this can be called from any screen without coordinating with
 * the consent prompts that run at launch.
 *
 * Note there is no "did a message arrive" callback — messages are displayed by the OS, and taps
 * reach the app through the normal deep-link path (`data.link` in the payload).
 */
interface PushNotifications {

    /** Current OS authorization. Cheap enough to call on screen entry. */
    suspend fun permissionStatus(): PushPermissionStatus

    /**
     * Shows the OS permission prompt when the status is [PushPermissionStatus.NotDetermined],
     * and returns the resulting status. For any other status this returns it unchanged without
     * showing UI, so it is safe to call repeatedly.
     */
    suspend fun requestPermission(): PushPermissionStatus

    /** Subscribes or unsubscribes [MarsUpdatesTopic]. Idempotent; failures are logged, not thrown. */
    suspend fun setSubscribed(subscribed: Boolean)

    /**
     * Opens the OS notification settings for this app — the only route back from
     * [PushPermissionStatus.Denied], since the system prompt is spent.
     */
    fun openSystemSettings()
}

/** Used on targets without push support, so callers need no platform checks. */
class NoOpPushNotifications : PushNotifications {
    override suspend fun permissionStatus(): PushPermissionStatus = PushPermissionStatus.Unsupported
    override suspend fun requestPermission(): PushPermissionStatus = PushPermissionStatus.Unsupported
    override suspend fun setSubscribed(subscribed: Boolean) = Unit
    override fun openSystemSettings() = Unit
}
