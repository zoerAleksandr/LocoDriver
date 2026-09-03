# План защиты локальных маршрутов и диагностики обновлений

Статус: проектирование  
Платформа первого этапа: Android  
Затрагиваемые репозитории:

- клиент: `/Users/zoer/AndroidStudioProjects/LocoDriver`;
- сервер и кабинет администратора: `/Users/zoer/Downloads/proxy-parser`.

## 1. Цель

Сделать потерю локальных маршрутов максимально маловероятной, а любую миграцию,
синхронизацию и операцию удаления — наблюдаемой и объяснимой.

После реализации система должна отвечать на вопросы:

1. Сколько установок обновилось на каждую версию приложения?
2. С какой версии и схемы БД они обновлялись?
3. Сколько миграций завершилось успешно, с ошибкой или откатом?
4. Сохранилось ли количество маршрутов и дочерних записей после миграции?
5. Почему конкретный маршрут оказался в корзине или был физически удалён?
6. Создавался ли backup и удалось ли восстановить БД?
7. Есть ли массовая проблема у определённой версии приложения, Android или модели устройства?

## 2. Ограничения и принципы

- Сервер и Android находятся в продакшене: существующие API не меняются несовместимым образом.
- Диагностический API добавляется новым эндпоинтом и не влияет на старые клиенты.
- Полный `SyncData`, заметки, станции, номера поездов/локомотивов, email, `vk_id`, JWT и пароли не отправляются.
- Резервные копии `Route.db` не отправляются автоматически.
- Физическое удаление маршрута невозможно без явно указанной причины.
- Пользовательское удаление сначала перемещает маршрут в локальную корзину.
- Содержимое корзины хранится 30 суток или до явной очистки пользователем.
- Все миграции выполняются только после создания и проверки backup.
- Если backup создать нельзя, миграция не начинается.
- Диагностика не должна блокировать основной UI и синхронизацию: события отправляются с повтором через WorkManager.

## 3. Общая архитектура

```text
Android
  ├─ Route.db
  │   ├─ BasicData + дочерние таблицы
  │   └─ RouteEvent (локальный журнал)
  ├─ route_backups/
  │   └─ проверенные копии Route.db
  ├─ DataSafetyManager
  │   ├─ backup → migrate → validate → accept/rollback
  │   └─ формирование диагностического кода
  ├─ RouteDeletionService
  │   ├─ moveToTrash(...)
  │   ├─ restoreFromTrash(...)
  │   └─ purgeRoute(..., mandatoryReason)
  ├─ DiagnosticsOutbox
  └─ WorkManager ───────── POST /v1/diagnostics/events
                                │
Server/PostgreSQL               │
  ├─ diagnostic_installation ◀──┘
  ├─ diagnostic_event
  ├─ агрегирующие запросы
  └─ /admin/diagnostics
      ├─ обзор обновлений
      ├─ миграции и откаты
      ├─ ошибки и аномалии
      └─ карточка установки
```

## 4. Изменения локальной БД Android

Изменение схемы должно быть аддитивным. Не пересоздавать `BasicData` и не выполнять
`DROP TABLE BasicData`.

### 4.1. Новые поля `BasicData`

```sql
ALTER TABLE BasicData ADD COLUMN deletedAt INTEGER DEFAULT NULL;
ALTER TABLE BasicData ADD COLUMN deletionReason TEXT DEFAULT NULL;
ALTER TABLE BasicData ADD COLUMN remoteDeletionPending INTEGER NOT NULL DEFAULT 0;
```

Семантика:

- `isDeleted = 0` — обычный маршрут;
- `isDeleted = 1` — маршрут находится в корзине;
- `deletedAt` — время помещения в корзину, UTC milliseconds;
- `deletionReason` — причина помещения в корзину;
- `remoteDeletionPending = 1` — серверный `DELETE` ещё не подтверждён;
- `remoteDeletionPending = 0` — удаление на сервере завершено либо сервер о маршруте не знал.

