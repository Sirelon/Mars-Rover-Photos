import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.sirelon.marsroverphotos.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Mars Rover Photos"
            packageVersion = "3.0.0"

            description = "Explore Mars through NASA rover photos"
            vendor = "Sirelon"

            macOS {
                bundleID = "com.sirelon.marsroverphotos.desktop"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}
