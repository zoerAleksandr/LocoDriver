# 60_IOS_TODO.md — План релиза iOS-версии

> Список задач до публичного релиза iOS в App Store. Работа через
> Claude Code, по одной задаче за раз.
>
> Обновлено 25 апреля 2026 после аудита проекта (Этап 0.1 выполнен) и
> уточнения модели монетизации.
>
> Каждая задача — готовый промпт, который можно скопировать в Claude Code.
> В конце каждой задачи — критерии готовности (Definition of Done).

---

## Контекст (на основе аудита 25 апреля)

**Что уже готово (хорошие новости):**

| Экран | Статус | Замечания |
|---|---|---|
| HomeView | 🟢 готов | Нюанс: «Отвлечения» — заглушка |
| FormView | 🟢 готов | Нюанс: поля break не отображаются |
| FormPassengerView | 🟢 готов | — |
| SettingsView | 🟢 готов | Нет настроек зарплаты, региональных праздников |

**Что нужно доделать UI (Kotlin-VM уже есть):**

| Экран | Статус | Что доделать |
|---|---|---|
| FormLocoView | 🟡 | Секции read-only — нужны TextField для acceptedEnergy/deliveryEnergy/fuel; toolbar Save |
| FormTrainView | 🟡 | Нет станций, нет фазы обслуживания, нет toolbar Save |
| ProfileView | 🟡 | VK ID — заглушка; нет регистрации; нет смены пароля |
| SalaryCalculationView | 🟡 | Только summary; нет разбивки по 10 надбавкам |

**Что нужно делать с нуля (новый Kotlin-VM + UI):**

| Экран | Статус | Что нужно |
|---|---|---|
| WorkScheduleView | 🔴 | WorkScheduleIosViewModel + полный календарный UI |
| SearchView | 🔴 | SearchIosViewModel + UI с фильтрами |
| AllRoutesView | 🟡 | (опционально для MVP) AllRoutesIosViewModel + фильтры |
| PurchasesView | 🔴 | Заменить на ExternalSubscriptionView (см. ниже) |

**Хорошая новость:** все 8 существующих ViewModelWrapper'ов **подключены
к реальным Kotlin-VM**. Новые VM нужны только для WorkSchedule и Search.

---

## Стратегия монетизации в первом релизе

⚠️ **iOS App Store IAP в России сейчас не работает** (с 1 апреля 2026 Apple
отключил обработку платежей в России). Будущее российских разработчиков с
подписками — внешние платежи (по письму Apple 2023 года).

**Решение для релиза 1.0:**

- Триал: 20 рейсов (как на Android, лимит уже на сервере в
  `user.subscriptionPeriod`).
- После 20 рейсов: экран блокировки создания новых рейсов с
  кнопкой «Оформить подписку» → **открывает внешнюю ссылку в Safari** на
  `locodriver.ru/subscribe` (или твой сайт). Оплата через Robokassa и т.п.
  Сервер активирует подписку.
- В описании приложения и скриншотах **не упоминать внешние платежи**.
- StoreKit/IAP — **не делаем в 1.0**.

Это даёт паритет с Android по функциональности (после оплаты пользователь
получает доступ ко всем фичам) и обходит ограничения Apple в РФ.

---

## Стратегия авторизации в первом релизе

- **Email/пароль** (уже работает) — основной метод.
- **Sign in with Apple** — обязательно (требование Apple Review при наличии
  любого социального логина или просто как опция).
- **VK ID** — добавляем для удобства российских пользователей.

Apple обязывает: если есть VK ID → должен быть Sign in with Apple
равноценным. См. Этап 4.

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

---

## 🚨 ДЕЛАТЬ ПРЯМО СЕЙЧАС (параллельно с разработкой)

### A. Оплатить Apple Developer Program ($99/год)

**Срок горит**, активация 1-7 дней. Без этого:
- Нельзя создать TestFlight-сборку для бета-тестирования
- Нельзя залить приложение в App Store

Шаги:
1. Зайди на https://developer.apple.com/programs/enroll/
2. Войди под Apple ID
3. Выбери **Individual** (не Organization — Organization требует D-U-N-S
   Number и долгую проверку компании)
4. Оплати $99 (привязка к твоему Apple ID, продление автоматическое раз в год)
5. Дождись email от Apple с подтверждением (обычно 24-48 часов)

**Definition of Done**:
- [ ] Apple Developer Program активирован
- [ ] В App Store Connect появилась возможность создать новое приложение

---

### B. Создать VK Developer iOS-приложение

После того как у тебя будет Apple Developer + Bundle ID для iOS-приложения:

1. Зайди на https://dev.vk.com/
2. Открой существующее приложение LocoDriver (то, что для Android)
3. В настройках → Платформы → добавь iOS:
   - Bundle ID: тот же, что в Xcode (например `com.zcompany.locodriver`)
   - Universal Link / URL Scheme: `vk<APP_ID>` (стандартное)
4. Запиши APP_ID

**Definition of Done**:
- [ ] iOS-платформа добавлена в VK Developer
- [ ] APP_ID записан где-то безопасно

---

### C. Подготовить Privacy Policy

Apple Review **не пропустит без работающего Privacy Policy URL**.

Если у тебя есть сайт (locodriver.ru или подобный):
- Создай страницу `/privacy-policy` или `/privacy` с текстом политики
- Текст должен описывать: какие данные собираются (email, vk_id, рейсы),
  как используются, как удалить
- На русском (для российских пользователей) и желательно на английском

Можешь использовать генераторы Privacy Policy (FreePrivacyPolicy.com,
Iubenda и др.) и адаптировать.

**Definition of Done**:
- [ ] Privacy Policy опубликован на твоём сайте
- [ ] URL работает и открывается без авторизации

---

## ЭТАП 0 — Discovery (задача 0.1 ✅, остальные нужно сделать)

