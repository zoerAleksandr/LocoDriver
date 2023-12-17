import Versions.accompanist_navigation_animation_version
import Versions.app_compat_version
import Versions.coil_version
import Versions.compose_ui_version
import Versions.core_ktx_version
import Versions.coroutines_version
import Versions.material_compose
import Versions.room_version
import org.gradle.api.JavaVersion

object Plugins {
    const val android_app = "com.android.application"
    const val android_lib = "com.android.library"
    const val kotlin_android = "org.jetbrains.kotlin.android"
    const val kotlin_jvm = "org.jetbrains.kotlin.jvm"
    const val kotlin_kapt = "kapt"
}

object Apps {
    const val application_id = "com.nglauber.architecture_sample"
    const val compile_sdk_version = 34
    const val min_sdk_version = 24
    const val target_sdk_version = 34

    const val version_code = 1
    const val version_name = "1.0.0"

    const val jvm_target_version = "17"
    val java_compatibility_version = JavaVersion.VERSION_17

    const val test_instrumentation_runner = "androidx.test.runner.AndroidJUnitRunner"
}

object Versions {
    const val android_plugin_id = "8.1.1"
    const val accompanist_navigation_animation_version = "0.31.3-beta"
//    const val accompanist_swiperefresh_version = "0.31.3-beta"
//    const val activity_compose_version = "1.7.2"
//    const val androidx_exif_version = "1.3.6"
//    const val androidx_test_core_version = "1.6.0-alpha01"
//    const val androidx_test_ext_kotlin_runner_version = "1.1.5"
    const val app_compat_version = "1.7.0-alpha02"
    const val coil_version = "2.4.0"
    const val compose_ui_version = "1.5.1"
    const val core_ktx_version = "1.12.0"
//    const val core_testing_version = "2.2.0"
    const val coroutines_version = "1.7.1"
//    const val dependencies_check_version = "0.46.0"
//    const val espresso_core_version = "3.6.0-alpha01"
//    const val junit_version = "4.13.2"
    const val kotlin_version = "1.8.21"
    const val kotlin_compiler_ext_version = "1.4.7"
//    const val lifecycle_runtime_version = "2.6.1"
//    const val lifecycle_viewmodel_version = "2.6.1"
    const val material_compose = "1.1.2"
//    const val mockk_version = "1.13.2"
//    const val playservices_version = "20.5.0"
//    const val reveal_swipe_version = "1.1.0"
    const val room_version = "2.6.0"
//    const val test_ext_version = "1.1.5"
//    const val test_runner_version = "1.6.0-alpha01"
//    const val tracing_version = "1.1.0"
}

object Libs {
    // Projects
    const val project_core = ":core"
    const val project_core_android = ":core_android"
    const val project_domain = ":domain"
    const val project_data_local = ":data_local"
    const val project_data_firebase = ":data_firebase"
    const val project_feature_login = ":features:login"
    const val project_feature_books = ":features:books"
    const val project_feature_settings = ":features:settings"

    // Libs
    const val accompanist_navigation_animation =
        "com.google.accompanist:accompanist-navigation-animation:$accompanist_navigation_animation_version"
//    const val accompanist_swipe_refresh =
//        "com.google.accompanist:accompanist-swiperefresh:$accompanist_swiperefresh_version"
//    const val activity_compose =
//        "androidx.activity:activity-compose:$activity_compose_version"
    const val app_compat =
        "androidx.appcompat:appcompat:$app_compat_version"
    const val coil_compose =
        "io.coil-kt:coil-compose:$coil_version"
    const val compose_ui =
        "androidx.compose.ui:ui:$compose_ui_version"
    const val compose_material =
        "androidx.compose.material3:material3:$material_compose"
    const val core_ktx =
        "androidx.core:core-ktx:$core_ktx_version"
//    const val exif_interface =
//        "androidx.exifinterface:exifinterface:$androidx_exif_version"

    const val kotlinx_coroutines_core =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version"
//    const val lifecycle_runtime_ktx =
//        "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_runtime_version"
//    const val lifecycle_viewmodel_ktx =
//        "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_viewmodel_version"
//    const val material =
//        "androidx.compose.material:material:$material_compose"
//    const val play_services_auth =
//        "com.google.android.gms:play-services-auth:$playservices_version"
//    const val reveal_swipe =
//        "de.charlex.compose:revealswipe:$reveal_swipe_version"
    const val room_compiler =
        "androidx.room:room-compiler:$room_version"
    const val room_ktx =
        "androidx.room:room-ktx:$room_version"
    const val room_runtime =
        "androidx.room:room-runtime:$room_version"
//    const val tracing =
//        "androidx.tracing:tracing:$tracing_version"
    const val ui_tooling_preview =
        "androidx.compose.ui:ui-tooling-preview:$compose_ui_version"

//     Debug
    const val ui_tooling =
        "androidx.compose.ui:ui-tooling:$compose_ui_version"
    const val ui_test_manifest =
        "androidx.compose.ui:ui-test-manifest:$compose_ui_version"
}
