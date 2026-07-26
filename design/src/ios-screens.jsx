// iOS screens — Машинист redesign (light + dark)
// Screen 1: Маршрут (form/edit)
// Screen 2: Поездки (main/dashboard)

// ─────────────────────────────────────────────────────────────
// Custom iOS status bar (our theme colors)
// ─────────────────────────────────────────────────────────────
function MStatus({ dark, time = '15:13' }) {
  const c = dark ? '#F2EFE8' : '#14120E';
  return (
    <div style={{
      position: 'absolute', top: 0, left: 0, right: 0, height: 54, zIndex: 10,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '21px 24px 0',
    }}>
      <div style={{ fontFamily: M.fontSans, fontSize: 17, fontWeight: 600, color: c }}>{time}</div>
      <div style={{ width: 126, height: 37, borderRadius: 24, background: '#000', marginTop: -14 }}/>
      <div style={{ display: 'flex', gap: 6, color: c }}>
        <svg width="18" height="12" viewBox="0 0 18 12"><path d="M9 3.2C11.3 3.2 13.4 4.1 14.9 5.6L16 4.5C14.2 2.7 11.7 1.5 9 1.5S3.8 2.7 2 4.5L3.1 5.6C4.6 4.1 6.7 3.2 9 3.2z" fill="currentColor"/><path d="M9 6.8C10.4 6.8 11.6 7.3 12.5 8.2L13.6 7.1C12.3 5.9 10.7 5.1 9 5.1S5.7 5.9 4.4 7.1L5.5 8.2C6.4 7.3 7.6 6.8 9 6.8z" fill="currentColor"/><circle cx="9" cy="10.5" r="1.5" fill="currentColor"/></svg>
        <svg width="25" height="12" viewBox="0 0 25 12"><rect x="0.5" y="0.5" width="21" height="11" rx="3" fill="none" stroke="currentColor" opacity=".4"/><rect x="2" y="2" width="18" height="8" rx="1.5" fill="currentColor"/><path d="M23 4v4c.6-.2 1-.8 1-2s-.4-1.8-1-2z" fill="currentColor" opacity=".5"/></svg>
      </div>
    </div>
  );
}

// Android status bar — same absolute layout model (height 54) so screen
// content built for MStatus (paddingTop ~58) clears it without re-layout.
function MStatusAndroid({ dark, time = '15:13' }) {
  const c = dark ? '#F2EFE8' : '#14120E';
  return (
    <div style={{
      position: 'absolute', top: 0, left: 0, right: 0, height: 54, zIndex: 10,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '18px 22px 0',
    }}>
      <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: c, letterSpacing: 0.2 }}>{time}</div>
      <div style={{
        position: 'absolute', left: '50%', top: 16, transform: 'translateX(-50%)',
        width: 22, height: 22, borderRadius: 11, background: '#000',
      }}/>
      <div style={{ display: 'flex', alignItems: 'center', gap: 7, color: c }}>
        <svg width="16" height="16" viewBox="0 0 16 16"><path d="M8 13.3L.67 5.97a10.37 10.37 0 0114.66 0L8 13.3z" fill="currentColor"/></svg>
        <svg width="16" height="16" viewBox="0 0 16 16"><path d="M14.67 14.67V1.33L1.33 14.67h13.34z" fill="currentColor"/></svg>
        <svg width="16" height="16" viewBox="0 0 16 16"><rect x="3.75" y="2" width="8.5" height="13" rx="1.5" fill="currentColor"/><rect x="5.5" y="0.9" width="5" height="2" rx="0.5" fill="currentColor"/></svg>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// IOS Device (our custom frame — tuned to Машинист)
