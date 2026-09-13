# Keep Data Models & Response Objects for Gson & Reflection
-keep class com.example.moodymusicforandroid.data.model.** { *; }
-keepclassmembers class com.example.moodymusicforandroid.data.model.** { *; }

-keep class com.example.moodymusicforandroid.data.api.** { *; }
-keepclassmembers class com.example.moodymusicforandroid.data.api.** { *; }

-keep class com.example.moodymusicforandroid.common.network.** { *; }
-keepclassmembers class com.example.moodymusicforandroid.common.network.** { *; }

# Keep serialized fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# Preserve Generic Signatures and Annotations for Retrofit & Gson
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*

# Keep Retrofit API interfaces completely
-keep interface com.example.moodymusicforandroid.data.api.** { *; }
-keep interface com.example.moodymusicforandroid.common.network.** { *; }
-keepclassmembers interface com.example.moodymusicforandroid.data.api.** {
    @retrofit2.http.* <methods>;
}
-keepclassmembers interface com.example.moodymusicforandroid.common.network.** {
    @retrofit2.http.* <methods>;
}

# Retrofit & OkHttp internal
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-keep,allowobfuscation,allowshrinking class retrofit2.Response