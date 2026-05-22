package com.sirelon.marsroverphotos.platform

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the currently-active Android Activity.
 *
 * Set from `MainActivity.onCreate` and cleared in `MainActivity.onDestroy`.
 * Used by Android-only platform code (e.g. [AndroidAppReview]) that needs an
 * Activity reference but is constructed through Koin with Application context only.
 *
 * The reference is weak so it cannot prevent garbage collection of the Activity.
 */
object ActivityProvider {
    private var ref: WeakReference<ComponentActivity>? = null

    fun set(activity: ComponentActivity) {
        ref = WeakReference(activity)
    }

    fun get(): ComponentActivity? = ref?.get()

    /** Clears the reference only if it still points to [activity] (guards against races). */
    fun clear(activity: ComponentActivity) {
        if (ref?.get() === activity) {
            ref = null
        }
    }
}