### Задача 0.1 — Аудит iOS-проекта ✅ ВЫПОЛНЕНО

Результаты вынесены в шапку этого файла (раздел «Контекст»).

**Что найдено:**
- 4 экрана 🟢, 4 экрана 🟡, 4 экрана 🔴
- Все 8 ViewModelWrapper'ов имеют backing Kotlin-VM
- Нужны новые VM: `WorkScheduleIosViewModel`, `SearchIosViewModel`,
  опционально `AllRoutesIosViewModel`
- В worktree `.claude/worktrees/wonderful-cori/` есть `AppInitIosViewModel.kt`,
  которого нет в основной ветке — нужно проверить, не нужно ли смерджить

⚠️ **Действие**: проверь worktree `wonderful-cori` — что там за работа?
Если она нужна — смерджи в основную ветку, иначе потеряется. Команда для
проверки:

```bash
cd /Users/zoer/AndroidStudioProjects/LocoDriver
git worktree list
cd .claude/worktrees/wonderful-cori
git log --oneline -10
git diff main..HEAD --stat
```

Если изменения нужны — смерджи или попроси Claude Code сделать это в
отдельной задаче.

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

### Задача 0.5 — Готовность серверного API к Sign in with Apple

**Цель**: понять, нужно ли расширять API для приёма Apple ID токена.

**Контекст**: для соответствия правилам Apple, если есть VK ID, нужен
Sign in with Apple. Apple Sign In возвращает identity token (JWT,
подписанный Apple). Сервер должен верифицировать этот токен и обменять
на свой JWT для пользователя.

**Промпт для Claude Code**:

```
Проверь серверный API на готовность к Sign in with Apple.

ВАЖНО: серверный код в другом репозитории (/Users/zoer/Downloads/proxy-parser/).
НЕ меняй ничего, только смотри.

1. Открой /Users/zoer/Downloads/proxy-parser/src/api/v1/auth.py и посмотри,
   какие методы аутентификации поддерживаются. По CLAUDE.md и
   31_API_REFERENCE.md ожидается: login, email, vkId.
2. Проверь, есть ли упоминание "apple", "appleId", "siwa" (Sign in with
   Apple) или подобных в:
   - src/api/v1/auth.py
   - src/services/auth.py
   - src/schemas/request.py
3. Проверь модель пользователя в src/models/users.py — есть ли поле
   apple_id или подобное.

Если ничего нет — это значит, на сервере нужно добавить:
- Новое значение `methodAuth: "appleId"` в Pydantic-схеме UserCredentials
- Логика верификации Apple identity token (через jwks от Apple)
- Поле user.apple_id в БД, миграция для существующих пользователей
- Эндпоинт POST /v1/auth (расширить) и/или PATCH /v1/auth/appleId/add

Сделай только отчёт. НЕ меняй код.
```

**Definition of Done**:
- [ ] Понятно, что нужно добавить на сервере для Sign in with Apple
- [ ] Если изменения нужны — записано как отдельная задача для серверного
      репозитория (НЕ для этого TODO, эта задача делается отдельно в той же
      Claude.ai сессии или новой)

⚠️ **Эту серверную задачу нужно сделать ДО Этапа 4** (интеграция Sign in with
Apple на iOS). Иначе клиент не сможет обменять Apple-токен на JWT.

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

**Контекст текущего состояния**:
- Несколько unit-тестов в `commonTest` уже есть (для `domain/`).
- iOS UI-тестов нет вообще, target в Xcode не создан.

⚠️ **Часть этой задачи делается тобой вручную в Xcode**, потому что
создание target'ов в Xcode-проекте — UI-операция, которую Claude Code
сделать не может.

**Промпт для Claude Code (часть 1 — Kotlin-тесты)**:

```
Проверь и настрой Kotlin-тесты в commonTest:

1. Найди существующие commonTest:
   find . -path "*/commonTest/*" -name "*.kt"

2. Запусти их:
   ./gradlew :domain:allTests
   ./gradlew :core:allTests
   ./gradlew :data_local:allTests
   ./gradlew :data_remote:allTests

3. Если ошибки — почини их (могут быть из-за обновления Kotlin или зависимостей)
4. Документируй в CODEBASE.md в разделе "Запуск тестов":
   - Команда для запуска commonTest: ./gradlew :module:allTests
   - Команда для запуска iOS-теста: ./gradlew :module:iosSimulatorArm64Test

Сделай это и покажи что получилось.
```

**Часть 2 — iOS Unit-тесты** (вручную в Xcode, ~10 минут):

1. Открой `iosApp/iosApp.xcodeproj` в Xcode
2. File → New → Target → выбери **Unit Testing Bundle**
3. Имя: `iosAppTests`, Language: Swift, Project: iosApp, Target to be Tested: iosApp
4. Нажми Finish
5. В созданном файле `iosAppTests.swift` (или подобном) есть пустой smoke-тест.
   Запусти Cmd+U — должен пройти

**Часть 3 — iOS UI-тесты** (вручную в Xcode, ~10 минут):

1. File → New → Target → **UI Testing Bundle**
2. Имя: `iosAppUITests`, Language: Swift, Project: iosApp,
   Target to be Tested: iosApp
3. Finish
4. В созданном файле есть пустой UI-тест. Запусти Cmd+U — должен пройти
   (запустит симулятор и приложение)

**Часть 4 — Документация** (через Claude Code):

```
Добавь в CODEBASE.md раздел "Запуск тестов" с командами:

## Запуск тестов

### Kotlin Multiplatform (commonTest)
- Все тесты модуля: ./gradlew :domain:allTests
- Только iOS-симулятор: ./gradlew :domain:iosSimulatorArm64Test
- Только Android JVM: ./gradlew :domain:testDebugUnitTest

### iOS Unit Tests (XCTest)
- В Xcode: Cmd+U с выбранной схемой iosApp
- Через CLI: xcodebuild -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15' test

### iOS UI Tests (XCUITest)
- В Xcode: Cmd+U с выбранным target'ом iosAppUITests
- Через CLI: xcodebuild -scheme iosAppUITests test
```

