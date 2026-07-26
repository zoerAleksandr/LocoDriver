// Android (Material-3-ish) screens — Машинист redesign

function hexToRgba(hex, a = 1) {
  if (!hex) return `rgba(0,0,0,${a})`;
  if (hex.startsWith('rgb')) return hex;
  let h = hex.replace('#', '');
  if (h.length === 3) h = h.split('').map(c => c + c).join('');
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  return `rgba(${r},${g},${b},${a})`;
}

function AStatus({ dark }) {
  const c = dark ? '#F2EFE8' : '#14120E';
  return (
    <div style={{
      height: 36, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 20px', position: 'relative', flexShrink: 0,
    }}>
      <div style={{ fontSize: 14, fontWeight: 500, color: c, fontFamily: M.fontSans }}>15:13</div>
      <div style={{
        position: 'absolute', left: '50%', top: 8, transform: 'translateX(-50%)',
        width: 20, height: 20, borderRadius: 10, background: '#000',
      }}/>
      <div style={{ display: 'flex', gap: 6, color: c }}>
        <svg width="14" height="14" viewBox="0 0 16 16"><path d="M8 13.3L.67 5.97a10.37 10.37 0 0114.66 0L8 13.3z" fill="currentColor"/></svg>
        <svg width="14" height="14" viewBox="0 0 16 16"><rect x="3.75" y="2" width="8.5" height="13" rx="1.5" fill="currentColor"/><rect x="5.5" y="0.9" width="5" height="2" rx="0.5" fill="currentColor"/></svg>
      </div>
    </div>
  );
}

