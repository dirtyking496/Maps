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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core:1.17.0")
}
