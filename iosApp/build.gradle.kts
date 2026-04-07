import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id(Plugins.kotlin_multiplatform)
    id(Plugins.compose_multiplatform)
    id(Plugins.compose_compiler)
    id(Plugins.sentry_kmp)
}

// Конфигурация Compose-компилятора:
// Помечаем классы compose-runtime/animation как стабильные через конфиг-файл,
// чтобы компилятор генерировал константу STABLE вместо вызова $stableprop_getter$artificial
// (несовместимость символов между Kotlin 2.2.20 и compose klib)
composeCompiler {
    stabilityConfigurationFiles.add(
        project.layout.projectDirectory.file("compose_stability.conf")
    )
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
            api(project(Libs.project_core))   // export для Swift: initSentry()

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
