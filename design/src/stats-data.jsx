// stats-data.jsx — демо-данные раздела «Статистика»
// Один источник правды для всех концептов и состояний.
//
// Дельты бывают двух типов:
//   • kind:'norm'  — ОЦЕНОЧНАЯ. Сравнивается факт с нормой → семантический
//                    цвет (red = превышение нормы, green = в норме,
//                    neutral = норма не задана). Используем у «Переработки».
//   • kind:'info'  — ИНФОРМАЦИОННАЯ. Просто больше/меньше прошлого периода,
//                    рост не «хорошо» и не «плохо» → монохром (никогда не
//                    красим в red/green по знаку). Стрелка показывает только
//                    направление изменения.

// ── Формат крупных чисел (сокращаем: 1,2 млн · 3,4 тыс.) ──────────────
function fmtRu(n) {
  // разряды неразрывным пробелом
  return n.toLocaleString('ru-RU').replace(/\u00A0/g, ' ').replace(/,/g, ' ');
}
function fmtCompact(n) {
  const abs = Math.abs(n);
  if (abs >= 1e6) return (n / 1e6).toFixed(1).replace('.', ',') + ' млн';
  if (abs >= 1e4) return (n / 1e3).toFixed(1).replace('.', ',') + ' тыс.';
  if (abs >= 1e3) return fmtRu(n);
  return String(n);
}
// Часы из минут → «205:05» или «+30:05»
function fmtHM(min, signed) {
  const sign = min < 0 ? '−' : signed ? '+' : '';
  const a = Math.abs(min);
  const h = Math.floor(a / 60);
  const m = a % 60;
  return `${sign}${h}:${String(m).padStart(2, '0')}`;
}

// ── Метрики за МАРТ 2026 (текущий) vs ФЕВРАЛЬ 2026 (предыдущий) ────────
// value — отформатированная строка; unit — единица; delta — изменение.
const STAT_METRICS = {
  worked:   {
    key: 'worked', label: 'Отработанное время', short: 'Время',
    value: '187:20', unit: '', icon: 'clock',
    delta: { kind: 'info', dir: 'up', abs: '+12:05', pct: '+7%' },
    prevValue: '175:15',
  },
  overtime: {
    key: 'overtime', label: 'Переработка', short: 'Переработка',
    value: '+11:20', unit: '', icon: 'gauge',
    // ОЦЕНОЧНАЯ: факт 187:20 при норме 176:00 → превышение → red
    delta: { kind: 'norm', state: 'over', abs: 'норма 176:00', pct: '+6%' },
    prevValue: '−0:45',
  },
  distance: {
    key: 'distance', label: 'Расстояние', short: 'Путь',
    value: '3,4', unit: 'тыс. км', icon: 'route',
    delta: { kind: 'info', dir: 'up', abs: '+420 км', pct: '+14%' },
    prevValue: '3,0 тыс. км',
  },
  tkm:      {
    key: 'tkm', label: 'Тонно-км брутто', short: 'Ткм брутто',
    value: '1,24', unit: 'млн ткм', icon: 'weight',
    delta: { kind: 'info', dir: 'down', abs: '−80 тыс.', pct: '−6%' },
    prevValue: '1,32 млн ткм',
  },
  night:    {
    key: 'night', label: 'Ночные часы', short: 'Ночные',
    value: '48:30', unit: '', icon: 'moon',
    delta: { kind: 'info', dir: 'up', abs: '+3:10', pct: '+7%' },
    prevValue: '45:20',
  },
  routes:   {
    key: 'routes', label: 'Маршрутов (смен)', short: 'Маршруты',
    value: '23', unit: '', icon: 'doc',
    delta: { kind: 'info', dir: 'flat', abs: '0', pct: '0%' },
    prevValue: '23',
  },
  earnings: {
    key: 'earnings', label: 'Заработано', short: 'Заработок',
    value: '142,8', unit: 'тыс. ₽', icon: 'ruble',
    delta: { kind: 'info', dir: 'up', abs: '+9 400 ₽', pct: '+7%' },
    prevValue: '133,4 тыс. ₽',
  },
  speed:    {
    key: 'speed', label: 'Средняя скорость', short: 'Скорость',
    value: '47,3', unit: 'км/ч', icon: 'speed',
    delta: { kind: 'info', dir: 'up', abs: '+1,8 км/ч', pct: '+4%' },
    prevValue: '45,5 км/ч',
  },
  transit:  {
    key: 'transit', label: 'Время в пути', short: 'В пути',
    value: '142:10', unit: '', icon: 'timer',
    delta: { kind: 'info', dir: 'up', abs: '+8:30', pct: '+6%' },
    prevValue: '133:40',
  },
  rider:    {
    key: 'rider', label: 'Следование пассажиром', short: 'Пассажиром',
    value: '18:25', unit: '', icon: 'seat',
    delta: { kind: 'info', dir: 'down', abs: '−2:15', pct: '−11%' },
    prevValue: '20:40',
  },
};

