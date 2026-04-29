# 60_IOS_TODO.md — План релиза iOS-версии

> Список задач на 2 недели разработки + время на TestFlight beta и
> App Store Review. Работа через Claude Code, по одной задаче за раз.
>
> Каждая задача — готовый промпт, который можно скопировать в Claude Code.
> В конце каждой задачи — критерии готовности (Definition of Done).

---

## Как работать с этим документом

1. Открой Claude Code в корне проекта: `cd /Users/zoer/AndroidStudioProjects/LocoDriver && claude`
2. Дай ему **одну задачу за раз** (не весь TODO):
   ```
   Прочитай CLAUDE.md и CODEBASE.md. Работаем по 60_IOS_TODO.md, задача N.M.
   Покажи план и подожди подтверждения, прежде чем начать.
   ```
3. Проверь план, дай зелёный свет («поехали»).
4. Когда задача выполнена — пройдись по чек-листу Definition of Done.
5. **Коммит**: `git add -A && git commit -m "ios: <короткое описание>"`.
6. Следующая задача.

⚠️ **Не разрешай Claude Code править серверный код** — он в другом репозитории
(`/Users/zoer/Downloads/proxy-parser/`). Если задача требует серверной правки —
останови, обсудим отдельно.

⚠️ **Параллельно с задачами 0.x — оплати Apple Developer Program** (если ещё
не оплачено). Подтверждение занимает 1-3 дня. Без него нельзя ни TestFlight,
ни App Store. https://developer.apple.com/programs/

---

## ЭТАП 0 — Discovery (день 1, ~2 часа)

Прежде чем что-то делать, нужно понять реальное состояние проекта в
4-х аспектах.

### Задача 0.1 — Аудит iOS-проекта

**Цель**: получить точную картину готовности iOS.

**Промпт для Claude Code**:

```
Сделай аудит iOS-проекта и составь отчёт в формате таблицы.

1. Проверь каждый Swift-файл в iosApp/iosApp/Screens/ — определи статус:
   - 🟢 готов: реальная функциональность, реальные данные через ViewModel
   - 🟡 частично: верстка есть, но что-то заглушено или работает не до конца
   - 🔴 заглушка: только placeholder

2. Для каждого ViewModelWrapper в iosApp/iosApp/ViewModels/ найди
   соответствующий Kotlin-VM (поиск файлов *IosViewModel.kt в репозитории):
   - Wrapper использует реальный Kotlin-VM из shared
   - Wrapper использует mock/заглушку

3. Составь таблицу:

| Экран | SwiftUI статус | Wrapper | Kotlin-VM | Замечания |
|---|---|---|---|---|

4. В конце укажи:
   - Список Wrapper'ов БЕЗ Kotlin-VM (значит, нужно создать)
   - Список Kotlin-VM в shared без подключённого Wrapper'а
   - Список экранов, которые требуют доработки в первую очередь

НЕ меняй код. Только отчёт.
```

**Definition of Done**:
- [ ] Получен отчёт с таблицей
- [ ] Ясен список «что нужно сделать в первую очередь»
- [ ] Записал результаты в начало `60_IOS_TODO.md` или отдельный файл
      `IOS_STATE_<дата>.md`

---

### Задача 0.2 — Аудит мёртвого CMP-кода

**Цель**: понять, что в `iosApp/src/commonMain/` и `iosApp/src/iosMain/`
реально используется, а что мёртвое.

**Промпт для Claude Code**:

```
В iosApp/src/commonMain/ и iosApp/src/iosMain/ есть остатки от попытки
сделать iOS UI на Compose Multiplatform. Сейчас UI делается на нативном
SwiftUI. Нужно понять, что оттуда удалить.

1. Найди все файлы в iosApp/src/commonMain/ и iosApp/src/iosMain/.
2. Для каждого файла определи:
   - Используется ли он Swift-частью (iosApp/iosApp/) — ищи в Swift-коде
     импорты ComposeApp и упоминания классов из этих файлов.
   - Используется ли в DI (через Koin-модули в iosApp/src/commonMain/.../di/).
   - Это часть Compose-UI инфраструктуры (MainViewController, AppNavHost,
     IosRouterImpl, Routes, Compose-экраны) — это к удалению.

3. Составь два списка:
   - 🟢 КЕЕП: что трогать нельзя (нужно для работы Swift-приложения)
   - 🚮 УДАЛИТЬ: мёртвый CMP-код, который точно не используется

4. Особо проверь:
   - IosUseCaseModule.kt — должен быть в КЕЕП (это DI iOS)
   - IosKoinHelper.kt — должен быть в КЕЕП
   - IosViewModelHelper.kt — должен быть в КЕЕП
   - MainViewController.kt — к удалению
   - AppNavHost.kt — к удалению
   - IosRouterImpl.kt — к удалению (если SwiftUI использует AppCoordinator)
   - Routes.kt — к удалению

НЕ удаляй файлы сам. Только отчёт.
```

**Definition of Done**:
- [ ] Получен список «что удалять»
- [ ] Подтверждено, что DI-файлы в КЕЕП

---

### Задача 0.3 — Проверка миграции Room → SQLDelight

**Цель**: понять, что произошло с данными существующих Android-пользователей.

**Промпт для Claude Code**:

