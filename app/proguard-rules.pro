# =============================================================================
# LocoDriver ProGuard rules
# =============================================================================

# ----- Стектрейсы для Sentry -----
-keepattributes SourceFile,LineNumberTable,Signature,Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-renamesourcefileattribute SourceFile

# =============================================================================
# kotlinx.serialization
# Документация: https://github.com/Kotlin/kotlinx.serialization#android
# =============================================================================
# Сохраняем kotlinx.serialization runtime классы
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keep,includedescriptorclasses class kotlinx.serialization.internal.** { *; }
-keepnames class kotlinx.serialization.** { *; }

# Сохраняем Companion для @Serializable классов — kotlinx использует через reflection
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Сохраняем сериализаторы
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Объекты без companion (если есть)
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Не убирать классы которые помечены @Serializable
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class **$$serializer { *; }

# =============================================================================
# Domain entities (используются в kotlinx.serialization и Ktor)
# =============================================================================
-keep class com.z_company.domain.entities.** { *; }
-keepclassmembers class com.z_company.domain.entities.** { *; }

# DTO для remote API
-keep class com.z_company.data_remote.** { *; }

# =============================================================================
# Koin
# =============================================================================
-keep class org.koin.** { *; }
-keepclassmembers class * extends org.koin.core.module.Module { *; }
-keep class * extends org.koin.core.context.GlobalContext { *; }

# =============================================================================
# Ktor
# =============================================================================
-keepclassmembers class io.ktor.** { *; }
-keep class io.ktor.** { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }

# =============================================================================
# kotlin coroutines
# =============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.flow.** { volatile <fields>; }
-keepclassmembernames class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.debug.AgentPremain

# =============================================================================
# kotlinx-datetime
# =============================================================================
-keep class kotlinx.datetime.** { *; }

# =============================================================================
# Sentry
# =============================================================================
-keep class io.sentry.** { *; }
-keepnames class io.sentry.** { *; }
-dontwarn io.sentry.**

# =============================================================================
# SQLDelight
# =============================================================================
-keep class app.cash.sqldelight.** { *; }
-keep class com.z_company.data_local.** { *; }
-keepclassmembers class com.z_company.data_local.** { *; }

# =============================================================================
# Glance AppWidget
# =============================================================================
-keep class androidx.glance.appwidget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.z_company.loco_driver.widget.** { *; }

# =============================================================================
# VK ID SDK
# =============================================================================
-keep class com.vk.id.** { *; }
-keep class com.vk.dto.** { *; }
-dontwarn com.vk.id.**

# =============================================================================
# RuStore SDK
# =============================================================================
-keep class ru.rustore.sdk.** { *; }
-dontwarn ru.rustore.sdk.**

# =============================================================================
# Robokassa SDK
# =============================================================================
-keep class com.robokassa.** { *; }
-dontwarn com.robokassa.**

# =============================================================================
# Compose (большинство правил включены в proguard-android-optimize.txt,
# но добавляем критичные на всякий случай)
# =============================================================================
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.tooling.preview.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }

# =============================================================================
# Profile Installer (Baseline Profile)
# =============================================================================
-keep class androidx.profileinstaller.** { *; }
-keepnames class androidx.profileinstaller.** { *; }

# =============================================================================
# Retrofit / Gson (legacy — пока ещё в зависимостях)
# =============================================================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# =============================================================================
# Tink (используется для шифрования токенов через SecureTokenStorage)
# =============================================================================
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# =============================================================================
# Camera X (используется для фото маршрутов)
# =============================================================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# =============================================================================
# WorkManager
# =============================================================================
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class androidx.work.** { *; }

# =============================================================================
# MyTracker SDK
# =============================================================================
-keep class com.my.tracker.** { *; }
-dontwarn com.my.tracker.**

# =============================================================================
# Suppress warnings from optional dependencies
# =============================================================================
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# JVM-only classes (Ktor debug detector + XML parsers, не используются на Android)
-dontwarn java.lang.management.**
-dontwarn javax.xml.stream.**
-dontwarn com.gitlab.mvysny.konsumexml.**
