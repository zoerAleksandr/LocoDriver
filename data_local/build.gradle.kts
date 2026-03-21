import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.android_lib)
    id(Plugins.sqldelight)
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
            implementation(Libs.sqldelight_coroutines_extensions)
            implementation(Libs.sqldelight_primitive_adapters)
        }
        androidMain.dependencies {
            implementation(project(Libs.project_core_android))
            implementation(Libs.koin_android)
            implementation(Libs.koin_androidx_compose)
            implementation(Libs.sqldelight_android_driver)
            implementation(Libs.datastore_pref)
        }
        iosMain.dependencies {
            implementation(Libs.sqldelight_native_driver)
        }
        commonTest.dependencies {
            implementation(TestLibs.kotlin_test)
        }
    }
}

android {
    namespace = "com.z_company.data_local"
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

sqldelight {
    databases {
        create("RouteDatabase") {
            packageName.set("com.z_company.data_local.route.db")
            srcDirs.setFrom("src/commonMain/sqldelight/RouteDatabase")
            version = 4
            verifyMigrations.set(false)
        }
        create("SettingsDatabase") {
            packageName.set("com.z_company.data_local.setting.db")
            srcDirs.setFrom("src/commonMain/sqldelight/SettingsDatabase")
            version = 2
            verifyMigrations.set(false)
        }
        create("SalarySettingDatabase") {
            packageName.set("com.z_company.data_local.setting.salarydb")
            srcDirs.setFrom("src/commonMain/sqldelight/SalarySettingDatabase")
            verifyMigrations.set(false)
        }
        create("SearchResponseDatabase") {
            packageName.set("com.z_company.data_local.route.searchdb")
            srcDirs.setFrom("src/commonMain/sqldelight/SearchResponseDatabase")
            verifyMigrations.set(false)
        }
    }
}
