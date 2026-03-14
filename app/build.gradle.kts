import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.plugin)
}

// Read signing config from local.properties (gitignored)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.spendshot.android"
    compileSdk = 35 // Or your project's compile SDK

    defaultConfig {
        applicationId = "com.spendshot.android"
        minSdk = 24 // Using a higher minSdk can sometimes help with library sizes
        targetSdk = 35
        versionCode = 21
        versionName = "0.0.21"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // CRITICAL: Filter native binaries to reduce size from ML libraries.
        // This includes binaries only for 32-bit and 64-bit ARM CPUs (most phones).
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // THIS BLOCK IS NO LONGER NEEDED when using the Compose Compiler plugin
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.3"
    // }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    signingConfigs {
        create("release") {
            val ksFile = localProperties.getProperty("RELEASE_STORE_FILE")
            if (ksFile != null) {
                storeFile = file(ksFile)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // Keep your core AndroidX and Compose dependencies
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")

    // Room
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.androidx.room.compiler)

    // Other libraries
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.accompanist:accompanist-drawablepainter:0.34.0")


    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // --- CRITICAL SIZE OPTIMIZATION: Use UNBUNDLED ML Libraries ---
    implementation(libs.play.services.mlkit.text.recognition)
    // Required for .await() on ML Kit's Task<T> in ReceiptProcessor
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")



    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coil 3
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-svg:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // Coil 2 (Stable for Kotlin 2.0.21) - COMMENTED OUT
    // implementation("io.coil-kt:coil-compose:2.7.0")
    // implementation("io.coil-kt:coil-svg:2.7.0")
    

}
