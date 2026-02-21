// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties

plugins {
    id(Plugins.android_app) version Versions.android_plugin_id apply false
    id(Plugins.kotlin_android) version Versions.kotlin_version apply false
    id(Plugins.android_lib) version Versions.android_plugin_id apply false
    id(Plugins.kotlin_jvm) version Versions.kotlin_version apply false
    id(Plugins.kotlin_multiplatform) version Versions.kotlin_version apply false
    id(Plugins.google_relay) version Versions.google_relay_ver apply false
    id(Plugins.compose_compiler) version Versions.kotlin_version apply false
    id(Plugins.ksp) version Versions.ksp_ver apply false
    id(Plugins.vkIdManifest) version Versions.vkIdManifestPluginVer apply true
    id("org.jetbrains.kotlin.plugin.serialization") version Versions.kotlin_version
}


// Добавление значений в Manifest Placeholders.
vkidManifestPlaceholders {
    val localProperties = Properties()
    localProperties.load(project.rootProject.file("secret.properties").inputStream())
    val clientId: String = localProperties.getProperty("VKIDClientID") ?: ""
    val clientSecret: String = localProperties.getProperty("VKIDClientSecret") ?: ""

    init(
        clientId = clientId,
        clientSecret = clientSecret,
    )
    vkidRedirectHost = "vk.ru" // Обычно vk.ru.
    vkidRedirectScheme = "vk$clientId"
    vkidClientId = clientId
    vkidClientSecret = clientSecret
}