// Мастер «Заполнить месяц» — Шаг 1
// Выбор паттерна графика + (для «Свой») редактор цикла смен.

function WizardStep1({ t, SC, pattern, cycle, pickerIndex, isCustom, editMode }) {
  return (
    <div>
      {/* ── Паттерн графика ── */}
      <SectionLabel t={t}>Паттерн графика</SectionLabel>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {PATTERNS.map(p => {
          const active = p.id === pattern;
          return (
            <div key={p.id} style={{
              padding: '16px 16px', borderRadius: 16,
              background: active ? t.accentSoft : t.surface,
              border: active ? `1.5px solid ${t.accent}` : `1px solid ${t.border}`,
              cursor: 'pointer', position: 'relative',
            }}>
              {active && (
                <div style={{
                  position: 'absolute', top: 12, right: 12,
                  width: 20, height: 20, borderRadius: 10, background: t.accent,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12l5 5L20 7"/></svg>
                </div>
              )}
              <div style={{
                fontFamily: M.fontMono, fontSize: 26, fontWeight: 800,
                color: active ? t.accent : t.text, letterSpacing: -0.5,
              }}>{p.big}</div>
              <div style={{ fontSize: 12.5, color: t.textMuted, marginTop: 4, lineHeight: 1.25 }}>{p.sub}</div>
            </div>
          );
        })}
      </div>

      {/* ── Стандартный паттерн → время смены ── */}
      {!isCustom && (
        <div>
          <SectionLabel t={t} mt={24}>Время смены</SectionLabel>
          <div style={{
            background: t.surface, borderRadius: 16, border: `1px solid ${t.border}`,
            display: 'flex', alignItems: 'center',
          }}>
            <TimeField t={t} label="Начало" value="08:00"/>
            <div style={{ width: 1, alignSelf: 'stretch', background: t.border, margin: '12px 0' }}/>
            <TimeField t={t} label="Конец" value="20:00"/>
          </div>
          <p style={{ ...M.t.hint(t), marginTop: 10, padding: '0 4px' }}>
            Смены продолжительностью 12 часов разложатся по всем рабочим дням паттерна.
          </p>
        </div>
      )}

      {/* ── «Свой» → редактор цикла ── */}
      {isCustom && (
        <CycleEditor t={t} SC={SC} cycle={cycle} pickerIndex={pickerIndex} editMode={editMode}/>
      )}
    </div>
  );
}

// ── Редактор цикла смен ──────────────────────────────────────
function CycleEditor({ t, SC, cycle, pickerIndex, editMode }) {
  const workCount = cycle.filter(c => c !== 'off').length;
  const offCount = cycle.filter(c => c === 'off').length;
  // Уникальные рабочие типы, для которых показываем строку времени
  const timeRows = [];
  if (cycle.includes('day'))   timeRows.push('day');
  if (cycle.includes('night')) timeRows.push('night');
  if (cycle.includes('duty'))  timeRows.push('duty');
  const TIMES = { day: ['08:00', '20:00'], night: ['20:00', '08:00'], duty: ['08:00', null] };

  return (
    <div>
      <SectionLabel t={t} mt={24}>Цикл смен</SectionLabel>

      {/* Ряд квадратов цикла */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-start' }}>
        {cycle.map((type, i) => {
          const c = SC[type];
          const isPicking = pickerIndex === i;
          const ink = type === 'off' ? t.textMuted : c.ink;
          return (
            <div key={i} style={{
              width: 54, height: 60, borderRadius: 13,
              background: type === 'off' ? t.bgSubtle : c.solid,
              position: 'relative', cursor: 'pointer',
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 3,
              border: isPicking ? `2px solid ${t.text}` : (type === 'off' ? 'none' : `1px solid ${c.stroke}`),
              color: ink,
            }}>
              <span style={{
                position: 'absolute', top: 4, left: 6,
                fontSize: 9, fontWeight: 700, fontFamily: M.fontMono,
                color: type === 'off' ? t.textFaint : ink, opacity: type === 'off' ? 1 : 0.65,
              }}>{i + 1}</span>
              <ShiftGlyph type={type} size={18} color={type === 'off' ? t.textFaint : ink}/>
              <span style={{ fontSize: 10, fontWeight: 700 }}>{c.short}</span>
            </div>
          );
        })}
        {/* Кнопка + */}
        <div style={{
          width: 54, height: 60, borderRadius: 13,
          border: `1.5px dashed ${t.borderStrong}`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: t.textMuted, cursor: 'pointer',
        }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><path d="M12 5v14M5 12h14"/></svg>
        </div>
      </div>

      {/* Сводка цикла */}
      <div style={{ fontSize: 13, color: t.textMuted, marginTop: 10, padding: '0 2px' }}>
        Цикл: <b style={{ color: t.text }}>{workCount} {plur(workCount, 'рабочий', 'рабочих', 'рабочих')}</b>, {offCount} {plur(offCount, 'выходной', 'выходных', 'выходных')}
      </div>

      {/* Пикер типа дня — раскрыт под рядом, если есть pickerIndex */}
      {pickerIndex != null && (
        <TypePicker t={t} SC={SC} dayNum={pickerIndex + 1} current={cycle[pickerIndex]}/>
      )}

      {/* Легенда */}
      <div style={{
        display: 'flex', flexWrap: 'wrap', gap: '6px 14px', marginTop: 14,
        padding: '12px 14px', borderRadius: 12, background: t.surfaceAlt,
      }}>
        {['day', 'night', 'off'].map(type => (
          <div key={type} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 12, height: 12, borderRadius: 4, background: type === 'off' ? t.borderStrong : SC[type].solid, border: type === 'off' ? 'none' : `1px solid ${SC[type].stroke}` }}/>
            <span style={{ fontSize: 12, color: t.textMuted }}>{SC[type].label}</span>
          </div>
        ))}
      </div>

      {/* Смены — режим просмотра / редактирования */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', margin: '24px 2px 10px' }}>
        <div style={{ ...M.t.group(t) }}>Смены</div>
        {editMode ? (
          <button style={{
            display: 'flex', alignItems: 'center', gap: 5, cursor: 'pointer',
            padding: '5px 12px', borderRadius: 9, border: 'none',
            background: t.accent, color: '#fff', fontSize: 13, fontWeight: 700,
          }}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12l5 5L20 7"/></svg>
            Готово
          </button>
        ) : (
          <button style={{
            display: 'flex', alignItems: 'center', gap: 5, cursor: 'pointer',
            padding: '5px 12px', borderRadius: 9, border: `1px solid ${t.border}`,
            background: t.surface, color: t.accent, fontSize: 13, fontWeight: 600,
          }}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>
            Изменить
          </button>
        )}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: editMode ? 10 : 8 }}>
        {timeRows.map(type => {
          const [start, end] = TIMES[type];
          return editMode
            ? <ShiftCard key={type} t={t} type={type} c={SC[type]} start={start} end={end}/>
            : <ShiftRow  key={type} t={t} type={type} c={SC[type]} start={start} end={end}/>;
        })}
        {editMode && (
          <button style={{
            height: 48, borderRadius: 14,
            border: `1.5px dashed ${t.borderStrong}`, background: 'transparent',
            color: t.textMuted, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
            fontSize: 14.5, fontWeight: 600,
          }}>
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><path d="M12 5v14M5 12h14"/></svg>
            Добавить смену
          </button>
        )}
      </div>
      <p style={{ ...M.t.hint(t), marginTop: 10, padding: '0 4px' }}>
        {editMode
          ? 'Название и время каждой смены можно изменить. Удаление смены освободит её дни в цикле.'
          : 'Нажмите «Изменить», чтобы переименовать смены, настроить время или удалить их.'}
      </p>

      {/* Удалить весь паттерн */}
      <div style={{ marginTop: 18, paddingTop: 18, borderTop: `1px solid ${t.border}` }}>
        <button style={{
          width: '100%', height: 50, borderRadius: 14,
          border: `1.5px solid ${hexToRgba(t.danger || '#E2483D', 0.4)}`,
          background: hexToRgba(t.danger || '#E2483D', 0.06),
          color: t.danger || '#E2483D', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          fontSize: 14.5, fontWeight: 700,
        }}>
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M10 11v6M14 11v6"/>
          </svg>
          Удалить весь паттерн
        </button>
        <p style={{ ...M.t.hint(t), marginTop: 8, padding: '0 4px', textAlign: 'center' }}>
          Цикл будет очищен — можно собрать новый с нуля.
        </p>
      </div>
    </div>
  );
}

// ── Пикер типа дня цикла ─────────────────────────────────────
function TypePicker({ t, SC, dayNum, current }) {
  const opts = ['day', 'night', 'off'];
  return (
    <div style={{
      marginTop: 12, padding: '14px 14px 14px', borderRadius: 14,
      background: t.surfaceAlt, border: `1px solid ${t.border}`,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.text }}>
          День {dayNum} — выберите тип
        </div>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={t.textMuted} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ cursor: 'pointer' }}>
          <path d="M18 6 6 18M6 6l12 12"/>
        </svg>
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {opts.map(type => {
          const c = SC[type];
          const active = type === current;
          return (
            <div key={type} style={{
              display: 'flex', alignItems: 'center', gap: 7,
              padding: '8px 12px', borderRadius: 11, cursor: 'pointer',
              background: active ? (type === 'off' ? t.bgSubtle : c.solid) : t.surface,
              border: active ? (type === 'off' ? 'none' : `1px solid ${c.stroke}`) : `1px solid ${t.border}`,
              color: active ? (type === 'off' ? t.text : c.ink) : t.text,
            }}>
              <ShiftGlyph type={type} size={15} color={active ? (type === 'off' ? t.textMuted : c.ink) : (type === 'off' ? t.textMuted : c.mark)}/>
              <span style={{ fontSize: 13.5, fontWeight: 600 }}>{c.label}</span>
            </div>
          );
        })}
      </div>
      {/* Удалить этот день из цикла */}
      <button style={{
        marginTop: 12, width: '100%', height: 42, borderRadius: 11,
        border: `1px solid ${hexToRgba(t.danger || '#E2483D', 0.35)}`,
        background: hexToRgba(t.danger || '#E2483D', 0.08),
        color: t.danger || '#E2483D', cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7,
        fontSize: 13.5, fontWeight: 600,
      }}>
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M10 11v6M14 11v6"/>
        </svg>
        Удалить день {dayNum} из цикла
      </button>
    </div>
  );
}

