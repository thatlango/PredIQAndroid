plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Firebase configuration is supplied through the protected release path rather than source control.
// Applying the plugin only when the config exists keeps ordinary CI/builds deterministic while
// production builds process app/google-services.json into the resources used by FCM.
if (file("google-services.json").exists()) {
    pluginManager.apply("com.google.gms.google-services")
}

android {
    namespace = "com.getprediq.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.getprediq.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.0"
        buildConfigField("String", "PREDIQ_API_BASE_URL", "\"https://api.getprediq.site/api/v1/\"")
    }

    signingConfigs {
        create("release") {
            val path = System.getenv("PREDIQ_ANDROID_KEYSTORE_FILE")
            val storePass = System.getenv("PREDIQ_ANDROID_KEYSTORE_PASSWORD")
            val alias = System.getenv("PREDIQ_ANDROID_KEY_ALIAS")
            val keyPass = System.getenv("PREDIQ_ANDROID_KEY_PASSWORD")
            if (!path.isNullOrBlank()) storeFile = file(path)
            if (!storePass.isNullOrBlank()) storePassword = storePass
            if (!alias.isNullOrBlank()) keyAlias = alias
            if (!keyPass.isNullOrBlank()) keyPassword = keyPass
        }
    }

    buildTypes {
        release {
            val cfg = signingConfigs.getByName("release")
            val signingReady = cfg.storeFile != null && !cfg.storePassword.isNullOrBlank() && !cfg.keyAlias.isNullOrBlank() && !cfg.keyPassword.isNullOrBlank()
            if (signingReady) signingConfig = cfg
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
}
