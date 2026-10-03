plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.attendance.core.data"
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
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    testImplementation(project(":core:testing"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(libs.androidx.room.ktx)
}

tasks.matching { it.name == "validateSigningDebugAndroidTest" }.configureEach {
    dependsOn(":app:createLocalDebugKeystore")
}
