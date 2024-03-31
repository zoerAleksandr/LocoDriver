import java.util.Properties

plugins {
    id(Plugins.android_app)
    id(Plugins.kotlin_android)
    kotlin(Plugins.kotlin_kapt)
}

android {
    namespace = "com.z_company.loco_driver"
    compileSdk = Apps.compile_sdk_version
    val properties = Properties()
    properties.load(project.rootProject.file("secret.properties").inputStream())

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    defaultConfig {
//        val VKIDClientSecret = properties.getProperty("VKIDClientSecret")
//        val VKIDClientID = properties.getProperty("VKIDClientID")

        addManifestPlaceholders(
            mapOf(
                "VKIDRedirectHost" to "vk.com",
                "VKIDRedirectScheme" to "vk51884740",
                "VKIDClientID" to "51884740",
                "VKIDClientSecret" to "l3G9HVocppd94ooNSBSs"
            )
        )

        applicationId = Apps.application_id
        minSdk = Apps.min_sdk_version
        targetSdk = Apps.target_sdk_version
        versionCode = Apps.version_code
        versionName = Apps.version_name

        testInstrumentationRunner = Apps.test_instrumentation_runner
        vectorDrawables {
            useSupportLibrary = true
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
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.kotlin_compiler_ext_version
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(Libs.project_core_android))
    implementation(project(Libs.project_domain))
    implementation(project(Libs.project_data_local))
    implementation(project(Libs.project_feature_login))
    implementation(project(Libs.project_feature_route))
    implementation(project(Libs.project_feature_settings))
    implementation(project(Libs.project_data_remote))
    implementation(Libs.parse_sdk_android)
    implementation(Libs.vkid)
    implementation(Libs.appwrite)
    implementation(Libs.splash_screen)
    implementation(Libs.core_ktx)
    implementation(Libs.lifecycle_runtime_ktx)
    implementation(Libs.activity_compose)
    implementation(Libs.compose_ui)
    implementation(Libs.ui_tooling_preview)
    implementation(Libs.material)

    implementation(Libs.accompanist_navigation_animation)

    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)

    debugImplementation(Libs.ui_tooling)
    debugImplementation(Libs.ui_test_manifest)
}