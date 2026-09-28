// Календарь · объединённый экран (поездки + отвлечения)
// Микс: шапка + сетка из варианта B + день-шторка из варианта A
//
// Состояния:
//   • Главный экран — статичный календарь, день 15 выбран по умолчанию
//   • Шторка дня — детали выбранного дня (поездки или отвлечение)
//   • Action-меню «+» — выбор типа: Поездка / Отпуск / Больничный
//   • Шторка «Добавить поездку» — время + сохранить
//   • Шторка «Добавить отпуск/больничный» — диапазон дат
//
// CAL_TRIPS / CAL_ABSENCE / WEEK_RU / calColors / buildMonthGrid
// — переиспользую из calendar-screen.jsx.

// ─── расширенные данные поездок (для деталей в шторке)
const CAL_TRIP_DETAILS = {
  15: [{ num: 'Поездка №115', time: '07:45', from: 'Москва', to: 'Тверь', hours: '04:35', date: '15.05', status: 'draft' }],
  17: [{ num: 'Поездка №117', time: '17:01', from: 'Тверь', to: 'Москва', hours: '04:20', date: '17.05', status: 'synced' }],
  19: [{ num: 'Поездка №118', time: '16:45', from: 'Москва', to: 'Бологое', hours: '06:12', date: '19.05', status: 'favorite' }],
   8: [
    { num: 'Поездка №108', time: '00:27', from: 'Тверь', to: 'Москва', hours: '04:08', date: '08.05', status: 'synced' },
    { num: 'Поездка №109', time: '21:00', from: 'Москва', to: 'Тверь', hours: '05:24', date: '08.05', status: 'draft' },
  ],
};

function getDayEvents(d) {
  const trips = CAL_TRIP_DETAILS[d] || (CAL_TRIPS[d] || []).map((time, k) => ({
    num: `Поездка №${110 + d}`, time, from: 'Москва', to: 'Тверь',
    hours: '04:35', date: `${String(d).padStart(2,'0')}.05`, status: 'draft',
  }));
  const abs = CAL_ABSENCE[d] || null;
  return { trips, abs };
}