```
В data_local/ — переход с Room на SQLDelight. Нужно понять, есть ли логика
миграции данных существующих Android-пользователей.

1. Найди в data_local/ файлы со словом "Migration" или "migrate".
2. Проверь, есть ли в androidMain копирование данных из старой Room-БД в
   новую SQLDelight-БД при первом запуске обновлённого приложения.
3. Если миграции нет — оцени риск: при обновлении Android-приложения
   пользователи теряют локальные данные и должны делать ресинк с сервера.
4. Проверь имена БД:
   - Старая Room: какое имя?
   - Новая SQLDelight: какое имя?
   Если имена разные — старая БД остаётся на устройстве (не используется),
   новая создаётся пустой. Если имена одинаковые — старая Room-БД будет
   распознана SQLDelight как несовместимая, что приведёт к падению или
   пересозданию.

Сделай только отчёт, не меняй код.
```

**Definition of Done**:
- [ ] Понятно, что происходит с данными существующих Android-пользователей
- [ ] Если есть риск — фиксируем как отдельную задачу для Android-релиза

---

### Задача 0.4 — Проверка фоновой синхронизации на iOS

**Цель**: понять, реализована ли фоновая sync на iOS (BGTaskScheduler).

**Промпт для Claude Code**:

```
На Android фоновая синхронизация работает через WorkManager (каждые 36 часов).
На iOS аналог — BGTaskScheduler из BackgroundTasks framework.

1. Поищи в iosApp/ упоминания:
   - BGTaskScheduler
   - BGAppRefreshTask
   - BackgroundTasks
   - "background" / "sync" в Info.plist (capabilities)

2. Если есть — опиши, как настроено.
3. Если нет — это блокер для релиза. Машинист может неделями забыть открыть
   приложение, и его данные не синхронизируются с сервером.

4. Также проверь, как сейчас вызывается sync на iOS (только через UI?
   при запуске приложения? при появлении сети?).

Сделай только отчёт.
```

**Definition of Done**:
- [ ] Ясно, есть ли фоновая sync на iOS
- [ ] Если нет — добавлена задача в Этап 1 (BGTaskScheduler нужно настроить
      ДО релиза)

---

## ЭТАП 1 — Чистка и базовая инфраструктура (дни 2-3)

### Задача 1.1 — Удаление мёртвого CMP-кода

**Цель**: убрать `iosApp/src/commonMain/` и `iosApp/src/iosMain/` (кроме
DI-файлов).

**Промпт для Claude Code**:

```
По результатам Задачи 0.2 у нас есть список файлов к удалению. Также есть
список КЕЕП. Сейчас:

1. Перенеси КЕЕП-файлы (IosUseCaseModule.kt, IosKoinHelper.kt,
   IosViewModelHelper.kt и любые другие нужные) в новую структуру:
   - iosApp/src/iosMain/kotlin/com/z_company/iosapp/di/

2. Удали всё остальное в iosApp/src/commonMain/ и iosApp/src/iosMain/.

3. Обнови build.gradle.kts модуля iosApp:
   - Убери зависимости от Compose Multiplatform
   - Убери зависимости от KMP Navigation Compose
   - Оставь только то, что нужно для DI и интеграции с shared-модулями

4. Соберай iOS-приложение через Xcode и убедись, что оно работает.

Покажи план, что удаляешь и что переносишь, прежде чем что-то делать.
После применения — закоммить.
```

**Definition of Done**:
- [ ] Папки `iosApp/src/commonMain/` и `iosApp/src/iosMain/` содержат
      только нужные DI-файлы
- [ ] iOS-приложение собирается через Xcode без ошибок
- [ ] Существующая функциональность (главный, форма, авторизация, sync)
      продолжает работать на симуляторе
- [ ] `git commit -m "ios: cleanup dead Compose Multiplatform code"`

---

### Задача 1.2 — Настройка Memory Leak Fix в ViewModelWrapper

**Цель**: устранить leak подписок `watchX(callback)` при деинициализации
Wrapper.

**Контекст**: сейчас Wrapper'ы (`HomeViewModelWrapper`, `FormViewModelWrapper`,
и др.) при инициализации вызывают `viewModel.watchRoutes { ... }`,
`viewModel.watchSettings { ... }` и т.д. Эти callbacks никогда не
отписываются. Если Kotlin-VM держится в Koin как singleton (что вероятно
для большинства), при закрытии экрана Wrapper уйдёт, но callback продолжит
работать в памяти.

**Промпт для Claude Code**:

```
В iosApp/iosApp/ViewModels/ есть несколько ViewModelWrapper-классов
(HomeViewModelWrapper, FormViewModelWrapper, SettingsViewModelWrapper и
другие). Каждый из них в init { ... } подписывается на Kotlin-VM через
viewModel.watchX(callback). Эти подписки не отписываются в deinit.

Задача — исправить memory leak. Варианты:

ВАРИАНТ A (если Kotlin-VM возвращает Closeable-токен подписки):
В Kotlin-VM каждый watchX(callback) возвращает Closeable/Job, который
позволяет отписаться. В Wrapper храним токены, в deinit вызываем
.close() или .cancel().

ВАРИАНТ B (если Kotlin-VM сейчас этого не умеет):
Изменить watchX(callback) в Kotlin-VM, чтобы возвращал DisposableHandle
или подобное. Wrapper хранит handles, отписывается в deinit.

ВАРИАНТ C (через Combine/AsyncSequence):
Переделать Wrapper'ы на Combine — Kotlin-VM выставляет StateFlow, Swift
конвертирует в Publisher. Сложнее, но идиоматично.

Шаги:
1. Изучи 1-2 Wrapper'а и соответствующие Kotlin-VM, выбери вариант
   (предпочтительнее A или B — без переписывания всего паттерна).
2. Реализуй на одном Wrapper'е (например HomeViewModelWrapper).
3. Проверь, что всё работает (открыл экран, посмотрел данные, закрыл —
   нет утечек).
4. Распространи на остальные Wrapper'ы.

Покажи план перед началом.
```

