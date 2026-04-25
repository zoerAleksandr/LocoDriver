# CODEBASE.md — LocoDriver

> Описание кодовой базы. Дополняет `CLAUDE.md` (правила) фактами о
> структуре. Если хочешь быстрый обзор — иди в `CLAUDE.md`. Если ищешь
> «где что лежит» — здесь.

---

## Назначение

**LocoDriver** — приложение для машинистов локомотивов
(грузовое и пассажирское движение). На Android в продакшене,
iOS в разработке (паритет с Android в течение ближайших недель).

Решает:
- Учёт рейсов с детальной информацией (BasicData, локомотивы со
  секциями, поезда со станциями, пассажиры, заметки).
- Расчёт ночных часов с учётом часового пояса и переходов через сутки.
- Расчёт зарплаты с десятком надбавок (ночные, районный, северные,
  тяжеловесные, длинносоставные, удлинённое плечо, в одно лицо и т.д.).
- Производственный календарь (дни) + личные отвлечения (отпуск,
  больничный, курсы).
- Синхронизация с сервером (offline-first).
- Шаринг маршрутных листов через короткую ссылку.

Целевая аудитория: машинисты в России. Интерфейс — русский.
Аутентификация: email/пароль + VK ID. Платежи: RuStore + Robokassa.

---

## Структура репозитория

Один репозиторий, с Android-приложением, KMP-модулями и iOS-проектом
вместе. Это нестандартная структура (исторически Android-проект,
постепенно мигрированный в KMP с сохранением имён модулей).

```
LocoDriver/
├── app/                   Android entry point + DI-граф (Koin)
│   └── src/main           ← StartApp.kt, MainActivity.kt, LocoDriverApp.kt
│
├── core/                  ✅ KMP (jvm + androidTarget)
│   └── src/commonMain     ← ResultState, утилиты
│
├── core_android/          Android-only
│   └── src/main           ← UI-расширения, Compose-helper'ы
│
├── domain/                ✅ KMP (jvm + androidTarget + iOS)
│   └── src/commonMain     ← модели, репозитории (интерфейсы), use cases
│
├── data_local/            ✅ KMP — SQLDelight
│   ├── src/commonMain     ← .sq файлы, репозитории
│   ├── src/androidMain    ← AndroidSqliteDriver
│   ├── src/iosMain        ← NativeSqliteDriver
│   └── schemas/           ← снапшоты схем БД (3 базы)
│
├── data_remote/           ✅ KMP — Ktor
│   ├── src/commonMain     ← API-клиент, менеджеры
│   ├── src/androidMain    ← AndroidClientEngine
│   └── src/iosMain        ← Darwin (NSURLSession)
│
├── features/              Android-only Compose UI
│   ├── route/             ← основной модуль (списки, формы)
│   ├── login/
│   ├── settings/
│   ├── tracking/
│   └── shared/            ← пустая
│
├── iosApp/
│   ├── src/commonMain     🚮 МЁРТВЫЙ ГРУЗ от попытки CMP
│   ├── src/iosMain        🚮 МЁРТВЫЙ ГРУЗ от попытки CMP
│   ├── iosApp/            ✅ Реальный SwiftUI-проект
│   │   ├── iOSApp.swift
│   │   ├── ContentView.swift
│   │   ├── Navigation/    ← AppCoordinator (TabView)
│   │   ├── Screens/       ← 10+ SwiftUI экранов
│   │   ├── ViewModels/    ← 8 ViewModelWrapper'ов
│   │   └── Shared/        ← TimeFormatter, утилиты
│   └── iosApp.xcodeproj
│
├── robokassa_sdk/         Android-only платежи
├── benchmark/             производительность
├── baselineprofile/       baseline profile
├── website/               вспомогательное
├── buildSrc/              ⚠️ старый стиль управления зависимостями
│   └── src/main/kotlin/Dependencies.kt   (вместо libs.versions.toml)
│
├── CLAUDE.md              ← правила для Claude Code (читать первым)
├── CODEBASE.md            ← этот файл
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/                wrapper, без version catalog
```

---

## Архитектурный паттерн

**Clean Architecture + MVVM**:

```
[Android]                              [iOS]
features/* (Compose)                   iosApp/iosApp/Screens/* (SwiftUI)
    ↓ State/Events                         ↓ Bindings/Actions
ViewModelModule (Android)              ViewModelWrapper (Swift, @MainActor)
    ↓ inject                               ↓ watchX(callback)
Android ViewModel                      iOS Kotlin-VM (FormIosViewModel и т.п.)
    ↓ flow / suspend                       ↓ flow / suspend
                  ↓
         UseCase (KMP, domain/)
                  ↓
         Repository interface (KMP, domain/)
                  ↓
   ┌──────────────────────────────────┐
   ↓                                  ↓
data_local (SQLDelight)        data_remote (Ktor)
```

