// Добавление маршрута из календаря — flow «время + даты»
// =====================================================================
// Логика: пользователь выбирает (1) время явки из частых либо новое,
// и (2) одну или несколько дат — маршруты создаются сразу пачкой.
// Никаких «откуда/куда/поезд» — это заполняется позже на экране маршрута.
//
// В отличие от Android (где список времён под календарём, а кнопки
// «Сохранить/Сбросить» в самом низу), здесь:
//   • Sticky-плашка выбранного времени видна сверху всегда (с × чтобы сменить)
//   • Чипы времени упорядочены по частоте использования и показывают её
//   • Выбранные даты подсвечены кружком + временем внутри ячейки
//   • CTA снизу меняет текст по шагу: подсказывает, что делать дальше
//   • «+ Своё время» открывает шторку с моно-цифровой клавиатурой


// Времена и сколько раз пользователь их уже использовал
const FREQUENT_TIMES = [
  { time: '08:00', count: 12 },
  { time: '20:00', count: 9 },
  { time: '23:00', count: 7 },
  { time: '05:01', count: 5 },
  { time: '08:46', count: 4 },
  { time: '11:35', count: 3 },
  { time: '15:25', count: 2 },
  { time: '00:27', count: 2 },
];

// Главный компонент. Все props — для контроля начального состояния
// на разных артбордах. Внутри — нормальное интерактивное состояние.
function RouteAddScreen({
  dark = false,
  initialTime = null,
  initialDates = [],
  initialCustomOpen = false,
  platform = 'ios',
}) {
  const t = dark ? M.dark : M.light;
  const C = calColors();

  const [time, setTime] = React.useState(initialTime);
  const [dates, setDates] = React.useState(new Set(initialDates));
  const [customOpen, setCustomOpen] = React.useState(initialCustomOpen);

  const toggleDate = (d) => {
    if (!d || !time) return;          // даты тапаются только после выбора времени
    if (CAL_ABSENCE[d]) return;       // нельзя в отвлечение
    const next = new Set(dates);
    next.has(d) ? next.delete(d) : next.add(d);
    setDates(next);
  };

  const datesArr = [...dates].sort((a, b) => a - b);
  const step = !time ? 'time' : dates.size === 0 ? 'dates' : 'done';

  return (
    <MDevice dark={dark} platform={platform}>
      <div style={{
        paddingTop: 58, height: '100%', display: 'flex', flexDirection: 'column',
      }}>
        {/* ── Шапка ─────────────────────────────────────── */}
        <RouteAddHeader t={t}/>

        {/* ── Единая плашка шагов: морфит между «Шаг 1» и «Шаг 2» ── */}
        <StepChip t={t} step={step}/>

        {/* ── Месяц + стрелки навигации (отдельной строкой) ── */}
        <div style={{
          padding: '10px 20px 2px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{CAL_MONTH.label}</div>
          <div style={{ display: 'flex', gap: 6 }}>
            <button style={pillBtn(t, { w: 30, h: 30 })}><IcChevronLeft width="13" height="13"/></button>
            <button style={pillBtn(t, { w: 30, h: 30 })}><IcChevronRight width="13" height="13"/></button>
          </div>
        </div>

        {/* Шапка дней недели */}
        <div style={{
          padding: '8px 8px 2px',
          display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)',
        }}>
          {WEEK_RU.map((d, i) => (
            <div key={d} style={{
              textAlign: 'center', fontSize: 10, fontWeight: 700,
              color: i >= 5 ? t.accent : t.textFaint, letterSpacing: 0.6,
            }}>{d.toUpperCase()}</div>
          ))}
        </div>

        {/* Сетка месяца с возможностью мульти-выбора */}
        <RouteAddMonthGrid t={t} C={C}
          time={time} dates={dates}
          interactive={!!time}
          onToggle={toggleDate}/>

        {/* ── Чипы времён ──────────────────────────────── */}
        <div style={{
          padding: '14px 16px 4px',
          marginTop: 4, borderTop: `1px solid ${t.border}`,
        }}>
          <div style={{
            ...M.t.group(t),
            display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
            marginBottom: 10,
          }}>
            <span>Время явки</span>
            <span style={{ fontSize: 10, color: t.textFaint, textTransform: 'none', letterSpacing: 0 }}>
              {time ? 'часто используемые' : 'выберите одно'}
            </span>
          </div>
          <TimeChipsRow t={t}
            current={time}
            onPick={setTime}
            onCustom={() => setCustomOpen(true)}/>
        </div>

        {/* Растяжимый зазор */}
        <div style={{ flex: 1 }}/>

        {/* ── CTA снизу с динамическим текстом ────────────── */}
        <RouteAddCTA t={t} step={step} count={dates.size} time={time} dates={datesArr}/>
      </div>

      {/* ── Шторка ввода своего времени ──────────────────── */}
      {customOpen && (
        <CustomTimeSheet
          t={t}
          onClose={() => setCustomOpen(false)}
          onApply={(v) => { setTime(v); setCustomOpen(false); }}/>
      )}
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════
// Header — компактная навбар без фона
// ════════════════════════════════════════════════════════════
function RouteAddHeader({ t }) {
  return (
    <div style={{
      padding: '8px 16px 6px',
      display: 'grid', gridTemplateColumns: '1fr auto 1fr', alignItems: 'center',
    }}>
      <button style={{ ...pillBtn(t, { w: 36, h: 36 }), justifySelf: 'start' }}>
        <IcChevronLeft width="16" height="16"/>
      </button>
      <div style={{ textAlign: 'center' }}>
        <div style={{ ...M.t.navTitle(t) }}>Новый маршрут</div>
        <div style={{ fontSize: 11, color: t.textMuted, marginTop: 1 }}>
          Время явки и даты
        </div>
      </div>
      <div/>
    </div>
  );
}

// ════════════════════════════════════════════════════════════
// StepChip — единая плашка, морфит между двумя шагами
//
//   step==='time'  → "ШАГ 1 · Время явки", акцентный пунктир (active)
//   step==='dates' → "ШАГ 2 · Даты",       акцентный пунктир (active)
//   step==='done'  → "ШАГ 2 · Даты",       выполненный (soft, галочка)
//
// Сам выбранный 20:00 и список дат тут НЕ показываем —
// выбранное время видно по подсветке чипа в ряду ниже,
// а сводка по датам — над кнопкой «Создать».
// ════════════════════════════════════════════════════════════
function StepChip({ t, step }) {
  const done = step === 'done';
  const num = step === 'time' ? 1 : 2;
  const title = step === 'time' ? 'Время явки' : 'Даты';
  const subtitle = step === 'time'
    ? 'Выберите время явки из списка ниже'
    : step === 'dates'
      ? 'Выберите одну или несколько дат'
      : 'Шаг выполнен — можно создавать маршруты';

  return (
    <div style={{ padding: '6px 16px 2px' }}>
      <div style={{
        minHeight: 56, borderRadius: 14,
        background: done ? t.accentSoft : 'transparent',
        border: done
          ? `1.5px solid ${t.accent}`
          : `1.5px dashed ${t.accent}`,
        display: 'flex', alignItems: 'center', padding: '10px 14px', gap: 12,
        transition: 'all 220ms ease',
      }}>
        {/* Левая иконка-индекс шага: круглый бейдж с номером или галочкой */}
        <div style={{
          flex: 'none',
          width: 30, height: 30, borderRadius: 15,
          background: t.accent, color: '#fff',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
          fontWeight: 800, fontSize: 14,
        }}>
          {done ? (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M5 12l5 5L20 7"/>
            </svg>
          ) : num}
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontSize: 10, fontWeight: 700, letterSpacing: 0.8,
            textTransform: 'uppercase',
            color: t.accent,
          }}>Шаг {num} · {title}</div>
          <div style={{
            fontSize: 14, fontWeight: 600,
            color: t.text, marginTop: 2, lineHeight: 1.2,
          }}>{subtitle}</div>
        </div>
      </div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════
// Чипы времён — две строки, кнопка «Своё время»
// ════════════════════════════════════════════════════════════
function TimeChipsRow({ t, current, onPick, onCustom }) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {FREQUENT_TIMES.map(({ time, count }) => {
        const active = current === time;
        return (
          <button key={time} onClick={() => onPick(time)} style={{
            border: 'none', cursor: 'pointer',
            padding: '8px 12px', borderRadius: 12,
            background: active ? t.accent : t.surface,
            color: active ? '#fff' : t.text,
            boxShadow: active ? 'none' : `inset 0 0 0 1px ${t.border}`,
            display: 'flex', alignItems: 'center', gap: 6,
            transition: 'all 120ms ease',
          }}>
            <span style={{
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              fontWeight: 700, fontSize: 15, letterSpacing: -0.2,
            }}>{time}</span>
            <span style={{
              fontSize: 11, fontWeight: 600,
              color: active ? 'rgba(255,255,255,0.7)' : t.textFaint,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
            }}>·&nbsp;{count}</span>
          </button>
        );
      })}
      <button onClick={onCustom} style={{
        border: 'none', cursor: 'pointer',
        padding: '8px 12px', borderRadius: 12,
        background: 'transparent', color: t.accent,
        boxShadow: `inset 0 0 0 1.5px ${t.accent}40`,
        display: 'flex', alignItems: 'center', gap: 4,
        fontFamily: M.fontSans, fontWeight: 600, fontSize: 14,
      }}>
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        Своё время
      </button>
    </div>
  );
}

// ════════════════════════════════════════════════════════════
// Сетка месяца в режиме выбора нескольких дат
// ════════════════════════════════════════════════════════════
function RouteAddMonthGrid({ t, C, time, dates, interactive, onToggle }) {
  const cells = buildMonthGrid(CAL_MONTH.year, CAL_MONTH.month);
  return (
    <div style={{
      padding: '4px 8px 6px',
      display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 3,
    }}>
      {cells.map((d, i) => (
        <RouteAddCell key={i} t={t} C={C} d={d} idx={i}
          time={time} isSelected={dates.has(d)}
          interactive={interactive}
          onClick={() => onToggle(d)}/>
      ))}
    </div>
  );
}

function RouteAddCell({ t, C, d, idx, time, isSelected, interactive, onClick }) {
  if (d == null) return <div style={{ height: 54 }}/>;
  const trips = CAL_TRIPS[d];
  const abs = CAL_ABSENCE[d];
  const isToday = d === CAL_TODAY;
  const isWeekend = (idx % 7) >= 5;
  const ac = abs ? C[abs.type] : null;
  const blocked = !!abs || !interactive;

  return (
    <div onClick={!blocked ? onClick : undefined} style={{
      height: 54, borderRadius: 10, padding: '4px 4px',
      background: isSelected ? t.accent
        : abs ? ac.soft
        : (trips ? C.trip.softer : t.surface),
      border: isSelected ? `1.5px solid ${t.accent}`
        : isToday ? `1.5px solid ${t.accent}40`
        : `1px solid ${abs ? ac.solid + '44' : (trips ? C.trip.solid + '22' : t.border)}`,
      opacity: !interactive && !trips && !abs ? 0.55 : 1,
      cursor: blocked ? 'default' : 'pointer',
      display: 'flex', flexDirection: 'column',
      position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{
          fontSize: 13, fontWeight: 700,
          color: isSelected ? '#fff'
            : isToday ? t.accent
            : isWeekend ? t.textMuted : t.text,
          fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
        }}>{d}</span>
      </div>

      {/* Что показываем в ячейке:
          - если выбран дату — большое время (то, что создаём)
          - если был трип — его время мелким серым
          - если ничего — пусто */}
      {isSelected ? (
        <div style={{
          marginTop: 'auto',
          fontSize: 12, fontWeight: 800,
          color: '#fff',
          fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
          lineHeight: 1, textAlign: 'right',
        }}>{time}</div>
      ) : trips ? (
        <div style={{
          marginTop: 1,
          fontSize: 10, fontWeight: 600,
          color: C.trip.solid,
          fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
          lineHeight: 1.1,
        }}>{trips[0]}{trips.length > 1 ? `·${trips.length}` : ''}</div>
      ) : null}

      {abs && (
        <div style={{
          marginTop: 'auto', textAlign: 'right',
          fontSize: 9, fontWeight: 700, color: ac.solid,
          textTransform: 'uppercase', letterSpacing: 0.3,
        }}>{absBadge(abs.type)}</div>
      )}
    </div>
  );
}

// ════════════════════════════════════════════════════════════
// CTA — sticky кнопка снизу, текст и состояние зависят от шага
// ════════════════════════════════════════════════════════════
function RouteAddCTA({ t, step, count, time, dates }) {
  const disabled = step !== 'done';
  let label, sub;
  if (step === 'time') {
    label = 'Создать маршрут';
    sub = '\u00A0'; // placeholder, чтобы высота не прыгала
  } else if (step === 'dates') {
    label = 'Создать маршрут';
    sub = `${time} · выберите даты`;
  } else {
    label = count === 1
      ? `Создать маршрут на ${dates[0]} мая`
      : `Создать ${count} ${declension(count)}`;
    sub = `${time} · ${formatDatesSummary(dates)}`;
  }

  return (
    <div style={{
      padding: '10px 16px 24px',
      borderTop: `1px solid ${t.border}`,
      background: t.bg,
    }}>
      <div style={{
        fontSize: 12, color: t.textMuted, marginBottom: 8, padding: '0 4px',
        fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
        minHeight: 16,
      }}>{sub}</div>
      <button disabled={disabled} style={{
        width: '100%', height: 50, borderRadius: 14,
        background: disabled ? t.bgSubtle : t.accent,
        color: disabled ? t.textMuted : '#fff',
        border: 'none',
        fontSize: 16, fontWeight: 700,
        cursor: disabled ? 'default' : 'pointer',
        boxShadow: disabled ? 'none' : '0 4px 12px rgba(0,160,245,0.25)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
      }}>
        {!disabled && (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
            <path d="M5 12l5 5L20 7"/>
          </svg>
        )}
        {label}
      </button>
    </div>
  );
}

function declension(n) {
  const last = n % 10, last2 = n % 100;
  if (last2 >= 11 && last2 <= 14) return 'маршрутов';
  if (last === 1) return 'маршрут';
  if (last >= 2 && last <= 4) return 'маршрута';
  return 'маршрутов';
}

function formatDatesSummary(dates) {
  if (dates.length <= 3) return dates.map(d => `${d} мая`).join(', ');
  return `${dates.slice(0, 2).join(', ')} и ещё ${dates.length - 2}`;
}

// ════════════════════════════════════════════════════════════
// Шторка ввода своего времени — моно-цифровая клавиатура
// ════════════════════════════════════════════════════════════
function CustomTimeSheet({ t, onClose, onApply }) {
  const [input, setInput] = React.useState('');
  const padded = input.padEnd(4, '_');
  const hh = padded.slice(0, 2), mm = padded.slice(2, 4);
  const valid = input.length === 4
    && Number(input.slice(0, 2)) < 24
    && Number(input.slice(2, 4)) < 60;
  const formatted = `${hh}:${mm}`;

  const push = (n) => input.length < 4 && setInput(input + n);
  const back = () => setInput(input.slice(0, -1));

  const Key = ({ children, onClick, span = 1 }) => (
    <button onClick={onClick} style={{
      gridColumn: `span ${span}`,
      height: 56, border: 'none', borderRadius: 14,
      background: t.surface, color: t.text,
      fontFamily: M.fontMono, fontSize: 22, fontWeight: 600,
      cursor: 'pointer',
      boxShadow: `inset 0 0 0 1px ${t.border}`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>{children}</button>
  );

  return (
    <ModalScrim onClose={onClose}>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        background: t.bg,
        borderTopLeftRadius: 24, borderTopRightRadius: 24,
        paddingBottom: 24, paddingTop: 8,
        boxShadow: '0 -16px 40px rgba(0,0,0,0.18)',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 12 }}>
          <div style={{ width: 36, height: 5, borderRadius: 3, background: t.borderStrong }}/>
        </div>

        <div style={{
          padding: '0 20px 16px', display: 'flex',
          alignItems: 'flex-end', justifyContent: 'space-between',
        }}>
          <div>
            <div style={{ ...M.t.group(t) }}>Своё время явки</div>
            <div style={{
              marginTop: 6,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              fontSize: 48, fontWeight: 800,
              color: input.length ? t.text : t.textFaint,
              letterSpacing: -1.5, lineHeight: 1,
            }}>
              <span style={{ color: input.length >= 1 ? t.text : t.textFaint }}>{padded[0]}</span>
              <span style={{ color: input.length >= 2 ? t.text : t.textFaint }}>{padded[1]}</span>
              <span style={{ color: t.textFaint, margin: '0 4px' }}>:</span>
              <span style={{ color: input.length >= 3 ? t.text : t.textFaint }}>{padded[2]}</span>
              <span style={{ color: input.length >= 4 ? t.text : t.textFaint }}>{padded[3]}</span>
            </div>
          </div>
          <button onClick={onClose} style={{
            ...pillBtn(t, { w: 36, h: 36 }),
          }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div style={{
          padding: '0 16px 14px',
          display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8,
        }}>
          {[1,2,3,4,5,6,7,8,9].map(n => (
            <Key key={n} onClick={() => push(String(n))}>{n}</Key>
          ))}
          <Key onClick={back}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 4H8L1 12l7 8h13a2 2 0 002-2V6a2 2 0 00-2-2z"/>
              <line x1="18" y1="9" x2="12" y2="15"/>
              <line x1="12" y1="9" x2="18" y2="15"/>
            </svg>
          </Key>
          <Key onClick={() => push('0')}>0</Key>
          <button disabled={!valid} onClick={() => onApply(formatted)} style={{
            height: 56, border: 'none', borderRadius: 14,
            background: valid ? t.accent : t.bgSubtle,
            color: valid ? '#fff' : t.textMuted,
            fontFamily: M.fontSans, fontSize: 15, fontWeight: 700,
            cursor: valid ? 'pointer' : 'default',
          }}>Готово</button>
        </div>
      </div>
    </ModalScrim>
  );
}
