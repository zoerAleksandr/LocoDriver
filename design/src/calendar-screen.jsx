// Календарь — единый экран: поездки + отвлечения в одном месте
// 3 варианта на iOS light. Каждый вариант = один артборд DCArtboard.
//
// События:
//   trip     — поездка (синяя)
//   vacation — отпуск (зелёная заливка)
//   sick     — больничный (оранжевая заливка)
//
// ▸ Variant A — iOS-нативный: точки-индикаторы под числом + agenda снизу
// ▸ Variant B — Карточный месяц: время видно прямо в ячейке (как сейчас),
//               отвлечения — мягкая заливка ячейки. FAB снизу.
// ▸ Variant C — Agenda-first: тонкая неделя-стрип сверху, лента событий
//               вертикально. Хорошо для тех, у кого много дней пустых.

const CAL_MONTH = { year: 2026, month: 4, label: 'Май 2026' };

// число дня → ['ЧЧ:ММ', ...]  (трип ID-шка генерится из времени)
const CAL_TRIPS = {
   2: ['22:10'],
   6: ['08:00'],
   8: ['00:27', '21:00'],
  10: ['05:01'],
  12: ['20:00'],
  13: ['11:00'],
  15: ['07:45'],
  16: ['03:15'],
  17: ['17:01'],
  19: ['16:45'],
  20: ['11:00'],
  22: ['08:46'],
};

const CAL_ABSENCE = {
  4:  { type: 'sick',     label: 'Больничный' },
  5:  { type: 'sick',     label: 'Больничный' },
  11: { type: 'dayoff',   label: 'Выходной' },
  21: { type: 'dayoff',   label: 'Выходной' },
  25: { type: 'vacation', label: 'Отпуск' },
  26: { type: 'vacation', label: 'Отпуск' },
  27: { type: 'vacation', label: 'Отпуск' },
  28: { type: 'vacation', label: 'Отпуск' },
};

// Короткая метка типа отвлечения для угла ячейки календаря.
function absBadge(type) {
  return { vacation: 'ОТП', sick: 'Б/Л', dayoff: 'ВЫХ' }[type] || 'ОТВ';
}

const CAL_TODAY = 15;
const WEEK_RU = ['Пн','Вт','Ср','Чт','Пт','Сб','Вс'];

function calColors() {
  return {
    trip:     { solid: '#00A0F5', soft: 'rgba(0,160,245,0.10)', softer: 'rgba(0,160,245,0.05)' },
    vacation: { solid: '#34C759', soft: 'rgba(52,199,89,0.16)',  softer: 'rgba(52,199,89,0.08)' },
    sick:     { solid: '#FF9F0A', soft: 'rgba(255,159,10,0.18)', softer: 'rgba(255,159,10,0.09)' },
    dayoff:   { solid: '#F4433C', soft: 'rgba(244,67,60,0.16)', softer: 'rgba(244,67,60,0.08)' },
  };
}

function buildMonthGrid(year, month) {
  const first = new Date(year, month, 1);
  const offset = (first.getDay() + 6) % 7; // ПН=0
  const days = new Date(year, month + 1, 0).getDate();
  const cells = [];
  for (let i = 0; i < offset; i++) cells.push(null);
  for (let d = 1; d <= days; d++) cells.push(d);
  while (cells.length % 7) cells.push(null);
  return cells;
}

