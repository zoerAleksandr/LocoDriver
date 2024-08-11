pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            setUrl("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-andorid/")
            setUrl("https://artifactory-external.vkpartner.ru/artifactory/maven")
        }

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
}
include(":features:purchses")
