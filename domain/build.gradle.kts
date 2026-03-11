import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.android_lib)
    id("org.jetbrains.kotlin.plugin.serialization")
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
            api(project(Libs.project_core))
            implementation(Libs.kotlinx_coroutines_core)
            implementation(Libs.kotlin_x_serialization_json)
            implementation(Libs.kotlinx_date_time)
        }
        commonTest.dependencies {
            implementation(TestLibs.kotlin_test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines_version}")
        }
    }
}

android {
    namespace = "com.z_company.domain"
    compileSdk = Apps.compile_sdk_version
    defaultConfig {
        minSdk = Apps.min_sdk_version
    }
    compileOptions {
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
    }
}