**Definition of Done**:
- [ ] Kotlin commonTest запускается без ошибок
- [ ] iOS Unit Test target создан и пустой smoke-тест проходит
- [ ] iOS UI Test target создан и пустой smoke-тест проходит
- [ ] Раздел "Запуск тестов" добавлен в CODEBASE.md
- [ ] `git commit -m "test: setup testing infrastructure for iOS"`

---

### Задача 1.4 — Сетевая надёжность и обработка ошибок ⚠️ БЛОКЕР РЕЛИЗА

**Контекст** (обнаружено 26 апреля 2026 при тестировании синхронизации):
- При pull-to-refresh на HomeView и при сохранении рейсов **периодически**
  выскакивает iOS-alert с **полным stack trace** ошибки `NSURLErrorDomain
  Code=-1005 "The network connection was lost"`. Это **блокер**:
  - Apple Review отклоняет приложения, показывающие технические сообщения
    об ошибках конечному пользователю
  - Машинисты не понимают что произошло, паникуют, теряют доверие к
    приложению
- Ошибка плавающая: иногда сохраняется, иногда нет. Это намекает на:
  - Проблемы с keep-alive HTTP-соединениями (uvicorn закрывает idle,
    Ktor использует устаревший connection)
  - HTTP без TLS + iOS App Transport Security временами разрывает
    соединения для безопасности
  - Отсутствие retry-логики для transient-ошибок

**Цель**: добавить надёжную обработку сетевых ошибок на всех уровнях
(Ktor → ViewModel → SwiftUI), показывать пользователю короткие понятные
сообщения, автоматически повторять transient-ошибки.

**Промпт для Claude Code**:

```
Реши проблему с сетевой надёжностью на iOS. План работы — большой,
делать по шагам с моим подтверждением после каждого.

ШАГ 1. Аудит текущего обработчика ошибок

1. Найди где сейчас обрабатываются сетевые ошибки на iOS:
   - В data_remote/src/commonMain/.../KtorRemoteRestApi.kt
   - В data_remote/src/commonMain/.../RemoteRestClient.kt
   - В data_remote/src/commonMain/.../SyncManager.kt и
     RoutesManager.kt
   - На уровне iOS-ViewModel (HomeIosViewModel, FormIosViewModel)
   - На уровне Swift Wrapper (HomeViewModelWrapper и др.)

2. Найди где формируется текст alert'а который показал пользователь.
   В скриншоте видно полный NSError description — значит, где-то
   делается `Text(error.localizedDescription)` или аналогично, и сырое
   сообщение от iOS-NSURLSession улетает в UI.

3. Составь отчёт:
   - Где ловится Throwable / NSError
   - Где он мапится в строку для UI
   - Где показывается alert
   
   Это поможет понять, что нужно изменить.

ШАГ 2. Доменный слой ошибок (commonMain)

Создай (или дополни) sealed class в core/ или domain/:

```kotlin
sealed class AppError(
    val userMessage: String,  // короткое для UI
    val technicalDetails: String? = null  // для логов
) {
    object NoInternet : AppError("Нет соединения с интернетом")
    object Timeout : AppError("Сервер не отвечает. Попробуйте снова.")
    object ServerError : AppError("Ошибка сервера. Попробуйте позже.")
    object Unauthorized : AppError("Нужна повторная авторизация")
    data class Unknown(val cause: Throwable) :
        AppError(
            userMessage = "Что-то пошло не так",
            technicalDetails = cause.message
        )
}
```

Добавь функцию маппинга платформенных ошибок в AppError. Для Ktor
конкретно:
- `IOException` / `ConnectException` → NoInternet
- `HttpRequestTimeoutException`, `SocketTimeoutException` → Timeout
- `ClientRequestException` (4xx):
  - 401 → Unauthorized
  - остальные → ServerError
- `ServerResponseException` (5xx) → ServerError
- Остальное → Unknown

⚠️ Особый случай для iOS: NSURLError -1005, -1009, -1001, -1004 — это
сетевые проблемы, маппить в NoInternet или Timeout. Их Ktor оборачивает
в IOException, но проверь это на практике.

ШАГ 3. Retry-логика в Ktor

В RemoteRestClient.kt добавь HttpRequestRetry плагин Ktor:

```kotlin
install(HttpRequestRetry) {
    retryOnExceptionIf(maxRetries = 3) { _, cause ->
        cause is IOException && cause !is CancellationException
    }
    retryOnServerErrors(maxRetries = 2)  // 5xx
    exponentialDelay(base = 2.0, maxDelayMs = 5000)
}
```

⚠️ ВАЖНО: retry безопасен только для GET и для idempotent POST (где
сервер корректно обработает повторный запрос с тем же ID — у нас
POST /v1/route/ это делает после фикса IDOR).

⚠️ ВАЖНО: НЕ ставь retry на login/auth-эндпоинты. Если 401 — нужен
явный logout, не retry.

Также добавь HttpTimeout:

```kotlin
install(HttpTimeout) {
    requestTimeoutMillis = 30_000
    connectTimeoutMillis = 10_000
    socketTimeoutMillis = 30_000
}
```

И отключи keep-alive (или поставь короткий keep-alive), чтобы избежать
устаревших соединений. Для Darwin engine на iOS:

```kotlin
install(DefaultRequest) {
    headers.append("Connection", "close")
}
```

Это force-close после каждого запроса. Не оптимально для
производительности, но устраняет проблему -1005. Альтернативно —
настроить URLSessionConfiguration более тонко (timeoutIntervalForRequest,
HTTPMaximumConnectionsPerHost), но для надёжности «close после каждого»
проще.

ШАГ 4. ViewModel-слой

В iOS-ViewModel'ах (HomeIosViewModel и др.) сейчас, скорее всего, есть
поле `errorMessage: StateFlow<String?>`. Замени на:

```kotlin
private val _error = MutableStateFlow<AppError?>(null)
val error: StateFlow<AppError?> = _error
fun watchError(callback: (AppError?) -> Unit) { ... }