---

## Доменный слой (`domain/src/commonMain/`)

### Модели (entities)

В пакете `com.z_company.domain.entities`:
- `route/Route.kt` — корневая сущность рейса
- `route/BasicData.kt` — метаданные (время работы, флаги, заметки)
- `route/Locomotive.kt` — локомотив + список секций
- `route/SectionElectric.kt`, `SectionDiesel.kt` — секции
- `route/Train.kt` — поезд + станции + фаза обслуживания
- `route/Passenger.kt` — пассажирские поездки
- `route/LocoType.kt` — enum ELECTRIC / DIESEL
- `setting/UserSettings.kt` — настройки пользователя
- `setting/SalarySetting.kt` — настройки зарплаты
- `setting/ServicePhase.kt` — плечо обслуживания
- `setting/NightTime.kt` — границы ночного времени
- `MonthOfYear.kt` — рабочий месяц
- `Day.kt`, `ReleaseType.kt` — дни и типы отвлечений

### Репозитории (интерфейсы)

`com.z_company.domain.repositories`:
- `RouteRepository`, `SettingsRepository`, `SalarySettingRepository`
- `CalendarRepositories`, `CalendarStorage` — производственный календарь
- `ProductionCalendarRepository`, `ReleaseDayRepository`,
  `RegionalHolidaysRepository`, `HardcodedRegionalHolidaysRepository`
- `HistoryResponseRepository` — история запросов
- `SharedPreferencesRepositories` — настройки приложения

### Use Cases

`com.z_company.domain.use_cases`:
- `RouteUseCase` — CRUD рейсов
- `SalaryCalculationUseCase` — расчёт зарплаты
- `SalarySettingUseCase` — настройки расчёта
- `SettingsUseCase` — пользовательские настройки
- `CalendarUseCase`, `ReleaseDayUseCase`, `LoadCalendarFromStorage`
- `TrainUseCase`, `LocomotiveUseCase`, `PassengerUseCase`

### Утилиты

`com.z_company.domain.util`:
- `CalculateNightTime.kt` — ⚠️ **сложный**, расчёт ночных часов с
  переходами через сутки и часовыми поясами. Покрыт тестами на JVM,
  при правках обязательно прогонять тесты.
- `TimeZoneUtils.kt` — `getTimeZone(timeZoneInMillis: Long): String`
- `IdGenerator.kt` — UUID v4 (через `kotlin.uuid.Uuid`)
- `BigDecimalUtil.kt`, `DoubleUtil.kt` — арифметика
- `OperatorsLong.kt`, `LongUtil.kt`, `IntUtil.kt`, `StringUtil.kt`,
  `CollectionUtil.kt` — расширения
- `CalculationEnergy.kt` — расчёт энергии локомотивов
- `TimeCalculationContext.kt` — контекст расчётов времени
- `SharedRouteHolder.kt` — singleton, держит загруженный
  расшаренный рейс между deep link и экраном
- `Router.kt` (в `domain.navigation`) — интерфейс навигации, реализуется
  на каждой платформе

---

## Слой данных

### `data_local/` — SQLDelight

3 базы:
- `RouteDatabase` — рейсы и связанные сущности
- `SettingsDatabase` — настройки пользователя + календарь
- `SalarySettingDatabase` — настройки зарплаты

Снапшоты схем в `data_local/schemas/`:
- `com.z_company.data_local.route.data_base.RouteDB`
- `com.z_company.data_local.setting.data_base.SettingsDB`
- `com.z_company.data_local.setting.data_base.SalarySettingDB`

`.sq`-файлы: BasicData, Locomotive, Passenger, Photo, Train,
SearchResponse, MonthOfYear, UserSettings, SalarySetting.

`expect/actual` для драйверов:
- `androidMain` → `AndroidSqliteDriver`
- `iosMain` → `NativeSqliteDriver`

[?**Нужно проверить через Claude Code**: была ли реализована миграция
данных существующих Android-пользователей с Room на SQLDelight, или
данные обнуляются при обновлении приложения и пересинхронизируются с
сервера.]

### `data_remote/` — Ktor

