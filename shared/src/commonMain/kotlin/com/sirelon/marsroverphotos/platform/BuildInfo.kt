package com.sirelon.marsroverphotos.platform

object BuildInfo {
    var versionName: String = "unknown"
        private set
    var isDebug: Boolean = false
        private set
    var packageName: String = "com.sirelon.marsroverphotos"
        private set

    fun init(versionName: String, isDebug: Boolean, packageName: String) {
        this.versionName = versionName
        this.isDebug = isDebug
        this.packageName = packageName
    }
}
