# CLAUDE.md — LocoDriver

> **Этот файл читается Claude Code в начале каждой сессии.** Здесь —
> правила, которые Claude Code должен соблюдать ВСЕГДА, и контекст,
> который не очевиден из самого кода. Подробности — в `CODEBASE.md` и
> в Project knowledge (документы 00–60).

---

## Что это за проект

**LocoDriver** — приложение для машинистов локомотивов (грузовое и
пассажирское движение). В релизе на Android, в активной разработке на iOS.

**Состав:**
- **Клиент**: Kotlin Multiplatform (Android + iOS), архитектура Clean + MVVM.
- **Сервер**: Python/FastAPI/PostgreSQL, отдельный репозиторий
  (`/Users/zoer/Downloads/proxy-parser/`). Сейчас в продакшене.
- **Сайт**: на отдельном домене с HTTPS, только для просмотра расшаренных
  маршрутных листов через `GET /v1/share/route/{id}`.

---

## Текущая фаза разработки

🎯 **Главный приоритет**: релиз iOS-версии (паритет с Android).

iOS на 30-40% готов: главный экран, FormScreen, авторизация по email,
синхронизация, расчёт времени работают. Осталось: дочерние формы,
WorkScheduleView, SearchView, расчёт зарплаты, AllRoutes, Profile до конца,
Purchases, авторизация по VK ID, чистка от Compose Multiplatform.

📋 См. `60_IOS_TODO.md` для актуального roadmap'а.

---

## 🛑 ЖЁСТКИЕ ПРАВИЛА — НИКОГДА НЕ НАРУШАТЬ

### 1. Сервер и Android в продакшене

**Любое изменение JSON-контракта может сломать существующих пользователей.**
- Старые версии Android-клиента до сих пор устанавливаются и работают.
- Сервер обслуживает реальных машинистов, потеря данных недопустима.

Перед изменением имени поля, типа, наличия в API — **остановиться и
спросить пользователя**: «Это изменение контракта. Старые клиенты могут
сломаться. Точно нужно?»

### 2. UI-стек зафиксирован

- **iOS** — нативный SwiftUI. Никакого Compose Multiplatform.
  Если видишь упоминания CMP/`MainViewController.kt`/Compose-кода в
  `iosApp/src/commonMain` или `iosApp/src/iosMain` — это мёртвый груз,
  оставшийся от попытки. Удаляется в рамках TODO.
- **Android** — Jetpack Compose в `features/`.
- **Общая логика** — `domain`, `data_local`, `data_remote`, `core`
  (через `commonMain`).

### 3. KMP-ограничения для общего кода

В `commonMain` любого модуля **запрещено**:
- `java.*`, `javax.*`, `android.*`
- `java.util.Calendar`, `java.util.Date`, `java.util.TimeZone`
- `java.util.UUID`
- `java.math.BigDecimal`
- `java.io.Serializable`
- Gson, Retrofit, Room, OkHttp, Glide, Coil, Picasso

**Используй вместо этого:**
- `kotlinx-datetime` (Instant, LocalDateTime, TimeZone, Clock)
- `kotlin.uuid.Uuid` (Kotlin 2.0+, требует `@OptIn(ExperimentalUuidApi::class)`)
- `Double + DoubleAsStringSerializer` (вместо BigDecimal — точность для
  топлива/энергии достаточна)
- `kotlinx.serialization` (`@Serializable`, `@SerialName`)
- Ktor Client (через `expect/actual` engine factory)
- SQLDelight (`AndroidSqliteDriver` / `NativeSqliteDriver`)

### 4. Безопасность — что недавно было закрыто

🔒 **Не сломать!** В `pg_client.py:PostgresRouteDbClient.process` есть
проверка владельца перед upsert (защита от IDOR). Класс
`RouteOwnershipError` ловится в `route.py:save_data` и возвращает 404.
**Не убирать эту проверку.**

🔒 **Не возвращать `global` переменные** в `route.py`. Раньше там были
`global passengers, trains, locomotives, basic_data` — это race condition,
который был устранён.

🔒 **БД и Redis на сервере привязаны к 127.0.0.1**, наружу не выставлены.
Не менять `docker-compose.yml` обратно на `0.0.0.0`.

