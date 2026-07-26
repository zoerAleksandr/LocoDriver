// salary-screen.jsx — Экран «Зарплата» (расчёт ЗП за месяц), iOS
// Использует общую стилистику приложения: MDevice, токены M, иконки.

// Сетка столбцов (фиксированные ширины — данные ровно друг под другом).
const ACCRUAL_GRID = '106px 52px 46px 84px';

// Строка таблицы начислений: вид выплаты · часы · % · сумма.
function PayRow({ t, name, hours, pct, amount, last }) {
  return (
    <React.Fragment>
      <div style={{
        padding: '13px 20px', display: 'grid', gridTemplateColumns: ACCRUAL_GRID,
        alignItems: 'center', gap: 10,
      }}>
        <div style={{ minWidth: 0, ...M.t.body(t), fontSize: 15, fontWeight: 500 }}>{name}</div>
        <span style={{
          fontFamily: M.fontMono, fontSize: 13, fontWeight: 600,
          color: hours ? t.textMuted : t.textFaint, textAlign: 'right',
        }}>{hours || '—'}</span>
        <span style={{
          fontFamily: M.fontMono, fontSize: 13, fontWeight: 600,
          color: pct ? t.textMuted : t.textFaint, textAlign: 'right',
        }}>{pct || '—'}</span>
        <span style={{
          fontFamily: M.fontMono, fontSize: 15, fontWeight: 600, color: t.text, textAlign: 'right',
        }}>{amount}</span>
      </div>
      {!last && <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>}
    </React.Fragment>
  );
}

// Строка таблицы удержаний: вид удержания · сумма.
function DeductRow({ t, name, amount, last }) {
  return (
    <React.Fragment>
      <div style={{
        padding: '13px 20px', display: 'grid', gridTemplateColumns: DEDUCT_GRID,
        alignItems: 'center', gap: 10,
      }}>
        <div style={{ minWidth: 0, ...M.t.body(t), fontSize: 15, fontWeight: 500 }}>{name}</div>
        <span style={{
          fontFamily: M.fontMono, fontSize: 15, fontWeight: 600, color: t.text, textAlign: 'right',
        }}>{amount}</span>
      </div>
      {!last && <div style={{ height: 1, background: t.border, marginLeft: 20 }}/>}
    </React.Fragment>
  );
}

// Итоговая строка внутри карточки — accent-tint фон.
function PayTotalRow({ t, name, amount }) {
  return (
    <div style={{
      padding: '14px 20px', display: 'flex', alignItems: 'center', gap: 12,
      background: t.accentSoft,
    }}>
      <div style={{ flex: 1, fontSize: 15, fontWeight: 700, color: t.accent }}>{name}</div>
      <span style={{
        fontFamily: M.fontMono, fontSize: 16, fontWeight: 700, color: t.accent, whiteSpace: 'nowrap',
      }}>{amount}</span>
    </div>
  );
}

// Заголовок столбцов таблицы (grid — выровнен со строками, фикс. ширины).
function PayHead({ t, grid, cols }) {
  return (
    <div style={{
      padding: '11px 20px 9px', display: 'grid', gridTemplateColumns: grid,
      alignItems: 'center', gap: 10, borderBottom: `1px solid ${t.border}`,
    }}>
      {cols.map((label, i) => (
        <div key={i} style={{ ...M.t.group(t), textAlign: i === 0 ? 'left' : 'right' }}>{label}</div>
      ))}
    </div>
  );
}