Существующие строки получают `NULL`, `NULL`, `0`; их содержимое не изменяется.

### 4.2. Локальный журнал `RouteEvent`

```sql
CREATE TABLE IF NOT EXISTS RouteEvent (
    eventId TEXT NOT NULL PRIMARY KEY,
    routeId TEXT,
    eventType TEXT NOT NULL,
    source TEXT NOT NULL,
    reason TEXT,
    occurredAt INTEGER NOT NULL,
    appVersionCode INTEGER NOT NULL,
    dbSchemaVersion INTEGER NOT NULL,
    wasSynchronized INTEGER,
    isSynchronized INTEGER,
    wasDeleted INTEGER,
    isDeleted INTEGER,
    localUpdatedAt INTEGER,
    serverUpdatedAt INTEGER,
    metadataJson TEXT
);

CREATE INDEX IF NOT EXISTS idx_route_event_occurred_at
    ON RouteEvent(occurredAt);

CREATE INDEX IF NOT EXISTS idx_route_event_route_id
    ON RouteEvent(routeId);
```

У `RouteEvent.routeId` намеренно нет внешнего ключа: журнал должен сохраниться после
физического удаления маршрута.

`metadataJson` содержит только ограниченный набор технических значений: counts,
код результата, длительность. Доменные данные маршрута туда не записываются.

### 4.3. Локальная outbox диагностики

Рекомендуется отдельная таблица, а не попытка отправлять событие прямо из операции:

```sql
CREATE TABLE IF NOT EXISTS DiagnosticOutbox (
    eventId TEXT NOT NULL PRIMARY KEY,
    payloadJson TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    attemptCount INTEGER NOT NULL DEFAULT 0,
    nextAttemptAt INTEGER NOT NULL,
    lastErrorCode TEXT
);
```

После HTTP 2xx событие удаляется из outbox. Повторная доставка безопасна благодаря
уникальному `eventId` на сервере.

## 5. Обязательная причина физического удаления

### 5.1. Проблема текущего API

Текущий вызов `removeRoute(route)` без контекста физически удаляет `BasicData`, а
SQLite каскадно удаляет дочерние записи. По сигнатуре невозможно понять, почему
удаление разрешено.

### 5.2. Новый API

Низкоуровневое физическое удаление скрывается внутри data/domain слоя:

```kotlin
enum class PhysicalDeletionReason {
    TRASH_RETENTION_EXPIRED,
    USER_EMPTIED_TRASH,
    SHARED_PREVIEW_DISCARDED,
    DIAGNOSTIC_ROLLBACK_CLEANUP,
}

internal fun purgeRoute(
    routeId: String,
    reason: PhysicalDeletionReason,
): Flow<ResultState<Unit>>
```

Причина обязательна на уровне компиляции. Вызов без `reason` невозможен.

Перед `DELETE FROM BasicData` выполняется транзакция:

1. Проверить, что причина разрешена для текущего состояния.
2. Записать `PHYSICAL_DELETE_REQUESTED` в `RouteEvent`.
3. Физически удалить маршрут.
4. Записать `PHYSICAL_DELETE_COMPLETED` без FK на маршрут.
5. При исключении откатить транзакцию и записать ошибку после rollback.

### 5.3. Разрешённые переходы

| Событие | Действие | Физическое удаление |
|---|---|---|
| Пользователь нажал «Удалить» | Переместить в корзину | Нет |
| Сервер не вернул clean-маршрут | Переместить в корзину с подтверждением действующих правил | Нет |
| Сервер подтвердил DELETE | Оставить в корзине, снять `remoteDeletionPending` | Нет |
| Пользователь восстановил | Снять `isDeleted`, пометить unsynchronized | Нет |
| Прошло 30 суток | Purge с `TRASH_RETENTION_EXPIRED` | Да |
| Пользователь очистил корзину | Purge с `USER_EMPTIED_TRASH` | Да |
| Пользователь удалил shared-preview | Purge с `SHARED_PREVIEW_DISCARDED` либо также корзина | Да/по выбранному UX |