// ─────────────────────────────────────────────────────────────
function MDevice({ children, dark = false, width = 390, height = 844, platform = 'ios' }) {
  const t = dark ? M.dark : M.light;
  const android = platform === 'android';
  // height === 'auto' → рамка растёт под контент (для статичных артбордов,
  // где скролл не нужен и экран должен помещаться целиком).
  const auto = height === 'auto';
  return (
    <div style={{
      width, height: auto ? 'auto' : height, borderRadius: android ? 36 : 48, overflow: 'hidden', position: 'relative',
      background: t.bg,
      boxShadow: '0 40px 80px rgba(0,0,0,0.22), 0 0 0 1px rgba(0,0,0,0.15)',
      fontFamily: M.fontSans, WebkitFontSmoothing: 'antialiased',
    }}>
      {android ? <MStatusAndroid dark={dark}/> : <MStatus dark={dark}/>}
      {children}
      {/* nav indicator */}
      <div style={{
        position: 'absolute', bottom: 8, left: 0, right: 0,
        display: 'flex', justifyContent: 'center', pointerEvents: 'none', zIndex: 60,
      }}>
        <div style={{
          width: android ? 112 : 139, height: android ? 4 : 5, borderRadius: 100,
          background: dark ? 'rgba(242,239,232,0.55)' : 'rgba(20,18,14,0.3)',
        }}/>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Screen 1 — Маршрут
// ─────────────────────────────────────────────────────────────
function IOSScreenRoute({ dark = false, state = 'filled', sheet = null, confirm = null, height = 844, nightWarn = false }) {
  const t = dark ? M.dark : M.light;
  const empty = state === 'empty';
  return (
    <MDevice dark={dark} height={height}>
      {sheet === 'calc' && <IOSCalcSheet t={t}/>}
      {sheet === 'rest' && <IOSRestSheet t={t} mode="po"/>}
      {sheet === 'rest-home' && <IOSRestSheet t={t} mode="home"/>}
      {confirm === 'loco' && <RouteDeleteAlert t={t} title="Удалить локомотив?" message="ВЛ10-1456 будет убран из маршрута. Это действие нельзя отменить."/>}
      {/* Navigation */}
      <div style={{ position: 'relative', paddingTop: 58 }}>
        <div style={{
          padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <button style={pillBtn(t, { w: 40, h: 40 })}><IcChevronLeft width="18" height="18"/></button>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>
            {empty ? 'Новый маршрут' : 'Маршрут'}
          </div>
          <button style={pillBtn(t, { px: 14, h: 40, accent: true })}>Сохранить</button>
        </div>
      </div>

      <div style={{ padding: '8px 16px 140px', overflowY: 'auto', height: 'calc(100% - 110px)' }}>
        {/* Основные данные */}
        <GroupLabel t={t}>ОСНОВНЫЕ ДАННЫЕ</GroupLabel>
        <Card t={t}>
          <div style={{
            padding: '18px 20px', display: 'flex', alignItems: 'center', gap: 12,
            fontSize: 17, color: t.text,
          }}>
            <span style={{ color: t.textMuted, fontSize: 15 }}>№</span>
            {empty
              ? <span style={{ fontSize: 17, color: t.textFaint }}>Введите номер</span>
              : <span style={{ fontFamily: M.fontMono, fontWeight: 600, fontSize: 20 }}>112</span>}
          </div>
        </Card>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 10 }}>
          {/* Расчёт — открывает шторку с разбивкой заработка */}
          <Card t={t}>
            <button style={{
              width: '100%', border: 'none', background: 'transparent', textAlign: 'left',
              padding: 18, display: 'flex', flexDirection: 'column', gap: 8, cursor: 'pointer',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: t.textMuted }}>
                  <IcRuble width="16" height="16"/>
                  <span style={{ fontSize: 13 }}>Расчёт</span>
                </div>
                <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
              </div>
              <div style={{
                fontSize: empty ? 24 : 22, fontWeight: 800, fontFamily: M.fontMono,
                color: empty ? t.textFaint : t.text, letterSpacing: -.5,
              }}>
                {empty ? '0,00 ₽' : '5 027,50 ₽'}
              </div>
            </button>
          </Card>
          {/* Отдых — открывает шторку с расчётом отдыха */}
          <Card t={t}>
            <button style={{
              width: '100%', border: 'none', background: 'transparent', textAlign: 'left',
              padding: 18, display: 'flex', flexDirection: 'column', gap: 8, cursor: 'pointer',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: t.textMuted }}>
                  <IcHome width="16" height="16"/>
                  <span style={{ fontSize: 13 }}>Отдых</span>
                </div>
                <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
              </div>
              <div style={{ fontSize: 20, fontWeight: 700, color: empty ? t.textFaint : t.text }}>
                {empty ? 'Не задан' : 'Домашний'}
              </div>
            </button>
          </Card>
        </div>

        {/* Время работы */}
        <div style={{
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
          padding: '22px 4px 8px',
        }}>
          <div style={{
            fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.4, whiteSpace: 'nowrap',
          }}>ВРЕМЯ РАБОТЫ</div>
          {empty ? (
            <div style={{ fontFamily: M.fontMono, fontSize: 20, fontWeight: 700, color: t.textFaint }}>—:—</div>
          ) : (
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
              <div style={{ fontFamily: M.fontMono, fontSize: 20, fontWeight: 700, color: t.text, letterSpacing: -.3 }}>08:00</div>
            </div>
          )}
        </div>
        <Card t={t}>
          <TimeRow t={t} label="Явка" date={empty ? 'Выбрать' : '02 июн 2026'} time={empty ? '—:—' : '19:09'} muted={empty} tag={empty ? null : 'по прибытию пассажиром'}/>
          <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>
          <TimeRow t={t} label="Сдача" date={empty ? 'Выбрать' : '03 июн 2026'} time={empty ? '—:—' : '03:09'} muted={empty}/>
        </Card>
        {!empty && (
          <div style={{
            display: 'flex', gap: 8, padding: '9px 6px 0', color: t.textMuted,
          }}>
            <IcPassenger width="15" height="15" style={{ flexShrink: 0, marginTop: 1, color: t.accent }}/>
            <span style={{ fontFamily: M.fontSans, fontSize: 12, lineHeight: 1.4 }}>
              Отсчёт ведётся с прибытия пассажиром (№ 8023).
            </span>
          </div>
        )}

        {/* Предупреждение о второй ночи подряд */}
        {nightWarn && !empty && (
          <div style={{ marginTop: 10 }}><RouteNightWarn t={t}/></div>
        )}

        {/* Локомотивы */}
        <GroupLabel t={t} style={{ marginTop: 22 }}>ЛОКОМОТИВЫ</GroupLabel>
        <Card t={t}>
          {!empty && <>
            <UnitRow t={t} icon={<IcLocomotive width="20" height="20"/>} title="ВЛ10-1456"/>
            <div style={{ height: 1, background: t.border, marginLeft: 56 }}/>
          </>}
          <AddRow t={t} icon={<IcLocomotive width="20" height="20"/>} label="Добавить локомотив"/>
        </Card>

        {/* Поезда */}
        <GroupLabel t={t} style={{ marginTop: 22 }}>ПОЕЗДА</GroupLabel>
        <Card t={t}>
          {!empty && <>
            <UnitRow t={t} icon={<IcRails width="20" height="20"/>} title="№2712" meta="Лужская — СПбСМ"/>
            <div style={{ height: 1, background: t.border, marginLeft: 56 }}/>
          </>}
          <AddRow t={t} icon={<IcRails width="20" height="20"/>} label="Добавить поезд"/>
        </Card>

        {/* Пассажиром */}
        <GroupLabel t={t} style={{ marginTop: 22 }}>ПАССАЖИРОМ</GroupLabel>
        <Card t={t}>
          {!empty && <>
            <UnitRow t={t} icon={<IcPassenger width="20" height="20"/>} title="№8023" meta="Новосибирск — Омск" right="04:20" badge="Начало работы"/>
            <div style={{ height: 1, background: t.border, marginLeft: 56 }}/>
          </>}
          <AddRow t={t} icon={<IcPassenger width="20" height="20"/>} label="Добавить пассажиром"/>
        </Card>

        {/* Заметки */}
        <GroupLabel t={t} style={{ marginTop: 22 }}>ЗАМЕТКИ</GroupLabel>
        <Card t={t}>
          <div style={{ padding: 20, minHeight: 64, fontSize: 15, color: empty ? t.textFaint : t.text, lineHeight: 1.45 }}>
            {empty ? 'Опиши смену, если нужно…' : 'Подмена на ст. Лужская. Опоздание поезда 12 мин.'}
          </div>
        </Card>
      </div>

      {/* Gradient scrim — faded content under toolbar */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0, height: 140, zIndex: 40,
        pointerEvents: 'none',
        background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)} 0%, ${hexToRgba(t.bg, 0.85)} 45%, ${t.bg} 100%)`,
      }}/>

      {/* Floating toolbar — sliders · [heart, share, copy] · trash */}
      <div style={{
        position: 'absolute', bottom: 30, left: 16, right: 16, zIndex: 50,
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        {/* Primary — sliders (ink pill) */}
        <button style={{
          width: 52, height: 52, borderRadius: 26, background: t.cta || t.accent, color: t.ctaInk || t.accentInk,
          border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 12px 32px rgba(0,0,0,0.28)',
        }}><IcSliders width="22" height="22"/></button>

        {/* Center — reversible actions */}
        <div style={{
          flex: 1, height: 52, borderRadius: 26, background: t.surface,
          boxShadow: '0 12px 32px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.06)',
          border: `1px solid ${t.border}`,
          display: 'flex', alignItems: 'center', justifyContent: 'space-around',
          color: t.text,
        }}>
          <IcHeart width="22" height="22"/>
          <IcShare width="22" height="22"/>
          <IcCopy width="22" height="22"/>
        </div>

        {/* Destructive — trash (isolated circle) */}
        <button style={{
          width: 52, height: 52, borderRadius: 26, background: t.surface, color: t.danger,
          border: `1px solid ${t.border}`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 12px 32px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.06)',
        }}><IcTrash width="22" height="22"/></button>
      </div>
    </MDevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Screen 2 — Поездки (main)
// ─────────────────────────────────────────────────────────────
function IOSScreenTrips({ dark = false, height = 980, sheet = null, empty = false, state = 'current', heroPage = 0, holiday = false }) {
  const t = dark ? M.dark : M.light;
  const upcoming = state === 'upcoming';
  const rest = state === 'rest';
  return (
    <MDevice dark={dark} height={height}>
      {sheet === 'legend' && <IOSLegendSheet t={t}/>}
      {sheet === 'units' && <IOSUnitsSheet t={t}/>}
      {/* Month switcher */}
      <div style={{ paddingTop: 58 }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-around',
          padding: '12px 16px', color: t.textFaint, fontSize: 17,
        }}>
          <span>Март</span>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>Апрель</div>
            <div style={{ fontSize: 11, color: t.textMuted, fontFamily: M.fontMono, letterSpacing: 1 }}>2026</div>
          </div>
          <span>Май</span>
        </div>
      </div>

      <div style={{ padding: '8px 16px 120px', overflowY: 'auto', height: 'calc(100% - 182px)' }}>
        {/* Hours card — hero (свайп-карусель: сводка / вид работы / признаки) */}
        <HeroCard t={t} page={heroPage} state={state} holiday={holiday}/>

        {/* Rest at turnaround — машинист отдыхает в пункте оборота */}
        {rest ? (<>
          <SecH t={t}>Отдых в ПО</SecH>
          <RestAtTurnaround t={t}/>
        </>) :
        /* Upcoming route — текущий маршрут закончился, впереди сохранённый */
        upcoming ? (<>
          <SecH t={t}>Следующий маршрут</SecH>
          <UpcomingRoute t={t}
            remaining="18:45"
            date="25 апреля, Сб"
            time="20:00"
          />
        </>) : (<>
        {/* Current route */}
        <SecH t={t}>Текущий маршрут</SecH>
        <div style={{ display: 'flex', gap: 12, overflowX: 'auto', padding: '9px 0 8px', scrollSnapType: 'x mandatory' }}>
          <CurrentTile t={t} active>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.2 }}>НА РАБОТЕ</div>
            <div style={{ fontSize: 28, fontWeight: 800, fontFamily: M.fontMono, color: t.text, letterSpacing: -1 }}>00:08</div>
            <div style={{
              height: 3, background: t.accentSoft, borderRadius: 2, marginTop: 8, overflow: 'hidden',
            }}>
              <div style={{ width: '8%', height: '100%', background: t.accent }}/>
            </div>
          </CurrentTile>
          {empty ? (<>
            <AddTile t={t} icon={<IcLocomotive width="30" height="30"/>} kind="Локомотив"/>
            <AddTile t={t} icon={<IcRails width="30" height="30"/>} kind="Поезд"/>
            <AddTile t={t} icon={<IcPassenger width="30" height="30"/>} kind="Пассажиром"/>
          </>) : (<>
            <FilledTile t={t} icon={<IcLocomotive width="30" height="30"/>} kind="Локомотив"
              title="ВЛ10-1456" count={2}/>
            <FilledTile t={t} icon={<IcRails width="30" height="30"/>} kind="Поезд"
              title="№2712" meta="Лужская — СПбСМ"/>
            <FilledTile t={t} icon={<IcPassenger width="30" height="30"/>} kind="Пассажиром"
              title="№8023" meta="Новосибирск — Омск"/>
          </>)}
        </div>
        </>)}

        {/* Routes list */}
        <SecH t={t} trailing={
          <span style={{ fontSize: 13, color: t.accent, display: 'inline-flex', alignItems: 'center', gap: 2 }}>Все (23) <IcChevronRight width="10" height="10"/></span>
        }>
          Маршруты
        </SecH>
        <Card t={t}>
          <TripRow t={t} date="28.05 17:40 — 21:08" num="#14" route="№4404 Луга — СПбСМ" hours="03:27" attrs={['passenger', 'over12']} synced={false}/>
          <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>
          <TripRow t={t} date="22.05 08:46 — 19:27" num="#13" route="№2712 Лужская — СПбСМ" hours="10:41" attrs={['length']}/>
        </Card>

        {/* Tools */}
        <SecH t={t}>Инструменты</SecH>
        <div style={{
          display: 'flex', gap: 10, overflowX: 'auto', padding: '0 0 4px',
          scrollSnapType: 'x mandatory',
        }}>
          <ToolTile t={t} icon={<IcCalendar/>} label="График"/>
          <ToolTile t={t} icon={<IcArmchair/>} label="Отвлечения"/>
          <ToolTile t={t} icon={<IcSearch/>} label="Поиск"/>
          <ToolTile t={t} icon={<IcPdf/>} label="PDF"/>
        </div>
      </div>

      {/* Gradient scrim — faded content under tab bar */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0, height: 140, zIndex: 40,
        pointerEvents: 'none',
        background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)} 0%, ${hexToRgba(t.bg, 0.85)} 45%, ${t.bg} 100%)`,
      }}/>

      {/* Tab bar */}
      <div style={{
        position: 'absolute', bottom: 22, left: 10, right: 10, zIndex: 50,
        background: t.surface, borderRadius: 30,
        boxShadow: '0 12px 32px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.06)',
        border: `1px solid ${t.border}`,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 14px',
      }}>
        <TabBtn t={t} active icon={<IcDocument/>} label="Главный"/>
        <TabBtn t={t} icon={<IcRuble/>} label="Зарплата"/>
        <button style={{
          width: 46, height: 46, borderRadius: 23, background: t.cta || t.accent, color: t.ctaInk || t.accentInk,
          border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 18px rgba(0,0,0,0.25)',
        }}><IcPlus width="22" height="22"/></button>
        <TabBtn t={t} icon={<IcSliders/>} label="Настройки"/>
        <TabBtn t={t} icon={<IcProfile/>} label="Профиль"/>
      </div>
    </MDevice>
  );
}

