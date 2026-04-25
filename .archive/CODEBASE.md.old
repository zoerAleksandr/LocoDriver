# LocoDriver — Описание кодовой базы

## Назначение приложения

**LocoDriver** — мобильное Android-приложение для машинистов локомотивов (работников РЖД). Приложение помогает водителям:

- Вести учёт маршрутов/смен с детальной информацией
- Фиксировать данные о локомотивах (расход топлива/энергии)
- Записывать состав поездов и информацию о пассажирах
- Рассчитывать заработную плату по сложным правилам начисления
- Отслеживать рабочее время и графики смен
- Управлять производственным календарём (отпуска, больничные, учёба)
- Синхронизировать данные с удалённым сервером

**Целевая аудитория**: Машинисты локомотивов в России (интерфейс на русском языке, аутентификация через VK ID, платежи через RuStore/Robokassa).

---

## Структура проекта

Проект разделён на независимые gradle-модули по принципу Clean Architecture:

```
LocoDriver/
├── app/                  # Модуль приложения (точка входа, навигация, DI-граф)
├── core/                 # Чистые Kotlin-утилиты и базовые классы
├── core_android/         # Android-специфичные UI-компоненты и утилиты
├── data_local/           # Слой локальных данных (Room БД)
├── data_remote/          # Слой удалённых данных (Retrofit API)
├── domain/               # Доменный слой (бизнес-логика, сущности, use cases)
├── features/
│   ├── route/            # Модуль маршрутов/смен (основной)
│   ├── login/            # Модуль входа в систему
│   └── settings/         # Модуль настроек
├── buildSrc/             # Управление зависимостями и конфигурация сборки
└── robokassa_sdk/        # SDK платежей Robokassa
```

---

## Архитектура

### Общий паттерн: Clean Architecture + MVVM

```
UI (Compose Screen)
    ↕ State/Events
ViewModel
    ↕ Flow<T>
Use Case
    ↕ suspend fun / Flow<T>
Repository (интерфейс из domain)
    ↕
Repository Impl (data_local / data_remote)
    ↕                    ↕
Room Database       Retrofit API
```

### Ключевые архитектурные решения

| Компонент | Решение |
|-----------|---------|
| UI | Jetpack Compose (декларативный UI) |
| Состояние | StateFlow + sealed class `ResultState` |
| DI | Koin (service locator) |
| Async | Kotlin Coroutines + Flow |
| БД | Room с миграциями (12 версий) |
| API | Retrofit + OkHttp |
| Навигация | Jetpack Navigation (Compose) |
| Фоновые задачи | WorkManager (синхронизация каждые 36 часов) |

---

## Доменный слой (`domain/`)

### Основные сущности

**`Route.kt`** — центральная сущность, описывает полную рабочую смену:
```
Route
├── BasicData      — метаданные (время, статус синхронизации, заметки)
├── List<Locomotive> — локомотивы (электровозы и тепловозы)
├── List<Train>    — составы поездов со станциями
├── List<Passenger> — пассажирские поезда
└── List<Photo>    — фотодокументация
```

**`Locomotive.kt`** — данные о локомотиве:
- `SectionElectric` — секция электровоза (расход энергии, показания счётчиков)
- `SectionDiesel` — секция тепловоза (расход топлива, уровни бака)

**`UserSettings.kt`** — пользовательские настройки:
- Минимальное время отдыха, время ночной работы (22:00–06:00 по умолчанию)
- Тип локомотива по умолчанию, фазы службы, часовой пояс

**`SalarySetting.kt`** — параметры расчёта зарплаты:
- Ночные надбавки, районный коэффициент, надбавка за тяжёлые поезда

**`MonthOfYear.kt`** — производственный календарь:
- Тарифные ставки, дни освобождения (отпуск, больничный, учёба)

### Use Cases

| Use Case | Ответственность |
|----------|----------------|
| `RouteUseCase` | CRUD маршрутов, фильтрация по датам |
| `SalaryCalculationUseCase` | Расчёт заработной платы |
| `CalendarUseCase` | Управление производственным календарём |
| `SettingsUseCase` | Операции с настройками пользователя |
| `SalarySettingUseCase` | Конфигурация расчёта зарплаты |
| `TrainUseCase` | Операции с составами поездов |
| `LocomotiveUseCase` | Операции с локомотивами |
| `PassengerUseCase` | Операции с пассажирскими данными |

---

## Слой данных

### `data_local/` — локальное хранилище

**Room БД** с 12 версиями схемы и авто-миграциями:
- `RoomRouteRepository` — маршруты
- `RoomSettingRepository` — пользовательские настройки
- `RoomSalarySettingRepository` — настройки зарплаты
- `RoomCalendarRepository` — производственный календарь

Конверторы (Converter Pattern) для маппинга Room Entity ↔ Domain Entity.

### `data_remote/` — удалённое API

- `RemoteRestApi.kt` — Retrofit-интерфейс для всех эндпоинтов
- `RemoteRestClient.kt` — конфигурация Retrofit (кастомная JSON-сериализация)
- `AuthManager.kt` — аутентификация (email/пароль, VKID)
- `RoutesManager.kt` — синхронизация маршрутов
- `SettingManager.kt` — синхронизация настроек

**Стратегия синхронизации**: Offline-first — данные сначала пишутся в Room, затем синхронизируются с сервером через WorkManager.

---

## Слой представления

### Точка входа: `app/`

