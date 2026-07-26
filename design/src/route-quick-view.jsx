// route-quick-view.jsx — Быстрый просмотр маршрута (long-press peek, в духе Telegram)
// =====================================================================
// Долгое нажатие на маршрут в списке → фон затемняется и размывается,
// карточка маршрута «поднимается» в диалог со всей подробной информацией,
// а ниже появляется контекстное меню действий (Редактировать / Поделиться /
// Копировать / Удалить).
//
// Экспорт: IOSRouteQuickView({ dark, platform, height })
// =====================================================================

// ── Насыщенный пример маршрута №147 ──────────────────────────────────
const QV_ROUTE = {
  num: '147',
  from: 'Иланская',
  to: 'Боготол',
  work: '11:32',
  pay: '6 184,20 ₽',
  rest: { mode: 'home', type: 'Домашний', dur: '18:40', until: '16.06 · 03:52' },
  synced: false,
  arrival: { date: '14 июн 2026', time: '21:40' },
  handover: { date: '15 июн 2026', time: '09:12' },

  locos: [
    {
      title: '3ЭС5К-218',
      meters: [
        { label: 'Расход', a: '1 283 456', b: '1 288 276', total: '4 820', unit: 'кВт·ч' },
        { label: 'Рекуперация', a: '412 300', b: '413 540', total: '1 240', unit: 'кВт·ч', tone: 'pos' },
      ],
    },
    {
      title: '2ТЭ116У-0345',
      meters: [
        { label: 'Топливо', a: '3 200', b: '2 020', total: '1 180', unit: 'кг' },
      ],
      net: { label: 'Расход', value: '1 180', unit: 'кг' },
    },
  ],

  trains: [
    {
      num: '№2654', leg: 'Иланская — Красноярск', kind: '71 ваг · 5 800 т · 84 у.д.',
      stops: [
        { name: 'Иланская', dep: '22:10', first: true },
        { name: 'Канск-Енисейский', arr: '23:48', dep: '00:06' },
        { name: 'Заозёрная', arr: '01:32', dep: '02:10' },
        { name: 'Уяр', arr: '02:58', dep: '03:12' },
        { name: 'Красноярск', arr: '05:40', last: true },
      ],
    },
    {
      num: '№3208', leg: 'Красноярск — Боготол', kind: '65 ваг · 5 200 т · 78 у.д.',
      stops: [
        { name: 'Красноярск', dep: '06:25', first: true },
        { name: 'Минино', arr: '06:58', dep: '07:10' },
        { name: 'Кача', arr: '07:40', dep: '07:52' },
        { name: 'Чернореченская', arr: '08:34', dep: '08:46' },
        { name: 'Боготол', arr: '09:05', last: true },
      ],
    },
  ],

  pass: {
    num: '№118', leg: 'Боготол — Ачинск', note: 'возвращение домой',
    stops: [
      { name: 'Боготол', dep: '09:40', first: true },
      { name: 'Критово', arr: '10:12', dep: '10:14' },
      { name: 'Ачинск', arr: '11:08', last: true },
    ],
  },
  attrs: [
    ['length', 'Повышенная длина'],
    ['weight', 'Повышенная масса'],
    ['over12', 'Свыше 12 часов'],
    ['shoulder', 'Удлинённое плечо'],
  ],
  note: 'Предупреждение ДСП по ст. Кравченко: ограничение 40 км/ч, 812–814 км. Смена бригады на ст. Красноярск.',
};

// Отдых в пункте оборота — короткий и полный (как в шторке «Отдых», mode='po')
const QV_REST_PO = {
  mode: 'po', station: 'Боготол',
  short: { dur: '4:00', until: '15.06 · 13:05' },
  full:  { dur: '8:00', until: '15.06 · 17:05' },
};

const QV_ATTR_ICON = {
  length: IcRuler, weight: IcWeight, over12: IcMedal, shoulder: IcShoulder,
};