// Порядок карточек на главном
const STAT_ORDER = ['worked', 'overtime', 'distance', 'tkm', 'night', 'routes', 'earnings', 'speed', 'transit', 'rider'];

// ── Варианты периода-эталона для сравнения ───────────────────────────
// Выбранный период сравнивают с одним из этих. selected — текущий выбор.
const COMPARE_MONTH = [
  { id: 'prev',     title: 'Февраль 2026',  note: 'предыдущий месяц', selected: true },
  { id: 'yearago',  title: 'Март 2025',     note: 'этот месяц год назад' },
  { id: 'avg12',    title: 'Среднее за 12 мес', note: 'скользящее среднее' },
  { id: 'pick',     title: 'Выбрать месяц…', note: 'любой из истории', action: true },
  { id: 'none',     title: 'Не сравнивать', note: 'показать только выбранный период', off: true },
];
const COMPARE_YEAR = [
  { id: 'prev',     title: '2025 год',  note: 'предыдущий год', selected: true },
  { id: 'y2024',    title: '2024 год',  note: 'два года назад' },
  { id: 'first',    title: '2023 год',  note: 'первый год в приложении' },
  { id: 'pick',     title: 'Выбрать год…', note: 'любой из истории', action: true },
  { id: 'none',     title: 'Не сравнивать', note: 'показать только выбранный год', off: true },
];

// ── Источник для пикеров «выбрать месяц» / «выбрать год» ──────────────
const MONTH_NAMES_SH = ['Янв','Фев','Мар','Апр','Май','Июн','Июл','Авг','Сен','Окт','Ноя','Дек'];
const MONTH_NAMES_FULL = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];
// Текущий выбранный период — Март 2026. Сравнивать можно с любым месяцем ДО него.
const PICK_CUR = { y: 2026, m: 3 };
const PICK_MONTH_YEARS = [
  { year: 2026, upTo: 12 },
  { year: 2025, upTo: 12 },
  { year: 2024, upTo: 12 },
];
// Текущий выбранный год — 2026. Сравнивать можно с любым прошлым.
const PICK_YEARS = [2025, 2024, 2023];