### 5.4. Защита на уровне кода

- Удалить публичный `RouteUseCase.removeRoute(route)`.
- Удалить публичный `RouteRepository.remove(route)`.
- Все старые call sites перевести либо на `moveToTrash`, либо на `purgeRoute` с enum.
- Запретить прямой вызов `basicDataQueries.delete` вне одного repository-метода.
- Добавить архитектурный тест/поиск, который падает при появлении новых прямых удалений.

## 6. Корзина маршрутов

### 6.1. Расположение

Android: `Настройки → Корзина маршрутов`.

Добавить `SettingsSubScreen.TRASH` и карточку на hub Настроек:

- заголовок «Корзина маршрутов»;
- подзаголовок «Удалённые маршруты хранятся 30 дней»;
- badge с количеством;
- дата ближайшего автоматического удаления.

### 6.2. Экран корзины

Для каждого маршрута показать только безопасную для UI сводку:

- дата и время работы;
- номер маршрута, если есть;
- дата удаления;
- «Удалено пользователем» / «Удалено на другом устройстве»;
- сколько дней осталось до очистки.

Действия:

- «Восстановить»;
- «Удалить навсегда» с отдельным подтверждением;
- верхнее действие «Очистить корзину» с количеством элементов и подтверждением.

### 6.3. Восстановление

В одной транзакции:

1. `isDeleted = false`;
2. `deletedAt = NULL`;
3. `deletionReason = NULL`;
4. `remoteDeletionPending = 0`;
5. `isSynchronized = false`;
6. `remoteRouteId = NULL`, если серверное удаление уже подтверждено;
7. `updatedAt = now`;
8. событие `ROUTE_RESTORED`.

На следующей синхронизации маршрут снова отправляется целиком.

### 6.4. Автоматическая очистка

WorkManager запускается не чаще одного раза в сутки:

```text
deletedAt <= now - 30 days
AND remoteDeletionPending = false
```

Маршрут с незавершённым серверным удалением не очищается, пока состояние не будет
разрешено либо пока пользователь явно не подтвердит локальную очистку.

## 7. Локальный журнал событий

### 7.1. События маршрута

- `ROUTE_CREATED`
- `ROUTE_SAVED`
- `ROUTE_MARKED_UNSYNCHRONIZED`
- `ROUTE_UPLOAD_STARTED`
- `ROUTE_UPLOAD_SUCCEEDED`
- `ROUTE_UPLOAD_FAILED`
- `ROUTE_REMOTE_RECEIVED`
- `ROUTE_REMOTE_APPLIED`
- `ROUTE_MOVED_TO_TRASH`
- `ROUTE_REMOTE_DELETE_STARTED`
- `ROUTE_REMOTE_DELETE_SUCCEEDED`
- `ROUTE_REMOTE_DELETE_FAILED`
- `ROUTE_RESTORED`
- `PHYSICAL_DELETE_REQUESTED`
- `PHYSICAL_DELETE_COMPLETED`
- `PHYSICAL_DELETE_FAILED`

### 7.2. События приложения и БД

- `APP_FIRST_LAUNCH`
- `APP_UPDATED`
- `DB_BACKUP_STARTED`
- `DB_BACKUP_SUCCEEDED`
- `DB_BACKUP_FAILED`
- `DB_MIGRATION_STARTED`
- `DB_MIGRATION_SUCCEEDED`
- `DB_MIGRATION_FAILED`
- `DB_VALIDATION_FAILED`
- `DB_ROLLBACK_STARTED`
- `DB_ROLLBACK_SUCCEEDED`
- `DB_ROLLBACK_FAILED`
- `SYNC_ANOMALY_BLOCKED`

### 7.3. Ограничение размера

- хранить не более 90 дней;
- дополнительно ограничить, например, 5 000 событий;
- события migration/rollback/physical-delete хранить дольше обычных;
- очистка журнала сама записывает агрегированное событие без маршрутных данных.

