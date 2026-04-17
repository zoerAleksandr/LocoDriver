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

        // Запускать только на Samsung A12 (R58R625VJBP), не на эмуляторах:
        // в эмуляторах нет реальных данных и SplashScreen долго стартует
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
    // Использовать подключённое устройство (Samsung) для генерации
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.4")
}