**Definition of Done**:
- [ ] В deinit Wrapper'ов отписываются от подписок
- [ ] Тест: открыть экран, закрыть, открыть снова — нет дублирования
      обновлений
- [ ] Все 8 Wrapper'ов обновлены
- [ ] `git commit -m "ios: fix memory leaks in ViewModelWrapper subscriptions"`

---

### Задача 1.3 — Настройка тестового окружения

**Цель**: убедиться, что тесты в commonTest и UI-тесты iOS можно запускать.

**Промпт для Claude Code**:

```
Проверь и настрой тестовое окружение для iOS-разработки:

1. КОТЛИН-ТЕСТЫ В commonTest:
   - Найди существующие commonTest в domain/, core/, data_local/, data_remote/
   - Запусти ./gradlew :domain:allTests или :domain:iosSimulatorArm64Test
   - Если ошибки — почини их (могут быть из-за обновления Kotlin)

2. iOS XCTest:
   - В Xcode проверь, есть ли target "iosAppTests" или подобный
   - Если нет — создай через File → New → Target → Unit Testing Bundle
   - Создай простой smoke-тест и запусти Cmd+U

3. iOS UI-тесты (XCUITest):
   - Проверь, есть ли target "iosAppUITests"
   - Если нет — создай через File → New → Target → UI Testing Bundle
   - Создай smoke-тест: запустился, главный экран отрисовался

4. Документируй, как запускать тесты:
   - В CODEBASE.md добавь раздел "Запуск тестов" с командами
   - Для commonTest: ./gradlew :module:test
   - Для iOS Unit: Cmd+U в Xcode
   - Для iOS UI: Cmd+U с выбранным UI-тест target'ом

Покажи план перед началом.
```

**Definition of Done**:
- [ ] commonTest запускаются командой gradle
- [ ] iOS XCTest target существует и пустой smoke-тест проходит
- [ ] iOS XCUITest target существует и пустой smoke-тест проходит
- [ ] В CODEBASE.md задокументировано, как запускать тесты
- [ ] `git commit -m "test: setup testing infrastructure for iOS"`

---

## ЭТАП 2 — Доделать частичные экраны (дни 4-7)

Каждая задача в этом этапе — **доделать конкретный экран** с тремя слоями
(Kotlin-VM в shared → Wrapper в Swift → SwiftUI экран) + тестами.

### Задача 2.1 — FormLocoView (форма локомотива)

**Контекст**: SwiftUI-экран существует частично, нужно доделать. Локомотив
имеет тип (электровоз/тепловоз), серию, номер, время приёмки/сдачи, нормы
расхода, и список секций (электрические или дизельные в зависимости от
типа).

**Промпт для Claude Code**:

```
Доделай экран FormLocoView (форма локомотива).

Контекст:
- iosApp/iosApp/Screens/Form/FormLocoView.swift существует частично
- iosApp/iosApp/ViewModels/LocoFormViewModelWrapper.swift есть
- Нужно проверить наличие LocoFormIosViewModel в shared (в iosApp/src/iosMain/)

Доменная модель Locomotive (см. domain/src/commonMain/.../entities/route/
Locomotive.kt):
  - locoId: String (UUID, генерируется клиентом)
  - basicId: String (UUID родительского рейса)
  - series: String? (серия)
  - number: String? (номер)
  - type: LocoType (ELECTRIC | DIESEL)
  - timeStartOfAcceptance: Long? (ms)
  - timeEndOfAcceptance: Long?
  - timeStartOfDelivery: Long?
  - timeEndOfDelivery: Long?
  - normaElectricCurrent1: Double?
  - normaElectricCurrent2: Double?
  - normaDiesel: Int?
  - heatingCounterAccepted: Double?
  - heatingCounterDelivery: Double?
  - auxiliaryCounterAccepted: Double?
  - auxiliaryCounterDelivery: Double?
  - electricSectionList: [SectionElectric]
  - dieselSectionList: [SectionDiesel]

Шаги:
1. Если LocoFormIosViewModel в shared нет — создай (по образцу
   HomeIosViewModel и FormIosViewModel). Поля состояния:
   - locomotive: StateFlow<Locomotive?>
   - isLoading: StateFlow<Boolean>
   - errorMessage: StateFlow<String?>
   - isSaved: StateFlow<Boolean>
   Методы:
   - loadLocomotive(routeId, locoId: String?) — null = новый
   - updateField(name, value)
   - addElectricSection() / addDieselSection()
   - removeSection(sectionId)
   - saveLocomotive()
   - watchX(callback) для каждого поля состояния

2. Подключи LocoFormIosViewModel в iosUseCaseModule (DI) и
   IosViewModelHelper (singleton-фасад).

3. Обнови LocoFormViewModelWrapper.swift — подпишись на состояние Kotlin-VM
   через watchX, экспонируй @Published свойства, добавь cleanup в deinit
   (по образцу из Задачи 1.2).

4. Доделай FormLocoView.swift:
   - Form section "Тип локомотива" — Picker (Электровоз / Тепловоз)
   - Form section "Серия и номер" — два TextField
   - Form section "Приёмка" — DatePicker для timeStartOfAcceptance и
     timeEndOfAcceptance
   - Form section "Сдача" — аналогично
   - Form section "Нормы" — поля норм в зависимости от типа
   - Form section "Секции" — список SectionElectric или SectionDiesel
     с возможностью добавления/удаления (NavigationLink на отдельный экран
     для редактирования секции, или сразу inline)
   - Кнопка "Сохранить" в navigationBar

5. Тесты:
   - В commonTest добавь LocoFormIosViewModelTest:
     * Создание нового локомотива → loadLocomotive(routeId, null) →
       состояние с пустыми полями
     * Загрузка существующего → loadLocomotive(routeId, "uuid") → состояние
       с данными из БД
     * Сохранение валидного → saveLocomotive() → isSaved = true
     * Сохранение с пустой серией → ошибка валидации
   - SwiftUI Preview для FormLocoView с моковыми данными:
     * Loading state
     * Новый локомотив (пустая форма)
     * Существующий электровоз с 3 секциями
     * Существующий тепловоз с 2 секциями
   - Не трогай UI-тесты XCUITest — это сделаем в Этапе 5 общим прогоном.

Покажи план перед началом. Если что-то неясно — спроси.
```