### 5. Логирование и PII

Не логируй в `LogManager.log_*`:
- Пароли, JWT-токены, refresh-токены
- Email-адреса, vk_id
- Полное содержимое `SyncData` (есть PII пользователей)

Текущие логи на сервере **уже нарушают эти правила** (записывают `data=...`).
Это известная проблема, в `SECURITY_ACTION_PLAN.md` помечена 🟡 — фиксить
постепенно, не вводить новых таких логов.

---

## 🌐 JSON-контракт с сервером

**Полная карта** — в `31_API_REFERENCE.md`. Здесь — только то, что
не очевидно из кода и часто ломается:

### Имена полей
- **camelCase** для большинства полей.
- **Исключения** (snake_case в API):
  - `track_number` (станция)
  - `auth_param` (логин)
  - `vk_id` (только в response `UserSafeResponse`)

### Типы и форматы
- **Время**: `Long` миллисекунды от Unix epoch (UTC).
  ⚠️ **Исключение**: эндпоинты `/v1/norma_time/locomotives/` и
  `/v1/norma_time/stations/` возвращают `updatedAt` как `Double`
  (например `1779032766922.0`). Конвертировать в `Long` через `.toLong()`
  в DTO-маппере (`NormaTimeLocomotiveResponse`, `NormaTimeStationResponse`).
- **Даты без времени**: три поля — `year`, `month`, `dayOfMonth`.
  ⚠️ **`month` 0-based** (январь = 0, декабрь = 11). Это контракт со
  старым Android-клиентом, который использовал `Calendar.MONTH`.
  При работе с `kotlinx-datetime.Month` (1-based) — конвертировать
  только в DTO-маппере, не менять формат на сервере.
- **UUID**: строки в JSON. Клиент сам генерирует UUID для рейсов,
  локомотивов, поездов, секций, пассажиров, станций. Сервер не присваивает.
- **`weight`, `axle`, `conditionalLength` поезда**: **строки**, не числа
  (наследие Gson). На сервере конвертируются. Не менять.
- **`distance` поезда**: **строка** на клиенте и в БД.

### Семантика синхронизации
- `POST /v1/route/` принимает рейс **целиком** (`SyncData`).
- **Full-replace** для дочерних коллекций: что не упомянуто в
  `locomotives`/`trains`/`passengers` — удаляется на сервере.
- Если `basicData.isDeleted == true` → рейс удаляется (страховка от
  потери `DELETE /v1/route/{id}`).
- `POST /v1/norma_time/locomotives/` и `POST /v1/norma_time/stations/` —
  **full replace на сервере**: сервер удаляет все старые записи и
  вставляет пришедшие. Клиент вызывает POST только если есть локальные
  данные; если локальных нет — делает GET (см. стратегию синхронизации в `CODEBASE.md`).

### Известные баги контракта (не копировать в новый код!)
- `isHeavyLongDistance` всегда теряется при синхронизации
  (в POST игнорируется, в GET хардкод `False`).
- `int(accepted_energy)` теряет точность float при выгрузке.
- `Notes` накапливают мусор (старые не удаляются).
- `GET /v1/route/` возвращает 404 при пустом списке (должно быть `200 []`).
- `fuelSupplyKg` (GET) vs `fuelSupplyInKilo` (POST) — расхождение
  имён в DieselSection.
- `surchargeLongTrainsList` есть на сервере, нет в Kotlin
  `SalarySetting.kt` — НЕ добавлять до релиза iOS, чтобы не было
  сюрпризов с full-replace.
- `photos` в SyncData — устаревшее поле, в БД нет таблицы. Сервер
  игнорирует, клиент шлёт `[]`. Не удалять без координированной миграции.

---

## Архитектура клиента (KMP)

Подробности — в `CODEBASE.md`. Здесь — карта модулей:

```
app/                  Android entry point + DI-граф (Koin)
├─ core/             ✅ KMP (jvm + androidTarget)
├─ core_android/     Android-only утилиты
├─ domain/           ✅ KMP (jvm + androidTarget + iOS)
├─ data_local/       ✅ KMP (SQLDelight, AndroidSqliteDriver/NativeSqliteDriver)
├─ data_remote/      ✅ KMP (Ktor, AndroidClientEngine/Darwin)
├─ features/         Android-only Compose UI (route, login, settings, ...)
├─ iosApp/           iOS:
│   ├─ src/commonMain  🚮 МЁРТВЫЙ ГРУЗ от CMP — оставить только IosUseCaseModule.kt
│   ├─ src/iosMain     🚮 МЁРТВЫЙ ГРУЗ — удалить
│   └─ iosApp/         ✅ SwiftUI entry point + Screens + ViewModelWrappers
└─ robokassa_sdk/    Android-only платежи
```

### Зависимости направления
```
features (Android UI) → domain ← data_local, data_remote
                              ↓
                            core
```

`domain` ничего не знает о `data_*` (только интерфейсы). `data_*` ничего
не знает о `features` (наоборот — `features` зависит).

---

## DI (Koin)

10 модулей, разделение по платформам:

**Android-side (собираются в `LocoDriverApp.kt`)**:
- `repositoryModule` — Room (legacy), Ktor, Tink SecureStorage
- `useCaseModule` — все use cases
- `viewModelModule` — Android ViewModel'и
- `resourcesModule` — Context, AssetManager
- `updateModule` — RuStore AppUpdateManager

**iOS-side (собираются в `iosApp/src/commonMain/.../IosKoinHelper.kt`)**:
- `iosRepositoryModule` — Ktor (Darwin engine) + Keychain SecureStorage
- `iosUseCaseModule` — SQLDelight + БД + репозитории + UseCases + iOS-VM
  (всё в одном модуле, потому что `data_local` не экспортируется в
  `ComposeApp.framework`)

**Общие KMP**:
- `sqlDelightSettingsModule` — `commonMain`
- `sqlDelightRouteModule` — `androidMain` / `iosMain` (expect/actual)

**ВАЖНО**: при добавлении новых iOS-ViewModel — добавлять их в
`iosUseCaseModule.kt` и в `IosViewModelHelper.kt` (singleton-фасад,
через который Swift получает ViewModels).

---

## Паттерн SwiftUI ↔ Kotlin ViewModel

Используется самописный `ViewModelWrapper` на основе callback-функций
`watchX(callback)` в Kotlin-ViewModel. **SKIE не используется** — переделка
не оправдана за 2 недели до релиза.

### Шаблон Kotlin-ViewModel для iOS:

```kotlin
class HomeIosViewModel(
    private val routeUseCase: RouteUseCase,
    // ...
) : ViewModel() {
    private val _routes = MutableStateFlow<List<DomainRoute>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            routeUseCase.routesFlow().collect { _routes.value = it }
        }
    }

    fun watchRoutes(callback: (List<DomainRoute>) -> Unit) {
        viewModelScope.launch {
            _routes.collect { callback(it) }
        }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.collect { callback(it) }
        }
    }

    fun deleteRoute(routeId: String) {
        viewModelScope.launch { routeUseCase.deleteRoute(routeId) }
    }
}
```

### Шаблон SwiftUI Wrapper:

```swift
@MainActor
final class HomeViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getHomeViewModel()

    @Published var routes: [DomainRoute] = []
    @Published var isLoading: Bool = true

    init() {
        viewModel.watchRoutes { [weak self] list in
            DispatchQueue.main.async {
                self?.routes = list as? [DomainRoute] ?? []
            }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
    }

    func deleteRoute(routeId: String) {
        viewModel.deleteRoute(routeId: routeId)
    }
}
```

### ⚠️ Известные ограничения этого паттерна
- **Memory leak**: подписки `watchX(callback)` не отписываются при
  деинициализации Wrapper. Если Kotlin-ViewModel живёт дольше Swift-View
  (singleton в Koin) — leak. **Решение**: либо scoped Koin-инстанс на
  жизнь экрана, либо добавить `cancelWatchers()` в `deinit` Wrapper'а.
- **iOS 17 syntax** в `.onChange(of:) { _ in }` (без `oldValue`) —
  требует Deployment Target ≥ iOS 17. Проверить в проекте.

---

## Стандарты кода

### Kotlin
- `data class` с `val` (не `var`). Изменения через `.copy()`.
- `@Serializable` обязателен для всех DTO и доменных моделей,
  передающихся по сети.
