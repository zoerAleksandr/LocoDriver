// Справочники норм — серии локомотивов и станции
// Экран: список всего → тап → редактор
// Используется и в настройках, и из шторки времени по ссылке "Настроить"

const SERIES_DATA = [
  { code: 'ВЛ80с', type: 'Электровоз', acceptMin: 40, returnMin: 50, lastUsed: '26 апр' },
  { code: 'ЧС7',   type: 'Электровоз', acceptMin: 35, returnMin: 45, lastUsed: '14 апр' },
  { code: 'ЭП1М',  type: 'Электровоз', acceptMin: null, returnMin: null, lastUsed: null },
  { code: '2ТЭ10М', type: 'Тепловоз',  acceptMin: 50, returnMin: 60, lastUsed: '22 мар' },
  { code: '2ТЭ116', type: 'Тепловоз',  acceptMin: 45, returnMin: 55, lastUsed: '08 мар' },
];

const STATIONS_DATA = [
  { name: 'Лянгасово',  code: 'ЛНГ', arr1: 20, arr2: 5,  dep1: 10, dep2: 0,    lastUsed: '26 апр' },
  { name: 'Балезино',   code: 'БЛЗ', arr1: 25, arr2: 10, dep1: 15, dep2: 5,    lastUsed: '26 апр' },
  { name: 'Киров',      code: 'КРВ', arr1: 15, arr2: 5,  dep1: 10, dep2: 0,    lastUsed: '20 апр' },
  { name: 'Котельнич',  code: 'КТЛ', arr1: null, arr2: null, dep1: null, dep2: null, lastUsed: null },
  { name: 'Шарья',      code: 'ШРЯ', arr1: 30, arr2: 10, dep1: 15, dep2: 5,    lastUsed: '02 апр' },
];

// ─────────────────────────────────────────────────────────────
// Список серий — iOS
// ─────────────────────────────────────────────────────────────
function IOSSeriesList({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark}>
      <div style={{ paddingTop: 58 }}>
        <NavBar t={t} title="Серии" backLabel="Настройки" trailing="Изменить"/>
      </div>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '12px 16px 32px' }}>
        <Hint t={t}>
          Для каждой серии задаются нормы длительности приёмки и сдачи. Нормы применяются автоматически при заполнении времени в шторке локомотива.
        </Hint>

        <GroupHead t={t} mt={20}>Электровозы · 3</GroupHead>
        <Card t={t}>
          {SERIES_DATA.filter(s => s.type === 'Электровоз').map((s, i, arr) => (
            <React.Fragment key={s.code}>
              {i > 0 && <Sep t={t} inset={20}/>}
              <SeriesRow t={t} {...s}/>
            </React.Fragment>
          ))}
        </Card>

        <GroupHead t={t}>Тепловозы · 2</GroupHead>
        <Card t={t}>
          {SERIES_DATA.filter(s => s.type === 'Тепловоз').map((s, i, arr) => (
            <React.Fragment key={s.code}>
              {i > 0 && <Sep t={t} inset={20}/>}
              <SeriesRow t={t} {...s}/>
            </React.Fragment>
          ))}
        </Card>

        <AddBtn t={t} icon={<IcPlus/>} label="Добавить серию"/>
      </div>
    </MDevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Список станций — iOS
// ─────────────────────────────────────────────────────────────
function IOSStationsList({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark}>
      <div style={{ paddingTop: 58 }}>
        <NavBar t={t} title="Станции" backLabel="Настройки" trailing="Изменить"/>
      </div>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '12px 16px 32px' }}>
        <Hint t={t}>
          Для каждой станции задаются 4 интервала: от явки до начала приёмки, от окончания приёмки до выхода на КП, от захода на КП до начала сдачи, от окончания сдачи до окончания работы.
        </Hint>

        <GroupHead t={t} mt={20}>Часто используемые · {STATIONS_DATA.filter(s => s.lastUsed).length}</GroupHead>
        <Card t={t}>
          {STATIONS_DATA.filter(s => s.lastUsed).map((s, i, arr) => (
            <React.Fragment key={s.code}>
              {i > 0 && <Sep t={t} inset={20}/>}
              <StationRow t={t} {...s}/>
            </React.Fragment>
          ))}
        </Card>

        <GroupHead t={t}>Без норм · 1</GroupHead>
        <Card t={t}>
          {STATIONS_DATA.filter(s => !s.lastUsed).map((s, i, arr) => (
            <React.Fragment key={s.code}>
              {i > 0 && <Sep t={t} inset={20}/>}
              <StationRow t={t} {...s}/>
            </React.Fragment>
          ))}
        </Card>

        <AddBtn t={t} icon={<IcPlus/>} label="Добавить станцию"/>
      </div>
    </MDevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Редактор серии — iOS