`com.z_company.repository.remote_rest`:
- `KtorRemoteRestApi.kt` — реализация всех API-вызовов
- `RemoteRestApi.kt` — интерфейс
- `RemoteRestClient.kt` — конфигурация HttpClient
- `HttpClientEngineFactory.kt` — `expect fun createHttpEngine()`
  - `androidMain` → `AndroidClientEngine`
  - `iosMain` → `Darwin` (NSURLSession)
- `AuthManager.kt` — логин/регистрация/email/vkId
- `RoutesManager.kt` — синхронизация рейсов
- `SettingManager.kt` — синхронизация настроек
- `SyncManager.kt` — оркестрирует синхронизацию
- `ShareRouteManager.kt` — создание/получение share-ссылок
- `UserRemote.kt` — работа с пользователем

DTO в `request/` и `response/` подпапках.

**Безопасное хранилище токенов** — через `expect/actual`:
- Android: DataStore + Tink (AES256_GCM, AndroidKeyStore)
- iOS: Keychain Services (`kSecClassGenericPassword`)

---

## Android (`features/` + `app/`)

### Точка входа

- **`StartApp.kt`** — `Application`. Инициализирует:
  - Koin (модули собираются и стартуют через `startKoin`)
  - MyTracker (аналитика)
  - VK ID SDK (auth)
  - WorkManager (для фоновой синхронизации)
  - ru.ok.tracer (crash reporting)
- **`MainActivity.kt`** — единственная Activity. Compose, deep links
  для возврата из Robokassa, VKID, App Links для шаринга.
- **`LocoDriverApp.kt`** — корневой Compose, навигация, темизация.

### Экраны

В `features/route/src/main/`, `features/login/`, `features/settings/`,
`features/tracking/`. Каждый — свой набор Compose-экранов и
ViewModel'ов.

Основные ViewModel'и (`app/src/main/.../viewmodels/`):
- `HomeViewModel` — дашборд
- `FormViewModel` — создание/редактирование рейса
- `LocoFormViewModel`, `TrainFormViewModel`, `PassengerFormViewModel`
- `SearchViewModel`, `AllRouteViewModel`
- `SalaryCalculationViewModel`, `SettingSalaryViewModel`
- `SettingsViewModel`, `ProfileViewModel`
- `WorkScheduleViewModel`, `PurchasesViewModel`

### Стратегия синхронизации (Android)

Offline-first:
1. Изменения сначала пишутся в SQLDelight (локально).
2. WorkManager запускает `SyncWorker` каждые 36 часов (+ при появлении
   сети, по триггеру).
3. `SyncWorker` → `SyncManager` → `RoutesManager`/`SettingManager` →
   Ktor → сервер.

---

## iOS (`iosApp/`)

### Реальная часть: `iosApp/iosApp/`

**Точка входа**:
- `iOSApp.swift` — `@main`, инициализирует Koin через
  `IosKoinHelperKt.doInitKoin([iosUseCaseModule])`. Обрабатывает deep
  link `locodriver://share/{id}` через `SharedRouteLinkHandler.shared`.
- `ContentView.swift` — корневой view, делегирует `AppCoordinator`.
- `Navigation/AppCoordinator.swift` — `TabView` с 5 вкладками.

**Экраны** (`Screens/`):
| Экран | Файл | Статус |
|---|---|---|
| Home | `Home/HomeView.swift` | 🟢 готов |
| Form (рейс) | `Form/FormView.swift` | 🟢 готов |
| Form Loco | `Form/FormLocoView.swift` | 🟡 частично |
| Form Train | `Form/FormTrainView.swift` | 🟡 частично |
| Form Passenger | `Form/FormPassengerView.swift` | 🟡 частично |
| Settings | `Settings/SettingsView.swift` | 🟡 частично |
| Profile | `Profile/ProfileView.swift` | 🟡 частично |
| Salary Calculation | `SalaryCalculation/SalaryCalculationView.swift` | 🔴 не доделан |
| Work Schedule | `WorkSchedule/WorkScheduleView.swift` | 🔴 не доделан |
| All Routes | `AllRoutes/AllRoutesView.swift` | 🔴 не доделан |
| Search | `Search/SearchView.swift` | 🔴 не доделан |
| Purchases | `Purchases/PurchasesView.swift` | 🔴 не доделан |

