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

- **Время** — `Long`, миллисекунды от Unix epoch (UTC). Отображение — с
  учётом тайм-зоны пользователя (строка вида `GMT+3`), конвертация только на
  уровне UI/DTO.
- **Месяц в API** — 0-based (январь = 0). Конвертация только в DTO-маппере.
- **Нормы времени** — `Int`, **минуты**. Значение `0` трактуется как
  «не задано» → хранится как `null`. Нигде не сохранять `0` как норму.
- **UUID** — строки, генерируются клиентом (`generateId()`), сервер не
  присваивает.
- **Числа топлива/энергии** — `Double`, но по сети сериализуются строками
  (наследие; не терять дробную часть).
- **Реактивность** — репозитории отдают `Flow`; UI подписывается и
  перерисовывается при изменениях (везде, где сказано «getAllFlow()», на iOS —
  эквивалент: наблюдаемый источник, публикующий обновления).

### Доменные модели (сущности)

```
Locomotive(
  locoId, basicId, remoteObjectId?,
  series?: String, number?: String, type: LocoType = ELECTRIC,
  electricSectionList: [SectionElectric], dieselSectionList: [SectionDiesel],
  timeStartOfAcceptance?: Long, timeEndOfAcceptance?: Long,      // приёмка: начало, конец
  timeStartOfDelivery?: Long,   timeEndOfDelivery?: Long,        // сдача: начало, конец
  timeBarrierOut?: Long,   // приёмка: «Выход на КП»
  timeBarrierIn?: Long,    // сдача: «Заход на КП»
  acceptanceStationId?: String, deliveryStationId?: String,      // ссылки на StationNorm
  normaElectricCurrent1?: Double, normaElectricCurrent2?: Double,
  normaDiesel?: String,
  heatingCounterAccepted?/Delivery?: Double,                     // счётчик отопления
  auxiliaryCounterAccepted?/Delivery?: Double,                   // счётчик собственных нужд
)
LocoType = { ELECTRIC, DIESEL }

StationNorm(
  stationId, name,
  appearanceToStartMin?: Int,   // Явка → Начало приёмки
  endToBarrierMin?: Int,        // Окончание приёмки → Выход на КП
  barrierToStartMin?: Int,      // Заход на КП → Начало сдачи
  endToWorkEndMin?: Int,        // Окончание сдачи → Окончание работы
  updatedAt: Long
)

LocomotiveSeries(
  seriesId, name, type: LocoType,
  acceptanceDurationMin?: Int,      // «После отстоя»: длительность приёмки
  deliveryDurationMin?: Int,        // «После отстоя»: длительность сдачи
  acceptanceHandToHandMin?: Int,    // «Из рук в руки»: длительность приёмки
  deliveryHandToHandMin?: Int,      // «Из рук в руки»: длительность сдачи
  updatedAt: Long
)
```

Репозитории: `LocomotiveSeriesRepository`, `StationNormRepository`
(методы `getAllFlow()`, `getAll()`, `replaceAll(list)` — full-replace).

---

## 1. Экран «Локомотив» (FormLocoScreen)

Форма одного локомотива внутри маршрута. Открывается из FormScreen
(«ЛОКОМОТИВ» → карточка локомотива или «Добавить локомотив»).

### 1.1. Вход / выход и данные

- **Навигация**: `FormLocoDestination` c параметрами `locoId`, `basicId`.
  Колбэки навигации: `onNavigateToSeriesSettings`, `onNavigateToStationSettings`,
  `onEditStation(stationId)` → `router.showSettingsStationEditor`,
  `onEditSeries(seriesId)` → `router.showSettingsSeriesEditor`.
- **ViewModel**: `LocoFormViewModel` (Koin `viewModel { (locoId, basicId) -> ... }`).
  Наблюдаемые состояния:
  - `currentLoco: Locomotive?` — редактируемый локомотив.
  - `electricSectionListState / dieselSectionListState` — секции по виду тяги.
  - `routeStartWork: Long? / routeEndWork: Long?` — время явки/сдачи **маршрута**
    (из `basicData` через `RouteUseCase`); нужны шторке времени.
  - `uiState` — флаги (в т.ч. `changesHaveState`, `isShowUpdateHint`).
- **Загрузка**: по `locoId` через `locomotiveUseCase.getLocoById`; секции
  раскладываются по типу. Новый локомотив создаётся с одной секцией по
  умолчанию (тип из формы).

