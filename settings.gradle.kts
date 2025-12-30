pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

rootProject.name = "MarsRoverPhotos"

// include(":app")  // Old module - migrating to :androidApp
include(":shared")
include(":androidApp")
// include(":iosApp")  // Not yet created
include(":desktopApp")
// include(":webApp")  // Temporarily disabled while debugging