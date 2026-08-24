import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Force patched versions of vulnerable transitive dependencies in the build/plugin classpath.
// These packages are pulled in by Gradle plugins (Firebase Perf, Crashlytics, AGP, Kotlin plugin,
// etc.) and are not project runtime dependencies. buildscript-level forcing is required because
// resolutionStrategy in allprojects {} does not reach the plugin classpath.
buildscript {
    configurations.all {
        resolutionStrategy {
            force(
                "io.netty:netty-codec-http:4.1.137.Final",
                "io.netty:netty-codec-http2:4.1.132.Final",
                "io.netty:netty-codec:4.1.137.Final",
                "io.netty:netty-common:4.1.137.Final",
                "io.netty:netty-handler:4.1.118.Final",
                "com.google.guava:guava:32.1.3-android",
                "com.google.protobuf:protobuf-javalite:4.36.0",
                "org.apache.commons:commons-lang3:3.18.0",
                "org.apache.httpcomponents:httpclient:4.5.14",
                "org.bouncycastle:bcpkix-jdk18on:1.84",
                "org.bitbucket.b_c:jose4j:0.9.6",
                "org.jdom:jdom2:2.0.6.1",
            )
        }
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        google()
    }

    // Force patched versions of vulnerable transitive dependencies.
    // These are not used directly by the app code; forcing here overrides whichever
    // older version a transitive dependency would otherwise pull in.
    configurations.all {
        resolutionStrategy {
            force(
                // Netty — various DoS / request-smuggling / cache-poisoning CVEs
                "io.netty:netty-codec-http:4.1.137.Final",
                "io.netty:netty-codec-http2:4.1.132.Final",
                "io.netty:netty-codec:4.1.137.Final",
                "io.netty:netty-common:4.1.137.Final",
                "io.netty:netty-handler:4.1.118.Final",
                // Google libraries
                "com.google.guava:guava:32.1.3-android",
                "com.google.protobuf:protobuf-javalite:4.36.0",
                // Apache
                "org.apache.commons:commons-lang3:3.18.0",
                "org.apache.httpcomponents:httpclient:4.5.14",
                // Crypto / auth / XML parsers
                "org.bouncycastle:bcpkix-jdk18on:1.84",
                "org.bitbucket.b_c:jose4j:0.9.6",
                "org.jdom:jdom2:2.0.6.1",
            )
        }
    }
}

detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    autoCorrect = true
}

// Versioning tasks (bumpVersion / syncIosVersion) — see gradle/versioning.gradle.kts
apply(from = "gradle/versioning.gradle.kts")

// To check performance run script
// ./gradlew assembleRelease -PcomposeCompilerReports=true
// what's need to pay attention for check here https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md#enabling-metrics
subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        val dirProperty = project.layout.projectDirectory.asFile
        val rootDirPath = dirProperty.absolutePath
        val stabilityConfigPath = rootProject.file("shared/compose-stability-config.txt").absolutePath

        compilerOptions.freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=$stabilityConfigPath"
        )

        val composeAnalyticsDir = "$rootDirPath/compose_compiler_analytics"
        compilerOptions.freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$composeAnalyticsDir",
        )
        compilerOptions.freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$composeAnalyticsDir",
        )
    }
}
