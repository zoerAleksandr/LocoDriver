// stats-screens.jsx — раздел «Статистика», iOS light
// 3 концепта главного экрана + состояния (графики, год, пустое, детализация, легенда).
// Открывается из «Инструментов» на Главном → экран с кнопкой «назад».

// ════════════════════════════════════════════════════════════════════
// ОБЩИЙ ХЕДЕР: nav (← Главный · Статистика · PDF) + период
// ════════════════════════════════════════════════════════════════════
function StatsNav({ t, action, platform = 'ios' }){
  if (platform === 'android') {
    return (
      <div style={{ paddingTop: 54 }}>
        <div style={{ height: 56, display: 'flex', alignItems: 'center', padding: '0 4px', gap: 4 }}>
          <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} aria-label="Назад">
            <IcChevronLeft width="24" height="24"/>
          </button>
          <div style={{ flex: 1, fontFamily: M.fontSans, fontSize: 20, fontWeight: 500, color: t.text }}>Статистика</div>
          <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} aria-label="PDF">
            {action || <IcPdf width="22" height="22"/>}
          </button>
        </div>
      </div>
    );
  }
  return (
    <div style={{ paddingTop: 58 }}>
      <div style={{ height: 44, display: 'flex', alignItems: 'center', padding: '0 8px', position: 'relative' }}>
        <button style={{ border: 'none', background: 'transparent', color: t.accent, display: 'inline-flex', alignItems: 'center', gap: 2, fontSize: 16, padding: '4px 6px', cursor: 'pointer' }}>
          <IcChevronLeft width="18" height="18"/> Главный
        </button>
        <div style={{ position: 'absolute', left: 0, right: 0, textAlign: 'center', pointerEvents: 'none', fontFamily: M.fontSans, fontSize: 17, fontWeight: 700, color: t.text }}>Статистика</div>
        <div style={{ marginLeft: 'auto' }}>
          <button style={{ border: 'none', background: 'transparent', color: t.accent, fontSize: 16, padding: '4px 8px', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            {action || <IcPdf width="20" height="20"/>}
          </button>
        </div>
      </div>
    </div>
  );
}

// Хедер экрана детализации (← назад · заголовок метрики) — платформо-зависимый
function StatsDetailNav({ t, title, platform = 'ios' }){
  if (platform === 'android') {
    return (
      <div style={{ paddingTop: 54 }}>
        <div style={{ height: 56, display: 'flex', alignItems: 'center', padding: '0 4px', gap: 4 }}>
          <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} aria-label="Назад">
            <IcChevronLeft width="24" height="24"/>
          </button>
          <div style={{ flex: 1, fontFamily: M.fontSans, fontSize: 20, fontWeight: 500, color: t.text }}>{title}</div>
        </div>
      </div>
    );
  }
  return (
    <div style={{ paddingTop: 58 }}>
      <div style={{ height: 44, display: 'flex', alignItems: 'center', padding: '0 8px', position: 'relative' }}>
        <button style={{ border: 'none', background: 'transparent', color: t.accent, display: 'inline-flex', alignItems: 'center', gap: 2, fontSize: 16, padding: '4px 6px', cursor: 'pointer' }}>
          <IcChevronLeft width="18" height="18"/> Статистика
        </button>
        <div style={{ position: 'absolute', left: 0, right: 0, textAlign: 'center', pointerEvents: 'none', fontFamily: M.fontSans, fontSize: 17, fontWeight: 700, color: t.text }}>{title}</div>
      </div>
    </div>
  );
}

// Шапка периода: сегмент Месяц/Год + навигация ← период →
function PeriodHeader({ t, mode = 'month', label, sub, onMode }){
  return (
    <div style={{ padding: '6px 16px 12px', display: 'flex', flexDirection: 'column', gap: 12 }}>
      <PeriodSegment t={t} value={mode} onChange={onMode}/>
      <PeriodNav t={t} label={label} sub={sub} nextDisabled={true}/>
    </div>
  );
}

// Подпись «по сравнению с …» — общий маркер сравнения
function CompareCaption({ t, children }){
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '2px 4px 10px', color: t.textMuted }}>
      <span style={{ fontFamily: M.fontSans, fontSize: 12.5, whiteSpace: 'nowrap' }}>в сравнении с <b style={{ color: t.text, fontWeight: 600 }}>{children}</b></span>
    </div>
  );
}

// Заголовок секции (как SectionH2, но локальный — с trailing-ссылкой)
function SH({ t, children, trailing }){
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '20px 4px 12px' }}>
      <div style={{ fontFamily: M.fontSans, fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -0.3 }}>{children}</div>
      {trailing}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// HERO отработанного времени (общий для A и C) — крупная цифра + дельта