**ViewModelWrappers** (`ViewModels/`):
| Wrapper | Соответствующий Kotlin-VM | Статус Kotlin-VM |
|---|---|---|
| `HomeViewModelWrapper` | `HomeIosViewModel` | ✅ есть (подтверждено) |
| `FormViewModelWrapper` | `FormIosViewModel` | ✅ есть (подтверждено) |
| `SettingsViewModelWrapper` | `SettingsIosViewModel` | ✅ есть (подтверждено) |
| `SalaryCalculationViewModelWrapper` | `SalaryCalculationIosViewModel` | ✅ есть (подтверждено) |
| `LocoFormViewModelWrapper` | `LocoFormIosViewModel` | [?] |
| `TrainFormViewModelWrapper` | `TrainFormIosViewModel` | [?] |
| `PassengerFormViewModelWrapper` | `PassengerFormIosViewModel` | [?] |
| `ProfileViewModelWrapper` | `ProfileIosViewModel` | [?] |
| `ViewModelWrapper.swift` | (базовый маркер) | — |

**Shared**:
- `Shared/TimeFormatter.swift` — `msToDate` / `dateToMs` для конверсии
  Long ms ↔ Date.

[?**Нужно проверить через Claude Code**: какие из 4 Wrapper'ов с
пометкой `[?]` имеют реальный Kotlin-VM в `iosApp/src/commonMain/`,
а какие — только Wrapper, без backing-VM (Wrapper подключает
mock/заглушку).]

### Мёртвый груз: `iosApp/src/commonMain/`, `iosApp/src/iosMain/`

Это остатки попытки сделать iOS UI на **Compose Multiplatform**, от
которой ты отказался в пользу нативного SwiftUI. Содержит:
- `MainViewController.kt` — мост Kotlin → UIKit
- `AppNavHost.kt` — навигация на KMP Navigation Compose
- `IosRouterImpl.kt` — реализация `Router`
- `Routes.kt`, экраны на Compose, etc.

🚮 **К удалению**, **кроме**:
- `iosApp/src/commonMain/kotlin/com/z_company/iosapp/di/IosUseCaseModule.kt`
  — этот файл нужен (DI для iOS).
- Возможно — `IosKoinHelper.kt` и `IosViewModelHelper.kt`, если они там.

[?**Перед удалением**: попросить Claude Code сделать аудит
`iosApp/src/`, выяснить, что **реально используется** Swift-частью
(через ComposeApp.framework), а что — мёртвый код. Конкретно — найти
все `@objc` и `public` классы/функции, которые экспортируются в
ComposeApp.framework и потенциально могут вызываться из Swift.]

### Стратегия синхронизации (iOS)

[?**Нужно проверить через Claude Code**: реализована ли фоновая
синхронизация на iOS (BGTaskScheduler, BackgroundTasks framework)?
Или sync вызывается только из ViewModel при действиях пользователя
(refresh, опубликование изменения)?]

⚠️ Если фоновой sync **нет** — это блокер для продакшен-релиза iOS.
Машинист может неделями забыть открыть приложение, и его данные не
попадут на сервер. Нужно решение: либо BGTaskScheduler (Apple строго
ограничивает время выполнения, ~30 секунд), либо очень настойчивые
push-нотификации, либо явное «синхронизировать сейчас» в UI с
напоминаниями.

---

## DI (Koin)

10 модулей. Подробности в `CLAUDE.md` → раздел DI. Краткое:

**Android-side** (5):
`repositoryModule`, `useCaseModule`, `viewModelModule`,
`resourcesModule`, `updateModule`. Собираются в `app/.../LocoDriverApp.kt`
через `startKoin { modules(...) }`.

**iOS-side** (2):
- `iosRepositoryModule` (Ktor + Keychain) — в `data_remote/src/iosMain/`
- `iosUseCaseModule` (всё остальное в одном модуле, потому что
  `data_local` не экспортируется в `ComposeApp.framework`) — в
  `iosApp/src/commonMain/kotlin/com/z_company/iosapp/di/`

Собираются через `IosKoinHelperKt.doInitKoin(additionalModules)`.

**KMP common** (1): `sqlDelightSettingsModule` — в `data_local/.../commonMain`.

**Per-platform actual** (2): `sqlDelightRouteModule` —
`androidMain` и `iosMain` варианты.

---

## Технологический стек

### Версии (на 25 апреля 2026)

| Категория | Библиотека | Версия |
|---|---|---|
| Kotlin | — | 2.2.0 |
| Coroutines | kotlinx-coroutines | 1.7.1 |
| DateTime | kotlinx-datetime | 0.6.2 |
| Serialization | kotlinx.serialization | 1.10.0 |
| HTTP | Ktor | 3.0.3 |
| БД | SQLDelight | 2.0.2 |
| DI | Koin | 3.5.6 |
| Compose (Android) | androidx.compose | (стабильная 2025) |
| Java/Kotlin Target | JVM | 21 |
| Min Android SDK | — | 26 (Android 8.0) |
| Target Android SDK | — | 35 (Android 15) |

