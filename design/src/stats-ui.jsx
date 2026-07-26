// stats-ui.jsx — примитивы раздела «Статистика»
// Все цвета через t, шрифты через M.fontSans/M.fontMono. hexToRgba — глобальный
// из shared-ui. Стиль-объекты именованы (suMetricChip и т.п.).

// ════════════════════════════════════════════════════════════════════
// 1. ИКОНКИ МЕТРИК (которых нет в icons.jsx)
// ════════════════════════════════════════════════════════════════════
const suIcoBase = { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.5, strokeLinecap: 'round', strokeLinejoin: 'round' };
function IcGauge(p){ return (<svg {...suIcoBase} {...p}><path d="M12 13l4-3"/><path d="M4.5 17a9 9 0 1 1 15 0"/><circle cx="12" cy="13" r="1.4" fill="currentColor" stroke="none"/></svg>); }
function IcRoute(p){ return (<svg {...suIcoBase} {...p}><circle cx="6" cy="18" r="2.4"/><circle cx="18" cy="6" r="2.4"/><path d="M8.4 18H14a3.5 3.5 0 0 0 0-7H10a3.5 3.5 0 0 1 0-7h5.6"/></svg>); }
function IcMoon(p){ return (<svg {...suIcoBase} {...p}><path d="M20 14.5A8 8 0 1 1 9.5 4a6.3 6.3 0 0 0 10.5 10.5z"/></svg>); }
function IcArrowUpRight(p){ return (<svg {...suIcoBase} {...p}><path d="M7 17L17 7M9 7h8v8"/></svg>); }
function IcSpeed(p){ return (<svg {...suIcoBase} {...p}><path d="M5 19a9 9 0 1 1 14 0"/><path d="M12 15l4.5-5.5"/><circle cx="12" cy="15" r="1.3" fill="currentColor" stroke="none"/></svg>); }
function IcSeat(p){ return (<svg {...suIcoBase} {...p}><path d="M6 4v8a2 2 0 0 0 2 2h6"/><path d="M6 12l-1 7"/><path d="M14 14l1.5 5"/><path d="M9 8h4"/></svg>); }
function IcChevronDown(p){ return (<svg {...suIcoBase} {...p}><path d="M6 9l6 6 6-6"/></svg>); }
function IcTimer(p){ return (<svg {...suIcoBase} {...p}><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2"/><path d="M9 2h6"/></svg>); }
function IcCompareOff(p){ return (<svg {...suIcoBase} {...p}><path d="M3 7h13M3 7l3-3M3 7l3 3"/><path d="M21 17H8m13 0l-3-3m3 3l-3 3"/><path d="M3 21L21 3" opacity="0.55"/></svg>); }

// Метрика → иконка
const SU_ICONS = {
  clock: (pr)=> <IcClock {...pr}/>,
  gauge: (pr)=> <IcGauge {...pr}/>,
  route: (pr)=> <IcRoute {...pr}/>,
  weight:(pr)=> <IcWeight {...pr}/>,
  moon:  (pr)=> <IcMoon {...pr}/>,
  doc:   (pr)=> <IcDocument {...pr}/>,
  ruble: (pr)=> <IcRuble {...pr}/>,
  speed: (pr)=> <IcSpeed {...pr}/>,
  seat:  (pr)=> <IcSeat {...pr}/>,
  timer: (pr)=> <IcTimer {...pr}/>,
};

// ════════════════════════════════════════════════════════════════════
// 2. ЦВЕТ ОЦЕНОЧНОЙ ДЕЛЬТЫ ПО НОРМЕ
// ════════════════════════════════════════════════════════════════════
// state: 'over' (превышение → danger) | 'ok' (в норме → success) | 'none' (нейтр.)
function normTone(t, state){
  if (state === 'over') return { color: t.danger, bg: hexToRgba(t.danger, 0.12) };
  if (state === 'ok')   return { color: t.success, bg: hexToRgba(t.success, 0.12) };
  return { color: t.textMuted, bg: t.bgSubtle };
}

// ════════════════════════════════════════════════════════════════════
// 3. ДЕЛЬТА-ЧИП
// ════════════════════════════════════════════════════════════════════
// Информационная дельта (kind:'info') — ВСЕГДА монохром (textMuted),
//   стрелка = только направление, без оценки «хорошо/плохо».
// Оценочная дельта (kind:'norm') — семантический цвет (red/green/neutral).
// variant: 'chip' (заливка) | 'plain' (без фона, для компактных строк)
function DeltaTag({ t, delta, variant = 'chip', size = 'md' }){
  const isNorm = delta.kind === 'norm';
  let color, bg, arrow;
  if (isNorm) {
    const tone = normTone(t, delta.state);
    color = tone.color; bg = tone.bg;
    arrow = delta.state === 'over' ? 'up' : delta.state === 'ok' ? 'down' : 'flat';
  } else {
    color = t.textMuted; bg = t.bgSubtle; arrow = delta.dir;
  }
  const fs = size === 'sm' ? 11 : 12;
  const main = delta.pct || delta.abs;
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 3,
      padding: variant === 'chip' ? '3px 8px' : 0,
      borderRadius: M.r.pill,
      background: variant === 'chip' ? bg : 'transparent',
      color, fontFamily: M.fontMono, fontSize: fs, fontWeight: 700, letterSpacing: 0.2,
      whiteSpace: 'nowrap',
    }}>
      <DeltaArrow dir={arrow} size={size === 'sm' ? 9 : 10}/>
      {main}
    </span>
  );
}

