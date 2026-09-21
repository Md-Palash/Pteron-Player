// Kotlin compilation is built into the Android Gradle plugin since AGP 9.0, so the
// org.jetbrains.kotlin.android plugin is intentionally NOT applied anymore. The Compose compiler
// plugin below is still needed; declaring it with an explicit version also pins the Kotlin
// Gradle plugin version that AGP's built-in Kotlin uses.
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