// Helpers ──────────────────────────────────────────────────────
function hexToRgba(hex, a = 1) {
  // accept #RGB, #RRGGBB, or rgba() passthrough
  if (!hex) return `rgba(0,0,0,${a})`;
  if (hex.startsWith('rgb')) return hex;
  let h = hex.replace('#', '');
  if (h.length === 3) h = h.split('').map(c => c + c).join('');
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  return `rgba(${r},${g},${b},${a})`;
}

function pillBtn(t, { w, h, px, accent }) {
  return {
    height: h, width: w, padding: px ? `0 ${px}px` : 0, borderRadius: 999,
    background: t.surface, border: 'none', boxShadow: M.shadow.sm,
    color: accent ? t.accent : t.text, fontSize: 15, fontWeight: 600,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontFamily: M.fontSans,
  };
}

function SecH({ t, children, trailing }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '22px 4px 12px',
    }}>
      <div style={{ fontSize: 20, fontWeight: 700, color: t.text, letterSpacing: -.3 }}>{children}</div>
      {trailing}
    </div>
  );
}

function GroupLabel({ t, children, style }) {
  return (
    <div style={{
      fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.4,
      padding: '10px 4px 8px', ...style,
    }}>{children}</div>
  );
}

function Card({ t, children }) {
  return (
    <div style={{
      background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm,
      overflow: 'hidden',
    }}>{children}</div>
  );
}