**Definition of Done**:
- [ ] LocoFormIosViewModel реализован с тестами
- [ ] Wrapper подписан, нет утечек памяти (cleanup в deinit)
- [ ] FormLocoView полностью функционален: загрузка, редактирование,
      сохранение
- [ ] SwiftUI Preview с 4 состояниями
- [ ] Юнит-тесты проходят (`./gradlew :domain:allTests` или модуль с VM)
- [ ] Ручная проверка на симуляторе:
  - [ ] Открыл рейс, нажал «+ Добавить локомотив», заполнил, сохранил → виден в списке
  - [ ] Открыл существующий локомотив, изменил поле, сохранил → изменения
        сохранены
  - [ ] Создал секцию, удалил секцию → корректно
- [ ] `git commit -m "ios: complete FormLocoView with sections"`

---

### Задача 2.2 — FormTrainView (форма поезда)

**Контекст**: поезд имеет номер, вес, оси, длину, расстояние, флаг
тяжеловесного, дополнительные номера, фазу обслуживания, толкача, двойную
тягу, соединённый поезд, и список станций.

**Промпт для Claude Code**:

```
Доделай экран FormTrainView (форма поезда). Структура работы аналогична
Задаче 2.1.

Доменная модель Train (см. domain/src/commonMain/.../entities/route/Train.kt):
  - trainId, basicId
  - number: String?
  - weight: String? (СТРОКА, не Int — наследие)
  - axle: String?
  - conditionalLength: String?
  - distance: String?
  - additionalNumbers: List<String>
  - servicePhase: ServicePhase? (с departureStation/arrivalStation/distance)
  - pusher: ? (объект толкача)
  - doubleTraction: ? (объект двойной тяги)
  - doubledTrain: ? (объект соединённого поезда)
  - isHeavyLongDistance: Boolean (⚠️ известная грабля: всегда теряется при sync,
    см. CLAUDE.md)
  - stations: List<Station>

⚠️ ВАЖНО: weight, axle, conditionalLength, distance — это СТРОКИ. Не
конвертируй их в числа на клиенте — сервер ожидает строки.

⚠️ ВАЖНО: track_number в Station — snake_case в JSON. На клиенте поле
trackNumber с @SerialName("track_number").

Шаги:
1. Создать TrainFormIosViewModel (если ещё нет) — по образцу LocoForm.
2. Обновить TrainFormViewModelWrapper.
3. Доделать FormTrainView с секциями:
   - Основные данные (номер, дополнительные номера)
   - Параметры (вес, оси, длина, расстояние) — TextField с keyboardType .numberPad,
     но значения хранятся как String
   - Чекбокс "Тяжеловесный/длинносоставный"
   - Фаза обслуживания — выбор из списка пользовательских фаз
   - Толкач, двойная тяга, соединённый поезд — складные секции с подформами
   - Список станций — отдельный экран на каждую станцию (NavigationLink),
     с timeArrival, timeDeparture, trackNumber

4. Тесты:
   - commonTest: TrainFormIosViewModelTest (валидация, сохранение)
   - SwiftUI Preview: пустой поезд, поезд с 5 станциями, тяжеловесный поезд
   - Ручная проверка через симулятор

Покажи план перед началом.
```

**Definition of Done**:
- [ ] TrainFormIosViewModel + Wrapper + UI работают
- [ ] Поля weight/axle/conditionalLength/distance остаются строками в DTO
- [ ] track_number сериализуется правильно
- [ ] Юнит-тесты, Preview, ручная проверка
- [ ] `git commit -m "ios: complete FormTrainView with stations and service phase"`

---

### Задача 2.3 — FormPassengerView (форма пассажирской поездки)

**Контекст**: упрощённая форма — машинист едет пассажиром (например, к
месту приёма локомотива).

**Промпт для Claude Code**:

```
Доделай FormPassengerView. Это самая простая форма из всех.

Доменная модель Passenger:
  - passengerId, basicId
  - trainNumber: String?
  - stationDeparture: String?
  - stationArrival: String?
  - timeArrival: Long? (ms)
  - timeDeparture: Long?
  - notes: String?

По образцу Задачи 2.1: проверь Kotlin-VM, Wrapper, доделай UI, добавь тесты.

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Все три слоя работают
- [ ] Юнит-тесты + Preview + ручная проверка
- [ ] `git commit -m "ios: complete FormPassengerView"`

---

### Задача 2.4 — SettingsView (полные настройки)

**Контекст**: сейчас Settings частично работает. Нужно доделать все секции
из UserSettings.

**Промпт для Claude Code**:

```
Доделай экран SettingsView (пользовательские настройки).

