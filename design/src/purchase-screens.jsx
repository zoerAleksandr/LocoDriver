// Покупки — витрина подписки «Машинист Pro» (3 направления) + управление
// Тарифы: Месяц / 3 месяца / Год — функции одинаковы, отличается срок и цена.
//   Месяц   — 199 ₽      (199 ₽/мес)
//   3 месяца — 499 ₽      (166 ₽/мес, −17 %)
//   Год     — 1 490 ₽    (124 ₽/мес, −38 %, рекомендуем)
//
// Направления отличаются СТРУКТУРОЙ выбора тарифа:
//   A — список из 3 карточек-тарифов (radio) + список функций.
//   B — сегмент-переключатель сверху, одна крупная динамическая цена.
//   C — годовой тариф крупным героем + два компактных чипа.

// Премиальный «ink» герой (тёмная карточка в светлой теме — на бренде, без градиентов)
function ProHero({ t, compact, flush }) {
  return (
    <div style={{
      background: t.cta, color: t.ctaInk, borderRadius: flush ? 0 : 22,
      padding: compact ? '20px' : '24px 22px',
      boxShadow: flush ? 'none' : M.shadow.md, position: 'relative', overflow: 'hidden',
    }}>
      {/* тонкая «рельса» как фактура */}
      <div style={{
        position: 'absolute', right: -30, top: -30, width: 160, height: 160,
        borderRadius: '50%', border: `1px solid rgba(255,255,255,0.06)`,
      }}/>
      <div style={{
        position: 'absolute', right: -10, top: -10, width: 120, height: 120,
        borderRadius: '50%', border: `1px solid rgba(255,255,255,0.06)`,
      }}/>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, position: 'relative' }}>
        <div style={{
          width: 44, height: 44, borderRadius: 13, background: hexToRgba(t.ctaInk, 0.12),
          display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.ctaInk,
        }}><IcCrown width={24} height={24}/></div>
        <div>
          <div style={{ fontFamily: M.fontDisplay, fontSize: 22, fontWeight: 800, letterSpacing: -0.4, whiteSpace: 'nowrap' }}>Машинист Pro</div>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: hexToRgba(t.ctaInk, 0.55), letterSpacing: 1 }}>ПОЛНАЯ ВЕРСИЯ</div>
        </div>
      </div>
      {!compact && (
        <div style={{ fontFamily: M.fontSans, fontSize: 14, color: hexToRgba(t.ctaInk, 0.7), marginTop: 14, lineHeight: 1.45 }}>
          Облако, экспорт и безлимит истории. Все поездки под рукой и в безопасности.
        </div>
      )}
    </div>
  );
}

// Список функций Pro
const PRO_FEATURES = [
  ['Облачная копия и синхронизация', <IcCloud/>],
  ['Экспорт в PDF и Excel', <IcPdf/>],
  ['Безлимит маршрутов и истории', <IcDocument/>],
  ['Расширенный расчёт зарплаты', <IcRuble/>],
  ['Несколько дорог и депо', <IcMapPin/>],
  ['Без рекламы', <IcCheck/>],
];

// Вариант A — финальный набор функций (без Excel, зарплаты, депо, рекламы)
const PRO_FEATURES_A = [
  ['Облачная копия и синхронизация', <IcCloud/>],
  ['Экспорт в PDF', <IcPdf/>],
  ['Безлимит маршрутов и истории', <IcDocument/>],
];

function FeatureRow({ t, label, icon, last }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '11px 0' }}>
      <div style={{
        width: 30, height: 30, borderRadius: 9, flexShrink: 0,
        background: t.accentSoft, color: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{React.cloneElement(icon, { width: 17, height: 17 })}</div>
      <span style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text }}>{label}</span>
    </div>
  );
}

// Карточка-тариф с radio
function PlanCard({ t, term, total, perMonth, badge, selected, onClick }) {
  return (
    <button onClick={onClick} style={{
      width: '100%', textAlign: 'left', cursor: 'pointer',
      background: t.surface, borderRadius: 16,
      border: `2px solid ${selected ? t.accent : t.border}`,
      boxShadow: selected ? '0 4px 16px rgba(0,160,245,0.12)' : M.shadow.sm,
      padding: '16px 16px', display: 'flex', alignItems: 'center', gap: 14,
      position: 'relative',
    }}>
      {/* radio */}
      <div style={{
        width: 22, height: 22, borderRadius: 11, flexShrink: 0,
        border: `2px solid ${selected ? t.accent : t.borderStrong}`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{selected && <div style={{ width: 12, height: 12, borderRadius: 6, background: t.accent }}/>}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontFamily: M.fontSans, fontSize: 16, fontWeight: 700, color: t.text }}>{term}</span>
          {badge && <Pill t={t} tone="success" mono={false} style={{ fontSize: 11, padding: '2px 8px', whiteSpace: 'nowrap' }}>{badge}</Pill>}
        </div>
        <div style={{ ...M.t.captionMuted(t), marginTop: 3, fontFamily: M.fontMono }}>{perMonth} ₽/мес</div>
      </div>
      <div style={{ textAlign: 'right', flexShrink: 0 }}>
        <div style={{ fontFamily: M.fontMono, fontSize: 19, fontWeight: 700, color: t.text, letterSpacing: -0.5, whiteSpace: 'nowrap' }}>{total} ₽</div>
      </div>
    </button>
  );
}

