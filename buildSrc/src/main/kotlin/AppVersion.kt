// Single source of truth for the app version across all platforms.
// Android and Desktop read this directly. `./gradlew bumpVersion` also syncs the
// values into the Xcode project; use `./gradlew syncIosVersion` only after a
// manual edit here.
object AppVersion {
    const val name = "5.4.0"
    const val code = 58
}
