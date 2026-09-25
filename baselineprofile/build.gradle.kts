plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.sirelon.marsroverphotos.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Profile generation without root needs API 33+; Macrobenchmark itself needs 28+.
        minSdk = 28
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":androidApp"

    // A headless emulator owned by Gradle, so generation never installs over the app on a
    // connected phone or a shared emulator. `google` images allow root, which the collector uses;
    // API 37 ships them with 16 KB pages only (system-images;android-37.0;google_apis_ps16k).
    testOptions.managedDevices.localDevices {
        create("pixel6Api37") {
            device = "Pixel 6"
            sdkVersion = 37
            systemImageSource = "google"
            pageAlignment = com.android.build.api.dsl.ManagedVirtualDevice.PageAlignment.FORCE_16KB_PAGES
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api37"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