### 1.2. Сохранение (persistence)

- **Автосохранение с дебаунсом 500 мс**: любое изменение вызывает
  `changesHave()` → `triggerAutoSave()` (отменяет прошлую задачу, ждёт 500 мс,
  затем `saveLoco()` → `locomotiveUseCase.saveLocomotive`).
- **Guard пустого локомотива** (`isLocoEmpty`): полностью пустой локомотив
  (без серии/номера/времён/станций/счётчиков/данных секций) не сохраняется,
  чтобы «открыл-закрыл новую форму» не создавало мусор.
- iOS/PWA: повторить дебаунс-автосейв и guard пустого объекта.

### 1.3. Разделы UI (сверху вниз)

1. **Топ-бар**: «‹ Локомотив», справа иконка настроек (⚙, ведёт в настройки).
2. **ОСНОВНЫЕ ДАННЫЕ**
   - Переключатель вида тяги — сегмент из двух иконок: **капля** = Тепловоз
     (DIESEL), **молния** = Электровоз (ELECTRIC). `changeLocoType(type)`
     гарантирует минимум одну секцию нужного вида.
   - **Серия** — поле с автодополнением (`ExposedDropdownMenu`, список
     `dropDownSeriesMenuList`). Ввод → `onSeriesChanged(text)` → `setSeries`
     (`currentLoco.series`). ⚠️ Локальное состояние поля синхронизируется с
     `locomotive.series` через `LaunchedEffect(locomotive.series)` — чтобы
     смена серии из шторки времени сразу отражалась в поле (см. 2.12).
   - **Номер** — текст → `setNumber` (`currentLoco.number`).
3. **ВРЕМЯ** — карточка с двумя строками, каждая открывает шторку времени:
   - **ПРИЁМКА**: `начало` (`timeStartOfAcceptance`) → `конец`
     (`timeEndOfAcceptance`) → `КП` (`timeBarrierOut`). Тап → шторка
     `kind="acceptance"`.
   - **СДАЧА**: `КП` (`timeBarrierIn`) → `начало` (`timeStartOfDelivery`) →
     `конец` (`timeEndOfDelivery`). Тап → шторка `kind="delivery"`.
   - Отображение времени — через конвертер тайм-зоны.
4. **СЕКЦИИ** — счётчик `−/N/+` (добавить/удалить секцию текущего вида).
   Для каждой секции:
   - **Электровоз**: энергия принято/сдано (расход/рекуперация, при наличии
     «другого рода тока» — доп. поля), поля `SectionElectric`.
   - **Тепловоз**: топливо принято/сдано (`accepted/deliveryFuel`),
     коэффициент, экипировка (`refuel`/`refuelInKilo`, пересчёт л↔кг по
     коэффициенту), «Расход» по секции. Ввод экипировки/коэффициента — через
     нижние шторки `EnteredRefuelDialog`/`EnteredCoefficientDialog` (ввод в
     шторке, не в диалоге).
   - Переключатель единиц топлива Л/КГ.
5. **ИТОГО** — суммарный расход по локомотиву.
6. **Удалить** локомотив (нижняя панель / кнопка).

### 1.4. Запись времени из шторки

- Приёмка: `saveAcceptanceFromSheet(startTime, endTime, barrierOut, stationId)`
  → `currentLoco.copy(timeStartOfAcceptance, timeEndOfAcceptance,
  timeBarrierOut, acceptanceStationId)`.
- Сдача: `saveDeliveryFromSheet(barrierIn, startTime, endTime, stationId)`
  → `currentLoco.copy(timeBarrierIn, timeStartOfDelivery, timeEndOfDelivery,
  deliveryStationId)`.
- **Окончание работы** (сдача) пишется НЕ в локомотив, а в **маршрут**:
  `setTimeEndWork(value)` → `RouteUseCase.saveRoute(basicData.timeEndWork=value)`
  и обновляет `routeEndWork`. Вызывается из шторки колбэком
  `onTimeEndWorkChanged` (мгновенная синхронизация).

---

## 2. Шторка «Установка времени приёмки/сдачи» (TimeBottomSheet)

Нижняя шторка расчёта и ручного ввода времени приёмки **или** сдачи
локомотива. Один компонент, два режима: `kind = "acceptance" | "delivery"`.

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

