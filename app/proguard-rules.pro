# Gson
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.dousports.app.utils.ExerciseJson { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Hilt / Dagger
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.EntryPoint class * { *; }
-keepclasseswithmembers class * { @javax.inject.Inject <init>(...); }
-keepclasseswithmembers class * { @javax.inject.Inject <fields>; }
-dontwarn dagger.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-dontwarn androidx.room.paging.**

# DataStore
-keep class androidx.datastore.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Coil
-dontwarn coil.**

# App models — évite que R8 supprime les data classes utilisées en runtime
-keep class com.dousports.app.data.local.entity.** { *; }
-keep class com.dousports.app.data.local.dao.** { *; }
-keep class com.dousports.app.data.preferences.** { *; }
