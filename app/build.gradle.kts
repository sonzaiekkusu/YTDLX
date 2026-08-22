import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

android {
    namespace = "com.sonzaiekkusu.ytdlx"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonzaiekkusu.ytdlx"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            keystoreProperties["storeFile"]?.toString()?.let { storeFilePath ->
                storeFile = file(storeFilePath)
                storePassword = keystoreProperties["storePassword"]?.toString()
                keyAlias = keystoreProperties["keyAlias"]?.toString()
                keyPassword = keystoreProperties["keyPassword"]?.toString()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            isDebuggable = false
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                // Allows CI/other checkouts to build until a release keystore is supplied.
                signingConfigs.getByName("debug")
            }
        }

        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Android-native yt-dlp runtime, following the YTDLnis/youtubedl-android approach.
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