## 8. Backup `Route.db` перед миграцией

### 8.1. Когда создавать

На первом запуске новой версии, если `PRAGMA user_version` меньше целевой версии
`RouteDatabase.Schema.version`.

### 8.2. Алгоритм

1. Не создавать основной SQLDelight driver.
2. Открыть SQLite в контролируемом режиме.
3. Снять pre-migration snapshot:
   - `user_version`;
   - `PRAGMA integrity_check`;
   - список route IDs;
   - counts каждой таблицы;
   - count unsynchronized и deleted.
4. Выполнить `PRAGMA wal_checkpoint(FULL)`.
5. Закрыть соединение.
6. Проверить свободное место: минимум размер БД × 2 плюс запас.
7. Скопировать основной файл во внутренний каталог:

   ```text
   files/route_backups/Route_before_schema_12_<timestamp>.db
   ```

8. Открыть копию read-only и проверить `integrity_check=ok` и counts.
9. Только после этого начать миграцию оригинала.
10. Миграцию выполнять транзакционно и без destructive operations.
11. Выполнить post-migration validation.
12. При успехе пометить backup проверенным и продолжить запуск.
13. При ошибке закрыть оригинал, переместить неудачную БД в quarantine и вернуть backup.

### 8.3. Проверка после миграции

Обязательные условия:

- `PRAGMA integrity_check = ok`;
- новая версия схемы установлена;
- все обязательные таблицы и столбцы существуют;
- все прежние route IDs существуют;
- count `BasicData` не уменьшился;
- counts дочерних таблиц не уменьшились без явно предусмотренной миграционной причины;
- count unsynchronized не уменьшился;
- нет orphan-записей.

### 8.4. Поведение при rollback

Если validation не проходит:

1. Остановить инициализацию route repository.
2. Вернуть backup на место `Route.db`.
3. Проверить восстановленную БД.
4. Не запускать синхронизацию в этом процессе.
5. Поставить диагностическое событие в outbox или отправить его из отдельного безопасного хранилища.
6. Показать recovery-экран с диагностическим кодом.
7. На следующем запуске не повторять миграцию бесконечно: использовать счётчик попыток и recovery mode.

Новая версия приложения не должна пытаться полноценно работать со старой схемой после
rollback. Допустим recovery mode: экспорт диагностики, повторная безопасная попытка и
инструкция обратиться в поддержку.

### 8.5. Хранение backup

- последние 3 успешных backup;
- не дольше 30 дней;
- backup после rollback не удалять автоматически до успешной последующей миграции;
- хранить только во внутреннем каталоге приложения;
- исключить автоматическую отправку в аналитику;
- включать в ручной диагностический архив только после отдельного согласия.

## 9. Диагностический код установки

При первой установке создать случайный UUID `installationId`. Он не меняется при
обновлении, но исчезает при очистке данных/переустановке.

Пользователю показывается короткий код, вычисленный локально/сервером, например:

```text
A7F4-92CD
```

В кабинете поиск выполняется по этому коду. Email для диагностики не нужен.

Ограничение: `installationId` идентифицирует установку, а не человека. Один пользователь
с двумя устройствами считается двумя установками.

## 10. Диагностический API

Добавляется новый обратносуместимый endpoint:

```text
POST /v1/diagnostics/events
```

### 10.1. Запрос