// Стрелка направления (вверх/вниз/без изменений)
function DeltaArrow({ dir, size = 10 }){
  if (dir === 'flat') {
    return (<svg width={size} height={size} viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M2 6h8"/></svg>);
  }
  const up = dir === 'up';
  return (
    <svg width={size} height={size} viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
      style={{ transform: up ? 'none' : 'rotate(180deg)' }}>
      <path d="M6 10V2M3 5l3-3 3 3"/>
    </svg>
  );
}

// Подпись «vs прошлый период» под дельтой
function DeltaSub({ t, children }){
  return <span style={{ fontFamily: M.fontSans, fontSize: 11, color: t.textFaint }}>{children}</span>;
}

// ════════════════════════════════════════════════════════════════════
// 4. ПЕРЕКЛЮЧАТЕЛЬ МЕСЯЦ / ГОД (segmented)
// ════════════════════════════════════════════════════════════════════
function PeriodSegment({ t, value = 'month', onChange }){
  const opts = [['month','Месяц'],['year','Год'],['all','История']];
  return (
    <div style={{ display: 'flex', gap: 4, padding: 4, background: t.bgSubtle, borderRadius: 12 }}>
      {opts.map(([k, label]) => {
        const active = k === value;
        return (
          <button key={k} onClick={()=>onChange&&onChange(k)} style={{
            flex: 1, textAlign: 'center', padding: '8px 0', borderRadius: 9, cursor: 'pointer',
            border: 'none', fontFamily: M.fontSans, fontSize: 14, fontWeight: 600,
            background: active ? t.surface : 'transparent',
            color: active ? t.text : t.textMuted,
            boxShadow: active ? M.shadow.sm : 'none',
          }}>{label}</button>
        );
      })}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// 5. НАВИГАЦИЯ ПО ПЕРИОДАМ  ← Март 2026 →
// ════════════════════════════════════════════════════════════════════
function PeriodNav({ t, label, sub, onPrev, onNext, nextDisabled }){
  const Btn = ({ dir, disabled }) => (
    <button onClick={dir==='prev'?onPrev:onNext} disabled={disabled} style={{
      width: 36, height: 36, borderRadius: 18, flexShrink: 0, cursor: disabled ? 'default' : 'pointer',
      background: 'transparent', border: 'none',
      color: disabled ? t.textFaint : t.text, opacity: disabled ? 0.4 : 1,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      {dir==='prev' ? <IcChevronLeft width="20" height="20"/> : <IcChevronRight width="20" height="20"/>}
    </button>
  );
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <Btn dir="prev"/>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 17, fontWeight: 700, color: t.text, letterSpacing: -0.2 }}>{label}</div>
        {sub && <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, letterSpacing: 1, marginTop: 1 }}>{sub}</div>}
      </div>
      <Btn dir="next" disabled={nextDisabled}/>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// 5b. ВЫБОР ПЕРИОДА-ЭТАЛОНА ДЛЯ СРАВНЕНИЯ
// ════════════════════════════════════════════════════════════════════
// Тапабельная строка «в сравнении с …» — открывает шторку выбора.
// none=true — режим «без сравнения»: показываем только выбранный период.
function CompareSelector({ t, title, none, onOpen }){
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '2px 2px 12px' }}>
      <span style={{ fontFamily: M.fontSans, fontSize: 12.5, color: t.textMuted, whiteSpace: 'nowrap' }}>
        {none ? 'режим' : 'в сравнении с'}
      </span>
      <button onClick={onOpen} style={{
        display: 'inline-flex', alignItems: 'center', gap: 5, padding: '5px 8px 5px 11px',
        borderRadius: M.r.pill, border: `1px solid ${none ? t.border : t.accent}`,
        background: none ? t.surface : t.accentSoft, cursor: 'pointer', boxShadow: M.shadow.sm,
      }}>
        <span style={{ fontFamily: M.fontSans, fontSize: 12.5, fontWeight: 700, color: none ? t.text : t.accent, whiteSpace: 'nowrap' }}>
          {none ? 'Без сравнения' : title}
        </span>
        <IcChevronDown width="14" height="14" style={{ color: none ? t.textMuted : t.accent }}/>
      </button>
    </div>
  );
}

