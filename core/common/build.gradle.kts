plugins {
    alias(libs.plugins.kotlin.jvm)
}
kotlin { jvmToolchain(17) }
dependencies {
    testImplementation(project(":core:testing"))
    implementation(project(":core:model"))
}