// ── Итоги за ГОД по метрикам (2026 vs 2025) ──────────────────────────
// num — крупная цифра; unit — единица; prev — было (прошлый год); delta — год к году.
const YEAR_TOTALS = {
  worked:   { num: '2 122', unit: 'ч',       prev: '2 024 ч',      delta: { kind: 'info', dir: 'up',   pct: '+5%', abs: '+98 ч' } },
  overtime: { num: '+48:20', unit: '',        prev: '+42:10',       delta: { kind: 'norm', state: 'over', pct: '+2%', abs: 'норма 2 074 ч' } },
  distance: { num: '41,2',  unit: 'тыс. км',  prev: '39,8 тыс. км', delta: { kind: 'info', dir: 'up',   pct: '+4%', abs: '+1,4 тыс. км' } },
  tkm:      { num: '14,8',  unit: 'млн ткм',  prev: '13,9 млн ткм', delta: { kind: 'info', dir: 'up',   pct: '+6%', abs: '+0,9 млн' } },
  night:    { num: '583',   unit: 'ч',        prev: '548 ч',        delta: { kind: 'info', dir: 'up',   pct: '+6%', abs: '+35 ч' } },
  earnings: { num: '1,68',  unit: 'млн ₽',    prev: '1,54 млн ₽',   delta: { kind: 'info', dir: 'up',   pct: '+9%', abs: '+138 тыс. ₽' } },
  speed:    { num: '46,8',  unit: 'км/ч',     prev: '45,4 км/ч',    delta: { kind: 'info', dir: 'up',   pct: '+3%', abs: '+1,4 км/ч' } },
  transit:  { num: '1 640', unit: 'ч',        prev: '1 562 ч',      delta: { kind: 'info', dir: 'up',   pct: '+5%', abs: '+78 ч' } },
  routes:   { num: '243',   unit: '',         prev: '236',          delta: { kind: 'info', dir: 'up',   pct: '+3%', abs: '+7' } },
  rider:    { num: '214',   unit: 'ч',        prev: '230 ч',        delta: { kind: 'info', dir: 'down', pct: '−7%', abs: '−16 ч' } },
};

// ── Топ направлений за период (рейтинг: разы + часы) ──────────────────
// НЕ дельта-метрика — распределение по плечам обслуживания.
const TOP_DIRECTIONS = [
  { from: 'Лужская', to: 'СПб-Сорт.',   trips: 9, hours: '74:10', pct: 100 },
  { from: 'Бабаево',  to: 'Лужская',     trips: 6, hours: '52:40', pct: 71 },
  { from: 'Волховстрой', to: 'Бабаево',  trips: 5, hours: '38:25', pct: 52 },
  { from: 'СПб-Сорт.', to: 'Волховстрой', trips: 3, hours: '21:55', pct: 30 },
];

// ── Парные столбцы «текущий vs предыдущий» для блока графиков ─────────
// norm — есть ли оценка по норме (для цветовой логики столбца факта)
const STAT_BARS = [
  { key: 'worked',   label: 'Время, ч',     cur: 187, prev: 175, fmt: v => fmtHM(v * 60).replace(':00','') },
  { key: 'distance', label: 'Путь, км',     cur: 3420, prev: 3000, fmt: v => fmtCompact(v) },
  { key: 'tkm',      label: 'Ткм, млн',     cur: 1.24, prev: 1.32, fmt: v => v.toLocaleString('ru-RU').replace('.', ',') },
  { key: 'night',    label: 'Ночные, ч',    cur: 48.5, prev: 45.3, fmt: v => Math.round(v) },
  { key: 'routes',   label: 'Маршруты',     cur: 23, prev: 23, fmt: v => v },
];

// ── Помесячная динамика за год (отработанные часы) ───────────────────
// cur — 2026, prev — 2025; норма месяца — norm
const YEAR_MONTHS = [
  { m: 'Янв', cur: 168, prev: 159, norm: 160 },
  { m: 'Фев', cur: 175, prev: 164, norm: 168 },
  { m: 'Мар', cur: 187, prev: 171, norm: 176 },
  { m: 'Апр', cur: 182, prev: 178, norm: 176 },
  { m: 'Май', cur: 159, prev: 169, norm: 152 },
  { m: 'Июн', cur: 178, prev: 174, norm: 168 },
  { m: 'Июл', cur: 191, prev: 183, norm: 184 },
  { m: 'Авг', cur: 186, prev: 180, norm: 184 },
  { m: 'Сен', cur: 172, prev: 170, norm: 176 },
  { m: 'Окт', cur: 188, prev: 175, norm: 176 },
  { m: 'Ноя', cur: 165, prev: 168, norm: 160 },
  { m: 'Дек', cur: 181, prev: 177, norm: 184 },
];