// Внутренняя обёртка шторки (ручка + опц. кнопка «назад» + заголовок).
function SheetShell({ t, title, sub, onClose, onBack, children }){
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 80 }}>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(20,18,14,0.42)' }}/>
      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, maxHeight: '88%', display: 'flex', flexDirection: 'column', background: t.bg, borderRadius: '24px 24px 0 0', boxShadow: '0 -20px 60px rgba(0,0,0,0.28)', padding: '12px 16px 28px' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 12 }}>
          <div style={{ width: 40, height: 5, borderRadius: 3, background: t.borderStrong }}/>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: sub ? 4 : 14 }}>
          {onBack && (
            <button onClick={onBack} style={{ border: 'none', background: 'transparent', color: t.accent, display: 'inline-flex', alignItems: 'center', gap: 2, fontSize: 15, padding: '2px 6px 2px 0', cursor: 'pointer', marginLeft: -4 }}>
              <IcChevronLeft width="20" height="20"/>
            </button>
          )}
          <div style={{ fontFamily: M.fontSans, fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -0.3 }}>{title}</div>
        </div>
        {sub && <div style={{ fontFamily: M.fontSans, fontSize: 13, color: t.textMuted, lineHeight: 1.4, marginBottom: 14 }}>{sub}</div>}
        <div style={{ overflowY: 'auto', minHeight: 0 }}>{children}</div>
      </div>
    </div>
  );
}

