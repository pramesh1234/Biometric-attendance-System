plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.attendance.core.device"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    implementation(project(":core:domain"))
    implementation(libs.opencv)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.core.ktx)
}

// Use the same disposable local debug key for the device regression APK.
tasks.matching { it.name == "validateSigningDebugAndroidTest" }.configureEach {
    dependsOn(":app:createLocalDebugKeystore")
}
