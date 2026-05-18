# Контекст: новый функционал «Нормы времени» для Android

## О чём речь

В PWA-версии приложения «Машинист» реализован новый функционал которого
**ещё нет в Android**. Задача этого чата — реализовать то же самое в Android.

Функционал состоит из двух частей:
1. **Справочники** — серии локомотивов и станции с нормами времени
2. **Шторка ввода времени** — при заполнении приёмки/сдачи локомотива

---

## Бизнес-логика

Машинист приходит на явку и принимает локомотив. Этот процесс состоит из
нескольких этапов с нормативным временем:

### Приёмка локомотива
```
Явка (timeStartWork)
  ↓ [норма станции: appearanceToStartMin]
Начало приёмки (timeStartOfAcceptance)
  ↓ [норма серии: acceptanceDurationMin]
Окончание приёмки (timeEndOfAcceptance)
  ↓ [норма станции: endToBarrierMin]
Выход на КП (timeBarrierOut) — контрольный пункт, локомотив выезжает на станцию
```

### Сдача локомотива
```
Заход на КП (timeBarrierIn) — пользователь вводит сам
  ↓ [норма станции: barrierToStartMin]
Начало сдачи (timeStartOfDelivery)
  ↓ [норма серии: deliveryDurationMin]
Окончание сдачи (timeEndOfDelivery)
  ↓ [норма станции: endToWorkEndMin]
Окончание работы (timeEndWork)
```

**Ключевое:** при заполнении сдачи пользователь вводит только **Заход на КП**,
остальное рассчитывается по нормам кнопкой «По нормам».

Нормы бывают двух видов:
- **Норма серии** — зависит от серии локомотива (ВЛ80с, 2ТЭ10М и т.д.)
  Хранит: длительность приёмки и сдачи в минутах
- **Норма станции** — зависит от станции (Лянгасово, Киров и т.д.)
  Хранит: 4 интервала в минутах

---

## Новые поля в модели Locomotive

В Android модель `Locomotive.kt` нужно добавить 4 новых поля:

```kotlin
data class Locomotive(
    // ... существующие поля ...

    // НОВЫЕ ПОЛЯ (PWA + Android):
    var timeBarrierOut: Long? = null,        // Выход на КП (приёмка), UTC ms
    var timeBarrierIn: Long? = null,         // Заход на КП (сдача), UTC ms
    var acceptanceStationId: String? = null, // UUID станции приёмки
    var deliveryStationId: String? = null,   // UUID станции сдачи
)
```

Эти поля уже добавлены в бэкенд (миграция 006). Android должен их
сохранять и отправлять на сервер при синхронизации.

---

## Новые сущности (справочники)

### LocomotiveSeries (серия локомотива)

```kotlin
data class LocomotiveSeries(
    val seriesId: String = generateId(),
    val name: String,                           // "ВЛ80с", "2ТЭ10М"
    val type: LocoType,                         // ELECTRIC | DIESEL
    val acceptanceDurationMin: Int? = null,     // длительность приёмки, мин
    val deliveryDurationMin: Int? = null,       // длительность сдачи, мин
    val updatedAt: Long = System.currentTimeMillis()
)
```

### StationNorm (станция с нормами)

```kotlin
data class StationNorm(
    val stationId: String = generateId(),
    val name: String,                           // "Лянгасово"
    val appearanceToStartMin: Int? = null,      // явка → начало приёмки
    val endToBarrierMin: Int? = null,           // конец приёмки → КП
    val barrierToStartMin: Int? = null,         // КП → начало сдачи
    val endToWorkEndMin: Int? = null,           // конец сдачи → конец работы
    val updatedAt: Long = System.currentTimeMillis()
)
```

---

## Бэкенд API (уже готов на сервере)

Сервер: `https://api.locodriver.ru`
Аутентификация: `Authorization: Bearer <jwt_token>`

### Серии локомотивов

```
GET  /v1/norma_time/locomotives/
     → List<NormaTimeLocomotiveResponse>
     → [] если нет данных (не 404)

POST /v1/norma_time/locomotives/
     body: List<NormaTimeLocomotiveResponse>
     → {"status_code": 200, "content": "NormaTimeLocomotive сохранены"}
     Стратегия: FULL REPLACE — удаляет все старые, вставляет новые
```

### Станции

```
GET  /v1/norma_time/stations/
     → List<NormaTimeStationResponse>
     → [] если нет данных

POST /v1/norma_time/stations/
     body: List<NormaTimeStationResponse>
     → {"status_code": 200, "content": "NormaTimeStation сохранены"}
     Стратегия: FULL REPLACE
```

### Формат данных (JSON)