// при ошибке:
catch (t: Throwable) {
    _error.value = AppError.from(t)  // мапим
    Logger.e(TAG, "Sync failed", t)  // лог с деталями
}
```

ШАГ 5. SwiftUI alerts

В Wrapper'ах:
```swift
@Published var error: AppError? = nil

init() {
    viewModel.watchError { [weak self] err in
        DispatchQueue.main.async { self?.error = err }
    }
}
```

В View вместо текущего alert с полным сообщением:

```swift
.alert(
    error?.userMessage ?? "Ошибка",  // короткое
    isPresented: Binding(
        get: { vm.error != nil },
        set: { if !$0 { vm.clearError() } }
    )
) {
    Button("Повторить") { vm.retry() }
    Button("OK", role: .cancel) {}
}
```

⚠️ Важно: НЕ показывать `error.technicalDetails` пользователю! Только
`userMessage`. Технические детали идут только в Sentry/логи.

ШАГ 6. Логирование (только в логи, не в UI)

В классе AppError или в фабрике маппинга добавь логирование через
Kermit (или console.log если Kermit ещё не подключен):

```kotlin
fun AppError.Companion.from(t: Throwable): AppError {
    // лог технических деталей
    Logger.e("Network", t.message ?: "Unknown", t)
    return when (t) {
        is IOException -> NoInternet
        // ...
    }
}
```

⚠️ Не логируй URL, тело запроса, заголовки (там могут быть токены).

ШАГ 7. Проверка на симуляторе

Я не могу запустить Xcode, поэтому проверка на тебе. Сценарии:

ТЕСТ A. Нормальная работа:
- Открой приложение, pull-to-refresh на главном.
- Должен работать без ошибок.

ТЕСТ B. Симуляция ошибки (выключение Wi-Fi на Mac):
- Pull-to-refresh когда сети нет.
- Ожидаемо: alert "Нет соединения с интернетом" с кнопками
  "Повторить" и "OK".
- НЕ должен показывать stack trace.

ТЕСТ C. Симуляция -1005 (Network Link Conditioner):
- В Settings симулятора → Developer → Network Link Conditioner →
  выбери "100% Loss" и попробуй pull-to-refresh.
- Ожидаемо: alert "Нет соединения" или "Сервер не отвечает", без
  технических деталей.

ТЕСТ D. Сохранение рейса при плохой сети:
- Переключи Network Link Conditioner на "Very Bad Network" (50% loss).
- Создай рейс, нажми Сохранить несколько раз.
- Ожидаемо: при ошибке — alert с "Повторить", retry автоматически
  работает в Ktor (3 попытки), и в большинстве случаев рейс
  всё-таки сохраняется.
- На сервере проверь логи: нет дублирующихся записей (идемпотентность
  по UUID работает).

ШАГ 8. Коммит

ios: improve network reliability and error handling

- Add AppError sealed class with user-friendly messages
- Configure Ktor HttpRequestRetry and HttpTimeout
- Force connection: close on iOS to avoid -1005 stale connections
- Replace technical NSError messages in UI with localized strings
- Log technical details only, never show in UI

⚠️ ОБЩИЕ ПРАВИЛА:
- Перед каждым шагом — покажи что собираешься делать и подожди моего
  ОК.
- НЕ меняй серверный код. Если потребуется — останови и обсудим
  отдельно.
- НЕ ломай существующую функциональность Android (ViewModel'и общие
  через KMP — изменения должны быть совместимы).

Покажи план Шага 1 и начинай.
```

**Definition of Done**:
- [ ] AppError sealed class в общем коде
- [ ] HttpRequestRetry настроен в Ktor (3 попытки на IOException, 2 на 5xx)
- [ ] HttpTimeout настроен (30s request, 10s connect)
- [ ] Connection: close для iOS (или другое решение проблемы -1005)
- [ ] ViewModel'и используют AppError вместо строк
- [ ] Wrapper'ы конвертируют в @Published
- [ ] SwiftUI alerts показывают только `userMessage`, без технических деталей
- [ ] Логи пишут технические детали (без чувствительных данных)
- [ ] Тесты A-D на симуляторе проходят:
  - [ ] A: нормальная работа без ошибок
  - [ ] B: при выключенном Wi-Fi — короткий alert
  - [ ] C: с Network Link Conditioner 100% Loss — короткий alert
  - [ ] D: при плохой сети — retry работает, нет дубликатов на сервере
- [ ] Android-приложение продолжает работать без регрессии
- [ ] `git commit -m "ios: improve network reliability and error handling"`

⚠️ **Это блокер релиза**. Без этой задачи нельзя в Submit.

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

### Задача 2.6 — Экран блокировки после превышения лимита триала

**Контекст**: на Android после 20 рейсов блокируется создание новых
рейсов. Информация о подписке (`user.subscriptionPeriod`) хранится на
сервере. Для iOS-релиза 1.0 не делаем StoreKit/IAP — вместо этого
открываем внешнюю ссылку на сайт.

⚠️ **Apple Review требование:** в описании приложения и скриншотах НЕ
упоминать внешние платежи. На самом экране кнопка может быть
нейтральной — «Оформить подписку» — это разрешено по письму Apple 2023
для российских разработчиков.

**Промпт для Claude Code**:

```
Реализуй экран блокировки создания новых рейсов после превышения
триала (20 рейсов).

Состояние подписки уже приходит с сервера в user.subscriptionPeriod
(timestamp). Логика на Android:
- Если subscriptionPeriod в прошлом ИЛИ количество рейсов >= 20 → блок

Шаги:
1. Найди в shared (data_remote или domain) метод проверки подписки.
   Скорее всего он есть в AuthManager или UserRepository — должен
   возвращать что-то вроде SubscriptionStatus с полями:
   - isActive: Boolean
   - canCreateNewRoutes: Boolean
   - reason: String? (почему заблокировано)

2. Если такого метода нет — добавь в shared (НЕ копируй Android-логику
   слепо, проверь что она правильно работает). Не меняй сервер.

3. На FormView (Form/FormView.swift) при попытке создать новый рейс:
   - Если canCreateNewRoutes == false → показать sheet/alert
     SubscriptionBlockedView вместо открытия формы
   - Если рейс уже существует (редактирование) → разрешить (только
     создание блокируется, как на Android)

4. SubscriptionBlockedView (новый файл,
   iosApp/iosApp/Screens/Subscription/SubscriptionBlockedView.swift):
   - Заголовок: "Подписка"
   - Текст: "Достигнут бесплатный лимит — 20 рейсов. Для продолжения
     оформите подписку."
   - Кнопка "Оформить подписку" → открывает в Safari URL
     https://locodriver.ru/subscribe (используй UIApplication.shared.open
     или SwiftUI .openURL environment)
   - Кнопка "Уже есть подписка? Обновить статус" → вызывает sync с
     сервером (на случай если оплата прошла, но клиент не узнал)
   - Кнопка "Закрыть"

⚠️ ВАЖНО:
- НЕ упоминай в тексте на экране конкретные платёжные системы
  (Robokassa, банки и т.д.) — только нейтральное "оформить подписку"
- URL https://locodriver.ru/subscribe — спроси точный адрес у
  пользователя
- Кнопка "Восстановить покупки" Apple-style НЕ нужна (нет StoreKit)
- НЕ показывай цены в приложении

Тесты:
- commonTest для логики проверки подписки (если она в shared)
- SwiftUI Preview для SubscriptionBlockedView
- Ручная проверка:
  * Пользователь с активной подпиской → создание работает
  * Пользователь с истёкшей подпиской / >= 20 рейсов → блок
  * Кнопка "Оформить подписку" открывает Safari
  * После оплаты на сайте → "Обновить статус" разблокирует

Покажи план перед началом.
```

⚠️ **Уточни у меня перед стартом**: URL подписки — `https://locodriver.ru/subscribe`
(если другой — поменяй в коде). Страница на сайте должна быть готова к
этому моменту: с описанием подписки, кнопкой оплаты через Robokassa,
после успешной оплаты сервер должен обновлять `user.subscriptionPeriod`
(механизм уже работает на Android).

**Definition of Done**:
- [ ] Логика проверки подписки в shared
- [ ] FormView не открывается при превышении лимита, показывает
      SubscriptionBlockedView
- [ ] Кнопка ведёт на внешний URL в Safari
- [ ] Кнопка обновления статуса синхронизирует с сервером
- [ ] Тесты + ручная проверка с двумя пользователями (активная подписка
      и истёкшая)
- [ ] **Текст на экране НЕ упоминает конкретные платёжные системы**
- [ ] `git commit -m "ios: subscription expired blocking screen with external link"`

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

## ЭТАП 4 — Авторизация: VK ID и Sign in with Apple (дни 13-16)

⚠️ Apple обязывает: если приложение использует сторонний метод авторизации
(VK ID), необходимо предложить Sign in with Apple как равноценный вариант.
Без этого — отказ в App Store Review.

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

### Задача 4.3 — Добавление поддержки Sign in with Apple на сервере

⚠️ Это **серверная задача в другом репозитории** (`/Users/zoer/Downloads/proxy-parser/`).
Делать в **отдельной Claude.ai сессии** или в той же, но как отдельную задачу.

**Эту задачу нужно сделать ДО задачи 4.4 (интеграция Sign in with Apple на iOS).**

**Что нужно на сервере**:

1. Добавить значение `"appleId"` в `Literal["login", "email", "vkId", "appleId"]`
   в `UserCredentials.methodAuth` (`src/schemas/request.py`).
2. Добавить поле `apple_id: Optional[str]` в модель `User` (`src/models/users.py`)
   и миграцию для существующих БД.
3. Реализовать функцию верификации Apple identity token:
   - Получить JWKS от https://appleid.apple.com/auth/keys
   - Проверить подпись JWT (использовать `python-jose` или `pyjwt`)
   - Извлечь `sub` (Apple-уникальный ID пользователя)
   - Проверить `aud` (твой Apple Bundle ID)
   - Проверить `iss == "https://appleid.apple.com"`
   - Проверить `exp` (не истёк ли токен)
4. Расширить `POST /v1/auth`:
   - Если `methodAuth == "appleId"` → вызвать верификацию,
     найти/создать пользователя по `apple_id`, выдать JWT
5. Добавить `PATCH /v1/auth/appleId/add` (привязка к существующему аккаунту).
6. Добавить `PATCH /v1/auth/appleId/remove`.

⚠️ **Это изменение JSON-контракта**. Старые клиенты не используют
`appleId` — добавление безопасно. Но проверь, что изменения **не ломают**
существующих клиентов: добавление нового значения в Literal — безопасно
(старые клиенты не пришлют его). Поле `apple_id` в response —
безопасно (старые клиенты проигнорируют).

**Definition of Done**:
- [ ] На сервере есть метод `auth.py` с верификацией Apple identity token
- [ ] Pydantic-схема UserCredentials поддерживает `methodAuth: "appleId"`
- [ ] Миграция БД добавляет поле `apple_id`
- [ ] Эндпоинты `PATCH /v1/auth/appleId/add|remove` работают
- [ ] Тесты на сервере: верификация валидного токена, отказ невалидному

---

