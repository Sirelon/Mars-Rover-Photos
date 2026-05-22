import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

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

    // iOS targets — XCFramework bundles both slices so Xcode picks the right one
    // for simulator and real device builds automatically.
    val xcf = XCFramework("shared")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
            export(libs.koin.core)
            xcf.add(this)
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
            implementation("androidx.paging:paging-compose:3.4.2")

            // Room KMP (Android, iOS, Desktop only - no WASM support)
            implementation(libs.androidx.room.runtime)
            // room-paging supports all KMP targets since room3 3.0.0-alpha05 (b/339934824)
            implementation(libs.androidx.room.paging)

            // GitLive Firebase KMP (Firestore supports all platforms)
            implementation(libs.gitlive.firebase.firestore)
        }

        // Android-specific dependencies
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.lifecycle.viewmodel.navigation3)

            // Firebase native (Android-only — GitLive wraps these, still needed for Android init)
            implementation(libs.firebase.analytics.versioned)
            implementation(libs.firebase.crashlytics.versioned)
            implementation(libs.firebase.firestore.versioned)
            implementation(libs.firebase.messaging.versioned)

            // GitLive Firebase KMP (analytics + crashlytics — Android + iOS only)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)

            // AndroidX
            implementation(libs.androidx.ktx)
        }

        // iOS-specific dependencies
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            // GitLive Firebase KMP (analytics + crashlytics — Android + iOS only)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)
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

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // KSP for Room - all platforms with Room support (Android, iOS, Desktop)
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}