function ADevice({ children, dark = false, width = 390, height = 844 }) {
  const t = dark ? M.dark : M.light;
  return (
    <div style={{
      width, height, borderRadius: 32, overflow: 'hidden',
      background: t.bg, border: `6px solid ${dark ? '#2a2a2a' : 'rgba(0,0,0,0.3)'}`,
      boxShadow: '0 40px 80px rgba(0,0,0,0.22)',
      fontFamily: M.fontSans, display: 'flex', flexDirection: 'column',
      boxSizing: 'border-box', position: 'relative',
    }}>
      <AStatus dark={dark}/>
      <div style={{ flex: 1, overflow: 'hidden', position: 'relative' }}>
        {children}
      </div>
      {/* gesture pill */}
      <div style={{ height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        <div style={{ width: 120, height: 3, borderRadius: 2, background: dark ? 'rgba(242,239,232,0.6)' : 'rgba(20,18,14,0.4)' }}/>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Android: Маршрут
// ─────────────────────────────────────────────────────────────
function AndroidScreenRoute({ dark = false, state = 'filled', sheet = null, confirm = null, nightWarn = false }) {
  const t = dark ? M.dark : M.light;
  const empty = state === 'empty';
  return (
    <ADevice dark={dark}>
      {sheet === 'calc' && <ACalcSheet t={t}/>}
      {sheet === 'rest' && <ARestSheet t={t} mode="po"/>}
      {sheet === 'rest-home' && <ARestSheet t={t} mode="home"/>}
      {confirm === 'loco' && <ARouteDeleteDialog t={t} title="Удалить локомотив?"
        message="ВЛ10-1456 будет убран из маршрута. Это действие нельзя отменить."/>}

      {/* App bar (Material small) */}
      <div style={{
        padding: '8px 4px 8px 4px', display: 'flex', alignItems: 'center', gap: 4,
        background: t.bg,
      }}>
        <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <IcChevronLeft width="22" height="22"/>
        </button>
        <div style={{ flex: 1, fontSize: 20, fontWeight: 500, color: t.text }}>
          {empty ? 'Новый маршрут' : 'Маршрут · №112'}
        </div>
        <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}
          aria-label="Ещё">
          <svg width="4" height="18" viewBox="0 0 4 18" fill="currentColor">
            <circle cx="2" cy="2" r="2"/><circle cx="2" cy="9" r="2"/><circle cx="2" cy="16" r="2"/>
          </svg>
        </button>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px - 80px)', padding: '8px 16px 24px' }}>
        <AGroup t={t}>Основные данные</AGroup>
        <MCard t={t}>
          <div style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ fontSize: 14, color: t.textMuted }}>Номер смены</span>
            {empty
              ? <span style={{ marginLeft: 'auto', fontSize: 15, color: t.textFaint }}>Введите номер</span>
              : <span style={{ marginLeft: 'auto', fontFamily: M.fontMono, fontSize: 20, fontWeight: 600, color: t.text }}>112</span>}
          </div>
        </MCard>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 12 }}>
          {/* Расчёт — карточка, открывает шторку */}
          <button onClick={() => {}} style={{
            textAlign: 'left', border: 'none', cursor: 'pointer',
            background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, padding: 16,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: t.accent, marginBottom: 8 }}>
              <IcRuble width="16" height="16"/><span style={{ fontSize: 12, color: t.textMuted }}>Расчёт</span>
            </div>
            <div style={{ fontSize: 22, fontWeight: 700, fontFamily: M.fontMono, color: empty ? t.textFaint : t.text }}>
              {empty ? '0,00 ₽' : '5 027,50 ₽'}
            </div>
          </button>
          {/* Отдых — карточка, открывает шторку */}
          <button onClick={() => {}} style={{
            textAlign: 'left', border: 'none', cursor: 'pointer',
            background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, padding: 16,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: t.accent, marginBottom: 8 }}>
              <IcHome width="16" height="16"/><span style={{ fontSize: 12, color: t.textMuted }}>Отдых</span>
            </div>
            <div style={{ fontSize: 18, fontWeight: 600, color: empty ? t.textFaint : t.text }}>
              {empty ? 'Не задан' : 'Домашний'}
            </div>
          </button>
        </div>

        <div style={{
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
          padding: '18px 4px 8px',
        }}>
          <div style={{
            fontFamily: M.fontMono, fontSize: 11, color: t.textMuted,
            letterSpacing: 1.2, textTransform: 'uppercase',
          }}>Время работы</div>
          {empty
            ? <div style={{ fontFamily: M.fontMono, fontSize: 20, fontWeight: 700, color: t.textFaint }}>—:—</div>
            : <span style={{ fontFamily: M.fontMono, fontSize: 20, fontWeight: 700, color: t.text, letterSpacing: -.3 }}>08:00</span>}
        </div>
        <MCard t={t}>
          <ATimeRow t={t} label="Явка" date={empty ? 'Выбрать' : '02 июн · 19:09'} muted={empty} tag={empty ? null : 'по прибытию пассажиром'}/>
          <div style={{ height: 1, background: t.border }}/>
          <ATimeRow t={t} label="Сдача" date={empty ? 'Выбрать' : '03 июн · 03:09'} muted={empty}/>
        </MCard>
        {!empty && (
          <div style={{ display: 'flex', gap: 8, padding: '9px 6px 0', color: t.textMuted }}>
            <IcPassenger width="15" height="15" style={{ flexShrink: 0, marginTop: 1, color: t.accent }}/>
            <span style={{ fontFamily: M.fontSans, fontSize: 12, lineHeight: 1.4 }}>
              Отсчёт ведётся с прибытия пассажиром (№ 8023).
            </span>
          </div>
        )}

        {/* Предупреждение о второй ночи подряд */}
        {nightWarn && !empty && (
          <div style={{ marginTop: 12 }}><RouteNightWarn t={t}/></div>
        )}

        <AGroup t={t}>Локомотивы</AGroup>
        <MCard t={t}>
          {!empty && <>
            <AUnitRow t={t} icon={<IcLocomotive width="22" height="22"/>} title="ВЛ10-1456"/>
            <div style={{ height: 1, background: t.border }}/>
          </>}
          <AAddRow t={t} icon={<IcLocomotive width="22" height="22"/>} label="Добавить локомотив"/>
        </MCard>

        <AGroup t={t}>Поезда</AGroup>
        <MCard t={t}>
          {!empty && <>
            <AUnitRow t={t} icon={<IcRails width="22" height="22"/>} title="№2712" meta="Лужская — СПбСМ"/>
            <div style={{ height: 1, background: t.border }}/>
          </>}
          <AAddRow t={t} icon={<IcRails width="22" height="22"/>} label="Добавить поезд"/>
        </MCard>

        <AGroup t={t}>Пассажиром</AGroup>
        <MCard t={t}>
          {!empty && <>
            <AUnitRow t={t} icon={<IcPassenger width="22" height="22"/>} title="№8023" meta="Новосибирск — Омск" right="04:20" badge="Начало работы"/>
            <div style={{ height: 1, background: t.border }}/>
          </>}
          <AAddRow t={t} icon={<IcPassenger width="22" height="22"/>} label="Добавить пассажиром"/>
        </MCard>

        <AGroup t={t}>Заметки</AGroup>
        <MCard t={t}>
          <div style={{ padding: '16px 20px', minHeight: 60, fontSize: 14, color: empty ? t.textFaint : t.text, lineHeight: 1.45 }}>
            {empty ? 'Опиши смену, если нужно…' : 'Подмена на ст. Лужская. Опоздание поезда 12 мин.'}
          </div>
        </MCard>
      </div>

      {/* Gradient scrim — faded content under bottom app bar */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0, height: 120, zIndex: 40,
        pointerEvents: 'none',
        background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)} 0%, ${hexToRgba(t.bg, 0.9)} 50%, ${t.bg} 100%)`,
      }}/>

      {/* Material 3 Bottom App Bar — данные сохраняются автоматически, кнопки
          совпадают с iOS: настройки маршрута + обратимые действия + удаление.
          Кнопки «Сохранить» нет. */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0, height: 80, zIndex: 50,
        background: t.surface,
        display: 'flex', alignItems: 'center', padding: '0 8px',
        borderTop: `1px solid ${t.border}`,
      }}>
        {/* Left cluster — настройки + обратимые действия, 48dp targets */}
        {[IcSliders, IcHeart, IcShare, IcCopy].map((I, i) => (
          <button key={i} style={{
            width: 48, height: 48, marginLeft: 4, border: 'none', background: 'transparent',
            color: t.text, borderRadius: 24, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><I width="22" height="22"/></button>
        ))}
        {/* Удаление — деструктивное, прижато вправо, error-цвет */}
        <button style={{
          marginLeft: 'auto', marginRight: 4,
          width: 48, height: 48, borderRadius: 24, border: 'none', cursor: 'pointer',
          background: 'transparent', color: t.danger,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }} aria-label="Удалить маршрут"><IcTrash width="22" height="22"/></button>
      </div>
    </ADevice>
  );
}

// Строка единицы маршрута (Material) — иконка в тональном контейнере,
// заголовок/описание, опциональное значение и кнопка-удаление (error-tonal).
function AUnitRow({ t, icon, title, meta, right, badge }) {
  return (
    <div style={{ padding: '12px 12px 12px 16px', display: 'flex', alignItems: 'center', gap: 14 }}>
      <div style={{
        width: 40, height: 40, borderRadius: 12, flexShrink: 0,
        background: t.bgSubtle, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{icon}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{title}</div>
        {meta && <div style={{ fontSize: 12.5, color: t.textMuted }}>{meta}</div>}
        {badge && <span style={{
          display: 'inline-flex', alignItems: 'center', marginTop: 5,
          fontFamily: M.fontMono, fontSize: 9.5, fontWeight: 700, letterSpacing: 0.8,
          color: t.accent, background: t.accentSoft,
          padding: '3px 7px', borderRadius: 6, textTransform: 'uppercase',
        }}>{badge}</span>}
      </div>
      {right && <div style={{ fontFamily: M.fontMono, fontSize: 14, color: t.text, whiteSpace: 'nowrap' }}>{right}</div>}
      <button style={{
        width: 40, height: 40, borderRadius: 20, flexShrink: 0, cursor: 'pointer', border: 'none',
        background: hexToRgba(t.danger, 0.10), color: t.danger,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }} aria-label="Удалить"><IcTrash width="20" height="20"/></button>
    </div>
  );
}

// Material AlertDialog — подтверждение удаления единицы маршрута.
function ARouteDeleteDialog({ t, title, message }) {
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 80, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'relative', width: '100%', maxWidth: 312, background: t.surface,
        borderRadius: 28, padding: 24, boxShadow: '0 24px 64px rgba(0,0,0,0.34)',
      }}>
        <div style={{ fontSize: 22, fontWeight: 600, color: t.text, marginBottom: 14 }}>{title}</div>
        <div style={{ fontSize: 14, color: t.textMuted, lineHeight: 1.45 }}>{message}</div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 24 }}>
          <button style={{
            padding: '10px 16px', borderRadius: 20, border: 'none', cursor: 'pointer',
            background: 'transparent', color: t.accent, fontSize: 14, fontWeight: 600,
          }}>Отмена</button>
          <button style={{
            padding: '10px 16px', borderRadius: 20, border: 'none', cursor: 'pointer',
            background: 'transparent', color: t.danger, fontSize: 14, fontWeight: 700,
          }}>Удалить</button>
        </div>
      </div>
    </div>
  );
}

// Material bottom-sheet shell для экрана Маршрут (Расчёт / Отдых).
function ARouteSheetShell({ t, title, subtitle, children }) {
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '88%',
        background: t.bg, borderRadius: '28px 28px 0 0',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.28)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 12 }}>
          <div style={{ width: 32, height: 4, borderRadius: 2, background: t.borderStrong }}/>
        </div>
        <div style={{ padding: '14px 24px 8px' }}>
          <div style={{ fontSize: 22, fontWeight: 600, color: t.text }}>{title}</div>
          {subtitle && <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>{subtitle}</div>}
        </div>
        <div style={{ overflowY: 'auto', padding: '6px 16px 28px' }}>{children}</div>
      </div>
    </div>
  );
}

// Шторка «Расчёт» (Material) — разбивка заработка за смену.
function ACalcSheet({ t }) {
  const rows = [
    ['Почасовая оплата', '8:00 × 375,00 ₽/ч', '3 000,00 ₽'],
    ['Зональная надбавка', '25 % от тарифа', '750,00 ₽'],
    ['Ночные', '07:51 в ночное время', '1 177,50 ₽'],
    ['Переотдых', 'компенсация за переотдых', '100,00 ₽'],
  ];
  return (
    <ARouteSheetShell t={t} title="Расчёт за смену" subtitle="Маршрут №112 · 08:00">
      {/* Итоговая сумма — filled tonal */}
      <div style={{
        background: t.accentSoft, borderRadius: 18, padding: '18px 20px', marginBottom: 12,
        display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
      }}>
        <div style={{ fontSize: 13, color: t.accent, fontWeight: 600 }}>Заработано</div>
        <div style={{ fontSize: 30, fontWeight: 800, fontFamily: M.fontMono, color: t.accent, letterSpacing: -1, lineHeight: 1, whiteSpace: 'nowrap' }}>5 027,50 ₽</div>
      </div>
      <MCard t={t}>
        {rows.map(([label, sub, amount], i) => (
          <React.Fragment key={label}>
            {i > 0 && <div style={{ height: 1, background: t.border }}/>}
            <div style={{ padding: '14px 20px', display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{label}</div>
                <div style={{ fontSize: 12, color: t.textMuted, marginTop: 1 }}>{sub}</div>
              </div>
              <div style={{ fontFamily: M.fontMono, fontSize: 16, fontWeight: 600, color: t.text, whiteSpace: 'nowrap' }}>{amount}</div>
            </div>
          </React.Fragment>
        ))}
      </MCard>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 6px 6px', color: t.textMuted }}>
        <IcInfo width="16" height="16"/>
        <span style={{ fontSize: 13, lineHeight: 1.4 }}>Суммы рассчитаны по вашим тарифам и нормам.</span>
      </div>
      {/* Переход в настройки зарплаты — Material list item */}
      <button style={{
        marginTop: 4, width: '100%', padding: '16px 20px', borderRadius: 16, cursor: 'pointer',
        background: t.surface, border: 'none', boxShadow: M.shadow.sm,
        display: 'flex', alignItems: 'center', gap: 14, color: t.text,
      }}>
        <IcGear width="22" height="22" style={{ color: t.textMuted }}/>
        <span style={{ flex: 1, textAlign: 'left', fontSize: 15, fontWeight: 500 }}>Настройки зарплаты</span>
        <IcChevronRight width="16" height="16" style={{ color: t.textFaint }}/>
      </button>
    </ARouteSheetShell>
  );
}

// Шторка «Отдых» (Material). mode: 'po' — в пункте оборота; 'home' — домашний.
function ARestSheet({ t, mode = 'po' }) {
  const home = mode === 'home';
  // Material segmented button (две опции)
  const Seg = ({ label, active }) => (
    <button style={{
      flex: 1, padding: '10px 0', border: 'none', cursor: 'pointer',
      background: active ? t.accentSoft : 'transparent',
      color: active ? t.accent : t.textMuted,
      fontSize: 14, fontWeight: 600,
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
    }}>
      {active && <IcCheck width="16" height="16"/>}
      {label}
    </button>
  );
  const RestItem = ({ title, dur, until }) => (
    <div style={{ padding: '16px 20px' }}>
      <div style={{ fontSize: 14, color: t.textMuted, marginBottom: 3 }}>
        {title} <span style={{ fontFamily: M.fontMono }}>· {dur}</span>
      </div>
      <div style={{ fontFamily: M.fontMono, fontSize: 22, fontWeight: 700, letterSpacing: -.3, color: t.text, whiteSpace: 'nowrap' }}>
        до {until}
      </div>
    </div>
  );
  return (
    <ARouteSheetShell t={t} title="Отдых" subtitle="От времени сдачи · 03.06.26 03:09">
      {/* Тип отдыха — Material segmented button */}
      <div style={{
        display: 'flex', borderRadius: 999, overflow: 'hidden',
        border: `1px solid ${t.borderStrong}`, marginBottom: 14,
      }}>
        <Seg label="Домашний" active={home}/>
        <div style={{ width: 1, background: t.borderStrong }}/>
        <Seg label="В пункте оборота" active={!home}/>
      </div>
      {home ? (<>
        <MCard t={t}>
          <RestItem title="Минимальный отдых" dur="16 ч 0 м" until="03.06 · 19:09"/>
          <div style={{ height: 1, background: t.border }}/>
          <RestItem title="Полный отдых" dur="20 ч 48 м" until="03.06 · 23:57"/>
        </MCard>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 6px 0', color: t.textMuted }}>
          <IcInfo width="16" height="16"/>
          <span style={{ fontSize: 13, lineHeight: 1.4 }}>Полный = время работы 08:00 × 2,6. Минимум — 16 ч.</span>
        </div>
      </>) : (
        <MCard t={t}>
          <RestItem title="Короткий отдых" dur="4 ч 0 м" until="03.06 · 07:09"/>
          <div style={{ height: 1, background: t.border }}/>
          <RestItem title="Полный отдых" dur="8 ч 0 м" until="03.06 · 11:09"/>
        </MCard>
      )}
      <button style={{
        marginTop: 12, width: '100%', padding: '16px 20px', borderRadius: 16, cursor: 'pointer',
        background: t.surface, border: 'none', boxShadow: M.shadow.sm,
        display: 'flex', alignItems: 'center', gap: 14, color: t.text,
      }}>
        <IcGear width="22" height="22" style={{ color: t.textMuted }}/>
        <span style={{ flex: 1, textAlign: 'left', fontSize: 15, fontWeight: 500 }}>Настройки времени отдыха</span>
        <IcChevronRight width="16" height="16" style={{ color: t.textFaint }}/>
      </button>
    </ARouteSheetShell>
  );
}

// ─────────────────────────────────────────────────────────────
// Android: Главный (Поездки)
// ─────────────────────────────────────────────────────────────
function AndroidScreenTrips({ dark = false, height = 980, sheet = null, empty = false, state = 'current', heroPage = 0, holiday = false }) {
  const t = dark ? M.dark : M.light;
  const upcoming = state === 'upcoming';
  const rest = state === 'rest';
  return (
    <ADevice dark={dark} height={height}>
      {sheet === 'legend' && <ALegendSheet t={t}/>}
      {sheet === 'units' && <AUnitsSheet t={t}/>}
      {/* Large top app bar */}
      <div style={{ padding: '8px 4px 0' }}>
        <div style={{ height: 48, display: 'flex', alignItems: 'center', padding: '0 4px', gap: 4 }}>
          <div style={{ width: 48, height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.text }}>
            <IcLogo size={26} color={t.text}/>
          </div>
          <div style={{ flex: 1, fontSize: 15, fontWeight: 600, color: t.text }}>Машинист</div>
          <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}><IcSearch width="22" height="22"/></button>
          <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}><IcProfile width="22" height="22"/></button>
        </div>
        <div style={{ padding: '12px 20px 16px', fontSize: 28, fontWeight: 700, color: t.text, letterSpacing: -.5 }}>
          Апрель <span style={{ color: t.textMuted, fontWeight: 500 }}>2026</span>
        </div>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px)', padding: '0 16px 100px' }}>
        {/* Постоянная шапка — итог месяца, вне свайп-зоны */}
        <div style={{ padding: '2px 4px', display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 16 }}>
          <div>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4 }}>ОТРАБОТАНО</div>
            <div style={{ fontSize: 46, fontWeight: 800, fontFamily: M.fontMono, color: t.text, letterSpacing: -1.4, lineHeight: 1, marginTop: 2 }}>
              205:05
            </div>
            {holiday && (
              <div style={{ fontSize: 12, color: t.textMuted, marginTop: 6, letterSpacing: -.1 }}>
                праздничные <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>16:00</span>
                <span style={{ color: t.textFaint, margin: '0 6px' }}>·</span>
                обычные <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>189:05</span>
              </div>
            )}
          </div>
          <div style={{
            background: t.accentSoft, color: t.accent, padding: '6px 12px', borderRadius: 999,
            fontSize: 13, fontWeight: 700, fontFamily: M.fontMono,
          }}>+30:05 сверх</div>
        </div>

        {/* Свайп-карточка с разбивками */}
        <div style={{
          background: t.surface, borderRadius: 20, padding: 22, boxShadow: M.shadow.sm,
        }}>
          {heroPage === 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <HourBar t={t} label="Норма на месяц" value={175} max={200} color={t.accent}/>
            <HourBar t={t} label="Норма на 23.04" value={136} max={200} color={t.accent}/>
            <HourBar t={t} label="Отработано на 23.04" value={181} max={200}
              color={state === 'upcoming' ? t.warning : t.success}
              fillPct={state === 'rest' ? 100 : undefined} highlight/>
          </div>
          )}

          {heroPage === 1 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <HeroStatRow t={t} label="Ночные" value="13:51" pct={64} accent={t.accent}/>
            <HeroStatRow t={t} label="Пассажиром" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Резервом" value="00:00" pct={0}/>
          </div>
          )}

          {heroPage === 2 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 15 }}>
            <HeroStatRow t={t} label="Удл. плечи обслуживания" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Длинносоставные" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Тяжелые" value="00:00" pct={0}/>
            <HeroStatRow t={t} label="Одно лицо" value="00:00" pct={0}/>
          </div>
          )}
        </div>

        {/* Точечный индикатор под свайп-карточкой */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 12, marginBottom: 20 }}>
          {[0, 1, 2].map(i => (
            <div key={i} style={{
              width: i === heroPage ? 18 : 6, height: 6, borderRadius: 3,
              background: i === heroPage ? t.textMuted : t.borderStrong,
              transition: 'width .2s',
            }}/>
          ))}
        </div>

        {/* Текущий маршрут — крупные карточки с горизонтальным скролом */}
        {rest ? (<>
          <AGroup t={t}>Отдых в ПО</AGroup>
          <div style={{
            background: t.surface, borderRadius: 18, padding: 20, boxShadow: M.shadow.sm,
            margin: '8px 0 16px',
          }}>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase' }}>Отдыхаете</div>
            <div style={{ fontSize: 30, fontWeight: 800, fontFamily: M.fontMono, color: t.text, letterSpacing: -1, lineHeight: 1, marginTop: 4 }}>02:14</div>
            <div style={{ fontSize: 13, color: t.textMuted, marginTop: 8 }}>
              начало отдыха в <span style={{ fontFamily: M.fontMono, color: t.text }}>12:16</span>
            </div>

            {/* Полоса прогресса с точками короткого и полного отдыха */}
            <div style={{ position: 'relative', marginTop: 22, height: 3 }}>
              <div style={{ position: 'absolute', inset: 0, background: t.accentSoft, borderRadius: 2, overflow: 'hidden' }}>
                <div style={{ width: '41%', height: '100%', background: t.accent }}/>
              </div>
              <div style={{
                position: 'absolute', top: '50%', left: '55%', transform: 'translate(-50%, -50%)',
                width: 9, height: 9, borderRadius: '50%', background: t.success,
              }}/>
              <div style={{
                position: 'absolute', top: '50%', left: '100%', transform: 'translate(-50%, -50%)',
                width: 9, height: 9, borderRadius: '50%', background: t.warning,
              }}/>
            </div>
            <div style={{ position: 'relative', marginTop: 7, height: 14, fontSize: 11, fontFamily: M.fontMono, color: t.textFaint }}>
              <span style={{ position: 'absolute', left: 0 }}>12:16</span>
              <span style={{ position: 'absolute', left: '55%', transform: 'translateX(-50%)', fontWeight: 700 }}>15:16</span>
              <span style={{ position: 'absolute', right: 0 }}>17:46</span>
            </div>

            <div style={{ height: 1, background: t.border, margin: '18px 0 0' }}/>
            {/* Короткий отдых */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '15px 2px' }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 700, color: t.text, letterSpacing: -.2 }}>Короткий отдых</div>
                <div style={{ fontSize: 12, color: t.textMuted, marginTop: 1 }}>до <span style={{ fontFamily: M.fontMono, color: t.text }}>15:16</span></div>
              </div>
              <div style={{ fontSize: 13, color: t.textMuted, whiteSpace: 'nowrap' }}>осталось <b style={{ fontFamily: M.fontMono, color: t.text }}>00:46</b></div>
            </div>
            <div style={{ height: 1, background: t.border }}/>
            {/* Полный отдых */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '15px 2px' }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 700, color: t.text, letterSpacing: -.2 }}>Полный отдых</div>
                <div style={{ fontSize: 12, color: t.textMuted, marginTop: 1 }}>до <span style={{ fontFamily: M.fontMono, color: t.text }}>17:46</span></div>
              </div>
              <div style={{ fontSize: 13, color: t.textMuted, whiteSpace: 'nowrap' }}>осталось <b style={{ fontFamily: M.fontMono, color: t.text }}>03:16</b></div>
            </div>
          </div>
        </>) : upcoming ? (<>
          <AGroup t={t}>Следующий маршрут</AGroup>
          <div style={{
            background: t.surface, borderRadius: 18, padding: 20, boxShadow: M.shadow.sm,
            margin: '8px 0 16px',
          }}>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4, textTransform: 'uppercase' }}>До явки осталось</div>
            <div style={{ fontSize: 30, fontWeight: 800, fontFamily: M.fontMono, color: t.text, letterSpacing: -1, lineHeight: 1, marginTop: 4 }}>18:45</div>
            <div style={{ height: 3, background: t.accentSoft, borderRadius: 2, marginTop: 12, overflow: 'hidden' }}>
              <div style={{ width: '64%', height: '100%', background: t.accent }}/>
            </div>
            <div style={{ height: 1, background: t.border, margin: '18px 0 0' }}/>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14, paddingTop: 16 }}>
              <div style={{
                width: 38, height: 38, borderRadius: 11, flexShrink: 0,
                background: t.bgSubtle, color: t.accent,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}><IcCalendar width="20" height="20"/></div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 12, color: t.textMuted, marginBottom: 2 }}>Явка</div>
                <div style={{ fontSize: 16, fontWeight: 700, color: t.text, letterSpacing: -.2 }}>
                  25 апреля, Сб<span style={{ fontWeight: 500, color: t.textMuted }}> · </span>
                  <span style={{ fontFamily: M.fontMono }}>20:00</span>
                </div>
              </div>
            </div>
          </div>
        </>) : (<>
        <AGroup t={t}>Текущий маршрут</AGroup>
        <div style={{
          display: 'flex', gap: 12, overflowX: 'auto', margin: '0 0 16px', padding: '8px 0 6px',
          scrollSnapType: 'x mandatory',
        }}>
          {/* Активная — на работе */}
          <div style={{
            width: 150, height: 150, flexShrink: 0, scrollSnapAlign: 'start',
            background: t.surface, color: t.text, borderRadius: 18, padding: 18,
            display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
            boxShadow: M.shadow.sm,
          }}>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.2, textTransform: 'uppercase' }}>На работе</div>
            <div>
              <div style={{ fontSize: 30, fontWeight: 800, fontFamily: M.fontMono, letterSpacing: -1, lineHeight: 1 }}>00:08</div>
              <div style={{ height: 3, background: t.accentSoft, borderRadius: 2, marginTop: 8, overflow: 'hidden' }}>
                <div style={{ width: '8%', height: '100%', background: t.accent }}/>
              </div>
            </div>
          </div>
          {empty ? (<>
            <AAddTile t={t} icon={<IcLocomotive width="32" height="32"/>} kind="Локомотив"/>
            <AAddTile t={t} icon={<IcRails width="32" height="32"/>} kind="Поезд"/>
            <AAddTile t={t} icon={<IcPassenger width="32" height="32"/>} kind="Пассажиром"/>
          </>) : (<>
            {/* Локомотив — есть данные */}
            <AFilledTile t={t} icon={<IcLocomotive width="32" height="32"/>} kind="Локомотив"
              title="ВЛ10-1456" count={2}/>
            {/* Поезд — есть данные */}
            <AFilledTile t={t} icon={<IcRails width="32" height="32"/>} kind="Поезд"
              title="№2712" meta="Лужская — СПбСМ"/>
            {/* Пассажиром — есть данные */}
            <AFilledTile t={t} icon={<IcPassenger width="32" height="32"/>} kind="Пассажиром"
              title="№8023" meta="Новосибирск — Омск"/>
          </>)}
        </div>
        </>)}

        <div style={{
          padding: '18px 4px 8px',
        }}>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted,
            letterSpacing: 1.2, textTransform: 'uppercase' }}>Последние маршруты · 23</div>
        </div>
        <MCard t={t}>
          <ATripRow t={t} date="28.05 17:40 — 21:08" num="№14" route="№4404 Луга — СПбСМ" hours="03:27" attrs={['passenger', 'over12']} synced={false}/>
          <div style={{ height: 1, background: t.border }}/>
          <ATripRow t={t} date="22.05 08:46 — 19:27" num="№13" route="№2712 Лужская — СПбСМ" hours="10:41" attrs={['length']}/>
        </MCard>

        <AGroup t={t}>Инструменты</AGroup>
        <div style={{
          display: 'flex', gap: 10, overflowX: 'auto', padding: '0 0 4px',
          scrollSnapType: 'x mandatory',
        }}>
          <MToolTile t={t} icon={<IcCalendar/>} label="График"/>
          <MToolTile t={t} icon={<IcArmchair/>} label="Отвлечения"/>
          <MToolTile t={t} icon={<IcSearch/>} label="Поиск"/>
          <MToolTile t={t} icon={<IcPdf/>} label="PDF"/>
        </div>
      </div>

      {/* Bottom nav bar (Material) */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0,
        background: t.surface, height: 72,
        display: 'flex', alignItems: 'center',
        borderTop: `1px solid ${t.border}`,
      }}>
        {[
          [IcDocument, 'Главный', true],
          [IcRuble, 'Зарплата', false],
          [IcPlus, '', false, true],
          [IcSliders, 'Настройки', false],
          [IcProfile, 'Профиль', false],
        ].map(([I, l, active, fab], i) => fab ? (
          <div key={i} style={{ flex: 1, display: 'flex', justifyContent: 'center' }}>
            <button style={{
              width: 56, height: 32, borderRadius: 16, background: t.accent, color: t.accentInk, border: 'none',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: M.shadow.sm,
            }}><I width="22" height="22"/></button>
          </div>
        ) : (
          <button key={i} style={{
            flex: 1, border: 'none', background: 'transparent', display: 'flex',
            flexDirection: 'column', alignItems: 'center', gap: 2, cursor: 'pointer',
            color: active ? t.accent : t.textMuted,
          }}>
            <div style={{
              padding: '4px 16px', borderRadius: 16,
              background: active ? t.accentSoft : 'transparent',
            }}><I width="22" height="22"/></div>
            <span style={{ fontSize: 11, fontWeight: 600 }}>{l}</span>
          </button>
        ))}
      </div>
    </ADevice>
  );
}

// Android helpers
function AGroup({ t, children }) {
  return (
    <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted,
      letterSpacing: 1.2, textTransform: 'uppercase',
      padding: '18px 4px 8px',
    }}>{children}</div>
  );
}
function MCard({ t, children }) {
  return (
    <div style={{ background: t.surface, borderRadius: 16, overflow: 'hidden', boxShadow: M.shadow.sm }}>
      {children}
    </div>
  );
}
function ATimeRow({ t, label, date, muted = false, tag }) {
  return (
    <div style={{ padding: '14px 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
      <span style={{ fontSize: 14, color: t.textMuted }}>{label}</span>
      {tag && <span style={{
        fontFamily: M.fontMono, fontSize: 9.5, fontWeight: 700, letterSpacing: 0.8,
        color: t.accent, background: t.accentSoft,
        padding: '3px 7px', borderRadius: 6, textTransform: 'uppercase',
      }}>{tag}</span>}
      <span style={{ flex: 1 }}/>
      <span style={{ fontFamily: M.fontMono, fontSize: 15, color: muted ? t.textFaint : t.text, fontWeight: 500 }}>{date}</span>
    </div>
  );
}
function AAddRow({ t, icon, label }) {
  return (
    <div style={{ padding: '14px 20px', display: 'flex', alignItems: 'center', gap: 14,
      color: t.accent, fontWeight: 500, fontSize: 15,
    }}>
      <div style={{ color: t.accent }}>{icon}</div>
      {label}
      <IcChevronRight width="14" height="14" style={{ marginLeft: 'auto', color: t.textFaint }}/>
    </div>
  );
}
function MToolTile({ t, icon, label }) {
  return (
    <div style={{
      background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm,
      width: 120, height: 120, flexShrink: 0, scrollSnapAlign: 'start',
      boxSizing: 'border-box', overflow: 'hidden',
      display: 'flex', flexDirection: 'column',
    }}>
      <div style={{
        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: t.text, opacity: .85,
      }}>{React.cloneElement(icon, { width: 38, height: 38 })}</div>
      <div style={{
        padding: '0 16px 14px',
        fontSize: 13, color: t.text, fontWeight: 500,
      }}>{label}</div>
    </div>
  );
}

const A_ATTR_META = {
  holiday:  IcHoliday, break: IcBreak, length: IcRuler, weight: IcWeight,
  shoulder: IcShoulder, solo: IcCrewSolo, passenger: IcPassenger, over12: IcMedal,
  pusher: IcPusher, double: IcDoubleTraction, coupled: IcCoupledTrain,
};
const A_LEGEND = [
  [IcHoliday, 'Работа в праздничный день'], [IcBreak, 'Перерыв в работе'],
  [IcRuler, 'Поезда повышенной длины'], [IcWeight, 'Поезда повышенной массы'],
  [IcShoulder, 'Удлинённое плечо обслуживания'], [IcCrewSolo, 'Работа в одно лицо'],
  [IcPassenger, 'Следование пассажиром'], [IcMedal, 'Работа свыше 12-ти часов'],
  [IcPusher, 'Толкач'], [IcDoubleTraction, 'Двойная тяга'],
  [IcCoupledTrain, 'Сдвоенный поезд'], [IcSync, 'Статус синхронизации маршрута'],
];

function ATripRow({ t, date, num, route, hours, attrs = [], synced = true }) {  return (
    <div style={{ padding: '14px 20px', display: 'flex', flexDirection: 'column', gap: 6 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ fontSize: 15, fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{date}</div>
        {hours && <div style={{ fontFamily: M.fontMono, fontSize: 16, fontWeight: 700, color: t.text }}>{hours}</div>}
      </div>
      {route && <div style={{ fontSize: 14, color: t.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{route}</div>}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ fontSize: 12, color: t.textMuted, fontFamily: M.fontMono }}>{num}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {attrs.map(k => { const Icon = A_ATTR_META[k]; return Icon ? <Icon key={k} width="18" height="18" style={{ color: t.text }}/> : null; })}
          <IcSync width="18" height="18" style={{ color: synced ? t.success : t.textFaint }}/>
        </div>
      </div>
    </div>
  );
}

// Бейдж-счётчик «N» для угла карточки нескольких единиц.
function ACountBadge({ t, n }) {
  return (
    <div style={{
      minWidth: 19, height: 19, borderRadius: 10, padding: '0 5px',
      background: t.accent, color: t.accentInk,
      fontSize: 11, fontWeight: 700, fontFamily: M.fontMono,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>{n}</div>
  );
}

// Пустая карточка текущего маршрута (Material) — когда единица ещё не добавлена.
function AAddTile({ t, icon, kind }) {
  return (
    <div style={{
      width: 150, height: 150, flexShrink: 0, scrollSnapAlign: 'start', position: 'relative',
    }}>
      <button style={{
        position: 'absolute', inset: 0, padding: 16,
        background: 'transparent', borderRadius: 18, border: `1.5px dashed ${t.borderStrong}`,
        cursor: 'pointer', textAlign: 'left',
        display: 'flex', flexDirection: 'column',
      }} aria-label={'Добавить: ' + kind}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 6, minHeight: 32 }}>
          <div style={{ color: t.textFaint }}>{icon}</div>
        </div>
        <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1, textTransform: 'uppercase', marginBottom: 3 }}>{kind}</div>
      </button>
      <span style={{
        position: 'absolute', right: 12, bottom: 12, zIndex: 2,
        width: 30, height: 30, borderRadius: 15, flexShrink: 0,
        background: t.accentSoft, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }} aria-label={'Добавить: ' + kind}><IcPlus width="18" height="18"/></span>
    </div>
  );
}

// Заполненная карточка текущего маршрута — крупная иконка + данные.
// count > 1 — несколько единиц: карточка позади + бейдж «N Все» вверху справа,
// тап открывает шторку со списком. Кнопка «+» внизу справа добавляет ещё одну.
function AFilledTile({ t, icon, kind, title, meta, count = 1 }) {
  const many = count > 1;
  return (
    <div style={{
      width: 150, height: 150, flexShrink: 0, scrollSnapAlign: 'start', position: 'relative',
    }}>
      {many && (
        <div style={{
          position: 'absolute', left: 7, right: -7, top: -7, height: 150,
          background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm,
        }}/>
      )}
      <button style={{
        position: 'absolute', inset: 0, padding: 16,
        background: t.surface, color: t.text, borderRadius: 18, boxShadow: M.shadow.sm,
        border: many ? `1px solid ${t.border}` : 'none',
        cursor: 'pointer', textAlign: 'left',
        display: 'flex', flexDirection: 'column',
      }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 6, minHeight: 32 }}>
          <div style={{ color: t.accent }}>{icon}</div>
          {many && <ACountBadge t={t} n={count}/>}
        </div>
        <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1, textTransform: 'uppercase', marginBottom: 3 }}>{kind}</div>
        <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, whiteSpace: 'nowrap' }}>{title}</div>
        {meta && <div style={{ fontSize: 12.5, color: t.textMuted, lineHeight: 1.25, marginTop: 2, paddingRight: 34 }}>{meta}</div>}
      </button>
      <span style={{
        position: 'absolute', right: 12, bottom: 12, zIndex: 2,
        width: 30, height: 30, borderRadius: 15, flexShrink: 0,
        background: t.accentSoft, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }} aria-label={'Добавить ещё: ' + kind}><IcPlus width="18" height="18"/></span>
    </div>
  );
}

// Bottom-sheet со списком единиц одного типа (Material) — открывается тапом
// на карточку «Локомотив» с бейджем «N Все».
function AUnitsSheet({ t }) {
  const Row = ({ title }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '14px 8px' }}>
      <div style={{
        width: 44, height: 44, borderRadius: 14, flexShrink: 0,
        background: t.bgSubtle, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}><IcLocomotive width="24" height="24"/></div>
      <div style={{ flex: 1, fontSize: 17, fontWeight: 600, color: t.text }}>{title}</div>
      <IcChevronRight width="18" height="18" style={{ color: t.textFaint }}/>
    </div>
  );
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '82%',
        background: t.bg, borderRadius: '28px 28px 0 0',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.28)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 12 }}>
          <div style={{ width: 32, height: 4, borderRadius: 2, background: t.borderStrong }}/>
        </div>
        <div style={{ padding: '14px 24px 10px', fontSize: 22, fontWeight: 600, color: t.text }}>
          Локомотивы · 2
        </div>
        <div style={{ padding: '0 16px 12px' }}>
          <Row title="ВЛ10-1456"/>
          <Row title="ВЛ10-1457"/>
        </div>
        <div style={{ padding: '4px 16px 28px' }}>
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

// Bottom-sheet «Обозначения» — открывается с главного экрана (Поездки).
function ALegendSheet({ t }) {
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '82%',
        background: t.bg, borderRadius: '28px 28px 0 0',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.28)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 12 }}>
          <div style={{ width: 32, height: 4, borderRadius: 2, background: t.borderStrong }}/>
        </div>
        <div style={{ padding: '14px 24px 6px', fontSize: 22, fontWeight: 600, color: t.text }}>
          Обозначения
        </div>
        <div style={{ overflowY: 'auto', padding: '0 16px 28px' }}>
          <p style={{ margin: '4px 8px 12px', fontSize: 14, color: t.textMuted, lineHeight: 1.5 }}>
            Значки в строке маршрута показывают его особенности. У одного маршрута может быть несколько отметок.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {A_LEGEND.map(([Icon, label], i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 8px' }}>
                <div style={{
                  width: 40, height: 40, borderRadius: 12, background: t.bgSubtle, flexShrink: 0,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: i === A_LEGEND.length - 1 ? t.success : t.text,
                }}><Icon width="22" height="22"/></div>
                <div style={{ fontSize: 15, color: t.text }}>{label}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function AndroidScreenLegend({ dark = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark}>
      <div style={{ padding: '8px 4px 0' }}>
        <div style={{ height: 56, display: 'flex', alignItems: 'center', padding: '0 4px', gap: 4 }}>
          <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}><IcChevronLeft width="22" height="22"/></button>
          <div style={{ flex: 1, fontSize: 20, fontWeight: 600, color: t.text }}>Обозначения</div>
        </div>
      </div>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 120px)', padding: '0 16px 40px' }}>
        <p style={{ margin: '4px 4px 16px', fontSize: 14, color: t.textMuted, lineHeight: 1.5 }}>
          Значки в строке маршрута показывают его особенности. У одного маршрута может быть несколько отметок.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {A_LEGEND.map(([Icon, label], i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 8px' }}>
              <div style={{
                width: 40, height: 40, borderRadius: 12, background: t.bgSubtle, flexShrink: 0,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: i === A_LEGEND.length - 1 ? t.success : t.text,
              }}><Icon width="22" height="22"/></div>
              <div style={{ fontSize: 15, color: t.text }}>{label}</div>
            </div>
          ))}
        </div>
      </div>
    </ADevice>
  );
}

Object.assign(window, { AndroidScreenRoute, AndroidScreenTrips, AndroidScreenLegend });
