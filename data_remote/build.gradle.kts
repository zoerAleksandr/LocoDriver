import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
//    id(Plugins.android_lib)
//    id(Plugins.kotlin_android)

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
//    kotlinOptions {
//        jvmTarget = Apps.jvm_target_version
//    }
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
    implementation(Libs.rustore_pay)
    implementation(Libs.rustore_billing_client)
    implementation(Libs.rustore_review)
    implementation(Libs.rustore_sdk_appupdate)

    implementation(platform(Libs.ru_ok_tracer_platform))
    implementation(Libs.ru_ok_tracer_tracer_crash_report)
    implementation(Libs.kotlin_x_serialization_json)
    implementation(Libs.core_ktx)
    implementation(Libs.app_compat)
    implementation(Libs.compose_material3)
    implementation(Libs.parse_sdk_android)
    implementation(Libs.parse_sdk_android_coroutine)
    implementation(Libs.work_manager)
    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)
    implementation(Libs.gson)
    implementation(Libs.datastore_preferences)
    implementation(Libs.crypto_tink)
    implementation ("io.ktor:ktor-client-core:3.0.1")
    implementation ("io.ktor:ktor-client-okhttp:3.0.1")
    implementation ("io.ktor:ktor-client-auth:3.0.1")
    implementation ("io.ktor:ktor-client-content-negotiation:3.0.1")
    implementation ("io.ktor:ktor-serialization-gson:3.0.1")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}