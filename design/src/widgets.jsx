// widgets.jsx — Домашние виджеты «Машинист»: мини + развёрнутый
// =====================================================================
// Оба виджета построены на одной системе:
//   • одна карточка-обёртка (WCard): радиус 26, мягкая тень, бордер
//   • один заголовок-строка (WHead): mono-капс лейбл + брендовый чип «М»
//   • одна метрика времени (mono) + один индикатор нормы (NormBar)
//   • один тип разделителя и одна сетка отступов
// Малый = блок «норма за месяц». Развёрнутый = тот же блок + текущий
// маршрут + быстрая запись прибытия/отправления текущего поезда.
// Экспорт: WidgetMini, WidgetExpanded, WidgetStage
// =====================================================================

const WDATA = {
  month: 'Июнь',
  workedMin: 187 * 60 + 20,   // 187:20 отработано
  normMin: 176 * 60,          // 176:00 норма месяца
  route: { num: '147', from: 'Иланская', to: 'Боготол', work: '11:32', loco: '3ЭС5К' },
  train: { num: '№2654', nextStop: 'Канск-Енисейский', arr: '23:48', dep: null },
};

function wFmtHM(min) {
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${h}:${String(m).padStart(2, '0')}`;
}
function wNow() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

// ── Карточка-обёртка, общая для обоих виджетов ───────────────────────
function WCard({ t, w, h, pad = 16, children }) {
  return (
    <div style={{
      width: w, height: h, borderRadius: 26, background: t.bgElevated,
      border: `1px solid ${t.border}`,
      boxShadow: '0 1px 2px rgba(20,18,14,0.05), 0 14px 36px rgba(20,18,14,0.13)',
      padding: pad, display: 'flex', flexDirection: 'column',
      fontFamily: M.fontSans, overflow: 'hidden', position: 'relative', boxSizing: 'border-box',
    }}>{children}</div>
  );
}

// ── Шапка: mono-капс лейбл + брендовый чип «М» ───────────────────────
function WHead({ t, label, right }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
      <span style={{ fontFamily: M.fontMono, fontSize: 10.5, fontWeight: 600, letterSpacing: 1.3, textTransform: 'uppercase', color: t.textMuted, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{label}</span>
      {right !== undefined ? right : (
        <div style={{ width: 24, height: 24, borderRadius: 7, background: t.ink, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <IcLogo size={17} color="#FFFFFF"/>
        </div>
      )}
    </div>
  );
}

// ── Индикатор нормы: заполнение до нормы (accent) + переработка (warn) ─
function NormBar({ t, workedMin, normMin, h = 8 }) {
  // Трек = max(норма, отработано). Если переработка — полоса заполнена
  // целиком: синяя часть до нормы, оранжевый «хвост» — переработка.
  const over = workedMin >= normMin;
  const total = Math.max(workedMin, normMin);
  const accentPct = (Math.min(workedMin, normMin) / total) * 100;
  const warnPct = over ? ((workedMin - normMin) / total) * 100 : 0;
  return (
    <div style={{ position: 'relative', height: h, display: 'flex', alignItems: 'center' }}>
      <div style={{ position: 'relative', width: '100%', height: h, borderRadius: h / 2, background: t.bgSubtle, overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, bottom: 0, left: 0, width: `${accentPct}%`, background: t.accent }}/>
        {over && <div style={{ position: 'absolute', top: 0, bottom: 0, left: `${accentPct}%`, width: `${warnPct}%`, background: t.warning }}/>}
      </div>
      {over && <div style={{ position: 'absolute', top: -3, bottom: -3, left: `${accentPct}%`, width: 2, background: t.bgElevated, borderRadius: 1, transform: 'translateX(-1px)' }}/>}
    </div>
  );
}

// ── Индикатор нормы целиком: полоса + единая подпись (норма / месяц) ──
// Один и тот же блок в обоих виджетах — чтобы индикаторы были идентичны.
function NormMeter({ t }) {
  const { workedMin, normMin } = WDATA;
  const over = workedMin >= normMin;
  const factColor = over ? t.warning : t.success;
  return (
    <div>
      <NormBar t={t} workedMin={workedMin} normMin={normMin}/>
      <div style={{ marginTop: 8, display: 'flex', alignItems: 'baseline', gap: 10 }}>
        <span style={{ fontSize: 11.5, color: t.textMuted, whiteSpace: 'nowrap' }}>норма <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{wFmtHM(normMin)}</span></span>
      </div>
    </div>
  );
}

// ── Блок нормы — общий «движок» обоих виджетов ───────────────────────
// compact=true — вертикальная мини-раскладка; иначе — широкая.
function NormBlock({ t, compact }) {
  const { workedMin, normMin } = WDATA;
  const deltaMin = workedMin - normMin;
  const over = deltaMin >= 0;
  const deltaStr = `${over ? '+' : '−'}${wFmtHM(Math.abs(deltaMin))}`;
  const deltaColor = over ? t.warning : t.success;

  if (compact) {
    return (
      <>
        <WHead t={t} label="Отработано"/>
        <div style={{ flex: 1 }}/>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 7 }}>
          <span style={{ fontFamily: M.fontMono, fontSize: 38, fontWeight: 800, letterSpacing: -1.5, lineHeight: 1, color: t.text }}>{wFmtHM(workedMin)}</span>
        </div>
        <div style={{ marginTop: 5, display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontFamily: M.fontMono, fontSize: 12.5, fontWeight: 700, color: deltaColor }}>{deltaStr}</span>
          <span style={{ fontSize: 12, color: t.textMuted }}>сверх нормы</span>
        </div>
        <div style={{ flex: 1 }}/>
        <NormMeter t={t}/>
      </>
    );
  }

  // Широкая раскладка для развёрнутого виджета
  return (
    <div>
      <WHead t={t} label="Отработано"/>
      <div style={{ marginTop: 10, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
        <span style={{ fontFamily: M.fontMono, fontSize: 44, fontWeight: 800, letterSpacing: -2, lineHeight: 0.92, color: t.text }}>{wFmtHM(workedMin)}</span>
        <div style={{ textAlign: 'right', paddingBottom: 3 }}>
          <div style={{ fontFamily: M.fontMono, fontSize: 15, fontWeight: 700, color: deltaColor, lineHeight: 1 }}>{deltaStr}</div>
          <div style={{ fontSize: 11.5, color: t.textMuted, marginTop: 3 }}>сверх нормы</div>
        </div>
      </div>
      <div style={{ marginTop: 14 }}>
        <NormMeter t={t}/>
      </div>
    </div>
  );
}

// ── Тонкий разделитель ───────────────────────────────────────────────
function WSep({ t, my = 14 }) {
  return <div style={{ height: 1, background: t.border, margin: `${my}px 0` }}/>;
}

// ── Кнопка быстрой записи прибытия/отправления ───────────────────────
function QuickStampBtn({ t, dir, time, onStamp }) {
  const stamped = !!time;
  const Icon = dir === 'arr' ? IcArrowDown : IcArrowUp;
  const label = dir === 'arr' ? 'Прибытие' : 'Отправление';
  return (
    <button onClick={onStamp} style={{
      flex: 1, minWidth: 0, cursor: 'pointer', textAlign: 'left',
      border: stamped ? `1px solid ${t.border}` : `1.5px dashed ${t.borderStrong}`,
      background: stamped ? t.accentSoft : 'transparent',
      borderRadius: 14, padding: '10px 12px',
      display: 'flex', alignItems: 'center', gap: 10, fontFamily: M.fontSans,
    }}>
      <div style={{
        width: 30, height: 30, borderRadius: 9, flexShrink: 0,
        background: stamped ? t.accent : t.bgSubtle, color: stamped ? t.accentInk : t.textMuted,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <Icon width="17" height="17"/>
      </div>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 11, color: t.textMuted, fontWeight: 500, lineHeight: 1.1 }}>{label}</div>
        {stamped
          ? <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 2 }}>
              <span style={{ fontFamily: M.fontMono, fontSize: 17, fontWeight: 700, color: t.text, lineHeight: 1 }}>{time}</span>
              <IcCheck width="13" height="13" style={{ color: t.success }}/>
            </div>
          : <div style={{ fontSize: 14, fontWeight: 600, color: t.accent, marginTop: 2, lineHeight: 1 }}>Записать</div>}
      </div>
    </button>
  );
}

// ── Блок текущего маршрута + быстрая запись ──────────────────────────
function RouteBlock({ t }) {
  const r = WDATA.route;
  const tr = WDATA.train;
  const [arr, setArr] = React.useState(tr.arr);
  const [dep, setDep] = React.useState(tr.dep);
  return (
    <div>
      <WHead t={t} label="Текущий маршрут" right={
        <span style={{ fontFamily: M.fontMono, fontSize: 13, fontWeight: 700, color: t.text }}>№{r.num}</span>
      }/>
      {/* Направление */}
      <div style={{ marginTop: 9, display: 'flex', alignItems: 'center', gap: 9 }}>
        <span style={{ fontSize: 18, fontWeight: 600, color: t.text }}>{r.from}</span>
        <IcArrowDown width="15" height="15" style={{ transform: 'rotate(-90deg)', color: t.accent, flexShrink: 0 }}/>
        <span style={{ fontSize: 18, fontWeight: 600, color: t.text }}>{r.to}</span>
      </div>
      <WSep t={t} my={14}/>

      {/* Быстрая запись по текущему поезду */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 9 }}>
        <span style={{ fontFamily: M.fontMono, fontSize: 10.5, fontWeight: 600, letterSpacing: 1, textTransform: 'uppercase', color: t.textMuted, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {tr.num} · {tr.nextStop}
        </span>
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <QuickStampBtn t={t} dir="arr" time={arr} onStamp={() => setArr(wNow())}/>
        <QuickStampBtn t={t} dir="dep" time={dep} onStamp={() => setDep(wNow())}/>
      </div>
    </div>
  );
}

// ── МИНИ-виджет (2×2) ────────────────────────────────────────────────
function WidgetMini({ dark = false, size = 170 }) {
  const t = dark ? M.dark : M.light;
  return (
    <WCard t={t} w={size} h={size}>
      <NormBlock t={t} compact/>
    </WCard>
  );
}

// ── РАЗВЁРНУТЫЙ виджет (4×4) ─────────────────────────────────────────
function WidgetExpanded({ dark = false, w = 360, h = 356 }) {
  const t = dark ? M.dark : M.light;
  return (
    <WCard t={t} w={w} h={h} pad={18}>
      <NormBlock t={t}/>
      <WSep t={t} my={14}/>
      <RouteBlock t={t}/>
    </WCard>
  );
}

// ── Подложка-«обои» для показа виджетов в контексте ──────────────────
function WidgetStage({ dark = false, w = 420, h = 460, children, label }) {
  const wall = dark
    ? 'radial-gradient(120% 90% at 30% 0%, #20222A 0%, #131419 55%, #0B0C0F 100%)'
    : 'radial-gradient(120% 90% at 30% 0%, #EDE9DE 0%, #DED9CA 55%, #CFC9B8 100%)';
  return (
    <div style={{ width: w, height: h, background: wall, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', position: 'relative', overflow: 'hidden' }}>
      {label && <div style={{ position: 'absolute', top: 16, left: 0, right: 0, textAlign: 'center', fontFamily: M.fontMono, fontSize: 11, letterSpacing: 1.4, textTransform: 'uppercase', color: dark ? 'rgba(255,255,255,0.45)' : 'rgba(28,26,22,0.45)' }}>{label}</div>}
      {children}
    </div>
  );
}

// ── Композиция «на домашнем экране» ──────────────────────────────────
function WHomeIcon({ t, icon, brand }) {
  return (
    <div style={{ width: 60, height: 60, borderRadius: 15, background: brand ? t.ink : t.bgElevated, border: `1px solid ${t.border}`, boxShadow: '0 4px 12px rgba(20,18,14,0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: brand ? '#FFFFFF' : t.accent }}>
      {icon}
    </div>
  );
}

function WidgetHome({ dark = false }) {
  const t = dark ? M.dark : M.light;
  const wall = dark
    ? 'radial-gradient(130% 100% at 25% 0%, #232634 0%, #14151B 55%, #0A0B0E 100%)'
    : 'radial-gradient(130% 100% at 25% 0%, #ECE7DB 0%, #DCD6C6 55%, #CBC4B2 100%)';
  const onWall = dark ? '#F5F3EE' : '#1C1A16';
  return (
    <div style={{ width: 390, height: 720, background: wall, position: 'relative', overflow: 'hidden', fontFamily: M.fontSans, padding: '0 22px' }}>
      {/* Дата/время как на домашнем экране iOS */}
      <div style={{ paddingTop: 58, textAlign: 'center', color: onWall }}>
        <div style={{ fontSize: 13, fontWeight: 600, letterSpacing: 0.4, opacity: 0.85 }}>Среда, 24 июня</div>
        <div style={{ fontSize: 68, fontWeight: 700, letterSpacing: -1, lineHeight: 1.05, marginTop: 2 }}>9:41</div>
      </div>

      {/* Развёрнутый виджет */}
      <div style={{ marginTop: 26, display: 'flex', justifyContent: 'center' }}>
        <WidgetExpanded dark={dark} w={346} h={350}/>
      </div>

      {/* Ряд: мини-виджет + иконки приложений */}
      <div style={{ marginTop: 20, display: 'flex', alignItems: 'center', gap: 16 }}>
        <WidgetMini dark={dark} size={162}/>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'flex', gap: 14 }}>
            <WHomeIcon t={t} icon={<IcLogo size={30} color="#FFFFFF"/>} brand/>
            <WHomeIcon t={t} icon={<IcCalendar width="26" height="26"/>}/>
          </div>
          <div style={{ display: 'flex', gap: 14 }}>
            <WHomeIcon t={t} icon={<IcRuble width="26" height="26"/>}/>
            <WHomeIcon t={t} icon={<IcGear width="26" height="26"/>}/>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { WidgetMini, WidgetExpanded, WidgetStage, WidgetHome, WDATA });