// ════════════════════════════════════════════════════════════
// Variant A — iOS Calendar style + agenda снизу
// ════════════════════════════════════════════════════════════
function CalendarVariantA({ dark = false, selected = 15 }) {
  const t = dark ? M.dark : M.light;
  const C = calColors();
  const cells = buildMonthGrid(CAL_MONTH.year, CAL_MONTH.month);

  return (
    <MDevice dark={dark}>
      {/* Nav header */}
      <div style={{ paddingTop: 58 }}>
        <div style={{
          padding: '6px 12px 4px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <button style={pillBtn(t, { w: 40, h: 40 })}><IcChevronLeft width="18" height="18"/></button>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{CAL_MONTH.label}</div>
            <div style={{
              fontSize: 11, color: t.textMuted, marginTop: 2,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
            }}>129:55 · 12 поездок · 4 отпуск</div>
          </div>
          <button style={pillBtn(t, { w: 40, h: 40 })}><IcChevronRight width="18" height="18"/></button>
        </div>
      </div>

      {/* Week header */}
      <div style={{
        padding: '12px 8px 4px',
        display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)',
      }}>
        {WEEK_RU.map((d, i) => (
          <div key={d} style={{
            textAlign: 'center', fontSize: 11, fontWeight: 600,
            color: i >= 5 ? t.accent : t.textMuted, letterSpacing: 0.5,
          }}>{d.toUpperCase()}</div>
        ))}
      </div>

      {/* Month grid */}
      <div style={{
        padding: '0 8px 8px',
        display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', rowGap: 0,
      }}>
        {cells.map((d, i) => {
          if (d == null) return <div key={i} style={{ height: 50 }}/>;
          const trips = CAL_TRIPS[d];
          const abs = CAL_ABSENCE[d];
          const isSelected = d === selected;
          const isToday = d === CAL_TODAY;
          const isWeekend = (i % 7) >= 5;

          return (
            <div key={i} style={{
              height: 50, display: 'flex', flexDirection: 'column',
              alignItems: 'center', justifyContent: 'flex-start',
              paddingTop: 4, cursor: 'pointer',
            }}>
              <div style={{
                width: 30, height: 30, borderRadius: 15,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: isSelected ? t.accent
                  : isToday ? 'rgba(0,160,245,0.12)'
                  : 'transparent',
                color: isSelected ? '#fff'
                  : isToday ? t.accent
                  : isWeekend ? t.textMuted : t.text,
                fontWeight: isSelected || isToday ? 700 : 500,
                fontSize: 15,
                fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              }}>{d}</div>
              <div style={{ marginTop: 3, display: 'flex', gap: 3, height: 5 }}>
                {abs && <span style={{ width: 5, height: 5, borderRadius: 3, background: C[abs.type].solid }}/>}
                {trips && trips.slice(0, 3).map((_, k) => (
                  <span key={k} style={{ width: 5, height: 5, borderRadius: 3, background: C.trip.solid }}/>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      {/* Agenda */}
      <CalAgendaA t={t} C={C} selected={selected}/>
    </MDevice>
  );
}

function CalAgendaA({ t, C, selected }) {
  const trips = CAL_TRIPS[selected] || [];
  const abs = CAL_ABSENCE[selected];
  const wd = (new Date(CAL_MONTH.year, CAL_MONTH.month, selected).getDay() + 6) % 7;
  const totalH = trips.length * 4 + Math.floor(Math.random() * 60); // dummy

  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0,
      background: t.surface,
      borderTop: `1px solid ${t.border}`,
      borderTopLeftRadius: 24, borderTopRightRadius: 24,
      paddingBottom: 32, paddingTop: 8,
      height: 360,
      display: 'flex', flexDirection: 'column',
      boxShadow: '0 -8px 24px rgba(0,0,0,0.04)',
    }}>
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 10 }}>
        <div style={{ width: 36, height: 5, borderRadius: 3, background: t.borderStrong }}/>
      </div>

      <div style={{
        padding: '0 20px 12px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
      }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 800, color: t.text, letterSpacing: -0.3 }}>
            {selected} мая
          </div>
          <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>
            {WEEK_RU[wd]}{trips.length > 0 && ` · ${trips.length === 1 ? '1 поездка' : trips.length + ' поездки'} · 11:40`}
            {abs && trips.length === 0 && ` · ${abs.label}`}
          </div>
        </div>
        <button style={{
          height: 36, padding: '0 14px', borderRadius: 18,
          background: t.accent, color: '#fff', border: 'none',
          fontWeight: 600, fontSize: 14,
          display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer',
        }}>
          <span style={{ fontSize: 18, fontWeight: 400, marginTop: -2 }}>+</span>
          Добавить
        </button>
      </div>

      <div style={{
        padding: '0 16px 8px', display: 'flex', flexDirection: 'column', gap: 8,
        overflowY: 'auto', flex: 1,
      }}>
        {abs && (
          <CalEventCard t={t} color={C[abs.type]} title={abs.label} sub="Весь день" badge={abs.type === 'vacation' ? '🌴' : '🏥'}/>
        )}
        {trips.map((time, k) => (
          <CalEventCard key={k} t={t} color={C.trip}
            title={`Поездка №${112 + k}`}
            sub={`Явка ${time} · Москва → Тверь`}
            meta="04:35"/>
        ))}
        {trips.length === 0 && !abs && (
          <div style={{
            padding: '40px 24px', textAlign: 'center', color: t.textMuted, fontSize: 14,
            lineHeight: 1.4,
          }}>
            Нет событий в этот день.<br/>Нажмите «Добавить», чтобы создать поездку или отметить отвлечение.
          </div>
        )}
      </div>
    </div>
  );
}

function CalEventCard({ t, color, title, sub, meta, badge }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '12px 14px',
      background: color.soft, borderRadius: 14,
    }}>
      <div style={{ width: 4, alignSelf: 'stretch', borderRadius: 2, background: color.solid }}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{title}</div>
        {sub && <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>{sub}</div>}
      </div>
      {meta && (
        <div style={{
          fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
          fontWeight: 700, fontSize: 15, color: t.text,
        }}>{meta}</div>
      )}
    </div>
  );
}