// ─── основной компонент — интерактивный
function CalendarMerged({ dark = false, initialSelected = 15, initialModal = null }) {
  const t = dark ? M.dark : M.light;
  const C = calColors();
  const cells = buildMonthGrid(CAL_MONTH.year, CAL_MONTH.month);

  const [selected, setSelected] = React.useState(initialSelected);
  const [modal, setModal] = React.useState(initialModal);
  // modal: null | 'addMenu' | 'addTrip' | 'addVacation' | 'addSick'

  const filterRef = React.useRef('all');
  const [filter, setFilter] = React.useState('all'); // all | trips | absences

  // подсчёт длительности отвлечения, если выбранный день — часть диапазона
  const absRange = React.useMemo(() => {
    if (!CAL_ABSENCE[selected]) return null;
    const type = CAL_ABSENCE[selected].type;
    let s = selected, e = selected;
    while (CAL_ABSENCE[s - 1] && CAL_ABSENCE[s - 1].type === type) s--;
    while (CAL_ABSENCE[e + 1] && CAL_ABSENCE[e + 1].type === type) e++;
    return { start: s, end: e, days: e - s + 1, type };
  }, [selected]);

  return (
    <MDevice dark={dark}>
      {/* ─── Шапка из варианта B ─── */}
      <div style={{ paddingTop: 58 }}>
        <div style={{
          padding: '8px 20px 4px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
        }}>
          <div>
            <div style={{
              fontSize: 12, fontWeight: 700, color: t.textMuted,
              letterSpacing: 0.6, textTransform: 'uppercase',
            }}>{CAL_MONTH.label}</div>
            <div style={{
              fontSize: 36, fontWeight: 800, color: t.text, marginTop: 2,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              letterSpacing: -1.2, lineHeight: 1,
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

      {/* ─── Сегментный фильтр ─── */}
      <div style={{ padding: '14px 16px 8px' }}>
        <div style={{
          display: 'flex', padding: 3, borderRadius: 10, background: t.bgSubtle, gap: 2,
        }}>
          {[
            { id: 'all', label: 'Всё' },
            { id: 'trips', label: 'Поездки' },
            { id: 'absences', label: 'Отвлечения' },
          ].map(s => (
            <div key={s.id} onClick={() => setFilter(s.id)} style={{
              flex: 1, textAlign: 'center', padding: '7px 0',
              fontSize: 13, fontWeight: 600,
              color: filter === s.id ? t.text : t.textMuted,
              background: filter === s.id ? t.surface : 'transparent',
              borderRadius: 8,
              boxShadow: filter === s.id ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
              cursor: 'pointer',
            }}>{s.label}</div>
          ))}
        </div>
      </div>

      {/* ─── Шапка дней недели ─── */}
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

      {/* ─── Сетка месяца ─── */}
      <div style={{
        padding: '0 8px 24px',
        display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4,
      }}>
        {cells.map((d, i) => (
          <CalCellMerged
            key={i} t={t} C={C} d={d} idx={i}
            filter={filter}
            isSelected={d === selected}
            onClick={() => d && setSelected(d)}
          />
        ))}
      </div>

      {/* ─── Шторка выбранного дня ─── */}
      <DaySheet
        t={t} C={C}
        selected={selected}
        absRange={absRange}
        onAdd={() => setModal('addMenu')}
      />

      {/* ─── Модалки поверх ─── */}
      {modal === 'addMenu' && (
        <AddMenu t={t} selected={selected} onClose={() => setModal(null)}
          onPick={kind => setModal(kind)}/>
      )}
      {modal === 'addTrip' && (
        <AddTripSheet t={t} selected={selected} onClose={() => setModal(null)}/>
      )}
      {(modal === 'addVacation' || modal === 'addSick') && (
        <AddAbsenceSheet t={t} C={C}
          kind={modal === 'addVacation' ? 'vacation' : 'sick'}
          selected={selected}
          onClose={() => setModal(null)}/>
      )}
    </MDevice>
  );
}

// ═══════════════════════════════════════════════════════════════════
// Ячейка месяца
// ═══════════════════════════════════════════════════════════════════
function CalCellMerged({ t, C, d, idx, filter, isSelected, onClick }) {
  if (d == null) return <div style={{ height: 64 }}/>;

  const tripsAll = CAL_TRIPS[d];
  const abs = CAL_ABSENCE[d];
  const trips = filter === 'absences' ? null : tripsAll;
  const showAbs = filter === 'trips' ? null : abs;
  const isToday = d === CAL_TODAY;
  const isWeekend = (idx % 7) >= 5;
  const ac = showAbs ? C[showAbs.type] : null;

  // Стиль фона — выбранный день НЕ перекрашиваем, сохраняем его цвет
  // (отвлечение / поездка), добавляем только синюю обводку.
  const bg = showAbs ? ac.soft
    : (trips ? C.trip.softer : t.surface);
  const border = isSelected ? `2px solid ${t.accent}`
    : `1px solid ${showAbs ? ac.solid + '44' : (trips ? C.trip.solid + '22' : t.border)}`;

  return (
    <div onClick={onClick} style={{
      height: 64, borderRadius: 10, padding: isSelected ? '4px 4px 3px' : '5px 5px 4px',
      background: bg, border,
      display: 'flex', flexDirection: 'column',
      position: 'relative', overflow: 'hidden',
      cursor: 'pointer',
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
      {showAbs && (
        <div style={{
          marginTop: 'auto', textAlign: 'right',
          fontSize: 9, fontWeight: 700, color: ac.solid,
          textTransform: 'uppercase', letterSpacing: 0.3,
        }}>{absBadge(showAbs.type)}</div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// Day sheet — детали выбранного дня
// ═══════════════════════════════════════════════════════════════════
function DaySheet({ t, C, selected, absRange, onAdd }) {
  const { trips, abs } = getDayEvents(selected);
  const wd = (new Date(CAL_MONTH.year, CAL_MONTH.month, selected).getDay() + 6) % 7;

  // Заголовок дня + summary
  const totalH = trips.reduce((s, tr) => {
    const [h, m] = tr.hours.split(':').map(Number);
    return s + h * 60 + m;
  }, 0);
  const totalLabel = totalH ? `${Math.floor(totalH/60)}:${String(totalH%60).padStart(2,'0')}` : null;

  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0,
      background: t.surface,
      borderTop: `1px solid ${t.border}`,
      borderTopLeftRadius: 24, borderTopRightRadius: 24,
      paddingBottom: 32, paddingTop: 8,
      height: 320,
      display: 'flex', flexDirection: 'column',
      boxShadow: '0 -8px 24px rgba(0,0,0,0.06)',
    }}>
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}>
        <div style={{ width: 36, height: 5, borderRadius: 3, background: t.borderStrong }}/>
      </div>

      <div style={{
        padding: '4px 20px 14px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
      }}>
        <div>
          <div style={{
            fontSize: 22, fontWeight: 800, color: t.text, letterSpacing: -0.3,
          }}>
            {selected} мая
            <span style={{
              fontSize: 14, fontWeight: 600, color: t.textMuted, marginLeft: 8,
            }}>{WEEK_RU[wd]}</span>
          </div>
          <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>
            {trips.length > 0 && `${trips.length === 1 ? '1 поездка' : trips.length + ' поездки'} · ${totalLabel}`}
            {!trips.length && abs && `${abs.label}`}
            {!trips.length && !abs && 'Нет событий'}
          </div>
        </div>
        <button onClick={onAdd} style={{
          height: 36, padding: '0 14px', borderRadius: 18,
          background: t.accent, color: '#fff', border: 'none',
          fontWeight: 600, fontSize: 14,
          display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer',
        }}>
          <span style={{ fontSize: 18, fontWeight: 400, marginTop: -2 }}>+</span>
          Добавить
        </button>
      </div>

      {/* Список */}
      <div style={{
        padding: '0 16px 8px', display: 'flex', flexDirection: 'column', gap: 8,
        overflowY: 'auto', flex: 1,
      }}>
        {abs && (
          <AbsenceCard t={t} C={C} abs={abs} range={absRange}/>
        )}
        {trips.map((tr, k) => (
          <TripItemCard key={k} t={t} trip={tr}/>
        ))}
        {!abs && trips.length === 0 && (
          <div style={{
            padding: '40px 24px', textAlign: 'center', color: t.textMuted, fontSize: 14, lineHeight: 1.4,
          }}>
            Нет событий в этот день.<br/>
            Нажмите «Добавить», чтобы создать поездку или отметить отвлечение.
          </div>
        )}
      </div>
    </div>
  );
}

// Поездка — как item с главного экрана (TripRow), но в карточке
function TripItemCard({ t, trip }) {
  return (
    <div style={{
      padding: '14px 16px',
      background: t.bg, borderRadius: 14,
      border: `1px solid ${t.border}`,
      display: 'flex', alignItems: 'center', gap: 12,
      cursor: 'pointer',
    }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 15, fontFamily: M.fontMono, color: t.text, fontWeight: 500,
          fontVariantNumeric: 'tabular-nums',
        }}>{trip.date} {trip.time}</div>
        <div style={{ fontSize: 12, color: t.textMuted, fontFamily: M.fontMono, marginTop: 2 }}>
          {trip.num}
        </div>
      </div>
      <div style={{
        fontFamily: M.fontMono, fontSize: 16, fontWeight: 600, color: t.text,
        fontVariantNumeric: 'tabular-nums',
      }}>{trip.hours}</div>
      <div style={{
        color: trip.status === 'favorite' ? t.accent
          : trip.status === 'synced' ? t.success : t.textFaint,
      }}>
        {trip.status === 'favorite' && <IcHeart width="16" height="16"/>}
        {trip.status === 'synced' && <IcCloud width="16" height="16"/>}
        {trip.status === 'draft' && <IcCloudOff width="16" height="16"/>}
      </div>
    </div>
  );
}

// Отвлечение — карточка с типом и длительностью
function AbsenceCard({ t, C, abs, range }) {
  const c = C[abs.type];
  const isDayoff = abs.type === 'dayoff';
  // Чистый SVG-значок вместо эмодзи: солнце для отпуска, мед-крест для больничного,
  // полумесяц для выходного (день отдыха).
  const glyph = abs.type === 'vacation' ? (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="4"/>
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>
    </svg>
  ) : isDayoff ? (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="17" rx="2.5"/><path d="M3 9h18M8 2v4M16 2v4"/>
    </svg>
  ) : (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="6" width="18" height="14" rx="3"/>
      <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
      <path d="M12 10v6M9 13h6"/>
    </svg>
  );
  const hours = range ? range.days * 8 : 8; // условно 8 рабочих часов в день
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{
        padding: '14px 16px',
        background: c.soft, borderRadius: 14,
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <div style={{ width: 4, alignSelf: 'stretch', borderRadius: 2, background: c.solid }}/>
        <div style={{
          width: 40, height: 40, borderRadius: 20,
          background: c.solid, color: '#fff',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>{glyph}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: t.text }}>{abs.label}</div>
          <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>
            {range && range.days > 1
              ? `${range.start}–${range.end} мая · ${range.days} ${range.days < 5 ? 'дня' : 'дней'}`
              : 'Весь день'}
          </div>
        </div>
        {isDayoff ? (
          <div style={{ textAlign: 'right' }}>
            <div style={{
              fontSize: 18, fontWeight: 800, color: t.accent,
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums', letterSpacing: -0.3,
            }}>×2</div>
            <div style={{ fontSize: 11, color: t.textMuted, marginTop: 1 }}>тариф</div>
          </div>
        ) : (
          <div style={{
            textAlign: 'right',
            fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
          }}>
            <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{hours}:00</div>
            <div style={{ fontSize: 11, color: t.textMuted, marginTop: 1 }}>часов</div>
          </div>
        )}
      </div>

      {/* Пометка о двойном тарифе — только для выходного */}
      {isDayoff && (
        <div style={{
          display: 'flex', alignItems: 'flex-start', gap: 10,
          padding: '12px 14px', borderRadius: 12,
          background: 'rgba(0,160,245,0.08)', border: `1px solid ${t.accent}26`,
        }}>
          <div style={{
            flexShrink: 0, width: 22, height: 22, borderRadius: 11,
            background: t.accent, color: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>
            </svg>
          </div>
          <div style={{ fontSize: 12.5, color: t.text, lineHeight: 1.45 }}>
            Работа в этот день оплачивается по{' '}
            <span style={{ fontWeight: 700, color: t.accent }}>двойному тарифу (×2)</span>.
          </div>
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// Action-меню «+»
// ═══════════════════════════════════════════════════════════════════
function AddMenu({ t, selected, onClose, onPick }) {
  const opts = [
    { id: 'addTrip',     label: 'Поездка',     sub: `Время на ${selected} мая`, color: '#00A0F5', icon: <IcLocomotive width="22" height="22"/> },
    { id: 'addVacation', label: 'Отпуск',      sub: 'Диапазон дат',             color: '#34C759', emoji: '🌴' },
    { id: 'addSick',     label: 'Больничный',  sub: 'Диапазон дат',             color: '#FF9F0A', emoji: '🏥' },
  ];
  return (
    <ModalScrim onClose={onClose}>
      <SheetShell t={t} title="Что добавить?">
        <div style={{ padding: '4px 12px 12px', display: 'flex', flexDirection: 'column', gap: 8 }}>
          {opts.map(o => (
            <div key={o.id} onClick={() => onPick(o.id)} style={{
              padding: '14px 16px', borderRadius: 14, background: t.bg,
              border: `1px solid ${t.border}`,
              display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer',
            }}>
              <div style={{
                width: 44, height: 44, borderRadius: 22,
                background: o.color, color: '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 20,
              }}>{o.emoji || o.icon}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 16, fontWeight: 600, color: t.text }}>{o.label}</div>
                <div style={{ fontSize: 13, color: t.textMuted, marginTop: 1 }}>{o.sub}</div>
              </div>
              <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
            </div>
          ))}
        </div>
      </SheetShell>
    </ModalScrim>
  );
}

// ═══════════════════════════════════════════════════════════════════
// Добавить поездку — только время на выбранную дату
// ═══════════════════════════════════════════════════════════════════
function AddTripSheet({ t, selected, onClose }) {
  return (
    <ModalScrim onClose={onClose}>
      <SheetShell t={t}
        title="Новая поездка"
        sub={`${selected} мая · понедельник`}
        primary="Создать"
        onPrimary={onClose}>
        <div style={{ padding: '4px 16px 16px' }}>
          {/* Время явки — крупный mono ввод */}
          <div style={{
            padding: '20px 18px', borderRadius: 14,
            background: t.bg, border: `1px solid ${t.border}`,
            display: 'flex', alignItems: 'baseline', gap: 14,
          }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: t.textMuted }}>Время явки</div>
            <div style={{
              marginLeft: 'auto',
              fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              fontSize: 32, fontWeight: 700, color: t.text, letterSpacing: -0.5,
            }}>07:45</div>
          </div>

          <div style={{ marginTop: 10, fontSize: 12, color: t.textMuted, padding: '0 4px' }}>
            После создания откроется форма маршрута, чтобы заполнить станции и поезд.
          </div>
        </div>
      </SheetShell>
    </ModalScrim>
  );
}

// ═══════════════════════════════════════════════════════════════════
// Добавить отвлечение — диапазон дат
// ═══════════════════════════════════════════════════════════════════
function AddAbsenceSheet({ t, C, kind, selected, onClose }) {
  const c = C[kind];
  const label = kind === 'vacation' ? 'Отпуск' : 'Больничный';
  const [start, setStart] = React.useState(selected);
  const [end, setEnd] = React.useState(selected + 3);
  const days = end - start + 1;

  return (
    <ModalScrim onClose={onClose}>
      <SheetShell t={t}
        title={`Новый ${label.toLowerCase()}`}
        sub="Выберите диапазон дат"
        primary="Сохранить"
        onPrimary={onClose}>
        <div style={{ padding: '4px 16px 8px' }}>
          {/* Диапазон — два поля */}
          <div style={{
            padding: 12, borderRadius: 14, background: t.bg, border: `1px solid ${t.border}`,
            display: 'flex', alignItems: 'center', gap: 8,
          }}>
            <DateField t={t} label="С" value={`${start} мая`}/>
            <div style={{
              flex: 1, height: 2, background: c.solid, opacity: 0.4, borderRadius: 1,
            }}/>
            <DateField t={t} label="По" value={`${end} мая`}/>
          </div>

          {/* Мини-полоса диапазона */}
          <div style={{ marginTop: 12, padding: '12px 14px', borderRadius: 14,
            background: c.soft,
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          }}>
            <div>
              <div style={{ fontSize: 13, color: t.textMuted }}>Всего</div>
              <div style={{
                fontSize: 22, fontWeight: 800, color: t.text, marginTop: 2,
                fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              }}>
                {days} {days === 1 ? 'день' : days < 5 ? 'дня' : 'дней'}
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 13, color: t.textMuted }}>Часов</div>
              <div style={{
                fontSize: 22, fontWeight: 800, color: t.text, marginTop: 2,
                fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
              }}>{days * 8}:00</div>
            </div>
          </div>

          <div style={{ marginTop: 12, fontSize: 12, color: t.textMuted, padding: '0 4px', lineHeight: 1.4 }}>
            {kind === 'vacation'
              ? 'Дни отпуска войдут в норму как отработанные при расчёте зарплаты.'
              : 'Больничные дни не влияют на норму, но учитываются в общем балансе.'}
          </div>
        </div>
      </SheetShell>
    </ModalScrim>
  );
}

function DateField({ t, label, value }) {
  return (
    <div style={{
      flex: 1, padding: '8px 12px', borderRadius: 10,
      background: t.surface, border: `1px solid ${t.border}`, cursor: 'pointer',
    }}>
      <div style={{ fontSize: 11, fontWeight: 600, color: t.textMuted, letterSpacing: 0.3, textTransform: 'uppercase' }}>{label}</div>
      <div style={{
        fontSize: 16, fontWeight: 700, color: t.text, marginTop: 2,
        fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
      }}>{value}</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════
// Generic шторка-каркас
// ═══════════════════════════════════════════════════════════════════
function SheetShell({ t, title, sub, primary, onPrimary, children }) {
  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0,
      background: t.surface,
      borderTopLeftRadius: 24, borderTopRightRadius: 24,
      paddingBottom: 32, paddingTop: 8,
      boxShadow: '0 -16px 40px rgba(0,0,0,0.18)',
      animation: 'sheetSlideUp 280ms cubic-bezier(0.32, 0.72, 0, 1)',
    }}>
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}>
        <div style={{ width: 36, height: 5, borderRadius: 3, background: t.borderStrong }}/>
      </div>
      <div style={{ padding: '4px 20px 14px' }}>
        <div style={{ fontSize: 20, fontWeight: 800, color: t.text }}>{title}</div>
        {sub && <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>{sub}</div>}
      </div>
      {children}
      {primary && (
        <div style={{ padding: '8px 16px 0' }}>
          <button onClick={onPrimary} style={{
            width: '100%', height: 50, borderRadius: 14,
            background: t.accent, color: '#fff', border: 'none',
            fontSize: 16, fontWeight: 700, cursor: 'pointer',
          }}>{primary}</button>
        </div>
      )}
    </div>
  );
}

function ModalScrim({ children, onClose }) {
  return (
    <div onClick={onClose} style={{
      position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)',
      zIndex: 50,
    }}>
      <div onClick={e => e.stopPropagation()} style={{ position: 'absolute', inset: 0 }}>
        {children}
      </div>
    </div>
  );
}