// ─────────────────────────────────────────────────────────────
function IOSSeriesEditor({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark}>
      <div style={{ paddingTop: 58 }}>
        <NavBar t={t} title="ВЛ80с" backLabel="Серии" trailing="Готово" trailingAccent/>
      </div>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '12px 16px 32px' }}>
        <Hint t={t}>
          Длительности применяются между «Начало приёмки» → «Окончание приёмки» (и так же для сдачи) в шторке локомотива.
        </Hint>

        <GroupHead t={t} mt={20}>Основное</GroupHead>
        <Card t={t}>
          <FieldRow t={t} label="Серия" value="ВЛ80с"/>
          <Sep t={t} inset={20}/>
          <FieldRow t={t} label="Тип" value="Электровоз" chevron/>
        </Card>

        <GroupHead t={t}>Приёмка</GroupHead>
        <Card t={t}>
          <DurationRow t={t} icon={<IcLogIn/>} label="Длительность" minutes={40}/>
        </Card>

        <GroupHead t={t}>Сдача</GroupHead>
        <Card t={t}>
          <DurationRow t={t} icon={<IcLogOut/>} label="Длительность" minutes={50}/>
        </Card>

        <DangerBtn t={t} icon={<IcTrash/>} label="Удалить серию"/>
      </div>
    </MDevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Редактор станции — iOS
// ─────────────────────────────────────────────────────────────
function IOSStationEditor({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark}>
      <div style={{ paddingTop: 58 }}>
        <NavBar t={t} title="Лянгасово" backLabel="Станции" trailing="Готово" trailingAccent/>
      </div>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '12px 16px 32px' }}>
        <Hint t={t}>
          4 интервала, привязанных к этой станции. Используются автоматически при заполнении времени.
        </Hint>

        <GroupHead t={t} mt={20}>Основное</GroupHead>
        <Card t={t}>
          <FieldRow t={t} label="Название" value="Лянгасово"/>
        </Card>

        <GroupHead t={t}>Приёмка</GroupHead>
        <Card t={t}>
          <DurationRow t={t} icon={<IcCalendar/>} label="Явка → Начало" minutes={20}/>
          <Sep t={t} inset={56}/>
          <DurationRow t={t} icon={<IcBarrier/>} label="Конец → КП" minutes={5}/>
        </Card>

        <GroupHead t={t}>Сдача</GroupHead>
        <Card t={t}>
          <DurationRow t={t} icon={<IcBarrier/>} label="КП → Начало" minutes={10}/>
          <Sep t={t} inset={56}/>
          <DurationRow t={t} icon={<IcWorkClock/>} label="Конец → Окончание работы" minutes={0}/>
        </Card>

        <DangerBtn t={t} icon={<IcTrash/>} label="Удалить станцию"/>
      </div>
    </MDevice>
  );
}

// ═════════════════════════════════════════════════════════════
// Android варианты
// ═════════════════════════════════════════════════════════════
function AndroidSeriesList({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark}>
      <ATopBar t={t} title="Серии локомотивов" backLabel/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px)', padding: '8px 16px 100px' }}>
        <Hint t={t}>
          Для каждой серии задаются нормы длительности приёмки и сдачи. Нормы применяются автоматически.
        </Hint>

        <GroupHead t={t} mt={20}>Электровозы · 3</GroupHead>
        <MCard t={t}>
          {SERIES_DATA.filter(s => s.type === 'Электровоз').map((s, i, arr) => (
            <React.Fragment key={s.code}>
              {i > 0 && <Sep t={t} inset={0}/>}
              <SeriesRow t={t} {...s}/>
            </React.Fragment>
          ))}
        </MCard>

        <GroupHead t={t}>Тепловозы · 2</GroupHead>
        <MCard t={t}>
          {SERIES_DATA.filter(s => s.type === 'Тепловоз').map((s, i, arr) => (
            <React.Fragment key={s.code}>
              {i > 0 && <Sep t={t} inset={0}/>}
              <SeriesRow t={t} {...s}/>
            </React.Fragment>
          ))}
        </MCard>
      </div>

      {/* FAB */}
      <button style={{
        position: 'absolute', bottom: 24, right: 24,
        width: 56, height: 56, borderRadius: 16,
        background: t.accent, color: t.accentInk, border: 'none',
        boxShadow: '0 4px 12px rgba(0,0,0,0.18), 0 2px 4px rgba(0,0,0,0.08)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        zIndex: 50,
      }}><IcPlus width="26" height="26"/></button>
    </ADevice>
  );
}

