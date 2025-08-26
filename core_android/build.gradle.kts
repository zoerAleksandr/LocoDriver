
plugins {
    id(Plugins.android_lib)
    id(Plugins.kotlin_android)
    id(Plugins.compose_compiler)
}

android {
    namespace = "com.z_company.core"
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
    }
    kotlinOptions {
        jvmTarget = Apps.jvm_target_version
    }
    buildFeatures {
        compose = true
    }
//    composeOptions {
//        kotlinCompilerExtensionVersion = Versions.kotlin_compiler_ext_version
//    }
}

dependencies {
    api(project(Libs.project_core))

    implementation(project(Libs.project_domain))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.accompanist_navigation_animation)
    implementation(Libs.ui_tooling_preview)
    implementation(Libs.core_ktx)
    implementation(Libs.app_compat)
    implementation(Libs.compose_material3)
    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)
    implementation(Libs.kotlinx_date_time)

    coreLibraryDesugaring(Libs.desugaring)

    testImplementation(TestLibs.kotlin_test)
}