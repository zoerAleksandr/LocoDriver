// ════════════════════════════════════════════════════════════════
// ПОДЭКРАН НАСТРОЙКИ · ЗАРПЛАТА (тарифы и параметры расчёта)
//   Открывается из Настройки → Расчёт → «Зарплата».
//   Это форма ВВОДА параметров (ставки, надбавки, удержания),
//   из которых потом считается экран «Зарплата» (salary-screen).
//
//   Исходные Samsung-скрины переведены в общий стиль приложения:
//   • секции «Начисления» / «Удержания» — крупный SecH (как в расчётном
//     экране ЗП), а не системный right-aligned заголовок;
//   • поля — label сверху + единый инпут (mono-значение + единица справа);
//   • iOS: инпут с тонким контуром; Android: filled bgSubtle (Material);
//   • пороговые доплаты (тяж./длинносост./удлин. плечо) — два поля
//     [порог + единица] [%] и ссылка «Добавить» для второго порога;
//   • дата начала действия тарифа — accent-ссылка, открывает date-sheet.
// ════════════════════════════════════════════════════════════════

// Маленькая accent-ссылка (дата тарифа / «Добавить»)
function salLink(t) {
  return {
    background: 'transparent', border: 'none', color: t.accent,
    fontFamily: M.fontSans, fontSize: 13, fontWeight: 600,
    cursor: 'pointer', padding: 0, whiteSpace: 'nowrap',
  };
}

// Единый инпут поля ЗП: mono-значение слева, единица справа.
// mat=true → filled (Material), иначе bordered (iOS).
function PaySetInput({ t, value, unit, mat }) {
  const has = value !== '' && value != null;
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      minHeight: 46, padding: '0 14px', borderRadius: M.r.input, boxSizing: 'border-box',
      background: mat ? t.bgSubtle : 'transparent',
      border: mat ? '1px solid transparent' : `1px solid ${t.border}`,
    }}>
      <span style={{
        fontFamily: M.fontMono, fontSize: 17, fontWeight: 600,
        color: has ? t.text : t.textFaint,
      }}>{has ? value : '0'}</span>
      {unit && (
        <span style={{
          fontFamily: M.fontSans, fontSize: 14, fontWeight: 500, color: t.textMuted, flexShrink: 0,
        }}>{unit}</span>
      )}
    </div>
  );
}

// Поле: label (+ опц. action справа) сверху, инпут снизу.
function PaySetField({ t, label, value, unit, action, mat }) {
  return (
    <div style={{ padding: '12px 16px 14px' }}>
      <div style={{
        display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
        gap: 10, marginBottom: 8,
      }}>
        <div style={{ ...M.t.label(t), fontSize: 13, color: t.textMuted }}>{label}</div>
        {action}
      </div>
      <PaySetInput t={t} value={value} unit={unit} mat={mat}/>
    </div>
  );
}

// Пороговая доплата: [порог + ед.] [%] + ссылка «Добавить».
function PaySetTier({ t, label, threshold, thUnit, percent, mat }) {
  return (
    <div style={{ padding: '12px 16px 14px' }}>
      <div style={{
        display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
        gap: 10, marginBottom: 8,
      }}>
        <div style={{ ...M.t.label(t), fontSize: 13, color: t.textMuted }}>{label}</div>
        <button style={salLink(t)}>Добавить</button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <PaySetInput t={t} value={threshold} unit={thUnit} mat={mat}/>
        <PaySetInput t={t} value={percent} unit="%" mat={mat}/>
      </div>
    </div>
  );
}

// ── Контент формы (общий для iOS и Android) ─────────────────────
const SAL_NADBAVKI = [
  ['Зональная надбавка', '25'],
  ['Доплаты за класс и права', '0'],
  ['Работа в одно лицо · грузовой', '40'],
  ['Работа в одно лицо · пассажирский', '50'],
  ['Доплата за вредность', '0'],
  ['Северная надбавка', '0'],
  ['Районный коэффициент', '0'],
];
const SAL_TIERS = [
  ['Доплата за тяжёлые поезда', '5000', 'т', '9'],
  ['Доплата за длинносоставные поезда', '57', 'ваг', '5'],
  ['Доплата за удлинённое плечо', '250', 'км', '10'],
];
const SAL_UDER = [
  ['Подоходный налог · НДФЛ', '13'],
  ['Профсоюз', '1'],
  ['Прочие удержания', '0'],
];

function SalSettingsBody({ t, mat }) {
  const CardC = mat ? MCard : Card;
  const inset = mat ? 0 : 16;
  const gap = (h) => <div style={{ height: h }}/>;
  const dateLink = <button style={salLink(t)}>с 1 июня 2026</button>;

  return (
    <React.Fragment>
      {/* ── НАЧИСЛЕНИЯ ── */}
      <SecH t={t}>Начисления</SecH>

      <CardC t={t}>
        <PaySetField t={t} mat={mat} label="Тарифная ставка" value="375" unit="₽" action={dateLink}/>
        <Sep t={t} inset={inset}/>
        <PaySetField t={t} mat={mat} label="Средний час" value="0" unit="₽"/>
      </CardC>

      {gap(12)}
      <CardC t={t}>
        {SAL_NADBAVKI.map(([label, val], i) => (
          <React.Fragment key={label}>
            {i > 0 && <Sep t={t} inset={inset}/>}
            <PaySetField t={t} mat={mat} label={label} value={val} unit="%"/>
          </React.Fragment>
        ))}
      </CardC>

      {gap(12)}
      <CardC t={t}>
        {SAL_TIERS.map(([label, th, unit, pct], i) => (
          <React.Fragment key={label}>
            {i > 0 && <Sep t={t} inset={inset}/>}
            <PaySetTier t={t} mat={mat} label={label} threshold={th} thUnit={unit} percent={pct}/>
          </React.Fragment>
        ))}
      </CardC>
      <SectionNote t={t}>
        Надбавка начисляется, когда показатель поезда превышает порог. «Добавить» — задать ещё один порог со своим процентом.
      </SectionNote>

      {gap(12)}
      <CardC t={t}>
        <PaySetField t={t} mat={mat} label="Другие надбавки" value="0" unit="%"/>
      </CardC>

      {/* ── УДЕРЖАНИЯ ── */}
      <SecH t={t}>Удержания</SecH>
      <CardC t={t}>
        {SAL_UDER.map(([label, val], i) => (
          <React.Fragment key={label}>
            {i > 0 && <Sep t={t} inset={inset}/>}
            <PaySetField t={t} mat={mat} label={label} value={val} unit="%"/>
          </React.Fragment>
        ))}
      </CardC>
    </React.Fragment>
  );
}

