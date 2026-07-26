// Мастер «Заполнить месяц» — паттерн смен → предпросмотр
// =====================================================================
// Самая новая часть спеца «График работы»: вместо ручного проставления
// смен по дням машинист задаёт ПАТТЕРН (2/2, 5/2, свой цикл) и точку
// старта — приложение раскладывает смены на весь месяц.
//
// Экраны (артборды):
//   • Шаг 1 — выбор паттерна + время смены
//   • Шаг 1 «Свой» — редактор цикла смен + пикер типа дня + времена
//   • Шаг 2 — первый день цикла + предпросмотр месяца
//
// Используем токены приложения: дневная смена — синий accent, ночная —
// глубокий сине-стальной (та же гамма, темнее), явка — зелёная,
// больничный — красный, выходной — серый. Без эмодзи: чистые SVG-глифы.

// ── Семантика типов смен ─────────────────────────────────────
function shiftColors(t, dark = false) {
  // День — белая плашка (surface), ночь — чёрная. В тёмной теме
  // ночь осветляем до светло-серого, иначе глиф не читается на тёмном фоне.
  const night = dark ? '#D4D7DD' : '#1A1F2B';
  const dayInk = dark ? '#E8EAED' : '#1A1F2B';
  // stroke — хайрлайн для заливок (белый день нужно обвести, иначе теряется
  // на белой карточке). mark — цвет для подписей/точек/глифов НА светлом фоне
  // (для дня белый не читается → берём тёмный).
  return {
    day:   { solid: t.surface, soft: dark ? 'rgba(255,255,255,0.10)' : 'rgba(10,14,20,0.05)', ink: dayInk, stroke: t.borderStrong, mark: dayInk, label: 'Дневная', short: 'Дн' },
    night: { solid: night,     soft: dark ? 'rgba(212,215,221,0.16)' : 'rgba(26,31,43,0.12)', ink: dark ? '#0A0E14' : '#fff', stroke: 'transparent', mark: night, label: 'Ночная',  short: 'Ноч' },
    duty:  { solid: '#1F9D55', soft: 'rgba(31,157,85,0.14)',  ink: '#fff', stroke: 'transparent', mark: '#1F9D55', label: 'Явка',    short: 'Яв'  },
    off:   { solid: t.textFaint, soft: t.bgSubtle,            ink: t.textMuted, stroke: 'transparent', mark: t.textFaint, label: 'Выходной', short: 'Вых' },
    sick:  { solid: '#E2483D', soft: 'rgba(226,72,61,0.12)',  ink: '#fff', stroke: 'transparent', mark: '#E2483D', label: 'Больничный', short: 'Б' },
  };
}

// ── Глифы типов смен ─────────────────────────────────────────
function ShiftGlyph({ type, size = 18, color = 'currentColor' }) {
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke: color, strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round' };
  if (type === 'day') return (
    <svg {...common}>
      <circle cx="12" cy="12" r="4.5"/>
      <path d="M12 2v2.5M12 19.5V22M3.5 12H6M18 12h2.5M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M5.6 18.4l1.8-1.8M16.6 7.4l1.8-1.8"/>
    </svg>
  );
  if (type === 'night') return (
    <svg {...common}>
      <path d="M20 13.5A8 8 0 1 1 10.5 4a6.5 6.5 0 0 0 9.5 9.5z"/>
    </svg>
  );
  if (type === 'duty') return (
    <svg {...common}>
      <rect x="5" y="3" width="14" height="18" rx="2.5"/>
      <path d="M9 8h6M9 12h6M9 16h3"/>
    </svg>
  );
  // off — короткое тире
  return (
    <svg {...common}><path d="M6 12h12"/></svg>
  );
}

// ── Пресеты паттернов ────────────────────────────────────────
const PATTERNS = [
  { id: '2-2', big: '2 / 2', sub: '2 работа, 2 отдых' },
  { id: '5-2', big: '5 / 2', sub: 'Пн–пт, выходные' },
  { id: '3-3', big: '3 / 3', sub: '3 через 3' },
  { id: 'custom', big: 'Свой', sub: 'Настроить вручную' },
];

