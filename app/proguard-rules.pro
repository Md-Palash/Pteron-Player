# Media3 / ExoPlayer keep rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.pteron.player.**$$serializer { *; }
-keepclassmembers class com.pteron.player.** { *** Companion; }
-keepclasseswithmembers class com.pteron.player.** { kotlinx.serialization.KSerializer serializer(...); }
