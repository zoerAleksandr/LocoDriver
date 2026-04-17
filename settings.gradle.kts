pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/maven/")
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/maven/")
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")
//        flatDir { dirs("libs") }
        maven {
            setUrl("https://jitpack.io")
        }
    }

    rootProject.name = "LocoDriver"
    include(":app")
    include(":baselineprofile")
    include(":core_android")
    include(":data_local")
    include(":core")
    include(":domain")
    include(":features")
    include(":features:route")
    include(":features:login")
    include(":features:settings")
    include(":data_remote")
    include(":robokassa_sdk")
    include(":iosApp")
}
