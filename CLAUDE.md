# CLAUDE.md — LocoDriver KMP Migration Plan

## Цель
Мигрировать Android-приложение LocoDriver на Kotlin Multiplatform (KMP)
для поддержки iOS-платформы.

## Ветка разработки
`claude/explain-codebase-mlusp4bmp9lredgk-7hTS7`

---

## Статус шагов миграции

### ✅ Шаг 1 — Retrofit → Ktor + kotlinx.serialization
- `RemoteRestClient.kt` переписан на Ktor HttpClient
- `RemoteRestApi.kt` — очищен от Retrofit-аннотаций
- Создан `KtorRemoteRestApi.kt`
- Gson TypeAdapters → kotlinx.serialization KSerializers
- Все request/response модели помечены `@Serializable`

### ✅ Шаг 2 — object-синглтоны → DI-классы
- `AuthManager`, `SettingManager`, `RoutesManager`: `object` → `class`
- Обновлён `RepositoryModule.kt`

### ✅ Шаг 3 — Абстракция Context из бизнес-логики
- Создан `SecureTokenStorage` (DataStore + Tink, Android-specific)
- Убраны прямые вызовы `Context` из domain/data_remote

### ✅ Шаг 5 — AndroidViewModel → ViewModel
- Все ViewModel переведены на `org.jetbrains.lifecycle.ViewModel`

### ✅ Шаг 4 — Удаление KoinComponent из domain
- `UtilsForEntities`, `CalculateNightTime`: `object : KoinComponent` → `object`
- Создан `TimeZoneUtils.kt` с `fun getTimeZone(timeZoneInMillis: Long): String`
- `SalarySettingUseCase`, `SalaryCalculationUseCase`: конструкторная инжекция
- `domain/build.gradle.kts`: удалены `koin_core` и `gson`

### ✅ Шаг 6 — Конвертация `core` модуля в KMP
- Plugin: `java-library` + `kotlin-jvm` → `kotlin("multiplatform")` + `android.library`
- Targets: `jvm()` + `androidTarget()`
- Исходники: `src/main/java/` → `src/commonMain/kotlin/`
- `ResultState.flowRequest`: `Dispatchers.IO` → `Dispatchers.Default`
- Namespace: `com.z_company.core.common`

### ✅ Шаг 7 — domain: java.util.Calendar → kotlinx-datetime
- Все `java.util.Calendar`, `java.util.TimeZone`, `java.util.Date` заменены на `kotlinx-datetime`
- `java.util.UUID` → `kotlin.uuid.Uuid` (Kotlin 2.0+)
- `java.math.BigDecimal` → `Double` + `DoubleAsStringSerializer`

### ✅ Шаг 8 — domain: конвертация в KMP
- Plugin: `kotlin("multiplatform")` + `android.library`
- Targets: `jvm()`, `androidTarget()`, `iosArm64()`, `iosX64()`, `iosSimulatorArm64()`
- Исходники: `src/commonMain/kotlin/`
- Зависимости: `kotlinx-datetime`, `kotlinx-coroutines`, `kotlinx-serialization`

### ✅ Шаг 14 — iosApp: Compose Multiplatform iOS entry point
- `iosApp/build.gradle.kts`: KMP + XCFramework «ComposeApp»
- `App.kt`: корневой @Composable (заглушка, шаг 15 подключит навигацию)
- `MainViewController.kt`: `ComposeUIViewController` мост Kotlin → UIKit
- `iOSApp.swift` + `ContentView.swift`: Swift entry point

---

### ✅ Шаг 9 — Room → SQLDelight в data_local
- Plugin `sqldelight`, 4 базы: RouteDatabase, SettingsDatabase, SalarySettingDatabase, SearchResponseDatabase
- 9 `.sq` файлов: BasicData, Locomotive, Passenger, Photo, Train, SearchResponse, MonthOfYear, UserSettings, SalarySetting
- expect/actual `DatabaseDriverFactory`: Android → `AndroidSqliteDriver`, iOS → `NativeSqliteDriver`

### ✅ Шаг 10 — data_remote: iOS Ktor engine
- `expect fun createHttpEngine()` → Android: `AndroidClientEngine`, iOS: `Darwin` (NSURLSession)

### ✅ Шаг 11 — SecureTokenStorage: expect/actual
- Android: DataStore + Tink (AES256_GCM, AndroidKeyStore)
- iOS: Keychain Services (`kSecClassGenericPassword`)

### ✅ Шаг 12/14 — iOS entry point (Compose Multiplatform)
- `iosApp` KMP-модуль, XCFramework «ComposeApp», Compose MP 1.8.0

---

## 🔴 Текущий шаг: Шаг 15 — подключить навигацию features/ к iosApp

---

## Технический стек

| Категория | Библиотека | Версия |
|-----------|-----------|--------|
| Kotlin | 2.2.0 | — |
| KMP Runtime | kotlinx-coroutines | 1.7.1 |
| KMP DateTime | kotlinx-datetime | 0.6.2 |
| KMP HTTP | Ktor | 3.x |
| KMP DB | SQLDelight | (не добавлен) |
| DI | Koin | 3.5.6 (поддерживает KMP) |
| Serialization | kotlinx.serialization | — |

## Зависимости между модулями
```
app (Android)
├── core_android (Android)
│   └── core (KMP ✅)
├── domain (JVM → нужно KMP)
│   └── core (KMP ✅)
├── data_local (Android → нужно SQLDelight)
│   └── domain
├── data_remote (Android → нужно KMP)
│   └── domain
└── features (Android → UI)
    └── domain, data_local, data_remote
```
