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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Android-specific dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.ktx)

    // Koin Android
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Glance App Widgets
    implementation(libs.androidx.glance.appwidget)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase (already in shared, but needed for initialization)
    implementation(platform(libs.firebase.bom))

    // AdMob & GDPR
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Testing
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test.junit)
}

// Disable Google Services if google-services.json doesn't exist
if (!file("$projectDir/google-services.json").exists()) {
    tasks.matching { it.name == "processDebugGoogleServices" }.configureEach {
        enabled = false
    }
}