// ════════════════════════════════════════════════════════════════════
function WorkedHero({ t, withSpark = false }){
  const m = STAT_METRICS.worked;
  return (
    <div style={{ background: t.surface, borderRadius: 24, padding: 22, boxShadow: M.shadow.md }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.4, marginBottom: 8 }}>ОТРАБОТАНО · Ч</div>
          <div style={{ fontFamily: M.fontMono, fontSize: 52, fontWeight: 800, color: t.text, letterSpacing: -2, lineHeight: 1 }}>{m.value}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <DeltaTag t={t} delta={m.delta}/>
          <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted, marginTop: 6 }}>{m.delta.abs}</div>
        </div>
      </div>
      {withSpark && (
        <div style={{ marginTop: 18, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
          <Sparkline t={t} data={SPARKS.worked} w={230} h={40}/>
          <span style={{ fontFamily: M.fontSans, fontSize: 11, color: t.textFaint }}>6 мес</span>
        </div>
      )}
      <div style={{ height: 1, background: t.border, margin: '18px 0 14px' }}/>
      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
        <HeroFoot t={t} label="Февраль" value={m.prevValue}/>
        <HeroFoot t={t} label="Норма" value="176:00"/>
        <HeroFoot t={t} label="Сверх нормы" value="+11:20" accent={t.danger}/>
      </div>
    </div>
  );
}
function HeroFoot({ t, label, value, accent }){
  return (
    <div>
      <div style={{ fontFamily: M.fontSans, fontSize: 11, color: t.textMuted, marginBottom: 4 }}>{label}</div>
      <div style={{ fontFamily: M.fontMono, fontSize: 15, fontWeight: 700, color: accent || t.text }}>{value}</div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// КОНЦЕПТ A — Hero + список метрик · дельта = чип (стрелка+%) · парные столбцы
// ════════════════════════════════════════════════════════════════════
function MetricRowA({ t, m, last }){
  return (
    <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 14, borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
      <div style={{ width: 40, height: 40, borderRadius: 12, flexShrink: 0, background: t.accentSoft, color: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {SU_ICONS[m.icon]({ width: 21, height: 21 })}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: t.text }}>{m.label}</div>
        <div style={{ fontFamily: M.fontSans, fontSize: 12, color: t.textFaint, marginTop: 2 }}>было {m.prevValue}</div>
      </div>
      <div style={{ textAlign: 'right', flexShrink: 0 }}>
        <div style={{ fontFamily: M.fontMono, fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -0.4, lineHeight: 1.1 }}>
          {m.value}{m.unit && <span style={{ fontSize: 11, color: t.textMuted, fontWeight: 500 }}> {m.unit}</span>}
        </div>
        <div style={{ marginTop: 5 }}><DeltaTag t={t} delta={m.delta} size="sm"/></div>
      </div>
    </div>
  );
}

function IOSStatsA({ height = 1240, dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  const rest = ['overtime','distance','tkm','night','routes'].map(k => STAT_METRICS[k]);
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <PeriodHeader t={t} mode="month" label="Март 2026" sub="МЕСЯЦ"/>
      <div style={{ padding: '0 16px 40px', overflowY: 'auto', height: 'calc(100% - 196px)' }}>
        <CompareCaption t={t}>февралём 2026</CompareCaption>
        <WorkedHero t={t}/>

        <SH t={t}>Показатели</SH>
        <Card t={t}>
          {rest.map((m, i) => <MetricRowA key={m.key} t={t} m={m} last={i===rest.length-1}/>)}
        </Card>

        <SH t={t} trailing={<TextAction t={t} label="Развернуть" suffix="→"/>}>Сравнение с февралём</SH>
        <Card t={t}>
          <div style={{ padding: 20 }}>
            <PairedBarsChart t={t} bars={STAT_BARS}/>
          </div>
        </Card>

        <SH t={t} trailing={<TextAction t={t} label="Все" suffix="→"/>}>Топ направлений</SH>
        <Card t={t}>
          <div style={{ padding: '6px 18px' }}>
            <TopDirections t={t} items={TOP_DIRECTIONS}/>
          </div>
        </Card>
      </div>
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════════
// КОНЦЕПТ B — Сетка карточек · дельта = мини-бар было/стало · столбцы группами
// ════════════════════════════════════════════════════════════════════
function MetricCardB({ t, m, wide }){
  const isNorm = m.delta.kind === 'norm';
  const curColor = isNorm ? normTone(t, m.delta.state).color : t.accent;
  // числа было/стало для мини-пары — из SPARKS (последние два)
  const sp = SPARKS[m.key];
  const prevN = sp[sp.length-2], curN = sp[sp.length-1];
  return (
    <div style={{ gridColumn: wide ? 'span 2' : 'span 1', background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm, padding: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: t.textMuted, marginBottom: wide ? 10 : 12 }}>
        <span style={{ color: t.accent, display: 'flex' }}>{SU_ICONS[m.icon]({ width: 17, height: 17 })}</span>
        <span style={{ fontFamily: M.fontSans, fontSize: 12.5, fontWeight: 600, color: t.textMuted }}>{m.label}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 8 }}>
        <div>
          <div style={{ fontFamily: M.fontMono, fontSize: wide ? 40 : 26, fontWeight: 800, color: t.text, letterSpacing: -1, lineHeight: 1, whiteSpace: 'nowrap' }}>
            {m.value}
          </div>
          {m.unit && <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted, marginTop: 4, whiteSpace: 'nowrap' }}>{m.unit}</div>}
        </div>
        <MiniPair t={t} prev={Math.abs(prevN)} cur={Math.abs(curN)} curColor={curColor} h={wide ? 44 : 34}/>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 12 }}>
        <DeltaTag t={t} delta={m.delta} variant="plain" size="sm"/>
        <span style={{ fontFamily: M.fontSans, fontSize: 11, color: t.textFaint }}>было {m.prevValue}</span>
      </div>
    </div>
  );
}

// Плашка месяца из STAT_METRICS — обёртка над общей MetricPlate.
function MonthPlate({ t, mkey, wide, compare }){
  const m = STAT_METRICS[mkey];
  return (
    <MetricPlate t={t} icon={m.icon} label={m.label} value={m.value} unit={m.unit}
      prev={m.prevValue} delta={m.delta} spark={SPARKS[m.key]} wide={wide} compare={compare}/>
  );
}