```json
{
  "schemaVersion": 1,
  "events": [
    {
      "eventId": "2e67c2ce-5777-49ae-a786-b8c24096d32f",
      "installationId": "9f655f3e-28b2-41db-b728-e710a8cb87f8",
      "diagnosticCode": "A7F4-92CD",
      "eventType": "DB_MIGRATION_SUCCEEDED",
      "occurredAt": 1787961000000,
      "platform": "android",
      "appVersionCode": 81,
      "appVersionName": "3.0.4",
      "previousAppVersionCode": 80,
      "androidSdk": 35,
      "deviceManufacturer": "Samsung",
      "deviceModel": "SM-S928B",
      "dbSchemaFrom": 11,
      "dbSchemaTo": 12,
      "status": "success",
      "failureStage": null,
      "errorCode": null,
      "backupCreated": true,
      "backupRestored": false,
      "integrityBefore": "ok",
      "integrityAfter": "ok",
      "routesBefore": 184,
      "routesAfter": 184,
      "unsyncedBefore": 3,
      "unsyncedAfter": 3,
      "deletedBefore": 0,
      "deletedAfter": 0,
      "childrenBefore": {
        "locomotives": 170,
        "trains": 146,
        "passengers": 22,
        "otherWorks": 4,
        "partners": 9
      },
      "childrenAfter": {
        "locomotives": 170,
        "trains": 146,
        "passengers": 22,
        "otherWorks": 4,
        "partners": 9
      },
      "durationMs": 438
    }
  ]
}
```

Pydantic-схемы используют `extra = "ignore"` для forward compatibility.

### 10.2. Ответ

```json
{
  "accepted": ["2e67c2ce-5777-49ae-a786-b8c24096d32f"],
  "duplicates": [],
  "rejected": []
}
```

HTTP-коды:

- `200` — пакет обработан, включая дубликаты;
- `400/422` — неверная схема;
- `413` — превышен размер пакета;
- `429` — rate limit;
- `500` — клиент сохраняет outbox и повторяет позже.

### 10.3. Авторизация и защита от мусора

Чтобы считать обновления также у незалогиненных пользователей, endpoint принимает
анонимные события. Для MVP:

- максимум 20 событий в пакете;
- максимум 32 КБ запроса;
- строгий allow-list event types и полей;
- rate limit по installationId и IP;
- уникальность `eventId`;
- допустимый диапазон времени;
- нормализация manufacturer/model/version;
- неизвестные поля игнорируются;
- значения строк ограничены по длине;
- stack trace целиком не принимается; только стабильный `errorCode` и Sentry event ID.

Для более высокой достоверности статистики позже добавить Play Integrity. Без attestation
анонимная телеметрия полезна операционно, но не является финансово точной аналитикой.

Если пользователь авторизован, сервер может связать событие с `current_user.id`, но не
возвращает и не показывает email в общей таблице без явного перехода в карточку поддержки.

## 11. Серверная модель PostgreSQL

### 11.1. `diagnostic_installation`

```text
id                      UUID PK
installation_hash       TEXT UNIQUE NOT NULL
diagnostic_code         TEXT UNIQUE NOT NULL
first_seen_at           TIMESTAMPTZ NOT NULL
last_seen_at            TIMESTAMPTZ NOT NULL
last_app_version_code   INTEGER
last_app_version_name   TEXT
last_android_sdk        INTEGER
manufacturer            TEXT
model                   TEXT
last_db_schema_version  INTEGER
user_id                 UUID NULL
last_migration_status   TEXT NULL
has_unresolved_problem  BOOLEAN NOT NULL DEFAULT FALSE
```

На сервере рекомендуется хранить HMAC/хеш `installationId`, а не исходное значение.

### 11.2. `diagnostic_event`

```text
id                    UUID PK (= client eventId)
installation_id       UUID FK diagnostic_installation
event_type            TEXT NOT NULL
occurred_at            TIMESTAMPTZ NOT NULL
received_at            TIMESTAMPTZ NOT NULL
app_version_code       INTEGER NOT NULL
app_version_name       TEXT
previous_version_code  INTEGER
db_schema_from         INTEGER
db_schema_to           INTEGER
status                 TEXT
failure_stage          TEXT
error_code             TEXT
sentry_event_id        TEXT
backup_created         BOOLEAN
backup_restored        BOOLEAN
integrity_before       TEXT
integrity_after        TEXT
routes_before          INTEGER
routes_after           INTEGER
unsynced_before        INTEGER
unsynced_after         INTEGER
deleted_before         INTEGER
deleted_after          INTEGER
children_before        JSONB
children_after         JSONB
duration_ms             INTEGER
metadata                JSONB
```

