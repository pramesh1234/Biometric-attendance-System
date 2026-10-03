plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("test") {
    group = "verification"
    description = "Run domain coverage gates, JVM tests, Android lint and assemble the app."
    dependsOn(":core:domain:check", ":core:common:test", ":core:data:testDebugUnitTest",
        ":feature:onboarding:testDebugUnitTest", ":feature:admin:testDebugUnitTest",
        ":feature:staff:testDebugUnitTest", ":app:testDebugUnitTest",
        ":app:lintDebug", ":app:assembleDebug")
}
