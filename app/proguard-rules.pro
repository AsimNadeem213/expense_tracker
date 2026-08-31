# Preserve Line Numbers for Crash Stack Traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
}
-dontwarn androidx.room.paging.**

# --- Firebase Realtime Database & Auth ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName *;
    @com.google.firebase.database.Exclude *;
    @com.google.firebase.database.IgnoreExtraProperties *;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Domain & Data Models (Firebase / Room Serialization DTOs) ---
-keep class com.asim.splitmate.domain.model.** { *; }
-keep class com.asim.splitmate.data.local.entity.** { *; }
-keep class com.asim.splitmate.domain.usecase.DashboardSummary { *; }

# --- Koin Dependency Injection ---
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# --- Kotlin Coroutines & Serialization ---
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepattributes *Annotation*,ElementValuePairs
-keepnames class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
    @kotlinx.serialization.Serializable *;
}

# --- Apache POI (XLSX Export) ---
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn java.awt.**
-dontwarn javax.awt.**
-dontwarn com.graphbuilder.**
-dontwarn javax.xml.stream.**
-dontwarn javax.xml.namespace.**
-dontwarn javax.imageio.**
-dontwarn java.beans.**
-dontwarn org.apache.harmony.**
-dontwarn org.apache.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.bouncycastle.**
-dontwarn com.zaxxer.sparsebitset.**

# --- Coil Image Loading ---
-keep class coil.** { *; }
-dontwarn coil.**