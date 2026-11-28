import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    jvmToolchain(17)

    sourceSets {
        main {
            kotlin.srcDirs("src/jvmMain/kotlin")
            resources.srcDirs("src/jvmMain/resources")
        }
    }
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Main dispatcher for desktop (Swing event loop)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.sirelon.marsroverphotos.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Mars Rover Photos"
            packageVersion = AppVersion.name

            description = "Explore Mars through NASA rover photos"
            vendor = "Sirelon"

            macOS {
                bundleID = "com.sirelon.marsroverphotos.desktop"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}

/**
 * dev.gitlive:firebase-java-sdk, the desktop Firebase shim that :shared pulls in, bundles its own
 * older copy of androidx.savedstate. Whenever that jar lands ahead of the real savedstate-desktop
 * on the classpath, the window crashes on start with `NoSuchFieldError ...
 * SavedStateRegistryController.Companion`; Gradle orders the classpath by graph traversal, so a
 * dependency bump anywhere can flip it. This transform drops the bundled copy from that one jar and
 * passes every other jar through untouched.
 */
abstract class StripBundledSavedState : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (!input.name.startsWith("firebase-java-sdk-")) {
            outputs.file(input)
            return
        }
        val output = outputs.file("${input.nameWithoutExtension}-without-savedstate.jar")
        ZipInputStream(input.inputStream().buffered()).use { zipIn ->
            ZipOutputStream(output.outputStream().buffered()).use { zipOut ->
                generateSequence { zipIn.nextEntry }
                    .filterNot { it.name.startsWith("androidx/savedstate/") }
                    .forEach { entry ->
                        zipOut.putNextEntry(ZipEntry(entry.name))
                        zipIn.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
            }
        }
    }
}

val savedStateStripped = Attribute.of("marsrover.savedStateStripped", Boolean::class.javaObjectType)

dependencies {
    attributesSchema { attribute(savedStateStripped) }
    artifactTypes.getByName("jar") { attributes.attribute(savedStateStripped, false) }
    registerTransform(StripBundledSavedState::class) {
        from.attribute(savedStateStripped, false).attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
        to.attribute(savedStateStripped, true).attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
    }
}

// Hot reload launches from its own composeHotReload*RuntimeClasspath configurations, so match all of
// them, not only the plain `run` task's runtimeClasspath.
configurations
    .matching { it.isCanBeResolved && it.name.endsWith("untimeClasspath") }
    .configureEach { attributes.attribute(savedStateStripped, true) }