- Имена полей в JSON — camelCase, кроме исключений (см. контракт выше).
- Время — `Long` ms внутри, `kotlinx.datetime.Instant` при работе с тайм-зонами.
- Корутины: `Dispatchers.Default` (не `IO`, потому что `IO` — JVM-only).
- Логирование — Kermit (когда подключим), сейчас — `println()` или
  Android-`Log` в платформенных модулях.

### Swift
- `@MainActor` на всех Wrapper'ах (UI-апдейты на главном потоке).
- `@Published` для observable-свойств.
- `[weak self]` в callback'ах из Kotlin.
- `DispatchQueue.main.async { ... }` оборачивает любое изменение
  `@Published` (callback может приходить с фонового потока Kotlin).
- Минимум force-unwrap (`!`). Используй `?` или `guard let`.

### Python (на сервере)
- Type hints везде.
- Pydantic-схемы с `extra = 'ignore'` (для forward-compatibility).
- Async всё что трогает БД и сеть.
- **Никаких `global` в роутерах.**

---

## Диагностика и отладка

### Как запустить
**Android**:
```bash
./gradlew :app:assembleDebug
# или Android Studio Run
```

**iOS**:
1. Открыть `iosApp/iosApp.xcodeproj` в Xcode.
2. Выбрать симулятор/устройство.
3. Run. Xcode сам вызовет:
   ```
   ./gradlew :iosApp:embedAndSignAppleFrameworkForXcode
   ```

### Адрес API (на 25 апреля 2026)
- **Android-клиент** ходит на `http://87.228.110.32:8766/`
- Email/page-эндпоинты: `http://locodrivers.freemyip.com/`

⚠️ HTTP без TLS. Запланирован переезд на HTTPS — отдельная задача.

### Где код сервера
`/Users/zoer/Downloads/proxy-parser/` (отдельный репозиторий).
SSH на прод: `ssh root@87.228.110.32`.

---

## Что делать, если задача неоднозначна

1. **Если задача меняет JSON-контракт с сервером** — остановиться и
   спросить пользователя, прежде чем менять.
2. **Если задача требует изменения схемы БД** — спросить про миграцию
   (текущая Android-аудитория должна продолжить работать).
3. **Если задача в файлах `iosApp/src/commonMain/` или
   `iosApp/src/iosMain/` (старый CMP-код)** — не делать там новый код,
   только удалять или переносить в `iosApp/iosApp/`.
4. **Если задача требует библиотек, которых нет в проекте** — предложить
   варианты (KMP-совместимые!), но не добавлять без подтверждения.

---

## Ссылки на документы Project knowledge

В Claude.ai Project (не в репозитории):

- `00_PROJECT_OVERVIEW.md` — общая карта
- `01_GLOSSARY.md` — термины (плечо, тяжеловесный, ночные и т.п.)
- `02_KMP_MIGRATION_PLAN.md` — план миграции и выбор библиотек
- `03_CODING_CONVENTIONS.md` — стандарты кода
- `30_SERVER_ARCHITECTURE.md` — про сервер
- `31_API_REFERENCE.md` — все эндпоинты, контракты, известные проблемы
- `60_IOS_TODO.md` — задачи на 2 недели до релиза iOS
- `SECURITY_ACTION_PLAN.md` — оставшиеся задачи по безопасности
  (некритичные)

В репозитории (рядом с этим файлом):
- `CODEBASE.md` — детальное описание кодовой базы

---

## Краткое резюме для tl;dr

1. **iOS на SwiftUI**, Compose Multiplatform убрали.
2. **Сервер и Android в проде** — обратная совместимость превыше всего.
3. **Все JSON-имена камелКейс**, кроме `track_number`/`auth_param`/`vk_id`.
4. **Месяц 0-based** в API, конвертировать в маппере.
5. **`val`, не `var`. `kotlinx.serialization`, не Gson.
   `kotlinx-datetime`, не Calendar.**
6. **iOS-ViewModel = Kotlin-VM с `watchX()` callbacks + SwiftUI Wrapper
   с `@Published`.** SKIE не используется.
7. **При сомнении — спрашивай пользователя, не угадывай.**
