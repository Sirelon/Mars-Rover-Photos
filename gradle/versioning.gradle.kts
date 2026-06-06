// Applied from the root build.gradle.kts via `apply(from = "gradle/versioning.gradle.kts")`.
//
// Single source of truth for the app version is buildSrc/AppVersion.kt.
// Android and Desktop read it directly; iOS can't, so these tasks push it into
// the Xcode project.
//
// Writes name/code into iosApp's project.pbxproj. Returns true if anything changed.
fun writeIosVersion(pbxproj: File, name: String, code: Int): Boolean {
    require(pbxproj.exists()) { "Not found: ${pbxproj.path}" }
    val original = pbxproj.readText()
    val updated = original
        .replace(Regex("""MARKETING_VERSION = [^;]+;"""), "MARKETING_VERSION = $name;")
        .replace(Regex("""CURRENT_PROJECT_VERSION = [^;]+;"""), "CURRENT_PROJECT_VERSION = $code;")
    if (updated != original) pbxproj.writeText(updated)
    return updated != original
}

// Sync iOS to the version currently in AppVersion.kt.
// Run ./gradlew syncIosVersion if you edit AppVersion.kt by hand.
tasks.register("syncIosVersion") {
    group = "versioning"
    description = "Sync iOS version in project.pbxproj from AppVersion.kt"
    notCompatibleWithConfigurationCache("Rewrites version files imperatively")
    val pbxproj = rootProject.file("iosApp/iosApp.xcodeproj/project.pbxproj")
    doLast {
        val changed = writeIosVersion(pbxproj, AppVersion.name, AppVersion.code)
        val state = if (changed) "Updated" else "Already up to date"
        println("$state iOS version -> ${AppVersion.name} (${AppVersion.code})")
    }
}

// Bump the app version in AppVersion.kt and sync iOS in one shot.
//   ./gradlew bumpVersion                       -> minor bump (3.0.0 -> 3.1.0), code +1
//   ./gradlew bumpVersion -PversionName=5.0.0   -> set name to 5.0.0,        code +1
tasks.register("bumpVersion") {
    group = "versioning"
    description = "Increment build code and bump version name (minor by default, or -PversionName=x.y.z)"
    notCompatibleWithConfigurationCache("Rewrites version files imperatively")
    val appVersionFile = rootProject.file("buildSrc/src/main/kotlin/AppVersion.kt")
    val pbxproj = rootProject.file("iosApp/iosApp.xcodeproj/project.pbxproj")
    val requestedName = (project.findProperty("versionName") as String?)?.trim()
    doLast {
        require(appVersionFile.exists()) { "Not found: ${appVersionFile.path}" }

        val newName = when {
            !requestedName.isNullOrEmpty() -> {
                require(Regex("""\d+\.\d+\.\d+""").matches(requestedName)) {
                    "versionName must be in major.minor.patch form, got: $requestedName"
                }
                requestedName
            }
            else -> {
                val (major, minor, _) = AppVersion.name.split(".").map { it.toInt() }
                "$major.${minor + 1}.0"
            }
        }
        val newCode = AppVersion.code + 1

        appVersionFile.writeText(
            appVersionFile.readText()
                .replace(Regex("""const val name = "[^"]*""""), """const val name = "$newName"""")
                .replace(Regex("""const val code = \d+"""), "const val code = $newCode")
        )
        writeIosVersion(pbxproj, newName, newCode)

        println("Bumped version: ${AppVersion.name} (${AppVersion.code}) -> $newName ($newCode)")
    }
}