function TimeRow({ t, label, date, time, muted = false, tag }) {
  return (
    <div style={{ padding: '14px 20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
        <span style={{ fontSize: 13, color: t.textMuted }}>{label}</span>
        {tag && <span style={{
          fontFamily: M.fontMono, fontSize: 9.5, fontWeight: 700, letterSpacing: 0.8,
          color: t.accent, background: t.accentSoft,
          padding: '3px 7px', borderRadius: 6, textTransform: 'uppercase',
        }}>{tag}</span>}
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <div style={{
          background: t.bgSubtle, padding: '8px 14px', borderRadius: 10,
          fontSize: 15, fontFamily: M.fontMono, color: muted ? t.textFaint : t.text, fontWeight: 500, whiteSpace: 'nowrap',
        }}>{date}</div>
        <div style={{
          background: t.bgSubtle, padding: '8px 14px', borderRadius: 10,
          fontSize: 15, fontFamily: M.fontMono, color: muted ? t.textFaint : t.text, fontWeight: 600,
        }}>{time}</div>
      </div>
    </div>
  );
}

// Строка добавленной единицы (локомотив / поезд / пассажиром) в маршруте.
function UnitRow({ t, icon, title, meta, right, badge }) {
  return (
    <div style={{ padding: '12px 12px 12px 20px', display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10, background: t.bgSubtle, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
      }}>{icon}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{title}</div>
        {meta && <div style={{ fontSize: 12, color: t.textMuted }}>{meta}</div>}
        {badge && <span style={{
          display: 'inline-flex', alignItems: 'center', gap: 4, marginTop: 5,
          fontFamily: M.fontMono, fontSize: 9.5, fontWeight: 700, letterSpacing: 0.8,
          color: t.accent, background: t.accentSoft,
          padding: '3px 7px', borderRadius: 6, textTransform: 'uppercase',
        }}>{badge}</span>}
      </div>
      {right && <div style={{ fontFamily: M.fontMono, fontSize: 14, color: t.text, whiteSpace: 'nowrap' }}>{right}</div>}
      {/* Удалить единицу — подтверждение вызывается по тапу */}
      <button style={{
        width: 34, height: 34, borderRadius: 17, flexShrink: 0, cursor: 'pointer',
        background: hexToRgba(t.danger, 0.10), color: t.danger, border: 'none',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }} aria-label="Удалить"><IcTrash width="17" height="17"/></button>
    </div>
  );
}

// iOS alert «Удалить?» — подтверждение удаления единицы маршрута.
function RouteDeleteAlert({ t, title, message }) {
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 80, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 28 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'relative', width: 270, background: t.elevated || t.surface,
        borderRadius: 18, overflow: 'hidden', boxShadow: '0 24px 64px rgba(0,0,0,0.34)',
        backdropFilter: 'blur(20px)',
      }}>
        <div style={{ padding: '20px 18px 16px', textAlign: 'center' }}>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{title}</div>
          <div style={{ fontSize: 13, color: t.textMuted, marginTop: 4, lineHeight: 1.35 }}>{message}</div>
        </div>
        <div style={{ height: 1, background: t.border }}/>
        <div style={{ display: 'flex' }}>
          <button style={{
            flex: 1, padding: '13px 0', border: 'none', background: 'transparent', cursor: 'pointer',
            fontSize: 16, color: t.accent, fontWeight: 500,
          }}>Отмена</button>
          <div style={{ width: 1, background: t.border }}/>
          <button style={{
            flex: 1, padding: '13px 0', border: 'none', background: 'transparent', cursor: 'pointer',
            fontSize: 16, color: t.danger, fontWeight: 700,
          }}>Удалить</button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Bottom-sheet shell для экрана Маршрут (Расчёт / Отдых)
// ─────────────────────────────────────────────────────────────
function RouteSheetShell({ t, title, subtitle, children }) {
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '88%',
        background: t.bg, borderRadius: '24px 24px 0 0',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.28)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 10 }}>
          <div style={{ width: 40, height: 5, borderRadius: 3, background: t.borderStrong }}/>
        </div>
        <div style={{ padding: '12px 20px 8px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontSize: 20, fontWeight: 700, color: t.text, letterSpacing: -.3 }}>{title}</div>
            {subtitle && <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>{subtitle}</div>}
          </div>
          <button style={{
            width: 30, height: 30, borderRadius: 15, border: 'none', cursor: 'pointer',
            background: t.bgSubtle, color: t.textMuted, fontSize: 15, lineHeight: 1,
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>✕</button>
        </div>
        <div style={{ overflowY: 'auto', padding: '8px 16px 28px' }}>{children}</div>
      </div>
    </div>
  );
}

// Шторка «Расчёт» — разбивка заработка за смену (пример данных пользователя).
function IOSCalcSheet({ t }) {
  const rows = [
    ['Почасовая оплата', '8:00 × 375,00 ₽/ч', '3 000,00 ₽'],
    ['Зональная надбавка', '25 % от тарифа', '750,00 ₽'],
    ['Ночные', '07:51 в ночное время', '1 177,50 ₽'],
    ['Переотдых', 'компенсация за переотдых', '100,00 ₽'],
  ];
  return (
    <RouteSheetShell t={t} title="Расчёт за смену" subtitle="Маршрут №112 · 08:00">
      {/* Итоговая сумма */}
      <div style={{
        background: t.accentSoft, borderRadius: 18, padding: '18px 20px', marginBottom: 14,
        display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
      }}>
        <div style={{ fontSize: 13, color: t.accent, fontWeight: 600 }}>Заработано</div>
        <div style={{ fontSize: 30, fontWeight: 800, fontFamily: M.fontMono, color: t.accent, letterSpacing: -1, lineHeight: 1, whiteSpace: 'nowrap' }}>5 027,50 ₽</div>
      </div>
      {/* Разбивка */}
      <Card t={t}>
        {rows.map(([label, sub, amount], i) => (
          <React.Fragment key={label}>
            {i > 0 && <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>}
            <div style={{ padding: '14px 20px', display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{label}</div>
                <div style={{ fontSize: 12, color: t.textMuted, marginTop: 1 }}>{sub}</div>
              </div>
              <div style={{ fontFamily: M.fontMono, fontSize: 16, fontWeight: 600, color: t.text, whiteSpace: 'nowrap' }}>{amount}</div>
            </div>
          </React.Fragment>
        ))}
      </Card>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '14px 6px 6px', color: t.textMuted }}>
        <IcInfo width="16" height="16"/>
        <span style={{ fontSize: 13, lineHeight: 1.4 }}>Суммы рассчитаны по вашим тарифам и нормам.</span>
      </div>
      {/* Переход в настройки зарплаты прямо из шторки */}
      <button style={{
        marginTop: 6, width: '100%', padding: '14px 16px', borderRadius: 14, cursor: 'pointer',
        background: t.surface, border: `1px solid ${t.border}`, boxShadow: M.shadow.sm,
        display: 'flex', alignItems: 'center', gap: 12, color: t.text,
      }}>
        <IcGear width="20" height="20" style={{ color: t.textMuted }}/>
        <span style={{ flex: 1, textAlign: 'left', fontSize: 15, fontWeight: 500 }}>Настройки зарплаты</span>
        <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
      </button>
    </RouteSheetShell>
  );
}

// Шторка «Отдых» — расчёт короткого и полного отдыха от времени сдачи.
// mode: 'po' — отдых в пункте оборота (короткий/полный от мин. 4 ч);
//       'home' — домашний отдых (минимальный 16 ч + полный = время работы × 2,6).
function IOSRestSheet({ t, mode = 'po' }) {
  const home = mode === 'home';
  const Seg = ({ label, active }) => (
    <div style={{
      flex: 1, textAlign: 'center', padding: '9px 0', borderRadius: 9, cursor: 'pointer',
      fontSize: 14, fontWeight: 600,
      background: active ? t.surface : 'transparent',
      color: active ? t.text : t.textMuted,
      boxShadow: active ? M.shadow.sm : 'none',
    }}>{label}</div>
  );
  const RestItem = ({ title, dur, until, accent }) => (
    <div style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', gap: 14 }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, color: t.textMuted, marginBottom: 3 }}>
          {title} <span style={{ fontFamily: M.fontMono }}>· {dur}</span>
        </div>
        <div style={{
          fontFamily: M.fontMono, fontSize: 22, fontWeight: 700, letterSpacing: -.3,
          color: t.text, whiteSpace: 'nowrap',
        }}>до {until}</div>
      </div>
    </div>
  );
  return (
    <RouteSheetShell t={t} title="Отдых" subtitle="От времени сдачи · 03.06.26 03:09">
      {/* Тип отдыха */}
      <div style={{ display: 'flex', gap: 4, padding: 4, background: t.bgSubtle, borderRadius: 12, marginBottom: 14 }}>
        <Seg label="Домашний" active={home}/>
        <Seg label="В пункте оборота" active={!home}/>
      </div>
      {home ? (
        <>
          {/* Домашний отдых: минимальный 16 ч и полный = время работы × 2,6 */}
          <Card t={t}>
            <RestItem title="Минимальный отдых" dur="16 ч 0 м" until="03.06 · 19:09"/>
            <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>
            <RestItem title="Полный отдых" dur="20 ч 48 м" until="03.06 · 23:57" accent/>
          </Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 6px 0', color: t.textMuted }}>
            <IcInfo width="16" height="16"/>
            <span style={{ fontSize: 13, lineHeight: 1.4 }}>Полный = время работы 08:00 × 2,6. Минимум — 16 ч.</span>
          </div>
        </>
      ) : (
        <Card t={t}>
          <RestItem title="Короткий отдых" dur="4 ч 0 м" until="03.06 · 07:09"/>
          <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>
          <RestItem title="Полный отдых" dur="8 ч 0 м" until="03.06 · 11:09" accent/>
        </Card>
      )}
      {/* Настройка коэффициентов */}
      <button style={{
        marginTop: 12, width: '100%', padding: '14px 16px', borderRadius: 14, cursor: 'pointer',
        background: t.surface, border: `1px solid ${t.border}`, boxShadow: M.shadow.sm,
        display: 'flex', alignItems: 'center', gap: 12, color: t.text,
      }}>
        <IcGear width="20" height="20" style={{ color: t.textMuted }}/>
        <span style={{ flex: 1, textAlign: 'left', fontSize: 15, fontWeight: 500 }}>Настройки времени отдыха</span>
        <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
      </button>
    </RouteSheetShell>
  );
}

function AddRow({ t, icon, label }) {
  return (
    <div style={{
      padding: '16px 20px', display: 'flex', alignItems: 'center', gap: 12,
      color: t.accent, fontSize: 16, fontWeight: 500,
    }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10, background: t.accentSoft,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{icon}</div>
      {label}
    </div>
  );
}

function PassRow({ t, train, route, time }) {
  return (
    <div style={{ padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10, background: t.bgSubtle, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}><IcPassenger width="20" height="20"/></div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{train}</div>
        <div style={{ fontSize: 12, color: t.textMuted }}>{route}</div>
      </div>
      <div style={{ fontFamily: M.fontMono, fontSize: 14, color: t.text }}>{time}</div>
      <IcChevronRight width="14" height="14" color={t.textFaint}/>
    </div>
  );
}

function HourBar({ t, label, value, max, color, highlight, fillPct }) {
  const pct = fillPct != null ? fillPct : Math.min(100, (value / max) * 100);
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
        <span style={{ color: t.textMuted }}>{label}</span>
        <span style={{ fontFamily: M.fontMono, color: highlight ? color : t.text, fontWeight: 600 }}>
          {value} ч
        </span>
      </div>
      <div style={{ height: 4, background: t.bgSubtle, borderRadius: 4, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: color }}/>
      </div>
    </div>
  );
}

// Строка категории на страницах разбивки часов: подпись слева, mono-значение
// справа, тонкая пропорциональная полоса под ними (как в эталонном приложении).
function HeroStatRow({ t, label, value, pct = 0, accent }) {
  const filled = pct > 0;
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', fontSize: 14 }}>
        <span style={{ color: t.textMuted }}>{label}</span>
        <span style={{
          fontFamily: M.fontMono, fontWeight: 600,
          color: filled ? (accent || t.text) : t.textFaint,
        }}>{value}</span>
      </div>
      <div style={{ height: 4, background: t.bgSubtle, borderRadius: 4, overflow: 'hidden', marginTop: 7 }}>
        <div style={{ width: `${Math.min(100, pct)}%`, height: '100%', background: accent || t.accent }}/>
      </div>
    </div>
  );
}

// Hero-карточка верхнего раздела — свайп-карусель из трёх страниц.
// page 0 — сводка отработанных часов (норма/факт);
// page 1 — разбивка по виду работы (ночные / пассажиром / резервом);
// page 2 — доплатные признаки (удл. плечи / длинносоставные / тяжёлые / одно лицо).
function HeroCard({ t, page = 0, state = 'current', holiday = false }) {
  return (
    <div>
      {/* Постоянная шапка — итог месяца, вне свайп-зоны */}
      <div style={{ padding: '4px 6px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4, marginBottom: 5 }}>ОТРАБОТАНО</div>
          <div style={{ fontSize: 52, fontWeight: 800, color: t.text, letterSpacing: -2, fontFamily: M.fontMono, lineHeight: 1 }}>205:05</div>
          {holiday && (
            <div style={{ fontSize: 12, color: t.textMuted, marginTop: 6, letterSpacing: -.1 }}>
              праздничные <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>16:00</span>
              <span style={{ color: t.textFaint, margin: '0 6px' }}>·</span>
              обычные <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>189:05</span>
            </div>
          )}
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4, marginBottom: 5 }}>СВЕРХ</div>
          <div style={{ fontSize: 20, fontWeight: 700, color: t.accent, fontFamily: M.fontMono }}>+30:05</div>
        </div>
      </div>

      {/* Свайп-карточка с разбивками */}
      <div style={{ background: t.surface, borderRadius: 24, padding: 22, boxShadow: M.shadow.md, marginTop: 16 }}>
        {page === 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <HourBar t={t} label="Норма на месяц" value={175} max={200} color={t.accent}/>
            <HourBar t={t} label="Норма на 23.04" value={136} max={200} color={t.accent}/>
            <HourBar t={t} label="Отработано на 23.04" value={181} max={200}
              color={state === 'upcoming' ? t.warning : t.success}
              fillPct={state === 'rest' ? 100 : undefined} highlight/>
          </div>
        )}

        {page === 1 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <HeroStatRow t={t} label="Ночные" value="13:51" pct={64} accent={t.accent}/>
            <HeroStatRow t={t} label="Пассажиром" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Резервом" value="00:00" pct={0}/>
          </div>
        )}

        {page === 2 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 15 }}>
            <HeroStatRow t={t} label="Удл. плечи обслуживания" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Длинносоставные" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Тяжелые" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Одно лицо" value="00:00" pct={0}/>
          </div>
        )}
      </div>

      {/* Точечный индикатор под свайп-карточкой */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 12 }}>
        {[0, 1, 2].map(i => (
          <div key={i} style={{
            width: i === page ? 18 : 6, height: 6, borderRadius: 3,
            background: i === page ? t.textMuted : t.borderStrong,
            transition: 'width .2s',
          }}/>
        ))}
      </div>
    </div>
  );
}

