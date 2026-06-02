plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("marsRoverPhotosWeb")
        browser {
            commonWebpackConfig {
                outputFileName = "marsRoverPhotosWeb.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.compose.mp.runtime)
                implementation(libs.compose.mp.foundation)
                implementation(libs.compose.mp.material3)
                implementation(libs.compose.mp.ui)
            }
        }
    }
}
