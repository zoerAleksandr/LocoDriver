import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.android_lib)
    id(Plugins.compose_multiplatform)
    id(Plugins.compose_compiler)
}

kotlin {
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
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        commonMain.dependencies {
            // Compose Multiplatform UI
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // KMP Navigation
            implementation(Libs.navigation_compose_kmp)

            // KMP business logic
            implementation(project(Libs.project_core))
            implementation(project(Libs.project_domain))
            implementation(project(Libs.project_data_local))
            implementation(project(Libs.project_data_remote))

            // DI + utilities
            implementation(Libs.koin_core)
            implementation(Libs.koin_compose)
            implementation(Libs.kotlinx_coroutines_core)
            implementation(Libs.kotlinx_date_time)
        }
        androidMain.dependencies {
            // Android-specific platform implementations
        }
        iosMain.dependencies {
            // iOS-specific platform implementations
        }
    }
}

android {
    namespace = "com.z_company.shared"
    compileSdk = Apps.compile_sdk_version
    defaultConfig {
        minSdk = Apps.min_sdk_version
    }
    compileOptions {
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
    }
}
