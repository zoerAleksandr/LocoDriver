plugins {
    id(Plugins.java_lib)
    id(Plugins.kotlin_jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(Libs.project_core))
//    implementation(project(Libs.project_core_android))
    implementation(Libs.kotlinx_coroutines_core)
}