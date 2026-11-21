package com.sirelon.marsroverphotos.domain.featureflags

/**
 * Remotely controlled switches. [key] is the Firebase Remote Config parameter name; [default] is
 * what the app uses until a fetch lands, or forever on a platform with no Remote Config.
 *
 * [enabledInDebug] pins the flag on in debug builds, ignoring the remote value: the console flag is
 * global, so without this a feature could not be exercised on a dev device before it is switched on
 * for every user.
 */
enum class FeatureFlag(val key: String, val default: Boolean, val enabledInDebug: Boolean = default) {
    /** "Support the developer" tiers: the About row and the Support screen entry point. */
    SUPPORT_TIERS(key = "support_tiers_enabled", default = false, enabledInDebug = true),

    /**
     * The "Today on Mars" card at the top of the rovers list: the freshest photo of an active
     * rover, one tap from its feed. On by default; the remote value is the off switch.
     */
    TODAY_ON_MARS(key = "today_on_mars_enabled", default = true),
}