// ════════════════════════════════════════════════════════════
// Variant B — Карточный месяц, время в ячейке + FAB
// ════════════════════════════════════════════════════════════
function CalendarVariantB({ dark = false }) {
  const t = dark ? M.dark : M.light;
  const C = calColors();
  const cells = buildMonthGrid(CAL_MONTH.year, CAL_MONTH.month);

  return (
    <MDevice dark={dark}>
      <div style={{ paddingTop: 58 }}>
        <div style={{
          padding: '8px 20px 4px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
        }}>
          <div>
            <div style={{ fontSize: 13, fontWeight: 600, color: t.textMuted, letterSpacing: 0.4, textTransform: 'uppercase' }}>
              Май 2026
            </div>
            <div style={{
              fontSize: 36, fontWeight: 800, color: t.text, marginTop: 0,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums', letterSpacing: -1.2, lineHeight: 1,
            }}>129:55</div>
            <div style={{ fontSize: 12, color: t.textMuted, marginTop: 4 }}>
              12 поездок · 4 дня отпуска · 2 больничных
            </div>
          </div>
          <div style={{ display: 'flex', gap: 6 }}>
            <button style={pillBtn(t, { w: 38, h: 38 })}><IcChevronLeft width="16" height="16"/></button>
            <button style={pillBtn(t, { w: 38, h: 38 })}><IcChevronRight width="16" height="16"/></button>
          </div>
        </div>
      </div>

      {/* Segmented filter */}
      <div style={{ padding: '14px 16px 8px' }}>
        <div style={{
          display: 'flex', padding: 3, borderRadius: 10, background: t.bgSubtle, gap: 2,
        }}>
          {['Всё', 'Поездки', 'Отвлечения'].map((label, i) => (
            <div key={label} style={{
              flex: 1, textAlign: 'center', padding: '7px 0',
              fontSize: 13, fontWeight: 600,
              color: i === 0 ? t.text : t.textMuted,
              background: i === 0 ? t.surface : 'transparent',
              borderRadius: 8,
              boxShadow: i === 0 ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
            }}>{label}</div>
          ))}
        </div>
      </div>

      <div style={{
        padding: '4px 8px 4px',
        display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4,
      }}>
        {WEEK_RU.map((d, i) => (
          <div key={d} style={{
            textAlign: 'center', fontSize: 10, fontWeight: 700,
            color: i >= 5 ? t.accent : t.textFaint, letterSpacing: 0.6, padding: '4px 0',
          }}>{d.toUpperCase()}</div>
        ))}
      </div>

      <div style={{
        padding: '0 8px 80px',
        display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4,
      }}>
        {cells.map((d, i) => <CalCellB key={i} t={t} C={C} d={d} idx={i}/>)}
      </div>

      {/* Legend */}
      <div style={{
        position: 'absolute', left: 16, right: 16, bottom: 28,
        display: 'flex', gap: 14, padding: '10px 14px', borderRadius: 14,
        background: t.surface, border: `1px solid ${t.border}`,
        fontSize: 12, color: t.textMuted,
        boxShadow: '0 4px 16px rgba(0,0,0,0.04)',
      }}>
        <LegendDot color={C.trip.solid} label="Поездка"/>
        <LegendDot color={C.vacation.solid} label="Отпуск"/>
        <LegendDot color={C.sick.solid} label="Больничный"/>
      </div>

      <button style={{
        position: 'absolute', right: 20, bottom: 92, width: 56, height: 56, borderRadius: 28,
        background: t.accent, color: '#fff', border: 'none',
        boxShadow: '0 8px 24px rgba(0,160,245,0.4), 0 2px 6px rgba(0,0,0,0.12)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 30, fontWeight: 300, lineHeight: 1, cursor: 'pointer',
      }}>+</button>
    </MDevice>
  );
}

