plugins {
    id("com.android.application")
    // No org.jetbrains.kotlin.android here: Kotlin support is built into AGP 9.
    id("org.jetbrains.kotlin.plugin.compose")
}

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

    signingConfigs {
        // Release signing is intentionally NOT hardcoded here.
        // Create app/keystore.properties (gitignored) with:
        //   storeFile=/absolute/path/to/pteron-release.jks
        //   storePassword=...
        //   keyAlias=...
        //   keyPassword=...
        // and uncomment the block below before building a release AAB.
        //
        // create("release") {
        //     val props = java.util.Properties()
        //     val propsFile = rootProject.file("app/keystore.properties")
        //     if (propsFile.exists()) props.load(propsFile.inputStream())
        //     storeFile = props["storeFile"]?.let { file(it) }
        //     storePassword = props["storePassword"] as String?
        //     keyAlias = props["keyAlias"] as String?
        //     keyPassword = props["keyPassword"] as String?
        // }
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
            // signingConfig = signingConfigs.getByName("release")
        }
        // Mirrors `release` (minified + shrunk, so the size is realistic) but signed
        // with the debug key so it installs on a device without a production
        // keystore. Use this build type to actually measure/verify APK size --
        // `assembleDebug` is never representative, since it isn't minified at all.
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
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
