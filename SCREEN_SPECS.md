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
  isWorkStartByArrival: Boolean = false,    // см. §7.2
  workStartOffsetMinutes: Int = 0           // смещение явки от прибытия, мин (§7.2); БД: Passenger.workStartOffsetMinutes
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
  // Данные локомотива (Settings.db миграция 26; сервер — миграция 056). Все необязательные: null = «не задано».
  brakeVolumeL?: Int,               // общий объём главных резервуаров, л
  axleCount?: Int,                  // число осей
  axleLoadTf?: Double,              // нажатие на ось, тс
  weightT?: Double,                 // вес, т
  sectionCount?: Int,               // количество секций (1–9); null = запись ещё не дополнена значениями по умолчанию
  updatedAt: Long = now
)
SectionNumberingType = { NUMERIC, LETTERS }   // «Секция 1/2/3» | «Секция А/Б/В»
```

```
Partner(                     // запись справочника напарников
  partnerId: String = generateId(), fullName: String = "",
  tabNumber?: String,        // табельный номер (строка: ведущие нули)
  notes?: String,
  isPermanent: Boolean = false, // «постоянный напарник»: не больше одного в справочнике
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
  passengerWagonLengthMeters: Double = 24.5,
  lastEnteredWorkStartOffsetMinutes: Int = 1          // запомненное смещение «явки по прибытию», мин (§7.2)
)
ServicePhase(id = generateId(), departureStation, arrivalStation: String,
             distance: Int /* км */, linearMileageRate: Double = 0.0 /* руб/км */,
             surchargeHeavyTrainsList: [SurchargeHeavyTrains] = [] /* свои ступени плеча; пусто = общие из настроек ЗП */,
             surchargeLongTrainsList: [SurchargeLongTrains] = [])
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
- **Порядок секций является частью данных и не меняется самопроизвольно.** Пользователь
  меняет его перетаскиванием: долгое нажатие в любом месте карточки секции → секция едет
  за пальцем, соседи меняются местами на половине высоты (`moveDieselSection` /
  `moveElectricSection`, нумерация «Секция N» — по новому порядку; считается изменением
  формы).
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
     ставит серию, **применяет `type` записи справочника**, её
     `sectionNumberingType` и **число секций**: у списка секций нужного вида становится
     ровно `LocomotiveSeries.sectionCount` (запись справочника; нет записи или поле пусто —
     по правилу «первая цифра названия», иначе 1; 1–9) секций — недостающие добавляются
     пустыми, лишние убираются только пустые с конца (заполненные секции не теряются). У каждой строки —
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

**iOS (реализация, ios/loco).** Логика — Kotlin `LocoScreenIosViewModel` (экран) и `LocoTimeSheetIosViewModel` (шторка §2), Swift только показывает. Отличия от Android: (1) перестановка секций — пункты «Выше/Ниже» в контекстном меню карточки (долгое нажатие), а не перетаскивание; (2) автоподбор числа секций по серии (`sectionCount`) не реализован — поля нет в master (есть в ветке `feature/loco-brake-data`), выбор серии гарантирует минимум одну секцию; (3) настройки iOS пока хранятся в памяти (`IosSharedPreferencesRepository`): блок «ДРУГОЕ» по умолчанию скрыт, как в коде Android; (4) валидация секций (BUG-05) сообщений не показывает; (5) нормы и счётчики отопления/собственных нужд автосохраняются так же, как остальные поля (отклонение от BUG-03), плюс немедленное сохранение при сворачивании приложения и по «Готово»; (6) подтверждение удаления секции — центральный alert «Удалить секцию?», как в форме маршрута; открытая свайпом карточка закрывается тапом в любом месте; (7) Dynamic Type не поддерживается дизайн-китом (шрифты фиксированного размера) — раскладка «столбиком» при крупном шрифте не реализована.

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
- Локальная запись — через `replaceAll` (весь список справочника; пропавшие из списка станции
  получают надгробия, см. §17.4 «Синхронизация справочников»). Это не про сервер.
- Выбранная станция в шторке синхронизируется со справочником: если нормы
  правятся в редакторе пикера (или «Настроить станцию»), шторка сразу
  подхватывает свежие нормы — без переоткрытия.
- **Синхронизация справочника станций** (`/v1/norma_time/stations/`): не full-replace, а
  слияние с сервером при каждой синхронизации — по каждой станции побеждает запись с
  большим `updatedAt`. Удаление станции (в Настройках, пикере) оставляет локальное
  «надгробие» (`StationTombstone`, SettingsDB v25) и уходит на сервер как элемент с
  `isDeleted = true` (`updatedAt` = момент удаления); так станция, добавленная на другом
  устройстве, не стирается, а удалённая не возвращается. Надгробия старше 180 дней
  вычищаются. Станция, изменённая после удаления, оживает.
  Станции с одинаковым названием (без учёта регистра и лишних пробелов), добавленные на
  разных устройствах, схлопываются в одну: остаётся самая заполненная, недостающие поля
  берутся у остальных, лишние id становятся надгробиями, а ссылки маршрутов (станция
  приёмки/сдачи локомотива) перенаправляются на оставшуюся станцию.
- **Серии локомотивов** (`/v1/norma_time/locomotives/`) синхронизируются так же: слияние
  по серии, надгробия при удалении, дубли с тем же названием и типом схлопываются
  (маршруты ссылаются на серию по названию, перенаправлять нечего).

### 2.9. Удаление значения времени

Долгое нажатие на строку с заданным временем → нижняя шторка `AppBottomSheet`
с заголовком-названием строки и действием **«Удалить значение»** (как «время
явки» в FormScreen). Явка (locked) не удаляется; для пустой строки long-press
ничего не делает. Для «Окончание работы» удаление также сбрасывает
предложение «Обновить …» и сразу синхронизирует маршрут
(`onTimeEndWorkChanged(null)`).

### 2.10. Пикеры серии/станции (внутри шторки)

`SeriesPickerSheet` / `StationPickerSheet` — вложенные нижние шторки;
на iOS — на всю высоту (`.large`), открываются сразу полностью, без видимой ручки:
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

**iOS (реализация, ios/loco).** Пикер — на всю высоту; в редакторах серии и станции (и в Настройках, и в пикере) верхней плашки-подсказки нет, поля «Данные локомотива» — как «Километраж» у станции: подпись слева, значение справа (моно 15/500, без плашки и фона), единица измерения сразу после значения, правый край значений на общем отступе строки; иконок в строках норм серии и станции нет; курсор при входе в поле — в конец значения. Шторка — `LocoTimeSheetView`, пикеры и редакторы серии/станции — `LocoRefPickerSheet`; расчёты якорей, валидация, предупреждения и сохранение норм — Kotlin `LocoTimeSheetIosViewModel`. Редакторы серии/станции в пикере — **те же вью, что в Настройках** (`SeriesEditorContent` / `StationEditorContent`, `Screens/Settings/Norms/`), логика — Kotlin `SeriesEditorIosViewModel` / `StationEditorIosViewModel`; переименование выбранной серии и её удаление возвращаются в форму локомотива (`seriesRenamed` / `seriesRemoved`, §2.12). «Настроить серию/станцию» без записи в справочнике ведёт на экран списка «Серии»/«Станции» в Настройках.

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
  повторный тап по текущей вкладке ничего не делает. **Общее правило видимости:** меню видно только на
  главном экране корневой вкладки — Главная, Зарплата, хаб Настроек (`SettingsSubScreen.HUB`), хаб Профиля. Как только
  пользователь ушёл глубже основного экрана, меню скрыто: (а) любой отдельный маршрут (Мои треки, Данные и
  конфиденциальность, Все маршруты, формы, подписка и т.д. — их нет в списке корневых маршрутов `LocoDriverApp`);
  (б) любой подэкран Настроек (Норма, Плечи, редакторы серий/станций и т.д. — вложены в `SettingsScreen` без своего
  маршрута); (в) экраны, открытые из хаба Профиля поверх него без своего маршрута (Корзина маршрутов, Рефералы).
  Вложенные экраны объявляют это через `HideBottomMenu(hide)` (`NestedScreenBarState` — счётчик скрывающих экранов,
  поэтому смена вкладки не «возвращает» меню раньше времени). Режимы поверх основного экрана (множественный выбор,
  §9.2, §17.8) — на экранах без меню; их нижняя панель действий занимает место меню. На iOS — 4 вкладки + отдельная
  кнопка «+» с тем же действием: у каждой вкладки свой `NavigationStack` (стек
  сохраняется при переключении), плавающий таб-бар `DSTabBar` виден только на корневом
  экране вкладки (`AppCoordinator`, `docs/IOS_UI_GUIDE.md` §5).
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
`SettingsScreenRoute`, `ProfileRoute`,
`StatisticsRoute`, `CalendarRoute`, `ScheduleWizardRoute`, `AbsenceRoute`.
`DetailsRoute`,
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
7. **Тексты ошибок**: сырые сообщения локальной БД («no such table/column», SQLite…)
   пользователю не показываются — вместо них общий текст экрана («Ошибка», «Ошибка
   синхронизации» и т.п.; Android: `isTechnicalDbErrorMessage` в `NetworkUtils`).
   Справочники станций/серий при сбое локальной схемы пропускаются в синхронизации,
   а при открытии БД настроек выполняется самопроверка схемы (недостающие таблицы и
   столбцы достраиваются идемпотентно).

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
iOS: решение считает `NewRouteIosViewModel.onNewRouteClick` (те же правила), диалоги —
`AppRouter.newRouteGate` (алерт в `AppCoordinator`), «Оформить подписку» — через гейт
покупок `AppRouter.showPurchases(isAuthorized:)`.

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
  двусторонняя синхронизация. Индикатора загрузки нет (пользователь работает и без сети);
  модального диалога нет. Нет сети (транспортный сбой, `NetworkErrorMapper.isConnectivityError/Message`) →
  тоста нет, в верхней строке слева от поиска горит красная иконка сети (`ic_offline_24px`, wifi-off;
  `HomeUiState.isOffline`); тап по иконке — snackbar «Нет связи с сервером»; следующая успешная фоновая синхронизация её гасит. Прочая ошибка → snackbar с понятным
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
    удерживаемая увеличивается до 1,04, не покачивается и следует за пальцем; когда
    она заходит на половину соседней плитки, они меняются местами (отклик), а смещение
    компенсируется на ширину соседа + промежуток — без прыжков (`detectReorderDrag` +
    `reorderStep`; ключ жеста — только тип плитки, поэтому включение режима посреди
    жеста его не обрывает). В режиме плитку можно тянуть сразу, без долгого нажатия. Режим остаётся после отпускания —
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
8. **«ИНСТРУМЕНТЫ»** — вертикальный список полноширинных карточек высотой как
   свёрнутая карточка маршрута (65 dp на Android/iOS, с тем же скруглением): слева
   иконка, справа название инструмента моноширинным шрифтом карточки маршрута.
   Карточки имеют горизонтальный отступ 8 dp; между основными блоками Главного экрана
   используется единый вертикальный зазор 32 dp. «Календарь» → §14; **«Карта»** (иконка карты, сразу
   после «Календаря») → если есть текущий маршрут и в нём есть поезд — карта этого поезда (§6.9) открывается
   НАПРЯМУЮ поверх Главного (`HomeTrainMap`, без перехода и мелькания формы «Поезд»; данные — `TrainFormViewModel` с id поезда в
   собственном хранилище, очищается при смене поезда и закрытии карты); поезд выбирается **по активности**
   (`TrainOrdering.pickHomeMapTargetByActivity`, 28.10.2026): (1) поезд, который пишется сейчас («Автозапись» включён и цель — поезд этого
   маршрута); (2) поезд с самым свежим записанным треком (по `endedAt` треков `Track.db`); (3) иначе правило «Автозапись»
   (`pickAutoTarget`: последний пустой, иначе последний в порядке UI); данные не
   создаются; **если в текущем маршруте 2+ поездов — на карте сверху под шапкой ряд чипов-переключателей** (§6.9); нет текущего маршрута или в нём нет поездов — только карта с текущим положением
   (§6.9, состояние 1; геолокация по необходимости, ничего не записывается, пустой поезд не
   создаётся; кнопка записи на карте: поезд в текущем маршруте; нет поезда - создаёт поезд; нет маршрута - трек без поезда, §6.9). «Назад» с карты возвращает на Главный (форм нет). «Статистика» → §13;
   «ВУ-45» (третья, после «Карты», перед «Статистикой»; иконки плиток двухтонные: бледный корпус + яркие детали) → §26 с предзаполнением из поезда по правилу «Автозапись» и последнего локомотива текущего маршрута; «PDF» → выбор содержимого PDF (§21; карточка неактивна, пока PDF
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
  удаления → шторка «Удалить маршрут?» / «от dd.MM HH:mm» / «Да, удалить» (если у поездов маршрута есть записанные треки в `Track.db` — между заголовком и кнопками строка «Будут удалены и записанные треки.», §6.9).
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

iOS: расчёты и форматирование — `iosApp/src/commonMain/.../viewmodel/HomeScreenIosViewModel.kt`
(перенос `HomeViewModel` + `PullToSyncViewModel` + расчётов отдыха `RouteActionsHelper`, модели —
`HomeScreenIosModels.kt`), экран — `iosApp/iosApp/Screens/Home/*`. Отличия платформы: порядок плиток
«Текущий маршрут» хранится в `UserDefaults` (`home.currentRouteBlockOrder`); проверки обновления
RuStore и виджета на iOS нет; тикер живых блоков — в ViewModel (граница минуты, для отдыха — секунда).

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
    `timeStartWork` принудительно = его прибытию + `workStartOffsetMinutes` минут
    (`Passenger.workStartTime`, §7.2).
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
     - «Ожидание до явки» · `HH:MM` — зазор «прибытие → явка» при «явке по прибытию»
       больше 1 мин (`getWorkStartWaitingInterval()`), с разделителем снизу. Сумма трёх
       строк («Пассажиром до явки» + «Ожидание до явки» + «Отработано») = отработанное
       время маршрута в списке/месяце (`getWorkTime()`);
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
     станция (если одна) или «{первая} - {последняя}» (если несколько), а если
     станций с названием нет — «Поезд № {k}»; пассажир — «№ {номер} {откуда} - {куда}», если все три пусты —
     «Пассажиром № {k}»; прочая работа — «{тип или «Прочая работа»} · {станция}»,
     если оба пусты — «Прочая работа № {k}».
   - Порядок: локомотивы с `timeStartOfAcceptance` — по возрастанию, затем без
     времени в порядке добавления; поезда — по `stations.first().timeDeparture` так
     же; пассажиры и прочая работа — в порядке из БД.
   - Подтверждения удаления: «Удалить локомотив?» / «Локомотив будет убран из
     маршрута. Это действие нельзя отменить.»; «Удалить поезд?» / «Поезд будет
     убран…» (+ строка «Будут удалены и записанные треки.» под текстом, только если по поезду есть записанные треки, §6.9); «Удалить запись?» / «Запись прочей работы будет убрана…»; «Удалить
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
  «Ожидание пассажиром» (018M; `ЧЧ:ММ × ставка/ч`, оплата по тарифу без надбавок зоны),
  по строке «Доплата за пробег» на каждое плечо («{плечо}: {км} км × {ставка}/км»),
  (в расчётном листе, если плеч с 011L больше одного, это одна обычная строка «Доплата за пробег» с суммой по всем плечам, а под ней мелким курсивом «станция 1 - станция 2 ... сумма» по каждому плечу: без кода и разделителей между ними, суммы по правому краю колонки; общая сумма в «Всего начислено» считается как раньше; с одним плечом - прежняя строка «Доплата за пробег: {плечо}»),
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

**iOS (волна `ios/route-form`)**: форма — `Screens/Form/FormView.swift` + Kotlin `RouteFormIosViewModel` (Koin `factory`, по экземпляру на форму).
Отклонения от Android: шестерёнка «Настройки маршрута» — слева в нижней панели (как в PWA), иконка «Прочая работа» — гаечный ключ; строки «Явка/Сдача/перерыв» — как в PWA (подпись слева, значение справа `дд.мм.гггг чч:мм` акцентным mono, пусто — серое «Выбрать»); пикер времени — нативный барабан (без календаря; recent-times — чипами, на iOS хранятся только до перезапуска); подтверждения — центральный диалог `RouteConfirmDialog`; панель скрывается по направлению жеста прокрутки; свайп напарника — жест с подтверждением; удаление поезда не удаляет треки (их на iOS нет).

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
сводка вспомогательной тяги → сводка вагонника → ряд «Автозапись · секундомер · GO» →
заголовок «МАРШРУТ» + кнопки сортировки и перегонов (круглые мягкие фоны, область нажатия 48dp) → список станций → пилюли сводки. Поверх —
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
   - Нули из БД/синхронизации (сервер пустые вес/оси/У.Д. хранит как 0 и отдаёт «0»/«0.0») при открытии
     формы превращаются в «не задано» — поля пустые (также в версиях данных состава).
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
   - **Тап по строке** — шторка станции; **долгое нажатие в любом месте станции** — сразу
     перетаскивание одним жестом (`detectReorderDrag`, общий для поезда, секций локомотива и
     плиток текущего маршрута): станция едет за пальцем вместе с перегоном под ней, сосед
     меняется местами, когда она заходит на половину его высоты; перегоны видны всё время и
     после отпускания пересчитываются по новому порядку; `moveStation(from, to)` — с учётом
     обратного порядка отображения. Без долгого нажатия палец прокручивает экран. **Свайп влево** (вне режима
     перестановки) → красная кнопка удаления → шторка «Удалить станцию {название}?»
     (без названия — «Удалить станцию?»), «Да, удалить»; отмена закрывает свайп.
   - Строка станции: точка таймлайна, название (2 строки, многоточие; пустое — «—»),
     под ним «проходная · путь {N}» (у проходной с заполненным путём — оба), «проходная» или «путь {N}»; справа колонки ПРИБ · бейдж стоянки · ОТПР
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
   - Кнопки сортировки (↕) и перегонов справа от заголовка «МАРШРУТ» — по кругу (36dp, заливка `tertiary` с alpha 0.14, иконка
     `onSurface`; контраст иконки 15.1/16.7:1 в светлой, 14.0/9.9:1 в тёмной), область нажатия 48dp, зазор 16dp между областями нажатия. Правый край правого видимого круга совпадает с правой границей блока «МАРШРУТ» и карточек станций (область нажатия выступает на 6dp за край через offset).
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
   и сразу при возврате на экран. **Проходные станции (`isPassingStation`) в секундомере
   не участвуют** — их время проследования не считается ни стоянкой, ни отправлением;
   отсчёт идёт от последней непроходной станции. Так же и кнопка «▶/⏸» (GO): проходные станции не учитываются ни при поиске последнего заполненного времени, ни как цель следующего прибытия — остановка пишется на следующую непроходную станцию (или создаётся новая). При равном времени прибытия и отправления событием
   считается отправление. **Если последнее событие —
   прибытие на станцию, совпадающую с конечной станцией выбранного плеча, чип стоянки
   не показывается** (маршрут по плечу завершён — стоянку на конечной станции не считаем).
   То же и для станции с флагом `isFinalStation`: прибыли на неё — чип стоянки скрыт.
6а. **Кнопка «Автозапись»** (решение 07.10.2026; Android реализован в ветке `feature/auto-record`). Переключатель в ряду над
   списком станций: «Автозапись» слева, чип секундомера строго по горизонтальной середине ряда, «GO» (▶/⏸) справа (три равных слота;
   форма/высота у всех трёх — `AutoRecordStyle`). Включена — стоянки и отправления записываются сами.
   - **Плашка плохой связи (30.10.2026, `AutoGnssNotice`).** Под рядом «Автозапись» (на Главном — под строкой «ТЕКУЩИЙ МАРШРУТ» + чип, в «Поезде» —
     под рядом «Автозапись · секундомер · GO») появляется плашка, **только пока «Автозапись» включён** и сглаженный уровень GPS (`GpsRecorder.quality`,
     §6.9) — не хороший: «Слабый сигнал GPS» (стоянки и отправления могут записаться неточно), «Нет сигнала GPS» (запись приостановлена),
     «Помеха GPS» (координаты скачут, такие точки не учитываются). Цвет акцента — как у индикатора связи (жёлтый/красный/оранжевый).
     При хорошем сигнале и «поиске спутников» плашки нет; появляется/исчезает анимацией.
   - **Платформа:** только Android; iOS — позже отдельной задачей; PWA — нет (в вебе нет фоновой геолокации).
   - **Источник — скорость GPS (основное).** Остановки бывают и вне станций, поэтому событие определяется по скорости, а не по
     станции: скорость < 2 км/ч дольше 60 с → **прибытие**; после остановки скорость > 5 км/ч дольше 30 с → **отправление**.
     **Поправка на задержку детектора:** время события — момент, когда остановка/движение реально началось, а не когда детектор
     подтвердил. Прибытие — первый отсчёт < 2 км/ч, после которого скорость уже не росла (начало окна). Отправление — откат
     по буферу последних отсчётов (только в памяти, 300 шт., на диск не пишется) до **последнего отсчёта со скоростью ≤ 1 км/ч**
     перед ростом (откат не дальше 2 мин, не раньше начала текущей стоянки и не через дыру GPS > 30 с; иначе — начало окна).
     Время в запись — **округление вниз до минуты** (§3.3 п.6): стоянка/трогание не «переносятся» в будущее. Любая точка
     вне порога (≥ 2 км/ч при ожидании стоянки, ≤ 5 км/ч при ожидании отправления), пропажа GPS дольше 30 с или точка с
     точностью хуже 50 м сбрасывают окно ожидания (точка с плохой точностью игнорируется и сбрасывает окно). **Потеря сигнала
     (уровень LOST, §6.9 «Качество сигнала GPS») не принимается за стоянку:** при переходе в LOST менеджер вызывает
     `StopDetector.onSignalLost()` — окно «прибытие/отправление» сбрасывается, события не генерируются; после возвращения
   - **Отправление сразу после включения «Автозапись» (29.10.2026).** Детектор при включении «взводится» (`StopDetector.arm()`): если первый достоверный отсчёт — стоянка (< 2 км/ч), он запоминается как точка покоя, и отправление (30 с > 5 км/ч) засчитывается без подтверждённого прибытия (60 с) — машинист включил запись на стоянке и сразу тронулся; время — последний отсчёт ≈ 0. Если в первые 3 минуты были «Помеха GPS»/потеря сигнала (`markStartObscured()`) и первый достоверный отсчёт уже на ходу — время отправления оценивается назад от него на время разгона (скорость / 0,3 м/с², не более 60 с). Без помехи быстрый первый отсчёт отправлением не считается. Запись в поезд по-прежнему решает `AutoTrainWriter` (отправление пишется только на первую станцию без времён или после прибытия).
     сигнала окно начинается с первого достоверного отсчёта (паузу в данных детектор стоянкой не считает; время прибытия — не из
     дыры), а откат времени отправления не уходит через дыру в данных. Точка трека «стоянка» (скорость < 2 км/ч или неизвестна)
     при потере сигнала/точности хуже 50 м не пишется (`TrackWriter.onPoint(signalOk = false)`; точка в
     движении пишется как обычно). Тот же детектор и пороги использует анализ записанного трека (§6.9). Точки без значения скорости игнорируются. Пороги — константы
     (`StopDetectorConfig`; уточнятся по реальным поездкам). Станция — вторичный признак: если точка остановки в радиусе
     1 км от станции (справочника или плеча, выбор — единая функция §6.6, `pickForStop`: при плече учитываются и станции справочника на линии плеча, §6.6 п.2; без плеча — станция по рельсам с сервера; кандидаты — станции справочника в 2 км, §6.6 п.3) с координатами (§17.4), подставляется её название; **вне станций — название
     пустое** (пользователь впишет сам; остановка не теряется). Логика — чистый код `domain/commonMain` (`StopDetector`).
   - **Запись в поезд** — по правилам «GO» (прибытие → отправление → следующая станция), но событие записывается, только если
     оно совпадает с тем, что «GO» поставил бы следующим: «прибытие» — когда следующим идёт прибытие, «отправление» — когда
     отправление; несовпавшее событие (например, прибытие, когда стоянка уже отмечена вручную) пропускается. Уже заполненные
     времена и названия не перезаписываются. Название проставляется только новой станции и только если оно ещё пусто.
     **Целевой поезд — тот, где нажата кнопка «Автозапись»** (решение 07.10.2026): при включении из экрана «Поезд» id этого поезда
     запоминается в состоянии менеджера (`SharedPreferences`, рядом с временем автовыключения) и все события пишутся в него, пока Автозапись
     включено — даже если открыта форма другого поезда (если открыта форма именно этого поезда — запись через неё). При включении с
     главного экрана или из виджета (контекста поезда нет) цель определяется **один раз в момент включения** по правилу
     `TrainOrdering.pickAutoTarget` (domain): (1) поезд без единого времени станций (новый пустой; если таких несколько — последний
     в порядке UI); (2) иначе последний в порядке UI; (3) в текущем маршруте поездов нет — создаётся пустой поезд (`resolveRecordTarget`), его id запоминается; **текущего маршрута нет — маршрут не создаётся, цель = `NO_TRAIN_ID`:
     пишется только трек «без поезда» (§6.9), события станций отбрасываются; при появлении текущего маршрута цель переходит на его поезд**; дальше цель зафиксирована. **Запомненный поезд ищется среди поездов ВСЕХ неудалённых маршрутов**
     (`TrainOrdering.findTargetTrain`), не только текущего: маршрут цели может быть любым (в том числе ещё идущим/будущим) —
     события станций и точки трека пишутся строго в него, уведомление «текущего маршрута нет» при найденной цели не показывается.
     **Только если поезд цели удалён или цель не задана** — тихий fallback на правило `TrainOrdering` по текущему маршруту
     (без уведомления), выбранный поезд запоминается как новая цель (`TrainOrdering.pickTarget`).
   - **Конечная станция плеча:** если у поезда выбрано плечо и станция прибытия (по справочнику, радиус 1 км) совпадает по
     названию с конечной станцией плеча (без учёта регистра, е/ё, дефиса/пробелов, как у «GO»), прибытие записывается в неё,
     а не в новую промежуточную станцию. Без плеча поведение прежнее. Запомненный id сбрасывается при выключении Автозапись. Порядок UI — `TrainOrdering.sortForDisplay`
     (общий с FormScreen): поезда с временем отправления первой станции — по возрастанию, затем без него — в порядке добавления. Запись идёт прямо в БД
     (не через форму), поэтому работает при закрытом экране «Поезд»; если экран открыт, он подхватывает изменения.
   - **Автовыключение через 12 ч** после включения (забыли выключить после поездки); уведомление «Автозапись выключено: прошло 12 часов».
     «GO» при включённом Автозапись работает как обычно; ручная правка времени станции Автозапись не перезаписывает. Состояние (вкл/выкл,
     время включения) хранится на устройстве (`SharedPreferences`), на сервер не уходит. После перезапуска процесса при
     включённом и не истёкшем состоянии запись возобновляется (служба `START_STICKY`).
   - **Включение** требует разрешения на точную геолокацию (запрашивается при тапе; фонового доступа не нужно — служба
     стартует, пока приложение на экране). Уведомления (Android 13+) запрашиваются вместе; отказ от них запись не блокирует.
     Если GPS выключен в системе — запись стоит и возобновится сама, индикация остаётся.
   - **GPS-запись — отдельный переиспользуемый компонент** (`core_android`, пакет `com.z_company.core.gps`: `GpsRecorder`,
     `GpsRecordingService`, `GpsPoint`, `GpsPermissions`; на `LocationManager`, без Google Play Services). Контракт: поток точек
     {время, lat, lon, скорость, точность}, `start/stop`, foreground service (тип location), разрешения. Без знания про поезд и
     навигатор: «Автозапись» — его первый потребитель; навигатор (§33) позже переключится на него. Точки трека «Автозапись» пишет в локальное
     хранилище ТОЛЬКО при согласии №1 (§6.9, §17.7), на сервер не отправляет (только события стоянок/отправлений); без согласия
     трек не пишется, при включении «Автозапись» один раз показывается уведомление «Трек не будет сохранён: нет вашего согласия».
     При ПЕРВОМ включении «Автозапись» (пока решение не принято) перед запросом разрешений открывается экран согласия (§6.9);
     кнопка «Отмена» на нём отменяет включение. Состав экрана (вводный текст, кнопки над переключателями) - §6.9, «Знакомство с картой и «Автозапись»». Координаты не логируются.
   - **Живучесть службы** (прошивки Xiaomi/Huawei/Samsung убивают фоновые службы). (1) При включении Автозапись (один раз, а также после
     «прервано») — диалог «Чтобы Автозапись не отключалось»: кнопка «Разрешить» открывает системный запрос исключения из оптимизации
     батареи (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), «Настройки приложения» — карточку приложения; текст-инструкция: на Xiaomi,
     Huawei, Samsung дополнительно включить «Автозапуск» и в батарее выбрать «Без ограничений». Включение не ждёт ответа.
     (2) Heartbeat — время последней точки в `SharedPreferences` (запись не чаще раза в 20 с). (3) Пока Автозапись включено,
     WorkManager (`AutoRecordWatchdogWorker`, период 15 мин; включается/отменяется менеджером) вызывает `checkAlive()`:
     если службы нет — перезапуск и ожидание ~20 с; не поднялась (Android 12+ не даёт стартовать location-службу из фона) —
     состояние **«прервано»**: уведомление «Автозапись остановлено системой. Включить снова» (тап открывает приложение и включает
     Автозапись; геолокация уже выдана), чипы показывают «Автозапись · прервано» оранжевым, тап по чипу включает заново (и снова
     показывает диалог). Если служба жива, а точек нет > 10 мин — мягкий перезапуск подписки без «прервано» (в тоннелях GPS
     пропадает законно). Тот же перевод в «прервано» (вместо молчаливого выключения) — при неудачном старте службы и при
     восстановлении после убийства процесса. Выключение пользователем / автовыключение сбрасывают «прервано».
     **Перезапуск приложения (этап 4в).** `restore()` в `Application.onCreate` может отработать, когда процесс поднят в фоне (после
     `force-stop`/обновления, виджет, воркер): `startForegroundService` из фона запрещён (Android 12+, лог «Background started FGS:
     Disallowed»), раньше через 15 с Автозапись уходило в «прервано» даже при открытом приложении. Теперь `MainActivity.onStart` →
     `AutoRecordManager.onAppForeground()`: если Автозапись «включено», а службы нет — старт повторяется с переднего плана (разрешён); если уже
     «прервано», время работы ещё не вышло (`interrupted_until_ms`) и есть геолокация — запись возобновляется без нажатия «Автозапись».
     Состояние детектора («стоим/едем» и момент) сохраняется в `auto_record` (`detector_stopped*`) при каждом событии и восстанавливается,
     если пауза записи ≤ 10 мин (`StopDetector.seed`): раньше новый детектор считал «неизвестно = едем», терял отправление и заново давал
     прибытие. На узком экране (< 170 dp под чип, ~343 dp экран формы «Поезд») чип «Автозапись · прервано» двухстрочный («Автозапись» / «прервано»,
     12/10 sp), а не обрезается.
   - **Автовыключение по цели (31.10.2026, `AutoRecordManager.checkTarget`).** Подписка на список маршрутов; «Автозапись» выключается
     (запись трека закрывается, уведомление с причиной), когда: (1) в поезде-цели записано прибытие на конечную станцию - станция с флагом
     «конечная» или при выбранном плече последняя станция (`AutoTrainWriter.hasTerminalArrival`; если прибытие уже стояло к моменту
     первого наблюдения цели - не выключаем); (2) поезд-цель удалён, в том числе вместе с маршрутом (сам поезд не переносится в другой,
     как раньше). Новый несохранённый поезд открытой формы удалённым не считается. Срабатывает и при ручной правке времени.
   - **Нет текущего маршрута / работа окончена, а Автозапись включено:** события отбрасываются (писать некуда), но не молча —
     уведомление «Автозапись включено, но текущего маршрута нет (работа окончена?)» с действиями «Выключить» и «Открыть приложение»;
     не чаще раза в 30 мин. Проверяется при каждом событии и при проверке живучести.
   - **Часовой пояс «до чч:мм»** (только уведомление службы; на чипах и виджетах времени нет) — по `UserSettings.displayTimeZone()` (РФ/Беларусь — МСК,
     Казахстан — свой пояс, §0.1), а не по часам устройства; пояс кэшируется в `SharedPreferences` менеджера (виджеты читают оттуда).
   - **Пустой поезд:** первая стоянка в начале маршрута не пишется; при пустом поезде записывается ОТПРАВЛЕНИЕ (создаётся первая
     станция с временем отправления и, если рядом станция справочника, её названием); дальше — как обычно.
   - **Поезд создаётся сразу и один раз (10.10.2026).** Если поезда-цели нет (маршрут без поездов, цель удалена), поезд
     создаётся `RecordTargetResolver` под мьютексом и с ожиданием записи в БД — включение Автозапись, первое событие и кнопка записи на
     карте не создают второго поезда (второй вызов видит пустой созданный поезд, `TrainOrdering.pickAutoTarget`); форма маршрута, «Поезд»
     и Главный видят его сразу через подписку на БД. Если цель — несохранённый поезд открытой формы «Поезд» (новый), события пишутся
     через форму (раньше fallback создавал другой поезд, а форма потом сохраняла свой — в маршруте оказывалось два).
   - **Единственное активное «Автозапись» (10.10.2026).** Режим пишет один поезд (`target_train_id`). Чип «Автозапись» и кнопка записи на карте на экранах
     ДРУГИХ поездов показываются выключенными (сравнение id; без контекста поезда — Главный, виджет — общее состояние). Включение в другом
     поезде при идущей записи: диалог «Запись уже идёт» — «Идёт запись трека поезда № X. Если продолжите, она будет прервана.» (X — номер;
     пустой номер — «Лужская — Гатчина» по станциям; иначе «поезда без номера»; трек без поезда — «трека без поезда»), кнопки «Продолжить»
     (цель переключается на этот поезд: текущий трек закрывается, следующая точка открывает новый) и «Отмена». Кнопка «Сделать целью»
     в пустом графике (§6.9), когда пишется другой поезд, показывает тот же диалог «Запись уже идёт» (`AutoTargetRules.conflictsWith`/`conflictText`; «Продолжить» переключает цель, «Отмена» ничего не меняет); без конфликта переключает цель сразу.
   **Индикация состояния (кнопка = переключатель + индикатор):**
   - **Вид кнопки (решение 07.10.2026):** времени окончания («до чч:мм») на кнопке/чипе НЕТ нигде — оно только в уведомлении службы.
     Цвета: **включено — зелёный** (светлая тема: фон `#15803D`, текст/точка `#FFFFFF`; тёмная: фон `#4ADE80`, текст/точка `#0A0E14`),
     **выключено — серый** (светлая: фон `#E7E9EC`, текст `#4A515A`, контур `#7A828B`; тёмная: фон `#3A3C40`, текст `#C9CDD2`,
     контур `#8A929B`), **«прервано» — оранжевый** с подписью «Автозапись · прервано» (светлая: фон `#FFE8CC`, текст `#8A4B00`, контур
     `#C26A00`; тёмная: фон `#4A3410`, текст и контур `#FFB547`). Контраст WCAG: текст/иконка к фону кнопки ≥ 4.5:1 (5.0 / 11.1 /
     6.6 / 6.9 / 5.7 / 6.7), контур и заливка к фону экрана/карточки ≥ 3:1. Скругление и высота — как у кнопки «GO» (▶/⏸):
     `AutoRecordStyle.shape` (= `ButtonDefaults.shape`) и `AutoRecordStyle.height` (= `ButtonDefaults.MinHeight`, 40 dp) в `AutoRecordUi.kt`.
   - «Поезд»: ряд [«Автозапись» слева | плашка секундомера | GO справа] (31.10.2026): чип «Автозапись» занимает свою ширину (не больше
     ширины строки минус кнопка «GO» и плашка; слишком длинный текст обрезается многоточием), плашка секундомера («в пути»/«стоянка» и время)
     стоит по центру свободного места МЕЖДУ чипом и «GO», а не по центру экрана; подпись («в пути»/«стоянка») - строго под плашкой. Включён - зелёный с пульсирующей точкой,
     выключен - серый. Плашка секундомера той же формы (пилюля, `AutoRecordStyle.shape`) и высоты (40 dp), что «Автозапись» и «GO».
     Кнопка сортировки списка станций (↕) вынесена в строку заголовка «МАРШРУТ» (справа, рядом с кнопкой перегонов).
   - Главный экран: чип «Автозапись» (без времени) в строке заголовка блока «ТЕКУЩИЙ МАРШРУТ» (справа); тап — выключить/включить (включение — после разрешения
     геолокации, подтверждения не нужно).
   - Виджеты: малый 2×2 — точка Автозапись в правом нижнем углу (справа от строки «норма», не перекрывает время; включено — зелёная, выключено — серая; в RemoteViews пульсации нет,
     точка статичная), тап по ней переключает; развёрнутый — кнопка с подписью «АВТО» (на виджете полное слово не помещается) третьей рядом с двумя кнопками записи (включена —
     зелёная, выключена — серая, без времени, радиус 14 dp как у кнопок записи); когда кнопок записи нет (у поезда нет станций / поезда нет) — «Автозапись» отдельной строкой на всю
     ширину под блоком маршрута. Цвета — ресурсы `widget_auto_*` (values / values-night). Состояние виджеты читают из `SharedPreferences` менеджера и обновляются при его смене. Выключение из виджета работает сразу; включение открывает приложение
     (Android не даёт стартовать службу геолокации из фона) и включает Автозапись там.
   - Постоянное уведомление foreground service «LocoDriver: Автозапись включено до чч:мм» с действием «Выключить» — требование Android
     и главный индикатор вне приложения.
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
   Панель — шесть равных по ширине слотов (`weight(1f)`), кнопка по центру слота, поэтому
   центры кнопок равноудалены (шаг = ширина панели / 5; до 28.10.2026 слотов было пять);
   порядок слева направо: вагонник (иконка двух сцепленных автосцепок, при отсутствии записи
   сразу создаёт `CarInspector`), параметры вспомогательной тяги (иконка настройки),
   «Карта», **«Ограничения поезда»** (28.10.2026, иконка-треугольник `ic_restriction_24`, `contentDescription` «Ограничения поезда»;
   открывает полноэкранный экран «Ограничения поезда», см. «Журнал проследования ограничений» в §6.9), **«ВУ-45»** (иконка-манометр `ic_card_brake`, `contentDescription` «Справка ВУ-45»; открывает экран §26 с
   предзаполнением: вес `Train.weight`, оси `Train.axle`, номер поезда `Train.number` этого поезда, серия и номер
   последнего локомотива маршрута; пустые/нулевые значения не подставляются; индикатора-точки у кнопки нет), **«+»** (самая правая). «+» — основная кнопка: круг 56 dp синего цвета (`tertiary`) с контрастным
   плюсом 28 dp и только им, без слова «Станция» (`contentDescription` «Добавить станцию»),
   вызывает `startAddingNewStation`; заметно крупнее остальных (48 dp). Иконка «Карта» —
   цветная (сложенная карта с зелёной, голубой и жёлтой зонами и красной меткой,
   `ic_train_map_color_24`, без тонирования). Прежней кнопки в конце списка нет. Оранжевая точка на
   вагоннике/параметрах показывает, что раздел содержит хотя бы одно заполненное поле. Простое
   открытие раздела и создание пустой записи индикатор не включает.
   Между верхней границей панели и рядом кнопок оставлен отступ `8 dp`.
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
  «Путь» (плейсхолдер «№», текстовая клавиатура, **не более 6 символов** («ЧМО10»; поле шире — 124dp)).
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
станций i/i+1), «Путь» (≤ 6 символов, плейсхолдер «№») + кнопки «I», «II», «III»
(ширина делится на четыре), «Примечание» (многострочное, плейсхолдер «Например: по
неправильному»), кнопка «Готово». **Закрытие любым способом = сохранение**
(`saveSegmentFromSheet`): путь/примечание пишутся в station[i+1]; если станции i+1
нет и «До» не пустое — создаётся новая станция в конце.

### 6.6. Плечи: выбор и редактор (`ShoulderEditBottomSheet`)

**Без аккаунта (решение 09.10.2026):** автоподбор, замер, варианты маршрута, линия плеча, поиск координат и станция по рельсам работают без входа (запрос без `Authorization`, лимит по IP); при 429 - «Слишком много запросов. Попробуйте позже или войдите в аккаунт» и «Повторить» после `Retry-After`; подробности - §6.9 «Что доступно бесплатному и неавторизованному пользователю».

**Выпадающие подсказки станций (общий `StationDropdownMenu`, все поля станций/серий: плечо От/До, промежуточные, Поезд, Локомотив, Пассажиром, Прочая работа):** список не показывается, если текст поля ПОЛНОСТЬЮ совпадает с одним из вариантов (без учёта регистра, лишних пробелов, е/ё). Тап по булавке станции в редакторе плеча открывает шторку станции, закрывает подсказки и снимает фокус с поля. **Порядок по положению (30.10.2026):** в подсказках «Пассажиром» и «Прочая работа» (как уже в «Поезде») ближайшая к пользователю станция (в радиусе подбора) идёт первой, остальные — как были (`StationSuggestionSorter`). Положение запрашивается один раз при открытии формы, ждётся не дольше 1 с и дальше не обновляется — порядок не меняется во время набора; нет положения или разрешения — список как есть.

**Шторка «Плечи»** (фон `secondary`; прокрутка списка и закрытие шторки свайпом — два отдельных
движения: **шторка закрывается свайпом вниз только за «ручку»/шапку (заголовок «Плечи»), а жест по самому списку — только прокрутка и шторку
не тянет никогда, даже когда список в самом верху** (решение 07.10.2026; раньше при прокрутке до верха тот же жест закрывал шторку) —
`separateSheetFromScroll`; на Android и PWA):
заголовок «Плечи» и «✕»; список всех
`UserSettings.servicePhases` строками «{отпр} — {приб}» + «{N} км» (пусто — «Список
пуст»). Первый тап по строке выделяет её (фон `tertiary` 12%, галочка **в отдельном слоте у правого края строки, правее «{N} км»** — не поверх и не вместо километража (решение 07.10.2026); слот под галочку зарезервирован у всех строк, чтобы «{N} км» не сдвигались при выделении), **повторный
тап по выделенной снимает выделение** (решение 07.10.2026; раньше применял и закрывал — применение теперь только кнопкой «Выбрать»).
При снятом выделении кнопки «Выбрать»/«Редактировать» неактивны (или скрыты, как в текущей вёрстке), «Новое плечо» остаётся. *Android, жесты шторок:* `separateSheetFromScroll` теперь безусловный — остаток жеста/инерции списка (список наверху, оверскролл) съедается всегда, шторку список не тянет никогда; закрыть можно только свайпом за ручку/шапку. Применён в «Плечи», шторке станции плеча, автоподборе (все три списка), `StationPickerSheet`, `SeriesPickerSheet`. *Android (`FormTrainScreen`):* `selectedInSheet` переключается тапом (тот же тап → `null`); при `null` «Выбрать»/«Редактировать» скрыты (`AnimatedVisibility`), слот галочки — `Box` 24 dp в конце строки у всех строк. При выделении появляются кнопки
«Выбрать» (применить) и «✎ Редактировать»; всегда внизу «⊕ Новое плечо». **Кнопки (включая «Выбрать»/«Редактировать»/«Новое плечо»)
всегда видны: закреплены внизу шторки, прокручивается только список над ними (Android и PWA; решение 07.10.2026).**
**Редактор** (`ShoulderEditBottomSheet`, поверх): заголовок «Новое плечо» /
«Плечо» (31.10.2026; раньше «Редактировать плечо»); поля «От станции», «До станции» (автодополнение),
«Расстояние» (цифры, суффикс «км») и рядом, в одном ряду и одной высоты (56 dp), кнопка «Измерить по карте» (подпись переносится на две строки, чтобы влезла; после замера — плашка
«Измерено по карте»), для нового — переключатель
«Создать плечо в обратном направлении». Ниже полей — сворачиваемый блок **«Доплаты за поезд на этом плече»**
(заголовок — стиль группы как у «Промежуточные станции»: mono, UPPERCASE; «Настроить»/«Свернуть», без ripple при нажатии; по умолчанию ВСЕГДА свёрнут, даже если у плеча есть ставка пробега или свои ступени - раскрывается по «Настроить»). Подразделы внутри — единые подзаголовки 12 sp
(«Доплата за пробег», «Тяжеловесные поезда», «Длинносоставные поезда»), мельче заголовка блока; поясняющая подпись «Если списки … пусты, берутся доплаты из настроек ЗП» стоит в конце блока, под последним полем. Первым идёт поле
**«{валюта}/км»** (дробное; символ валюты по `UserSettings.country`; название — подзаголовок «Доплата за пробег» над полем) и под ним «За плечо: расстояние × ставка»; затем списки «Тяжеловесные поезда»
(вес, т → %) и «Длинносоставные поезда» (длина, ваг. → %), «Добавить» / корзина в строке. Пустой список = для плеча
действуют общие ступени из настроек ЗП (по каждому из двух списков отдельно). Пустые строки при сохранении
отбрасываются. Обратное плечо при создании получает пустые списки (весовые нормы по направлениям разные). **Автосохранение, кнопки «Сохранить» нет:** правки
сохраняются через ~0,6 с после изменения и при закрытии («Готово», системный «назад»), если обе
станции не пустые (расстояние не обязательно: пусто = 0, его можно указать или измерить позже;
в форме поезда расстояние 0 не подставляется); иначе не сохраняется (новое плечо не создаётся,
существующее остаётся в последнем сохранённом виде). У нового плеча один `id` на всё
редактирование — повторные сохранения обновляют то же плечо; обратное создаётся при закрытии;
новые названия из автоподбора пишутся в справочник при закрытии. Ставка < 0 → 0, нечисло → 0.
Для редактирования сохраняется прежний `id`; обратное плечо получает то же
расстояние и ставку. «Удалить плечо» (только редактирование) удаляет **сразу, без
подтверждения** (⚠️ Поведение Android (возможный баг, BUG-28): расходится с правилом §24.3);
если удалённое плечо было выбрано
в поезде — выбор снимается. Изменение выбранного плеча обновляет его в поезде и
расстояние. Все изменения пишутся в `UserSettings.servicePhases` и ставят флаг
отложенной синхронизации настроек (`setSettingsSyncPending(true)`).

**Кнопки редактора плеча, автоподбора и шторок станций** (`PstButton`) — скругление как в остальном
приложении: 16 dp (высота ≥ 44 dp) и 12 dp у маленьких (36 dp), не «пилюли».

**Автоподбор от точек пользователя.** Если у крайней станции есть координаты (справочник станций или
плечо, в т.ч. указанные вручную), клиент передаёт их в `suggest` (`departurePoint`/`arrivalPoint`):
путь и км промежуточных станций считаются от этих точек, а не от точки станции в OSM — длина
автоподбора совпадает с «Измерить по карте». Своя станция OSM в промежуточные не попадает (по osmId,
иначе ближайшая в 3 км с похожим названием или ближе 1 км).

**Поиск станции по части названия (решение 06.10.2026; «Построить другой маршрут», подсказки станций с сервера).**
По мере ввода (от 2 символов, пауза ~400 мс) клиент показывает **все подходящие станции, а не 5**: сервер
`POST /v1/service_phase/stations/lookup` получает необязательное поле `limit` (по умолчанию 5 — как раньше;
клиенты поиска передают 30) и ранжирует: 1) название **начинается** с введённого («Санкт-» → все «Санкт-Петербург-…»),
2) любое **слово** названия начинается с введённого, 3) введённое **встречается в любом месте** названия; внутри
группы — по близости к станциям плеча, затем по алфавиту. Сравнение без регистра, е = ё, дефис = пробел, хвостовой
дефис/пробел запроса не мешает. Старые клиенты (без `limit`) получают прежний ответ. Список в клиенте прокручивается,
каждая запись — название, ЕСР, дорога/оператор, соседние станции.

**Свой маршрут: правка станций после построения и без О.П. (решение 07.10.2026).** После «Построить» в предпросмотре есть действие
«Изменить станции»: возвращает в панель «Через какие станции построить?» с **тем же списком** (добавить, убрать, переставить) —
после правки «Построить» перестраивает путь заново; «Назад» из панели возвращает к предпросмотру/вариантам без потери списка;
временные станции сохраняются в плечо только по «Выбрать маршрут». В подсказках поиска станций для этой панели (запрос с `limit`)
**не предлагаются остановочные пункты (О.П., `kind = halt`)** — только станции; сервер отфильтровывает их в таком поиске, клиенты
дополнительно игнорируют записи с `kind = halt`. (Выбор О.П. вручную в других местах — «Добавить вручную», справочник — не меняется.) Android: «Изменить станции» — широкая кнопка над «Назад» / «Выбрать маршрут» в предпросмотре; при перестройке «до» (исходные станции плеча и экран вариантов) сохраняется, поэтому «Назад» из предпросмотра возвращает ровно к исходным вариантам, а ошибка пути возвращает в панель с тем же списком.

**Путь без «зигзагов» (решение 06.10.2026).** Линия плеча не должна заходить на станционный путь и возвращаться на
соседний, если существует путь без такого захода: поезда так не ходят. Разворот допускается только в точке
промежуточной станции пользователя и только когда иначе пути нет. Контрольный случай: Лужская → Кобралово →
Санкт-Петербург-Товарный-Витебский → СПбСМ — после Витебского линия идёт по основному ходу без зигзага.
**Разворотов нет (решение 07.10.2026, уточнено пользователем).** Поезд не разворачивается — ни на развилке, ни в точке
промежуточной станции, ни на конечной: **порядок станций задаёт пользователь** (нужен разворот — он укажет другой порядок).
Сервер (`route/length`) ищет путь без разворотов в любых разумных пределах (даже заметно длиннее: Лужская →
Санкт-Петербург-Товарный-Витебский → СПбСМ идёт через Кобралово, 180 км, без захода на СПбСМ и возврата) и, если
безразворотного пути нет, отвечает 404 «Путь по железной дороге между станциями не найден». Предупреждения «с разворотом»
и поля `hasReversal` нет. Клиент при 404 для «своего» маршрута возвращает в панель «Через какие станции построить?» с текстом
«Путь через эти станции не найден — измените порядок или список», для «Измерить по карте» — снекбар «Не удалось проложить
маршрут между станциями — проверьте порядок и положение станций» (**слова «разворот», «без разворотов» в текстах для пользователя не
используются никогда**: ни в ошибках, ни в подсказках, ни в письме разработчику — это внутреннее правило поиска пути).
**Согласованность кнопок (решение 07.10.2026).** Автоподбор («Найти автоматически»), «Измерить по карте» и «Карта» считают расстояние
по одному и тому же пути: станции, найденные автоподбором и добавленные в плечо, обязаны измеряться без ошибки и давать ту же длину
(допуск 1%). Если «Измерить» не может проложить путь через станции, найденные автоподбором, — это ошибка сервера (требование
проверяется регрессионным тестом «подобрал → измерил → совпало»). Для этого сервер считает промежуточную точку (станцию, О.П.) пройденной, если путь проходит от неё не дальше 400 м по любому
пути (точки станций в OSM лежат на боковых путях и в стороне), а не требует заезжать на её ближайший путь.
**Защита от «ложного километража» (решение 07.10.2026; причина — неверные координаты станции давали крюк на сотни км: 710 км вместо 165).**
Если результат «Измерить по карте» или автоподбора (длина пути) больше **1,5×** введённого расстояния (введённое > 0) либо больше **3×** суммы
прямых между станциями плеча, его **нельзя записывать молча** (автосохранение не должно перезаписать расстояние и км станций): показывается
диалог «Расстояние намного больше введённого: {X} км вместо {Y} км. Возможно, неверно указана одна из станций — проверьте станции.» с
действиями «Применить» и «Отмена» (по умолчанию — «Отмена»; км станций при отмене не меняются). В предпросмотре автоподбора то же — плашка
расхождения становится красной с этим текстом, «Установить N км» требует подтверждения. Если введённое расстояние 0 — проверка по сумме прямых.
*Реализация Android:* `util/FalseMileage` (`isSuspicious`: длина > 1,5× введённого при введённом > 0, либо > 3× суммы прямых
отправление → станции с координатами → прибытие; равенство порогу — ещё допустимо; нулевая длина — не подозрительна;
`message` — текст выше, «вместо» = введённое, а при введённом 0 — сумма прямых, км округляются). В `ShoulderEditBottomSheet`
проверка стоит в `applyChecked` — общая для «Измерить по карте» (единственный результат) и «Выбрать маршрут»; диалог
`AppAlertDialog` «Проверьте станции» («Применить» / «Отмена»), до подтверждения расстояние, км станций и чип «Измерено» не
меняются. В `PhaseAutoFillSheet` плашка расхождения красная (`Pst.danger`), показывается и при введённом 0, а «Установить N км»
открывает тот же диалог.

**Сообщить разработчику, что маршрут не проложить (решение 07.10.2026).** Когда путь не найден, рядом с сообщением есть
действие, открывающее почту (`ACTION_SENDTO`): снекбар после «Измерить по карте» — «Сообщить» (8 с); панель «Через какие
станции построить?» после ошибки — подсказка «Если такой маршрут должен существовать, сообщите разработчику — мы поправим
карту.» и кнопка «Сообщить разработчику»; сообщение автоподбора «Путь между станциями не найден» — то же. Письмо: адрес
поддержки `locodriver.app@yandex.ru` (как в «Настройках»), тема «Не удаётся проложить маршрут: A → B → C», текст — «Здравствуйте!»,
плечо, станции по порядку (отправление / через / прибытие) с координатами станций, просьба написать, как поезд должен идти;
пользователь дополняет и отправляет сам. Нет почтового приложения — тост «Нет почтового приложения. Напишите на
locodriver.app@yandex.ru».

**Редактор плеча: «Изменить» (режим правки промежуточных станций) и компактные снекбары (решение 07.10.2026).** Режим
«Изменить» всегда можно закрыть кнопкой «Готово» на месте «Изменить» (на Android и PWA), в том числе когда остались только
крайние станции; при пустом списке станций режим выходит автоматически. Снекбары после «Измерить по карте»/автоподбора —
компактные: до 2 строк (исключение — длинное «Станция «X» не найдена на карте…» с действием «Написать в поддержку»: текст целиком на всю ширину, действие под ним справа), ширина по тексту (не на весь экран), небольшие отступы, без многострочных портянок; текст
сокращается («Координаты «X» определены — проверьте точку»); автоскрытие 6 с (с действием — 8 с).

**Станции за границей (решение 31.10.2026).** Поиск станции (`/stations/lookup`, автоподбор, подсказки) сначала идёт в стране
пользователя (`country` из настроек). Если там ничего нет, сервер повторяет поиск по всем странам графа (RU/KZ/BY) — так находятся
станции пограничных плеч (Курган → Петропавловск у пользователя с country=RU). Станции своей страны по-прежнему имеют приоритет.

**Сокращённые названия станций** («Гатчина ТБ», «СПбСМ», «Лужская-Сорт»). Если точного совпадения
нет, сервер (`/stations/lookup`, автоподбор) возвращает до 5 похожих станций с `approximate: true`
(части названия по порядку: слова, начала слов, слитные инициалы, типовые сокращения СПб/Сорт/Тов/Пасс/
Гл/Моск/Балт/Варш…; ранжирование по совпадению и близости к станциям плеча). Неточный вариант
**никогда не подставляется сам**: везде («Определить автоматически» в карточке станции, «Найти
координаты» в шторке станции, «Измерить по карте» для крайней без координат, автоподбор) открывается
шторка «Похоже на «{имя}»» — «Станции с таким названием нет — возможно, это сокращение. Выберите свою
станцию или укажите точку на карте.», список вариантов (ЕСР, оператор, соседи), «Указать на карте
вручную», «Отмена»/«Выбрать». Название у пользователя остаётся своё («Гатчина ТБ»), станции
достаются координаты/ЕСР/osmId выбранной (в справочник — при сохранении). В автоподборе уточнение
крайней станции показывает тот же текст про сокращение; выбранный вариант уходит повторным запросом
по osmId. Ничего похожего — «Не нашлось на карте — проверьте название. Если это не станция (пути
необщего пользования, терминал, пост) — укажите точку на карте вручную.» (так же в автоподборе и в
«Измерить по карте»). После выбора в «Измерить по карте» замер продолжается автоматически.

**Ошибки сети (автоподбор, «Найти координаты», «Измерить по карте», карточка станции).** Причина
определяется по исключению и состоянию сети телефона: нет интернета (нет сети или она не проверена
системой) — «Нет подключения к интернету»; сеть есть, но запрос не дошёл — «Не удалось связаться с
сервером — проверьте подключение к интернету» (без утверждения «интернет есть»: при кратком обрыве телефон
ещё считает сеть рабочей) (при включённом VPN — подсказка «Нет связи с сервером. Возможно, слабый сигнал сети…»: VPN не упоминается и отключать его не советуем); 429 —
«Слишком много запросов — попробуйте позже»; 503 («сервис занят / карты недоступны») — «Сервер занят или долго считал — повторите через минуту» (отдельный экран «Сервер занят»: «Повторить» / «Добавить вручную»); «Сервис не ответил — попробуйте позже» — только прочая ошибка
сервера (в т.ч. не разобранный ответ) или ответ дольше 30 с при живом интернете. Линия плеча на карте при неудаче запрашивается
повторно через 2 с.

**Координаты станций — из справочника станций (`StationNorm`, §17.4), связь по названию
(без регистра и лишних пробелов).** Плечо читается с координатами из справочника
(`StationDirectory.withDirectory`: где у станции справочника есть координаты — они, иначе
копия в самом плече; так в редакторе, на карте плеча, в счётчике «нет координат» списка
плеч и в подстановке станции по GPS в поезде). При каждом сохранении плеча его
координаты (крайние и промежуточные, с ЕСР/OSM/типом) записываются в справочник: новые
станции создаются записью, у существующих обновляются координаты (нормы и км оси не
трогаются); крайним станциям без координат подставляются известные справочнику. В самом
плече (`UserSettings.servicePhases`) координаты остаются копией для старых клиентов/PWA.
**Автоподбор координаты только дописывает:** при «Применить» у крайних станций с уже известной точкой
(в редакторе или в справочнике) и у промежуточных станций, которые есть в справочнике с координатами,
остаются их координаты (в т.ч. указанные вручную), точки OSM ставятся только станциям без координат.
Плечи, сохранённые до справочника, один раз за запуск переносят координаты в справочник —
только станциям, у которых их ещё нет. Подстановка названия по GPS и сортировка подсказок
учитывают и станции справочника, не входящие ни в одно плечо. Правка координат в карточке
станции сразу видна во всех плечах с этой станцией.

**Редактор плеча (новый, по `design/service_phase_stations/` экраны 01–15; не проверен на
эмуляторе):** **отдельный полноэкранный экран** (полноэкранное окно поверх экрана, вызывающий код
тот же; в шторке длинный список станций при прокрутке конфликтовал с жестом шторки — она «прыгала»),
верхняя панель: слева «Готово» (закрыть; правки уже сохранены), по центру «Новое плечо»/«Редактировать плечо», справа «Карта» (всё плечо на карте,
`PhaseRouteMap`: крайние — крупные тёмные точки, станции — белые, о.п. — мелкие, подписи, линия пути
**по рельсам** — сплошная синяя (`POST /v1/service_phase/route/length` с `withGeometry: true` →
`geometry` [[lat, lon], …], шаг ~100 м; запрашивается при открытии карты); линию напрямик между
станциями не рисуем: пока путь строится — внизу карточка со спиннером «Строим путь по рельсам…»; запрос линии повторяется при появлении/смене маршрута (при входе с Главного плечо подгружается после открытия карты) — иначе спиннер висел бесконечно (баг 29.10.2026);
путь не найден — «Не удалось проложить путь по рельсам между станциями плеча. Проверьте координаты
станций — точка могла оказаться в стороне от железной дороги.» + кнопка «Уточнить» (31.10.2026: на карте из редактора плеча закрывает карту и возвращает в редактор; на карте поезда закрывает карту и открывает редактор этого плеча без автоопределения; на карте с Главного кнопки нет); сбой сети/сервера (после одного
повтора через 2 с) — «Путь по рельсам не построен. {причина}.» + «Повторить»; линия всегда запрашивается
заново (граф путей на сервере обновляется); последняя полученная хранится на устройстве по набору точек
(`noBackupFilesDir/rail_lines/`) и рисуется, только если сервер недоступен (без сети); поверх карты при этом
показывается заметная плашка «Маршрут показан из сохранённых данных». Камера охватывает плечо; в шапке «18 станций», если координаты есть у всех,
иначе «Показано 17 из 18 · у 1 нет координат» (оранжевым); справа «+», «−», «Моё местоположение»
(разовая координата, синяя точка + камера к ней; разрешение — по кнопке, отказ → «Нет доступа к
местоположению»); только просмотр), фон `background`, поля «От/До станции» (с автодополнением),
«Расстояние, км», «Доплата за пробег, {валюта}/км» (символ валюты по `UserSettings.country`);  в
строке кнопки/чипа «Измерить по карте» справа (прижато к правому краю), если заданы и расстояние,
и ставка > 0, — «За плечо: {расстояние × ставка} {валюта}» (округление до копеек, как в расчёте
зарплаты; пересчитывается при вводе); для нового — «Создать плечо в обратном
направлении» (**по умолчанию вкл**). **Блок «Промежуточные станции» не показывается, пока не заполнены обе крайние станции** («От»/«До» непусты; решение 07.10.2026: у нового пустого плеча экран не засоряется; у существующего плеча блок виден сразу; при очистке крайней блок скрывается, введённые станции не теряются), появляется плавно при заполнении второй крайней. *Android:* `if (canAutoFill)` вокруг заголовка/схемы/кнопок блока в `ShoulderEditBottomSheet`; элементы с `animateItem()` (плавное появление/исчезновение), при скрытии блока режим «Изменить» сбрасывается. Блок «Промежуточные станции · N»: пусто — «Найти
автоматически» / «Добавить вручную» (пояснение: «Найдём их по карте или добавьте вручную.») под схемой; схема участка (крайние +
промежуточные, км, «нет координат») видна всегда. Тап по **крайней** станции — та же шторка в
упрощённом виде «Станция отправления/прибытия»: название (меняет поле «От/До станции»), карточка
координат («Найти координаты», «Указать на карте»; точка — в `departurePoint`/`arrivalPoint`), без
типа, км и удаления. Тап по промежуточной — шторка станции (название с подсказками по началу слова, Станция/О.П., км 0 < км < расстояния,
карточка координат, «Удалить станцию» с подтверждением); «Изменить» — ✕ (убрать сразу +
снекбар «Отменить» 6 с), перетаскивание за ≡ / долгим нажатием, «+ вставить» между соседними.
Новая станция без позиции встаёт по км. «Удалить плечо» — теперь с подтверждением (закрывает
BUG-28). Изменения сохраняются автоматически (см. выше). При
переименовании крайней станции её координаты сбрасываются. В списке плеч — вторичная строка
«через N станций · M без координат» / «без промежуточных».

**Выбор точки на карте** (`PhaseMapPicker`, экран 16; «Указать/Уточнить на карте» в шторке станции):
(**При открытии выбора точки шторка станции скрывается, чтобы карта была видна целиком** (решение 07.10.2026; баг: шторка оставалась поверх карты); после «Выбрать» — шторка возвращается с новыми координатами, «назад» — шторка возвращается без изменений. Android: шторка не закрывается, а скрывается — прозрачная, без затемнения, содержимое невидимо; состояние (название, тип, км, найденные координаты) сохраняется; так в шторке станции плеча (крайние и промежуточные), в «Не та станция → Указать на карте вручную» автоподбора; в карточке станции справочника (отдельный экран) шторки под картой нет.)
полноэкранно, MapLibre ≥ 13 (нативные библиотеки 16 КБ) + векторная подложка OpenFreeMap Liberty
без ключа через свой прокси `https://app.locodriver.ru/tiles/styles/liberty` (прямой домен
`tiles.openfreemap.org` в России без VPN недоступен; в стиле и TileJSON ссылки уже переписаны на прокси,
`STYLE_URL` общий для `PhaseMapPicker` и `PhaseRouteMap`); поверх — свои слои из того же источника: ж/д линии (`transportation`, class=rail) и
станции/о.п. (`poi`, class=railway, subclass station|halt; метро не показываем; в тайлах с z14) с
подписями `name:ru`. Метка неподвижна, её острие — 42% высоты (центр камеры смещён нижним
отступом 16%). Тап по станции на карте — камера на неё, подпись «Точка на станции «X» с карты».
Соседние станции плеча с координатами — белые кружки-ориентиры. Старт камеры: координаты станции
→ середина соседей → текущее местоположение (если разрешение уже дано) → Москва z5. «+»/«−»,
«Моё местоположение» (разовый запрос; запрос разрешения ACCESS_FINE/COARSE_LOCATION по кнопке;
отказ → подсказка «Нет доступа к местоположению…»). Предупреждения «Рядом нет железной дороги»
нет: запрос к отрисовке из событий камеры ронял приложение при закрытии карты (нативный SIGSEGV
MapLibre) — `queryRenderedFeatures` вызывается только на тап пользователя.
«Выбрать» — координаты центра камеры в станцию (`source = map`), применяются по «Готово» в шторке
станции; «назад» — без изменений.

**«Измерить по карте»** — под полями «Расстояние / Доплата»: кнопка «Измерить по карте» (иконка —
две метки на карте, соединённые путём, `PstIcons.Measure`) или чип
«✓ Измерено по карте». Чип — когда расстояние измерено по карте (этой кнопкой или при автоподборе:
«Установить N км» либо расстояние совпало с длиной пути) **и** с тех пор не менялись ни расстояние,
ни координаты станций (крайних и промежуточных; сравнивается «подпись» координат по порядку);
иначе — кнопка (расстояние введено вручную / не введено / координаты изменились). Кнопка:
`POST /v1/service_phase/route/length` {points: отправление, промежуточные с координатами, прибытие} →
длина по ж/д путям через все станции по порядку (сервер ищет **один непрерывный путь по рельсам**: на
станции нельзя перескочить на соседний путь, сменить направление можно только в точке промежуточной
станции; в точке промежуточной станции можно перейти на соседний путь этой же станции (≤ 300 м — в OSM пути
у станций бывают не соединены), только продолжая движение в ту же сторону — поэтому плечо и обратное
плечо дают одну длину); расстояние = целое, км промежуточных станций
обновляются по тому же пути. Нет координат у крайней станции — сначала она ищется на карте по
названию (`/v1/service_phase/stations/lookup`, ориентиры — другая крайняя и станции плеча с координатами;
берётся первая находка), затем измеряется; после замера — снекбар (12 с) «Координаты станции «{имя}»
определены автоматически — проверьте, что точка верная» (для двух — «станций «A» и «B» … точки верные»)
с действием «Проверить» → шторка этой крайней станции. Найденные координаты сохраняются вместе с плечом
(и в справочник станций). Не нашлась — «{имя}» не нашлась на карте — укажите её координаты в схеме»;
нет связи при поиске — «Не удалось найти «{имя}» на карте — нет связи с сервером»; 404 — «Путь по железной дороге между станциями не найден»; сеть/429/сервис —
соответствующий снекбар. Признак «измерено» — строка «расстояние|подпись» (подпись координат по порядку), **синхронизируется в самом
плече** новым необязательным полем `measuredSignature: String?` (`UserSettings.servicePhases[]`; решение 03.10.2026;
сервер хранит плечи в JSON — миграция не нужна, старые клиенты поле не присылают, сервер сохраняет прежнее
значение по id плеча, как для `intermediateStations`). Пишется при каждом сохранении плеча и замере; чип «Измерено»
показывается, если поле совпадает с текущими расстоянием и подписью. Прежнее локальное хранилище
(`service_phase_measured`) — миграция: Android при открытии редактора плеча берёт значение из поля, а если его нет — из
этого хранилища; при первом сохранении плеча значение пишется в поле, локальная запись удаляется. В рейс (`Train.servicePhase`)
поле не копируется, у обратного плеча пусто.

**Варианты маршрута плеча (целевое поведение от 03.10.2026, реализуется; Android и PWA одинаково).**
Если между крайними станциями возможно несколько заметно разных путей по рельсам (Лужская — СПбСМ:
вдоль залива или через Гатчину), клиент **предлагает варианты, как навигатор**: в «Измерить по карте» и в
автоподборе, пока у плеча нет пользовательских промежуточных станций, запрашивается `route/length` с
`alternatives: 3` (до 3 вариантов; ответ — `routes[]` с `id`, `lengthKm`, `geometry`, `viaStations[]` —
станции, через которые идёт вариант, с км; первый — кратчайший; прежние поля ответа остаются за
кратчайшим, старые клиенты работают как раньше). Вариантом считается путь, отличающийся от уже найденных
не менее чем на 15% длины своей линии (иначе это тот же путь со съездами). Экран выбора — карта плеча
(«Карта»/полноэкранно): все варианты разными цветами (кратчайший — синий, далее оранжевый, фиолетовый),
линии одинаковой толщины (5) цветов `#0B57D0` (кратчайший), `#C2410C`, `#6D28D9`; выбранный — непрозрачный и поверх, остальные не светлее 85% непрозрачности.
**Внешний вид (эталон — PWA, `RouteVariantsPicker.vue`; Android повторяет композицию, размеры, скругления, отступы и шрифты на токенах темы):**
сверху на карте — одна скруглённая (16) карточка поверхности шторки с тенью: слева синий «✕» (акцентный цвет, 40×40,
закрыть), далее «{От} → {До}» (16 sp, жирный, в одну строку), отдельной стрелки «назад» нет; под ней справа колонка
одинаковых кнопок 44×44 со скруглением 12 и тенью — «+», «−», «Моё местоположение» (запрос системного разрешения,
сообщение о недоступности). Снизу — шторка во всю ширину со скруглением верхних углов 18, цвет поверхности темы (тёмная:
`bgSubtle`, светлая: `surface`), отступы 14/16 + навигационная панель, высота не больше 75% экрана, прокрутка при
нехватке места. Поля/карточки внутри — чуть светлее шторки в тёмной теме (`surface`) и серее в светлой (`bg`).
Режим вариантов: ряд из равных карточек (зазор 8; скругление 12, мин. высота 84, отступы 10/8, рамка 2): цветная точка
12 слева сверху, «через {станция}» (12 sp, жирный, до 3–4 строк, перенос после дефиса «Гатчина-/Варшавская»), «{N} км»
(12 sp, серый) у нижнего края — у всех карточек на одной линии; выбранная — рамка цвета линии, невыбранные — без рамки.
Под ними (серая подсказка 13 sp, если у варианта нет станций: «Чтобы замер шёл этим путём, добавьте промежуточные
станции.») два равных по высоте действия в ряд (мин. высота 48, скругление 12, 14 sp жирный): слева «Построить другой
маршрут» (акцентный текст на подложке поля), справа «Выбрать маршрут» (CTA: светлая заливка и тёмный текст в тёмной теме
и наоборот в светлой). Режим «Через какие станции построить?»: заголовок 16 sp, серая подсказка, нумерованный список
выбранных (подложка поля, скругление 12; номер, название 15 sp жирный, «↑» «↓» «✕» — иконки 36×36 акцентного цвета),
поле поиска во всю ширину (высота 44, скругление 10, без подписи; «Название станции» / «Добавить ещё станцию»), список
результатов (подложка поля, макс. 32% высоты экрана, прокрутка; название 15 sp жирный, «ЕСР …», оператор, «Соседние: …»,
акцентный «+» справа), ниже «Назад» (акцентный текст) и «Построить» (CTA, прозрачность 35% пока список пуст).
Предпросмотр построенного маршрута: одна карточка «через A, B» + км (рамка `#0B57D0`), подсказка «Станции и километраж
применятся к плечу после «Выбрать маршрут».», «Назад» / «Выбрать маршрут». Различитель — первая станция на отличающемся участке
после расхождения линий: станция сопоставляется ближайшей точке полной `geometry`, а общий коридор маршрутов
определяется с допуском; поэтому общая Веймарн не становится подписью одного варианта из-за отличающегося
`osmId`/состава `viaStations`, учитывается и последующее схождение путей. Если координат `viaStations` нет,
используется упорядоченный fallback: пропускаются общий префикс и имена, встречающиеся у конкурирующих путей.
Если надёжного различителя нет, карточка показывает «Основной маршрут» (первый) или «Вариант маршрута» без
ложного «через»; километраж остаётся отдельной нижней строкой. Тап по карточке или по линии на карте выбирает
вариант; выбран кратчайший.
«Выбрать маршрут» применяет: расстояние плеча = длина варианта, **в промежуточные станции вносятся
станции этого варианта** (автоподбор — по обычным правилам предпросмотра; «Измерить» — предлагает
«Добавить станции маршрута»), чтобы дальнейший замер шёл тем же путём. Вариант без станций применяется
только расстоянием, с подсказкой «Чтобы замер шёл этим путём, добавьте промежуточные станции».
**Свой вариант:** пользователь добавляет/убирает/переставляет промежуточные станции в редакторе и жмёт
«Измерить» — путь строится строго через них по порядку (`alternatives` не запрашивается, вариант один).
На экране вариантов действие «Построить другой маршрут» не открывает шторку «Новая станция»: нижняя панель на том же картографическом экране переходит в режим «Через какие станции построить?» (одинаково на Android и в PWA). Пока список пуст — подсказка «Добавьте станции в порядке прохождения пути — маршрут пройдёт строго через них.» Поиск (`/stations/lookup` с `limit: 30`, от 2 символов, пауза ~400 мс; подпись поля «Название станции», после первой станции — «Добавить ещё станцию») показывает **все** подходящие станции в порядке сервера (название, ЕСР, дорога/оператор, «Соседние: A · B») в прокручиваемом списке (до ~240 dp, выбранные станции — отдельный блок выше со своей прокруткой); сервер без поддержки `limit` вернёт прежние до 5 — клиент работает и с таким ответом; тап по записи добавляет станцию в **нумерованный список выбранных** (порядок = порядок прохождения; поиск очищается; станция с тем же osmId или теми же координатами повторно не добавляется — помечена «добавлена»). У каждой строки списка действия «Выше» (↑), «Ниже» (↓), «Убрать» (✕). «Построить» активна при ≥1 станции: список промежуточных станций плеча временно заменяется выбранными, ОДНИМ запросом `route/length` [от, via1…viaN, до] с `geometry` путь строится строго через них по порядку и показывается как обычный вариант — карточка «через A, B · N км» (все выбранные станции через запятую; не влезает — перенос до 3 строк, затем «…»). «Выбрать маршрут» применяет расстояние и все станции (в редакторе плеча — как промежуточные, в автоподборе — заменяют список предложенных). «Назад» из предпросмотра возвращает экран со всеми вариантами; временные станции нигде не сохраняются. Путь не найден (404) — панель открывается снова с тем же списком и текстом «Путь через эти станции не найден — измените список»; другая ошибка сети/сервера — её обычный текст там же. Белого/пустого промежуточного экрана нет.
Если вариант один — экран выбора не показывается (как раньше).
Android (реализация): запрос с `alternatives: 3` и `withGeometry` — при «Измерить по карте» и в предпросмотре
автоподбора, только если у плеча нет станций (`stations.isEmpty()`). «Измерить» при ≥2 вариантах открывает
полноэкранный выбор (`PhaseRouteVariantsMap`): до выбора ничего не применяется, «Назад» закрывает без изменений.
Подпись карточки «через …» определяется по расхождению полной геометрии, как описано выше. «Выбрать
маршрут»: расстояние = длина варианта, чип «Измерено»; кратчайший — как обычный замер; другой — снекбар
«Добавить станции маршрута» (станции с координатами, без дублей, км по пути) либо, если станций нет, подсказка
«Чтобы замер шёл этим путём, добавьте промежуточные станции»; после добавления станций подпись замера пересобирается,
поэтому чип «Измерено» сохраняется. Автоподбор станций теперь сам предлагает выбрать маршрут ДО подбора (см. «Автоподбор идёт по
выбранному маршруту…»): плашки «Есть другие маршруты» в его предпросмотре больше нет. Android: «Найти автоматически» — если в плече
есть станции с координатами, они уходят в `suggest` как `viaPoints` по порядку; если нет — запрашиваются варианты
(`route/length`, 3), при ≥2 открывается экран вариантов (кратчайший → без `viaPoints`; другой вариант → до 12 его станций, равномерно;
«Построить другой маршрут» → выбранные станции), при одном — сразу подбор; длина в предпросмотре — по выбранному пути.
Для двух крайних точек Android отправляет запрос в стабильном каноническом порядке координат; для обратного
плеча геометрии, порядок станций и километры разворачиваются в пользовательское направление. Поэтому оба
направления получают одинаковый набор вариантов одним запросом, несмотря на направленную асимметрию поиска
серверного графа (например, СПбСМ → Лужская). Это не является офлайн-режимом.
**Кнопка «Карта» в редакторе плеча открывается ВСЕГДА (решение 09.10.2026)** — даже когда станции не указаны или у них нет
координат (раньше замер падал со «Станция не найдена», и карту было не открыть). Открывается `PhaseRouteMap`: станции с координатами,
линия по рельсам (как выше). Без станций с координатами камера: на текущее местоположение (если разрешено и определилось), иначе на
последнюю станцию справочника с координатами (масштаб ~9), иначе на Россию целиком; шапка: «Станции на карте не указаны». Внизу карточка
с текстом (когда обе станции пусты) «Станции не выбраны. Укажите их на карте: первое нажатие на карту определяет станцию отправления,
второе — станцию прибытия.» и кнопками (активны всегда): для пустой станции — «Указать станцию отправления» / «Указать станцию прибытия»,
для уже названной — «Уточнить на карте отправление» / «Уточнить на карте прибытие»;
при обеих станциях с координатами добавляется «Измерить по карте» — закрывает карту и запускает прежний предпросмотр: без промежуточных
станций показывает все найденные варианты, с промежуточными — единственный путь строго через них. Единственный путь берётся из
основного поля `geometry`, даже если сервер не заполнил массив `routes`. При сетевой ошибке открывается
сохранённая линия через `PhaseRouteMap` и показывается плашка «Маршрут показан из сохранённых данных». Линия, полученная онлайн при «Измерить»/«Карта», сразу
сохраняется в тот же кэш по набору точек (иначе запасу без сети нечего показывать).
Если для текущих точек варианты уже были загружены через «Измерить по карте», «Измерить по карте» на карте повторно использует их без нового сетевого запроса; при изменении точек кэш не применяется.

**Выбор станции нажатием на карте (редактор плеча, решение 09.10.2026).** Кнопка «Указать…/Уточнить на карте…» включает режим выбора
(внизу вместо кнопок: «Нажмите на карте место станции отправления»/«…прибытия» и «Отмена»). Какую станцию определит нажатие, решает
`PhaseMapPick.target`: пока обе пусты — всегда отправление (какая бы кнопка ни была нажата), следующее нажатие — прибытие; кнопка
не обязательна — нажатие на карту без режима выбирает первую пустую станцию (обе названы — только через «Уточнить…»).
Нажатая точка помечается оранжевой отметкой; поиск ближайшей станции (чистая логика `PointStationPicker`, `StopStationNamer.stationAtPoint`;
без кэша, каждое нажатие — отдельный запрос), «Ищем ближайшую станцию…»: 1) сервер `POST /v1/service_phase/stations/nearest` с
`alongRail=true` (по рельсам, радиус 1 км) и `candidates` — станции справочника и станции плеча в редакторе с координатами в 2 км
(`docs/NEAREST_ALONG_RAIL.md`; при ничьей выигрывает справочник); 2) сервер не ответил (нет сети/ошибка/503) или `off_rails`/`not_found` —
ближайшая станция справочника по прямой в 1 км; 3) станция OSM, нарисованная на карте под пальцем (24 dp); станция OSM с названием,
которое уже есть в справочнике с координатами, берёт координаты справочника. Далее диалог: заголовок «Станция «X»?», без вспомогательного текста (расстояние до точки и пометка об отсутствии сети не
показываются; если в поле было другое название — «Название в поле «Y» будет заменено.»), кнопки «Применить» / «Отмена» и текстовая «Выбрать другую» (остаться в режиме выбора). «Применить» пишет название
и координаты (osmId, ЕСР — если есть) в поле станции плеча и, **только если у станции в справочнике нет координат**, дописывает их в
справочник (`StationDirectory.fillCoordinates`, существующие координаты не перезаписываются, §17.4); карта сразу показывает станцию
и пересчитывает линию. Рядом станций нет (в 1 км): с названием в поле — «Станция не найдена»: «Рядом (в радиусе 1 км) станций нет. Указать
эту точку как «Y»?» («Применить» ставит на точку введённое название — так указывают станции, которых нет в OSM, например «Ультрамар»);
без названия — «Рядом (в радиусе 1 км) станций нет. Нажмите ближе к станции или сначала введите название в поле.» и «Выбрать другую».
После выбора обеих станций карта показывает их и предлагает «Измерить по карте».

**Значок местоположения станции (редактор плеча, решение 09.10.2026).** В полях «От станции»/«До станции» значок-булавка стоит
**справа** внутри поля (раньше слева), 40 dp, кликабелен: открывает карточку этой станции («Станция отправления»/«Станция прибытия»:
название, блок «Координаты» — как при нажатии на крайнюю станцию в схеме). С координатами — акцентный цвет; без — приглушённый с
оранжевой точкой «!». `contentDescription`/подпись нажатия: «Координаты станции есть. Открыть карточку станции» / «Нет координат.
Открыть карточку станции». В схеме участка такая же булавка (36 dp) стоит в правой части строки каждой станции (отправление, промежуточные,
прибытие) перед шевроном и открывает ту же карточку (весь ряд по-прежнему нажимаем); в режиме «Изменить» её нет. Название и км в строке
сжимаются первыми (`weight(1f)`), значок не уезжает на узких экранах.


**«Не та станция» в автоподборе и шторке станции.** В предпросмотре автоподбора у каждой предложенной
станции (и в карточке «Найдено» шторки станции плеча) есть действие «Не та станция»: открывает шторку
«Выберите станцию» — кандидаты `/stations/lookup` с тем же/похожим названием (название, ЕСР, дорога/
оператор, «Соседние: A · B», км от начала плеча, если считается), «Указать на карте вручную», «Отмена».
Выбор подменяет станцию (координаты/ЕСР/osmId, название у пользователя своё), путь и км пересчитываются.
Android: в предпросмотре действие стоит в шторке «Название станции» (✎) под полем; при подмене км от начала плеча
пересчитывается запросом `route/length` до новой точки (не удалось — остаётся прежний), длина всего пути не
меняется. В карточке «Найдено» кнопка открывает шторку из кандидатов последнего поиска (текущий отмечен).

**Стиль карты — цепочка запасных вариантов (Android и PWA).** Подложка: 1) свой прокси
`https://app.locodriver.ru/tiles/styles/liberty`; 2) если не загрузился — `https://tiles.openfreemap.org/styles/liberty`;
3) если и он недоступен — растровая подложка OSM (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`, с атрибуцией).
Линия плеча и точки рисуются поверх любой подложки.

**Автоподбор идёт по выбранному маршруту, а не по кратчайшему (решение 07.10.2026).** (1) Если в плече уже есть промежуточные
станции с координатами (например, выбранные при «Построить другой маршрут» — Цветочная, Шоссейная), автоподбор ищет станции вдоль пути
**строго через них по порядку**: клиент передаёт их в `suggest` (`viaPoints` — необязательный массив `{latitude, longitude}` по порядку); уже
имеющиеся станции помечены «уже в списке», подбираются только недостающие между ними; путь автоподбора совпадает с линией на карте
плеча (Лужская → Цветочная → Шоссейная → СПбСМ идёт через Гатчину, а не через Копорье). (2) Если промежуточных станций нет и между
крайними возможно несколько путей, **автоподбор сначала предлагает выбрать маршрут**: тот же экран вариантов (§6.6, цвета, км,
«Построить другой маршрут»), и после «Выбрать маршрут» станции ищутся вдоль выбранного варианта (в `suggest` уходят `viaPoints` —
координаты станций варианта `viaStations`); вариант один — экран выбора не показывается. (3) Длина «по карте» в предпросмотре
автоподбора считается по тому же пути.

**Автоподбор станций** («Найти автоматически», активна при заполненных обеих крайних станциях;
шторка 92%, экраны 17–26; `ServicePhaseAutoFillViewModel`): `POST /v1/service_phase/stations/suggest`
{departureStation, arrivalStation, country = UserSettings.country, departureOsmId?, arrivalOsmId?}
(osmId — из `departurePoint`/`arrivalPoint`, если название станции не менялось). Таймаут 30 с.
Загрузка — 3 шага (переключаются по таймеру, сервер отвечает одним ответом), «Отменить».
`ambiguous` → выбор кандидата (название, ЕСР, дорога/регион, «Соседние: A · B»), «Далее» → повторный
запрос с osmId. `ok` → предпросмотр: «Найдено: N станций, M О.П.», «длина пути X км»; плашка
расхождения длины (|по карте − введено| > 5% или > 3 км; нет расстояния — «Расстояние не указано»),
«Установить {целое} км» пишет в поле «Расстояние»; «Показывать О.П.» (по умолчанию выкл);
чипы «Отметить все станции» / «Снять все»; схема с чекбоксами (у каждой предложенной станции
✎ — шторка «Название станции» с полем и «Готово»: название можно поправить до добавления): по умолчанию отмечены станции,
О.П. нет; «уже в списке» (совпадение по названию без учёта регистра/е-ё/дефиса и типу) —
заблокирован, у имеющейся дополняются пустые координаты/км/ЕСР/osmId
(`ServicePhaseStationsMerge`). Итог упорядочен по км, станции без км — за ближайшей предыдущей с
км. Чекбокс «Добавить N новых названий в справочник станций» (по умолчанию вкл) — названия
пишутся в `stationList` только при сохранении плеча. «Добавить N станций» → в редактор:
подсветка добавленных 2 с + снекбар «Добавлено N станций[ · M названия — в справочник]» с
«Отменить» (откатывает станции, названия и точки). Точки крайних станций сохраняются в плечо.
Атрибуция «© участники OpenStreetMap». Ошибки: нет сети («Повторить» / «Добавить вручную»),
станция не найдена («Изменить название» — фокус в поле / «Добавить вручную»), путь не найден,
«Слишком много запросов» (сервер ответил 429: лимит 200 автоподборов в час на пользователя;
«Добавить вручную» / «Закрыть»), сервер занят (503), сервис не ответил (прочие 4xx/5xx и таймаут). Крайняя станция, указанная вручную и не найденная в данных карты, приходит с `osmId = null` (поле `OsmStationCandidate.osmId` nullable) — это штатный ответ, не ошибка. Отличие от макета: подпись загрузки «Обычно
несколько секунд» (сервер считает по локальной выгрузке OSM).

**Станции плеча** (данные; дизайн `design/service_phase_stations/`):
- `ServicePhase.intermediateStations: List<ServicePhaseStation>` — станции между
  крайними, порядок = позиция в списке. `ServicePhaseStation`: `id`, `name`,
  `latitude`/`longitude` (оба или ни одного), `kmFromStart` (км от отправления по пути,
  опц.), `kind` = `station` | `halt` (о.п.), `esr` (ЕСР, опц.), `osmId` (опц.),
  `source` = `manual` | `osm` | `map`. Неизвестные `kind`/`source` при чтении → `station`/`manual`.
- `departurePoint` / `arrivalPoint: ServicePhasePoint?` — координаты крайних станций
  (`latitude`, `longitude`, `osmId?`, `esr?`); null — неизвестны.
- **Обратное плечо** (`ServicePhase.reversed()`): станции в обратном порядке с новыми id,
  `kmFromStart = distance − km` (без км — остаётся без км), точки отправления/прибытия
  меняются местами, ставка и расстояние копируются.
- **В рейс** (`Train.servicePhase`) кладётся копия без станций и точек
  (`ServicePhase.forTrain()`): станции берутся из справочника по `id` плеча.
- Синхронизация: поля в том же JSON `servicePhases` (`POST/GET /v1/user_settings/`).
  Сервер сохраняет прежние станции/точки по `id` плеча, если клиент их не прислал
  (старый клиент); точка крайней станции сбрасывается, если старый клиент переименовал
  станцию. Клиент, приславший `intermediateStations`, считается новым: отсутствующая точка
  = координат нет (Kotlin-клиент не шлёт null-поля). Некорректные координаты/значения
  сервер чистит, не отклоняя запрос; ≤ 200 станций на плечо.

**Станции плеча в форме поезда** (станции плеча в поезд целиком НЕ копируются;
`ServicePhaseStationPicker`, `StationSuggestionOrder`, `ServicePhaseOrder` в domain):
- **Единое определение названия станции по месту** (решение 09.10.2026; `StopStationNamer` в domain) — одна функция
  для **GO**, **«Добавить станцию»** (шторка «Новая станция»), **«Автозапись»**, карты поезда и просмотра трека:
  1. **Радиус 1 км** (`StopDetectionConfig.STATION_RADIUS_M`); прежние 500 м («уже на станции») и 2 км убраны.
  2. **Выбрано плечо с координатами станций** — `pickForStop`: кандидаты = станции полного плеча (из справочника по id, с
     промежуточными) **плюс станции справочника с координатами, лежащие на пути плеча**: боковое отклонение от рельсовой линии
     плеча (линия из кэша устройства, та же, что на карте поезда, `RailLineCache` по набору точек станций плеча) **≤ 100 м**; линии
     нет — отклонение от прямых между станциями плеча, допуск **150 м**. Все кандидаты упорядочиваются по положению ВДОЛЬ линии
     плеча (км от начала). Берутся кандидаты в радиусе 1 км от точки остановки, стоящие **не ранее** последней записанной в
     поезде станции (в том числе справочные; сравнение без учёта регистра/е-ё/дефиса; **повтор той же допускается** — «уже на
     станции»); выбирается ближайший по пути (вдоль линии; если точка остановки не на линии — по прямой), при равенстве — впереди по
     порядку. Нет подходящих кандидатов — ближайшая станция справочника в 1 км; иначе название пустое. Прибытие плеча
     (последняя строка формы при выбранном плече) записанной не считается (`ServicePhaseStationPicker.writtenNames`).
  3. **Плеча нет (или у плеча нет координат станций)** — ближайшая станция **ПО РЕЛЬСАМ**: `POST /v1/service_phase/stations/nearest`
     с `alongRail=true`, `radiusM=1000` и полем `candidates` — **станции своего справочника с координатами в радиусе 2 км от точки**
     (не более 40 ближайших по прямой; `key` — ключ справочника, `name`; аддитивное поле, старый сервер его игнорирует); сервер выбирает
     ближайшую ПО РЕЛЬСАМ среди кандидатов и станций OSM (при ничьей в 30 м выигрывает кандидат). Ответ `source="candidate"` +
     `candidateKey` → название берётся из справочника по ключу, в справочник ничего не добавляется. Ответ `source="osm"` (или без
     `source`) → название станции OSM; если такой нет в справочнике станций — дописывается
     (`fillCoordinates`: существующие записи и их координаты не перезаписываются). Ответ `station=null` (`reason`: `off_rails`,
     `not_found`, `no_rail_data`), 503 (пауза 30 с по `Retry-After`, сервер не опрашивается), нет сети/токена/таймаут 5 с —
     тихий запасной вариант: ближайшая станция справочника по прямой в радиусе 1 км (как п.2 без плеча), иначе пусто;
     диалогов и снекбаров об ошибке нет (ни в ручном GO, ни в «Автозапись»). Координата передаётся только в этом запросе, не логируется
     и не хранится. Кэш в памяти (не на диске): повторно сервер не спрашивается, пока позиция сдвинулась < 150 м и прошло < 2 мин
     (кэшируются только ответы сервера, не сбои). Ответы старого сервера без `alongRail` читаются как обычно (поля аддитивны).
  4. **Координата** (GO, «Добавить станцию») — разовая (`DeviceLocation`: последняя известная не старше 60 с, иначе новая с
     таймаутом 5 с); время ставится сразу, название — когда придёт координата и (GO) только если поле ещё пустое.
  5. **«Стоя на месте»** отдельно не определяется (прежнее правило «сдвиг < 300 м» и ближайшая станция плеча «вообще» убраны):
     его роль выполняет «повтор допускается» — стоя у станции, названной ранее, получаем её же. Отличие от прежнего GO: название
     больше не пустеет, если совпало с предыдущей станцией поезда.
  Первая станция при GO на пустом списке тоже получает название.
- Разрешение на геолокацию спрашивается один раз — при первом GO / «Добавить станцию»;
  отказ — подстановки нет, всё остальное как раньше.
- **Шторка «Плечи»**: сначала последнее использованное плечо с текущей станции, затем другие
  плечи с неё, затем по частоте выбора в рейсах, затем остальные. Текущая станция (`ServicePhaseOrder.currentStation`) — первая
  станция поезда, если от неё есть плечи; иначе ближайшее к местоположению **отправление плеча** (с координатами) в 2 км
  (ближайшая станция «вообще» — прибытие/промежуточная — не затмевает её). Порядок пересчитывается, когда позже приходят
  координата и статистика использования.
- **Подсказки станций** (поезд) и **выбор станции в шторке «Локомотив»** (`StationPickerSheet`):
  ближайшая по местоположению (координаты станций из плеч, 2 км) → по частоте в рейсах (поезд —
  названия станций поездов; «Локомотив» — станции приёмки/сдачи) → остальные в прежнем порядке.
  В «Локомотиве» разделы «с нормами / без норм» сохраняются, порядок — внутри них.
  **Без «прыжков» (30.10.2026):** положение берётся один раз при открытии и ждётся не дольше 1 с, дальше не обновляется — порядок не меняется при уточнении GPS. В шторке выбора станции «Локомотива» пока ждём положение — индикатор загрузки, затем сразу отсортированный список (нет положения — по частоте/как есть). Подсказки станций «Поезда» (шторка станции, плечи): в `TrainFormViewModel` порядок считается по зафиксированному `suggestFix`, список пересортируется один раз, когда оно получено.

### 6.10. Скачанные участки (07.11.2026, только Android)

Экран «Скачанные участки»: Профиль → «ДАННЫЕ» → строка «Скачанные участки» («Профиль пути, станции и светофоры на телефоне»), маршрут `OfflineSectionsRoute`. Сверху карточка «На телефоне: N участков · размер», под ней поле поиска по названию и список участков (`GET /v1/advisor/sections`, у которых есть геометрия; по названию). Скачивается весь участок целиком и **только вместе с картой (08.11.2026):** ось (геометрия), профиль (элементы), километровые отметки, станции, светофоры, переезды и места опробования тормозов + тайлы подложки одной операцией; профиль без карты не считается скачанным (в шторке нет опции «только данные»). **Тайлы подложки (07.11.2026):** полоса вдоль оси участка (MapLibre `OfflineGeometryRegionDefinition` по линии оси, зумы 7–12; «Удалить» убирает профиль и все принадлежащие ему паки). **Скачанным считается только участок с полностью завершённым ready-паком тайлов той же версии оси (`axisHash`):** авто-кэш, прерванный staging-пак и несовпадающая версия карты в «Скачано» не попадают; незавершённый staging после падения приложения игнорируется и позднее удаляется. Системный признак `NET_CAPABILITY_NOT_METERED` разрешает загрузку молча; на тарифицируемой сети (в том числе платной точке Wi-Fi) показывается предупреждение о размере («Скачать участок с картой? … займёт около N МБ с картой. Данные и карта скачиваются вместе») с кнопками «Скачать» / «Отмена» - жёсткого запрета нет. **Данные и карта атомарно (08.11.2026):** одновременно выполняется только одна загрузка/замена/удаление; новая версия профиля хранится в дисковой транзакции, а карта — в отдельном staging-регионе. Отказ, отмена, сбой или перезапуск приложения откатывает новую версию; уже работавшие профиль и ready-пак остаются доступны. Только после полной карты фиксируется новый профиль, затем удаляются старые паки. Если участок скачивается впервые и операция не завершилась, он возвращается к кнопке «Скачать». Размер оценивается заранее по числу тайлов в полосе (`MapTilePrefetch.estimateTileCount`, ~20 КБ/тайл); готовые паки и их размер читаются из MapLibre (`MapTilePrefetch.downloadedPacks`). Код: `route/util/MapTilePrefetch.kt`, `OfflineSectionsViewModel`. Строка: название, длина в км, при сохранённом - размер (данные + карта) и «на телефоне» / «есть обновление». Кнопки строки: «Скачать» (нет на телефоне или нет карты), «Обновить» и «Удалить» (устарел), «Удалить» (актуален; диалог подтверждения «Удалить участок с телефона?»), индикатор при загрузке. В карточке сверху «Обновить (N)» обновляет все устаревшие по очереди (останавливается на первом неуспешном участке). Без входа - «Участки доступны после входа в аккаунт…».

**Плечо без сети (кнопка «Определить», `LegDetermineRow`).** Сначала запрос на сервер (OSM). Если он не удался из-за сети/лимита/занятости сервера, плечо считается по скачанным участкам (`OfflineLeg.determine`, `SectionProfileRepository.cachedSections()`): обе станции плеча должны лежать на оси одного скачанного участка, в любом направлении. Расстояние - разность км станций на оси (округляется до км), промежуточные станции - станции оси строго между ними по ходу плеча (`kmFromStart` от отправления; источник `MAP`), координаты - из данных станции, иначе точка оси по км. Из нескольких участков берётся с кратчайшим расстоянием. Под блоком плеча подпись «Нет связи с сервером: плечо определено по скачанному участку «…»». Нет подходящего участка - текст ошибки + «Скачайте нужные участки заранее: Профиль → Скачанные участки». Плечо через несколько участков офлайн не собирается.
Поля «От станции»/«До станции» в редакторе плеча подсказывают и станции скачанных участков (любые две станции участка). Шторка «Где действует» (ДУ-61) при непроверенном плече объясняет, что ограничение всё равно можно добавить (станция/перегон из списка или «Другое место»).

Явная загрузка (`SectionProfileRepository.downloadForOffline`) - действие пользователя: паузы и задержки после прошлых сбоев её не блокируют; результат: OK (объявляется после карты: «Участок и карта сохранены на телефоне»), лимит 429 («Достигнут суточный предел загрузки участков…»), отказ регистрации ключа («Не удалось зарегистрировать защищённый ключ устройства…»), нет данных, сбой («Не удалось скачать участок. Проверьте соединение с сервером.»). «Устарел» = `updatedAt` в списке новее сохранённого. Сохранённое читается без сети (`offlineSections`: id, версия, размер из файлов кэша `filesDir/advisor_profiles`, зашифрованных Keystore); «Профиль пути» и определение плеча по сохранённым участкам работают офлайн. Код: `ui/OfflineSectionsScreen.kt`, `viewmodel/OfflineSectionsViewModel.kt`, `SectionProfileRepository`/`FileSectionProfileCache` (`cachedIds`, `remove`, `sizeBytes`).

### 6.9. Карта поезда (решение 08.10.2026; этапы 1–5 реализованы; только Android)

В нижней панели экрана «Поезд» (§6.2, п. 9) цветная иконка «Карта» (48 dp, подпись для доступности «Карта») — третья из равноудалённых кнопок, левее самой правой «+». По нажатию открывается полноэкранная карта
(`TrainMapScreen`, диалог на весь экран) на общих компонентах карты плеч (§6.6): цепочка подложек (свой прокси → OpenFreeMap →
растр OSM), ж/д линии и станции, справа кнопки «+» / «−» / «Моё местоположение» (компас — только при повороте карты, над «+»); внизу — кнопка **«Начать запись» / «Остановить запись»**, полоса «Профиль пути» и полоса скорости с кнопкой «Развернуть» (п. «Раскладка карты (21.10.2026)» ниже); в верхней строке — «Назад», плашка, «?», «Настройки карты»; «График скорости» открывается кнопкой «Развернуть» полосы скорости. Сверху — плашка «Карта поезда» (при отказе в
доступе к местоположению под заголовком — «Нет доступа к местоположению»).
**Переключатель поездов (решение 28.10.2026, только карта с Главного).** Карта всегда показывает трек ОДНОГО поезда (все его записи по
времени, одной линией). Если в текущем маршруте 2+ поездов, под шапкой — горизонтальный ряд чипов (`MapTrainChips`) в порядке экрана маршрута
(`TrainOrdering.sortForDisplay`): подпись — номер поезда, без номера — «Поезд N» (N — позиция в порядке экрана); выбранный чип залит акцентом;
поезд с идущей записью помечен «●». Порядок чипов фиксируется при первом показе карты и при переключении не меняется (открытие поезда перекладывает его в списке маршрута). Тап по другому
чипу меняет поезд карты (трек, остановки, профиль, цель кнопки записи — строго выбранного поезда; прежняя запись «Автозапись» не прерывается); окно карты
не закрывается (без мигания Главного), пока грузится новый поезд, остаётся прежний; содержимое карты пересоздаётся. Отдельной плашки «нет трека» нет: запись выбранного поезда включает обычная кнопка «Начать запись» (диалог «Запись уже идёт», если пишется другой поезд). Чипы только при 2+ поездах в маршруте. Треки остальных поездов на карте не рисуются. Карта, открытая из формы
«Поезд», и просмотр трека (`viewOnly`) чипов не показывают. Плашки под шапкой (ЭТА, «Цель») сдвигаются вниз на высоту ряда чипов.
**Раскладка карты (решение 21.10.2026).** 
- **Правая колонка** (по центру высоты, приподнята на 90 dp, 28.10.2026: плашка состояния пути / «нет интернета» над нижними полосами не закрывает нижнюю кнопку): компас (только при повороте карты), «+», «−», «Моё местоположение». Кнопки «Еду по карте», «Значок положения», «Зоны без GPS», «График скорости» из колонки убраны.
- **«Моё местоположение» = слежение + «еду по карте» (одна кнопка, иконка-стрелка, два состояния):** синяя — карта ведётся за положением и поворачивается по курсу (маркер всегда «вверх»); серая — пользователь сдвинул/повернул карту жестом (режим выключен). Через 10 с без касаний (`FollowReturn.RETURN_DELAY_MS`) слежение и поворот по курсу возвращаются сами; нажатие кнопки возвращает сразу. Кнопка-компас переводит карту на север и выключает поворот по курсу (слежение остаётся). По умолчанию при открытии карты поворот по курсу включён. В `viewOnly` кнопки нет.
- **Маркер положения** — всегда выбранный в настройках значок (повёрнут по последнему известному курсу, до первого курса «вверх»); синей точки больше нет. Маркер — самый верхний слой приложения: рисуется поверх линии скорости, состава, точек и значков (баг 29.10.2026: слои якорились на несуществующий id и перекрывали маркер).
    - **Пунктир впереди перестраивается по факту (29.10.2026).** Если поезд в движении устойчиво (5 фиксов подряд) ушёл > 15 м от проложенной линии (выход с многопутной станции на другой путь), от текущего положения через оставшиеся станции плеча заново запрашивается линия по рельсам (route/length, не чаще раза в 15 с и каждые ≥ 150 м пути); принимается, если начинается не дальше 80 м от положения и не делает объезда (> 1,5 x хорда + 300 м). Пунктир = прежняя линия до места съезда + новая. Вернулись на прежнюю линию (5 фиксов ≤ 15 м) — снова прежняя. Расчёт прогресса/ETA по-прежнему идёт по линии плеча. Не удалось (нет сети, сервер) — остаётся прежний пунктир.
    - **Повтор запроса линии при слабой связи (29.10.2026).** Пока линия «из сохранённых данных» (кэш) или не построена, а сеть есть, запрос повторяется раз в 30 с; при успехе плашка «Маршрут показан из сохранённых данных» исчезает сама, без повторного входа в карту (раньше обновление шло только при переходе офлайн → онлайн, а при слабом интернете перехода нет).
- **Полосы внизу** (сверху вниз: «Профиль пути», скорость; обе сплошные на всю ширину, высота 12% экрана, не меньше 88 dp). В каждой у правого края **две кнопки по вертикали: «Развернуть» (иконка четырёх углов) — вверху, «Скрыть» (стрелка вниз) — внизу**. **Мини-профиль** (полоса «Профиль пути») целиком кликабелен (весь блок слева от кнопок, не только график) — открывает полноэкранный «Профиль пути», как «Развернуть». Окно мини-профиля 6 км: поезд целиком (длина + 150 м, не меньше 300 м и не больше половины окна) у ближнего к хвосту края, остальная ширина — профиль впереди по ходу (29.10.2026, раньше поезд стоял у конца графика и впереди ничего не было видно).
    - **Зона ограничения на плане и ручная привязка (29.10.2026).** Привязка профиля («я на км…») сдвигает положение поезда по км, а не зону. Чтобы план, профиль и запись журнала проследования показывали одно и то же место, границы зоны на карте плана смещаются на −поправка своего участка (`ProfileCalibrationStore`), перерисовка — при смене поправки. Время проследования (журнал, PDF) считается по профилю с поправкой, карта плана в записи не участвует.
    - **Профиль из кэша при слабой сети (29.10.2026).** Участки рядом с положением сначала берутся из памяти/диска без сети (`SectionProfileRepository.sectionsNear(cacheOnly = true)`) и сразу показываются, затем идёт докачка/обновление. Обновление списка участков идёт вне замка репозитория и с таймаутом 10 с (раньше сетевой запрос держал замок, и чтение кэша ждало сеть — профиль появлялся только при повторном входе). Пустой результат (сбой сети) не запоминается как «загружено»: повтор через 30 с, а не после сдвига на 2 км.
  - **Скорость:** слева текущая скорость (число ≈42% высоты полосы + «км/ч»; источник — живая, иначе последняя точка с подписью «последняя точка чч:мм:сс»; **скорости нет — «0», не прочерк**; в `viewOnly` — скорость выбранной точки пути), правее **мини-график скорости** (мини-версия «Графика скорости», без прокрутки и зума): линия по пути цвета шкалы скорости (как на карте и в полном графике; для пассажирского поезда — своя шкала), подписи «0» и верха шкалы слева, сетка 0/½/верх, разрывы записи («нет данных») линию рвут; показываются последние 15 км пути (если трек короче — весь); данные — загруженный трек + живые точки записи (живые — только при согласии на сохранение треков; без согласия — только сохранённый трек, как у полноэкранного графика); в `viewOnly` выбранная точка — вертикальный курсор. Меньше двух точек — пустое состояние. **Нет согласия на сохранение треков** (не `viewOnly`): текст «Для записи скорости нужно ваше согласие на обработку геоданных» и кнопка «Настройки согласия» (открывает «Данные и конфиденциальность»; карта при этом остаётся «открытой» в состоянии экрана-владельца, поэтому «Готово» возвращает **на карту**, а не на Главный; на всех входах карты, включая «голую» карту с Главного; про Автозапись не пишем). **Согласие есть, запись не идёт** — «Для сохранения трека нажмите «Начать запись»» (про Автозапись на карте не пишем). Иначе (запись идёт) — **та же причина, что у полноэкранного графика** (`TrackEmptyReasons`: нет согласия на сохранение трека / Автозапись выключен / не выбран поезд / запись идёт, точек пока нет и т.д.; в `viewOnly` — «Нет данных о скорости»); состояние записи опрашивается раз в 3 с, пока график пуст; тап по области графика открывает полноэкранный график с кнопкой действия («Открыть согласие», «Включить Автозапись» и т.д.). «Развернуть» открывает полноэкранный «График скорости» (§6.9, этап 4b); в `viewOnly` — только если у трека есть точки. Плашка скорости вверху слева убрана (индикатор GPS остаётся).
  - **«Профиль пути» (22.10.2026, только Android).** Продольный профиль участка и **живое положение поезда на нём**. Данные — участки советника `GET /v1/advisor/sections` (список: `id, name, fromStation, toStation, lengthKm, elementsCount, updatedAt`) и `GET /v1/advisor/sections/{id}` (поля `geometry [km,lat,lon]`, `elements [startKm,endKm,gradePermille,…]`, `kmMarks [km,kmAbs,dir]`, `stations`); **только для вошедшего пользователя (25.10.2026):** оба запроса идут с `Authorization: Bearer`, без входа клиент их не отправляет (`AdvisorApiException 401 login_required`). **Без входа (25.10.2026)** `ProfileModel.needLogin = true`: ни список, ни кэш участков прежнего аккаунта не показываются; в полосе и в списке участков надпись «Профиль пути доступен после входа в аккаунт…» (`ProfileEmpty.NEED_LOGIN`) вместо «Проверьте интернет». Проверка токена — при открытии карты (`load()`). Участок приходит **запечатанным** бинарным конвертом `LD3` (`application/octet-stream`; ключ шифрования участка завёрнут открытым RSA-ключом устройства, закрытая часть остаётся в Android Keystore, по возможности TEE/StrongBox; формат — `docs/ADVISOR_SEAL_PROTOCOL.md`). **Google Key Attestation не является условием доступа (08.11.2026):** новый клиент создаёт обычный Keystore-ключ без attestation challenge и регистрирует его через прежний `POST /v1/advisor/device`; сервер при `ADVISOR_ATTEST_ENFORCE=false` извлекает открытый ключ и не обращается к сервисам Google. Старые клиенты с цепочкой аттестации совместимы; `GET /v1/advisor/device/challenge` сохранён для них. Токен устройства хранится локально ~60 дней и привязан к аккаунту и ключу. Слабый LD2 (`X-Device-Fallback: 1`, `ADVISOR_FALLBACK_LD2`) остаётся только запасом для устройств, где вообще не удалось создать/прочитать RSA-ключ Keystore, и для старого строгого сервера; ошибка не должна называться отказом «проверки подлинности». Запасной LD2 для всех - флаг `ADVISOR_ALLOW_LD2`. Конверт открывает `SectionSeal` в `data_remote`; в геометрию сервер вносит невидимый водяной знак аккаунта (≤0,11 м); **единственный серверный лимит - число РАЗНЫХ участков в сутки на аккаунт (`ADVISOR_MAX_DISTINCT_SECTIONS`, с 06.11.2026 - 300, выше числа участков: старые клиенты качают всю сеть)** (повтор того же участка, потерянный кэш и повторы клиента лимит не расходуют; лимитов по IP и по числу запросов нет; 429 → остаётся кэш). Согласие №2 не требуется, о пользователе, кроме токена входа, ничего не отправляется. **Габарит участка:** список отдаёт `bbox` `[minLat, minLon, maxLat, maxLon]`; ось и профиль качаются только для участков, чей габарит не дальше 5 км от положения (не больше 8 ближайших; перезапрос при сдвиге на 2 км) и для выбранных вручную; сервер без `bbox` (старая версия) — как раньше, все участки. Кэш на диске `filesDir/advisor_profiles` (**зашифрован ключом Android Keystore, `LocalSeal`, формат `LC1` (25.10.2026)**: файл вне устройства и вне приложения нечитаем; старые открытые файлы и файлы с потерянным ключом молча перекачиваются; в резервную копию и перенос на новое устройство каталог не входит, см. `backup_rules.xml`/`data_extraction_rules.xml`; кэш осей `cacheDir/advisor_axes` зашифрован так же) (работает офлайн). **Обновление дополнений автора (28.10.2026):** у каждого участка есть метка `updatedAt` (мс; сервер ставит её при любом сохранении участка в кабинете, в списке и в самом участке). Сохранённый участок перекачивается, как только `updatedAt` в списке стал новее сохранённого — и при открытии карты (`sectionsNear`, независимо от флага refresh; раньше при уже обновлённом списке отдавался устаревший кэш), и **в общей синхронизации** (`SyncManager.syncSectionProfiles` → `SectionProfileRepository.syncAll`, после справочника станций, в обеих ветках — двусторонней и «с сервера»): обновляется список, перекачиваются только уже сохранённые на телефоне участки с новым `updatedAt`, снятые с публикации удаляются с диска. Новые участки в синхронизации НЕ качаются (по-прежнему по положению, габарит и квоты); сеть недоступна или ответ с ошибкой — кэш не трогается, синхронизация не ломается и ошибки не показывает. **Экономия запросов (клиент, 06.11.2026):** регистрация ключа идёт строго по одной (`Mutex`), отказ регистрации (403) ставит паузу 30 мин на аккаунт, лимит регистрации (429) - 10 мин; сбой загрузки участка (сеть, 5xx) повторяется с задержкой 1, 2, 4, 8, 15 мин (дальше раз в 15) для каждого участка отдельно, отказ устройства/429 - пауза на все загрузки (30/10 мин); список участков у сервера спрашивается не чаще раза в 5 мин; на плече поезда участки у станций и через каждые 25 км (не больше 16 точек) докачиваются заранее (`ProfileModel.prefetchLeg`, останавливается при отказе устройства или лимите), чтобы в зоне без связи профиль был в кэше. Дополнение участка на сервере без смены `updatedAt` до телефонов не дойдёт: `SectionProfileRepository`, `FileSectionProfileCache`. Код: `domain/track/SectionProfile.kt` (километраж `KmMarks`, `ElevationLine`, `SectionPicker`), `LiveProfile.kt` (привязка фиксов к оси `AxisTracker`, окно, `LegMatch`), `servicephase/ProfileModel.kt`, `ProfileUi.kt`.
    - **Полоса:** слева над графиком километр и пикет («7003 км 7 пк» — без метров, 30.10.2026; по `kmMarks`; нет отметок — «N км от начала») и уклон в направлении движения («↑ 4,0 ‰» подъём, «↓ 2,5 ‰» спуск, «площадка»; вне участка — красное «вне участка»); мини-график: 1 км позади и 5 км впереди по ходу, маркер — положение. Тап по графику = «Развернуть».
    - **Выбор участка.** Автоматически: ближайший по оси участок не дальше 2 км (`SectionPicker`), а если у поезда заполнено плечо (≥ 2 станции плеча, названия сверяются без регистра/знаков) — предпочитается участок, на оси которого есть первая и последняя станции плеча; при уходе с оси (3 фикса подряд дальше 200 м) подбирается заново. Вручную: на полном экране нажать на карточку участка → список всех участков из списка сервера (сверху совпадающие с плечом — у незагруженных по названию, затем по расстоянию «рядом с вами» / «в N км от вас» — до оси, если она загружена, иначе до габарита; метки «совпадает с плечом», «профиль собирается»), поиск по названию и станциям загруженных участков; ось выбранного вручную участка подгружается при выборе (нет сети и нет в кэше — «Не удалось загрузить участок. Проверьте интернет.»); ручной выбор отключает автоподбор до пункта «Подбирать по местоположению». Если плечо заполнено, а участок не совпадает — жёлтая плашка-предупреждение (выбор не блокируется). **Список «Участок профиля» (30.10.2026):** порядок строк зависит от положения GPS (ближайшие сверху) и фиксируется в момент первого показа (на каждый поисковый запрос): дальше список не переставляется, даже если положение уточнилось (расстояния в строках обновляются, новые строки идут в конец). При открытии список сначала ждёт положение GPS до 1 с (индикатор загрузки), затем сразу показывается отсортированным; положения нет — после 1 с показывается как есть (по названию).
    - **Виртуальный участок: плечо через несколько участков (07.11.2026).** Если ни один участок не содержит обе конечные станции плеча, клиент берёт граф стыков и развилок (`GET /v1/advisor/graph`: `nodes [{id,lat,lon,name}]`, `edges [{id,sectionId,fromKm,toKm,fromNode,toNode,lengthKm}]`, `version`; сервер строит его по опубликованным участкам, кабинет «Стыки» скрывает ложные развилки и выбирает владельца общего отрезка: новее → проверенный → длинный). Путь между станциями ищет **сервер** (`GET /v1/advisor/graph/path?from_station=&to_station=` → `{version,lengthKm,traversals:[{edge,startKm,endKm}]}`, 404 `path_not_found` — станции не соединены оцифрованными участками): клиенту не нужно докачивать владельцев, чтобы найти путь; локальный поиск по уже загруженным (`GraphLeg`) срабатывает первым (работает офлайн). Из проходящих рёбер собирается «виртуальный участок» (`VirtualSection.compose`): ось, элементы профиля, станции, светофоры, переезды, места опробования тормозов и отметки километража каждого ребра срезаются диапазоном км ребра и складываются с накоплением км по ходу поезда (обратный проход ребра меняет знак уклона и `facing` светофоров). Показывается как обычный участок, в карточке — «Плечо: От — До»; направление всегда «по плечу» (ось строится по ходу). Плечо работает **без геоданных** (координат станций нет): концы плеча берутся по названиям (`LegContext.endNames` → `ProfileModel.legEnds`); карта маршрута при этом по-прежнему просит «Определить плечо». Положение по GPS главнее плеча: если GPS ближе к чужому участку, показывается он (жёлтая плашка «не совпадает с плечом», ручное «Сменить»); плечо/виртуальный участок подбираются, когда положения на участке нет или GPS ещё не поймал. Граф кэшируется по `version` (память + `advisor_profiles/graph.json`; в «Скачанных участках» не показывается); старый сервер без эндпоинтов графа — как раньше, только участок целиком. Ограничения ДУ-61 на виртуальном участке пока не привязываются — следующий шаг (правило: ввод только до ближайшей развилки). Код: `domain/track/SectionGraph.kt`, `domain/track/VirtualSection.kt`, `SectionProfileRepository.graph/graphPath`, `ProfileModel.buildVirtualLeg`.
    - **«Маршрут по факту» до ближайшей развилки (07.11.2026, без плеча).** Когда плеча нет, участок подбирается по GPS («где поезд реально едет»), а показ профиля ограничен **до ближайшей развилки пути впереди по ходу** (`FactRoute.axisRange`: узел графа с числом рёбер ≥ 3, примыкающий к оси участка в пределах 60 м; диапазон — от поезда (запас 2 км позади) до развилки; «Весь участок» тоже ограничен развилкой). Развилки впереди нет — участок показывается целиком. Позиция неизвестна («Поиск спутников…») — кропа нет. **Название в карточке профиля (07.11.2026):** показ не всегда совпадает с участком, поэтому заголовок уточняется (`ProfileModel.profileTitle`): виртуальное плечо — «Плечо: От — До»; профиль обрезан по плечу — «Плечо: От — До» (а не имя участка); обрезан «по факту» — «Маршрут по факту» (а не имя одного участка); без обрезок — имя участка как раньше. Кнопка «Сменить» и список участков остаются с настоящими именами. Подпись «до ст. X» у ближайшей развилки — в строке «Сейчас» (07.11). **Чипы веток (07.11, решение владельца):** над лентой профиля показываются ветки за ближайшей развилкой («→ Богдо · 27,8 км» — ближайшая станция владельца ветки или узел в конце); нажатие — отметка выбранной ветки (подсветка чипа). Профиль при этом **не переходит за развилку**: он всегда обрезан до неё, как плечо меньше участка (автоподгрузка выбранной ветки в профиль убрана 07.11 как лишняя). После прохода развилки дальше поезда ведёт GPS-подбор. Код: `domain/track/SectionGraph.kt` (`FactRoute`, `Branches`), `ProfileModel.refreshGraphAhead`, `ProfileUi.BranchChipsRow`.
    - **Что видит пользователь: карта и профиль по состояниям (07.11.2026).** Правило-скобки: **профиль от геоданных плеча не зависит** (строится по именам станций через граф), **карта маршрута — зависит** (линия нуждается в координатах станций). Пустая полоса профиля всегда показывает ровно ОДНУ причину (подсказка «плечо не проверено» из профиля убрана 07.11 — она устарела и дублировала карточку на карте).

      **Профиль пути (полоса и полноэкранный) — пустые состояния (участка нет):**
      - нет входа → «Профиль пути доступен после входа в аккаунт…»;
      - идёт загрузка → «Загружаем профили участков…»;
      - нет первого GPS-фикса и ничего не подобрано → «Поиск спутников… Определяем местоположение.»;
      - фикс есть, рядом нет участков с профилем → «Рядом нет участков с профилем…»;
      - вне путей (нет участка и GPS вне рельсов) → «Вы находитесь вне путей.»;
      - участок без данных профиля → «Для участка «X» данные профиля собираются…»;
      - отказ регистрации ключа / лимит сервера → «Не удалось зарегистрировать защищённый ключ устройства…» / «Сервер временно ограничил загрузку…».

      **Ограничения ДУ-61 — добавление до ближайшей развилки (07.11.2026).** В шторке «Где действует» при добавлении ограничения с карты/профиля доступны только станции и перегоны **до ближайшей развилки впереди** по ходу движения (`FactRoute.nextJunction` по графу от текущего положения): дальше маршрут не определён. В строке «Сейчас» показывается ближайшая развилка — «до ст. X — N км» (имя узла графа; безымянный узел подписи не даёт). Если графа, положения нет или после фильтра список пуст — показываются все места. Добавление из раздела «Поезд» (без профиля) — список плеча целиком, фильтра нет.

      **Профиль пути — с содержимым (что показано):**
      - **плечо найдено** (односекционное — кроп по плечу, мультисекционное — виртуальный из графа): график плеча, заголовок «Плечо: От — До»; работает офлайн по кэшу и без геоданных плеча; положение в строке «Сейчас» — по GPS, нет фикса — «Положение на участке определяется…»;
      - **плечо нет или оно не совпадает с положением**, участок подобран по GPS: заголовок «Маршрут по факту», профиль до ближайшей развилки впереди; если плечо есть и не совпадает — жёлтая плашка «Этот участок не совпадает с плечом поезда…»;
      - **участок выбран вручную** («Сменить»): участок целиком, заголовок — имя участка, кропа «по факту» нет (автокроп только для автоподбора).
      - **Чипы веток в режиме «по факту»** (нет плеча с двумя крайними станциями): над графиком до ближайшей развилки показываются варианты продолжения («далее на …»). Если несколько параллельных рёбер ведут к одной первой станции, одинаковый чип показывается один раз; выбирается ближайшее такое ребро. При заданном плече чипы веток не показываются.
      - Сверка крайних станций плеча с профилем выполняется локально и понимает дорожные сокращения слов (например, «Гатчина ТБ» и «Гатчина-Товарная-Балтийская» считаются одной станцией), поэтому профиль плеча обрезается по найденной станции.

      **Карта поезда:**
      - плечо **с геоданными** (проверено «Определить»): линия маршрута плеча со станциями, прогресс при записи;
      - плечо **без геоданных**: карточка «У плеча нет геоданных — Определите плечо…» (линии маршрута нет); профиль внизу при этом работает;
      - геолокация выключена/нет доступа: «Нет доступа к местоположению» в шапке карты;
      - офлайн: подложка и данные профиля из «Скачанных участков» (кэш), карточка про геоданные остаётся, пока плечо не определено.
    - **Направление:** по плечу (станция отправления ближе к началу оси — вперёд), иначе по движению (набрали 300 м по оси; шаги больше 1 км и скачки не считаются), иначе вперёд; кнопка «⇄» на полном экране меняет вручную. При развороте график зеркалится, подъём становится спуском, километры идут в обратную сторону; поезд при этом по-прежнему идёт вправо (хвост слева, у головы стрелка по ходу) — относительно профиля он едет в обратную сторону, а светофоры/чётность считаются для нового направления (30.10.2026; раньше хвост переносился на другую сторону и физически направление не менялось).
    - **Зоны «нет данных» внутри профиля (23.10.2026).** Если на части плеча профиль определить не удалось, у элемента профиля стоит `noData: true` (необязательное поле, `GET /v1/advisor/sections/{id}` → `elements[]`; кабинет — колонка «Нет данных» в таблице элементов). На графике и в ряду уклонов это серая зона (`muted` с прозрачностью 22%) на всю высоту; подпись внутри зоны: на полном экране «Данные профиля собираются, скоро он появится», в полосе — «Данные собираются» (если зона шире ~60 dp); в строке «Сейчас» вместо уклона — «нет данных». Высота по линии профиля в зоне не меняется (горизонталь). Участок, у которого все элементы `noData`, считается без профиля (`hasProfile = false`, сообщение «данные профиля собираются»).
    - **Нет данных:** «Загружаем профили участков…» / «Поиск спутников… Определяем местоположение.» (пока нет первого живого фикса подписки; «последнее известное» положение профиль не использует) / «Профили участков не загружены. Проверьте интернет…» (нет ни кэша, ни сети) / «Для этого места профиль пока не собран. Данные профиля собираются — скоро он появится.» (нет участка рядом) / «Для участка «…» данные профиля собираются. Скоро профиль появится.» (у участка нет элементов профиля).
    - **«Развернуть» — полноэкранный «Профиль пути», только горизонтально** (как «График скорости»: на время показа `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, по выходу прежняя ориентация; поворачивается только открытый поверх него экран «Предупреждения», см. ниже). Сверху одна строка: «Назад», карточка участка (высота 48 dp, скругление 16 dp и тень как у кнопки «Назад»; «название», «от → к · по местоположению / выбран вручную», «Сменить» → список), масштаб «3 км / 6 км / 12 км / Весь участок», «К поезду» (только после сдвига), «⇄» (направление); если у участка есть светофоры — ещё две кнопки-переключателя (08.11.2026): «1П»/«2П» (путь светофоров) и «Авто»/«Чётное»/«Нечётное» (чётность направления), см. «Светофоры на профиле». Режимные рекомендации («80/540», «ОП») не показываются (решение 22.10.2026).
    - **График и ряды (как режимная карта):** заливка высот (интеграл уклонов от начала участка), станции пунктиром с названиями, границы км (столбы) — пунктирные вертикали **от линейки вверх до линии профиля** (рисуются поверх заливки). Под графиком: **ряд уклонов** — клетка на элемент профиля шириной по его длине: диагональ вверх (подъём) или вниз (спуск) в направлении движения (тонкая, 1 dp, светлее линии профиля — 55% её цвета; на стыке соседних элементов — вертикальные соединения между концом одной и началом следующей линии), сверху уклон в ‰ (одна цифра после запятой, «0» — площадка), снизу длина элемента в метрах; клетка под поездом подсвечена; числа рисуются, только если помещаются. **Ряд кривых** — только если в данных есть радиусы (`curveRadiusM > 0`): выступ и «R…»; сейчас в данных радиусов нет, ряда нет. **Линейка:** номер км по центру километровой ячейки, под ним пикеты 1–10 с рисками (столб км — длинная риска); пк 1 — первые 100 м после столба; при убывающей нумерации номера идут в обратную сторону. Маркер положения — по всей высоте (серый, если вне оси или нет уверенной привязки).
    - **Линия поезда (23.10.2026).** На профиле положение — не только точка: от головы (положение GPS с поправкой) назад по ходу вдоль линии профиля идёт толстая линия длиной с поезд. **Границы поезда** — голова (с точкой на линии профиля) и хвост — обозначены вертикальными линиями **вниз** от линии профиля до километровой разметки; вверх линий нет. Цвет акцентный, серый — если нет уверенной привязки; вне участка линии нет. **Окно профиля всегда включает поезд целиком:** в полосе позади головы не меньше длины поезда + 150 м, на полном экране при слежении то же (но не больше 80% окна). Длина — из данных поезда `Train.conditionalLength` (УД): у обычного поезда × 14 м, у пассажирского номера (по номеру поезда, `UtilsForEntities.passengerTrainNumberList`) × длина вагона из настроек (`passengerWagonLengthMeters`, по умолчанию 24,5 м) — та же функция `conditionalLengthToMeters`, что показывает «· N м» в форме поезда. Длина не указана — линии нет, остаётся только точка. **Выбор поезда:** карта открыта из «Поезда» — этот поезд; с Главного — поезд текущего маршрута по правилу «Автозапись» (`TrainOrdering.pickAutoTarget`; поездов несколько — последний); «голая» карта (нет текущего маршрута или в нём нет поезда) — без поезда. В строке «Сейчас» чип «№2552 · 798 м» (или «длина не указана») открывает список поездов маршрута для смены; для поезда в форме учитываются несохранённые правки длины. Код: `TrainFormViewModel.trainMapTrains()`, `ProfileTrain`, `TrainPickerDialog`. **На географической карте (плане)** поезд показывается одним из двух способов, переключатель «Поезд на плане» в «Настройках карты» (не в режиме просмотра трека; `train_map_prefs` `train_as_line`, по умолчанию «Линией»): «Линией» — линия состава вдоль оси выбранного участка (белая окантовка + синяя линия поверх пройденного пути, под точками станций), «Точкой со значком» — только значок навигации из «Значок навигации». Карточка выбора значка в «Настройках карты» показывается только при «Точкой со значком»; над ней в обоих режимах карточка «Так это будет выглядеть» с примером на мини-карте (рисуется на Canvas, не настоящая карта): изогнутый путь, зелёное пятно, станция; при «Линией» — синяя линия поезда с белой окантовкой, при «Точкой со значком» — выбранный значок на пути, повёрнутый по касательной (тот же растр вида сверху, что на настоящей карте); значок в примере меняется сразу при выборе в сетке ниже (значок на плане не используется, а при запасном показе без линии берётся ранее выбранный). Два варианта сразу не показываются: при «Линией» значок скрыт; линии нет (нет длины поезда, положения на оси или участка, вне оси) — вместо неё показывается значок. Линия от головы назад по ходу, у начала/конца участка обрезается по участку; она не ломается в вершинах оси, а повторяет изгиб пути — кривая Катмулла-Рома через точки оси с шагом до 20 м (`AxisSlice.smooth`); голова — положение по GPS **без** ручной поправки (поправка двигает только положение на профиле, на карте поезд там, где GPS). **Масштаб при открытии:** если карта открыта без маршрута и без записанного трека, а линия построена, камера один раз охватывает всю длину поезда (запас: сверху плашка, справа кнопки, снизу панели), дальше слежение держит этот масштаб (не меньше 14). Код: `LiveProfile.trainPath`, `AxisSlice.between`, источник `train-map-body`.
    - **Зум и сдвиг:** щипок двумя пальцами масштабирует окно (от 0,3 км до всего участка) вокруг точки между пальцами; чистый щипок не отключает слежение за поездом. Сдвиг пальцем отключает слежение и показывает «К поезду» (возврат). Пресеты масштаба включают слежение (кроме «Весь участок»). При слежении окно сдвигается за поездом при каждом новом положении GPS (экран подписан на положение), масштаб остаётся выбранным; поезд держится на 1/6 ширины окна от левого края (не меньше длины поезда + 150 м позади головы).
    - **Ручная привязка («Уточнить км», кнопка в строке «Сейчас»; только у участков с километровыми отметками).** Диалог «Уточнить километр»: поля «Км», «Пикет» (1–10, по умолчанию 1), «+ м» (0–99, по умолчанию 0). GPS остаётся основой, ввод даёт **поправку по оси** (км): показываемое положение = GPS + поправка, она применяется ко всем следующим фиксам. Подбор: ближайшее к показываемому положению место с введённой километровой координатой (нумерация может повторяться после смены — `KmMarks.candidates`). Ошибки: «Введите номер километра.», «Пикет — число от 1 до 10.», «Такого километра нет на этом участке.», «Положение по GPS пока не определено.»; если введённое место дальше 2 км от показываемого — предупреждение и повторное нажатие «Привязать». Новая привязка заменяет прежнюю; «Сбросить поправку» (если задана) убирает. Поправка хранится на устройстве (`SharedPreferences` `profile_calibration`, ключ — id участка + `axisHash`, на сервер не уходит; при смене геометрии участка сбрасывается сама). Текст с величиной поправки («поправка +223 м») в строке «Сейчас» не показывается (убран 23.10.2026). Код: `LiveProfile.calibrationFor/applyCalibration`, `ProfileCalibrationStore`, `CalibrateDialog`.
    - **Светофоры на профиле (24.10.2026, только Android; клиент готов, данные — только **проходные светофоры на перегонах** участка «Лужская — Мга» части «Лужская — Будогощь» (100 шт., оцифровка режимных карт ТЧЭ-14); входные, выходные, маршрутные, маневровые и предупредительные на станциях пока не вносятся — читаются на карте ненадёжно, будут позже).** Слой на графике профиля и подсказка «Светофор … — N км» в строке «Сейчас».
      - **Данные.** Необязательное поле `signals` в `GET /v1/advisor/sections/{id}`: `[{km, label, track, kind, facing}]`. `km` — место на оси участка, `label` — как напечатано на режимной карте (у проходного номер «6», у остальных вместе с литером: «Ч2», «Н1А», «ЧМ2», «IIЧВ»), `track` — литер: `"E"` (Ч) или `"O"` (Н), `kind` — `pass` (проходной) / `station` (входной, выходной, маршрутный) / `shunt` (маневровый, «ЧМ…/НМ…») / `distant` (предупредительный, «ПЧ…/ПН…»), `facing` — в какую сторону оси светофор виден лицом (+1 по возрастанию км оси, −1 против). Нет поля (старый сервер) или пусто — слоя нет, блок «Направление/Путь» не показывается. Кэш на диске — тот же JSON участка (`SectionProfileRepository`).
      - **Литеры и чётность.** Светофор литера **Ч** виден поезду **чётного** направления, литера **Н** — **нечётного**; на двухпутном перегоне оба ряда стоят рядом, лицом в противоположные стороны. **Проходные светофоры** нумеруются так: по чётному (как правило, 2-му) пути чётные номера убывают к следующей станции (…8, 6, 4, 2 — затем входной Ч), по нечётному — нечётные (…7, 5, 3, 1 — затем входной Н). **Проходной светофор подписывается только цифрами** («6», «5») — литер Ч/Н на нём не пишется ни на светофоре, ни на экране; чей он (Ч или Н), видно по цвету чипа и чётности номера. У остальных видов (когда появятся) подпись как на карте: «Ч2», «Н1А».
      - **Какое направление чётное.** Чётное направление на оси — то, куда смотрят светофоры литера Ч (`SignalRules.evenAxisDir`): ближайший к месту светофор (Ч: его `facing`, Н: обратный), в пределах 15 км; дальше — большинство по участку. Так, где ось поворачивается относительно нумерации, чётность определяется по месту, а не раз на участок. **На Лужская — Мга чётное направление — от Лужской к Мге** (к росту оси).
      - **Настройки профиля (08.11.2026 — кнопками в шапке, диалог убран).** В верхней строке полноэкранного профиля справа от «⇄» (только если у участка есть светофоры) — два переключателя одной кнопкой, размер и вид как у соседних кнопок строки: **«1П»/«2П»** (текст - текущий путь; нажатие меняет на другой; 1П - нечётные, литер Н; 2П - чётные, литер Ч) и **«Авто»/«Чётное»/«Нечётное»** (текст - текущее состояние; нажатие переводит в следующее: Авто → Чётное → Нечётное → Авто). «Авто» - направление по плечу поезда или по движению (как у кнопки «⇄»); «Чётное»/«Нечётное» - выбор вручную. По неправильному пути машинист включает светофоры того пути, по которому едет. **При повороте карты («⇄») обе кнопки принимают стандартное для нового направления состояние** (путь по умолчанию - по чётности поезда, иначе по направлению; чётность - самого направления).
      - **Выбор направления.** Автоматически: направление профиля (по плечу поезда, а без плеча — по движению, как раньше) даёт чётность — совпало с чётным направлением оси → «Чётное», иначе «Нечётное». Вручную («Чётное»/«Нечётное» в настройках) профиль разворачивается так, чтобы направление совпало (так же, как «⇄», хвост поезда переходит на другую сторону); «Авто» возвращает автоподбор. Кнопка «⇄» по-прежнему меняет направление (тоже ручной выбор). **Название в шапке участка разворачивается вместе с направлением** («Лужская — Будогощь» ⇄ «Будогощь — Лужская», у «А — Б — В» — «В — Б — А»), отдельной строки со стрелкой нет; вторая строка шапки — «нечётное · по местоположению» (чётность — только если у участка есть светофоры). Участок один, в списке он под своим названием: обратное направление выбирается плечом поезда, настройками или «⇄».
      - **Неправильный путь.** «Правильный» — показываются светофоры литера направления (чётное → Ч, нечётное → Н). «Неправильный» — поезд идёт, например, в чётном направлении по нечётному (1-му) пути: машинист ориентируется на светофоры **этого пути**, то есть противоположного литера (Н), а они стоят к нему **спиной**. Показываются светофоры противоположного литера, все с пометкой «спиной». Выбор пути не запоминается: сбрасывается на «Правильный» при выборе другого участка или «Авто» участка. Пометок «светофоры Ч/Н» на профиле нет.
      - **Внешний вид.** Над линией профиля, на полосе вверху графика, чип с подписью (`TrackSignal.title`): литер Ч — акцентная заливка (`accent`), Н — тёмная (`cta`); светофор спиной к поезду — контур без заливки и лёгкий тон. Тонкий стебель от чипа вниз до линии профиля. Чипы в два яруса, тесные пропускаются (на станциях при мелком масштабе видны не все; при приближении — все). **Знаки на ножках (08.11.2026):** переезд (дорожный знак «Железнодорожный переезд без шлагбаума», `section.crossings`) и «НТ» (места опробования тормозов, `section.brakeTests`) стоят на той же полосе на тонком стебле вниз до линии профиля — как светофор; те же два яруса и пропуск тесных. Видны и в мини-профиле полосы (подписи мельче). В строке «Сейчас» к «До «станция» — N км» добавляется «Светофор Ч4 — 1,2 км» (ближайший впереди по ходу нужного литера; «(спиной)» на неправильном пути).
      - **Код:** `domain/track/TrackSignals.kt` (`TrackSignal`, `SignalRules`, `TravelParity`), `SectionProfile.signals`, `AdvisorSignalDto`, `ProfileModel` (`parity`, `shownSignals`, `signalsAhead`, `chooseParity`, `useAutoDirection`, `wrongTrack`, `manualSignalTrack` сброс при смене направления), `ProfileUi.kt` (`SignalTrackSwitch`, `ParitySwitch`, `HeaderToggle`, значки на ножках в `ProfileChart`). Тесты: `TrackSignalsTest`.
  - **Скрытые полосы:** каждая остаётся на своём месте (профиль выше, скорость ниже). «Скрыть» заменяет полосу кнопкой «Профиль» / «Скорость» (стрелка вверх) **в её же слоте**: скрыт профиль — кнопка над полосой скорости, скорость остаётся внизу; скрыта скорость — кнопка внизу под полосой профиля; скрыты обе — две кнопки в ряд внизу, поровну. Нажатие возвращает полосу. Видимость запоминается отдельно (`train_map_prefs`: `profile_visible`, `speed_visible`; по умолчанию обе показаны). Кнопка записи сохраняет ширину и всегда над полосами/кнопками.
- **Кнопка записи** — над полосами, во всю ширину, значок состояния + подпись «Начать запись» / «Остановить запись»; условия показа те же, что ниже. Карточки планирования/чипы/баннер «Строим путь…» сдвинуты над нижней областью.
- **«Настройки карты»** (кнопка со значком ползунков в верхней строке, есть и в `viewOnly`) — полноэкранный экран: переключатель «Зоны плохой связи» (прежний слой «Зоны без GPS», §ниже); «Экран не гаснет при включённой карте» (по умолчанию включён); «Значок навигации» (выбор из 8 значков, `PositionIconStore`); «Чувствительность локации» — ползунок 1..5 («Низкая» … «Высокая», по умолчанию 3 = прежнее поведение, `train_map_prefs`, `location_sensitivity`) с пояснением выбранного уровня и общей подсказкой: выше чувствительность — быстрее реакция маркера, но больше шума. При включённой настройке экран не гаснет, пока открыта карта, включая полноэкранный «Профиль пути». Все настройки карты хранятся локально в Android `SharedPreferences` `train_map_prefs` и между устройствами не синхронизируются. Ползунок применяется при отпускании. Влияет только на отображение положения на карте (`LocationSensitivity`: радиус гашения дрейфа на стоянке 150/120/100/60/30 м и порог «поезд едет» 3,0/2,5/2,0/1,5/1,0 км/ч → `DriftFilter.Config`); запись трека «Автозапись» не затрагивается. В `viewOnly` остаётся только переключатель зон.

**Кнопка записи (решение 09.10.2026; расположение изменено 21.10.2026 — теперь внизу карты с подписью).** Исторически — в правой колонке: квадратная кнопка 48 dp «Начать запись» /
«Остановить запись» (описание для доступности). Есть во всех входах, где карта «живая»: из экрана «Поезд», с Главного плиткой «Карта»
(и с поездом — через «Поезд», и без поезда — «голая» карта); **в просмотре записанного трека из «Истории» (`viewOnly`) её нет**. Состояние кнопки =
режим «Автозапись» (`AutoRecordManager.state.enabled`), обновляется живо (тот же индикатор, что у чипа «Автозапись»): выключен — кольцо с красной
точкой; включён — красный квадрат «стоп» и пульсирующая красная точка-бейдж в углу (состояние «прервано» считается «не пишет»). Нажатие — то же
переключение, что у чипа «Автозапись» (`rememberAutoRecordToggle`): выключение без подтверждения (`manager.disable()`); включение — если нет решения по
согласию №1 на сохранение треков, стандартный экран согласия (как при первом включении «Автозапись»), затем разрешения геолокации/уведомлений и подсказка про
батарею (как у «Автозапись»). При смене состояния — короткая подсказка (Toast) «Запись трека включена» / «Запись трека остановлена».
Цель записи: карта открыта из «Поезда» (в том числе с Главного, когда у текущего маршрута есть поезд, §4.4) — **этот поезд**; карта без поезда
(Главный, поездов в текущем маршруте нет) — цель определяется в момент включения (`TrainOrdering.pickRecordTarget` + `AutoRecordManager.resolveRecordTarget`):
поезд текущего маршрута по правилу «Автозапись» (`pickHomeMapTarget`); если в текущем маршруте поездов нет — **создаётся пустой поезд** в нём (`TrainUseCase.updateTrain`);
если текущего маршрута нет — **маршрут НЕ создаётся**: трек пишется локально «без поезда» (`RecordTarget.NoTrain`, `Track.trainId = NO_TRAIN_ID` = `""`;
колонка `trainId` остаётся NOT NULL, схема `Track.db` не меняется, миграции нет). Данные создаются только в одном случае: в текущем маршруте нет поезда (пустой поезд, по нажатию кнопки записи).
**Трек «без поезда»** (решение 09.10.2026): в «Моих треках» заголовок «Без поезда», без строки «Явка»; открывается на карте как обычный (`viewOnly`); не считается
сиротой (`TrackOrphanCleaner.clean/visible` его пропускают); на сервер не отправляется никогда (серверу нужен `routeId`; `TrackUploader.needsUpload` = false), в меню карточки нет
«Синхронизировать»; индикатор — нейтральный «не синхронизирован» (без красного); события станций «Автозапись» не записываются (нечего писать) - пишется только трек; если пока идёт запись
появился текущий маршрут, при ближайшем событии стоянки цель переходит на его поезд (новый трек). Удаляется только вручную или по сроку 180 дней (`purgeExpired`).
**Что доступно бесплатному и неавторизованному пользователю (решение 09.10.2026; целевое поведение).** Всё работает в полном объёме и без аккаунта, и без
подписки — чтобы сразу после установки можно было оценить функционал: карта (тайлы через свой прокси `app.locodriver.ru/tiles`, публично, без токена; запасные
подложки OpenFreeMap/OSM), текущее положение, запись трека локально (при согласии №1), остановки по треку, станции по справочнику, а также серверные
вычисления карты и станций — линия плеча по рельсам (`POST /v1/service_phase/route/length` с геометрией), станция по рельсам (`stations/nearest` с `alongRail`/`candidates`),
автоподбор станций (`stations/suggest`), `stations/lookup`. Эти 4 эндпоинта (`stations/suggest`, `stations/lookup`, `stations/nearest`, `route/length`) для сервера не требуют JWT: без `Authorization` - аноним, лимит по IP
(120/час; тяжёлый `route/length` с `alternatives > 1` - 30/час), для вошедшего - лимит на пользователя; подписка не проверяется. **Клиент (реализовано, ветка feature/anon-geo-client):**
токен необязателен (`RemoteRestApi.suggestServicePhaseStations/measureRouteLength/nearestStation/lookupServicePhaseStation(token: String?, ...)`) - нет токена: запрос без
`Authorization`; токен есть: как раньше; **401 с токеном (просрочен/невалиден) - один повтор того же запроса без токена** (`withAnonymousFallback`); такой 401 **не** вызывает `SessionExpiredNotifier.signal()` (нет разлогина): запрос помечен `GeoRequestKey`, а если анонимный повтор тоже не прошёл - ошибка как обычно. Нет UI-гейтов «требуется вход» для
линии плеча и пунктира, станции по рельсам и кандидатов, автоподбора, замера, вариантов маршрута. **429 (`GeoRateLimit`):** клиент запоминает паузу по `Retry-After` (нет заголовка - 60 с)
и до её конца не шлёт гео-запросы: интерактивные места (автоподбор, замер, поиск координат, линия на карте плеч и поезда) показывают «Слишком много запросов. Попробуйте позже
или войдите в аккаунт» и «Повторить» (до конца паузы повтор сразу даёт тот же отказ); на карте поезда - плашка «Путь по рельсам не построен. {это сообщение}.» с «Повторить»;
фоновые («Автозапись», определение названия остановки) молча берут запасной вариант (справочник по прямой, `RailLookupResult.Unavailable(retryAfterMs)`) и не обращаются к серверу до конца паузы.
Если сервер выключил анонимный доступ (kill-switch, 401 «Not authenticated» без токена) - те же состояния ошибки, что при недоступности сервиса.
**Недоступно без подписки:** синхронизация (маршруты, **треки**, справочники, настройки); серверные треки не загружаются. **Недоступно без входа в аккаунт:** вклад высот (согласие №2, `/v1/advisor/...`) и синхронизация.
**Синхронизация треков - только с активной подпиской** (`RouteActionsHelper.hasActiveSubscription()`, как маршруты): без неё `TrackUploader`/`TrackUploadWorker` не отправляют треки и не создают метки
(`Outcome.NO_SUBSCRIPTION`; очередь удалений и согласие доставляются), «Мои треки» не загружают серверные (§17.8). После оформления подписки накопленные локальные треки с синхронизированным рейсом уходят сами
(`SubscriptionNoticesViewModel` запускает воркер при появлении активной подписки). Согласие №1 остаётся отдельным условием хранения на сервере.
Карта также открывается плиткой «Карта» на Главном (§4.4): либо карта поезда текущего маршрута напрямую поверх Главного
(`HomeTrainMap`, без формы «Поезд»; закрытие возвращает на Главный), либо — если поезда нет — только карта (состояние 1) без
привязки к поезду и записи данных.
Содержимое по состоянию:
1. **Ничего не заполнено** — пустая карта с текущим местоположением (синяя точка, камера на неё, масштаб 14). Разрешение
   геолокации запрашивается при открытии карты, если ещё не выдано; без него карта остаётся на Москве, точки нет.
   Подгонка камеры (общая `fitCameraTo`, все карты): 0 точек — камера не двигается; одна точка или все совпадают (охват < ~11 м) —
   центр на точке, масштаб 14 (или выбранный пользователем); охват границами — только при 2+ различающихся точках.
2. **Выбрано плечо, у которого есть ≥ 2 станции с координатами** (отправление/прибытие из справочника + промежуточные;
   `TrainMapRoute.stops`, `TrainFormViewModel.trainMapStops`) — **будущий маршрут (этап 2)**: линия по рельсам запрашивается
   тем же `ServicePhaseAutoFillViewModel.railLine`, что у карты плеч (§6.6: всегда запрос к серверу, сохранённая — запас без
   сети) и рисуется **тонким пунктиром** (2,5 px, цвет акцента). Станции плеча — точками (крайние крупные тёмные, станции
   средние, о.п. мелкие) с подписями 11–13 px. **Подписи станций показываются только при таком зуме, когда текст помещается:**
рамка подписи (под точкой по центру) не должна пересекать ни одну точку (станции плеча и остановки) и ни одну уже выбранную
подпись; иначе у станции только точка без подписи. При конкуренции сначала выбираются крайние и «впереди», затем остальные
по порядку; при приближении расстояния между точками на экране растут и подписи появляются. Расчёт — `LabelPlacer`
(domain), пересчитывается при каждой остановке камеры (`onCameraIdle`), по событию «карта в покое» (`OnDidBecomeIdleListener`) и смене данных.
**Дубли с подписями базовой карты (10.10.2026):** перед расчётом запрашиваются отрисованные подписи symbol-слоёв стиля (станции, населённые пункты;
кроме слоёв `train-map-*`, `queryRenderedFeatures`); подпись приложения не рисуется (остаётся точка), если рядом (**≤ 40 dp**, `LabelDedup.RADIUS_DP`)
есть подпись базовой карты с тем же названием (без регистра и «ё», допускается «Луга» / «Луга I»; `LabelDedup`, domain). **Дубли своих подписей одного названия** в радиусе 300 м (`LabelDedup.sameNameDuplicates`; остановки по треку в парке станции) сводятся к одной: остаётся с меньшим приоритетом (крайняя/«впереди»), при равенстве - ближайшая к центру карты; маркеры остаются. Источники пишутся,
только если результат изменился (иначе «карта в покое» зацикливалась бы).
   **Ближайшая станция впереди** — крупная акцентная точка; считается **вдоль рельсовой линии** (`TrainMapRoute.nextAhead`):
   положение и станции проецируются на линию с сервера, впереди — первая станция, чья проекция дальше проекции положения
   (до начала маршрута — первая); расстояние до неё тоже по линии (для будущих ETA/этапа 5), не по прямым между станциями.
   Без положения или без линии (ещё строится / не найдена / ошибка) не выделяется. **Маршрут идёт только по рельсам:** прямых
   между станциями не рисуется нигде; нет линии — ничего не рисуется, показывается ошибка/«Повторить». Камера охватывает линию (пока её нет —
   станции) и текущее место, повторно подгоняется, когда пришла линия/первая точка местоположения; кнопка «Моё
   местоположение» центрирует на себе (масштаб 14). Пока линия строится — снизу карточка со спиннером «Строим путь по
   рельсам…»; «путь не найден» — «Не удалось проложить путь по рельсам между станциями плеча. Проверьте координаты станций —
   точка могла оказаться в стороне от железной дороги.» и кнопка «Уточнить» (см. §6.6); сбой сети/сервера — «Путь по рельсам не построен. {причина}.» и кнопка
   «Повторить» (тексты как в §6.6, напрямик между станциями линия не рисуется); из кэша — жёлтая плашка «Маршрут показан из
   сохранённых данных» (без интернета под ней подпись «Нет интернета»; подпись скрывается, как только сеть вернулась, а линия из кэша / «не построена»
перезапрашивается автоматически — `RailRefetch`, прежняя линия на время перезапроса не убирается; плашка исчезает, когда пришла свежая линия). **Функции, которым нужен интернет** (сейчас — «Повторить» построение
линии; этап 5 — расчёт прибытия по тапу на точку впереди) без сети не выполняются, а показывают диалог (`AppAlertDialog`, DIALOGS_STANDARD):
заголовок «Нет интернета», текст «Эта функция работает при наличии интернета. Проверьте подключение и повторите.», кнопка «Понятно».
Общий хелпер — `NetworkRequiredDialog.kt` (`rememberNetworkGate().run { ... }`, `rememberIsOnline()`), проверка связи — `isOnline`/`onlineFlow`
в `core_android` `NetworkUtils` (сеть с INTERNET + VALIDATED). Тап по станциям на карте поезда сеть не использует (остановки из локального трека),
«Определить координаты по GPS» — тоже только GPS. Если у плеча координат меньше двух — маршрут не рисуется (остаётся состояние 1/3). Панель состояния 3
   (нет координат у станции поезда) показывается под картой маршрута независимо.
3. **Координаты станций не определены** — внизу панель: «У станции «X» нет координат» / «Найдём станцию на карте по названию — вы подтвердите место перед сохранением в справочник.»; кнопки «Найти станцию на карте», «Указать на карте» и «Закрыть» (панель скрывается до следующего открытия карты). X — последняя названная станция поезда, которой нет в справочнике станций с координатами; если таких нет — отправление/прибытие выбранного плеча без координат (`StationDirectory.stationWithoutCoordinates`). **Геолокация пользователя не используется** (30.10.2026: он может заполнять данные не на месте).

   **Порядок с экскурсией (31.10.2026).** Первое открытие карты начинается с экскурсии (решение о ней принимается в первом кадре); панель «нет координат» не показывается, пока экскурсия открыта, и появляется после её закрытия. **Плечо без геоданных** (у выбранного плеча нет координат отправления или прибытия — так у всех, кто обновился с версии, где плечи хранились без координат): заголовок панели «У плеча нет геоданных», пояснение «Определите плечо A — B: координаты станций, расстояние и промежуточные станции — тогда на карте появится маршрут.», широкая синяя кнопка «Найти автоматически» (та же автоподборка, что в «Плечо» — `LegDetermineRow`, прямо на карте; пояснительного блока под ней нет — пояснение уже в заголовке шторки), «Открыть редактор плеча» и «Указать на карте»; «Найти станцию на карте» в этом случае скрыта. Если автоподбор требует уточнений (несколько путей/станций, станция не найдена, путь не проложен), диалог «Плечо требует уточнений» говорит «Откройте редактор плеча, чтобы уточнить.» с кнопками «В редактор» и «Не сейчас»; «В редактор» открывает шторку редактора **этого** плеча (на карте с Главного — поверх карты, карта остаётся; из формы поезда карта закрывается), «Уточнить» при «путь не найден» делает то же без автоподбора. После определения недостающая станция пересчитывается и панель уходит сама.
   - **«Найти станцию на карте»** — тот же поиск по названию, что «Найти координаты» в редакторе плеча (`ServicePhaseAutoFillViewModel.lookup`, страна из настроек, ориентиры — станции плеча), требует интернета (`networkGate`). Найдена одна точная — камера переходит к ней, ставится синяя точка и панель «Нашлось: Имя, станция · оператор» с кнопками «Сохранить координаты» и «Не та станция». Несколько вариантов или неточное совпадение — шторка выбора (`StationChoiceSheet`), после выбора то же подтверждение. Не найдено — «Станция не найдена на карте. Укажите место вручную.»; сбой сети — «Не удалось выполнить поиск. Проверьте интернет и повторите.».
   - **«Указать на карте»** — панель сменяется подсказкой «Нажмите на карте место станции «X»» с «Отмена»; тап по карте **только отмечает** синюю точку и показывает «Станция «X» здесь?» с кнопками «Сохранить координаты» и «Выбрать другое место» (следующий тап переставляет точку).
   - Запись в справочник (§17.4) — только по «Сохранить координаты», через `StationDirectory.fillCoordinates`: **только если у станции координат нет** (новая запись создаётся, существующие координаты и нормы не перезаписываются). После сохранения — «Координаты станции «X» сохранены» (гаснет через 4 с) и, если есть ещё станции без координат, предложение для следующей.
4. **Пройденный путь (этап 4а)** — если для поезда есть записанный трек (`Track.db`, ниже; все треки поезда по времени),
   он рисуется **жирной линией** поверх пунктира плеча: толщина 3→5,5→8 px на зуме 6/12/16 (пунктир — 2,5 px) на белой
   полупрозрачной подложке (+3 px) — контраст на светлой подложке карты (карта всегда светлая, отдельной тёмной подложки нет;
   плашки поверх карты берут цвета темы приложения). Цвет по скорости — **ступенчатая шкала `SpeedScale`** (решение 09.10.2026; одна на карту, график и легенду; границы
   включительно сверху): грузовой — до 20 км/ч зелёный, (20; 70] жёлтый, (70; 80] оранжевый, свыше 80 красный; **пассажирский/пригородный поезд**
   — до 20 зелёный, (20; 120] жёлтый, (120; 160] оранжевый, свыше 160 красный (**допущение**: в задании заданы только 20 и «красный свыше 160»,
   пороги 120/160 жёлтого–оранжевого приняты; правятся в `SpeedScale.limits`). Пассажирским считается поезд, у которого `Train.trainCategory()` (по номеру)
   — «Скорые…», «Скоростные…», «Высокоскоростные», «Пассажирские…», «Пригородные…», «Обслуживаемые моторвагонным…», «Туристические», «Людские»
   (`SpeedScale.isPassengerCategory`; грузопассажирские, «без пассажиров», толкачи и всё прочее — грузовая шкала); `TrainMapTrack.passenger`.
   Просмотр трека из «Моих треков» — шкала по категории поезда трека (локальный трек: поезд ищется по `trainId` в рейсах; пассажирский/пригородный — пассажирская шкала); серверный трек без локальной копии или поезд не найден — грузовая. Оттенки: карта (всегда светлая) —
   зелёный `#2E7D32`, жёлтый `#F9A825`, оранжевый `#EF6C00`, красный `#D32F2F`; график в тёмной теме — светлее (`#66BB6A`, `#FDD835`, `#FFA040`,
   `#EF5350`); сегмент без скорости — серый `#78909C`; разрыв между точками > 5 мин линию не соединяет. Для отрисовки трек **прореживается**
   (Дуглас–Пекер, допуск 4 м, не более ~2500 точек — допуск удваивается; точки смены 10-км/ч «корзины» скорости и разрывов
   сохраняются); когда есть рельсовая линия маршрута, точки с отклонением от неё ≤ **50 м** (`TrainMapRoute.SNAP_TOLERANCE_M`) рисуются привязанными к линии
   (проекция, сглаживает шум GPS), остальные — сырыми (`TrainMapRoute.snapMarked`) — **«вне путей»**: такие участки получают тонкий тёмный
   пунктир поверх цвета скорости, а в левом столбце под лентой — оранжевая плашка «Вне путей» (текущее положение дальше 50 м от линии);
   плашки «Участки вне путей - пунктиром» нет (убрана 10.10.2026, остаётся только сам пунктир); время/скорость в тапе — из исходных точек.
   **Без зигзагов (11.10.2026, `PathCleaner` + `ActualRails.snapMarked`; поле: парк Будогощь, ~8 путей через 5 м):** перед привязкой к путям
   сырая ломаная очищается по участкам (разрыв > 5 мин участки разделяет): (1) **стоянка/маневры** - подряд идущие точки со скоростью < 3 км/ч
   в пределах 50 м от центроида кластера заменяются одной точкой-центроидом (время первой точки и копия на время последней, чтобы длинная
   стоянка не рвала линию разрывом; точки без скорости = движение); (2) **возвраты/острые углы** - точка, в которую линия приходит и сразу
   возвращается (ноги 20-300 м, угол при вершине < ~60 градусов), отбрасывается; (3) Дуглас-Пекер 4 м (скорость оставленной точки - средняя
   по покрытому участку). Привязка к пути - **с непрерывностью**: пока точка в допуске от текущей линии, смена на соседнюю (параллельный путь)
   только если та ближе на >= 2 x ошибку (accuracyM, по умолчанию 5 м, не меньше 2 м) и так 3 точки подряд; поодиночные скачки между путями не
   рисуются. «Хвост» до положения не рисуется на стоянке (скорость < 3 км/ч, положение в 50 м от последней точки). Линия по факту: участок
   для запроса и ответ сервера отклоняются, если ломаная (via-точки и сам участок/линия) длиннее 1,3 x хорда + 100 м или самопересекается
   (via-точки с разных параллельных путей дают петли); к прежним условиям (<= 50 м от точек, <= 1,5 x участок + 300 м) это добавляется.
   **Параллельный/соседний путь (10.10.2026, `ActualRails`):** «вне путей» и привязка считаются относительно ЛЮБОГО известного пути — линии плеча и
   «линий по факту» (до 6 на экране, только в памяти): точка привязывается к ближайшему из путей (≤ 50 м), «Вне путей» и пунктир — только если дальше
   50 м от всех. Линия по факту строится, когда трек в движении (≥ 3 км/ч) устойчиво (≥ 5 точек и ≥ 200 м, без разрывов > 5 мин) отклонён > 15 м от
   всех известных путей: `POST /v1/service_phase/route/length` (`withGeometry`, контракт без изменений) через 2–6 via-точек из этого участка трека
   (первая, последняя и ~через 400 м); принимается, только если все точки участка ≤ 50 м от линии и она не длиннее 1,5 × участок + 300 м; сбой/отказ —
   прежнее поведение, тот же участок не перезапрашивается (растущий участок — другой ключ). Просмотр трека (`viewOnly`) линии по факту не строит.
   Остаток до станций, ЭТА и план прибытия считаются вдоль линии плеча: положение на линии по факту проецируется на линию плеча (`alongRouteFix`),
   поэтому «Расчёт прибытия недоступен: дальше 50 м» не показывается, пока поезд на рельсах.
   **Пройденный путь не пропадает (10.10.2026):** рисуется из локальных данных (Track.db + живые точки записи `GpsRecorder.points`, `PathKeeper`)
   и не зависит от интернета и рельсовой линии (привязка к рельсам — улучшение; сбой привязки -> сырые точки). Ошибка/пустой/«усохший» результат
   перечитывания не затирает прежний путь (`PathKeeper.keep`); пока пути нет, перечитывание каждые 10 с идёт и без записи. Слои приложения добавляются
   в каждый загруженный стиль (`OnDidFinishLoadingStyleListener` + счётчик `styleGen`): смена стиля (прокси -> OpenFreeMap -> растр) или перезагрузка
   стиля не теряет путь, точки и подписи — данные перерисовываются.
   **Линия не отстаёт от маркера:** пока «Автозапись» пишет, жирная линия дорисовывается «хвостом» от последней точки трека (она прорежена
   и подгружается раз в 10 с) до текущего положения (привязанного к линии, если ≤ 50 м), цвет — по живой скорости.
   Чтение и прореживание — асинхронно (`Dispatchers.Default`), UI не блокируется; пока «Автозапись» пишет, трек
   перечитывается раз в 10 с. Тап и поиск ближайшей точки работают по полному (непрореженному) треку.
   **Начало и окончание пути (решение 09.10.2026):** на первой точке трека — значок «Старт» (зелёный круг с белым треугольником), на последней —
   «Финиш» (красный круг с клетчатым флажком); 28 dp, белое кольцо и тёмная обводка (контраст на любой подложке), поверх линии, под маркером положения
   (`TrainMapEndpoints.kt`). Финиш показывается, только когда запись не идёт (или в просмотре трека); пока «Автозапись» пишет, конец пути — текущее
   положение. Тап по значку (до 24 dp) — карточка «Старт чч:мм:сс» / «Финиш чч:мм:сс» и скорость точки; значок проверяется раньше остановок.
   **Живое положение и «следование» (этап 4а).** Положение берётся из **подписки на обновления** (`DeviceLocation.updates`:
   все включённые провайдеры — GPS и сеть (в помещении/до первого GPS-fix положение приходит от сети; сетевой fix игнорируется 10 с после GPS-fix, чтобы
   маркер не прыгал между источниками); раз в секунду, только пока карта открыта — подписка снимается при закрытии), а не из
   разового «последнего известного» (из-за него в этапе 2 точка не двигалась после `adb emu geo fix`). Маркер: синяя точка;
   пока курс неизвестен — синяя точка, дальше — **значок положения** (стрелка по умолчанию, см. ниже), повёрнутый по курсу. **Режим «следовать»**
   включён при открытии: пока есть живое положение и поезд едет (≥ 2 км/ч), камера плавно (900 мс, `easeCamera`) ведёт к
   положению, масштаб не меньше 14 (больший, выставленный пользователем, сохраняется); в этом режиме охват маршрута/трека
   камерой не применяется. Ручной жест (перетаскивание/зум) следование выключает; кнопка «Моё местоположение» включает снова
   и центрирует. **Автовозврат в слежение (10.10.2026):** слежение — состояние по умолчанию; через **10 с без касаний** (`FollowReturn.RETURN_DELAY_MS`,
   единственное место константы) после ручного перемещения/зума/поворота карта сама возвращается: слежение включается, камера плавно (600 мс) идёт
   на положение с прежним (до жеста) масштабом, «еду по карте» включается снова, если было включено. Таймер сбрасывается каждым касанием/жестом/кнопкой «+»/«−»;
   не возвращает, пока палец на карте, открыта карточка планирования или выбранной точки (тап по пути/остановке), график скорости или панель значка;
   при закрытии карточки отсчёт 10 с начинается заново. В просмотре трека (`viewOnly`) не применяется. Стоим (< 2 км/ч) — камеру не двигаем (кроме режима «еду по карте»).
   **Масштаб при следовании:** выбранный кнопками «+»/«−» масштаб запоминается (`userZoom`) и сохраняется при каждом шаге следования (раньше
   следование каждую секунду возвращало масштаб ≥ 14 и «−» не работал); без выбора — не меньше 14; ручной жест сбрасывает следование и выбор.
   **Курс маркера (этап 4в, `HeadingEstimator`):** `Location.bearing` провайдера (ровно 0,0 считается «нет курса» — эмулятор/mock
   отдают 0), иначе азимут между опорной и текущей точкой при сдвиге ≥ 5 м; при скорости < 2 км/ч сохраняется последний курс (дрейф GPS
   на стоянке курс не меняет); новый курс применяется сглаживанием по кратчайшей дуге (доля 0,5 на фикс).
   **Значок положения:** «Стрелка» (по умолчанию), «ВЛ-10», «ВЛ-80», «2ЭС6 «Синара»», «2ТЭ25КМ «Витязь»», «Ласточка», «Паровоз», «ЧМЭ3» (`PositionIcon`).
   У каждого два вектора (VectorDrawable `ic_posicon_<ключ>_top/_iso`, исходные SVG — `docs/design/position_icons/`): **вид сверху**
   (на карте, «нос» вверх, поворачивается по курсу; белый ореол + тёмный контур; длина 56 dp, у стрелки 44 dp) и **изометрия** (только
   превью в списках выбора, не поворачивается). Ключи хранения: `arrow`, `vl10`, `vl80`, `es6`, `te25km`, `swallow`, `steam`, `chme3`; миграция старых значений:
   `diesel` → 2ТЭ25КМ, `electric` → ВЛ-10, `emu` → Ласточка, `steam` → Паровоз, остальное (вагон, грузовой, неизвестное) → Стрелка.
   Ливреи (v2, по прототипам; все двухсекционные стыкуются вплотную, на изометрии лишь тонкий шов; сверху — скошенные носы с лобовыми
   стёклами, крыша с оборудованием и пантографами, ореол и контур сохранены):
   «ВЛ-10» и «ВЛ-80» — классическая зелёная ливрея с жёлтой полосой, серая крыша с люками и по пантографу на секцию, жалюзи на бортах
   (ВЛ-10: широкая полоса по борту и поперёк кабин, пологий лоб; ВЛ-80: две тонкие полосы, козырёк над лобовыми стёклами и скошенный лоб,
   рёбра жалюзи на крыше); «2ЭС6 «Синара»» — красно-серая ливрея РЖД по фото-справке (красные кабины с красной диагональной полосой на борту, светло-серая
   середина кузова, красная полоса по верху борта и красная «пирамидальная» полоса к торцу, тёмно-серый низ и тележки, серая крыша
   с красными пантографами, тёмное лобовое стекло с козырьком, белая окантовка спереди), сильно скошенный лоб, полупантограф на секцию;
   «2ТЭ25КМ «Витязь»» — красно-серая ливрея РЖД (ярко-красные кабины с двойным тёмным лобовым стеклом, красный передний торец
   с тёмным буферным брусом, чёрная/тёмная крыша, серебристо-серый капот с красной ступенчатой полосой от кабины, тёмный низ рамы
   и тележки, красный овал «РЖД» с белой точкой на борту, жалюзи на капоте и вентиляторы на тёмной крыше); «Ласточка» — пять вагонов,
   тёмно-серая крыша, серо-белый борт с рядом окон, красно-оранжевая полоса вдоль борта и «волна» у носа, оранжево-красный нос
   (оранжевый низ, красный перед) с тёмным лобовым стеклом в серой рамке, полупантографы на 2-м и 4-м вагонах; «Паровоз» — чёрный Су/Л: красные колёса и рама, труба с раструбом, купола,
   фара и красная звезда на дымовой коробке, будка и тендер с углём; «ЧМЭ3» — маневровый тепловоз капотного типа: ярко-зелёный кузов, красная рама,
   красно-оранжевая буферная балка, жёлтые поручни вдоль рамы, жёлто-красная «тигровая» маска на лбу (жёлтый шеврон с красной полосой и звездой; на виде сверху вершина красного угла направлена к носу, звезда под ней),
   высокая угловатая кабина, отодвинутая от носа (между маской и кабиной — короткий капот-площадка, не вплотную к торцу) (лобовые и боковые стёкла, светлая крыша с люком), длинный капот ниже кабины с жалюзи, люком и вентилятором, красные колёса на изометрии.
   Выбор: быстро на карте (в Настройках пункта больше нет, убран 28.10.2026) — кнопка со значком в правом столбце
   (панель 3+3+2 — восемь плиток, ряды по 3, с изометрическими плитками 64 dp и подписью-названием модели); хранится локально (`SharedPreferences train_map_prefs`, ключ `position_icon`), на сервер не уходит.
   Панель закрывается нажатием мимо неё (на карту, любую кнопку — нажатие только закрывает панель) и системным «Назад» (`BackHandler` + прозрачная подложка на весь экран).
   **Режим «еду по карте»** (с 21.10.2026 — не отдельная кнопка, а состояние слежения кнопки «Моё местоположение»): включает
   следование, поворачивает карту по курсу (маркер всегда «вверх»). Ручной жест (перетаскивание, зум, поворот)
   выключает режим; север не возвращается сам — его возвращает кнопка-компас (ниже).
   **Поворот карты пальцами (`uiSettings.isRotateGesturesEnabled = true`, в том числе в просмотре трека из «Моих треков»):** жест двумя пальцами
   поворачивает карту; в обычном режиме поворот сохраняется (следование за положением берёт текущий `cameraPosition.bearing`, а не 0);
   ручной поворот, как pan/зум, выключает следование и «еду по карте». **Компас один:** системный компас MapLibre (вверху экрана, под названием маршрута) отключён (`uiSettings.isCompassEnabled = false`).
   Пока поворот ≠ 0 (по модулю 0,5°), в правом столбце первой
   (над «+») показывается кнопка-компас «Север сверху» (красная стрелка на север поворачивается против поворота карты): нажатие
   выключает «еду по карте» и плавно (400 мс) возвращает север. Подписи станций/остановок считаются `LabelPlacer` в экранных координатах
   и пересчитываются по `onCameraIdle`, текст остаётся горизонтальным; значок положения выровнен по карте (`ICON_ROTATION_ALIGNMENT_MAP`). **Лента над картой** (`EtaRibbon`, только при маршруте с линией и положением, не в просмотре истории, и **только когда положение на пути
следования**: при отклонении от рельсовой линии > 50 м, то есть при плашке «Вне путей», лента скрыта): «{следующая станция}
   X,X км · чч:мм» и «{конечная станция} X,X км · чч:мм» (подписей «Следующая»/«Конечная» нет, только названия; если следующая и есть конечная — одна строка); **лента кликабельна и открывает экран «Время прибытия»** (ниже); расстояния — вдоль рельсовой
   линии (`TrainMapRoute.progress`, обновляются с каждой точкой; < 1 км — в метрах с шагом 10), время прибытия = сейчас + расстояние / скорость,
   где скорость — живая, если ≥ 10 км/ч, иначе средняя по точкам трека со скоростью ≥ 10 км/ч за последние 10 мин, иначе «-» (`TrainMapRoute.etaSpeedKmh`,
   `Eta.arrivalMs`); время — пояс отображения (`displayTimeZone()`). Скорость в плашке — из живого положения (иначе точка записи/трека).
   **Текущая скорость** — плашка слева под заголовком: крупно целое число км/ч (mono, 34 sp) и «км/ч»; источник — живая точка
   `GpsRecorder` (не старше 15 с, пока карта открыта и «Автозапись» пишет), иначе скорость последней точки трека с подписью
   «последняя точка чч:мм:сс»; нет ни того ни другого — плашки нет.
   **Тап по пройденному пути (п. 5)** — **ближайшая сохранённая точка** (`TrackAnalysis.nearestOnTrack`; точки пишутся раз в 5 с, при крупном
   масштабе между ними сотни метров): если касание ближе **48 dp** к линии (сегмент между двумя соседними точками; через разрыв записи > 5 мин
   сегмента нет), берётся ближайшая к касанию из двух его точек; иначе — ближайшая точка в пределах 30 dp. Время и скорость — всегда с этой
   ближайшей точки, маркер ставится на неё (не на касание). На карте белый маркер с чёрной
   обводкой, карточка слева под плашкой: время «чч:мм:сс» (пояс `displayTimeZone()`, МСК или местное для KZ) и «N км/ч»
   («скорость неизвестна»). Тап мимо — выбор снимается. Если трека нет — всё как в этапе 2 (скорости и тапов нет).
   **Остановки (п. 6)** — оранжевые точки с белой обводкой: станции поезда (не проходные) с временами — положение по
   стоянке трека, совпавшей по времени (±60 с к окну прибытие–отправление), иначе по точке трека в момент прибытия (≤ 3 мин);
   плюс стоянки по треку без станции. **Единое определение остановки (решение 09.10.2026):** «Автозапись» (онлайн), остановки на карте и график
   используют ОДИН алгоритм и одни пороги (`StopDetectionConfig`): `TrackAnalysis.detectDwells` проигрывает записанные точки
   через тот же `StopDetector` — прибытие < 2 км/ч ≥ 60 с, отправление > 5 км/ч ≥ 30 с, точность > 50 м и пропажа данных сбрасывают окно,
   откат времени отправления к моменту трогания, округление вниз до минуты (времена стоянки кратны минуте — совпадают со списком станций
   поезда). Единственное отличие — разреженность записи: стоянка пишется точкой раз в 60 с, поэтому между «медленными» точками разрыв допускается до
   90 с (`StopDetectionConfig.RECORDED_SLOW_GAP_MS` = `TrackAnalysis.DATA_GAP_MS`); больший — «нет данных» (потеря сигнала), не стоянка.
   Чтобы оффлайн-результат совпадал с онлайн, `TrackThinner` пишет СРАЗУ точку при смене «стою/еду» (точное время остановки) и при трогании
   добавляет последнюю отброшенную «медленную» точку (момент покоя перед ростом скорости). Разрыв > 5 мин (другой сеанс) закрывает стоянку; стоянка,
   не закончившаяся к концу трека, длится до последней точки; отдельных эвристик «расширения границ» нет. Тест согласованности: один и тот же поток 1 Гц
   (с тоннелем) даёт те же прибытие/отправление онлайн и по прореженной записи.
   **Источник правды — станции поезда:** если у поезда в списке станций есть запись с этой стоянкой (время ±60 с), остановка на карте берётся ИЗ СТАНЦИИ
   (те же название, прибытие, отправление и стоянка до минуты); остановки, вычисленные по треку, показываются только без соответствующей станции
   (в т.ч. просмотр трека без поезда из «Моих треков»).
   **Радиус станции — 1 км везде** (`StopDetectionConfig.STATION_RADIUS_M`; было 500 м для Автозапись и 400 м для карты). **Выбор названия — одна функция**
   `StopStationNamer` над `ServicePhaseStationPicker.pickForStop` (GO, «Добавить станцию», Автозапись, карта, просмотр трека; §6.6 «Единое определение названия станции»): при выбранном плече кандидаты — станции плеча (полный список с
   промежуточными) и станции справочника на линии плеча (отклонение ≤ 100 м; без линии — от прямых между станциями плеча, ≤ 150 м), в радиусе 1 км;
   упорядочены вдоль линии плеча; берутся не ранее последней записанной станции поезда (повтор той же допускается — «уже на станции»), выбирается
   ближайшая по пути (при равенстве — впереди по порядку); если подходящих нет — ближайшая станция справочника в 1 км; без плеча — станция ПО РЕЛЬСАМ с сервера (`alongRail`, 1 км; кандидаты — станции справочника в 2 км, п.3 §6.6; при сбое/`off_rails` — ближайшая справочника по прямой в 1 км); иначе название пустое. Для стоянок по треку без станции название подставляет тот же выбор.
   Названия: название станции поезда; иначе (просмотр трека, станций поезда нет) результат этого выбора, иначе «Стоянка N мин»
   (по времени из трека). Тап по точке (до 24 dp; остановки приоритетнее точек пути) —
   карточка: название станции (или «Остановка»), «Прибытие чч:мм · Отправление чч:мм · стоянка Nм» (части без данных
   опускаются). Подписи названий — по тому же правилу «помещается» (выше). Остальное: (7) тап по точке маршрута впереди —
   планирование прибытия (ниже, «Состояние 7»). Если трека нет (нет согласия №1, «Автозапись» не писал) — карта как в этапе 2. Камера охватывает ещё и
   пройденный путь. Источники: `domain/.../track/TrackAnalysis.kt` (ближайшая точка, стоянки, остановки, прореживание, время),
   `LabelPlacer.kt`, `TrainFormViewModel.trainMapTrack()`.
**Состояние 7 - «Цель»: планирование прибытия (`ArrivalPlan` в domain, `TrainMapPlan.kt`, `TrainMapTarget.kt`).** Работает при маршруте плеча с
рельсовой линией (не в просмотре истории). **Кнопка «Цель»** - в верхнем ряду карты, прижата к правому краю экрана; всё остальное место ряда занимает блок станций (названия не обрезаются без необходимости) (с флажком-иконкой, той же высоты, что блок станций;
при заданной цели - акцентный фон); видна при маршруте плеча даже если блока станций нет. Открывает отдельный **экран «Цель»** (полноэкранное окно, крестик).
Тап на карте по станции плеча/точке рельсовой линии **впереди** (24/30 dp от касания, дальше положения больше чем на 100 м, `ArrivalPlan.ARRIVED_M`) открывает
тот же экран сразу на шаге 2 с этой точкой; тап по флажку цели снова показывает карточку расчёта на карте.
- **Шаг 1 «Выберите станцию впереди»:** список станций плеча впереди текущего положения (вдоль линии, `ArrivalPlan.stopsAhead`) с расстоянием; если положение не на линии (вне путей, нет GPS, нет линии), станции плеча известны всё равно — список показывает все станции плеча по порядку без расстояния (`ArrivalPlan.targetStops`, 29.10.2026)
  (формат ленты). Положение не определено / не на линии (дальше 50 м) - «Впереди нет станций плеча…». Тап по станции - шаг 2. Если у плеча нет промежуточных станций и оно не проверено на карте — над списком заметка «У плеча нет промежуточных станций, поэтому цель можно выбрать только на конечной» и кнопка «Определить» (тот же `LegDetermineRow`, что в блоке плеча: подбор станций по карте и сохранение плеча одной кнопкой). После подбора станции в списке цели появляются сразу, без повторного входа на экран (список станций карты пересчитывается при изменении самого плеча, а не только при смене плеча/числа плеч).
- **Шаг 2 «Время прибытия»:** выбранная станция (тап - вернуться к списку); поле «Время прибытия» (маска `TimeMask`: только цифры, двоеточие ставится само,
  «905» -> «9:05»; пояс `displayTimeZone()`; принимается «18:30», «9:05», «0905»); переключатель (`Switch`) **«На остановку»** - при включении поле «Время
  торможения до остановки, мин» (по умолчанию 3, 0-60); переключатель **«Показывать цель на экране Карты»** (по умолчанию включён); кнопка «Рассчитать».
  «На остановку» и время запоминаются (`SharedPreferences train_map_prefs`: `plan_on_stop`, `plan_stop_min`) для следующих расчётов.
  Неверный ввод - «Введите время в формате чч:мм, например 18:30.» (рамка 2 dp и подпись цвета `error`). Время не позже текущего - «Это время раньше
  текущего. Считать на следующие сутки?» с «Да, на следующие сутки» / «Изменить».
- **Экран цели (цель задана):** карточка расчёта, переключатель «Показывать цель на экране Карты» (применяется сразу), «Сменить цель» (шаг 1) и
  кнопка **«Готово»** (закрывает экран, цель остаётся), красная **«Отменить цель»** - **цель снимается только здесь** (флажок исчезает).
**Экран «Время прибытия»** (`TrainMapSpeedEta.kt`, открывается тапом по ленте станций): полноэкранное окно; список **всех станций плеча впереди** (`ArrivalPlan.stopsAhead`) с расстоянием вдоль линии (формат ленты), под названием - «Прибуду в чч:мм» (сейчас + расстояние / введённая скорость; для следующих суток - «(следующие сутки)»); внизу закреплённый блок «Если ехать со средней скоростью:» с полем скорости (км/ч, цифры, до 3, без ведущих нулей: «0» + «5» = «5»). Значение по умолчанию - фактическая живая скорость на момент открытия, округлённая; стоим или скорости нет - 0, тогда под каждой станцией «Укажите скорость для расчёта времени». Пересчёт сразу при вводе. Станций впереди нет - пояснение.
**Расчёт.** Расстояние d - по рельсовой линии от текущего положения. Время на расчёт T = плановое - сейчас (время на остановку из него не вычитается). «На остановку»: время торможения tb (мин) - равномерное замедление до остановки, начинающееся на скорости хода v: торможение занимает на tb/2 больше безостановочного проследования того же участка (если участок короче тормозного пути, тормозит весь остаток: прибавка d/v). Нужная v - наименьшая, при которой d/v + потеря на разгон + прибавка торможения <= T (подбор с шагом 0,1 км/ч; «Время прибытия уже прошло» - только когда плановый момент прошёл).
Скорость v0 - текущая (живая, если ≥ 10 км/ч, иначе средняя по трекам за 10 мин, `TrainMapRoute.etaSpeedKmh`, как у ленты). Если простая d/T не
больше v0, нужная скорость = d/T. Иначе учитывается **разгон**: поезд разгоняется от v0 до скорости хода v с ускорением 0,2 м/с²
(`ArrivalPlan.ACCEL_MS2`) и далее идёт с v; потеря времени (v-v0)²/(2·a·v), v - корень уравнения d/v + (v-v0)²/(2·a·v) = T. Если корня нет -
«Не успеть: с учётом разгона от текущей скорости времени не хватает.». T <= 0 - «Время прибытия уже прошло.». v0 неизвестна - расчёт без разгона и «Текущая скорость неизвестна.».
Карточка: название и «До неё X,X км»; «Плановое прибытие» (чч:мм, для следующих суток - «(следующие сутки)»); «Торможение до остановки: N мин» (если включено);
крупно **«Нужная скорость хода»** в «N км/ч»; «Текущая скорость N км/ч» (та же, что на мини-графике скорости: живая, иначе последняя точка трека); «При текущей скорости прибудете чч:мм» (при включённой остановке включает торможение: + min(N/2 мин, время хода до цели)); итог: «Опережаете» (текущая ≥ нужной x 1,15),
«Успеваете» (нужная ≤ текущая < нужной x 1,15), «Нужно быстрее на N км/ч» (красным; N - разница, округлена вверх). **Ограничения скорости:** расчёт (и «нужная скорость», и «прибудете») учитывает действующие ограничения из журнала поезда на выбранном участке профиля впереди по ходу: скорость на зоне не выше лимита, перед ней торможение (0,4 м/с²), после - разгон (0,2 м/с²); зона действует до прохода хвоста (конец + длина поезда). Под расчётом строка «Учтены ограничения скорости на пути: N». Учитываются ограничения всех загруженных участков плеча впереди: границы зоны с оси своего участка переводятся в координаты и проецируются на линию плеча (как станции цели; зона дальше 400 м от линии пропускается). Если положения на линии нет (вне путей), берутся только ограничения выбранного участка профиля. В этом случае (и если скоростные ограничения вообще есть) под расчётом красным текстом причина и предупреждение: «Ограничения учтены не полностью: {причина}. Учтены только ограничения выбранного участка профиля, расчёт может быть неточным.» (если и по оси участка зон не получить - «Ограничения скорости не учтены: {причина}. Расчёт может быть неточным.»). Причины: линия пути плеча не загружена / положение не определено / положение дальше 50 м от рельсового пути. Пересчёт - раз в ~5 с.
**На экране Карты** при включённом «Показывать цель на экране Карты» - тот же блок расчёта снизу (над навигационной панелью) и красный флажок цели на карте;
единственная кнопка блока - **«Скрыть»**: блок сворачивается в плашку «План прибытия: {станция · }чч:мм» внизу слева (тап по плашке или флажку раскрывает снова); цель
при этом остаётся. Выключенное «Показывать цель на экране Карты» убирает блок, плашку и флажок с карты (расчёт виден на экране «Цель»).
**Хранение плана:** локально, `SharedPreferences train_map_prefs`, ключ `arrival_plan_<id поезда>` (строка «широта TAB долгота TAB плановое
время мс TAB «на остановку» 0/1 TAB минуты на остановку TAB «показывать на карте» 0/1 TAB название»; прежний 4-польный формат читается), на сервер не уходит. План снимается автоматически, когда до точки осталось не больше 100 м или она пройдена, если точка
больше чем на 200 м не лежит на текущей линии (сменили плечо) и если плановое время прошло больше чем на 12 часов; также кнопкой «Отменить цель» на экране «Цель».
**Недоступно:** если текущее положение дальше 50 м от линии - пояснение «Расчёт прибытия недоступен: положение дальше 50 м от рельсового пути.
Вернитесь на путь маршрута.» (список станций пуст, у сохранённой цели вместо результата - то же пояснение); положение не определено -
«Расчёт прибытия недоступен: положение не определено.»; линия сбрасывается/перестраивается - «Линия пути ещё не загружена.». **Интернет:** линия
берётся из кэша (§6.6) - без сети с сохранённой линией расчёт работает; если линия не построена (ошибка), тап по станции плеча вызывает
`rememberNetworkGate().run { повторить построение }` - без сети диалог «Нет интернета» (тексты выше), с сетью - повторное построение линии.
Пока строится линия или после «путь не найден», тапы по произвольным точкам не интерпретируются (линии нет). Пока показана карточка плана или
плашка, жёлтая плашка «Маршрут показан из сохранённых данных» не отображается. Время вне зависимости от трека - `displayTimeZone()` настроек
(для трека - тот же пояс).
**График скорости (этап 4b, `SpeedChartScreen`, полноэкранный диалог поверх карты).** Открывается кнопкой «График скорости»
на карте; на телефоне под картой места нет, поэтому отдельный экран. Данные — тот же записанный трек поезда (`TrainMapTrack.points`,
из `Track.db`; обновляется вместе с картой), ряд строится в `SpeedChart.buildSeries`: время, накопленный путь по прямым между
соседними точками (через разрыв записи > 5 мин путь не растёт), скорость. **Показывается только в горизонтальной ориентации** (решение 09.10.2026): при открытии активность принудительно переводится в
`SCREEN_ORIENTATION_SENSOR_LANDSCAPE` (любой из двух альбомных), по выходу возвращается прежняя ориентация; чтобы это не пересоздавало Activity и не теряло
состояние карты/графика, у `MainActivity` в манифесте добавлено `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout"`.
Шапка: «Назад», «График скорости», **легенда цвета скорости** (маленькие цветные метки с порогами текущей шкалы, мини-подпись «км/ч»: «≤20 · 20–70 · 70–80 · >80»
или для пассажирского «≤20 · 20–120 · 120–160 · >160»; на узком экране — отдельной строкой под шапкой); переключатель оси X
**«Путь» (по умолчанию) / «Время»**; кнопки «−», «+», «Всё» (весь ряд) и (решение 09.10.2026) «В начало» / «В конец» (иконки «черта + шеврон», описание для доступности): показываются, только когда увеличено и соответствующий край ряда за пределами окна (`SpeedChart.canScrollToStart/End`, допуск 0,1 % ряда; весь ряд виден - обеих нет), нажатие плавно (350 мс) прокручивает окно к началу/концу с тем же охватом (`SpeedChart.edgeWindow`). На широком экране (≥ 600 dp, альбомная ориентация)
всё в одной строке шапки. График рисуется на Compose Canvas: ось Y 0…max (округление вверх до кратного 20, минимум 40) км/ч
с подписями, ось X — км (круглый шаг 1/2/5·10ⁿ) или время «чч:мм» (шаг 1…720 мин по часам пояса `displayTimeZone()`, метки кратны шагу).
Линия — отрезки цвета шкалы скорости карты (`SpeedScale`, общая с картой и легендой, шкала по категории поезда; сегмент без скорости или разрыв > 5 мин не рисуется) толщиной
3 dp на «обводке» 5 dp цвета текста темы с прозрачностью 35 % — контраст в светлой и тёмной теме. Для отрисовки ряд прореживается
по ширине графика (≤ 4 точки на пиксельный столбец: первая, минимум, максимум, последняя). **Зум и прокрутка по оси X:** щипок
(якорь — центр жеста), перетаскивание по горизонтали, «+»/«−» (×2/÷2 вокруг центра окна); минимальное окно 300 м или 2 мин, не шире
всего ряда; «Всё» сбрасывает окно. **Смена оси «Путь» ⇄ «Время» окно не сбрасывает и не двигает** (решение 09.10.2026): граничные точки окна (первая и последняя точки ряда внутри него) переносятся на новую ось (`SpeedChart.convertWindow`; на «Пути» стоянка схлопывается в точку, при возврате на «Время» граничные точки те же); полное окно остаётся полным; выбранная точка (курсор) определяется по времени и остаётся той же. **Остановки** (станции поезда с временами и стоянки по треку, §6.9 п. 6) — вертикальные
пунктирные линии с точкой в верхней полосе; на оси «Время» — ещё **голубая полоса стоянки** (прибытие–отправление, цвет акцента с прозрачностью 18 %; если запись началась на станции, полоса видна слева от x = 0 до отправления). В легенде на оси «Время», когда на экране есть такие полосы, добавлена метка «стоянка» (голубой прямоугольник). Серая штриховая полоса — «нет данных» (разрыв > 90 с), не стоянка. Названия станций — по правилу
«показывать, если помещается» (`LabelPlacer`, ширина подписи ≤ 110 dp с «…», стоянки без названия — только точка). **Курсор:** касание
графика ставит вертикальную линию в ближайшей по оси точке, внизу карточка «чч:мм:сс · N км/ч · X,X км от начала». **Курсор можно тянуть**
(решение 09.10.2026): касание в пределах 48 dp от курсора и движение пальца вдоль графика — курсор следует за пальцем и перескакивает к соседним точкам
без отпускания, карточка со временем, скоростью и километром обновляется на лету; касание в другом месте — тап выбирает точку, сдвиг пальца листает окно
(панорама), два пальца — масштаб и сдвиг (один обработчик жестов `SpeedPlot`). Выбор общий с картой:
касание графика выбирает ту же точку, что тап по пройденному пути на карте (при возврате маркер подсвечен, камера смещается к точке с
сохранением зума), а точка, выбранная на карте, показывается курсором при открытии графика. Без выбора карточка показывает сводку
(путь, длительность, макс. скорость). **Пустое состояние** (трека нет, < 2 точек или ни у одной точки нет скорости) показывает **реальную причину** (`TrackEmptyReasons.determine`,
domain; состояние записи опрашивается раз в 3 с, пока экран пуст: `TrainFormViewModel.trainMapRecordingState` → `AutoRecordManager.trackRecordingState`).
Порядок проверок: (1) ≥ 2 точек, но ни одной со скоростью — «Нет данных о скорости от GPS»; (2) согласие №1 на этом устройстве выключено
(SharedPreferences `track_privacy`, после переустановки сбрасывается) — «Для записи скорости дайте согласие на сохранение трека» + «Открыть согласие»
(закрывает карту, Профиль → «Данные и конфиденциальность», `Router.showDataPrivacy`); (3) «Автозапись» выключено — по центру «Для записи скорости включите режим Автозапись» + кнопка «Включить Автозапись» (та же логика, что у чипа «Автозапись» в «Поезд»:
разрешения, согласие, целью записи становится этот поезд; `onEnableAuto` из `FormTrainScreen`; в просмотре трека из «Моих треков» кнопки нет); (4) цель записи (`target_train_id`) не определена (включали с главного/виджета без поездов; цель фиксируется только по первому
событию стоянки/отправления) — «Автозапись включено, но поезд для записи не выбран» + «Записывать этот поезд»; (5) цель — другой поезд (трек читается по поезду
экрана, `tracksForTrain`) — «Автозапись записывает другой поезд (№…)» + «Записывать этот поезд» (`AutoRecordManager.setTarget`, без перезапуска сеанса, следующая
точка откроет новый трек); (6) иначе — «Запись идёт. Для построения графика скорости нужно начать движение» (прореживание: движение — раз в 5 с, стоянка — раз в 60 с).
   **Открытый с живой карты полноэкранный график (с 22.10.2026)** не гадает по `TrackRecordingState`, а берёт состояние карты: согласия нет — «Для записи скорости нужно ваше согласие на обработку геоданных» + кнопка «Настройки согласия»; согласие есть, запись не идёт — «Для сохранения трека нажмите «Начать запись»» + кнопка «Начать запись» (запускает запись и закрывает график); запись идёт, точек нет — «Запись идёт. Для построения графика скорости нужно начать движение» (без кнопки). Про Автозапись и согласие при выданном согласии не напоминаем.
Состояние неизвестно (просмотр истории) — прежний общий текст и «Настройки согласия». Внизу пустого экрана мелкий отладочный текст
«согласие: вкл/выкл · Автозапись: вкл/выкл · цель: этот/другой/нет · точек: N» (без координат).
Ориентация: само приложение заблокировано в портрете (манифест), экран графика — принудительно альбомный (см. выше); тексты и кнопки пустого
состояния — `TrackEmptyReasons.message/action/actionLabel` (domain, покрыто тестами). Источники: `domain/.../track/SpeedChart.kt`
(ряды, окно/зум, прореживание, метки осей, `SpeedScale`), `features/route/.../servicephase/SpeedChartScreen.kt`.
Подробный план по этапам, открытые решения и идеи — `docs/TRAIN_MAP_PLAN.md`.

**Режим просмотра (`viewOnly`, «Мои треки», §17.7):** параметры `title` (заголовок плашки) и `viewOnly` — запись, «Моё местоположение» и живая скорость скрыты; кнопка «График скорости» доступна, если у трека есть точки; выбранная точка общая для карты и графика.

**Хранение трека «Автозапись» (этап 3, только Android).** Точки пишет `AutoRecordManager` из потока `GpsRecorder.points` ТОЛЬКО пока
включена «Автозапись» И дано согласие №1 (ниже). Трек привязан к поезду, куда нажата «Автозапись» (`target_train_id` менеджера); один
сеанс «Автозапись» на одном поезде = один трек (`trackId` — UUID). **Один поезд - один трек, перезапуск продолжает**
(решение 10.10.2026): при начале записи (первая точка после старта процесса/службы, переармирования «Автозапись», стоп/старт «GO»,
смены цели туда-обратно) `TrackWriter` ищет в `Track.db` последний трек этого же поезда; если он закончился не более 12 ч
назад (страховка `TrackWriter.RESUME_GAP_MS`, чтобы трек не тянулся через отдых) - точки пишутся в него (тот же `trackId`, прореживание считается от его последней записанной
точки), иначе открывается новый трек. Состояние берётся из `Track.db`, не из памяти - убийство процесса не рвёт трек. Другой
поезд или пауза дольше 12 ч - новый трек. **Одноразовая склейка** (флаг `tracks_merged_v1` в prefs менеджера, при старте
приложения): смежные треки одного поезда с разрывом ≤ 12 ч сливаются в самый ранний (`TrackMerger`, транзакция
`mergeTracks`): точки переносятся, `startedAt`/`endedAt` пересчитываются, звезда - если была у любого (помечается к отправке),
оставшийся трек получает `uploadedPoints = 0` (будет отправлен заново), поглощённый попадает в `TrackPendingDelete` (удалится на
сервере); пишущиеся прямо сейчас (закончились < 5 мин назад) не трогаются. Пока целевой поезд не определён, точки не пишутся. Хранилище — **отдельный файл БД `Track.db`** (SQLDelight
`TrackDatabase`, `data_local`; существующие `Route.db`/`Settings.db` не затронуты). **Схема версии 2** (этап 3б): у уже созданных
`Track.db` миграция `1.sqm` добавляет три колонки `Track` и таблицу `TrackPendingDelete` (записи и точки не затрагиваются),
свежая установка создаёт сразу последнюю версию (3, см. высоты ниже). Таблицы:
`Track(trackId PK, trainId, startedAt, endedAt, isFavorite 0/1, uploadState 0/1/2, uploadedPoints, favoriteDirty 0/1)`
(индексы по `trainId` и `endedAt`; `uploadState`: 0 — ждёт отправки, 1 — отправлен, 2 — сервер отклонил окончательно, напр.
квота; `uploadedPoints` — сколько точек было в отправленной версии; `favoriteDirty` — звезда изменена и не доставлена);
`TrackPendingDelete(trackId PK)` — очередь удалений, которые надо доставить на сервер;
`TrackPoint(trackId, timeMs, latitude, longitude, speedKmh?, accuracyM?, altitudeM?, baroAltitudeM?, PK(trackId, timeMs))` — первичный ключ
служит индексом «трек + время». **Высоты (этап 3в, часть 1, схема версии 3, миграция `2.sqm` добавляет `baroAltitudeM`)** пишутся ЛОКАЛЬНО: `altitudeM` —
высота GPS (`Location.mslAltitudeMeters` на API 34+, иначе `altitude`, только при `hasAltitude`), `baroAltitudeM` — по барометру (`TYPE_PRESSURE` →
`SensorManager.getAltitude` по стандартной атмосфере, только если датчик есть, иначе `NULL`); значения вне -500…9000 м и NaN отбрасываются
(`AltitudeMath`). В составе ТРЕКОВ (`/v1/tracks`) высоты на сервер НЕ уходят (`TrackWireCodec.encodeChunk` по умолчанию их обнуляет).
Высоты уходят только отдельным обезличенным вкладом по согласию №2 (см. «Вклад высот» ниже).
Время — мс UTC. **Прореживание при записи** (`TrackThinner`): первая точка — всегда; затем в движении (скорость ≥ 2 км/ч или
неизвестна) — не чаще раза в 5 с, на стоянке (< 2 км/ч) — не чаще раза в 60 с; при тронулении после стоянки запись возвращается
к 5 с сразу. **Срок хранения:** 180 дней от окончания трека (`endedAt` = время последней точки); избранный трек (`isFavorite`)
не удаляется. Очистка — фоновая, при старте приложения (`AutoRecordManager.restore()` → `purgeExpired`). **Удаление поезда**
(`TrainUseCase.removeTrain`) удаляет и его треки (удаление на сервере ставится в `TrackPendingDelete`). **Удаление рейса**
(`RouteUseCase.markAsRemoved` — в корзину, в том числе групповое и из синхронизации; `removeRoute`; `purgeRoute`) удаляет треки всех
его поездов сразу, не дожидаясь очистки корзины; восстановление из корзины треки НЕ возвращает. Поезд, чьи треки удалены,
помечается в `TrackRepository` (в памяти процесса): запись «Автозапись» с ещё запомненной целью не пишет в него новые точки
(раньше `TrackWriter` заново создавал только что удалённый трек с тем же id — он оставался в «Мои треки» как сирота и
не открывался); метка снимается при восстановлении рейса из корзины (`allowTrain`). **Диалоги удаления:** подтверждения удаления поезда (форма маршрута, §6.2) и маршрута («Все маршруты», Главный, Календарь, форма маршрута, групповое удаление, «Очистить корзину») дополняются строкой «Будут удалены и записанные треки.» — только если по удаляемым поездам есть записанные треки (проверка `TrackRepository.tracksForTrain` по `Track.db`), иначе строка не показывается. Отзыв согласия треки не удаляет
(остановка записи и отправки новых; удалить — вручную в «Мои треки»). **Осиротевшие треки** (поезда нет ни в одном рейсе — рейс удалён целиком, серверный full-replace убрал поезд,
рейс удалён на другом устройстве) удаляет `TrackOrphanCleaner` (с постановкой серверного удаления): при каждом прогоне
отправки и при открытии «Мои треки»; при прогоне отправки трек моложе суток не трогается (поезд мог быть ещё не сохранён), при
открытии «Мои треки» — моложе 10 минут (явное удаление уже удалило треки, защищается только пишущийся прямо сейчас);
пустой/нечитаемый список рейсов — ничего не удаляется; рейсы в корзине считаются существующими. История и избранное — «Мои треки» (§17.8).

**Отправка треков на сервер (этап 3б; `/v1/tracks`, дизайн — `docs/TRAIN_TRACKS_SERVER_DESIGN.md`).** Только при согласии №1 и
входе в аккаунт; сервер проверяет флаг согласия на каждой записи (403 `consent_required`). Состояние согласия, подтверждённое
сервером (`server_consent` в `track_privacy`: нет значения — не сверялись), расходится с локальным — первым запросом
`PUT /v1/tracks/consent {storageGranted, policyVersion}` (при отзыве серверные треки сохраняются, метки «отправлено» не сбрасываются;
новые треки не отправляются). Воркер `TrackUploadWorker` (WorkManager, уникальная работа `track_upload`, только при сети, экспоненциальная пауза,
до 8 попыток) запускается: по окончании сеанса «Автозапись», при старте приложения, при смене согласия, при открытии «Мои треки».
Порядок прогона `TrackUploader`: согласие → очередь удалений (`DELETE /tracks/{id}`, идемпотентно; доставляется и без согласия) →
треки, в которых не было новых точек дольше 5 мин (пока трек пишется — прогон повторяется через ~5,5 мин) → доставка изменённой
звезды (`PATCH {isFavorite}`). Трек отправляется целиком и детерминированно: `PUT /tracks/{id}` (мета: `routeId` = id рейса поезда,
`title` «№ поезда · Откуда — Куда», `startedAt/endedAt`, длина по ломаной без разрывов > 5 мин, макс. и средняя скорость
(≤ 500 км/ч), `pointsCount`, `chunksExpected`, `preview` ≤ 100 точек) → чанки `PUT /tracks/{id}/chunks/{n}` по ≤ 2000 точек →
`POST /tracks/{id}/complete {chunks}`; пропущенные чанки (`missing`) досылаются (до 3 кругов). Повтор безопасен: тот же чанк
даёт `duplicate`. Формат чанка (конверт `ld-track-v1`, версия содержимого 2: скорость и точность могут быть неизвестны — маска присутствия, null не превращается в 0; на карте неизвестная скорость серая) — `domain/.../track/TrackWireCodec.kt`, зеркало серверного `track_codec.py`
(колонки дельтами + varint/zigzag, затем zlib level 6; шифрование — серверное; JSON-обёртка `{enc, data(base64), sha256}`);
скорость/точность неизвестны → 0, высота пока всегда `null`. Рейс ещё не синхронизирован (409 `route_not_synced`) — трек ждёт,
рейс удалён на сервере (410) — трек удаляется, квота (413 `quota_exceeded`) — трек помечен «отклонён», прогон останавливается,
403 — согласие переотправляется в следующем прогоне, 401 — прогон прекращается. Трек, выросший после отправки (сеанс продолжился),
удаляется на сервере и отправляется заново. iOS: zlib/SHA-256 — заглушка (`IosTrackBlobTools`), записи треков на iOS нет.

**Согласие №1 «Сохранять мои треки и историю поездок».** По умолчанию ВЫКЛ (предустановленных «галочек» нет). Состояние,
время согласия, признак «решение принято» и флаг однократного предупреждения — локально, `SharedPreferences` `track_privacy`
(`TrackConsentStore`); само согласие (без времени) дублируется на сервер `PUT /v1/tracks/consent` — см. «Отправка треков» ниже. Экран согласия (полноэкранный диалог, `TrackConsentDialog`) открывается при первом
включении «Автозапись» кнопкой из приложения (экран «Поезд», главный экран), пока решение не принято; включение «Автозапись» из виджета и
через уведомление экран не показывает (там действует предупреждение ниже). Тексты (дословно):
- заголовок: «Сохранять мои треки и историю поездок»
- подпись переключателя: «Сохранять треки» (заголовок экрана/блока остаётся «Сохранять мои треки и историю поездок»)
- текст: «Когда включен режим «Автозапись», приложение записывает ваш путь: время, положение и скорость в точках следования. Это нужно, чтобы вы могли открыть пройденный путь на карте, посмотреть скорость и историю поездок.»
- пункты (отдельные абзацы с маркером «•»; ровно два):
  - «Треки хранятся на вашем устройстве и в зашифрованном виде на сервере LocoDriver. Доступ к ним только у вас, администраторы приложения их не видят.»
  - «Срок хранения — 180 дней. Избранные треки, отмеченные звездой в разделе «Мои треки», не удаляются автоматически.»
- примечание: «Без согласия режим «Автозапись» продолжит записывать стоянки и отправления поезда, но путь не сохранится.»
- кнопки: «Продолжить» (запоминает выбор переключателя и включает «Автозапись» дальше по обычному сценарию: разрешения → подсказка про
  батарею) и «Отмена» (включение отменяется, решение не принято — экран появится снова).
Без согласия «Автозапись» работает как раньше (стоянки/отправления пишутся), трек не пишется, и при включении «Автозапись» один раз
(пока согласие не дано; после отзыва — снова один раз) приходит уведомление «Трек не будет сохранён: нет вашего согласия».
**Согласие №2 «Передавать обезличенные данные для карт: высоты и места потери GPS».** Отдельный переключатель ПОД первым на том же экране согласия
(Настройки → Конфиденциальность), по умолчанию ВЫКЛ; не зависит от №1 (можно включить только №2). На экране первого включения «Автозапись» оба согласия выставляются одной кнопкой «Согласиться и включить».
Пояснение под ним (мелким шрифтом, без слов «трек» и «координаты»): «Передаются километр пути и высота, а также места, где пропадает GPS (участки карты около 250 м) — без вашего имени и без точного времени. Помогает строить профиль пути для режимных карт и общую карту мест без GPS.»
Редакция текста (`TrackConsentStore.ALTITUDE_TEXT_VERSION = 2`, ключ `altitude_contrib_text_v`): включение пишет версию 2; согласие, данное раньше (версии нет = 1, только высоты), места потери GPS НЕ отправляет — в «Данные и конфиденциальность» под переключателем появляется пояснение «Вы согласились на передачу только высот. Места потери GPS не передаются, пока вы не подтвердите новые условия.» и кнопка «Подтвердить новые условия» (`confirmGnssText`). Высоты у таких пользователей идут как раньше.
«Продолжить» применяет оба выбора (`setConsent`, `setAltitudeContrib`). Состояние и время — `SharedPreferences` `track_privacy`
(`altitude_contrib`, `altitude_contrib_at_ms`, `TrackConsentStore.altitudeContrib`); сервер флаг не хранит. Отзыв только останавливает дальнейшую
передачу и удаляет накопленные неотправленные точки; отправленное вернуть нельзя (в диалоге отзыва это сказано одной фразой).

**Вклад высот (согласие №2; `AltitudeContribution.kt`, `AltitudeContributor`, `AltitudeContribWorker`).**
- Сбор: пока «Автозапись» включён и согласие №2 дано, каждая точка GPS проходит фильтр качества (точность ≤ 20 м, скорость известна, есть правдоподобная GPS- или барометрическая высота), прореживается как трек (раз в ~5 с, `AltitudeSessionBuffer`, потолок 10 000) и копится В ПАМЯТИ. Не зависит от согласия №1 и не пишет в Track.db. По окончании сеанса буфер сбрасывается во временный файл `filesDir/altitude_contrib/<sessionId>.pts` (только lat, lon, GPS-высота, барометр, точность — без времени) и ставится `AltitudeContribWorker` (WorkManager, только при сети, backoff 1 мин, до 8 попыток). Процесс убит посреди сеанса — буфер теряется. Файл старше 7 суток удаляется; удаление файла = «вклад по сеансу отправлен».
- Привязка: оси опубликованных участков берутся из `GET /v1/advisor/sections` (хэш `axisHash`) и `/sections/{id}` (геометрия `[km, lat, lon]`), кэшируются в `cacheDir/advisor_axes` и перекачиваются только при смене хэша. Точка привязывается к ближайшему отрезку оси с интерполяцией по отрезку (`AxisLocator`, плоская метрика 110540 / 111320·cosφ): км оси (м) и боковое отклонение `offsetM`. Точки буфера идут по времени, поэтому поиск идёт в окне ±250 вершин вокруг прошлой привязки (`AltitudeContribBuilder.WINDOWED_PROJECTION`), при d > 200 м — полный перебор (параллельный путь не перехватывает привязку); точки дальше 30 м от оси пропускаются (могут попасть в несколько участков). Живая привязка потока фиксов (`AxisTracker`: пороги точности 80/150 м, фильтр скачков, «вне маршрута», гистерезис на стоянке) и нумерация/пикеты (`RailKmMap`, `KmCalibration`, `Picket`) готовы в `domain/track/KmMapping.kt`, но в UI пока не подключены (docs/KM_MAPPING_PLAN.md).
- Ячейки: шаг 25 м, ячейка `i` = позиция `i*25` м по оси (ближайший узел), `kmFromM` — начало первой ячейки. Источник одного фрагмента: барометр, если он есть у ≥ 50% точек на оси, иначе GPS (`source` `baro`|`gnss`). Значение ячейки — медиана, затем медиана по соседям ±2 ячейки; высота в дм (−500…9000 м); `offsetM` — среднее |отклонение| по точкам ячейки, `null` у пустых. Фрагмент рвётся на разрывах > 120 ячеек (3 км) и при > 4000 ячеек; короче 5 ячеек с высотой не отправляется.
- Тело `POST /v1/advisor/altitude-contrib` (JWT только как защита от спама): `contribId` (детерминированный UUID от сеанса/участка/оси/начала — повтор даёт `duplicate`), `sectionId`, `axisHash`, `kmFromM`, `stepM`=25, `source`, `meanAccM`, `data` = base64(zlib(токены: 0 — нет данных, иначе 1+zigzag(дельта в дм от предыдущей известной))), `offsetM`. Нет user_id, времени, координат.
- Ошибки: 409 `axis_changed` — участок перекачивается, вклад пересобирается под новый хэш (если хэш тот же — пропуск); 404 — участок пропущен; 422/прочие 4xx — фрагмент пропущен; 401 — сеансы ждут входа; 429/5xx/сеть — повтор позже. Согласие №2 отозвано — файлы удаляются, отправка отменяется.
Источник: `features/route/.../component/servicephase/TrainMapScreen.kt`, `ui/FormTrainScreen.kt` (`TrainBottomAppBar`),
`viewmodel/TrainFormViewModel.kt` (`trainMapMissingStation`, `saveStationCoordinates`),
`domain/.../util/StationDirectory.kt`, `use_cases/StationDirectoryUseCase.kt` (`fillCoordinates`),
`domain/.../util/TrainMapRoute.kt` (станции маршрута, «впереди», точки охвата камеры), `ServicePhaseAutoFillViewModel.railLine`.

#### Качество сигнала GPS (индикация и учёт потери сигнала)

Источник для записи/детектора остановок — `GpsRecorder.quality` (`StateFlow<GnssQuality>`: `level` GOOD/WEAK/LOST/UNKNOWN/INTERFERENCE, `satellitesUsed`,
`satellitesVisible`, `accuracyM`, `lastFixAgeMs`; обновляется раз в секунду, пока работает служба записи): `GnssStatus.Callback` (число спутников в
решении/в поле зрения) + возраст и точность последнего fix. **Для значка на карте поезда — собственный `GnssQualityMonitor`** (решение 09.10.2026):
`GpsRecorder.quality` обновляется только при работающей службе записи, а положение на карте приходит из `DeviceLocation.updates` (GPS **и** сеть) —
при выключенном «Автозапись» fix в службе не было и значок показывал красный LOST при верно определённом месте. Монитор живёт, пока карта открыта (и есть
разрешение), подписан на `GnssStatus` и получает КАЖДЫЙ fix, который показывает маркер на карте (из `DeviceLocation.updates` и точек записи);
возраст fix — по `SystemClock.elapsedRealtime()` в момент получения (не по часам GPS/телефона). Пока fix не было ни разу (запись выключена, положение ещё не
пришло) — UNKNOWN («нет данных», серый), а не LOST; красный — только когда ранее приходившие fix пропали > 15 с (или определение местоположения выключено в системе).
Сетевой fix без спутников (≥ 4 в решении нет) даёт WEAK (жёлтый), не красный. Правила и пороги — чистый код `domain/gnss/GnssQuality.kt`
(`GnssQualityConfig`, `GnssQualityRules`, `GnssQualityEvaluator`):
- **GOOD** — fix не старше 5 с, точность ≤ 20 м, ≥ 4 спутников в решении (если статус спутников недоступен — критерий не применяется);
- **WEAK** — иначе при fix не старше 15 с (точность 20 м и хуже, 3 и меньше спутников, fix 5–15 с);
- **LOST** — fix старше 15 с или GPS-провайдер выключен; **UNKNOWN** — первые 15 с после старта записи, пока не было fix (запись: после 15 без fix —
  LOST; монитор на карте: без fix остаётся UNKNOWN, `GnssQualityEvaluator.update(lostWithoutFix = false)`).
- **INTERFERENCE («Помеха GPS»)** — отвергнуто ≥ 2 фиксов как подмена/скачок за последние 60 с (`GnssQualityEvaluator.update(rejectedRecent)`): приоритетнее остальных уровней, без задержки; выход — как улучшение (5 с). Значок: 3 штриха + подпись «Помеха GPS» оранжевым (светлая `#C2410C` / тёмная `#FB923C`); подсказка «Помеха GPS — координаты скачут или подменены, такие точки не учитываются». Не вызывает уведомлений LOST.
- **Гистерезис:** LOST наступает сразу; ухудшение GOOD→WEAK подтверждается 3 с стабильного худшего значения, улучшение — 5 с.
- **Значок** (`GnssSignalIndicator`, 44×52 dp, иконка 22 dp — три штриха растущей высоты, **под иконкой подпись «GPS»** 10 sp цвета `textMuted`): GOOD — 3 залитых штриха зелёный
  (светлая `#15803D` / тёмная `#4ADE80`), WEAK — 2 штриха жёлтый (`#B45309` / `#FBBF24`), LOST — перечёркнут красный (`#B91C1C` / `#F87171`),
  UNKNOWN — серый (`#6B7280` / `#9CA3AF`); контраст ≥ 3:1 к фону в обеих темах; `contentDescription` — «Сигнал GPS. » + текст подсказки.
  **Показывается ТОЛЬКО на карте поезда** (решение 09.10.2026; из раздела «Поезд» и с главного экрана убран — `AutoGnssIndicator` оставлен
  пустой функцией): отдельная плашка над плашкой скорости, видна всегда, пока карта открыта (не только при записи); в просмотре записанного трека
  из «Моих треков» (без геолокации) — нет.
- **Тап** — подсказка на 4 с: «GPS: хороший сигнал, N спутников, точность ±N м» / «GPS: слабый сигнал, …» / «Нет сигнала GPS — записи стоянок и
  отправлений приостановлены» / «GPS: поиск спутников».
- **LOST дольше 30 с** — одно уведомление «Потерян сигнал GPS. Записи стоянок и отправлений приостановлены»; при возвращении сигнала
  (GOOD/WEAK) — «Сигнал GPS восстановлен» (по одному на эпизод потери).
- **Учёт потери:** детектор не принимает потерю сигнала за стоянку и не теряет отправление (§6.2 п.6а); «точка стоянки» не пишется
  без сигнала; стоянка по треку не склеивается через разрыв записи > 90 с; такие участки — «нет данных»: на карте серый пунктир, на графике
  скорости штриховая полоса с разрывом линии.

#### Подмена и скачки GPS (фильтр правдоподобия, решение 10.10.2026)

Полевой баг (Кириши, на станции глушат GNSS): положение прыгало между городами, скорость ~1200 км/ч, на карте ставились старт/финиш от ложных точек.
Чистый класс `domain/util/GnssPlausibility.kt` сравнивает каждый новый GNSS-фикс с предыдущим ПРИНЯТЫМ. Отвергается («подмена/скачок»), если:
заявленная скорость > 200 км/ч; скорость перемещения (расстояние / Δt, Δt ≥ 1 с) > 250 км/ч; прыжок > 1 км при Δt < 30 с; либо фикс далее
max(5 км, 3 × точность сети) от свежей (≤ 2 мин) сетевой позиции. Отвергнутый фикс НЕ попадает никуда: ни в трек (`GpsRecorder.points` его не получает), ни в
путь/livePoints на карте, ни в `StopDetector`, ни в скорость/ETA, ни в «текущую позицию» (маркер остаётся на последней правдоподобной), ни в маркеры
старта/финиша; он только увеличивает счётчик для состояния «Помеха GPS». Выход из тупика после реального телепорта/долгой потери: ≥ 5 согласованных между собой
фиксов в новой области (скорость между ними правдоподобна) на протяжении ≥ 60 с — область принимается (`Reason.NEW_AREA`); сетевая позиция,
опровергающая область, это принятие блокирует. Потеря сигнала на 10 мин и появление на 12 км дальше (≈ 70 км/ч) — норма, принимается.
Места применения: `GpsRecordingService` (единый вход записи: сначала `DriftFilter`, затем `GnssPlausibility`; TrackWriter/детектор получают уже чистые точки), `DeviceLocation.updates` (карта вне записи; сетевой fix
показывается только если GPS-fix ещё не принят либо GPS молчит ≥ 10 с и сеть не противоречит последней принятой позиции), `GnssQualityMonitor.onRejected`.
Уже записанные треки при открытии на карте очищаются `GnssPlausibility.sanitize` (`trainMapTrack`, `mapTrack`), поэтому старт/финиш не ставятся от подменных точек.
**Сетевая сверка:** `core_android/gps/NetworkLocationReference` — общий (счётчик подписчиков) редкий запрос `NETWORK_PROVIDER` раз в 45 с, `minDistance` 0, только пока
идёт запись GPS или открыта «живая» карта; новых разрешений нет. Сетевая позиция — только ориентир для проверки, в трек/на диск/в лог НЕ пишется. Нет сети / провайдер
выключен / нет разрешения — прежняя логика (скорость и прыжок). Авиарежим без Wi-Fi: сетевых позиций нет, сверка отключена; при включённом Wi-Fi работает по Wi-Fi.
Расход батареи: один пассивный-по-смыслу сетевой запрос раз в 45 с на фоне непрерывного GPS 1 Гц — пренебрежимо мал.
**Восстановление после выезда из зоны глушения (11.10.2026).** Сеть блокирует фикс, только если она свежая (≤ 2 мин; возраст считается по метке самого сетевого фикса, а не по моменту доставки), точность лучше 5 км И сеть подтверждает последнюю принятую область (иначе она ни при чём). Если залипшая сеть всё ещё показывает старую область, согласованная серия ≥ 5 фиксов за ≥ 120 с с реальным перемещением ≥ 500 м принимается вопреки ней (`NEW_AREA`); без сети новая область принимается через ≥ 5 фиксов за ≥ 60 с. Нет принятых фиксов > 5 мин - якорь «последняя принятая» снимается (следующий фикс = `FIRST`; `DeviceLocation` в этом случае не фильтрует сетевой fix по устаревшей позиции, карта не «залипает»). Фикс со временем дальше 24 ч от часов телефона (спуфинг времени) отвергается (`GnssPlausibility.isTimeSane`) в `GpsRecordingService` и `DeviceLocation`.
**Ложные метки времени в сохранённых треках (11.10.2026).** Полевой баг: «в пути 9522 ч» у склеенного трека - точка со спуфинговой меткой времени (±недели) попадала в `TrackPoint` и раздувала `startedAt`/`endedAt` и длительность в «Мои треки». Теперь `GnssPlausibility.sanitize` сначала отбрасывает точки дальше 72 ч от медианы времени трека (`dropTimeOutliers`); карточка «Мои треки» (`TrackHistoryViewModel.build`: время/длина/превью) и сводка отправки (`TrackUploader.prepare`) считаются по очищенным точкам. Одноразовая починка (флаг `tracks_fixed_v2` в prefs `AutoRecordManager`, при старте до склейки): `TrackRepair.fixBounds` удаляет такие точки и пересчитывает `startedAt`/`endedAt` как min/max времени оставшихся (`TrackRepository.trimTrack`, `uploadedPoints = 0` - трек уйдёт заново).
Тесты: `domain/src/test/.../util/GnssPlausibilityTest.kt`, `.../track/TrackRepairTest.kt`.
**Учёт точности (12.10.2026).** Прыжок считается за вычетом заявленных погрешностей двух фиксов (каждая — не более 300 м): смещение в пределах суммы
точностей - шум, а не прыжок; иначе один фикс с точностью 200 м становился «якорем», следующие хорошие фиксы отвергались как прыжок, и на треке
появлялась дыра до минуты ожидания «новой области» (так выглядели пропуски при плохом сигнале на ходу). Огромная заявленная точность спрятать подмену не может.
**Дыры на ходу при отличной точности (12.10.2026).** Полевой баг: трек от 11.10, дыра ровно 121 с при скорости 41-50 км/ч и точности 2-3 м (также 149/184 с 10.10). Причина:
`NETWORK_MISMATCH` - сетевая позиция в едущем поезде (Wi-Fi/вышки состава, залипший кэш) уезжает от GNSS дальше 5 км, фиксы отвергались до обхода по правилу «≥ 5 фиксов за ≥ 120 с»
(дыра = 120 с + 1 с). Теперь **непрерывная цепочка сетью не блокируется**: фикс не позже 30 с (`CONTINUITY_MAX_GAP_MS`) после предыдущего принятого и без прыжка принимается
(`Result.networkIgnored = true`, `Reason.OK`); сеть по-прежнему проверяет старт цепочки (первый фикс/после 5 мин молчания) и любой прыжок.
**Отображение дыр на карте (12.10.2026).** Пустоты в цветной линии на ходу (фото 11.10, перегон Кёрстово-Алексеевка) - это дыры данных > 90 с (слой «нет данных» был тонким 2.2 и терялся под синим пунктиром плеча). Теперь линия «нет данных» толще (4) и темнее (#546E7A), рисуется между соседними точками одного трека при паузе 90 с - 5 мин, если смещение согласуется со скоростью (≤ 200 км/ч); пауза > 5 мин по-прежнему разрыв.
**Запись при выключенном экране.** На время записи служба держит `PARTIAL_WAKE_LOCK` (`locodriver:gps-recording`, предохранитель 16 ч; снимается при остановке записи): без него Samsung усыплял процесс и фиксы приходили с дырами 2-10 мин (диагностика: доставлено = принято, отказов нет).

**Диагностика дыр (без координат).** `domain/gnss/GpsFixDiagnostics` в `GpsRecordingService` ведёт счётчики и сохраняет их в prefs `gps_recorder` (раз в 15 с, при дыре и при остановке; читается через `run-as`):
`diag_delivered` (фиксов доставлено системой), `diag_accepted`, `diag_rejected` (`TIME`/`SPEED_CLAIM`/`JUMP`/`NETWORK_MISMATCH` : число), `diag_net_ignored` (принято вопреки сети),
`diag_max_gap_s`, `diag_last_delivery_wall_ms` (возраст последнего фикса = сейчас − значение), `diag_last_accepted_wall_ms`, `diag_gaps` (до 20 строк `t=<мс>;dur=<с>;deliv=<сколько доставлено за дыру>;acc=<G<10м/M<30м/B/N>;rej=<причины>`;
дыра = пауза ≥ 20 с между принятыми). `deliv=0` - система молчала (Doze/ограничения батареи), `deliv>0` и `rej` - фиксы были, но отвергнуты. В prefs `AutoRecordManager`: `diag_signal_drops` - «точек стоянки», отброшенных из-за потери сигнала.

#### Дрейф GNSS на стоянке (DriftFilter, решение 12.10.2026)

Полевой баг (Самсунг, трек от 11.10): на стоянке скорость 0 и точность 2-7 м, но координаты уходят на 20-90 м за минуту, один скачок ~280 м.
Чистый класс `domain/util/DriftFilter.kt` (тесты `DriftFilterTest`) стоит в точке входа ПЕРЕД `GnssPlausibility` (`GpsRecordingService`, `DeviceLocation.updates`
- на карте вне записи маркер не «плавает»). Он **ничего не отбрасывает**: для каждого фикса возвращает позицию (исходную либо «якорь стоянки») и признак `held`;
время и заявленная скорость фикса не меняются, поэтому `StopDetector` получает непрерывный поток отсчётов (его окна не рвутся) и не теряет трогание.
- **Вход в «стоянку»:** подряд ≥ 15 с (≥ 3 фиксов) со скоростью датчика < 2 км/ч, все в радиусе max(100 м, 2 × точность) от центроида окна. Якорь = взвешенный
  (1/точность²) центроид, замораживается после 30 фиксов. Фиксы с точностью хуже 100 м в решениях не участвуют.
- **В стоянке** фикс внутри радиуса - дрейф: позиция = якорь, `held = true`; в трек такая точка пишется как «стоянка» (`TrackThinner.accept(point, held)`: раз в 60 с,
  даже если датчик заявил скорость), т.е. за стоянку 10 мин - ~10 точек в одном месте вместо сотен разбросанных.
- **Движение ПОДТВЕРЖДЕНО** (фильтр отпускает позицию, `held = false`) одним из способов: (1) допплер ≥ 3 км/ч у ≥ 4 фиксов подряд (серия держится, пока ≥ 2 км/ч)
  И согласованность: смещение первого→последнего D совместимо с путём E = Σv·Δt (D ≤ E + 3 × шум; D ≥ E/2 - 3 × шум; шум = max(5 м, средняя точность)), ломаная не длиннее
  2E + 2,5 × шум на шаг (хаотичные скачки туда-обратно не проходят), и при известном курсе направление смещения совпадает с курсом (≤ 70°); так проходят маневры 3-10 км/ч
  с шумом 10 м, реверс, разгон (подтверждение за 3-6 с, потеряна лишь пара точек у якоря); (2) фиксы дальше радиуса от якоря, согласованные между собой (в пределах 40 м),
  ≥ 5 штук за ≥ 20 с - якорь переносится в новое место (`NEW_AREA`; одиночный скачок, вернувшийся к якорю, игнорируется); (3) страховка: дальше радиуса непрерывно ≥ 30 с -
  уход со стоянки, дальше всё как есть (не «залипнуть» навсегда, если скорость датчика недоступна).
- **Пауза между фиксами > 30 с** (потеря сигнала) сбрасывает фильтр: за паузу поезд мог уехать, якорь недействителен; первый фикс после неё проходит как есть.
- Скорость между 2 и 3 км/ч не доказывает ни стоянку, ни движение: ползущий < 2 км/ч считается стоянкой (позиции у якоря до ухода > радиуса / страховки), ползущий 2-3 км/ч
  вход в стоянку не включает.

**Пропуски на записанном треке (разбор 12.10.2026, `Track.db`, треки 09-11.10).** Дыры > 2 мин при скорости > 20 км/ч: 11.10 - три на участке с точностью 60-240 м
(17:40-17:56, плохой сигнал; соседние фиксы отвергал `GnssPlausibility` как «прыжок» после попавшего в якорь фикса с большой погрешностью - исправлено «учётом точности» выше), 10.10 -
дыра 149 с при точности 2-4 м (причина по данным не установлена: нет GNSS либо ранний отброс плавности) и 21 мин на фоне подмены (скорость датчика 1215 км/ч - отвергнуто
верно). На стоянках «дыры» в 3-25 мин при скорости 0: по-видимому, дрейфующие фиксы отвергались как «прыжок» (дрейф ~200-280 м за секунды) до принятия «новой области» через 60 с - `DriftFilter`
подставляет якорь, поэтому точки стоянки больше не отбрасываются. `signalOk = false` по-прежнему не пишет «точку стоянки» без сигнала (иначе потеря сигнала выглядела бы как стоянка);
`PathCleaner` влияет только на отображение, на запись не влияет.

#### Зоны без GPS (решение 10.10.2026; только локальные данные, только Android)

С 21.10.2026 кнопки «Зоны без GPS» в колонке нет: слой включается переключателем «Зоны плохой связи» в «Настройках карты» (и «живой», и `viewOnly`).
Выключен по умолчанию; личный выбор запоминается на устройстве (`SharedPreferences` `gnss_dead_zones`, ключ `enabled`;
не синхронизируется). Включено — на карте красноватые (`#E53935`) строгие шестиугольники (30.10.2026, как «зоны повышенного спроса»; раньше были размытые
пятна): `FillLayer` + тонкая обводка `LineLayer` (`#C62828`, 1,2 px). Ячейки ≈250 м пересчитываются на фиксированную гексагональную сетку
(плоская вершина, сторона 150 м на земле, проекция с широтой, округлённой до градуса — сетка не «ползёт» при панорамировании), вес
шестиугольника — наибольший из ячеек в нём; прозрачность заливки 0,28…0,55 по весу: 1 поездка — бледно, ≥4 — насыщенно). Тап по зоне по-прежнему ищет ближайшую ячейку (`gnssZoneAt`). Слой лежит **под** всеми слоями приложения (линия пути,
остановки, подписи, значок положения) и под подписями базовой карты (вставлен перед первым `SymbolLayer` стиля).
- **Источник:** собственные треки из `Track.db` (все, не только этого поезда), включая трек без поезда. Работает офлайн, **без подписки и без согласия №2**; сами треки
  существуют, только если дано согласие №1 на сохранение (§17.7) — без него зон нет. Ничего не отправляется на сервер, схема `Track.db` не менялась.
- **Детектор** (`domain/gnss/GnssDeadZones.kt`, `GnssDeadZoneDetector.detect(points)`; все пороги — `GnssDeadZoneConfig`; общий для будущей общей карты):
  `GAP` — между соседними записанными точками больше 30 с, если предыдущая точка в движении или со скоростью неизвестной, и больше 90 с, если предыдущая «стоянка»
  (< 2 км/ч; запись на стоянке идёт раз в 60 с, поэтому порог тот же, что у «нет данных» на карте); разрыв > 20 мин не считается (другой сеанс/телефон выключен);
  `POOR_ACCURACY` — точность fix хуже 50 м (`GnssQualityConfig.DETECTOR_MAX_ACCURACY_M`) на отрезке ≥ 15 с от последней хорошей точки до следующей хорошей (единичный плохой
  fix игнорируется); `SPEED_JUMP` — относительно последней достоверной точки (в окне 120 с): смещение ≥ 300 м со скоростью > 300 км/ч, либо заявленная скорость > 250 км/ч, либо
  изменение скорости ≥ 40 км/ч быстрее 11 км/ч за секунду. Участок без достоверной точки ПЕРЕД ним (трек начался с плохого fix: холодный старт) не засчитывается;
  точки с откатом времени пропускаются. Координаты недостоверных точек не используются — только достоверные опорные (до и после участка).
- **Ячейки** (`GnssGrid`): фиксированная всемирная сетка ≈250 м (строка = floor((lat+90)/0,00225°), шаг по долготе — целые микроградусы round(2250/cos(широта центра строки))).
  Участок накрывает ячейки по хорде между опорными точками шагом 100 м (хорда длиннее 3 км — только ячейки на концах). Зона = ячейка; `tracks` — число РАЗНЫХ треков, в которых
  ячейка попала в плохой участок (повторы внутри одного трека не копятся); показываются зоны с `tracks ≥ 1`. Результат по треку кэшируется в памяти процесса по (трек, число точек).
- **Расчёт:** при включении кнопки (и при открытии карты с включённым слоем), в фоне (`Dispatchers.Default`); пока идёт запись «Автозапись» — пересчёт раз в минуту. Ни одной зоны —
  Toast «В ваших поездках нет мест с потерей GPS.» — только когда пользователь сам включил слой переключателем в «Настройках карты»; при открытии карты с уже включённым слоем тоста нет (21.10.2026).
- **Тап по пятну** (после проверки пути, остановок и концов пути — на самой линии пути остаётся карточка точки; ближайший центр ячейки не дальше 200 м и не дальше 24 dp): белый маркер с тёмной
  обводкой в центре ячейки и карточка: «Здесь пропадал GPS» / «Замечено в N поездках» (склонение: 1 — «поездке», 11–14 — «поездках») / «<виды> (до N с | до N мин)» (виды: «нет данных GPS»,
  «низкая точность», «скачки положения» через запятую; длительность — самый длинный участок, < 90 с в секундах, иначе в минутах) / «Последний раз дд.мм.гггг» (пояс отображения) /
  примечание «По вашим записанным поездкам. Причина неизвестна: глушение, тоннель, выемка или сбой телефона.». Выбор снимается тапом мимо или выключением слоя.
- **Общая карта зон (серверные ячейки, 11.10.2026; контракт сервера — `docs/GNSS_DEAD_ZONES_PLAN.md` §12).**
  - **Кому:** только вошедшим в аккаунт (JWT; без подписки). Не вошёл или сессия недействительна — серверных ячеек нет, остаются локальные пятна; кэш на устройстве стирается. Запрос идёт только при включённой кнопке «Зоны без GPS».
  - **Запрос:** `GET /v1/gnss/zones?minLat&minLon&maxLat&maxLon` по видимой области, когда камера остановилась. Область режется на плитки 0,5° (не больше 4, ближайшие к центру; сервер принимает ≤ 1° по оси); кэш плитки — `cacheDir/gnss_zones/<ty>_<tx>.tile` (ETag, время сверки, ячейки), запрос на плитку не чаще раза в 5 мин с `If-None-Match` (304 — кэш продлевается), независимо от исхода. 422 `too_many_cells` — плитка делится на четыре. 429 — пауза всех запросов на час. 404/503/сеть/любая ошибка — тихо, без плашек и тостов, показывается кэш (или только локальные). Ответ — `{step, updatedAt, cells:[{iy, ix, level 1..3, kinds}]}`; ячейка на сервере появляется с задержкой до ~15 мин; порог сервера — не меньше 3 разных устройств в 2 разных дня за 90 дней.
  - **Один слой:** локальные и серверные ячейки объединяются по ячейке `(iy, ix)` (`GnssZoneMerge`): цвет по максимуму веса — локально `tracks/4` (1 поездка — бледно), общий уровень 1/2/3 → 0,5/0,75/1,0. Локальные пятна работают офлайн и без входа.
  - **Карточка общей ячейки:** «Здесь у машинистов пропадал GPS» / «Случается: изредка | нередко | часто» (по level 1/2/3; точное число машинистов не раскрывается) / виды сбоев («Нет данных GPS, Низкая точность, Скачки положения») / если есть свои поездки — «Ваши поездки: N», «<виды> (до …)», «Последний раз дд.мм.гггг» / примечание «Данные обезличены и собраны по поездкам других машинистов. Причина неизвестна: глушение, тоннель, выемка или сбой телефона.». Ячейка только из локальных данных — прежняя карточка «Здесь пропадал GPS».
  - **Вклад (отправка):** под согласием №2 в редакции 2 (см. §6.9 «Согласие №2») и только при входе. За сеанс «Автозапись» в памяти копятся точки (`GnssSessionBuffer`: прореживание раз в ~5 с в движении и раз в 60 с на стоянке, потолок 10 000; Track.db и согласие №1 не нужны). По окончании сеанса: `GnssPlausibility.sanitize` → `GnssDeadZoneDetector.detect` → `GnssDeadZones.cellsOf` → ячейки с маской видов (GAP=1, POOR=2, JUMP=4) и `count` 1..100, отсортированы (порядок поездки не раскрывается), ≤ 1000. Пустой вклад не сохраняется и не отправляется. Файл `filesDir/gnss_contrib/<sessionId>.cells` (без времени, кроме момента окончания в заголовке) → `GnssZoneContribWorker` (WorkManager, только при сети, backoff 1 мин, до 8 попыток).
  - **`POST /v1/gnss/zones/contrib`:** `{contribId = id сеанса (повтор = duplicate), tag, step: 1, day: UTC-день окончания сеанса, cells:[{iy, ix, kinds, count}]}`. `tag` — случайный UUID устройства (`SharedPreferences` `gnss_contrib_tag`), не связан с аккаунтом, ротация раз в 30 дней, стирается при отзыве. Файл удаляется после ответа сервера; 422/400/413 — вклад отбрасывается; 401, 404 (сервер не развёрнут), 429, 5xx, сеть — остаётся для повтора до 6 суток (старше — удаляется, сервер принимает день поездки не старше 7 суток). Отзыв согласия №2 — файлы и tag удаляются, отправка отменяется. Запросы помечены как «гео» (401 на них не разлогинивает). Ячейки, tag и координаты не логируются.
  - Код: `domain/gnss/GnssSharedZones.kt`, `data_remote/.../tracks/GnssZones{Api,Dto,Repository}.kt`, `GnssZoneContributor.kt`, `features/route/.../autorecord/GnssFiles.kt`, `app/.../track/GnssZoneContribWorker.kt`; тесты `GnssSharedZonesTest`, `GnssZonesTest`, `GnssFilesTest`, `TrackConsentStoreTest`.

#### Знакомство с картой и «Автозапись»: подсказки на месте, экран первого включения, напоминание (решение 12.10.2026; только Android)
Тексты утверждены (`docs/ONBOARDING_MATERIALS.md` §2-§4), в коде - `domain/onboarding/OnboardingRules.kt` (чистая логика) и `component/CoachMark.kt` (UI).
- **Подсказки на месте (coach marks).** Облачко со стрелкой к элементу: светлая карточка цвета поверхности приложения, слева круглый значок «i», заголовок + текст, справа заливная кнопка «Понятно» акцентного (синего) цвета (`CoachMarkPopup`, общие `HintBadgeIcon`/`HintPrimaryButton`; popup без фокуса, не блокирует экран, закрывается только кнопкой; над элементом, если он в нижней половине экрана, иначе под ним; элемент прокручен за экран - не показывается). Каждая подсказка показывается **один раз** (помечается в момент показа, не при закрытии): ключ в `SharedPreferences` `onboarding_hints` (`shown`); действие по самому элементу (нажали «Карта» / «Автозапись» / «Пригласить друга») тоже считается показом. Не показываются: пока пользователь не увидел экран «Главная» (флаг `home_reached` ставит `HomeScreen`), пока идёт запись «Автозапись» (`AutoRecordManager.state.enabled`), пока открыты шторки/карта/диалоги экрана. Существующим пользователям показываются один раз после обновления; хранится на устройстве, на сервер не уходит.
  1) «Поезд», кнопка «Карта»: «Карта и Профиль» / «Рассчитать время прибытия, просматривайте историю поездки. Где и с какой скоростью двигались - смотри в истории.» 2) «Поезд», чип «Автозапись» (после п. 1, по одной): «Режим «Автозапись»» / «Записывает стоянки и отправления сам. Выключите, когда закончите, или он выключится через 12 часов.» 3) «Профиль», строка «Пригласить друга» (§18.1; строка есть только в авторизованном профиле): «Приглашайте друзей» / «После первой оплаты друга вы оба получите бесплатные дни.»
  **Подсказки к новинкам 3.0.7 (24.10.2026, `OnceCoachMark`, по одной на элемент, один раз):** 4) «Поезд», кнопка «Справка ВУ-45» (после «Карта» и «Автозапись»): «Справка ВУ-45» / «Расчёт тормозов поезда, замеры плотности и оборот. Справка сохраняется и синхронизируется с другими устройствами.» 5) «Главная», плитка «Карта» в «Инструментах» (только тем, кто уже видел «Главную» раньше; не при открытой голой карте): «Карта в «Инструментах»». 6) «Профиль», строка «Мои треки» (после «Пригласить друга», если та ещё не показана): «Мои треки». 7) «Пассажиром», поле «Явка через»: «Явка по прибытию» (зазор > 1 мин - ожидание следования пассажиром 018M, значение запоминается). 8) Список напарников (режим выбора), переключатель «Постоянный напарник». 9) Редактор серии, блок «Данные локомотива» (объём ГР, оси, нажатие, вес - подставляются в справку ВУ-45). Тексты - `OnboardingHints.TITLE_*/TEXT_*`. Нажатие на сам элемент (кнопка ВУ-45, плитка «Карта», «Мои треки») тоже считается показом. Облачко не показывается, если элемент вне экрана (в т.ч. по горизонтали).
- **Первое открытие карты - экскурсия по экрану (24.10.2026).** Поверх живой карты (не `viewOnly`): затемнение с «окном» и акцентной рамкой вокруг реального элемента и карточка шага (значок «i», заголовок, текст, «n из N» с точками прогресса, «Пропустить» / «Назад» / заливная «Далее», на последнем шаге «Понятно»). Шаги (`MapTourStep`, порядок): «Моё местоположение», «Следующая и конечная» (лента станций), «Цель», «Сигнал GPS», «Настройки карты», «Запись трека» (кнопка записи), «Профиль пути» (полоса), «Скорость» (полоса; цвет линии пути - скорость), «Подсказки» (кнопка «?»). **Шаг показывается, только если его элемент сейчас на экране** (нет маршрута - нет ленты и «Цели»; полоса скрыта - нет шага): границы берутся из `coachAnchor` (при уходе элемента с экрана сбрасываются). Карточка стоит над элементом, если он в нижней половине экрана, иначе под ним. Помечается «показано» в момент показа (вышли с карты, не дойдя до конца, - повторно не навязывается). Закрывается «Пропустить», «Понятно» или системным «Назад»; нажатие на затемнение ничего не делает. Кнопка **«?»** (описание «Подсказки») - в верхней строке карты справа от плашки «Карта поезда» - открывает экскурсию снова в любой момент.
- **Экскурсия по редактору плеча (24.10.2026, версия 3.0.6).** При первом открытии `ShoulderEditBottomSheet` (Настройки → «Плечи» и форма «Поезд»; ключ `shoulder_editor`, помечается в момент показа) - та же экскурсия (`CoachTour`), что на карте. Шаги (`ShoulderTourStep`): «Крайние станции», «Расстояние», «Измерить по карте», «Обратное направление» (только новое плечо), «Промежуточные станции», «Найти автоматически», «Плечо на карте», «Сохранение» («Готово», автосохранение), «Подсказки». Шаг пропускается, если элемента в этом режиме нет (нет блока промежуточных станций и «Найти автоматически», пока не заполнены обе крайние; «Обратное направление» - только у нового плеча). Элементы списка вне экрана (длинная схема станций) экскурсия сама прокручивает в кадр: верхние поля - к началу, «Найти автоматически» - к концу списка. В шапке редактора между названием и «Карта» - кнопка **«?»** (описание «Подсказки»), повторно открывает экскурсию. Системное «Назад» при открытой экскурсии закрывает её, а не редактор.
- **Экскурсия по экрану «Справка ВУ-45» (24.10.2026, `Vu45TourStep`, ключ `vu45_screen`, помечается при показе).** При первом открытии экрана - та же экскурсия (`CoachTour`), 10 шагов: «Четыре вкладки», «Заполнить из маршрута» и «Допустимая скорость» (вкладка «Бланк»), «Лицевая и оборот» (только если для категории есть оборот), «Данные для расчёта» и «Расчёт плотности» (вкладка «Расчёт»; плотность - не для пассажирских), «Замеры плотности», «Справочник», «Сохранение и удаление» (корзина), «Подсказки». Экскурсия **сама переключает вкладку** под шаг (`TourStep.reveal`), по закрытии возвращает «Бланк». В шапке экрана слева от корзины - кнопка **«?»** (описание «Подсказки»), открывает экскурсию снова. Шапка экрана «Справка ВУ-45» (с 01.11.2026): лёгкие кнопки-плитки `HeaderTile` 44 dp (скругление 14 dp, фон `Pst.surface`, рамка 1 dp `Pst.border`, без тени): «Назад» (шеврон) слева, «?» и корзина справа; название «Справка ВУ-45» - простым текстом по центру экрана, между «Назад» и «?». На экранах поверх карты остаётся `MapButton` с тенью.

**Единая шапка экранов (01.11.2026).** Все обычные экраны (не поверх карты) используют `ScreenHeader` из `component/HeaderTile.kt`: слева слот навигации, справа слот действий, название по центру экрана (симметрично более широкой стороне, одна строка, многоточие). Кнопки - лёгкие плитки: `HeaderBackTile` (шеврон), `HeaderCloseTile` (крестик), `HeaderTile` (иконка), `HeaderTextTile` (подпись: «Выбрать»/«Готово» режима выбора, «Все»/«Снять», «Сохранить», «Очистить», «Восстановить»; выход с сохранением из форм, настроек и справочников - плитка «Назад», а не «Готово»), высота 44 dp. Перешли на неё: «Справка ВУ-45», «Мои справки ВУ-45», «Маршруты», «Мои треки», «Данные и конфиденциальность», «Корзина», «Зарплата» (расчётный лист, поиск кода, настройки), «Статистика», «Напарники», «Напарник», «Пригласить друга», «Магазин/Покупки», формы «Маршрут», «Локомотив», «Поезд», «Пассажиром», «Прочая работа», Настройки (подразделы и редакторы); на «ДУ-61» кнопки «Назад» и «?» заменены на те же плитки. В полях-названиях с поиском (расчётный лист) поле стоит в слоте названия и имеет высоту плиток (44 dp). «Новое отвлечение» (`AbsenceScreen`) - тоже на `ScreenHeader` (плитка «Назад», название с подписью о выбранных датах; шапка в `topBar` Scaffold). Тёмная тема: `onTertiary = DarkBg` задан явно (без него Material подставляет бордовый - головка включённого свитча и иконки на акценте). Переход «Настройки» (хаб) → «Зарплата» анимирован как остальные подэкраны: «Зарплата» въезжает справа, хаб уезжает влево (при возврате наоборот); из расчётного листа и формы маршрута - fade. Под «Показать»/«Скрыть» в блоках «По плечам» (тяжёлые и длинносоставные поезда) отступ 16 dp. «Настройки карты» - тоже на `ScreenHeader`; на экране «Карта» (поверх карты) название по центру своей плитки. Главная, карты и графики (`MapButton`) не менялись.
- **Подсказки к «ДУ-61» (28.10.2026).** (а) «Поезд», кнопка «ДУ-61» нижней панели: облачко `TRAIN_RESTRICTIONS` («ДУ-61» / «Журнал ограничений скорости и предупреждений поезда: добавляйте их по плечу, а с «Автозапись» проследование запишется само.»), порядок на экране: «Карта» -> «Автозапись» -> «ДУ-61» -> «Справка ВУ-45»; нажатие на кнопку тоже считается показом; пока открыт экран «ДУ-61», облачко не показывается. (б) Экскурсия по самому экрану «ДУ-61» (`RestrictionsTourStep`, ключ `restrictions_screen`, помечается при показе, `CoachTour`): «Плечо поезда», «Добавить ограничение», «Список ограничений» (только если список не пуст), «Отчёт PDF», «Подсказки». В шапке справа от названия - кнопка **«?»** (описание «Подсказки»), открывает экскурсию снова; «Назад» при открытой экскурсии закрывает её. `OnboardingHintsStore.markShown` подмешивает уже записанные другими экземплярами ключи, чтобы не затирать их.
- **Экран первого включения «Автозапись»** (`TrackConsentDialog`, §6.2 п.6а; 31.10.2026 одно согласие вместо двух переключателей): белый полноэкранный диалог в стиле «условия и согласие»: вверху справа круглая кнопка-крестик (отмена включения, тег `track_consent_cancel`); заголовок «Автозапись»; текст «Автозапись сама записывает стоянки и отправления вашего поезда по скорости и месту. Время считается по тому моменту, когда поезд остановился или тронулся. Режим выключится сам через 12 часов.»; «Если вы согласитесь:» и два пункта (сохранение поездок: путь на карте, скорость, история; хранение на устройстве и зашифрованно на сервере, 180 дней, избранные не удаляются; передача обезличенных данных для карт: километр, высота, места потери GPS, без имени и точного времени); строка «Без согласия режим «Автозапись» продолжит записывать стоянки и отправления поезда, но путь не сохранится.». Внизу закреплены две кнопки: «Согласиться и включить» (залитая; выставляет оба согласия - №1 и №2 - и включает режим) и «Включить без сохранения» (контурная; оба согласия выключены, режим включается). Переключателей на экране нет; отзыв каждого согласия по отдельности - в Настройках → Конфиденциальность. Теги: `track_consent_continue`/`track_consent_without`.
- **Мягкое напоминание про «Автозапись»** (`AutoHintReminderWorker`, WorkManager раз в сутки; правило `AutoReminderRules`): уведомление «Попробуйте режим «Автозапись»» / «Записывает стоянки и отправления сам. Включается в «Поезде».» показывается, если одновременно: «Автозапись» сейчас выключено; за последние 7 дней есть рейсы минимум в 3 разных днях (по часовому поясу устройства; `basicData.timeStartWork`); «Автозапись» не включали за последние 30 дней (`AutoRecordManager.lastEnabledMs`); прошлое напоминание было не меньше 30 дней назад (`onboarding_reminder.auto_hint_last_ms`, ставится только при успешной постановке уведомления); есть разрешение на уведомления. Отдельный канал «Подсказки про «Автозапись»» (низкая важность) - пользователь отключает его в системных настройках уведомлений; отдельного переключателя в приложении нет. Нажатие открывает последний поезд (рейс с поездом с самым поздним началом работы, поезд по правилу «Автозапись»; те же extras `widget_train_id`/`widget_basic_id`, что у виджета), нет поездов - просто приложение.

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

**Запоминание смещения.** Введённое в поле «Явка через» значение пишется в настройки пользователя
(`UserSettings.lastEnteredWorkStartOffsetMinutes`, Int 0–1439, по умолчанию 1; JSON-имя то же;
SQLDelight Settings.db миграция 27 / колонка `workStartOffsetMinutes`; сервер —
`user_settings.lastEnteredWorkStartOffsetMinutes`, миграция 058) и подставляется при следующем
включении режима вместо 1 мин. Синхронизируется вместе с остальными настройками (LWW по
`updateAt`); POST без поля (старый клиент) значение на сервере не трогает. Вводится только на
экране «Пассажиром», отдельного пункта в Настройках нет.

**Смещение явки (`Passenger.workStartOffsetMinutes`, Int, минуты).** При включении «явки по
прибытию» на экране «Пассажиром» появляется поле «Явка через» (формат ЧЧ:ММ, ввод цифр
справа налево, 0:00–23:59, минуты ≤ 59). Явка маршрута = `timeArrival + смещение`; блок
«Начало работы» показывает именно это время. Значение по умолчанию при включении — **1 мин**;
в контракте/БД default `0` (старые записи и клиенты без поля сохраняют прежнее «явка =
прибытие»). Поле едет в `passengers[]` (POST/GET, camelCase `workStartOffsetMinutes`),
сервер хранит `passenger.work_start_offset_minutes` (миграция 057) и при выдаче
`timeStartWork` считает его от прибытия источника + смещение. Проезд пассажиром до явки
(`Pout`, §«Проезд до явки») считается до `timeStartWork`. Зазор `[прибытие; явка)`:
если он **больше 1 мин** — целиком оплачивается как «ожидание следования пассажиром»
(018M, `PASSENGER_WAITING`; `UtilsForEntities.getWorkStartWaitingInterval`), входит в рабочее
окно сегментов (ночные/праздничные/надбавки как у ожидания) и в отработанное время
(`getWorkTime`, норма/переработка); из ожидания одного лица (153L) исключается. Зазор ≤ 1 мин
(значение по умолчанию) не оплачивается и в время не входит. Ночная часть зазора (по окну ночи
пользователя) входит и в ночные деньги, и в ночные часы (строка 023L, карточка «В ночное время»,
главный экран, статистика) — `UtilsForEntities.getWorkStartWaitingNightTime`: часы и деньги считаются
по одним минутам. Выключение режима значение не стирает (повторное включение незаметно сохраняет
введённое).
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

- Сортировка записей — постоянный напарник первым, дальше по `fullName`
  (SQL `ORDER BY isPermanent DESC, fullName`, двоичное сравнение строк).
- У постоянного напарника под текстом строки — отметка-пилюля «★ Постоянный»
  (accentSoft фон / accent текст) — в MANAGE, SELECT и встроенном в Настройки списке.
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
  **Постоянный напарник из пикера:** под поиском — карточка с переключателем (Switch)
  «Постоянный напарник» (вторая строка — короткое ФИО целевого напарника). Целевой —
  **последний отмеченный** пользователем; если такого нет — отмеченный постоянный. Положение
  переключателя = `isPermanent` целевого. Нет целевого — переключатель выключен и неактивен
  («Отметьте напарника в списке»). Включение: если постоянного нет — флаг ставится сразу; если
  есть другой — диалог замены (§8.5.4). Выключение — сразу, без подтверждения (напарник остаётся
  в справочнике). Флаг пишется в справочник **сразу**, независимо от «Готово»/Back. Те же
  действия доступны переключателем в карточке (§8.5.2).
  VM `PartnerPickerViewModel(basicId)` (`toggle`, `confirm{...}`, `deletePartner`,
  `lastSelectedId`, `makePermanent`, `unsetPermanent`, `confirm/dismissReplacePermanent`).
- Инициалы аватара — первые буквы первых двух слов ФИО (uppercase; пусто — «?»);
  короткое ФИО — фамилия целиком + остальные слова инициалами с точкой («Иванов И. И.»)
  — `partnerInitials` / `partnerShortName` в `ui/partner/PartnerUi.kt`.

### 8.5.2. Экран `PartnerEditScreen` (создание/редактирование записи справочника)

- App bar: стрелка «назад» (`ic_arrow_back`) + заголовок «Напарник»; кнопки «Готово» нет.
  Выход стрелкой или системным «назад» **сразу сохраняет** текущие значения (не дожидаясь
  debounce автосейва), затем закрывает экран — как встроенный в Настройки редактор. Карточка
  под `ДАННЫЕ НАПАРНИКА`: ФИО (плейсхолдер «Фамилия Имя Отчество»), Табельный №
  (плейсхолдер «1234», цифровая клавиатура, значение mono; хранится строкой),
  Примечания (плейсхолдер «Плечи, контакты»). Под карточкой пояснение «Напарник
  сохранится в справочнике — в следующий раз его можно будет быстро выбрать в
  маршруте.». Ниже — отдельная карточка с переключателем **«Постоянный напарник»**
  (подпись «Автоматически добавляется в каждый новый маршрут. Постоянным может быть
  только один напарник.»). Включение при другом постоянном — диалог замены (§8.5.4),
  «Отмена» оставляет переключатель выключенным; выключение — сразу. Значение сохраняется
  автосейвом вместе с записью (для новой записи — как только введено ФИО).
  В edit (есть `partnerId`) — «Удалить напарника»: удаляет запись **сразу, без
  подтверждения** и закрывает экран (⚠️ Поведение Android (возможный баг, BUG-30):
  расходится с правилом подтверждения удаления §24.3). **Кнопки «Выбрать из справочника»
  здесь нет** —
  экран только создаёт/правит запись.
- VM `PartnerEditorViewModel(partnerId?)`: `persistentId` один раз (защита от дублей),
  автосейв через 500 мс после изменения любого поля (silent), `upsert`/`delete`.
  После удаления `save()` запись не пересоздаёт.
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

Поле `isPermanent` (Boolean) в `PartnerResponse` — опциональное: новые клиенты шлют его
**всегда** (true/false); если поля нет (старый клиент), сервер **сохраняет прежнее
значение** (миграция сервера 048). При загрузке с сервера инвариант «не больше одного
постоянного» восстанавливается на клиенте (остаётся первый отмеченный).

### 8.5.4. Постоянный напарник

- Постоянный — запись справочника с `isPermanent = true`; в справочнике не больше
  одного. Инвариант держит репозиторий: `setPermanent(id|null)` и `upsert` записи с
  `isPermanent` снимают флаг с остальных в той же транзакции (с обновлением `updatedAt`).
- **Автодобавление:** в каждый новый маршрут добавляется копия `RoutePartner` постоянного
  напарника (`sourcePartnerId` = его id), если его ещё нет в маршруте
  (`PartnerUseCase.withPermanentPartner`). Где: новый маршрут в форме (в том числе из
  виджета — он открывает форму), копия маршрута, «запланировать» в Календаре, мастер
  «Заполнить месяц». Импорт чужого маршрута по ссылке — **не** добавляет. Новый маршрут
  без правок пользователя по-прежнему не сохраняется. Удалить напарника из конкретного
  маршрута можно как обычно.
- **Диалог замены** (`AppAlertDialog`, обычное действие): заголовок «Заменить постоянного
  напарника?», текст «Постоянный напарник сейчас: {короткое ФИО}.⏎После замены в новые
  маршруты будет автоматически добавляться {короткое ФИО кандидата}.» (точка не
  дублируется, если ФИО кончается инициалом); кнопки «Отмена» / «Заменить».

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
  рассчитывается локальный список. Индикатора загрузки нет; список обновляется реактивно,
  без модального диалога. Уход с экрана отменяет синхронизацию. Нет сети (транспортный
  сбой) — тоста нет: в топбаре слева от «Выбрать» горит красная иконка сети
  (`AllRouteUiState.isOffline`, как на Главной), тап — snackbar «Нет связи с сервером»;
  успешная синхронизация её гасит. Прочая ошибка — snackbar с понятным текстом
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
  удалить» (§9.0.1; со строкой «Будут удалены и записанные треки.», если у поездов маршрута есть треки), а не `AlertDialog`.
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
    «Удалить маршрут?» (1 шт.) или «Удалить маршруты?» и строкой «Выбрано: N» (+ «Будут удалены и записанные треки.», если у поездов любого из выбранных маршрутов есть треки);
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
  безвозвратно удалены {N} маршрутов.» [+ вторая строка «Будут удалены и записанные треки.», если у поездов удаляемых маршрутов есть треки] / «Удалить» / «Отмена» (N — число удаляемых;
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
по своей тарифной ставке. На эту часть начисляется подтверждённая вредность;
зональная надбавка и остальные коэффициенты не добавляются (зональная — только
на часы работы, проверено по расчётным листкам Орехово и СПб).

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
численно. Порог строгий: ступень действует при значении БОЛЬШЕ порога, равенство
порогу доплату не включает; если подходят несколько порогов, начисляется только
диапазон с максимальным подходящим порогом.

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
фактического сверхурочного сегмента. Ни классность, ни зональная не
применяются к `018L`/`018M`; командировка по среднему исключена из обеих строк.
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
настроенная вредность (зональная надбавка на них не начисляется). Вредность по умолчанию равна `4%` по
памятке, но остаётся редактируемой.

Ожидание следования пассажиром `018M` моделируется отдельной строкой и считается
как вычисление, без отдельного поля ввода. Это непокрытый остаток рабочего
времени, примыкающий концом СТРОГО к моменту отправления пассажиром внутри смены
(сдал локомотив → ждёт → поехал пассажиром). Покрытием считаются работа на
локомотиве (от начала приёмки `timeStartOfAcceptance` до конца сдачи
`timeEndOfDelivery`), следование с поездом (первое отправление — последнее
прибытие) и следование пассажиром; перерыв исключается. Из остатка берутся только
куски, чей конец совпадает с отправлением пассажиром и чьё начало не раньше
окончания первого интервала работы на локомотиве или следования с поездом в этой
смене (ожидание бывает только после работы). Поэтому остаток от явки до первой
посадки пассажиром (подготовительно-заключительное время), остаток без
последующего следования пассажиром, а также весь остаток в маршруте, где время
локомотива и поезда не заполнено, ожиданием не считаются и остаются обычной
работой по тарифу — в чисто грузовом маршруте `018M` = 0. Оплата — по часовой тарифной ставке (как `018L`) и
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
хотя бы один пассажирский или пригородный (КСОТ: «при вождении пассажирских и
пригородных поездов» — 50%) — вся смена идёт по пассажирской строке; иначе (в т. ч.
поезд не указан или без номера) — по грузовой.
Доплата за сдвоенный поезд начисляется только на фактический станционный интервал
поезда внутри выбранного месяца. Перерыв и следование пассажиром исключаются,
а части до и после изменения тарифа используют соответствующие ставки. Проценты
`30%` для первого и `15%` для второго сохраняются до подтверждения локальным актом.
Классность рассчитывается от оплачиваемых рабочих сегментов без следования
пассажиром. Вредность включает рабочие часы, `018L` и проезд до явки; перерыв
исключается общим конвейером, а денежная база каждой части использует ставку,
действовавшую до или после изменения тарифа. Зональная (`150А`) считается
только от часов работы: следование пассажиром `018L` (в смене и до явки) и
ожидание `018M` в базу не входят. Проверено по расчётным листкам Орехово и СПб
(сентябрь 2026): база `150А` равна сумме строк `004L`+`004Q`+`035L`+`052L`+`072L`+
`073L`+`073M` до 0,05 ₽. Памятка (п. 1.4) пишет иначе — верить листкам. Строка «Другие надбавки»
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
Время этих доплат — вся рабочая смена маршрута без перерыва, следования и
ожидания следования пассажиром (как в расчётном листке работодателя и памятке:
«за фактически отработанное время в данной поездке»); времена станций поезда
на доплату не влияют, поэтому поезд без заполненных станций тоже даёт доплату.
Если в маршруте несколько поездов, на всю смену применяется ступень самого
тяжёлого (наибольший превышенный порог), поэтому одна строка времени не
начисляется дважды. Результат ограничивается выбранным месяцем; изменение
тарифа внутри смены делит деньги по ставкам соответствующих сегментов.
**Доплаты по плечам (тяжеловесный / длинносоставный).** Для каждого поезда маршрута действуют ступени его
плеча (`Train.servicePhase.id` → актуальное плечо из `UserSettings.servicePhases`; копия плеча в рейсе ступени
не хранит), если у плеча список не пуст, иначе — общие из настроек ЗП; тяжёлые и длинные списки выбираются
независимо. Правила порога (строго «больше») те же. На смену берётся ступень самого «выгодного» поезда: между
ступенями одного списка — с наибольшим порогом, между списками плеча и общим — с наибольшим процентом. В расчётном
листке строки доплаты — это ступени общего списка (по порядку, даже нулевые) плюс по одной строке на каждый процент
ступеней плеча, которого нет в общем списке (процент, равный общему, попадает в строку общего). Признаки
«Повышенная масса / длина» на карточках маршрута учитывают ступени плеча. В «Настройки → Зарплата» карточки «Доплата за тяж. поезда» и «Доплата за длинносост. поезда» содержат и общие ступени (строки [порог][%]),
и ниже, под подписью «По плечам» (справа переключатель «Показать» / «Скрыть», по умолчанию список скрыт), разворачиваемый список плеч (подпись «свои: N» / «общие»): внутри строки [порог][%] плеча и «Добавить». Удаление любой строки доплаты (общей, по плечу, удлинённого плеча) — корзина справа в строке
(свайпа нет), затем подтверждение «Удалить доплату?»;
правка сохраняется через ~0,5 с в `UserSettings.servicePhases` (метка `updateAt` обновляется) и уходит на сервер вместе
с настройками (`POST /v1/settings`; поля плеча `surchargeHeavyTrainsList` / `surchargeLongTrainsList`, добавочные,
старые клиенты их не затирают).
Для удлинённого плеча сохраняется текущее правило выбора диапазона по сумме
положительных расстояний поездов маршрута. После выбора одного наибольшего
подходящего порога оплачиваются рабочие сегменты маршрута без перерыва и
следования пассажиром; смена месяца или тарифа делит их по соответствующим
границам. Пороги плеча проходят ту же проверку положительных целых значений,
числовую сортировку и устранение дубликатов, что пороги массы и длины.

Округление как в расчётном листке работодателя: по строкам «часы × ставка»
(ночные, пассажиром, ожидание, резерв, праздничные, одно лицо, сдвоенные,
тяжеловесные/длинносоставные/плечо) часы каждой смены округляются до сотых,
умножаются на ставку (и процент) и округляются до копейки, затем суммируются по
сменам; не по точным миллисекундам всего месяца (сверка: ночные за сентябрь 2026 —
70,97 ч и 10 696,30 ₽ в листке). НДФЛ в РФ округляется до полных рублей (ст. 52
НК РФ; 50 копеек и больше — вверх), для других стран — до копеек.

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
| `PASSENGER_WAITING` | 018M: работа минус (локомотив `[начало приёмки; конец сдачи)` и подынтервалы приёмки/сдачи, поезд `[отпр. 1-й станции; приб. последней)`, пассажир внутри смены, перерыв); берутся только куски, чей конец совпадает с отправлением пассажиром (пассажиры без `isWorkStartByArrival`) и чьё начало ≥ конца самого раннего интервала локомотива/поезда; нет ни одного интервала локомотива/поезда со временем → `018M` = 0. |
| `ONE_PERSON_FREIGHT` / `ONE_PERSON_PASSENGER` | Только при `basicData.isOnePersonOperation`. Интервал — вся смена (clip месяца) минус перерыв, `PASSENGER` и `PASSENGER_WAITING`; время станций поездов не используется. Категория одна на смену (`Route.usesOnePersonPassengerRate()`): `ONE_PERSON_PASSENGER`, если хотя бы у одного поезда `number.trim().toInt()` входит в пассажирские 1–150, 151–298, 301–450, 451–598, 601–698, 701–750, 751–788, 801–898 или пригородные (859р) 6001–6998, 7001–7098, 7101–7498, 7501–7598; иначе (нет поездов, номер пустой/грузовой) — `ONE_PERSON_FREIGHT`. |
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
| Зональная надбавка | 150A | — | `zonalSurcharge` (25) | `Σ M(seg без PASSENGER/WAITING) × %/100` (без Pout) |
| Надбавка за класс квалификации | 025L | — | `surchargeQualificationClass` (0) | `Σ M(seg без PASSENGER/WAITING) × %/100` |
| Доплата за пробег: {Отпр.} — {Приб.} | 011L | — | — | по каждому плечу: `км × ставка` (см. ниже) |
| В одно лицо | 153L | Σ `ONE_PERSON_FREIGHT` без `PASSENGER` | `onePersonOperationPercent` (40) | `Σ M × %/100` |
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
   «Работа в одно лицо», «Работа в одно лицо (пассажирский)»,
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

## 16. Отвлечения (AbsenceScreen)

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

### 16.2. Список отвлечений / норма месяца (SelectReleaseDaysScreen) — удалён

Экран `SelectReleaseDaysScreen` (с `SelectReleaseDaysViewModel`, маршрутом и `Router.showSelectReleaseDayScreen()`) был недостижимым мёртвым кодом и удалён 01.11.2026. Отвлечения добавляются через Календарь (§14.3) и `AbsenceScreen` (§16.4); «Изменить норму» в Настройках (§17.2) ведёт на `AbsenceScreen`.
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
- ⚠️ Список дней недели Пн…Вс **убран**, как и раздел «Рабочая неделя» на «Норма/Регион» (решение 01.11.2026): выбор недели
  и «своя настройка» — на экране «Норма по месяцам» (§16.3.1), открывается кнопкой «Изменить норму». Часы дней недели профиля
  остаются как шаблон: стандартная (8 ч, Пн–Пт), шестидневная (7 ч + Сб 5 ч) или значения «По неделям».
- При активной подписке профиль синхронизируется отдельным ресурсом
  `GET/PUT /v1/work_schedule/`. Сервер хранит одну запись на пользователя;
  `user_id` берётся только из JWT. Конфликт устройств разрешается по
  `updatedAt` (последняя запись побеждает), причём сервер не принимает более
  старое значение. Отсутствующая серверная запись читается как стандартный
  профиль, поэтому старые версии приложения продолжают работать без изменений.
- Профиль реактивно пересчитывает месячную норму через `NormaUseCase`, часы
  диапазона в `AbsenceScreen`, карточки отвлечений календаря, расчёт оплаты
  отвлечений и раздел графика в PDF.
- Профиль (включая норму по датам `customDays`) единообразно учитывается везде, где есть норма: норма месяца на Главном и в
  виджете, норма «на сегодня» на Главном (`getNormaHoursInDate(date, profile)`; `HomeUiState.workScheduleProfile`), расчёт
  зарплаты, Статистика (в т.ч. «Переработка» в метриках), блок «Норма на месяц/Личная норма» в PDF. Правка нормы дня сразу
  меняет эти значения; в PDF личный «Выходной» в режиме «Своя настройка» даёт 0, «Командировка» норму не уменьшает.
- **Синхронизация нормы по датам** (`customDays`, §16.3.1) — отдельным ресурсом `GET/PUT /v1/work_schedule/days/` (контракт
  `/v1/work_schedule/` не менялся). Тело: `{months:[{monthKey:"yyyy-MM" (месяц 1..12, не 0-based), days:{"yyyy-MM-dd":0..12},
  updatedAt: ms}]}`; GET без данных — `200 {months:[]}`. Единица LWW — месяц целиком: у каждого месяца своя метка
  (`WorkScheduleProfile.customMonthStamps`, ставится только на пользовательских правках, `stampChangedMonths`). Месяц, где
  правок не осталось (сброс), хранится с пустым `days` и меткой — сброс побеждает на других устройствах. Сверка
  (`planCustomDaysSync`, вызывается из `SyncManager.syncCustomDays` после синхронизации профиля недели): локальный месяц
  свежее — отправить, серверный свежее — принять, равны — ничего. PUT отвечает актуальными данными всех месяцев; сервер
  отвергает более старую метку месяца. Правки до появления меток получают метку 1 («новее отсутствия, старше любой
  настоящей»). Ошибка этой сверки общую синхронизацию не валит (повторится в следующую).
- `HOLIDAY` всегда даёт 0 ч. Для `SHORTENED_DAY` используется значение дня
  недели минус 1 ч (не ниже 0). Для `NON_WORKING_DAY` применяется профиль дня
  недели: это позволяет сделать обычную субботу рабочей при шестидневке.
- Специальная семантика сохраняется: `DayOff` и `TechnicalStudy` не получают
  норма-часы; `ChildCare` продолжает считаться по отдельному действующему правилу.

### 16.3.1. Календарь нормы «Своя настройка» (SettingsNormMonthContent)

Подэкран `SettingsSubScreen.NORMA_MONTH` («Норма по месяцам», глубина 2; «Готово» и «назад» → «Норма/Регион»).
Открывается кнопкой **«Изменить норму»** на «Норма/Регион».
- «Норма за месяц» — отдельная карточка. Ниже заголовок **«РАБОЧАЯ НЕДЕЛЯ»** и карточка вариантов: **«Стандартная · 8 ч, Пн–Пт»**,
  **«Шестидневная · 7 ч + Сб 5 ч»**, **«По неделям»** (раскрывающийся шаблон). Выбранный вариант — «✓ », жирный, акцентный цвет;
  «По неделям» выделяется так же, только когда активна своя неделя (иначе обычный цвет). Выбор недели **сбрасывает правки по дням только показанного месяца** (другие месяцы и уже
  стоящие ВЫХ не затрагиваются) и возвращает шаблон «По неделям» к значениям выбранной недели. Правки по дням
  (`WorkScheduleProfile.customDays`) действуют при любой выбранной неделе. Любая правка дня, «Применить к месяцу» или постановка ВЫХ переводит неделю в «свою» (`CUSTOM`): обе недели
  перестают быть отмеченными. Пока неделя стандартная/шестидневная, «Выходной» из Календаря норму не меняет (§16.1.1); как только
  режим стал «своим», день с ВЫХ нормы не имеет.
- Шапка «‹ {Месяц} {год} ›» (листание только по месяцам, которые есть в производственном календаре; выбор
  в настройках приложения не меняет; старт — `settings.selectMonthOfYear`), строка «Пн…Вс», сетка как в Календаре
  (ячейки 8dp, `surface`, рамка `outlineVariant`, mono-цифры).
- Ячейка: число месяца (праздник — красным, Сб/Вс — приглушённо), по центру **норма на день** (mono 18sp), внизу
  **норма с начала месяца** (mono 10sp; пусто у дней без нормы — ВЫХ, праздник, отвлечение, 0 ч). Постоянной подсветки изменённых дней
  нет: изменившаяся норма дня (или ВЫХ) отмечается миганием рамки ячейки **3 раза** (пульс в цвет `tertiary`, 2×220 мс, рамка 1→2,5dp)
  **после закрытия шторки дня** (сравнивается с состоянием при её открытии: вернули прежнее значение — мигания нет); правка без шторки
  («Применить к месяцу») мигает сразу у всех изменившихся дней. При открытии экрана и смене месяца ячейки не мигают. Метки вместо числа: «ПР» (праздник, красным), «ВЫХ»
  (цвет «Выходного» Календаря, фон 16%), бейдж отвлечения (ОТП/Б/Л…).
- Под сеткой карточка «Норма за месяц» (`ЧЧ:ММ`) — то же значение, что «Норма часов» и расчёт зарплаты
  (`NormaUseCase`), и пояснение.
- Тап по дню — шторка (высота фиксирована, ничего не «прыгает»): слева дата и день недели, **«Готово» справа сверху**;
  поле с цифровой клавиатурой (0–12) и кнопки «−»/«+» (заливка `background`, отличная от фона шторки), «С начала месяца: N ч»
  (всегда на месте, «—» если нормы нет), переключатель **«Выходной (ВЫХ)»**, «Вернуть по умолчанию (N ч)» (место занято всегда,
  кнопка видна, только если значение задано пользователем). Изменения применяются сразу.
  - **Ввод с клавиатуры:** курсор при заходе в поле — в конец; первая набранная цифра **заменяет** прежнее число (было 7, набрал 8 → 8),
    дальше цифры дописываются (1, 2 → 12). Если набралось больше 12 («81»), остаётся последняя цифра.
  - **Праздник:** нормы и ВЫХ нет, шторка показывает название праздника — региональное (БД `RegionalHoliday`, если выбран регион),
    иначе федеральное РФ по фиксированной дате (`RU_FEDERAL_HOLIDAY_NAMES`, как в Календаре §14), иначе «Праздничный день».
  - **Отвлечение:** название и непрерывный период («Отпуск, 10–24 ноября (15 дн.)»); норма и ВЫХ недоступны — меняются в Календаре.
- **«По неделям»** (строка под «Норма за месяц», раскрывается): одна линия Пн…Вс, в каждой ячейке норма дня недели (по умолчанию —
  часы недели профиля); тап — шторка со счётчиком (0–12). Кнопка **«Применить к месяцу»** переносит шаблон на показанный месяц:
  норма каждого дня становится значением его дня недели (сокращённый день — на 1 ч меньше); значения, равные умолчанию,
  как правки не хранятся. Для каждого дня недели в шторке («Пн…Вс», счётчик 0–12) есть переключатель **«Выходной (ВЫХ)»**: такой
  день недели в ячейке шаблона показывается как «ВЫХ», а при «Применить» все такие дни месяца получают ВЫХ (тот же `ReleaseDay`
  `DayOff`, что в Календаре; уже стоящие ВЫХ не снимаются). Праздники и дни с отвлечением не затрагиваются. Сам шаблон на экране живёт до выхода и в профиль не пишется.
- **Значение по умолчанию** — по шаблону недели профиля и производственному календарю (сокращённый день −1 ч, праздник 0).
  Хранятся только изменённые дни: `WorkScheduleProfile.customDays` (ключ `yyyy-MM-dd` → часы 0..12, `SharedPreferences`,
  **только локально**: в `PUT /v1/work_schedule/` не уходит, при приёме профиля с сервера сохраняется — `SyncManager.adoptRemoteWorkSchedule`).
  Если введено значение, равное шаблону, правка не хранится. Правки действуют только в режиме `CUSTOM`.
- **ВЫХ** — это тот же `ReleaseDay` типа `DayOff`, что ставится в Календаре (§14): одно хранилище, ставится/снимается
  с обоих экранов, синхронизируется как раньше. В режиме `CUSTOM` день с ВЫХ **не имеет нормы** (0 ч, как «В» в бумажном
  графике); в стандартном и шестидневном режимах ВЫХ норму по-прежнему не меняет (§16.1.1).
- Праздники берутся из `MonthOfYear.days` (теги производственного календаря страны и региона); `HOLIDAY` → 0 ч всегда.
- **Экскурсия** (`NormMonthTourStep`, ключ `norm_month_screen`, помечается при показе, `CoachTour`; показывается один раз при первом открытии,
  когда есть данные календаря и пользователь уже видел Главную): «Месяц», «День» (первая ячейка с нормой), «Норма за месяц»,
  «По неделям», «Подсказки». Кнопка **«?»** (как кнопки карты, `MapButton` 40dp) — в верхней панели справа в одном ряду с заголовком
  «Норма по месяцам», открывает экскурсию снова; затемнение — на весь экран, включая панель; «Назад» при открытой экскурсии закрывает её.
- Расчёт: `buildMonthNorm` (`NormaUseCase.kt`) — единый для нормы месяца, нормы на дату и календаря; день под отпуском/
  больничным и т.п. в норму не входит (`countedHours = 0`).

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
  въезд справа, назад — слева; контейнер анимации всегда на весь экран, без анимации размера — иначе нижняя кнопка «Добавить плечо» и контент прыгают по вертикали).
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
   месяца); ниже синяя строка-кнопка **«Изменить норму»** → экран «Норма по месяцам»
   (`SettingsSubScreen.NORMA_MONTH`, §16.3.1). Переход на «Новое отвлечение» отсюда
   убран (01.11.2026): отвлечения добавляются в Календаре (§14.3).
   Подпись: «Месячная норма рабочего времени. По ней считается переработка и
   недоработка.»
2. Раздел «РАБОЧАЯ НЕДЕЛЯ» с этого экрана **убран**: «Стандартная» и «Шестидневная» неделя теперь
   на экране «Норма по месяцам» (§16.3.1), «Своя настройка» — это сам календарь нормы. Правила расчёта — §16.3.
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
  часов для расчёта доплаты за работу ночью. Время указывается по МСК.». «БУДУЩИЕ МАРШРУТЫ» — переключатель
  «Учитывать будущие маршруты» / «С ещё не наступившей явкой» (`isConsiderFutureRoute`);
  подпись «Маршруты, время явки которых не наступило, будут учитываться при
  подсчёте отработанного времени.»
- `NightRangeSheet`: заголовок «Ночные часы», справа синее «Готово»; подпись
  «Интервал, который считается ночным временем (по МСК)»; две крупные моно-плитки «Начало» →
  «Конец» (ЧЧ:ММ), каждая открывает `AppTimePicker` («Начало ночи»/«Окончание ночи»,
  недавние значения по ключам `night_start`/`night_end`) и меняет только черновик;
  внизу кнопка «Сохранить». «Готово» и «Сохранить» одинаково сохраняют обе границы;
  закрытие шторки без них ничего не сохраняет. Интервал через полночь валиден.
  Пресетов нет.
- Учитывать будущие маршруты, формат времени и cross-month timezone применяются
  сразу и инициируют реактивный пересчёт экранов, но не переписывают маршруты.

### 17.4. Справочники и редакторы норм (бывш. §30.4)

- Списки серий/станций: поиск/список, add, tap→editor, swipe/delete→confirmation.
  Пустое состояние содержит действие добавления. Кнопка **«Добавить серию» / «Добавить станцию»** — плавающая у нижнего
  края (`ScrollHidingBottomBar`, 600 мс, как нижняя панель формы «Поезд»): уезжает вниз при прокрутке списка и возвращается
  при прокрутке назад; в конце списка оставлен запас 80 dp. Подсказки станций при вводе — см. дополнение в конце.
- Редактор серии: имя, тип тяги, тип нумерации секций (`1/2/3` или `А/Б/В`),
  четыре нормы приёмки/сдачи (§0) и блок «ДАННЫЕ ЛОКОМОТИВА». Нумерация показана отдельным блоком
  «Номер секции» с выбором `1/2/3` или `А/Б/В`; выбор сохраняется автоматически;
  `0` нормы сохраняется как `null`. Изменение имени из `TimeBottomSheet`
  синхронизируется обратно по §2.12.
  **«ДАННЫЕ ЛОКОМОТИВА»** (внизу, перед «Удалить серию») — пять строк «название слева — значение
  справа (моно, `—` приглушённо, если пусто)»: «Количество секций» (1–9), «Вес» (т), «Число осей»,
  «Нажатие на ось» (тс), «Объём ГР» (л); тап → шторка ввода (десятичные — с запятой/точкой, пусто =
  «не задано», для секций пусто = значение по умолчанию). Правка сохраняется автоматически (как нормы).
  Для новой серии и для «старой» серии (только имя, записи нет) поля заранее показывают значения по
  умолчанию (`LocoSeriesDefaults.forSeries(имя)`; при смене названия — пока пользователь поля не менял);
  запись при этом создаётся только когда пользователь что-то ввёл (норма, нумерация, данные).
  Подсказка под блоком: поля подставляются в форму локомотива (секции) и в справку ВУ-45.
  **Значения по умолчанию** (`domain/brake/LocoSeriesDefaults`), правило достоверности — только
  подтверждённое: вес, оси — из `BrakeLocoCatalog` (табл. 4–5 прежней Инструкции 277); у справочных
  2ЭС4К, 3ЭС4К, 3ЭС5К, 2ЭС6 (`weightAxlesConfirmed = false`) вес не предзаполняется, а оси — да (8 у двухсекционных, 12 у трёхсекционных, открытая информация); нажатие — из каталога (у этих
  четырёх ЭС и 2ЭС5К — 14,0 по указанию владельца); объём ГР — `BrakeDensity.confirmedVolumeLiters`:
  одно точное значение из примеров групп (файл 1 / Инструкция 277), плюс 2ЭС4К/2ЭС5К/2ЭС6 = 2000 и
  3ЭС4К/3ЭС5К = 3000 по указанию владельца; выведенное по допущениям (2ТЭП60, 2М62(У), 3ТЭ10М, 3М62У,
  4ТЭ10С, ЧС1–ЧС4(Т), ЧС3, ЧМЭ3Т/Э, ТЭМ2А/УМ), диапазоны, ВЛ10 (зависит от номера) и серии без данных
  — пусто. **Секции** — по первой цифре названия, только если название *начинается* с цифры (2ЭС6 → 2,
  3ЭС4К → 3, 4ТЭ10С → 4, 2ТЭ10У → 2, 2М62У → 2), иначе 1 (ЭП1М, ВЛ80, ВЛ10, ЧС7, ТЭМ2, ЧМЭ3, ЭП2К,
  ТЭП70, ТГ102 — цифра внутри названия часть индекса). Исключений среди серий каталога нет (тест
  `catalogSeriesNamesStartingWithDigitAreAllRealSectionCounts`).
- Редактор станции (`SettingsStationEditorContent`), сверху вниз: «ОСНОВНОЕ» — имя;
  «КООРДИНАТЫ» — статус (зелёная точка «Координаты есть» + моно «lat, lon · ЕСР …» /
  **Справочник (30.10.2026):** в шторке станции плеча (промежуточной и крайней) введённое/выбранное название сразу ищется в справочнике станций (`normalizeName`: «22км» = «22 км», регистр/ё/дефис не важны); найдена с координатами — карточка «Координаты есть · Из справочника станций», поиск не нужен, координаты уходят в плечо по «Готово» (явный выбор на карте/поиск главнее). В подсказки названия добавлены станции справочника с координатами.
  **Железная дорога станции (30.10.2026):** у найденной поиском станции в подписи показывается дорога («Октябрьская ЖД», «Южно-Уральская ЖД») — в карточке «Нашлось» шторки станции плеча, в списке выбора среди одноимённых, в «Определить автоматически» карточки станции справочника и в подтверждении на карте поезда. Источник (`RailwayByEsr`, domain, чистая функция без ввода-вывода): сначала тег `operator`, если в нём есть название дороги («…железная дорога»/«…ЖД»; просто «РЖД» не считается); иначе код ЕСР по таблице диапазонов (первые две цифры — сетевой район, дорога занимает несколько подряд; на стыках Ю-В/Северо-Кавказская/Приволжская/Крымская/Якутия — отдельные диапазоны; Сахалин (99xxxx) подписывается «Дальневосточная ЖД» — дорога в её составе; БАМ Хани—Тында и Нерюнгри — Дальневосточная, станции за Нерюнгри — «Железные дороги Якутии»; границы уточнены по подтверждению пользователя 30.10.2026). Таблица выведена из выгрузки osm.sbin.ru/esr (≈25,7 тыс. станций): на 16 249 российских станциях совпадение с OSM 99,7 % до правок по Сахалину. Станции СНГ/Балтии, неизвестные и нешестизначные коды дают пустую подпись — лучше без дороги, чем с неверной.
  оранжевая «Нет координат» + «Без них станция не определится по GPS и не попадёт на карту
  плеча»); строка-кнопка без шеврона «Определить автоматически» («Определить заново», если
  есть) — `POST /v1/service_phase/stations/lookup` по имени, ориентиры — соседние станции по
  плечам: одна находка применяется сразу и под статусом пишется «Нашлась: {имя}[, О.П.] ·
  {регион/оператор}. Если это не та станция — укажите точку на карте вручную.»; несколько —
  шторка «Какая «{имя}»?» (`StationChoiceSheet`: те же строки с радио, ЕСР, оператором и
  соседними станциями, что при уточнении крайней станции в автоподборе; «Отмена» / «Выбрать»),
  после выбора — «Нашлась: …» + действие «Выбрать другую» (снова шторка); «Не нашлось на
  карте — …»; сбой — текст причины (§6.6 «Ошибки сети»); во время поиска «Ищем «…» на карте…» +
  «Отменить»; строка с шевроном «Указать на карте» / «Уточнить на карте» (`PhaseMapPicker`;
  точка с карты сбрасывает osmId, ЕСР остаётся). «КИЛОМЕТРАЖ» — два поля ввода в ряд на всю ширину: число вводится
  прямо в поле (цифровая клавиатура), справа постфикс «км» / «пк»; пусто — тусклые «км» / «пк»; км — целое ≥ 0, пикет 1–10 (введённое больше 10 становится 10; 0 и нечисло не вводятся),
  пикет доступен только при заданном км; пустое поле = стереть), подпись «Ось станции по
  ж/д километражу.»; затем четыре интервальные нормы (§0), те же правила `0→null`, и под ними подпись «Нормы выше используются
  автоматически при заполнении времени приёмки и сдачи локомотива на этой станции.»;
  шторка ввода нормы (как и в карточке серии) открывается с курсором в конце значения (ввод дописывает),
  незаданное значение — пустое поле; пусто при сохранении = «не задано». Всё сохраняется
  автоматически в запись `StationNorm` (новые поля `latitude/longitude/esr/osmId/kind/
  axisKm/axisPicket`, Settings.db миграция 24); запись создаётся, только когда есть
  данные (нормы, координаты, ЕСР или км). «Старая» станция (только имя) открывается по
  имени и подхватывает существующую запись с тем же именем (не создаёт дубль).
  Удаление справочной записи не очищает stationId/текст уже сохранённых маршрутов
  скрыто; экран должен корректно показать отсутствующую ссылку.
- Список станций: у строк со станциями с координатами справа значок-метка
  (`location_on`, акцентный). Разделы «С нормами» / «Без норм» — по нормам, как раньше.
- **Выпадающие списки станций** (поезд, пассажиром, прочая работа, шторка времени)
  берут названия из `UserSettings.stationList`. Удаление названия оттуда (крестик в
  подсказках) убирает только название из списка: запись `StationNorm` с данными
  (нормы/координаты/км) остаётся в справочнике; станция «только с именем» исчезает
  полностью. Удаление из карточки станции удаляет и запись, и название.
- **Синхронизация** записи: `/v1/norma_time/stations/` (слияние, см. «Синхронизация
  справочников» ниже) с новыми полями `latitude, longitude, esr, osmId, kind ("station"|"halt"), axisKm,
  axisPk` и маркером `hasGeoFields: true`. Сервер перезаписывает геополя только при
  маркере (null-поля клиент не шлёт), запрос без маркера (старый Android, PWA)
  координаты не стирает (миграция сервера `049_station_geo`).
- **Данные локомотива серии (миграция Settings.db 26, сервера 056).** Колонки `brakeVolumeL, axleCount,
  axleLoadTf, weightT, sectionCount` (nullable). Миграция БД только добавляет колонки; значения по
  умолчанию записываются Kotlin-кодом (`LocomotiveSeriesRepository.prefillBrakeDefaults`, при старте
  Android и в начале синхронизации серий): у серий с `sectionCount == null` пустые поля заполняются
  `LocoSeriesDefaults`, заполненные не трогаются, `updatedAt` растёт (значения уходят на сервер). Так у
  существующих записей 2ЭС6 получает 2 секции, ЭП1М — 1. Синхронизация — тот же `/v1/norma_time/locomotives/`
  с полями `brakeVolumeL, axleCount, axleLoadTf, weightT, sectionCount` и маркером `hasBrakeFields: true`
  (как `hasGeoFields` у станций: null-поля не отправляются, поэтому нужен маркер). Сервер перезаписывает
  эти колонки (в т.ч. очищает) только при маркере; запрос без него (старый Android, PWA) значения не
  стирает — ни при слиянии, ни при full-replace (сохраняет по `seriesId`). Слияние на клиенте: если у
  победившей копии все пять полей пусты, а у проигравшей есть — берутся они (`LocomotiveSeriesMerge`);
  дубли по названию дополняют друг друга. Порядок выкладки: миграция сервера 056 и код сервера раньше
  клиентов. PWA пока поля не знает и не отправляет (значения на сервере сохраняются).
- **Синхронизация справочников станций и серий** (`/v1/norma_time/stations/`,
  `/v1/norma_time/locomotives/`; Android, PWA и сервер ведут себя одинаково):
  - **Слияние, не full-replace.** При каждой синхронизации (не только при «ожидает отправки»)
    клиент: GET с `?includeDeleted=true` → слияние с локальным → запись локально → POST,
    только если сервер отстаёт. Не удалось получить справочник — локальное не меняется и
    ничего не отправляется. Пустые оба списка (записи и надгробия) не отправляются: пустой
    запрос без `isDeleted` сервер счёл бы full-replace старого клиента.
  - **Правило слияния** по каждому `stationId`/`seriesId`: побеждает запись с большим временем
    (`updatedAt` у живой, `deletedAt` у надгробия); при равенстве — удаление. Правка после
    удаления оживляет запись. Реализации: `StationNormMerge`/`LocomotiveSeriesMerge` (Kotlin,
    `domain/util`) и `stationNormMerge.js` (PWA) — менять синхронно, тесты есть на обеих сторонах.
  - **Удаление** — надгробие: Android — таблицы `StationTombstone`/`LocomotiveSeriesTombstone`
    (SettingsDB v25; `replaceAll` создаёт их для пропавших записей и снимает у присланных),
    PWA — сторы Dexie `normaTimeStationTombstones`/`normaTimeLocomotiveTombstones` (v12).
    Хранится `deletedAt`; старше 180 дней вычищается (и на клиенте, и на сервере).
    PWA при выходе из аккаунта очищает надгробия вместе со справочником; Android справочники
    при выходе не очищает (как и раньше).
  - **Дубли по названию** (без учёта регистра и пробелов; у серий — ещё и по типу) после
    слияния схлопываются: остаётся самая заполненная (больше заполненных норм/координат, затем
    новее, затем меньший id), недостающие поля берутся у остальных, лишние id — надгробия с
    `deletedAt` = максимум `updatedAt` группы + 1. Результат одинаков на всех устройствах.
    Ссылки маршрутов на удалённый дубль станции (`acceptanceStationId`/`deliveryStationId`)
    перенаправляются на оставшуюся станцию с тем же названием; маршрут помечается
    несинхронизированным. У серий ссылка идёт по названию — перенаправлять нечего.
  - **Контракт сервера:** элемент несёт `isDeleted` (`true` — надгробие, поле `updatedAt` =
    момент удаления). Если `isDeleted` есть хотя бы у одного элемента POST — сервер сливает по
    `updatedAt` (ничего не удаляет из-за отсутствия в списке); если ни у одного (старый клиент,
    в т.ч. пустой список) — прежний full-replace живых записей, надгробия не трогает. GET без
    `includeDeleted` надгробий не отдаёт (старый клиент принял бы их за живые записи).
    Миграция сервера `053_station_tombstones`, порядок выкладки: сервер раньше клиентов.
  - **Ограничение:** старые версии Android по-прежнему шлют full-replace и могут стереть
    станции, добавленные на другом устройстве, пока такие версии в обращении.
- **iOS (ios/norms-editors).** Списки (`SeriesListView`/`StationListView`, группы «Электровозы»/«Тепловозы» и «С нормами»/«Без норм», поиск, «Добавить …» под списком, удаление — долгое нажатие на строку → стандартный alert «Удалить …?») и экраны-редакторы (`AppRoute.settingsSection(.seriesList/.seriesEditor/.seriesNew/.stationList/.stationEditor)`; `stationEditor(id: "")` — новая станция). Логика в Kotlin (`SeriesListIosViewModel`, `StationListIosViewModel`, `SeriesEditorIosViewModel`, `StationEditorIosViewModel`, файл `NormsCatalogIosViewModels.kt`) — правила как у Android: 0 нормы → «не задано» (серия до 240 мин, станция до 120), запись создаётся только при наличии данных, имя регистрируется в справочнике автодополнения только в `commit()` («Готово»/уход с экрана/сворачивание), название сохраняется с debounce 500 мс, остальное — сразу; запись без изменений не пишется и `updatedAt` не двигает; удаление убирает и запись, и имя из `locomotiveSeriesList`/`stationList`. Редактор серии: название, тип, номер секции, 4 нормы (степперы), «Данные локомотива» (значения по умолчанию `LocoSeriesDefaults`). Редактор станции: название, **координаты — только статус** (определение по названию и выбор на карте на iOS пока нет; существующие координаты/ЕСР/osmId сохраняются как есть), км и пикет (пикет > 10 → 10, только при заданном км), 4 нормы. Синхронизация и надгробия — общий Kotlin (`replaceAll`), как на Android.
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
  **Экран списка плеч (решение 07.10.2026):** кнопка «Добавить плечо» **закреплена внизу экрана** (не прокручивается вместе со списком;
  список получает нижний отступ, чтобы последняя строка не пряталась под кнопкой) и анимируется **так же, как нижняя панель действий в
  форме «Поезд»** (те же появление/нажатие/исчезновение — взять реализацию оттуда, не изобретать новую). **Нижнее меню приложения
  (Главная/Зарплата/+/Настройки/Профиль) на этом экране не показывается** — экран открывается как вложенный (стрелка назад в шапке),
  меню возвращается при выходе. Правило касается и Android, и PWA.
  *Реализация Android:* общий компонент `ScrollHidingBottomBar` + `BottomBarScrollState` (`component/ScrollHidingBottomBar.kt`) —
  сдвиг вниз на высоту панели за 600 мс (FastOutSlowIn): панель уезжает при прокрутке списка вверх и возвращается при прокрутке
  вниз; тот же компонент использует форма «Поезд». Кнопка «Добавить плечо» — синяя (`tertiary`) на всю ширину, 48 dp, скругление
  14 dp, над ней разделитель; нижний отступ списка 96 dp. Нижнее меню скрывает `NestedScreenBarState.hideBottomMenu`
  (общее правило для всех подэкранов Настроек — §3.1: `HideBottomMenu` в `SettingsScreen`, сбрасывается при выходе в хаб; читает `LocoDriverApp`).

### 17.5. Основные, маршрут, локомотив, поезд, отдых (бывш. §30.5)

- **Основные** («Основные», `SettingsRouteContent`), сверху вниз:
  1. «ТЕМА ОФОРМЛЕНИЯ» — радио «Как в системе» / «Светлая» / «Тёмная» (§17);
  1б. «ГЛАВНЫЙ ЭКРАН» — три переключателя «Локомотив» / «Поезд» / «Пассажиром» с подписью «Всегда открывать последний» (`HomeUnitPrefs`, локально);
     подпись под группой «В разделе «Текущий маршрут», если есть несколько объектов». Тот же флаг, что чекбокс в шторке плитки;
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

**iOS (ios/settings).** Hub и под-экраны реализованы на SwiftUI (`Screens/Settings`, VM `SettingsScreenIosViewModel`): правка поля — сразу в UI, запись через 300 мс, принудительно при уходе с экрана. Тема, флаги показа полей локомотива («Отопление», «Собственные нужды», «Смена рода тока») и «Свыше 12 часов» хранятся локально в `NSUserDefaults` (ключи `ld.*`), не в `UserSettings`. Отличия от Android: смена страны/пояса только записывает поля (загрузки производственного календаря нет), блок «РЕГИОН» не показан (сквозная задача С1); группа «ГЛАВНЫЙ ЭКРАН» в «Основные» не реализована; экспорт диагностики формирует упрощённый файл (версия приложения и системы) и открывает системное окно «Поделиться»; редактор плеча упрощённый (без подбора станций и карты, §6.6).

### 17.7. Данные и конфиденциальность (только Android)

Отдельный экран (маршрут `DataPrivacyRoute`, не под-экран Настроек): открывается из **Профиля** → группа «ДАННЫЕ» → строка «Данные и конфиденциальность» (подзаголовок «Треки и история поездок», §18), а также из пустого экрана графика/карты («Открыть настройки», `Router.showDataPrivacy`). В Настройках группы «ДАННЫЕ» больше нет. Top bar как у «Основные»: заголовок по центру «Данные и конфиденциальность», слева синий
«Готово». Содержимое:
- заголовок группы «ТРЕКИ И ИСТОРИЯ ПОЕЗДОК»; карточка с переключателем «Сохранять треки» (согласие №1,
  §6.9), без подписи внутри карточки;
- сразу под карточкой — отдельная мелкая строка «Согласие дано дд.мм.гггг» (локальная дата) — только когда согласие дано; при выключенном переключателе никакой подписи («Не сохраняются» не показывается);
- затем — текст, два пункта и примечание согласия из §6.9 (дословно те же);
- включение — сразу, без подтверждения; **выключение (отзыв)** — через `AppAlertDialog`: заголовок «Отозвать согласие?», текст
  «Новые треки сохраняться не будут. Уже записанные треки останутся в разделе «Мои треки» — удалить их можно там.», кнопки «Отозвать» (красная,
  деструктивная) и «Отмена». После подтверждения запись трека прекращается немедленно; уже записанные треки не удаляются и остаются в «Мои треки».
- Ниже — вторая группа «ДАННЫЕ ДЛЯ КАРТ» (согласие №2, §6.9): карточка с переключателем «Передавать обезличенные данные для карт» (по умолчанию ВЫКЛ, независим от №1), при включении — строка «Согласие дано дд.мм.гггг», под ним мелкое пояснение («Передаются километр пути и высота, а также места, где пропадает GPS (участки карты около 250 м) — без вашего имени и без точного времени. Помогает строить профиль пути для режимных карт и общую карту мест без GPS.»; для согласия прежней редакции — ещё пояснение и кнопка «Подтвердить новые условия», §6.9). Включение — сразу; выключение — `AppAlertDialog` «Остановить передачу данных для карт?» / «Новые высоты и места потери GPS передаваться не будут. Уже отправленное вернуть нельзя: оно обезличено и не связано с вашим аккаунтом.», кнопки «Остановить» (красная) и «Отмена».
- Тексты согласия и отзыва (дословно §6.9 + здесь) говорят, что трек хранится и на устройстве, и на сервере в зашифрованном
  виде и доступен только владельцу; отзыв треки не удаляет (`PUT /tracks/consent {false}` — без удаления, выполняется воркером,
  как только появится сеть и вход); удаляются они только вручную («Мои треки»: карточка, «Удалить всю историю»). Текст отзыва — выше.
- **Цвет переключателей** (здесь и в диалоге согласия §6.9) — общий `settingsSwitchColors()`: включён — акцент `tertiary` (бегунок `onTertiary`), выключен — серый трек `surfaceVariant`, бегунок/рамка `outline`; слоты `surfaceContainer*` темы окрашены нестандартно, поэтому дефолтные цвета Switch не использовать.
- Группа «ДАННЫЕ» Профиля содержит и строку «Мои треки» (подзаголовок «История записанных поездок», §17.8).

Источник: `ui/settings/SettingsPrivacyContent.kt`, `autorecord/TrackConsentStore.kt`, `autorecord/TrackConsentUi.kt`.

### 17.8. Мои треки (история; только Android)

Отдельный экран (маршрут `TrackHistoryRoute`; открывается из Профиля → «ДАННЫЕ» → «Мои треки», §18), заголовок «Мои треки», слева
стрелка «назад». Список карточек (`SettingsTrackHistoryContent`, `TrackHistoryViewModel`), новые сверху по времени начала.
Источники: локальные треки (`Track.db`) и серверные, которых на устройстве нет (`GET /v1/tracks?limit=50`, курсор; только при
согласии и входе; кнопка «Показать ещё» при `nextCursor`; сбой сети — локальный список + примечание «Треки с сервера не
загрузились. Показаны треки этого устройства.»). При открытии экрана: удаляются осиротевшие треки (§6.9) и запускается отправка. Сироты (поезда нет ни в одном рейсе) в списке
не показываются даже до удаления (`TrackOrphanCleaner.visible`); серверные треки, удаление которых стоит в очереди, тоже скрыты.
Если трек при тапе уже исчез (локально или `404` на сервере) — карточка убирается, снекбар «Трек уже удалён» (не «Не удалось открыть»).
Пока согласия нет, вверху карточка «Треки не сохраняются» с подсказкой, где включить.
**Подписка (09.10.2026):** без активной подписки серверная часть списка не загружается; индикатор карточки у неподписанного - нейтральный «не синхронизирован» (локальный трек, не красный);
трек «без поезда» (§6.9) - заголовок «Без поезда», всегда только на устройстве. «Синхронизировать» (меню карточки и массовое) без подписки показывает `AppAlertDialog` «Синхронизация треков доступна по подписке «Машинист Pro»»
с кнопками «Оформить подписку» (экран покупок) и «Закрыть»; проверка подписки идёт ПЕРЕД проверкой согласия.

**Карточка** (паттерн карточки маршрута «Все маршруты», §9; тап — открыть карту): слева мини-карта 72 dp (ломаная превью ≤ 60 точек без
тайлов, начало — заливка, конец — кольцо; нет точек — пустая плитка); название (до 2 строк: «№ 2345 · Откуда — Куда», иначе «Трек»);
«Явка: д МММ гггг, чч:мм» — явка рейса, к которому записан трек (`basicData.timeStartWork` рейса поезда трека; у серверного трека
рейс берётся по `routeId`; рейса/явки нет — строки нет; пояс `displayTimeZone`); моно-строка «N км · в пути N ч M мин» (до 10 км — одна цифра
после запятой; время в пути — по времени первой/последней точки; пока сводка не достроена — только «в пути …»). Средней скорости на карточке нет.
Внизу карточки, справа, одним рядом (как индикаторы внизу карточки маршрута: 20 dp, отступ 8 dp): золотая звезда (трек в избранном; трек не
удаляется через 180 дней) и индикатор синхронизации: `sync_on_icon` (зелёные стрелки)
— отправлен на сервер (в т.ч. серверный), `sync_disabled_24px` — не отправлен (нет согласия / ожидает; красным — отказ сервера по лимиту,
под названием красная строка «Не отправлен: лимит хранилища на сервере»), вращающаяся `sync_24px` — отправляется сейчас. Значка «облако» нет. Пометки «Только на
сервере» / «Только на устройстве» / «Ожидает отправки» нет. Кнопок звезды и корзины на карточке нет. Сводки (длина, мини-карта, явка)
достраиваются после показа списка, по одной карточке.
**Единая карточка**: локальный и серверный трек с одним `trackId` — одна карточка (локальная копия приоритетна; серверные без локальной
копии и не стоящие в очереди на удаление добавляются; `mergeTrackItems`). Серверный трек без локальной копии выглядит как остальные (синхронизирован); отличие только в тапе: он
открывает просмотр с загрузкой (`GET /tracks/{id}` + чанки).
**Жесты** (`SwipeToRevealDelete` с `onSwipeRight` и `rightReveal`): свайп ВЛЕВО — раскрывает красную кнопку «Удалить» → `AppAlertDialog` «Удалить трек?»
(«Трек будет удалён с этого устройства и с сервера. Это действие нельзя отменить.»; для серверного — «…с сервера…»), кнопки «Удалить»
(красная) и «Отмена»; свайп ВПРАВО (дальше половины кнопки) — раскрывает слева золотую кнопку-звезду «В избранное»/«Убрать из избранного» и фиксирует карточку
открытой (до тапа по карточке, закрытия или свайпа обратно); нажатие на кнопку — действие без подтверждения + снекбар, карточка закрывается
(длинный свайп само действие не выполняет); ДОЛГОЕ НАЖАТИЕ — контекстное меню (`AppBottomSheet`, заголовок — название трека):
«В избранное»/«Убрать из избранного», «Синхронизировать» (только локальный и ещё не отправленный трек; проверки как в массовом
действии: нет согласия — диалог «Нет согласия на сохранение треков», нет интернета — `NetworkRequiredDialog`, нет входа — тост), «Удалить»
(тот же диалог подтверждения). Избранное локально меняет `isFavorite` и ставит `favoriteDirty`, серверный трек — сразу `PATCH`.
Локальный трек удаляется сразу, серверное удаление идёт через очередь (переживает перезапуск и офлайн); серверный — сразу
`DELETE /tracks/{id}` (нет сети — сообщение «Не получилось: проверьте подключение к интернету», карточка остаётся).
**Сообщения** — снекбар внизу экрана (не системный тост): «Трек добавлен в избранное» / «Трек убран из избранного» (звезда-свайп, пункт меню,
массовое действие над одним треком), «Треки добавлены в избранное» / «Треки убраны из избранного» (массовое над несколькими).
Кнопки «Удалить всю историю» нет (решение 09.10.2026): массовое удаление — через режим выбора ниже.

**Множественный выбор** (тот же паттерн и визуал, что «Все маршруты», §9.2). «Выбрать» справа в топбаре (не показывается, пока список пуст).
В режиме: слева «Все»/«Снять» (все карточки списка), по центру «Выбрано: N» вместо «Мои треки», справа «Готово» (выход); системная «назад» тоже
выходит из режима, а не с экрана. Карточка получает слева круглый маркер (`SelectionMarker`), выбранная обводится акцентной рамкой; тап
переключает отметку (вместо открытия карты), свайпы и долгое нажатие отключены. Нижнее меню приложения на экране не показывается
(отдельный маршрут, §3.1) — на его месте нижняя панель действий (разделитель, фон `surface`, как у «Все маршруты»), три действия с подписями; все
неактивны при пустом выборе или пока идёт действие:
- **«В избранное»** / **«Убрать из избранного»** (если все выбранные уже избранные; золотая звезда): ставит/снимает звезду со всех
  (локальные — `isFavorite` + `favoriteDirty`, серверные — `PATCH`). После — выход из режима, снекбар «Трек добавлен в избранное» / «Трек убран из избранного» (для одного) либо «Треки добавлены в избранное» /
  «Треки убраны из избранного», «Ничего не изменилось», если менять нечего.
- **«Синхронизировать»**: принудительная отправка выбранных локальных треков на сервер сейчас (`TrackUploader.run(token, only = ids)` — без ожидания паузы
  `QUIET_MS` и без очереди воркера; ранее отвергнутые `REJECTED` пробуются заново). Порядок проверок: нет подписки → диалог подписки; нет согласия №1 → диалог «Нет согласия на сохранение
  треков» (кнопки «Открыть настройки» → «Данные и конфиденциальность», «Отмена»); нет интернета → `NetworkRequiredDialog` («Нет интернета»);
  нет входа → тост «Войдите в аккаунт, чтобы синхронизировать треки». Результат тостом: «Отправлено треков: N» / «Трек отправлен на сервер» / «Выбранные треки уже
  синхронизированы» / «Лимит хранилища на сервере исчерпан» / «Не все треки отправлены, повторим позже» / «Не получилось: проверьте подключение к интернету»;
  серверные (не локальные) треки пропускаются («Выбранные треки уже на сервере»). При успехе — выход из режима.
- **«Удалить»**: `AppAlertDialog` «Удалить трек?»/«Удалить треки?», текст «Выбран 1 трек.»/«Выбрано треков: N. Они будут удалены с этого устройства и с сервера.
  Это действие нельзя отменить.», «Удалить» (красная) и «Отмена». Локальные удаляются сразу с постановкой серверного удаления в очередь, серверные —
  `DELETE /tracks/{id}`; сбой сети по части — они остаются выбранными, тост «Удалено: N, не удалось: M».
**Просмотр**: локальный трек читается из `Track.db`, серверный — `GET /tracks/{id}` + чанки (проверка sha256, декодирование
`ld-track-v1`); пока грузится — затемнение со спиннером; не открылся — «Не удалось открыть трек» / «Не удалось загрузить трек с
сервера». Открывается `TrainMapScreen` (§6.9) в режиме просмотра (`viewOnly`): заголовок плашки — название, пройденный путь по
скорости, стоянки по треку, тап по точке; без запроса геолокации, живой скорости, обновления по таймеру и кнопки «Моё
местоположение»; станции плеча не рисуются.

Источник: `ui/settings/SettingsTrackHistoryContent.kt`, `viewmodel/TrackHistoryViewModel.kt`, `autorecord/TrackRouteResolver.kt`,
`data_remote/.../remote_rest/tracks/*`, `app/.../track/TrackUploadWorker.kt`.

---

## 18. Экран «Профиль» и аутентификация (ProfileScreen)

### 18.S. Назначение и состояния экрана

Аккаунт, подписка, синхронизация, вход/регистрация, миграция данных. Корневая
вкладка нижнего меню «Профиль» (§3.2 `showProfile`).

**Состояния экрана (взаимоисключающие, проверяются в этом порядке):**
1. `!sessionChecked` (локальный токен ещё читается из хранилища) → короткая заглушка `GenericLoading` «Открываем профиль...». Сеть экран больше не блокирует.
2. Нужна легаси-миграция (`!isFirstAppEntry && !isMigrated`) → экран переноса данных (§18.5).
3. Есть токен (`isLoggedIn`) → авторизованный профиль (§18.1) СРАЗУ с локальными данными (имя/VK из локальных данных, срок подписки из `UserSettings.subscriptionPeriod`, пункты списка). Сетевые блоки (email аккаунта, VK-шапка) показывают индикатор загрузки, при ошибке - «Ошибка загрузки»/кнопка. Вверху списка баннер `ProfileDataBanner` (`ProfileUiState.dataBanner`): пока `userDetailsState == Loading` - «Загружаем данные. Текущая информация могла устареть» со спиннером; при ошибке загрузки (нет сети) - «Нет сети. Показаны сохранённые данные, они могли устареть» и «Повторить» (→ `refresh()`; при активном VPN под баннером `VPN_ERROR_HINT`); после успешной загрузки баннер исчезает. Полноэкранных состояний «Нет интернета» нет. Шапка (VK/имя) и почта объединены в ОДНУ карточку (разделитель между ними), без отдельной подписи «EMAIL»; почта локально не хранится (в защищённом хранилище только токен, userId и vkId), поэтому ждёт сети; индикатор загрузки - один (в шапке, пока грузится и VK-профиль, и профиль аккаунта). Диалог синхронизации («Синхронизация данных») содержит кнопку «Отмена» - доступна сразу, прерывает синхронизацию (отмена job, `ProfileViewModel.cancelSync`/`HomeViewModel.cancelSync`), диалог закрывается, тост «Синхронизация отменена»; данные не портятся (merge по updatedAt/full-replace идемпотентны, недоставленные маршруты остаются несинхронизированными).
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
(email — query-параметр, тела нет) с email из поля (пробелы удаляются). **Перед запросом — проверка:**
пустое поле → snackbar «Введите email, на который отправить ссылку», неверный формат →
«Проверьте email: адрес введён неверно» (запрос не уходит, кулдаун не запускается). Во время запроса — snackbar «Отправляем
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
   - строка **«Пригласить друга»** / «Получите бесплатные дни обслуживания» → §18.R. Один раз над строкой показывается подсказка «Приглашайте друзей: после первой оплаты друга вы оба получите бесплатные дни.» / «Понятно» (§6.9, «Знакомство с картой и «Автозапись»»).
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
6. Группа **ДАННЫЕ** — одна карточка с тремя строками (иконка в плитке, заголовок, подзаголовок, шеврон), порядок:
   «Мои треки» / «История записанных поездок» (→ экран «Мои треки», §17.8, `Router.showTrackHistory`);
   «Данные и конфиденциальность» / «Треки и история поездок» (→ §17.7, `Router.showDataPrivacy`);
   «Корзина маршрутов» / «N маршрут/маршрута/маршрутов»
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
- **Резервный способ (28.10.2026):** под подписью CTA всегда есть второстепенная (outlined) кнопка
  «Оплатить резервным способом» — `PurchasesViewModel.onFallbackPaymentClick` →
  `POST /v1/payment/ckassa/checkout` для выбранного тарифа, ссылка открывается в браузере;
  ожидаемая оплата и поллинг те же, что у Robokassa. Автопродление через CKassa не
  поддерживается (галочка относится только к Robokassa). Во время создания платежа обе
  кнопки заблокированы (`isStartingPayment`).
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

**Отчёт о проследовании ограничений (28.10.2026)** — отдельный PDF из экрана «Ограничения поезда» (§6.9, «Журнал проследования ограничений»), тот же `PdfActionSheet`
(«Открыть PDF» / «Поделиться»); формируется в любое время, когда журнал не пуст (28.10.2026). Если не всё проследовано, отчёт **промежуточный**: под заголовком красная строка «Промежуточный отчёт: проследовано N из M[, в зоне K]», в шапке время формирования (дд.ММ.гггг чч:мм), у каждой записи справа подпись состояния («ЕЩЁ НЕ ПРОСЛЕДОВАНО» / «В ЗОНЕ» / «БЕЗ УЧАСТКА»; «ПРОСЛЕДОВАНО» не подписывается), у записи без данных проследования (голова не заходила) вместо «Заход головой / Выход составом / Скорость» одна строка «Нет данных о проследовании» (карточка короче); у записи с данными пишутся все три строки, максимум скорости в зоне помечен «(пока)», в итоге «Ещё не проследовано: N (данные могут измениться)». Логика - `RestrictionReport.canGenerate/progress/statusLabel`.

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
  `CustomDatePickerDialog` (§24.2); шторка «Новое
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

**Станция не найдена (решение 07.10.2026, уточнено 07.10).** Если у станции нет координат и автоопределение их не нашло, об этом
сообщается **только в карточке самой станции** (блок «Координаты», шторка/экран станции): «Станция «X» не найдена на карте.
Напишите в техническую поддержку — мы добавим её.» и кнопка «Написать в поддержку». Кнопка открывает почту с заполненными
полями: адрес `locodriver.app@yandex.ru`, тема «Добавьте станцию: X», текст — «Здравствуйте!», название станции, плечо
(отправление → прибытие), регион/дорога, если известны, просьба добавить станцию на карту. Нет почтового приложения — тот же тост,
что выше. **На экране плеча (схема станций, «Измерить по карте», автоподбор, «Построить другой маршрут») кнопки и подписи
«Написать в поддержку» нет** — экран не нагружаем: там остаётся короткая пометка «нет координат» у станции и общее сообщение
об ошибке; открыв станцию, пользователь попадает в её карточку с кнопкой письма.

## 24. Общий контракт шторок, диалогов, пикеров и transient UI

Этот раздел обязателен для iOS/PWA: перечисленные компоненты не всегда являются
отдельными nav-destination, но представляют самостоятельные пользовательские
состояния. Нативный внешний вид платформы допустим; содержимое, результат и
правила закрытия должны совпадать.

### 24.1. Базовые контейнеры

- **Жесты шторок (решение 07.10.2026):** закрыть шторку свайпом можно только за ручку/шапку; жест по содержимому (список, форма) только прокручивает его, остаток жеста шторку не тянет. Правило действует для всех шторок: «Плечи», «Время», «Станция в справочнике», «Пассажирские 12 ч», станция плеча, автоподбор, пикеры. *Android:* `Modifier.separateSheetFromScroll()` стоит на прокручиваемом содержимом шторок: «Плечи», станция плеча, автоподбор, пикеры станции/серии, «Время» (`TimeBottomSheet`), станция справочника и её редактор (`StationEditBottomSheet`, оба), «Пассажирские 12 ч», `RouteLegendSheet`, шторки настроек (`SettingsUnifiedUi`), шторки формы «Поезд» (с прокруткой). Шторки без прокрутки внутри (короткие формы) штатные; `RouteQuickViewSheet` имеет собственную схему.

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

## 26. Инструмент «Справка ВУ-45» (BrakeCertificateScreen)

Источник норм — «Правила технического обслуживания тормозного оборудования и управления тормозами железнодорожного подвижного состава железных дорог» (редакция 2025 г., АО «ВНИИЖТ», взамен Правил СЖТ 2014 г., протокол № 60; реквизиты утверждения в тексте не заполнены): нормы — прил. 2 (табл. 1-4, п. 1-7), форма справки — прил. 1, опробование и плотность — разд. 9 (табл. 4 п. 9.3.3), порядок заполнения граф — Инструкция 808-2022 ПКБ ЦВ, прил. М; Правила 2014 г. (нумерация III.1-III.4, IV.1) и Инструкция 277 не применяются (соответствие пунктов — `docs/VU45_INSTRUCTION_ACTUALITY.md`). Предметная часть (формулы, таблицы, источники) — `docs/VU45_SPEC.md`; хранение и синхронизация — `docs/VU45_STORAGE_DESIGN.md` (реализовано, отправка на сервер за флагом, см. 26.10).
ВУ-45 — справка об обеспечении поезда тормозами и исправном их действии (не расход топлива). Экран — **бланк-калькулятор**: данные живут во
`ViewModel` как одно сериализуемое состояние (`BrakeCertificateUiState` → `toData()` → `BrakeCertificateData`) и сохраняются локально (26.10). Запоминаются (SharedPreferences `brake_certificate_prefs`): норма ручных тормозов (`manual_norm`, по умолчанию 0,6), автоблокировка, композиция (по умолчанию К-100).
Видна всем (без Pro-гейта). Только Android; на iOS `Router.showBrakeCertificate` — no-op по умолчанию. Год формы нигде в UI не указывается; отметки «Принял» нет.
Дизайн: экраны Claude Design (реестр из 20 состояний), цвета из токенов приложения (`vuColors()`: primary = текст, tertiary = акцент, secondary = поверхность,
surfaceBright = bgSubtle, surfaceTint = success, error = danger) в обеих темах.

**26.1. Вход.** (а) Карточка «ВУ-45» в «Инструментах» Главного (§4 п.8), третья по счёту: после «Карта», перед «Статистика»: справка открывается ПУСТОЙ (без аргументов и автоподстановки); данные подставляет кнопка «Заполнить из маршрута» (§26.8у2). (б) Кнопка «ВУ-45» нижней панели формы «Поезд» (§6.2 п.9): текущий поезд формы и последний
локомотив маршрута формы. Передаётся `BrakePrefill(weight, axles, trainNumber, locoSeries, locoNumber, routeId, trainId)` query-аргументами `BrakeCertificateRoute`.
При входе поля заполняются из аргументов, затем (если есть `routeId`) один раз вызывается «Заполнить из маршрута» (§26.3). Серия локомотива сопоставляется со справочником
справочником серий пользователя (§17.4: вес, оси, нажатие, объём ГР) и каталогом (`BrakeLocoCatalog`, прежняя Инструкция 277; в Правилах - п. 6.3 прил. 2: данные утверждает владелец инфраструктуры) — вес/оси/нажатие локомотива и свой объём ГР подставляются и правятся вручную.
**Приоритет подстановки** (`LocoSeriesDefaults.passportFor`, по каждому полю отдельно): запись `LocomotiveSeries` пользователя → каталог (вес, оси, нажатие, подтверждённый объём ГР; в справке и справочные вес/оси ЭС-серий — с пометкой источника) → пусто. Подставленный объём ГР попадает в «Свой объём ГР» (группа табл. 4 п. 9.3.3 определяется по нему, прим. 3), пока группа/объём не выбраны вручную; при смене серии заменяется (`volumeAuto`).
**Запись обратно:** если пользователь изменил вес, оси, нажатие или свой объём ГР, через 0,8 с после последнего ввода значение записывается в справочник серий пользователя (`LocoSeriesDefaults.recordWithEdits`): запись по названию серии (без учёта регистра) перезаписывается только изменёнными полями (очищенное поле → «не задано»), записи нет — создаётся (тип тяги по названию, остальные поля — по умолчанию). Если введённое совпало с тем, что и так подставляется (нет записи) или с записью — ничего не пишется. Выбор строки группы ГР вручную в справочник не пишется. В следующей справке подставятся уже эти значения.

**26.2. Каркас.** Топ-бар «Справка ВУ-45»; сегменты «Бланк» | «Расчёт» | «Замеры» | «Справочник» (h48, контейнер bgSubtle r16, активный — accent/accentInk; вкладка и
режим замера переживают пересоздание Activity). Прокручиваемые вкладки имеют `imePadding()`. Приложение фиксировано в портрете.

**26.3. «Расчёт».** Один экран без прокрутки: пикер «Категория поезда» (название · норма); в ряд «Вес состава, тс» и «Всего осей»; в ряд «Нажатие на ось, тс» (среднее) и пикер
«Ручных на 100 тс» (значение «Авто · 0,8» или ручное; для категорий без нормы поля нет; под рядом строка-источник, например «По табл. 4 прил. 2 для уклона 0,010 - 0,8»; пункт «Авто (по уклону)» — по умолчанию, ручные 0,4…1,8 — переопределение, запоминается в SharedPreferences, от уклона не зависит, возврат на «Авто» снова следует за уклоном); «Оси по типам вагонов ›» (переход в «Бланк»); «Руководящий спуск, ‰» — прокручиваемые чипы по ступеням табл. 4 прил. 2 «≤ 6 | 8 | 10 | 12 | 14 | 16 | 18 | 20» и «АБ» (если у категории есть значение в скобках и спуск не круче 0,010). Один выбор определяет и ручные оси (табл. 4 прил. 2: 0,4 / 0,6 / 0,8 / 1,0 / 1,2 / 1,4 / 1,6 / 1,8 на 100 тс; «Авто» = max(0,6; табл. 4 прил. 2) — единое наименьшее по п. 7.2 прил. 2, значения мгновенно пересчитываются на «Расчёте» и в «Бланке»), и класс для скоростей табл. 1/2 прил. 2: ≤ 6 → «до 0,006», 8 и 10 → «круче 0,006 до 0,010», 12 и 14 → «круче 0,010 до 0,015» (верхняя граница ступени, безопасная сторона); 16-20 — норм скорости нет («скорость устанавливает начальник дороги»). Далее **одна кнопка «Заполнить из маршрута»** (видна на «Расчёте» и в «Бланке»): одним нажатием подставляет из раздела «Поезд»
(вес, число осей, номер поезда) и из раздела «Локомотив» (серия и номер) маршрута, из которого пришли; пустые и нулевые значения игнорируются (`BrakePrefill.sanitized()`);
под кнопкой строка итога «Подставлено: вес, оси, поезд №, локомотив. Не нашлось: …» либо «Экран открыт без маршрута - данные поезда и локомотива можно ввести вручную.».
Затем карточка результата: «ДОПУСКАЕМАЯ СКОРОСТЬ» (48sp mono; accent, danger при снижении), «Потребное, тс | Фактическое, тс | Ручных осей», вердикт («Нажатие достаточно: запас …» /
«Нажатие недостаточно: не хватает … тс (… на 100 тс при норме …)»), пояснение источника правила, предупреждения, замеры «Плотность II: … с, IV: … с»; при нехватке — красный блок
«Что делать (п. 4.4 прил. 2)» (3 пункта) с «?». При открытой клавиатуре карточка результата заменяется закреплённой строкой скорости (h56).

**26.4. «Бланк».** Сверху закреплённая строка результата (h56: «80 км/ч | Нажатие достаточно» или «— | Не хватает X тс»). Переключатель «Лицевая | Оборот» (только для не-пассажирских
категорий). Кнопка «Заполнить из маршрута». **Лист-бланк** (surface, рамка 1 borderStrong, r6): «чернила» — вписанное mono 17/600 accent на линейке, расчётное mono 17/700 text.
Шапка: «Штемпель станции», «Форма ВУ-45 | № —», заголовок «СПРАВКА О ТОРМОЗАХ»; реквизиты (дата, локомотив серия №, поезд №, штемпель станции, время выдачи ч/м) свёрнуты в строку
с пунктирными линиями («Заполнить ▾» / «Показать ▾», краткая сводка введённого), раскрываются на месте. Строки: «весом ___ тс. Всего осей ___», «Требуется нажатие тормозных колодок
в тс. ___» (расчётное), «ручных тормозов в осях ___» (расчётное; «все» для пассажирских). **Таблица в 4 колонки**: «Тормозное нажатие на ось, тс» (напечатаны все 26 строк по образцу формы (прил. 1 Правил ТО тормозного оборудования; строки 20,0, как в Инструкции 808-2022, в форме нет): 1,25 / 3 / 3,5 / 4,5 / 5 / 5,5 / 6 / 6,5 / 7 / 7,5 / 8 / 8,5 / 9 / 9,5 / 10 / 11 / 11,5 / 12 / 12,5 / 13 / 14 / 15 / 16 / 18 / 19 / 21; остальные значения, в т. ч. 20, - «+ Другое нажатие») | «Количество осей» (ввод цифрами в строке) | «Нажатие колодок, тс» (расчётное) | «Другие данные» (одна высокая графа справа на все строки: «К 100»,
«Торм. ЭПТ/П», «ТЦПВ …», «Встр.», «ДПВ», «ВО2ХВ», «Напр.», «Пит.»; «+ Заполнить/Изменить» открывает шторку «Другие данные»); строка с введённым числом осей подсвечена accentSoft;
итог «Всего» (при нехватке нажатия сумма красным); «+ Другое нажатие» (1,25…21 тс) — шторка «Нажатие на ось» (чипы значений, поле «Количество осей», кнопка «Добавить · N × p = X тс»).
В режиме «одно среднее нажатие» («Расчёт») таблица показывает одну строку из среднего нажатия × «Всего осей»; ввод осей в строку переводит в режим таблицы. Ниже: «Наличие ручных
тормозных осей» (с вердиктом против потребных), «Плотность тормозной сети поезда» (значения «II … с · IV … с» из «Замеров», тап — на «Замеры»), «Хвостовой вагон №», «Подпись» (текстовое поле «фамилия осмотрщика», до 30 символов;
хранится в `BrakeCertificateData.inspectorName`, необязательное — старые справки читаются без него). «?» у граф бланка → шторка пояснения. Вне листа — карточка «Учитывать локомотив» (переключатель; вес/оси/нажатие по справочнику серий, правятся).
Шторка «Другие данные»: «обведённый» выбор из напечатанных вариантов (кольцо 2dp accent): К-50/К-75/К-100, Г/ЭПТ/ЭПП/ДТ/МРТ/П/В10/РИЦ/5 ХВ/СТРП; «?» у блоков и каждого поля; поля ТЦПВ мм, Встр., ДПВ кгс/см² (с оценкой по п. 9.3.3),
ВО2ХВ с, напряжение в хвосте В, питательная сеть.

**26.5. Оборот (опробования в пути).** Только для грузовых поездов всех категорий (для пассажирского оборот скрыт). Стоянки определяются по станциям раздела «Поезд» маршрута:
`TurnaroundPlanner.candidates(stations, now)` — станции с временем прибытия, не проходные и не конечные, стоянка = (отправление или «сейчас») − прибытие; > 5 мин — запись с полями
«Станция или место опробования» (название как в разделе «Поезд»), «Вид опробования» (по умолчанию пусто: опробования не было, только замер плотности; «ТП» - полное, «СП» - сокращённое; старые записи читаются как раньше), «Плотность»; графы оборота (форма - прил. 1 Правил, заполнение - 808-2022 ПКБ ЦВ, прил. М): 3-6 (вес поезда, всего осей, нажатие требуемое и фактическое) заполняются только при изменении состава и веса, иначе прочерк; графа 7 - подпись (табельный №) и время опробования (для любой записи). Времена прибытия/отправления станций заполняет режим «Автозапись»
(детектор остановок по GPS, §6.2 п.6а) или машинист вручную — отдельного источника не вводится. Записи создаются автоматически при «Заполнить из маршрута», входе в справку и при открытии вкладки «Оборот» (`refreshStops`; идущая стоянка пересчитывается по текущему времени; кнопки «Обновить по стоянкам» нет); вручную введённые данные и ручные записи сохраняются. Над таблицей поясняющего текста нет. Таблица оборота (4 столбца): «Станция / место» (под названием мелко «> 5/> 30 мин · ЧЧ:ММ–ЧЧ:ММ») | «Вид» (ТП/СП/пусто) | «Плотность» (секунды, рядом отклонение от эталона «−12 %», под ним «✓ в норме» / «! более 20 %» / «нет эталона») | «Время» (время опробования/замера ЧЧ:ММ). Если состав менялся - вторая строка на всю ширину «Новый вес N т · Потребное X тс · Фактическое Y тс» (число осей не выводится; если не менялся - строки нет). Подпись (таб. №) и «время готовности» в таблицу не выводятся, правятся в шторке. Пустое состояние: «Записей нет» и «+ Добавить отметку». Тап по записи — шторка «Отметка об опробовании»: выбор класса стоянки, станция (поле с подсказками: под полем список «станции этого поезда в порядке следования, затем остальные станции пользователя (список станций из настроек + справочник станций) по алфавиту», без дубликатов по нормализованному названию; список фильтруется по набранному без учёта регистра, можно выбрать или ввести своё; `BrakeStations`), «Плотность, с» (ввод или «Замерить секундомером»: открывает «Замеры → В пути», результат после «Стоп» записывается в эту отметку), вид опробования («—» / «ТП» / «СП»), оценка плотности и допуск «X–Y с», блок изменения состава (см. 26.8ф), время и подпись (таб. №) (время - формат «ЧЧ:ММ», единое поле `VuTimeField` для всех времён ВУ-45 (время опробования на обороте и «Время выдачи» на лицевой; двух полей «ч.»/«м.» больше нет): цифровая клавиатура, жёсткая маска `TimeOfDayInput.mask` посимвольно - буквы и знаки не проходят, первая цифра часов 0-2, при первой 2 вторая 0-3, десятки минут 0-5, не более 4 цифр, двоеточие подставляется само, курсор всегда в конце; в запись уходит только пустое или полное корректное значение, неполное подсвечивается; старые значения «1245» показываются как «12:45»),
«Удалить», «Готово». Кнопка «+ Добавить отметку» создаёт ручную запись (станция — последняя станция поезда, класс > 5 мин). Автосоздание по GPS без станций раздела «Поезд» не делается.

**26.6. «Замеры».** Селектор «II | IV | В пути» (у пассажирских только «II» и «В пути»; на кнопке — «· X с» или «· идёт»). Карточка замера: заголовок, пояснение, «ЗАМЕР ИДЁТ · ЭКРАН НЕ ГАСНЕТ»,
таймер `мм:сс.д` (72sp mono), одна кнопка 64dp «Старт» → «Стоп» → «Заново» + «Сбросить»; после остановки — «Записано: X с — норма/превышена/оценка недоступна», норма и «перенесено в бланк»,
поля «ГР в начале/в конце» (кгс/см²) либо «Падение давления в ТМ» (пассажирский II). Под карточкой: «Зарядное давление ТМ», «Локомотивов (СМЕ)», «Группа локомотива (табл. 4 п. 9.3.3)» (по умолчанию «Не выбрана»;
подставляется только при однозначном совпадении серии), строка нормы II. Правила оценки: II (грузовой) — время ≥ нормы табл. 4 п. 9.3.3 (× поправка на зарядное давление 5,3-5,6 → 0,9 (прим. 1 к табл. 4 п. 9.3.3),
× число локомотивов; > 480 осей — нормы нет); группа не выбрана — «Оценка недоступна: выберите группу локомотива» нейтральным цветом; IV — время ≥ 0,9 × II; «В пути» — ±20 % от II; пассажирский —
после 20 с выдержки падение ≤ 0,2 кгс/см² за 1 мин (п. 9.3.5; вариант «0,5 за 2,5 мин» из Правил 2014 г. не применяется). Время — по `SystemClock.elapsedRealtime` во `ViewModel` (переживает смену конфигурации и сворачивание); экран не гаснет
(`keepScreenOn`), пока идёт замер. Результат «В пути» пишется в оборот кнопкой «Записать» (см. 26.8с).

**26.7. «Справочник».** Заголовок «Справочник · Правила ТО тормозного оборудования»; 8 раскрывающихся карточек (номер, название, шеврон; открыта одна): нормы и скорость; недостаток нажатия; ручные тормоза; плотность II/IV
(плюс плашка «Ваш поезд: группа N, M осей ≥ X с» и «Открыть замер ›»); полное и сокращённое опробование; проверка действия тормозов в пути; давление в хвосте. Тексты — по Правил ТО тормозного оборудования
(п. 4.2-4.4, 5.1, 6.4, 6.6, 7.1-7.3 прил. 2, табл. 1-4 прил. 2; п. 9.3.1-9.3.5, 9.5.1-9.5.3, 9.6, 10.1.19, 10.2.8-10.2.12, табл. 4 п. 9.3.3 основного текста). Внизу оговорка: справочный инструмент, официальной справкой остаётся документ осмотрщика; нормы могут меняться.

**26.8. Шторка «?».** Название графы, «Что это», «Ваш расчёт» (для «Требуемое нажатие» и «Ручных осей»), «Источник» (пункт и полное название Инструкции). Строки «Кто заполняет» нет: справку всегда заполняет вагонник (осмотрщик). Тексты — `brakeHints` в `BrakeCertificateHelp.kt`.

**26.8а. Числовые поля.** Все дробные поля (вес, нажатие на ось в т.ч. в строках и «Другое нажатие», давления ГР/ТМ/ДПВ, ТЦПВ, напряжение, время и плотность в секундах) принимают запятую и точку: `sanitizeDecimalInput` оставляет цифры и один разделитель (показывается запятой; «8,» и «,5» не сбрасываются), разбор — `toFiniteDoubleOrNull`, пустое/некорректное значение расчёт игнорирует; клавиатура Decimal. Целые по смыслу (оси, ручные оси, СМЕ, табельный №) — `sanitizeIntegerInput`. Результаты показываются без потери значащих дробных разрядов (до 2 знаков, без хвостовых нулей).

**26.8б. Формулировки и ссылки.** Норма нажатия везде — «тс на 100 тс веса» (источник: «на каждые 100 тс веса поезда (состава)», табл. 1 прил. 2 и п. 4.4 прил. 2 Правил ТО тормозного оборудования). Любая ссылка на пункт/таблицу называет документ: «п. 4.4 прил. 2 Правил ТО тормозного оборудования»; приложение пишется явно («прил. 2» - нормы, без него - основной текст: основной текст и прил. 2 нумеруются независимо, табл. 4 п. 9.3.3 - время падения давления в ГР, табл. 4 прил. 2 - ручные тормоза); полное название — «Правила технического обслуживания тормозного оборудования и управления тормозами железнодорожного подвижного состава железных дорог» (редакция 2025 г., АО «ВНИИЖТ») — в подсказках («Источник») и справочнике (константы `InstructionRef.FULL`/`SHORT`/`GENITIVE`/`DENSITY_TABLE`; `InstructionRef.annotate` дописывает название после первой ссылки в строке).

**26.8в. Скорость при нехватке нажатия.** Закреплённая строка результата (под переключателем вкладок «Расчёт» и в «Бланке») видна всегда и обновляется по мере ввода (в т. ч. при клавиатуре). Три ветки: (1) нажатие ≥ нормы табл. 1 прил. 2 — скорость табл. 1 прил. 2; (2) ниже нормы, но не ниже минимума табл. 2 прил. 2 — снижение по п. 4.4 прил. 2 с показом шагов в карточке «Расчёт скорости» («Не хватает N тс на каждые 100 тс (норма − факт) → M т (с округлением вверх); скорость = X − k×M = Y → до кратного 5 вниз: Z км/ч (п. 4.4 прил. 2 Инструкции …)») и условия применимости (п. 4.2-4.3 прил. 2 Инструкции …; только для случаев выключенных неисправных автотормозов в пути и особых составов); (3) ниже минимума табл. 2 прил. 2 — крупное красное «Нажатие ниже допускаемого минимума» в карточке и в закреплённой строке: «скорость по таблицам не определяется: в исключительных случаях допускаемые скорости устанавливает начальник железной дороги по номограммам, на 20 % ниже определённых по ним (п. 5.1 прил. 2 Инструкции …)». Нет нормы в таблице (категории без минимума в табл. 2 прил. 2 — например порожний 350-400 осей — и спуск круче 0,010 там, где в табл. 1 прил. 2 прочерк; спуск круче 0,015) — «скорость по таблицам не определяется», устанавливает начальник дороги (табл. 1 прил. 2, 2, п. 5.2 прил. 2 Инструкции …). Инструкция не содержит прямого запрета движения — формулировка источника сохранена дословно по смыслу.

**26.8г. Числовые поля и ввод.** Источник истины поля — строка ввода; число — производная (`toFiniteDoubleOrNull`); «8,» не превращается в «8». «+ Другое нажатие» имеет собственное поле «Нажатие на ось, тс» (любое дробное) и быстрые чипы. Поля — системная клавиатура (`KeyboardType.Decimal`/`Number`), как на остальных экранах приложения (форма «Локомотив» и др.: поле + `KeyboardOptions` + очистка `sanitizeNumericInput`); с физической клавиатуры точка и запятая принимаются и показываются запятой.

**26.8д. Короткие красные блоки и сверка осей.** При нехватке нажатия: «между минимумом и нормой» — одна строка «Нажатие ниже нормы: скорость снижена до N км/ч, п. 4.4 прил. 2 Правил ТО тормозного оборудования» + «Подробнее ▾» (шаги расчёта и условия п. 4.2-4.3 прил. 2); «ниже минимума» — «Нажатие ниже минимального (табл. 2 прил. 2 Правил ТО тормозного оборудования). Скорость по таблицам не определена» (прямого запрета отправления/следования в Инструкции нет: п. 8.1 основного текста требует обеспечить нормы, п. 4.3 и 5.1 прил. 2 допускают исключения; формулировка «не допускается» не используется) + «Подробнее» (п. 5.1 прил. 2); блок «Что делать при нехватке нажатия» свёрнут, раскрывается по «Подробнее». Сверка осей (режим «Оси по типам вагонов» на «Расчёте» и «Бланке»): сумма осей по строкам таблицы сравнивается с «Всего осей» (оси локомотива не входят — «Всего осей» относится к составу); расхождение — строка предупреждения цветом warning «В справке указано осей: N, текущие данные: M - не хватает K. Расчёт неполный» / «... - больше на K. Проверьте данные на лицевой стороне справки»; «Всего осей» пусто — кнопка «Взять из таблицы: N»; совпало — «✓ осей сходится: N». Строки таблицы бланка — высота 40 dp, значения mono 15-16 sp; колонки 0,22/0,22/0,22 и «Другие данные» 0,34 ширины.

**26.8е. Цвет вердикта (красный / жёлтый), автоблокировка, композит, вес.** Уровень вердикта считает `BrakeStatusResolver` (domain): `OK` - нажатие не ниже нормы (зелёный/нейтральный); `WARNING` - нажатия не хватает, но скорость табл. 1 прил. 2 не снижается (п. 3.4 прил. 2: груженый грузовой, композитные колодки К-75 при >=31 / К-100 при >=30 тс на 100 тс веса) - ЖЁЛТЫЙ блок (токен `warning` темы, текст цвета основного текста), в закреплённой строке «Не хватает X тс на 100 тс веса; скорость не снижена (п. 3.4 прил. 2 Правил ТО тормозного оборудования)», в карточке - жёлтый блок с «Подробнее»; `DANGER` - скорость снижена (п. 4.4 прил. 2), нажатие ниже минимума (табл. 2 прил. 2) или скорость по таблицам не определена - КРАСНЫЙ. Блок «Что делать при нехватке нажатия» показывается только при `DANGER`; красная рамка поля «Нажатие на ось» - только при `DANGER`. Если п. 3.4 прил. 2 даёт скорость ниже табл. 1 прил. 2 (80 при 90), это `DANGER`.
«Автоблокировка» - ОТДЕЛЬНЫЙ переключатель (Switch справа, карточка под рядом спуска и композитом; подсказка «?» `autoblock` с расшифровкой: автоблокировка с 3-значной сигнализацией, зелёный огонь АЛСН, п. 3.1 прил. 2; текста-пояснения под переключателем нет). По умолчанию ВЫКЛЮЧЕН (основная скорость табл. 1 прил. 2), выбор запоминается (SharedPreferences `brake_certificate_prefs`/`autoblock3`) и восстанавливается после перезапуска. Влияет на скорость только при спуске до 0,010 и там, где в табл. 1 прил. 2 есть значение в скобках (груженый грузовой 80 -> 90); в остальных случаях переключатель остаётся, но приглушён (alpha 0,5) и результат не меняет; смена ступени спуска состояние переключателя не сбрасывает. Ряд «Композиционные колодки» (Нет/К-50/К-75/К-100, подсказка «?» `composite`) дублируется на вкладке «Расчёт» и в «Бланк» - «Другие данные»: одно состояние `compositeShare`. Вес состава подписан «Вес состава, т» (суффикс «т»; в бланке «весом … т.»); остальные величины остаются в «тс».

**26.8ж. Сверка с действующими Правилами (числа).** К-50 даёт «скорость не снижена» при нажатии не менее 32 тс на 100 тс веса (К-75 - 31, К-100 - 30); плотность - колонка «481-530 осей» (11/13/15/20/22/28/33 с для групп 1-7), больше 530 осей нормы нет; давление в хвосте грузового поезда не должно отличаться от зарядного в голове более чем на 0,3 (до 300 осей) / 0,5 (300-400) / 0,7 кгс/см2 (более 400) - норма ДПВ = зарядное минус отклонение; в таблице бланка строки нажатия 7,5 и 9,5 тс на ось (26 строк по образцу формы 2025 г.; строка 20,0 из Инструкции 808-2022 убрана); в «Другие данные» добавлена отметка «Г»; выход штока 180-230 мм - эффективность тормоза 2/3 (не 70 %). Подробности и цитаты - `docs/VU45_INSTRUCTION_ACTUALITY.md`.

**26.8з. Группы плотности по объёму ГР, параметры плотности на «Расчёте», пустое состояние, К-100 по умолчанию.** Группа локомотива - строка табл. 4 п. 9.3.3 по общему объёму главных резервуаров (1000, 1200, 1500, 1800, 2000, 2500, 3000 л); в пикере и в шторке «?» у каждой группы - только названия серий одной строкой (без объёмов, пометок и источников; для 3000 л строки серий нет; ВЛ10 не показывается - объём зависит от номера локомотива). Источники и пометки о достоверности - в docs/VU45_SPEC.md. Серия из маршрута: если есть в примерах с однозначным объёмом (ВЛ80, ВЛ60, ВЛ11, ВЛ10У, ЭП1, ЧС7, ТЭП70, ТЭМ2, ЧМЭ3, 2ТЭ116, ВЛ85 ...) - группа ставится автоматически (в поле «· по серии»), иначе «Не выбрана»; ВЛ10 и ЧС2 автоподбора не имеют. Карточка «Плотность тормозной сети» (зарядное давление, локомотивов (СМЕ), группа) - на «Расчёте» для непассажирских поездов (из «Замеров» убрана, там ); зарядное давление дублируется в «Другие данные». Поправка «-20 % при 5,6-5,8» удалена: остаётся только -10 % при 5,3-5,6 кгс/см2 (прим. 1 к табл. 4 п. 9.3.3). Пока нет веса, нажатия или осей: нижняя карточка результата на «Расчёте» не показывается, закреплённая плашка (на «Расчёте» и «Бланке») - одна строка «Расчёт скорости будет выполнен после ввода веса, осей и нажатия на ось» нейтральным цветом, без прочерка и «км/ч». Доля композиционных колодок по умолчанию К-100, выбор запоминается (SharedPreferences `brake_certificate_prefs`/`composite_share`), смена категории его не сбрасывает. Требуемое нажатие округляется ВВЕРХ до целого (808-2022, прил. М, п. 8). В «Другие данные» у каждого поля и у блоков «Композиционные колодки» и «Тормоза в составе» есть «?» (допустимые значения, смысл отметок).

**26.8и. Группа ГР: серия и номер, свой объём, «Заполнить из маршрута».** В пикере групп в UI только названия серий: 3000 л - «3ЭС4К, 3ЭС5К»; ВЛ10 показывается как «ВЛ10 (№ 1-18)» в группе 1800 л и «ВЛ10 (№ 19+)» в группе 1500 л. Автовыбор ВЛ10 идёт по НОМЕРУ локомотива из маршрута (`BrakeDensity.groupForLoco`): номер до буквы секции («1234А» -> 1234; допускается «ВЛ10-1234», «№ 0012»), № 1-18 -> 1800 л (объём 1960 л, прим. 3), № 19 и выше -> 1500 л; номер пуст или не разобран (0, вне 1..9999, не число) -> «Не выбрана» без догадок; ВЛ10У -> 1500 л. Пункт пикера «Свой объём, л» открывает шторку с полем (дробные допустимы, запятая или точка) и подписью «Свой объём 1700 л → группа 1500 л» (ближайший наименьший объём из строк табл. 4 п. 9.3.3; больше 3000 л - строка 3000; меньше 1000 л - «меньше наименьшей строки таблицы: норма Правилами не задана, оценка недоступна»); значение хранится в `BrakeCertificateData.customVolume` (необязательное поле, старые справки читаются), имеет приоритет над выбранной группой и автоподбором; выбор группы из списка его сбрасывает. «Заполнить из маршрута» берёт АКТУАЛЬНЫЕ данные маршрута (последний локомотив и выбранный поезд) заново при каждом нажатии: серия и номер локомотива всегда заменяются (итог «Подставлено: серия …, локомотив № …, группа …»), группа определяется заново по новой серии и номеру; вес, оси, № поезда заменяются, если пусты, совпадают, равны прошлой автоподстановке (не правились) или по кнопке «Заменить введённое данными маршрута»; группа, выбранная вручную, и свой объём остаются (в итоге «Не заменено (уже введено): группа (выбрана вручную: …)»), заменяются только по «Заменить…».

**26.8к. Ripple и шторки.** Шторки приложения - СТАНДАРТНЫЕ `ModalBottomSheet` Material 3 (1.4.0) с маркером по умолчанию; своей обёртки и своего маркера нет. Штатного способа убрать нажатие маркера нет: контейнер маркера в самом `ModalBottomSheet` обёрнут в кликабельный `Box` (раскрыть/скрыть шторку для доступности), поэтому подсказка «Drag handle» и подсветка остаются даже при своём компоненте в слоте `dragHandle`; убрать их можно только обёрткой на весь проект, от неё отказались. Ripple чипов и кнопок ВУ-45 (`VuChip`, ступени уклона, «Нет/К-50/…», чипы значений нажатия, обведённые отметки, вкладки, кнопки, «?», выбор-поле) - скруглённый по форме элемента (`clip(shape)` до `clickable`); эти компоненты используются только на экране ВУ-45.

**26.8л. Порядок «Расчёта» и нормы II/IV.** Сверху вниз: закреплённая плашка скорости; кнопка «Заполнить из маршрута» (вверху, чтобы не прокручивать; итог подстановки НЕ показывается - только короткая ошибка при отсутствии маршрута/«Маршрут не найден» и, при расхождении с введённым, кнопка «Заменить введённое данными маршрута»); категория поезда; вес | оси; нажатие на ось | ручные на 100 тс веса; «Оси по типам вагонов ›»; (для непассажирских) зарядное давление | локомотивов (СМЕ) и группа локомотива (объём ГР) на всю ширину - в стиле «Вес состава» (подпись + «?», без карточки и заголовка), под ними две строки «Норма плотности во II положении: N сек» и «Норма плотности в IV положении: не менее M сек» (вместо N/M - «укажите группу» / «укажите число осей», вне таблицы - «число осей вне таблицы (1-530)»); композиционные колодки; руководящий спуск; автоблокировка; карточка результата (только после расчёта). Пояснительных абзацев между полями нет: источник значения «Ручных на 100 тс веса» (табл. 4 прил. 2, п. 7.2 прил. 2) - только в «?». Норма IV = 0,9 от нормы II (плотность в IV положении не должна отличаться от поездного более чем на 10 % в сторону уменьшения - п. 9.3.3 Правил; для состава до 100 осей включительно IV не проверяется); норма II показывается как раньше («не менее», вверх до 0,1 с, без лишнего «,0»), а порог IV округляется до ЦЕЛОГО в БОЛЬШУЮ сторону («не менее» - безопасная сторона): II = 69 → 0,9 × 69 = 62,1 → «не менее 63 сек», II = 70 → 63, II = 62,1 → 56 (вещественная погрешность не даёт лишней секунды). На «Замерах» IV сравнивается с ЗАМЕРЕННЫМ II (порог = 0,9 от замера, целое вверх; в тексте оценки целое), при отсутствии замера показывается норма IV по норме II. Шторка «Свой объём ГР»: строка «1450 л - округляем до 1200 л» (равно строке - «1500 л - группа 1500 л»; больше 3000 - «округляем до 3000 л»; меньше 1000 - норма не задана) и те же две строки норм по введённому объёму.

**26.8м. Раскладка «Расчёта» (актуальная; заменяет порядок в 26.8л), «Заполнить из маршрута», «Нажатие на ось», нормы.** Сверху вниз: плашка скорости; кнопка «Заполнить из маршрута»; [Категория поезда | Зарядное давление] (категория - только сжатое название без нормы N, полные названия и норма - в шторке и «?»; у пассажирских поездов зарядного давления нет, категория на всю ширину); [Вес состава, т | Всего осей]; [Нажатие на ось, тс | Ручные на 100 тс веса]; «Разное нажатие на вагонах ›»; (для непассажирских) [Кол-во локомотивов | Объём ГР] (поле «Объём ГР» - бывшая «Группа локомотива»: выбор строки табл. 4 п. 9.3.3 по объёму главных резервуаров или «Свой объём, л»; в «?» пояснение про СМЕ для «Кол-во локомотивов»); две строки норм; композиция; руководящий спуск; автоблокировка; карточка результата. Строки норм - «Норма плотности во II-м: не менее N сек» и «Норма плотности в IV-м: не менее M сек» (норма II - «наименьшее допустимое время», т.е. «не менее»; слов «положении» нет) - пояснительный текст, не поле: 12 sp, приглушённый цвет (`muted`), число с «сек» моно-шрифтом акцентным цветом (`accent`), слева тонкий значок «i» в кружке (`faint`), без «?», рамки и карточки (`VuNormLine`); то же на вкладке «Замеры» и в шторке «Свой объём». «Заполнить из маршрута» НЕ требует подтверждения и не выводит итог: нажатие сразу заменяет вес, оси, № поезда, серию и номер локомотива актуальными данными маршрута (в т.ч. введёнными вручную); пустые и нулевые поля маршрута введённое не затирают; Объём ГР определяется заново по серии (ВЛ10 - по номеру), ручная группа и свой объём сбрасываются ТОЛЬКО если маршрут содержит серию, по которой группа определяется (иначе группа не меняется); при открытии экрана подставляются только пустые поля; ошибка - короткий красный текст («Маршрут не найден», «Нет данных маршрута»). Поле «Нажатие на ось» связано с таблицей справки: пустая таблица - обычное поле (то, что введено); одна строка или все заполненные строки с одним значением - значение показывается в поле, правка меняет все эти строки (число осей не меняется); две и более разных значения - в поле «см. в справке», ввод недоступен, тап открывает «Бланк»; состояние едино на «Расчёте» и «Бланке» (`PressureField`). Зелёной строки «осей сходится» нет: показывается только предупреждение, когда число осей таблицы не сходится со «Всего осей».

**26.8н. Формат значений и «Замеры».** Поле «Объём ГР»: по серии - «1800 л · авто» (значение, затем «авто»; ВЛ10 - по номеру), вручную - «1800 л», свой объём - «1450 л → 1200 л» (равен строке - «1500 л»; меньше 1000 л - «800 л · нет нормы»), не выбрано - «Не выбрана»; поле «Ручные на 100 тс веса»: «0,6 · авто» по уклону или «0,8» при ручном выборе (в шторке пункт «Авто (по уклону)» без изменений). «Замеры»: поля «ГР в начале/в конце» убраны полностью (в расчётах, бланке и оценке не участвовали; в `DensityMeasurementData` поля `grStart`/`grEnd` остаются необязательными и помечены Deprecated, чтобы старые справки и синхронизация не ломались); остаются секундомер и строки норм II/IV (поясняющего абзаца о зарядном давлении, кол-ве локомотивов и объёме ГР нет; текст внизу вкладки: «Результаты замеров автоматически попадают во вкладку «Бланк». Экран не гаснет, пока идёт замер.»); замер ниже нормы «не менее N сек» (II и IV) оценивается «недостаточно» (красным), в норме - «норма», для «В пути» при отличии от записанного более чем на 20 % - «отличается от записанного более чем на 20 %» (слово «превышена» не используется); для пассажирского поезда падение давления больше допуска - тоже «недостаточно».

**26.8о. Блоки «Расчёта» (актуальная раскладка; заменяет порядок в 26.8л/26.8м).** Поля сгруппированы в карточки (стиль групп «Настроек»: заголовок моно-капслок с разрядкой над карточкой `VuBlock`, фон-карточка со скруглением, поля внутри «утоплены» `sunk`). Порядок: плашка скорости → «Заполнить из маршрута» (вне блоков, сразу под плашкой, чтобы не прокручивать) → «ДАННЫЕ» → «РАСЧЁТ ПЛОТНОСТИ» → «РАСЧЁТ РТ» → «ДОПОЛНИТЕЛЬНО» → карточка результата (после расчёта). «ДАННЫЕ»: «Категория поезда» (сжатое название; на всю ширину), под ней строка «Требуемое нажатие: N тс на 100 т веса» (N - норма категории по табл. 1 прил. 2, стиль `VuNormLine`: «i», 12 sp, приглушённый цвет, число с «тс» акцентным моно), [Вес состава, т | Всего осей], «Нажатие на ось, тс» (логика «см. в справке»/значение/ввод по таблице справки) и «Разное нажатие на вагонах ›». «РАСЧЁТ ПЛОТНОСТИ» (непассажирские): [Зарядное давление | Кол-во локомотивов], «Объём ГР» на всю ширину, две строки норм II/IV (зарядное давление помещено в этот блок, так как влияет только на норму плотности - поправка -10 % при 5,3-5,5 кгс/см2 и норма давления в хвосте). «РАСЧЁТ РТ» (категории с нормой ручных тормозов): «Ручных на 100 т веса» («0,6 · авто») и чипы «Руководящий уклон, ‰» (поле называется «Руководящий уклон»; в цитатах Правил сохранено «руководящий спуск»). «ДОПОЛНИТЕЛЬНО»: композиционные колодки (Нет/К-50/К-75/К-100) и «Автоблокировка» (переключатель). Для пассажирских категорий (и категорий без нормы ручных тормозов) блок «РАСЧЁТ РТ» не показывается, а чипы «Руководящий уклон» (нужны для скорости) переходят в «ДОПОЛНИТЕЛЬНО»; блок плотности скрыт у пассажирских. Внутри блоков поясняющих абзацев нет, «?» у всех полей и блоков сохранены.

**26.8п. Правки раскладки и категории.** «Зарядное давление» - в блоке «ДАННЫЕ», во второй колонке справа от «Нажатие на ось, тс» (для пассажирских категорий зарядного давления нет, «Нажатие на ось» тогда на всю ширину); «РАСЧЁТ ПЛОТНОСТИ» - одна строка [Кол-во локомотивов | Объём ГР] и строки норм II/IV. Неактивные чипы ВУ-45 (ступени уклона, «Нет/К-50/К-75/К-100», значения нажатия, вкладки выбора) - тон `bgSubtle` с рамкой 1 dp `borderStrong` (контрастны с фоном карточки в обеих темах), активный - `accent`. Категория поезда: последний выбранный пользователем вариант запоминается (SharedPreferences `brake_certificate_prefs`/`last_category`) и становится значением по умолчанию для НОВОЙ справки (сохранённая справка поезда хранит свою категорию; автоподбор по номеру поезда не запоминается). «Заполнить из маршрута» подбирает категорию по НОМЕРУ поезда из маршрута (`BrakeCategory.byTrainNumber(number, axles)`) только когда номер определяет её однозначно: 961-970 (грузопассажирские) -> грузопассажирский (44); 9301-9398 (порожние 330-348 осей) -> порожний до 350 осей (55); 9401-9498 (порожние 350-520 осей) -> 350-400 осей (44) или 401-520 осей (33) по числу осей (число осей неизвестно или вне 350..520 - не меняется). Все остальные номера категорию не меняют: пассажирские (подвид по скорости из номера не следует), пригородные, рефрижераторные (1001-1020: 55 или 60 по составу и скорости), соединённые и тяжеловесные, сквозные, участковые, сборные, вывозные, передаточные (груженый или порожний из номера не виден). Форматы номера: «2604», «№ 2604», «2604А», пустой и нечисловой - не определяется. **Пассажирские номера** (диапазоны 859р 1-298, 301-450, 451-598, 601-698): `BrakeCategory.suggestByTrainNumber` определяет группу, но не подвид (скорость/тормоза в номере не закодированы): если текущая категория не пассажирская - выбирается «Пассажирский, до 120 км/ч» с пояснением «уточните скорость и тормоза» (и «Учитывать локомотив» включается, как для пассажирских), если уже пассажирская - не меняется. Однозначные диапазоны (961-970, 9301-9398, 9401-9498 с осями) перекрывают запомненную категорию. Для прочих номеров категория не меняется, под кнопкой «Заполнить из маршрута» пояснение «Номер N: категория по номеру однозначно не определяется - проверьте категорию». Причина прежнего поведения (номер 22): пассажирские номера возвращали «не определено», категория оставалась грузовой, а «Учитывать локомотив» следовал грузовой категории (выключен).

**26.8р. Плашка скорости, «Учитывать локомотив», «Другие данные».** Закреплённая плашка (на «Расчёте» и «Бланке»): «N км/ч | текст» показывается только когда скорость определена (норма, не снижена при нехватке нажатия по п. 3.4 прил. 2, либо снижена до N по п. 4.4 прил. 2 - `BrakeStatusResolver.speedKmh`); когда скорость не определена (нажатие ниже минимума табл. 2 прил. 2, нет норм для категории и уклона) - без «— км/ч» и разделителя, на всю плашку только текст ошибки в цвете уровня (красный); до расчёта - нейтральный текст «Расчёт скорости будет выполнен после ввода веса, осей и нажатия на ось». «Учитывать локомотив» (карточка в «Бланке»): «?» стоит рядом с заголовком, три поля в ряд одной ширины «Вес, т» (вес локомотива в тоннах, суффикс «т»), «Осей», «Нажатие, тс/ось»; переключатель включается автоматически для пассажирских и грузопассажирских категорий и выключается для грузовых (при открытии новой справки - по категории; при «Заполнить из маршрута» - по категории после подбора по номеру поезда; ручное изменение действует до следующего заполнения); значения серии из справочника подставляются как раньше. Основание: п. 6.4 прил. 2 Правил ТО тормозного оборудования - «При подсчете обеспечения тормозами грузовых поездов вес локомотива и его тормозное нажатие не учитываются»; для пассажирских и грузопассажирских учёт следует из п. 8.1-8.2 основного текста и п. 6.3 прил. 2 (владелец инфраструктуры устанавливает «расчетные нажатия … локомотивов и мотор-вагонного подвижного состава, а также учетный вес локомотивов …, число его тормозных осей»); прямого утверждения «в пассажирских учитывается» в Правилах нет, только исключение для грузовых. Графа «Другие данные» таблицы бланка: пары «подпись - значение» размещаются в строку (`FlowRow`), если помещаются по ширине графы и размеру шрифта, иначе переносятся (пара не разрывается; проверено при `font_scale` 1,0 и 1,3).

**26.8с. Запись замера «В пути» в оборот.** «Стоп» только останавливает замер и показывает результат с оценкой (в оборот сам не пишет). Для непассажирских категорий (`TurnaroundPlanner.applies`) над карточкой строка «Запишется в оборот: <станция, время | новая запись>» и «Выбрать» (шторка «Запись оборота»: «Автоматически», записи без плотности, «К отметкам оборота ›»; цель, выбранная кнопкой «замерить» на «Обороте», учитывается так же). После «Стоп» рядом с «Заново»/«Сбросить» - основная кнопка «Записать» (на всю ширину, 64 dp). Выбор записи (`TurnaroundTarget.pick`): выбранная цель, если в ней нет плотности; иначе запись БЕЗ плотности, ближайшая по времени к текущему моменту (идущая стоянка или последняя прошедшая; записи без времён - в конце); иначе новая ручная запись (класс «более 5 мин», станция пустая, время = сейчас). Существующая плотность молча не затирается. После записи вместо кнопки «Записано в оборот: <станция>» и «К отметкам оборота ›»; «Заново»/«Сбросить» сбрасывают это состояние. Для пассажирских категорий кнопка, строка и «Выбрать» не показываются. Строка оценки: «Замер: X с - ...».

**26.8т. Контроль плотности на стоянках.** Эталон - замер II (поездное положение) на лицевой стороне (`timerII.recordedSec`): п. 10.1.19 Правил ТО тормозного оборудования - изменение плотности более чем на 20 % от указанной в справке (в любую сторону) - сокращённое опробование. Допуск 0,8-1,2 от эталона, границы (ровно 20 %) - в норме; отклонение = (t − II) / II, минус - время падения давления меньше (плотность хуже). `TurnaroundDensityCheck.check`, пересчитывается при правке плотности и эталона. Нет замера II - «нет эталона: запишите замер II на лицевой стороне справки» (не ошибка). При превышении в шторке записи - пояснение со ссылкой на прил. 3 п. 1 и подсказка отметить «СП» (вид сам не меняется). Для пассажирских категорий оборот не ведётся.

**26.8у. Подсказки станций.** Поле «Штемпель станции» на лицевой стороне и «Станция» в записи оборота - ввод с выпадающими подсказками (фильтр по подстроке без учёта регистра, показывается в фокусе, пока набранное не совпало с вариантом целиком). Источники: станции раздела «Поезд» маршрута (первыми, в порядке следования), `UserSettings.stationList` и справочник станций (`StationDirectoryUseCase`) - по алфавиту, без дубликатов.

**26.8ф. Изменение состава на обороте (графы 3-6).** В шторке записи: «Вес поезда, т» (вводит машинист, из осей не выводится); блок «Что изменилось в составе» - строки изменений (можно несколько, «+ строка», «×» удаляет): переключатель «Отцеплено»/«Прицеплено», «Нажатие на ось, тс» (чипы значений как на лицевой таблице или своё, в том числе дробное) и «Осей»; строки сохраняются в записи (`TurnaroundEntry.changes`, `WagonChange`; поле необязательное, у старых записей пусто) и не теряются при повторном открытии. Расчёт `TurnaroundComposition` по осям: фактическое = фактическое лицевой (с локомотивом, если он учитывается) + Σ(прицеплено: оси × нажатие) − Σ(отцеплено: оси × нажатие); «Всего осей» = оси лицевой ± оси строк (поле заполняется само, правится вручную). Неполные строки (нет нажатия или осей) не учитываются. Предупреждения без блокировки: «Такого нажатия (X тс/ось) нет на лицевой» (вычитается как введено), «Отцеплено N осей по X тс/ось, на лицевой только M», «Отцеплено осей больше, чем в составе». Потребное = ceil(N × Q / 100) (п. 8 прил. М 808-2022), Q = новый вес (+ локомотив); N берётся заново по категории: для порожних поездов по числу осей (до 350 - 55, 350-400 - 44, более 400 до 520 - 33, табл. 1 прил. 2), для остальных N от осей не зависит; ручные оси по табл. 4 прил. 2 (не менее 0,6, п. 7.2 прил. 2) на новый вес. Подпись поля «· расчёт», при ручной правке «· правка» (значение, отличное от расчёта, при правках не затирается) и кнопка «Пересчитать». Если строк нет, а вес/оси введены, фактическое остаётся ручным полем с подсказкой «Введите фактическое нажатие или добавьте строки прицепленных/отцепленных вагонов». Строка «Норма изменилась: было N₁, стало N₂ тс на 100 тс веса», вывод «Нажатия достаточно / не хватает: X из Y тс», потребное число ручных осей (с «было»). Расчёта нет, если на лицевой не заполнен вес (подсказка, значения вводятся вручную). «?» - шторка «Как считается» (формулы, пример по осям, ссылки на табл. 1-4 прил. 2 и прил. М 808-2022). Строка в таблице оборота: «Новый вес N т · Потребное X тс · Фактическое Y тс».

**26.8у2. «Заполнить из маршрута»: выбор маршрута, поезда и локомотива.** Открыта из формы «Поезд» (известен `routeId`) - тот же маршрут и поезд формы. Открыта из «Инструментов» (пустая) - берётся текущий маршрут тем же правилом, что «Текущий маршрут» главного экрана (`findCurrentRoute`): нет текущего маршрута - диалог (`AppAlertDialog`) «Нет текущего маршрута»; нет поезда - «В текущем маршруте нет поезда» (без заполнения); один поезд - заполняется; несколько - шторка «Поезда · N» (стиль единиц «Текущего маршрута»: `UnitSheetRow`, как на главном), после выбора заполняется. Локомотив: один - подставляется сразу; несколько - шторка «Локомотивы · N» (после выбора поезда). Серия и номер выбранного локомотива подставляются, затем по серии - группа ГР и вес/оси/нажатие локомотива из `BrakeLocoCatalog` (ВЛ10 - по номеру). После заполнения справка привязывается к маршруту и поезду (`routeId`/`trainId`), включаются сохранение и стоянки оборота. Категория подбирается по номеру поезда (§26.8п).

**26.8у3. Источники подсказок станций.** Подписка (не разовое чтение): `UserSettings.stationList` и список маршрутов (`getListRoutesAsFlow`) через `combine` с `debounce(300)`, плюс справочник станций (`StationDirectoryUseCase`) и станции всех поездов всех маршрутов (история). Порядок: станции текущего поезда, затем остальные по алфавиту, без дубликатов. Список показывается уже при фокусе поля (все варианты, прокручивается), при вводе фильтруется.

**26.8у4. «Другие данные» и плотность на лицевой.** Графа «Другие данные»: каждый параметр - строка «подпись значение» (подпись слева, значение сразу после неё), параметры друг под другом столбцом (не `FlowRow`), значение переносится при нехватке ширины, проверено при fontScale 1,3. Плотность на лицевой: II и IV - каждое в своей строке («II  27,1 с» / «IV  24,3 с»), по правому краю, без обрезки; ниже «из «Замеров» · IV не замерено».

**26.8у5. Ручной ввод плотности.** На «Замерах» у каждого замера (II, IV, «В пути»), когда секундомер не идёт: поле «или введите вручную, с» (дробные, 0 < t <= 3600, округление до 0,1) и кнопка «Принять» (`setManualDensity`, `BrakeDensity.parseManualSeconds`). Ручное значение помечается «введено вручную» (в табе «*»), участвует в нормах и вердиктах как замер, «Заново»/«Сбросить» его очищают; для «В пути» его можно записать в оборот кнопкой «Записать». На лицевой стороне тап по строке плотности II/IV открывает шторку «Плотность, с» (чипы II/IV, поле, «Готово», «Замерить ›»); в бланке у ручного значения подпись «вручную». Хранится в `DensityMeasurementData.manual` (необязательное, по умолчанию `false`; старые справки читаются как замер).

**26.8у6. Норма на «Замерах» показывается всегда** (до старта, во время замера и после) под подзаголовком карточки: II (грузовой) - «Норма плотности во II-м: не менее N сек»; пассажирский II - «падение давления в ТМ не более 0,2 кгс/см² за 1 мин (п. 9.3.5)»; IV - 90 % от замера II или норма по группе; «В пути» - «Допуск ±20 % от замера II: X–Y с». Если данных нет - причина: «Нормы нет: выберите группу локомотива (или объём ГР) на «Расчёте»», «укажите число осей», «нужен замер II или группа/число осей», «Допуска нет: нужен замер II». Под результатом - только вердикт (повтор нормы убран; при превышении остаётся пояснение).

**26.8у7. «Записать» замер «В пути» и шторка «Куда записать замер».** После «Записать» таймер «В пути» возвращается в исходное состояние (кнопка «Старт»), запись в оборот остаётся; подтверждение - снекбар «Записано в оборот: <станция, время>» с действием «К отметкам оборота» (постоянной строки на карточке нет; для II/IV такого поведения нет). Над карточкой: «Запишется в оборот: <станция, время>» или «Запишется в новую запись» и «Выбрать». Шторка «Куда записать замер»: список всех записей оборота (станция/место, время; если плотность уже есть - строка неактивна с пометкой «плотность уже записана: N с») и «Новая запись» (`TurnaroundTarget.NEW_ENTRY`) в конце; радио-меткой всегда отмечена текущая цель (подобранная автоматически или выбранная); пункта «Автоматически» нет. Выбор «Новая запись» создаёт ручную запись (>5 мин, станция пустая, время = сейчас).

**26.8ф1. Локомотив в итогах справки.** При включённом «Учитывать локомотив» (п. 6.3-6.4 прил. 2 Правил ТО тормозного оборудования: в грузовых не учитывается, для пассажирских и грузопассажирских данные локомотива утверждает владелец инфраструктуры; 808-2022 прил. М) вес, оси и нажатие локомотива входят в потребное (N × (вес состава + вес локомотива) / 100), фактическое (+ оси × нажатие локомотива) и число осей (`BrakeCertificateResult.totalAxles`) - так было всегда, теперь это видно: на «Расчёте» под показателями строка «Локомотив учтён: A осей, B тс, вес W т - входят в потребное, фактическое и число осей»; на лицевой под «Требуется нажатие» - «с учётом веса локомотива (W т)», под таблицей - «Итого с локомотивом: N осей, X тс (в т.ч. локомотив: A осей, B тс)». Таблица «Всего» относится только к вагонам. При выключенном переключателе локомотив в итогах не участвует. `BrakeCertificateResult.loco` (`LocoShare`).

**26.8ф2. «Другие данные» и давление при замере.** В графе «Другие данные» подпись и значение выровнены по базовой линии текста (`alignByBaseline`), кнопка называется «Редактировать». В шторке плотности бланка (тап по строке плотности) для II и IV - поля «Давление в ГР, кгс/см²»: «В начале замера» и «В конце замера» (дробные), показывается снижение. Проверка допуска только там, где она есть в нормативе: II положение грузового поезда - п. 9.3.3 прил. 2 Правил ТО тормозного оборудования (время замеряется при снижении давления на 0,05 МПа = 0,5 кгс/см²; допуск на эту разность в тексте не приведён - сравнение с точностью записи 0,01): «соответствует интервалу замера 0,5» либо «время замеряется при снижении на 0,5»; конец выше начала - ошибка ввода. Для IV положения и пассажирских поездов - только показ разницы (для пассажирского II разность переносится в «падение давления в ТМ», норма п. 9.3.5 - по времени). Хранится в `DensityMeasurementData.grStart/grEnd` (необязательные, ранее неиспользуемые).

**26.8ф3. «Замеры»: секундомер, ручной ввод, повторный замер.** (а) Секундомеры показывают только секунды с десятыми («75,3», моно, крупно, без минут), в подписях вкладок - «В пути · 75,3 с». (б) В строке нормы число или диапазон «a–b с» целиком одним акцентным цветом (вердикт - своим цветом). (в) Ручной ввод плотности секундомер НЕ трогает: остаётся «0,0» и «Старт»; введённое значение показывается отдельно «введено вручную: N с — <вердикт>» со ссылкой «Убрать значение» (во всех вариантах II, IV, «В пути»); «Записать» для «В пути» доступно и с ручным значением. (г) В шторке «Куда записать замер» можно выбрать запись с плотностью (пометка «плотность уже записана: N с», строки активны); при «Записать» диалог «Заменить плотность? В записи «…» уже есть плотность. Заменить N на M с?» (`AppAlertDialog`, `replaceConfirm`); автоподбор записи с плотностью не берёт, явный выбор - берёт (`TurnaroundTarget.pick`).

**26.8ф4. Шторка записи оборота, «Автоблокировка», лицевая.** Шторка записи: порядок - стоянка, станция, плотность (с оценкой), вид опробования, «Подпись и время» (время опробования и подпись), затем СВОРАЧИВАЕМЫЙ блок «При изменении состава и веса поезда» в самом низу (по умолчанию свёрнут, развёрнут, если в записи есть строки изменений, вес, оси, потребное или фактическое); кнопки «Удалить»/«Готово» закреплены внизу. «Автоблокировка» на «Расчёте»: подпись и переключатель в обычных цветах (text, дорожка accent); приглушается (alpha 0,5) только когда для категории и уклона нет значения в скобках табл. 1 прил. 2 - тогда под переключателем причина текстом. Лицевая сторона: шапка с данными поезда (штемпель, время выдачи, дата, локомотив, поезд) показывается всегда, без сворачивания; строки таблицы нажатий - 32 dp (было 40), «Итого»/«Всего» - от 32 dp.

**26.8ф5. Иконка ВУ-45.** Карточка «ВУ-45» в «Инструментах» Главного (§4 п.8) и кнопка справки в нижней панели формы «Поезд» (§6.2 п.9) используют текстовую иконку `Vu45Icon` - «ВУ-45» жирным моно-шрифтом приложения (ExtraBold, размер подбирается под размер иконки: 72 dp на карточке, 28 dp в панели), цвет по теме (на карточке - как у остальных иконок, в панели - основной цвет текста). Растрового ресурса `ic_card_brake` больше не используется.

**26.8ф6. Удаление справки.** В топ-баре справки - иконка корзины; подтверждение шторкой «Удалить справку? Справка будет удалена с этого устройства и с сервера» (стиль удаления маршрута, `AppBottomSheet`, «Да, удалить»/«Отмена»). После подтверждения: справка помечается удалённой (надгробие `BrakeCertificateUseCase.deleteByIds`, как при удалении поезда), маршрут помечается несинхронизированным (`markUnsynchronized`, при включённой отправке справок) - при следующей синхронизации на сервер уходит список без справки (full-replace, контракт не менялся), затем надгробие очищается; с сервера удалённая справка не воскресает (слияние пропускает локальные надгробия, `mergeRemote`); автосохранение отключается (иначе при выходе справка записалась бы заново); экран закрывается, тост «Справка удалена». Справка, ещё не сохранённая в хранилище, просто закрывается.

**26.10. «Мои справки ВУ-45»** (Профиль → ДАННЫЕ → «Мои справки ВУ-45», «Сохранённые справки о тормозах», после «Мои треки»). Список всех сохранённых справок (`BrakeCertificateUseCase.listAll`, новые сверху): карточка - дата и время сохранения (моно), «маршрут от ДД.ММ.ГГГГ ЧЧ:ММ» (или «маршрут не найден»), «Поезд N · локомотив серия-номер», станция выдачи справки (штемпель: «Станция: …» или серым «Станция не указана»); информации о нажатии, скорости и вердикте в карточке нет. Тап открывает справку (`BrakeCertificateRoute` с `routeId`/`trainId`, загрузка из хранилища). Оформление и поведение - как в «Все маршруты» (`AllRouteScreen`), общие компоненты: `CenterAlignedTopAppBar` («‹», «Выбрать»/«Готово», в режиме выбора «Все»/«Снять» и «Выбрано: N»), карточка (`Card`, 1 dp, фон secondary, выбранная - рамка 2 dp), `SelectionMarker`, `SwipeToRevealDelete` (свайп - красная «УДАЛИТЬ» → шторка подтверждения), тап в режиме выбора - выделение, долгое нажатие - без действия (в «Все маршруты» оно открывает просмотр маршрута), нижняя панель `SelectionBottomBar` (красная корзина; общий компонент, в «Все маршруты» к нему добавлены «в избранное» и «поделиться»), шторка подтверждения «Удалить справку?/Удалить справки?» с «Выбрано: N» и «Да, удалить». Удаление - как в 26.8ф6 (пачкой, маршруты помечаются несинхронизированными), тост «Справка удалена»/«Справок удалено: N». Пустое состояние: «Справок нет» и пояснение. Методы хранилища: `getById`, `listAllActive`, `markDeletedById` (`BrakeCertificateRepository`). Формат данных и контракт не менялись.

**26.8ц. Правки третьего пакета.** (1) Иконка «ВУ-45» (`Vu45Icon`): буквы моно-шрифтом ExtraBold в прямоугольной рамке (скругление 3 dp, цвет иконки, 2 dp на карточке 72 dp и 1,5 dp в панели формы «Поезд» 38 dp). (2) На обороте нет «Штемпеля станции» (реквизит лицевой стороны); станции записей - в столбце «Станция / место». (3) В шторке записи кнопка «Добавить изменения состава»; на обороте под записью при смене состава: «Новый вес 3100 т. (290 осей) · Потребное X тс · Фактическое Y тс» (вес «ХХХХ т.», оси в скобках «(ХХХ осей)»). (4) Локомотив на лицевой при включённом «Учитывать локомотив»: в таблице нажатий отдельная строка (подсветка accentSoft, «лок. <серия>», нажатие на ось, оси, нажатие колодок - не редактируется, правка в карточке «Учитывать локомотив»), входит в «Всего»; в шапке под «весом»/«Всего осей» строка «весом N т (с лок.)», «всего осей N (с лок.)» (вес = состав + локомотив, оси = вагоны + локомотив, как в калькуляторе); при выключенном переключателе строки и пометок нет. (5) Порядок вкладок «Бланк | Расчёт | Замеры | Справочник»; всегда открывается «Бланк» (вход из «Инструментов», формы «Поезд», «Мои справки»), запомненная вкладка не используется. (6) Поле «Падение давления в ТМ» пассажирского II имеет «?» (`pass_drop`): что вводить и откуда брать (манометр ТМ, прил. 2 п. 9.3.5).

**26.8ч. Четвёртый пакет (лицевая, справочник).** (а) При включённом «Учитывать локомотив» в шапке бланка «весом» и «Всего осей» показывают значения С локомотивом (одно значение, не редактируется; вагонные - в «Расчёте»), снизу мелко «(с учётом локомотива)»; при выключенном - обычные поля. (б) Строка локомотива в таблице нажатий: если в таблице уже есть строка с таким же нажатием на ось (`BrakeTable.locoRow`), оси локомотива прибавляются в неё (нажатие колодок включает локомотив, в графе «осей» мелко «в т.ч. лок. N»), отдельной строки нет; иначе - отдельная строка локомотива. «Всего» - локомотив один раз. (в) Границы столбцов во всех строках таблицы (шапка, строки, локомотив, «Всего») совпадают: у ячеек шапки границы рисуются по ширине ячейки. (г) Строка изменения состава в шторке оборота: сначала «Осей», затем «Нажатие на ось, тс». (д) Справочник локомотивов: добавлены 2ЭС4К, 3ЭС4К, 2ЭС5К, 3ЭС5К, 2ЭС6 (вес 192/288 т и оси 8/12 - справочно; нажатие на ось - пусто, вводится вручную; поиск по серии без учёта регистра и латиницы). Объём ГР 3000 л для 3ЭС4К/3ЭС5К (со слов владельца) - уже в группе 7; для 2ЭС-серий объём не задан (ввод вручную).

**26.8ш. Пятый пакет (лицевая, справочник).** (а) Подпись «с учётом локомотива: N т, M осей» - один компонент `LocoCaption` (10 sp, muted, по правому краю), одинаково под весом/осями и под «Требуется нажатие». (б) Слияние локомотива с таблицей (`BrakeTable.locoRow`): по равенству нажатия с допуском 0,001; сначала с заполненными строками, затем с напечатанной шкалой бланка (строка 14 существует и когда не заполнена - оси локомотива и нажатие колодок пишутся в неё, пометка «в т.ч. лок. N»); нет такого значения - отдельная строка. Прежняя ошибка: сравнивались только заполненные строки точным равенством, поэтому для незаполненной строки шкалы создавалась лишняя отдельная строка. (в) Объём ГР 2000 л у 2ЭС4К, 2ЭС5К, 2ЭС6 (справочно, открытая информация; группа 5 по табл. 4 п. 9.3.3; подбирается по серии автоматически).

**26.8ы. Эталон плотности на обороте.** Запись с видом «СП» (и «ТП» - так же, по аналогии) и указанной плотностью НЕ оценивается: в таблице только значение и мелкая пометка «эталон», без «в норме/более 20 %» и без процента; в шторке вместо допуска «Станет эталоном для следующих замеров (сама не оценивается)». Эта плотность - эталон для следующих по времени записей БЕЗ вида (только замер плотности): им показывается процент, «✓ в норме»/«! более 20 %» и источник «от СП 09:30» (`TurnaroundReference`); до первого СП/ТП эталон - замер II лицевой («от II»); нет ни того ни другого - «нет эталона». Запись без вида сама эталоном не становится; СП без плотности эталоном не становится. Порядок - по времени опробования, иначе по времени прибытия (время суток), иначе по порядку списка; оценки пересчитываются при любом изменении записей. Подтверждение правила в тексте Правил/808-2022 не найдено (п. 10.1.19 говорит об «указанной в справке» плотности) - правило машиниста.

**26.8щ. Шестой пакет: лицевая.** (а) Ячейка «Кол-во осей» строки, в которую влит локомотив: крупно число осей строки (вагоны + локомотив), мелко под ним «в т.ч. лок. N»; работает и для пустой строки (число = оси локомотива); тап по ячейке - правка числа осей вагонов. (б) Сверка осей (`AxleCheck.compareWithLoco`): таблица и «Всего осей» сравниваются обе С осями локомотива (если он учитывается) или обе без - оси локомотива не попадают в разницу.

**26.8э. Шторка «Плотность, с» (бланк).** Хранит оба положения одновременно: введённые II и IV (и давление в начале/в конце для каждого) не теряются при переключении чипа (✓ на чипе - значение есть); «Готово» сохраняет оба. Решение по каждому значению - `BrakeDensity.commitDensity`: пустое поле или неизменённый текст сохранённое (в т.ч. замер секундомером) не затирают; изменённое корректное число записывается как «введено вручную»; «Очистить» у поля - явное удаление.

**26.8ю. Категория по нагрузке на ось.** Нагрузка = вес вагонов (брутто, без локомотива - как в «Весе состава») / оси вагонов. Больше 10,0 т/ось - «Груженый грузовой»; не больше (ровно 10,0 - порожний) - порожний по осям: до 350 → «Порожний до 350 осей», 350-400 → 44, более 400 до 520 → 33; более 520 осей порожнего - категория не меняется (`BrakeCategory.byAxleLoad`). Применяется только к категориям груженый/порожние (`axleLoadCategoryIds`; рефрижераторные, грузопассажирские, пассажирские и т.п. не меняются), при открытии справки, «Заполнить из маршрута» и при изменении номера поезда, веса, «Всего осей», строк таблицы осей на «Расчёте» и «Бланке» (`autoCategory`), пока категория не выбрана вручную (`categoryManual` - поле данных справки `BrakeCertificateData.categoryManual`, у каждой справки своё; справки, сохранённые до появления поля (null), считаются ручными и не перетираются; новая справка - авто; «Заполнить из маршрута» признак сбрасывает); однозначный номер поезда (961-970, 9301-9398, 9401-9498) приоритетнее. Нет веса или осей - категория не меняется. Под выбором категории мелкая пометка «Определено по нагрузке: N,N т/ось (> 10 - груженый)» / «(≤ 10 - порожний)»; при ручном выборе пометка скрывается; диалогов нет. Под строкой «Требуется нажатие тормозных колодок» на Бланке и под метриками на «Расчёте» - на Бланке - подпись справа «Авто (M тс на 100 т веса)» / «Выбрано (M тс на 100 т веса)» (категория определена автоматически / выбрана вручную, `categoryManual`); на «Расчёте» - «Потребное на ось: N тс (норма M тс на 100 тс веса)» = потребное / всего осей (с локомотивом, если учтён).

**26.8я. Седьмой пакет (лицевая).** В графе «Другие данные» добавлена строка «Заряд. давл. N кгс/см²» (тот же источник `chargePressureText`, что поле на «Расчёте»/в шторке «Другие данные»; не задано - строки нет). Убраны значки «?» у «Всего осей» и у «Локомотив, серия / №» в шапке бланка (пояснения - в «Справочнике»); остальные «?» сохранены.

**26.8ь. Справочник локомотивов (дополнение).** Нажатие 14,0 для 2ЭС4К/3ЭС4К/2ЭС5К/3ЭС5К/2ЭС6; серии из проекта Приложения 2 (ВЛ8, ВЛ8М, ВЛ11К, ЭП1М, ЭП2К, ЭП10, ЧС2К, ЧС3, 2ТЭ25К, ТЭМ21, 2ТЭ70, 2ТЭ116, ТЭП70У, ТЭП70БС); группы ГР по Приложению № 1 к Правилам эксплуатации тормозов (владельцем предоставлены; проект - не действующий документ). Полный список допущений и пробелов - docs/VU45_SPEC.md «Пробелы справочника локомотивов».

**26.8ъ. «Справочник»: опробования и недостаток нажатия.** (а) «Полное опробование тормозов» - только случаи проведения (п. 9.3.1 Правил ТО тормозного оборудования): станции формирования и оборота, после смены локомотива, после стоянки без локомотивной бригады, станции между гарантийными участками, станции перед затяжными спусками (0,018 и круче - выдержка 600 с); порядка выполнения нет. (б) Новая карточка «Технологическое опробование в грузовых поездах» (п. 9.6 Правил ТО; ГР ниже 0,55 МПа): что это, когда (5 случаев), что проверяется (головные вагоны, не менее 5; плотность ±20 % от справки; действие тормозов ступенью 0,06-0,07 МПа), при изменении плотности более 20 % - сокращённое опробование с отметкой в ВУ-45; отдельного вида в выборе «—/ТП/СП» на обороте нет (прямого требования отметки технологического опробования в тексте нет). (в) «Недостаток нажатия: что делать» - алгоритм по ступеням: шаг 0 подсчёт (Па = N × Q / 100 вверх; факт; на 100 тс), ступень 1 (норма), ступень 2 (между табл. 2 и табл. 1, случаи п. 4.2-4.3 прил. 2; п. 4.4 прил. 2: −2 км/ч за тонну / −1 или −2 для пассажирских, вниз до кратного 5; пример 3000 т: 960 тс → 78 → 75 км/ч, 900 тс → 70 км/ч; минимум груженого 28 на 100 тс), ступень 3 (ниже табл. 2: владелец по номограммам, −20 %, п. 5.1 прил. 2; порядок доклада - по документам владельца), доведение нажатия (отцепка вагонов с неисправными тормозами - осмотрщик ПТО), ручные тормоза (п. 7.1 прил. 2-46, табл. 4 прил. 2; пример 18 осей), отказ автотормозов на ходу (п. 33; до первой станции с ПТО; п. 132), запись в ВУ-45 (осмотрщик, прил. М). Красный блок «Что делать при нехватке нажатия» на «Расчёте» - 4 пункта (`deficitShortSteps`) и кнопка «Алгоритм в «Справочнике» ›» (открывает вкладку «Справочник» с раскрытой карточкой «Недостаток нажатия»); текст «?» `deficit` согласован.

**26.8ъ1. Упрощение карточек «Справочника».** «Недостаток нажатия: что делать» - три цветные плашки-ступени (зелёная: норма - по табл. 1 прил. 2; жёлтая: не хватает, но выше минимума табл. 2 прил. 2 - скорость снижается, приложение считает само; красная: ниже минимума - скорость устанавливает владелец инфраструктуры, сообщить диспетчеру/ДСП), одна строка-пример («3000 т, не хватает 3 тс на 100 т - 70 км/ч»), блок «Что можно сделать» из трёх строк (довести нажатие; ручные тормоза - число осей в «Расчёте», табл. 4 прил. 2; доложить и записать в ВУ-45); ссылки на пункты - мелким тегом справа в конце строки (`RefLine`, формат «текст ‖ тег»); формул нет. Красный блок на «Расчёте» и «?» `deficit` - в том же кратком виде (4 строки). В карточке «Технологическое опробование в грузовых поездах» удалены два последних абзаца (про сокращённое опробование при >20 % и про отметку в ВУ-45).

**26.8ъ2. «Недостаток нажатия» - только факты.** Карточка «Справочника» «Недостаток нажатия» (бывшая «что делать»): две ступени-плашки - жёлтая (одно предложение: «Если нажатие ниже нормы, но не ниже минимума, то скорость снижается за каждую недостающую тонну нажатия на 100 т веса поезда: на 2 км/ч - в грузовых, рефрижераторных и хозяйственных поездах; на 1 км/ч - в пассажирских, грузопассажирских и рефрижераторных (при скорости свыше 90 км/ч) поездах на спусках до 0,006, на более крутых спусках - на 2 км/ч. Полученный результат округляется в меньшую сторону до числа, кратного 5»; тег «п. 4.4 прил. 2»; перед таблицей заголовок «Ниже - данные из таблиц 1 и 2 прил. 2 (тс на 100 т веса)») и красная (ниже минимума: скорость по таблицам не определяется, устанавливает владелец инфраструктуры по номограммам, на 20 % ниже; п. 5.1 прил. 2). Зелёной ступени «в норме» нет. Мини-таблица «категория: норма, минимум» (тс на 100 т веса) строится из `BrakeNorms` (табл. 1/2 прил. 2): груженый 33/28, порожний до 350 осей 55/50, порожний 350-400 и 400-520 - минимум не задан, грузопассажирский 44/38, пассажирские 60/45, 68/60, 78/68, 80/70; одна строка-пример. Блока «Что можно сделать» и советов нет. Красный блок на «Расчёте» теперь «Нехватка нажатия» - факты по текущему расчёту (`deficitFacts`): «Не хватает X тс (Y на 100 т веса): фактическое A из B тс», «Скорость: N км/ч (табл. 1 прил. 2: M, снижена по п. 4.4 прил. 2)» либо «Скорость по таблицам не определена (...)»; кнопка «Нормы и снижение скорости в «Справочнике» ›». «?» `deficit` - те же факты без советов. Открыто к уточнению: нужны ли в красном блоке рекомендации «что делать» (сейчас убраны).

**26.8э. Сверка с Правилами 2025 г. (01.11.2026).** Нормы и ссылки приведены к «Правилам технического обслуживания тормозного оборудования и управления тормозами железнодорожного подвижного состава железных дорог» (редакция 2025 г., АО «ВНИИЖТ»): старые III.1-III.4 / IV.1 / п. 25-59 / прил. 3 → табл. 1-4 и п. 1-7 прил. 2, табл. 4 и п. 9.3.3 / 9.3.5 / 9.5 / 10.1.19 основного текста, форма — прил. 1. Изменения в расчёте: (1) строки табл. 1 перенумерованы (порожние 350-520 осей — 15.1/15.2; соединённые и повышенного веса — 15.3-15.7); «Соединённый, объединённая ТМ» теперь «до 14,2 тыс. тс» с нормами 33, 75(80)/65 (строка 15.5) вместо «до 12 тыс. тс» 65(75)/60; добавлена категория «Соединённый порожний до 800 осей, объединённая ТМ, локомотивы в голове и составе» (15.7: 33, 70(80)/60; табл. 2 — 28; 60, 60, (55); при нажатии 28 тс на спуске круче 0,010 формула п. 4.4 даёт 50, табл. 2 — 55: принимается меньшая); (2) поправка плотности на зарядное давление: -10 % при 5,3-5,6 кгс/см² (было 5,3-5,5); (3) плотность ТМ пассажирского поезда: не более 0,2 кгс/см² за 1 мин, вариант «0,5 за 2,5 мин» убран; (4) бланк ВУ-45: 26 строк нажатия на ось (строки 20,0 в форме 2025 г. нет); (5) справочник типов вагонов: убраны значения «120-130 км/ч» (в табл. 3 прил. 2 для композиционных колодок свыше 120 до 140 км/ч - на 25 % больше, свыше 140 до 160 - на 30 %), добавлены контейнерные платформы (12,5 / 9,5 / 3,5); (6) подсказки: давление зарядное по табл. 1 п. 3.2.29, сокращённое опробование (п. 9.5.1-9.5.3: стоянка более 30 мин для всех поездов), проверка в пути (п. 10.2.8-10.2.12: пассажирский — нет эффекта за 10 с), питательная сеть (с 8,4 кгс/см² не более 0,5 за 60 с, п. 9.3.5), технологическое опробование (ГР ниже 0,55 МПа), «до 100 осей IV положение не проверяется», выход штока (п. 17.2.6, зависит от диаметра цилиндра), список локомотивов «без подсчёта 60 тс» (п. 6.6: ВЛ80, ВЛ10, ВЛ10У, ВЛ11, ВЛ11М, ВЛ65, Э5К, 2ЭС4К). Округление требуемого нажатия вверх остаётся по 808-2022, прил. М, п. 8 (в Правилах 2025 г. об этом не сказано). Подробности — `docs/VU45_INSTRUCTION_ACTUALITY.md`, раздел 8.

**26.9. Не учитывается:** выход штока (п. 17.2.6 Правил), номограммы владельца инфраструктуры (п. 5.1-5.2 прил. 2), локомотивы вне справочника (вводятся вручную), тормозные башмаки (табл. 4 прил. 2),
+25/+30 % композита пассажирских >120 км/ч (прим. 4 к табл. 3 прил. 2), строки табл. 1 прил. 2 для скоростных поездов (7, 8: 200 км/ч, с округлением скорости до 10 км/ч) и грузовых со скоростями свыше 100 км/ч (13, 14.1, 14.2), расчёт 85 км/ч при К-100 без вагонов с несимметричной передачей (п. 3.4 прил. 2), хранение копий справки (п. 9.2.5), подписи/печать, автосоздание отметок оборота по GPS без станций раздела «Поезд».

**26.8э. Оборот: оси при отцепке/прицепке.** Новое число осей = «Всего осей» лицевой стороны (введённое число; если не введено - сумма осей таблицы нажатий) минус отцеплено плюс прицеплено; раньше бралась только сумма по таблице, и при незаполненном нажатии отцепка 4 осей давала «-4» вместо 196. Фактическое нажатие считается, только когда лицевая задаёт нажатие (таблица или среднее). Число осей считается и без веса лицевой (нужна только категория). При раскрытии блока «При изменении состава и веса поезда» первая строка изменений создаётся сразу. В строке «Отцеплено» быстрый выбор нажатия - только те значения, что есть на лицевой с осями (под строкой «На лицевой по N тс/ось: M осей»; нет таблицы - пояснение); «Прицеплено» - печатные значения как раньше.

**26.8ю. Бланк: число осей и PWA-раскладка.** «Всего осей» на лицевой при учтённом локомотиве = оси состава + оси локомотива; если таблицы нажатий нет (нажатие не задано), оси состава берутся из введённого «Всего осей» (раньше показывались только оси локомотива: пассажирский поезд после «Заполнить из маршрута»). В PWA: боковые поля экрана справки 8 px, шторки ВУ-45 - 12-14 px; поле серии локомотива на бланке (и на Android тоже: `knownSeriesNames`) и поле станции (штемпель, оборот) подсказывают значения из справочников пользователя (серии: справочник серий и список серий; станции: станции поезда, справочник станций, станции прошлых поездов); шторка с полем ввода поднимается над экранной клавиатурой; иконка «ВУ-45» на главном крупнее иконок-квадратов. **Подсказки станций и серий** показываются, когда ввод начался (значение изменилось), а не сразу при получении фокуса. «Расчёт РТ»: под полем нормы строка «Требуется ручных тормозов: N осей (X на 100 т веса)» - когда хватает данных (вес), иначе ничего. Номер маршрута в форме маршрута - только цифры (Android и PWA). **Дата на лицевой:** только цифры, маска «ДД.ММ.ГГГГ» (`DateInput.mask`, точки подставляются сами, при стирании не мешают; день 01-31, месяц 01-12; цифровая клавиатура), одинаково на Android и в PWA.

**26.10. Хранение и синхронизация.** Одна текущая справка на поезд маршрута (история перевыдач не ведётся). Хранится в отдельной БД `BrakeCertificate.db`
(SQLDelight, `data_local/.../brake`; Route.db не затронут): `certificateId`, `routeId`, `trainId`, `updatedAt` (мс), `data` (JSON `BrakeCertificateData`), `isDeleted` (надгробие).
Загрузка: при входе экран ищет справку по (`routeId`, `trainId`) и восстанавливает её; нет справки - предзаполнение как в 26.1. Сохранение: автоматически с задержкой 1 с
после последнего изменения и при выходе с экрана (идущие секундомеры, вкладка и раскрытые карточки не хранятся). Кнопка «Заполнить из маршрута» не затирает введённое:
пустые поля заполняет, при расхождении со значением из маршрута показывает список конфликтов с действием «Заменить». Удаление поезда помечает справку надгробием
(`TrainUseCase.removeTrain`), удаление рейса стирает все справки рейса (`RouteUseCase.purgeRoute`). Справка не показывается на сайте/в шаринге маршрута.
Синхронизация: необязательное поле `brakeCertificates` в `SyncData` (POST/GET `/v1/route/`, `/v1/route/delta`; контракт сервера - `docs/VU45_CERTIFICATES_API.md` репозитория сервера).
`null`/нет поля - сервер не трогает справки рейса (старый клиент, выключенный флаг, нет справок и надгробий), `[]` - стирает, иначе full-replace по `certificateId`;
слияние при загрузке по LWW (`updatedAt`); отсутствующее поле (`null`) локальные справки не удаляет. **Удаление на другом устройстве:** если сервер прислал массив справок рейса и справки в нём нет (она удалена на другом устройстве), локальная копия стирается (`BrakeCertificateStorage.pruneDeletedRemotely`, `SyncManager.pruneRemoteDeletedCertificates`) - только когда рейс чистый (`isSynchronized`) и справка не новее снимка рейса (`local.basicData.updatedAt`); несинхронизированная или более новая локальная справка остаётся и уходит на сервер. PWA ведёт себя так же (`pruneCertificatesDeletedRemotely`, порог - время начала синхронизации). Удаление в «Моих справках» ставит надгробие и помечает рейс несинхронизированным; надгробие уходит на сервер вместе с рейсом и после доставки стирается (последняя справка рейса - `[]`). PWA использует тот же код расчётов (мост Kotlin/JS `PwaBrakeBridge`, `BrakeCertificateEngine`), а не его копию: результаты на платформах совпадают (паритет-тест `BrakeCertificateEngineParityTest`, кросс-платформенный тест `BrakeCertificateCrossPlatformTest` на настоящем ответе сервера). В PWA иконка «ВУ-45» (`Vu45Icon`) - рамка с запасом до букв; кнопка «+» формы «Поезд» - круг 48 px без подписи. Отправка включена флагом
`RemoteFeatureFlags.REMOTE_CERTIFICATES_ENABLED = true` (data_remote); сервер с миграцией 055 должен быть на проде до релиза клиента. Правка справки помечает рейс несинхронизированным (`routeUseCase.markUnsynchronized`), иначе справка не ушла бы без изменения самого рейса. Известное ограничение: если при синхронизации побеждает серверная версия рейса, более новая локальная справка уйдёт при следующем изменении рейса или справки. Лимиты сервера: до 20 справок на рейс, `data` до 64 КБ.

## 27. Инструмент «Закрепление поезда» (BrakeHoldingScreen)

Расчёт закрепления поезда на перегоне при вынужденной остановке (башмаки и ручные тормоза) и порядок действий бригады. Источники: «Порядок определения потребного количества ручных тормозов и тормозных башмаков для закрепления поезда на перегоне» и «Памятка локомотивным бригадам по порядку действий при вынужденной остановке поезда на перегоне» (ОАО «РЖД», ЦТ-162/р от 28.04.2025); нормы на 100 тс веса - табл. 4 прил. 2 Правил ТО тормозного оборудования (в Правилах 2014 г. - табл. III.4). Спека и решения владельца - `docs/BRAKE_FEATURES_PLAN.md`; расчёт - `domain/.../brake/BrakeHolding.kt` (тесты на примеры памятки: `BrakeHoldingTest`).

**Входы (2).** (1) Карточка «Закрепление» в «Инструментах» на «Главном» - экран открывается пустым. (2) Кнопка (колесо с башмаком, «Закрепление поезда») в нижней панели формы поезда - рядом с ДУ-61 и ВУ-45: подставляются вес, число осей и локомотив поезда (`BrakeHoldingRoute` с теми же параметрами, что у ВУ-45; вес локомотива - из `BrakeLocoCatalog` по серии; над блоками подсказка «Данные поезда подставлены…»).

**Экран.** Шапка «Закрепление поезда» с «назад»; вкладки «Расчёт» | «Порядок действий». «Расчёт»: блок «ПОЕЗД» - вес состава, т | вес локомотива, т; «Всего осей состава»; «Состав» - чипы «Груженый»/«Порожний» (по умолчанию определяется по нагрузке на ось = вес состава / оси, 10 т и более - груженый; под чипами пояснение «Определено по нагрузке: N т/ось» или «Выбрано вручную»; чип «По нагрузке на ось» возвращает автоопределение); блок «УЧАСТОК И СРЕДСТВА» - «Уклон (спуск), ‰» (0,018 = 18) и быстрые чипы 6-20, «Башмаков на локомотиве, шт»; блок «РЕЗУЛЬТАТ». Расчёт - для однородного состава (целиком груженого или порожнего); смешанный не поддерживается.

**Формулы.** `Nб = ceil((Qп + Qл) / 100 × kб)` (масса локомотива учитывается); kб - из таблицы по уклону и типу состава; уклон не из таблицы - ближайшая большая чётная ступень (0,017 → 0,018; до 40 ‰, круче - «таблицы нет»). Башмаков меньше потребного (`S`): закреплено `S × 100 / kб` тс, остаток `Qост`, ручных осей `ceil(Qост / 100 × kо)` (kо - ручные оси на 100 тс, до 0,020 включительно; круче - нормы нет: «ручными тормозами не закрепляется», нужны башмаки), вагонов с ручным тормозом - оси / 4 (ручной тормоз вагона на 4 оси; в результате крупно «Вагонов с ручным тормозом: N» и строка пересчёта «106 осей / 4 → 27 вагонов»); у вагонов с раздельным торможением (два тормозных цилиндра) ДВА ручных тормоза по 2 оси - жёлтое напоминание: на таких вагонах затормозить оба, всего оси / 2 ручных тормозов на те же вагоны. Если ручных осей больше, чем осей в поезде - предупреждение «закрепить имеющимися средствами нельзя». Результат: крупно «Башмаков: N»; строка массы и норм; зелёная отметка «Имеющихся башмаков достаточно» либо жёлтая «Башмаков не хватает: M. Закрепляется X т из Y т» с «Ручных тормозных осей: K» и вагонами; правила укладки (со стороны уклона; запрещено - между колёсными парами одной тележки, перед стыком, перед крестовиной, на рамный рельс стрелки, на наружный рельс кривой).

**«Порядок действий».** Сверху блок «ВРЕМЯ ОТ ОСТАНОВКИ»: кнопка «Остановились» запускает таймер (mm:ss, время остановки хранится в SharedPreferences `brake_holding_prefs` и переживает перезапуск; экран не гаснет) и отсчёт «Вспомогательный локомотив - затребовать не позднее 10 мин после остановки: осталось mm:ss» (зелёная отметка; после срока - красная «прошёл на mm:ss: затребуйте немедленно»); «Сбросить» обнуляет таймер и отметки шагов. Ниже девять шагов технологической карты ЦТ-162/р в виде вертикальной шкалы с тремя этапами («Сразу после остановки» 1-4, «Подготовка и вызов помощи» 5-7, «Закрепление и устранение неисправности» 8-9; заголовок этапа - акцентная метка с линией): слева номер в кружке и соединительная линия, справа карточка - исполнитель (метка «Машинист»/«Помощник»), заголовок, маркированные пункты (где исполнитель пункта отличается от исполнителя шага - акцентный префикс «Помощник:»), у шага 6 красный бейдж «не позднее 10 мин после остановки», у шага 8 красная плашка «Башмаки нельзя укладывать» (между колёсными парами одной тележки; перед рельсовым стыком; перед крестовиной; на рамный рельс стрелки; на наружный рельс кривой). Времени на действиях нет. Тап по карточке или кружку ставит/снимает отметку «выполнено» (✓ на зелёном кружке, текст приглушается; отметки хранятся). Содержание - только по карте: шаг 4 - время передачи сообщения, время и фамилии подтвердивших фиксирует помощник на обороте ДУ-61, он же контролирует отсчёт времени от остановки (оп. 4.3, 6.5); шаг 8 - укладка со стороны уклона, запреты, разнородный состав (определить груженые вагоны по натурному листу; рекомендация владельца: сначала класть башмаки под груженые вагоны - так их нужно меньше, норма груженого состава ниже порожнего; если под вагоны с нагрузкой на ось более 10 т башмаки уложить нельзя - считать по вагонам менее 10 т), переломный профиль (со стороны предполагаемого ухода, пропорционально весу частей и уклону), ручные тормоза при нехватке башмаков, доклад ДСП/ДНЦ, запись в ТУ-152; шаг 9 (метка «Для машиниста и помощника», пункты без должностей) - аварийная схема, радиопереговоры ведёт помощник, заход в высоковольтную камеру - «если для устранения нужен» (токоприёмники опущены, напряжения нет по вольтметру). Очерёдность «сначала под груженые» - рекомендация владельца (в памятке не указана). Шаг 5 выполняет помощник целиком (по решению владельца; в карте красные огни, оп. 5.1, закреплены за машинистом). Красная строка «Отпуск тормозов… ЗАПРЕЩЕНО»; примечание про предварительный расчёт и бланк ДУ-61. Блок «СООБЩЕНИЯ ПО РАДИО»: кнопка «Определить место по GPS» (запрашивает геолокацию, если разрешения нет; положение проецируется на ось ближайшего участка с километровыми отметками тем же механизмом, что профиль пути, - `HoldingPositionResolver`/`ProfileModel`; нужны GPS, вход в аккаунт и участок рядом; ждёт до ~20 с; при неудаче под кнопкой причина: «Положение по GPS не определено», «войдите в аккаунт», «участок рядом не найден»; координата не сохраняется) подставляет километр и пикет в сообщения (хранятся вместе с остановкой, «Сбросить» очищает), и две карточки текстов без полей ввода и без «Копировать»: «Об остановке» (оп. 4.1 карты) и «Требование вспомогательного локомотива» (оп. 7.2; время - из «Остановились», «ЧЧ час ММ мин»). В тексты подставлено только известное приложению: номер поезда и локомотив (из «Поезда», пока остановка не запущена; во время остановки - запомненные), время, километр и пикет; остальное (фамилия, перегон, путь, причина, секция) - «…», машинист называет сам. Тексты - `BrakeHoldingRadio` (тесты `BrakeHoldingRadioTest`).

**Не реализовано.** Смешанный состав; запись в ДУ-61; iOS и PWA. Подстановка км/пк по GPS на эмуляторе проверена только по ветке «участок не найден» (участки с сервера нужны вход и сеть).

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
| 16 Отвлечения | `AbsenceScreen.kt` | `AbsenceViewModel.kt` |
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


### Главная: плитки Локомотив / Поезд / Пассажиром и шторка списка (дополнение)
- При ≥ 2 элементах тап по плитке открывает шторку-список; в шторке чекбокс **«Всегда открывать последний»**
  (раздельно для loco/train/passenger, SharedPreferences `home_units_prefs`, ключ `always_open_last_<тип>`).
  Включён → тап по плитке сразу открывает последний элемент; список — тап по бейджу-счётчику на плитке (в самой шторке пояснения про счётчик нет).
  «Последний» и порядок в шторке-списке — как в форме маршрута: локомотивы по времени приёмки, поезда по отправлению первой
  станции, без времени — в конце в порядке добавления (`locomotivesInOrder`/`trainsInOrder`); пассажиры — как в списке. Плитка показывает последний.
- Подсказки станций (поезд, пассажиром, прочая работа) = `settings.stationList` ∪ имена справочника станций;
  совпадение — с начала названия или с начала любого слова (`stationNameMatches`).
- «Пункт оборота» в быстром просмотре и коннекторе «Отдых в ПО» = `routeEndStation(route)`: самая поздняя по времени
  конечная станция среди поездов и поездок пассажиром (после пассажира в дом отдыха — станция прибытия пассажиром).
- Быстрый просмотр маршрута (долгий тап): блок «Напарники · N» (ФИО, таб. №, примечание); вагонник — в карточке поезда.

### Настройки → Станции (справочник): кнопка «Добавить станцию»
- То же для «Серии локомотивов» («Добавить серию»).
- Кнопка не в конце списка, а плавающая у нижнего края (`ScrollHidingBottomBar`, анимация 600 мс как у нижней панели формы
  «Поезд»): прокрутка списка вверх — уезжает вниз, прокрутка назад — возвращается; в конце списка оставлен запас высоты 80 dp.

### Настройки → Основные (дополнение)
- Группа «Главный экран»: три переключателя «Локомотив / Поезд / Пассажиром — Всегда открывать последний»
  (`HomeUnitPrefs`, те же ключи `home_units_prefs`, что и чекбокс в шторке плитки): так галку можно снять, когда шторка
  списка больше не открывается.
- Настройки → Учёт → «Ночные часы»: в подсказке и в шторке указано, что время задаётся по МСК.

### Развёрнутый виджет: «На работе» в реальном времени
- При текущем маршруте с явкой в блоке маршрута — строка «НА РАБОТЕ» + прошедшее время **«Ч:ММ» (без секунд)** от явки
  (`WKeys.CURRENT_WORK_START_MS`, `basicData.timeStartWork`). Реализация: `TextClock` формата `H:mm` в подобранном
  часовом поясе `GMT±hh:mm` = −явка (`WidgetRender.elapsedClockZone`, покрыто тестом) — обновляется сам раз в минуту,
  без перерисовки виджета (RemoteViews). Ограничение: смена короче 24 часов.
  Строка занимает место скрытой строки «ТЕКУЩИЙ МАРШРУТ · №» (когда показаны кнопки записи), поэтому высота виджета не растёт;
  без кнопок показывается вместе с ней. Нет текущего маршрута / явка в будущем — строки нет.
- **Текущий маршрут без поезда** (карточка «ЯВКА»): в строке «ЯВКА» справа — «НА РАБОТЕ Ч:ММ» (тот же `TextClock`, что и в блоке с поездом),
  если явка уже наступила; ниже 150 dp слово «ЯВКА» убирается, а счётчик остаётся.
- **Карточки в меню установки**: развёрнутый (`widget_preview`) — норма сверху, снизу «На работе 5:12», направление, кнопки «Прибытие»/«Отправление»
  и «Автозапись», между ними растяжка (высота карточки зависит от лаунчера); малый (`widget_small_preview`) — время, дельта, индикатор, «норма», серая точка Автозапись.
  Оба повторяют настоящий вид (в живом виджете время — битмап шрифта ExtraBold, поэтому в карточках оно — картинка `widget_preview_metric*.png`,
  сгенерированная из `jetbrains_mono_extrabold.ttf` и окрашенная `android:tint`: TextView с этим шрифтом в лаунчере рисуется тонким); при смене вида виджета обновлять и их.
- **Адаптация к высоте** (`SizeMode.Exact`, `HeightTier.of(высота, состояние, есть ли поезд)`): RemoteViews не скроллит, а обрезает,
  поэтому по мере уменьшения высоты виджета убирается второстепенное (проверено в хосте на 110…400 dp при ширине 330 dp).
  Ярусы для «текущий маршрут с поездом» (для блоков явки/отдыха пороги сдвинуты на разницу высот: «Следующий» без направления 0, с направлением +35 dp,
  «Текущий без поезда» +35, «Отдых в ПО» +10, «Домашний отдых» +105; поэтому индикатор нормы в «Следующем маршруте» виден уже от ≈270 dp): ≥270 dp — всё; ≥230 — без индикатора и строки «норма»; ≥185 — «компакт» (время 30 sp,
  поля 12 dp, без разделителей и подписи поезда, кнопки записи ниже); ≥150 — ещё и без «На работе»; ниже — блок нормы скрыт.
  Для остальных состояний в «компакте» блоки сжимаются (меньше отступы и цифры; в «Домашнем отдыхе» нет «Минимальный отдых до»),
  а ниже 160 dp (для «Домашнего отдыха» — 200) пропадают подписи («ЯВКА», «ОТДЫХ В ПО», «ПРОДОЛЖИТЕЛЬНОСТЬ», «Закончится»,
  направление явки, «до …» в плитках), у текущего маршрута без поезда — кнопка «Автозапись». Состояние «без маршрута» норму не скрывает никогда.
  Кнопки записи: подписи 9,5 sp, время 14 sp — «23:48» помещается при ширине 330 dp.

### Тосты при отсутствии сети (Android и PWA)
- Любая сетевая ошибка (нет соединения / таймаут / «не удалось связаться») в тосте показывается **одним коротким «Нет сети»**,
  без деталей шагов. Android: `SnackbarManagerImpl` сводит такие сообщения к «Нет сети» и отбрасывает такой же повтор в течение 6 с
  (массовая синхронизация маршрутов без интернета больше не даёт череды тостов). PWA: `runFullSync` не показывает тост на каждый
  упавший шаг, а в конце даёт один «Нет сети» (только при ручной синхронизации, `silent=false`); `toast.show` игнорирует тот же текст,
  пока он на экране. Прочие ошибки (ответ сервера) показываются как раньше.

### График скорости: жд-километры (23.10.2026)

Подписи оси «Путь» на полноэкранном графике скорости показывают не километры от начала записи, а **железнодорожные километры по карте участка** (с учётом ручной поправки «Уточнить км»): точка трека на соответствующем расстоянии проецируется на ось выбранного участка профиля (дальше 300 м от оси — не определяется), к позиции добавляется поправка и по отметкам километража получается км (`LiveProfile.absKmAt`, подпись с одним знаком после запятой). Деления остаются на равных расстояниях по пути; если км не определился хотя бы в одном делении (нет выбранного участка, точка вне оси, у участка нет отметок км) или график открыт из «Истории» (режим просмотра, участок по положению не выбирается), все подписи — км от начала записи, как раньше.

### Иконка «Карта» (23.10.2026)

Плитка «Карта» на Главном (§4) и кнопка карты в нижней панели формы «Поезд» (§6.2) используют одну иконку `ic_map_pin_color` — серый план с тремя белыми дорогами и метка с тенью (по присланному рисунку). Иконка одноцветная с разной прозрачностью (план бледнее, метка плотная, дороги и отверстие метки прозрачные) и красится в цвет, как остальные иконки: на Главном — цветом иконок плиток, в форме «Поезд» — основным цветом приложения. Старые `ic_train_map_24` и `ic_train_map_color_24` не используются.

### Пройденный путь: серый пунктир только на настоящих разрывах записи (23.10.2026)

Линия пройденного пути на карте строится по упрощённой ломаной (прореживание `TrackAnalysis.decimate` + `PathCleaner`: на прямом участке остаются только вершины на поворотах и при смене 10-км/ч «корзины» скорости, соседние вершины могут отстоять на минуты). Раньше любой отрезок с интервалом между вершинами > 90 с (`DATA_GAP_MS`) рисовался серым пунктиром «нет данных» или не рисовался вовсе, поэтому на прямых участках появлялись «пробелы» при правильно записанной скорости, а на хвосте у положения линия то появлялась, то пропадала при каждом пересчёте. Теперь «нет данных» рисуется только там, где между СОСЕДНИМИ записанными (полными, не упрощёнными) точками разрыв > 90 с и ≤ 5 мин (`PathGaps.of(track.points + живые точки)`, `PathGaps.spans`): отрезок упрощённой линии, накрывающий такой разрыв, — серый пунктир (если смещение согласуется со скоростью ≤ 200 км/ч), все остальные — сплошные, цвет по скорости. Разрыв > 5 мин по-прежнему разрывает линию.

### Профиль пути: вне путей, индикатор GPS (23.10.2026)

- Пока участок рядом не найден (нет ни одного в радиусе 2 км) или положение ушло с оси выбранного участка, раз в 60 с (и при сдвиге не меньше 100 м) приложение спрашивает сервер, на путях ли точка (`POST /v1/service_phase/stations/nearest`, `alongRail=true`, радиус 500 м; ответ `off_rails` — дальше ~60 м от рельсов). Если точка вне путей: на графике/в полосе «Вы находитесь вне путей» (вместо «Для этого места профиль пока не собран…» и «Вы вне этого участка…»), в строке полосы красное «вне путей». Нет сети или лимит запросов — прежние тексты. Положение уходит только в этом запросе и не логируется.
- Индикатор связи со спутниками (плашка слева на карте): у иконки и подписи («GPS» / «Помеха GPS») горизонтальные отступы 8 dp и вертикальные 2 dp, подпись «Помеха GPS» не упирается в края плашки.
- **Чип плохой связи на «Профиле пути» (30.10.2026, `GnssBadChip`).** При плохой связи показывается компактный чип: «Слабый GPS» (жёлтый), «Нет GPS» (красный), «Помеха GPS» (оранжевый) со значком-штрихами; при хорошем сигнале и «поиске спутников» его нет (строки не прыгают). Место: только полноэкранный профиль — отдельной строкой **под верхней строкой с участком/плечом** (над кнопками масштаба и графиком); в мини-полосе «Профиль пути» чипа нет. Уровень — тот же `GnssQualityMonitor`, что у индикатора на карте.


### Профиль пути: замер пройденного расстояния «Линейка» (25.10.2026, кнопки — 27.10.2026)

В шапке полноэкранного профиля (рядом с «Сменить направление») — кнопка-линейка (только иконка). Она ничего не запускает: нажатие показывает панель замера в строке «Сейчас» (кнопка подсвечена), повторное — прячет панель и сбрасывает замер. На панели справа: пройденное расстояние (до 1000 м — целые метры, дальше — км с одним знаком; до старта «0 м»), под ним «поезд N м» (длина выбранного поезда, если указана) и кнопка по состоянию: **«Старт»** (замера нет или он сброшен) → **«Стоп»** (замер идёт, число растёт) → **«Сброс»** (остановлен, число заморожено) → «Старт». «Старт» запоминает текущее место как начало (без фикса GPS ничего не происходит); «Стоп» перестаёт считать, число и пунктирная вертикаль начала на графике остаются; «Сброс» убирает замер. Когда пройдено не меньше длины поезда, число красится в акцент (хвост вышел со стрелки / с места ограничения).

Расчёт: если начало замера было привязано к оси участка и участок не сменился, то расстояние = |позиция по оси сейчас − позиция по оси в начале| (точно на кривых); иначе (нет привязки, ушли на другой участок, вне пути) — расстояние по прямой между точками начала и текущей (в плашке пометка «по прямой»). Состояние хранится в `ProfileModel` (живёт, пока открыта карта), на диск и сервер не пишется, контракт не затронут.

### Журнал проследования ограничений (28.10.2026, только Android; клиент + сервер)

Машинист привязывает предупреждения участка (§6.9 «ДУ-61») к поезду; пока включён «Автозапись», приложение само записывает, **когда и с какой
скоростью** поезд прошёл каждое ограничение. Данные нужны в конце поездки и позже (разбор, отчёт).

**Что фиксируется (запись `RestrictionPass`, ключ — `trainId` + `warningId`).** Снимок предупреждения (вид, подпись «ограничение 40 км/ч»,
станция/перегон, примечание, ограничение скорости, границы в железнодорожных км и в км оси) и данные проследования:
- **заход головой** — время (мс, UTC) и скорость (км/ч) в момент, когда голова поезда пересекла границу зоны (интерполяция между двумя соседними фиксами
  по положению на оси); если первый же фикс уже внутри зоны — заход по этому фиксу, `enterApprox = true` (в отчёте «по первому фиксу в зоне»); если первый фикс уже за зоной —
  запись остаётся «не проследовано» (момент неизвестен);
- **выход всем составом** (только у ограничения скорости) — время и скорость в момент, когда **хвост** (голова + длина поезда из карточки поезда, `conditionalLength`
  в метрах: ×14 м, у пассажирских номеров — ×`passengerWagonLengthMeters`) вышел за дальний край зоны; длина не указана — выход по голове, `exitByHead = true`;
- **максимальная скорость**, пока поезд (хотя бы частью) в зоне, и `exceeded` — максимум выше ограничения больше чем на **1 км/ч** (допуск на шум GPS-скорости,
  `RestrictionPass.EXCEED_TOLERANCE_KMH`);
- у «Опускания», «Выключения тока» и «Прочего» — только заход (время, скорость); запись считается проследованной по заходу.
В записи **нет координат**: только время, скорость и км/пикет.

**Когда ловится.** В фоновом конвейере «Автозапись» (`AutoRecordManager.onPoint` → `RestrictionJournalRecorder`), для поезда-цели «Автозапись», на каждый фикс, **не только при открытой карте**.
Положение на оси участка считает `LiveProfile` (с ручной поправкой оси профиля, `ProfileCalibrationStore`), участок берётся **только из кэша на устройстве**
(`SectionProfileRepository.cached`, без сети и квот), повтор поиска отсутствующего участка — раз в минуту. Направление проезда — по смещению головы вдоль оси (≥ 30 м).
Состояние машины — сама запись в БД (заход/выход/максимум пишутся при событии и не чаще раза в 20 с по максимуму), поэтому **переживает убитый процесс**; в памяти только предыдущая точка и направление.
Согласие №1 для журнала **не нужно** ни на устройстве, ни на сервере (согласие регулирует сохранение пути/треков; журнал — производные данные без координат: время, скорость, км/пикет); тексты согласий не менялись. На сервер журнал уходит только при активной подписке «Машинист Pro» (клиентский гейт, как у треков).

**Цель записи — текущая цель «Автозапись» на каждом фиксе** (`AutoRecordManager.onPoint` читает `target_train_id` заново, а не запоминает поезд при старте). Смена цели: если «Автозапись» выключили на одном поезде и включили на другом (или цель сменилась в пути), запись идёт на новом; незавершённые ограничения скорости прежнего поезда (голова зашла, выход не записан) **закрываются**: время выхода — последний фикс прежнего поезда (если процесс перезапускался — метка последнего обновления записи), скорость — с этого фикса, `exitByHead = true`; иначе они висели бы «Состав в зоне» вечно и блокировали отчёт. Записи «Ожидает проследования» не трогаются. Закрытие делается при первом фиксе нового поезда (в том числе после перезапуска процесса).

**«Действующие» привязаны к поезду.** Галочка «Действующие» в экране «Предупреждения» теперь персистентна: она создаёт/снимает запись журнала поезда
(`ProfileModel.attachToTrain/detachFromTrain`; при открытии участка галочки подтягиваются из журнала, окно «Предупреждения на участке — применить прошлые?» выбранные также привязывает к поезду).
Добавление нового предупреждения привязывает его к поезду сразу. Проследованное предупреждение **нельзя снять галочкой** (чекбокс неактивен); убрать такую запись можно только в журнале поезда с подтверждением.
Правка предупреждения обновляет снимок записи, данные проследования сохраняются; удаление предупреждения с участка не удаляет записи с данными. Без поезда (карта без маршрута) галочки остаются в памяти, как раньше.

**Экран «Ограничения поезда»** (кнопка-треугольник в нижней панели «Поезд», полноэкранный, поворачивается по датчику): шапка (назад, «Ограничения поезда», подпись поезда); строка **«ПЛЕЧО»** (текущее плечо поезда «А — Б» или «не выбрано» и кнопка «Выбрать плечо» / «Сменить»; см. ниже «Выбор плеча на экранах ограничений»); кнопки **«Добавить»**
(только если у поезда выбрано плечо; иначе неактивна и подсказка «Чтобы добавлять ограничения, выберите плечо поезда: участок определяется по нему.») и **«Отчёт PDF»** (активна, когда журнал не пуст - в любое время, решение 28.10.2026;
если не все записи проследованы - подсказка «Отчёт можно сформировать в любое время: непроследованные ограничения будут без времени (осталось проследовать: N)»); список карточек: заголовок (+ «ПРЕВЫШЕНИЕ» красным), станция/перегон и км/пикет,
«Заход головой: дд.мм чч:мм:сс · V км/ч», у ограничения скорости «Выход составом: …» и «Максимум: V км/ч при ограничении L км/ч», статус («Ожидает проследования» / «Состав в зоне» / «Проследовано»),
«Убрать» (диалог «Убрать ограничение из поезда?», деструктивный; для записи с данными — «Записанные время и скорость проследования будут удалены.»). Время — в поясе отображения пользователя (`displayTimeZone`).
Список обновляется раз в 2 с, пока экран открыт (запись идёт в фоне).
**«Добавить» (29.10.2026)** профиль пути не загружает: сеть не нужна, лимиты сервера на загрузку профилей не тратятся (сервер: на аккаунт 120 загрузок участков в час и 400 в сутки, на IP 120 в минуту и 600 в час). Плеча нет — сообщение «Выберите плечо поезда…» с кнопками «Выбрать плечо» и «Ввести место вручную». Плечо есть — сразу шторка «ДУ-61: где действует» (`ModalBottomSheet`, как и форма): список станций плеча (включая промежуточные с координатами) и перегонов между соседними («А — Б»); нет станций с координатами — концы плеча. Кнопки «Другое место» и «Закрыть». Выбор места открывает шторку формы.
**Шторка формы (`ModalBottomSheet`, на весь рост)** — порядок полей: вид (4 вида), «Станция или перегон» (подставлено выбранное место), скорость «Ограничение, км/ч» (для вида «Ограничение», сразу под местом), «Начало, км» и «Начало, пикет» (без подсказки-заполнителя; одна строка), «Конец, км» и «Конец, пикет» (ниже), примечание (для «Прочее» обязательно). **Начало обязательно** (пикет 1–10, по умолчанию 1); конец не обязателен — тогда ограничение действует на один пикет (100 м), под полями подсказка «Конец не указан — предупреждение действует на одном пикете (100 м).».
**Один интерфейс из любого места (29.10.2026).** ДУ-61 добавляется и правится одной и той же шторкой `RestrictionFormSheet` (поля и порядок выше) из профиля пути, карты и раздела «Поезд»; списки мест одинаковые (строки «СТ»/«ПЕР» + название) и берутся из одного источника — станций плеча поезда (полный список с координатами): на профиле показываются те, что лежат на оси участка (проекция координат на ось, до 500 м от неё), перегоны — между соседними; плеча нет или на оси нет его станций — станции самого участка. Различается только место сохранения: с участком (профиль/карта) предупреждение проверяется по оси («Такого километра нет на этом участке.», «Место за пределами участка.»), попадает в хранилище участка и в журнал поезда; из «Поезда» без участка — в журнал поезда, на участок подцепляется при открытии профиля.
**Повторяющийся километраж.** Если километровая координата на участке встречается несколько раз (смена нумерации), `TrackWarnings.resolve` выбирает вхождение, ближайшее к месту предупреждения: границы места (если создано на профиле), иначе станция/перегон по названию: километр станции берётся по станциям плеча (проекция координат на ось участка — не зависит от написания названия и от того, есть ли станция в данных участка), затем по станциям участка; километр может относиться к соседнему месту — берётся ближайшее вхождение, а не только лежащее внутри. Конец зоны — вхождение, ближайшее к началу. Место на участке не найдено — первое вхождение по ходу участка.
**Плечо на экранах ДУ-61 (29.10.2026).** На всех экранах (раздел «Ограничения поезда», диалог «ДУ-61: где действует», экран ДУ-61 профиля пути и карты — из «Поезда» и с Главного) блок плеча один (`LegBlock`): строка «ПЛЕЧО» (выбрать/сменить) и под ней либо пометка **«Плечо проверено и найдено на карте»** (`ServicePhase.isMapVerified`: есть координаты обеих крайних станций, есть промежуточные станции и у всех есть координаты, расстояние измерено по карте именно по этим координатам — `measuredSignature`), либо текст «Плечо не проверено на карте…» и кнопка **«Определить»**. «Определить» одной кнопкой делает всё, что делает автоподбор справочника: подбирает путь, ставит расстояние по карте, координаты крайних станций, добавляет все найденные промежуточные СТАНЦИИ (с координатами и км; остановочные пункты не добавляются), добавляет их названия в справочник станций и сохраняет плечо тем же путём (`updateServicePhase`). Если названия неоднозначны (нужен выбор пользователя), открывается справочник (в «Поезде» — окно плеча с автоподбором, на карте с Главного — выбор плеча); при ошибке сети/сервера — сообщение под кнопкой.
**«Определить»: та же логика, что в справочнике (29.10.2026).** Запуск с теми же параметрами, что «Подобрать станции» в редакторе плеча: уже есть станции с координатами — путь строго через них; нет — сначала выбор маршрута, если путей несколько. Результат применяется целиком (станции без остановочных пунктов, расстояние по карте, координаты крайних, названия в справочник). Если автоматически не получилось (сложный маршрут, несколько путей, неоднозначные названия, путь не проложен) — открывается справочник в форме этого плеча. Если у поезда выбрано плечо, не проверенное на карте, и профиль пути на карте пуст, к пояснению добавляется подсказка «Плечо не проверено на карте: определите его в «Ограничениях поезда»…».
**«Определить»: переход в справочник только с согласия (29.10.2026).** Если автоматически не вышло, показывается диалог «Плечо требует уточнений» с причиной: «Между станциями плеча несколько путей: нужно выбрать нужный маршрут.», «Название станции «X» подходит нескольким станциям на карте: нужно выбрать нужную.», «Станция «X» не найдена на карте: у неё нет координат, их нужно указать.», «Между станциями плеча не удалось проложить путь по рельсам: станция может стоять не на той ветке, а маршрут быть слишком сложным.» и кнопками «Открыть справочник» / «Не сейчас»; без справочника на экране (карта с Главного) — «Понятно» и просьба уточнить плечо в «Поезде». Сетевые отказы (нет сети, лимит, занятость сервера) показываются сообщением под кнопкой, без перехода.
**Нельзя ставить ограничения там, где километраж неизвестен (29.10.2026).** Место, попавшее в серую зону профиля (`noData`) или за край оси участка, не принимается: форма на профиле/карте — ошибка «На этом участке режимная карта уточняется: километраж здесь неизвестен, поставить ограничение нельзя. Свяжитесь с поддержкой, чтобы уточнить информацию.»; ограничение из «Поезда» при открытии профиля не подцепляется к участку (остаётся в журнале без участка), а на карте показывается диалог с местом в заголовке («Не поставлено: Станция/перегон», при нескольких — «Не поставлено ограничений: N» и список мест) и тем же текстом (раз за запуск приложения на запись). Ранее подцепленное в такую зону ограничение при открытии профиля снимается с участка и остаётся в журнале без участка.
**Ограничения из «Поезда» на карте (29.10.2026).** Записи журнала поезда с участком подмешиваются в действующие под ключом хранения «участок + направление + плечо» (по нему профиль читает действующие), запись журнала, которой нет в хранилище участка, восстанавливается — все ограничения раздела «Поезд», лежащие на участке с известным километражем, видны на профиле и карте.
**Построение маршрута по своим станциям (редактор плеча):** нижняя панель «Через какие станции построить?» поднимается над клавиатурой, её высота ограничена свободным местом (поле ввода не перекрывается).
**Список ограничений: порядок и действия (29.10.2026).** Порядок — по ходу движения: по месту ограничения на плече (станция — её номер среди станций плеча, перегон — между номерами его концов); внутри места — по километру начала **в сторону движения** (если километраж по ходу поезда убывает, то по убыванию; сторону определяет `kmDecreasesAlongTravel`: наклон км начала по номеру места на плече, а при одном месте - сторона оси у проследованных записей; данных нет - по возрастанию) (30.10.2026); ограничения, чьего места нет на плече («Другое место»), — в конце. Нажатие на элемент открывает редактирование той же шторкой ДУ-61 (`RestrictionFormSheet` с «Удалить»); у записи с участком правка километров снимает привязку к участку (подцепится заново при открытии профиля), остальные правки обновляют запись и предупреждение участка. Удаление — свайпом влево как у маршрутов на Главном (красная кнопка «Удалить», затем подтверждение «Убрать ограничение из поезда?»).
**Элемент списка ограничений:** строка 1 — место (станция или перегон), справа жирным скорость (у ограничения скорости; красная при превышении); строка 2 — «км пк начала — км пк конца»; у опускания токоприёмника, выключения тока и прочего ниже жирным вид (у «Прочее» — текст); после проследования ниже время и скорость захода головой, у ограничения скорости — и выхода составом. Больше ничего; нажатие на элемент — «Убрать ограничение из поезда?».
**Запись.** Создаётся в журнале поезда без участка (`sectionId = "none"`, `RestrictionPass.NO_SECTION`; на сервер уходит как обычная запись) и привязана к рейсу поезда.
Поведение: фоновый детектор такие записи пропускает (без оси заход не определить), статус «Не отслеживается автоматически (нет профиля участка)», в журнале и в PDF запись есть («Заход головой: не отслеживается автоматически (нет профиля участка)»), отчёт не блокирует
(готовность считается по отслеживаемым записям; если все записи без участка — отчёт доступен сразу). Когда подходящий плечу участок позже откроется (профиль/карта) и километр записи есть на оси участка, запись **подцепляется** к участку: предупреждение
сохраняется в хранилище участка (видно на профиле и карте), запись журнала получает `sectionId` и дальше отслеживается автоматически (`ProfileModel.adoptManualRestrictions`). С километром вне участка запись остаётся как есть.

**Загрузка промежуточных станций плеча (28.10.2026).** Если у выбранного плеча в справочнике нет промежуточных станций, на экране «Ограничения поезда» под строкой «ПЛЕЧО» показывается строка «У плеча не загружены промежуточные станции…» с кнопкой «Загрузить».
Кнопка закрывает экран ограничений и открывает **то же окно редактирования плеча** (`ShoulderEditBottomSheet`, что и в справочнике «Плечи» → «Редактировать»), с автозапуском автоподбора станций (`autoStartAutoFill`, запускается только если станций ещё нет): тот же `ServicePhaseAutoFillViewModel`, тот же индикатор и выбор маршрута,
то же сохранение (`onSave` → `onUpdateServicePhase` → `TrainFormViewModel.updateServicePhase`: те же поля плеча, тот же путь записи в справочник и в поезд, та же дедупликация и порядок). После закрытия окна экран ограничений открывается снова. Если станции уже загружены — строка не показывается.

**Отчёт PDF** (`RestrictionPdfGenerator`, A4, много страниц; общий `PdfActionSheet`: «Открыть PDF» / «Поделиться» — стандартный Android share через `FileProvider`, файл в `cacheDir`, имя `restrictions_<дата>.pdf`, без персональных данных):
шапка «Проследование ограничений», подпись поезда и дата формирования; карточка на ограничение (название, станция/перегон и км, заход головой, выход составом, ограничение и максимальная скорость, «ПРЕВЫШЕНИЕ»);
итог «Всего ограничений: N», «Превышений нет» / «Превышений скорости: M»; подвал «не является официальным документом», номера страниц. В файле нет координат, имени и почты.

**Локальное хранение.** Таблица `RestrictionPass` в `Track.db` (SQLDelight, миграция `3.sqm`, чисто новая таблица; `Route.db` не затронута), `dirty = 1` — не доставлено на сервер.
Снятие с поезда и удаление поезда/рейса (`TrainUseCase.removeTrain`, `RouteUseCase.deleteRoute/markAsRemoved`) делают **надгробия** (`isDeleted`), доставленные надгробия чистятся.

**Синхронизация (`RestrictionPassSyncer`, тем же `TrackUploadWorker`, что и треки; только вошедший пользователь; нужна подписка «Машинист Pro», как у треков).**
Сначала отправка изменённых записей пачками по 100, затем догрузка изменений (`since` = метка последней загруженной записи − 24 ч на расхождение часов), слияние по LWW (`updatedAt` больше — побеждает).
Записи рейса, которого ещё нет на сервере (`route_unavailable`), и упёршиеся в квоту остаются неотправленными до следующего прогона.

**Контракт сервера (аддитивный: новая таблица `restriction_pass` + новый эндпоинт; существующие эндпоинты, `SyncData` и старые клиенты не затронуты).**
`POST /v1/restriction_passes/` (JWT) — `{"passes": [<до 100 записей>]}` → `{"accepted": n, "skipped": [{"trainId","warningId","reason"}]}`, причины: `invalid` (запись не прошла проверку), `bad_time` (`updatedAt` дальше чем на сутки в будущем),
`route_unavailable` (рейс не найден или чужой), `quota` (больше 20 000 записей), `stale` (на сервере не старее). Согласие №1 не требуется (403 `consent_required` у этого эндпоинта **нет**). 413/422/429 как у треков, значения полей в ответ и в лог не попадают.
`GET /v1/restriction_passes/?limit<=100&cursor=&since=&routeId=` → `{"passes": [...], "nextCursor": str|null}` (записи только текущего пользователя, **включая надгробия**, порядок по `updatedAt`).
Поля записи (camelCase, время — `Long` мс UTC): `trainId`, `warningId`, `routeId`, `sectionId`, `kind` (`speed|pantograph|power_off|other`), `title`, `place`, `note`, `limitKmh`, `startAbsKm`, `endAbsKm`, `startRelKm`, `endRelKm`,
`enterAt`, `enterSpeedKmh`, `enterApprox`, `exitAt`, `exitSpeedKmh`, `exitByHead`, `trainLengthM`, `dir` (−1/0/1), `maxSpeedKmh`, `exceeded`, `updatedAt`, `isDeleted`.
Пример: `{"trainId":"…","warningId":"…","routeId":"…","sectionId":"…","kind":"speed","title":"ограничение 40 км/ч","limitKmh":40,"enterAt":1761660309000,"enterSpeedKmh":41.5,"exitAt":1761660429000,"exitSpeedKmh":38.0,"maxSpeedKmh":44.0,"exceeded":true,"updatedAt":1761660429000}`.
Владелец определяется токеном; запись принимается только для рейса пользователя (`Route.user == user.id`); ключ `(user_id, trainId, warningId)`; удаление рейса удаляет журнал каскадом (FK на `route`), `trainId`/`warningId` без FK (поезда пересоздаются при каждом сохранении рейса).

### Профиль пути: предупреждения на участке (25.10.2026)

В шапке полноэкранного профиля — кнопка «ДУ-61» (треугольник, `onClickLabel` «ДУ-61»), видна всегда. Пока участок не выбран/не определён, нажатие показывает подсказку «Чтобы добавить ДУ-61, выберите участок.»; с участком открывает окно. Кнопка жёлтая, если на участке действуют предупреждения.

**Экран «ДУ-61» (до 28.10.2026 назывался «Предупреждения»; классы и ключи хранения не менялись).** Это отдельный полноэкранный экран (не карточка-диалог): отступы под системные панели и клавиатуру. Ориентация — по положению устройства: на время показа экран снимает горизонтальную блокировку профиля (`SCREEN_ORIENTATION_FULL_SENSOR`, вертикаль и горизонталь в любую сторону), по выходу профиль снова горизонтальный; кнопки «Повернуть» нет. В вертикали заголовок и кнопки идут двумя строками. Кнопки шапки (31.10.2026): «Добавить» и «Голос» стоят вместе слева, «Готово» - отдельно у правого края (в горизонтали между ними промежуток 24 dp). То же для формы редактирования (из карточки на карте и из зоны на профиле). Сверху название участка, кнопки «Голос» (настройки озвучки) и «Готово». Между названием экрана и названием участка — отступ 6 dp. Ниже — «Действующие» (весь список поезда из журнала, в том же порядке и тем же элементом `RestrictionCard`, что в «Ограничениях поезда»; свайп влево — удалить с подтверждением, нажатие — правка: на этом участке формой профиля с проверкой по оси, на другом участке плеча или без участка — той же шторкой и тем же путём, что в «Ограничениях поезда»), «Прошлые на этом участке» (история участка; нажатие — сделать действующим, свайп — стереть из истории) и список станций и перегонов участка по ходу движения («СТ …», «ПЕР А — Б»). Тап по станции/перегону открывает форму: начало и конец — км и пикет (1–10); **у перегона поля км/пикета не предзаполняются** (28.10.2026), вместо этого под заголовком подсказка «Перегон: 138 км 1 пк — 139 км 5 пк»; у станции предзаполнено только начало, конец пуст; тип — «Ограничение» (по умолчанию; тогда обязательна скорость, км/ч), «Опускание» (токоприёмники), «Выключение тока», «Прочее» (обязательно примечание); примечание у остальных типов необязательно. «Сохранить» записывает предупреждение и возвращает к списку станций и перегонов для следующего; «Готово» закрывает окно. У участка без отметок км поля км/пикета не показываются, предупреждение занимает весь выбранный отрезок. **Конец можно не вводить:** если «Конец, км» пуст (или совпал с началом), предупреждение действует на одном пикете — 100 м от начала пикета (для станции поле конца пустое по умолчанию). Проверки: км и пикет — числа, пикет 1–10, место должно существовать на участке.

**Хранение.** На устройстве (SharedPreferences `track_warnings`, JSON по ключу «id участка + направление (+/−) + плечо (первая и последняя станции плеча без регистра/знаков; без плеча пусто)», `ProfileModel.warningsKey`: id, начало/конец в железнодорожных км и в км оси, тип, скорость, примечание, название места); на сервер не уходят, контракт и БД не затронуты. Сохранённые предупреждения остаются между поездками. Предупреждения разных плеч и направлений не смешиваются: список «Добавленные» и предложение показывают только записанные для того же плеча и направления (записи, сделанные до 28.10.2026 под ключом «id участка», больше не предлагаются). Когда плечо и направление встречаются снова и действующих на них нет, показывается диалог «Предупреждения на участке» со списком прошлых (галочки у каждого, по умолчанию все): «Применить» включает отмеченные, «Не нужны» — ни одно. Новое предупреждение сразу действует.

**Отображение.** На профиле пути (полоса и полный экран): жёлтый фон зоны на всю высоту графика; подпись (30.10.2026) — маленькая белая карточка с жёлтой рамкой и указателем вниз, как на карте, но короче: у ограничения скорости только число («40»), у остальных видов текст («опустить токоприёмники», «выключить ток» или примечание); карточка стоит по центру видимой части зоны и может быть шире самой зоны (внутри зоны подпись не помещалась), у краёв графика прижимается к ним (`TrackWarning.chartLabel`); на карте — жёлтая линия по рельсам и подпись-«диалог»: белая карточка с жёлтой рамкой и указателем («Ограничение 40 км/ч», ниже «138 км 1 пк — 139 км 5 пк» (без метров, 30.10.2026)), поставленная в середине зоны. Ось участка редкая (точки через километры), поэтому линия по оси уходила бы с путей: рельсовые линии берутся из слоёв подложки (`queryRenderedFeatures` по слоям стиля с «rail» в id, без «transit»), точки линии зоны прижимаются к рельсам не дальше 250 м от оси, причём идут по ОДНОМУ пути: пока текущий путь не дальше лучшего больше чем на 40 м, остаёмся на нём (параллельные пути и стрелки не смешиваются, линия одна), на конце куска (тайл, разветвление) берётся путь, чья проекция ближе к прежней точке; скачки больше `3×шаг+150 м` разрывают линию; пересчёт при остановке камеры и когда тайлы дозагрузились. Нет рельсов на экране — рисуется линия по оси.

**Голос.** Системный TextToSpeech (русский). «Голос» в окне ДУ-61 открывает **шторку** (31.10.2026; раньше диалог) с заголовком «Голос ДУ-61» и крестиком; внизу закреплены «Проверить» и «Готово»: включена/выключена, за сколько до начала зоны начинать (500 м – 3 км, по умолчанию 1,5 км), повтор (один раз / 15 / 30 / 60 с, по умолчанию 30 с), скорость речи, выбор русского голоса, «Проверить». Фраза (30.10.2026): «Через 500 метров ограничение скорости на 40» — без «километров в час» (расстояние округляется до 50 м). Озвучиваются действующие предупреждения впереди по ходу при уверенной привязке. **Заход и выход (28.10.2026, `WarningPassTracker`):** когда голова поезда зашла в зону — «Ограничение скорости на 40» (у «Опускания»/«Выключения тока»/«Прочего» — сама инструкция: «Опустите токоприёмники», «Выключите ток», текст примечания; выхода у них нет); у ограничения скорости, когда зону проследовал весь поезд (хвост, т.е. голова + длина поезда из карточки, вышел за дальний край; длина не указана — по голове), — «Конец ограничения скорости на 40». Каждое событие один раз за проезд; первый фикс внутри зоны даёт заход, первый фикс уже за зоной — тишина; скачок через короткую зону между фиксами даёт оба события подряд; смена участка, направления или правка/снятие галочки предупреждения начинает отсчёт заново. **Приоритет и очередь (30.10.2026, `WarningSpeechQueue`):** сообщения не перебивают друг друга, а встают в очередь и говорятся по приоритету: 1) заход на место ограничения, 2) лежащее впереди («Через N метров…»), 3) выход всем составом, 4) «Больше предупреждений нет»; внутри приоритета — по порядку поступления, одинаковое сообщение, уже стоящее в очереди, не дублируется. **«Больше предупреждений нет»:** когда в этом такте закончилось последнее из действующих предупреждений (все проследованы: у ограничения скорости — выход всем составом, у остальных видов — заход), один раз звучит «Больше предупреждений нет»; если все были уже позади при старте — тишина. Озвучка заходов подчиняется тому же выключателю «Голос». Настройки — SharedPreferences `warning_voice`.

**Единый источник ограничений (29.10.2026).** Действующие ограничения поезда — только журнал поезда (`RestrictionJournalStore`): его читают «Ограничения поезда», экран ДУ-61, профиль, карта, озвучка и фоновая запись «Автозапись». Добавить, править и убрать ограничение можно из любого места — везде видно одно и то же. На профиле и карте рисуются те из них, что лежат на выбранном участке (`sectionId`); ограничения без участка с известными км подцепляются к подходящему плечу участку сами (`ProfileModel.reconcile`), журнал перечитывается опросом раз в 2 с (его меняют и другие экраны). Прошлые ограничения хранятся отдельно, по участку (`TrackWarningStore`, SharedPreferences `track_warnings`, ключи «участок|направление|плечо» и прежние «участок» читаются вместе): действующие ограничения участка всегда попадают в историю; снятие с поезда историю не стирает, из истории ограничение снова делают действующим. Предложение «ДУ-61 на участке: сделать их действующими?» строится из истории, когда на участке у поезда ничего не действует. Предложение показывается только если плечо указано в разделе «Поезд» (минимум две станции, `ProfileModel.warningOffer`): при автоопределении участка по положению (начало смены, поезда ещё нет) направление может быть неверным, поэтому прошлые ограничения не предлагаются (30.10.2026). Ответ на предложение («Применить» или «Не нужны») запоминается на связку «поезд + участок» (`OfferDeclineStore`, SharedPreferences `profile_offer_declined`): пока поезд тот же, предложение не повторяется ни после перезапуска, ни при повторной прокладке маршрута. Список «Прошлые» на профиле собирается по всему участку, но **предложение** (окно на карте) берёт из него только записи плеча поезда (`LegWarnings.matches`): место записи - станция плеча либо перегон «А — Б» с обеими станциями на плече **в том же порядке** (обратное направление не предлагается). Так по общему участку при плечах Лужская-Луга, Лужская-Будогощь, Лужская-СПб каждому поезду предлагаются записи общих перегонов и лежащие дальше по его плечу.
**ДУ-61 из «Поезда» (30.10.2026).** Плечо не выбрано - под подсказкой строка «Выберите плечо для подбора предупреждений из прошлых поездок». Плечо выбрано и есть подходящие прошлые записи (вся история по всем участкам, `TrackWarningStore.loadEvery`, фильтр `LegWarnings`, ещё не в журнале поезда) - первой строкой списка карточка «Из прошлых поездок»: записи по ходу движения с галочками (по умолчанию не отмечены) и кнопка «Добавить выбранные (N)»; выбранные попадают в журнал поезда (`RestrictionPass.planned`, с участком из записи), карточка исчезает, когда подходящих не осталось. **Прошлые ограничения — общий блок (30.10.2026, `PastRestrictionsBlock`).** Одинаково выглядит и работает на обоих экранах ДУ-61 («Ограничения поезда» — заголовок «Из прошлых поездок»; ДУ-61 на карте/из профиля — «Прошлые на этом участке»): заголовок с кнопкой «Скрыть» / «Показать» (в свёрнутом виде — заголовок со счётчиком; состояние одно на оба экрана, помнится на устройстве: `restrictions_ui` / `past_hidden`), под ним строки — отдельные карточки `RestrictionItem` (как в профиле) с чекбоксом слева (тап по карточке тоже переключает; в ДУ-61 на карте остаётся свайп «убрать из прошлых»), внизу списка кнопки «Выбрать все», «Отменить» (снять выбор) и «Добавить выбранные (N)». По умолчанию ничего не отмечено; «Выбрать все» неактивна, когда отмечено всё, «Отменить» и «Добавить выбранные» — пока ничего не отмечено. Раньше на карте тап по прошлому сразу делал его действующим — теперь только через «Добавить выбранные». **Шторки «Где действует» и выбор участка плеча:** прокрутка списка не тянет шторку (`separateSheetFromScroll`) — при пролистывании вверх шторка не «прыгает», закрывается только за ручку/шапку.

**Карта рисует все ограничения поезда (29.10.2026).** Плечо поезда состоит из нескольких участков, поэтому на карте показываются все действующие ограничения поезда с участком, а не только выбранного на профиле: недостающие участки подгружаются (`ProfileModel.mapWarnings`, `loadWarningSections`), каждое ограничение раскладывается по оси своего участка. Нажатие на карточку открывает правку: ограничение выбранного участка — формой профиля, другого участка — шторкой «Ограничений поезда». Без участка и без км (место «Волосово — Кикерино» без километров) на карте нет — только в списках.

**Расстояние до цели и скорость — запасной расчёт по режимной карте (29.10.2026).** Основной расчёт — вдоль линии плеча с сервера (положение не дальше 50 м от неё). Когда он не даёт расстояния (положение дальше 50 м от линии, линии нет, положения по линии нет), но положение на выбранном участке профиля определено, расстояние до станции считается вдоль оси участка: станция должна лежать не дальше 500 м от оси и впереди по ходу (`ProfileModel.axisDistanceTo`, учитывается ручная поправка по оси). Это работает для списка цели, расчёта прибытия и экрана «Скорость»; станции, которых нет на оси режимной карты, в запасном расчёте не участвуют.

**Полноэкранный профиль: шапка, масштаб, длина поезда (29.10.2026).** Все элементы верхней строки — «Назад», карточка участка (только название и «Сменить», без подписи «по местоположению»), масштабы, замер, направление, ДУ-61 — одной высоты 38 dp. Масштабы: 6 км, 12 км, «Весь участок» (пресета «3 км» нет). Нижняя строка: слева номер поезда (нажатие — выбор поезда), рядом длина. Длина из данных поезда приоритетна и здесь не правится; если в данных её нет, длина акцентного цвета, нажатие открывает ввод «Длина, м» (10–3000), значение хранится на устройстве (`TrainLengthStore`, SharedPreferences `profile_train_length`, ключ — id поезда) и используется для линии поезда на профиле и озвучки выхода из зоны; как только длина появится в данных поезда, используется она. «Голос ДУ-61» вертикально: заголовок над кнопками «Проверить» и «Готово». Список «Цель» имеет нижний отступ (навигационная панель + 24 dp): последние станции доступны.

**Профиль плеча внутри участка (07.11.2026).** Если выбранное плечо поезда лежит ВНУТРИ участка (обе крайние станции плеча есть на оси, а плечо не занимает весь участок: концы дальше 0,3 км от краёв, длина не меньше 0,3 км), профиль показывает только плечо: от первой до последней станции плеча (`LegMatch.axisRange` -> `LiveProfile.legAxisRange`; границы вида `viewFrom/viewTo`, ширина `viewSpanKm`). Это касается полноэкранного профиля, мини-профиля на карте и масштаба «Весь участок» (он становится «весь отрезок плеча»); окно, жесты и прокрутка не выходят за плечо. Положение, километраж, ограничения и разворот по-прежнему считаются по ВСЕМУ участку. Плеча нет, оно не на оси участка или совпадает с участком - показывается весь участок, как раньше. Диапазон пересчитывается при каждом обновлении состояния (станции плеча могут появиться позже участка).

**Загрузка участков и лимиты (29.10.2026).** Сервер ограничивает загрузки участков (на аккаунт 120 в час и 400 в сутки), считаются и отклонённые запросы. Правила клиента (`SectionProfileRepository`): участок качается один раз и лежит в зашифрованном кэше, пока у него не изменился `updatedAt` в списке; участки без геометрии (`hasGeometry = false`) не качаются никогда (раньше каждый подбор участков «рядом» тратил запрос на каждый такой участок); участок, отданный без оси, запоминается и повторно не запрашивается, пока не изменится `updatedAt`; после отказа 429 загрузки участков приостанавливаются на 10 минут (повторы только продлевают блокировку), в паузе работает кэш. Подбор участка плеча без положения (`ProfileModel.ensureLeg`) не качает ничего, если участок уже выбран по положению, и берёт участки у конца плеча только когда у начала подходящего не нашлось. Холодный запуск на новом устройстве качает участки плеча, поэтому повторные чистые установки на нескольких устройствах одного аккаунта быстро упираются в лимит.

**Названия и шторка «Где действует» (29.10.2026).** Главный экран ограничений в разделе «Поезд» называется «ДУ-61» (раньше «Ограничения поезда»); так же подписана иконка-треугольник на нижней панели «Поезда». Добавление — одна шторка «Где действует» (`WhereSheet`: плечо, подсказка, список станций «СТ» и перегонов «ПЕР», «Другое место» только в «Поезде») из раздела «Поезд» и из профиля пути (кнопка «Добавить» на экране ДУ-61 профиля; места — с оси участка); выбранное место открывает шторку формы `RestrictionFormSheet`. Из профиля список мест больше не лежит на самом экране. **Значок с числом** на иконке-треугольнике нижней панели «Поезда» (`Badge`): число действующих ограничений поезда (записей журнала, обновляется раз в 2 с, «99+» от 100); нет ограничений — значка нет.

**Пометка «не на карте» (29.10.2026).** Нанесётся ли ограничение на карту, определяется без сети (`RestrictionPlacement`, `rememberPlacements`): по списку участков (габарит ± 1 км, участок с профилем) и сохранённым на устройстве участкам; лимиты загрузок сервера не тратятся, пометка появляется сразу после добавления. Состояния: на карте (привязано к участку) — пометки нет; «Нет профиля участка — на карту не нанесётся» (в этом месте нет участка с профилем), «Километраж здесь неизвестен — на карту не нанесётся» (серая зона профиля; определяется, только если участок уже сохранён на устройстве), «Без километров — на карту не нанесётся» (красная плашка — выделено). Если участок рабочий, но ограничение ещё не привязано к нему (`PENDING`), пометки нет: оно встанет на карту само, когда профиль участка откроется (30.10.2026: прежняя ложная пометка «карта пути этого участка в разработке» убрана). Место, не совпавшее со станциями плеча («Другое место»), и пустой список участков — без пометки. Пометка показывается в «ДУ-61» раздела «Поезд» и на экране ДУ-61 профиля.

**Подпись места в элементе ограничения (29.10.2026):** километр и пикет без метров («124 км 2 пк — 124 км 3 пк»); на карточке карты и в строке положения профиля формат прежний.

**Профиль без положения на путях (29.10.2026).** Плечо поезда известно, а положения на участке нет (вне путей, GPS ещё не поймал) — `ProfileModel.ensureLeg` подгружает участки у крайних станций плеча и выбирает подходящий плечу (`LegMatch`), профиль показывается сразу, без ожидания положения; как только положение найдено рядом с другим участком, автоподбор переключает на него.

**Редактирование.** Нажатие на карточку предупреждения на карте или на жёлтую зону на профиле (полоса и полный экран) открывает форму этого предупреждения («ДУ-61: предупреждение»): те же поля, что при создании, плюс «Удалить»; «Отмена» закрывает без изменений, «Сохранить» заменяет предупреждение (id сохраняется, признак «Действующие» не меняется). Зона на один пикет показывается с пустым концом. На узком (портретном) экране заголовок и кнопки идут в две строки, типы переносятся.