- **`StartApp.kt`** — Application-класс; инициализирует Koin, MyTracker аналитику, VK ID SDK, WorkManager
- **`MainActivity.kt`** — единственная Activity; настраивает Compose, обрабатывает deep links (Robokassa, VKID, App Links)
- **`LocoDriverApp.kt`** — корневой Compose-компонент, навигация, темизация

### Основные экраны и их ViewModel

| Экран | ViewModel | Назначение |
|-------|-----------|-----------|
| Home | `HomeViewModel` | Дашборд с маршрутами, рабочими часами, суммой заработка |
| Форма маршрута | `FormViewModel` | Создание/редактирование маршрута |
| Форма локомотива | `LocoFormViewModel` | Ввод данных локомотива |
| Форма поезда | `TrainFormViewModel` | Состав поезда и станции |
| Форма пассажира | `PassengerFormViewModel` | Пассажирский поезд |
| Поиск | `SearchViewModel` | Поиск и фильтрация маршрутов |
| Расчёт ЗП | `SalaryCalculationViewModel` | Отображение расчёта заработка |
| Настройки | `SettingsViewModel` | Пользовательские настройки |
| Настройки ЗП | `SettingSalaryViewModel` | Параметры расчёта зарплаты |
| Профиль | `ProfileViewModel` | Аккаунт пользователя |
| Покупки | `PurchasesViewModel` | Покупки через Robokassa |
| График работы | `WorkScheduleViewModel` | Календарный вид графика |
| Все маршруты | `AllRouteViewModel` | Список всех маршрутов с фильтрами |

### Навигация

- Bottom Navigation Bar с 5 вкладками: Главная, Расчёт ЗП, Форма, Настройки, Профиль
- Модальные формы для создания/редактирования маршрутов и вложенных сущностей
- Deep links для обработки возврата из платёжной системы и входа через VK ID

---

## Расчёт заработной платы

Реализован в `SalaryCalculationUseCase` и `SalaryCalculationHelper`. Учитывает:

- Базовую тарифную ставку за месяц
- Ночные надбавки (40% за работу с 22:00 до 06:00)
- Надбавку за работу в одно лицо (40–50%)
- Надбавку за тяжёлые поезда
- Надбавку за расширенную фазу службы
- Районный коэффициент
- Удержания (НДФЛ, профсоюзные взносы)

---

## Технологический стек

### Основной фреймворк
- **Kotlin 2.2.0** + **Jetpack Compose** — декларативный UI
- **Min SDK 26** (Android 8.0), **Target SDK 35** (Android 15)
- **Java/Kotlin Target: 21**

### Ключевые библиотеки

| Категория | Библиотека | Версия |
|-----------|-----------|--------|
| DI | Koin | 3.5.6 |
| БД | Room | 2.7.1 |
| API | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Serialization | Gson + kotlinx.serialization | — |
| Async | Kotlin Coroutines | 1.7.1 |
| Background | WorkManager | 2.9.0 |
| Auth | VK ID SDK | 2.6.0 |
| Payments | Robokassa SDK (custom) + RuStore | — |
| Images | Coil | 2.4.0 |
| Camera | CameraX | 1.3.1 |
| UI | Material Design 3, Accompanist | — |
| Analytics | MyTracker | 3.3.2 |
| Crash reporting | ru.ok.tracer | — |
| Memory leaks | LeakCanary | 2.14 (debug) |
| Encryption | Tink | 1.7.0 |
| DateTime | kotlinx-datetime | 0.6.2 |

---

## Паттерны кода

| Паттерн | Где используется |
|---------|----------------|
| `ResultState<T>` sealed class | Success / Loading / Error состояния во всех ViewModel |
| Converter Pattern | Маппинг Room Entity ↔ Domain Entity |
| Repository Pattern | Абстракция источника данных (local / remote) |
| Helper Pattern | `RouteActionsHelper`, `SalaryCalculationHelper` |
| Manager Pattern | `AuthManager`, `RoutesManager`, `SettingManager` |
| Offline-first | Room как источник истины, фоновая синхронизация |

---

## Безопасность

- Зашифрованное хранилище токенов (DataStore + Tink)
- HTTPS для всех API-вызовов (OkHttp SSL)
- Верификация deep links (Intent filter auto-verification)
- Секреты в `secrets.properties` (не хранятся в репозитории)

---

## Конфигурация сборки

- **Gradle 8.9.2** с Kotlin DSL
- **KSP** (Kotlin Symbol Processing) для кодогенерации Room
- **Версия приложения**: 2.1.6 (build 64)
- ProGuard/R8 для продакшна (минификация отключена в текущей конфигурации)
- Core Library Desugaring для поддержки Java 21 API на старых Android

---

## Поток данных (пример)

```
Пользователь открывает Home Screen
    → HomeViewModel.uiState (StateFlow)
        → RouteUseCase.routeListByMonthFlow()
            → RoomRouteRepository.loadRouteByPeriodFlow()
                → Room DAO → Flow<List<RouteEntity>>
                    → Converter → Flow<List<Route>>
                        → ViewModel → UI State
                            → Compose recomposition
```

```
Пользователь сохраняет маршрут
    → FormViewModel.saveRoute()
        → RouteUseCase.saveRoute()
            → RoomRouteRepository.saveRoute()
                → Room DAO (локально сохранено)
    → WorkManager запускает SyncWorker (фон)
        → RoutesManager.syncRoutes()
            → RemoteRestApi (Retrofit)
                → Сервер обновлён
```
