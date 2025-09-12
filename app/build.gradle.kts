plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") // Firebase plugin
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.mdksolutions.flowr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mdksolutions.flowr"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // ✅ Speed up DEBUG startup (skip analytics/crash init)
            manifestPlaceholders["firebase_crashlytics_collection_enabled"] = "false"
            manifestPlaceholders["firebase_analytics_collection_deactivated"] = "true"
        }
        release {
            // Keeping your current setting (no minify) to avoid behavioral changes
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ✅ Enable collection in release
            manifestPlaceholders["firebase_crashlytics_collection_enabled"] = "true"
            manifestPlaceholders["firebase_analytics_collection_deactivated"] = "false"
            // If/when you want smaller APKs, also set:
            // isMinifyEnabled = true
            // isShrinkResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.compose.material:material-icons-extended:<compose_version>")

    // ✅ Firebase BOM (pick one BOM version and keep it once; you currently have two below)
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ✅ Firebase Services
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // ✅ Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ✅ Coil for Image Loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Other Dependencies
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // (You have duplicates below; consider removing them to avoid confusion)
    // implementation("com.google.firebase:firebase-firestore-ktx")
    // implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // ✅ Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