Индексы:

- `(event_type, received_at DESC)`;
- `(app_version_code, event_type)`;
- `(status, received_at DESC)`;
- `(installation_id, occurred_at DESC)`;
- partial index `WHERE status IN ('failed', 'rollback')`.

Retention:

- подробные успешные события: 180 дней;
- ошибки и rollback: минимум 2 года;
- дневные агрегаты: бессрочно либо по политике проекта.

## 12. Как считать обновившихся пользователей

### 12.1. Основная метрика

Уникальные установки, приславшие один из событий:

- `APP_UPDATED`;
- `APP_FIRST_LAUNCH`, если версия новая для установки;
- `DB_MIGRATION_*`.

Событие `APP_UPDATED` отправляется один раз на пару
`installationId + appVersionCode`. Повтор не увеличивает счётчик.

### 12.2. Метрики кабинета

Для каждой версии:

- уникальных установок обновилось;
- уникальных авторизованных аккаунтов обновилось;
- с каких версий пришли;
- успешных миграций;
- миграций без изменения схемы;
- ошибок миграции;
- rollback успешен;
- rollback не удался;
- recovery mode активен;
- доля успеха;
- медианная и p95 длительность миграции;
- количество установок, не приславших финальный результат после `MIGRATION_STARTED`.

Последний пункт выявляет краш/принудительное закрытие посередине миграции.

### 12.3. Ограничения интерпретации

- очистка данных создаёт новую установку;
- переустановка создаёт новую установку;
- одно лицо с несколькими устройствами учитывается несколько раз;
- устройство без сети пришлёт событие позже;
- старые версии до внедрения endpoint не видны.

В UI использовать формулировку «установок», а не «людей», кроме отдельной метрики
авторизованных уникальных аккаунтов.

## 13. Кабинет администратора

Текущий кабинет расположен на `/admin`, управление ценами — `/admin/tariffs`.
Добавить кнопку «Диагностика» в общий header рядом с «Тарифы».

### 13.1. `/admin/diagnostics`

Верхние карточки за выбранный период:

- «Обновилось установок»;
- «Авторизованных аккаунтов»;
- «Миграции успешны»;
- «Ошибки миграции»;
- «Откаты выполнены»;
- «Нерешённые проблемы».

Фильтры:

- период;
- версия приложения;
- предыдущая версия;
- Android SDK;
- производитель/модель;
- версия схемы from/to;
- статус `success / failed / rollback / interrupted`;
- только нерешённые.

### 13.2. Таблица версий

Колонки:

- versionCode / versionName;
- дата первого события;
- обновилось установок;
- успешных миграций;
- ошибок;
- rollback;
- success rate;
- interrupted;
- p95 duration.

Клик открывает `/admin/diagnostics/version/{version_code}`.

### 13.3. Последние проблемы

Таблица:

- время;
- диагностический код;
- версия приложения;
- переход схемы;
- устройство;
- стадия ошибки;
- errorCode;
- backup создан;
- rollback выполнен;
- изменение routes/unsynced counts;
- статус расследования.

Цвета:

- красный — данные не восстановлены;
- оранжевый — rollback выполнен, требуется анализ;
- жёлтый — подозрительное расхождение заблокировано;
- зелёный — успешная миграция.

### 13.4. Карточка установки

`/admin/diagnostics/install/{diagnostic_code}`:

- история версий приложения;
- история версий Route.db;
- timeline событий;
- counts до/после;
- ошибки и Sentry event ID;
- результаты backup/rollback;
- кнопка «Пометить решённым» с CSRF;
- внутренний комментарий администратора;
- экспорт JSON без маршрутного содержимого.

### 13.5. Прерывание миграции

Фоновая серверная задача помечает `DB_MIGRATION_STARTED` как `interrupted`, если для
той же установки и версии в течение заданного окна, например 24 часов, не пришло
ни `SUCCEEDED`, ни `FAILED`, ни `ROLLBACK_SUCCEEDED`.

## 14. Диагностический экспорт на Android

