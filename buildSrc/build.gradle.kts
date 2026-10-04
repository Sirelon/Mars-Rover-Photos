plugins {
    `kotlin-dsl`
}

// `kotlin-dsl` pins the Kotlin Gradle plugin embedded in Gradle (2.4.0 for Gradle 9.7),
// which is flagged by GHSA-r937-wjx7-w2jp. Resolve it to the project's Kotlin version
// (gradle/libs.versions.toml, `kotlin`) instead. Drop this once Gradle embeds >= 2.4.20.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

repositories {
    mavenCentral()
}