// Шторка-список вариантов сравнения (radio). options: COMPARE_MONTH / COMPARE_YEAR.
// onAction(id) — для пунктов action:true (открыть пикер месяца/года).
function ComparePicker({ t, title, sub, options, selectedId, onSelect, onAction, onClose, onBack }){
  return (
    <SheetShell t={t} title={title} sub={sub || 'С чем сравнивать выбранный период'} onClose={onClose} onBack={onBack}>
      <div style={{ background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, overflow: 'hidden' }}>
        {options.map((o, i) => {
          const active = o.id === selectedId;
          return (
            <button key={o.id} onClick={()=>{ if(o.action){ onAction && onAction(o.id); } else { onSelect(o.id); onClose(); } }} style={{
              width: '100%', display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px', cursor: 'pointer',
              border: 'none', background: active ? t.accentSoft : 'transparent', textAlign: 'left',
              borderBottom: i === options.length - 1 ? 'none' : `1px solid ${t.border}`,
            }}>
              {o.action ? (
                <span style={{ width: 22, height: 22, flexShrink: 0, color: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <IcChevronRight width="18" height="18"/>
                </span>
              ) : o.off ? (
                <span style={{ width: 22, height: 22, flexShrink: 0, color: active ? t.accent : t.textMuted, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <IcCompareOff width="20" height="20"/>
                </span>
              ) : (
                <span style={{
                  width: 20, height: 20, borderRadius: 10, flexShrink: 0, boxSizing: 'border-box',
                  border: active ? `6px solid ${t.accent}` : `2px solid ${t.borderStrong}`,
                }}/>
              )}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: o.action ? t.accent : t.text }}>{o.title}</div>
                {o.note && <div style={{ fontFamily: M.fontSans, fontSize: 12, color: t.textMuted, marginTop: 1 }}>{o.note}</div>}
              </div>
            </button>
          );
        })}
      </div>
    </SheetShell>
  );
}

// Пикер «выбрать месяц» — сетка месяцев по годам. Будущие и текущий — недоступны.
function MonthGridPicker({ t, selectedLabel, onPick, onClose, onBack }){
  return (
    <SheetShell t={t} title="Выбрать месяц" sub="С каким месяцем сравнить Март 2026" onClose={onClose} onBack={onBack}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        {PICK_MONTH_YEARS.map(({ year }) => (
          <div key={year}>
            <div style={{ fontFamily: M.fontMono, fontSize: 12, fontWeight: 700, color: t.textMuted, letterSpacing: 1, marginBottom: 10 }}>{year}</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
              {MONTH_NAMES_SH.map((nm, mi) => {
                const monthNo = mi + 1;
                const disabled = year > PICK_CUR.y || (year === PICK_CUR.y && monthNo >= PICK_CUR.m);
                const label = `${MONTH_NAMES_FULL[mi]} ${year}`;
                const active = label === selectedLabel;
                return (
                  <button key={mi} disabled={disabled} onClick={()=>{ onPick(label); onClose(); }} style={{
                    padding: '12px 0', borderRadius: 12, cursor: disabled ? 'default' : 'pointer',
                    border: active ? `1.5px solid ${t.accent}` : `1px solid ${t.border}`,
                    background: active ? t.accent : t.surface,
                    color: disabled ? t.textFaint : active ? t.accentInk : t.text,
                    opacity: disabled ? 0.4 : 1,
                    fontFamily: M.fontSans, fontSize: 14, fontWeight: 600,
                  }}>{nm}</button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </SheetShell>
  );
}

// Пикер «выбрать год» — список прошлых лет.
function YearListPicker({ t, selectedLabel, onPick, onClose, onBack }){
  return (
    <SheetShell t={t} title="Выбрать год" sub="С каким годом сравнить 2026 год" onClose={onClose} onBack={onBack}>
      <div style={{ background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, overflow: 'hidden' }}>
        {PICK_YEARS.map((y, i) => {
          const label = `${y} год`;
          const active = label === selectedLabel;
          return (
            <button key={y} onClick={()=>{ onPick(label); onClose(); }} style={{
              width: '100%', display: 'flex', alignItems: 'center', gap: 12, padding: '15px 16px', cursor: 'pointer',
              border: 'none', background: active ? t.accentSoft : 'transparent', textAlign: 'left',
              borderBottom: i === PICK_YEARS.length - 1 ? 'none' : `1px solid ${t.border}`,
            }}>
              <span style={{
                width: 20, height: 20, borderRadius: 10, flexShrink: 0, boxSizing: 'border-box',
                border: active ? `6px solid ${t.accent}` : `2px solid ${t.borderStrong}`,
              }}/>
              <span style={{ flex: 1, fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>{label}</span>
            </button>
          );
        })}
      </div>
    </SheetShell>
  );
}

// ════════════════════════════════════════════════════════════════════
// 6. СПАРКЛАЙН (мини-линия динамики в карточке)
// ════════════════════════════════════════════════════════════════════
function Sparkline({ t, data, color, w = 96, h = 30, dot = true }){
  const min = Math.min(...data), max = Math.max(...data);
  const span = (max - min) || 1;
  const stepX = w / (data.length - 1);
  const pts = data.map((v, i) => [i * stepX, h - 3 - ((v - min) / span) * (h - 6)]);
  const d = pts.map((p, i) => (i ? 'L' : 'M') + p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join(' ');
  const c = color || t.accent;
  const last = pts[pts.length - 1];
  return (
    <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} style={{ display: 'block', overflow: 'visible' }}>
      <path d={d} fill="none" stroke={c} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
      {dot && <circle cx={last[0]} cy={last[1]} r="2.6" fill={c}/>}
    </svg>
  );
}

// ════════════════════════════════════════════════════════════════════
// 7. ПАРНЫЕ СТОЛБЦЫ «было / стало» (мини, внутри карточки)
// ════════════════════════════════════════════════════════════════════
// prev — приглушённый, cur — акцент (или семантика по норме).
function MiniPair({ t, prev, cur, curColor, h = 34 }){
  const max = Math.max(prev, cur) || 1;
  const c = curColor || t.accent;
  // Столбик «было» — нейтральный, но видимый в обеих темах (в тёмной
  // t.bgSubtle почти сливается с поверхностью, поэтому берём тон от текста).
  const prevFill = hexToRgba(t.text, 0.22);
  const Bar = ({ v, fill }) => (
    <div style={{ width: 12, height: h, display: 'flex', alignItems: 'flex-end' }}>
      <div style={{ width: '100%', height: `${Math.max(6,(v/max)*100)}%`, background: fill, borderRadius: 3 }}/>
    </div>
  );
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 5 }}>
      <Bar v={prev} fill={prevFill}/>
      <Bar v={cur} fill={c}/>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// 7b. ПЛАШКА МЕТРИКИ (прямоугольная карточка) — общая для «Месяц» и «Год»
// ════════════════════════════════════════════════════════════════════
// compare=false — режим без сравнения: прячем мини-пару, дельту и «было».
function MetricPlate({ t, icon, label, value, unit, prev, delta, spark, wide, compare = true }){
  const isNorm = delta && delta.kind === 'norm';
  const curColor = isNorm ? normTone(t, delta.state).color : t.accent;
  const sp = spark || [];
  const prevN = sp.length ? Math.abs(sp[sp.length - 2]) : 0;
  const curN = sp.length ? Math.abs(sp[sp.length - 1]) : 1;
  return (
    <div style={{ gridColumn: wide ? 'span 2' : 'span 1', background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm, padding: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: t.textMuted, marginBottom: wide ? 10 : 12 }}>
        <span style={{ color: t.accent, display: 'flex' }}>{SU_ICONS[icon]({ width: 17, height: 17 })}</span>
        <span style={{ fontFamily: M.fontSans, fontSize: 12.5, fontWeight: 600, color: t.textMuted }}>{label}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 8 }}>
        <div>
          <div style={{ fontFamily: M.fontMono, fontSize: wide ? 40 : 26, fontWeight: 800, color: t.text, letterSpacing: -1, lineHeight: 1, whiteSpace: 'nowrap' }}>{value}</div>
          {unit && <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted, marginTop: 4, whiteSpace: 'nowrap' }}>{unit}</div>}
        </div>
        {compare && spark && <MiniPair t={t} prev={prevN} cur={curN} curColor={curColor} h={wide ? 44 : 34}/>}
      </div>
      {compare && delta && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 12 }}>
          <DeltaTag t={t} delta={delta} variant="plain" size="sm"/>
          {prev && <span style={{ fontFamily: M.fontSans, fontSize: 11, color: t.textFaint }}>было {prev}</span>}
        </div>
      )}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// 8. БЛОК ПАРНЫХ СТОЛБЦОВ ПО МЕТРИКАМ (полноширинный график)
// ════════════════════════════════════════════════════════════════════
function PairedBarsChart({ t, bars, curLabel = 'Март', prevLabel = 'Февраль' }){
  return (
    <div>
      {/* Легенда */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
        <LegendDot t={t} color={t.bgSubtle} label={prevLabel} border/>
        <LegendDot t={t} color={t.accent} label={curLabel}/>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {bars.map(b => {
          const max = Math.max(b.cur, b.prev) || 1;
          return (
            <div key={b.key}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 7 }}>
                <span style={{ fontFamily: M.fontSans, fontSize: 13, color: t.textMuted }}>{b.label}</span>
                <span style={{ fontFamily: M.fontMono, fontSize: 13, fontWeight: 700, color: t.text, whiteSpace: 'nowrap' }}>
                  {b.fmt(b.cur)}
                  <span style={{ color: t.textFaint, fontWeight: 500 }}> / {b.fmt(b.prev)}</span>
                </span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <HBar pct={(b.prev/max)*100} fill={t.bgSubtle}/>
                <HBar pct={(b.cur/max)*100} fill={t.accent}/>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
function HBar({ pct, fill }){
  return (
    <div style={{ height: 10, borderRadius: 5, background: 'transparent' }}>
      <div style={{ width: `${Math.max(2,pct)}%`, height: '100%', background: fill, borderRadius: 5 }}/>
    </div>
  );
}
function LegendDot({ t, color, label, border }){
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontFamily: M.fontSans, fontSize: 12, color: t.textMuted }}>
      <span style={{ width: 10, height: 10, borderRadius: 3, background: color, border: border ? `1px solid ${t.borderStrong}` : 'none' }}/>
      {label}
    </span>
  );
}

// ════════════════════════════════════════════════════════════════════
// 9. ГОДОВОЙ ГРАФИК — 12 месяцев, текущий vs прошлый год
// ════════════════════════════════════════════════════════════════════
// Сгруппированные столбцы (cur поверх/рядом prev). Подписи через один месяц.
function YearBarsChart({ t, months, h = 150, overlayPrev = true }){
  const max = Math.max(...months.map(m => Math.max(m.cur, m.prev))) * 1.05;
  return (
    <div>
      <div style={{ display: 'flex', gap: 16, marginBottom: 14 }}>
        <LegendDot t={t} color={t.accent} label="2026"/>
        {overlayPrev && <LegendDot t={t} color={t.bgSubtle} label="2025" border/>}
      </div>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 0, height: h }}>
        {months.map((m, i) => (
          <div key={m.m} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', height: '100%' }}>
            <div style={{ flex: 1, width: '100%', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: 2 }}>
              {overlayPrev && (
                <div title="2025" style={{ width: 5, height: `${(m.prev/max)*100}%`, background: hexToRgba(t.text, 0.22), borderRadius: 2 }}/>
              )}
              <div title="2026" style={{ width: overlayPrev ? 6 : '100%', maxWidth: overlayPrev ? 6 : 18, height: `${(m.cur/max)*100}%`, background: t.accent, borderRadius: overlayPrev ? 2 : '4px 4px 0 0' }}/>
            </div>
            <div style={{ fontFamily: M.fontMono, fontSize: 8.5, color: i % 2 ? t.textFaint : t.textMuted, marginTop: 6, height: 10 }}>
              {i % 2 === 0 ? m.m : ''}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// Годовой ЛИНЕЙНЫЙ график (для концепта с линией динамики)
function YearLineChart({ t, months, h = 150 }){
  const w = 326;
  const all = months.flatMap(m => [m.cur, m.prev]);
  const min = Math.min(...all) * 0.96, max = Math.max(...all) * 1.02;
  const span = (max - min) || 1;
  const stepX = w / (months.length - 1);
  const toPts = (key) => months.map((m, i) => [i * stepX, h - ((m[key] - min) / span) * h]);
  const line = (pts) => pts.map((p, i) => (i ? 'L' : 'M') + p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join(' ');
  const cur = toPts('cur'), prev = toPts('prev');
  return (
    <div>
      <div style={{ display: 'flex', gap: 16, marginBottom: 12 }}>
        <LegendLine t={t} color={t.accent} label="2026"/>
        <LegendLine t={t} color={t.borderStrong} label="2025" dashed/>
      </div>
      <svg width="100%" viewBox={`0 0 ${w} ${h + 18}`} style={{ display: 'block', overflow: 'visible' }}>
        <path d={line(prev)} fill="none" stroke={t.borderStrong} strokeWidth="2" strokeDasharray="4 4" strokeLinejoin="round" strokeLinecap="round"/>
        <path d={line(cur)} fill="none" stroke={t.accent} strokeWidth="2.4" strokeLinejoin="round" strokeLinecap="round"/>
        {cur.map((p, i) => i % 2 === 0 && (
          <text key={i} x={p[0]} y={h + 14} textAnchor="middle" fontFamily={M.fontMono} fontSize="8.5" fill={t.textFaint}>{months[i].m}</text>
        ))}
        <circle cx={cur[cur.length-1][0]} cy={cur[cur.length-1][1]} r="3" fill={t.accent}/>
      </svg>
    </div>
  );
}
function LegendLine({ t, color, label, dashed }){
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontFamily: M.fontSans, fontSize: 12, color: t.textMuted }}>
      <svg width="18" height="6"><line x1="0" y1="3" x2="18" y2="3" stroke={color} strokeWidth="2.4" strokeDasharray={dashed ? '4 3' : 'none'} strokeLinecap="round"/></svg>
      {label}
    </span>
  );
}

// ════════════════════════════════════════════════════════════════════
// 10. ТОП НАПРАВЛЕНИЙ (рейтинг плеч: разы + часы)
// ════════════════════════════════════════════════════════════════════
function TopDirections({ t, items }){
  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      {items.map((d, i) => (
        <React.Fragment key={i}>
          {i > 0 && <div style={{ height: 1, background: t.border }}/>}
          <div style={{ padding: '13px 0' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
              <span style={{
                width: 20, height: 20, borderRadius: 6, flexShrink: 0, background: i===0 ? t.accent : t.bgSubtle,
                color: i===0 ? t.accentInk : t.textMuted, fontFamily: M.fontMono, fontSize: 11, fontWeight: 700,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>{i+1}</span>
              <span style={{ flex: 1, minWidth: 0, fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: t.text, display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{d.from}</span>
                <IcArrowUpRight width="12" height="12" style={{ color: t.textFaint, flexShrink: 0, transform: 'rotate(45deg)' }}/>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{d.to}</span>
              </span>
              <span style={{ fontFamily: M.fontMono, fontSize: 13, fontWeight: 700, color: t.text, whiteSpace: 'nowrap' }}>{d.trips}<span style={{ color: t.textFaint, fontWeight: 500 }}> раз</span></span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 30 }}>
              <div style={{ flex: 1, height: 6, background: t.bgSubtle, borderRadius: 3, overflow: 'hidden' }}>
                <div style={{ width: `${d.pct}%`, height: '100%', background: i===0 ? t.accent : hexToRgba(t.accent, 0.4), borderRadius: 3 }}/>
              </div>
              <span style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, whiteSpace: 'nowrap' }}>{d.hours} ч</span>
            </div>
          </div>
        </React.Fragment>
      ))}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// 10b. КРУГОВАЯ ДИАГРАММА (donut) + легенда строками
// ════════════════════════════════════════════════════════════════════
// Категориальная палитра — разные тона, чтобы секторы чётко различались.
// Синий-бренд ведёт, дальше — бирюза / янтарь / коралл / фиолет.
const DONUT_RAMP = ['#0079C2', '#17B0A8', '#F2A33C', '#E5683C', '#7C6CD6'];

// segments: [{ label, value, sub?, color? }]
// centerMain / centerSub — крупная подпись в центре кольца.
function DonutChart({ t, segments, size = 150, thickness = 26, gap = 3, centerMain, centerSub }){
  const total = segments.reduce((s, x) => s + x.value, 0) || 1;
  const r = (size - thickness) / 2;
  const circ = 2 * Math.PI * r;
  const cx = size / 2;
  let acc = 0;
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ flexShrink: 0, display: 'block' }}>
      <circle cx={cx} cy={cx} r={r} fill="none" stroke={t.bgSubtle} strokeWidth={thickness}/>
      <g transform={`rotate(-90 ${cx} ${cx})`}>
        {segments.map((s, i) => {
          const frac = s.value / total;
          const seg = frac * circ;
          const dash = Math.max(0.6, seg - gap);
          const offset = -acc * circ;
          acc += frac;
          return (
            <circle key={i} cx={cx} cy={cx} r={r} fill="none"
              stroke={s.color || DONUT_RAMP[i % DONUT_RAMP.length]}
              strokeWidth={thickness} strokeLinecap="butt"
              strokeDasharray={`${dash} ${circ - dash}`}
              strokeDashoffset={offset}/>
          );
        })}
      </g>
      {centerMain && (
        <text x={cx} y={cx - 2} textAnchor="middle" fontFamily={M.fontMono} fontSize={size > 130 ? 21 : 17} fontWeight="800" fill={t.text} letterSpacing="-0.5">{centerMain}</text>
      )}
      {centerSub && (
        <text x={cx} y={cx + (size > 130 ? 16 : 14)} textAnchor="middle" fontFamily={M.fontSans} fontSize="10.5" fill={t.textMuted}>{centerSub}</text>
      )}
    </svg>
  );
}

// Легенда-строки к donut: цветной маркер · подпись · значение
function DonutLegend({ t, segments, fmtVal }){
  const total = segments.reduce((s, x) => s + x.value, 0) || 1;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 11, flex: 1, minWidth: 0 }}>
      {segments.map((s, i) => {
        const pct = Math.round((s.value / total) * 100);
        return (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 9, minWidth: 0 }}>
            <span style={{ width: 9, height: 9, borderRadius: 3, flexShrink: 0, background: s.color || DONUT_RAMP[i % DONUT_RAMP.length] }}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontFamily: M.fontSans, fontSize: 13, fontWeight: 600, color: t.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.label}</div>
              {s.sub && <div style={{ fontFamily: M.fontMono, fontSize: 10.5, color: t.textFaint, marginTop: 1 }}>{s.sub}</div>}
            </div>
            <span style={{ fontFamily: M.fontMono, fontSize: 13, fontWeight: 700, color: t.text, whiteSpace: 'nowrap' }}>{pct}<span style={{ color: t.textFaint, fontWeight: 500 }}>%</span></span>
          </div>
        );
      })}
    </div>
  );
}

// Легенда-строки на всю ширину: маркер · полная подпись (не обрезается) · доля
function DonutLegendRows({ t, segments }){
  const total = segments.reduce((s, x) => s + x.value, 0) || 1;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {segments.map((s, i) => {
        const pct = Math.round((s.value / total) * 100);
        return (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '9px 0', borderTop: i ? `1px solid ${t.border}` : 'none' }}>
            <span style={{ width: 11, height: 11, borderRadius: 3, flexShrink: 0, background: s.color || DONUT_RAMP[i % DONUT_RAMP.length] }}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontFamily: M.fontSans, fontSize: 13.5, fontWeight: 600, color: t.text, lineHeight: 1.25 }}>{s.label}</div>
              {s.sub && <div style={{ fontFamily: M.fontMono, fontSize: 10.5, color: t.textFaint, marginTop: 2 }}>{s.sub}</div>}
            </div>
            <span style={{ fontFamily: M.fontMono, fontSize: 14, fontWeight: 700, color: t.text, whiteSpace: 'nowrap' }}>{pct}<span style={{ color: t.textFaint, fontWeight: 500 }}>%</span></span>
          </div>
        );
      })}
    </div>
  );
}