// ── Фоновый «призрак» списка маршрутов (под размытием) ───────────────
function QVGhostList({ t }) {
  const Row = ({ date, route, hours, dim }) => (
    <div style={{ padding: '14px 20px', display: 'flex', flexDirection: 'column', gap: 6, opacity: dim ? 0.5 : 1 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
        <div style={{ fontSize: 16, fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{date}</div>
        <div style={{ fontFamily: M.fontMono, fontSize: 17, fontWeight: 700, color: t.text }}>{hours}</div>
      </div>
      <div style={{ fontSize: 15, color: t.text }}>{route}</div>
      <div style={{ height: 14 }}/>
    </div>
  );
  return (
    <div style={{ padding: '64px 16px 0' }}>
      <div style={{ fontSize: 20, fontWeight: 700, color: t.text, padding: '12px 4px' }}>Маршруты</div>
      <div style={{ background: t.surface, borderRadius: 18, overflow: 'hidden', boxShadow: M.shadow.sm }}>
        <Row date="15.06 21:40 — 09:12" route="№2654 Иланская — Боготол" hours="11:32"/>
        <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>
        <Row date="11.06 06:12 — 15:48" route="№4404 Луга — СПбСМ" hours="09:36" dim/>
        <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>
        <Row date="06.06 08:46 — 19:27" route="№2712 Лужская — СПбСМ" hours="10:41" dim/>
      </div>
    </div>
  );
}

// ── Компактная стат-плашка (Время работы / Отдых) ────────────────────
function QVStat({ t, icon, label, value, sub, until }) {
  return (
    <div style={{ flex: 1, background: t.surface, borderRadius: 12, padding: '10px 13px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: t.textMuted, marginBottom: 5 }}>
        {icon}
        <span style={{ fontFamily: M.fontMono, fontSize: 9.5, letterSpacing: 0.8, textTransform: 'uppercase' }}>{label}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
        <span style={{ fontFamily: M.fontMono, fontSize: 22, fontWeight: 800, color: t.text, letterSpacing: -.5 }}>{value}</span>
        {sub && <span style={{ fontSize: 12, color: t.textMuted, fontWeight: 500 }}>{sub}</span>}
      </div>
      {until && <div style={{ fontSize: 11, color: t.textMuted, fontWeight: 500, marginTop: 3 }}>до <span style={{ fontFamily: M.fontMono, color: t.text }}>{until}</span></div>}
    </div>
  );
}

// ── Стат-плашка отдыха в ПО (короткий + полный) ──────────────────────
function QVRestStatPO({ t, rest }) {
  return (
    <div style={{ flex: 1.25, background: t.surface, borderRadius: 12, padding: '10px 13px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: t.textMuted, marginBottom: 5 }}>
        <IcHome width="13" height="13"/>
        <span style={{ fontFamily: M.fontMono, fontSize: 9.5, letterSpacing: 0.8, textTransform: 'uppercase' }}>Отдых в ПО</span>
      </div>
      <div style={{ display: 'flex', gap: 14 }}>
        <div>
          <div style={{ fontFamily: M.fontMono, fontSize: 18, fontWeight: 800, color: t.text, letterSpacing: -.5 }}>{rest.short.dur}</div>
          <div style={{ fontSize: 10.5, color: t.textMuted, fontWeight: 500 }}>короткий</div>
          <div style={{ fontSize: 10, color: t.textMuted, fontWeight: 500, marginTop: 2 }}>до <span style={{ fontFamily: M.fontMono, color: t.text }}>{rest.short.until}</span></div>
        </div>
        <div style={{ width: 1, background: t.border }}/>
        <div>
          <div style={{ fontFamily: M.fontMono, fontSize: 18, fontWeight: 800, color: t.accent, letterSpacing: -.5 }}>{rest.full.dur}</div>
          <div style={{ fontSize: 10.5, color: t.textMuted, fontWeight: 500 }}>полный</div>
          <div style={{ fontSize: 10, color: t.textMuted, fontWeight: 500, marginTop: 2 }}>до <span style={{ fontFamily: M.fontMono, color: t.text }}>{rest.full.until}</span></div>
        </div>
      </div>
    </div>
  );
}

// ── Заголовок-«поднятая карточка» внутри диалога ─────────────────────
function QVHeader({ t, rest }) {
  const r = QV_ROUTE;
  return (
    <div style={{ padding: '18px 16px 16px', background: t.accentSoft, borderBottom: `1px solid ${t.border}` }}>
      {/* № + направление */}
      <div style={{ padding: '0 4px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 13, color: t.textMuted }}>Маршрут</span>
          <span style={{ fontFamily: M.fontMono, fontSize: 19, fontWeight: 700, color: t.text }}>№{r.num}</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6, fontSize: 17, fontWeight: 600, color: t.text }}>
          <span>{r.from}</span>
          <IcArrowDown width="14" height="14" style={{ transform: 'rotate(-90deg)', color: t.accent }}/>
          <span>{r.to}</span>
        </div>
      </div>

      {/* Время работы + Отдых — рядом */}
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <QVStat t={t} icon={<IcWorkClock width="13" height="13"/>} label="Время работы" value={r.work}/>
        {rest.mode === 'po'
          ? <QVRestStatPO t={t} rest={rest}/>
          : <QVStat t={t} icon={<IcHome width="13" height="13"/>} label="Отдых" value={rest.dur} sub={rest.type} until={rest.until}/>}
      </div>

      {/* Заработок + синхронизация */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8 }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, background: t.surface, borderRadius: 9, padding: '6px 11px', fontSize: 13, fontWeight: 600, color: t.text }}>
          <IcRuble width="14" height="14" style={{ color: t.accent }}/> {r.pay}
        </span>
        {r.synced
          ? <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, background: t.surface, borderRadius: 9, padding: '6px 11px', fontSize: 13, fontWeight: 500, color: t.success }}>
              <IcSync width="14" height="14"/> Синхронизирован
            </span>
          : <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, background: t.surface, borderRadius: 9, padding: '6px 11px', fontSize: 13, fontWeight: 500, color: t.textMuted }}>
              <IcCloudOff width="14" height="14"/> Не синхронизирован
            </span>}
      </div>
    </div>
  );
}

// ── Подпись секции внутри диалога ────────────────────────────────────
function QVLabel({ t, children }) {
  return (
    <div style={{ fontFamily: M.fontMono, fontSize: 10.5, color: t.textMuted, letterSpacing: 1.3, padding: '16px 20px 7px', textTransform: 'uppercase' }}>{children}</div>
  );
}

// ── Строка явки/сдачи ────────────────────────────────────────────────
function QVTimeRow({ t, label, date, time, tag, last }) {
  return (
    <div style={{ padding: '11px 20px', borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 7 }}>
        <span style={{ fontSize: 13, color: t.textMuted }}>{label}</span>
        {tag && <span style={{ fontFamily: M.fontMono, fontSize: 9, fontWeight: 700, letterSpacing: 0.6, color: t.accent, background: t.accentSoft, padding: '3px 7px', borderRadius: 6, textTransform: 'uppercase' }}>{tag}</span>}
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <div style={{ background: t.bgSubtle, padding: '7px 13px', borderRadius: 10, fontSize: 15, fontFamily: M.fontMono, color: t.text, fontWeight: 500 }}>{date}</div>
        <div style={{ background: t.bgSubtle, padding: '7px 13px', borderRadius: 10, fontSize: 15, fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{time}</div>
      </div>
    </div>
  );
}

// ── Счётчик локомотива: принял → сдал + итог ─────────────────────────
function QVMeter({ t, label, a, b, total, unit, tone }) {
  const totalColor = tone === 'pos' ? t.success : t.text;
  return (
    <div style={{ padding: '9px 0' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 10 }}>
        <span style={{ fontSize: 13, color: t.textMuted }}>{label}</span>
        <span style={{ fontFamily: M.fontMono, fontSize: 15, fontWeight: 700, color: totalColor }}>
          {tone === 'pos' ? '−' : ''}{total} <span style={{ fontSize: 11, color: t.textMuted, fontWeight: 500 }}>{unit}</span>
        </span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 5 }}>
        <span style={{ flex: 1, background: t.bgSubtle, borderRadius: 8, padding: '6px 10px', fontFamily: M.fontMono, fontSize: 13.5, color: t.text, textAlign: 'center' }}>{a}</span>
        <IcArrowDown width="13" height="13" style={{ transform: 'rotate(-90deg)', color: t.textFaint, flexShrink: 0 }}/>
        <span style={{ flex: 1, background: t.bgSubtle, borderRadius: 8, padding: '6px 10px', fontFamily: M.fontMono, fontSize: 13.5, color: t.text, textAlign: 'center' }}>{b}</span>
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, padding: '0 2px', fontSize: 9.5, color: t.textFaint, fontFamily: M.fontMono, letterSpacing: 0.5, textTransform: 'uppercase' }}>
        <span>принял</span><span>сдал</span>
      </div>
    </div>
  );
}

