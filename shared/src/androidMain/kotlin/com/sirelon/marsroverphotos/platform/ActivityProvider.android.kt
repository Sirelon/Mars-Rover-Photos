package com.sirelon.marsroverphotos.platform

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the currently-foregrounded Activity so shared-module
 * Android code (e.g. in-app review) can reach a real `Activity` without holding it
 * across configuration changes.
 *
 * The host Activity is expected to call [set] in `onResume` (or `onCreate`) and
 * [clear] in `onDestroy`.
 */
object ActivityProvider {
    @Volatile
    private var activityRef: WeakReference<Activity>? = null

    fun set(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun clear(activity: Activity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    /** Returns the current Activity if one is registered and still alive, otherwise null. */
    fun current(): Activity? = activityRef?.get()
}
