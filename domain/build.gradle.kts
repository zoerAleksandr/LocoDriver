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
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation ("org.mockito:mockito-core:3.10.0")
    testImplementation ("org.mockito:mockito-inline:2.8.9")
    testImplementation("com.nhaarman:mockito-kotlin:1.5.0") {
        exclude("org.jetbrains.kotlin")
        exclude("org.mockito")
    }
}