// Заполненная карточка текущего маршрута — крупная иконка + данные.
// Бейдж-счётчик «N» для угла карточки нескольких единиц.
function CountBadge({ t, n }) {
  return (
    <div style={{
      minWidth: 19, height: 19, borderRadius: 10, padding: '0 5px',
      background: t.accent, color: t.accentInk,
      fontSize: 11, fontWeight: 700, fontFamily: M.fontMono,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>{n}</div>
  );
}

// Заполненная карточка текущего маршрута — крупная иконка + данные.
// count > 1 — несколько единиц: карточка позади + бейдж «N Все» вверху справа,
// тап открывает шторку со списком. Кнопка «+» внизу справа добавляет ещё одну.
function FilledTile({ t, icon, kind, title, meta, count = 1 }) {
  const many = count > 1;
  return (
    <div style={{
      width: 150, height: 150, flexShrink: 0, scrollSnapAlign: 'start', position: 'relative',
    }}>
      {many && (
        <div style={{
          position: 'absolute', left: 7, right: -7, top: -7, height: 150,
          background: t.surface, borderRadius: 20, boxShadow: M.shadow.sm,
        }}/>
      )}
      <button style={{
        position: 'absolute', inset: 0, padding: 18,
        background: t.surface, borderRadius: 20, boxShadow: M.shadow.sm,
        border: many ? `1px solid ${t.border}` : 'none',
        cursor: 'pointer', textAlign: 'left',
        display: 'flex', flexDirection: 'column',
      }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 6, minHeight: 30 }}>
          <div style={{ color: t.accent }}>{icon}</div>
          {many && <CountBadge t={t} n={count}/>}
        </div>
        <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.2, textTransform: 'uppercase', marginBottom: 3 }}>{kind}</div>
        <div style={{ fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -0.4, whiteSpace: 'nowrap' }}>{title}</div>
        {meta && <div style={{ fontSize: 13, color: t.textMuted, lineHeight: 1.3, marginTop: 3, paddingRight: 36 }}>{meta}</div>}
      </button>
      <span style={{
        position: 'absolute', right: 14, bottom: 14, zIndex: 2,
        width: 32, height: 32, borderRadius: 16, flexShrink: 0,
        background: t.accentSoft, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }} aria-label={'Добавить ещё: ' + kind}><IcPlus width="18" height="18"/></span>
    </div>
  );
}

