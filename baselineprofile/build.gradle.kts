import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.test")
    id(Plugins.kotlin_android)
    id("androidx.baselineprofile")
}

android {
    namespace = "com.z_company.loco_driver.baselineprofile"
    compileSdk = Apps.compile_sdk_version

    defaultConfig {
        minSdk = 28          // Baseline Profile API доступен с Android 9+
        targetSdk = Apps.target_sdk_version
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Подавить ошибки про эмулятор и debuggable — мы запускаемся в managed device
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,LOW-BATTERY,UNLOCKED,DEBUGGABLE"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = Apps.java_compatibility_version
        targetCompatibility = Apps.java_compatibility_version
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(Apps.jvm_target_version)
        }
    }

}

baselineProfile {
    // Используем подключённый эмулятор с Android 13+ и реальными данными
    // (Samsung A12 = API 31, для Baseline Profile нужен API 33+).
    // Подготовка эмулятора:
    //   1. Запустить Pixel 5/6 API 33+ через AVD Manager
    //   2. Установить release APK: adb install app/build/outputs/apk/release/app-release.apk
    //   3. Войти в приложении (VK ID) — данные подгрузятся с сервера
    //   4. ./gradlew :app:generateReleaseBaselineProfile
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    // 1.4.1 — минимум, где androidx.benchmark умеет парсить новый формат
    // вывода `pm dump-profiles` на свежих Android (у нас эмулятор API 36).
    // На 1.3.4 генерация профиля падала с IllegalStateException.
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
}
