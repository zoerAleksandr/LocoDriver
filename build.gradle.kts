// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec

plugins {
    id(Plugins.android_app) version Versions.android_plugin_id apply false
    id(Plugins.kotlin_android) version Versions.kotlin_version apply false
    id(Plugins.android_lib) version Versions.android_plugin_id apply false
    id(Plugins.kotlin_jvm) version Versions.kotlin_version apply false
    id(Plugins.kotlin_multiplatform) version Versions.kotlin_version apply false
    id(Plugins.google_relay) version Versions.google_relay_ver apply false
    id(Plugins.compose_compiler) version Versions.kotlin_version apply false
    id(Plugins.compose_multiplatform) version Versions.compose_mp_ver apply false
    id(Plugins.ksp) version Versions.ksp_ver apply false
    id(Plugins.sqldelight) version Versions.sqldelight_ver apply false
    id(Plugins.sentry_kmp) version Versions.sentry_kmp_plugin_ver apply false
    id(Plugins.vkIdManifest) version Versions.vkIdManifestPluginVer apply true
    id("org.jetbrains.kotlin.plugin.serialization") version Versions.kotlin_version
    // Baseline Profile генерация (для AOT-компиляции горячих путей)
    id("com.android.test") version Versions.android_plugin_id apply false
    id("androidx.baselineprofile") version "1.3.4" apply false
}

@Suppress("DEPRECATION_ERROR")
plugins.withType<NodeJsRootPlugin>().configureEach {
    extensions.configure<NodeJsRootExtension> {
        download = false
        downloadBaseUrl = null
        command = "/usr/local/bin/node"
    }
}

plugins.withType<YarnPlugin>().configureEach {
    extensions.configure<YarnRootEnvSpec> {
        download.set(false)
        downloadBaseUrl.set("")
    }
    extensions.configure<NodeJsRootExtension> {
        packageManagerExtension.set(extensions.getByType<NpmExtension>())
    }
}

tasks.register<Sync>("preparePwa") {
    dependsOn(":domain:jsBrowserProductionLibraryDistribution")
    into(layout.projectDirectory.dir("pwa/vendor"))
    from(layout.projectDirectory.dir("domain/build/dist/js/productionLibrary")) {
        include("*.js")
    }
    from(layout.buildDirectory.file("js/node_modules/@js-joda/core/dist/js-joda.min.js"))
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
