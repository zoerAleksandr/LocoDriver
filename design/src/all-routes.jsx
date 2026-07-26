// «Все маршруты за месяц» — экран по тапу «Все (23)» с Главного.
// Плоский список маршрутов за выбранный месяц, переключение месяцев ‹ Май ›,
// фильтр и сортировка (шторки), переключатель плотности (компактно / развёрнуто).
// Карточка маршрута повторяет строку с Главного, но умеет разворачиваться:
// в развёрнутом виде показывает ВСЕ локомотивы (серия + номер) и ВСЕ поезда
// (номер + маршрут), а не только последний.
//
// Экспортирует: IOSScreenAllRoutes, AndroidScreenAllRoutes.

// Локальный конвертер цвета (в icons/ios-screens он не экспортирован).
function arRgba(hex, a = 1) {
  if (!hex) return `rgba(0,0,0,${a})`;
  if (hex.startsWith('rgb')) return hex;
  let h = hex.replace('#', '');
  if (h.length === 3) h = h.split('').map(c => c + c).join('');
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  return `rgba(${r},${g},${b},${a})`;
}

// Доплатные признаки → иконка (значки уже на window из icons.jsx).
const AR_ATTR = {
  holiday: IcHoliday, break: IcBreak, length: IcRuler, weight: IcWeight,
  shoulder: IcShoulder, solo: IcCrewSolo, passenger: IcPassenger, over12: IcMedal,
  pusher: IcPusher, double: IcDoubleTraction, coupled: IcCoupledTrain,
};

// ─────────────────────────────────────────────────────────────
// Данные месяца (демо). Плоский список, новые сверху.
// locos — серия+номер; trains — номер+маршрут; pass — следование пассажиром.
// ─────────────────────────────────────────────────────────────
const AR_MONTH = [
  { n: 23, d: '28.05', wd: 'Чт', span: '17:40 — 21:08', hours: '03:27', synced: false,
    locos: [['ВЛ10', '1456']],
    trains: [['№4404', 'Луга — СПбСМ']],
    pass: [['№8023', 'Новосибирск — Омск']],
    attrs: ['passenger', 'over12'] },
  { n: 22, d: '26.05', wd: 'Вт', span: '08:46 — 19:27', hours: '10:41', synced: true,
    locos: [['ВЛ10', '1456'], ['ВЛ10', '1457']],
    trains: [['№2712', 'Лужская — СПбСМ']],
    attrs: ['length'] },
  { n: 21, d: '24.05', wd: 'Вс', span: '20:10 — 04:55', hours: '08:45', synced: true,
    locos: [['2ТЭ116', '0204']],
    trains: [['№3601', 'Бабаево — Лужская'], ['№3602', 'Лужская — Бабаево']],
    attrs: ['weight', 'holiday'] },
  { n: 20, d: '22.05', wd: 'Пт', span: '06:30 — 14:12', hours: '07:42', synced: true,
    locos: [['ЭП2К', '0312']],
    trains: [['№6202', 'СПбГл — Тосно']],
    attrs: [] },
  { n: 19, d: '20.05', wd: 'Ср', span: '22:05 — 07:48', hours: '09:43', synced: true,
    locos: [['ВЛ10', '1456']],
    trains: [['№2901', 'СПбСМ — Волховстрой'], ['№2902', 'Волховстрой — СПбСМ']],
    attrs: ['solo', 'over12'] },
  { n: 18, d: '18.05', wd: 'Пн', span: '09:15 — 16:02', hours: '06:47', synced: true,
    locos: [['ЧС7', '0118']],
    trains: [['№7110', 'Луга — Гатчина']],
    attrs: [] },
  { n: 17, d: '16.05', wd: 'Сб', span: '19:30 — 05:18', hours: '09:48', synced: true,
    locos: [['2ЭС6', '0890'], ['2ЭС6', '0891']],
    trains: [['№1455', 'СПбСМ — Бабаево']],
    attrs: ['weight', 'double', 'holiday'] },
  { n: 16, d: '14.05', wd: 'Чт', span: '07:00 — 13:36', hours: '06:36', synced: true,
    locos: [['ЭП2К', '0312']],
    trains: [['№6204', 'СПбГл — Тосно']],
    pass: [['№6201', 'Тосно — СПбГл']],
    attrs: ['passenger'] },
  { n: 15, d: '12.05', wd: 'Вт', span: '21:40 — 06:25', hours: '08:45', synced: true,
    locos: [['ВЛ10', '1457']],
    trains: [['№2712', 'Лужская — СПбСМ']],
    attrs: ['length', 'solo'] },
  { n: 14, d: '10.05', wd: 'Вс', span: '08:20 — 15:05', hours: '06:45', synced: true,
    locos: [['2ТЭ116', '0204']],
    trains: [['№3601', 'Бабаево — Лужская']],
    attrs: ['holiday'] },
  { n: 13, d: '08.05', wd: 'Пт', span: '14:10 — 23:52', hours: '09:42', synced: true,
    locos: [['ЭП2К', '0312']],
    trains: [['№6206', 'СПбГл — Малая Вишера'], ['№6207', 'Малая Вишера — СПбГл']],
    attrs: ['over12'] },
  { n: 12, d: '05.05', wd: 'Вт', span: '06:45 — 12:18', hours: '05:33', synced: true,
    locos: [['ЧС7', '0118']],
    trains: [['№7110', 'Луга — Гатчина']],
    attrs: [] },
];

