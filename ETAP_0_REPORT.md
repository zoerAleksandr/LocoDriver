# ETAP 0 — Discovery. Полный отчёт

> Справочник по результатам Этапа 0 (Discovery) из `60_IOS_TODO.md`.
> Создан 2026-04-25, к нему возвращаемся в последующих этапах.
> Все ссылки `file:line` валидны на момент создания (ветка `feature/regional-holidays`).

**Статус задач**: 0.1, 0.2, 0.3, 0.4, 0.5 — все ✅ ВЫПОЛНЕНО.

**Найдено блокеров релиза**: 2.
1. Фоновая синхронизация на iOS отсутствует → **Задача 1.4** в Этапе 1.
2. Sign in with Apple на сервере отсутствует → отдельная задача в `proxy-parser/`, до Этапа 4.

---

## Содержание

- [0.1 — Аудит iOS-проекта](#01--аудит-ios-проекта)
- [0.2 — Аудит мёртвого CMP-кода](#02--аудит-мёртвого-cmp-кода)
- [0.3 — Миграция Room → SQLDelight](#03--миграция-room--sqldelight)
- [0.4 — Фоновая синхронизация на iOS](#04--фоновая-синхронизация-на-ios)
- [0.5 — Готовность серверного API к Sign in with Apple](#05--готовность-серверного-api-к-sign-in-with-apple)
- [Итоги](#итоги-этапа-0)

---

## 0.1 — Аудит iOS-проекта

### Сводная таблица экранов и Wrappers

| Экран | SwiftUI статус | Wrapper | Kotlin-VM | Замечания |
|---|---|---|---|---|
| **HomeView** (`Home/HomeView.swift`) | 🟢 готов | `HomeViewModelWrapper` | ✅ `HomeIosViewModel.kt` | Все данные через VM (`routes`, `settings`, `currentMonth/Year`). 3 свайп-страницы статистики, удаление/копирование, нав. между месяцами. **Нюанс**: тулкарта "Отвлечения" открывает inline-`stubScreen("Скоро")` (line 437, 458–466). |
| **FormView** (`Form/FormView.swift`) | 🟢 готов | `FormViewModelWrapper` | ✅ `FormIosViewModel.kt` | Полный CRUD рейса. Save в toolbar, навигация в дочерние формы. Поля break (`timeStartBreak/EndBreak`) **не отображаются**, хотя они есть в settings (`isShowBreak`). |
| **FormLocoView** (`Form/FormLocoView.swift`) | 🟡 частично | `LocoFormViewModelWrapper` | ✅ `LocoFormIosViewModel.kt` | Базовые поля (серия, номер, тип, время приёмки/сдачи) — работают. **Секции (electric/diesel) — read-only** (line 59–95): отображаются `acceptedEnergy/deliveryEnergy`, но нет TextField для ввода. **Нет toolbar `Сохранить`** (только реактивно по `vm.isSaved`). |
| **FormTrainView** (`Form/FormTrainView.swift`) | 🟡 частично | `TrainFormViewModelWrapper` | ✅ `TrainFormIosViewModel.kt` | Только базовые характеристики поезда + `isHeavy`. **Нет станций (`stations`)**, нет фазы обслуживания (`servicePhase`), нет сохранения через toolbar. |
| **FormPassengerView** (`Form/FormPassengerView.swift`) | 🟢 готов | `PassengerFormViewModelWrapper` | ✅ `PassengerFormIosViewModel.kt` | Полный набор полей (номер, станции, время, заметки), save в toolbar, расчёт длительности, корректные DatePicker'ы. |
| **SettingsView** (`Settings/SettingsView.swift`) | 🟢 готов | `SettingsViewModelWrapper` | ✅ `SettingsIosViewModel.kt` | Hub + 5 разделов: Норма, Учёт, Отдых, Маршрут, Локомотив. Все toggle/stepper'ы пишут в VM. **Не покрыто**: настройки зарплаты (отдельный SalarySetting), региональные праздники, районный коэффициент, decimal time. |
| **ProfileView** (`Profile/ProfileView.swift`) | 🟡 частично | `ProfileViewModelWrapper` | ✅ `ProfileIosViewModel.kt` | Email/пароль логин — работает; sync, logout — работают. **VK ID — заглушка** (line 21–25, 94–104): только alert «Скоро». Нет регистрации, нет смены пароля. |
| **SalaryCalculationView** (`SalaryCalculation/SalaryCalculationView.swift`) | 🟡 частично | `SalaryCalculationViewModelWrapper` | ✅ `SalaryCalculationIosViewModel.kt` | Month nav + Summary + список маршрутов — работают через VM. **Не доделано**: разбивка по надбавкам (тяжеловесные, длинносоставные, удлинённое плечо, в одно лицо, районный, северные), нет редактирования `SalarySetting`. CODEBASE.md помечает 🔴, фактически ближе к 🟡. |
| **WorkScheduleView** (`WorkSchedule/WorkScheduleView.swift`) | 🔴 заглушка | переиспользует `HomeViewModelWrapper` | — (нет своего VM) | Простой календарный grid на **текущий месяц** (`Calendar.current`, line 10–11), **нет навигации** между месяцами. Нет производственного календаря (`workingDay/shortenedDay`), нет release days (отпуск/больничный/курсы), нет тапа по дню. Только цветные ячейки + точка для дней с маршрутом. |
| **AllRoutesView** (`AllRoutes/AllRoutesView.swift`) | 🟡 частично | переиспользует `HomeViewModelWrapper` (`@ObservedObject`) | — (нет своего VM) | Список + swipe-to-delete. **Не доделано**: фильтры (по месяцу/году/тип), поиск, группировка по дням, сортировка, пагинация. Привязан к выбранному в Home месяцу — нельзя посмотреть «всё за всё время». |
| **SearchView** (`Search/SearchView.swift`) | 🔴 заглушка | — | — | Только `@State query`, никакого VM. `Text("Нет истории поиска")` и `Text("Поиск: «\(query)»...")` (line 8–18). Полностью placeholder. |
| **PurchasesView** (`Purchases/PurchasesView.swift`) | 🔴 заглушка | — | — | Хардкод массива продуктов (line 4–8), кнопки `Купить`/`Восстановить` с `// TODO` (line 21, 31). Платежей нет. По CLAUDE.md релиз iOS планируется без подписки — ожидаемо. |

### Wrappers без Kotlin-VM

**Нет таких.** Все 8 Wrappers (`Home`, `Form`, `LocoForm`, `TrainForm`, `PassengerForm`, `Settings`, `Profile`, `SalaryCalculation`) подключены к реальному `*IosViewModel.kt` в `iosApp/src/commonMain/kotlin/com/z_company/iosapp/viewmodel/` через `IosViewModelHelper.shared.getXxxViewModel()`.

### Kotlin-VM в shared без подключённого Wrapper

**Нет таких** в основном репозитории (`iosApp/src/commonMain/`).

⚠️ **Примечание**: в worktree `.claude/worktrees/wonderful-cori/` есть **9-й** Kotlin-VM — `AppInitIosViewModel.kt`, которого нет в основном дереве. Это, видимо, in-progress работа в отдельном worktree (init flow приложения). В основной ветке его нет, и Wrapper'а тоже нет.

### Wrappers / экраны, требующие новых Kotlin-VM

| Что нужно | Зачем |
|---|---|
| `WorkScheduleIosViewModel` | Производственный календарь, release days, навигация по месяцам, создание маршрута по тапу. |
| `SearchIosViewModel` | История запросов (есть `HistoryResponseRepository` в domain), полнотекстовый поиск по рейсам. |
| `AllRoutesIosViewModel` (опционально) | Сейчас переиспользует `HomeViewModelWrapper` — допустимо для MVP, но фильтры/группировка просят отдельной VM. |
| `PurchasesIosViewModel` | Только когда будет решение по платежам на iOS (App IAP / отложить). По CLAUDE.md — после релиза. |

### Приоритеты для доработки (на 2 недели до релиза)

#### Блокеры релиза
1. **WorkScheduleView** — критичная фича Android, на iOS пустышка. Нужен новый Kotlin-VM + загрузка `MonthOfYear` из календаря.
2. **SearchView** — без него навигация по большому архиву рейсов невозможна. Нужен Kotlin-VM + интеграция с `HistoryResponseRepository`.
3. **AllRoutesView** — нужны как минимум фильтры по месяцу (сейчас завязан на Home).
4. **FormLocoView (секции)** — без редактирования секций пользователь не может вписать энергию/топливо. Требуется UI для `acceptedEnergy/deliveryEnergy/acceptedFuel/deliveryFuel` и сохранение в `LocoFormIosViewModel`.
5. **FormTrainView (станции, фаза)** — тяжеловесный/длинносоставный есть, но нет станций по плечам — это часть бизнес-логики расчёта.

#### Высокий приоритет
6. **ProfileView (VK ID)** — заглушка вместо рабочего входа, надо либо реализовать (CLAUDE.md — авторизация по VK ID в TODO), либо явно скрыть кнопку до релиза.
7. **SalaryCalculationView (детализация надбавок)** — сейчас только summary, нет разбивки по 10 надбавкам, как в Android.
8. **FormView (поля break)** — не отображаются, хотя `isShowBreak` уже есть в settings.

#### Низкий приоритет
9. **PurchasesView** — по решению CLAUDE.md релиз iOS без платежей.
10. **HomeView (Отвлечения)** — inline `stubScreen("Скоро")`, не блокер.

**Итог 0.1**: 4 экрана 🟢, 4 экрана 🟡, 4 экрана 🔴. 0 mock-Wrapper'ов. Все существующие Kotlin-VM подключены. Главная проблема — **отсутствие Kotlin-VM** для WorkSchedule/Search/Purchases (и неявно — для AllRoutes), а также неполный UI в FormLoco/FormTrain.

---

## 0.2 — Аудит мёртвого CMP-кода

### Контекст

Все Wrappers (`iosApp/iosApp/ViewModels/`) используют только `IosViewModelHelper.shared.getXxxViewModel()`. Swift-сторона:
- `iOSApp.swift` импортирует `IosKoinHelperKt`, `IosUseCaseModuleKt`, `SharedRouteLinkHandler`.
- `ContentView.swift` делегирует к `AppCoordinator()` (SwiftUI), **не использует `MainViewController`**.
- 19 Swift-файлов импортируют `ComposeApp`, но обращаются только к KMP-моделям/ViewModel'ам/Helper'ам, не к Compose UI.

`IosKoinHelper.kt` лежит в `data_remote/src/iosMain/kotlin/com/z_company/di/` (вне `iosApp/src/`) — за рамками этой задачи, но используется и трогать его не надо.

### 🟢 КЕЕП — обязательно оставить (12 файлов)

| Файл | Назначение | Кто использует |
|---|---|---|
| `iosApp/src/commonMain/.../di/IosUseCaseModule.kt` | DI-модуль iOS (БД, репозитории, UseCases, ViewModels) | `iOSApp.swift:20` (`IosUseCaseModuleKt.iosUseCaseModule`) |
| `iosApp/src/commonMain/.../di/IosViewModelHelper.kt` | Фасад для получения ViewModels из Koin | Все 8 Wrapper'ов в `iosApp/iosApp/ViewModels/` |
| `iosApp/src/commonMain/.../repository/IosSharedPreferencesRepository.kt` | iOS-реализация `SharedPreferencesRepositories` (in-memory stub) | Регистрируется в `IosUseCaseModule.kt:69` |
| `iosApp/src/commonMain/.../deeplink/SharedRouteLinkHandler.kt` | Обработка deep-link `locodriver://share/{id}` | `iOSApp.swift:31` (`SharedRouteLinkHandler.shared.handle`) |
| `iosApp/src/commonMain/.../viewmodel/HomeIosViewModel.kt` | KMP ViewModel главного экрана | `IosViewModelHelper:25,34` + `HomeViewModelWrapper.swift:6` |
| `iosApp/src/commonMain/.../viewmodel/FormIosViewModel.kt` | KMP ViewModel формы рейса | `IosViewModelHelper:26,35` + `FormViewModelWrapper.swift:6` |
| `iosApp/src/commonMain/.../viewmodel/SettingsIosViewModel.kt` | KMP ViewModel настроек | `IosViewModelHelper:27,36` + `SettingsViewModelWrapper.swift:6` |
| `iosApp/src/commonMain/.../viewmodel/SalaryCalculationIosViewModel.kt` | KMP ViewModel расчёта ЗП | `IosViewModelHelper:28,37` + `SalaryCalculationViewModelWrapper.swift:6` |
| `iosApp/src/commonMain/.../viewmodel/LocoFormIosViewModel.kt` | KMP ViewModel формы локомотива | `IosViewModelHelper:29,38` + `LocoFormViewModelWrapper.swift:6` |
| `iosApp/src/commonMain/.../viewmodel/TrainFormIosViewModel.kt` | KMP ViewModel формы поезда | `IosViewModelHelper:30,39` + `TrainFormViewModelWrapper.swift:6` |
| `iosApp/src/commonMain/.../viewmodel/PassengerFormIosViewModel.kt` | KMP ViewModel формы пассажирской поездки | `IosViewModelHelper:31,40` + `PassengerFormViewModelWrapper.swift:6` |
| `iosApp/src/commonMain/.../viewmodel/ProfileIosViewModel.kt` | KMP ViewModel профиля | `IosViewModelHelper:32,41` + `ProfileViewModelWrapper.swift:6` |

### 🚮 УДАЛИТЬ — мёртвый CMP-код (11 файлов, ~1215 строк)

| Файл | Строк | Чем подтверждено что мёртвый |
|---|---|---|
| `iosApp/src/iosMain/.../MainViewController.kt` | 16 | `ContentView.swift` использует `AppCoordinator()`, не `MainViewController_iosKt`. Swift нигде не вызывает `MainViewController()`. |
| `iosApp/src/commonMain/.../App.kt` | 39 | Compose root. Используется **только** из `MainViewController.kt:15`. |
| `iosApp/src/commonMain/.../navigation/AppNavHost.kt` | 98 | KMP NavHost (Compose Navigation). Используется **только** из `App.kt:36`. |
| `iosApp/src/commonMain/.../navigation/IosRouterImpl.kt` | 68 | `internal class`, реализация `domain.navigation.Router`. Используется **только** из `AppNavHost.kt:29`. В DI не регистрируется. |
| `iosApp/src/commonMain/.../navigation/Routes.kt` | 65 | Маршруты для Compose Navigation (`HomeRoute`, `FormRoute`, …). Используется **только** из `IosRouterImpl.kt`/`AppNavHost.kt`. |
| `iosApp/src/commonMain/.../screen/HomeScreen.kt` | 369 | Compose-экран. Используется **только** из `AppNavHost.kt:13`. |
| `iosApp/src/commonMain/.../screen/FormScreen.kt` | 184 | Compose-экран. Используется **только** из `AppNavHost.kt:12`. |
| `iosApp/src/commonMain/.../screen/SettingsScreen.kt` | 111 | Compose-экран. Используется **только** из `AppNavHost.kt:16`. |
| `iosApp/src/commonMain/.../screen/ProfileScreen.kt` | 67 | Compose-экран. Используется **только** из `AppNavHost.kt:14`. |
| `iosApp/src/commonMain/.../screen/SalaryCalculationScreen.kt` | 137 | Compose-экран. Используется **только** из `AppNavHost.kt:15`. |
| `iosApp/src/commonMain/.../screen/StubScreen.kt` | 61 | Compose-stub. Используется **только** из `AppNavHost.kt:17`. |

Все они образуют **замкнутый граф зависимостей**: `MainViewController → App → AppNavHost → {IosRouterImpl, Routes, *Screen}`. Снаружи этого графа их никто не вызывает (ни Swift, ни DI, ни тесты).

### ⚠️ Замечания перед удалением

1. **Deep link → переход на FormView не работает на текущем Swift-UI.**
   `SharedRouteLinkHandler.pendingFormRouteId` (StateFlow) сейчас читается **только** в `AppNavHost.kt:37` (`collectAsState`). После удаления CMP-кода никто не подпишется на этот flow — Swift-сторона импортирует обработчик в `iOSApp.swift`, но **не наблюдает** `pendingFormRouteId`/`errorMessage` (грепом подтверждено: 0 совпадений в `iosApp/iosApp/`).
   **Действие на этап 1.1 (удаление CMP)**: добавить наблюдение `pendingFormRouteId` в Swift (например, в `AppCoordinator` через `ViewModelWrapper`), иначе клик по shared-ссылке загрузит рейс в БД, но не откроет форму. Альтернатива — упростить `SharedRouteLinkHandler` и сразу пушить `routeId` в callback из Swift.

2. **`IosRouterImpl` реализует `com.z_company.domain.navigation.Router`.**
   Интерфейс остаётся в `domain/`, его используют features Android. На iOS реализация удаляется — это нормально, потому что в iOS-DI он не зарегистрирован, а Swift-навигацию делает SwiftUI/`AppCoordinator`. Никаких expect/actual у `Router` нет.

3. **Зависимости от Compose в `build.gradle.kts` модуля `iosApp`.**
   После удаления экранов и `App.kt` можно почистить gradle-зависимости `androidx.compose.*` и `org.jetbrains.androidx.navigation.*` в `iosApp/build.gradle.kts`. Это уменьшит размер `ComposeApp.framework`. Сделать в задаче 1.1.

4. **`HomeScreen.kt` — самый большой файл (369 строк).**
   Содержит готовую логику swipe-страниц статистики на Compose. Эта логика уже **переписана на SwiftUI** в `iosApp/iosApp/Screens/Home/HomeView.swift` (с тем же набором страниц `pageMainInfo`/`pageDetailWorkTime`/`pageDetailTrain`). Дублирование подтверждено — Compose-версия удаляется без потерь.

5. **Тестовое окружение.**
   Тестов в `iosApp/src/.../Test*` не найдено. Удаление не сломает ни один существующий тест.

---

## 0.3 — Миграция Room → SQLDelight

### TL;DR

✅ **Миграция реализована, данные существующих Android-пользователей сохраняются.** SQLDelight использует **те же имена БД-файлов**, что и Room (`Route.db`, `Settings.db`, `SalarySetting.db`), и в `data_local/src/androidMain/.../DatabaseDriverFactory.kt` есть полноценная логика **in-place миграции схемы**: пересоздание таблиц с несовместимой структурой, добавление недостающих столбцов, починка NULL-значений и принудительная установка версии БД. Дополнительной задачи на Android-релиз **не требуется** — миграция встроена в каждый запуск драйвера.

### 1. Что произошло с Room

| Категория | Результат |
|---|---|
| Импорты `androidx.room.*`, `@Database`, `@Entity`, `RoomDatabase` | ❌ Отсутствуют во всех `*.kt` под `data_local/`, `app/`, `features/`. |
| Подключение Room в `build.gradle.kts` модулей | ❌ Нет (`grep room_compiler/room_ktx/room_runtime` по всем `*.gradle.kts` пустой). |
| Декларация Room в `buildSrc/src/main/kotlin/Dependencies.kt` | 🟡 Осталась — `room_version = "2.7.1"`, константы `Versions.room_*` объявлены, но **нигде не используются**. Это мёртвый код в `Dependencies.kt`, можно удалить отдельной задачей (не блокер). |

**Вывод**: Room физически выкорчёван из кодовой базы — Gradle его не подтягивает, рантайм его не использует. `Dependencies.kt` нужно почистить, но это косметика.

### 2. Имена БД: Room vs SQLDelight

`data_local/src/androidMain/.../DatabaseDriverFactory.kt:18, 30, 50, 95, 100, 104` использует **те же имена файлов**, что Room:

| База | Имя файла | Назначение |
|---|---|---|
| RouteDatabase | `Route.db` | BasicData, Locomotive, Train, Passenger, Photo |
| SettingsDatabase | `Settings.db` | UserSettings, MonthOfYear, ReleaseDay, ProductionCalendarDay, RegionalHoliday |
| SalarySettingDatabase | `SalarySetting.db` | SalarySetting |
| SearchResponseDatabase | `SearchResponse.db` | История поиска |

**Это намеренно**: на устройстве пользователя физический SQLite-файл, созданный Room, **открывается** SQLDelight'ом. Не копирование, а **переиспользование**. Поэтому ключевой риск — не «потеря данных», а **несовместимость схем** между Room-версией и SQLDelight-версией.

### 3. Логика миграции

#### 3.1. `Route.db` — `migrateRouteDbIfNeeded()` (line 255–438)

Самая сложная миграция. Алгоритм:

1. **Детекция Room**: `hasColumn("Train", "remoteObjectId")` и `hasColumn("Locomotive", "removeObjectId")` (line 263–264). Если эти Room-специфичные столбцы есть — таблицы пересоздаются.
2. **Пересоздание `Train`** (line 266–308):
   - Сначала `ALTER TABLE Train ADD COLUMN` для 5 новых столбцов (`additionalNumbers`, `servicePhase`, `pusher`, `doubleTraction`, `doubledTrain`).
   - `CREATE TABLE Train_new` со схемой SQLDelight.
   - `INSERT INTO Train_new (...) SELECT (...) FROM Train` — копирует данные.
   - `DROP TABLE Train` → `ALTER TABLE Train_new RENAME TO Train`.
   - Создаёт индекс `index_Train_basicId`.
3. **Пересоздание `Locomotive`** (line 310–353): аналогично, добавляет `auxiliaryCounterAccepted/Delivery`, потом пересоздаёт.
4. **Создание отсутствующих таблиц** (line 357–406): `BasicData` и `Locomotive`, если они вообще не существуют (страховка на случай частичной Room-миграции, где какие-то таблицы ещё не были созданы).
5. **Дополнение столбцов** (line 410–430): для `BasicData`, `Locomotive`, `Train` — `ALTER TABLE ADD COLUMN` для всех новых полей с дефолтами из `COLUMN_SPECS`.
6. **Bump `db.version`** до `RouteDatabase.Schema.version` (line 432–434), чтобы SQLDelight потом не пытался прогонять `1.sqm…7.sqm` поверх уже мигрированной БД.

Также есть **`onDowngrade` no-op** (line 245) — при понижении версии (например, откат на старый APK) ничего не делается, лишние столбцы безвредны.

#### 3.2. `Settings.db` — `ensureSettingsTablesV6` + `fixVersionIfColumnsExist` (line 21–51)

1. `ensureSettingsTablesV6` (line 58–91): создаёт таблицы `ReleaseDay` и `ProductionCalendarDay` с индексами, если их нет (миграция 6 не сработала бы при `db.version=targetVersion`).
2. `fixVersionIfColumnsExist` (line 119–167): для 16 столбцов в `UserSettings` и 2 в `MonthOfYear` — `ALTER TABLE ADD COLUMN`, потом `UPDATE … SET column = default WHERE column IS NULL` (для NOT NULL Int — иначе `NullPointerException` при чтении из SQLDelight).
3. `db.version = targetVersion` — только если `primaryTable=UserSettings` существует. Иначе позволяет SQLDelight выполнить миграции `1.sqm…11.sqm` нормально.

11 миграций SettingsDatabase (`1.sqm…11.sqm`) — самая длинная история.

#### 3.3. `SalarySetting.db` (line 93–101)

Простая: `fixVersionIfColumnsExist` добавляет `nightTimePercent` (REAL DEFAULT 40.0) и `surchargeLongTrainsList` (TEXT DEFAULT `'[]'`).

#### 3.4. `SearchResponse.db` (line 103–104)

Без миграции — просто `createDriver`. Это новая БД, существовавшая ли в Room — не критично (история поиска пересоздаётся).

### 4. Доп. флаги миграции в SharedPreferences

`data_local/src/androidMain/.../SharedPreferenceStorage.kt` хранит 4 флага (line 29, 43–45):

| Токен | Что означает |
|---|---|
| `TOKEN_IS_MIGRATED` | Общий флаг «миграция Room→SQLDelight выполнена». |
| `TOKEN_TIMEZONE_MIGRATION` | Перевод временных меток в UTC (отдельная миграция данных, не схемы). |
| `TOKEN_RELEASE_DAY_MIGRATION` | Перенос «отвлечений» (отпуск/больничный/курсы) в новую таблицу `ReleaseDay`. |
| `TOKEN_PRODUCTION_CALENDAR_MIGRATION` | Перенос производственного календаря в `ProductionCalendarDay`. |

Эти флаги — **миграция данных** (не схемы), которую инициирует use case при старте, чтобы не делать одну и ту же работу повторно. На iOS-стороне `IosSharedPreferencesRepository.kt` все три миграционных флага возвращают `true` сразу (нет старых данных).

### 5. Оценка риска для существующих Android-пользователей

#### 🟢 Что хорошо
- Стратегия **«переиспользовать тот же файл + мигрировать схему»** — данные не теряются.
- Есть страховки от частичных миграций: создание `BasicData`/`Locomotive` через `IF NOT EXISTS`, проверка `hasTable` перед `ALTER`, проверка `hasColumn` перед `ADD COLUMN`.
- `onDowngrade` — no-op, откат на старый APK не падает.
- Идемпотентность: повторный запуск миграции не ломает ничего (`hasColumn` гарантирует пропуск уже существующих столбцов).

#### 🟡 Зоны внимания (не блокеры, но стоит держать в уме)

1. **`migrateRouteDbIfNeeded` пересоздаёт таблицы через `INSERT … SELECT`.**
   Если в продакшене обнаружится баг (опечатка в списке столбцов, потеря дефолта) — старая `Train`/`Locomotive` уже `DROP`'нута, откатиться нельзя. Сейчас список столбцов в `INSERT` корректен, но любой будущий рефакторинг этой функции должен покрываться тестами.

2. **`fixVersionIfColumnsExist` принудительно ставит `db.version = targetVersion`**, даже если SQLDelight-миграции `5.sqm…11.sqm` не были прогнаны. Это работает, потому что `COLUMN_SPECS` дублирует все добавления столбцов из `*.sqm`. Если в новой `*.sqm` появится столбец, который **не добавлен в `COLUMN_SPECS`** — пользователи Android получат `SQLiteException` (что и описано в комментарии line 22–27 для миграции 5).
   👉 **Соглашение для команды**: «при добавлении нового `ALTER TABLE` в `.sqm` — обязательно добавить такой же столбец в `COLUMN_SPECS`».

3. **Детекция Room по `remoteObjectId`/`removeObjectId`** — фрагильная. Если кто-то когда-то добавит столбец с таким именем в SQLDelight-схему, миграция запустится повторно на уже мигрированной БД. На текущей схеме конфликта нет, проверено (грепом по `*.sq` — таких столбцов нет).

4. **Никакого write-ahead логирования / транзакции вокруг миграций.** Если процесс убьют посреди миграции (kill-9, OOM-killer), БД может остаться в полу-мигрированном состоянии. Однако благодаря идемпотентности при следующем старте миграция доделается. Риск низкий.

#### 🔴 Реальных блокеров нет.

---

## 0.4 — Фоновая синхронизация на iOS

### TL;DR

🔴 **Фоновой синхронизации на iOS НЕТ.** Ни `BGTaskScheduler`, ни `BGAppRefreshTask`, ни даже стандартных триггеров «при старте» / «при foreground» / «при появлении сети». Sync вызывается **только** ручным нажатием кнопки в `ProfileView`. Это **блокер релиза**: машинист может неделями не открывать приложение → данные не уходят на сервер → потеря данных при потере/сбросе устройства.

### 1. Поиск API фоновых задач

| Что искал | Результат |
|---|---|
| `BGTaskScheduler` | ❌ 0 совпадений |
| `BGAppRefreshTask` | ❌ 0 совпадений |
| `BGProcessingTask` | ❌ 0 совпадений |
| `BackgroundTasks` (фреймворк) | ❌ 0 совпадений |
| `import BackgroundTasks` в Swift | ❌ 0 совпадений |

### 2. `Info.plist` — capabilities

`iosApp/iosApp/Info.plist` (60 строк) содержит:
- `LSRequiresIPhoneOS`, `UIApplicationSceneManifest`, `CFBundleURLTypes` (deep link `locodriver://`), `NSAppTransportSecurity` (разрешение HTTP), `UILaunchScreen`, `UISupportedInterfaceOrientations`.

**Чего нет:**
- ❌ `UIBackgroundModes` — без этого ключа iOS вообще не разрешит зарегистрировать `BGAppRefreshTask`/`BGProcessingTask`.
- ❌ `BGTaskSchedulerPermittedIdentifiers` — список идентификаторов фоновых задач, которые приложение хочет запускать.
- ❌ `*.entitlements`-файла нет вообще.

### 3. Где сейчас вызывается sync

| Точка вызова | Файл | Тип | Когда срабатывает |
|---|---|---|---|
| Кнопка «Синхронизировать данные» | `Profile/ProfileView.swift:151` (`vm.syncData()`) | UI, ручной | Пользователь нажимает в `ProfileView` |
| `firstSyncAfterRegistration` | `ProfileIosViewModel.kt:269` | Авто, разовый | **Один раз** после регистрации нового аккаунта |

**Чего нет** (грепом подтверждено):
- ❌ Sync при запуске приложения. `iOSApp.swift` (35 строк) делает только `IosKoinHelperKt.doInitKoin(...)` + `SharedRouteLinkHandler.shared.handle(urlString:)` для deep link. Никакого `viewModel.syncData()` на старте.
- ❌ Sync при возврате из background. `scenePhase`/`applicationDidBecomeActive` в SwiftUI-коде **не используются**.
- ❌ Sync при появлении сети. `NWPathMonitor` отсутствует.
- ❌ Sync по таймеру внутри приложения. `Timer.scheduledTimer`/`DispatchQueue.asyncAfter` не используются для sync.

### 4. Сравнение с Android

| | Android | iOS |
|---|---|---|
| Периодический фоновой sync | ✅ `WorkManager` + `SyncWorker`, **раз в 36 часов** + initial delay 6 часов, требует сети (`StartApp.kt:57–73`) | ❌ Отсутствует |
| Поведение «приложение долго не открывали» | Sync сам сработает при следующем сетевом доступе | Данные не уйдут до тех пор, пока пользователь сам не нажмёт кнопку |

### 5. Дополнительная находка

`IosSharedPreferencesRepository.kt:18` хранит `lastSyncTimestamp` **только в оперативной памяти** (`private var lastSyncTimestamp: Long = 0L`) — обнуляется при перезапуске приложения. То есть даже базовое отслеживание «когда последний раз синхронизировались» сейчас не работает на iOS. В комментарии рекомендуется заменить на `NSUserDefaults`. Это связанная задача (и блокер для умной логики «синхронизировать, если давно не синхронизировались»).

### 6. Что нужно сделать (Задача 1.4 в Этапе 1)

Минимально для устранения блокера:

1. **Триггер на foreground (дёшево, обязательно)**:
   `iOSApp.swift` → подписаться на `@Environment(\.scenePhase)`, при переходе в `.active` дёрнуть `vm.syncDataIfNeeded()` с throttle (не чаще раза в N минут).

2. **`BGAppRefreshTask` (важно)**:
   - В `Info.plist` добавить `UIBackgroundModes` → `fetch` + `processing` (если нужны длительные апсёрты).
   - Добавить `BGTaskSchedulerPermittedIdentifiers` со строкой типа `com.z_company.locodriver.sync`.
   - При старте приложения регистрировать handler: `BGTaskScheduler.shared.register(forTaskWithIdentifier:)`.
   - Внутри handler — короткий `syncManager.syncToRemote(...)` (Apple даёт ~30 секунд, нужно успеть).
   - При уходе в background — планировать следующую задачу: `BGTaskScheduler.shared.submit(BGAppRefreshTaskRequest)`.

3. **Персистентный `lastSyncTimestamp`** через `NSUserDefaults` (через `expect/actual` или прямо в `IosSharedPreferencesRepository`).

4. **Опционально — мониторинг сети** через `NWPathMonitor`: если sync был отложен из-за отсутствия сети, дотолкать при появлении.

⚠️ **Важно про Apple ограничения**: `BGAppRefreshTask` не гарантирует время запуска — Apple запускает «когда сочтёт нужным» (по эвристикам поведения пользователя, обычно раз в сутки). Поэтому p.1 (foreground-триггер) обязателен — он закрывает 90% реальных кейсов.

---

## 0.5 — Готовность серверного API к Sign in with Apple

### TL;DR

🔴 **На сервере Sign in with Apple НЕ реализован.** Ноль упоминаний `apple`/`siwa`/`appleId`/`apple_id` в коде Python (только CSS-шрифты `-apple-system` в HTML-шаблонах, не относится к делу). Чтобы клиент iOS смог обменивать Apple identity token на JWT, серверу нужны: новое значение enum `methodAuth="appleId"`, верификация Apple JWT через JWKS, поле `apple_id` в таблице `user`, Alembic-миграция и расширение нескольких эндпоинтов. Это **отдельная серверная задача** (репо `/Users/zoer/Downloads/proxy-parser/`), которую надо выполнить **до Этапа 4** клиентского TODO.

### 1. Что сейчас поддерживает сервер

#### `backend/src/schemas/request.py:6–16` — `UserCredentials`
```python
class UserCredentials(BaseModel):
    auth_param: str
    password: str
    methodAuth: Literal["login", "email", "vkId"]
```
Только три метода: `login`, `email`, `vkId`. **Нет `appleId`.**

#### `backend/src/schemas/request.py:19–30` — `UserRegister`
```python
class UserRegister(BaseModel):
    login: str
    password: str
    email: str
    vkId: Optional[str] = None
```
Регистрация принимает `login`+`email`+`password` (+опционально `vkId`). Нет `appleId`.

#### `backend/src/models/users.py:21–26` — `User`
```python
class User(BaseModel, table=True):
    login: str = Field(...)
    email: str = Field(...)
    vk_id: str = Field(unique=True, index=True, nullable=True)
    hashed_password: str
    linked: bool
```
Поля **`apple_id` нет**. Структура аналогична `vk_id` — оно тоже nullable+unique, для пользователей без VK.

#### `backend/src/api/v1/auth.py` (461 строка) — эндпоинты

| Метод | Путь | Что делает |
|---|---|---|
| `POST` | `/v1/auth/` (login_for_access_token, line 31–102) | Принимает `UserCredentials`, делегирует в `authenticate_user(auth_param, password, methodAuth)` — то есть метод верификации **зашит в `methodAuth`**, новый enum-вариант надо добавлять туда. |
| `GET` | `/v1/auth/` (get_user, line 105–157) | Профиль текущего пользователя по JWT. |
| `POST` | `/v1/auth/create` (create_user, line 160–214) | Регистрация по `UserRegister`. |
| `PATCH` | `/v1/auth/vkId/remove` (remove_vkId, line 216) | Открепить VK от аккаунта. |
| `PATCH` | `/v1/auth/vkId/add` (add_vkId, line 273–330) | Привязать VK к существующему аккаунту. |
| `PATCH` | `/v1/auth/email/add` (add_email, line 332) | Добавить email к VK-only-аккаунту. |
| `PATCH` | `/v1/auth/email/update` (update_email, line 404) | Сменить email. |

**Шаблон** `add_vkId`/`remove_vkId` — это и есть готовая модель для `add_appleId`/`remove_appleId`.

#### `backend/src/schemas/request.py:43–48` — `UserSafeResponse`
```python
class UserSafeResponse(BaseModel):
    id: uuid_pkg.UUID
    email: str
    vk_id: str
```
Возвращается клиенту в `GET /v1/auth/`. **Не содержит `apple_id`** — клиент iOS не сможет узнать, привязан ли Apple ID к его аккаунту.

### 2. Что нужно добавить на сервере (полный список)

#### 2.1. Pydantic-схемы (`backend/src/schemas/request.py`)
- `UserCredentials.methodAuth`: расширить `Literal["login", "email", "vkId"]` → `Literal["login", "email", "vkId", "appleId"]`.
- `UserRegister`: добавить `appleId: Optional[str] = None` (по аналогии с `vkId`). Альтернатива — не давать регистрироваться через Apple через старый эндпоинт, а сделать отдельный `POST /v1/auth/apple/login` (см. ниже).
- `UserSafeResponse`: добавить `apple_id: Optional[str] = None`. Это дополнение, не сломает старых клиентов.
- Новая схема `appleTokenAdd` (по аналогии с `tokenAdd` line 73) — принимает Apple identity token (JWT).

#### 2.2. БД и модель (`backend/src/models/users.py`)
- В `User`: добавить
  ```python
  apple_id: Optional[str] = Field(max_length=100, unique=True, index=True, nullable=True)
  ```
- **Alembic-миграция**: новый файл в `backend/src/migrations/versions/` с `op.add_column("user", sa.Column("apple_id", sa.String(100), nullable=True))` + `op.create_index("ix_user_apple_id", "user", ["apple_id"], unique=True)`.

#### 2.3. Верификация Apple identity token

Apple возвращает identity token — это JWT, подписанный одной из публичных ключей Apple, доступных через JWKS. Серверу нужно:

1. **Загружать JWKS** с `https://appleid.apple.com/auth/keys` (с кэшированием, т.к. ключи меняются редко).
2. **Декодировать JWT** через `python-jose` (или `pyjwt[crypto]` — обе либы умеют JWKS):
   ```python
   from jose import jwt
   header = jwt.get_unverified_header(id_token)
   key = next(k for k in jwks["keys"] if k["kid"] == header["kid"])
   payload = jwt.decode(id_token, key, algorithms=["RS256"], audience=APP_BUNDLE_ID, issuer="https://appleid.apple.com")
   ```
3. **Проверки**:
   - `iss == "https://appleid.apple.com"`
   - `aud == APP_BUNDLE_ID` (например `com.z_company.locodriver`)
   - `exp > now()`
   - `nonce` совпадает с тем, что отправил клиент (защита от replay)
4. Из payload берём `sub` — это и есть **Apple user ID** (стабильный, постоянный, уникальный для одной пары developer-team + user). Сохраняем в `user.apple_id`.

⚠️ **Ключевые ограничения Apple**:
- Email-адрес (`payload["email"]`) Apple отдаёт **только при первом входе**. При повторных логинах email будет отсутствовать в payload — поэтому при первом успешном входе обязательно сохранять email в БД.
- `payload["email"]` может быть **proxy-адресом** вида `xxxx@privaterelay.appleid.com`, если пользователь выбрал «Hide My Email». Это нормальный валидный email — сервер должен принимать.
- `payload["email_verified"]` — может быть строкой `"true"` (а не bool), это особенность Apple JWT.

#### 2.4. Эндпоинты

Минимальный набор:

| Эндпоинт | Назначение |
|---|---|
| Расширение `POST /v1/auth/` через `methodAuth="appleId"` | `auth_param` = Apple identity token (JWT), `password` = пустая строка или `nonce`. После верификации возвращает JWT. |
| `PATCH /v1/auth/appleId/add` | Привязать Apple ID к существующему аккаунту (как `add_vkId`). |
| `PATCH /v1/auth/appleId/remove` | Отвязать Apple ID (нужно по требованию Apple — пользователь должен иметь возможность отозвать связку). |

Альтернатива: добавить отдельный `POST /v1/auth/apple/login` — он чище логически, но требует двух точек входа (старый `/v1/auth/` плюс новый).

#### 2.5. Логика `authenticate_user` в сервисе (`backend/src/services/auth.py`)
Добавить ветку для `methodAuth="appleId"`:
1. Парсим `auth_param` как Apple identity token.
2. Верифицируем (см. 2.3).
3. Ищем `User` по `apple_id == payload["sub"]`.
4. Если есть — выдаём JWT.
5. Если нет — создаём пользователя на лету (если payload содержит `email`) **или** возвращаем 404, и клиент сам делает регистрацию через отдельный flow.

#### 2.6. Соответствие политикам Apple App Store

⚠️ **Важно**: Apple Review Guidelines §4.8 требует, что **если приложение поддерживает сторонний social login (VK, Google, Facebook), оно ОБЯЗАНО также предоставлять Sign in with Apple**. У нас есть VK ID → SIWA становится **обязательным условием апрува** в App Store. Без него ревью отклонит приложение.

#### 2.7. Тесты

- В `backend/tests/` нет тестов аутентификации сейчас (есть `test_security.py`, но это про другое). При добавлении SIWA — нужны тесты на:
  - Валидный Apple JWT → выдача access token.
  - Невалидная подпись → 401.
  - Просроченный JWT → 401.
  - Неправильный `aud` → 401.

#### 2.8. Зависимости
- `python-jose[cryptography]` или `pyjwt[crypto]` — нужны (проверить, есть ли уже в `pyproject.toml`/`requirements.txt`).
- `httpx` — для загрузки JWKS (вероятно уже есть, т.к. сервер использует FastAPI).

### 3. Сводная карта файлов сервера к изменению

| Файл | Что менять |
|---|---|
| `backend/src/schemas/request.py` | `UserCredentials.methodAuth` → добавить `appleId`. Новая схема `appleTokenAdd`. `UserSafeResponse` → `apple_id`. |
| `backend/src/models/users.py` | Поле `apple_id` в `User`. |
| `backend/src/migrations/versions/<новый>.py` | Alembic: добавить колонку `apple_id` + индекс. |
| `backend/src/services/auth.py` | Функция `verify_apple_id_token(token, nonce)`. Расширить `authenticate_user` для `methodAuth="appleId"`. |
| `backend/src/api/v1/auth.py` | Расширить docstring `login_for_access_token`. Добавить эндпоинты `PATCH /vkId-style` для `appleId/add` и `appleId/remove`. |
| `backend/src/core/config.py` | Конфиг: `APPLE_BUNDLE_ID`, `APPLE_TEAM_ID`, `APPLE_JWKS_URL`. |
| `pyproject.toml` / `requirements.txt` | `python-jose[cryptography]` или `pyjwt[crypto]`, если ещё не установлены. |
| `backend/tests/tests_services/` | Тесты SIWA. |

---

## Итоги Этапа 0

| | Результат |
|---|---|
| **Готово** | 5/5 задач Discovery, ясна структура iOS-проекта, состояние CMP-мусора, миграция Room→SQLDelight в порядке. |
| **Найдено блокеров релиза** | **2**: фоновая sync iOS (Задача 1.4) и SIWA на сервере (отдельная задача в `proxy-parser/`). |
| **Уточнённый scope клиента** | Нужны **3 новых Kotlin-VM** (WorkSchedule/Search/Purchases) + доделка `FormLoco`/`FormTrain`/`Profile`/`SalaryCalculation`. |
| **Косметика** | Удалить мёртвые Room-константы из `buildSrc/Dependencies.kt`. |

### Цепочка зависимостей задач

```
0.1 Аудит iOS ──┬──► 1.x Доделка экранов (Этап 2-3)
                │
0.2 Аудит CMP ──┴──► 1.1 Удаление мёртвого кода
                     (с учётом замечания о SharedRouteLinkHandler)

0.3 Миграция БД ────► (нет дальнейшей работы, всё в порядке)

0.4 Фоновая sync ───► 1.4 BGTaskScheduler (НОВЫЙ блокер)

0.5 SIWA сервер ────► [серверная задача в proxy-parser/]
                      ────► 4.x Интеграция SIWA в iOS (Этап 4)
```

### Ссылки

- `60_IOS_TODO.md` — все задачи Этапа 0 помечены `✅ ВЫПОЛНЕНО` с краткими резюме в Definition of Done.
- `CLAUDE.md` — правила работы с проектом.
- `CODEBASE.md` — карта кодовой базы.
- `SECURITY_ACTION_PLAN.md` — связанные задачи по безопасности сервера.
