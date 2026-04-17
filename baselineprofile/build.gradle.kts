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
    }

    targetProjectPath = ":app"

    // Используем тот же signing что и debug — чтобы профиль ставился без конфликта подписи
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

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
    // Использовать подключённое устройство (Samsung) для генерации
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.4")
}