function payCta(t) {
  return {
    width: '100%', height: 54, borderRadius: 15, border: 'none', cursor: 'pointer',
    background: t.cta, color: t.ctaInk,
    fontFamily: M.fontSans, fontSize: 16, fontWeight: 700,
    boxShadow: '0 10px 28px rgba(0,0,0,0.18)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
  };
}

function PaywallTopBar({ t, restore = true }) {
  return (
    <div style={{ paddingTop: 58 }}>
      <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button style={{
          width: 40, height: 40, borderRadius: 20, border: 'none',
          background: t.surface, boxShadow: M.shadow.sm, color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }}><span style={{ fontSize: 17, lineHeight: 1 }}>✕</span></button>
        {restore && (
          <button style={{
            border: 'none', background: 'transparent', color: t.accent,
            fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, cursor: 'pointer',
          }}>Восстановить</button>
        )}
      </div>
    </div>
  );
}

function FootNote({ t }) {
  return (
    <div style={{ ...M.t.captionMuted(t), textAlign: 'center', lineHeight: 1.4, padding: '14px 12px 0' }}>
      Оплата спишется с Apple ID. Автопродление можно отключить в&nbsp;настройках. <span style={{ color: t.accent }}>Условия</span>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// ПОКУПКИ — Вариант A · «Список тарифов» (карточки-radio)
//   state = 'buy'    — подписка не куплена (витрина тарифов)
//   state = 'active' — подписка активна (показываем срок действия)
// ═══════════════════════════════════════════════════════════════
function IOSPaywallA({ dark = false, height = 1040, selected = 'year', state = 'buy', renewsOn = '14.02.2027', daysLeft = null }) {
  const t = dark ? M.dark : M.light;
  const ctaLabel = { month: 'Оформить за 199 ₽/мес', q: 'Оформить за 499 ₽', year: 'Оформить за 1 490 ₽/год' }[selected];
  const active = state === 'active';
  return (
    <MDevice dark={dark} height={height}>
      <PaywallTopBar t={t} restore={!active}/>
      <div style={{ overflowY: 'auto', height: active ? 'calc(100% - 110px)' : 'calc(100% - 110px - 96px)', padding: '4px 16px 24px' }}>
        <ProHero t={t}/>

        {active && (
          <React.Fragment>
            {/* Статус активной подписки + срок */}
            <div style={{
              display: 'flex', alignItems: 'center', gap: 13, marginTop: 16,
              padding: '14px 16px 14px 16px', borderRadius: 18,
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
                <div style={{ fontFamily: M.fontSans, fontSize: 16, fontWeight: 700, color: t.text }}>Подписка активна</div>
                <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>
                  Действует до <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{renewsOn}</span>
                </div>
              </div>
            </div>

            {daysLeft != null && (
              /* Внутриэкранное напоминание — скоро истечёт */
              <div style={{
                marginTop: 10, padding: '14px 15px', borderRadius: 16,
                background: hexToRgba(t.warning, 0.10), border: `1px solid ${hexToRgba(t.warning, 0.30)}`,
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 38, height: 38, borderRadius: 11, flexShrink: 0, background: t.warning, color: '#1b1300', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v4l3 2"/></svg>
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 700, color: t.text }}>Подписка истекает через {daysLeft} дня</div>
                    <div style={{ ...M.t.captionMuted(t), marginTop: 2, lineHeight: 1.4 }}>Продлите заранее — новый срок прибавится к текущему, дни не сгорят.</div>
                  </div>
                </div>
              </div>
            )}

            {/* Текущий тариф — детали */}
            <GroupHead t={t} mt={22}>Текущий тариф</GroupHead>
            <Card t={t}>
              <FieldRow t={t} label="Тариф" value="Годовая"/>
              <Sep t={t} inset={20}/>
              <FieldRow t={t} label="Стоимость" value="1 490 ₽/год" mono/>
              <Sep t={t} inset={20}/>
              <FieldRow t={t} label="Действует до" value={renewsOn} mono/>
            </Card>
          </React.Fragment>
        )}

        {/* Функции — только в состоянии покупки */}
        {!active && (
          <div style={{ background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm, padding: '6px 18px', marginTop: 16 }}>
            {PRO_FEATURES_A.map(([label, icon], i) => <FeatureRow key={i} t={t} label={label} icon={icon}/>)}
          </div>
        )}

        {!active && (
          <React.Fragment>
            {/* Тарифы */}
            <GroupHead t={t} mt={22}>Выберите тариф</GroupHead>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <PlanCard t={t} term="Год" total="1 490" perMonth="124" badge="−38 %" selected={selected === 'year'}/>
              <PlanCard t={t} term="3 месяца" total="499" perMonth="166" badge="−17 %" selected={selected === 'q'}/>
              <PlanCard t={t} term="Месяц" total="199" perMonth="199" selected={selected === 'month'}/>
            </div>
          </React.Fragment>
        )}
      </div>

      {active ? (
        /* Продление срока активной подписки */
        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 50, padding: '0 16px 26px', background: t.bg }}>
          <div style={{ height: 18, marginTop: -18, background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})` }}/>
          <button style={payCta(t)}>Продлить срок</button>
          <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Новый период прибавится к текущему сроку.</div>
        </div>
      ) : (
        /* Закреплённая CTA — покупка */
        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 50, padding: '0 16px 26px', background: t.bg }}>
          <div style={{ height: 18, marginTop: -18, background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})` }}/>
          <button style={payCta(t)}>{ctaLabel}</button>
          <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Первые 20 маршрутов — бесплатно. Дальше добавление маршрутов — только по подписке.</div>
        </div>
      )}
    </MDevice>
  );
}