// Цикл по умолчанию для «Свой»: день, ночь, выходной, выходной
const DEFAULT_CYCLE = ['day', 'night', 'off', 'off'];

// ═════════════════════════════════════════════════════════════
// ГЛАВНЫЙ КОМПОНЕНТ
// ═════════════════════════════════════════════════════════════
function ScheduleWizardScreen({
  dark = false,
  step = 1,                 // 1 | 2
  pattern = '2-2',          // id паттерна
  cycle = DEFAULT_CYCLE,    // массив типов для custom
  pickerIndex = null,       // индекс квадрата цикла, для которого открыт пикер типа
  firstDay = 1,             // выбранный первый день цикла (для шага 2)
  editMode = false,         // режим редактирования карточек смен (для custom)
  confirm = null,           // 'day' | 'pattern' — показать диалог подтверждения удаления
  confirmDay = 1,           // номер дня для диалога удаления дня
  platform = 'ios',         // 'ios' | 'android' — хром устройства
  height = 852,             // высота устройства — растим, если не помещается
}) {
  const t = dark ? M.dark : M.light;
  const SC = shiftColors(t, dark);
  const isCustom = pattern === 'custom';

  return (
    <MDevice dark={dark} height={height} platform={platform}>
      <div style={{ paddingTop: 58, height: '100%', display: 'flex', flexDirection: 'column' }}>
        {/* Навбар */}
        <div style={{
          padding: '8px 12px 6px',
          display: 'grid', gridTemplateColumns: 'auto 1fr auto', alignItems: 'center', gap: 4,
        }}>
          <button style={{ ...pillBtn(t, { px: 12, h: 36 }), gap: 4 }}>
            <IcChevronLeft width="15" height="15"/>
            <span style={{ fontSize: 14, fontWeight: 600 }}>Апрель</span>
          </button>
          <div style={{ textAlign: 'center', fontSize: 16, fontWeight: 700, color: t.text }}>
            Заполнить месяц
          </div>
          <div style={{ width: 36 }}/>
        </div>

        {/* Индикатор шагов */}
        <StepRail t={t} step={step}/>

        {/* Контент шага */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '4px 16px 20px' }}>
          {step === 1
            ? <WizardStep1 t={t} SC={SC} pattern={pattern} cycle={cycle} pickerIndex={pickerIndex} isCustom={isCustom} editMode={editMode}/>
            : <WizardStep2 t={t} SC={SC} cycle={isCustom ? cycle : patternToCycle(pattern)} firstDay={firstDay}/>
          }
        </div>

        {/* Нижняя кнопка */}
        <div style={{
          padding: '10px 16px 26px',
          borderTop: `1px solid ${t.border}`, background: t.bg,
          display: 'flex', gap: 10,
        }}>
          {step === 2 && (
            <button style={{
              height: 50, padding: '0 18px', borderRadius: 14,
              background: t.surface, color: t.text, border: `1px solid ${t.borderStrong}`,
              fontSize: 15, fontWeight: 600, cursor: 'pointer',
              display: 'flex', alignItems: 'center', gap: 6,
            }}>
              <IcChevronLeft width="15" height="15"/> Назад
            </button>
          )}
          <button style={{
            flex: 1, height: 50, borderRadius: 14,
            background: t.accent, color: '#fff', border: 'none',
            fontSize: 16, fontWeight: 700, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
            boxShadow: '0 4px 12px rgba(0,160,245,0.25)',
          }}>
            {step === 1 ? 'Далее' : 'Применить'}
            {step === 1 && (
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
            )}
          </button>
        </div>
      </div>

      {/* Диалог подтверждения удаления */}
      {confirm && (
        <ConfirmDeleteDialog t={t} kind={confirm} dayNum={confirmDay}/>
      )}
    </MDevice>
  );
}

