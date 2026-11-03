// Машинист Design System — tokens v3
// Что нового по сравнению с v2:
//   • text styles — готовые spread-ready объекты (берут t как аргумент функции)
//     было: M.text.label.color(t)         (приходилось распаковывать руками)
//     стало: ...M.t.label(t)              (один spread — все нужные стили)
//   • spacing — устоявшиеся значения вынесены в M.s.* (короче чем M.space.*)
//   • semantic — высокоуровневые «строительные» токены (rowPadX, cardRadius и т.д.)
//   • радиусы пересмотрены, чтобы совпадать с тем что реально используется в макетах


const M = {
  // ─────────────────────────────────────────────────────────────
  // 1. ЦВЕТА — light + dark (без изменений по сравнению с v2)
  // ─────────────────────────────────────────────────────────────
  light: {
    bg:           '#F2F3F5',
    bgElevated:   '#FFFFFF',
    bgSubtle:     '#E7E9EC',
    surface:      '#FFFFFF',
    surfaceAlt:   '#F7F8FA',
    border:       'rgba(10,14,20,0.06)',
    borderStrong: 'rgba(10,14,20,0.14)',
    text:         '#0A0E14',
    textMuted:    'rgba(10,14,20,0.55)',
    textFaint:    'rgba(10,14,20,0.38)',
    accent:       '#00A0F5',
    accentInk:    '#FFFFFF',
    accentSoft:   'rgba(0,160,245,0.10)',
    accentHover:  '#0091D9',
    cta:          '#0A0E14',
    ctaInk:       '#FFFFFF',
    ctaHover:     '#242A33',
    chipBg:       '#EEF0F3',
    chipBgActive: '#D6EEFC',
    success:      '#00B341',
    warning:      '#FF8A00',
    danger:       '#FF4053',
    rail:         '#0A0E14',
    ink:          '#0A0E14',
  },
  dark: {
    bg:           '#0F1011',
    bgElevated:   '#2A2B2D',
    bgSubtle:     '#1A1B1D',
    surface:      '#2A2B2D',
    surfaceAlt:   '#34363A',
    border:       'rgba(255,255,255,0.08)',
    borderStrong: 'rgba(255,255,255,0.16)',
    text:         '#F5F5F5',
    textMuted:    'rgba(245,245,245,0.60)',
    textFaint:    'rgba(245,245,245,0.38)',
    accent:       '#33BFFF',
    accentInk:    '#0A0E14',
    accentSoft:   'rgba(51,191,255,0.16)',
    accentHover:  '#5ECFFF',
    cta:          '#F5F5F5',
    ctaInk:       '#0A0E14',
    ctaHover:     '#E5E5E5',
    chipBg:       '#2A2A2A',
    chipBgActive: '#0D2F44',
    success:      '#4ADE80',
    warning:      '#FFB547',
    danger:       '#FF6575',
    rail:         '#F5F5F5',
    ink:          '#060708',
  },

  // ─────────────────────────────────────────────────────────────
  // 2. ШРИФТЫ
  // ─────────────────────────────────────────────────────────────
  fontSans: '"Inter", -apple-system, BlinkMacSystemFont, "SF Pro Text", "Segoe UI", system-ui, sans-serif',
  fontMono: '"JetBrains Mono", ui-monospace, "SF Mono", Menlo, Consolas, monospace',
  fontDisplay: '"Inter", -apple-system, system-ui, sans-serif',

  // ─────────────────────────────────────────────────────────────
  // 3. РАДИУСЫ — пересмотрены, чтобы совпадать с реальным использованием
  // ─────────────────────────────────────────────────────────────
  // Что где использовать:
  //   xs   — пилюли-метки внутри строк (4–6 px)
  //   sm   — мелкие чипы, бэйджи (8–10 px)
  //   md   — поля ввода, кнопки в строках (10–12 px)
  //   lg   — карточки (iOS — 18, Android — 16; см. cardRadius)
  //   xl   — крупные hero-блоки, шторки iOS (24–28)
  //   sheet — шторки (Android 28, iOS 18) — берётся из platform
  //   pill — полностью круглые элементы (999)
  r: {
    xs: 6, sm: 10, md: 12, input: 12, btn: 12,
    cardIOS: 18, cardAndroid: 16,
    lg: 18, xl: 24,
    sheetIOS: 18, sheetAndroid: 28,
    pill: 999,
  },

  // ─────────────────────────────────────────────────────────────
  // 4. ТЕНИ
  // ─────────────────────────────────────────────────────────────
  shadow: {
    sm: '0 1px 2px rgba(20,18,14,0.04), 0 1px 3px rgba(20,18,14,0.06)',
    md: '0 2px 6px rgba(20,18,14,0.05), 0 8px 24px rgba(20,18,14,0.06)',
    lg: '0 12px 40px rgba(20,18,14,0.12)',
    floating: '0 12px 32px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.06)',
    fab:      '0 4px 12px rgba(0,0,0,0.18), 0 2px 4px rgba(0,0,0,0.08)',
    sheet:    '0 -8px 24px rgba(0,0,0,0.18)',
  },

  // ─────────────────────────────────────────────────────────────
  // 5. СПЕЙСИНГ — короткий алиас s.* (вместе со старым space.*)
  // ─────────────────────────────────────────────────────────────
  s: { xs: 4, sm: 8, md: 12, lg: 16, xl: 20, xxl: 24, xxxl: 32 },
  space: { xs: 4, sm: 8, md: 12, lg: 16, xl: 20, xxl: 24, xxxl: 32, card: 20, section: 32 },

  // ─────────────────────────────────────────────────────────────
  // 6. СЕМАНТИЧЕСКИЕ ТОКЕНЫ — готовые «строительные» значения
  // ─────────────────────────────────────────────────────────────
  // Эти числа повторяются в десятках мест по экранам. Если когда-нибудь
  // решим что строки в карточках стали тесно — меняем здесь, а не в 30
  // местах сразу.
  semantic: {
    // Внутренние отступы строк в карточках
    rowPadX:        20,           // горизонтальный padding строки в карточке
    rowPadY:        14,           // вертикальный для compact-строк (FieldRow, TripRow, TimeRow)
    rowPadYTouch:   16,           // для строк-таргетов (SheetTimeRow) — выше тач-зона
    rowGap:         12,           // gap между иконкой и контентом в строке
    // Карточки
    cardPadX:       20,
    cardPadY:       18,
    cardGap:        12,           // gap между карточками одной группы
    // Группы (зазор перед заголовком группы)
    groupHeadMt:    22,           // отступ перед GROUP-заголовком (между разными группами)
    groupHeadFirst: 4,            // отступ для первого заголовка после хедера
    groupHeadPadY:  '10px 4px 8px',
    // Инпуты
    inputMinH:      44,           // минимальная высота инпута (тач-зона iOS HIG)
    inputPadX:      14,
    inputPadY:      10,
    // Кнопки в навбарах
    navBtnSize:     40,           // iOS pill-button в навбаре
    navBtnPadX:     14,           // расширение для текстовой кнопки «Сохранить»
    // Иконка-аватар (круг с иконкой в начале строки)
    avatarSm:       28,           // ContextField шторки времени
    avatarMd:       32,           // SheetTimeRow, DurationRow, StationRow
    avatarLg:       36,           // AddRow, PassRow
    avatarRadius:   {             // под каждый размер свой радиус
      sm: 8, md: 16, lg: 10,
    },
  },
};