function IOSStatsB({ height = 1240, initialSheet = null, initialCmp = 'prev', dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  const [cmpId, setCmpId] = React.useState(initialCmp);
  const [customLabel, setCustomLabel] = React.useState('Декабрь 2025');
  const [sheet, setSheet] = React.useState(initialSheet); // 'compare' | 'monthpick' | null
  const compare = cmpId !== 'none';
  const cmp = COMPARE_MONTH.find(o => o.id === cmpId) || COMPARE_MONTH[0];
  const title = cmpId === 'custom' ? customLabel : cmp.title;
  // Тот же порядок, что и в «Год» (worked — крупной плашкой первым)
  const order = ['worked','overtime','earnings','speed','transit','distance','tkm','night','routes'];
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <PeriodHeader t={t} mode="month" label="Март 2026" sub="МЕСЯЦ"/>
      <div style={{ padding: '0 16px 40px', overflowY: 'auto', height: 'calc(100% - 196px)' }}>
        <CompareSelector t={t} title={title} none={!compare} onOpen={()=>setSheet('compare')}/>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          {order.map((k, i) => <MonthPlate key={k} t={t} mkey={k} wide={i===0} compare={compare}/>)}
        </div>

        <SH t={t}>Топ направлений</SH>
        <Card t={t}>
          <div style={{ padding: '4px 18px 10px' }}>
            <DonutBreakdown t={t}
              centerMain={TOP_DIRECTIONS.reduce((s,d)=>s+d.trips,0)} centerSub="поездок"
              segments={TOP_DIRECTIONS.map(d => ({
                label: `${d.from} → ${d.to}`, value: d.trips, sub: `${d.trips} раз · ${d.hours} ч`,
              }))}/>
          </div>
        </Card>
      </div>
      {sheet === 'compare' && (
        <ComparePicker t={t} title="Сравнить месяц с" options={COMPARE_MONTH}
          selectedId={cmpId} onSelect={setCmpId} onAction={()=>setSheet('monthpick')}
          onClose={()=>setSheet(null)}/>
      )}
      {sheet === 'monthpick' && (
        <MonthGridPicker t={t} selectedLabel={cmpId==='custom' ? customLabel : null}
          onPick={(lbl)=>{ setCustomLabel(lbl); setCmpId('custom'); }}
          onClose={()=>setSheet(null)} onBack={()=>setSheet('compare')}/>
      )}
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════════
// КОНЦЕПТ C — Насыщенный дашборд со спарклайнами · дельта = стрелка+% · линия
// ════════════════════════════════════════════════════════════════════
function MetricCardC({ t, m }){
  const isNorm = m.delta.kind === 'norm';
  const sColor = isNorm ? normTone(t, m.delta.state).color : t.accent;
  return (
    <div style={{ background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, padding: 14 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
        <span style={{ color: t.accent, display: 'flex' }}>{SU_ICONS[m.icon]({ width: 16, height: 16 })}</span>
        <DeltaTag t={t} delta={m.delta} variant="plain" size="sm"/>
      </div>
      <div style={{ fontFamily: M.fontMono, fontSize: 23, fontWeight: 800, color: t.text, letterSpacing: -0.8, lineHeight: 1 }}>{m.value}</div>
      <div style={{ fontFamily: M.fontSans, fontSize: 11.5, color: t.textMuted, marginTop: 3 }}>{m.unit ? m.unit + ' · ' : ''}{m.short}</div>
      <div style={{ marginTop: 10 }}>
        <Sparkline t={t} data={SPARKS[m.key].map(Math.abs)} color={sColor} w={120} h={26} dot={false}/>
      </div>
    </div>
  );
}

function IOSStatsC({ height = 1240, dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  const grid = ['overtime','distance','tkm','night','routes'].map(k => STAT_METRICS[k]);
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <PeriodHeader t={t} mode="month" label="Март 2026" sub="МЕСЯЦ"/>
      <div style={{ padding: '0 16px 40px', overflowY: 'auto', height: 'calc(100% - 196px)' }}>
        <CompareCaption t={t}>февралём 2026</CompareCaption>
        <WorkedHero t={t} withSpark/>

        <SH t={t}>Показатели</SH>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {grid.map(m => <MetricCardC key={m.key} t={t} m={m}/>)}
        </div>

        <SH t={t} trailing={<TextAction t={t} label="Год" suffix="→"/>}>Динамика часов</SH>
        <Card t={t}>
          <div style={{ padding: 18 }}>
            <YearLineChart t={t} months={YEAR_MONTHS}/>
          </div>
        </Card>

        <SH t={t} trailing={<TextAction t={t} label="Все" suffix="→"/>}>Топ направлений</SH>
        <Card t={t}>
          <div style={{ padding: '6px 18px' }}>
            <TopDirections t={t} items={TOP_DIRECTIONS.slice(0,3)}/>
          </div>
        </Card>
      </div>
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════════
// СОСТОЯНИЕ: ГРАФИКИ СРАВНЕНИЯ (парные столбцы по метрикам)
// ════════════════════════════════════════════════════════════════════
function IOSStatsCompare({ height = 1040, dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <PeriodHeader t={t} mode="month" label="Март 2026" sub="МЕСЯЦ"/>
      <div style={{ padding: '0 16px 40px', overflowY: 'auto', height: 'calc(100% - 196px)' }}>
        <CompareCaption t={t}>февралём 2026</CompareCaption>
        <Card t={t}>
          <div style={{ padding: 20 }}>
            <div style={{ fontFamily: M.fontSans, fontSize: 16, fontWeight: 700, color: t.text, marginBottom: 16 }}>Текущий vs предыдущий</div>
            <PairedBarsChart t={t} bars={STAT_BARS}/>
          </div>
        </Card>

        <SH t={t}>Переработка</SH>
        <Card t={t}>
          <div style={{ padding: 20 }}>
            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 14 }}>
              <span style={{ fontFamily: M.fontSans, fontSize: 13, color: t.textMuted }}>Факт против нормы месяца</span>
              <DeltaTag t={t} delta={STAT_METRICS.overtime.delta}/>
            </div>
            <NormBar t={t} norm={176} fact={187} unit="ч"/>
          </div>
        </Card>
      </div>
    </MDevice>
  );
}

// Полоса «норма vs факт» — единый стиль с виджетом: заполнение до нормы
// (accent) + хвост переработки (warning) с засечкой; подписи по краям.
function NormBar({ t, norm, fact, unit }){
  const over = fact >= norm;
  const total = Math.max(norm, fact);
  const accentPct = (Math.min(norm, fact) / total) * 100;
  const warnPct = over ? ((fact - norm) / total) * 100 : 0;
  const factColor = over ? t.danger : t.success;
  return (
    <div>
      <div style={{ position: 'relative', height: 18, background: t.bgSubtle, borderRadius: 9, overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, bottom: 0, left: 0, width: `${accentPct}%`, background: t.accent }}/>
        {over && <div style={{ position: 'absolute', top: 0, bottom: 0, left: `${accentPct}%`, width: `${warnPct}%`, background: t.danger }}/>}
        {over && <div style={{ position: 'absolute', top: 0, bottom: 0, left: `${accentPct}%`, width: 2, background: t.bgElevated, transform: 'translateX(-1px)' }}/>}
      </div>
      <div style={{ marginTop: 7, display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
        <span style={{ fontFamily: M.fontMono, fontSize: 11.5, color: t.textMuted, whiteSpace: 'nowrap' }}>
          норма <span style={{ color: t.text, fontWeight: 600 }}>{norm}{unit ? ` ${unit}` : ''}</span>
        </span>
        <span style={{ fontFamily: M.fontMono, fontSize: 11.5, fontWeight: 700, color: factColor, whiteSpace: 'nowrap' }}>
          факт {fact}{unit ? ` ${unit}` : ''}
        </span>
      </div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// СОСТОЯНИЕ: РЕЖИМ «ГОД» — помесячная динамика 12 мес + прошлый год
// ════════════════════════════════════════════════════════════════════
function IOSStatsYear({ height = 1240, initialSheet = null, initialCmp = 'prev', dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  const [cmpId, setCmpId] = React.useState(initialCmp);
  const [customLabel, setCustomLabel] = React.useState('2024 год');
  const [sheet, setSheet] = React.useState(initialSheet); // 'compare' | 'yearpick' | null
  const compare = cmpId !== 'none';
  const cmp = COMPARE_YEAR.find(o => o.id === cmpId) || COMPARE_YEAR[0];
  const title = cmpId === 'custom' ? customLabel : cmp.title;
  const yearKeys = ['overtime','earnings','speed','transit','distance','tkm','night','routes'];
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <PeriodHeader t={t} mode="year" label="2026 год" sub="ГОД"/>
      <div style={{ padding: '0 16px 40px', overflowY: 'auto', height: 'calc(100% - 196px)' }}>
        <CompareSelector t={t} title={title} none={!compare} onOpen={()=>setSheet('compare')}/>

        {/* Итог года — крупно */}
        <div style={{ background: t.surface, borderRadius: 24, padding: 22, boxShadow: M.shadow.md }}>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.4, marginBottom: 8 }}>ОТРАБОТАНО ЗА ГОД · Ч</div>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
            <div style={{ fontFamily: M.fontMono, fontSize: 46, fontWeight: 800, color: t.text, letterSpacing: -1.5, lineHeight: 1, whiteSpace: 'nowrap' }}>2 122</div>
            {compare && <DeltaTag t={t} delta={YEAR_TOTALS.worked.delta}/>}
          </div>
          <div style={{ marginTop: 20 }}>
            <YearBarsChart t={t} months={YEAR_MONTHS} overlayPrev={compare}/>
          </div>
        </div>

        <div style={{ height: 20 }}/>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          {yearKeys.map((key) => {
            const m = STAT_METRICS[key];
            const yt = YEAR_TOTALS[key];
            return (
              <MetricPlate key={key} t={t} icon={m.icon} label={m.label}
                value={yt.num} unit={yt.unit} prev={yt.prev} delta={yt.delta}
                spark={SPARKS[key]} compare={compare}/>
            );
          })}
        </div>

        <SH t={t}>Топ направлений за год</SH>
        <Card t={t}>
          <div style={{ padding: '4px 18px 10px' }}>
            <DonutBreakdown t={t}
              centerMain={TOP_DIRECTIONS.reduce((s,d)=>s+d.trips,0)} centerSub="поездок"
              segments={TOP_DIRECTIONS.map(d => ({
                label: `${d.from} → ${d.to}`, value: d.trips, sub: `${d.trips} раз · ${d.hours} ч`,
              }))}/>
          </div>
        </Card>
      </div>
      {sheet === 'compare' && (
        <ComparePicker t={t} title="Сравнить год с" options={COMPARE_YEAR}
          selectedId={cmpId} onSelect={setCmpId} onAction={()=>setSheet('yearpick')}
          onClose={()=>setSheet(null)}/>
      )}
      {sheet === 'yearpick' && (
        <YearListPicker t={t} selectedLabel={cmpId==='custom' ? customLabel : null}
          onPick={(lbl)=>{ setCustomLabel(lbl); setCmpId('custom'); }}
          onClose={()=>setSheet(null)} onBack={()=>setSheet('compare')}/>
      )}
    </MDevice>
  );
}
function yearValue(key){
  return ({ worked: '2 122 ч', distance: '41,2 тыс. км', tkm: '14,8 млн ткм', night: '583 ч' })[key];
}

// ════════════════════════════════════════════════════════════════════
// СОСТОЯНИЕ: ПУСТОЕ (нет данных за период)
// ════════════════════════════════════════════════════════════════════
function IOSStatsEmpty({ height = 844, dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <PeriodHeader t={t} mode="month" label="Март 2026" sub="МЕСЯЦ"/>
      <div style={{ padding: '0 16px 40px', height: 'calc(100% - 196px)', display: 'flex', flexDirection: 'column' }}>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: '0 24px' }}>
          <div style={{ width: 76, height: 76, borderRadius: 24, background: t.bgSubtle, color: t.textFaint, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 22 }}>
            <IcGauge width="38" height="38"/>
          </div>
          <div style={{ fontFamily: M.fontSans, fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -0.3 }}>За март данных нет</div>
          <div style={{ fontFamily: M.fontSans, fontSize: 14, color: t.textMuted, lineHeight: 1.5, marginTop: 8, maxWidth: 280 }}>
            В этом месяце ещё не закрыто ни одного маршрута. Как появятся смены — здесь будет статистика и сравнение с февралём.
          </div>
          <button style={{ marginTop: 24, padding: '13px 22px', borderRadius: 14, background: t.cta, color: t.ctaInk, border: 'none', fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <IcPlus width="18" height="18"/> Добавить маршрут
          </button>
          <button style={{ marginTop: 6, padding: '10px 16px', background: 'transparent', border: 'none', color: t.accent, fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
            Перейти к февралю →
          </button>
        </div>
      </div>
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════════
// СОСТОЯНИЕ: ДЕТАЛИЗАЦИЯ МЕТРИКИ (тап по карточке)
// Помесячная разбивка интерактивна: тап по столбцу выбирает месяц,
// крупная цифра вверху и разбивка пересчитываются под него.
// ════════════════════════════════════════════════════════════════════
// 12 месяцев пути (тыс. км), скользящее окно по март 2026 включительно.
const DETAIL_DIST = [
  { sh: 'Апр', full: 'Апрель 2025',  v: 3.1 },
  { sh: 'Май', full: 'Май 2025',     v: 2.7 },
  { sh: 'Июн', full: 'Июнь 2025',    v: 3.0 },
  { sh: 'Июл', full: 'Июль 2025',    v: 3.5 },
  { sh: 'Авг', full: 'Август 2025',  v: 3.3 },
  { sh: 'Сен', full: 'Сентябрь 2025', v: 2.8 },
  { sh: 'Окт', full: 'Октябрь 2025', v: 3.4 },
  { sh: 'Ноя', full: 'Ноябрь 2025',  v: 2.9 },
  { sh: 'Дек', full: 'Декабрь 2025', v: 3.2 },
  { sh: 'Янв', full: 'Январь 2026',  v: 2.9 },
  { sh: 'Фев', full: 'Февраль 2026', v: 3.0 },
  { sh: 'Мар', full: 'Март 2026',    v: 3.4 },
];
const fmtTk = (v) => v.toFixed(1).replace('.', ',');

// Интерактивный помесячный график: один ряд столбцов, выбранный — акцентом.
function MonthBars({ t, data, sel, onSelect, h = 168 }){
  const max = Math.max(...data.map(d => d.v)) * 1.12;
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: h }}>
      {data.map((d, i) => {
        const active = i === sel;
        return (
          <button key={i} onClick={() => onSelect(i)} style={{
            flex: 1, height: '100%', padding: 0, border: 'none', background: 'transparent',
            cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end',
          }}>
            <span style={{ fontFamily: M.fontMono, fontSize: 10, fontWeight: 700, color: active ? t.accent : 'transparent', marginBottom: 5, whiteSpace: 'nowrap' }}>{fmtTk(d.v)}</span>
            <span style={{
              width: '100%', maxWidth: 22, height: `${(d.v/max)*100}%`, borderRadius: '5px 5px 0 0',
              background: active ? t.accent : hexToRgba(t.accent, 0.18),
            }}/>
            <span style={{ fontFamily: M.fontMono, fontSize: 9.5, fontWeight: active ? 800 : 500, color: active ? t.text : t.textFaint, marginTop: 7 }}>{d.sh}</span>
          </button>
        );
      })}
    </div>
  );
}

function IOSStatsDetail({ height = 1100, metric = 'distance', dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  const m = STAT_METRICS[metric];
  const [sel, setSel] = React.useState(DETAIL_DIST.length - 1); // по умолчанию — март 2026
  const cur = DETAIL_DIST[sel];
  const prev = sel > 0 ? DETAIL_DIST[sel - 1] : null;
  const diff = prev ? cur.v - prev.v : 0;
  const delta = prev ? {
    kind: 'info',
    dir: Math.abs(diff) < 0.05 ? 'flat' : diff > 0 ? 'up' : 'down',
    pct: `${diff >= 0 ? '+' : '−'}${Math.abs(Math.round((diff / prev.v) * 100))}%`,
    abs: `${diff >= 0 ? '+' : '−'}${Math.abs(Math.round(diff * 1000))} км`,
  } : null;
  // разбивка пути по роду движения, пропорционально выбранному месяцу
  const freight = +(cur.v * 0.64).toFixed(1), passenger = +(cur.v * 0.22).toFixed(1), reserve = +(cur.v - freight - passenger).toFixed(1);
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsDetailNav t={t} title={m.short} platform={platform}/>
      <div style={{ padding: '8px 16px 40px', overflowY: 'auto', height: 'calc(100% - 102px)' }}>
        {/* Крупное значение выбранного месяца + сравнение с предыдущим */}
        <div style={{ background: t.surface, borderRadius: 24, padding: 22, boxShadow: M.shadow.md }}>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.4, marginBottom: 8 }}>РАССТОЯНИЕ · {cur.full.toUpperCase()}</div>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
            <div style={{ fontFamily: M.fontMono, fontSize: 46, fontWeight: 800, color: t.text, letterSpacing: -1.5, lineHeight: 1 }}>
              {fmtTk(cur.v)}<span style={{ fontSize: 18, color: t.textMuted, fontWeight: 600 }}> тыс. км</span>
            </div>
            {delta && (
              <div style={{ textAlign: 'right' }}>
                <DeltaTag t={t} delta={delta}/>
                <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted, marginTop: 6 }}>{prev.full.replace(/ \d{4}$/, '')} {fmtTk(prev.v)}</div>
              </div>
            )}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 14, color: t.textFaint }}>
            <IcRoute width="14" height="14"/>
            <span style={{ fontFamily: M.fontSans, fontSize: 12 }}>тап по столбцу — выбрать месяц</span>
          </div>
        </div>

        <SH t={t}>По месяцам</SH>
        <Card t={t}>
          <div style={{ padding: '18px 16px 16px' }}>
            <MonthBars t={t} data={DETAIL_DIST} sel={sel} onSelect={setSel}/>
          </div>
        </Card>

        <SH t={t}>Детали</SH>
        <Card t={t}>
          <div style={{ padding: '4px 18px 12px' }}>
            <DonutBreakdown t={t} size={142}
              centerMain={fmtTk(cur.v)} centerSub="тыс. км"
              segments={[
                { label: 'Грузовой поезд', value: 64, sub: `${fmtTk(freight)} тыс. км` },
                { label: 'Пассажирский поезд', value: 22, sub: `${fmtTk(passenger)} тыс. км` },
                { label: 'Резервом', value: 14, sub: `${fmtTk(reserve)} тыс. км` },
              ]}/>
          </div>
        </Card>
      </div>
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════════
// СОСТОЯНИЕ: ДЕТАЛИЗАЦИЯ МЕТРИКИ — обзор ЗА ГОД (месяц не выбран)
// Копия экрана детализации: вверху итог года, столбцы без выделения,
// разбивка пути — за весь год. Тап по столбцу уводит в помесячный вид.
// ════════════════════════════════════════════════════════════════════
function IOSStatsDetailYear({ height = 1100, metric = 'distance', dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  const m = STAT_METRICS[metric];
  const yearV = 41.2;            // тыс. км за 2026 год
  const freight = +(yearV * 0.64).toFixed(1), passenger = +(yearV * 0.22).toFixed(1), reserve = +(yearV - freight - passenger).toFixed(1);
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsDetailNav t={t} title={m.short} platform={platform}/>
      <div style={{ padding: '8px 16px 40px', overflowY: 'auto', height: 'calc(100% - 102px)' }}>
        {/* Крупное значение за год + сравнение с прошлым годом */}
        <div style={{ background: t.surface, borderRadius: 24, padding: 22, boxShadow: M.shadow.md }}>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.4, marginBottom: 8 }}>РАССТОЯНИЕ · 2026 ГОД</div>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
            <div style={{ fontFamily: M.fontMono, fontSize: 46, fontWeight: 800, color: t.text, letterSpacing: -1.5, lineHeight: 1 }}>
              {fmtTk(yearV)}<span style={{ fontSize: 18, color: t.textMuted, fontWeight: 600 }}> тыс. км</span>
            </div>
            <div style={{ textAlign: 'right' }}>
              <DeltaTag t={t} delta={{ kind:'info', dir:'up', pct:'+4%', abs:'+1,4 тыс. км' }}/>
              <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted, marginTop: 6 }}>2025 год 39,8</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 14, color: t.textFaint }}>
            <IcRoute width="14" height="14"/>
            <span style={{ fontFamily: M.fontSans, fontSize: 12 }}>тап по столбцу — посмотреть месяц</span>
          </div>
        </div>

        <SH t={t}>По месяцам</SH>
        <Card t={t}>
          <div style={{ padding: '18px 16px 16px' }}>
            <MonthBars t={t} data={DETAIL_DIST} sel={-1} onSelect={() => {}}/>
          </div>
        </Card>

        <SH t={t}>Детали</SH>
        <Card t={t}>
          <div style={{ padding: '4px 18px 12px' }}>
            <DonutBreakdown t={t} size={142}
              centerMain={fmtTk(yearV)} centerSub="тыс. км"
              segments={[
                { label: 'Грузовой поезд', value: 64, sub: `${fmtTk(freight)} тыс. км` },
                { label: 'Пассажирский поезд', value: 22, sub: `${fmtTk(passenger)} тыс. км` },
                { label: 'Резервом', value: 14, sub: `${fmtTk(reserve)} тыс. км` },
              ]}/>
          </div>
        </Card>
      </div>
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════════
// ЛЕГЕНДА ЦВЕТОВОЙ ЛОГИКИ (как шторка поверх главного)
// ════════════════════════════════════════════════════════════════════
function IOSStatsLegend({ height = 844, dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <PeriodHeader t={t} mode="month" label="Март 2026" sub="МЕСЯЦ"/>
      <div style={{ position: 'absolute', inset: 0, zIndex: 70 }}>
        <div style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, background: t.bg, borderRadius: '24px 24px 0 0', boxShadow: '0 -20px 60px rgba(0,0,0,0.28)', padding: '12px 20px 32px' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 14 }}>
            <div style={{ width: 40, height: 5, borderRadius: 3, background: t.borderStrong }}/>
          </div>
          <div style={{ fontFamily: M.fontSans, fontSize: 20, fontWeight: 700, color: t.text, marginBottom: 4 }}>Что значат цвета</div>
          <div style={{ fontFamily: M.fontSans, fontSize: 13, color: t.textMuted, lineHeight: 1.45, marginBottom: 8 }}>
            Цветом выделяем только дельту по норме. Остальные дельты — серые: рост не «хорошо» и не «плохо».
          </div>
          <DeltaLegend t={t}/>
          <button style={{ marginTop: 18, width: '100%', padding: 14, borderRadius: 14, background: t.cta, color: t.ctaInk, border: 'none', fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, cursor: 'pointer' }}>
            Понятно
          </button>
        </div>
      </div>
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════════
// СОСТОЯНИЕ: ВКЛАДКА «ИСТОРИЯ» — все годы в приложении (с июля 2023)
// ════════════════════════════════════════════════════════════════════
function IOSStatsHistory({ height = 1380, metric = 'worked', dark = false, platform = 'ios' }){
  const t = dark ? M.dark : M.light;
  // Активная метрика управляет крупным итогом и блоком «Год за годом».
  const cfg = HISTORY_METRICS.find(m => m.key === metric) || HISTORY_METRICS[0];
  const total = YEAR_HISTORY.reduce((s, y) => s + y[cfg.field], 0);
  const totRoutes = YEAR_HISTORY.reduce((s, y) => s + y.routes, 0);
  // Столбцы нормируем на максимум года по выбранной метрике → лучший год = вся ширина.
  const maxV = Math.max(...YEAR_HISTORY.map(y => y[cfg.field]));
  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <StatsNav t={t} platform={platform}/>
      <div style={{ padding: '6px 16px 10px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <PeriodSegment t={t} value="all"/>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: '2px 0' }}>
          <span style={{ fontFamily: M.fontSans, fontSize: 17, fontWeight: 700, color: t.text }}>Всё время</span>
          <span style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted }}>с июля 2023</span>
        </div>
      </div>
      <div style={{ padding: '0 16px 44px' }}>
        {/* Итог за всё время по ВЫБРАННОЙ метрике — крупно */}
        <div style={{ background: t.surface, borderRadius: 24, padding: 22, boxShadow: M.shadow.md }}>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1.4, marginBottom: 8 }}>
            {cfg.label.toUpperCase()} ВСЕГО{cfg.totUnit ? ` · ${cfg.totUnit.toUpperCase()}` : ''}
          </div>
          <div style={{ fontFamily: M.fontMono, fontSize: 46, fontWeight: 800, color: t.text, letterSpacing: -1.5, lineHeight: 1, whiteSpace: 'nowrap' }}>
            {cfg.sumFmt(total)}
          </div>
          <div style={{ fontFamily: M.fontSans, fontSize: 13, color: t.textMuted, marginTop: 10 }}>
            За {YEAR_HISTORY.length} календарных года · {totRoutes} смен
          </div>
        </div>

        {/* Плашки метрик — по 2 в ряд, как во вкладках «Месяц» / «Год»: иконка + название
            сверху, крупный итог за всё время ниже. Выбранная подсвечена — именно она
            строит блок «Детализация» ниже. */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 12 }}>
          {HISTORY_METRICS.map((m) => {
            const on = m.key === cfg.key;
            const sub = m.totUnit || '';
            return (
              <div key={m.key} style={{
                background: on ? t.accent : t.surface,
                borderRadius: 18, boxShadow: on ? M.shadow.md : M.shadow.sm,
                border: on ? `1px solid ${t.accent}` : `1px solid ${t.border}`,
                padding: 16, cursor: 'pointer', transition: 'all .15s' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                  <span style={{ color: on ? '#fff' : t.accent, display: 'flex', flexShrink: 0 }}>{SU_ICONS[m.icon]({ width: 17, height: 17 })}</span>
                  <span style={{ flex: 1, minWidth: 0, fontFamily: M.fontSans, fontSize: 12.5, fontWeight: 600, color: on ? 'rgba(255,255,255,0.92)' : t.textMuted, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{m.label}</span>
                  {on && <span style={{ width: 7, height: 7, borderRadius: 99, background: '#fff', flexShrink: 0 }}/>}
                </div>
                <div style={{ fontFamily: M.fontMono, fontSize: 26, fontWeight: 800, color: on ? '#fff' : t.text, letterSpacing: -1, lineHeight: 1, whiteSpace: 'nowrap' }}>
                  {m.sumFmt(YEAR_HISTORY.reduce((s, y) => s + y[m.field], 0))}
                </div>
                {sub && <div style={{ fontFamily: M.fontMono, fontSize: 12, color: on ? 'rgba(255,255,255,0.75)' : t.textMuted, marginTop: 4 }}>{sub}</div>}
              </div>
            );
          })}
        </div>

        {/* Год за годом — разбивка выбранной метрики */}
        <SH t={t}>Детализация</SH>
        <Card t={t}>
          <div style={{ padding: '10px 18px 14px', display: 'flex', flexDirection: 'column', gap: 18 }}>
            {YEAR_HISTORY.map((y) => {
              const v = y[cfg.field];
              return (
                <div key={y.year}>
                  <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 7 }}>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                      <span style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 700, color: t.text }}>{y.year}</span>
                      {y.note && <span style={{ fontFamily: M.fontSans, fontSize: 11, color: t.textFaint }}>{y.note}</span>}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 5 }}>
                      <span style={{ fontFamily: M.fontMono, fontSize: 15, fontWeight: 700, color: t.text }}>{cfg.fmt(v)}</span>
                      {cfg.unit && <span style={{ fontFamily: M.fontMono, fontSize: 11.5, color: t.textMuted }}>{cfg.unit}</span>}
                    </div>
                  </div>
                  <div style={{ position: 'relative', height: 14, borderRadius: 7, background: t.bgSubtle, overflow: 'hidden' }}>
                    <div style={{ position: 'absolute', inset: 0, width: `${(v/maxV)*100}%`, borderRadius: 7,
                      background: y.partial ? `repeating-linear-gradient(135deg, ${t.accent}, ${t.accent} 5px, ${hexToRgba(t.accent,0.5)} 5px, ${hexToRgba(t.accent,0.5)} 10px)` : t.accent }}/>
                  </div>
                </div>
              );
            })}
          </div>
          <div style={{ padding: '2px 18px 16px', display: 'flex', alignItems: 'center', gap: 7, color: t.textFaint }}>
            <span style={{ width: 22, height: 9, borderRadius: 3, background: `repeating-linear-gradient(135deg, ${t.accent}, ${t.accent} 4px, ${hexToRgba(t.accent,0.5)} 4px, ${hexToRgba(t.accent,0.5)} 8px)`, display: 'inline-block', flexShrink: 0 }}/>
            <span style={{ fontFamily: M.fontSans, fontSize: 11.5 }}>штриховка — неполный год (2023 с июля, 2026 по март)</span>
          </div>
        </Card>

        <button style={{ marginTop: 18, width: '100%', padding: 14, borderRadius: 14, background: 'transparent', border: `1px solid ${t.border}`, color: t.accent, fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
          <IcPdf width="18" height="18"/> Сохранить историю в PDF
        </button>
      </div>
    </MDevice>
  );
}


