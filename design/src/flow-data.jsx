// flow-data.jsx — дерево навигации «Машинист».
// buildFlow(platform, dark) -> { root }. Один источник для всех 4 схем;
// файлы различаются только флагами platform / dark.
//
// Узел: { id, label, w, h, edge, note, render, children, clusterLabel }
//   edge  — подпись стрелки от родителя (кнопка/действие)
//   render(platform, dark) -> JSX экрана в натуральную величину
//   clusterLabel — заголовок пунктирной рамки (только узлы 1-го уровня)

const W = 390; // ширина телефона

// helper: выбрать компонент по платформе
const pick = (ios, android) => (platform) => (platform === 'ios' ? ios : android);

function buildFlow(platform, dark) {
  const isIOS = platform === 'ios';

  // ── ЛОКОМОТИВ (внутри Маршрута) ──────────────────────────────
  const Loco = isIOS ? IOSScreenLocomotiveV2 : AndroidScreenLocomotiveV2;
  const locoDiesel = {
    id: 'loco-diesel', label: 'Локомотив · тепловоз', edge: 'Добавить локомотив',
    w: W, h: 1620,
    render: (p, d) => <Loco dark={d} traction="diesel" height={1620}/>,
    children: [
      { id: 'loco-collapsed', label: 'Экипировка свёрнута', edge: 'свернуть Экипировку',
        w: W, h: 1480, render: (p, d) => <Loco dark={d} traction="diesel" height={1480} supplyExpanded={false}/> },
      { id: 'loco-sheet-section', label: 'Шторка k секции', edge: 'тап «k секции»',
        w: W, h: 1620, render: (p, d) => <Loco dark={d} traction="diesel" height={1620} sheet={{ kind: 'section', value: '0.83' }}/> },
      { id: 'loco-sheet-supply', label: 'Шторка k экипировки', edge: 'тап «k экипировки»',
        w: W, h: 1620, render: (p, d) => <Loco dark={d} traction="diesel" height={1620} sheet={{ kind: 'supply', value: '0.91' }}/> },
      { id: 'loco-electric', label: 'Локомотив · электровоз', edge: 'тип тяги → электровоз', note: '2 секции',
        w: W, h: 1820, render: (p, d) => <Loco dark={d} traction="electric" height={1820}/> },
      { id: 'time-arrival', label: 'Шторка времени · Приёмка', edge: 'тап времени приёмки',
        w: W, h: 844, render: (p, d) => <SheetWrap dark={d} platform={p} kind="arrival"/>,
        children: [
          { id: 'time-no-station', label: 'Станция не выбрана', edge: 'станция не выбрана',
            w: W, h: 844, render: (p, d) => <SheetWrap dark={d} platform={p} kind="arrival" stationEmpty={true}/> },
          { id: 'time-no-norms', label: 'Нет норм серии и станции', edge: 'нормы не заданы',
            w: W, h: 844, render: (p, d) => <SheetWrap dark={d} platform={p} kind="arrival" noSeriesNorm={true} noStationNorm={true}/> },
          { id: 'time-departure', label: 'Шторка времени · Сдача', edge: 'вкладка «Сдача»',
            w: W, h: 844, render: (p, d) => <SheetWrap dark={d} platform={p} kind="departure"/> },
        ] },
    ],
  };

  // ── ДОБАВЛЕНИЕ ПОЕЗДА (только iOS) ───────────────────────────
  const addTrainNode = {
    id: 'add-train', label: 'Добавление поезда', edge: 'Добавить поезд',
    w: W, h: 920, render: (p, d) => <IOSScreenAddTrain dark={d} height={920}/>,
    children: [
      { id: 'add-train-shoulders', label: 'Шторка «Плечи»', edge: 'тап «Плечи»',
        w: W, h: 920, render: (p, d) => <IOSScreenAddTrain dark={d} height={920} sheet="shoulders"/> },
      { id: 'add-train-settings', label: 'Настройки поезда', edge: 'Настройки поезда', note: 'свёрнуто',
        w: W, h: 920, render: (p, d) => <IOSScreenAddTrain dark={d} height={920} sheet="settings"/>,
        children: [
          { id: 'add-train-settings-exp', label: 'Настройки поезда', edge: 'развернуть', note: 'развёрнуто',
            w: W, h: 920, render: (p, d) => <IOSScreenAddTrain dark={d} height={920} sheet="settings-expanded"/> },
        ] },
      { id: 'station-edit', label: 'Шторка станции', edge: 'тап станции в маршруте',
        w: W, h: 920, render: (p, d) => <StationEditSheetWrap dark={d} platform={p} mode="edit"/>,
        children: [
          { id: 'station-new', label: 'Новая станция', edge: '＋ новая станция',
            w: W, h: 920, render: (p, d) => <StationEditSheetWrap dark={d} platform={p} mode="new"/> },
        ] },
    ],
  };

  // station-edit для Android живёт прямо под Маршрутом (нет экрана «Поезд»)
  const stationEditAndroid = {
    id: 'station-edit', label: 'Шторка станции', edge: 'тап станции в маршруте',
    w: W, h: 920, render: (p, d) => <StationEditSheetWrap dark={d} platform={p} mode="edit"/>,
    children: [
      { id: 'station-new', label: 'Новая станция', edge: '＋ новая станция',
        w: W, h: 920, render: (p, d) => <StationEditSheetWrap dark={d} platform={p} mode="new"/> },
    ],
  };

  // ── МАРШРУТ (редактор смены) ─────────────────────────────────
  const Route = isIOS ? IOSScreenRoute : AndroidScreenRoute;
  const Pass = isIOS ? IOSScreenPass : AndroidScreenPass;
  const route = {
    id: 'route', label: 'Маршрут', edge: 'Кнопка ＋ · новый маршрут', clusterLabel: 'Маршрут',
    w: W, h: 844, render: (p, d) => <Route dark={d}/>,
    children: [
      locoDiesel,
      ...(isIOS ? [addTrainNode] : [stationEditAndroid]),
      { id: 'pass', label: 'Следование пассажиром', edge: 'Добавить пассажиром',
        w: W, h: 920, render: (p, d) => <Pass dark={d} height={920}/> },
    ],
  };

  // ── МАСТЕР «ЗАПОЛНИТЬ МЕСЯЦ» (внутри Календаря) ──────────────
  const wizard = {
    id: 'wiz-pattern', label: 'Мастер · Шаг 1', edge: 'Заполнить месяц', note: 'паттерн 2/2',
    w: W, h: 852, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={1} pattern="2-2"/>,
    children: [
      { id: 'wiz-step2-standard', label: 'Мастер · Шаг 2', edge: 'Далее', note: 'паттерн 2/2',
        w: W, h: 852, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={2} pattern="2-2" firstDay={1}/> },
      { id: 'wiz-custom', label: 'Мастер · Шаг 1', edge: 'Свой цикл', note: 'свой цикл',
        w: W, h: 1080, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={1} pattern="custom" height={1080}/>,
        children: [
          { id: 'wiz-edit', label: 'Редактирование смен', edge: 'редактировать смены',
            w: W, h: 1240, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={1} pattern="custom" editMode={true} height={1240}/>,
            children: [
              { id: 'wiz-picker', label: 'Пикер типа дня', edge: 'тап типа дня',
                w: W, h: 1260, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={1} pattern="custom" pickerIndex={0} height={1260}/> },
              { id: 'wiz-confirm-day', label: 'Удалить день?', edge: 'удалить день',
                w: W, h: 1260, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={1} pattern="custom" pickerIndex={0} confirm="day" confirmDay={1} height={1260}/> },
            ] },
          { id: 'wiz-confirm-pattern', label: 'Удалить паттерн?', edge: 'удалить паттерн',
            w: W, h: 1080, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={1} pattern="custom" confirm="pattern" height={1080}/> },
          { id: 'wiz-step2-preview', label: 'Мастер · Шаг 2', edge: 'Далее', note: 'предпросмотр',
            w: W, h: 852, render: (p, d) => <ScheduleWizardScreen platform={p} dark={d} step={2} pattern="custom" firstDay={1}/> },
        ] },
    ],
  };

  // ── КАЛЕНДАРЬ / ГРАФИК ───────────────────────────────────────
  const calMonth = {
    id: 'cal-month', label: 'Календарь · месяц', edge: 'Инструменты → График', clusterLabel: 'Календарь · График',
    w: W, h: 844, render: (p, d) => <CalendarVariantE platform={p} dark={d} selected={15} mode="expanded"/>,
    children: [
      { id: 'cal-week', label: 'Календарь · неделя', edge: 'свернуть в неделю',
        w: W, h: 844, render: (p, d) => <CalendarVariantE platform={p} dark={d} selected={15} mode="collapsed"/> },
      { id: 'cal-day-absence', label: 'День · отвлечение', edge: 'тап дня с отвлечением',
        w: W, h: 844, render: (p, d) => <CalendarVariantE platform={p} dark={d} selected={26} mode="expanded"/> },
      { id: 'cal-day-empty', label: 'День · пусто', edge: 'тап пустого дня',
        w: W, h: 844, render: (p, d) => <CalendarVariantE platform={p} dark={d} selected={14} mode="expanded"/> },
      { id: 'cal-sheet', label: 'Что добавить?', edge: 'Кнопка ＋',
        w: W, h: 844, render: (p, d) => <CalendarVariantE platform={p} dark={d} selected={15} mode="expanded" initialSheetOpen={true}/>,
        children: [
          { id: 'route-add-1', label: 'Маршрут · выбор времени', edge: 'Маршрут', note: 'шаг 1',
            w: W, h: 844, render: (p, d) => <RouteAddScreen platform={p} dark={d}/>,
            children: [
              { id: 'route-add-2', label: 'Маршрут · выбор дат', edge: 'выбрал время', note: 'шаг 2',
                w: W, h: 844, render: (p, d) => <RouteAddScreen platform={p} dark={d} initialTime="20:00"/>,
                children: [
                  { id: 'route-add-3', label: 'Готово · 3 даты', edge: 'отметил 3 даты',
                    w: W, h: 844, render: (p, d) => <RouteAddScreen platform={p} dark={d} initialTime="20:00" initialDates={[24, 28, 30]}/> },
                  { id: 'route-add-single', label: 'Готово · 1 дата', edge: 'одна дата',
                    w: W, h: 844, render: (p, d) => <RouteAddScreen platform={p} dark={d} initialTime="07:45" initialDates={[27]}/> },
                ] },
              { id: 'route-add-custom', label: 'Своё время', edge: 'Своё время', note: 'клавиатура',
                w: W, h: 844, render: (p, d) => <RouteAddScreen platform={p} dark={d} initialCustomOpen={true}/> },
            ] },
          { id: 'abs-range', label: 'Отвлечение · диапазон', edge: 'Отвлечение',
            w: W, h: 844, render: (p, d) => <AbsenceFlowScreen platform={p} dark={d} rangeStart={15} rangeEnd={20} type="vacation"/>,
            children: [
              { id: 'abs-single', label: 'Отвлечение · 1 дата', edge: 'одна дата',
                w: W, h: 844, render: (p, d) => <AbsenceFlowScreen platform={p} dark={d} rangeStart={15} rangeEnd={15} type="vacation"/> },
              { id: 'abs-picker', label: 'Выбор типа', edge: 'тап типа отвлечения',
                w: W, h: 844, render: (p, d) => <AbsenceFlowScreen platform={p} dark={d} rangeStart={15} rangeEnd={20} type="courses" pickerOpen={true}/> },
            ] },
        ] },
      wizard,
    ],
  };

  // ── НАСТРОЙКИ И СПРАВОЧНИКИ ──────────────────────────────────
  const Settings = isIOS ? IOSScreenSettings : AndroidScreenSettings;
  const SettingsGeneral = isIOS ? IOSScreenSettingsGeneral : AndroidScreenSettingsGeneral;
  const SeriesList = isIOS ? IOSSeriesList : AndroidSeriesList;
  const SeriesEditor = isIOS ? IOSSeriesEditor : AndroidSeriesEditor;
  const StationsList = isIOS ? IOSStationsList : AndroidStationsList;
  const StationEditor = isIOS ? IOSStationEditor : AndroidStationEditor;
  const settings = {
    id: 'settings', label: 'Настройки', edge: 'Вкладка «Настройки»', clusterLabel: 'Настройки · справочники',
    w: W, h: 1700, render: (p, d) => <Settings dark={d} height={1700}/>,
    children: [
      { id: 'settings-general', label: 'Основные', edge: 'Основные',
        w: W, h: 1000, render: (p, d) => <SettingsGeneral dark={d} height={1000}/> },
      { id: 'series-list', label: 'Серии локомотивов', edge: 'Серии локомотивов',
        w: W, h: 844, render: (p, d) => <SeriesList dark={d}/>,
        children: [
          { id: 'series-edit', label: 'Редактор серии', edge: 'тап серии · ＋',
            w: W, h: 844, render: (p, d) => <SeriesEditor dark={d}/> },
        ] },
      { id: 'stations-list', label: 'Станции', edge: 'Станции',
        w: W, h: 844, render: (p, d) => <StationsList dark={d}/>,
        children: [
          { id: 'station-editor', label: 'Редактор станции', edge: 'тап станции · ＋',
            w: W, h: 844, render: (p, d) => <StationEditor dark={d}/> },
        ] },
    ],
  };

  // ── КОРЕНЬ: Поездки (главный экран) ──────────────────────────
  const Trips = isIOS ? IOSScreenTrips : AndroidScreenTrips;
  const root = {
    id: 'trips', label: 'Поездки', note: 'главный экран',
    w: W, h: 1120, render: (p, d) => <Trips dark={d}/>,
    children: [route, calMonth, settings],
  };

  return { root };
}

Object.assign(window, { buildFlow });