// Готовый блок «donut + легенда»: кольцо по центру сверху, расшифровка ниже на всю ширину
function DonutBreakdown({ t, segments, centerMain, centerSub, size = 150 }){
  return (
    <div style={{ padding: '14px 2px 6px' }}>
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 16 }}>
        <DonutChart t={t} segments={segments} size={size} thickness={26} centerMain={centerMain} centerSub={centerSub}/>
      </div>
      <DonutLegendRows t={t} segments={segments}/>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// 11. ЛЕГЕНДА ЦВЕТОВОЙ ЛОГИКИ ДЕЛЬТ
// ════════════════════════════════════════════════════════════════════
function DeltaLegend({ t }){
  const Item = ({ color, bg, title, desc, arrow }) => (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, padding: '12px 0' }}>
      <span style={{
        display: 'inline-flex', alignItems: 'center', gap: 3, padding: '3px 8px', borderRadius: M.r.pill,
        background: bg, color, fontFamily: M.fontMono, fontSize: 12, fontWeight: 700, flexShrink: 0, marginTop: 1,
      }}>
        {arrow !== 'none' && <DeltaArrow dir={arrow} size={10}/>}
        {arrow === 'none' ? '—' : '+6%'}
      </span>
      <div>
        <div style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: t.text }}>{title}</div>
        <div style={{ fontFamily: M.fontSans, fontSize: 12.5, color: t.textMuted, lineHeight: 1.4, marginTop: 2 }}>{desc}</div>
      </div>
    </div>
  );
  return (
    <div>
      <Item color={t.danger} bg={hexToRgba(t.danger,0.12)} arrow="up"
        title="Превышение нормы" desc="Оценочная дельта: факт больше нормы за период."/>
      <div style={{ height: 1, background: t.border }}/>
      <Item color={t.success} bg={hexToRgba(t.success,0.12)} arrow="down"
        title="В пределах нормы" desc="Оценочная дельта: факт не превышает норму."/>
      <div style={{ height: 1, background: t.border }}/>
      <Item color={t.textMuted} bg={t.bgSubtle} arrow="up"
        title="Информационная дельта" desc="Путь, ткм, ночные: просто больше / меньше прошлого периода. Цветом не оцениваем."/>
    </div>
  );
}

Object.assign(window, {
  IcGauge, IcRoute, IcMoon, IcArrowUpRight, IcChevronDown, IcTimer, IcCompareOff, SU_ICONS, normTone,
  DeltaTag, DeltaArrow, DeltaSub,
  PeriodSegment, PeriodNav, CompareSelector, SheetShell, ComparePicker, MonthGridPicker, YearListPicker,
  Sparkline, MiniPair, MetricPlate, PairedBarsChart, HBar, LegendDot,
  YearBarsChart, YearLineChart, LegendLine,
  TopDirections, DeltaLegend,
  DonutChart, DonutLegend, DonutLegendRows, DonutBreakdown, DONUT_RAMP,
});
