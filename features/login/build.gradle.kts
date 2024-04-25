import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    id(Plugins.android_lib)
    id(Plugins.kotlin_android)
}

android {
    namespace = "com.z_company.login"
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
    kotlinOptions {
        jvmTarget = Apps.jvm_target_version
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.kotlin_compiler_ext_version
    }
}

dependencies {
    implementation(project(Libs.project_domain))
    implementation(project(Libs.project_core_android))
    implementation(project(Libs.project_data_remote))
    implementation(Libs.parse_sdk_android)
    implementation(Libs.vkid)
    implementation(Libs.vkid_onetap)
    implementation(Libs.activity_compose)
    implementation(Libs.core_ktx)
    implementation(Libs.lifecycle_viewmodel_ktx)

    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)

    implementation(Libs.compose_ui)
    implementation(Libs.ui_tooling_preview)
    implementation(Libs.compose_material)

    implementation(Libs.accompanist_navigation_animation)
}