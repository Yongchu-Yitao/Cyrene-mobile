plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ai.cyrene.mobile.runtime"
    compileSdk = 35
    defaultConfig {
        applicationId = "ai.cyrene.mobile.runtime"
        minSdk = 28
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.3"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"
    buildFeatures.buildConfig = true
}

dependencies {
    implementation(project(":runtime-protocol"))
}