// ── Диалог подтверждения удаления (день / весь паттерн) ───────
function ConfirmDeleteDialog({ t, kind, dayNum = 1 }) {
  const isPattern = kind === 'pattern';
  const title = isPattern ? 'Удалить весь паттерн?' : `Удалить день ${dayNum}?`;
  const body = isPattern
    ? 'Цикл смен будет полностью очищен. Это действие нельзя отменить — паттерн придётся собрать заново.'
    : `День ${dayNum} будет убран из цикла, а следующие дни сдвинутся. Раскладку смен на месяц нужно будет применить заново.`;
  const confirmLabel = isPattern ? 'Удалить паттерн' : 'Удалить день';
  const danger = t.danger || '#E2483D';
  return (
    <div style={{
      position: 'absolute', inset: 0, zIndex: 80,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: 24, background: 'rgba(8,12,20,0.5)', backdropFilter: 'blur(2px)',
    }}>
      <div style={{
        width: '100%', maxWidth: 320, borderRadius: 22, background: t.surface,
        border: `1px solid ${t.border}`, boxShadow: '0 24px 60px rgba(0,0,0,0.4)',
        padding: 22, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center',
      }}>
        <div style={{
          width: 56, height: 56, borderRadius: 28, marginBottom: 14,
          background: hexToRgba(danger, 0.12),
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke={danger} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M10 11v6M14 11v6"/>
          </svg>
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, color: t.text, marginBottom: 8 }}>{title}</div>
        <div style={{ fontSize: 13.5, lineHeight: 1.45, color: t.textMuted, marginBottom: 20 }}>{body}</div>
        <button style={{
          width: '100%', height: 50, borderRadius: 14, border: 'none', cursor: 'pointer',
          background: danger, color: '#fff', fontSize: 15.5, fontWeight: 700,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          boxShadow: `0 6px 16px ${hexToRgba(danger, 0.3)}`,
        }}>
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M10 11v6M14 11v6"/>
          </svg>
          {confirmLabel}
        </button>
        <button style={{
          width: '100%', height: 48, borderRadius: 14, marginTop: 10, cursor: 'pointer',
          background: 'transparent', color: t.text, border: `1px solid ${t.border}`,
          fontSize: 15, fontWeight: 600,
        }}>
          Отмена
        </button>
      </div>
    </div>
  );
}

// Индикатор шагов ①—②
function StepRail({ t, step }) {
  const Dot = ({ n, done, active }) => (
    <div style={{
      width: 26, height: 26, borderRadius: 13,
      background: done ? t.accent : active ? t.accent : t.bgSubtle,
      color: done || active ? '#fff' : t.textMuted,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: M.fontMono, fontWeight: 700, fontSize: 13,
      border: active && !done ? `none` : 'none',
    }}>
      {done ? (
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12l5 5L20 7"/></svg>
      ) : n}
    </div>
  );
  const labels = ['Паттерн', 'Старт и предпросмотр'];
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
      padding: '4px 16px 10px',
    }}>
      <Dot n={1} done={step > 1} active={step === 1}/>
      <span style={{ fontSize: 12, fontWeight: 600, color: step === 1 ? t.text : t.textMuted }}>{labels[0]}</span>
      <div style={{ width: 28, height: 2, borderRadius: 1, background: step > 1 ? t.accent : t.bgSubtle }}/>
      <Dot n={2} done={false} active={step === 2}/>
      <span style={{ fontSize: 12, fontWeight: 600, color: step === 2 ? t.text : t.textMuted }}>{labels[1]}</span>
    </div>
  );
}

// Преобразование пресета в массив-цикл (для предпросмотра)
function patternToCycle(id) {
  if (id === '2-2') return ['day', 'day', 'off', 'off'];
  if (id === '5-2') return ['day', 'day', 'day', 'day', 'day', 'off', 'off'];
  if (id === '3-3') return ['day', 'day', 'day', 'off', 'off', 'off'];
  return DEFAULT_CYCLE;
}
