plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") // Firebase plugin
    id("com.google.firebase.crashlytics")
}

/* --- Signing props (works in Kotlin DSL) --- */
val flowrStoreFile: String? = (project.findProperty("FLOWR_STORE_FILE") as String?)
    ?: System.getenv("FLOWR_STORE_FILE")
val flowrStorePassword: String? = (project.findProperty("FLOWR_STORE_PASSWORD") as String?)
    ?: System.getenv("FLOWR_STORE_PASSWORD")
val flowrKeyAlias: String? = (project.findProperty("FLOWR_KEY_ALIAS") as String?)
    ?: System.getenv("FLOWR_KEY_ALIAS")
val flowrKeyPassword: String? = (project.findProperty("FLOWR_KEY_PASSWORD") as String?)
    ?: System.getenv("FLOWR_KEY_PASSWORD")
val hasSigning = !flowrStoreFile.isNullOrBlank()

android {
    namespace = "com.mdksolutions.flowr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mdksolutions.flowr"
        minSdk = 23
        targetSdk = 35
        versionCode = 11
        versionName = "1.0.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        // ✅ required for BuildConfig.DEBUG and your AdUnits logic
        buildConfig = true
        // ✅ Compose
        compose = true
    }

    // 🔐 Signing config using gradle.properties/env
    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(flowrStoreFile!!)
                storePassword = flowrStorePassword
                keyAlias = flowrKeyAlias
                keyPassword = flowrKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["firebase_crashlytics_collection_enabled"] = "false"
            manifestPlaceholders["firebase_analytics_collection_deactivated"] = "true"
            manifestPlaceholders["admob_app_id"] = "ca-app-pub-3940256099942544~3347511713" // TEST
        }
        release {
            // ✅ Enable code shrinking/obfuscation for Play Store builds
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["firebase_crashlytics_collection_enabled"] = "true"
            manifestPlaceholders["firebase_analytics_collection_deactivated"] = "false"
            manifestPlaceholders["admob_app_id"] = "ca-app-pub-3774551279310155~4285760094" // REAL

            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ✅ Compose (use ONE BOM: the libs one)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Material icons (BOM-managed)
    implementation("androidx.compose.material:material-icons-extended")

    // Android UI/support
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // ✅ Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // ✅ Navigation & images
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ✅ Ads / Places
    implementation("com.google.android.gms:play-services-ads:23.4.0")
    implementation("com.google.android.libraries.places:places:3.5.0")

    // ✅ Compose tooling (debug only)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation(libs.androidx.ui.test.manifest)

    // ✅ Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
