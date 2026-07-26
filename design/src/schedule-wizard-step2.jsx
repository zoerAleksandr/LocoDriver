// Мастер «Заполнить месяц» — Шаг 2
// Выбор первого дня цикла + предпросмотр раскладки смен на месяц.

const WIZ_SICK = new Set([9, 10, 11, 12, 13, 14, 15, 16]);
const WIZ_DAYS = 30;

function WizardStep2({ t, SC, cycle, firstDay }) {
  const layout = [];
  for (let d = 1; d <= WIZ_DAYS; d++) {
    if (WIZ_SICK.has(d)) { layout.push('sick'); continue; }
    if (d < firstDay) { layout.push('off'); continue; }
    const idx = (d - firstDay) % cycle.length;
    layout.push(cycle[idx]);
  }
  const shiftCount = layout.filter(x => x !== 'off' && x !== 'sick').length;

  return (
    <div>
      <SectionLabel t={t}>Первый день цикла</SectionLabel>
      <p style={{ ...M.t.hint(t), margin: '-4px 2px 12px' }}>
        Число месяца, с которого начинается ваш цикл смен.
      </p>
      <WizFirstDayGrid t={t} SC={SC} firstDay={firstDay}/>

      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', margin: '26px 2px 10px' }}>
        <div style={M.t.group(t)}>Предпросмотр · апрель</div>
        <div style={{ fontSize: 12, color: t.textMuted, fontFamily: M.fontMono }}>{shiftCount} смен</div>
      </div>
      <WizPreviewGrid t={t} SC={SC} layout={layout}/>
      <WizLegend t={t} SC={SC}/>
    </div>
  );
}

function WizFirstDayGrid({ t, SC, firstDay }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 6 }}>
      {Array.from({ length: WIZ_DAYS }, (_, i) => i + 1).map(d => {
        const sick = WIZ_SICK.has(d);
        const sel = d === firstDay;
        return (
          <div key={d} style={{
            height: 38, borderRadius: 10,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
            fontSize: 14, fontWeight: 700,
            cursor: sick ? 'not-allowed' : 'pointer',
            background: sel ? t.accent : sick ? SC.sick.soft : t.surface,
            color: sel ? '#fff' : sick ? SC.sick.solid : t.text,
            border: sel ? 'none' : `1px solid ${sick ? SC.sick.solid + '33' : t.border}`,
            opacity: sick ? 0.85 : 1,
          }}>{d}</div>
        );
      })}
    </div>
  );
}

function WizPreviewGrid({ t, SC, layout }) {
  return (
    <div style={{ padding: 12, borderRadius: 16, background: t.surface, border: `1px solid ${t.border}` }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 5 }}>
        {layout.map((type, i) => {
          const c = SC[type];
          const isOff = type === 'off';
          return (
            <div key={i} style={{
              aspectRatio: '1 / 1', borderRadius: 8,
              background: isOff ? t.bgSubtle : c.soft,
              border: `1px solid ${isOff ? 'transparent' : c.mark + '33'}`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              position: 'relative',
            }}>
              <span style={{
                position: 'absolute', top: 2, left: 4,
                fontSize: 8, fontWeight: 700, fontFamily: M.fontMono,
                color: isOff ? t.textFaint : c.mark,
              }}>{i + 1}</span>
              {!isOff && (
                <span style={{ fontSize: 13, fontWeight: 800, lineHeight: 1, color: c.mark }}>
                  {type === 'sick' ? 'Б' : c.short[0]}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function WizLegend({ t, SC }) {
  const items = [['day','Дневная'],['night','Ночная'],['duty','Явка'],['sick','Больничный'],['off','Выходной']];
  return (
    <div style={{
      display: 'flex', flexWrap: 'wrap', gap: '8px 16px', marginTop: 14,
      padding: '12px 14px', borderRadius: 12, background: t.surfaceAlt,
    }}>
      {items.map(([type, label]) => (
        <div key={type} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 12, height: 12, borderRadius: 4, background: type === 'off' ? t.borderStrong : SC[type].solid, border: type === 'off' ? 'none' : `1px solid ${SC[type].stroke}` }}/>
          <span style={{ fontSize: 12, color: t.textMuted }}>{label}</span>
        </div>
      ))}
    </div>
  );
}
