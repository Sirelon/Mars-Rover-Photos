// Single source of truth for the app version across all platforms.
// Android and Desktop read this directly. After bumping, run
// `./gradlew syncIosVersion` to push the values into the Xcode project.
object AppVersion {
    const val name = "5.0.0"
    const val code = 51
}