function IOSScreenSalary({ dark = false, height = 940 }) {
  const t = dark ? M.dark : M.light;

  const accruals = [
    { name: 'Оплата по тарифу', hours: '21:31', amount: '8 068,75' },
    { name: 'Ночные часы', hours: '13:51', pct: '40,0', amount: '2 077,50' },
    { name: 'Зональная надбавка', pct: '25,0', amount: '2 017,19' },
    { name: 'Переотдых', hours: '00:24', amount: '100,00' },
  ];
  const deductions = [
    { name: 'НДФЛ (13 %)', amount: '1 594,25' },
    { name: 'Профсоюз', amount: '122,63' },
  ];

  return (
    <MDevice dark={dark} height={height}>
      {/* Шапка — крупная сумма «К выдаче» + действия */}
      <div style={{ paddingTop: 58 }}>
        <div style={{
          padding: '8px 16px 4px', display: 'flex', alignItems: 'flex-start',
          justifyContent: 'space-between',
        }}>
          <div>
            <div style={{
              fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4,
              marginBottom: 6,
            }}>К ВЫДАЧЕ · АПРЕЛЬ</div>
            <div style={{
              fontSize: 38, fontWeight: 800, fontFamily: M.fontMono, color: t.text,
              letterSpacing: -1.5, lineHeight: 1,
              whiteSpace: 'nowrap',
            }}>10 546,56 <span style={{ fontSize: 24, fontWeight: 700, color: t.textMuted }}>₽</span></div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
            <button style={pillBtn(t, { w: 40, h: 40 })}><IcPdf width="18" height="18"/></button>
            <button style={pillBtn(t, { w: 40, h: 40 })}><IcGear width="18" height="18"/></button>
          </div>
        </div>
      </div>

      <div style={{ padding: '8px 16px 150px', overflowY: 'auto', height: 'calc(100% - 158px)' }}>
        {/* Начисления */}
        <SecH t={t}>Начисления</SecH>
        <Card t={t}>
          <PayHead t={t} grid={ACCRUAL_GRID} cols={['Вид выплаты', 'Часы', '%', 'Сумма']}/>
          {accruals.map((r) => (
            <PayRow key={r.name} t={t} {...r}/>
          ))}
          <PayTotalRow t={t} name="Всего начислено" amount="12 263,44"/>
        </Card>

        {/* Удержания */}
        <SecH t={t}>Удержания</SecH>
        <Card t={t}>
          <PayHead t={t} grid={ACCRUAL_GRID} cols={['Вид удержания', '', '', 'Сумма']}/>
          {deductions.map((r) => (
            <PayRow key={r.name} t={t} name={r.name} amount={r.amount}/>
          ))}
          <PayTotalRow t={t} name="Всего удержано" amount="1 716,88"/>
        </Card>

        {/* Итог «К выдаче» */}
        <div style={{
          marginTop: 22, padding: '20px 20px', borderRadius: 20,
          background: t.cta, color: t.ctaInk,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          boxShadow: M.shadow.md,
        }}>
          <div>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, letterSpacing: 1.4, opacity: 0.6 }}>К ВЫДАЧЕ</div>
          </div>
          <div style={{
            fontFamily: M.fontMono, fontSize: 27, fontWeight: 800, letterSpacing: -1, whiteSpace: 'nowrap',
          }}>10 546,56</div>
        </div>

        {/* Пояснение */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '16px 6px 0', color: t.textMuted }}>
          <IcInfo width="16" height="16"/>
          <span style={{ fontSize: 13, lineHeight: 1.4 }}>Расчёт по вашим тарифам и нормам за 23 смены.</span>
        </div>
      </div>

      {/* Gradient scrim под таб-баром */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0, height: 140, zIndex: 40,
        pointerEvents: 'none',
        background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)} 0%, ${hexToRgba(t.bg, 0.85)} 45%, ${t.bg} 100%)`,
      }}/>

      {/* Tab bar — активна «Зарплата» */}
      <div style={{
        position: 'absolute', bottom: 22, left: 10, right: 10, zIndex: 50,
        background: t.surface, borderRadius: 30,
        boxShadow: '0 12px 32px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.06)',
        border: `1px solid ${t.border}`,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 14px',
      }}>
        <TabBtn t={t} icon={<IcDocument/>} label="Главный"/>
        <TabBtn t={t} active icon={<IcRuble/>} label="Зарплата"/>
        <button style={{
          width: 46, height: 46, borderRadius: 23, background: t.cta || t.accent, color: t.ctaInk || t.accentInk,
          border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 18px rgba(0,0,0,0.25)',
        }}><IcPlus width="22" height="22"/></button>
        <TabBtn t={t} icon={<IcSliders/>} label="Настройки"/>
        <TabBtn t={t} icon={<IcProfile/>} label="Профиль"/>
      </div>
    </MDevice>
  );
}

// ─────────────────────────────────────────────────────────────
// Android (Material 3) — экран «Зарплата»
// Системные особенности: small top app bar c заголовком слева и icon-кнопками
// 48dp; tonal hero-контейнер «К выдаче»; группы как mono-caption + MCard 16r;
// итог — filled primary-container; Material navigation bar снизу (Зарплата —
// активная вкладка с pill-индикатором). Таблицы — те же фикс. колонки, что и iOS.
// ─────────────────────────────────────────────────────────────
function AndroidScreenSalary({ dark = false, height = 940 }) {
  const t = dark ? M.dark : M.light;

  const accruals = [
    { name: 'Оплата по тарифу', hours: '21:31', amount: '8 068,75' },
    { name: 'Ночные часы', hours: '13:51', pct: '40,0', amount: '2 077,50' },
    { name: 'Зональная надбавка', pct: '25,0', amount: '2 017,19' },
    { name: 'Переотдых', hours: '00:24', amount: '100,00' },
  ];
  const deductions = [
    { name: 'НДФЛ (13 %)', amount: '1 594,25' },
    { name: 'Профсоюз', amount: '122,63' },
  ];

  return (
    <ADevice dark={dark} height={height}>
      {/* Small top app bar (Material) */}
      <div style={{
        height: 56, display: 'flex', alignItems: 'center', padding: '0 4px 0 12px', gap: 4,
        background: t.bg,
      }}>
        <div style={{ flex: 1, fontSize: 20, fontWeight: 500, color: t.text }}>Зарплата</div>
        <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} aria-label="PDF">
          <IcPdf width="22" height="22"/>
        </button>
        <button style={{ width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} aria-label="Настройки зарплаты">
          <IcGear width="22" height="22"/>
        </button>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 56px)', padding: '0 16px 96px' }}>
        {/* Headline — крупная сумма «К выдаче» (как на iOS: текст, не карточка) */}
        <div style={{ padding: '6px 4px 16px' }}>
          <div>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4, marginBottom: 6 }}>К ВЫДАЧЕ · АПРЕЛЬ</div>
            <div style={{
              fontSize: 38, fontWeight: 800, fontFamily: M.fontMono, color: t.text,
              letterSpacing: -1.5, lineHeight: 1, whiteSpace: 'nowrap',
            }}>10 546,56 <span style={{ fontSize: 24, fontWeight: 700, color: t.textMuted }}>₽</span></div>
          </div>
        </div>

        {/* Начисления */}
        <AGroup t={t}>Начисления</AGroup>
        <MCard t={t}>
          <PayHead t={t} grid={ACCRUAL_GRID} cols={['Вид выплаты', 'Часы', '%', 'Сумма']}/>
          {accruals.map((r) => (
            <PayRow key={r.name} t={t} {...r}/>
          ))}
          <PayTotalRow t={t} name="Всего начислено" amount="12 263,44"/>
        </MCard>

        {/* Удержания */}
        <AGroup t={t}>Удержания</AGroup>
        <MCard t={t}>
          <PayHead t={t} grid={ACCRUAL_GRID} cols={['Вид удержания', '', '', 'Сумма']}/>
          {deductions.map((r) => (
            <PayRow key={r.name} t={t} name={r.name} amount={r.amount}/>
          ))}
          <PayTotalRow t={t} name="Всего удержано" amount="1 716,88"/>
        </MCard>

        {/* Итог «К выдаче» — те же цвета, что и на iOS (t.cta) */}
        <div style={{
          marginTop: 22, padding: '20px 20px', borderRadius: 20,
          background: t.cta, color: t.ctaInk,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12,
          boxShadow: M.shadow.md,
        }}>
          <div style={{ fontSize: 11, fontFamily: M.fontMono, letterSpacing: 1.4, opacity: 0.6 }}>К ВЫДАЧЕ</div>
          <div style={{
            fontFamily: M.fontMono, fontSize: 27, fontWeight: 800, letterSpacing: -1, whiteSpace: 'nowrap',
          }}>10 546,56</div>
        </div>

        {/* Пояснение */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '16px 6px 0', color: t.textMuted }}>
          <IcInfo width="16" height="16"/>
          <span style={{ fontSize: 13, lineHeight: 1.4 }}>Расчёт по вашим тарифам и нормам за 23 смены.</span>
        </div>
      </div>

      {/* Material navigation bar — активна «Зарплата» */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0,
        background: t.surface, height: 72,
        display: 'flex', alignItems: 'center',
        borderTop: `1px solid ${t.border}`,
      }}>
        {[
          [IcDocument, 'Главный', false],
          [IcRuble, 'Зарплата', true],
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

Object.assign(window, { IOSScreenSalary, AndroidScreenSalary, PayRow, PayTotalRow, PayHead });