Доменная модель UserSettings (см. domain/.../entities/setting/UserSettings.kt):
  - minTimeRestPointOfTurnover: Long (миллисекунды)
  - minTimeHomeRest: Long
  - lastEnteredDieselCoefficient: Double
  - nightTime: NightTime (startNightHour, startNightMinute, endNightHour, endNightMinute)
  - defaultLocoType: String ("ELECTRIC" | "DIESEL")
  - defaultWorkTime: Long
  - usingDefaultWorkTime: Boolean
  - isConsiderFutureRoute: Boolean
  - stationList: List<String> (история введённых станций)
  - locomotiveSeriesList: List<String> (история серий)
  - timeZone: Int (смещение от Москвы в часах)
  - country: String ("RU"/"KZ"/"BY")
  - servicePhases: List<ServicePhase>
  - standardTimesStartWork: List<Long> (стандартные времена начала работы)
  - subscriptionPeriod: Long
  - isDecimalTime: Boolean (показывать время в десятичном формате)
  - isShowBreak: Boolean
  - crossMonthTimezone: String ("LOCAL" по умолчанию)

Шаги:
1. Проверь SettingsIosViewModel — должен загружать настройки и сохранять
   изменения через SettingsUseCase.
2. Wrapper по образцу.
3. SettingsView секции:
   - "Время работы" (минимальное время отдыха, стандартные времена)
   - "Ночное время" (часы начала/окончания)
   - "Локомотив по умолчанию" (Picker)
   - "Часовой пояс" (Picker по UTC offset от Москвы)
   - "Плечи обслуживания" (список + добавить/удалить)
   - "История станций" (показать, очистить)
   - "Серии локомотивов" (показать, очистить)
   - "Отображение" (десятичное время, показывать перерыв)

4. Тесты:
   - SettingsIosViewModelTest: загрузка, изменение, сохранение
   - SwiftUI Preview: дефолтные настройки, заполненные настройки
   - Ручная проверка

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Все секции работают
- [ ] Изменения сохраняются и видны при перезапуске приложения
- [ ] Юнит-тесты, Preview, ручная проверка
- [ ] `git commit -m "ios: complete SettingsView with all sections"`

---

### Задача 2.5 — ProfileView (профиль)

**Контекст**: показ информации о пользователе, выход из аккаунта, привязка
email/vkId.

**Промпт для Claude Code**:

```
Доделай ProfileView. Используется для:
- Показа email и vk_id пользователя
- Кнопки "Выйти из аккаунта" (logout)
- Привязка/отвязка vkId
- Изменение пароля
- Удаление аккаунта (если поддерживается на сервере)

Контракт API (см. CLAUDE.md и 31_API_REFERENCE.md):
- GET /v1/auth — профиль
- PATCH /v1/auth/email/update — изменить email
- PATCH /v1/auth/vkId/add — привязать vkId
- PATCH /v1/auth/vkId/remove — отвязать vkId
- POST /v1/page/change_password — изменить пароль (требует Bearer)

ProfileIosViewModel должен:
- Загружать пользователя через AuthManager
- logout() — очистить токен и SQLDelight, переключить на Login
- changePassword(new: String) — вызвать API, показать результат
- updateEmail(new: String)
- removeVkId()

Тесты:
- commonTest: моки AuthManager, проверка вызовов
- Preview: реальный пользователь, пользователь без vkId
- Ручная проверка: сменить пароль → выйти → войти с новым → работает

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Профиль загружается, поля корректны
- [ ] Logout работает корректно (нельзя случайно удалить локальные рейсы)
- [ ] Тесты + ручная проверка
- [ ] `git commit -m "ios: complete ProfileView with auth actions"`

---

## ЭТАП 3 — Сделать новые экраны (дни 8-12)

### Задача 3.1 — WorkScheduleView (график работы)

**Контекст**: календарь рабочих дней, типов дней (рабочий, выходной,
праздник, отвлечение), плюс отображение рейсов на каждый день.

**Промпт для Claude Code**:

```
Сделай экран WorkScheduleView с нуля. Это календарь:
- Месячный вид
- Каждая ячейка: число + цвет/иконка типа дня
- Тапнуть → детали дня (рейсы на этот день, тип отвлечения если есть)

Источники данных:
- Производственный календарь (CalendarUseCase, GET /v1/production_calendar)
- Региональные праздники (RegionalHolidaysRepository, GET /v1/regional_holidays)
- Личные отвлечения (ReleaseDayUseCase, GET /v1/release_days)
- Рейсы пользователя (RouteUseCase, GET /v1/route/)

WorkScheduleIosViewModel:
  Состояние:
  - currentMonth: Int (0-based!)
  - currentYear: Int
  - days: List<DayInfo> (число, тип, рейсы, отвлечение)
  - isLoading: Boolean
  Методы:
  - loadMonth(month, year)
  - addRelease(date, type) — добавить отвлечение (отпуск, больничный...)
  - removeRelease(date)
  - prevMonth() / nextMonth()
  - watchX(callback) для каждого

⚠️ ВАЖНО: month 0-based в API (январь = 0). Конвертируй в kotlinx-datetime.Month
(1-based) только при отображении или работе с системными API.