`Настройки → Диагностика → Сформировать отчёт`.

Архив без согласия на данные содержит:

- диагностический код;
- версию приложения/Android/устройства;
- версии локальных БД;
- `integrity_check`;
- агрегированные counts;
- журнал операций без доменных полей;
- последние коды ошибок синхронизации;
- наличие и метаданные backup;
- Sentry event IDs.

Отдельный переключатель с явным предупреждением может добавить зашифрованную копию
`Route.db`. По умолчанию он выключен. Пароль архива показывается пользователю отдельно.

## 15. Изменение синхронизации для корзины

### 15.1. Пользователь удаляет маршрут

1. `moveToTrash(USER_DELETED)`.
2. Если маршрут никогда не был облачным: `remoteDeletionPending = 0`.
3. Если был облачным: `remoteDeletionPending = 1`.
4. Sync вызывает существующий `DELETE /v1/route/{id}`.
5. После 2xx/404 маршрут остаётся в корзине, `remoteDeletionPending = 0`,
   `remoteRouteId = NULL`.

### 15.2. Маршрут удалён на другом устройстве

Текущие правила определения remote deletion сохраняются, но вместо физического удаления:

```text
moveToTrash(REMOTE_DELETED)
```

Для массового расхождения подтверждение сохраняется. Unsynchronized маршрут не может
быть перемещён в корзину только из-за отсутствия в серверном ответе.

### 15.3. Очистка корзины

Очистка локальная. Серверный DELETE должен быть завершён до автоматического purge.
При явной очистке пользователь получает предупреждение о pending-элементах.

## 16. Безопасность и приватность

- Не использовать существующий `LogManager` для полного payload диагностики.
- Не записывать `SyncData` в `RouteEvent`/`DiagnosticOutbox`.
- Не отправлять route ID открытым; для серверной корреляции использовать HMAC/hash.
- Не принимать произвольный `metadataJson` без серверного allow-list.
- Админские страницы защищены существующей httpOnly-сессией и CSRF.
- События доступны только администратору.
- В privacy policy описать техническую диагностику, installation ID, модель устройства,
  версию ОС и агрегированные counts.
- Добавить пользовательский выключатель необязательной расширенной диагностики;
  критические события целостности можно обрабатывать как необходимые для безопасности
  сервиса после юридической проверки политики.

## 17. План реализации

### Этап 0. Зафиксировать контракты

- Утвердить миграцию Route.db.
- Утвердить список enum причин удаления.
- Утвердить JSON schema v1 диагностического endpoint.
- Добавить раздел в `31_API_REFERENCE.md` после реализации.
- Добавить поведение Корзины/Диагностики в `SCREEN_SPECS.md` в том же коммите с UI.

### Этап 1. Обязательная причина и журнал

- Добавить domain enums и модели событий.
- Добавить `RouteEvent` и repository.
- Централизовать физическое удаление.
- Перевести call sites.
- Добавить unit-тесты допустимых переходов и запрета purge без причины.

### Этап 2. Корзина

- Добавить поля `BasicData`.
- Реализовать move/restore/purge.
- Изменить SyncManager: remote deletion → trash.
- Добавить экран в Настройки.
- Добавить WorkManager retention cleanup.
- Тестировать восстановление полного графа дочерних данных.

### Этап 3. Backup и validator

- Реализовать snapshot counts.
- Реализовать checkpoint/copy/verify.
- Обернуть миграцию в state machine.
- Реализовать quarantine и rollback.
- Реализовать recovery mode.
- Добавить instrumented migration tests на копиях схем старых версий.

### Этап 4. Клиентская диагностика

- installation ID и diagnostic code.
- outbox.
- WorkManager retry.
- события обновления/миграции/rollback.
- локальный экран экспорта.

### Этап 5. Сервер

- Alembic migration диагностических таблиц.
- Pydantic request/response с `extra='ignore'`.
- endpoint с validation/rate limits/idempotency.
- агрегирующие DB client методы.
- retention job.
- тесты endpoint и дедупликации.

