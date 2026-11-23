package com.sirelon.marsroverphotos.platform

/** The store a build is distributed through. Galaxy Store builds count as [GOOGLE_PLAY]. */
enum class AppStore { GOOGLE_PLAY, APP_STORE }

/** The store this build comes from, or null for desktop, which has none. */
expect val currentAppStore: AppStore?