function AndroidStationsList({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark}>
      <ATopBar t={t} title="Станции" backLabel/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px)', padding: '8px 16px 100px' }}>
        <Hint t={t}>
          4 интервала на станцию: от явки до начала приёмки, от конца приёмки до КП, от КП до начала сдачи, от конца сдачи до конца работы.
        </Hint>

        <GroupHead t={t} mt={20}>Часто используемые · 4</GroupHead>
        <MCard t={t}>
          {STATIONS_DATA.filter(s => s.lastUsed).map((s, i, arr) => (
            <React.Fragment key={s.code}>
              {i > 0 && <Sep t={t} inset={0}/>}
              <StationRow t={t} {...s}/>
            </React.Fragment>
          ))}
        </MCard>

        <GroupHead t={t}>Без норм · 1</GroupHead>
        <MCard t={t}>
          {STATIONS_DATA.filter(s => !s.lastUsed).map((s, i) => (
            <StationRow key={s.code} t={t} {...s}/>
          ))}
        </MCard>
      </div>

      <button style={{
        position: 'absolute', bottom: 24, right: 24,
        width: 56, height: 56, borderRadius: 16,
        background: t.accent, color: t.accentInk, border: 'none',
        boxShadow: '0 4px 12px rgba(0,0,0,0.18), 0 2px 4px rgba(0,0,0,0.08)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        zIndex: 50,
      }}><IcPlus width="26" height="26"/></button>
    </ADevice>
  );
}

function AndroidSeriesEditor({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark}>
      <ATopBar t={t} title="ВЛ80с" backLabel/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px)', padding: '8px 16px 32px' }}>
        <Hint t={t}>
          Длительности применяются для интервалов «Начало → Окончание приёмки» и «Начало → Окончание сдачи».
        </Hint>

        <GroupHead t={t} mt={20}>Основное</GroupHead>
        <MCard t={t}>
          <FieldRow t={t} label="Серия" value="ВЛ80с"/>
          <Sep t={t} inset={0}/>
          <FieldRow t={t} label="Тип" value="Электровоз" chevron/>
        </MCard>

        <GroupHead t={t}>Приёмка</GroupHead>
        <MCard t={t}>
          <DurationRow t={t} icon={<IcLogIn/>} label="Длительность" minutes={40}/>
        </MCard>

        <GroupHead t={t}>Сдача</GroupHead>
        <MCard t={t}>
          <DurationRow t={t} icon={<IcLogOut/>} label="Длительность" minutes={50}/>
        </MCard>

        <DangerBtn t={t} icon={<IcTrash/>} label="Удалить серию"/>
      </div>
    </ADevice>
  );
}

function AndroidStationEditor({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark}>
      <ATopBar t={t} title="Лянгасово" backLabel/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px)', padding: '8px 16px 32px' }}>
        <Hint t={t}>
          4 интервала, привязанных к этой станции. Используются автоматически при заполнении времени.
        </Hint>

        <GroupHead t={t} mt={20}>Основное</GroupHead>
        <MCard t={t}>
          <FieldRow t={t} label="Название" value="Лянгасово"/>
        </MCard>

        <GroupHead t={t}>Приёмка</GroupHead>
        <MCard t={t}>
          <DurationRow t={t} icon={<IcCalendar/>} label="Явка → Начало" minutes={20}/>
          <Sep t={t} inset={56}/>
          <DurationRow t={t} icon={<IcBarrier/>} label="Конец → КП" minutes={5}/>
        </MCard>

        <GroupHead t={t}>Сдача</GroupHead>
        <MCard t={t}>
          <DurationRow t={t} icon={<IcBarrier/>} label="КП → Начало" minutes={10}/>
          <Sep t={t} inset={56}/>
          <DurationRow t={t} icon={<IcWorkClock/>} label="Конец → Окончание работы" minutes={0}/>
        </MCard>

        <DangerBtn t={t} icon={<IcTrash/>} label="Удалить станцию"/>
      </div>
    </ADevice>
  );
}

