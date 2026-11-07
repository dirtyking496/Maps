plugins { id("com.android.application") }

android {
    namespace = "ca.xiogps.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "ca.xiogps.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core:1.17.0")
    // MapLibre Native Android map renderer (BSD-2-Clause).
    implementation("org.maplibre.gl:android-sdk:13.6.1")
    // Valhalla Mobile: native Android offline routing engine (MIT).
    implementation("io.github.rallista:valhalla-mobile:0.6.4")
    implementation("io.github.rallista:valhalla-models:0.6.0")
    implementation("io.github.rallista:valhalla-models-config:0.6.0")
}
