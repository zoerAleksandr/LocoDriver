import Versions.accompanist_navigation_animation_version
import Versions.accompanist_pager_version
import Versions.accompanist_swiperefresh_version
import Versions.accompanist_ui_controller_version
import Versions.activity_compose_version
import Versions.app_compat_version
import Versions.appwrite_ver
import Versions.camera_ver
import Versions.coil_version
import Versions.compose_ui_version
import Versions.constraint_layout_ver
import Versions.core_ktx_version
import Versions.core_testing_version
import Versions.coroutines_version
import Versions.datastore_pref_ver
import Versions.desugaring_ver
import Versions.gson_version
import Versions.koin_version
import Versions.lifecycle_runtime_version
import Versions.lifecycle_viewmodel_version
import Versions.material_compose3
import Versions.maxkeppeler_sheets_ver
import Versions.mockito_core_ver
import Versions.mockito_inline_ver
import Versions.mockito_kotlin_ver
import Versions.parse_sdk_android_ver
import Versions.permission_accompanist_ver
import Versions.rebugger_ver
import Versions.reveal_swipe_version
import Versions.room_version
import Versions.splash_screen_ver
import Versions.test_ext_version
import Versions.test_runner_version
import Versions.vkid_ver
import Versions.work_manager_version
import org.gradle.api.JavaVersion

object Plugins {
    const val android_app = "com.android.application"
    const val android_lib = "com.android.library"
    const val kotlin_android = "org.jetbrains.kotlin.android"
    const val kotlin_jvm = "org.jetbrains.kotlin.jvm"
    const val kotlin_kapt = "kapt"
    const val java_lib = "java-library"
    const val google_relay = "com.google.relay"
}

object Apps {
    const val application_id = "com.z_company.loco_driver"
    const val compile_sdk_version = 34
    const val min_sdk_version = 24
    const val target_sdk_version = 34

    const val version_code = 19
    const val version_name = "1.2.4"

    const val jvm_target_version = "17"
    val java_compatibility_version = JavaVersion.VERSION_17

    const val test_instrumentation_runner = "androidx.test.runner.AndroidJUnitRunner"
}

object Versions {
    const val android_plugin_id = "8.1.1"
    const val accompanist_navigation_animation_version = "0.31.3-beta"
    const val accompanist_swiperefresh_version = "0.31.3-beta"
    const val activity_compose_version = "1.9.0"
    const val app_compat_version = "1.7.0-alpha02"
    const val coil_version = "2.4.0"
    const val compose_ui_version = "1.5.1"
    const val core_ktx_version = "1.12.0"
    const val core_testing_version = "2.2.0"
    const val coroutines_version = "1.7.1"
    const val kotlin_version = "1.8.21"
    const val kotlin_compiler_ext_version = "1.4.7"
    const val lifecycle_runtime_version = "2.6.1"
    const val lifecycle_viewmodel_version = "2.6.1"
    const val material_compose3 = "1.2.1"
    const val reveal_swipe_version = "1.2.0"
    const val room_version = "2.6.0"
    const val test_ext_version = "1.1.5"
    const val test_runner_version = "1.6.0-alpha01"
    const val koin_version = "3.5.6"
    const val accompanist_pager_version = "0.13.0"
    const val gson_version = "2.9.0"
    const val constraint_layout_ver = "1.0.1"
    const val datastore_pref_ver = "1.0.0"
    const val maxkeppeler_sheets_ver = "1.1.1"
    const val camera_ver = "1.3.1"
    const val permission_accompanist_ver = "0.31.0-alpha"
    const val google_relay_ver = "0.3.11"
    const val appwrite_ver = "4.0.0"

    const val splash_screen_ver = "1.0.1"
    const val vkid_ver = "1.3.2"
    const val parse_sdk_android_ver = "4.3.0"
    const val work_manager_version = "2.9.0"
    const val accompanist_ui_controller_version = "0.35.0-alpha"
    const val rebugger_ver = "1.0.0-rc03"
    const val desugaring_ver = "2.0.4"
    const val mockito_core_ver= "3.10.0"
    const val mockito_inline_ver= "2.8.9"
    const val mockito_kotlin_ver= "1.5.0"
}

object Libs {
    // Projects
    const val project_core = ":core"
    const val project_core_android = ":core_android"
    const val project_domain = ":domain"
    const val project_data_local = ":data_local"
    const val project_data_remote = ":data_remote"
    const val project_feature_login = ":features:login"
    const val project_feature_route = ":features:route"
    const val project_feature_settings = ":features:settings"