// ═════════════════════════════════════════════════════════════
// Helpers
// ═════════════════════════════════════════════════════════════

function NavBar({ t, title, backLabel, trailing, trailingAccent }) {
  // iOS-style three-zone bar: back / centered title / trailing.
  // Title is absolutely centered to the bar (which IS position:relative),
  // and clipped with side padding so it never collides with the side buttons.
  const SIDE = 88; // reserved space for back/trailing on each side
  return (
    <div style={{
      position: 'sticky', top: 0, zIndex: 10,
      borderBottom: `1px solid ${t.border}`, background: t.bg,
      height: 44,
      display: 'flex', alignItems: 'center',
    }}>
      {/* Back */}
      <div style={{ flex: '0 0 auto', display: 'flex', alignItems: 'center', paddingLeft: 8, minWidth: 0, maxWidth: '40%' }}>
        <button style={{
          border: 'none', background: 'transparent', color: t.accent,
          display: 'inline-flex', alignItems: 'center', gap: 2,
          fontSize: 16, padding: '4px 6px', cursor: 'pointer',
          minWidth: 0, maxWidth: '100%',
        }}>
          <IcChevronLeft width="18" height="18" style={{ flexShrink: 0 }}/>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{backLabel}</span>
        </button>
      </div>

      {/* Spacer to balance flex layout — actual title is absolutely positioned */}
      <div style={{ flex: 1 }}/>

      {/* Trailing */}
      <div style={{ flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', paddingRight: 12 }}>
        {trailing && (
          <button style={{
            border: 'none', background: 'transparent', color: t.accent,
            fontSize: 16, fontWeight: trailingAccent ? 600 : 400, padding: '4px 4px', cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}>{trailing}</button>
        )}
      </div>

      {/* Centered title — clipped on both sides so it never overlaps */}
      <div style={{
        position: 'absolute', left: SIDE, right: SIDE, top: 0, bottom: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        pointerEvents: 'none',
      }}>
        <div style={{
          fontSize: 17, fontWeight: 600, color: t.text,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          maxWidth: '100%',
        }}>{title}</div>
      </div>
    </div>
  );
}

function ATopBar({ t, title, backLabel }) {
  return (
    <div style={{
      padding: '14px 16px 12px', display: 'flex', alignItems: 'center', gap: 16,
      borderBottom: `1px solid ${t.border}`,
    }}>
      <button style={{
        width: 40, height: 40, border: 'none', background: 'transparent',
        color: t.text, borderRadius: 20, cursor: 'pointer',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      }}><IcChevronLeft width="22" height="22"/></button>
      <div style={{ fontSize: 22, fontWeight: 500, color: t.text, flex: 1 }}>{title}</div>
    </div>
  );
}

function Hint({ t, children }) {
  return (
    <div style={{
      marginTop: 12, padding: '12px 14px', borderRadius: 12,
      background: t.bgSubtle, color: t.textMuted,
      fontSize: 13, lineHeight: 1.45,
    }}>{children}</div>
  );
}

function Sep({ t, inset = 0 }) {
  return <div style={{ height: 1, background: t.border, marginLeft: inset }}/>;
}

function SeriesRow({ t, code, type, acceptMin, returnMin, lastUsed }) {
  const empty = acceptMin == null;
  return (
    <button style={{
      width: '100%', padding: '14px 20px',
      display: 'flex', alignItems: 'center', gap: 14,
      background: 'transparent', border: 'none', cursor: 'pointer', textAlign: 'left',
    }}>
      <div style={{
        minWidth: 56, padding: '4px 8px', borderRadius: 8,
        background: t.accentSoft, color: t.accent,
        fontFamily: M.fontMono, fontSize: 13, fontWeight: 700, letterSpacing: .3,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      }}>{code}</div>
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 3 }}>
        {empty ? (
          <div style={{ fontSize: 13, color: t.warn, fontFamily: M.fontMono }}>
            Нормы не заданы
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, fontFamily: M.fontMono, fontSize: 13 }}>
              <span style={{ color: t.textMuted, minWidth: 56 }}>Приёмка</span>
              <span style={{ color: t.text, fontWeight: 600 }}>{acceptMin} мин</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, fontFamily: M.fontMono, fontSize: 13 }}>
              <span style={{ color: t.textMuted, minWidth: 56 }}>Сдача</span>
              <span style={{ color: t.text, fontWeight: 600 }}>{returnMin} мин</span>
            </div>
          </>
        )}
      </div>
      <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>
    </button>
  );
}