UI:
- Header: "Январь 2026" + стрелки prev/next
- Сетка 7×6 (стандартный календарь)
- Цветовая кодировка: рабочий — нейтральный, выходной — серый, праздник —
  красный, отпуск — зелёный, больничный — оранжевый
- При тапе на ячейку — sheet с деталями

Тесты:
- commonTest: правильность маппинга 0-based ↔ 1-based, обработка
  пересечений (например, рабочий + личный отпуск = отпуск)
- Preview: январь 2026 с праздниками, июль 2026 с отпуском, текущий месяц
- Ручная проверка

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Календарь корректно отображает все типы дней
- [ ] Можно добавить/удалить отвлечение
- [ ] Тесты, Preview, ручная проверка
- [ ] `git commit -m "ios: implement WorkScheduleView with calendar"`

---

### Задача 3.2 — SearchView (поиск рейсов)

**Промпт для Claude Code**:

```
Сделай SearchView — поиск рейсов по фильтрам:
- По датам (от - до)
- По номеру маршрута
- По серии локомотива
- По станции (любая в маршруте)

SearchIosViewModel:
  Состояние:
  - filters: SearchFilters (datesFrom, datesTo, number, locoSeries, station)
  - results: List<DomainRoute>
  - isSearching: Boolean

Поиск реализуется через RouteUseCase.search(filters) (если такой метод
есть; если нет — добавить).

UI:
- Форма с фильтрами в верхней части
- Кнопка "Найти"
- Список результатов снизу (NavigationLink на FormView для каждого)

⚠️ НЕ используй GET /v1/route/search/routes — этот эндпоинт сейчас
публичный (без авторизации) и подлежит фиксу. Поиск делать локально по
SQLDelight, по уже синхронизированным рейсам.

Тесты:
- commonTest: фильтрация по датам, по номеру, по станции, комбинированные
- Preview: пустой результат, 1 рейс, 20 рейсов
- Ручная проверка

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Поиск работает локально по SQLDelight
- [ ] Тесты, Preview, ручная проверка
- [ ] `git commit -m "ios: implement SearchView with local filters"`

---

### Задача 3.3 — AllRoutesView (все рейсы списком)

**Промпт для Claude Code**:

```
Сделай AllRoutesView — простой список всех рейсов пользователя без фильтров
по месяцам (в отличие от Home, которая показывает только текущий месяц).

AllRoutesIosViewModel:
  Состояние:
  - routes: List<DomainRoute> (отсортированы по timeStartWork DESC)
  - isLoading: Boolean
  Методы:
  - loadAll()
  - delete(routeId)
  - copy(routeId)

UI:
- List из NavigationLink → FormView
- Каждая ячейка: дата, номер маршрута, длительность, превью локомотивов/поездов
- Pull-to-refresh для синхронизации с сервером
- Swipe action: удалить, копировать

Тесты:
- commonTest: загрузка, удаление, копирование
- Preview: пустой список, 5 рейсов, 50 рейсов
- Ручная проверка

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Список загружается, работают swipe-действия
- [ ] Pull-to-refresh запускает sync
- [ ] Тесты, Preview, ручная проверка
- [ ] `git commit -m "ios: implement AllRoutesView with swipe actions"`

---

### Задача 3.4 — SalaryCalculationView (расчёт зарплаты)

**Контекст**: самый сложный логически экран. Считает зарплату по правилам
из SalarySetting. Уже есть SalaryCalculationUseCase в commonMain.

**Промпт для Claude Code**:

```
Сделай SalaryCalculationView — экран расчёта зарплаты за месяц.

Используй существующий SalaryCalculationUseCase из domain/. Если нужны
улучшения — обсудим, но логику расчёта НЕ меняй (она уже работает на
Android и покрыта тестами).

SalaryCalculationIosViewModel (возможно уже частично есть):
  Состояние:
  - month, year
  - calculation: SalaryCalculation (структура с разбивкой)
  - isLoading: Boolean

UI с разбивкой:
- Header: "Январь 2026"
- Секция "Часы работы" (всего, ночные, праздничные)
- Секция "Базовая ставка" (тариф × часы)
- Секция "Надбавки":
  - Ночные (+40%)
  - Районный коэффициент
  - Северные
  - Класс квалификации
  - Тяжеловесные
  - Длинносоставные
  - Удлинённое плечо
  - В одно лицо
  - Зональная
  - Прочие
- Секция "Итого начислено"
- Секция "Удержания":
  - НДФЛ
  - Профсоюзные
  - Прочие
- Секция "К выплате"

Можно использовать DisclosureGroup для каждой секции, чтобы пользователь
мог разворачивать детали.

Тесты:
- commonTest: уже есть для SalaryCalculationUseCase. Если нет — добавить
  для критичных кейсов (полный месяц, частичный, с отвлечениями)
- Preview: 3 типа месяцев (большой, средний, пустой)
- Ручная проверка с реальными настройками

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Расчёт совпадает с Android для одного и того же входа
- [ ] Все секции отображаются корректно
- [ ] Тесты, Preview, ручная проверка
- [ ] `git commit -m "ios: implement SalaryCalculationView"`

---

## ЭТАП 4 — VK ID авторизация (дни 13-14)

### Задача 4.1 — Регистрация iOS-приложения в VK Developer

**Не для Claude Code** — это твоя ручная работа в браузере.

Шаги:
1. Зайди на https://dev.vk.com/
2. Найди существующее приложение LocoDriver (то, что используется для Android)
3. В настройках добавь iOS-платформу:
   - Bundle ID: тот же, что в Xcode
   - URL Scheme для callback: например `vk<APP_ID>` (стандартное)
4. Запиши APP_ID и сохрани

**Definition of Done**:
- [ ] iOS-приложение зарегистрировано в VK Developer
- [ ] APP_ID и URL Scheme записаны где-то в безопасном месте

---

### Задача 4.2 — Интеграция VKID iOS SDK

**Промпт для Claude Code**:

```
Интегрируй VKID iOS SDK в наше приложение. Документация:
https://id.vk.com/about/business/go/docs/ru/vkid/latest/sdk-ios/install

