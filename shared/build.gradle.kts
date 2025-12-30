plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    // JVM toolchain for all JVM targets
    jvmToolchain(17)

    // Android target
    androidTarget()

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true

            // Export dependencies to iOS
            export(libs.koin.core)
        }
    }

    // Desktop (JVM) target
    jvm("desktop")

    // Web (Wasm) target - Temporarily disabled for debugging
    // @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    // wasmJs {
    //     moduleName = "marsRoverPhotosShared"
    //     browser()
    //     binaries.executable()
    // }

    // Workaround for Room KMP alpha issue with coroutines-android on iOS
    configurations.all {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "org.jetbrains.kotlinx" && requested.name == "kotlinx-coroutines-android") {
                    useTarget("org.jetbrains.kotlinx:kotlinx-coroutines-core:${requested.version}")
                    because("Room KMP uses coroutines-android which is not compatible with iOS")
                }
            }
        }
    }

    sourceSets {
        // Common source set - shared across all platforms
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Ktor (networking)
            implementation(libs.ktor.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)

            // Kotlinx DateTime
            implementation(libs.kotlinx.datetime)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Koin Multiplatform
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Image loading (Coil KMP)
            implementation(libs.coil)
            implementation(libs.coil.network)

            // Immutable collections
            implementation(libs.kotlinx.collections.immutable)

            // Lifecycle
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose.multiplatform)

            // Navigation Compose KMP
            implementation(libs.navigation.compose)

            // Kermit logging
            implementation(libs.kermit)

            // Paging (common)
            implementation(libs.paging.common)
        }

        // Android-specific dependencies
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // Room 2.8.4 stable (Android-only)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.room.paging)

            // Firebase (Android-only) - Latest stable versions
            implementation("com.google.firebase:firebase-analytics:22.1.2")
            implementation("com.google.firebase:firebase-crashlytics:19.2.1")
            implementation("com.google.firebase:firebase-firestore:25.1.1")
            implementation("com.google.firebase:firebase-messaging:24.1.0")

            // AndroidX
            implementation(libs.androidx.ktx)
        }

        // iOS-specific dependencies
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        // Desktop-specific dependencies
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        // Web-specific dependencies - Temporarily disabled
        // val wasmJsMain by getting {
        //     dependencies {
        //         implementation(libs.ktor.client.js)
        //     }
        // }

        // Common test dependencies
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
            implementation(libs.kotlin.test.annotations.common)
        }
    }
}

compose.resources {
    packageOfResClass = "com.sirelon.marsroverphotos.shared.resources"
}

android {
    namespace = "com.sirelon.marsroverphotos.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // KSP for Room - Android only (Room 2.8.4 is Android-specific)
    add("kspAndroid", libs.androidx.room.compiler)
}