function StationRow({ t, name, code, arr1, arr2, dep1, dep2, lastUsed }) {
  const empty = arr1 == null;
  return (
    <button style={{
      width: '100%', padding: '14px 20px',
      display: 'flex', alignItems: 'center', gap: 14,
      background: 'transparent', border: 'none', cursor: 'pointer', textAlign: 'left',
    }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10,
        background: t.bgSubtle, color: empty ? t.warn : t.accent,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>
        <IcMapPin width="18" height="18"/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 14, fontWeight: 500, color: t.text,
        }}>
          {name}
        </div>
        <div style={{
          fontSize: 12, color: empty ? t.warn : t.textMuted, marginTop: 3,
          fontFamily: M.fontMono,
        }}>
          {empty ? 'Нормы не заданы' : `+${arr1}/+${arr2} приём · +${dep1}/+${dep2} сдача`}
        </div>
      </div>
      <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>
    </button>
  );
}

function FieldRow({ t, label, value, chevron, mono }) {
  return (
    <div style={{
      padding: '14px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16,
    }}>
      <span style={{ fontSize: 14, color: t.textMuted }}>{label}</span>
      <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
        <span style={{
          fontSize: 15, color: t.text, fontWeight: 500,
          fontFamily: mono ? M.fontMono : 'inherit',
        }}>{value}</span>
        {chevron && <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>}
      </div>
    </div>
  );
}

function DurationRow({ t, icon, label, minutes }) {
  return (
    <div style={{
      padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 12,
    }}>
      <div style={{
        width: 32, height: 32, borderRadius: 16,
        background: t.bgSubtle, color: t.text,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
      }}>
        {React.cloneElement(icon, { width: 16, height: 16 })}
      </div>
      <div style={{ flex: 1, fontSize: 14, color: t.text }}>{label}</div>
      <div style={{
        display: 'inline-flex', alignItems: 'center', gap: 4,
      }}>
        <button style={{
          width: 28, height: 28, borderRadius: 14, background: t.bgSubtle, color: t.text,
          border: 'none', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 16, fontWeight: 600,
        }}>−</button>
        <div style={{
          minWidth: 78, padding: '6px 10px', borderRadius: 10,
          background: t.bg, border: `1px solid ${t.border}`,
          textAlign: 'center', fontFamily: M.fontMono, fontSize: 15, fontWeight: 600, color: t.text,
        }}>{minutes} мин</div>
        <button style={{
          width: 28, height: 28, borderRadius: 14, background: t.bgSubtle, color: t.text,
          border: 'none', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 16, fontWeight: 600,
        }}>+</button>
      </div>
    </div>
  );
}

function AddBtn({ t, icon, label }) {
  return (
    <button style={{
      width: '100%', marginTop: 16, padding: '14px',
      borderRadius: 14, background: t.bgSubtle, color: t.accent,
      border: 'none', fontSize: 14, fontWeight: 500, cursor: 'pointer',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
    }}>
      {React.cloneElement(icon, { width: 16, height: 16 })}
      {label}
    </button>
  );
}

function DangerBtn({ t, icon, label }) {
  return (
    <button style={{
      width: '100%', marginTop: 32, padding: '14px',
      borderRadius: 14, background: 'transparent', color: t.danger,
      border: `1px solid ${t.border}`, fontSize: 14, fontWeight: 500, cursor: 'pointer',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
    }}>
      {React.cloneElement(icon, { width: 16, height: 16 })}
      {label}
    </button>
  );
}

Object.assign(window, {
  IOSSeriesList, IOSStationsList, IOSSeriesEditor, IOSStationEditor,
  AndroidSeriesList, AndroidStationsList, AndroidSeriesEditor, AndroidStationEditor,
});
