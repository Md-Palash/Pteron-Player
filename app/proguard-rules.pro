# Media3 / ExoPlayer ship their own consumer ProGuard rules (they are applied automatically),
# so no blanket keep rule is needed here. The old `-keep class androidx.media3.** { *; }`
# stopped R8 from removing anything inside Media3 -- DASH, HLS, RTSP, SmoothStreaming and
# every other part this offline player never uses stayed in the APK.
-dontwarn androidx.media3.**
