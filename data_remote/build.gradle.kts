import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.android_lib)
    id("org.jetbrains.kotlin.plugin.serialization")
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
        commonMain.dependencies {
            api(project(Libs.project_domain))
            implementation(project(Libs.project_core))
            implementation(Libs.kotlinx_coroutines_core)
            implementation(Libs.koin_core)
            implementation(Libs.kotlin_x_serialization_json)
            implementation(Libs.kotlinx_date_time)
            // Ktor KMP-core (движок — через expect/actual)
            implementation(Libs.ktor_client_core)
            implementation(Libs.ktor_client_content_negotiation)
            implementation(Libs.ktor_serialization_kotlinx_json)
            implementation(Libs.ktor_client_logging)
        }

        androidMain.dependencies {
            implementation(project(Libs.project_core_android))
            implementation(Libs.koin_android)
            implementation(Libs.koin_androidx_compose)
            // Android Ktor engine
            implementation(Libs.ktor_client_android)
            // Android-specific
            implementation(Libs.oneTapVkId)
            implementation(Libs.vkId)
            implementation(Libs.core_ktx)
            implementation(Libs.app_compat)
            implementation(Libs.compose_material3)
            implementation(Libs.work_manager)
            implementation(Libs.datastore_preferences)
            implementation(Libs.crypto_tink)
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                // iOS Ktor engine (Darwin / NSURLSession)
                implementation(Libs.ktor_client_darwin)
            }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        commonTest.dependencies {
            implementation(TestLibs.kotlin_test)
        }
    }
}

android {
    namespace = "com.z_company.data_remote"
    compileSdk = Apps.compile_sdk_version

    defaultConfig {
        minSdk = Apps.min_sdk_version
        testInstrumentationRunner = Apps.test_instrumentation_runner
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
    }
}
