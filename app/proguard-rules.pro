# ProGuard rules for Sloosh Android TV
-keep class com.sloosh.tv.data.api.** { *; }
-keep class com.sloosh.tv.data.db.** { *; }
-keep class com.sloosh.tv.data.alloha.** { *; }
-keep class com.sloosh.tv.data.update.** { *; }

# Gson serialization rules
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# OkHttp rules
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Media3 & ExoPlayer rules
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