### Управление зависимостями

⚠️ Используется **`buildSrc/src/main/kotlin/Dependencies.kt`** —
старый стиль до Gradle Version Catalog. Не критично, но Claude Code
привычнее работать с `gradle/libs.versions.toml`. Миграция возможна,
но не приоритет на 2 недели до релиза iOS.

### Дополнительно (Android-only)

| Категория | Библиотека | Версия |
|---|---|---|
| Auth (VK) | VK ID SDK | 2.6.0 |
| Payments | RuStore + Robokassa SDK | — |
| Background | WorkManager | 2.9.0 |
| Images | Coil | 2.4.0 |
| Camera | CameraX | 1.3.1 |
| Analytics | MyTracker | 3.3.2 |
| Crash | ru.ok.tracer | — |
| Memory leaks | LeakCanary | 2.14 (debug) |
| Encryption | Tink | 1.7.0 |

### Что переехало

| Было (Android) | Стало (KMP) |
|---|---|
| Room | SQLDelight |
| Retrofit + OkHttp | Ktor + Darwin (iOS) |
| Gson | kotlinx.serialization |
| `java.util.Calendar` | `kotlinx-datetime` |
| `java.util.UUID` | `kotlin.uuid.Uuid` |
| `java.math.BigDecimal` | `Double + DoubleAsStringSerializer` |
| `androidx.lifecycle.ViewModel` (Android) | `androidx.lifecycle.ViewModel` (KMP-версия 2.8+) |
| `KoinComponent` (object) | конструкторная инжекция через Koin |

---

## Известные особенности и грабли

### 1. Платёжная интеграция

`robokassa_sdk/` — самописный SDK, Android-only. Для iOS нужно либо
аналог, либо использование Apple In-App Purchase. **На 2 недели до
релиза этим не успеть** — релиз iOS без платной подписки, добавить
позже.

### 2. Глоссарий ключевых терминов

См. `01_GLOSSARY.md` в Project knowledge. Самое важное:
- **Рейс** = `Route`, корневая сущность смены машиниста.
- **Плечо обслуживания** = `ServicePhase`, участок между станциями.
- **Тяжеловесный поезд** — поезд с весом выше норматива (доплата).
- **Длинносоставный поезд** — длина выше норматива.
- **Ночные часы** — рассчитываются по `nightTime` (по умолчанию 22:00–06:00).

### 3. Контракт с сервером

Полный контракт — в `31_API_REFERENCE.md`. Краткие правила в `CLAUDE.md`.
Самое важное:
- **`month` 0-based** в API.
- **`weight`/`axle`/`conditionalLength` поезда** — строки.
- **Клиент сам генерирует UUID** для всех сущностей.
- **`POST /v1/route/`** — full-replace для дочерних коллекций.

### 4. Open issues в коде сервера

Известные проблемы, которые **не нужно копировать в новый код**:
- `isHeavyLongDistance` всегда теряется при синхронизации.
- `int(accepted_energy)` теряет точность при выгрузке.
- Notes накапливают мусор.
- `GET /v1/route/` возвращает 404 при пустом списке.

См. `31_API_REFERENCE.md` → раздел «Известные проблемы».

### 5. Безопасность — что было сделано 25 апреля 2026

- Закрыт IDOR при `POST /v1/route/`
- Удалены `global` race conditions
- БД и Redis закрыты от внешнего мира
- Очищен `pg_hba.conf`
- Убран `--reload` из uvicorn

См. `SECURITY_ACTION_PLAN.md`. Что **осталось** — менее критично, но
помнить.

### 6. Memory leak в iOS-обёртках

В `HomeViewModelWrapper` (и других) подписка через `watchX(callback)`
**не отписывается при деинициализации**. Если Kotlin-VM держится в
Koin как singleton — leak. Решение либо в `deinit`, либо через
scoped Koin-инстанс. Оставлено как технический долг, см. TODO.

---

## Связанные документы

В Project knowledge:
- `00_PROJECT_OVERVIEW.md` — обзор всего проекта
- `01_GLOSSARY.md` — термины
- `02_KMP_MIGRATION_PLAN.md` — миграция и выбор библиотек
- `03_CODING_CONVENTIONS.md` — стандарты кода
- `30_SERVER_ARCHITECTURE.md` — про сервер
- `31_API_REFERENCE.md` — все эндпоинты
- `60_IOS_TODO.md` — задачи на 2 недели

В репозитории:
- `CLAUDE.md` — правила для Claude Code (главный файл)
- `CODEBASE.md` — этот файл