    // Libs
    const val desugaring = "com.android.tools:desugar_jdk_libs:$desugaring_ver"
    const val rebugger = "io.github.theapache64:rebugger:$rebugger_ver"
    const val datastore_pref = "androidx.datastore:datastore-preferences:$datastore_pref_ver"
    const val constraint_layout =
        "androidx.constraintlayout:constraintlayout-compose:$constraint_layout_ver"
    const val accompanist_navigation_animation =
        "com.google.accompanist:accompanist-navigation-animation:$accompanist_navigation_animation_version"
    const val accompanist_swipe_refresh =
        "com.google.accompanist:accompanist-swiperefresh:$accompanist_swiperefresh_version"
    const val activity_compose =
        "androidx.activity:activity-compose:$activity_compose_version"
    const val app_compat =
        "androidx.appcompat:appcompat:$app_compat_version"
    const val coil_compose =
        "io.coil-kt:coil-compose:$coil_version"
    const val compose_ui =
        "androidx.compose.ui:ui:$compose_ui_version"
    const val compose_material3 =
        "androidx.compose.material3:material3:$material_compose3"
    const val core_ktx =
        "androidx.core:core-ktx:$core_ktx_version"
    const val kotlinx_coroutines_core =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version"
    const val lifecycle_runtime_ktx =
        "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_runtime_version"
    const val lifecycle_viewmodel_ktx =
        "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_viewmodel_version"
    const val reveal_swipe =
        "de.charlex.compose:revealswipe:$reveal_swipe_version"
    const val room_compiler =
        "androidx.room:room-compiler:$room_version"
    const val room_ktx =
        "androidx.room:room-ktx:$room_version"
    const val room_runtime =
        "androidx.room:room-runtime:$room_version"
    const val ui_tooling_preview =
        "androidx.compose.ui:ui-tooling-preview:$compose_ui_version"
    const val koin_core =
        "io.insert-koin:koin-core:$koin_version"
    const val koin_android =
        "io.insert-koin:koin-android:$koin_version"
    const val koin_androidx_compose =
        "io.insert-koin:koin-androidx-compose:$koin_version"
    const val accompanist_pager =
        "com.google.accompanist:accompanist-pager:$accompanist_pager_version"
    const val accompanist_pager_indicator =
        "com.google.accompanist:accompanist-pager-indicators:$accompanist_pager_version"
    const val accompanist_ui_controller =
        "com.google.accompanist:accompanist-systemuicontroller:$accompanist_ui_controller_version"
    const val gson =
        "com.google.code.gson:gson:$gson_version"
    const val maxkeppeler_sheets =
        "com.maxkeppeler.sheets-compose-dialogs:core:$maxkeppeler_sheets_ver"
    const val camera_camera2 = "androidx.camera:camera-camera2:$camera_ver"
    const val camera_lifecycle = "androidx.camera:camera-lifecycle:$camera_ver"
    const val camera_view = "androidx.camera:camera-view:$camera_ver"
    const val permission_accompanist =
        "com.google.accompanist:accompanist-permissions:$permission_accompanist_ver"

    const val appwrite = "io.appwrite:sdk-for-android:$appwrite_ver"
    const val splash_screen = "androidx.core:core-splashscreen:$splash_screen_ver"
    const val vkid = "com.vk.id:vkid:$vkid_ver"
    const val vkid_onetap ="com.vk.id:onetap-compose:$vkid_ver"

    const val parse_sdk_android = "com.github.parse-community.Parse-SDK-Android:parse:$parse_sdk_android_ver"
    const val parse_sdk_android_coroutine = "com.github.parse-community.Parse-SDK-Android:coroutines:$parse_sdk_android_ver"
    const val work_manager = "androidx.work:work-runtime-ktx:$work_manager_version"
    //     Debug
    const val ui_tooling =
        "androidx.compose.ui:ui-tooling:$compose_ui_version"
    const val ui_test_manifest =
        "androidx.compose.ui:ui-test-manifest:$compose_ui_version"
}

object TestLibs {
    const val arch_core =
        "androidx.arch.core:core-testing:$core_testing_version"
    const val compose_ui_test_junit =
        "androidx.compose.ui:ui-test-junit4:$compose_ui_version"
    const val ext_junit =
        "androidx.test.ext:junit:$test_ext_version"
    const val kotlin_coroutines_test =
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version"
    const val room =
        "androidx.room:room-testing:$room_version"
    const val test_runner =
        "androidx.test:runner:$test_runner_version"
    const val kotlin_test =
        "org.jetbrains.kotlin:kotlin-test"
    const val mockito_core =
        "org.mockito:mockito-core:$mockito_core_ver"
    const val mockito_inline =
        "org.mockito:mockito-inline:$mockito_inline_ver"
    const val mockito_kotlin=
        "com.nhaarman:mockito-kotlin:$mockito_kotlin_ver"
    const val exclude_jetbrains_kotlin =
        "org.jetbrains.kotlin"
    const val exclude_mockito =
        "org.mockito"
}
