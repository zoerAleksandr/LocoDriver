plugins {
    id(Plugins.android_lib)
    id(Plugins.kotlin_android)
}

android {
    namespace = "com.example.route"
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

    implementation(project(Libs.project_core_android))
    implementation(project(Libs.project_domain))

    implementation(Libs.activity_compose)
    implementation(Libs.core_ktx)
    implementation(Libs.lifecycle_viewmodel_ktx)

    implementation(Libs.compose_ui)
    implementation(Libs.ui_tooling_preview)
    implementation(Libs.compose_material)

    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)

    implementation(Libs.reveal_swipe)
    implementation(Libs.constraint_layout)

    implementation(Libs.accompanist_swipe_refresh)
    implementation(Libs.accompanist_navigation_animation)

    implementation(Libs.accompanist_pager)
    implementation(Libs.accompanist_pager_indicator)

    implementation(Libs.coil_compose)
    implementation(Libs.reveal_swipe)
    implementation(Libs.maxkeppeler_sheets)
    implementation(project(mapOf("path" to ":data_local")))

}