// ── Строка сравнения «без Pro / с Pro» ──────────────────────────
function CompareRow({ t, label, free, pro, last }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 64px 64px', alignItems: 'center', padding: '12px 0', borderBottom: last ? 'none' : `1px solid ${t.border}` }}>
      <span style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 500, color: t.text, lineHeight: 1.25, paddingRight: 8 }}>{label}</span>
      <span style={{ display: 'flex', justifyContent: 'center', fontFamily: M.fontMono, fontSize: 13, fontWeight: 600, color: t.textFaint }}>{free}</span>
      <span style={{ display: 'flex', justifyContent: 'center', fontFamily: M.fontMono, fontSize: 13, fontWeight: 700, color: t.accent }}>{pro}</span>
    </div>
  );
}
const IcX = ({ t }) => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={t.textFaint} strokeWidth="2.6" strokeLinecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>;
const IcOk = ({ t }) => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={t.accent} strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12l5 5L20 7"/></svg>;

// ═══════════════════════════════════════════════════════════════
// ПОКУПКИ — Вариант A2 · «Сравнение» (что есть сейчас / что даст Pro)
// ═══════════════════════════════════════════════════════════════
function IOSPaywallCompare({ dark = false, height = 1080, selected = 'year', used = 18, limit = 20 }) {
  const t = dark ? M.dark : M.light;
  const ctaLabel = { month: 'Оформить за 199 ₽/мес', q: 'Оформить за 499 ₽', year: 'Оформить за 1 490 ₽/год' }[selected];
  const left = used / limit;
  return (
    <MDevice dark={dark} height={height}>
      <PaywallTopBar t={t} restore/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px - 96px)', padding: '4px 16px 24px' }}>
        {/* Тёмный герой Pro + список функций — единый блок */}
        <div style={{ marginTop: 4, borderRadius: 22, overflow: 'hidden', boxShadow: M.shadow.md }}>
          <ProHero t={t} flush/>
          <div style={{ background: t.surface, padding: '6px 18px' }}>
            {PRO_FEATURES_A.map(([label, icon], i) => <FeatureRow key={i} t={t} label={label} icon={icon}/>)}
          </div>
        </div>

        {/* Тарифы */}
        <GroupHead t={t} mt={22}>Выберите тариф</GroupHead>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <PlanCard t={t} term="Год" total="1 490" perMonth="124" badge="−38 %" selected={selected === 'year'}/>
          <PlanCard t={t} term="3 месяца" total="499" perMonth="166" badge="−17 %" selected={selected === 'q'}/>
          <PlanCard t={t} term="Месяц" total="199" perMonth="199" selected={selected === 'month'}/>
        </div>
      </div>

      {/* Закреплённая CTA — покупка */}
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 50, padding: '0 16px 26px', background: t.bg }}>
        <div style={{ height: 18, marginTop: -18, background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})` }}/>
        <button style={payCta(t)}>{ctaLabel}</button>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Первые 20 маршрутов — бесплатно. Дальше добавление маршрутов — только по подписке.</div>
      </div>
    </MDevice>
  );
}

// ═══════════════════════════════════════════════════════════════
// ПОКУПКИ — Вариант A3 · «Замок → разблокировка» (два панно)
//   Сверху приглушённое «Сейчас» с лимитами, снизу яркое Pro
// ═══════════════════════════════════════════════════════════════
function IOSPaywallUnlock({ dark = false, height = 1000, selected = 'year', used = 18, limit = 20 }) {
  const t = dark ? M.dark : M.light;
  const ctaLabel = { month: 'Разблокировать за 199 ₽/мес', q: 'Разблокировать за 499 ₽', year: 'Разблокировать за 1 490 ₽/год' }[selected];
  const left = used / limit;
  const proItems = [
    ['Безлимит маршрутов', 'без ограничения в 20 шт.', <IcDocument/>],
    ['Облачная копия и синхронизация', 'данные не потеряются', <IcCloud/>],
    ['Экспорт в PDF', 'делитесь и распечатывайте', <IcPdf/>],
  ];
  return (
    <MDevice dark={dark} height={height}>
      <PaywallTopBar t={t}/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px)', padding: '4px 16px 28px' }}>

        {/* ── СЕЙЧАС: приглушённое панно с лимитом ── */}
        <div style={{ position: 'relative', background: t.bgSubtle, borderRadius: 20, padding: '18px 18px 20px', overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, letterSpacing: 0.6, color: t.textMuted }}>СЕЙЧАС · БЕСПЛАТНЫЙ ПЕРИОД</span>
            <span style={{
              display: 'inline-flex', alignItems: 'center', gap: 5,
              fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, color: t.warning,
            }}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke={t.warning} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>
              ЗАБЛОКИРОВАНО
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginTop: 14 }}>
            <span style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>Маршруты</span>
            <span style={{ fontFamily: M.fontMono, fontSize: 14, fontWeight: 700, color: t.text }}>{used} / {limit}</span>
          </div>
          <div style={{ height: 8, borderRadius: 99, background: dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)', marginTop: 10, overflow: 'hidden' }}>
            <div style={{ width: `${left * 100}%`, height: '100%', borderRadius: 99, background: t.warning }}/>
          </div>
          <div style={{ ...M.t.captionMuted(t), marginTop: 9, lineHeight: 1.4 }}>
            В бесплатном периоде доступно {limit} маршрутов — осталось <span style={{ color: t.text, fontWeight: 600 }}>{limit - used}</span>. Синхронизация и экспорт недоступны.
          </div>
        </div>

        {/* ── Соединительная стрелка ── */}
        <div style={{ display: 'flex', justifyContent: 'center', margin: '-2px 0' }}>
          <div style={{
            width: 36, height: 36, borderRadius: 99, background: t.bg,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: M.shadow.sm, position: 'relative', zIndex: 2,
          }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={t.accent} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"><path d="M12 5v14M6 13l6 6 6-6"/></svg>
          </div>
        </div>

        {/* ── С PRO: яркое панно с разблокировкой ── */}
        <div style={{
          position: 'relative', borderRadius: 20, padding: '20px 18px 22px',
          background: `linear-gradient(160deg, ${hexToRgba(t.accent, dark ? 0.20 : 0.12)}, ${hexToRgba(t.accent, dark ? 0.06 : 0.03)})`,
          border: `1.5px solid ${hexToRgba(t.accent, 0.35)}`,
        }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 7, color: t.accent }}>
            <IcCrown width={16} height={16}/>
            <span style={{ fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, letterSpacing: 0.6 }}>С МАШИНИСТ PRO</span>
          </div>
          <div style={{ fontFamily: M.fontDisplay, fontSize: 20, fontWeight: 800, color: t.text, letterSpacing: -0.4, lineHeight: 1.2, marginTop: 8 }}>
            Всё открывается сразу
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
            {proItems.map(([title, sub, icon], i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                <div style={{
                  width: 36, height: 36, borderRadius: 11, flexShrink: 0,
                  background: t.accent, color: t.accentInk,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  {React.cloneElement(icon, { width: 18, height: 18 })}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text, lineHeight: 1.25 }}>{title}</div>
                  <div style={{ ...M.t.captionMuted(t), marginTop: 1 }}>{sub}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* CTA */}
        <button style={{ ...payCta(t), marginTop: 18 }}>{ctaLabel}</button>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Год · 1 490 ₽ · 124 ₽/мес · отмена в любой момент</div>
      </div>
    </MDevice>
  );
}

// ═══════════════════════════════════════════════════════════════
// ПОКУПКИ — Вариант B · «Сегмент-переключатель» (одна крупная цена)
// ═══════════════════════════════════════════════════════════════
function IOSPaywallB({ dark = false, height = 1000, selected = 'year' }) {
  const t = dark ? M.dark : M.light;
  const plans = {
    month: { seg: 'Месяц', total: '199', per: '199', sub: 'Списываем 199 ₽ каждый месяц', badge: null },
    q:     { seg: '3 месяца', total: '499', per: '166', sub: 'Списываем 499 ₽ раз в 3 месяца', badge: '−17 %' },
    year:  { seg: 'Год', total: '1 490', per: '124', sub: 'Списываем 1 490 ₽ раз в год', badge: '−38 %' },
  };
  const p = plans[selected];
  const Seg = ({ id, label }) => (
    <div style={{
      flex: 1, textAlign: 'center', padding: '10px 0', borderRadius: 11, cursor: 'pointer',
      fontFamily: M.fontSans, fontSize: 14, fontWeight: 600,
      background: selected === id ? t.surface : 'transparent',
      color: selected === id ? t.text : t.textMuted,
      boxShadow: selected === id ? M.shadow.sm : 'none',
    }}>{label}</div>
  );
  return (
    <MDevice dark={dark} height={height}>
      <PaywallTopBar t={t}/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px - 96px)', padding: '4px 16px 24px' }}>
        <ProHero t={t} compact/>

        {/* Сегмент-переключатель тарифа */}
        <div style={{ display: 'flex', gap: 4, padding: 4, background: t.bgSubtle, borderRadius: 14, marginTop: 18 }}>
          <Seg id="month" label="Месяц"/>
          <Seg id="q" label="3 месяца"/>
          <Seg id="year" label="Год"/>
        </div>

        {/* Крупная динамическая цена */}
        <div style={{
          background: t.surface, borderRadius: 20, boxShadow: M.shadow.md,
          padding: '24px 22px', marginTop: 14, textAlign: 'center',
        }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'center', gap: 8 }}>
            <span style={{ fontFamily: M.fontMono, fontSize: 48, fontWeight: 800, color: t.text, letterSpacing: -2, lineHeight: 1 }}>{p.per}</span>
            <span style={{ fontFamily: M.fontMono, fontSize: 18, color: t.textMuted, whiteSpace: 'nowrap' }}>₽/мес</span>
          </div>
          <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
            <span style={{ ...M.t.captionMuted(t), fontSize: 13 }}>{p.sub}</span>
            {p.badge && <Pill t={t} tone="success" mono={false} style={{ fontSize: 11, padding: '2px 8px', whiteSpace: 'nowrap' }}>выгода {p.badge}</Pill>}
          </div>
        </div>

        {/* Функции — компактно */}
        <div style={{ background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm, padding: '6px 18px', marginTop: 14 }}>
          {PRO_FEATURES.slice(0, 5).map(([label, icon], i) => <FeatureRow key={i} t={t} label={label} icon={icon}/>)}
        </div>
      </div>

      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 50, padding: '0 16px 26px', background: t.bg }}>
        <div style={{ height: 18, marginTop: -18, background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})` }}/>
        <button style={payCta(t)}>Попробовать 7 дней бесплатно</button>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10 }}>Потом {p.total} ₽ {selected === 'month' ? '/мес' : selected === 'q' ? 'за 3 мес' : '/год'} · отмена в любой момент</div>
      </div>
    </MDevice>
  );
}

