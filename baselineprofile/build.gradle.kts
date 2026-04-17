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

    @Suppress("UnstableApiUsage")
    testOptions.managedDevices.allDevices {
        // Эмулятор Pixel 6 API 34 — Gradle создаст автоматически.
        // Baseline Profile collection требует API 33+, а Samsung A12 = API 31.
        create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    // Используем managed device pixel6Api34 (Samsung A12 = API 31, не подходит)
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.4")
}
