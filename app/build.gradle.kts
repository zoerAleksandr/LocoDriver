import org.gradle.kotlin.dsl.invoke
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion


plugins {
    id(Plugins.android_app)
    id(Plugins.kotlin_android)
    id(Plugins.ksp)
    id("ru.ok.tracer").version(Versions.ru_ok_tracer_platform_ver)
    id(Plugins.compose_compiler)
    id(Plugins.vkIdManifest)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    val properties = Properties()
    properties.load(project.rootProject.file("secret.properties").inputStream())
    val filePath: String = properties.getProperty("storeFile") ?: ""
    val storePass: String = properties.getProperty("storePassword") ?: ""
    val keyAliasValue: String = properties.getProperty("keyAlias") ?: ""
    val keyPass: String = properties.getProperty("keyPassword") ?: ""

    signingConfigs {
        create("release") {
            storeFile = file(filePath)
            storePassword = storePass
            keyAlias = keyAliasValue
            keyPassword = keyPass
        }
    }
    namespace = "com.z_company.loco_driver"
    compileSdk = Apps.compile_sdk_version

    defaultConfig {
        buildConfigField(
            type = "String",
            name = "MERCHANT_LOGIN",
            value = "\"${properties.getProperty("MERCHANT_LOGIN", "default_value")}\""
        )
        buildConfigField(
            type = "String",
            name = "PASSWORD_1",
            value = "\"${properties.getProperty("PASSWORD_1", "default_value")}\""
        )
        buildConfigField(
            type = "String",
            name = "PASSWORD_2",
            value = "\"${properties.getProperty("PASSWORD_2", "default_value")}\""
        )
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

    defaultConfig {

        applicationId = Apps.application_id
        minSdk = Apps.min_sdk_version
        targetSdk = Apps.target_sdk_version
        versionCode = Apps.version_code
        versionName = Apps.version_name

        testInstrumentationRunner = Apps.test_instrumentation_runner
        vectorDrawables {
            useSupportLibrary = true
        }
        signingConfig = signingConfigs.getByName("release")
    }

    compileOptions {
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
        isCoreLibraryDesugaringEnabled = true

    }
//    kotlinOptions {
//        jvmTarget = Apps.jvm_target_version
//    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(Apps.jvm_target_version)
    }
}

tracer {
    create("defaultConfig") {
        pluginToken = "fXDqsEd0kMKfFaGKjIOyiNSkthd3jXtkvvvP1Ckevvf0"
        appToken = "1io5grTlupk1xA2hlnGu4uthwGqLCge4HE5xHMfTgYH0"
    }
}


dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(project(Libs.project_robokassa_sdk))
    implementation (Libs.ksp_api)
    implementation(Libs.mytracker_sdk)
    implementation(platform(Libs.rustore_bom))
    implementation(Libs.rustore_pay)
    implementation(Libs.rustore_billing_client)
    implementation(Libs.rustore_sdk_appupdate)

    implementation(platform(Libs.ru_ok_tracer_platform))
    implementation(Libs.ru_ok_tracer_tracer_crash_report)
    implementation(Libs.ru_ok_tracer_tracer_heap_dumps)

    implementation(project(Libs.project_core_android))
    implementation(project(Libs.project_domain))
    implementation(project(Libs.project_data_local))
    implementation(project(Libs.project_feature_login))
    implementation(project(Libs.project_feature_route))
    implementation(project(Libs.project_feature_settings))
    implementation(project(Libs.project_data_remote))
    implementation(Libs.parse_sdk_android)
    implementation(Libs.vkId)
    implementation(Libs.appwrite)
    implementation(Libs.splash_screen)
    implementation(Libs.core_ktx)
    implementation(Libs.lifecycle_runtime_ktx)
    implementation(Libs.activity_compose)
    implementation(Libs.compose_ui)
    implementation(Libs.ui_tooling_preview)
    implementation(Libs.compose_material3)
    implementation(Libs.kotlin_x_serialization_json)

    implementation(Libs.accompanist_navigation_animation)

    implementation(Libs.koin_core)
    implementation(Libs.koin_android)
    implementation(Libs.koin_androidx_compose)
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    debugImplementation(Libs.ui_tooling)
    debugImplementation(Libs.ui_test_manifest)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
configurations.all {
    exclude (group = "com.squareup.okhttp3", module = "okhttp-bom")
}