Шаги:
1. Установить VKID через SPM (предпочтительно):
   File → Add Packages → https://github.com/VKCOM/vkid-ios-sdk
2. В Info.plist добавить:
   - LSApplicationQueriesSchemes: vk, vkauthorize
   - URL Types с URL Scheme vk<APP_ID>
3. В iOSApp.swift инициализировать VKID после Koin:
   try VKID.shared.set(config: VKIDConfig(appId: <APP_ID>, ...))
4. Добавить onOpenURL handler для возврата от VK
5. Создать новый экран LoginView (или дополнить существующий) с кнопкой
   "Войти через VK"
6. По нажатию — VKID.shared.authorize → получить access_token →
   отправить на наш сервер через AuthManager.loginWithVkId(token)
7. Обработать ответ: если успех — сохранить наш JWT, перейти на главный

Тесты:
- Сложно автоматизировать VKID auth flow, поэтому только ручная проверка:
  * Войти через VK на симуляторе → главный экран
  * Выйти → войти ещё раз → работает

⚠️ APP_ID получи из задачи 4.1.

Покажи план перед началом.
```

**Definition of Done**:
- [ ] VKID SDK установлен
- [ ] LoginView с кнопкой "Войти через VK" работает
- [ ] После успешной авторизации главный экран открывается
- [ ] Logout и повторная авторизация работают
- [ ] `git commit -m "ios: integrate VK ID authentication"`

---

## ЭТАП 5 — Тестирование и подготовка к релизу (после 14 дней)

### Задача 5.1 — UI-тесты критичных потоков

**Промпт для Claude Code**:

```
Напиши XCUITest для критичных пользовательских потоков:

1. Авторизация по email:
   - Запуск приложения → форма логина
   - Ввод email и пароля → нажатие "Войти"
   - Проверка: появился TabView с главным экраном

2. Создание рейса:
   - С главного экрана → "+ Новый маршрут"
   - Заполнить timeStartWork, timeEndWork, номер
   - Сохранить
   - Проверка: маршрут появился в списке Home

3. Удаление рейса:
   - Long-press или swipe на рейсе → удалить
   - Проверка: рейса нет в списке

4. Синхронизация:
   - Pull-to-refresh на Home
   - Проверка: indicator появился, потом скрылся, нет ошибок

Используй стабильные accessibilityIdentifier у ключевых элементов.

Покажи план перед началом.
```

**Definition of Done**:
- [ ] 4 UI-теста написаны и проходят
- [ ] `git commit -m "test: add XCUITest for critical user flows"`

---

### Задача 5.2 — End-to-end тест синхронизации

**Это полу-ручная задача**.

**Промпт для Claude Code**:

```
Помоги написать инструкцию для ручного end-to-end теста синхронизации
между Android и iOS:

1. Создать тестового пользователя на сервере (или использовать существующий)
2. Авторизоваться на Android-эмуляторе → создать рейс с уникальным номером
   (например "TEST_E2E_<timestamp>") → сохранить → дождаться sync (или
   принудительно)
3. Авторизоваться на iOS-симуляторе тем же пользователем → pull-to-refresh
4. Проверить: рейс с уникальным номером появился в списке
5. Открыть рейс на iOS, добавить локомотив, сохранить → дождаться sync
6. На Android — pull-to-refresh, проверить что локомотив появился

Сохрани этот сценарий как ios/E2E_TEST.md в репозитории.

Если возможно — автоматизируй sync-вызов через тестовый скрипт.
```

**Definition of Done**:
- [ ] E2E_TEST.md создан с инструкцией
- [ ] Прогон сценария вручную успешен
- [ ] `git commit -m "test: add E2E sync test scenario"`

---

### Задача 5.3 — TestFlight beta-сборка

**Не для Claude Code** — твоя ручная работа в Xcode + App Store Connect.

Шаги:
1. В Xcode: Product → Archive → Distribute App → TestFlight & App Store
2. Дождаться обработки в App Store Connect
3. Добавить тестеров (минимум — себя)
4. Установить TestFlight на iPhone, скачать сборку, прогнать критичные
   потоки
5. Если есть знакомые-машинисты — пригласить, попросить отзыв
6. Накопить минимум 2-3 дня тестирования и фидбэка

**Definition of Done**:
- [ ] Сборка в TestFlight доступна
- [ ] Минимум 2-3 дня тестирования прошло
- [ ] Все обнаруженные крэши и серьёзные баги исправлены

---

### Задача 5.4 — App Store metadata

**Не для Claude Code, но пара вещей можно сделать через него.**

Что нужно:
- Название: LocoDriver
- Описание (русское и английское) — около 4000 знаков
- Скриншоты для всех размеров iPhone (можно 6.5", 6.7", и т.д.)
- Иконка приложения (1024x1024)
- Privacy Policy URL — нужна страница на сайте с политикой конфиденциальности
- Поддержка URL — твой сайт или email
- Category: Business / Productivity (выбери)
- Privacy Manifest (с iOS 17+) — список используемых API и данных

**Промпт для Claude Code (для текста)**:

```
Помоги написать описание приложения LocoDriver для App Store на русском
(до 4000 знаков) и на английском.

