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

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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

    // Web (Wasm) target - TEMPORARILY DISABLED
    // TODO: WASM support requires significant refactoring:
    // - Room Database doesn't support WASM
    // - Need to implement expect/actual for all database-dependent code
    // - Or use alternative storage (IndexedDB) for web
    // @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    // wasmJs {
    //     browser()
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
            implementation(libs.koin.compose.navigation3)

            // Image loading (Coil KMP)
            implementation(libs.coil)
            implementation(libs.coil.network)

            // Immutable collections
            implementation(libs.kotlinx.collections.immutable)

            // Lifecycle
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose.multiplatform)

            // Navigation 3
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)

            // Kermit logging
            implementation(libs.kermit)

            // Paging (common)
            implementation(libs.paging.common)

            // Room KMP (Android, iOS, Desktop only - no WASM support)
            implementation(libs.androidx.room.runtime)
        }

        // Android-specific dependencies
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.lifecycle.viewmodel.navigation3)

            // Room extensions (Android-only)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.room.paging)

            // Firebase (Android-only)
            implementation(libs.firebase.analytics.versioned)
            implementation(libs.firebase.crashlytics.versioned)
            implementation(libs.firebase.firestore.versioned)
            implementation(libs.firebase.messaging.versioned)

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

        // Web-specific dependencies (WASM disabled for now)
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
    // KSP for Room - all platforms with Room support (Android, iOS, Desktop)
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}
