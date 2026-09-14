plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pteron.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pteron.player"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables.useSupportLibrary = true
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

    // Note: this app has no translated strings, only en, so there's no locale
    // filtering config here to prune -- a `localeFilters` block was tried and
    // removed after CI reported it unresolved against this AGP/DSL version;
    // not worth chasing since there's nothing to filter yet anyway.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Media playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    // Powers system media-notification controls, lock-screen playback controls, and
    // headphone/Bluetooth media-button routing (see playback/PlaybackSessionService).
    implementation("androidx.media3:media3-session:1.4.1")

    // Preferences / persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Thumbnail loading & caching
    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
