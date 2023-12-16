// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id(Plugins.android_app) version Versions.android_plugin_id apply false
    id(Plugins.kotlin_android) version Versions.kotlin_version apply false
    id(Plugins.android_lib) version Versions.android_plugin_id apply false
    id(Plugins.kotlin_jvm) version Versions.kotlin_version apply false
}