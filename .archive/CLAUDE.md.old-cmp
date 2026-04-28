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

### ✅ Шаг 9 — Room → SQLDelight в data_local
- Plugin `sqldelight`, 4 базы: RouteDatabase, SettingsDatabase, SalarySettingDatabase, SearchResponseDatabase
- 9 `.sq` файлов: BasicData, Locomotive, Passenger, Photo, Train, SearchResponse, MonthOfYear, UserSettings, SalarySetting
- expect/actual `DatabaseDriverFactory`: Android → `AndroidSqliteDriver`, iOS → `NativeSqliteDriver`

### ✅ Шаг 10 — data_remote: iOS Ktor engine
- `expect fun createHttpEngine()` → Android: `AndroidClientEngine`, iOS: `Darwin` (NSURLSession)

### ✅ Шаг 11 — SecureTokenStorage: expect/actual
- Android: DataStore + Tink (AES256_GCM, AndroidKeyStore)
- iOS: Keychain Services (`kSecClassGenericPassword`)

### ✅ Шаг 12/14 — iosApp: iOS entry point (Compose Multiplatform)
- `iosApp` KMP-модуль, XCFramework «ComposeApp», Compose MP 1.8.0
- `MainViewController.kt`: `ComposeUIViewController` мост Kotlin → UIKit
- `iOSApp.swift` + `ContentView.swift`: Swift entry point

### ✅ Шаг 15 — Навигация iOS (KMP Navigation Compose)
- `org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10`
- `Routes.kt` — маршруты зеркалят `features/route/navigation/Routes.kt`
- `IosRouterImpl.kt` — реализует `domain.Router` через `NavHostController`
- `AppNavHost.kt` — `NavHost` с composable-маршрутами для всех экранов

### ✅ Шаг 16 — iOS ViewModels (Home + Settings)
- `HomeIosViewModel` + `SettingsIosViewModel`: `androidx.lifecycle.ViewModel` (KMP)
- `koin-compose` (koinInject) для инжекции в Composable
- `IosUseCaseModule.kt`: DI-цепочка репозиторий → UseCase → ViewModel
- `HomeScreen.kt`, `SettingsScreen.kt`: подключены реальные ViewModels

### ✅ Шаг 17 — iOS ViewModels (Form + SalaryCalculation) + Xcode Project
- `FormIosViewModel`: `RouteUseCase` (routeDetails + saveRoute), стейт-машина
- `SalaryCalculationIosViewModel`: `RouteUseCase` + `SettingsUseCase`, месячная статистика
- `FormScreen.kt`: TextField для номера/заметок, отображение времени, кнопка сохранить
- `SalaryCalculationScreen.kt`: Card с кол-вом маршрутов и общим временем работы
- `AppNavHost.kt`: передача `routeId` из `NavBackStackEntry` в `FormScreen`
- `iosApp.xcodeproj`: Xcode-проект (project.pbxproj, Info.plist, Assets.xcassets)

---

## 🟢 Проект готов к запуску

### Запуск Android
```bash
./gradlew :app:assembleDebug
# или через Android Studio — Run 'app'
```

### Запуск iOS (требует macOS + Xcode 15+)
1. Открыть `iosApp/iosApp.xcodeproj` в Xcode
2. Выбрать симулятор или устройство
3. Нажать Run — Xcode автоматически вызовет:
   ```bash
   ./gradlew :iosApp:embedAndSignAppleFrameworkForXcode
   ```
4. ComposeApp.framework встраивается и приложение запускается

---

## Технический стек

| Категория | Библиотека | Версия |
|-----------|-----------|--------|
| Kotlin | 2.2.0 | — |
| KMP Runtime | kotlinx-coroutines | 1.7.1 |
| KMP DateTime | kotlinx-datetime | 0.6.2 |
| KMP HTTP | Ktor | 3.0.3 |
| KMP DB | SQLDelight | 2.0.2 |
| DI | Koin | 3.5.6 (поддерживает KMP) |
| Serialization | kotlinx.serialization | 1.10.0 |
| Compose MP | org.jetbrains.compose | 1.8.0 |
| Navigation KMP | org.jetbrains.androidx.navigation | 2.8.0-alpha10 |

## Зависимости между модулями
```
app (Android)
├── core_android (Android)
│   └── core (KMP ✅)
├── domain (KMP ✅)
│   └── core (KMP ✅)
├── data_local (KMP ✅ — SQLDelight)
│   └── domain
├── data_remote (KMP ✅ — Ktor)
│   └── domain
├── features (Android — UI)
│   └── domain, data_local, data_remote
└── iosApp (KMP iOS ✅ — Compose Multiplatform)
    ├── data_remote (api — экспорт в Swift: initKoin)
    ├── data_local (impl — SQLDelight, Koin modules)
    └── domain (impl — UseCases, Router)
```
