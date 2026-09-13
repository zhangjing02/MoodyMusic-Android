# Keep Data Models & Response Objects for Gson & Serialization
-keep class com.example.moodymusicforandroid.data.model.** { *; }
-keepclassmembers class com.example.moodymusicforandroid.data.model.** { *; }

-keep class com.example.moodymusicforandroid.data.api.** { *; }
-keepclassmembers class com.example.moodymusicforandroid.data.api.** { *; }

-keep class com.example.moodymusicforandroid.common.network.** { *; }
-keepclassmembers class com.example.moodymusicforandroid.common.network.** { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# Keep EventBus
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Keep Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keep interface com.example.moodymusicforandroid.data.api.** { *; }
-keep interface com.example.moodymusicforandroid.common.network.** { *; }
-keepclassmembers interface com.example.moodymusicforandroid.data.api.** {
    @retrofit2.http.* <methods>;
}
-keepclassmembers interface com.example.moodymusicforandroid.common.network.** {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep Coil
-keep class coil.** { *; }

# Preserve Line Numbers, Generic Signatures and Annotations for Reflection
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations