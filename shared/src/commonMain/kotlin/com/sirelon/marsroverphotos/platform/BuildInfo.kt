package com.sirelon.marsroverphotos.platform

object BuildInfo {
    var versionName: String = "unknown"
        private set
    var isDebug: Boolean = false
        private set
    var packageName: String = "com.sirelon.marsroverphotos"
        private set

    // Set at launch (via a launch argument) only for automated screenshot capture, to hide all ads.
    // Always false in normal use, so production behavior is unchanged.
    var hideAds: Boolean = false

    fun init(versionName: String, isDebug: Boolean, packageName: String) {
        this.versionName = versionName
        this.isDebug = isDebug
        this.packageName = packageName
    }
}
