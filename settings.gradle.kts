pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://nexus-external.vkteam.ru/repository/vkid-sdk-android/")
        maven(url = "https://nexus-external.vkteam.ru/repository/maven/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven(url = "https://nexus-external.vkteam.ru/repository/vkid-sdk-android/")
        maven(url = "https://nexus-external.vkteam.ru/repository/maven/")
        maven(url = "https://nexus-external.rustore.ru/repository/maven-rustore-exposed/")
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