```json
// NormaTimeLocomotiveResponse
{
    "seriesId": "dc9d73b5-a1b5-4de5-ace0-f53e6dff6bae",
    "name": "ВЛ80с",
    "type": "ELECTRIC",
    "acceptanceDurationMin": 40,
    "deliveryDurationMin": 50,
    "updatedAt": 1778966466007.0
}

// NormaTimeStationResponse
{
    "stationId": "30b924cc-c6d1-4584-8cf9-4b1ed8f26c8e",
    "name": "Лянгасово",
    "appearanceToStartMin": 10,
    "endToBarrierMin": 4,
    "barrierToStartMin": 5,
    "endToWorkEndMin": 14,
    "updatedAt": 1779032766922.0
}
```

### Поля КП в Locomotive (уже в существующем API)

При POST /v1/route/ в объекте локомотива теперь принимаются и возвращаются:
```json
{
    "locoId": "...",
    "timeStartOfAcceptance": 1779037500000,
    "timeEndOfAcceptance": 1779037980000,
    "timeBarrierOut": 1779038280000,
    "timeBarrierIn": 1779060000000,
    "timeStartOfDelivery": 1779060360000,
    "timeEndOfDelivery": 1779063360000,
    "acceptanceStationId": "uuid-станции-приёмки",
    "deliveryStationId": "uuid-станции-сдачи"
}
```

Если Android не передаёт эти поля — они будут NULL на сервере. Это безопасно,
обратная совместимость обеспечена.

---

## Стратегия синхронизации

Обе сущности используют **full replace** (как ReleaseDay):
- Нет локальных данных → GET с сервера → сохранить локально
- Есть локальные данные → POST на сервер (полная замена)

Синхронизация запускается в рамках общего `syncAll()`.

---

## Что реализовано в PWA (для справки)

### Справочники

**Хранение:** Dexie (IndexedDB) таблицы `normaTimeLocomotives` и `normaTimeStations`

**UI экраны:**
- `Настройки → Серии локомотивов` — список серий, группированный по типу (электро/дизель)
- `Настройки → Станции` — список станций, группированный (с нормами / без норм)
- Редактор серии: название, тип (Электровоз/Тепловоз), приёмка/сдача (степпер, шаг 5 мин)
- Редактор станции: название, 4 интервала (степпер, шаг 1 мин)

### Шторка времени приёмки/сдачи

Открывается из формы локомотива при тапе на «Приёмка» или «Сдача».

**Структура шторки:**
```
[Отмена]     [Приёмка / Сдача]     [Готово]

[🚂 ВЛ80с  ›]    [📍 Лянгасово  ›]    [⚡ По нормам]

Явка           из маршрута    07:45  ← locked
Начало приёмки +5 мин         07:50  ← редактируемое
Окончание      +40 мин        08:30  ← редактируемое
Выход на КП    +5 мин         08:35  ← редактируемое, highlighted

[🚂 Сохранить норму серии ВЛ80с]
[📍 Сохранить норму станции Лянгасово]
```

**Логика «По нормам»:**
- Приёмка: Явка + appearanceToStartMin → Начало; + acceptanceDurationMin → Конец; + endToBarrierMin → КП
- Сдача: Заход на КП (ввод пользователя) + barrierToStartMin → Начало; + deliveryDurationMin → Конец; + endToWorkEndMin → Конец работы

**Дельты:** каждое поле показывает разницу от предыдущего (`+5 мин`, `+40 мин`)

**Шторка выбора серии:** список из справочника, поиск, кнопка «+ Добавить серию»
**Шторка выбора станции:** список с группами (часто используемые / без норм), поиск

**Сохранение норм:** кнопки активны только если текущие данные отличаются от сохранённых в справочнике. При нажатии — обновляют справочник.

**Станция сохраняется в локомотиве:** `acceptanceStationId` / `deliveryStationId` — UUID из справочника.

---

## Что нужно сделать в Android

1. **Добавить модели** `LocomotiveSeries` и `StationNorm`

2. **Добавить локальное хранение** (SQLDelight таблицы)

3. **Добавить API клиенты** для 4 новых эндпоинтов

4. **Добавить синхронизацию** в `syncAll()` — full replace по паттерну ReleaseDay

5. **Добавить поля в Locomotive:** `timeBarrierOut`, `timeBarrierIn`,
   `acceptanceStationId`, `deliveryStationId` — в модель, SQLDelight, маппер, API

6. ✅ **UI справочников** в разделе Настройки

7. ✅ **Шторка времени** в форме локомотива — по описанию выше

---

## Технические детали бэкенда

- Сервер: FastAPI + PostgreSQL 16 на Selectel
- Таблицы: `norma_time_locomotive`, `norma_time_station`
- Миграции: 005 и 006 применены на проде
- Android-пользователи не затронуты (все новые поля nullable, дефолт NULL)
- `updatedAt` возвращается как `float` (например `1779032766922.0`) — учесть при парсинге
