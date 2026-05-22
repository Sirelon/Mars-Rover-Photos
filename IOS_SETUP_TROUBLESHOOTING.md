# iOS Build & Runtime Troubleshooting

This document records every issue hit during the KMP → iOS migration and how each was resolved. Useful reference if the build or app breaks again.

---

## 1. Associated Domains blocking Personal Team signing

**Symptom:** Xcode fails to create a provisioning profile.
> "Provisioning profile: The 'com.apple.developer.associated-domains' entitlement is not allowed for Personal Team."

**Cause:** `iosApp.entitlements` had an empty `com.apple.developer.associated-domains` key left over from a previous configuration. Personal Team certificates do not support this capability.

**Fix:** Removed the key entirely from `iosApp/iosApp/iosApp.entitlements`.

---

## 2. Firebase SPM package resolution failure

**Symptom:** Xcode shows "Missing package product" for all Firebase products; `xcodebuild -resolvePackageDependencies` errors with `http parser error: stream ended at an unexpected time`.

**Cause:** Corrupted SPM package cache on disk.

**Fix:**
```bash
# Delete the corrupted cache
rm -rf ~/Library/Caches/org.swift.swiftpm/
rm -rf ~/Library/Developer/Xcode/DerivedData/

# Re-resolve packages
xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj -scheme iosApp
```

---

## 3. PhaseScriptExecution: Gradle build phase using wrong task name

**Symptom:** Xcode build fails at the "Build KMP Framework" run-script phase.

**Cause:** The script was calling `./gradlew :shared:assembleXCFramework` which is ambiguous (matches both debug and release). The release build runs out of memory with the default JVM heap.

**Fix:** Updated `project.pbxproj` to call the unambiguous debug task and bumped JVM heap in `gradle.properties`:

```
# project.pbxproj shell script
./gradlew :shared:assembleSharedDebugXCFramework
```

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096M -Dkotlin.daemon.jvm.options="-Xmx4096M"
```

---

## 4. Room 3.0 crash: no SQLite driver provided

**Symptom:** App crashes immediately on launch with:
> `IllegalArgumentException: Cannot create a RoomDatabase without providing a SQLiteDriver via setDriver()`

**Cause:** Room 3.0 (the KMP-compatible alpha, package `androidx.room3`) requires an explicit SQLite driver rather than discovering one automatically. The iOS implementation was missing `.setDriver()`.

**Fix:** Added `NativeSQLiteDriver` to `shared/src/iosMain/.../platform/DatabaseBuilder.ios.kt`:

```kotlin
import androidx.sqlite.driver.NativeSQLiteDriver

return Room.databaseBuilder<AppDataBase>(name = dbPath)
    .setDriver(NativeSQLiteDriver())
    .fallbackToDestructiveMigration(false)
    .addMigrations(AppDataBase.migration7To8, AppDataBase.migration8To9)
```

---

## 5. Compose PlistSanityCheck crash

**Symptom:** App crashes during Compose initialisation with `SIGABRT` / `IllegalStateException` from `androidx.compose.ui.uikit.PlistSanityCheck.performIfNeeded`.

**Cause:** Compose Multiplatform 1.11.0 enforces that `CADisableMinimumFrameDurationOnPhone` is set to `true` in `Info.plist`. Without this flag, ProMotion (120 Hz) iPhones are capped at 60 fps and Compose considers this a misconfiguration. The check runs on a background thread shortly after the first frame is composed, so it can look like a random startup crash.

**Fix:** Added to `iosApp/iosApp/Info.plist`:
```xml
<key>CADisableMinimumFrameDurationOnPhone</key>
<true/>
```

> To silence the check without fixing it (not recommended): set `enforceStrictPlistSanityCheck = false` in `ComposeUIViewController`'s `configure` block.

---

## 6. MissingResourceException: font not found at runtime

**Symptom:** App crashes after Compose initialises with:
> `org.jetbrains.compose.resources.MissingResourceException`

The stack trace points to `DefaultIOsResourceReader.readData` trying to load `font/material_symbols_outlined.ttf`.

**Cause:** `DefaultIOsResourceReader` searches for Compose resources inside `app.app/Frameworks/*.framework/composeResources/`. The `shared.xcframework` was correctly linked (appearing in the "Frameworks" build phase) but **not embedded** — the "Embed Frameworks" build phase was empty. As a result, `shared.framework` was never copied into the app bundle, and the font (and all other Compose resources) were unreachable at runtime.

**Fix:** Added `shared.xcframework` to the "Embed Frameworks" (`PBXCopyFilesBuildPhase`) in `project.pbxproj` with `CodeSignOnCopy` and `RemoveHeadersOnCopy` attributes. After this change, `app.app/Frameworks/shared.framework/composeResources/` is present in the installed bundle and all resources resolve correctly.

---

## Summary table

| # | Crash / Error | Root cause | File changed |
|---|---------------|-----------|--------------|
| 1 | Provisioning profile blocked | Empty Associated Domains entitlement | `iosApp.entitlements` |
| 2 | Firebase SPM resolution failure | Corrupted SPM cache | — (cache deleted) |
| 3 | Gradle PhaseScriptExecution failure | Wrong/ambiguous Gradle task + OOM | `project.pbxproj`, `gradle.properties` |
| 4 | Room `IllegalArgumentException` | Missing `NativeSQLiteDriver` | `DatabaseBuilder.ios.kt` |
| 5 | Compose `PlistSanityCheck` crash | Missing `CADisableMinimumFrameDurationOnPhone` | `Info.plist` |
| 6 | `MissingResourceException` on font | `shared.xcframework` not embedded | `project.pbxproj` |