// Bottom-sheet со списком единиц одного типа — открывается тапом на карточку
// «Локомотив» с бейджем «N Все».
function IOSUnitsSheet({ t }) {
  const Row = ({ title }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 18px' }}>
      <div style={{
        width: 44, height: 44, borderRadius: 12, flexShrink: 0,
        background: t.bgSubtle, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}><IcLocomotive width="24" height="24"/></div>
      <div style={{ flex: 1, fontSize: 17, fontWeight: 700, color: t.text }}>{title}</div>
      <IcChevronRight width="16" height="16" style={{ color: t.textFaint }}/>
    </div>
  );
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '82%',
        background: t.bg, borderRadius: '24px 24px 0 0',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.28)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 10 }}>
          <div style={{ width: 40, height: 5, borderRadius: 3, background: t.borderStrong }}/>
        </div>
        <div style={{ padding: '14px 20px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ fontSize: 20, fontWeight: 700, color: t.text }}>Локомотивы · 2</div>
          <button style={{
            width: 30, height: 30, borderRadius: 15, border: 'none', background: t.bgSubtle,
            color: t.textMuted, fontSize: 14, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>✕</button>
        </div>
        <div style={{ padding: '0 16px 20px' }}>
          <Card t={t}>
            <Row title="ВЛ10-1456"/>
            <div style={{ height: 1, background: t.border, marginLeft: 74 }}/>
            <Row title="ВЛ10-1457"/>
          </Card>
        </div>
        <div style={{ padding: '0 16px 28px' }}>
          <button style={{
            width: '100%', padding: '14px', borderRadius: 16, border: `1.5px dashed ${t.borderStrong}`,
            background: 'transparent', color: t.accent, fontSize: 15, fontWeight: 600, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}><IcPlus width="20" height="20"/> Добавить локомотив</button>
        </div>
      </div>
    </div>
  );
}

// Пустая карточка текущего маршрута — когда единица ещё не добавлена.
// Раскладка ПОЛНОСТЬЮ повторяет FilledTile: иконка сверху-слева, метка типа
// на том же месте, кнопка «+» в правом нижнем углу. Меняется только заголовок
// (на приглушённое «Добавить») и пунктирная рамка — ничего не «прыгает».
function AddTile({ t, icon, kind }) {
  return (
    <div style={{
      width: 150, height: 150, flexShrink: 0, scrollSnapAlign: 'start', position: 'relative',
    }}>
      <button style={{
        position: 'absolute', inset: 0, padding: 18,
        background: 'transparent', borderRadius: 20, border: `1.5px dashed ${t.borderStrong}`,
        cursor: 'pointer', textAlign: 'left',
        display: 'flex', flexDirection: 'column',
      }} aria-label={'Добавить: ' + kind}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 6, minHeight: 30 }}>
          <div style={{ color: t.textFaint }}>{icon}</div>
        </div>
        <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.2, textTransform: 'uppercase', marginBottom: 3 }}>{kind}</div>
      </button>
      <span style={{
        position: 'absolute', right: 14, bottom: 14, zIndex: 2,
        width: 32, height: 32, borderRadius: 16, flexShrink: 0,
        background: t.accentSoft, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }} aria-label={'Добавить: ' + kind}><IcPlus width="18" height="18"/></span>
    </div>
  );
}