// ════════════════════════════════════════════════════════════════════
// PDF-ОТЧЁТ: как выглядит выгрузка статистики (лист A4, превью печати)
// ════════════════════════════════════════════════════════════════════
function StatsPdfDoc(){
  const t = M.light;
  const ink = '#1a1714', sub = '#6b6760', line = '#e2ded6', accent = t.accent;
  const kpis = [
    ['Отработано', '2 122 ч', '+5% к 2025'],
    ['Заработано', '1,68 млн ₽', '+9%'],
    ['Пройдено', '41,2 тыс. км', '+4%'],
    ['Грузооборот', '14,8 млн ткм', '+6%'],
    ['Средняя скорость', '46,8 км/ч', '+3%'],
    ['Время в пути', '1 640 ч', '+5%'],
    ['Ночные', '583 ч', '+6%'],
    ['Переработка', '+48:20', 'норма 2 074 ч'],
    ['Смены', '243', '+7'],
  ];
  const maxW = Math.max(...YEAR_MONTHS.map(m => m.cur)) * 1.05;
  const Mono = { fontFamily: M.fontMono };
  const Sans = { fontFamily: M.fontSans };
  return (
    <div style={{ width: 794, minHeight: 1123, background: '#fff', padding: '56px 60px', boxSizing: 'border-box', color: ink, position: 'relative' }}>
      {/* Шапка */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: `2px solid ${ink}`, paddingBottom: 20 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
            <span style={{ color: accent, display: 'flex' }}><IcLogo width="26" height="26"/></span>
            <span style={{ ...Sans, fontSize: 15, fontWeight: 700, letterSpacing: 0.3 }}>Маршрутный лист</span>
          </div>
          <div style={{ ...Sans, fontSize: 27, fontWeight: 800, letterSpacing: -0.6 }}>Статистика за 2026 год</div>
          <div style={{ ...Sans, fontSize: 13.5, color: sub, marginTop: 6 }}>Машинист · Колесников И. А. · ТЧЭ-8 Санкт-Петербург — Сортировочный</div>
        </div>
        <div style={{ textAlign: 'right', ...Mono, fontSize: 11, color: sub, lineHeight: 1.7, paddingTop: 4 }}>
          <div>в сравнении с 2025</div>
          <div>сформирован 31.12.2026</div>
          <div>стр. 1 из 1</div>
        </div>
      </div>

      {/* KPI-таблица */}
      <div style={{ ...Sans, fontSize: 11, fontWeight: 700, letterSpacing: 1.4, color: sub, marginTop: 28, marginBottom: 14 }}>ОСНОВНЫЕ ПОКАЗАТЕЛИ</div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', borderTop: `1px solid ${line}`, borderLeft: `1px solid ${line}` }}>
        {kpis.map(([label, value, note], i) => (
          <div key={i} style={{ borderRight: `1px solid ${line}`, borderBottom: `1px solid ${line}`, padding: '14px 16px' }}>
            <div style={{ ...Sans, fontSize: 11.5, color: sub, marginBottom: 7 }}>{label}</div>
            <div style={{ ...Mono, fontSize: 21, fontWeight: 800, letterSpacing: -0.5, lineHeight: 1 }}>{value}</div>
            <div style={{ ...Mono, fontSize: 11, color: accent, marginTop: 6 }}>{note}</div>
          </div>
        ))}
      </div>

      {/* Помесячный график */}
      <div style={{ ...Sans, fontSize: 11, fontWeight: 700, letterSpacing: 1.4, color: sub, marginTop: 32, marginBottom: 16 }}>ОТРАБОТАНО ПО МЕСЯЦАМ · Ч</div>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 0, height: 150, borderBottom: `1px solid ${line}`, paddingBottom: 0 }}>
        {YEAR_MONTHS.map((m) => (
          <div key={m.m} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', height: '100%' }}>
            <span style={{ ...Mono, fontSize: 9, color: sub, marginBottom: 4 }}>{m.cur}</span>
            <div style={{ width: '58%', height: `${(m.cur/maxW)*100}%`, background: accent, borderRadius: '3px 3px 0 0' }}/>
          </div>
        ))}
      </div>
      <div style={{ display: 'flex', gap: 0 }}>
        {YEAR_MONTHS.map((m) => (
          <div key={m.m} style={{ flex: 1, textAlign: 'center', ...Mono, fontSize: 9, color: sub, paddingTop: 6 }}>{m.m}</div>
        ))}
      </div>

      {/* Топ направлений */}
      <div style={{ ...Sans, fontSize: 11, fontWeight: 700, letterSpacing: 1.4, color: sub, marginTop: 32, marginBottom: 12 }}>ТОП НАПРАВЛЕНИЙ</div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: `1.5px solid ${ink}` }}>
            <th style={{ ...Sans, fontSize: 11, color: sub, fontWeight: 600, textAlign: 'left', padding: '8px 0' }}>Плечо обслуживания</th>
            <th style={{ ...Sans, fontSize: 11, color: sub, fontWeight: 600, textAlign: 'right', padding: '8px 0', width: 90 }}>Поездок</th>
            <th style={{ ...Sans, fontSize: 11, color: sub, fontWeight: 600, textAlign: 'right', padding: '8px 0', width: 100 }}>Часов</th>
          </tr>
        </thead>
        <tbody>
          {TOP_DIRECTIONS.map((d, i) => (
            <tr key={i} style={{ borderBottom: `1px solid ${line}` }}>
              <td style={{ ...Sans, fontSize: 13.5, fontWeight: 600, padding: '11px 0' }}>{d.from} → {d.to}</td>
              <td style={{ ...Mono, fontSize: 13.5, textAlign: 'right', padding: '11px 0' }}>{d.trips}</td>
              <td style={{ ...Mono, fontSize: 13.5, textAlign: 'right', padding: '11px 0' }}>{d.hours}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Подвал */}
      <div style={{ position: 'absolute', left: 60, right: 60, bottom: 40, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: `1px solid ${line}`, paddingTop: 14, ...Mono, fontSize: 10, color: sub }}>
        <span>Маршрутный лист · экспорт PDF</span>
        <span>Данные приложения · не является официальным документом</span>
      </div>
    </div>
  );
}

