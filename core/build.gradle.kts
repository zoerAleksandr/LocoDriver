plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = Apps.java_compatibility_version
    targetCompatibility = Apps.java_compatibility_version
}

dependencies {
    implementation(Libs.kotlinx_coroutines_core)
}