// ── Карточка локомотива со счётчиками ────────────────────────────────
function QVLoco({ t, loco, last }) {
  return (
    <div style={{ padding: '14px 18px', borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 36, height: 36, borderRadius: 10, background: t.bgSubtle, color: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <IcLocomotive width="20" height="20"/>
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{loco.title}</div>
        </div>
      </div>

      <div style={{ marginTop: 6 }}>
        {loco.meters.map((m, i) => (
          <React.Fragment key={m.label}>
            {i > 0 && <div style={{ height: 1, background: t.border, margin: '2px 0' }}/>}
            <QVMeter t={t} {...m}/>
          </React.Fragment>
        ))}
      </div>

      {/* Итог */}
      {loco.net && (
        <div style={{ marginTop: 8, display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', background: t.accentSoft, borderRadius: 10, padding: '9px 13px' }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: t.text }}>{loco.net.label}</span>
          <span style={{ fontFamily: M.fontMono, fontSize: 18, fontWeight: 800, color: t.text, letterSpacing: -.5 }}>
            {loco.net.value} <span style={{ fontSize: 12, color: t.textMuted, fontWeight: 500 }}>{loco.net.unit}</span>
          </span>
        </div>
      )}
    </div>
  );
}

// ── Минуты стоянки между прибытием и отправлением ────────────────────
function qvStopMin(arr, dep) {
  const [ah, am] = arr.split(':').map(Number);
  const [dh, dm] = dep.split(':').map(Number);
  let mins = (dh * 60 + dm) - (ah * 60 + am);
  if (mins < 0) mins += 24 * 60;
  return mins;
}

// ── Строка отдыха в ПО (короткий / полный) ───────────────────────────
function QVRestRow({ t, label, dur, until, accent, last }) {
  return (
    <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12, borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{label}</div>
        <div style={{ fontSize: 12, color: t.textMuted, marginTop: 2 }}>до <span style={{ fontFamily: M.fontMono, color: t.text }}>{until}</span></div>
      </div>
      <div style={{ fontFamily: M.fontMono, fontSize: 18, fontWeight: 800, color: accent ? t.accent : t.text, letterSpacing: -.5 }}>{dur}</div>
    </div>
  );
}

// ── Строка станции в графике поезда ──────────────────────────────────
function QVStation({ t, stop, last }) {
  const accent = stop.first || stop.last;
  return (
    <div style={{ display: 'flex', gap: 12, position: 'relative' }}>
      {/* Рельс с точкой */}
      <div style={{ position: 'relative', width: 12, flexShrink: 0, alignSelf: 'stretch' }}>
        <div style={{ position: 'absolute', left: 5, top: stop.first ? 16 : 0, bottom: stop.last ? 'calc(100% - 16px)' : 0, width: 2, background: t.border }}/>
        <div style={{
          position: 'absolute', left: 1, top: 11, width: 10, height: 10, borderRadius: 5,
          background: accent ? t.accent : t.surface, border: `2px solid ${accent ? t.accent : t.borderStrong}`,
        }}/>
      </div>
      {/* Название + времена */}
      <div style={{ flex: 1, minWidth: 0, display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 10, padding: '7px 0', borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
        <div style={{ minWidth: 0 }}>
          <span style={{ display: 'block', fontSize: 14, fontWeight: accent ? 600 : 500, color: t.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{stop.name}</span>
          {stop.arr && stop.dep && (() => {
            const mn = qvStopMin(stop.arr, stop.dep);
            const tone = mn > 30 ? { c: t.danger, bg: hexToRgba(t.danger, 0.13) }
                        : mn > 5 ? { c: t.warning, bg: hexToRgba(t.warning, 0.16) }
                        : { c: t.textMuted, bg: 'transparent' };
            return <span style={{ fontFamily: M.fontMono, fontSize: 10.5, fontWeight: 600, color: tone.c, background: tone.bg, padding: tone.bg === 'transparent' ? '2px 0' : '2px 7px', borderRadius: 6, marginTop: 3, display: 'inline-block' }}>стоянка · {mn} мин</span>;
          })()}
        </div>
        <div style={{ flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'flex-end' }}>
          {stop.arr && <span style={{ fontFamily: M.fontMono, fontSize: 12.5, color: t.text }}><span style={{ color: t.textFaint, fontSize: 10 }}>приб </span>{stop.arr}</span>}
          {stop.dep && <span style={{ fontFamily: M.fontMono, fontSize: 12.5, color: t.text }}><span style={{ color: t.textFaint, fontSize: 10 }}>отпр </span>{stop.dep}</span>}
        </div>
      </div>
    </div>
  );
}

// ── Карточка поезда: шапка + полный график станций ───────────────────
function QVTrain({ t, train, last }) {
  return (
    <div style={{ padding: '14px 18px 12px', borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 36, height: 36, borderRadius: 10, background: t.bgSubtle, color: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          {train.icon === 'pass' ? <IcPassenger width="20" height="20"/> : <IcRails width="20" height="20"/>}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{train.num}</div>
          <div style={{ fontSize: 12, color: t.textMuted, marginTop: 2 }}>{train.leg}</div>
        </div>
      </div>
      {train.kind && <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, margin: '8px 0 4px', paddingLeft: 2 }}>{train.kind}</div>}
      {/* График следования */}
      <div style={{ marginTop: 4 }}>
        {train.stops.map((s, i) => (
          <QVStation key={s.name} t={t} stop={s} last={i === train.stops.length - 1}/>
        ))}
      </div>
    </div>
  );
}

// ── Универсальная строка единицы (пассажиром) ────────────────────────
function QVUnitRow({ t, icon, title, sub, badge, last }) {
  return (
    <div style={{ padding: '12px 20px', display: 'flex', alignItems: 'center', gap: 12, borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
      <div style={{ width: 36, height: 36, borderRadius: 10, background: t.bgSubtle, color: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>{icon}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
          <span style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{title}</span>
          {badge && <span style={{ fontFamily: M.fontMono, fontSize: 9, fontWeight: 700, letterSpacing: 0.6, color: t.accent, background: t.accentSoft, padding: '2px 6px', borderRadius: 5, textTransform: 'uppercase' }}>{badge}</span>}
        </div>
        {sub && <div style={{ fontSize: 12, color: t.textMuted, marginTop: 2 }}>{sub}</div>}
      </div>
    </div>
  );
}

// ── Контекстное меню (iOS-style action list) ─────────────────────────
function QVMenu({ t, synced = true }) {
  const Item = ({ icon, label, danger, last }) => (
    <button style={{
      width: '100%', border: 'none', background: 'transparent', cursor: 'pointer',
      padding: '15px 18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      borderBottom: last ? 'none' : `1px solid ${t.border}`, color: danger ? t.danger : t.text,
    }}>
      <span style={{ fontSize: 16, fontWeight: 500 }}>{label}</span>
      <span style={{ display: 'flex', color: danger ? t.danger : t.textMuted }}>{icon}</span>
    </button>
  );
  return (
    <div style={{
      background: t.elevated || t.surface, borderRadius: 16, overflow: 'hidden',
      boxShadow: '0 18px 48px rgba(0,0,0,0.30), 0 2px 6px rgba(0,0,0,0.10)',
      border: `1px solid ${t.border}`, backdropFilter: 'blur(20px)',
    }}>
      <Item icon={<IcPencil width="20" height="20"/>} label="Редактировать"/>
      <Item icon={<IcShare width="20" height="20"/>} label="Поделиться"/>
      {!synced && <Item icon={<IcSync width="20" height="20"/>} label="Синхронизировать"/>}
      <Item icon={<IcHeart width="20" height="20"/>} label="В Избранное"/>
      <Item icon={<IcCopy width="20" height="20"/>} label="Копировать"/>
      <Item icon={<IcTrash width="20" height="20"/>} label="Удалить" danger last/>
    </div>
  );
}

// ── Экран целиком ────────────────────────────────────────────────────
function IOSRouteQuickView({ dark = false, platform = 'ios', height = 844, rest = QV_ROUTE.rest }) {
  const t = dark ? M.dark : M.light;
  const r = QV_ROUTE;
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      {/* Фоновый список — размыт и затемнён */}
      <div style={{ position: 'absolute', inset: 0, filter: 'blur(3px)', transform: 'scale(1.02)', pointerEvents: 'none' }}>
        <QVGhostList t={t}/>
      </div>
      <div style={{ position: 'absolute', inset: 0, background: dark ? 'rgba(8,7,5,0.58)' : 'rgba(20,18,14,0.40)', zIndex: 5 }}/>

      {/* Передний план — диалог + меню */}
      <div style={{ position: 'absolute', inset: 0, zIndex: 10, display: 'flex', flexDirection: 'column', padding: '64px 14px 18px' }}>
        {/* Диалог с подробной информацией */}
        <div style={{
          flex: '1 1 auto', minHeight: 0, position: 'relative',
          background: t.bg, borderRadius: 22, overflow: 'hidden',
          boxShadow: '0 24px 64px rgba(0,0,0,0.34)', display: 'flex', flexDirection: 'column',
        }}>
          <div style={{ flex: 1, minHeight: 0, overflowY: 'auto' }}>
            <QVHeader t={t} rest={rest}/>
            {/* Явка / Сдача */}
            <QVLabel t={t}>Время работы</QVLabel>
            <div style={{ margin: '0 14px', background: t.surface, borderRadius: 14, overflow: 'hidden' }}>
              <QVTimeRow t={t} label="Явка" date={r.arrival.date} time={r.arrival.time} tag={r.arrival.tag}/>
              <QVTimeRow t={t} label="Сдача" date={r.handover.date} time={r.handover.time} last/>
            </div>

            {/* Отдых в ПО — подробно (короткий / полный) */}
            {rest.mode === 'po' && (<>
              <QVLabel t={t}>Отдых в пункте оборота · {rest.station}</QVLabel>
              <div style={{ margin: '0 14px', background: t.surface, borderRadius: 14, overflow: 'hidden' }}>
                <QVRestRow t={t} label="Короткий отдых" dur={rest.short.dur} until={rest.short.until}/>
                <QVRestRow t={t} label="Полный отдых" dur={rest.full.dur} until={rest.full.until} accent last/>
              </div>
            </>)}

            {/* Локомотивы */}
            <QVLabel t={t}>Локомотивы · {r.locos.length}</QVLabel>
            <div style={{ margin: '0 14px', background: t.surface, borderRadius: 14, overflow: 'hidden' }}>
              {r.locos.map((l, i) => (
                <QVLoco key={l.title} t={t} loco={l} last={i === r.locos.length - 1}/>
              ))}
            </div>

            {/* Поезда */}
            <QVLabel t={t}>Поезда · {r.trains.length}</QVLabel>
            <div style={{ margin: '0 14px', background: t.surface, borderRadius: 14, overflow: 'hidden' }}>
              {r.trains.map((tr, i) => (
                <QVTrain key={tr.num} t={t} train={tr} last={i === r.trains.length - 1}/>
              ))}
            </div>

            {/* Пассажиром */}
            <QVLabel t={t}>Следование пассажиром</QVLabel>
            <div style={{ margin: '0 14px', background: t.surface, borderRadius: 14, overflow: 'hidden' }}>
              <QVTrain t={t} train={{ num: r.pass.num, leg: `${r.pass.leg} · ${r.pass.note}`, kind: null, stops: r.pass.stops, icon: 'pass' }} last/>
            </div>

            {/* Доплатные признаки — без заголовка */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, padding: '16px 16px 0' }}>
              {r.attrs.map(([key, label]) => {
                const Icon = QV_ATTR_ICON[key];
                return (
                  <span key={key} style={{ display: 'inline-flex', alignItems: 'center', gap: 6, background: t.surface, borderRadius: 10, padding: '8px 12px', fontSize: 13, fontWeight: 500, color: t.text }}>
                    <Icon width="16" height="16" style={{ color: t.accent }}/> {label}
                  </span>
                );
              })}
            </div>

            {/* Заметки */}
            <QVLabel t={t}>Заметки</QVLabel>
            <div style={{ margin: '0 14px 18px', background: t.surface, borderRadius: 14, padding: '14px 16px' }}>
              <div style={{ fontSize: 14, color: t.text, lineHeight: 1.5 }}>{r.note}</div>
            </div>
          </div>

          {/* Подсказка прокрутки — нижний градиент */}
          <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 28, pointerEvents: 'none', background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${hexToRgba(t.bg, 0.9)})` }}/>
        </div>

        {/* Контекстное меню действий */}
        <div style={{ flexShrink: 0, marginTop: 12, width: 230, alignSelf: 'flex-end' }}>
          <QVMenu t={t} synced={r.synced}/>
        </div>
      </div>
    </MDevice>
  );
}

Object.assign(window, { IOSRouteQuickView, QV_REST_PO });
