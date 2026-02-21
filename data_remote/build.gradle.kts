import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
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
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(Apps.jvm_target_version)
    }
}

dependencies {
    implementation(project(Libs.project_domain))
    implementation(project(Libs.project_core_android))
    implementation(project(Libs.project_data_local))

    implementation(Libs.oneTapVkId)
    implementation(Libs.vkId)

    implementation(platform(Libs.rustore_bom))
    implementation(Libs.rustore_review)

    implementation(platform(Libs.ru_ok_tracer_platform))
    implementation(Libs.ru_ok_tracer_tracer_crash_report)
    implementation(Libs.kotlin_x_serialization_json)
    implementation(Libs.core_ktx)
    implementation(Libs.app_compat)
    implementation(Libs.compose_material3)
    implementation(Libs.work_manager)
    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)
    implementation(Libs.datastore_preferences)
    implementation(Libs.crypto_tink)

    // Ktor (заменяет Retrofit + OkHttp + Gson)
    implementation(Libs.ktor_client_android)
    implementation(Libs.ktor_client_content_negotiation)
    implementation(Libs.ktor_serialization_kotlinx_json)
    implementation(Libs.ktor_client_logging)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
