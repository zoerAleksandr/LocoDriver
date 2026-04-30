import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.sentry_kmp)
}

kotlin {
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    // Генерируем статический XCFramework «ComposeApp», который Xcode подключает как бинарник.
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Экспортируем DI-хелпер, чтобы Swift видел fun initKoin()
            export(project(Libs.project_data_remote))
            // Экспортируем core, чтобы Swift видел fun initSentry()
            export(project(Libs.project_core))
        }
    }

    sourceSets {
        commonMain.dependencies {
            // KMP бизнес-логика
            api(project(Libs.project_data_remote))   // SecureTokenStorage, API, initKoin()
            implementation(project(Libs.project_data_local))
            implementation(project(Libs.project_domain))
            api(project(Libs.project_core))   // export для Swift: initSentry()

            // DI + утилиты
            implementation(Libs.koin_core)
            implementation(Libs.kotlinx_coroutines_core)
            implementation(Libs.kotlinx_date_time)
            // androidx.lifecycle.ViewModel + viewModelScope для KMP
            // (org.jetbrains.androidx.lifecycle — JetBrains-форк только lifecycle-viewmodel,
            // используют все *IosViewModel.kt; раньше приходил транзитивно через
            // navigation-compose-kmp, теперь объявляем напрямую).
            implementation(Libs.lifecycle_viewmodel_kmp)
        }

        iosMain.dependencies {
            // iOS-специфичные Compose-зависимости (если потребуются)
        }
    }
}