Внутреннее состояние (mutable, инициализируется из initial*):
`startTime, endTime, barrierOut, barrierIn, workEnd(=routeEndWork),
selectedStation (из StationNormRepository по initialStationId),
selectedSeriesName`.

### 2.2. Смысл полей по режимам

| Строка (acceptance)      | Поле        | Норма-интервал (StationNorm)      |
|--------------------------|-------------|-----------------------------------|
| Явка (locked, из маршрута) | routeStartWork | —                              |
| Начало приёмки           | startTime   | appearanceToStartMin (явка→начало)|
| Окончание приёмки        | endTime     | *длительность серии* (приёмка)    |
| Выход на КП              | barrierOut  | endToBarrierMin (конец→КП)        |

| Строка (delivery)        | Поле        | Норма-интервал                    |
|--------------------------|-------------|-----------------------------------|
| Заход на КП              | barrierIn   | —                                 |
| Начало сдачи             | startTime   | barrierToStartMin (КП→начало)     |
| Окончание сдачи          | endTime     | *длительность серии* (сдача)      |
| Окончание работы         | workEnd     | endToWorkEndMin (конец→работа); пишется в маршрут |

### 2.3. Иконка статуса строки

- Время **задано** → зелёная галочка (`check_circle`, зелёный `#00B341`,
  фон — светло-зелёный).
- Время **не задано** → серые часы (`outline_access_time`).
- В режиме выбора якоря (ASKING) строка подсвечивается акцентом (мигание).
- Под «Явка» отдельной строкой подпись **«из маршрута»** (Явка не редактируется
  здесь; меняется только в форме маршрута).

### 2.4. Норма серии и вариант «После отстоя / Из рук в руки»

- Серия берётся из `LocomotiveSeriesRepository` по имени (`selectedSeries`).
- Переключатель варианта: `normHandToHand` (false = «После отстоя», true =
  «Из рук в руки»), запоминается в `SharedPreferencesRepositories`
  (`setLocoNormHandToHand`). Определяет, какая пара норм серии активна:
  - acceptance: `acceptanceDurationMin` / `acceptanceHandToHandMin`;
  - delivery: `deliveryDurationMin` / `deliveryHandToHandMin`.
- **Отклонения в строке длительности** («+N мин норма» / «на X мин больше/
  меньше») считаются относительно **применённого** варианта
  `appliedHandToHand`, а не текущего тумблера. Поэтому переключение тумблера
  НЕ мигает красным.
- При переключении тумблера, если время задано и норма нового варианта реально
  другая, показывается **диалог** «Другая норма — Пересчитать / Оставить»
  (`AppAlertDialog`). Пересчёт — только по согласию; если время не задано —
  диалог не показывается. После пересчёта `appliedHandToHand` = выбранный.

### 2.5. Кнопка «Рассчитать время» и выбор точки отсчёта (якоря)

Нижняя кнопка «Рассчитать время» (активна, если есть серия/станция и, для
сдачи, хотя бы одно время). Логика `applyNorms`:

- **Acceptance**: собираются присутствующие якоря `{Явка(если есть),
  Начало, Окончание, Выход}`.
  - 0 якорей → ничего;
  - 1 якорь → считаем сразу от него (`applyFromAcceptanceField`);
  - ≥2 → режим **ASKING**: баннер «Нажмите на момент времени, от которого
    рассчитать», строки мигают; тап по строке выбирает якорь.
- **Delivery**: якоря `{Заход, Начало, Окончание, Окончание работы}`; та же
  схема (0 / 1 / ASKING).
- **Явка НИКОГДА не пересчитывается** (она из маршрута). При расчёте «от начала
  приёмки» и позже время явки остаётся прежним, даже если интервал вне нормы.
- Расчёт вперёд/назад использует нормы станции (интервалы) и длительность серии.
  Отсутствующая норма интервала = 0 (интервал нулевой).
- Выбранный якорь запоминается (`accAnchor`/`delAnchor`) — для корректного
  пересчёта при смене варианта нормы.

### 2.6. Валидация последовательности

Красное «Время должно быть позже предыдущего» показывается, когда время
**меньше** предыдущего. Для интервалов, которые могут быть нулевыми (норма не
задана), используется строгое «<» (равенство — не ошибка):
- acceptance: «Выход на КП» vs «Окончание приёмки» — строгое `<`;
- delivery: «Начало сдачи» vs «Заход на КП» — строгое `<`;
- остальные интервалы — `<=` (равенство считается ошибкой).

