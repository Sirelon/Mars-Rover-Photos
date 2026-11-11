pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MarsRoverPhotos"

include(":shared")
include(":androidApp")
include(":baselineprofile")
// include(":iosApp")  // Not yet created
include(":desktopApp")
include(":webApp")