Целевая аудитория — машинисты локомотивов в России (грузовое и
пассажирское движение).

Основные функции:
- Учёт рейсов с детальной информацией (локомотивы, секции, поезда,
  станции, пассажиры)
- Расчёт ночных часов с учётом часового пояса
- Расчёт зарплаты с надбавками
- Производственный календарь с отвлечениями (отпуска, больничные)
- Синхронизация с сервером
- Шаринг маршрутных листов

Стиль: профессионально, кратко, ключевые функции в первом параграфе
(чтобы влезли в превью в App Store).

Также:
- Промо-текст: до 170 знаков
- Ключевые слова: до 100 знаков (через запятую)
```

**Definition of Done**:
- [ ] Описания готовы
- [ ] Скриншоты подготовлены (можно через симулятор: Cmd+S)
- [ ] Privacy Policy опубликован на твоём сайте
- [ ] Заполнено всё в App Store Connect

---

### Задача 5.5 — Submit в App Store Review

**Ручная работа** в App Store Connect.

1. Создать новую версию (1.0.0)
2. Прикрепить TestFlight-сборку
3. Заполнить metadata (см. 5.4)
4. Submit for Review
5. Ждать 1-7 дней (обычно 1-3)
6. Если отказ — читать причину, исправлять, повторно submit

**Возможные причины отказа** (по опыту):
- Privacy Policy недоступен — проверь URL
- Нет описания назначения камеры/микрофона/локации в Info.plist (если
  используются)
- Login-функционал требует тестового аккаунта — добавь Demo Account в
  App Review Information
- Crash при первом запуске — проверь на чистом устройстве

**Definition of Done**:
- [ ] Приложение в App Store Review
- [ ] Прошёл review успешно
- [ ] Релиз опубликован

---

## Реалистичная оценка времени

| Этап | Срок | Что зависит |
|---|---|---|
| Этап 0 (Discovery) | День 1, ~2 часа | — |
| Этап 1 (Чистка) | Дни 2-3 | Этап 0 |
| Этап 2 (Доделать экраны) | Дни 4-7 | Этап 1 |
| Этап 3 (Новые экраны) | Дни 8-12 | Этап 2 |
| Этап 4 (VK ID) | Дни 13-14 | Этап 3 |
| Этап 5 (Релиз) | +1-2 недели после Этапа 4 | TestFlight beta + App Review |

**Итого до публичного релиза в App Store: ~3-4 недели от сегодня.**

Если идти быстрее — рискуешь крэшами в проде. Если идти медленнее —
Android-аудитория ждёт.

---

## Критичные точки контроля

Перед каждым этапом проверяй:

**Перед Этапом 2**:
- [ ] iOS-приложение собирается без ошибок после чистки
- [ ] Существующая функциональность (Home, FormView, авторизация, sync)
      работает
- [ ] Apple Developer Program активен

**Перед Этапом 3**:
- [ ] Все 5 экранов из Этапа 2 (Loco, Train, Passenger, Settings, Profile)
      работают
- [ ] Локальные тесты проходят

**Перед Этапом 4**:
- [ ] Все 4 экрана из Этапа 3 (WorkSchedule, Search, AllRoutes, Salary)
      работают
- [ ] Sync с сервером стабильный

**Перед Этапом 5**:
- [ ] VK ID работает
- [ ] Все основные функции протестированы вручную
- [ ] Нет известных крэшей

**Перед Submit**:
- [ ] TestFlight beta минимум 2-3 дня без серьёзных проблем
- [ ] Все скриншоты, описания, Privacy Policy готовы

---

## Что делать, если задача буксует

Если задача занимает более 1.5×ожидаемого времени — **остановись и спроси
помощь** (у пользователя в Project knowledge — продолжающиеся сессии в
Claude.ai могут помочь разобрать проблему).

Не превращай 2-часовую задачу в 8-часовую без анализа причины. Иногда
лучше:
- Упростить функциональность (для релиза достаточно базового, расширим
  потом)
- Перенести в backlog (на после релиза)
- Использовать заглушку/мок (`TODO: реализовать после релиза`)

---

## После релиза

Добавь задачи в backlog (отдельный файл `BACKLOG.md`):

- 🟡 Координированная миграция публичных GET (см. SECURITY_ACTION_PLAN.md)
- 🟡 TLS для API
- 🟡 Adminer через SSH-туннель
- 🟡 Email-эндпоинты на freemyip.com — переезд на свой домен
- 🟡 Покупки (Robokassa SDK для iOS — или In-App Purchase)
- 🟡 Push-уведомления
- 🟡 Apple Sign In (требование Apple, если есть VK ID)
- 🟡 Dark Mode
- 🟡 Dynamic Type (доступность шрифтов)
- 🟡 iPad layout
- 🟢 Куча мелких багов из API reference (см. 31_API_REFERENCE.md)

---

## Ссылки

- `CLAUDE.md` — правила работы Claude Code
- `CODEBASE.md` — структура кодовой базы
- `31_API_REFERENCE.md` — контракт с сервером
- `SECURITY_ACTION_PLAN.md` — оставшиеся задачи безопасности
- `02_KMP_MIGRATION_PLAN.md` — выбор библиотек и архитектура

В Project knowledge на claude.ai — все эти документы есть.