// Сводка месяца
const AR_SUMMARY = { count: 23, hours: '205:05' };

// Итог по плечу для развёрнутой карточки (демо-расчёт).
const arPay = '5 027,50 ₽';

// ════════════════════════════════════════════════════════════
// iOS
// ════════════════════════════════════════════════════════════
function IOSScreenAllRoutes({ dark = false, height = 1180, state = 'list', sheet = null, density = 'compact' }) {
  const t = dark ? M.dark : M.light;
  const empty = state === 'empty';
  const expandedAll = density === 'expanded';

  return (
    <MDevice dark={dark} height={height}>
      {sheet === 'filter' && <IOSFilterSheet t={t}/>}
      {sheet === 'sort' && <IOSSortSheet t={t}/>}

      <div style={{ height: '100%', display: 'flex', flexDirection: 'column', paddingTop: 54 }}>
        {/* Навбар */}
        <div style={{ padding: '8px 16px 6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0 }}>
          <button style={pillBtn(t, { w: 40, h: 40 })}><IcChevronLeft width="18" height="18"/></button>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>Маршруты</div>
          {/* Плотность карточек — компактно / развёрнуто */}
          <button style={pillBtn(t, { w: 40, h: 40, accent: expandedAll })} aria-label="Плотность">
            {expandedAll ? <ArCollapseRows width="18" height="18"/> : <ArExpandRows width="18" height="18"/>}
          </button>
        </div>

        {/* Переключатель месяца ‹ Май 2026 › */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: '4px 16px 12px', flexShrink: 0 }}>
          <button style={arMonthArrow(t)} aria-label="Предыдущий месяц"><IcChevronLeft width="16" height="16"/></button>
          <div style={{ textAlign: 'center', minWidth: 150 }}>
            <div style={{ fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -.3 }}>Май</div>
            <div style={{ fontSize: 11, color: t.textMuted, fontFamily: M.fontMono, letterSpacing: 1 }}>2026</div>
          </div>
          <button style={arMonthArrow(t)} aria-label="Следующий месяц"><IcChevronRight width="16" height="16"/></button>
        </div>

        {!empty && (
          /* Панель: счётчик · фильтр · сортировка · PDF */
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px 10px', flexShrink: 0 }}>
            <div style={{ fontSize: 13, color: t.textMuted }}>
              <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{AR_SUMMARY.count}</span> маршрута
              <span style={{ color: t.textFaint, margin: '0 6px' }}>·</span>
              <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{AR_SUMMARY.hours}</span>
            </div>
            <div style={{ flex: 1 }}/>
            <button style={arChip(t)}><ArFilter width="15" height="15"/> Фильтр</button>
            <button style={arChip(t)}><ArSort width="15" height="15"/> Дата</button>
            <button style={arIconChip(t)} aria-label="Экспорт в PDF"><IcPdf width="17" height="17"/></button>
          </div>
        )}

        {/* Список / пусто */}
        {empty ? (
          <ArEmpty t={t}/>
        ) : (
          <div style={{ flex: 1, overflowY: 'auto', padding: '2px 16px 32px' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {AR_MONTH.map((r) => (
                <IOSRouteCard key={r.n} t={t} r={r} defaultOpen={expandedAll}/>
              ))}
            </div>
            <div style={{ textAlign: 'center', padding: '18px 0 4px', fontSize: 12, color: t.textFaint, fontFamily: M.fontMono, letterSpacing: .5 }}>
              КОНЕЦ МЕСЯЦА
            </div>
          </div>
        )}
      </div>
    </MDevice>
  );
}

// Карточка маршрута (iOS) — компактная шапка + разворачиваемое тело.
function IOSRouteCard({ t, r, defaultOpen }) {
  const [open, setOpen] = React.useState(!!defaultOpen);
  React.useEffect(() => { setOpen(!!defaultOpen); }, [defaultOpen]);
  const lead = r.trains[0];

  return (
    <div style={{ background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm, overflow: 'hidden' }}>
      {/* Шапка — тап разворачивает */}
      <button onClick={() => setOpen(o => !o)} style={{
        width: '100%', textAlign: 'left', border: 'none', background: 'transparent', cursor: 'pointer',
        padding: '14px 16px 12px', display: 'flex', flexDirection: 'column', gap: 7,
      }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, minWidth: 0 }}>
            <span style={{ fontSize: 16, fontFamily: M.fontMono, color: t.text, fontWeight: 700, letterSpacing: -.2 }}>{r.d}</span>
            <span style={{ fontSize: 12, color: t.textFaint }}>{r.wd}</span>
            <span style={{ fontSize: 14, fontFamily: M.fontMono, color: t.textMuted }}>{r.span}</span>
          </div>
          <div style={{ fontFamily: M.fontMono, fontSize: 17, fontWeight: 700, color: t.text, whiteSpace: 'nowrap' }}>{r.hours}</div>
        </div>

        {!open && (
          <div style={{ fontSize: 15, color: t.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {lead[0]} {lead[1]}
            {r.trains.length > 1 && <span style={{ color: t.textMuted }}> +{r.trains.length - 1}</span>}
          </div>
        )}

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
          <div style={{ fontSize: 13, color: t.textMuted, fontFamily: M.fontMono }}>#{r.n}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {r.attrs.map(k => { const I = AR_ATTR[k]; return I ? <I key={k} width="18" height="18" style={{ color: t.text }}/> : null; })}
            <IcSync width="18" height="18" style={{ color: r.synced ? t.success : t.textFaint }}/>
            <ArChev width="16" height="16" style={{ color: t.textFaint, transform: open ? 'rotate(180deg)' : 'none', transition: 'transform .18s' }}/>
          </div>
        </div>
      </button>

      {/* Развёрнутое тело — все локомотивы и поезда */}
      {open && (
        <div style={{ padding: '0 16px 14px' }}>
          <div style={{ height: 1, background: t.border, margin: '2px 0 10px' }}/>
          <ArUnitGroup t={t} label="Локомотивы" items={r.locos.map(([s, n]) => ({ icon: <IcLocomotive width="18" height="18"/>, title: s, mono: n }))}/>
          <ArUnitGroup t={t} label="Поезда" items={r.trains.map(([n, route]) => ({ icon: <IcRails width="18" height="18"/>, title: n, sub: route }))}/>
          {r.pass && <ArUnitGroup t={t} label="Пассажиром" items={r.pass.map(([n, route]) => ({ icon: <IcPassenger width="18" height="18"/>, title: n, sub: route }))}/>}
          {/* Итог по плечу */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 12, padding: '11px 14px', background: t.bgSubtle, borderRadius: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: t.textMuted }}>
              <IcRuble width="16" height="16"/>
              <span style={{ fontSize: 13 }}>Расчёт за смену</span>
            </div>
            <div style={{ fontFamily: M.fontMono, fontSize: 15, fontWeight: 700, color: t.text }}>{arPay}</div>
          </div>
        </div>
      )}
    </div>
  );
}

// Группа единиц внутри развёрнутой карточки.
function ArUnitGroup({ t, label, items }) {
  if (!items || !items.length) return null;
  return (
    <div style={{ marginBottom: 4 }}>
      <div style={{ fontFamily: M.fontMono, fontSize: 10, color: t.textFaint, letterSpacing: 1.2, textTransform: 'uppercase', padding: '6px 2px 4px' }}>{label}</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {items.map((it, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '6px 2px' }}>
            <div style={{ width: 30, height: 30, borderRadius: 9, flexShrink: 0, background: t.bgSubtle, color: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{it.icon}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: t.text }}>
                {it.title}{it.mono && <span style={{ fontFamily: M.fontMono, color: t.textMuted, fontWeight: 500 }}> · {it.mono}</span>}
              </div>
              {it.sub && <div style={{ fontSize: 12, color: t.textMuted }}>{it.sub}</div>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// Пустой месяц (iOS)
function ArEmpty({ t }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 40px', textAlign: 'center', gap: 14 }}>
      <div style={{ width: 72, height: 72, borderRadius: 22, background: t.bgSubtle, color: t.textFaint, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <IcCalendar width="34" height="34"/>
      </div>
      <div>
        <div style={{ fontSize: 18, fontWeight: 700, color: t.text, marginBottom: 6 }}>За май маршрутов нет</div>
        <div style={{ fontSize: 14, color: t.textMuted, lineHeight: 1.45 }}>Маршруты появятся здесь, когда вы отметите смену. Полистайте соседние месяцы стрелками выше.</div>
      </div>
    </div>
  );
}

// Шторка «Фильтр» (iOS)
function IOSFilterSheet({ t }) {
  return (
    <ArSheet t={t} platform="ios" title="Фильтр" subtitle="Маршруты за май">
      <ArFilterBody t={t} platform="ios"/>
      <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
        <button style={{ flex: 1, padding: '14px', borderRadius: 14, border: `1px solid ${t.border}`, background: t.surface, color: t.text, fontSize: 15, fontWeight: 600, cursor: 'pointer' }}>Сбросить</button>
        <button style={{ flex: 2, padding: '14px', borderRadius: 14, border: 'none', background: t.cta || t.accent, color: t.ctaInk || t.accentInk, fontSize: 15, fontWeight: 700, cursor: 'pointer' }}>Показать 18</button>
      </div>
    </ArSheet>
  );
}

// Шторка «Сортировка» (iOS)
function IOSSortSheet({ t }) {
  return (
    <ArSheet t={t} platform="ios" title="Сортировка">
      <ArSortBody t={t} platform="ios"/>
    </ArSheet>
  );
}

// ════════════════════════════════════════════════════════════
// Android
// ════════════════════════════════════════════════════════════
function AndroidScreenAllRoutes({ dark = false, height = 1180, state = 'list', sheet = null, density = 'compact' }) {
  const t = dark ? M.dark : M.light;
  const empty = state === 'empty';
  const expandedAll = density === 'expanded';

  return (
    <ADevice dark={dark} height={height}>
      {sheet === 'filter' && <AndroidFilterSheet t={t}/>}
      {sheet === 'sort' && <AndroidSortSheet t={t}/>}

      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Top app bar */}
      <div style={{ padding: '4px 4px 0', display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0, background: t.bg }}>
        <button style={arMatIcon(t)}><IcChevronLeft width="22" height="22"/></button>
        <div style={{ flex: 1, fontSize: 20, fontWeight: 500, color: t.text }}>Маршруты</div>
        <button style={arMatIcon(t, expandedAll)} aria-label="Плотность">
          {expandedAll ? <ArCollapseRows width="22" height="22"/> : <ArExpandRows width="22" height="22"/>}
        </button>
        <button style={arMatIcon(t)} aria-label="Экспорт в PDF"><IcPdf width="21" height="21"/></button>
      </div>

      {/* Месяц ‹ Май 2026 › */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 12px 8px', flexShrink: 0 }}>
        <button style={arMatIcon(t)} aria-label="Предыдущий месяц"><IcChevronLeft width="22" height="22"/></button>
        <div style={{ fontSize: 22, fontWeight: 700, color: t.text, letterSpacing: -.4 }}>
          Май <span style={{ color: t.textMuted, fontWeight: 500 }}>2026</span>
        </div>
        <button style={arMatIcon(t)} aria-label="Следующий месяц"><IcChevronRight width="22" height="22"/></button>
      </div>

      {!empty && (
        /* Material assist chips: счётчик + фильтр + сортировка */
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px 10px', flexShrink: 0 }}>
          <button style={arMatChip(t)}><ArFilter width="16" height="16"/> Фильтр</button>
          <button style={arMatChip(t)}><ArSort width="16" height="16"/> Дата</button>
          <div style={{ flex: 1 }}/>
          <div style={{ fontSize: 12.5, color: t.textMuted, fontFamily: M.fontMono }}>
            {AR_SUMMARY.count} · {AR_SUMMARY.hours}
          </div>
        </div>
      )}

      {empty ? (
        <ArEmpty t={t}/>
      ) : (
        <div style={{ flex: 1, overflowY: 'auto', padding: '2px 16px 28px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {AR_MONTH.map((r) => (
              <AndroidRouteCard key={r.n} t={t} r={r} defaultOpen={expandedAll}/>
            ))}
          </div>
          <div style={{ textAlign: 'center', padding: '16px 0 4px', fontSize: 11, color: t.textFaint, fontFamily: M.fontMono, letterSpacing: 1 }}>
            КОНЕЦ МЕСЯЦА
          </div>
        </div>
      )}
      </div>
    </ADevice>
  );
}

// Карточка маршрута (Android / Material)
function AndroidRouteCard({ t, r, defaultOpen }) {
  const [open, setOpen] = React.useState(!!defaultOpen);
  React.useEffect(() => { setOpen(!!defaultOpen); }, [defaultOpen]);
  const lead = r.trains[0];

  return (
    <div style={{ background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, overflow: 'hidden' }}>
      <button onClick={() => setOpen(o => !o)} style={{
        width: '100%', textAlign: 'left', border: 'none', background: 'transparent', cursor: 'pointer',
        padding: '14px 16px 12px', display: 'flex', flexDirection: 'column', gap: 7,
      }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, minWidth: 0 }}>
            <span style={{ fontSize: 15, fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{r.d}</span>
            <span style={{ fontSize: 12, color: t.textFaint }}>{r.wd}</span>
            <span style={{ fontSize: 13.5, fontFamily: M.fontMono, color: t.textMuted }}>{r.span}</span>
          </div>
          <div style={{ fontFamily: M.fontMono, fontSize: 16, fontWeight: 700, color: t.text, whiteSpace: 'nowrap' }}>{r.hours}</div>
        </div>

        {!open && (
          <div style={{ fontSize: 14, color: t.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {lead[0]} {lead[1]}
            {r.trains.length > 1 && <span style={{ color: t.textMuted }}> +{r.trains.length - 1}</span>}
          </div>
        )}

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
          <div style={{ fontSize: 12, color: t.textMuted, fontFamily: M.fontMono }}>№{r.n}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {r.attrs.map(k => { const I = AR_ATTR[k]; return I ? <I key={k} width="18" height="18" style={{ color: t.text }}/> : null; })}
            <IcSync width="18" height="18" style={{ color: r.synced ? t.success : t.textFaint }}/>
            <ArChev width="16" height="16" style={{ color: t.textFaint, transform: open ? 'rotate(180deg)' : 'none', transition: 'transform .18s' }}/>
          </div>
        </div>
      </button>

      {open && (
        <div style={{ padding: '0 16px 14px' }}>
          <div style={{ height: 1, background: t.border, margin: '2px 0 10px' }}/>
          <ArUnitGroup t={t} label="Локомотивы" items={r.locos.map(([s, n]) => ({ icon: <IcLocomotive width="18" height="18"/>, title: s, mono: n }))}/>
          <ArUnitGroup t={t} label="Поезда" items={r.trains.map(([n, route]) => ({ icon: <IcRails width="18" height="18"/>, title: n, sub: route }))}/>
          {r.pass && <ArUnitGroup t={t} label="Пассажиром" items={r.pass.map(([n, route]) => ({ icon: <IcPassenger width="18" height="18"/>, title: n, sub: route }))}/>}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 12, padding: '11px 14px', background: t.bgSubtle, borderRadius: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: t.textMuted }}>
              <IcRuble width="16" height="16"/>
              <span style={{ fontSize: 13 }}>Расчёт за смену</span>
            </div>
            <div style={{ fontFamily: M.fontMono, fontSize: 15, fontWeight: 700, color: t.text }}>{arPay}</div>
          </div>
        </div>
      )}
    </div>
  );
}

// Шторки Android
function AndroidFilterSheet({ t }) {
  return (
    <ArSheet t={t} platform="android" title="Фильтр" subtitle="Маршруты за май">
      <ArFilterBody t={t} platform="android"/>
      <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
        <button style={{ flex: 1, padding: '13px', borderRadius: 999, border: `1px solid ${t.borderStrong}`, background: 'transparent', color: t.accent, fontSize: 15, fontWeight: 600, cursor: 'pointer' }}>Сбросить</button>
        <button style={{ flex: 2, padding: '13px', borderRadius: 999, border: 'none', background: t.accent, color: t.accentInk, fontSize: 15, fontWeight: 700, cursor: 'pointer' }}>Показать 18</button>
      </div>
    </ArSheet>
  );
}
function AndroidSortSheet({ t }) {
  return (
    <ArSheet t={t} platform="android" title="Сортировка">
      <ArSortBody t={t} platform="android"/>
    </ArSheet>
  );
}

// ─────────────────────────────────────────────────────────────
// Общие части шторок
// ─────────────────────────────────────────────────────────────
// Чипы-фильтры (как в референсе): «Все» + признаки маршрута. Иконка + подпись.
const AR_FILTER_CHIPS = [
  { key: 'fav',      label: 'Избранные',         icon: IcHeart,          tint: 'danger' },
  { key: 'weight',   label: 'Тяжелые',           icon: IcWeight },
  { key: 'shoulder', label: 'Удлинённые плечи',  icon: IcShoulder },
  { key: 'reserve',  label: 'Резервом',          icon: IcLocomotive },
  { key: 'solo',     label: 'Одно лицо',         icon: IcCrewSolo },
  { key: 'over12',   label: 'Свыше 12ч',         icon: IcMedal },
  { key: 'length',   label: 'Длинные поезда',    icon: IcRuler },
  { key: 'break',    label: 'С перерывами',      icon: IcBreak },
  { key: 'pusher',   label: 'Толкач',            icon: IcPusher },
  { key: 'double',   label: 'Двойная тяга',      icon: IcDoubleTraction },
  { key: 'coupled',  label: 'Сдвоенный',         icon: IcCoupledTrain },
];

function ArFilterBody({ t, platform }) {
  const android = platform === 'android';
  const [sel, setSel] = React.useState(new Set());        // выбранные признаки
  const all = sel.size === 0;
  const toggle = (k) => setSel(prev => {
    const next = new Set(prev);
    next.has(k) ? next.delete(k) : next.add(k);
    return next;
  });

  const Chip = ({ active, onClick, children }) => (
    <button onClick={onClick} style={{
      display: 'inline-flex', alignItems: 'center', gap: 8,
      height: 42, padding: '0 16px', borderRadius: 999, cursor: 'pointer',
      border: active ? 'none' : `1px solid ${t.borderStrong}`,
      background: active ? (android ? t.accent : t.cta) : 'transparent',
      color: active ? (android ? t.accentInk : t.ctaInk) : t.text,
      fontSize: 15, fontWeight: 600, whiteSpace: 'nowrap',
    }}>{children}</button>
  );

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
      <Chip active={all} onClick={() => setSel(new Set())}>Все</Chip>
      {AR_FILTER_CHIPS.map(f => {
        const I = f.icon;
        const active = sel.has(f.key);
        const iconColor = active ? 'currentColor' : (f.tint === 'danger' ? t.danger : t.text);
        return (
          <Chip key={f.key} active={active} onClick={() => toggle(f.key)}>
            <I width="18" height="18" style={{ color: iconColor }}/>
            {f.label}
          </Chip>
        );
      })}
    </div>
  );
}

// Сортировка — radio-список (как в референсе).
const AR_SORTS = [
  ['Старые', 'date-asc'],
  ['Новые', 'date-desc'],
  ['Мало часов на работе', 'dur-asc'],
  ['Много часов на работе', 'dur-desc'],
];
function ArSortBody({ t, platform }) {
  const [sel, setSel] = React.useState('date-desc');
  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      {AR_SORTS.map(([label, key]) => {
        const on = sel === key;
        return (
          <button key={key} onClick={() => setSel(key)} style={{
            display: 'flex', alignItems: 'center', gap: 16, padding: '15px 6px',
            border: 'none', background: 'transparent', cursor: 'pointer', textAlign: 'left',
          }}>
            <ArRadio t={t} on={on}/>
            <div style={{ flex: 1, fontSize: 17, fontWeight: on ? 600 : 400, color: t.text }}>{label}</div>
          </button>
        );
      })}
    </div>
  );
}

// Material/iOS radio — кольцо + внутренняя точка.
function ArRadio({ t, on }) {
  return (
    <div style={{
      width: 22, height: 22, borderRadius: 11, flexShrink: 0,
      border: `2px solid ${on ? t.accent : t.borderStrong}`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      {on && <div style={{ width: 11, height: 11, borderRadius: 6, background: t.accent }}/>}
    </div>
  );
}

// Checkbox — iOS (галочка в круге) / Android (квадрат)
function ArCheckbox({ t, on, android }) {
  if (android) {
    return (
      <div style={{
        width: 22, height: 22, borderRadius: 6, flexShrink: 0,
        background: on ? t.accent : 'transparent',
        border: on ? 'none' : `2px solid ${t.borderStrong}`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{on && <IcCheck width="15" height="15" style={{ color: t.accentInk }}/>}</div>
    );
  }
  return (
    <div style={{
      width: 24, height: 24, borderRadius: 12, flexShrink: 0,
      background: on ? t.accent : 'transparent',
      border: on ? 'none' : `1.5px solid ${t.borderStrong}`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>{on && <IcCheck width="15" height="15" style={{ color: t.accentInk }}/>}</div>
  );
}

// Унифицированная нижняя шторка (iOS / Android)
function ArSheet({ t, platform, title, subtitle, children }) {
  const android = platform === 'android';
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '88%',
        background: t.bg, borderRadius: android ? '28px 28px 0 0' : '24px 24px 0 0',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.28)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: android ? 12 : 10 }}>
          <div style={{ width: android ? 32 : 40, height: android ? 4 : 5, borderRadius: 3, background: t.borderStrong }}/>
        </div>
        <div style={{ padding: '12px 20px 8px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontSize: android ? 22 : 20, fontWeight: android ? 600 : 700, color: t.text, letterSpacing: -.3 }}>{title}</div>
            {subtitle && <div style={{ fontSize: 13, color: t.textMuted, marginTop: 2 }}>{subtitle}</div>}
          </div>
          {!android && (
            <button style={{ width: 30, height: 30, borderRadius: 15, border: 'none', cursor: 'pointer', background: t.bgSubtle, color: t.textMuted, fontSize: 15, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>✕</button>
          )}
        </div>
        <div style={{ overflowY: 'auto', padding: '8px 16px 28px' }}>{children}</div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Мелкие стили-хелперы и локальные иконки
// ─────────────────────────────────────────────────────────────
function arMonthArrow(t) {
  return {
    width: 36, height: 36, borderRadius: 18, border: 'none', cursor: 'pointer',
    background: t.surface, boxShadow: M.shadow.sm, color: t.text,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  };
}
function arChip(t) {
  return {
    display: 'inline-flex', alignItems: 'center', gap: 6, height: 34, padding: '0 12px',
    borderRadius: 999, border: `1px solid ${t.border}`, background: t.surface, boxShadow: M.shadow.sm,
    color: t.text, fontSize: 13, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap',
  };
}
function arIconChip(t) {
  return {
    width: 34, height: 34, borderRadius: 999, border: `1px solid ${t.border}`, background: t.surface,
    boxShadow: M.shadow.sm, color: t.text, cursor: 'pointer',
    display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
  };
}
function arMatIcon(t, active) {
  return {
    width: 44, height: 44, border: 'none', cursor: 'pointer', borderRadius: 22,
    background: active ? t.accentSoft : 'transparent', color: active ? t.accent : t.text,
    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
  };
}
function arMatChip(t) {
  return {
    display: 'inline-flex', alignItems: 'center', gap: 7, height: 36, padding: '0 14px',
    borderRadius: 10, border: `1px solid ${t.borderStrong}`, background: 'transparent',
    color: t.text, fontSize: 14, fontWeight: 500, cursor: 'pointer', whiteSpace: 'nowrap',
  };
}

// Иконки, которых нет в общем наборе
function ArFilter(p) {
  return (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M3 5h18l-7 8v6l-4 2v-8L3 5z"/></svg>);
}
function ArSort(p) {
  return (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M4 7h12M4 12h8M4 17h4M17 16l3 3 3-3M20 5v14"/></svg>);
}
function ArChev(p) {
  return (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M6 9l6 6 6-6"/></svg>);
}
function ArExpandRows(p) {
  return (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M8 4l4-2 4 2M8 20l4 2 4-2M3 9h18M3 14h18"/></svg>);
}
function ArCollapseRows(p) {
  return (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" {...p}><path d="M8 2l4 2 4-2M8 22l4-2 4 2M3 9h18M3 14h18"/></svg>);
}

Object.assign(window, { IOSScreenAllRoutes, AndroidScreenAllRoutes });
