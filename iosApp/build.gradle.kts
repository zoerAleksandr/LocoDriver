import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.compose_multiplatform)
    id(Plugins.compose_compiler)
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
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        commonMain.dependencies {
            // Compose Multiplatform UI
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            // KMP Navigation (JetBrains fork androidx.navigation)
            implementation(Libs.navigation_compose_kmp)

            // KMP бизнес-логика
            api(project(Libs.project_data_remote))   // SecureTokenStorage, API, initKoin()
            implementation(project(Libs.project_data_local))
            implementation(project(Libs.project_domain))
            implementation(project(Libs.project_core))

            // DI + утилиты
            implementation(Libs.koin_core)
            implementation(Libs.koin_compose)   // koinInject() для Compose MP
            implementation(Libs.kotlinx_coroutines_core)
            implementation(Libs.kotlinx_date_time)
        }

        iosMain.dependencies {
            // iOS-специфичные Compose-зависимости (если потребуются)
        }
    }
}