// ═══════════════════════════════════════════════════════════════
// ПОКУПКИ — Вариант C · «Годовой герой» (рекомендуем + чипы)
// ═══════════════════════════════════════════════════════════════
function IOSPaywallC({ dark = false, height = 1060 }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <PaywallTopBar t={t}/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px - 96px)', padding: '4px 16px 24px' }}>
        {/* Заголовок */}
        <div style={{ textAlign: 'center', padding: '4px 8px 0' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 7, color: t.accent, marginBottom: 10 }}>
            <IcCrown width={22} height={22}/>
            <span style={{ fontFamily: M.fontMono, fontSize: 12, fontWeight: 700, letterSpacing: 1.2 }}>МАШИНИСТ PRO</span>
          </div>
          <div style={{ fontFamily: M.fontDisplay, fontSize: 26, fontWeight: 800, color: t.text, letterSpacing: -0.6, lineHeight: 1.15 }}>
            Все поездки в облаке<br/>и без ограничений
          </div>
        </div>

        {/* Годовой герой — рекомендуемый тариф */}
        <div style={{
          background: t.cta, color: t.ctaInk, borderRadius: 22, marginTop: 22,
          padding: '20px 20px', boxShadow: M.shadow.md, position: 'relative',
        }}>
          <div style={{
            position: 'absolute', top: 16, right: 16,
            fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, letterSpacing: 0.6,
            padding: '4px 10px', borderRadius: 999, background: t.accent, color: t.accentInk, whiteSpace: 'nowrap',
          }}>ВЫГОДНЕЕ ВСЕГО</div>
          <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: hexToRgba(t.ctaInk, 0.7), paddingRight: 150 }}>Годовая подписка</div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginTop: 8 }}>
            <span style={{ fontFamily: M.fontMono, fontSize: 44, fontWeight: 800, letterSpacing: -1.5, lineHeight: 1 }}>124</span>
            <span style={{ fontFamily: M.fontMono, fontSize: 16, color: hexToRgba(t.ctaInk, 0.6), whiteSpace: 'nowrap' }}>₽/мес</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 12 }}>
            <span style={{ fontFamily: M.fontMono, fontSize: 13, color: hexToRgba(t.ctaInk, 0.6), whiteSpace: 'nowrap' }}>1 490 ₽ в год</span>
            <span style={{
              fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, padding: '2px 8px',
              borderRadius: 999, background: hexToRgba(t.ctaInk, 0.14), color: t.ctaInk, whiteSpace: 'nowrap',
            }}>−38 %</span>
          </div>
        </div>

        {/* Чипы — короткие тарифы */}
        <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
          {[['3 месяца', '499 ₽', '166 ₽/мес'], ['Месяц', '199 ₽', '199 ₽/мес']].map(([term, total, per], i) => (
            <button key={i} style={{
              flex: 1, cursor: 'pointer', textAlign: 'left',
              background: t.surface, borderRadius: 14, border: `1px solid ${t.border}`, boxShadow: M.shadow.sm,
              padding: '13px 14px',
            }}>
              <div style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 700, color: t.text }}>{term}</div>
              <div style={{ fontFamily: M.fontMono, fontSize: 16, fontWeight: 700, color: t.text, marginTop: 4 }}>{total}</div>
              <div style={{ ...M.t.captionMuted(t), fontFamily: M.fontMono, marginTop: 1 }}>{per}</div>
            </button>
          ))}
        </div>

        {/* Функции — 2 колонки компактно */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4px 16px', marginTop: 22 }}>
          {PRO_FEATURES.map(([label, icon], i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '8px 0' }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={t.success} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}><path d="M5 12l5 5L20 7"/></svg>
              <span style={{ fontFamily: M.fontSans, fontSize: 13.5, fontWeight: 500, color: t.text, lineHeight: 1.2 }}>{label}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 50, padding: '0 16px 26px', background: t.bg }}>
        <div style={{ height: 18, marginTop: -18, background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})` }}/>
        <button style={payCta(t)}>Оформить Pro · 7 дней бесплатно</button>
        <FootNote t={t}/>
      </div>
    </MDevice>
  );
}

// ═══════════════════════════════════════════════════════════════
// УПРАВЛЕНИЕ — активная подписка (открывается из профиля)
// ═══════════════════════════════════════════════════════════════
function IOSProManage({ dark = false, height = 920 }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <NavBarIOS t={t} title="Подписка" back trailing={<div style={{ width: 40 }}/>}/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px)', padding: '8px 16px 32px' }}>
        <ProHero t={t} compact/>

        <GroupHead t={t} mt={22}>Текущий тариф</GroupHead>
        <Card t={t}>
          <FieldRow t={t} label="Тариф" value="Годовая"/>
          <Sep t={t} inset={20}/>
          <FieldRow t={t} label="Стоимость" value="1 490 ₽/год" mono/>
          <Sep t={t} inset={20}/>
          <FieldRow t={t} label="Действует до" value="14.02.2027" mono/>
        </Card>

        <GroupHead t={t}>Входит в подписку</GroupHead>
        <div style={{ background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm, padding: '6px 18px' }}>
          {PRO_FEATURES_A.map(([label, icon], i) => <FeatureRow key={i} t={t} label={label} icon={icon}/>)}
        </div>

        <button style={{
          width: '100%', marginTop: 22, padding: '15px', borderRadius: 14, cursor: 'pointer',
          background: t.cta, color: t.ctaInk, border: 'none', boxShadow: '0 10px 28px rgba(0,0,0,0.18)',
          fontFamily: M.fontSans, fontSize: 16, fontWeight: 700,
        }}>Продлить срок</button>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Новый период прибавится к текущему сроку оплаты.</div>
      </div>
    </MDevice>
  );
}

// ── маленькая «дата-плашка» для визуальной математики срока ──────
function DateChunk({ t, cap, date, accent }) {
  return (
    <div style={{ flex: 1, textAlign: 'center', padding: '2px 4px' }}>
      <div style={{ fontFamily: M.fontMono, fontSize: 10.5, fontWeight: 700, letterSpacing: 0.5, color: t.textMuted, textTransform: 'uppercase' }}>{cap}</div>
      <div style={{ fontFamily: M.fontMono, fontSize: 16, fontWeight: 700, marginTop: 5, color: accent ? t.accent : t.text }}>{date}</div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// ПРОДЛЕНИЕ — модальный лист (по нажатию «Продлить срок»)
//   Главная мысль: новый период ПРИБАВЛЯЕТСЯ к текущему сроку.
// ═══════════════════════════════════════════════════════════════
function IOSRenewSheet({ dark = false, height = 1040, selected = 'year', currentEnd = '14.02.2027' }) {
  const t = dark ? M.dark : M.light;
  const plan = {
    year:  { add: '+ 1 год',     newEnd: '14.02.2028', total: '1 490' },
    q:     { add: '+ 3 месяца',  newEnd: '14.05.2027', total: '499' },
    month: { add: '+ 1 месяц',   newEnd: '14.03.2027', total: '199' },
  }[selected];
  return (
    <MDevice dark={dark} height={height}>
      {/* затемнённый контекст под листом */}
      <div style={{ position: 'absolute', inset: 0, background: dark ? '#05070b' : '#11141A' }}/>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.45)' }}/>

      {/* лист снизу */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 10,
        background: t.bg, borderTopLeftRadius: 28, borderTopRightRadius: 28,
        padding: '10px 16px 30px', boxShadow: '0 -20px 60px rgba(0,0,0,0.35)',
      }}>
        <div style={{ width: 40, height: 5, borderRadius: 99, background: t.border, margin: '0 auto 16px' }}/>
        <div style={{ fontFamily: M.fontDisplay, fontSize: 23, fontWeight: 800, color: t.text, letterSpacing: -0.4, textAlign: 'center' }}>Продление подписки</div>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 7, lineHeight: 1.45, padding: '0 14px' }}>
          Новый период не заменяет текущий, а&nbsp;прибавляется к&nbsp;нему — дни не сгорают.
        </div>

        {/* визуальная математика срока */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6, marginTop: 18,
          background: t.surface, borderRadius: 18, boxShadow: M.shadow.sm, padding: '16px 10px',
        }}>
          <DateChunk t={t} cap="Сейчас до" date={currentEnd}/>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, flexShrink: 0 }}>
            <span style={{ fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, color: t.accent, whiteSpace: 'nowrap' }}>{plan.add}</span>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke={t.accent} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h15M13 6l6 6-6 6"/></svg>
          </div>
          <DateChunk t={t} cap="Станет до" date={plan.newEnd} accent/>
        </div>

        {/* выбор периода продления */}
        <GroupHead t={t} mt={20}>Период продления</GroupHead>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <PlanCard t={t} term="Год" total="1 490" perMonth="124" badge="−38 %" selected={selected === 'year'}/>
          <PlanCard t={t} term="3 месяца" total="499" perMonth="166" badge="−17 %" selected={selected === 'q'}/>
          <PlanCard t={t} term="Месяц" total="199" perMonth="199" selected={selected === 'month'}/>
        </div>

        <button style={{ ...payCta(t), marginTop: 20 }}>Продлить · {plan.total} ₽</button>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Оплата спишется с Apple ID.</div>
      </div>
    </MDevice>
  );
}

// ═══════════════════════════════════════════════════════════════
// ИСТЕКЛА — подписка была куплена и закончилась (возобновление)
// ═══════════════════════════════════════════════════════════════
function IOSPaywallExpired({ dark = false, height = 1040, selected = 'year', expiredOn = '12.05.2026' }) {
  const t = dark ? M.dark : M.light;
  const red = dark ? '#FF4D4D' : '#C81E14'; // насыщенный чистый красный (не розовый)
  return (
    <MDevice dark={dark} height={height}>
      <PaywallTopBar t={t}/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px - 96px)', padding: '4px 16px 24px' }}>
        <ProHero t={t}/>

        {/* баннер: подписка истекла */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 13, marginTop: 16,
          padding: '14px 16px', borderRadius: 18,
          background: hexToRgba(red, 0.12), border: `1px solid ${hexToRgba(red, 0.34)}`,
        }}>
          <div style={{ width: 40, height: 40, borderRadius: 12, flexShrink: 0, background: red, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5M12 16h.01"/></svg>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: M.fontSans, fontSize: 16, fontWeight: 700, color: t.text }}>Подписка истекла</div>
            <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>
              Закончилась <span style={{ fontFamily: M.fontMono, color: t.text, fontWeight: 600 }}>{expiredOn}</span>
            </div>
          </div>
        </div>

        {/* что отключилось */}
        <div style={{ background: hexToRgba(red, 0.05), borderRadius: 14, padding: '12px 16px', marginTop: 10 }}>
          <div style={{ ...M.t.captionMuted(t), lineHeight: 1.5 }}>
            Маршруты и история сохранены. Но добавлять новые сверх 20 и пользоваться синхронизацией нельзя, пока подписка не возобновлена.
          </div>
        </div>

        <GroupHead t={t} mt={22}>Выберите тариф</GroupHead>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <PlanCard t={t} term="Год" total="1 490" perMonth="124" badge="−38 %" selected={selected === 'year'}/>
          <PlanCard t={t} term="3 месяца" total="499" perMonth="166" badge="−17 %" selected={selected === 'q'}/>
          <PlanCard t={t} term="Месяц" total="199" perMonth="199" selected={selected === 'month'}/>
        </div>
      </div>

      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 50, padding: '0 16px 26px', background: t.bg }}>
        <div style={{ height: 18, marginTop: -18, background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)}, ${t.bg})` }}/>
        <button style={payCta(t)}>Возобновить подписку</button>
        <div style={{ ...M.t.captionMuted(t), textAlign: 'center', marginTop: 10, lineHeight: 1.4 }}>Возобновление начнётся с сегодняшнего дня.</div>
      </div>
    </MDevice>
  );
}

