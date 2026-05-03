import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.firebase.crashlytics)
    id("io.gitlab.arturbosch.detekt")
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.sirelon.marsroverphotos"
    compileSdk = 36

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
            }
        }
    }

    defaultConfig {
        applicationId = "com.sirelon.marsroverphotos"
        minSdk = 23
        targetSdk = 36
        versionCode = 48
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        if (keystorePropertiesFile.exists()) {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug configuration
        }
    }

    configurations {
        all {
            exclude(group = "com.google.guava", module = "listenablefuture")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Android-specific dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Koin Android
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Glance App Widgets
    implementation(libs.androidx.glance.appwidget)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase (already in shared, but needed for initialization)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)

    // AdMob & GDPR
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Testing
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test.junit)
}

// Validate google-services.json configuration
val googleServicesFile = file("$projectDir/google-services.json")
if (!googleServicesFile.exists()) {
    logger.warn("WARNING: google-services.json not found. Firebase features will be disabled.")
    logger.warn("Add google-services.json to enable Firebase Analytics, Crashlytics, and other services.")

    // Disable Google Services tasks for non-release builds
    tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }
        .configureEach {
            if (!name.contains("Release")) {
                enabled = false
            }
        }

    // Fail release builds if google-services.json is missing
    gradle.taskGraph.whenReady {
        if (hasTask(":androidApp:assembleRelease") || hasTask(":androidApp:bundleRelease")) {
            throw GradleException(
                "google-services.json is required for release builds. " +
                "Please add the file to androidApp/ directory."
            )
        }
    }
}