### Задача 4.4 — Интеграция Sign in with Apple в iOS-приложение

**Контекст**: Apple Sign In нативно поддерживается через AuthenticationServices
framework. Не нужно сторонних SDK.

**Промпт для Claude Code**:

```
Интегрируй Sign in with Apple в iOS-приложение.

Документация Apple:
https://developer.apple.com/documentation/sign_in_with_apple/

Шаги:
1. В Xcode → Signing & Capabilities → "+ Capability" → выбрать
   "Sign In with Apple". Это автоматически включит entitlement в проекте.

2. На LoginView (созданном в Задаче 4.2) добавить кнопку Sign in with Apple
   через нативный SwiftUI компонент:

   import AuthenticationServices

   SignInWithAppleButton(
       onRequest: { request in
           request.requestedScopes = [.email]  // только email, без полного имени
       },
       onCompletion: { result in
           switch result {
           case .success(let auth):
               if let credential = auth.credential as? ASAuthorizationAppleIDCredential {
                   // identity token нужен серверу
                   guard let tokenData = credential.identityToken,
                         let token = String(data: tokenData, encoding: .utf8) else {
                       // ошибка
                       return
                   }
                   // отправить на сервер для обмена на JWT
                   loginViewModel.signInWithApple(identityToken: token)
               }
           case .failure(let error):
               // обработать ошибку (пользователь отменил, нет сети и т.д.)
           }
       }
   )
   .signInWithAppleButtonStyle(.black)  // или .white, .whiteOutline в зависимости от темы
   .frame(height: 50)

3. Расширить LoginIosViewModel (Kotlin):
   - Добавить метод signInWithApple(identityToken: String)
   - Метод вызывает AuthManager.loginWithApple(token) → POST /v1/auth с
     methodAuth="appleId" и token (Apple identity token)
   - Сервер обменяет на JWT, который сохраняется в Keychain

4. Расширить AuthManager в data_remote/:
   - suspend fun loginWithApple(identityToken: String): AuthResult

5. На ProfileView добавить возможность отвязать Apple ID, если привязан
   (через PATCH /v1/auth/appleId/remove).

Тесты:
- Симулятор: попробовать Sign In with Apple. Симулятор поддерживает
  с iOS 13+ через тестовый Apple ID
- Реальное устройство: попробовать с тестового Apple ID (можешь
  использовать Hide My Email)
- Проверить: после Sign In приложение получает identity token, отправляет
  серверу, сервер возвращает JWT, главный экран открывается

⚠️ ВАЖНО: Apple identity token **истекает быстро** (10 минут после выдачи).
Клиент должен сразу отправить его серверу, не накапливать. После этого
работа идёт через наш JWT (как обычно).

⚠️ ВАЖНО для Apple Review: Sign in with Apple **должен быть равноценным**.
Если на LoginView есть VK ID — Apple ID должен быть рядом, тех же размеров,
не скрытый.

Покажи план перед началом.
```

**Definition of Done**:
- [ ] Sign in with Apple capability включён в проекте
- [ ] Кнопка отображается на LoginView
- [ ] При нажатии — открывается нативный диалог Apple
- [ ] После успешного входа — JWT получен, главный экран открыт
- [ ] Logout и повторная авторизация работают
- [ ] Тестировано на симуляторе и (если есть) на реальном устройстве
- [ ] `git commit -m "ios: integrate Sign in with Apple"`

---

## ЭТАП 5 — Тестирование и подготовка к релизу (дни 17-21+)

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

### Задача 5.3 — TestFlight beta-сборка с реальными машинистами

**Не для Claude Code** — твоя ручная работа в Xcode + App Store Connect.

**Контекст**: у тебя есть опытные водители, которые были в Android-бете.
Это **бесценно** — они быстро найдут реальные проблемы. Но нужна
подготовка, чтобы они дали полезный фидбэк.

**Шаги**:

1. **Сборка в Xcode**:
   - Product → Archive → Distribute App → TestFlight & App Store
   - Дождаться обработки в App Store Connect (~10-30 минут)

2. **Подготовка тестового build'а в App Store Connect**:
   - Заполнить "What to Test" (что нужно проверить)
   - Указать тестового пользователя (если есть)
   - Email для багов и обратной связи

3. **Подготовить инструкцию для тестеров** (текстовый документ или
   страница на сайте):

   ```
   # LocoDriver iOS — инструкция для бета-тестеров

   Спасибо, что согласились помочь с тестированием iOS-версии!

   ## Установка
   1. Установи приложение TestFlight из App Store
   2. Открой ссылку, которую я тебе пришлю → "Принять приглашение"
   3. В TestFlight установи LocoDriver
   4. Открой приложение

   ## Что нужно проверить
   Постарайтесь использовать приложение в **обычной работе** — оно
   должно вести себя так же, как на Android.

   ### Базовое
   - [ ] Логин (email/пароль или VK ID)
   - [ ] Главный экран показывает правильные рейсы
   - [ ] Создание нового рейса
   - [ ] Редактирование рейса (изменили номер → сохранили → вернулись →
         номер сохранился)
   - [ ] Удаление рейса
   - [ ] Синхронизация: создал рейс на iPhone → видно на Android
         через 1-2 минуты после открытия

   ### Сложные сценарии
   - [ ] Рейс с локомотивом, секциями, поездом, станциями (полная форма)
   - [ ] Расчёт зарплаты за месяц (значения совпадают с Android?)
   - [ ] Календарь с праздниками, отвлечениями
   - [ ] Поиск рейсов по дате/номеру

   ## Как сообщить о баге
   1. Сделай скриншот (Power + Volume Up на iPhone)
   2. Опиши:
      - Что делал
      - Что произошло (или не произошло)
      - Что ожидал
   3. Отправь на email <твой_email> или в Telegram <твой_контакт>

   ## Подписка
   - Триал: первые 20 рейсов бесплатно (как на Android)
   - После 20 рейсов — экран блокировки с кнопкой «Оформить подписку»,
     которая откроет сайт. Оплати через сайт — приложение разблокируется.
   - Уже есть подписка через Android-версию? Войди в тот же аккаунт —
     подписка автоматически активна.

   ## Что НЕ работает (известные ограничения первого релиза)
   - Push-уведомления — отсутствуют
   - Покупка подписки внутри приложения (через App Store IAP) —
     невозможна из-за ограничений Apple для России. Только через сайт.

   ## Срок
   Тестирование займёт минимум 5-7 дней. После этого мы выпустим
   приложение в публичный App Store.
   ```