// ═══════════════════════════════════════════════════════════════
// СКОРО ИСТЕЧЁТ — push-уведомление на экране блокировки (за N дней)
// ═══════════════════════════════════════════════════════════════
function IOSExpiringSoon({ dark = false, height = 1040, daysLeft = 3, renewsOn = '14.02.2027' }) {
  const t = M.dark;
  return (
    <MDevice dark height={height}>
      {/* обои */}
      <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(165deg, #243044 0%, #141a26 55%, #080b11 100%)' }}/>
      <div style={{ position: 'absolute', inset: 0, background: 'radial-gradient(120% 55% at 80% 0%, rgba(120,150,200,0.20), transparent 60%)' }}/>

      {/* часы / дата экрана блокировки */}
      <div style={{ position: 'absolute', top: 92, left: 0, right: 0, textAlign: 'center', color: '#fff' }}>
        <div style={{ fontFamily: M.fontMono, fontSize: 14, fontWeight: 600, letterSpacing: 0.5, opacity: 0.82 }}>пятница, 11 февраля</div>
        <div style={{ fontFamily: M.fontDisplay, fontSize: 84, fontWeight: 700, letterSpacing: -2, lineHeight: 1, marginTop: 2 }}>9:41</div>
      </div>

      {/* push-уведомление */}
      <div style={{ position: 'absolute', left: 12, right: 12, top: 268 }}>
        <div style={{ fontFamily: M.fontMono, fontSize: 11, fontWeight: 700, letterSpacing: 0.7, color: 'rgba(255,255,255,0.5)', padding: '0 8px 9px' }}>УВЕДОМЛЕНИЕ</div>
        <div style={{
          background: 'rgba(28,30,36,0.72)', backdropFilter: 'blur(24px)', WebkitBackdropFilter: 'blur(24px)',
          borderRadius: 22, padding: '14px 15px', boxShadow: '0 18px 44px rgba(0,0,0,0.4)',
          border: '1px solid rgba(255,255,255,0.08)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: t.warning, color: '#1b1300', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v4l3 2"/></svg>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <span style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 700, color: '#fff' }}>Машинист</span>
                <span style={{ fontFamily: M.fontMono, fontSize: 11, color: 'rgba(255,255,255,0.5)' }}>сейчас</span>
              </div>
              <div style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: '#fff', marginTop: 3, lineHeight: 1.3 }}>Подписка истекает через {daysLeft} дня</div>
            </div>
          </div>
          <div style={{ fontFamily: M.fontSans, fontSize: 13.5, color: 'rgba(255,255,255,0.82)', marginTop: 9, lineHeight: 1.42 }}>
            Срок действия закончится {renewsOn}. Продлите заранее — новый период прибавится к&nbsp;текущему, дни не&nbsp;сгорают.
          </div>
        </div>
        <div style={{ textAlign: 'center', marginTop: 14, fontFamily: M.fontSans, fontSize: 13, fontWeight: 500, color: 'rgba(255,255,255,0.6)' }}>
          Нажмите, чтобы продлить
        </div>
      </div>
    </MDevice>
  );
}

Object.assign(window, {
  IOSPaywallA, IOSPaywallCompare, IOSPaywallUnlock, IOSPaywallB, IOSPaywallC, IOSProManage,
  IOSRenewSheet, IOSPaywallExpired, IOSExpiringSoon,
  ProHero, FeatureRow, PlanCard,
});
