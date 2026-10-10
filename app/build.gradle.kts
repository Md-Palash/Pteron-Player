plugins {
    id("com.android.application")
    // No org.jetbrains.kotlin.android here: Kotlin support is built into AGP 9.
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing values come from environment variables (GitHub Actions) or from
// app/keystore.properties (gitignored, for building on your own computer):
//   storeFile=/absolute/path/to/pteron-release.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
val keystoreProps = java.util.Properties().apply {
    val propsFile = rootProject.file("app/keystore.properties")
    if (propsFile.exists()) propsFile.inputStream().use { load(it) }
}

fun signingValue(env: String, key: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(key)

android {
    namespace = "com.pteron.player"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pteron.player"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    // The app only ships English strings, so keep the translations bundled inside
    // libraries (media3, material, ...) out of the APK.
    androidResources {
        localeFilters += "en"
    }

    // The dependency list that Gradle normally embeds (encrypted) in every APK is only useful for
    // Google Play's own tooling; leaving it out makes the APK a little smaller.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        // Only created when a keystore is available, so a plain `assembleDebug` still works anywhere.
        val storePath = signingValue("KEYSTORE_PATH", "storeFile")
        if (storePath != null) {
            create("release") {
                storeFile = file(storePath)
                storePassword = signingValue("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signed with your own key when one is configured (this is the APK to install and share).
            signingConfig = signingConfigs.findByName("release")
        }
        // Mirrors `release` (minified + shrunk, so the size is realistic). Signed with the release
        // key when available, otherwise with the debug key so it still installs for size checks.
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            applicationIdSuffix = ".benchmark"
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Debug/tooling metadata that is never read at runtime.
            excludes += "/DebugProbesKt.bin"
            excludes += "/kotlin-tooling-metadata.json"
        }
    }
}

// Built-in Kotlin: the Kotlin compiler options live in the top-level `kotlin {}` block, and the
// JVM target follows compileOptions above (17).
kotlin {
    compilerOptions {
        // Drops the null-check intrinsics Kotlin inserts into every function/call site.
        // Slightly smaller and faster code; they only guard against Java callers passing null.
        freeCompilerArgs.addAll(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // The icon libraries stopped shipping with the rest of Compose after 1.7, so this one is
    // pinned to its final version instead of following the BOM.
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.9.7")

    // Media playback
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
    implementation("androidx.media3:media3-common:1.10.1")
    // Powers system media-notification controls, lock-screen playback controls, and
    // headphone/Bluetooth media-button routing (see playback/PlaybackSessionService).
    implementation("androidx.media3:media3-session:1.10.1")

    // Preferences / persistence
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Thumbnail loading & caching
    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
