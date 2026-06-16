# ============================================================================
# ProGuard / R8 rules for MarsRoverPhotos (androidApp)
#
# R8 is the shrinker/optimizer in use (isMinifyEnabled = true, isShrinkResources
# = true). Most libraries ship their own consumer rules embedded in their AARs,
# so this file only carries what those don't cover or what R8 can't infer from
# bytecode alone (reflection, code-gen, service loaders, native callbacks).
#
# Keep this file lean: prefer narrow, justified keeps over broad ones. A broad
# `-keep class **` defeats the point of shrinking.
# ============================================================================

# ----------------------------------------------------------------------------
# Crash-report quality
# Keep line numbers and source file names so Crashlytics stack traces stay
# readable, then rename the source attribute so we don't leak original names.
# ----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures, annotations and enclosing-method info — required by
# kotlinx.serialization, Ktor type tokens, and reflection-based libraries.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# ----------------------------------------------------------------------------
# kotlinx.serialization
# R8 bundles baseline rules, but generated serializers are referenced
# reflectively via Companion.serializer(); keep them for every @Serializable.
# ----------------------------------------------------------------------------
-keepattributes *Annotation*

# Keep the generated *$$serializer classes and the synthetic Companion holding
# serializer()/INSTANCE for any class annotated @Serializable.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sirelon.marsroverphotos.**$$serializer { *; }
-keepclassmembers class com.sirelon.marsroverphotos.** {
    *** Companion;
}
-keepclasseswithmembers class com.sirelon.marsroverphotos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Enum members are accessed by name during (de)serialization; do not strip them.
-keepclassmembers enum com.sirelon.marsroverphotos.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
# Ktor client (OkHttp engine on Android)
# Ktor selects engines / serialization plugins via reflection & service loaders.
# ----------------------------------------------------------------------------
-keep class io.ktor.client.engine.okhttp.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# OkHttp / Okio (transitive via Ktor) — silence optional-dependency warnings.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ----------------------------------------------------------------------------
# Kotlin coroutines
# ----------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
# ServiceLoader-based main dispatcher / debug probes.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# ----------------------------------------------------------------------------
# Room (androidx.room3)
# The compiler generates *_Impl classes invoked reflectively at runtime.
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# Koin DI — uses reflection to resolve constructors/parameters.
# ----------------------------------------------------------------------------
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(...);
}
-dontwarn org.koin.**

# ----------------------------------------------------------------------------
# Coil 3 image loader
# ----------------------------------------------------------------------------
-dontwarn coil3.**

# ----------------------------------------------------------------------------
# Firebase (Analytics, Crashlytics, Performance, Firestore via GitLive wrapper)
# Firebase ships consumer rules; these only silence reflective-access warnings.
# ----------------------------------------------------------------------------
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepnames class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ----------------------------------------------------------------------------
# Glance app widgets — receivers & workers referenced from the manifest/system.
# ----------------------------------------------------------------------------
-keep class com.sirelon.marsroverphotos.widget.** { *; }
-keep class androidx.glance.appwidget.** { *; }

# ----------------------------------------------------------------------------
# WorkManager — Workers are instantiated reflectively by the framework.
# ----------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class * extends androidx.work.CoroutineWorker { <init>(...); }

# ----------------------------------------------------------------------------
# Compose Multiplatform resources — generated Res accessor & resource readers.
# ----------------------------------------------------------------------------
-keep class com.sirelon.marsroverphotos.shared.resources.** { *; }
-dontwarn org.jetbrains.compose.resources.**

# ----------------------------------------------------------------------------
# App entry points referenced from the manifest.
# ----------------------------------------------------------------------------
-keep class com.sirelon.marsroverphotos.MainActivity { *; }
-keep class com.sirelon.marsroverphotos.MarsRoverApplication { *; }

# ----------------------------------------------------------------------------
# Kotlin metadata & misc warnings.
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-dontwarn javax.annotation.**