function CalCellB({ t, C, d, idx }) {
  if (d == null) return <div style={{ height: 68 }}/>;
  const trips = CAL_TRIPS[d];
  const abs = CAL_ABSENCE[d];
  const isToday = d === CAL_TODAY;
  const isWeekend = (idx % 7) >= 5;
  const ac = abs ? C[abs.type] : null;

  return (
    <div style={{
      height: 68, borderRadius: 10, padding: '5px 5px 4px',
      background: abs ? ac.soft : (trips ? C.trip.softer : t.surface),
      border: `1px solid ${abs ? ac.solid + '44' : (trips ? C.trip.solid + '22' : t.border)}`,
      display: 'flex', flexDirection: 'column',
      position: 'relative', overflow: 'hidden',
    }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <span style={{
          fontSize: 13, fontWeight: isToday ? 800 : 600,
          color: isToday ? '#fff' : isWeekend ? t.textMuted : t.text,
          background: isToday ? t.accent : 'transparent',
          width: 20, height: 20, borderRadius: 10,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
        }}>{d}</span>
      </div>
      {trips && (
        <div style={{ marginTop: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
          {trips.slice(0, 2).map((time, k) => (
            <div key={k} style={{
              fontSize: 11, fontWeight: 700, color: C.trip.solid,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              lineHeight: 1.2,
            }}>{time}</div>
          ))}
          {trips.length > 2 && (
            <div style={{ fontSize: 9, color: t.textMuted }}>+{trips.length - 2}</div>
          )}
        </div>
      )}
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

function LegendDot({ color, label }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <span style={{ width: 8, height: 8, borderRadius: 4, background: color }}/>
      <span>{label}</span>
    </div>
  );
}

// ════════════════════════════════════════════════════════════
// Variant C — Agenda-first: тонкая неделя сверху + лента событий
// ════════════════════════════════════════════════════════════
function CalendarVariantC({ dark = false, selected = 15 }) {
  const t = dark ? M.dark : M.light;
  const C = calColors();

  // Выбираем неделю с выбранным днём
  const weekStart = selected - ((new Date(CAL_MONTH.year, CAL_MONTH.month, selected).getDay() + 6) % 7);
  const weekDays = Array.from({ length: 7 }, (_, i) => weekStart + i);

  // Список событий месяца, отсортированный
  const items = [];
  Object.keys(CAL_TRIPS).map(Number).forEach(d => {
    CAL_TRIPS[d].forEach((time, k) => items.push({ d, kind: 'trip', time, idx: k }));
  });
  Object.keys(CAL_ABSENCE).map(Number).forEach(d => {
    items.push({ d, kind: 'absence', abs: CAL_ABSENCE[d] });
  });
  items.sort((a, b) => a.d - b.d);

  // Группировка по дню
  const byDay = {};
  items.forEach(it => {
    (byDay[it.d] = byDay[it.d] || []).push(it);
  });
  const dayKeys = Object.keys(byDay).map(Number).sort((a, b) => a - b);

  return (
    <MDevice dark={dark}>
      {/* Header */}
      <div style={{ paddingTop: 58 }}>
        <div style={{
          padding: '8px 16px 4px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div>
            <div style={{ fontSize: 22, fontWeight: 800, color: t.text, letterSpacing: -0.3 }}>
              {CAL_MONTH.label}
            </div>
            <div style={{ fontSize: 12, color: t.textMuted, marginTop: 2,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
            }}>129:55 отработано</div>
          </div>
          <div style={{ display: 'flex', gap: 6 }}>
            <button style={pillBtn(t, { w: 40, h: 40 })}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Week strip */}
      <div style={{
        padding: '14px 8px 14px', margin: '12px 12px 0',
        background: t.surface, borderRadius: 16, border: `1px solid ${t.border}`,
        display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)',
      }}>
        {weekDays.map((d, i) => {
          if (d < 1 || d > 31) return <div key={i}/>;
          const isSelected = d === selected;
          const isToday = d === CAL_TODAY;
          const trips = CAL_TRIPS[d];
          const abs = CAL_ABSENCE[d];
          const isWeekend = i >= 5;
          return (
            <div key={i} style={{
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, cursor: 'pointer',
            }}>
              <div style={{
                fontSize: 10, fontWeight: 700, letterSpacing: 0.4,
                color: isWeekend ? t.accent : t.textMuted,
              }}>{WEEK_RU[i].toUpperCase()}</div>
              <div style={{
                width: 36, height: 36, borderRadius: 18,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: isSelected ? t.accent : isToday ? 'rgba(0,160,245,0.12)' : 'transparent',
                color: isSelected ? '#fff' : isToday ? t.accent : t.text,
                fontWeight: 700, fontSize: 16,
                fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              }}>{d}</div>
              <div style={{ display: 'flex', gap: 2, height: 4 }}>
                {abs && <span style={{ width: 4, height: 4, borderRadius: 2, background: C[abs.type].solid }}/>}
                {trips && <span style={{ width: 4, height: 4, borderRadius: 2, background: C.trip.solid }}/>}
              </div>
            </div>
          );
        })}
      </div>

      {/* Toggle: неделя / месяц */}
      <div style={{
        display: 'flex', justifyContent: 'center', padding: '6px 0 4px',
      }}>
        <div style={{
          fontSize: 12, color: t.textMuted, display: 'flex', alignItems: 'center', gap: 4,
        }}>
          <span>Показать месяц</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
      </div>

      {/* Agenda list */}
      <div style={{
        padding: '4px 16px 100px', overflowY: 'auto',
        height: 'calc(100% - 270px)',
      }}>
        {dayKeys.map(d => {
          const wd = (new Date(CAL_MONTH.year, CAL_MONTH.month, d).getDay() + 6) % 7;
          const isToday = d === CAL_TODAY;
          return (
            <div key={d} style={{ marginTop: 12 }}>
              <div style={{
                display: 'flex', alignItems: 'baseline', gap: 8,
                padding: '0 4px 8px',
              }}>
                <span style={{
                  fontSize: 22, fontWeight: 800,
                  color: isToday ? t.accent : t.text,
                  fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
                }}>{String(d).padStart(2, '0')}</span>
                <span style={{ fontSize: 13, fontWeight: 600, color: t.textMuted }}>
                  {WEEK_RU[wd]}
                </span>
                {isToday && <span style={{
                  fontSize: 10, fontWeight: 700, color: t.accent,
                  background: 'rgba(0,160,245,0.12)', padding: '2px 6px', borderRadius: 4,
                  textTransform: 'uppercase', letterSpacing: 0.5,
                }}>Сегодня</span>}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {byDay[d].map((it, k) => {
                  if (it.kind === 'absence') {
                    const c = C[it.abs.type];
                    return (
                      <div key={k} style={{
                        padding: '12px 14px', borderRadius: 12,
                        background: c.soft, display: 'flex', alignItems: 'center', gap: 12,
                      }}>
                        <div style={{ width: 4, alignSelf: 'stretch', borderRadius: 2, background: c.solid }}/>
                        <div style={{ flex: 1 }}>
                          <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{it.abs.label}</div>
                          <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>Весь день</div>
                        </div>
                      </div>
                    );
                  }
                  return (
                    <div key={k} style={{
                      padding: '12px 14px', borderRadius: 12,
                      background: t.surface, border: `1px solid ${t.border}`,
                      display: 'flex', alignItems: 'center', gap: 12,
                    }}>
                      <div style={{
                        width: 56, textAlign: 'left',
                        fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
                        fontSize: 17, fontWeight: 700, color: t.text,
                      }}>{it.time}</div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>
                          Поездка №{112 + it.d}
                        </div>
                        <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>
                          Москва → Тверь · 04:35
                        </div>
                      </div>
                      <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      {/* FAB */}
      <button style={{
        position: 'absolute', right: 20, bottom: 28, width: 56, height: 56, borderRadius: 28,
        background: t.accent, color: '#fff', border: 'none',
        boxShadow: '0 8px 24px rgba(0,160,245,0.4), 0 2px 6px rgba(0,0,0,0.12)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 30, fontWeight: 300, lineHeight: 1, cursor: 'pointer',
      }}>+</button>
    </MDevice>
  );
}
