# SCREEN_SPECS.md — эталонное описание экранов

> **Назначение.** Этот файл — единый источник правды по поведению экранов
> приложения. По нему воспроизводится **точная логика и функционал** в
> iOS-версии (SwiftUI) и PWA-версии. Здесь описано и то, **что видит
> пользователь**, и то, **где хранятся данные и как обрабатываются**.
>
> Эталон — Android-реализация (Jetpack Compose, `features/route`).

---

## ⚠️ ПРАВИЛО ОБНОВЛЕНИЯ (обязательно)

**Любое изменение кода, затрагивающее поведение описанного здесь экрана
(UI, расчёты, хранение, контракт), ОБЯЗАНО сопровождаться правкой этого
файла в том же коммите.** Если экран ещё не описан — добавить описание при
первом изменении. Расхождение кода и `SCREEN_SPECS.md` считается багом.

Чек перед коммитом: «Тронул ли я логику экрана из SCREEN_SPECS.md? Если да —
обновил ли соответствующий раздел?»

---

## 0. Общие соглашения (важно для всех платформ)

### 0.1. Время, часовой пояс, форматы

- **Время** — `Long`, миллисекунды от Unix epoch (UTC). Все поля времени в моделях
  хранят абсолютный момент; часовой пояс применяется только при отображении/вводе.
- **Часовой пояс пользователя** — `UserSettings.timeZone: Long` = смещение **в
  миллисекундах относительно Москвы (UTC+3)**, `0` = московское время. Строка
  пояса строится `getTimeZone(ms)`: смещение `ms + 10 800 000` → `"GMT+N"` (целые
  часы) или `"GMT+HH:MM"` (с минутами); отрицательное → `"GMT-…"`.
- **Пояс отображения и ввода** (`UserSettings.displayTimeZone()`, он же
  `DateAndTimeConverter.timeZoneText`): страна `KZ` → `getTimeZone(timeZone)`
  (местное время); любая другая страна (`RU`, `BY`, …) → **всегда `"GMT+3"`**,
  независимо от `timeZone`. Пикеры даты/времени получают эту же строку.
- **Пояс расчётов** (`TimeCalculationContext.from(settings)`): `localTZ =
  getTimeZone(timeZone)` — для ночных/праздничных часов; `crossMonthTZ` — для
  границ месяца у переходных маршрутов: `CrossMonthTimezone.MOSCOW` → `GMT+3`,
  `LOCAL` (по умолчанию) → `localTZ`.

  Где какой пояс используется:

  | Пояс | Значение | Где используется |
  |---|---|---|
  | пояс отображения `displayTimeZone()` | RU, BY и любая страна кроме KZ → всегда `GMT+3`; KZ → `getTimeZone(timeZone)` (местный) | вывод и ввод всех дат/времён на экранах и в пикерах; дата явки в Календаре и мастере графика (§14.1, §15.2); даты Профиля и Покупок |
  | `localTZ` | `getTimeZone(timeZone)` | ночные окна, праздничные сутки, сутки командировки (§11.2) |
  | `crossMonthTZ` | `MOSCOW` → `GMT+3`; `LOCAL` → `localTZ` | границы месяца: список маршрутов месяца, обрезка переходных маршрутов, дата смены тарифа, месяц явки для пробега (§4.1, §11.1–11.2, §13) |

  Пояс устройства в расчётах и отображении данных маршрутов не используется (места, где
  Android так делает, — возможные баги, см. §9.4, §10.2, §21).
- **Форматы дат** (в поясе отображения): время `HH:mm`; «мини-дата+время»
  `dd.MM HH:mm`; дата `dd.MM.yy`; дата+время `dd.MM.yy HH:mm` (для `0L` — «Нет данных»).
  Пустое значение → пустая строка.
  Полный год (`dd.MM.yyyy`, `dd.MM.yyyy HH:mm`, `dd.MM.yyyy · HH:mm`) — только в PDF
  (§21), корзине (§9.4), глобальных диалогах подписки (§19.5–19.6; там дата — в поясе
  устройства) и сроке акции тарифа (§19.0). Месяц в заголовках — именительный падеж («Апрель
  2026»), в датах с числом —
  родительный («5 апреля»).
- **Длительности** (`ConverterLongToTime`): обычный режим — `ЧЧ:ММ`, часы не
  ограничены 24 и дополняются нулём до двух знаков (`05:07`, `168:30`); `null` →
  строка из пробелов; отрицательное → `00:00`. При `UserSettings.isDecimalTime =
  true` — десятичные часы `Ч,ММ`, где `ММ` — сотые доли часа (30 мин → `7,50`).
  Секундомеры/обратные отсчёты на Главном всегда в `ЧЧ:ММ`. Длительности отдыха
  в виджете — `formatDurationFromMillis`: `«1д 2ч 5м»`.
- **Деньги**:
  - **Округление** — единая функция общего KMP-кода `Double.roundMoneyToCents()`:
    `round(x) = знак(x) × floor(|x| × 100 + 0.5) / 100` (половина копейки — от нуля;
    `NaN`/∞ → `0`). Каждая денежная строка округляется **до** суммирования (порядок —
    §11.5). iOS/PWA вызывают эту же функцию, а не платформенное округление.
  - **Формат** (`toMoneyString` / `str2decimalSign`): значение после округления,
    группировка тысяч **неразрывным** пробелом, запятая, ровно 2 знака, пробел и символ
    валюты: `12 345,67 ₽`; отрицательное — со знаком `-`. `null` → `0 ₽`.
  - **Валюта** по `UserSettings.country`: `KZ` → `₸`, `BY` → `Br`, остальное (в т.ч.
    `RU`/`null`) → `₽`. Цены подписки — всегда в ₽ (§19.0).
- **Месяц** — 0-based везде: в `MonthOfYear.month`, `ReleaseDay.month`, в API
  (январь = 0). Конвертация в 1-based — только в DTO-маппере/при построении дат.
- **Нормы времени** (`StationNorm`, `LocomotiveSeries`) — `Int`, **минуты**.
  Значение `0` трактуется как «не задано» → хранится как `null`; редакторы норм
  ограничивают ввод диапазоном `0..240` и `0` сохраняют как `null`.
  ⚠️ Поведение Android (возможный баг, BUG-01): кнопка «Сохранить норму
  станции …» в шторке времени (§2.8) записывает вычисленный интервал как есть и может
  сохранить `0` или отрицательное число.
- **UUID** — строки, генерируются клиентом: `generateId()` = `Uuid.random()
  .toString()` (строчный UUID v4 с дефисами). Сервер id не присваивает.
- **Числа по сети** (сериализация `kotlinx.serialization`):
  - `SectionElectric.*` (энергия/рекуперация) — `@Contextual Double`,
    сериализуются **строкой** через `DoubleAsStringSerializer`: целое → `"1783"`,
    дробное → `"1783.32"`; при чтении принимаются и строка, и JSON-число.
  - `SectionDiesel.*` и счётчики `heating*/auxiliary*` локомотива — обычные
    JSON-числа `Double`.
  - `Locomotive.normaElectricCurrent1/2` — `NumberAsDoubleSerializer`: целое
    уходит как `12`, дробное как `12.5`; принимается Int/Long/Double.
  - `Locomotive.normaDiesel` — **строка** с точкой (`"12.5"`).
  - `BasicData.updatedAt` — `DateAsLongSerializer`: пишется `Long`, при чтении
    принимается `Long` или строка Gson-формата `"Feb 17, 2026 17:24:45"` (UTC).
- **Реактивность** — репозитории отдают `Flow`; UI подписывается и
  перерисовывается при изменениях (везде, где сказано «getAllFlow()», на iOS —
  эквивалент: наблюдаемый источник, публикующий обновления).

### 0.2. Доменные модели (сущности)

Нотация: `?` — nullable; `= x` — значение по умолчанию. Все классы —
`@Serializable`. Поля с `@Transient` в JSON не попадают (только локально).

```
Route(                              // маршрут (рейс) целиком — единица синхронизации
  basicData: BasicData = BasicData(),
  locomotives: [Locomotive] = [], trains: [Train] = [], passengers: [Passenger] = [],
  otherWorks: [OtherWork] = [], partners: [RoutePartner] = [],
  photos: [Photo] = []              // устаревшее, клиент шлёт []
)

BasicData(
  id: String = generateId(),        // id маршрута
  remoteRouteId?: String, remoteObjectId?: String,
  isSynchronized: Boolean = false,  // отправлен ли текущий вариант на сервер
  isOnePersonOperation: Boolean = false,   // «в одно лицо»
  isDeleted: Boolean = false,       // soft-delete (корзина)
  @Transient deletedAt?: Long, @Transient deletionReason?: String,
  @Transient remoteDeletionPending: Boolean = false, @Transient remoteDeletedAt?: Long,
  updatedAt: Long = now,            // DateAsLongSerializer
  number?: String,                  // номер маршрутного листа
  timeStartWork?: Long,             // явка
  timeEndWork?: Long,               // окончание работы (сдача)
  restPointOfTurnover: Boolean = false,    // после маршрута — отдых в пункте оборота
  notes?: String, isFavorite: Boolean = false,
  timeStartBreak?: Long, timeEndBreak?: Long,  // перерыв
  workStartByArrivalPassengerId?: String,  // «явка по прибытию»: id пассажира; в БД не хранится
  @Transient timeStartWorkBeforeArrival?: Long
)

Locomotive(
  locoId: String = generateId(), basicId: String, remoteObjectId?: String,
  series?: String, number?: String, type: LocoType = ELECTRIC,
  electricSectionList: [SectionElectric] = [], dieselSectionList: [SectionDiesel] = [],
  timeStartOfAcceptance?: Long, timeEndOfAcceptance?: Long,      // приёмка: начало, конец
  timeStartOfDelivery?: Long,   timeEndOfDelivery?: Long,        // сдача: начало, конец
  normaElectricCurrent1?: Double, normaElectricCurrent2?: Double, // норма расхода, ток 1/2
  normaDiesel?: String,                                          // норма тепловоза, кг
  heatingCounterAccepted?: Double, heatingCounterDelivery?: Double,     // отопление, кВт·ч
  auxiliaryCounterAccepted?: Double, auxiliaryCounterDelivery?: Double, // собств. нужды, кВт·ч
  timeBarrierOut?: Long,   // приёмка: «Выход на КП»
  timeBarrierIn?: Long,    // сдача: «Заход на КП»
  acceptanceStationId?: String, deliveryStationId?: String       // ссылки на StationNorm
)
// Оба списка секций сохраняются всегда, независимо от type (см. §1.2).

LocoType = { ELECTRIC("Электротяга"), DIESEL("Теплотяга") }   // text — подпись в UI

SectionElectric(                   // показания счётчиков секции электровоза, кВт·ч
  sectionId: String = generateId(), locoId: String = "", type: LocoType = ELECTRIC,
  acceptedEnergy?, deliveryEnergy?,                    // расход, ток 1: принял/сдал
  acceptedRecovery?, deliveryRecovery?,                // рекуперация, ток 1
  acceptedEnergyOtherCurrent?, deliveryEnergyOtherCurrent?,     // расход, ток 2
  acceptedRecoveryOtherCurrent?, deliveryRecoveryOtherCurrent?  // рекуперация, ток 2
)                                  // все показания — Double?, по сети строками

SectionDiesel(                     // топливо секции тепловоза
  sectionId: String = generateId(), locoId: String = "", type: LocoType = DIESEL,
  acceptedFuel?: Double, deliveryFuel?: Double,   // литры: принял/сдал
  coefficient?: Double,                            // k секции (плотность, кг/л)
  fuelSupply?: Double,                             // экипировка, л
  fuelSupplyInKilo?: Double,                       // экипировка, кг
  coefficientSupply?: Double                       // k экипировки
)

Train(
  trainId: String = generateId(), basicId: String = "",
  number?: String, additionalNumbers: [String] = [],
  distance?: String, weight?: String, axle?: String, conditionalLength?: String, // строки!
  stations: [Station] = [], servicePhase?: ServicePhase,
  pusher?: TrainAssist, doubleTraction?: TrainAssist, doubledTrain?: TrainAssist,
  dataVersions: [TrainDataVersion] = [], carInspector?: CarInspector
)
TrainAssist(locomotiveNumber?, locomotiveSeries?, driverName?, notes?: String,
            isFirst?: Boolean)     // null — не задано, true «Я первый», false «Я второй»
CarInspector(fullName?, tabNumber?: String, couplingTime?: Long)
TrainDataVersion(stationId?, stationName?, weight?, axle?, conditionalLength?: String,
                 changedAt: Long = 0)
Station(
  stationId: String = generateId(), trainId: String = "",
  stationName?: String,            // JSON-имя "name"
  timeArrival?: Long, timeDeparture?: Long, orderIndex: Int = 0,
  trackNumber?: String,            // JSON-имя "track_number"
  isFinalStation: Boolean = false,
  isPassingStation: Boolean = false,  // проходная: одно время в timeArrival, timeDeparture = null
  segmentTrackNumber?: String, segmentNotes?: String  // перегон ПЕРЕД станцией
)

Passenger(
  passengerId: String = generateId(), basicId: String = "", remoteObjectId?: String,
  trainNumber?, stationDeparture?, stationArrival?: String,
  timeArrival?: Long, timeDeparture?: Long, notes?: String,
  isWorkStartByArrival: Boolean = false     // см. §7.2
)

OtherWork(
  otherWorkId: String = generateId(), basicId: String = "", remoteObjectId?: String,
  workType?: String,               // "Маневровая" | "Вывозная" | "При депо" | пользовательский
  timeStart?: Long, timeEnd?: Long, station?: String, notes?: String
)                                  // в расчёте времени/зарплаты не участвует

Photo(photoId: String = generateId(), basicId: String = "", remoteObjectId?: String,
      url: String, dateOfCreate: Long)   // устаревшее

StationNorm(
  stationId: String = generateId(), name: String,
  appearanceToStartMin?: Int,   // Явка → Начало приёмки
  endToBarrierMin?: Int,        // Окончание приёмки → Выход на КП
  barrierToStartMin?: Int,      // Заход на КП → Начало сдачи
  endToWorkEndMin?: Int,        // Окончание сдачи → Окончание работы
  updatedAt: Long = now
)

LocomotiveSeries(
  seriesId: String = generateId(), name: String, type: LocoType,
  acceptanceDurationMin?: Int,      // «После отстоя»: длительность приёмки
  deliveryDurationMin?: Int,        // «После отстоя»: длительность сдачи
  acceptanceHandToHandMin?: Int,    // «Из рук в руки»: длительность приёмки
  deliveryHandToHandMin?: Int,      // «Из рук в руки»: длительность сдачи
  sectionNumberingType: SectionNumberingType = NUMERIC,
  updatedAt: Long = now
)
SectionNumberingType = { NUMERIC, LETTERS }   // «Секция 1/2/3» | «Секция А/Б/В»
```

```
Partner(                     // запись справочника напарников
  partnerId: String = generateId(), fullName: String = "",
  tabNumber?: String,        // табельный номер (строка: ведущие нули)
  notes?: String,
  updatedAt: Long = now
)

RoutePartner(                // напарник внутри маршрута (копия + ссылка)
  routePartnerId: String = generateId(), basicId: String = "", remoteObjectId?: String,
  sourcePartnerId?: String,  // ссылка на Partner в справочнике (может быть null)
  fullName?: String, tabNumber?: String, notes?: String
)
```

```
UserSettings(                                // одна запись, key = "User_Settings_Key"
  minTimeRestPointOfTurnover: Long = 10 800 000,        // 3 ч — мин. отдых в ПО
  minTimeRestPointOfTurnoverSecond: Long = 14 400 000,  // 4 ч — второй подряд отдых в ПО
  minTimeHomeRest: Long = 57 600 000,                   // 16 ч — мин. домашний отдых
  lastEnteredDieselCoefficient: Double = 0.83,          // k секции по умолчанию
  nightTime: NightTime = 22:00–06:00,
  defaultLocoType: LocoType = ELECTRIC,
  defaultWorkTime: Long = 43 200 000 (12 ч), usingDefaultWorkTime: Boolean = false,
  isConsiderFutureRoute: Boolean = true,                // «Учитывать будущие маршруты»
  updateAt: Long, selectMonthOfYear: MonthOfYear = текущий месяц,
  stationList: [String] = [], locomotiveSeriesList: [String] = [],  // автодополнение
  timeZone: Long = 0,                                   // мс от Москвы, см. 0.1
  servicePhases: [ServicePhase] = [],
  standardTimesStartWork: [Long] = [8 ч, 20 ч],         // мс от полуночи
  subscriptionPeriod: Long = 0,                         // конец подписки, epoch ms; 0 — не было
  isDecimalTime: Boolean = false,
  isShowBreak, isShowOnePersonSwitch, isShowLocomotive, isShowTrain, isShowPassenger,
  isShowOtherWork, isShowPartner: Boolean = true,       // видимость блоков формы
  otherWorkTypeList: [String] = [],
  isShowLocoHeating, isShowLocoAuxiliary: Boolean = true,
  isConsiderLocoHeatingInTotal, isConsiderLocoAuxiliaryInTotal: Boolean = true,
  isShowLocoStatistics, isShowLocoNorma: Boolean = true,
  isShowOtherCurrent: Boolean = false,                  // «Смена рода тока»
  country: String = "RU", crossMonthTimezone = LOCAL,
  useStandardTimePicker: Boolean = false,               // системный пикер вместо шторки
  region?: String,                                      // ISO 3166-2, напр. "RU-TA"
  passengerWagonLengthMeters: Double = 24.5
)
ServicePhase(id = generateId(), departureStation, arrivalStation: String,
             distance: Int /* км */, linearMileageRate: Double = 0.0 /* руб/км */)
NightTime(startNightHour = 22, startNightMinute = 0, endNightHour = 6, endNightMinute = 0)
CrossMonthTimezone = { LOCAL, MOSCOW }

MonthOfYear(id = generateId(), year: Int, month: Int /* 0-based */, days: [Day] = [],
            tariffRate: Double = 0.0, dateSetTariffRate?: DateSetTariffRate)
Day(dayOfMonth: Int, tag: TagForDay, isReleaseDay = false, releaseType?: ReleaseType,
    hours?: Double /* только «Технические занятия» */)
TagForDay = { WORKING_DAY, NON_WORKING_DAY, SHORTENED_DAY, HOLIDAY }
ReleaseType (по сети — строка text) = Отпуск | Больничный | Курсы | Донорские |
  По уходу за ребенком-инвалидом | Выходной | Командировка | Технические занятия | Прочее
ReleaseDay(id = generateId(), year, month /* 0-based */, dayOfMonth: Int,
           releaseType: ReleaseType, hours?: Double)

SalarySetting — ставки/проценты для расчёта зарплаты; поля и смысл — §11–12.
```

Репозитории: `LocomotiveSeriesRepository`, `StationNormRepository`, `PartnerRepository`
(методы `getAllFlow()`, `getAll()`, `replaceAll(list)` — full-replace; `PartnerRepository`
дополнительно `upsert(partner)`, `delete(id)`, `getById(id)`).

**Напарники — модель хранения.** В маршруте хранится **копия** данных напарника
(`fullName`/`tabNumber`/`notes`), а не ссылка: маршрут самодостаточен (как станции-строки
в поезде), карточка открывается всегда, расшаривание работает даже если запись справочника
изменили/удалили. `sourcePartnerId` связывает копию с записью справочника (переход к
редактированию, если запись ещё существует). Синхронизация: справочник — full-replace
`GET/POST /v1/partners/` (по образцу norma_time), напарники маршрута — часть `SyncData`
(поле `partners`, full-replace только если поле присутствует; старые клиенты не шлют → не
трогаем). **PII:** ФИО/табельные не логируются на сервере и не отдаются публичным
`GET /v1/share/route/{id}`.

### 0.3. Общие производные величины маршрута

Используются на Главном (§4), в карточках и расчётах отдыха:
- **Перерыв** `getBreakDuration()` = `floorMin(timeEndBreak) − floorMin(timeStartBreak)`,
  если оба заданы и конец > начала, иначе `0` (`floorMin` — отбросить секунды).
- **Чистая работа** `getPureWorkTime()` = `floorMin(timeEndWork) −
  floorMin(timeStartWork) − перерыв`; `null`, если нет явки или сдачи.
- **Отработано по маршруту** `getWorkTime()` = чистая работа + проезд пассажиром
  до явки («явка по прибытию», §7.2).
- **Текущий маршрут** (`findCurrentRoute(now)`, по всем маршрутам всех месяцев):
  `timeStartWork < now` и (`now < timeEndWork` либо `timeEndWork == null` и после
  его явки не стартовал другой маршрут с явкой `≤ now`); из подходящих — с самой
  поздней явкой.
- **Следующий маршрут** (`findNextFutureRoute(now)`): минимальный `timeStartWork >
  now`; сдача не обязательна.
- **Переходный маршрут** (`isTransition`): явка и сдача попадают в разные
  календарные месяцы (в `crossMonthTZ`).

Источник: `domain/.../entities/route/{Route,BasicData,Locomotive,LocoType,Train,Passenger,OtherWork,RoutePartner,Photo,UtilsForEntities}.kt`,
`domain/.../entities/norma_time/{StationNorm,LocomotiveSeries}.kt`, `domain/.../entities/partner/Partner.kt`,
`domain/.../entities/setting/{UserSettings,SalarySetting}.kt`, `domain/.../entities/{MonthOfYear,ReleaseDay}.kt`,
`domain/.../entities/serializers/Serializers.kt`, `domain/.../util/{IdGenerator,TimeZoneUtils,TimeCalculationContext,DoubleUtil,Currency}.kt`,
`core_android/.../util/{DateAndTimeConverter,ConverterLongToTime,DateAndTimeFormat}.kt`.

---

## 1. Экран «Локомотив» (FormLocoScreen)

Форма одного локомотива внутри маршрута. Открывается из FormScreen
(«ЛОКОМОТИВ» → карточка локомотива или «Добавить локомотив») и с Главного
(плитка «Локомотив» текущего маршрута, §4.3).

### 1.1. Вход / выход и данные

- **Навигация**: `FormLoco` c параметрами `locoId` (null → новый) и `basicId`
  (id маршрута). Колбэки: «Готово» → `router.back()`; ⚙ →
  `router.showSettingsLoco(series)`; из шторки времени —
  `showSettingsSeriesList`, `showSettingsStationList`,
  `showSettingsStationEditor(id)`, `showSettingsSeriesEditor(id)`.
  При каждом `ON_RESUME` экрана флаги видимости перечитываются из настроек
  (`reloadSettingsFromPrefs`), чтобы изменения в Настройках → Локомотив
  применились без пересоздания экрана.
- **ViewModel**: `LocoFormViewModel(locoId, basicId)`. Наблюдаемые состояния:
  - `currentLoco: Locomotive?` — редактируемый локомотив (`null` до загрузки →
    экран пуст).
  - `electricSectionListState / dieselSectionListState` — **текстовые** состояния
    полей секций (строки ввода), в модель переводятся при сохранении
    (`toDoubleOrNull()`).
  - `routeStartWork: Long? / routeEndWork: Long?` — явка/окончание работы
    **маршрута** (из `basicData` через `RouteUseCase.routeDetails(basicId)`);
    нужны шторке времени. Ошибка загрузки не критична — поля остаются `null`.
  - `sectionNumberingType` — текущие подписи секций.
  - `uiState: LocoFormUiState` — флаги (`changesHaveState`, `isKiloMode`,
    `isShowOtherCurrent`, `confirmDelete*SectionId`, тексты счётчиков/норм и др.).
- **Новый локомотив** (`locoId == null`): `Locomotive(basicId, type =
  UserSettings.defaultLocoType)` и **сразу обе** стартовые секции: одна
  электрическая (пустая) и одна дизельная с `k секции =
  lastEnteredDieselCoefficient` и `k экипировки = последний k экипировки`
  (SharedPreferences, по умолчанию `0.83`).
- **Существующий**: загрузка `locomotiveUseCase.getLocoById(locoId)`; тип
  нумерации секций берётся у записи `LocomotiveSeries` с совпадающим (без
  регистра, по `trim`) именем, иначе — локальный тип по умолчанию
  (SharedPreferences `defaultLocoSectionNumberingType`). Секции раскладываются
  только для текущего `type` (секции другого вида в форму не грузятся).
  ⚠️ Поведение Android (возможный баг, BUG-02): при следующем сохранении
  список секций другого вида пишется пустым — ранее сохранённые секции другого вида
  теряются (на практике у локомотива заполнен только один вид).
  Тексты: счётчики отопления/собств. нужд и нормы электровоза — значение `0.0`
  показывается пустым полем.
- **Порядок секций является частью данных и не меняется самопроизвольно.**
  Локальная БД хранит секции в порядке JSON-массива. Сервер сохраняет индекс
  каждой секции (`position`) из порядка входного массива и возвращает секции
  по этому индексу. При обновлении уже существующего локального маршрута
  синхронизация дополнительно сохраняет прежний порядок совпадающих
  `sectionId`; секции, впервые пришедшие с другого устройства, добавляются
  после них в серверном порядке.

### 1.2. Сохранение (persistence)

- **Автосохранение с дебаунсом 500 мс**: изменение вызывает `changesHave()` →
  `triggerAutoSave()` (отменяет прошлую задачу, ждёт 500 мс, затем `saveLoco()` →
  `locomotiveUseCase.saveLocomotive`). `changesHave()` вызывают: серия, номер,
  тип тяги, все времена/станции из шторки, поля секций, добавление/удаление
  секций, смена нумерации, удаление подсказки серии.
  ⚠️ Поведение Android (возможный баг, BUG-03): ввод **нормы**
  (`setNorma*`) и **счётчиков отопления/собственных нужд** не вызывает `changesHave()` —
  эти значения попадают в БД только при следующем автосохранении от другого поля или при
  уходе с экрана (`onCleared`); при убийстве процесса ввод может потеряться.
- **Что сохраняется**: `currentLoco` + **оба** списка секций (`dieselSectionList`
  и `electricSectionList`) из текстовых состояний, независимо от `type`; пустая
  строка/невалидное число → `null`. Перед записью: непустая `series`
  добавляется в `UserSettings.locomotiveSeriesList` (`setLocomotiveSeries`), а
  если после тапа по нумерации (§1.3 п.4) серия есть в справочнике — у неё
  обновляется `sectionNumberingType` (`replaceAll`).
- **Финальное сохранение в `onCleared()`** (`NonCancellable + IO`) тем же
  способом.
- **Guard пустого локомотива** (`isLocoEmpty`): новый локомотив не сохраняется в
  `onCleared`, если пусты серия, номер, все 6 времён, обе станции, нормы
  (`0`/`null`/пусто), все 4 счётчика (`0`/`null`) и все секции (в дизельной
  проверяются принял/сдал/экипировка л/кг, в электрической — все 8 показаний;
  значение `0` считается пустым; коэффициенты вводом не считаются).
  ⚠️ Поведение Android (возможный баг, BUG-04): автосохранение с дебаунсом
  guard не проверяет — если пользователь ввёл и затем стёр значение, пустая запись уже
  могла быть сохранена.
- Ошибка сохранения → snackbar «Ошибка: <message>».
- iOS/PWA: повторить дебаунс-автосейв, финальное сохранение при уходе и guard.

### 1.3. Разделы UI (сверху вниз)

Все блоки — карточки с тенью 2dp на фоне экрана, над каждой — моноширинный
заголовок группы капсом (`ОСНОВНЫЕ ДАННЫЕ`, `ВРЕМЯ`, `СЕКЦИИ`, `ДРУГОЕ`, `ИТОГО`).

1. **Топ-бар**: слева текстовая кнопка **«Готово»** (акцентный цвет) — скрывает
   клавиатуру и выполняет `router.back()` (данные уже сохранены автосейвом и
   `onCleared`); по центру «Локомотив»; справа ⚙ → `showSettingsLoco(series)`
   (раздел «Настройки → Локомотив», §17.5; при выбранной серии в нём сверху
   кнопка «Настройки <имя>» либо «Создать серию <имя> в справочнике»). Так же
   оформлены топ-бары формы маршрута и Поезд/Пассажиром/Прочая работа.
   При прокрутке списка под топ-баром появляется тень (fade 300 мс).
2. **ОСНОВНЫЕ ДАННЫЕ**
   - Справа от заголовка — переключатель вида тяги: **молния** = Электровоз
     (ELECTRIC, «включено»), **капля** = Тепловоз (DIESEL). `changeLocoType`
     гарантирует минимум одну секцию нового вида; секции прежнего вида не
     удаляются.
   - Карточка из двух ячеек через вертикальный разделитель: **«Серия»** и
     **«Номер»** (моноширинный `bodyLarge`, однострочные, IME «Done»).
   - **Серия** — поле с автодополнением (`ExposedDropdownMenu`). Список = legacy
     `UserSettings.locomotiveSeriesList` ∪ имена записей `LocomotiveSeries`
     (без дублей без учёта регистра; недостающие имена справочника
     дописываются в `locomotiveSeriesList`). Фильтр: `startsWith(ввод,
     ignoreCase)`, точное совпадение с вводом скрывается; при пустом вводе —
     весь список. Ввод → `setSeries(text)`. Выбор строки → `selectSeries(name)`:
     ставит серию, **применяет `type` записи справочника** (и добавляет секцию
     нужного вида, если её нет) и её `sectionNumberingType`. У каждой строки —
     удаление: убирает имя только из `locomotiveSeriesList` (запись
     `LocomotiveSeries` с нормами остаётся).
     ⚠️ Локальное значение поля синхронизируется с `locomotive.series` (эффект по
     изменению серии) — чтобы выбор серии в шторке времени сразу отражался в
     поле (см. 2.12), не мешая набору.
   - **Номер** — свободный текст → `setNumber`.
3. **ВРЕМЯ** — карточка из двух строк; тап по строке открывает шторку времени.
   Долгого нажатия нет (удаление значений — внутри шторки, §2.9).
   - Строка: слева заголовок капсом (`ПРИЁМКА`/`СДАЧА`), справа шеврон `›`;
     ниже три ячейки `ЧЧ:ММ` (моноширинный `bodyLarge`, пустое — `—:—`
     приглушённо) с подписями капсом под ними, между ячейками стрелки `→`.
   - **ПРИЁМКА**: `начало` (`timeStartOfAcceptance`) → `конец`
     (`timeEndOfAcceptance`) → `КП` (`timeBarrierOut`). Тап → шторка
     `kind="acceptance"`.
   - **СДАЧА**: `КП` (`timeBarrierIn`) → `начало` (`timeStartOfDelivery`) →
     `конец` (`timeEndOfDelivery`). Тап → шторка `kind="delivery"`.
   - Время — в поясе отображения (§0.1). При крупном системном шрифте
     (`fontScale > 1.15`) ячейки выстраиваются столбцом «ПОДПИСЬ … ЧЧ:ММ».
4. **СЕКЦИИ** — справа от заголовка степпер `[−] N [+]` (N — число секций
   текущего вида). «−» неактивен при `N ≤ 1` и удаляет **последнюю** секцию
   (через подтверждение, см. ниже); «+» добавляет секцию текущего вида.
   - **Подписи секций**: `NUMERIC` → «Секция 1/2/3», `LETTERS` → «Секция
     А/Б/В/Г/Д/Е/Ж/З» (с 9-й — снова цифры). Тап по всей шапке карточки секции
     (без ripple) переключает тип для всех секций, запоминает его как тип по
     умолчанию (SharedPreferences) и, если текущая серия есть в справочнике,
     сразу пишет его в `LocomotiveSeries.sectionNumberingType` (`replaceAll`).
     Неизвестная серия в справочник не добавляется. Поле синхронизируется через
     `/v1/norma_time/locomotives/`; оно необязательное, сервер и клиент
     используют `NUMERIC` для записей без значения; при full-replace от старого
     клиента, который вообще не присылает поле, сервер сохраняет уже записанный
     тип серии.
   - **Удаление секции**: свайп карточки влево открывает красную кнопку удаления
     (`SwipeToRevealDelete`). Пустая секция (все поля пусты или `0`) удаляется
     сразу; иначе — нижняя шторка «Удалить секцию?» с действием «Да, удалить».
   - **Электровоз** (`ElectricSectionItem`): блок `РАСХОД` — поля «Принял» →
     «Сдал» (при включённом втором роде тока подписи «Ток 1 принял/сдал» и
     второй ряд «Ток 2 принял/сдал»); раскрываемый блок `РЕКУПЕРАЦИЯ` (по тапу
     на заголовок; изначально раскрыт, если есть данные рекуперации) с теми же
     полями. Ввод: клавиатура Decimal, до 10 символов, без фильтрации (строка с
     запятой даёт `null` при сохранении). Второй род тока показывается, если
     включена настройка `isShowOtherCurrent` или у секций есть данные тока 2
     (или `normaElectricCurrent2 ≠ 0`).
     Под пунктиром итоги секции: «Расход» = `сдал − принял` (всегда, `0` при
     отсутствии пары), «Рекуперация», «Расход (ток 2)», «Рекуперация (ток 2)» —
     только если обе величины пары заданы. Округление — до максимального числа
     знаков после точки среди двух введённых значений.
   - **Тепловоз** (`DieselSectionItem`):
     - Шапка: «Секция <подпись>» и справа пилюля **«k секции <значение>»** (пусто
       → «1.0»). Тап → шторка коэффициента (см. ниже).
     - `ТОПЛИВО` + переключатель единиц **Л | КГ** (`isKiloMode`, запоминается
       в SharedPreferences, общий для всех секций). Поля «Принял» → «Сдал»
       (плейсхолдер `0`, суффикс «л.»/«кг.»). Ввод фильтруется до цифр и одной
       точки, максимум 6 символов. **В модели всегда литры**: в режиме КГ
       введённое значение делится на `k секции` (`coeff = k ?: 1.0`; при `k = 0`
       сохраняется пусто); в режиме Л — округляется до 2 знаков. Под полем —
       подсказка-конвертация во вторую единицу («N кг» / «N л», округление 2
       знака; видна только при заданных значении и `k`).
     - `ЭКИПИРОВКА` — раскрываемый блок (шеврон; изначально раскрыт, если есть
       данные). В раскрытом виде справа пилюля **«k экипировки <значение>»**.
       Поля «Объём» (л.) и «Масса» (кг.), ввод до 7 символов:
       - ввод литров: если `k экипировки` задан — кг = `round2(л × k)`; если
         литры очищены/невалидны — **кг тоже очищаются**; если `k` не задан —
         кг не трогаются;
       - ввод кг: если `k` задан и `≠ 0` — л = `round2(кг ÷ k)`; иначе литры не
         трогаются;
       - смена `k экипировки`: при заданных литрах кг пересчитываются.
     - Пунктир и строка **«Расход» `<кг> кг / <л> л`** (всегда, `0` без данных):
       - л = `(round2(принял) − round2(сдал))` (округление до max знаков, ≤ 2) `+
         экипировка л`;
       - кг = `(принял × k секции − сдал × k секции) + экипировка кг
         (refuelInKilo)` — экипировка берётся из своего поля кг, а не
         пересчитывается по `k секции`. Если `k секции` пуст — кг-итог
         отсутствует (показывается `0 кг`).
       - Если нет пары принял/сдал — итог `null` (экипировка без пары не
         учитывается).
     - **Шторка коэффициента** (`CoeffSheet`, заголовок «Коэффициент секции» /
       «Коэффициент экипировки», справа «Готово»): степпер `[−] значение [+]`
       с шагом `0.01` (или `0.001`, если введено ≥ 3 знаков; минимум 0; пустое
       значение шагает от `1.0`), поле ввода до 5 символов (цифры и одна точка),
       плейсхолдер `1.0`; ниже `ИЗ ИСТОРИИ` — чипы последних 5 введённых
       коэффициентов (SharedPreferences; активный подсвечен). Изменения
       применяются сразу. В режиме КГ при смене `k секции` введённые **кг
       сохраняются**, а литры пересчитываются (снимок кг на момент открытия).
       Для экипировки есть чекбокс «Применить ко всем значениям этой секции»
       (по умолчанию включён): значение копируется и в `k секции`.
       Закрытие («Готово»/свайп): коэффициент добавляется в историю; `k секции`
       сохраняется как `UserSettings.lastEnteredDieselCoefficient`; `k
       экипировки` — как последний k экипировки (для новых секций).
       🚫 Мёртвый код Android — в iOS не реализуется: `EnteredCoefficientDialog` /
       `EnteredRefuelDialog` и флаги `refuelDialogShow`/`coefficientDialogShow` объявлены,
       но нигде не вызываются — актуальный UI только `CoeffSheet`.
   - **Валидация секций** (сообщения в `errorMessage` состояния секции):
     дизель — «Принял меньше чем сдал» / «Сдал больше чем принял» / «Не хватает
     экипировки» при отрицательном расходе, «Коэффициент больше 1.0» при `k > 1`;
     электро — «Принято больше чем сдано» / «Сдано меньше чем принято».
     ⚠️ Поведение Android (возможный баг, BUG-05): эти сообщения **нигде не
     отображаются** в UI и не блокируют сохранение; для электровоза сравнение идёт
     **строк**, а не чисел (`"9" > "10"`). iOS (паритет): сообщения не показывать.
5. **ДРУГОЕ** (показания счётчиков) — карточка появляется, если включено хотя бы
   одно из `isShowLocoHeating` / `isShowLocoAuxiliary` (по умолчанию оба). Строки
   **«Отопление»** и **«Собств. нужды»**: иконка, название, поля «Принял» →
   «Сдал» (Decimal, без фильтрации; значение → `toDoubleOrNull()`), под ними
   «Расход: `сдал − принял`» (округление 2 знака; только при двух валидных
   числах). При `fontScale > 1.15` поля стопкой.
6. **ИТОГО** — показывается всегда.
   - **Электровоз** (`ElectricStatisticsSection`): для тока 1 и (если показан
     второй род тока) для тока 2 — подзаголовок «ТОК 1»/«ТОК 2» (только когда
     токов два), `РАСХОД` (крупно), при наличии рекуперации — `РЕКУПЕРАЦИЯ`
     (зелёным) и `ЧИСТЫЙ РАСХОД` = расход − рекуперация. Числа: округление 2
     знака, тысячи через пробел, дробная часть через запятую (`12 345,6`),
     пусто → `0`.
     - Расход тока 1 = Σ по секциям `(сдал − принял)` (секции без пары не
       участвуют; если ни у одной нет пары — `null`), затем прибавляются
       `сдал − принял` счётчиков отопления и собственных нужд, но только при
       `isConsiderLocoHeatingInTotal` / `isConsiderLocoAuxiliaryInTotal`
       (оба по умолчанию `true`) и только если расход секций не `null`;
       неполная пара показаний ничего не добавляет. Ток 2 — только секции.
   - **Тепловоз** (`DieselStatisticsSection`): `РАСХОД` — крупно кг (если у
     секций есть `k`) с пилюлей «N л», иначе крупно литры (`—`, если данных
     нет); при наличии экипировки — `ЭКИПИРОВКА` тем же стилем (Σ л и Σ кг).
     Кг-итог = Σ по секциям `(принял×k − сдал×k) + refuelInKilo` — **тем же
     способом, что в карточке секции**. НЕ умножать суммарные литры (топливо +
     экипировка) на коэффициент секции — иначе итог расходится с суммой секций
     (был баг: 498 вместо 500).
   - **Поле «НОРМА»** (`NormaPill`, клавиатура Decimal, до 7 символов; суффикс
     «кг» у тепловоза, без единицы у электровоза) — **допускает дробные
     значения** («2710.5», «12.5»). Ввод нормализуется во ViewModel через
     `sanitizeNumericInput(allowDecimal = true)`: запятая → точка, один
     разделитель, только ASCII-цифры (`setNormaElectricCurrent1/2`,
     `setNormaDiesel`).
     - Электровоз: поле показывает **введённый текст** (`uiState.norma1Text` /
       `norma2Text`), а не `Double.str()` — иначе «12.» при наборе схлопывается
       в «12», а «12,5» очищает поле. Модель: `normaElectricCurrent1/2: Double?`.
       Инициализация текста — один раз при загрузке.
     - Тепловоз: `normaDiesel: String?`, хранится и уходит на сервер строкой
       **с точкой** (`"12.5"`); пустая строка → `null`.
     - Сервер: `locomotive.normaDiesel` — `DOUBLE PRECISION` (миграция 042,
       раньше был `Integer` и усекал дробную часть); `safe_float` терпит
       десятичную запятую от старых клиентов. В `GET /v1/route/` целое
       значение отдаётся как `12`, дробное как `12.5` (`_compact_number`) —
       чтобы старые клиенты со строковым `normaDiesel` не показывали «12.0».
   - **Результат** (только при непустой норме): `результат = норма − расход`
     (для электровоза — расход, а не чистый расход; для тепловоза — кг-итог;
     норма через `toDoubleOrZero()`, терпит запятую). `результат < 0` →
     красная плашка «↓ ПЕРЕРАСХОД −X», иначе зелёная «↑ ЭКОНОМИЯ +X», где
     `X = |результат|` (2 знака) и в скобках `(±P%)`, `P = |результат/норма×100|`
     (целое, `0` при норме 0). ⚠️ Поведение Android (возможный баг, BUG-06): если у тепловоза
     нет кг-итога (не задан `k секции`), результат считается как `0` → «ЭКОНОМИЯ +0».
7. Удаления локомотива на этом экране **нет** — он удаляется из формы маршрута
   (§5). Флаги `isShowLocoStatistics`/`isShowLocoNorma` на экран не влияют.

### 1.4. Запись времени из шторки

- Приёмка: `saveAcceptanceFromSheet(startTime, endTime, barrierOut, stationId)`
  → `currentLoco.copy(timeStartOfAcceptance, timeEndOfAcceptance,
  timeBarrierOut, acceptanceStationId)` + `changesHave()`.
- Сдача: `saveDeliveryFromSheet(barrierIn, startTime, endTime, stationId)`
  → `currentLoco.copy(timeBarrierIn, timeStartOfDelivery, timeEndOfDelivery,
  deliveryStationId)` + `changesHave()`.
- **Окончание работы** (сдача) пишется НЕ в локомотив, а в **маршрут**:
  `setTimeEndWork(value)` → перечитывает маршрут, `RouteUseCase.saveRoute(
  basicData.timeEndWork = value)` и обновляет `routeEndWork`. Вызывается из
  шторки колбэком `onTimeEndWorkChanged` (мгновенная синхронизация).
- Выбор/сброс серии в шторке → `setSeries(name)` (сброс — пустая строка).

Источник: `features/route/.../ui/FormLocoScreen.kt`, `.../navigation/FormLocoDestination.kt`,
`.../viewmodel/{LocoFormViewModel,LocoFormUiState}.kt`, `.../component/{DieselSectionItem,ElectricSectionItem,StatisticsSection,SwipeToRevealDelete,EnteredCoefficientDialog,EnteredRefuelDialog}.kt`,
`.../ui/settings/SettingsLocoContent.kt`, `.../ui/UIHelper.kt`, `domain/.../util/{CalculationEnergy,DoubleUtil,StringUtil}.kt`.

---

## 2. Шторка «Установка времени приёмки/сдачи» (TimeBottomSheet)

Нижняя шторка расчёта и ручного ввода времени приёмки **или** сдачи
локомотива. Один компонент, два режима: `kind = "acceptance" | "delivery"`.
Раскрывается сразу полностью (без промежуточного состояния), содержимое
прокручивается; закрывается свайпом вниз / тапом вне.

### 2.1. Параметры входа

```
TimeBottomSheet(
  kind, seriesName?, locoType,
  initialStartTime?, initialEndTime?, initialBarrierOut?, initialBarrierIn?,
  routeStartWork?, routeEndWork?, initialStationId?, timeZoneText,
  onSave(TimeSheetResult), onClose,
  onNavigateToSeriesSettings, onNavigateToStationSettings,
  onEditStation?(stationId), onSeriesChanged?(name), onEditSeries?(seriesId),
  onTimeEndWorkChanged?(Long?)   // только delivery
)
TimeSheetResult(startTime?, endTime?, barrierOut?, barrierIn?, routeEndWork?, stationId?)
```

Для приёмки `startTime/endTime` = начало/конец приёмки, для сдачи — начало/
конец сдачи. `timeZoneText` — пояс отображения (§0.1), им форматируются все
`ЧЧ:ММ` в шторке и открываются пикеры.
⚠️ Поведение Android (возможный баг, BUG-07): `fmtTime` разбирает `timeZoneText`
как число часов (`"GMT+5"` → 5); для пояса с минутами (`GMT+05:30`) разбор не удаётся, и
времена в строках шторки форматируются в `GMT+3`.

Внутреннее состояние (mutable, инициализируется из initial*):
`startTime, endTime, barrierOut, barrierIn, workEnd(=routeEndWork),
selectedStation (из StationNormRepository по initialStationId),
selectedSeriesName (= seriesName, пустая строка → null)`. Серия из справочника
ищется по **точному** совпадению имени.

⚠️ `selectedStation` **НЕ** пере-ключается на `initialStationId` (иначе при
немедленном сохранении id «догоняет» выбор, state пересоздаётся и на кадр
сбрасывается в null, а авто-сейв затирает станцию). Инициализация синхронная +
восстановление из справочника, пока выбор пуст. Авто-сейв считает станцию
изменённой только когда она реально выбрана (`selectedStation != null`).

### 2.2. Состав сверху вниз

1. Шапка: заголовок «Приёмка»/«Сдача» (22sp) и справа «Готово» (§2.11).
2. Две строки-поля контекста (подпись капсом + значение + шеврон): **«СЕРИЯ»**
   (значение или «Выберите серию») → `SeriesPickerSheet`; **«СТАНЦИЯ
   ПРИЁМКИ»/«СТАНЦИЯ СДАЧИ»** (имя или «Выберите станцию») → `StationPickerSheet`.
3. Только при выбранной серии — сегмент **«После отстоя | Из рук в руки»** (§2.4).
4. Оранжевые предупреждения (§2.7).
5. Баннер выбора якоря (только в режиме ASKING, §2.5).
6. Строки времени (§2.3), соединённые короткими вертикальными линиями.
7. Кнопки «Сохранить норму …» (§2.8) — только когда доступны.
8. Кнопка **«Рассчитать время»** (§2.5).

### 2.3. Строки времени и смысл полей

| Строка (acceptance)      | Поле        | Отклонение считается от нормы    |
|--------------------------|-------------|-----------------------------------|
| Явка (locked, из маршрута) | routeStartWork | —                              |
| Начало приёмки           | startTime   | appearanceToStartMin (явка→начало)|
| Окончание приёмки        | endTime     | *длительность серии* (приёмка)    |
| Выход на КП              | barrierOut  | endToBarrierMin (конец→КП)        |

| Строка (delivery)        | Поле        | Отклонение считается от нормы     |
|--------------------------|-------------|-----------------------------------|
| Заход на КП              | barrierIn   | —                                 |
| Начало сдачи             | startTime   | barrierToStartMin (КП→начало)     |
| Окончание сдачи          | endTime     | *длительность серии* (сдача)      |
| Окончание работы         | workEnd     | endToWorkEndMin (конец→работа); пишется в маршрут |

Строка: слева круглая иконка статуса, в центре название (+ подписи), справа
«кнопка» времени `ЧЧ:ММ` (моноширинный 18sp; пусто — `—:—`).
- **Иконка статуса**: время задано → зелёная галочка (`#00B341` на светло-
  зелёном фоне 14%); не задано → серые часы.
- Под «Явка» — подпись **«из маршрута»** (Явка здесь не редактируется, тап по
  ней ничего не делает вне режима ASKING; меняется только в форме маршрута).
- **Отклонение** (под названием, для строк с нормой, когда заданы обе
  соседние точки): `фактМин = (время − предыдущее) / 60 000` (целые минуты),
  текст `+N мин` / `−N мин`. Нет нормы → только `+N мин` серым. Есть норма:
  совпадает → `+N мин норма` серым; больше → `+N мин на D мин больше`
  красным; меньше → `+N мин на D мин меньше` зелёным.
- **Тап** по строке → пикер даты+времени `AppDateTimePicker` (§24.2) с
  заголовком строки; стартовое значение — текущее значение поля или «сейчас».
  Результат — с обнулёнными секундами. Для «Окончание работы» выбор сразу
  вызывает `onTimeEndWorkChanged(value)`.
- В режиме ASKING строка подсвечивается акцентом (мигание 800 мс) — см. 2.5.

### 2.4. Норма серии и вариант «После отстоя / Из рук в руки»

- Серия берётся из `LocomotiveSeriesRepository` по имени (`selectedSeries`).
- Переключатель варианта: `normHandToHand` (false = «После отстоя», true =
  «Из рук в руки»), запоминается в `SharedPreferencesRepositories`
  (`setLocoNormHandToHand`), общий для приёмки и сдачи. Определяет, какая пара
  норм серии активна:
  - acceptance: `acceptanceDurationMin` / `acceptanceHandToHandMin`;
  - delivery: `deliveryDurationMin` / `deliveryHandToHandMin`.
- **Отклонения в строке длительности** считаются относительно **применённого**
  варианта `appliedHandToHand`, а не текущего тумблера. Поэтому переключение
  тумблера НЕ мигает красным. `appliedHandToHand` = тумблер при открытии и после
  любого расчёта.
- При переключении тумблера, если заданы начало и окончание, у нового варианта
  норма задана и отличается от нормы применённого — диалог (`AppAlertDialog`)
  «Другая норма»: «Для условия «Из рук в руки|После отстоя» действует другая
  норма длительности. Пересчитать время по ней?», кнопки «Пересчитать» /
  «Оставить». «Пересчитать» → расчёт от ранее выбранного якоря, а если его не
  было — от «Начало приёмки/сдачи».

### 2.5. Кнопка «Рассчитать время» и выбор точки отсчёта (якоря)

Кнопка во всю ширину; активна (акцентный фон, белый текст), только если есть
хотя бы один **пригодный якорь**; иначе серая с рамкой и не нажимается.
Пригодность (норма = реально заданное значение, не `null`):

| Якорь | acceptance: пригоден, если | delivery: пригоден, если |
|---|---|---|
| Явка / Заход на КП | явка есть и `appearanceToStartMin` задан | заход есть и `barrierToStartMin` задан |
| Начало | время есть и норма серии (текущий вариант) задана | то же |
| Окончание | время есть и (норма серии или `endToBarrierMin` задан) | время есть и (норма серии или `endToWorkEndMin` задан) |
| Выход на КП / Окончание работы | время есть и `endToBarrierMin` задан | время есть и `endToWorkEndMin` задан |

Станция не обязательна для расчёта длительности по норме серии; серия не
обязательна для станционного интервала. Логика `applyNorms`: 0 пригодных
якорей → ничего; ровно 1 → расчёт сразу от него; ≥ 2 → режим **ASKING**:
баннер **«Выберите, от какой операции начать отсчёт времени»**, пригодные
строки мигают, тап по пригодной строке выбирает якорь и считает; непригодные
строки в ASKING не реагируют на тап.

Формулы (интервалы в минутах × 60 000 мс; «нет нормы» → участок пропускается,
фиктивного нулевого времени не создаётся):
- **Acceptance** (явка никогда не меняется):
  - от Явки: `начало = явка + appearanceToStart`; при норме серии `конец =
    начало + длит.`; далее `КП = конец + endToBarrier`, если норма КП задана,
    иначе КП = `null`.
  - от Начала: при норме серии `конец = начало + длит.`, `КП` — как выше.
  - от Окончания: `КП` — как выше; при норме серии `начало = конец − длит.`.
  - от КП: `конец = КП − endToBarrier`; при норме серии `начало = конец − длит.`.
- **Delivery**:
  - от Захода на КП: `начало = заход + barrierToStart`; при норме серии `конец =
    начало + длит.` и далее окончание работы `= конец + endToWorkEnd` (если задано).
  - от Начала: при норме серии `конец = начало + длит.` (+ окончание работы);
    `заход = начало − barrierToStart`, если норма КП задана, иначе `null`.
  - от Окончания: окончание работы `= конец + endToWorkEnd` (если задано); при
    норме серии `начало = конец − длит.`, `заход` — как выше.
  - от Окончания работы: `конец = работа − endToWorkEnd`; при норме серии
    `начало`, `заход` — как выше. Окончание работы остаётся прежним.
- **Вычисленное окончание работы**: если у маршрута уже есть `routeEndWork` и
  результат от него отличается — поле **не** меняется, вместо кнопки времени в
  строке появляется кнопка **«Обновить ЧЧ:ММ»**; её нажатие принимает значение и
  сразу вызывает `onTimeEndWorkChanged`. Пока висит это предложение, тап по
  строке пикер не открывает. Если у маршрута окончания не было — значение
  подставляется сразу (в маршрут уходит по «Готово»).
- Выбранный якорь запоминается (`accAnchor`/`delAnchor`) для пересчёта при смене
  варианта нормы.

### 2.6. Валидация последовательности

Красная подпись под строкой «⛔ Время должно быть позже предыдущего»:
- acceptance: «Начало приёмки» `≤` явки; «Окончание приёмки» `≤` начала;
  «Выход на КП» **`<`** окончания (равенство допустимо — интервал может быть
  нулевым);
- delivery: «Начало сдачи» **`<`** захода на КП (равенство допустимо);
  «Окончание сдачи» `≤` начала; «Окончание работы» `≤` окончания сдачи.
Проверяются только пары, где оба значения заданы. При любой ошибке «Готово»
неактивно (бледное), но немедленное сохранение (§2.11) продолжает работать.

### 2.7. Предупреждения (WarnItem, оранжевые инфо-блоки)

Плашка: оранжевая рамка и фон `#D98F20` (10%), значок «!», заголовок, серая
подсказка, опционально кнопка-CTA «… ›» и крестик ✕. Порядок сборки:
1. Серия: не выбрана → «Серия не выбрана» / «Выберите серию, чтобы применить
   нормы» (без CTA); выбрана, но у серии нет нормы текущего варианта для этой
   операции (или записи нет) → **«Нет нормы для серии <имя>»**, CTA «Настроить
   серию».
2. Станция не выбрана **и серия выбрана**:
   - acceptance, начало приёмки задано и норма серии есть → «Будет рассчитано
     только время приёмки» / «Чтобы применить нормы станции, выберите станцию»;
   - acceptance, иначе → «Укажите время начала / окончания приёмки или выберите
     станцию» / «Начало нужно для расчёта времени приёмки по норме локомотива»;
   - delivery → «Станция сдачи не выбрана» / «Выберите станцию или укажите
     начало / окончание сдачи локомотива».
   Если не выбраны ни серия, ни станция — показывается только п.1.
3. Иначе, если станция выбрана и у неё нет **обеих** относящихся к операции
   норм (acceptance: `appearanceToStartMin` и `endToBarrierMin`; delivery:
   `endToWorkEndMin` и `barrierToStartMin`) → **«Нет нормы для станции <имя>»**,
   CTA «Настроить станцию», закрывается ✕.
4. Иначе, если нет только **основной** станционной нормы → «Нет нормы времени:
   от явки до начала приёмки» (acceptance) / «Нет нормы времени: от окончания
   сдачи до окончания работы» (delivery), подсказка «Расчёт выполнится без этой
   нормы», CTA «Настроить станцию», ✕. Отсутствие опционального интервала КП
   предупреждения не создаёт: КП может быть не актуален на станции.
5. Delivery без единого заполненного времени (заход/начало/окончание/работа) →
   «Укажите время захода на КП или время окончания работы» / «От него будет
   выполнен расчёт по нормам».

- Закрытые крестиком предупреждения хранятся в `dismissedWarnings` (по тексту,
  на сессию открытия шторки).
- **CTA «Настроить серию/станцию»** открывает редактирование **прямо в
  шторке-пикере** (см. 2.10) для текущей серии/станции; шторка времени НЕ
  закрывается. Если записи серии в справочнике нет — шторка закрывается и
  открывается список серий в Настройках (`onNavigateToSeriesSettings`).

### 2.8. Сохранение нормы серии/станции из шторки

Внизу появляются кнопки-плашки (заголовок + подзаголовок, акцентный фон):
- **«Сохранить норму серии <имя>»** — если серия выбрана, заданы начало и
  окончание и (серии нет в справочнике **или** `длит. = (конец − начало)/60 000`
  отличается от нормы текущего варианта / норма не задана). Подзаголовок:
  «Добавить в справочник · после отстоя|из рук в руки» (нет записи) либо
  «Длительность приёмки|сдачи · после отстоя|из рук в руки». Нажатие пишет
  длительность в поле текущего варианта; если записи не было — создаётся новая
  `LocomotiveSeries(name = trim(имя), type = locoType)` с этим одним полем.
- **«Сохранить норму станции <имя>»** — если станция выбрана и:
  acceptance — заданы явка, начало и конец, и (интервал явка→начало ≠
  сохранённому или (КП задан и конец→КП ≠ сохранённому)); delivery — заданы
  заход, начало, конец и окончание работы, и (заход→начало ≠ сохранённому или
  конец→работа ≠ сохранённому). Сравнение нормализует: вычисленный интервал
  `≤ 0` и сохранённый `0` трактуются как «не задано». Подзаголовок: «Добавить в
  справочник» (если нет обеих норм операции) либо «явка -> приемка / приемка ->
  выход на КП» / «КП -> сдача / сдача -> окончание работы». Нажатие записывает
  оба интервала операции (если соответствующие времена заданы; иначе
  оставляет прежнее значение) и обновляет выбранную станцию.
  ⚠️ Поведение Android (возможный баг, BUG-01): интервал записывается без
  нормализации — возможен `0`/отрицательный (§0.1).
- Запись — через `replaceAll` (full-replace списка справочника).

### 2.9. Удаление значения времени

Долгое нажатие на строку с заданным временем → нижняя шторка `AppBottomSheet`
с заголовком-названием строки и действием **«Удалить значение»** (как «время
явки» в FormScreen). Явка (locked) не удаляется; для пустой строки long-press
ничего не делает. Для «Окончание работы» удаление также сбрасывает
предложение «Обновить …» и сразу синхронизирует маршрут
(`onTimeEndWorkChanged(null)`).

### 2.10. Пикеры серии/станции (внутри шторки)

`SeriesPickerSheet` / `StationPickerSheet` — вложенные нижние шторки высотой
85% экрана, открываются сразу полностью, без видимой ручки:
- **Шапка списка**: заголовок «Серия»/«Станция» по центру и справа
  увеличенная кнопка **«Готово»** (17sp, закрывает пикер без изменения выбора).
  Левой кнопки нет.
- **Поиск** «Поиск серии...» / «Поиск станции...» — `contains(ignoreCase)` по
  имени.
- Первая строка списка — текстовая кнопка **«+ Добавить серию» / «+ Добавить
  станцию»** → форма создания в этой же шторке.
- Пустой справочник: «Нет серий. Добавьте в Настройках → Серии.» / «Нет станций.
  Добавьте в Настройках → Станции.» (не кликабельно).
- **Группы**: серии — «ЭЛЕКТРОВОЗЫ» и «ТЕПЛОВОЗЫ» (по `type`); станции — «С
  НОРМАМИ» (задана хотя бы одна из 4 норм) и «БЕЗ НОРМ». Порядок внутри групп —
  как в справочнике.
- **Строка**: имя (серия — моноширинным) и подпись норм:
  - серия: `acc = acceptanceDurationMin ?: acceptanceHandToHandMin`, `del` —
    аналогично; «Приёмка A мин · Сдача D мин» / только одна часть / «Нормы не
    заданы»;
  - станция: `Приёмка A/B · Сдача C/D` (A = явка→начало, B = конец→КП, C =
    КП→начало, D = конец→работа; незаданный = 0); полностью пустая — «Норма не
    задана».
  Справа **карандаш** — редактирование в этой же шторке. Текущая выбранная
  строка подсвечена акцентом 14%.
- Тап по строке — **выбор** и закрытие пикера; повторный тап по уже выбранной —
  **снимает выбор** и закрывает пикер (станция → `null` с немедленным `onSave`;
  серия → `onSeriesChanged("")`). Выбор серии вызывает `onSeriesChanged(name)`.
- **Редактирование и создание — прямо в этой же шторке** (свап контента):
  карандаш → редактор существующей; «+ Добавить …» → форма создания (заголовок
  «Новая станция/серия», иначе «Станция/Серия»); справа «Готово»
  (`commit()` + возврат к списку, а не на форму локомотива). Редакторы — те же
  `SettingsStationEditorContent` / `SettingsSeriesEditorContent` (§17.4),
  ViewModel через `koinViewModel(parametersOf(id, null))`.
- `initialEditStation/Series` (из CTA «Настроить …») открывает пикер сразу в
  редакторе нужного элемента.
- Все редактируемые поля автоматически сохраняются через 500 мс после изменения
  (`save()` в VM; новая серия без единой нормы не создаётся). «Готово» вызывает
  финальный `commit()`: последний ввод сохраняется без ожидания debounce, а имя
  новой станции/серии регистрируется в общем справочнике автодополнения.

### 2.11. Шапка, немедленное сохранение и завершение

- **«Готово»** (синий текст, неактивен при ошибке последовательности): для
  сдачи с заданным `workEnd` — `onTimeEndWorkChanged(workEnd)`; затем
  `sheetState.hide()` + финальный `onSave(TimeSheetResult)` + `onClose()`.
- **Немедленное сохранение (не только по «Готово»):** любое изменение времени
  или станции пишется в форму сразу — эффект на
  `startTime/endTime/barrierOut/barrierIn/selectedStation` вызывает `onSave`, как
  только состояние отличается от переданных `initial*` (при открытии и при
  асинхронной загрузке станции лишних записей нет). Поэтому данные не теряются
  при закрытии шторки свайпом. «Окончание работы» (delivery) синхронизируется в
  маршрут через `onTimeEndWorkChanged` сразу при вводе из пикера, при принятии
  «Обновить …» и при удалении значения (§2.9); значение, подставленное расчётом
  без конфликта, уходит в маршрут только по «Готово».
- `onSave` в форме локомотива вызывает `saveAcceptanceFromSheet` /
  `saveDeliveryFromSheet` (см. 1.4) и **не закрывает** шторку; закрытие — только
  через `onClose` («Готово» или свайп).

### 2.12. Синхронизация серии обратно в форму

Смена/выбор серии в шторке (`onSeriesChanged` = `viewModel::setSeries`)
обновляет `currentLoco.series`; поле «Серия» на форме подхватывает изменение
через эффект по `locomotive.series` (не мешая ручному вводу). Тип тяги при
выборе серии в шторке не меняется (в отличие от выбора в поле формы, §1.3).

### 2.13. История именования

Кнопка расчёта раньше называлась «Установить по ПЗВ» (плашка справа сверху);
сейчас: «Готово» — синий текст в шапке, а расчёт — нижняя кнопка «Рассчитать
время». 🚫 Мёртвый код Android — в iOS не реализуется: неиспользуемый `AnchorSelectionBanner` («От
какого момента рассчитать время?» с чипами).

Источник: `features/route/.../ui/{TimeBottomSheet,SeriesPickerSheet,StationPickerSheet,NormaTimeComponents}.kt`,
`.../viewmodel/SeriesEditorViewModel.kt`, `.../ui/settings/{SettingsSeriesEditorContent,SettingsStationEditorContent}.kt`,
`.../component/{AppDateTimePicker,AppBottomSheet,AppAlertDialog}.kt`,
`core_android/.../ui/component/DateTimePickerApp.kt`.

---

## 3. Навигация и общая структура приложения

Единая `Activity` (Android) с Compose-навигацией (`NavHost`, старт —
`HomeFeature` → `HomeRoute`; переходы — fade in/out). Навигация вынесена в
интерфейс `Router` (`domain.navigation.Router`), реализуемый на каждой платформе
(`RouterImpl` на Android). Экраны НЕ знают про `NavController` — дёргают методы
`router.showX()`.

### 3.1. Точка входа, оверлеи запуска и нижнее меню

- **Старт**: сплэш (Android 12+ — системный, держится до инициализации и не
  менее 0,9 с; ниже — свой брендовый, не менее 1,5 с) → Главный экран.
  Отдельного экрана входа/онбординга при запуске **нет**: вход выполняется из
  «Профиля» (§18).
- **`FirstPresentationBlockScreen`** (онбординг 7 слайдов, кнопка → `showSignIn`) —
  🚫 Мёртвый код Android — в iOS не реализуется: объявлен, но **не зарегистрирован** в
  `NavHost` и нигде не показывается (флаг `showFirstPresentation` не используется), §20.
- **«Что нового» после обновления** (`UpdatePresentationBlockScreen`) —
  полноэкранный оверлей поверх приложения, если при старте
  `isShowUpdatePresentation()` (SharedPreferences, по умолчанию `true`) и это не
  первый запуск. Слайды: «Часовой пояс» — «Для учета переходных маршрутов,
  ночных и праздничных часов установите свой часовой пояс»; «Главный экран» —
  «Добавили обшее отработанное время на главный экран». Сверху стрелка «назад»
  (предыдущий слайд) и «Пропустить», внизу индикатор и кнопка «Далее» / «Начать»
  (последний слайд); «Пропустить» и «Начать» → `router.showHome`. Флаг сбрасывается в `false` при первой
  инициализации `HomeViewModel`, поэтому оверлей показывается один раз.
  ⚠️ Поведение Android (возможный баг, BUG-08): видимость оверлея — неизменяемое
  значение на время жизни Activity, поэтому после «Начать» оверлей не скрывается до
  пересоздания Activity. Android-легаси: в iOS этот оверлей не реализуется (§22).
- **«Новость при запуске»** (`AnnouncementScreen`) — полноэкранный оверлей после
  сплэша, очередь сообщений; поведение и контракт — §23.
- **Глобальные диалоги поверх любого экрана** (Activity-уровень): «Платёж
  принят!», «Бонус начислен! 🎉» / «Вам добавлено N дн. подписки по
  реферальной программе.» / «Ура!», «Подписка продлена» (изменение срока) — см.
  §19; «Импорт маршрута» («Импортировать маршрут от dd.MM.yyyy?», «Импортировать»
  / «Отмена») при открытии файла `.zroute`.
- **Тап по пустому месту** любого экрана снимает фокус и скрывает клавиатуру.
- **Нижнее меню** (`BottomNavigationBar`) показывается **только** на корневых
  экранах: Главная, Расчёт зарплаты, Настройки, Профиль. Пункты слева направо:
  «Главная» → `HomeRoute`; «Зарплата» → `SalaryCalculationRoute`; центральная
  **«Добавить»** (круглая «+») — не вкладка, а действие «новый маршрут» (§3.4);
  «Настройки» → `SettingsScreenRoute`; «Профиль» → `ProfileRoute`. Переход по
  вкладке: `popUpTo(start) { saveState }`, `launchSingleTop`, `restoreState`;
  повторный тап по текущей вкладке ничего не делает. На iOS — `TabView` из 4
  вкладок + отдельная кнопка «+» с тем же действием.
- **Deep links / внешние входы**:
  - `locodriver://share/{id}` и `https://locodriver.ru/r/{id}` — загрузка
    расшаренного маршрута в `SharedRouteHolder` и открытие формы маршрута в
    режиме preview (см. 5.6);
  - `locodriver://profile` — переход на вкладку «Профиль» (как `showProfile`);
  - возврат из оплаты Robokassa (`robokassa://…`) — обработка платежа (§19);
  - виджет: «добавить маршрут» → новая форма маршрута; тап по телу → Главная
    (`popBackStack(HomeRoute)`); малый виджет → форма текущего маршрута или
    форма его поезда.

### 3.2. Карта переходов (методы Router)

Реестр активных route-объектов Android NavHost (имена нужны для автоматической
проверки покрытия): `HomeRoute`, `FormRoute`, `FormLoco`, `FormTrain`,
`FormPassenger`, `FormOtherWork`, `PartnersManageRoute`, `PartnerPickerRoute`,
`PartnerEditRoute`, `SearchRoute`, `PurchasesRoute`, `SalaryCalculationRoute`,
`SettingSalaryRoute`, `UpdatePresentationBlockRoute`, `AllRouteScreenRoute`,
`SettingsScreenRoute`, `SelectReleaseDaysScreenRoute`, `ProfileRoute`,
`StatisticsRoute`, `CalendarRoute`, `ScheduleWizardRoute`, `AbsenceRoute`.
`SelectReleaseDaysScreenRoute` зарегистрирован, но недостижим (§16.2). `DetailsRoute`,
`SignInScreenRoute`, `LogInScreenRoute`, `FirstPresentationBlockRoute` объявлены, но в
`NavHost` не зарегистрированы. Все пять — 🚫 Мёртвый код Android — в iOS не реализуется
(§16.2, §20, §22).

| Метод | Экран назначения | Раздел |
|---|---|---|
| `showStartScreen` | граф `HomeFeature` (Главный) | 4 |
| `showHome(startingRoute)` | Главный (`HomeRoute`; аргумент не используется) | 4 |
| `showRouteForm(basicId?, isMakeCopy)` | Форма маршрута (FormScreen) | 5 |
| `showEmptyLocoForm(basicId)` / `showChangedLocoForm(loco)` | Локомотив (FormLocoScreen) | 1 |
| `showEmptyTrainForm(basicId)` / `showChangeTrainForm(train)` | Поезд (FormTrainScreen) | 6 |
| `showEmptyPassengerForm` / `showChangePassengerForm` | Пассажиром (FormPassengerScreen) | 7 |
| `showEmptyOtherWorkForm` / `showChangeOtherWorkForm` | Прочая работа (FormOtherWorkScreen) | 8 |
| `showPartnersManage` / `showPartnerPicker(basicId)` / `showNewPartnerEditor` / `showEditPartnerEditor(id)` | Напарники (справочник/выбор/редактор) | 8.5 |
| `showAllRoute` | Все маршруты (AllRouteScreen) | 9 |
| `showSearch` | Поиск (SearchScreen) | 10 |
| `showSalaryCalculation` | Расчёт зарплаты (SalaryCalculationScreen) | 11 |
| `showSettingSalary` | Настройки зарплаты (SettingSalaryScreen) | 12 |
| `showStatistics` | Статистика (StatisticsScreen) | 13 |
| `showCalendar` | Календарь (CalendarScreen) | 14 |
| `showScheduleWizard` | Мастер «Заполнить месяц» | 15 |
| `showAbsence` | «Новое отвлечение» (`AbsenceScreen`); в iOS — тоже `AbsenceScreen` | 16.4 |
| `showSelectReleaseDayScreen` | `SelectReleaseDaysScreen` — 🚫 мёртвый код Android, в iOS не реализуется | 16.2 |
| `showSettings` и под-разделы (см. ниже) | Настройки (вкладка нижнего меню) | 17 |
| `showPurchasesScreen` | Покупки/подписка (только через гейт, см. ниже) | 19 |
| `showProfile` | Профиль (вкладка нижнего меню) | 18 |
| `showSignIn` / `showLogIn` | 🚫 мёртвый код Android: маршруты в NavHost не зарегистрированы, вызов привёл бы к крэшу (BUG-09); вход — в Профиле (§18.0) | 20 |
| `showRouteDetails(basicData)` | `DetailsRoute` — 🚫 мёртвый код Android, в NavHost нет | 22 |
| `back()` / `navigationUp()` | возврат по стеку (`popBackStack` / `navigateUp`) | — |

Под-разделы Настроек открываются как `SettingsScreenRoute` с аргументом
под-экрана: `showSettingsRoute` → `ROUTE`; `showSettingsRouteForm` →
`ROUTE_FORM`; `showSettingsLoco(series?)` → `LOCOMOTIVE` либо
`LOCOMOTIVE_SERIES_<имя>` (при непустой серии); `showSettingsTrain` → `TRAIN`;
`showSettingsRest` → `REST`; `showSettingsSeriesList` → `SERIES_LIST`;
`showSettingsSeriesEditor(id)` → `SERIES_EDITOR_<id>`;
`showCreateSettingsSeriesEditor(name)` → `SERIES_NEW_<имя>`;
`showSettingsStationList` → `STATION_LIST`; `showSettingsStationEditor(id)` →
`STATION_EDITOR_<id>` (те же редакторы, что в пикерах шторки времени, §2.10).

- `showPartnersManage()` — полноэкранный справочник напарников (из Настроек);
  `showPartnerPicker(basicId)` — экран мультивыбора напарников в маршрут (из формы);
  `showNewPartnerEditor()` / `showEditPartnerEditor(id)` — экран создания/редактирования
  записи справочника. Все три — отдельные nav-destination (не под-экраны Настроек).
- **Гейт покупок**: любой переход на экран покупок идёт через
  `rememberShowPurchasesScreen(router)`, а не через `router.showPurchasesScreen()`
  напрямую. Хелпер проверяет авторизацию (`RouteActionsHelper.isAuthorized()` —
  непустой bearer-токен в защищённом хранилище):
  - токен есть → `showPurchasesScreen()`;
  - токена нет → диалог «Нужен вход в аккаунт» («Войти» → `showProfile()`,
    «Отмена» → остаться на месте). В Профиле показывается форма входа/регистрации.

  Причина: подписка живёт на сервере и привязана к аккаунту, срок
  (`subscriptionPeriod`) приезжает в приложение только при синхронизации
  авторизованного пользователя. Оплата без входа спишет деньги, но срок
  в приложении не обновится.
  Через гейт проходят все точки входа: карточки подписки/бесплатного лимита на
  Главном (4.5), нижние шторки в форме маршрута и «Все маршруты», мастер
  «Заполнить месяц», строка подписки в Профиле, диалоги «Бесплатный лимит
  исчерпан» / «Пробный период» при создании маршрута.
- `showProfile()` — переход на корневую вкладку «Профиль» с той же семантикой,
  что и нижнее меню (`popUpTo(startDestination) { saveState }`, `launchSingleTop`,
  `restoreState`); если пользователь уже на Профиле — no-op.

### 3.3. Инварианты, общие для всех форм

Все под-формы маршрута (Локомотив, Поезд, Пассажиром, Прочая работа) следуют
одному контракту (эталон — Локомотив, раздел 1):

1. **Автосохранение с дебаунсом 500 мс** (`changesHave()` → `triggerAutoSave()`).
2. **Guard пустого объекта**: полностью пустой новый объект не сохраняется при
   выходе (`isXEmpty()` в каждой VM), чтобы «открыл-закрыл» не плодил мусор.
   ⚠️ Предзаполненные по умолчанию значения не считаются вводом. В «Прочей работе»
   тип работы предзаполняется последним выбранным (`getLastOtherWorkType`), поэтому
   `onCleared` дополнительно требует `changesHaveState` — новая запись без реальных
   правок пользователя не сохраняется, даже если тип непустой.
3. **Финальное сохранение в `onCleared()`** через `NonCancellable + IO`.
4. **Выход без подтверждения**: кнопка «Готово» и системный Back просто
   закрывают форму — всё уже сохранено автосейвом и `onCleared`. 🚫 Мёртвый код Android — в
   iOS не реализуется: компонент `ConfirmExitDialog` («Внимание» /
   «При выходе все несохранённые данные будут утеряны…», «Выйти» / «Сохранить и выйти») и
   флаги `confirmExitDialogShow` / `exitWithoutSaving()` остались в коде, но ни одна форма
   их не показывает и не вызывает.
5. **Автодополнение станций/серий**: общий список станций/серий из `UserSettings`,
   фильтр по префиксу (`startsWith`, ignoreCase), удаление строки из общего списка
   через `removeStation`/`removeLocomotiveSeries`.
6. Времена — `Long` (epoch ms) с точностью до минуты: пикеры возвращают значение с
   обнулёнными секундами (форма маршрута дополнительно обрезает
   `truncateToMinute`).

### 3.4. Создание нового маршрута (кнопка «+»)

Единое действие для центральной кнопки нижнего меню (и других «добавить
маршрут», см. §9, §5) — `RouteActionsHelper.newRouteClick()`:
- подписка активна, если `subscriptionPeriod ≠ 0` и `subscriptionPeriod + 24 ч ≥
  now` (грейс 1 сутки) → сразу открыть новую форму маршрута;
- иначе считается число всех локальных маршрутов **включая удалённые в
  корзину** (`listRouteWithDeleting`); лимит `FREE_ROUTES_LIMIT = 20`:
  - `≥ 20` → диалог «Бесплатный лимит исчерпан» / «Для добавления новых
    маршрутов оформите подписку.», кнопки «Оформить подписку» (через гейт
    покупок) / «Отмена»;
  - `< 20` → диалог «Пробный период» / «Осталось бесплатных маршрутов: N из 20.
    Оформите подписку для неограниченного использования или продолжите
    бесплатно.», кнопки «Продолжить бесплатно» (открыть форму) / «Оформить
    подписку» (через гейт покупок);
- ошибка — ничего не происходит.
Из нижнего меню форма открывается с `popUpTo(start) { saveState }` +
`launchSingleTop`, чтобы после неё вкладки восстанавливались корректно.

Источник: `domain/.../navigation/Router.kt`, `app/.../ui/navigation/RouterImpl.kt`, `app/.../ui/LocoDriverApp.kt`,
`app/.../MainActivity.kt`, `app/.../viewmodel/MainViewModel.kt`, `features/route/.../navigation/{Navigation,HomeDestination,FormLocoDestination,PurchasesEntry,UpdatePresentationBlockdestination}.kt`,
`.../navigation/login/{FirstPresentationBlockDestination,Routes}.kt`, `.../component/{BottomNavigationBar,ConfirmExitDialog}.kt`,
`.../ui/{UpdatePresentationBlockScreen,login/FirstPresentationBlockScreen}.kt`, `core_android/.../ui/component/{PresentationBlock,OnBoardingItems}.kt`,
`.../viewmodel/RouteActionsHelper.kt`.

---

## 4. Главный экран (HomeScreen)

Дашборд выбранного рабочего месяца: отработанное время и «К выдаче», карусель
метрик, карточки-уведомления, живые блоки «текущий/следующий маршрут» и
«отдых», два последних маршрута месяца, инструменты.

### 4.1. Данные, загрузка, фоновая синхронизация

- **ViewModel**: `HomeViewModel` (`KoinComponent`). Реактивно наблюдает
  `UserSettings` + `SalarySetting` (`combine`), список маршрутов выбранного месяца
  (`routeUseCase.routeListByMonthFlow(month, TimeCalculationContext)`, с
  `debounce(300)` и `flatMapLatest` по паре (месяц, контекст)) и отдельно **все**
  маршруты (`getListRoutesAsFlow`) — для живых блоков, счётчика
  несинхронизированных и бесплатного лимита.
- Маршруты месяца = не удалённые маршруты, пересекающиеся с месяцем в
  `crossMonthTZ` (`start < конец месяца` и (`сдача == null` или `сдача ≥ начало
  месяца`)), отсортированы по явке **по убыванию**.
- Выбранный месяц — `UserSettings.selectMonthOfYear` (сохраняется при смене).
  Доступные месяцы — все `MonthOfYear` из календаря (`loadFlowMonthOfYearListState`):
  отдельные отсортированные списки месяцев и лет для шторки и упорядоченный
  список пар (год, месяц) для стрелок.
- **Состояния экрана**: пока не загружены обе настройки — скелетон
  (`HomeScreenSkeleton`: плашка 140dp вместо карусели, индикатор из 3 точек и 4
  скелетона карточек `SkeletonItemHomeScreen`, шиммер 1 с). После этого экран
  больше не возвращается в скелетон: при смене месяца показываются старые данные,
  пока грузятся новые. Ошибка загрузки маршрутов — полноэкранная ошибка
  (иконка + «Что-то пошло не так…»). Отдельные метрики до расчёта показывают
  маленький спиннер, при ошибке — текст «Ошибка».
- Все тяжёлые расчёты идут параллельно (`Dispatchers.Default`) и реактивно
  пересчитываются при изменении маршрутов месяца, настроек зарплаты/пользователя
  (debounce 150 мс, без повторной загрузки из БД) и при смене соседних маршрутов
  месяца (переотдых на стыке месяцев).
- **Фоновая синхронизация при открытии экрана** (`syncOnScreenOpen`): при каждом
  входе на Главный, если нет идущей синхронизации и прошло ≥ 5 мин с последней
  успешной (или есть неотправленные настройки), через 1,5 с проверяются активная
  подписка (`subscriptionPeriod > now`, без грейса) и токен; при успехе —
  двусторонняя синхронизация. Пока идёт — тонкий `LinearProgressIndicator` сразу
  под верхней строкой; модального диалога нет. Ошибка → snackbar с понятным
  текстом; если сервер «потерял» маршруты → snackbar «На сервере пропало маршрутов: N.
  Проверьте корзину в Настройках.» (⚠️ BUG-10: корзина — в Профиле, §9.4). Уход с экрана
  отменяет фоновую
  синхронизацию. Изменения локальной БД сразу попадают в список и расчёты.
- При инициализации VM: однократно (флаг в SharedPreferences) собирает серии и
  станции из всех маршрутов в списки автодополнения; проверяет обновление в RuStore
  (flexible): когда загружено — snackbar «Обновление загружено» с действием
  «Установить».

### 4.2. Метрики месяца (все в `HomeUiState`)

Считаются по списку месяца, отфильтрованному `filterByConsiderFutureRoute`: при
`isConsiderFutureRoute = true` — все маршруты месяца, иначе только с `явка <
now`.

- `totalTimeWithHoliday` — отработано всего = Σ `getWorkTime()` по маршрутам; для
  переходных — только часть внутри месяца + проезд пассажиром до явки.
- `timeWithoutHoliday` (`totalTime` в UI) — отработано без праздничных часов.
- `nightTimeInRouteList` — ночные (по `UserSettings.nightTime`, `localTZ`).
- `holidayHours` — работа в праздники; `dayOffHours` — часы личных выходных
  («Выходной» ×2). ⚠️ Эти две метрики считаются, но на Главном не выводятся.
- `singleLocomotiveTimeState` — следование резервом (одиночным локомотивом).
- `extendedServicePhaseTime` / `heavyTrainsTime` / `longDistanceTrainsTime` —
  удлинённое плечо / тяжеловесные / длинносоставные (через
  `SalaryCalculationHelper`, по настройкам доплат).
- `onePersonOperationTime` — «в одно лицо» (грузовые + пассажирские; вся смена без
  перерыва, пассажиром и ожидания 018M — то же правило, что в расчёте зарплаты).
  ⚠️ Считается, но на Главном не выводится.
- `passengerTimeInRouteList` — время следования пассажиром.
- `toBeCredited` — «К выдаче» (`SalaryCalculationHelper.getMoneyToBeCredited()`,
  с соседними маршрутами для переотдыха — чтобы совпадало с экраном расчёта).
- `normaHours` — норма часов месяца (через `NormaUseCase`: регион + отвлечения +
  производственный календарь; реактивно по выбранному месяцу). Пока `null` —
  используется `MonthOfYear.getPersonalNormaHours()`. «Выходной» и «Технические
  занятия» норму не уменьшают — см. §16.1.1.
- `todayWorkTime` — только при `isConsiderFutureRoute`: отработано по маршрутам
  месяца, **уже завершённым** (`сдача ≤ now`).
- ⚠️ Поведение Android (возможный баг, BUG-11): при ошибке расчёта
  праздничных часов в состояние ошибки ставится поле ночных часов (опечатка).
- Виджет главного экрана перерисовывается по сигналу `WidgetUpdater`, но цифры
  берёт не из состояния экрана: `WidgetDataLoader.loadAndPush` заново читает БД
  (значения, которые `HomeViewModel.pushWidgetData` передаёт в `update(...)`,
  игнорируются). Чтобы «отработано» и «до нормы» совпадали с экраном, виджет обязан
  считать по тем же правилам: `TimeCalculationContext.from(userSettings)`, отбор
  маршрутов месяца через `UtilsForEntities.filterByMonth` (границы месяца в
  `crossMonthTZ`, а не в жёстко зашитом GMT+3), время —
  `calculateWorkTimeWithSettings`, норма — через `NormaUseCase`.
  ⚠️ Поведение Android (возможный баг, BUG-12): `HomeViewModel.pushWidgetData`
  берёт название месяца как `monthNames[month − 1]` при 0-based месяце (соседний месяц);
  сейчас не проявляется — виджет эти данные игнорирует.

### 4.3. Живые блоки состояния (счётчики реального времени)

Блоки не зависят от выбранного месяца: считаются по всем маршрутам
(`updateCurrentAndNextRoute`/`recomputeRestBlock` вызываются из коллектора всех
маршрутов И из коллектора настроек — идемпотентно; это защита от гонки старта:
если список маршрутов эмитится до настроек, без повторного вызова «Следующий
маршрут» навсегда завис бы в `null`). Таймеры перезапускаются только при реальной
смене маршрута (id или явка).

- **Текущий маршрут** (`findCurrentRoute`, §0.3): заголовок «ТЕКУЩИЙ МАРШРУТ»
  (тап → форма маршрута) и горизонтальный ряд плиток 150×150dp:
  - **«НА РАБОТЕ»** — секундомер `ЧЧ:ММ` = `now − явка`, обновляется на границе
    каждой минуты и считается от РЕАЛЬНОГО времени (не накопительно, чтобы не
    отставать после блокировки/Doze); при возврате на экран (`RESUMED`)
    перезапускается. Под ним полоса прогресса `часы/12` (красная после 12 ч).
    Тап → форма маршрута. При наступлении `timeEndWork` (в т.ч. пока телефон был
    заблокирован) — `handleRouteEnded()`: блок исчезает, пересчитываются
    `todayWorkTime`, «Следующий маршрут» и отдых без обращения к БД.
  - **«ЛОКОМОТИВ» / «ПОЕЗД» / «ПАССАЖИРОМ»** — иконка, подпись, имя последней
    единицы (моноширинный, автоуменьшение 17→11sp), подзаголовок станций; при
    нескольких единицах — бейдж-счётчик и «стопка» (вторая карточка выглядывает
    справа сверху); пустая плитка — пунктирная рамка. Имена:
    локомотив `серия-номер` / `серия б/н` / `Электротяга|Теплотяга номер` /
    `Электротяга|Теплотяга N` (N — порядковый); поезд/пассажир `№номер` (тогда
    подзаголовок — станции плеча или «первая — последняя»), иначе станции плеча,
    иначе «A — B» / «A — » / « — B», иначе «б/н». Круглая «+» в углу —
    добавить новую единицу. Тап по плитке: 0 единиц → новая форма; 1 → её форма;
    > 1 → шторка списка «Локомотивы · N» / «Поезда · N» / «Пассажиром · N»
    (строки с иконкой и именем, внизу пунктирная «+ Добавить локомотив|поезд|
    пассажиром»; выбор закрывает шторку, затем навигация).
  - **Перестановка плиток**: долгое нажатие на любую плитку начинает её
    перетаскивание (тактильный отклик); все плитки покачиваются (±1,2°, 110 мс),
    удерживаемая увеличивается до 1,04 и следует за пальцем; сдвиг на ≥ 81dp
    меняет её местами с соседней (отклик). Режим остаётся после отпускания —
    можно сразу брать другие плитки; клики по плиткам и «+» в режиме
    отключены; касание вне всей секции «Текущий маршрут» завершает режим и не
    выполняет действие под касанием. У краёв (36dp) ряд автопрокручивается шагом
    18dp, захваченная плитка удерживается в видимой области. Порядок (4 ключа
    `work`, `loco`, `train`, `passenger`) сохраняется локально
    (SharedPreferences), восстанавливается после перезапуска, не синхронизируется.
    Пока порядок не меняли: «На работе» первой, затем заполненные единицы, потом
    пустые в порядке «Локомотив» → «Поезд» → «Пассажиром».
- **Приоритет блоков** (когда нет текущего маршрута): Отдых в ПО > Следующий
  маршрут > Домашний отдых. Т.е. если во время домашнего отдыха уже известна
  следующая явка — вместо «Домашний отдых» показывается «Следующий маршрут»;
  отдых в ПО этим не перекрывается (сам учитывает явку обратного маршрута через
  границу окна).
- **Следующий маршрут** (`findNextFutureRoute`): заголовок «СЛЕДУЮЩИЙ МАРШРУТ» и
  карточка «ДО ЯВКИ ОСТАЛОСЬ» + обратный отсчёт `ЧЧ:ММ` (`явка − now`,
  обновление на границе минуты от реального времени), разделитель, строка
  «Явка» `dd.MM HH:mm`. Тап по карточке/заголовку → форма маршрута. При
  обнулении маршрут сразу становится «текущим» без ожидания БД.
- **Блок отдыха** (`recomputeRestBlock`): берётся последний завершённый маршрут
  (`сдача < now`, максимальная сдача) и его флаг `restPointOfTurnover`.
  - **«ОТДЫХ В ПУНКТЕ ОБОРОТА»** (флаг = true): `короткий = max(ceil_мин(
    getWorkTime/2), минимум)`, `полный = max(getWorkTime, минимум)`, где минимум =
    `minTimeRestPointOfTurnover` (3 ч), а если предыдущий по явке маршрут тоже
    закончился отдыхом в ПО — `minTimeRestPointOfTurnoverSecond` (4 ч). Окончания
    = `сдача + длительность`. Считать именно от `getWorkTime` (сдача − явка −
    перерыв + проезд пассажиром до явки), а НЕ от «сдача − явка». Окно показа —
    до явки следующего маршрута, а если его нет — до конца полного отдыха.
    Карточка: «ОТДЫХАЕТЕ» + счётчик `now − сдача` (обновление раз в секунду),
    «начало отдыха dd.MM HH:mm», шкала (прогресс `(now − сдача)/(полный −
    сдача)`, зелёная точка короткого, оранжевая точка в конце, подписи `HH:mm`
    начала/короткого/полного), строки «Короткий отдых» и «Полный отдых»: «до
    dd.MM HH:mm» и «осталось ЧЧ:ММ».
    ⚠️ Поведение Android (возможный баг, BUG-13): «округление вверх до
    минуты» в коде — если половина не кратна минуте, к ней прибавляется целая минута (а не
    округляется); при нечётном числе минут работы окончание получается с `:30` секунд (на
    экране не видно).
  - **«ДОМАШНИЙ ОТДЫХ»** (флаг = false): цепочка = этот маршрут + идущие перед ним
    подряд маршруты с `restPointOfTurnover = true` (из выбранного и предыдущего
    месяца); `длительность = max(Σ(сдача − явка) × 2.6 − Σ отдыхов между ними,
    minTimeHomeRest)`; `полный = сдача + длительность`; `минимальный = сдача +
    minTimeHomeRest` (16 ч). Окно показа — до полного. Карточка: как выше, на
    шкале оранжевая точка минимального; строки «Минимальный отдых» и «Полный
    отдых».
    ⚠️ Поведение Android (возможный баг, BUG-14): цепочка берётся из маршрутов
    **выбранного в UI** месяца и предыдущего, а не месяца самого маршрута — при просмотре
    другого месяца расчёт может отличаться. Здесь берётся `сдача − явка`, а не
    `getWorkTime`.
  - Автоскрытие по достижении границы окна (`restTransitionJob`). При крупном
    шрифте строки отдыха раскладываются в столбец.
  Единый расчёт — в `UtilsForEntities.fullRest`/`shortRest` и
  `RouteActionsHelper.calculate*Rest`/`calculationHomeRest`; виджет
  (`LocoDriverWidget.computeRestHome`) обязан считать так же.
- **Кнопки «GO» на Главном нет.** `onGoClicked` (запись времени отправления/
  прибытия на станции последнего поезда текущего маршрута) используется только
  виджетом (`GoActionCallback`), см. §22/виджет.

### 4.4. UI сверху вниз

Единый внешний горизонтальный отступ секций — `8.dp`; между секциями 32dp.
Весь экран поддерживает pull-to-refresh (§4.6).

1. **Верхняя строка**: слева логотип «М» + «Машинист» (тап → шторка «Перейти на
   сайт?» / «Будет выполнен переход на официальный сайт приложения
   locodriver.ru», действие «Перейти» открывает `https://locodriver.ru`); справа
   иконка поиска → `showSearch`. Под строкой — индикатор фоновой синхронизации
   (§4.1).
2. **Строка месяца**: крупно «Месяц» (именительный: «Январь»…) и год (приглушённо);
   тап → шторка выбора (ниже). Справа стрелки `‹` `›` — предыдущая/следующая
   пара (год, месяц) из списка доступных; на краях неактивны (приглушены).
   - **Шторка месяца**: «Выберите месяц и год», чипы месяцев (из доступных),
     чипы лет, кнопка «Применить» → `setCurrentMonth(год, месяц)` и закрытие.
     Выбор чипа сам по себе ничего не применяет. Если выбранной пары нет в календаре —
     ничего не происходит (BUG-15).
3. **Блок «ОТРАБОТАНО»**: строка «ОТРАБОТАНО» слева и сумма «К выдаче» справа
   (`toMoneyString`, §0.1; пока считается — «считаем деньги»; ошибка — «—»).
   Ниже крупно (46sp) отработано всего `totalTimeWithHoliday`; если есть
   праздничные часы — рядом ` (без_праздн + праздн)`; справа чип «еще ЧЧ:ММ»
   (не хватает до нормы) или «сверх ЧЧ:ММ», считается от `totalTime` (без
   праздничных) и `normaHours`; при норме 0 чипа нет. Тап по числу — подсказка
   «Общее отработанное время», по разбивке — «Рабочие + праздничные часы».
   Раскладка адаптивна: если не помещается, чип уходит строкой выше, разбивка —
   под число.
4. **Карусель метрик** (3 страницы, свайп; под ней линейный индикатор страниц).
   Тап по карусели → «Статистика» (§13). Каждая страница — карточка с тремя
   строками «подпись … значение» и тонкой полосой прогресса под каждой (высота
   карусели постоянна). Отступ страниц `12.dp`.
   - **Стр. 1**: «Норма на месяц» `N ч.` (прогресс `totalTime / норма`);
     «Норма на dd.MM.yy» (сегодня) `N ч.` — норма по календарю на сегодня
     (прогресс `totalTime / норма_на_сегодня`); третья строка: при
     `isConsiderFutureRoute` — «Отработано на dd.MM.yy» `todayWorkTime`
     (прогресс к норме на сегодня), иначе «Осталось до нормы» / «Сверх нормы»
     `|норма − totalTime|` (прогресс `|разница|/норма`). Здесь `totalTime` —
     **без праздничных часов**.
   - **Стр. 2**: «Ночные», «Пассажиром», «Резервом» (одиночное следование) —
     прогресс = доля от `totalTimeWithHoliday`.
   - **Стр. 3**: «Удл. плечи обслуживания», «Длинносоставные», «Тяжелые» — то же.
   - `MetricInfoSheet` (шторка пояснения метрики) на Главном **не используется** —
     он есть только на экране «Статистика» (§13).
5. **Карточки-уведомления** (карусель, если их больше одной; с индикатором).
   Каждая: иконка, заголовок, крестик «Закрыть», текст, подсказка, кнопка.
   Скрытие крестиком — до изменения соответствующего состояния (числа/статуса).
   - **Подписка** (если подписка когда-либо была, `subscriptionEndTime ≠ 0`):
     осталось `ceil(дней) ≤ 7` → оранжевая «Подписка заканчивается»: «Осталось N
     день|дня|дней — до dd.MM.yy» (или «Заканчивается сегодня, dd.MM.yy»),
     «Продлите заранее — новый срок прибавится к текущему, дни не сгорят.»,
     кнопка «Продлить»; истекла → красная «Подписка закончилась»: «Закончилась
     dd.MM.yy», «Маршруты и история сохранены. Но добавлять новые и пользоваться
     синхронизацией нельзя, пока подписка не возобновлена.», «Возобновить».
   - **Бесплатный период** (подписки никогда не было, `subscriptionEndTime = 0`),
     used = `freeRoutesUsedCount` (все маршруты, включая корзину), лимит 20,
     полоса прогресса `used/20`: used ≥ 20 → красная «Бесплатный лимит исчерпан»
     / «Использовано 20 из 20 бесплатных маршрутов» / «Маршруты и история
     сохранены. Чтобы добавлять новые и пользоваться синхронизацией — оформите
     подписку.»; осталось ≤ 5 → оранжевая «Бесплатный период» / «Осталось N из 20
     бесплатных маршрутов» / «Оформите подписку заранее, чтобы не потерять
     возможность добавлять маршруты.»; иначе нейтральная «Бесплатный период» /
     «Использовано N из 20 бесплатных маршрутов» / «Оформите подписку в любой
     момент — снимет лимит и откроет синхронизацию.». Кнопка «Оформить подписку».
   - **Не синхронизировано** (подписка активна и несинхронизированных маршрутов
     > 2): «Внимание!» / «Не синхронизировано маршрутов: N» / «Проверьте
     подключение к интернету и выполните синхронизацию.», кнопка
     «Синхронизировать» → ручная синхронизация (§4.6).
   - Кнопки «Оформить подписку/Продлить/Возобновить» ведут на покупки только у
     авторизованного пользователя; без входа — диалог «Нужен вход в аккаунт»
     (гейт покупок, §3.2).
6. Живые блоки (§4.3): «ТЕКУЩИЙ МАРШРУТ» или (по приоритету) «ОТДЫХ В ПУНКТЕ
   ОБОРОТА» / «СЛЕДУЮЩИЙ МАРШРУТ» / «ДОМАШНИЙ ОТДЫХ».
7. **«ПОСЛЕДНИЕ МАРШРУТЫ»** и справа кнопка «Все (N)» (N — число маршрутов
   месяца) → «Все маршруты» (§9). Показываются **только первые два** маршрута
   месяца (по убыванию явки, в т.ч. будущие) карточками `ItemHomeScreen` (§4.5),
   с номерами `#N` и `#N−1`. Пусто — текст по центру «Список пуст / Нажмите  +
   чтобы добавить маршрут / или создайте график работы».
8. **«ИНСТРУМЕНТЫ»** — горизонтальный ряд квадратных карточек (сторона ≈ ⅓
   ширины экрана, одинаковая высота): «Календарь» → §14; «Статистика» → §13;
   «PDF» → выбор содержимого PDF (§21; карточка неактивна, пока PDF
   формируется); «Поиск» → §10.
9. Отступ 50dp снизу. Кнопки «+» на самом экране нет — новый маршрут создаётся
   центральной кнопкой нижнего меню (§3.4).

Snackbar-сообщения экрана показываются по очереди (не перебивают друг друга).

### 4.5. Карточка маршрута `ItemHomeScreen`

Общий компонент Главной, «Все маршруты» и Календаря.
- **Фон**: будущий маршрут (`явка > now + timeZone`) — `surfaceBright`;
  переходный — `surfaceDim`; иначе обычный. ⚠️ Поведение Android (возможный баг, BUG-16):
  `isFuture` прибавляет к
  `now` смещение пояса пользователя от Москвы (`UserSettings.timeZone`), хотя оба значения
  — абсолютные моменты (затрагивает пользователей не из GMT+3, напр. KZ).
- **Заголовок**: «явка − сдача» и справа продолжительность. Явка —
  `dd.MM HH:mm`; сдача — `HH:mm`, если в тот же день (в поясе отображения),
  иначе `dd.MM HH:mm`. Продолжительность = `getWorkTimeInMonth` (для переходного
  — только часть в месяце) в формате §0.1. Шрифт одинаковый (`bodyLarge` 18sp,
  Medium), без фона у итога. Если строка не помещается в ширину без
  уменьшения — явка и сдача (обе с датой) столбцом слева, итог справа; иначе
  одна строка с автоуменьшением до 10sp.
- **Свёрнутый вид** (Главная, Календарь): одна строка моноширинным 14sp —
  поезд с самой поздней отправлением с первой станции: «№номер Первая —
  Последняя» (пустые части пропускаются); если поездов нет — первая «Прочая
  работа»: «Тип — Станция».
- **Развёрнутый вид** (только «Все маршруты», `isExpand`): группы
  «ЛОКОМОТИВЫ» (серия или «Локомотив» · номер), «ПОЕЗДА» (№ или «Поезд»,
  станции), «ПАССАЖИРОМ» (№ или «Поездка», станции), «ПРОЧАЯ РАБОТА» (тип,
  станция), примечание маршрута, блок «Расчёт за смену» (§9).
- **Нижняя строка**: слева `#номер` (порядковый, от количества), справа значки
  (долгое нажатие на значки → шторка «Обозначения», `RouteLegendSheet`, §9.3):
  праздник, перерыв (`перерыв > 0`), длинносоставный, тяжеловесный, удлинённое
  плечо, «в одно лицо», пассажиром (`время пассажиром > 0`), «орден» (работа
  > 12 ч), толкач, двойная тяга, сдвоенный поезд, избранное, статус синхронизации
  (синхронизирован / нет).
- **Жесты**: тап → форма маршрута; долгое нажатие → быстрый просмотр
  `RouteQuickViewSheet` (§9.1; на Главном при открытии пересчитываются домашний
  отдых, фактический отдых и оплата смены); свайп влево → красная кнопка
  удаления → шторка «Удалить маршрут?» / «от dd.MM HH:mm» / «Да, удалить».
  Удаление — soft-delete в корзину, snackbar «Маршрут перемещён в корзину».
- **Действия из быстрого просмотра** (шторка сначала закрывается): избранное
  (snackbar «Маршрут добавлен в избранное» / «Маршрут удален из избранного»);
  копия → шторка «Создать копию маршрута?» / «Откроется редактирование копии —
  исходный маршрут не изменится.» / «Создать копию» → `showRouteForm(id,
  isMakeCopy = true)`; поделиться → без токена snackbar «Неавторизованный
  пользователь», иначе создание публичной ссылки (повторный запрос во время
  создания игнорируется) и системный share-sheet, ошибка → «Не удалось создать
  ссылку»; синхронизация одного маршрута → без токена «Войдите в аккаунт, чтобы
  синхронизировать маршрут», без подписки «Синхронизация доступна по
  подписке», успех «Маршрут сохранен в облаке», иначе текст ошибки; удаление →
  шторка выше.
- **Скелетон** `SkeletonItemHomeScreen` — карточка min 65dp: полоса 70% + 20%
  высотой 18dp и полоса 50% высотой 14dp, шиммер.
- PWA: чтобы long-press не конфликтовал с прокруткой/тапом, быстрый просмотр можно
  дополнительно продублировать явной кнопкой.

### 4.6. Синхронизация с Главного

- **Pull-to-refresh** (`PullToSyncViewModel.refresh`, без cooldown): без
  активной подписки → «Синхронизация доступна по подписке»; без токена →
  «Необходимо войти в профиль»; иначе двусторонняя синхронизация, по итогу
  сообщение: текст ошибки / «Синхронизация не выполнена: сервер не завершил
  обработку данных» / «На сервере пропало маршрутов: N. Проверьте корзину в Настройках.»
  (BUG-10) / «Синхронизация завершена». Сообщение показывается snackbar'ом
  внутри контейнера того экрана, где сделан жест; в глобальную очередь не
  ставится. Повторный жест во время синхронизации игнорируется.
- **Ручная синхронизация** («Синхронизировать» в карточке несинхронизированных,
  `manualSync`) — **выгрузка** на сервер (`syncToRemote`): без подписки →
  snackbar «Синхронизация доступна по подписке»; без токена → диалог с ошибкой
  «Неавторизованный пользователь». Иначе `SyncProgressDialog` в режиме
  процентов: заголовок «Выгрузка данных», название текущего шага («Настройки
  пользователя» → «Настройки зарплаты» → «Отвлечения» → «Маршруты»; до старта —
  «Подготовка», в конце — «Завершено»), линейный прогресс = доля завершённых
  шагов из 4, «N%», текст первой ошибки. Полный успех (есть timestamp и нет
  ошибок маршрутов) → диалог «Выгрузка завершена!» / «Данные успешно
  синхронизированы.» / «Отлично!» и сохранение времени последней синхронизации.
  Частичный успех по маршрутам → шаг «Маршруты» с ошибкой «синхронизировано N из
  M». Транспортная ошибка → экран «Нет интернета» / «Понятно»; 401 → экран
  «Сессия истекла» / «Войдите в аккаунт заново в разделе «Профиль». Данные на
  устройстве сохранены и будут синхронизированы после входа.» / «Понятно» (и
  разлогин, §18.5). По завершении с не-сетевыми ошибками — кнопка «Отправить
  отчет об ошибке» (письмо на `locodriver.app@yandex.ru` с файлом-отчётом) и
  «Понятно». Закрытие диалога сбрасывает состояние синхронизации.

Источник: `features/route/.../ui/{HomeScreen,HomeStateBlocks,SyncProgressDialog}.kt`,
`.../navigation/HomeDestination.kt`, `.../viewmodel/home_view_model/{HomeViewModel,HomeUiState}.kt`,
`.../viewmodel/{RouteActionsHelper,PullToSyncViewModel}.kt`, `.../component/{ItemHomeScreen,SkeletonHomeScreen,WorkedTimeHeader,PullToSyncContainer,RouteLegendSheet}.kt`,
`domain/.../entities/route/UtilsForEntities.kt`, `domain/.../use_cases/RouteUseCase.kt`,
`data_remote/.../remote_rest/SyncManager.kt`, `core_android/.../ui/component/{AsyncData,GenericError}.kt`.

---

## 5. Форма маршрута (FormScreen)

Корневая форма рейса (`Route`). Хранит `BasicData` (времена работы, номер, флаги,
заметки) и является контейнером под-разделов: Напарники, Локомотив, Поезд, Прочая
работа, Пассажиром. Отдельного раздела «Фото» в UI нет: `Route.photos` только
перечитывается из БД и сохраняется как есть (см. раздел 0 CLAUDE.md про `photos`).

### 5.1. Вход и данные

- **Навигация**: `showRouteForm(basicId?, isMakeCopy)`. Без `basicId` — новый
  маршрут. Открывается с Главного, «Все маршруты», Календаря, Поиска и по
  deep link шаринга (§3.1). После сохранения экран закрывается через `back()` —
  пользователь возвращается туда, откуда пришёл, а не всегда на Главный.
- **ViewModel**: `FormViewModel(routeId, isCopy, application)`.
  - `routeId == NULLABLE_ID` → новый маршрут: создаётся `Route(BasicData(id=UUID))`,
    в БД не пишется до первого изменения поля или перехода в дочерний раздел.
  - `isCopy` → загружается существующий маршрут и пропускается через
    `reidentifyForImport()`: **новые id** получают `BasicData`, все локомотивы (и их
    секции), поезда (и станции), пассажиры, прочая работа и напарники маршрута;
    сбрасываются `remoteRouteId`, `remoteObjectId`, `isSynchronized=false`,
    `isDeleted=false`, `isFavorite=false`. Времена и остальные данные копируются как
    есть. Копия тоже не пишется в БД до первого изменения.
  - Маршрут по публичной ссылке: `SharedRouteHolder.consume(routeId)` → режим
    shared preview (§5.6).
- **Заголовок топ-бара**: «Новый маршрут» — для нового маршрута (и копии, см. ⚠️
  ниже); «Маршрут б/н» — существующий без номера; «Маршрут · №{номер}» — с номером.
- ⚠️ **Поведение Android (возможный баг, BUG-17)**: в UI флаг копии берётся из
  `RouteFormUiState.isCopy`, который ViewModel никогда не выставляет (всегда
  `false`). Поэтому у копии заголовок совпадает с оригиналом («Маршрут · №N»), а
  задуманный автоматический пикер «Явка» для копии (выбранная явка + сдача =
  явка + `getPureWorkTime()` оригинала) **не открывается**. Копия сохраняется с
  той же явкой, что у оригинала, и при сохранении срабатывает шторка дубля (§5.6).
- Реактивно наблюдает настройки (`UserSettings`), настройки зарплаты
  (`SalarySetting`, живой Flow — смена «среднего часа» на другом экране сразу
  пересчитывает форму) и текущий маршрут. При изменении любого из трёх параллельно
  пересчитывает: зарплату (§5.4), ночные часы, праздничные часы, домашний отдых,
  короткий/полный отдых в ПО, флаг «второй отдых в ПО подряд», фактический отдых,
  валидность времени, время пассажиром, предупреждение «вторая ночь подряд».

### 5.2. Сохранение (persistence) — важные инварианты

- **Автосейв дебаунс 500 мс** (`triggerAutoSave`) после любого изменения поля
  (`changesHave()`). Новый/копия пишется в БД только при первом изменении; после
  этого — `subscribeToChanges` (подписка на дочерние сущности из БД).
- **Общие правила `RouteUseCase.saveRoute`** (все пути записи маршрута):
  - если явка и сдача заданы и `timeEndWork < timeStartWork` — запись отклоняется
    с ошибкой «Окончание работы раньше начала. Невозможно сохранить маршрут.»;
  - если `timeStartWork == null`, в БД пишется **`timeStartWork = now`** (текущий
    момент) — маршрут без явки не сохраняется (⚠️ Поведение Android (возможный баг,
    BUG-18): явка подставляется молча, пользователь об этом не узнаёт);
  - `isSynchronized=false`, `updatedAt=now` (часы LWW-merge);
  - если у какого-то пассажира `isWorkStartByArrival=true` и задано прибытие —
    `timeStartWork` принудительно = этому прибытию (§7.2).
- **`BasicData` держится в памяти**, из БД подтягиваются только под-разделы
  (`locomotives/trains/passengers/otherWorks/partners/photos`) — иначе автосейв→emit БД
  затирает ввод пользователя. Исключения, которые берутся из БД: `timeEndWork` (его
  пишет шторка сдачи локо), `timeStartWork` и `timeStartWorkBeforeArrival` (меняет
  экран «Пассажиром» при «явке по прибытию»).
- **Переход в дочерний раздел** (`preSaveRoute()`): если маршрут уже в БД и
  несохранённых изменений нет — запись не делается; иначе маршрут сохраняется,
  включается подписка, затем навигация. Так дочерняя форма всегда находит
  родителя по `basicId`.
- Удаление под-сущности (`onDeleteLoco/Train/Passenger/OtherWork/Partner`) — сразу из БД
  (иначе emit БД восстановит её в UI). Перед удалением — **подтверждение-шторка**
  (`AppBottomSheet` с заголовком/описанием и действием «Да, удалить», паттерн
  удаления маршрута), а не Material-диалог. Удаление физическое (у дочерних
  сущностей корзины нет).
- **«Готово» (`onSaveClick`)**:
  - нет несохранённых изменений и это не shared preview / не `isDeleted` → экран
    просто закрывается без записи;
  - иначе — проверка дубля по явке (§5.6), затем `performSave`: снимает
    `isDeleted` (shared preview), пишет маршрут, эмитит `RouteSaved` → `back()`.
    Через 400 мс (после анимации навигации) — snackbar «Маршрут сохранен»; если
    есть bearer-токен — фоновая `syncManager.syncRoute(id)` и при успехе snackbar
    «Маршрут сохранен в облаке». Ошибка записи → snackbar «Ошибка: {текст}».
  - После `RouteSaved` готовится запрос оценки RuStore, если число маршрутов
    (включая удалённые) `> 10` и кратно 5.
- **Системный Back** закрывает форму без диалога (`ConfirmExitDialog` — мёртвый код, §3.3
  п.4). `onCleared` дописывает маршрут только если он уже в БД И есть
  несохранённые изменения (`NonCancellable`). Новый маршрут без единого изменения
  в БД не попадает.
- **Флаг «явки по прибытию» (`Passenger.isWorkStartByArrival`) форме не принадлежит.**
  Включает его только экран «Пассажиром», поэтому перед КАЖДОЙ записью маршрута
  (автосейв, `preSaveRoute`, явное сохранение, финал в `onCleared`) форма перечитывает
  флаг из БД (`withPassengerArrivalFlagsFromDb`). Иначе устаревшая копия списка
  пассажиров в памяти возвращала выключенный пользователем режим обратно —
  переключатель «включался сам». Отключение с самой формы
  (`disableWorkStartByArrival`, подтверждение «Изменить время явки?») сразу пишет в БД
  снятый флаг у всех пассажиров и очищенный `timeStartWorkBeforeArrival`
  (вне `viewModelScope`, чтобы уход с экрана не отменил запись); все последующие
  записи маршрута ждут завершения этой операции.

### 5.3. UI-разделы (сверху вниз)

1. **Топ-бар** (`CenterAlignedTopAppBar`, фон `background`): слева текстовая кнопка
   **«Готово»** (цвет `tertiary`, вместо стрелки «‹») — `onSaveClick`; по центру
   заголовок (§5.1); справа шестерёнка «Настройки маршрута» → `Настройки → Маршрут`
   (`showSettingsRouteForm`).
2. **Баннер ошибки** — если `errorMessage != null` (§5.5): красная карточка
   (градиент `error` 0.9→0.72) с текстом ошибки цветом `onError`.
3. Заголовок группы **«ОСНОВНЫЕ ДАННЫЕ»** (UPPERCASE mono); если маршрут хотя бы
   частично в командировке (`SalaryForRouteState.isBusinessTrip`) — справа бейдж
   **«КОМ»** (цвет `#30B0C7`, как в календаре).
4. Карточка **«Номер»**: подпись слева, поле ввода справа (mono, выравнивание по
   правому краю, цифровая клавиатура, плейсхолдер «Введите номер»). Пустая строка
   хранится как `null` (`setNumber`).
5. Карточка **«Работа в одно лицо»** + `Switch` (`isOnePersonOperation`); вся строка
   кликабельна. Показывается только при `UserSettings.isShowOnePersonSwitch`.
6. Две плитки в ряд (при `fontScale > 1.15` — в столбец):
   - **«Расчёт»** (иконка — символ валюты страны `₽/₸/Br`): значение
     `totalPayment` в формате «1 234,56 ₽» (2 знака) при `isCalculated`, иначе
     «0 ₽» приглушённо (alpha 0.4). Тап → `CalcBottomSheet` (§11.9).
   - **«Отдых»** (иконка `hotel` при отдыхе в ПО, иначе `home`): «В пункте оборота» /
     «Домашний». Тап → `RestBottomSheet` (§5.7). **Переключатель типа отдыха
     (`restPointOfTurnover`) находится только в этой шторке**, на самой форме его нет.
7. **НАПАРНИКИ** (лёгкий блок, вариант D) — показывается при `UserSettings.isShowPartner`.
   Карточка со строками: аватар-инициалы (32dp) · короткое ФИО («Иванов И. И.», если
   ФИО пустое — «Напарник») · `таб. N` справа (mono, только если задан); последняя
   строка — accent «＋ Добавить напарника». Без крупной иконки-контейнера. Порядок
   строк — порядок добавления в маршрут. **Тап по строке** → редактор записи
   справочника (`showEditPartnerEditor(sourcePartnerId)`); если `sourcePartnerId == null`
   — тап ничего не делает; если запись справочника уже удалена — редактор откроется
   пустым. **Свайп влево** → «Удалить» → шторка «Удалить напарника?» + короткое ФИО,
   «Да, удалить» → копия удаляется из маршрута (справочник не меняется).
   «Добавить напарника» → `preSaveRoute()` → экран мультивыбора (§8.5.1).
8. Заголовок **«ВРЕМЯ РАБОТЫ»** и единая скруглённая карточка:
   - **Голубая шапка** (`primaryContainer`) — только если явка и сдача заданы:
     - «Пассажиром до явки» · `HH:MM` — если есть следование пассажиром вне окна
       «явка–сдача» (`getPassengerTimeOutsideWork() > 0`), с разделителем снизу;
     - **«Отработано»** · значение `getPureWorkTime()` = `floorToMinute(сдача) −
       floorToMinute(явка) − длительность перерыва`, в формате `HH:MM` или десятичном
       (`UserSettings.isDecimalTime`), крупно mono;
     - «В ночное время» (иконка луны) · `HH:MM` — если ночные часы > 0 (окно ночи из
       `UserSettings.nightTime`, по умолчанию 22:00–06:00, минус перерыв);
     - «Пассажиром» (иконка) · `HH:MM` — следование пассажиром внутри окна работы.
   - Строки (формат значения — `getDateAndTime` = `dd.MM.yy HH:mm` в поясе пользователя, mono; пусто → плейсхолдер «Выбрать»
     с alpha 0.4): **«Явка»**, **«Сдача»**, и при `UserSettings.isShowBreak` —
     **«Начало перерыва»**, **«Конец перерыва»**. Тап → `AppDateTimePicker`
     (заголовки «Явка», «Сдача», «Начало перерыва», «Окончание перерыва»; пояс
     `displayTimeZone`; начальное значение — текущее значение поля, иначе для сдачи —
     явка, для перерыва — начало перерыва/явка, иначе «сейчас»; recent-times по ключам
     `time_start_work`, `time_end_work`, `time_start_break`, `time_end_break`).
     Долгое нажатие на заполненной строке → шторка с заголовком «Время явки» /
     «Время сдачи» / «Начало перерыва» / «Окончание перерыва» и действием
     «Удалить значение».
   - Под «Явкой» подзаголовок **«по прибытию пассажиром»**, если хоть у одного
     пассажира включён `isWorkStartByArrival`. Тогда тап по «Явке» сначала открывает
     `RouteConfirmDialog` (§5.8) «Изменить время явки?»; «Изменить» →
     `disableWorkStartByArrival()` (§5.2) и пикер.
   - Все времена обрезаются до минуты. Если включена настройка **«Стандартное время
     работы»** (`usingDefaultWorkTime`), при каждом ручном указании или изменении
     явки сдача автоматически устанавливается как `timeStartWork + defaultWorkTime`
     (по умолчанию 12 ч); ранее заданная сдача заменяется. Поведение одинаково в
     Android- и iOS-форме.
   - Под карточкой — предупреждение **«Вторая ночь подряд»** (§5.5), если есть.
9. Секции дочерних сущностей — в **таком порядке**: **ЛОКОМОТИВ** (`isShowLocomotive`),
   **ПОЕЗД** (`isShowTrain`), **ПРОЧАЯ РАБОТА** (`isShowOtherWork`), **ПАССАЖИРОМ**
   (`isShowPassenger`). Каждая: заголовок группы и карточка; строка элемента = иконка
   типа в тональном контейнере 40dp · текст · кнопка «✕» (удаление с
   подтверждением); последняя строка «Добавить {название секции строчными}» и «›».
   Тап по строке → форма элемента; «Добавить» → `preSaveRoute()` → пустая форма.
   - Текст строки: локомотив — «{серия} №{номер}» (серия mono; без серии — тип
     «Электровоз/Тепловоз»), если нет ни серии, ни номера — «{тип} № {порядковый}»;
     поезд — «№ {номер}» (mono) + « {первая станция} - {последняя}», без номера —
     «Поезд № {k}»; пассажир — «№ {номер} {откуда} - {куда}», если все три пусты —
     «Пассажиром № {k}»; прочая работа — «{тип или «Прочая работа»} · {станция}»,
     если оба пусты — «Прочая работа № {k}».
   - Порядок: локомотивы с `timeStartOfAcceptance` — по возрастанию, затем без
     времени в порядке добавления; поезда — по `stations.first().timeDeparture` так
     же; пассажиры и прочая работа — в порядке из БД.
   - Подтверждения удаления: «Удалить локомотив?» / «Локомотив будет убран из
     маршрута. Это действие нельзя отменить.»; «Удалить поезд?» / «Поезд будет
     убран…»; «Удалить запись?» / «Запись прочей работы будет убрана…»; «Удалить
     поездку пассажиром?» / «Запись будет убрана из маршрута. Это действие нельзя
     отменить.». Кнопка — «Да, удалить».
   - Удаление поездки-источника «явки по прибытию» возвращает прежнюю явку (§7.2).
10. **ЗАМЕТКИ** — многострочное поле без рамки, плейсхолдер «Опиши смену, если
    нужно…»; пустое → `null`.
11. **Нижняя панель действий** (`FormBottomAppBar`, оверлей): Избранное (иконка
    заливается у избранного; snackbar «Маршрут добавлен в избранное» / «Убрали из
    избранного»), Поделиться (§5.6), Копировать (шторка «Создать копию маршрута?» /
    «Откроется редактирование копии — исходный маршрут не изменится.» / «Создать
    копию» → `showRouteForm(id, isMakeCopy=true)`), Удалить (шторка «Удалить
    маршрут?\nот {дата·время явки}» / «Да, удалить» → `markAsRemoved` — маршрут
    уходит в корзину §9.4 — и экран закрывается). Панель уезжает вниз при прокрутке
    вниз и возвращается при прокрутке вверх (`600 мс`, `FastOutSlowInEasing`), список
    имеет нижний отступ `84 dp`.

### 5.4. Расчёт зарплаты за маршрут (`SalaryForRouteState`)

`calculateSalary` собирает через `SalaryCalculationHelper` параллельно (`async`):
оплата по тарифу, ночные, зональная надбавка (%+время), пассажир (в смене + проезд
до явки по тарифу), праздничные, доплата за пробег (сумма расстояний поездов маршрута
× актуальные ставки выбранных плеч), доплаты за поезд (тяжеловесный / длинносоставный /
удлинённое плечо / сдвоенный), классность, северные, районные, вредность, «в одно
лицо» (обычный или пассажирский поезд по номеру — `passengerTrainNumberList`),
переотдых (по предыдущему маршруту с `restPointOfTurnover`), прочие надбавки.
- Если `getPureWorkTime()` не вычисляется (нет явки или сдачи) — `isCalculated=false`,
  плитка «Расчёт» показывает «0 ₽», шторка — текст «Укажите начало и окончание
  рабочего времени для расчёта заработной платы за поездку».
- `isSetTariffRate = selectMonthOfYear.tariffRate != 0` (тариф берётся из
  **выбранного в настройках месяца**, а не из месяца явки).
- Итог = тариф (не меньше 0) + ночные + зональная + пассажир в смене + ожидание
  пассажиром + пассажир до явки + праздничные + пробег + доплаты за поезд + «в одно
  лицо» + (классность + северные + районные + вредность + прочие) + переотдых +
  командировка.
- Строки шторки (показываются только ненулевые, в этом порядке): «Почасовая оплата»,
  «Праздничные», «Зональная надбавка», «Ночные», «Пассажиром», «Пассажиром до явки»,
  по строке «Доплата за пробег» на каждое плечо («{плечо}: {км} км × {ставка}/км»),
  «Одно лицо», «Доплаты за поезд» (пояснение — перечень применённых видов:
  «тяжеловесный, длинносоставный, удлинённое плечо, сдвоенный»), «Прочие доплаты»,
  «Переотдых» (пояснение «HH:MM × {тариф}/ч × 2/3»), «Командировка (по среднему)».
  Подсказки-формулы: «HH:MM × {тариф}/ч [× N %]», при нехватке данных — текстовое
  описание. Полный контракт шторки — §11.9.
**Переотдых** считается по календарю, без привязки к выбранному в настройках
месяцу: «предыдущий» — ближайший по явке маршрут с явкой раньше явки текущего
среди маршрутов месяца явки *и предыдущего месяца*
(`RouteUseCase.previousRouteForOverRestFlow` → `OverRestRoutes.previousRouteFor`).
Поэтому первый маршрут месяца после отдыха в ПО в конце прошлого месяца получает
переотдых, как и маршрут не из `selectMonthOfYear`. Сумма — полный интервал
переотдыха (`getOverRestInterval`, без обрезки по месяцу) × 2/3 тарифа
выбранного месяца (`Route.overRestPayment`). Ту же цепочку используют «Расчёт
за смену» в «Все маршруты»/Календаре/быстром просмотре (`computeRouteTotalPayment`)
и PWA-мост `calculateTrip` (`previousRoute` либо `candidateRoutes`).
**Командировка**: маршрут разрезается по локальным календарным границам дней
командировки. Командировочная часть оплачивается по среднему часу
(`businessTripMoney`), обычная часть сохраняет тариф и применимые надбавки. Все
обычные строки принудительно обнуляются только если смена целиком находится в
командировке. Частично командировочный маршрут показывает обе части и бейдж `КОМ`.
Если маршрут в командировке, а средний час не задан (`businessTripMoney` = 0/null),
шторка показывает кликабельную карточку «Маршрут в командировке» / «Оплачивается
только по среднему часу, без надбавок. Средний час не указан — поэтому сумма 0.
Нажмите, чтобы задать его в настройках зарплаты.» (→ настройки зарплаты).

### 5.5. Валидации и предупреждения

- **Валидность времён** (`RouteUseCase.isValidBasicData`, в баннер попадает только
  первая найденная ошибка, в этом порядке):
  1. сдача раньше явки — «Окончание работы раньше начала. Невозможно сохранить
     маршрут.» (эта ошибка также **блокирует** запись, §5.2);
  2. начало перерыва ≥ конца — «Начало перерыва позже или равно окончанию.»;
  3. начало перерыва < явки — «Начало перерыва раньше явки.»;
  4. конец перерыва > сдачи — «Окончание перерыва позже сдачи.».
  Ошибки перерыва только показываются, запись не блокируют. Если некорректная
  запись уже лежит в БД или пришла из старых данных, расчёт зарплаты для неё не
  падает и возвращает `0`.
- **«Вторая ночь подряд»** (`computeNightWarn` → `CalculateNightTime.getConsecutiveNightPeriods`):
  ночью считается присутствие в фиксированном окне **00:00–05:00** (пояс
  пользователя) — работа по маршруту либо отдых в пункте оборота. Домашний отдых,
  пришедшийся на это окно, ночью не считается. При этом дневной домашний промежуток
  не разрывает две рабочие ночи на соседних календарных датах. Предыдущий маршрут —
  ближайший по явке среди маршрутов **выбранного в настройках месяца** (⚠️ Поведение Android
  (возможный баг, BUG-19): а не месяца самого маршрута), если между
  его сдачей и текущей явкой ≤ 36 ч; один длинный маршрут, захвативший два ночных
  окна, тоже даёт предупреждение. Пользовательские ночные для оплаты на это
  предупреждение не влияют.
  UI: плашка (`surfaceContainerHigh`, иконка луны) «Вторая ночь подряд» / «Третья
  ночь подряд не допускается.», крестик скрывает плашку до следующего изменения
  данных. Ниже — строки «Предыдущий маршрут» / «Отдых в пункте оборота» / «Этот
  маршрут»: диапазон `dd.MM HH:mm – dd.MM HH:mm`, ночное время `HH:MM` и полоса-
  таймлайн (серая — весь интервал, синие сегменты — ночные окна). Легенда: «Маршрут»,
  «Ночь · 00:00–05:00».
- **Смена > 12 ч** (`checkWorkTimeExceeds12h`): проверяется после ручной установки
  сдачи (не `null`) и после установки явки при включённом «Стандартном времени
  работы». Если `сдача − явка > 12 ч`:
  - «не спрашивать» + «автоматически» (`SharedPreferences`) — сразу создаётся
    пассажир без шторки;
  - «не спрашивать» без «автоматически» — ничего не происходит;
  - иначе — `Passenger12hBottomSheet` (ниже).
  Предзаполнение: отправление = `явка + 12 ч + 1 мин`, прибытие = `сдача − 1 мин`
  (обе обрезаны до минуты), станция отправления — последняя станция последнего
  поезда маршрута. Сохранение сначала пишет маршрут (`preSaveRoute`), затем
  `Passenger` c `basicId` маршрута.
  **`Passenger12hBottomSheet`**: шаг 1 — заголовок «Время работы превышает 12 часов»,
  вопрос «Записать время свыше 12 часов как следование пассажиром?», ссылка «Изменить в
  настройках» (закрыть шторку → `Настройки → Основные`, пункт «Свыше 12 часов —
  пассажиром», §17.5), кнопки «Нет» (закрыть, ничего не создавать) и «Да» (шаг 2). Шаг 2 —
  заголовок «Пассажиром»; поля «Станция отправления» / «Станция прибытия» (автодополнение,
  плейсхолдер «Станция») и «Время отправления» / «Время прибытия» (`AppDateTimePicker`
  «Отправление пассажиром» / «Прибытие пассажиром»), предзаполнены как выше. Ошибка «Время
  следования должно быть в пределах рабочего времени», если отправление < явки, прибытие >
  сдачи или отправление ≥ прибытия; «Сохранить» активна только без ошибки. Закрытие шторки
  свайпом/тапом вне = «Нет».

### 5.6. Дубли, shared preview, шаринг и подписка

- **Дубль по явке** (`findDuplicateByStartWork`, точность до минуты, среди неудалённых
  маршрутов, кроме самого себя): при «Готово» показывает шторку «Маршрут с такой
  явкой уже сохранён.» → «Заменить» (найденный маршрут отправляется в корзину через
  `removeRoute` = `markAsRemoved`, затем сохраняется текущий) / «Оставить оба».
  Закрытие шторки (свайп, тап вне, Back) = отмена, ничего не сохраняется.
- **Shared preview**: маршрут по публичной ссылке лежит в БД с `isDeleted=true`
  (новые id, `reidentifyForImport`). Поверх формы шторка «Получен новый маршрут»:
  «Просмотр» — закрыть шторку и смотреть форму; «Сохранить»
  (`saveSharedRouteAndExit`) — проверка дубля, снятие `isDeleted`, запись и
  немедленный выход; «Отмена» — выйти с экрана (временная запись остаётся в БД с
  `isDeleted=true`, см. BUG-20 в §9.4).
- **Поделиться** (`onShareClick`): без bearer-токена — snackbar «Войдите в аккаунт,
  чтобы делиться маршрутами»; иначе `ShareRouteManager.createShareLink` → системный
  share sheet с текстом `ShareLinkData.fromRoute`. Ошибка — snackbar с текстом
  сервера либо «Не удалось создать ссылку» / «Ошибка создания ссылки».
- **Подписка**: проверяется **до** входа в форму (кнопки «+»/копия на Главном,
  «Все маршруты», Календаре). В коде формы есть шторки «Пробный период» и «Нужна
  подписка», но `FormViewModel` их событие никогда не эмитит — 🚫 Мёртвый код Android — в iOS
  не реализуется.

### 5.7. Шторка «Отдых» (`RestBottomSheet`)

Открывается плиткой «Отдых». `ModalBottomSheet` на фоне `background`, своя ручка;
заголовок «Отдых», подзаголовок «Тип и длительность отдыха».
1. **Сегментный переключатель** «Домашний» / «В пункте оборота» (капсула; активная
   половина залита) — сразу меняет `restPointOfTurnover` маршрута (`onRestChanged`,
   автосейв). При крупном шрифте подписи не увеличиваются больше `fontScale 1.15`.
2. **В пункте оборота**:
   - если короткий/полный или их окончания не посчитаны — карточка «Невозможно
     рассчитать время отдыха.\nПроверьте начало и окончание работы.»;
   - иначе при `isSecondTurnaroundRest` — плашка «Второй отдых в ПО подряд» /
     «Предыдущий маршрут тоже завершился отдыхом в пункте оборота. Нормативы ниже
     посчитаны от минимума для второго отдыха.» (иконка `hotel`, без крестика);
   - карточка строк «Короткий отдых · {длит.}» / «до {dd.MM HH:mm}», «Полный отдых ·
     …», затем «Фактический отдых» (если есть следующая явка).
   - Формулы (`RouteActionsHelper`): `W = getWorkTime()` (чистая работа + пассажиром
     до явки; если не считается — `сдача − явка`); `min` = `minTimeRestPointOfTurnoverSecond`
     (по умолчанию 4 ч) если непосредственно предыдущий по явке неудалённый маршрут
     тоже с отдыхом в ПО, иначе `minTimeRestPointOfTurnover` (по умолчанию 3 ч).
     **Короткий** = `max(W/2, min)` (если `W/2` не кратно минуте — +1 мин, BUG-13);
     **Полный** = `max(W, min)`; окончание = `сдача + длительность`. Если времена
     невалидны (сдача раньше явки) — не считается.
3. **Домашний**:
   - если не посчитано — «Невозможно рассчитать время отдыха.\nПроверьте начало и
     окончание работы во всей цепочке маршрутов.»;
   - иначе карточка: «Минимальный отдых · {длит.}» / «до …» (длительность =
     `minEnd − сдача` = `minTimeHomeRest`, по умолчанию 16 ч), «Полный отдых», затем
     «Фактический отдых»; под карточкой инфо-строка «Полный = время работы × 2,6
     минус отдых в пункте оборота.».
   - Формула (`calculationHomeRest`): маршруты выбранного месяца и предыдущего (BUG-14)
     сортируются по явке; цепочка = текущий маршрут + идущие перед ним подряд
     маршруты с `restPointOfTurnover=true`. `sumWork` = Σ(сдача − явка) цепочки,
     `sumRest` = Σ отдыхов в ПО между звеньями. **Полный** = `max(sumWork × 2,6 −
     sumRest, minTimeHomeRest)`, окончание = `сдача + Полный`. Если у звена нет явки
     или сдачи — ошибка (показывается текст «…во всей цепочке маршрутов.»).
4. **«Фактический отдых»** (`calculationActualRest`): следующая явка = минимальная
   `timeStartWork` строго позже явки текущего среди маршрутов **выбранного месяца и
   следующего** (BUG-19); строка показывается, только если она позже сдачи: длительность =
   `следующая явка − сдача`, «до {следующая явка}».
5. Строка-кнопка «⚙ Настройки времени отдыха ›» → закрыть шторку и открыть
   `Настройки → Отдых` (`showSettingsRest`).
- Длительности — `formatDurationFromMillis` («1д 2ч 5м», нулевые дни/часы
  опускаются, минуты всегда), даты — `dd.MM HH:mm` (mono, без переноса). При `fontScale > 1.15` название и длительность — на разных строках.

### 5.8. `RouteConfirmDialog`

Нейтральное подтверждение (не удаление) на базе `AppAlertDialog`: заголовок, текст,
кнопка подтверждения с передаваемым текстом и «Отмена». Сейчас используется один
раз — при тапе по «Явке», когда явка определяется прибытием пассажиром: заголовок
«Изменить время явки?», текст «Сейчас явка определяется прибытием пассажиром. Если
задать время вручную, режим «явка по прибытию» отключится.», кнопка «Изменить».
Отмена ничего не меняет.

Источник: `features/route/.../ui/FormScreen.kt`, `navigation/FormDestination.kt`,
`viewmodel/FormViewModel.kt`, `viewmodel/RouteFormUiState.kt`,
`viewmodel/DialogRestUiState.kt`, `viewmodel/RouteActionsHelper.kt`
(`calculationHomeRest`, `calculationActualRest`, `calculateShortRest`,
`calculateFullRest`, `isSecondTurnaroundRest`), `viewmodel/SalaryCalculationHelper.kt`,
`domain/.../use_cases/RouteUseCase.kt` (`saveRoute`, `isValidBasicData`),
`domain/.../entities/route/RouteShareExt.kt` (`reidentifyForImport`),
`domain/.../entities/route/UtilsForEntities.kt` (`getPureWorkTime`, `getWorkTime`),
`domain/.../util/CalculateNightTime.kt`, `component/Passenger12hBottomSheet.kt`,
`app/.../viewmodel/MainViewModel.kt` (`handleShareDeepLink`).

---

## 6. Экран «Поезд» (FormTrainScreen)

Форма одного поезда (`Train`) внутри маршрута: номера, характеристики, станции
следования, плечо обслуживания, вспомогательная тяга.

### 6.1. Данные и сохранение

- **Навигация**: `showEmptyTrainForm(basicId)` / `showChangeTrainForm(train)`.
- **ViewModel**: `TrainFormViewModel(trainId, basicId)`. Стандартный контракт форм
  (автосейв 500 мс, guard пустого — `isTrainEmpty`, финал в `onCleared`).
  - Автосейв (`saveTrainSilently` → `performSave`) запускается 500 мс после любого
    изменения: номера, характеристик, плеча, тяги, вагонника, станций (добавление,
    правка из шторки, перегон, удаление, перестановка, «GO»). Каждый автосейв также
    дописывает названия станций поезда в общий список `UserSettings.stationList` и
    серии тяги — в общий список серий.
  - **«Готово» = просто `back()`**: явного сохранения нет, финальную запись делает
    `onCleared` (`NonCancellable + IO`). Существующий поезд сохраняется всегда; новый —
    только если не пустой (`isTrainEmpty`: нет номера и доп. номеров, расстояния,
    веса, осей, У.Д., толкача, двойной тяги, сдвоенного, плеча и ни одной станции с
    названием/временем/путём). Диалога выхода (`ConfirmExitDialog`) нет.
  - ⚠️ **Поведение Android (возможный баг, BUG-21)**: `isTrainEmpty` не учитывает вагонника —
    новый поезд, где заполнен только вагонник, при быстром выходе (< 500 мс после
    ввода) в `onCleared` не сохраняется.
  - Ошибка записи → snackbar «Ошибка: {текст}».
- Станции ведутся отдельным `stationsListState: List<StationFormState>` (id, имя,
  прибытие, отправление, номер пути) и переносятся в `Train.stations` только при
  сохранении.
- Сохранение поезда (`TrainUseCase.saveTrain` → `SqlDelightRouteRepository.saveTrain`)
  идёт в обход `RouteUseCase.saveRoute()` — пишет только строку `Train` и штампует
  родительский `BasicData` через `markUnsynchronizedAndTouch`: `isSynchronized = false`
  **и** `updatedAt = now`. Это касается всех прямых путей сохранения дочерних сущностей
  (`saveLocomotive`/`saveTrain`/`savePassenger`/`saveOtherWork`/`savePartner`) — каждый
  обязан штамповать `updatedAt`, иначе LWW-merge в `SyncManager.syncBidirectional`
  сравнивает устаревший локальный `updatedAt` с серверным, и pull-to-refresh, случившийся
  до завершения фонового автопуша, затирает ещё не отправленную правку (например,
  удалённую станцию) старой версией с сервера.
- Маршрут (`route`) для валидации загружается **один раз** при открытии формы:
  изменение явки/сдачи на других экранах во время работы формы валидатор не видит.
- **iOS (текущий урезанный экран)** загружает маршрут и поезд однократно, чтобы
  обновления БД не затирали несохранённый ввод. Кнопка «Сохранить» заменяет поезд
  с тем же `trainId` либо добавляет новый через `RouteUseCase.saveRoute()`, после
  успешной записи закрывает экран. Если родительский маршрут ещё не сохранён,
  показывает ошибку и оставляет введённые данные на экране.

### 6.2. UI-разделы

Порядок сверху вниз: баннер ошибки → доп. номера → карточка «Расстояние | Номер | +»
(+ строка категории) → карточка «Вес | Оси | У.Д.» (+ «Данные менялись…») → плечо →
сводка вспомогательной тяги → сводка вагонника → ряд «сортировка · секундомер · GO» →
заголовок «МАРШРУТ» + кнопка перегонов → список станций → пилюли сводки. Поверх —
нижняя панель действий.

1. **Топ-бар**: слева «Готово» (скрывает клавиатуру и выходит, см. 6.1), по центру
   «Поезд», справа шестерёнка → подраздел настроек «Поезд» (`showSettingsTrain`).
   Под топ-баром при прокрутке — тень. Сверху списка — красный баннер
   `errorMessage` (§6.3), если есть.
2. **Номер поезда** (`number`) + доп. номера (`additionalNumbers`, добавить/удалить).
   Раскладка: над карточкой — чипы доп. номеров «№ X» с «✕» (выровнены вправо,
   перенос строк); карточка в ряд: ячейка **«Расстояние»** (Decimal-клавиатура,
   суффикс «км» при непустом значении; `"0"` показывается как пусто) | ячейка
   **«Номер»** (префикс «№ » при непустом, Decimal-клавиатура; справа иконка «i» —
   раскрывает под карточкой строку категории поезда `train.trainCategory()`; при
   очистке номера строка скрывается) | квадратная кнопка **«+»** — доступна при
   непустом номере: переносит текущий номер в `additionalNumbers` и очищает поле.
   Кнопка добавления дополнительного номера при доступном действии имеет синий
   фон и светлую иконку — тот же стиль основного действия, что у «Станция».
   В расшифровке номеров грузовых поездов перед подкатегорией явно показывается
   слово «Грузовые» (например, «Грузовые — Участковые»).
   Названия станций в маршруте и названия плеча на карточке/в списке выбора имеют
   обычное начертание (`FontWeight.Normal`), как менее акцентные значения формы.
3. **Характеристики**: расстояние (`distance`), вес (`weight`), оси (`axle`),
   условная длина (`conditionalLength`). ⚠️ Вес/оси/длина по сети — строки.
   - Рядом с «У.Д.» показывается расчётная длина состава в метрах. Категория
     определяется по `UtilsForEntities.passengerTrainNumberList`: для грузового
     поезда применяется `conditionalLength × 14`, для пассажирского введённое
     значение трактуется как число вагонов и умножается на настраиваемую среднюю
     синхронизируемую между платформами через сервер длину пассажирского вагона
     (`UserSettings.passengerWagonLengthMeters`, по
     умолчанию `24,5 м`). Если номер пустой, некорректный или не входит в
     пассажирские диапазоны, применяется грузовой коэффициент 14 м.
   - Шестерёнка в топ-баре открывает отдельный подраздел настроек **«Поезд»**.
     Там пользователь меняет среднюю длину пассажирского вагона; значение должно
     быть положительным, хранится в SQLDelight и входит в межплатформенную
     синхронизацию `UserSettings`. Этот же подраздел доступен из общего экрана
     **«Настройки»**, рядом с пунктами «Маршрут» и «Локомотив».
   - Вес и оси — целые, условная длина дробная (клавиатура `Decimal`, у веса и
     осей — `Number`). Тип клавиатуры при этом ничего не гарантирует: её
     вспомогательный ряд содержит «( ) - , .», и промах по скобке рядом с
     цифрами отправлял на сервер `"4623("`, а русская раскладка — `"29,13"`.
   - Поэтому значение нормализуется во ViewModel (`sanitizeNumericInput`):
     остаётся ведущее число, запятая приводится к точке, разбор
     останавливается на первом чужом символе — `"4623 (60)"` даёт `4623`, а не
     склейку `462360`. Пустой результат хранится как `null`, не как `"0"`.
   - Та же нормализация — в `addTrainDataVersion` (значения из шторки станции
     попадают и в `dataVersions`, и в сам поезд). Правка карточки в шторке истории
     (`updateTrainDataVersion`) не нормализуется: там поле принимает только цифры,
     «.» и «,».
   - Порядок ячеек: «Вес, т» | «Оси» | «У.Д.» (+ подсказка «· N м», при нехватке
     ширины — просто «N»). Длина в метрах = `floor(У.Д. × k)`, где k = 14 м для
     грузовых и `passengerWagonLengthMeters` для пассажирских номеров.
   - Если версий данных > 1 — под карточкой текстовая кнопка «Данные менялись · N
     версия/версии/версий» (склонение по-русски) → шторка «История данных поезда» (§6.7).
   - Сервер с сентября 2026 чинит такие значения сам и отвечает 200 с
     `warnings` вместо 500; клиентская нормализация нужна, чтобы в базе лежало
     введённое машинистом, а не угаданное сервером.
4. **Плечо обслуживания** (`ServicePhase`): карточка-строка с иконкой поезда. Без
   плеча — плейсхолдер «Выбрать плечо» (фон `surface`); с плечом — подпись «Плечо»,
   «{отправление} — {прибытие}», справа «{N} км» и «✕» (снять плечо: `servicePhase =
   null`, расстояние очищается). Тап → шторка «Плечи» (§6.6).
   Выбор плеча (`setSelectedServicePhase`): `distance = phase.distance`; если станций
   нет — создаются две (отправление и прибытие плеча); иначе название первой станции
   заменяется станцией отправления, а последней — станцией прибытия (если станция
   одна — прибытие добавляется второй).
   🚫 Мёртвый код Android — в iOS не реализуется: во ViewModel есть проверка «первая+последняя
   станции не совпадают ни с одним плечом → шторка «Новое плечо» / «Создать плечо?»
   (расстояние, «Создать плечо в обратном направлении», «Добавить»/«Пропустить»)», но она
   вызывается только из `saveTrain()`, который нигде не вызывается («Готово» — просто
   выход).
   Плечи можно создавать/редактировать/удалять прямо здесь (`ShoulderEditBottomSheet`).
   Название выбранного плеча и названия плеч в списке отображаются обычным
   начертанием, без жирного выделения выбранного значения.
5. **Станции следования**: список, каждая — имя (автодополнение), прибытие,
   отправление, номер пути. Добавление/удаление/переупорядочивание (drag),
   редактирование через шторку (`StationEditBottomSheet`, §6.4). Порядок сортировки
   строк (прямой/обратный) переключается иконкой `sort` слева над списком (поворот
   на 180° с анимацией 300 мс) и запоминается (`isReversedSortStationList`).
   - **Тап по строке** — шторка станции; **долгое нажатие** — режим перестановки этой
     станции: слева появляются стрелки «Переместить вверх/вниз» (`moveStation`), тап
     по строке или вне списка — выход из режима. **Свайп влево** (вне режима
     перестановки) → красная кнопка удаления → шторка «Удалить станцию {название}?»
     (без названия — «Удалить станцию?»), «Да, удалить»; отмена закрывает свайп.
   - Строка станции: точка таймлайна, название (2 строки, многоточие; пустое — «—»),
     под ним «проходная» или «путь {N}»; справа колонки ПРИБ · бейдж стоянки · ОТПР
     (время `HH:mm`; ⚠️ Поведение Android (возможный баг, BUG-22): список станций
     форматирует время через `TimeManager()` в фиксированном поясе **GMT+3**, а шторка
     станции — в поясе пользователя; для пользователей не из GMT+3 значения расходятся).
     Бейдж стоянки = `floorMin(отпр) − floorMin(приб)` при > 0:
     текст «N'», «Hч», «HчM'»; цвет — жёлтый `#FFC107` при > 5 мин, красный `#EF5350`
     при > 30 мин (для пассажирского номера поезда — уже при > 20 мин), иначе без фона.
     У станции с флагом «Конечная» бейджа нет.
   - Под карточкой станций — пилюли сводки (считаются по исходному порядку):
     «Время в пути: {H ч M мин}» = от отправления (или прибытия) первой станции до
     прибытия (или отправления) последней; при расстоянии > 0 — «Уч {v} км/ч» =
     `distance / время_в_пути_ч` и «Техн {v} км/ч» = `distance / (время_в_пути −
     Σ стоянок промежуточных станций)_ч` (формат `%.1f`). Нужны ≥ 2 станции и
     положительное время. При наличии плеча
   новые станции вставляются ПЕРЕД конечной станцией плеча. Названия всех станций,
   включая первую и последнюю, отображаются обычным начертанием без жирного шрифта.
   - **Номер пути** (`trackNumber`, `track_number` в API): обычная текстовая
     клавиатура — путь бывает буквенным («3Г»). Переключателей раскладки у поля нет.
   - У **первой станции** флажков «Конечная»/«Проходная» нет: поезд с неё
     отправляется, поэтому такой она быть не может. При сохранении первой
     станции оба флага принудительно сбрасываются — иначе после
     переупорядочивания станция осталась бы со скрытым, но активным флагом.
   - **Флаг «Конечная»** (`isFinalStation`), стандартный `Checkbox`: отмечает
     конечную станцию маршрута поезда. В списке станций **никак не подписывается**;
     единственный видимый эффект — **стоянка не показывается** (маршрут закончился):
     ни бейджем в списке, ни разделителем в шторке, ни чипом секундомера над списком
     (см. п. 6).
   - **Флаг «Проходная»** (`isPassingStation`), стандартный `Checkbox`: станция без
     остановки. Когда включён — в шторке доступно только одно время
     («Проследование», хранится в `timeArrival`). В списке станций у такой строки
     время показывается ОДНО, по центру ширины, отведённой под
     прибытие+стоянку+отправление (прижато вправо, с приглушённой меткой «просл.»
     перед временем), а название станции — приглушённым цветом (alpha 0.55). **Введённое время отправления при включении флага не стирается** —
     пользователь может выключить флаг обратно; `timeDeparture` отбрасывается только
     в момент сохранения.
   - **Перегон** (между station[i] и station[i+1], данные хранятся на
     station[i+1]: `segmentTrackNumber`, `segmentNotes`): в списке между двумя
     станциями область «время в пути» кликабельна и открывает `SegmentEditBottomSheet`
     — поля «От»/«До» предзаполнены названиями соседних станций и редактируемы через
     тот же выпадающий список станций, что и в шторке станции (правка меняет сами
     station[i]/station[i+1]), плюс путь на перегоне и примечание (плейсхолдер
     «Например: по неправильному»).
   - **Отображение перегона** — отдельная карточка между строками станций (по
     референсу), залитая `background`: карточка станций лежит на `secondary`,
     поэтому перегон читается как «утопленный» блок (в тёмной теме темнее, в
     светлой светло-серый). Содержимое разнесено по смыслу: **верхняя строка** —
     подпись «ПЕРЕГОН» слева и время в пути справа (mono), **нижняя** — номер пути
     отдельной пилюлей «N путь», ниже — примечание текстом; переводы строк,
     введённые пользователем в примечании, сохраняются, текст не обрезается, а
     карточка растёт по его высоте. При развороте порядка станций путь и примечание
     остаются у того же физического перегона. При пустых данных нижнего блока нет
     вовсе. Слева иконка-карандаш (о редактировании говорит она, а не
     подпись-подсказка), справа шеврон. Километры и число смежных путей не
     выводятся.
   - **«Будущий перегон»** — тот, у которого ещё неизвестна вторая граница, из-за
     чего маршрут может быть продолжен. Рисуется БЕЗ заливки, пунктирной рамкой,
     с иконкой локомотива и без шеврона. Если для него уже заполнены путь или
     примечание, они показываются прямо в пунктирной карточке даже до заполнения
     времени прибытия соседней станции. Карточка **кликабельна** и открывает ту же
     шторку перегона: «От» — последняя станция, «До» пустое. Название, введённое
     в «До», СОЗДАЁТ вторую станцию (`saveSegmentFromSheet` дописывает её в
     конец списка вместе с путём и примечанием перегона) — иначе перегон некуда
     было бы записать. Пустое «До» ничего не создаёт. Условия появления блока:
     • выбрано плечо, но не проставлено время прибытия на конечную станцию —
     блок занимает место последнего промежутка (перед конечной ещё может
     появиться станция);
     • плечо не выбрано и последняя станция не отмечена флагом «конечная» —
     блок уходит ПОД неё;
     • флаг «конечная» на крайней станции убирает блок совсем.
     Позиция считается на экране в исходном порядке и переводится в индекс
     отображаемого списка (при развороте блок между исходными k и k+1 попадает
     после строки `size-2-k`; для случая «под последней» это −1, т.е. над списком).
     Данные перегона всегда берутся по ИСХОДНОМУ порядку станций: при развороте
     «следующая на экране» — это предыдущая по ходу поездки, и путь с примечанием
     иначе пропадают из карточки.
   - В шторке перегона строка «Путь» делит ширину **поровну на четыре элемента**:
     поле ввода и три кнопки римских номеров (I, II, III), которые подставляют
     значение в поле. Активная кнопка подсвечивается.
   - **Кнопка показа/скрытия перегонов** справа от заголовка «МАРШРУТ»
     (`collapse_rows_24px` / `expand_rows_24px`). Выбор **запоминается**
     (`SharedPreferencesRepositories.isShowSegments` / `setShowSegments`,
     по умолчанию показаны) и восстанавливается при следующих входах на экран —
     так же, как порядок сортировки станций. Переключение сопровождается
     снекбаром «Перегоны показаны» / «Перегоны скрыты».
     Скрываются только **карточки** перегона (путь, примечание, «будущий
     перегон»). **Время в пути между станциями остаётся** — компактной пилюлей
     на пунктирной линии таймлайна, как было до появления карточек; клик по ней
     по-прежнему открывает шторку перегона. Если время посчитать не из чего,
     строки нет вовсе. В PWA выбор хранится в `localStorage`
     (`loco.train.showSegments`) — это настройка устройства, а не маршрута.
   - Над списком станций — шапка колонок «СТАНЦИЯ · ПРИБ · ОТПР».
   - **PWA** повторяет карточку перегона, шторку (От/До + путь с римскими I/II/III +
     примечание), кнопку показа/скрытия и кликабельный «будущий перегон» с созданием
     второй станции. Флажки «Конечная» и «Проходная» в шторке станции там тоже
     есть, с теми же правилами: у первой станции флажков нет и при сохранении они
     принудительно сбрасываются, у проходной остаётся одно время
     («ПРОСЛЕДОВАНИЕ» в `timeArrival`), введённое отправление не стирается при
     включении флага и отбрасывается только при сохранении, а в списке время
     показывается по центру с приглушённым названием. Поля сквозные:
     `station.is_final_station`, `is_passing_station`,
     `segment_track_number`, `segment_notes` на сервере той же миграцией
     `026_car_inspector_station`, и так же не трогаются, если клиент ключ не прислал.
6. **Кнопка «GO»** (`onGoClicked`, контурная кнопка справа над списком; иконка ▶
   если следующим будет отправление, ⏸ если прибытие): проставляет текущее время
   **с секундами** (`nowExact`, чтобы секундомер стартовал с нуля; ⚠️ Поведение Android
   (возможный баг, BUG-23): это единственное место, где время станции не кратно
   минуте, — нарушение инварианта §3.3 п.6) по алгоритму:
   ищется последнее заполненное время с конца списка (при выбранном плече последняя
   станция — прибытие плеча — пропускается); нет станций → создать станцию и
   поставить ей отправление; нет времён → отправление первой станции; последнее —
   прибытие → отправление той же станции; последнее — отправление → прибытие
   следующей станции, а если её нет (или следующая — конечная станция плеча) —
   добавить новую станцию (с плечом — перед конечной) и поставить ей прибытие.
   **Секундомер над списком станций** (`rememberStopwatchState`): считает время от
   последнего события ≤ «сейчас». Прибытие → подпись «стоянка», отправление → «в пути».
   Формат: до 5 мин — секунды `M:SS`, до часа — «Nмин», есть часы — «Yч Zмин», есть дни —
   «Xд Yч Zмин». Цветом подсвечивается **только стоянка**: фон жёлтый `#FFC107` от 5 мин,
   красный `#EF5350` от 30 мин; «в пути» — всегда нейтральный фон `surfaceDim`. Под
   чипом подпись «стоянка»/«в пути». Обновление раз в секунду по абсолютному времени
   и сразу при возврате на экран. При равном времени прибытия и отправления событием
   считается отправление. **Если последнее событие —
   прибытие на станцию, совпадающую с конечной станцией выбранного плеча, чип стоянки
   не показывается** (маршрут по плечу завершён — стоянку на конечной станции не считаем).
   То же и для станции с флагом `isFinalStation`: прибыли на неё — чип стоянки скрыт.
7. **Вспомогательная тяга** (`TrainAssist`, серия/номер/машинист/заметки):
   - **Толкач** (`pusher`) — с направлением `isFirst` («Я толкаю» / «Меня толкают»).
   - **Двойная тяга** (`doubleTraction`).
   - **Сдвоенный поезд** (`doubledTrain`) — с направлением `isFirst`.
   Редактируются в шторке **«Параметры поезда»** (кнопка-иконка настройки на нижней
   панели или тап по сводке): три раздела «Толкач», «Двойная тяга», «Сдвоенный
   поезд»; у пустого раздела справа «Добавить», у заполненного — «✕» (удалить
   раздел). Поля раздела: «Серия» (автодополнение по общему списку серий, фильтр
   `startsWith`, удаление серии из списка), «Номер» (префикс «№ »), «Машинист»
   (префикс «ТЧМ »), «Примечание». Толкач — сегмент «Я толкаю»/«Меня толкают»;
   сдвоенный — «Я первый»/«Я второй» и подсказка «Данные второго»/«Данные первого»
   (данные вводятся про другой поезд). До выбора сегмента направление `null`.
   Справа в шапке шторки «Готово»; закрытие шторки любым способом сохраняет серии
   тяги в общий список серий (`saveAssistSeries`).
   Заполненные разделы тяги показываются **сводкой на самом экране** — под данными
   поезда и плечом, одним блоком на голубом фоне (`surfaceDim`, скруглённая
   карточка). Строка раздела: подпись («Толкач» и т.п.) и направление сверху,
   «серия №номер · ТЧМ имя · примечание» снизу. Клик по блоку открывает шторку
   «Параметры поезда».
8. **Вагонник** (`CarInspector`: `fullName`, `tabNumber`, `couplingTime`) — тем же
   add/remove-паттерном, что толкач/двойная тяга, в отдельной шторке «Вагонник»
   (`CarInspectorSection`). Осматривает и закрепляет автосцепку перед
   прицепкой к конкретному поезду — опционален, может отсутствовать. Поля ФИО и
   табельный номер участвуют в поиске по маршрутам (`SearchRouteUseCase`).
   - В шторке фон и рамка строки «Время прицепки» совпадают с соседними полями
     «ФИО» и «Табельный номер» (`secondary` + рамка `outlineVariant`): строка
     читается как такое же поле ввода, а не как отдельный блок. Подпись имеет
     обычное начертание; пока время не задано, справа не показывается дефис.
   - На экране данные вагонника выводятся **отдельным** голубым блоком — сразу под
     блоком вспомогательной тяги, тем же оформлением, но своей карточкой: вагонник
     не тяга, и смешивать его со списком тяги нельзя. PWA повторяет это один в один.
     Поле сквозное: сервер хранит `train.car_inspector` (миграция
     `026_car_inspector_station`) и не трогает колонку, если клиент ключ не прислал —
     старый клиент не стирает вагонника, сохранённого новым. Верхняя строка — «Вагонник» и,
     если время задано, «· прицепка» приглушённым `onSurfaceVariant` и само время
     моноширинно обычным цветом текста (`primary`, не акцентным); нижняя —
     «ФИО · таб. N». Блок показывается, только если заполнено хоть что-то из трёх
     полей, и по клику открывает шторку «Вагонник».
   Шторка «Вагонник»: раздел «Вагонник» с «Добавить»/«✕», поля «ФИО», «Табельный
   номер», строка «Время прицепки» → `AppDateTimePicker` (заголовок «Время прицепки»;
   на карточке показывается только `HH:mm`). ⚠️ Поведение Android (возможный баг, BUG-24):
   очистить время прицепки нельзя (нет long-press/кнопки) — только удалить вагонника
   целиком.
   На текущем iOS-экране вагонник редактируется отдельной секцией формы: его можно
   добавить/удалить, указать ФИО, табельный номер и установить/очистить дату и время
   прицепки.
9. **Нижняя панель действий** — оверлей поверх формы, по поведению совпадает с
   панелью формы маршрута: при прокрутке вниз за `600 мс` уезжает за нижнюю границу,
   при прокрутке вверх возвращается; список имеет нижний отступ `84 dp`.
   Правая половина панели — основная кнопка «Станция», вызывающая
   `startAddingNewStation`; кнопка синяя с контрастными иконкой/текстом — в стиле
   «Добавить событие» на экране календаря. Прежней кнопки в конце списка нет. Левая половина
   поделена поровну между кнопкой вагонника (иконка двух сцепленных автосцепок,
   при отсутствии записи сразу создаёт `CarInspector`) и кнопкой параметров
   вспомогательной тяги (иконка настройки). Оранжевая точка показывает, что
   соответствующий раздел содержит хотя бы одно заполненное поле. Простое
   открытие раздела и создание пустой записи индикатор не включает.
   Центры двух кнопок равномерно распределены относительно друг друга и краёв
   левой половины панели (`Arrangement.SpaceEvenly`).
   Между верхней границей панели и рядом кнопок оставлен отступ `10 dp`.
   Панель выравнивается по нижней границе всей доступной области экрана, а не по
   высоте содержимого списка. Область системной навигации под ней окрашивается в
   тот же `surface`, как на форме маршрута.

### 6.3. Валидация

`checkFormValidStation` (`RouteUseCase.isValidTrain`) запускается после изменения
времени/названия станции, удаления станции и сохранения из шторки. Проверки (по всем
станциям по порядку; в баннер попадает только **первая** найденная ошибка; `{name}` —
название станции или её порядковый номер):
- отправление раньше явки маршрута — «Станция {name}. Отправление раньше начала работы.
  Невозможно сохранить данные.»; то же для прибытия («Прибытие раньше начала работы…»);
- отправление/прибытие позже сдачи — «Станция {name}. Отправление позже окончания
  работы…» / «…Прибытие позже окончания работы…»;
- прибытие позже отправления той же станции — «Станция {name}. Прибытие позже
  отправления…»;
- для станции i>0: её отправление раньше прибытия предыдущей — «Отправление со станции
  {name} раньше прибытия на станцию {prev}…»; её прибытие раньше отправления
  предыдущей — «Прибытие на станцию {name} раньше отправления со станции {prev}…».
Сравниваются абсолютные моменты (epoch ms); отдельной логики «перехода через
полночь» нет. Пустые значения не проверяются.
⚠️ **Поведение Android (возможный баг, BUG-25)**: несмотря на текст «Невозможно сохранить
данные», ошибка **не блокирует** запись — автосейв и `onCleared` сохраняют поезд
с некорректными временами.

### 6.4. Шторка станции (`StationEditBottomSheet`)

`ModalBottomSheet` (фон `secondary`, прокручиваемая). Открывается тапом по станции
(`startEditingStation(index)`) или кнопкой «Станция» на нижней панели (новая
станция, `index = -1`; для неё заранее генерируется стабильный UUID).
- **Шапка**: «Новая станция» / «Редактировать станцию» + круглая кнопка «✕».
- **Ряд полей**: «Название» (плейсхолдер «Станция», выпадающий список общего
  справочника станций с фильтром `startsWith`, удаление станции из справочника) и
  «Путь» (плейсхолдер «№», текстовая клавиатура, **не более 4 символов**).
- **Флажки** «Конечная» / «Проходная» (`Checkbox`) — только если это не первая
  станция (индекс 0 или первая добавляемая в пустой список).
- **Время**: у проходной — один блок «ПРОСЛЕДОВАНИЕ» (пишется в `timeArrival`); иначе
  «ПРИБЫТИЕ», между ними разделитель «Стоянка N мин» (если отпр > приб и станция не
  конечная), «ОТПРАВЛЕНИЕ». Блок: подпись + дата `dd.MM.yy` справа, крупное время
  `HH:mm` (пусто — «00:00»), кнопка «✕» очистки (если задано); тап по времени —
  `AppDateTimePicker` («Прибытие»/«Отправление», старт — текущее значение или
  «сейчас»); ряд кнопок **«-5», «-1», «Сейчас», «+1», «+5»** (минуты; если значения
  нет — от текущего времени, обрезанного до минуты).
- **«Изменить данные поезда»** (кроме первой станции) — раскрывает поля «Вес, т»,
  «Оси», «У.Д.» (только цифры, «.» и «,»; пусто — «—») и кнопку «Сохранить новые
  данные» → `addTrainDataVersion` сразу (история версий — §6.7). Предзаполнение:
  версия этой станции, иначе текущие данные поезда.
  ⚠️ **Поведение Android (возможный баг, BUG-26)**: для новой станции поиск версии идёт по
  `stationId == null`, что совпадает с исходной версией «Исходные данные» — при
  наличии истории поля предзаполняются исходными, а не текущими данными.
- **«Готово»** (primary) и, для существующей станции, «Удалить станцию» (красный
  текст на `error` 8%) → шторка подтверждения (§6.2 п.5).
- **Закрытие шторки любым способом (свайп, тап вне, «✕», Back) = сохранение**
  (`saveAndDismiss`), кроме полностью пустой новой станции (нет названия, пути,
  времён и флажков) — она просто не создаётся. При сохранении: у первой станции оба
  флага сбрасываются; у проходной `timeDeparture` отбрасывается; новая станция при
  выбранном плече и ≥ 2 станциях вставляется перед последней, иначе в конец.

### 6.5. Шторка перегона (`SegmentEditBottomSheet`)

Открывается тапом по области «время в пути»/карточке перегона между станциями i и
i+1 (или по «будущему перегону»). Заголовок «Перегон». Поля: «От», «До» (оба с
автодополнением станций, плейсхолдер «Станция»; правка меняет название самих
станций i/i+1), «Путь» (≤ 4 символа, плейсхолдер «№») + кнопки «I», «II», «III»
(ширина делится на четыре), «Примечание» (многострочное, плейсхолдер «Например: по
неправильному»), кнопка «Готово». **Закрытие любым способом = сохранение**
(`saveSegmentFromSheet`): путь/примечание пишутся в station[i+1]; если станции i+1
нет и «До» не пустое — создаётся новая станция в конце.

### 6.6. Плечи: выбор и редактор (`ShoulderEditBottomSheet`)

**Шторка «Плечи»** (фон `secondary`): заголовок «Плечи» и «✕»; список всех
`UserSettings.servicePhases` строками «{отпр} — {приб}» + «{N} км» (пусто — «Список
пуст»). Первый тап по строке выделяет её (фон `tertiary` 12%, галочка), повторный
тап по выделенной — применяет и закрывает. При выделении появляются кнопки
«Выбрать» (применить) и «✎ Редактировать»; всегда внизу «⊕ Новое плечо».
**Редактор** (`ShoulderEditBottomSheet`, поверх): заголовок «Новое плечо» /
«Редактировать плечо»; поля «От станции», «До станции» (автодополнение),
«Расстояние» (цифры, суффикс «км»), «Доплата за пробег» (дробное, суффикс «₽/км» — ⚠️
Поведение Android (возможный баг, BUG-27): символ валюты захардкожен, не зависит от страны),
для нового — переключатель
«Создать плечо в обратном направлении». Кнопка «Добавить»/«Сохранить» активна, если
обе станции не пустые и расстояние > 0 (целое); ставка < 0 → 0, нечисло → 0.
Для редактирования сохраняется прежний `id`; обратное плечо получает то же
расстояние и ставку. «Удалить плечо» (только редактирование) удаляет **сразу, без
подтверждения** (⚠️ Поведение Android (возможный баг, BUG-28): расходится с правилом §24.3);
если удалённое плечо было выбрано
в поезде — выбор снимается. Изменение выбранного плеча обновляет его в поезде и
расстояние. Все изменения пишутся в `UserSettings.servicePhases` и ставят флаг
отложенной синхронизации настроек (`setSettingsSyncPending(true)`).

### 6.7. История данных поезда (`dataVersions`)

- Кнопка **«Изменить данные поезда»** в шторке станции (§6.4) есть у любой станции, кроме
  первой (её значения считаются исходными данными поезда), в т.ч. в шторке новой станции:
  для неё заранее создаётся стабильный UUID, который используют и станция, и её версия.
- «Сохранить новые данные» не удаляет прежние значения: при первом изменении создаются
  версия «Исходные данные» (`stationId = null`) и версия выбранной станции; повторное
  изменение на той же станции редактирует существующую версию, а не добавляет новую.
  Текущими `Train.weight` / `axle` / `conditionalLength` становятся значения последней
  версии.
- Вес, оси и У.Д. остаются редактируемыми прямо в форме поезда; такой ввод обновляет
  последнюю (актуальную) версию.
- Индикатор «Данные менялись · N версия/версии/версий» (§6.2 п.3) открывает шторку
  **«История данных поезда»**: все версии по порядку — «Исходные данные», затем название
  станции и тройка значений. Каждая карточка — редактируемые поля (только цифры, «.» и «,»)
  и кнопка «Сохранить»; правка старой версии не меняет актуальные поля формы, правка
  последней — обновляет их. Карточка удаляется свайпом влево (красная область с корзиной;
  постоянной иконки удаления нет). После удаления актуальной версии текущими становятся
  значения новой последней; удаление более старой актуальные поля не меняет.
- В полях У.Д. шторки станции и шторки истории нулевая дробная часть не показывается
  (`45.0` → `45`, `45.5` без изменений).
- Хранение: локально — в обёртке JSON существующей колонки `Train.stations` (старый
  формат массива читается как раньше). В API `Train.dataVersions` — опциональное поле;
  сервер хранит его JSON-массивом в nullable-колонке `train.data_versions` и возвращает при
  загрузке маршрута. Старый клиент поле не присылает — сервер сохраняет прежнюю историю;
  новый клиент прислал `[]` — история очищается.
- Названия станций в полях шторки станции/перегона и в подсказках — обычное начертание
  (`FontWeight.Normal`).

Источник: `features/route/.../ui/FormTrainScreen.kt`, `navigation/FormTrainDestination.kt`,
`viewmodel/TrainFormViewModel.kt`, `viewmodel/TrainFormUiState.kt`,
`viewmodel/TrainFieldState.kt`, `component/StationEditBottomSheet.kt`
(`StationEditBottomSheet`, `SegmentEditBottomSheet`),
`component/ShoulderEditBottomSheet.kt`, `component/TrainStationTimeline.kt`
(`computeRouteSummary`, бейджи стоянок), `domain/.../use_cases/RouteUseCase.kt`
(`isValidTrain`), `domain/.../entities/route/Train.kt`,
`domain/.../entities/setting/UserSettings.kt` (`ServicePhase`).

---

## 7. Экран «Пассажиром» (FormPassengerScreen)

Форма следования пассажиром (`Passenger`): станция/время отправления и прибытия,
номер поезда, «явка по прибытию».

### 7.1. Данные и сохранение

- **Навигация**: `showEmptyPassengerForm(basicId)` / `showChangePassengerForm(passenger)`.
  Также пассажир создаётся без экрана из шторки «Смена > 12 ч» (§5.5).
- **ViewModel**: `PassengerFormViewModel(passengerId, basicId)`. Контракт форм
  (автосейв 500 мс, guard `isPassengerEmpty`, финал в `onCleared`).
  - Автосейв (`savePassenger`) выполняется только при отсутствии ошибки валидации;
    вместе с ним: `syncWorkStartToRoute` (§7.2) и добавление станций отправления/
    прибытия в общий список станций.
  - **«Готово» = `back()`**; финальная запись — `onCleared` (`NonCancellable + IO`):
    новый пустой пассажир (`isPassengerEmpty`: нет номера, станций, времён, заметок и
    флага) не сохраняется; иначе `syncWorkStartToRoute` + запись.
    ⚠️ **Поведение Android (возможный баг, BUG-29)**: `onCleared` сохраняет пассажира **даже
    при ошибке валидации** (прибытие раньше отправления), хотя автосейв такие данные
    пропускает, а текст ошибки говорит «Невозможно сохранить данные».
  - Диалога выхода нет. Метод `clearAllField` во VM — 🚫 Мёртвый код Android — в iOS не
    реализуется (кнопки в UI нет).
- **Валидация** (`RouteUseCase.isValidPassenger`): единственная ошибка — прибытие
  раньше отправления: «Прибытие пассажиром раньше отправления. Невозможно сохранить
  данные.». Следование вне окна «явка–сдача» ошибкой не является (только
  предупреждение, §7.3). При валидных данных `resultTime = прибытие − отправление`
  (если оба заданы).
- Времена не обрезаются во VM дополнительно — приходят из `AppDateTimePicker` уже
  кратными минуте.

### 7.2. «Явка по прибытию» — ключевая бизнес-логика

`setWorkStartByArrival(enabled)`: делает прибытие ЭТОГО пассажира началом рабочего
времени маршрута (`BasicData.timeStartWork = timeArrival`) и снимает флаг с
остальных пассажиров (единственная точка отсчёта). Через `syncWorkStartToRoute`:
- Включение: запоминает прежнюю явку в `timeStartWorkBeforeArrival` и ставит прибытие.
- Выключение: возвращает прежнюю явку из резерва и очищает резерв.
- Изменение времени прибытия при активном флаге — держит `timeStartWork` синхронным.
- Возврат прежней явки из резерва срабатывает, только когда **ни у одного** пассажира
  маршрута нет флага. Иначе открытие соседней поездки (у которой флага нет) сбрасывало
  бы явку из резерва, пока режим держит другой пассажир.
- **Удаление поездки-источника** (`onDeletePassenger` в форме маршрута) равнозначно
  выключению режима: явка возвращается из резерва, резерв очищается, флаг снимается —
  и это пишется в БД сразу вместе с удалением. Иначе явка осталась бы равной прибытию
  уже удалённой поездки, причём без пометки «по прибытию пассажиром».
Сохраняет весь маршрут (помечая несинхронизированным) и ожидает
фактического завершения записи в БД; обновление явки также входит в обычное
автосохранение и финальное сохранение при закрытии формы.
Флаг — **единственный владелец** этого экрана: форма маршрута его не выставляет и
перед своими записями перечитывает из БД (см. 5.2), иначе выключенный переключатель
возвращался в положение «включено».
Каноническое состояние режима на сервере хранится на уровне маршрута в
`basicData.workStartByArrivalPassengerId`: это id единственного пассажира-источника
или `null`, если режим выключен. Поле нужно, чтобы отличать явное выключение (`null`)
от отсутствия поля у старого клиента и не допускать нескольких источников. Android
выводит его из локальных `Passenger.isWorkStartByArrival` непосредственно перед POST;
в локальной SQLDelight-БД отдельная копия route-level поля не хранится.
`Passenger.isWorkStartByArrival` сервер продолжает принимать и возвращать ради старых
Android-клиентов, поддерживая его согласованным с route-level полем. Если старый клиент
не прислал `workStartByArrivalPassengerId`, сервер выводит значение из пассажирского
флага и явно разбирает legacy-строки `"True"`/`"False"` (нельзя применять Python
truthiness: непустая строка `"False"` считается истинной).
`timeStartWorkBeforeArrival` синхронизируется в `basicData` через колонку
`route.time_start_work_before_arrival` (миграция 029). Раньше резерв жил только локально
и обнулялся при любом скачивании маршрута — после этого выключение режима не возвращало
прежнюю явку.
В итоговое отработанное время добавляется только та часть следования пассажиром,
которая лежит за границами интервала «явка–сдача»; пересекающиеся часы повторно не прибавляются.

### 7.3. UI-разделы (сверху вниз)

1. **Топ-бар**: слева «Готово» (скрывает клавиатуру и выходит, см. 7.1), по центру
   «Пассажиром». Других кнопок нет.
2. **Баннер ошибки** (янтарный `PassInfoBanner`: фон `surfaceContainerHigh` 10%,
   рамка 55%) — текст `errorMessage`.
3. **Баннер «не входит в оплату»** (тот же стиль) — только если режим «явка по
   прибытию» выключен, у маршрута заданы явка `ws` и сдача `we`, и НЕ (оба времени
   заданы и прибытие ≤ отправления). Первое сработавшее условие:
   - отправление < явки → «Отправление пассажиром раньше явки в {HH:mm dd.MM.yy} —
     не входит в оплату.»;
   - прибытие > сдачи → «Прибытие пассажиром позже сдачи в {…} — не входит в оплату.»;
   - отправление > сдачи → «Отправление пассажиром позже сдачи в {…} — не входит в
     оплату.»;
   - прибытие < явки → «Прибытие пассажиром раньше явки в {…} — не входит в оплату.».
   Окно явки/сдачи берётся из маршрута в БД и обновляется реактивно.
4. Группа **«ПОЕЗД»**: карточка с круглой иконкой, префиксом «№» и полем номера
   (`trainNumber`, плейсхолдер «поезда», Decimal-клавиатура; пустое → `null`).
5. Две карточки-плеча **ОТКУДА** / **КУДА** (`StationLegCard`): станция + строка
   даты/времени.
   - **Станция** — поле с автодополнением (`ExposedDropdownMenu` +
     `StationDropdownMenu`), как в «Поезд». Выпадающий список **раскрывается по
     фокусу** (полный список станций из `UserSettings`), при вводе фильтруется по
     тексту; тап по подсказке подставляет значение, свайп/действие удаления —
     `onDeleteStationName`. Список **пополняется** станциями, введёнными на этом
     экране (общий источник станций).
   - **Дата/время** — одна тапабельная строка в стиле «Явка»/«Сдача» из
     FormScreen: слева пояснение (**«Отправился»** для ОТКУДА, **«Прибыл»** для
     КУДА), справа объединённые дата+время (`getDateAndTime`, mono). Тап → общий
     `AppDateTimePicker` (заголовок «Отправление»/«Прибытие»); долгое нажатие на
     заполненной строке → шторка «Удалить значение». При крупном системном шрифте
     (`fontScale > 1.15`) пояснение и значение раскладываются **в столбец**, чтобы
     дата/время не обрезались.
   - Плейсхолдеры станций: «От станции» / «До станции». Дата/время — формат
     `getDateAndTime` (`dd.MM.yy HH:mm`), пусто — «Выбрать». Пикер стартует с
     текущего значения поля, иначе «сейчас». Шторки удаления: «Время отправления» /
     «Время прибытия» + «Удалить значение».
6. Между ОТКУДА и КУДА — бейдж **«В пути · {H ч M мин}»** (`resultTime`, если задано и
   валидно; «0 мин» при нуле).
7. Группа **«УЧЁТ РАБОЧЕГО ВРЕМЕНИ»**: строка «Явка по прибытию» + `Switch`
   (`setWorkStartByArrival`). Переключатель **недоступен**, пока не задано время
   прибытия; подзаголовок — «Явка на работу по прибытию пассажиром.» или «Укажите
   время прибытия («Куда»), чтобы включить.». Во включённом состоянии ниже
   раскрывается строка с иконкой часов: «НАЧАЛО РАБОТЫ» и «{станция прибытия} ·
   {дата·время прибытия}» (без станции — только время).
8. Группа **«ПРИМЕЧАНИЯ»**: многострочное поле, плейсхолдер «Например: номер
   приказа».

Источник: `features/route/.../ui/FormPassengerScreen.kt`,
`navigation/FormPassengerDestination.kt`, `viewmodel/PassengerFormViewModel.kt`,
`viewmodel/PassengerFormUiState.kt`, `domain/.../use_cases/RouteUseCase.kt`
(`isValidPassenger`, `saveRoute`), `domain/.../entities/route/Passenger.kt`,
`domain/.../entities/route/UtilsForEntities.kt` (`getPassengerTimeWithinWork`,
`getPassengerTimeOutsideWork`).

---

## 8. Экран «Прочая работа» (FormOtherWorkScreen)

Произвольная работа в рамках смены (`OtherWork`): тип, станция, время начала/конца,
примечание. **Раздел информационный**: данные хранятся и синхронизируются, но НЕ
участвуют в расчёте рабочего времени и зарплаты.

```
OtherWork(otherWorkId, basicId, remoteObjectId?, workType?: String,
          timeStart?: Long, timeEnd?: Long, station?: String, notes?: String)
PREDEFINED_TYPES = ["Маневровая", "Вывозная", "При депо"]   // в этом порядке
```

### 8.1. Данные и сохранение

- **Навигация**: `showEmptyOtherWorkForm(basicId)` / `showChangeOtherWorkForm(otherWork)`.
- **ViewModel**: `OtherWorkFormViewModel(otherWorkId, basicId)`. Контракт форм
  (автосейв 500 мс, guard `isOtherWorkEmpty`, финал в `onCleared`).
  - Новая запись создаётся с `workType` = последний выбранный тип
    (`SharedPreferences.getLastOtherWorkType`); это предзаполнение не считается
    вводом (§3.3 п. 2).
  - Автосейв и финал **не пишут запись при ошибке валидации**.
  - `onCleared`: новая запись не сохраняется, если не было изменений или она пустая
    (нет типа, станции, времён, заметок); иначе — запись и добавление станции в общий
    список станций (только финальное значение, не на каждый символ).
  - **«Готово» = `back()`**, диалога выхода нет.
- `recalcResultTime`: при заданных начале и окончании `resultTime = конец − начало`;
  если `конец ≤ начало` — ошибка «Окончание должно быть позже начала.» и
  `resultTime = null`.

### 8.2. Типы работ и автоподстановка времени

- **Тип работы** (`workType`): список = `PREDEFINED_TYPES` + пользовательские
  (`UserSettings.otherWorkTypeList`), без пустых и дублей. Выбор сохраняет тип и
  запоминает его как последний выбранный.
- **Свой тип** (`addCustomType`): имя обрезается по краям; пустое игнорируется;
  совпадающее с предопределённым просто выбирается (в настройки не пишется);
  новый пользовательский тип добавляется в **начало** `otherWorkTypeList`;
  повторный ввод существующего (с учётом регистра) дубля не создаёт, а поднимает его
  в начало.
- **Удаление своего типа** (`deleteCustomType`): удаляет из `otherWorkTypeList`;
  уже сохранённые записи других маршрутов не меняются, но если удаляемый тип выбран
  в **открытой** записи — её тип сбрасывается в `null`.
- **«От явки до окончания работы»** (`applyTimeFromWork`): начало = явка,
  окончание = сдача маршрута. Если чего-то нет — snackbar «Укажите время явки и
  окончания работы в маршруте».
- **«От приёмки до сдачи локомотива»** (`applyTimeFromLoco`): нет локомотивов —
  snackbar «В маршруте нет локомотива»; один — применяется сразу; несколько —
  шторка выбора (§8.3). Начало = `timeBarrierOut` (выход на КП), иначе
  `timeEndOfAcceptance`, **+1 мин**; окончание = `timeBarrierIn` (заход на КП),
  иначе `timeStartOfDelivery`, **−1 мин**. Если какой-то базы нет — snackbar
  «Укажите время КП или окончание приёмки и начало сдачи локомотива».
  Маршрут для автоподстановки читается реактивно из БД.

### 8.3. UI-разделы (сверху вниз)

1. **Топ-бар**: слева «Готово», по центру «Прочая работа».
2. **Баннер ошибки** (`OwBanner`, янтарный стиль как в «Пассажиром»).
3. **«ТИП РАБОТЫ»**: карточка с иконкой, текущим типом (пусто — «Выберите тип»,
   приглушённо) и стрелкой ▾. Тап → шторка: заголовок «ТИП РАБОТЫ», строки типов
   (выбранный — фон `tertiary` 10% и галочка; у пользовательских — иконка корзины),
   последняя строка «⊕ Добавить свой тип». Выбор закрывает шторку.
   - Корзина → `AppAlertDialog` (деструктивный): «Удалить тип «{X}»?» / «Тип работы
     будет удалён из списка. Уже сохранённые записи не изменятся.» / «Удалить» /
     «Отмена».
   - «Добавить свой тип» → закрыть шторку → `AppInputBottomSheet`: заголовок «Новый
     тип работы», подсказка «Как называется этот вид работы?», поле «Название»
     (текстовая клавиатура), «Добавить» (неактивна при пустом/пробельном вводе). Значение
     обрезается по краям и обрабатывается `addCustomType` (§8.2): новый тип сохраняется и
     сразу выбирается.
4. **«СТАНЦИЯ»**: поле с автодополнением (плейсхолдер «Станция»); выпадающий список
   раскрывается по фокусу — как в «Поезд»/«Пассажиром», фильтр `startsWith`
   без учёта регистра, удаление станции из общего списка.
5. **«ВРЕМЯ»**: строки «Начало» и «Окончание» в стиле «Явка»/«Сдача» из FormScreen:
   одна тапабельная строка, слева метка, справа дата+время (`getDateAndTime`,
   `dd.MM.yy HH:mm`, mono; плейсхолдер «Выбрать»). Тап → `AppDateTimePicker`
   («Начало»/«Окончание», старт — текущее значение или «сейчас»); долгое нажатие на
   заполненной строке → шторка «Время начала»/«Время окончания» с «Удалить
   значение». При `fontScale > 1.15` метка и значение — в столбец. Ниже две кнопки
   автоподстановки «От явки до окончания работы» и «От приёмки до сдачи
   локомотива», затем бейдж «Длительность · {H ч M мин}» (если `resultTime`).
6. **«ПРИМЕЧАНИЯ»**: многострочное поле, плейсхолдер «Например: фамилия составителя».
- **Шторка выбора локомотива** (`LocoPickerSheet`): заголовок «ЛОКОМОТИВ · {N}»,
  строка на каждый локомотив маршрута: «{серия}-{номер}», «{серия} б/н», «{тип}
  {номер}» или «Локомотив {k}». Выбор закрывает шторку и применяет время.

Источник: `features/route/.../ui/FormOtherWorkScreen.kt`,
`navigation/FormOtherWorkDestination.kt`, `viewmodel/OtherWorkFormViewModel.kt`,
`viewmodel/OtherWorkFormUiState.kt`, `domain/.../entities/route/OtherWork.kt`,
`component/AppInputBottomSheet.kt`, `component/AppAlertDialog.kt`.

---

## 8.5. Напарники: справочник, выбор в маршрут, редактор

Напарник — человек, с которым выполнялась поездка. **Справочник** — записи `Partner`
(у пользователя свой список, хранится в БД настроек). **В маршруте** хранится копия
`RoutePartner` (+ `sourcePartnerId`). Три отдельных полноэкранных экрана (все на одном
composable `PartnersListScreen` / `PartnerEditScreen`), НЕ шторки — чтобы клавиатура
не перекрывала поля. Правка или удаление записи справочника **не меняет** копии в уже
сохранённых маршрутах.

### 8.5.1. Экран-список `PartnersListScreen` (режимы MANAGE / SELECT)

- Сортировка записей — по `fullName` (SQL `ORDER BY fullName`, двоичное сравнение
  строк).
- Заголовок списка `ВСЕ НАПАРНИКИ · N`. Строка (три текстовых строки):
  круглый аватар-инициалы (accentSoft фон / accent текст) · ФИО (600) / `таб. N`(accent, mono) /
  примечание (muted, **отдельной строкой под табельным**). Табельный и примечание
  выводятся только если заданы. Справа строки — **карандаш**
  (`ic_edit`) → редактор карточки этого напарника (`showEditPartnerEditor`). Нижняя
  **filled-кнопка** «＋ Добавить напарника» (primary/onPrimary, не FAB) →
  `showNewPartnerEditor()`. Пустое состояние (справочник пуст): круг-иконка + «Пока
  нет напарников» + «Добавьте машинистов и помощников, с которыми работаете — их можно
  будет быстро выбрать в маршруте.». **Свайп влево по строке** → «Удалить»
  с подтверждением-шторкой «Удалить напарника?» / «Да, удалить» (удаляет запись
  справочника; `SwipeToRevealDelete`, compact).
- Над списком (если справочник не пуст) — однострочный поиск шириной в остальные
  элементы списка, с плейсхолдером `Поиск` и иконкой поиска справа. Он без учёта
  регистра фильтрует записи по ФИО, табельному номеру и примечанию: запрос режется по
  пробелам, **каждое** слово должно присутствовать в совокупном тексте записи.
  При активном поиске заголовок меняется на `НАЙДЕНО · N`, при отсутствии совпадений выводится
  сообщение `По запросу «…» ничего не найдено`. Поиск доступен и в MANAGE, и в SELECT, включая
  встроенный в Настройки список.
- **MANAGE** (из Настроек, `showPartnersManage`): `TopAppBar` со стрелкой «‹» и
  заголовком «Напарники»; тап по строке = редактор. VM `PartnerListViewModel`.
- **SELECT** (из формы маршрута, `showPartnerPicker(basicId)`): `CenterAlignedTopAppBar`
  «Напарники», слева синяя текстовая кнопка **«Готово»** вместо стрелки. Тап по строке
  = выбор/снятие, **выделение — цветом фона строки** (`tertiary` 10%, без галочки).
  Изначально отмечены напарники, уже добавленные в маршрут (по `sourcePartnerId`).
  По «Готово» — сверка: для отмеченных без копии создаём `RoutePartner`(копия ФИО/
  табельного/примечания + `sourcePartnerId`), для снятых копий — удаляем; затем
  `back()`. Копии без `sourcePartnerId` сверкой не затрагиваются. **Системный Back —
  выход без применения выбора.** Свайп-удаление записи справочника в этом режиме
  также снимает её выделение.
  VM `PartnerPickerViewModel(basicId)` (`toggle`, `confirm{...}`, `deletePartner`).
- Инициалы аватара — первые буквы первых двух слов ФИО (uppercase; пусто — «?»);
  короткое ФИО — фамилия целиком + остальные слова инициалами с точкой («Иванов И. И.»)
  — `partnerInitials` / `partnerShortName` в `ui/partner/PartnerUi.kt`.

### 8.5.2. Экран `PartnerEditScreen` (создание/редактирование записи справочника)

- App bar «‹ Напарник» + текстовая кнопка «Готово» (accent) — сохранить и выйти. Карточка
  под `ДАННЫЕ НАПАРНИКА`: ФИО (плейсхолдер «Фамилия Имя Отчество»), Табельный №
  (плейсхолдер «1234», цифровая клавиатура, значение mono; хранится строкой),
  Примечания (плейсхолдер «Плечи, контакты»). Под карточкой пояснение «Напарник
  сохранится в справочнике — в следующий раз его можно будет быстро выбрать в
  маршруте.». В edit (есть `partnerId`) — «Удалить напарника»: удаляет запись **сразу, без
  подтверждения** и закрывает экран (⚠️ Поведение Android (возможный баг, BUG-30):
  расходится с правилом подтверждения удаления §24.3). **Кнопки «Выбрать из справочника»
  здесь нет** —
  экран только создаёт/правит запись.
- VM `PartnerEditorViewModel(partnerId?)`: `persistentId` один раз (защита от дублей),
  автосейв через 500 мс после изменения любого поля (silent), `upsert`/`delete`.
  **Запись с пустым ФИО не создаётся и не сохраняется** (ни автосейвом, ни «Готово»);
  при записи поля обрезаются по краям, пустые табельный/примечание → `null`.
  Каждое изменение справочника ставит флаг отложенной синхронизации настроек
  (`setSettingsSyncPending(true)`). `showNew/EditPartnerEditor`. Каждое открытие
  создания нового напарника получает отдельный экземпляр VM и новый `persistentId`; при выходе
  из встроенного в Настройки редактора текущее значение дополнительно сохраняется немедленно,
  поэтому быстрый возврат до истечения debounce не теряет введённые данные.

### 8.5.3. Синхронизация справочника

Full-replace `GET/POST /v1/partners/`: POST если локально есть записи, GET если локально
пусто (по образцу norma_time в `SyncManager`). Напарники маршрута синхронизируются в
составе `SyncData` (поле `partners`, см. раздел 0).

Источник: `features/route/.../ui/PartnersListScreen.kt`, `ui/PartnerEditScreen.kt`,
`ui/partner/PartnerUi.kt`, `ui/partner/PartnerSearch.kt`,
`navigation/PartnerDestinations.kt`, `viewmodel/PartnerListViewModel.kt`,
`viewmodel/PartnerPickerViewModel.kt`, `viewmodel/PartnerEditorViewModel.kt`,
`data_local/.../sqldelight/SettingsDatabase/.../Partner.sq`,
`ui/FormScreen.kt` (`RoutePartnersBlock`).

---

## 9. Экран «Все маршруты» (AllRouteScreen)

Полный список маршрутов **выбранного месяца** (`UserSettings.selectMonthOfYear` —
общий с Главным) с фильтрами, сортировкой, сводкой, быстрым просмотром и
множественным выбором. Открывается `showAllRoute()`.

### 9.0. Данные, загрузка и синхронизация

- **ViewModel**: `AllRouteViewModel`. Загружает маршруты месяца
  (`listRoutesByMonth(selectMonthOfYear, timeCalculationContext)`, реактивно) и для
  каждого строит `ItemState`: `isHoliday`, `isHeavyTrains`,
  `isExtendedServicePhaseTrains`, `isLongCompositionTrain` (по `SalarySetting`),
  `isFuture`, `isTransition` (переходный маршрут на стыке месяцев).
- **Оплата по маршрутам** (`routePayments`: id → «1 234,56 ₽», валюта по стране):
  `computeRouteTotalPayment` по каждому маршруту месяца; для переотдыха в кандидаты
  добавляются соседние маршруты (`adjacentRoutesOfMonthFlow`). Пересчёт при изменении
  маршрутов/настроек; фильтры на суммы не влияют. Ошибка расчёта маршрута — суммы
  просто нет.
- **Отработано за месяц** (`monthWorkedTimeText`): `calculateWorkTimeWithSettings` по
  всем маршрутам месяца (не по фильтру), с учётом «Учитывать будущие маршруты»;
  формат `HH:MM` или десятичный.
- **Состояния списка**: загрузка (первая) — центрированный `CircularProgressIndicator`
  (при последующих перезагрузках спиннер не показывается, список остаётся); ошибка —
  «Ошибка: {текст}» + кнопка «Повторить» (`reload`) и snackbar; после фильтра пусто —
  «Список пуст».
- **Тихая синхронизация при открытии** (`syncOnScreenOpen`): при наличии авторизации
  и активной подписки запускается двусторонняя облачная синхронизация с общим
  cooldown 5 минут и задержкой старта 1,5 секунды: сначала отображается и
  рассчитывается локальный список. Под верхним app bar на время операции показывается
  тонкий `LinearProgressIndicator`; список обновляется реактивно, без модального
  диалога. Уход с экрана отменяет синхронизацию. Ошибка — snackbar с понятным текстом
  (`NetworkErrorMapper.syncFailureMessage`); если сервер прислал удаления —
  snackbar «На сервере пропало маршрутов: N. Проверьте корзину в Настройках.»
  (⚠️ Поведение Android (возможный баг, BUG-10): корзина находится в Профиле, а не в
  Настройках — §9.4).
- **Pull-to-refresh** (`PullToSyncContainer` + `PullToSyncViewModel.refresh`): без
  подписки — «Синхронизация доступна по подписке»; без токена — «Необходимо войти в
  профиль»; иначе двусторонняя синхронизация и сообщение «Синхронизация завершена» /
  «Синхронизация не выполнена: сервер не завершил обработку данных» / «На сервере
  пропало маршрутов: N. Проверьте корзину в Настройках.» (BUG-10) / текст ошибки сети.
  Сообщение показывается на этом же экране.

### 9.0.1. Панели управления и список

- **Топбар**: слева «‹» (назад), по центру «Маршруты», справа текстовая кнопка
  «Выбрать» (режим выбора, §9.2). В режиме выбора: слева «Все»/«Снять», по центру
  «Выбрано: N», справа «Готово».
- **Строка месяца** (`FlowRow`; при нехватке ширины счётчики переносятся целиком):
  слева «{Месяц} {год}» — тап открывает шторку «Выберите месяц и год» (чипы месяцев
  и чипы лет из доступного диапазона, кнопка «Применить» → `setCurrentMonth`); стрелки
  «‹»/«›» листают **хронологический список доступных месяцев** (недоступные —
  приглушены и неактивны). Справа пилюли: число маршрутов месяца (без иконки;
  считается по всем маршрутам, не по фильтру) и отработанное время (иконка часов,
  mono; скрыта, если пусто).
  ⚠️ Поведение Android (возможный баг, BUG-15): в шторке месяц и год выбираются
  независимо; если пары нет в списке доступных месяцев, «Применить» ничего не делает (без
  сообщения). Так же ведут себя шторки месяца на Главном (§4.4) и в «Расчёте зарплаты»
  (§11.7).
- **Строка фильтра/сортировки**: чип «Фильтр» (иконка воронки) → шторка «Фильтры»;
  чип сортировки «Дата ↓» / «Дата ↑» / «Часы ↓» / «Часы ↑» → шторка «Сортировка»;
  справа переключатели «отдых в ПО» (иконка `hotel`; snackbar «Отдых в ПО показан» /
  «Отдых в ПО скрыт») и плотности карточек (`expand_rows`/`collapse_rows`). Цвет
  переключателей: активен — `surfaceContainerLow`, выключен — `primary`.
- **Фильтры** (`RouteFilter`, множественный выбор, чипы с иконками; **все выбранные
  условия объединяются через И**). `ALL` («Все») очищает остальные; выбор
  конкретного снимает `ALL`; снятие последнего возвращает `ALL`:
  - «Избранные» — `isFavorite`;
  - «Тяжелые» — `isHeavyTrains` (время в тяжеловесных поездах по диапазонам
    `SalarySetting.surchargeHeavyTrainsList` > 0);
  - «Удлинённые плечи» — `isExtendedServicePhaseTrains` (по
    `surchargeExtendedServicePhaseList`, только диапазоны с расстоянием > 0);
  - «Резервом» — есть поезд с номером в диапазонах следования резервом
    (4001–4148, 4151–4188, 4191–4198, 4201–4228, 4231–4258, 4261–4298, 4301–4398,
    4401–4698, 4701–4778, 4801–4898) и ненулевым временем следования внутри окна
    «явка–сдача» (`timeFollowingSingleLocomotive`). Это **не** флаг отдыха в ПО;
  - «Одно лицо» — `isOnePersonOperation`;
  - «Свыше 12ч» — `сдача − явка > 12 ч`;
  - «Длинные поезда» — `getLongDistanceTime(0) > 0`, т.е. у любого поезда
    маршрута целая часть У.Д. > 0. ⚠️ **Поведение Android (возможный баг, BUG-31)**: это не
    то же самое, что признак «длинносоставный» карточки (`isLongCompositionTrain` по
    диапазонам из настроек зарплаты) — под фильтр попадает любой поезд с указанной
    условной длиной;
  - «С перерывами» — длительность перерыва > 0;
  - «Толкач» / «Двойная тяга» / «Сдвоенный» — есть поезд с `pusher` /
    `doubleTraction` / `doubledTrain` ≠ null.
  Выбранные фильтры **запоминаются** в `SharedPreferences` и восстанавливаются при
  следующем входе.
- **Сортировка** (`SortOption`, шторка «Сортировка», радиокнопки, выбор закрывает
  шторку, **запоминается**): «Старые» (`DATE_ASC`, по явке; без явки — в конце),
  «Новые» (`DATE_DESC`, по умолчанию; без явки — в конце), «Мало часов на работе»
  (`WORKTIME_ASC`, по `getWorkTime()`, пусто = 0), «Много часов на работе»
  (`WORKTIME_DESC`).
- **Вид** (`isExpandedView`, запоминается): в развёрнутой карточке — группы
  «Локомотивы» (серия + номер mono), «Поезда» («№N» + «первая — последняя станция»),
  «Пассажиром» («№N» + «откуда — куда»), «Прочая работа» (тип + станция), текст
  заметок и блок «Расчёт за смену» (`routePayments`). В свёрнутой — одна строка
  первого поезда («№N первая — последняя») или, если поездов нет, первой прочей
  работы («тип — станция»). Смена вида анимируется (220 мс).
- **Карточка маршрута** — общий компонент `ItemHomeScreen` (как на Главном, §4.5).
  Фон: будущий маршрут — `surfaceBright`, переходный — `surfaceDim`, остальные —
  `secondary`. Номер карточки «#N» = позиция в текущем отсортированном списке,
  считая **снизу** (нижняя = #1). Тап — форма маршрута; долгое нажатие —
  быстрый просмотр (§9.1); свайп влево — удаление с подтверждением (шторка «Удалить
  маршрут?» / «от {dd.MM.yy HH:mm}» / «Да, удалить» → `markAsRemoved`, snackbar
  «Маршрут перемещён в корзину»; отмены в snackbar нет, восстановление — из корзины
  §9.4). Тап по значкам карточки — `RouteLegendSheet` (§9.3).
- **Трей «поездка с отдыхом в ПО»** (`buildAllRouteRows` → `TripGroupRow`, только при
  `showTurnaroundRest`, по умолчанию вкл., запоминается):
  смежные в отсортированном списке маршруты, связанные отдыхом в ПО
  (`turnaroundRestBetween`: из двух маршрутов ранний по явке — «туда»; у него стоит
  `restPointOfTurnover`, а явка позднего строго позже сдачи раннего; верхнего предела
  длительности нет), сворачиваются в один общий фон (`surfaceBright`, радиус 22dp).
  Цепочка **не ограничена парой**: пока каждая следующая смежная пара связана,
  плечо добавляется в тот же трей (A→B→C с двумя отдыхами в ПО = один трей
  на три плеча, между каждыми соседними — свой коннектор). Коннектор: «Отдых в ПО ·
  {станция}» (станция = последняя станция последнего поезда плеча «туда», иначе первая
  станция первого поезда плеча «обратно»; без станции — «Отдых в ПО») и длительность
  `явка «обратно» − сдача «туда»` (`HH:MM` или десятичный формат). Шапка трея (UPPERCASE mono, иконка
  `hotel`): при одном отдыхе — «Поездка с отдыхом · ЧЧ:ММ», при нескольких — «Поездка с
  N отдыхами · ЧЧ:ММ», где ЧЧ:ММ — сумма отработанного времени всех плеч в пределах
  месяца (`getWorkTimeInMonth`; только отображение, ничего не пересчитывает).
- **FAB «Добавить маршрут»** — круг `tertiary` / `ic_add` внизу справа
  (`newRouteClick()`, та же проверка подписки, что на Главном: лимит не исчерпан —
  шторка «Пробный период» с «ОК» (открыть новую форму) и «Оформить подписку за 69
  руб/мес»; исчерпан — шторка «Нужна подписка» с «Оформить подписку за 69 руб/мес» и
  «Восстановить покупки»; покупки — через гейт авторизации §3.2). Показывается только
  вне режима выбора; прячется при прокрутке вниз и появляется при прокрутке вверх
  (600 мс, `FastOutSlowInEasing`).
- **Действия по маршруту** (из быстрого просмотра): избранное (snackbar «Маршрут
  добавлен в избранное» / «Маршрут удален из избранного»), удалить, синхронизировать
  (без токена — «Войдите в аккаунт, чтобы синхронизировать маршрут»; без подписки —
  «Синхронизация доступна по подписке»; успех — «Маршрут сохранен в облаке» или
  «{маршрут} сохранен с предупреждениями:\n…»; ошибка — «{маршрут}: {текст}»),
  поделиться (без токена — «Неавторизованный пользователь»; ошибка сети →
  `friendlyNetworkErrorMessage`/подсказка про VPN, по умолчанию «Не удалось создать
  ссылку»), копировать (шторка «Создать копию маршрута?» → проверка подписки →
  форма копии).
- Список маршрутов текущего вида передаётся в общий `PdfViewModel` (используется
  экраном расчёта зарплаты); кнопки PDF на этом экране нет.

### 9.1. Быстрый просмотр маршрута (RouteQuickViewSheet)

Быстрый взгляд на маршрут без открытия полноэкранной формы. Единый компонент
для списков «Все маршруты», «Главная» и «Календарь» (`RouteQuickViewSheet.kt`).
Полная дизайн-спецификация — `route-quick-view-android.md` в корне проекта.

- **Триггер**: **долгое нажатие** (`onLongClick`) на строке маршрута. Обычный tap
  по-прежнему открывает полноэкранный `RouteScreen` — long-press его не заменяет,
  это ускоритель. На вход передаётся `ItemState` (а не только `Route`), чтобы
  показать доплатные признаки, вычисленные на уровне списка. На «Календаре» строки —
  плоские `Route`, поэтому доплатные признаки (`isHeavyTrains` /
  `isLongCompositionTrain` / `isExtendedServicePhaseTrains`) считаются в
  `CalendarViewModel.loadMonth` в карту `routeFlags` (по id), а заработок — в
  `routePayments` (тем же `computeRouteTotalPayment`, что и на «Все маршруты»).
  Оба показываются и на самой карточке дня, и в шторке.
- **Контейнер**: `ModalBottomSheet` (Android-паттерн), `skipPartiallyExpanded`,
  `RoundedCornerShape(28dp)` сверху, `containerColor = background`, штатное
  затемнение **без блюра** (iOS-peek на Android не переносить). Своя ручка (drag
  handle) нарисована внутри тональной шапки — **без ripple**, чтобы верх был
  одного цвета с зоной времени. Высота — **по контенту**, но не более ~92% экрана:
  область контента ограничена **фиксированным dp** (`heightIn(max = 92% − запас)`,
  Column обнимает содержимое). Важно: высота НЕ должна зависеть от позиции драга
  (`weight(fill=false)` так делал — шторка «прыгала» при поднятии вверх), поэтому
  кап именно фиксированный.
- **Жесты (закрытие «вторым свайпом», паритет с PWA)**: штатные драг-жесты шторки
  **отключены** (`sheetGesturesEnabled = false`) — их nested-scroll поглощал
  остаток флинга `LazyColumn` на верхней кромке и давал дёрганье вверх-вниз.
  Закрытие свайпом реализовано самостоятельно через смещение **всей панели**
  шторки (`Modifier.graphicsLayer { translationY = dragPx }` на самом
  `ModalBottomSheet`, а не на внутреннем контенте — иначе над сдвинутым контентом
  оставался белый фон панели; над сдвинутой панелью видно штатное затемнение):
  - Свайп **по контенту** закрывает шторку **только если жест начался на самом
    верху списка** (`gestureFromTop`, фиксируется на touch-down по
    `!listState.canScrollBackward`) **и сразу пошёл вниз**. Свайп, начатый с
    прокрутки, докручивает список до верха, но **не закрывает** — чтобы закрыть,
    нужен **новый (второй) свайп** вниз от верхней кромки. Любое движение вверх до
    перехвата отменяет закрытие в этом жесте (как в PWA `BottomSheet.vue`).
  - Свайп **по неподвижной шапке** тянет шторку всегда (прокручивать там нечего).
  - Порог закрытия — 110 dp (`CLOSE_DISTANCE` из PWA); ниже порога шторка
    возвращается на место анимацией.
  - Также закрывают: затемнение, кнопка «назад», действия панели.
  Механика перенесена из PWA (`src/components/BottomSheet.vue`:
  `onPanelTouchStart/Move/End`, `dragAllowed` фиксируется по `scrollTop<=0` на
  старте жеста).
- **Толщина шрифта**: крупные значения и названия внутри шторки набраны на одну
  ступень легче базового варианта (номер маршрута, номер поезда, номер+серия
  локомотива, значения времени отдыха, «Время работы»/«Заработано», названия
  станций и т.п.) — единый сдвиг W800→W700, W700→W600, W600→W500, чтобы данные
  не выглядели перегружено-жирными.
- **Сортировка списков** (от старого к новому — старые сверху, новые снизу):
  **поезда** — по времени отправления первой станции
  (`stations.first().timeDeparture`) по возрастанию; **локомотивы** — по времени
  приёмки (`timeStartOfAcceptance`) по возрастанию. Элементы без соответствующего
  времени уходят в конец списка в исходном порядке. Тот же порядок применяется и
  в форме маршрута (`FormScreen`). Причина сортировки: порядок
  `route.trains`/`route.locomotives` из БД/сети не гарантирован (нет `ORDER BY`,
  `insertOrReplace` может переставить строки).
- **Структура**: фиксированная тональная шапка (`accentSoft`) → скроллящийся
  `LazyColumn` с контентом → фиксированная нижняя панель действий.
  - **Шапка**: «Маршрут» + «№{номер}» (номер mono; без номера — «№б/н»), направление «перваяСтанция →
    последняяСтанция» (из графика поездов, скрыто если поездов нет), две
    компактные плашки **«Время работы»** и **«Заработано»** (`shiftPaymentText`,
    в `accent`; «—» если не задан), ниже — чип статуса синхронизации
    «Синхронизирован» / «Не синхронизирован» (`isSynchronized`).
  - **Отдых показывается только в контенте** (не в шапке): домашний
    (`!restPointOfTurnover`, если `homeRest` посчитан) — секция «Домашний отдых»;
    в ПО (`restPointOfTurnover`) — секция «Отдых в пункте оборота · {станция}» с
    короткий/полный. В обеих секциях, если есть **следующая явка**, добавляется
    строка **«Фактический отдых»**: реальный отдых до следующей явки —
    `до {дата·время} · {длительность}`. Все продолжительности отдыха —
    **одного цвета** (`primary`, без акцентов). Считается через
    `RouteActionsHelper.calculationActualRest` (следующий маршрут — минимальный
    `timeStartWork` позже текущего, среди текущего и следующего месяца).
  - **Заработок**: на «Все маршруты» берётся из `routePayments`; на «Главной» —
    считается по требованию (`HomeViewModel.computeRoutePayment` →
    `computeRouteTotalPayment`), даже для будущих маршрутов.
  - **Контент-секции** (по наличию данных): Время работы (Явка/Перерыв/Сдача),
    Отдых (домашний / в ПО), Локомотивы · N (иконка + серия-номер, ниже —
    блок **«Время»**: строки «Приёмка» (`начало` → `конец` → `КП` =
    `timeStartOfAcceptance`/`timeEndOfAcceptance`/`timeBarrierOut`) и «Сдача»
    (`КП` → `начало` → `конец` = `timeBarrierIn`/`timeStartOfDelivery`/
    `timeEndOfDelivery`) — те же поля, что в шторке времени `FormLocoScreen`
    (§2.1). Показывается только то, что реально сохранено: строка «Приёмка»
    рендерится, если есть хотя бы одно из трёх её полей, аналогично «Сдача»;
    если оба пусты — блок не выводится вовсе. Далее — расход
    «принял→сдал», рекуперация со знаком «−» в `success`. Показания секций
    электровоза и тепловоза **не суммируются**: для каждой заполненной секции
    выводится самостоятельный блок «Секция N». У тепловоза под плашками приёмки
    и сдачи (за пределами их фона) показывается пересчёт в кг по коэффициенту
    соответствующей секции, а при наличии экипировки — строка внутри этой же
    секции с её объёмом в литрах и/или массой в кг. Поезда · N (шапка +
    характеристика [вес `т` · оси `ваг` · условная длина `у.д.` — значение у.д.
    **без дробной части**, если целое] + вертикальный график станций с бейджами
    стоянок: >30 мин `danger`, >5 мин `warning`; время прибытия и отправления —
    **оба в `primary`**, приб не бледнее отпр). У станции с флагом «конечная» —
    суффикс «· конечная»; у «проходной» название приглушено и показывается ОДНО
    время «просл HH:MM» (вместо приб/отпр); флаг «конечная» не подписывается.
    Данные перегона перед станцией («перегон · путь N · примечание», если у
    станции заполнены `segmentTrackNumber`/`segmentNotes`) выводятся **отдельной
    строкой на линии таймлайна МЕЖДУ точками** соседних станций (перед строкой
    станции, которой перегон принадлежит), а не под названием станции — иначе для
    последней станции перегон визуально уходил в самый низ графика. **Вагонник**
    (строка «Вагонник: ФИО · таб. N · прицепка HH:MM», `CarInspector`) — это
    данные состава, поэтому выводится **сразу под характеристикой поезда, до
    графика станций**. Ниже графика — только вспомогательная тяга.
    Следование пассажиром, **Доплаты** (повышенная
    длина / масса / свыше 12 ч / удлинённое плечо), Заметки.
- **Нижняя панель** (совпадает с панелью «Маршрут»): Открыть (закрывает шторку →
  форма маршрута), В избранное (иконка переключается **реактивно** — локальный
  оптимистичный флаг, т.к. `route` — неизменяемый снимок), Поделиться (**закрывает
  шторку**, чтобы snackbar с результатом/ошибкой не оказался под ней), Копировать
  (закрывает шторку → шторка «Создать копию маршрута?»), Синхронизировать (**только
  если `!isSynchronized`**, закрывает шторку), Удалить (иконка цвета `error`):
  шторка закрывается, и вызывающий экран показывает **своё** подтверждение удаления —
  на «Все маршруты» это `AppBottomSheet` «Удалить маршрут?» / «от {дата}» / «Да,
  удалить» (§9.0.1), а не `AlertDialog`.
- В шторке **не выводятся** напарники маршрута и прочая работа.
- **Ошибки шаринга** — через `friendlyNetworkErrorMessage`/`vpnAwareErrorMessage`
  (NetworkUtils): сырые технические сообщения о сбое соединения заменяются
  человекочитаемым текстом (или подсказкой про VPN).
- **Данные — только чтение**: расчёты берутся готовыми (`getWorkTime`,
  `shortRest`/`fullRest`, `homeRest`, `getBreakDuration`, `CalculationEnergy`),
  числа — моноширинным шрифтом. Ничего не пересчитывается и не сохраняется.
  ⚠️ `homeRest` во вьюмоделях считается через `calculationHomeRest` с `.collect`
  (не `.first`, иначе берётся эмит `Loading` и значение не обновляется).

Источник: `features/route/.../component/RouteQuickViewSheet.kt`,
`ui/AllRouteScreen.kt`, `viewmodel/all_route_view_model/AllRouteViewModel.kt`
(`calculationHomeRest`, `calculationActualRest`), `viewmodel/RouteActionsHelper.kt`,
`route-quick-view-android.md`.

### 9.2. Множественный выбор и массовые действия

- **Вход**: кнопка «Выбрать» справа в топбаре. В режиме выбора топбар меняется:
  слева — «Все»/«Снять» (выделяет/снимает выделение со всех **видимых** после
  фильтров маршрутов, `toggleSelectAll`), по центру — «Выбрано: N» вместо
  «Маршруты», справа — «Готово» (выход из режима, `exitSelectionMode`).
  Системная «назад» в режиме выбора тоже выходит из режима, а не с экрана
  (`BackHandler`).
- **Выделение карточки**: тап по карточке в режиме выбора переключает выделение
  (`toggleRouteSelection`) вместо перехода в `RouteScreen`; свайп-удаление
  карточки в этом режиме отключён. Слева на карточке — круглый маркер (пустой
  контур / залитый акцентом с галочкой). Выбранная карточка дополнительно
  обведена акцентной рамкой.
- **Нижняя панель действий** — оверлей внизу экрана (тот же визуальный паттерн, что нижняя
  панель формы маршрута, §5.3 п.11): избранное и «поделиться» слева,
  удаление справа. Все три действия неактивны при пустом выборе.
  - **Избранное** (`toggleFavoriteSelectedRoutes`): если среди выбранных есть
    хотя бы один маршрут не в избранном — добавляет в избранное все выбранные;
    если все уже избранные — снимает избранное со всех (иконка панели — залитое
    сердце, когда все выбранные в избранном). После действия — выход из режима
    выбора и snackbar: «Ничего не изменилось» / «Маршрут добавлен в избранное» /
    «Добавлено в избранное: N» / «Маршрут удален из избранного» / «Убрано из
    избранного: N».
  - **Поделиться** (`shareSelectedRoutes`): создаёт публичную ссылку для
    каждого выбранного маршрута и отправляет **одним** сообщением (см.
    `ShareLinkData.fromRoutes` — при одном маршруте текст идентичен обычному
    шарингу одного маршрута). Если для части маршрутов не удалось создать
    ссылку — сообщение отправляется по успешным, дополнительный snackbar
    «Ссылки созданы не для всех маршрутов». Ни одной ссылки — snackbar с ошибкой
    (или «Не удалось создать ссылку»), режим выбора не закрывается. Без токена —
    «Неавторизованный пользователь». После успешной отправки — выход из режима.
  - **Удалить** (`deleteSelectedRoutes`): открывает подтверждение (тот же
    компонент `AppBottomSheet`, что для одиночного удаления) с заголовком
    «Удалить маршрут?» (1 шт.) или «Удалить маршруты?» и строкой «Выбрано: N»;
    «Да, удалить» выполняет soft-delete (`markAsRemoved` — в корзину §9.4) по каждому
    маршруту, snackbar: «Маршрут перемещён в корзину» / «Маршруты перемещены в
    корзину: N» / «Не удалось удалить маршруты» / «Перемещено в корзину: N, не
    удалось: M», затем выход из режима выбора.
  - Действия применяются к выбранным id среди **всех** маршрутов месяца (не только
    видимых после фильтра); «Все»/«Снять» оперирует видимыми. Список имеет нижний
    отступ 88dp под панель.
- Смена месяца (`setCurrentMonth`) сбрасывает режим выбора — выделение относится
  только к маршрутам текущего месяца.

### 9.3. Легенда значков (`RouteLegendSheet`)

Read-only `ModalBottomSheet` «Обозначения». Открывается тапом по ряду значков в
карточке маршрута (`ItemHomeScreen` — Главная, «Все маршруты», Календарь).
Подзаголовок: «Значки в строке маршрута показывают его особенности. У одного
маршрута может быть несколько отметок.». Далее строки «иконка — подпись», в этом
порядке:
1. «Работа в праздничный день» (цветная картинка);
2. «Перерыв в работе» (пауза);
3. «Поезда повышенной длины» (линейка);
4. «Поезда повышенной массы» (гиря);
5. «Удлинённое плечо обслуживания»;
6. «Работа в одно лицо» (человек);
7. «Следование пассажиром»;
8. «Работа свыше 12-ти часов» (цветная картинка «орден»);
9. «Толкач»; 10. «Двойная тяга»; 11. «Сдвоенный поезд»;
12. «Статус синхронизации маршрута» (цветная картинка).
Закрывается свайпом, тапом вне и Back; действий нет.

Источник: `features/route/.../component/RouteLegendSheet.kt`,
`component/ItemHomeScreen.kt`.

### 9.4. Корзина маршрутов (TrashScreen)

**Что попадает.** Любое пользовательское удаление маршрута — soft-delete
`markAsRemoved`: `isDeleted=true`, `deletedAt=now`, `deletionReason="USER_REQUESTED"`,
`remoteDeletionPending=false`, `updatedAt=now`; все дочерние записи остаются. Источники:
удаление из формы маршрута, свайп/быстрый просмотр/массовое удаление на «Все
маршруты», удаление на Главном и в Календаре, «Заменить» в шторке дубля. Серверный
tombstone (`deletedIds` при синхронизации) для подтверждённо синхронизированного
маршрута без локальных правок кладёт его в корзину с `deletionReason=
"REMOTE_SYNC_DELETE"` и сразу подтверждённым удалением (`remoteDeletedAt`).
Отсутствие маршрута в ответе сервера удалением не считается никогда. Маршрут с
локальными правками по tombstone не удаляется, а выгружается обратно.
Дочерние сущности (локомотив, поезд, пассажир, прочая работа, напарник) в корзину не
попадают — удаляются физически. Список корзины = `BasicData.isDeleted=1`,
сортировка по `deletedAt` по убыванию. Поля корзины (`deletedAt`, `deletionReason`,
`remoteDeletionPending`, `remoteDeletedAt`) — локальные, в JSON-синхронизацию не
входят. Сообщение после пользовательского soft-delete — «Маршрут перемещён в корзину»
(при массовом удалении — с количеством, §9.2); формулировка «Маршрут удалён» для
soft-delete не используется. Массового `clearRepository` / `DELETE FROM BasicData` в API
маршрутов нет.

**Вход.** `Профиль → группа «ДАННЫЕ» → «Корзина маршрутов»` (справа — число
«N маршрут/маршрута/маршрутов»). Экран открывается поверх профиля (не отдельный
nav-destination), Back и «‹» возвращают в профиль. В Настройках входа нет.

**Экран.**
- Топ-бар (фон страницы): «‹», заголовок «Корзина»; справа текстовое действие
  «Очистить» (во время очистки «Очистка…») — только если есть записи, которые можно
  удалить физически; неактивно во время восстановления/очистки.
- Пусто: «Корзина пуста» / «Удалённые маршруты появятся здесь».
- Во время синхронизации сверху строка «Синхронизация корзины…».
- Инфо-блок (голубой фон, синяя рамка `tertiary`, без заголовка): «Здесь маршруты
  хранятся 30 дней, затем удаляются автоматически. До этого их можно восстановить.»;
  ниже, если есть записи с `remoteDeletionPending` — «Проверяем статус: N», иначе если
  есть ожидающие серверного удаления — «Ожидают синхронизации: N».
- Кнопка «Восстановить всё» (во время — «Восстановление…»; светло-зелёный фон,
  зелёные текст и рамка).
- Карточка удалённого маршрута (без номера): сверху дата и время явки
  `dd.MM.yyyy · HH:mm` (нет явки — «Дата и время не указаны») и справа кнопка
  «Восстановить» («Восстановление…»); при `fontScale ≥ 1.3` кнопка — отдельной
  верхней строкой, дата — полной строкой ниже. Ниже мелко: «Удалён {dd.MM.yyyy в
  HH:mm}» и оставшийся срок «Осталось N день/дня/дней» (округление вверх до суток,
  от `deletedAt + 30 дней`; 0 → «Срок хранения истёк»); для записей, ожидающих
  серверного удаления, — «Ожидает удаления с сервера».
  ⚠️ Поведение Android (возможный баг, BUG-32): даты корзины
  форматируются в поясе устройства (`SimpleDateFormat`), а не в поясе отображения (§0.1).
- Во время любого восстановления остальные кнопки восстановления неактивны.

**Восстановление** (`restoreFromTrash`): `isDeleted=0`, `deletedAt`, `deletionReason`,
`remoteDeletedAt` = null, `remoteDeletionPending=0`, `isSynchronized=0`,
`updatedAt=now`. Сразу после этого, при активной подписке и наличии токена, маршрут
отправляется на сервер (`syncRoute`), чтобы серверный tombstone не удалил его снова
на других устройствах; неудача отправки восстановление не отменяет (маршрут уедет
со следующей синхронизацией). Сообщения: «Маршрут восстановлен» / текст ошибки
отправки («Маршрут восстановлен, но отправить его на сервер не удалось») / «Не
удалось восстановить маршрут»; для всех: «Все маршруты восстановлены» / «Маршруты
восстановлены, часть не отправлена на сервер» / «Часть маршрутов восстановить не
удалось».

**Окончательное удаление.** Физически удалить можно только запись, которая не
потеряет серверный tombstone (`TrashPurgePolicy.canPurgeManually`): `isDeleted` и
НЕ `remoteDeletionPending` и (маршрут никогда не был на сервере — `remoteRouteId`
пуст — ИЛИ удаление на сервере уже подтверждено — `remoteDeletedAt != null`).
- «Очистить» → `AppAlertDialog` (деструктивный): «Очистить корзину?» / «Будут
  безвозвратно удалены {N} маршрутов.» / «Удалить» / «Отмена» (N — число удаляемых;
  ⚠️ Поведение Android (возможный баг, BUG-33): слово «маршрутов» не
  склоняется). Диалог — стандартный деструктивный `AppAlertDialog`: поверхность `surface`,
  синяя «Отмена», красное «Удалить».
  Итог: «Корзина очищена: N» / «Удалено: N. Ожидают удаления с сервера: M» / «Не
  удалось очистить корзину» / «Удалено: N, не удалось удалить: M».
- **Срок хранения — 30 дней** от `deletedAt`. Подтверждённые записи старше срока
  удаляются автоматически при открытии корзины и после каждой синхронизации. Записи
  без `deletedAt`, ожидающие сервера или `remoteDeletionPending`, не удаляются.
- Физическое удаление — только через `purgeRoute(route, reason)` с журналом событий;
  массового `DELETE FROM BasicData` нет.

**Синхронизация.**
- При каждой двусторонней синхронизации для всех локально удалённых маршрутов (кроме
  `REMOTE_SYNC_DELETE` и `remoteDeletionPending`) отправляется `DELETE
  /v1/route/{id}` (идемпотентно; 404 = успех), после чего фиксируется `remoteDeletedAt`
  и `isSynchronized=1`; локальная копия остаётся в корзине до истечения срока.
- При открытии корзины, если с последней успешной синхронизации прошло ≥ 5 минут, нет
  идущей синхронизации, есть активная подписка и токен, — тихая двусторонняя
  синхронизация («Синхронизация корзины…»); ошибка — snackbar с текстом или «Не
  удалось синхронизировать корзину».
- Пока есть хотя бы один `remoteDeletionPending`, клиент запрашивает серверные данные
  с начала (без delta-cursor). В текущем коде tombstone сразу подтверждается, так что
  `remoteDeletionPending` живёт только внутри одной синхронизации (наследие старых
  версий). ❓ не проверено: возврат маршрута из pending, если сервер снова прислал
  его целиком.

⚠️ **Поведение Android (возможный баг, BUG-20)**: маршрут, открытый по публичной ссылке и не
сохранённый (shared preview, §5.6), лежит в БД с `isDeleted=true`, но без `deletedAt`
и причины — поэтому он **виден в корзине**, никогда не удаляется по сроку и при
синхронизации для него отправляется `DELETE`. Предусмотренная причина
`SHARED_PREVIEW_DISCARDED` нигде не используется.

Источник: `features/route/.../ui/TrashScreen.kt`, `viewmodel/TrashViewModel.kt`,
`ui/ProfileScreen.kt` (вход), `domain/.../entities/route/TrashPurgePolicy.kt`,
`domain/.../use_cases/RouteUseCase.kt` (`markAsRemoved`, `listTrash`,
`restoreFromTrash`, `purgeRoute`), `data_local/.../route/SqlDelightRouteRepository.kt`,
`data_local/.../sqldelight/RouteDatabase/.../BasicData.sq` (`getTrash`,
`restoreFromTrash`, `acknowledgeRemoteDeletion`),
`data_remote/.../remote_rest/SyncManager.kt` (шаги 2.2, 2.4, retention). Прежний
дублирующий раздел «Корзина маршрутов» в конце файла объединён сюда.

Источник (§9 целиком): `features/route/.../ui/AllRouteScreen.kt`,
`navigation/AllRouteScreenDestination.kt`,
`viewmodel/all_route_view_model/AllRouteViewModel.kt`, `AllRouteUiState.kt`,
`viewmodel/PullToSyncViewModel.kt`, `component/ItemHomeScreen.kt`,
`util/TurnaroundRest.kt`, `domain/.../entities/route/UtilsForEntities.kt`
(`isHeavyTrains`, `isExtendedServicePhaseTrains`, `isLongCompositionTrain`,
`timeFollowingSingleLocomotive`, `getLongDistanceTime`).

---

## 10. Экран «Поиск» (SearchScreen)

Поиск по **всем** неудалённым маршрутам (без привязки к выбранному месяцу) с историей
запросов, подсказками и параметрами. Открывается `showSearch()`; тап по результату —
`showRouteForm(id)`, «назад» — `back()`.

### 10.1. Данные и индекс

- **ViewModel**: `SearchViewModel`. Подписывается на настройки и поток всех
  маршрутов (`routesFlow`, `isDeleted = 0`) и при каждом изменении **один раз**
  строит индекс (`SearchRouteUseCase.buildIndex`); сам поиск идёт по индексу в памяти
  без обращений к БД. После пересборки текущий запрос повторяется.
- Индекс маршрута — по одной строке (нижний регистр) на каждую сущность; в строку
  входят **сырые значения** полей, даты в формате `dd.MM.yy HH:mm` (пояс пользователя):
  - **Основные данные**: номер, явка, сдача;
  - **Локомотив** (на каждый): слово «электровоз»/«тепловоз», серия, номер, нормы,
    счётчики отопления/собственных нужд, все 6 времён приёмки/сдачи/КП, по секциям —
    топливо, коэффициенты, экипировка / энергия, рекуперация, «другой род тока»;
  - **Поезд** (на каждый): слово «поезд», номер, доп. номера, расстояние, вес, оси,
    У.Д., станции плеча, серия/номер/машинист/примечание тяги, ФИО/табельный/время
    прицепки вагонника, по станциям — название, путь, путь и примечание перегона,
    времена;
  - **Пассажир**: слово «пассажир», номер поезда, станции, времена, заметки;
  - **Прочая работа**: «прочая работа», тип, станция, времена, заметки;
  - **Напарник**: «напарник», ФИО, табельный, примечание;
  - **Примечания** маршрута.
  Отдельно собирается словарь идентификаторов (номер маршрута, серии/номера
  локомотивов, номера поездов и доп. номера, станции плеча и маршрута, пути, серии/
  номера/машинисты тяги, вагонник, номер поезда и станции пассажира, тип/станция
  прочей работы, ФИО/табельный напарника) — для ранжирования и подсказок.
- **Совпадение**: запрос режется по пробелам и запятым на токены (нижний регистр).
  Маршрут получает тег раздела, если **хотя бы одна сущность этого раздела содержит
  ВСЕ токены** (подстрока). Один маршрут может дать несколько результатов — по одному
  на каждый совпавший раздел (`RouteWithTag`). Выключенный в параметрах раздел не
  проверяется. Маршрут вне периода (§10.3) не участвует.
- **Ранжирование**: оценка = лучшее по токенам: 3 — токен равен идентификатору, 2 —
  идентификатор начинается с токена, 1 — содержит токен, 0 — совпадение только по
  неключевым полям. Сортировка: оценка ↓, затем явка ↓ (новые выше).
- **Подсказки**: до 5 слов из словаря совпавших маршрутов, которые содержат
  **последний** токен, но не равны ему (без дублей, в порядке появления).

### 10.2. Поведение ввода и состояния

- Ввод (`setQueryValue`) — «предварительный» поиск с дебаунсом **200 мс**: результат —
  подсказки (`SearchStateScreen.Input`), история остаётся видимой. Список прокручивается
  к началу.
- **Отправка** (IME-действие «Поиск», тап по подсказке или записи истории —
  `onSearch`): немедленный полный поиск → список результатов, история скрывается;
  непустой запрос (после `trim`) добавляется в историю.
- Пустой запрос → результатов нет, история видна.
- Индекс ещё строится при непустом запросе → состояние загрузки; ошибка загрузки
  маршрутов → состояние ошибки.
- **Результаты**: карточка (фон `surface`, тень 1dp) — «№{номер}» или «Маршрут», дата
  явки `dd.MM.yy` (⚠️ Поведение Android (возможный баг, BUG-32): форматируется в поясе
  устройства, а не в поясе отображения), подпись
  раздела UPPERCASE («Основные данные», «Локомотив», «Поезд», «Следование
  пассажиром», «Прочая работа», «Напарник», «Примечания») и текст всех сущностей
  этого раздела (через пустую строку; формат `EntityString`; ⚠️ Поведение Android (возможный
  баг, BUG-34): заголовок пассажира «Следование паccажиром.» набран с латинскими «cc»), где
  все вхождения
  токенов подсвечены фоном `inversePrimary`. Пустой результат — «Ничего не найдено»
  / «Измените запрос или проверьте фильтры».
- **Подсказки** — чипы; тап: если подсказка содержит весь текущий текст — текст
  заменяется подсказкой, иначе подсказка дописывается через пробел; затем отправка.
- **История** «НЕДАВНИЕ ЗАПРОСЫ» (видна до отправки запроса): уникальные строки
  (таблица `SearchResponse`, `INSERT OR IGNORE`), новые сверху; повторный запрос не
  поднимается наверх. Тап — подставить и отправить; крестик — удалить запись. Лимита
  нет. Если истории нет — блок не выводится.

### 10.3. Параметры поиска (`SearchSettingBottomSheet`)

Строка поиска: поле с плейсхолдером «Я хочу найти...», слева «‹» (скрыть клавиатуру
и выйти), справа лупа (только при непустом тексте — отправка) и иконка параметров
(`tune`) — открывает шторку. IME-клавиша — «Поиск».
- Шапка: «✕» (закрыть), «Параметры», «Сбросить» (`clearFilter`: все разделы
  включены, период очищен, текущий запрос сразу перезапускается).
- **«ГДЕ ИСКАТЬ»**: 7 независимых чипов (по умолчанию все включены): «основные
  данные», «локомотив», «поезд», «следование пассажиром», «прочая работа»,
  «напарник», «примечания» (подписи строчными, как в `FilterNames`).
- **«ПЕРИОД ВРЕМЕНИ»**: две строки «c {dd.MM.yy}» и «по {dd.MM.yy}» (пустые — без
  даты); тап → `AppDateTimePicker` «Начало периода»/«Конец периода» (старт — текущее
  значение или «сейчас»). Маршрут проходит, если (нет начала периода или нет явки,
  или явка ≥ начала) и (нет конца периода или нет сдачи, или сдача ≤ конца).
  Можно задать одну границу. Проверки «начало ≤ конец» нет; очистить одну границу
  нельзя — только «Сбросить».
- Изменения чипов/периода применяются к текущему запросу при **закрытии** шторки
  (закрытие любым способом повторяет поиск по текущему тексту).
- Параметры не сохраняются между входами на экран.

Источник: `features/route/.../ui/SearchScreen.kt`, `navigation/SearchDestination.kt`,
`viewmodel/SearchViewModel.kt`, `viewmodel/SearchUIState.kt`,
`component/SearchSettingBottomSheet.kt`, `component/SearchBar.kt`,
`component/SearchTextField.kt`, `data_local/.../route/SearchRouteUseCase.kt`,
`data_local/.../route/SqlDelightHistoryResponseRepository.kt`,
`domain/.../entities/SearchHelperClass.kt`, `domain/.../entities/route/UtilsForEntities.kt`
(`inTimePeriod`), `core_android/.../util/EntityString.kt`.

---

## 11. Экран «Расчёт зарплаты» (SalaryCalculationScreen)

Детальный помесячный расчёт зарплаты по всем строкам начислений и удержаний.

Районный коэффициент и северная надбавка рассчитываются независимо друг от
друга: коэффициенты не начисляются друг на друга. В их денежную базу входит
праздничная оплата; средний заработок не включается повторно, поскольку введённый
средний час уже считается содержащим применимые коэффициенты.

Начисление за переотдых (2/3 тарифа за оплачиваемое время сверх нормы отдыха в
пункте оборота) включается в «Всего начислено» отдельной строкой. Оно не входит в
базу сверхурочных. Вопрос о начислении на него районного и северного коэффициентов
остаётся нормативным и без подтверждения в расчёт не добавляется.
Переотдых атрибутируется месяцам по календарю: интервал от конца нормы отдыха до
следующей явки режется по границе месяца (`crossMonthTZ`), часть до 1-го числа —
в прошлый месяц, после — в текущий; сумма частей равна полному переотдыху поездки.
Чтобы стык месяцев не терялся, `SalaryCalculationHelper` получает
`adjacentRoutes` — последний маршрут предыдущего и первый маршрут следующего
месяца (`RouteUseCase.adjacentRoutesOfMonthFlow`, на главном —
`OverRestRoutes.adjacentRoutesOfMonth` по всем маршрутам, в PWA — поле
`adjacentRoutes` в `PwaSalaryRequest`). Соседи участвуют только как
«предыдущий/следующий» для переотдыха и не входят в отработанное время, тариф и
надбавки; маршрут, уже входящий в месяц, среди соседей игнорируется.

Следование пассажиром до явки (`isWorkStartByArrival`) входит в общую строку
«Пассажиром» вместе со следованием внутри смены и включается в «Всего начислено»
по своей тарифной ставке. На эту часть начисляются подтверждённые вредность и
зональная надбавка; остальные коэффициенты без нормативного основания не
добавляются.

Для сдвоенных поездов пересекающиеся интервалы одного вида (30% либо 15%) не
суммируются повторно. Интервал ограничивается рабочей сменой и выбранным месяцем,
делится на границе изменения тарифа; перерыв и следование пассажиром исключаются.

Праздничное время считается по точному пересечению смены с календарными сутками:
перерыв вычитается только своей пересекающейся частью, дробные часы сохраняются
(включая интервал ровно в одну минуту) и оплачиваются без округления до часа.

Поля денежных ставок и процентов принимают только конечные неотрицательные числа
(десятичная точка или запятая, пробелы в разрядах допустимы). Отрицательное число,
`NaN`, бесконечность или текст показывают ошибку и не заменяют последнее корректное
значение в сохраняемой модели. Невалидная строка процента в списочной надбавке не
участвует в расчёте.
Если поле настроек ЗП (ставки, средний час, проценты, пороги списочных надбавок,
удержания) содержит ровно `0`, при получении фокуса ноль выделяется целиком и
первая набранная цифра заменяет его (`0` → `5`, а не `05`). Любое другое значение
при фокусе не выделяется — курсор остаётся там, куда тапнул пользователь.
Расчётчик дополнительно защищён от некорректных значений, уже сохранённых старой
версией или полученных при синхронизации: отрицательный либо не конечный текущий
тариф, старый тариф и средний час нормализуются в `0`. Корректная ставка другого
тарифного периода при этом продолжает применяться.
То же правило применяется ко всем одиночным процентам начислений и удержаний;
конечные неотрицательные значения выше `100%` не ограничиваются автоматически.
Для доплаты за линейный пробег не конечная/отрицательная ставка плеча исключает
начисление по этому плечу, а не конечное/отрицательное расстояние конкретного
поезда считается нулевым.
Настраиваемые пороги тяжести, условной длины и удлинённого плеча сортируются
численно. Значение на пороге входит в диапазон; если подходят несколько порогов,
начисляется только диапазон с максимальным подходящим порогом.

«Всего начислено» является суммой всех денежных строк начислений. Для комбинации
нескольких одновременных условий (поездные доплаты, одно лицо, классность,
вредность, зональная, прочая, районная и северная) это соответствие закреплено
интеграционным тестом, чтобы новая строка не могла остаться только в интерфейсе.

Смещение часового пояса хранится в миллисекундах относительно Москвы и при
расчётах сохраняет минуты. Поддерживаются не только целые часы, но и дробные зоны
вида `GMT+05:30`, `GMT+05:45`, а также отрицательные смещения с минутами. Это
фиксированное смещение, а не IANA-зона, поэтому сезонные переходы DST текущей
моделью не представлены.

При изменении тарифной ставки внутри месяца тарифная часть рассчитывается
посегментно: для каждого периода используются только маршруты этого периода,
а также действовавшие в нём условия и ставка. Рабочее время другого периода не
входит в расчёт текущей части.
Норма до границы заканчивается предыдущим календарным днём, норма нового периода
начинается в день действия новой ставки; день границы не учитывается дважды.
Для обоих периодов применяется выбранный профиль рабочего графика, включая
сокращённый шестидневный и пользовательские часы по дням недели.

Строка обычных сверхурочных содержит только тарифную часть и не повторяет
надбавки, уже начисленные отдельными строками. Строки 50%/100% содержат только
дополнительную часть полной применимой базы. При суммированном учёте часы
переработки относятся сначала к более позднему тарифному периоду месяца.
С 01.09.2026 первые два часа для доплаты `0,5` определяются по фактической
переработке каждой смены. Смена без переработки не увеличивает этот лимит; общая
переработка при суммированном учёте распределяется от конца месяца назад.
До 01.09.2024 обе дополнительные части сверхурочных (`0,5` и `1,0`) используют
только тариф. Начиная с 01.09.2024 обе используют расширенную применимую часовую
базу с компенсационными и стимулирующими надбавками.
Маршрут с нулевой фактической длительностью не считается сменой для распределения
сверхурочной доплаты и не изменяет сумму расчёта.
Порядок маршрутов, полученный из хранилища, не влияет на расчёт: временные операции
используют хронологический порядок, а итоговые суммы инвариантны к перестановке списка.
Оплата сверхнормативного отдыха показывается отдельной строкой и не входит в
часовую базу повышенной оплаты сверхурочных. Оплачиваемый интервал отдыха
обрезается выбранным месяцем и границей изменения тарифа: каждая часть использует
ставку, действовавшую в этот момент. Текущий коэффициент `2/3` сохраняется до
подтверждения локальным актом.
В доменной сегментной модели сверхурочные выделяются из последних фактических
рабочих отрезков периода; сегмент сохраняет действовавшие на нём тариф и причины
надбавок. Праздничный отрезок в этот набор не включается и не расходует часы
категорий 50%/100%.
Пересекающиеся интервалы разных маршрутов перед выделением сверхурочных
нормализуются в единую временную шкалу: часы пересечения не дублируются, условия
объединяются, применяется максимальная из одновременно действующих ставок.
Некорректное пересечение данных не должно аварийно закрывать приложение.
С 01.09.2026 первые два часа повышенной оплаты определяются отдельно по каждой
смене с фактической переработкой. Начиная со 121-го сверхурочного часа
календарного года применяется категория 100%; автоматического ограничения
расчёта на 240 часах нет.
Общий KMP-конвейер сначала обрезает рабочие интервалы и исключает пересечение
перерыва, затем разрезает результат по границам тарифов и фактических условий.
Адаптер маршрута переносит в сегменты ночь, следование пассажиром и работу в одно
лицо; на каждом выходном сегменте сохраняются ставка и причины начисления.
Денежная строка ночных рассчитывается по этим сегментам: перерыв в ночном окне
не оплачивается, а маршрут через дату изменения тарифа делится точно в полночь и
для каждой части использует действовавшую ставку.
Ночное окно, переходящее через полночь, включает и утреннюю часть `00:00–06:00`,
даже если маршрут начался уже после полуночи. В повышающей базе сверхурочных
ночной процент применяется только к фактическим ночным сегментам переработки:
ночные часы в начале месяца не повышают дневной сверхурочный хвост.
Для расчётов до 01.09.2024 ночная доплата начисляется отдельно один раз, а
повышающие `0,5/1,0` считаются от тарифа. Начиная с 01.09.2024 по № 91-ФЗ
повышающие части считаются от заработной платы с применимыми компенсационными и
стимулирующими выплатами, поэтому фактический ночной сверхурочный сегмент получает
`0,5/1,0` также на свою ночную доплату.
Доплата за работу без помощника входит в повышающую часть сверхурочных только на
фактических сегментах работы в одно лицо; наличие такой работы в начале месяца не
повышает обычный сверхурочный хвост. Грузовой и пассажирский проценты выбираются
по фактической категории поезда сегмента.
Вредность в повышающей части сверхурочных использует тариф фактического сегмента,
а не среднюю базу месяца. Пока настройка вредности едина для выбранного месяца,
условие применяется ко всем обычным рабочим сегментам и `018L`; командировка по
среднему в эту базу не включается.
Классность и зональная надбавка в повышающей части также используют тариф
фактического сверхурочного сегмента. Классность не применяется к `018L`, а
зональная применяется; командировка по среднему исключена из обеих строк.
«Другие надбавки» означают постоянный процент тарифа обычной работы. В
повышающей части он применяется только к фактическим сверхурочным сегментам;
`018L` и командировка по среднему исключаются.
Доплаты за тяжеловесный длинносоставный и сдвоенный поезд входят в повышающую
часть только на пересечении сверхурочного хвоста с соответствующим поездным
сегментом. Для сдвоенного поезда сохраняются ставки 30% за первый и 15% за
второй поезд.
Пороговые доплаты за тяжеловесный, длинносоставный поезд и удлинённое плечо в
повышающей части используют процент той ступени, которая действует на конкретном
сверхурочном сегменте. Перерыв, `018L` и интервалы вне следования исключаются.
Месячное усреднение совокупных начислений для восстановления сверхурочной
часовой базы не используется: тариф и каждая существующая процентная доплата
берутся только из фактических сегментов сверхурочного хвоста.
Самостоятельные тарифные строки `018L`, `018M` и `052L` не считаются надбавками
и не добавляются повторно через усреднённый остаток сверхурочной базы. Ожидание
`018M`, как и `018L`, исключается из процентных надбавок повышающей части
сверхурочных (классность, зональная, «другие»).
На экране настроек зарплаты под заголовками полей и пороговых доплат отдельные
поясняющие подписи не показываются: карточки содержат только название, элементы
управления и поля ввода. Подробные пояснения для сложных порогов доступны через
кнопку справки.
По тому же правилу считаются время и тарифная оплата следования пассажиром внутри
смены: интервал ограничивается рабочим временем и выбранным месяцем, пересечение
с перерывом исключается, смена тарифа делит начисление на две ставки.
Перекрывающиеся пассажирские записи объединяются и не удваивают время. Для
«явки по прибытию» оплачивается только часть за пределами рабочей смены, обрезанная
выбранным месяцем; пересечение со сменой не учитывается одновременно внутри и вне
работы. На часы следования пассажиром `018L`, в том числе до явки, начисляются
настроенные вредность и зональная надбавка. Вредность по умолчанию равна `4%` по
памятке, но остаётся редактируемой.

Ожидание следования пассажиром `018M` моделируется отдельной строкой и считается
как вычисление, без отдельного поля ввода. Это непокрытый остаток рабочего
времени, примыкающий концом СТРОГО к моменту отправления пассажиром внутри смены
(сдал локомотив → ждёт → поехал пассажиром). Покрытием считаются работа на
локомотиве (от начала приёмки `timeStartOfAcceptance` до конца сдачи
`timeEndOfDelivery`), следование с поездом (первое отправление — последнее
прибытие) и следование пассажиром; перерыв исключается. Из остатка берутся только
куски, чей конец совпадает с отправлением пассажиром; крайние остатки
(подготовительно-заключительное время) и остаток без последующего следования
пассажиром ожиданием не считаются и остаются обычной работой по тарифу — в чисто
грузовом маршруте `018M` = 0. Оплата — по часовой тарифной ставке (как `018L`) и
вычитается из строки «Оплата по тарифу», чтобы не задваиваться. Вредность на
ожидание начисляется (4%); зональная надбавка, классность, «другие надбавки»,
премия ПХД и допремирование (плечо/тяжеловесные/длинносоставные/сдвоенные) — НЕ
начисляются. Следование резервом определяется по номеру поезда и фактическому
станционному интервалу: перерыв и пассажирское время исключаются, смена тарифа и
граница командировки делят расчёт.
Праздничная строка использует точное пересечение смены с календарными сутками
праздника/отмеченного выходного, исключает пересекающийся перерыв и применяет
двойную ставку того тарифного сегмента, в котором фактически находились часы.
Обычный тег календаря `NON_WORKING_DAY` сам по себе не создаёт праздничную
строку: такие часы участвуют в расчёте сверхурочных. Явно назначенный пользователем
`ReleaseType.DayOff` считается основанием для двойной оплаты и исключается из
сверхурочных. Вариант «одинарная оплата + другой день отдыха» пока не моделируется,
поскольку в данных нет выбора работником способа компенсации.
Работа в одно лицо (`153L`, «по маршруту машиниста») начисляется за всю смену
от явки до сдачи в пределах месяца, а не только за интервал следования поезда:
доплата по Положению о КСОТ ОАО «РЖД» идёт «за фактически отработанное время в
этих условиях», а без помощника машинист выполняет и приёмку, и сдачу, и маневры.
Из смены исключаются перерыв, следование пассажиром (перекрывающиеся записи — один
раз) и ожидание следования пассажиром (`018M`). Тариф берётся по каждому временному
сегменту. Категория ставки общая на всю смену: если среди поездов маршрута есть
хотя бы один пассажирский — вся смена идёт по пассажирской строке; иначе (в т. ч.
поезд не указан или без номера) — по грузовой.
Доплата за сдвоенный поезд начисляется только на фактический станционный интервал
поезда внутри выбранного месяца. Перерыв и следование пассажиром исключаются,
а части до и после изменения тарифа используют соответствующие ставки. Проценты
`30%` для первого и `15%` для второго сохраняются до подтверждения локальным актом.
Классность рассчитывается от оплачиваемых рабочих сегментов без следования
пассажиром. Вредность включает рабочие часы, `018L` и проезд до явки; перерыв
исключается общим конвейером, а денежная база каждой части использует ставку,
действовавшую до или после изменения тарифа. Зональная использует ту же базу,
включая `018L`, но исключает ожидание `018M`. Строка «Другие надбавки»
по-прежнему не включает ни следование пассажиром `018L`, ни ожидание `018M`.
Сдвоенные поезда разделяются на строки первого (30%) и второго (15%) по
фактическим интервалам станций. Перекрывающиеся интервалы одной строки
объединяются, перерыв исключается, границы месяца и изменения тарифа применяются
до расчёта денег.
ПДМ для поезда с массой строго больше 6000 тонн и числом осей строго больше 350
начисляется только на фактический интервал этого поезда. Перерыв и следование
пассажиром исключаются, ставка берётся из соответствующего тарифного сегмента;
процент остаётся пользовательской настройкой.

Для переходящего маршрута время и перерыв обрезаются границами выбранного
месяца: из рабочей части вычитается только пересечение перерыва с этим месяцем.

Пороги тяжеловесных и длинносоставных поездов обрабатываются как положительные
целые числа, сортируются численно и невалидные значения не участвуют в расчёте.
При повторении одного порога он участвует в расчёте один раз; применяется
последняя настройка этого порога в сохранённом списке.
Пробелы и десятичная запятая разбираются предсказуемо. Для целочисленных порогов
допускается форма `6000,0`, но дробные, `NaN`, бесконечность и значения вне
диапазона исключаются из расчёта, а не превращают деньги в нечисловое значение.
Время этих доплат не включает пересекающийся перерыв и пассажиром; результат
строится только по фактическому интервалу станций подходящего поезда и
ограничивается рабочей сменой и выбранным месяцем. При пересечении поездов разных
диапазонов каждый момент относится только к диапазону с наибольшим подходящим
порогом, поэтому одна строка времени не начисляется дважды. Изменение тарифа
внутри поездного интервала делит деньги по ставкам соответствующих сегментов.
Для удлинённого плеча сохраняется текущее правило выбора диапазона по сумме
положительных расстояний поездов маршрута. После выбора одного наибольшего
подходящего порога оплачиваются рабочие сегменты маршрута без перерыва и
следования пассажиром; смена месяца или тарифа делит их по соответствующим
границам. Пороги плеча проходят ту же проверку положительных целых значений,
числовую сортировку и устранение дубликатов, что пороги массы и длины.

- **Расчётчик**: единственная реализация `domain/salary/SalaryCalculator.kt`
  находится в `domain/commonMain` и не зависит от Android API. Модуль имеет
  JavaScript browser/node target, поэтому PWA обязана вызывать этот же
  расчётчик через типизированный мост, а не копировать формулы на JavaScript.
  Android-файл
  `SalaryCalculationHelper.kt` оставлен только как фасад совместимости и не
  содержит формул; удалённый старый `SalaryCalculationUseCase` не используется.
  Общий расчётчик проверяется `commonTest` на JVM и iOS Simulator.
  Нативный iOS `SalaryCalculationIosViewModel` также получает из него рабочие,
  ночные и сверхурочные часы, «Начислено», «Удержано» и «К выдаче»; упрощённые
  самостоятельные формулы iOS удалены.
- **PWA**: автономная оболочка находится в `pwa/` и подготавливается задачей
  `./gradlew preparePwa`. Она получает маршруты и настройки одним локальным
  JSON-импортом, передаёт их в `PwaSalaryBridge` и сохраняет только в
  `localStorage`; сетевые запросы для расчёта не выполняются. Мост возвращает
  все ненулевые строки общего расчётчика с устойчивым идентификатором, кодом,
  понятным и служебным названием, часами, процентом и суммой. Экран повторяет
  две таблицы Android («Начислено» и «Удержано»), итоговые суммы и часы.
  Печатный стиль формирует чёрно-белую A4-версию через системное сохранение в
  PDF. Manifest, иконка и service worker обеспечивают установку и повторный
  запуск оболочки без сети после первого успешного открытия.
- Поиск кодов в PWA использует полный `PayrollCodeReferenceCatalog`, ищет по
  коду, служебному названию, расшифровке и типу, поддерживает визуально
  совпадающие кириллические/латинские символы и использует тот же исходный
  порядок: начисления по числовой части кода, затем удержания. Поле `source`
  наружу не экспортируется и в интерфейсе не показывается.
- **ViewModel**: `SalaryCalculationViewModel`. Реактивно пересчитывает при смене
  месяца (`selectYearAndMonth` пишет месяц в настройки) и настроек зарплаты. Каждая
  строка считается отдельным `set…Data(helper) → PartialState`, результаты
  сливаются в `SalaryCalculationUIState`. Пустая частичная строка не может стереть
  уже рассчитанный заголовок месяца. Расчёт — через `SalaryCalculationHelper`.
- Маршруты месяца загружаются через `listRoutesByMonth` с единым
  `TimeCalculationContext.from(userSettings)`. Граница переходной поездки берётся
  из `UserSettings.crossMonthTimezone` (`MOSCOW` или `LOCAL`), поэтому список и
  часы на экране зарплаты совпадают с Главной и календарём также для пользователей
  вне московского часового пояса. То же правило применяется при подсчёте годовой
  сверхурочной работы по предыдущим месяцам.
- **Строки начислений** (каждая — часы + сумма, часть с процентом): по тарифу;
  ночные (%); пассажир (`018L`); ожидание пассажиром (`018M`, показывается только
  когда сумма > 0); одиночное следование; праздничные (одна строка, ×2 тарифа);
  сверхурочные (первые часы ×0.5 и последующие); зональная (%); классность (%);
  удлинённое плечо / тяжеловесные / длинносоставные (списки по порогам); сдвоенный
  (первый/второй); **«Доплата за ПДМ (>6000 т. и >350 осей)»** (вес строго
  больше 6000 т И строго больше 350 осей, отдельный настраиваемый %, default 5%);
  «в одно лицо»
  (грузовой и пассажирский поезд, %); переотдых;
  вредность (%); северные (%); районные (коэффициент); средний час; **недоработка**
  (если отработано < нормы; при отсутствии среднего часа — инфо-окно с предложением
  задать, `showSetAverageHourInfo` / `dismissUnderworkInfoForever`); уход за
  детьми-инвалидами; командировка.
  - Строка «Оплата недоработки» и само инфо-окно про средний час показываются
    только если включён тумблер `SalarySetting.showUnderworkPayments` (настройки
    зарплаты, раздел 12). При выключенном тумблере `SalaryCalculationHelper.
    getUnderworkTimeFlow()` эмитит `0` независимо от нормы/отработанного —
    недоработка не считается и не входит в `totalChargedMoney`, а не просто
    скрывается на экране.
- **Доплата за пробег:** строка использует код `011L`; расчёт и отображение
  выполняются отдельно для каждого
  плеча с ненулевой ставкой. Строка содержит название плеча, суммарный километраж
  только по нему, его ставку ₽/км и сумму. Пробеги других плеч в эту строку не
  входят; плечо со ставкой `0` строку и начисление не создаёт. Один и тот же участок
  учитывается при каждом фактическом прохождении. Для каждого поезда
  используется его фактическое `Train.distance` (при отсутствии — расстояние
  выбранного плеча), умноженное на `ServicePhase.linearMileageRate`. Ставка
  ищется в актуальном справочнике по `ServicePhase.id`, поэтому изменение цены
  пересчитывает ранее сохранённые маршруты. Неделимый километраж целиком относится
  к месяцу явки (`timeStartWork`) в часовом поясе границы месяца, поэтому
  переходящий маршрут не создаёт тот же пробег повторно в следующем месяце.
  Доплата входит в `totalChargedMoney`,
  базы удержаний и «К выдаче», а также выводится в PDF.
- **Итого/удержания**: начислено (`totalChargedMoney`), НДФЛ, профсоюз, прочие
  удержания, **Благосостояние**, **Алименты**, **К выдаче** (`toBeCredited`).
  - **Благосостояние** (`welfareRetention`) — % (`SalarySetting.welfarePercent`)
    от грязной суммы начисления (`totalChargedMoney`), как профсоюз/прочие.
  - **Алименты** (`alimonyRetention`) — % (`SalarySetting.alimonyPercent`) от
    «чистой» суммы к выдаче БЕЗ учёта самих алиментов, т.е. от
    `totalChargedMoney − НДФЛ − профсоюз − прочие удержания − Благосостояние`
    (`SalaryCalculationHelper.getMoneyAlimonyBaseFlow`). Оба удержания входят в
    `totalRetention` и, соответственно, вычитаются из `toBeCredited`; строки не
    показываются при нулевой сумме (как остальные удержания). Отражены и в PDF
    (`PdfGenerator`).
- Таблицы начислений и удержаний на экране и в PDF имеют отдельный столбец
  «Код». Коды и два варианта названия (понятное и как в расчётном листке)
  находятся в едином локальном `PayrollPaymentCatalog`, выбираются по устойчивому
  идентификатору строки и не входят в БД или JSON-контракт. Изменение
  отображаемого названия не должно менять или скрывать код.
  Пока UI показывает понятные названия. Если памятка не задаёт
  однозначный код для строки либо код зависит от основания/подразделения,
  показывается «—», а не предположительное значение. Для составных выплат
  допускается несколько кодов через `/` (например, праздничная оплата).
  Если для одного начисления существуют автоматический код по маршруту и код
  ручного ввода, в расчётном листе показывается только автоматический код
  (`151L`, `152L`, но не варианты `151P`/`152P`).
- Строка «Оплата недоработки» при положительной сумме выводится одинаково на
  экране и в PDF с идентификатором `UNDERWORK` и кодом из каталога.
- Экран и PDF получают один и тот же упорядоченный набор строк из
  `buildAccrualRows`/`buildDeductionRows`; собственные перечни или сопоставление
  выплат по отображаемому тексту не допускаются.
- В топбаре вместо отдельной кнопки PDF находится заметное поле
  «Расшифровать код». Первое нажатие атомарно заменяет расчётный лист экраном
  справочника и только после появления настоящего поля ввода передаёт ему фокус
  и открывает клавиатуру; промежуточное отображение обоих экранов не допускается.
  Слева от поля на расчётном листе постоянно показана стрелка возврата, справа —
  кнопка настроек зарплаты. Экран справочника сохраняет обе боковые кнопки на тех
  же местах: его стрелка возвращает к расчётному листу, а кнопка настроек открывает
  настройки зарплаты. Благодаря одинаковым боковым слотам поле не смещается при
  переходе между расчётным листом и справочником.
  Стрелка и системная кнопка «Назад» возвращают тот же расчётный лист с
  сохранёнными месяцем и состоянием прокрутки.
- Поисковый справочник содержит все подтверждённые коды начислений и удержаний
  из `PayrollCodeReferenceCatalog`. При пустом запросе показывается весь список.
  Поле на расчётном листе и активное поле поиска имеют одинаковое контурное
  оформление. Между полем и кнопкой настроек сохраняется визуальный промежуток.
  Информационная карточка использует мягкий основной синий фон и обычный
  контрастный цвет текста (без фиолетового оттенка); текст карточки:
  «Введите код, служебное название или слова из расшифровки любого удержания
  или начисления».
  Чипы «Начисление»/«Удержание» используют тот же мягкий синий фон и синий
  акцентный текст темы; системный `onPrimaryContainer` для них не используется.
  Введённые слова фильтруют его без отдельной кнопки по коду, служебному названию,
  полной расшифровке и типу («начисление»/«удержание»);
  все слова запроса должны встретиться, регистр не учитывается. Если в коде
  запроса набрана визуально совпадающая кириллическая буква (например, `152Р`
  вместо `152P`), код также находится. Если одному коду
  соответствуют несколько подтверждённых строк (например, `035L`), показываются
  все варианты без технической ссылки на исходное фото. Исходный порядок:
  начисления по возрастанию числовой части кода, затем удержания в таком же
  порядке. Над списком находится краткое объяснение поиска и количество совпадений.
- Денежные строки округляются до копеек до сложения. «Всего начислено», каждое
  удержание, «Всего удержано» и «К выдаче» рассчитываются из уже округлённых
  строк; экран и PDF используют тот же порядок округления.
- Заголовок: переключатель месяц/год со стрелками (как на главном) и блок
  «К выдаче» с символом валюты (₽/₸/Br). Тарифная ставка (`tariffRate`, строка
  `"{ставка} ₽"` либо `"{старая} / {новая} ₽"`) и норма часов (`normaHours` через
  `NormaUseCase.normaHoursFlow`) есть в `SalaryCalculationUIState`, но **на экране
  не выводятся**: ставка используется только для карточки-предупреждения (11.6).
  В блоке «К выдаче» справа показываются подпись «Всего отработано» и общее время
  с учётом следования пассажиром и командировки. Компоновка определяется по
  фактически измеренной ширине блоков: пока оба помещаются, время находится
  справа; если не помещаются (в том числе при крупном системном шрифте), блок
  «Всего отработано» целиком переносится под сумму и прижимается к левому краю.
  Когда блоки находятся рядом, значение часов прижато к правому краю и его
  нижняя линия совпадает с нижней линией значения суммы.
- Выплаты и удержания сортируются по возрастанию первого известного кода;
  строки без кода сохраняют взаимный порядок и находятся в конце. Поэтому
  «Прочие надбавки» и «Прочие удержания» остаются последними соответственно.
- В названии строк «Доплата за пробег» показывается только плечо; ставка за
  километр в скобках не выводится.
- Таблицы начислений и удержаний начинаются с компактного отступа перед кодом.
  Заголовок «КОД» и значения этого столбца выровнены по левому краю; числовые
  столбцы часов, процентов и сумм остаются выровненными по правому краю.
  В удержаниях, как и в начислениях, процент вынесен в отдельный столбец;
  поэтому строка НДФЛ называется «НДФЛ», а ставка `13,0` показывается в `%`.
- Начисление «Переотдых» отображается с подтверждённым кодом `172L`; его
  служебное название в справочнике — `ВыпЛБЗаВрОтдСвНорВПОЛБ`.
- PDF расчётного листа повторяет структуру бумажной формы ФТУ-69 в пределах
  данных, известных приложению: чёрно-белый A4, заголовок периода и краткая
  сводка, начисления слева и удержания справа в общей рамке, служебные названия,
  коды, часы, проценты и суммы, затем общие итоги и сумма к перечислению.
  Неизвестные приложению кадровые, налоговые и пенсионные реквизиты не
  подставляются фиктивными значениями. При переносе повторяются заголовок и
  шапка двух таблиц. Заголовки и значения используют одну сетку столбцов;
  ширина столбца «Месяц» рассчитана на полное название месяца, а границы
  столбцов продолжаются через все строки таблицы.

### 11.1. Как получить на iOS тот же результат до копейки

Все формулы находятся в общем KMP-коде (`domain/commonMain/.../salary/SalaryCalculator.kt`,
класс `SalaryCalculationHelper`, плюс `domain/util/SalarySegment.kt`,
`RouteSalarySegments.kt`, `OvertimeSegmentCalculation.kt`). **iOS обязана вызывать
этот же `SalaryCalculationHelper`, а не переписывать формулы на Swift.** Совпадение
«до копейки» зависит только от того, что экран передаёт в конструктор. Описания
формул в 11.2–11.5 даны для проверки и тестов; источник правды — общий код.

Входы расчёта (Android: `SalaryCalculationViewModel.calculationSalary`):

| Параметр | Как получить |
|---|---|
| `userSettings` | Текущие `UserSettings`. Ключевые поля: `selectMonthOfYear` (снимок `MonthOfYear`: `tariffRate`, `dateSetTariffRate`, `days` с тегами и release-флагами), `timeZone` (смещение от МСК, мс), `crossMonthTimezone`, `nightTime`, `minTimeRestPointOfTurnover`, `servicePhases`, `isConsiderFutureRoute`, `isDecimalTime`, `country`. |
| `salarySetting` | `SalarySetting` из `SalarySettingUseCase.salarySettingFlow()`. |
| `allRoutes` | `RouteUseCase.listRoutesByMonth(selectMonthOfYear, TimeCalculationContext.from(userSettings))`: окно месяца `[1-е число 00:00; последний день 23:59:00]` в `crossMonthTZ`, SQL-выборка начинается на 48 ч раньше, затем пост-фильтр: маршрут без `timeStartWork` остаётся; иначе `start < конецОкна && (timeEndWork == null \|\| timeEndWork >= началоОкна)`. Если `isConsiderFutureRoute == false` — дополнительно оставить только `timeStartWork < now` (текущее время устройства, мс). |
| `effectiveNormaHoursForUnderwork` | `isUnderworkPeriodClosed(год, месяц0, сегодня)` (см. §12) → `NormaUseCase.normaHoursFlow(год, месяц0).first()`, иначе `0`. «Сегодня» — локальная дата устройства. |
| `annualOvertimeBeforePeriod` | Только если месяц ≥ сентябрь 2026 (`isFederalLaw144Effective`: `год > 2026` или `год == 2026 && месяц0 >= 8`), иначе `0`. Для каждого месяца того же года с `месяц0 < выбранного` (из `CalendarUseCase.loadFlowMonthOfYearListState()`, первая запись на месяц): `SalaryCalculationHelper(userSettings.copy(selectMonthOfYear = тотМесяц), salarySetting, listRoutesByMonth(тотМесяц), workScheduleProfile).getTimeOvertimeFlow()`; значения суммируются. Фильтр будущих маршрутов здесь **не** применяется. |
| `workScheduleProfile` | `SharedPreferencesRepositories.getWorkScheduleProfile()` (§16.3). |
| `adjacentRoutes` | `RouteUseCase.adjacentRoutesOfMonthFlow(месяц, context)`: последний по явке маршрут с `timeStartWork < начало месяца` и первый с `timeStartWork >= начало следующего месяца` (границы в `crossMonthTZ`). Фильтр будущих — как у `allRoutes`. |

Пересчёт запускается заново (предыдущий отменяется) при любом изменении
`UserSettings`, `SalarySetting`, списка маршрутов месяца или соседей. На время
расчёта `screenState = Loading("Пересчет...")`.

⚠️ Поведение Android (возможный баг, BUG-35): при `isConsiderFutureRoute == false`
фильтр использует `timeStartWork!!`; маршрут месяца без явки дал бы падение (на практике
SQL-выборка по периоду такие маршруты, вероятно, не возвращает — ❓ не проверено).

⚠️ iOS: текущий `SalaryCalculationIosViewModel` не передаёт в расчётчик
`annualOvertimeBeforePeriod` и, по-видимому, `workScheduleProfile`. Для совпадения «до
копейки» с Android iOS обязана передавать **все** входы из таблицы выше.

### 11.2. Базовые примитивы расчёта

- **Интервалы** — полуинтервалы `[start, end)` в мс. Маршрут с
  `timeEndWork < timeStartWork` целиком исключается из расчёта; сосед без явки
  или уже входящий в месяц — тоже.
- **Часовые пояса** (`TimeCalculationContext.from`): `localTZ` — фиксированное
  смещение `GMT±(3 ч + userSettings.timeZone)` (строка `getTimeZone`, например
  `GMT+7`, `GMT+05:30`); `crossMonthTZ` = `GMT+3` при
  `crossMonthTimezone == MOSCOW`, иначе `localTZ`. Границы месяца, дата смены
  тарифа и месяц явки — в `crossMonthTZ`; ночные окна, праздничные сутки и сутки
  командировки — в `localTZ`.
- **Обрезка по месяцу** (`clipToMonth`): `[max(явка, начало месяца),
  min(сдача, начало следующего месяца))`; пустой результат — маршрут не даёт сегментов.
- **Перерыв**: `[timeStartBreak, timeEndBreak)`, если заданы оба (при обратном
  порядке границы меняются местами), пересечённый с рабочим интервалом.
- **Тариф**: ставки с `NaN`/∞/отрицательным значением считаются `0`. Если
  `dateSetTariffRate == null` — одна ставка `tariffRate`. Иначе до момента
  `00:00 (crossMonthTZ)` дня `dateNewRate` выбранного месяца действует `oldRate`,
  начиная с него — `tariffRate`.
- **Командировка**: дни месяца с release-типом `BusinessTrip` дают интервалы
  `[день 00:00; следующий день 00:00)` в `localTZ`. Каждый маршрут режется на
  командировочные и обычные фрагменты; перерыв обрезается по фрагменту. Обычные
  строки считаются по обычным фрагментам (`routeList`), оплата командировки — по
  командировочным, норма/недоработка/распределение переработки — по исходным маршрутам.
- **Сегменты** (`Route.buildSalarySegments`): рабочий интервал (после обрезки по
  месяцу) минус перерыв → разрез по дате смены тарифа → разрез по границам условий.
  Каждый сегмент хранит интервал, ставку ₽/ч и набор условий.
  `M(s) = длительность_мс × ставка / 3 600 000` — тарифные деньги сегмента.

Условия сегмента:

| Условие | Где действует |
|---|---|
| `NIGHT` | Ночные окна `nightTime` в `localTZ` (по умолчанию 22:00–06:00). Если начало позже конца, окно дня D — `[D нач; D+1 кон)`, иначе `[D нач; D кон)`; перебираются дни от (дата явки − 1 при переходе через полночь) до даты сдачи. |
| `HOLIDAY` | Календарные сутки (`localTZ`) дней месяца с тегом `HOLIDAY` или с release-типом `DayOff`. |
| `PASSENGER` | Каждый `Passenger` `[timeDeparture; timeArrival)` ∩ работа (включая часть «явки по прибытию», попавшую в смену). |
| `PASSENGER_WAITING` | 018M: работа минус (локомотив `[начало приёмки; конец сдачи)` и подынтервалы приёмки/сдачи, поезд `[отпр. 1-й станции; приб. последней)`, пассажир внутри смены, перерыв); берутся только куски, чей конец совпадает с отправлением пассажиром (пассажиры без `isWorkStartByArrival`). |
| `ONE_PERSON_FREIGHT` / `ONE_PERSON_PASSENGER` | Только при `basicData.isOnePersonOperation`. Интервал — вся смена (clip месяца) минус перерыв, `PASSENGER` и `PASSENGER_WAITING`; время станций поездов не используется. Категория одна на смену: `ONE_PERSON_PASSENGER`, если хотя бы у одного поезда `number.trim().toInt()` входит в 1–150, 151–298, 301–450, 451–598, 601–698, 701–750, 751–788, 801–898; иначе (нет поездов, номер пустой/грузовой) — `ONE_PERSON_FREIGHT`. |
| `RESERVE` (052L) | Поезда с номером (`toIntOrNull`) в 4001–4148, 4151–4188, 4191–4198, 4201–4228, 4231–4258, 4261–4298, 4301–4398, 4401–4698, 4701–4778, 4801–4898. |
| `HEAVY_LONG_DISTANCE_TRAIN` (ПДМ) | Поезда с массой (`trim`, `,`→`.`) `> 6000` **и** целым числом осей `> 350`. |
| `DOUBLED_TRAIN_FIRST` / `_SECOND` | Поезда с `doubledTrain.isFirst == true` / `false`. |
| `HARMFUL` | Все сегменты, если процент вредности > 0. |
| `ZONAL` | Все, кроме `PASSENGER_WAITING`, если процент > 0. |
| `QUALIFICATION_CLASS`, `OTHER_SURCHARGE` | Все, кроме `PASSENGER` и `PASSENGER_WAITING`, если процент > 0. |

Интервал поезда — `[timeDeparture первой станции; timeArrival последней)` ∩ работа.

**Пороговые доплаты** (тяжеловесные — `weight`, длинносоставные —
`conditionalLength`): валидный список порогов — процент неотрицательный конечный
(пустая строка = 0), порог — целое > 0 (`"6000,0"` допустимо); при повторе порога
берётся последняя запись; сортировка по возрастанию. Ступень i действует на поезд,
если `значение > T[i]` и (`T[i+1]` нет или `значение ≤ T[i+1]`); значение поезда —
только целое (дробное → поезд не подходит). При пересечении поездов разных ступеней
время забирает старшая. Деньги ступени = `Σ M(сегментов ∩ поезда ступени, кроме
PASSENGER) × p/100`, часы — сумма длительностей.

**Удлинённое плечо**: расстояние маршрута = сумма `train.distance` (конечные > 0).
Ступень — последний порог, который расстояние строго **превышает** (верхней
границы нет). Весь маршрут (сегменты без `PASSENGER` и `PASSENGER_WAITING`)
относится к этой ступени.

### 11.3. Формулы строк начислений

Обозначения: `seg` — сегменты обычных фрагментов месяца; `W` = `getTotalWorkTime`
= Σ `getWorkTime(месяц)` − проезд до явки; `P`, `Wt`, `H` — длительности сегментов
`PASSENGER`, `PASSENGER_WAITING`, `HOLIDAY`; `avg` = `averagePaymentHour`.

| Строка на экране | Код | Часы | % | Сумма |
|---|---|---|---|---|
| Оплата по тарифу | 004L | `max(0, W − P − Wt − Rleg − H − OTreg)`, где `Rleg` — время резервных поездов по старой формуле (интервал поезда, обрезанный только по явке/сдаче, без месяца и перерыва), `OTreg` — переработка по одним обычным фрагментам (без командировки) | — | без смены тарифа: `часы × ставка/3 600 000`. Со сменой — см. ниже |
| Ночные часы | 023L | Отображаемые часы — legacy `List<Route>.getNightTime(userSettings)` (с вычетом перерыва) | `nightTimePercent` (по умолчанию 40; в UI не редактируется — решение, §12) | `Σ M(seg ∋ NIGHT) × %/100` — включая сегменты пассажиром и ожидания |
| Пассажиром | 018L | `P + Pout` | — | `Σ M(seg ∋ PASSENGER) + Σ M(Pout)` |
| Ожидание пассажиром | 018M | `Wt` | — | `Σ M(seg ∋ PASSENGER_WAITING)` |
| Резервом | 052L | Σ сегментов `RESERVE` без `PASSENGER` | — | `Σ M(тех же)` |
| Праздничные | 035L/076L | `H` (включая пассажирские) | — | `Σ M(seg ∋ HOLIDAY) × 2.0` |
| Оплата по среднему | 030A/030B | Σ `effectiveHours` дней отвлечений, кроме `ChildCare`, `BusinessTrip`, `DayOff`, `TechnicalStudy` | — | `avg × целые часы` |
| Оплата недоработки | 048A | `max(0, Нэфф×3 600 000 − Wall − TS)`, `Wall` — отработанное всех исходных маршрутов с проездом до явки, `TS` — часы техзанятий | — | `avg × часы` |
| По уходу за ребенком-инвалидом | — | Дни `ChildCare`: рабочий 8, сокращённый 7, нерабочий 8, праздник 8 (без профиля недели) | — | `avg × часы` |
| Командировка (по среднему) | 030A/030B | `getWorkTime` командировочных фрагментов | — | `avg × часы` |
| Технические занятия | 049A | Σ `Day.hours` дней `TechnicalStudy` | — | `avg × часы` |
| Зональная надбавка | 150A | — | `zonalSurcharge` (25) | `(Σ M(seg без WAITING) + Σ M(Pout)) × %/100` |
| Надбавка за класс квалификации | 025L | — | `surchargeQualificationClass` (0) | `Σ M(seg без PASSENGER/WAITING) × %/100` |
| Доплата за пробег: {Отпр.} — {Приб.} | 011L | — | — | по каждому плечу: `км × ставка` (см. ниже) |
| В одно лицо (грузовые) | 153L | Σ `ONE_PERSON_FREIGHT` без `PASSENGER` | `onePersonOperationPercent` (40) | `Σ M × %/100` |
| В одно лицо (пассажирские) | 153L | Σ `ONE_PERSON_PASSENGER` без `PASSENGER` | `onePersonOperationPassengerTrainPercent` (50) | `Σ M × %/100` |
| Вредность | 057L | — | `harmfulnessPercent` (4) | `(Σ M(все seg) + Σ M(Pout)) × %/100` |
| Районный коэффициент | 026A | — | `districtCoefficient` (%, 0) | `(basicMoney + праздничные) × %/100` |
| Северная надбавка | 027A | — | `nordicPercent` (0) | `(basicMoney + праздничные) × %/100` |
| Переотдых | 172L | Σ интервалов переотдыха в месяце | — | `Σ M × 2/3` |
| Удлиненное плечо ({p}%) | 151L | по ступени | p | по ступени |
| Тяжелые поезда ({p}%) | 152L | по ступени | p | по ступени |
| Длинносост. ({p}%) | 152L | по ступени | p | по ступени |
| Доплата за ПДМ (>6000 т. и >350 осей) | — | Σ `HEAVY_LONG_DISTANCE_TRAIN` без `PASSENGER` | `surchargeHeavyLongDistanceTrains` (5) | `Σ M × %/100` |
| Сдвоенные поезда (30%) / (15%) | 158L | Σ `DOUBLED_TRAIN_FIRST` / `_SECOND` без `PASSENGER` | 30 / 15 | `Σ M × 0.30` / `× 0.15` |
| Сверхурочные часы | 072L | `OT` (11.4) | — | 11.4 |
| Доплата за сверхурочные (50%) / (100%) | 073L / 073M | `half` / `OT − half` | 50 / 100 | 11.4 |
| Прочие надбавки | — | — | `otherSurcharge` (0) | `Σ M(seg без PASSENGER/WAITING) × %/100` |

- **Проезд до явки `Pout`**: для пассажиров с `isWorkStartByArrival` —
  `[отпр.; приб.)` с округлением границ вниз до минуты, ∩ месяц, минус
  `[явка; сдача)` маршрута; куски маршрута объединяются; затем режутся по дате
  смены тарифа.
- **Пробег**: берутся обычные фрагменты маршрутов (по одному на `basicData.id`),
  чья явка (в `crossMonthTZ`) приходится на выбранный месяц. Для каждого поезда с
  `servicePhase` ставка берётся из актуального `userSettings.servicePhases` по `id`
  (если плеча нет — из сохранённого в поезде); ставка `0` — поезд пропускается;
  км = `train.distance` (`,`→`.`), при пустом/нечисловом — расстояние плеча;
  отрицательные/не конечные → 0. Сумма плеча `= км × ставка`; в «Всего
  начислено» каждое плечо входит, округлённое до копеек.
- **Переотдых**: обычные фрагменты месяца + соседи сортируются по явке; для
  маршрута с `restPointOfTurnover` и следующего по списку: `начало = сдача +
  max(getWorkTime маршрута, minTimeRestPointOfTurnover)`; интервал
  `[начало; явка следующего)`, если он не пуст; ∩ месяц (`crossMonthTZ`); разрез
  по тарифу.
- **Оплата по тарифу при смене ставки**: маршруты режутся на две части по
  `00:00 dateNewRate` (`getTwoRouteList`): `[1-е 00:00; dateNewRate 00:00]` и
  `[dateNewRate 00:00; следующий месяц 00:00]`. Для каждой части — формула часов
  из таблицы, но норма берётся за свой период (дни `1…dateNewRate−1` и
  `dateNewRate…последний`), сумма = часы₁ × oldRate + часы₂ × tariffRate.
  Отображаемые часы строки при этом считаются по однопериодной формуле, поэтому
  `часы × ставка` ≠ сумма.
  ⚠️ Поведение Android (возможный баг, BUG-36): переработка в этой ветке
  вычитается по нормам периодов, а строка «Сверхурочные часы» считает её по норме месяца;
  если один период переработан, а другой недоработан, часть отработанных часов не
  оплачивается ни по тарифу, ни как сверхурочные. Перерыв маршрута, пересекающего дату
  смены тарифа, вычитается в обеих частях.

⚠️ Поведение Android (возможный баг, BUG-37): для **переходящего** маршрута
`List<Route>.getWorkTime` (база `W`, «Всего отработано», переработка) берёт часть маршрута
в месяце через `getTimeInCurrentMonth` **без вычета перерыва** — в отличие от сегментов и
`Route.getWorkTimeInMonth`.

### 11.4. Сверхурочные

1. **Норма** `N = selectMonthOfYear.getPersonalNormaHours(profile) × 3 600 000`:
   сумма `profile.effectiveHours(дата, тег)` по дням, кроме дней отвлечений,
   уменьшающих норму (всё, кроме `DayOff`, `BusinessTrip`, `TechnicalStudy`).
2. **Переработка** `OT = max(0, (W + командировка) − H − N)`.
3. **Строка 072L**: без смены тарифа — `OT × ставка / 3 600 000`. Со сменой —
   сначала в новый период: `новые = min(OT, max(0, W₂ − H₂))`,
   `старые = min(OT − новые, max(0, W₁ − H₁))` (части `getTwoRouteList` по
   исходным маршрутам); сумма `= (новые × tariffRate + старые × oldRate)/3 600 000`.
4. **Распределение по сменам**: для исходных маршрутов по возрастанию явки
   `доступно_i = max(0, W(обычных фрагментов) + командировочная часть − H(обычных))`;
   `OT` раздаётся с последней смены назад, каждой не больше `доступно_i`.
5. **Часы 50% (`half`)**:
   - до 09.2026: `min(Σ распределённого, 2 ч × число смен с распределением > 0)`;
   - с 09.2026: `min(Σ min(распр._i, 2 ч), max(0, 120 ч − annualOvertimeBeforePeriod))`.
   Часы 100% на экране = `OT − half` (ограничения в 240 ч нет).
6. **Деньги 50%/100%**:
   - до 09.2024: `half × tariffRate/3 600 000 × 0.5` и
     `(OT − half) × tariffRate/3 600 000` (текущая ставка даже при смене тарифа);
   - с 09.2024: если `W ≤ 0` — `f = max(tariffRate, avg)/3 600 000`,
     50% = `half × f × 0.5`, 100% = `(OT − half) × f`. Иначе:
     1. из сегментов без `PASSENGER`/`PASSENGER_WAITING` и без `HOLIDAY`
        строится единая шкала (пересечения маршрутов не дублируются: на атомарном
        отрезке — максимальная ставка и объединение условий, соседние одинаковые
        сливаются) и с конца месяца назад выбирается `min(OT, доступное)` времени;
     2. выбранные сегменты в хронологическом порядке: первые `half` мс — «50%»,
        остальные — «100%»;
     3. `база(s) = M(s) × (1 + Σ%/100)` по условиям сегмента: ночь (night%),
        одно лицо грузовой/пассажирский, вредность, классность, зональная,
        прочие, ПДМ, сдвоенный первый 30, второй 15;
     4. поездные ступени (тяжёлые, длинносоставные, удлинённое плечо):
        `Σ длит(сегм ∩ сегм ступени) × ставка сегмента/3 600 000 × p/100`;
     5. 50% = `Σ база(«50%») × 0.5 + ступени(«50%») × 0.5 + max(0, half −
        длит(«50%»)) × f × 0.5`; 100% = `Σ база(«100%») + ступени(«100%») +
        max(0, (OT − half) − длит(«100%»)) × f`, `f` как выше.

### 11.5. Итоги, удержания и округление

`round(x) = знак × floor(|x| × 100 + 0.5) / 100`; `NaN`/∞ → 0.

1. `basicForOT = round(Σ round(x))` по: тариф, пассажиром (в смене), ожидание,
   резервом, зональная, ночные, классность, удлинённое плечо (`Σ round` ступеней),
   одно лицо грузовой, одно лицо пассажирский, вредность, тяжёлые (`Σ round`),
   длинносоставные (`Σ round`), ПДМ, прочие, сдвоенный 30%, сдвоенный 15%.
2. `basicMoney = round(round(basicForOT) + round(072L) + round(50%) + round(100%))`.
3. Районный и северный: `(basicMoney + праздничные_неокругл.) × %/100`.
4. `Всего начислено = round(Σ round(y))` по: `basicMoney`, праздничные, по
   среднему, уход за ребёнком-инвалидом, командировка, техзанятия, северная,
   районный, недоработка, пробег (`Σ round` плеч), переотдых, проезд до явки.
5. НДФЛ = `round((Всего − уход_неокругл.) × ndfl/100)`; Профсоюз, Прочие,
   Благосостояние = `round(Всего × %/100)`; база алиментов =
   `round(Всего − НДФЛ − Профсоюз − Прочие − Благосостояние)`; Алименты =
   `round(база × %/100)`; `Всего удержано = round(Σ round)`;
   `К выдаче = round(Всего начислено − Всего удержано)`.

Строка «Пассажиром» на экране показывает `round(в смене + до явки)`, а в итог
обе части входят по отдельности округлёнными — возможна разница в 1 копейку
между строкой и её вкладом в итог.

### 11.6. Экран: состав и состояния

- **Топбар**: слева стрелка «Назад» (закрывает экран); по центру поле-кнопка с
  лупой и серым текстом «Расшифровать код» (контур 1 dp, скругление 12 dp) →
  11.8; справа иконка настроек → `showSettingSalary`.
- **Загрузка**: пока идёт пересчёт, всё содержимое заменяется спиннером с текстом
  «Пересчет...».
- **Содержимое** (сверху вниз, горизонтальный отступ 16 dp):
  1. Переключатель месяца: «{Месяц}» + «{год}» (тап → 11.7) и стрелки ‹ › по
     отсортированному списку `(год, месяц)` производственного календаря; на
     краях стрелка неактивна (прозрачность 35%). Выбор пишет
     `settings.selectMonthOfYear` — месяц меняется во всём приложении.
  2. Hero: «К ВЫДАЧЕ» + сумма `toBeCredited` в формате «69 928,32» и « ₽/₸/Br»;
     справа «ВСЕГО ОТРАБОТАНО» + время `getTotalWorkTimeWithCommute` (формат
     `ЧЧ:ММ` либо десятичный «7,50» при `isDecimalTime`).
  3. Карточка-предупреждение «Не установлена тарифная ставка. Перейдите в
     настройки для её указания.» (не кликабельна) — если строка тарифа пуста или
     её первое число равно 0. ⚠️ Поведение Android (возможный баг, BUG-38): при смене тарифа
     проверяется **старая** ставка (первое число строки `"старая / новая ₽"`).
  4. «НАЧИСЛЕНИЯ»: таблица `КОД | ВИД ВЫПЛАТЫ | ЧАСЫ | % | СУММА`, затем строка
     «Всего начислено». Пустая ячейка — «—» бледным.
  5. «УДЕРЖАНИЯ»: таблица `КОД | ВИД УДЕРЖАНИЯ | % | СУММА` (строки «НДФЛ»,
     «Профсоюз», «Благосостояние», «Алименты», «Прочие удержания»), затем
     «Всего удержано». ⚠️ Поведение Android (возможный баг, BUG-39): в столбце «%» у НДФЛ
     всегда `13,0`, даже если в настройках задан другой процент; у остальных удержаний «—».
  6. Тёмный блок «К ВЫДАЧЕ» с суммой.
  7. Иконка «i» и текст «Расчёт носит информационный характер: некоторые виды
     выплат могут отличаться в зависимости от нормативных документов вашего депо.»
- **Видимость строк**: любая строка начислений и удержаний показывается только
  при сумме `> 0` (сравнение до округления).
- **Порядок строк**: стабильная сортировка по первому коду из
  `PayrollPaymentCatalog` как строке (`"004L" < "011L" < … < "172L"`); строки без
  кода («По уходу за ребенком-инвалидом», «Доплата за ПДМ…», «Прочие надбавки»,
  «Прочие удержания») — в конце в исходном порядке. Исходный порядок
  начислений: тариф, ночные, пассажиром, ожидание, резервом, праздничные, по
  среднему, недоработка, уход, командировка, техзанятия, зональная, классность,
  пробег (по плечам), одно лицо груз./пасс., вредность, районный, северная,
  переотдых, удлинённое плечо, тяжёлые, длинносоставные, ПДМ, сдвоенные 30/15,
  сверхурочные (072L, 50%, 100%), прочие надбавки.
- **Форматы**: деньги — `str2decimalSign` («1 234,56», неразрывный пробел в
  разрядах, всегда 2 знака); проценты — `"%.1f"` с запятой («40,0»); часы —
  `ЧЧ:ММ` (часы не ограничены, минуты отбрасываются вниз) либо десятичный
  формат `Ч,ДД` (доля часа, округление до сотых).
- **Инфо-окно недоработки** (`AppAlertDialog`, если `underworkTime > 0` и средний
  час не задан/`≤ 0`/не конечен и окно не было закрыто навсегда): заголовок
  «Оплата недоработки», текст «За выбранный период отработано меньше нормы.
  Укажите средний час в настройках зарплаты — и приложение рассчитает оплату
  недоработки за недостающие часы.»; «В настройки» — скрыть до конца сессии
  экрана и открыть настройки ЗП; «Понятно» — больше никогда не показывать
  (флаг `isUnderworkInfoDismissed` в SharedPreferences); тап вне окна — скрыть
  до конца сессии экрана.

### 11.7. `SalaryMonthSheet` (выбор месяца расчёта)

- Нижняя шторка (раскрыта полностью), заголовок «Выберите месяц и год».
- Два ряда чипов: все номера месяцев, встречающиеся в календаре (по возрастанию,
  без учёта года), и все годы. Выбор месяца и года независимый; изначально
  отмечены текущие.
- «Применить» → `selectYearAndMonth(год, месяц)` и закрытие шторки. Если такой
  пары нет в календаре, выбор молча игнорируется. Свайп/тап вне — закрыть без
  изменений. Шторка открывается только когда месяц и год уже известны.

### 11.8. `PayrollCodeSearchScreen` (справочник кодов)

- Не отдельный nav-destination: состояние экрана расчёта `isCodeSearchActive`
  подменяет содержимое целиком. Текст запроса сохраняется между открытиями.
- Топбар: стрелка «Назад к расчётному листу», поле ввода (лупа слева, крестик
  «Очистить поиск» при непустом тексте, плейсхолдер «Расшифровать код»),
  справа — настройки ЗП. При открытии поле получает фокус и показывается клавиатура.
  Системный Back возвращает к расчётному листу.
- Список: карточка «Введите код, служебное название или слова из расшифровки
  любого удержания или начисления.», строка «Найдено: N», затем карточки
  (код крупно, чип «Начисление»/«Удержание», служебное название, расшифровка).
  Пусто — «Совпадений нет. Проверьте код или попробуйте часть названия.»
- Поиск (`PayrollCodeReferenceCatalog.search`, 286 записей в общем KMP-коде):
  запрос режется по пробелам, всё в нижнем регистре. Запись подходит, если
  **каждое** слово является подстрокой текста «код + служебное название +
  расшифровка + (“начисление начисления” | “удержание удержания”)» либо, если в
  слове есть цифра, нормализованный код содержит нормализованное слово
  (кириллица → латиница: а→a, в→b, е→e, к→k, м→m, н→h, о→o, р→p, с→c, т→t, у→y,
  х→x). Пустой запрос — весь список. Порядок: начисления, затем удержания;
  внутри — по числовой части кода, затем по коду.

### 11.9. Шторка «Расчёт за смену» (`CalcBottomSheet`)

Открывается из формы маршрута (§5.3 п.6, §5.4); описание здесь, так как считает
тот же `SalaryCalculationHelper`.

- Заголовок «Расчёт за смену», подзаголовок «Маршрут №{номер}» (если номер не пуст).
- Нет явки или сдачи — карточка «Укажите начало и окончание рабочего времени для
  расчёта заработной платы за поездку».
- Иначе: плашка «Заработано» + `totalPayment` («12 345,67 ₽»). Если в маршруте
  есть командировочная часть, а её оплата 0 — блок «Маршрут в командировке» +
  «Оплачивается только по среднему часу, без надбавок. Средний час не указан —
  поэтому сумма 0. Нажмите, чтобы задать его в настройках зарплаты.» (тап →
  настройки ЗП).
- Строки (только ненулевые), у каждой подпись-пояснение: «Почасовая оплата»
  (`ЧЧ:ММ × ставка/ч`), «Праздничные» (`ЧЧ:ММ в праздничные дни`), «Зональная
  надбавка», «Ночные», «Одно лицо» (`ЧЧ:ММ × ставка/ч × %`), «Пассажиром»,
  «Пассажиром до явки» (`ЧЧ:ММ × ставка/ч`), «Доплата за пробег» по каждому плечу
  (`{плечо}: {км} км × {ставка}/км`), «Доплаты за поезд» (перечень применённых:
  тяжеловесный, длинносоставный, удлинённое плечо, сдвоенный), «Прочие доплаты»
  (= классность + северная + районный + вредность + прочие), «Переотдых»
  (`ЧЧ:ММ × ставка/ч × 2/3`), «Командировка (по среднему)». Если исходных данных
  для формулы нет — текстовая подсказка.
- При тарифной ставке 0 — курсивная подчёркнутая ссылка «Установите значение
  тарифной ставки в настройках.»; внизу всегда строка «Настройки зарплаты».
  Любой переход в настройки сначала закрывает шторку.
- Расчёт: `SalaryCalculationHelper(userSettings, salarySetting, [маршрут],
  profile)` без нормы (сверхурочные и недоработка не считаются).
  `totalPayment` = тариф (`getMoneyAtWorkTimeAtTariffSingleRoute`, не ниже 0) +
  ночные + зональная + пассажиром + ожидание пассажиром + проезд до явки +
  праздничные + пробег + поездные доплаты + одно лицо (пассажирский процент, если
  хотя бы один поезд пассажирский по номеру, иначе грузовой) + «прочие доплаты» +
  переотдых + командировка; без округления. Переотдых: предыдущий по явке маршрут
  среди маршрутов месяца явки и предыдущего месяца, полный интервал без обрезки
  по месяцу × 2/3 × `selectMonthOfYear.tariffRate`. Маршрут целиком в
  командировке — итог = только оплата командировки.
- ⚠️ Поведение Android (возможный баг, BUG-40): ожидание пассажиром (018M) входит в
  «Заработано», но отдельной строки в шторке нет — сумма строк меньше итога.
- ⚠️ Поведение Android (возможный баг, BUG-41): расчёт использует
  `selectMonthOfYear` (ставку и границы месяца), а не месяц маршрута. Для маршрута из
  другого месяца сегменты пусты: ночные/зональная/пассажиром и т.п. = 0, а тариф считается
  за всё время (❓ не проверено на устройстве).
- Та же сумма (без учёта командировочного обнуления переотдыха) используется как
  «Расчёт за смену» в Календаре и быстром просмотре (`computeRouteTotalPayment`).

Источник: `domain/src/commonMain/kotlin/com/z_company/domain/salary/SalaryCalculator.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/salary/UnderworkPeriod.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/SalarySegment.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/RouteSalarySegments.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/OvertimeSegmentCalculation.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/CalculateNightTime.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/TimeCalculationContext.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/DoubleUtil.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/StringUtil.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/route/UtilsForEntities.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/route/OverRestRoutes.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/UtilForMonthOfYear.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/WorkScheduleProfile.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/salary/PayrollPaymentCatalog.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/salary/PayrollCodeReferenceCatalog.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/use_cases/RouteUseCase.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/use_cases/NormaUseCase.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/SalaryCalculationViewModel.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/SalaryCalculationUIState.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/SalaryCalculationHelper.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/RouteSalaryCalculator.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/FormViewModel.kt` (`calculateSalary`),
`features/route/src/main/java/com/z_company/route/ui/SalaryCalculationScreen.kt`,
`features/route/src/main/java/com/z_company/route/ui/FormScreen.kt` (`CalcBottomSheet`),
`core_android/src/main/java/com/z_company/core/util/ConverterLongToTime.kt`.

---

## 12. Экран «Настройки зарплаты» (SettingSalaryScreen)

Ввод всех параметров расчёта (`SalarySetting` + `MonthOfYear.tariffRate`).

- **ViewModel**: `SettingSalaryViewModel`. Каждое поле — свой сеттер; значения —
  строки с флагом ошибки ввода (`isErrorInput…`). Все поля в `SettingSalaryUIState`.
- **Тарифная ставка** — особый случай. Хранится не в `SalarySetting`, а в
  `MonthOfYear.tariffRate` выбранного месяца (`UserSettings.selectMonthOfYear`),
  вместе с датой смены тарифа внутри месяца (`dateSetTariffRate`:
  `dateNewRate` + `oldRate` — ставка до этой даты).
  - Если ставка была изменена, то **при выходе с экрана** (кнопка «Назад» и
    системный back) вместо закрытия открывается нижняя шторка «Изменилась
    тарифная ставка» с выбором области применения: «Только для этого месяца»
    (`saveSettingAndOnlyMonthTariffRate`) или «Для этого и следующих»
    (`saveSettingAndTariffRateCurrentAndNextMonth`). «Отмена» закрывает шторку и
    оставляет пользователя на экране. Если ставка не менялась, back закрывает
    экран сразу.
  - **Прошлые месяцы не изменяются ни в одном из вариантов.** «Для этого и
    следующих» переносит ставку в месяцы, у которых пара (год, месяц) строго
    больше текущей; у них `dateSetTariffRate` сбрасывается в `null` (дата смены
    относилась к прежнему тарифу). Месяцы-дубликаты одного года+месяца в
    локальной БД обновляются все, чтобы UI не подхватил строку со старой ставкой.
  - Синяя подпись даты в карточке ставки («на {день} {Месяц} {год}», у старой
    ставки — «до …») открывает шторку «Изменилась тарифная ставка»
    (`showDialogTariffRate`) — даже если ставка не менялась. Выбор календарной
    даты (`setDateSetTariffRate`) открывается только из этой шторки тапом по
    строке «Новый тариф начнёт действовать с …»: шторка сначала закрывается, а
    после выбора даты или отмены пикера открывается снова — две модальные шторки
    одновременно не показываются. День `1` означает «нового тарифа внутри месяца
    нет» и очищает `dateSetTariffRate`. Дата из другого месяца переключает
    редактируемый месяц: в поле подставляется ставка того месяца. При первой
    установке даты `oldRate` = ставка, загруженная при открытии экрана.
  - После выбора варианта месяцы записываются в локальную БД, затем сохраняются
    `SalarySetting` и `UserSettings.selectMonthOfYear`, и только после этого
    настройки выгружаются на сервер; затем экран закрывается.
- **Синхронизация тарифной ставки.** Любое изменение ставки, старой ставки или
  даты смены тарифа сразу взводит флаг `settingsSyncPending`, а запись месяца
  (в том числе в `onCleared` при быстром выходе) заканчивается выгрузкой настроек
  (`SyncManager.autoPushSettings`) в `NonCancellable`-контексте. Без этого
  двусторонняя синхронизация не пушит локальные месяцы и возвращает старую ставку
  с сервера. Порядок «сначала запись в БД, потом push» обязателен.
- Все остальные числовые поля, тумблер и изменения строк в списках порогов
  сохраняются автоматически с debounce 500 мс. При закрытии экрана ожидающее
  автосохранение дублируется в `NonCancellable`-контексте, чтобы быстрый выход не
  приводил к потере введённых значений. Невалидное одиночное значение не меняет
  сохраняемую модель. Списки порогов сохраняются атомарно: если вес/длина/
  расстояние не являются положительным конечным числом либо процент не является
  неотрицательным конечным числом, в БД остаётся последняя корректная версия
  всего соответствующего списка, в том числе при быстром выходе с экрана.
- Поля: средний час, районный коэффициент, северный коэффициент, зональная надбавка,
  классность, «в одно лицо» (грузовой/пассажирский %), вредность %, прочая надбавка,
  НДФЛ, профсоюз, прочие удержания, **Благосостояние**, **Алименты**. Отдельное поле
  «Доплата за ПДМ (>6000 т. и >350 осей)» задаёт процент доплаты (по умолчанию 5%);
  критерии фиксированы и пользователем не редактируются.
  - **Благосостояние** (`welfarePercent`, `setWelfarePercent`) и **Алименты**
    (`alimonyPercent`, `setAlimonyPercent`) — оба поля хранят процент; база
    расчёта (грязная/чистая сумма) не настраивается, см. раздел 11.
- **Списки порогов** (добавить/изменить/удалить строку): доплаты за тяжеловесные
  (вес→%), длинносоставные (длина→%), удлинённое плечо (расстояние→%).
  Для тяжеловесного, длинносоставного поезда и удлинённого плеча введённое
  значение является строгой границей: доплата (и статус поезда в карточках /
  календаре) начинается только при значении **больше** порога, равенство порогу
  ступень не включает; верхняя граница ступени — включительно (значение, равное
  следующему порогу, остаётся в нижней ступени). Одно правило в
  `buildTieredTrainSurchargeSegments` (по умолчанию `thresholdIsInclusive = false`),
  `getTimeInHeavyTrain`/`getTimeInLongTrain`/`getTimeInServicePhase` и в PWA.
  Рядом с названиями длинносоставного и удлинённого плеча показывается
  информационная кнопка, открывающая пояснение с примером.
- **Недоработка считается только по закрытому месяцу**
  (`isUnderworkPeriodClosed`): прошлый месяц — по полной норме, текущий — только
  в его последний календарный день, будущий и середина текущего — 0 (норма «на
  дату» для недоработки не используется). Правило общее для Android, iOS и PWA.
- **Показывать оплату недоработки** (`showUnderworkPayments`,
  `setShowUnderworkPayments`) — тумблер отдельной карточкой в самом низу
  раздела «Начисления» (после «Другие надбавки», перед «Удержания»).
  Boolean, default `true`. Выключение полностью отключает строку «Оплата
  недоработки» и инфо-окно про средний час на экране «Расчёт зарплаты»
  (раздел 11) — не только визуально, расчёт тоже не выполняется.
- **Хранение и синхронизация:** процент хранится в
  `SalarySetting.surchargeHeavyLongDistanceTrains` (локально `REAL`, default `5`)
  и передаётся одноимённым необязательным полем API. Если старый клиент не
  прислал поле при POST, сервер сохраняет прежнее значение, а не обнуляет его.
  Так же устроены `welfarePercent` и `alimonyPercent` (локально `REAL`,
  default `0`; на сервере — `Optional[float] = None` в `SalarySettingResponse`,
  в апсерт попадают только если `is not None`). `showUnderworkPayments` —
  тот же паттерн: локально `INTEGER` 0/1 (default `1`), на сервере
  `Optional[bool] = None`.

### 12.1. Состав экрана (сверху вниз)

Топбар: стрелка «Назад» (логика выхода — выше) и заголовок «Зарплата». Ошибка
сохранения показывается snackbar «Ошибка: {текст}».

**«Начисления»** (крупный заголовок), далее карточки:

1. «Тарифная ставка» (суффикс «₽») с синей подписью «на {день} {Месяц} {год}»
   (день = `dateNewRate` либо `1`). Если задана дата смены тарифа, ниже второе
   поле «Тарифная ставка» с подписью «до {день} {Месяц} {год}» — старая ставка
   (`oldRate`). Затем «Средний час» (₽).
2. «Зональная надбавка», «Доплаты за класс и права» (`surchargeQualificationClass`),
   «Работа в одно лицо (грузовой)», «Работа в одно лицо (пассажирский)»,
   «Доплата за вредность», «Северная надбавка», «Районный коэффициент» — все в «%».
3. «Доплата за ПДМ (>6000 т. и >350 осей)» (%).
4. Три карточки порогов: «Доплата за тяж. поезда» (поля «т.» и «%»),
   «Доплата за длинносост. поезда» («ваг.», «%»), «Доплата за удлиненное плечо»
   («км», «%»). В шапке карточки — «Добавить» (добавляет пустую строку и
   запускает автосохранение). У длинносоставных и плеча рядом с названием кнопка
   «?» (у тяжеловесных её нет) → шторка-справка с кнопкой «Понятно»:
   - «Граница длинносоставного поезда»: «Доплата начинается только когда условная
     длина поезда больше указанного значения. При равенстве порогу доплата ещё не
     начисляется. Например: для порога 80 доплата действует с 81 условного вагона.»
   - «Граница удлинённого плеча»: «Доплата начинается только когда пробег больше
     указанного значения. При равенстве порогу доплата ещё не начисляется.
     Например: для порога 250 км доплата действует при пробеге свыше 250 км.»
   Строка порога удаляется свайпом влево («Удалить») → шторка «Удалить доплату?»
   с действием «Да, удалить»; отмена возвращает строку на место. Поля порогов
   ошибку ввода не показывают.
5. «Другие надбавки» (%).
6. Переключатель «Показывать оплату недоработки».

**«Удержания»**: «Подоходный налог» (`ndfl`), «Профсоюз», «Прочие удержания»,
«Благосостояние», «Алименты» — все в «%».

**Решение (зафиксировано): процент ночных (`SalarySetting.nightTimePercent`) в UI не
редактируется и редактироваться не будет** — ни на Android, ни в iOS/PWA; поле ввода не
добавлять. Значение по умолчанию — 40%; если с сервера пришло другое значение, расчёт
использует его как есть (§11.3).

### 12.2. Ввод и валидация

- Поля числовые (десятичная клавиатура), значение разбирается
  `toNonNegativeFiniteDoubleOrNull`: пробелы удаляются, запятая = точка, **пустая
  строка = 0**; отрицательное, `NaN`, ∞ или текст — ошибка «Некорректные данные»
  под полем, в модель не пишется (остаётся последнее корректное значение).
- Значения отображаются через `str()`: целое без дробной части (`40`), иначе как есть.
- Все поля, кроме тарифа, после корректного ввода сохраняются автоматически через
  500 мс (`saveLocalEdit` ставит `updatedAt`, затем `autoPushSettings`).
- ⚠️ Поведение Android (возможный баг, BUG-42): поле старой ставки
  подсвечивает ошибку по флагу **новой** ставки (`isErrorInputTariffRate`), собственный
  флаг `isErrorInputOldTariffRate` на экран не передаётся. После выбора даты старая ставка
  показывается через `toString()` («1234.0»), а не `str()`.

### 12.3. Шторка «Изменилась тарифная ставка»

`AppBottomSheet`: заголовок «Изменилась тарифная ставка», текст «Для какого месяца
сохранить тариф? Прошлые месяцы останутся со старой ставкой.», синяя строка «Новый
тариф начнёт действовать с {день} {Месяц} {год}» (тап → выбор даты
`DateRangePickerBottomSheet` с заголовком «Дата начала действия нового тарифа»,
берётся первая выбранная дата), действия «Только для этого месяца» и «Для этого и
следующих», «Отмена». Любое из двух действий сохраняет данные (12, выше) и
закрывает экран.

Источник: `features/route/src/main/java/com/z_company/route/ui/SettingSalaryScreen.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/SettingSalaryViewModel.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/SettingSalaryUIState.kt`,
`features/route/src/main/java/com/z_company/route/navigation/SettingSalaryDestination.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/use_cases/SalarySettingUseCase.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/setting/SalarySetting.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/salary/UnderworkPeriod.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/util/StringUtil.kt`.

---

## 13. Экран «Статистика» (StatisticsScreen)

Аналитика по маршрутам: вкладки «Месяц», «Год», «История». ViewModel считает
метрики (переиспользуя `UtilsForEntities` и `SalaryCalculationHelper` — те же
расчёты, что на главном) и отдаёт УЖЕ отформатированный `StatisticsUiState`; экран
только рендерит.

- **ViewModel**: `StatisticsViewModel`. Вкладки (`setTab`), навигация по периодам
  (`prevPeriod`/`nextPeriod`), сравнение с базовым периодом (`setCompare` /
  `setCompareCustomMonth` / `setCompareCustomYear`).
- **Месяц**: сетка метрик (`StatMetric`: значение, дельта к базовому периоду, мини-
  бар текущий/предыдущий), первая метрика крупная; топ направлений
  (`StatTopDirection`).
- **Год**: hero-значение + помесячные столбцы (`StatMonthBar`, широкие — 14dp текущий/
  10dp предыдущий). Тап по столбцу — локальная UI-подсказка (`selected` состояние
  внутри `YearBarsChart`, без обращения к ViewModel и без перехода на другой экран):
  над столбцом появляется число часов за этот месяц, повторный тап скрывает. Ripple
  при тапе не показывается: столбец не является кнопкой или переходом.
  Аналогичный паттерн (число над активным столбцом) уже был в `DetailBars`
  детализации метрики — обе шкалы переиспользуют один форматтер
  (`formatBarValueLabel`).
- **История**: итоги за всё время (`StatHistoryMetric`), строки «год за годом»
  (`StatYearRow`), выбор метрики (`selectHistoryMetric`).
- **Детализация метрики** (тап по плашке, `openDetail`): overlay с hero, помесячными
  интерактивными столбцами (`StatDetailBar`, выбор месяца — `selectDetailMonth`) и
  разбивкой по направлениям (donut, `StatDonutSeg`). Все столбцы имеют фиксированную
  ширину 14dp, поэтому график не меняет геометрию при выборе месяца; ripple при тапе
  не показывается.
- Форматтеры (`StatFormat`): часы:минуты, деньги (тыс./млн + валюта), расстояние
  (км/тыс. км), русские десятичные, склонение «год/года/лет».
- **Настройка «Учитывать будущие маршруты»** (`UserSettings.isConsiderFutureRoute`):
  учитывается во всех расчётах статистики. При выключенной настройке маршруты
  фильтруются в источнике (`allRoutes`) через `filterByConsiderFutureRoute` — ещё не
  начавшиеся (`timeStartWork >= now`) исключаются, так же как на Главном и на экране
  «Зарплата». За счёт фильтра в источнике согласованы и метрики (время, зарплата,
  пробег, направления, детализация, история), и диапазоны навигации/пикеры периодов:
  при выключенной настройке будущие месяцы/годы не появляются в выборе периода и
  сравнения.
- **Принадлежность маршрута месяцу** (`routesOfMonth`): статистика грузит ВСЕ
  маршруты (`getListRoutesAsFlow`) и фильтрует по месяцу в памяти. Правило:
  - завершённый маршрут (`timeEndWork != null`) — по пересечению с месяцем
    (`start < конецМесяца && end >= началоМесяца`); переходной попадает и в месяц
    явки, и в месяц сдачи — как в `routeListByMonthFlow`;
  - **открытый маршрут** (сдача не проставлена, `timeEndWork == null`) — принадлежит
    ТОЛЬКО месяцу своей явки (`timeStartWork` внутри месяца). Это важно: без такого
    условия «висящий» маршрут из прошлого месяца учитывался бы во всех последующих
    месяцах и завышал бы расстояние/скорость/грузооборот. На Главном/ЗП этого не
    происходит, потому что там месяц грузится SQL-запросом `getByPeriod`, который
    открытые маршруты берёт только по `timeStartWork` в окне месяца.
- **Переходные маршруты** (явка в одном месяце, сдача в другом) по членству попадают
  в ОБА месяца, но метрики учитывают их по-разному:
  - **в календаре** месяц явки определяется в часовом поясе ввода и отображения
    (`displayTimeZone`; для РФ — МСК), независимо от `crossMonthTimezone`.
    Поэтому явка 30 сентября по МСК остаётся в сентябре и при локальном ЧП
    Красноярска;
  - **по времени** (отработанное, переработка, ночные, пассажиром, зарплата) —
    клипаются по границе месяца (МСК/`crossMonthTZ`, `getTimeInCurrentMonth` /
    `clipToMonth`): каждый месяц получает свою часть, сумма = целое, без задвоения;
  - **по поездам** (расстояние, грузооборот, время в пути, средняя скорость, топ
    направлений) — считаются по **месяцу явки** (`trainRoutesOfMonth` берёт из
    `routesOfMonth` только маршруты с `timeStartWork` внутри месяца). Неделимый пробег
    поезда нельзя резать по полуночи, а учтённый целиком в оба месяца он задваивал бы
    годовой/исторический итог (`rawYear` = сумма месяцев);
  - **счётчик «Маршрутов (смен)»** — по членству (в обоих месяцах), как на Главном.
    Следствие: в годовом/историческом итоге переходной маршрут внутри одного года в
    счётчике смен учитывается дважды — это осознанный компромисс ради совпадения
    помесячного счётчика с Главным.
- **Метрика «Переработка»** (`overtime`, месяц/год/детализация — единая логика,
  `Raw.overtimeMs`): `max(0, отработано_за_месяц − ПОЛНАЯ_месячная_норма)`, т.е.
  всегда против полной нормы месяца (не «на сегодня» — та норма используется только
  для оплаты недоработки, см. `effectiveNormaForUnderwork`, это другой расчёт) и
  никогда не уходит в минус: показывается только переработка, недоработки в этой
  метрике нет. Месяц без маршрутов даёт 0 переработки (не «минус норма»). Год —
  **сумма уже клэмпнутых к нулю месячных значений**, а не `(отработано_за_год −
  норма_за_год)`: недоработка одного месяца не должна вычитать переработку другого.
  Детализация (`openDetail`/`detailValue`) показывает те же помесячные значения, что
  суммируются в годовой итог (месяц без маршрутов — «—», а не «0»/данных нет).

### 13.1. Источники и пересчёт

- Данные: все маршруты (`getListRoutesAsFlow`), `UserSettings`, список
  `MonthOfYear` календаря; изменения схлопываются debounce 150 мс. Маршруты без
  `timeStartWork` отбрасываются. Настройки зарплаты читаются при каждом пересчёте.
- Начальный период — `selectMonthOfYear`. Выбранный на экране месяц/год
  **не** записывается в настройки.
- Пересчёт идёт в один поток; полноэкранная загрузка (круговой индикатор, «Считаем
  статистику…» / «Собираем показатели за период») показывается минимум 550 мс,
  если данных вкладки ещё нет. Смена вкладки мгновенно очищает прежние данные и
  показывает загрузку.
- Месяц без маршрутов: зарплата считается только если положена оплата
  недоработки (норма закрытого месяца > 0 и средний час > 0), остальные
  метрики — 0.

### 13.2. Метрики (`Raw` за месяц)

| Ключ | Подпись плашки | Формула | Формат |
|---|---|---|---|
| `worked` | Отработанное время | `routes.getWorkTime(месяц, ctx)` (с проездом до явки, клип по месяцу) | `Ч:ММ` |
| `overtime` | Переработка | `max(0, worked − getPersonalNormaHours(profile) × 3 600 000)`, пустой месяц — 0 | `Ч:ММ` |
| `earnings` | Заработано | `SalaryCalculationHelper(...).getMoneyToBeCredited()` — **«К выдаче» после удержаний**; параметры: месяц, маршруты месяца, `effectiveNormaForUnderwork`, профиль недели | деньги |
| `speed` | Средняя скорость | `distance / (transit в часах)`, 0 при `transit = 0` | `дес(1)` + «км/ч» |
| `transit` | Время в пути | Σ `getTravelTime` поездов (≥ 2 станций, от отпр. 1-й до приб. последней, округлено вниз до минуты) | `Ч:ММ` |
| `distance` | Расстояние | Σ `train.distance.toDoubleOrNull()` | км |
| `tkm` | Грузооборот | Σ `distance × weight` (`toDoubleOrNull`, без замены запятой) | `дес(tkm/10⁶, 2)` + «млн ткм» |
| `night` | Ночные часы | `routes.getNightTime(settings)` | `Ч:ММ` |
| `routes` | Маршрутов (смен) | число маршрутов месяца по членству | целое |

Год — сумма 12 месячных `Raw` (скорость пересчитывается из сумм). Порядок плашек:
месяц — `worked` (крупная на всю ширину), `overtime`, `earnings`, `speed`,
`transit`, `distance`, `tkm`, `night`, `routes`; год — те же без `worked` (его
заменяет hero). При системном шрифте > 115% плашки идут по одной в ряд.

Форматы (`StatFormat`): `Ч:ММ` — часы без ограничения, минуты вниз; деньги —
`≥ 1 000 000` → `дес(x/10⁶, 2)` + «млн ₽», `≥ 1 000` → `дес(x/10³, 1)` + «тыс. ₽»,
иначе целое + «₽» (валюта по стране: ₽/₸/Br); расстояние — `≥ 1000` →
`дес(км/1000, 1)` + «тыс. км», иначе целое + «км»; десятичные — округление
`roundToInt` до нужных знаков, запятая, минус «−»; целые — с пробелами по разрядам.

⚠️ Поведение Android (возможный баг, BUG-43): «Заработано» считается без
`annualOvertimeBeforePeriod` и без соседей для переотдыха, а маршруты берутся фильтром в
памяти (а не `listRoutesByMonth`), поэтому для месяцев с 09.2026 и при переотдыхе на
стыке месяцев сумма может отличаться от «К выдаче» на экране «Расчёт зарплаты».

### 13.3. Сравнение

- Опции месяца: «{Месяц} {год}» / «предыдущий месяц» (`prev`, по умолчанию),
  «{Месяц} {год−1}» / «этот месяц год назад» (`yearago`), «Выбрать месяц…» /
  «любой из истории» (открывает `MonthGridPicker`), «Не сравнивать» / «показать
  только выбранный период» (`none`).
- Опции года: «{год−1} год» / «предыдущий год», «Выбрать год…» / «любой из
  истории», «Не сравнивать» / «показать только выбранный год».
- Шторка `ComparePicker`: заголовок «Сравнить месяц с» / «Сравнить год с»,
  подзаголовок «С чем сравнивать выбранный период».
- `MonthGridPicker`: «Выбрать месяц», «Сравнить можно с месяцем, где есть
  маршруты»; `YearListPicker`: «Выбрать год», «Сравнить с любым годом из истории».
  Доступны месяцы/годы, где есть явка или сдача маршрута, кроме текущего.
  `MonthGridPicker` — по каждому году сетка 4×3 коротких названий («Янв»…«Дек»),
  недоступные приглушены (30%). В шапке обоих пикеров — стрелка «назад» к `ComparePicker`;
  закрытие свайпом/тапом вне закрывает всю цепочку.
- База без маршрутов → дельты не показываются, под селектором «В периоде
  «{название}» нет маршрутов — сравнивать не с чем».
- Дельта: направление `up`/`down`/`flat` (|разница| < 1e-6); процент
  `round((тек − база)/база × 100)` со знаком «+»/«−» и «%»; при базе 0 — `+100%`
  (или `0%`, если текущее тоже 0). Под значением «было {значение базы}».
- Селектор над плашками: «в сравнении с» + название, при `none` — «режим» +
  «Без сравнения».

### 13.4. Вкладки, навигация и детализация

- Сегмент «Месяц» / «Год» / «История». Навигация `PeriodNav` (на «Истории» нет):
  «{Месяц} {год}» / «МЕСЯЦ» либо «{год} год» / «ГОД», стрелки «Предыдущий
  период» / «Следующий период». Диапазон — от самого раннего месяца (года) явки до
  максимума из самого позднего и `selectMonthOfYear`.
- Пустой период: «За месяц данных нет» / «За год данных нет» + «В этом периоде ещё
  не закрыто ни одного маршрута.»
- «Топ направлений» (месяц; «Топ направлений за год» — маршруты с явкой в этом
  году): поезда ≥ 2 станций с непустыми названиями первой и последней, ключ
  «Первая → Последняя», сортировка по числу поездок, первые 4; donut с центром
  «{сумма поездок} поездок», у каждой строки время в пути `Ч:ММ`.
- Год: hero «ОТРАБОТАНО ЗА ГОД · Ч» — целые часы (`worked / 3 600 000`, вниз),
  дельта; столбцы по месяцам (текущий год и, при сравнении, база; легенда
  «предыдущий»).
- История: плашки итогов «Отработано», «Заработок», «Расстояние», «Грузооборот»,
  «Время в пути», «Ночные», «Смены»; итог «Всё время» с подписью «{МЕТРИКА} ВСЕГО
  · {ЕДИНИЦА}» и «За {N} {год/года/лет} · {M} смен»; раздел «Год за годом» —
  по каждому году значение и полоса от максимума; у первого года пометка «с
  {месяца}» (если начался не с января), у года `selectMonthOfYear` — «по {месяц}»
  (если не декабрь). Часы в истории — целые (`roundToInt`) + «ч»; грузооборот —
  1 знак.
- Детализация (тап по плашке): 12 столбцов. Год — янв…дек выбранного года;
  месяц — окно, где выбранный месяц по возможности посередине: правый край =
  `max(выбранный, min(последний месяц с данными, выбранный + 5))`. Hero: значение,
  дельта к предыдущему столбцу (только если у обоих есть данные), «{Мес}
  {значение}» предыдущего. Для «Расстояние», «Грузооборот», «Время в пути» —
  «Детали · по направлениям»: donut до 4 направлений + «Прочие». Для
  «Переработки» пустой месяц — «—». Топбар детализации: короткое имя метрики,
  «Назад»/системный Back закрывает детализацию.
- ⚠️ Поведение Android (возможный баг, BUG-44): «Переработка» в
  детализации считается по стандартному профилю недели (`getPersonalNormaHours()` без
  профиля), а в плашке — по выбранному профилю.
- Кружок «?» есть только у «Грузооборота» → шторка «Грузооборот»: «Тонно-км брутто
  — объём выполненной перевозочной работы.» / «Считается как вес поезда брутто (в
  тоннах) × пройденное расстояние (в км), просуммированный по всем поездам за
  период.» / «Единица — млн ткм (миллионы тонно-километров).»
- Кнопка PDF в топбаре (кроме детализации): во время генерации — индикатор,
  повторный тап заблокирован; ошибка — Toast «Ошибка формирования PDF: {текст}»;
  готовый файл — `PdfActionSheet` (§21); содержимое — §13.5.

### 13.5. PDF статистики (`StatisticsPdfGenerator`)

Кнопка PDF в топбаре формирует **одностраничный** A4 (595×842 pt) по текущему состоянию
экрана (вкладка, период, сравнение) и открывает общий `PdfActionSheet` (§21). Файл —
`Статистика_{период}.pdf` в кэше приложения (пробелы в названии периода → `_`; для
«Истории» — `Статистика_все_время.pdf`).
- Шапка: «Приложение «Машинист»» и справа «сформирован dd.MM.yyyy» (дата устройства);
  крупно «Статистика · {период}» (на «Истории» — «Статистика · всё время»); при включённом
  сравнении (не на «Истории») — «в сравнении с {название базы}»; горизонтальная линия.
- «Месяц»/«Год»: блок «ОСНОВНЫЕ ПОКАЗАТЕЛИ» — сетка по 3 ячейки в ряд из тех же плашек,
  что на экране (подпись, значение, под ним дельта `±N%` и единица). На «Год» дальше —
  «ОТРАБОТАНО ПО МЕСЯЦАМ · Ч» (столбцы по месяцам, над столбцом — целые часы, если > 0).
  Если есть направления — «ТОП НАПРАВЛЕНИЙ»: таблица «Плечо обслуживания | Поездок |
  Часов», строки «Первая → Последняя».
- «История»: подпись итога и крупное значение «Всё время», затем «ГОД ЗА ГОДОМ» — по
  каждому году значение (с пометками «с …»/«по …», §13.4) и полоса от максимума.
- Подвал: «Маршрутный лист · экспорт статистики» и справа «Данные приложения · не
  является официальным документом».
- Страница одна: содержимое, не поместившееся на неё, не переносится.

Источник: `features/route/src/main/java/com/z_company/route/viewmodel/StatisticsViewModel.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/StatisticsUiState.kt`,
`features/route/src/main/java/com/z_company/route/ui/StatisticsScreen.kt`,
`features/route/src/main/java/com/z_company/route/navigation/StatisticsDestination.kt`,
`features/route/src/main/java/com/z_company/route/util/StatisticsPdfGenerator.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/route/UtilsForEntities.kt`.

---

## 14. Экран «Календарь» (CalendarScreen)

Объединяет в одном месяце поездки (маршруты) и отвлечения (личные дни).

- **ViewModel**: `CalendarViewModel`. Грузит маршруты месяца → `tripsByDay`
  (по дню явки), дни-отвлечения из `MonthOfYear.days` → `absenceByDay`, суммарное
  отработанное → `totalWorkedText`. Переключение месяца (`shiftMonth`, только если
  есть в производственном календаре, иначе snackbar «Нет данных календаря за этот
  месяц») записывает месяц в `settings.selectMonthOfYear` — он меняется во всём
  приложении.
- **Ячейка дня**: поездки (`TripInfo`) и/или отвлечение (`AbsenceInfo`); федеральные
  праздники РФ подписываются по фиксированным датам (ст.112 ТК РФ), переносы — просто
  «Праздничный день».
- **Режим планирования** (`enterRoutePlan`): выбор времени явки (чипы `TimeOption`
  из настроек + своё время `addCustomPlanTime`), отметка дней (`toggleDayPlan`). При
  входе через **«Добавить событие на {дату}» → «Маршрут»** выбранная дата
  сохраняется, но время не подставляется. После выбора времени эта дата
  отмечается автоматически; остальные дни пользователь добавляет вручную →
  перед сохранением показывается вопрос **«Указать продолжительность работы?»**.
  Вопрос оформлен фирменной нижней шторкой `AppBottomSheet`, а не системным
  `TimePicker`: продолжительность вводится с цифровой клавиатуры в одном
  центрированном поле формата **`ЧЧ:ММ`** (часы 0–99, минуты 0–59); пользователь
  вводит четыре цифры, двоеточие подставляется автоматически. Начальное значение — стандартное время
  работы из настроек; между поясняющим текстом и полями выдерживается вертикальный
  отступ 20dp. Пояснение использует `bodySmall`, а поля — высоту 60dp и mono-числа
  24sp, как поля в шторке «Ночные часы». Под полем до блока действий оставлен
  отступ 20dp. Нулевая/некорректная длительность не сохраняется.
  Пользователь
  может выбрать **«Без окончания»**. `createPlannedRoutes` создаёт
  черновики на выбранные дни с `timeStartWork` (через `toEpochMillis` в поясе
  отображения `displayTimeZone()`: МСК для RU/BY, местный для KZ) и, если
  длительность указана, с `timeEndWork = timeStartWork + duration`.
  Перед сохранением выполняется проверка дублей по `timeStartWork` с точностью до
  минуты среди **всех** маршрутов (берётся первый найденный). При совпадении —
  шторка «Маршрут с такой явкой уже сохранён.» с действиями «Заменить» (найденный
  маршрут удаляется физически `removeRoute`, затем создаётся вся пачка) и
  «Оставить оба». ⚠️ Поведение Android (возможный баг, BUG-45): «Заменить»
  удаляет маршрут без soft-delete-метки, поэтому серверная копия не удаляется (❓ не
  проверено).
  **Гейт бесплатного лимита**: перед созданием проверяется вся пачка —
  `RouteActionsHelper.canCreateRoutes(количество дней)`. Без подписки пачка
  создаётся только целиком и только если помещается в остаток лимита; иначе
  ничего не создаётся и показывается диалог «Не хватает бесплатных маршрутов»
  («Оформить подписку» → гейт покупок из 3.2, «Отмена»). Если пачка прошла, но
  подписки нет, в сообщении об успехе указывается остаток: «Создано черновиков
  маршрутов: N. Осталось бесплатных: M из 20».
- **Мастер заполнения графика**: на шаге выбора первого дня доступны стрелки
  переключения месяца; продолжение графика прошлого месяца — см. 15.1.
- **Долгие операции**: при пакетном создании маршрутов и удалении маршрута
  поверх календаря показывается блокирующий индикатор с пояснением. Мастер
  заполнения месяца показывает такой же индикатор при применении графика.
- **Отвлечения**: добавить «Выходной» на день (`addDayOff`, личный день ×2 тариф);
  удалить день (`deleteAbsenceDay`) или весь период (`deleteAbsencePeriod`).
- **Свой `SnackbarHost`**: экран сам подписан на `ISnackbarManager.events`.
  Без этого сообщения Календаря (создано черновиков, остаток лимита, ошибки)
  оставались в общем канале и всплывали только при возврате на Главный экран.
- Удаление маршрута (`deleteRoute` — soft-delete + sync). `reload` при возврате.
- **Быстрый просмотр (LongClick)**: долгое нажатие на маршрут в деталях дня
  открывает `RouteQuickViewSheet` — как на Главном и «Все маршруты» (см. §9.1).
  `CalendarViewModel` реализует те же действия: избранное (`setFavoriteRoute`),
  шаринг (`shareRoute` → `shareRouteEvent`, Intent строит `CalendarDestination`),
  синхронизация (`syncRoute`), копия (`makeCopyRoute` → проверка подписки →
  `openRouteFormEvent`), удаление, расчёт отдыха (`calculationHomeRest` /
  `calculationActualRest` → `previewRouteUiState`). Обычный tap по-прежнему
  открывает форму маршрута.

### 14.1. Загрузка месяца

- Старт: месяц = `settings.selectMonthOfYear`; при каждом возврате на экран
  (`ON_RESUME`) и после любой операции месяц перечитывается. Pull-to-refresh
  запускает синхронизацию и перечитывание.
- Месяц берётся из реактивного календаря (производственный календарь + отвлечения
  из таблицы `ReleaseDay`).
- Маршруты: `listRoutesByMonth` с контекстом, в котором `crossMonthTZ` заменён
  на пояс отображения (`displayTimeZone()`), — дата явки в календаре совпадает с
  введённой. В ячейки попадают только маршруты, чья явка (в поясе отображения)
  приходится на показанный месяц; день — число явки.
- «Отработано» в шапке = `calculateWorkTimeWithSettings` за показанный месяц
  (с учётом «Учитывать будущие маршруты»), формат `ЧЧ:ММ`.
- Отвлечения группируются в периоды: подряд идущие дни одного типа. Часы дня:
  `DayOff` и `TechnicalStudy` — 0 (для техзанятий показываются введённые часы),
  `ChildCare` — рабочий 8 / сокращённый 7 / нерабочий 8 / праздник 8, остальные —
  `profile.effectiveHours(дата, тег)`.
- Праздник — день с тегом `HOLIDAY`. Название: региональный праздник из локальной
  БД (если выбран регион), иначе для RU — фиксированные даты ст. 112 ТК РФ
  («Новогодние каникулы» 1–8 января, «Рождество Христово» 7 января, «День
  защитника Отечества» 23.02, «Международный женский день» 08.03, «Праздник Весны
  и Труда» 01.05, «День Победы» 09.05, «День России» 12.06, «День народного
  единства» 04.11), иначе «Праздничный день».
- Для быстрого просмотра заранее считаются признаки поездов (тяжеловесный,
  длинносоставный, удлинённое плечо) и «Расчёт за смену» каждого маршрута
  (`computeRouteTotalPayment`, 11.9; кандидаты для переотдыха — маршруты месяца +
  соседи).

### 14.2. Состав экрана

1. Тонкая полоса загрузки 3 dp (при смене месяца), полноэкранный индикатор —
   только при первой загрузке.
2. Шапка: «{МЕСЯЦ} {ГОД}» + крупно «Отработано» (`ЧЧ:ММ`); справа стрелки ‹ ›
   (в режиме планирования вместо них кнопка «Отмена» — выход из планирования).
3. Сегмент-фильтр «Всё» / «Поездки» / «Отвлечения» (только в развёрнутом месяце
   вне планирования).
4. Строка дней недели «ПН…ВС» (СБ, ВС — акцентным цветом).
5. Сетка месяца (неделя начинается с понедельника) или одна неделя выбранного
   дня в свёрнутом виде. Ячейка: число (сегодня — в акцентном кружке; праздник —
   красным; выходные дни недели — приглушённо); до двух времён явки и «+N»;
   бейдж отвлечения («ОТП», «Б/Л», «КУР», «ДОН», «УХ», «ВЫХ», «КОМ», «ТЕХ»,
   «ОТВ») в цвете типа. Выбранный день — рамка 2 dp. Тап — выбрать день.
6. Кнопка «⌃ Свернуть в неделю» / «⌄ Развернуть в месяц».
7. Кнопка «+ Добавить событие на {день} {месяца}» → `AddEventSheet` (14.3).
8. Кнопка «Заполнить месяц по графику» → мастер (§15).
9. Детали выбранного дня: «{день} {месяца}» + день недели; плашка праздника с
   названием; карточка отвлечения (свайп → удаление, 14.5; у техзанятий тап →
   редактирование часов); маршруты дня — элементы как на Главном (тап — форма,
   long-press — `RouteQuickViewSheet`, свайп — удаление); пусто — «Нет событий в
   этот день.»

Карточка отвлечения: название типа; подпись «{a}–{b} {месяца} · N дней» для
периода, иначе «Оплата по среднему часу» (техзанятия) или «Весь день»; справа
для `DayOff` — «×2» / «тариф», для техзанятий — введённые часы («3» или «3,5») и
«часов»/«всего», для остальных — «{часы}:00» и «часов»/«всего».

Блокирующие операции (создание пачки, удаление маршрута) показывают диалог
«Подождите» с индикатором и текстом «Создаём маршруты…\nЭто может занять несколько
секунд» / «Удаляем маршрут…».

### 14.3. `AddEventSheet` («Что добавить?»)

Нижняя шторка: заголовок «Что добавить?», подзаголовок «На {день} {месяца}»,
четыре строки с цветной полоской и «›»:

| Строка | Действие (шторка сначала закрывается) |
|---|---|
| «Маршрут» | Режим планирования (14.4) с выбранным днём как начальным. Гейт подписки — только при создании. |
| «Отвлечение» | `router.showAbsence()` → `AbsenceScreen` (§16.4; в iOS — так же). Выбранная дата **не** передаётся. |
| «Выходной» | Сразу, без подтверждения, `addDayOff(день)`: запись `ReleaseDay(DayOff)`; при успехе — выгрузка настроек; обновление `selectMonthOfYear`. ⚠️ Поведение Android (возможный баг, BUG-46): существующие отвлечения этого дня не удаляются (❓ возможен дубль записи на дату, §16.4). |
| «Технические занятия» | `TechnicalStudyDialog` (14.6) с начальным значением 2 ч. |

### 14.4. Режим планирования маршрутов

- Подсказка-шаг: «ШАГ 1 · ВРЕМЯ ЯВКИ» / «Выберите время явки из списка ниже»;
  после выбора времени — «ШАГ 2 · ДАТЫ» / «Отметьте дни в календаре»; после
  отметки дня — «✓» и «Готово — можно создавать маршруты».
- Чипы «ВРЕМЯ ЯВКИ» из `standardTimesStartWork` (формат `ЧЧ:ММ`) и «+ Своё время»
  → `TimePickerDialog` («Своё время явки», 24-часовой, начальное 08:00, «Готово» /
  «Отмена»); своё время добавляется в чипы (сортировка) и сохраняется в
  `standardTimesStartWork`.
- Выбор времени (чип или своё) при пустом наборе отмечает начальный день. Тап по
  дню: без выбранного времени — ничего; день с отвлечением (кроме «Выходной»)
  недоступен (приглушён 45%); иначе переключает отметку, фиксируя за днём
  **текущее** активное время. Отмеченный день — заливка акцентом и время явки.
- Нижняя панель: «N маршрут(а/ов) к созданию» или «Выберите время и отметьте
  дни»; кнопка «Создать маршруты» активна при N > 0 → шторка продолжительности
  (описана выше в §14). В шторке подзаголовок «Окончание рассчитается от времени
  явки», поле `ЧЧ:ММ` (4 цифры, двоеточие подставляется, ошибка при минутах > 59),
  действия «Указать» (активно при 4 цифрах, минутах 0–59 и длительности > 0) и
  «Без окончания».
- Создание: гейт лимита на всю пачку → проверка дублей → последовательное
  сохранение → snackbar («Создано черновиков маршрутов: N» / «…. Осталось
  бесплатных: M из 20»), выход из планирования, перезагрузка. Ошибки: «Не удалось
  проверить подписку», «Ошибка создания маршрутов».
- Диалог лимита (`SubscriptionLimitDialog`): «Не хватает бесплатных маршрутов»;
  текст при остатке 0 — «Бесплатный лимит исчерпан: использовано 20 из 20. Сейчас
  создаётся N маршрутов. Оформите подписку — лимит снимется и включится
  синхронизация.», иначе «В бесплатном периоде доступно 20 маршрутов, у вас
  осталось M. Сейчас создаётся N маршрутов. Оформите подписку — …»; «Оформить
  подписку» (гейт покупок §3.2) / «Отмена».

### 14.5. Удаление из календаря

- Маршрут (свайп в деталях дня или из быстрого просмотра): шторка «Удалить
  маршрут?» + дата/время явки (моноширинно), «Да, удалить» → soft-delete
  (`markAsRemoved`) + перезагрузка; ошибка — «Не удалось удалить маршрут».
- Отвлечение одного дня: «Удалить «{тип}»?», «Да, удалить». Период из нескольких
  дней: подзаголовок «Занимает {a}–{b} {месяца} · N дней», действия «Только этот
  день» и «Весь период · N дней». Удаляются записи `ReleaseDay` только указанных
  дат (остальные дни месяцев сохраняются), затем выгрузка настроек и обновление
  `selectMonthOfYear`; ошибка — «Не удалось удалить отвлечение».
- Копия из быстрого просмотра: шторка «Создать копию маршрута?» → «Создать
  копию»; без подписки — «Создание маршрутов доступно по подписке».

### 14.6. `TechnicalStudyDialog` («Технические занятия»)

- `AlertDialog`: заголовок «Технические занятия», «На {день} {месяца}», системный
  24-часовой `TimePicker` (часы 0–23, минуты 0–59). Начальное значение: 2:00 при
  добавлении, сохранённые часы при редактировании (дробная часть × 60 с
  округлением).
- Часы = `час + минута/60`. Если средний час > 0 — плашка «К начислению» с
  суммой `средний час × часы` (формат «2 800» или «2 800,50», копейки
  отбрасываются вниз); иначе предупреждение «Средний час не задан в настройках
  зарплаты — сумма не рассчитается. Часы сохранятся.»
- «Сохранить» активна при часах > 0 → `addTechnicalStudy(день, часы)`: удаляет
  любое отвлечение этого дня, сохраняет `ReleaseDay(TechnicalStudy, hours)`,
  выгружает настройки, обновляет `selectMonthOfYear`; ошибка — «Не удалось
  добавить технические занятия». «Отмена» — без изменений.
- Максимум за день — 23 ч 59 мин (ограничение пикера).

Источник: `features/route/src/main/java/com/z_company/route/ui/CalendarScreen.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/CalendarViewModel.kt`,
`features/route/src/main/java/com/z_company/route/navigation/CalendarDestination.kt`,
`features/route/src/main/java/com/z_company/route/ui/SubscriptionLimitDialog.kt`,
`features/route/src/main/java/com/z_company/route/component/AppBottomSheet.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/RouteSalaryCalculator.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/use_cases/ReleaseDayUseCase.kt`,
`data_local/src/commonMain/kotlin/com/z_company/data_local/calendar/SqlDelightCalendarRepository.kt`,
`core_android/src/main/java/com/z_company/core/util/DateAndTimeConverter.kt`.

---

## 15. Мастер «Заполнить месяц» (ScheduleWizardScreen)

- **ViewModel**: `ScheduleWizardViewModel`. По паттерну графика раскладывает смены
  на месяц и создаёт черновики маршрутов (`Route` c `timeStartWork/timeEndWork`) на
  каждый рабочий день. Соответствует дизайну `schedule-wizard.jsx`.
- **Паттерны** (`SchedulePattern`, в UI — «график») хранятся ЛОКАЛЬНО
  (SharedPreferences): стандартные (2/2, 4/4, 2/1) можно удалить; «свой»
  цикл-конструктор (`CUSTOM_PATTERN_ID`) после применения сохраняется новым.
  Тип дня — `ShiftKind` (DAY/NIGHT/OFF). Долгое нажатие по плитке → удаление
  через шторку `AppBottomSheet` («Удалить график «…»?» + действие «Да, удалить»,
  паттерн главного экрана), не через диалог.
- **UI-термины**: шаг называется «График» (индикатор под цифрой 1), список
  вариантов — подзаголовок «ВАРИАНТЫ ГРАФИКА».
- **Шаг 1**: выбор/редактирование графика (тап по дню цикла → пикер типа;
  добавить/убрать день цикла), время дневной и ночной смены (своё у каждой).
  **Шаг 2**: первый день цикла в месяце (со стрелками смены месяца),
  флажок «Продлить на следующий месяц» и предпросмотр раскладки на месяц. При включённом флажке `apply()` создаёт
  маршруты и на следующий календарный месяц, продолжая фазу того же цикла с
  первого числа следующего месяца; проверка бесплатного лимита выполняется по
  общей пачке двух месяцев.
- `apply()` сначала раскладывает месяц в список маршрутов, затем проверяет
  бесплатный лимит на **всю пачку** (`RouteActionsHelper.canCreateRoutes`) и только
  потом сохраняет. Не хватает лимита — не создаётся ничего, показывается диалог
  «Не хватает бесплатных маршрутов» (`WizardUiState.subscriptionLimit`; «Оформить
  подписку» → гейт покупок из 3.2). Длительность смены — в минутах, через
  полночь → +24 ч.
  ⚠️ До этого проверялась только возможность создать *один* маршрут
  (`newRouteClick`), поэтому без подписки мастер заполнял месяц целиком и лимит
  обходился.
- У мастера свой `SnackbarHost` (как у Календаря) — сообщения показываются на
  самом экране, а не после возврата на Главный.
- Диалоги прогресса создания и выбора времени используют нейтральный фон
  карточек `secondary` и акцентные цвета приложения; оранжевая системная заливка Material
  в мастере не используется.

### 15.1. Продолжение графика прошлого месяца

- Предложение действует, **только если мастером заполнен ровно предыдущий
  месяц** относительно выбранного, и только если тот паттерн ещё существует
  в списке (иначе фазу цикла продолжать не от чего). Проверка — по записи
  `last_schedule_month` в SharedPreferences.
- **Формат записи**:
  `year-month|patternId|firstDay|nextPhase|дн.начало|дн.конец|ноч.начало|ноч.конец`.
  `nextPhase` — индекс цикла, с которого должно начаться **1-е число следующего
  месяца**. Хранится именно конечная фаза, а не «сколько дней израсходовано»:
  так продолжение склеивается в цепочку (месяц, сам заполненный с фазы, отдаёт
  дальше правильную фазу). Записи старого формата (3 поля) читаются: фаза
  восстанавливается из длины сохранённого месяца, время смен берётся текущее.
  Для «своего» цикла записывается id реально сохранённого паттерна, а не
  служебный `CUSTOM_PATTERN_ID`.
- **Продолжение** подставляет паттерн и время смен прошлого месяца, ставит
  первый день = 1 и сдвиг `phaseOffset = nextPhase`, после чего переводит на
  шаг 2. Цикл перетекает в новый месяц без разрыва, а не начинается заново.
- `buildPreview` учитывает `phaseOffset` — предпросмотр совпадает с тем, что
  создаст `apply()`. ⚠️ Раньше предпросмотр рисовал цикл с 1-го числа заново,
  а `apply()` раскладывал со сдвигом: показывалось не то, что создавалось.
- Ручной выбор паттерна, первого дня или правка «своего» цикла выходят из
  режима продолжения (`continuePrevious = false`, `phaseOffset = 0`) — фаза
  считалась для прежнего цикла и к другому неприменима. Смена месяца стрелками
  тоже сбрасывает режим и пересчитывает доступность продолжения.
- **Два флага состояния**: `canContinuePrevious` — продолжение доступно
  (кнопка «Продолжить график прошлого месяца» на шаге 1, остаётся после отказа
  от шторки); `showContinuePreviousSheet` — показывать шторку с предложением.
  «Выбрать заново» и любое закрытие шторки гасят только второй флаг.
  Сбрасывать его обязательно — иначе шторка остаётся в композиции и её scrim
  блокирует нажатия по всему экрану мастера.

### 15.2. Состав экрана и раскладка

- Месяц мастера = `settings.selectMonthOfYear` (стрелки на шаге 2 листают месяцы
  календаря, **не** меняя настройки). Шапка: кнопка «‹ {Месяц}» (выход без
  изменений), заголовок «Заполнить месяц», индикатор шагов «1 График» / «2 Старт и
  предпросмотр» (пройденный шаг — «✓»). Внизу: на шаге 2 «‹ Назад» (на шаг 1 с
  сохранением ввода) и основная кнопка «Далее →» / «Применить» (во время
  сохранения — индикатор, повторный тап заблокирован).
- **Шаг 1**: при `canContinuePrevious` сверху кнопка «Продолжить график прошлого
  месяца» / «Цикл продолжится с той же фазы, с 1 числа»; при открытии экрана
  (если доступно) — шторка «Продолжить график прошлого месяца?» с действиями
  «Продолжить» и «Выбрать заново». Далее «ВАРИАНТЫ ГРАФИКА»: плитки паттернов по
  две в ряд + плитка «Свой» / «Настроить вручную» (не удаляется); подпись
  «Удерживайте график, чтобы удалить его.» При выбранном «Свой» — «ЦИКЛ СМЕН»:
  дни цикла чипами «Дн»/«Ноч»/«Вых», подпись «Цикл: N дневных, M ночных, K
  выходных. Нажмите день, чтобы изменить тип или удалить.»; тап по дню —
  пикер «День N — выберите тип» (День / Ночь / Выходной, «✕» закрыть, «Удалить
  день N из цикла» — скрыто, если в цикле один день); кнопка добавления дня
  добавляет день типа «День» и сразу открывает его пикер. Начальный свой цикл: День, Ночь, Вых, Вых.
  «ВРЕМЯ СМЕНЫ»: карточки «Дневная» (заголовок только если есть и ночные) и
  «Ночная» — по составу выбранного цикла, поля «Начало»/«Конец» открывают
  `TimeInputDialog` (24-часовой пикер, «ОК»/«Отмена»); по умолчанию день
  08:00–20:00, ночь 20:00–08:00; подпись «Смены разложатся по всем рабочим дням
  паттерна.»
- Стандартные паттерны при первом запуске: «2/2» (день · ночь · вых · вых),
  «4/4» (2 дня · 2 ночи · 4 вых), «2/1» (день · ночь · вых). Выбран первый
  паттерн списка (или «Свой», если список пуст). Дефолты создаются только если
  список в хранилище отсутствует; удалённые пользователем стандартные паттерны
  (даже все) не восстанавливаются.
- **Шаг 2**: «ПЕРВЫЙ ДЕНЬ ЦИКЛА» + стрелки ‹ › месяца, подпись «Число месяца, с
  которого начинается ваш цикл смен.», сетка чисел 7 в ряд; флажок «Продлить на
  следующий месяц»; «ПРЕДПРОСМОТР · {месяц}» и «N смен» (рабочих дней).
- **Раскладка** (`apply`): для дня `d ≥ firstDay` тип =
  `cycle[(phaseOffset + d − firstDay) mod len]`; дни до `firstDay` — выходные.
  Для DAY/NIGHT создаётся `Route` с `timeStartWork = toEpochMillis(год, месяц,
  d, чч, мм)` в поясе отображения (`displayTimeZone()`) и `timeEndWork = начало +
  длительность`, где длительность = `конец − начало` в минутах, при `≤ 0` —
  `+24 ч` (равные начало и конец дают 24 ч). При продлении следующий месяц
  раскладывается с 1-го числа и фазой `(phaseOffset + дней_с_firstDay) mod len`.
  Цикл без рабочих дней → snackbar «В паттерне нет рабочих дней».
- Мастер **не** проверяет дубли с уже созданными маршрутами и **не** пропускает
  дни отвлечений (отпуск и т.п.) — маршруты создаются на все рабочие дни цикла.
- После сохранения: «Свой» цикл сохраняется паттерном «{рабочих}/{выходных}» с
  подписью из слов «день/ночь/вых» (если такой цикл уже есть — берётся его id);
  пишется `last_schedule_month` (для продления — следующий месяц и фаза после
  него); snackbar «Не создано ни одного маршрута» / «Создано черновиков маршрутов:
  N» / «…. Осталось бесплатных: M из 20»; экран закрывается. Ошибка — «Ошибка:
  {текст}». Диалог прогресса: «Создаём маршруты» / «Это может занять несколько
  секунд».
- Удаление паттерна: long-press по плитке (кроме «Свой») → шторка «Удалить график
  «{название}»?» → «Да, удалить»; если удалён выбранный — выбирается первый
  оставшийся или «Свой». Уже созданные маршруты не затрагиваются.

> Старый экран «График» (`WorkScheduleScreen` + `WorkScheduleViewModel`) удалён как
> мёртвый код — его заменили Календарь (14) и этот Мастер.

Источник: `features/route/src/main/java/com/z_company/route/ui/ScheduleWizardScreen.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/ScheduleWizardViewModel.kt`,
`features/route/src/main/java/com/z_company/route/navigation/ScheduleWizardDestination.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/SchedulePattern.kt`,
`features/route/src/main/java/com/z_company/route/ui/SubscriptionLimitDialog.kt`.

---

## 16. Отвлечения (AbsenceScreen / SelectReleaseDaysScreen)

Личные нерабочие дни (отпуск, больничный, курсы, уход и т.п.) — «release-дни»,
влияют на норму часов.

### 16.1. Новое отвлечение (AbsenceScreen)

- **ViewModel**: `AbsenceViewModel`. Выбор диапазона дат (тапы — §16.4: 1-й — однодневный диапазон,
  2-й — диапазон между датами, 3-й — новый однодневный диапазон) + тип (`ReleaseType`, по
  умолчанию «Отпуск»; без «Выходного» и «Технических занятий» — они добавляются из
  Календаря, §14.3). Норма-часы за диапазон (`rangeHours`): только рабочие дни
  (кроме «по уходу» — все дни). `save()` пишет в `ReleaseDayRepository`.
- **Месяц шторки и диапазон дат — независимы**: `rangeStart`/`rangeEnd` — это
  полные даты (`kotlinx.datetime.LocalDate`), а не день месяца. Стрелки ‹ › рядом
  с названием месяца (`shiftMonth(±1)`) листают показанный в сетке месяц, не
  сбрасывая уже выбранную дату начала — так можно тапнуть начало диапазона в
  одном месяце, перелистнуть на соседний и тапнуть конец там же. Листание
  ограничено месяцами, уже загруженными в `monthsCache` (все месяцы из
  `CalendarUseCase.loadFlowMonthOfYearListState()`); при попытке уйти за
  пределы — snackbar «Нет данных календаря за этот месяц» (как в
  `CalendarViewModel.shiftMonth`). Часы диапазона (`computeHours`) и `save()`
  корректно суммируют/сохраняют дни по обе стороны границы месяца — для каждого
  затронутого месяца отдельно вызывается `settingsUseCase.setCurrentMonthOfYear`.

### 16.1.1. Какие типы уменьшают норму месяца

Единое правило для всех расчётов (`NormaUseCase.calculate`,
`UtilForMonthOfYear.reducesNorma`/`getPersonalNormaHours`, виджет, PDF):

- Норму уменьшают отпуск, больничный, курсы, донорские, уход за
  ребёнком-инвалидом и прочее — рабочий день 8 ч,
  сокращённый 7 ч, выходной/праздник 0 ч.
- **`ReleaseType.BusinessTrip` («Командировка») норму НЕ уменьшает.** В карточке
  выбранного диапазона поле «Часов по рабочим дням» показывает сумму
  по производственному календарю (8/7/0), как для отпуска. Эти часы справочные:
  оплата маршрутов командировки по-прежнему считается отдельно по среднему часу.
  Фактические часы маршрутов командировки при этом входят в общее
  отработанное время, закрывают норму и участвуют в расчёте переработки.

### 16.1.2. Командировка и сверхурочные

- Оплата командировки = фактические часы маршрутов внутри календарных суток,
  отмеченных как командировка, ×
  `SalarySetting.averagePaymentHour`.
- Переходящий маршрут разрезается в локальную полночь: часть вне командировки
  остаётся в обычном тарифном расчёте и надбавках, командировочная часть из них
  исключается. Перерыв вычитается только из той части, с которой пересекается.
- Переработка = `(все обычные + все командировочные часы − праздничные часы) − норма`,
  не менее нуля. Например: `200 ч командировки + 50 ч обычной работы − 180 ч нормы = 70 ч`.
  Обычные и командировочные фрагменты суммируются явно, поэтому полностью
  командировочный маршрут не теряется из переработки даже на нерабочем дне
  календаря. Сама отметка командировки норму не уменьшает.
- Все часы командировки также остаются в отдельной строке оплаты по среднему;
  сверхурочная оплата начисляется дополнительно по общим правилам.
- Это действует и когда весь маршрут командировочный, и для переходящего
  смешанного маршрута: в переработку входит сумма обычного и командировочного
  фрагментов, а обычные тарифные строки не возвращают командировочный фрагмент.
- Ночная и праздничная части внутри командировочного фрагмента не создают
  повторных обычных строк: сам фрагмент оплачивается по среднему часу.
- База сверхурочных включает тариф, компенсационные и стимулирующие выплаты.
- Для месяцев до сентября 2026 действует прежнее распределение сверхурочных. С 01.09.2026
  применяется ФЗ №144-ФЗ: первые 2 ч на смену оплачиваются не ниже ×1,5, остальные — не ниже ×2;
  начиная со 121-го сверхурочного часа за календарный год — не ниже ×2.
  Приложение для каждого предыдущего месяца с января заново считает `фактические часы − норма`
  (с учётом отвлечений, праздничных и командировочных часов), суммирует годовой итог и передаёт
  его в расчёт выбранного месяца. Если до нача месяца уже накоплено 120 ч, вся его переработка идёт в ×2.
- **`ReleaseType.DayOff` («Выходной») норму НЕ уменьшает** — это перенос
  выходного дня: работник отдыхает в этот день и отрабатывает другой.
  Единственный эффект — ×2 при работе в этот день.
- **`ReleaseType.TechnicalStudy` («Технические занятия») норму НЕ уменьшает.**
  У этого типа собственные часы (`ReleaseDay.hours` / `Day.hours`, задаёт
  пользователь в диалоге `TechnicalStudyDialog`, §14.6), которые оплачиваются отдельной
  строкой «Технические занятия» по среднему часу
  (`getTechnicalStudyHours` → `getMoneyTechnicalStudyFlow`). В общий пул
  «оплата по среднему» (`getDayoffHoursIncludingWeekends`) и в счётчик
  отвлечений (`getDayoffHours`, «Отвлечения» в PDF) эти часы не входят.
- В «недоработке» (`getUnderworkTimeFlow`) часы техзанятий вычитаются из
  нормы наравне с отработанным временем: норма не уменьшена, но эти часы уже
  оплачены по среднему — иначе те же часы оплатились бы дважды.

### 16.2. Список отвлечений / норма месяца (SelectReleaseDaysScreen) — 🚫 мёртвый код Android

- **ViewModel**: `SelectReleaseDaysViewModel`. Периоды отвлечений (`ReleasePeriod`)
  строятся из `MonthOfYear.days` (обогащённых release-флагами). Добавить/удалить
  период (`addReleasePeriod`/`deleteReleasePeriod`). Смена месяца (`setCurrentMonth`).
  Отдельно — сохранение тарифной ставки и полей `MonthOfYear` (`saveNormaHours`).
- 🚫 Мёртвый код Android — в iOS не реализуется. Экран зарегистрирован в навигации
  (`SelectReleaseDaysScreenRoute`), но ни один UI не вызывает
  `router.showSelectReleaseDayScreen()`, а `showAbsence()` ведёт на `AbsenceScreen` —
  пользователь на него попасть не может. Отвлечения добавляются через Календарь (§14.3) и
  `AbsenceScreen` (§16.4); «Изменить норму» в Настройках (§17.2) тоже ведёт на
  `AbsenceScreen`.
- **Навигация iOS:** `showAbsence()` → `AbsenceScreen` (§16.4). Старый CMP-роутер
  (`iosApp/src/commonMain/.../IosRouterImpl.kt`) вёл `showAbsence()` на
  `SelectReleaseDaysScreen` — это не переносить.
- **Хранение (общее для всех экранов отвлечений):** отвлечения хранятся в таблице
  `ReleaseDay` (одна запись на дату, месяц 0-based); при чтении календаря `MonthOfYear.days`
  получают `isReleaseDay`,
  `releaseType` и `hours` **только** из этой таблицы (старые флаги в `days`
  перезаписываются). Удаление периода (`deletePeriod`) удаляет все записи
  затронутых месяцев и возвращает записи дат, не входящих в период.

### 16.3. Индивидуальная продолжительность рабочей недели

- В `Настройки → Норма и регион → Рабочая неделя` пользователь выбирает профиль:
  стандартный (Пн–Пт по 8 ч), шестидневный (Пн–Пт по 7 ч, Сб 5 ч) или свой.
- В своём профиле часы задаются отдельно для каждого дня недели в диапазоне
  `0..24`. Настройка хранится локально в `SharedPreferencesRepositories` как
  `WorkScheduleProfile` и не входит в существующие контракты `UserSettings` и
  `SyncData`.
- Повторный тап по выбранной строке «Своя настройка» сворачивает или раскрывает
  подробные поля дней недели. При первом выборе своего профиля подробности
  раскрываются автоматически; сворачивание не меняет сохранённые часы.
- При активной подписке профиль синхронизируется отдельным ресурсом
  `GET/PUT /v1/work_schedule/`. Сервер хранит одну запись на пользователя;
  `user_id` берётся только из JWT. Конфликт устройств разрешается по
  `updatedAt` (последняя запись побеждает), причём сервер не принимает более
  старое значение. Отсутствующая серверная запись читается как стандартный
  профиль, поэтому старые версии приложения продолжают работать без изменений.
- Профиль реактивно пересчитывает месячную норму через `NormaUseCase`, часы
  диапазона в `AbsenceScreen`, карточки отвлечений календаря, расчёт оплаты
  отвлечений и раздел графика в PDF.
- `HOLIDAY` всегда даёт 0 ч. Для `SHORTENED_DAY` используется значение дня
  недели минус 1 ч (не ниже 0). Для `NON_WORKING_DAY` применяется профиль дня
  недели: это позволяет сделать обычную субботу рабочей при шестидневке.
- Специальная семантика сохраняется: `DayOff` и `TechnicalStudy` не получают
  норма-часы; `ChildCare` продолжает считаться по отдельному действующему правилу.

### 16.4. «Новое отвлечение» (AbsenceScreen): состав и сохранение

- Открывается из Календаря («Что добавить?» → «Отвлечение», §14.3). Выбранный в
  календаре день **не** передаётся: начальный показанный месяц —
  `settings.selectMonthOfYear`, диапазон пуст.
- Шапка: «‹ Назад» (выход без сохранения), заголовок «Новое отвлечение»,
  подзаголовок «Выберите даты» / «Выбрана 1 дата» / «Выбран диапазон дат». Ниже
  «{Месяц} {год}» со стрелками ‹ ›, дни недели «ПН…ВС», сетка дней (с
  понедельника), подсказка «Нажмите дату для одного дня или две даты — начало и
  конец диапазона.»
- Тапы: 1-й — однодневный диапазон; 2-й — диапазон между двумя датами
  (упорядочивается); 3-й — новый однодневный диапазон с этой даты. Границы
  включительно, могут лежать в разных месяцах. Концы диапазона залиты цветом
  типа, середина — 30% цвета. Дни с уже сохранёнными отвлечениями (вне
  выбранного диапазона) показывают бейдж типа («ОТП», «Б/Л», …) — выбирать их
  не запрещено.
- «Тип отвлечения» — раскрывающийся список (галочка у выбранного): Отпуск,
  Больничный, Курсы, Донорские, По уходу за ребенком-инвалидом, Командировка,
  Прочее. По умолчанию «Отпуск». «Выходной» и «Технические занятия» здесь не
  выбираются (они добавляются из Календаря).
- Карточка-сводка: цветная метка и тип; диапазон «a–b {месяца}» (один месяц),
  «a {месяца} – b {месяца}[ год]» (разные месяцы/годы) или «a {месяца}»; «Не
  выбрано» без выбора; «ДНЕЙ» N; «ЧАСОВ» «{часы}:00» / «по рабочим дням». Часы —
  Σ по дням диапазона: `ChildCare` — 8/7/8/8 (рабочий/сокращённый/нерабочий/
  праздник), иначе `profile.effectiveHours(дата, тег)`; тег берётся из известных
  месяцев календаря, неизвестный месяц — как рабочий день.
- ⚠️ Поведение Android (возможный баг, BUG-47): слово после числа дней
  выбирается как «день» (1), «дня» (< 5), иначе «дней» — для 21, 22… получается «21 дней».
- Кнопка «Сохранить» внизу активна при выбранном диапазоне и не во время
  сохранения. Сохранение: по записи `ReleaseDay` (новый id) на каждую дату
  диапазона с выбранным типом → выгрузка настроек (`settingsSyncPending`,
  `autoPushSettings`) → для каждого затронутого месяца
  `settingsUseCase.setCurrentMonthOfYear(месяц)` → закрытие экрана. Ошибка —
  snackbar «Не удалось сохранить отвлечение», выбор сохраняется.
- ⚠️ Поведение Android (возможный баг, BUG-46): существующие записи на эти
  даты не удаляются — на дату может оказаться две записи разных типов; в месяце
  показывается одна из них (какая — зависит от порядка чтения, ❓ не проверено).
- ⚠️ Поведение Android (возможный баг, BUG-48): после сохранения выбранным
  месяцем приложения становится **последний** затронутый месяц.

Источник: `features/route/src/main/java/com/z_company/route/ui/AbsenceScreen.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/AbsenceViewModel.kt`,
`features/route/src/main/java/com/z_company/route/navigation/AbsenceDestination.kt`,
`features/route/src/main/java/com/z_company/route/ui/SelectReleaseDaysScreen.kt`,
`features/route/src/main/java/com/z_company/route/viewmodel/SelectReleaseDaysViewModel.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/use_cases/ReleaseDayUseCase.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/use_cases/NormaUseCase.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/UtilForMonthOfYear.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/MonthOfYear.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/ReleaseDay.kt`,
`domain/src/commonMain/kotlin/com/z_company/domain/entities/WorkScheduleProfile.kt`,
`data_local/src/commonMain/kotlin/com/z_company/data_local/calendar/SqlDelightCalendarRepository.kt`.

---

## 17. Экран «Настройки» (SettingsScreen)

Общие настройки приложения. Корневая вкладка нижнего меню «Настройки» (hub);
может открываться сразу в нужном под-разделе (`showSettings*` → аргумент
ROUTE/ROUTE_FORM/NORMA/ACCOUNTING/REST/SHOULDERS/LOCOMOTIVE/TRAIN/SERIES_LIST/
STATION_LIST/SERIES_EDITOR_{id}/STATION_EDITOR_{id}; неизвестный аргумент → hub).
Под-экраны, их тексты и модальные редакторы — §17.1–17.5 ниже; здесь — состав hub,
сохранение и доменные правила.

**Hub (сверху вниз).** Заголовок app bar «Настройки» (без кнопки «назад»).
Прокручиваемый столбец из групп; заголовок группы — UPPERCASE, мелкий, цвет
`onSurfaceVariant`; группа — карточка с тонкой рамкой, строки разделены линией.
Строка = квадратный icon-аватар 34dp · заголовок (+ подзаголовок) · значение справа ·
шеврон. При системном масштабе шрифта > 1.15 значение переносится под заголовок.

| Группа | Строка | Подзаголовок / значение | Переход |
|---|---|---|---|
| СПРАВОЧНИКИ | Серии локомотивов | «N серия/серии/серий» | §17.4 |
| | Станции | «N станция/станции/станций» | §17.4 |
| | Плечи | «N плечо/плечи/плеч» | §17.4 |
| | Напарники | «N напарник/напарника/напарников» | §8.5 |
| РАСЧЁТ | Зарплата | «Тарифная ставка и доплаты» | экран §12 (`showSettingSalary`) |
| | Норма и регион | название страны («Россия»/«Беларусь»/«Казахстан») | §17.2 |
| | Учёт | «Ночные часы и будущие маршруты» | §17.3 |
| | Отдых | «Нормы домашнего отдыха и в ПО» | §17.5 |
| ПОЕЗДКИ | Основные | «Значения по умолчанию для новых маршрутов» | §17.5 |
| | Маршрут | «Поля формы маршрута» | §17.5 |
| | Локомотив | «Поля ввода показаний и вид тяги» | §17.5 |
| | Поезд | «Расчёт длины состава» | §17.5 |
| О ПРИЛОЖЕНИИ | карточка «М» · «Машинист» · «версия X.Y.Z» (моно) | — | — |
| | Поддержка · `locodriver.app@yandex.ru` | — | почтовый клиент (`mailto:`) |
| | Экспорт диагностики · «Для поддержки» | — | §17.6 |

- Счётчик серий/станций = число записей справочника (`LocomotiveSeries`/
  `StationNorm`) + число «старых» имён из `UserSettings.locomotiveSeriesList`/
  `stationList`, которым не сопоставлена запись (сравнение по `trim().lowercase()`,
  пустые и дубли не считаются). Плечи — `UserSettings.servicePhases.size`,
  напарники — число записей `PartnerRepository`.
- Склонение: `n%10==1 && n%100!=11` → форма 1; `n%10 in 2..4 && n%100 !in 12..14` →
  форма 2; иначе → форма 3.
- Pull-to-refresh на всех под-экранах Настроек запускает полную ручную
  синхронизацию (§18, «Синхронизация»).

**Сохранение.** Любая правка поля `UserSettings` в VM (`currentSettings = …`)
пишет состояние и планирует автосохранение через **300 мс** (дебаунс; повторная
правка перезапускает таймер). Сохранение штампует `updateAt = max(now, prev+1)`,
пишет в локальную БД и при успехе ставит флаг `settingsSyncPending=true` и
запускает фоновую выгрузку настроек (`autoPushSettings`, только если есть токен).
При уходе с экрана (`onCleared`) незавершённое сохранение выполняется принудительно.
⚠️ Поведение Android (возможный баг, BUG-49): финальное сохранение в
`onCleared` пишет запись **без** нового `updateAt` — если пользователь ушёл быстрее 300 мс,
правка сохранится локально с прежней меткой и может проиграть LWW-сравнение с сервером.

**Настройки, живущие только на устройстве** (в `SharedPreferences`, без синхронизации):
тема (`ThemeManager`), показ полей «Отопление» / «Собственные нужды» / «Смена рода
тока» (`isShowLocoHeating` / `isShowLocoAuxiliary` / `isShowOtherCurrent` — при
загрузке всегда перекрываются значениями из `SharedPreferences`), поведение
«Свыше 12 часов — пассажиром» (`Passenger12hOption`), недавние значения пикеров
времени. Всё остальное — поля `UserSettings` (синхронизируются, §18).

- **ViewModel**: `SettingsViewModel`. Все поля в `SettingsUiState` (`UserSettings`).
- **Время**: формат (обычный/десятичный, `changeTimeFormat`), стиль пикера
  (`changeTimePickerStyle`), часовой пояс (`setTimeZone`), пояс переходных
  маршрутов (`setCrossMonthTimezone`: MOSCOW/LOCAL), ночные часы
  (`changeStart/EndNightTime`).
  - `MOSCOW` обрезает переходящий маршрут календарным месяцем по МСК независимо
    от местонахождения пользователя. Для работника во Владивостоке, ведущего
    маршрут по МСК, интервал 31 января 23:00–1 февраля 01:00 делится по полуночи
    МСК. `LOCAL` использует локальную календарную границу.
- **Работа**: время по умолчанию (`changeDefaultWorkTime` + `usingDefaultWorkTime`),
  тип локо по умолчанию (`setDefaultLocoType`), учитывать будущие маршруты,
  показывать перерыв, поведение «пассажир >12ч» (`Passenger12hOption`).
- **Тема оформления** (под-раздел «Основные», `SettingsRouteContent`): три
  взаимоисключающих варианта — «Как в системе» / «Светлая» / «Тёмная»
  (`ThemeMode.MODE_SYSTEM/MODE_LIGHT/MODE_DARK`). Выбор через `ThemeManager`
  (Koin-singleton). Хранение — **локально**, `SharedPreferences`
  (`TOKEN_THEME_MODE`, имя enum'а строкой; по умолчанию `MODE_SYSTEM`), **без
  синхронизации с сервером**. `ThemeManager.themeMode` — `StateFlow`; корневой
  `LocoDriverApp` подписан на него и пересобирает `LocoDriverTheme` при
  переключении (для `MODE_SYSTEM` — `isSystemInDarkTheme()`). Применяется
  мгновенно, ко всему приложению.
- **Видимость блоков формы** — под-экраны «Маршрут» и «Локомотив» (§17.5).
- **Отдых**: мин. время отдыха в ПО (`changeMinTimeRest`, по умолчанию 3 ч),
  «Отдых в ПО (второй отдых)» (`changeMinTimeRestSecond`, по умолчанию 4 ч),
  мин. домашний отдых. Второй минимум используется для короткого и полного
  отдыха, если непосредственно предыдущий по времени явки маршрут тоже завершился
  отдыхом в ПО; иначе используется обычный минимум. Данные хранятся в
  `UserSettings.minTimeRestSecond` (SQLDelight, миграция 22) и синхронизируются
  необязательным JSON-полем `minTimeRestPointOfTurnoverSecond`. Отсутствие поля
  в POST старого клиента не сбрасывает сохранённое значение на сервере.
  На Android `createSettingsDriver` также проверяет и добавляет этот столбец до
  открытия SQLDelight-драйвера, потому что версия существующей БД может быть
  поднята сразу до текущей `fixVersionIfColumnsExist` без запуска `.sqm`.
  PWA показывает то же поле в разделе «Отдых», хранит его в persisted Pinia
  `settings.data` и отправляет в существующем `POST /v1/user_settings/`.
  В главной карточке, форме маршрута и быстром просмотре PWA выбирает тот же
  минимум по непосредственно предыдущему маршруту.
- **Плечи обслуживания** (`ServicePhase`) — список §17.4, редактор §6.6. Ставка —
  `Double >= 0`, пустое значение означает `0.0`; при создании обратного плеча копируется
  вместе с расстоянием.
- **Хранение/синхронизация плеч:** `linearMileageRate` — добавочное поле объекта
  `ServicePhase` внутри `UserSettings.servicePhases`, default `0.0`. Сервер хранит
  список в JSON. Если старый клиент присылает существующее плечо без этого поля,
  сервер сохраняет ранее записанную ставку по `id`; новое плечо без поля получает
  `0.0`. Старые клиенты игнорируют поле в GET, поэтому контракт совместим назад.
- **Справочники** серий и станций — §17.4 (редакторы норм те же, что в шторке времени, §2.10).
- **Регион/страна и норма** — §17.2: смена страны (`changeCountry`, `CountryLoadingState`),
  смена региона (`changeRegion` — атомарно, `RegionLoadingState`); норма часов
  (`normaHours`) реактивно через `NormaUseCase`. Смена **страны** не атомарна
  (BUG-50, §17.2).
- Отдельных кнопок «выгрузить/загрузить» на экране нет: настройки уходят на
  сервер автосохранением (см. «Сохранение» выше) и общей синхронизацией (§18).
  Состояния `uploadState`/`downloadState`/`logOutState` в `SettingsUiState` —
  легаси: VM их не выставляет, связанные snackbar'ы не показываются.

Источник: `features/route/.../ui/SettingsScreen.kt`, `ui/settings/*.kt`,
`viewmodel/SettingsViewModel.kt`, `viewmodel/SettingsUiState.kt`,
`navigation/SettingDestination.kt`, `navigation/HomeDestination.kt` (`NavigationItem`),
`core/.../ThemeManager`.

### 17.1. Hub и навигация (бывш. §30.1)

- Состав hub — таблица в начале §17. «Зарплата» открывает отдельный экран §12,
  остальные строки — под-экраны внутри того же экрана настроек (не отдельные
  nav-destination на Android; смена под-экрана — горизонтальный слайд: вглубь —
  въезд справа, назад — слева).
- **Top bar под-экранов:**
  - «Основные», «Маршрут», «Норма/Регион», «Учёт», «Отдых», «Локомотив», «Поезд» —
    заголовок по центру (`currentSubScreen.title`), слева **синий текст «Готово»**
    вместо стрелки (просто возврат, т.к. всё уже сохранено автосохранением);
  - списки справочников («Серии», «Станции», «Плечи», «Напарники») и редактор
    напарника — стрелка «назад» + заголовок;
  - редакторы серии/станции — слева синее **«Готово»**, которое **фиксирует**
    запись (`commit()`) и возвращает к списку; заголовок редактора серии = имя серии
    или «Новая серия», станции — «Станция»;
  - системный Back ведёт себя так же, как кнопка слева (включая commit/сохранение
    напарника).
- Если под-экран открыт напрямую (deep link `showSettings*` из формы/шторки),
  возврат (Готово/Back) закрывает весь экран настроек и возвращает на исходный
  экран, а не в hub; редактор, открытый напрямую, возвращает туда же, а не в список.
- Переключатели и радио-кнопки сохраняются сразу (автосохранение 300 мс, §17).
  Числовой ввод есть только у длины пассажирского вагона (§17.5): некорректное
  значение подсвечивается, «Сохранить» неактивна, сохранённое значение не меняется.

### 17.2. Норма и регион; универсальные picker sheets (бывш. §30.2)

**Под-экран «Норма/Регион» сверху вниз:**
1. «НОРМА ЧАСОВ»: строка «{Месяц} {год}» (выбранный месяц `selectMonthOfYear`) со
   значением нормы в `ЧЧ:ММ` (из `NormaUseCase`; до загрузки — личная норма
   месяца); ниже синяя строка-кнопка **«Изменить норму»** → `router.showAbsence()` → «Новое
   отвлечение» (`AbsenceScreen`, §16.4).
   Подпись: «Месячная норма рабочего времени. По ней считается переработка и
   недоработка.»
2. «РАБОЧАЯ НЕДЕЛЯ»: три варианта — «Стандартная · 8 ч, Пн–Пт», «Шестидневная · 7 ч
   + Сб 5 ч», «Своя настройка» (выбранный — «✓ » и акцентный цвет). «Своя
   настройка» раскрывает семь строк Пн…Вс со счётчиком «− N ч +» (0…24); повторный
   тап по выбранной «Своей настройке» сворачивает/разворачивает список. Подпись:
   «Используется для нормы месяца и часов отпуска, больничного и других отвлечений.
   Праздники остаются нерабочими.» Правила расчёта — §16.3.
3. «СТРАНА»: плитка с флагом и названием (🇷🇺 Россия / 🇰🇿 Казахстан / 🇧🇾 Беларусь)
   → пикер «Страна».
4. «РЕГИОН» — только для RU и только если список регионов уже загружен или
   грузится (`GET /v1/regions` через `RegionalHolidaysRepository`): плитка с
   названием выбранного региона или «Стандартный календарь»; пока список пуст и
   грузится — «Загрузка регионов…» (не нажимается). Подпись «Региональные праздники
   добавятся к стандартному календарю.» → пикер «Регион» (первый вариант
   «Стандартный календарь» = `region = null`).
5. «ДОМАШНИЙ ЧАСОВОЙ ПОЯС»: для RU — плитка с описанием пояса → пикер «Часовой
   пояс» (11 вариантов, значение — смещение от Москвы в ms): «Калининград (MSK–1,
   UTC+2)» −1 ч, «Москва (UTC+3)» 0, «Самара (MSK+1, UTC+4)», «Екатеринбург (MSK+2,
   UTC+5)», «Омск (MSK+3, UTC+6)», «Красноярск (MSK+4, UTC+7)», «Иркутск (MSK+5,
   UTC+8)», «Якутск (MSK+6, UTC+9)», «Владивосток (MSK+7, UTC+10)», «Магадан
   (MSK+8, UTC+11)», «Камчатка (MSK+9, UTC+12)», «Анадырь (MSK+10, UTC+13)» (+1…+10 ч).
   Неизвестное значение отображается как «Москва». Для KZ — нередактируемая плитка
   «UTC+5 (Kazakhstan Time, KZT)», для BY — «UTC+3 (Минск)». Подпись «Установите
   местный часовой пояс. Учитывается при расчёте ночных, праздничных часов и
   переходных поездках.»
6. «ПЕРЕХОДНЫЕ МАРШРУТЫ» — только RU и пояс ≠ Москва: плитка «По местному времени» /
   «По московскому времени» → пикер «Переходные маршруты» (`CrossMonthTimezone.LOCAL`
   / `MOSCOW`, смысл — §17). Подпись «Определяет, по какому времени переходной
   маршрут относится к месяцу.»

**`SettingsPickerSheet(title, hint?, options)`** — нижняя шторка одиночного выбора
(раскрыта полностью, прокручивается): заголовок, необязательная подсказка, строки
с радио-кружком (+ эмодзи-флаг); выбранная строка жирнее. Тап по строке вызывает
действие и **сразу закрывает** шторку. Используется для страны, региона, пояса и
переходных маршрутов.

**Смена страны** (`changeCountry`, в т.ч. при выборе той же страны — повторная
загрузка):
- сразу записывает в настройки страну, пояс (KZ → +2 ч; BY → 0; RU — прежний),
  режим переходных маршрутов (для KZ/BY → `LOCAL`) и регион (сбрасывается в `null`,
  если не принадлежит новой стране);
- блокирующий диалог (`AnimationDialog`, закрыть нельзя, пока идёт загрузка) со
  спиннером «Загружаем производственный календарь для {Россия|Казахстан|Беларусь}»;
- загружает производственный календарь текущего года (и следующего, если сейчас
  декабрь), применяет его, накладывает праздники региона, **удаляет календари
  других стран**, обновляет выбранный месяц и список регионов;
- итог: «Календарь успешно загружен» + «ОК»; «Ошибка загрузки» (красным) + «OK»;
  «Нет интернета» + «OK». Повторить из диалога нельзя.
- ⚠️ Поведение Android (возможный баг, BUG-50): при ошибке страна/пояс/регион
  **уже** сменены, календарь — частично (ожидалась атомарность, как у региона).

**Смена региона** (`changeRegion`; выбор уже выбранного — ничего не делает):
атомарно — диалог «Загружаем региональные праздники: {название или «Без региона»}»;
сначала строгая загрузка праздников региона с сервера (текущий год + следующий в
декабре), только при успехе регион записывается, стандартный календарь
перезагружается и поверх накладываются региональные праздники, настройки
сохраняются, норма пересчитывается. Итог: «Региональные праздники применены, норма
пересчитана» + «ОК»; «Ошибка сервера» / «Не удалось загрузить региональные
праздники.\nРегион не изменён — попробуйте ещё раз позже.» + «ОК»; «Нет соединения с
интернетом» / «Проверьте Wi-Fi или мобильную сеть.\nРегион не изменён — для загрузки
праздников нужен интернет.» + «ОК». При ошибке регион остаётся прежним.

### 17.3. Учёт и ночные часы (бывш. §30.3)

- Под-экран «Учёт»: «НОЧНЫЕ ЧАСЫ» — строка «Ночь» со значением «ЧЧ:ММ - ЧЧ:ММ»
  (моно, по умолчанию 22:00 - 06:00) → `NightRangeSheet`; подпись «Интервал ночных
  часов для расчёта доплаты за работу ночью.». «БУДУЩИЕ МАРШРУТЫ» — переключатель
  «Учитывать будущие маршруты» / «С ещё не наступившей явкой» (`isConsiderFutureRoute`);
  подпись «Маршруты, время явки которых не наступило, будут учитываться при
  подсчёте отработанного времени.»
- `NightRangeSheet`: заголовок «Ночные часы», справа синее «Готово»; подпись
  «Интервал, который считается ночным временем»; две крупные моно-плитки «Начало» →
  «Конец» (ЧЧ:ММ), каждая открывает `AppTimePicker` («Начало ночи»/«Окончание ночи»,
  недавние значения по ключам `night_start`/`night_end`) и меняет только черновик;
  внизу кнопка «Сохранить». «Готово» и «Сохранить» одинаково сохраняют обе границы;
  закрытие шторки без них ничего не сохраняет. Интервал через полночь валиден.
  Пресетов нет.
- Учитывать будущие маршруты, формат времени и cross-month timezone применяются
  сразу и инициируют реактивный пересчёт экранов, но не переписывают маршруты.

### 17.4. Справочники и редакторы норм (бывш. §30.4)

- Списки серий/станций: поиск/список, add, tap→editor, swipe/delete→confirmation.
  Пустое состояние содержит действие добавления.
- Редактор серии: имя, тип тяги, тип нумерации секций (`1/2/3` или `А/Б/В`)
  и четыре нормы приёмки/сдачи (§0). Нумерация показана отдельным блоком
  «Номер секции» с выбором `1/2/3` или `А/Б/В`; выбор сохраняется автоматически;
  `0` нормы сохраняется как `null`. Изменение имени из `TimeBottomSheet`
  синхронизируется обратно по §2.12.
- Редактор станции: имя и четыре интервальные нормы (§0), те же правила `0→null`.
  Удаление справочной записи не очищает stationId/текст уже сохранённых маршрутов
  скрыто; экран должен корректно показать отсутствующую ссылку.
- ❓ Списки и редакторы серий/станций/напарников в этом аудите построчно не
  сверялись (их поведение описано в §2.10/§8.5, зона других разделов).
- **Плечи** (`SettingsShouldersContent`): подпись «Расстояние участка обслуживания.
  Подставляется автоматически при выборе плеча в маршруте.»; пусто — «Нет
  сохранённых плеч» / «Добавьте участки обслуживания с расстоянием»; иначе
  заголовок «ПЛЕЧИ · N» и карточка-список строк «{отправление} — {прибытие}»,
  справа моно «{distance} км» и «{linearMileageRate} ₽/км» (значение `Double` как
  есть, напр. «0.0 ₽/км»), шеврон; тап → `ShoulderEditBottomSheet` (§6.6) на редактирование;
  внизу акцентная кнопка «Добавить плечо» → та же шторка на
  создание. Сохранение: редактирование заменяет плечо с тем же `id`, новое —
  добавляется в конец; флаг «создать обратное» добавляет плечо с переставленными
  станциями, тем же расстоянием и ставкой. Удаление — из шторки. Изменения
  сохраняются автосохранением `UserSettings.servicePhases`.

### 17.5. Основные, маршрут, локомотив, поезд, отдых (бывш. §30.5)

- **Основные** («Основные», `SettingsRouteContent`), сверху вниз:
  1. «ТЕМА ОФОРМЛЕНИЯ» — радио «Как в системе» / «Светлая» / «Тёмная» (§17);
  2. «ВИД ТЯГИ» — радио «Электротяга» / «Теплотяга» (`defaultLocoType`; то же поле,
     что в под-экране «Локомотив»);
  3. «ФОРМАТ ВРЕМЕНИ» — «Часы и минуты» / «Например, 12:30» и «Десятичный» /
     «Например, 12,5» (`isDecimalTime`);
  4. «ВРЕМЯ РАБОТЫ» — переключатель «Стандартное время работы»
     (`usingDefaultWorkTime`); при включении ниже строка «Время работы» ЧЧ:ММ
     (`defaultWorkTime`, по умолчанию 12:00) → `AppTimePicker` «Время работы по
     умолчанию» (недавние по ключу `default_work_time`); подпись «Подставляется по
     умолчанию при создании нового маршрута.»;
  5. «СТИЛЬ ВЫБОРА ВРЕМЕНИ» — «Системный» / «Стандартный диалог Android» и
     «Кастомный» / «Быстрый ввод цифрами» (`useStandardTimePicker`; iOS — аналог
     системного/кастомного пикера);
  6. «СВЫШЕ 12 ЧАСОВ - ПАССАЖИРОМ» — радио «Всегда спрашивать» / «Автоматически» /
     «Никогда не спрашивать»; хранится только на устройстве двумя флагами
     (`dontAsk`, `autoAccepted`): ALWAYS_ASK = (false,false), AUTO = (true,true),
     NEVER_ASK = (true,false). Подпись «Если поездка длится более 12 часов, часть
     времени можно оформить как следование пассажиром.»
- **Маршрут** (`SettingsRouteFormContent`): «ЭЛЕМЕНТЫ ФОРМЫ» — «Показывать
  перерыв» / «Поля перерыва в форме маршрута»; «Переключатель «Одно лицо»» /
  «Отметка работы в одно лицо в маршруте»; «Добавить напарника» / «Раздел
  «Напарники» в форме маршрута». «РАЗДЕЛЫ МАРШРУТА» — «Показывать локомотив»,
  «Показывать поездную работу», «Показывать пассажиром», «Показывать прочую работу»
  (подпись «Маневровая, вывозная, при депо и др.»). Общая подпись «Управляет
  отображением разделов и элементов в форме маршрута.» Скрытие блока не удаляет его
  данные.
- **Локомотив** (`SettingsLocoContent`): если открыт из формы локомотива с серией —
  сверху кнопка «Настройки {серия}» (серия есть в справочнике → её редактор) или
  «Создать серию {имя} в справочнике» (→ редактор новой серии с этим именем).
  «ПОЛЯ ВВОДА ПОКАЗАНИЙ» — «Отопление» и под ним (если включено) «Учитывать в общем
  расходе» / «Отопление»; «Собственные нужды» и (если включено) «Учитывать в общем
  расходе» / «Собственные нужды»; подпись «Показывать эти поля в форме локомотива
  для ввода показаний.». «РОД ТОКА» — «Смена рода тока» / «Для
  переменно-постоянных электровозов.». «ТИП ТЯГИ ПО УМОЛЧАНИЮ» — «Электротяга» /
  «Теплотяга», подпись «Будет выбран при добавлении локомотива.». Показ полей
  отопления/собственных нужд/смены рода тока — только на устройстве (§17); флаги
  «учитывать в общем расходе» — в `UserSettings`. Скрытие не обнуляет показания.
- **Поезд** (`SettingsTrainContent`): «РАСЧЁТ ДЛИНЫ» — строка «Пассажирский вагон»
  «{N} м» (целое без дробной части, иначе через запятую; по умолчанию 24,5) →
  диалог «Длина пассажирского вагона»: поле «Метры» (только цифры, `,` и `.`),
  подсказка «По умолчанию 24,5 м», ошибка, если значение не число > 0;
  «Сохранить» (активна только при валидном значении) / «Отмена». Подпись под
  строкой: «Для пассажирского поезда значение У.Д. считается количеством вагонов и
  умножается на эту длину. Пассажирский поезд определяется по номеру. Для грузового
  поезда сохраняется расчёт 1 У.Д. = 14 м.» Подраздел открывается как из
  шестерёнки формы поезда, так и из общего списка настроек; значение
  синхронизируется между Android и PWA через `UserSettings`. Android отправляет
  настройки только когда общий флаг локальных изменений установлен и собственный
  `UserSettings.updateAt` новее последней успешной синхронизации. Pending от
  зарплаты, календаря или справочников не должен инициировать push старой длины.
  При отсутствии несинхронизированных изменений самой записи Android применяет
  серверные `UserSettings` независимо от локального `updateAt`, чтобы клиентская
  метка времени из будущего не блокировала PWA.
- **Отдых** (`SettingsRestContent`): «МИНИМАЛЬНЫЙ ОТДЫХ» — «Отдых в ПО» (по
  умолчанию 03:00), «Отдых в ПО (второй отдых)» (04:00), «Домашний отдых» (16:00),
  значения ЧЧ:ММ моно; подпись «Второй минимум применяется, когда два
  последовательных маршрута заканчиваются отдыхом в ПО.» Изменение влияет на новые
  расчёты всех экранов, но не материализует рассчитанное время в Route. Шторка
  выбора значения (`AppTimePicker` → `TimePickerApp`, заголовки «Минимальный отдых в
  ПО» / «Отдых в ПО (второй отдых)» / «Минимальный домашний отдых») для всех трёх
  минимумов открывается без дублирующего лейбла под барабаном и без чипов недавних
  значений (`showTimeLabel = false`, `recentTimes` не передаются); **жесты
  перетаскивания шторки отключены** (`swipeToDismiss = false` →
  `ModalBottomSheet(sheetGesturesEnabled = false)`), чтобы при прокрутке
  барабана она не уезжала за пальцем. Тап по скриму и кнопки закрывают как
  обычно; кнопка отмены подписана «Отмена» (не «Пропустить»). Для
  стандартного Material-пикера флаг не применяется.

Активные Compose-подэкраны и их назначение (для проверки будущих изменений):

| Файл | Контракт |
|---|---|
| `SettingsAccountingContent` | учёт, ночной интервал, будущие маршруты (§17.3) |
| `SettingsNormaContent` | норма, рабочая неделя, страна, регион, timezone, переходные маршруты, loading/error (§17.2) |
| `SettingsRestContent` | минимумы отдыха (§17.5) |
| `SettingsRouteContent` | тема, вид тяги, формат и стиль времени, время работы, Passenger12h (§17, §17.5) |
| `SettingsRouteFormContent` | видимость полей маршрута (§17.5) |
| `SettingsLocoContent` | видимость полей локомотива, тип тяги (§17.5) |
| `SettingsTrainContent` | длина пассажирского вагона (§6.2, §17.5) |
| `SettingsShouldersContent` | список/редактор плеч (§6.6, §17.4) |
| `SettingsSeriesListContent` | справочник серий (§17.4) |
| `SettingsStationListContent` | справочник станций (§17.4) |
| `SettingsPartnerListContent` / `SettingsPartnerEditorContent` | встроенный путь справочника напарников (§8.5) |
| `SettingsUnifiedUi` / `SettingsCommon` | общие строки, карточки, `SettingsPickerSheet` |

Источник: `features/route/.../ui/SettingsScreen.kt`, `ui/settings/*.kt`,
`viewmodel/SettingsViewModel.kt` (`timeZoneList`, `changeCountry`, `changeRegion`,
`setPassenger12hOption`, `Passenger12hOption`), `viewmodel/SettingsUiState.kt`,
`domain/.../entities/setting/UserSettings.kt` (значения по умолчанию, `NightTime.toString`),
`domain/.../entities/route/LocoType.kt`.

### 17.6. Экспорт диагностики (карточка «О приложении»)

- В карточке «О приложении» доступно действие «Экспорт диагностики» с подписью
  «Для поддержки».
- Подтверждение экспорта показывается через общий `AppAlertDialog`: нейтральный
  фон карточек `secondary`, основной текст темы и синяя акцентная кнопка без
  оранжевой заливки.
- Перед системным окном отправки показывается краткое уведомление: в поддержку
  будут отправлены обезличенные диагностические данные без личной информации.
- Route ID, содержимое маршрутов, заметки, email, идентификаторы авторизации и
  токены в файл не включаются. Файл создаётся во внутреннем cache-каталоге и
  передаётся наружу только после явного действия пользователя через FileProvider.
  Системное окно отправки получает готовое письмо: адрес поддержки
  `locodriver.app@yandex.ru`, тему с диагностическим кодом, короткий текст и
  диагностический `.txt` во вложении.

Источник: `ui/SettingsScreen.kt` (`SettingsAboutCard`),
`domain/.../repositories/DiagnosticRepository.kt`,
`domain/.../entities/diagnostic/DiagnosticReportFormatter`.

---

## 18. Экран «Профиль» и аутентификация (ProfileScreen)

### 18.S. Назначение и состояния экрана

Аккаунт, подписка, синхронизация, вход/регистрация, миграция данных. Корневая
вкладка нижнего меню «Профиль» (§3.2 `showProfile`).

**Состояния экрана (взаимоисключающие, проверяются в этом порядке):**
1. `userDetailsState == Loading` → полноэкранный `GenericLoading` «Получаем данные
   пользователя...».
2. Нужна легаси-миграция (`!isFirstAppEntry && !isMigrated`) → экран переноса данных (§18.5).
3. Есть токен (`isLoggedIn`):
   - если профиль не загрузился из-за сети (`isProfileNetworkError`) → экран
     «Нет интернета» (или «Возможно, мешает VPN» + подсказка, если активен VPN) с
     иконкой отключённого сигнала и кнопкой **«Повторить»** внизу → `refresh()`;
   - иначе → авторизованный профиль (§18.1).
4. Токена нет → форма входа/регистрации (§18.0).

Поверх любого состояния: при `AuthState.Loading` — `GenericLoading` «Выполняется
вход...», при `RegistrationState.Loading` — «Регистрируем нового полььзователя...» (⚠️
Поведение Android (возможный баг, BUG-51): опечатка в тексте); у обоих есть крестик — он
отменяет запрос и
сбрасывает состояние. Глобальные диалоги подписки — §19.5–19.6.

### 18.0. Форма входа/регистрации — состав и порядок

Один экран на оба режима, режим выбирается переключателем. Порядок блоков
сверху вниз (тот же в PWA — экраны намеренно совпадают):

1. Заголовок «Добро пожаловать!».
2. Переключатель **«Вход | Регистрация»** (`SwitchApp`, состояние `login`).
   Начальный режим — `login = !isFirstAppEntry`; флаг первого запуска сбрасывается
   ещё при старте приложения (`MainViewModel`), поэтому форма фактически всегда
   открывается в режиме **«Вход»**. Значения полей при переключении режима
   сохраняются.
3. Поле **email** (плейсхолдер «email», клавиатура email).
4. Поле **пароль** (плейсхолдер «пароль») отображается всегда, без анимации
   появления. Под ним при `0 < длина < MIN_LENGTH_PASSWORD` (= **4**) — «Минимум 4
   символа». В режиме входа под полем текстовая кнопка **«Создать новый пароль»**
   (сброс пароля, см. ниже); во время 60-секундного кулдауна она неактивна и
   подписана «Повторить через N с» (обратный отсчёт по секундам).
5. Поле **«подтвердите пароль»** отображается в режиме регистрации. При
   несовпадении введённых значений показывается «Пароли не совпадают». У обоих
   парольных полей есть отдельная кнопка-глаз для показа и скрытия значения.
6. Кнопка **«Войти» / «Зарегистрировать»** отображается всегда и визуально
   остаётся кнопкой даже в disabled-состоянии (фон `primary` с alpha 0.35). Для
   входа она активна при валидном email (`android.util.Patterns.EMAIL_ADDRESS`) и
   пароле не короче 4 символов; для регистрации — дополнительно только при
   непустом совпадающем подтверждении и обоих принятых чекбоксах.
7. Чекбоксы согласий (только в режиме регистрации, появляются с анимацией):
   «Я принимаю условия **Лицензионного соглашения**» и «Я ознакомлен с **политикой
   обработки персональных данных**» (выделенная часть подчёркнута акцентным цветом).
   Тап по чекбоксу переключает его; тап по тексту строки (в любом месте) открывает
   соответствующий документ во внешнем браузере и чекбокс не переключает.
8. Разделитель **«или»**.
9. Кнопка **VK ID** (`OneTap`, «войти в другой аккаунт» включено). В режиме «Вход»
   запускает вход по VK (`authWithVKID`), в режиме «Регистрация» — сразу
   регистрацию по VK (`registeredUserByVKID`, без диалога подтверждения).
   ⚠️ Поведение Android (возможный баг, BUG-52): ошибка самого VK SDK (`onFail`) на форме
   входа только логируется — пользователь сообщения не видит.
10. Подпись под ней: «Если аккаунта ещё нет, он будет создан автоматически.
   Продолжая, вы принимаете Лицензионное соглашение и политику обработки
   персональных данных.» — «Лицензионное соглашение» и «политику … данных»
   кликабельны.

Вся форма входа/регистрации вертикально прокручивается, включая небольшие
экраны и состояние с открытой клавиатурой; верх формы не должен становиться
недоступным из-за центрирования контента.

Email визуально отделён от парольной группы увеличенным вертикальным
интервалом; поля пароля и его подтверждения расположены рядом с малым интервалом.

**Сброс пароля («Создать новый пароль»).** Отправляет `POST /v1/page/forgot_password?email=…`
(email — query-параметр, тела нет) с email из поля (пробелы удаляются; **клиентской проверки формата email нет** —
запрос уходит и с пустым полем). Во время запроса — snackbar «Отправляем
запрос...». Ответы: `200` → «Если такой email зарегистрирован, мы отправили на него
ссылку. Проверьте входящие и папку «Спам».» (долгий snackbar, ответ нейтральный —
клиент не знает, есть ли аккаунт); `429` → «Слишком много попыток, попробуйте
позже»; сеть/сервер → текст ошибки (с VPN-подсказкой, если активен VPN). После
`200` и `429` запускается локальный кулдаун **60 с**. Сброс идёт ссылкой из письма
(веб-страница), ввода кода или нового пароля в приложении нет.

**Результаты входа/регистрации.**
- Ошибка входа по email: `401` → «Неверная почта или пароль», прочее — «Ошибка: …»;
  показывается долгим snackbar (с VPN-подсказкой). Ошибки VK — ниже.
- Ошибка регистрации: `409` → «Пользователь с таким email уже существует», прочее —
  общий текст («Ошибка: N» / «Ошибка сервера: N»).
- Успешный **вход** (email или VK): смена аккаунта (`AccountSwitcher`: сброс прежнего
  `userId`, сохранение токена, загрузка серверного срока подписки — §19.5), затем
  перечитывание профиля и **однократная полная загрузка данных с сервера**
  (`syncFromRemote`: дни отвлечений, `UserSettings` со сроком подписки =
  `max(локальный, серверный)`, настройки зарплаты, маршруты) — без проверки
  подписки.
- Успешная **регистрация по email**: смена аккаунта; если на устройстве есть
  легаси-срок локальной покупки (`getSubscriptionExpiration() != 0`), он
  переносится в `UserSettings.subscriptionPeriod`; затем **первая выгрузка**
  (`firstSyncAfterRegistration`: `UserSettings`, `SalarySetting`, тарифные ставки
  по месяцам, дни отвлечений, нормы серий/станций, напарники и все
  несинхронизированные маршруты) — без проверки подписки; время успешной выгрузки
  сохраняется как «последняя синхронизация».
- Успешная **регистрация по VK**: то же без переноса легаси-срока.

**Почему VK ниже формы, а не выше.** Переключатель «Вход/Регистрация»
управляет только формой почты и пароля. Вход через VK ID — альтернативный
способ, к переключателю он не относится, поэтому вынесен из его области и
отделён разделителем. Согласие с документами для VK-пути оформлено текстом
под кнопкой: чекбоксы формы кнопку VK не гейтят и в режиме входа вообще
не показываются.

**Юридические ссылки** (`url_to_license_agreement`, `url_to_user_agreement`,
`url_to_personal_data_processing_policy` в `features/route/res/values/string.xml`
и `features/login/.../string.xml`) ведут на собственный домен:
`locodriver.ru/license.html`, `/offer.html`, `/privacy.html`. Раньше
соглашение лежало на Яндекс.Диске, а вместо политики ПДн стояла ссылка на
текст закона — это забраковал эквайер: нужны свои документы с описанием
платных услуг, порядка возврата средств и юридическими реквизитами.

### 18.1. Авторизованный профиль — состав сверху вниз

Прокручиваемый список (боковые отступы 12dp), pull-to-refresh = полная ручная
синхронизация + перечитывание профиля. Заголовок групп — UPPERCASE `labelMedium`,
`onSurfaceVariant`. Все даты — в поясе отображения пользователя
(`UserSettings.displayTimeZone()`, §0.1: Россия/Беларусь → GMT+3, Казахстан → местный пояс).

1. Заголовок **«Профиль»** (`headlineLarge`).
2. **Шапка VK** (карточка): состояние берётся из `vkUserState` (профиль VK SDK) и
   `UserRemote.vkId` (признак привязки на сервере):
   - VK-профиль загружен → аватар 52dp (фото VK или заглушка), имя, бейдж VK +
     vk_id моноширинным 13sp; справа красное «Отвязать» (если `vkId` с сервера не
     пуст);
   - профиля VK нет → заголовок «VK ID привязан» (если `vkId` с сервера есть; под ним
     «Войдите в VK, чтобы показать профиль») или «VK ID не привязан», ниже кнопка
     VK OneTap во всю ширину: при уже привязанном на сервере VK — только локальная
     авторизация SDK без сетевого запроса (`onVkAuthForLinkedAccount`), иначе — привязка
     (`attachVKID`, §18.4). Ошибка SDK → snackbar «Ошибка привязки VK:
     {описание}»;
   - при `vkId` с сервера и отсутствии профиля VK под блоком дополнительно красная
     ссылка «Отвязать VK ID».
   - Пока идёт загрузка — маленький спиннер.
3. Группа **EMAIL**: иконка письма; адрес (`UserRemote.email`) с карандашом
   «Изменить почту» → шторка «Новый email» (§18.4). Если email пуст — «Почта не
   добавлена» / «Вход только через VK ID» и кнопка **«Добавить»** → шторка
   «Добавить почту» (§18.6). Ошибка загрузки профиля в этом блоке — «Ошибка загрузки».
4. Группа **ПОДПИСКА**:
   - карточка **«Машинист Pro»** (иконка короны). При активной подписке
     (`subscriptionPeriod > now`, без грейса) — тёмная карточка (`primary`), бейдж
     «АКТИВНА» (скрыт при масштабе шрифта > 1.15), подпись «Оплачено до
     dd.MM.yy HH:mm»; без подписки — светлая карточка и «Оформить подписку —
     откроется больше возможностей». Тап → гейт покупок (§3.2) → экран «Покупки».
   - строка **«Пригласить друга»** / «Получите бесплатные дни обслуживания» → §18.R.
5. Группа **СИНХРОНИЗАЦИЯ**:
   - при активной подписке — кнопка «Синхронизация» (иконка sync слева, спиннер
     вместо иконки во время синхронизации) → `startSync` (§18.5); под ней «Последняя
     синхронизация: dd.MM.yy HH:mm» (из локальной метки последней успешной
     синхронизации); при загрузке маршрутов — плашка «Загрузка маршрутов: N из M» с
     линейным прогрессом;
   - без подписки — текст «Раздел синхронизации доступен после оплаты подписки.»
   ⚠️ Поведение Android (возможный баг, BUG-53): если синхронизаций ещё не было (метка = 0),
   в строке «Последняя синхронизация» подставляется **текущее время** — вводит в
   заблуждение.
6. Группа **ДАННЫЕ**: строка «Корзина маршрутов» / «N маршрут/маршрута/маршрутов»
   (иконка корзины) → экран корзины (§9.4; как и реферальный экран — флаг внутри
   `ProfileScreen`, не nav-destination).
7. Кнопка-обводка **«Выйти из аккаунта»** (цвет ошибки) → `logOut` **без
   подтверждения**: в защищённом хранилище очищаются токен, vk_id и `userId`,
   сбрасывается курсор дельта-синхронизации маршрутов; экран переходит в форму
   входа. Локальные маршруты и настройки (включая срок подписки) не удаляются;
   сессия VK SDK при выходе не завершается.

При каждой успешной загрузке профиля (`GET /v1/users/me`) Android сохраняет
`user.id` как `userId`, тихо обновляет срок подписки с сервера
(`restorePurchases` без snackbar'ов; локальный срок меняется, только если серверный
срок в будущем) и, если сервер знает vk_id, а локально его нет, —
сохраняет vk_id локально. `401` на загрузке профиля = истёкшая сессия (§18.5).

### 18.2. Логика: аутентификация, VK, email, синхронизация

- **ViewModel**: `ProfileViewModel`. Данные пользователя (`UserRemote`), срок
  подписки (`purchasesEndTime`), VK-профиль (`VkUserInfo`).
- **Аутентификация**: вход по email/паролю (`authWithEmail`), по VK ID
  (`authWithVKID`); регистрация (`registeredUserByEmail`/`ByVKID`); смена email
  (`updateEmail`; отдельного подтверждения email в UI нет — поле
  `resentVerificationEmailButton` легаси); восстановление пароля
  (`forgotRequest`/`forgotResetState`); привязка VK ID к существующему аккаунту
  (`attachVKID`, `onVkAuthForLinkedAccount`), отвязка (`removeUsersVKID`); обновление
  VK-токена (`vkIdRefreshToken`); выход (`logOut`).
- Если регистрация по email получает от сервера HTTP `409 Conflict`, Android и
  PWA показывают точное сообщение: **«Пользователь с таким email уже существует»**.
  Для остальных ошибок сохраняется общий сетевой/серверный текст.
- После успешного входа по email или VK Android дожидается отдельной загрузки
  серверного `UserSettings.subscriptionPeriod` и сохраняет срок локально до
  запуска обновления профиля и полной синхронизации. Ошибка этого шага не
  отменяет вход, но показывается пользователю; полная синхронизация остаётся
  дополнительной попыткой обновления.
- **VK ID подтверждается access-токеном (с версии 3.0.4).** Личность пользователя
  выводит сервер: клиент шлёт `vkAccessToken` (`accessToken.token` из VKID SDK)
  и `vkClientId` (`BuildConfig.VKID_CLIENT_ID` из `secret.properties`), сервер
  меняет токен на VK user id через `id.vk.com/oauth2/user_info` и присланному
  клиентом id не верит. Раньше вход был по одному лишь vk id — публичному
  значению из адреса страницы `vk.com/id…`, то есть входом в чужой аккаунт.
  - Токен добавляется в три запроса: `POST /v1/auth` (`methodAuth = "vkId"`),
    `POST /v1/auth/create`, `PATCH /v1/auth/vkId/add`. `PATCH /v1/auth/vkId/remove`
    не изменён.
  - Легаси-поля (`auth_param`, `vkId`, `token`) продолжают уходить с прежними
    значениями: бэкенд с проверкой раскатывается уже ПОСЛЕ публикации Android,
    и до его деплоя вход обеспечивают именно они. Сервер с проверкой их
    игнорирует. Вход по email/паролю не затронут — новые поля `null` и при
    `explicitNulls = false` в тело запроса не попадают.
  - **Access token нигде не сохраняется**: он живёт ровно один сетевой запрос,
    в `SecureTokenStorage` не кладётся, в логи и Sentry не пишется. Локально
    по-прежнему хранится только vk id — как признак «VK привязан».
  - Ответы `POST /v1/auth` для `vkId` разложены по `AuthState.Error.vkError`
    (`VkAuthError`): 401 `vk_token_invalid` → «не удалось подтвердить вход,
    повторите»; 401 `vk_access_token_required` → «обновите приложение» (на
    сервере уже выключен переходный флаг); 404 `vk_user_not_found` → аккаунта
    с этим VK нет; 503 `vk_unavailable` → «VK ID временно недоступен».
    Голый 401 без известного `detail` трактуется как «аккаунта нет» — так
    отвечает ещё не обновлённый прод. Текст «Неверная почта или пароль» для
    VK-входа больше не используется.
  - `VkAuthError.UserNotFound` (вход по VK, аккаунта с этим VK нет) не
    показывается как ошибка входа: вместо неё встаёт диалог «Аккаунта с этим
    VK нет» → «Создать» / «Отмена» (`ProfileUiState.vkRegistrationOffer`).
    «Создать» регистрирует аккаунт тем же VK-токеном (`confirmVkRegistration`),
    почта берётся из данных VK, пароль не задаётся — форму почты и пароля
    пользователю не показываем, почту можно добавить позже в профиле.
    «Отмена» (`dismissVkRegistration`) стирает отложенный токен.
    Токен до подтверждения лежит только в памяти ViewModel.
  - Ошибка привязки (`attachVKID`) доходит до пользователя snackbar'ом через
    `ProfileUiState.vkLinkMessage`; 409 `vk_id_already_linked` показывается как
    «Этот VK ID уже привязан к другому аккаунту «Машиниста»». Тем же путём
    показываются ошибки отвязки.
  - `PATCH /v1/auth/vkId/add` и `/vkId/remove` отвечают `SuccessResponse`
    (`{"status_code":200,"content":"…"}`), а не пользователем. Тело ответа не
    разбирается, обновлённый профиль дочитывается отдельным `getUserProfile`.
    Раньше клиент ждал там `UserResponse`, падал на разборе и всегда уходил в
    Error: сервер привязку делал, а локальный признак «VK привязан» не
    сохранялся и профиль не обновлялся; при отвязке не чистился локальный
    vk_id и не завершалась сессия VK SDK.
  - **Шапка профиля при выполненном входе** — компактная строка: аватар 52dp,
    имя из VK, под ним бейдж VK и сам vk_id моноширинным (значение с сервера,
    `UserRemote.vkId`). Подписи «Вход через VK ID» нет: она дублирует то, что
    и так видно по бейджу, а номер полезен для сверки со страницей VK.
  - **Отвязка VK** — «Отвязать» справа в той же строке; когда профиль VK ещё
    не загружен (нет сессии SDK), вместо неё показывается ссылка «Отвязать
    VK ID» под блоком. Видна, когда `vkId` пришёл с сервера (наличие сессии
    VK SDK не требуется). Действие
    деструктивное, поэтому подтверждается диалогом. Если у аккаунта пустой
    `email` — он заведён через VK, пароля у него нет, и отвязка означала бы
    потерю доступа: вместо подтверждения показывается диалог «Сначала
    добавьте почту» с переходом в шторку добавления почты и пароля.
    После успешной отвязки чистится локальный vk_id, завершается сессия
    VK SDK (`VKID.instance.logout`) и обновляется профиль.
- **Добавление почты и пароля к VK-аккаунту.** У аккаунтов, заведённых через
  VK (`registerByVKID` шлёт `password = ""`), почты и пароля нет: VK —
  единственный способ войти, и если привязка отвалится, доступ потерян.
  В группе «EMAIL» профиля при пустом `user.email` вместо адреса выводится
  «Почта не добавлена» с подписью «Вход только через VK ID» и кнопкой
  «Добавить» (вместо карандаша «изменить»). Кнопка открывает шторку
  `AppEmailPasswordBottomSheet` — два поля, почта и пароль (не короче
  `MIN_LENGTH_PASSWORD` символов), по стандарту `docs/DIALOGS_STANDARD.md`.
  - Запрос: `PATCH /v1/auth/email/add` с телом `{email, password}`
    (`AuthManager.addEmail` → `ProfileUiState.addEmailState`). Сервер
    отказывает без пароля (400) и при занятой почте (409); тексты ошибок
    показываются под полем, успех — snackbar'ом, после чего профиль
    перечитывается и в группе появляется адрес с карандашом.
  - Если почта уже есть, сервер отвечает 200 и ничего не меняет — менять
    адрес нужно через `PATCH /v1/auth/email/update` (карандаш).
- **Синхронизация**: в профиле отображается одна кнопка «Синхронизация»
  (`startSync`) вместо раздельных upload/download. Операция сначала получает
  серверное состояние, затем объединяет данные и показывает единый диалог
  прогресса (`SyncStepState`, `SyncType.Sync`). В едином диалоге вместо четырёх
  отдельных круговых индикаторов показывается один линейный progress bar,
  текущий этап и процент выполнения (четыре этапа дают шаг 25%). В кнопке используется
  горизонтальная компоновка: иконка синхронизации слева, текст справа. Под
  кнопкой показывается время последней успешной синхронизации.
  - В начале полной синхронизации клиент отправляет в авторизованный
    `POST /v1/client-info` только свою платформу и актуальную версию:
    `{platform: "android"|"ios"|"pwa", version: String}`. Сервер атомарно
    обновляет соответствующий ключ JSONB-поля `user.app_versions`, сохраняя
    значения других платформ. Эти служебные данные обратно на устройство не
    загружаются; ошибка их отправки не блокирует основную синхронизацию.
  - Маршруты объединяются по `basicData.id`; если маршрут изменён с обеих сторон,
    побеждает версия с большим `updatedAt` (LWW). Любое локальное сохранение и
    soft-delete обновляет `updatedAt`; загруженная серверная версия сохраняет
    серверное значение времени.
  - Локальный новый/изменённый маршрут выгружается; новый/более свежий серверный
    загружается. После первой успешной выгрузки клиент сохраняет локальный
    `remoteRouteId` как маркер «маршрут уже существовал в облаке» (для старых
    установок маркер восстанавливается при первом совпадении `id` с сервером).
    Локальный soft-delete вызывает `DELETE /v1/route/{id}` и остаётся в локальной
    корзине на 30 дней. Отсутствие маршрута в любом серверном ответе, включая
    `full_resync`, никогда не считается удалением. Удаление с другого устройства
    применяется только по явному серверному tombstone (`deletedIds`), при этом
    локальная полная копия хранится в корзине 30 дней без дополнительного
    подтверждения пользователя. Если маршрут после предыдущей синхронизации
    изменили локально (`isSynchronized == false`), tombstone не удаляет его:
    клиент повторно выгружает локальную версию. При ошибке получения списка merge
    и локальные удаления не выполняются. `GET /v1/route/` при пустом
    списке возвращает `200 []` (не `404`) — пустой аккаунт не считается ошибкой
    сети/сервера.
  - **Массовое удаление.** Количество отсутствующих в ответе маршрутов значения
    не имеет: отсутствие игнорируется. Явные tombstone применяются одинаково для
    одного и множества маршрутов — копии переходят в корзину на 30 дней. Действия
    «Принять удаление» нет. Возврат маршрута из `remoteDeletionPending`, если сервер снова
    прислал его целиком, — ❓ не проверено (§9.4).
  - **`UserSettings` и `SalarySetting` — LWW только по дате**, как маршруты:
    локальная метка новее серверной → push, серверная новее → pull, равны →
    ничего. У `UserSettings` это `updateAt`; у `SalarySetting` — новое локальное
    поле `updatedAt` (SQLDelight-миграция 6, default `0`) против серверного
    `updated_at` из `GET /v1/salary_settings/` (snake_case, epoch ms; принимается
    в `SalarySettingResponse` DTO и в POST не отправляется — сервер ставит свою
    метку). Флаг `settingsPending` на решение pull/push этих двух записей не
    влияет — он только триггер «есть что пушить» для коллекций без дат
    (тарифы, дни отвлечений, нормы, напарники) и для выгрузки `SalarySetting`,
    когда серверной строки ещё нет (404). Срок подписки — максимум локального
    и серверного значений; если серверный больше — переносится локально в любой
    ветке. Строка `user_settings`, созданная сервером при оплате, имеет
    `updateAt = 0` и никогда не затирает локальные станции/серии.
  - Сервер на POST ставит собственный `updateAt`/`updated_at`, поэтому после
    успешного push клиент перечитывает серверную метку и переносит её в локальную
    запись (только метку и только если за время push локальная запись не
    менялась) — иначе следующий sync делал бы лишний pull.
  - Любая локальная правка `UserSettings` (в т.ч. станции/серии/типы прочей
    работы из форм) и `SalarySetting` (`SalarySettingUseCase.saveLocalEdit`)
    штампует метку строго больше прежней (`max(now, prev + 1)`), и только если
    данные реально изменились; загруженная серверная версия сохраняет
    серверную метку. Тарифы, дни отвлечений, нормы и напарники отправляются
    после локального сохранения; при сетевой ошибке остаётся локальный флаг
    pending для следующей ручной попытки. Full-replace семантика существующих
    серверных эндпоинтов не меняется.
  - Все входы в `SyncManager` (ручной sync, автоматический sync экранов,
    WorkManager, autosave настроек и отдельного маршрута) сериализованы одним
    mutex. Серверные full-replace ответы не могут применяться параллельно с
    выгрузкой локального изменения. Если есть pending-настройки, сначала
    выгружается локальная версия и только затем читается сервер; без pending
    серверная версия считается изменённой на другом устройстве. Успешный
    autosave обновляет время последней синхронизации.
  - Открытие Главного и «Всех маршрутов» выполняет автоматическую проверку не
    чаще одного раза в 5 минут и не запускает второй sync, пока уже идёт другой.
    Ручная кнопка и локальный autosave работают без этого ограничения.
  - На Главном, «Всех маршрутах», Календаре, Настройках, в Профиле и Покупках
    жест протягивания содержимого вниз (`pull-to-refresh`) запускает полную
    ручную `syncBidirectional`. Для явного жеста 5-минутный cooldown не
    применяется. Пока операция ожидает mutex или выполняется, сверху показывается
    стандартный индикатор обновления; повторный жест на этом экране игнорируется.
    После завершения показывается snackbar об успехе либо частичных ошибках.
    Календарь дополнительно перечитывает выбранный месяц, Профиль — данные
    аккаунта, Покупки — тарифы и покупки. Гейт активной подписки и авторизации
    тот же, что у остальных ручных путей синхронизации.
  - Ручная и автоматическая операция синхронизации имеют общий дедлайн 25 секунд;
    connect/request/socket timeout отдельного HTTP-запроса также равен 25 секундам.
    По дедлайну цепочка запросов отменяется, mutex освобождается, локальные pending-
    флаги и несинхронизированные маршруты сохраняются. Пользователь получает
    snackbar «Синхронизация не выполнена» с причиной: отсутствие соединения,
    таймаут, ответ сервера либо ошибка обработки данных. Индикатор pull-to-refresh
    рисуется поверх содержимого с контрастными цветами; на Главном располагается
    ниже app bar, а на «Всех маршрутах» — над списком маршрутов.
  - Синхронизация доступна только при активной подписке: без неё
    `syncBidirectional` сразу завершается ошибкой «Синхронизация доступна по
    подписке», ничего не читая и не отправляя. Без подписки с сервером общаются
    только вход (однократная загрузка `syncFromRemote`), регистрация (первая
    выгрузка `firstSyncAfterRegistration`), загрузка профиля, восстановление
    покупок, реферальный статус и проверка оплаты (§18.0, §19.6). JSON-контракт и
    набор эндпоинтов не изменены.
  - Экран «Нет интернета» показывается только для транспортных ошибок
    (недоступный хост, разрыв соединения, таймаут). Ошибки JSON-декодирования,
    маппинга и локального сохранения показываются в диалоге как ошибка
    соответствующего этапа с технической причиной, а не маскируются сообщением
    об отсутствии интернета.
- **Миграция** данных (`startMigration`, `MigrationState`) — одноразовый перенос
  локальных данных старых установок в новый аккаунт (маршруты + настройки, с
  прогрессом); подробно — §18.5.

Источник: `features/route/.../ui/ProfileScreen.kt`, `viewmodel/ProfileViewModel.kt`,
`navigation/ProfileDestination.kt`, `ui/ReferralScreen.kt`, `ui/TrashScreen.kt`,
`component/AppEmailPasswordBottomSheet.kt`, `component/AppInputBottomSheet.kt`,
`ui/SyncProgressDialog.kt`, `data_remote/.../remote_rest/AuthManager.kt`,
`remote_rest/ApiForSendEmail.kt`, `remote_rest/SyncManager.kt`
(`syncFromRemote`, `firstSyncAfterRegistration`, `syncBidirectional`),
`data_remote/src/androidMain/.../use_case/SubscriptionHelper.kt`,
`core_android/.../util/DateAndTimeConverter.kt`, `core_android/.../util/StringExtention.kt`,
`app/.../viewmodel/MainViewModel.kt` (флаг первого запуска).

### 18.3. Состояния аккаунта (бывш. §31.1)

- **Гость** (токена нет): форма входа/регистрации §18.0.
- **Регистрация**: режим той же формы. Успех сразу авторизует (подтверждения email
  нет) и выполняет первую выгрузку (§18.0). Ошибка — snackbar; введённые значения
  остаются.
- **Авторизован**: §18.1 — шапка VK, email, подписка + реферальная программа,
  синхронизация, корзина, выход.
- **Нет сети при загрузке профиля**: отдельный экран «Нет интернета»/«Возможно,
  мешает VPN» с «Повторить» (§18.S). Остальные ошибки загрузки профиля (не сеть) —
  профиль показывается с «Ошибка загрузки» в группе EMAIL.
- **Продление сессии** (`SessionRefresher`, вызывается из `StartApp` при
  старте процесса): если в хранилище есть токен и он выдан больше 7 дней
  назад (`iat` в payload JWT) либо вовсе без `iat` (выдан до 2026-09), клиент
  делает `POST /v1/auth/refresh` с текущим bearer и сохраняет новый токен.
  Сервер принимает и просроченный токен в пределах своего грейса (365 дней
  после `exp`), поэтому сессия активного пользователя не истекает никогда;
  повторный вход нужен только после года+ без запуска приложения. Сетевая
  ошибка молча откладывает попытку до следующего запуска; 401 на refresh
  обрабатывается как любой другой 401 — см. §18.5 «Сессия истекла».
  UI это не затрагивает.
- **Loading** auth/registration: полноэкранный `GenericLoading` с крестиком отмены
  (§18.S) — повторная отправка невозможна. Transport error — snackbar с
  VPN-подсказкой; серверная ошибка — snackbar с текстом из §18.0 (inline-ошибок
  у полей формы входа нет).

### 18.4. Email, пароль и VK (бывш. §31.2)

- **Смена email** (карандаш в группе EMAIL) — `AppInputBottomSheet` (§18.6):
  заголовок «Новый email», начальное значение — текущий email (выделен целиком,
  фокус и клавиатура email сразу), кнопка «Сохранить» активна только при валидном
  email (`Patterns.EMAIL_ADDRESS`); при открытии состояние запроса сбрасывается.
  `PATCH /v1/auth/email/update` `{email}` (Bearer); Android перед запросом
  искусственно ждёт 2 с (`delay(2000)`), всё это время на кнопке спиннер. Ошибка
  → текст под полем («Ошибка: {код} - {сообщение}», как есть), шторка остаётся;
  правка поля стирает ошибку. Успех → шторка закрывается, затем snackbar «Email
  успешно изменён», профиль перечитывается.
- **Добавление почты** к VK-аккаунту без email — `AppEmailPasswordBottomSheet`
  «Добавить почту» (§18.2, §18.6): подсказка «Сейчас в аккаунт можно войти только
  через VK ID. Почта с паролем — второй способ входа на случай, если привязка VK
  отвалится.», кнопка «Добавить». `PATCH /v1/auth/email/add` `{email, password}`.
  Ошибки под полем пароля: 400 «Не удалось сохранить пароль. Проверьте, что он
  заполнен.»; 409 «Эта почта уже занята другим аккаунтом «Машиниста».»; 401/403
  «Сессия устарела. Войдите в аккаунт заново.»; 404 «Аккаунт не найден.»; ≥500
  «Сервер временно недоступен. Попробуйте позже.»; иначе `detail` сервера или «Не
  удалось привязать почту. Ошибка N.». Успех → шторка закрывается, snackbar «Почта
  добавлена — теперь можно входить и по паролю», профиль перечитывается.
- **Сброс пароля** — §18.0 (только ссылкой из письма, кулдаун 60 с). Android отправляет
  запрос через основной HTTPS API
  `https://api.locodriver.ru/`, как и остальные auth-запросы. Отдельный
  незашифрованный DDNS-домен не используется: его DNS/HTTP-фильтрация на
  устройстве не должна ложно выглядеть как отсутствие интернета.
- **Привязка VK** к существующему аккаунту отличается от входа через VK: OneTap в
  шапке профиля → `PATCH /v1/auth/vkId/add` с VK access-токеном; успех → локально
  сохраняется vk_id, профиль перечитывается; ошибка → долгий snackbar (409 «Этот VK
  ID уже привязан к другому аккаунту «Машиниста».»; `vk_token_invalid` «Не удалось
  подтвердить вход через VK ID. Попробуйте ещё раз.»; `vk_access_token_required`
  «Обновите приложение: привязка VK ID изменилась.»; 503 «VK ID временно
  недоступен, попробуйте позже.»; иначе «Не удалось привязать VK ID (ошибка N).»).
  Ошибка привязки не разлогинивает email-сессию.
- **Отвязка VK** — только после подтверждения (§18.2: диалог «Отвязать VK ID?» / «Вход
  через VK перестанет работать. Останется вход по почте {email} и паролю, а
  привязать VK можно будет снова.», красное «Отвязать» / «Отмена»; при пустом email
  вместо него «Сначала добавьте почту» → «Добавить почту» открывает шторку
  добавления). `PATCH /v1/auth/vkId/remove`. **Отвязка отзывает все токены
  аккаунта** (сервер инкрементирует `user.token_version`; сессии через VK на других
  устройствах перестают работать) и возвращает в `access_token` новый токен для
  этого устройства — клиент сохраняет его до следующего запроса
  (`RevokeSessionsResponse`), и профиль после отвязки дочитывается уже с ним.
  Затем очищается локальный vk_id и завершается сессия VK SDK. Сборки, которые
  поле не читают (≤ 3.0.6), после отвязки получают 401 → «Сессия истекла». Ошибка
  отвязки — snackbar (`vkLinkMessage`).
- Вход и привязка через VK подтверждаются VK access-токеном, ошибки разложены по
  кодам `detail` (см. §18.2, «VK ID подтверждается access-токеном»): «аккаунта с
  этим VK нет» — это отдельное состояние (диалог «Аккаунта с этим VK нет» →
  «Создать»/«Отмена»), а не ошибка пароля; «VK уже привязан к другому аккаунту»
  показывается текстом, а не молчаливым логом.
- **Сессия VK SDK на устройстве** (не влияет на авторизацию в «Машинисте»): при
  наличии локального vk_id Android обновляет токен SDK и загружает имя/фото. Нет
  сессии SDK — показывается OneTap, vk_id не стирается; refresh-токен VK отозван
  (`RefreshTokenExpired` / `invalid_grant`) — локальный vk_id стирается. iOS —
  аналог VK ID SDK для iOS (❓ не реализовано).
- Токены/пароли/email/vk_id не выводятся в логи (правило 5 в CLAUDE.md/AGENTS.md).
  ⚠️ Поведение Android (возможный баг, BUG-54): `ProfileViewModel.startMigration`
  пишет в `Log.e` bearer-токен, `userId` и содержимое маршрутов/настроек при ошибках
  миграции (нарушение правила).

### 18.5. `SyncProgressDialog` и миграция (бывш. §31.3)

- `SyncProgressDialog` — модальный `AnimationDialog`. Для полной синхронизации
  (`SyncType.Sync`): заголовок «Синхронизация данных», название текущего этапа,
  один линейный progress bar и процент. Этапы по порядку (ключ → подпись):
  `UserSettings` «Настройки пользователя» → `SalarySettings` «Настройки зарплаты» →
  `ReleaseDays` «Отвлечения» → `Routes` «Маршруты»; до первого завершённого этапа
  подпись «Подготовка», когда все завершены — «Завершено». Процент = завершённые
  этапы / 4 (шаг 25%). Под процентом — текст первой ошибки этапа, если есть.
  Dialog не запускает sync сам, только отображает state VM. (Для легаси-типов
  Upload/Download — построчный список этапов с иконками успех/ошибка, «Синхронизировано
  N из M маршрутов» и «Показать/Скрыть ошибки».)
- **Полный успех** (есть метка времени и нет ошибок маршрутов): прогресс
  закрывается и показывается `AlertDialog` с галочкой «Синхронизация завершена!» /
  «Данные успешно синхронизированы.» / «Отлично!»; метка сохраняется как «последняя
  синхронизация», профиль перечитывается.
- **Частичный результат/ошибки этапов**: диалог прогресса остаётся с отмеченными
  ошибками; ошибка маршрутов — «синхронизировано с ошибками». Появляется кнопка
  **«Отправить отчет об ошибке»** (только при не-сетевых ошибках) → текстовый файл
  «Отчет об ошибках синхронизации» (тип, ID пользователя, ошибки по этапам и
  маршрутам) отправляется письмом на `locodriver.app@yandex.ru` через chooser
  «Отправить отчет» (fallback — `mailto:` с темой). ⚠️ Поведение Android (возможный баг,
  BUG-55): для полной синхронизации в отчёте тип пишется как «Загрузка с
  сервера» (подпись выбирается только между Upload и остальным).
  В завершённом состоянии (`isSyncComplete`) внизу кнопка **«Понятно»** →
  `resetSyncState`. Тап вне/Back тоже вызывают `resetSyncState` — в том числе
  во время идущей синхронизации (диалог скрывается, сама операция продолжается).
- **Нет интернета** (транспортная ошибка): отдельный вид — иконка отключённого
  сигнала, «Нет интернета», «Понятно». Синхронизация дальше не продолжается.
- **Сессия истекла** (`isSessionExpired`): сервер ответил 401 на запрос с
  bearer-токеном (`detail` «Invalid creadential»; при регулярном запуске токен
  продлевается — §18.3). Диалог показывает отдельный экран «Сессия истекла»
  с текстом «Войдите в аккаунт заново в разделе „Профиль“. Данные на устройстве
  сохранены и будут синхронизированы после входа» и кнопкой «Понятно» — без
  строк по шагам и без кнопки «Отправить отчёт»: причина одна и лечится
  только повторным входом. Синхронизация прерывается на первом 401
  (`SyncManager.abortOnSessionExpired`), а не перебирает все маршруты.
  Одновременно `SessionExpiredHandler` закрывает сессию как `logOut`
  (чистит токен, vk id, курсор дельта-синхронизации) — с любого экрана, где
  случился 401 (главный, профиль, фоновый sync, pull-to-refresh, воркер), а
  не только при загрузке профиля. Текст «Сессия истекла. Войдите в аккаунт
  заново.» показывает тот экран, где случился 401: тихий фоновый sync и
  pull-to-refresh — своим обычным сообщением об ошибке, профиль — snackbar;
  сам handler сообщений не показывает, чтобы не дублировать. 401
  `vk_token_invalid` при привязке VK сессию не закрывает. Экран «Профиль»,
  если открыт, реагирует на пропажу токена из хранилища и переходит в
  состояние гостя.
- Подтверждения массового удаления маршрутов в Профиле **нет**: поле
  `pendingDeletionRouteIds` в результате синхронизации никогда не заполняется
  (серверные удаления сразу уходят в корзину, §18.2, §9.4), связанный
  `AppAlertDialog` в Профиле не реализован.
- Кнопка «Синхронизация» не блокируется, но проверяет подписку и токен; повторный
  запуск во время идущей синхронизации ждёт общий mutex `SyncManager` (§18.2). После
  завершения пользователь может закрыть и повторить.
- **Легаси-миграция «на отечественный сервер»** (только для старых установок:
  флаг первого запуска уже сброшен и `isMigrated == false`; новым установкам
  `isMigrated` ставится при первом запуске):
  - вместо профиля показывается форма: логотип «М», «Вход в Машинист», «Пожалуйста,
    введите ваш email или войдите с VK ID.», кнопка VK OneTap, «или», поле email;
    при валидном email появляется поле пароля (с глазом, «Минимум 4 символа») и
    подсказка «Если не помните ваш пороль, можете указать новый или использовать VK
    ID»; при валидных email и пароле ≥ 4 появляется кнопка «Далее»;
  - «Далее»/VK → **регистрация** нового аккаунта (email+пароль или VK) → смена
    аккаунта → перенос: экран «Переносим данные на отечественный сервер», текущий
    шаг, два прогресс-бара: «Загружено N из M» (маршруты, по одному с паузой 400 мс)
    и «Настройки: P%» (UserSettings с легаси-сроком подписки → 33, SalarySetting →
    66, дни отвлечений → ⚠️ 1.0 вместо 100 — BUG-56);
  - при любом исходе (успех или ошибка) миграция помечается выполненной
    (`isMigrated=true`) и больше не предлагается; итог — snackbar «Данные успешно
    перенесены на новый сервер» или «Ошибка переноса данных: …».
    ⚠️ Поведение Android (возможный баг, BUG-56): экран успеха «Данные успешно
    перенесены!» с кнопкой «Продолжить работу» практически недостижим — результат сразу
    сбрасывается `completeMigration()`; при ошибке миграция помечается выполненной, повторить
    перенос нельзя.
  - Локальные данные при миграции не удаляются. Для iOS — не переносить (новых
    установок это не касается).

### 18.6. Шторки ввода: `AppInputBottomSheet` и `AppEmailPasswordBottomSheet` (бывш. §31.4)

Стандарт `docs/DIALOGS_STANDARD.md`: всё, что требует ввода, — нижняя шторка,
а не диалог.

- **`AppInputBottomSheet(title, initialValue, onConfirm, confirmText="Сохранить",
  hint?, label?, suffix?, keyboardType, singleLine=true, transform, isValid,
  errorText?, isLoading, onValueChange?)`**: `ModalBottomSheet`, верхний радиус
  28dp, grabber, фон `surface`, отступ под клавиатуру. Сверху вниз: заголовок
  (20sp, W600), необязательная подсказка (14sp), `OutlinedTextField` (радиус 12dp,
  акцентная рамка в фокусе, label/suffix по параметрам; при `errorText` — красная
  рамка и текст ошибки под полем 12sp), первичная кнопка во всю ширину (радиус
  14dp, фон акцентный при `canConfirm`, иначе `surfaceVariant`; при `isLoading` —
  спиннер вместо текста), под ней серая «Отмена» (закрывает). Начальное значение
  **выделено целиком**, поле сразу получает фокус. `transform` применяется к
  каждому вводу (при изменении текста курсор — в конец). `canConfirm =
  isValid(text) && !isLoading`; IME-действие «Done» подтверждает только при
  `canConfirm`. Свайп/тап вне/Back/«Отмена» → `onDismissRequest`.
- **`AppEmailPasswordBottomSheet(title, onConfirm(email, password),
  confirmText="Сохранить", hint?, isEmailValid, minPasswordLength=4, errorText?,
  isLoading)`**: тот же контейнер и кнопки; два поля — «Почта» (клавиатура email,
  IME «Next», **пробелы по краям обрезаются при вводе**, фокус сразу) и «Пароль»
  (скрыт, кнопка-глаз «Показать/Скрыть пароль», IME «Done»). Под паролем всегда
  подсказка «Не короче {min} символов», при ошибке вместо неё — `errorText`
  красным (обе рамки красные). `canConfirm = isEmailValid(email) &&
  password.length ≥ min && !isLoading`. Поля стартуют пустыми.
- `ConfirmEmailDialog` — легаси-обёртка над `AppInputBottomSheet`; 🚫 мёртвый код Android (§22).

Источник: `features/route/.../ui/ProfileScreen.kt`, `viewmodel/ProfileViewModel.kt`,
`ui/SyncProgressDialog.kt`, `component/AppInputBottomSheet.kt`,
`component/AppEmailPasswordBottomSheet.kt`, `component/ConfirmEmailDialog.kt`,
`data_remote/.../remote_rest/AuthManager.kt` (`addEmailErrorMessage`,
`attachErrorMessage`, `vkAuthError`), `remote_rest/SessionRefresher.kt`,
`remote_rest/NetworkErrorMapper.kt`, `remote_rest/SyncManager.kt`,
`app/.../StartApp.kt`.

### 18.R. Экран «Пригласить друга» (ReferralScreen) — реферальная программа

**Вход.** Строка «Пригласить друга» / «Получите бесплатные дни обслуживания»
(иконка подарка, шеврон) в группе «ПОДПИСКА» Профиля, сразу под карточкой
«Машинист Pro» (§18.1). На Android это не nav-destination, а флаг внутри
`ProfileScreen` (`showReferrals`, переживает поворот); системный Back/стрелка
возвращают в Профиль. Доступен только авторизованному пользователю (строка есть
только в авторизованном Профиле).

**Данные.** `GET /v1/referrals/me` (Bearer) → `ReferralStatusResponse`:
`code: String` (личный код), `canApplyCode: Boolean`, `canInvite: Boolean = false`,
`appliedCode: String?`, `appliedStatus: String?` (`pending`/`rewarded`/`reversed`),
`invitedCount: Int` (все применившие мой код), `rewardedCount: Int` (оплатившие,
бонус не отменён), `awardedDays: Int` (все подаренные мне дни, исторический счётчик,
не убывает), `remainingBonusDays: Int` (сколько из оставшихся дней подписки —
бонусные; обнуляется, если подписка когда-либо истекла). Код создаёт сервер; он не
содержит email/VK ID/JWT и не хранится в локальной БД. Запрос выполняется при
**каждом** открытии экрана (не только при первом создании VM).

**UI сверху вниз.**
1. Top bar: стрелка «назад» слева, заголовок **«Пригласить друга»**, прозрачный фон.
2. Hero-иллюстрация: локальная анимированная SVG `referral_v3_animated.svg`
   (пропорции 360:334, скруглённые углы). Цвета SVG задаются CSS-переменными
   светлой/тёмной палитры (копия токенов PWA) по текущей теме приложения.
   Смысл: «друг впервые оплачивает год Про → каждому ≈ 6 месяцев» — это пример
   правила половины периода, а не фиксированный размер бонуса.
   Android-механика: WebView без JavaScript, без файлового/контент-доступа, с
   запретом сетевых загрузок; HTML грузится с base URL
   `https://appassets.androidplatform.net/` (чтобы работали ссылки `#id` внутри
   SVG), WebView уничтожается при уходе с экрана. iOS — любым нативным способом
   показа SVG/анимации.
3. Блок статуса (взаимоисключающие состояния):
   - загрузка и данных ещё нет → карточка 150dp со спиннером;
   - ошибка и данных нет → карточка цвета ошибки с текстом **«Не удалось загрузить
     код»** и кнопкой «Повторить» (иконка sync) → повторный запрос;
   - данные есть → контент ниже (при повторной ошибке старые данные остаются).
4. Контент при `canInvite == false`: жёлтая плашка (фон `#FFF0BD`, текст/иконка
   `#593D00`/`#805800`, иконка короны) «Приглашения после первой оплаты» /
   «Реферальная программа доступна для приглашающих, которые уже хотя бы раз
   оплачивали Про. После вашей первой оплаты здесь появится код.» Код и кнопки не
   показываются. Это успешный ответ, а не ошибка.
5. Контент при `canInvite == true`: карточка кода — подпись «ВАШ КОД», сам код
   моноширинным `headlineMedium` жирным + иконка копирования справа; **тап по всей
   строке кода копирует код** в буфер (отдельной кнопки «Копировать» нет, своего
   тоста/snackbar Android не показывает). Ниже кнопка на всю ширину
   **«Поделиться кодом»** (иконка share) → системный share-sheet с текстом:
   «Попробуй «Машинист Про». Введи мой код {code} перед первой оплатой — после
   оплаты каждому добавят половину оплаченного периода.»
6. Две карточки-метрики в ряд (при любом `canInvite`):
   **«Приглашено»** = `rewardedCount` (только оплатившие друзья, НЕ `invitedCount`)
   и **«Начислено»** = «`awardedDays` дн.».
7. Блок «Как это работает»: пояснение «Приглашать может только тот, кто уже хотя бы
   раз оплачивал Про. Друг может пользоваться Android или iOS — код работает
   одинаково на обеих платформах.»; карточка из трёх шагов (номер в скруглённой
   плашке): 1 «Отправьте код другу» / «Количество приглашений не ограничено.»;
   2 «Друг вводит код» / «Применить код можно до его первой оплаты Про.»;
   3 «Получите бонус вместе» / «После оплаты каждому добавится половина оплаченного
   периода.»; под карточкой — «При возврате платежа неиспользованный бонус
   отменяется у обоих участников.»
8. Текстовая кнопка **«Подробные правила программы ›»** → внешний браузер
   `https://locodriver.ru/referral.html`.

На реферальном экране Android статус применённого **чужого** кода не показывается —
он виден на экране «Покупки» (§19.R).

**Правила программы (серверные, для всех платформ).**
- Приглашённый может быть зарегистрирован заранее, но должен применить чужой код
  **до своей первой оплаты**; код закрепляется за аккаунтом, второй код применить
  нельзя, свой — нельзя. За повторную покупку бонуса нет.
- Приглашающий должен иметь хотя бы одну подтверждённую собственную оплату Про
  (`UserFirstPayment`), активность текущей подписки не обязательна; бесплатный бонус
  без оплаты права приглашать не даёт. Сервер проверяет это и при принятии кода,
  и при начислении.
- После подтверждения первой оплаты приглашённого **каждый** участник получает
  ровно половину срока оплаченного тарифа (31 день → 15 дней 12 часов). Срок «Про»
  меняет только сервер: по записи начисления на участника + обновление
  `user_settings.subscriptionPeriod` как `max(срок, сейчас) + бонус`. Клиент
  узнаёт новый срок при обновлении настроек; о начислении сообщает глобальный
  диалог «Бонус начислен!» (§19.5).
- Общего лимита приглашений нет. При возврате первой оплаты бонус отменяется у обоих:
  сервер вычитает не более оставшейся части бонуса и не переносит конец подписки в
  прошлое; статус `reversed`. Админ-кабинет `/admin/referrals` показывает приглашения
  и начисления.
- Реферальные endpoint'ы отдельные; ответы авторизации и настроек не менялись.

**iOS (текущее состояние).** Переходы «Машинист Про» и «Реферальная программа» —
системные `Label`; экран в стиле `List`/`Section`: hero с системной иконкой,
моноширинный код с `ShareLink`, `LabeledContent` для статистики, три нумерованных
шага, inline-заголовок. **PWA**: `/profile/referrals`, та же логика и та же SVG через
`img` (360:320).

Источник: `features/route/.../ui/ReferralScreen.kt`,
`viewmodel/ReferralViewModel.kt`, `ui/ProfileScreen.kt` (строка «Пригласить друга»),
`data_remote/.../response/ReferralResponse.kt`, `features/route/src/main/assets/referral_v3_animated.svg`.

---

## 19. Экран «Покупки» / подписка (PurchasesScreen)

Оформление и продление платной подписки «Машинист Pro». Открывается только через
гейт покупок (§3.2: без токена — диалог «Нужен вход в аккаунт»). Не вкладка:
открывается поверх текущего экрана, закрывается крестиком слева в app bar
(`router.back()`).

### 19.2. Состав экрана и состояния

**Состояние подписки** берётся из локального `UserSettings.subscriptionPeriod`
(реактивно; пока не прочитан — `isSubscriptionLoaded=false`):
- `Paywall` — `subscriptionPeriod == 0` (не оформлялась никогда);
- `Active(endTime, daysLeft)` — `subscriptionPeriod > now`,
  `daysLeft = ceil((endTime − now) / 1 сутки)`;
- `Expired(endTime)` — `0 < subscriptionPeriod ≤ now`.
Грейс-периода здесь нет (в отличие от гейта создания маршрута, §19.1).

**Тарифы** — `GET /v1/tariffs` (§19.0), сортировка по цене к оплате по убыванию
(обычно год → квартал → месяц); **по умолчанию выбран первый** (самый дорогой).
Пока грузятся — спиннер 160dp на месте списка.

**App bar**: слева крестик «Закрыть»; справа текстовая кнопка **«Восстановить»**
только при `Paywall`/`Expired` (и загруженном статусе) → восстановление покупок
(§19.3). Контент прокручивается; pull-to-refresh = полная синхронизация +
перезагрузка тарифов.

**Контент сверху вниз по состояниям:**
- статус ещё не загружен → hero «Машинист Pro» + спиннер (без «неактивной» шапки).
- `Paywall` → карточка-витрина: hero (тёмный фон `primary`, иконка короны,
  «Машинист Pro», «ПОЛНАЯ ВЕРСИЯ», «Облако, экспорт и безлимит истории. Все поездки
  под рукой и в безопасности.») + три преимущества с иконками: «Облачная копия и
  синхронизация», «Экспорт в PDF», «Безлимит маршрутов и истории»; затем
  «ВЫБЕРИТЕ ТАРИФ» и список `PlanCard`.
- `Active` → hero; зелёный баннер «Подписка активна» / «Действует до **dd.MM.yy**»;
  если `referralStatus.remainingBonusDays > 0` — мелко «Из них N дн. — бонус по
  реферальной программе»; если `daysLeft ∈ 0..7` — жёлтый баннер «Подписка истекает
  через N день/дня/дней» / «Продлите заранее — новый срок прибавится к текущему,
  дни не сгорят.»; затем «ПЕРИОД ПРОДЛЕНИЯ»: карточка перехода дат «СЕЙЧАС ДО
  dd.MM.yy → «+ 1 месяц»/«+ 3 месяца»/«+ 1 год» → СТАНЕТ ДО dd.MM.yy» и список тарифов.
  Дата «станет до» = текущий срок **+ календарные месяцы** (1/3/12 по названию
  тарифа) в поясе пользователя. ⚠️ Поведение Android (возможный баг, BUG-57): сервер начисляет
  `periodDays` (31/93/365), а превью прибавляет календарные месяцы — даты могут
  расходиться на 1–2 дня; число месяцев определяется разбором
  названия тарифа («год» → 12, начинается с «3» → 3, иначе 1).
- `Expired` → hero; красный баннер «Подписка истекла» / «Закончилась **dd.MM.yy**»;
  плашка «Маршруты и история сохранены. Но добавлять новые и пользоваться
  синхронизацией нельзя, пока подписка не возобновлена.»; «ВЫБЕРИТЕ ТАРИФ» и список.
- Под тарифами (когда статус и тарифы загружены и тариф выбран) — блок
  **автопродления** (§19.A).
- Ниже при любом состоянии — блок реферального кода (§19.R).

Даты — формат `dd.MM.yy` в поясе отображения пользователя (§0.1).

**Закреплённая снизу CTA** (показывается, когда статус и тарифы загружены и тариф
выбран) — кнопка на всю ширину 54dp + подпись под ней:

| Состояние | Текст кнопки | Подпись |
|---|---|---|
| `Paywall` | «Оформить за {sum} ₽/мес» (месяц), «…₽/год» (год), «Оформить за {sum} ₽» (квартал) | «Первые 20 маршрутов — бесплатно. Дальше добавление маршрутов — только по подписке.» |
| `Active` | «Продлить · {sum} ₽» | «Новый период прибавится к текущему сроку.» |
| `Expired` | «Возобновить подписку» | «Возобновление начнётся с сегодняшнего дня.» |

`{sum}` — итоговая цена целым числом. Нажатие запускает оплату выбранного тарифа
(§19.3). Пока сервер создаёт платёж, кнопка неактивна — повторный тап не создаёт
второй платёж (закрывает BUG-58).

Источник: `features/route/.../ui/PurchasesScreen.kt`,
`viewmodel/PurchasesViewModel.kt`, `navigation/PurchasesDestination.kt`,
`navigation/PurchasesEntry.kt`.

### 19.A. Автопродление (рекуррентные платежи Robokassa)

Подключается галочкой при оплате; дальше сервер сам списывает стоимость за сутки до
конца периода без участия пользователя. Условия — раздел 5 оферты
(https://locodriver.ru/offer.html).

- **Условия/текст согласия** — `GET /v1/payment/recurring/terms?tariff_code=…` (без
  авторизации) → `{ available, tariff_code, amount, renewal_amount, period_days,
  consent_version, consent_text, offer_url }`. Грузится при выборе тарифа; ответ,
  пришедший для другого (уже не выбранного) тарифа, отбрасывается. Ошибка — блок не
  показывается.
- **Статус** — `GET /v1/payment/recurring` (Bearer) → `{ available, enabled,
  tariff_code, amount, next_charge_at (ms), last_charge_at, last_charge_status
  ("paid"|"failed"), last_error, disabled_reason ("user"|"charge_failed") }`.
  Перечитывается при открытии экрана и после возврата из оплаты.
- **`enabled == true`** → карточка «Автопродление включено»: значок, «Следующее
  списание dd.MM.yy · {amount} ₽» (без даты — «{amount} ₽ за период»); при
  `last_charge_status == "failed"` — красная строка «Последнее списание не прошло —
  повторим через сутки.»; справа кнопка **«Отключить»** (красный акцент). Чекбокс не
  показывается.
- **Иначе, если `terms.available`** → карточка с чекбоксом «Автопродление»
  (**по умолчанию не отмечен**; тап по карточке переключает) и текстом согласия
  `consent_text` **без изменений**. Адрес оферты в тексте `(https://…)` убирается,
  ссылкой на оферту становится слово перед ним («оферты»). Галочка снимается при
  смене тарифа (согласие дается на его сумму) и при любом изменении статуса
  автопродления (после отключения чекбокс снова не отмечен).
- `disabled_reason == "charge_failed"` → пояснение «Автопродление отключено: несколько
  списаний подряд не прошли. Продлите подписку вручную — при оплате его можно включить
  снова.»
- **Отключение**: «Отключить» → диалог по стандарту (`AppAlertDialog`, деструктивный):
  «Отключить автопродление?» / «Подписка продолжит действовать до dd.MM.yy. Дальше
  деньги списываться не будут — продлить можно будет вручную.» (фраза с датой — только
  при сроке в будущем) / «Оставить» · «Отключить» (во время запроса — «Отключаем…»).
  `POST /v1/payment/recurring/disable` (Bearer) → статус; snackbar «Автопродление
  отключено» или «Не удалось отключить автопродление. Попробуйте ещё раз.» Оплаченный
  период сохраняется.
- **Суммы** (правило сервера): первый платёж — цена тарифа с акцией, автосписания —
  стандартная цена без акции на момент оформления (при акции `consent_text` называет
  обе суммы); цена автосписаний не меняется при изменении тарифов. Первый платёж с
  галочкой принимается **только банковской картой** (на странице Robokassa нет
  «Другой способ»). Автосписания в реферальной программе не участвуют (§18.R).
- iOS — те же правила и эндпоинты. PWA (`/payment`) — реализовано.

Источник: `ui/PurchasesScreen.kt` (`AutoRenewSection`, `AutoRenewConsentCard`,
`AutoRenewStatusCard`), `viewmodel/PurchasesViewModel.kt`,
`data_remote/.../response/RecurringResponses.kt`.

### 19.R. Реферальный код перед первой оплатой

- Статус берётся из `GET /v1/referrals/me` при открытии экрана (§18.R). Ошибка
  запроса молча скрывает блок (поле не показывается, сообщения нет).
- Если `canApplyCode == true` — карточка ввода (акцентная рамка, иконка звезды):
  «Есть код друга?» / «Введите его до первой оплаты» / «После оплаты каждый получит
  половину срока выбранного тарифа дополнительно.»; поле «Реферальный код»
  (плейсхолдер «Например, ABCD2345»): ввод приводится к верхнему регистру, остаются
  только буквы/цифры, максимум **16** символов; кнопка на всю ширину «Применить код»
  (активна при непустом коде; во время запроса — спиннер и «Проверяем код…», поле
  заблокировано).
  - `POST /v1/referrals/apply` `{ "code": "<код>" }` (Bearer). Успех → зелёный текст
    «Код принят. Бонус начислится после первой оплаты.» и перечитывание статуса
    (карточка ввода сменится статусной). Любая ошибка (свой/неизвестный/второй код,
    сеть) → красный текст «Не удалось применить код. Проверьте его и попробуйте
    снова.» (причину UI не детализирует).
- Иначе, если `appliedCode != null` и `appliedStatus == "pending"` — статусная
  карточка «Код {code} применён» / «При первой оплате вам и другу начислится по
  половине оплаченного периода. Бонус можно получить только один раз — при первой
  оплате.» Для `rewarded`/`reversed` карточки нет (о начислении сообщает диалог
  «Бонус начислен!», остаток бонуса — строка «Из них N дн.»).
- Право ввода определяет только сервер (`canApplyCode`): нет отметки первой оплаты
  и ранее применённого кода. Старые аккаунты с ненулевым `subscriptionPeriod`
  отмечены платившими при миграции; бонусный срок без оплаты права ввода не
  отнимает, поэтому поле показывается и при активном бесплатном сроке.
- Применение кода ничего не начисляет сразу: начисление — в серверном callback
  первой оплаты (Robokassa/CKassa); платёж без кода — прежнее поведение.
- iOS: системная секция, моноширинное поле, `borderedProminent`-кнопка; после
  применения — статусная секция. PWA: `/payment`, те же правила ввода.

### 19.3. Оплата: пользовательский сценарий, серверный контракт, платформенная механика

**Пользовательский сценарий (одинаков для всех платформ).**
1. Пользователь авторизован (гейт §3.2), выбирает тариф, жмёт CTA.
2. Клиент запоминает «ожидаемую оплату» (`userId`, момент ухода в оплату, срок до
   оплаты, `periodDays` тарифа) — §19.6.
3. Пользователь проходит оплату на странице **Robokassa** (ссылку формирует сервер,
   §19.3 «Серверный checkout»; CKassa — резервный путь, только Android).
4. Срок подписки **начисляет только сервер** (по уведомлению провайдера): новый срок =
   `max(текущий срок, момент оплаты) + periodDays тарифа`; при первой оплате с
   применённым реферальным кодом — ещё и бонусы обоим (§18.R).
5. Клиент при возврате поллит сервер (§19.6) и при росте срока показывает глобальный
   «Платёж принят!»; иначе на экране Покупок — «Платёж обрабатывается» или «Оплата не
   завершена» (§19.4).
6. «Восстановить» в любой момент перечитывает срок с сервера.

**Восстановление покупок** («Восстановить»): snackbar «Начинаем поиск...» →
`GET /v1/user_settings/`. Серверный срок в будущем → записывается локально,
snackbar «Данные об оплате обновлены. До dd.MM.yy HH:mm»; срок `0` → «Не найдено
информации об оплате»; срок в прошлом → «Срок оплаты истек dd.MM.yy HH:mm» (локально
не пишется); ошибка сети → «Ошибка связи с сервером. Напишите в поддержку.»; без
токена → «Неавторизованный пользователь».

**Серверный контракт (платформенно-независимый).**
- `GET /v1/tariffs` (без авторизации) — §19.0.
- `GET /v1/user_settings/` (Bearer) → поле `subscriptionPeriod` (ms epoch; `0` — не
  оформлялась). Единственный источник срока; сервер клиентский срок не принимает
  (§19.5).
- `GET /v1/users/me` (Bearer) → `user.id` — идентификатор, к которому сервер
  привязывает оплату.
- `GET /v1/referrals/me`, `POST /v1/referrals/apply` — §18.R, §19.R.
- **Серверный checkout Robokassa** — `POST /v1/payment/checkout` (Bearer)
  `{ "tariff_code": "month|quarter|year", "auto_renew": false, "consent_version": null,
  "platform": "android|ios|pwa" }` → `{ "payment_url": "https://auth.robokassa.ru/…",
  "invoice_id": 123 }`. Цену (со скидкой), срок, чек и подпись формирует сервер;
  пароли мерчанта на клиенте не нужны. `shp_user_id` берётся из JWT, `shp_tariff_code` —
  из тарифа, `shp_source` — из `platform` (по нему страница возврата
  `locodriver.ru/payment-success.html` ведёт в PWA или предлагает вернуться в
  приложение). С `auto_renew: true` + `consent_version` сервер записывает согласие и
  выставляет первый платёж серии автопродления (§19.A); `409` — автопродление
  недоступно или текст согласия обновился: клиент перечитывает условия, снимает
  галочку, snackbar «Условия автопродления обновились. Проверьте их и нажмите оплату
  ещё раз.» Старые клиенты (без полей автопродления) получают прежнюю разовую оплату.
- Срок начисляется по уведомлению Robokassa по `shp_tariff_code` (§19.05).
- `POST /v1/payment/ckassa/checkout` (Bearer) `{ "tariff_code": "month|quarter|year",
  "platform": "android" }` → `{ "payment_url": "...", "tariff_code": "..." }` — только
  резервный Android-путь; цену и срок сервер берёт по коду тарифа (со скидкой).
- Автопродление: `GET /v1/payment/recurring/terms`, `GET /v1/payment/recurring`,
  `POST /v1/payment/recurring/disable` — §19.A.

**Android-механика.** Оплата — через серверный checkout (`platform: "android"`):
`payment_url` открывается во внешнем браузере (та же страница Robokassa, что в PWA).
Встроенный Robokassa SDK и пароли мерчанта в клиенте больше не используются (модуль
`robokassa_sdk` остаётся в репозитории как легаси, приложение от него не зависит).
CKassa — резервный путь, выбор — константой `PurchasesViewModel.USE_CKASSA` (сейчас —
Robokassa; статус в `docs/PAYMENT_PROVIDERS.md`).
- После возврата в приложение (`ON_RESUME` экрана Покупок) — поллинг сервера 5×3 с
  (плюс глобальная проверка §19.6) и перечитывание статуса автопродления.
- Ошибки запуска: тариф без кода → snackbar «Ошибка: Тариф недоступен для оплаты»;
  страница не открылась → «Не удалось открыть страницу оплаты»; `409` checkout — §19.3
  (серверный контракт); прочие → «Ошибка: {текст}».
- `userId` для оплаты — сохранённый `user.id`; если пуст — дочитывается из
  `GET /v1/users/me`; не удалось → snackbar «Ошибка: Отсутствует User ID», оплата не
  запускается.
- VK ID SDK к оплате не относится (только вход, §18).

**iOS.** Оплата — через **Robokassa**. App Store In-App Purchase и Apple Pay в РФ не
работают и не используются.
- Сценарий шагов 1–6, экран §19.2, диалоги §19.4 и глобальные диалоги §19.5–19.6 — как на
  Android.
- **Логин и пароли мерчанта Robokassa в iOS-клиенте запрещены.** Подпись платежа формирует
  **сервер**: `POST /v1/payment/checkout` с `platform: "ios"` (контракт выше), клиент
  только открывает `payment_url` (SFSafariViewController / ASWebAuthenticationSession).
  Автопродление — §19.A.
- Срок подписки, как и на Android, начисляет только сервер; клиент узнаёт о нём поллингом
  (§19.6).

Источник: `viewmodel/PurchasesViewModel.kt`, `ui/PurchasesScreen.kt`,
`subscription/PaymentReturnChecker.kt`,
`data_remote/src/androidMain/.../use_case/SubscriptionHelper.kt`,
`data_remote/.../request/RobokassaCheckoutRequest.kt`,
`data_remote/.../response/RobokassaCheckoutResponse.kt`,
`data_remote/.../request/CkassaCheckoutRequest.kt`,
`data_remote/.../response/CkassaCheckoutResponse.kt`.

### 19.0. Цены и скидки (единый источник — сервер)

Цены и скидки задаются в **кабинете администратора** (`/admin/tariffs`) и хранятся
в таблице `subscription_tariff` на сервере. Один источник правды для **приложения
и сайта** — публичный `GET /v1/tariffs` (без авторизации, CORS открыт).

- **JSON ответа**: `{ "currency": "RUB", "tariffs": [ { "code", "title", "desc",
  "periodDays", "basePrice", "price", "discountPercent", "discountActive",
  "discountUntil" } ] }`. Маппинг в доменный `Product`: `title → name`,
  `price → sum`, `discountPercent` берётся только при `discountActive`, иначе `0`.
- **Модель тарифа** (`Product`): `code` (стабильный идентификатор: `month` |
  `quarter` | `year`), `name`/`desc` (заголовок/подпись), `periodDays` (срок),
  `basePrice` (базовая «старая» цена), `sum` (итоговая цена к оплате — со скидкой,
  если активна), `discountPercent`, `discountActive`, `discountUntil` (ms epoch
  окончания акции или null).
- **Скидка** = базовая цена + процент. Итог = `round(basePrice*(1−percent/100))`.
  Активна, если `percent > 0` **и** (`discountUntil` пусто **или** ещё не прошло).
  По истечении даты `discountActive=false` — сервер гасит скидку без крона.
- **Карточка тарифа** (`PlanCard`): слева радио-кружок 22dp; название тарифа
  (жирное) и под ним «{sum/месяцев, округлено} ₽/мес» (месяцев: 1/3/12 по названию,
  см. §19.2); справа цена. Выбранная карточка — фон `primaryContainer` и рамка
  акцентного цвета 1.5dp. Одиночный выбор, тап выбирает.
- **Акция** (`discountActive && discountPercent > 0`): справа базовая цена
  **зачёркнута** (мелко), под ней крупная итоговая, под ней оранжевый бейдж
  «Акция −N%»; под «₽/мес» оранжевая строка «Действует до dd.MM.yyyy» — только если
  задан `discountUntil`.
- **«Выгода тарифа»** (зелёный бейдж «−N%» рядом с «₽/мес») показывается, только
  если **ни у одного** тарифа нет активной акции:
  `N = round(100 × (1 − perMonth / monthlyBase))`, где `perMonth =
  round(sum / месяцев)`, `monthlyBase` = цена/месяц тарифа с наименьшим числом
  месяцев; бейдж не показывается при `N ≤ 0`.
- **Офлайн/ошибка**: `PurchasesViewModel.fetchTariffsOrDefault()` откатывается к
  дефолтному набору (месяц/квартал/год = 69/179/599 ₽, 31/93/365 дней). Дефолты
  несут `code`, поэтому оплата корректна и офлайн-путём.
- **Валюта**: подписки Robokassa — в ₽ (не зависят от валюты пользователя по
  стране; см. память проекта про валюту).
- Ошибка/офлайн загрузки тарифов пользователю не показывается (только Sentry) —
  сразу подставляются дефолты.

Источник: `viewmodel/PurchasesViewModel.kt` (`fetchTariffsOrDefault`,
`defaultProducts`), `ui/PurchasesScreen.kt` (`PlanCard`, `PlanList`),
`data_remote/.../response/TariffResponse.kt`.

### 19.05. Оплата: срок начисляется по тарифу, не по сумме

- Клиент передаёт `code` тарифа в `shp_tariff_code` (пользовательский параметр
  Robokassa, входит в подпись). `orderSum` = итоговая цена (`Product.sum`).
- **Сервер (webhook Result URL)** берёт `period_days` по `shp_tariff_code` из
  таблицы тарифов — **не по сумме платежа** (подпись Robokassa покрывает все
  `shp_*`). Если оплаченная сумма разошлась с актуальной ценой тарифа (цена сменилась между
  открытием экрана и оплатой) — срок всё равно
  по тарифу, расхождение только логируется.
- **Обратная совместимость**: старые клиенты без `shp_tariff_code` → сервер
  откатывается к легаси-маппингу по сумме (69→31, 179→93, 599→365; иначе 3 дня).
  Не удалять до полного отказа этих версий.
- **Подпись**: сервер собирает все `shp_*` из формы webhook **динамически и
  сортирует** (Robokassa включает их в подпись по алфавиту). Раньше имена были
  захардкожены — добавление `shp_tariff_code` без этого сломало бы подпись.

Источник: `viewmodel/PurchasesViewModel.kt` (`createPaymentParams`); серверная
часть — вне репозитория (❓ по коду клиента не проверена).

### 19.1. Лимит бесплатных маршрутов и гейт синхронизации

Что определяет «активную подписку» и как она ограничивает создание маршрутов и
синхронизацию. Единый источник статуса — `UserSettings.subscriptionPeriod`
(Long, ms epoch; `0` = не оформлена).

- **Активная подписка** = `subscriptionPeriod != 0` **и** не истекла. Для гейта
  создания маршрута истечение считается с грейс-периодом **1 день**
  (`subscriptionPeriod + 24 ч ≥ now`); для гейта синхронизации грейса нет
  (`subscriptionPeriod > now`), т.к. речь о записи на сервер.

- **Лимит бесплатных маршрутов = 20.** Гейт в
  `RouteActionsHelper.newRouteClick` (общий для всех точек создания: «+» в нижней
  навигации, «Все маршруты», копия, Календарь, мастер «Заполнить месяц»). Логика:
  - активная подписка → форма открывается всегда (лимита нет);
  - иначе (не куплена **или** истекла): при `количество_маршрутов ≥ 20` →
    `NeedSubscribeDialog` (форма **не** открывается); при `< 20` →
    `AlertSubscribeDialog` (мягкое предупреждение, создание разрешено).
  - Учитываются все локальные маршруты, включая помеченные удалёнными
    (`listRouteWithDeleting`). Порог одинаков и для «не куплена», и для «истекла».
  - Локальное сохранение отдельно не блокируется — оно недостижимо, пока форма
    не открыта.
  - Ошибка проверки → форма не открывается, сообщения нет (`NewRouteResult.Error`).
  - Диалоги пути «+» в нижнем меню (глобальные, `LocoDriverApp`; у FAB «Все маршруты» — свои
    шторки, §9.0.1):
    - `NeedSubscribeDialog` → `AppAlertDialog` «Бесплатный лимит исчерпан» / «Для
      добавления новых маршрутов оформите подписку.»; «Оформить подписку» → гейт
      покупок (§3.2); «Отмена» — закрыть.
    - `AlertSubscribeDialog(freeRoutesLeft)` → «Пробный период» / «Осталось
      бесплатных маршрутов: N из 20. Оформите подписку для неограниченного
      использования или продолжите бесплатно.»; подтверждение **«Продолжить
      бесплатно»** → новая форма маршрута; вторая кнопка **«Оформить подписку»** →
      гейт покупок. Показывается при **каждом** создании маршрута без подписки,
      пока лимит не исчерпан.
- **Пакетное создание** (Календарь в режиме планирования, мастер «Заполнить
  месяц») использует отдельную проверку `canCreateRoutes(count)`: подписка активна
  (тот же критерий с грейсом 1 день) → разрешено без лимита; иначе
  `left = max(0, 20 − количество_маршрутов_включая_удалённые)`; пачка создаётся
  **только целиком** и только если `count ≤ left`, иначе ничего не создаётся и
  показывается `SubscriptionLimitDialog` (§19.1a). Ошибка проверки → snackbar «Не
  удалось проверить подписку». Частичного создания нет.

- **Синхронизация — платная функция целиком.** Любой upload в облако требует
  активной подписки:
  - фоновый `SyncWorker` — при `subscriptionPeriod < now` завершается без
    синхронизации (`Result.success()`, без ошибки);
  - ручные пути (`HomeViewModel.manualSync`, пер-маршрутный `syncRoute` на
    Главном / «Все маршруты» / Календаре, `ProfileViewModel.startSync`)
    проверяют `RouteActionsHelper.hasActiveSubscription()`; при отсутствии —
    снекбар «Синхронизация доступна по подписке», upload не выполняется;
  - пер-маршрутный `syncRoute` **сначала проверяет авторизацию**: если токена
    нет (`isNullOrBlank`) — снекбар «Войдите в аккаунт, чтобы синхронизировать
    маршрут» и выход **без обращения к сети** (проверка авторизации идёт до
    проверки подписки);
  - раздел «Синхронизация» в Профиле показывается только при активной подписке
    (`ProfileViewModel.hasSubscription`).

- **Периодический WorkManager (36 часов) — только страховочный upload.** Перед
  сетью Worker проверяет `settingsSyncPending` и наличие маршрутов с
  `isSynchronized == false` либо удалений, уже существовавших на сервере. Если
  pending-данных нет, задача завершается без HTTP-запросов. При наличии отправляет
  только локальные изменения/удаления (`syncToRemote(pendingSettingsOnly=true)`),
  не загружает и не применяет серверные данные. При неуспехе возвращает `retry()`;
  серверный pull выполняется при открытии Главного/«Всех маршрутов» или вручную.

- **Карточка «Не синхронизировано маршрутов: N»** на Главном
  (`HomeScreen`) показывается только при **активной подписке** и
  `N > 2`; у неё есть крестик закрытия (скрывает до изменения `N` —
  `remember(unsyncedRoutesCount)`). Без подписки карточка не показывается вовсе
  (иначе сбивала бы с толку: маршруты «не синхронизированы», но синхронизировать
  их нельзя). Флаг — `HomeUiState.hasActiveSubscription`.

- **Карточка «Подписка заканчивается / закончилась»** на Главном — состояния
  повторяют `PurchaseUi.Active/Expired` экрана Покупок:
  - `ExpiringSoon` — подписка ещё активна, но осталось ≤ 7 дней
    (`SUBSCRIPTION_EXPIRING_SOON_DAYS`); текст «Осталось N дней — до <дата>»,
    кнопка «Продлить».
  - `Expired` — подписка была, но истекла (`0 < subscriptionEndTime ≤ now`);
    текст «Закончилась <дата>», кнопка «Возобновить». Если подписки не было
    никогда (`subscriptionEndTime == 0`) — эта карточка не показывается вовсе
    (см. карточку «Бесплатный период» ниже).
  - Крестик закрытия скрывает карточку до смены состояния
    (`remember(subscriptionNotice)`). Источник — `HomeUiState.subscriptionEndTime`.

- **Карточка «Бесплатный период»** на Главном — показывается, пока подписки
  никогда не было (`subscriptionEndTime == 0L`), индикатор использования лимита
  из 19.1 (`RouteActionsHelper.FREE_ROUTES_LIMIT = 20`, `freeRoutesUsedCount` —
  тот же счётчик, что в `newRouteClick`, т.е. с учётом удалённых маршрутов).
  Прогресс-бар `usedCount / 20`. Три состояния по остатку лимита:
  - обычное (остаток > 5) — нейтральная карточка, «Использовано N из 20
    бесплатных маршрутов», кнопка «Оформить подписку»;
  - «подходит к концу» (остаток ≤ 5) — предупреждающий тон
    (`surfaceContainerHigh`, как у `ExpiringSoon`), «Осталось N из 20»;
    подсказка оформить подписку заранее;
  - «лимит исчерпан» (`usedCount ≥ 20`) — тон ошибки (как у `Expired`),
    «Бесплатный лимит исчерпан», подсказка про необходимость подписки для
    новых маршрутов и синхронизации.
  - Крестик закрытия скрывает карточку до следующего изменения счётчика
    (`remember(freeRoutesUsedCount)` — новый маршрут показывает карточку снова).
  - Карточки «Подписка заканчивается/закончилась», «Бесплатный период» и «Не
    синхронизировано маршрутов» листаются одной каруселью (`HomeNoticeCarousel`),
    порядок: Подписка → Бесплатный период → Синхронизация. «Подписка» и
    «Бесплатный период» взаимоисключающие (зависят от `subscriptionEndTime == 0`
    в разные стороны); «Синхронизация» требует активной подписки и может
    показаться одновременно с `ExpiringSoon` (подписка ещё активна, но
    заканчивается).


### 19.1a. Диалог «Не хватает бесплатных маршрутов» (SubscriptionLimitDialog)

Показывается, когда пакет маршрутов (Календарь — «запланировать», мастер
«Заполнить месяц» — «Применить») не помещается в остаток бесплатного лимита
(`BatchRoutesResult.LimitExceeded(requested, remaining)`). Раньше вместо него был
snackbar, которого у этих экранов не было видно, — маршруты «молча» не создавались.

- Вид — `AppAlertDialog`, заголовок **«Не хватает бесплатных маршрутов»**.
- Текст при `remaining == 0`: «Бесплатный лимит исчерпан: использовано 20 из 20.
  Сейчас создаётся {requested} {маршрут|маршрута|маршрутов}. Оформите подписку —
  лимит снимется и включится синхронизация.»
- Текст при `remaining > 0`: «В бесплатном периоде доступно 20 маршрутов, у вас
  осталось {remaining}. Сейчас создаётся {requested} {маршрут|маршрута|маршрутов}.
  Оформите подписку — лимит снимется и включится синхронизация.»
- Склонение «маршрут»: `n%100 ∈ 11..14` → «маршрутов»; `n%10 == 1` → «маршрут»;
  `n%10 ∈ 2..4` → «маршрута»; иначе «маршрутов».
- Кнопки: **«Оформить подписку»** → закрыть диалог и перейти через гейт покупок
  (§3.2); **«Отмена»** / тап вне / Back → закрыть. Ни один маршрут пакета не создан;
  в мастере флаг сохранения сбрасывается, введённый цикл остаётся.

Источник: `features/route/.../ui/SubscriptionLimitDialog.kt`,
`viewmodel/RouteActionsHelper.kt` (`canCreateRoutes`, `freeRoutesLeft`,
`newRouteClick`, `hasActiveSubscription`, `FREE_ROUTES_LIMIT`),
`viewmodel/CalendarViewModel.kt` (`createPlannedRoutes`, `SubscriptionLimitState`),
`viewmodel/ScheduleWizardViewModel.kt`, `ui/CalendarScreen.kt`,
`ui/ScheduleWizardScreen.kt`, `app/.../ui/LocoDriverApp.kt` (диалоги «+»),
`data_remote/src/androidMain/.../work_manager/SyncWorker.kt`, `ui/HomeScreen.kt` (карточки).

### 19.4. Покупки и `PaymentDialog` (бывш. §32.1)

- Состояния экрана, тарифы и CTA — §19.2, §19.0 (без выбранного тарифа CTA не
  показывается; кнопка не блокируется — BUG-58).
- **`PaymentDialog(title, body?, iconRes?, iconTone?, dismissible=true,
  primaryLabel?, secondaryLabel?, showSpinner=false)`** — общий вид диалогов оплаты
  и подписки: центрированная карточка (радиус 22dp, фон `surface`, рамка
  `outlineVariant`, отступ 22dp); сверху круг 56dp с иконкой цвета `iconTone` на
  фоне того же цвета с alpha 0.12; заголовок жирный по центру; текст
  `onSurfaceVariant` по центру; при `showSpinner` — спиннер; первичная кнопка во всю
  ширину 50dp (радиус 14dp, фон `tertiary`, текст цвета `surface`); вторичная —
  обводка 48dp. `dismissible=false` запрещает закрытие Back'ом и тапом вне.
- После возврата со страницы оплаты (Robokassa или CKassa), если экран Покупок
  дожил:
  - **«Получаем данные…»** / «Проверяем оплату на сервере — это займёт несколько
    секунд.» — недисмиссируемый, со спиннером, на всё время поллинга;
  - **«Оплата не завершена»** (иконка тревоги, цвет ошибки) / «Данные об оплате не
    получены. Если у вас есть вопросы, напишите в поддержку.» — кнопки «Написать в
    поддержку» (письмо на `locodriver.app@yandex.ru`, тема «Вопрос по оплате») и
    «Закрыть». Показывается, если оплата не подтвердилась;
  - **«Платёж обрабатывается»** (иконка часов) / «Robokassa подтвердила платёж, но
    сервер ещё не обновил подписку. Нажмите «Восстановить» через несколько минут.» —
    «Понятно». Показывается, если клиент получил подтверждение успеха от провайдера,
    а сервер за 10×3 с срок не обновил. При оплате в браузере (текущая схема Android
    и PWA) клиент такого подтверждения не получает — диалог не показывается;
  - **«Платёж принят!»** — **глобальный**, см. §19.6 (если подтверждение пришло
    раньше — экранных диалогов нет).
  Второй запуск проверки, пока идёт первая, игнорируется.


Источник: `features/route/.../ui/PurchasesScreen.kt` (`PaymentDialog`),
`viewmodel/PurchasesViewModel.kt` (`checkPaymentOnServer`).

### 19.5. Глобальные диалоги «Бонус начислен!» и «Подписка продлена» (бывш. §32.1a)

Два **разных** события, оба всплывают **на любом экране** (Android:
`SubscriptionNoticesViewModel`, Activity-scoped в `LocoDriverApp` вне NavHost;
вид — `PaymentDialog`):

- **«Бонус начислен! 🎉»** — выросли бонусные дни реферальной программы
  (`awardedDays` из `GET /v1/referrals/me`). Подарочный тон `#8A6200`, иконка
  подарка, «Вам добавлено N дн. подписки по реферальной программе.», кнопка
  «Ура!». Текст без «друг оплатил»: бонус получает и пригласивший, и
  приглашённый — за собственную первую оплату. Увиденное значение —
  `getLastSeenReferralAwardedDays(userId)`; `-1` — только база, без диалога.
- **«Подписка продлена»** — `subscriptionPeriod` **вырос не за счёт бонуса** и
  пользователь этого не видел: автопродление (рекуррентный платёж), оплата на
  другой платформе, правка на сервере. Галочка, «Срок подписки продлён до
  dd.MM.yyyy. Спасибо за поддержку приложения!», «Отлично!». **Уменьшение срока
  диалогом не сообщается** (новый срок просто запоминается): пользователь может
  не помнить прежний срок, и нейтральное «срок изменён» читалось бы двусмысленно.

Правила (проверка при старте и при каждом изменении `subscriptionPeriod` в БД,
bearer-токена или `userId`, т.е. и при выходе/входе под другим аккаунтом;
сначала бонус, затем срок):

- **Ключ аккаунта `userId`** — серверный `user.id` (`GET /v1/users/me`), в
  `SecureTokenStorage`. Он же уходит `user_id` в оплату Robokassa, поэтому
  чужой id недопустим:
  - выход (`logOut`) и истечение сессии (`SessionExpiredHandler`) очищают его
    вместе с токеном;
  - вход и регистрация (email, VK ID, регистрация по VK, в т.ч. миграционные;
    Android: `AccountSwitcher`) стирают прежний id **до** сохранения токена и
    сразу сохраняют новый из профиля, не дожидаясь открытия экрана Профиль.
    Всё идёт под `SubscriptionPeriodTracker` (см. ниже): срок нового
    аккаунта подтягивается раньше, чем известен его id, и не должен
    сравниться с «увиденным» прежнего аккаунта;
  - **срок подписки в локальных настройках при входе/регистрации заменяется**
    серверным сроком нового аккаунта (`GET /v1/user_settings/`; 404 — нет
    настроек → 0), а не сливается через `max`. Причина: после выхода локально
    остаётся срок прежнего аккаунта, а все синхронизации и `restorePurchases`
    сливают `max(локальный, серверный)` — без замены аккаунт без подписки
    навсегда получал бы чужие платные функции. Сервер клиентский срок не
    принимает, так что источник истины — он. Сервер не ответил → локально 0 и
    snackbar «Вход выполнен, но данные подписки не загрузились»; свой срок
    вернёт первый успешный restore/синк. Legacy-срок локальной покупки
    (`getSubscriptionExpiration`) регистрация по email по-прежнему переносит
    в настройки — уже после сброса. Сам выход срок не трогает;
  - токен есть, а `userId` пуст (профиль не загрузился при входе, старая
    установка) — проверка сама получает id из профиля и сохраняет его, после
    чего перепроверяет уже под верным ключом. Экран оплаты Robokassa так же
    получает id с сервера, если сохранённый пуст.
- Нет токена или не удалось получить `userId`/`awardedDays` (офлайн/ошибка) —
  ничего не запоминаем, проверка повторится при следующем изменении/запуске.
- «Увиденный» срок — `getLastSeenSubscriptionPeriod(userId)` плюс
  `getSubscriptionPeriodReferralDays(userId)` — сколько `awardedDays` уже учтено
  в нём. `subscriptionPeriod <= 0` — пропуск; `-1` (первое отслеживание
  аккаунта, в т.ч. вход с оплаченной подпиской) — только база.
- Бонусные дни с прошлого увиденного срока: `bonus = awardedDays − учтённые`.
  Сервер начисляет бонус как `max(срок, сейчас) + бонус`, поэтому о сроке
  сообщаем только о росте **до будущей даты** (рост до уже истёкшей даты — не
  продление, так приходит, например, прошлый срок из синхронизации): при
  `bonus == 0` — о любом, иначе — только если
  `новый − bonus·сутки > max(прежний, сейчас) + 1 сутки` (запас на округление
  `awardedDays` вниз). Бонус, пришедший раньше синхронизации срока, так не
  превращается в «Подписка продлена».
- Бонус и оплата одновременно — оба диалога по очереди: сначала бонус.
- **Оплата из приложения не дублирует диалог о сроке**: пока есть
  неподтверждённая оплата (§19.6, `PaymentReturnChecker.hasPending`), срок не
  оценивается и «увиденное» по нему не трогается (бонус — как обычно); на время
  поллинга проверка срока приостановлена
  (`SubscriptionPeriodTracker.runPeriodUpdate` — **счётчик**, а не флаг:
  проверка возобновляется, только когда завершились все обновления срока —
  поллинги и вход). Второй поллинг, пока идёт первый, не запускается. Поллинг
  ждёт строго `subscriptionPeriod > срок до оплаты`, уже активная подписка
  успехом не считается. При успехе срок помечается увиденным до выхода из
  трекера (учтённые бонусные дни → `-1`, база берётся из текущего
  `awardedDays` — бонус приглашённого за эту оплату уже внутри срока). Бонус
  при этом показывается как обычно. Если оплата так и не подтвердилась и
  запись истекла (24 ч), а срок сменился — всплывёт «Подписка продлена».
- iOS: диалоги не реализованы. При переносе учесть: в
  `ProfileIosViewModel` (старый CMP-код) `userId` = email входа, вход по VK —
  заглушка, а при протухшем токене (`GET /v1/users/me` с ошибкой) `userId` не
  очищается. Ключом делать серверный `user.id` и чистить его вместе с токеном
  (см. `60_IOS_TODO.md`).
- PWA (`loco-driver-pwa`): ключ id → email → `vk:<vk_id>`; оплата на экране
  Подписки защищена счётчиком поллингов.
- Вид обоих диалогов — `PaymentDialog` (§19.4): «Бонус начислен! 🎉» — иконка
  подарка, тон `#8A6200`, кнопка «Ура!»; «Подписка продлена» — галочка, тон
  `surfaceTint` (успех), кнопка «Отлично!»; дата — `dd.MM.yyyy` в системном поясе
  устройства. Оба закрываются кнопкой, Back'ом и тапом вне. Если бонус пришёл
  несколько раз до закрытия диалога, дни суммируются в одном диалоге. Порядок
  показа: «Платёж принят!» → «Бонус начислен!» → «Подписка продлена» (одновременно
  виден только один).

Источник: `features/route/.../viewmodel/SubscriptionNoticesViewModel.kt`,
`subscription/SubscriptionNoticesPolicy.kt`, `subscription/SubscriptionPeriodTracker.kt`,
`ui/SubscriptionPeriodChangedDialog.kt`, `app/.../ui/LocoDriverApp.kt`,
`session/AccountSwitcher.kt`.

### 19.6. «Платёж принят!» при любом возврате из оплаты (бывш. §32.1b)

Цель: пользователь, оплативший в приложении, **всегда** видит «Платёж принят!»,
каким бы способом ни вернулся — «Назад», недавние, иконка, холодный старт после
того, как система выгрузила приложение, пока он был в банке. Android:
`PaymentReturnChecker` (singleton), показ в `LocoDriverApp` поверх любого экрана.

- **Запись ожидаемой оплаты** ставится при переходе в оплату (открытие страницы
  оплаты Robokassa или CKassa): `userId`, момент перехода, срок до оплаты
  (в т.ч. `0`), срок тарифа в днях. Хранится в SharedPreferences
  (`getPendingPayment`), переживает выгрузку процесса.
- **Проверка** — на каждом `ON_RESUME` активности (возврат из браузера, холодный
  старт) и с экрана Покупок; проверка одна на всех (вторая
  присоединяется к идущей). Срок запрашивается с сервера (`restorePurchases`)
  **в обход гейта подписки** — у бесплатного пользователя автосинхронизации нет.
  Первые 10 мин после перехода — до 10 попыток × 3 с, дальше — 1 запрос на
  возврат, не чаще раза в 30 с.
- **Подтверждение**: `новый срок > срок до оплаты`, в будущем и вырос минимум на
  тариф − 1 сутки от `max(срок до оплаты, момент перехода)` — бонус рефералки
  оплатой не считается. Тогда запись снимается, срок отмечается увиденным,
  показывается «Платёж принят!» (галочка, «Подписка активна до dd.MM.yyyy.
  Спасибо за поддержку приложения!», «Отлично!»). Порядок глобальных диалогов:
  «Платёж принят!» → «Бонус начислен!» → «Подписка продлена».
- **Не подтвердилось** — глобально ничего не показываем (пользователь мог
  только посмотреть оплату или передумать); запись живёт 24 ч и проверяется на
  следующих возвратах. Выход / другой аккаунт — запись снимается без диалога.
- Страница возврата CKassa на сервере эту гарантию не заменяет (открывается
  только в браузере, где шла оплата, и не всегда) — только удобство.
- Проверка по возврату в Android вешается на `ON_RESUME` lifecycle корневого
  `LocoDriverApp`; срок с сервера берётся через тот же `restorePurchases` (без
  snackbar'ов) и затем читается из локальных настроек. Диалог — `PaymentAcceptedDialog`
  (`PaymentDialog`, галочка, `surfaceTint`, «Платёж принят!» / «Подписка активна до
  dd.MM.yyyy. Спасибо за поддержку приложения!» / «Отлично!»; дата в системном поясе).
- Запись хранится строкой `userId|startedAt|periodBefore|periodDays`; константы:
  TTL 24 ч, «свежая оплата» 10 мин, 10 попыток, пауза 3 с, троттлинг возвратов 30 с.

Источник: `features/route/.../subscription/PaymentReturnChecker.kt`,
`ui/SubscriptionPeriodChangedDialog.kt` (`PaymentAcceptedDialog`),
`app/.../ui/LocoDriverApp.kt`, `app/.../di/RepositoryModule.kt`.
---

## 20. Онбординг и вход

**Фактическое поведение Android (код — правда):**

- **Первый запуск** (`SharedPreferences` `TOKEN_IS_FIRST_APP_ENTRY_TAG`, по
  умолчанию `true`) обрабатывается в `MainViewModel.init`, **без отдельного экрана**:
  1. помечаются выполненными все одноразовые миграции (`isMigrated`, миграция
     часовых поясов, дней отвлечений, производственного календаря) — новому
     пользователю переносить нечего;
  2. автоопределение страны и пояса (`applyInitialCountryAndTimezone`):
     страна = страна локали устройства, если она из {RU, KZ, BY}, иначе RU;
     пояс (смещение от Москвы, ms): KZ → `+2 ч` (UTC+5), BY → `0` (UTC+3),
     RU → `(смещение устройства − 3 ч)`, усечённое до целых часов (к нулю), и только в
     диапазоне `[−1 ч … +9 ч]`, иначе `0` (Москва). Результат пишется в
     `UserSettings.country`/`timeZone`;
  3. флаг первого запуска **сразу** сбрасывается в `false` (синхронный commit).
  Затем открывается Главный экран (§4), как и при обычном запуске.
- **Приветственный онбординг** `FirstPresentationBlockScreen` (7 слайдов
  `OnBoardingItems.getDataFirstPresentation()`: «Добавляй маршруты», «Больше
  информации», «Расход топлива и электроэнергии», «Просмотривай статистику»,
  «Рассчетный лист», «Поиск», «Персонализация») в коде есть, но **не зарегистрирован в
  NavHost** и ниоткуда не вызывается
  (`showFirstPresentation` в `MainViewModel` не используется) — пользователь его не видит.
  🚫 Мёртвый код Android — в iOS не реализуется.
- **Экраны входа/регистрации** `SignInScreenRoute` («SignInScreen») /
  `LogInScreenRoute` («LogInScreen») объявлены в `Router`
  (`showSignIn()`/`showLogIn()`), но **не зарегистрированы в NavHost**; живых вызовов нет
  (вызов из Настроек привязан к
  легаси-`logOutState`, который никогда не выставляется; из
  `FirstPresentationBlockDestination` — мёртвый код). 🚫 Мёртвый код Android — в iOS не
  реализуется. ⚠️ Поведение Android (возможный баг, BUG-09): вызов
  `router.showSignIn()`/`showLogIn()` привёл бы к навигации на незарегистрированный маршрут
  (крэш). Вся
  аутентификация — в Профиле (§18.0), куда ведёт и гейт покупок (§3.2).
- **Компонент `PresentationBlock`** (общий для онбординга и «Что нового», §22):
  фон `secondaryContainer`; сверху стрелка «назад» (на предыдущий слайд) и
  текстовая кнопка «Пропустить» (→ завершение); по центру `HorizontalPager` со
  слайдом «картинка + заголовок + описание» (листается и свайпом); снизу точки-
  индикаторы и кнопка «Далее» / на последнем слайде «Начать» (→ завершение).

Источник: `app/.../viewmodel/MainViewModel.kt` (`isFirstEntry`,
`applyInitialCountryAndTimezone`), `features/route/.../ui/login/FirstPresentationBlockScreen.kt`,
`navigation/login/FirstPresentationBlockDestination.kt`, `navigation/login/Routes.kt`,
`navigation/Navigation.kt`, `app/.../ui/navigation/RouterImpl.kt`,
`core_android/.../ui/component/OnBoardingItems.kt`, `core_android/.../ui/component/PresentationBlock.kt`,
`data_local/src/androidMain/.../SharedPreferenceStorage.kt`.

---

## 21. Экспорт PDF (маршрутный лист / расчёт / график)

Формирование PDF за выбранный месяц Главного экрана и передача его в системное
меню «Открыть»/«Поделиться». Не отдельный экран.

**Точки входа.**
- Главный экран, карточка действия **«PDF»** в ряду быстрых действий (§4.4).
  Пока идёт генерация, карточка неактивна.
- Экран «Статистика» формирует **свой** PDF (`StatisticsPdfGenerator`, §13.5) и
  переиспользует только `PdfActionSheet` — вне этого раздела.
- На «Расчёте зарплаты» PDF-иконки нет (заменена поиском по кодам выплат), на
  «Всех маршрутах» — нет (место заняла кнопка «Выбрать»). Гейта подписки у экспорта
  нет (хотя «Экспорт в PDF» перечислен среди преимуществ Pro на экране Покупок).

**Сценарий.**
1. Тап «PDF» → шторка **`PdfContentDialog`** (`AppBottomSheet`, раскрыта сразу
   полностью): заголовок **«Содержание PDF»**, три чекбокса, все по умолчанию
   **включены** (значения переживают поворот): «Поездки детально»
   (`includeRouteDetails`), «График» (`includeSchedule`), «Расчётный листок»
   (`includeSalary`); одно действие **«Сформировать»**. Кнопка доступна и при всех
   выключенных чекбоксах (iOS/PWA повторяют, пока поведение не изменено везде).
2. «Сформировать» закрывает шторку и вызывает
   `generateAndShare(sections, routes, monthLabel, calendarDays)`, где `routes` —
   маршруты текущего месяца Главного, `monthLabel` — «Апрель 2026» (именительный
   падеж, как в шапке), `calendarDays` — дни `MonthOfYear` месяца (для отвлечений и
   нормы). Повторный вызов во время генерации игнорируется (`isGenerating`).
3. Если включён расчётный листок — берётся последний `SalaryCalculationUIState`,
   переданный экраном расчёта, иначе ожидается успешный расчёт singleton-VM
   `SalaryCalculationViewModel` **до 8 с**. Не дождались → PDF всё равно
   формируется, а на странице листка вместо таблицы текст «Данные расчёта
   недоступны. Откройте «Расчёт заработной платы» и повторите формирование.»
   (не ошибка).
4. Успех → шторка **`PdfActionSheet`** (`AppBottomSheet`): заголовок **«PDF
   сформирован»**, действия **«Открыть PDF»** (системный chooser «Открыть в…»,
   `ACTION_VIEW`, `application/pdf`) и **«Поделиться»** (chooser «Поделиться PDF»,
   `ACTION_SEND`). Выбор любого действия **сначала закрывает шторку** (общее правило
   `AppBottomSheet`), затем открывает chooser. Закрытие шторки файл не удаляет.
5. Ошибка генерации → snackbar «Ошибка формирования PDF: {сообщение}» на Главном,
   `isGenerating` сбрасывается, можно повторить. Частичный файл не отдаётся.
   ❓ Не проверено: что происходит при всех выключенных разделах (документ без
   страниц) — вероятно, ошибка записи → snackbar.

**Файл.** Кэш-каталог приложения, имя `Машинист_{Месяц}_{Год}.pdf` (из
`monthLabel`, месяц с заглавной; при сбое разбора — «Месяц»/«Год»). Перед
генерацией удаляются старые `Машинист_*.pdf` из кэша. Android отдаёт файл через
`FileProvider` (authority `${packageName}.fileprovider`); iOS/PWA — платформенный
эквивалент временного файла + share sheet.

**Страница.** A4 в пунктах: 595×842, поля слева/справа 36. На каждой странице
разделов «Поездки» и «График» сверху мелкий курсив «Файл сформирован в приложении
«Машинист»» с линией; страницы расчётного листка — без этой шапки. Каждый
включённый раздел начинается с новой страницы в порядке: Поездки → График →
Расчётный листок. Шрифты: заголовок 13 жирный, подзаголовок 10 жирный, текст 9,
мелкий 7–7.5. Таблицы — прямоугольные ячейки с рамкой, строки-заголовки с заливкой;
текст, не влезающий в ячейку, обрезается посимвольно.

**Формат дат/времени в PDF.** `dd.MM.yyyy` / `dd.MM.yyyy HH:mm`, длительности —
`ЧЧ:ММ` (`ConverterLongToTime`). ⚠️ Поведение Android (возможный баг, BUG-59): все времена
печатаются в
фиксированном поясе **GMT+3** (Москва), а не в поясе отображения пользователя (для KZ —
местный пояс, §0.1).

**Раздел «Поездки детально».** Заголовок «Поездки за {monthLabel}»; при пустом
списке — «Нет маршрутов за период.». Маршруты по возрастанию явки (без явки — в
конце); маршрут не разрывается между страницами, если помещается (оценка высоты).
Для каждого маршрута:
- строка-заголовок «Маршрут №{номер или «б/н»}   {дата явки}»;
- таблица «Явка | Сдача | Рабочее время» (рабочее время = сдача − явка − перерыв,
  не меньше 0; нет данных → «—»);
- если задан перерыв — «Начало перерыва | Окончание перерыва»;
- пометки «• Отдых в пункте оборота», «• Работа в одно лицо», «Примечание: …»;
- по каждому локомотиву: «Локомотив N (Электровоз|Тепловоз) | серия | номер»;
  «Нач. приёмки | Оконч. приёмки | Нач. сдачи | Оконч. сдачи»; при наличии
  показаний — «Отопление (счётчик) | Принял | Сдал | Расход» и «Собственные нужды |
  …» (1 знак после запятой). ⚠️ Поведение Android (возможный баг, BUG-60): «Расход» здесь =
  `принял − сдал`, тогда как в форме локомотива (§1.3) — `сдано − принято`.
  Затем секции (только с ненулевыми данными): «Секция N | Принял | Сдал | Итого»
  (+ «Норма | Результат», если у локомотива задана норма) со строками «Расход
  тяги»/«Рекуперация» (электровоз) или «Топливо» (тепловоз) и итоговые строки
  «Итого»;
- по каждому поезду: «Поезд N | №… (или «б/н») | Вес … т | Оси … | у.д. …» (у.д.
  без «.0»), затем таблица станций (правила ниже);
- по каждому следованию пассажиром: «Пассажиром N | Поезд №…»; «Отправление |
  Прибытие | В пути»; при наличии станций — «Откуда | Куда»; «Примечание: …».
- Прочая работа, напарники, вспомогательная тяга и вагонник в PDF **не выводятся**.
- **Таблица станций поезда** (3 колонки: Станция | Прибытие | Отправление):
  в колонке «Станция» к названию добавляются номер пути `(N п.)` и пометка
  «· проходная». У проходной станции «Отправление» — всегда `—`
  (без остановки; пометка в названии нужна, чтобы прочерк не читался как
  незаполненные данные). Если у перегона перед станцией заполнены
  `segmentTrackNumber`/`segmentNotes`, под строкой станции печатается
  дополнительная строка «перегон · путь N · примечание» (учтена в расчёте
  высоты страницы).

**Раздел «График».** Заголовок «График за {monthLabel}» (не удалось разобрать
месяц — «Не удалось определить период из «…».»). Календарная сетка 7 колонок
(Пн…Вс, ячейка высотой 48): номер дня в углу; дни работы — голубая заливка,
пропорциональная времени работы внутри суток; отвлечения — фиолетовая заливка и
подпись («Отпуск», «Больн.», «Курсы», «Донор», «Уход», «Команд.», «Техзан.», иначе
«Отвл.»); дни только пассажиром — оранжевая заливка и длительность; время явки —
крупно по центру (несколько явок — столбиком). Под сеткой сводка: «Норма на месяц
N ч.», «Личная норма N ч.» (если отличается), «Маршрутов в месяце», «Отработано в
месяце» (переходные маршруты обрезаются границами месяца по МСК, перерыв
вычитается), «Следование пассажиром» (если > 0), «Отвлечения N ч.» (кроме
«Выходной» и «Технические занятия», часы по рабочей неделе §16.3), «Всего» =
отработано + отвлечения.

**Раздел «Расчётный листок».** Чёрно-белая форма по структуре ФТУ-69: заголовок
«Расчётный листок за {месяц} месяц {год} года» (на следующих страницах —
«(продолжение)»); строки «Оклад/Тариф», «Норма часов», «Всего отработано часов»,
«Валюта расчёта»; две колонки «Начислено» (Месяц | Вид выплаты | Код | Часы | % |
Сумма) и «Удержано и перечислено» (Месяц | Вид удержания | Код | % | Сумма) —
строки те же, что на экране расчёта (§11, `buildAccrualRows`/`buildDeductionRows`,
коды из `PayrollPaymentCatalog`); итоги «Всего начислено», «Всего удержано»,
«Всего отработано часов за месяц», «К перечислению», подпись «Сформировано в
приложении «Машинист»». Часы в листке — десятичные (ч), нулевые часы и проценты
не печатаются; валюта — `SalaryCalculationUIState.currency`.

- **ViewModel**: `PdfViewModel` (Android: `AndroidViewModel`). Данные подаются
  экранами извне (`updateSalaryState`, `updateRoutes(routes, monthLabel)`,
  `updateCalendarDays`); пустые аргументы `generateAndShare` заменяются этими
  последними значениями. Выход: `pdfReady` (однократное событие с Uri),
  `isGenerating`, `errorMessage`.
- Быстрый просмотр маршрута выполняется через `RouteQuickViewSheet` (§9.1);
  отдельного действующего `PreviewRouteDialog` в Android-коде нет.

Источник: `features/route/.../viewmodel/PdfViewModel.kt`, `util/PdfGenerator.kt`,
`util/PdfSections.kt`, `component/PdfContentDialog.kt`, `component/PdfActionSheet.kt`,
`component/AppBottomSheet.kt`, `ui/HomeScreen.kt` (карточка «PDF», обработка
`pdfReady`/`errorMessage`), `navigation/StatisticsDestination.kt`.

---

## 22. Служебные и легаси-экраны

- **«Что нового» после обновления** (`UpdatePresentationBlockScreen`,
  `UpdatePresentationBlockRoute`) — легаси-блок из 2 слайдов
  `OnBoardingItems.getDataUpdatePresentation()` («Часовой пояс» / «Для учета
  переходных маршрутов, ночных и праздничных часов установите свой часовой пояс»;
  «Главный экран» / «Добавили обшее отработанное время на главный экран»), компонент
  `PresentationBlock` (§20); «Пропустить»/«Начать» → `router.showHome`.
  Показывается **overlay'ем поверх всего приложения** (`LocoDriverApp`), если флаг
  `TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16` (по умолчанию `true`) ещё не
  сброшен и это не первый запуск. Флаг сбрасывается при первом создании
  `HomeViewModel` (метод с обманчивым именем `enableShowingUpdatePresentation()`
  пишет `false`), поэтому блок видят только пользователи, обновившиеся с очень
  старых версий, один раз. Флаг читается один раз при старте процесса.
  ⚠️ Поведение Android (возможный баг, BUG-08): overlay управляется
  неизменяемым значением, прочитанным при старте, — «Начать»/«Пропустить» переходят на
  Главный под overlay'ем, но сам overlay в этой сессии не исчезает. Актуальный механизм анонсов —
  §23 (`AnnouncementScreen`); в iOS/PWA этот блок не переносить.
- **`DetailsRoute` / `Router.showRouteDetails`**: на Android НЕ зарегистрирован в NavHost
  (`Navigation.kt`) — 🚫 Мёртвый код Android — в iOS не реализуется. Метод остаётся в
  `Router` только потому, что его вызывает старый CMP-экран
  (`iosApp/src/commonMain/.../screen/HomeScreen.kt`, мёртвый груз); удалять вместе с
  CMP-кодом.
- `ConfirmEmailDialog` — обёртка над `AppInputBottomSheet` (§18.6): заголовок
  «Подтверждение email», поле с email (клавиатура email), подсказка «На данный email
  будет отправлено письмо со ссылкой для верификации аккаунта.», кнопка «Отправить
  письмо» (доступность задаёт вызывающий). Вызовов нет — подтверждения email в
  текущем Android нет.
- `HomeBottomSheetContent`, `ConfirmEmailDialog` и `RemoveTimeContent` сейчас не
  вызываются действующим Android UI (поиск usages находит только объявления;
  внутри `HomeBottomSheetContent` старый вызов `ItemHomeScreen` закомментирован).
  🚫 Мёртвый код Android — в iOS не реализуется. Актуальные аналоги:
  `RouteQuickViewSheet`, state UI Профиля и общий контракт удаления §24.2–24.3.
- `ItemHomeScreen` — не самостоятельный экран, а активная карточка маршрута на
  Главной, «Все маршруты» и Календаре (§4.5, §9, §14). `SkeletonHomeScreen` — только
  состояние первой загрузки Главной (§4.1), не отдельный destination.

- **Сводный список: 🚫 Мёртвый код Android — в iOS не реализуется.**
  `FirstPresentationBlockScreen` и `SignInScreenRoute`/`LogInScreenRoute` (§20);
  `ConfirmExitDialog` (§3.3 п.4); `EnteredCoefficientDialog`/`EnteredRefuelDialog` и флаги
  `refuelDialogShow`/`coefficientDialogShow` (§1.3); `AnchorSelectionBanner` (§2.13);
  `CustomDatePickerDialog` (§24.2); `SelectReleaseDaysScreen` (§16.2); шторка «Новое
  плечо»/«Создать плечо?» (§6.2 п.4); шторки подписки в `FormScreen` (§5.6);
  `PassengerFormViewModel.clearAllField` (§7.1); `LocoFormViewModel.timeZoneText`;
  `DetailsRoute`, `HomeBottomSheetContent`, `ConfirmEmailDialog`, `RemoveTimeContent`
  (выше); `LegacyTrashContent` в `SettingsScreen.kt` (с действием «Принять удаление»; живая
  корзина — `TrashScreen`, §9.4); `ProfileViewModel.startSyncUpload`/`startSyncDownload`;
  `UIHelper.getMessageThrowable`; легаси-состояния `uploadState`/`downloadState`/
  `logOutState` в `SettingsUiState` (§17); `resentVerificationEmailButton` (§18.2).
  Android-легаси, живое, но в iOS не переносится: «Что нового» (выше), легаси-миграция
  (§18.5), восстановление после ошибки миграции и диагностика (§25).

Источник: `features/route/.../ui/UpdatePresentationBlockScreen.kt`,
`navigation/UpdatePresentationBlockdestination.kt`, `app/.../ui/LocoDriverApp.kt`,
`app/.../viewmodel/MainViewModel.kt` (`showUpdatePresentation`),
`viewmodel/home_view_model/HomeViewModel.kt` (`init`),
`component/ConfirmEmailDialog.kt`, `ui/SettingsScreen.kt` (`LegacyTrashContent`),
`ui/login/UIHelper.kt`.

> **Проверено (аудит покрытия и мёртвого кода):** все экраны, зарегистрированные в
> `features/route/.../navigation/Navigation.kt`, плюс онбординг и PDF-экспорт описаны
> в разделах 1–23. Шторки, диалоги и состояния описаны в разделах своих
> экранов; общий контракт — §24, проверять покрытие — по реестру §24.5. Удалён мёртвый код:
> старый «График» (`WorkScheduleScreen`+VM), экраны-заглушки `NormsScreen` /
> `WidgetsInfoScreen`, цепочка `MoreInfo` (`showMoreInfo` / `MoreInfoRoute` /
> `onMoreInfoClick`) — вместе со ссылками в `Router`, `RouterImpl`, DI и iOS-модуле.

---

## 23. Экран «Новость при запуске» (AnnouncementScreen)

Полноэкранное широковещательное сообщение от разработчика, показываемое **при
запуске приложения** (не пуш). Служит для анонсов версий/новостей.

### 23.1. Что видит пользователь
Два типа экрана, диспетчеризуются по полю `type` (`AnnouncementScreen.kt`):

**Тип «Новость» (`type = "news"`):**
- Полноэкранный экран поверх приложения после сплэша: крупный заголовок (жирный),
  многострочный текст, внизу pill-кнопка **«Понятно»** во всю ширину. В правом
  верхнем углу — крестик закрытия. Тема-адаптив (`LocoDriverTheme`).
- **С картинкой** (`imageUrl != null`): картинка сверху (в скруглённой карточке,
  `AsyncImage`/Coil), ниже заголовок и текст слева; без скролла, картинка и текст
  делят высоту через `weight`, текст при нехватке места обрезается (ellipsis).
- **Без картинки** (`imageUrl == null`): hero-иконка (ℹ) в тональном скруглённом
  квадрате и **центрированный текст** (заголовок и текст по центру), со скроллом
  для длинного сообщения. Тональный фон иконки — на `primary` с alpha (не
  `surfaceContainerHigh`, который в этой теме оранжевый).
- Без mono-оверлайна с датой и без нижней карточки со временем недоступности
  (в отличие от исходного дизайн-референса) — по требованию.

**Тип «Обновление» (`type = "update"`):**
- Карусель фич обновления, **строго на одном экране без скролла**, листается
  **свайпом в обе стороны** (`HorizontalPager`) и кнопкой «Далее» (свайп назад
  кнопкой не ограничен — можно вернуться к уже просмотренной фиче). Сверху, над
  картинкой, на **каждой** странице карусели — общий заголовок обновления
  (`announcement.title`, например «Обновление 3.0.0»): одна строка, слева,
  мелким акцентным шрифтом (`titleSmall`, `onSurfaceVariant`), обрезается
  (ellipsis) если не помещается. Не показывается, если `title` пустой. Ниже —
  одна фича на экране: **картинка (крупная) → название → описание**. Внизу
  индикатор-точки (если фич больше одной, активная точка = текущая страница
  пейджера) и pill-кнопка **«Далее»** (если есть ещё фичи) либо **«Понятно»**
  (на последней). В правом верхнем углу — крестик закрытия (закрывает всё
  обновление сразу).
- Картинка (`AsyncImage`/Coil) и описание — гибкие области внутри страницы
  пейджера (`weight`, картинка занимает бо́льшую долю — `1.9f` против `0.55f`
  у описания, поэтому текст сдвинут ниже); **кнопка всегда видна**, при крупном
  системном шрифте описание **обрезается** (ellipsis), а не выталкивает
  кнопку. `body` самого сообщения на этом типе не показывается; в кабинете
  `title` подписан как «внутренняя метка/версия», но фактически показывается
  пользователю сверху карусели (кабинету это поле стоит переименовать/пояснить
  заново, если правится).
- Если у фичи **нет картинки** — её текст (название + описание) центрируется
  (общее правило: нет картинки ⇒ текст по центру).
- **Предзагрузка**: при первом появлении карусели картинки **всех** фич сразу
  ставятся в очередь Coil (`context.imageLoader.enqueue`, не дожидаясь показа
  каждой страницы) — так при свайпе между фичами не видно задержки на
  загрузку картинки.

- Кнопка/крестик закрывают экран; повторно то же сообщение (по `number`) не
  показывается (см. 23.3). Одна отметка «видел» на всё обновление.

### 23.2. Контракт с сервером
- `GET /v1/announcements/latest?platform=android&build=<versionCode>` — **без
  авторизации**. Ответ `200` с телом `{number, title, body, platform,
  minBuild, maxBuild, isActive, displayMode, type, features}` либо `204`
  (показывать нечего).
- Сервер отдаёт одно активное сообщение с максимальным `number`,
  отфильтрованное по платформе (`all`/`android`/`ios`) и версии сборки
  (`min_build`/`max_build`).
- `type` — `"news"` | `"update"` (аддитивно, дефолт `"news"` для старого сервера).
- `imageUrl` (верхний уровень) — необязательная картинка **новости**:
  относительный `/v1/announcements/announcement-image/{id}` либо `null`. Клиент
  так же склеивает с базовым URL. Отдаётся `GET
  /v1/announcements/announcement-image/{id}` (публичный, аналогично feature-image).
- `features` — список фич (только для `type = "update"`, иначе пусто); каждый
  элемент `{position, title, description, imageUrl}`. `imageUrl` приходит
  **относительным** (`/v1/announcements/feature-image/{id}`) либо `null`; клиент
  склеивает его с базовым URL API (`RemoteRestClient.BASE_URL`) в
  `RemoteAnnouncementRepository`.
- `GET /v1/announcements/feature-image/{feature_id}` — **публичный**, отдаёт
  байты картинки фичи с её MIME (картинки хранятся `bytea` в БД), `404` если
  нет. `Cache-Control: public, max-age=86400`.
- Клиенту из тела нужны: `number`, `type`, `displayMode`; для `news` —
  `title`/`body`; для `update` — `features`.

### 23.3. Логика «видел/не видел» (хранение — на устройстве)
- `number` — монотонный ключ управления и дедупликации. На устройстве хранится
  `lastSeenAnnouncementNumber` (`SharedPreferencesRepositories`, ключ
  `LAST_SEEN_ANNOUNCEMENT_NUMBER`, дефолт `-1` = ещё ничего не видел).
- Алгоритм (`AnnouncementUseCase.getAnnouncementToShow`) простой и предсказуемый:
  1. запросить `getLatest(platform, build)`; при null (нет активного / 204 /
     офлайн) — ничего не показывать;
  2. **тип `update` не показываем свежим установкам** (`isFreshInstall`) — им
     нечего «обновлять»; тип `news` показываем всем;
  3. показать, только если `number > lastSeen`;
  4. по «Понятно» — `markSeen(number)` (сохранить `number`).
- **Свежая установка** (`isFreshInstall`) определяется на Android в
  `MainViewModel`: `PackageInfo.firstInstallTime == lastUpdateTime` (APK ни разу
  не обновлялся). После первого обновления APK условие ложно → «Обновление»
  начинает показываться. Флаг передаётся параметром в `getAnnouncementToShow`
  (KMP-use case платформо-независим). iOS: аналог ещё не реализован.
- Следствия управления:
  - чтобы разослать новое сообщение всем — задать `number` **больше** предыдущего;
  - новые установки (`lastSeen = -1`) увидят текущее активное сообщение один раз;
  - повторный POST с тем же/меньшим `number` никому не покажется.
- Загрузка запускается после инициализации приложения
  (`MainViewModel.loadAnnouncement()` после `appInitialized`), рендер —
  overlay'ем в `MainActivity`.

### 23.4. Заведение сообщений (админ)
- Веб-кабинет `/admin` (сессия по паролю, CSRF): создание/редактирование,
  выбор `type` = «Новость»/«Обновление».
  - Для «Новости» — заголовок, текст и **необязательная картинка** (PNG/JPEG/WEBP,
    до 2 МБ), сохраняется `bytea` в `announcement.image_data`. При редактировании
    без нового файла картинка сохраняется; есть чекбокс «Удалить картинку».
  - Для «Обновления» — редактор списка фич: добавлять по одной
    **скрин + название + описание**; форма `multipart/form-data`, картинки
    (PNG/JPEG/WEBP, **до 2 МБ**, без сжатия на сервере) сохраняются `bytea` в
    таблицу `announcement_feature` (full-replace фич сообщения при сохранении).
    Порядок строк = `position`. При редактировании без нового файла картинка
    сохраняется. Удаление сообщения каскадно удаляет фичи (FK `ON DELETE CASCADE`).
- API: `POST /v1/announcements/` с `X-ADMIN-KEY` (upsert по `number`; поле `type`
  поддерживается, но фичи задаются через кабинет), `GET /v1/announcements/` —
  список, `DELETE /v1/announcements/{number}`. Ключ `ADMIN_API_KEY` в конфиге
  сервера (fail-closed: пусто ⇒ доступа нет).

---

## 24. Общий контракт шторок, диалогов, пикеров и transient UI

Этот раздел обязателен для iOS/PWA: перечисленные компоненты не всегда являются
отдельными nav-destination, но представляют самостоятельные пользовательские
состояния. Нативный внешний вид платформы допустим; содержимое, результат и
правила закрытия должны совпадать.

### 24.1. Базовые контейнеры

- **`AppBottomSheet`** — стандартная нижняя шторка действий. Затемняет фон,
  закрывается свайпом вниз, тапом вне и системным Back, если конкретный сценарий
  не запрещает это. Внутри: ручка, необязательный заголовок/описание, затем
  `BottomSheetAction`. Опасное действие визуально выделяется цветом ошибки. Тап по действию
  сначала вызывает `onDismissRequest()` (шторка закрывается), затем само действие.
- **`AppInputBottomSheet`** — шторка одного текстового/числового значения:
  заголовок, поле с начальным значением, inline-ошибка, кнопки отмены/подтверждения.
  Подтверждение недоступно при `isValid=false`; при `isLoading` повторная отправка
  блокируется. Клавиатура задаётся вызывающим экраном. Полный контракт — §18.6.
- **`AppAlertDialog`** — короткое подтверждение поверх экрана. Не использовать
  вместо шторки там, где Android-эталон явно применяет `AppBottomSheet`.
- **`AnimationDialog`** — контейнер модального прогресса/результата с анимацией
  появления. Сам по себе бизнес-логики не имеет; результат задаёт вызывающий экран.
- Все модальные состояния **взаимоисключающие**: после выбора действие сначала
  закрывает текущую шторку, затем открывает следующий экран/пикер. Это исключает
  наложение двух overlays и скрытые snackbar под шторкой.

### 24.2. Дата и время

- **`AppDateTimePicker`** редактирует один `Long?` в пользовательском часовом
  поясе. Заголовок задаёт контекст («Явка», «Сдача», «Начало периода» и т.п.).
  Результат обрезается до минуты; отмена не меняет исходное значение.
- Вариант отображения зависит от `UserSettings.useStandardTimePicker` (синхронизируется;
  «Стиль выбора времени», §17.5):
  - `false` (по умолчанию) — кастомная нижняя шторка `DateTimePickerBottomSheet`
    (календарь + барабаны), описана ниже;
  - `true` — стандартный режим: `DatePickerDialog` (кнопки «Далее» / «Отмена») → диалог
    времени (24-часовой, кнопки «ОК» / «Назад»; «Назад» возвращает к выбору даты;
    переключатель циферблат/клавиатура в заголовке запоминается на устройстве).
  В обоих режимах результат — с обнулёнными секундами в поясе отображения. Это только
  UI-настройка, формат хранения один.
- Кастомная нижняя шторка даты и времени не закрывается и не перемещается
  свайпом; drag-handle не показывается. В верхней строке справа находится кнопка
  **«Закрыть»** синего акцентного цвета, которая закрывает пикер без выбора нового
  значения. Календарь всегда показывает полный месяц без переключателя
  «Неделя / Месяц» и визуально повторяет календарь экрана «Отвлечения»: месяц и
  год со стрелками, компактная строка дней недели и карточки дат с рамками.
  Системный Back и тап по затемнённой области также выполняют отмену через
  `onDismiss`.
- **`AppTimePicker`** редактирует время суток. Показывает последние значения для
  соответствующего ключа, если они переданы; подтверждённое значение добавляется
  в recent-times. Для интервалов дата не меняется.
- **Удаление времени**: долгое нажатие по заполненной строке открывает
  `AppBottomSheet` с названием поля, текущим значением и действием «Удалить
  значение». Простое нажатие открывает пикер. Для пустого значения long-press
  ничего не делает.
- `CustomDatePickerDialog` — 🚫 Мёртвый код Android — в iOS не реализуется (объявлен, нигде
  не вызывается).

### 24.3. Подтверждения выхода, удаления и копирования

- `ConfirmExitDialog` — 🚫 Мёртвый код Android — в iOS не реализуется: ни одна форма его
  не показывает; выход из форм — без подтверждения, данные уже сохранены (§3.3 п.4).
- Удаление маршрута/дочерней сущности/справочника/паттерна всегда требует
  подтверждения. После успеха закрываются и confirmation sheet, и экран/quick
  view, если удалён открытый объект. Ошибка показывается snackbar, объект остаётся.
  Исключения в Android (возможные баги): удаление плеча в редакторе (BUG-28,
  §6.6) и напарника в редакторе (BUG-30, §8.5.2) — без подтверждения. Пустая
  секция локомотива удаляется без подтверждения намеренно (§1.3).
- Копирование маршрута требует подтверждения «Создать копию». Копия получает
  новый `basicData.id`, исходный маршрут не меняется.

### 24.4. Snackbar, загрузка, пустые и ошибочные состояния

- Snackbar используется для результатов операций: удаление (без действия «Отменить» —
  восстановление только из корзины, §9.4), результат синхронизации/шаринга/PDF, сетевые
  и validation-ошибки. Технические
  network-тексты преобразуются в понятное сообщение; при вероятном VPN добавляется
  соответствующая подсказка.
- Блокирующая загрузка применяется только когда без результата нельзя показать
  экран/продолжить действие. Фоновая синхронизация — тонкий progress bar под app
  bar, контент остаётся доступен.
- Пустой список не подменяется бесконечным spinner: после завершения загрузки
  показываются назначение экрана и доступное основное действие («Добавить…»).
- Кнопка, запускающая async-операцию, блокируется на время выполнения. Повторный
  tap не создаёт второй запрос, платёж, sync или PDF. ⚠️ Android нарушает правило в CTA
  оплаты (BUG-58, §19.2).

### 24.5. Реестр обязательных overlays

| Контекст | Overlay | Где специфицирован |
|---|---|---|
| Запуск | «Что нового», Announcement, импорт маршрута, глобальные диалоги | §3.1, §22, §23, §19.5–19.6 |
| Локомотив | шторка времени, пикеры серии/станции, `CoeffSheet`, удаление секции | §1.3, §2 |
| Главная | месяц, список единиц маршрута, quick view, удаление/копия, «Перейти на сайт?», PDF, sync | §4.3–4.6, §9.1, §21 |
| Маршрут | «Расчёт за смену», «Отдых», дубль, shared preview, Passenger12h, `RouteConfirmDialog`, удаления | §5.3–5.8, §11.9 |
| Поезд | станция, перегон, плечи и редактор плеча, «Параметры поезда», «Вагонник», история данных, удаление станции | §6.2–6.7 |
| Пассажиром | пикеры времени, удаление значения | §7.3 |
| Прочая работа | тип, новый тип, удаление типа, выбор локомотива | §8.3 |
| Напарники | выбор, удаление | §8.5 |
| Все маршруты | месяц, фильтр, сортировка, легенда, quick view, копия, удаление, множественный выбор | §9.0.1–9.3 |
| Корзина | очистка, восстановление | §9.4 |
| Поиск | параметры поиска и период | §10.3 |
| Зарплата | месяц, справочник кодов, инфо-окно недоработки, «Расчёт за смену» | §11.6–11.9 |
| Настройки зарплаты | «Изменилась тарифная ставка», справки порогов, удаление порога | §12.1–12.3 |
| Статистика | сравнение, выбор месяца/года, детализация, «Грузооборот», PDF | §13.3–13.5 |
| Календарь | «Что добавить?», планирование, продолжительность, дубль, лимит, удаления, техзанятия | §14.2–14.6 |
| Мастер графика | продолжение графика, тип дня, время смены, удаление графика, прогресс | §15.1–15.2 |
| Отвлечения | диапазон, тип | §16.4 |
| Настройки | `SettingsPickerSheet`, `NightRangeSheet`, пикеры времени, страна/регион, длина вагона, редакторы | §17.1–17.5 |
| Профиль | состояния, вход/регистрация, email/VK, `SyncProgressDialog`, миграция, шторки ввода | §18.S–18.6 |
| Покупки | `PaymentDialog`, лимит, глобальные диалоги подписки | §19.1a, §19.4–19.6 |
| PDF | содержание, действия | §21 |

---

## 25. Служебные механизмы Android: восстановление после ошибки миграции и диагностика

> Раздел целиком Android-специфичный (миграция локальной `Route.db`, диагностика
> поддержки). В iOS/PWA **не переносится**.

### 25.1. Экран восстановления после ошибки миграции (`MigrationRecoveryScreen`): назначение

Android-специфичный экран (миграция локальной `Route.db`); в iOS/PWA не реализуется.
Экран открывается вместо основного приложения, если предзапусковая проверка
`Route.db` не смогла безопасно завершить миграцию. Он не зависит от основного
Koin-графа и самой маршрутной БД.

### 25.2. `MigrationRecoveryScreen`: поведение

- Состав (по центру, стандартная `MaterialTheme` без темы приложения): заголовок
  **«Данные сохранены»**; текст «Обновление базы не завершилось. Исходная копия
  маршрутов сохранена. Можно безопасно повторить восстановление.» (при нехватке
  места — «Для безопасного обновления не хватает свободного места. Освободите место
  на устройстве и повторите восстановление. Исходные маршруты не изменены.»); мелко
  «Код ошибки: {имя класса ошибки}»; кнопки во всю ширину: **«Восстановить
  приложение»**, при включённом облачном флаге — обводка **«Восстановить резервную
  копию аккаунта»** (+ сообщение под ней: «Для облачного восстановления нужно снова
  войти в аккаунт.» / «Резервная копия аккаунта не найдена.» / «Не удалось
  восстановить резервную копию. Код ошибки: …»), обводка **«Закрыть»**. Во время
  любой операции все кнопки неактивны, на нажатой — спиннер. Успех → пересоздание
  Activity (обычный запуск).
- приложение сообщает, что исходная копия маршрутов сохранена;
- техническое содержимое маршрутов и персональные данные не показываются;
- «Восстановить приложение» явно разрешает повторную попытку миграции;
- без нажатия одна и та же сборка не повторяет уже упавшую миграцию при каждом
  запуске;
- новая сборка может автоматически повторить миграцию, поскольку могла содержать
  исправление;
- существующая БД без обязательной таблицы не дополняется пустой таблицей молча:
  приложение остаётся на recovery-экране, сохраняя исходный файл;
- при успехе запускается обычное приложение;
- при повторной ошибке экран остаётся доступен;
- если до начала копирования не хватает свободного места, исходная БД не меняется,
  экран просит освободить место и позволяет повторить попытку;
- «Закрыть» завершает задачу приложения и не изменяет сохранённую копию.
- Облачное восстановление скрыто compile-time feature flag, который по умолчанию
  выключен. При выключенном флаге экран и локальный сценарий не меняются,
  фоновые upload-задачи не планируются, а ранее запланированные отменяются.
- При включённом флаге отдельная кнопка получает последнюю готовую резервную копию
  текущего авторизованного аккаунта. На время операции все действия блокируются.
- Скачанная копия проверяется по размеру, SHA-256 и внутреннему manifest, импортируется
  в три новые БД-кандидата и только затем устанавливается с журналом и rollback.
- Отсутствие токена, серверной копии или временная ошибка не меняют рабочие БД:
  экран остаётся открытым и показывает краткое сообщение без токена и PII.
- После успешной установки повторно выполняется bootstrap-проверка и приложение
  открывается только если восстановленные базы приняты.

### 25.3. `MigrationRecoveryScreen`: хранение состояния

Маркер ошибки хранится в Android `SharedPreferences` отдельно от `Route.db` и
содержит только номер сборки и класс технической ошибки. Содержимое маршрутов,
токены и PII в marker не записываются.

До открытия Route.db обезличенные recovery-события временно хранятся в отдельной
очереди `SharedPreferences`, ограниченной 50 элементами. Допустимы только заранее
заданные enum типов и причин; произвольный текст, route ID, токены, email и данные
аккаунта не принимаются. После успешного запуска события переносятся в обычный
diagnostic outbox и только после успешной записи удаляются из временной очереди.

Источник: `app/src/main/java/com/z_company/loco_driver/ui/MigrationRecoveryScreen.kt`,
`MigrationRecoveryBootstrap.kt`, `MainActivity.kt` (`showMigrationRecovery`),
`StartApp.kt` (`retryRouteMigration`, `restoreLatestCloudSnapshot`,
`cloudRecoveryEnabled` = `BuildConfig.RECOVERY_CLOUD_ENABLED`),
`recovery/RecoveryTelemetryQueue.kt`, `recovery/RecoverySnapshotRestoreCoordinator.kt`.

### 25.4. Проверка миграции Route.db

- До миграции и в проверенной backup-копии фиксируются полный набор route ID,
  количество несинхронизированных маршрутов и количества дочерних записей.
- После миграции эти значения должны совпасть, а число orphan-записей в дочерних
  таблицах не должно увеличиться. Уже существующий legacy orphan не блокирует
  обновление старого клиента. Любое новое расхождение считается провалом
  validation и приводит к восстановлению исходной базы из backup.

### 25.5. Очередь диагностических событий

- Готовая к отправке порция ограничена 20 событиями независимо от значения,
  запрошенного вызывающим кодом.
- Подтверждённые event ID атомарно получают `uploadedAt` и удаляются из outbox;
  повторное подтверждение безопасно.
- Ошибка планирует повтор с ограниченным exponential backoff (не более суток),
  а технический error code ограничивается 64 символами.
- Сетевой DTO события строится по явной allow-list. Локальный `detailsJson`,
  installation ID из отдельного события и любые route ID не копируются в DTO;
  общая идентификация установки добавляется только на уровне batch-запроса.

Источник: `domain/.../repositories/DiagnosticRepository.kt`,
`domain/.../entities/diagnostic/DiagnosticReportFormatter` (❓ реализация очереди событий
построчно не сверялась).

---

## Приложение. Где смотреть в коде (Android-эталон)

- Форма локомотива: `features/route/.../ui/FormLocoScreen.kt`,
  `viewmodel/LocoFormViewModel.kt`, навигация `navigation/FormLocoDestination.kt`.
- Шторка времени: `features/route/.../ui/TimeBottomSheet.kt`.
- Пикеры: `ui/StationPickerSheet.kt`, `ui/SeriesPickerSheet.kt`,
  общий ряд серии `ui/NormaTimeComponents.kt`.
- Редакторы норм: `ui/settings/SettingsStationEditorContent.kt`,
  `ui/settings/SettingsSeriesEditorContent.kt`,
  `viewmodel/StationNormEditorViewModel.kt`, `viewmodel/SeriesEditorViewModel.kt`.
- Сущности: `domain/.../entities/route/Locomotive.kt`,
  `domain/.../entities/norma_time/StationNorm.kt`, `LocomotiveSeries.kt`.

### Экраны и ViewModel'и (разделы 3–25)

Все экраны — в `features/route/src/main/java/com/z_company/route/`
(`ui/*Screen.kt` + `viewmodel/*ViewModel.kt`), навигация — `Router`
(`domain/navigation/Router.kt`, реализация `app/.../ui/navigation/RouterImpl.kt`).

| Раздел | Экран (`ui/`) | ViewModel (`viewmodel/`) |
|---|---|---|
| 4 Главный | `HomeScreen.kt`, `HomeStateBlocks.kt` | `home_view_model/HomeViewModel.kt` |
| 5 Маршрут | `FormScreen.kt` | `FormViewModel.kt` (`RouteFormUiState`, `SalaryForRouteState`) |
| 6 Поезд | `FormTrainScreen.kt` | `TrainFormViewModel.kt` (`TrainFormUiState`, `TrainFieldState`) |
| 7 Пассажиром | `FormPassengerScreen.kt` | `PassengerFormViewModel.kt` |
| 8 Прочая работа | `FormOtherWorkScreen.kt` | `OtherWorkFormViewModel.kt` |
| 9 Все маршруты | `AllRouteScreen.kt` | `all_route_view_model/AllRouteViewModel.kt` |
| 9.1 Быстрый просмотр | `component/RouteQuickViewSheet.kt` | — (использует `AllRouteViewModel`/`HomeViewModel`/`CalendarViewModel`) |
| 10 Поиск | `SearchScreen.kt` | `SearchViewModel.kt` |
| 11 Расчёт зарплаты | `SalaryCalculationScreen.kt` | `SalaryCalculationViewModel.kt` + KMP `domain/salary/SalaryCalculator.kt` |
| 12 Настройки зарплаты | `SettingSalaryScreen.kt` | `SettingSalaryViewModel.kt` |
| 13 Статистика | `StatisticsScreen.kt`, `util/StatisticsPdfGenerator.kt` (§13.5) | `StatisticsViewModel.kt` (`StatisticsUiState`, `StatFormat`) |
| 14 Календарь | `CalendarScreen.kt` | `CalendarViewModel.kt` |
| 15 Мастер «Заполнить месяц» | `ScheduleWizardScreen.kt` | `ScheduleWizardViewModel.kt` |
| 16 Отвлечения | `AbsenceScreen.kt`, `SelectReleaseDaysScreen.kt` (🚫) | `AbsenceViewModel.kt`, `SelectReleaseDaysViewModel.kt` |
| 17 Настройки | `SettingsScreen.kt` | `SettingsViewModel.kt` |
| 18 Профиль / вход | `ProfileScreen.kt`, `SyncProgressDialog.kt`, `TrashScreen.kt` | `ProfileViewModel.kt`, `TrashViewModel.kt` |
| 18.R Пригласить друга | `ReferralScreen.kt` | `ReferralViewModel.kt` |
| 19 Покупки | `PurchasesScreen.kt` (`PaymentDialog`), `SubscriptionLimitDialog.kt` | `PurchasesViewModel.kt`, `RouteActionsHelper.kt` |
| 20 Онбординг | `ui/login/FirstPresentationBlockScreen.kt` (🚫 мёртвый код, §20) | — (`app/.../MainViewModel.kt`) |
| 21 PDF-экспорт | `component/PdfContentDialog.kt`, `component/PdfActionSheet.kt`, `util/PdfGenerator.kt` | `PdfViewModel.kt` |
| 22 «Что нового» | `ui/UpdatePresentationBlockScreen.kt` | — |
| 19.5–19.6 Глобальные диалоги подписки | `ui/SubscriptionPeriodChangedDialog.kt`, `app/.../ui/LocoDriverApp.kt` | `SubscriptionNoticesViewModel.kt`, `subscription/PaymentReturnChecker.kt` |
| 25 Восстановление миграции | `app/.../ui/MigrationRecoveryScreen.kt` | — (`app/.../MainActivity.kt`, `StartApp.kt`) |

Реестр навигации (что зарегистрировано в NavHost) — `navigation/Navigation.kt`;
маршруты — `navigation/Routes.kt`, `navigation/*Destination.kt`.

- Общие компоненты форм: `component/AppBottomSheet.kt`, `AppInputBottomSheet.kt`,
  `ConfirmExitDialog.kt` (🚫), `AppAlertDialog.kt`, `ShoulderEditBottomSheet.kt` (плечо),
  `StationEditBottomSheet.kt` (станция поезда), `Passenger12hBottomSheet.kt`,
  `SearchSettingBottomSheet.kt`, `SyncProgressDialog.kt`.
- Доменные расчёты: `domain/.../util/` (`CalculateNightTime.kt`,
  `TimeCalculationContext.kt`), `domain/.../entities/route/UtilsForEntities.kt`
  (метрики маршрутов), use cases `NormaUseCase`, `RouteUseCase`, `CalendarUseCase`.
- Сущности разделов: `route/Route.kt`, `route/BasicData.kt`, `route/Train.kt`
  (+`TrainAssist`, `Station`), `route/Passenger.kt`, `route/OtherWork.kt`,
  `setting/SalarySetting.kt`, `setting/ServicePhase.kt`, `MonthOfYear.kt`,
  `Day.kt`/`ReleaseType.kt`.

---

## Приложение. Баги Android на проверку

Решение владельца: **в iOS поведение переносится как есть (паритет с Android)**, а
перечисленное ниже остаётся на ручную проверку. В тексте разделов каждый пункт помечен
«⚠️ Поведение Android (возможный баг, BUG-NN)». Исправление бага в Android обязательно
сопровождается правкой соответствующего раздела и этой таблицы (статус «исправлено» /
«не баг»), после чего то же исправление переносится в iOS.

Влияние: **деньги** — расчёт сумм; **данные** — сохранение, синхронизация, потеря или
порча данных; **UI** — отображение и тексты.

| ID | Раздел | Описание | Влияние | Статус |
|---|---|---|---|---|
| BUG-01 | §0.1, §2.8 | «Сохранить норму станции» в шторке времени записывает интервал без нормализации — может сохранить `0`/отрицательное (нарушение «0 = не задано») | данные | не проверено |
| BUG-02 | §1.1 | При сохранении локомотива список секций другого вида тяги пишется пустым — ранее сохранённые секции теряются | данные | не проверено |
| BUG-03 | §1.2 | Ввод нормы и счётчиков отопления/собственных нужд не запускает автосейв — риск потери ввода | данные | не проверено |
| BUG-04 | §1.2 | Дебаунс-автосейв локомотива не проверяет guard пустого объекта — возможна пустая запись | данные | не проверено |
| BUG-05 | §1.3 | Сообщения валидации секций локомотива не показываются; у электровоза сравниваются строки («9» > «10») | UI | не проверено |
| BUG-06 | §1.3 | Тепловоз без `k секции`: результат показывается как «ЭКОНОМИЯ +0» | UI | не проверено |
| BUG-07 | §2.1 | Шторка времени: пояс с минутами (`GMT+05:30`) не разбирается, времена форматируются в GMT+3 | UI | не проверено |
| BUG-08 | §3.1, §22 | Оверлей «Что нового» не скрывается после «Начать»/«Пропустить» до пересоздания Activity | UI | не проверено |
| BUG-09 | §3.2, §20 | `router.showSignIn()`/`showLogIn()` ведут на незарегистрированные маршруты — крэш при вызове (сейчас вызовов нет) | UI | не проверено |
| BUG-10 | §4.1, §4.6, §9.0 | Snackbar «Проверьте корзину в Настройках» — корзина находится в Профиле | UI | не проверено |
| BUG-11 | §4.2 | Главная: ошибка расчёта праздничных часов пишется в поле ночных часов | UI | не проверено |
| BUG-12 | §4.2 | `pushWidgetData`: `monthNames[month − 1]` при 0-based месяце (сейчас не проявляется) | UI | не проверено |
| BUG-13 | §4.3, §5.7 | Короткий отдых в ПО: «округление вверх» прибавляет целую минуту — окончание с `:30` с | UI | не проверено |
| BUG-14 | §4.3, §5.7 | Домашний отдых: цепочка маршрутов берётся из выбранного в UI месяца, а не месяца маршрута | UI | не проверено |
| BUG-15 | §4.4, §9.0.1 | Шторка месяца: несуществующая пара месяц/год молча не применяется (Главная, Все маршруты, Зарплата) | UI | не проверено |
| BUG-16 | §4.5 | `Route.isFuture` прибавляет к `now` смещение пояса — неверный фон карточки у пользователей не из GMT+3 | UI | не проверено |
| BUG-17 | §5.1 | Копия маршрута: `isCopy` не выставляется — старый заголовок, не открывается пикер явки, срабатывает шторка дубля | данные | не проверено |
| BUG-18 | §5.2 | `saveRoute` молча подставляет явку = текущий момент, если явка пуста | данные | не проверено |
| BUG-19 | §5.5, §5.7 | «Вторая ночь подряд» и «Фактический отдых» ищут соседние маршруты в выбранном месяце настроек, а не в месяце маршрута | UI | не проверено |
| BUG-20 | §5.6, §9.4 | Shared preview (`isDeleted` без `deletedAt`) виден в корзине, не удаляется по сроку, для него шлётся DELETE | данные | не проверено |
| BUG-21 | §6.1 | `isTrainEmpty` не учитывает вагонника — новый поезд только с вагонником теряется при быстром выходе | данные | не проверено |
| BUG-22 | §6.2 | Список станций поезда форматирует время в фиксированном GMT+3, шторка станции — в поясе пользователя | UI | не проверено |
| BUG-23 | §6.2 | Кнопка «GO» пишет время станции с секундами — нарушение инварианта «время кратно минуте» | данные | не проверено |
| BUG-24 | §6.2 | Время прицепки вагонника нельзя очистить | UI | не проверено |
| BUG-25 | §6.3 | Ошибки валидации поезда («Невозможно сохранить данные») не блокируют сохранение | данные | не проверено |
| BUG-26 | §6.4 | Новая станция: «Изменить данные поезда» предзаполняется исходной версией, а не текущими данными | данные | не проверено |
| BUG-27 | §6.6 | Редактор плеча: «₽/км» захардкожено, не зависит от страны | UI | не проверено |
| BUG-28 | §6.6, §24.3 | Удаление плеча в редакторе — без подтверждения | данные | не проверено |
| BUG-29 | §7.1 | «Пассажиром»: `onCleared` сохраняет данные с ошибкой валидации | данные | не проверено |
| BUG-30 | §8.5.2, §24.3 | Удаление напарника в редакторе — без подтверждения | данные | не проверено |
| BUG-31 | §9.0.1 | Фильтр «Длинные поезда» = любой поезд с У.Д. > 0, а не признак `isLongCompositionTrain` | UI | не проверено |
| BUG-32 | §9.4, §10.2 | Даты в корзине и в результатах поиска — в поясе устройства, а не в поясе отображения | UI | не проверено |
| BUG-33 | §9.4 | Диалог очистки корзины: «удалены N маршрутов» без склонения | UI | не проверено |
| BUG-34 | §10.2 | `EntityString.passengerStr`: «паccажиром» с латинскими «cc» | UI | не проверено |
| BUG-35 | §11.1 | Расчёт зарплаты: `timeStartWork!!` в фильтре будущих маршрутов — падение на маршруте без явки (❓) | UI | не проверено |
| BUG-36 | §11.3 | Смена тарифа внутри месяца: часть часов не оплачивается ни по тарифу, ни как сверхурочные; перерыв вычитается дважды | деньги | не проверено |
| BUG-37 | §11.3 | Переходящий маршрут: `List<Route>.getWorkTime` берёт часть месяца без вычета перерыва — завышает тариф и переработку | деньги | не проверено |
| BUG-38 | §11.6 | Карточка «Не установлена тарифная ставка» при смене тарифа проверяет старую ставку | UI | не проверено |
| BUG-39 | §11.6 | Столбец «%» у НДФЛ всегда `13,0`, независимо от настройки | UI | не проверено |
| BUG-40 | §11.9 | «Расчёт за смену»: ожидание 018M входит в итог, но строки нет — сумма строк меньше итога | деньги | не проверено |
| BUG-41 | §11.9 | «Расчёт за смену» считает по выбранному месяцу настроек, а не по месяцу маршрута (❓) | деньги | не проверено |
| BUG-42 | §12.2 | Настройки ЗП: поле старой ставки показывает ошибку по флагу новой; после выбора даты — «1234.0» | UI | не проверено |
| BUG-43 | §13.2 | Статистика «Заработано» без годовой переработки и соседей — расходится с «К выдаче» на экране зарплаты | деньги | не проверено |
| BUG-44 | §13.4 | Статистика: переработка в детализации — по стандартному профилю недели, в плашке — по выбранному | UI | не проверено |
| BUG-45 | §14 | Календарь, «Заменить» дубль: маршрут удаляется физически, без soft-delete — серверная копия остаётся (❓) | данные | не проверено |
| BUG-46 | §14.3, §16.4 | «Выходной» и `AbsenceScreen` не удаляют прежние отвлечения на те же даты — две записи на дату (❓) | данные | не проверено |
| BUG-47 | §16.4 | `AbsenceScreen`: «21 дней» — неверное склонение | UI | не проверено |
| BUG-48 | §16.4 | `AbsenceScreen`: после сохранения выбранным месяцем приложения становится последний затронутый месяц | UI | не проверено |
| BUG-49 | §17 | Настройки: финальное сохранение в `onCleared` без нового `updateAt` — правка может проиграть LWW | данные | не проверено |
| BUG-50 | §17, §17.2 | Смена страны не атомарна: при ошибке загрузки календаря страна/пояс/регион уже изменены | данные | не проверено |
| BUG-51 | §18.S | Опечатка «Регистрируем нового полььзователя...» | UI | не проверено |
| BUG-52 | §18.0 | Ошибка VK SDK на форме входа только логируется, пользователь её не видит | UI | не проверено |
| BUG-53 | §18.1 | «Последняя синхронизация» без единой синхронизации показывает текущее время | UI | не проверено |
| BUG-54 | §18.4 | `ProfileViewModel.startMigration` пишет в `Log.e` bearer-токен, `userId` и данные маршрутов | данные | не проверено |
| BUG-55 | §18.5 | Отчёт об ошибках синхронизации: для полной синхронизации тип «Загрузка с сервера» | UI | не проверено |
| BUG-56 | §18.5 | Легаси-миграция: прогресс настроек «1.0%», экран успеха недостижим, при ошибке помечается выполненной без повтора | данные | не проверено |
| BUG-57 | §19.2 | Превью «Станет до» прибавляет календарные месяцы, сервер — `periodDays` (расхождение 1–2 дня) | UI | не проверено |
| BUG-58 | §19.2, §19.4, §24.4 | Кнопка оплаты не блокируется — двойной тап запускает две оплаты | деньги | не проверено |
| BUG-59 | §21 | PDF: все времена в фиксированном GMT+3, а не в поясе отображения | UI | не проверено |
| BUG-60 | §21 | PDF: «Расход» отопления/собственных нужд = принял − сдал (в форме локомотива — наоборот) | UI | не проверено |