// ── Компактная строка смены (режим просмотра) ─────────────────
function ShiftRow({ t, type, c, start, end }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '11px 14px', borderRadius: 14,
      background: t.surface, border: `1px solid ${t.border}`,
    }}>
      <div style={{
        width: 34, height: 34, borderRadius: 10, background: c.solid, flex: 'none',
        border: `1px solid ${c.stroke}`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <ShiftGlyph type={type} size={17} color={c.ink}/>
      </div>
      <span style={{ fontSize: 14.5, fontWeight: 600, color: t.text }}>{c.label}</span>
      <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ ...M.t.valueMono(t), fontSize: 16 }}>{start}</span>
        {end ? (
          <>
            <span style={{ color: t.textFaint }}>—</span>
            <span style={{ ...M.t.valueMono(t), fontSize: 16 }}>{end}</span>
          </>
        ) : (
          <span style={{ fontSize: 13, color: t.textMuted }}>только явка</span>
        )}
      </div>
    </div>
  );
}

// ── Карточка смены: редактируемое название + время + удаление ──
function ShiftCard({ t, type, c, start, end }) {
  return (
    <div style={{
      background: t.surface, borderRadius: 16, border: `1px solid ${t.border}`,
      padding: 14, display: 'flex', flexDirection: 'column', gap: 12,
    }}>
      {/* Шапка: глиф + редактируемое название + удалить */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{
          width: 38, height: 38, borderRadius: 11, background: c.solid, flex: 'none',
          border: `1px solid ${c.stroke}`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <ShiftGlyph type={type} size={19} color={c.ink}/>
        </div>
        <div style={{
          flex: 1, display: 'flex', alignItems: 'center', gap: 6,
          borderBottom: `1.5px solid ${t.border}`, paddingBottom: 5,
        }}>
          <span style={{ fontSize: 16, fontWeight: 700, color: t.text }}>{c.label}</span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={t.textMuted} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginLeft: 'auto' }}>
            <path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>
          </svg>
        </div>
        <button style={{
          width: 38, height: 38, borderRadius: 11, flex: 'none',
          border: `1px solid ${t.border}`, background: t.surface, cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke={t.danger || '#E2483D'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M10 11v6M14 11v6"/>
          </svg>
        </button>
      </div>
      {/* Время: начало / конец */}
      <div style={{ display: 'flex', gap: 10 }}>
        <TimeBox t={t} label="Начало" value={start}/>
        <TimeBox t={t} label="Конец" value={end}/>
      </div>
    </div>
  );
}

function TimeBox({ t, label, value }) {
  return (
    <div style={{
      flex: 1, padding: '9px 14px', borderRadius: 12,
      background: t.bgSubtle, border: `1px solid ${t.border}`, cursor: 'pointer',
    }}>
      <div style={{ fontSize: 11.5, color: t.textMuted, marginBottom: 2 }}>{label}</div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ ...M.t.valueMono(t), fontSize: 19 }}>{value || '—'}</span>
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={t.textFaint} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>
        </svg>
      </div>
    </div>
  );
}

// ── Мелкие хелперы ───────────────────────────────────────────
function SectionLabel({ t, children, mt = 4 }) {
  return (
    <div style={{ ...M.t.group(t), margin: `${mt}px 2px 10px` }}>{children}</div>
  );
}

function TimeField({ t, label, value }) {
  return (
    <div style={{ flex: 1, padding: '12px 16px' }}>
      <div style={{ fontSize: 12, color: t.textMuted, marginBottom: 3 }}>{label}</div>
      <div style={{ ...M.t.valueMono(t), fontSize: 20 }}>{value}</div>
    </div>
  );
}

function plur(n, one, few, many) {
  const m10 = n % 10, m100 = n % 100;
  if (m100 >= 11 && m100 <= 14) return many;
  if (m10 === 1) return one;
  if (m10 >= 2 && m10 <= 4) return few;
  return many;
}
