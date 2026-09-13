pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        // Compose Hot Reload dev builds (see gradle/libs.versions.toml, compose-hot-reload)
        maven { url = uri("https://packages.jetbrains.team/maven/p/firework/dev") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        // Compose Hot Reload dev builds (see gradle/libs.versions.toml, compose-hot-reload)
        maven { url = uri("https://packages.jetbrains.team/maven/p/firework/dev") }
    }
}

rootProject.name = "MarsRoverPhotos"

include(":shared")
include(":androidApp")
// include(":iosApp")  // Not yet created
include(":desktopApp")
include(":webApp")