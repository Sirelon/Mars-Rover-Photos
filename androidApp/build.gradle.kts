import java.io.FileInputStream
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
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
    compileSdk = 37

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
        versionCode = AppVersion.code
        versionName = AppVersion.name

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

// Workaround: CMP 1.12.0-alpha01 bug — copyAndroidMainComposeResourcesToAndroidAssets has no
// outputDirectory when the shared module uses android.kotlin.multiplatform.library (AGP 9+).
// Copy commonMain composeResources into the APK assets via the AGP Variant API. Wiring the task's
// output through addGeneratedSourceDirectory makes AGP add the task dependency to the merge-assets
// step automatically — no deprecated sourceSets.assets.srcDir(..) and no afterEvaluate name match.
val sharedResPackage = "com.sirelon.marsroverphotos.shared.resources"

@CacheableTask
abstract class CopySharedComposeResources : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:Input
    abstract val resourcePackage: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copy() {
        fs.sync {
            from(sourceDir)
            into(outputDir.dir("composeResources/${resourcePackage.get()}"))
        }
    }
}

androidComponents {
    onVariants { variant ->
        val capName = variant.name.replaceFirstChar { it.uppercase() }
        val copyTask = tasks.register<CopySharedComposeResources>("copy${capName}SharedComposeResources") {
            sourceDir.set(
                project(":shared").layout.buildDirectory.dir(
                    "generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"
                )
            )
            resourcePackage.set(sharedResPackage)
            dependsOn(":shared:prepareComposeResourcesTaskForCommonMain")
        }
        variant.sources.assets?.addGeneratedSourceDirectory(
            copyTask,
            CopySharedComposeResources::outputDir
        )
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

    // Compose UI (Material3 + foundation needed by widget config activity)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)

    // Coil image loading (used by widget worker to download photos)
    implementation(libs.coil)

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
