plugins {
    id(Plugins.android_lib)
    id(Plugins.kotlin_android)
    kotlin(Plugins.kotlin_kapt)
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
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }
    compileOptions {
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
    }
    kotlinOptions {
        jvmTarget = Apps.jvm_target_version
    }
}

dependencies {
    implementation(project(Libs.project_domain))
    implementation(project(Libs.project_core_android))

    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)

    implementation(Libs.room_runtime)
    implementation(Libs.room_ktx)
    implementation(Libs.gson)
    kapt(Libs.room_compiler)

    implementation(Libs.datastore_pref)


    androidTestImplementation(TestLibs.ext_junit)
    androidTestImplementation(TestLibs.arch_core)
    androidTestImplementation(TestLibs.test_runner)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}