// ─────────────────────────────────────────────────────────────
// 7. TEXT STYLES
// ─────────────────────────────────────────────────────────────
// Каждый стиль — функция `(t) => style-object`.
// Использование: <div style={{ ...M.t.label(t), marginBottom: 6 }}>...</div>
//
// Иерархия от мелкого к крупному:
//   group         — UPPERCASE-MONO заголовок группы карточек ("ОСНОВНЫЕ ДАННЫЕ")
//   tag           — UPPERCASE-MONO badge ("ИЗ МАРШРУТА", "СВЕРХ", ВРЕМЯ РАБОТЫ)
//   label         — мелкая подпись над значением ("Серия", "Плечо", "Начало")
//   labelMuted    — то же, но выделено как hint
//   captionMuted  — серый текст в подзаголовке/sub-line строки ("Новосибирск — Омск")
//   body          — обычный текст строки ("Добавить локомотив")
//   value         — основное значение (sans, 17/600) — "Домашний", "Электровоз"
//   valueMono     — числовое значение (mono, 17/600) — "ВЛ80с", "1234", "20:00"
//   stationName   — название станции (17/500) — менее жирное чем value
//   navTitle      — заголовок iOS-навбара ("Локомотив", "Маршрут")
//   navAction     — текстовая кнопка в навбаре ("Сохранить", "Готово", "Отмена")
//   sectionH2     — крупный заголовок («Текущий маршрут», «Маршруты») — sans 20/700
//   metricLg      — большое число метрики (mono, 32/700) — итоги
//   metricXL      — hero-цифра (mono, 44–48/800) — "205:05"
//   timeMono      — формат времени в строках (mono, 18/600) — "08:20"
M.t = {
  group: (t) => ({
    fontFamily: M.fontMono, fontSize: 11, fontWeight: 400,
    color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase',
  }),
  tag: (t) => ({
    fontFamily: M.fontMono, fontSize: 11, fontWeight: 600,
    color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase',
  }),
  label: (t) => ({
    fontFamily: M.fontSans, fontSize: 13, fontWeight: 400,
    color: t.textMuted,
  }),
  labelMuted: (t) => ({
    fontFamily: M.fontSans, fontSize: 14, fontWeight: 400,
    color: t.textMuted,
  }),
  captionMuted: (t) => ({
    fontFamily: M.fontSans, fontSize: 12, fontWeight: 400,
    color: t.textMuted,
  }),
  hint: (t) => ({
    fontFamily: M.fontSans, fontSize: 13, fontWeight: 400,
    color: t.textMuted, lineHeight: 1.45,
  }),
  body: (t) => ({
    fontFamily: M.fontSans, fontSize: 15, fontWeight: 500,
    color: t.text,
  }),
  value: (t) => ({
    fontFamily: M.fontSans, fontSize: 17, fontWeight: 600,
    color: t.text,
  }),
  valueMono: (t) => ({
    fontFamily: M.fontMono, fontSize: 17, fontWeight: 600,
    color: t.text,
  }),
  stationName: (t) => ({
    fontFamily: M.fontSans, fontSize: 17, fontWeight: 500,
    color: t.text, lineHeight: 1.2,
  }),
  navTitle: (t) => ({
    fontFamily: M.fontSans, fontSize: 17, fontWeight: 700,
    color: t.text,
  }),
  navAction: (t, accent) => ({
    fontFamily: M.fontSans, fontSize: 16, fontWeight: accent ? 600 : 400,
    color: t.accent,
  }),
  sectionH2: (t) => ({
    fontFamily: M.fontSans, fontSize: 20, fontWeight: 700,
    color: t.text, letterSpacing: -0.3,
  }),
  metricLg: (t) => ({
    fontFamily: M.fontMono, fontSize: 32, fontWeight: 700,
    color: t.text, letterSpacing: -0.8, lineHeight: 1,
  }),
  metricXL: (t) => ({
    fontFamily: M.fontMono, fontSize: 48, fontWeight: 800,
    color: t.text, letterSpacing: -1.5, lineHeight: 1,
  }),
  timeMono: (t) => ({
    fontFamily: M.fontMono, fontSize: 18, fontWeight: 600,
    color: t.text,
  }),
};


