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