// Спарклайн для карточек (6 последних периодов, нормализуем сами)
const SPARKS = {
  worked:   [159, 164, 158, 175, 181, 187],
  overtime: [-2, 4, -1, 0, 6, 11],
  distance: [2.8, 3.1, 2.7, 3.0, 3.2, 3.42],
  tkm:      [1.18, 1.30, 1.22, 1.32, 1.28, 1.24],
  night:    [42, 46, 41, 45, 47, 48.5],
  routes:   [22, 24, 21, 23, 23, 23],
  earnings: [121, 128, 119, 133, 138, 142.8],
  speed:    [44.1, 45.0, 44.6, 45.5, 46.8, 47.3],
  transit:  [121, 128, 124, 133, 138, 142],
  rider:    [24, 21, 23, 20.6, 19, 18.4],
};

// ── Итоги по годам — вся история в приложении ──────────────────
// Данные ведутся с июля 2023. 2026 — текущий, неполный (по март).
// worked/night/transit — часы; distance — тыс. км; tkm — млн ткм;
// earnings — тыс. ₽; routes — смен.
const YEAR_HISTORY = [
  { year: 2023, note: 'с июля',  worked: 980,  distance: 19.4, tkm: 6.9,  night: 268, routes: 112, earnings: 742,  transit: 760,  partial: true },
  { year: 2024, note: null,      worked: 2050, distance: 40.1, tkm: 14.2, night: 561, routes: 241, earnings: 1610, transit: 1588 },
  { year: 2025, note: null,      worked: 2024, distance: 39.8, tkm: 13.9, night: 548, routes: 236, earnings: 1540, transit: 1562 },
  { year: 2026, note: 'по март', worked: 549,  distance: 10.6, tkm: 3.7,  night: 142, routes: 64,  earnings: 432,  transit: 421,  partial: true },
];

// Какие метрики можно открыть в разделе «Год за годом» (вкладка История).
// field — ключ в YEAR_HISTORY; fmt — формат значения; unit — подпись единицы;
// totUnit — единица для общего итога; sumFmt — формат суммы за всё время.
const HISTORY_METRICS = [
  { key: 'worked',   label: 'Отработано', icon: 'clock', field: 'worked',
    fmt: v => v.toLocaleString('ru-RU'), unit: 'ч', sumFmt: v => v.toLocaleString('ru-RU'), totUnit: 'ч' },
  { key: 'earnings', label: 'Заработок', icon: 'ruble', field: 'earnings',
    fmt: v => v.toLocaleString('ru-RU'), unit: 'тыс. ₽', sumFmt: v => (v/1000).toFixed(2).replace('.', ','), totUnit: 'млн ₽' },
  { key: 'distance', label: 'Путь', icon: 'route', field: 'distance',
    fmt: v => v.toFixed(1).replace('.', ','), unit: 'тыс. км', sumFmt: v => v.toFixed(1).replace('.', ','), totUnit: 'тыс. км' },
  { key: 'tkm',      label: 'Грузооборот', icon: 'weight', field: 'tkm',
    fmt: v => v.toFixed(1).replace('.', ','), unit: 'млн ткм', sumFmt: v => v.toFixed(1).replace('.', ','), totUnit: 'млн ткм' },
  { key: 'night',    label: 'Ночные', icon: 'moon', field: 'night',
    fmt: v => v.toLocaleString('ru-RU'), unit: 'ч', sumFmt: v => v.toLocaleString('ru-RU'), totUnit: 'ч' },
  { key: 'routes',   label: 'Смены', icon: 'doc', field: 'routes',
    fmt: v => v.toLocaleString('ru-RU'), unit: '', sumFmt: v => v.toLocaleString('ru-RU'), totUnit: 'смен' },
];

Object.assign(window, {
  fmtRu, fmtCompact, fmtHM,
  STAT_METRICS, STAT_ORDER, TOP_DIRECTIONS, STAT_BARS, YEAR_MONTHS, SPARKS, YEAR_HISTORY, YEAR_TOTALS, HISTORY_METRICS,
  COMPARE_MONTH, COMPARE_YEAR,
  MONTH_NAMES_SH, MONTH_NAMES_FULL, PICK_CUR, PICK_MONTH_YEARS, PICK_YEARS,
});
