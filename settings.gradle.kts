import org.gradle.kotlin.dsl.flatDir

include(":robokassa-sdk")


pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")  // Добавьте для плагинов VKID
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/maven/")  // Добавьте для зависимостей
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")  // Добавьте для captcha (если используется)
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")  // Добавьте для SDK
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/maven/")  // Добавьте
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")  // Добавьте
//        flatDir { dirs("libs") }
        maven {
            setUrl("https://jitpack.io")
        }
    }

    rootProject.name = "LocoDriver"
    include(":app")
    include(":core_android")
    include(":data_local")
    include(":core")
    include(":domain")
    include(":features")
    include(":features:route")
    include(":features:login")
    include(":features:settings")
    include(":data_remote")
    include(":data_remote")
    include(":data_remote")
    include(":robokassa_sdk")
    include(":iosApp")
}