4. **Пригласить тестеров**:
   - В App Store Connect → TestFlight → External Testers (или Internal)
   - Добавить email-адреса машинистов
   - Они получат ссылку для активации

5. **Минимум 5-7 дней beta** — для приложения водителей это критично:
   нужны реальные смены, реальные рейсы, реальные расчёты.

6. **Принимать фидбэк ежедневно**:
   - Bugfix цикл: получил баг → починил → новая сборка в TestFlight
   - Обычно нужно 2-3 итерации, чтобы стабилизировать

**⚠️ Важно про сложности первого релиза**:

- **Sandbox vs Production API**: проверь, что TestFlight-сборка
  использует **тот же production-сервер**, что и Android-релиз.
  Иначе тестеры будут работать с пустой БД.
- **Push в Production от первой сборки**: Apple может ограничить, не
  волнуйся, это норма для первой подачи.
- **Реальные машинисты могут найти баги, которые тебе и в голову не
  приходили** (например, ввод текста в специфическом формате,
  переключение в режим экономии заряда). Будь готов к итерациям.

**Definition of Done**:
- [ ] Сборка в TestFlight доступна
- [ ] Инструкция для тестеров отправлена
- [ ] Минимум 3 машиниста активно тестируют (или сам + 1-2 человека,
      если машинистов нельзя привлечь)
- [ ] 5-7 дней тестирования прошло
- [ ] Все обнаруженные крэши и серьёзные баги исправлены
- [ ] Финальная стабильная версия в TestFlight

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

### Задача 5.5 — Submit в App Store Review (первый релиз!)

**Ручная работа** в App Store Connect.

⚠️ Это **твой первый релиз в App Store**. Apple строго проверяет, и
самые частые отказы для первого релиза — формальные. Распишу подводные
камни.

**Шаги**:

1. **Создать новую версию в App Store Connect**:
   - Зайди на https://appstoreconnect.apple.com/
   - Выбери LocoDriver → + Version → 1.0.0
   - Прикрепи TestFlight-сборку как Build for Submission

