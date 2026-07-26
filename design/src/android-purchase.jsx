// ═══════════════════════════════════════════════════════════════
// ANDROID · ПОКУПКИ — Машинист Pro (Material 3)
// Вариант A (как и на iOS): витрина тарифов · активная подписка ·
// продление (Material bottom sheet) · подписка истекла.
//   • ADevice + Material small top app bar (✕ + «Восстановить»)
//   • Material outlined plan-cards с radio (tonal-заливка при выборе)
//   • Material filled button снизу (полноширинный)
//   • Material bottom sheet радиус 28
// Переиспользуем ProHero и FeatureRow из purchase-screens.jsx.
// ═══════════════════════════════════════════════════════════════

const A_PRO_FEATURES = [
  ['Облачная копия и синхронизация', <IcCloud/>],
  ['Экспорт в PDF', <IcPdf/>],
  ['Безлимит маршрутов и истории', <IcDocument/>],
];

// Material small top app bar для экранов покупки (✕ слева, текст-действие справа).
function APaywallTopBar({ t, restore = true }) {
  return (
    <div style={{ padding: '8px 4px', display: 'flex', alignItems: 'center', gap: 4, background: t.bg }}>
      <button style={{
        width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
      }} aria-label="Закрыть">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>
      </button>
      <div style={{ flex: 1 }}/>
      {restore && (
        <button style={{
          border: 'none', background: 'transparent', color: t.accent,
          fontSize: 14, fontWeight: 600, cursor: 'pointer', padding: '0 12px', height: 40,
        }}>Восстановить</button>
      )}
    </div>
  );
}

// Material plan-card: outlined, radio слева, tonal-заливка при выборе.
function AndroidPlanCard({ t, term, total, perMonth, badge, selected }) {
  return (
    <button style={{
      width: '100%', textAlign: 'left', cursor: 'pointer',
      background: selected ? t.accentSoft : t.surface, borderRadius: 16,
      border: `1.5px solid ${selected ? t.accent : t.border}`,
      boxShadow: 'none', padding: '16px', display: 'flex', alignItems: 'center', gap: 14,
    }}>
      <div style={{
        width: 22, height: 22, borderRadius: 11, flexShrink: 0,
        border: `2px solid ${selected ? t.accent : t.borderStrong}`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{selected && <div style={{ width: 12, height: 12, borderRadius: 6, background: t.accent }}/>}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 16, fontWeight: 700, color: t.text }}>{term}</span>
          {badge && <Pill t={t} tone="success" mono={false} style={{ fontSize: 11, padding: '2px 8px', whiteSpace: 'nowrap' }}>{badge}</Pill>}
        </div>
        <div style={{ ...M.t.captionMuted(t), marginTop: 3, fontFamily: M.fontMono }}>{perMonth} ₽/мес</div>
      </div>
      <div style={{ fontFamily: M.fontMono, fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -0.5, whiteSpace: 'nowrap', flexShrink: 0 }}>{total} ₽</div>
    </button>
  );
}

function aPayCta(t) {
  return {
    width: '100%', height: 54, borderRadius: 16, border: 'none', cursor: 'pointer',
    background: t.cta, color: t.ctaInk, fontSize: 16, fontWeight: 700,
    boxShadow: '0 8px 24px rgba(0,0,0,0.20)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
  };
}

function APlanList({ t, selected }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <AndroidPlanCard t={t} term="Год" total="1 490" perMonth="124" badge="−38 %" selected={selected === 'year'}/>
      <AndroidPlanCard t={t} term="3 месяца" total="499" perMonth="166" badge="−17 %" selected={selected === 'q'}/>
      <AndroidPlanCard t={t} term="Месяц" total="199" perMonth="199" selected={selected === 'month'}/>
    </div>
  );
}

