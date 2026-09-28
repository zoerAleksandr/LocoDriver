// «Вторая ночь подряд» — предупреждающая плашка для блока «Время»
// =====================================================================
// Встраивается в экран Маршрут (iOS + Android) сразу под карточкой
// «Время работы». Появляется, когда рабочее время захватывает второе
// ночное окно (00:00–05:00) подряд — два ночных маршрута друг за другом.
//
// Тон — warning (жёлтый t.warning). Только информирование, без действий.
// На временной линии (18:00 → 09:00) видно и время маршрута (белая
// полоса), и ночные часы внутри окна 00:00–05:00 (синие), плюс счётчик
// ночных часов справа.
//
// Зависимости (глобальные): M (tokens.js), hexToRgba (shared-ui).
// Экспорт: window.RouteNightWarn
// =====================================================================

// ── Иконка «луна» (в наборе нет) ─────────────────────────────────────
function NW_IcMoon(p) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>
    </svg>
  );
}

// Временная ось: 18:00 → 09:00 след. дня (минуты от полуночи, ночь по центру)
const NW_AX0  = 18 * 60;          // старт оси, мин
const NW_SPAN = 15 * 60;          // длина оси, мин (15 ч)
const nwPct   = (min) => ((min - NW_AX0) / NW_SPAN) * 100;

// Ночное окно 00:00–05:00 (следующие сутки → +24ч)
const NW_NIGHT_A = 24 * 60;       // 00:00
const NW_NIGHT_B = 29 * 60;       // 05:00

function NightTimeline({ t, rs, re, routeCol }) {
  // rs / re — явка и сдача в минутах от 18:00-полуночи (со сдвигом +24ч за полночь)
  const routeL = nwPct(rs), routeW = nwPct(re) - routeL;
  const ovS = Math.max(rs, NW_NIGHT_A), ovE = Math.min(re, NW_NIGHT_B);
  const hasOv = ovE > ovS;
  const ovL = nwPct(ovS), ovW = nwPct(ovE) - ovL;
  const nightL = nwPct(NW_NIGHT_A);
  return (
    <div style={{ position: 'relative', flex: 1, height: 26, borderRadius: 6 }}>
      {/* ночное окно 00:00–05:00 — только вертикальные линии */}
      <div style={{
        position: 'absolute', top: -3, bottom: -3, left: `${nightL}%`,
        borderLeft: `1.5px dashed ${hexToRgba(t.accent, .7)}`,
      }}/>
      <div style={{
        position: 'absolute', top: -3, bottom: -3, left: `${nwPct(NW_NIGHT_B)}%`,
        borderLeft: `1.5px dashed ${hexToRgba(t.accent, .7)}`,
      }}/>
      {/* маршрут — сплошная полоса (белая) */}
      <div style={{
        position: 'absolute', top: 4, bottom: 4, left: `${routeL}%`, width: `${routeW}%`,
        borderRadius: 5, background: routeCol, border: `1px solid ${hexToRgba(t.text, .18)}`,
      }}/>
      {/* ночные часы маршрута — поверх (синие) */}
      {hasOv && (
        <div style={{
          position: 'absolute', top: 4, bottom: 4, left: `${ovL}%`, width: `${ovW}%`,
          background: t.accent,
        }}/>
      )}
    </div>
  );
}

// ── Плашка-предупреждение ────────────────────────────────────────────
function RouteNightWarn({ t, style }) {
  const [open, setOpen] = React.useState(true);
  if (!open) return null;
  const routeCol = '#FFFFFF'; // белый — маршрут
  const rows = [
    { day: 'Пред. маршрут', rs: 20 * 60,      re: 28 * 60,      route: '20:00–04:00' }, // 04:00 +1
    { day: 'Этот маршрут',  rs: 19 * 60 + 9,  re: 27 * 60 + 9,  route: '19:09–03:09' }, // 03:09 +1
  ];
  const nightHrs = (r) => {
    const m = Math.min(r.re, NW_NIGHT_B) - Math.max(r.rs, NW_NIGHT_A);
    const h = m / 60;
    return Number.isInteger(h) ? `${h} ч` : `${h.toFixed(1).replace('.', ',')} ч`;
  };
  const ticks = [
    { min: NW_NIGHT_A, lab: '00:00' },
    { min: NW_NIGHT_B, lab: '05:00' },
  ];
  return (
    <div style={{
      padding: '12px 14px 13px', borderRadius: 14,
      background: hexToRgba(t.warning, .10),
      border: `1px solid ${hexToRgba(t.warning, .55)}`,
      ...style,
    }}>
      {/* шапка */}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <div style={{
          width: 26, height: 26, borderRadius: 13, flexShrink: 0,
          background: hexToRgba(t.warning, .18), color: t.warning,
          display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <NW_IcMoon width="14" height="14"/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13.5, fontWeight: 700, color: t.text, lineHeight: 1.25 }}>
            Вторая ночь подряд
          </div>
          <div style={{ fontSize: 12, color: t.textMuted, marginTop: 2, lineHeight: 1.4 }}>
            Третья ночь подряд не&nbsp;допускается.
          </div>
        </div>
        <button onClick={() => setOpen(false)} aria-label="Скрыть" style={{
          flexShrink: 0, width: 24, height: 24, marginTop: -2, marginRight: -4,
          padding: 0, border: 'none', background: 'transparent', cursor: 'pointer',
          display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          color: t.textMuted, borderRadius: 12,
        }}>
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor"
               strokeWidth="2.2" strokeLinecap="round">
            <path d="M6 6l12 12M18 6L6 18"/>
          </svg>
        </button>
      </div>

      {/* временная линия */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 14 }}>
        {rows.map((r, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 96, flexShrink: 0 }}>
              <div style={{ fontSize: 11.5, fontWeight: 600, color: t.text, lineHeight: 1.2, whiteSpace: 'nowrap' }}>{r.day}</div>
              <div style={{ fontFamily: M.fontMono, fontSize: 10, color: t.textMuted, marginTop: 1 }}>{r.route}</div>
            </div>
            <NightTimeline t={t} rs={r.rs} re={r.re} routeCol={routeCol}/>
            <div style={{
              width: 40, flexShrink: 0, textAlign: 'right',
              fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, color: t.accent,
            }}>{nightHrs(r)}</div>
          </div>
        ))}
      </div>

      {/* ось времени */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 5 }}>
        <div style={{ width: 96, flexShrink: 0 }}/>
        <div style={{ position: 'relative', flex: 1, height: 13 }}>
          {ticks.map((tk, i) => (
            <span key={i} style={{
              position: 'absolute', top: 0, left: `${nwPct(tk.min)}%`,
              transform: 'translateX(-50%)',
              fontFamily: M.fontMono, fontSize: 9, color: t.textFaint, whiteSpace: 'nowrap',
            }}>{tk.lab}</span>
          ))}
        </div>
        <div style={{ width: 40, flexShrink: 0 }}/>
      </div>

      {/* легенда */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginTop: 11 }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 14, height: 8, borderRadius: 2, background: routeCol, border: `1px solid ${hexToRgba(t.text, .18)}` }}/>
          <span style={{ fontSize: 11, color: t.textMuted }}>Маршрут</span>
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 14, height: 8, borderRadius: 2, background: t.accent }}/>
          <span style={{ fontSize: 11, color: t.textMuted }}>Ночные часы · 00:00–05:00</span>
        </span>
      </div>
    </div>
  );
}

Object.assign(window, { RouteNightWarn });