### Этап 6. Кабинет администратора

- `/admin/diagnostics` dashboard.
- страницы версии и установки.
- фильтры и пагинация.
- статусы расследования и комментарии.
- ссылки на Sentry.
- CSRF для изменяющих действий.

### Этап 7. Безопасный rollout

- Внутренний debug build с искусственными миграционными ошибками.
- Закрытый тест Google Play.
- Поэтапный rollout 5% → 20% → 50% → 100%.
- Переход к следующему этапу только при отсутствии необъяснённых rollback и потерь counts.
- Возможность сервером отключить purge/миграцию feature flag без выпуска новой версии.

## 18. Тестовая стратегия

### Unit

- каждый deletion reason;
- запрещённые переходы;
- restore;
- retention boundary ровно 30 суток;
- event deduplication;
- payload privacy allow-list;
- агрегирование уникальных установок.

### SQLDelight migration

- каждая поддерживаемая старая версия Route.db → новая;
- пустая БД;
- сотни маршрутов;
- unsynchronized маршруты;
- deleted/shared-preview маршруты;
- WAL с неподтверждёнными страницами;
- отсутствующий необязательный столбец;
- частично применённая старая миграция.

### Android instrumentation

- нехватка места до backup;
- исключение в середине миграции;
- kill process;
- повреждённая копия;
- rollback;
- повторный запуск recovery mode;
- экспорт отчёта через share sheet.

### Server

- duplicate eventId;
- oversized batch;
- invalid event type;
- rate limit;
- anonymous/authenticated event;
- dashboard counts;
- interrupted migration detection;
- admin auth/CSRF.

### End-to-end

1. Установить старую production-сборку.
2. Создать synced и unsynchronized маршруты с дочерними данными.
3. Обновить APK поверх старой версии.
4. Проверить backup, migration, counts и dashboard.
5. Удалить/восстановить маршрут.
6. Переместить время на +31 день и проверить purge reason.
7. Инъецировать ошибку миграции и проверить rollback + кабинет.

## 19. Критерии готовности

- Ни один путь обычного удаления не вызывает физический DELETE.
- Каждый физический DELETE имеет enum-причину и journal event.
- Все маршруты и дочерние записи переживают успешную миграцию.
- При искусственной ошибке исходная БД восстанавливается byte-for-byte либо логически
  эквивалентно после WAL checkpoint.
- Unsynchronized count после миграции не уменьшается.
- Корзина восстанавливает полный маршрут.
- Автоочистка не удаляет записи моложе 30 дней.
- Endpoint не принимает PII/полный SyncData.
- Повтор события не увеличивает статистику.
- Кабинет показывает update count, success/failure/rollback/interrupted по версии.
- Для каждой проблемы доступен диагностический код и timeline.

## 20. Решения, которые нужно утвердить перед кодом

1. Подтвердить аддитивную миграцию `BasicData` и новые локальные таблицы.
2. Shared-preview тоже хранить 30 дней или удалять сразу с обязательной причиной?
3. Срок хранения серверных успешных событий: 180 дней подходит?
4. Ошибки/rollback хранить 2 года или бессрочно?
5. Разрешать ли авторизованному событию связываться с `user_id` для поддержки?
6. Нужна ли отправка критической телеметрии без пользовательского opt-in после обновления privacy policy?
7. Нужен ли серверный feature flag для отключения auto-purge и миграции?

## 21. Рекомендуемый минимальный первый релиз

Чтобы не выпускать слишком много рискованных изменений одновременно:

1. Обязательная причина физического удаления + локальный журнал.
2. Backup и post-migration validation.
3. Диагностический endpoint + кабинет миграций.
4. После наблюдения за одной стабильной версией — включить корзину и 30-дневный purge.

Схему корзины можно добавить в первой миграции, но автоматический purge первое время
держать выключенным серверным feature flag. Это даст возможность проверить backup,
события и восстановление до появления нового автоматического физического удаления.