// Превью PDF: лист A4 с тенью на сером фоне (как окно «Поделиться / Печать»)
function StatsPdfPreview({ width = 980, height = 1320 }){
  const t = M.light;
  return (
    <div style={{ width, height, background: '#3a3631', display: 'flex', flexDirection: 'column' }}>
      {/* Панель действий, как в просмотре PDF на iOS */}
      <div style={{ height: 60, flexShrink: 0, background: '#2b2825', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 22px' }}>
        <span style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: '#fff' }}>Готово</span>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 700, color: '#fff' }}>Статистика_2026.pdf</div>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: 'rgba(255,255,255,0.55)' }}>1 страница · A4</div>
        </div>
        <span style={{ color: '#fff', display: 'flex' }}><IcShare width="21" height="21"/></span>
      </div>
      {/* Лист */}
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', justifyContent: 'center', padding: '28px 0 40px' }}>
        <div style={{ boxShadow: '0 18px 50px rgba(0,0,0,0.4)', alignSelf: 'flex-start' }}>
          <StatsPdfDoc/>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  IOSStatsA, IOSStatsB, IOSStatsC,
  IOSStatsCompare, IOSStatsYear, IOSStatsEmpty, IOSStatsDetail, IOSStatsDetailYear, IOSStatsLegend,
  IOSStatsHistory, StatsPdfDoc, StatsPdfPreview,
});