// Закреплённая снизу CTA + подпись (Material).
function APinnedCta({ t, label, note }) {
  return (
    <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 50, padding: '0 16px 24px', background: t.bg }}>
      <div style={{ height: 20, marginTop: -20, background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})` }}/>
      <button style={aPayCta(t)}>{label}</button>
      <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>{note}</div>
    </div>
  );
}

// ── Витрина тарифов (подписка не куплена) ───────────────────────
function AndroidPaywall({ dark = false, height = 1040, selected = 'year' }) {
  const t = dark ? M.dark : M.light;
  const ctaLabel = { month: 'Оформить за 199 ₽/мес', q: 'Оформить за 499 ₽', year: 'Оформить за 1 490 ₽/год' }[selected];
  return (
    <ADevice dark={dark} height={height}>
      <APaywallTopBar t={t} restore/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px - 110px)', padding: '4px 16px 24px' }}>
        <div style={{ borderRadius: 16, overflow: 'hidden', boxShadow: M.shadow.sm }}>
          <ProHero t={t} flush/>
          <div style={{ background: t.surface, padding: '6px 18px' }}>
            {A_PRO_FEATURES.map(([label, icon], i) => <FeatureRow key={i} t={t} label={label} icon={icon}/>)}
          </div>
        </div>

        <AGroup t={t}>Выберите тариф</AGroup>
        <APlanList t={t} selected={selected}/>
      </div>
      <APinnedCta t={t} label={ctaLabel} note="Первые 20 маршрутов — бесплатно. Дальше добавление маршрутов — только по подписке."/>
    </ADevice>
  );
}

// ── Активная подписка (управление). daysLeft → баннер «скоро истечёт» ──
function AndroidPaywallActive({ dark = false, height = 820, renewsOn = '14.02.2027', daysLeft = null }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark} height={height}>
      <APaywallTopBar t={t} restore={false}/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px - 110px)', padding: '4px 16px 24px' }}>
        <ProHero t={t}/>

        {/* Статус активной подписки — Material tonal */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 13, marginTop: 16,
          padding: '14px 16px', borderRadius: 16,
          background: hexToRgba(t.success, 0.10), border: `1px solid ${hexToRgba(t.success, 0.25)}`,
        }}>
          <div style={{
            width: 40, height: 40, borderRadius: 12, flexShrink: 0,
            background: hexToRgba(t.success, 0.16), color: t.success,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={t.success} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12l5 5L20 7"/></svg>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: t.text }}>Подписка активна</div>
            <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>
              Действует до <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{renewsOn}</span>
            </div>
          </div>
        </div>

        {daysLeft != null && (
          <div style={{
            marginTop: 10, padding: '14px 15px', borderRadius: 16,
            background: hexToRgba(t.warning, 0.10), border: `1px solid ${hexToRgba(t.warning, 0.30)}`,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{ width: 38, height: 38, borderRadius: 11, flexShrink: 0, background: t.warning, color: '#1b1300', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v4l3 2"/></svg>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 700, color: t.text }}>Подписка истекает через {daysLeft} дня</div>
                <div style={{ ...M.t.captionMuted(t), marginTop: 2, lineHeight: 1.4 }}>Продлите заранее — новый срок прибавится к текущему, дни не сгорят.</div>
              </div>
            </div>
          </div>
        )}

        <AGroup t={t}>Текущий тариф</AGroup>
        <MCard t={t}>
          <FieldRow t={t} label="Тариф" value="Годовая"/>
          <Sep t={t} inset={20}/>
          <FieldRow t={t} label="Стоимость" value="1 490 ₽/год" mono/>
          <Sep t={t} inset={20}/>
          <FieldRow t={t} label="Действует до" value={renewsOn} mono/>
        </MCard>
      </div>
      <APinnedCta t={t} label="Продлить срок" note="Новый период прибавится к текущему сроку."/>
    </ADevice>
  );
}

// ── Подписка истекла (возобновление) ────────────────────────────
function AndroidPaywallExpired({ dark = false, height = 1040, selected = 'year', expiredOn = '12.05.2026' }) {
  const t = dark ? M.dark : M.light;
  const red = dark ? '#FF6575' : '#C81E14';
  return (
    <ADevice dark={dark} height={height}>
      <APaywallTopBar t={t} restore={false}/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px - 110px)', padding: '4px 16px 24px' }}>
        <ProHero t={t}/>

        <div style={{
          display: 'flex', alignItems: 'center', gap: 13, marginTop: 16,
          padding: '14px 16px', borderRadius: 16,
          background: hexToRgba(red, 0.12), border: `1px solid ${hexToRgba(red, 0.34)}`,
        }}>
          <div style={{ width: 40, height: 40, borderRadius: 12, flexShrink: 0, background: red, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5M12 16h.01"/></svg>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: t.text }}>Подписка истекла</div>
            <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>
              Закончилась <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{expiredOn}</span>
            </div>
          </div>
        </div>

        <div style={{ background: hexToRgba(red, 0.05), borderRadius: 14, padding: '12px 16px', marginTop: 10 }}>
          <div style={{ ...M.t.captionMuted(t), lineHeight: 1.5 }}>
            Маршруты и история сохранены. Но добавлять новые сверх 20 и пользоваться синхронизацией нельзя, пока подписка не возобновлена.
          </div>
        </div>

        <AGroup t={t}>Выберите тариф</AGroup>
        <APlanList t={t} selected={selected}/>
      </div>
      <APinnedCta t={t} label="Возобновить подписку" note="Возобновление начнётся с сегодняшнего дня."/>
    </ADevice>
  );
}

// ── Продление — Material bottom sheet (радиус 28) ───────────────
function AndroidRenewSheet({ dark = false, height = 1040, selected = 'year', currentEnd = '14.02.2027' }) {
  const t = dark ? M.dark : M.light;
  const plan = {
    year:  { add: '+ 1 год',    newEnd: '14.02.2028', total: '1 490' },
    q:     { add: '+ 3 месяца', newEnd: '14.05.2027', total: '499' },
    month: { add: '+ 1 месяц',  newEnd: '14.03.2027', total: '199' },
  }[selected];
  const DateChunk = ({ cap, date, accent }) => (
    <div style={{ flex: 1, textAlign: 'center', padding: '2px 4px' }}>
      <div style={{ fontFamily: M.fontMono, fontSize: 10.5, fontWeight: 700, letterSpacing: 0.5, color: t.textMuted, textTransform: 'uppercase' }}>{cap}</div>
      <div style={{ fontFamily: M.fontMono, fontSize: 16, fontWeight: 700, marginTop: 5, color: accent ? t.accent : t.text }}>{date}</div>
    </div>
  );
  return (
    <ADevice dark={dark} height={height}>
      <div style={{ position: 'absolute', inset: 0, background: dark ? '#05070b' : '#11141A' }}/>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.45)' }}/>

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 10,
        background: t.bg, borderTopLeftRadius: 28, borderTopRightRadius: 28,
        padding: '12px 16px 28px', boxShadow: '0 -20px 60px rgba(0,0,0,0.35)',
      }}>
        <div style={{ width: 32, height: 4, borderRadius: 2, background: t.borderStrong, margin: '0 auto 16px' }}/>
        <div style={{ fontSize: 22, fontWeight: 600, color: t.text, textAlign: 'center' }}>Продление подписки</div>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 7, lineHeight: 1.45, padding: '0 14px' }}>
          Новый период не заменяет текущий, а&nbsp;прибавляется к&nbsp;нему — дни не сгорают.
        </div>

        <div style={{
          display: 'flex', alignItems: 'center', gap: 6, marginTop: 18,
          background: t.surface, borderRadius: 16, boxShadow: M.shadow.sm, padding: '16px 10px',
        }}>
          <DateChunk cap="Сейчас до" date={currentEnd}/>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, flexShrink: 0 }}>
            <span style={{ fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, color: t.accent, whiteSpace: 'nowrap' }}>{plan.add}</span>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke={t.accent} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h15M13 6l6 6-6 6"/></svg>
          </div>
          <DateChunk cap="Станет до" date={plan.newEnd} accent/>
        </div>

        <AGroup t={t}>Период продления</AGroup>
        <APlanList t={t} selected={selected}/>

        <button style={{ ...aPayCta(t), marginTop: 20 }}>Продлить · {plan.total} ₽</button>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Оплата спишется с Google Play.</div>
      </div>
    </ADevice>
  );
}

Object.assign(window, {
  AndroidPaywall, AndroidPaywallActive, AndroidPaywallExpired, AndroidRenewSheet,
  AndroidPlanCard,
});