2. **Заполнить App Information**:
   - Название (Russian + English)
   - Описание (до 4000 знаков на каждом языке)
   - Скриншоты (минимум для 6.7" iPhone — например, iPhone 16 Pro Max)
   - Иконка приложения (1024×1024, без альфа-канала, без скруглений)

3. **App Review Information** (это часто пропускают, и это причина
   автоматического отказа):
   - **Demo Account**: Apple Review будет тестировать функциональность.
     Создай тестового пользователя на сервере с реальными данными:
     ```
     Login: appstore-review@locodriver.test (или подобный)
     Password: <надёжный пароль>
     Demo Account Email: appstore-review@locodriver.test
     Demo Account Password: <тот же пароль>
     ```
   - **Notes for Reviewer**: расскажи Apple про специфику приложения:
     ```
     LocoDriver is an app for railway locomotive drivers in Russia.
     The app helps drivers track their work shifts, calculate wages
     according to Russian Railway industry rules, and synchronize data
     across devices.

     The app is in Russian language. Login methods:
     - Email/password
     - Sign in with Apple
     - VK ID (popular Russian social network)

     Demo account credentials are provided above. After login, you can:
     - View existing routes
     - Create a new route
     - Calculate monthly salary

     The app does not include any in-app purchases at this time.
     ```
   - **Contact Information**: телефон + email, на которые Apple может
     связаться

4. **App Privacy** (обязательно с iOS 14+):
   - Заполни декларацию о собираемых данных
   - Укажи, что собираешь: Email Address, User Content (рейсы), Identifiers
     (Apple ID), Other Data (рабочие данные машиниста)
   - Цели: App Functionality (для большинства)
   - Privacy Policy URL: твоя страница

5. **Pricing & Availability**:
   - Free
   - Доступность: можешь ограничить Россией + СНГ или выбрать
     "All Available Countries"

6. **Categories**:
   - Primary: **Productivity** или **Business**
   - Secondary: можно не указывать или выбрать релевантную

7. **Age Rating**:
   - Заполни анкету, должно получиться 4+

8. **Build Selection**:
   - Выбери стабильную TestFlight-сборку
   - **Перед Submit убедись**, что эта сборка протестирована минимум 5 дней

9. **Submit for Review**

**⚠️ Что часто становится причиной отказа для первого релиза**:

| Причина | Как избежать |
|---|---|
| Privacy Policy URL не работает | Проверь URL до Submit |
| Demo Account не работает | Залогинься сам с тех же кредов до Submit |
| Crash при первом запуске на чистом устройстве | Тестируй на новом симуляторе с очищенными данными |
| Pricing указан, но IAP не настроены | Если нет покупок — выставь Free, без IAP |
| Скриншоты с реальными чужими данными (PII) | Используй тестового пользователя для скриншотов |
| Privacy Manifest не заполнен (с iOS 17+) | Создай через Xcode → New File → Privacy |
| Sign in with Apple не работает на тестовом аккаунте | Проверь, что он работает на симуляторе и реальном устройстве |
| App Store содержит упоминания других платформ ("Доступно на Android") | Удали такие упоминания из описания iOS |
| Версия 1.0.0, но в "What's New" пусто | Заполни "What's New": "Initial release" |

**Время рассмотрения**: 1-3 дня обычно, иногда до недели. Apple
показывает статус: Waiting for Review → In Review → Pending Developer
Release / Approved / Rejected.

**Если отказ**:
1. Прочитай причину в Resolution Center
2. Исправь, **не отвечай долго** — делай новую сборку
3. Загрузи новую сборку в TestFlight, прикрепи как новую версию
4. Resubmit
5. Повторное рассмотрение обычно быстрее (1-2 дня)

**После одобрения**:
- Можешь выбрать **Manual Release** (нажмёшь "Release" сам, когда готов)
- Или **Automatic** — выйдет в App Store сразу после одобрения
- Релиз = все могут скачать через несколько часов

**Definition of Done**:
- [ ] Submit в App Store Review выполнен
- [ ] Прошёл review успешно (с первой или второй попытки)
- [ ] Релиз 1.0.0 опубликован в App Store
- [ ] Поделился новостью в Android-приложении/каналах:
  «LocoDriver теперь в App Store!»

---

## Реалистичная оценка времени

| Этап | Срок | Что зависит |
|---|---|---|
| **A. Apple Developer** (параллельно) | 1-7 дней | оплата + проверка Apple |
| **B. VK Developer iOS** (параллельно) | 30 минут | твоя ручная работа |
| **C. Privacy Policy** (параллельно) | 1-2 часа | твоя ручная работа |
| Этап 0 (Discovery) | ✅ выполнено | — |
| Этап 1 (Чистка + тесты) | Дни 1-2 | — |
| Этап 2 (5 задач: формы, экран блокировки) | Дни 3-7 | Этап 1 |
| Этап 3 (3 новых экрана) | Дни 8-12 | Этап 2 |
| Этап 4 (VK ID + Apple Sign In + серверная) | Дни 13-16 | **серверная задача 4.3** |
| Этап 5.1-5.2 (Тесты) | Дни 17-18 | Этап 4 |
| Этап 5.3 (TestFlight beta) | Дни 19-25 (5-7 дней beta) | Этап 5.2 |
| Этап 5.4 (App Store metadata) | Параллельно с TestFlight | — |
| Этап 5.5 (Submit + Review) | Дни 26-30 (Apple Review 1-7 дней) | Всё выше |

**Итого до публичного релиза в App Store: ~4-5 недель от сегодня.**

⚠️ Если что-то задерживает критичный путь:
- **Apple Developer не активен на день 17-18** → блокирует TestFlight.
  Поэтому оплачивай **сегодня**.
- **Серверная задача 4.3 (Sign in with Apple)** не сделана к Этапу 4 →
  блокирует задачу 4.4. Делать в отдельной сессии параллельно с
  Этапами 1-3.
- **Privacy Policy не готов к Submit** → блокирует Этап 5.5.
  Подготовь сразу.
- **URL для оформления подписки** на сайте должен быть готов к Этапу 2
  (задача 2.6). Если ещё нет страницы `https://locodriver.ru/subscribe`
  или подобной — сделай заранее.

Если идти быстрее — рискуешь крэшами в проде. Если идти медленнее —
Android-аудитория ждёт.

---

## Критичные точки контроля

Перед каждым этапом проверяй:

**Перед Этапом 1**:
- [ ] Apple Developer Program оплачен (хотя бы заявка отправлена)
- [ ] Privacy Policy опубликован
- [ ] iOS-приложение в VK Developer создано (для будущей задачи 4.2)

**Перед Этапом 2**:
- [ ] iOS-приложение собирается без ошибок после чистки
- [ ] Существующая функциональность (Home, FormView, авторизация, sync)
      работает
- [ ] Apple Developer Program **активен** (получил подтверждение от Apple)

**Перед Этапом 3**:
- [ ] Все 5 экранов из Этапа 2 (Loco, Train, Passenger, Settings, Profile)
      работают
- [ ] Локальные тесты проходят

**Перед Этапом 4**:
- [ ] Все 4 экрана из Этапа 3 (WorkSchedule, Search, AllRoutes, Salary)
      работают
- [ ] Sync с сервером стабильный
- [ ] **Серверная задача 4.3 (Sign in with Apple)** выполнена в отдельной
      сессии с серверным репозиторием

**Перед Этапом 5**:
- [ ] VK ID работает
- [ ] Sign in with Apple работает
- [ ] Все основные функции протестированы вручную
- [ ] Нет известных крэшей

**Перед Submit**:
- [ ] TestFlight beta минимум 5-7 дней без серьёзных проблем
- [ ] Все скриншоты, описания, Privacy Policy готовы
- [ ] Demo-аккаунт для App Review создан и протестирован
- [ ] App Privacy декларация заполнена

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
- 🟡 Push-уведомления
- 🟡 Возможно — App Store IAP (если/когда Apple восстановит платежи в РФ
  и/или при выходе на международный рынок)
- 🟡 Dark Mode
- 🟡 Dynamic Type (доступность шрифтов)
- 🟡 iPad layout
- 🟡 Сделать `AllRoutesView` через отдельный `AllRoutesIosViewModel`
  (сейчас MVP с переиспользованием HomeViewModelWrapper)
- 🟢 Куча мелких багов из API reference (см. 31_API_REFERENCE.md)
- 🟢 Поле break (timeStartBreak/timeEndBreak) в FormView — отображать
  если settings.isShowBreak == true

---

## Ссылки

- `CLAUDE.md` — правила работы Claude Code
- `CODEBASE.md` — структура кодовой базы
- `31_API_REFERENCE.md` — контракт с сервером
- `SECURITY_ACTION_PLAN.md` — оставшиеся задачи безопасности
- `02_KMP_MIGRATION_PLAN.md` — выбор библиотек и архитектура

В Project knowledge на claude.ai — все эти документы есть.
