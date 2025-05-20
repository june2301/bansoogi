plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ppg"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ppg"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        androidResources.noCompress += "tflite"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.5" }
}

dependencies {
    /* ─── Compose Wear ─ */
    implementation(platform(libs.compose.bom))
    implementation("androidx.wear.compose:compose-material:1.4.1")
    implementation("androidx.wear.compose:compose-foundation:1.4.1")
    implementation("androidx.wear.compose:compose-navigation:1.4.1")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    debugImplementation(libs.ui.tooling)
    implementation(libs.activity.compose)

    /* Core */
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.splashscreen)
    implementation(libs.play.services.wearable)

    /* JSON */
    implementation(libs.json)

    /* Tensor‑lite runtime */
    implementation(libs.litert)

    /* Samsung Health Tracking SDK AAR */
    implementation(files("$rootDir/libs/samsung/health/samsung-health-sensor-api-v1.3.0.aar"))
}