// ── Date-sheet: дата начала действия нового тарифа ──────────────
function SalTariffDateSheet({ t, mat }) {
  const wd = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
  const weeks = [
    [1, 2, 3, 4, 5, 6, 7],
    [8, 9, 10, 11, 12, 13, 14],
    [15, 16, 17, 18, 19, 20, 21],
    [22, 23, 24, 25, 26, 27, 28],
    [29, 30, null, null, null, null, null],
  ];
  const sel = 1; // 1 июня — текущая дата тарифа

  return (
    <React.Fragment>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 60 }}/>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 61,
        background: t.surface || t.bg,
        borderTopLeftRadius: mat ? M.r.sheetAndroid : M.r.sheetIOS,
        borderTopRightRadius: mat ? M.r.sheetAndroid : M.r.sheetIOS,
        boxShadow: M.shadow.sheet,
        padding: mat ? '10px 20px 26px' : '14px 20px 30px',
      }}>
        {mat && (
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}>
            <div style={{ width: 34, height: 4, borderRadius: 2, background: t.borderStrong }}/>
          </div>
        )}

        {/* Заголовок + переключатель гранулярности */}
        <div style={{
          display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12,
        }}>
          <div style={{
            fontFamily: M.fontDisplay, fontSize: 19, fontWeight: 700, color: t.text,
            letterSpacing: -0.3, lineHeight: 1.2, maxWidth: 260,
          }}>Дата начала действия нового тарифа</div>
          <button style={{ ...salLink(t), fontSize: 14, marginTop: 2 }}>Месяц</button>
        </div>

        {/* Навигация по месяцам */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginTop: 18, marginBottom: 6,
        }}>
          <button style={{ ...salLink(t), color: t.textMuted, display: 'flex', alignItems: 'center', gap: 4 }}>
            <IcChevronLeft width="16" height="16"/> Май
          </button>
          <div style={{ fontFamily: M.fontSans, fontSize: 16, fontWeight: 700, color: t.text }}>июнь</div>
          <button style={{ ...salLink(t), color: t.textMuted, display: 'flex', alignItems: 'center', gap: 4 }}>
            Июль <IcChevronRight width="16" height="16"/>
          </button>
        </div>

        {/* Дни недели */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', marginTop: 8 }}>
          {wd.map((d) => (
            <div key={d} style={{
              textAlign: 'center', fontFamily: M.fontMono, fontSize: 12, fontWeight: 600,
              color: t.textMuted, padding: '4px 0',
            }}>{d}</div>
          ))}
        </div>

        {/* Сетка чисел */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', rowGap: 4, marginTop: 2 }}>
          {weeks.flat().map((day, i) => (
            <div key={i} style={{ display: 'flex', justifyContent: 'center', padding: '4px 0' }}>
              {day != null && (
                <div style={{
                  width: 38, height: 38, borderRadius: 19,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontFamily: M.fontMono, fontSize: 16, fontWeight: day === sel ? 700 : 500,
                  background: day === sel ? t.accent : 'transparent',
                  color: day === sel ? t.accentInk : t.text,
                }}>{day}</div>
              )}
            </div>
          ))}
        </div>

        {/* Выбранная дата */}
        <div style={{
          marginTop: 14, fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text,
        }}>1 июня 2026</div>

        <button style={{ ...payCta(t), marginTop: 16 }}>Применить</button>
      </div>
    </React.Fragment>
  );
}

// ════════════════════════════════════════════════════════════════
// iOS — Настройки · Зарплата
// ════════════════════════════════════════════════════════════════
function IOSScreenSalarySettings({ dark = false, height = 1900, sheet = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <div style={{ paddingTop: 58 }}>
        <NavBarIOS t={t} title="Зарплата" back backLabel="Настройки" style="text"/>
      </div>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '4px 16px 36px' }}>
        <SalSettingsBody t={t}/>
      </div>
      {sheet && <SalTariffDateSheet t={t}/>}
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════════
// Android — Настройки · Зарплата (Material 3)
// ════════════════════════════════════════════════════════════════
function AndroidScreenSalarySettings({ dark = false, height = 1900, sheet = false }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark} height={height}>
      <TopBarAndroid t={t} title="Зарплата" back/>
      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px)', padding: '4px 16px 28px' }}>
        <SalSettingsBody t={t} mat/>
      </div>
      {sheet && <SalTariffDateSheet t={t} mat/>}
    </ADevice>
  );
}

Object.assign(window, {
  IOSScreenSalarySettings, AndroidScreenSalarySettings,
  SalSettingsBody, SalTariffDateSheet,
  PaySetField, PaySetTier, PaySetInput,
});