// Состояние «Следующий маршрут» — текущий маршрут закончился, впереди сохранён
// будущий. Перекликается со статус-плиткой «НА РАБОТЕ»: тот же mono-лейбл,
// крупная mono-цифра и тонкая полоса прогресса. Главное — обратный отсчёт
// ЧЧ:ММ до явки и дата/время явки. Маршрут следования здесь не показываем.
function UpcomingRoute({ t, remaining, date, time }) {
  return (
    <div style={{
      background: t.surface, borderRadius: 24, boxShadow: M.shadow.md,
      padding: 22, marginTop: 9,
    }}>
      {/* Обратный отсчёт до явки — как плитка «НА РАБОТЕ» */}
      <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4, marginBottom: 4 }}>
        ДО ЯВКИ ОСТАЛОСЬ
      </div>
      <div style={{
        fontSize: 28, fontWeight: 800, fontFamily: M.fontMono, color: t.text,
        letterSpacing: -1, lineHeight: 1,
      }}>{remaining}</div>
      {/* Тонкая полоса прогресса — как у активной плитки */}
      <div style={{ height: 3, background: t.accentSoft, borderRadius: 2, marginTop: 14, overflow: 'hidden' }}>
        <div style={{ width: '64%', height: '100%', background: t.accent }}/>
      </div>

      <div style={{ height: 1, background: t.border, margin: '20px 0 0' }}/>

      {/* Дата/время явки */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, paddingTop: 18 }}>
        <div style={{
          width: 38, height: 38, borderRadius: 11, flexShrink: 0,
          background: t.bgSubtle, color: t.accent,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}><IcCalendar width="20" height="20"/></div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 12, color: t.textMuted, marginBottom: 2 }}>Явка</div>
          <div style={{ fontSize: 16, fontWeight: 700, color: t.text, letterSpacing: -.2 }}>
            {date}<span style={{ fontWeight: 500, color: t.textMuted }}> · </span>
            <span style={{ fontFamily: M.fontMono }}>{time}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

// Состояние «Отдых в ПО» — машинист прибыл в пункт оборота и отдыхает.
// Вместо отсчёта до явки показываем, сколько отдых уже длится (крупная mono-
// цифра тех же кеглей, что и плитка «НА РАБОТЕ»), и две контрольные точки:
// когда заканчивается короткий отдых и когда — полный. Полоса прогресса с
// засечкой короткого отдыха показывает, где сейчас находится отдых.
function RestAtTurnaround({ t,
  elapsed = '02:14',     // сколько уже длится отдых
  started = '12:16',     // во сколько начался
  shortEnd = '15:16', shortLeft = '00:46',   // конец короткого отдыха
  fullEnd = '17:46',  fullLeft = '03:16',    // конец полного отдыха
  pct = 41,              // позиция «сейчас» на шкале (0…100)
  shortPct = 55,         // засечка короткого отдыха
}) {
  const reached = pct >= shortPct;  // короткий отдых уже выдержан?
  return (
    <div style={{
      background: t.surface, borderRadius: 24, boxShadow: M.shadow.md,
      padding: 22, marginTop: 9,
    }}>
      <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4 }}>
        ОТДЫХАЕТЕ
      </div>

      {/* Сколько отдых уже длится — mono-цифра как плитка «НА РАБОТЕ» */}
      <div style={{
        fontSize: 28, fontWeight: 800, fontFamily: M.fontMono, color: t.text,
        letterSpacing: -1, lineHeight: 1, marginTop: 7,
      }}>{elapsed}</div>

      {/* Полоса прогресса с точкой короткого отдыха */}
      <div style={{ position: 'relative', marginTop: 22, height: 3 }}>
        <div style={{ position: 'absolute', inset: 0, background: t.accentSoft, borderRadius: 2, overflow: 'hidden' }}>
          <div style={{
            width: `${pct}%`, height: '100%',
            background: reached ? t.success : t.accent,
          }}/>
        </div>
        {/* Точка короткого отдыха — зелёная */}
        <div style={{
          position: 'absolute', top: '50%', left: `${shortPct}%`, transform: 'translate(-50%, -50%)',
          width: 9, height: 9, borderRadius: '50%',
          background: t.success,
        }}/>
        {/* Точка конца полного отдыха — warning */}
        <div style={{
          position: 'absolute', top: '50%', left: '100%', transform: 'translate(-50%, -50%)',
          width: 9, height: 9, borderRadius: '50%',
          background: t.warning,
        }}/>
      </div>
      {/* Подписи под шкалой: начало · короткий отдых (у засечки) · полный отдых */}
      <div style={{ position: 'relative', marginTop: 7, height: 14, fontSize: 11, fontFamily: M.fontMono }}>
        <span style={{ position: 'absolute', left: 0, color: t.textFaint }}>{started}</span>
        <span style={{
          position: 'absolute', left: `${shortPct}%`, transform: 'translateX(-50%)',
          color: t.textFaint, fontWeight: 700,
        }}>{shortEnd}</span>
        <span style={{ position: 'absolute', right: 0, color: t.textFaint }}>{fullEnd}</span>
      </div>

      <div style={{ height: 1, background: t.border, margin: '18px 0 0' }}/>

      {/* Короткий отдых */}
      <RestRow t={t}
        label="Короткий отдых"
        end={shortEnd}
        right={reached
          ? <span style={{ color: t.success, fontWeight: 700 }}>выдержан</span>
          : <span>осталось <b style={{ fontFamily: M.fontMono, color: t.text }}>{shortLeft}</b></span>}
      />
      <div style={{ height: 1, background: t.border }}/>
      {/* Полный отдых */}
      <RestRow t={t}
        label="Полный отдых"
        end={fullEnd}
        right={<span>осталось <b style={{ fontFamily: M.fontMono, color: t.text }}>{fullLeft}</b></span>}
      />
    </div>
  );
}

// Строка контрольной точки отдыха: название + «до ЧЧ:ММ» · остаток справа
function RestRow({ t, label, end, right }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '15px 2px' }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 700, color: t.text, letterSpacing: -.2 }}>{label}</div>
        <div style={{ fontSize: 12, color: t.textMuted, marginTop: 1 }}>
          до <span style={{ fontFamily: M.fontMono, color: t.text }}>{end}</span>
        </div>
      </div>
      <div style={{ fontSize: 13, color: t.textMuted, textAlign: 'right', whiteSpace: 'nowrap' }}>{right}</div>
    </div>
  );
}

function CurrentTile({ t, children, active, dashed, icon, label }) {
  return (
    <div style={{
      width: 150, flexShrink: 0, scrollSnapAlign: 'start', minHeight: 150, padding: 18,
      background: active ? t.surface : 'transparent',
      borderRadius: 20,
      border: dashed ? `1.5px dashed ${t.borderStrong}` : 'none',
      boxShadow: active ? M.shadow.sm : 'none',
      display: 'flex', flexDirection: 'column', justifyContent: dashed ? 'center' : 'space-between',
      alignItems: dashed ? 'center' : 'stretch',
      gap: dashed ? 6 : 0,
      color: dashed ? t.accent : t.text,
    }}>
      {dashed ? (<>
        <div style={{ color: t.accent }}>{icon}</div>
        <div style={{ fontSize: 12, color: t.textMuted }}>{label}</div>
      </>) : children}
    </div>
  );
}

// Карта поездки в списке маршрутов.
// attrs — массив ключей атрибутов (см. ATTR_META); synced — статус синхронизации.
const ATTR_META = {
  holiday:  { Icon: IcHoliday,        label: 'Работа в праздничный день' },
  break:    { Icon: IcBreak,          label: 'Перерыв в работе' },
  length:   { Icon: IcRuler,          label: 'Поезда повышенной длины' },
  weight:   { Icon: IcWeight,         label: 'Поезда повышенной массы' },
  shoulder: { Icon: IcShoulder,       label: 'Удлинённое плечо обслуживания' },
  solo:     { Icon: IcCrewSolo,       label: 'Работа в одно лицо' },
  passenger:{ Icon: IcPassenger,      label: 'Следование пассажиром' },
  over12:   { Icon: IcMedal,          label: 'Работа свыше 12-ти часов' },
  pusher:   { Icon: IcPusher,         label: 'Толкач' },
  double:   { Icon: IcDoubleTraction, label: 'Двойная тяга' },
  coupled:  { Icon: IcCoupledTrain,   label: 'Сдвоенный поезд' },
};