// ─────────────────────────────────────────────────────────────
// 8. PALETTE PRESETS — без изменений
// ─────────────────────────────────────────────────────────────
const M_PALETTES = {
  red: {
    name: 'Кремовая бумага',
    note: 'Тёплый крем, графит и сигнальный красный — как Cardy Pay',
    light: {
      bg: '#E8E4D8', bgSubtle: '#DED9CA', surface: '#F3F0E6', surfaceAlt: '#EEEADC',
      text: '#1C1A16', textMuted: 'rgba(28,26,22,0.60)', textFaint: 'rgba(28,26,22,0.38)',
      border: 'rgba(28,26,22,0.08)', borderStrong: 'rgba(28,26,22,0.18)',
      accent: '#E2483D', accentInk: '#FFFFFF', accentSoft: 'rgba(226,72,61,0.10)', accentHover: '#C13A30',
      success: '#1F8A3F', warning: '#E08100', danger: '#E2483D', rail: '#1C1A16',
    },
    dark: {
      bg: '#101010', bgSubtle: '#1A1916', surface: '#2A2823', surfaceAlt: '#36332C',
      text: '#E8E4D8', textMuted: 'rgba(232,228,216,0.60)', textFaint: 'rgba(232,228,216,0.38)',
      border: 'rgba(232,228,216,0.08)', borderStrong: 'rgba(232,228,216,0.18)',
      accent: '#FF5A4E', accentInk: '#121210', accentSoft: 'rgba(255,90,78,0.16)', accentHover: '#FF7568',
      success: '#4ADE80', warning: '#FFB547', danger: '#FF5A4E', rail: '#E8E4D8',
    },
  },
  // ... (остальные палитры можно скопировать из tokens-2.js — они без изменений)
};

function makeTheme(paletteKey) {
  const base = JSON.parse(JSON.stringify(M));
  const p = M_PALETTES[paletteKey];
  if (!p) return base;
  base.light = { ...base.light, ...p.light };
  base.dark  = { ...base.dark,  ...p.dark  };
  base._paletteName = p.name;
  base._paletteNote = p.note;
  return base;
}

window.M = M;
window.M_PALETTES = M_PALETTES;
window.makeTheme = makeTheme;
