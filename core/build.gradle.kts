import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.android_lib)
}

kotlin {
    jvm()

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = JvmTarget.fromTarget(Apps.jvm_target_version)
                }
            }
        }
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(Libs.kotlinx_coroutines_core)
        }
    }
}

android {
    namespace = "com.z_company.core.common"
    compileSdk = Apps.compile_sdk_version
    defaultConfig {
        minSdk = Apps.min_sdk_version
    }
    compileOptions {
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
    }
}
