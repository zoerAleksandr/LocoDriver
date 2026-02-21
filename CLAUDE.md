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

---

## 🔴 Текущий шаг: Шаг 7 — domain: java.util.Calendar → kotlinx-datetime

### Контекст
`domain` — JVM-модуль с `java-library` + `kotlin-jvm`. Чтобы конвертировать
его в KMP (`commonMain`), нужно убрать все Java API, недоступные на iOS.

### Java API в domain (что заменить)

| Файл | Java API | KMP-замена |
|------|----------|-----------|
| `CalculateNightTime.kt` | `java.util.Calendar`, `java.util.TimeZone` | `kotlinx-datetime` |
| `UtilsForEntities.kt` | `java.util.Calendar`, `java.util.TimeZone` | `kotlinx-datetime` |
| `UtilForMonthOfYear.kt` | `java.util.Calendar` | `kotlinx-datetime` |
| `MonthOfYear.kt` | `java.util.Calendar`, `java.util.Date` | `kotlinx-datetime` |
| `BasicData.kt` | `java.util.Calendar` | `kotlinx-datetime` |
| `RouteUseCase.kt` | `java.util.Calendar` | `kotlinx-datetime` |
| `UserSettings.kt` | `java.util.Calendar` | `kotlinx-datetime` |
| `Serializers.kt` | `java.util.Date` | убрать/заменить |
| везде | `java.util.UUID` | `kotlin.uuid.Uuid` (Kotlin 2.0+) |
| везде | `java.math.BigDecimal` | `Double` (уже в моделях) |

### Стратегия замены Calendar

```
java.util.Calendar (эпоха в миллисекундах) →
    kotlinx.datetime.Instant (fromEpochMilliseconds / toEpochMilliseconds)
    kotlinx.datetime.LocalDateTime
    kotlinx.datetime.TimeZone
    kotlinx.datetime.toLocalDateTime()
```

**Ключевые паттерны:**
```kotlin
// Было
Calendar.getInstance(TimeZone.getTimeZone("GMT+3")).also {
    it.timeInMillis = millis
    it.set(Calendar.HOUR_OF_DAY, hour)
}
// Стало
val tz = TimeZone.of("GMT+3")
val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
    .let { it.date.atTime(hour, minute) }
    .toInstant(tz)
```

### После замены Calendar: Шаг 8

- Конвертировать `domain` в KMP: добавить `kotlin("multiplatform")`,
  переместить в `commonMain/kotlin/`
- Добавить iOS-таргеты (`iosArm64`, `iosX64`, `iosSimulatorArm64`) в `core` и `domain`

---

## Шаг 9 — Room → SQLDelight в data_local (крупный)
- 37 файлов используют `androidx.room`
- SQLDelight поддерживает Android (SQLite) и iOS (SQLiteDriver)
- Сохранить все 12 версий схемы / авто-миграции

## Шаг 10 — data_remote: добавить iOS Ktor engine
- Заменить `ktor-client-android` → `ktor-client-darwin` (iOS) / общий `ktor-client-core`
- `expect/actual` для создания HttpClient на каждой платформе

## Шаг 11 — SecureTokenStorage: expect/actual
- Android: DataStore + Tink (текущая реализация)
- iOS: Keychain через `multiplatform-settings` или нативный API

## Шаг 12 — Создать Xcode проект + iOS UI
- Compose Multiplatform (рекомендуется для переиспользования UI)
- ИЛИ SwiftUI + XCFramework (если нужен нативный iOS UI)

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