### 2.7. Предупреждения (WarnItem, оранжевые инфо-блоки)

Порядок сборки:
- Серия не выбрана (нет CTA) / **«Нет нормы для серии …»** (CTA «Настроить
  серию»).
- Станция не выбрана / **«Нет нормы для станции …»** (обе относящиеся к
  операции нормы отсутствуют; CTA «Настроить станцию»; **закрывается ✕**).
- **«Нет нормы интервала: …»** — часть норм есть; сообщает, какого интервала
  нет; расчёт остаётся доступным (интервал = 0); **закрывается ✕**.
- Delivery без единого якоря — «Укажите время захода на КП или окончания
  работы».
- Закрытые крестиком предупреждения хранятся в `dismissedWarnings` (на сессию
  открытия шторки).
- **CTA «Настроить серию/станцию»** открывает редактирование **прямо в
  шторке-пикере** (см. 2.10), передавая `initialEditSeries/Station` = текущую
  серию/станцию. Шторка времени при этом НЕ закрывается.

### 2.8. Сохранение нормы серии/станции из шторки

Внизу появляются кнопки «Сохранить норму серии …» / «Сохранить норму станции …»
когда введённые времена дают интервалы, **отличные** от сохранённых норм:
- `canSaveSeriesNorm` — длительность (start→end) ≠ норма выбранного варианта.
- `canSaveStationNorm` — любой из относящихся к операции интервалов ≠ сохранённого.
  Сравнение нормализует значения: вычисленный интервал `≤0` и сохранённый `0`
  трактуются как `null` («не задано»), чтобы «0 == не задано» не считалось
  отличием.
- Клик → запись в репозиторий через `replaceAll` (full-replace списка).

### 2.9. Удаление значения времени

Долгое нажатие на строку с заданным временем → нижняя шторка подтверждения
`AppBottomSheet` с действием **«Удалить значение»** (как «время явки» в
FormScreen). Явка (locked) не удаляется. Для «Окончание работы» удаление также
сбрасывает pending-обновление и синхронизирует маршрут (`onTimeEndWorkChanged(null)`).

### 2.10. Пикеры серии/станции (внутри шторки)

`SeriesPickerSheet` / `StationPickerSheet` — вложенные нижние шторки:
- **Список** с поиском, группами и подписями норм. Тап по строке — **выбор**
  серии/станции. Справа **карандаш** — переход в редактирование.
- Подпись станции: `Приёмка A/B · Сдача C/D` (незаданный интервал = 0);
  полностью пустая — «Норма не задана».
- **Редактирование и создание — прямо в этой же шторке** (свап контента):
  карандаш → редактор существующей; «+ Добавить …» → форма создания («Новая
  станция/серия»). Кнопка **«Назад»** возвращает к списку (а не на форму
  локомотива). Редакторы — те же `SettingsStationEditorContent` /
  `SettingsSeriesEditorContent`, ViewModel через `koinViewModel(parametersOf(id, null))`.
- `initialEditStation/Series` (из CTA «Настроить …») открывает пикер сразу в
  редакторе нужного элемента.
- Изменения норм в редакторе сохраняются **немедленно** (каждое изменение
  поля → `save()` в VM), чтобы правки не терялись при быстром выходе.

### 2.11. Шапка и завершение

- Заголовок: «Приёмка» или «Сдача». Справа — **«Готово»** (просто синий текст):
  проверяет корректность и вызывает `onSave(TimeSheetResult)` + `sheetState.hide()`.
  При ошибке последовательности «Готово» неактивно.
- `onSave` в форме локомотива вызывает `saveAcceptanceFromSheet` /
  `saveDeliveryFromSheet` (см. 1.4).

### 2.12. Синхронизация серии обратно в форму

Смена/выбор серии в шторке (`onSeriesChanged` = `viewModel::setSeries`)
обновляет `currentLoco.series`; поле «Серия» на форме подхватывает изменение
через `LaunchedEffect(locomotive.series)` (не мешая ручному вводу).

### 2.13. История именования

Кнопка расчёта раньше называлась «Установить по ПЗВ» (плашка справа сверху);
сейчас: «Готово» — синий текст в шапке, а расчёт — нижняя кнопка «Рассчитать
время».

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
