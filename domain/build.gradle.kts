import TestLibs.exclude_jetbrains_kotlin
import TestLibs.exclude_mockito

plugins {
    id(Plugins.java_lib)
    id(Plugins.kotlin_jvm)
}

java {
    sourceCompatibility = Apps.java_compatibility_version
    targetCompatibility = Apps.java_compatibility_version
}

dependencies {
    api(project(Libs.project_core))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.koin_core)
    testImplementation(TestLibs.kotlin_test)
    testImplementation (TestLibs.mockito_core)
    testImplementation (TestLibs.mockito_inline)
    testImplementation(TestLibs.mockito_kotlin) {
        exclude(exclude_jetbrains_kotlin)
        exclude(exclude_mockito)
    }
}