function TripRow({ t, date, num, route, hours, attrs = [], synced = true }) {
  return (
    <div style={{ padding: '14px 20px', display: 'flex', flexDirection: 'column', gap: 6 }}>
      {/* Верх: даты + отработанные часы */}
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ fontSize: 16, fontFamily: M.fontMono, color: t.text, fontWeight: 600, letterSpacing: -.2 }}>{date}</div>
        {hours && <div style={{ fontFamily: M.fontMono, fontSize: 17, fontWeight: 700, color: t.text }}>{hours}</div>}
      </div>
      {/* Маршрут */}
      {route && (
        <div style={{ fontSize: 15, color: t.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{route}</div>
      )}
      {/* Низ: индекс + полоса атрибутов + статус синхронизации */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ fontSize: 13, color: t.textMuted, fontFamily: M.fontMono }}>{num}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {attrs.map(k => {
            const m = ATTR_META[k];
            if (!m) return null;
            const Icon = m.Icon;
            return <Icon key={k} width="18" height="18" style={{ color: t.text }}/>;
          })}
          <IcSync width="18" height="18" style={{ color: synced ? t.success : t.textFaint, opacity: synced ? 1 : .8 }}/>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Screen 3 — Обозначения (легенда символов)
// ─────────────────────────────────────────────────────────────
const LEGEND = [
  ['holiday',   IcHoliday,        'Работа в праздничный день'],
  ['break',     IcBreak,          'Перерыв в работе'],
  ['length',    IcRuler,          'Поезда повышенной длины'],
  ['weight',    IcWeight,         'Поезда повышенной массы'],
  ['shoulder',  IcShoulder,       'Удлинённое плечо обслуживания'],
  ['solo',      IcCrewSolo,       'Работа в одно лицо'],
  ['passenger', IcPassenger,      'Следование пассажиром'],
  ['over12',    IcMedal,          'Работа свыше 12-ти часов'],
  ['pusher',    IcPusher,         'Толкач'],
  ['double',    IcDoubleTraction, 'Двойная тяга'],
  ['coupled',   IcCoupledTrain,   'Сдвоенный поезд'],
  ['sync',      IcSync,           'Статус синхронизации маршрута'],
];

// Bottom-sheet «Обозначения» — открывается с главного экрана (Поездки).
function IOSLegendSheet({ t }) {
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
      {/* Scrim */}
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      {/* Sheet */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '82%',
        background: t.bg, borderRadius: '24px 24px 0 0',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.28)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        {/* Grabber */}
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 10 }}>
          <div style={{ width: 40, height: 5, borderRadius: 3, background: t.borderStrong }}/>
        </div>
        {/* Header */}
        <div style={{ padding: '12px 20px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ fontSize: 20, fontWeight: 700, color: t.text, letterSpacing: -.3 }}>Обозначения</div>
          <button style={{
            width: 30, height: 30, borderRadius: 15, border: 'none', cursor: 'pointer',
            background: t.bgSubtle, color: t.textMuted, fontSize: 15, lineHeight: 1,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>✕</button>
        </div>
        <div style={{ overflowY: 'auto', padding: '0 16px 28px' }}>
          <p style={{ margin: '0 4px 14px', fontSize: 14, color: t.textMuted, lineHeight: 1.5 }}>
            Значки в строке маршрута показывают его особенности. Один маршрут может иметь сразу несколько отметок.
          </p>
          <Card t={t}>
            {LEGEND.map(([key, Icon, label], i) => (
              <React.Fragment key={key}>
                {i > 0 && <div style={{ height: 1, background: t.border, marginLeft: 64 }}/>}
                <div style={{ padding: '12px 18px', display: 'flex', alignItems: 'center', gap: 16 }}>
                  <div style={{
                    width: 34, height: 34, borderRadius: 9, background: t.bgSubtle,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                    color: key === 'sync' ? t.success : t.text,
                  }}>
                    <Icon width="20" height="20"/>
                  </div>
                  <div style={{ fontSize: 15, color: t.text, lineHeight: 1.3 }}>{label}</div>
                </div>
              </React.Fragment>
            ))}
          </Card>
        </div>
      </div>
    </div>
  );
}

function IOSScreenLegend({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark}>
      <div style={{ paddingTop: 58 }}>
        <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <button style={pillBtn(t, { w: 40, h: 40 })}><IcChevronLeft width="18" height="18"/></button>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>Обозначения</div>
          <div style={{ width: 40 }}/>
        </div>
      </div>

      <div style={{ padding: '8px 16px 40px', overflowY: 'auto', height: 'calc(100% - 110px)' }}>
        <p style={{ margin: '4px 4px 16px', fontSize: 14, color: t.textMuted, lineHeight: 1.5 }}>
          Значки в строке маршрута показывают его особенности. Один маршрут может иметь сразу несколько отметок.
        </p>
        <Card t={t}>
          {LEGEND.map(([key, Icon, label], i) => (
            <React.Fragment key={key}>
              {i > 0 && <div style={{ height: 1, background: t.border, marginLeft: 64 }}/>}
              <div style={{ padding: '13px 18px', display: 'flex', alignItems: 'center', gap: 16 }}>
                <div style={{
                  width: 34, height: 34, borderRadius: 9, background: t.bgSubtle,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                  color: key === 'sync' ? t.success : t.text,
                }}>
                  <Icon width="20" height="20"/>
                </div>
                <div style={{ fontSize: 15, color: t.text, lineHeight: 1.3 }}>{label}</div>
              </div>
            </React.Fragment>
          ))}
        </Card>
      </div>
    </MDevice>
  );
}

function ToolTile({ t, icon, label }) {
  return (
    <div style={{
      background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm,
      width: 120, height: 120, flexShrink: 0, scrollSnapAlign: 'start',
      boxSizing: 'border-box', overflow: 'hidden',
      display: 'flex', flexDirection: 'column',
    }}>
      <div style={{
        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: t.text, opacity: .85,
      }}>{React.cloneElement(icon, { width: 40, height: 40 })}</div>
      <div style={{
        padding: '0 16px 14px',
        fontSize: 13, color: t.text, fontWeight: 500,
      }}>{label}</div>
    </div>
  );
}

function TabBtn({ t, active, icon, label }) {
  return (
    <button style={{
      flex: 1, border: 'none', background: 'transparent', padding: '4px 0',
      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
      color: active ? t.accent : t.textMuted, cursor: 'pointer',
    }}>
      {React.cloneElement(icon, { width: 22, height: 22 })}
      <span style={{ fontSize: 10, fontWeight: 600, fontFamily: M.fontSans }}>{label}</span>
    </button>
  );
}

Object.assign(window, { IOSScreenRoute, IOSScreenTrips, IOSScreenLegend, MDevice });
