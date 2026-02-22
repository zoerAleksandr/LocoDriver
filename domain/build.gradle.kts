import TestLibs.exclude_jetbrains_kotlin
import TestLibs.exclude_mockito
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(Plugins.java_lib)
    id(Plugins.kotlin_jvm)
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = Apps.java_compatibility_version
    targetCompatibility = Apps.java_compatibility_version
}

dependencies {
    api(project(Libs.project_core))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlin_x_serialization_json)
    implementation(Libs.kotlinx_date_time)
    testImplementation(TestLibs.kotlin_test)
    testImplementation (TestLibs.mockito_core)
    testImplementation (TestLibs.mockito_inline)
    testImplementation(TestLibs.mockito_kotlin) {
        exclude(exclude_jetbrains_kotlin)
        exclude(exclude_mockito)
    }
}